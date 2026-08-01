package me.weishu.kernelsu.ui.component.ink

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InkStyleTest {
    @Test
    fun unknownAndMissingValuesFallBackToMistJiangnan() {
        assertEquals(InkStyle.MistJiangnan, InkStyle.fromValue(null))
        assertEquals(InkStyle.MistJiangnan, InkStyle.fromValue("unknown"))
        assertEquals(InkStyle.MistJiangnan.value, InkStyle.DEFAULT_VALUE)
    }

    @Test
    fun indexAndStoredValueMappingsAreStable() {
        InkStyle.entries.forEachIndexed { index, style ->
            assertEquals(style, InkStyle.fromIndex(index))
            assertEquals(index, InkStyle.selectedIndex(style.value))
        }
        assertEquals(InkStyle.MistJiangnan, InkStyle.fromIndex(-1))
        assertEquals(InkStyle.MistJiangnan, InkStyle.fromIndex(Int.MAX_VALUE))
        assertEquals(0, InkStyle.selectedIndex("invalid"))
    }

    @Test
    fun modesHaveUniquePersistentValuesAndKeyColors() {
        assertEquals(4, InkStyle.entries.size)
        assertEquals(InkStyle.entries.size, InkStyle.entries.map { it.value }.toSet().size)
        assertEquals(InkStyle.entries.size, InkStyle.entries.map { it.keyColor }.toSet().size)
        InkStyle.entries.forEach { style ->
            assertTrue(style.value.isNotBlank())
            assertNotEquals(0, style.keyColor)
        }
    }

    @Test
    fun inkPresentationDefaultsAreEnabled() {
        assertTrue(DEFAULT_INK_FONT_ENABLED)
        assertTrue(DEFAULT_INK_CARD_MOTION_ENABLED)
    }

    @Test
    fun palettesKeepBodyTextReadableOnCardSurfaces() {
        InkStyle.entries.forEach { style ->
            listOf(false, true).forEach { dark ->
                val palette = inkPalette(style, dark)
                assertTrue(contrastRatio(palette.content.luminance(), palette.surfaceTop.luminance()) >= 4.5f)
                assertTrue(
                    contrastRatio(palette.secondaryContent.luminance(), palette.surfaceTop.luminance()) >= 3f,
                )
                assertNotEquals(palette.backgroundTop, palette.backgroundBottom)
            }
        }
    }

    private fun contrastRatio(first: Float, second: Float): Float {
        val lighter = maxOf(first, second)
        val darker = minOf(first, second)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
