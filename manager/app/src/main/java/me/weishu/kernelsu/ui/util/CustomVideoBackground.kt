package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.util.Locale

const val CUSTOM_VIDEO_BACKGROUND_URI_KEY = "custom_video_background_uri"
const val CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS_KEY = "custom_video_background_duration_seconds"
const val CUSTOM_VIDEO_BACKGROUND_FRAME_RATE_KEY = "custom_video_background_frame_rate"
const val DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS = 10
const val DEFAULT_CUSTOM_VIDEO_BACKGROUND_FRAME_RATE = 120
const val MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS = 3
const val MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS = 60

val CUSTOM_VIDEO_BACKGROUND_FRAME_RATE_OPTIONS = listOf(60, 90, 120, 165)

val CUSTOM_BACKGROUND_MIME_TYPES = arrayOf(
    "image/*",
    "video/*",
)

fun sanitizeCustomVideoBackgroundDurationSeconds(value: Int): Int {
    return value.coerceIn(
        MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
        MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
    )
}

fun sanitizeCustomVideoBackgroundFrameRate(value: Int): Int {
    return when (value) {
        60, 90, 120, 165 -> value
        else -> DEFAULT_CUSTOM_VIDEO_BACKGROUND_FRAME_RATE
    }
}

fun takePersistableVideoBackgroundReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}

fun releasePersistableVideoBackgroundReadPermission(context: Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    runCatching {
        context.contentResolver.releasePersistableUriPermission(
            Uri.parse(uriString),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}

fun isCustomVideoBackground(context: Context, uri: Uri): Boolean {
    val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    if (mimeType?.startsWith("video/") == true) return true

    return hasVideoExtension(uri.toString()) ||
        hasVideoExtension(queryDisplayName(context, uri))
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex < 0) null else cursor.getString(nameIndex)
        }
    }.getOrNull()
}

private fun hasVideoExtension(value: String?): Boolean {
    val text = value?.lowercase(Locale.ROOT) ?: return false
    return text.endsWith(".mp4") ||
        text.endsWith(".webm") ||
        text.endsWith(".mkv") ||
        text.endsWith(".3gp") ||
        text.endsWith(".mov") ||
        text.endsWith(".video")
}
