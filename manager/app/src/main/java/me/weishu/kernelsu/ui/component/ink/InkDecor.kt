package me.weishu.kernelsu.ui.component.ink

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.custom.CustomCardTarget
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max

@Composable
@ReadOnlyComposable
fun isInkInterfaceStyle(): Boolean = LocalInterfaceStyle.current == InterfaceStyle.Ink.value

@Composable
fun InkBackdrop(modifier: Modifier = Modifier) {
    if (!isInkInterfaceStyle()) return
    val style = LocalInkStyle.current
    val palette = inkPalette(style, isInDarkTheme())
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(palette.backgroundTop, palette.backgroundBottom),
            ),
        )
        drawInkBackdropScene(style, palette)
    }
}

@Composable
fun InkChromeOverlay(modifier: Modifier = Modifier) {
    if (!isInkInterfaceStyle()) return
    val style = LocalInkStyle.current
    val palette = inkPalette(style, isInDarkTheme())
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Canvas(modifier = modifier.fillMaxSize()) {
        val top = max(8.dp.toPx(), statusBarPadding.toPx() + 5.dp.toPx())
        val inset = 16.dp.toPx()
        val lineWidth = 0.75.dp.toPx()
        drawLine(
            color = palette.primary.copy(alpha = 0.46f),
            start = Offset(inset, top),
            end = Offset(size.width * 0.31f, top),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = palette.secondary.copy(alpha = 0.38f),
            start = Offset(size.width * 0.69f, top),
            end = Offset(size.width - inset - 12.dp.toPx(), top),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round,
        )
        drawInkSeal(
            topLeft = Offset(size.width - inset - 7.dp.toPx(), top - 3.5.dp.toPx()),
            side = 7.dp.toPx(),
            color = palette.seal.copy(alpha = 0.72f),
        )
    }
}

@Composable
fun Modifier.inkMiuixCardSurface(
    shape: Shape = RoundedCornerShape(13.dp),
    enabled: Boolean = true,
    capHeight: Dp = 13.dp,
    customTarget: CustomCardTarget = CustomCardTarget.Default,
): Modifier {
    if (!enabled || !isInkInterfaceStyle()) {
        return uiDecoratedCard(shape = shape, enabled = enabled, customTarget = customTarget)
    }
    val style = LocalInkStyle.current
    val palette = inkPalette(style, isInDarkTheme())
    val motionEnabled = LocalInkCardMotionEnabled.current
    val motionProgress = LocalInkCardMotionProgress.current.value
    val surfaceBrush = Brush.verticalGradient(
        listOf(palette.surfaceTop, palette.surfaceBottom),
    )
    return this
        .shadow(
            elevation = 1.dp,
            shape = shape,
            ambientColor = palette.shadow.copy(alpha = 0.12f),
            spotColor = palette.shadow.copy(alpha = 0.10f),
        )
        .clip(shape)
        .background(surfaceBrush, shape)
        .border(0.75.dp, palette.outline.copy(alpha = 0.58f), shape)
        .drawWithContent {
            if (motionEnabled) {
                drawInkCardMotionUnderlay(style, palette, motionProgress)
            }
            drawInkCardInterior(style, palette)
            drawContent()
            if (motionEnabled) {
                drawInkCardMotionOverlay(style, palette, motionProgress)
            }
            drawInkCardFrame(style, palette, capHeight.toPx())
            if (motionEnabled) {
                drawInkCapMotion(palette, motionProgress, capHeight.toPx())
            }
        }
        .uiDecoratedCard(
            shape = shape,
            enabled = true,
            customTarget = customTarget,
            nativeInterior = true,
        )
}

@Composable
fun inkMiuixCardColors(
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
    enabled: Boolean = true,
) = if (enabled && isInkInterfaceStyle()) {
    CardDefaults.defaultColors(color = color.copy(alpha = 0f))
} else {
    CardDefaults.defaultColors(color = color)
}

@Composable
fun inkNavigationContainerColor(): Color {
    val palette = inkPalette(LocalInkStyle.current, isInDarkTheme())
    return palette.surfaceBottom.copy(alpha = if (isInDarkTheme()) 0.88f else 0.84f)
}

@Composable
fun Modifier.inkNavigationSurface(
    shape: Shape,
    paintBackground: Boolean = true,
): Modifier {
    if (!isInkInterfaceStyle()) return this
    val style = LocalInkStyle.current
    val palette = inkPalette(style, isInDarkTheme())
    val motionEnabled = LocalInkCardMotionEnabled.current
    val motionProgress = LocalInkCardMotionProgress.current.value
    return clip(shape)
        .then(if (paintBackground) Modifier.background(inkNavigationContainerColor(), shape) else Modifier)
        .border(0.75.dp, palette.outline.copy(alpha = 0.56f), shape)
        .drawWithContent {
            drawInkNavigationUnderlay(style, palette)
            drawContent()
            if (motionEnabled) {
                drawInkNavigationMotion(palette, motionProgress)
            }
            drawInkNavigationFrame(palette)
        }
}

@Composable
fun Modifier.inkNavigationIndicator(
    shape: Shape,
    paintBackground: Boolean = true,
    interactionKey: Any? = Unit,
): Modifier {
    if (!isInkInterfaceStyle()) return this
    val palette = inkPalette(LocalInkStyle.current, isInDarkTheme())
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val spread = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(interactionKey, animationsEnabled) {
        if (animationsEnabled) {
            spread.snapTo(0f)
            spread.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
        } else {
            spread.snapTo(1f)
        }
    }
    return clip(shape)
        .then(
            if (paintBackground) {
                Modifier.background(palette.primary.copy(alpha = 0.15f), shape)
            } else {
                Modifier
            },
        )
        .border(0.75.dp, palette.primary.copy(alpha = 0.52f), shape)
        .drawWithContent {
            val progress = spread.value
            drawOval(
                color = palette.primary.copy(alpha = 0.10f),
                topLeft = Offset(size.width * 0.10f, size.height * 0.30f),
                size = Size(size.width * 0.80f, size.height * 0.44f),
            )
            drawContent()
            if (progress < 1f) {
                val radiusX = size.width * (0.16f + progress * 0.54f)
                val radiusY = size.height * (0.12f + progress * 0.28f)
                drawOval(
                    color = palette.secondary.copy(alpha = (1f - progress) * 0.40f),
                    topLeft = Offset(size.width / 2f - radiusX, size.height / 2f - radiusY),
                    size = Size(radiusX * 2f, radiusY * 2f),
                    style = Stroke(0.8.dp.toPx()),
                )
            }
        }
}

@Composable
fun inkTopBarContainerColor(): Color = inkNavigationContainerColor().copy(alpha = 0.72f)

@Composable
fun inkTopBarContentColor(): Color =
    inkPalette(LocalInkStyle.current, isInDarkTheme()).content

private fun DrawScope.drawInkBackdropScene(style: InkStyle, palette: InkPalette) {
    val moonCenter = Offset(size.width * 0.80f, size.height * 0.17f)
    when (style) {
        InkStyle.PurpleNightMountain -> {
            drawCircle(
                color = palette.mist.copy(alpha = 0.08f),
                radius = size.minDimension * 0.105f,
                center = moonCenter,
            )
            drawCircle(
                color = palette.mist.copy(alpha = 0.36f),
                radius = size.minDimension * 0.055f,
                center = moonCenter,
            )
        }

        InkStyle.CinnabarScroll -> drawCircle(
            color = palette.seal.copy(alpha = 0.08f),
            radius = size.minDimension * 0.075f,
            center = moonCenter,
        )

        else -> Unit
    }

    drawMountainLayer(
        color = palette.farMountain.copy(alpha = 0.13f),
        baseline = size.height * 0.69f,
        amplitude = size.height * 0.17f,
        phase = 0.05f,
    )
    drawMountainLayer(
        color = palette.nearMountain.copy(alpha = 0.22f),
        baseline = size.height * 0.82f,
        amplitude = size.height * 0.20f,
        phase = 0.34f,
    )

    listOf(0.52f to 0.11f, 0.66f to 0.08f, 0.76f to 0.06f).forEachIndexed { index, (y, alpha) ->
        val inset = size.width * (0.08f + index * 0.06f)
        drawLine(
            color = palette.mist.copy(alpha = alpha),
            start = Offset(inset, size.height * y),
            end = Offset(size.width - inset * 0.72f, size.height * y + index * 2.dp.toPx()),
            strokeWidth = (7 - index * 1.5f).dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    repeat(4) { index ->
        val y = size.height * (0.84f + index * 0.026f)
        drawLine(
            color = palette.water.copy(alpha = 0.15f - index * 0.025f),
            start = Offset(size.width * (0.12f + index * 0.035f), y),
            end = Offset(size.width * (0.88f - index * 0.03f), y),
            strokeWidth = 0.65.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    when (style) {
        InkStyle.MistJiangnan -> repeat(14) { index ->
            val x = size.width * (0.08f + index * 0.061f)
            val y = size.height * (0.08f + (index % 5) * 0.075f)
            drawLine(
                color = palette.water.copy(alpha = 0.09f),
                start = Offset(x, y),
                end = Offset(x - 1.5.dp.toPx(), y + 10.dp.toPx()),
                strokeWidth = 0.55.dp.toPx(),
            )
        }

        InkStyle.VerdantLandscape -> repeat(8) { index ->
            drawCircle(
                color = (if (index % 3 == 0) palette.secondary else palette.primary)
                    .copy(alpha = 0.11f + index % 2 * 0.025f),
                radius = (1f + index % 3 * 0.4f).dp.toPx(),
                center = Offset(
                    size.width * (0.10f + index * 0.10f),
                    size.height * (0.18f + (index % 4) * 0.05f),
                ),
            )
        }

        InkStyle.CinnabarScroll -> {
            val branch = Path().apply {
                moveTo(size.width * 0.04f, size.height * 0.27f)
                cubicTo(
                    size.width * 0.14f,
                    size.height * 0.19f,
                    size.width * 0.18f,
                    size.height * 0.29f,
                    size.width * 0.31f,
                    size.height * 0.18f,
                )
            }
            drawPath(
                path = branch,
                color = palette.nearMountain.copy(alpha = 0.24f),
                style = Stroke(0.9.dp.toPx(), cap = StrokeCap.Round),
            )
            listOf(0.11f to 0.225f, 0.20f to 0.245f, 0.285f to 0.195f).forEach { (x, y) ->
                drawCircle(palette.seal.copy(alpha = 0.30f), 1.6.dp.toPx(), Offset(size.width * x, size.height * y))
            }
        }

        InkStyle.PurpleNightMountain -> repeat(11) { index ->
            drawCircle(
                color = palette.mist.copy(alpha = 0.13f + index % 3 * 0.04f),
                radius = (0.5f + index % 2 * 0.3f).dp.toPx(),
                center = Offset(
                    size.width * (0.08f + index * 0.075f),
                    size.height * (0.08f + (index % 4) * 0.04f),
                ),
            )
        }
    }
}

private fun DrawScope.drawMountainLayer(
    color: Color,
    baseline: Float,
    amplitude: Float,
    phase: Float,
) {
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, baseline)
        cubicTo(
            size.width * 0.13f,
            baseline - amplitude * (0.42f + phase),
            size.width * 0.21f,
            baseline + amplitude * 0.05f,
            size.width * 0.34f,
            baseline - amplitude * 0.66f,
        )
        cubicTo(
            size.width * 0.48f,
            baseline - amplitude * 0.10f,
            size.width * 0.58f,
            baseline - amplitude * (0.88f - phase),
            size.width * 0.72f,
            baseline - amplitude * 0.24f,
        )
        cubicTo(
            size.width * 0.83f,
            baseline + amplitude * 0.03f,
            size.width * 0.92f,
            baseline - amplitude * 0.56f,
            size.width,
            baseline - amplitude * 0.18f,
        )
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawInkCardInterior(style: InkStyle, palette: InkPalette) {
    if (size.width < 72.dp.toPx() || size.height < 48.dp.toPx()) return
    val horizon = size.height * 0.79f
    val farMountain = Path().apply {
        moveTo(size.width * 0.48f, size.height)
        lineTo(size.width * 0.48f, horizon)
        cubicTo(
            size.width * 0.58f,
            horizon - size.height * 0.18f,
            size.width * 0.65f,
            horizon + size.height * 0.02f,
            size.width * 0.73f,
            horizon - size.height * 0.12f,
        )
        cubicTo(
            size.width * 0.82f,
            horizon,
            size.width * 0.91f,
            horizon - size.height * 0.15f,
            size.width,
            horizon - size.height * 0.04f,
        )
        lineTo(size.width, size.height)
        close()
    }
    drawPath(farMountain, palette.farMountain.copy(alpha = 0.055f))

    val nearMountain = Path().apply {
        moveTo(size.width * 0.68f, size.height)
        lineTo(size.width * 0.68f, size.height * 0.88f)
        cubicTo(
            size.width * 0.78f,
            size.height * 0.70f,
            size.width * 0.84f,
            size.height * 0.92f,
            size.width,
            size.height * 0.74f,
        )
        lineTo(size.width, size.height)
        close()
    }
    drawPath(nearMountain, palette.nearMountain.copy(alpha = 0.045f))

    repeat(3) { index ->
        val y = size.height * (0.84f + index * 0.052f)
        drawLine(
            color = palette.water.copy(alpha = 0.10f - index * 0.02f),
            start = Offset(size.width * (0.56f + index * 0.04f), y),
            end = Offset(size.width * (0.94f - index * 0.03f), y),
            strokeWidth = 0.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    when (style) {
        InkStyle.MistJiangnan -> {
            val bridgeY = size.height * 0.76f
            drawLine(
                color = palette.nearMountain.copy(alpha = 0.10f),
                start = Offset(size.width * 0.72f, bridgeY),
                end = Offset(size.width * 0.91f, bridgeY - 1.dp.toPx()),
                strokeWidth = 0.65.dp.toPx(),
                cap = StrokeCap.Round,
            )
            listOf(0.75f, 0.88f).forEach { x ->
                drawLine(
                    color = palette.nearMountain.copy(alpha = 0.08f),
                    start = Offset(size.width * x, bridgeY),
                    end = Offset(size.width * x, bridgeY + 5.dp.toPx()),
                    strokeWidth = 0.55.dp.toPx(),
                )
            }
        }

        InkStyle.VerdantLandscape -> repeat(5) { index ->
            val center = Offset(
                size.width * (0.70f + index * 0.055f),
                size.height * (0.54f - index % 2 * 0.04f),
            )
            drawOval(
                color = (if (index == 2) palette.secondary else palette.primary)
                    .copy(alpha = 0.065f + index % 2 * 0.015f),
                topLeft = center - Offset(2.6.dp.toPx(), 1.2.dp.toPx()),
                size = Size(5.2.dp.toPx(), 2.4.dp.toPx()),
            )
        }

        InkStyle.CinnabarScroll -> {
            val branch = Path().apply {
                moveTo(size.width * 0.74f, size.height * 0.12f)
                cubicTo(
                    size.width * 0.82f,
                    size.height * 0.17f,
                    size.width * 0.86f,
                    size.height * 0.10f,
                    size.width * 0.98f,
                    size.height * 0.19f,
                )
            }
            drawPath(
                branch,
                palette.nearMountain.copy(alpha = 0.10f),
                style = Stroke(0.7.dp.toPx(), cap = StrokeCap.Round),
            )
            listOf(0.80f to 0.15f, 0.88f to 0.13f, 0.94f to 0.18f).forEach { (x, y) ->
                drawCircle(
                    color = palette.seal.copy(alpha = 0.14f),
                    radius = 1.35.dp.toPx(),
                    center = Offset(size.width * x, size.height * y),
                )
            }
        }

        InkStyle.PurpleNightMountain -> {
            drawCircle(
                color = palette.mist.copy(alpha = 0.06f),
                radius = 12.dp.toPx(),
                center = Offset(size.width * 0.88f, size.height * 0.23f),
            )
            drawCircle(
                color = palette.mist.copy(alpha = 0.18f),
                radius = 5.dp.toPx(),
                center = Offset(size.width * 0.88f, size.height * 0.23f),
            )
        }
    }
}

private fun DrawScope.drawInkCardFrame(style: InkStyle, palette: InkPalette, requestedCapHeight: Float) {
    if (size.width < 72.dp.toPx() || size.height < 48.dp.toPx()) return
    val capHeight = minOf(requestedCapHeight, size.height * 0.18f).coerceAtLeast(0f)
    if (capHeight > 0f) {
        val ridge = Path().apply {
            moveTo(0f, capHeight * 0.78f)
            cubicTo(
                size.width * 0.08f,
                capHeight * 0.32f,
                size.width * 0.16f,
                capHeight * 0.90f,
                size.width * 0.25f,
                capHeight * 0.48f,
            )
            cubicTo(
                size.width * 0.32f,
                capHeight * 0.18f,
                size.width * 0.40f,
                capHeight * 0.80f,
                size.width * 0.50f,
                capHeight * 0.52f,
            )
        }
        drawPath(
            ridge,
            (if (style == InkStyle.CinnabarScroll) palette.nearMountain else palette.primary)
                .copy(alpha = 0.38f),
            style = Stroke(0.75.dp.toPx(), cap = StrokeCap.Round),
        )
        drawLine(
            color = palette.water.copy(alpha = 0.24f),
            start = Offset(size.width * 0.54f, capHeight * 0.64f),
            end = Offset(size.width * 0.78f, capHeight * 0.64f),
            strokeWidth = 0.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
        when (style) {
            InkStyle.MistJiangnan -> repeat(3) { index ->
                val x = size.width * (0.60f + index * 0.055f)
                drawLine(
                    color = palette.water.copy(alpha = 0.18f),
                    start = Offset(x, capHeight * 0.22f),
                    end = Offset(x - 1.dp.toPx(), capHeight * 0.48f),
                    strokeWidth = 0.5.dp.toPx(),
                )
            }

            InkStyle.VerdantLandscape -> repeat(3) { index ->
                drawCircle(
                    color = (if (index == 1) palette.secondary else palette.primary).copy(alpha = 0.28f),
                    radius = (0.8f + index * 0.15f).dp.toPx(),
                    center = Offset(size.width * (0.60f + index * 0.07f), capHeight * (0.36f + index % 2 * 0.16f)),
                )
            }

            InkStyle.CinnabarScroll -> repeat(2) { index ->
                drawCircle(
                    color = palette.seal.copy(alpha = 0.40f),
                    radius = (1.2f + index * 0.25f).dp.toPx(),
                    center = Offset(size.width * (0.64f + index * 0.09f), capHeight * (0.44f - index * 0.10f)),
                )
            }

            InkStyle.PurpleNightMountain -> {
                drawCircle(
                    color = palette.mist.copy(alpha = 0.34f),
                    radius = 2.2.dp.toPx(),
                    center = Offset(size.width * 0.68f, capHeight * 0.40f),
                )
                drawCircle(
                    color = palette.secondary.copy(alpha = 0.30f),
                    radius = 0.8.dp.toPx(),
                    center = Offset(size.width * 0.76f, capHeight * 0.28f),
                )
            }
        }
    }
    drawLine(
        color = palette.water.copy(alpha = 0.28f),
        start = Offset(8.dp.toPx(), size.height - 1.dp.toPx()),
        end = Offset(size.width * 0.30f, size.height - 1.dp.toPx()),
        strokeWidth = 0.55.dp.toPx(),
        cap = StrokeCap.Round,
    )
    if (style == InkStyle.CinnabarScroll || size.width >= 160.dp.toPx()) {
        drawInkSeal(
            topLeft = Offset(size.width - 11.dp.toPx(), 5.dp.toPx()),
            side = 6.dp.toPx(),
            color = palette.seal.copy(alpha = 0.60f),
        )
    }
}

private fun DrawScope.drawInkSeal(topLeft: Offset, side: Float, color: Color) {
    drawRect(color = color, topLeft = topLeft, size = Size(side, side), style = Stroke(side * 0.12f))
    drawLine(
        color = color.copy(alpha = color.alpha * 0.82f),
        start = topLeft + Offset(side * 0.26f, side * 0.22f),
        end = topLeft + Offset(side * 0.26f, side * 0.78f),
        strokeWidth = side * 0.10f,
    )
    drawLine(
        color = color.copy(alpha = color.alpha * 0.82f),
        start = topLeft + Offset(side * 0.26f, side * 0.50f),
        end = topLeft + Offset(side * 0.74f, side * 0.50f),
        strokeWidth = side * 0.10f,
    )
}

private fun DrawScope.drawInkNavigationUnderlay(style: InkStyle, palette: InkPalette) {
    val topY = 1.5.dp.toPx()
    drawLine(
        color = palette.water.copy(alpha = 0.20f),
        start = Offset(size.width * 0.08f, topY),
        end = Offset(size.width * 0.92f, topY),
        strokeWidth = 0.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    when (style) {
        InkStyle.MistJiangnan -> repeat(4) { index ->
            val x = size.width * (0.18f + index * 0.19f)
            drawLine(
                color = palette.water.copy(alpha = 0.10f),
                start = Offset(x, size.height * 0.14f),
                end = Offset(x - 1.dp.toPx(), size.height * 0.23f),
                strokeWidth = 0.5.dp.toPx(),
            )
        }

        InkStyle.VerdantLandscape -> repeat(4) { index ->
            val center = Offset(size.width * (0.18f + index * 0.19f), size.height * 0.18f)
            drawOval(
                color = (if (index == 2) palette.secondary else palette.primary).copy(alpha = 0.12f),
                topLeft = center - Offset(2.4.dp.toPx(), 1.dp.toPx()),
                size = Size(4.8.dp.toPx(), 2.dp.toPx()),
            )
        }

        InkStyle.CinnabarScroll -> {
            drawLine(
                color = palette.nearMountain.copy(alpha = 0.12f),
                start = Offset(size.width * 0.10f, size.height * 0.22f),
                end = Offset(size.width * 0.30f, size.height * 0.13f),
                strokeWidth = 0.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            listOf(0.16f, 0.23f).forEach { x ->
                drawCircle(
                    color = palette.seal.copy(alpha = 0.22f),
                    radius = 1.25.dp.toPx(),
                    center = Offset(size.width * x, size.height * 0.17f),
                )
            }
        }

        InkStyle.PurpleNightMountain -> repeat(5) { index ->
            drawCircle(
                color = palette.mist.copy(alpha = 0.10f + index % 2 * 0.04f),
                radius = (0.55f + index % 2 * 0.25f).dp.toPx(),
                center = Offset(size.width * (0.14f + index * 0.18f), size.height * 0.20f),
            )
        }
    }
}

private fun DrawScope.drawInkNavigationFrame(palette: InkPalette) {
    repeat(2) { index ->
        val y = size.height - (4 + index * 3).dp.toPx()
        drawLine(
            color = palette.water.copy(alpha = 0.16f - index * 0.04f),
            start = Offset(size.width * (0.12f + index * 0.06f), y),
            end = Offset(size.width * (0.88f - index * 0.05f), y),
            strokeWidth = 0.65.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
