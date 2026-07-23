package me.weishu.kernelsu.ui.component.snow

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import kotlin.math.max

private data class SeasonChromePalette(
    val container: Color,
    val content: Color,
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val outline: Color,
)

@Composable
fun SeasonChromeOverlay(modifier: Modifier = Modifier) {
    if (!isSnowInterfaceStyle()) return
    val season = LocalSeasonStyle.current
    val palette = seasonChromePalette(season, isInDarkTheme())
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Canvas(modifier = modifier.fillMaxSize()) {
        val top = max(8.dp.toPx(), statusBarPadding.toPx() + 5.dp.toPx())
        val inset = 16.dp.toPx()
        val unit = 2.dp.toPx()
        when (season) {
            SeasonStyle.Spring -> drawSpringTopChrome(top, inset, unit, palette)
            SeasonStyle.Summer -> drawSummerTopChrome(top, inset, unit, palette)
            SeasonStyle.Autumn -> drawAutumnTopChrome(top, inset, unit, palette)
            SeasonStyle.Winter -> drawWinterTopChrome(top, inset, unit, palette)
        }
    }
}

@Composable
fun SeasonMotto(modifier: Modifier = Modifier) {
    if (!isSnowInterfaceStyle()) return
    val season = LocalSeasonStyle.current
    val palette = seasonChromePalette(season, isInDarkTheme())
    val mottoRes = when (season) {
        SeasonStyle.Spring -> R.string.season_motto_spring
        SeasonStyle.Summer -> R.string.season_motto_summer
        SeasonStyle.Autumn -> R.string.season_motto_autumn
        SeasonStyle.Winter -> R.string.season_motto_winter
    }
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(palette.container.copy(alpha = 0.88f), shape)
            .border(1.dp, palette.outline.copy(alpha = 0.72f), shape)
            .drawWithContent {
                drawContent()
                drawSeasonMottoDetails(season, palette, 2.dp.toPx())
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(mottoRes),
            modifier = Modifier.padding(horizontal = 32.dp),
            color = palette.content,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun seasonNavigationContainerColor(): Color {
    return seasonChromePalette(LocalSeasonStyle.current, isInDarkTheme()).container
}

@Composable
fun Modifier.seasonNavigationSurface(
    shape: Shape,
    paintBackground: Boolean = true,
): Modifier {
    if (!isSnowInterfaceStyle()) return this
    val season = LocalSeasonStyle.current
    val palette = seasonChromePalette(season, isInDarkTheme())
    return clip(shape)
        .then(if (paintBackground) Modifier.background(palette.container, shape) else Modifier)
        .border(1.dp, palette.outline.copy(alpha = 0.78f), shape)
        .drawWithContent {
            val unit = 2.dp.toPx().coerceAtMost(size.minDimension / 12f)
            if (unit > 0f) drawSeasonNavigationAtmosphere(palette, unit)
            drawContent()
            if (unit > 0f) drawSeasonNavigationFrame(season, palette, unit)
        }
}

@Composable
fun Modifier.seasonNavigationIndicator(
    shape: Shape,
    paintBackground: Boolean = true,
): Modifier {
    if (!isSnowInterfaceStyle()) return this
    val season = LocalSeasonStyle.current
    val palette = seasonChromePalette(season, isInDarkTheme())
    return clip(shape)
        .then(
            if (paintBackground) {
                Modifier.background(palette.primary.copy(alpha = 0.16f), shape)
            } else {
                Modifier
            },
        )
        .border(1.dp, palette.primary.copy(alpha = 0.58f), shape)
        .drawWithContent {
            val unit = 1.8.dp.toPx().coerceAtMost(size.minDimension / 12f)
            if (unit > 0f) drawSeasonIndicatorAtmosphere(palette, unit)
            drawContent()
            if (unit > 0f) drawSeasonIndicatorDetail(season, palette, unit)
        }
}

@Composable
@ReadOnlyComposable
fun seasonTopBarContentColor(default: Color): Color {
    if (!isSnowInterfaceStyle()) return default
    return seasonChromePalette(LocalSeasonStyle.current, isInDarkTheme()).content
}

private fun seasonChromePalette(season: SeasonStyle, dark: Boolean): SeasonChromePalette {
    return when (season) {
        SeasonStyle.Spring -> SeasonChromePalette(
            container = if (dark) Color(0xE61A3020) else Color(0xEAF3F8EC),
            content = if (dark) Color(0xFFE0F3D5) else Color(0xFF203D26),
            primary = Color(0xFF6B9D55),
            secondary = Color(0xFFF0AFC1),
            highlight = Color(0xFFF4D45F),
            outline = if (dark) Color(0xFF86B873) else Color(0xFF8EB67A),
        )

        SeasonStyle.Summer -> SeasonChromePalette(
            container = if (dark) Color(0xE6103034) else Color(0xEAF0F9F7),
            content = if (dark) Color(0xFFD9F4EF) else Color(0xFF153F42),
            primary = Color(0xFF4EB6B1),
            secondary = Color(0xFF69A45F),
            highlight = Color(0xFFF0A9BC),
            outline = if (dark) Color(0xFF65C4C0) else Color(0xFF58A9A5),
        )

        SeasonStyle.Autumn -> SeasonChromePalette(
            container = if (dark) Color(0xE6382D24) else Color(0xEAFBF4E8),
            content = if (dark) Color(0xFFFFE3BD) else Color(0xFF53361F),
            primary = Color(0xFFC17A3D),
            secondary = Color(0xFFB6503A),
            highlight = Color(0xFFD9B66E),
            outline = if (dark) Color(0xFFD29A63) else Color(0xFFB68152),
        )

        SeasonStyle.Winter -> SeasonChromePalette(
            container = if (dark) Color(0xE6132C35) else Color(0xEAF4FAFC),
            content = if (dark) Color(0xFFE1F5FA) else Color(0xFF173D48),
            primary = Color(0xFF8CCAD5),
            secondary = Color(0xFFBBDDE5),
            highlight = Color.White,
            outline = if (dark) Color(0xFF8FC5D0) else Color(0xFF8DBBC5),
        )
    }
}

private fun DrawScope.drawSpringTopChrome(
    top: Float,
    inset: Float,
    unit: Float,
    palette: SeasonChromePalette,
) {
    val branch = Path().apply {
        moveTo(inset, top + unit * 4f)
        cubicTo(inset + unit * 10f, top + unit * 3.4f, inset + unit * 16f, top, inset + unit * 27f, top + unit)
    }
    drawPath(branch, palette.primary.copy(alpha = 0.70f), style = Stroke(unit * 0.55f))
    listOf(8f to 2.8f, 16f to 1.1f, 24f to 1.3f).forEachIndexed { index, (x, y) ->
        val center = Offset(inset + unit * x, top + unit * y)
        drawOval(
            color = palette.primary.copy(alpha = 0.66f),
            topLeft = center - Offset(unit * 1.5f, unit * 0.65f),
            size = Size(unit * 3f, unit * 1.3f),
        )
        if (index == 1) drawChromeFlower(center + Offset(unit * 1.2f, -unit), unit, palette)
    }
    drawChromeFlower(Offset(size.width - inset - unit * 4f, top + unit * 2f), unit * 1.05f, palette)
}

private fun DrawScope.drawSummerTopChrome(
    top: Float,
    inset: Float,
    unit: Float,
    palette: SeasonChromePalette,
) {
    listOf(0f, unit * 1.7f).forEachIndexed { index, y ->
        drawLine(
            color = palette.primary.copy(alpha = 0.66f - index * 0.14f),
            start = Offset(inset + unit * index * 5f, top + y),
            end = Offset(size.width * (0.34f - index * 0.03f), top + y),
            strokeWidth = unit * 0.52f,
        )
    }
    val leafCenter = Offset(size.width - inset - unit * 8f, top + unit * 2.2f)
    drawOval(
        color = palette.secondary.copy(alpha = 0.78f),
        topLeft = leafCenter - Offset(unit * 3.2f, unit * 1.35f),
        size = Size(unit * 6.4f, unit * 2.7f),
    )
    drawLine(palette.highlight.copy(alpha = 0.62f), leafCenter, leafCenter + Offset(unit * 3f, 0f), unit * 0.34f)
    drawChromeLotus(leafCenter - Offset(unit * 5f, unit * 0.8f), unit, palette)
}

private fun DrawScope.drawAutumnTopChrome(
    top: Float,
    inset: Float,
    unit: Float,
    palette: SeasonChromePalette,
) {
    listOf(0.08f to 0.15f, 0.27f to 0.10f).forEachIndexed { index, (x, width) ->
        drawRect(
            color = if (index == 0) palette.highlight.copy(alpha = 0.62f) else palette.primary.copy(alpha = 0.68f),
            topLeft = Offset(size.width * x, top + unit * index),
            size = Size(size.width * width, unit * 0.55f),
        )
    }
    repeat(3) { index ->
        val x = size.width - inset - unit * (17f - index * 5f)
        drawLine(
            color = palette.outline.copy(alpha = 0.42f),
            start = Offset(x, top),
            end = Offset(x - unit, top + unit * (3.8f + index * 0.8f)),
            strokeWidth = unit * 0.34f,
        )
    }
    drawChromeLeaf(
        center = Offset(size.width - inset - unit * 5f, top + unit * 2f),
        length = unit * 4.4f,
        width = unit * 1.5f,
        color = palette.secondary.copy(alpha = 0.80f),
        rotation = -24f,
    )
}

private fun DrawScope.drawWinterTopChrome(
    top: Float,
    inset: Float,
    unit: Float,
    palette: SeasonChromePalette,
) {
    drawLine(
        color = palette.highlight.copy(alpha = 0.72f),
        start = Offset(inset, top),
        end = Offset(size.width * 0.30f, top),
        strokeWidth = unit * 0.65f,
    )
    listOf(5f, 12f, 21f).forEachIndexed { index, x ->
        drawLine(
            color = palette.primary.copy(alpha = 0.70f),
            start = Offset(inset + unit * x, top),
            end = Offset(inset + unit * x, top + unit * (2.2f + index % 2 * 1.3f)),
            strokeWidth = unit * 0.45f,
        )
    }
    drawChromeFrostCrystal(
        center = Offset(size.width - inset - unit * 4f, top + unit * 2f),
        radius = unit * 2.4f,
        color = palette.highlight.copy(alpha = 0.78f),
    )
}

private fun DrawScope.drawSeasonMottoDetails(
    season: SeasonStyle,
    palette: SeasonChromePalette,
    unit: Float,
) {
    when (season) {
        SeasonStyle.Spring -> {
            drawChromeFlower(Offset(unit * 6f, size.height / 2f), unit * 0.82f, palette)
            drawChromeFlower(Offset(size.width - unit * 6f, size.height / 2f), unit * 0.82f, palette)
        }

        SeasonStyle.Summer -> {
            drawLine(palette.primary, Offset(unit * 2f, size.height * 0.64f), Offset(unit * 11f, size.height * 0.64f), unit * 0.36f)
            drawLine(
                palette.primary,
                Offset(size.width - unit * 11f, size.height * 0.64f),
                Offset(size.width - unit * 2f, size.height * 0.64f),
                unit * 0.36f,
            )
            drawChromeLotus(Offset(size.width - unit * 6f, size.height * 0.45f), unit * 0.72f, palette)
        }

        SeasonStyle.Autumn -> {
            drawChromeLeaf(Offset(unit * 6f, size.height / 2f), unit * 4f, unit * 1.3f, palette.primary, -24f)
            drawChromeLeaf(
                Offset(size.width - unit * 6f, size.height / 2f),
                unit * 4f,
                unit * 1.3f,
                palette.secondary,
                28f,
            )
        }

        SeasonStyle.Winter -> {
            drawChromeFrostCrystal(Offset(unit * 6f, size.height / 2f), unit * 2f, palette.highlight)
            drawChromeFrostCrystal(Offset(size.width - unit * 6f, size.height / 2f), unit * 2f, palette.highlight)
        }
    }
}

private fun DrawScope.drawSeasonNavigationFrame(
    season: SeasonStyle,
    palette: SeasonChromePalette,
    unit: Float,
) {
    if (size.width >= size.height) {
        drawHorizontalSeasonNavigationFrame(season, palette, unit)
    } else {
        drawVerticalSeasonNavigationFrame(season, palette, unit)
    }
}

private fun DrawScope.drawSeasonNavigationAtmosphere(
    palette: SeasonChromePalette,
    unit: Float,
) {
    if (size.width >= size.height) {
        drawRect(
            color = palette.highlight.copy(alpha = 0.055f),
            topLeft = Offset(size.width * 0.12f, unit * 0.72f),
            size = Size(size.width * 0.50f, unit * 0.34f),
        )
        drawRect(
            color = palette.primary.copy(alpha = 0.075f),
            topLeft = Offset(size.width * 0.30f, size.height - unit * 1.08f),
            size = Size(size.width * 0.52f, unit * 0.38f),
        )
    } else {
        drawRect(
            color = palette.highlight.copy(alpha = 0.055f),
            topLeft = Offset(unit * 0.72f, size.height * 0.12f),
            size = Size(unit * 0.34f, size.height * 0.48f),
        )
        drawRect(
            color = palette.primary.copy(alpha = 0.075f),
            topLeft = Offset(size.width - unit * 1.08f, size.height * 0.34f),
            size = Size(unit * 0.38f, size.height * 0.48f),
        )
    }
}

private fun DrawScope.drawSeasonIndicatorAtmosphere(
    palette: SeasonChromePalette,
    unit: Float,
) {
    drawRect(
        color = palette.highlight.copy(alpha = 0.075f),
        topLeft = Offset(unit * 1.4f, unit * 0.75f),
        size = Size((size.width - unit * 2.8f).coerceAtLeast(0f), unit * 0.42f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.10f),
        topLeft = Offset(size.width * 0.28f, size.height - unit * 1.15f),
        size = Size(size.width * 0.44f, unit * 0.42f),
    )
}

private fun DrawScope.drawHorizontalSeasonNavigationFrame(
    season: SeasonStyle,
    palette: SeasonChromePalette,
    unit: Float,
) {
    val bottom = size.height - unit * 1.3f
    when (season) {
        SeasonStyle.Spring -> {
            drawLine(palette.primary.copy(alpha = 0.62f), Offset(unit * 5f, bottom), Offset(size.width - unit * 5f, bottom), unit * 0.42f)
            repeat(7) { index ->
                val x = size.width * (0.12f + index * 0.125f)
                drawLine(
                    palette.primary.copy(alpha = 0.60f),
                    Offset(x, bottom),
                    Offset(x + if (index % 2 == 0) -unit else unit, bottom - unit * (1.4f + index % 3 * 0.55f)),
                    unit * 0.34f,
                )
            }
            drawChromeFlower(Offset(size.width * 0.88f, bottom - unit), unit * 0.72f, palette)
        }

        SeasonStyle.Summer -> {
            repeat(2) { index ->
                val y = bottom - index * unit * 1.45f
                drawLine(
                    palette.primary.copy(alpha = 0.60f - index * 0.12f),
                    Offset(size.width * (0.08f + index * 0.08f), y),
                    Offset(size.width * (0.50f - index * 0.03f), y),
                    unit * 0.42f,
                )
            }
            val center = Offset(size.width * 0.84f, bottom - unit * 0.8f)
            drawOval(
                palette.secondary.copy(alpha = 0.74f),
                center - Offset(unit * 3f, unit * 1.2f),
                Size(unit * 6f, unit * 2.4f),
            )
            drawChromeLotus(center - Offset(unit * 4f, unit * 0.5f), unit * 0.72f, palette)
        }

        SeasonStyle.Autumn -> {
            listOf(0.07f to 0.22f, 0.36f to 0.16f, 0.61f to 0.31f).forEachIndexed { index, (x, width) ->
                drawRect(
                    color = if (index == 1) palette.secondary.copy(alpha = 0.70f) else palette.primary.copy(alpha = 0.62f),
                    topLeft = Offset(size.width * x, unit * (0.5f + index % 2 * 0.6f)),
                    size = Size(size.width * width, unit * 0.48f),
                )
            }
            drawChromeLeaf(Offset(size.width * 0.12f, bottom - unit), unit * 4f, unit * 1.3f, palette.primary, -20f)
            drawChromeLeaf(Offset(size.width * 0.88f, bottom - unit), unit * 4f, unit * 1.3f, palette.secondary, 24f)
        }

        SeasonStyle.Winter -> {
            repeat(9) { index ->
                drawCircle(
                    color = if (index % 2 == 0) palette.highlight.copy(alpha = 0.76f) else palette.secondary.copy(alpha = 0.72f),
                    radius = unit * (0.8f + index % 3 * 0.18f),
                    center = Offset(size.width * (0.06f + index * 0.11f), unit * 0.6f),
                )
            }
            listOf(0.18f, 0.49f, 0.82f).forEachIndexed { index, x ->
                drawLine(
                    palette.primary.copy(alpha = 0.64f),
                    Offset(size.width * x, unit * 0.5f),
                    Offset(size.width * x, unit * (2.1f + index % 2)),
                    unit * 0.40f,
                )
            }
        }
    }
}

private fun DrawScope.drawVerticalSeasonNavigationFrame(
    season: SeasonStyle,
    palette: SeasonChromePalette,
    unit: Float,
) {
    val x = size.width - unit * 1.4f
    when (season) {
        SeasonStyle.Spring -> {
            val vine = Path().apply {
                moveTo(x, size.height * 0.16f)
                cubicTo(x - unit * 3f, size.height * 0.35f, x + unit, size.height * 0.58f, x - unit * 2f, size.height * 0.82f)
            }
            drawPath(vine, palette.primary.copy(alpha = 0.60f), style = Stroke(unit * 0.40f))
            drawChromeFlower(Offset(x - unit, size.height * 0.22f), unit * 0.72f, palette)
        }

        SeasonStyle.Summer -> repeat(4) { index ->
            drawCircle(
                color = palette.primary.copy(alpha = 0.54f),
                radius = unit * (0.55f + index * 0.16f),
                center = Offset(x - unit * (1f + index % 2), size.height * (0.24f + index * 0.15f)),
                style = Stroke(unit * 0.28f),
            )
        }

        SeasonStyle.Autumn -> {
            repeat(4) { index ->
                val y = size.height * (0.16f + index * 0.19f)
                drawLine(palette.outline.copy(alpha = 0.42f), Offset(x - unit, y), Offset(x - unit * 3f, y + unit * 4f), unit * 0.30f)
            }
            drawChromeLeaf(Offset(x - unit * 2f, size.height * 0.82f), unit * 4f, unit * 1.3f, palette.secondary, 70f)
        }

        SeasonStyle.Winter -> {
            drawChromeFrostCrystal(Offset(x - unit * 1.4f, size.height * 0.18f), unit * 2.2f, palette.highlight)
            drawChromeFrostCrystal(Offset(x - unit * 1.4f, size.height * 0.82f), unit * 1.8f, palette.secondary)
        }
    }
}

private fun DrawScope.drawSeasonIndicatorDetail(
    season: SeasonStyle,
    palette: SeasonChromePalette,
    unit: Float,
) {
    val center = Offset(size.width - unit * 4.5f, size.height - unit * 3f)
    when (season) {
        SeasonStyle.Spring -> drawChromeFlower(center, unit * 0.68f, palette)
        SeasonStyle.Summer -> {
            drawLine(
                color = palette.primary.copy(alpha = 0.68f),
                start = Offset(unit * 2f, size.height - unit * 1.5f),
                end = Offset(size.width - unit * 2f, size.height - unit * 1.5f),
                strokeWidth = unit * 0.34f,
            )
            drawOval(
                color = palette.secondary.copy(alpha = 0.72f),
                topLeft = center - Offset(unit * 1.7f, unit * 0.7f),
                size = Size(unit * 3.4f, unit * 1.4f),
            )
        }

        SeasonStyle.Autumn -> drawChromeLeaf(center, unit * 3.5f, unit * 1.2f, palette.secondary, -22f)
        SeasonStyle.Winter -> drawChromeFrostCrystal(center, unit * 1.7f, palette.highlight.copy(alpha = 0.80f))
    }
}

private fun DrawScope.drawChromeFlower(center: Offset, unit: Float, palette: SeasonChromePalette) {
    listOf(Offset(-unit, 0f), Offset(unit, 0f), Offset(0f, -unit), Offset(0f, unit)).forEach { offset ->
        drawCircle(palette.secondary.copy(alpha = 0.86f), unit * 0.58f, center + offset)
    }
    drawCircle(palette.highlight.copy(alpha = 0.92f), unit * 0.50f, center)
}

private fun DrawScope.drawChromeLotus(center: Offset, unit: Float, palette: SeasonChromePalette) {
    listOf(-28f, 0f, 28f).forEach { angle ->
        rotate(angle, center) {
            drawOval(
                color = palette.highlight.copy(alpha = 0.82f),
                topLeft = center - Offset(unit * 0.62f, unit * 1.55f),
                size = Size(unit * 1.24f, unit * 2f),
            )
        }
    }
    drawCircle(palette.secondary.copy(alpha = 0.88f), unit * 0.45f, center + Offset(0f, unit * 0.35f))
}

private fun DrawScope.drawChromeLeaf(
    center: Offset,
    length: Float,
    width: Float,
    color: Color,
    rotation: Float,
) {
    rotate(rotation, center) {
        val leaf = Path().apply {
            moveTo(center.x - length / 2f, center.y)
            cubicTo(
                center.x - length * 0.15f,
                center.y - width,
                center.x + length * 0.18f,
                center.y - width,
                center.x + length / 2f,
                center.y,
            )
            cubicTo(
                center.x + length * 0.18f,
                center.y + width,
                center.x - length * 0.18f,
                center.y + width,
                center.x - length / 2f,
                center.y,
            )
            close()
        }
        drawPath(leaf, color.copy(alpha = 0.82f))
        drawLine(
            color = color.copy(alpha = 0.62f),
            start = Offset(center.x - length * 0.35f, center.y),
            end = Offset(center.x + length * 0.35f, center.y),
            strokeWidth = width * 0.16f,
        )
    }
}

private fun DrawScope.drawChromeFrostCrystal(center: Offset, radius: Float, color: Color) {
    repeat(3) { index ->
        rotate(index * 60f, center) {
            drawLine(color, center - Offset(radius, 0f), center + Offset(radius, 0f), radius * 0.10f)
        }
    }
}
