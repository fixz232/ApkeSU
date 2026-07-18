package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawBianliangCardInterior(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val windowOrigin = Offset(size.width - unit * 7.2f, size.height * 0.22f)
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.58f),
        topLeft = windowOrigin,
        size = Size(unit * 4.8f, unit * 5.2f),
        style = Stroke(width = unit * 0.42f),
    )
    repeat(2) { index ->
        drawRect(
            color = primary.copy(alpha = primary.alpha * 0.68f),
            topLeft = windowOrigin + Offset(unit * (1.55f + index * 1.55f), unit * 0.6f),
            size = Size(unit * 0.38f, unit * 4f),
        )
        drawRect(
            color = primary.copy(alpha = primary.alpha * 0.56f),
            topLeft = windowOrigin + Offset(unit * 0.55f, unit * (1.55f + index * 1.55f)),
            size = Size(unit * 3.7f, unit * 0.38f),
        )
    }

    val vaseOrigin = Offset(unit * 2.5f, size.height - unit * 7f)
    drawRect(
        color = outlineColor(primary, highlight),
        topLeft = vaseOrigin + Offset(unit * 0.7f, 0f),
        size = Size(unit * 1.4f, unit * 1.1f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.62f),
        topLeft = vaseOrigin,
        size = Size(unit * 2.8f, unit * 4.2f),
        style = Stroke(width = unit * 0.45f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.72f),
        topLeft = vaseOrigin + Offset(unit * 0.7f, unit * 1.4f),
        size = Size(unit * 1.4f, unit * 0.45f),
    )

    val pendant = Offset(size.width - unit * 6f, size.height - unit * 6f)
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.76f),
        topLeft = pendant,
        size = Size(unit * 3.2f, unit * 3.2f),
        style = Stroke(width = unit * 0.5f),
    )
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.82f),
        topLeft = pendant + Offset(unit, unit),
        size = Size(unit * 1.2f, unit * 1.2f),
    )
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.70f),
        topLeft = pendant + Offset(unit * 1.35f, unit * 3.2f),
        size = Size(unit * 0.5f, unit * 1.8f),
    )
}

internal fun DrawScope.drawBianliangCardTop(
    topHeight: Float,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val roofY = (topHeight - unit * 2.3f).coerceAtLeast(0f)
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.68f),
        topLeft = Offset(size.width * 0.19f, roofY + unit * 1.2f),
        size = Size(size.width * 0.62f, unit * 0.65f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.78f),
        topLeft = Offset(size.width * 0.24f, roofY + unit * 0.45f),
        size = Size(size.width * 0.52f, unit * 0.62f),
    )
    repeat(9) { index ->
        drawRect(
            color = if (index % 2 == 0) {
                primary.copy(alpha = primary.alpha * 0.76f)
            } else {
                highlight.copy(alpha = highlight.alpha * 0.42f)
            },
            topLeft = Offset(size.width * (0.25f + index * 0.056f), roofY),
            size = Size(unit * 1.1f, unit * 0.42f),
        )
    }

    listOf(size.width * 0.17f, size.width * 0.81f).forEach { x ->
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * 0.62f),
            topLeft = Offset(x, unit * 0.3f),
            size = Size(unit * 0.35f, unit * 1.2f),
        )
        drawRect(
            color = secondary.copy(alpha = secondary.alpha * 0.86f),
            topLeft = Offset(x - unit * 0.6f, unit * 1.5f),
            size = Size(unit * 1.55f, unit * 1.9f),
        )
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * 0.42f),
            topLeft = Offset(x - unit * 0.12f, unit * 1.85f),
            size = Size(unit * 0.6f, unit * 1.15f),
        )
    }

    repeat(6) { index ->
        val x = size.width * (0.07f + index * 0.045f)
        drawRect(
            color = primary.copy(alpha = primary.alpha * (0.42f + index % 2 * 0.12f)),
            topLeft = Offset(x, unit * (0.6f + index % 3 * 0.55f)),
            size = Size(unit * 0.72f, unit * 0.72f),
        )
    }
}

internal fun DrawScope.drawBianliangFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val line = (unit * 0.42f).coerceAtLeast(1f)
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.54f),
        topLeft = Offset(unit * 1.5f, unit * 1.5f),
        size = Size(size.width - unit * 3f, size.height - unit * 3f),
        style = Stroke(width = line),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.58f),
        topLeft = Offset(unit * 2.3f, unit * 2.3f),
        size = Size(size.width - unit * 4.6f, size.height - unit * 4.6f),
        style = Stroke(width = line * 0.72f),
    )

    listOf(
        Offset(unit * 2.6f, unit * 2.6f),
        Offset(size.width - unit * 5.8f, unit * 2.6f),
        Offset(unit * 2.6f, size.height - unit * 5.8f),
        Offset(size.width - unit * 5.8f, size.height - unit * 5.8f),
    ).forEach { corner ->
        drawRect(
            color = secondary.copy(alpha = secondary.alpha * 0.70f),
            topLeft = corner,
            size = Size(unit * 3.2f, unit * 3.2f),
            style = Stroke(width = line),
        )
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * 0.42f),
            topLeft = corner + Offset(unit, unit),
            size = Size(unit * 1.2f, unit * 1.2f),
        )
    }

    repeat(8) { index ->
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * 0.42f),
            topLeft = Offset(size.width * (0.06f + index * 0.12f), size.height - unit * 1.5f),
            size = Size(size.width * 0.08f, unit * 0.4f),
        )
    }
}

private fun outlineColor(primary: Color, highlight: Color): Color = Color(
    red = (primary.red + highlight.red) / 2f,
    green = (primary.green + highlight.green) / 2f,
    blue = (primary.blue + highlight.blue) / 2f,
    alpha = minOf(primary.alpha, highlight.alpha) * 0.72f,
)
