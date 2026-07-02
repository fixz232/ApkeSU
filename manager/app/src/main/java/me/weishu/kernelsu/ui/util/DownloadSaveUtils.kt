package me.weishu.kernelsu.ui.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun saveTextToDownloads(
    context: Context,
    displayName: String,
    text: String,
    mimeType: String = "text/plain",
): String = withContext(Dispatchers.IO) {
    val resolver = context.applicationContext.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: error("Unable to create download entry")
    var saved = false
    try {
        resolver.openOutputStream(uri)?.use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        } ?: error("Unable to open download entry")

        val publishValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        resolver.update(uri, publishValues, null, null)
        saved = true
        "${Environment.DIRECTORY_DOWNLOADS}/$displayName"
    } finally {
        if (!saved) {
            resolver.delete(uri, null, null)
        }
    }
}
