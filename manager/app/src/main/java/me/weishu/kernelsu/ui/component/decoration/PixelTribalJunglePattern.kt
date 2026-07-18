package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

internal fun DrawScope.drawTribalJungleCardInterior(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val pottery = Offset(unit * 2.2f, size.height - unit * 6.2f)
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.58f),
        topLeft = pottery + Offset(unit * 0.45f, unit * 0.65f),
        size = Size(unit * 3.5f, unit * 4.5f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.54f),
        topLeft = pottery,
        size = Size(unit * 4.4f, unit * 1.15f),
    )
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.72f),
        topLeft = pottery + Offset(unit * 1.05f, unit * 2.2f),
        size = Size(unit * 2.3f, unit * 0.55f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.66f),
        topLeft = pottery + Offset(unit * 1.55f, unit * 3.35f),
        size = Size(unit * 1.3f, unit * 0.55f),
    )

    val altarY = size.height - unit * 2.2f
    repeat(3) { level ->
        val width = unit * (7f - level * 1.5f)
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * (0.46f + level * 0.08f)),
            topLeft = Offset(size.width - unit * 2.2f - width, altarY - unit * (level + 1f)),
            size = Size(width, unit * 0.82f),
        )
    }
    val crystal = Offset(size.width - unit * 6f, altarY - unit * 5.2f)
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.82f),
        topLeft = crystal + Offset(unit * 0.75f, 0f),
        size = Size(unit * 1.2f, unit * 3.2f),
    )
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.68f),
        topLeft = crystal,
        size = Size(unit * 0.9f, unit * 2.2f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.58f),
        topLeft = crystal + Offset(unit * 1.05f, unit * 0.45f),
        size = Size(unit * 0.38f, unit * 1.6f),
    )

    val spearPivot = Offset(size.width * 0.48f, size.height - unit * 3.1f)
    rotate(-18f, spearPivot) {
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * 0.56f),
            topLeft = spearPivot - Offset(unit * 5.2f, unit * 0.22f),
            size = Size(unit * 10.4f, unit * 0.44f),
        )
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * 0.68f),
            topLeft = spearPivot + Offset(unit * 5.2f, -unit * 0.65f),
            size = Size(unit * 1.6f, unit * 1.3f),
        )
    }
}

internal fun DrawScope.drawTribalJungleCardTop(
    topHeight: Float,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val roofBottom = topHeight - unit * 0.45f
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.72f),
        topLeft = Offset(size.width * 0.18f, roofBottom - unit * 0.35f),
        size = Size(size.width * 0.64f, unit * 0.8f),
    )
    repeat(9) { index ->
        val stripHeight = unit * (1.25f + index % 3 * 0.55f)
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * (0.44f + index % 2 * 0.10f)),
            topLeft = Offset(size.width * (0.23f + index * 0.061f), roofBottom - stripHeight),
            size = Size(size.width * 0.052f, stripHeight),
        )
    }

    listOf(0.10f, 0.86f).forEachIndexed { index, x ->
        val postTop = unit * (0.45f + index * 0.25f)
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * 0.74f),
            topLeft = Offset(size.width * x, postTop),
            size = Size(unit * 2.1f, roofBottom - postTop),
        )
        drawRect(
            color = secondary.copy(alpha = secondary.alpha * 0.82f),
            topLeft = Offset(size.width * x + unit * 0.45f, postTop + unit * 0.72f),
            size = Size(unit * 0.48f, unit * 0.48f),
        )
        drawRect(
            color = primary.copy(alpha = primary.alpha * 0.70f),
            topLeft = Offset(size.width * x + unit * 1.2f, postTop + unit * 0.72f),
            size = Size(unit * 0.48f, unit * 0.48f),
        )
    }

    repeat(5) { index ->
        val x = size.width * (0.04f + index * 0.045f)
        val stemHeight = unit * (0.8f + index % 3 * 0.55f)
        drawRect(
            color = primary.copy(alpha = primary.alpha * 0.64f),
            topLeft = Offset(x, 0f),
            size = Size(unit * 0.38f, stemHeight),
        )
        drawRect(
            color = if (index == 3) secondary.copy(alpha = secondary.alpha * 0.76f) else primary,
            topLeft = Offset(x - unit * 0.42f, stemHeight - unit * 0.45f),
            size = Size(unit * 1.2f, unit * 0.62f),
        )
    }
}

internal fun DrawScope.drawTribalJungleFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val inset = unit * 1.35f
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.72f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(width = unit * 0.46f),
    )
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.58f),
        topLeft = Offset(unit * 2.15f, unit * 2.15f),
        size = Size(size.width - unit * 4.3f, size.height - unit * 4.3f),
        style = Stroke(width = unit * 0.28f),
    )

    repeat(8) { index ->
        val x = size.width * (0.05f + index * 0.125f)
        drawRect(
            color = (if (index % 2 == 0) primary else highlight)
                .copy(alpha = 0.44f),
            topLeft = Offset(x, unit * 0.78f),
            size = Size(size.width * 0.085f, unit * 0.38f),
        )
        drawRect(
            color = (if (index % 2 == 0) highlight else primary)
                .copy(alpha = 0.38f),
            topLeft = Offset(x + size.width * 0.032f, size.height - unit * 1.18f),
            size = Size(size.width * 0.085f, unit * 0.38f),
        )
    }

    listOf(
        Offset(unit * 2.2f, unit * 2.2f),
        Offset(size.width - unit * 5.2f, unit * 2.2f),
        Offset(unit * 2.2f, size.height - unit * 5.2f),
        Offset(size.width - unit * 5.2f, size.height - unit * 5.2f),
    ).forEachIndexed { index, origin ->
        drawRect(
            color = if (index == 1 || index == 2) secondary.copy(alpha = 0.62f) else primary.copy(alpha = 0.68f),
            topLeft = origin,
            size = Size(unit * 3f, unit * 3f),
            style = Stroke(width = unit * 0.42f),
        )
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * 0.40f),
            topLeft = origin + Offset(unit, unit),
            size = Size(unit, unit),
        )
    }

    repeat(5) { index ->
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * (0.34f + index % 2 * 0.10f)),
            topLeft = Offset(unit * 0.75f, size.height * (0.16f + index * 0.17f)),
            size = Size(unit * 0.38f, size.height * 0.09f),
        )
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * (0.34f + index % 2 * 0.10f)),
            topLeft = Offset(size.width - unit * 1.13f, size.height * (0.16f + index * 0.17f)),
            size = Size(unit * 0.38f, size.height * 0.09f),
        )
    }
}
