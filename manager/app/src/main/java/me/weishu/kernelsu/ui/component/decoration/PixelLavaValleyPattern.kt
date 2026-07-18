package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawLavaValleyCardInterior(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val fissureY = size.height - unit * 2.1f
    listOf(
        0.06f to 0.18f,
        0.22f to 0.13f,
        0.38f to 0.20f,
        0.62f to 0.14f,
        0.78f to 0.16f,
    ).forEachIndexed { index, (x, width) ->
        val lift = unit * (index % 3 * 0.65f)
        drawRect(
            color = primary.copy(alpha = primary.alpha * (0.58f + index % 2 * 0.10f)),
            topLeft = Offset(size.width * x, fissureY - lift),
            size = Size(size.width * width, unit * 0.48f),
        )
        if (index == 1 || index == 3) {
            drawRect(
                color = highlight.copy(alpha = highlight.alpha * 0.48f),
                topLeft = Offset(size.width * (x + width * 0.44f), fissureY - lift - unit * 1.5f),
                size = Size(unit * 0.42f, unit * 1.5f),
            )
        }
    }

    val bridgeY = size.height - unit * 5.1f
    repeat(5) { index ->
        val blockWidth = size.width * 0.075f
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * (0.62f + index % 2 * 0.10f)),
            topLeft = Offset(size.width * (0.30f + index * 0.078f), bridgeY - unit * (index % 2 * 0.28f)),
            size = Size(blockWidth, unit * 1.05f),
        )
    }

    val geode = Offset(size.width - unit * 7.4f, size.height - unit * 8f)
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.78f),
        topLeft = geode,
        size = Size(unit * 5.2f, unit * 5.8f),
        style = Stroke(width = unit * 0.54f),
    )
    listOf(0.6f to 1.8f, 2f to 3.1f, 3.35f to 2.4f).forEachIndexed { index, (x, height) ->
        drawRect(
            color = secondary.copy(alpha = secondary.alpha * (0.70f + index * 0.06f)),
            topLeft = geode + Offset(unit * x, unit * (4.7f - height)),
            size = Size(unit * 1.05f, unit * height),
        )
    }
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.50f),
        topLeft = geode + Offset(unit * 2.3f, unit * 1.2f),
        size = Size(unit * 0.36f, unit * 1.5f),
    )

    val fireCrystal = Offset(unit * 2.5f, size.height - unit * 7f)
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.82f),
        topLeft = fireCrystal + Offset(unit * 0.75f, 0f),
        size = Size(unit * 1.2f, unit * 4.4f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.66f),
        topLeft = fireCrystal,
        size = Size(unit * 0.9f, unit * 3f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.58f),
        topLeft = fireCrystal + Offset(unit * 1.05f, unit * 0.75f),
        size = Size(unit * 0.34f, unit * 2f),
    )
}

internal fun DrawScope.drawLavaValleyCardTop(
    topHeight: Float,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val baseY = topHeight - unit * 0.48f
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.82f),
        topLeft = Offset(size.width * 0.08f, baseY),
        size = Size(size.width * 0.84f, unit * 0.48f),
    )
    repeat(4) { level ->
        val width = size.width * (0.58f - level * 0.11f)
        drawRect(
            color = shadow.copy(alpha = shadow.alpha * (0.62f + level * 0.06f)),
            topLeft = Offset((size.width - width) / 2f, baseY - unit * (level + 1f) * 0.72f),
            size = Size(width, unit * 0.78f),
        )
    }
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.88f),
        topLeft = Offset(size.width * 0.44f, unit * 0.72f),
        size = Size(size.width * 0.12f, baseY - unit * 0.85f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.62f),
        topLeft = Offset(size.width * 0.48f, unit * 1.05f),
        size = Size(size.width * 0.035f, baseY - unit * 1.5f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.86f),
        topLeft = Offset(size.width * 0.39f, unit * 0.35f),
        size = Size(size.width * 0.22f, unit * 0.62f),
    )

    listOf(0.10f, 0.18f, 0.78f, 0.87f).forEachIndexed { index, x ->
        val height = unit * (0.9f + index % 2 * 0.65f)
        drawRect(
            color = (if (index == 1 || index == 2) secondary else shadow)
                .copy(alpha = if (index == 1 || index == 2) 0.70f else 0.76f),
            topLeft = Offset(size.width * x, baseY - height),
            size = Size(unit * 1.1f, height),
        )
    }

    repeat(3) { index ->
        drawRect(
            color = highlight.copy(alpha = 0.32f - index * 0.06f),
            topLeft = Offset(size.width * (0.60f + index * 0.06f), unit * (0.25f + index * 0.48f)),
            size = Size(unit * (1.7f + index * 0.4f), unit * 0.42f),
        )
    }
}

internal fun DrawScope.drawLavaValleyFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val inset = unit * 1.35f
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.76f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(width = unit * 0.52f),
    )
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.48f),
        topLeft = Offset(unit * 2.15f, unit * 2.15f),
        size = Size(size.width - unit * 4.3f, size.height - unit * 4.3f),
        style = Stroke(width = unit * 0.28f),
    )

    listOf(
        0.04f to 0.12f,
        0.19f to 0.08f,
        0.32f to 0.15f,
        0.54f to 0.10f,
        0.68f to 0.17f,
        0.88f to 0.07f,
    ).forEachIndexed { index, (x, width) ->
        drawRect(
            color = (if (index % 3 == 1) secondary else shadow).copy(alpha = 0.62f),
            topLeft = Offset(size.width * x, unit * 0.78f),
            size = Size(size.width * width, unit * 0.42f),
        )
        drawRect(
            color = primary.copy(alpha = 0.50f + index % 2 * 0.08f),
            topLeft = Offset(size.width * x, size.height - unit * 1.2f),
            size = Size(size.width * width, unit * 0.42f),
        )
    }

    listOf(
        Offset(unit * 2.2f, unit * 2.2f),
        Offset(size.width - unit * 5.4f, unit * 2.2f),
        Offset(unit * 2.2f, size.height - unit * 5.4f),
        Offset(size.width - unit * 5.4f, size.height - unit * 5.4f),
    ).forEachIndexed { index, origin ->
        drawRect(
            color = shadow.copy(alpha = 0.78f),
            topLeft = origin,
            size = Size(unit * 3.2f, unit * 3.2f),
        )
        drawRect(
            color = if (index == 1 || index == 2) secondary.copy(alpha = 0.68f) else primary.copy(alpha = 0.72f),
            topLeft = origin + Offset(unit * 0.85f, unit * 0.85f),
            size = Size(unit * 1.5f, unit * 1.5f),
            style = Stroke(width = unit * 0.40f),
        )
        drawRect(
            color = highlight.copy(alpha = 0.40f),
            topLeft = origin + Offset(unit * 1.35f, unit * 1.35f),
            size = Size(unit * 0.5f, unit * 0.5f),
        )
    }

    repeat(5) { index ->
        val y = size.height * (0.12f + index * 0.19f)
        drawRect(
            color = primary.copy(alpha = 0.42f + index % 2 * 0.10f),
            topLeft = Offset(unit * 0.78f, y),
            size = Size(unit * 0.40f, size.height * 0.09f),
        )
        drawRect(
            color = secondary.copy(alpha = 0.34f + index % 2 * 0.08f),
            topLeft = Offset(size.width - unit * 1.18f, y + unit * 0.6f),
            size = Size(unit * 0.40f, size.height * 0.07f),
        )
    }
}
