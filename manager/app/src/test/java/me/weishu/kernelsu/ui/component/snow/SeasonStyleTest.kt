package me.weishu.kernelsu.ui.component.snow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonStyleTest {
    @Test
    fun unknownAndMissingValuesFallBackToWinter() {
        assertEquals(SeasonStyle.Winter, SeasonStyle.fromValue(null))
        assertEquals(SeasonStyle.Winter, SeasonStyle.fromValue("unknown"))
        assertEquals(SeasonStyle.Winter.value, SeasonStyle.DEFAULT_VALUE)
    }

    @Test
    fun indexMappingUsesStableSeasonOrder() {
        assertEquals(SeasonStyle.Spring, SeasonStyle.fromIndex(0))
        assertEquals(SeasonStyle.Summer, SeasonStyle.fromIndex(1))
        assertEquals(SeasonStyle.Autumn, SeasonStyle.fromIndex(2))
        assertEquals(SeasonStyle.Winter, SeasonStyle.fromIndex(3))
        assertEquals(SeasonStyle.Winter, SeasonStyle.fromIndex(-1))
        assertEquals(SeasonStyle.Winter, SeasonStyle.fromIndex(99))
    }

    @Test
    fun selectedIndexSanitizesStoredValue() {
        SeasonStyle.entries.forEachIndexed { index, season ->
            assertEquals(index, SeasonStyle.selectedIndex(season.value))
        }
        assertEquals(SeasonStyle.entries.indexOf(SeasonStyle.Winter), SeasonStyle.selectedIndex("invalid"))
    }

    @Test
    fun seasonsHaveUniqueValuesAndPaletteColors() {
        assertEquals(SeasonStyle.entries.size, SeasonStyle.entries.map { it.value }.toSet().size)
        assertEquals(SeasonStyle.entries.size, SeasonStyle.entries.map { it.keyColor }.toSet().size)
        SeasonStyle.entries.forEach { season ->
            assertTrue(season.value.isNotBlank())
            assertNotEquals(0, season.wallpaperRes)
        }
    }
}
