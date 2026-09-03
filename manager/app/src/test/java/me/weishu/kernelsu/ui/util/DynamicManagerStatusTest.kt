package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicManagerStatusTest {
    @Test
    fun parsesVerifiedRuntimeIdentity() {
        val state = parseDynamicManagerStatusJson(
            """
            {
              "schemaVersion": 1,
              "supported": true,
              "configured": true,
              "active": true,
              "packageName": "com.example.manager",
              "appId": 10123,
              "certificateSize": 744,
              "certificateSha256": "${"a".repeat(64)}",
              "error": null
            }
            """.trimIndent(),
        )

        assertTrue(state.supported)
        assertTrue(state.configured)
        assertTrue(state.active)
        assertEquals("com.example.manager", state.packageName)
        assertEquals(10_123, state.appId)
        assertEquals(744, state.certificateSize)
        assertEquals("a".repeat(64), state.certificateSha256)
        assertEquals("", state.error)
    }

    @Test
    fun parsesUnsupportedUnconfiguredRuntime() {
        val state = parseDynamicManagerStatusJson(
            """{"schemaVersion":1,"supported":false,"configured":false,"active":false,"error":"ioctl unavailable"}""",
        )

        assertFalse(state.supported)
        assertFalse(state.configured)
        assertFalse(state.active)
        assertEquals("ioctl unavailable", state.error)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsActiveStateWithoutConfiguration() {
        parseDynamicManagerStatusJson(
            """{"schemaVersion":1,"supported":true,"configured":false,"active":true}""",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedConfiguredIdentity() {
        parseDynamicManagerStatusJson(
            """{"schemaVersion":1,"supported":true,"configured":true,"active":false,"packageName":"","appId":0,"certificateSize":0,"certificateSha256":""}""",
        )
    }
}
