package me.weishu.kernelsu.ui.component.pixel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.isInDarkTheme

@Composable
fun PixelCloudTownMotto(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle() || LocalPixelStyle.current != PixelStyle.CloudTown) return
    val palette = pixelPalette(PixelStyle.CloudTown, isInDarkTheme())
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.94f), shape)
            .border(1.dp, palette.secondary.copy(alpha = 0.62f), shape)
            .drawWithContent {
                drawContent()
                drawCloudTownMottoFrame(palette, 2.dp.toPx())
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pixel_cloud_town_motto),
            modifier = Modifier.padding(horizontal = 42.dp),
            color = palette.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun DrawScope.drawCloudTownScene(
    palette: PixelPalette,
    progress: Float,
    dark: Boolean,
) {
    val unit = 5.dp.toPx()

    if (dark) {
        repeat(5) { band ->
            drawRoundRect(
                color = palette.secondary.copy(alpha = 0.032f + band * 0.010f),
                topLeft = Offset(size.width * (0.04f + band * 0.05f), size.height * (0.09f + band * 0.055f)),
                size = Size(size.width * (0.84f - band * 0.08f), unit * (1.2f + band * 0.18f)),
                cornerRadius = CornerRadius(unit * 0.55f),
            )
        }
    } else {
        drawRoundRect(
            color = palette.highlight.copy(alpha = 0.18f),
            topLeft = Offset(size.width * 0.08f, size.height * 0.12f),
            size = Size(size.width * 0.84f, unit * 2.2f),
            cornerRadius = CornerRadius(unit),
        )
    }

    repeat(7) { index ->
        val cloudWidth = unit * (7f + index % 3 * 2.4f)
        val travel = size.width + cloudWidth
        val x = (
            size.width * ((index * 19 % 89) / 89f) +
                progress * travel * (0.035f + index % 3 * 0.010f)
            ) % travel - cloudWidth
        val y = size.height * (0.10f + (index * 0.113f) % 0.54f)
        drawCloudCluster(
            origin = Offset(x, y),
            unit = unit * (0.56f + index % 2 * 0.08f),
            palette = palette,
            alpha = 0.20f + index % 3 * 0.045f,
        )
    }

    drawCloudIsland(
        origin = Offset(size.width * 0.10f, size.height * 0.46f),
        unit = unit,
        widthUnits = 18f,
        palette = palette,
    )
    drawCloudHouse(
        origin = Offset(size.width * 0.17f, size.height * 0.39f),
        unit = unit * 0.76f,
        palette = palette,
        dark = dark,
    )
    drawCloudWindmill(
        origin = Offset(size.width * 0.32f, size.height * 0.405f),
        unit = unit * 0.62f,
        palette = palette,
    )

    drawCloudIsland(
        origin = Offset(size.width * 0.57f, size.height * 0.65f),
        unit = unit * 0.90f,
        widthUnits = 16f,
        palette = palette,
    )
    drawCloudHouse(
        origin = Offset(size.width * 0.64f, size.height * 0.59f),
        unit = unit * 0.68f,
        palette = palette,
        dark = dark,
    )
    drawCloudCrystal(
        origin = Offset(size.width * 0.79f, size.height * 0.60f),
        unit = unit * 0.58f,
        palette = palette,
    )

    drawCloudIsland(
        origin = Offset(size.width * 0.37f, size.height * 0.80f),
        unit = unit * 0.72f,
        widthUnits = 11f,
        palette = palette,
    )
    drawCloudDandelionPatch(
        origin = Offset(size.width * 0.41f, size.height * 0.755f),
        unit = unit * 0.52f,
        palette = palette,
    )
    drawCloudSheep(
        origin = Offset(size.width * 0.48f, size.height * 0.765f),
        unit = unit * 0.42f,
        palette = palette,
    )

    drawCloudBalloon(
        origin = Offset(size.width * 0.76f, size.height * 0.24f),
        unit = unit * 0.82f,
        palette = palette,
    )

    repeat(5) { index ->
        drawCloudBird(
            origin = Offset(size.width * (0.08f + index * 0.15f), size.height * (0.24f + index % 2 * 0.055f)),
            unit = unit * (0.38f + index % 2 * 0.07f),
            palette = palette,
        )
    }

    repeat(if (dark) 13 else 7) { index ->
        val x = size.width * ((index * 43 % 97) / 97f)
        val y = size.height * (0.10f + (index * 0.137f) % 0.70f)
        drawCloudStar(
            center = Offset(x, y),
            unit = unit * (0.22f + index % 3 * 0.08f),
            palette = palette,
            violet = index % 4 == 1,
            alpha = if (dark) 0.52f else 0.22f,
        )
    }

    repeat(18) { index ->
        val travelX = size.width + unit * 5f
        val travelY = size.height + unit * 5f
        val x = (
            size.width * ((index * 29 % 101) / 101f) +
                progress * travelX * (0.05f + index % 4 * 0.012f)
            ) % travelX - unit * 2f
        val y = (
            size.height * ((index * 53 % 103) / 103f) -
                progress * travelY * (0.08f + index % 3 * 0.018f)
            ).let { value -> (value % travelY + travelY) % travelY - unit * 2f }
        drawCloudDandelionSeed(
            origin = Offset(x, y),
            unit = unit * (0.26f + index % 2 * 0.07f),
            palette = palette,
            alpha = 0.26f + index % 3 * 0.045f,
        )
    }
}

internal fun DrawScope.drawCloudTownCardMaterial(
    palette: PixelPalette,
    line: Float,
) {
    repeat(4) { index ->
        drawRoundRect(
            color = (if (index == 2) palette.secondary else palette.highlight).copy(alpha = 0.026f + index * 0.006f),
            topLeft = Offset(size.width * (0.07f + index * 0.13f), size.height * (0.18f + index * 0.17f)),
            size = Size(size.width * (0.30f - index * 0.025f), line * (1.0f + index % 2 * 0.35f)),
            cornerRadius = CornerRadius(line * 0.8f),
        )
    }
    repeat(5) { index ->
        drawRoundRect(
            color = palette.primary.copy(alpha = 0.020f + index % 2 * 0.008f),
            topLeft = Offset(size.width * (0.42f + index * 0.10f), size.height - line * (4.6f - index % 2)),
            size = Size(size.width * 0.11f, line * 0.55f),
            cornerRadius = CornerRadius(line * 0.4f),
        )
    }
}

internal fun DrawScope.drawCloudTownHudAccent(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    val centerX = size.width / 2f
    repeat(5) { index ->
        val width = unit * (2.2f + index % 3 * 0.8f)
        val y = top + unit * (0.65f + kotlin.math.abs(index - 2) * 0.34f)
        drawRoundRect(
            color = (if (index == 2) palette.secondary else palette.highlight).copy(alpha = 0.50f + index % 2 * 0.06f),
            topLeft = Offset(centerX - unit * 6f + index * unit * 2.3f, y),
            size = Size(width, unit * 0.65f),
            cornerRadius = CornerRadius(unit * 0.45f),
        )
    }
}

internal fun DrawScope.drawCloudTownModeHudPolish(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    drawCloudMiniBalloon(Offset(unit * 6.5f, top + unit * 0.35f), unit * 0.48f, palette)
    drawCloudCrystal(Offset(size.width - unit * 7.5f, top + unit * 0.65f), unit * 0.34f, palette)
}

internal fun DrawScope.drawCloudTownNavigationAccent(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(8) { index ->
        val width = size.width * (0.09f + index % 3 * 0.015f)
        val lift = unit * (index % 3 * 0.35f)
        drawRoundRect(
            color = palette.highlight.copy(alpha = 0.34f + index % 2 * 0.06f),
            topLeft = Offset(size.width * (0.08f + index * 0.11f), size.height - unit * 1.15f - lift),
            size = Size(width, unit * (0.60f + index % 2 * 0.15f)),
            cornerRadius = CornerRadius(unit * 0.45f),
        )
    }
    drawCloudMiniIsland(
        origin = Offset(size.width * 0.41f, unit * 0.45f),
        unit = unit * 0.42f,
        palette = palette,
    )
    drawCloudMiniBalloon(Offset(size.width - unit * 7f, unit * 0.45f), unit * 0.36f, palette)
}

internal fun DrawScope.drawCloudTownModeNavigationFrame(
    palette: PixelPalette,
    unit: Float,
) {
    drawRoundRect(
        color = palette.secondary.copy(alpha = 0.46f),
        topLeft = Offset(size.width * 0.34f, 0f),
        size = Size(size.width * 0.32f, unit * 0.30f),
        cornerRadius = CornerRadius(unit * 0.20f),
    )
    listOf(0.13f, 0.87f).forEachIndexed { index, x ->
        drawCloudStar(
            center = Offset(size.width * x, unit * 1.8f),
            unit = unit * 0.32f,
            palette = palette,
            violet = index == 1,
            alpha = 0.56f,
        )
    }
}

internal fun DrawScope.drawCloudTownIndicatorAccent(
    palette: PixelPalette,
    unit: Float,
) {
    drawCloudMiniBalloon(
        origin = Offset(size.width - unit * 5.1f, unit * 0.5f),
        unit = unit * 0.52f,
        palette = palette,
    )
    drawRoundRect(
        color = palette.highlight.copy(alpha = 0.46f),
        topLeft = Offset(unit * 2f, size.height - unit * 1.0f),
        size = Size(size.width * 0.24f, unit * 0.36f),
        cornerRadius = CornerRadius(unit * 0.25f),
    )
}

internal fun DrawScope.drawCloudTownModeIndicatorPolish(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(3) { index ->
        drawCloudStar(
            center = Offset(size.width * (0.22f + index * 0.13f), unit * (1.1f + index % 2 * 0.35f)),
            unit = unit * (0.24f + index * 0.045f),
            palette = palette,
            violet = index == 1,
            alpha = 0.50f + index * 0.05f,
        )
    }
}

private fun DrawScope.drawCloudCluster(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
    alpha: Float,
) {
    listOf(
        Offset(0f, unit * 1.2f) to Size(unit * 7f, unit * 2.2f),
        Offset(unit * 1.2f, unit * 0.45f) to Size(unit * 2.8f, unit * 2.4f),
        Offset(unit * 3.2f, 0f) to Size(unit * 2.9f, unit * 2.8f),
        Offset(unit * 5.0f, unit * 0.8f) to Size(unit * 2.6f, unit * 1.9f),
    ).forEachIndexed { index, (offset, cloudSize) ->
        drawRoundRect(
            color = palette.highlight.copy(alpha = alpha + index % 2 * 0.035f),
            topLeft = origin + offset,
            size = cloudSize,
            cornerRadius = CornerRadius(unit * 1.05f),
        )
    }
}

private fun DrawScope.drawCloudIsland(
    origin: Offset,
    unit: Float,
    widthUnits: Float,
    palette: PixelPalette,
) {
    val width = unit * widthUnits
    drawRoundRect(
        color = palette.primary.copy(alpha = 0.38f),
        topLeft = origin,
        size = Size(width, unit * 1.25f),
        cornerRadius = CornerRadius(unit * 0.65f),
    )
    repeat(5) { layer ->
        val layerWidth = width * (0.82f - layer * 0.11f)
        drawRoundRect(
            color = palette.outline.copy(alpha = 0.20f + layer * 0.025f),
            topLeft = origin + Offset((width - layerWidth) / 2f, unit * (1.15f + layer * 0.8f)),
            size = Size(layerWidth, unit * 0.9f),
            cornerRadius = CornerRadius(unit * 0.40f),
        )
    }
    drawCloudCluster(
        origin = origin + Offset(width * 0.22f, unit * 4.5f),
        unit = unit * 0.58f,
        palette = palette,
        alpha = 0.18f,
    )
}

private fun DrawScope.drawCloudHouse(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
    dark: Boolean,
) {
    drawRoundRect(
        color = palette.highlight.copy(alpha = if (dark) 0.20f else 0.72f),
        topLeft = origin + Offset(unit, unit * 3f),
        size = Size(unit * 8f, unit * 5.2f),
        cornerRadius = CornerRadius(unit * 0.65f),
    )
    repeat(3) { layer ->
        val width = unit * (10f - layer * 2.1f)
        drawRoundRect(
            color = (if (layer == 1) palette.secondary else palette.primary).copy(alpha = 0.42f + layer * 0.045f),
            topLeft = origin + Offset(unit * 5f - width / 2f, unit * layer * 0.92f),
            size = Size(width, unit * 1.05f),
            cornerRadius = CornerRadius(unit * 0.50f),
        )
    }
    drawRoundRect(
        color = palette.secondary.copy(alpha = if (dark) 0.52f else 0.34f),
        topLeft = origin + Offset(unit * 2f, unit * 4.3f),
        size = Size(unit * 2f, unit * 1.7f),
        cornerRadius = CornerRadius(unit * 0.35f),
    )
    drawRoundRect(
        color = palette.surfaceAlt.copy(alpha = 0.62f),
        topLeft = origin + Offset(unit * 6f, unit * 4.2f),
        size = Size(unit * 1.8f, unit * 4f),
        cornerRadius = CornerRadius(unit * 0.35f),
    )
}

private fun DrawScope.drawCloudWindmill(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRoundRect(
        color = palette.outline.copy(alpha = 0.42f),
        topLeft = origin + Offset(unit * 2.2f, unit * 2.2f),
        size = Size(unit * 1.2f, unit * 5.2f),
        cornerRadius = CornerRadius(unit * 0.35f),
    )
    val center = origin + Offset(unit * 2.8f, unit * 2.4f)
    listOf(
        Offset(-unit * 2.5f, -unit * 0.35f) to Size(unit * 2.4f, unit * 0.7f),
        Offset(unit * 0.1f, -unit * 0.35f) to Size(unit * 2.4f, unit * 0.7f),
        Offset(-unit * 0.35f, -unit * 2.5f) to Size(unit * 0.7f, unit * 2.4f),
        Offset(-unit * 0.35f, unit * 0.1f) to Size(unit * 0.7f, unit * 2.4f),
    ).forEach { (offset, bladeSize) ->
        drawRoundRect(
            color = palette.primary.copy(alpha = 0.54f),
            topLeft = center + offset,
            size = bladeSize,
            cornerRadius = CornerRadius(unit * 0.25f),
        )
    }
    drawRoundRect(
        color = palette.secondary.copy(alpha = 0.62f),
        topLeft = center - Offset(unit * 0.6f, unit * 0.6f),
        size = Size(unit * 1.2f, unit * 1.2f),
        cornerRadius = CornerRadius(unit * 0.45f),
    )
}

private fun DrawScope.drawCloudBalloon(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(5) { row ->
        val width = unit * (6f - kotlin.math.abs(row - 2) * 1.1f)
        drawRoundRect(
            color = (if (row == 2) palette.secondary else palette.primary).copy(alpha = 0.48f + row % 2 * 0.05f),
            topLeft = origin + Offset(unit * 3f - width / 2f, unit * row * 0.95f),
            size = Size(width, unit * 1.05f),
            cornerRadius = CornerRadius(unit * 0.50f),
        )
    }
    drawLine(palette.outline.copy(alpha = 0.42f), origin + Offset(unit * 1.5f, unit * 4.6f), origin + Offset(unit * 2.2f, unit * 6.2f), unit * 0.25f)
    drawLine(palette.outline.copy(alpha = 0.42f), origin + Offset(unit * 4.5f, unit * 4.6f), origin + Offset(unit * 3.8f, unit * 6.2f), unit * 0.25f)
    drawRoundRect(
        color = palette.outline.copy(alpha = 0.50f),
        topLeft = origin + Offset(unit * 2.1f, unit * 6f),
        size = Size(unit * 1.8f, unit * 1.1f),
        cornerRadius = CornerRadius(unit * 0.28f),
    )
}

private fun DrawScope.drawCloudMiniBalloon(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRoundRect(
        color = palette.secondary.copy(alpha = 0.58f),
        topLeft = origin,
        size = Size(unit * 3f, unit * 3.4f),
        cornerRadius = CornerRadius(unit * 1.1f),
    )
    drawLine(palette.outline.copy(alpha = 0.42f), origin + Offset(unit * 0.7f, unit * 3f), origin + Offset(unit, unit * 4.2f), unit * 0.20f)
    drawLine(palette.outline.copy(alpha = 0.42f), origin + Offset(unit * 2.3f, unit * 3f), origin + Offset(unit * 2f, unit * 4.2f), unit * 0.20f)
    drawRoundRect(
        color = palette.outline.copy(alpha = 0.48f),
        topLeft = origin + Offset(unit * 0.9f, unit * 4f),
        size = Size(unit * 1.2f, unit * 0.7f),
        cornerRadius = CornerRadius(unit * 0.22f),
    )
}

private fun DrawScope.drawCloudMiniIsland(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRoundRect(palette.primary.copy(alpha = 0.48f), origin, Size(unit * 8f, unit), CornerRadius(unit * 0.45f))
    drawRoundRect(palette.outline.copy(alpha = 0.30f), origin + Offset(unit, unit * 0.9f), Size(unit * 6f, unit), CornerRadius(unit * 0.40f))
    drawCloudCluster(origin + Offset(unit * 1.4f, unit * 1.6f), unit * 0.46f, palette, 0.28f)
}

private fun DrawScope.drawCloudCrystal(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRoundRect(palette.secondary.copy(alpha = 0.60f), origin + Offset(unit, 0f), Size(unit, unit * 3.2f), CornerRadius(unit * 0.35f))
    drawRoundRect(palette.primary.copy(alpha = 0.52f), origin + Offset(0f, unit), Size(unit, unit * 2.2f), CornerRadius(unit * 0.35f))
    drawRoundRect(palette.highlight.copy(alpha = 0.46f), origin + Offset(unit * 1.25f, unit * 0.55f), Size(unit * 0.30f, unit * 1.5f), CornerRadius(unit * 0.15f))
}

private fun DrawScope.drawCloudBird(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawLine(palette.outline.copy(alpha = 0.48f), origin, origin + Offset(unit * 1.4f, unit * 0.8f), unit * 0.28f)
    drawLine(palette.outline.copy(alpha = 0.48f), origin + Offset(unit * 1.4f, unit * 0.8f), origin + Offset(unit * 2.8f, 0f), unit * 0.28f)
}

private fun DrawScope.drawCloudStar(
    center: Offset,
    unit: Float,
    palette: PixelPalette,
    violet: Boolean,
    alpha: Float,
) {
    val color = if (violet) palette.secondary else palette.highlight
    drawRoundRect(color.copy(alpha = alpha), center - Offset(unit * 0.25f, unit), Size(unit * 0.5f, unit * 2f), CornerRadius(unit * 0.2f))
    drawRoundRect(color.copy(alpha = alpha), center - Offset(unit, unit * 0.25f), Size(unit * 2f, unit * 0.5f), CornerRadius(unit * 0.2f))
}

private fun DrawScope.drawCloudDandelionPatch(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(4) { index ->
        val x = unit * index * 1.6f
        val height = unit * (2.2f + index % 2 * 0.7f)
        drawRoundRect(palette.primary.copy(alpha = 0.44f), origin + Offset(x, unit * 3f - height), Size(unit * 0.25f, height), CornerRadius(unit * 0.12f))
        drawCloudStar(origin + Offset(x + unit * 0.12f, unit * 2.7f - height), unit * 0.42f, palette, index == 2, 0.50f)
    }
}

private fun DrawScope.drawCloudDandelionSeed(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
    alpha: Float,
) {
    drawLine(palette.primary.copy(alpha = alpha), origin, origin + Offset(unit * 1.3f, unit * 1.8f), unit * 0.18f)
    drawRoundRect(palette.highlight.copy(alpha = alpha), origin - Offset(unit * 0.35f, unit * 0.35f), Size(unit * 0.7f, unit * 0.7f), CornerRadius(unit * 0.30f))
}

private fun DrawScope.drawCloudSheep(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(3) { index ->
        drawRoundRect(
            color = palette.highlight.copy(alpha = 0.46f),
            topLeft = origin + Offset(unit * index * 1.1f, unit * (index % 2 * 0.4f)),
            size = Size(unit * 1.8f, unit * 1.6f),
            cornerRadius = CornerRadius(unit * 0.75f),
        )
    }
    drawRoundRect(palette.outline.copy(alpha = 0.46f), origin + Offset(unit * 3.1f, unit * 0.7f), Size(unit * 1.2f, unit), CornerRadius(unit * 0.35f))
    listOf(unit * 0.7f, unit * 2.8f).forEach { x ->
        drawRoundRect(palette.outline.copy(alpha = 0.38f), origin + Offset(x, unit * 1.4f), Size(unit * 0.30f, unit * 1.1f), CornerRadius(unit * 0.12f))
    }
}

private fun DrawScope.drawCloudTownMottoFrame(
    palette: PixelPalette,
    unit: Float,
) {
    drawCloudCluster(Offset(unit * 3.2f, unit * 0.6f), unit * 0.42f, palette, 0.38f)
    drawCloudMiniBalloon(Offset(size.width - unit * 7f, unit * 0.35f), unit * 0.34f, palette)
    repeat(4) { index ->
        drawCloudStar(
            center = Offset(size.width * (0.36f + index * 0.09f), size.height - unit * 0.55f),
            unit = unit * (0.22f + index % 2 * 0.04f),
            palette = palette,
            violet = index == 2,
            alpha = 0.46f,
        )
    }
}
