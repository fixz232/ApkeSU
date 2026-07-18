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
fun PixelLavaValleyMotto(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle() || LocalPixelStyle.current != PixelStyle.LavaValley) return
    val palette = pixelPalette(PixelStyle.LavaValley, isInDarkTheme())
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.95f), shape)
            .border(1.dp, palette.primary.copy(alpha = 0.82f), shape)
            .drawWithContent {
                drawContent()
                drawLavaValleyMottoFrame(palette, 2.dp.toPx())
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pixel_lava_valley_motto),
            modifier = Modifier.padding(horizontal = 40.dp),
            color = palette.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun DrawScope.drawLavaValleyScene(
    palette: PixelPalette,
    progress: Float,
) {
    val unit = 5.dp.toPx()
    val valleyY = size.height * 0.66f

    drawRect(
        color = palette.shadow.copy(alpha = 0.15f),
        topLeft = Offset(0f, valleyY),
        size = Size(size.width, size.height - valleyY),
    )
    drawLavaVolcano(Offset(size.width * 0.16f, valleyY - unit * 15f), unit, palette)
    drawLavaCave(Offset(size.width * 0.70f, valleyY - unit * 8f), unit, palette)
    drawLavaRiver(Offset(size.width * 0.38f, valleyY - unit * 1.5f), unit, palette, progress)
    drawLavaStoneBridge(Offset(size.width * 0.34f, size.height * 0.79f), unit, palette)
    drawLavaGeode(Offset(size.width * 0.79f, size.height * 0.76f), unit, palette)
    drawObsidianPillars(Offset(size.width * 0.05f, size.height * 0.76f), unit, palette)
    drawVolcanicMoss(Offset(size.width * 0.62f, size.height * 0.89f), unit, palette)

    repeat(7) { index ->
        val smokeWidth = size.width * (0.08f + index % 3 * 0.035f)
        val travel = size.width + smokeWidth
        val x = (size.width * (0.04f + index * 0.16f) + progress * travel * (0.22f + index % 2 * 0.05f)) % travel - smokeWidth
        drawRect(
            color = palette.highlight.copy(alpha = 0.024f + index % 2 * 0.012f),
            topLeft = Offset(x, size.height * (0.16f + index * 0.085f)),
            size = Size(smokeWidth, unit * 0.72f),
        )
    }

    repeat(10) { index ->
        val travel = size.width * 1.1f
        val x = (size.width * (0.03f + index * 0.10f) + progress * travel * (0.26f + index % 3 * 0.04f)) % travel - size.width * 0.04f
        val y = size.height * (0.20f + (index * 0.151f) % 0.52f)
        val side = unit * (0.28f + index % 3 * 0.10f)
        drawRect(
            color = (if (index % 4 == 0) palette.secondary else palette.primary)
                .copy(alpha = 0.16f + index % 2 * 0.05f),
            topLeft = Offset(x, y),
            size = Size(side, side),
        )
    }

    repeat(6) { index ->
        val rise = ((progress + index * 0.17f) % 1f)
        val x = size.width * (0.28f + index * 0.065f)
        val y = valleyY - size.height * 0.28f * rise
        drawRect(
            color = palette.shadow.copy(alpha = 0.16f + index % 2 * 0.04f),
            topLeft = Offset(x, y),
            size = Size(unit * (0.7f + index % 2 * 0.35f), unit * (0.7f + index % 3 * 0.22f)),
        )
    }
}

internal fun DrawScope.drawLavaValleyCardMaterial(
    palette: PixelPalette,
    line: Float,
) {
    drawRect(
        color = palette.shadow.copy(alpha = 0.13f),
        topLeft = Offset.Zero,
        size = Size(size.width, line * 3f),
    )
    repeat(7) { index ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.035f + index % 2 * 0.014f),
            topLeft = Offset(size.width * (0.04f + index % 4 * 0.11f), size.height * (0.12f + index * 0.12f)),
            size = Size(size.width * (0.13f + index % 3 * 0.05f), line * 0.52f),
        )
    }
    repeat(4) { index ->
        drawRect(
            color = (if (index == 2) palette.secondary else palette.primary).copy(alpha = 0.032f),
            topLeft = Offset(size.width * (0.52f + index * 0.09f), size.height - line * (4.5f - index * 0.7f)),
            size = Size(size.width * 0.13f, line * 0.48f),
        )
    }
}

internal fun DrawScope.drawLavaValleyHudAccent(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    val centerX = size.width / 2f
    repeat(5) { index ->
        val width = unit * (2.1f + index % 2 * 0.8f)
        drawRect(
            color = palette.shadow.copy(alpha = 0.72f),
            topLeft = Offset(centerX - unit * 6.5f + index * unit * 2.8f, top + unit * (0.4f + index % 3 * 0.55f)),
            size = Size(width, unit * 0.52f),
        )
    }
    drawRect(
        color = palette.primary.copy(alpha = 0.82f),
        topLeft = Offset(centerX - unit * 4.8f, top + unit * 2.15f),
        size = Size(unit * 9.6f, unit * 0.42f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.76f),
        topLeft = Offset(centerX - unit * 0.55f, top + unit * 0.45f),
        size = Size(unit * 1.1f, unit * 1.1f),
    )
}

internal fun DrawScope.drawLavaValleyModeHudPolish(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    drawLavaMiniCrystal(Offset(unit * 6.5f, top + unit * 0.4f), unit * 0.46f, palette, purple = false)
    drawLavaMiniCrystal(Offset(size.width - unit * 8f, top + unit * 0.7f), unit * 0.44f, palette, purple = true)
    listOf(0.14f, 0.86f).forEachIndexed { index, x ->
        drawRect(
            color = (if (index == 0) palette.primary else palette.secondary).copy(alpha = 0.62f),
            topLeft = Offset(size.width * x, top + unit * 2.5f),
            size = Size(unit * 3.8f, unit * 0.38f),
        )
    }
}

internal fun DrawScope.drawLavaValleyNavigationAccent(
    palette: PixelPalette,
    unit: Float,
) {
    val slabY = size.height - unit * 1.35f
    repeat(7) { index ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.78f),
            topLeft = Offset(size.width * (0.11f + index * 0.115f), slabY - unit * (index % 2 * 0.32f)),
            size = Size(size.width * 0.095f, unit * 0.72f),
        )
    }
    repeat(5) { index ->
        val lift = unit * (index % 3 * 0.48f)
        drawRect(
            color = (if (index == 3) palette.secondary else palette.primary).copy(alpha = 0.72f),
            topLeft = Offset(size.width * (0.22f + index * 0.14f), unit * 0.9f - lift),
            size = Size(size.width * 0.10f, unit * 0.46f),
        )
    }
    drawLavaMiniCrystal(Offset(unit * 4.2f, unit * 1.5f), unit * 0.50f, palette, purple = false)
    drawLavaMiniCrystal(Offset(size.width - unit * 6.3f, unit * 1.6f), unit * 0.46f, palette, purple = true)
}

internal fun DrawScope.drawLavaValleyModeNavigationFrame(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(6) { index ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.66f + index % 2 * 0.08f),
            topLeft = Offset(size.width * (0.18f + index * 0.11f), 0f),
            size = Size(size.width * 0.085f, unit * 0.46f),
        )
    }
    drawRect(
        color = palette.primary.copy(alpha = 0.72f),
        topLeft = Offset(size.width * 0.42f, size.height - unit * 1.08f),
        size = Size(size.width * 0.16f, unit * 0.40f),
    )
}

internal fun DrawScope.drawLavaValleyIndicatorAccent(
    palette: PixelPalette,
    unit: Float,
) {
    drawLavaMiniCrystal(
        origin = Offset(size.width - unit * 5.5f, unit * 0.55f),
        unit = unit * 0.78f,
        palette = palette,
        purple = false,
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.72f),
        topLeft = Offset(unit * 2f, size.height - unit * 1.2f),
        size = Size(size.width * 0.23f, unit * 0.38f),
    )
}

internal fun DrawScope.drawLavaValleyModeIndicatorPolish(
    palette: PixelPalette,
    unit: Float,
) {
    repeat(3) { index ->
        val height = unit * (0.75f + index * 0.48f)
        drawRect(
            color = palette.primary.copy(alpha = 0.66f + index * 0.06f),
            topLeft = Offset(size.width * (0.19f + index * 0.13f), size.height - height),
            size = Size(unit * 0.46f, height),
        )
    }
}

private fun DrawScope.drawLavaVolcano(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(7) { level ->
        val width = unit * (24f - level * 2.5f)
        drawRect(
            color = palette.shadow.copy(alpha = 0.12f + level * 0.012f),
            topLeft = origin + Offset(unit * level * 1.25f, unit * level * 1.55f),
            size = Size(width, unit * 1.7f),
        )
    }
    drawRect(
        color = palette.primary.copy(alpha = 0.16f),
        topLeft = origin + Offset(unit * 8.2f, unit * 1.2f),
        size = Size(unit * 7.6f, unit * 1.2f),
    )
    repeat(4) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.14f + index * 0.015f),
            topLeft = origin + Offset(unit * (10f + index * 0.8f), unit * (2.4f + index * 2.2f)),
            size = Size(unit * (3f - index * 0.35f), unit * 3f),
        )
    }
    drawRect(
        color = palette.highlight.copy(alpha = 0.12f),
        topLeft = origin + Offset(unit * 11f, unit * 3f),
        size = Size(unit * 0.65f, unit * 7f),
    )
}

private fun DrawScope.drawLavaCave(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(5) { level ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.16f + level * 0.015f),
            topLeft = origin + Offset(unit * level, unit * level),
            size = Size(unit * (15f - level * 2f), unit),
        )
    }
    drawRect(
        color = palette.shadow.copy(alpha = 0.22f),
        topLeft = origin + Offset(unit * 4f, unit * 3f),
        size = Size(unit * 7f, unit * 6f),
    )
    repeat(3) { index ->
        drawRect(
            color = palette.secondary.copy(alpha = 0.14f + index * 0.02f),
            topLeft = origin + Offset(unit * (5f + index * 1.8f), unit * (4f - index * 0.45f)),
            size = Size(unit, unit * (2.8f + index * 0.8f)),
        )
    }
}

private fun DrawScope.drawLavaRiver(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
    progress: Float,
) {
    repeat(7) { segment ->
        val xShift = unit * ((segment % 3) - 1f) * 1.2f
        val y = origin.y + segment * unit * 4f
        drawRect(
            color = palette.primary.copy(alpha = 0.15f + segment % 2 * 0.025f),
            topLeft = Offset(origin.x + xShift, y),
            size = Size(unit * (6f + segment * 0.65f), unit * 4.4f),
        )
        val glowOffset = ((progress + segment * 0.13f) % 1f) * unit * 3.2f
        drawRect(
            color = palette.highlight.copy(alpha = 0.10f),
            topLeft = Offset(origin.x + xShift + unit * 1.2f + glowOffset, y + unit * 1.2f),
            size = Size(unit * 1.4f, unit * 0.55f),
        )
    }
}

private fun DrawScope.drawLavaStoneBridge(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(8) { index ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.19f + index % 2 * 0.025f),
            topLeft = origin + Offset(unit * index * 2.2f, -unit * (1.4f - kotlin.math.abs(index - 3.5f) * 0.24f)),
            size = Size(unit * 2f, unit * 1.45f),
        )
    }
    listOf(unit * 2f, unit * 13.4f).forEach { x ->
        drawRect(
            color = palette.outline.copy(alpha = 0.15f),
            topLeft = origin + Offset(x, unit * 0.2f),
            size = Size(unit * 1.1f, unit * 4.2f),
        )
    }
}

private fun DrawScope.drawLavaGeode(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.shadow.copy(alpha = 0.22f),
        topLeft = origin,
        size = Size(unit * 10f, unit * 8f),
        style = Stroke(width = unit * 0.75f),
    )
    repeat(5) { index ->
        val height = unit * (2.5f + index % 3 * 1.3f)
        drawRect(
            color = palette.secondary.copy(alpha = 0.15f + index * 0.012f),
            topLeft = origin + Offset(unit * (1.2f + index * 1.55f), unit * 6.8f - height),
            size = Size(unit * 1.15f, height),
        )
    }
}

private fun DrawScope.drawObsidianPillars(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    listOf(8f, 12f, 6f, 10f).forEachIndexed { index, height ->
        drawRect(
            color = palette.shadow.copy(alpha = 0.20f + index * 0.012f),
            topLeft = origin + Offset(unit * index * 2.4f, unit * (12f - height)),
            size = Size(unit * 1.8f, unit * height),
        )
        drawRect(
            color = palette.secondary.copy(alpha = 0.07f),
            topLeft = origin + Offset(unit * (index * 2.4f + 0.45f), unit * (12.6f - height)),
            size = Size(unit * 0.42f, unit * (height - 1.2f)),
        )
    }
}

private fun DrawScope.drawVolcanicMoss(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    repeat(7) { index ->
        val height = unit * (1.4f + index % 3 * 0.7f)
        drawRect(
            color = palette.outline.copy(alpha = 0.12f + index % 2 * 0.02f),
            topLeft = origin + Offset(unit * index * 1.5f, -height),
            size = Size(unit * 0.55f, height),
        )
        if (index % 2 == 0) {
            drawRect(
                color = palette.primary.copy(alpha = 0.11f),
                topLeft = origin + Offset(unit * index * 1.5f - unit * 0.55f, -height),
                size = Size(unit * 1.5f, unit * 0.55f),
            )
        }
    }
}

private fun DrawScope.drawLavaMiniCrystal(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
    purple: Boolean,
) {
    val color = if (purple) palette.secondary else palette.primary
    drawRect(
        color = color.copy(alpha = 0.84f),
        topLeft = origin + Offset(unit * 0.75f, 0f),
        size = Size(unit * 1.15f, unit * 3.2f),
    )
    drawRect(
        color = color.copy(alpha = 0.68f),
        topLeft = origin,
        size = Size(unit * 0.85f, unit * 2.2f),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.58f),
        topLeft = origin + Offset(unit, unit * 0.55f),
        size = Size(unit * 0.34f, unit * 1.4f),
    )
}

private fun DrawScope.drawLavaValleyMottoFrame(
    palette: PixelPalette,
    unit: Float,
) {
    drawLavaMiniCrystal(Offset(unit * 2.8f, unit * 2.6f), unit * 0.62f, palette, purple = false)
    drawLavaMiniCrystal(Offset(size.width - unit * 5.4f, unit * 2.8f), unit * 0.60f, palette, purple = true)
    repeat(4) { index ->
        val lift = unit * (index % 3 * 0.45f)
        drawRect(
            color = (if (index == 2) palette.secondary else palette.primary).copy(alpha = 0.64f),
            topLeft = Offset(size.width * (0.18f + index * 0.19f), size.height - unit * 1.7f - lift),
            size = Size(size.width * 0.12f, unit * 0.42f),
        )
    }
}
