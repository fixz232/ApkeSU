package me.weishu.kernelsu.ui.component.rain

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.component.custom.CustomCardTarget
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.CardDefaults
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
@ReadOnlyComposable
fun isRainInterfaceStyle(): Boolean = LocalInterfaceStyle.current == InterfaceStyle.Rain.value

val LocalRainCardMotionEnabled = staticCompositionLocalOf { DEFAULT_RAIN_CARD_MOTION_ENABLED }

val LocalRainCardMotionProgress = staticCompositionLocalOf<State<Float>> {
    mutableFloatStateOf(STATIC_CARD_MOTION_PROGRESS)
}

val LocalRainSceneProgress = staticCompositionLocalOf<State<Float>> {
    mutableFloatStateOf(STATIC_PROGRESS)
}

@Composable
fun rememberRainCardMotionProgress(enabled: Boolean): State<Float> {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!enabled || !animationsEnabled) {
        return remember { mutableFloatStateOf(STATIC_CARD_MOTION_PROGRESS) }
    }
    val transition = rememberInfiniteTransition(label = "rainCardMotion")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(CARD_MOTION_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rainCardMotionProgress",
    )
}

@Composable
fun rememberRainSceneProgress(enabled: Boolean, style: RainStyle): State<Float> {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!enabled || !animationsEnabled) {
        return remember(style) {
            mutableFloatStateOf(if (style == RainStyle.AfterRain) 1f else STATIC_PROGRESS)
        }
    }
    if (style == RainStyle.AfterRain) {
        val clearing = remember(style) { Animatable(0f) }
        val clearingState = remember(clearing) { derivedStateOf { clearing.value } }
        LaunchedEffect(clearing) {
            clearing.snapTo(0f)
            clearing.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = AFTER_RAIN_CLEARING_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
        return clearingState
    }
    val cycleMillis = remember(style) { RainSceneSpec.forStyle(style).cycleMillis }
    val transition = rememberInfiniteTransition(label = "rainScene")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rainSceneProgress",
    )
}

@Composable
fun RainBackdrop(modifier: Modifier = Modifier) {
    if (!isRainInterfaceStyle()) return
    val style = LocalRainStyle.current
    val dark = isInDarkTheme()
    val palette = rainPalette(style, dark)
    val spec = remember(style) { RainSceneSpec.forStyle(style) }
    val drops = remember(style) { createRainDrops(spec, style.value.hashCode()) }
    val mist = remember(style) { createMistParticles(style.value.hashCode() xor MIST_SEED) }
    val progress = LocalRainSceneProgress.current

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        drawRainBackground(style, palette, mist, progress.value)
        val clearingAlpha = afterRainRainAlpha(style, progress.value)
        drawRainField(
            drops = drops,
            spec = spec,
            palette = palette,
            progress = progress.value,
            minimumDepth = 0f,
            maximumDepth = 0.50f,
            alphaMultiplier = 0.34f * clearingAlpha,
            lengthMultiplier = 0.78f,
            strokeMultiplier = 0.82f,
        )
    }
}

@Composable
fun RainForegroundOverlay(modifier: Modifier = Modifier) {
    if (!isRainInterfaceStyle()) return
    val style = LocalRainStyle.current
    val dark = isInDarkTheme()
    val palette = rainPalette(style, dark)
    val spec = remember(style) { RainSceneSpec.forStyle(style) }
    val drops = remember(style) { createRainDrops(spec, style.value.hashCode()) }
    val ripples = remember(style) { createRainRipples(spec, style.value.hashCode() xor RIPPLE_SEED) }
    val lensDrops = remember(style) { createLensDrops(style.value.hashCode() xor LENS_DROP_SEED) }
    val progress = LocalRainSceneProgress.current
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    var lightningAlpha by remember(style, dark) { mutableFloatStateOf(0f) }

    LaunchedEffect(style, dark, animationsEnabled) {
        lightningAlpha = 0f
        if (!isRainLightningEnabled(style, dark, animationsEnabled)) return@LaunchedEffect
        val random = Random(LIGHTNING_SEED)
        while (true) {
            delay(nextLightningDelayMillis(random))
            lightningAlpha = if (random.nextBoolean()) 0.30f else 0.24f
            delay(nextLightningFlashDurationMillis(random))
            lightningAlpha = 0f
            if (random.nextInt(100) < 32) {
                delay(random.nextLong(90L, 181L))
                lightningAlpha = 0.15f
                delay(nextLightningFlashDurationMillis(random))
                lightningAlpha = 0f
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 1f || size.height <= 1f) return@Canvas
            val clearingAlpha = afterRainRainAlpha(style, progress.value)
            drawRainField(
                drops = drops,
                spec = spec,
                palette = palette,
                progress = progress.value,
                minimumDepth = 0.56f,
                maximumDepth = 1f,
                alphaMultiplier = foregroundRainAlpha(style) * clearingAlpha,
                lengthMultiplier = 1.12f,
                strokeMultiplier = 1f,
            )
            drawRainRipples(
                ripples = ripples,
                spec = spec,
                palette = palette,
                progress = progress.value,
                alphaMultiplier = 0.54f * clearingAlpha,
            )
            drawLensDrops(
                drops = lensDrops,
                palette = palette,
                progress = progress.value,
                alphaMultiplier = if (style == RainStyle.AfterRain) 0.74f else 1f,
            )
        }
        if (lightningAlpha > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLightningIllumination(palette, lightningAlpha)
                drawLightningBranch(lightningAlpha)
            }
        }
    }
}

@Composable
fun RainChromeOverlay(modifier: Modifier = Modifier) {
    if (!isRainInterfaceStyle()) return
    val style = LocalRainStyle.current
    val palette = rainPalette(style, isInDarkTheme())
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val progress = LocalRainCardMotionProgress.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val inset = 16.dp.toPx()
        val top = topPadding.toPx() + 5.dp.toPx()
        val lineY = top + 2.dp.toPx()
        drawLine(
            color = palette.highlight.copy(alpha = 0.34f),
            start = Offset(inset, lineY),
            end = Offset(size.width - inset, lineY),
            strokeWidth = 0.75.dp.toPx(),
            cap = StrokeCap.Round,
        )
        val travel = (size.width - inset * 2f).coerceAtLeast(1f)
        val x = inset + travel * (0.08f + progress.value * 0.84f)
        val radius = 1.15.dp.toPx()
        drawLine(
            color = palette.rain.copy(alpha = 0.24f),
            start = Offset(x, lineY),
            end = Offset(x - 0.35.dp.toPx(), lineY + radius * 1.7f),
            strokeWidth = 0.55.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = palette.rain.copy(alpha = 0.52f),
            radius = radius,
            center = Offset(x - 0.35.dp.toPx(), lineY + radius * 2.2f),
        )
        drawCircle(
            color = palette.highlight.copy(alpha = 0.72f),
            radius = radius * 0.25f,
            center = Offset(x - radius * 0.60f, lineY + radius * 1.72f),
        )
    }
}

@Composable
fun RainMotto(modifier: Modifier = Modifier) {
    if (!isRainInterfaceStyle()) return
    val style = LocalRainStyle.current
    val palette = rainPalette(style, isInDarkTheme())
    val motionProgress = LocalRainCardMotionProgress.current
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(22.dp)
            .shadow(1.dp, shape, ambientColor = palette.shadow.copy(alpha = 0.14f))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.surfaceTop.copy(alpha = 0.72f),
                        palette.surfaceBottom.copy(alpha = 0.56f),
                    ),
                ),
                shape,
            )
            .border(0.75.dp, palette.outline.copy(alpha = 0.48f), shape)
            .drawWithContent {
                drawRainMottoDetails(palette, motionProgress.value)
                drawContent()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(style.mottoRes),
            modifier = Modifier.padding(horizontal = 28.dp),
            color = palette.content,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun Modifier.rainMiuixCardSurface(
    enabled: Boolean = true,
    capHeight: Dp = 11.dp,
    customTarget: CustomCardTarget = CustomCardTarget.Default,
): Modifier {
    if (!enabled || !isRainInterfaceStyle()) return this
    val style = LocalRainStyle.current
    val dark = isInDarkTheme()
    val palette = rainPalette(style, dark)
    val motionEnabled = LocalRainCardMotionEnabled.current
    val motionProgress = LocalRainCardMotionProgress.current
    val shape = RoundedCornerShape(14.dp)
    val surfaceBrush = Brush.verticalGradient(
        listOf(
            palette.surfaceTop,
            palette.surfaceBottom,
            palette.surfaceBottom.copy(alpha = palette.surfaceBottom.alpha * 0.92f),
        ),
    )
    return this
        .shadow(2.dp, shape, ambientColor = palette.shadow.copy(alpha = 0.22f))
        .clip(shape)
        .background(surfaceBrush, shape)
        .border(0.85.dp, palette.outline.copy(alpha = if (dark) 0.54f else 0.66f), shape)
        .drawWithContent {
            val progress = motionProgress.value
            drawRainCardInterior(style, palette, progress, motionEnabled)
            drawContent()
            drawRainCardFrame(palette, progress, motionEnabled)
            val height = rainCardDecorationHeight(
                requestedHeight = capHeight.toPx(),
                width = size.width,
                height = size.height,
            )
            if (height > 0f) drawRainCardCanopy(style, palette, height, progress, motionEnabled)
        }
        .uiDecoratedCard(shape = shape, enabled = true, customTarget = customTarget)
}

@Composable
fun rainMiuixCardColors(
    color: Color,
) = if (isRainInterfaceStyle()) {
    CardDefaults.defaultColors(color = rainCardContentLayerColor(color))
} else {
    CardDefaults.defaultColors(color = color)
}

internal fun rainCardContentLayerColor(baseColor: Color): Color =
    baseColor.copy(alpha = baseColor.alpha * 0.14f)

internal fun rainCardDecorationHeight(
    requestedHeight: Float,
    width: Float,
    height: Float,
): Float {
    if (requestedHeight <= 0f || width < 72f || height < 48f) return 0f
    return minOf(requestedHeight, height * 0.19f)
}

@Composable
fun rainNavigationContainerColor(): Color {
    val palette = rainPalette(LocalRainStyle.current, isInDarkTheme())
    return palette.surfaceBottom.copy(alpha = if (isInDarkTheme()) 0.66f else 0.58f)
}

@Composable
fun Modifier.rainNavigationSurface(
    shape: Shape,
    paintBackground: Boolean = true,
): Modifier {
    if (!isRainInterfaceStyle()) return this
    val style = LocalRainStyle.current
    val palette = rainPalette(style, isInDarkTheme())
    val motionEnabled = LocalRainCardMotionEnabled.current
    val motionProgress = LocalRainCardMotionProgress.current
    return clip(shape)
        .then(if (paintBackground) Modifier.background(rainNavigationContainerColor(), shape) else Modifier)
        .border(0.8.dp, palette.outline.copy(alpha = 0.58f), shape)
        .drawWithContent {
            val progress = motionProgress.value
            drawRainNavigationUnderlay(style, palette, progress, motionEnabled)
            drawContent()
            drawRainNavigationFrame(palette)
        }
}

@Composable
fun Modifier.rainNavigationIndicator(
    shape: Shape,
    paintBackground: Boolean = true,
    interactionKey: Any? = Unit,
): Modifier {
    if (!isRainInterfaceStyle()) return this
    val palette = rainPalette(LocalRainStyle.current, isInDarkTheme())
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val rippleProgress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(interactionKey, animationsEnabled) {
        if (!animationsEnabled) {
            rippleProgress.snapTo(1f)
        } else {
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(420, easing = FastOutSlowInEasing),
            )
        }
    }
    return clip(shape)
        .then(
            if (paintBackground) {
                Modifier.background(palette.ripple.copy(alpha = 0.14f), shape)
            } else {
                Modifier
            },
        )
        .border(0.8.dp, palette.rainAccent.copy(alpha = 0.52f), shape)
        .drawWithContent {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        palette.highlight.copy(alpha = 0.16f),
                        palette.ripple.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                ),
            )
            drawContent()
            val progress = rippleProgress.value
            if (progress < 1f) {
                val radius = size.maxDimension * (0.22f + progress * 0.84f)
                drawOval(
                    color = palette.rainAccent.copy(alpha = (1f - progress) * 0.34f),
                    topLeft = Offset(size.width / 2f - radius, size.height / 2f - radius * 0.42f),
                    size = Size(radius * 2f, radius * 0.84f),
                    style = Stroke(width = 0.9.dp.toPx()),
                )
            }
        }
}

@Composable
fun rainTopBarContainerColor(): Color {
    val palette = rainPalette(LocalRainStyle.current, isInDarkTheme())
    return palette.surfaceBottom.copy(alpha = 0.58f)
}

@Composable
fun rainTopBarContentColor(): Color {
    return rainPalette(LocalRainStyle.current, isInDarkTheme()).content
}

private data class RainDrop(
    val x: Float,
    val y: Float,
    val phase: Float,
    val speed: Int,
    val lengthDp: Float,
    val strokeDp: Float,
    val alpha: Float,
    val depth: Float,
)

private data class RainRipple(
    val x: Float,
    val y: Float,
    val phase: Float,
    val cycles: Int,
    val radiusDp: Float,
    val alpha: Float,
)

private data class MistParticle(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val alpha: Float,
)

private data class LensDrop(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val phase: Float,
)

private fun createRainDrops(spec: RainSceneSpec, seed: Int): List<RainDrop> {
    return List(spec.dropCount) { index ->
        val random = Random(seed + index * 7_919)
        val depth = random.nextFloat().coerceIn(0.18f, 1f)
        RainDrop(
            x = random.nextFloat(),
            y = random.nextFloat(),
            phase = random.nextFloat(),
            speed = random.nextInt(2, 6),
            lengthDp = random.nextFloatRange(spec.minLengthDp, spec.maxLengthDp) * (0.72f + depth * 0.36f),
            strokeDp = random.nextFloatRange(spec.minStrokeDp, spec.maxStrokeDp) * (0.78f + depth * 0.28f),
            alpha = random.nextFloatRange(spec.minAlpha, spec.maxAlpha),
            depth = depth,
        )
    }
}

private fun createRainRipples(spec: RainSceneSpec, seed: Int): List<RainRipple> {
    return List(spec.rippleCount) { index ->
        val random = Random(seed + index * 3_571)
        RainRipple(
            x = random.nextFloatRange(0.04f, 0.96f),
            y = random.nextFloatRange(0.76f, 0.97f),
            phase = random.nextFloat(),
            cycles = random.nextInt(2, 5),
            radiusDp = random.nextFloatRange(10f, 24f),
            alpha = random.nextFloatRange(0.16f, 0.42f),
        )
    }
}

private fun createMistParticles(seed: Int): List<MistParticle> {
    return List(MIST_PARTICLE_COUNT) { index ->
        val random = Random(seed + index * 1_301)
        MistParticle(
            x = random.nextFloat(),
            y = random.nextFloat(),
            radiusDp = random.nextFloatRange(0.25f, 0.85f),
            alpha = random.nextFloatRange(0.018f, 0.07f),
        )
    }
}

private fun createLensDrops(seed: Int): List<LensDrop> {
    return List(LENS_DROP_COUNT) { index ->
        val random = Random(seed + index * 2_003)
        val onLeft = index % 2 == 0
        LensDrop(
            x = if (onLeft) random.nextFloatRange(0.025f, 0.075f) else random.nextFloatRange(0.925f, 0.975f),
            y = random.nextFloatRange(0.13f, 0.82f),
            radiusDp = random.nextFloatRange(1.6f, 3.2f),
            phase = random.nextFloat(),
        )
    }
}

private fun DrawScope.drawRainBackground(
    style: RainStyle,
    palette: RainPalette,
    mist: List<MistParticle>,
    progress: Float,
) {
    val clearing = if (style == RainStyle.AfterRain) progress.coerceIn(0f, 1f) else 0f
    drawRect(brush = Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                palette.fog.copy(alpha = 0.22f),
                Color.Transparent,
                palette.fog.copy(alpha = 0.12f),
            ),
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                palette.highlight.copy(
                    alpha = when (style) {
                        RainStyle.LightRain -> 0.14f
                        RainStyle.AfterRain -> 0.12f + clearing * 0.16f
                        else -> 0.08f
                    },
                ),
                palette.fog.copy(alpha = 0.055f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.27f, size.height * (0.70f - clearing * 0.42f)),
            radius = size.maxDimension * (0.62f + clearing * 0.12f),
        ),
    )

    val cloudAlpha = when (style) {
        RainStyle.LightRain -> 0.18f
        RainStyle.MediumRain -> 0.23f
        RainStyle.HeavyRain -> 0.28f
        RainStyle.Thunderstorm -> 0.32f
        RainStyle.AfterRain -> 0.22f * (1f - clearing * 0.72f)
    }
    val cloudLift = size.height * clearing * 0.055f
    drawRainCloudLayer(
        baseline = size.height * 0.12f - cloudLift,
        depth = size.height * 0.075f,
        color = palette.cloud,
        alpha = cloudAlpha,
        phase = 0.2f + progress * PI.toFloat() * 0.20f,
    )
    drawRainCloudLayer(
        baseline = size.height * 0.21f - cloudLift * 0.72f,
        depth = size.height * 0.09f,
        color = palette.fog,
        alpha = cloudAlpha * 0.58f,
        phase = 1.1f - progress * PI.toFloat() * 0.13f,
    )
    drawRainCloudLayer(
        baseline = size.height * 0.29f - cloudLift * 0.48f,
        depth = size.height * 0.065f,
        color = palette.cloud,
        alpha = cloudAlpha * 0.32f,
        phase = 2.2f + progress * PI.toFloat() * 0.08f,
    )
    mist.forEachIndexed { index, particle ->
        val drift = sin(progress * PI.toFloat() * 2f + index * 0.73f) * size.width * 0.012f
        drawCircle(
            color = palette.highlight.copy(alpha = particle.alpha * (1f - clearing * 0.46f)),
            radius = particle.radiusDp.dp.toPx(),
            center = Offset(particle.x * size.width + drift, particle.y * size.height),
        )
    }
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.52f to Color.Transparent,
                0.76f to palette.fog.copy(alpha = 0.11f),
                1f to palette.ripple.copy(alpha = 0.16f),
            ),
        ),
    )
    val horizon = Path().apply {
        moveTo(0f, size.height * 0.89f)
        cubicTo(
            size.width * 0.22f,
            size.height * 0.87f,
            size.width * 0.34f,
            size.height * 0.92f,
            size.width * 0.55f,
            size.height * 0.895f,
        )
        cubicTo(
            size.width * 0.72f,
            size.height * 0.88f,
            size.width * 0.84f,
            size.height * 0.92f,
            size.width,
            size.height * 0.90f,
        )
    }
    drawPath(
        path = horizon,
        color = palette.highlight.copy(alpha = 0.10f),
        style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round),
    )
    if (style == RainStyle.AfterRain) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.72f to Color.Transparent,
                    0.90f to palette.highlight.copy(alpha = 0.035f + clearing * 0.065f),
                    1f to palette.ripple.copy(alpha = 0.12f + clearing * 0.09f),
                ),
            ),
        )
        repeat(3) { index ->
            val y = size.height * (0.88f + index * 0.034f)
            val halfWidth = size.width * (0.10f + index * 0.07f)
            drawOval(
                color = palette.highlight.copy(alpha = clearing * (0.075f - index * 0.014f)),
                topLeft = Offset(size.width * 0.34f - halfWidth, y - 1.5.dp.toPx()),
                size = Size(halfWidth * 2f, 3.dp.toPx()),
            )
        }
    }
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                palette.shadow.copy(alpha = 0.20f - clearing * 0.11f),
            ),
            center = Offset(size.width * 0.5f, size.height * 0.44f),
            radius = size.maxDimension * 0.78f,
        ),
    )
}

private fun DrawScope.drawRainCloudLayer(
    baseline: Float,
    depth: Float,
    color: Color,
    alpha: Float,
    phase: Float,
) {
    val left = -size.width * 0.08f
    val right = size.width * 1.08f
    val wave = { value: Float -> sin(value + phase) * depth * 0.18f }
    val path = Path().apply {
        moveTo(left, baseline + wave(0f))
        cubicTo(
            size.width * 0.08f,
            baseline - depth * 0.48f + wave(0.7f),
            size.width * 0.18f,
            baseline + depth * 0.26f + wave(1.4f),
            size.width * 0.31f,
            baseline - depth * 0.08f + wave(2.1f),
        )
        cubicTo(
            size.width * 0.45f,
            baseline - depth * 0.42f + wave(2.8f),
            size.width * 0.58f,
            baseline + depth * 0.30f + wave(3.5f),
            size.width * 0.70f,
            baseline - depth * 0.04f + wave(4.2f),
        )
        cubicTo(
            size.width * 0.82f,
            baseline - depth * 0.38f + wave(4.9f),
            size.width * 0.94f,
            baseline + depth * 0.20f + wave(5.6f),
            right,
            baseline - depth * 0.10f + wave(6.3f),
        )
        lineTo(right, 0f)
        lineTo(left, 0f)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = alpha * 0.82f),
                color.copy(alpha = alpha),
                color.copy(alpha = 0f),
            ),
            endY = baseline + depth,
        ),
    )
}

private fun DrawScope.drawRainField(
    drops: List<RainDrop>,
    spec: RainSceneSpec,
    palette: RainPalette,
    progress: Float,
    minimumDepth: Float,
    maximumDepth: Float,
    alphaMultiplier: Float,
    lengthMultiplier: Float,
    strokeMultiplier: Float,
) {
    val margin = 42.dp.toPx()
    val travelHeight = size.height + margin * 2f
    val wrapWidth = size.width + margin * 2f
    drops.forEach { drop ->
        if (drop.depth < minimumDepth || drop.depth > maximumDepth) return@forEach
        val local = (drop.y + progress * drop.speed + drop.phase * 0.08f) % 1f
        val y = local * travelHeight - margin
        val rawX = drop.x * size.width + local * size.height * spec.windRatio
        val x = (((rawX + margin) % wrapWidth) + wrapWidth) % wrapWidth - margin
        val length = drop.lengthDp.dp.toPx() * lengthMultiplier
        val slant = length * spec.windRatio
        val color = if (drop.depth > 0.82f) palette.rainAccent else palette.rain
        val edgeFade = minOf(
            ((y + margin) / (margin * 1.4f)).coerceIn(0f, 1f),
            ((size.height + margin - y) / (margin * 1.4f)).coerceIn(0f, 1f),
        )
        if (edgeFade <= 0.01f) return@forEach
        val alpha = drop.alpha * edgeFade * (0.68f + drop.depth * 0.32f) * alphaMultiplier
        val strokeWidth = drop.strokeDp.dp.toPx() * strokeMultiplier
        if (drop.depth > 0.72f) {
            drawLine(
                color = color.copy(alpha = alpha * 0.09f),
                start = Offset(x - slant, y - length),
                end = Offset(x, y),
                strokeWidth = strokeWidth * 3f,
                cap = StrokeCap.Round,
            )
        }
        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(x - slant, y - length),
            end = Offset(x, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawRainRipples(
    ripples: List<RainRipple>,
    spec: RainSceneSpec,
    palette: RainPalette,
    progress: Float,
    alphaMultiplier: Float,
) {
    val stroke = Stroke(width = 0.72.dp.toPx())
    ripples.forEach { ripple ->
        val local = (progress * ripple.cycles + ripple.phase) % 1f
        if (local > RIPPLE_VISIBLE_FRACTION) return@forEach
        val life = local / RIPPLE_VISIBLE_FRACTION
        val radius = ripple.radiusDp.dp.toPx() * (0.24f + life * 0.76f)
        val alpha = ripple.alpha * sin(life * PI.toFloat()).coerceAtLeast(0f) *
            (1f - life * 0.36f) * alphaMultiplier
        val center = Offset(ripple.x * size.width, ripple.y * size.height)
        drawOval(
            color = palette.ripple.copy(alpha = alpha),
            topLeft = Offset(center.x - radius, center.y - radius * 0.30f),
            size = Size(radius * 2f, radius * 0.60f),
            style = stroke,
        )
        if (spec.rippleCount >= 10 && life < 0.55f) {
            val innerRadius = radius * 0.55f
            drawOval(
                color = palette.highlight.copy(alpha = alpha * 0.38f),
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius * 0.28f),
                size = Size(innerRadius * 2f, innerRadius * 0.56f),
                style = stroke,
            )
        }
    }
}

private fun DrawScope.drawLightningBranch(alpha: Float) {
    val main = Path().apply {
        moveTo(size.width * 0.74f, 0f)
        lineTo(size.width * 0.69f, size.height * 0.075f)
        lineTo(size.width * 0.72f, size.height * 0.105f)
        lineTo(size.width * 0.65f, size.height * 0.18f)
        lineTo(size.width * 0.68f, size.height * 0.205f)
        lineTo(size.width * 0.60f, size.height * 0.31f)
    }
    val branch = Path().apply {
        moveTo(size.width * 0.66f, size.height * 0.17f)
        lineTo(size.width * 0.60f, size.height * 0.20f)
        lineTo(size.width * 0.56f, size.height * 0.26f)
    }
    drawPath(
        path = main,
        color = Color.White.copy(alpha = (alpha * 0.38f).coerceAtMost(0.18f)),
        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
        path = main,
        color = Color.White.copy(alpha = (alpha * 1.55f).coerceAtMost(0.72f)),
        style = Stroke(width = 1.15.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
        path = branch,
        color = Color.White.copy(alpha = (alpha * 1.12f).coerceAtMost(0.52f)),
        style = Stroke(width = 0.85.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawLightningIllumination(
    palette: RainPalette,
    alpha: Float,
) {
    drawRect(palette.highlight.copy(alpha = alpha * 0.11f))
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                palette.rainAccent.copy(alpha = alpha * 0.22f),
                palette.highlight.copy(alpha = alpha * 0.07f),
                Color.Transparent,
            ),
            endY = size.height * 0.46f,
        ),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                palette.highlight.copy(alpha = alpha * 0.72f),
                palette.rainAccent.copy(alpha = alpha * 0.24f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.70f, size.height * 0.10f),
            radius = size.maxDimension * 0.34f,
        ),
        radius = size.maxDimension * 0.34f,
        center = Offset(size.width * 0.70f, size.height * 0.10f),
    )
}

private fun DrawScope.drawLensDrops(
    drops: List<LensDrop>,
    palette: RainPalette,
    progress: Float,
    alphaMultiplier: Float,
) {
    drops.forEachIndexed { index, drop ->
        val radius = drop.radiusDp.dp.toPx()
        val slide = ((progress * (0.05f + index * 0.004f) + drop.phase) % 1f) * 8.dp.toPx()
        val center = Offset(
            x = drop.x * size.width + sin(progress * PI.toFloat() * 2f + index) * 0.7.dp.toPx(),
            y = drop.y * size.height + slide,
        )
        val path = Path().apply {
            moveTo(center.x, center.y - radius * 1.35f)
            cubicTo(
                center.x + radius * 0.92f,
                center.y - radius * 0.25f,
                center.x + radius * 0.80f,
                center.y + radius,
                center.x,
                center.y + radius * 1.08f,
            )
            cubicTo(
                center.x - radius * 0.80f,
                center.y + radius,
                center.x - radius * 0.92f,
                center.y - radius * 0.25f,
                center.x,
                center.y - radius * 1.35f,
            )
            close()
        }
        drawPath(path, color = palette.rain.copy(alpha = 0.11f * alphaMultiplier))
        drawPath(
            path = path,
            color = palette.highlight.copy(alpha = 0.19f * alphaMultiplier),
            style = Stroke(width = 0.55.dp.toPx()),
        )
        drawCircle(
            color = palette.highlight.copy(alpha = 0.31f * alphaMultiplier),
            radius = radius * 0.17f,
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.47f),
        )
    }
}

private fun DrawScope.drawRainCardInterior(
    style: RainStyle,
    palette: RainPalette,
    progress: Float,
    motionEnabled: Boolean,
) {
    if (size.width < 72.dp.toPx() || size.height < 48.dp.toPx()) return
    drawRect(
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to palette.highlight.copy(alpha = 0.075f),
                0.34f to Color.Transparent,
                0.80f to palette.ripple.copy(alpha = 0.038f),
                1f to palette.shadow.copy(alpha = 0.045f),
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
    )
    val sweepWidth = minOf(size.width * 0.11f, 44.dp.toPx())
    val sweepCenter = if (motionEnabled) {
        -sweepWidth + progress * (size.width + sweepWidth * 2f)
    } else {
        size.width * 0.30f
    }
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                palette.highlight.copy(alpha = 0.045f),
                Color.Transparent,
            ),
            start = Offset(sweepCenter - sweepWidth, 0f),
            end = Offset(sweepCenter + sweepWidth, size.height),
        ),
    )
    val dropCount = when (style) {
        RainStyle.LightRain -> 1
        RainStyle.MediumRain -> 2
        RainStyle.HeavyRain -> 2
        RainStyle.Thunderstorm -> 2
        RainStyle.AfterRain -> 1
    }
    repeat(dropCount) { index ->
        val x = size.width * (0.89f + index * 0.036f)
        val laneStart = size.height * (0.24f + (index % 3) * 0.14f)
        val laneProgress = if (motionEnabled) {
            (progress * (0.54f + index * 0.08f) + index * 0.23f) % 1f
        } else {
            0.20f + (index % 3) * 0.19f
        }
        val y = laneStart + size.height * 0.22f * laneProgress
        val radius = (1.05f + (index % 2) * 0.52f).dp.toPx()
        val streak = (4.5f + index * 1.4f).dp.toPx()
        drawLine(
            color = palette.rain.copy(alpha = 0.075f),
            start = Offset(x, y - streak),
            end = Offset(x, y - radius * 0.8f),
            strokeWidth = 0.62.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawOval(
            color = palette.rain.copy(alpha = 0.12f),
            topLeft = Offset(x - radius * 0.70f, y - radius),
            size = Size(radius * 1.32f, radius * 1.85f),
        )
        drawCircle(
            color = palette.highlight.copy(alpha = 0.31f),
            radius = radius * 0.20f,
            center = Offset(x - radius * 0.26f, y - radius * 0.48f),
        )
    }
    val reflectionAlpha = if (style == RainStyle.AfterRain) 0.16f else 0.08f
    drawOval(
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                palette.highlight.copy(alpha = reflectionAlpha),
                palette.ripple.copy(alpha = reflectionAlpha * 0.62f),
                Color.Transparent,
            ),
        ),
        topLeft = Offset(size.width * 0.12f, size.height - 5.dp.toPx()),
        size = Size(size.width * 0.62f, 3.5.dp.toPx()),
    )
}

private fun DrawScope.drawRainCardCanopy(
    style: RainStyle,
    palette: RainPalette,
    height: Float,
    progress: Float,
    motionEnabled: Boolean,
) {
    val phase = if (motionEnabled) progress * PI.toFloat() * 2f else PI.toFloat() * 0.62f
    val breathing = sin(phase) * height * 0.018f
    val y0 = height * 0.22f + breathing
    val y1 = height * 0.31f - breathing * 0.45f
    val y2 = height * 0.22f + breathing * 0.35f
    val y3 = height * 0.29f - breathing * 0.25f
    val y4 = height * 0.24f + breathing * 0.40f
    val waterline = Path().apply {
        moveTo(0f, y0)
        cubicTo(size.width * 0.08f, height * 0.18f, size.width * 0.15f, height * 0.36f, size.width * 0.24f, y1)
        cubicTo(size.width * 0.33f, height * 0.24f, size.width * 0.41f, height * 0.17f, size.width * 0.49f, y2)
        cubicTo(size.width * 0.58f, height * 0.31f, size.width * 0.66f, height * 0.34f, size.width * 0.74f, y3)
        cubicTo(size.width * 0.83f, height * 0.15f, size.width * 0.92f, height * 0.29f, size.width, y4)
    }
    val fill = Path().apply {
        moveTo(0f, y0)
        cubicTo(size.width * 0.08f, height * 0.18f, size.width * 0.15f, height * 0.36f, size.width * 0.24f, y1)
        cubicTo(size.width * 0.33f, height * 0.24f, size.width * 0.41f, height * 0.17f, size.width * 0.49f, y2)
        cubicTo(size.width * 0.58f, height * 0.31f, size.width * 0.66f, height * 0.34f, size.width * 0.74f, y3)
        cubicTo(size.width * 0.83f, height * 0.15f, size.width * 0.92f, height * 0.29f, size.width, y4)
        lineTo(size.width, 0f)
        lineTo(0f, 0f)
        close()
    }
    val waterAlpha = if (style == RainStyle.AfterRain) 0.76f else 1f
    drawPath(
        path = fill,
        brush = Brush.verticalGradient(
            colors = listOf(
                palette.highlight.copy(alpha = 0.23f * waterAlpha),
                palette.rainAccent.copy(alpha = 0.11f * waterAlpha),
                Color.Transparent,
            ),
            endY = height * 0.82f,
        ),
    )
    drawPath(
        path = waterline,
        color = palette.highlight.copy(alpha = 0.34f * waterAlpha),
        style = Stroke(width = 0.68.dp.toPx(), cap = StrokeCap.Round),
    )
    val dripCount = when (style) {
        RainStyle.LightRain -> 1
        RainStyle.MediumRain -> 2
        RainStyle.HeavyRain -> 2
        RainStyle.Thunderstorm -> 3
        RainStyle.AfterRain -> 1
    }
    val dripFractions = floatArrayOf(0.24f, 0.66f, 0.84f)
    val dripWaterlines = floatArrayOf(y1, y3, height * 0.21f)
    repeat(dripCount) { index ->
        val x = size.width * dripFractions[index]
        val radius = height * if (index % 2 == 0) 0.105f else 0.082f
        val beadProgress = if (motionEnabled) {
            (progress * (0.42f + index * 0.047f) + index * 0.27f) % 1f
        } else {
            0.18f + (index % 3) * 0.22f
        }
        val waterY = dripWaterlines[index]
        val dripLength = height * (0.12f + beadProgress * 0.26f)
        drawLine(
            color = palette.rain.copy(alpha = 0.25f * waterAlpha),
            start = Offset(x, waterY),
            end = Offset(x, waterY + dripLength),
            strokeWidth = radius * 0.54f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = palette.rain.copy(alpha = 0.48f * waterAlpha),
            radius = radius,
            center = Offset(x, waterY + dripLength),
        )
        drawCircle(
            color = palette.highlight.copy(alpha = 0.62f * waterAlpha),
            radius = radius * 0.25f,
            center = Offset(x - radius * 0.28f, waterY + dripLength - radius * 0.26f),
        )
    }
    drawLine(
        color = palette.highlight.copy(alpha = 0.38f),
        start = Offset(10.dp.toPx(), 0.8.dp.toPx()),
        end = Offset(size.width * 0.42f, 0.8.dp.toPx()),
        strokeWidth = 0.65.dp.toPx(),
    )
}

private fun foregroundRainAlpha(style: RainStyle): Float = when (style) {
    RainStyle.LightRain -> 0.62f
    RainStyle.MediumRain -> 0.52f
    RainStyle.HeavyRain -> 0.44f
    RainStyle.Thunderstorm -> 0.40f
    RainStyle.AfterRain -> 0.30f
}

private fun DrawScope.drawRainCardFrame(
    palette: RainPalette,
    progress: Float,
    motionEnabled: Boolean,
) {
    val inset = 1.4.dp.toPx()
    drawLine(
        color = palette.highlight.copy(alpha = 0.26f),
        start = Offset(inset, inset),
        end = Offset(inset, size.height * 0.72f),
        strokeWidth = 0.7.dp.toPx(),
    )
    drawLine(
        color = palette.shadow.copy(alpha = 0.20f),
        start = Offset(size.width * 0.32f, size.height - inset),
        end = Offset(size.width - inset, size.height - inset),
        strokeWidth = 1.dp.toPx(),
    )
    val segmentWidth = minOf(size.width * 0.17f, 58.dp.toPx())
    val segmentStart = if (motionEnabled) {
        -segmentWidth + progress * (size.width + segmentWidth)
    } else {
        size.width * 0.18f
    }
    drawLine(
        color = palette.highlight.copy(alpha = if (motionEnabled) 0.48f else 0.30f),
        start = Offset(segmentStart, inset),
        end = Offset(segmentStart + segmentWidth, inset),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawRainMottoDetails(
    palette: RainPalette,
    progress: Float,
) {
    val y = size.height * 0.50f
    val start = 8.dp.toPx()
    val end = size.width - 8.dp.toPx()
    drawLine(
        color = palette.highlight.copy(alpha = 0.28f),
        start = Offset(start, y),
        end = Offset(end, y),
        strokeWidth = 0.58.dp.toPx(),
        cap = StrokeCap.Round,
    )
    val x = start + (end - start) * (0.06f + progress * 0.88f)
    val radius = 1.2.dp.toPx()
    drawLine(
        color = palette.rainAccent.copy(alpha = 0.34f),
        start = Offset(x, y - radius * 1.6f),
        end = Offset(x, y + radius * 0.6f),
        strokeWidth = 0.55.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = palette.rainAccent.copy(alpha = 0.66f),
        radius = radius,
        center = Offset(x, y + radius),
    )
    drawCircle(
        color = palette.highlight.copy(alpha = 0.78f),
        radius = radius * 0.25f,
        center = Offset(x - radius * 0.35f, y + radius * 0.62f),
    )
}

private fun DrawScope.drawRainNavigationUnderlay(
    style: RainStyle,
    palette: RainPalette,
    progress: Float,
    motionEnabled: Boolean,
) {
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                palette.highlight.copy(alpha = 0.15f),
                palette.ripple.copy(alpha = 0.055f),
                Color.Transparent,
            ),
        ),
    )
    val count = if (style == RainStyle.LightRain) 2 else 3
    repeat(count) { index ->
        val drift = if (motionEnabled) (progress * (5f + index)).dp.toPx() else 0f
        val x = size.width * (0.78f + index * 0.07f)
        drawLine(
            color = palette.rain.copy(alpha = 0.10f + (index % 2) * 0.035f),
            start = Offset(x, 5.dp.toPx() + drift),
            end = Offset(x - 1.5.dp.toPx(), 13.dp.toPx() + drift),
            strokeWidth = 0.7.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    val phase = if (motionEnabled) progress * PI.toFloat() * 2f else PI.toFloat() * 0.4f
    val wave = Path()
    repeat(17) { index ->
        val fraction = index / 16f
        val y = 3.2.dp.toPx() + sin(fraction * PI.toFloat() * 4f + phase) * 0.75.dp.toPx()
        if (index == 0) wave.moveTo(0f, y) else wave.lineTo(size.width * fraction, y)
    }
    drawPath(
        path = wave,
        color = palette.highlight.copy(alpha = 0.28f),
        style = Stroke(width = 0.65.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawRainNavigationFrame(palette: RainPalette) {
    drawLine(
        color = palette.highlight.copy(alpha = 0.20f),
        start = Offset(size.width * 0.12f, 1.dp.toPx()),
        end = Offset(size.width * 0.46f, 1.dp.toPx()),
        strokeWidth = 0.62.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun Random.nextFloatRange(start: Float, end: Float): Float {
    return start + nextFloat() * (end - start)
}

private const val STATIC_PROGRESS = 0.37f
private const val STATIC_CARD_MOTION_PROGRESS = 0.31f
private const val CARD_MOTION_CYCLE_MILLIS = 9_600
private const val RIPPLE_VISIBLE_FRACTION = 0.58f
private const val MIST_PARTICLE_COUNT = 96
private const val LENS_DROP_COUNT = 8
private const val RIPPLE_SEED = 0x4D19A2
private const val MIST_SEED = 0x73C0F1
private const val LENS_DROP_SEED = 0x31A7D4
private const val LIGHTNING_SEED = 0x7A11CE
