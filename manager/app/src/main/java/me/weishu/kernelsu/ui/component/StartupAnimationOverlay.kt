package me.weishu.kernelsu.ui.component

import android.graphics.Matrix
import android.graphics.ImageDecoder
import android.graphics.SurfaceTexture
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ui.util.isCustomStartupAnimationGif
import me.weishu.kernelsu.ui.util.isCustomStartupAnimationVideo
import me.weishu.kernelsu.ui.util.MAX_STARTUP_ANIMATION_DURATION_MS
import me.weishu.kernelsu.ui.util.StartupAnimationScaleMode
import me.weishu.kernelsu.ui.util.StartupAnimationSettings
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun StartupAnimationOverlay(
    uriString: String?,
    modifier: Modifier = Modifier,
    settings: StartupAnimationSettings = StartupAnimationSettings(),
    onFinished: () -> Unit,
    onError: () -> Unit = {},
) {
    if (uriString.isNullOrBlank()) {
        LaunchedEffect(Unit) {
            onFinished()
        }
        return
    }

    val context = LocalContext.current
    val uri = remember(uriString) { uriString.toUri() }
    val normalizedSettings = remember(settings) { settings.normalized() }
    val isGif = remember(uriString) { isCustomStartupAnimationGif(context, uri) }
    val isVideo = remember(uriString, isGif) { !isGif && isCustomStartupAnimationVideo(context, uri) }
    var isVideoRendering by remember(uriString) { mutableStateOf(false) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    var verticalDrag by remember(uriString) { mutableFloatStateOf(0f) }

    LaunchedEffect(uriString, normalizedSettings.durationMillis) {
        delay(normalizedSettings.durationMillis.coerceAtMost(MAX_STARTUP_ANIMATION_DURATION_MS))
        currentOnFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(normalizedSettings.backgroundArgb))
            .clickable(
                enabled = normalizedSettings.allowTapSkip,
                indication = null,
                interactionSource = null,
                onClick = currentOnFinished,
            )
            .pointerInput(normalizedSettings.allowSwipeSkip) {
                if (!normalizedSettings.allowSwipeSkip) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { verticalDrag = 0f },
                    onVerticalDrag = { change, amount ->
                        verticalDrag += amount
                        change.consume()
                    },
                    onDragEnd = {
                        if (abs(verticalDrag) >= 72f) currentOnFinished()
                        verticalDrag = 0f
                    },
                    onDragCancel = { verticalDrag = 0f },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isVideo) {
            StartupAnimationVideo(
                uri = uri,
                visible = isVideoRendering,
                settings = normalizedSettings,
                onFirstFrame = { isVideoRendering = true },
                onFinished = onFinished,
                onError = onError,
            )
        } else {
            StartupAnimationImage(
                uri = uri,
                settings = normalizedSettings,
                onFinished = onFinished,
                onError = onError,
            )
        }
        val brightnessOverlay = when {
            normalizedSettings.brightness < 1f -> Color.Black.copy(alpha = 1f - normalizedSettings.brightness)
            normalizedSettings.brightness > 1f -> Color.White.copy(
                alpha = ((normalizedSettings.brightness - 1f) * 0.45f).coerceAtMost(0.16f)
            )
            else -> Color.Transparent
        }
        if (brightnessOverlay.alpha > 0f) {
            Box(Modifier.fillMaxSize().background(brightnessOverlay))
        }
    }
}

@Composable
private fun StartupAnimationVideo(
    uri: Uri,
    visible: Boolean,
    settings: StartupAnimationSettings,
    onFirstFrame: () -> Unit,
    onFinished: () -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnFirstFrame by rememberUpdatedState(onFirstFrame)
    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentOnError by rememberUpdatedState(onError)
    var mediaPlayer by remember(uri) { mutableStateOf<MediaPlayer?>(null) }
    var surface by remember(uri) { mutableStateOf<Surface?>(null) }

    key(uri) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (visible) 1f else 0f },
            factory = { viewContext ->
                TextureView(viewContext).apply textureView@{
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            val nextSurface = Surface(surfaceTexture)
                            surface = nextSurface
                            runCatching {
                                MediaPlayer().apply {
                                    setSurface(nextSurface)
                                    setDataSource(context.applicationContext, uri)
                                    setOnPreparedListener { player ->
                                        player.isLooping = false
                                        applyStartupVideoTransform(
                                            this@textureView,
                                            player.videoWidth,
                                            player.videoHeight,
                                            settings,
                                        )
                                        runCatching {
                                            player.playbackParams = player.playbackParams.setSpeed(settings.playbackSpeed)
                                        }
                                        player.start()
                                        this@textureView.postDelayed({ currentOnFirstFrame() }, 250L)
                                    }
                                    setOnInfoListener { _, what, _ ->
                                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                            currentOnFirstFrame()
                                        }
                                        false
                                    }
                                    setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                                        applyStartupVideoTransform(this@textureView, videoWidth, videoHeight, settings)
                                    }
                                    setOnCompletionListener {
                                        currentOnFinished()
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        currentOnError()
                                        currentOnFinished()
                                        true
                                    }
                                    prepareAsync()
                                }
                            }.onSuccess {
                                mediaPlayer = it
                            }.onFailure {
                                currentOnError()
                                currentOnFinished()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            mediaPlayer?.let { player ->
                                applyStartupVideoTransform(this@textureView, player.videoWidth, player.videoHeight, settings)
                            }
                        }

                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            runCatching { mediaPlayer?.release() }
                            mediaPlayer = null
                            runCatching { surface?.release() }
                            surface = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                    }
                }
            },
        )
    }

    DisposableEffect(uri) {
        onDispose {
            runCatching { mediaPlayer?.release() }
            mediaPlayer = null
            runCatching { surface?.release() }
            surface = null
        }
    }
}

private fun applyStartupVideoTransform(
    textureView: TextureView,
    videoWidth: Int,
    videoHeight: Int,
    settings: StartupAnimationSettings,
) {
    if (videoWidth <= 0 || videoHeight <= 0 || textureView.width <= 0 || textureView.height <= 0) return

    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    val viewAspect = viewWidth / viewHeight
    val crop = settings.cropForViewport(textureView.width, textureView.height)
    val videoAspect = (videoWidth * crop.width).coerceAtLeast(1f) /
        (videoHeight * crop.height).coerceAtLeast(1f)
    val (scaleX, scaleY) = when (settings.scaleMode) {
        StartupAnimationScaleMode.Fill -> 1f to 1f
        StartupAnimationScaleMode.Fit -> if (videoAspect > viewAspect) {
            1f to viewAspect / videoAspect
        } else {
            videoAspect / viewAspect to 1f
        }
        StartupAnimationScaleMode.Crop -> if (videoAspect > viewAspect) {
            videoAspect / viewAspect to 1f
        } else {
            1f to viewAspect / videoAspect
        }
    }

    textureView.setTransform(
        Matrix().apply {
            setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
            val focusX = (crop.left + crop.right) / 2f
            val focusY = (crop.top + crop.bottom) / 2f
            postTranslate(
                (0.5f - focusX) * viewWidth * scaleX,
                (0.5f - focusY) * viewHeight * scaleY,
            )
        }
    )
}

@Composable
private fun StartupAnimationImage(
    uri: Uri,
    settings: StartupAnimationSettings,
    onFinished: () -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentOnError by rememberUpdatedState(onError)
    val drawableResult by produceState<Result<Drawable>?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeDrawable(source)
            }
        }
    }

    val result = drawableResult ?: return
    val drawable = result.getOrNull()
    if (drawable == null) {
        LaunchedEffect(uri) {
            currentOnError()
            currentOnFinished()
        }
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            ImageView(context).apply {
                setBackgroundColor(settings.backgroundArgb.toInt())
                scaleType = ImageView.ScaleType.MATRIX
                setImageDrawable(drawable)
                post { applyStartupImageMatrix(this, drawable, settings) }
            }
        },
        update = { imageView ->
            if (imageView.drawable !== drawable) {
                imageView.setImageDrawable(drawable)
            }
            imageView.setBackgroundColor(settings.backgroundArgb.toInt())
            imageView.scaleType = ImageView.ScaleType.MATRIX
            imageView.post { applyStartupImageMatrix(imageView, drawable, settings) }
        },
    )

    if (drawable is AnimatedImageDrawable) {
        DisposableEffect(drawable) {
            val callback = object : Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    currentOnFinished()
                }
            }
            drawable.repeatCount = 0
            drawable.registerAnimationCallback(callback)
            drawable.start()
            onDispose {
                runCatching { drawable.unregisterAnimationCallback(callback) }
                runCatching { drawable.stop() }
            }
        }
    } else {
        LaunchedEffect(drawable) {
            delay(settings.durationMillis)
            currentOnFinished()
        }
    }
}

private fun applyStartupImageMatrix(
    imageView: ImageView,
    drawable: Drawable,
    settings: StartupAnimationSettings,
) {
    val viewWidth = imageView.width.toFloat()
    val viewHeight = imageView.height.toFloat()
    val sourceWidth = drawable.intrinsicWidth.toFloat()
    val sourceHeight = drawable.intrinsicHeight.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f || sourceWidth <= 0f || sourceHeight <= 0f) return
    val crop = settings.cropForViewport(imageView.width, imageView.height)
    val cropWidth = (sourceWidth * crop.width).coerceAtLeast(1f)
    val cropHeight = (sourceHeight * crop.height).coerceAtLeast(1f)
    val scaleX: Float
    val scaleY: Float
    when (settings.scaleMode) {
        StartupAnimationScaleMode.Fill -> {
            scaleX = viewWidth / cropWidth
            scaleY = viewHeight / cropHeight
        }
        StartupAnimationScaleMode.Fit -> {
            val scale = min(viewWidth / cropWidth, viewHeight / cropHeight)
            scaleX = scale
            scaleY = scale
        }
        StartupAnimationScaleMode.Crop -> {
            val scale = max(viewWidth / cropWidth, viewHeight / cropHeight)
            scaleX = scale
            scaleY = scale
        }
    }
    val cropCenterX = sourceWidth * (crop.left + crop.right) / 2f
    val cropCenterY = sourceHeight * (crop.top + crop.bottom) / 2f
    imageView.imageMatrix = Matrix().apply {
        setScale(scaleX, scaleY)
        postTranslate(
            viewWidth / 2f - cropCenterX * scaleX,
            viewHeight / 2f - cropCenterY * scaleY,
        )
    }
}
