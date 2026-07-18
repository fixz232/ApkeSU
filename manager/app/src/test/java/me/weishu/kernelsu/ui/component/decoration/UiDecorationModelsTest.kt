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

    @Test
    fun pixelPresetUsesAllPixelDecorationSlots() {
        val config = UiDecorationConfig().withPreset(UiDecorationPreset.Pixel)

        assertEquals(UiCardDecoration.PixelFrame, config.card)
        assertEquals(UiBackgroundDecoration.PixelGrid, config.background)
        assertEquals(UiTopBarDecoration.PixelHud, config.topBar)
        assertEquals(UiNavigationDecoration.PixelDock, config.navigation)
        assertEquals(UiDecorationPreset.Pixel, config.matchingPreset())
    }

    @Test
    fun nativePixelChromeIsDeduplicatedWithoutDroppingCardSelection() {
        val source = UiDecorationConfig().withPreset(UiDecorationPreset.Pixel)

        val rendered = source.deduplicateNativePixelChrome(pixelStyleActive = true)

        assertEquals(UiCardDecoration.PixelFrame, rendered.card)
        assertEquals(UiBackgroundDecoration.None, rendered.background)
        assertEquals(UiTopBarDecoration.None, rendered.topBar)
        assertEquals(UiNavigationDecoration.None, rendered.navigation)
        assertEquals(source, source.deduplicateNativePixelChrome(pixelStyleActive = false))
    }

    @Test
    fun nativePixelSurfaceReportsEveryEffectiveLayer() {
        val source = UiDecorationConfig().withPreset(UiDecorationPreset.Pixel)

        val effective = source.effectiveOnNativePixelSurface(pixelStyleActive = true)

        assertEquals(UiCardDecoration.None, effective.card)
        assertEquals(UiBackgroundDecoration.None, effective.background)
        assertEquals(UiTopBarDecoration.None, effective.topBar)
        assertEquals(UiNavigationDecoration.None, effective.navigation)
        assertEquals(source, source.effectiveOnNativePixelSurface(pixelStyleActive = false))
    }

    @Test
    fun cardDecorationOnlyDropsTheMatchingNativeLayer() {
        assertEquals(
            UiCardDecoration.None,
            UiCardDecoration.PixelFrame.withoutNativeDuplicate(PIXEL_CARD_DECORATIONS),
        )
        assertEquals(
            UiCardDecoration.Circuit,
            UiCardDecoration.Circuit.withoutNativeDuplicate(PIXEL_CARD_DECORATIONS),
        )
    }

    @Test
    fun componentLibraryExposesEveryPixelCardPattern() {
        assertEquals(
            setOf(
                UiCardDecoration.PixelFrame,
                UiCardDecoration.PixelHandheld,
                UiCardDecoration.PixelArcade,
                UiCardDecoration.PixelPastoral,
                UiCardDecoration.PixelStarVoyage,
                UiCardDecoration.PixelInkJade,
                UiCardDecoration.PixelWasteland,
                UiCardDecoration.PixelOcean,
                UiCardDecoration.PixelCyber,
                UiCardDecoration.PixelThreeKingdoms,
                UiCardDecoration.PixelBianliang,
                UiCardDecoration.PixelFishingHarbor,
                UiCardDecoration.PixelTribalJungle,
                UiCardDecoration.PixelLavaValley,
                UiCardDecoration.PixelDunhuangDesert,
                UiCardDecoration.PixelVikingSnowfield,
                UiCardDecoration.PixelJiangnanWatertown,
                UiCardDecoration.PixelCloudTown,
            ),
            PIXEL_CARD_DECORATIONS,
        )
        PIXEL_CARD_DECORATIONS.forEach { decoration ->
            val restored = UiDecorationConfig.fromJsonString(
                UiDecorationConfig(enabled = true, card = decoration).toJsonString()
            )
            assertEquals(decoration, restored.card)
        }
    }

    @Test
    fun customPresetBundleRoundTripsAndNormalizesNames() {
        val preset = CustomUiDecorationPreset(
            id = "preset-1",
            name = "  Winter   tools  ",
            updatedAt = 1234L,
            config = UiDecorationConfig(
                enabled = true,
                card = UiCardDecoration.Snow,
                scopes = setOf(UiDecorationScope.Home, UiDecorationScope.Settings),
            ),
        )

        val restored = customUiDecorationPresetsFromJson(customUiDecorationPresetsToJson(listOf(preset)))

        assertEquals(1, restored.size)
        assertEquals(preset.id, restored.single().id)
        assertEquals("Winter tools", restored.single().name)
        assertEquals(preset.config, restored.single().config)
        assertEquals("Winter tools", sanitizeCustomUiDecorationPresetName(preset.name))
    }

    @Test(expected = IllegalArgumentException::class)
    fun customPresetBundleRejectsUnknownSchema() {
        customUiDecorationPresetsFromJson(
            """{"schema":"unknown","version":1,"presets":[]}"""
        )
    }

    @Test
    fun componentTokensUseStableCategoryPrefixes() {
        val tokens = UiDecorationConfig(
            card = UiCardDecoration.Circuit,
            background = UiBackgroundDecoration.StarMap,
            topBar = UiTopBarDecoration.Prism,
            navigation = UiNavigationDecoration.Orbit,
        ).componentTokens()

        assertEquals(
            listOf("card:circuit", "background:star_map", "top_bar:prism", "navigation:orbit"),
            tokens,
        )
    }

    @Test
    fun previewConfigIgnoresSavedMasterSwitchAndScope() {
        val source = UiDecorationConfig(
            enabled = false,
            scopes = setOf(UiDecorationScope.Home),
        )

        val preview = source.forPreview()

        assertTrue(preview.enabled)
        assertEquals(UiDecorationScope.entries.toSet(), preview.scopes)
        assertEquals(source.card, preview.card)
        assertEquals(source.background, preview.background)
    }
}
