package me.weishu.kernelsu.ui.component

import android.animation.ValueAnimator
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.snow.LocalSeasonStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
internal fun GlobalSeasonalRainEffect(
    modifier: Modifier = Modifier,
) {
    val season = LocalSeasonStyle.current
    val spec = remember(season) { SeasonalRainSpec.forSeason(season) }
    val drops = remember(season) { createSeasonalRainDrops(spec, season.rainSeed()) }
    val impacts = remember(season) { createSeasonalRainImpacts(spec, season.rainSeed() xor IMPACT_SEED) }
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val progress = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "seasonalRain")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = spec.cycleMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "seasonalRainProgress",
        )
        animatedProgress
    } else {
        STATIC_RAIN_PROGRESS
    }
    val density = LocalDensity.current
    val rippleStroke = remember(density.density) {
        Stroke(width = with(density) { 0.72.dp.toPx() })
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas

        drawSeasonalRainDrops(drops, spec, progress)
        drawSeasonalRainImpacts(impacts, spec, progress, rippleStroke)
    }
}

internal data class SeasonalRainSpec(
    val dropCount: Int,
    val impactCount: Int,
    val cycleMillis: Int,
    val minLengthDp: Float,
    val maxLengthDp: Float,
    val minStrokeDp: Float,
    val maxStrokeDp: Float,
    val minAlpha: Float,
    val maxAlpha: Float,
    val minFallCycles: Int,
    val maxFallCycles: Int,
    val slantRatio: Float,
    val horizontalTravel: Float,
    val sway: Float,
    val rippleRadiusDp: Float,
    val accentChance: Int,
    val primaryColor: Color,
    val accentColor: Color,
    val rippleColor: Color,
) {
    companion object {
        fun forSeason(season: SeasonStyle): SeasonalRainSpec = when (season) {
            SeasonStyle.Spring -> SeasonalRainSpec(
                dropCount = 76,
                impactCount = 9,
                cycleMillis = 15_000,
                minLengthDp = 8f,
                maxLengthDp = 18f,
                minStrokeDp = 0.42f,
                maxStrokeDp = 0.82f,
                minAlpha = 0.16f,
                maxAlpha = 0.44f,
                minFallCycles = 2,
                maxFallCycles = 3,
                slantRatio = 0.16f,
                horizontalTravel = 0.035f,
                sway = 0.006f,
                rippleRadiusDp = 15f,
                accentChance = 14,
                primaryColor = Color(0xFFE3F7F1),
                accentColor = Color(0xFFFFC2D7),
                rippleColor = Color(0xFFB8E4CF),
            )

            SeasonStyle.Summer -> SeasonalRainSpec(
                dropCount = 112,
                impactCount = 14,
                cycleMillis = 10_500,
                minLengthDp = 13f,
                maxLengthDp = 28f,
                minStrokeDp = 0.5f,
                maxStrokeDp = 1.04f,
                minAlpha = 0.16f,
                maxAlpha = 0.5f,
                minFallCycles = 3,
                maxFallCycles = 5,
                slantRatio = 0.12f,
                horizontalTravel = 0.045f,
                sway = 0.005f,
                rippleRadiusDp = 20f,
                accentChance = 18,
                primaryColor = Color(0xFFC9F3FF),
                accentColor = Color(0xFF72DDEB),
                rippleColor = Color(0xFF61C7D2),
            )

            SeasonStyle.Autumn -> SeasonalRainSpec(
                dropCount = 84,
                impactCount = 11,
                cycleMillis = 13_000,
                minLengthDp = 14f,
                maxLengthDp = 27f,
                minStrokeDp = 0.46f,
                maxStrokeDp = 0.92f,
                minAlpha = 0.15f,
                maxAlpha = 0.45f,
                minFallCycles = 2,
                maxFallCycles = 4,
                slantRatio = 0.42f,
                horizontalTravel = 0.15f,
                sway = 0.012f,
                rippleRadiusDp = 18f,
                accentChance = 22,
                primaryColor = Color(0xFFDAD6E8),
                accentColor = Color(0xFFE8BA72),
                rippleColor = Color(0xFFB99477),
            )

            SeasonStyle.Winter -> SeasonalRainSpec(
                dropCount = 56,
                impactCount = 8,
                cycleMillis = 17_500,
                minLengthDp = 9f,
                maxLengthDp = 21f,
                minStrokeDp = 0.4f,
                maxStrokeDp = 0.82f,
                minAlpha = 0.14f,
                maxAlpha = 0.38f,
                minFallCycles = 1,
                maxFallCycles = 3,
                slantRatio = 0.24f,
                horizontalTravel = 0.075f,
                sway = 0.004f,
                rippleRadiusDp = 13f,
                accentChance = 12,
                primaryColor = Color(0xFFDCEFFF),
                accentColor = Color(0xFFABC8EA),
                rippleColor = Color(0xFFB8D7EA),
            )
        }
    }
}

internal data class SeasonalRainDrop(
    val baseX: Float,
    val baseY: Float,
    val fallCycles: Int,
    val swayCycles: Int,
    val lengthDp: Float,
    val strokeDp: Float,
    val alpha: Float,
    val phase: Float,
    val depth: Float,
    val accent: Boolean,
)

internal data class SeasonalRainImpact(
    val x: Float,
    val y: Float,
    val phase: Float,
    val cycles: Int,
    val radiusScale: Float,
    val alpha: Float,
    val splashDirection: Float,
    val accent: Boolean,
)

internal fun createSeasonalRainDrops(
    spec: SeasonalRainSpec,
    seed: Int,
): List<SeasonalRainDrop> = List(spec.dropCount) { index ->
    val random = Random(seed + index * DROP_SEED_STEP)
    val depth = random.nextRainRange(0.22f, 1f)
    val depthScale = 0.64f + depth * 0.48f
    SeasonalRainDrop(
        baseX = random.nextFloat(),
        baseY = random.nextFloat(),
        fallCycles = random.nextInt(spec.minFallCycles, spec.maxFallCycles + 1),
        swayCycles = random.nextInt(1, 4),
        lengthDp = random.nextRainRange(spec.minLengthDp, spec.maxLengthDp) * depthScale,
        strokeDp = random.nextRainRange(spec.minStrokeDp, spec.maxStrokeDp) * depthScale,
        alpha = random.nextRainRange(spec.minAlpha, spec.maxAlpha) * (0.68f + depth * 0.32f),
        phase = random.nextFloat(),
        depth = depth,
        accent = random.nextInt(100) < spec.accentChance,
    )
}

internal fun createSeasonalRainImpacts(
    spec: SeasonalRainSpec,
    seed: Int,
): List<SeasonalRainImpact> = List(spec.impactCount) { index ->
    val random = Random(seed + index * IMPACT_SEED_STEP)
    SeasonalRainImpact(
        x = random.nextRainRange(0.04f, 0.96f),
        y = random.nextRainRange(0.68f, 0.96f),
        phase = random.nextFloat(),
        cycles = random.nextInt(2, 5),
        radiusScale = random.nextRainRange(0.58f, 1.08f),
        alpha = random.nextRainRange(0.18f, 0.42f),
        splashDirection = random.nextRainRange(-0.32f, 0.32f),
        accent = random.nextInt(100) < spec.accentChance,
    )
}

private fun DrawScope.drawSeasonalRainDrops(
    drops: List<SeasonalRainDrop>,
    spec: SeasonalRainSpec,
    progress: Float,
) {
    val verticalMargin = 44.dp.toPx()
    val horizontalMargin = 54.dp.toPx()
    val travelHeight = size.height + verticalMargin * 2f
    val edgeFade = 52.dp.toPx()
    val windPulse = sin(progress * TWO_PI) * spec.sway * size.width

    drops.forEach { drop ->
        val localProgress = (drop.baseY + progress * drop.fallCycles + drop.phase * 0.07f) % 1f
        val y = localProgress * travelHeight - verticalMargin
        val sway = sin((progress * drop.swayCycles + drop.phase) * TWO_PI) *
            spec.sway * size.width * (0.55f + drop.depth * 0.45f)
        val rawX = drop.baseX * size.width +
            localProgress * spec.horizontalTravel * size.height + sway + windPulse
        val x = wrapRainCoordinate(rawX, -horizontalMargin, size.width + horizontalMargin)
        val edgeAlpha = minOf(
            ((y + verticalMargin) / edgeFade).coerceIn(0f, 1f),
            ((size.height + verticalMargin - y) / edgeFade).coerceIn(0f, 1f),
        )
        if (edgeAlpha <= 0.01f) return@forEach

        val length = drop.lengthDp.dp.toPx()
        val slant = length * spec.slantRatio
        val head = Offset(x, y)
        val tail = Offset(x - slant, y - length)
        val color = if (drop.accent) spec.accentColor else spec.primaryColor
        val alpha = (drop.alpha * edgeAlpha).coerceAtMost(0.58f)
        val strokeWidth = drop.strokeDp.dp.toPx()

        if (drop.depth > 0.72f) {
            drawLine(
                color = color.copy(alpha = alpha * 0.11f),
                start = tail,
                end = head,
                strokeWidth = strokeWidth * 2.8f,
                cap = StrokeCap.Round,
            )
        }
        drawLine(
            color = color.copy(alpha = alpha),
            start = tail,
            end = head,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        if (drop.depth > 0.84f) {
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.48f),
                radius = strokeWidth * 0.58f,
                center = head,
            )
        }
    }
}

private fun DrawScope.drawSeasonalRainImpacts(
    impacts: List<SeasonalRainImpact>,
    spec: SeasonalRainSpec,
    progress: Float,
    rippleStroke: Stroke,
) {
    impacts.forEach { impact ->
        val localProgress = (progress * impact.cycles + impact.phase) % 1f
        if (localProgress > RIPPLE_VISIBLE_FRACTION) return@forEach

        val life = localProgress / RIPPLE_VISIBLE_FRACTION
        val expansion = 1f - (1f - life) * (1f - life)
        val fade = sin(life * PI.toFloat()).coerceAtLeast(0f) * (1f - life * 0.38f)
        val radius = spec.rippleRadiusDp.dp.toPx() * impact.radiusScale * (0.24f + expansion * 0.76f)
        val center = Offset(
            x = impact.x * size.width,
            y = impact.y * size.height,
        )
        val color = if (impact.accent) spec.accentColor else spec.rippleColor
        val alpha = (impact.alpha * fade).coerceIn(0f, 0.34f)

        drawOval(
            color = color.copy(alpha = alpha),
            topLeft = Offset(center.x - radius, center.y - radius * 0.25f),
            size = Size(radius * 2f, radius * 0.5f),
            style = rippleStroke,
        )
        if (impact.radiusScale > 0.86f) {
            val innerRadius = radius * 0.62f
            drawOval(
                color = spec.primaryColor.copy(alpha = alpha * 0.48f),
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius * 0.24f),
                size = Size(innerRadius * 2f, innerRadius * 0.48f),
                style = rippleStroke,
            )
        }

        if (life < SPLASH_VISIBLE_FRACTION) {
            val splashLife = life / SPLASH_VISIBLE_FRACTION
            val splashAlpha = alpha * (1f - splashLife)
            val splashHeight = 5.dp.toPx() * impact.radiusScale * (0.55f + splashLife * 0.45f)
            val spread = 3.2.dp.toPx() * impact.radiusScale
            drawLine(
                color = color.copy(alpha = splashAlpha),
                start = center,
                end = Offset(
                    x = center.x - spread + impact.splashDirection * spread,
                    y = center.y - splashHeight,
                ),
                strokeWidth = 0.72.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = spec.primaryColor.copy(alpha = splashAlpha * 0.82f),
                start = center,
                end = Offset(
                    x = center.x + spread + impact.splashDirection * spread,
                    y = center.y - splashHeight * 0.82f,
                ),
                strokeWidth = 0.66.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = Color.White.copy(alpha = splashAlpha * 0.58f),
                radius = 0.82.dp.toPx(),
                center = Offset(center.x, center.y - splashHeight * 0.72f),
            )
        }
    }
}

private fun SeasonStyle.rainSeed(): Int = RAIN_SEED + ordinal * SEASON_SEED_STEP

private fun wrapRainCoordinate(value: Float, min: Float, max: Float): Float {
    val range = max - min
    return ((value - min) % range + range) % range + min
}

private fun Random.nextRainRange(start: Float, end: Float): Float {
    return start + nextFloat() * (end - start)
}

private const val STATIC_RAIN_PROGRESS = 0.37f
private const val RIPPLE_VISIBLE_FRACTION = 0.72f
private const val SPLASH_VISIBLE_FRACTION = 0.2f
private const val TWO_PI = (PI * 2.0).toFloat()
private const val RAIN_SEED = 0x6A31
private const val IMPACT_SEED = 0x2D17
private const val DROP_SEED_STEP = 7_919
private const val IMPACT_SEED_STEP = 5_003
private const val SEASON_SEED_STEP = 10_007
