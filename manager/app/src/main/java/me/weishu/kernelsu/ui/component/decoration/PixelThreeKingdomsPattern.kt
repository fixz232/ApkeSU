package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

internal fun DrawScope.drawThreeKingdomsCardInterior(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val slipsOrigin = Offset(unit * 2.2f, size.height - unit * 7f)
    repeat(4) { index ->
        drawRect(
            color = if (index % 2 == 0) primary.copy(alpha = primary.alpha * 0.76f) else shadow,
            topLeft = slipsOrigin + Offset(unit * index * 1.15f, unit * (index % 2) * 0.45f),
            size = Size(unit * 0.82f, unit * (4.6f - index % 2 * 0.5f)),
        )
    }
    listOf(0.8f, 3.2f).forEach { y ->
        drawRect(
            color = secondary.copy(alpha = secondary.alpha * 0.70f),
            topLeft = slipsOrigin + Offset(-unit * 0.35f, unit * y),
            size = Size(unit * 5f, unit * 0.38f),
        )
    }

    val fanCenter = Offset(size.width - unit * 5.2f, size.height - unit * 3.2f)
    repeat(5) { index ->
        rotate(-42f + index * 21f, fanCenter) {
            drawRect(
                color = highlight.copy(alpha = highlight.alpha * (0.54f + index * 0.04f)),
                topLeft = fanCenter + Offset(0f, -unit * 0.38f),
                size = Size(unit * 4f, unit * 0.76f),
            )
            drawRect(
                color = primary.copy(alpha = primary.alpha * 0.64f),
                topLeft = fanCenter + Offset(unit * 3.2f, -unit * 0.38f),
                size = Size(unit * 0.8f, unit * 0.76f),
            )
        }
    }
    drawRect(
        color = secondary,
        topLeft = fanCenter - Offset(unit * 0.65f, unit * 0.65f),
        size = Size(unit * 1.3f, unit * 1.3f),
    )

    val tally = Offset(size.width * 0.48f, size.height - unit * 3.2f)
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.68f),
        topLeft = tally,
        size = Size(unit * 5f, unit * 1.8f),
        style = Stroke(width = unit * 0.45f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.70f),
        topLeft = tally + Offset(unit, unit * 0.55f),
        size = Size(unit * 3f, unit * 0.45f),
    )
}

internal fun DrawScope.drawThreeKingdomsCardTop(
    topHeight: Float,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    drawRect(
        color = shadow,
        topLeft = Offset(size.width * 0.10f, topHeight - unit * 0.55f),
        size = Size(size.width * 0.80f, unit * 0.55f),
    )
    repeat(3) { level ->
        val width = size.width * (0.68f - level * 0.14f)
        drawRect(
            color = if (level == 1) primary else secondary.copy(alpha = secondary.alpha * 0.82f),
            topLeft = Offset((size.width - width) / 2f, topHeight * (0.58f - level * 0.22f)),
            size = Size(width, unit * 0.68f),
        )
    }
    listOf(0.16f, 0.84f).forEach { x ->
        drawRect(
            color = secondary,
            topLeft = Offset(size.width * x, topHeight * 0.56f),
            size = Size(unit * 0.72f, unit * 1.6f),
        )
        drawRect(
            color = primary,
            topLeft = Offset(size.width * x - unit * 0.5f, topHeight - unit * 0.55f),
            size = Size(unit * 1.7f, unit * 0.55f),
        )
    }
    val lanternCenter = Offset(size.width / 2f, topHeight * 0.76f)
    drawRect(
        color = secondary,
        topLeft = lanternCenter - Offset(unit, unit * 0.78f),
        size = Size(unit * 2f, unit * 1.55f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.78f),
        topLeft = lanternCenter - Offset(unit * 0.4f, unit * 0.34f),
        size = Size(unit * 0.8f, unit * 0.68f),
    )
}

internal fun DrawScope.drawThreeKingdomsFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val inset = unit * 1.4f
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.74f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(width = unit * 0.30f),
    )
    listOf(0.18f, 0.50f, 0.82f).forEachIndexed { index, x ->
        val width = size.width * 0.12f
        drawRect(
            color = if (index == 1) secondary else primary,
            topLeft = Offset(size.width * x - width / 2f, unit * 0.75f),
            size = Size(width, unit * 0.42f),
        )
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * 0.56f),
            topLeft = Offset(size.width * x - unit * 0.28f, unit * 0.75f),
            size = Size(unit * 0.56f, unit * 1.15f),
        )
    }

    val brickY = size.height - unit * 1.3f
    repeat(7) { index ->
        drawRect(
            color = if (index % 3 == 1) secondary.copy(alpha = secondary.alpha * 0.72f) else primary,
            topLeft = Offset(size.width * (0.07f + index * 0.135f), brickY),
            size = Size(size.width * 0.105f, unit * 0.46f),
        )
    }

    drawBronzeKeyCorner(Offset(unit * 2.2f, size.height * 0.40f), unit, primary, right = false)
    drawBronzeKeyCorner(
        origin = Offset(size.width - unit * 2.2f, size.height * 0.62f),
        unit = unit,
        color = secondary,
        right = true,
    )
}

private fun DrawScope.drawBronzeKeyCorner(
    origin: Offset,
    unit: Float,
    color: Color,
    right: Boolean,
) {
    val direction = if (right) -1f else 1f
    val path = Path().apply {
        moveTo(origin.x, origin.y - unit * 2f)
        lineTo(origin.x + direction * unit * 2f, origin.y - unit * 2f)
        lineTo(origin.x + direction * unit * 2f, origin.y)
        lineTo(origin.x + direction * unit * 4f, origin.y)
        lineTo(origin.x + direction * unit * 4f, origin.y + unit * 2f)
    }
    drawPath(
        path = path,
        color = color.copy(alpha = color.alpha * 0.76f),
        style = Stroke(width = unit * 0.42f),
    )
}
