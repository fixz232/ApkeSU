package me.weishu.kernelsu.ui.component.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Visual phase for a furniture interaction, shared by the card and habitat. */
internal data class PixelPetFurnitureActivity(
    val furnitureId: String,
    val action: PixelPetAction,
    val progress: Float,
    val approaching: Boolean,
)

internal fun PixelPetState.activeFurnitureActivity(
    now: Long = System.currentTimeMillis(),
): PixelPetFurnitureActivity? {
    val furnitureId = activeFurnitureId ?: return null
    val action = when {
        queuedAction != null -> PixelPetAction.Walking
        hasRecentAction(now) -> lastAction
        else -> return null
    }
    val progress = if (action.durationMillis <= 0L) {
        1f
    } else {
        ((now - lastActionAt).toFloat() / action.durationMillis.toFloat()).coerceIn(0f, 1f)
    }
    return PixelPetFurnitureActivity(
        furnitureId = furnitureId,
        action = action,
        progress = progress,
        approaching = queuedAction != null || action == PixelPetAction.Walking,
    )
}

internal fun DrawScope.drawPixelPetFurnitureItems(
    furniture: List<PixelPetFurniture>,
    palette: PixelPalette,
    phase: Float,
    activeFurnitureId: String? = null,
    activeAction: PixelPetAction? = null,
    activeProgress: Float = 0f,
) {
    val unit = (size.minDimension / 32f).coerceAtLeast(1.5f)
    furniture.sortedWith(compareBy<PixelPetFurniture> { it.layer }.thenBy { it.y }).forEachIndexed { index, item ->
        val x = size.width * item.x
        val y = size.height * item.y
        val active = item.id == activeFurnitureId
        val bob = if (active) {
            kotlin.math.sin((phase + index * 0.17f) * 2f * kotlin.math.PI).toFloat() * unit * 0.08f
        } else {
            0f
        }
        val pulse = if (active) {
            1f + ((kotlin.math.sin(phase * 2f * kotlin.math.PI).toFloat() + 1f) / 2f) * 0.035f
        } else {
            1f
        }
        if (active) {
            drawRect(
                palette.highlight.copy(alpha = 0.12f),
                Offset(x - unit * 4.4f, y - unit * 5.8f),
                Size(unit * 8.8f, unit * 7.2f),
            )
        }
        rotate(item.rotationQuarterTurns.mod(4) * 90f, pivot = Offset(x, y + bob)) {
            drawPixelPetFurnitureGlyph(item.kind, palette, unit, Offset(x, y + bob), pulse)
        }
        if (active && activeAction != null) {
            drawPixelPetFurnitureInteractionFeedback(
                item = item,
                palette = palette,
                phase = phase,
                action = activeAction,
                progress = activeProgress,
            )
        }
        if (item.durability <= 20) {
            drawRect(
                palette.highlight.copy(alpha = 0.62f),
                Offset(x - unit * 2.3f, y - unit * 4.1f),
                Size(unit * 4.6f * (item.durability / 20f).coerceIn(0f, 1f), unit * 0.22f),
            )
        }
    }
}

internal fun DrawScope.drawPixelPetActiveFurnitureForeground(
    item: PixelPetFurniture,
    palette: PixelPalette,
    phase: Float,
    action: PixelPetAction? = null,
    progress: Float = 0f,
) {
    val unit = (size.minDimension / 32f).coerceAtLeast(1.5f)
    val x = size.width * item.x
    val y = size.height * item.y
    val pulse = (kotlin.math.sin(phase * 2f * kotlin.math.PI).toFloat() + 1f) / 2f
    rotate(item.rotationQuarterTurns.mod(4) * 90f, pivot = Offset(x, y)) {
        when (item.kind) {
        PixelPetFurnitureKind.Bed -> {
            drawRect(
                palette.secondary.copy(alpha = 0.78f),
                Offset(x - unit * 3.45f, y + unit * 0.20f),
                Size(unit * 6.9f, unit * 1.65f),
            )
            drawRect(
                palette.highlight.copy(alpha = 0.34f + pulse * 0.18f),
                Offset(x - unit * 2.7f, y + unit * 0.42f),
                Size(unit * 2.6f, unit * 0.34f),
            )
        }
        PixelPetFurnitureKind.FoodBowl -> {
            drawRect(
                palette.shadow.copy(alpha = 0.82f),
                Offset(x - unit * 3.2f, y + unit * 0.35f),
                Size(unit * 6.4f, unit * 0.58f),
            )
            drawRect(
                palette.highlight.copy(alpha = 0.46f + pulse * 0.20f),
                Offset(x - unit * 1.35f, y + unit * 0.18f),
                Size(unit * 2.7f, unit * 0.24f),
            )
        }
        PixelPetFurnitureKind.Toy -> {
            val travel = (phase * 2f % 1f) * unit * 1.3f
            drawRect(
                palette.highlight.copy(alpha = 0.46f),
                Offset(x - unit * 0.7f + travel, y + unit * 2.1f),
                Size(unit * 0.42f, unit * 0.22f),
            )
        }
        PixelPetFurnitureKind.Aquarium -> {
            drawRect(
                palette.highlight.copy(alpha = 0.25f + pulse * 0.24f),
                Offset(x - unit * 3.2f, y - unit * 2.25f),
                Size(unit * 0.34f, unit * 4.2f),
            )
        }
        PixelPetFurnitureKind.Lamp -> {
            drawRect(
                palette.highlight.copy(alpha = 0.08f + pulse * 0.08f),
                Offset(x - unit * 4.8f, y - unit * 6.2f),
                Size(unit * 9.6f, unit * 7.6f),
            )
        }
        PixelPetFurnitureKind.Plant -> {
            drawRect(
                palette.highlight.copy(alpha = 0.20f + pulse * 0.12f),
                Offset(x - unit * 2.0f, y - unit * 4.9f),
                Size(unit * 0.34f, unit * 0.34f),
            )
        }
        }
    }
    action?.let {
        drawPixelPetFurnitureInteractionFeedback(item, palette, phase, it, progress)
    }
}

/**
 * Every furniture kind owns a visible behaviour chain. The same renderer is
 * used in the compact LKM stage and the immersive habitat so an interaction
 * remains readable even when the card is small.
 */
private fun DrawScope.drawPixelPetFurnitureInteractionFeedback(
    item: PixelPetFurniture,
    palette: PixelPalette,
    phase: Float,
    action: PixelPetAction,
    progress: Float,
) {
    val unit = (size.minDimension / 32f).coerceAtLeast(1.5f)
    val x = size.width * item.x
    val y = size.height * item.y
    val pulse = (kotlin.math.sin(phase * 2f * kotlin.math.PI).toFloat() + 1f) / 2f
    if (action == PixelPetAction.Walking) {
        val travel = progress.coerceIn(0f, 1f)
        drawLine(
            palette.highlight.copy(alpha = 0.28f + pulse * 0.16f),
            Offset(x - unit * (6.0f - travel * 2f), y + unit * 1.8f),
            Offset(x - unit * 1.4f, y + unit * 0.4f),
            unit * 0.18f,
        )
        drawRect(
            palette.highlight.copy(alpha = 0.38f),
            Offset(x - unit * 1.7f, y + unit * 0.15f),
            Size(unit * 0.7f, unit * 0.7f),
        )
        return
    }
    rotate(item.rotationQuarterTurns.mod(4) * 90f, pivot = Offset(x, y)) {
        when (item.kind) {
            PixelPetFurnitureKind.FoodBowl -> {
                val bites = (progress * 4f).toInt().coerceIn(0, 4)
                repeat(4 - bites) { index ->
                    drawRect(
                        palette.highlight.copy(alpha = 0.78f),
                        Offset(x - unit * (1.6f - index * 0.88f), y - unit * 0.70f),
                        Size(unit * 0.44f, unit * 0.28f),
                    )
                }
                repeat(2) { index ->
                    val lift = (phase + index * 0.31f) % 1f
                    drawRect(
                        palette.secondary.copy(alpha = 0.44f * (1f - lift)),
                        Offset(x - unit * 0.6f + index * unit, y - unit * (1.6f + lift * 1.2f)),
                        Size(unit * 0.24f, unit * 0.24f),
                    )
                }
            }
            PixelPetFurnitureKind.Bed -> {
                drawRect(
                    palette.highlight.copy(alpha = 0.24f + pulse * 0.18f),
                    Offset(x - unit * 2.8f, y - unit * 1.6f),
                    Size(unit * (3.2f + progress * 1.5f), unit * 0.52f),
                )
                drawRect(
                    palette.highlight.copy(alpha = 0.62f),
                    Offset(x + unit * (1.0f + pulse), y - unit * (3.2f + pulse * 1.2f)),
                    Size(unit * 0.72f, unit * 0.30f),
                )
            }
            PixelPetFurnitureKind.Toy -> {
                val bounce = kotlin.math.abs(kotlin.math.sin(phase * 4f * kotlin.math.PI).toFloat())
                val ballX = x - unit * 2.2f + progress * unit * 4.4f
                val ballY = y - unit * (1.4f + bounce * 2.2f)
                drawRect(palette.secondary.copy(alpha = 0.96f), Offset(ballX, ballY), Size(unit * 0.82f, unit * 0.82f))
                drawRect(palette.highlight.copy(alpha = 0.92f), Offset(ballX + unit * 0.18f, ballY + unit * 0.18f), Size(unit * 0.22f, unit * 0.22f))
            }
            PixelPetFurnitureKind.Lamp -> {
                drawRect(
                    palette.highlight.copy(alpha = 0.08f + pulse * 0.14f),
                    Offset(x - unit * 5.3f, y - unit * 7.0f),
                    Size(unit * 10.6f, unit * 7.9f),
                )
                drawRect(
                    palette.highlight.copy(alpha = 0.32f + pulse * 0.24f),
                    Offset(x - unit * 0.7f, y - unit * 4.7f),
                    Size(unit * 1.4f, unit * 1.1f),
                )
            }
            PixelPetFurnitureKind.Plant -> {
                repeat(3) { index ->
                    val arc = kotlin.math.sin((phase + index * 0.17f) * 2f * kotlin.math.PI).toFloat()
                    drawLine(
                        palette.highlight.copy(alpha = 0.46f + pulse * 0.22f),
                        Offset(x - unit * 0.3f, y - unit * 2.6f),
                        Offset(x + unit * (index - 1) * 1.1f + arc * unit * 0.36f, y - unit * (4.2f + index * 0.35f)),
                        unit * 0.18f,
                    )
                }
                drawRect(
                    palette.highlight.copy(alpha = 0.58f),
                    Offset(x + unit * 1.6f, y - unit * (4.4f - progress * 1.2f)),
                    Size(unit * 0.28f, unit * 0.54f),
                )
            }
            PixelPetFurnitureKind.Aquarium -> {
                val fishX = x - unit * 2.4f + ((phase * 1.6f) % 1f) * unit * 4.4f
                val fishY = y - unit * (1.1f + pulse * 0.55f)
                drawRect(palette.highlight.copy(alpha = 0.84f), Offset(fishX, fishY), Size(unit * 0.92f, unit * 0.42f))
                drawRect(palette.secondary.copy(alpha = 0.74f), Offset(fishX - unit * 0.30f, fishY + unit * 0.10f), Size(unit * 0.32f, unit * 0.22f))
                repeat(3) { index ->
                    val rise = (phase + index * 0.21f) % 1f
                    drawRect(
                        palette.highlight.copy(alpha = 0.30f + (1f - rise) * 0.26f),
                        Offset(x + unit * (-1.2f + index * 0.95f), y + unit * 1.0f - rise * unit * 3.2f),
                        Size(unit * 0.20f, unit * 0.20f),
                    )
                }
            }
        }
    }
}

/** Transparent hotspots keep furniture interactive in immersive mode. */
@Composable
internal fun PixelPetFurnitureInteractionLayer(
    state: PixelPetState,
    onInteract: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val hitSize = 72.dp
    BoxWithConstraints(modifier = modifier) {
        val hitSizePx = with(density) { hitSize.toPx() }
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        state.furniture.filter { it.durability > 0 }.forEach { item ->
            Box(
                modifier = Modifier
                    .size(hitSize)
                    .graphicsLayer {
                        translationX = (item.x * widthPx - hitSizePx / 2f)
                            .coerceIn(0f, (widthPx - hitSizePx).coerceAtLeast(0f))
                        translationY = (item.y * heightPx - hitSizePx / 2f)
                            .coerceIn(0f, (heightPx - hitSizePx).coerceAtLeast(0f))
                    }
                    .clickable { onInteract(item.id) },
            )
        }
    }
}

@Composable
fun PixelPetFurnitureEditor(
    state: PixelPetState,
    onMove: (String, Float, Float) -> Unit,
    onInteract: (String) -> Unit,
    onSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val itemSize = 64.dp
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val itemSizePx = with(density) { itemSize.toPx() }
        val minOffsetX = -itemSizePx / 2f
        val maxOffsetX = widthPx - itemSizePx / 2f
        val minOffsetY = -itemSizePx / 2f
        val maxOffsetY = heightPx - itemSizePx / 2f
        state.furniture.forEach { item ->
            var offsetX by remember(item.id, item.x, widthPx, itemSizePx) {
                mutableFloatStateOf(pixelPetFurnitureEditorOffset(item.x, widthPx, itemSizePx))
            }
            var offsetY by remember(item.id, item.y, heightPx, itemSizePx) {
                mutableFloatStateOf(pixelPetFurnitureEditorOffset(item.y, heightPx, itemSizePx))
            }
            Box(
                modifier = Modifier
                    .size(itemSize)
            .graphicsLayer {
                translationX = offsetX
                translationY = offsetY
                rotationZ = item.rotationQuarterTurns.mod(4) * 90f
                    }
                    .shadow(3.dp, RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), RoundedCornerShape(10.dp))
                    .clickable {
                        onSelected(item.id)
                        onInteract(item.id)
                    }
                    .pointerInput(item.id, widthPx, heightPx, itemSizePx) {
                        detectDragGestures(
                            onDragEnd = {
                                val x = snapFurniturePosition(
                                    pixelPetFurnitureCenterFromEditorOffset(offsetX, widthPx, itemSizePx),
                                )
                                val y = snapFurniturePosition(
                                    pixelPetFurnitureCenterFromEditorOffset(offsetY, heightPx, itemSizePx),
                                )
                                offsetX = pixelPetFurnitureEditorOffset(x, widthPx, itemSizePx)
                                offsetY = pixelPetFurnitureEditorOffset(y, heightPx, itemSizePx)
                                onMove(item.id, x, y)
                            },
                            onDragCancel = {
                                offsetX = pixelPetFurnitureEditorOffset(item.x, widthPx, itemSizePx)
                                offsetY = pixelPetFurnitureEditorOffset(item.y, heightPx, itemSizePx)
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(minOffsetX, maxOffsetX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(minOffsetY, maxOffsetY)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                PixelPetFurnitureIcon(kind = item.kind, modifier = Modifier.size(56.dp))
            }
        }
    }
}

internal fun pixelPetFurnitureEditorOffset(
    center: Float,
    viewport: Float,
    itemSize: Float,
): Float = center.coerceIn(0f, 1f) * viewport.coerceAtLeast(0f) - itemSize.coerceAtLeast(0f) / 2f

internal fun pixelPetFurnitureCenterFromEditorOffset(
    offset: Float,
    viewport: Float,
    itemSize: Float,
): Float = ((offset + itemSize.coerceAtLeast(0f) / 2f) / viewport.coerceAtLeast(1f)).coerceIn(0f, 1f)

internal fun snapFurniturePosition(value: Float): Float =
    ((value.coerceIn(0f, 1f) * 20f).roundToInt() / 20f).coerceIn(0f, 1f)

@Composable
private fun PixelPetFurnitureIcon(kind: PixelPetFurnitureKind, modifier: Modifier = Modifier) {
    val palette = pixelPalette(PixelStyle.PetCompanion, androidx.compose.foundation.isSystemInDarkTheme())
    Canvas(modifier = modifier) {
        drawPixelPetFurnitureGlyph(kind, palette, (size.minDimension / 16f).coerceAtLeast(1.5f), Offset(size.width / 2f, size.height * 0.66f), 1f)
    }
}

private fun DrawScope.drawPixelPetFurnitureGlyph(
    kind: PixelPetFurnitureKind,
    palette: PixelPalette,
    unit: Float,
    center: Offset,
    scale: Float,
) {
    val outline = palette.shadow.copy(alpha = 0.92f)
    val base = palette.secondary.copy(alpha = 0.92f)
    val highlight = palette.highlight.copy(alpha = 0.90f)
    val x = center.x
    val y = center.y
    fun rect(color: Color, dx: Float, dy: Float, width: Float, height: Float) {
        drawRect(
            color,
            Offset(x + dx * unit * scale, y + dy * unit * scale),
            Size(width * unit * scale, height * unit * scale),
        )
    }
    when (kind) {
        PixelPetFurnitureKind.FoodBowl -> {
            rect(outline, -3.2f, -1.0f, 6.4f, 2.3f)
            rect(base, -2.6f, -0.6f, 5.2f, 1.45f)
            rect(highlight, -1.5f, -0.25f, 2.8f, 0.35f)
        }
        PixelPetFurnitureKind.Bed -> {
            rect(outline, -4.2f, -2.4f, 8.4f, 4.6f)
            rect(base, -3.5f, -1.8f, 7.0f, 3.0f)
            rect(highlight, -2.9f, -1.55f, 2.1f, 1.25f)
            rect(palette.primary.copy(alpha = 0.76f), -3.4f, 0.95f, 6.8f, 0.5f)
        }
        PixelPetFurnitureKind.Toy -> {
            rect(outline, -1.3f, -3.0f, 2.6f, 6.0f)
            rect(base, -0.8f, -2.5f, 1.6f, 5.0f)
            rect(highlight, -2.9f, -0.8f, 1.8f, 1.4f)
            rect(highlight, 1.1f, -0.8f, 1.8f, 1.4f)
        }
        PixelPetFurnitureKind.Lamp -> {
            rect(outline, -1.2f, -5.0f, 2.4f, 8.0f)
            rect(highlight, -3.0f, -4.4f, 6.0f, 2.6f)
            rect(base, -2.4f, -3.9f, 4.8f, 1.5f)
            rect(outline, -3.7f, 2.6f, 7.4f, 0.8f)
        }
        PixelPetFurnitureKind.Plant -> {
            rect(outline, -2.4f, 0.1f, 4.8f, 2.7f)
            rect(base, -1.8f, 0.4f, 3.6f, 1.9f)
            rect(outline, -0.5f, -5.3f, 1.0f, 5.8f)
            rect(highlight, -3.8f, -4.7f, 3.4f, 1.4f)
            rect(palette.primary.copy(alpha = 0.85f), 0.6f, -3.6f, 3.5f, 1.5f)
        }
        PixelPetFurnitureKind.Aquarium -> {
            rect(outline, -4.0f, -3.2f, 8.0f, 6.2f)
            rect(palette.primary.copy(alpha = 0.62f), -3.3f, -2.5f, 6.6f, 4.7f)
            rect(highlight, -2.0f, -1.0f, 1.6f, 0.7f)
            rect(base, 0.8f, 0.2f, 1.5f, 0.7f)
        }
    }
}
