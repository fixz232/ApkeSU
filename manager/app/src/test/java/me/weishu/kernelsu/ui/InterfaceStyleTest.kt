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
        assertFalse(InterfaceStyle.entries.any { it.value == UiMode.Material.value })
        assertFalse(InterfaceStyle.selectableEntries.any { it.value == UiMode.Material.value })
        assertEquals(InterfaceStyle.Miuix.value, InterfaceStyle.normalizeValue(UiMode.Material.value))
        assertEquals(
            InterfaceStyle.selectedIndex(InterfaceStyle.Miuix.value),
            InterfaceStyle.selectedIndex(UiMode.Material.value),
        )
        assertEquals(UiMode.Miuix, UiMode.fromValue(UiMode.Material.value))
        assertEquals(
            InterfaceStyle.Miuix.value,
            ThemePreset.GEEK_DARK.targetUiMode(UiMode.Material.value),
        )
    }
}
