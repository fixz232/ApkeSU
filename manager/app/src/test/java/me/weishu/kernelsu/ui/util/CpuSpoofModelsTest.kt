package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CpuSpoofModelsTest {
    @Test
    fun modelValidationMatchesAndroidPropertyRules() {
        assertTrue(isCpuSpoofModelValid(" SM8750-AB "))
        assertTrue(isCpuSpoofModelValid("A".repeat(CPU_SPOOF_PROPERTY_VALUE_LIMIT)))
        assertFalse(isCpuSpoofModelValid("A".repeat(CPU_SPOOF_PROPERTY_VALUE_LIMIT + 1)))
        assertFalse(isCpuSpoofModelValid("\nSM8750"))
        assertFalse(isCpuSpoofModelValid("SM8750\tAB"))
        assertFalse(isCpuSpoofModelValid("-SM8750"))
    }

    @Test
    fun emptyRefreshFailurePreservesLastKnownStatus() {
        val previous = CpuSpoofStatus(
            supported = true,
            configured = true,
            enabled = true,
            applied = true,
            current = "SM8750-AB",
            target = "SM8750-AB",
            original = "SM8650",
        )

        val merged = mergeCpuSpoofStatus(
            previous = previous,
            refreshed = CpuSpoofStatus(error = "timeout"),
        )

        assertEquals(previous.copy(error = "timeout"), merged)
    }

    @Test
    fun partialRefreshFailureKeepsFreshPayload() {
        val refreshed = CpuSpoofStatus(
            configured = true,
            enabled = true,
            target = "MT6991",
            error = "ro.soc.model unavailable",
        )

        assertEquals(
            refreshed,
            mergeCpuSpoofStatus(
                previous = CpuSpoofStatus(current = "SM8650"),
                refreshed = refreshed,
            ),
        )
    }
}
