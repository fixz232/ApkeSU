package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawVikingCardInterior(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val shield = Offset(unit * 2.4f, size.height - unit * 7.1f)
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.76f),
        topLeft = shield,
        size = Size(unit * 4.8f, unit * 4.8f),
        style = Stroke(width = unit * 0.55f),
    )
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.68f),
        topLeft = shield + Offset(unit * 1.55f, unit * 1.55f),
        size = Size(unit * 1.7f, unit * 1.7f),
    )
    drawLine(
        color = shadow.copy(alpha = shadow.alpha * 0.72f),
        start = shield + Offset(-unit * 0.5f, unit * 5.4f),
        end = shield + Offset(unit * 5.3f, -unit * 0.4f),
        strokeWidth = unit * 0.55f,
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.64f),
        topLeft = shield + Offset(unit * 4.3f, -unit * 0.8f),
        size = Size(unit * 2.2f, unit * 1.3f),
    )

    val runestone = Offset(size.width - unit * 7.2f, size.height - unit * 8.2f)
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.66f),
        topLeft = runestone,
        size = Size(unit * 4.8f, unit * 6.4f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.62f),
        topLeft = runestone + Offset(unit * 0.55f, unit * 0.5f),
        size = Size(unit * 3.7f, unit * 5.2f),
        style = Stroke(width = unit * 0.40f),
    )
    drawVikingPatternRune(
        origin = runestone + Offset(unit * 1.15f, unit * 1.25f),
        unit = unit * 0.62f,
        color = secondary.copy(alpha = secondary.alpha * 0.78f),
    )

    val iceY = size.height - unit * 1.7f
    val fissure = listOf(
        Offset(size.width * 0.28f, iceY),
        Offset(size.width * 0.40f, iceY - unit * 0.8f),
        Offset(size.width * 0.53f, iceY + unit * 0.1f),
        Offset(size.width * 0.68f, iceY - unit * 0.9f),
    )
    fissure.zipWithNext().forEach { (start, end) ->
        drawLine(primary.copy(alpha = primary.alpha * 0.52f), start, end, unit * 0.34f)
    }
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.76f),
        topLeft = Offset(size.width * 0.51f, size.height - unit * 5.3f),
        size = Size(unit * 1.2f, unit * 3.2f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.54f),
        topLeft = Offset(size.width * 0.515f, size.height - unit * 4.8f),
        size = Size(unit * 0.32f, unit * 1.5f),
    )
}

internal fun DrawScope.drawVikingCardTop(
    topHeight: Float,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val roofBase = topHeight - unit * 0.52f
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.76f),
        topLeft = Offset(size.width * 0.12f, roofBase),
        size = Size(size.width * 0.76f, unit * 0.52f),
    )
    repeat(4) { level ->
        val width = size.width * (0.64f - level * 0.11f)
        drawRect(
            color = primary.copy(alpha = primary.alpha * (0.58f + level * 0.06f)),
            topLeft = Offset((size.width - width) / 2f, roofBase - unit * (level + 1f) * 0.72f),
            size = Size(width, unit * 0.76f),
        )
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * (0.46f + level * 0.05f)),
            topLeft = Offset((size.width - width) / 2f, roofBase - unit * (level + 1f) * 0.72f),
            size = Size(width * 0.58f, unit * 0.26f),
        )
    }
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.80f),
        topLeft = Offset(size.width * 0.48f, unit * 0.40f),
        size = Size(size.width * 0.04f, topHeight * 0.42f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.62f),
        topLeft = Offset(size.width * 0.46f, unit * 0.25f),
        size = Size(size.width * 0.08f, unit * 0.45f),
    )
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.72f),
        topLeft = Offset(size.width * 0.46f, unit * 0.72f),
        size = Size(size.width * 0.08f, unit * 1.15f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.58f),
        topLeft = Offset(size.width * 0.42f, unit * 0.55f),
        size = Size(size.width * 0.045f, unit * 0.42f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.58f),
        topLeft = Offset(size.width * 0.535f, unit * 0.55f),
        size = Size(size.width * 0.045f, unit * 0.42f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.68f),
        topLeft = Offset(size.width * 0.395f, unit * 0.18f),
        size = Size(size.width * 0.035f, unit * 0.82f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.68f),
        topLeft = Offset(size.width * 0.57f, unit * 0.18f),
        size = Size(size.width * 0.035f, unit * 0.82f),
    )

    listOf(0.12f, 0.88f).forEachIndexed { index, x ->
        drawRect(
            color = primary.copy(alpha = primary.alpha * 0.72f),
            topLeft = Offset(size.width * x - unit * 0.25f, topHeight * 0.34f),
            size = Size(unit * 0.50f, topHeight * 0.48f),
        )
        drawRect(
            color = if (index == 0) secondary.copy(alpha = 0.68f) else highlight.copy(alpha = 0.58f),
            topLeft = Offset(size.width * x - unit, topHeight * 0.24f),
            size = Size(unit * 2f, unit * 0.55f),
        )
    }
}

internal fun DrawScope.drawVikingFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val inset = unit * 1.35f
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.68f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(width = unit * 0.52f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.52f),
        topLeft = Offset(unit * 2.15f, unit * 2.15f),
        size = Size(size.width - unit * 4.3f, size.height - unit * 4.3f),
        style = Stroke(width = unit * 0.28f),
    )

    repeat(9) { index ->
        val width = size.width * (0.075f + index % 3 * 0.018f)
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * (0.48f + index % 2 * 0.08f)),
            topLeft = Offset(size.width * (0.03f + index * 0.108f), unit * 0.72f),
            size = Size(width, unit * (0.40f + index % 3 * 0.10f)),
        )
    }

    repeat(5) { index ->
        val y = size.height * (0.18f + index * 0.16f)
        listOf(unit * 0.74f, size.width - unit * 1.15f).forEachIndexed { side, x ->
            drawRect(
                color = (if ((index + side) % 2 == 0) secondary else primary)
                    .copy(alpha = 0.42f + index % 2 * 0.08f),
                topLeft = Offset(x, y),
                size = Size(unit * 0.40f, unit * 1.15f),
            )
        }
    }

    val cracks = listOf(
        Offset(unit * 2.2f, unit * 3f) to Offset(unit * 5.2f, unit * 5.4f),
        Offset(size.width - unit * 2.2f, unit * 3f) to Offset(size.width - unit * 5.2f, unit * 5.4f),
        Offset(unit * 2.2f, size.height - unit * 3f) to Offset(unit * 5.2f, size.height - unit * 5.4f),
        Offset(size.width - unit * 2.2f, size.height - unit * 3f) to
            Offset(size.width - unit * 5.2f, size.height - unit * 5.4f),
    )
    cracks.forEachIndexed { index, (start, end) ->
        drawLine(
            color = (if (index % 2 == 0) primary else secondary).copy(alpha = 0.58f),
            start = start,
            end = end,
            strokeWidth = unit * 0.34f,
        )
        drawLine(
            color = highlight.copy(alpha = 0.40f),
            start = end,
            end = end + Offset(if (index % 2 == 0) unit * 1.4f else -unit * 1.4f, unit * 1.2f),
            strokeWidth = unit * 0.26f,
        )
    }
}

private fun DrawScope.drawVikingPatternRune(
    origin: Offset,
    unit: Float,
    color: Color,
) {
    drawRect(color, origin + Offset(unit, 0f), Size(unit * 0.42f, unit * 3f))
    drawRect(color, origin + Offset(0f, unit * 0.70f), Size(unit * 2.2f, unit * 0.42f))
    drawLine(color, origin + Offset(unit * 1.2f, unit), origin + Offset(unit * 2.1f, unit * 2.1f), unit * 0.34f)
}
