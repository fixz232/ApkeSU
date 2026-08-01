package me.weishu.kernelsu.ui.util

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale

data class MediaFileInfo(
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val width: Int?,
    val height: Int?,
    val durationMillis: Long?,
    val hasAlpha: Boolean?,
    val decodable: Boolean,
    val error: String? = null,
) {
    val resolution: String?
        get() = if (width != null && height != null && width > 0 && height > 0) "${width}x${height}" else null
}

suspend fun inspectMediaFile(context: Context, uriString: String?): MediaFileInfo? {
    if (uriString.isNullOrBlank()) return null
    return inspectMediaFile(context, uriString.toUri())
}

suspend fun inspectMediaFile(context: Context, uri: Uri): MediaFileInfo = withContext(Dispatchers.IO) {
    inspectMediaFileBlocking(context, uri)
}

internal fun inspectMediaFileBlocking(
    context: Context,
    uri: Uri,
    mimeTypeHint: String? = null,
): MediaFileInfo {
    val resolver = context.applicationContext.contentResolver
    var displayName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank) ?: "media"
    var sizeBytes: Long? = null
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: displayName
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
    }
    val mimeType = mimeTypeHint ?: runCatching { resolver.getType(uri) }.getOrNull()
    val looksVideo = mimeType?.startsWith("video/") == true ||
        uri.toString().hasExtension("mp4", "webm", "mkv", "mov", "3gp", "m4v", "video")
    val looksAudio = mimeType?.startsWith("audio/") == true ||
        uri.toString().hasExtension("mp3", "ogg", "oga", "opus", "wav", "m4a", "aac", "flac", "amr", "audio")

    if (looksVideo || looksAudio) {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context.applicationContext, uri)
            }
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val frameOk = if (looksVideo) {
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) != null
            } else {
                duration != null && duration > 0L
            }
            MediaFileInfo(displayName, mimeType, sizeBytes, width, height, duration, null, frameOk)
        } catch (error: Throwable) {
            MediaFileInfo(displayName, mimeType, sizeBytes, null, null, null, null, false, error.message)
        } finally {
            runCatching { retriever.release() }
        }
    }

    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openMediaInputStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = BitmapFactory.Options().apply { inSampleSize = 8 }
        val bitmap = openMediaInputStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, sample) }
        val valid = bounds.outWidth > 0 && bounds.outHeight > 0 && bitmap != null
        val alpha = bitmap?.hasAlpha()
        bitmap?.recycle()
        MediaFileInfo(
            displayName = displayName,
            mimeType = mimeType ?: bounds.outMimeType,
            sizeBytes = sizeBytes,
            width = bounds.outWidth.takeIf { it > 0 },
            height = bounds.outHeight.takeIf { it > 0 },
            durationMillis = null,
            hasAlpha = alpha,
            decodable = valid,
            error = if (valid) null else "Unable to decode the selected image",
        )
    } catch (error: Throwable) {
        MediaFileInfo(displayName, mimeType, sizeBytes, null, null, null, null, false, error.message)
    }
}

private fun openMediaInputStream(context: Context, uri: Uri): InputStream? {
    return if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
        FileInputStream(uri.path!!)
    } else {
        context.applicationContext.contentResolver.openInputStream(uri)
    }
}

fun formatMediaBytes(bytes: Long?): String? {
    bytes ?: return null
    if (bytes < 1024L) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024.0) return String.format(Locale.ROOT, "%.1f KB", kib)
    val mib = kib / 1024.0
    if (mib < 1024.0) return String.format(Locale.ROOT, "%.1f MB", mib)
    return String.format(Locale.ROOT, "%.2f GB", mib / 1024.0)
}

fun formatMediaDuration(durationMillis: Long?): String? {
    durationMillis ?: return null
    val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
}

private fun String.hasExtension(vararg extensions: String): Boolean {
    val lower = lowercase(Locale.ROOT).substringBefore('?').substringBefore('#')
    return extensions.any { lower.endsWith(".$it") }
}
