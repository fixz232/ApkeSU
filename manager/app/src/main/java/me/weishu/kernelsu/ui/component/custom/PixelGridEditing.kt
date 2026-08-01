package me.weishu.kernelsu.ui.component.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

enum class PixelCanvasTool {
    Pencil,
    Eraser,
    Eyedropper,
    FloodFill,
    Select,
}

enum class PixelSymmetry {
    None,
    Horizontal,
    Vertical,
    Both,
}

data class PixelSelection(
    val left: Int,
    val top: Int,
    val rightExclusive: Int,
    val bottomExclusive: Int,
) {
    val width: Int get() = rightExclusive - left
    val height: Int get() = bottomExclusive - top

    fun normalized(gridWidth: Int, gridHeight: Int): PixelSelection {
        val safeLeft = left.coerceIn(0, gridWidth - 1)
        val safeTop = top.coerceIn(0, gridHeight - 1)
        val safeRight = rightExclusive.coerceIn(safeLeft + 1, gridWidth)
        val safeBottom = bottomExclusive.coerceIn(safeTop + 1, gridHeight)
        return PixelSelection(safeLeft, safeTop, safeRight, safeBottom)
    }

    companion object {
        fun between(firstX: Int, firstY: Int, secondX: Int, secondY: Int): PixelSelection = PixelSelection(
            left = minOf(firstX, secondX),
            top = minOf(firstY, secondY),
            rightExclusive = maxOf(firstX, secondX) + 1,
            bottomExclusive = maxOf(firstY, secondY) + 1,
        )
    }
}

enum class PixelComponentPreset {
    CornerBrackets,
    SteppedFrame,
    DataLine,
    SnowCap,
    WaterRipple,
    LeafVine,
}

fun PixelGrid.floodFilled(
    startX: Int,
    startY: Int,
    argb: Long,
    isEditable: (x: Int, y: Int, width: Int, height: Int) -> Boolean = { _, _, _, _ -> true },
): PixelGrid {
    if (startX !in 0 until width || startY !in 0 until height) return this
    if (!isEditable(startX, startY, width, height)) return this
    val sourceColor = colorAt(startX, startY)
    if (sourceColor == argb) return this

    val next = pixels.toMutableList()
    val pending = ArrayDeque<Pair<Int, Int>>()
    pending.add(startX to startY)
    while (pending.isNotEmpty()) {
        val (x, y) = pending.removeFirst()
        if (x !in 0 until width || y !in 0 until height) continue
        val index = y * width + x
        if (next[index] != sourceColor || !isEditable(x, y, width, height)) continue
        next[index] = argb
        pending.add(x - 1 to y)
        pending.add(x + 1 to y)
        pending.add(x to y - 1)
        pending.add(x to y + 1)
    }
    return copy(pixels = next)
}

fun PixelGrid.mirroredVertically(): PixelGrid = PixelGrid(
    width = width,
    height = height,
    pixels = List(width * height) { index ->
        val x = index % width
        val y = index / width
        colorAt(x, height - 1 - y)
    },
)

fun PixelGrid.cropped(selection: PixelSelection): PixelGrid {
    val area = selection.normalized(width, height)
    return PixelGrid(
        width = area.width,
        height = area.height,
        pixels = List(area.width * area.height) { index ->
            colorAt(area.left + index % area.width, area.top + index / area.width)
        },
    )
}

fun PixelGrid.pasted(source: PixelGrid, destinationX: Int, destinationY: Int): PixelGrid {
    val next = pixels.toMutableList()
    for (y in 0 until source.height) {
        for (x in 0 until source.width) {
            val targetX = destinationX + x
            val targetY = destinationY + y
            if (targetX !in 0 until width || targetY !in 0 until height) continue
            next[targetY * width + targetX] = source.colorAt(x, y)
        }
    }
    return copy(pixels = next)
}

fun PixelGrid.movedSelection(selection: PixelSelection, deltaX: Int, deltaY: Int): Pair<PixelGrid, PixelSelection> {
    val area = selection.normalized(width, height)
    val targetLeft = (area.left + deltaX).coerceIn(0, width - area.width)
    val targetTop = (area.top + deltaY).coerceIn(0, height - area.height)
    if (targetLeft == area.left && targetTop == area.top) return this to area
    val selected = cropped(area)
    val cleared = pixels.toMutableList().apply {
        for (y in area.top until area.bottomExclusive) {
            for (x in area.left until area.rightExclusive) this[y * width + x] = TRANSPARENT_PIXEL
        }
    }
    val next = copy(pixels = cleared).pasted(selected, targetLeft, targetTop)
    return next to PixelSelection(
        targetLeft,
        targetTop,
        targetLeft + area.width,
        targetTop + area.height,
    )
}

fun PixelGrid.rotatedClockwise(): PixelGrid = PixelGrid(
    width = height,
    height = width,
    pixels = List(width * height) { index ->
        val targetX = index % height
        val targetY = index / height
        colorAt(targetY, height - 1 - targetX)
    },
)

fun PixelGrid.rotatedSelection(selection: PixelSelection): Pair<PixelGrid, PixelSelection> {
    val area = selection.normalized(width, height)
    val rotated = cropped(area).rotatedClockwise()
    val targetLeft = area.left.coerceAtMost((width - rotated.width).coerceAtLeast(0))
    val targetTop = area.top.coerceAtMost((height - rotated.height).coerceAtLeast(0))
    val cleared = pixels.toMutableList().apply {
        for (y in area.top until area.bottomExclusive) {
            for (x in area.left until area.rightExclusive) this[y * width + x] = TRANSPARENT_PIXEL
        }
    }
    val next = copy(pixels = cleared).pasted(rotated, targetLeft, targetTop)
    return next to PixelSelection(
        targetLeft,
        targetTop,
        targetLeft + rotated.width.coerceAtMost(width - targetLeft),
        targetTop + rotated.height.coerceAtMost(height - targetTop),
    )
}

fun PixelGrid.withPreset(
    preset: PixelComponentPreset,
    primary: Long,
    secondary: Long,
    isEditable: (x: Int, y: Int, width: Int, height: Int) -> Boolean = { _, _, _, _ -> true },
): PixelGrid {
    val next = pixels.toMutableList()
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (!isEditable(x, y, width, height)) continue
            val color = presetColor(preset, x, y, width, height, primary, secondary) ?: continue
            next[y * width + x] = color
        }
    }
    return copy(pixels = next)
}

private fun presetColor(
    preset: PixelComponentPreset,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    primary: Long,
    secondary: Long,
): Long? = when (preset) {
    PixelComponentPreset.CornerBrackets -> {
        val cornerX = x < 4 || x >= width - 4
        val cornerY = y < 3 || y >= height - 3
        val edgeX = x == 0 || x == width - 1
        val edgeY = y == 0 || y == height - 1
        when {
            edgeX && cornerY -> primary
            edgeY && cornerX -> primary
            (x == 1 || x == width - 2) && (y == 1 || y == height - 2) -> secondary
            else -> null
        }
    }
    PixelComponentPreset.SteppedFrame -> when {
        y == 0 && x in 2 until width - 2 -> primary
        y == height - 1 && x in 2 until width - 2 -> primary
        x == 0 && y in 2 until height - 2 -> primary
        x == width - 1 && y in 2 until height - 2 -> primary
        (x == 1 || x == width - 2) && (y == 1 || y == height - 2) -> secondary
        else -> null
    }
    PixelComponentPreset.DataLine -> when {
        y == height / 2 && x % 3 != 1 -> primary
        y == (height / 2 - 1).coerceAtLeast(0) && x % 6 == 0 -> secondary
        y == (height / 2 + 1).coerceAtMost(height - 1) && x % 6 == 3 -> secondary
        else -> null
    }
    PixelComponentPreset.SnowCap -> when {
        y == 0 -> primary
        y == 1 && x % 5 in 0..2 -> primary
        y == 2 && x % 7 == 1 -> secondary
        else -> null
    }
    PixelComponentPreset.WaterRipple -> when {
        y == height - 2 && x % 6 in 1..4 -> primary
        y == height - 1 && x % 8 in 3..5 -> secondary
        y == height - 3 && x % 10 == 5 -> secondary
        else -> null
    }
    PixelComponentPreset.LeafVine -> when {
        y == height / 2 && x % 2 == 0 -> primary
        y == height / 2 - 1 && x % 4 == 1 -> secondary
        y == height / 2 + 1 && x % 4 == 3 -> secondary
        else -> null
    }
}

@SuppressLint("UseKtx")
suspend fun decodeImageToPixelGrid(
    context: Context,
    source: Uri,
    width: Int,
    height: Int,
    palette: List<Long>,
): PixelGrid = withContext(Dispatchers.IO) {
    require(width > 0 && height > 0) { "Pixel grid dimensions are invalid" }
    val resolver = context.applicationContext.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: error("Unable to open image")
    require(bounds.outWidth in 1..MAX_COMPONENT_IMAGE_SIDE) { "Image width is invalid" }
    require(bounds.outHeight in 1..MAX_COMPONENT_IMAGE_SIDE) { "Image height is invalid" }
    require(bounds.outWidth.toLong() * bounds.outHeight <= MAX_COMPONENT_IMAGE_PIXELS) {
        "Image has too many pixels"
    }

    var sampleSize = 1
    while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > MAX_IMPORT_DECODE_SIDE) {
        sampleSize *= 2
    }
    val decoded = resolver.openInputStream(source)?.use { input ->
        BitmapFactory.decodeStream(
            input,
            null,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    } ?: error("Unable to decode image")

    try {
        val sourceAspect = decoded.width.toFloat() / decoded.height
        val targetAspect = width.toFloat() / height
        val crop = if (sourceAspect > targetAspect) {
            val cropWidth = (decoded.height * targetAspect).toInt().coerceAtLeast(1)
            Rect((decoded.width - cropWidth) / 2, 0, (decoded.width + cropWidth) / 2, decoded.height)
        } else {
            val cropHeight = (decoded.width / targetAspect).toInt().coerceAtLeast(1)
            Rect(0, (decoded.height - cropHeight) / 2, decoded.width, (decoded.height + cropHeight) / 2)
        }
        val target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            Canvas(target).drawBitmap(
                decoded,
                crop,
                Rect(0, 0, width, height),
                Paint().apply { isFilterBitmap = false },
            )
            val colors = IntArray(width * height)
            target.getPixels(colors, 0, width, 0, 0, width, height)
            PixelGrid(
                width = width,
                height = height,
                pixels = colors.map { color -> quantizePixelColor(color.toLong() and 0xFFFFFFFFL, palette) },
            )
        } finally {
            target.recycle()
        }
    } finally {
        decoded.recycle()
    }
}

internal fun quantizePixelColor(argb: Long, palette: List<Long>): Long {
    val alpha = (argb shr 24).toInt() and 0xFF
    if (alpha < MIN_IMPORT_ALPHA) return TRANSPARENT_PIXEL
    val candidates = palette.filter { it != TRANSPARENT_PIXEL }.ifEmpty { DEFAULT_PIXEL_PALETTE.drop(1) }
    val sourceRed = (argb shr 16).toInt() and 0xFF
    val sourceGreen = (argb shr 8).toInt() and 0xFF
    val sourceBlue = argb.toInt() and 0xFF
    return candidates.minByOrNull { candidate ->
        val red = (candidate shr 16).toInt() and 0xFF
        val green = (candidate shr 8).toInt() and 0xFF
        val blue = candidate.toInt() and 0xFF
        3 * abs(sourceRed - red) + 4 * abs(sourceGreen - green) + 2 * abs(sourceBlue - blue)
    } ?: argb
}

private const val MAX_IMPORT_DECODE_SIDE = 1024
private const val MIN_IMPORT_ALPHA = 40
