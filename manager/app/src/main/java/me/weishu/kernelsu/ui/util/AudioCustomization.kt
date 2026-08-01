package me.weishu.kernelsu.ui.util

import android.app.NotificationManager
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import org.json.JSONObject

const val CUSTOM_AUDIO_SETTINGS_KEY = "custom_audio_settings_v2"

data class AudioTrackSettings(
    val enabled: Boolean = true,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val fadeInMs: Int = 0,
    val fadeOutMs: Int = 0,
    val loop: Boolean = false,
    val normalizeVolume: Boolean = true,
) {
    fun normalized(): AudioTrackSettings = copy(
        trimStartMs = trimStartMs.coerceAtLeast(0L),
        trimEndMs = trimEndMs.coerceAtLeast(0L),
        fadeInMs = fadeInMs.coerceIn(0, 5_000),
        fadeOutMs = fadeOutMs.coerceIn(0, 5_000),
    )

    fun toJson(): JSONObject = normalized().let { value ->
        JSONObject()
            .put("enabled", value.enabled)
            .put("trimStartMs", value.trimStartMs)
            .put("trimEndMs", value.trimEndMs)
            .put("fadeInMs", value.fadeInMs)
            .put("fadeOutMs", value.fadeOutMs)
            .put("loop", value.loop)
            .put("normalizeVolume", value.normalizeVolume)
    }

    companion object {
        fun fromJson(json: JSONObject?, defaults: AudioTrackSettings): AudioTrackSettings {
            if (json == null) return defaults.normalized()
            return AudioTrackSettings(
                enabled = json.optBoolean("enabled", defaults.enabled),
                trimStartMs = json.optLong("trimStartMs", defaults.trimStartMs),
                trimEndMs = json.optLong("trimEndMs", defaults.trimEndMs),
                fadeInMs = json.optInt("fadeInMs", defaults.fadeInMs),
                fadeOutMs = json.optInt("fadeOutMs", defaults.fadeOutMs),
                loop = json.optBoolean("loop", defaults.loop),
                normalizeVolume = json.optBoolean("normalizeVolume", defaults.normalizeVolume),
            ).normalized()
        }
    }
}

data class AppAudioSettings(
    val masterEnabled: Boolean = true,
    val respectSilentMode: Boolean = true,
    val respectDoNotDisturb: Boolean = true,
    val pauseOnHeadsetDisconnect: Boolean = true,
    val hapticWithClick: Boolean = true,
    val startup: AudioTrackSettings = AudioTrackSettings(fadeInMs = 120, fadeOutMs = 180),
    val click: AudioTrackSettings = AudioTrackSettings(normalizeVolume = true),
    val background: AudioTrackSettings = AudioTrackSettings(loop = true, fadeInMs = 350, fadeOutMs = 500),
) {
    fun normalized(): AppAudioSettings = copy(
        startup = startup.normalized(),
        click = click.normalized(),
        background = background.normalized(),
    )

    fun toJson(): JSONObject = normalized().let { value ->
        JSONObject()
            .put("masterEnabled", value.masterEnabled)
            .put("respectSilentMode", value.respectSilentMode)
            .put("respectDoNotDisturb", value.respectDoNotDisturb)
            .put("pauseOnHeadsetDisconnect", value.pauseOnHeadsetDisconnect)
            .put("hapticWithClick", value.hapticWithClick)
            .put("startup", value.startup.toJson())
            .put("click", value.click.toJson())
            .put("background", value.background.toJson())
    }

    companion object {
        fun fromJson(json: JSONObject?): AppAudioSettings {
            val defaults = AppAudioSettings()
            if (json == null) return defaults
            return AppAudioSettings(
                masterEnabled = json.optBoolean("masterEnabled", defaults.masterEnabled),
                respectSilentMode = json.optBoolean("respectSilentMode", defaults.respectSilentMode),
                respectDoNotDisturb = json.optBoolean("respectDoNotDisturb", defaults.respectDoNotDisturb),
                pauseOnHeadsetDisconnect = json.optBoolean(
                    "pauseOnHeadsetDisconnect",
                    defaults.pauseOnHeadsetDisconnect,
                ),
                hapticWithClick = json.optBoolean("hapticWithClick", defaults.hapticWithClick),
                startup = AudioTrackSettings.fromJson(json.optJSONObject("startup"), defaults.startup),
                click = AudioTrackSettings.fromJson(json.optJSONObject("click"), defaults.click),
                background = AudioTrackSettings.fromJson(json.optJSONObject("background"), defaults.background),
            ).normalized()
        }
    }
}

fun readAppAudioSettings(context: Context): AppAudioSettings {
    val raw = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        .getString(CUSTOM_AUDIO_SETTINGS_KEY, null)
    return AppAudioSettings.fromJson(raw?.let { runCatching { JSONObject(it) }.getOrNull() })
}

fun setAppAudioSettings(context: Context, settings: AppAudioSettings) {
    context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE).edit {
        putString(CUSTOM_AUDIO_SETTINGS_KEY, settings.normalized().toJson().toString())
    }
}

fun isAudioPlaybackAllowed(
    context: Context,
    track: AudioTrackSettings,
): Boolean {
    val settings = readAppAudioSettings(context)
    if (!settings.masterEnabled || !track.enabled) return false
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    if (settings.respectSilentMode && audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return false
    if (settings.respectDoNotDisturb) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val filter = runCatching { notificationManager?.currentInterruptionFilter }.getOrNull()
        if (filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
            filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
        ) return false
    }
    return true
}

fun hasConnectedHeadphones(context: Context): Boolean {
    val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
    return manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
        device.type in setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
        )
    }
}

internal class RuntimeAudioCoordinator(
    private val onPause: () -> Unit,
    private val onResume: () -> Unit,
) {
    private var appContext: Context? = null
    private var manager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var receiver: BroadcastReceiver? = null
    private var track = AudioTrackSettings()
    private var pausedBySystem = false

    fun request(
        context: Context,
        attributes: AudioAttributes,
        focusGain: Int,
        track: AudioTrackSettings,
    ): Boolean {
        release()
        val safeContext = context.applicationContext
        val audioManager = safeContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return true
        this.appContext = safeContext
        this.manager = audioManager
        this.track = track.normalized()
        val request = AudioFocusRequest.Builder(focusGain)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseForSystem()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> pauseForSystem()
                    AudioManager.AUDIOFOCUS_GAIN -> resumeAfterSystem()
                }
            }
            .build()
        if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            appContext = null
            manager = null
            return false
        }
        focusRequest = request
        registerSystemReceiver(safeContext)
        return true
    }

    fun release() {
        receiver?.let { current ->
            runCatching { appContext?.unregisterReceiver(current) }
        }
        receiver = null
        focusRequest?.let { request ->
            runCatching { manager?.abandonAudioFocusRequest(request) }
        }
        focusRequest = null
        manager = null
        appContext = null
        pausedBySystem = false
    }

    private fun pauseForSystem() {
        pausedBySystem = true
        onPause()
    }

    private fun resumeAfterSystem() {
        val context = appContext ?: return
        if (!pausedBySystem || !isAudioPlaybackAllowed(context, track)) return
        pausedBySystem = false
        onResume()
    }

    @Suppress("DEPRECATION")
    private fun registerSystemReceiver(context: Context) {
        val settings = readAppAudioSettings(context)
        val filter = IntentFilter().apply {
            if (settings.pauseOnHeadsetDisconnect) addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (settings.respectSilentMode) addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            if (settings.respectDoNotDisturb) addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        if (filter.countActions() == 0) return
        val current = object : BroadcastReceiver() {
            override fun onReceive(receiveContext: Context, intent: Intent) {
                if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    pauseForSystem()
                    return
                }
                if (!isAudioPlaybackAllowed(receiveContext, track)) pauseForSystem()
                else resumeAfterSystem()
            }
        }
        receiver = current
        ContextCompat.registerReceiver(context, current, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}
