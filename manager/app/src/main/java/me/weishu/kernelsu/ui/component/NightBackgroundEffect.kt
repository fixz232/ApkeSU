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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

const val NIGHT_BACKGROUND_EFFECT_KEY = "night_background_effect"

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
        drawNightBase(spec)
        drawStarLayer(stars, progress)
        when (effect) {
            NightBackgroundEffect.Aurora -> drawAurora(progress)
            NightBackgroundEffect.Galaxy -> drawGalaxy(progress)
            NightBackgroundEffect.Nebula -> drawNebula(progress)
            NightBackgroundEffect.Starfield -> drawStarfieldGlow(progress)
            NightBackgroundEffect.Off -> Unit
        }
    }
}

private data class NightBackgroundSpec(
    val cycleMillis: Int,
    val starCount: Int,
    val baseColor: Color,
) {
    companion object {
        fun forEffect(effect: NightBackgroundEffect): NightBackgroundSpec {
            return when (effect) {
                NightBackgroundEffect.Aurora -> NightBackgroundSpec(
                    cycleMillis = 32000,
                    starCount = 58,
                    baseColor = Color(0xFF04120F),
                )

                NightBackgroundEffect.Galaxy -> NightBackgroundSpec(
                    cycleMillis = 36000,
                    starCount = 96,
                    baseColor = Color(0xFF050615),
                )

                NightBackgroundEffect.Nebula -> NightBackgroundSpec(
                    cycleMillis = 34000,
                    starCount = 76,
                    baseColor = Color(0xFF090413),
                )

                NightBackgroundEffect.Starfield -> NightBackgroundSpec(
                    cycleMillis = 30000,
                    starCount = 118,
                    baseColor = Color(0xFF02040D),
                )

                NightBackgroundEffect.Off -> NightBackgroundSpec(
                    cycleMillis = 30000,
                    starCount = 0,
                    baseColor = Color.Transparent,
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
    drawRect(spec.baseColor.copy(alpha = 0.62f))
}

private fun DrawScope.drawStarLayer(stars: List<NightStar>, progress: Float) {
    stars.forEach { star ->
        val drift = sin((progress + star.phase) * TWO_PI) * 4.dp.toPx()
        val twinkle = 0.72f + 0.28f * sin((progress * 2f + star.phase) * TWO_PI).coerceAtLeast(0f)
        drawCircle(
            color = star.tint.copy(alpha = (star.alpha * twinkle).coerceIn(0f, 0.9f)),
            radius = star.radius.dp.toPx(),
            center = Offset(star.x * size.width + drift, star.y * size.height),
        )
    }
}

private fun DrawScope.drawAurora(progress: Float) {
    val height = size.height
    val width = size.width
    val colors = listOf(
        Color(0xFF2DE2A3).copy(alpha = 0.24f),
        Color(0xFF58C7F3).copy(alpha = 0.18f),
        Color(0xFFB86DFF).copy(alpha = 0.14f),
    )
    colors.forEachIndexed { index, color ->
        val top = height * (0.16f + index * 0.09f)
        val wave = sin((progress + index * 0.17f) * TWO_PI) * height * 0.045f
        val path = Path().apply {
            moveTo(-width * 0.12f, top + wave)
            cubicTo(
                width * 0.18f,
                top - height * 0.12f - wave,
                width * 0.42f,
                top + height * 0.18f + wave,
                width * 0.72f,
                top + height * 0.02f - wave,
            )
            cubicTo(
                width * 0.92f,
                top - height * 0.09f,
                width * 1.12f,
                top + height * 0.08f,
                width * 1.18f,
                top,
            )
            lineTo(width * 1.18f, top + height * (0.2f + index * 0.04f))
            cubicTo(
                width * 0.78f,
                top + height * 0.28f,
                width * 0.36f,
                top + height * 0.16f,
                -width * 0.12f,
                top + height * 0.3f,
            )
            close()
        }
        drawPath(path = path, color = color)
        drawPath(
            path = path,
            color = color.copy(alpha = color.alpha * 0.42f),
            style = Stroke(width = (18 + index * 7).dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawGalaxy(progress: Float) {
    val center = Offset(size.width * 0.56f, size.height * 0.42f)
    val longRadius = size.minDimension * 0.56f
    val shortRadius = size.minDimension * 0.16f
    repeat(44) { index ->
        val angle = index * 0.31f + progress * TWO_PI * 0.18f
        val radius = longRadius * (0.18f + index / 44f)
        val x = center.x + cos(angle) * radius
        val y = center.y + sin(angle) * shortRadius * (0.45f + index / 72f)
        val color = if (index % 3 == 0) Color(0xFF8E7DFF) else Color(0xFF7AD7FF)
        drawCircle(
            color = color.copy(alpha = 0.1f + index / 44f * 0.18f),
            radius = (3.2f + index % 5).dp.toPx(),
            center = Offset(x, y),
        )
    }
    drawLine(
        color = Color(0xFFB9A8FF).copy(alpha = 0.18f),
        start = Offset(size.width * 0.12f, size.height * 0.68f),
        end = Offset(size.width * 0.92f, size.height * 0.18f),
        strokeWidth = 28.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFF7DE7FF).copy(alpha = 0.11f),
        start = Offset(size.width * 0.08f, size.height * 0.74f),
        end = Offset(size.width * 0.86f, size.height * 0.24f),
        strokeWidth = 11.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawNebula(progress: Float) {
    val phase = sin(progress * TWO_PI)
    drawCircle(
        color = Color(0xFFFF5FD7).copy(alpha = 0.18f),
        radius = size.minDimension * (0.42f + phase * 0.02f),
        center = Offset(size.width * 0.28f, size.height * 0.32f),
    )
    drawCircle(
        color = Color(0xFF5DDCFF).copy(alpha = 0.13f),
        radius = size.minDimension * (0.34f - phase * 0.015f),
        center = Offset(size.width * 0.72f, size.height * 0.48f),
    )
    drawCircle(
        color = Color(0xFF8B5CFF).copy(alpha = 0.17f),
        radius = size.minDimension * 0.5f,
        center = Offset(size.width * 0.54f, size.height * 0.18f),
    )
}

private fun DrawScope.drawStarfieldGlow(progress: Float) {
    val glow = 0.7f + 0.3f * sin(progress * TWO_PI)
    drawCircle(
        color = Color(0xFF4CC9F0).copy(alpha = 0.12f * glow),
        radius = size.minDimension * 0.42f,
        center = Offset(size.width * 0.78f, size.height * 0.22f),
    )
    drawCircle(
        color = Color(0xFFFFF0B3).copy(alpha = 0.09f * glow),
        radius = size.minDimension * 0.25f,
        center = Offset(size.width * 0.18f, size.height * 0.72f),
    )
}

private fun Random.nextRange(min: Float, max: Float): Float {
    return min + nextFloat() * (max - min)
}

private const val TWO_PI = (PI * 2.0).toFloat()
