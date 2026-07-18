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
fun PixelJiangnanMotto(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle() || LocalPixelStyle.current != PixelStyle.JiangnanWatertown) return
    val palette = pixelPalette(PixelStyle.JiangnanWatertown, isInDarkTheme())
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.95f), shape)
            .border(1.dp, palette.outline.copy(alpha = 0.68f), shape)
            .drawWithContent {
                drawContent()
                drawJiangnanMottoFrame(palette, 2.dp.toPx())
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pixel_jiangnan_motto),
            modifier = Modifier.padding(horizontal = 42.dp),
            color = palette.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun DrawScope.drawJiangnanScene(
    palette: PixelPalette,
    progress: Float,
    dark: Boolean,
) {
    val unit = 5.dp.toPx()
    val riverTop = size.height * 0.68f

    if (dark) {
        repeat(4) { band ->
            drawRect(
                color = palette.secondary.copy(alpha = 0.038f + band * 0.012f),
                topLeft = Offset(size.width * (0.08f + band * 0.07f), size.height * (0.10f + band * 0.06f)),
                size = Size(size.width * (0.72f - band * 0.08f), unit * (0.58f + band * 0.12f)),
            )
        }
    }

    drawRect(
        color = palette.backgroundAlt.copy(alpha = 0.74f),
        topLeft = Offset(0f, riverTop),
        size = Size(size.width, size.height - riverTop),
    )
    repeat(6) { index ->
        val y = riverTop + unit * (2.5f + index * 3.2f)
        val width = size.width * (0.14f + index % 3 * 0.07f)
        val x = (size.width * (0.04f + index * 0.18f) + progress * size.width * 0.16f) % size.width
        drawRect(
            color = (if (index == 2 || index == 5) palette.secondary else palette.highlight)
                .copy(alpha = if (dark) 0.055f else 0.11f),
            topLeft = Offset(x - width / 2f, y),
            size = Size(width, unit * 0.34f),
        )
    }

    drawJiangnanHouse(
        origin = Offset(size.width * 0.02f, riverTop - unit * 10.5f),
        unit = unit,
        palette = palette,
        wide = true,
        dark = dark,
    )
    drawJiangnanHouse(
        origin = Offset(size.width * 0.72f, riverTop - unit * 9.4f),
        unit = unit * 0.92f,
        palette = palette,
        wide = false,
        dark = dark,
    )
    drawJiangnanStoneBridge(
        origin = Offset(size.width * 0.35f, riverTop - unit * 1.8f),
        unit = unit,
        palette = palette,
    )
    drawJiangnanBoat(
        origin = Offset(size.width * 0.18f, size.height * 0.80f),
        unit = unit * 0.82f,
        palette = palette,
    )
    drawJiangnanWillow(
        origin = Offset(size.width * 0.90f, riverTop - unit * 12f),
        unit = unit * 0.78f,
        palette = palette,
    )
    drawJiangnanLotusPond(
        origin = Offset(size.width * 0.67f, size.height * 0.84f),
        unit = unit * 0.70f,
        palette = palette,
    )
    drawJiangnanRiverSteps(
        origin = Offset(size.width * 0.04f, riverTop + unit * 1.2f),
        unit = unit * 0.78f,
        palette = palette,
    )
    drawJiangnanUmbrellaCat(
        origin = Offset(size.width * 0.55f, riverTop - unit * 5.2f),
        unit = unit * 0.58f,
        palette = palette,
    )
    drawJiangnanKoi(
        origin = Offset(size.width * 0.49f, size.height * 0.84f),
        unit = unit * 0.45f,
        palette = palette,
    )

    repeat(if (dark) 4 else 3) { index ->
        drawJiangnanLantern(
            origin = Offset(size.width * (0.13f + index * 0.24f), riverTop - unit * (7.4f + index % 2 * 1.3f)),
            unit = unit * 0.48f,
            palette = palette,
            lit = dark,
        )
    }

    repeat(8) { index ->
        val mistWidth = size.width * (0.11f + index % 3 * 0.045f)
        val travel = size.width + mistWidth
        val x = (
            size.width * (0.03f + index * 0.16f) +
                progress * travel * (0.18f + index % 2 * 0.035f)
            ) % travel - mistWidth
        drawRect(
            color = palette.highlight.copy(alpha = 0.028f + index % 3 * 0.010f),
            topLeft = Offset(x, size.height * (0.20f + index * 0.072f)),
            size = Size(mistWidth, unit * 0.62f),
        )
    }

    repeat(22) { index ->
        val travel = size.height + unit * 6f
        val y = (
            size.height * ((index * 47 % 101) / 101f) +
                progress * travel * (0.36f + index % 4 * 0.035f)
            ) % travel - unit * 3f
        val x = size.width * ((index * 31 % 97) / 97f)
        val length = unit * (0.52f + index % 3 * 0.18f)
        drawRect(
            color = palette.highlight.copy(alpha = 0.12f + index % 4 * 0.025f),
            topLeft = Offset(x, y),
            size = Size(unit * 0.16f, length),
        )
    }
}

internal fun DrawScope.drawJiangnanCardMaterial(
    palette: PixelPalette,
    line: Float,
) {
    repeat(5) { index ->
        drawRect(
            color = palette.outline.copy(alpha = 0.018f + index * 0.004f),
            topLeft = Offset(size.width * (0.06f + index * 0.08f), size.height * (0.18f + index * 0.15f)),
            size = Size(size.width * (0.38f - index * 0.025f), line * 0.34f),
        )
    }
    repeat(5) { index ->
        val y = size.height - line * (5.2f - index * 0.78f)
        drawRect(
            color = (if (index == 2) palette.secondary else palette.primary).copy(alpha = 0.026f),
            topLeft = Offset(size.width * (0.48f + index * 0.09f), y),
            size = Size(size.width * 0.13f, line * 0.32f),
        )
    }
}

internal fun DrawScope.drawJiangnanHudAccent(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    val centerX = size.width / 2f
    drawRect(
        color = palette.outline.copy(alpha = 0.60f),
        topLeft = Offset(centerX - unit * 7f, top + unit * 1.8f),
        size = Size(unit * 14f, unit * 0.34f),
    )
    repeat(5) { index ->
        val width = unit * (2.2f + index % 2 * 0.8f)
        drawRect(
            color = (if (index == 2) palette.secondary else palette.primary).copy(alpha = 0.58f),
            topLeft = Offset(centerX - unit * 6f + index * unit * 2.6f, top + unit * (0.55f + index % 2 * 0.45f)),
            size = Size(width, unit * 0.34f),
        )
    }
}

internal fun DrawScope.drawJiangnanModeHudPolish(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    drawJiangnanMiniUmbrella(Offset(unit * 6.4f, top + unit * 0.6f), unit * 0.48f, palette)
    drawJiangnanLantern(Offset(size.width - unit * 8f, top + unit * 0.6f), unit * 0.38f, palette, true)
}

internal fun DrawScope.drawJiangnanNavigationAccent(
    palette: PixelPalette,
    unit: Float,
) {
    val bridgeY = size.height - unit * 1.15f
    repeat(9) { index ->
        val distance = kotlin.math.abs(index - 4)
        val lift = unit * (3.0f - distance * 0.55f).coerceAtLeast(0.45f)
        drawRect(
            color = palette.outline.copy(alpha = 0.58f),
            topLeft = Offset(size.width * (0.10f + index * 0.095f), bridgeY - lift),
            size = Size(size.width * 0.082f, unit * 0.34f),
        )
    }
    drawJiangnanMiniBoat(
        origin = Offset(size.width * 0.42f, size.height - unit * 2.6f),
        unit = unit * 0.45f,
        palette = palette,
    )
    repeat(4) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.42f + index * 0.04f),
            topLeft = Offset(size.width * (0.30f + index * 0.13f), unit * (0.45f + index % 2 * 0.38f)),
            size = Size(size.width * 0.08f, unit * 0.30f),
        )
    }
}

internal fun DrawScope.drawJiangnanModeNavigationFrame(
    palette: PixelPalette,
    unit: Float,
) {
    drawRect(
        color = palette.primary.copy(alpha = 0.54f),
        topLeft = Offset(size.width * 0.28f, 0f),
        size = Size(size.width * 0.44f, unit * 0.30f),
    )
    listOf(0.10f, 0.90f).forEachIndexed { index, x ->
        drawJiangnanLantern(
            origin = Offset(size.width * x - unit, unit * 0.65f),
            unit = unit * 0.36f,
            palette = palette,
            lit = index == 1,
        )
    }
}

internal fun DrawScope.drawJiangnanIndicatorAccent(
    palette: PixelPalette,
    unit: Float,
) {
    drawJiangnanMiniUmbrella(
        origin = Offset(size.width - unit * 5.2f, unit * 0.6f),
        unit = unit * 0.62f,
        palette = palette,
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.52f),
        topLeft = Offset(unit * 2f, size.height - unit * 1.05f),
        size = Size(size.width * 0.24f, unit * 0.28f),
    )
}

internal fun DrawScope.drawJiangnanModeIndicatorPolish(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(3) { index ->
        drawRect(
            color = (if (index == 1) palette.secondary else palette.outline).copy(alpha = 0.54f),
            topLeft = Offset(size.width * (0.22f + index * 0.14f), unit * (0.75f + index % 2 * 0.4f)),
            size = Size(unit * 0.42f, unit * (0.8f + index * 0.28f)),
        )
    }
}

private fun DrawScope.drawJiangnanHouse(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
    wide: Boolean,
    dark: Boolean,
) {
    val width = unit * if (wide) 17f else 13f
    val height = unit * if (wide) 7.4f else 6.6f
    drawRect(
        color = palette.highlight.copy(alpha = if (dark) 0.16f else 0.62f),
        topLeft = origin + Offset(unit, unit * 3.4f),
        size = Size(width - unit * 2f, height),
    )
    repeat(4) { level ->
        val roofWidth = width - unit * (level * 2.6f)
        drawRect(
            color = palette.shadow.copy(alpha = 0.50f + level * 0.045f),
            topLeft = origin + Offset((width - roofWidth) / 2f, unit * (level * 0.82f)),
            size = Size(roofWidth, unit * 0.72f),
        )
    }
    repeat(if (wide) 4 else 3) { index ->
        drawJiangnanWindow(
            origin = origin + Offset(unit * (2.4f + index * 3.4f), unit * 5.1f),
            unit = unit * 0.62f,
            palette = palette,
            lit = dark && index == 1,
        )
    }
    repeat(5) { index ->
        drawRect(
            color = palette.outline.copy(alpha = 0.32f),
            topLeft = origin + Offset(unit * (2f + index * 2.6f), unit * 9.8f),
            size = Size(unit * 1.8f, unit * 0.30f),
        )
    }
}

private fun DrawScope.drawJiangnanWindow(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
    lit: Boolean,
) {
    val fill = if (lit) palette.secondary.copy(alpha = 0.42f) else palette.surfaceAlt.copy(alpha = 0.42f)
    drawRect(fill, origin, Size(unit * 3f, unit * 3.2f))
    drawRect(palette.shadow.copy(alpha = 0.58f), origin, Size(unit * 3f, unit * 3.2f), style = Stroke(width = unit * 0.28f))
    drawRect(palette.outline.copy(alpha = 0.56f), origin + Offset(unit * 1.35f, 0f), Size(unit * 0.28f, unit * 3.2f))
    drawRect(palette.outline.copy(alpha = 0.56f), origin + Offset(0f, unit * 1.45f), Size(unit * 3f, unit * 0.28f))
}

private fun DrawScope.drawJiangnanStoneBridge(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(9) { index ->
        val distance = kotlin.math.abs(index - 4)
        val y = unit * (3.7f - distance * 0.62f).coerceAtLeast(0.7f)
        drawRect(
            color = palette.outline.copy(alpha = 0.58f + index % 2 * 0.04f),
            topLeft = origin + Offset(unit * index * 1.55f, unit * 4.2f - y),
            size = Size(unit * 1.45f, unit * 0.58f),
        )
    }
    drawRect(palette.shadow.copy(alpha = 0.42f), origin + Offset(0f, unit * 4.1f), Size(unit * 13.8f, unit * 0.42f))
    drawRect(palette.outline.copy(alpha = 0.44f), origin + Offset(unit * 0.4f, unit * 4.5f), Size(unit * 0.35f, unit * 2.6f))
    drawRect(palette.outline.copy(alpha = 0.44f), origin + Offset(unit * 13f, unit * 4.5f), Size(unit * 0.35f, unit * 2.6f))
}

private fun DrawScope.drawJiangnanBoat(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(palette.shadow.copy(alpha = 0.62f), origin + Offset(0f, unit * 3.4f), Size(unit * 13f, unit * 0.72f))
    drawRect(palette.outline.copy(alpha = 0.58f), origin + Offset(unit, unit * 4.1f), Size(unit * 11f, unit * 0.42f))
    repeat(4) { level ->
        val width = unit * (7.2f - level * 1.25f)
        drawRect(
            color = palette.shadow.copy(alpha = 0.52f + level * 0.04f),
            topLeft = origin + Offset(unit * 6.5f - width / 2f, unit * (0.45f + level * 0.72f)),
            size = Size(width, unit * 0.60f),
        )
    }
    drawRect(palette.highlight.copy(alpha = 0.40f), origin + Offset(unit * 5.2f, unit * 1.25f), Size(unit * 2.6f, unit * 1.25f))
}

private fun DrawScope.drawJiangnanMiniBoat(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(palette.shadow.copy(alpha = 0.68f), origin + Offset(0f, unit * 2f), Size(unit * 8f, unit * 0.62f))
    drawRect(palette.outline.copy(alpha = 0.58f), origin + Offset(unit, unit * 1.5f), Size(unit * 6f, unit * 0.50f))
    drawRect(palette.shadow.copy(alpha = 0.56f), origin + Offset(unit * 2.4f, unit * 0.5f), Size(unit * 3.2f, unit))
}

private fun DrawScope.drawJiangnanWillow(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(palette.shadow.copy(alpha = 0.46f), origin + Offset(unit * 3f, 0f), Size(unit * 0.72f, unit * 12f))
    repeat(6) { branch ->
        val side = if (branch % 2 == 0) -1f else 1f
        val start = origin + Offset(unit * 3.4f, unit * (1.2f + branch * 1.35f))
        drawLine(
            color = palette.primary.copy(alpha = 0.42f),
            start = start,
            end = start + Offset(unit * side * (2.5f + branch % 3), unit * (3f + branch % 2)),
            strokeWidth = unit * 0.34f,
        )
        repeat(3) { leaf ->
            drawRect(
                color = palette.primary.copy(alpha = 0.38f + leaf * 0.04f),
                topLeft = start + Offset(unit * side * (1.2f + leaf), unit * (1.4f + leaf * 0.7f)),
                size = Size(unit * 0.50f, unit * 0.82f),
            )
        }
    }
}

private fun DrawScope.drawJiangnanLotusPond(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(5) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.40f),
            topLeft = origin + Offset(unit * index * 2.1f, unit * (2.1f - index % 2 * 0.55f)),
            size = Size(unit * 1.7f, unit * 0.42f),
        )
    }
    listOf(1, 3).forEachIndexed { flower, index ->
        val center = origin + Offset(unit * (flower * 2.1f + 0.8f), unit * (0.75f + index * 0.35f))
        drawRect(palette.secondary.copy(alpha = 0.62f), center + Offset(-unit * 0.8f, 0f), Size(unit * 0.7f, unit * 0.7f))
        drawRect(palette.secondary.copy(alpha = 0.62f), center + Offset(unit * 0.1f, 0f), Size(unit * 0.7f, unit * 0.7f))
        drawRect(palette.highlight.copy(alpha = 0.58f), center, Size(unit * 0.55f, unit * 0.55f))
    }
}

private fun DrawScope.drawJiangnanRiverSteps(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(5) { step ->
        drawRect(
            color = palette.outline.copy(alpha = 0.42f + step * 0.035f),
            topLeft = origin + Offset(0f, unit * step * 0.92f),
            size = Size(unit * (7f - step * 0.9f), unit * 0.58f),
        )
    }
}

private fun DrawScope.drawJiangnanUmbrellaCat(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawJiangnanMiniUmbrella(origin, unit, palette)
    drawRect(palette.shadow.copy(alpha = 0.58f), origin + Offset(unit * 2.8f, unit * 4.4f), Size(unit * 1.5f, unit * 1.4f))
    drawRect(palette.shadow.copy(alpha = 0.58f), origin + Offset(unit * 2.8f, unit * 3.8f), Size(unit * 0.55f, unit * 0.7f))
    drawRect(palette.shadow.copy(alpha = 0.58f), origin + Offset(unit * 3.75f, unit * 3.8f), Size(unit * 0.55f, unit * 0.7f))
    drawRect(palette.secondary.copy(alpha = 0.56f), origin + Offset(unit * 3.3f, unit * 4.25f), Size(unit * 0.36f, unit * 0.36f))
}

private fun DrawScope.drawJiangnanMiniUmbrella(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(5) { segment ->
        val distance = kotlin.math.abs(segment - 2)
        drawRect(
            color = palette.secondary.copy(alpha = 0.54f + distance * 0.035f),
            topLeft = origin + Offset(unit * segment, unit * distance * 0.55f),
            size = Size(unit * 1.1f, unit * 0.60f),
        )
    }
    drawRect(palette.shadow.copy(alpha = 0.56f), origin + Offset(unit * 2.45f, unit * 0.65f), Size(unit * 0.32f, unit * 4.3f))
}

private fun DrawScope.drawJiangnanKoi(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(palette.secondary.copy(alpha = 0.48f), origin, Size(unit * 3.2f, unit * 0.72f))
    drawRect(palette.highlight.copy(alpha = 0.52f), origin + Offset(unit * 0.8f, unit * 0.12f), Size(unit * 0.65f, unit * 0.46f))
    drawRect(palette.secondary.copy(alpha = 0.48f), origin + Offset(unit * 3f, -unit * 0.55f), Size(unit, unit * 1.7f))
}

private fun DrawScope.drawJiangnanLantern(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
    lit: Boolean,
) {
    drawRect(palette.shadow.copy(alpha = 0.54f), origin + Offset(unit * 1.2f, 0f), Size(unit * 0.28f, unit * 1.1f))
    val lanternColor = if (lit) palette.secondary else palette.outline
    drawRect(lanternColor.copy(alpha = if (lit) 0.70f else 0.48f), origin + Offset(unit * 0.4f, unit), Size(unit * 1.8f, unit * 2.2f))
    drawRect(palette.highlight.copy(alpha = if (lit) 0.62f else 0.34f), origin + Offset(unit * 1f, unit * 1.35f), Size(unit * 0.55f, unit * 1.3f))
    drawRect(palette.shadow.copy(alpha = 0.50f), origin + Offset(unit * 1.1f, unit * 3.2f), Size(unit * 0.30f, unit * 1.1f))
}

private fun DrawScope.drawJiangnanMottoFrame(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(5) { index ->
        val width = size.width * (0.07f + index % 2 * 0.018f)
        drawRect(
            color = palette.primary.copy(alpha = 0.42f + index * 0.035f),
            topLeft = Offset(size.width * (0.31f + index * 0.085f), size.height - unit * (0.58f + index % 2 * 0.18f)),
            size = Size(width, unit * 0.30f),
        )
    }
    drawJiangnanMiniUmbrella(Offset(unit * 4.1f, unit * 1.05f), unit * 0.42f, palette)
    drawJiangnanLantern(Offset(size.width - unit * 7f, unit * 0.55f), unit * 0.34f, palette, true)
}
