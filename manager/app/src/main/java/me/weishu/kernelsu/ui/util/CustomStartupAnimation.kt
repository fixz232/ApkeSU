package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import androidx.core.net.toUri
import org.json.JSONObject
import org.json.JSONArray
import java.util.Locale
import java.util.UUID

const val CUSTOM_STARTUP_ANIMATION_URI_KEY = "custom_startup_animation_uri"
const val CUSTOM_STARTUP_ANIMATION_SETTINGS_KEY = "custom_startup_animation_settings_v2"
private const val CUSTOM_STARTUP_ANIMATION_LIBRARY_KEY = "custom_startup_animation_library_v1"
private const val MAX_STARTUP_ANIMATION_PRESETS = 8
const val CUSTOM_STARTUP_ANIMATION_GIF_MIME_TYPE = "image/gif"
const val MAX_STARTUP_ANIMATION_DURATION_MS = 10_000L

enum class StartupAnimationScaleMode(val value: String) {
    Fit("fit"),
    Fill("fill"),
    Crop("crop");

    companion object {
        fun fromValue(value: String?): StartupAnimationScaleMode {
            return entries.firstOrNull { it.value == value } ?: Fit
        }
    }
}

@Immutable
data class StartupAnimationSettings(
    val scaleMode: StartupAnimationScaleMode = StartupAnimationScaleMode.Fit,
    val backgroundArgb: Long = 0xFF000000,
    val brightness: Float = 1f,
    val playbackSpeed: Float = 1f,
    val durationMillis: Long = 5_000L,
    val allowTapSkip: Boolean = true,
    val allowSwipeSkip: Boolean = true,
    val portraitCrop: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
    val landscapeCrop: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
    val syncStartupSound: Boolean = true,
) {
    fun normalized(): StartupAnimationSettings = copy(
        backgroundArgb = backgroundArgb and 0xFFFFFFFFL,
        brightness = brightness.takeIf(Float::isFinite)?.coerceIn(0.35f, 1.35f) ?: 1f,
        playbackSpeed = playbackSpeed.takeIf(Float::isFinite)?.coerceIn(0.5f, 2f) ?: 1f,
        durationMillis = durationMillis.coerceIn(500L, MAX_STARTUP_ANIMATION_DURATION_MS),
        portraitCrop = sanitizeCustomWallpaperCrop(portraitCrop),
        landscapeCrop = sanitizeCustomWallpaperCrop(landscapeCrop),
    )

    fun cropForViewport(width: Int, height: Int): CustomWallpaperCrop {
        return if (width > height) landscapeCrop else portraitCrop
    }

    fun toJson(): JSONObject = normalized().let { value ->
        JSONObject()
            .put("scaleMode", value.scaleMode.value)
            .put("backgroundArgb", value.backgroundArgb)
            .put("brightness", value.brightness.toDouble())
            .put("playbackSpeed", value.playbackSpeed.toDouble())
            .put("durationMillis", value.durationMillis)
            .put("allowTapSkip", value.allowTapSkip)
            .put("allowSwipeSkip", value.allowSwipeSkip)
            .put("portraitCrop", value.portraitCrop.toStartupAnimationJson())
            .put("landscapeCrop", value.landscapeCrop.toStartupAnimationJson())
            .put("syncStartupSound", value.syncStartupSound)
    }

    companion object {
        fun fromJson(json: JSONObject?): StartupAnimationSettings {
            if (json == null) return StartupAnimationSettings()
            val defaults = StartupAnimationSettings()
            return StartupAnimationSettings(
                scaleMode = StartupAnimationScaleMode.fromValue(json.optString("scaleMode")),
                backgroundArgb = json.optLong("backgroundArgb", defaults.backgroundArgb),
                brightness = json.optDouble("brightness", defaults.brightness.toDouble()).toFloat(),
                playbackSpeed = json.optDouble("playbackSpeed", defaults.playbackSpeed.toDouble()).toFloat(),
                durationMillis = json.optLong("durationMillis", defaults.durationMillis),
                allowTapSkip = json.optBoolean("allowTapSkip", defaults.allowTapSkip),
                allowSwipeSkip = json.optBoolean("allowSwipeSkip", defaults.allowSwipeSkip),
                portraitCrop = json.optJSONObject("portraitCrop").toStartupAnimationCrop(defaults.portraitCrop),
                landscapeCrop = json.optJSONObject("landscapeCrop").toStartupAnimationCrop(defaults.landscapeCrop),
                syncStartupSound = json.optBoolean("syncStartupSound", defaults.syncStartupSound),
            ).normalized()
        }
    }
}

data class StartupAnimationPreset(
    val id: String,
    val name: String,
    val uriString: String,
    val settings: StartupAnimationSettings,
) {
    fun normalized(): StartupAnimationPreset = copy(
        id = id.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString(),
        name = name.trim().take(32).takeIf(String::isNotBlank) ?: "Startup animation",
        uriString = uriString.trim(),
        settings = settings.normalized(),
    )

    fun toJson(): JSONObject = normalized().let { value ->
        JSONObject()
            .put("id", value.id)
            .put("name", value.name)
            .put("uri", value.uriString)
            .put("settings", value.settings.toJson())
    }

    companion object {
        fun fromJson(json: JSONObject): StartupAnimationPreset? {
            val uri = json.optString("uri").takeIf(String::isNotBlank) ?: return null
            return StartupAnimationPreset(
                id = json.optString("id"),
                name = json.optString("name"),
                uriString = uri,
                settings = StartupAnimationSettings.fromJson(json.optJSONObject("settings")),
            ).normalized()
        }
    }
}

fun readStartupAnimationSettings(context: Context): StartupAnimationSettings {
    val raw = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        .getString(CUSTOM_STARTUP_ANIMATION_SETTINGS_KEY, null)
    return StartupAnimationSettings.fromJson(raw?.let { runCatching { JSONObject(it) }.getOrNull() })
}

fun setStartupAnimationSettings(context: Context, settings: StartupAnimationSettings) {
    context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE).edit {
        putString(CUSTOM_STARTUP_ANIMATION_SETTINGS_KEY, settings.normalized().toJson().toString())
    }
}

fun readStartupAnimationPresets(context: Context): List<StartupAnimationPreset> {
    val raw = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        .getString(CUSTOM_STARTUP_ANIMATION_LIBRARY_KEY, null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length().coerceAtMost(MAX_STARTUP_ANIMATION_PRESETS)) {
                StartupAnimationPreset.fromJson(array.optJSONObject(index) ?: continue)?.let(::add)
            }
        }.distinctBy(StartupAnimationPreset::id)
    }.getOrDefault(emptyList())
}

fun saveCurrentStartupAnimationPreset(context: Context, name: String): StartupAnimationPreset? {
    val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val uri = prefs.getString(CUSTOM_STARTUP_ANIMATION_URI_KEY, null)?.takeIf(String::isNotBlank) ?: return null
    val preset = StartupAnimationPreset(
        id = UUID.randomUUID().toString(),
        name = name,
        uriString = uri,
        settings = readStartupAnimationSettings(context),
    ).normalized()
    writeStartupAnimationPresets(context, (readStartupAnimationPresets(context) + preset).takeLast(
        MAX_STARTUP_ANIMATION_PRESETS
    ))
    return preset
}

fun applyStartupAnimationPreset(context: Context, preset: StartupAnimationPreset): Boolean {
    val value = preset.normalized()
    if (value.uriString.isBlank()) return false
    return context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        .edit()
        .putString(CUSTOM_STARTUP_ANIMATION_URI_KEY, value.uriString)
        .putString(CUSTOM_STARTUP_ANIMATION_SETTINGS_KEY, value.settings.toJson().toString())
        .commit()
}

fun deleteStartupAnimationPreset(context: Context, id: String): Boolean {
    val current = readStartupAnimationPresets(context)
    val updated = current.filterNot { it.id == id }
    if (current.size == updated.size) return false
    writeStartupAnimationPresets(context, updated)
    return true
}

fun isStartupAnimationUriReferencedByPreset(context: Context, uriString: String?): Boolean {
    return isStartupAnimationUriReferencedByPresets(readStartupAnimationPresets(context), uriString)
}

internal fun isStartupAnimationUriReferencedByPresets(
    presets: List<StartupAnimationPreset>,
    uriString: String?,
): Boolean {
    if (uriString.isNullOrBlank()) return false
    return presets.any { it.uriString == uriString }
}

private fun writeStartupAnimationPresets(context: Context, presets: List<StartupAnimationPreset>) {
    val array = JSONArray()
    presets.takeLast(MAX_STARTUP_ANIMATION_PRESETS).forEach { array.put(it.normalized().toJson()) }
    context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE).edit {
        putString(CUSTOM_STARTUP_ANIMATION_LIBRARY_KEY, array.toString())
    }
}

val CUSTOM_STARTUP_ANIMATION_MIME_TYPES = arrayOf(
    "image/*",
    CUSTOM_STARTUP_ANIMATION_GIF_MIME_TYPE,
    "video/*",
)

fun takePersistableStartupAnimationReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

fun releasePersistableStartupAnimationReadPermission(context: Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    runCatching {
        context.contentResolver.releasePersistableUriPermission(
            uriString.toUri(),
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

fun isCustomStartupAnimationVideo(context: Context, uri: Uri): Boolean {
    val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    if (mimeType?.startsWith("video/") == true) return true

    return hasVideoExtension(uri.toString()) ||
        hasVideoExtension(queryDisplayName(context, uri))
}

fun isCustomStartupAnimationGif(context: Context, uri: Uri): Boolean {
    val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    if (mimeType.equals(CUSTOM_STARTUP_ANIMATION_GIF_MIME_TYPE, ignoreCase = true)) return true

    return hasGifExtension(uri.toString()) ||
        hasGifExtension(queryDisplayName(context, uri))
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex < 0) null else cursor.getString(nameIndex)
        }
    }.getOrNull()
}

private fun hasVideoExtension(value: String?): Boolean {
    val text = value?.lowercase(Locale.ROOT) ?: return false
    return text.endsWith(".mp4") ||
        text.endsWith(".webm") ||
        text.endsWith(".mkv") ||
        text.endsWith(".3gp") ||
        text.endsWith(".mov") ||
        text.endsWith(".video")
}

private fun hasGifExtension(value: String?): Boolean {
    val text = value?.lowercase(Locale.ROOT) ?: return false
    return text.endsWith(".gif")
}

private fun CustomWallpaperCrop.toStartupAnimationJson(): JSONObject = JSONObject()
    .put("left", left.toDouble())
    .put("top", top.toDouble())
    .put("right", right.toDouble())
    .put("bottom", bottom.toDouble())

private fun JSONObject?.toStartupAnimationCrop(fallback: CustomWallpaperCrop): CustomWallpaperCrop {
    if (this == null) return fallback
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = optDouble("left", fallback.left.toDouble()).toFloat(),
            top = optDouble("top", fallback.top.toDouble()).toFloat(),
            right = optDouble("right", fallback.right.toDouble()).toFloat(),
            bottom = optDouble("bottom", fallback.bottom.toDouble()).toFloat(),
        )
    )
}
