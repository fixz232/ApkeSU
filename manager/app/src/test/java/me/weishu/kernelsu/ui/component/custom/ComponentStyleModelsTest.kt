package me.weishu.kernelsu.ui.component.custom

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentStyleModelsTest {
    @Test
    fun pixelGridDimensionCompatibilityRejectsCrossLayerUpdates() {
        val top = PixelGrid.blank(CARD_GRID_WIDTH, CARD_TOP_GRID_HEIGHT)
        val body = PixelGrid.blank(CARD_GRID_WIDTH, CARD_BODY_GRID_HEIGHT)
        val anotherTop = PixelGrid.blank(CARD_GRID_WIDTH, CARD_TOP_GRID_HEIGHT)

        assertTrue(top.hasSameDimensionsAs(anotherTop))
        assertFalse(top.hasSameDimensionsAs(body))
    }

    @Test
    fun cardStyleRoundTripsAllTargetsAndMotion() {
        val top = PixelGrid.blank(CARD_GRID_WIDTH, CARD_TOP_GRID_HEIGHT)
            .withPixel(2, 1, 0xFF42B9F5L)
        val defaultLayers = CustomCardLayers(top = top)
        val overrideLayers = CustomCardLayers(
            border = PixelGrid.blank(CARD_GRID_WIDTH, CARD_BODY_GRID_HEIGHT)
                .withPixel(0, 0, 0xFF9D7BF7L),
        )
        val source = CustomCardStyle(
            id = "card-test-style",
            name = "  Test   card  ",
            author = "  Example   author ",
            updatedAt = 123L,
            defaultLayers = defaultLayers,
            cardOverrides = mapOf(CustomCardTarget.Module to overrideLayers),
            bottomBar = CustomNavigationLayers(
                top = PixelGrid.blank(NAVIGATION_GRID_WIDTH, NAVIGATION_TOP_GRID_HEIGHT)
                    .withPixel(4, 0, 0xFF48C78EL),
            ),
            palette = listOf(0L, 0xFFFFFFFFL, 0xFF42B9F5L),
            motion = PixelMotionRule(
                enabled = true,
                mode = PixelMotionMode.Drift,
                durationMillis = 3_000,
                amplitudeCells = 2,
                repeat = PixelMotionRepeat.Reverse,
            ),
        )

        val restored = CustomCardStyle.fromJsonString(source.toJsonString())

        assertEquals("Test card", restored.name)
        assertEquals("Example author", restored.author)
        assertEquals(0xFF42B9F5L, restored.defaultLayers.top.colorAt(2, 1))
        assertEquals(0xFF9D7BF7L, restored.layersFor(CustomCardTarget.Module).border.colorAt(0, 0))
        assertEquals(0xFF42B9F5L, restored.layersFor(CustomCardTarget.Lkm).top.colorAt(2, 1))
        assertEquals(PixelMotionMode.Drift, restored.motion.mode)
        assertTrue(restored.motion.enabled)
    }

    @Test
    fun switchPackageJsonNeverLeaksLocalImageUri() {
        val source = CustomSwitchStyle(
            id = "switch-test-style",
            source = CustomSwitchSource.Image,
            imageUri = "file:///data/user/0/private/image.png",
            imageSha256 = "a".repeat(64),
            imageMimeType = "image/png",
            imageOnUri = "file:///data/user/0/private/image-on.png",
            imageOnSha256 = "b".repeat(64),
            imageOnMimeType = "image/webp",
        )

        val packageJson = source.toJson(includeLocalImageUri = false)
        val restored = CustomSwitchStyle.fromJson(packageJson, allowLocalImageUri = false)

        assertTrue(packageJson.isNull("image_uri"))
        assertTrue(packageJson.isNull("image_on_uri"))
        assertNull(restored.imageUri)
        assertNull(restored.imageOnUri)
        assertEquals(source.imageSha256, restored.imageSha256)
        assertEquals(source.imageOnSha256, restored.imageOnSha256)
        assertEquals(CustomSwitchSource.Image, restored.source)
    }

    @Test
    fun switchStyleRoundTripsImageGeometryAndEffects() {
        val source = CustomSwitchStyle(
            id = "switch-complete-style",
            source = CustomSwitchSource.Image,
            imageUri = "file:///data/user/0/private/off.png",
            imageSha256 = "a".repeat(64),
            imageMimeType = "image/png",
            imageOnUri = "file:///data/user/0/private/on.png",
            imageOnSha256 = "b".repeat(64),
            imageOnMimeType = "image/png",
            imageZoom = 1.75f,
            imageOffsetX = -0.25f,
            imageOffsetY = 0.4f,
            imageRotationDegrees = 45f,
            imageFlipHorizontal = true,
            imageTint = 0xCC9D7BF7L,
            imageSaturation = 1.4f,
            imageBrightness = -0.2f,
            imageBlend = SwitchImageBlend.Screen,
            trackScaleX = 0.8f,
            trackScaleY = 0.7f,
            trackBaseColor = 0xFF101216L,
            cornerRadiusFraction = 0.2f,
            borderColor = 0xFF42B9F5L,
            trackOffColorOverride = 0xFF20242CL,
            trackOnColorOverride = 0xFF5457ECL,
            borderOffColorOverride = 0x66888888L,
            borderOnColorOverride = 0xFF9D7BF7L,
            borderWidthDp = 2.5f,
            thumbScale = 0.8f,
            thumbPaddingDp = 2f,
            thumbTravel = 0.75f,
            thumbBaseColor = 0xFFFFFFFFL,
            thumbOffColorOverride = 0xFFB8C0CCL,
            thumbOnColorOverride = 0xFFFFFFFFL,
            shadowRadiusDp = 3f,
            glowRadiusDp = 4f,
            disabledAlpha = 0.6f,
            transitionDurationMillis = 180,
            transitionEasing = SwitchTransitionEasing.Decelerate,
        )

        val restored = CustomSwitchStyle.fromJsonString(source.toJsonString())

        assertEquals(source.normalized(), restored)
    }

    @Test
    fun switchStyleRoundTripsIndependentOffAndOnImageAppearances() {
        val source = CustomSwitchStyle(
            id = "switch-independent-appearance",
            source = CustomSwitchSource.Image,
            imageUri = "file:///data/user/0/private/off.png",
            imageSha256 = "a".repeat(64),
            imageOnUri = "file:///data/user/0/private/on.png",
            imageOnSha256 = "b".repeat(64),
            imageOffAppearance = SwitchImageAppearance(
                scale = SwitchImageScale.Fit,
                opacity = 0.72f,
                zoom = 1.4f,
                offsetX = -0.3f,
                rotationDegrees = -18f,
                tint = 0xCC42B9F5L,
                blend = SwitchImageBlend.Multiply,
            ),
            imageOnAppearance = SwitchImageAppearance(
                scale = SwitchImageScale.Crop,
                opacity = 0.94f,
                zoom = 2.1f,
                offsetY = 0.24f,
                flipHorizontal = true,
                saturation = 1.6f,
                brightness = 0.12f,
                blend = SwitchImageBlend.Screen,
            ),
        )

        val restored = CustomSwitchStyle.fromJsonString(source.toJsonString())

        assertEquals(source.normalized(), restored)
        assertEquals(SwitchImageScale.Fit, restored.imageAppearanceFor(on = false).scale)
        assertEquals(2.1f, restored.imageAppearanceFor(on = true).zoom)
        assertEquals(SwitchImageBlend.Screen, restored.imageAppearanceFor(on = true).blend)
    }

    @Test
    fun legacySwitchStyleUsesCompatibleDefaults() {
        val legacy = CustomSwitchStyle(id = "switch-legacy-style").toJson(includeLocalImageUri = true).apply {
            put("version", 1)
            remove("track_scale_x")
            remove("track_scale_y")
            remove("transition_duration_ms")
            remove("transition_easing")
        }

        val restored = CustomSwitchStyle.fromJson(legacy, allowLocalImageUri = true)

        assertEquals(1f, restored.trackScaleX)
        assertEquals(1f, restored.trackScaleY)
        assertEquals(220, restored.transitionDurationMillis)
        assertEquals(SwitchTransitionEasing.Standard, restored.transitionEasing)
    }

    @Test
    fun pixelSwitchDropsUnusedImageReferences() {
        val normalized = CustomSwitchStyle(
            source = CustomSwitchSource.Pixel,
            imageUri = "file:///data/user/0/private/image.png",
            imageSha256 = "a".repeat(64),
            imageMimeType = "image/png",
        ).normalized()

        assertNull(normalized.imageUri)
        assertNull(normalized.imageSha256)
        assertNull(normalized.imageMimeType)
    }

    @Test
    fun motionRuleClampsUnsafeImportedValues() {
        val restored = PixelMotionRule.fromJson(
            JSONObject()
                .put("enabled", true)
                .put("mode", "pulse")
                .put("duration_ms", 1)
                .put("amplitude_cells", 99)
        )

        assertEquals(MIN_PIXEL_MOTION_DURATION_MS, restored.durationMillis)
        assertEquals(MAX_PIXEL_MOTION_AMPLITUDE, restored.amplitudeCells)
        assertTrue(restored.enabled)
        assertFalse(PixelMotionRule(enabled = true).normalized().enabled)
    }

    @Test(expected = IllegalArgumentException::class)
    fun pixelGridRejectsMismatchedCellCount() {
        PixelGrid(width = 4, height = 4, pixels = listOf(0L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun cardStyleRejectsUnexpectedGridDimensions() {
        val json = CustomCardStyle(id = "card-valid-id").toJson()
        json.getJSONObject("default")
            .getJSONObject("top")
            .put("width", CARD_GRID_WIDTH - 1)

        CustomCardStyle.fromJson(json)
    }

    @Test
    fun librariesDeduplicateIdsAndKeepNewestFirst() {
        val old = CustomCardStyle(id = "card-same-id", name = "Old")
        val replacement = old.copy(name = "Replacement")
        val updated = ComponentStyleStore.upsertCardStyle(listOf(old), replacement)

        assertEquals(1, updated.size)
        assertEquals("Replacement", updated.single().name)
        assertEquals(updated, decodeCardStyleLibrary(encodeCardStyleLibrary(updated)))
    }

    @Test
    fun colorParserSupportsRgbAndArgb() {
        assertEquals(0xFF42B9F5L, parseArgbHex("#42B9F5"))
        assertEquals(0x8042B9F5L, parseArgbHex("8042B9F5"))
        assertNull(parseArgbHex("not-a-color"))
        assertEquals("#8042B9F5", formatArgbHex(0x8042B9F5L))
    }

    @Test
    fun floodFillOnlyChangesConnectedEditablePixels() {
        val barrier = 0xFFFFFFFFL
        val fill = 0xFF42B9F5L
        var grid = PixelGrid.blank(5, 3)
        repeat(3) { y -> grid = grid.withPixel(2, y, barrier) }

        val filled = grid.floodFilled(0, 0, fill)

        assertEquals(fill, filled.colorAt(1, 2))
        assertEquals(barrier, filled.colorAt(2, 1))
        assertEquals(TRANSPARENT_PIXEL, filled.colorAt(3, 1))
    }

    @Test
    fun verticalMirrorReversesRowsWithoutChangingDimensions() {
        val color = 0xFF9D7BF7L
        val source = PixelGrid.blank(4, 3).withPixel(1, 0, color)

        val mirrored = source.mirroredVertically()

        assertEquals(4, mirrored.width)
        assertEquals(3, mirrored.height)
        assertEquals(color, mirrored.colorAt(1, 2))
        assertEquals(TRANSPARENT_PIXEL, mirrored.colorAt(1, 0))
    }

    @Test
    fun selectionCanMoveCropPasteAndRotate() {
        val first = 0xFF42B9F5L
        val second = 0xFF9D7BF7L
        val source = PixelGrid.blank(5, 4)
            .withPixel(1, 1, first)
            .withPixel(2, 1, second)
        val selection = PixelSelection(1, 1, 3, 2)

        val cropped = source.cropped(selection)
        val (moved, movedSelection) = source.movedSelection(selection, 1, 1)
        val (rotated, rotatedSelection) = source.rotatedSelection(selection)

        assertEquals(2, cropped.width)
        assertEquals(first, cropped.colorAt(0, 0))
        assertEquals(first, moved.colorAt(2, 2))
        assertEquals(PixelSelection(2, 2, 4, 3), movedSelection)
        assertEquals(1, rotatedSelection.width)
        assertEquals(2, rotatedSelection.height)
        assertEquals(first, rotated.colorAt(1, 1))
        assertEquals(second, rotated.colorAt(1, 2))
        assertEquals(first, PixelGrid.blank(5, 4).pasted(cropped, 0, 0).colorAt(0, 0))
    }

    @Test
    fun componentPresetOverlaysWithoutClearingExistingPixels() {
        val existing = 0xFFFFFFFFL
        val primary = 0xFF5457ECL
        val source = PixelGrid.blank(8, 5).withPixel(4, 4, existing)

        val result = source.withPreset(PixelComponentPreset.DataLine, primary, primary)

        assertEquals(existing, result.colorAt(4, 4))
        assertTrue(result.pixels.any { it == primary })
    }

    @Test
    fun imageQuantizationUsesTransparencyAndNearestPaletteColor() {
        val palette = listOf(TRANSPARENT_PIXEL, 0xFFFF0000L, 0xFF0000FFL)

        assertEquals(TRANSPARENT_PIXEL, quantizePixelColor(0x100000FFL, palette))
        assertEquals(0xFFFF0000L, quantizePixelColor(0xFFFF2200L, palette))
        assertEquals(0xFF0000FFL, quantizePixelColor(0xFF1010EFL, palette))
    }
}
