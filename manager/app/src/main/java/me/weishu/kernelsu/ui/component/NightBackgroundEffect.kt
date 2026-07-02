package me.weishu.kernelsu.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

const val NIGHT_BACKGROUND_EFFECT_KEY = "night_background_effect"
const val NIGHT_BACKGROUND_PASSTHROUGH_KEY = "night_background_passthrough"
const val NIGHT_BACKGROUND_PASSTHROUGH_OPACITY_KEY = "night_background_passthrough_opacity"
const val MIN_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY = 0.05f
const val MAX_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY = 1f
const val DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY = 0.35f

enum class NightBackgroundEffect(
    val value: String,
    @StringRes val labelRes: Int,
) {
    Off("off", R.string.night_background_effect_off),
    Aurora("aurora", R.string.night_background_effect_aurora),
    Galaxy("galaxy", R.string.night_background_effect_galaxy),
    Nebula("nebula", R.string.night_background_effect_nebula),
    Starfield("starfield", R.string.night_background_effect_starfield);

    companion object {
        val Default = Off
        const val DEFAULT_VALUE = "off"

        fun fromValue(value: String?): NightBackgroundEffect {
            return entries.firstOrNull { it.value == value } ?: Default
        }

        fun fromIndex(index: Int): NightBackgroundEffect {
            return entries.getOrElse(index) { Default }
        }

        fun selectedIndex(value: String): Int {
            return entries.indexOf(fromValue(value)).coerceAtLeast(0)
        }
    }
}

@Composable
fun NightBackgroundEffectOverlay(
    enabled: Boolean,
    effectValue: String,
    passthrough: Boolean,
    passthroughOpacity: Float = DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY,
    modifier: Modifier = Modifier,
) {
    val effect = NightBackgroundEffect.fromValue(effectValue)
    if (!enabled || effect == NightBackgroundEffect.Off) return

    val spec = remember(effect) { NightBackgroundSpec.forEffect(effect) }
    val stars = remember(effect) {
        List(spec.starCount) { index -> NightStar.create(index, effect.value.hashCode()) }
    }
    val transition = rememberInfiniteTransition(label = "nightBackground")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = spec.cycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "nightBackgroundProgress",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val effectAlpha = if (passthrough) {
            sanitizeNightBackgroundPassthroughOpacity(passthroughOpacity)
        } else {
            1f
        }
        if (!passthrough) {
            drawNightBase(spec)
        }
        drawStarLayer(stars, progress, effectAlpha * spec.starAlphaScale)
        when (effect) {
            NightBackgroundEffect.Aurora -> drawAurora(progress, effectAlpha)
            NightBackgroundEffect.Galaxy -> drawGalaxy(progress, effectAlpha)
            NightBackgroundEffect.Nebula -> drawNebula(progress, effectAlpha)
            NightBackgroundEffect.Starfield -> drawStarfieldGlow(progress, effectAlpha)
            NightBackgroundEffect.Off -> Unit
        }
    }
}

private data class NightBackgroundSpec(
    val cycleMillis: Int,
    val starCount: Int,
    val baseColor: Color,
    val starAlphaScale: Float,
) {
    companion object {
        fun forEffect(effect: NightBackgroundEffect): NightBackgroundSpec {
            return when (effect) {
                NightBackgroundEffect.Aurora -> NightBackgroundSpec(
                    cycleMillis = 38000,
                    starCount = 86,
                    baseColor = Color(0xFF020B13),
                    starAlphaScale = 0.88f,
                )

                NightBackgroundEffect.Galaxy -> NightBackgroundSpec(
                    cycleMillis = 42000,
                    starCount = 128,
                    baseColor = Color(0xFF02030C),
                    starAlphaScale = 0.94f,
                )

                NightBackgroundEffect.Nebula -> NightBackgroundSpec(
                    cycleMillis = 34000,
                    starCount = 68,
                    baseColor = Color(0xFF090413),
                    starAlphaScale = 0.86f,
                )

                NightBackgroundEffect.Starfield -> NightBackgroundSpec(
                    cycleMillis = 30000,
                    starCount = 96,
                    baseColor = Color(0xFF02040D),
                    starAlphaScale = 0.92f,
                )

                NightBackgroundEffect.Off -> NightBackgroundSpec(
                    cycleMillis = 30000,
                    starCount = 0,
                    baseColor = Color.Transparent,
                    starAlphaScale = 0f,
                )
            }
        }
    }
}

private data class NightStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val phase: Float,
    val tint: Color,
) {
    companion object {
        fun create(index: Int, effectHash: Int): NightStar {
            val random = Random(effectHash * 43 + index * 7919)
            val tint = when (random.nextInt(4)) {
                0 -> Color(0xFFFFFFFF)
                1 -> Color(0xFFDDEBFF)
                2 -> Color(0xFFFFF6D0)
                else -> Color(0xFFE6D7FF)
            }
            return NightStar(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = random.nextRange(0.65f, 2.15f),
                alpha = random.nextRange(0.24f, 0.74f),
                phase = random.nextFloat(),
                tint = tint,
            )
        }
    }
}

private fun DrawScope.drawNightBase(spec: NightBackgroundSpec) {
    drawRect(spec.baseColor.copy(alpha = 0.74f))
}

private fun DrawScope.drawStarLayer(stars: List<NightStar>, progress: Float, alphaScale: Float) {
    stars.forEach { star ->
        val drift = sin((progress + star.phase) * TWO_PI) * 3.dp.toPx()
        val twinkle = 0.72f + 0.28f * sin((progress * 2f + star.phase) * TWO_PI).coerceAtLeast(0f)
        drawCircle(
            color = star.tint.copy(alpha = (star.alpha * twinkle * alphaScale).coerceIn(0f, 0.9f)),
            radius = star.radius.dp.toPx(),
            center = Offset(star.x * size.width + drift, star.y * size.height),
        )
    }
}

private fun DrawScope.drawAurora(progress: Float, alphaScale: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF3EFFB8).copy(alpha = 0.12f * alphaScale),
                Color(0xFF0B4A3A).copy(alpha = 0.055f * alphaScale),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.5f, size.height * 0.72f),
            radius = size.minDimension * 0.9f,
        ),
        radius = size.minDimension * 0.9f,
        center = Offset(size.width * 0.5f, size.height * 0.72f),
    )

    drawAuroraCurtain(
        progress = progress,
        alphaScale = alphaScale,
        layer = 0,
        baseYRatio = 0.16f,
        thicknessRatio = 0.32f,
        primary = Color(0xFF51F6B3),
        secondary = Color(0xFF92FFF0),
        accent = Color(0xFF785DFF),
    )
    drawAuroraCurtain(
        progress = progress + 0.13f,
        alphaScale = alphaScale,
        layer = 1,
        baseYRatio = 0.24f,
        thicknessRatio = 0.27f,
        primary = Color(0xFF6DFF8F),
        secondary = Color(0xFF38D6FF),
        accent = Color(0xFFB47BFF),
    )
    drawAuroraCurtain(
        progress = progress + 0.27f,
        alphaScale = alphaScale,
        layer = 2,
        baseYRatio = 0.1f,
        thicknessRatio = 0.22f,
        primary = Color(0xFFB9FF7A),
        secondary = Color(0xFF42FFCB),
        accent = Color(0xFFCF6BFF),
    )
}

private fun DrawScope.drawAuroraCurtain(
    progress: Float,
    alphaScale: Float,
    layer: Int,
    baseYRatio: Float,
    thicknessRatio: Float,
    primary: Color,
    secondary: Color,
    accent: Color,
) {
    val width = size.width
    val height = size.height
    val steps = 26
    val topPoints = List(steps + 1) { step ->
        val fraction = step / steps.toFloat()
        val x = -width * 0.1f + width * 1.2f * fraction
        Offset(x, auroraCurveY(fraction, height, baseYRatio, progress, layer))
    }
    val bottomPoints = List(steps + 1) { step ->
        val fraction = step / steps.toFloat()
        val x = -width * 0.1f + width * 1.2f * fraction
        val ripple = sin((fraction * 2.3f - progress * 0.42f + layer * 0.19f) * TWO_PI)
        val y = auroraCurveY(fraction, height, baseYRatio, progress, layer) +
            height * thicknessRatio * (0.78f + ripple * 0.16f)
        Offset(x, y)
    }
    val path = Path().apply {
        topPoints.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
        bottomPoints.asReversed().forEach { point -> lineTo(point.x, point.y) }
        close()
    }
    val startY = height * (baseYRatio - 0.04f)
    val endY = height * (baseYRatio + thicknessRatio + 0.16f)
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.2f to primary.copy(alpha = 0.28f * alphaScale),
            0.42f to secondary.copy(alpha = 0.2f * alphaScale),
            0.7f to accent.copy(alpha = 0.1f * alphaScale),
            1f to Color.Transparent,
            startY = startY,
            endY = endY,
        ),
    )

    repeat(44) { index ->
        val fraction = index / 43f
        val noise = seededUnit(index + layer * 97, 21)
        val x = -width * 0.08f + width * 1.16f * fraction
        val top = auroraCurveY(fraction, height, baseYRatio, progress, layer) + height * (noise - 0.5f) * 0.035f
        val rayLength = height * thicknessRatio * (0.62f + seededUnit(index, 44 + layer) * 0.76f)
        val drift = sin((progress * 0.8f + fraction * 1.8f + layer * 0.31f) * TWO_PI) * width * 0.012f
        val color = when (index % 5) {
            0 -> secondary
            1 -> accent
            else -> primary
        }
        drawLine(
            color = color.copy(alpha = (0.055f + noise * 0.11f) * alphaScale),
            start = Offset(x + drift, top),
            end = Offset(x - drift * 0.35f, top + rayLength),
            strokeWidth = (1.1f + noise * 3.2f).dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    val edgePath = Path().apply {
        topPoints.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
    }
    drawPath(
        path = edgePath,
        color = primary.copy(alpha = 0.12f * alphaScale),
        style = Stroke(width = (2 + layer).dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun auroraCurveY(
    fraction: Float,
    height: Float,
    baseYRatio: Float,
    progress: Float,
    layer: Int,
): Float {
    val slowWave = sin((fraction * 1.15f + progress * 0.32f + layer * 0.17f) * TWO_PI)
    val fineWave = sin((fraction * 3.4f - progress * 0.2f + layer * 0.27f) * TWO_PI)
    return height * baseYRatio + height * (slowWave * 0.045f + fineWave * 0.018f)
}

private fun DrawScope.drawGalaxy(progress: Float, alphaScale: Float) {
    drawMilkyWayBand(progress, alphaScale)

    val core = galaxyBandPoint(0.56f, 0f, progress)
    val coreRadius = size.minDimension * 0.42f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE7B4).copy(alpha = 0.18f * alphaScale),
                Color(0xFFB99AFF).copy(alpha = 0.12f * alphaScale),
                Color(0xFF4E78FF).copy(alpha = 0.055f * alphaScale),
                Color.Transparent,
            ),
            center = core,
            radius = coreRadius,
        ),
        radius = coreRadius,
        center = core,
    )

    repeat(132) { index ->
        val t = seededUnit(index, 101)
        val offset = (seededUnit(index, 102) - 0.5f) * size.minDimension * 0.36f
        val point = galaxyBandPoint(t, offset, progress)
        val twinkle = 0.74f + 0.26f * sin((progress * 2f + seededUnit(index, 103)) * TWO_PI)
        val radius = (0.65f + seededUnit(index, 104) * 2.4f).dp.toPx()
        val warm = seededUnit(index, 105)
        val color = when {
            warm > 0.72f -> Color(0xFFFFE6AA)
            warm > 0.42f -> Color(0xFFDCE8FF)
            else -> Color(0xFFFFFFFF)
        }
        drawCircle(
            color = color.copy(alpha = (0.18f + seededUnit(index, 106) * 0.42f) * twinkle * alphaScale),
            radius = radius,
            center = point,
        )
    }

    repeat(36) { index ->
        val arm = index % 3
        val angle = index * 0.43f + progress * TWO_PI * 0.08f
        val radius = size.minDimension * (0.05f + index / 36f * 0.36f)
        val x = core.x + cos(angle + arm * 0.55f) * radius
        val y = core.y + sin(angle + arm * 0.55f) * radius * 0.34f
        drawCircle(
            color = Color(0xFFC6B8FF).copy(alpha = (0.055f + index / 36f * 0.09f) * alphaScale),
            radius = (2.2f + seededUnit(index, 109) * 4.4f).dp.toPx(),
            center = Offset(x, y),
        )
    }
}

private fun DrawScope.drawMilkyWayBand(progress: Float, alphaScale: Float) {
    val bandWidth = size.minDimension * 0.32f
    val steps = 34
    val topPoints = List(steps + 1) { step ->
        galaxyBandPoint(step / steps.toFloat(), bandWidth * 0.56f, progress)
    }
    val bottomPoints = List(steps + 1) { step ->
        galaxyBandPoint(step / steps.toFloat(), -bandWidth * 0.66f, progress)
    }
    val path = Path().apply {
        topPoints.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
        bottomPoints.asReversed().forEach { point -> lineTo(point.x, point.y) }
        close()
    }
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF354C92).copy(alpha = 0.08f * alphaScale),
                Color(0xFFFFD7A3).copy(alpha = 0.16f * alphaScale),
                Color(0xFF8D7CFF).copy(alpha = 0.12f * alphaScale),
                Color.Transparent,
            ),
            start = Offset(size.width * 0.02f, size.height * 0.88f),
            end = Offset(size.width * 0.94f, size.height * 0.12f),
        ),
    )

    repeat(4) { lane ->
        val laneOffset = bandWidth * (-0.42f + lane * 0.24f)
        val laneStart = galaxyBandPoint(0.02f, laneOffset, progress)
        val laneEnd = galaxyBandPoint(0.98f, laneOffset + bandWidth * 0.1f, progress)
        drawLine(
            color = Color(0xFF05050E).copy(alpha = (0.05f + lane * 0.012f) * alphaScale),
            start = laneStart,
            end = laneEnd,
            strokeWidth = (18 + lane * 7).dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.galaxyBandPoint(t: Float, offset: Float, progress: Float): Offset {
    val x = size.width * (-0.16f + 1.34f * t)
    val y = size.height * (0.84f - 0.69f * t) +
        sin((t * 2.1f + progress * 0.12f) * TWO_PI) * size.height * 0.035f
    val dx = size.width * 1.34f
    val dy = -size.height * 0.69f
    val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val normalX = -dy / length
    val normalY = dx / length
    return Offset(x + normalX * offset, y + normalY * offset)
}

private fun DrawScope.drawNebula(progress: Float, alphaScale: Float) {
    val phase = sin(progress * TWO_PI)
    drawCircle(
        color = Color(0xFFFF5FD7).copy(alpha = 0.15f * alphaScale),
        radius = size.minDimension * (0.42f + phase * 0.02f),
        center = Offset(size.width * 0.28f, size.height * 0.32f),
    )
    drawCircle(
        color = Color(0xFF5DDCFF).copy(alpha = 0.11f * alphaScale),
        radius = size.minDimension * (0.34f - phase * 0.015f),
        center = Offset(size.width * 0.72f, size.height * 0.48f),
    )
    drawCircle(
        color = Color(0xFF8B5CFF).copy(alpha = 0.14f * alphaScale),
        radius = size.minDimension * 0.5f,
        center = Offset(size.width * 0.54f, size.height * 0.18f),
    )
}

private fun DrawScope.drawStarfieldGlow(progress: Float, alphaScale: Float) {
    val glow = 0.7f + 0.3f * sin(progress * TWO_PI)
    drawCircle(
        color = Color(0xFF4CC9F0).copy(alpha = 0.1f * glow * alphaScale),
        radius = size.minDimension * 0.42f,
        center = Offset(size.width * 0.78f, size.height * 0.22f),
    )
    drawCircle(
        color = Color(0xFFFFF0B3).copy(alpha = 0.075f * glow * alphaScale),
        radius = size.minDimension * 0.25f,
        center = Offset(size.width * 0.18f, size.height * 0.72f),
    )
}

fun sanitizeNightBackgroundPassthroughOpacity(value: Float): Float {
    return if (value.isFinite()) {
        value.coerceIn(MIN_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY, MAX_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY)
    } else {
        DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY
    }
}

private fun Random.nextRange(min: Float, max: Float): Float {
    return min + nextFloat() * (max - min)
}

private fun seededUnit(index: Int, salt: Int): Float {
    val raw = sin(index * 12.9898f + salt * 78.233f) * 43758.547f
    return raw - floor(raw)
}

private const val TWO_PI = (PI * 2.0).toFloat()
