package me.weishu.kernelsu.ui.component.custom

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentStyleModelsTest {
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
        )

        val packageJson = source.toJson(includeLocalImageUri = false)
        val restored = CustomSwitchStyle.fromJson(packageJson, allowLocalImageUri = false)

        assertTrue(packageJson.isNull("image_uri"))
        assertNull(restored.imageUri)
        assertEquals(source.imageSha256, restored.imageSha256)
        assertEquals(CustomSwitchSource.Image, restored.source)
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
}
