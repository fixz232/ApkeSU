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
fun PixelFishingHarborMotto(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle() || LocalPixelStyle.current != PixelStyle.FishingHarbor) return
    val palette = pixelPalette(PixelStyle.FishingHarbor, isInDarkTheme())
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.94f), shape)
            .border(1.dp, palette.secondary.copy(alpha = 0.78f), shape)
            .drawWithContent {
                drawContent()
                drawFishingHarborMottoFrame(palette, 2.dp.toPx())
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pixel_fishing_harbor_motto),
            modifier = Modifier.padding(horizontal = 40.dp),
            color = palette.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun DrawScope.drawFishingHarborScene(
    palette: PixelPalette,
    progress: Float,
) {
    val unit = 5.dp.toPx()
    val horizon = size.height * 0.58f
    val pierY = size.height * 0.82f

    drawRect(
        color = palette.outline.copy(alpha = 0.11f),
        topLeft = Offset(0f, horizon),
        size = Size(size.width, size.height - horizon),
    )
    repeat(7) { index ->
        val bandWidth = size.width * (0.12f + index % 3 * 0.045f)
        val travel = size.width + bandWidth
        val x = (size.width * (0.04f + index * 0.17f) + progress * travel * (0.24f + index % 2 * 0.08f)) % travel - bandWidth
        drawRect(
            color = (if (index % 3 == 1) palette.primary else palette.highlight)
                .copy(alpha = 0.035f + index % 2 * 0.018f),
            topLeft = Offset(x, horizon + unit * (2.4f + index * 4.3f)),
            size = Size(bandWidth, unit * 0.48f),
        )
    }

    drawFishingHarborCabin(Offset(size.width * 0.08f, horizon - unit * 7f), unit, palette)
    drawFishingHarborCabin(Offset(size.width * 0.27f, horizon - unit * 5.5f), unit * 0.84f, palette)
    drawFishingHarborMarketStall(Offset(size.width * 0.37f, horizon - unit * 3.8f), unit * 0.72f, palette)
    drawFishingHarborLighthouse(Offset(size.width * 0.82f, horizon - unit * 12f), unit, palette)
    drawFishingHarborBoat(Offset(size.width * 0.48f, horizon + unit * 5f), unit, palette)
    drawFishingHarborPier(pierY, unit, palette)
    drawFishingHarborPierLamp(Offset(size.width * 0.34f, pierY - unit * 5.8f), unit, palette)

    listOf(0.08f, 0.14f, 0.20f, 0.91f, 0.95f).forEachIndexed { index, x ->
        val reedHeight = unit * (2.8f + index % 3 * 1.2f)
        drawRect(
            color = palette.primary.copy(alpha = 0.12f + index % 2 * 0.025f),
            topLeft = Offset(size.width * x, size.height - reedHeight),
            size = Size(unit * 0.55f, reedHeight),
        )
        drawRect(
            color = palette.primary.copy(alpha = 0.10f),
            topLeft = Offset(size.width * x - unit, size.height - reedHeight * 0.68f),
            size = Size(unit * 1.7f, unit * 0.5f),
        )
    }

    repeat(5) { index ->
        val mistWidth = size.width * (0.11f + index % 2 * 0.05f)
        val travel = size.width + mistWidth
        val x = (size.width * (0.06f + index * 0.22f) + progress * travel * 0.46f) % travel - mistWidth
        drawRect(
            color = palette.highlight.copy(alpha = 0.022f + index % 2 * 0.012f),
            topLeft = Offset(x, size.height * (0.20f + index * 0.09f)),
            size = Size(mistWidth, unit * 0.72f),
        )
    }

    repeat(4) { index ->
        val travel = size.width * 1.2f
        val x = (size.width * (0.08f + index * 0.25f) + progress * travel * (0.32f + index * 0.04f)) % travel - size.width * 0.1f
        val y = size.height * (0.16f + index % 3 * 0.07f)
        drawPixelHarborGull(Offset(x, y), unit * (0.62f + index % 2 * 0.12f), palette)
    }
}

internal fun DrawScope.drawFishingHarborCardMaterial(
    palette: PixelPalette,
    line: Float,
) {
    drawRect(
        color = palette.secondary.copy(alpha = 0.045f),
        topLeft = Offset.Zero,
        size = Size(size.width, line * 3f),
    )
    repeat(5) { index ->
        val y = size.height * (0.18f + index * 0.15f)
        drawRect(
            color = palette.secondary.copy(alpha = 0.025f + index % 2 * 0.01f),
            topLeft = Offset(size.width * (0.06f + index % 3 * 0.09f), y),
            size = Size(size.width * (0.24f + index % 2 * 0.08f), line * 0.42f),
        )
    }
    repeat(3) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.032f - index * 0.006f),
            topLeft = Offset(size.width * (0.08f + index * 0.17f), size.height - line * (4.5f - index)),
            size = Size(size.width * 0.42f, line * 0.48f),
        )
    }
}

internal fun DrawScope.drawFishingHarborHudAccent(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    val centerX = size.width / 2f
    drawRect(
        color = palette.secondary.copy(alpha = 0.70f),
        topLeft = Offset(centerX - unit * 6f, top + unit * 1.5f),
        size = Size(unit * 12f, unit * 0.55f),
    )
    repeat(5) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.58f + index % 2 * 0.10f),
            topLeft = Offset(centerX - unit * 5f + index * unit * 2.2f, top + unit * (0.35f + index % 2 * 0.45f)),
            size = Size(unit * 1.7f, unit * 0.42f),
        )
    }
}

internal fun DrawScope.drawFishingHarborModeHudPolish(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    drawPixelHarborGull(Offset(unit * 7f, top + unit * 1.4f), unit * 0.48f, palette)
    drawPixelHarborGull(Offset(size.width - unit * 9f, top + unit * 1.9f), unit * 0.42f, palette)
    listOf(0.12f, 0.88f).forEachIndexed { index, x ->
        drawRect(
            color = (if (index == 0) palette.secondary else palette.primary).copy(alpha = 0.62f),
            topLeft = Offset(size.width * x, top + unit * 2.5f),
            size = Size(unit * 3.6f, unit * 0.38f),
        )
    }
}

internal fun DrawScope.drawFishingHarborNavigationAccent(
    palette: PixelPalette,
    unit: Float,
) {
    val deckY = size.height - unit * 1.4f
    drawRect(
        color = palette.secondary.copy(alpha = 0.72f),
        topLeft = Offset(size.width * 0.12f, deckY),
        size = Size(size.width * 0.76f, unit * 0.62f),
    )
    repeat(7) { index ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.46f),
            topLeft = Offset(size.width * (0.15f + index * 0.105f), deckY),
            size = Size(unit * 0.28f, unit * 0.62f),
        )
    }
    repeat(5) { index ->
        val lift = if (index % 2 == 0) unit * 0.55f else 0f
        drawRect(
            color = palette.primary.copy(alpha = 0.66f),
            topLeft = Offset(size.width * (0.23f + index * 0.13f), unit * 0.8f - lift),
            size = Size(size.width * 0.09f, unit * 0.48f),
        )
    }
    drawFishingHarborBuoy(Offset(unit * 4.2f, unit * 1.4f), unit * 0.62f, palette)
    drawFishingHarborBuoy(Offset(size.width - unit * 5.7f, unit * 2f), unit * 0.56f, palette)
}

internal fun DrawScope.drawFishingHarborModeNavigationFrame(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(5) { index ->
        drawRect(
            color = palette.secondary.copy(alpha = 0.52f + index % 2 * 0.12f),
            topLeft = Offset(size.width * (0.25f + index * 0.105f), 0f),
            size = Size(size.width * 0.085f, unit * 0.5f),
        )
    }
    drawRect(
        color = palette.primary.copy(alpha = 0.58f),
        topLeft = Offset(size.width * 0.39f, size.height - unit * 1.1f),
        size = Size(size.width * 0.22f, unit * 0.38f),
    )
}

internal fun DrawScope.drawFishingHarborIndicatorAccent(
    palette: PixelPalette,
    unit: Float,
) {
    drawFishingHarborBuoy(
        origin = Offset(size.width - unit * 5.4f, unit * 0.8f),
        unit = unit * 0.78f,
        palette = palette,
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.70f),
        topLeft = Offset(unit * 2f, size.height - unit * 1.3f),
        size = Size(size.width * 0.24f, unit * 0.38f),
    )
}

internal fun DrawScope.drawFishingHarborModeIndicatorPolish(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(3) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.64f - index * 0.10f),
            topLeft = Offset(size.width * (0.24f + index * 0.17f), unit * (0.55f + index % 2 * 0.45f)),
            size = Size(size.width * 0.14f, unit * 0.38f),
        )
    }
}

private fun DrawScope.drawFishingHarborCabin(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.shadow.copy(alpha = 0.16f),
        topLeft = origin,
        size = Size(unit * 9f, unit * 6f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.19f),
        topLeft = origin + Offset(-unit, -unit * 1.6f),
        size = Size(unit * 11f, unit * 1.8f),
    )
    repeat(3) { index ->
        drawRect(
            color = palette.outline.copy(alpha = 0.13f),
            topLeft = origin + Offset(unit * (1f + index * 2.6f), unit),
            size = Size(unit * 1.6f, unit * 1.6f),
        )
    }
    drawRect(
        color = palette.primary.copy(alpha = 0.13f),
        topLeft = origin + Offset(unit * 5.8f, unit * 3f),
        size = Size(unit * 1.8f, unit * 3f),
    )
}

private fun DrawScope.drawFishingHarborLighthouse(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.highlight.copy(alpha = 0.14f),
        topLeft = origin,
        size = Size(unit * 3.4f, unit * 12f),
    )
    listOf(3f, 7f).forEach { y ->
        drawRect(
            color = palette.primary.copy(alpha = 0.16f),
            topLeft = origin + Offset(0f, unit * y),
            size = Size(unit * 3.4f, unit * 1.4f),
        )
    }
    drawRect(
        color = palette.secondary.copy(alpha = 0.20f),
        topLeft = origin + Offset(-unit, -unit * 1.5f),
        size = Size(unit * 5.4f, unit * 1.7f),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.20f),
        topLeft = origin + Offset(unit * 0.7f, unit * 0.7f),
        size = Size(unit * 2f, unit * 1.4f),
    )
}

private fun DrawScope.drawFishingHarborMarketStall(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.secondary.copy(alpha = 0.18f),
        topLeft = origin,
        size = Size(unit * 8f, unit * 1.2f),
    )
    repeat(4) { index ->
        drawRect(
            color = (if (index % 2 == 0) palette.primary else palette.highlight).copy(alpha = 0.15f),
            topLeft = origin + Offset(unit * index * 2f, -unit * 1.5f),
            size = Size(unit * 2f, unit * 1.5f),
        )
    }
    listOf(unit * 0.6f, unit * 6.8f).forEach { x ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.16f),
            topLeft = origin + Offset(x, unit * 1.2f),
            size = Size(unit * 0.55f, unit * 3.4f),
        )
    }
    repeat(3) { index ->
        drawRect(
            color = palette.outline.copy(alpha = 0.15f),
            topLeft = origin + Offset(unit * (1.4f + index * 2.1f), unit * 1.7f),
            size = Size(unit * 1.3f, unit * 1.3f),
        )
    }
}

private fun DrawScope.drawFishingHarborBoat(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.secondary.copy(alpha = 0.20f),
        topLeft = origin,
        size = Size(unit * 13f, unit * 2.2f),
    )
    drawRect(
        color = palette.shadow.copy(alpha = 0.18f),
        topLeft = origin + Offset(unit * 2f, unit * 2.2f),
        size = Size(unit * 9f, unit),
    )
    drawRect(
        color = palette.outline.copy(alpha = 0.16f),
        topLeft = origin + Offset(unit * 5.6f, -unit * 5f),
        size = Size(unit * 0.7f, unit * 5f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.14f),
        topLeft = origin + Offset(unit * 6.3f, -unit * 4.6f),
        size = Size(unit * 4f, unit * 3.2f),
    )
}

private fun DrawScope.drawFishingHarborPier(
    pierY: Float,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.secondary.copy(alpha = 0.18f),
        topLeft = Offset(0f, pierY),
        size = Size(size.width * 0.38f, unit * 2.2f),
    )
    repeat(5) { index ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.13f),
            topLeft = Offset(size.width * index * 0.075f, pierY),
            size = Size(unit * 0.55f, unit * 2.2f),
        )
    }
    listOf(0.05f, 0.31f).forEach { x ->
        drawRect(
            color = palette.secondary.copy(alpha = 0.20f),
            topLeft = Offset(size.width * x, pierY - unit * 5f),
            size = Size(unit, unit * 10f),
        )
    }
}

private fun DrawScope.drawFishingHarborPierLamp(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.shadow.copy(alpha = 0.18f),
        topLeft = origin,
        size = Size(unit * 0.65f, unit * 5.8f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.22f),
        topLeft = origin + Offset(-unit * 0.9f, -unit * 1.4f),
        size = Size(unit * 2.45f, unit * 1.6f),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.20f),
        topLeft = origin + Offset(-unit * 0.3f, -unit),
        size = Size(unit * 1.2f, unit * 0.8f),
    )
}

private fun DrawScope.drawPixelHarborGull(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.highlight.copy(alpha = 0.24f),
        topLeft = origin,
        size = Size(unit * 1.8f, unit * 0.46f),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.24f),
        topLeft = origin + Offset(unit * 1.45f, -unit * 0.55f),
        size = Size(unit * 1.8f, unit * 0.46f),
    )
}

private fun DrawScope.drawFishingHarborBuoy(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.secondary.copy(alpha = 0.82f),
        topLeft = origin,
        size = Size(unit * 1.4f, unit * 2.6f),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.76f),
        topLeft = origin + Offset(-unit * 0.45f, unit * 0.55f),
        size = Size(unit * 2.3f, unit * 0.65f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.76f),
        topLeft = origin + Offset(unit * 0.5f, -unit),
        size = Size(unit * 0.42f, unit),
    )
}

private fun DrawScope.drawFishingHarborMottoFrame(
    palette: PixelPalette,
    unit: Float,
) {
    drawFishingHarborBuoy(Offset(unit * 3f, unit * 3f), unit * 0.72f, palette)
    drawFishingHarborBuoy(Offset(size.width - unit * 4f, unit * 3.4f), unit * 0.65f, palette)
    repeat(3) { index ->
        val lift = if (index % 2 == 0) unit * 0.45f else 0f
        drawRect(
            color = palette.primary.copy(alpha = 0.60f),
            topLeft = Offset(size.width * (0.15f + index * 0.28f), size.height - unit * 2f - lift),
            size = Size(size.width * 0.15f, unit * 0.42f),
        )
    }
}
