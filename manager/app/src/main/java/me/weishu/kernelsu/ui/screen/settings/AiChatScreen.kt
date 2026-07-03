package me.weishu.kernelsu.ui.screen.settings

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.data.repository.ModuleRepositoryImpl
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.getFileName
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val prefs = remember {
        context.applicationContext.getSharedPreferences(AI_CHAT_PREFS, Context.MODE_PRIVATE)
    }
    val defaultPrompt = stringResource(id = R.string.ai_chat_default_system_prompt)
    var baseUrl by rememberSaveable {
        mutableStateOf(prefs.getString(AI_CHAT_BASE_URL, DEFAULT_AI_BASE_URL) ?: DEFAULT_AI_BASE_URL)
    }
    var apiKey by rememberSaveable {
        mutableStateOf(prefs.getString(AI_CHAT_API_KEY, "") ?: "")
    }
    var model by rememberSaveable {
        mutableStateOf(prefs.getString(AI_CHAT_MODEL, DEFAULT_AI_MODEL) ?: DEFAULT_AI_MODEL)
    }
    var systemPrompt by rememberSaveable {
        mutableStateOf(prefs.getString(AI_CHAT_SYSTEM_PROMPT, defaultPrompt) ?: defaultPrompt)
    }
    var input by rememberSaveable { mutableStateOf("") }
    var configExpanded by rememberSaveable {
        mutableStateOf(
            (prefs.getString(AI_CHAT_BASE_URL, null).isNullOrBlank() ||
                prefs.getString(AI_CHAT_MODEL, null).isNullOrBlank())
        )
    }
    var sending by rememberSaveable { mutableStateOf(false) }
    var testingApi by rememberSaveable { mutableStateOf(false) }
    var loadingModules by rememberSaveable { mutableStateOf(false) }
    var activeCall by remember { mutableStateOf<Call?>(null) }
    var nextMessageId by remember { mutableStateOf(1L) }
    val messages = remember { mutableStateListOf<AiMessage>() }
    val pendingAttachments = remember { mutableStateListOf<AiAttachment>() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val restored = loadAiChatHistory(prefs)
        messages.clear()
        messages.addAll(restored)
        nextMessageId = (restored.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        saveAiChatHistory(prefs, messages)
    }

    fun currentConfig(): AiApiConfig {
        return AiApiConfig(
            baseUrl = baseUrl.trim(),
            apiKey = apiKey.trim(),
            model = model.trim(),
            systemPrompt = systemPrompt.trim(),
        )
    }

    fun addMessage(
        role: AiRole,
        text: String,
        attachments: List<AiAttachment> = emptyList(),
    ): Long {
        val id = nextMessageId
        messages.add(
            AiMessage(
                id = id,
                role = role,
                text = text,
                attachments = attachments,
            )
        )
        nextMessageId += 1
        return id
    }

    fun updateMessage(id: Long, text: String) {
        val index = messages.indexOfFirst { it.id == id }
        if (index >= 0) {
            messages[index] = messages[index].copy(text = text)
        }
    }

    fun copyMessage(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("ai-chat", text))
        Toast.makeText(context, R.string.ai_chat_copied, Toast.LENGTH_SHORT).show()
    }

    fun stopGeneration() {
        activeCall?.cancel()
        activeCall = null
        sending = false
    }

    fun sendText(text: String, attachments: List<AiAttachment>) {
        val config = currentConfig()
        val cleanText = text.trim()
        if (cleanText.isBlank() && attachments.isEmpty()) return
        if (!config.isValid()) {
            input = cleanText
            Toast.makeText(context, R.string.ai_chat_need_config, Toast.LENGTH_SHORT).show()
            return
        }
        if (sending) return

        addMessage(
            role = AiRole.User,
            text = cleanText.ifBlank { "Please analyze the attached content." },
            attachments = attachments,
        )
        input = ""
        pendingAttachments.clear()
        sending = true
        scope.launch {
            var streamedText = ""
            val assistantId = addMessage(AiRole.Assistant, context.getString(R.string.ai_chat_generating))
            val result = runCatching {
                requestAiChatStream(
                    config = config,
                    messages = messages.filterNot { it.id == assistantId },
                    onCall = { call -> scope.launch { activeCall = call } },
                    onDelta = { delta ->
                        scope.launch {
                            streamedText += delta
                            updateMessage(assistantId, streamedText)
                        }
                    },
                )
            }
            result.onSuccess { reply ->
                updateMessage(assistantId, reply.ifBlank { streamedText.ifBlank { context.getString(R.string.ai_chat_empty_response) } })
            }.onFailure { error ->
                if (streamedText.isBlank()) {
                    updateMessage(
                        assistantId,
                        context.getString(R.string.ai_chat_request_failed, error.message ?: error.javaClass.simpleName)
                    )
                    val index = messages.indexOfFirst { it.id == assistantId }
                    if (index >= 0) {
                        messages[index] = messages[index].copy(role = AiRole.Error)
                    }
                }
            }
            activeCall = null
            sending = false
        }
    }

    fun retryFrom(message: AiMessage) {
        val index = messages.indexOfFirst { it.id == message.id }
        if (index <= 0 || sending) return
        val previousUser = messages.take(index).lastOrNull { it.role == AiRole.User } ?: return
        while (messages.size > index) {
            messages.removeAt(messages.lastIndex)
        }
        sendText(previousUser.text, previousUser.attachments)
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching { readTextAttachment(context, uri) }
            result.onSuccess { pendingAttachments.add(it) }
                .onFailure {
                    Toast.makeText(context, R.string.ai_chat_file_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching { readImageAttachment(context, uri) }
            result.onSuccess { pendingAttachments.add(it) }
                .onFailure { error ->
                    val messageRes = if (error is ImageTooLargeException) {
                        R.string.ai_chat_image_too_large
                    } else {
                        R.string.ai_chat_file_failed
                    }
                    Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
                }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.ai_chat_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            AiApiConfigCard(
                expanded = configExpanded,
                onExpandedChange = { configExpanded = it },
                baseUrl = baseUrl,
                onBaseUrlChange = { baseUrl = it },
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it },
                model = model,
                onModelChange = { model = it },
                systemPrompt = systemPrompt,
                onSystemPromptChange = { systemPrompt = it },
                testing = testingApi,
                onSave = {
                    prefs.edit()
                        .putString(AI_CHAT_BASE_URL, baseUrl.trim())
                        .putString(AI_CHAT_API_KEY, apiKey.trim())
                        .putString(AI_CHAT_MODEL, model.trim())
                        .putString(AI_CHAT_SYSTEM_PROMPT, systemPrompt.trim())
                        .apply()
                    Toast.makeText(context, R.string.ai_chat_config_saved, Toast.LENGTH_SHORT).show()
                    configExpanded = false
                },
                onTest = {
                    val config = currentConfig()
                    if (!config.isValid() || testingApi) return@AiApiConfigCard
                    testingApi = true
                    scope.launch {
                        val ok = runCatching { testAiConnection(config) }
                        testingApi = false
                        Toast.makeText(
                            context,
                            ok.fold(
                                onSuccess = { R.string.ai_chat_test_ok },
                                onFailure = { R.string.ai_chat_test_failed },
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            )

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(id = R.string.ai_chat_welcome),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 10.dp,
                    ),
                ) {
                    items(messages, key = { it.id }) { message ->
                        AiMessageBubble(
                            message = message,
                            sending = sending,
                            onCopy = { copyMessage(message.text) },
                            onRetry = { retryFrom(message) },
                        )
                    }
                }
            }

            AiChatInputBar(
                input = input,
                onInputChange = { input = it },
                sending = sending,
                loadingModules = loadingModules,
                attachments = pendingAttachments,
                onRemoveAttachment = { pendingAttachments.remove(it) },
                onAttachFile = { fileLauncher.launch(arrayOf("*/*")) },
                onAttachImage = { imageLauncher.launch(arrayOf("image/*")) },
                onAnalyzeModules = {
                    if (loadingModules || sending) return@AiChatInputBar
                    loadingModules = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { ModuleRepositoryImpl().getModules() }
                        loadingModules = false
                        result.onSuccess { modules ->
                            if (modules.isEmpty()) {
                                Toast.makeText(context, R.string.ai_chat_modules_empty, Toast.LENGTH_SHORT).show()
                            } else {
                                sendText(buildModuleAnalysisPrompt(modules), emptyList())
                            }
                        }.onFailure { error ->
                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.ai_chat_modules_failed,
                                    error.message ?: error.javaClass.simpleName,
                                ),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                },
                onClear = {
                    messages.clear()
                    pendingAttachments.clear()
                    input = ""
                    saveAiChatHistory(prefs, emptyList())
                },
                onStop = ::stopGeneration,
                onSend = {
                    sendText(input, pendingAttachments.toList())
                },
            )
        }
    }
}

@Composable
private fun AiApiConfigCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    systemPrompt: String,
    onSystemPromptChange: (String) -> Unit,
    testing: Boolean,
    onSave: () -> Unit,
    onTest: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.AutoFixHigh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                text = stringResource(id = R.string.ai_chat_config_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { onExpandedChange(!expanded) }) {
                Text(
                    stringResource(
                        id = if (expanded) {
                            R.string.ai_chat_hide_config
                        } else {
                            R.string.ai_chat_edit_config
                        }
                    )
                )
            }
        }
        if (expanded) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = baseUrl,
                    onValueChange = onBaseUrlChange,
                    label = { Text(stringResource(id = R.string.ai_chat_api_base)) },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = model,
                        onValueChange = onModelChange,
                        label = { Text(stringResource(id = R.string.ai_chat_model)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text(stringResource(id = R.string.ai_chat_api_key)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    value = systemPrompt,
                    onValueChange = onSystemPromptChange,
                    label = { Text(stringResource(id = R.string.ai_chat_system_prompt)) },
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        enabled = !testing && baseUrl.isNotBlank() && model.isNotBlank(),
                        onClick = onTest,
                    ) {
                        if (testing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(id = R.string.ai_chat_test_config))
                    }
                    Button(onClick = onSave) {
                        Text(stringResource(id = R.string.ai_chat_save_config))
                    }
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiMessage,
    sending: Boolean,
    onCopy: () -> Unit,
    onRetry: () -> Unit,
) {
    val isUser = message.role == AiRole.User
    val isError = message.role == AiRole.Error
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 340.dp),
            shape = RoundedCornerShape(8.dp),
            color = when {
                isError -> MaterialTheme.colorScheme.errorContainer
                isUser -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isError -> MaterialTheme.colorScheme.onErrorContainer
                        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                AttachmentChips(attachments = message.attachments)
                if (!isUser && message.text.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = onCopy) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(stringResource(id = R.string.ai_chat_copy))
                        }
                        TextButton(
                            enabled = !sending,
                            onClick = onRetry,
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(stringResource(id = R.string.ai_chat_retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    sending: Boolean,
    loadingModules: Boolean,
    attachments: List<AiAttachment>,
    onRemoveAttachment: (AiAttachment) -> Unit,
    onAttachFile: () -> Unit,
    onAttachImage: () -> Unit,
    onAnalyzeModules: () -> Unit,
    onClear: () -> Unit,
    onStop: () -> Unit,
    onSend: () -> Unit,
) {
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { toolsExpanded = !toolsExpanded },
                    enabled = !sending,
                ) {
                    Icon(Icons.Rounded.MoreHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(id = R.string.ai_chat_tools))
                }
                if (sending) {
                    OutlinedButton(onClick = onStop) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(id = R.string.ai_chat_stop))
                    }
                }
            }

            if (toolsExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onAttachFile, enabled = !sending) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(id = R.string.ai_chat_attach_file))
                    }
                    OutlinedButton(onClick = onAttachImage, enabled = !sending) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(id = R.string.ai_chat_attach_image))
                    }
                    OutlinedButton(onClick = onAnalyzeModules, enabled = !sending && !loadingModules) {
                        if (loadingModules) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(id = R.string.ai_chat_analyze_modules))
                    }
                    TextButton(onClick = onClear, enabled = !sending) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(id = R.string.ai_chat_clear))
                    }
                }
            }

            PendingAttachmentRow(
                attachments = attachments,
                onRemoveAttachment = onRemoveAttachment,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = input,
                    onValueChange = onInputChange,
                    minLines = 1,
                    maxLines = 4,
                    placeholder = { Text(stringResource(id = R.string.ai_chat_input_hint)) },
                    enabled = !sending,
                )
                Button(
                    onClick = onSend,
                    enabled = !sending && (input.isNotBlank() || attachments.isNotEmpty()),
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(id = R.string.ai_chat_send))
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingAttachmentRow(
    attachments: List<AiAttachment>,
    onRemoveAttachment: (AiAttachment) -> Unit,
) {
    if (attachments.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { attachment ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                onClick = { onRemoveAttachment(attachment) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = attachment.displayLabel(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentChips(attachments: List<AiAttachment>) {
    if (attachments.isEmpty()) return
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        attachments.forEach { attachment ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    text = attachment.displayLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private suspend fun requestAiChatStream(
    config: AiApiConfig,
    messages: List<AiMessage>,
    onCall: (Call) -> Unit,
    onDelta: (String) -> Unit,
): String = withContext(Dispatchers.IO) {
    val body = buildChatRequestBody(config, messages, stream = true).toString()
    val requestBuilder = Request.Builder()
        .url(resolveChatEndpoint(config.baseUrl))
        .post(body.toRequestBody(JSON_MEDIA_TYPE))
        .header("Content-Type", "application/json")
    if (config.apiKey.isNotBlank()) {
        requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
    }

    val call = ksuApp.okhttpClient.newCall(requestBuilder.build())
    onCall(call)
    call.execute().use { response ->
        if (!response.isSuccessful) {
            val responseBody = response.body.string()
            throw IOException("${response.code} ${response.message}: ${responseBody.take(600)}")
        }
        val contentType = response.header("Content-Type").orEmpty()
        if (!contentType.contains("text/event-stream", ignoreCase = true)) {
            return@withContext parseAiResponse(response.body.string())
        }
        parseAiStreamResponse(response, onDelta)
    }
}

private suspend fun testAiConnection(config: AiApiConfig): String = withContext(Dispatchers.IO) {
    val messages = listOf(AiMessage(id = 1, role = AiRole.User, text = "Reply with OK."))
    val body = buildChatRequestBody(config, messages, stream = false)
        .put("max_tokens", 8)
        .toString()
    val requestBuilder = Request.Builder()
        .url(resolveChatEndpoint(config.baseUrl))
        .post(body.toRequestBody(JSON_MEDIA_TYPE))
        .header("Content-Type", "application/json")
    if (config.apiKey.isNotBlank()) {
        requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
    }
    ksuApp.okhttpClient.newCall(requestBuilder.build()).execute().use { response ->
        val responseBody = response.body.string()
        if (!response.isSuccessful) {
            throw IOException("${response.code} ${response.message}: ${responseBody.take(600)}")
        }
        parseAiResponse(responseBody)
    }
}

private fun buildChatRequestBody(
    config: AiApiConfig,
    messages: List<AiMessage>,
    stream: Boolean = false,
): JSONObject {
    val chatMessages = JSONArray()
    if (config.systemPrompt.isNotBlank()) {
        chatMessages.put(
            JSONObject()
                .put("role", "system")
                .put("content", config.systemPrompt)
        )
    }

    val requestMessages = messages
        .filter { it.role != AiRole.Error }
        .takeLast(MAX_HISTORY_MESSAGES)
    requestMessages.forEachIndexed { index, message ->
        val isLastUserMessage = index == requestMessages.lastIndex && message.role == AiRole.User
        val role = if (message.role == AiRole.User) "user" else "assistant"
        chatMessages.put(
            JSONObject()
                .put("role", role)
                .put("content", buildMessageContent(message, includeImages = isLastUserMessage))
        )
    }

    return JSONObject()
        .put("model", config.model)
        .put("messages", chatMessages)
        .put("temperature", 0.6)
        .put("stream", stream)
}

private fun buildMessageContent(message: AiMessage, includeImages: Boolean): Any {
    if (message.role != AiRole.User) return message.text

    val textContext = buildString {
        append(message.text)
        val files = message.attachments.filterIsInstance<AiAttachment.TextFile>()
        if (files.isNotEmpty()) {
            append("\n\nAttached file context:")
            files.forEach { file ->
                append("\n\n### ")
                append(file.name)
                append(" (")
                append(formatBytes(file.sizeBytes))
                if (file.truncated) append(", truncated")
                append(")\n")
                append(file.content)
            }
        }
        val images = message.attachments.filterIsInstance<AiAttachment.ImageFile>()
        if (images.isNotEmpty()) {
            append("\n\nAttached images: ")
            append(images.joinToString { it.name })
        }
    }.ifBlank {
        "Please analyze the attached content."
    }

    val imageAttachments = if (includeImages) {
        message.attachments.filterIsInstance<AiAttachment.ImageFile>()
    } else {
        emptyList()
    }
    if (imageAttachments.isEmpty()) return textContext

    return JSONArray().apply {
        put(JSONObject().put("type", "text").put("text", textContext))
        imageAttachments.forEach { image ->
            put(
                JSONObject()
                    .put("type", "image_url")
                    .put(
                        "image_url",
                        JSONObject().put("url", image.dataUrl()),
                    )
            )
        }
    }
}

private fun parseAiResponse(body: String): String {
    val json = runCatching { JSONObject(body) }.getOrNull() ?: return body
    val message = json.optJSONArray("choices")
        ?.optJSONObject(0)
        ?.optJSONObject("message")
        ?: return body.take(MAX_ERROR_BODY_CHARS)
    val content = message.opt("content")
    if (content == null || content == JSONObject.NULL) {
        return body.take(MAX_ERROR_BODY_CHARS)
    }
    if (content is JSONArray) {
        return buildString {
            for (index in 0 until content.length()) {
                val item = content.optJSONObject(index) ?: continue
                val text = item.optString("text")
                if (text.isNotBlank()) append(text)
            }
        }.ifBlank { body.take(MAX_ERROR_BODY_CHARS) }
    }
    return content.toString().ifBlank { body.take(MAX_ERROR_BODY_CHARS) }
}

private fun parseAiStreamResponse(response: okhttp3.Response, onDelta: (String) -> Unit): String {
    val builder = StringBuilder()
    val source = response.body.source()
    while (!source.exhausted()) {
        val line = source.readUtf8Line()?.trim() ?: continue
        if (!line.startsWith("data:")) continue
        val chunk = line.removePrefix("data:").trim()
        if (chunk == "[DONE]") break
        val json = runCatching { JSONObject(chunk) }.getOrNull() ?: continue
        val delta = json.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("delta")
            ?.opt("content")
        val text = when (delta) {
            is JSONArray -> buildString {
                for (index in 0 until delta.length()) {
                    val item = delta.optJSONObject(index) ?: continue
                    val part = item.optString("text")
                    if (part.isNotBlank()) append(part)
                }
            }
            null, JSONObject.NULL -> ""
            else -> delta.toString()
        }
        if (text.isNotBlank()) {
            builder.append(text)
            onDelta(text)
        }
    }
    return builder.toString()
}

private fun resolveChatEndpoint(baseUrl: String): String {
    val clean = baseUrl.trim().trimEnd('/')
    return if (clean.endsWith("/chat/completions")) {
        clean
    } else {
        "$clean/chat/completions"
    }
}

private suspend fun readTextAttachment(
    context: Context,
    uri: Uri,
): AiAttachment.TextFile = withContext(Dispatchers.IO) {
    val (bytes, truncated) = readBytesLimited(context, uri, MAX_TEXT_ATTACHMENT_BYTES)
    val name = uri.getFileName(context).orEmpty().ifBlank { uri.lastPathSegment ?: "attachment" }
    val text = bytes.toString(Charsets.UTF_8)
        .replace(Regex("[\\p{Cntrl}&&[^\n\t\r]]"), " ")
        .trim()
        .ifBlank { "[empty or non-text file]" }
        .take(MAX_TEXT_ATTACHMENT_CHARS)
    AiAttachment.TextFile(
        name = name,
        sizeBytes = bytes.size.toLong(),
        content = text,
        truncated = truncated || text.length >= MAX_TEXT_ATTACHMENT_CHARS,
    )
}

private suspend fun readImageAttachment(
    context: Context,
    uri: Uri,
): AiAttachment.ImageFile = withContext(Dispatchers.IO) {
    val (bytes, truncated) = readBytesLimited(context, uri, MAX_IMAGE_ATTACHMENT_BYTES + 1)
    if (truncated || bytes.size > MAX_IMAGE_ATTACHMENT_BYTES) {
        throw ImageTooLargeException()
    }
    val name = uri.getFileName(context).orEmpty().ifBlank { uri.lastPathSegment ?: "image" }
    val mime = context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
    AiAttachment.ImageFile(
        name = name,
        sizeBytes = bytes.size.toLong(),
        mimeType = mime,
        base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
    )
}

private fun readBytesLimited(
    context: Context,
    uri: Uri,
    limit: Int,
): Pair<ByteArray, Boolean> {
    val output = ByteArrayOutputStream()
    var truncated = false
    context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            val remaining = limit - output.size()
            if (remaining <= 0) {
                truncated = true
                break
            }
            val count = read.coerceAtMost(remaining)
            output.write(buffer, 0, count)
            if (count < read) {
                truncated = true
                break
            }
        }
    }
    return output.toByteArray() to truncated
}

private fun buildModuleAnalysisPrompt(modules: List<Module>): String {
    val disabled = modules.count { !it.enabled }
    val pendingUpdate = modules.count { it.update }
    val pendingRemove = modules.count { it.remove }
    val webUi = modules.count { it.hasWebUi }
    val actionScripts = modules.count { it.hasActionScript }
    val metamodules = modules.count { it.metamodule }
    return buildString {
        append("Please do a deep ApkeSU module health analysis in the user's language.\n")
        append("Focus on compatibility risk, disabled modules, pending update/removal state, metamodule behavior, WebUI/action script hints, possible conflicts, and safe next steps.\n")
        append("Do not invent facts. If evidence is insufficient, say what log or file should be checked next.\n\n")
        append("Summary:\n")
        append("- installed: ${modules.size}\n")
        append("- disabled: $disabled\n")
        append("- pending update: $pendingUpdate\n")
        append("- pending removal: $pendingRemove\n")
        append("- modules with WebUI: $webUi\n")
        append("- modules with action script: $actionScripts\n")
        append("- metamodules: $metamodules\n\n")
        append("Module inventory:\n")
        modules.sortedBy { it.name.lowercase() }.forEachIndexed { index, module ->
            append("\n")
            append(index + 1)
            append(". ")
            append(module.name)
            append(" [")
            append(module.id)
            append("]\n")
            append("   version: ")
            append(module.version)
            append(" (")
            append(module.versionCode)
            append("), author: ")
            append(module.author)
            append("\n")
            append("   enabled: ")
            append(module.enabled)
            append(", update: ")
            append(module.update)
            append(", remove: ")
            append(module.remove)
            append(", webui: ")
            append(module.hasWebUi)
            append(", action: ")
            append(module.hasActionScript)
            append(", metamodule: ")
            append(module.metamodule)
            if (module.description.isNotBlank()) {
                append("\n   description: ")
                append(module.description.take(MAX_MODULE_DESCRIPTION_CHARS))
            }
            append("\n")
        }
    }
}

private fun AiAttachment.displayLabel(): String {
    return "${name.take(28)} · ${formatBytes(sizeBytes)}"
}

private fun formatBytes(bytes: Long): String {
    val size = max(bytes, 0L).toDouble()
    return when {
        size >= 1024 * 1024 -> "%.1f MB".format(size / 1024 / 1024)
        size >= 1024 -> "%.1f KB".format(size / 1024)
        else -> "${bytes} B"
    }
}

private fun loadAiChatHistory(prefs: android.content.SharedPreferences): List<AiMessage> {
    val text = prefs.getString(AI_CHAT_HISTORY, null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(text)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val role = runCatching { AiRole.valueOf(item.optString("role")) }.getOrNull() ?: continue
                add(
                    AiMessage(
                        id = item.optLong("id", index + 1L),
                        role = role,
                        text = item.optString("text"),
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun saveAiChatHistory(
    prefs: android.content.SharedPreferences,
    messages: List<AiMessage>,
) {
    val array = JSONArray()
    messages
        .takeLast(MAX_SAVED_MESSAGES)
        .filter { it.attachments.isEmpty() }
        .forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("role", message.role.name)
                    .put("text", message.text)
            )
        }
    prefs.edit().putString(AI_CHAT_HISTORY, array.toString()).apply()
}

private data class AiApiConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val systemPrompt: String,
) {
    fun isValid(): Boolean = baseUrl.isNotBlank() && model.isNotBlank()
}

private data class AiMessage(
    val id: Long,
    val role: AiRole,
    val text: String,
    val attachments: List<AiAttachment> = emptyList(),
)

private enum class AiRole {
    User,
    Assistant,
    Error,
}

private sealed class AiAttachment {
    abstract val name: String
    abstract val sizeBytes: Long

    data class TextFile(
        override val name: String,
        override val sizeBytes: Long,
        val content: String,
        val truncated: Boolean,
    ) : AiAttachment()

    data class ImageFile(
        override val name: String,
        override val sizeBytes: Long,
        val mimeType: String,
        val base64: String,
    ) : AiAttachment() {
        fun dataUrl(): String = "data:$mimeType;base64,$base64"
    }
}

private class ImageTooLargeException : IOException()

private const val AI_CHAT_PREFS = "ai_chat"
private const val AI_CHAT_BASE_URL = "base_url"
private const val AI_CHAT_API_KEY = "api_key"
private const val AI_CHAT_MODEL = "model"
private const val AI_CHAT_SYSTEM_PROMPT = "system_prompt"
private const val AI_CHAT_HISTORY = "history"
private const val DEFAULT_AI_BASE_URL = "https://api.openai.com/v1"
private const val DEFAULT_AI_MODEL = "gpt-4o-mini"
private const val MAX_HISTORY_MESSAGES = 12
private const val MAX_SAVED_MESSAGES = 40
private const val MAX_TEXT_ATTACHMENT_BYTES = 192 * 1024
private const val MAX_TEXT_ATTACHMENT_CHARS = 80_000
private const val MAX_IMAGE_ATTACHMENT_BYTES = 3 * 1024 * 1024
private const val MAX_ERROR_BODY_CHARS = 4000
private const val MAX_MODULE_DESCRIPTION_CHARS = 240
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
