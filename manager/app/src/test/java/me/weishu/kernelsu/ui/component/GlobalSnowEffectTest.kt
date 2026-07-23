package me.weishu.kernelsu.ui.component

import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSnowEffectTest {
    @Test
    fun storedValuesMapToStableEffects() {
        GlobalSnowEffect.entries.forEachIndexed { index, effect ->
            assertEquals(effect, GlobalSnowEffect.fromValue(effect.value))
            assertEquals(effect, GlobalSnowEffect.fromIndex(index))
            assertEquals(index, GlobalSnowEffect.selectedIndex(effect.value))
        }
        assertEquals(GlobalSnowEffect.SeasonalRain, GlobalSnowEffect.fromValue("seasonal_rain"))
        assertEquals(GlobalSnowEffect.Default, GlobalSnowEffect.fromValue("unknown"))
        assertEquals(GlobalSnowEffect.Default, GlobalSnowEffect.fromIndex(-1))
        assertEquals(GlobalSnowEffect.entries.size, GlobalSnowEffect.entries.map { it.value }.toSet().size)
    }

    @Test
    fun rainSpecsKeepDistinctSeasonalCharacter() {
        val spring = SeasonalRainSpec.forSeason(SeasonStyle.Spring)
        val summer = SeasonalRainSpec.forSeason(SeasonStyle.Summer)
        val autumn = SeasonalRainSpec.forSeason(SeasonStyle.Autumn)
        val winter = SeasonalRainSpec.forSeason(SeasonStyle.Winter)

        assertTrue(summer.dropCount > spring.dropCount)
        assertTrue(summer.dropCount > autumn.dropCount)
        assertTrue(autumn.slantRatio > spring.slantRatio)
        assertTrue(autumn.slantRatio > summer.slantRatio)
        assertTrue(winter.cycleMillis > spring.cycleMillis)
        assertTrue(winter.cycleMillis > summer.cycleMillis)
        assertEquals(
            SeasonStyle.entries.size,
            SeasonStyle.entries.map { SeasonalRainSpec.forSeason(it).primaryColor }.toSet().size,
        )
    }

    @Test
    fun generatedRainParticlesAreDeterministicAndBounded() {
        SeasonStyle.entries.forEach { season ->
            val spec = SeasonalRainSpec.forSeason(season)
            val drops = createSeasonalRainDrops(spec, 1234 + season.ordinal)
            val impacts = createSeasonalRainImpacts(spec, 5678 + season.ordinal)

            assertEquals(spec.dropCount, drops.size)
            assertEquals(spec.impactCount, impacts.size)
            assertEquals(drops, createSeasonalRainDrops(spec, 1234 + season.ordinal))
            assertEquals(impacts, createSeasonalRainImpacts(spec, 5678 + season.ordinal))
            assertNotEquals(drops, createSeasonalRainDrops(spec, 9234 + season.ordinal))
            assertTrue(drops.all { it.baseX in 0f..1f && it.baseY in 0f..1f })
            assertTrue(drops.all { it.depth in 0.22f..1f && it.lengthDp > 0f && it.strokeDp > 0f })
            assertTrue(drops.all { it.fallCycles in spec.minFallCycles..spec.maxFallCycles })
            assertTrue(impacts.all { it.x in 0.04f..0.96f && it.y in 0.68f..0.96f })
            assertTrue(impacts.all { it.radiusScale > 0f && it.alpha > 0f })
        }
    }
}
