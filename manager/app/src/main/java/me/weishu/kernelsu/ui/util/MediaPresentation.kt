package me.weishu.kernelsu.ui.util

import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import org.json.JSONObject
import kotlin.math.roundToInt

const val DEFAULT_MEDIA_BRIGHTNESS = 0f
const val DEFAULT_MEDIA_CONTRAST = 1f
const val DEFAULT_MEDIA_SATURATION = 1f
const val DEFAULT_MEDIA_TEMPERATURE = 0f
const val DEFAULT_MEDIA_OPACITY = 1f
const val DEFAULT_MEDIA_BLUR_RADIUS = 0f
const val DEFAULT_MEDIA_OVERLAY_ALPHA = 0f
const val DEFAULT_MEDIA_NOISE_ALPHA = 0f

enum class MediaMotionStyle(val value: String) {
    None("none"),
    Parallax("parallax"),
    SlowZoom("slow_zoom"),
    SlowPan("slow_pan");

    companion object {
        fun fromValue(value: String?): MediaMotionStyle = entries.firstOrNull { it.value == value } ?: None
    }
}

enum class MediaVariantMode(val value: String) {
    FollowSystem("follow_system"),
    Schedule("schedule"),
    Random("random");

    companion object {
        fun fromValue(value: String?): MediaVariantMode = entries.firstOrNull { it.value == value } ?: FollowSystem
    }
}

@Immutable
data class MediaTransform(
    val quarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
) {
    fun normalized(): MediaTransform = copy(quarterTurns = ((quarterTurns % 4) + 4) % 4)

    fun toJson(): JSONObject = JSONObject()
        .put("quarterTurns", normalized().quarterTurns)
        .put("flipHorizontal", flipHorizontal)

    companion object {
        fun fromJson(json: JSONObject?): MediaTransform = MediaTransform(
            quarterTurns = json?.optInt("quarterTurns", 0) ?: 0,
            flipHorizontal = json?.optBoolean("flipHorizontal", false) ?: false,
        ).normalized()
    }
}

@Immutable
data class MediaVisualSettings(
    val brightness: Float = DEFAULT_MEDIA_BRIGHTNESS,
    val contrast: Float = DEFAULT_MEDIA_CONTRAST,
    val saturation: Float = DEFAULT_MEDIA_SATURATION,
    val temperature: Float = DEFAULT_MEDIA_TEMPERATURE,
    val opacity: Float = DEFAULT_MEDIA_OPACITY,
    val blurRadius: Float = DEFAULT_MEDIA_BLUR_RADIUS,
    val overlayAlpha: Float = DEFAULT_MEDIA_OVERLAY_ALPHA,
    val noiseAlpha: Float = DEFAULT_MEDIA_NOISE_ALPHA,
    val transform: MediaTransform = MediaTransform(),
    val motionStyle: MediaMotionStyle = MediaMotionStyle.None,
) {
    fun normalized(): MediaVisualSettings = copy(
        brightness = brightness.sanitize(DEFAULT_MEDIA_BRIGHTNESS, -0.6f, 0.6f),
        contrast = contrast.sanitize(DEFAULT_MEDIA_CONTRAST, 0.5f, 1.8f),
        saturation = saturation.sanitize(DEFAULT_MEDIA_SATURATION, 0f, 2f),
        temperature = temperature.sanitize(DEFAULT_MEDIA_TEMPERATURE, -1f, 1f),
        opacity = opacity.sanitize(DEFAULT_MEDIA_OPACITY, 0.1f, 1f),
        blurRadius = blurRadius.sanitize(DEFAULT_MEDIA_BLUR_RADIUS, 0f, 28f),
        overlayAlpha = overlayAlpha.sanitize(DEFAULT_MEDIA_OVERLAY_ALPHA, 0f, 0.82f),
        noiseAlpha = noiseAlpha.sanitize(DEFAULT_MEDIA_NOISE_ALPHA, 0f, 0.22f),
        transform = transform.normalized(),
    )

    fun toJson(): JSONObject {
        val value = normalized()
        return JSONObject()
            .put("brightness", value.brightness.toDouble())
            .put("contrast", value.contrast.toDouble())
            .put("saturation", value.saturation.toDouble())
            .put("temperature", value.temperature.toDouble())
            .put("opacity", value.opacity.toDouble())
            .put("blurRadius", value.blurRadius.toDouble())
            .put("overlayAlpha", value.overlayAlpha.toDouble())
            .put("noiseAlpha", value.noiseAlpha.toDouble())
            .put("transform", value.transform.toJson())
            .put("motionStyle", value.motionStyle.value)
    }

    companion object {
        fun fromJson(json: JSONObject?, defaults: MediaVisualSettings = MediaVisualSettings()): MediaVisualSettings {
            if (json == null) return defaults.normalized()
            return MediaVisualSettings(
                brightness = json.optDouble("brightness", defaults.brightness.toDouble()).toFloat(),
                contrast = json.optDouble("contrast", defaults.contrast.toDouble()).toFloat(),
                saturation = json.optDouble("saturation", defaults.saturation.toDouble()).toFloat(),
                temperature = json.optDouble("temperature", defaults.temperature.toDouble()).toFloat(),
                opacity = json.optDouble("opacity", defaults.opacity.toDouble()).toFloat(),
                blurRadius = json.optDouble("blurRadius", defaults.blurRadius.toDouble()).toFloat(),
                overlayAlpha = json.optDouble("overlayAlpha", defaults.overlayAlpha.toDouble()).toFloat(),
                noiseAlpha = json.optDouble("noiseAlpha", defaults.noiseAlpha.toDouble()).toFloat(),
                transform = MediaTransform.fromJson(json.optJSONObject("transform")),
                motionStyle = MediaMotionStyle.fromValue(json.optString("motionStyle")),
            ).normalized()
        }
    }
}

@Immutable
data class ResponsiveCropSet(
    val portrait: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
    val landscape: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
    val square: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
) {
    fun normalized(): ResponsiveCropSet = copy(
        portrait = sanitizeCustomWallpaperCrop(portrait),
        landscape = sanitizeCustomWallpaperCrop(landscape),
        square = sanitizeCustomWallpaperCrop(square),
    )

    fun forAspectRatio(aspectRatio: Float): CustomWallpaperCrop = when {
        aspectRatio > 1.2f -> landscape
        aspectRatio < 0.82f -> portrait
        else -> square
    }

    fun withCropForAspectRatio(
        aspectRatio: Float,
        crop: CustomWallpaperCrop,
    ): ResponsiveCropSet {
        val safeCrop = sanitizeCustomWallpaperCrop(crop)
        return when {
            aspectRatio > 1.2f -> copy(landscape = safeCrop)
            aspectRatio < 0.82f -> copy(portrait = safeCrop)
            else -> copy(square = safeCrop)
        }.normalized()
    }

    fun toJson(): JSONObject {
        val value = normalized()
        return JSONObject()
            .put("portrait", value.portrait.toMediaJson())
            .put("landscape", value.landscape.toMediaJson())
            .put("square", value.square.toMediaJson())
    }

    companion object {
        fun fromJson(json: JSONObject?, fallback: CustomWallpaperCrop): ResponsiveCropSet {
            if (json == null) return ResponsiveCropSet(fallback, fallback, fallback).normalized()
            return ResponsiveCropSet(
                portrait = cropFromMediaJson(json.optJSONObject("portrait"), fallback),
                landscape = cropFromMediaJson(json.optJSONObject("landscape"), fallback),
                square = cropFromMediaJson(json.optJSONObject("square"), fallback),
            ).normalized()
        }
    }
}

@Immutable
data class MediaVariantSettings(
    val mode: MediaVariantMode = MediaVariantMode.FollowSystem,
    val dayStartMinutes: Int = 7 * 60,
    val nightStartMinutes: Int = 19 * 60,
    val randomIntervalMinutes: Int = 30,
) {
    fun normalized(): MediaVariantSettings = copy(
        dayStartMinutes = dayStartMinutes.coerceIn(0, 1439),
        nightStartMinutes = nightStartMinutes.coerceIn(0, 1439),
        randomIntervalMinutes = randomIntervalMinutes.coerceIn(5, 24 * 60),
    )

    fun toJson(): JSONObject {
        val value = normalized()
        return JSONObject()
            .put("mode", value.mode.value)
            .put("dayStartMinutes", value.dayStartMinutes)
            .put("nightStartMinutes", value.nightStartMinutes)
            .put("randomIntervalMinutes", value.randomIntervalMinutes)
    }

    companion object {
        fun fromJson(json: JSONObject?): MediaVariantSettings = MediaVariantSettings(
            mode = MediaVariantMode.fromValue(json?.optString("mode")),
            dayStartMinutes = json?.optInt("dayStartMinutes", 7 * 60) ?: 7 * 60,
            nightStartMinutes = json?.optInt("nightStartMinutes", 19 * 60) ?: 19 * 60,
            randomIntervalMinutes = json?.optInt("randomIntervalMinutes", 30) ?: 30,
        ).normalized()
    }
}

data class MediaVisualPreferenceKeys(val prefix: String) {
    val brightness = "${prefix}_brightness"
    val contrast = "${prefix}_contrast"
    val saturation = "${prefix}_saturation"
    val temperature = "${prefix}_temperature"
    val opacity = "${prefix}_opacity"
    val blurRadius = "${prefix}_blur_radius"
    val overlayAlpha = "${prefix}_overlay_alpha"
    val noiseAlpha = "${prefix}_noise_alpha"
    val quarterTurns = "${prefix}_quarter_turns"
    val flipHorizontal = "${prefix}_flip_horizontal"
    val motionStyle = "${prefix}_motion_style"

    val all: Set<String> = setOf(
        brightness,
        contrast,
        saturation,
        temperature,
        opacity,
        blurRadius,
        overlayAlpha,
        noiseAlpha,
        quarterTurns,
        flipHorizontal,
        motionStyle,
    )
}

fun SharedPreferences.readMediaVisualSettings(
    keys: MediaVisualPreferenceKeys,
    defaults: MediaVisualSettings = MediaVisualSettings(),
): MediaVisualSettings = MediaVisualSettings(
    brightness = getFloat(keys.brightness, defaults.brightness),
    contrast = getFloat(keys.contrast, defaults.contrast),
    saturation = getFloat(keys.saturation, defaults.saturation),
    temperature = getFloat(keys.temperature, defaults.temperature),
    opacity = getFloat(keys.opacity, defaults.opacity),
    blurRadius = getFloat(keys.blurRadius, defaults.blurRadius),
    overlayAlpha = getFloat(keys.overlayAlpha, defaults.overlayAlpha),
    noiseAlpha = getFloat(keys.noiseAlpha, defaults.noiseAlpha),
    transform = MediaTransform(
        quarterTurns = getInt(keys.quarterTurns, defaults.transform.quarterTurns),
        flipHorizontal = getBoolean(keys.flipHorizontal, defaults.transform.flipHorizontal),
    ),
    motionStyle = MediaMotionStyle.fromValue(getString(keys.motionStyle, defaults.motionStyle.value)),
).normalized()

fun SharedPreferences.Editor.putMediaVisualSettings(
    keys: MediaVisualPreferenceKeys,
    settings: MediaVisualSettings,
): SharedPreferences.Editor {
    val value = settings.normalized()
    return putFloat(keys.brightness, value.brightness)
        .putFloat(keys.contrast, value.contrast)
        .putFloat(keys.saturation, value.saturation)
        .putFloat(keys.temperature, value.temperature)
        .putFloat(keys.opacity, value.opacity)
        .putFloat(keys.blurRadius, value.blurRadius)
        .putFloat(keys.overlayAlpha, value.overlayAlpha)
        .putFloat(keys.noiseAlpha, value.noiseAlpha)
        .putInt(keys.quarterTurns, value.transform.quarterTurns)
        .putBoolean(keys.flipHorizontal, value.transform.flipHorizontal)
        .putString(keys.motionStyle, value.motionStyle.value)
}

fun SharedPreferences.Editor.removeMediaVisualSettings(keys: MediaVisualPreferenceKeys): SharedPreferences.Editor {
    keys.all.forEach(::remove)
    return this
}

internal fun centeredCropForAspect(
    imageWidth: Int,
    imageHeight: Int,
    targetAspectRatio: Float,
    focusX: Float = 0.5f,
    focusY: Float = 0.5f,
): CustomWallpaperCrop {
    if (imageWidth <= 0 || imageHeight <= 0 || targetAspectRatio <= 0f) {
        return DEFAULT_CUSTOM_WALLPAPER_CROP
    }
    val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()
    val cropWidth: Float
    val cropHeight: Float
    if (imageAspect > targetAspectRatio) {
        cropHeight = 1f
        cropWidth = (targetAspectRatio / imageAspect).coerceIn(0.05f, 1f)
    } else {
        cropWidth = 1f
        cropHeight = (imageAspect / targetAspectRatio).coerceIn(0.05f, 1f)
    }
    val left = (focusX.coerceIn(0f, 1f) - cropWidth / 2f).coerceIn(0f, 1f - cropWidth)
    val top = (focusY.coerceIn(0f, 1f) - cropHeight / 2f).coerceIn(0f, 1f - cropHeight)
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(left, top, left + cropWidth, top + cropHeight)
    )
}

internal fun generateResponsiveCrops(
    imageWidth: Int,
    imageHeight: Int,
    focusX: Float = 0.5f,
    focusY: Float = 0.5f,
): ResponsiveCropSet = ResponsiveCropSet(
    portrait = centeredCropForAspect(imageWidth, imageHeight, 9f / 16f, focusX, focusY),
    landscape = centeredCropForAspect(imageWidth, imageHeight, 16f / 9f, focusX, focusY),
    square = centeredCropForAspect(imageWidth, imageHeight, 1f, focusX, focusY),
)

private fun Float.sanitize(default: Float, min: Float, max: Float): Float {
    return if (isFinite()) coerceIn(min, max) else default
}

internal fun Float.toPreferencePercent(): Int = (coerceIn(0f, 1f) * 100f).roundToInt()

private fun CustomWallpaperCrop.toMediaJson(): JSONObject = JSONObject()
    .put("left", left.toDouble())
    .put("top", top.toDouble())
    .put("right", right.toDouble())
    .put("bottom", bottom.toDouble())

private fun cropFromMediaJson(json: JSONObject?, fallback: CustomWallpaperCrop): CustomWallpaperCrop {
    if (json == null) return sanitizeCustomWallpaperCrop(fallback)
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = json.optDouble("left", fallback.left.toDouble()).toFloat(),
            top = json.optDouble("top", fallback.top.toDouble()).toFloat(),
            right = json.optDouble("right", fallback.right.toDouble()).toFloat(),
            bottom = json.optDouble("bottom", fallback.bottom.toDouble()).toFloat(),
        )
    )
}
