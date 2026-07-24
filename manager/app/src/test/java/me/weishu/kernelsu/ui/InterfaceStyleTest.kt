package me.weishu.kernelsu.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import me.weishu.kernelsu.ui.theme.ThemePreset

class InterfaceStyleTest {
    @Test
    fun alphaAndDeltaShareOneSelectableEntry() {
        assertTrue(InterfaceStyle.Alpha in InterfaceStyle.selectableEntries)
        assertFalse(InterfaceStyle.Delta in InterfaceStyle.selectableEntries)
        assertEquals(
            InterfaceStyle.selectedIndex(InterfaceStyle.Alpha.value),
            InterfaceStyle.selectedIndex(InterfaceStyle.Delta.value),
        )
    }

    @Test
    fun selectableIndexRoundTripsToVisibleStyle() {
        InterfaceStyle.selectableEntries.forEachIndexed { index, style ->
            assertEquals(style, InterfaceStyle.fromIndex(index))
        }
    }

    @Test
    fun materialIsNotSelectableAndMigratesToMiuix() {
        val legacyMaterialValue = "material"
        assertFalse(InterfaceStyle.entries.any { it.value == legacyMaterialValue })
        assertFalse(InterfaceStyle.selectableEntries.any { it.value == legacyMaterialValue })
        assertEquals(InterfaceStyle.Miuix.value, InterfaceStyle.normalizeValue(legacyMaterialValue))
        assertEquals(
            InterfaceStyle.selectedIndex(InterfaceStyle.Miuix.value),
            InterfaceStyle.selectedIndex(legacyMaterialValue),
        )
        assertEquals(UiMode.Miuix, UiMode.fromValue(legacyMaterialValue))
        assertEquals(
            InterfaceStyle.Miuix.value,
            ThemePreset.GEEK_DARK.targetUiMode(legacyMaterialValue),
        )
    }

    @Test
    fun liquidGlassUsesStableFallbackInsteadOfRealtimeBackdrop() {
        assertFalse(resolveRealtimeBlurEnabled(InterfaceStyle.LiquidGlass.value, requested = true))
        assertFalse(resolveRealtimeBlurEnabled(InterfaceStyle.LiquidGlass.value, requested = false))
        assertTrue(resolveRealtimeBlurEnabled(InterfaceStyle.Miuix.value, requested = true))
        assertFalse(resolveRealtimeBlurEnabled(InterfaceStyle.Miuix.value, requested = false))
    }
}
