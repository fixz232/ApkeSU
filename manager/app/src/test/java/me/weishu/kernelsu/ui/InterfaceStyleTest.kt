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
    fun liquidGlassUsesStableFallbackInsteadOfRealtimeBackdrop() {
        assertFalse(resolveRealtimeBlurEnabled(InterfaceStyle.LiquidGlass.value, requested = true))
        assertFalse(resolveRealtimeBlurEnabled(InterfaceStyle.LiquidGlass.value, requested = false))
        assertTrue(resolveRealtimeBlurEnabled(InterfaceStyle.Miuix.value, requested = true))
        assertFalse(resolveRealtimeBlurEnabled(InterfaceStyle.Miuix.value, requested = false))
    }
}
