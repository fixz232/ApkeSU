package me.weishu.kernelsu.ui.component.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

internal val CYBER_MAGENTA = Color(0xFFFF3DAE)
private val CyberIce = Color(0xFF63D8FF)

internal fun DrawScope.drawCyberHackerScene(palette: PixelPalette, progress: Float) {
    val pixel = 1.dp.toPx()
    drawCyberCircuitBoard(palette, pixel)
    drawCyberDataColumns(palette, progress, pixel)
    drawCyberCodeWindow(palette, pixel)
    drawCyberServerRack(palette, Offset(size.width * 0.055f, size.height * 0.57f), pixel)
    drawCyberServerRack(palette, Offset(size.width * 0.78f, size.height * 0.51f), pixel)
    drawCyberChip(palette, Offset(size.width * 0.64f, size.height * 0.72f), pixel)
    drawCyberMask(palette, Offset(size.width * 0.46f, size.height * 0.78f), pixel)
    drawCyberGlitchAndScanline(palette, progress, pixel)
}

internal fun DrawScope.drawCyberCardMaterial(palette: PixelPalette, line: Float) {
    drawRect(
        color = palette.shadow.copy(alpha = 0.36f),
        topLeft = Offset.Zero,
        size = Size(size.width, line * 3f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.14f),
        topLeft = Offset(0f, 0f),
        size = Size(size.width * 0.58f, line),
    )
    drawRect(
        color = CYBER_MAGENTA.copy(alpha = 0.14f),
        topLeft = Offset(size.width * 0.61f, 0f),
        size = Size(size.width * 0.17f, line),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.16f),
        topLeft = Offset(size.width * 0.81f, 0f),
        size = Size(size.width * 0.11f, line),
    )
    var y = line * 5f
    while (y < size.height) {
        drawRect(
            color = CyberIce.copy(alpha = 0.022f),
            topLeft = Offset(line * 2f, y),
            size = Size((size.width - line * 4f).coerceAtLeast(0f), 1.dp.toPx()),
        )
        y += line * 4f
    }
    val traceY = size.height * 0.63f
    drawLine(
        color = palette.primary.copy(alpha = 0.08f),
        start = Offset(size.width * 0.07f, traceY),
        end = Offset(size.width * 0.32f, traceY),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Butt,
    )
    drawLine(
        color = palette.secondary.copy(alpha = 0.08f),
        start = Offset(size.width * 0.68f, traceY + line * 2f),
        end = Offset(size.width * 0.93f, traceY + line * 2f),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Butt,
    )
}

internal fun DrawScope.drawCyberHudAccent(palette: PixelPalette, top: Float, unit: Float) {
    val centerX = size.width / 2f
    drawRect(
        color = palette.shadow.copy(alpha = 0.90f),
        topLeft = Offset(centerX - unit * 7f, top - unit),
        size = Size(unit * 14f, unit * 3f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.92f),
        topLeft = Offset(centerX - unit * 6f, top - unit),
        size = Size(unit * 5f, unit),
    )
    drawRect(
        color = CyberIce.copy(alpha = 0.88f),
        topLeft = Offset(centerX + unit, top - unit),
        size = Size(unit * 3f, unit),
    )
    drawRect(
        color = CYBER_MAGENTA.copy(alpha = 0.92f),
        topLeft = Offset(centerX + unit * 5f, top - unit),
        size = Size(unit, unit),
    )
    repeat(5) { index ->
        drawRect(
            color = palette.secondary.copy(alpha = 0.48f + index * 0.08f),
            topLeft = Offset(centerX - unit * 5f + index * unit * 2f, top + unit),
            size = Size(unit, unit * 0.55f),
        )
    }
}

internal fun DrawScope.drawCyberNavigationAccent(palette: PixelPalette, unit: Float) {
    val centerX = size.width / 2f
    drawRect(
        color = palette.primary.copy(alpha = 0.92f),
        topLeft = Offset(size.width * 0.10f, 0f),
        size = Size(size.width * 0.24f, unit * 0.55f),
    )
    drawRect(
        color = CYBER_MAGENTA.copy(alpha = 0.90f),
        topLeft = Offset(size.width * 0.66f, 0f),
        size = Size(size.width * 0.24f, unit * 0.55f),
    )
    drawRect(
        color = CyberIce.copy(alpha = 0.90f),
        topLeft = Offset(centerX - unit * 2f, 0f),
        size = Size(unit * 4f, unit),
    )
    repeat(6) { index ->
        drawRect(
            color = palette.secondary.copy(alpha = 0.62f + index % 2 * 0.18f),
            topLeft = Offset(unit * (3f + index * 1.6f), size.height - unit * 2.2f),
            size = Size(unit * 0.7f, unit * 0.7f),
        )
    }
    repeat(6) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.54f + index % 2 * 0.16f),
            topLeft = Offset(size.width - unit * (4f + index * 1.6f), size.height - unit * 2.2f),
            size = Size(unit * 0.7f, unit * 0.7f),
        )
    }
}

internal fun DrawScope.drawCyberIndicatorAccent(palette: PixelPalette, unit: Float) {
    drawRect(
        color = palette.primary.copy(alpha = 0.94f),
        topLeft = Offset(unit * 1.5f, unit),
        size = Size(unit * 4f, unit),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.96f),
        topLeft = Offset(size.width - unit * 4.5f, unit),
        size = Size(unit * 2f, unit),
    )
    drawRect(
        color = CYBER_MAGENTA.copy(alpha = 0.92f),
        topLeft = Offset(size.width * 0.50f - unit * 0.5f, 0f),
        size = Size(unit, unit),
    )
}

private fun DrawScope.drawCyberCircuitBoard(palette: PixelPalette, pixel: Float) {
    val traces = listOf(
        listOf(0.04f to 0.16f, 0.22f to 0.16f, 0.22f to 0.28f, 0.38f to 0.28f),
        listOf(0.96f to 0.24f, 0.74f to 0.24f, 0.74f to 0.39f, 0.57f to 0.39f),
        listOf(0.07f to 0.72f, 0.24f to 0.72f, 0.24f to 0.61f, 0.41f to 0.61f),
        listOf(0.93f to 0.82f, 0.77f to 0.82f, 0.77f to 0.69f, 0.60f to 0.69f),
    )
    traces.forEachIndexed { traceIndex, points ->
        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = (if (traceIndex % 2 == 0) palette.primary else CyberIce).copy(alpha = 0.13f),
                start = Offset(size.width * start.first, size.height * start.second),
                end = Offset(size.width * end.first, size.height * end.second),
                strokeWidth = pixel,
                cap = StrokeCap.Butt,
            )
        }
        points.forEachIndexed { index, point ->
            if (index == 0 || index == points.lastIndex) {
                drawRect(
                    color = palette.secondary.copy(alpha = 0.20f),
                    topLeft = Offset(size.width * point.first - pixel, size.height * point.second - pixel),
                    size = Size(pixel * 3f, pixel * 3f),
                    style = Stroke(width = pixel),
                )
            }
        }
    }
}

private fun DrawScope.drawCyberDataColumns(palette: PixelPalette, progress: Float, pixel: Float) {
    val columns = listOf(0.10f, 0.29f, 0.48f, 0.69f, 0.89f)
    val digitPixel = pixel * 1.15f
    val stepY = digitPixel * 8f
    val travelHeight = size.height + stepY * 2f
    columns.forEachIndexed { column, xFraction ->
        val phase = (progress + column * 0.17f) % 1f
        val firstY = phase * travelHeight - stepY * 2f
        repeat(8) { row ->
            val rawY = firstY + row * size.height / 6f
            val y = ((rawY + stepY) % travelHeight) - stepY
            val digit = (row + column + (progress * 10f).toInt()) % 2
            val color = when (column % 4) {
                0 -> palette.secondary
                1 -> palette.primary
                2 -> CyberIce
                else -> CYBER_MAGENTA
            }
            drawCyberBinaryDigit(
                digit = digit,
                origin = Offset(size.width * xFraction, y),
                pixel = digitPixel,
                color = color.copy(alpha = if (row == 3) 0.24f else 0.11f),
            )
        }
    }
}

private fun DrawScope.drawCyberCodeWindow(palette: PixelPalette, pixel: Float) {
    val origin = Offset(size.width * 0.20f, size.height * 0.34f)
    val panelSize = Size(size.width * 0.60f, size.height * 0.18f)
    drawRect(
        color = palette.shadow.copy(alpha = 0.42f),
        topLeft = origin,
        size = panelSize,
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.19f),
        topLeft = origin,
        size = panelSize,
        style = Stroke(width = pixel),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.15f),
        topLeft = origin,
        size = Size(panelSize.width, pixel * 8f),
    )
    listOf(palette.secondary, CyberIce, CYBER_MAGENTA).forEachIndexed { index, color ->
        drawRect(
            color = color.copy(alpha = 0.28f),
            topLeft = origin + Offset(pixel * (5f + index * 5f), pixel * 3f),
            size = Size(pixel * 2f, pixel * 2f),
        )
    }
    val widths = listOf(0.48f, 0.68f, 0.37f, 0.57f)
    widths.forEachIndexed { index, width ->
        drawRect(
            color = (if (index % 2 == 0) palette.secondary else CyberIce).copy(alpha = 0.14f),
            topLeft = origin + Offset(pixel * 7f, pixel * (13f + index * 7f)),
            size = Size(panelSize.width * width, pixel),
        )
    }
}

private fun DrawScope.drawCyberServerRack(palette: PixelPalette, origin: Offset, pixel: Float) {
    val rackSize = Size(size.width * 0.16f, size.height * 0.17f)
    drawRect(
        color = CyberIce.copy(alpha = 0.14f),
        topLeft = origin,
        size = rackSize,
        style = Stroke(width = pixel),
    )
    repeat(4) { row ->
        val y = origin.y + rackSize.height * (0.14f + row * 0.22f)
        drawRect(
            color = palette.primary.copy(alpha = 0.12f),
            topLeft = Offset(origin.x + pixel * 5f, y),
            size = Size(rackSize.width - pixel * 10f, pixel * 5f),
            style = Stroke(width = pixel),
        )
        drawRect(
            color = (if (row % 2 == 0) palette.secondary else CYBER_MAGENTA).copy(alpha = 0.26f),
            topLeft = Offset(origin.x + rackSize.width - pixel * 11f, y + pixel * 2f),
            size = Size(pixel * 2f, pixel * 2f),
        )
    }
}

private fun DrawScope.drawCyberChip(palette: PixelPalette, center: Offset, pixel: Float) {
    val side = minOf(size.width, size.height) * 0.10f
    val origin = center - Offset(side / 2f, side / 2f)
    drawRect(
        color = palette.primary.copy(alpha = 0.14f),
        topLeft = origin,
        size = Size(side, side),
        style = Stroke(width = pixel * 1.5f),
    )
    drawRect(
        color = palette.shadow.copy(alpha = 0.34f),
        topLeft = origin + Offset(pixel * 5f, pixel * 5f),
        size = Size(side - pixel * 10f, side - pixel * 10f),
    )
    repeat(4) { index ->
        val pinOffset = side * (0.18f + index * 0.21f)
        drawRect(
            palette.secondary.copy(alpha = 0.20f),
            Offset(origin.x + pinOffset, origin.y - pixel * 4f),
            Size(pixel, pixel * 4f),
        )
        drawRect(
            CyberIce.copy(alpha = 0.18f),
            Offset(origin.x + pinOffset, origin.y + side),
            Size(pixel, pixel * 4f),
        )
        drawRect(
            palette.primary.copy(alpha = 0.18f),
            Offset(origin.x - pixel * 4f, origin.y + pinOffset),
            Size(pixel * 4f, pixel),
        )
        drawRect(
            CYBER_MAGENTA.copy(alpha = 0.18f),
            Offset(origin.x + side, origin.y + pinOffset),
            Size(pixel * 4f, pixel),
        )
    }
}

private fun DrawScope.drawCyberMask(palette: PixelPalette, origin: Offset, pixel: Float) {
    val maskWidth = size.width * 0.10f
    val maskHeight = size.height * 0.055f
    drawRect(
        color = palette.primary.copy(alpha = 0.11f),
        topLeft = origin,
        size = Size(maskWidth, maskHeight),
        style = Stroke(width = pixel),
    )
    drawRect(
        color = CyberIce.copy(alpha = 0.22f),
        topLeft = origin + Offset(maskWidth * 0.16f, maskHeight * 0.32f),
        size = Size(maskWidth * 0.22f, pixel * 3f),
    )
    drawRect(
        color = CyberIce.copy(alpha = 0.22f),
        topLeft = origin + Offset(maskWidth * 0.62f, maskHeight * 0.32f),
        size = Size(maskWidth * 0.22f, pixel * 3f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.18f),
        topLeft = origin + Offset(maskWidth * 0.38f, maskHeight * 0.70f),
        size = Size(maskWidth * 0.24f, pixel),
    )
}

private fun DrawScope.drawCyberGlitchAndScanline(palette: PixelPalette, progress: Float, pixel: Float) {
    val scanY = progress * size.height
    drawRect(
        color = CyberIce.copy(alpha = 0.12f),
        topLeft = Offset(0f, scanY),
        size = Size(size.width, pixel),
    )
    val pulseX = progress * size.width
    repeat(7) { index ->
        val y = size.height * (0.13f + index * 0.12f)
        val barWidth = size.width * (0.035f + index % 3 * 0.018f)
        val x = (pulseX + size.width * index * 0.19f) % (size.width + barWidth) - barWidth
        drawRect(
            color = when (index % 4) {
                0 -> palette.primary.copy(alpha = 0.24f)
                1 -> palette.secondary.copy(alpha = 0.20f)
                2 -> CYBER_MAGENTA.copy(alpha = 0.22f)
                else -> CyberIce.copy(alpha = 0.20f)
            },
            topLeft = Offset(x, y),
            size = Size(barWidth, pixel),
        )
    }
    listOf(0.27f, 0.58f, 0.73f).forEachIndexed { index, yFraction ->
        val shift = ((progress * 11f + index * 0.31f) % 1f) * size.width * 0.12f
        drawRect(
            color = (if (index == 1) CYBER_MAGENTA else palette.primary).copy(alpha = 0.13f),
            topLeft = Offset(size.width * (0.08f + index * 0.18f) + shift, size.height * yFraction),
            size = Size(size.width * (0.17f + index * 0.04f), pixel * (1f + index % 2)),
        )
    }
}

private fun DrawScope.drawCyberBinaryDigit(
    digit: Int,
    origin: Offset,
    pixel: Float,
    color: Color,
) {
    val pattern = if (digit == 0) CYBER_ZERO else CYBER_ONE
    pattern.forEachIndexed { index, enabled ->
        if (!enabled) return@forEachIndexed
        val column = index % 3
        val row = index / 3
        drawRect(
            color = color,
            topLeft = origin + Offset(column * pixel, row * pixel),
            size = Size(pixel, pixel),
        )
    }
}

private val CYBER_ZERO = booleanArrayOf(
    true, true, true,
    true, false, true,
    true, false, true,
    true, false, true,
    true, true, true,
)

private val CYBER_ONE = booleanArrayOf(
    false, true, false,
    true, true, false,
    false, true, false,
    false, true, false,
    true, true, true,
)
