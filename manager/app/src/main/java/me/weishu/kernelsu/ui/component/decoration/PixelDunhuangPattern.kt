package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawDunhuangCardInterior(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val lamp = Offset(unit * 2.4f, size.height - unit * 6.4f)
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.64f),
        topLeft = lamp + Offset(unit * 0.7f, unit * 2f),
        size = Size(unit * 2.8f, unit * 3.6f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.82f),
        topLeft = lamp,
        size = Size(unit * 4.2f, unit * 1.1f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.62f),
        topLeft = lamp + Offset(unit * 1.55f, -unit * 1.8f),
        size = Size(unit * 1.1f, unit * 2f),
    )
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.64f),
        topLeft = lamp + Offset(unit * 1.15f, unit * 3.2f),
        size = Size(unit * 1.9f, unit * 0.52f),
    )

    val scrollY = size.height - unit * 2.2f
    repeat(6) { index ->
        val width = size.width * (0.10f + index % 2 * 0.035f)
        drawRect(
            color = (if (index % 3 == 1) secondary else primary)
                .copy(alpha = if (index % 3 == 1) 0.54f else 0.48f),
            topLeft = Offset(size.width * (0.23f + index * 0.12f), scrollY - unit * (index % 2 * 0.55f)),
            size = Size(width, unit * 0.38f),
        )
    }

    val agate = Offset(size.width - unit * 7.4f, size.height - unit * 7.2f)
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.64f),
        topLeft = agate,
        size = Size(unit * 5.2f, unit * 5.2f),
        style = Stroke(width = unit * 0.46f),
    )
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.78f),
        topLeft = agate + Offset(unit * 0.7f, unit * 1.2f),
        size = Size(unit * 2f, unit * 2.8f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.72f),
        topLeft = agate + Offset(unit * 2.7f, unit * 0.65f),
        size = Size(unit * 1.7f, unit * 3.7f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.50f),
        topLeft = agate + Offset(unit * 3.15f, unit * 1.1f),
        size = Size(unit * 0.38f, unit * 1.8f),
    )

    listOf(0.42f to 0f, 0.48f to 0.8f, 0.54f to 0.2f).forEachIndexed { index, (x, lift) ->
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * (0.40f + index * 0.07f)),
            topLeft = Offset(size.width * x, size.height - unit * (4.4f + lift)),
            size = Size(unit * 0.9f, unit * 1.5f),
        )
    }
}

internal fun DrawScope.drawDunhuangCardTop(
    topHeight: Float,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val baseY = topHeight - unit * 0.48f
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.72f),
        topLeft = Offset(size.width * 0.10f, baseY),
        size = Size(size.width * 0.80f, unit * 0.48f),
    )
    repeat(4) { level ->
        val width = size.width * (0.44f - level * 0.075f)
        drawRect(
            color = primary.copy(alpha = primary.alpha * (0.58f + level * 0.06f)),
            topLeft = Offset((size.width - width) / 2f, baseY - unit * (level + 1f) * 0.72f),
            size = Size(width, unit * 0.78f),
        )
    }
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.70f),
        topLeft = Offset(size.width * 0.45f, unit * 0.82f),
        size = Size(size.width * 0.10f, baseY - unit * 0.95f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.50f),
        topLeft = Offset(size.width * 0.485f, unit * 1.25f),
        size = Size(size.width * 0.03f, unit * 1.1f),
    )

    listOf(0.12f, 0.20f, 0.72f, 0.81f).forEachIndexed { index, x ->
        val ribbonY = unit * (0.6f + index % 2 * 0.65f)
        drawRect(
            color = secondary.copy(alpha = secondary.alpha * (0.62f + index % 2 * 0.08f)),
            topLeft = Offset(size.width * x, ribbonY),
            size = Size(size.width * 0.13f, unit * 0.46f),
        )
        drawRect(
            color = primary.copy(alpha = primary.alpha * 0.54f),
            topLeft = Offset(size.width * (x + 0.06f), ribbonY + unit * 0.46f),
            size = Size(unit * 0.38f, unit * (0.8f + index % 2 * 0.55f)),
        )
    }

    listOf(0.08f, 0.90f).forEachIndexed { index, x ->
        drawRect(
            color = primary.copy(alpha = primary.alpha * 0.74f),
            topLeft = Offset(size.width * x, unit * 0.45f),
            size = Size(unit * 0.42f, unit * 1.4f),
        )
        drawRect(
            color = if (index == 0) secondary.copy(alpha = 0.68f) else highlight.copy(alpha = 0.54f),
            topLeft = Offset(size.width * x - unit * 0.55f, unit * 1.85f),
            size = Size(unit * 1.5f, unit * 1.2f),
        )
    }
}

internal fun DrawScope.drawDunhuangFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val inset = unit * 1.35f
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.66f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(width = unit * 0.50f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.56f),
        topLeft = Offset(unit * 2.15f, unit * 2.15f),
        size = Size(size.width - unit * 4.3f, size.height - unit * 4.3f),
        style = Stroke(width = unit * 0.28f),
    )

    listOf(
        0.04f to 0.11f,
        0.17f to 0.08f,
        0.29f to 0.15f,
        0.50f to 0.10f,
        0.65f to 0.13f,
        0.84f to 0.10f,
    ).forEachIndexed { index, (x, width) ->
        drawRect(
            color = (if (index % 3 == 2) secondary else shadow).copy(alpha = 0.56f),
            topLeft = Offset(size.width * x, unit * 0.78f),
            size = Size(size.width * width, unit * 0.40f),
        )
        drawRect(
            color = primary.copy(alpha = 0.44f + index % 2 * 0.08f),
            topLeft = Offset(size.width * x, size.height - unit * 1.18f),
            size = Size(size.width * width, unit * 0.40f),
        )
    }

    listOf(
        Offset(unit * 2.2f, unit * 2.2f),
        Offset(size.width - unit * 5.4f, unit * 2.2f),
        Offset(unit * 2.2f, size.height - unit * 5.4f),
        Offset(size.width - unit * 5.4f, size.height - unit * 5.4f),
    ).forEachIndexed { index, origin ->
        drawRect(
            color = outlineMix(shadow, primary).copy(alpha = 0.66f),
            topLeft = origin,
            size = Size(unit * 3.2f, unit * 3.2f),
        )
        drawRect(
            color = if (index == 1 || index == 2) secondary.copy(alpha = 0.64f) else primary.copy(alpha = 0.70f),
            topLeft = origin + Offset(unit * 0.8f, unit * 0.8f),
            size = Size(unit * 1.6f, unit * 1.6f),
            style = Stroke(width = unit * 0.38f),
        )
        drawRect(
            color = highlight.copy(alpha = 0.38f),
            topLeft = origin + Offset(unit * 1.35f, unit * 1.35f),
            size = Size(unit * 0.5f, unit * 0.5f),
        )
    }

    repeat(5) { index ->
        val y = size.height * (0.13f + index * 0.18f)
        drawRect(
            color = primary.copy(alpha = 0.40f + index % 2 * 0.08f),
            topLeft = Offset(unit * 0.78f, y),
            size = Size(unit * 0.38f, size.height * 0.08f),
        )
        drawRect(
            color = secondary.copy(alpha = 0.34f + index % 2 * 0.08f),
            topLeft = Offset(size.width - unit * 1.16f, y + unit * 0.7f),
            size = Size(unit * 0.38f, size.height * 0.07f),
        )
    }
}

private fun outlineMix(first: Color, second: Color): Color = Color(
    red = (first.red + second.red) / 2f,
    green = (first.green + second.green) / 2f,
    blue = (first.blue + second.blue) / 2f,
    alpha = minOf(first.alpha, second.alpha),
)
