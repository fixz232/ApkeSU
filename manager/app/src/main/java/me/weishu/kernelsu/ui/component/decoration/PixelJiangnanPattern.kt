package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawJiangnanCardInterior(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val window = Offset(unit * 2.4f, size.height - unit * 7.4f)
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.56f),
        topLeft = window,
        size = Size(unit * 5.2f, unit * 5.2f),
        style = Stroke(width = unit * 0.34f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.48f),
        topLeft = window + Offset(unit * 2.35f, 0f),
        size = Size(unit * 0.32f, unit * 5.2f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.48f),
        topLeft = window + Offset(0f, unit * 2.35f),
        size = Size(unit * 5.2f, unit * 0.32f),
    )
    repeat(2) { index ->
        drawRect(
            color = secondary.copy(alpha = secondary.alpha * 0.52f),
            topLeft = window + Offset(unit * (0.75f + index * 3f), unit * (0.75f + index * 3f)),
            size = Size(unit * 0.72f, unit * 0.72f),
        )
    }

    val scroll = Offset(size.width - unit * 8.2f, size.height - unit * 7f)
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.34f),
        topLeft = scroll,
        size = Size(unit * 5.5f, unit * 4.5f),
    )
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.54f),
        topLeft = scroll,
        size = Size(unit * 5.5f, unit * 4.5f),
        style = Stroke(width = unit * 0.30f),
    )
    repeat(4) { index ->
        drawRect(
            color = primary.copy(alpha = primary.alpha * (0.38f + index * 0.035f)),
            topLeft = scroll + Offset(unit * 0.85f, unit * (0.75f + index * 0.82f)),
            size = Size(unit * (3.7f - index * 0.35f), unit * 0.26f),
        )
    }

    val waterY = size.height - unit * 1.7f
    repeat(6) { index ->
        drawRect(
            color = (if (index == 3) secondary else primary).copy(alpha = 0.42f + index % 2 * 0.05f),
            topLeft = Offset(size.width * (0.20f + index * 0.11f), waterY - unit * (index % 2 * 0.45f)),
            size = Size(size.width * 0.09f, unit * 0.30f),
        )
    }
    drawJiangnanPatternUmbrella(
        origin = Offset(size.width * 0.45f, size.height - unit * 6.6f),
        unit = unit * 0.62f,
        primary = primary,
        secondary = secondary,
        shadow = shadow,
    )
    drawJiangnanPatternLotus(
        origin = Offset(size.width * 0.67f, size.height - unit * 4.8f),
        unit = unit * 0.60f,
        primary = primary,
        secondary = secondary,
        highlight = highlight,
    )
}

internal fun DrawScope.drawJiangnanCardTop(
    topHeight: Float,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val roofBase = topHeight - unit * 0.42f
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.34f),
        topLeft = Offset(size.width * 0.12f, topHeight * 0.48f),
        size = Size(size.width * 0.76f, topHeight * 0.48f),
    )
    repeat(4) { level ->
        val width = size.width * (0.78f - level * 0.13f)
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * (0.52f + level * 0.05f)),
            topLeft = Offset((size.width - width) / 2f, roofBase - unit * (level + 1f) * 0.68f),
            size = Size(width, unit * 0.54f),
        )
    }
    repeat(7) { tile ->
        drawRect(
            color = primary.copy(alpha = primary.alpha * (0.42f + tile % 2 * 0.06f)),
            topLeft = Offset(size.width * (0.20f + tile * 0.10f), roofBase - unit * 0.75f),
            size = Size(size.width * 0.055f, unit * 0.28f),
        )
    }
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.60f),
        topLeft = Offset(size.width * 0.49f, unit * 0.30f),
        size = Size(size.width * 0.025f, topHeight * 0.38f),
    )
    listOf(0.15f, 0.85f).forEachIndexed { index, x ->
        val petalColor = if (index == 0) secondary else primary
        repeat(3) { petal ->
            drawRect(
                color = petalColor.copy(alpha = 0.48f + petal * 0.04f),
                topLeft = Offset(size.width * x + unit * (petal - 1), unit * (0.6f + petal % 2 * 0.45f)),
                size = Size(unit * 0.62f, unit * 0.62f),
            )
        }
    }
}

internal fun DrawScope.drawJiangnanFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val inset = unit * 1.55f
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.42f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(width = unit * 0.24f),
    )
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.40f),
        topLeft = Offset(unit * 2.2f, unit * 2.2f),
        size = Size(size.width - unit * 4.4f, size.height - unit * 4.4f),
        style = Stroke(width = unit * 0.18f),
    )

    repeat(5) { index ->
        val y = size.height * (0.16f + index * 0.17f)
        listOf(unit * 0.92f, size.width - unit * 1.22f).forEachIndexed { side, x ->
            drawRect(
                color = (if ((index + side) % 3 == 1) secondary else outlineBlend(primary, shadow))
                    .copy(alpha = 0.34f + index % 2 * 0.05f),
                topLeft = Offset(x, y),
                size = Size(unit * 0.28f, unit * 0.86f),
            )
        }
    }

    repeat(7) { index ->
        val width = size.width * (0.07f + index % 2 * 0.015f)
        drawRect(
            color = (if (index == 3) secondary else primary).copy(alpha = 0.32f + index % 3 * 0.045f),
            topLeft = Offset(size.width * (0.16f + index * 0.10f), size.height - unit * (1.10f + index % 2 * 0.34f)),
            size = Size(width, unit * 0.26f),
        )
    }

    listOf(
        Offset(unit * 2.1f, unit * 2.1f),
        Offset(size.width - unit * 5.1f, unit * 2.1f),
        Offset(unit * 2.1f, size.height - unit * 5.1f),
        Offset(size.width - unit * 5.1f, size.height - unit * 5.1f),
    ).forEachIndexed { index, origin ->
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * 0.24f),
            topLeft = origin,
            size = Size(unit * 3f, unit * 3f),
        )
        drawRect(
            color = (if (index == 1 || index == 2) secondary else primary).copy(alpha = 0.46f),
            topLeft = origin,
            size = Size(unit * 3f, unit * 3f),
            style = Stroke(width = unit * 0.24f),
        )
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * 0.38f),
            topLeft = origin + Offset(unit * 1.35f, 0f),
            size = Size(unit * 0.24f, unit * 3f),
        )
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * 0.38f),
            topLeft = origin + Offset(0f, unit * 1.35f),
            size = Size(unit * 3f, unit * 0.24f),
        )
    }
}

private fun DrawScope.drawJiangnanPatternUmbrella(
    origin: Offset,
    unit: Float,
    primary: Color,
    secondary: Color,
    shadow: Color,
) {
    repeat(5) { segment ->
        val distance = kotlin.math.abs(segment - 2)
        drawRect(
            color = secondary.copy(alpha = secondary.alpha * (0.52f + distance * 0.04f)),
            topLeft = origin + Offset(unit * segment, unit * distance * 0.52f),
            size = Size(unit * 1.1f, unit * 0.55f),
        )
    }
    drawRect(shadow.copy(alpha = shadow.alpha * 0.50f), origin + Offset(unit * 2.45f, unit * 0.60f), Size(unit * 0.28f, unit * 4.2f))
    drawRect(primary.copy(alpha = primary.alpha * 0.42f), origin + Offset(unit * 2.6f, unit * 4.1f), Size(unit * 1.8f, unit * 0.30f))
}

private fun DrawScope.drawJiangnanPatternLotus(
    origin: Offset,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
) {
    drawRect(primary.copy(alpha = primary.alpha * 0.50f), origin + Offset(unit * 1.8f, unit * 1.2f), Size(unit * 0.28f, unit * 2.7f))
    drawRect(primary.copy(alpha = primary.alpha * 0.48f), origin + Offset(0f, unit * 3.5f), Size(unit * 4f, unit * 0.36f))
    drawRect(secondary.copy(alpha = secondary.alpha * 0.62f), origin + Offset(unit * 0.8f, unit * 0.65f), Size(unit, unit))
    drawRect(secondary.copy(alpha = secondary.alpha * 0.62f), origin + Offset(unit * 2.1f, unit * 0.65f), Size(unit, unit))
    drawRect(highlight.copy(alpha = highlight.alpha * 0.52f), origin + Offset(unit * 1.5f, 0f), Size(unit * 0.9f, unit * 1.4f))
}

private fun outlineBlend(first: Color, second: Color): Color = Color(
    red = (first.red + second.red) / 2f,
    green = (first.green + second.green) / 2f,
    blue = (first.blue + second.blue) / 2f,
    alpha = minOf(first.alpha, second.alpha),
)
