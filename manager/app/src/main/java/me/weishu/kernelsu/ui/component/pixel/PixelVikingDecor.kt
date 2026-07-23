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
fun PixelVikingMotto(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle() || LocalPixelStyle.current != PixelStyle.VikingSnowfield) return
    val palette = pixelPalette(PixelStyle.VikingSnowfield, isInDarkTheme())
    val shape = pixelMottoShape
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.95f), shape)
            .border(1.dp, palette.secondary.copy(alpha = 0.78f), shape)
            .drawWithContent {
                drawContent()
                drawVikingMottoFrame(palette, 2.dp.toPx())
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pixel_viking_motto),
            modifier = Modifier.padding(horizontal = 42.dp),
            color = palette.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun DrawScope.drawVikingScene(
    palette: PixelPalette,
    progress: Float,
    dark: Boolean,
) {
    val unit = 5.dp.toPx()
    val horizon = size.height * 0.68f

    if (dark) {
        drawVikingAurora(palette, progress, unit)
    }

    drawVikingMountain(
        centerX = size.width * 0.22f,
        baseY = horizon,
        unit = unit,
        halfSteps = 15,
        palette = palette,
        distant = true,
    )
    drawVikingMountain(
        centerX = size.width * 0.72f,
        baseY = horizon,
        unit = unit,
        halfSteps = 20,
        palette = palette,
        distant = false,
    )

    drawRect(
        color = palette.backgroundAlt.copy(alpha = 0.72f),
        topLeft = Offset(0f, horizon),
        size = Size(size.width, size.height - horizon),
    )
    drawRect(
        color = palette.highlight.copy(alpha = if (dark) 0.065f else 0.22f),
        topLeft = Offset(0f, size.height * 0.80f),
        size = Size(size.width, size.height * 0.20f),
    )

    repeat(8) { index ->
        val x = size.width * (0.03f + index * 0.125f)
        val treeUnit = unit * (0.62f + index % 3 * 0.10f)
        drawVikingPine(
            origin = Offset(x, horizon - treeUnit * (8f + index % 2 * 1.5f)),
            unit = treeUnit,
            palette = palette,
        )
    }

    drawVikingIceCave(
        origin = Offset(size.width * 0.02f, horizon - unit * 5.4f),
        unit = unit * 0.82f,
        palette = palette,
    )
    drawVikingLongship(
        origin = Offset(size.width * 0.12f, size.height * 0.74f),
        unit = unit,
        palette = palette,
    )
    drawVikingLonghouse(
        origin = Offset(size.width * 0.67f, horizon - unit * 8.5f),
        unit = unit,
        palette = palette,
    )
    drawVikingRunestone(
        origin = Offset(size.width * 0.87f, horizon - unit * 5.8f),
        unit = unit * 0.78f,
        palette = palette,
    )
    drawVikingReindeer(
        origin = Offset(size.width * 0.54f, horizon - unit * 2.4f),
        unit = unit * 0.62f,
        palette = palette,
    )
    drawVikingWolf(
        origin = Offset(size.width * 0.61f, horizon - unit * 1.4f),
        unit = unit * 0.48f,
        palette = palette,
    )
    drawVikingCampfire(
        origin = Offset(size.width * 0.76f, size.height * 0.78f),
        unit = unit * 0.72f,
        palette = palette,
    )

    drawVikingIceFissure(
        origin = Offset(size.width * 0.40f, size.height * 0.88f),
        unit = unit,
        palette = palette,
    )
    drawVikingCrystal(
        origin = Offset(size.width * 0.92f, size.height * 0.88f),
        unit = unit * 0.72f,
        palette = palette,
    )

    repeat(24) { index ->
        val travelX = size.width + unit * 5f
        val travelY = size.height + unit * 5f
        val x = (
            size.width * ((index * 37 % 101) / 101f) +
                progress * travelX * (0.11f + index % 4 * 0.018f)
            ) % travelX - unit * 2f
        val y = (
            size.height * ((index * 61 % 103) / 103f) +
                progress * travelY * (0.42f + index % 5 * 0.035f)
            ) % travelY - unit * 2f
        val side = unit * (0.22f + index % 3 * 0.10f)
        drawRect(
            color = palette.highlight.copy(alpha = 0.20f + index % 4 * 0.045f),
            topLeft = Offset(x, y),
            size = Size(side, side),
        )
    }
}

internal fun DrawScope.drawVikingCardMaterial(
    palette: PixelPalette,
    line: Float,
) {
    drawRect(
        color = palette.highlight.copy(alpha = 0.060f),
        topLeft = Offset.Zero,
        size = Size(size.width, line * 3.4f),
    )
    repeat(6) { index ->
        val x = size.width * (0.08f + index * 0.15f)
        val y = size.height * (0.20f + index % 3 * 0.21f)
        drawLine(
            color = palette.outline.copy(alpha = 0.030f + index % 2 * 0.012f),
            start = Offset(x, y),
            end = Offset(x + size.width * 0.08f, y + line * (1.2f + index % 2)),
            strokeWidth = line * 0.45f,
        )
    }
    repeat(5) { index ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.024f + index % 2 * 0.010f),
            topLeft = Offset(size.width * (0.09f + index * 0.18f), size.height - line * 3f),
            size = Size(size.width * 0.10f, line * 0.45f),
        )
    }
}

internal fun DrawScope.drawVikingHudAccent(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    val centerX = size.width / 2f
    drawRect(
        color = palette.outline.copy(alpha = 0.72f),
        topLeft = Offset(centerX - unit * 6f, top + unit * 1.8f),
        size = Size(unit * 12f, unit * 0.48f),
    )
    listOf(-4f, -2f, 0f, 2f, 4f).forEachIndexed { index, offset ->
        val height = unit * (0.75f + index % 3 * 0.42f)
        drawRect(
            color = (if (index == 2) palette.secondary else palette.primary)
                .copy(alpha = 0.68f),
            topLeft = Offset(centerX + offset * unit - unit * 0.35f, top + unit * 1.7f - height),
            size = Size(unit * 0.7f, height),
        )
    }
}

internal fun DrawScope.drawVikingModeHudPolish(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    drawVikingMiniAxe(Offset(unit * 6.4f, top + unit * 0.6f), unit * 0.52f, palette, false)
    drawVikingMiniAxe(Offset(size.width - unit * 8.2f, top + unit * 0.6f), unit * 0.52f, palette, true)
    drawVikingCrystal(Offset(size.width * 0.12f, top + unit * 2.2f), unit * 0.34f, palette)
    drawVikingCrystal(Offset(size.width * 0.86f, top + unit * 2.2f), unit * 0.34f, palette)
}

internal fun DrawScope.drawVikingNavigationAccent(
    palette: PixelPalette,
    unit: Float,
) {
    val hullY = size.height - unit * 1.45f
    drawRect(
        color = palette.shadow.copy(alpha = 0.78f),
        topLeft = Offset(size.width * 0.16f, hullY),
        size = Size(size.width * 0.68f, unit * 0.58f),
    )
    drawRect(
        color = palette.outline.copy(alpha = 0.74f),
        topLeft = Offset(size.width * 0.22f, hullY - unit * 0.52f),
        size = Size(size.width * 0.56f, unit * 0.52f),
    )
    repeat(5) { index ->
        drawVikingShield(
            origin = Offset(size.width * (0.29f + index * 0.105f), hullY - unit * 1.15f),
            unit = unit * 0.45f,
            palette = palette,
            violet = index == 2,
        )
    }
    drawRect(
        color = palette.primary.copy(alpha = 0.66f),
        topLeft = Offset(size.width * 0.50f, unit * 0.35f),
        size = Size(unit * 0.45f, hullY - unit * 0.5f),
    )
}

internal fun DrawScope.drawVikingModeNavigationFrame(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(7) { index ->
        val width = size.width * (0.07f + index % 2 * 0.02f)
        drawRect(
            color = (if (index == 3) palette.secondary else palette.highlight)
                .copy(alpha = if (index == 3) 0.60f else 0.34f),
            topLeft = Offset(size.width * (0.15f + index * 0.11f), 0f),
            size = Size(width, unit * (0.34f + index % 3 * 0.10f)),
        )
    }
}

internal fun DrawScope.drawVikingIndicatorAccent(
    palette: PixelPalette,
    unit: Float,
) {
    drawVikingShield(
        origin = Offset(size.width - unit * 5f, unit * 0.7f),
        unit = unit * 0.70f,
        palette = palette,
        violet = true,
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.58f),
        topLeft = Offset(unit * 2f, size.height - unit * 1.15f),
        size = Size(size.width * 0.24f, unit * 0.36f),
    )
}

internal fun DrawScope.drawVikingModeIndicatorPolish(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(3) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.60f + index * 0.06f),
            topLeft = Offset(size.width * (0.21f + index * 0.12f), unit * (0.55f + index % 2 * 0.45f)),
            size = Size(unit * 0.48f, size.height * (0.24f + index * 0.08f)),
        )
    }
}

private fun DrawScope.drawVikingAurora(
    palette: PixelPalette,
    progress: Float,
    unit: Float,
) {
    repeat(3) { band ->
        repeat(15) { segment ->
            val segmentWidth = size.width * 0.055f
            val travel = size.width + segmentWidth * 2f
            val x = (
                size.width * (0.03f + segment * 0.068f) +
                    progress * travel * (0.025f + band * 0.008f)
                ) % travel - segmentWidth
            val step = (segment * (band + 2) + band * 3) % 7
            val y = size.height * (0.10f + band * 0.055f) + unit * (step - 3) * 0.42f
            drawRect(
                color = (if (band == 1) palette.primary else palette.secondary)
                    .copy(alpha = 0.075f + band * 0.025f),
                topLeft = Offset(x, y),
                size = Size(segmentWidth + unit * 0.4f, unit * (0.58f + band * 0.18f)),
            )
        }
    }
}

private fun DrawScope.drawVikingMountain(
    centerX: Float,
    baseY: Float,
    unit: Float,
    halfSteps: Int,
    palette: PixelPalette,
    distant: Boolean,
) {
    val rowHeight = unit * if (distant) 0.72f else 0.88f
    repeat(halfSteps) { row ->
        val width = unit * (2f + row * 2.35f)
        val y = baseY - rowHeight * (halfSteps - row)
        drawRect(
            color = palette.outline.copy(alpha = if (distant) 0.18f else 0.28f),
            topLeft = Offset(centerX - width / 2f, y),
            size = Size(width, rowHeight + unit * 0.08f),
        )
        if (row < halfSteps / 3) {
            drawRect(
                color = palette.highlight.copy(alpha = if (distant) 0.16f else 0.24f),
                topLeft = Offset(centerX - width * 0.16f, y),
                size = Size(width * 0.34f, rowHeight),
            )
        }
    }
}

private fun DrawScope.drawVikingPine(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.shadow.copy(alpha = 0.52f),
        topLeft = origin + Offset(unit * 2.7f, unit * 3f),
        size = Size(unit * 0.65f, unit * 6f),
    )
    repeat(4) { tier ->
        val width = unit * (2.8f + tier * 1.7f)
        drawRect(
            color = palette.primary.copy(alpha = 0.30f + tier * 0.035f),
            topLeft = origin + Offset(unit * 3f - width / 2f, unit * (1.2f + tier * 1.55f)),
            size = Size(width, unit * 1.25f),
        )
        drawRect(
            color = palette.highlight.copy(alpha = 0.22f),
            topLeft = origin + Offset(unit * 3f - width / 2f, unit * (1f + tier * 1.55f)),
            size = Size(width * 0.55f, unit * 0.32f),
        )
    }
}

private fun DrawScope.drawVikingIceCave(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(4) { level ->
        val width = unit * (8f - level * 1.4f)
        drawRect(
            color = palette.outline.copy(alpha = 0.36f + level * 0.05f),
            topLeft = origin + Offset(unit * 4f - width / 2f, unit * level * 0.85f),
            size = Size(width, unit * 0.9f),
        )
    }
    drawRect(
        color = palette.shadow.copy(alpha = 0.72f),
        topLeft = origin + Offset(unit * 2.1f, unit * 2.3f),
        size = Size(unit * 3.8f, unit * 3.2f),
    )
    repeat(3) { index ->
        drawRect(
            color = palette.highlight.copy(alpha = 0.40f + index * 0.06f),
            topLeft = origin + Offset(unit * (1.2f + index * 2.5f), unit * 0.8f),
            size = Size(unit * 0.42f, unit * (1.3f + index % 2 * 0.7f)),
        )
    }
}

private fun DrawScope.drawVikingLongship(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(palette.shadow.copy(alpha = 0.70f), origin + Offset(0f, unit * 5f), Size(unit * 16f, unit * 1.2f))
    drawRect(palette.outline.copy(alpha = 0.76f), origin + Offset(unit, unit * 4.2f), Size(unit * 14f, unit))
    drawRect(palette.outline.copy(alpha = 0.64f), origin + Offset(unit * 2f, unit * 6.2f), Size(unit * 12f, unit * 0.7f))
    drawRect(palette.primary.copy(alpha = 0.72f), origin + Offset(unit * 7.7f, 0f), Size(unit * 0.6f, unit * 4.5f))
    repeat(4) { row ->
        val width = unit * (6f - row * 1.15f)
        drawRect(
            color = palette.secondary.copy(alpha = 0.54f + row * 0.04f),
            topLeft = origin + Offset(unit * 8f - width / 2f, unit * (0.7f + row * 0.8f)),
            size = Size(width, unit * 0.72f),
        )
    }
    repeat(5) { index ->
        drawVikingShield(
            origin = origin + Offset(unit * (3f + index * 2.25f), unit * 4.35f),
            unit = unit * 0.45f,
            palette = palette,
            violet = index == 2,
        )
    }
    drawRect(palette.primary.copy(alpha = 0.70f), origin + Offset(-unit, unit * 2.8f), Size(unit * 1.1f, unit * 3f))
    drawRect(palette.primary.copy(alpha = 0.70f), origin + Offset(unit * 15.9f, unit * 2.8f), Size(unit * 1.1f, unit * 3f))
}

private fun DrawScope.drawVikingLonghouse(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(palette.outline.copy(alpha = 0.54f), origin + Offset(unit, unit * 3.2f), Size(unit * 11f, unit * 5.2f))
    repeat(4) { level ->
        val width = unit * (13f - level * 2.2f)
        drawRect(
            color = palette.shadow.copy(alpha = 0.62f),
            topLeft = origin + Offset(unit * 6.5f - width / 2f, unit * level * 0.8f),
            size = Size(width, unit * 0.85f),
        )
        drawRect(
            color = palette.highlight.copy(alpha = 0.24f),
            topLeft = origin + Offset(unit * 6.5f - width / 2f, unit * level * 0.8f),
            size = Size(width * 0.58f, unit * 0.28f),
        )
    }
    drawRect(palette.shadow.copy(alpha = 0.74f), origin + Offset(unit * 5.3f, unit * 5f), Size(unit * 2.3f, unit * 3.4f))
    drawRect(palette.secondary.copy(alpha = 0.58f), origin + Offset(unit * 2f, unit * 5f), Size(unit * 1.4f, unit * 1.4f))
    drawRect(palette.secondary.copy(alpha = 0.58f), origin + Offset(unit * 9.5f, unit * 5f), Size(unit * 1.4f, unit * 1.4f))
}

private fun DrawScope.drawVikingRunestone(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(palette.shadow.copy(alpha = 0.66f), origin + Offset(0f, unit), Size(unit * 4f, unit * 6f))
    drawRect(palette.outline.copy(alpha = 0.74f), origin + Offset(unit * 0.45f, unit * 0.45f), Size(unit * 3.1f, unit * 6.1f))
    drawVikingRune(origin + Offset(unit * 1.1f, unit * 1.4f), unit * 0.55f, palette.secondary.copy(alpha = 0.76f))
    drawVikingRune(origin + Offset(unit * 2f, unit * 3.4f), unit * 0.48f, palette.primary.copy(alpha = 0.72f))
}

private fun DrawScope.drawVikingReindeer(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(palette.outline.copy(alpha = 0.54f), origin, Size(unit * 5f, unit * 2.2f))
    drawRect(palette.outline.copy(alpha = 0.54f), origin + Offset(unit * 4.4f, -unit * 1.3f), Size(unit * 1.6f, unit * 2.1f))
    listOf(0.6f, 3.7f).forEach { x ->
        drawRect(palette.shadow.copy(alpha = 0.62f), origin + Offset(unit * x, unit * 2f), Size(unit * 0.55f, unit * 2.4f))
    }
    drawRect(palette.outline.copy(alpha = 0.62f), origin + Offset(unit * 4.8f, -unit * 2.4f), Size(unit * 0.40f, unit * 1.3f))
    drawRect(palette.outline.copy(alpha = 0.62f), origin + Offset(unit * 5.8f, -unit * 2.4f), Size(unit * 0.40f, unit * 1.3f))
    drawRect(palette.outline.copy(alpha = 0.62f), origin + Offset(unit * 4.3f, -unit * 2.2f), Size(unit * 1.1f, unit * 0.40f))
    drawRect(palette.outline.copy(alpha = 0.62f), origin + Offset(unit * 5.5f, -unit * 2.2f), Size(unit * 1.1f, unit * 0.40f))
}

private fun DrawScope.drawVikingWolf(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(palette.shadow.copy(alpha = 0.58f), origin, Size(unit * 4.6f, unit * 1.7f))
    drawRect(palette.outline.copy(alpha = 0.62f), origin + Offset(unit * 3.9f, -unit * 0.9f), Size(unit * 1.6f, unit * 1.8f))
    drawRect(palette.outline.copy(alpha = 0.62f), origin + Offset(unit * 4f, -unit * 1.6f), Size(unit * 0.55f, unit * 0.8f))
    drawRect(palette.outline.copy(alpha = 0.62f), origin + Offset(unit * 5f, -unit * 1.6f), Size(unit * 0.55f, unit * 0.8f))
    listOf(0.7f, 3.3f).forEach { x ->
        drawRect(palette.shadow.copy(alpha = 0.64f), origin + Offset(unit * x, unit * 1.5f), Size(unit * 0.48f, unit * 1.6f))
    }
    drawRect(palette.shadow.copy(alpha = 0.56f), origin + Offset(-unit * 1.5f, -unit * 0.2f), Size(unit * 1.7f, unit * 0.48f))
}

private fun DrawScope.drawVikingCampfire(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(palette.outline.copy(alpha = 0.66f), origin + Offset(-unit, unit * 2.4f), Size(unit * 4f, unit * 0.55f))
    drawRect(palette.secondary.copy(alpha = 0.72f), origin + Offset(0f, unit), Size(unit * 2f, unit * 1.6f))
    drawRect(palette.highlight.copy(alpha = 0.70f), origin + Offset(unit * 0.65f, 0f), Size(unit * 0.7f, unit * 1.8f))
}

private fun DrawScope.drawVikingIceFissure(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    val points = listOf(
        origin,
        origin + Offset(unit * 3f, unit * 0.8f),
        origin + Offset(unit * 5f, -unit * 0.2f),
        origin + Offset(unit * 9f, unit * 1.2f),
    )
    points.zipWithNext().forEach { (start, end) ->
        drawLine(palette.primary.copy(alpha = 0.46f), start, end, unit * 0.32f)
    }
    drawLine(
        palette.secondary.copy(alpha = 0.44f),
        points[1],
        points[1] + Offset(-unit * 0.8f, unit * 2.2f),
        unit * 0.28f,
    )
}

private fun DrawScope.drawVikingCrystal(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(palette.secondary.copy(alpha = 0.72f), origin + Offset(unit, 0f), Size(unit, unit * 3f))
    drawRect(palette.secondary.copy(alpha = 0.58f), origin + Offset(0f, unit), Size(unit, unit * 2f))
    drawRect(palette.primary.copy(alpha = 0.58f), origin + Offset(unit * 2f, unit * 0.6f), Size(unit, unit * 2.4f))
    drawRect(palette.highlight.copy(alpha = 0.56f), origin + Offset(unit * 1.25f, unit * 0.5f), Size(unit * 0.35f, unit * 1.4f))
}

private fun DrawScope.drawVikingShield(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
    violet: Boolean,
) {
    val color = if (violet) palette.secondary else palette.primary
    drawRect(color.copy(alpha = 0.78f), origin, Size(unit * 2f, unit * 2f), style = Stroke(width = unit * 0.45f))
    drawRect(palette.highlight.copy(alpha = 0.62f), origin + Offset(unit * 0.75f, unit * 0.75f), Size(unit * 0.5f, unit * 0.5f))
}

private fun DrawScope.drawVikingMiniAxe(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
    mirrored: Boolean,
) {
    val direction = if (mirrored) -1f else 1f
    drawLine(
        color = palette.outline.copy(alpha = 0.78f),
        start = origin,
        end = origin + Offset(unit * 3.2f * direction, unit * 3.2f),
        strokeWidth = unit * 0.50f,
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.76f),
        topLeft = origin + Offset(unit * (if (mirrored) -1.8f else 0.2f), -unit * 0.25f),
        size = Size(unit * 1.7f, unit * 1.1f),
    )
}

private fun DrawScope.drawVikingRune(
    origin: Offset,
    unit: Float,
    color: Color,
) {
    drawRect(color, origin + Offset(unit, 0f), Size(unit * 0.42f, unit * 3f))
    drawRect(color, origin + Offset(0f, unit * 0.7f), Size(unit * 2.2f, unit * 0.42f))
    drawLine(color, origin + Offset(unit * 1.2f, unit), origin + Offset(unit * 2.1f, unit * 2.1f), unit * 0.36f)
}

private fun DrawScope.drawVikingMottoFrame(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(8) { index ->
        val width = size.width * (0.055f + index % 2 * 0.014f)
        drawRect(
            color = palette.highlight.copy(alpha = 0.48f + index % 3 * 0.06f),
            topLeft = Offset(size.width * (0.03f + index * 0.12f), 0f),
            size = Size(width, unit * (0.42f + index % 2 * 0.12f)),
        )
    }
    drawVikingRune(Offset(unit * 4f, size.height * 0.37f), unit * 0.52f, palette.primary.copy(alpha = 0.74f))
    drawVikingRune(Offset(size.width - unit * 7f, size.height * 0.37f), unit * 0.52f, palette.secondary.copy(alpha = 0.74f))
    drawRect(
        color = palette.outline.copy(alpha = 0.58f),
        topLeft = Offset(size.width * 0.29f, size.height - unit * 0.55f),
        size = Size(size.width * 0.42f, unit * 0.42f),
    )
}
