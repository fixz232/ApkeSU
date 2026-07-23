package me.weishu.kernelsu.ui.component.snow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun DrawScope.drawSeasonCardMotionUnderlay(
    season: SeasonStyle,
    dark: Boolean,
    progress: Float,
) {
    if (!hasSeasonMotionBounds()) return
    val phase = progress.normalizedProgress()
    val palette = seasonMotionPalette(season, dark)
    val sweepWidth = minOf(size.width * 0.22f, 72.dp.toPx())
    val sweepCenter = -sweepWidth + phase * (size.width + sweepWidth * 2f)
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                palette.highlight.copy(alpha = 0.075f),
                Color.Transparent,
            ),
            start = Offset(sweepCenter - sweepWidth, 0f),
            end = Offset(sweepCenter + sweepWidth, size.height),
        ),
    )

    when (season) {
        SeasonStyle.Spring -> drawSpringBreeze(phase, palette)
        SeasonStyle.Summer -> drawSummerWaterLight(phase, palette)
        SeasonStyle.Autumn -> drawAutumnWarmLight(phase, palette)
        SeasonStyle.Winter -> drawWinterIceLight(phase, palette)
    }
}

internal fun DrawScope.drawSeasonCardMotionOverlay(
    season: SeasonStyle,
    dark: Boolean,
    progress: Float,
) {
    if (!hasSeasonMotionBounds()) return
    val phase = progress.normalizedProgress()
    val palette = seasonMotionPalette(season, dark)
    when (season) {
        SeasonStyle.Spring -> drawSpringPetals(phase, palette)
        SeasonStyle.Summer -> drawSummerLife(phase, palette)
        SeasonStyle.Autumn -> drawAutumnLeaves(phase, palette)
        SeasonStyle.Winter -> drawWinterSparkles(phase, palette)
    }
}

internal fun DrawScope.drawSeasonCapMotion(
    season: SeasonStyle,
    dark: Boolean,
    progress: Float,
    capHeight: Float,
) {
    if (size.width < 72.dp.toPx() || capHeight <= 1f) return
    val height = capHeight.coerceAtMost(size.height)
    val phase = progress.normalizedProgress()
    val palette = seasonMotionPalette(season, dark)
    when (season) {
        SeasonStyle.Spring -> drawSpringCapMotion(height, phase, palette)
        SeasonStyle.Summer -> drawSummerCapMotion(height, phase, palette)
        SeasonStyle.Autumn -> drawAutumnCapMotion(height, phase, palette)
        SeasonStyle.Winter -> drawWinterCapMotion(height, phase, palette)
    }
}

private fun DrawScope.drawSpringBreeze(progress: Float, palette: SeasonMotionPalette) {
    repeat(2) { lane ->
        val path = Path()
        val yBase = size.height * (0.73f + lane * 0.10f)
        repeat(13) { index ->
            val fraction = index / 12f
            val y = yBase + sin(fraction * PI.toFloat() * 2.4f + progress * TAU + lane) * 1.2.dp.toPx()
            if (index == 0) path.moveTo(size.width * 0.06f, y) else path.lineTo(size.width * (0.06f + fraction * 0.88f), y)
        }
        drawPath(
            path = path,
            color = palette.primary.copy(alpha = 0.075f - lane * 0.015f),
            style = Stroke(width = 0.65.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawSummerWaterLight(progress: Float, palette: SeasonMotionPalette) {
    repeat(3) { index ->
        val local = (progress * (0.72f + index * 0.08f) + index * 0.27f).normalizedProgress()
        val radius = (10 + index * 5).dp.toPx() * (0.48f + local * 0.72f)
        val center = Offset(
            size.width * (0.18f + index * 0.32f),
            size.height * (0.72f + index % 2 * 0.12f),
        )
        val alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.12f
        drawOval(
            color = palette.primary.copy(alpha = alpha),
            topLeft = Offset(center.x - radius, center.y - radius * 0.18f),
            size = Size(radius * 2f, radius * 0.36f),
            style = Stroke(width = 0.7.dp.toPx()),
        )
    }
}

private fun DrawScope.drawAutumnWarmLight(progress: Float, palette: SeasonMotionPalette) {
    val bandWidth = minOf(size.width * 0.30f, 88.dp.toPx())
    val center = -bandWidth + progress * (size.width + bandWidth * 2f)
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                palette.secondary.copy(alpha = 0.055f),
                palette.highlight.copy(alpha = 0.085f),
                Color.Transparent,
            ),
            start = Offset(center - bandWidth, size.height),
            end = Offset(center + bandWidth, 0f),
        ),
    )
}

private fun DrawScope.drawWinterIceLight(progress: Float, palette: SeasonMotionPalette) {
    val y = size.height * 0.82f
    repeat(2) { index ->
        val width = size.width * (0.22f + index * 0.11f)
        val x = (progress + index * 0.48f).normalizedProgress() * (size.width + width) - width
        drawLine(
            color = palette.highlight.copy(alpha = 0.10f - index * 0.025f),
            start = Offset(x, y - index * 5.dp.toPx()),
            end = Offset(x + width, y - index * 5.dp.toPx()),
            strokeWidth = (1.1f - index * 0.25f).dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSpringPetals(progress: Float, palette: SeasonMotionPalette) {
    val petals = listOf(
        Triple(0.08f, 0.82f, 0f),
        Triple(0.43f, 0.91f, 32f),
        Triple(0.71f, 0.95f, -26f),
    )
    petals.forEachIndexed { index, (offset, lane, rotation) ->
        val local = (progress * (0.43f + index * 0.035f) + offset).normalizedProgress()
        val center = Offset(
            x = size.width * lane + sin(local * TAU + index) * 4.dp.toPx(),
            y = size.height * (0.06f + local * 0.86f),
        )
        val alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.38f
        drawMotionPetal(
            center = center,
            length = (4.8f + index * 0.6f).dp.toPx(),
            width = (2.0f + index * 0.25f).dp.toPx(),
            color = if (index == 1) palette.highlight.copy(alpha = alpha) else palette.secondary.copy(alpha = alpha),
            rotation = rotation + local * 130f,
        )
    }
}

private fun DrawScope.drawSummerLife(progress: Float, palette: SeasonMotionPalette) {
    val wingBeat = sin(progress * TAU * 6f)
    val dragonflyCenter = Offset(
        size.width * 0.86f + sin(progress * TAU) * 4.dp.toPx(),
        size.height * 0.22f + cos(progress * TAU) * 2.dp.toPx(),
    )
    drawMotionDragonfly(dragonflyCenter, wingBeat, palette)

    repeat(2) { index ->
        val local = (progress * 0.66f + index * 0.48f).normalizedProgress()
        val radius = (15 + index * 8).dp.toPx() * (0.40f + local * 0.70f)
        val center = Offset(size.width * (0.16f + index * 0.67f), size.height * 0.91f)
        val alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.22f
        drawOval(
            color = palette.primary.copy(alpha = alpha),
            topLeft = Offset(center.x - radius, center.y - radius * 0.18f),
            size = Size(radius * 2f, radius * 0.36f),
            style = Stroke(width = 0.75.dp.toPx()),
        )
    }
}

private fun DrawScope.drawAutumnLeaves(progress: Float, palette: SeasonMotionPalette) {
    val leaves = listOf(
        Triple(0.04f, 0.82f, -22f),
        Triple(0.38f, 0.90f, 18f),
        Triple(0.72f, 0.96f, -38f),
    )
    leaves.forEachIndexed { index, (offset, lane, rotation) ->
        val local = (progress * (0.36f + index * 0.035f) + offset).normalizedProgress()
        val center = Offset(
            x = size.width * lane + sin(local * TAU * 1.4f + index) * 6.dp.toPx(),
            y = size.height * (0.04f + local * 0.90f),
        )
        val alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.46f
        drawMotionLeaf(
            center = center,
            length = (8.5f + index * 1.4f).dp.toPx(),
            width = (2.7f + index * 0.25f).dp.toPx(),
            color = if (index == 1) palette.secondary.copy(alpha = alpha) else palette.primary.copy(alpha = alpha),
            rotation = rotation + local * 170f,
        )
    }
}

private fun DrawScope.drawWinterSparkles(progress: Float, palette: SeasonMotionPalette) {
    val points = listOf(
        0.08f to 0.16f,
        0.22f to 0.90f,
        0.58f to 0.08f,
        0.82f to 0.88f,
        0.94f to 0.34f,
    )
    points.forEachIndexed { index, (x, y) ->
        val pulse = (sin(progress * TAU * (1.1f + index * 0.08f) + index * 1.37f) + 1f) * 0.5f
        drawMotionSparkle(
            center = Offset(size.width * x, size.height * y),
            radius = (1.5f + pulse * 1.8f).dp.toPx(),
            color = palette.highlight.copy(alpha = 0.16f + pulse * 0.34f),
        )
    }
    repeat(4) { index ->
        val local = (progress * (0.24f + index * 0.02f) + index * 0.23f).normalizedProgress()
        drawCircle(
            color = palette.secondary.copy(alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.28f),
            radius = (0.75f + index % 2 * 0.35f).dp.toPx(),
            center = Offset(
                size.width * (0.84f + index * 0.035f),
                size.height * (0.10f + local * 0.78f),
            ),
        )
    }
}

private fun DrawScope.drawSpringCapMotion(
    height: Float,
    progress: Float,
    palette: SeasonMotionPalette,
) {
    repeat(5) { index ->
        val x = size.width * (0.10f + index * 0.19f)
        val sway = sin(progress * TAU + index * 0.82f) * height * 0.10f
        drawLine(
            color = palette.primary.copy(alpha = 0.48f),
            start = Offset(x, height * 0.62f),
            end = Offset(x + sway, height * (0.18f + index % 2 * 0.08f)),
            strokeWidth = 0.58.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSummerCapMotion(
    height: Float,
    progress: Float,
    palette: SeasonMotionPalette,
) {
    val path = Path()
    repeat(21) { index ->
        val fraction = index / 20f
        val y = height * 0.52f + sin(fraction * PI.toFloat() * 4f + progress * TAU) * height * 0.08f
        if (index == 0) path.moveTo(0f, y) else path.lineTo(size.width * fraction, y)
    }
    drawPath(
        path = path,
        color = palette.highlight.copy(alpha = 0.42f),
        style = Stroke(width = 0.72.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawAutumnCapMotion(
    height: Float,
    progress: Float,
    palette: SeasonMotionPalette,
) {
    val local = (progress * 0.55f).normalizedProgress()
    val center = Offset(
        x = -8.dp.toPx() + local * (size.width + 16.dp.toPx()),
        y = height * (0.28f + 0.16f * sin(local * TAU * 2f)),
    )
    drawMotionLeaf(
        center = center,
        length = (height * 0.46f).coerceAtLeast(4.dp.toPx()),
        width = (height * 0.16f).coerceAtLeast(1.5.dp.toPx()),
        color = palette.highlight.copy(alpha = 0.48f),
        rotation = -24f + local * 120f,
    )
}

private fun DrawScope.drawWinterCapMotion(
    height: Float,
    progress: Float,
    palette: SeasonMotionPalette,
) {
    val segmentWidth = minOf(size.width * 0.18f, 56.dp.toPx())
    val start = -segmentWidth + progress * (size.width + segmentWidth)
    drawLine(
        color = palette.highlight.copy(alpha = 0.64f),
        start = Offset(start, height * 0.20f),
        end = Offset(start + segmentWidth, height * 0.20f),
        strokeWidth = 0.8.dp.toPx(),
        cap = StrokeCap.Round,
    )
    val pulse = (sin(progress * TAU * 2f) + 1f) * 0.5f
    drawMotionSparkle(
        center = Offset(size.width * 0.78f, height * 0.42f),
        radius = (1.4f + pulse * 1.2f).dp.toPx(),
        color = palette.highlight.copy(alpha = 0.32f + pulse * 0.42f),
    )
}

private fun DrawScope.drawMotionPetal(
    center: Offset,
    length: Float,
    width: Float,
    color: Color,
    rotation: Float,
) {
    rotate(rotation, center) {
        drawOval(
            color = color,
            topLeft = center - Offset(length * 0.5f, width * 0.5f),
            size = Size(length, width),
        )
    }
}

private fun DrawScope.drawMotionDragonfly(
    center: Offset,
    wingBeat: Float,
    palette: SeasonMotionPalette,
) {
    val wingWidth = (4.2f + wingBeat * 0.8f).dp.toPx()
    val wingHeight = (1.7f - wingBeat * 0.25f).dp.toPx()
    listOf(-1f, 1f).forEach { direction ->
        rotate(direction * (22f + wingBeat * 10f), center) {
            drawOval(
                color = palette.highlight.copy(alpha = 0.40f),
                topLeft = Offset(center.x + direction * 0.7.dp.toPx() - wingWidth / 2f, center.y - wingHeight),
                size = Size(wingWidth, wingHeight * 2f),
                style = Stroke(width = 0.55.dp.toPx()),
            )
        }
    }
    drawLine(
        color = palette.secondary.copy(alpha = 0.62f),
        start = center - Offset(0f, 3.dp.toPx()),
        end = center + Offset(0f, 3.5.dp.toPx()),
        strokeWidth = 0.8.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(palette.secondary.copy(alpha = 0.72f), 1.dp.toPx(), center - Offset(0f, 3.5.dp.toPx()))
}

private fun DrawScope.drawMotionLeaf(
    center: Offset,
    length: Float,
    width: Float,
    color: Color,
    rotation: Float,
) {
    rotate(rotation, center) {
        val path = Path().apply {
            moveTo(center.x - length * 0.5f, center.y)
            cubicTo(
                center.x - length * 0.18f,
                center.y - width,
                center.x + length * 0.18f,
                center.y - width,
                center.x + length * 0.5f,
                center.y,
            )
            cubicTo(
                center.x + length * 0.18f,
                center.y + width,
                center.x - length * 0.18f,
                center.y + width,
                center.x - length * 0.5f,
                center.y,
            )
            close()
        }
        drawPath(path, color)
        drawLine(
            color = color.copy(alpha = color.alpha * 0.64f),
            start = Offset(center.x - length * 0.34f, center.y),
            end = Offset(center.x + length * 0.34f, center.y),
            strokeWidth = 0.45.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawMotionSparkle(center: Offset, radius: Float, color: Color) {
    drawLine(color, center - Offset(radius, 0f), center + Offset(radius, 0f), 0.55.dp.toPx(), StrokeCap.Round)
    drawLine(color, center - Offset(0f, radius), center + Offset(0f, radius), 0.55.dp.toPx(), StrokeCap.Round)
    drawCircle(color.copy(alpha = color.alpha * 0.76f), radius * 0.22f, center)
}

private fun DrawScope.hasSeasonMotionBounds(): Boolean {
    return size.width >= 72.dp.toPx() && size.height >= 48.dp.toPx()
}

private data class SeasonMotionPalette(
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
)

private fun seasonMotionPalette(season: SeasonStyle, dark: Boolean): SeasonMotionPalette = when (season) {
    SeasonStyle.Spring -> SeasonMotionPalette(
        primary = Color(0xFF79AD64),
        secondary = Color(0xFFF1AFC2),
        highlight = if (dark) Color(0xFFF1D878) else Color(0xFFFFE58A),
    )

    SeasonStyle.Summer -> SeasonMotionPalette(
        primary = Color(0xFF69D0CC),
        secondary = Color(0xFF6FA56B),
        highlight = if (dark) Color(0xFFF0B4C5) else Color(0xFFFFC4D2),
    )

    SeasonStyle.Autumn -> SeasonMotionPalette(
        primary = Color(0xFFD18D45),
        secondary = Color(0xFFB8573E),
        highlight = if (dark) Color(0xFFDDBB78) else Color(0xFFF0CC84),
    )

    SeasonStyle.Winter -> SeasonMotionPalette(
        primary = Color(0xFF8FC9D5),
        secondary = Color(0xFFBCE3EA),
        highlight = Color.White,
    )
}

private fun Float.normalizedProgress(): Float = ((this % 1f) + 1f) % 1f

private const val TAU = (PI * 2.0).toFloat()
