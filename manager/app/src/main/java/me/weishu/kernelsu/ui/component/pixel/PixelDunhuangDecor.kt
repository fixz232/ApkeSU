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
import androidx.compose.ui.graphics.Color
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
fun PixelDunhuangMotto(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle() || LocalPixelStyle.current != PixelStyle.DunhuangDesert) return
    val palette = pixelPalette(PixelStyle.DunhuangDesert, isInDarkTheme())
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.95f), shape)
            .border(1.dp, palette.primary.copy(alpha = 0.78f), shape)
            .drawWithContent {
                drawContent()
                drawDunhuangMottoFrame(palette, 2.dp.toPx())
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pixel_dunhuang_motto),
            modifier = Modifier.padding(horizontal = 40.dp),
            color = palette.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun DrawScope.drawDunhuangScene(
    palette: PixelPalette,
    progress: Float,
) {
    val unit = 5.dp.toPx()
    val groundY = size.height * 0.69f

    drawRect(
        color = palette.outline.copy(alpha = 0.08f),
        topLeft = Offset(0f, groundY),
        size = Size(size.width, size.height - groundY),
    )
    drawDunhuangDunes(groundY, unit, palette)
    drawDunhuangGrotto(Offset(size.width * 0.58f, groundY - unit * 14f), unit, palette)
    drawDunhuangRuinedWall(Offset(size.width * 0.06f, groundY - unit * 7f), unit, palette)
    drawDunhuangCamel(Offset(size.width * 0.36f, groundY - unit * 4.5f), unit, palette)
    drawDunhuangCrescentSpring(Offset(size.width * 0.46f, size.height * 0.84f), unit, palette)
    drawDunhuangPoplar(Offset(size.width * 0.88f, groundY - unit * 7f), unit, palette)
    drawDunhuangMuralPanel(Offset(size.width * 0.06f, size.height * 0.27f), unit, palette)
    drawDunhuangFlyingRibbon(Offset(size.width * 0.28f, size.height * 0.18f), unit, palette)
    drawDunhuangWindChime(Offset(size.width * 0.80f, size.height * 0.20f), unit, palette)

    repeat(8) { index ->
        val mistWidth = size.width * (0.10f + index % 3 * 0.04f)
        val travel = size.width + mistWidth
        val x = (size.width * (0.02f + index * 0.15f) + progress * travel * (0.30f + index % 2 * 0.07f)) % travel - mistWidth
        drawRect(
            color = palette.highlight.copy(alpha = 0.026f + index % 2 * 0.012f),
            topLeft = Offset(x, size.height * (0.18f + index * 0.078f)),
            size = Size(mistWidth, unit * 0.74f),
        )
    }

    repeat(14) { index ->
        val travel = size.width * 1.18f
        val x = (size.width * (0.02f + index * 0.073f) + progress * travel * (0.23f + index % 4 * 0.03f)) % travel - size.width * 0.04f
        val y = size.height * (0.13f + (index * 0.119f) % 0.66f)
        val width = unit * (0.45f + index % 3 * 0.18f)
        drawRect(
            color = (if (index % 5 == 0) palette.secondary else palette.primary)
                .copy(alpha = 0.10f + index % 2 * 0.04f),
            topLeft = Offset(x, y),
            size = Size(width, unit * 0.24f),
        )
    }

    repeat(5) { index ->
        val footprintX = size.width * (0.18f + index * 0.045f)
        val footprintY = size.height * (0.82f + index * 0.025f)
        drawRect(
            color = palette.shadow.copy(alpha = 0.10f + index * 0.012f),
            topLeft = Offset(footprintX, footprintY),
            size = Size(unit * 0.75f, unit * 1.15f),
        )
    }
}

internal fun DrawScope.drawDunhuangCardMaterial(
    palette: PixelPalette,
    line: Float,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.060f),
        topLeft = Offset.Zero,
        size = Size(size.width, line * 3f),
    )
    repeat(7) { index ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.028f + index % 2 * 0.012f),
            topLeft = Offset(size.width * (0.04f + index % 4 * 0.11f), size.height * (0.13f + index * 0.12f)),
            size = Size(size.width * (0.14f + index % 3 * 0.05f), line * 0.48f),
        )
    }
    repeat(4) { index ->
        drawRect(
            color = (if (index == 2) palette.secondary else palette.primary).copy(alpha = 0.030f),
            topLeft = Offset(size.width * (0.54f + index * 0.085f), size.height - line * (4.6f - index * 0.75f)),
            size = Size(size.width * 0.13f, line * 0.44f),
        )
    }
}

internal fun DrawScope.drawDunhuangHudAccent(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    val centerX = size.width / 2f
    drawRect(
        color = palette.primary.copy(alpha = 0.68f),
        topLeft = Offset(centerX - unit * 6f, top + unit * 1.8f),
        size = Size(unit * 12f, unit * 0.46f),
    )
    repeat(5) { index ->
        val y = top + unit * (0.4f + index % 2 * 0.55f)
        drawRect(
            color = (if (index == 1 || index == 3) palette.secondary else palette.highlight)
                .copy(alpha = if (index == 1 || index == 3) 0.68f else 0.42f),
            topLeft = Offset(centerX - unit * 5f + index * unit * 2.2f, y),
            size = Size(unit * 1.7f, unit * 0.44f),
        )
    }
}

internal fun DrawScope.drawDunhuangModeHudPolish(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    drawDunhuangMiniLamp(Offset(unit * 6.2f, top + unit * 0.8f), unit * 0.44f, palette)
    drawDunhuangMiniBell(Offset(size.width - unit * 8f, top + unit * 0.5f), unit * 0.46f, palette)
    listOf(0.14f, 0.86f).forEachIndexed { index, x ->
        drawRect(
            color = (if (index == 0) palette.primary else palette.secondary).copy(alpha = 0.58f),
            topLeft = Offset(size.width * x, top + unit * 2.5f),
            size = Size(unit * 3.7f, unit * 0.38f),
        )
    }
}

internal fun DrawScope.drawDunhuangNavigationAccent(
    palette: PixelPalette,
    unit: Float,
) {
    val wallY = size.height - unit * 1.35f
    repeat(8) { index ->
        drawRect(
            color = palette.outline.copy(alpha = 0.68f + index % 2 * 0.08f),
            topLeft = Offset(size.width * (0.09f + index * 0.105f), wallY - unit * (index % 3 * 0.25f)),
            size = Size(size.width * 0.09f, unit * 0.72f),
        )
    }
    repeat(5) { index ->
        val lift = unit * (index % 2 * 0.55f)
        drawRect(
            color = (if (index == 2) palette.secondary else palette.primary).copy(alpha = 0.66f),
            topLeft = Offset(size.width * (0.22f + index * 0.14f), unit * 0.9f - lift),
            size = Size(size.width * 0.10f, unit * 0.45f),
        )
    }
    drawDunhuangMiniLamp(Offset(unit * 4.2f, unit * 1.7f), unit * 0.48f, palette)
    drawDunhuangMiniBell(Offset(size.width - unit * 6.2f, unit * 1.3f), unit * 0.46f, palette)
}

internal fun DrawScope.drawDunhuangModeNavigationFrame(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(6) { index ->
        drawRect(
            color = (if (index % 2 == 0) palette.outline else palette.primary).copy(alpha = 0.56f),
            topLeft = Offset(size.width * (0.18f + index * 0.11f), 0f),
            size = Size(size.width * 0.085f, unit * 0.44f),
        )
    }
    drawRect(
        color = palette.secondary.copy(alpha = 0.66f),
        topLeft = Offset(size.width * 0.44f, size.height - unit * 1.08f),
        size = Size(size.width * 0.12f, unit * 0.40f),
    )
}

internal fun DrawScope.drawDunhuangIndicatorAccent(
    palette: PixelPalette,
    unit: Float,
) {
    drawDunhuangMiniLamp(
        origin = Offset(size.width - unit * 5.8f, unit * 0.65f),
        unit = unit * 0.76f,
        palette = palette,
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.72f),
        topLeft = Offset(unit * 2f, size.height - unit * 1.2f),
        size = Size(size.width * 0.23f, unit * 0.38f),
    )
}

internal fun DrawScope.drawDunhuangModeIndicatorPolish(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(3) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.62f + index * 0.06f),
            topLeft = Offset(size.width * (0.20f + index * 0.13f), size.height - unit * (0.75f + index * 0.45f)),
            size = Size(unit * 0.46f, unit * (0.75f + index * 0.45f)),
        )
    }
}

private fun DrawScope.drawDunhuangDunes(
    groundY: Float,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(6) { level ->
        val y = groundY + unit * level * 4.2f
        repeat(5) { segment ->
            val width = size.width * (0.16f + (segment + level) % 3 * 0.045f)
            drawRect(
                color = palette.primary.copy(alpha = 0.045f + level * 0.008f),
                topLeft = Offset(size.width * (segment * 0.21f - 0.03f) + if (level % 2 == 0) 0f else size.width * 0.05f, y),
                size = Size(width, unit * 0.58f),
            )
        }
    }
}

private fun DrawScope.drawDunhuangGrotto(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.13f),
        topLeft = origin,
        size = Size(unit * 22f, unit * 14f),
    )
    repeat(3) { niche ->
        val nicheOrigin = origin + Offset(unit * (2f + niche * 6.5f), unit * 2f)
        repeat(3) { level ->
            drawRect(
                color = palette.shadow.copy(alpha = 0.14f + level * 0.02f),
                topLeft = nicheOrigin + Offset(unit * level * 0.55f, unit * level * 0.65f),
                size = Size(unit * (5f - level * 1.1f), unit * (8f - level * 0.65f)),
                style = Stroke(width = unit * 0.45f),
            )
        }
        drawRect(
            color = palette.primary.copy(alpha = 0.14f),
            topLeft = nicheOrigin + Offset(unit * 2f, unit * 2.8f),
            size = Size(unit, unit * 3.8f),
        )
        drawRect(
            color = palette.highlight.copy(alpha = 0.12f),
            topLeft = nicheOrigin + Offset(unit * 1.4f, unit * 6.6f),
            size = Size(unit * 2.2f, unit * 0.65f),
        )
    }
}

private fun DrawScope.drawDunhuangRuinedWall(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(3) { row ->
        repeat(5) { column ->
            if ((row + column) % 7 != 0) {
                drawRect(
                    color = palette.outline.copy(alpha = 0.12f + row * 0.012f),
                    topLeft = origin + Offset(unit * (column * 2.4f + row % 2 * 1.1f), unit * row * 1.6f),
                    size = Size(unit * 2.1f, unit * 1.25f),
                )
            }
        }
    }
    listOf(unit * 1.2f, unit * 9.3f).forEach { x ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.13f),
            topLeft = origin + Offset(x, -unit * 2f),
            size = Size(unit * 1.4f, unit * 7f),
        )
    }
}

private fun DrawScope.drawDunhuangCamel(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.16f),
        topLeft = origin,
        size = Size(unit * 9f, unit * 3.4f),
    )
    listOf(1.2f, 5.5f).forEach { x ->
        drawRect(
            color = palette.outline.copy(alpha = 0.17f),
            topLeft = origin + Offset(unit * x, -unit * 1.8f),
            size = Size(unit * 2.6f, unit * 2f),
        )
    }
    drawRect(
        color = palette.outline.copy(alpha = 0.17f),
        topLeft = origin + Offset(unit * 8f, -unit * 3.8f),
        size = Size(unit * 1.2f, unit * 4f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.15f),
        topLeft = origin + Offset(unit * 8.2f, -unit * 4.5f),
        size = Size(unit * 2f, unit * 1.5f),
    )
    listOf(1f, 3.1f, 5.8f, 7.4f).forEach { x ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.14f),
            topLeft = origin + Offset(unit * x, unit * 3.2f),
            size = Size(unit * 0.65f, unit * 4f),
        )
    }
}

private fun DrawScope.drawDunhuangCrescentSpring(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(4) { index ->
        drawRect(
            color = DUNHUANG_SPRING.copy(alpha = 0.10f + index * 0.018f),
            topLeft = origin + Offset(unit * index * 1.4f, unit * index * 0.55f),
            size = Size(unit * (14f - index * 2.3f), unit * 0.62f),
        )
    }
    drawRect(
        color = palette.highlight.copy(alpha = 0.08f),
        topLeft = origin + Offset(unit * 2f, unit * 0.2f),
        size = Size(unit * 4f, unit * 0.35f),
    )
}

private fun DrawScope.drawDunhuangPoplar(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.15f),
        topLeft = origin,
        size = Size(unit * 0.75f, unit * 9f),
    )
    listOf(-3f to 1f, 0.5f to 2.2f, -2f to 4.1f, 0.2f to 5.4f).forEachIndexed { index, (x, y) ->
        drawRect(
            color = palette.primary.copy(alpha = 0.11f + index * 0.012f),
            topLeft = origin + Offset(unit * x, -unit * y),
            size = Size(unit * (4.2f + index % 2), unit * 1.2f),
        )
    }
}

private fun DrawScope.drawDunhuangMuralPanel(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.055f),
        topLeft = origin,
        size = Size(unit * 17f, unit * 10f),
        style = Stroke(width = unit * 0.48f),
    )
    repeat(5) { index ->
        drawRect(
            color = (if (index == 2) palette.secondary else palette.primary).copy(alpha = 0.07f),
            topLeft = origin + Offset(unit * (1.5f + index * 2.8f), unit * (2f + index % 2 * 2.4f)),
            size = Size(unit * 2f, unit * 1.2f),
        )
    }
    drawRect(
        color = palette.primary.copy(alpha = 0.07f),
        topLeft = origin + Offset(unit * 2f, unit * 7.6f),
        size = Size(unit * 13f, unit * 0.42f),
    )
}

private fun DrawScope.drawDunhuangFlyingRibbon(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(5) { segment ->
        drawRect(
            color = palette.secondary.copy(alpha = 0.11f + segment * 0.012f),
            topLeft = origin + Offset(unit * segment * 3f, unit * (segment % 3 * 0.8f)),
            size = Size(unit * 3.6f, unit * 0.65f),
        )
    }
    drawRect(
        color = palette.primary.copy(alpha = 0.10f),
        topLeft = origin + Offset(unit * 6.5f, -unit * 1.5f),
        size = Size(unit * 1.3f, unit * 3.2f),
    )
}

private fun DrawScope.drawDunhuangWindChime(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.primary.copy(alpha = 0.14f),
        topLeft = origin,
        size = Size(unit * 0.45f, unit * 2.3f),
    )
    repeat(3) { index ->
        drawRect(
            color = (if (index == 1) palette.secondary else palette.highlight).copy(alpha = 0.14f),
            topLeft = origin + Offset(unit * (-1.6f + index * 1.35f), unit * (2.2f + index % 2 * 0.5f)),
            size = Size(unit * 1.2f, unit * 1.5f),
        )
        drawRect(
            color = palette.primary.copy(alpha = 0.12f),
            topLeft = origin + Offset(unit * (-1.15f + index * 1.35f), unit * 3.7f),
            size = Size(unit * 0.32f, unit * 1.4f),
        )
    }
}

private fun DrawScope.drawDunhuangMiniLamp(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.78f),
        topLeft = origin + Offset(unit * 0.45f, unit * 1.5f),
        size = Size(unit * 2f, unit * 2.4f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.84f),
        topLeft = origin,
        size = Size(unit * 2.9f, unit * 0.65f),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.70f),
        topLeft = origin + Offset(unit * 1.05f, -unit * 1.2f),
        size = Size(unit * 0.8f, unit * 1.4f),
    )
}

private fun DrawScope.drawDunhuangMiniBell(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.primary.copy(alpha = 0.82f),
        topLeft = origin + Offset(unit, 0f),
        size = Size(unit * 0.42f, unit * 1.5f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.76f),
        topLeft = origin,
        size = Size(unit * 2.4f, unit * 2.2f),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.60f),
        topLeft = origin + Offset(unit * 0.9f, unit * 2.2f),
        size = Size(unit * 0.6f, unit * 1.1f),
    )
}

private fun DrawScope.drawDunhuangMottoFrame(
    palette: PixelPalette,
    unit: Float,
) {
    drawDunhuangMiniLamp(Offset(unit * 2.8f, unit * 2.8f), unit * 0.60f, palette)
    drawDunhuangMiniBell(Offset(size.width - unit * 5.2f, unit * 2.5f), unit * 0.60f, palette)
    repeat(4) { index ->
        val lift = unit * (index % 2 * 0.45f)
        drawRect(
            color = (if (index == 2) palette.secondary else palette.primary).copy(alpha = 0.60f),
            topLeft = Offset(size.width * (0.18f + index * 0.19f), size.height - unit * 1.7f - lift),
            size = Size(size.width * 0.12f, unit * 0.42f),
        )
    }
}

private val DUNHUANG_SPRING = Color(0xFF5F8A82)
