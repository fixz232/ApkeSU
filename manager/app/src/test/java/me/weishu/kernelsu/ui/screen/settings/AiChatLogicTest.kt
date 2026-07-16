package me.weishu.kernelsu.ui.screen.settings

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiChatLogicTest {
    @Test
    fun contextBuilderKeepsLatestMessageAndDropsOldContext() {
        val config = testConfig(contextWindowTokens = 2_048, maxOutputTokens = 64)
        val messages = buildList {
            repeat(8) { index ->
                add(
                    AiMessage(
                        id = index + 1L,
                        role = if (index % 2 == 0) AiRole.User else AiRole.Assistant,
                        text = "message-$index " + "x".repeat(1_200),
                        status = if (index % 2 == 0) AiMessageStatus.Ready else AiMessageStatus.Completed,
                    )
                )
            }
            add(
                AiMessage(
                    id = 99L,
                    role = AiRole.User,
                    text = "latest question",
                    status = AiMessageStatus.Ready,
                )
            )
        }

        val result = AiContextBuilder.select(config, messages)

        assertEquals(99L, result.messages.last().id)
        assertEquals(AiRole.User, result.messages.first().role)
        assertTrue(result.droppedMessages > 0)
        assertTrue(result.estimatedInputTokens < config.contextWindowTokens)
    }

    @Test
    fun contextBuilderTrimsOversizedChineseMessageByTokenBudget() {
        val config = testConfig(contextWindowTokens = 2_048, maxOutputTokens = 64)
        val original = AiMessage(
            id = 1L,
            role = AiRole.User,
            text = "测".repeat(10_000),
            status = AiMessageStatus.Ready,
        )

        val result = AiContextBuilder.select(config, listOf(original))

        assertTrue(result.trimmed)
        assertTrue(result.messages.single().text.length < original.text.length)
        assertTrue(result.estimatedInputTokens < config.contextWindowTokens - config.maxOutputTokens)
    }

    @Test
    fun sseParserReportsTextUsageFinishAndDone() {
        val parser = AiSseParser()
        val delta = parser.event(
            """{"choices":[{"delta":{"content":"hello"},"finish_reason":null}]}"""
        )
        val usage = parser.event(
            """{"choices":[],"usage":{"prompt_tokens":11,"completion_tokens":3,"total_tokens":14}}"""
        )
        val finished = parser.event(
            """{"choices":[{"delta":{},"finish_reason":"stop"}]}"""
        )
        val done = parser.event("[DONE]")

        assertEquals("hello", (delta.single() as AiStreamEvent.Delta).text)
        assertEquals(14, (usage.single() as AiStreamEvent.Usage).value.totalTokens)
        assertEquals("stop", (finished.single() as AiStreamEvent.Finished).reason)
        assertEquals(AiStreamEvent.Done, done.single())
    }

    @Test
    fun sseParserDoesNotIgnoreApiErrorsOrToolCalls() {
        val parser = AiSseParser()
        val error = parser.event("""{"error":{"message":"quota exceeded"}}""")
        val tool = parser.event(
            """{"choices":[{"delta":{"tool_calls":[{"function":{"name":"shell"}}]}}]}"""
        )

        assertEquals("quota exceeded", (error.single() as AiStreamEvent.Error).message)
        assertEquals("shell", (tool.single() as AiStreamEvent.ToolRequested).name)
    }

    @Test
    fun persistedStateRoundTripRecoversInterruptedGeneration() {
        val attachment = AiAttachment(
            kind = AiAttachmentKind.Image,
            name = "screen.webp",
            sizeBytes = 123L,
            mimeType = "image/webp",
            storageId = "12345678-1234-1234-1234-123456789abc",
            sha256 = "abc",
        )
        val conversation = AiConversation(
            title = "diagnostic",
            messages = listOf(
                AiMessage(1L, AiRole.User, "look", AiMessageStatus.Ready, listOf(attachment)),
                AiMessage(2L, AiRole.Assistant, "partial", AiMessageStatus.Generating),
            ),
        )
        val original = AiPersistedState(
            config = testConfig(),
            conversations = listOf(conversation),
            activeConversationId = conversation.id,
            trustedHosts = setOf("api.example.com"),
        )

        val restored = parseAiPersistedState(original.toJson().toString(), "default")

        assertNotNull(restored)
        assertEquals(AiMessageStatus.Partial, restored!!.conversations.single().messages.last().status)
        assertEquals(attachment.storageId, restored.conversations.single().messages.first().attachments.single().storageId)
        assertTrue("api.example.com" in restored.trustedHosts)
    }

    @Test
    fun legacyHistoryKeepsErrorAsMessageStatus() {
        val legacy = JSONArray()
            .put(JSONObject().put("id", 1).put("role", "User").put("text", "question"))
            .put(JSONObject().put("id", 2).put("role", "Error").put("text", "failed"))

        val messages = parseLegacyAiMessages(legacy.toString())

        assertEquals(AiRole.User, messages.first().role)
        assertEquals(AiRole.Assistant, messages.last().role)
        assertEquals(AiMessageStatus.Error, messages.last().status)
    }

    @Test
    fun persistedConversationIsSizeBoundedAndStartsWithUser() {
        val messages = buildList {
            repeat(24) { index ->
                add(
                    AiMessage(
                        id = index + 1L,
                        role = if (index % 2 == 0) AiRole.User else AiRole.Assistant,
                        text = "x".repeat(120_000),
                        status = if (index % 2 == 0) AiMessageStatus.Ready else AiMessageStatus.Completed,
                    )
                )
            }
        }
        val conversation = AiConversation(title = "large", messages = messages)
        val state = AiPersistedState(testConfig(), listOf(conversation), conversation.id)

        val restored = parseAiPersistedState(state.toJson().toString(), "default")!!
        val savedMessages = restored.conversations.single().messages

        assertTrue(savedMessages.size < messages.size)
        assertEquals(AiRole.User, savedMessages.first().role)
        assertTrue(savedMessages.sumOf { it.text.length } <= 1_200_000)
    }

    @Test
    fun configValidationRejectsCredentialLeaksOverPlainHttp() {
        assertEquals(
            AiConfigValidationError.InsecureUrl,
            testConfig(baseUrl = "http://api.example.com/v1").validationError(),
        )
        assertNull(
            testConfig(baseUrl = "http://127.0.0.1:8080/v1").validationError(),
        )
        assertEquals(
            AiConfigValidationError.MissingApiKey,
            AiApiConfig(
                provider = AiProviderPreset.OpenAi,
                baseUrl = "https://api.openai.com/v1",
                apiKey = "",
                model = "gpt-test",
                systemPrompt = "test",
            ).validationError(),
        )
        assertFalse(
            testConfig(baseUrl = "https://user:pass@example.com/v1").isValid(),
        )
        assertEquals(
            AiConfigValidationError.InvalidHeaders,
            testConfig().copy(customHeaders = "Authorization: leaked").validationError(),
        )
    }

    private fun testConfig(
        baseUrl: String = "https://api.example.com/v1",
        contextWindowTokens: Int = 8_192,
        maxOutputTokens: Int = 512,
    ) = AiApiConfig(
        provider = AiProviderPreset.Compatible,
        baseUrl = baseUrl,
        model = "test-model",
        systemPrompt = "test system prompt",
        contextWindowTokens = contextWindowTokens,
        maxOutputTokens = maxOutputTokens,
    )

    private fun AiSseParser.event(payload: String): List<AiStreamEvent> {
        assertTrue(acceptLine("data: $payload").isEmpty())
        return acceptLine("")
    }
}
