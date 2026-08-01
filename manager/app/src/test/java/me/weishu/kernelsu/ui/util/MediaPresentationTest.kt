package me.weishu.kernelsu.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPresentationTest {

    @Test
    fun withCropForAspectRatio_updatesOnlyMatchingCrop() {
        val portrait = CustomWallpaperCrop(0.10f, 0.10f, 0.70f, 0.90f)
        val landscape = CustomWallpaperCrop(0.05f, 0.20f, 0.95f, 0.80f)
        val square = CustomWallpaperCrop(0.15f, 0.15f, 0.85f, 0.85f)
        val replacement = CustomWallpaperCrop(0.20f, 0.25f, 0.80f, 0.75f)
        val crops = ResponsiveCropSet(portrait, landscape, square)

        assertEquals(
            crops.copy(portrait = replacement),
            crops.withCropForAspectRatio(0.75f, replacement),
        )
        assertEquals(
            crops.copy(landscape = replacement),
            crops.withCropForAspectRatio(1.5f, replacement),
        )
        assertEquals(
            crops.copy(square = replacement),
            crops.withCropForAspectRatio(1f, replacement),
        )
    }

    @Test
    fun withCropForAspectRatio_sanitizesUntrustedCrop() {
        val invalid = CustomWallpaperCrop(-1f, Float.NaN, 4f, Float.POSITIVE_INFINITY)

        val updated = ResponsiveCropSet().withCropForAspectRatio(1f, invalid)

        assertEquals(sanitizeCustomWallpaperCrop(invalid), updated.square)
    }

    @Test
    fun mediaVisualSettings_jsonRoundTripPreservesBlurAndImageAdjustments() {
        val expected = MediaVisualSettings(
            brightness = -0.18f,
            contrast = 1.24f,
            saturation = 0.76f,
            temperature = -0.15f,
            opacity = 0.82f,
            blurRadius = 13.5f,
            overlayAlpha = 0.36f,
            noiseAlpha = 0.04f,
            transform = MediaTransform(quarterTurns = 1, flipHorizontal = true),
            motionStyle = MediaMotionStyle.SlowPan,
        ).normalized()

        assertEquals(expected, MediaVisualSettings.fromJson(expected.toJson()))
    }

    @Test
    fun mediaVisualPreferenceKeys_includeBlurForLiveStateRefresh() {
        val keys = MediaVisualPreferenceKeys("home_reboot_menu")

        assertTrue(keys.blurRadius in keys.all)
        assertEquals(keys.all.size, keys.all.toSet().size)
    }
}
