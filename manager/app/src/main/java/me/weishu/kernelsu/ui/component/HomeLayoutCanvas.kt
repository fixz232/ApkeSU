package me.weishu.kernelsu.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.HomeLayoutCard
import me.weishu.kernelsu.ui.util.HomeLayoutItem
import me.weishu.kernelsu.ui.util.HomeLayoutResizeEdge
import me.weishu.kernelsu.ui.util.HomeLayoutState
import me.weishu.kernelsu.ui.util.itemsForOrientation
import me.weishu.kernelsu.ui.util.resolveHomeLayoutCollisions
import kotlin.math.abs
import kotlin.math.roundToInt

data class HomeLayoutResizeGesture(
    val edge: HomeLayoutResizeEdge,
    val delta: Offset,
    val renderedHeightRows: Float,
)

data class HomeLayoutEditor(
    val selectedCard: HomeLayoutCard,
    val onSelectedCardChange: (HomeLayoutCard) -> Unit,
    val onDragCard: (HomeLayoutCard, Offset) -> Unit,
    val onResizeCard: (HomeLayoutCard, HomeLayoutResizeGesture) -> Unit,
    val onTransformStart: (HomeLayoutCard) -> Unit = {},
    val onTransformEnd: (HomeLayoutCard) -> Unit = {},
    val onOverlapChange: (Set<HomeLayoutCard>) -> Unit = {},
)

@Composable
fun HomeLayoutCanvas(
    state: HomeLayoutState,
    modifier: Modifier = Modifier,
    editor: HomeLayoutEditor? = null,
    selectedCard: HomeLayoutCard? = null,
    onCardSelected: ((HomeLayoutCard) -> Unit)? = null,
    isLandscapeOverride: Boolean? = null,
    rowHeight: Dp = 150.dp,
    cardContent: @Composable (HomeLayoutItem) -> Unit,
) {
    val currentEditor by rememberUpdatedState(editor)
    val configuration = LocalConfiguration.current
    val isLandscape = isLandscapeOverride
        ?: (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    val sourceItems = state.itemsForOrientation(isLandscape)
    val layoutItems = if (state.autoAvoidOverlap && editor == null) {
        resolveHomeLayoutCollisions(sourceItems)
    } else {
        sourceItems
    }
    val visibleItems = layoutItems.filter { it.visible }.sortedBy { it.zIndex }
    val visibleCards = visibleItems.map { it.card }.toSet()
    val cardBounds = remember { mutableStateMapOf<HomeLayoutCard, Rect>() }
    val overlappingCards by remember {
        derivedStateOf { findOverlappingCards(cardBounds) }
    }

    LaunchedEffect(visibleCards) {
        cardBounds.keys.filterNot(visibleCards::contains).forEach(cardBounds::remove)
    }
    LaunchedEffect(overlappingCards) {
        currentEditor?.onOverlapChange?.invoke(overlappingCards)
    }

    BoxWithConstraints(modifier = modifier) {
        val canvasWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
        val guideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
        val selectedItem = visibleItems.firstOrNull {
            it.card == (editor?.selectedCard ?: selectedCard)
        }
        Layout(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (editor == null || selectedItem == null || !state.autoSnap) return@drawBehind
                    val horizontalAnchor = listOf(0f, 0.5f, 1f)
                        .minByOrNull { abs(selectedItem.x - it) }
                        ?.takeIf { abs(selectedItem.x - it) <= 0.045f }
                    if (horizontalAnchor != null) {
                        val cardLeft = (size.width - size.width * selectedItem.width) * horizontalAnchor
                        val guideX = when (horizontalAnchor) {
                            0f -> cardLeft
                            0.5f -> size.width / 2f
                            else -> cardLeft + size.width * selectedItem.width
                        }
                        drawLine(
                            color = guideColor,
                            start = Offset(guideX, 0f),
                            end = Offset(guideX, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    val guideY = selectedItem.y * rowHeightPx
                    drawLine(
                        color = guideColor.copy(alpha = 0.44f),
                        start = Offset(0f, guideY),
                        end = Offset(size.width, guideY),
                        strokeWidth = 1.dp.toPx(),
                    )
                },
            content = {
                visibleItems.forEach { item ->
                    key(item.card) {
                        val selected = editor?.selectedCard == item.card || selectedCard == item.card
                        Box(
                            modifier = Modifier
                                .then(
                                    if (editor != null) {
                                        Modifier.onGloballyPositioned { coordinates ->
                                            val position = coordinates.positionInParent()
                                            val next = Rect(
                                                left = position.x,
                                                top = position.y,
                                                right = position.x + coordinates.size.width,
                                                bottom = position.y + coordinates.size.height,
                                            )
                                            if (cardBounds[item.card] != next) {
                                                cardBounds[item.card] = next
                                            }
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .then(
                                    if (editor != null) {
                                        Modifier
                                            .clickable {
                                                currentEditor?.onSelectedCardChange?.invoke(item.card)
                                            }
                                            .pointerInput(item.card, item.width, canvasWidthPx, rowHeightPx) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        currentEditor?.onSelectedCardChange?.invoke(item.card)
                                                        currentEditor?.onTransformStart?.invoke(item.card)
                                                    },
                                                    onDragEnd = {
                                                        currentEditor?.onTransformEnd?.invoke(item.card)
                                                    },
                                                    onDragCancel = {
                                                        currentEditor?.onTransformEnd?.invoke(item.card)
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        currentEditor?.onDragCard?.invoke(
                                                            item.card,
                                                            Offset(
                                                                x = dragAmount.x / canvasWidthPx,
                                                                y = dragAmount.y / rowHeightPx.coerceAtLeast(1f),
                                                            ),
                                                        )
                                                    },
                                                )
                                            }
                                    } else {
                                        Modifier
                                    },
                                )
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(18.dp),
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                            propagateMinConstraints = true,
                        ) {
                            cardContent(item)
                            if (onCardSelected != null) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { onCardSelected(item.card) },
                                )
                            }
                            if (selected && editor != null) {
                                HomeLayoutResizeEdge.entries.forEach { edge ->
                                    ResizeEdgeHandle(
                                        edge = edge,
                                        modifier = Modifier.align(edge.alignment()),
                                        onResizeStart = {
                                            currentEditor?.onTransformStart?.invoke(item.card)
                                        },
                                        onResize = { deltaPx ->
                                            val renderedHeightRows = (
                                                cardBounds[item.card]?.height ?: rowHeightPx
                                            ) / rowHeightPx.coerceAtLeast(1f)
                                            currentEditor?.onResizeCard?.invoke(
                                                item.card,
                                                HomeLayoutResizeGesture(
                                                    edge = edge,
                                                    delta = Offset(
                                                        x = deltaPx.x / canvasWidthPx,
                                                        y = deltaPx.y / rowHeightPx.coerceAtLeast(1f),
                                                    ),
                                                    renderedHeightRows = renderedHeightRows,
                                                ),
                                            )
                                        },
                                        onResizeEnd = {
                                            currentEditor?.onTransformEnd?.invoke(item.card)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val canvasWidth = constraints.maxWidth.takeIf { it != Constraints.Infinity }
                ?: constraints.minWidth
            val bottomPaddingPx = 24.dp.roundToPx()
            val measured = visibleItems.zip(measurables).map { (item, measurable) ->
                val widthPx = (canvasWidth * item.width)
                    .roundToInt()
                    .coerceIn(1, canvasWidth.coerceAtLeast(1))
                val cardConstraints = if (item.height > 0f) {
                    val heightPx = (rowHeightPx * item.height).roundToInt().coerceAtLeast(1)
                    Constraints.fixed(widthPx, heightPx)
                } else {
                    Constraints.fixedWidth(widthPx)
                }
                MeasuredHomeCard(item = item, placeable = measurable.measure(cardConstraints))
            }
            val placements = mutableMapOf<HomeLayoutCard, PixelPlacement>()
            val occupied = mutableListOf<PixelRect>()

            measured.sortedWith(
                compareBy<MeasuredHomeCard> { it.item.y }.thenBy { it.item.zIndex },
            ).forEach { card ->
                val maxX = (canvasWidth - card.placeable.width).coerceAtLeast(0)
                val x = (maxX * card.item.x).roundToInt().coerceIn(0, maxX)
                val y = (rowHeightPx * card.item.y).roundToInt().coerceAtLeast(0)
                val rect = PixelRect(
                    left = x,
                    top = y,
                    right = x + card.placeable.width,
                    bottom = y + card.placeable.height,
                )
                occupied += rect
                placements[card.item.card] = PixelPlacement(x = x, y = y)
            }

            val desiredHeight = (occupied.maxOfOrNull { it.bottom } ?: 0) + bottomPaddingPx
            val canvasHeight = if (constraints.hasBoundedHeight) {
                desiredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
            } else {
                desiredHeight.coerceAtLeast(constraints.minHeight)
            }.coerceAtLeast(1)
            layout(canvasWidth, canvasHeight) {
                measured.forEach { card ->
                    val placement = placements.getValue(card.item.card)
                    card.placeable.placeRelative(
                        x = placement.x,
                        y = placement.y,
                        zIndex = if (
                            editor?.selectedCard == card.item.card || selectedCard == card.item.card
                        ) {
                            HomeLayoutCard.entries.size.toFloat()
                        } else {
                            card.item.zIndex.toFloat()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResizeEdgeHandle(
    edge: HomeLayoutResizeEdge,
    modifier: Modifier,
    onResizeStart: () -> Unit,
    onResize: (Offset) -> Unit,
    onResizeEnd: () -> Unit,
) {
    val currentOnResizeStart by rememberUpdatedState(onResizeStart)
    val currentOnResize by rememberUpdatedState(onResize)
    val currentOnResizeEnd by rememberUpdatedState(onResizeEnd)
    val horizontal = edge == HomeLayoutResizeEdge.Top || edge == HomeLayoutResizeEdge.Bottom
    Box(
        modifier = modifier
            .size(40.dp)
            .pointerInput(edge) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    down.consume()
                    currentOnResizeStart()
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val dragAmount = change.position - change.previousPosition
                            change.consume()
                            if (dragAmount != Offset.Zero) {
                                currentOnResize(
                                    if (horizontal) {
                                        Offset(0f, dragAmount.y)
                                    } else {
                                        Offset(dragAmount.x, 0f)
                                    },
                                )
                            }
                        }
                    } finally {
                        currentOnResizeEnd()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(6.dp)
        Box(
            modifier = Modifier
                .then(
                    if (horizontal) {
                        Modifier.width(24.dp).height(12.dp)
                    } else {
                        Modifier.width(12.dp).height(24.dp)
                    },
                )
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary)
                .border(1.dp, Color.White.copy(alpha = 0.72f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (horizontal) Icons.Rounded.DragHandle else Icons.Rounded.DragIndicator,
                contentDescription = stringResource(edge.descriptionRes()),
                tint = Color.White,
                modifier = if (horizontal) {
                    Modifier.width(16.dp).height(10.dp)
                } else {
                    Modifier.width(10.dp).height(16.dp)
                },
            )
        }
    }
}

private fun HomeLayoutResizeEdge.alignment(): Alignment = when (this) {
    HomeLayoutResizeEdge.Left -> Alignment.CenterStart
    HomeLayoutResizeEdge.Top -> Alignment.TopCenter
    HomeLayoutResizeEdge.Right -> Alignment.CenterEnd
    HomeLayoutResizeEdge.Bottom -> Alignment.BottomCenter
}

private fun HomeLayoutResizeEdge.descriptionRes(): Int = when (this) {
    HomeLayoutResizeEdge.Left -> R.string.home_layout_resize_left
    HomeLayoutResizeEdge.Top -> R.string.home_layout_resize_top
    HomeLayoutResizeEdge.Right -> R.string.home_layout_resize_right
    HomeLayoutResizeEdge.Bottom -> R.string.home_layout_resize_bottom
}

private data class MeasuredHomeCard(
    val item: HomeLayoutItem,
    val placeable: androidx.compose.ui.layout.Placeable,
)

private data class PixelPlacement(val x: Int, val y: Int)

private data class PixelRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

private fun findOverlappingCards(bounds: Map<HomeLayoutCard, Rect>): Set<HomeLayoutCard> {
    val entries = bounds.entries.toList()
    return buildSet {
        for (leftIndex in entries.indices) {
            for (rightIndex in leftIndex + 1 until entries.size) {
                val left = entries[leftIndex]
                val right = entries[rightIndex]
                if (left.value.overlaps(right.value)) {
                    add(left.key)
                    add(right.key)
                }
            }
        }
    }
}
