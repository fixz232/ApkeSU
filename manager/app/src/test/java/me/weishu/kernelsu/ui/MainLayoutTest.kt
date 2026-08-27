package me.weishu.kernelsu.ui

import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.bottombar.MainDestination
import me.weishu.kernelsu.ui.component.bottombar.mainDestinations
import me.weishu.kernelsu.ui.component.bottombar.shouldResetMainPagerForFeatureAvailability
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.KPatchNextStatus
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

        assertTrue(shouldReturnMainPagerBackToHome(selectedPage = 1, kpmActive = true))
        assertFalse(shouldReturnMainPagerBackToHome(selectedPage = 2, kpmActive = true))
        assertTrue(shouldReturnMainPagerBackToHome(selectedPage = 3, kpmActive = true))
        assertTrue(shouldReturnMainPagerBackToHome(selectedPage = 4, kpmActive = true))
    }

    @Test
    fun unresolvedFeatureCheckDoesNotResetSuperuserPage() {
        assertFalse(shouldResetMainPagerForFeatureAvailability(available = null, selectedPage = 1))
        assertFalse(shouldResetMainPagerForFeatureAvailability(available = true, selectedPage = 1))
        assertTrue(shouldResetMainPagerForFeatureAvailability(available = false, selectedPage = 1))
        assertTrue(shouldResetMainPagerForFeatureAvailability(available = false, selectedPage = 2))
        assertFalse(shouldResetMainPagerForFeatureAvailability(available = false, selectedPage = 3))

        assertTrue(
            shouldResetMainPagerForFeatureAvailability(
                available = false,
                selectedPage = 3,
                kpmActive = true,
            )
        )
        assertFalse(
            shouldResetMainPagerForFeatureAvailability(
                available = false,
                selectedPage = 4,
                kpmActive = true,
            )
        )
    }

    @Test
    fun kpmPageUsesEnabledModuleStateAndFallsBackWithoutWebUi() {
        assertFalse(shouldShowKpmPage(null))
        assertFalse(shouldShowKpmPage(KPatchNextStatus(installed = true)))
        assertTrue(shouldShowKpmPage(KPatchNextStatus(installed = true, enabled = true)))
        assertFalse(shouldShowKpmPage(KPatchNextStatus(installed = true, enabled = true, error = "status failed")))
        assertTrue(
            shouldShowKpmPage(
                KPatchNextStatus(installed = true, enabled = true, webUi = true),
            )
        )
        assertFalse(
            shouldShowKpmPage(
                KPatchNextStatus(installed = true, enabled = true, pendingRemove = true, webUi = true),
            )
        )
    }

    @Test
    fun kpmDestinationInsertionKeepsOtherPagesStable() {
        assertEquals(
            MainDestination.SuperUser,
            mainDestinations(kpmActive = false)[1],
        )
        assertEquals(
            MainDestination.SuperUser,
            mainDestinations(kpmActive = true)[2],
        )
        assertEquals(
            MainDestination.Module,
            mainDestinations(kpmActive = false)[2],
        )
        assertEquals(
            MainDestination.Module,
            mainDestinations(kpmActive = true)[3],
        )
    }

    @Test
    fun kpmStatusErrorsAreKeptSeparateFromConfirmedInactiveState() {
        assertEquals(
            KpmPageAvailability.Unknown,
            KpmPageAvailability.fromStatus(KPatchNextStatus(error = "status failed")),
        )
        assertEquals(
            KpmPageAvailability.Inactive,
            KpmPageAvailability.fromStatus(KPatchNextStatus(installed = false)),
        )
        assertEquals(
            KpmPageAvailability.Active,
            KpmPageAvailability.fromStatus(KPatchNextStatus(installed = true, enabled = true)),
        )
    }

    @Test
    fun destinationMappingKeepsTheCurrentLogicalPageWhenKpmChanges() {
        assertEquals(
            2,
            mainDestinations(kpmActive = true).indexOf(MainDestination.SuperUser),
        )
        assertEquals(
            1,
            mainDestinations(kpmActive = false).indexOf(MainDestination.SuperUser),
        )
        assertEquals(
            3,
            mainDestinations(kpmActive = true).indexOf(MainDestination.Module),
        )
        assertEquals(
            2,
            mainDestinations(kpmActive = false).indexOf(MainDestination.Module),
        )
    }

    @Test
    fun kpmDestinationUsesItsOwnCustomNavigationIconSlot() {
        assertEquals(CustomNavigationIconSlot.Kpm, MainDestination.Kpm.slot)
    }
}
