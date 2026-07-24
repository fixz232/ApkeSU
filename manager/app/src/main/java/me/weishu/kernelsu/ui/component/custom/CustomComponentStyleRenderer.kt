package me.weishu.kernelsu.ui.component.custom

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

val LocalCustomCardStyle = staticCompositionLocalOf<CustomCardStyle?> { null }
val LocalCustomSwitchStyle = staticCompositionLocalOf<CustomSwitchStyle?> { null }

@Composable
fun rememberComponentMotionProgress(
    rule: PixelMotionRule,
    enabled: Boolean,
    label: String,
): Float {
    val normalized = rule.normalized()
    val systemAnimationsEnabled = ValueAnimator.areAnimatorsEnabled()
    if (!enabled || !normalized.enabled || !systemAnimationsEnabled) return INACTIVE_MOTION_PROGRESS
    val transition = rememberInfiniteTransition(label = label)
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(normalized.durationMillis, easing = LinearEasing),
            repeatMode = when (normalized.repeat) {
                PixelMotionRepeat.Restart -> RepeatMode.Restart
                PixelMotionRepeat.Reverse -> RepeatMode.Reverse
            },
        ),
        label = "${label}Progress",
    )
    return progress
}

@Composable
fun rememberCustomSwitchImage(style: CustomSwitchStyle?): ImageBitmap? {
    val context = LocalContext.current
    val uri = style?.takeIf { it.source == CustomSwitchSource.Image }?.imageUri
    val image by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            val file = ComponentStyleStore(context).resolveImageFile(uri) ?: return@withContext null
            runCatching { decodeCustomSwitchImage(file) }.getOrNull()
        }
    }
    return image
}

fun DrawScope.drawCustomCardInterior(
    style: CustomCardStyle,
    target: CustomCardTarget,
    alpha: Float,
    motionProgress: Float,
) {
    val layers = style.layersFor(target)
    drawPixelLayer(
        grid = layers.interior,
        topLeft = Offset.Zero,
        destinationSize = size,
        alpha = alpha * 0.70f * motionAlpha(style.motion, motionProgress),
        offsetX = motionOffset(style.motion, motionProgress, size.width / layers.interior.width),
    )
    drawMotionScan(style.motion, motionProgress, alpha * 0.30f)
}

fun DrawScope.drawCustomCardChrome(
    style: CustomCardStyle,
    target: CustomCardTarget,
    alpha: Float,
    motionProgress: Float,
) {
    val layers = style.layersFor(target)
    val motionAlpha = motionAlpha(style.motion, motionProgress)
    val bodyCellWidth = size.width / layers.border.width
    val offsetX = motionOffset(style.motion, motionProgress, bodyCellWidth)
    drawPixelLayer(
        grid = layers.border,
        topLeft = Offset.Zero,
        destinationSize = size,
        alpha = alpha * motionAlpha,
        offsetX = offsetX,
        filter = { x, y, width, height ->
            x < CARD_BORDER_GRID_CELLS || y < CARD_BORDER_GRID_CELLS ||
                x >= width - CARD_BORDER_GRID_CELLS || y >= height - CARD_BORDER_GRID_CELLS
        },
    )
    val topHeight = (size.width / layers.top.width * layers.top.height)
        .coerceAtMost(size.height * MAX_CARD_TOP_HEIGHT_FRACTION)
    drawPixelLayer(
        grid = layers.top,
        topLeft = Offset.Zero,
        destinationSize = Size(size.width, topHeight),
        alpha = alpha * motionAlpha,
        offsetX = motionOffset(style.motion, motionProgress, size.width / layers.top.width),
    )
}

fun DrawScope.drawCustomNavigationStyle(
    style: CustomCardStyle,
    floating: Boolean,
    areaHeight: Float,
    alpha: Float,
    motionProgress: Float,
) {
    val layers = if (floating) style.floatingBottomBar else style.bottomBar
    val sideInset = if (floating) 16.dp.toPx() else 0f
    val verticalInset = if (floating) 8.dp.toPx() else 0f
    val destinationSize = Size(
        width = (size.width - sideInset * 2f).coerceAtLeast(1f),
        height = (areaHeight - verticalInset * 2f).coerceAtLeast(1f),
    )
    val topLeft = Offset(
        x = sideInset,
        y = size.height - areaHeight + verticalInset,
    )
    val dynamicAlpha = alpha * motionAlpha(style.motion, motionProgress)
    val offsetX = motionOffset(style.motion, motionProgress, destinationSize.width / layers.border.width)
    drawPixelLayer(
        grid = layers.border,
        topLeft = topLeft,
        destinationSize = destinationSize,
        alpha = dynamicAlpha,
        offsetX = offsetX,
        filter = { x, y, width, height ->
            x < NAVIGATION_BORDER_GRID_CELLS || y < NAVIGATION_BORDER_GRID_CELLS ||
                x >= width - NAVIGATION_BORDER_GRID_CELLS || y >= height - NAVIGATION_BORDER_GRID_CELLS
        },
    )
    val topHeight = (destinationSize.width / layers.top.width * layers.top.height)
        .coerceAtMost(destinationSize.height * MAX_NAVIGATION_TOP_HEIGHT_FRACTION)
    drawPixelLayer(
        grid = layers.top,
        topLeft = topLeft,
        destinationSize = Size(destinationSize.width, topHeight),
        alpha = dynamicAlpha,
        offsetX = motionOffset(style.motion, motionProgress, destinationSize.width / layers.top.width),
    )
    if (
        style.motion.enabled &&
        style.motion.mode == PixelMotionMode.Scan &&
        motionProgress.isActiveMotionProgress()
    ) {
        val scanY = topLeft.y + destinationSize.height * motionProgress.coerceIn(0f, 1f)
        drawLine(
            color = Color.White.copy(alpha = alpha * 0.22f),
            start = Offset(topLeft.x, scanY),
            end = Offset(topLeft.x + destinationSize.width, scanY),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

fun DrawScope.drawCustomSwitchStyle(
    style: CustomSwitchStyle,
    checkedProgress: Float,
    enabledAlpha: Float,
    motionProgress: Float,
    image: ImageBitmap?,
) {
    val normalized = style.normalized()
    val corner = size.height / 2f
    val path = Path().apply {
        addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(corner, corner)))
    }
    val dynamicAlpha = enabledAlpha * motionAlpha(normalized.motion, motionProgress)
    clipPath(path) {
        drawRoundRect(
            color = Color(0xFF3D4450).copy(alpha = enabledAlpha),
            size = size,
            cornerRadius = CornerRadius(corner, corner),
        )
        if (normalized.source == CustomSwitchSource.Image && image != null) {
            drawSwitchImage(
                image = image,
                scale = normalized.imageScale,
                alpha = dynamicAlpha * normalized.imageOpacity,
            )
        } else {
            val offsetX = motionOffset(
                normalized.motion,
                motionProgress,
                size.width / normalized.trackOff.width,
            )
            drawPixelLayer(
                grid = normalized.trackOff,
                topLeft = Offset.Zero,
                destinationSize = size,
                alpha = dynamicAlpha * (1f - checkedProgress),
                offsetX = offsetX,
            )
            drawPixelLayer(
                grid = normalized.trackOn,
                topLeft = Offset.Zero,
                destinationSize = size,
                alpha = dynamicAlpha * checkedProgress,
                offsetX = offsetX,
            )
        }
        if (
            normalized.motion.enabled &&
            normalized.motion.mode == PixelMotionMode.Scan &&
            motionProgress.isActiveMotionProgress()
        ) {
            val scanX = size.width * motionProgress.coerceIn(0f, 1f)
            drawRect(
                color = Color.White.copy(alpha = enabledAlpha * 0.24f),
                topLeft = Offset(scanX, 0f),
                size = Size(1.dp.toPx(), size.height),
            )
        }
    }

    drawRoundRect(
        color = Color.White.copy(alpha = enabledAlpha * 0.32f),
        size = size,
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 1.dp.toPx()),
    )

    val inset = 3.dp.toPx()
    val radius = size.height / 2f - inset
    val startX = inset + radius
    val endX = size.width - inset - radius
    val center = Offset(startX + (endX - startX) * checkedProgress, size.height / 2f)
    val thumbSize = Size(radius * 2f, radius * 2f)
    val thumbTopLeft = center - Offset(radius, radius)
    val offBlank = normalized.thumbOff.pixels.all { it == TRANSPARENT_PIXEL }
    val onBlank = normalized.thumbOn.pixels.all { it == TRANSPARENT_PIXEL }
    if (offBlank && onBlank) {
        drawCircle(
            color = Color.White.copy(alpha = enabledAlpha),
            radius = radius,
            center = center,
        )
    } else {
        drawPixelLayer(
            grid = normalized.thumbOff,
            topLeft = thumbTopLeft,
            destinationSize = thumbSize,
            alpha = dynamicAlpha * (1f - checkedProgress),
        )
        drawPixelLayer(
            grid = normalized.thumbOn,
            topLeft = thumbTopLeft,
            destinationSize = thumbSize,
            alpha = dynamicAlpha * checkedProgress,
        )
    }
}

fun DrawScope.drawPixelLayer(
    grid: PixelGrid,
    topLeft: Offset,
    destinationSize: Size,
    alpha: Float = 1f,
    offsetX: Float = 0f,
    filter: (x: Int, y: Int, width: Int, height: Int) -> Boolean = { _, _, _, _ -> true },
) {
    if (alpha <= 0f || destinationSize.width <= 0f || destinationSize.height <= 0f) return
    val cellWidth = destinationSize.width / grid.width
    val cellHeight = destinationSize.height / grid.height
    clipRect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + destinationSize.width,
        bottom = topLeft.y + destinationSize.height,
    ) {
        translate(left = offsetX) {
            for (y in 0 until grid.height) {
                for (x in 0 until grid.width) {
                    if (!filter(x, y, grid.width, grid.height)) continue
                    val argb = grid.colorAt(x, y)
                    if (argb == TRANSPARENT_PIXEL) continue
                    val color = Color(argb.toInt())
                    drawRect(
                        color = color.copy(alpha = color.alpha * alpha.coerceIn(0f, 1f)),
                        topLeft = Offset(
                            x = topLeft.x + x * cellWidth,
                            y = topLeft.y + y * cellHeight,
                        ),
                        size = Size(cellWidth + PIXEL_OVERDRAW, cellHeight + PIXEL_OVERDRAW),
                    )
                }
            }
        }
    }
}

private fun decodeCustomSwitchImage(file: File): ImageBitmap? {
    val cacheKey = runCatching {
        "${file.canonicalPath}:${file.length()}:${file.lastModified()}"
    }.getOrElse {
        "${file.absolutePath}:${file.length()}:${file.lastModified()}"
    }
    synchronized(customSwitchImageCacheLock) {
        customSwitchImageCache[cacheKey]?.let { return it }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > MAX_RENDERED_SWITCH_IMAGE_SIDE ||
            bounds.outHeight / sampleSize > MAX_RENDERED_SWITCH_IMAGE_SIDE
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
            ?: return null
        customSwitchImageCache[cacheKey] = decoded
        return decoded
    }
}

private fun DrawScope.drawMotionScan(rule: PixelMotionRule, progress: Float, alpha: Float) {
    if (
        !rule.enabled ||
        rule.mode != PixelMotionMode.Scan ||
        !progress.isActiveMotionProgress() ||
        alpha <= 0f
    ) return
    val y = size.height * progress.coerceIn(0f, 1f)
    drawRect(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(0f, y),
        size = Size(size.width, 1.dp.toPx()),
    )
}

private fun DrawScope.drawSwitchImage(
    image: ImageBitmap,
    scale: SwitchImageScale,
    alpha: Float,
) {
    val sourceWidth = image.width
    val sourceHeight = image.height
    if (sourceWidth <= 0 || sourceHeight <= 0 || alpha <= 0f) return
    val sourceAspect = sourceWidth.toFloat() / sourceHeight
    val destinationAspect = size.width / size.height
    val sourceSize = when (scale) {
        SwitchImageScale.Fit -> androidx.compose.ui.unit.IntSize(sourceWidth, sourceHeight)
        SwitchImageScale.Crop -> if (sourceAspect > destinationAspect) {
            androidx.compose.ui.unit.IntSize((sourceHeight * destinationAspect).toInt(), sourceHeight)
        } else {
            androidx.compose.ui.unit.IntSize(sourceWidth, (sourceWidth / destinationAspect).toInt())
        }
    }
    val sourceOffset = androidx.compose.ui.unit.IntOffset(
        x = ((sourceWidth - sourceSize.width) / 2).coerceAtLeast(0),
        y = ((sourceHeight - sourceSize.height) / 2).coerceAtLeast(0),
    )
    val destination = if (scale == SwitchImageScale.Fit) {
        val fitted = if (sourceAspect > destinationAspect) {
            androidx.compose.ui.unit.IntSize(size.width.toInt(), (size.width / sourceAspect).toInt())
        } else {
            androidx.compose.ui.unit.IntSize((size.height * sourceAspect).toInt(), size.height.toInt())
        }
        fitted
    } else {
        androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
    }
    val destinationOffset = androidx.compose.ui.unit.IntOffset(
        x = ((size.width.toInt() - destination.width) / 2).coerceAtLeast(0),
        y = ((size.height.toInt() - destination.height) / 2).coerceAtLeast(0),
    )
    drawImage(
        image = image,
        srcOffset = sourceOffset,
        srcSize = sourceSize,
        dstOffset = destinationOffset,
        dstSize = destination,
        alpha = alpha.coerceIn(0f, 1f),
    )
}

private fun motionAlpha(rule: PixelMotionRule, progress: Float): Float {
    if (!rule.enabled || rule.mode != PixelMotionMode.Pulse || !progress.isActiveMotionProgress()) return 1f
    return 0.66f + 0.34f * abs(sin(progress.coerceIn(0f, 1f) * PI.toFloat()))
}

private fun motionOffset(rule: PixelMotionRule, progress: Float, cellWidth: Float): Float {
    if (
        !rule.enabled ||
        rule.mode != PixelMotionMode.Drift ||
        rule.amplitudeCells == 0 ||
        !progress.isActiveMotionProgress()
    ) return 0f
    return sin(progress.coerceIn(0f, 1f) * PI.toFloat() * 2f) * cellWidth * rule.amplitudeCells
}

private fun Float.isActiveMotionProgress(): Boolean = isFinite() && this >= 0f

private const val MAX_CARD_TOP_HEIGHT_FRACTION = 0.34f
private const val MAX_NAVIGATION_TOP_HEIGHT_FRACTION = 0.42f
private const val PIXEL_OVERDRAW = 0.35f
private const val MAX_RENDERED_SWITCH_IMAGE_SIDE = 512
private const val MAX_CACHED_SWITCH_IMAGES = 4
private const val INACTIVE_MOTION_PROGRESS = -1f
private val customSwitchImageCacheLock = Any()
private val customSwitchImageCache = object : LinkedHashMap<String, ImageBitmap>(
    MAX_CACHED_SWITCH_IMAGES,
    0.75f,
    true,
) {
    override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<String, ImageBitmap>?,
    ): Boolean = size > MAX_CACHED_SWITCH_IMAGES
}
