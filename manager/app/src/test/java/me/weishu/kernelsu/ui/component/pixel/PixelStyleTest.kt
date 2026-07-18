package me.weishu.kernelsu.ui.component.pixel

import me.weishu.kernelsu.ui.component.decoration.PixelCardPattern
import me.weishu.kernelsu.ui.component.decoration.pixelCardTopDecorationHeight
import me.weishu.kernelsu.ui.component.decoration.pixelPatternFramePolishEnabled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelStyleTest {
    @Test
    fun unknownAndMissingValuesFallBackToClassicHandheld() {
        assertEquals(PixelStyle.ClassicHandheld, PixelStyle.fromValue(null))
        assertEquals(PixelStyle.ClassicHandheld, PixelStyle.fromValue("unknown"))
        assertEquals(PixelStyle.ClassicHandheld.value, PixelStyle.DEFAULT_VALUE)
    }

    @Test
    fun indexMappingUsesStableVariantOrder() {
        assertEquals(PixelStyle.ClassicHandheld, PixelStyle.fromIndex(0))
        assertEquals(PixelStyle.NeonArcade, PixelStyle.fromIndex(1))
        assertEquals(PixelStyle.PastoralFields, PixelStyle.fromIndex(2))
        assertEquals(PixelStyle.StarVoyage, PixelStyle.fromIndex(3))
        assertEquals(PixelStyle.InkJade, PixelStyle.fromIndex(4))
        assertEquals(PixelStyle.RustWasteland, PixelStyle.fromIndex(5))
        assertEquals(PixelStyle.OceanDepths, PixelStyle.fromIndex(6))
        assertEquals(PixelStyle.CyberHacker, PixelStyle.fromIndex(7))
        assertEquals(PixelStyle.ThreeKingdoms, PixelStyle.fromIndex(8))
        assertEquals(PixelStyle.BianliangMarket, PixelStyle.fromIndex(9))
        assertEquals(PixelStyle.FishingHarbor, PixelStyle.fromIndex(10))
        assertEquals(PixelStyle.TribalJungle, PixelStyle.fromIndex(11))
        assertEquals(PixelStyle.LavaValley, PixelStyle.fromIndex(12))
        assertEquals(PixelStyle.DunhuangDesert, PixelStyle.fromIndex(13))
        assertEquals(PixelStyle.VikingSnowfield, PixelStyle.fromIndex(14))
        assertEquals(PixelStyle.JiangnanWatertown, PixelStyle.fromIndex(15))
        assertEquals(PixelStyle.CloudTown, PixelStyle.fromIndex(16))
        assertEquals(PixelStyle.ClassicHandheld, PixelStyle.fromIndex(-1))
        assertEquals(PixelStyle.ClassicHandheld, PixelStyle.fromIndex(99))
    }

    @Test
    fun selectedIndexSanitizesStoredValue() {
        PixelStyle.entries.forEachIndexed { index, style ->
            assertEquals(index, PixelStyle.selectedIndex(style.value))
        }
        assertEquals(0, PixelStyle.selectedIndex("invalid"))
        assertEquals(2, PixelStyle.selectedIndex("forest_quest"))
    }

    @Test
    fun variantsHaveUniqueValuesAndKeyColors() {
        assertEquals(17, PixelStyle.entries.size)
        assertEquals(PixelStyle.entries.size, PixelStyle.entries.map { it.value }.toSet().size)
        assertEquals(PixelStyle.entries.size, PixelStyle.entries.map { it.keyColor }.toSet().size)
        PixelStyle.entries.forEach { style ->
            assertTrue(style.value.isNotBlank())
            assertNotEquals(0, style.keyColor)
        }
    }

    @Test
    fun newVariantsKeepDistinctSurfaceAndAccentLayers() {
        listOf(
            PixelStyle.InkJade,
            PixelStyle.RustWasteland,
            PixelStyle.OceanDepths,
            PixelStyle.CyberHacker,
            PixelStyle.ThreeKingdoms,
            PixelStyle.BianliangMarket,
            PixelStyle.FishingHarbor,
            PixelStyle.TribalJungle,
            PixelStyle.LavaValley,
            PixelStyle.DunhuangDesert,
            PixelStyle.VikingSnowfield,
            PixelStyle.JiangnanWatertown,
            PixelStyle.CloudTown,
        ).forEach { style ->
            listOf(false, true).forEach { dark ->
                val palette = pixelPalette(style, dark)
                assertNotEquals(palette.background, palette.surface)
                assertNotEquals(palette.surface, palette.surfaceAlt)
                assertNotEquals(palette.primary, palette.secondary)
                assertNotEquals(palette.outline, palette.highlight)
            }
        }
    }

    @Test
    fun legacyForestValueMigratesToPastoralFields() {
        assertEquals(PixelStyle.PastoralFields, PixelStyle.fromValue("forest_quest"))
        assertEquals("pastoral_fields", PixelStyle.PastoralFields.value)
    }

    @Test
    fun everyInterfaceVariantUsesItsOwnDecoratedCardPattern() {
        assertEquals(PixelCardPattern.Handheld, PixelStyle.ClassicHandheld.cardPattern())
        assertEquals(PixelCardPattern.Arcade, PixelStyle.NeonArcade.cardPattern())
        assertEquals(PixelCardPattern.Pastoral, PixelStyle.PastoralFields.cardPattern())
        assertEquals(PixelCardPattern.StarVoyage, PixelStyle.StarVoyage.cardPattern())
        assertEquals(PixelCardPattern.InkJade, PixelStyle.InkJade.cardPattern())
        assertEquals(PixelCardPattern.Wasteland, PixelStyle.RustWasteland.cardPattern())
        assertEquals(PixelCardPattern.Ocean, PixelStyle.OceanDepths.cardPattern())
        assertEquals(PixelCardPattern.Cyber, PixelStyle.CyberHacker.cardPattern())
        assertEquals(PixelCardPattern.ThreeKingdoms, PixelStyle.ThreeKingdoms.cardPattern())
        assertEquals(PixelCardPattern.Bianliang, PixelStyle.BianliangMarket.cardPattern())
        assertEquals(PixelCardPattern.FishingHarbor, PixelStyle.FishingHarbor.cardPattern())
        assertEquals(PixelCardPattern.TribalJungle, PixelStyle.TribalJungle.cardPattern())
        assertEquals(PixelCardPattern.LavaValley, PixelStyle.LavaValley.cardPattern())
        assertEquals(PixelCardPattern.DunhuangDesert, PixelStyle.DunhuangDesert.cardPattern())
        assertEquals(PixelCardPattern.VikingSnowfield, PixelStyle.VikingSnowfield.cardPattern())
        assertEquals(PixelCardPattern.JiangnanWatertown, PixelStyle.JiangnanWatertown.cardPattern())
        assertEquals(PixelCardPattern.CloudTown, PixelStyle.CloudTown.cardPattern())
        assertEquals(17, PixelStyle.entries.map(PixelStyle::cardPattern).toSet().size)
    }

    @Test
    fun allVariantsKeepDistinctGlobalPalettes() {
        listOf(false, true).forEach { dark ->
            val palettes = PixelStyle.entries.map { pixelPalette(it, dark) }
            assertEquals(PixelStyle.entries.size, palettes.map { it.background }.toSet().size)
            assertEquals(PixelStyle.entries.size, palettes.map { it.surface }.toSet().size)
            assertEquals(PixelStyle.entries.size, palettes.map { it.primary }.toSet().size)
        }
    }

    @Test
    fun cyberHackerKeepsItsDarkConsolePaletteInEveryThemeMode() {
        assertEquals(
            pixelPalette(PixelStyle.CyberHacker, false),
            pixelPalette(PixelStyle.CyberHacker, true),
        )
    }

    @Test
    fun cardTopDecorationsStayCompactAndSkipTinyCards() {
        val unit = 3f
        assertEquals(12f, pixelCardTopDecorationHeight(unit, 360f, 120f), 0.001f)
        assertEquals(9.6f, pixelCardTopDecorationHeight(unit, 240f, 60f), 0.001f)
        assertEquals(0f, pixelCardTopDecorationHeight(unit, 48f, 60f), 0.001f)
        assertEquals(0f, pixelCardTopDecorationHeight(unit, 240f, 24f), 0.001f)
    }

    @Test
    fun polishedFramesSkipCardsThatCannotKeepTheirContentClear() {
        assertTrue(pixelPatternFramePolishEnabled(unit = 3f, width = 360f, height = 120f))
        assertFalse(pixelPatternFramePolishEnabled(unit = 3f, width = 48f, height = 120f))
        assertFalse(pixelPatternFramePolishEnabled(unit = 3f, width = 360f, height = 24f))
        assertFalse(pixelPatternFramePolishEnabled(unit = 0f, width = 360f, height = 120f))
    }
}
