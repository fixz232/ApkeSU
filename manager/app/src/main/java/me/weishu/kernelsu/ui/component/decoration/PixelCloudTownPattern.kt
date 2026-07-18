package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawCloudTownCardInterior(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val island = Offset(unit * 2.5f, size.height - unit * 5.5f)
    drawRoundRect(
        color = primary.copy(alpha = primary.alpha * 0.52f),
        topLeft = island,
        size = Size(unit * 9f, unit * 0.95f),
        cornerRadius = CornerRadius(unit * 0.44f),
    )
    repeat(3) { layer ->
        val width = unit * (7f - layer * 1.45f)
        drawRoundRect(
            color = shadow.copy(alpha = shadow.alpha * (0.22f + layer * 0.04f)),
            topLeft = island + Offset(unit * 4.5f - width / 2f, unit * (0.8f + layer * 0.75f)),
            size = Size(width, unit * 0.80f),
            cornerRadius = CornerRadius(unit * 0.34f),
        )
    }
    drawCloudPatternHouse(
        origin = island + Offset(unit * 2.4f, -unit * 4.2f),
        unit = unit * 0.62f,
        primary = primary,
        secondary = secondary,
        highlight = highlight,
        shadow = shadow,
    )

    drawCloudPatternBalloon(
        origin = Offset(size.width - unit * 8.2f, size.height - unit * 8.2f),
        unit = unit * 0.62f,
        primary = primary,
        secondary = secondary,
        outline = shadow,
    )

    drawCloudPatternCrystal(
        origin = Offset(size.width * 0.57f, size.height - unit * 4.8f),
        unit = unit * 0.56f,
        primary = primary,
        secondary = secondary,
        highlight = highlight,
    )

    repeat(4) { index ->
        drawCloudPatternStar(
            center = Offset(size.width * (0.34f + index * 0.10f), size.height - unit * (7f + index % 2 * 1.2f)),
            unit = unit * (0.34f + index % 2 * 0.06f),
            color = if (index == 2) secondary else highlight,
        )
    }
}

internal fun DrawScope.drawCloudTownCardTop(
    topHeight: Float,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    repeat(9) { index ->
        val width = size.width * (0.085f + index % 3 * 0.015f)
        val lift = unit * (index % 3 * 0.34f)
        drawRoundRect(
            color = highlight.copy(alpha = highlight.alpha * (0.44f + index % 2 * 0.06f)),
            topLeft = Offset(size.width * (0.04f + index * 0.105f), topHeight - unit * 1.25f - lift),
            size = Size(width, unit * (0.75f + index % 2 * 0.16f)),
            cornerRadius = CornerRadius(unit * 0.56f),
        )
    }
    drawRoundRect(
        color = primary.copy(alpha = primary.alpha * 0.48f),
        topLeft = Offset(size.width * 0.33f, topHeight * 0.45f),
        size = Size(size.width * 0.34f, unit * 0.70f),
        cornerRadius = CornerRadius(unit * 0.34f),
    )
    drawCloudPatternHouse(
        origin = Offset(size.width * 0.44f, unit * 0.25f),
        unit = unit * 0.50f,
        primary = primary,
        secondary = secondary,
        highlight = highlight,
        shadow = shadow,
    )
    listOf(0.15f, 0.85f).forEachIndexed { index, x ->
        drawCloudPatternStar(
            center = Offset(size.width * x, unit * (1.1f + index * 0.4f)),
            unit = unit * 0.40f,
            color = if (index == 0) primary else secondary,
        )
    }
}

internal fun DrawScope.drawCloudTownFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val inset = unit * 1.55f
    drawRoundRect(
        color = primary.copy(alpha = primary.alpha * 0.34f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(unit * 0.72f),
        style = Stroke(width = unit * 0.22f),
    )

    repeat(6) { index ->
        val y = size.height * (0.15f + index * 0.14f)
        listOf(unit * 0.95f, size.width - unit * 1.35f).forEachIndexed { side, x ->
            drawRoundRect(
                color = (if ((index + side) % 3 == 1) secondary else highlight)
                    .copy(alpha = 0.28f + index % 2 * 0.05f),
                topLeft = Offset(x, y),
                size = Size(unit * 0.40f, unit * (0.72f + index % 2 * 0.20f)),
                cornerRadius = CornerRadius(unit * 0.20f),
            )
        }
    }

    val corners = listOf(
        Offset(unit * 2f, unit * 2f),
        Offset(size.width - unit * 6f, unit * 2f),
        Offset(unit * 2f, size.height - unit * 5f),
        Offset(size.width - unit * 6f, size.height - unit * 5f),
    )
    corners.forEachIndexed { index, origin ->
        drawCloudPatternPuff(
            origin = origin,
            unit = unit * 0.48f,
            color = if (index == 1 || index == 2) secondary else highlight,
        )
    }

    repeat(7) { index ->
        val width = size.width * (0.07f + index % 2 * 0.016f)
        drawRoundRect(
            color = (if (index == 3) secondary else primary).copy(alpha = 0.28f + index % 3 * 0.04f),
            topLeft = Offset(size.width * (0.16f + index * 0.10f), size.height - unit * (1.05f + index % 2 * 0.30f)),
            size = Size(width, unit * 0.28f),
            cornerRadius = CornerRadius(unit * 0.18f),
        )
    }

    drawRoundRect(
        color = shadow.copy(alpha = shadow.alpha * 0.18f),
        topLeft = Offset(unit * 2.3f, unit * 2.3f),
        size = Size(size.width - unit * 4.6f, size.height - unit * 4.6f),
        cornerRadius = CornerRadius(unit * 0.58f),
        style = Stroke(width = unit * 0.16f),
    )
}

private fun DrawScope.drawCloudPatternHouse(
    origin: Offset,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    drawRoundRect(
        color = highlight.copy(alpha = highlight.alpha * 0.40f),
        topLeft = origin + Offset(unit, unit * 2f),
        size = Size(unit * 6f, unit * 4f),
        cornerRadius = CornerRadius(unit * 0.48f),
    )
    repeat(3) { layer ->
        val width = unit * (8f - layer * 1.7f)
        drawRoundRect(
            color = (if (layer == 1) secondary else primary).copy(alpha = 0.48f + layer * 0.04f),
            topLeft = origin + Offset(unit * 4f - width / 2f, unit * layer * 0.72f),
            size = Size(width, unit * 0.82f),
            cornerRadius = CornerRadius(unit * 0.38f),
        )
    }
    drawRoundRect(
        color = shadow.copy(alpha = shadow.alpha * 0.34f),
        topLeft = origin + Offset(unit * 3f, unit * 3.2f),
        size = Size(unit * 1.5f, unit * 2.8f),
        cornerRadius = CornerRadius(unit * 0.28f),
    )
}

private fun DrawScope.drawCloudPatternBalloon(
    origin: Offset,
    unit: Float,
    primary: Color,
    secondary: Color,
    outline: Color,
) {
    repeat(5) { row ->
        val width = unit * (5f - kotlin.math.abs(row - 2) * 0.9f)
        drawRoundRect(
            color = (if (row == 2) secondary else primary).copy(alpha = 0.52f + row % 2 * 0.04f),
            topLeft = origin + Offset(unit * 2.5f - width / 2f, unit * row * 0.82f),
            size = Size(width, unit * 0.90f),
            cornerRadius = CornerRadius(unit * 0.42f),
        )
    }
    drawLine(outline.copy(alpha = outline.alpha * 0.34f), origin + Offset(unit * 1.2f, unit * 4f), origin + Offset(unit * 1.8f, unit * 5.2f), unit * 0.20f)
    drawLine(outline.copy(alpha = outline.alpha * 0.34f), origin + Offset(unit * 3.8f, unit * 4f), origin + Offset(unit * 3.2f, unit * 5.2f), unit * 0.20f)
    drawRoundRect(outline.copy(alpha = outline.alpha * 0.38f), origin + Offset(unit * 1.7f, unit * 5f), Size(unit * 1.6f, unit * 0.8f), CornerRadius(unit * 0.24f))
}

private fun DrawScope.drawCloudPatternCrystal(
    origin: Offset,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
) {
    drawRoundRect(secondary.copy(alpha = secondary.alpha * 0.56f), origin + Offset(unit, 0f), Size(unit, unit * 3.2f), CornerRadius(unit * 0.30f))
    drawRoundRect(primary.copy(alpha = primary.alpha * 0.48f), origin + Offset(0f, unit), Size(unit, unit * 2.2f), CornerRadius(unit * 0.30f))
    drawRoundRect(highlight.copy(alpha = highlight.alpha * 0.44f), origin + Offset(unit * 1.25f, unit * 0.55f), Size(unit * 0.28f, unit * 1.4f), CornerRadius(unit * 0.12f))
}

private fun DrawScope.drawCloudPatternStar(
    center: Offset,
    unit: Float,
    color: Color,
) {
    drawRoundRect(color.copy(alpha = color.alpha * 0.52f), center - Offset(unit * 0.22f, unit), Size(unit * 0.44f, unit * 2f), CornerRadius(unit * 0.16f))
    drawRoundRect(color.copy(alpha = color.alpha * 0.52f), center - Offset(unit, unit * 0.22f), Size(unit * 2f, unit * 0.44f), CornerRadius(unit * 0.16f))
}

private fun DrawScope.drawCloudPatternPuff(
    origin: Offset,
    unit: Float,
    color: Color,
) {
    listOf(
        Offset(0f, unit) to Size(unit * 6f, unit * 2f),
        Offset(unit, unit * 0.35f) to Size(unit * 2.4f, unit * 2.2f),
        Offset(unit * 3f, 0f) to Size(unit * 2.5f, unit * 2.5f),
    ).forEach { (offset, puffSize) ->
        drawRoundRect(
            color = color.copy(alpha = color.alpha * 0.44f),
            topLeft = origin + offset,
            size = puffSize,
            cornerRadius = CornerRadius(unit),
        )
    }
}
