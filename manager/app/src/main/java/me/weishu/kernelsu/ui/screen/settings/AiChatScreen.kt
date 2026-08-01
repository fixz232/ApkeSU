package me.weishu.kernelsu.ui.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.markdown.GithubMarkdown
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.navigation3.Route
import java.text.DateFormat
import java.util.Date

@Composable
fun AiChatScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val viewModel = viewModel<AiChatViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = state.activeConversation.messages
    val listState = rememberLazyListState()
    var conversationMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var actionMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var renameDialogVisible by rememberSaveable { mutableStateOf(false) }
    var renameValue by rememberSaveable { mutableStateOf("") }
    var autoFollow by remember { mutableStateOf(true) }
    val defaultConversationTitle = stringResource(R.string.ai_chat_default_conversation_title)

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importDocument)
    }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importImage)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            listState.isScrollInProgress to (lastVisible >= layout.totalItemsCount - 2)
        }.collect { (scrolling, nearBottom) ->
            if (scrolling) autoFollow = nearBottom
        }
    }
    LaunchedEffect(messages.lastOrNull()?.id, messages.lastOrNull()?.text?.length) {
        if (autoFollow && messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = immersivePageColor(
            MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
        ),
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.ai_chat_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = state.activeConversation.displayTitle(defaultConversationTitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.ai_chat_back),
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { conversationMenuExpanded = true }) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = stringResource(R.string.ai_chat_conversations),
                            )
                        }
                        DropdownMenu(
                            expanded = conversationMenuExpanded,
                            onDismissRequest = { conversationMenuExpanded = false },
                        ) {
                            state.conversations.forEach { conversation ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            conversation.displayTitle(defaultConversationTitle),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (conversation.id == state.activeConversationId) {
                                                Icons.Rounded.CheckCircle
                                            } else {
                                                Icons.Rounded.ChatBubbleOutline
                                            },
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        conversationMenuExpanded = false
                                        viewModel.selectConversation(conversation.id)
                                    },
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ai_chat_new_conversation)) },
                                leadingIcon = { Icon(Icons.Rounded.AddComment, contentDescription = null) },
                                onClick = {
                                    conversationMenuExpanded = false
                                    viewModel.newConversation()
                                },
                            )
                        }
                    }
                    IconButton(onClick = viewModel::newConversation, enabled = !state.isSending) {
                        Icon(
                            Icons.Rounded.AddComment,
                            contentDescription = stringResource(R.string.ai_chat_new_conversation),
                        )
                    }
                    Box {
                        IconButton(onClick = { actionMenuExpanded = true }) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.ai_chat_more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = actionMenuExpanded,
                            onDismissRequest = { actionMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ai_module_studio_title)) },
                                leadingIcon = { Icon(Icons.Rounded.Build, contentDescription = null) },
                                onClick = {
                                    actionMenuExpanded = false
                                    navigator.push(Route.AiModuleStudio)
                                },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ai_chat_rename)) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    actionMenuExpanded = false
                                    renameValue = state.activeConversation.displayTitle(defaultConversationTitle)
                                    renameDialogVisible = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ai_chat_clear)) },
                                leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                                enabled = messages.isNotEmpty() && !state.isSending,
                                onClick = {
                                    actionMenuExpanded = false
                                    viewModel.requestClearConversation()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ai_chat_delete_conversation)) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                enabled = !state.isSending,
                                onClick = {
                                    actionMenuExpanded = false
                                    viewModel.requestDeleteConversation()
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = immersiveTopBarColor(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    ),
                    scrolledContainerColor = immersiveScrolledTopBarColor(
                        MaterialTheme.colorScheme.surface,
                    ),
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
                state = state,
                onExpandedChange = viewModel::setConfigExpanded,
                onProviderChange = viewModel::selectProvider,
                onConfigChange = { updated -> viewModel.updateConfig { updated } },
                onSave = viewModel::saveConfig,
                onTest = viewModel::testConnection,
                onLoadModels = viewModel::loadModels,
            )

            if (messages.isEmpty()) {
                AiChatEmptyState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.52f)),
                    configured = state.config.isValid(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.52f)),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(messages, key = AiMessage::id) { message ->
                        AiMessageBubble(
                            message = message,
                            sending = state.isSending,
                            onCopy = { copyToClipboard(context, message.text) },
                            onCopyCode = { copyToClipboard(context, it) },
                            onRetry = { viewModel.retry(message.id) },
                        )
                    }
                }
            }

            AiChatInputBar(
                state = state,
                onInputChange = viewModel::updateDraft,
                onRemoveAttachment = viewModel::removePendingAttachment,
                onAttachFile = { fileLauncher.launch(arrayOf("*/*")) },
                onAttachImage = { imageLauncher.launch(arrayOf("image/*")) },
                onAnalyzeModules = viewModel::prepareModuleAnalysis,
                onStop = viewModel::stopGeneration,
                onSend = viewModel::requestSend,
            )
        }
    }

    state.pendingTransmission?.let { pending ->
        TransmissionConfirmationDialog(
            pending = pending,
            onConfirm = viewModel::confirmTransmission,
            onDismiss = viewModel::cancelTransmission,
        )
    }
    if (state.showClearConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.ai_chat_clear_title),
            text = stringResource(R.string.ai_chat_clear_message),
            confirmText = stringResource(R.string.ai_chat_clear),
            onConfirm = viewModel::clearConversation,
            onDismiss = viewModel::dismissClearConversation,
        )
    }
    if (state.showDeleteConversationConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.ai_chat_delete_conversation),
            text = stringResource(R.string.ai_chat_delete_conversation_message),
            confirmText = stringResource(R.string.ai_chat_delete),
            onConfirm = viewModel::deleteConversation,
            onDismiss = viewModel::dismissDeleteConversation,
        )
    }
    if (renameDialogVisible) {
        AlertDialog(
            onDismissRequest = { renameDialogVisible = false },
            title = { Text(stringResource(R.string.ai_chat_rename)) },
            text = {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = renameValue,
                    onValueChange = { renameValue = it.take(32) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.ai_chat_conversation_name)) },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameValue.isNotBlank(),
                    onClick = {
                        viewModel.renameActiveConversation(renameValue)
                        renameDialogVisible = false
                    },
                ) { Text(stringResource(R.string.ai_chat_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogVisible = false }) {
                    Text(stringResource(R.string.ai_chat_cancel))
                }
            },
        )
    }
}

@Composable
private fun AiApiConfigCard(
    state: AiChatUiState,
    onExpandedChange: (Boolean) -> Unit,
    onProviderChange: (AiProviderPreset) -> Unit,
    onConfigChange: (AiApiConfig) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onLoadModels: () -> Unit,
) {
    val config = state.config
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var keyVisible by rememberSaveable { mutableStateOf(false) }
    var modelMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var outputLimitText by rememberSaveable(config.maxOutputTokens) {
        mutableStateOf(config.maxOutputTokens.toString())
    }
    var contextLimitText by rememberSaveable(config.contextWindowTokens) {
        mutableStateOf(config.contextWindowTokens.toString())
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, end = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Text(
                    stringResource(R.string.ai_chat_config_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (config.isValid()) {
                        "${resolveAiTargetHost(config.baseUrl)} · ${config.model}"
                    } else {
                        stringResource(R.string.ai_chat_config_required)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { onExpandedChange(!state.configExpanded) }) {
                Icon(
                    if (state.configExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(
                        if (state.configExpanded) R.string.ai_chat_hide_config else R.string.ai_chat_edit_config
                    ),
                )
            }
        }

        if (state.configExpanded) {
            HorizontalDivider()
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AiProviderPreset.entries.forEach { provider ->
                        FilterChip(
                            selected = config.provider == provider,
                            onClick = { onProviderChange(provider) },
                            label = { Text(provider.displayName()) },
                        )
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = config.baseUrl,
                    onValueChange = { value ->
                        val newBaseUrl = value.take(2048)
                        val hostChanged = resolveAiTargetHost(config.baseUrl) != resolveAiTargetHost(newBaseUrl)
                        onConfigChange(
                            config.copy(
                                baseUrl = newBaseUrl,
                                apiKey = if (hostChanged) "" else config.apiKey,
                                customHeaders = if (hostChanged) "" else config.customHeaders,
                            )
                        )
                    },
                    label = { Text(stringResource(R.string.ai_chat_api_base)) },
                    supportingText = { Text(stringResource(R.string.ai_chat_api_base_hint)) },
                    singleLine = true,
                    enabled = !state.isSending,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = config.apiKey,
                    onValueChange = { onConfigChange(config.copy(apiKey = it.take(4096))) },
                    label = { Text(stringResource(R.string.ai_chat_api_key)) },
                    singleLine = true,
                    enabled = !state.isSending,
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                if (keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = stringResource(
                                    if (keyVisible) R.string.ai_chat_hide_key else R.string.ai_chat_show_key
                                ),
                            )
                        }
                    },
                )
                Box {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = config.model,
                        onValueChange = { onConfigChange(config.copy(model = it.take(256))) },
                        label = { Text(stringResource(R.string.ai_chat_model)) },
                        singleLine = true,
                        enabled = !state.isSending,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.availableModels.isNotEmpty()) {
                                    IconButton(onClick = { modelMenuExpanded = true }) {
                                        Icon(Icons.Rounded.ExpandMore, contentDescription = null)
                                    }
                                }
                                IconButton(
                                    onClick = onLoadModels,
                                    enabled = !state.loadingModels && !state.testingApi && !state.isSending,
                                ) {
                                    if (state.loadingModels) {
                                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(
                                            Icons.Rounded.Refresh,
                                            contentDescription = stringResource(R.string.ai_chat_load_models),
                                        )
                                    }
                                }
                            }
                        },
                    )
                    DropdownMenu(
                        expanded = modelMenuExpanded,
                        onDismissRequest = { modelMenuExpanded = false },
                    ) {
                        state.availableModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = {
                                    onConfigChange(config.copy(model = model))
                                    modelMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                TextButton(onClick = { advancedExpanded = !advancedExpanded }) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.ai_chat_advanced_config))
                    Spacer(Modifier.size(4.dp))
                    Icon(
                        if (advancedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (advancedExpanded) {
                    Text(
                        stringResource(R.string.ai_chat_temperature, config.temperature),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Slider(
                        value = config.temperature,
                        onValueChange = { onConfigChange(config.copy(temperature = it)) },
                        valueRange = 0f..2f,
                        steps = 19,
                        enabled = !state.isSending,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = outputLimitText,
                            onValueChange = { value ->
                                outputLimitText = value.filter(Char::isDigit).take(6)
                                outputLimitText.toIntOrNull()?.let {
                                    onConfigChange(config.copy(maxOutputTokens = it))
                                }
                            },
                            label = { Text(stringResource(R.string.ai_chat_output_tokens)) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            ),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = contextLimitText,
                            onValueChange = { value ->
                                contextLimitText = value.filter(Char::isDigit).take(7)
                                contextLimitText.toIntOrNull()?.let {
                                    onConfigChange(config.copy(contextWindowTokens = it))
                                }
                            },
                            label = { Text(stringResource(R.string.ai_chat_context_tokens)) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            ),
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 92.dp),
                        value = config.customHeaders,
                        onValueChange = { onConfigChange(config.copy(customHeaders = it.take(8000))) },
                        label = { Text(stringResource(R.string.ai_chat_custom_headers)) },
                        supportingText = { Text(stringResource(R.string.ai_chat_custom_headers_hint)) },
                        minLines = 2,
                        maxLines = 5,
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 112.dp),
                        value = config.systemPrompt,
                        onValueChange = { onConfigChange(config.copy(systemPrompt = it.take(16000))) },
                        label = { Text(stringResource(R.string.ai_chat_system_prompt)) },
                        minLines = 3,
                        maxLines = 7,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        modifier = Modifier.padding(start = 7.dp),
                        text = stringResource(R.string.ai_chat_secure_storage_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                state.configNotice?.let { notice ->
                    Text(
                        text = notice,
                        color = if (state.configNoticeIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (state.configDirty) {
                    Text(
                        text = stringResource(R.string.ai_chat_unsaved_config),
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        enabled = !state.testingApi && !state.loadingModels && !state.isSending,
                        onClick = onTest,
                    ) {
                        if (state.testingApi) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Rounded.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.ai_chat_test_config))
                    }
                    Button(onClick = onSave, enabled = !state.isSending) {
                        Text(stringResource(R.string.ai_chat_save_config))
                    }
                }
            }
        }
    }
}

@Composable
private fun AiChatEmptyState(modifier: Modifier, configured: Boolean) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = if (configured) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (configured) Icons.Rounded.AutoAwesome else Icons.Rounded.Key,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (configured) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                )
            }
        }
        Text(
            modifier = Modifier.padding(top = 18.dp),
            text = stringResource(
                if (configured) R.string.ai_chat_empty_title else R.string.ai_chat_setup_title
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .widthIn(max = 360.dp),
            text = stringResource(
                if (configured) R.string.ai_chat_empty_ready else R.string.ai_chat_welcome
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AiMessageBubble(
    message: AiMessage,
    sending: Boolean,
    onCopy: () -> Unit,
    onCopyCode: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val isUser = message.role == AiRole.User
    val isError = message.status == AiMessageStatus.Error
    val horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    val roleLabel = stringResource(
        if (isUser) R.string.ai_chat_role_user else R.string.ai_chat_role_assistant
    )
    val timeLabel = remember(message.createdAt) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.createdAt))
    }
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }
    val bubbleColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val messageColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        if (isUser) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            AiMessageAvatar(isUser = false)
            Spacer(modifier = Modifier.size(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .widthIn(max = if (isUser) 560.dp else 640.dp),
            horizontalAlignment = horizontalAlignment,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isUser) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(7.dp))
                }
                Text(
                    text = roleLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!isUser) {
                    Spacer(Modifier.size(7.dp))
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .widthIn(max = if (isUser) 560.dp else 640.dp),
                shape = bubbleShape,
                color = bubbleColor,
                tonalElevation = if (isUser) 0.dp else 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    if (message.retryOfId != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = stringResource(R.string.ai_chat_regenerated_answer),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    when {
                        message.status == AiMessageStatus.Generating && message.text.isBlank() -> {
                            AiGeneratingIndicator()
                        }
                        !isUser && message.status == AiMessageStatus.Completed -> {
                            GithubMarkdown(
                                content = message.text,
                                isMarkdown = true,
                                containerColor = Color.Transparent,
                                allowRemoteContent = false,
                                contentPaddingDp = 0,
                            )
                        }
                        else -> SelectionContainer {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = messageColor,
                            )
                        }
                    }
                    if (message.status == AiMessageStatus.Generating && message.text.isNotBlank()) {
                        AiGeneratingIndicator()
                    }
                    AttachmentChips(message.attachments)
                    if (!isUser) {
                        MessageStatus(message)
                        CodeBlockActions(message.text, onCopyCode)
                    }
                }
            }

            if (!isUser && message.text.isNotBlank() && message.status != AiMessageStatus.Generating) {
                Surface(
                    modifier = Modifier.padding(top = 5.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = onCopy) {
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.ai_chat_copy),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(enabled = !sending, onClick = onRetry) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.ai_chat_retry),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.size(8.dp))
            AiMessageAvatar(isUser = true)
        }
    }
}

@Composable
private fun AiMessageAvatar(isUser: Boolean) {
    Surface(
        modifier = Modifier.size(34.dp),
        shape = CircleShape,
        color = if (isUser) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isUser) Icons.Rounded.Person else Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                },
            )
        }
    }
}

@Composable
private fun AiGeneratingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 1.5.dp,
        )
        Text(
            modifier = Modifier.padding(start = 7.dp),
            text = stringResource(R.string.ai_chat_generating),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MessageStatus(message: AiMessage) {
    val statusText = when (message.status) {
        AiMessageStatus.Ready,
        AiMessageStatus.Completed,
        AiMessageStatus.Generating,
        -> null
        AiMessageStatus.Stopped -> stringResource(R.string.ai_chat_stopped)
        AiMessageStatus.Partial -> stringResource(R.string.ai_chat_partial_response)
        AiMessageStatus.Error -> stringResource(R.string.ai_chat_failed_response)
    }
    if (statusText != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (message.status == AiMessageStatus.Stopped) Icons.Rounded.StopCircle else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (message.status == AiMessageStatus.Stopped) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = buildString {
                    append(statusText)
                    message.errorDetail?.takeIf { it.isNotBlank() }?.let { append(" ").append(it) }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val details = buildList {
        message.usage?.let { usage ->
            val input = usage.inputTokens
            val output = usage.outputTokens
            if (input != null || output != null) add("${input ?: "?"} in / ${output ?: "?"} out")
        }
        if (message.droppedContextMessages > 0) {
            add(
                pluralStringResource(
                    R.plurals.ai_chat_context_dropped,
                    message.droppedContextMessages,
                    message.droppedContextMessages,
                )
            )
        }
    }
    if (details.isNotEmpty()) {
        Text(
            details.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CodeBlockActions(markdown: String, onCopy: (String) -> Unit) {
    val blocks = remember(markdown) { extractCodeBlocks(markdown).take(4) }
    if (blocks.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        blocks.forEachIndexed { index, block ->
            OutlinedButton(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                onClick = { onCopy(block.code) },
            ) {
                Icon(
                    Icons.Rounded.Code,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    block.language.ifBlank { stringResource(R.string.ai_chat_code_block, index + 1) },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AiChatInputBar(
    state: AiChatUiState,
    onInputChange: (String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onAttachFile: () -> Unit,
    onAttachImage: () -> Unit,
    onAnalyzeModules: () -> Unit,
    onStop: () -> Unit,
    onSend: () -> Unit,
) {
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    val tokenEstimate = remember(
        state.config,
        state.activeConversation.messages,
        state.draft,
        state.pendingAttachments,
    ) {
        val draftMessage = if (state.draft.isNotBlank() || state.pendingAttachments.isNotEmpty()) {
            AiMessage(
                id = Long.MAX_VALUE,
                role = AiRole.User,
                text = state.draft.ifBlank { "Please analyze the attached content." },
                status = AiMessageStatus.Ready,
                attachments = state.pendingAttachments,
            )
        } else {
            null
        }
        AiContextBuilder.estimateConversationTokens(
            state.config,
            state.activeConversation.messages + listOfNotNull(draftMessage),
        )
    }
    Surface(
        tonalElevation = 2.dp,
        color = immersiveSurfaceColor(
            defaultColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            darkAlpha = 0.70f,
            lightAlpha = 0.76f,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            PendingAttachmentRow(state.pendingAttachments, onRemoveAttachment)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        text = stringResource(
                            R.string.ai_chat_context_usage,
                            tokenEstimate,
                            state.config.contextWindowTokens,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (state.importingAttachment || state.analyzingModules) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box {
                    FilledTonalIconButton(
                        onClick = { toolsExpanded = true },
                        enabled = !state.isSending && !state.importingAttachment,
                    ) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = stringResource(R.string.ai_chat_tools))
                    }
                    DropdownMenu(
                        expanded = toolsExpanded,
                        onDismissRequest = { toolsExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ai_chat_attach_file)) },
                            leadingIcon = { Icon(Icons.Rounded.AttachFile, contentDescription = null) },
                            onClick = {
                                toolsExpanded = false
                                onAttachFile()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ai_chat_attach_image)) },
                            leadingIcon = { Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null) },
                            onClick = {
                                toolsExpanded = false
                                onAttachImage()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ai_chat_analyze_modules)) },
                            leadingIcon = { Icon(Icons.Rounded.Shield, contentDescription = null) },
                            enabled = !state.analyzingModules,
                            onClick = {
                                toolsExpanded = false
                                onAnalyzeModules()
                            },
                        )
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.draft,
                    onValueChange = onInputChange,
                    minLines = 1,
                    maxLines = 5,
                    placeholder = { Text(stringResource(R.string.ai_chat_input_hint)) },
                    enabled = !state.isSending,
                    shape = RoundedCornerShape(20.dp),
                )
                if (state.isSending) {
                    FilledIconButton(
                        onClick = onStop,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(
                            Icons.Rounded.StopCircle,
                            contentDescription = stringResource(R.string.ai_chat_stop),
                        )
                    }
                } else {
                    FilledIconButton(
                        onClick = onSend,
                        enabled = state.draft.isNotBlank() || state.pendingAttachments.isNotEmpty(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = stringResource(R.string.ai_chat_send),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingAttachmentRow(
    attachments: List<AiAttachment>,
    onRemove: (String) -> Unit,
) {
    if (attachments.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        attachments.forEach { attachment ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(start = 9.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (attachment.kind == AiAttachmentKind.Image) Icons.Rounded.Image else Icons.Rounded.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        modifier = Modifier.padding(start = 5.dp),
                        text = attachment.displayLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { onRemove(attachment.id) },
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.ai_chat_remove),
                            modifier = Modifier.size(16.dp),
                        )
                    }
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
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (attachment.kind == AiAttachmentKind.Image) Icons.Rounded.Image else Icons.Rounded.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        modifier = Modifier.padding(start = 5.dp),
                        text = attachment.displayLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransmissionConfirmationDialog(
    pending: AiPendingTransmission,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val files = pending.attachments.count { it.kind != AiAttachmentKind.Image }
    val images = pending.attachments.count { it.kind == AiAttachmentKind.Image }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Shield, contentDescription = null) },
        title = { Text(stringResource(R.string.ai_chat_confirm_send_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.ai_chat_send_destination, pending.targetHost),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(stringResource(R.string.ai_chat_external_api_warning))
                Text(
                    buildString {
                        append(stringResource(R.string.ai_chat_data_summary))
                        append(' ')
                        append(stringResource(R.string.ai_chat_data_text))
                        if (pending.sendsAuthentication) {
                            append(", ").append(stringResource(R.string.ai_chat_data_auth))
                        }
                        if (files > 0) {
                            append(", ").append(
                                pluralStringResource(R.plurals.ai_chat_data_files, files, files)
                            )
                        }
                        if (images > 0) {
                            append(", ").append(
                                pluralStringResource(R.plurals.ai_chat_data_images, images, images)
                            )
                        }
                        if (pending.containsModuleInventory) {
                            append(", ").append(stringResource(R.string.ai_chat_data_modules))
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    SelectionContainer {
                        Text(
                            modifier = Modifier.padding(10.dp),
                            text = buildTransmissionPreview(
                                pending,
                                stringResource(R.string.ai_chat_preview_truncated),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.ai_chat_confirm_send)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_chat_cancel)) }
        },
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_chat_cancel)) }
        },
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("ai-chat", text))
    Toast.makeText(context, R.string.ai_chat_copied, Toast.LENGTH_SHORT).show()
}

@Composable
private fun AiProviderPreset.displayName(): String = when (this) {
    AiProviderPreset.OpenAi -> stringResource(R.string.ai_chat_provider_openai)
    AiProviderPreset.DeepSeek -> stringResource(R.string.ai_chat_provider_deepseek)
    AiProviderPreset.Compatible -> stringResource(R.string.ai_chat_provider_compatible)
}

private fun AiConversation.displayTitle(defaultTitle: String): String =
    title.takeUnless { it.isBlank() || it == DEFAULT_CONVERSATION_TITLE } ?: defaultTitle

private fun AiAttachment.displayLabel(): String =
    "${name.take(26)} · ${formatBytes(sizeBytes)}"

private data class CodeBlock(val language: String, val code: String)

private fun extractCodeBlocks(markdown: String): List<CodeBlock> = CODE_BLOCK_REGEX
    .findAll(markdown)
    .map { match ->
        CodeBlock(
            language = match.groupValues[1].trim().take(20),
            code = match.groupValues[2].trimEnd(),
        )
    }
    .filter { it.code.isNotBlank() }
    .toList()

private fun buildTransmissionPreview(pending: AiPendingTransmission, truncatedLabel: String): String = buildString {
    append(pending.text.take(700))
    pending.attachments.forEach { attachment ->
        append("\n\n[").append(attachment.name).append("]")
        append(" ").append(attachment.mimeType).append(" / ").append(formatBytes(attachment.sizeBytes))
        if (attachment.extractedText.isNotBlank()) {
            append("\n").append(attachment.extractedText.take(1_200))
            if (attachment.extractedText.length > 1_200) append("\n...").append(truncatedLabel)
        }
    }
}.take(3_000)

private val CODE_BLOCK_REGEX = Regex("```([^\\n`]*)\\n([\\s\\S]*?)```", RegexOption.MULTILINE)
