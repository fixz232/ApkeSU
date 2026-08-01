package me.weishu.kernelsu.ui.component

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ui.util.HomeLayoutSticker
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap

private const val HOME_LAYOUT_STICKER_MAX_SIDE = 512
private const val HOME_LAYOUT_STICKER_MIN_CACHE_BYTES = 8 * 1024 * 1024
private const val HOME_LAYOUT_STICKER_MAX_CACHE_BYTES = 24 * 1024 * 1024

private val homeLayoutStickerBitmapCache = object : LruCache<String, Bitmap>(
    (Runtime.getRuntime().maxMemory() / 16L)
        .coerceIn(
            HOME_LAYOUT_STICKER_MIN_CACHE_BYTES.toLong(),
            HOME_LAYOUT_STICKER_MAX_CACHE_BYTES.toLong(),
        )
        .toInt()
) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
}

@Composable
internal fun BoxScope.HomeLayoutStickerLayer(
    stickers: List<HomeLayoutSticker>,
) {
    if (stickers.isEmpty()) return

    BoxWithConstraints(
        modifier = Modifier
            .matchParentSize()
            .clipToBounds(),
    ) {
        val availableWidth = maxWidth.takeIf { it.value.isFinite() && it > 0.dp }
            ?: return@BoxWithConstraints
        val availableHeight = maxHeight.takeIf { it.value.isFinite() && it > 0.dp }
        stickers.forEach { sticker ->
            val bitmap = rememberHomeLayoutStickerBitmap(sticker.uriString) ?: return@forEach
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return@forEach
            val widthFraction = sticker.width.takeIf(Float::isFinite)?.coerceIn(0.08f, 1f) ?: 0.28f
            val horizontalPosition = sticker.x.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0.5f
            val verticalPosition = sticker.y.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0.5f
            val stickerWidth = availableWidth * widthFraction
            val sourceRatio = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
            val naturalHeight = stickerWidth / sourceRatio.coerceAtLeast(0.01f)
            val stickerHeight = availableHeight?.let(naturalHeight::coerceAtMost) ?: naturalHeight
            if (!stickerHeight.value.isFinite() || stickerHeight <= 0.dp) return@forEach
            val xOffset = (availableWidth - stickerWidth) * horizontalPosition
            val yOffset = availableHeight?.let { (it - stickerHeight) * verticalPosition } ?: 0.dp
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .width(stickerWidth)
                    .height(stickerHeight)
                    .alpha(sticker.opacity.takeIf(Float::isFinite)?.coerceIn(0.1f, 1f) ?: 1f),
            )
        }
    }
}

@Composable
private fun rememberHomeLayoutStickerBitmap(uriString: String): Bitmap? {
    val context = LocalContext.current.applicationContext
    return produceState<Bitmap?>(initialValue = cachedStickerBitmap(uriString), uriString, context) {
        value = withContext(Dispatchers.IO) {
            cachedStickerBitmap(uriString) ?: loadCustomImageBitmap(
                context = context,
                uriString = uriString,
                maxSide = HOME_LAYOUT_STICKER_MAX_SIDE,
            )?.also { bitmap ->
                if (!bitmap.isRecycled) {
                    synchronized(homeLayoutStickerBitmapCache) {
                        homeLayoutStickerBitmapCache.put(uriString, bitmap)
                    }
                }
            }
        }
    }.value
}

private fun cachedStickerBitmap(uriString: String): Bitmap? {
    return synchronized(homeLayoutStickerBitmapCache) {
        homeLayoutStickerBitmapCache.get(uriString)
    }?.takeUnless(Bitmap::isRecycled)
}
