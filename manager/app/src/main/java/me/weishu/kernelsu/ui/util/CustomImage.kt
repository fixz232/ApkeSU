package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.roundToInt

const val CUSTOM_WALLPAPER_URI_KEY = "custom_wallpaper_uri"
const val CUSTOM_WALLPAPER_OPACITY_KEY = "custom_wallpaper_opacity"
const val CUSTOM_WALLPAPER_CROP_LEFT_KEY = "custom_wallpaper_crop_left"
const val CUSTOM_WALLPAPER_CROP_TOP_KEY = "custom_wallpaper_crop_top"
const val CUSTOM_WALLPAPER_CROP_RIGHT_KEY = "custom_wallpaper_crop_right"
const val CUSTOM_WALLPAPER_CROP_BOTTOM_KEY = "custom_wallpaper_crop_bottom"
const val CUSTOM_WALLPAPER_PASSTHROUGH_ENABLED_KEY = "custom_wallpaper_passthrough_enabled"
const val CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY_KEY = "custom_wallpaper_passthrough_opacity"
const val DEFAULT_CUSTOM_WALLPAPER_OPACITY = 0.30f
const val MIN_CUSTOM_WALLPAPER_OPACITY = 0.10f
const val MAX_CUSTOM_WALLPAPER_OPACITY = 0.80f
const val DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY = 0.25f
const val MIN_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY = 0.0f
const val MAX_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY = 0.65f
private const val CUSTOM_IMAGE_DIR_NAME = "custom-images"
private const val CUSTOM_IMAGE_EXTENSION = ".image"
private val HEX_CHARS = "0123456789abcdef".toCharArray()

val DEFAULT_CUSTOM_WALLPAPER_CROP = CustomWallpaperCrop(
    left = 0.10f,
    top = 0.10f,
    right = 0.90f,
    bottom = 0.90f,
)

val FULL_CUSTOM_WALLPAPER_CROP = CustomWallpaperCrop(
    left = 0f,
    top = 0f,
    right = 1f,
    bottom = 1f,
)

@Immutable
data class CustomWallpaperCrop(
    val left: Float = 0.10f,
    val top: Float = 0.10f,
    val right: Float = 0.90f,
    val bottom: Float = 0.90f,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

fun sanitizeCustomWallpaperOpacity(value: Float): Float {
    return value.takeIf(Float::isFinite)
        ?.coerceIn(MIN_CUSTOM_WALLPAPER_OPACITY, MAX_CUSTOM_WALLPAPER_OPACITY)
        ?: DEFAULT_CUSTOM_WALLPAPER_OPACITY
}

fun sanitizeCustomWallpaperPassthroughOpacity(value: Float): Float {
    return value.takeIf(Float::isFinite)
        ?.coerceIn(MIN_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY, MAX_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY)
        ?: DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
}

fun sanitizeCustomWallpaperCrop(crop: CustomWallpaperCrop): CustomWallpaperCrop {
    val minSize = 0.12f
    val left = crop.left.takeIf(Float::isFinite)?.coerceIn(0f, 1f - minSize) ?: 0f
    val top = crop.top.takeIf(Float::isFinite)?.coerceIn(0f, 1f - minSize) ?: 0f
    val right = crop.right.takeIf(Float::isFinite)?.coerceIn(left + minSize, 1f) ?: 1f
    val bottom = crop.bottom.takeIf(Float::isFinite)?.coerceIn(top + minSize, 1f) ?: 1f
    return CustomWallpaperCrop(left, top, right, bottom)
}

fun takePersistableImageReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

fun releasePersistableImageReadPermission(context: Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    runCatching {
        context.contentResolver.releasePersistableUriPermission(
            uriString.toUri(),
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

fun persistCustomImageReference(
    context: Context,
    sourceUri: Uri,
    storageKey: String,
    maxBytes: Long = Long.MAX_VALUE,
): String? {
    return runCatching {
        require(maxBytes > 0L) { "maxBytes must be positive" }
        val appContext = context.applicationContext
        val targetFile = customImageFile(appContext, storageKey)
        val parentDir = targetFile.parentFile ?: return@runCatching null
        val tempFile = File(parentDir, "${targetFile.name}.tmp")
        parentDir.mkdirs()
        try {
            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        totalBytes += count
                        if (totalBytes > maxBytes) {
                            throw IOException("The selected image is too large")
                        }
                        output.write(buffer, 0, count)
                    }
                    if (totalBytes == 0L) throw IOException("The selected image is empty")
                    output.fd.sync()
                }
            } ?: return@runCatching null

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            Uri.fromFile(targetFile).toString()
        } finally {
            tempFile.delete()
        }
    }.getOrNull()
}

fun releaseCustomImageReference(context: Context, uriString: String?) {
    releasePersistableImageReadPermission(context, uriString)
    deletePersistedCustomImageReference(context, uriString)
}

fun loadCustomImageBitmap(
    context: Context,
    uriString: String,
    maxSide: Int,
    crop: CustomWallpaperCrop? = null,
): Bitmap? {
    return runCatching {
        val uri = uriString.toUri()
        val source = if (uri.scheme == "file") {
            ImageDecoder.createSource(File(uri.path ?: return@runCatching null))
        } else {
            ImageDecoder.createSource(context.contentResolver, uri)
        }
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val imageMaxSide = maxOf(info.size.width, info.size.height)
            val sampleSize = maxOf(1, (imageMaxSide + maxSide - 1) / maxSide)
            decoder.setTargetSampleSize(sampleSize)
        }
        cropBitmap(bitmap, crop)
    }.getOrNull()
}

private fun deletePersistedCustomImageReference(context: Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    runCatching {
        val uri = uriString.toUri()
        if (uri.scheme != "file") return@runCatching
        val path = uri.path ?: return@runCatching
        val file = File(path).canonicalFile
        val imageDir = customImageDir(context.applicationContext).canonicalFile
        if (file.path != imageDir.path && !file.path.startsWith(imageDir.path + File.separator)) {
            return@runCatching
        }
        file.delete()
    }
}

private fun customImageFile(context: Context, storageKey: String): File {
    return File(
        customImageDir(context),
        "${hashStorageKey(storageKey)}_${UUID.randomUUID()}$CUSTOM_IMAGE_EXTENSION"
    )
}

private fun customImageDir(context: Context): File {
    return File(context.filesDir, CUSTOM_IMAGE_DIR_NAME)
}

private fun hashStorageKey(storageKey: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(storageKey.toByteArray(Charsets.UTF_8))
    val chars = CharArray(digest.size * 2)
    digest.forEachIndexed { index, byte ->
        val value = byte.toInt() and 0xff
        chars[index * 2] = HEX_CHARS[value ushr 4]
        chars[index * 2 + 1] = HEX_CHARS[value and 0x0f]
    }
    return String(chars)
}

private fun cropBitmap(bitmap: Bitmap, crop: CustomWallpaperCrop?): Bitmap {
    val safeCrop = crop?.let(::sanitizeCustomWallpaperCrop) ?: return bitmap
    if (safeCrop.left <= 0f && safeCrop.top <= 0f && safeCrop.right >= 1f && safeCrop.bottom >= 1f) {
        return bitmap
    }
    val left = (safeCrop.left * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
    val top = (safeCrop.top * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
    val right = (safeCrop.right * bitmap.width).roundToInt().coerceIn(left + 1, bitmap.width)
    val bottom = (safeCrop.bottom * bitmap.height).roundToInt().coerceIn(top + 1, bitmap.height)
    val cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    if (cropped !== bitmap && !bitmap.isRecycled) {
        bitmap.recycle()
    }
    return cropped
}
