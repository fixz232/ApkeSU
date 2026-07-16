package me.weishu.kernelsu.ui.screen.settings

import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil
import kotlin.math.max

internal data class AiContextSelection(
    val messages: List<AiMessage>,
    val estimatedInputTokens: Int,
    val droppedMessages: Int,
    val trimmed: Boolean,
)

internal data class AiStreamResult(
    val text: String,
    val usage: AiTokenUsage?,
    val finishReason: String?,
    val requestedTools: Set<String>,
)

internal object AiContextBuilder {
    fun select(
        config: AiApiConfig,
        allMessages: List<AiMessage>,
        throughMessageId: Long? = null,
    ): AiContextSelection {
        val boundedMessages = if (throughMessageId == null) {
            allMessages
        } else {
            val index = allMessages.indexOfFirst { it.id == throughMessageId }
            if (index >= 0) allMessages.take(index + 1) else allMessages
        }
        val eligible = boundedMessages.filter { message ->
            when (message.role) {
                AiRole.User -> true
                AiRole.Assistant -> message.text.isNotBlank() && message.status in setOf(
                    AiMessageStatus.Completed,
                    AiMessageStatus.Partial,
                    AiMessageStatus.Stopped,
                )
            }
        }
        val systemTokens = estimateAiTokens(config.systemPrompt)
        val budget = max(
            MIN_CONTEXT_BODY_TOKENS,
            config.contextWindowTokens - config.maxOutputTokens - CONTEXT_RESERVE_TOKENS - systemTokens,
        )
        val selectedReversed = mutableListOf<AiMessage>()
        var used = 0
        var trimmed = false
        for (message in eligible.asReversed()) {
            val estimate = estimateMessageTokens(message)
            if (used + estimate <= budget) {
                selectedReversed += message
                used += estimate
                continue
            }
            if (selectedReversed.isEmpty()) {
                val trimmedMessage = trimMessageToBudget(message, budget)
                selectedReversed += trimmedMessage
                used += estimateMessageTokens(trimmedMessage)
                trimmed = true
            }
            break
        }
        val selected = selectedReversed.asReversed().dropWhile { it.role == AiRole.Assistant }
        val dropped = (eligible.size - selected.size).coerceAtLeast(0)
        return AiContextSelection(
            messages = selected,
            estimatedInputTokens = selected.sumOf(::estimateMessageTokens) + systemTokens,
            droppedMessages = dropped,
            trimmed = trimmed,
        )
    }

    fun estimateConversationTokens(config: AiApiConfig, messages: List<AiMessage>): Int =
        select(config, messages).estimatedInputTokens

    private fun trimMessageToBudget(message: AiMessage, tokenBudget: Int): AiMessage {
        val availableTokens = (tokenBudget - MESSAGE_OVERHEAD_TOKENS).coerceAtLeast(64)
        val maxImages = (availableTokens / IMAGE_TOKEN_ESTIMATE).coerceAtMost(MAX_IMAGES_PER_REQUEST)
        val images = message.attachments
            .filter { it.kind == AiAttachmentKind.Image }
            .take(maxImages)
        var remainingTokens = (availableTokens - images.size * IMAGE_TOKEN_ESTIMATE).coerceAtLeast(32)
        val text = truncateToTokenBudget(message.text, remainingTokens)
        remainingTokens = (remainingTokens - estimateAiTokens(text)).coerceAtLeast(0)
        val textAttachments = message.attachments.mapNotNull { attachment ->
            when (attachment.kind) {
                AiAttachmentKind.Image -> null
                AiAttachmentKind.Text,
                AiAttachmentKind.Archive,
                -> if (remainingTokens <= ATTACHMENT_OVERHEAD_TOKENS) {
                    null
                } else {
                    remainingTokens -= ATTACHMENT_OVERHEAD_TOKENS
                    val content = truncateToTokenBudget(attachment.extractedText, remainingTokens)
                    remainingTokens = (remainingTokens - estimateAiTokens(content)).coerceAtLeast(0)
                    attachment.copy(extractedText = content, truncated = true)
                }
            }
        }
        return message.copy(text = text, attachments = textAttachments + images)
    }
}

internal fun estimateAiTokens(text: String): Int {
    if (text.isBlank()) return 0
    var ascii = 0
    var nonAscii = 0
    text.forEach { character ->
        if (character.code < 128) ascii += 1 else nonAscii += 1
    }
    return ceil(ascii / 4.0 + nonAscii * 0.9).toInt().coerceAtLeast(1)
}

private fun estimateMessageTokens(message: AiMessage): Int {
    val textTokens = estimateAiTokens(message.text)
    val attachmentTokens = message.attachments.sumOf { attachment ->
        when (attachment.kind) {
            AiAttachmentKind.Image -> IMAGE_TOKEN_ESTIMATE
            AiAttachmentKind.Text,
            AiAttachmentKind.Archive,
            -> estimateAiTokens(attachment.extractedText) + ATTACHMENT_OVERHEAD_TOKENS
        }
    }
    return textTokens + attachmentTokens + MESSAGE_OVERHEAD_TOKENS
}

private fun truncateMiddleAware(text: String, maxChars: Int): String {
    if (text.length <= maxChars) return text
    if (maxChars < TRUNCATION_MARKER.length + 16) return text.take(maxChars)
    val head = (maxChars * 0.7f).toInt()
    val tail = maxChars - head - TRUNCATION_MARKER.length
    return text.take(head) + TRUNCATION_MARKER + text.takeLast(tail.coerceAtLeast(0))
}

private fun truncateToTokenBudget(text: String, tokenBudget: Int): String {
    val estimate = estimateAiTokens(text)
    if (estimate <= tokenBudget) return text
    if (tokenBudget <= 0) return ""
    val ratio = (tokenBudget.toDouble() / estimate.toDouble()).coerceIn(0.0, 1.0)
    val maxChars = (text.length * ratio * 0.92).toInt().coerceAtLeast(1)
    return truncateMiddleAware(text, maxChars)
}

internal class AiChatRepository(
    baseClient: OkHttpClient,
    private val imageLoader: (String) -> ByteArray?,
) {
    private val client = baseClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val activeCall = AtomicReference<Call?>(null)

    fun cancelActive() {
        activeCall.getAndSet(null)?.cancel()
    }

    suspend fun streamChat(
        config: AiApiConfig,
        context: AiContextSelection,
        onDelta: suspend (String) -> Unit,
    ): AiStreamResult = withContext(Dispatchers.IO) {
        val body = buildChatRequestBody(config, context.messages, stream = true)
        val request = createRequest(
            config = config,
            url = resolveChatEndpoint(config.baseUrl),
            body = body.toString(),
        )
        executeTracked(request) { response ->
            if (!response.isSuccessful) throw response.toApiException()
            val contentType = response.header("Content-Type").orEmpty()
            if (!contentType.contains("text/event-stream", ignoreCase = true)) {
                return@executeTracked parseChatCompletion(
                    response.body.readTextLimited(MAX_NON_STREAM_RESPONSE_CHARS)
                )
            }
            parseStream(response, onDelta)
        }
    }

    suspend fun testConnection(config: AiApiConfig): String = withContext(Dispatchers.IO) {
        val probe = AiMessage(
            id = 1L,
            role = AiRole.User,
            text = "Reply with OK.",
            status = AiMessageStatus.Ready,
        )
        val body = buildChatRequestBody(config, listOf(probe), stream = false)
            .put("max_tokens", 8)
            .toString()
        val request = createRequest(config, resolveChatEndpoint(config.baseUrl), body)
        executeTracked(request) { response ->
            if (!response.isSuccessful) throw response.toApiException()
            parseChatCompletion(response.body.readTextLimited(MAX_NON_STREAM_RESPONSE_CHARS)).text
        }
    }

    suspend fun fetchModels(config: AiApiConfig): List<String> = withContext(Dispatchers.IO) {
        val request = createRequest(
            config = config,
            url = resolveModelsEndpoint(config.baseUrl),
            body = null,
        )
        executeTracked(request) { response ->
            if (!response.isSuccessful) throw response.toApiException()
            val root = parseJsonOrThrow(response.body.readTextLimited(MAX_MODELS_RESPONSE_CHARS))
            root.throwIfApiError()
            val data = root.optJSONArray("data") ?: JSONArray()
            buildList {
                for (index in 0 until data.length()) {
                    data.optJSONObject(index)?.optString("id")
                        ?.trim()
                        ?.takeIf(String::isNotEmpty)
                        ?.let(::add)
                }
            }.distinct().sorted()
        }
    }

    private suspend fun parseStream(
        response: Response,
        onDelta: suspend (String) -> Unit,
    ): AiStreamResult {
        val parser = AiSseParser()
        val text = StringBuilder()
        var usage: AiTokenUsage? = null
        var finishReason: String? = null
        val tools = linkedSetOf<String>()

        suspend fun consume(events: List<AiStreamEvent>) {
            events.forEach { event ->
                currentCoroutineContext().ensureActive()
                when (event) {
                    is AiStreamEvent.Delta -> {
                        val remaining = MAX_STREAM_RESPONSE_CHARS - text.length
                        if (remaining <= 0) throw AiApiException("Response exceeded the safety limit")
                        val accepted = event.text.take(remaining)
                        text.append(accepted)
                        onDelta(accepted)
                        if (accepted.length < event.text.length) {
                            throw AiApiException("Response exceeded the safety limit")
                        }
                    }
                    is AiStreamEvent.Usage -> usage = event.value
                    is AiStreamEvent.Finished -> finishReason = event.reason
                    is AiStreamEvent.ToolRequested -> tools += event.name
                    is AiStreamEvent.Error -> throw AiApiException(event.message)
                    AiStreamEvent.Done -> Unit
                }
            }
        }

        val source = response.body.source()
        while (!source.exhausted()) {
            currentCoroutineContext().ensureActive()
            val line = source.readUtf8Line() ?: ""
            if (line.length > MAX_SSE_LINE_CHARS) throw AiApiException("SSE event is too large")
            consume(parser.acceptLine(line))
        }
        consume(parser.finish())
        if (text.isEmpty() && tools.isNotEmpty()) {
            throw AiApiException("Model requested unsupported tools: ${tools.joinToString()}")
        }
        return AiStreamResult(text.toString(), usage, finishReason, tools)
    }

    private fun buildChatRequestBody(
        config: AiApiConfig,
        messages: List<AiMessage>,
        stream: Boolean,
    ): JSONObject {
        val wireMessages = JSONArray()
        if (config.systemPrompt.isNotBlank()) {
            wireMessages.put(JSONObject().put("role", "system").put("content", config.systemPrompt))
        }
        messages.forEach { message ->
            wireMessages.put(
                JSONObject()
                    .put("role", if (message.role == AiRole.User) "user" else "assistant")
                    .put("content", buildMessageContent(message))
            )
        }
        return JSONObject()
            .put("model", config.model.trim())
            .put("messages", wireMessages)
            .put("temperature", config.temperature.toDouble())
            .put("max_tokens", config.maxOutputTokens)
            .put("stream", stream)
            .apply {
                if (stream) put("stream_options", JSONObject().put("include_usage", true))
            }
    }

    private fun buildMessageContent(message: AiMessage): Any {
        if (message.role == AiRole.Assistant) return message.text
        val loadedImages = message.attachments
            .filter { it.kind == AiAttachmentKind.Image }
            .take(MAX_IMAGES_PER_REQUEST)
            .map { attachment -> attachment to attachment.storageId?.let(imageLoader) }
        val text = buildString {
            append(message.text.ifBlank { "Please analyze the attached content." })
            message.attachments.filter { it.kind != AiAttachmentKind.Image }.forEach { attachment ->
                append("\n\n### Attachment: ").append(attachment.name)
                append("\nType: ").append(attachment.mimeType)
                if (attachment.truncated) append(" (safely truncated)")
                append("\n").append(attachment.extractedText)
            }
            val unavailableImages = loadedImages.filter { it.second == null }.map { it.first }
            if (unavailableImages.isNotEmpty()) {
                append("\n\nUnavailable local images: ")
                append(unavailableImages.joinToString { it.name })
            }
        }
        val images = loadedImages.mapNotNull { (attachment, bytes) ->
                bytes ?: return@mapNotNull null
                val data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                JSONObject()
                    .put("type", "image_url")
                    .put(
                        "image_url",
                        JSONObject().put("url", "data:${attachment.mimeType};base64,$data"),
                    )
            }
        if (images.isEmpty()) return text
        return JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", text))
            images.forEach(::put)
        }
    }

    private fun createRequest(config: AiApiConfig, url: HttpUrl, body: String?): Request {
        val builder = Request.Builder().url(url)
        if (body == null) {
            builder.get()
        } else {
            builder.post(body.toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")
        }
        if (config.apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer ${config.apiKey.trim()}")
        }
        runCatching { parseAiCustomHeaders(config.customHeaders) }
            .getOrElse { throw AiApiException(it.message ?: "Invalid custom headers") }
            .forEach { (name, value) -> builder.header(name, value) }
        return builder.build()
    }

    private suspend fun <T> executeTracked(request: Request, block: suspend (Response) -> T): T {
        currentCoroutineContext().ensureActive()
        val call = client.newCall(request)
        activeCall.getAndSet(call)?.cancel()
        val cancellation = currentCoroutineContext().job.invokeOnCompletion { cause ->
            if (cause is CancellationException) call.cancel()
        }
        return try {
            call.execute().use { response -> block(response) }
        } catch (error: IOException) {
            currentCoroutineContext().ensureActive()
            throw error
        } finally {
            cancellation.dispose()
            activeCall.compareAndSet(call, null)
        }
    }
}

internal sealed interface AiStreamEvent {
    data class Delta(val text: String) : AiStreamEvent
    data class Usage(val value: AiTokenUsage) : AiStreamEvent
    data class Finished(val reason: String?) : AiStreamEvent
    data class ToolRequested(val name: String) : AiStreamEvent
    data class Error(val message: String) : AiStreamEvent
    data object Done : AiStreamEvent
}

internal class AiSseParser {
    private val dataLines = mutableListOf<String>()

    fun acceptLine(rawLine: String): List<AiStreamEvent> {
        val line = rawLine.removeSuffix("\r")
        if (line.isBlank()) return dispatch()
        if (line.startsWith(":")) return emptyList()
        if (line.startsWith("data:")) {
            dataLines += line.removePrefix("data:").removePrefix(" ")
        }
        return emptyList()
    }

    fun finish(): List<AiStreamEvent> = dispatch()

    private fun dispatch(): List<AiStreamEvent> {
        if (dataLines.isEmpty()) return emptyList()
        val payload = dataLines.joinToString("\n").trim()
        dataLines.clear()
        if (payload == "[DONE]") return listOf(AiStreamEvent.Done)
        val root = runCatching { JSONObject(payload) }.getOrElse {
            return listOf(AiStreamEvent.Error("Invalid SSE payload"))
        }
        extractApiError(root)?.let { return listOf(AiStreamEvent.Error(it)) }
        val events = mutableListOf<AiStreamEvent>()
        root.optJSONObject("usage")?.toUsage()?.let { events += AiStreamEvent.Usage(it) }
        val choices = root.optJSONArray("choices") ?: return events
        for (index in 0 until choices.length()) {
            val choice = choices.optJSONObject(index) ?: continue
            val delta = choice.optJSONObject("delta")
            extractContent(delta?.opt("content"))
                .takeIf(String::isNotEmpty)
                ?.let { events += AiStreamEvent.Delta(it) }
            val toolCalls = delta?.optJSONArray("tool_calls")
            if (toolCalls != null) {
                for (toolIndex in 0 until toolCalls.length()) {
                    val name = toolCalls.optJSONObject(toolIndex)
                        ?.optJSONObject("function")
                        ?.optString("name")
                        .orEmpty()
                        .ifBlank { "unnamed_tool" }
                    events += AiStreamEvent.ToolRequested(name)
                }
            }
            if (!choice.isNull("finish_reason")) {
                events += AiStreamEvent.Finished(choice.optString("finish_reason").ifBlank { null })
            }
        }
        return events
    }
}

private fun parseChatCompletion(body: String): AiStreamResult {
    val root = parseJsonOrThrow(body)
    root.throwIfApiError()
    val choice = root.optJSONArray("choices")?.optJSONObject(0)
        ?: throw AiApiException("Response has no choices")
    val message = choice.optJSONObject("message")
        ?: throw AiApiException("Response has no message")
    val content = extractContent(message.opt("content"))
    val tools = buildSet {
        val toolCalls = message.optJSONArray("tool_calls") ?: return@buildSet
        for (index in 0 until toolCalls.length()) {
            add(
                toolCalls.optJSONObject(index)
                    ?.optJSONObject("function")
                    ?.optString("name")
                    .orEmpty()
                    .ifBlank { "unnamed_tool" }
            )
        }
    }
    if (content.isBlank() && tools.isNotEmpty()) {
        throw AiApiException("Model requested unsupported tools: ${tools.joinToString()}")
    }
    if (content.isBlank()) throw AiApiException("Response content is empty")
    return AiStreamResult(
        text = content,
        usage = root.optJSONObject("usage")?.toUsage(),
        finishReason = choice.optString("finish_reason").ifBlank { null },
        requestedTools = tools,
    )
}

private fun extractContent(value: Any?): String = when (value) {
    null, JSONObject.NULL -> ""
    is JSONArray -> buildString {
        for (index in 0 until value.length()) {
            val item = value.optJSONObject(index) ?: continue
            val text = item.optString("text")
            if (text.isNotEmpty()) append(text)
        }
    }
    else -> value.toString()
}

private fun JSONObject.toUsage(): AiTokenUsage = AiTokenUsage(
    inputTokens = optInt("prompt_tokens").takeIf { has("prompt_tokens") },
    outputTokens = optInt("completion_tokens").takeIf { has("completion_tokens") },
    totalTokens = optInt("total_tokens").takeIf { has("total_tokens") },
)

private fun parseJsonOrThrow(body: String): JSONObject = runCatching { JSONObject(body) }
    .getOrElse { throw AiApiException("Invalid JSON response") }

private fun JSONObject.throwIfApiError() {
    extractApiError(this)?.let { throw AiApiException(it) }
}

private fun extractApiError(root: JSONObject): String? {
    val error = root.opt("error") ?: return null
    return when (error) {
        is JSONObject -> error.optString("message")
            .ifBlank { error.optString("code") }
            .ifBlank { "API returned an error" }
        JSONObject.NULL -> null
        else -> error.toString().take(MAX_ERROR_MESSAGE_CHARS)
    }
}

private fun Response.toApiException(): AiApiException {
    val responseText = body.readTextLimited(MAX_ERROR_BODY_CHARS)
    val detail = runCatching { extractApiError(JSONObject(responseText)) }.getOrNull()
        ?: responseText.lineSequence().firstOrNull().orEmpty().trim()
    val message = buildString {
        append(code).append(' ').append(this@toApiException.message)
        if (detail.isNotBlank()) append(": ").append(detail.take(MAX_ERROR_MESSAGE_CHARS))
    }
    return AiApiException(message)
}

private fun resolveChatEndpoint(baseUrl: String): HttpUrl {
    val clean = baseUrl.trim().trimEnd('/')
    val target = if (clean.endsWith("/chat/completions")) clean else "$clean/chat/completions"
    return target.toHttpUrlOrNull() ?: throw AiApiException("Invalid API base URL")
}

private fun resolveModelsEndpoint(baseUrl: String): HttpUrl {
    val clean = baseUrl.trim().trimEnd('/').removeSuffix("/chat/completions")
    return "$clean/models".toHttpUrlOrNull() ?: throw AiApiException("Invalid API base URL")
}

internal fun resolveAiTargetHost(baseUrl: String): String =
    baseUrl.trim().toHttpUrlOrNull()?.host ?: "unknown"

internal class AiApiException(message: String) : IOException(message)

private fun ResponseBody.readTextLimited(maxChars: Int): String {
    charStream().use { reader ->
        val buffer = CharArray(DEFAULT_BUFFER_SIZE)
        val output = StringBuilder(minOf(maxChars, DEFAULT_BUFFER_SIZE))
        while (output.length < maxChars) {
            val count = reader.read(buffer, 0, minOf(buffer.size, maxChars - output.length))
            if (count < 0) break
            output.append(buffer, 0, count)
        }
        return output.toString()
    }
}

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
private const val MIN_CONTEXT_BODY_TOKENS = 512
private const val MESSAGE_OVERHEAD_TOKENS = 6
private const val ATTACHMENT_OVERHEAD_TOKENS = 24
private const val IMAGE_TOKEN_ESTIMATE = 1_024
private const val MAX_IMAGES_PER_REQUEST = 4
private const val MAX_ERROR_BODY_CHARS = 4_000
private const val MAX_ERROR_MESSAGE_CHARS = 600
private const val MAX_NON_STREAM_RESPONSE_CHARS = 300_000
private const val MAX_MODELS_RESPONSE_CHARS = 2_000_000
private const val MAX_STREAM_RESPONSE_CHARS = 300_000
private const val MAX_SSE_LINE_CHARS = 1_000_000
private const val TRUNCATION_MARKER = "\n...[context truncated]...\n"
