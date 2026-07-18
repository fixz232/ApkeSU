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
fun PixelBianliangMotto(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle() || LocalPixelStyle.current != PixelStyle.BianliangMarket) return
    val palette = pixelPalette(PixelStyle.BianliangMarket, isInDarkTheme())
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.94f), shape)
            .border(1.dp, palette.primary.copy(alpha = 0.74f), shape)
            .drawWithContent {
                drawContent()
                drawBianliangMottoFrame(palette, 2.dp.toPx())
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pixel_bianliang_motto),
            modifier = Modifier.padding(horizontal = 38.dp),
            color = palette.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun DrawScope.drawBianliangScene(
    palette: PixelPalette,
    progress: Float,
) {
    val unit = 5.dp.toPx()
    val riverTop = size.height * 0.68f
    val streetBase = riverTop - unit * 1.5f

    drawRect(
        color = palette.outline.copy(alpha = 0.08f),
        topLeft = Offset(0f, riverTop),
        size = Size(size.width, size.height - riverTop),
    )
    repeat(5) { index ->
        val width = size.width * (0.16f + index % 3 * 0.05f)
        val travel = size.width + width
        val x = (size.width * (index * 0.23f) + progress * travel) % travel - width
        drawRect(
            color = palette.highlight.copy(alpha = 0.055f + index % 2 * 0.018f),
            topLeft = Offset(x, riverTop + unit * (3f + index * 4.2f)),
            size = Size(width, unit * 0.45f),
        )
    }

    drawBianliangShops(streetBase, unit, palette)
    drawBianliangRainbowBridge(
        origin = Offset(size.width * 0.46f, riverTop + unit * 1.5f),
        unit = unit,
        palette = palette,
    )
    drawBianliangBoat(
        origin = Offset(size.width * 0.14f, size.height * 0.82f),
        unit = unit,
        palette = palette,
    )
    drawBianliangWillow(
        origin = Offset(size.width * 0.88f, riverTop - unit * 1.5f),
        unit = unit,
        palette = palette,
    )

    repeat(6) { index ->
        val mistWidth = size.width * (0.10f + index % 3 * 0.035f)
        val travel = size.width + mistWidth
        val x = (size.width * (0.08f + index * 0.19f) + progress * travel * 0.55f) % travel - mistWidth
        drawRect(
            color = palette.highlight.copy(alpha = 0.026f + index % 2 * 0.012f),
            topLeft = Offset(x, size.height * (0.22f + index * 0.075f)),
            size = Size(mistWidth, unit * 0.7f),
        )
    }
}

internal fun DrawScope.drawBianliangCardMaterial(
    palette: PixelPalette,
    line: Float,
) {
    repeat(4) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.018f + index * 0.006f),
            topLeft = Offset(size.width * (0.07f + index * 0.08f), size.height * (0.24f + index * 0.17f)),
            size = Size(size.width * (0.44f - index * 0.04f), line * 0.48f),
        )
    }
    val pavingTop = size.height - line * 5f
    drawRect(
        color = palette.outline.copy(alpha = 0.055f),
        topLeft = Offset(0f, pavingTop),
        size = Size(size.width, line * 5f),
    )
    repeat(7) { index ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.055f),
            topLeft = Offset(size.width * (0.04f + index * 0.15f), pavingTop + line * (1f + index % 2 * 1.8f)),
            size = Size(size.width * 0.09f, line * 0.38f),
        )
    }
}

internal fun DrawScope.drawBianliangHudAccent(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    val centerX = size.width / 2f
    drawRect(
        color = palette.shadow.copy(alpha = 0.56f),
        topLeft = Offset(centerX - unit * 7f, top + unit * 1.8f),
        size = Size(unit * 14f, unit * 0.55f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.70f),
        topLeft = Offset(centerX - unit * 5f, top + unit),
        size = Size(unit * 10f, unit * 0.5f),
    )
    repeat(3) { index ->
        drawRect(
            color = palette.outline.copy(alpha = 0.68f),
            topLeft = Offset(centerX - unit * 2.5f + index * unit * 2f, top),
            size = Size(unit, unit),
            style = Stroke(width = unit * 0.28f),
        )
    }
}

internal fun DrawScope.drawBianliangModeHudPolish(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    drawBianliangLantern(Offset(unit * 7f, top + unit * 1.2f), unit * 0.52f, palette)
    drawBianliangLantern(Offset(size.width - unit * 8.5f, top + unit * 1.2f), unit * 0.52f, palette)
    listOf(0.14f, 0.86f).forEach { x ->
        drawRect(
            color = palette.primary.copy(alpha = 0.46f),
            topLeft = Offset(size.width * x, top + unit * 2.2f),
            size = Size(unit * 4f, unit * 0.38f),
        )
    }
}

internal fun DrawScope.drawBianliangNavigationAccent(
    palette: PixelPalette,
    unit: Float,
) {
    val bridgeY = size.height - unit * 1.4f
    repeat(7) { index ->
        val distance = kotlin.math.abs(index - 3)
        val y = bridgeY - unit * (2.8f - distance * 0.55f)
        drawRect(
            color = palette.outline.copy(alpha = 0.70f),
            topLeft = Offset(size.width * (0.16f + index * 0.11f), y),
            size = Size(size.width * 0.09f, unit * 0.55f),
        )
    }
    drawBianliangLantern(Offset(unit * 5f, unit * 3.1f), unit * 0.46f, palette)
    drawBianliangLantern(Offset(size.width - unit * 6.4f, unit * 3.1f), unit * 0.46f, palette)
}

internal fun DrawScope.drawBianliangModeNavigationFrame(
    palette: PixelPalette,
    unit: Float,
) {
    drawRect(
        color = palette.primary.copy(alpha = 0.66f),
        topLeft = Offset(size.width * 0.33f, 0f),
        size = Size(size.width * 0.34f, unit * 0.45f),
    )
    repeat(3) { index ->
        drawRect(
            color = palette.secondary.copy(alpha = 0.62f),
            topLeft = Offset(size.width * (0.43f + index * 0.07f), size.height - unit * 1.2f),
            size = Size(unit * 0.65f, unit * 0.45f),
        )
    }
}

internal fun DrawScope.drawBianliangIndicatorAccent(
    palette: PixelPalette,
    unit: Float,
) {
    val pendant = Offset(size.width - unit * 5f, unit * 1.1f)
    drawRect(
        color = palette.primary.copy(alpha = 0.82f),
        topLeft = pendant,
        size = Size(unit * 2.8f, unit * 2.8f),
        style = Stroke(width = unit * 0.42f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.78f),
        topLeft = pendant + Offset(unit * 0.85f, unit * 0.85f),
        size = Size(unit * 1.1f, unit * 1.1f),
    )
}

internal fun DrawScope.drawBianliangModeIndicatorPolish(
    palette: PixelPalette,
    unit: Float,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.68f),
        topLeft = Offset(unit * 2f, size.height - unit * 1.25f),
        size = Size(size.width * 0.26f, unit * 0.38f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.72f),
        topLeft = Offset(size.width * 0.44f, size.height - unit * 1.25f),
        size = Size(size.width * 0.22f, unit * 0.38f),
    )
}

private fun DrawScope.drawBianliangShops(
    streetBase: Float,
    unit: Float,
    palette: PixelPalette,
) {
    val shops = listOf(
        Triple(0.03f, 0.18f, 8f),
        Triple(0.23f, 0.20f, 11f),
        Triple(0.73f, 0.17f, 9f),
    )
    shops.forEachIndexed { index, (xFraction, widthFraction, heightUnits) ->
        val x = size.width * xFraction
        val width = size.width * widthFraction
        val top = streetBase - unit * heightUnits
        drawRect(
            color = palette.shadow.copy(alpha = 0.11f),
            topLeft = Offset(x, top),
            size = Size(width, unit * heightUnits),
        )
        drawRect(
            color = palette.outline.copy(alpha = 0.15f),
            topLeft = Offset(x - unit, top - unit),
            size = Size(width + unit * 2f, unit),
        )
        repeat(3) { window ->
            drawRect(
                color = palette.primary.copy(alpha = 0.10f),
                topLeft = Offset(x + width * (0.14f + window * 0.29f), top + unit * 2.4f),
                size = Size(width * 0.16f, unit * 3f),
                style = Stroke(width = unit * 0.32f),
            )
        }
        drawBianliangLantern(
            origin = Offset(x + width * (if (index % 2 == 0) 0.18f else 0.78f), top + unit * 1.8f),
            unit = unit * 0.44f,
            palette = palette,
        )
        drawRect(
            color = palette.secondary.copy(alpha = 0.14f),
            topLeft = Offset(x + width * 0.08f, top + unit * 6.5f),
            size = Size(width * 0.84f, unit * 0.55f),
        )
    }
}

private fun DrawScope.drawBianliangRainbowBridge(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(9) { index ->
        val distance = kotlin.math.abs(index - 4)
        val rise = unit * (6f - distance * 0.95f)
        val x = origin.x + unit * index * 2.1f
        drawRect(
            color = palette.outline.copy(alpha = 0.16f),
            topLeft = Offset(x, origin.y - rise),
            size = Size(unit * 2.2f, unit * 0.72f),
        )
        drawRect(
            color = palette.shadow.copy(alpha = 0.10f),
            topLeft = Offset(x + unit * 0.8f, origin.y - rise + unit * 0.72f),
            size = Size(unit * 0.45f, rise),
        )
    }
    drawRect(
        color = palette.secondary.copy(alpha = 0.12f),
        topLeft = Offset(origin.x + unit * 2f, origin.y - unit * 8f),
        size = Size(unit * 15f, unit * 0.45f),
    )
}

private fun DrawScope.drawBianliangBoat(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.shadow.copy(alpha = 0.16f),
        topLeft = origin,
        size = Size(unit * 17f, unit * 1.7f),
    )
    drawRect(
        color = palette.outline.copy(alpha = 0.14f),
        topLeft = origin + Offset(unit * 3f, -unit * 2.8f),
        size = Size(unit * 10f, unit * 2.8f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.12f),
        topLeft = origin + Offset(unit * 5f, -unit * 2.2f),
        size = Size(unit * 6f, unit * 0.45f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.12f),
        topLeft = origin + Offset(unit * 8f, -unit * 5.4f),
        size = Size(unit * 0.45f, unit * 2.6f),
    )
}

private fun DrawScope.drawBianliangWillow(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.secondary.copy(alpha = 0.12f),
        topLeft = origin - Offset(unit * 0.7f, unit * 12f),
        size = Size(unit * 1.4f, unit * 12f),
    )
    repeat(5) { branch ->
        val x = origin.x + unit * (branch - 2) * 1.6f
        val top = origin.y - unit * (13f - branch % 2 * 1.5f)
        drawRect(
            color = palette.primary.copy(alpha = 0.09f + branch % 2 * 0.02f),
            topLeft = Offset(x, top),
            size = Size(unit * 0.48f, unit * (5f + branch % 3 * 1.4f)),
        )
        repeat(3) { leaf ->
            drawRect(
                color = palette.primary.copy(alpha = 0.10f),
                topLeft = Offset(x - unit * 0.7f, top + unit * (1f + leaf * 1.8f)),
                size = Size(unit * 1.6f, unit * 0.42f),
            )
        }
    }
}

private fun DrawScope.drawBianliangLantern(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.62f),
        topLeft = origin,
        size = Size(unit * 0.38f, unit * 1.3f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.82f),
        topLeft = origin + Offset(-unit * 0.65f, unit * 1.3f),
        size = Size(unit * 1.7f, unit * 2.1f),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.42f),
        topLeft = origin + Offset(-unit * 0.18f, unit * 1.7f),
        size = Size(unit * 0.76f, unit * 1.2f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.68f),
        topLeft = origin + Offset(0f, unit * 3.4f),
        size = Size(unit * 0.32f, unit * 1.2f),
    )
}

private fun DrawScope.drawBianliangMottoFrame(
    palette: PixelPalette,
    unit: Float,
) {
    listOf(unit * 2.2f, size.width - unit * 7.2f).forEach { x ->
        drawRect(
            color = palette.primary.copy(alpha = 0.68f),
            topLeft = Offset(x, unit * 2f),
            size = Size(unit * 5f, unit * 0.45f),
        )
        repeat(2) { column ->
            repeat(2) { row ->
                drawRect(
                    color = palette.outline.copy(alpha = 0.60f),
                    topLeft = Offset(x + unit * (1.2f + column * 2f), unit * (3.2f + row * 2f)),
                    size = Size(unit * 0.75f, unit * 0.75f),
                    style = Stroke(width = unit * 0.26f),
                )
            }
        }
    }
    drawRect(
        color = palette.secondary.copy(alpha = 0.76f),
        topLeft = Offset(unit * 7f, size.height - unit * 2f),
        size = Size(unit, unit),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.76f),
        topLeft = Offset(size.width - unit * 8f, size.height - unit * 2f),
        size = Size(unit, unit),
    )
}
