package me.weishu.kernelsu.ui

import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.bottombar.shouldResetMainPagerForFeatureAvailability
import me.weishu.kernelsu.ui.component.bottombar.shouldSnapMainPagerToTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainLayoutTest {
    @Test
    fun visibleFloatingBarKeepsScrollableContentClear() {
        assertEquals(
            112.dp,
            resolveMainContentBottomPadding(
                scaffoldPadding = 64.dp,
                systemNavigationPadding = 24.dp,
                floatingBarEnabled = true,
                navigationBarVisible = true,
            ),
        )
        assertEquals(
            124.dp,
            resolveMainContentBottomPadding(
                scaffoldPadding = 124.dp,
                systemNavigationPadding = 24.dp,
                floatingBarEnabled = true,
                navigationBarVisible = true,
            ),
        )
    }

    @Test
    fun hiddenOrFixedNavigationKeepsScaffoldPadding() {
        assertEquals(
            0.dp,
            resolveMainContentBottomPadding(
                scaffoldPadding = 0.dp,
                systemNavigationPadding = 24.dp,
                floatingBarEnabled = true,
                navigationBarVisible = false,
            ),
        )
        assertEquals(
            72.dp,
            resolveMainContentBottomPadding(
                scaffoldPadding = 72.dp,
                systemNavigationPadding = 24.dp,
                floatingBarEnabled = false,
                navigationBarVisible = true,
            ),
        )
    }

    @Test
    fun superuserBackIsNotRedirectedToHome() {
        assertFalse(shouldReturnMainPagerBackToHome(selectedPage = 0))
        assertFalse(shouldReturnMainPagerBackToHome(selectedPage = 1))
        assertTrue(shouldReturnMainPagerBackToHome(selectedPage = 2))
        assertTrue(shouldReturnMainPagerBackToHome(selectedPage = 3))
    }

    @Test
    fun unresolvedFeatureCheckDoesNotResetSuperuserPage() {
        assertFalse(shouldResetMainPagerForFeatureAvailability(available = null, selectedPage = 1))
        assertFalse(shouldResetMainPagerForFeatureAvailability(available = true, selectedPage = 1))
        assertTrue(shouldResetMainPagerForFeatureAvailability(available = false, selectedPage = 1))
        assertTrue(shouldResetMainPagerForFeatureAvailability(available = false, selectedPage = 2))
        assertFalse(shouldResetMainPagerForFeatureAvailability(available = false, selectedPage = 3))
    }

    @Test
    fun interruptedPagerNavigationRequiresFinalSnap() {
        assertFalse(shouldSnapMainPagerToTarget(targetPage = 3, currentPage = 3, currentPageOffsetFraction = 0f))
        assertTrue(shouldSnapMainPagerToTarget(targetPage = 3, currentPage = 3, currentPageOffsetFraction = -0.42f))
        assertTrue(shouldSnapMainPagerToTarget(targetPage = 3, currentPage = 2, currentPageOffsetFraction = 0f))
        assertTrue(shouldSnapMainPagerToTarget(targetPage = 3, currentPage = 3, currentPageOffsetFraction = Float.NaN))
    }
}
