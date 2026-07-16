package me.weishu.kernelsu.ui.component.decoration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiDecorationModelsTest {
    @Test
    fun configurationRoundTripsThroughJson() {
        val source = UiDecorationConfig(
            enabled = true,
            card = UiCardDecoration.Lotus,
            background = UiBackgroundDecoration.StarMap,
            topBar = UiTopBarDecoration.Prism,
            navigation = UiNavigationDecoration.Orbit,
            intensity = 0.74f,
            opacity = 0.53f,
            motionEnabled = false,
            scopes = setOf(UiDecorationScope.Home, UiDecorationScope.Modules),
        )

        val restored = UiDecorationConfig.fromJsonString(source.toJsonString())

        assertEquals(source, restored)
    }

    @Test
    fun invalidConfigurationFallsBackAndSanitizesRanges() {
        val restored = UiDecorationConfig.fromJsonString(
            """{"enabled":true,"card":"missing","intensity":4.0,"opacity":-2.0,"scopes":[]}"""
        )

        assertTrue(restored.enabled)
        assertEquals(UiCardDecoration.Highlight, restored.card)
        assertEquals(1f, restored.intensity)
        assertEquals(0f, restored.opacity)
        assertEquals(UiDecorationScope.entries.toSet(), restored.scopes)
        assertFalse(UiDecorationConfig.fromJsonString("not-json").enabled)
    }

    @Test
    fun presetOnlyReplacesComponentSlots() {
        val source = UiDecorationConfig(
            enabled = true,
            intensity = 0.31f,
            opacity = 0.44f,
            motionEnabled = false,
            scopes = setOf(UiDecorationScope.Settings),
        )

        val autumn = source.withPreset(UiDecorationPreset.Autumn)

        assertEquals(UiCardDecoration.Maple, autumn.card)
        assertEquals(UiBackgroundDecoration.Botanical, autumn.background)
        assertEquals(UiDecorationPreset.Autumn, autumn.matchingPreset())
        assertEquals(source.enabled, autumn.enabled)
        assertEquals(source.intensity, autumn.intensity)
        assertEquals(source.opacity, autumn.opacity)
        assertEquals(source.motionEnabled, autumn.motionEnabled)
        assertEquals(source.scopes, autumn.scopes)
    }

    @Test
    fun activeStateRequiresMasterSwitchAndMatchingScope() {
        val config = UiDecorationConfig(
            enabled = true,
            scopes = setOf(UiDecorationScope.Home),
        )

        assertTrue(config.isActiveFor(UiDecorationScope.Home))
        assertFalse(config.isActiveFor(UiDecorationScope.Modules))
        assertFalse(config.copy(enabled = false).isActiveFor(UiDecorationScope.Home))
    }
}
