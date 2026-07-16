package me.weishu.kernelsu.ui.screen.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.repository.ModuleRepositoryImpl
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.collectRootDiagnosticInfo
import java.security.MessageDigest

internal class AiChatViewModel : ViewModel() {
    private val defaultSystemPrompt = ksuApp.getString(R.string.ai_chat_default_system_prompt)
    private val secureStore = AiChatSecureStore(ksuApp)
    private val attachmentReader = AiAttachmentReader(ksuApp, secureStore)
    private val repository = AiChatRepository(ksuApp.okhttpClient, secureStore::readImage)
    private val moduleRepository = ModuleRepositoryImpl()
    private val restored = secureStore.load(defaultSystemPrompt)
    private var savedConfig = restored.config
    private val trustedHosts = restored.trustedHosts.toMutableSet()
    private val saveMutex = Mutex()
    private var saveJob: Job? = null
    private var generationJob: Job? = null
    private var requestGeneration = 0L
    private var nextMessageId = restored.conversations
        .flatMap(AiConversation::messages)
        .maxOfOrNull(AiMessage::id)
        ?.plus(1L)
        ?: 1L

    private val _uiState = MutableStateFlow(
        AiChatUiState(
            config = restored.config,
            conversations = restored.conversations,
            activeConversationId = restored.activeConversationId,
            configExpanded = !restored.config.isValid(),
        )
    )
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        scheduleSave()
    }

    fun updateDraft(value: String) {
        _uiState.update { it.copy(draft = value.take(MAX_DRAFT_CHARS)) }
    }

    fun setConfigExpanded(expanded: Boolean) {
        _uiState.update { it.copy(configExpanded = expanded) }
    }

    fun updateConfig(transform: (AiApiConfig) -> AiApiConfig) {
        if (_uiState.value.isSending) return
        _uiState.update {
            it.copy(
                config = transform(it.config),
                configDirty = true,
                availableModels = emptyList(),
                configNotice = null,
            )
        }
    }

    fun selectProvider(provider: AiProviderPreset) {
        updateConfig { current ->
            if (current.provider == provider) return@updateConfig current
            if (provider == AiProviderPreset.Compatible) {
                current.copy(provider = provider, apiKey = "", customHeaders = "")
            } else {
                current.copy(
                    provider = provider,
                    baseUrl = provider.defaultBaseUrl,
                    model = provider.defaultModel,
                    apiKey = "",
                    customHeaders = "",
                )
            }
        }
    }

    fun saveConfig() {
        val config = _uiState.value.config.normalized()
        val error = config.validationError()
        if (error != null) {
            val message = validationMessage(error)
            _uiState.update { it.copy(configNotice = message, configNoticeIsError = true) }
            emit(message)
            return
        }
        _uiState.update {
            it.copy(
                config = config,
                configDirty = false,
                configExpanded = false,
                configNotice = ksuApp.getString(R.string.ai_chat_config_saved),
                configNoticeIsError = false,
            )
        }
        savedConfig = config
        scheduleSave(immediate = true)
    }

    fun testConnection() {
        val state = _uiState.value
        if (state.testingApi || state.isSending) return
        val config = state.config.normalized()
        config.validationError()?.let {
            val message = validationMessage(it)
            _uiState.update { current ->
                current.copy(configNotice = message, configNoticeIsError = true)
            }
            return
        }
        _uiState.update { it.copy(testingApi = true, configNotice = null) }
        viewModelScope.launch {
            runCatching { repository.testConnection(config) }
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            testingApi = false,
                            configNotice = ksuApp.getString(R.string.ai_chat_test_ok),
                            configNoticeIsError = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            testingApi = false,
                            configNotice = ksuApp.getString(
                                R.string.ai_chat_test_failed_detail,
                                sanitizeError(error, config),
                            ),
                            configNoticeIsError = true,
                        )
                    }
                }
        }
    }

    fun loadModels() {
        val state = _uiState.value
        if (state.loadingModels || state.testingApi || state.isSending) return
        val config = state.config.normalized()
        config.validationError()?.let {
            _uiState.update { current ->
                current.copy(configNotice = validationMessage(it), configNoticeIsError = true)
            }
            return
        }
        _uiState.update { it.copy(loadingModels = true, configNotice = null) }
        viewModelScope.launch {
            runCatching { repository.fetchModels(config) }
                .onSuccess { models ->
                    _uiState.update { current ->
                        current.copy(
                            loadingModels = false,
                            availableModels = models,
                            configNotice = if (models.isEmpty()) {
                                ksuApp.getString(R.string.ai_chat_models_empty)
                            } else {
                                ksuApp.resources.getQuantityString(
                                    R.plurals.ai_chat_models_loaded,
                                    models.size,
                                    models.size,
                                )
                            },
                            configNoticeIsError = models.isEmpty(),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            loadingModels = false,
                            configNotice = ksuApp.getString(
                                R.string.ai_chat_models_failed,
                                sanitizeError(error, config),
                            ),
                            configNoticeIsError = true,
                        )
                    }
                }
        }
    }

    fun importDocument(uri: Uri) = importAttachment { attachmentReader.readDocument(uri) }

    fun importImage(uri: Uri) = importAttachment { attachmentReader.readImage(uri) }

    fun removePendingAttachment(attachmentId: String) {
        val attachment = _uiState.value.pendingAttachments.firstOrNull { it.id == attachmentId } ?: return
        _uiState.update { state ->
            state.copy(pendingAttachments = state.pendingAttachments.filterNot { it.id == attachmentId })
        }
        attachment.storageId?.let(secureStore::deleteImage)
    }

    fun requestSend() {
        val state = _uiState.value
        if (state.isSending) return
        val text = state.draft.trim()
        if (text.isBlank() && state.pendingAttachments.isEmpty()) return
        prepareTransmission(
            AiPendingTransmission(
                text = text.ifBlank { ksuApp.getString(R.string.ai_chat_attachment_prompt) },
                attachments = state.pendingAttachments,
                source = AiTransmissionSource.Chat,
                targetHost = resolveAiTargetHost(state.config.baseUrl),
            )
        )
    }

    fun retry(messageId: Long) {
        val state = _uiState.value
        if (state.isSending) return
        val messages = state.activeConversation.messages
        val assistantIndex = messages.indexOfFirst { it.id == messageId && it.role == AiRole.Assistant }
        if (assistantIndex <= 0) return
        val userMessage = messages.take(assistantIndex).lastOrNull { it.role == AiRole.User } ?: return
        prepareTransmission(
            AiPendingTransmission(
                text = userMessage.text,
                attachments = userMessage.attachments,
                source = AiTransmissionSource.Retry,
                targetHost = resolveAiTargetHost(state.config.baseUrl),
                retryOfId = messageId,
                contextThroughMessageId = userMessage.id,
                containsModuleInventory = userMessage.attachments.any { it.name == MODULE_REPORT_NAME },
            )
        )
    }

    fun prepareModuleAnalysis() {
        val state = _uiState.value
        if (state.analyzingModules || state.isSending) return
        _uiState.update { it.copy(analyzingModules = true) }
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val modules = async { moduleRepository.getModules().getOrThrow() }
                    val root = async { collectRootDiagnosticInfo() }
                    modules.await() to root.await()
                }
            }.onSuccess { (modules, root) ->
                if (modules.isEmpty()) {
                    emit(ksuApp.getString(R.string.ai_chat_modules_empty))
                    _uiState.update { it.copy(analyzingModules = false) }
                    return@onSuccess
                }
                val report = buildModuleAnalysisReport(modules, root)
                val bytes = report.toByteArray(Charsets.UTF_8)
                val attachment = AiAttachment(
                    kind = AiAttachmentKind.Text,
                    name = MODULE_REPORT_NAME,
                    sizeBytes = bytes.size.toLong(),
                    mimeType = "text/markdown",
                    extractedText = report,
                    sha256 = bytes.sha256(),
                )
                val current = _uiState.value
                _uiState.update { it.copy(analyzingModules = false) }
                prepareTransmission(
                    AiPendingTransmission(
                        text = ksuApp.getString(R.string.ai_chat_module_analysis_prompt),
                        attachments = listOf(attachment),
                        source = AiTransmissionSource.ModuleAnalysis,
                        targetHost = resolveAiTargetHost(current.config.baseUrl),
                        containsModuleInventory = true,
                    )
                )
            }.onFailure { error ->
                _uiState.update { it.copy(analyzingModules = false) }
                emit(
                    ksuApp.getString(
                        R.string.ai_chat_modules_failed,
                        sanitizeError(error, state.config),
                    )
                )
            }
        }
    }

    fun confirmTransmission() {
        val pending = _uiState.value.pendingTransmission ?: return
        trustedHosts += pending.targetHost
        _uiState.update { it.copy(pendingTransmission = null) }
        executeTransmission(pending)
    }

    fun cancelTransmission() {
        _uiState.update { it.copy(pendingTransmission = null) }
    }

    fun stopGeneration() {
        if (!_uiState.value.isSending) return
        requestGeneration += 1L
        _uiState.update { state ->
            state.copy(
                conversations = state.conversations.map { conversation ->
                    conversation.copy(
                        messages = conversation.messages.map { message ->
                            if (message.status == AiMessageStatus.Generating) {
                                message.copy(
                                    status = AiMessageStatus.Stopped,
                                    errorDetail = ksuApp.getString(R.string.ai_chat_stopped),
                                )
                            } else {
                                message
                            }
                        }
                    )
                }
            )
        }
        repository.cancelActive()
        generationJob?.cancel()
        generationJob = null
        scheduleSave(immediate = true)
    }

    fun requestClearConversation() {
        if (_uiState.value.isSending) return
        _uiState.update { it.copy(showClearConfirmation = true) }
    }

    fun dismissClearConversation() {
        _uiState.update { it.copy(showClearConfirmation = false) }
    }

    fun clearConversation() {
        if (_uiState.value.isSending) return
        _uiState.update { state ->
            state.copy(
                conversations = state.conversations.map { conversation ->
                    if (conversation.id == state.activeConversationId) {
                        conversation.copy(
                            title = DEFAULT_CONVERSATION_TITLE,
                            updatedAt = System.currentTimeMillis(),
                            messages = emptyList(),
                        )
                    } else {
                        conversation
                    }
                },
                draft = "",
                pendingAttachments = emptyList(),
                showClearConfirmation = false,
            )
        }
        scheduleSave(immediate = true)
    }

    fun newConversation() {
        if (_uiState.value.isSending) {
            emit(ksuApp.getString(R.string.ai_chat_finish_current_first))
            return
        }
        val conversation = newAiConversation()
        _uiState.update { state ->
            state.copy(
                conversations = (listOf(conversation) + state.conversations)
                    .take(MAX_SAVED_CONVERSATIONS),
                activeConversationId = conversation.id,
                draft = "",
                pendingAttachments = emptyList(),
            )
        }
        scheduleSave()
    }

    fun selectConversation(id: String) {
        val state = _uiState.value
        if (state.conversations.none { it.id == id }) return
        _uiState.update {
            it.copy(activeConversationId = id, draft = "", pendingAttachments = emptyList())
        }
        scheduleSave()
    }

    fun renameActiveConversation(title: String) {
        val clean = title.trim().take(MAX_CONVERSATION_TITLE_CHARS)
        if (clean.isBlank()) return
        updateActiveConversation { it.copy(title = clean, updatedAt = System.currentTimeMillis()) }
        scheduleSave()
    }

    fun requestDeleteConversation() {
        if (_uiState.value.isSending) return
        _uiState.update { it.copy(showDeleteConversationConfirmation = true) }
    }

    fun dismissDeleteConversation() {
        _uiState.update { it.copy(showDeleteConversationConfirmation = false) }
    }

    fun deleteConversation() {
        if (_uiState.value.isSending) return
        _uiState.update { state ->
            val remaining = state.conversations.filterNot { it.id == state.activeConversationId }
                .ifEmpty { listOf(newAiConversation()) }
            state.copy(
                conversations = remaining,
                activeConversationId = remaining.first().id,
                draft = "",
                pendingAttachments = emptyList(),
                showDeleteConversationConfirmation = false,
            )
        }
        scheduleSave(immediate = true)
    }

    private fun importAttachment(reader: suspend () -> AiAttachment) {
        val state = _uiState.value
        if (state.importingAttachment || state.isSending) return
        if (state.pendingAttachments.size >= MAX_PENDING_ATTACHMENTS) {
            emit(
                ksuApp.resources.getQuantityString(
                    R.plurals.ai_chat_too_many_attachments,
                    MAX_PENDING_ATTACHMENTS,
                    MAX_PENDING_ATTACHMENTS,
                )
            )
            return
        }
        _uiState.update { it.copy(importingAttachment = true) }
        viewModelScope.launch {
            runCatching { reader() }
                .onSuccess { attachment ->
                    val current = _uiState.value
                    if (
                        attachment.kind == AiAttachmentKind.Image &&
                        current.pendingAttachments.count { it.kind == AiAttachmentKind.Image } >= MAX_PENDING_IMAGES
                    ) {
                        attachment.storageId?.let(secureStore::deleteImage)
                        _uiState.update { it.copy(importingAttachment = false) }
                        emit(
                            ksuApp.resources.getQuantityString(
                                R.plurals.ai_chat_too_many_images,
                                MAX_PENDING_IMAGES,
                                MAX_PENDING_IMAGES,
                            )
                        )
                        return@onSuccess
                    }
                    _uiState.update { current ->
                        current.copy(
                            importingAttachment = false,
                            pendingAttachments = current.pendingAttachments + attachment,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(importingAttachment = false) }
                    val message = when (error) {
                        is AttachmentTooLargeException -> ksuApp.getString(
                            R.string.ai_chat_attachment_too_large,
                            formatBytes(error.maxBytes.toLong()),
                        )
                        is UnsupportedAttachmentException -> ksuApp.getString(
                            R.string.ai_chat_unsupported_file,
                        )
                        else -> ksuApp.getString(R.string.ai_chat_file_failed)
                    }
                    emit(message)
                }
        }
    }

    private fun prepareTransmission(pending: AiPendingTransmission) {
        val state = _uiState.value
        if (state.configDirty) {
            emit(ksuApp.getString(R.string.ai_chat_save_config_first))
            _uiState.update { it.copy(configExpanded = true) }
            return
        }
        val config = state.config.normalized()
        config.validationError()?.let {
            emit(validationMessage(it))
            _uiState.update { current -> current.copy(configExpanded = true) }
            return
        }
        if (pending.targetHost == "unknown") {
            emit(ksuApp.getString(R.string.ai_chat_invalid_url))
            return
        }
        val prepared = pending.copy(
            sendsAuthentication = config.apiKey.isNotBlank() || config.customHeaders.isNotBlank()
        )
        val containsSensitiveData = prepared.attachments.isNotEmpty() || prepared.containsModuleInventory
        if (containsSensitiveData || pending.targetHost !in trustedHosts) {
            _uiState.update { it.copy(config = config, pendingTransmission = prepared) }
        } else {
            _uiState.update { it.copy(config = config) }
            executeTransmission(prepared)
        }
    }

    private fun executeTransmission(pending: AiPendingTransmission) {
        val initialState = _uiState.value
        if (initialState.isSending) return
        val conversationId = initialState.activeConversationId
        var contextThroughId = pending.contextThroughMessageId

        if (pending.source != AiTransmissionSource.Retry) {
            val userId = nextMessageId++
            contextThroughId = userId
            val userMessage = AiMessage(
                id = userId,
                role = AiRole.User,
                text = pending.text,
                status = AiMessageStatus.Ready,
                attachments = pending.attachments,
            )
            updateConversation(conversationId) { conversation ->
                conversation.copy(
                    title = if (conversation.messages.isEmpty()) {
                        deriveConversationTitle(pending.text)
                    } else {
                        conversation.title
                    },
                    updatedAt = System.currentTimeMillis(),
                    messages = conversation.messages + userMessage,
                )
            }
            if (pending.source == AiTransmissionSource.Chat) {
                _uiState.update { it.copy(draft = "", pendingAttachments = emptyList()) }
            }
        }

        val assistantId = nextMessageId++
        val contextMessages = _uiState.value.conversations
            .first { it.id == conversationId }
            .messages
        val context = AiContextBuilder.select(
            config = _uiState.value.config,
            allMessages = contextMessages,
            throughMessageId = contextThroughId,
        )
        val assistant = AiMessage(
            id = assistantId,
            role = AiRole.Assistant,
            text = "",
            status = AiMessageStatus.Generating,
            retryOfId = pending.retryOfId,
            droppedContextMessages = context.droppedMessages,
        )
        updateConversation(conversationId) { conversation ->
            conversation.copy(
                updatedAt = System.currentTimeMillis(),
                messages = conversation.messages + assistant,
            )
        }
        scheduleSave()

        val generation = ++requestGeneration
        val config = _uiState.value.config
        generationJob = viewModelScope.launch {
            try {
                val result = repository.streamChat(config, context) { delta ->
                    withContext(Dispatchers.Main.immediate) {
                        if (generation != requestGeneration) return@withContext
                        updateMessage(conversationId, assistantId) { message ->
                            message.copy(text = message.text + delta)
                        }
                        scheduleSave()
                    }
                }
                if (generation != requestGeneration) return@launch
                updateMessage(conversationId, assistantId) { message ->
                    message.copy(
                        text = result.text.ifBlank { message.text },
                        status = AiMessageStatus.Completed,
                        usage = result.usage,
                        errorDetail = result.requestedTools.takeIf { it.isNotEmpty() }?.let {
                            ksuApp.getString(R.string.ai_chat_tools_not_executed, it.joinToString())
                        },
                    )
                }
            } catch (_: CancellationException) {
                if (generation == requestGeneration) {
                    updateMessage(conversationId, assistantId) { message ->
                        if (message.status == AiMessageStatus.Generating) {
                            message.copy(status = AiMessageStatus.Stopped)
                        } else {
                            message
                        }
                    }
                }
            } catch (error: Throwable) {
                if (generation != requestGeneration) return@launch
                val detail = sanitizeError(error, config)
                updateMessage(conversationId, assistantId) { message ->
                    if (message.text.isBlank()) {
                        message.copy(
                            text = ksuApp.getString(R.string.ai_chat_request_failed, detail),
                            status = AiMessageStatus.Error,
                            errorDetail = detail,
                        )
                    } else {
                        message.copy(
                            status = AiMessageStatus.Partial,
                            errorDetail = detail,
                        )
                    }
                }
                emit(ksuApp.getString(R.string.ai_chat_request_failed, detail))
            } finally {
                if (generation == requestGeneration) generationJob = null
                scheduleSave(immediate = true)
            }
        }
    }

    private fun updateMessage(
        conversationId: String,
        messageId: Long,
        transform: (AiMessage) -> AiMessage,
    ) {
        updateConversation(conversationId) { conversation ->
            conversation.copy(
                updatedAt = System.currentTimeMillis(),
                messages = conversation.messages.map { message ->
                    if (message.id == messageId) transform(message) else message
                },
            )
        }
    }

    private fun updateActiveConversation(transform: (AiConversation) -> AiConversation) {
        updateConversation(_uiState.value.activeConversationId, transform)
    }

    private fun updateConversation(id: String, transform: (AiConversation) -> AiConversation) {
        _uiState.update { state ->
            state.copy(
                conversations = state.conversations.map { conversation ->
                    if (conversation.id == id) transform(conversation) else conversation
                }
            )
        }
    }

    private fun scheduleSave(immediate: Boolean = false) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            if (!immediate) delay(SAVE_DEBOUNCE_MS)
            saveMutex.withLock {
                val snapshot = persistedSnapshot()
                if (secureStore.save(snapshot)) {
                    secureStore.pruneImages(referencedImageIds(snapshot))
                }
            }
        }
    }

    private fun persistedSnapshot(): AiPersistedState {
        val state = _uiState.value
        return AiPersistedState(
            config = savedConfig.normalized(),
            conversations = state.conversations,
            activeConversationId = state.activeConversationId,
            trustedHosts = trustedHosts.toSet(),
        )
    }

    private fun referencedImageIds(state: AiPersistedState): Set<String> = buildSet {
        state.conversations.forEach { conversation ->
            conversation.messages.forEach { message ->
                message.attachments.mapNotNullTo(this) { it.storageId }
            }
        }
        _uiState.value.pendingAttachments.mapNotNullTo(this) { it.storageId }
    }

    private fun validationMessage(error: AiConfigValidationError): String = ksuApp.getString(
        when (error) {
            AiConfigValidationError.InvalidUrl -> R.string.ai_chat_invalid_url
            AiConfigValidationError.InsecureUrl -> R.string.ai_chat_insecure_url
            AiConfigValidationError.MissingApiKey -> R.string.ai_chat_missing_api_key
            AiConfigValidationError.MissingModel -> R.string.ai_chat_missing_model
            AiConfigValidationError.InvalidTemperature -> R.string.ai_chat_invalid_temperature
            AiConfigValidationError.InvalidOutputLimit -> R.string.ai_chat_invalid_output_limit
            AiConfigValidationError.InvalidContextLimit -> R.string.ai_chat_invalid_context_limit
            AiConfigValidationError.ContextTooSmall -> R.string.ai_chat_context_too_small
            AiConfigValidationError.InvalidHeaders -> R.string.ai_chat_invalid_headers
        }
    )

    private fun sanitizeError(error: Throwable, config: AiApiConfig): String {
        var detail = error.message.orEmpty().ifBlank { error.javaClass.simpleName }
        if (config.apiKey.isNotBlank()) detail = detail.replace(config.apiKey, "***")
        return detail.replace(Regex("Bearer\\s+\\S+", RegexOption.IGNORE_CASE), "Bearer ***")
            .replace('\n', ' ')
            .replace('\r', ' ')
            .take(MAX_ERROR_DISPLAY_CHARS)
    }

    private fun emit(message: String) {
        _events.tryEmit(message)
    }

    override fun onCleared() {
        requestGeneration += 1L
        repository.cancelActive()
        generationJob?.cancel()
        saveJob?.cancel()
        secureStore.save(persistedSnapshot())
        super.onCleared()
    }

    private fun AiApiConfig.normalized(): AiApiConfig = copy(
        baseUrl = baseUrl.trim().trimEnd('/').take(MAX_BASE_URL_CHARS),
        apiKey = apiKey.trim().take(MAX_API_KEY_CHARS),
        model = model.trim().take(MAX_MODEL_CHARS),
        systemPrompt = systemPrompt.trim().ifBlank { defaultSystemPrompt }.take(MAX_SYSTEM_PROMPT_CHARS),
        customHeaders = customHeaders.trim().take(MAX_CUSTOM_HEADERS_CHARS),
        temperature = temperature.coerceIn(0f, 2f),
        maxOutputTokens = maxOutputTokens.coerceIn(MIN_OUTPUT_TOKENS, MAX_OUTPUT_TOKENS),
        contextWindowTokens = contextWindowTokens.coerceIn(MIN_CONTEXT_TOKENS, MAX_CONTEXT_TOKENS),
    )

    private companion object {
        const val SAVE_DEBOUNCE_MS = 450L
        const val MAX_DRAFT_CHARS = 32_000
        const val MAX_PENDING_ATTACHMENTS = 5
        const val MAX_PENDING_IMAGES = 4
        const val MAX_CONVERSATION_TITLE_CHARS = 32
        const val MAX_ERROR_DISPLAY_CHARS = 700
        const val MAX_BASE_URL_CHARS = 2_048
        const val MAX_API_KEY_CHARS = 4_096
        const val MAX_MODEL_CHARS = 256
        const val MAX_SYSTEM_PROMPT_CHARS = 16_000
        const val MAX_CUSTOM_HEADERS_CHARS = 8_000
        const val MODULE_REPORT_NAME = "apkesu-module-report.md"
    }
}

private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { "%02x".format(it) }

internal fun formatBytes(bytes: Long): String {
    val size = bytes.coerceAtLeast(0L).toDouble()
    return when {
        size >= 1024 * 1024 -> "%.1f MB".format(size / 1024 / 1024)
        size >= 1024 -> "%.1f KB".format(size / 1024)
        else -> "$bytes B"
    }
}
