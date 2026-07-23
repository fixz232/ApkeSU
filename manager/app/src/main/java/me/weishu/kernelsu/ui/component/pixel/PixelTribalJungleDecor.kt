package me.weishu.kernelsu.ui.component.pixel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun PixelTribalJungleMotto(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle() || LocalPixelStyle.current != PixelStyle.TribalJungle) return
    val palette = pixelPalette(PixelStyle.TribalJungle, isInDarkTheme())
    val shape = pixelMottoShape
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.94f), shape)
            .border(1.dp, palette.primary.copy(alpha = 0.78f), shape)
            .drawWithContent {
                drawContent()
                drawTribalJungleMottoFrame(palette, 2.dp.toPx())
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pixel_tribal_jungle_motto),
            modifier = Modifier.padding(horizontal = 40.dp),
            color = palette.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun DrawScope.drawTribalJungleScene(
    palette: PixelPalette,
    progress: Float,
) {
    val unit = 5.dp.toPx()
    val groundY = size.height * 0.72f

    drawRect(
        color = palette.outline.copy(alpha = 0.10f),
        topLeft = Offset(0f, groundY),
        size = Size(size.width, size.height - groundY),
    )
    drawTribalJungleCanopy(unit, palette)
    drawTribalRockPainting(Offset(size.width * 0.60f, size.height * 0.30f), unit, palette)
    drawTribalThatchedHut(Offset(size.width * 0.07f, groundY - unit * 10f), unit, palette)
    drawTribalHideTent(Offset(size.width * 0.28f, groundY - unit * 7f), unit, palette)
    drawTribalBoneMask(Offset(size.width * 0.40f, groundY - unit * 8.5f), unit * 0.72f, palette)
    drawTribalTotem(Offset(size.width * 0.66f, groundY - unit * 12f), unit, palette)
    drawTribalStoneAltar(Offset(size.width * 0.78f, groundY - unit * 2f), unit, palette)
    drawTribalCampfire(Offset(size.width * 0.48f, groundY - unit * 2f), unit, palette)
    drawTribalRaft(Offset(size.width * 0.12f, size.height * 0.88f), unit, palette)
    drawTribalSnake(Offset(size.width * 0.55f, size.height * 0.88f), unit, palette)
    drawTribalParrot(Offset(size.width * 0.85f, size.height * 0.22f), unit, palette)
    drawTribalOrchidAndBerries(Offset(size.width * 0.72f, groundY + unit * 1.2f), unit, palette)

    repeat(7) { index ->
        val mistWidth = size.width * (0.10f + index % 3 * 0.035f)
        val travel = size.width + mistWidth
        val x = (size.width * (0.03f + index * 0.17f) + progress * travel * (0.28f + index % 2 * 0.06f)) % travel - mistWidth
        drawRect(
            color = palette.highlight.copy(alpha = 0.022f + index % 2 * 0.012f),
            topLeft = Offset(x, size.height * (0.20f + index * 0.085f)),
            size = Size(mistWidth, unit * 0.68f),
        )
    }

    repeat(12) { index ->
        val travel = size.width * 1.15f
        val x = (size.width * (0.04f + index * 0.087f) + progress * travel * (0.22f + index % 4 * 0.025f)) % travel - size.width * 0.06f
        val y = size.height * (0.16f + (index * 0.137f) % 0.58f)
        val side = unit * (0.26f + index % 3 * 0.08f)
        drawRect(
            color = (if (index % 5 == 0) palette.secondary else palette.highlight)
                .copy(alpha = 0.14f + index % 2 * 0.05f),
            topLeft = Offset(x, y),
            size = Size(side, side),
        )
    }
}

internal fun DrawScope.drawTribalJungleCardMaterial(
    palette: PixelPalette,
    line: Float,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.052f),
        topLeft = Offset.Zero,
        size = Size(size.width, line * 3f),
    )
    repeat(6) { index ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.028f + index % 2 * 0.012f),
            topLeft = Offset(size.width * (0.05f + index % 3 * 0.12f), size.height * (0.14f + index * 0.13f)),
            size = Size(size.width * (0.18f + index % 2 * 0.09f), line * 0.46f),
        )
    }
    drawRect(
        color = palette.primary.copy(alpha = 0.036f),
        topLeft = Offset(0f, size.height - line * 3f),
        size = Size(size.width, line * 3f),
    )
    repeat(4) { index ->
        drawRect(
            color = palette.secondary.copy(alpha = 0.024f + index % 2 * 0.008f),
            topLeft = Offset(size.width * (0.58f + index * 0.08f), size.height * (0.18f + index * 0.16f)),
            size = Size(line * 0.8f, line * 1.5f),
        )
    }
}

internal fun DrawScope.drawTribalJungleHudAccent(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    val centerX = size.width / 2f
    drawRect(
        color = palette.primary.copy(alpha = 0.66f),
        topLeft = Offset(centerX - unit * 6f, top + unit * 1.4f),
        size = Size(unit * 12f, unit * 0.5f),
    )
    repeat(5) { index ->
        val vineHeight = unit * (0.7f + index % 3 * 0.4f)
        drawRect(
            color = palette.primary.copy(alpha = 0.58f),
            topLeft = Offset(centerX - unit * 4.6f + index * unit * 2.1f, top + unit * 1.4f - vineHeight),
            size = Size(unit * 0.42f, vineHeight),
        )
        drawRect(
            color = if (index == 2) palette.secondary.copy(alpha = 0.76f) else palette.highlight.copy(alpha = 0.42f),
            topLeft = Offset(centerX - unit * 5f + index * unit * 2.1f, top + unit * 0.35f),
            size = Size(unit * 1.15f, unit * 0.55f),
        )
    }
}

internal fun DrawScope.drawTribalJungleModeHudPolish(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    drawTribalMiniTotem(Offset(unit * 6.5f, top + unit * 0.4f), unit * 0.46f, palette)
    drawTribalMiniTotem(Offset(size.width - unit * 8f, top + unit * 0.7f), unit * 0.42f, palette)
    listOf(0.14f, 0.86f).forEachIndexed { index, x ->
        drawRect(
            color = (if (index == 0) palette.primary else palette.secondary).copy(alpha = 0.58f),
            topLeft = Offset(size.width * x, top + unit * 2.45f),
            size = Size(unit * 3.7f, unit * 0.38f),
        )
    }
}

internal fun DrawScope.drawTribalJungleNavigationAccent(
    palette: PixelPalette,
    unit: Float,
) {
    val vineY = size.height - unit * 1.3f
    drawRect(
        color = palette.primary.copy(alpha = 0.72f),
        topLeft = Offset(size.width * 0.12f, vineY),
        size = Size(size.width * 0.76f, unit * 0.55f),
    )
    repeat(8) { index ->
        val x = size.width * (0.14f + index * 0.095f)
        drawRect(
            color = (if (index % 3 == 1) palette.secondary else palette.highlight)
                .copy(alpha = if (index % 3 == 1) 0.66f else 0.40f),
            topLeft = Offset(x, vineY - unit * (0.55f + index % 2 * 0.55f)),
            size = Size(unit * 1.25f, unit * 0.58f),
        )
    }
    drawTribalMiniTotem(Offset(unit * 4.2f, unit * 1.4f), unit * 0.50f, palette)
    drawTribalMiniTotem(Offset(size.width - unit * 6.4f, unit * 1.6f), unit * 0.46f, palette)
}

internal fun DrawScope.drawTribalJungleModeNavigationFrame(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(7) { index ->
        drawRect(
            color = (if (index % 2 == 0) palette.primary else palette.highlight).copy(alpha = 0.50f),
            topLeft = Offset(size.width * (0.18f + index * 0.095f), 0f),
            size = Size(size.width * 0.075f, unit * 0.44f),
        )
    }
    drawRect(
        color = palette.secondary.copy(alpha = 0.66f),
        topLeft = Offset(size.width * 0.45f, size.height - unit * 1.1f),
        size = Size(size.width * 0.10f, unit * 0.42f),
    )
}

internal fun DrawScope.drawTribalJungleIndicatorAccent(
    palette: PixelPalette,
    unit: Float,
) {
    drawTribalCrystal(
        origin = Offset(size.width - unit * 5.5f, unit * 0.6f),
        unit = unit * 0.78f,
        palette = palette,
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.72f),
        topLeft = Offset(unit * 2f, size.height - unit * 1.2f),
        size = Size(size.width * 0.25f, unit * 0.38f),
    )
}

internal fun DrawScope.drawTribalJungleModeIndicatorPolish(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(3) { index ->
        val height = unit * (0.7f + index * 0.45f)
        drawRect(
            color = palette.primary.copy(alpha = 0.62f + index * 0.06f),
            topLeft = Offset(size.width * (0.20f + index * 0.12f), size.height - height),
            size = Size(unit * 0.46f, height),
        )
    }
}

private fun DrawScope.drawTribalJungleCanopy(
    unit: Float,
    palette: PixelPalette,
) {
    repeat(11) { index ->
        val x = size.width * (-0.04f + index * 0.10f)
        val width = size.width * (0.12f + index % 3 * 0.025f)
        val height = unit * (3.5f + index % 4 * 1.2f)
        drawRect(
            color = palette.primary.copy(alpha = 0.075f + index % 3 * 0.018f),
            topLeft = Offset(x, 0f),
            size = Size(width, height),
        )
        drawRect(
            color = palette.shadow.copy(alpha = 0.055f),
            topLeft = Offset(x + width * 0.42f, 0f),
            size = Size(unit * 0.55f, height * 1.5f),
        )
    }
    listOf(0.04f, 0.11f, 0.88f, 0.95f).forEachIndexed { index, x ->
        val vineHeight = size.height * (0.16f + index % 2 * 0.09f)
        drawRect(
            color = palette.primary.copy(alpha = 0.10f),
            topLeft = Offset(size.width * x, 0f),
            size = Size(unit * 0.6f, vineHeight),
        )
        repeat(3) { leaf ->
            drawRect(
                color = (if (leaf == 2 && index % 2 == 1) palette.secondary else palette.primary)
                    .copy(alpha = 0.11f),
                topLeft = Offset(size.width * x - unit * (1.2f - leaf % 2 * 0.4f), vineHeight * (0.34f + leaf * 0.23f)),
                size = Size(unit * 2.1f, unit * 0.85f),
            )
        }
    }
}

private fun DrawScope.drawTribalThatchedHut(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.17f),
        topLeft = origin,
        size = Size(unit * 12f, unit * 8f),
    )
    repeat(8) { index ->
        drawRect(
            color = palette.highlight.copy(alpha = 0.12f + index % 2 * 0.025f),
            topLeft = origin + Offset(-unit * 1.2f + index * unit * 1.8f, -unit * (2.8f + index % 3 * 0.5f)),
            size = Size(unit * 2f, unit * 3.2f),
        )
    }
    drawRect(
        color = palette.shadow.copy(alpha = 0.19f),
        topLeft = origin + Offset(unit * 4.5f, unit * 3f),
        size = Size(unit * 3f, unit * 5f),
    )
}

private fun DrawScope.drawTribalHideTent(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(5) { level ->
        drawRect(
            color = palette.highlight.copy(alpha = 0.11f + level * 0.012f),
            topLeft = origin + Offset(unit * level, unit * level),
            size = Size(unit * (10f - level * 2f), unit),
        )
    }
    drawRect(
        color = palette.secondary.copy(alpha = 0.13f),
        topLeft = origin + Offset(unit * 4.3f, unit * 2f),
        size = Size(unit * 1.4f, unit * 3f),
    )
    listOf(-0.8f, 9.8f).forEach { x ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.17f),
            topLeft = origin + Offset(unit * x, -unit),
            size = Size(unit * 0.55f, unit * 7f),
        )
    }
}

private fun DrawScope.drawTribalTotem(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.shadow.copy(alpha = 0.20f),
        topLeft = origin,
        size = Size(unit * 4.2f, unit * 12f),
    )
    repeat(3) { face ->
        val top = origin + Offset(unit * 0.6f, unit * (0.8f + face * 3.6f))
        drawRect(
            color = palette.outline.copy(alpha = 0.18f),
            topLeft = top,
            size = Size(unit * 3f, unit * 2.7f),
            style = Stroke(width = unit * 0.42f),
        )
        listOf(unit * 0.65f, unit * 1.9f).forEachIndexed { index, x ->
            drawRect(
                color = (if ((face + index) % 2 == 0) palette.secondary else palette.primary).copy(alpha = 0.19f),
                topLeft = top + Offset(x, unit * 0.65f),
                size = Size(unit * 0.48f, unit * 0.48f),
            )
        }
    }
}

private fun DrawScope.drawTribalBoneMask(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.highlight.copy(alpha = 0.17f),
        topLeft = origin,
        size = Size(unit * 4.2f, unit * 5.2f),
    )
    listOf(unit * 0.75f, unit * 2.65f).forEach { x ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.20f),
            topLeft = origin + Offset(x, unit * 1.2f),
            size = Size(unit * 0.8f, unit * 0.8f),
        )
    }
    drawRect(
        color = palette.secondary.copy(alpha = 0.16f),
        topLeft = origin + Offset(unit * 1.65f, unit * 2.7f),
        size = Size(unit * 0.9f, unit * 0.65f),
    )
    repeat(3) { index ->
        drawRect(
            color = palette.highlight.copy(alpha = 0.15f),
            topLeft = origin + Offset(unit * (0.8f + index * 1.15f), unit * 4.7f),
            size = Size(unit * 0.55f, unit * 1.2f),
        )
    }
    listOf(-1.4f, 4.5f).forEach { x ->
        drawRect(
            color = palette.highlight.copy(alpha = 0.14f),
            topLeft = origin + Offset(unit * x, -unit * 1.2f),
            size = Size(unit * 1.2f, unit * 2.4f),
        )
    }
}

private fun DrawScope.drawTribalOrchidAndBerries(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(4) { index ->
        val stemHeight = unit * (2.8f + index % 3 * 0.8f)
        val x = origin.x + unit * index * 1.5f
        drawRect(
            color = palette.primary.copy(alpha = 0.14f),
            topLeft = Offset(x, origin.y - stemHeight),
            size = Size(unit * 0.45f, stemHeight),
        )
        drawRect(
            color = palette.secondary.copy(alpha = 0.19f),
            topLeft = Offset(x - unit * 0.7f, origin.y - stemHeight - unit * 0.6f),
            size = Size(unit * 1.8f, unit * 0.9f),
        )
        if (index != 2) {
            drawRect(
                color = palette.secondary.copy(alpha = 0.17f),
                topLeft = Offset(x + unit * 0.5f, origin.y - unit * (1.2f + index % 2 * 0.6f)),
                size = Size(unit * 0.75f, unit * 0.75f),
            )
        }
    }
}

private fun DrawScope.drawTribalStoneAltar(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(3) { level ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.15f + level * 0.025f),
            topLeft = origin + Offset(unit * level, -unit * level),
            size = Size(unit * (9f - level * 2f), unit),
        )
    }
    drawTribalCrystal(origin + Offset(unit * 3.1f, -unit * 5.8f), unit, palette)
}

private fun DrawScope.drawTribalCampfire(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.20f),
        topLeft = origin + Offset(-unit * 2.2f, unit * 1.2f),
        size = Size(unit * 4.4f, unit * 0.75f),
    )
    repeat(3) { level ->
        val width = unit * (3.5f - level * 0.9f)
        drawRect(
            color = (if (level == 1) palette.secondary else palette.highlight).copy(alpha = 0.15f + level * 0.025f),
            topLeft = origin + Offset(-width / 2f, -unit * (level + 0.1f)),
            size = Size(width, unit * 1.2f),
        )
    }
    repeat(4) { index ->
        drawRect(
            color = palette.highlight.copy(alpha = 0.035f),
            topLeft = origin + Offset(unit * (-1.5f + index), -unit * (4f + index * 1.2f)),
            size = Size(unit * (1.8f + index % 2), unit * 0.55f),
        )
    }
}

private fun DrawScope.drawTribalRaft(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(5) { index ->
        drawRect(
            color = palette.outline.copy(alpha = 0.13f + index % 2 * 0.025f),
            topLeft = origin + Offset(0f, unit * index * 0.62f),
            size = Size(unit * 11f, unit * 0.48f),
        )
    }
    listOf(unit * 2f, unit * 8f).forEach { x ->
        drawRect(
            color = palette.primary.copy(alpha = 0.12f),
            topLeft = origin + Offset(x, -unit * 0.5f),
            size = Size(unit * 0.45f, unit * 3.8f),
        )
    }
}

private fun DrawScope.drawTribalSnake(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    listOf(0f to 0f, 1.2f to -0.5f, 2.4f to 0f, 3.6f to 0.55f, 4.8f to 0f).forEachIndexed { index, (x, y) ->
        drawRect(
            color = palette.primary.copy(alpha = 0.15f + index % 2 * 0.025f),
            topLeft = origin + Offset(unit * x, unit * y),
            size = Size(unit * 1.5f, unit * 0.65f),
        )
    }
    drawRect(
        color = palette.secondary.copy(alpha = 0.16f),
        topLeft = origin + Offset(unit * 5.8f, -unit * 0.25f),
        size = Size(unit, unit),
    )
}

private fun DrawScope.drawTribalParrot(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.primary.copy(alpha = 0.17f),
        topLeft = origin,
        size = Size(unit * 2.6f, unit * 3.6f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.18f),
        topLeft = origin + Offset(unit * 1.6f, unit * 0.6f),
        size = Size(unit * 1.8f, unit * 2f),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.20f),
        topLeft = origin + Offset(unit * 0.55f, unit * 0.7f),
        size = Size(unit * 0.45f, unit * 0.45f),
    )
    drawRect(
        color = palette.outline.copy(alpha = 0.18f),
        topLeft = origin + Offset(unit * 0.8f, unit * 3.2f),
        size = Size(unit * 0.55f, unit * 3f),
    )
}

private fun DrawScope.drawTribalRockPainting(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.055f),
        topLeft = origin,
        size = Size(unit * 14f, unit * 9f),
        style = Stroke(width = unit * 0.5f),
    )
    listOf(2f to 2f, 5f to 4f, 8f to 2.5f, 11f to 5f).forEachIndexed { index, (x, y) ->
        drawRect(
            color = (if (index == 2) palette.secondary else palette.shadow).copy(alpha = 0.07f),
            topLeft = origin + Offset(unit * x, unit * y),
            size = Size(unit * 1.2f, unit * 1.2f),
        )
    }
}

private fun DrawScope.drawTribalMiniTotem(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.78f),
        topLeft = origin,
        size = Size(unit * 2.8f, unit * 4.4f),
    )
    listOf(unit * 0.55f, unit * 1.7f).forEachIndexed { index, x ->
        drawRect(
            color = (if (index == 0) palette.secondary else palette.primary).copy(alpha = 0.82f),
            topLeft = origin + Offset(x, unit * 0.8f),
            size = Size(unit * 0.52f, unit * 0.52f),
        )
    }
    drawRect(
        color = palette.shadow.copy(alpha = 0.72f),
        topLeft = origin + Offset(unit * 0.7f, unit * 2.5f),
        size = Size(unit * 1.4f, unit * 0.48f),
    )
}

private fun DrawScope.drawTribalCrystal(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.secondary.copy(alpha = 0.82f),
        topLeft = origin + Offset(unit * 0.8f, 0f),
        size = Size(unit * 1.2f, unit * 3.2f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.66f),
        topLeft = origin,
        size = Size(unit * 0.9f, unit * 2.2f),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.62f),
        topLeft = origin + Offset(unit * 1.05f, unit * 0.55f),
        size = Size(unit * 0.34f, unit * 1.4f),
    )
}

private fun DrawScope.drawTribalJungleMottoFrame(
    palette: PixelPalette,
    unit: Float,
) {
    drawTribalMiniTotem(Offset(unit * 2.8f, unit * 2.4f), unit * 0.60f, palette)
    drawTribalCrystal(Offset(size.width - unit * 5.4f, unit * 2.6f), unit * 0.64f, palette)
    repeat(4) { index ->
        drawRect(
            color = (if (index == 2) palette.secondary else palette.primary).copy(alpha = 0.60f),
            topLeft = Offset(size.width * (0.18f + index * 0.19f), size.height - unit * (1.8f + index % 2 * 0.45f)),
            size = Size(size.width * 0.12f, unit * 0.42f),
        )
    }
}
