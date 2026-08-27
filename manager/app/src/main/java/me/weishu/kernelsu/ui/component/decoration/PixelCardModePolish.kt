package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawPixelPatternFramePolish(
    pattern: PixelCardPattern,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
    alpha: Float,
) {
    if (!pixelPatternFramePolishEnabled(unit, size.width, size.height)) return
    val primaryColor = primary.pixelAlpha(alpha * 0.52f)
    val secondaryColor = secondary.pixelAlpha(alpha * 0.56f)
    val highlightColor = highlight.pixelAlpha(alpha * 0.46f)
    val shadowColor = shadow.pixelAlpha(alpha * 0.58f)
    when (pattern) {
        PixelCardPattern.Generic,
        PixelCardPattern.PetCompanion,
        PixelCardPattern.Ocean,
        PixelCardPattern.Cyber,
        -> Unit

        PixelCardPattern.ThreeKingdoms -> drawThreeKingdomsFramePolish(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.Bianliang -> drawBianliangFramePolish(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.FishingHarbor -> drawFishingHarborFramePolish(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.TribalJungle -> drawTribalJungleFramePolish(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.LavaValley -> drawLavaValleyFramePolish(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.DunhuangDesert -> drawDunhuangFramePolish(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.VikingSnowfield -> drawVikingFramePolish(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.JiangnanWatertown -> drawJiangnanFramePolish(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.CloudTown -> drawCloudTownFramePolish(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.Handheld -> drawHandheldFramePolish(
            unit,
            primaryColor,
            secondaryColor,
            highlightColor,
            shadowColor,
        )

        PixelCardPattern.Arcade -> drawArcadeFramePolish(
            unit,
            primaryColor,
            secondaryColor,
            highlightColor,
            shadowColor,
        )

        PixelCardPattern.Pastoral -> drawPastoralFramePolish(
            unit,
            primaryColor,
            secondaryColor,
            highlightColor,
            shadowColor,
        )

        PixelCardPattern.StarVoyage -> drawStarVoyageFramePolish(
            unit,
            primaryColor,
            secondaryColor,
            highlightColor,
            shadowColor,
        )

        PixelCardPattern.InkJade -> drawInkJadeFramePolish(
            unit,
            primaryColor,
            secondaryColor,
            highlightColor,
            shadowColor,
        )

        PixelCardPattern.Wasteland -> drawWastelandFramePolish(
            unit,
            primaryColor,
            secondaryColor,
            highlightColor,
            shadowColor,
        )
    }
}

internal fun pixelPatternFramePolishEnabled(unit: Float, width: Float, height: Float): Boolean {
    return unit > 0f && width >= unit * 18f && height >= unit * 10f
}

private fun DrawScope.drawHandheldFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val inset = unit * 1.45f
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.55f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(width = unit * 0.38f),
    )
    drawRect(shadow, Offset(size.width * 0.31f, 0f), Size(size.width * 0.38f, unit * 0.55f))
    drawRect(primary, Offset(size.width * 0.36f, unit * 0.55f), Size(size.width * 0.28f, unit * 0.55f))
    listOf(0.07f, 0.93f).forEachIndexed { index, x ->
        drawRect(
            color = if (index == 0) secondary else highlight,
            topLeft = Offset(size.width * x - unit * 0.5f, unit * 2.2f),
            size = Size(unit, unit * 2.5f),
        )
    }
    val ledY = size.height - unit * 2.5f
    drawRect(shadow, Offset(unit * 2f, ledY), Size(unit * 5f, unit))
    drawRect(secondary, Offset(unit * 2f, ledY), Size(unit, unit))
    repeat(2) { row ->
        repeat(5) { column ->
            drawRect(
                color = primary.copy(alpha = primary.alpha * 0.70f),
                topLeft = Offset(
                    size.width - unit * (8f - column * 1.2f),
                    size.height - unit * (3.6f - row * 1.2f),
                ),
                size = Size(unit * 0.55f, unit * 0.55f),
            )
        }
    }
}

private fun DrawScope.drawArcadeFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    drawRect(primary, Offset(unit * 5f, 0f), Size(size.width * 0.38f, unit * 0.65f))
    drawRect(secondary, Offset(size.width * 0.52f, 0f), Size(size.width * 0.38f, unit * 0.65f))
    drawRect(secondary, Offset(unit * 5f, size.height - unit * 0.65f), Size(size.width * 0.30f, unit * 0.65f))
    drawRect(primary, Offset(size.width * 0.60f, size.height - unit * 0.65f), Size(size.width * 0.30f, unit * 0.65f))
    listOf(unit * 1.6f, size.width - unit * 2.2f).forEachIndexed { index, x ->
        drawRect(
            color = if (index == 0) primary else secondary,
            topLeft = Offset(x, unit * 4f),
            size = Size(unit * 0.6f, size.height - unit * 8f),
        )
    }
    repeat(7) { index ->
        val color = when (index % 3) {
            0 -> primary
            1 -> highlight
            else -> secondary
        }
        drawRect(
            color = color,
            topLeft = Offset(size.width * (0.24f + index * 0.085f), unit * 1.8f),
            size = Size(unit * 0.65f, unit * 0.65f),
        )
    }
    repeat(5) { index ->
        val barHeight = unit * (0.6f + index % 3 * 0.55f)
        drawRect(
            color = if (index % 2 == 0) primary else secondary,
            topLeft = Offset(unit * (2f + index * 1.25f), size.height - unit - barHeight),
            size = Size(unit * 0.55f, barHeight),
        )
    }
    drawRect(shadow, Offset(size.width * 0.38f, size.height - unit), Size(size.width * 0.24f, unit))
}

private fun DrawScope.drawPastoralFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val topRail = unit * 1.3f
    val bottomRail = size.height - unit * 1.4f
    drawRect(shadow, Offset(unit * 4f, topRail + unit * 0.65f), Size(size.width - unit * 8f, unit * 0.48f))
    drawRect(secondary, Offset(unit * 3f, topRail), Size(size.width - unit * 6f, unit * 0.65f))
    drawRect(secondary, Offset(unit * 3f, bottomRail), Size(size.width - unit * 6f, unit * 0.65f))
    listOf(0.09f, 0.25f, 0.75f, 0.91f).forEachIndexed { index, x ->
        val postHeight = unit * (2.1f + index % 2 * 0.7f)
        drawRect(
            color = shadow,
            topLeft = Offset(size.width * x, bottomRail - postHeight),
            size = Size(unit * 0.65f, postHeight + unit * 1.2f),
        )
    }
    repeat(4) { index ->
        val y = size.height - unit * (2.3f + index * 0.72f)
        drawRect(
            color = primary.copy(alpha = primary.alpha * (0.48f + index * 0.10f)),
            topLeft = Offset(unit * (2f + index), y),
            size = Size(size.width * (0.18f - index * 0.018f), unit * 0.38f),
        )
    }
    drawTinyPixelBloom(Offset(unit * 5f, topRail), unit * 0.72f, primary, secondary, highlight)
    drawTinyPixelBloom(
        Offset(size.width - unit * 5f, topRail),
        unit * 0.72f,
        primary,
        highlight,
        secondary,
    )
}

private fun DrawScope.drawStarVoyageFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val inset = unit * 1.6f
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.62f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        style = Stroke(width = unit * 0.38f),
    )
    drawRect(shadow, Offset(size.width * 0.32f, 0f), Size(size.width * 0.36f, unit * 0.55f))
    drawRect(primary, Offset(size.width * 0.39f, unit * 0.55f), Size(size.width * 0.22f, unit * 0.55f))
    listOf(0.12f, 0.31f, 0.69f, 0.88f).forEachIndexed { index, x ->
        val color = if (index % 2 == 0) highlight else secondary
        drawRect(
            color = color,
            topLeft = Offset(size.width * x, if (index < 2) unit * 1.5f else size.height - unit * 2.1f),
            size = Size(unit * 0.75f, unit * 0.75f),
        )
    }
    listOf(unit * 2.4f, size.width - unit * 3f).forEachIndexed { index, x ->
        repeat(3) { row ->
            drawRect(
                color = (if (index == 0) primary else secondary).copy(alpha = primary.alpha * 0.72f),
                topLeft = Offset(x, size.height * 0.39f + row * unit * 1.45f),
                size = Size(unit * 0.6f, unit),
            )
        }
    }
    drawLine(
        color = primary.copy(alpha = primary.alpha * 0.42f),
        start = Offset(size.width * 0.22f, size.height - unit * 1.4f),
        end = Offset(size.width * 0.44f, size.height - unit * 1.4f),
        strokeWidth = unit * 0.4f,
    )
}

private fun DrawScope.drawInkJadeFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val upperY = unit * 1.2f
    val lowerY = size.height - unit * 1.5f
    listOf(0.12f to 0.76f, 0.20f to 0.60f, 0.29f to 0.42f).forEachIndexed { index, (start, width) ->
        drawRect(
            color = primary.copy(alpha = primary.alpha * (0.52f + index * 0.12f)),
            topLeft = Offset(size.width * start, upperY + index * unit * 0.62f),
            size = Size(size.width * width, unit * 0.48f),
        )
    }
    drawRect(shadow, Offset(size.width * 0.18f, lowerY), Size(size.width * 0.64f, unit * 0.55f))
    drawRect(primary, Offset(size.width * 0.26f, lowerY - unit * 0.6f), Size(size.width * 0.48f, unit * 0.6f))
    drawRect(secondary, Offset(size.width * 0.46f, lowerY - unit * 1.2f), Size(size.width * 0.08f, unit * 0.6f))
    listOf(unit * 1.8f, size.width - unit * 2.4f).forEachIndexed { index, x ->
        repeat(3) { level ->
            val width = unit * (2.8f - level * 0.65f)
            drawRect(
                color = if (index == 0) primary else secondary,
                topLeft = Offset(
                    if (index == 0) x else x - width,
                    size.height * 0.33f + level * unit * 0.72f,
                ),
                size = Size(width, unit * 0.45f),
            )
        }
    }
    val seal = Offset(size.width - unit * 5f, size.height - unit * 5f)
    drawRect(secondary, seal, Size(unit * 2.5f, unit * 2.5f), style = Stroke(width = unit * 0.55f))
    drawRect(highlight, seal + Offset(unit * 0.85f, unit * 0.85f), Size(unit * 0.8f, unit * 0.8f))
}

private fun DrawScope.drawWastelandFramePolish(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    val topPlates = listOf(
        Triple(0.05f, 0.22f, primary),
        Triple(0.31f, 0.17f, shadow),
        Triple(0.52f, 0.28f, primary),
        Triple(0.84f, 0.11f, shadow),
    )
    topPlates.forEachIndexed { index, (x, width, color) ->
        drawRect(
            color = color.copy(alpha = color.alpha * (0.72f + index % 2 * 0.18f)),
            topLeft = Offset(size.width * x, unit * (index % 2) * 0.55f),
            size = Size(size.width * width, unit * (0.7f + index % 2 * 0.35f)),
        )
    }
    val bottomY = size.height - unit * 1.1f
    listOf(0.04f to 0.31f, 0.40f to 0.18f, 0.64f to 0.32f).forEachIndexed { index, (x, width) ->
        drawRect(
            color = (if (index == 1) secondary else shadow),
            topLeft = Offset(size.width * x, bottomY + index % 2 * unit * 0.35f),
            size = Size(size.width * width, unit * 0.72f),
        )
    }
    listOf(
        Offset(unit * 2.2f, unit * 2.2f),
        Offset(size.width - unit * 3.2f, unit * 2.8f),
        Offset(unit * 2.7f, size.height - unit * 3.2f),
        Offset(size.width - unit * 3.7f, size.height - unit * 3.7f),
    ).forEachIndexed { index, bolt ->
        drawRect(
            color = if (index == 1) highlight else secondary,
            topLeft = bolt,
            size = Size(unit * 0.85f, unit * 0.85f),
        )
        drawRect(
            color = shadow,
            topLeft = bolt + Offset(unit * 0.23f, unit * 0.23f),
            size = Size(unit * 0.39f, unit * 0.39f),
        )
    }
    repeat(4) { index ->
        drawRect(
            color = if (index % 2 == 0) primary else shadow,
            topLeft = Offset(unit * 1.4f, size.height * (0.34f + index * 0.09f)),
            size = Size(unit * (1.6f + index % 2), unit * 0.45f),
        )
    }
}

private fun DrawScope.drawTinyPixelBloom(
    center: Offset,
    unit: Float,
    stem: Color,
    bloom: Color,
    core: Color,
) {
    drawRect(stem, center + Offset(-unit * 0.2f, unit * 0.35f), Size(unit * 0.4f, unit * 1.5f))
    drawRect(bloom, center + Offset(-unit, -unit * 0.45f), Size(unit * 0.8f, unit * 0.8f))
    drawRect(bloom, center + Offset(unit * 0.2f, -unit * 0.45f), Size(unit * 0.8f, unit * 0.8f))
    drawRect(core, center + Offset(-unit * 0.3f, -unit * 0.35f), Size(unit * 0.6f, unit * 0.6f))
}

private fun Color.pixelAlpha(multiplier: Float): Color = copy(
    alpha = (alpha * multiplier).coerceIn(0f, 1f),
)
