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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.isInDarkTheme
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
    Crystal("crystal", R.string.settings_global_snow_effect_crystal),
    Starfield("starfield", R.string.settings_global_snow_effect_starfield);

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
    if (!enabled || !isInDarkTheme()) return

    val effect = GlobalSnowEffect.fromValue(effectValue)
    val spec = remember(effect) { SnowSpec.forEffect(effect) }
    if (spec == null) {
        GlobalStarfieldEffect(modifier = modifier)
        return
    }
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
        if (size.width <= 1f || size.height <= 1f) return@Canvas

        val margin = 28.dp.toPx()
        val fadeDistance = 56.dp.toPx()
        val wrapWidth = size.width + margin * 2f
        val travelHeight = size.height + margin * 2f
        val wind = sin(progress * TWO_PI) * spec.gustStrength
        flakes.forEach { flake ->
            val yProgress = (flake.baseY + progress * flake.fallCycles) % 1f
            val localSway = sin((progress * flake.swayCycles + flake.phase) * TWO_PI) * flake.drift +
                sin((yProgress * 1.7f + flake.phase * 0.73f) * TWO_PI) * flake.drift * 0.28f
            val gustSway = sin((progress + flake.phase * 0.37f) * TWO_PI) *
                spec.gustStrength * flake.depth
            val rawX = (flake.baseX + localSway + gustSway + spec.windBias * progress + wind) *
                size.width
            val x = (((rawX + margin) % wrapWidth) + wrapWidth) % wrapWidth - margin
            val radius = flake.radiusDp.dp.toPx()
            val flutter = cos((progress * flake.swayCycles + flake.phase * 0.72f) * TWO_PI) *
                radius * (0.24f + flake.depth * 0.46f)
            val y = yProgress * travelHeight - margin + flutter
            val edgeAlpha = minOf(
                ((y + margin) / fadeDistance).coerceIn(0f, 1f),
                ((size.height + margin - y) / fadeDistance).coerceIn(0f, 1f),
            )
            if (edgeAlpha <= 0.01f) return@forEach
            drawFlake(
                effect = effect,
                center = Offset(x, y),
                radius = radius,
                alpha = flake.alpha * edgeAlpha,
                twinkle = flake.twinkle(progress, spec.shimmer),
                rotation = flake.rotation(progress),
                depth = flake.depth,
                shape = flake.shape,
                color = spec.color,
                accentColor = spec.accentColor,
                trailAlpha = spec.trailAlpha,
            )
        }
    }
}

@Composable
private fun GlobalStarfieldEffect(modifier: Modifier) {
    val stars = remember {
        List(STAR_COUNT) { index -> GlobalStar.create(index) }
    }
    val transition = rememberInfiniteTransition(label = "globalStarfield")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = STAR_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "globalStarfieldProgress",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        stars.forEach { star -> drawGlobalStar(star, progress) }
        drawShootingStar(progress)
    }
}

private data class GlobalStar(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val alpha: Float,
    val phase: Float,
    val twinkleCycles: Int,
    val driftDp: Float,
    val depth: Float,
    val tintIndex: Int,
    val sparkle: Boolean,
) {
    companion object {
        fun create(index: Int): GlobalStar {
            val random = Random(STARFIELD_SEED + index * 7919)
            val depth = random.nextRange(0.25f, 1f)
            return GlobalStar(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radiusDp = random.nextRange(0.48f, 1.65f) * (0.72f + depth * 0.38f),
                alpha = random.nextRange(0.24f, 0.74f) * (0.72f + depth * 0.28f),
                phase = random.nextFloat(),
                twinkleCycles = random.nextInt(1, 4),
                driftDp = random.nextRange(0.5f, 2.4f) * depth,
                depth = depth,
                tintIndex = random.nextInt(4),
                sparkle = random.nextInt(100) > 76,
            )
        }
    }
}

private fun DrawScope.drawGlobalStar(star: GlobalStar, progress: Float) {
    val wave = 0.5f + 0.5f * sin((progress * star.twinkleCycles + star.phase) * TWO_PI)
    val drift = star.driftDp.dp.toPx()
    val center = Offset(
        x = star.x * size.width + sin((progress + star.phase) * TWO_PI) * drift,
        y = star.y * size.height + cos((progress + star.phase * 0.73f) * TWO_PI) * drift * 0.42f,
    )
    val tint = when (star.tintIndex) {
        1 -> Color(0xFFCDEBFF)
        2 -> Color(0xFFFFEDC2)
        3 -> Color(0xFFD7FFF3)
        else -> Color.White
    }
    val radius = star.radiusDp.dp.toPx() * (0.86f + wave * 0.18f)
    val visibleAlpha = (star.alpha * (0.48f + wave * 0.52f)).coerceIn(0.08f, 0.78f)

    if (star.depth > 0.62f) {
        drawCircle(
            color = tint.copy(alpha = visibleAlpha * 0.10f),
            radius = radius * 3.2f,
            center = center,
        )
    }
    if (star.sparkle && wave > 0.58f) {
        val sparkleAlpha = visibleAlpha * ((wave - 0.58f) / 0.42f).coerceIn(0f, 1f)
        drawSpoke(
            center = center,
            angle = 0f,
            length = radius * 2.1f,
            color = tint.copy(alpha = sparkleAlpha * 0.52f),
            strokeWidth = 0.55.dp.toPx(),
        )
        drawSpoke(
            center = center,
            angle = PI_HALF,
            length = radius * 1.75f,
            color = tint.copy(alpha = sparkleAlpha * 0.44f),
            strokeWidth = 0.5.dp.toPx(),
        )
    }
    drawCircle(
        color = tint.copy(alpha = visibleAlpha),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = Color.White.copy(alpha = visibleAlpha * 0.72f),
        radius = radius * 0.34f,
        center = center,
    )
}

private fun DrawScope.drawShootingStar(progress: Float) {
    val phase = (progress + 0.35f) % 1f
    if (phase >= 0.13f) return

    val localProgress = phase / 0.13f
    val visibleAlpha = sin(localProgress * PI.toFloat()).coerceIn(0f, 1f) * 0.72f
    val head = Offset(
        x = size.width * (0.92f - localProgress * 0.56f),
        y = size.height * (0.13f + localProgress * 0.18f),
    )
    val tail = Offset(
        x = head.x + 48.dp.toPx(),
        y = head.y - 20.dp.toPx(),
    )
    drawLine(
        color = Color(0xFFBFE9FF).copy(alpha = visibleAlpha * 0.22f),
        start = tail,
        end = head,
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color.White.copy(alpha = visibleAlpha),
        start = tail,
        end = head,
        strokeWidth = 0.75.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = Color.White.copy(alpha = visibleAlpha),
        radius = 1.25.dp.toPx(),
        center = head,
    )
}

private data class SnowSpec(
    val count: Int,
    val cycleMillis: Int,
    val minRadius: Float,
    val maxRadius: Float,
    val minAlpha: Float,
    val maxAlpha: Float,
    val minFallCycles: Int,
    val maxFallCycles: Int,
    val maxDrift: Float,
    val windBias: Float,
    val gustStrength: Float,
    val trailAlpha: Float,
    val shimmer: Float,
    val color: Color,
    val accentColor: Color,
) {
    companion object {
        fun forEffect(effect: GlobalSnowEffect): SnowSpec? {
            return when (effect) {
                GlobalSnowEffect.Gentle -> SnowSpec(
                    count = 68,
                    cycleMillis = 26000,
                    minRadius = 0.55f,
                    maxRadius = 2.7f,
                    minAlpha = 0.16f,
                    maxAlpha = 0.72f,
                    minFallCycles = 1,
                    maxFallCycles = 2,
                    maxDrift = 0.048f,
                    windBias = 0.014f,
                    gustStrength = 0.016f,
                    trailAlpha = 0f,
                    shimmer = 0.12f,
                    color = Color.White,
                    accentColor = Color(0xFFEFF6FF),
                )

                GlobalSnowEffect.Blizzard -> SnowSpec(
                    count = 88,
                    cycleMillis = 16500,
                    minRadius = 0.42f,
                    maxRadius = 1.75f,
                    minAlpha = 0.12f,
                    maxAlpha = 0.52f,
                    minFallCycles = 2,
                    maxFallCycles = 4,
                    maxDrift = 0.075f,
                    windBias = 0.075f,
                    gustStrength = 0.038f,
                    trailAlpha = 0.56f,
                    shimmer = 0.08f,
                    color = Color.White,
                    accentColor = Color(0xFFDCEBFF),
                )

                GlobalSnowEffect.Starlight -> SnowSpec(
                    count = 50,
                    cycleMillis = 22000,
                    minRadius = 0.65f,
                    maxRadius = 2.35f,
                    minAlpha = 0.24f,
                    maxAlpha = 0.78f,
                    minFallCycles = 1,
                    maxFallCycles = 2,
                    maxDrift = 0.046f,
                    windBias = 0.012f,
                    gustStrength = 0.016f,
                    trailAlpha = 0f,
                    shimmer = 0.34f,
                    color = Color.White,
                    accentColor = Color(0xFFFFF7CC),
                )

                GlobalSnowEffect.Pink -> SnowSpec(
                    count = 58,
                    cycleMillis = 23000,
                    minRadius = 0.62f,
                    maxRadius = 2.5f,
                    minAlpha = 0.22f,
                    maxAlpha = 0.66f,
                    minFallCycles = 1,
                    maxFallCycles = 2,
                    maxDrift = 0.052f,
                    windBias = 0.016f,
                    gustStrength = 0.019f,
                    trailAlpha = 0f,
                    shimmer = 0.18f,
                    color = Color(0xFFFFF4FA),
                    accentColor = Color(0xFFFFB7D5),
                )

                GlobalSnowEffect.Crystal -> SnowSpec(
                    count = 34,
                    cycleMillis = 28000,
                    minRadius = 0.78f,
                    maxRadius = 3.1f,
                    minAlpha = 0.22f,
                    maxAlpha = 0.78f,
                    minFallCycles = 1,
                    maxFallCycles = 2,
                    maxDrift = 0.038f,
                    windBias = 0.012f,
                    gustStrength = 0.016f,
                    trailAlpha = 0f,
                    shimmer = 0.22f,
                    color = Color(0xFFF8FCFF),
                    accentColor = Color(0xFFBDEBFF),
                )

                GlobalSnowEffect.Starfield -> null
            }
        }
    }
}

private data class SnowFlake(
    val baseX: Float,
    val baseY: Float,
    val fallCycles: Int,
    val swayCycles: Int,
    val drift: Float,
    val radiusDp: Float,
    val alpha: Float,
    val phase: Float,
    val depth: Float,
    val rotation: Float,
    val spinCycles: Int,
    val shape: Int,
) {
    fun twinkle(progress: Float, shimmer: Float): Float {
        return 1f + sin((progress * swayCycles + phase) * TWO_PI) * shimmer
    }

    fun rotation(progress: Float): Float {
        return rotation + progress * spinCycles * TWO_PI
    }

    companion object {
        fun create(index: Int, spec: SnowSpec, effectHash: Int): SnowFlake {
            val random = Random(effectHash * 31 + index * 9973)
            val depth = random.nextRange(0.25f, 1f)
            val depthScale = 0.58f + depth * 0.56f
            return SnowFlake(
                baseX = random.nextFloat(),
                baseY = random.nextFloat(),
                fallCycles = random.nextInt(spec.minFallCycles, spec.maxFallCycles + 1),
                swayCycles = random.nextInt(1, 4),
                drift = random.nextRange(spec.maxDrift * 0.16f, spec.maxDrift) * (0.65f + depth * 0.45f),
                radiusDp = random.nextRange(spec.minRadius, spec.maxRadius) * depthScale,
                alpha = random.nextRange(spec.minAlpha, spec.maxAlpha) * (0.66f + depth * 0.34f),
                phase = random.nextFloat(),
                depth = depth,
                rotation = random.nextRange(-0.45f, 0.45f),
                spinCycles = random.nextInt(1, 3) * if (random.nextBoolean()) 1 else -1,
                shape = random.nextInt(100),
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
    rotation: Float,
    depth: Float,
    shape: Int,
    color: Color,
    accentColor: Color,
    trailAlpha: Float,
) {
    val visibleAlpha = (alpha * twinkle).coerceIn(0.08f, 0.86f)
    if (depth > 0.72f && effect != GlobalSnowEffect.Blizzard) {
        drawCircle(
            color = accentColor.copy(alpha = visibleAlpha * 0.07f),
            radius = radius * (2.2f + depth * 0.8f),
            center = center,
        )
    }
    when (effect) {
        GlobalSnowEffect.Blizzard -> {
            val trailLength = radius * (5.4f + depth * 5.2f)
            val trailAngle = rotation + 1.18f
            val start = center.polar(-trailLength * 0.52f, trailAngle)
            val end = center.polar(trailLength * 0.48f, trailAngle)
            drawLine(
                color = accentColor.copy(alpha = visibleAlpha * trailAlpha),
                start = start,
                end = end,
                strokeWidth = radius * (0.34f + depth * 0.24f),
                cap = StrokeCap.Round,
            )
            drawCircle(color = color.copy(alpha = visibleAlpha), radius = radius * 0.78f, center = center)
        }

        GlobalSnowEffect.Starlight -> {
            val lineColor = accentColor.copy(alpha = visibleAlpha)
            drawSpoke(center, rotation, radius * 2.05f, lineColor, radius * 0.3f)
            drawSpoke(center, rotation + PI_HALF, radius * 1.72f, lineColor, radius * 0.28f)
            if (shape > 58) {
                drawSpoke(center, rotation + PI_QUARTER, radius * 1.22f, lineColor.copy(alpha = visibleAlpha * 0.72f), radius * 0.18f)
                drawSpoke(center, rotation - PI_QUARTER, radius * 1.22f, lineColor.copy(alpha = visibleAlpha * 0.72f), radius * 0.18f)
            }
            drawCircle(color = color.copy(alpha = visibleAlpha * 0.86f), radius = radius * 0.46f, center = center)
        }

        GlobalSnowEffect.Pink -> {
            drawCircle(color = accentColor.copy(alpha = visibleAlpha * 0.22f), radius = radius * 1.8f, center = center)
            if (shape > 64) {
                drawCircle(color = accentColor.copy(alpha = visibleAlpha * 0.34f), radius = radius * 0.54f, center = center.polar(radius * 0.72f, rotation))
                drawCircle(color = accentColor.copy(alpha = visibleAlpha * 0.26f), radius = radius * 0.44f, center = center.polar(radius * 0.62f, rotation + PI_HALF))
            }
            drawCircle(color = color.copy(alpha = visibleAlpha), radius = radius * 0.92f, center = center)
        }

        GlobalSnowEffect.Crystal -> {
            drawCircle(
                color = accentColor.copy(alpha = visibleAlpha * 0.1f),
                radius = radius * 2.2f,
                center = center,
            )
            drawSnowCrystal(
                center = center,
                rotation = rotation,
                length = radius * 1.75f,
                color = color.copy(alpha = visibleAlpha * 0.82f),
                accentColor = accentColor.copy(alpha = visibleAlpha * 0.7f),
                strokeWidth = (radius * 0.22f).coerceAtLeast(0.65f),
                detailed = shape > 34,
            )
            drawCircle(color = Color.White.copy(alpha = visibleAlpha * 0.78f), radius = radius * 0.22f, center = center)
        }

        GlobalSnowEffect.Gentle -> {
            drawCircle(color = accentColor.copy(alpha = visibleAlpha * 0.18f), radius = radius * 1.72f, center = center)
            if (shape > 86) {
                drawSnowCrystal(
                    center = center,
                    rotation = rotation,
                    length = radius * 1.15f,
                    color = color.copy(alpha = visibleAlpha * 0.58f),
                    accentColor = accentColor.copy(alpha = visibleAlpha * 0.42f),
                    strokeWidth = (radius * 0.14f).coerceAtLeast(0.5f),
                    detailed = false,
                )
            }
            drawCircle(color = color.copy(alpha = visibleAlpha), radius = radius * 0.9f, center = center)
        }

        GlobalSnowEffect.Starfield -> Unit
    }
}

private fun DrawScope.drawSnowCrystal(
    center: Offset,
    rotation: Float,
    length: Float,
    color: Color,
    accentColor: Color,
    strokeWidth: Float,
    detailed: Boolean,
) {
    repeat(6) { arm ->
        val angle = rotation + arm * PI_THIRD
        drawLine(
            color = color,
            start = center,
            end = center.polar(length, angle),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        if (detailed) {
            val branch = center.polar(length * 0.58f, angle)
            val branchLength = length * 0.3f
            drawLine(
                color = accentColor,
                start = branch,
                end = branch.polar(branchLength, angle + PI_THIRD * 0.56f),
                strokeWidth = strokeWidth * 0.72f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = accentColor,
                start = branch,
                end = branch.polar(branchLength, angle - PI_THIRD * 0.56f),
                strokeWidth = strokeWidth * 0.72f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawSpoke(
    center: Offset,
    angle: Float,
    length: Float,
    color: Color,
    strokeWidth: Float,
) {
    drawLine(
        color = color,
        start = center.polar(-length, angle),
        end = center.polar(length, angle),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private fun Offset.polar(distance: Float, angle: Float): Offset {
    return Offset(
        x = x + cos(angle) * distance,
        y = y + sin(angle) * distance,
    )
}

private fun Random.nextRange(start: Float, end: Float): Float {
    return start + nextFloat() * (end - start)
}

private const val TWO_PI = (PI * 2.0).toFloat()
private const val PI_HALF = (PI / 2.0).toFloat()
private const val PI_QUARTER = (PI / 4.0).toFloat()
private const val PI_THIRD = (PI / 3.0).toFloat()
private const val STAR_COUNT = 72
private const val STAR_CYCLE_MILLIS = 24000
private const val STARFIELD_SEED = 0x4A17
