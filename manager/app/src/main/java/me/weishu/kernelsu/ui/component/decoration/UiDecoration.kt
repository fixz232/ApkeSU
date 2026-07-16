package me.weishu.kernelsu.ui.component.decoration

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LocalUiDecorationConfig = staticCompositionLocalOf { UiDecorationConfig() }
val LocalUiDecorationScope = staticCompositionLocalOf { UiDecorationScope.Secondary }

@Composable
@ReadOnlyComposable
fun isUiDecorationActive(): Boolean {
    return LocalUiDecorationConfig.current.isActiveFor(LocalUiDecorationScope.current)
}

@Composable
fun UiDecorationBackdrop(modifier: Modifier = Modifier) {
    val config = LocalUiDecorationConfig.current
    val active = config.isActiveFor(LocalUiDecorationScope.current) && config.background != UiBackgroundDecoration.None
    if (!active) return
    val palette = uiDecorationPalette()
    val progress = decorationProgress(config.motionEnabled, "uiDecorationBackdrop")
    Canvas(modifier = modifier.fillMaxSize()) {
        val alpha = config.opacity * config.intensity
        when (config.background) {
            UiBackgroundDecoration.None -> Unit
            UiBackgroundDecoration.SoftRays -> drawSoftRays(palette, alpha, progress)
            UiBackgroundDecoration.StarMap -> drawStarMap(palette, alpha, progress)
            UiBackgroundDecoration.Botanical -> drawBotanicalBackdrop(palette, alpha, progress)
            UiBackgroundDecoration.Frost -> drawFrostBackdrop(palette, alpha, progress)
        }
    }
}

@Composable
fun UiDecorationChromeOverlay(modifier: Modifier = Modifier) {
    val config = LocalUiDecorationConfig.current
    val active = config.isActiveFor(LocalUiDecorationScope.current)
    if (!active || (config.topBar == UiTopBarDecoration.None && config.navigation == UiNavigationDecoration.None)) return
    val palette = uiDecorationPalette()
    val progress = decorationProgress(config.motionEnabled, "uiDecorationChrome")
    val topEdge = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    val navigationHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 82.dp
    Canvas(modifier = modifier.fillMaxSize()) {
        val alpha = config.opacity * config.intensity
        drawTopBarDecoration(
            style = config.topBar,
            palette = palette,
            alpha = alpha,
            edgeY = topEdge.toPx().coerceAtMost(size.height * 0.24f),
            progress = progress,
        )
        drawNavigationDecoration(
            style = config.navigation,
            palette = palette,
            alpha = alpha,
            areaHeight = navigationHeight.toPx().coerceAtMost(size.height * 0.24f),
            progress = progress,
        )
    }
}

@Composable
fun Modifier.uiDecoratedCard(
    shape: Shape,
    enabled: Boolean = true,
): Modifier {
    val config = LocalUiDecorationConfig.current
    if (!enabled || !config.isActiveFor(LocalUiDecorationScope.current) || config.card == UiCardDecoration.None) {
        return this
    }
    val palette = uiDecorationPalette()
    val seasonalInterface = LocalInterfaceStyle.current == InterfaceStyle.Snow.value
    val style = if (seasonalInterface && config.card in SEASONAL_CARD_DECORATIONS) {
        UiCardDecoration.Highlight
    } else {
        config.card
    }
    val alpha = config.opacity * config.intensity
    return drawWithContent {
        drawContent()
        drawCardDecoration(style, shape, palette, alpha)
    }
}

@Composable
private fun decorationProgress(motionEnabled: Boolean, label: String): Float {
    val systemAnimationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!motionEnabled || !systemAnimationsEnabled) return 0.24f
    val transition = rememberInfiniteTransition(label = label)
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
        label = "${label}Progress",
    )
    return progress
}

@Composable
@ReadOnlyComposable
private fun uiDecorationPalette(): UiDecorationPalette {
    val dark = isInDarkTheme()
    val primary = when (LocalUiMode.current) {
        UiMode.Material -> androidx.compose.material3.MaterialTheme.colorScheme.primary
        UiMode.Miuix -> MiuixTheme.colorScheme.primary
    }
    val secondary = lerp(
        primary,
        if (dark) Color(0xFF8FD3C7) else Color(0xFF247E72),
        0.38f,
    )
    return UiDecorationPalette(
        primary = primary,
        secondary = secondary,
        highlight = if (dark) Color(0xFFEAF8F5) else Color.White,
        muted = if (dark) Color(0xFF82908F) else Color(0xFF55615F),
        dark = dark,
    )
}

private data class UiDecorationPalette(
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val muted: Color,
    val dark: Boolean,
)

private fun DrawScope.drawSoftRays(palette: UiDecorationPalette, alpha: Float, progress: Float) {
    val shift = sin(progress * PI * 2.0).toFloat() * size.width * 0.04f
    repeat(3) { index ->
        val startX = size.width * (-0.18f + index * 0.34f) + shift
        val band = Path().apply {
            moveTo(startX, 0f)
            lineTo(startX + size.width * 0.20f, 0f)
            lineTo(startX + size.width * 0.62f, size.height)
            lineTo(startX + size.width * 0.34f, size.height)
            close()
        }
        drawPath(
            path = band,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    palette.primary.copy(alpha = alpha * (0.055f + index * 0.012f)),
                    Color.Transparent,
                ),
            ),
        )
    }
}

private fun DrawScope.drawStarMap(palette: UiDecorationPalette, alpha: Float, progress: Float) {
    val points = STAR_POINTS.mapIndexed { index, point ->
        val drift = sin((progress * PI * 2.0 + index * 0.7)).toFloat() * 3.dp.toPx()
        Offset(size.width * point.first, size.height * point.second + drift)
    }
    STAR_CONNECTIONS.forEach { (from, to) ->
        drawLine(
            color = palette.primary.copy(alpha = alpha * 0.10f),
            start = points[from],
            end = points[to],
            strokeWidth = 0.7.dp.toPx(),
        )
    }
    points.forEachIndexed { index, point ->
        val twinkle = 0.45f + 0.55f * ((sin((progress * PI * 4.0 + index).toFloat()) + 1f) / 2f)
        drawCircle(
            color = if (index % 4 == 0) palette.secondary else palette.highlight,
            radius = (if (index % 5 == 0) 1.3.dp else 0.8.dp).toPx(),
            center = point,
            alpha = alpha * 0.28f * twinkle,
        )
    }
}

private fun DrawScope.drawBotanicalBackdrop(palette: UiDecorationPalette, alpha: Float, progress: Float) {
    val sway = sin(progress * PI * 2.0).toFloat() * 4f
    drawStemWithLeaves(
        origin = Offset(-4.dp.toPx(), size.height * 0.23f),
        direction = Offset(size.width * 0.20f, -size.height * 0.12f),
        color = palette.secondary.copy(alpha = alpha * 0.16f),
        rotationDegrees = sway,
    )
    drawStemWithLeaves(
        origin = Offset(size.width + 4.dp.toPx(), size.height * 0.74f),
        direction = Offset(-size.width * 0.22f, size.height * 0.10f),
        color = palette.primary.copy(alpha = alpha * 0.14f),
        rotationDegrees = -sway,
    )
}

private fun DrawScope.drawFrostBackdrop(palette: UiDecorationPalette, alpha: Float, progress: Float) {
    val shimmer = 0.72f + sin(progress * PI * 2.0).toFloat() * 0.18f
    FROST_POINTS.forEachIndexed { index, point ->
        val center = Offset(size.width * point.first, size.height * point.second)
        val radius = (4 + (index % 3) * 2).dp.toPx()
        repeat(3) { branch ->
            val angle = (branch * 60f + index * 17f) * (PI / 180f)
            val end = Offset(
                center.x + cos(angle).toFloat() * radius,
                center.y + sin(angle).toFloat() * radius,
            )
            drawLine(
                color = palette.highlight.copy(alpha = alpha * 0.14f * shimmer),
                start = center,
                end = end,
                strokeWidth = 0.65.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawTopBarDecoration(
    style: UiTopBarDecoration,
    palette: UiDecorationPalette,
    alpha: Float,
    edgeY: Float,
    progress: Float,
) {
    when (style) {
        UiTopBarDecoration.None -> Unit
        UiTopBarDecoration.FineLine -> {
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, palette.primary.copy(alpha = alpha * 0.42f), Color.Transparent)
                ),
                start = Offset(0f, edgeY),
                end = Offset(size.width, edgeY),
                strokeWidth = 1.dp.toPx(),
            )
        }
        UiTopBarDecoration.Prism -> {
            val segment = size.width / 3f
            listOf(palette.primary, palette.secondary, palette.highlight).forEachIndexed { index, color ->
                drawLine(
                    color = color.copy(alpha = alpha * (if (index == 2) 0.22f else 0.38f)),
                    start = Offset(segment * index, edgeY),
                    end = Offset(segment * (index + 1), edgeY),
                    strokeWidth = (1f + index * 0.25f).dp.toPx(),
                )
            }
        }
        UiTopBarDecoration.Seasonal -> {
            drawLine(
                color = palette.secondary.copy(alpha = alpha * 0.32f),
                start = Offset(size.width * 0.08f, edgeY),
                end = Offset(size.width * 0.92f, edgeY),
                strokeWidth = 0.8.dp.toPx(),
                cap = StrokeCap.Round,
            )
            listOf(0.17f, 0.50f, 0.83f).forEachIndexed { index, x ->
                drawTinyFlower(
                    center = Offset(size.width * x, edgeY),
                    radius = (1.8f + index * 0.2f).dp.toPx(),
                    color = if (index == 1) palette.primary else palette.secondary,
                    alpha = alpha * 0.55f,
                )
            }
        }
        UiTopBarDecoration.Circuit -> {
            val pulse = 0.65f + sin(progress * PI * 2.0).toFloat() * 0.25f
            val path = Path().apply {
                moveTo(size.width * 0.04f, edgeY)
                lineTo(size.width * 0.28f, edgeY)
                lineTo(size.width * 0.32f, edgeY - 4.dp.toPx())
                lineTo(size.width * 0.68f, edgeY - 4.dp.toPx())
                lineTo(size.width * 0.72f, edgeY)
                lineTo(size.width * 0.96f, edgeY)
            }
            drawPath(
                path = path,
                color = palette.primary.copy(alpha = alpha * 0.42f * pulse),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
            )
            listOf(0.04f, 0.32f, 0.68f, 0.96f).forEach { x ->
                drawCircle(
                    color = palette.secondary.copy(alpha = alpha * 0.55f),
                    radius = 1.4.dp.toPx(),
                    center = Offset(size.width * x, if (x in 0.33f..0.67f) edgeY - 4.dp.toPx() else edgeY),
                )
            }
        }
    }
}

private fun DrawScope.drawNavigationDecoration(
    style: UiNavigationDecoration,
    palette: UiDecorationPalette,
    alpha: Float,
    areaHeight: Float,
    progress: Float,
) {
    val center = Offset(size.width / 2f, size.height - areaHeight * 0.42f)
    when (style) {
        UiNavigationDecoration.None -> Unit
        UiNavigationDecoration.UnderGlow -> {
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, palette.primary.copy(alpha = alpha * 0.24f), Color.Transparent)
                ),
                start = Offset(size.width * 0.10f, center.y + 20.dp.toPx()),
                end = Offset(size.width * 0.90f, center.y + 20.dp.toPx()),
                strokeWidth = 10.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        UiNavigationDecoration.LiquidHalo -> {
            repeat(2) { index ->
                val path = Path().apply {
                    moveTo(size.width * 0.12f, center.y + index * 5.dp.toPx())
                    cubicTo(
                        size.width * 0.34f,
                        center.y - 8.dp.toPx(),
                        size.width * 0.66f,
                        center.y + 8.dp.toPx(),
                        size.width * 0.88f,
                        center.y + index * 5.dp.toPx(),
                    )
                }
                drawPath(
                    path = path,
                    color = (if (index == 0) palette.primary else palette.secondary)
                        .copy(alpha = alpha * (0.25f - index * 0.07f)),
                    style = Stroke(width = (1.4f - index * 0.3f).dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        UiNavigationDecoration.Orbit -> {
            val orbitWidth = size.width * 0.68f
            val orbitHeight = 34.dp.toPx()
            drawOval(
                color = palette.primary.copy(alpha = alpha * 0.20f),
                topLeft = Offset(center.x - orbitWidth / 2f, center.y - orbitHeight / 2f),
                size = Size(orbitWidth, orbitHeight),
                style = Stroke(width = 0.9.dp.toPx()),
            )
            val angle = progress * PI * 2.0
            drawCircle(
                color = palette.secondary.copy(alpha = alpha * 0.72f),
                radius = 2.dp.toPx(),
                center = Offset(
                    center.x + cos(angle).toFloat() * orbitWidth / 2f,
                    center.y + sin(angle).toFloat() * orbitHeight / 2f,
                ),
            )
        }
        UiNavigationDecoration.MinimalLine -> {
            drawLine(
                color = palette.primary.copy(alpha = alpha * 0.46f),
                start = Offset(center.x - 34.dp.toPx(), center.y + 23.dp.toPx()),
                end = Offset(center.x + 34.dp.toPx(), center.y + 23.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawCardDecoration(
    style: UiCardDecoration,
    shape: Shape,
    palette: UiDecorationPalette,
    alpha: Float,
) {
    when (style) {
        UiCardDecoration.None -> Unit
        UiCardDecoration.Highlight -> {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                brush = Brush.linearGradient(
                    listOf(
                        palette.highlight.copy(alpha = alpha * 0.42f),
                        palette.primary.copy(alpha = alpha * 0.16f),
                        Color.Transparent,
                    )
                ),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        UiCardDecoration.Blossom -> drawBlossomCard(palette, alpha)
        UiCardDecoration.Lotus -> drawLotusCard(palette, alpha)
        UiCardDecoration.Maple -> drawMapleCard(palette, alpha)
        UiCardDecoration.Snow -> drawSnowCard(palette, alpha)
        UiCardDecoration.Circuit -> drawCircuitCard(palette, alpha)
    }
}

private fun DrawScope.drawBlossomCard(palette: UiDecorationPalette, alpha: Float) {
    val y = 7.dp.toPx().coerceAtMost(size.height * 0.12f)
    val path = Path().apply {
        moveTo(size.width * 0.56f, 0f)
        cubicTo(size.width * 0.68f, y, size.width * 0.80f, -y * 0.2f, size.width, y * 0.55f)
    }
    drawPath(
        path = path,
        color = palette.muted.copy(alpha = alpha * 0.40f),
        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
    )
    listOf(0.66f, 0.78f, 0.91f).forEachIndexed { index, x ->
        drawTinyFlower(
            center = Offset(size.width * x, y * (0.55f + index * 0.12f)),
            radius = (2.2f + index * 0.15f).dp.toPx(),
            color = lerp(palette.primary, Color(0xFFF1A8B8), 0.62f),
            alpha = alpha * 0.70f,
        )
    }
}

private fun DrawScope.drawLotusCard(palette: UiDecorationPalette, alpha: Float) {
    val waterY = 9.dp.toPx().coerceAtMost(size.height * 0.14f)
    repeat(2) { index ->
        drawLine(
            color = (if (index == 0) palette.primary else palette.secondary)
                .copy(alpha = alpha * (0.34f - index * 0.09f)),
            start = Offset(size.width * (0.06f + index * 0.07f), waterY + index * 3.dp.toPx()),
            end = Offset(size.width * (0.70f - index * 0.04f), waterY + index * 3.dp.toPx()),
            strokeWidth = 0.8.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    drawOval(
        color = palette.secondary.copy(alpha = alpha * 0.50f),
        topLeft = Offset(size.width * 0.73f, 2.dp.toPx()),
        size = Size(16.dp.toPx(), 7.dp.toPx()),
    )
    drawTinyFlower(
        center = Offset(size.width * 0.88f, 6.dp.toPx()),
        radius = 2.5.dp.toPx(),
        color = lerp(palette.primary, Color(0xFFF3AEC0), 0.68f),
        alpha = alpha * 0.78f,
    )
}

private fun DrawScope.drawMapleCard(palette: UiDecorationPalette, alpha: Float) {
    val colors = listOf(Color(0xFFD58D38), Color(0xFFB9573D), Color(0xFF8B7D42))
    listOf(0.68f, 0.81f, 0.93f).forEachIndexed { index, x ->
        drawLeaf(
            center = Offset(size.width * x, (5 + index * 2).dp.toPx()),
            length = (7.dp + index.dp).toPx(),
            width = 2.8.dp.toPx(),
            color = lerp(colors[index], palette.primary, 0.16f).copy(alpha = alpha * 0.72f),
            rotationDegrees = -28f + index * 31f,
        )
    }
}

private fun DrawScope.drawSnowCard(palette: UiDecorationPalette, alpha: Float) {
    val height = 10.dp.toPx().coerceAtMost(size.height * 0.16f)
    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, height * 0.52f)
        cubicTo(size.width * 0.78f, height * 0.36f, size.width * 0.66f, height, size.width * 0.47f, height * 0.60f)
        cubicTo(size.width * 0.28f, height * 0.28f, size.width * 0.14f, height * 0.88f, 0f, height * 0.52f)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            listOf(
                palette.highlight.copy(alpha = alpha * 0.90f),
                lerp(palette.highlight, palette.primary, 0.16f).copy(alpha = alpha * 0.70f),
            ),
            endY = height,
        ),
    )
}

private fun DrawScope.drawCircuitCard(palette: UiDecorationPalette, alpha: Float) {
    val inset = 8.dp.toPx()
    val length = 28.dp.toPx().coerceAtMost(size.width * 0.18f)
    listOf(false, true).forEach { right ->
        val startX = if (right) size.width - inset else inset
        val direction = if (right) -1f else 1f
        val path = Path().apply {
            moveTo(startX, 0f)
            lineTo(startX, 5.dp.toPx())
            lineTo(startX + direction * length, 5.dp.toPx())
            lineTo(startX + direction * (length + 5.dp.toPx()), 10.dp.toPx())
        }
        drawPath(
            path = path,
            color = palette.primary.copy(alpha = alpha * 0.50f),
            style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(
            color = palette.secondary.copy(alpha = alpha * 0.70f),
            radius = 1.5.dp.toPx(),
            center = Offset(startX + direction * (length + 5.dp.toPx()), 10.dp.toPx()),
        )
    }
}

private fun DrawScope.drawTinyFlower(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    repeat(5) { index ->
        val angle = (index * 72f - 90f) * (PI / 180f)
        drawCircle(
            color = color,
            radius = radius * 0.56f,
            center = Offset(
                center.x + cos(angle).toFloat() * radius * 0.58f,
                center.y + sin(angle).toFloat() * radius * 0.58f,
            ),
            alpha = alpha,
        )
    }
    drawCircle(
        color = Color(0xFFF2CB5B),
        radius = radius * 0.32f,
        center = center,
        alpha = alpha,
    )
}

private fun DrawScope.drawStemWithLeaves(
    origin: Offset,
    direction: Offset,
    color: Color,
    rotationDegrees: Float,
) {
    rotate(rotationDegrees, origin) {
        val end = Offset(origin.x + direction.x, origin.y + direction.y)
        drawLine(color, origin, end, 1.dp.toPx(), StrokeCap.Round)
        repeat(4) { index ->
            val fraction = 0.22f + index * 0.19f
            val center = Offset(
                origin.x + direction.x * fraction,
                origin.y + direction.y * fraction,
            )
            drawLeaf(
                center = center,
                length = (9.dp + index.dp).toPx(),
                width = 3.5.dp.toPx(),
                color = color,
                rotationDegrees = if (index % 2 == 0) -38f else 38f,
            )
        }
    }
}

private fun DrawScope.drawLeaf(
    center: Offset,
    length: Float,
    width: Float,
    color: Color,
    rotationDegrees: Float,
) {
    rotate(rotationDegrees, center) {
        val path = Path().apply {
            moveTo(center.x - length / 2f, center.y)
            cubicTo(center.x - length * 0.18f, center.y - width, center.x + length * 0.24f, center.y - width, center.x + length / 2f, center.y)
            cubicTo(center.x + length * 0.20f, center.y + width, center.x - length * 0.22f, center.y + width, center.x - length / 2f, center.y)
            close()
        }
        drawPath(path, color)
    }
}

private val STAR_POINTS = listOf(
    0.08f to 0.12f, 0.18f to 0.26f, 0.31f to 0.16f, 0.43f to 0.34f,
    0.57f to 0.12f, 0.69f to 0.29f, 0.84f to 0.18f, 0.93f to 0.39f,
    0.12f to 0.58f, 0.27f to 0.72f, 0.40f to 0.55f, 0.54f to 0.78f,
    0.67f to 0.61f, 0.80f to 0.82f, 0.91f to 0.67f,
)

private val STAR_CONNECTIONS = listOf(
    0 to 1, 1 to 2, 2 to 3, 4 to 5, 5 to 6, 6 to 7,
    8 to 9, 9 to 10, 10 to 11, 11 to 12, 12 to 13, 13 to 14,
)

private val FROST_POINTS = listOf(
    0.03f to 0.10f, 0.06f to 0.31f, 0.02f to 0.62f, 0.08f to 0.88f,
    0.96f to 0.18f, 0.92f to 0.42f, 0.98f to 0.71f, 0.93f to 0.91f,
)

private val SEASONAL_CARD_DECORATIONS = setOf(
    UiCardDecoration.Blossom,
    UiCardDecoration.Lotus,
    UiCardDecoration.Maple,
    UiCardDecoration.Snow,
)
