package me.weishu.kernelsu.ui.component.snow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawSeasonCardAtmosphere(season: SeasonStyle, dark: Boolean) {
    val minimumWidth = 72.dp.toPx()
    val minimumHeight = 48.dp.toPx()
    if (!seasonCardFramePolishEnabled(size.width, size.height, minimumWidth, minimumHeight)) return
    val palette = seasonFramePalette(season, dark)
    val upperBandHeight = size.height * 0.24f
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                palette.highlight.copy(alpha = palette.highlight.alpha * 0.12f),
                palette.primary.copy(alpha = palette.primary.alpha * 0.08f),
                Color.Transparent,
            ),
        ),
        topLeft = Offset(0f, size.height * 0.10f),
        size = Size(size.width, upperBandHeight),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                palette.shadow.copy(alpha = palette.shadow.alpha * if (dark) 0.14f else 0.09f),
            ),
            startY = size.height * 0.66f,
            endY = size.height,
        ),
        topLeft = Offset(0f, size.height * 0.58f),
        size = Size(size.width, size.height * 0.42f),
    )
}

internal fun DrawScope.drawSeasonCardFramePolish(season: SeasonStyle, dark: Boolean) {
    val minimumWidth = 72.dp.toPx()
    val minimumHeight = 48.dp.toPx()
    if (!seasonCardFramePolishEnabled(size.width, size.height, minimumWidth, minimumHeight)) return
    val unit = 2.dp.toPx().coerceAtMost(size.minDimension / 18f)
    val palette = seasonFramePalette(season, dark)
    val inset = unit * 0.9f
    drawRect(
        color = palette.edge,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(width = unit * 0.34f),
    )
    drawSeasonCardDepthFrame(unit, palette)
    when (season) {
        SeasonStyle.Spring -> drawSpringFramePolish(unit, palette)
        SeasonStyle.Summer -> drawSummerFramePolish(unit, palette)
        SeasonStyle.Autumn -> drawAutumnFramePolish(unit, palette)
        SeasonStyle.Winter -> drawWinterFramePolish(unit, palette)
    }
}

private fun DrawScope.drawSeasonCardDepthFrame(unit: Float, palette: SeasonFramePalette) {
    val inset = unit * 1.15f
    drawLine(
        color = palette.highlight.copy(alpha = palette.highlight.alpha * 0.54f),
        start = Offset(size.width * 0.16f, inset),
        end = Offset(size.width * 0.62f, inset),
        strokeWidth = unit * 0.28f,
    )
    drawLine(
        color = palette.edge.copy(alpha = palette.edge.alpha * 0.58f),
        start = Offset(inset, size.height * 0.24f),
        end = Offset(inset, size.height * 0.58f),
        strokeWidth = unit * 0.26f,
    )
    drawLine(
        color = palette.shadow.copy(alpha = palette.shadow.alpha * 0.60f),
        start = Offset(size.width * 0.31f, size.height - inset),
        end = Offset(size.width * 0.78f, size.height - inset),
        strokeWidth = unit * 0.30f,
    )
    drawLine(
        color = palette.shadow.copy(alpha = palette.shadow.alpha * 0.46f),
        start = Offset(size.width - inset, size.height * 0.42f),
        end = Offset(size.width - inset, size.height * 0.72f),
        strokeWidth = unit * 0.26f,
    )
}

internal fun seasonCardFramePolishEnabled(
    width: Float,
    height: Float,
    minimumWidth: Float,
    minimumHeight: Float,
): Boolean {
    return width >= minimumWidth && height >= minimumHeight && minimumWidth > 0f && minimumHeight > 0f
}

internal fun seasonCardDecorationHeight(
    requestedHeight: Float,
    width: Float,
    height: Float,
    minimumWidth: Float,
    minimumHeight: Float,
): Float {
    if (
        requestedHeight <= 0f ||
        !seasonCardFramePolishEnabled(width, height, minimumWidth, minimumHeight)
    ) {
        return 0f
    }
    return minOf(requestedHeight, height * 0.16f)
}

private data class SeasonFramePalette(
    val edge: Color,
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val shadow: Color,
)

private fun seasonFramePalette(season: SeasonStyle, dark: Boolean): SeasonFramePalette = when (season) {
    SeasonStyle.Spring -> SeasonFramePalette(
        edge = Color(0xFFB9DFA0).copy(alpha = if (dark) 0.36f else 0.58f),
        primary = Color(0xFF6E9F58).copy(alpha = if (dark) 0.50f else 0.62f),
        secondary = Color(0xFFF1AEC0).copy(alpha = if (dark) 0.62f else 0.76f),
        highlight = Color(0xFFF3D665).copy(alpha = if (dark) 0.66f else 0.82f),
        shadow = Color(0xFF315336).copy(alpha = if (dark) 0.48f else 0.36f),
    )

    SeasonStyle.Summer -> SeasonFramePalette(
        edge = Color(0xFF9FE1DB).copy(alpha = if (dark) 0.38f else 0.62f),
        primary = Color(0xFF4EB8B4).copy(alpha = if (dark) 0.52f else 0.66f),
        secondary = Color(0xFF65A462).copy(alpha = if (dark) 0.54f else 0.68f),
        highlight = Color(0xFFF2A8BB).copy(alpha = if (dark) 0.68f else 0.82f),
        shadow = Color(0xFF1D5960).copy(alpha = if (dark) 0.52f else 0.40f),
    )

    SeasonStyle.Autumn -> SeasonFramePalette(
        edge = Color(0xFFE8C18E).copy(alpha = if (dark) 0.40f else 0.64f),
        primary = Color(0xFFC27B3D).copy(alpha = if (dark) 0.56f else 0.68f),
        secondary = Color(0xFFB64F3A).copy(alpha = if (dark) 0.60f else 0.72f),
        highlight = Color(0xFFD9B66F).copy(alpha = if (dark) 0.66f else 0.80f),
        shadow = Color(0xFF65442E).copy(alpha = if (dark) 0.54f else 0.42f),
    )

    SeasonStyle.Winter -> SeasonFramePalette(
        edge = Color(0xFFDDF6FA).copy(alpha = if (dark) 0.42f else 0.72f),
        primary = Color(0xFF8DCBD5).copy(alpha = if (dark) 0.54f else 0.68f),
        secondary = Color(0xFFBBDDE5).copy(alpha = if (dark) 0.58f else 0.76f),
        highlight = Color.White.copy(alpha = if (dark) 0.72f else 0.92f),
        shadow = Color(0xFF4D7D8B).copy(alpha = if (dark) 0.48f else 0.38f),
    )
}

private fun DrawScope.drawSpringFramePolish(unit: Float, palette: SeasonFramePalette) {
    val branch = Path().apply {
        moveTo(unit * 1.4f, size.height * 0.66f)
        cubicTo(
            size.width * 0.03f,
            size.height * 0.43f,
            size.width * 0.09f,
            size.height * 0.30f,
            size.width * 0.18f,
            unit * 1.8f,
        )
    }
    drawPath(branch, palette.shadow, style = Stroke(width = unit * 0.42f, cap = StrokeCap.Round))
    listOf(0.06f to 0.49f, 0.10f to 0.34f, 0.15f to 0.18f).forEachIndexed { index, (x, y) ->
        val center = Offset(size.width * x, size.height * y)
        drawOval(
            color = palette.primary,
            topLeft = center - Offset(unit * 1.8f, unit * 0.8f),
            size = Size(unit * 3.6f, unit * 1.6f),
        )
        if (index != 1) drawSeasonTinyFlower(center + Offset(unit, -unit), unit, palette.secondary, palette.highlight)
    }
    val meadowY = size.height - unit * 1.7f
    drawLine(palette.shadow, Offset(unit * 4f, meadowY), Offset(size.width - unit * 4f, meadowY), unit * 0.35f)
    repeat(9) { index ->
        val bladeHeight = unit * (1.1f + index % 3 * 0.55f)
        val x = size.width * (0.08f + index * 0.105f)
        drawLine(
            color = palette.primary,
            start = Offset(x, meadowY),
            end = Offset(x + if (index % 2 == 0) -unit * 0.6f else unit * 0.6f, meadowY - bladeHeight),
            strokeWidth = unit * 0.32f,
            cap = StrokeCap.Round,
        )
    }
    drawSeasonTinyFlower(Offset(size.width * 0.78f, meadowY - unit), unit * 0.78f, palette.highlight, palette.secondary)
}

private fun DrawScope.drawSummerFramePolish(unit: Float, palette: SeasonFramePalette) {
    listOf(unit * 1.2f, unit * 2.1f).forEachIndexed { index, y ->
        drawLine(
            color = if (index == 0) palette.primary else palette.edge,
            start = Offset(size.width * (0.08f + index * 0.05f), y),
            end = Offset(size.width * (0.92f - index * 0.05f), y),
            strokeWidth = unit * (0.38f - index * 0.08f),
            cap = StrokeCap.Round,
        )
    }
    val waterY = size.height - unit * 1.8f
    repeat(3) { index ->
        val start = size.width * (0.06f + index * 0.17f)
        drawLine(
            color = palette.primary.copy(alpha = palette.primary.alpha * (0.68f + index * 0.10f)),
            start = Offset(start, waterY - index % 2 * unit * 0.65f),
            end = Offset(start + size.width * (0.24f + index * 0.04f), waterY - index % 2 * unit * 0.65f),
            strokeWidth = unit * 0.35f,
            cap = StrokeCap.Round,
        )
    }
    val leafCenter = Offset(unit * 5.5f, size.height - unit * 4f)
    drawOval(
        color = palette.secondary,
        topLeft = leafCenter - Offset(unit * 3f, unit * 1.4f),
        size = Size(unit * 6f, unit * 2.8f),
    )
    drawLine(
        palette.edge,
        leafCenter,
        leafCenter + Offset(unit * 2.7f, 0f),
        unit * 0.28f,
        cap = StrokeCap.Round,
    )
    drawSeasonLotus(
        center = Offset(size.width - unit * 5f, size.height - unit * 3.4f),
        unit = unit,
        petal = palette.highlight,
        core = palette.secondary,
    )
    repeat(4) { index ->
        drawCircle(
            color = palette.edge.copy(alpha = palette.edge.alpha * 0.78f),
            radius = unit * (0.28f + index * 0.08f),
            center = Offset(size.width - unit * (3f + index * 1.5f), unit * (3f + index % 2)),
            style = Stroke(width = unit * 0.22f),
        )
    }
}

private fun DrawScope.drawAutumnFramePolish(unit: Float, palette: SeasonFramePalette) {
    val topSegments = listOf(0.06f to 0.27f, 0.38f to 0.19f, 0.63f to 0.31f)
    topSegments.forEachIndexed { index, (x, width) ->
        drawRect(
            color = if (index == 1) palette.highlight else palette.primary,
            topLeft = Offset(size.width * x, unit * (0.9f + index % 2 * 0.65f)),
            size = Size(size.width * width, unit * 0.5f),
        )
    }
    listOf(0.08f, 0.28f, 0.72f, 0.91f).forEachIndexed { index, x ->
        val y = size.height * (0.29f + index % 2 * 0.22f)
        drawLine(
            color = palette.edge.copy(alpha = palette.edge.alpha * 0.60f),
            start = Offset(size.width * x, y),
            end = Offset(size.width * x - unit * 1.2f, y + unit * 3f),
            strokeWidth = unit * 0.28f,
            cap = StrokeCap.Round,
        )
    }
    val bottomY = size.height - unit * 1.4f
    drawLine(palette.shadow, Offset(unit * 3f, bottomY), Offset(size.width - unit * 3f, bottomY), unit * 0.4f)
    listOf(
        Triple(0.13f, -18f, palette.primary),
        Triple(0.47f, 24f, palette.secondary),
        Triple(0.82f, -38f, palette.highlight),
    ).forEach { (x, rotation, color) ->
        drawSeasonLeaf(Offset(size.width * x, bottomY - unit * 1.4f), unit * 2.7f, unit, color, rotation)
    }
}

private fun DrawScope.drawWinterFramePolish(unit: Float, palette: SeasonFramePalette) {
    val inset = unit * 1.7f
    drawRect(
        color = palette.primary.copy(alpha = palette.primary.alpha * 0.58f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(width = unit * 0.30f),
    )
    listOf(
        Offset(unit * 3f, size.height * 0.58f),
        Offset(size.width - unit * 3.5f, size.height * 0.38f),
    ).forEachIndexed { index, center ->
        drawSeasonFrostCrystal(center, unit * (1.7f + index * 0.45f), palette.edge)
    }
    listOf(0.16f, 0.38f, 0.63f, 0.86f).forEachIndexed { index, x ->
        drawLine(
            color = palette.highlight.copy(alpha = palette.highlight.alpha * 0.72f),
            start = Offset(size.width * x, unit * 0.7f),
            end = Offset(size.width * x, unit * (2.1f + index % 2 * 0.8f)),
            strokeWidth = unit * 0.32f,
            cap = StrokeCap.Round,
        )
    }
    val driftY = size.height - unit * 1.7f
    repeat(5) { index ->
        drawCircle(
            color = if (index % 2 == 0) palette.highlight else palette.secondary,
            radius = unit * (1.1f + index % 3 * 0.25f),
            center = Offset(size.width * (0.10f + index * 0.20f), driftY),
        )
    }
}

private fun DrawScope.drawSeasonTinyFlower(
    center: Offset,
    unit: Float,
    petals: Color,
    core: Color,
) {
    listOf(Offset(-unit, 0f), Offset(unit, 0f), Offset(0f, -unit), Offset(0f, unit)).forEach { offset ->
        drawCircle(petals, unit * 0.58f, center + offset)
    }
    drawCircle(core, unit * 0.52f, center)
}

private fun DrawScope.drawSeasonLotus(center: Offset, unit: Float, petal: Color, core: Color) {
    listOf(-28f, 0f, 28f).forEach { angle ->
        rotate(angle, center) {
            drawOval(
                color = petal,
                topLeft = center - Offset(unit * 0.65f, unit * 1.8f),
                size = Size(unit * 1.3f, unit * 2.3f),
            )
        }
    }
    drawCircle(core, unit * 0.52f, center + Offset(0f, unit * 0.45f))
}

private fun DrawScope.drawSeasonLeaf(
    center: Offset,
    length: Float,
    width: Float,
    color: Color,
    rotation: Float,
) {
    rotate(rotation, center) {
        val path = Path().apply {
            moveTo(center.x - length / 2f, center.y)
            cubicTo(
                center.x - length * 0.15f,
                center.y - width,
                center.x + length * 0.20f,
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
        drawPath(path, color)
        drawLine(
            color = color.copy(alpha = color.alpha * 0.62f),
            start = Offset(center.x - length * 0.35f, center.y),
            end = Offset(center.x + length * 0.35f, center.y),
            strokeWidth = width * 0.18f,
        )
    }
}

private fun DrawScope.drawSeasonFrostCrystal(center: Offset, radius: Float, color: Color) {
    repeat(3) { index ->
        rotate(index * 60f, center) {
            drawLine(
                color = color,
                start = center - Offset(radius, 0f),
                end = center + Offset(radius, 0f),
                strokeWidth = radius * 0.10f,
                cap = StrokeCap.Round,
            )
        }
    }
}
