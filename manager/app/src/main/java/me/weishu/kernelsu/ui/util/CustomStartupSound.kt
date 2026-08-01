package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

const val CUSTOM_STARTUP_SOUND_URI_KEY = "custom_startup_sound_uri"
const val CUSTOM_STARTUP_SOUND_DURATION_SECONDS_KEY = "custom_startup_sound_duration_seconds"
const val CUSTOM_STARTUP_SOUND_VOLUME_KEY = "custom_startup_sound_volume"
const val CUSTOM_CLICK_SOUND_URI_KEY = "custom_click_sound_uri"
const val CUSTOM_CLICK_SOUND_VOLUME_KEY = "custom_click_sound_volume"
const val CUSTOM_BACKGROUND_MUSIC_URI_KEY = "custom_background_music_uri"
const val CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY = "custom_background_music_volume"
const val DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS = 5
const val MIN_CUSTOM_STARTUP_SOUND_DURATION_SECONDS = 1
const val MAX_CUSTOM_STARTUP_SOUND_DURATION_SECONDS = 30
const val DEFAULT_CUSTOM_AUDIO_VOLUME = 1.0f
const val DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME = 0.35f
const val MIN_CUSTOM_AUDIO_VOLUME = 0.0f
const val MAX_CUSTOM_AUDIO_VOLUME = 1.0f
const val MAX_PERSISTED_CUSTOM_AUDIO_BYTES = 500L * 1024L * 1024L
private const val CUSTOM_AUDIO_DIR_NAME = "custom-audio"
private const val CUSTOM_AUDIO_EXTENSION = ".audio"
private val CUSTOM_AUDIO_HEX_CHARS = "0123456789abcdef".toCharArray()

fun sanitizeCustomStartupSoundDurationSeconds(value: Int): Int {
    return value.coerceIn(
        MIN_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
        MAX_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
    )
}

fun sanitizeCustomAudioVolume(value: Float): Float {
    return if (value.isFinite()) {
        value.coerceIn(MIN_CUSTOM_AUDIO_VOLUME, MAX_CUSTOM_AUDIO_VOLUME)
    } else {
        DEFAULT_CUSTOM_AUDIO_VOLUME
    }
}

fun sanitizeCustomBackgroundMusicVolume(value: Float): Float {
    return if (value.isFinite()) {
        value.coerceIn(MIN_CUSTOM_AUDIO_VOLUME, MAX_CUSTOM_AUDIO_VOLUME)
    } else {
        DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME
    }
}

fun takePersistableAudioReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

fun releasePersistableAudioReadPermission(context: Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    runCatching {
        context.contentResolver.releasePersistableUriPermission(
            Uri.parse(uriString),
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

fun persistCustomAudioReference(
    context: Context,
    sourceUri: Uri,
    storageKey: String,
    maxBytes: Long = MAX_PERSISTED_CUSTOM_AUDIO_BYTES,
): String? {
    return runCatching {
        require(maxBytes > 0L) { "maxBytes must be positive" }
        val appContext = context.applicationContext
        val targetFile = customAudioFile(appContext, storageKey)
        val parentDir = targetFile.parentFile ?: return@runCatching null
        val tempFile = File(parentDir, "${targetFile.name}.tmp")
        parentDir.mkdirs()
        try {
            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        totalBytes += count
                        if (totalBytes > maxBytes) {
                            throw IOException("The selected audio file is too large")
                        }
                        output.write(buffer, 0, count)
                    }
                    if (totalBytes == 0L) throw IOException("The selected audio file is empty")
                    output.fd.sync()
                }
            } ?: return@runCatching null
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            Uri.fromFile(targetFile).toString()
        } finally {
            tempFile.delete()
        }
    }.getOrNull()
}

fun releaseCustomAudioReference(context: Context, uriString: String?) {
    releasePersistableAudioReadPermission(context, uriString)
    if (uriString.isNullOrBlank()) return
    runCatching {
        val uri = Uri.parse(uriString)
        if (uri.scheme != "file") return@runCatching
        val path = uri.path ?: return@runCatching
        val file = File(path).canonicalFile
        val audioDir = customAudioDir(context.applicationContext).canonicalFile
        if (file.path != audioDir.path && !file.path.startsWith(audioDir.path + File.separator)) {
            return@runCatching
        }
        file.delete()
    }
}

private fun customAudioFile(context: Context, storageKey: String): File {
    return File(
        customAudioDir(context),
        "${hashCustomAudioStorageKey(storageKey)}_${UUID.randomUUID()}$CUSTOM_AUDIO_EXTENSION",
    )
}

private fun customAudioDir(context: Context): File {
    return File(context.filesDir, CUSTOM_AUDIO_DIR_NAME)
}

private fun hashCustomAudioStorageKey(storageKey: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(storageKey.toByteArray(Charsets.UTF_8))
    val chars = CharArray(digest.size * 2)
    digest.forEachIndexed { index, byte ->
        val value = byte.toInt() and 0xff
        chars[index * 2] = CUSTOM_AUDIO_HEX_CHARS[value ushr 4]
        chars[index * 2 + 1] = CUSTOM_AUDIO_HEX_CHARS[value and 0x0f]
    }
    return String(chars)
}

object StartupSoundPlayer {

    private var player: MediaPlayer? = null
    private var source: AssetFileDescriptor? = null
    private var suppressNextAutoPlay = false
    @Volatile
    private var active = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null
    private val audioCoordinator = RuntimeAudioCoordinator(
        onPause = { pauseForSystem() },
        onResume = { resumeForSystem() },
    )

    fun playConfigured(context: Context) {
        if (suppressNextAutoPlay) {
            suppressNextAutoPlay = false
            return
        }
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val uriString = prefs.getString(CUSTOM_STARTUP_SOUND_URI_KEY, null)
        val durationSeconds = sanitizeCustomStartupSoundDurationSeconds(
            prefs.getInt(
                CUSTOM_STARTUP_SOUND_DURATION_SECONDS_KEY,
                DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
            )
        )
        val volume = sanitizeCustomAudioVolume(
            prefs.getFloat(CUSTOM_STARTUP_SOUND_VOLUME_KEY, DEFAULT_CUSTOM_AUDIO_VOLUME)
        )
        play(appContext, uriString, durationSeconds, volume)
    }

    fun suppressNextAutoPlay() {
        suppressNextAutoPlay = true
    }

    fun clearAutoPlaySuppression() {
        suppressNextAutoPlay = false
    }

    fun play(
        context: Context,
        uriString: String?,
        onError: ((Throwable?) -> Unit)? = null,
    ) {
        play(
            context = context,
            uriString = uriString,
            durationSeconds = readConfiguredDurationSeconds(context),
            volume = readConfiguredVolume(context),
            onError = onError,
        )
    }

    fun play(
        context: Context,
        uriString: String?,
        durationSeconds: Int,
        volume: Float = readConfiguredVolume(context),
        onError: ((Throwable?) -> Unit)? = null,
    ) {
        if (uriString.isNullOrBlank()) return
        val track = readAppAudioSettings(context).startup
        if (!isAudioPlaybackAllowed(context, track)) {
            stop()
            return
        }
        stop()
        val safeDurationSeconds = sanitizeCustomStartupSoundDurationSeconds(durationSeconds)
        val safeVolume = sanitizeCustomAudioVolume(volume)

        active = true
        runCatching {
            val appContext = context.applicationContext
            val uri = Uri.parse(uriString)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            if (!audioCoordinator.request(
                    appContext,
                    audioAttributes,
                    android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
                    track,
                )
            ) {
                active = false
                return
            }
            player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                val initialVolume = if (track.fadeInMs > 0) 0f else safeVolume
                setVolume(initialVolume, initialVolume)
                source = runCatching {
                    appContext.contentResolver.openAssetFileDescriptor(uri, "r")
                }.getOrNull()
                val currentSource = source
                if (currentSource != null) {
                    if (currentSource.length == AssetFileDescriptor.UNKNOWN_LENGTH) {
                        setDataSource(currentSource.fileDescriptor)
                    } else {
                        setDataSource(currentSource.fileDescriptor, currentSource.startOffset, currentSource.length)
                    }
                } else {
                    setDataSource(appContext, uri)
                }
                setOnPreparedListener {
                    runCatching {
                        val mediaDuration = it.duration.toLong().coerceAtLeast(0L)
                        val startMs = track.trimStartMs.coerceAtMost(mediaDuration)
                        val endMs = track.trimEndMs
                            .takeIf { end -> end > startMs }
                            ?.coerceAtMost(mediaDuration)
                            ?: mediaDuration
                        if (startMs > 0L) it.seekTo(startMs.toInt())
                        it.start()
                        scheduleFadeIn(it, safeVolume, track.fadeInMs)
                        scheduleStop(
                            it,
                            minOf(safeDurationSeconds * 1_000L, (endMs - startMs).coerceAtLeast(1L)),
                            safeVolume,
                            track.fadeOutMs,
                        )
                    }.onFailure { throwable ->
                        Log.e("StartupSound", "failed to start startup sound", throwable)
                        cleanup(it)
                        notifyError(onError, throwable)
                    }
                }
                setOnCompletionListener { mediaPlayer ->
                    cleanup(mediaPlayer)
                }
                setOnErrorListener { mediaPlayer, what, extra ->
                    Log.e("StartupSound", "failed to play startup sound: what=$what extra=$extra")
                    cleanup(mediaPlayer)
                    notifyError(onError, null)
                    true
                }
                prepareAsync()
            }
        }.onFailure {
            Log.e("StartupSound", "failed to play startup sound", it)
            stop()
            notifyError(onError, it)
        }
    }

    fun stop() {
        active = false
        clearScheduledStop()
        audioCoordinator.release()
        player?.let { mediaPlayer ->
            runCatching {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
            }
            mediaPlayer.release()
        }
        player = null
        runCatching { source?.close() }
        source = null
    }

    private fun cleanup(mediaPlayer: MediaPlayer) {
        if (player === mediaPlayer) {
            active = false
            player = null
            clearScheduledStop()
            audioCoordinator.release()
            runCatching { source?.close() }
            source = null
        }
        mediaPlayer.release()
    }

    fun isActive(): Boolean = active

    private fun pauseForSystem() {
        player?.let { active -> runCatching { if (active.isPlaying) active.pause() } }
    }

    private fun resumeForSystem() {
        player?.let { active -> runCatching { if (!active.isPlaying) active.start() } }
    }

    private fun scheduleStop(
        mediaPlayer: MediaPlayer,
        durationMs: Long,
        volume: Float,
        fadeOutMs: Int,
    ) {
        clearScheduledStop()
        val runnable = Runnable {
            if (player === mediaPlayer) {
                stop()
            }
        }
        stopRunnable = runnable
        if (fadeOutMs > 0 && durationMs > fadeOutMs) {
            mainHandler.postDelayed({ scheduleFadeOut(mediaPlayer, volume, fadeOutMs) }, durationMs - fadeOutMs)
        }
        mainHandler.postDelayed(runnable, durationMs)
    }

    private fun scheduleFadeIn(mediaPlayer: MediaPlayer, volume: Float, durationMs: Int) {
        if (durationMs <= 0) return
        val started = SystemClock.uptimeMillis()
        fun step() {
            if (player !== mediaPlayer) return
            val progress = ((SystemClock.uptimeMillis() - started).toFloat() / durationMs).coerceIn(0f, 1f)
            runCatching { mediaPlayer.setVolume(volume * progress, volume * progress) }
            if (progress < 1f) mainHandler.postDelayed(::step, 40L)
        }
        mainHandler.post(::step)
    }

    private fun scheduleFadeOut(mediaPlayer: MediaPlayer, volume: Float, durationMs: Int) {
        if (durationMs <= 0) return
        val started = SystemClock.uptimeMillis()
        fun step() {
            if (player !== mediaPlayer) return
            val progress = ((SystemClock.uptimeMillis() - started).toFloat() / durationMs).coerceIn(0f, 1f)
            val next = volume * (1f - progress)
            runCatching { mediaPlayer.setVolume(next, next) }
            if (progress < 1f) mainHandler.postDelayed(::step, 40L)
        }
        mainHandler.post(::step)
    }

    private fun clearScheduledStop() {
        stopRunnable?.let { mainHandler.removeCallbacks(it) }
        stopRunnable = null
    }

    private fun readConfiguredDurationSeconds(context: Context): Int {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return sanitizeCustomStartupSoundDurationSeconds(
            prefs.getInt(
                CUSTOM_STARTUP_SOUND_DURATION_SECONDS_KEY,
                DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
            )
        )
    }

    private fun readConfiguredVolume(context: Context): Float {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return sanitizeCustomAudioVolume(
            prefs.getFloat(CUSTOM_STARTUP_SOUND_VOLUME_KEY, DEFAULT_CUSTOM_AUDIO_VOLUME)
        )
    }

    private fun notifyError(onError: ((Throwable?) -> Unit)?, throwable: Throwable?) {
        onError ?: return
        mainHandler.post { onError(throwable) }
    }
}

object ClickSoundPlayer {

    private const val MIN_PLAY_INTERVAL_MS = 80L

    private var soundPool: SoundPool? = null
    private var soundId = 0
    private var loadedUriString: String? = null
    private var isLoaded = false
    private var pendingPlay = false
    private var source: AssetFileDescriptor? = null
    private var lastPlayUptime = 0L
    private var volume = DEFAULT_CUSTOM_AUDIO_VOLUME
    private val mainHandler = Handler(Looper.getMainLooper())

    fun playConfigured(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val uriString = prefs.getString(CUSTOM_CLICK_SOUND_URI_KEY, null)
        val configuredVolume = sanitizeCustomAudioVolume(
            prefs.getFloat(CUSTOM_CLICK_SOUND_VOLUME_KEY, DEFAULT_CUSTOM_AUDIO_VOLUME)
        )
        play(appContext, uriString, configuredVolume)
    }

    fun play(
        context: Context,
        uriString: String?,
        volume: Float = readConfiguredVolume(context),
        onError: ((Throwable?) -> Unit)? = null,
    ) {
        if (uriString.isNullOrBlank()) {
            release()
            return
        }
        val track = readAppAudioSettings(context).click
        if (!isAudioPlaybackAllowed(context, track)) return

        this.volume = sanitizeCustomAudioVolume(volume)
        val now = SystemClock.uptimeMillis()
        if (now - lastPlayUptime < MIN_PLAY_INTERVAL_MS) return

        if (loadedUriString != uriString || soundPool == null || soundId == 0) {
            load(context, uriString, playAfterLoad = true, onError = onError)
            return
        }

        if (!isLoaded) {
            pendingPlay = true
            return
        }

        playLoaded(onError)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundId = 0
        loadedUriString = null
        isLoaded = false
        pendingPlay = false
        closeSource()
    }

    private fun load(
        context: Context,
        uriString: String,
        playAfterLoad: Boolean,
        onError: ((Throwable?) -> Unit)?,
    ) {
        release()
        loadedUriString = uriString
        pendingPlay = playAfterLoad

        runCatching {
            val appContext = context.applicationContext
            val uri = Uri.parse(uriString)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val newSoundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()
            source = appContext.contentResolver.openAssetFileDescriptor(uri, "r")
            val currentSource = source ?: error("Unable to open click sound")

            newSoundPool.setOnLoadCompleteListener { pool, loadedSoundId, status ->
                if (pool !== soundPool || loadedSoundId != soundId) return@setOnLoadCompleteListener
                closeSource()
                if (status == 0) {
                    isLoaded = true
                    if (pendingPlay) {
                        pendingPlay = false
                        playLoaded(onError)
                    }
                } else {
                    Log.e("ClickSound", "failed to load click sound: status=$status")
                    release()
                    notifyError(onError, null)
                }
            }

            soundPool = newSoundPool
            soundId = newSoundPool.load(currentSource, 1)
            if (soundId == 0) {
                Log.e("ClickSound", "failed to queue click sound")
                release()
                notifyError(onError, null)
            }
        }.onFailure {
            Log.e("ClickSound", "failed to load click sound", it)
            release()
            notifyError(onError, it)
        }
    }

    private fun playLoaded(onError: ((Throwable?) -> Unit)?) {
        val currentPool = soundPool ?: return
        val currentSoundId = soundId.takeIf { it != 0 } ?: return
        val safeVolume = sanitizeCustomAudioVolume(volume)
        runCatching {
            val streamId = currentPool.play(currentSoundId, safeVolume, safeVolume, 1, 0, 1.0f)
            if (streamId == 0) {
                notifyError(onError, null)
            } else {
                lastPlayUptime = SystemClock.uptimeMillis()
            }
        }.onFailure {
            Log.e("ClickSound", "failed to play click sound", it)
            notifyError(onError, it)
        }
    }

    private fun closeSource() {
        runCatching { source?.close() }
        source = null
    }

    private fun notifyError(onError: ((Throwable?) -> Unit)?, throwable: Throwable?) {
        onError ?: return
        mainHandler.post { onError(throwable) }
    }

    private fun readConfiguredVolume(context: Context): Float {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return sanitizeCustomAudioVolume(
            prefs.getFloat(CUSTOM_CLICK_SOUND_VOLUME_KEY, DEFAULT_CUSTOM_AUDIO_VOLUME)
        )
    }
}

object BackgroundMusicPlayer {

    private const val STARTUP_SOUND_RETRY_DELAY_MS = 120L

    private var player: MediaPlayer? = null
    private var source: AssetFileDescriptor? = null
    private var loadedUriString: String? = null
    private var volume = DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME
    private val mainHandler = Handler(Looper.getMainLooper())
    private var trimBoundaryRunnable: Runnable? = null
    private var deferredPlayRunnable: Runnable? = null
    private val audioCoordinator = RuntimeAudioCoordinator(
        onPause = { pauseForSystem() },
        onResume = { resumeForSystem() },
    )

    fun playConfigured(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val uriString = prefs.getString(CUSTOM_BACKGROUND_MUSIC_URI_KEY, null)
        val configuredVolume = sanitizeCustomBackgroundMusicVolume(
            prefs.getFloat(CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY, DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME)
        )
        play(appContext, uriString, configuredVolume)
    }

    fun play(
        context: Context,
        uriString: String?,
        volume: Float = readConfiguredVolume(context),
        loop: Boolean = true,
        onError: ((Throwable?) -> Unit)? = null,
    ) {
        if (uriString.isNullOrBlank()) {
            stop()
            return
        }
        if (StartupSoundPlayer.isActive()) {
            deferUntilStartupSoundFinishes(context, uriString, volume, loop, onError)
            return
        }
        clearDeferredPlay()
        val track = readAppAudioSettings(context).background
        if (!isAudioPlaybackAllowed(context, track)) {
            stop()
            return
        }

        val safeVolume = sanitizeCustomBackgroundMusicVolume(volume)
        if (loadedUriString == uriString && player != null) {
            this.volume = safeVolume
            player?.setVolume(safeVolume, safeVolume)
            runCatching {
                if (player?.isPlaying == false) {
                    player?.start()
                }
            }.onFailure {
                Log.e("BackgroundMusic", "failed to resume background music", it)
                stop()
                notifyError(onError, it)
            }
            return
        }

        stop()
        this.volume = safeVolume
        loadedUriString = uriString

        runCatching {
            val appContext = context.applicationContext
            val uri = Uri.parse(uriString)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            if (!audioCoordinator.request(
                    appContext,
                    audioAttributes,
                    android.media.AudioManager.AUDIOFOCUS_GAIN,
                    track,
                )
            ) return
            player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                isLooping = loop && track.trimStartMs == 0L && track.trimEndMs == 0L
                val initialVolume = if (track.fadeInMs > 0) 0f else safeVolume
                setVolume(initialVolume, initialVolume)
                source = runCatching {
                    appContext.contentResolver.openAssetFileDescriptor(uri, "r")
                }.getOrNull()
                val currentSource = source
                if (currentSource != null) {
                    if (currentSource.length == AssetFileDescriptor.UNKNOWN_LENGTH) {
                        setDataSource(currentSource.fileDescriptor)
                    } else {
                        setDataSource(currentSource.fileDescriptor, currentSource.startOffset, currentSource.length)
                    }
                } else {
                    setDataSource(appContext, uri)
                }
                setOnPreparedListener {
                    runCatching {
                        val duration = it.duration.toLong().coerceAtLeast(0L)
                        val start = track.trimStartMs.coerceAtMost(duration)
                        val end = track.trimEndMs.takeIf { value -> value > start }
                            ?.coerceAtMost(duration)
                            ?: duration
                        if (start > 0L) it.seekTo(start.toInt())
                        it.start()
                        scheduleBackgroundFadeIn(it, safeVolume, track.fadeInMs)
                        if (end > start && (start > 0L || end < duration)) {
                            scheduleTrimBoundary(
                                it,
                                start,
                                end,
                                track.loop || loop,
                                safeVolume,
                                track.fadeInMs,
                                track.fadeOutMs,
                            )
                        }
                    }
                        .onFailure { throwable ->
                            Log.e("BackgroundMusic", "failed to start background music", throwable)
                            cleanup(it)
                            notifyError(onError, throwable)
                        }
                }
                setOnCompletionListener { mediaPlayer ->
                    cleanup(mediaPlayer)
                }
                setOnErrorListener { mediaPlayer, what, extra ->
                    Log.e("BackgroundMusic", "failed to play background music: what=$what extra=$extra")
                    cleanup(mediaPlayer)
                    notifyError(onError, null)
                    true
                }
                prepareAsync()
            }
        }.onFailure {
            Log.e("BackgroundMusic", "failed to play background music", it)
            stop()
            notifyError(onError, it)
        }
    }

    fun updateVolume(volume: Float) {
        val safeVolume = sanitizeCustomBackgroundMusicVolume(volume)
        this.volume = safeVolume
        player?.setVolume(safeVolume, safeVolume)
    }

    fun stop() {
        clearDeferredPlay()
        trimBoundaryRunnable?.let { mainHandler.removeCallbacks(it) }
        trimBoundaryRunnable = null
        audioCoordinator.release()
        player?.let { mediaPlayer ->
            runCatching {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
            }
            mediaPlayer.release()
        }
        player = null
        loadedUriString = null
        runCatching { source?.close() }
        source = null
    }

    private fun cleanup(mediaPlayer: MediaPlayer) {
        if (player === mediaPlayer) {
            player = null
            loadedUriString = null
            runCatching { source?.close() }
            source = null
            trimBoundaryRunnable?.let { mainHandler.removeCallbacks(it) }
            trimBoundaryRunnable = null
            audioCoordinator.release()
        }
        mediaPlayer.release()
    }

    private fun pauseForSystem() {
        player?.let { active -> runCatching { if (active.isPlaying) active.pause() } }
    }

    private fun resumeForSystem() {
        player?.let { active -> runCatching { if (!active.isPlaying) active.start() } }
    }

    private fun readConfiguredVolume(context: Context): Float {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return sanitizeCustomBackgroundMusicVolume(
            prefs.getFloat(CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY, DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME)
        )
    }

    private fun notifyError(onError: ((Throwable?) -> Unit)?, throwable: Throwable?) {
        onError ?: return
        mainHandler.post { onError(throwable) }
    }

    private fun deferUntilStartupSoundFinishes(
        context: Context,
        uriString: String,
        volume: Float,
        loop: Boolean,
        onError: ((Throwable?) -> Unit)?,
    ) {
        clearDeferredPlay()
        val appContext = context.applicationContext
        val runnable = object : Runnable {
            override fun run() {
                if (deferredPlayRunnable !== this) return
                if (StartupSoundPlayer.isActive()) {
                    mainHandler.postDelayed(this, STARTUP_SOUND_RETRY_DELAY_MS)
                    return
                }
                deferredPlayRunnable = null
                play(appContext, uriString, volume, loop, onError)
            }
        }
        deferredPlayRunnable = runnable
        mainHandler.postDelayed(runnable, STARTUP_SOUND_RETRY_DELAY_MS)
    }

    private fun clearDeferredPlay() {
        deferredPlayRunnable?.let(mainHandler::removeCallbacks)
        deferredPlayRunnable = null
    }

    private fun scheduleTrimBoundary(
        mediaPlayer: MediaPlayer,
        startMs: Long,
        endMs: Long,
        loop: Boolean,
        volume: Float,
        fadeInMs: Int,
        fadeOutMs: Int,
    ) {
        trimBoundaryRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = object : Runnable {
            override fun run() {
                if (player !== mediaPlayer) return
                val position = runCatching { mediaPlayer.currentPosition.toLong() }.getOrDefault(0L)
                if (fadeOutMs > 0 && endMs - position <= fadeOutMs) {
                    val factor = ((endMs - position).toFloat() / fadeOutMs).coerceIn(0f, 1f)
                    runCatching { mediaPlayer.setVolume(volume * factor, volume * factor) }
                }
                if (position >= endMs) {
                    if (loop) {
                        runCatching {
                            mediaPlayer.seekTo(startMs.toInt())
                            mediaPlayer.setVolume(if (fadeInMs > 0) 0f else volume,
                                if (fadeInMs > 0) 0f else volume)
                            mediaPlayer.start()
                            scheduleBackgroundFadeIn(mediaPlayer, volume, fadeInMs)
                        }
                    } else {
                        stop()
                        return
                    }
                }
                mainHandler.postDelayed(this, 60L)
            }
        }
        trimBoundaryRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun scheduleBackgroundFadeIn(mediaPlayer: MediaPlayer, volume: Float, durationMs: Int) {
        if (durationMs <= 0) return
        val started = SystemClock.uptimeMillis()
        fun step() {
            if (player !== mediaPlayer) return
            val progress = ((SystemClock.uptimeMillis() - started).toFloat() / durationMs).coerceIn(0f, 1f)
            runCatching { mediaPlayer.setVolume(volume * progress, volume * progress) }
            if (progress < 1f) mainHandler.postDelayed(::step, 40L)
        }
        mainHandler.post(::step)
    }
}
