package me.weishu.kernelsu.ui.component.ink

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private const val INK_CARD_MOTION_CYCLE_MILLIS = 18_000
private const val STATIC_INK_CARD_MOTION_PROGRESS = 0.24f
private const val TAU = (PI * 2.0).toFloat()

val LocalInkCardMotionEnabled = staticCompositionLocalOf { DEFAULT_INK_CARD_MOTION_ENABLED }

val LocalInkCardMotionProgress = staticCompositionLocalOf<State<Float>> {
    mutableFloatStateOf(STATIC_INK_CARD_MOTION_PROGRESS)
}

@Composable
fun rememberInkCardMotionProgress(enabled: Boolean): State<Float> {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!enabled || !animationsEnabled) {
        return remember { mutableFloatStateOf(STATIC_INK_CARD_MOTION_PROGRESS) }
    }
    val transition = rememberInfiniteTransition(label = "inkCardMotion")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(INK_CARD_MOTION_CYCLE_MILLIS, easing = LinearEasing),
        ),
        label = "inkCardMotionProgress",
    )
}

internal fun DrawScope.drawInkCardMotionUnderlay(
    style: InkStyle,
    palette: InkPalette,
    progress: Float,
) {
    if (!hasInkMotionBounds()) return
    val phase = progress.normalizedInkProgress()
    val washWidth = minOf(size.width * 0.28f, 92.dp.toPx())
    val washCenter = -washWidth + phase * (size.width + washWidth * 2f)
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                palette.mist.copy(alpha = 0.065f),
                palette.primary.copy(alpha = 0.035f),
                Color.Transparent,
            ),
            start = Offset(washCenter - washWidth, size.height),
            end = Offset(washCenter + washWidth, 0f),
        ),
    )

    when (style) {
        InkStyle.MistJiangnan -> drawMovingWaterRings(phase, palette)
        InkStyle.VerdantLandscape -> drawVerdantBreath(phase, palette)
        InkStyle.CinnabarScroll -> drawCinnabarGlow(phase, palette)
        InkStyle.PurpleNightMountain -> drawMoonlitWash(phase, palette)
    }
}

internal fun DrawScope.drawInkCardMotionOverlay(
    style: InkStyle,
    palette: InkPalette,
    progress: Float,
) {
    if (!hasInkMotionBounds()) return
    val phase = progress.normalizedInkProgress()
    when (style) {
        InkStyle.MistJiangnan -> drawFineRainDrift(phase, palette)
        InkStyle.VerdantLandscape -> drawFloatingLeaves(phase, palette)
        InkStyle.CinnabarScroll -> drawFallingBlossoms(phase, palette)
        InkStyle.PurpleNightMountain -> drawNightSparks(phase, palette)
    }
}

internal fun DrawScope.drawInkCapMotion(
    palette: InkPalette,
    progress: Float,
    capHeight: Float,
) {
    if (size.width < 72.dp.toPx() || capHeight <= 1f) return
    val phase = progress.normalizedInkProgress()
    val x = size.width * (0.08f + phase * 0.44f)
    val y = capHeight.coerceAtMost(size.height) * (0.48f + sin(phase * TAU) * 0.08f)
    drawCircle(
        color = palette.primary.copy(alpha = 0.24f),
        radius = 1.15.dp.toPx(),
        center = Offset(x, y),
    )
    drawLine(
        color = palette.water.copy(alpha = 0.10f),
        start = Offset(x - 10.dp.toPx(), y),
        end = Offset(x + 4.dp.toPx(), y),
        strokeWidth = 0.55.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

internal fun DrawScope.drawInkNavigationMotion(
    palette: InkPalette,
    progress: Float,
) {
    val phase = progress.normalizedInkProgress()
    repeat(2) { index ->
        val local = (phase * (0.62f + index * 0.08f) + index * 0.46f).normalizedInkProgress()
        val radius = (14 + index * 8).dp.toPx() * (0.48f + local * 0.64f)
        val center = Offset(size.width * (0.28f + index * 0.45f), size.height * 0.83f)
        val alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.12f
        drawOval(
            color = palette.water.copy(alpha = alpha),
            topLeft = Offset(center.x - radius, center.y - radius * 0.14f),
            size = Size(radius * 2f, radius * 0.28f),
            style = Stroke(0.65.dp.toPx()),
        )
    }
}

private fun DrawScope.drawMovingWaterRings(progress: Float, palette: InkPalette) {
    repeat(2) { index ->
        val local = (progress * 0.72f + index * 0.48f).normalizedInkProgress()
        val radius = (16 + index * 8).dp.toPx() * (0.44f + local * 0.72f)
        val center = Offset(size.width * (0.70f + index * 0.16f), size.height * (0.82f - index * 0.08f))
        val alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.14f
        drawOval(
            color = palette.water.copy(alpha = alpha),
            topLeft = Offset(center.x - radius, center.y - radius * 0.16f),
            size = Size(radius * 2f, radius * 0.32f),
            style = Stroke(0.7.dp.toPx()),
        )
    }
}

private fun DrawScope.drawVerdantBreath(progress: Float, palette: InkPalette) {
    val pulse = (sin(progress * TAU) + 1f) * 0.5f
    repeat(2) { index ->
        drawCircle(
            color = (if (index == 0) palette.primary else palette.secondary)
                .copy(alpha = 0.022f + pulse * 0.018f),
            radius = size.minDimension * (0.22f + index * 0.09f),
            center = Offset(size.width * (0.84f - index * 0.12f), size.height * (0.78f + index * 0.04f)),
        )
    }
}

private fun DrawScope.drawCinnabarGlow(progress: Float, palette: InkPalette) {
    val pulse = (sin(progress * TAU) + 1f) * 0.5f
    drawCircle(
        color = palette.seal.copy(alpha = 0.018f + pulse * 0.022f),
        radius = size.minDimension * (0.20f + pulse * 0.025f),
        center = Offset(size.width * 0.88f, size.height * 0.20f),
    )
}

private fun DrawScope.drawMoonlitWash(progress: Float, palette: InkPalette) {
    val drift = sin(progress * TAU) * size.width * 0.03f
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, palette.mist.copy(alpha = 0.055f), Color.Transparent),
            start = Offset(size.width * 0.62f + drift, 0f),
            end = Offset(size.width * 0.96f + drift, size.height),
        ),
    )
}

private fun DrawScope.drawFineRainDrift(progress: Float, palette: InkPalette) {
    repeat(3) { index ->
        val local = (progress * (0.42f + index * 0.035f) + index * 0.31f).normalizedInkProgress()
        val x = size.width * (0.76f + index * 0.07f) + sin(local * TAU + index) * 3.dp.toPx()
        val y = size.height * (0.08f + local * 0.78f)
        drawLine(
            color = palette.water.copy(alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.18f),
            start = Offset(x, y),
            end = Offset(x - 1.5.dp.toPx(), y + 8.dp.toPx()),
            strokeWidth = 0.55.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawFloatingLeaves(progress: Float, palette: InkPalette) {
    repeat(3) { index ->
        val local = (progress * (0.34f + index * 0.03f) + index * 0.29f).normalizedInkProgress()
        val center = Offset(
            size.width * (0.72f + index * 0.10f) + sin(local * TAU + index) * 4.dp.toPx(),
            size.height * (0.16f + local * 0.66f),
        )
        drawOval(
            color = (if (index == 1) palette.secondary else palette.primary).copy(
                alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.22f,
            ),
            topLeft = center - Offset(2.2.dp.toPx(), 1.1.dp.toPx()),
            size = Size(4.4.dp.toPx(), 2.2.dp.toPx()),
        )
    }
}

private fun DrawScope.drawFallingBlossoms(progress: Float, palette: InkPalette) {
    repeat(3) { index ->
        val local = (progress * (0.30f + index * 0.025f) + index * 0.34f).normalizedInkProgress()
        val center = Offset(
            size.width * (0.78f + index * 0.07f) + sin(local * TAU * 1.2f + index) * 5.dp.toPx(),
            size.height * (0.08f + local * 0.76f),
        )
        val alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.34f
        drawCircle(palette.seal.copy(alpha = alpha), (1.2f + index * 0.18f).dp.toPx(), center)
    }
}

private fun DrawScope.drawNightSparks(progress: Float, palette: InkPalette) {
    val points = listOf(0.72f to 0.20f, 0.84f to 0.36f, 0.92f to 0.14f, 0.77f to 0.58f)
    points.forEachIndexed { index, (x, y) ->
        val pulse = (sin(progress * TAU * (1.1f + index * 0.08f) + index) + 1f) * 0.5f
        val center = Offset(size.width * x, size.height * y)
        val radius = (0.8f + pulse * 0.8f).dp.toPx()
        val color = palette.mist.copy(alpha = 0.12f + pulse * 0.26f)
        drawLine(color, center - Offset(radius, 0f), center + Offset(radius, 0f), 0.55.dp.toPx())
        drawLine(color, center - Offset(0f, radius), center + Offset(0f, radius), 0.55.dp.toPx())
    }
}

private fun DrawScope.hasInkMotionBounds(): Boolean {
    return size.width >= 72.dp.toPx() && size.height >= 48.dp.toPx()
}

private fun Float.normalizedInkProgress(): Float {
    if (!isFinite()) return STATIC_INK_CARD_MOTION_PROGRESS
    return this - floor(this)
}
