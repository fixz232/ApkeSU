package me.weishu.kernelsu.ui.component.skrootpro

import org.junit.Assert.assertEquals
import org.junit.Test

class SkrootproScaffoldTest {
    @Test
    fun screenSurfaceIsOpaqueWithoutAnImmersiveBackdrop() {
        assertEquals(
            1f,
            skrootproScreenSurfaceAlpha(
                immersiveBackgroundActive = false,
                nightEffectActive = false,
                darkTheme = false,
            ),
            0.001f,
        )
    }

    @Test
    fun screenSurfaceKeepsAStableVeilOverImmersiveBackdrops() {
        assertEquals(
            0.90f,
            skrootproScreenSurfaceAlpha(
                immersiveBackgroundActive = true,
                nightEffectActive = false,
                darkTheme = false,
            ),
            0.001f,
        )
        assertEquals(
            0.86f,
            skrootproScreenSurfaceAlpha(
                immersiveBackgroundActive = false,
                nightEffectActive = true,
                darkTheme = true,
            ),
            0.001f,
        )
    }
}
