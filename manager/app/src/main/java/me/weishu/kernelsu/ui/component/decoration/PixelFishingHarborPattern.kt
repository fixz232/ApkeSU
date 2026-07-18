package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawFishingHarborCardInterior(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val waterY = size.height - unit * 2.6f
    repeat(3) { row ->
        val segmentWidth = size.width * (0.13f + row * 0.015f)
        repeat(4) { column ->
            val x = size.width * (0.05f + column * 0.25f) + if (row % 2 == 0) 0f else segmentWidth * 0.35f
            drawRect(
                color = primary.copy(alpha = primary.alpha * (0.34f - row * 0.06f)),
                topLeft = Offset(x, waterY + unit * row * 0.62f),
                size = Size(segmentWidth, unit * 0.38f),
            )
        }
    }

    val barrel = Offset(unit * 2.2f, size.height - unit * 6.3f)
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.78f),
        topLeft = barrel,
        size = Size(unit * 3.4f, unit * 4.2f),
    )
    listOf(0.65f, 3.05f).forEach { y ->
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * 0.72f),
            topLeft = barrel + Offset(0f, unit * y),
            size = Size(unit * 3.4f, unit * 0.42f),
        )
    }
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.38f),
        topLeft = barrel + Offset(unit * 0.72f, unit * 0.45f),
        size = Size(unit * 0.42f, unit * 3.1f),
    )

    val netOrigin = Offset(size.width - unit * 7.4f, size.height - unit * 7.2f)
    val netSize = Size(unit * 4.8f, unit * 4.8f)
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.62f),
        topLeft = netOrigin,
        size = netSize,
        style = Stroke(width = unit * 0.38f),
    )
    repeat(3) { index ->
        val offset = unit * (1.2f + index * 1.05f)
        drawRect(
            color = outlineBlend(primary, shadow).copy(alpha = 0.42f),
            topLeft = netOrigin + Offset(offset, unit * 0.35f),
            size = Size(unit * 0.28f, netSize.height - unit * 0.7f),
        )
        drawRect(
            color = outlineBlend(primary, shadow).copy(alpha = 0.42f),
            topLeft = netOrigin + Offset(unit * 0.35f, offset),
            size = Size(netSize.width - unit * 0.7f, unit * 0.28f),
        )
    }

    val fish = Offset(size.width * 0.56f, size.height - unit * 4.1f)
    drawRect(primary.copy(alpha = primary.alpha * 0.62f), fish, Size(unit * 2.4f, unit * 1.1f))
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.62f),
        topLeft = fish + Offset(-unit, -unit * 0.45f),
        size = Size(unit, unit * 2f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.68f),
        topLeft = fish + Offset(unit * 1.55f, unit * 0.3f),
        size = Size(unit * 0.38f, unit * 0.38f),
    )

    val shell = Offset(size.width * 0.38f, size.height - unit * 3.5f)
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.58f),
        topLeft = shell,
        size = Size(unit * 2.4f, unit * 1.5f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.42f),
        topLeft = shell + Offset(unit * 0.55f, -unit * 0.55f),
        size = Size(unit * 1.3f, unit * 0.55f),
    )
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.48f),
        topLeft = shell + Offset(unit * 1.05f, unit * 0.4f),
        size = Size(unit * 0.32f, unit * 1.1f),
    )
}

internal fun DrawScope.drawFishingHarborCardTop(
    topHeight: Float,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val pierY = topHeight - unit * 0.72f
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.76f),
        topLeft = Offset(size.width * 0.07f, pierY),
        size = Size(size.width * 0.86f, unit * 0.72f),
    )
    repeat(8) { index ->
        drawRect(
            color = secondary.copy(alpha = secondary.alpha * (0.58f + index % 2 * 0.12f)),
            topLeft = Offset(size.width * (0.09f + index * 0.105f), pierY - unit * 0.38f),
            size = Size(size.width * 0.085f, unit * 0.42f),
        )
    }

    val lighthouseX = size.width * 0.17f
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.70f),
        topLeft = Offset(lighthouseX, unit * 0.75f),
        size = Size(unit * 1.45f, pierY - unit * 0.75f),
    )
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.84f),
        topLeft = Offset(lighthouseX - unit * 0.4f, unit * 0.35f),
        size = Size(unit * 2.25f, unit * 0.72f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.76f),
        topLeft = Offset(lighthouseX + unit * 0.5f, unit * 1.45f),
        size = Size(unit * 0.48f, unit * 0.48f),
    )

    val boatX = size.width * 0.43f
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.84f),
        topLeft = Offset(boatX, pierY - unit * 1.1f),
        size = Size(size.width * 0.22f, unit * 0.72f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.76f),
        topLeft = Offset(boatX + size.width * 0.085f, unit * 0.55f),
        size = Size(unit * 0.42f, pierY - unit * 1.2f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.48f),
        topLeft = Offset(boatX + size.width * 0.088f, unit * 0.75f),
        size = Size(size.width * 0.06f, unit * 0.42f),
    )

    listOf(0.76f to 0.6f, 0.84f to 1.35f).forEach { (x, y) ->
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * 0.72f),
            topLeft = Offset(size.width * x, unit * y),
            size = Size(unit * 0.8f, unit * 0.34f),
        )
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * 0.72f),
            topLeft = Offset(size.width * x + unit * 0.65f, unit * (y - 0.32f)),
            size = Size(unit * 0.8f, unit * 0.34f),
        )
    }
}

internal fun DrawScope.drawFishingHarborFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val inset = unit * 1.35f
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.72f),
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

    repeat(6) { index ->
        val x = size.width * (0.08f + index * 0.165f)
        val plankColor = if (index % 2 == 0) secondary else outlineBlend(secondary, shadow)
        drawRect(
            color = plankColor.copy(alpha = plankColor.alpha * 0.54f),
            topLeft = Offset(x, unit * 0.82f),
            size = Size(size.width * 0.11f, unit * 0.35f),
        )
    }

    listOf(
        Offset(unit * 2.3f, unit * 2.3f),
        Offset(size.width - unit * 4.5f, unit * 2.3f),
        Offset(unit * 2.3f, size.height - unit * 4.5f),
        Offset(size.width - unit * 4.5f, size.height - unit * 4.5f),
    ).forEach { knot ->
        drawRect(
            color = primary.copy(alpha = primary.alpha * 0.64f),
            topLeft = knot,
            size = Size(unit * 2.2f, unit * 2.2f),
            style = Stroke(width = unit * 0.38f),
        )
        drawRect(
            color = highlight.copy(alpha = highlight.alpha * 0.42f),
            topLeft = knot + Offset(unit * 0.72f, unit * 0.72f),
            size = Size(unit * 0.76f, unit * 0.76f),
        )
    }

    repeat(5) { index ->
        val x = size.width * (0.10f + index * 0.19f)
        drawRect(
            color = primary.copy(alpha = primary.alpha * (0.42f + index % 2 * 0.10f)),
            topLeft = Offset(x, size.height - unit * (1.2f + index % 2 * 0.38f)),
            size = Size(size.width * 0.12f, unit * 0.38f),
        )
    }
}

private fun outlineBlend(first: Color, second: Color): Color = Color(
    red = (first.red + second.red) / 2f,
    green = (first.green + second.green) / 2f,
    blue = (first.blue + second.blue) / 2f,
    alpha = minOf(first.alpha, second.alpha),
)
