package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicManagerStatusTest {
    @Test
    fun parsesVerifiedRuntimeRegistry() {
        val state = parseDynamicManagerStatusJson(
            """
            {
              "schemaVersion": 2,
              "supported": true,
              "configured": true,
              "active": true,
              "certificateSize": 744,
              "certificateSha256": "${"a".repeat(64)}",
              "managers": [
                {"appId": 10100, "signatureIndex": 0},
                {"appId": 10123, "signatureIndex": 255}
              ],
              "error": null
            }
            """.trimIndent(),
        )

        assertTrue(state.supported)
        assertTrue(state.configured)
        assertTrue(state.active)
        assertEquals(744, state.certificateSize)
        assertEquals("a".repeat(64), state.certificateSha256)
        assertEquals(mapOf(10_100 to 0, 10_123 to 255), state.managerSignatureIndexes)
        assertEquals("", state.error)
    }

    @Test
    fun parsesUnsupportedUnconfiguredRuntime() {
        val state = parseDynamicManagerStatusJson(
            """{"schemaVersion":2,"supported":false,"configured":false,"active":false,"certificateSize":0,"certificateSha256":"","managers":[],"error":"ioctl unavailable"}""",
        )

        assertFalse(state.supported)
        assertFalse(state.configured)
        assertFalse(state.active)
        assertEquals("ioctl unavailable", state.error)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsActiveStateWithoutConfiguration() {
        parseDynamicManagerStatusJson(
            """{"schemaVersion":2,"supported":true,"configured":false,"active":true,"certificateSize":0,"certificateSha256":"","managers":[{"appId":10123,"signatureIndex":255}]}""",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedConfiguredCertificate() {
        parseDynamicManagerStatusJson(
            """{"schemaVersion":2,"supported":true,"configured":true,"active":false,"certificateSize":0,"certificateSha256":"","managers":[]}""",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsActiveStateWithoutDynamicRegistryEntry() {
        parseDynamicManagerStatusJson(
            """{"schemaVersion":2,"supported":true,"configured":true,"active":true,"certificateSize":744,"certificateSha256":"${"c".repeat(64)}","managers":[{"appId":10100,"signatureIndex":0}]}""",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidManagerRegistryEntry() {
        parseDynamicManagerStatusJson(
            """{"schemaVersion":2,"supported":true,"configured":false,"active":false,"certificateSize":0,"certificateSha256":"","managers":[{"appId":9999,"signatureIndex":255}]}""",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCertificateDataWhenUnconfigured() {
        parseDynamicManagerStatusJson(
            """{"schemaVersion":2,"supported":true,"configured":false,"active":false,"certificateSize":744,"certificateSha256":"${"d".repeat(64)}","managers":[]}""",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateManagerAppIds() {
        parseDynamicManagerStatusJson(
            """{"schemaVersion":2,"supported":true,"configured":false,"active":false,"certificateSize":0,"certificateSha256":"","managers":[{"appId":10123,"signatureIndex":0},{"appId":10123,"signatureIndex":255}]}""",
        )
    }
}
