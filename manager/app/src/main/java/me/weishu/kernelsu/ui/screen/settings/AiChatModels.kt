package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.UUID
import java.util.Locale

internal enum class AiProviderPreset(
    val defaultBaseUrl: String,
    val defaultModel: String,
) {
    OpenAi("https://api.openai.com/v1", "gpt-4o-mini"),
    DeepSeek("https://api.deepseek.com/v1", "deepseek-chat"),
    Compatible("", ""),
}

@Immutable
internal data class AiApiConfig(
    val provider: AiProviderPreset = AiProviderPreset.OpenAi,
    val baseUrl: String = AiProviderPreset.OpenAi.defaultBaseUrl,
    val apiKey: String = "",
    val model: String = AiProviderPreset.OpenAi.defaultModel,
    val systemPrompt: String = "",
    val temperature: Float = 0.6f,
    val maxOutputTokens: Int = 2_048,
    val contextWindowTokens: Int = 32_768,
    val customHeaders: String = "",
) {
    fun validationError(): AiConfigValidationError? {
        val uri = runCatching { URI(baseUrl.trim()) }.getOrNull()
            ?: return AiConfigValidationError.InvalidUrl
        if (uri.scheme !in setOf("https", "http") || uri.host.isNullOrBlank()) {
            return AiConfigValidationError.InvalidUrl
        }
        if (uri.rawQuery != null || uri.rawFragment != null || uri.rawUserInfo != null) {
            return AiConfigValidationError.InvalidUrl
        }
        if (uri.scheme == "http" && !uri.isLoopbackHost()) {
            return AiConfigValidationError.InsecureUrl
        }
        if (provider != AiProviderPreset.Compatible && apiKey.isBlank()) {
            return AiConfigValidationError.MissingApiKey
        }
        if (model.isBlank()) return AiConfigValidationError.MissingModel
        if (temperature !in 0f..2f) return AiConfigValidationError.InvalidTemperature
        if (maxOutputTokens !in MIN_OUTPUT_TOKENS..MAX_OUTPUT_TOKENS) {
            return AiConfigValidationError.InvalidOutputLimit
        }
        if (contextWindowTokens !in MIN_CONTEXT_TOKENS..MAX_CONTEXT_TOKENS) {
            return AiConfigValidationError.InvalidContextLimit
        }
        if (maxOutputTokens + CONTEXT_RESERVE_TOKENS >= contextWindowTokens) {
            return AiConfigValidationError.ContextTooSmall
        }
        if (runCatching { parseAiCustomHeaders(customHeaders) }.isFailure) {
            return AiConfigValidationError.InvalidHeaders
        }
        return null
    }

    fun isValid(): Boolean = validationError() == null
}

internal enum class AiConfigValidationError {
    InvalidUrl,
    InsecureUrl,
    MissingApiKey,
    MissingModel,
    InvalidTemperature,
    InvalidOutputLimit,
    InvalidContextLimit,
    ContextTooSmall,
    InvalidHeaders,
}

internal fun parseAiCustomHeaders(raw: String): List<Pair<String, String>> =
    raw.lineSequence().mapIndexedNotNull { index, line ->
        val clean = line.trim()
        if (clean.isBlank() || clean.startsWith('#')) return@mapIndexedNotNull null
        val separator = clean.indexOf(':')
        require(separator > 0) { "Invalid custom header on line ${index + 1}" }
        val name = clean.substring(0, separator).trim()
        val value = clean.substring(separator + 1).trim()
        require(name.lowercase(Locale.ROOT) !in AI_RESERVED_HEADERS) {
            "Reserved header cannot be overridden: $name"
        }
        require(AI_HEADER_NAME_REGEX.matches(name) && !value.contains('\r') && !value.contains('\n')) {
            "Invalid custom header name on line ${index + 1}"
        }
        name to value
    }.toList()

private fun URI.isLoopbackHost(): Boolean {
    val normalized = host.orEmpty().lowercase()
    return normalized == "localhost" || normalized == "127.0.0.1" || normalized == "::1"
}

internal enum class AiRole {
    User,
    Assistant,
}

internal enum class AiMessageStatus {
    Ready,
    Generating,
    Completed,
    Stopped,
    Partial,
    Error,
}

internal enum class AiAttachmentKind {
    Text,
    Archive,
    Image,
}

@Immutable
internal data class AiAttachment(
    val id: String = UUID.randomUUID().toString(),
    val kind: AiAttachmentKind,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val extractedText: String = "",
    val truncated: Boolean = false,
    val storageId: String? = null,
    val sha256: String = "",
    val width: Int? = null,
    val height: Int? = null,
)

@Immutable
internal data class AiTokenUsage(
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val totalTokens: Int? = null,
)

@Immutable
internal data class AiMessage(
    val id: Long,
    val role: AiRole,
    val text: String,
    val status: AiMessageStatus,
    val attachments: List<AiAttachment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val retryOfId: Long? = null,
    val errorDetail: String? = null,
    val usage: AiTokenUsage? = null,
    val droppedContextMessages: Int = 0,
)

@Immutable
internal data class AiConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val messages: List<AiMessage> = emptyList(),
)

internal data class AiPersistedState(
    val config: AiApiConfig,
    val conversations: List<AiConversation>,
    val activeConversationId: String,
    val trustedHosts: Set<String> = emptySet(),
)

internal enum class AiTransmissionSource {
    Chat,
    Retry,
    ModuleAnalysis,
}

@Immutable
internal data class AiPendingTransmission(
    val text: String,
    val attachments: List<AiAttachment>,
    val source: AiTransmissionSource,
    val targetHost: String,
    val retryOfId: Long? = null,
    val contextThroughMessageId: Long? = null,
    val containsModuleInventory: Boolean = false,
    val sendsAuthentication: Boolean = false,
)

internal data class AiChatUiState(
    val config: AiApiConfig,
    val configDirty: Boolean = false,
    val conversations: List<AiConversation>,
    val activeConversationId: String,
    val draft: String = "",
    val pendingAttachments: List<AiAttachment> = emptyList(),
    val configExpanded: Boolean = false,
    val testingApi: Boolean = false,
    val loadingModels: Boolean = false,
    val analyzingModules: Boolean = false,
    val importingAttachment: Boolean = false,
    val availableModels: List<String> = emptyList(),
    val configNotice: String? = null,
    val configNoticeIsError: Boolean = false,
    val pendingTransmission: AiPendingTransmission? = null,
    val showClearConfirmation: Boolean = false,
    val showDeleteConversationConfirmation: Boolean = false,
) {
    val activeConversation: AiConversation
        get() = conversations.firstOrNull { it.id == activeConversationId }
            ?: conversations.first()

    val isSending: Boolean
        get() = conversations.any { conversation ->
            conversation.messages.any { it.status == AiMessageStatus.Generating }
        }
}

internal fun newAiConversation(now: Long = System.currentTimeMillis()): AiConversation =
    AiConversation(title = DEFAULT_CONVERSATION_TITLE, createdAt = now, updatedAt = now)

internal fun AiPersistedState.toJson(): JSONObject = JSONObject()
    .put("schema", AI_CHAT_SCHEMA_VERSION)
    .put("config", config.toJson())
    .put("activeConversationId", activeConversationId)
    .put("trustedHosts", JSONArray(trustedHosts.sorted()))
    .put("conversations", JSONArray().apply {
        conversations
            .sortedByDescending { it.updatedAt }
            .take(MAX_SAVED_CONVERSATIONS)
            .forEach { put(it.toJson()) }
    })

internal fun parseAiPersistedState(
    raw: String,
    defaultSystemPrompt: String,
): AiPersistedState? = runCatching {
    val root = JSONObject(raw)
    val config = root.optJSONObject("config")?.toAiApiConfig(defaultSystemPrompt)
        ?: defaultAiApiConfig(defaultSystemPrompt)
    val conversations = root.optJSONArray("conversations")
        .toConversationList()
        .ifEmpty { listOf(newAiConversation()) }
        .map { conversation ->
            conversation.copy(messages = conversation.messages.map(AiMessage::recoverInterruptedGeneration))
        }
    val requestedActiveId = root.optString("activeConversationId")
    val activeId = requestedActiveId.takeIf { id -> conversations.any { it.id == id } }
        ?: conversations.first().id
    val trustedHosts = root.optJSONArray("trustedHosts").toStringSet()
    AiPersistedState(config, conversations, activeId, trustedHosts)
}.getOrNull()

internal fun parseLegacyAiMessages(raw: String?): List<AiMessage> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val legacyRole = item.optString("role")
                val role = if (legacyRole == "User") AiRole.User else AiRole.Assistant
                val status = when {
                    legacyRole == "Error" -> AiMessageStatus.Error
                    role == AiRole.User -> AiMessageStatus.Ready
                    else -> AiMessageStatus.Completed
                }
                add(
                    AiMessage(
                        id = item.optLong("id", index + 1L),
                        role = role,
                        text = item.optString("text"),
                        status = status,
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

internal fun defaultAiApiConfig(defaultSystemPrompt: String): AiApiConfig =
    AiApiConfig(systemPrompt = defaultSystemPrompt)

internal fun deriveConversationTitle(text: String): String {
    val normalized = text.lineSequence().firstOrNull().orEmpty()
        .replace(Regex("\\s+"), " ")
        .trim()
    return normalized.take(MAX_CONVERSATION_TITLE_CHARS).ifBlank { DEFAULT_CONVERSATION_TITLE }
}

private fun AiApiConfig.toJson(): JSONObject = JSONObject()
    .put("provider", provider.name)
    .put("baseUrl", baseUrl)
    .put("apiKey", apiKey)
    .put("model", model)
    .put("systemPrompt", systemPrompt)
    .put("temperature", temperature.toDouble())
    .put("maxOutputTokens", maxOutputTokens)
    .put("contextWindowTokens", contextWindowTokens)
    .put("customHeaders", customHeaders)

private fun JSONObject.toAiApiConfig(defaultSystemPrompt: String): AiApiConfig {
    val provider = enumValueOrDefault(optString("provider"), AiProviderPreset.OpenAi)
    return AiApiConfig(
        provider = provider,
        baseUrl = optString("baseUrl", provider.defaultBaseUrl),
        apiKey = optString("apiKey"),
        model = optString("model", provider.defaultModel),
        systemPrompt = optString("systemPrompt", defaultSystemPrompt),
        temperature = optDouble("temperature", 0.6).toFloat().coerceIn(0f, 2f),
        maxOutputTokens = optInt("maxOutputTokens", 2_048)
            .coerceIn(MIN_OUTPUT_TOKENS, MAX_OUTPUT_TOKENS),
        contextWindowTokens = optInt("contextWindowTokens", 32_768)
            .coerceIn(MIN_CONTEXT_TOKENS, MAX_CONTEXT_TOKENS),
        customHeaders = optString("customHeaders"),
    )
}

private fun AiConversation.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)
    .put("messages", JSONArray().apply {
        messages.forPersistence().forEach { put(it.toJson()) }
    })

private fun List<AiMessage>.forPersistence(): List<AiMessage> {
    if (isEmpty()) return emptyList()
    val bounded = takeLast(MAX_SAVED_MESSAGES_PER_CONVERSATION)
    var startIndex = bounded.size
    var characters = 0
    for (index in bounded.lastIndex downTo 0) {
        val messageCharacters = bounded[index].persistedCharacterEstimate()
        if (startIndex < bounded.size && characters + messageCharacters > MAX_SAVED_CONVERSATION_CHARS) {
            break
        }
        characters += messageCharacters
        startIndex = index
    }
    while (startIndex > 0 && bounded[startIndex].role == AiRole.Assistant) {
        startIndex -= 1
    }
    return bounded.subList(startIndex.coerceAtMost(bounded.lastIndex), bounded.size)
}

private fun AiMessage.persistedCharacterEstimate(): Int =
    text.length + errorDetail.orEmpty().length + attachments.sumOf { it.extractedText.length + 256 } + 256

private fun JSONArray?.toConversationList(): List<AiConversation> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val id = item.optString("id").ifBlank { UUID.randomUUID().toString() }
            val createdAt = item.optLong("createdAt", System.currentTimeMillis())
            val messages = item.optJSONArray("messages").toMessageList()
            add(
                AiConversation(
                    id = id,
                    title = item.optString("title", DEFAULT_CONVERSATION_TITLE),
                    createdAt = createdAt,
                    updatedAt = item.optLong("updatedAt", createdAt),
                    messages = messages,
                )
            )
        }
    }
}

private fun AiMessage.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("role", role.name)
    .put("text", text)
    .put("status", status.name)
    .put("createdAt", createdAt)
    .put("retryOfId", retryOfId)
    .put("errorDetail", errorDetail)
    .put("droppedContextMessages", droppedContextMessages)
    .put("usage", usage?.toJson())
    .put("attachments", JSONArray().apply { attachments.forEach { put(it.toJson()) } })

private fun JSONArray?.toMessageList(): List<AiMessage> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val role = enumValueOrDefault(item.optString("role"), AiRole.Assistant)
            val defaultStatus = if (role == AiRole.User) AiMessageStatus.Ready else AiMessageStatus.Completed
            add(
                AiMessage(
                    id = item.optLong("id", index + 1L),
                    role = role,
                    text = item.optString("text"),
                    status = enumValueOrDefault(item.optString("status"), defaultStatus),
                    attachments = item.optJSONArray("attachments").toAttachmentList(),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    retryOfId = item.optNullableLong("retryOfId"),
                    errorDetail = item.optNullableString("errorDetail"),
                    usage = item.optJSONObject("usage")?.toAiTokenUsage(),
                    droppedContextMessages = item.optInt("droppedContextMessages", 0),
                )
            )
        }
    }
}

private fun AiAttachment.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("kind", kind.name)
    .put("name", name)
    .put("sizeBytes", sizeBytes)
    .put("mimeType", mimeType)
    .put("extractedText", extractedText)
    .put("truncated", truncated)
    .put("storageId", storageId)
    .put("sha256", sha256)
    .put("width", width)
    .put("height", height)

private fun JSONArray?.toAttachmentList(): List<AiAttachment> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val name = item.optString("name").ifBlank { "attachment" }
            add(
                AiAttachment(
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                    kind = enumValueOrDefault(item.optString("kind"), AiAttachmentKind.Text),
                    name = name,
                    sizeBytes = item.optLong("sizeBytes", 0L),
                    mimeType = item.optString("mimeType", "application/octet-stream"),
                    extractedText = item.optString("extractedText"),
                    truncated = item.optBoolean("truncated", false),
                    storageId = item.optNullableString("storageId"),
                    sha256 = item.optString("sha256"),
                    width = item.optNullableInt("width"),
                    height = item.optNullableInt("height"),
                )
            )
        }
    }
}

private fun AiTokenUsage.toJson(): JSONObject = JSONObject()
    .put("inputTokens", inputTokens)
    .put("outputTokens", outputTokens)
    .put("totalTokens", totalTokens)

private fun JSONObject.toAiTokenUsage(): AiTokenUsage = AiTokenUsage(
    inputTokens = optNullableInt("inputTokens"),
    outputTokens = optNullableInt("outputTokens"),
    totalTokens = optNullableInt("totalTokens"),
)

private fun AiMessage.recoverInterruptedGeneration(): AiMessage {
    if (status != AiMessageStatus.Generating) return this
    return if (text.isBlank()) {
        copy(status = AiMessageStatus.Stopped, errorDetail = "Generation interrupted")
    } else {
        copy(status = AiMessageStatus.Partial, errorDetail = "Generation interrupted")
    }
}

private fun JSONArray?.toStringSet(): Set<String> {
    if (this == null) return emptySet()
    return buildSet {
        for (index in 0 until length()) {
            optString(index).trim().takeIf(String::isNotEmpty)?.let(::add)
        }
    }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default

private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

private fun JSONObject.optNullableInt(key: String): Int? =
    if (isNull(key) || !has(key)) null else optInt(key)

private fun JSONObject.optNullableLong(key: String): Long? =
    if (isNull(key) || !has(key)) null else optLong(key)

internal const val MIN_OUTPUT_TOKENS = 64
internal const val MAX_OUTPUT_TOKENS = 32_768
internal const val MIN_CONTEXT_TOKENS = 2_048
internal const val MAX_CONTEXT_TOKENS = 2_000_000
internal const val CONTEXT_RESERVE_TOKENS = 512
internal const val MAX_SAVED_MESSAGES_PER_CONVERSATION = 80
internal const val MAX_SAVED_CONVERSATIONS = 12
internal const val DEFAULT_CONVERSATION_TITLE = "New chat"
private const val MAX_CONVERSATION_TITLE_CHARS = 32
private const val MAX_SAVED_CONVERSATION_CHARS = 1_000_000
private const val AI_CHAT_SCHEMA_VERSION = 2
private val AI_HEADER_NAME_REGEX = Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
private val AI_RESERVED_HEADERS = setOf(
    "authorization",
    "content-type",
    "content-length",
    "host",
    "connection",
    "transfer-encoding",
)
