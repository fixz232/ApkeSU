package me.weishu.kernelsu.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundScrollFollowTest {
    @Test
    fun travelBoundsFavorVerticalMotionAndStayInsideOverscan() {
        val bounds = calculateBackgroundScrollTravelBounds(
            size = IntSize(width = 1_080, height = 2_400),
            minimumVerticalTravelPx = 72f,
            maximumVerticalTravelPx = 144f,
        )

        assertEquals(37.8f, bounds.x, 0.001f)
        assertEquals(108f, bounds.y, 0.001f)
        assertTrue(bounds.y > bounds.x)
    }

    @Test
    fun travelBoundsNeverExceedAvailableOverscan() {
        val bounds = calculateBackgroundScrollTravelBounds(
            size = IntSize(width = 100, height = 100),
            minimumVerticalTravelPx = 72f,
            maximumVerticalTravelPx = 144f,
        )

        assertEquals(3.5f, bounds.x, 0.001f)
        assertEquals(5.28f, bounds.y, 0.001f)
    }

    @Test
    fun followResponseIsStableAcrossRefreshRates() {
        val target = Offset(100f, -60f)
        val at60Hz = advanceFrames(target = target, frameCount = 6, frameSeconds = 1f / 60f)
        val at120Hz = advanceFrames(target = target, frameCount = 12, frameSeconds = 1f / 120f)

        assertEquals(at60Hz.x, at120Hz.x, 0.001f)
        assertEquals(at60Hz.y, at120Hz.y, 0.001f)
        assertTrue(at120Hz.x in 96f..target.x)
        assertTrue(at120Hz.y in target.y..-57f)
    }

    @Test
    fun followResponseApproachesTargetWithoutOvershoot() {
        val target = Offset(42f, -24f)
        var current = Offset(-18f, 16f)

        repeat(24) {
            val previous = current
            current = approachBackgroundScrollOffset(
                current = current,
                target = target,
                frameSeconds = 1f / 120f,
            )
            assertTrue(current.x in previous.x..target.x)
            assertTrue(current.y in target.y..previous.y)
        }
    }

    private fun advanceFrames(
        target: Offset,
        frameCount: Int,
        frameSeconds: Float,
    ): Offset {
        var current = Offset.Zero
        repeat(frameCount) {
            current = approachBackgroundScrollOffset(current, target, frameSeconds)
        }
        return current
    }
}
