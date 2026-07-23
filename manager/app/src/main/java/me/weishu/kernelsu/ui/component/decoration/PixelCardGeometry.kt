package me.weishu.kernelsu.ui.component.decoration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale

internal enum class PixelCardPattern {
    Generic,
    Handheld,
    Arcade,
    Pastoral,
    StarVoyage,
    InkJade,
    Wasteland,
    Ocean,
    Cyber,
    ThreeKingdoms,
    Bianliang,
    FishingHarbor,
    TribalJungle,
    LavaValley,
    DunhuangDesert,
    VikingSnowfield,
    JiangnanWatertown,
    CloudTown,
}

internal fun DrawScope.drawPixelCardPattern(
    pattern: PixelCardPattern,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
    alpha: Float = 1f,
) {
    drawPixelCardPatternUnderlay(
        pattern = pattern,
        unit = unit,
        primary = primary,
        secondary = secondary,
        highlight = highlight,
        shadow = shadow,
        alpha = alpha,
    )
    drawPixelCardPatternOverlay(
        pattern = pattern,
        unit = unit,
        primary = primary,
        secondary = secondary,
        highlight = highlight,
        shadow = shadow,
        alpha = alpha,
    )
}

internal fun DrawScope.drawPixelCardPatternUnderlay(
    pattern: PixelCardPattern,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
    alpha: Float = 1f,
) {
    val primaryColor = primary.scaledAlpha(alpha * 0.72f)
    val secondaryColor = secondary.scaledAlpha(alpha * 0.74f)
    val highlightColor = highlight.scaledAlpha(alpha * 0.70f)
    val shadowColor = shadow.scaledAlpha(alpha * 0.76f)
    when (pattern) {
        PixelCardPattern.Generic -> {
            drawRect(secondaryColor, Offset(unit * 2f, 0f), Size(unit, unit))
            drawRect(
                secondaryColor,
                Offset(size.width - unit * 3f, size.height - unit),
                Size(unit, unit),
            )
        }

        PixelCardPattern.Handheld -> {
            val led = Offset(unit * 3f, size.height - unit * 4f)
            drawRect(shadowColor, led, Size(unit * 3.5f, unit))
            drawRect(secondaryColor, led, Size(unit, unit))
            repeat(2) { row ->
                repeat(4) { column ->
                    val side = unit * 0.62f
                    drawRect(
                        color = primaryColor.copy(alpha = primaryColor.alpha * 0.72f),
                        topLeft = Offset(
                            size.width - unit * (7f - column * 1.25f),
                            size.height - unit * (4.7f - row * 1.25f),
                        ),
                        size = Size(side, side),
                    )
                }
            }
        }

        PixelCardPattern.Arcade -> {
            repeat(3) { index ->
                val color = if (index % 2 == 0) primaryColor else secondaryColor
                drawRect(
                    color = color,
                    topLeft = Offset(unit * 2f, size.height - unit * (4.5f - index * 1.2f)),
                    size = Size(unit * (2.5f + index), unit * 0.65f),
                )
            }
            val stickX = size.width - unit * 5f
            drawRect(
                color = highlightColor,
                topLeft = Offset(stickX, size.height - unit * 5f),
                size = Size(unit, unit * 2f),
            )
            drawRect(
                color = secondaryColor,
                topLeft = Offset(stickX - unit, size.height - unit * 6f),
                size = Size(unit * 3f, unit * 2f),
            )
            drawRect(
                color = primaryColor,
                topLeft = Offset(stickX - unit * 2f, size.height - unit * 2.5f),
                size = Size(unit * 5f, unit),
            )
        }

        PixelCardPattern.Pastoral -> {
            val fenceY = size.height - unit * 3f
            drawRect(secondaryColor, Offset(unit * 2f, fenceY), Size(unit * 8f, unit * 0.72f))
            listOf(3f, 7f).forEach { x ->
                drawRect(
                    color = shadowColor,
                    topLeft = Offset(unit * x, fenceY - unit * 1.5f),
                    size = Size(unit * 0.7f, unit * 3.5f),
                )
            }
            drawPixelFlower(
                center = Offset(size.width - unit * 5f, size.height - unit * 3f),
                unit = unit,
                stem = primaryColor,
                bloom = secondaryColor,
                centerColor = highlightColor,
            )
        }

        PixelCardPattern.StarVoyage -> {
            val points = listOf(
                Offset(size.width - unit * 9f, size.height - unit * 3f),
                Offset(size.width - unit * 6f, size.height - unit * 5f),
                Offset(size.width - unit * 3f, size.height - unit * 3f),
            )
            drawLine(primaryColor.copy(alpha = primaryColor.alpha * 0.64f), points[0], points[1], unit * 0.5f)
            drawLine(primaryColor.copy(alpha = primaryColor.alpha * 0.64f), points[1], points[2], unit * 0.5f)
            points.forEachIndexed { index, point ->
                drawPixelCross(
                    center = point,
                    unit = if (index == 1) unit else unit * 0.7f,
                    color = if (index == 1) highlightColor else secondaryColor,
                )
            }
            drawRect(primaryColor, Offset(unit * 2f, size.height - unit * 3f), Size(unit * 5f, unit))
            drawRect(secondaryColor, Offset(unit * 2f, size.height - unit * 5f), Size(unit, unit * 3f))
        }

        PixelCardPattern.InkJade -> {
            repeat(3) { level ->
                drawRect(
                    color = primaryColor.copy(alpha = primaryColor.alpha * (0.58f + level * 0.12f)),
                    topLeft = Offset(
                        unit * (2f + level),
                        size.height - unit * (2f + level),
                    ),
                    size = Size(unit * (7f - level * 2f), unit),
                )
            }
            val sealOrigin = Offset(size.width - unit * 6f, size.height - unit * 6f)
            drawRect(
                color = secondaryColor,
                topLeft = sealOrigin,
                size = Size(unit * 3f, unit * 3f),
                style = Stroke(width = unit * 0.75f),
            )
            drawRect(
                color = primaryColor,
                topLeft = sealOrigin + Offset(unit, unit),
                size = Size(unit, unit),
            )
        }

        PixelCardPattern.Wasteland -> {
            listOf(
                Offset(unit * 2f, unit * 2f),
                Offset(size.width - unit * 3f, unit * 2f),
                Offset(unit * 2f, size.height - unit * 3f),
                Offset(size.width - unit * 3f, size.height - unit * 3f),
            ).forEach { origin ->
                drawRect(secondaryColor, origin, Size(unit, unit))
                drawRect(
                    shadowColor.copy(alpha = shadowColor.alpha * 0.60f),
                    origin + Offset(unit * 0.25f, unit * 0.25f),
                    Size(unit * 0.5f, unit * 0.5f),
                )
            }
            repeat(4) { index ->
                drawRect(
                    color = if (index % 2 == 0) primaryColor else shadowColor,
                    topLeft = Offset(unit * (3f + index * 2f), size.height - unit * 1.5f),
                    size = Size(unit * 1.4f, unit * 0.5f),
                )
            }
        }

        PixelCardPattern.Ocean -> {
            val currentColor = primaryColor.copy(alpha = primaryColor.alpha * 0.54f)
            val reefY = size.height - unit * 2.4f
            repeat(4) { index ->
                drawRect(
                    color = currentColor,
                    topLeft = Offset(unit * (2f + index * 2.2f), reefY - unit * (index % 2)),
                    size = Size(unit * 1.6f, unit * 0.55f),
                )
            }
            listOf(0f, 1.4f, 2.8f).forEachIndexed { index, offset ->
                val stemHeight = unit * (2.2f + index * 0.8f)
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(unit * (3f + offset), reefY - stemHeight),
                    size = Size(unit * 0.58f, stemHeight),
                )
            }
            val coralX = size.width - unit * 6f
            drawRect(secondaryColor, Offset(coralX, reefY - unit * 3f), Size(unit * 0.7f, unit * 3f))
            drawRect(secondaryColor, Offset(coralX - unit, reefY - unit * 2.4f), Size(unit * 2.7f, unit * 0.7f))
            drawRect(secondaryColor, Offset(coralX + unit, reefY - unit * 4f), Size(unit * 0.7f, unit * 2f))
            listOf(
                Offset(size.width - unit * 9f, size.height - unit * 6f),
                Offset(size.width - unit * 5f, size.height - unit * 8f),
                Offset(size.width - unit * 3f, size.height - unit * 5f),
            ).forEachIndexed { index, bubble ->
                val side = unit * (0.55f + index * 0.18f)
                drawRect(
                    color = highlightColor,
                    topLeft = bubble,
                    size = Size(side, side),
                    style = Stroke(width = unit * 0.35f),
                )
            }
            val railAlpha = primaryColor.alpha * 0.42f
            drawRect(
                color = primaryColor.copy(alpha = railAlpha),
                topLeft = Offset(unit, size.height * 0.28f),
                size = Size(unit * 0.45f, size.height * 0.44f),
            )
            drawRect(
                color = primaryColor.copy(alpha = railAlpha),
                topLeft = Offset(size.width - unit * 1.45f, size.height * 0.28f),
                size = Size(unit * 0.45f, size.height * 0.44f),
            )
            drawRect(secondaryColor, Offset(unit, size.height * 0.48f), Size(unit * 1.2f, unit * 0.6f))
            drawRect(
                secondaryColor,
                Offset(size.width - unit * 2.2f, size.height * 0.48f),
                Size(unit * 1.2f, unit * 0.6f),
            )
        }

        PixelCardPattern.ThreeKingdoms -> drawThreeKingdomsCardInterior(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.Bianliang -> drawBianliangCardInterior(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.FishingHarbor -> drawFishingHarborCardInterior(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.TribalJungle -> drawTribalJungleCardInterior(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.LavaValley -> drawLavaValleyCardInterior(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.DunhuangDesert -> drawDunhuangCardInterior(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.VikingSnowfield -> drawVikingCardInterior(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.JiangnanWatertown -> drawJiangnanCardInterior(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.CloudTown -> drawCloudTownCardInterior(
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.Cyber -> {
            val cyberMagenta = Color(0xFFFF3DAE).scaledAlpha(alpha * 0.78f)
            val codeOrigin = Offset(unit * 2f, size.height - unit * 6.5f)
            val codeSize = Size(unit * 9f, unit * 4.5f)
            drawRect(
                color = shadowColor.copy(alpha = shadowColor.alpha * 0.72f),
                topLeft = codeOrigin,
                size = codeSize,
            )
            drawRect(
                color = primaryColor.copy(alpha = primaryColor.alpha * 0.72f),
                topLeft = codeOrigin,
                size = codeSize,
                style = Stroke(width = unit * 0.42f),
            )
            listOf(secondaryColor, highlightColor, cyberMagenta).forEachIndexed { index, color ->
                drawRect(
                    color = color,
                    topLeft = codeOrigin + Offset(unit * (1f + index * 1.5f), unit * 0.75f),
                    size = Size(unit * 0.65f, unit * 0.65f),
                )
            }
            listOf(4.8f, 6.6f, 3.7f).forEachIndexed { index, width ->
                drawRect(
                    color = (if (index % 2 == 0) secondaryColor else highlightColor)
                        .copy(alpha = 0.46f),
                    topLeft = codeOrigin + Offset(unit, unit * (2f + index * 0.8f)),
                    size = Size(unit * width, unit * 0.36f),
                )
            }

            val chipSide = unit * 4.5f
            val chipOrigin = Offset(size.width - unit * 7f, size.height - unit * 6f)
            drawRect(
                color = primaryColor.copy(alpha = primaryColor.alpha * 0.64f),
                topLeft = chipOrigin,
                size = Size(chipSide, chipSide),
                style = Stroke(width = unit * 0.5f),
            )
            drawRect(
                color = shadowColor,
                topLeft = chipOrigin + Offset(unit, unit),
                size = Size(chipSide - unit * 2f, chipSide - unit * 2f),
            )
            repeat(3) { index ->
                val pin = unit * (1f + index * 1.25f)
                drawRect(secondaryColor, Offset(chipOrigin.x + pin, chipOrigin.y - unit), Size(unit * 0.42f, unit))
                drawRect(highlightColor, Offset(chipOrigin.x + pin, chipOrigin.y + chipSide), Size(unit * 0.42f, unit))
            }
            drawRect(
                color = cyberMagenta,
                topLeft = Offset(size.width - unit * 3f, size.height - unit * 2f),
                size = Size(unit, unit),
            )
            drawRect(
                color = secondaryColor,
                topLeft = Offset(size.width - unit * 5f, size.height - unit * 2f),
                size = Size(unit, unit),
            )
            drawRect(
                color = highlightColor.copy(alpha = highlightColor.alpha * 0.58f),
                topLeft = Offset(size.width * 0.49f, size.height * 0.38f),
                size = Size(unit * 0.45f, size.height * 0.24f),
            )
        }
    }
}

internal fun DrawScope.drawPixelCardPatternOverlay(
    pattern: PixelCardPattern,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
    alpha: Float = 1f,
) {
    val safeInset = pixelCardOverlayInset(unit, size.width, size.height)
    inset(safeInset, safeInset, safeInset, safeInset) {
        drawPixelCardDepthFrame(
            unit = unit,
            primary = primary.scaledAlpha(alpha),
            secondary = secondary.scaledAlpha(alpha),
            highlight = highlight.scaledAlpha(alpha),
            shadow = shadow.scaledAlpha(alpha),
        )
        drawPixelComponentFrame(
            unit = unit,
            primary = primary.scaledAlpha(alpha * 0.64f),
            secondary = secondary.scaledAlpha(alpha * 0.70f),
        )
        drawPixelPatternFramePolish(
            pattern = pattern,
            unit = unit,
            primary = primary,
            secondary = secondary,
            highlight = highlight,
            shadow = shadow,
            alpha = alpha,
        )
        drawPixelCardTopDecoration(
            pattern = pattern,
            unit = unit,
            primary = primary,
            secondary = secondary,
            highlight = highlight,
            shadow = shadow,
            alpha = alpha,
        )
    }
}

internal fun pixelCardOverlayInset(unit: Float, width: Float, height: Float): Float {
    if (unit <= 0f || width <= 0f || height <= 0f) return 0f
    return minOf(unit * 0.72f, width * 0.02f, height * 0.055f)
}

internal fun pixelCardTopDecorationHeight(unit: Float, width: Float, height: Float): Float {
    if (unit <= 0f || width < unit * 18f || height < unit * 10f) return 0f
    return minOf(unit * 4f, height * 0.16f)
}

internal fun pixelCardTopDecorationScale(unit: Float, availableHeight: Float): Float {
    if (unit <= 0f || availableHeight <= 0f) return 0f
    return (availableHeight / (unit * 4f)).coerceIn(0f, 1f)
}

private fun DrawScope.drawPixelCardTopDecoration(
    pattern: PixelCardPattern,
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
    alpha: Float,
) {
    if (pattern == PixelCardPattern.Generic) return
    val availableTopHeight = pixelCardTopDecorationHeight(unit, size.width, size.height)
    if (availableTopHeight == 0f) return
    val verticalScale = pixelCardTopDecorationScale(unit, availableTopHeight)
    if (verticalScale == 0f) return
    val topHeight = availableTopHeight / verticalScale

    val primaryColor = primary.scaledAlpha(alpha * 0.84f)
    val secondaryColor = secondary.scaledAlpha(alpha * 0.82f)
    val highlightColor = highlight.scaledAlpha(alpha * 0.76f)
    val shadowColor = shadow.scaledAlpha(alpha * 0.72f)

    scale(scaleX = 1f, scaleY = verticalScale, pivot = Offset.Zero) {
        drawPixelCardTopDecorationContent(
            pattern = pattern,
            unit = unit,
            topHeight = topHeight,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            highlightColor = highlightColor,
            shadowColor = shadowColor,
            alpha = alpha,
        )
    }
}

private fun DrawScope.drawPixelCardTopDecorationContent(
    pattern: PixelCardPattern,
    unit: Float,
    topHeight: Float,
    primaryColor: Color,
    secondaryColor: Color,
    highlightColor: Color,
    shadowColor: Color,
    alpha: Float,
) {
    when (pattern) {
        PixelCardPattern.Generic -> Unit
        PixelCardPattern.Handheld -> {
            drawRect(shadowColor, Offset(size.width * 0.36f, 0f), Size(size.width * 0.28f, unit * 0.7f))
            drawRect(
                primaryColor,
                Offset(size.width * 0.31f, unit * 0.7f),
                Size(size.width * 0.38f, topHeight - unit * 0.7f),
            )
            drawRect(
                highlightColor,
                Offset(size.width * 0.36f, unit * 1.35f),
                Size(size.width * 0.28f, unit * 0.45f),
            )
            listOf(0.34f, 0.64f).forEach { x ->
                drawRect(secondaryColor, Offset(size.width * x, topHeight - unit), Size(unit, unit))
            }
        }

        PixelCardPattern.Arcade -> {
            drawRect(
                shadowColor,
                Offset(size.width * 0.19f, topHeight - unit * 0.65f),
                Size(size.width * 0.62f, unit * 0.65f),
            )
            drawRect(
                primaryColor,
                Offset(size.width * 0.23f, unit * 0.65f),
                Size(size.width * 0.27f, topHeight - unit * 1.15f),
            )
            drawRect(
                secondaryColor,
                Offset(size.width * 0.50f, unit * 0.65f),
                Size(size.width * 0.27f, topHeight - unit * 1.15f),
            )
            drawRect(
                highlightColor,
                Offset(size.width * 0.29f, unit * 1.25f),
                Size(size.width * 0.42f, unit * 0.42f),
            )
            listOf(0.31f, 0.43f, 0.55f, 0.67f).forEachIndexed { index, x ->
                drawRect(
                    color = if (index % 2 == 0) secondaryColor else primaryColor,
                    topLeft = Offset(size.width * x, topHeight - unit * 1.35f),
                    size = Size(unit * 0.7f, unit * 0.7f),
                )
            }
        }

        PixelCardPattern.Pastoral -> {
            val railY = topHeight * 0.62f
            drawRect(shadowColor, Offset(size.width * 0.06f, railY + unit * 0.72f), Size(size.width * 0.88f, unit * 0.45f))
            drawRect(secondaryColor, Offset(size.width * 0.05f, railY), Size(size.width * 0.90f, unit * 0.72f))
            listOf(0.10f, 0.18f, 0.31f, 0.43f, 0.57f, 0.70f, 0.83f, 0.91f).forEachIndexed { index, x ->
                val blade = unit * (0.9f + index % 3 * 0.45f)
                drawRect(
                    primaryColor,
                    Offset(size.width * x, railY - blade),
                    Size(unit * 0.62f, blade),
                )
            }
            listOf(0.27f, 0.73f).forEachIndexed { index, x ->
                drawPixelFlower(
                    center = Offset(size.width * x, railY - unit * 0.75f),
                    unit = unit * 0.8f,
                    stem = primaryColor,
                    bloom = if (index == 0) secondaryColor else highlightColor,
                    centerColor = secondaryColor,
                )
            }
        }

        PixelCardPattern.StarVoyage -> {
            drawRect(shadowColor, Offset(size.width * 0.29f, topHeight - unit * 0.65f), Size(size.width * 0.42f, unit * 0.65f))
            drawRect(primaryColor, Offset(size.width * 0.35f, topHeight * 0.44f), Size(size.width * 0.30f, unit))
            drawRect(primaryColor, Offset(size.width * 0.43f, topHeight * 0.20f), Size(size.width * 0.14f, topHeight * 0.35f))
            drawRect(highlightColor, Offset(size.width * 0.47f, unit * 0.75f), Size(size.width * 0.06f, unit))
            drawRect(secondaryColor, Offset(size.width * 0.49f, 0f), Size(unit, unit * 0.9f))
            drawRect(primaryColor, Offset(size.width * 0.24f, topHeight * 0.62f), Size(size.width * 0.12f, unit * 0.65f))
            drawRect(primaryColor, Offset(size.width * 0.64f, topHeight * 0.62f), Size(size.width * 0.12f, unit * 0.65f))
            drawPixelCross(Offset(size.width * 0.16f, unit * 1.5f), unit * 0.7f, secondaryColor)
            drawPixelCross(Offset(size.width * 0.84f, unit), unit * 0.55f, highlightColor)
        }

        PixelCardPattern.InkJade -> {
            drawRect(shadowColor, Offset(size.width * 0.14f, topHeight - unit * 0.55f), Size(size.width * 0.72f, unit * 0.55f))
            drawRect(primaryColor, Offset(size.width * 0.18f, topHeight * 0.58f), Size(size.width * 0.64f, unit * 0.75f))
            drawRect(primaryColor, Offset(size.width * 0.28f, topHeight * 0.34f), Size(size.width * 0.44f, unit * 0.72f))
            drawRect(secondaryColor, Offset(size.width * 0.47f, 0f), Size(size.width * 0.06f, topHeight * 0.42f))
            drawRect(highlightColor, Offset(size.width * 0.35f, topHeight * 0.52f), Size(size.width * 0.30f, unit * 0.34f))
            listOf(0.14f, 0.84f).forEach { x ->
                drawRect(secondaryColor, Offset(size.width * x, topHeight * 0.63f), Size(unit * 0.7f, unit * 1.2f))
                drawRect(primaryColor, Offset(size.width * x - unit * 0.45f, topHeight - unit * 0.5f), Size(unit * 1.6f, unit * 0.5f))
            }
        }

        PixelCardPattern.Wasteland -> {
            drawRect(shadowColor, Offset(size.width * 0.06f, topHeight - unit * 0.58f), Size(size.width * 0.88f, unit * 0.58f))
            val plates = listOf(
                Triple(0.08f, 0.20f, 0.58f),
                Triple(0.29f, 0.18f, 0.82f),
                Triple(0.49f, 0.16f, 0.64f),
                Triple(0.67f, 0.24f, 0.76f),
            )
            plates.forEachIndexed { index, (x, width, heightFraction) ->
                val plateHeight = topHeight * heightFraction
                drawRect(
                    color = if (index % 2 == 0) primaryColor else shadowColor,
                    topLeft = Offset(size.width * x, topHeight - plateHeight),
                    size = Size(size.width * width, plateHeight - unit * 0.25f),
                )
                drawRect(
                    color = secondaryColor,
                    topLeft = Offset(size.width * x + unit * 0.7f, topHeight - unit * 1.15f),
                    size = Size(unit * 0.65f, unit * 0.65f),
                )
            }
            repeat(3) { index ->
                drawRect(
                    color = if (index == 1) highlightColor else secondaryColor,
                    topLeft = Offset(size.width * (0.40f + index * 0.07f), unit * 0.55f),
                    size = Size(size.width * 0.045f, unit * 0.55f),
                )
            }
        }

        PixelCardPattern.Ocean -> {
            val waveY = topHeight * 0.54f
            drawRect(
                color = shadowColor,
                topLeft = Offset(size.width * 0.05f, topHeight - unit * 0.52f),
                size = Size(size.width * 0.90f, unit * 0.52f),
            )
            repeat(10) { index ->
                val segmentWidth = size.width * 0.09f
                val x = size.width * 0.05f + segmentWidth * index
                val lift = if (index % 2 == 0) unit * 0.65f else 0f
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(x, waveY - lift),
                    size = Size(segmentWidth + unit * 0.15f, unit * 0.72f),
                )
                if (index % 3 == 1) {
                    drawRect(
                        color = highlightColor,
                        topLeft = Offset(x + unit * 0.35f, waveY - lift - unit * 0.48f),
                        size = Size(segmentWidth * 0.45f, unit * 0.42f),
                    )
                }
            }
            val buoyX = size.width * 0.78f
            drawRect(
                color = secondaryColor,
                topLeft = Offset(buoyX, unit * 0.35f),
                size = Size(unit * 0.72f, waveY - unit * 0.25f),
            )
            drawRect(
                color = highlightColor,
                topLeft = Offset(buoyX - unit * 0.45f, 0f),
                size = Size(unit * 1.6f, unit * 0.62f),
            )
        }

        PixelCardPattern.ThreeKingdoms -> drawThreeKingdomsCardTop(
            topHeight = topHeight,
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.Bianliang -> drawBianliangCardTop(
            topHeight = topHeight,
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.FishingHarbor -> drawFishingHarborCardTop(
            topHeight = topHeight,
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.TribalJungle -> drawTribalJungleCardTop(
            topHeight = topHeight,
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.LavaValley -> drawLavaValleyCardTop(
            topHeight = topHeight,
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.DunhuangDesert -> drawDunhuangCardTop(
            topHeight = topHeight,
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.VikingSnowfield -> drawVikingCardTop(
            topHeight = topHeight,
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.JiangnanWatertown -> drawJiangnanCardTop(
            topHeight = topHeight,
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.CloudTown -> drawCloudTownCardTop(
            topHeight = topHeight,
            unit = unit,
            primary = primaryColor,
            secondary = secondaryColor,
            highlight = highlightColor,
            shadow = shadowColor,
        )

        PixelCardPattern.Cyber -> {
            val cyberMagenta = Color(0xFFFF3DAE).scaledAlpha(alpha * 0.88f)
            drawRect(
                color = shadowColor,
                topLeft = Offset(size.width * 0.06f, topHeight - unit * 0.55f),
                size = Size(size.width * 0.88f, unit * 0.55f),
            )
            drawRect(
                color = primaryColor,
                topLeft = Offset(size.width * 0.09f, unit * 0.55f),
                size = Size(size.width * 0.31f, unit * 0.7f),
            )
            drawRect(
                color = highlightColor,
                topLeft = Offset(size.width * 0.43f, unit * 0.55f),
                size = Size(size.width * 0.14f, unit * 0.7f),
            )
            drawRect(
                color = cyberMagenta,
                topLeft = Offset(size.width * 0.60f, unit * 0.55f),
                size = Size(size.width * 0.12f, unit * 0.7f),
            )
            drawRect(
                color = secondaryColor,
                topLeft = Offset(size.width * 0.75f, unit * 0.55f),
                size = Size(size.width * 0.16f, unit * 0.7f),
            )
            repeat(7) { index ->
                val x = size.width * (0.12f + index * 0.115f)
                val height = unit * (0.65f + index % 3 * 0.38f)
                drawRect(
                    color = if (index % 2 == 0) primaryColor else secondaryColor,
                    topLeft = Offset(x, topHeight - height),
                    size = Size(unit * 0.45f, height),
                )
            }
            listOf(0.07f, 0.93f).forEachIndexed { index, x ->
                drawRect(
                    color = if (index == 0) highlightColor else cyberMagenta,
                    topLeft = Offset(size.width * x - unit * 0.5f, topHeight * 0.34f),
                    size = Size(unit, unit),
                )
            }
        }
    }
}

private fun DrawScope.drawPixelCardDepthFrame(
    unit: Float,
    primary: Color,
    secondary: Color,
    highlight: Color,
    shadow: Color,
) {
    if (unit <= 0f || size.width < unit * 18f || size.height < unit * 10f) return
    val inset = unit * 1.45f
    val frameSize = Size(size.width - inset * 2f, size.height - inset * 2f)
    drawRect(
        color = primary.copy(alpha = primary.alpha * 0.24f),
        topLeft = Offset(inset, inset),
        size = frameSize,
        style = Stroke(width = unit * 0.28f),
    )
    drawRect(
        color = highlight.copy(alpha = highlight.alpha * 0.42f),
        topLeft = Offset(size.width * 0.19f, inset),
        size = Size(size.width * 0.28f, unit * 0.32f),
    )
    drawRect(
        color = secondary.copy(alpha = secondary.alpha * 0.46f),
        topLeft = Offset(size.width * 0.53f, inset),
        size = Size(size.width * 0.18f, unit * 0.32f),
    )
    drawRect(
        color = shadow.copy(alpha = shadow.alpha * 0.38f),
        topLeft = Offset(size.width * 0.28f, size.height - inset - unit * 0.32f),
        size = Size(size.width * 0.44f, unit * 0.32f),
    )
    listOf(0.30f, 0.70f).forEachIndexed { index, fraction ->
        drawRect(
            color = if (index == 0) secondary.copy(alpha = secondary.alpha * 0.56f)
            else highlight.copy(alpha = highlight.alpha * 0.48f),
            topLeft = Offset(size.width * fraction - unit * 0.38f, size.height - inset - unit * 0.38f),
            size = Size(unit * 0.76f, unit * 0.76f),
        )
    }
}

private fun DrawScope.drawPixelComponentFrame(
    unit: Float,
    primary: Color,
    secondary: Color,
) {
    val long = unit * 4.5f
    drawRect(primary, Offset.Zero, Size(long, unit))
    drawRect(primary, Offset.Zero, Size(unit, long))
    drawRect(primary, Offset(size.width - long, 0f), Size(long, unit))
    drawRect(primary, Offset(size.width - unit, 0f), Size(unit, long))
    drawRect(primary, Offset(0f, size.height - unit), Size(long, unit))
    drawRect(primary, Offset(0f, size.height - long), Size(unit, long))
    drawRect(primary, Offset(size.width - long, size.height - unit), Size(long, unit))
    drawRect(primary, Offset(size.width - unit, size.height - long), Size(unit, long))
    drawRect(secondary, Offset(unit * 2f, 0f), Size(unit, unit))
    drawRect(secondary, Offset(size.width - unit * 3f, size.height - unit), Size(unit, unit))
}

private fun DrawScope.drawPixelCross(center: Offset, unit: Float, color: Color) {
    drawRect(color, center - Offset(unit * 0.5f, unit * 1.5f), Size(unit, unit * 3f))
    drawRect(color, center - Offset(unit * 1.5f, unit * 0.5f), Size(unit * 3f, unit))
}

private fun DrawScope.drawPixelFlower(
    center: Offset,
    unit: Float,
    stem: Color,
    bloom: Color,
    centerColor: Color,
) {
    drawRect(stem, center + Offset(-unit * 0.25f, unit * 0.5f), Size(unit * 0.5f, unit * 1.8f))
    drawRect(bloom, center + Offset(-unit * 1.2f, -unit * 0.45f), Size(unit, unit))
    drawRect(bloom, center + Offset(unit * 0.2f, -unit * 0.45f), Size(unit, unit))
    drawRect(bloom, center + Offset(-unit * 0.5f, -unit * 1.15f), Size(unit, unit))
    drawRect(centerColor, center + Offset(-unit * 0.35f, -unit * 0.3f), Size(unit * 0.7f, unit * 0.7f))
}

private fun Color.scaledAlpha(multiplier: Float): Color = copy(
    alpha = (alpha * multiplier).coerceIn(0f, 1f),
)
