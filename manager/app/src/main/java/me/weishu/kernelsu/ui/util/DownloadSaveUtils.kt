package me.weishu.kernelsu.ui.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

suspend fun saveTextToDownloads(
    context: Context,
    displayName: String,
    text: String,
    mimeType: String = "text/plain",
): String = withContext(Dispatchers.IO) {
    saveToDownloads(context, displayName, mimeType) { output ->
        output.write(text.toByteArray(Charsets.UTF_8))
    }
}

suspend fun saveFileToDownloads(
    context: Context,
    displayName: String,
    source: File,
    mimeType: String = "application/octet-stream",
): String = withContext(Dispatchers.IO) {
    require(source.isFile && source.length() > 0) { "Source file is empty or missing" }
    saveToDownloads(context, displayName, mimeType) { output ->
        source.inputStream().use { input -> input.copyTo(output) }
    }
}

private fun saveToDownloads(
    context: Context,
    displayName: String,
    mimeType: String,
    write: (OutputStream) -> Unit,
): String {
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
    return try {
        resolver.openOutputStream(uri)?.use { output ->
            write(output)
        } ?: error("Unable to open download entry")

        val publishValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        check(resolver.update(uri, publishValues, null, null) > 0) {
            "Unable to publish download entry"
        }
        saved = true
        "${Environment.DIRECTORY_DOWNLOADS}/$displayName"
    } finally {
        if (!saved) {
            resolver.delete(uri, null, null)
        }
    }
}
