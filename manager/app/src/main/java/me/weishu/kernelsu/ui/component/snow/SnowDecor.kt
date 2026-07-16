package me.weishu.kernelsu.ui.component.snow

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.component.liquid.liquidGlassMiuixCardColors
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

@Composable
@ReadOnlyComposable
fun isSnowInterfaceStyle(): Boolean {
    return LocalInterfaceStyle.current == InterfaceStyle.Snow.value
}

@Composable
fun SeasonStyleWallpaper(
    modifier: Modifier = Modifier,
) {
    val dark = isInDarkTheme()
    val season = LocalSeasonStyle.current
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(season.wallpaperRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = seasonWallpaperOverlay(season, dark),
                ),
            )
        }
    }
}

@Composable
fun SeasonAmbientOverlay(
    modifier: Modifier = Modifier,
) {
    val season = LocalSeasonStyle.current
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val progress = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "seasonAmbient")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 18_000, easing = LinearEasing)),
            label = "seasonAmbientProgress",
        )
        animatedProgress
    } else {
        0.28f
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        when (season) {
            SeasonStyle.Spring -> drawSpringAmbient(progress)
            SeasonStyle.Summer -> drawSummerAmbient(progress)
            SeasonStyle.Autumn -> drawAutumnAmbient(progress)
            SeasonStyle.Winter -> Unit
        }
    }
}

@Composable
fun SnowBackdrop(
    snowColor: Color,
    accentColor: Color,
    useDefaultPhoto: Boolean,
    modifier: Modifier = Modifier,
) {
    val dark = isInDarkTheme()
    val season = LocalSeasonStyle.current
    Box(modifier = modifier.fillMaxSize()) {
        if (useDefaultPhoto) {
            Image(
                painter = painterResource(season.wallpaperRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (dark) 0.92f else 0.78f,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(0.8.dp),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (useDefaultPhoto) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = seasonWallpaperOverlay(season, dark),
                    ),
                )
            } else {
                drawRect(
                    color = seasonSurfaceTint(season).copy(alpha = if (dark) 0.10f else 0.08f),
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        lerp(accentColor, seasonSurfaceTint(season), 0.42f)
                            .copy(alpha = if (dark) 0.14f else 0.10f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.14f),
                    radius = size.maxDimension * 0.55f,
                ),
                radius = size.maxDimension * 0.55f,
                center = Offset(size.width * 0.18f, size.height * 0.14f),
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        if (dark) {
                            Color(0xFF031012).copy(alpha = 0.32f)
                        } else {
                            seasonSurfaceTint(season).copy(alpha = 0.14f)
                        },
                    ),
                    center = center,
                    radius = size.maxDimension * 0.74f,
                ),
            )

            if (season == SeasonStyle.Winter) {
                val frostDust = listOf(
                    0.07f to 0.08f,
                    0.16f to 0.18f,
                    0.27f to 0.07f,
                    0.39f to 0.14f,
                    0.52f to 0.06f,
                    0.65f to 0.17f,
                    0.78f to 0.09f,
                    0.91f to 0.16f,
                    0.11f to 0.32f,
                    0.46f to 0.28f,
                    0.84f to 0.35f,
                )
                frostDust.forEachIndexed { index, (x, y) ->
                    drawCircle(
                        color = snowColor.copy(alpha = if (dark) 0.30f else 0.54f),
                        radius = (if (index % 4 == 0) 1.35.dp else 0.8.dp).toPx(),
                        center = Offset(size.width * x, size.height * y),
                    )
                }
            }
        }
    }
}

@Composable
fun SnowCapBand(
    snowColor: Color,
    shadowColor: Color,
    modifier: Modifier = Modifier,
) {
    val season = LocalSeasonStyle.current
    val dark = isInDarkTheme()
    Canvas(modifier = modifier) {
        drawSeasonCardDecoration(
            season = season,
            capHeight = size.height,
            dark = dark,
            snowColor = snowColor,
            shadowColor = shadowColor,
        )
    }
}

@Composable
fun Modifier.snowMiuixCardSurface(
    shape: Shape = RoundedCornerShape(18.dp),
    enabled: Boolean = true,
    capHeight: Dp = 13.dp,
): Modifier {
    if (!enabled || !isSnowInterfaceStyle()) return uiDecoratedCard(shape = shape, enabled = enabled)

    val dark = isInDarkTheme()
    val season = LocalSeasonStyle.current
    val glassBrush = Brush.linearGradient(
        colors = seasonGlassColors(season, dark),
    )
    val snowColor = if (dark) Color(0xFFE4F2F5) else Color(0xFFFFFFFF)
    val shadowColor = if (dark) Color(0xFF71919C) else Color(0xFF9CBFCB)
    val edgeColor = seasonEdgeColor(season, dark)

    return this
        .shadow(3.dp, shape)
        .clip(shape)
        .background(glassBrush, shape)
        .border(1.dp, edgeColor, shape)
        .drawWithContent {
            drawContent()
            drawSeasonCardDecoration(
                season = season,
                capHeight = capHeight.toPx(),
                dark = dark,
                snowColor = snowColor,
                shadowColor = shadowColor,
            )
        }
        .uiDecoratedCard(shape = shape, enabled = enabled)
}

@Composable
fun snowMiuixCardColors(
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
    enabled: Boolean = true,
) = if (enabled && isSnowInterfaceStyle()) {
    val season = LocalSeasonStyle.current
    val dark = isInDarkTheme()
    val seasonalColor = if (season == SeasonStyle.Winter) {
        color
    } else {
        lerp(color, seasonSurfaceTint(season), if (dark) 0.10f else 0.08f)
    }
    CardDefaults.defaultColors(
        color = seasonalColor.copy(
            alpha = when {
                season == SeasonStyle.Winter && dark -> 0.24f
                season == SeasonStyle.Winter -> 0.30f
                dark -> 0.32f
                else -> 0.38f
            },
        ),
    )
} else {
    liquidGlassMiuixCardColors(color)
}

private fun seasonWallpaperOverlay(season: SeasonStyle, dark: Boolean): List<Color> {
    return when (season) {
        SeasonStyle.Spring -> if (dark) {
            listOf(Color(0x66304225), Color(0x553D4D25), Color(0xB20A160C))
        } else {
            listOf(Color(0x24FFFFFF), Color(0x102D5B32), Color(0x62384A24))
        }

        SeasonStyle.Summer -> if (dark) {
            listOf(Color(0x66102830), Color(0x4D0B4A4A), Color(0xB008171A))
        } else {
            listOf(Color(0x1AFFFFFF), Color(0x1422797A), Color(0x70213F35))
        }

        SeasonStyle.Autumn -> if (dark) {
            listOf(Color(0x662D271D), Color(0x594D331F), Color(0xB20E100D))
        } else {
            listOf(Color(0x1FFFFFFF), Color(0x124E402B), Color(0x6B4D3424))
        }

        SeasonStyle.Winter -> if (dark) {
            listOf(Color(0x4D09232B), Color(0x33102831), Color(0x8A061116))
        } else {
            listOf(Color(0x16FFFFFF), Color(0x0D287E86), Color(0x3D174C59))
        }
    }
}

private fun seasonSurfaceTint(season: SeasonStyle): Color {
    return when (season) {
        SeasonStyle.Spring -> Color(0xFF5E8D4E)
        SeasonStyle.Summer -> Color(0xFF21898A)
        SeasonStyle.Autumn -> Color(0xFFA56835)
        SeasonStyle.Winter -> Color(0xFF4C8F9A)
    }
}

private fun seasonGlassColors(season: SeasonStyle, dark: Boolean): List<Color> {
    if (season == SeasonStyle.Winter) {
        return if (dark) {
            listOf(Color(0x781A313B), Color(0x64112731), Color(0x70223C46))
        } else {
            listOf(Color(0x8FF9FDFF), Color(0x70E7F4F8), Color(0x80F5FBFD))
        }
    }

    return when (season) {
        SeasonStyle.Spring -> if (dark) {
            listOf(Color(0x7A223A28), Color(0x622B4227), Color(0x70485A2D))
        } else {
            listOf(Color(0xA6F7FBEF), Color(0x82E3F2D5), Color(0x8FF9F4D8))
        }

        SeasonStyle.Summer -> if (dark) {
            listOf(Color(0x7A12373D), Color(0x64103D42), Color(0x70305245))
        } else {
            listOf(Color(0xA6F1FBFA), Color(0x82D5F1ED), Color(0x8FE8F3DC))
        }

        SeasonStyle.Autumn -> if (dark) {
            listOf(Color(0x7A3B3028), Color(0x643B3229), Color(0x70524832))
        } else {
            listOf(Color(0xA6FFF8ED), Color(0x82F2E1CA), Color(0x8FF8EDD9))
        }

        SeasonStyle.Winter -> error("Winter colors are handled above")
    }
}

private fun seasonEdgeColor(season: SeasonStyle, dark: Boolean): Color {
    if (season == SeasonStyle.Winter) {
        return Color.White.copy(alpha = if (dark) 0.24f else 0.78f)
    }
    val tint = when (season) {
        SeasonStyle.Spring -> Color(0xFFCFE7B9)
        SeasonStyle.Summer -> Color(0xFFB7E7DF)
        SeasonStyle.Autumn -> Color(0xFFF0D0A5)
        SeasonStyle.Winter -> Color.White
    }
    return tint.copy(alpha = if (dark) 0.34f else 0.72f)
}

private fun DrawScope.drawSeasonCardDecoration(
    season: SeasonStyle,
    capHeight: Float,
    dark: Boolean,
    snowColor: Color,
    shadowColor: Color,
) {
    when (season) {
        SeasonStyle.Spring -> drawSpringCap(capHeight, dark)
        SeasonStyle.Summer -> drawSummerCap(capHeight, dark)
        SeasonStyle.Autumn -> drawAutumnCap(capHeight, dark)
        SeasonStyle.Winter -> drawSnowCap(capHeight, snowColor, shadowColor)
    }
}

private fun DrawScope.drawSpringCap(capHeight: Float, dark: Boolean) {
    val height = capHeight.coerceIn(1f, size.height)
    val grass = Path().apply {
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, height * 0.58f)
        cubicTo(
            size.width * 0.77f,
            height * 0.44f,
            size.width * 0.54f,
            height * 0.78f,
            size.width * 0.31f,
            height * 0.57f,
        )
        cubicTo(
            size.width * 0.19f,
            height * 0.45f,
            size.width * 0.08f,
            height * 0.72f,
            0f,
            height * 0.61f,
        )
        close()
    }
    drawPath(
        path = grass,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFB9D98D).copy(alpha = if (dark) 0.72f else 0.92f),
                Color(0xFF5F963F).copy(alpha = if (dark) 0.72f else 0.88f),
            ),
            endY = height,
        ),
    )

    val bladeColor = Color(0xFF3F7E39).copy(alpha = if (dark) 0.78f else 0.92f)
    listOf(0.08f, 0.16f, 0.27f, 0.39f, 0.51f, 0.64f, 0.78f, 0.91f).forEachIndexed { index, x ->
        val base = Offset(size.width * x, height * (0.58f + (index % 3) * 0.05f))
        val lean = if (index % 2 == 0) -1f else 1f
        drawLine(
            color = bladeColor,
            start = base,
            end = Offset(base.x + lean * height * 0.10f, height * (0.18f + (index % 4) * 0.05f)),
            strokeWidth = 0.7.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    val flowerRadius = (height * 0.16f).coerceIn(1.2.dp.toPx(), 2.8.dp.toPx())
    drawFlower(
        center = Offset(size.width * 0.18f, height * 0.27f),
        radius = flowerRadius,
        petalColor = Color(0xFFFFD93D),
        centerColor = Color(0xFFC77920),
    )
    drawFlower(
        center = Offset(size.width * 0.47f, height * 0.31f),
        radius = flowerRadius * 0.92f,
        petalColor = Color(0xFFF7B7C8),
        centerColor = Color(0xFFFFE3A5),
    )
    drawFlower(
        center = Offset(size.width * 0.82f, height * 0.24f),
        radius = flowerRadius * 1.05f,
        petalColor = Color(0xFFFFDF4C),
        centerColor = Color(0xFFB96B22),
    )
}

private fun DrawScope.drawSummerCap(capHeight: Float, dark: Boolean) {
    val height = capHeight.coerceIn(1f, size.height)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF82D6D1).copy(alpha = if (dark) 0.56f else 0.78f),
                Color(0xFF2A8D91).copy(alpha = if (dark) 0.64f else 0.74f),
                Color(0xFF17676F).copy(alpha = if (dark) 0.48f else 0.62f),
            ),
            endY = height,
        ),
        size = Size(size.width, height),
    )
    listOf(0.34f, 0.68f).forEachIndexed { index, y ->
        drawLine(
            color = Color.White.copy(alpha = if (dark) 0.20f else 0.42f),
            start = Offset(size.width * (0.03f + index * 0.08f), height * y),
            end = Offset(size.width * (0.42f + index * 0.24f), height * y),
            strokeWidth = 0.8.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    drawLotusLeaf(
        center = Offset(size.width * 0.15f, height * 0.42f),
        radius = height * 0.25f,
        color = Color(0xFF4A944D).copy(alpha = 0.92f),
        rotationDegrees = -10f,
    )
    drawLotusLeaf(
        center = Offset(size.width * 0.59f, height * 0.47f),
        radius = height * 0.20f,
        color = Color(0xFF70AA53).copy(alpha = 0.88f),
        rotationDegrees = 8f,
    )
    drawLotusRoot(
        center = Offset(size.width * 0.49f, height * 0.72f),
        radius = height * 0.11f,
    )
    drawLotusFlower(
        center = Offset(size.width * 0.78f, height * 0.38f),
        radius = height * 0.25f,
    )
    drawLotusSeedPod(
        center = Offset(size.width * 0.92f, height * 0.35f),
        radius = height * 0.16f,
    )
    drawDragonfly(
        center = Offset(size.width * 0.38f, height * 0.27f),
        scale = (height / 14.dp.toPx()).coerceIn(0.68f, 1.1f),
        alpha = if (dark) 0.72f else 0.86f,
    )
}

private fun DrawScope.drawAutumnCap(capHeight: Float, dark: Boolean) {
    val height = capHeight.coerceIn(1f, size.height)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFECE7DA).copy(alpha = if (dark) 0.52f else 0.82f),
                Color(0xFFB77A42).copy(alpha = if (dark) 0.55f else 0.74f),
                Color.Transparent,
            ),
            endY = height,
        ),
        size = Size(size.width, height),
    )
    drawLine(
        color = Color.White.copy(alpha = if (dark) 0.36f else 0.72f),
        start = Offset(6.dp.toPx(), 1.dp.toPx()),
        end = Offset((size.width - 6.dp.toPx()).coerceAtLeast(6.dp.toPx()), 1.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )

    listOf(0.12f, 0.31f, 0.52f, 0.72f, 0.89f).forEachIndexed { index, x ->
        val colors = listOf(Color(0xFFD89A3D), Color(0xFFB95334), Color(0xFF8A7D36))
        drawAutumnLeaf(
            center = Offset(size.width * x, height * (0.31f + (index % 2) * 0.18f)),
            length = height * (0.38f + (index % 3) * 0.05f),
            width = height * 0.20f,
            color = colors[index % colors.size].copy(alpha = if (dark) 0.78f else 0.92f),
            rotationDegrees = -34f + index * 19f,
        )
    }
    listOf(0.22f, 0.44f, 0.66f, 0.84f).forEachIndexed { index, x ->
        drawLine(
            color = Color(0xFFC9D6D7).copy(alpha = if (dark) 0.26f else 0.44f),
            start = Offset(size.width * x, height * 0.04f),
            end = Offset(size.width * x - height * 0.10f, height * (0.48f + index % 2 * 0.16f)),
            strokeWidth = 0.55.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private data class AmbientParticle(
    val x: Float,
    val phase: Float,
    val scale: Float,
    val drift: Float,
)

private val springParticles = listOf(
    AmbientParticle(0.08f, 0.02f, 0.82f, 18f),
    AmbientParticle(0.19f, 0.42f, 1.05f, 24f),
    AmbientParticle(0.31f, 0.71f, 0.72f, 16f),
    AmbientParticle(0.43f, 0.18f, 0.92f, 22f),
    AmbientParticle(0.57f, 0.56f, 1.12f, 28f),
    AmbientParticle(0.68f, 0.86f, 0.76f, 18f),
    AmbientParticle(0.79f, 0.31f, 0.98f, 24f),
    AmbientParticle(0.91f, 0.64f, 0.86f, 20f),
)

private val autumnParticles = listOf(
    AmbientParticle(0.06f, 0.11f, 0.82f, 24f),
    AmbientParticle(0.17f, 0.59f, 1.08f, 32f),
    AmbientParticle(0.29f, 0.86f, 0.76f, 18f),
    AmbientParticle(0.42f, 0.28f, 0.96f, 28f),
    AmbientParticle(0.55f, 0.71f, 1.14f, 36f),
    AmbientParticle(0.67f, 0.04f, 0.72f, 20f),
    AmbientParticle(0.79f, 0.47f, 0.91f, 26f),
    AmbientParticle(0.92f, 0.78f, 1.02f, 30f),
)

private fun DrawScope.drawSpringAmbient(progress: Float) {
    springParticles.forEachIndexed { index, particle ->
        val phase = wrap01(progress * 1.16f + particle.phase)
        val y = -24.dp.toPx() + phase * (size.height + 48.dp.toPx())
        val sway = sin((phase * PI * 3.0 + index).toFloat()) * particle.drift.dp.toPx()
        val color = if (index % 3 == 0) Color(0xFFFFD84A) else Color(0xFFF8C8D4)
        drawPetal(
            center = Offset(size.width * particle.x + sway, y),
            length = 6.dp.toPx() * particle.scale,
            width = 3.dp.toPx() * particle.scale,
            color = color.copy(alpha = 0.42f),
            rotationDegrees = phase * 320f + index * 37f,
        )
    }
}

private fun DrawScope.drawSummerAmbient(progress: Float) {
    listOf(
        Offset(size.width * 0.18f, size.height * 0.23f),
        Offset(size.width * 0.72f, size.height * 0.49f),
        Offset(size.width * 0.42f, size.height * 0.78f),
    ).forEachIndexed { index, center ->
        val phase = wrap01(progress * 1.3f + index * 0.31f)
        val rippleWidth = size.width * (0.10f + phase * 0.23f)
        val rippleHeight = 8.dp.toPx() + phase * 22.dp.toPx()
        drawOval(
            color = Color(0xFFBCEDE7).copy(alpha = (1f - phase) * 0.17f),
            topLeft = Offset(center.x - rippleWidth / 2f, center.y - rippleHeight / 2f),
            size = Size(rippleWidth, rippleHeight),
            style = Stroke(width = 0.8.dp.toPx()),
        )
    }

    val flight = wrap01(progress * 0.72f + 0.08f)
    drawDragonfly(
        center = Offset(
            x = -20.dp.toPx() + flight * (size.width + 40.dp.toPx()),
            y = size.height * 0.31f + sin((flight * PI * 4.0).toFloat()) * 22.dp.toPx(),
        ),
        scale = 0.82f,
        alpha = 0.36f,
    )
}

private fun DrawScope.drawAutumnAmbient(progress: Float) {
    repeat(14) { index ->
        val phase = wrap01(progress * 6.2f + index * 0.073f)
        val x = size.width * ((index * 0.173f + 0.04f) % 1f)
        val y = -28.dp.toPx() + phase * (size.height + 56.dp.toPx())
        drawLine(
            color = Color(0xFFC7D3D5).copy(alpha = 0.13f),
            start = Offset(x, y),
            end = Offset(x - 5.dp.toPx(), y + 18.dp.toPx()),
            strokeWidth = 0.65.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    autumnParticles.forEachIndexed { index, particle ->
        val phase = wrap01(progress * 1.24f + particle.phase)
        val y = -30.dp.toPx() + phase * (size.height + 60.dp.toPx())
        val sway = sin((phase * PI * 3.0 + index * 0.6).toFloat()) * particle.drift.dp.toPx()
        val colors = listOf(Color(0xFFD49A3D), Color(0xFFB95437), Color(0xFF8A7C3D))
        drawAutumnLeaf(
            center = Offset(size.width * particle.x + sway, y),
            length = 9.dp.toPx() * particle.scale,
            width = 4.dp.toPx() * particle.scale,
            color = colors[index % colors.size].copy(alpha = 0.38f),
            rotationDegrees = phase * 420f + index * 29f,
        )
    }
}

private fun DrawScope.drawFlower(
    center: Offset,
    radius: Float,
    petalColor: Color,
    centerColor: Color,
) {
    repeat(5) { index ->
        val angle = (index * 72f - 90f) * (PI / 180f)
        drawCircle(
            color = petalColor,
            radius = radius * 0.72f,
            center = Offset(
                center.x + cos(angle).toFloat() * radius * 0.72f,
                center.y + sin(angle).toFloat() * radius * 0.72f,
            ),
        )
    }
    drawCircle(color = centerColor, radius = radius * 0.44f, center = center)
}

private fun DrawScope.drawPetal(
    center: Offset,
    length: Float,
    width: Float,
    color: Color,
    rotationDegrees: Float,
) {
    rotate(rotationDegrees, center) {
        drawOval(
            color = color,
            topLeft = Offset(center.x - length / 2f, center.y - width / 2f),
            size = Size(length, width),
        )
    }
}

private fun DrawScope.drawLotusLeaf(
    center: Offset,
    radius: Float,
    color: Color,
    rotationDegrees: Float,
) {
    rotate(rotationDegrees, center) {
        drawOval(
            color = color,
            topLeft = Offset(center.x - radius, center.y - radius * 0.54f),
            size = Size(radius * 2f, radius * 1.08f),
        )
        drawLine(
            color = Color(0xFFE5F1CB).copy(alpha = 0.46f),
            start = center,
            end = Offset(center.x + radius * 0.82f, center.y),
            strokeWidth = 0.55.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawLotusFlower(center: Offset, radius: Float) {
    repeat(7) { index ->
        val angle = -62f + index * 20f
        rotate(angle, center) {
            drawOval(
                color = if (index % 2 == 0) Color(0xFFFFE7ED) else Color(0xFFF5AFC2),
                topLeft = Offset(center.x - radius * 0.62f, center.y - radius * 0.27f),
                size = Size(radius * 1.24f, radius * 0.54f),
            )
        }
    }
    drawCircle(Color(0xFFF5C94A), radius * 0.20f, center)
}

private fun DrawScope.drawLotusSeedPod(center: Offset, radius: Float) {
    drawLine(
        color = Color(0xFF477747),
        start = Offset(center.x, center.y + radius * 0.62f),
        end = Offset(center.x + radius * 0.18f, center.y + radius * 1.8f),
        strokeWidth = 0.7.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawOval(
        color = Color(0xFF9FC16A),
        topLeft = Offset(center.x - radius, center.y - radius * 0.55f),
        size = Size(radius * 2f, radius * 1.1f),
    )
    listOf(-0.42f to -0.10f, 0f to 0.06f, 0.42f to -0.10f).forEach { (x, y) ->
        drawCircle(
            color = Color(0xFF5D723C).copy(alpha = 0.72f),
            radius = radius * 0.12f,
            center = Offset(center.x + radius * x, center.y + radius * y),
        )
    }
}

private fun DrawScope.drawLotusRoot(center: Offset, radius: Float) {
    repeat(3) { index ->
        val segmentCenter = Offset(center.x + (index - 1) * radius * 1.45f, center.y)
        drawOval(
            color = Color(0xFFE7C58E).copy(alpha = 0.88f),
            topLeft = Offset(segmentCenter.x - radius, segmentCenter.y - radius * 0.68f),
            size = Size(radius * 2f, radius * 1.36f),
        )
        drawCircle(
            color = Color(0xFF9D7650).copy(alpha = 0.58f),
            radius = radius * 0.18f,
            center = Offset(segmentCenter.x - radius * 0.24f, segmentCenter.y),
        )
        drawCircle(
            color = Color(0xFF9D7650).copy(alpha = 0.58f),
            radius = radius * 0.15f,
            center = Offset(segmentCenter.x + radius * 0.28f, segmentCenter.y),
        )
    }
}

private fun DrawScope.drawDragonfly(center: Offset, scale: Float, alpha: Float) {
    val bodyLength = 9.dp.toPx() * scale
    val wingLength = 5.dp.toPx() * scale
    val wingWidth = 2.2.dp.toPx() * scale
    val bodyColor = Color(0xFF274B4D).copy(alpha = alpha)
    val wingColor = Color(0xFFE7FAF7).copy(alpha = alpha * 0.78f)
    rotate(-9f, center) {
        drawLine(
            color = bodyColor,
            start = Offset(center.x - bodyLength / 2f, center.y),
            end = Offset(center.x + bodyLength / 2f, center.y),
            strokeWidth = 1.dp.toPx() * scale,
            cap = StrokeCap.Round,
        )
        listOf(-34f, 34f, 146f, 214f).forEach { angle ->
            rotate(angle, center) {
                drawOval(
                    color = wingColor,
                    topLeft = Offset(center.x, center.y - wingWidth / 2f),
                    size = Size(wingLength, wingWidth),
                )
            }
        }
        drawCircle(bodyColor, 1.2.dp.toPx() * scale, Offset(center.x - bodyLength / 2f, center.y))
    }
}

private fun DrawScope.drawAutumnLeaf(
    center: Offset,
    length: Float,
    width: Float,
    color: Color,
    rotationDegrees: Float,
) {
    rotate(rotationDegrees, center) {
        val leaf = Path().apply {
            moveTo(center.x - length / 2f, center.y)
            cubicTo(
                center.x - length * 0.18f,
                center.y - width,
                center.x + length * 0.24f,
                center.y - width * 0.72f,
                center.x + length / 2f,
                center.y,
            )
            cubicTo(
                center.x + length * 0.20f,
                center.y + width * 0.80f,
                center.x - length * 0.22f,
                center.y + width,
                center.x - length / 2f,
                center.y,
            )
            close()
        }
        drawPath(leaf, color)
        drawLine(
            color = Color(0xFF6E4D2B).copy(alpha = color.alpha * 0.72f),
            start = Offset(center.x - length * 0.42f, center.y),
            end = Offset(center.x + length * 0.42f, center.y),
            strokeWidth = 0.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun wrap01(value: Float): Float = value - floor(value)

private fun DrawScope.drawSnowCap(
    capHeight: Float,
    snowColor: Color,
    shadowColor: Color,
) {
    val resolvedHeight = capHeight.coerceIn(1f, size.height)
    drawPath(
        path = snowCapPath(
            verticalOffset = 1.6.dp.toPx(),
            capHeight = resolvedHeight,
        ),
        color = shadowColor.copy(alpha = 0.34f),
    )
    drawPath(
        path = snowCapPath(
            verticalOffset = 0f,
            capHeight = resolvedHeight,
        ),
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.98f),
                snowColor.copy(alpha = 0.96f),
                snowColor.copy(alpha = 0.88f),
            ),
            endY = resolvedHeight,
        ),
    )

    val inset = 8.dp.toPx().coerceAtMost(size.width * 0.12f)
    drawLine(
        color = Color.White.copy(alpha = 0.82f),
        start = Offset(inset, 1.dp.toPx()),
        end = Offset(
            (size.width - inset).coerceAtLeast(inset),
            1.dp.toPx(),
        ),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )
    listOf(0.14f, 0.43f, 0.76f).forEachIndexed { index, x ->
        drawCircle(
            color = Color.White.copy(alpha = 0.62f),
            radius = (if (index == 1) 1.dp else 0.7.dp).toPx(),
            center = Offset(
                size.width * x,
                resolvedHeight * (0.28f + index * 0.06f),
            ),
        )
    }
}

private fun DrawScope.snowCapPath(
    verticalOffset: Float,
    capHeight: Float = size.height,
): Path {
    val resolvedHeight = capHeight.coerceAtLeast(1f)
    return Path().apply {
        moveTo(0f, verticalOffset)
        lineTo(size.width, verticalOffset)
        lineTo(size.width, resolvedHeight * 0.50f + verticalOffset)
        cubicTo(
            size.width * 0.91f,
            resolvedHeight * 0.56f + verticalOffset,
            size.width * 0.85f,
            resolvedHeight * 0.78f + verticalOffset,
            size.width * 0.75f,
            resolvedHeight * 0.61f + verticalOffset,
        )
        cubicTo(
            size.width * 0.64f,
            resolvedHeight * 0.43f + verticalOffset,
            size.width * 0.56f,
            resolvedHeight * 0.82f + verticalOffset,
            size.width * 0.44f,
            resolvedHeight * 0.65f + verticalOffset,
        )
        cubicTo(
            size.width * 0.33f,
            resolvedHeight * 0.49f + verticalOffset,
            size.width * 0.25f,
            resolvedHeight * 0.76f + verticalOffset,
            size.width * 0.15f,
            resolvedHeight * 0.60f + verticalOffset,
        )
        cubicTo(
            size.width * 0.08f,
            resolvedHeight * 0.49f + verticalOffset,
            size.width * 0.04f,
            resolvedHeight * 0.70f + verticalOffset,
            0f,
            resolvedHeight * 0.58f + verticalOffset,
        )
        close()
    }
}
