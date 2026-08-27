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

    @Test
    fun pagerPositionStaysContinuousWhenCurrentPageRollsOver() {
        val beforeRollover = calculateContinuousPagerPagePosition(
            currentPage = 0,
            currentPageOffsetFraction = 0.499f,
        )
        val afterRollover = calculateContinuousPagerPagePosition(
            currentPage = 1,
            currentPageOffsetFraction = -0.499f,
        )

        assertEquals(0.002f, afterRollover - beforeRollover, 0.0001f)
    }

    @Test
    fun invalidPagerFractionFallsBackToCurrentPage() {
        assertEquals(
            2f,
            calculateContinuousPagerPagePosition(
                currentPage = 2,
                currentPageOffsetFraction = Float.NaN,
            ),
            0f,
        )
    }

    @Test
    fun pagerBackgroundCrossfadeStaysFullyOpaque() {
        listOf(0f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1f).forEach { position ->
            val outgoing = calculatePagerBackgroundTransform(
                pages = listOf(0),
                pagerPosition = position,
            )
            val incoming = calculatePagerBackgroundTransform(
                pages = listOf(1),
                pagerPosition = position,
            )

            assertEquals(1f, outgoing.alpha + incoming.alpha, 0.0001f)
            assertTrue(outgoing.alpha in 0f..1f)
            assertTrue(incoming.alpha in 0f..1f)
        }
    }

    @Test
    fun sharedBackgroundRemainsFixedAcrossAdjacentPages() {
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { position ->
            val transform = calculatePagerBackgroundTransform(
                pages = listOf(0, 1),
                pagerPosition = position,
            )

            assertEquals(1f, transform.alpha, 0.0001f)
            assertEquals(0f, transform.translationXFraction, 0.0001f)
        }
    }

    @Test
    fun activePagerBackgroundPagesFollowFractionWithoutRolloverGap() {
        assertEquals(listOf(0), calculateActivePagerBackgroundPages(0f, 4))
        assertEquals(listOf(0, 1), calculateActivePagerBackgroundPages(0.499f, 4))
        assertEquals(listOf(0, 1), calculateActivePagerBackgroundPages(0.501f, 4))
        assertEquals(listOf(1), calculateActivePagerBackgroundPages(1f, 4))
        assertEquals(listOf(2, 3), calculateActivePagerBackgroundPages(2.75f, 4))
        assertEquals(listOf(3), calculateActivePagerBackgroundPages(3f, 4))
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
