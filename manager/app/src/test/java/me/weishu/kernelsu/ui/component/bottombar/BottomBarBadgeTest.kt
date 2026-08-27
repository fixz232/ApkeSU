package me.weishu.kernelsu.ui.component.bottombar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BottomBarBadgeTest {
    @Test
    fun moduleBadgeUsesInstalledModuleCount() {
        val badge = badgeFor(
            MainDestination.Module,
            NavigationBadgeState(moduleCount = 4),
        )

        assertEquals(4, badge?.count)
    }

    @Test
    fun moduleBadgeIsHiddenWhenNoModulesAreInstalled() {
        assertNull(
            badgeFor(
                MainDestination.Module,
                NavigationBadgeState(),
            )
        )
    }
}
