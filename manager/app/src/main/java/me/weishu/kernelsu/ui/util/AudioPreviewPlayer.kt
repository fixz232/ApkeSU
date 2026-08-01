package me.weishu.kernelsu.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioPreviewState(
    val uriString: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playing: Boolean = false,
    val paused: Boolean = false,
)

object AudioPreviewPlayer {
    private val mutableState = MutableStateFlow(AudioPreviewState())
    val state: StateFlow<AudioPreviewState> = mutableState.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val audioCoordinator = RuntimeAudioCoordinator(
        onPause = { pause() },
        onResume = { resume() },
    )
    private var trackSettings = AudioTrackSettings()
    private var configuredVolume = 1f
    private var endPositionMs = 0L

    private val ticker = object : Runnable {
        override fun run() {
            val active = player ?: return
            val position = runCatching { active.currentPosition.toLong() }.getOrDefault(0L)
            val duration = runCatching { active.duration.toLong() }.getOrDefault(0L)
            if (endPositionMs > 0L && position >= endPositionMs) {
                if (trackSettings.loop) {
                    runCatching {
                        active.seekTo(trackSettings.trimStartMs.toInt())
                        active.start()
                    }
                } else {
                    stop()
                    return
                }
            }
            applyFade(active, position)
            mutableState.value = mutableState.value.copy(
                positionMs = position,
                durationMs = duration,
                playing = runCatching { active.isPlaying }.getOrDefault(false),
                paused = !runCatching { active.isPlaying }.getOrDefault(false),
            )
            handler.postDelayed(this, 80L)
        }
    }

    fun play(
        context: Context,
        uriString: String?,
        volume: Float,
        settings: AudioTrackSettings,
        onError: ((Throwable?) -> Unit)? = null,
    ) {
        if (uriString.isNullOrBlank()) return
        stop()
        trackSettings = settings.normalized()
        if (!isAudioPlaybackAllowed(context, trackSettings)) return
        configuredVolume = sanitizeCustomAudioVolume(volume)
        val appContext = context.applicationContext
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        if (!requestAudioFocus(appContext, attributes)) return
        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(appContext, uriString.toUri())
                setVolume(if (trackSettings.fadeInMs > 0) 0f else configuredVolume,
                    if (trackSettings.fadeInMs > 0) 0f else configuredVolume)
                setOnPreparedListener { prepared ->
                    val duration = prepared.duration.toLong().coerceAtLeast(0L)
                    val start = trackSettings.trimStartMs.coerceAtMost(duration)
                    val configuredEnd = trackSettings.trimEndMs
                    endPositionMs = if (configuredEnd > start) configuredEnd.coerceAtMost(duration) else duration
                    if (start > 0L) prepared.seekTo(start.toInt())
                    loudnessEnhancer = runCatching {
                        LoudnessEnhancer(prepared.audioSessionId).apply {
                            setTargetGain(if (trackSettings.normalizeVolume) 250 else 0)
                            enabled = trackSettings.normalizeVolume
                        }
                    }.getOrNull()
                    prepared.start()
                    mutableState.value = AudioPreviewState(uriString, start, duration, playing = true)
                    handler.post(ticker)
                }
                setOnCompletionListener {
                    if (trackSettings.loop) {
                        it.seekTo(trackSettings.trimStartMs.toInt())
                        it.start()
                    } else {
                        stop()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    onError?.invoke(null)
                    stop()
                    true
                }
                prepareAsync()
                player = this
            }
        }.onFailure {
            onError?.invoke(it)
            stop()
        }
    }

    fun pause() {
        val active = player ?: return
        runCatching { if (active.isPlaying) active.pause() }
        mutableState.value = mutableState.value.copy(playing = false, paused = true)
    }

    fun resume() {
        val active = player ?: return
        runCatching { active.start() }
        mutableState.value = mutableState.value.copy(playing = true, paused = false)
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    fun seekTo(positionMs: Long) {
        val active = player ?: return
        val duration = mutableState.value.durationMs
        val start = trackSettings.trimStartMs.coerceAtMost(duration.coerceAtLeast(0L))
        val end = endPositionMs.takeIf { it > start } ?: duration.coerceAtLeast(start)
        val safe = positionMs.coerceIn(start, end)
        runCatching { active.seekTo(safe.toInt()) }
        mutableState.value = mutableState.value.copy(positionMs = safe)
    }

    fun stop() {
        handler.removeCallbacks(ticker)
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
        audioCoordinator.release()
        mutableState.value = AudioPreviewState()
    }

    private fun requestAudioFocus(context: Context, attributes: AudioAttributes): Boolean {
        return audioCoordinator.request(
            context = context,
            attributes = attributes,
            focusGain = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            track = trackSettings,
        )
    }

    private fun applyFade(active: MediaPlayer, positionMs: Long) {
        val fadeIn = trackSettings.fadeInMs.toLong()
        val fadeOut = trackSettings.fadeOutMs.toLong()
        val fromStart = (positionMs - trackSettings.trimStartMs).coerceAtLeast(0L)
        val toEnd = (endPositionMs - positionMs).coerceAtLeast(0L)
        val factor = minOf(
            if (fadeIn > 0L) (fromStart.toFloat() / fadeIn).coerceIn(0f, 1f) else 1f,
            if (fadeOut > 0L) (toEnd.toFloat() / fadeOut).coerceIn(0f, 1f) else 1f,
        )
        val volume = configuredVolume * factor
        runCatching { active.setVolume(volume, volume) }
    }
}
