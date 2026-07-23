package me.weishu.kernelsu.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
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
}
