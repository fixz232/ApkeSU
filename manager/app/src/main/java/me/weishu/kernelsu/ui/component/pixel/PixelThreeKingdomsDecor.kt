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
fun PixelThreeKingdomsMotto(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle() || LocalPixelStyle.current != PixelStyle.ThreeKingdoms) return
    val palette = pixelPalette(PixelStyle.ThreeKingdoms, isInDarkTheme())
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.94f), shape)
            .border(1.dp, palette.outline.copy(alpha = 0.82f), shape)
            .drawWithContent {
                drawContent()
                drawMottoEaves(palette, 2.dp.toPx())
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pixel_three_kingdoms_motto),
            modifier = Modifier.padding(horizontal = 34.dp),
            color = palette.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun DrawScope.drawThreeKingdomsScene(palette: PixelPalette) {
    val unit = 5.dp.toPx()
    val horizon = size.height * 0.70f

    drawRect(
        color = palette.primary.copy(alpha = 0.035f),
        topLeft = Offset(0f, size.height * 0.18f),
        size = Size(size.width, unit * 0.65f),
    )
    repeat(3) { range ->
        val base = horizon - unit * (9f + range * 4f)
        repeat(7 - range) { step ->
            val width = size.width * (0.16f + step * 0.035f)
            drawRect(
                color = palette.primary.copy(alpha = 0.035f + range * 0.012f),
                topLeft = Offset(
                    size.width * (0.02f + range * 0.12f) + step * size.width * 0.13f,
                    base + step % 2 * unit,
                ),
                size = Size(width, unit * 1.2f),
            )
        }
    }

    drawAncientCityWall(horizon, unit, palette)
    drawCityGate(Offset(size.width * 0.62f, horizon - unit * 10f), unit, palette)
    drawBattleFlag(Offset(size.width * 0.18f, horizon - unit * 14f), unit, palette, facesLeft = false)
    drawBattleFlag(Offset(size.width * 0.83f, horizon - unit * 17f), unit, palette, facesLeft = true)
    drawPixelWarship(Offset(size.width * 0.18f, size.height * 0.84f), unit, palette)
    drawBeaconTower(Offset(size.width * 0.88f, horizon - unit * 6f), unit, palette)

    val peachPoints = listOf(
        0.07f to 0.25f,
        0.12f to 0.20f,
        0.16f to 0.28f,
        0.22f to 0.22f,
        0.27f to 0.31f,
    )
    peachPoints.forEachIndexed { index, point ->
        val center = Offset(size.width * point.first, size.height * point.second)
        drawRect(
            color = palette.secondary.copy(alpha = 0.10f + index % 2 * 0.025f),
            topLeft = center - Offset(unit * 0.6f, unit * 0.6f),
            size = Size(unit * 1.2f, unit * 1.2f),
        )
        drawRect(
            color = palette.highlight.copy(alpha = 0.09f),
            topLeft = center - Offset(unit * 0.25f, unit * 1.1f),
            size = Size(unit * 0.5f, unit * 2.2f),
        )
    }
}

internal fun DrawScope.drawThreeKingdomsCardMaterial(
    palette: PixelPalette,
    line: Float,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.075f),
        topLeft = Offset.Zero,
        size = Size(size.width, line * 3f),
    )
    repeat(3) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.028f + index * 0.008f),
            topLeft = Offset(size.width * (0.08f + index * 0.07f), size.height * (0.30f + index * 0.20f)),
            size = Size(size.width * (0.48f - index * 0.06f), line * 0.62f),
        )
    }
    drawRect(
        color = palette.secondary.copy(alpha = 0.045f),
        topLeft = Offset(size.width * 0.82f, size.height * 0.18f),
        size = Size(line * 3f, line * 3f),
        style = Stroke(width = line * 0.45f),
    )
}

internal fun DrawScope.drawThreeKingdomsHudAccent(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    val centerX = size.width / 2f
    drawRect(
        color = palette.shadow.copy(alpha = 0.62f),
        topLeft = Offset(centerX - unit * 6f, top + unit * 1.6f),
        size = Size(unit * 12f, unit * 0.7f),
    )
    drawRect(
        color = palette.outline.copy(alpha = 0.78f),
        topLeft = Offset(centerX - unit * 4.5f, top + unit * 0.8f),
        size = Size(unit * 9f, unit * 0.7f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.72f),
        topLeft = Offset(centerX - unit * 2.8f, top),
        size = Size(unit * 5.6f, unit * 0.65f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.82f),
        topLeft = Offset(centerX - unit * 0.5f, top - unit),
        size = Size(unit, unit * 2.5f),
    )
}

internal fun DrawScope.drawThreeKingdomsModeHudPolish(
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    drawBattleFlag(
        origin = Offset(unit * 7f, top + unit * 5f),
        unit = unit * 0.55f,
        palette = palette,
        facesLeft = false,
    )
    drawBattleFlag(
        origin = Offset(size.width - unit * 7f, top + unit * 5f),
        unit = unit * 0.55f,
        palette = palette,
        facesLeft = true,
    )
}

internal fun DrawScope.drawThreeKingdomsNavigationAccent(
    palette: PixelPalette,
    unit: Float,
) {
    val wallY = size.height - unit * 1.6f
    drawRect(
        color = palette.outline.copy(alpha = 0.70f),
        topLeft = Offset(size.width * 0.10f, wallY),
        size = Size(size.width * 0.80f, unit * 0.62f),
    )
    repeat(7) { index ->
        drawRect(
            color = palette.outline.copy(alpha = 0.66f),
            topLeft = Offset(size.width * (0.13f + index * 0.12f), wallY - unit * 0.8f),
            size = Size(unit * 1.4f, unit * 0.8f),
        )
    }
    drawBattleFlag(Offset(unit * 5f, unit * 4.2f), unit * 0.52f, palette, false)
    drawBattleFlag(Offset(size.width - unit * 5f, unit * 4.2f), unit * 0.52f, palette, true)
}

internal fun DrawScope.drawThreeKingdomsModeNavigationFrame(
    palette: PixelPalette,
    unit: Float,
) {
    drawRect(
        color = palette.primary.copy(alpha = 0.62f),
        topLeft = Offset(size.width * 0.36f, 0f),
        size = Size(size.width * 0.28f, unit * 0.52f),
    )
    listOf(0.18f, 0.82f).forEach { x ->
        drawRect(
            color = palette.secondary.copy(alpha = 0.76f),
            topLeft = Offset(size.width * x, unit * 1.4f),
            size = Size(unit, unit),
            style = Stroke(width = unit * 0.34f),
        )
    }
}

internal fun DrawScope.drawThreeKingdomsIndicatorAccent(
    palette: PixelPalette,
    unit: Float,
) {
    val tallyOrigin = Offset(size.width - unit * 5.5f, unit * 1.2f)
    drawRect(
        color = palette.outline.copy(alpha = 0.86f),
        topLeft = tallyOrigin,
        size = Size(unit * 3.5f, unit * 2.2f),
        style = Stroke(width = unit * 0.45f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.84f),
        topLeft = tallyOrigin + Offset(unit * 0.7f, unit * 0.7f),
        size = Size(unit * 2.1f, unit * 0.55f),
    )
}

internal fun DrawScope.drawThreeKingdomsModeIndicatorPolish(
    palette: PixelPalette,
    unit: Float,
) {
    drawRect(
        color = palette.primary.copy(alpha = 0.70f),
        topLeft = Offset(unit * 2f, size.height - unit * 1.3f),
        size = Size(size.width * 0.30f, unit * 0.42f),
    )
    drawRect(
        color = palette.outline.copy(alpha = 0.70f),
        topLeft = Offset(size.width * 0.52f, size.height - unit * 1.3f),
        size = Size(size.width * 0.24f, unit * 0.42f),
    )
}

private fun DrawScope.drawAncientCityWall(
    horizon: Float,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.11f),
        topLeft = Offset(0f, horizon),
        size = Size(size.width, size.height - horizon),
    )
    repeat(12) { index ->
        val blockWidth = size.width / 10f
        val top = horizon - if (index % 2 == 0) unit * 1.3f else 0f
        drawRect(
            color = palette.outline.copy(alpha = 0.13f + index % 3 * 0.012f),
            topLeft = Offset(index * blockWidth - unit, top),
            size = Size(blockWidth + unit, unit * 1.4f),
        )
    }
    repeat(3) { row ->
        val y = horizon + unit * (2f + row * 3f)
        repeat(6) { column ->
            drawRect(
                color = palette.shadow.copy(alpha = 0.11f),
                topLeft = Offset(size.width * (0.04f + column * 0.18f + row % 2 * 0.08f), y),
                size = Size(size.width * 0.13f, unit * 0.55f),
            )
        }
    }
}

private fun DrawScope.drawCityGate(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.shadow.copy(alpha = 0.18f),
        topLeft = origin,
        size = Size(unit * 14f, unit * 10f),
    )
    repeat(3) { level ->
        drawRect(
            color = palette.outline.copy(alpha = 0.15f + level * 0.025f),
            topLeft = origin + Offset(unit * (level - 1f), unit * level),
            size = Size(unit * (16f - level * 2f), unit),
        )
    }
    drawRect(
        color = palette.primary.copy(alpha = 0.12f),
        topLeft = origin + Offset(unit * 5f, unit * 4f),
        size = Size(unit * 4f, unit * 6f),
    )
}

private fun DrawScope.drawBattleFlag(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
    facesLeft: Boolean,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.72f),
        topLeft = Offset(origin.x - unit * 0.25f, origin.y - unit * 7f),
        size = Size(unit * 0.5f, unit * 7f),
    )
    val flagX = if (facesLeft) origin.x - unit * 5f else origin.x
    drawRect(
        color = palette.secondary.copy(alpha = 0.76f),
        topLeft = Offset(flagX, origin.y - unit * 6.5f),
        size = Size(unit * 5f, unit * 2.6f),
    )
    val notchX = if (facesLeft) flagX else flagX + unit * 4f
    drawRect(
        color = palette.surface.copy(alpha = 0.90f),
        topLeft = Offset(notchX, origin.y - unit * 5.1f),
        size = Size(unit, unit * 1.2f),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.62f),
        topLeft = Offset(flagX + unit * 2f, origin.y - unit * 5.8f),
        size = Size(unit, unit),
    )
}

private fun DrawScope.drawPixelWarship(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.shadow.copy(alpha = 0.16f),
        topLeft = origin,
        size = Size(unit * 18f, unit * 2f),
    )
    drawRect(
        color = palette.outline.copy(alpha = 0.15f),
        topLeft = origin + Offset(unit * 2f, -unit),
        size = Size(unit * 13f, unit),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.14f),
        topLeft = origin + Offset(unit * 8f, -unit * 6f),
        size = Size(unit * 0.6f, unit * 5f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.13f),
        topLeft = origin + Offset(unit * 8.6f, -unit * 5.5f),
        size = Size(unit * 6f, unit * 2.8f),
    )
}

private fun DrawScope.drawBeaconTower(
    origin: Offset,
    unit: Float,
    palette: PixelPalette,
) {
    drawRect(
        color = palette.outline.copy(alpha = 0.18f),
        topLeft = origin,
        size = Size(unit * 5f, unit * 6f),
    )
    drawRect(
        color = palette.shadow.copy(alpha = 0.18f),
        topLeft = origin + Offset(unit, unit * 2f),
        size = Size(unit * 3f, unit * 4f),
    )
    listOf(0f, 1.4f, 2.5f).forEachIndexed { index, lift ->
        val side = unit * (2.2f - index * 0.45f)
        drawRect(
            color = palette.secondary.copy(alpha = 0.12f - index * 0.02f),
            topLeft = origin + Offset(unit * (1.4f + index * 0.55f), -unit * (2f + lift)),
            size = Size(side, side),
        )
    }
}

private fun DrawScope.drawMottoEaves(
    palette: PixelPalette,
    unit: Float,
) {
    listOf(false, true).forEach { right ->
        val edge = if (right) size.width - unit * 2f else unit * 2f
        val direction = if (right) -1f else 1f
        drawRect(
            color = palette.outline.copy(alpha = 0.82f),
            topLeft = Offset(
                if (right) edge - unit * 10f else edge,
                unit * 2f,
            ),
            size = Size(unit * 10f, unit * 0.65f),
        )
        repeat(3) { level ->
            val width = unit * (7f - level * 1.4f)
            drawRect(
                color = palette.primary.copy(alpha = 0.74f),
                topLeft = Offset(
                    edge + direction * unit * (1f + level) - if (right) width else 0f,
                    unit * (3f + level * 0.8f),
                ),
                size = Size(width, unit * 0.55f),
            )
        }
        drawRect(
            color = palette.secondary.copy(alpha = 0.86f),
            topLeft = Offset(edge + direction * unit * 4f - unit * 0.5f, size.height - unit * 3f),
            size = Size(unit, unit),
        )
    }
}
