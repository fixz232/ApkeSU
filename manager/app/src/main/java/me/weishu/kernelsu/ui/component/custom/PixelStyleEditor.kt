package me.weishu.kernelsu.ui.component.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.InvertColorsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun PixelEditorSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
fun PixelGridEditor(
    grid: PixelGrid,
    selectedColor: Long,
    contentDescription: String,
    onStrokeStart: (PixelGrid) -> Unit,
    onGridChange: (PixelGrid) -> Unit,
    modifier: Modifier = Modifier,
    isCellEditable: (x: Int, y: Int, width: Int, height: Int) -> Boolean = { _, _, _, _ -> true },
) {
    val latestGrid by rememberUpdatedState(grid)
    val latestColor by rememberUpdatedState(selectedColor)
    val latestOnStrokeStart by rememberUpdatedState(onStrokeStart)
    val latestOnGridChange by rememberUpdatedState(onGridChange)
    val latestIsCellEditable by rememberUpdatedState(isCellEditable)
    val checkerLight = MaterialTheme.colorScheme.surfaceContainerHighest
    val checkerDark = MaterialTheme.colorScheme.surfaceVariant
    val gridLine = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
    val outerBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)
    val shape = RoundedCornerShape(8.dp)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp, max = 340.dp)
            .border(1.dp, outerBorder, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(grid.width, grid.height) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var working = latestGrid
                    var strokeStarted = false
                    var previousCell: Pair<Int, Int>? = null

                    fun cellAt(position: Offset): Pair<Int, Int>? {
                        if (size.width <= 0 || size.height <= 0) return null
                        val cellSize = min(
                            size.width.toFloat() / working.width,
                            size.height.toFloat() / working.height,
                        )
                        if (cellSize <= 0f) return null
                        val contentWidth = cellSize * working.width
                        val contentHeight = cellSize * working.height
                        val originX = (size.width - contentWidth) / 2f
                        val originY = (size.height - contentHeight) / 2f
                        val x = floor((position.x - originX) / cellSize).toInt()
                        val y = floor((position.y - originY) / cellSize).toInt()
                        return (x to y).takeIf {
                            x in 0 until working.width && y in 0 until working.height
                        }
                    }

                    fun paintCell(x: Int, y: Int) {
                        if (!latestIsCellEditable(x, y, working.width, working.height)) return
                        val next = working.withPixel(x, y, latestColor)
                        if (next == working) return
                        if (!strokeStarted) {
                            latestOnStrokeStart(working)
                            strokeStarted = true
                        }
                        working = next
                        latestOnGridChange(next)
                    }

                    fun paint(position: Offset) {
                        val currentCell = cellAt(position)
                        if (currentCell == null) {
                            previousCell = null
                            return
                        }
                        val start = previousCell ?: currentCell
                        rasterizePixelLine(start.first, start.second, currentCell.first, currentCell.second) { x, y ->
                            paintCell(x, y)
                        }
                        previousCell = currentCell
                    }

                    paint(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        paint(change.position)
                        change.consume()
                    }
                }
            },
    ) {
        val cellSize = min(size.width / grid.width, size.height / grid.height)
        val contentWidth = cellSize * grid.width
        val contentHeight = cellSize * grid.height
        val origin = Offset(
            x = (size.width - contentWidth) / 2f,
            y = (size.height - contentHeight) / 2f,
        )
        for (y in 0 until grid.height) {
            for (x in 0 until grid.width) {
                val topLeft = Offset(origin.x + x * cellSize, origin.y + y * cellSize)
                drawRect(
                    color = if ((x + y) % 2 == 0) checkerLight else checkerDark,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                )
                val argb = grid.colorAt(x, y)
                if (argb != TRANSPARENT_PIXEL) {
                    drawRect(
                        color = Color(argb.toInt()),
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(cellSize + 0.5f, cellSize + 0.5f),
                    )
                }
                if (!isCellEditable(x, y, grid.width, grid.height)) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.28f),
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                    )
                }
            }
        }
        if (cellSize >= 7f) {
            for (x in 0..grid.width) {
                val lineX = origin.x + x * cellSize
                drawLine(gridLine, Offset(lineX, origin.y), Offset(lineX, origin.y + contentHeight), 0.7f)
            }
            for (y in 0..grid.height) {
                val lineY = origin.y + y * cellSize
                drawLine(gridLine, Offset(origin.x, lineY), Offset(origin.x + contentWidth, lineY), 0.7f)
            }
        }
    }
}

private inline fun rasterizePixelLine(
    startX: Int,
    startY: Int,
    endX: Int,
    endY: Int,
    visit: (x: Int, y: Int) -> Unit,
) {
    var x = startX
    var y = startY
    val deltaX = abs(endX - startX)
    val stepX = if (startX < endX) 1 else -1
    val deltaY = -abs(endY - startY)
    val stepY = if (startY < endY) 1 else -1
    var error = deltaX + deltaY
    while (true) {
        visit(x, y)
        if (x == endX && y == endY) return
        val doubledError = error * 2
        if (doubledError >= deltaY) {
            error += deltaY
            x += stepX
        }
        if (doubledError <= deltaX) {
            error += deltaX
            y += stepY
        }
    }
}

@Composable
fun PixelEditToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSelectEraser: () -> Unit,
    onFill: () -> Unit,
    onMirror: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.component_creator_undo))
        }
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(Icons.AutoMirrored.Rounded.Redo, stringResource(R.string.component_creator_redo))
        }
        IconButton(onClick = onSelectEraser) {
            Icon(Icons.Rounded.InvertColorsOff, stringResource(R.string.component_creator_eraser))
        }
        IconButton(onClick = onFill) {
            Icon(Icons.Rounded.FormatColorFill, stringResource(R.string.component_creator_fill))
        }
        IconButton(onClick = onMirror) {
            Icon(Icons.Rounded.Flip, stringResource(R.string.component_creator_mirror))
        }
        IconButton(onClick = onClear) {
            Icon(Icons.Rounded.DeleteSweep, stringResource(R.string.component_creator_clear_layer))
        }
    }
}

@Composable
fun PixelPaletteEditor(
    palette: List<Long>,
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    onPaletteChange: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hexValue by remember(selectedColor) { mutableStateOf(formatArgbHex(selectedColor)) }
    val parsedColor = parseArgbHex(hexValue)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            palette.forEach { color ->
                PixelColorSwatch(
                    argb = color,
                    selected = selectedColor == color,
                    onClick = { onColorSelected(color) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = hexValue,
                onValueChange = { value -> hexValue = value.take(9).uppercase() },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(R.string.component_creator_argb)) },
                isError = parsedColor == null,
                keyboardOptions = KeyboardOptions.Default,
            )
            IconButton(
                onClick = {
                    parsedColor?.let { color ->
                        onColorSelected(color)
                        if (color !in palette && palette.size < MAX_EDITOR_PALETTE_COLORS) {
                            onPaletteChange(palette + color)
                        }
                    }
                },
                enabled = parsedColor != null,
            ) {
                Icon(Icons.Rounded.Add, stringResource(R.string.component_creator_add_color))
            }
        }
    }
}

@Composable
private fun PixelColorSwatch(
    argb: Long,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val selectionColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                color = if (argb == TRANSPARENT_PIXEL) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color(argb.toInt())
                },
                shape = shape,
            )
            .border(if (selected) 3.dp else 1.dp, if (selected) selectionColor else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (argb == TRANSPARENT_PIXEL) {
            Icon(
                imageVector = Icons.Rounded.InvertColorsOff,
                contentDescription = stringResource(R.string.component_creator_transparent),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun PixelMotionEditor(
    rule: PixelMotionRule,
    onRuleChange: (PixelMotionRule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalized = rule.normalized()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.component_creator_motion),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.component_creator_motion_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = normalized.enabled,
                onCheckedChange = { enabled ->
                    onRuleChange(
                        normalized.copy(
                            enabled = enabled,
                            mode = if (enabled && normalized.mode == PixelMotionMode.Static) {
                                PixelMotionMode.Pulse
                            } else {
                                normalized.mode
                            },
                        ).normalized()
                    )
                },
            )
        }
        if (normalized.enabled) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PixelMotionMode.entries.filter { it != PixelMotionMode.Static }.forEach { mode ->
                    FilterChip(
                        selected = normalized.mode == mode,
                        onClick = { onRuleChange(normalized.copy(mode = mode).normalized()) },
                        label = { Text(stringResource(mode.labelRes())) },
                    )
                }
            }
            PixelSliderRow(
                title = stringResource(R.string.component_creator_motion_duration),
                value = normalized.durationMillis.toFloat(),
                valueRange = MIN_PIXEL_MOTION_DURATION_MS.toFloat()..MAX_PIXEL_MOTION_DURATION_MS.toFloat(),
                steps = 18,
                valueText = stringResource(
                    R.string.component_creator_motion_duration_value,
                    normalized.durationMillis / 1000f,
                ),
                onValueChange = { value ->
                    onRuleChange(normalized.copy(durationMillis = value.roundToInt()).normalized())
                },
            )
            if (normalized.mode == PixelMotionMode.Drift) {
                PixelSliderRow(
                    title = stringResource(R.string.component_creator_motion_amplitude),
                    value = normalized.amplitudeCells.toFloat(),
                    valueRange = 0f..MAX_PIXEL_MOTION_AMPLITUDE.toFloat(),
                    steps = MAX_PIXEL_MOTION_AMPLITUDE - 1,
                    valueText = stringResource(
                        R.string.component_creator_motion_amplitude_value,
                        normalized.amplitudeCells,
                    ),
                    onValueChange = { value ->
                        onRuleChange(normalized.copy(amplitudeCells = value.roundToInt()).normalized())
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PixelMotionRepeat.entries.forEach { repeat ->
                    FilterChip(
                        selected = normalized.repeat == repeat,
                        onClick = { onRuleChange(normalized.copy(repeat = repeat).normalized()) },
                        label = {
                            Text(
                                text = stringResource(repeat.labelRes()),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(text = valueText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

fun PixelGrid.filled(argb: Long): PixelGrid = copy(pixels = List(width * height) { argb })

fun PixelGrid.filledWhere(
    argb: Long,
    predicate: (x: Int, y: Int, width: Int, height: Int) -> Boolean,
): PixelGrid = copy(
    pixels = List(width * height) { index ->
        val x = index % width
        val y = index / width
        if (predicate(x, y, width, height)) argb else colorAt(x, y)
    },
)

fun PixelGrid.mirroredHorizontally(): PixelGrid = PixelGrid(
    width = width,
    height = height,
    pixels = List(width * height) { index ->
        val x = index % width
        val y = index / width
        colorAt(width - 1 - x, y)
    },
)

private fun PixelMotionMode.labelRes(): Int = when (this) {
    PixelMotionMode.Static -> R.string.component_creator_motion_static
    PixelMotionMode.Pulse -> R.string.component_creator_motion_pulse
    PixelMotionMode.Drift -> R.string.component_creator_motion_drift
    PixelMotionMode.Scan -> R.string.component_creator_motion_scan
}

private fun PixelMotionRepeat.labelRes(): Int = when (this) {
    PixelMotionRepeat.Restart -> R.string.component_creator_motion_restart
    PixelMotionRepeat.Reverse -> R.string.component_creator_motion_reverse
}

private const val MAX_EDITOR_PALETTE_COLORS = 24
