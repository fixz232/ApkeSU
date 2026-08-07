package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomVideoBackgroundTest {
    @Test
    fun sanitizeCustomVideoBackgroundFrameRateKeepsSupportedValues() {
        CUSTOM_VIDEO_BACKGROUND_FRAME_RATE_OPTIONS.forEach { value ->
            assertEquals(value, sanitizeCustomVideoBackgroundFrameRate(value))
        }
    }

    @Test
    fun sanitizeCustomVideoBackgroundFrameRateFallsBackToDefault() {
        listOf(0, 24, 59, 61, 89, 121, 164, 166, Int.MIN_VALUE, Int.MAX_VALUE).forEach { value ->
            assertEquals(
                DEFAULT_CUSTOM_VIDEO_BACKGROUND_FRAME_RATE,
                sanitizeCustomVideoBackgroundFrameRate(value),
            )
        }
    }
}
