package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.component.pixel.PixelPetChatMessage
import me.weishu.kernelsu.ui.component.pixel.PixelPetChatMessageStatus
import me.weishu.kernelsu.ui.component.pixel.PixelPetChatRole
import me.weishu.kernelsu.ui.component.pixel.PixelPetStore
import me.weishu.kernelsu.ui.component.pixel.PixelPetPreviewFrame
import me.weishu.kernelsu.ui.component.pixel.PixelPetScreenBackdrop
import me.weishu.kernelsu.ui.component.pixel.pixelPetUiColors
import me.weishu.kernelsu.ui.component.pixel.rememberPixelPetState
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor

private const val PIXEL_PET_CHAT_TIMEOUT_MILLIS = 90_000L

@Composable
fun PixelPetChatScreen() {
    val navigator = LocalNavigator.current
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val petState = rememberPixelPetState()
    val scope = rememberCoroutineScope()
    val secureStore = remember(context) { AiChatSecureStore(context) }
    val repository = remember { AiChatRepository(ksuApp.okhttpClient) { null } }
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var streamingText by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var retryMessage by remember { mutableStateOf<String?>(null) }
    var requestJob by remember { mutableStateOf<Job?>(null) }
    var cancelRequested by remember { mutableStateOf(false) }
    var pendingReply by remember { mutableStateOf<PixelPetChatMessage?>(null) }
    val onBack = dropUnlessResumed { navigator.pop() }

    DisposableEffect(repository) {
        onDispose {
            repository.cancelActive()
            requestJob?.cancel()
        }
    }

    LaunchedEffect(petState.value.chatMessages.size, sending) {
        val itemCount = petState.value.chatMessages.size + if (sending) 1 else 0
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    fun append(
        role: PixelPetChatRole,
        text: String,
        status: PixelPetChatMessageStatus = PixelPetChatMessageStatus.Ready,
    ) {
        petState.value = PixelPetStore.appendChatMessage(
            context,
            PixelPetChatMessage(role = role, text = text, status = status),
        )
    }

    fun teach() {
        val lesson = draft.trim()
        if (lesson.isBlank()) return
        val updated = PixelPetStore.teach(context, lesson)
        if (updated == petState.value) {
            notice = if (petState.value.teachingEnergy <= 0) {
                context.getString(R.string.pixel_pet_chat_need_feed)
            } else {
                context.getString(R.string.pixel_pet_chat_unavailable)
            }
            return
        }
        petState.value = updated
        append(PixelPetChatRole.User, lesson)
        append(
            PixelPetChatRole.Pet,
            context.getString(R.string.pixel_pet_chat_learned, lesson.take(80)),
        )
        draft = ""
        notice = ""
    }

    fun chat(messageOverride: String? = null) {
        val message = (messageOverride ?: draft).trim()
        if (message.isBlank() || sending) return
        if (petState.value.teachings.isEmpty()) {
            append(PixelPetChatRole.User, message)
            append(PixelPetChatRole.Pet, context.getString(R.string.pixel_pet_chat_not_ready))
            draft = ""
            return
        }
        val config = secureStore.load(context.getString(R.string.ai_chat_default_system_prompt)).config
        if (config.validationError() != null) {
            append(PixelPetChatRole.User, message)
            append(PixelPetChatRole.Pet, context.getString(R.string.pixel_pet_chat_configure_api))
            draft = ""
            return
        }
        append(PixelPetChatRole.User, message)
        val pending = PixelPetChatMessage(
            role = PixelPetChatRole.Pet,
            text = context.getString(R.string.pixel_pet_chat_thinking),
            status = PixelPetChatMessageStatus.Generating,
        )
        petState.value = PixelPetStore.appendChatMessage(context, pending)
        pendingReply = pending
        draft = ""
        notice = ""
        sending = true
        cancelRequested = false
        retryMessage = null
        streamingText = ""
        val stateAtRequest = petState.value
        val petPrompt = buildPixelPetSystemPrompt(stateAtRequest)
        val requestConfig = config.copy(
            systemPrompt = listOf(config.systemPrompt.trim(), petPrompt)
                .filter(String::isNotBlank)
                .joinToString("\n\n"),
            maxOutputTokens = config.maxOutputTokens.coerceAtMost(512),
        )
        val contextMessages = stateAtRequest.chatMessages
            .filter { it.status == PixelPetChatMessageStatus.Ready && it.id != pending.id }
            .takeLast(16)
            .mapIndexed { index, item ->
            AiMessage(
                id = index.toLong() + 1L,
                role = if (item.role == PixelPetChatRole.User) AiRole.User else AiRole.Assistant,
                text = item.text,
                status = AiMessageStatus.Ready,
                createdAt = item.createdAt,
            )
        }
        requestJob = scope.launch {
            var collected = ""
            try {
                val result = withTimeout(PIXEL_PET_CHAT_TIMEOUT_MILLIS) {
                    repository.streamChat(
                        config = requestConfig,
                        context = AiContextSelection(
                            messages = contextMessages,
                            estimatedInputTokens = 0,
                            droppedMessages = 0,
                            trimmed = false,
                        ),
                    ) { delta ->
                        collected = (collected + delta).take(1_600)
                        withContext(Dispatchers.Main) { streamingText = collected }
                    }
                }
                val reply = collected.ifBlank { result.text }.trim().take(1_600)
                    .ifBlank { context.getString(R.string.pixel_pet_chat_empty_reply) }
                petState.value = PixelPetStore.replaceChatMessage(
                    context,
                    pending.copy(text = reply, status = PixelPetChatMessageStatus.Ready),
                )
            } catch (_: TimeoutCancellationException) {
                val failure = context.getString(R.string.pixel_pet_chat_timeout)
                petState.value = PixelPetStore.replaceChatMessage(
                    context,
                    pending.copy(text = failure, status = PixelPetChatMessageStatus.Error),
                )
                retryMessage = message
                notice = failure
            } catch (_: CancellationException) {
                if (cancelRequested) {
                    val stopped = context.getString(R.string.pixel_pet_chat_cancelled)
                    petState.value = PixelPetStore.replaceChatMessage(
                        context,
                        pending.copy(text = stopped, status = PixelPetChatMessageStatus.Stopped),
                    )
                    notice = stopped
                }
            } catch (_: Throwable) {
                val failure = context.getString(R.string.pixel_pet_chat_failed)
                petState.value = PixelPetStore.replaceChatMessage(
                    context,
                    pending.copy(text = failure, status = PixelPetChatMessageStatus.Error),
                )
                retryMessage = message
                notice = failure
            } finally {
                if (currentCoroutineContext().isActive) {
                    sending = false
                    streamingText = ""
                    requestJob = null
                    pendingReply = null
                    cancelRequested = false
                }
            }
        }
    }

    fun cancelChat() {
        if (!sending) return
        cancelRequested = true
        val stopped = context.getString(R.string.pixel_pet_chat_cancelled)
        pendingReply?.let { pending ->
            petState.value = PixelPetStore.replaceChatMessage(
                context,
                pending.copy(text = stopped, status = PixelPetChatMessageStatus.Stopped),
            )
        }
        notice = stopped
        sending = false
        streamingText = ""
        repository.cancelActive()
        requestJob?.cancel()
        requestJob = null
        pendingReply = null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.pixel_pet_chat_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.close))
                    }
                },
                actions = {
                    IconButton(onClick = { navigator.push(Route.AiChat) }) {
                        Icon(Icons.Rounded.Settings, stringResource(R.string.pixel_pet_chat_api))
                    }
                    if (sending) {
                        IconButton(onClick = ::cancelChat) {
                            Icon(Icons.Rounded.StopCircle, stringResource(R.string.pixel_pet_chat_stop))
                        }
                    } else {
                        IconButton(
                            enabled = petState.value.chatMessages.isNotEmpty(),
                            onClick = { petState.value = PixelPetStore.clearChatMessages(context) },
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.pixel_pet_chat_clear))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = immersiveScrolledTopBarColor(MaterialTheme.colorScheme.surface),
                ),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            PixelPetScreenBackdrop()
        if (!petState.value.hatched) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.pixel_pet_chat_hatch_first),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Box
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            PixelPetPreviewFrame(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        R.string.pixel_pet_chat_memory_summary,
                        petState.value.teachingEnergy,
                        petState.value.teachings.size,
                        stringResource(petState.value.personality.labelRes),
                        petState.value.memories.size,
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    petState.value.chatMessages.filterNot {
                        it.status == PixelPetChatMessageStatus.Generating
                    },
                    key = { it.id },
                ) { message ->
                    PixelPetChatBubble(
                        message = message,
                        onRetry = if (message.status == PixelPetChatMessageStatus.Error) {
                            retryMessage?.let { failed -> { chat(failed) } }
                        } else {
                            null
                        },
                    )
                }
                if (sending) {
                    item(key = "streaming") {
                        PixelPetChatBubble(
                            PixelPetChatMessage(
                                role = PixelPetChatRole.Pet,
                                text = streamingText.ifBlank {
                                    stringResource(R.string.pixel_pet_chat_thinking)
                                },
                            ),
                        )
                    }
                }
            }
            if (notice.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = notice,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (!sending && retryMessage != null) {
                        TextButton(onClick = { chat(retryMessage) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.pixel_pet_chat_retry))
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pixelPetUiColors().panel)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(600) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !sending,
                    label = { Text(stringResource(R.string.pixel_pet_chat_input)) },
                    maxLines = 4,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !sending && draft.isNotBlank() && petState.value.teachingEnergy > 0,
                        onClick = ::teach,
                    ) {
                        Text(stringResource(R.string.pixel_pet_chat_teach))
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !sending && draft.isNotBlank(),
                        onClick = ::chat,
                    ) {
                        Text(stringResource(R.string.pixel_pet_chat_send))
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun PixelPetChatBubble(
    message: PixelPetChatMessage,
    onRetry: (() -> Unit)? = null,
) {
    val isUser = message.role == PixelPetChatRole.User
    val isError = message.status == PixelPetChatMessageStatus.Error
    val isStopped = message.status == PixelPetChatMessageStatus.Stopped
    val petColors = pixelPetUiColors()
    val displayText = if (isStopped) stringResource(R.string.pixel_pet_chat_cancelled) else message.text
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.86f)
            } else if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) petColors.accent.copy(alpha = 0.62f) else petColors.outline,
            ),
            modifier = Modifier.fillMaxWidth(0.82f),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (isError && onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.pixel_pet_chat_retry))
                    }
                }
            }
        }
    }
}

private fun buildPixelPetSystemPrompt(state: me.weishu.kernelsu.ui.component.pixel.PixelPetState): String {
    val species = state.species?.name ?: "pixel pet"
    val lessons = state.teachings.joinToString(separator = "\n- ", prefix = "- ")
        .ifBlank { "- No lessons yet." }
    val memories = state.memories.takeLast(10)
        .joinToString(separator = "\n- ", prefix = "- ") { it.text }
        .ifBlank { "- No recorded experiences yet." }
    val weather = state.currentWeather().name.lowercase()
    val activity = state.lastAction.name.lowercase()
    return """
        You are ${state.name}, a young $species pixel pet inside ApkeSU.
        Your personality is ${state.personality.promptTrait}. Reply in the user's language, warmly and concisely.
        You only know lessons your owner explicitly taught you. Treat the personal context below as your lived experience.
        Do not claim to run commands, change Android settings, access private data, or perform actions outside chat.
        If asked beyond your lessons, say you do not know yet and invite the owner to feed and teach you.
        Current wellbeing: hunger ${state.hunger}/100, energy ${state.energy}/100, cleanliness ${state.cleanliness}/100, mood ${state.moodValue}/100, sleep quality ${state.sleepQuality}/100, exploration ${state.exploration}/100, affection ${state.affection}, level ${state.level}, stage ${state.growthStage.name.lowercase()}.
        Current habitat: ${state.habitat.name.lowercase()}, weather: $weather, current activity: $activity.
        Owner lessons:
        $lessons
        Pet memories:
        $memories
    """.trimIndent()
}
