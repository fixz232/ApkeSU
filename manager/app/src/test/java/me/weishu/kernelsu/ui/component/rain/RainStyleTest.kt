package me.weishu.kernelsu.ui.component.rain

import androidx.compose.ui.graphics.Color
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RainStyleTest {
    @Test
    fun unknownAndMissingValuesFallBackToLightRain() {
        assertEquals(RainStyle.LightRain, RainStyle.fromValue(null))
        assertEquals(RainStyle.LightRain, RainStyle.fromValue("unknown"))
        assertEquals(RainStyle.LightRain.value, RainStyle.DEFAULT_VALUE)
    }

    @Test
    fun indexMappingUsesStableIntensityOrder() {
        assertEquals(RainStyle.LightRain, RainStyle.fromIndex(0))
        assertEquals(RainStyle.MediumRain, RainStyle.fromIndex(1))
        assertEquals(RainStyle.HeavyRain, RainStyle.fromIndex(2))
        assertEquals(RainStyle.Thunderstorm, RainStyle.fromIndex(3))
        assertEquals(RainStyle.LightRain, RainStyle.fromIndex(-1))
        assertEquals(RainStyle.LightRain, RainStyle.fromIndex(99))
    }

    @Test
    fun selectedIndexSanitizesStoredValue() {
        RainStyle.entries.forEachIndexed { index, style ->
            assertEquals(index, RainStyle.selectedIndex(style.value))
        }
        assertEquals(0, RainStyle.selectedIndex("invalid"))
    }

    @Test
    fun modesHaveUniqueValuesAndKeyColors() {
        assertEquals(4, RainStyle.entries.size)
        assertEquals(RainStyle.entries.size, RainStyle.entries.map { it.value }.toSet().size)
        assertEquals(RainStyle.entries.size, RainStyle.entries.map { it.keyColor }.toSet().size)
        RainStyle.entries.forEach { style ->
            assertTrue(style.value.isNotBlank())
            assertNotEquals(0, style.keyColor)
        }
    }

    @Test
    fun rainDensityAndSpeedIncreaseWithIntensity() {
        val specs = RainStyle.entries.map(RainSceneSpec::forStyle)
        specs.zipWithNext().forEach { (lighter, stronger) ->
            assertTrue(stronger.dropCount > lighter.dropCount)
            assertTrue(stronger.rippleCount > lighter.rippleCount)
            assertTrue(stronger.cycleMillis < lighter.cycleMillis)
            assertTrue(stronger.maxLengthDp > lighter.maxLengthDp)
        }
    }

    @Test
    fun requestedDayAndNightFoundationColorsArePreserved() {
        val light = rainPalette(RainStyle.LightRain, dark = false)
        assertEquals(Color(0xFF7395B8), light.backgroundTop)
        assertEquals(Color(0xFFA1B8CF), light.backgroundBottom)
        assertEquals(Color(0xFFB8C6D4), light.fog)

        val night = rainPalette(RainStyle.LightRain, dark = true)
        assertEquals(Color(0xFF2C3B4E), night.backgroundTop)
        assertEquals(Color(0xFF19222D), night.backgroundBottom)
    }

    @Test
    fun heavyModesForceReadableDarkSurfaces() {
        assertFalse(forceRainDarkTheme(RainStyle.LightRain))
        assertFalse(forceRainDarkTheme(RainStyle.MediumRain))
        assertTrue(forceRainDarkTheme(RainStyle.HeavyRain))
        assertTrue(forceRainDarkTheme(RainStyle.Thunderstorm))
        assertEquals(
            rainPalette(RainStyle.HeavyRain, dark = false),
            rainPalette(RainStyle.HeavyRain, dark = true),
        )
    }

    @Test
    fun lightningRequiresNightAnimationsAndThunderstorm() {
        assertTrue(isRainLightningEnabled(RainStyle.Thunderstorm, dark = true, animationsEnabled = true))
        assertFalse(isRainLightningEnabled(RainStyle.Thunderstorm, dark = false, animationsEnabled = true))
        assertFalse(isRainLightningEnabled(RainStyle.Thunderstorm, dark = true, animationsEnabled = false))
        assertFalse(isRainLightningEnabled(RainStyle.HeavyRain, dark = true, animationsEnabled = true))
    }

    @Test
    fun lightningTimingStaysInsideRequestedBounds() {
        val random = Random(32685)
        repeat(200) {
            assertTrue(nextLightningDelayMillis(random) in 3_000L..8_000L)
            assertTrue(nextLightningFlashDurationMillis(random) in 50L..100L)
        }
    }

    @Test
    fun cardCanopyScalesAndSkipsTinyCards() {
        assertEquals(13f, rainCardDecorationHeight(13f, 320f, 180f), 0.001f)
        assertEquals(9.5f, rainCardDecorationHeight(13f, 320f, 50f), 0.001f)
        assertEquals(0f, rainCardDecorationHeight(13f, 71f, 180f), 0.001f)
        assertEquals(0f, rainCardDecorationHeight(13f, 320f, 47f), 0.001f)
        assertEquals(0f, rainCardContentLayerColor(Color.Blue).alpha, 0f)
    }
}
