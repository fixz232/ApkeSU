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
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

const val GLOBAL_SNOW_ENABLED_KEY = "global_snow_enabled"
const val GLOBAL_SNOW_EFFECT_KEY = "global_snow_effect"

enum class GlobalSnowEffect(
    val value: String,
    @StringRes val labelRes: Int,
) {
    Gentle("gentle", R.string.settings_global_snow_effect_gentle),
    Blizzard("blizzard", R.string.settings_global_snow_effect_blizzard),
    Starlight("starlight", R.string.settings_global_snow_effect_starlight),
    Pink("pink", R.string.settings_global_snow_effect_pink),
    Crystal("crystal", R.string.settings_global_snow_effect_crystal);

    companion object {
        val Default = Gentle
        const val DEFAULT_VALUE = "gentle"

        fun fromValue(value: String?): GlobalSnowEffect {
            return entries.firstOrNull { it.value == value } ?: Default
        }

        fun fromIndex(index: Int): GlobalSnowEffect {
            return entries.getOrElse(index) { Default }
        }

        fun selectedIndex(value: String): Int {
            return entries.indexOf(fromValue(value)).coerceAtLeast(0)
        }
    }
}

@Composable
fun GlobalSnowEffectOverlay(
    enabled: Boolean,
    effectValue: String,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    val effect = GlobalSnowEffect.fromValue(effectValue)
    val spec = remember(effect) { SnowSpec.forEffect(effect) }
    val flakes = remember(effect) {
        List(spec.count) { index -> SnowFlake.create(index, spec, effect.value.hashCode()) }
    }
    val transition = rememberInfiniteTransition(label = "globalSnow")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = spec.cycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "globalSnowProgress",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val margin = 28.dp.toPx()
        val travelHeight = size.height + margin * 2f
        flakes.forEach { flake ->
            val yProgress = (flake.baseY + progress * flake.speed) % 1f
            val sway = sin((progress * flake.swaySpeed + flake.phase) * TWO_PI) * flake.drift
            val rawX = (flake.baseX + sway) * size.width
            val x = ((rawX % size.width) + size.width) % size.width
            val y = yProgress * travelHeight - margin
            drawFlake(
                effect = effect,
                center = Offset(x, y),
                radius = flake.radius,
                alpha = flake.alpha,
                twinkle = flake.twinkle(progress),
                color = spec.color,
                accentColor = spec.accentColor,
            )
        }
    }
}

private data class SnowSpec(
    val count: Int,
    val cycleMillis: Int,
    val minRadius: Float,
    val maxRadius: Float,
    val minAlpha: Float,
    val maxAlpha: Float,
    val minSpeed: Float,
    val maxSpeed: Float,
    val maxDrift: Float,
    val color: Color,
    val accentColor: Color,
) {
    companion object {
        fun forEffect(effect: GlobalSnowEffect): SnowSpec {
            return when (effect) {
                GlobalSnowEffect.Gentle -> SnowSpec(
                    count = 52,
                    cycleMillis = 19000,
                    minRadius = 1.8f,
                    maxRadius = 4.6f,
                    minAlpha = 0.3f,
                    maxAlpha = 0.72f,
                    minSpeed = 0.55f,
                    maxSpeed = 1.05f,
                    maxDrift = 0.038f,
                    color = Color.White,
                    accentColor = Color(0xFFEFF6FF),
                )

                GlobalSnowEffect.Blizzard -> SnowSpec(
                    count = 84,
                    cycleMillis = 10500,
                    minRadius = 1.4f,
                    maxRadius = 3.4f,
                    minAlpha = 0.2f,
                    maxAlpha = 0.62f,
                    minSpeed = 1.0f,
                    maxSpeed = 1.95f,
                    maxDrift = 0.075f,
                    color = Color.White,
                    accentColor = Color(0xFFDCEBFF),
                )

                GlobalSnowEffect.Starlight -> SnowSpec(
                    count = 44,
                    cycleMillis = 15800,
                    minRadius = 2.0f,
                    maxRadius = 4.3f,
                    minAlpha = 0.28f,
                    maxAlpha = 0.78f,
                    minSpeed = 0.45f,
                    maxSpeed = 0.96f,
                    maxDrift = 0.048f,
                    color = Color.White,
                    accentColor = Color(0xFFFFF7CC),
                )

                GlobalSnowEffect.Pink -> SnowSpec(
                    count = 56,
                    cycleMillis = 17200,
                    minRadius = 1.7f,
                    maxRadius = 4.8f,
                    minAlpha = 0.26f,
                    maxAlpha = 0.68f,
                    minSpeed = 0.52f,
                    maxSpeed = 1.12f,
                    maxDrift = 0.052f,
                    color = Color(0xFFFFF4FA),
                    accentColor = Color(0xFFFFB7D5),
                )

                GlobalSnowEffect.Crystal -> SnowSpec(
                    count = 48,
                    cycleMillis = 17800,
                    minRadius = 2.2f,
                    maxRadius = 5.0f,
                    minAlpha = 0.27f,
                    maxAlpha = 0.74f,
                    minSpeed = 0.5f,
                    maxSpeed = 1.08f,
                    maxDrift = 0.045f,
                    color = Color(0xFFF8FCFF),
                    accentColor = Color(0xFFBDEBFF),
                )
            }
        }
    }
}

private data class SnowFlake(
    val baseX: Float,
    val baseY: Float,
    val speed: Float,
    val swaySpeed: Float,
    val drift: Float,
    val radius: Float,
    val alpha: Float,
    val phase: Float,
) {
    fun twinkle(progress: Float): Float {
        return 0.72f + 0.28f * sin((progress * swaySpeed + phase) * TWO_PI).coerceAtLeast(0f)
    }

    companion object {
        fun create(index: Int, spec: SnowSpec, effectHash: Int): SnowFlake {
            val random = Random(effectHash * 31 + index * 9973)
            return SnowFlake(
                baseX = random.nextFloat(),
                baseY = random.nextFloat(),
                speed = random.nextRange(spec.minSpeed, spec.maxSpeed),
                swaySpeed = random.nextRange(0.45f, 1.45f),
                drift = random.nextRange(spec.maxDrift * 0.2f, spec.maxDrift),
                radius = random.nextRange(spec.minRadius, spec.maxRadius),
                alpha = random.nextRange(spec.minAlpha, spec.maxAlpha),
                phase = random.nextFloat(),
            )
        }
    }
}

private fun DrawScope.drawFlake(
    effect: GlobalSnowEffect,
    center: Offset,
    radius: Float,
    alpha: Float,
    twinkle: Float,
    color: Color,
    accentColor: Color,
) {
    when (effect) {
        GlobalSnowEffect.Blizzard -> {
            drawLine(
                color = color.copy(alpha = alpha * 0.45f),
                start = center.copy(x = center.x - radius * 1.8f, y = center.y - radius * 5f),
                end = center.copy(x = center.x + radius * 1.3f, y = center.y + radius * 2.6f),
                strokeWidth = radius * 0.72f,
                cap = StrokeCap.Round,
            )
            drawCircle(color = color.copy(alpha = alpha), radius = radius, center = center)
        }

        GlobalSnowEffect.Starlight -> {
            val lineColor = accentColor.copy(alpha = alpha * twinkle)
            drawLine(lineColor, center.copy(x = center.x - radius * 1.8f), center.copy(x = center.x + radius * 1.8f), radius * 0.34f, StrokeCap.Round)
            drawLine(lineColor, center.copy(y = center.y - radius * 1.8f), center.copy(y = center.y + radius * 1.8f), radius * 0.34f, StrokeCap.Round)
            drawCircle(color = color.copy(alpha = alpha * twinkle), radius = radius * 0.56f, center = center)
        }

        GlobalSnowEffect.Pink -> {
            drawCircle(color = accentColor.copy(alpha = alpha * 0.36f), radius = radius * 1.65f, center = center)
            drawCircle(color = color.copy(alpha = alpha), radius = radius, center = center)
        }

        GlobalSnowEffect.Crystal -> {
            val crystal = Path().apply {
                moveTo(center.x, center.y - radius * 1.8f)
                lineTo(center.x + radius * 1.28f, center.y)
                lineTo(center.x, center.y + radius * 1.8f)
                lineTo(center.x - radius * 1.28f, center.y)
                close()
            }
            drawPath(crystal, color.copy(alpha = alpha * 0.72f))
            drawLine(accentColor.copy(alpha = alpha), center.copy(y = center.y - radius * 1.2f), center.copy(y = center.y + radius * 1.2f), radius * 0.28f, StrokeCap.Round)
            drawLine(accentColor.copy(alpha = alpha), center.copy(x = center.x - radius * 0.85f), center.copy(x = center.x + radius * 0.85f), radius * 0.24f, StrokeCap.Round)
        }

        GlobalSnowEffect.Gentle -> {
            drawCircle(color = accentColor.copy(alpha = alpha * 0.25f), radius = radius * 1.55f, center = center)
            drawCircle(color = color.copy(alpha = alpha), radius = radius, center = center)
        }
    }
}

private fun Random.nextRange(start: Float, end: Float): Float {
    return start + nextFloat() * (end - start)
}

private const val TWO_PI = (PI * 2.0).toFloat()
