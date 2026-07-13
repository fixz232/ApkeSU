package me.weishu.kernelsu.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

const val NIGHT_BACKGROUND_EFFECT_KEY = "night_background_effect"
const val NIGHT_BACKGROUND_PASSTHROUGH_KEY = "night_background_passthrough"
const val NIGHT_BACKGROUND_PASSTHROUGH_OPACITY_KEY = "night_background_passthrough_opacity"
const val MIN_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY = 0.05f
const val MAX_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY = 1f
const val DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY = 0.35f

val LocalNightBackgroundEffectActive = compositionLocalOf { false }

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
    modifier: Modifier = Modifier,
    passthroughOpacity: Float = DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY,
) {
    val effect = NightBackgroundEffect.fromValue(effectValue)
    if (!enabled || !isInDarkTheme() || effect == NightBackgroundEffect.Off) return

    val effectAlpha = if (passthrough) {
        sanitizeNightBackgroundPassthroughOpacity(passthroughOpacity)
    } else {
        1f
    }
    val photoResource = when (effect) {
        NightBackgroundEffect.Aurora -> R.drawable.night_background_aurora
        NightBackgroundEffect.Galaxy -> R.drawable.night_background_galaxy
        else -> null
    }
    if (photoResource != null) {
        NightPhotoBackground(
            drawableResource = photoResource,
            alpha = effectAlpha,
            passthrough = passthrough,
            modifier = modifier,
        )
        return
    }

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
        if (!passthrough) {
            drawNightBase(spec)
        }
        drawStarLayer(stars, progress, effectAlpha * spec.starAlphaScale)
        when (effect) {
            NightBackgroundEffect.Aurora -> Unit
            NightBackgroundEffect.Galaxy -> Unit
            NightBackgroundEffect.Nebula -> drawNebula(progress, effectAlpha)
            NightBackgroundEffect.Starfield -> drawStarfieldGlow(progress, effectAlpha)
            NightBackgroundEffect.Off -> Unit
        }
        drawNightVignette(effectAlpha, passthrough)
    }
}

@Composable
private fun NightPhotoBackground(
    @DrawableRes drawableResource: Int,
    alpha: Float,
    passthrough: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(drawableResource),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = alpha,
            modifier = Modifier.fillMaxSize(),
        )
        if (!passthrough) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                        ),
                    ),
                )
            }
        }
    }
}

private data class NightBackgroundSpec(
    val cycleMillis: Int,
    val starCount: Int,
    val baseColor: Color,
    val horizonColor: Color,
    val starAlphaScale: Float,
) {
    companion object {
        fun forEffect(effect: NightBackgroundEffect): NightBackgroundSpec {
            return when (effect) {
                NightBackgroundEffect.Aurora -> NightBackgroundSpec(
                    cycleMillis = 38000,
                    starCount = 86,
                    baseColor = Color(0xFF020B13),
                    horizonColor = Color(0xFF071C24),
                    starAlphaScale = 0.88f,
                )

                NightBackgroundEffect.Galaxy -> NightBackgroundSpec(
                    cycleMillis = 42000,
                    starCount = 128,
                    baseColor = Color(0xFF02030C),
                    horizonColor = Color(0xFF0A0B21),
                    starAlphaScale = 0.94f,
                )

                NightBackgroundEffect.Nebula -> NightBackgroundSpec(
                    cycleMillis = 34000,
                    starCount = 68,
                    baseColor = Color(0xFF090413),
                    horizonColor = Color(0xFF180925),
                    starAlphaScale = 0.86f,
                )

                NightBackgroundEffect.Starfield -> NightBackgroundSpec(
                    cycleMillis = 30000,
                    starCount = 96,
                    baseColor = Color(0xFF02040D),
                    horizonColor = Color(0xFF071024),
                    starAlphaScale = 0.92f,
                )

                NightBackgroundEffect.Off -> NightBackgroundSpec(
                    cycleMillis = 30000,
                    starCount = 0,
                    baseColor = Color.Transparent,
                    horizonColor = Color.Transparent,
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
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                spec.baseColor.copy(alpha = 0.98f),
                spec.baseColor.copy(alpha = 0.92f),
                spec.horizonColor.copy(alpha = 0.94f),
                spec.baseColor.copy(alpha = 0.98f),
            ),
        ),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                spec.horizonColor.copy(alpha = 0.2f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.5f, size.height * 0.82f),
            radius = size.maxDimension * 0.72f,
        ),
        radius = size.maxDimension * 0.72f,
        center = Offset(size.width * 0.5f, size.height * 0.82f),
    )
}

private fun DrawScope.drawStarLayer(stars: List<NightStar>, progress: Float, alphaScale: Float) {
    stars.forEach { star ->
        val depth = ((star.radius - 0.65f) / 1.5f).coerceIn(0f, 1f)
        val driftX = sin((progress + star.phase) * TWO_PI) * (1.2f + depth * 3.2f).dp.toPx()
        val driftY = cos((progress + star.phase * 0.61f) * TWO_PI) * (0.4f + depth).dp.toPx()
        val twinkle = 0.72f + 0.28f * sin((progress * 2f + star.phase) * TWO_PI).coerceAtLeast(0f)
        val center = Offset(star.x * size.width + driftX, star.y * size.height + driftY)
        val visibleAlpha = (star.alpha * twinkle * alphaScale).coerceIn(0f, 0.9f)
        if (star.radius > 1.45f) {
            drawCircle(
                color = star.tint.copy(alpha = visibleAlpha * 0.12f),
                radius = star.radius.dp.toPx() * 3.2f,
                center = center,
            )
        }
        drawCircle(
            color = star.tint.copy(alpha = visibleAlpha),
            radius = star.radius.dp.toPx(),
            center = center,
        )
        if (star.radius > 1.8f && star.phase > 0.58f) {
            drawLine(
                color = star.tint.copy(alpha = visibleAlpha * 0.34f),
                start = Offset(center.x - star.radius.dp.toPx() * 2.2f, center.y),
                end = Offset(center.x + star.radius.dp.toPx() * 2.2f, center.y),
                strokeWidth = 0.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawNebula(progress: Float, alphaScale: Float) {
    val phase = sin(progress * TWO_PI)
    val crossPhase = cos(progress * TWO_PI)
    drawNebulaCloud(
        color = Color(0xFFFF5FD7),
        alpha = 0.2f * alphaScale,
        radius = size.minDimension * (0.48f + phase * 0.025f),
        center = Offset(
            size.width * (0.25f + crossPhase * 0.018f),
            size.height * (0.31f + phase * 0.014f),
        ),
    )
    drawNebulaCloud(
        color = Color(0xFF5DDCFF),
        alpha = 0.16f * alphaScale,
        radius = size.minDimension * (0.39f - phase * 0.018f),
        center = Offset(
            size.width * (0.75f - phase * 0.02f),
            size.height * (0.51f + crossPhase * 0.018f),
        ),
    )
    drawNebulaCloud(
        color = Color(0xFF8B5CFF),
        alpha = 0.18f * alphaScale,
        radius = size.minDimension * (0.54f + crossPhase * 0.02f),
        center = Offset(size.width * 0.54f, size.height * (0.16f - phase * 0.012f)),
    )
    drawNebulaCloud(
        color = Color(0xFFFFC46B),
        alpha = 0.075f * alphaScale,
        radius = size.minDimension * 0.24f,
        center = Offset(size.width * 0.42f, size.height * 0.44f),
    )
    drawLine(
        color = Color(0xFF02030A).copy(alpha = 0.18f * alphaScale),
        start = Offset(-size.width * 0.08f, size.height * 0.25f),
        end = Offset(size.width * 1.08f, size.height * 0.67f),
        strokeWidth = size.minDimension * 0.075f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawNebulaCloud(
    color: Color,
    alpha: Float,
    radius: Float,
    center: Offset,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.42f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawStarfieldGlow(progress: Float, alphaScale: Float) {
    val glow = 0.7f + 0.3f * sin(progress * TWO_PI)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF4CC9F0).copy(alpha = 0.13f * glow * alphaScale),
                Color(0xFF4C6EF0).copy(alpha = 0.045f * glow * alphaScale),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.78f, size.height * 0.22f),
            radius = size.minDimension * 0.46f,
        ),
        radius = size.minDimension * 0.42f,
        center = Offset(size.width * 0.78f, size.height * 0.22f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF0B3).copy(alpha = 0.09f * glow * alphaScale),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.18f, size.height * 0.72f),
            radius = size.minDimension * 0.28f,
        ),
        radius = size.minDimension * 0.25f,
        center = Offset(size.width * 0.18f, size.height * 0.72f),
    )
    drawComet(progress, alphaScale)
}

private fun DrawScope.drawComet(progress: Float, alphaScale: Float) {
    val phase = (progress * 3f) % 1f
    val pulse = sin(phase * PI).toFloat().coerceAtLeast(0f)
    val visibility = pulse * pulse * alphaScale
    if (visibility <= 0.01f) return

    val head = Offset(
        x = size.width * (1.16f - phase * 1.42f),
        y = size.height * (0.08f + phase * 0.38f),
    )
    val tail = Offset(head.x + size.width * 0.18f, head.y - size.height * 0.065f)
    drawLine(
        color = Color(0xFFA8D8FF).copy(alpha = 0.12f * visibility),
        start = tail,
        end = head,
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color.White.copy(alpha = 0.62f * visibility),
        start = Offset((tail.x + head.x) * 0.5f, (tail.y + head.y) * 0.5f),
        end = head,
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = Color(0xFFDDF3FF).copy(alpha = 0.18f * visibility),
        radius = 8.dp.toPx(),
        center = head,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.9f * visibility),
        radius = 1.5.dp.toPx(),
        center = head,
    )
}

private fun DrawScope.drawNightVignette(alphaScale: Float, passthrough: Boolean) {
    val edgeAlpha = (if (passthrough) 0.1f else 0.28f) * alphaScale
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color.Black.copy(alpha = edgeAlpha),
            ),
            center = Offset(size.width * 0.5f, size.height * 0.43f),
            radius = size.maxDimension * 0.78f,
        ),
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

private const val TWO_PI = (PI * 2.0).toFloat()
