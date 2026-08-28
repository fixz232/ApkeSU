package me.weishu.kernelsu.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import me.weishu.kernelsu.ui.theme.ThemePreset
import me.weishu.kernelsu.ui.theme.defaultThemePresetForUiMode

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
    fun inkIsRemovedAndLegacyValuesMigrateToMiuix() {
        val legacyInkValue = "ink"
        assertFalse(InterfaceStyle.entries.any { it.value == legacyInkValue })
        assertFalse(InterfaceStyle.selectableEntries.any { it.value == legacyInkValue })
        assertEquals(InterfaceStyle.Miuix.value, InterfaceStyle.normalizeValue(legacyInkValue))
        assertEquals(
            InterfaceStyle.selectedIndex(InterfaceStyle.Miuix.value),
            InterfaceStyle.selectedIndex(legacyInkValue),
        )
        assertEquals(ThemePreset.CLEAN_TOOL, ThemePreset.fromValue(legacyInkValue))
        assertEquals(ThemePreset.CLEAN_TOOL, defaultThemePresetForUiMode(legacyInkValue))
    }

    @Test
    fun materialIsSelectableAndUsesMaterialUiMode() {
        val materialValue = InterfaceStyle.Material.value
        assertTrue(InterfaceStyle.Material in InterfaceStyle.entries)
        assertTrue(InterfaceStyle.Material in InterfaceStyle.selectableEntries)
        assertEquals(materialValue, InterfaceStyle.normalizeValue(materialValue))
        assertEquals(
            InterfaceStyle.selectableEntries.indexOf(InterfaceStyle.Material),
            InterfaceStyle.selectedIndex(materialValue),
        )
        assertEquals(UiMode.Material, UiMode.fromValue(materialValue))
        assertEquals(
            materialValue,
            ThemePreset.CUSTOM.targetUiMode(materialValue),
        )
    }

    @Test
    fun unknownStyleFallsBackToMiuix() {
        assertEquals(InterfaceStyle.Miuix.value, InterfaceStyle.normalizeValue("unknown"))
        assertEquals(UiMode.Miuix, UiMode.fromValue("unknown"))
    }

    @Test
    fun studioIsRemovedAndLegacyValuesMigrateToMiuix() {
        val legacyStudioValue = "studio"
        assertFalse(InterfaceStyle.entries.any { it.value == legacyStudioValue })
        assertFalse(InterfaceStyle.selectableEntries.any { it.value == legacyStudioValue })
        assertEquals(InterfaceStyle.Miuix.value, InterfaceStyle.normalizeValue(legacyStudioValue))
        assertEquals(
            InterfaceStyle.selectedIndex(InterfaceStyle.Miuix.value),
            InterfaceStyle.selectedIndex(legacyStudioValue),
        )
        assertEquals(ThemePreset.CLEAN_TOOL, ThemePreset.fromValue(legacyStudioValue))
    }

    @Test
    fun liquidGlassUsesStableFallbackInsteadOfRealtimeBackdrop() {
        assertFalse(resolveRealtimeBlurEnabled(InterfaceStyle.LiquidGlass.value, requested = true))
        assertFalse(resolveRealtimeBlurEnabled(InterfaceStyle.LiquidGlass.value, requested = false))
        assertTrue(resolveRealtimeBlurEnabled(InterfaceStyle.Miuix.value, requested = true))
        assertFalse(resolveRealtimeBlurEnabled(InterfaceStyle.Miuix.value, requested = false))
    }
}
