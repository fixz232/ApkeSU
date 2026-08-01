package me.weishu.kernelsu.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.LruCache
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ui.component.liquid.isLiquidGlassTheme
import me.weishu.kernelsu.ui.component.liquid.liquidGlassBackdropColor
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.CustomPageBackgroundSet
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.FULL_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.MediaVisualSettings
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap
import me.weishu.kernelsu.ui.util.sanitizeCustomVideoBackgroundDurationSeconds
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperCrop
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperOpacity
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperPassthroughOpacity
import android.graphics.Matrix as AndroidMatrix
import java.util.concurrent.ConcurrentHashMap

private const val WALLPAPER_BACKGROUND_MAX_SIDE = 1800
private const val WALLPAPER_PREVIEW_MAX_SIDE = 1400
private const val WALLPAPER_PRELOAD_BATCH_SIZE = 2
private const val WALLPAPER_CACHE_MIN_KIB = 16 * 1024
private const val WALLPAPER_CACHE_MAX_KIB = 48 * 1024

private data class WallpaperBitmapCacheKey(
    val uriString: String,
    val maxSide: Int,
    val crop: CustomWallpaperCrop,
)

private val wallpaperBitmapCache = object : LruCache<WallpaperBitmapCacheKey, Bitmap>(
    (Runtime.getRuntime().maxMemory() / 8L / 1024L)
        .coerceIn(WALLPAPER_CACHE_MIN_KIB.toLong(), WALLPAPER_CACHE_MAX_KIB.toLong())
        .toInt(),
) {
    override fun sizeOf(key: WallpaperBitmapCacheKey, value: Bitmap): Int {
        return (value.allocationByteCount / 1024).coerceAtLeast(1)
    }
}

private val wallpaperBitmapDecodeLocks = ConcurrentHashMap<WallpaperBitmapCacheKey, Any>()

private fun cachedWallpaperBitmap(key: WallpaperBitmapCacheKey): Bitmap? {
    val bitmap = wallpaperBitmapCache.get(key) ?: return null
    if (!bitmap.isRecycled) return bitmap
    wallpaperBitmapCache.remove(key)
    return null
}

private fun loadCachedWallpaperBitmap(
    context: Context,
    key: WallpaperBitmapCacheKey,
): Bitmap? {
    cachedWallpaperBitmap(key)?.let { return it }
    val decodeLock = wallpaperBitmapDecodeLocks.getOrPut(key) { Any() }
    return try {
        synchronized(decodeLock) {
            cachedWallpaperBitmap(key)?.let { return@synchronized it }
            loadCustomImageBitmap(
                context = context.applicationContext,
                uriString = key.uriString,
                maxSide = key.maxSide,
                crop = key.crop,
            )?.also { wallpaperBitmapCache.put(key, it) }
        }
    } finally {
        wallpaperBitmapDecodeLocks.remove(key, decodeLock)
    }
}

internal suspend fun preloadCustomImageBitmap(
    context: Context,
    uriString: String?,
    maxSide: Int,
    crop: CustomWallpaperCrop = CustomWallpaperCrop(),
): Bitmap? {
    val key = uriString?.takeIf(String::isNotBlank)?.let {
        WallpaperBitmapCacheKey(
            uriString = it,
            maxSide = maxSide.coerceAtLeast(1),
            crop = sanitizeCustomWallpaperCrop(crop),
        )
    } ?: return null
    return withContext(Dispatchers.IO) {
        loadCachedWallpaperBitmap(context.applicationContext, key)
    }
}

suspend fun preloadCustomPageBackgroundImages(
    context: Context,
    backgrounds: CustomPageBackgroundSet,
) {
    val requests = CustomPageBackgroundTarget.entries
        .map { backgrounds[it] }
        .filter { it.hasWallpaper && !it.hasVideo }
        .mapNotNull { state ->
            state.wallpaperUriString?.takeIf(String::isNotBlank)?.let { uriString ->
                WallpaperBitmapCacheKey(
                    uriString = uriString,
                    maxSide = WALLPAPER_BACKGROUND_MAX_SIDE,
                    crop = sanitizeCustomWallpaperCrop(state.crop),
                )
            }
        }
        .distinct()
        .filter { cachedWallpaperBitmap(it) == null }

    requests.chunked(WALLPAPER_PRELOAD_BATCH_SIZE).forEach { batch ->
        coroutineScope {
            batch.map { key ->
                async(Dispatchers.IO) {
                    loadCachedWallpaperBitmap(context, key)
                }
            }.awaitAll()
        }
    }
}

@Composable
fun CustomWallpaperBackground(
    uriString: String?,
    opacity: Float = DEFAULT_CUSTOM_WALLPAPER_OPACITY,
    crop: CustomWallpaperCrop = CustomWallpaperCrop(),
    imageAlpha: Float = 1f,
    drawOverlay: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = rememberCustomImageBitmap(
        uriString = uriString,
        maxSide = WALLPAPER_BACKGROUND_MAX_SIDE,
        crop = crop,
    ) ?: return

    Image(
        modifier = modifier.fillMaxSize(),
        bitmap = imageBitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = imageAlpha.coerceIn(0f, 1f),
    )
    if (drawOverlay) {
        val overlayAlpha = 1f - sanitizeCustomWallpaperOpacity(opacity)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(if (isInDarkTheme()) Color.Black.copy(alpha = overlayAlpha) else Color.White.copy(alpha = overlayAlpha))
        )
    }
}

@Composable
fun CustomWallpaperRoot(
    uriString: String?,
    videoUriString: String? = null,
    videoDurationSeconds: Int = DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
    opacity: Float = DEFAULT_CUSTOM_WALLPAPER_OPACITY,
    crop: CustomWallpaperCrop = CustomWallpaperCrop(),
    visualSettings: MediaVisualSettings = MediaVisualSettings(),
    passthroughEnabled: Boolean = false,
    passthroughOpacity: Float = DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY,
    content: @Composable BoxScope.() -> Unit,
) {
    val isLiquidGlass = isLiquidGlassTheme()
    val surfaceColor = liquidGlassBackdropColor()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor),
    ) {
        CustomBackgroundMedia(
            imageUriString = uriString,
            videoUriString = videoUriString,
            videoDurationSeconds = videoDurationSeconds,
            opacity = if (isLiquidGlass) opacity.coerceAtMost(0.42f) else opacity,
            crop = crop,
            visualSettings = visualSettings,
        )
        content()
        if (passthroughEnabled) {
            val passthroughAlpha = sanitizeCustomWallpaperPassthroughOpacity(passthroughOpacity)
            when {
                !videoUriString.isNullOrBlank() -> CustomVideoPassthroughBackground(
                    uriString = videoUriString,
                    durationSeconds = videoDurationSeconds,
                    crop = crop,
                    visualSettings = visualSettings,
                    imageAlpha = passthroughAlpha,
                )

                !uriString.isNullOrBlank() -> CustomBackgroundMedia(
                    imageUriString = uriString,
                    videoUriString = null,
                    opacity = 1f,
                    crop = crop,
                    visualSettings = visualSettings,
                    imageAlpha = passthroughAlpha,
                    drawOverlay = false,
                )
            }
        }
    }
}

@Composable
fun CustomBackgroundMedia(
    imageUriString: String?,
    videoUriString: String?,
    videoDurationSeconds: Int = DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
    opacity: Float = DEFAULT_CUSTOM_WALLPAPER_OPACITY,
    crop: CustomWallpaperCrop = CustomWallpaperCrop(),
    visualSettings: MediaVisualSettings = MediaVisualSettings(),
    imageAlpha: Float = 1f,
    drawOverlay: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (videoUriString.isNullOrBlank() && imageUriString.isNullOrBlank()) return
    val imageBitmap = if (videoUriString.isNullOrBlank()) {
        rememberCustomImageBitmap(imageUriString, WALLPAPER_BACKGROUND_MAX_SIDE, crop)
    } else {
        null
    }
    Box(modifier = modifier.fillMaxSize()) {
        MediaVisualLayer(
            settings = visualSettings.copy(opacity = visualSettings.opacity * imageAlpha),
            modifier = Modifier.fillMaxSize(),
        ) { colorFilter ->
            if (!videoUriString.isNullOrBlank()) {
                CustomVideoBackground(
                    uriString = videoUriString,
                    durationSeconds = videoDurationSeconds,
                    crop = crop,
                    imageAlpha = 1f,
                    drawOverlay = false,
                    touchPassthrough = true,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (imageBitmap != null) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                )
            }
        }
        if (drawOverlay) {
            val overlayAlpha = 1f - sanitizeCustomWallpaperOpacity(opacity)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isInDarkTheme()) Color.Black.copy(alpha = overlayAlpha)
                        else Color.White.copy(alpha = overlayAlpha)
                    )
            )
        }
    }
}

@Composable
fun CustomVideoBackground(
    uriString: String?,
    durationSeconds: Int = DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
    opacity: Float = DEFAULT_CUSTOM_WALLPAPER_OPACITY,
    crop: CustomWallpaperCrop = FULL_CUSTOM_WALLPAPER_CROP,
    imageAlpha: Float = 1f,
    drawOverlay: Boolean = true,
    touchPassthrough: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (uriString.isNullOrBlank()) return

    val uri = remember(uriString) { uriString.toUri() }
    var isRendering by remember(uriString) { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize()) {
        VideoTextureBackground(
            uri = uri,
            durationSeconds = durationSeconds,
            crop = crop,
            visible = isRendering,
            imageAlpha = imageAlpha,
            touchPassthrough = touchPassthrough,
            onFirstFrame = { isRendering = true },
        )
        if (drawOverlay) {
            val overlayAlpha = 1f - sanitizeCustomWallpaperOpacity(opacity)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isInDarkTheme()) Color.Black.copy(alpha = overlayAlpha) else Color.White.copy(alpha = overlayAlpha))
            )
        }
    }
}

@Composable
fun rememberCustomWallpaperPreviewBitmap(
    uriString: String?,
    crop: CustomWallpaperCrop = CustomWallpaperCrop(),
) = rememberCustomImageBitmap(
    uriString = uriString,
    maxSide = WALLPAPER_PREVIEW_MAX_SIDE,
    crop = crop,
)

@Composable
fun rememberCustomVideoFrameBitmap(
    uriString: String?,
    maxSide: Int = WALLPAPER_PREVIEW_MAX_SIDE,
) = run {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uriString, maxSide, context) {
        value = if (uriString.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                loadCustomVideoFrameBitmap(context, uriString, maxSide)
            }
        }
    }
    remember(bitmap) { bitmap?.asImageBitmap() }
}

@Composable
internal fun rememberCustomImageAndroidBitmap(
    uriString: String?,
    maxSide: Int = WALLPAPER_PREVIEW_MAX_SIDE,
    crop: CustomWallpaperCrop = CustomWallpaperCrop(),
) = run {
    val context = LocalContext.current
    val cacheKey = remember(uriString, maxSide, crop) {
        uriString?.takeIf(String::isNotBlank)?.let {
            WallpaperBitmapCacheKey(
                uriString = it,
                maxSide = maxSide.coerceAtLeast(1),
                crop = sanitizeCustomWallpaperCrop(crop),
            )
        }
    }
    key(cacheKey) {
        val cachedBitmap = remember(cacheKey) { cacheKey?.let(::cachedWallpaperBitmap) }
        val bitmap by produceState<Bitmap?>(initialValue = cachedBitmap, cacheKey, context) {
            value = when {
                cacheKey == null -> null
                cachedBitmap != null -> cachedBitmap
                else -> withContext(Dispatchers.IO) {
                    loadCachedWallpaperBitmap(context, cacheKey)
                }
            }
        }
        bitmap
    }
}

@Composable
fun rememberCustomImageBitmap(
    uriString: String?,
    maxSide: Int = WALLPAPER_PREVIEW_MAX_SIDE,
    crop: CustomWallpaperCrop = CustomWallpaperCrop(),
) = rememberCustomImageAndroidBitmap(uriString, maxSide, crop).let { bitmap ->
    remember(bitmap) { bitmap?.asImageBitmap() }
}

@Composable
fun CustomVideoPassthroughBackground(
    uriString: String,
    durationSeconds: Int = DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
    crop: CustomWallpaperCrop = FULL_CUSTOM_WALLPAPER_CROP,
    visualSettings: MediaVisualSettings = MediaVisualSettings(),
    imageAlpha: Float,
    modifier: Modifier = Modifier,
) {
    if (uriString.isBlank() || imageAlpha <= 0f) return

    MediaVisualLayer(
        settings = visualSettings.copy(opacity = visualSettings.opacity * imageAlpha),
        modifier = modifier.fillMaxSize(),
    ) {
        CustomVideoBackground(
            uriString = uriString,
            durationSeconds = durationSeconds,
            crop = crop,
            imageAlpha = 1f,
            drawOverlay = false,
            touchPassthrough = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoTextureBackground(
    uri: Uri,
    durationSeconds: Int,
    crop: CustomWallpaperCrop,
    visible: Boolean,
    imageAlpha: Float,
    touchPassthrough: Boolean,
    onFirstFrame: () -> Unit,
) {
    if (touchPassthrough) {
        TouchPassthroughVideoTextureBackground(
            uri = uri,
            durationSeconds = durationSeconds,
            crop = crop,
            visible = visible,
            imageAlpha = imageAlpha,
            onFirstFrame = onFirstFrame,
        )
        return
    }

    val context = LocalContext.current
    val safeDurationSeconds = sanitizeCustomVideoBackgroundDurationSeconds(durationSeconds)
    val currentOnFirstFrame by rememberUpdatedState(onFirstFrame)
    var mediaPlayer by remember(uri) { mutableStateOf<MediaPlayer?>(null) }
    var surfaceWidth by remember(uri) { mutableIntStateOf(0) }
    var surfaceHeight by remember(uri) { mutableIntStateOf(0) }
    var videoWidth by remember(uri) { mutableIntStateOf(0) }
    var videoHeight by remember(uri) { mutableIntStateOf(0) }
    val transform = remember(surfaceWidth, surfaceHeight, videoWidth, videoHeight, crop) {
        buildCropTransform(
            surfaceWidth = surfaceWidth,
            surfaceHeight = surfaceHeight,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            crop = crop,
        )
    }

    AndroidEmbeddedExternalSurface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = if (visible) imageAlpha.coerceIn(0f, 1f) else 0f
            },
        isOpaque = false,
        transform = transform,
    ) {
        onSurface { surface, width, height ->
            surfaceWidth = width
            surfaceHeight = height
            val player = runCatching { MediaPlayer() }.getOrNull() ?: return@onSurface
            runCatching {
                player.apply {
                    setSurface(surface)
                    setDataSource(context.applicationContext, uri)
                    setVolume(0f, 0f)
                    setOnPreparedListener { prepared ->
                        prepared.isLooping = false
                        videoWidth = prepared.videoWidth
                        videoHeight = prepared.videoHeight
                        prepared.start()
                        currentOnFirstFrame()
                    }
                    setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            currentOnFirstFrame()
                        }
                        false
                    }
                    setOnVideoSizeChangedListener { _, newWidth, newHeight ->
                        videoWidth = newWidth
                        videoHeight = newHeight
                    }
                    setOnCompletionListener { completed ->
                        completed.seekTo(0)
                        completed.start()
                    }
                    setOnErrorListener { failedPlayer, _, _ ->
                        runCatching { failedPlayer.release() }
                        if (mediaPlayer === failedPlayer) {
                            mediaPlayer = null
                        }
                        true
                    }
                    prepareAsync()
                }
            }.onSuccess {
                mediaPlayer = it
            }.onFailure {
                runCatching { player.release() }
            }

            surface.onChanged { newWidth, newHeight ->
                surfaceWidth = newWidth
                surfaceHeight = newHeight
            }
            surface.onDestroyed {
                runCatching { player.release() }
                if (mediaPlayer === player) {
                    mediaPlayer = null
                }
            }
        }
    }

    LaunchedEffect(mediaPlayer, safeDurationSeconds) {
        val player = mediaPlayer ?: return@LaunchedEffect
        while (true) {
            delay(safeDurationSeconds * 1_000L)
            runCatching {
                player.seekTo(0)
                player.start()
            }
        }
    }

    DisposableEffect(uri) {
        onDispose {
            runCatching { mediaPlayer?.release() }
            mediaPlayer = null
        }
    }
}

@Composable
private fun TouchPassthroughVideoTextureBackground(
    uri: Uri,
    durationSeconds: Int,
    crop: CustomWallpaperCrop,
    visible: Boolean,
    imageAlpha: Float,
    onFirstFrame: () -> Unit,
) {
    val context = LocalContext.current
    val safeDurationSeconds = sanitizeCustomVideoBackgroundDurationSeconds(durationSeconds)
    val currentOnFirstFrame by rememberUpdatedState(onFirstFrame)
    var mediaPlayer by remember(uri) { mutableStateOf<MediaPlayer?>(null) }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = if (visible) imageAlpha.coerceIn(0f, 1f) else 0f
            },
        factory = { viewContext ->
            TouchPassthroughTextureView(viewContext).apply textureView@{
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                isOpaque = false
                isClickable = false
                isLongClickable = false
                isFocusable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var surface: Surface? = null
                    private var player: MediaPlayer? = null

                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        releasePlayer()
                        surface = Surface(surfaceTexture)
                        val currentSurface = surface ?: return
                        val nextPlayer = runCatching { MediaPlayer() }.getOrNull() ?: return
                        runCatching {
                            nextPlayer.apply {
                                setSurface(currentSurface)
                                setDataSource(context.applicationContext, uri)
                                setVolume(0f, 0f)
                                setOnPreparedListener { prepared ->
                                    prepared.isLooping = false
                                    applyCenterCropTransform(
                                        textureView = this@textureView,
                                        videoWidth = prepared.videoWidth,
                                        videoHeight = prepared.videoHeight,
                                        crop = crop,
                                    )
                                    prepared.start()
                                    currentOnFirstFrame()
                                }
                                setOnInfoListener { _, what, _ ->
                                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                        currentOnFirstFrame()
                                    }
                                    false
                                }
                                setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                                    applyCenterCropTransform(this@textureView, videoWidth, videoHeight, crop)
                                }
                                setOnCompletionListener { completed ->
                                    completed.seekTo(0)
                                    completed.start()
                                }
                                setOnErrorListener { failedPlayer, _, _ ->
                                    runCatching { failedPlayer.release() }
                                    if (player === failedPlayer) {
                                        player = null
                                    }
                                    if (mediaPlayer === failedPlayer) {
                                        mediaPlayer = null
                                    }
                                    true
                                }
                                prepareAsync()
                            }
                        }.onSuccess {
                            player = it
                            mediaPlayer = it
                        }.onFailure {
                            runCatching { nextPlayer.release() }
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        player?.let {
                            applyCenterCropTransform(this@textureView, it.videoWidth, it.videoHeight, crop)
                        }
                    }

                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                        releasePlayer()
                        runCatching { surface?.release() }
                        surface = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit

                    fun releasePlayer() {
                        runCatching { player?.release() }
                        if (mediaPlayer === player) {
                            mediaPlayer = null
                        }
                        player = null
                    }
                }
            }
        },
        update = { textureView ->
            textureView.isClickable = false
            textureView.isLongClickable = false
            textureView.isFocusable = false
            mediaPlayer?.let { player ->
                applyCenterCropTransform(textureView, player.videoWidth, player.videoHeight, crop)
            }
        },
    )

    LaunchedEffect(mediaPlayer, safeDurationSeconds) {
        val player = mediaPlayer ?: return@LaunchedEffect
        while (true) {
            delay(safeDurationSeconds * 1_000L)
            runCatching {
                player.seekTo(0)
                player.start()
            }
        }
    }

    DisposableEffect(uri) {
        onDispose {
            runCatching { mediaPlayer?.release() }
            mediaPlayer = null
        }
    }
}

private fun buildCropTransform(
    surfaceWidth: Int,
    surfaceHeight: Int,
    videoWidth: Int,
    videoHeight: Int,
    crop: CustomWallpaperCrop,
): Matrix? {
    if (surfaceWidth <= 0 || surfaceHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return null

    val viewWidth = surfaceWidth.toFloat()
    val viewHeight = surfaceHeight.toFloat()
    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    val cropWidth = videoWidth * safeCrop.width.coerceAtLeast(0.001f)
    val cropHeight = videoHeight * safeCrop.height.coerceAtLeast(0.001f)
    val scale = maxOf(viewWidth / cropWidth, viewHeight / cropHeight)
    val scaledWidth = videoWidth * scale
    val scaledHeight = videoHeight * scale
    val cropCenterX = videoWidth * (safeCrop.left + safeCrop.right) / 2f
    val cropCenterY = videoHeight * (safeCrop.top + safeCrop.bottom) / 2f
    val cropCenterScaledX = cropCenterX * scale
    val cropCenterScaledY = cropCenterY * scale
    val scaledVideoCenterX = scaledWidth / 2f
    val scaledVideoCenterY = scaledHeight / 2f

    return Matrix().apply {
        translate(viewWidth / 2f, viewHeight / 2f)
        scale(scaledWidth / viewWidth, scaledHeight / viewHeight)
        translate(
            (scaledVideoCenterX - cropCenterScaledX) / (scaledWidth / viewWidth).coerceAtLeast(0.001f) - viewWidth / 2f,
            (scaledVideoCenterY - cropCenterScaledY) / (scaledHeight / viewHeight).coerceAtLeast(0.001f) - viewHeight / 2f,
        )
    }
}

private fun applyCenterCropTransform(
    textureView: TextureView,
    videoWidth: Int,
    videoHeight: Int,
    crop: CustomWallpaperCrop,
) {
    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) return

    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    val cropWidth = videoWidth * safeCrop.width.coerceAtLeast(0.001f)
    val cropHeight = videoHeight * safeCrop.height.coerceAtLeast(0.001f)
    val scale = maxOf(viewWidth / cropWidth, viewHeight / cropHeight)
    val scaledWidth = videoWidth * scale
    val scaledHeight = videoHeight * scale
    val cropCenterX = videoWidth * (safeCrop.left + safeCrop.right) / 2f
    val cropCenterY = videoHeight * (safeCrop.top + safeCrop.bottom) / 2f
    val dx = viewWidth / 2f - cropCenterX * scale
    val dy = viewHeight / 2f - cropCenterY * scale
    val matrix = AndroidMatrix().apply {
        setScale(scaledWidth / viewWidth, scaledHeight / viewHeight)
        postTranslate(dx, dy)
    }
    textureView.setTransform(matrix)
}

private fun loadCustomVideoFrameBitmap(
    context: Context,
    uriString: String,
    maxSide: Int,
): Bitmap? {
    val retriever = runCatching { MediaMetadataRetriever() }.getOrNull() ?: return null
    return try {
        val uri = Uri.parse(uriString)
        retriever.setDataSource(context.applicationContext, uri)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
        if (width != null && height != null) {
            val scale = maxOf(width, height).toFloat() / maxSide.coerceAtLeast(1)
            val targetWidth = if (scale > 1f) (width / scale).toInt().coerceAtLeast(1) else width
            val targetHeight = if (scale > 1f) (height / scale).toInt().coerceAtLeast(1) else height
            retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetWidth, targetHeight)
                ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } else {
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
    } catch (_: Throwable) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private class TouchPassthroughTextureView(context: Context) : TextureView(context) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean = false
}
