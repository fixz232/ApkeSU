package me.weishu.kernelsu.ui.util

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val AUDIO_SCHEMES_KEY = "custom_audio_schemes_v1"
private const val MAX_AUDIO_SCHEMES = 12

data class AudioScheme(
    val id: String,
    val name: String,
    val startupSoundUri: String?,
    val startupDurationSeconds: Int,
    val startupVolume: Float,
    val clickSoundUri: String?,
    val clickVolume: Float,
    val backgroundMusicUri: String?,
    val backgroundVolume: Float,
    val settings: AppAudioSettings,
) {
    fun normalized(): AudioScheme = copy(
        id = id.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString(),
        name = name.trim().take(32).takeIf(String::isNotBlank) ?: "Audio scheme",
        startupDurationSeconds = sanitizeCustomStartupSoundDurationSeconds(startupDurationSeconds),
        startupVolume = sanitizeCustomAudioVolume(startupVolume),
        clickVolume = sanitizeCustomAudioVolume(clickVolume),
        backgroundVolume = sanitizeCustomBackgroundMusicVolume(backgroundVolume),
        settings = settings.normalized(),
    )

    fun toJson(): JSONObject = normalized().let { value ->
        JSONObject()
            .put("id", value.id)
            .put("name", value.name)
            .put("startupSoundUri", value.startupSoundUri)
            .put("startupDurationSeconds", value.startupDurationSeconds)
            .put("startupVolume", value.startupVolume.toDouble())
            .put("clickSoundUri", value.clickSoundUri)
            .put("clickVolume", value.clickVolume.toDouble())
            .put("backgroundMusicUri", value.backgroundMusicUri)
            .put("backgroundVolume", value.backgroundVolume.toDouble())
            .put("settings", value.settings.toJson())
    }

    companion object {
        fun fromJson(json: JSONObject): AudioScheme = AudioScheme(
            id = json.optString("id"),
            name = json.optString("name"),
            startupSoundUri = json.optString("startupSoundUri").takeIf(String::isNotBlank),
            startupDurationSeconds = json.optInt(
                "startupDurationSeconds",
                DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
            ),
            startupVolume = json.optDouble("startupVolume", DEFAULT_CUSTOM_AUDIO_VOLUME.toDouble()).toFloat(),
            clickSoundUri = json.optString("clickSoundUri").takeIf(String::isNotBlank),
            clickVolume = json.optDouble("clickVolume", DEFAULT_CUSTOM_AUDIO_VOLUME.toDouble()).toFloat(),
            backgroundMusicUri = json.optString("backgroundMusicUri").takeIf(String::isNotBlank),
            backgroundVolume = json.optDouble(
                "backgroundVolume",
                DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME.toDouble(),
            ).toFloat(),
            settings = AppAudioSettings.fromJson(json.optJSONObject("settings")),
        ).normalized()
    }
}

fun readAudioSchemes(context: Context): List<AudioScheme> {
    val raw = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        .getString(AUDIO_SCHEMES_KEY, null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length().coerceAtMost(MAX_AUDIO_SCHEMES)) {
                val value = array.optJSONObject(index) ?: continue
                add(AudioScheme.fromJson(value))
            }
        }.distinctBy(AudioScheme::id)
    }.getOrDefault(emptyList())
}

fun saveCurrentAudioScheme(context: Context, name: String): AudioScheme? {
    val safeName = name.trim().take(32).takeIf(String::isNotBlank) ?: return null
    val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val scheme = AudioScheme(
        id = UUID.randomUUID().toString(),
        name = safeName,
        startupSoundUri = prefs.getString(CUSTOM_STARTUP_SOUND_URI_KEY, null),
        startupDurationSeconds = prefs.getInt(
            CUSTOM_STARTUP_SOUND_DURATION_SECONDS_KEY,
            DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
        ),
        startupVolume = prefs.getFloat(CUSTOM_STARTUP_SOUND_VOLUME_KEY, DEFAULT_CUSTOM_AUDIO_VOLUME),
        clickSoundUri = prefs.getString(CUSTOM_CLICK_SOUND_URI_KEY, null),
        clickVolume = prefs.getFloat(CUSTOM_CLICK_SOUND_VOLUME_KEY, DEFAULT_CUSTOM_AUDIO_VOLUME),
        backgroundMusicUri = prefs.getString(CUSTOM_BACKGROUND_MUSIC_URI_KEY, null),
        backgroundVolume = prefs.getFloat(
            CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY,
            DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME,
        ),
        settings = readAppAudioSettings(context),
    ).normalized()
    val current = readAudioSchemes(context)
    val updated = (current + scheme).takeLast(MAX_AUDIO_SCHEMES)
    writeAudioSchemes(context, updated)
    releaseUnusedSchemeAudioPermissions(context, current, updated)
    return scheme
}

fun applyAudioScheme(context: Context, scheme: AudioScheme): Boolean {
    val value = scheme.normalized()
    if (value.audioUris().any { !canReadAudioUri(context, it) }) return false
    val editor = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
    editor.apply {
        putOptionalAudioUri(CUSTOM_STARTUP_SOUND_URI_KEY, value.startupSoundUri)
        putInt(CUSTOM_STARTUP_SOUND_DURATION_SECONDS_KEY, value.startupDurationSeconds)
        putFloat(CUSTOM_STARTUP_SOUND_VOLUME_KEY, value.startupVolume)
        putOptionalAudioUri(CUSTOM_CLICK_SOUND_URI_KEY, value.clickSoundUri)
        putFloat(CUSTOM_CLICK_SOUND_VOLUME_KEY, value.clickVolume)
        putOptionalAudioUri(CUSTOM_BACKGROUND_MUSIC_URI_KEY, value.backgroundMusicUri)
        putFloat(CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY, value.backgroundVolume)
        putString(CUSTOM_AUDIO_SETTINGS_KEY, value.settings.toJson().toString())
    }
    return editor.commit()
}

fun deleteAudioScheme(context: Context, id: String): Boolean {
    val current = readAudioSchemes(context)
    val updated = current.filterNot { it.id == id }
    if (updated.size == current.size) return false
    writeAudioSchemes(context, updated)
    releaseUnusedSchemeAudioPermissions(context, current, updated)
    return true
}

fun isAudioUriReferencedBySavedScheme(context: Context, uriString: String?): Boolean {
    return isAudioUriReferencedBySchemes(readAudioSchemes(context), uriString)
}

internal fun isAudioUriReferencedBySchemes(schemes: List<AudioScheme>, uriString: String?): Boolean {
    if (uriString.isNullOrBlank()) return false
    return schemes.any { scheme ->
        uriString == scheme.startupSoundUri ||
            uriString == scheme.clickSoundUri ||
            uriString == scheme.backgroundMusicUri
    }
}

private fun writeAudioSchemes(context: Context, schemes: List<AudioScheme>) {
    val array = JSONArray()
    schemes.takeLast(MAX_AUDIO_SCHEMES).forEach { array.put(it.normalized().toJson()) }
    context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE).edit {
        putString(AUDIO_SCHEMES_KEY, array.toString())
    }
}

private fun AudioScheme.audioUris(): List<String> = listOfNotNull(
    startupSoundUri?.takeIf(String::isNotBlank),
    clickSoundUri?.takeIf(String::isNotBlank),
    backgroundMusicUri?.takeIf(String::isNotBlank),
)

private fun canReadAudioUri(context: Context, uriString: String): Boolean {
    return runCatching {
        context.applicationContext.contentResolver.openAssetFileDescriptor(Uri.parse(uriString), "r")
            ?.use { true }
            ?: false
    }.getOrDefault(false)
}

private fun releaseUnusedSchemeAudioPermissions(
    context: Context,
    previous: List<AudioScheme>,
    current: List<AudioScheme>,
) {
    val active = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE).let { prefs ->
        setOfNotNull(
            prefs.getString(CUSTOM_STARTUP_SOUND_URI_KEY, null),
            prefs.getString(CUSTOM_CLICK_SOUND_URI_KEY, null),
            prefs.getString(CUSTOM_BACKGROUND_MUSIC_URI_KEY, null),
        )
    }
    val retained = current.flatMap(AudioScheme::audioUris).toSet() + active
    previous.flatMap(AudioScheme::audioUris).toSet()
        .filterNot(retained::contains)
        .forEach { releaseCustomAudioReference(context, it) }
}

private fun android.content.SharedPreferences.Editor.putOptionalAudioUri(key: String, value: String?) {
    if (value.isNullOrBlank()) remove(key) else putString(key, value)
}
