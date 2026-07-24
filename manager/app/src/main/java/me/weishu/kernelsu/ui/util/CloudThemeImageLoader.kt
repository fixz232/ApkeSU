package me.weishu.kernelsu.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ksuApp
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

private const val CLOUD_THEME_IMAGE_MAX_BYTES = 500L * 1024L * 1024L
private const val CLOUD_THEME_IMAGE_DISK_CACHE_BYTES = 512L * 1024L * 1024L
private const val CLOUD_THEME_IMAGE_MAX_PIXELS = 80_000_000L
private val cloudThemeImageDiskLock = Any()

private val cloudThemeImageMemoryCache = object : LruCache<String, Bitmap>(16 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
}

suspend fun loadCloudThemeImage(
    context: Context,
    url: String,
    maxSide: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    if (url.isBlank() || maxSide <= 0) return@withContext null
    val validatedUrl = validateCloudThemeUrl(url, allowPackage = false)
    val memoryKey = "$validatedUrl@$maxSide"
    cloudThemeImageMemoryCache.get(memoryKey)?.let { return@withContext it }

    val cacheDirectory = File(context.applicationContext.cacheDir, "cloud-theme-images").apply {
        mkdirs()
    }
    val cacheFile = File(cacheDirectory, cloudThemeImageCacheName(validatedUrl))
    decodeCloudThemeBitmap(cacheFile, maxSide)?.let { bitmap ->
        cacheFile.setLastModified(System.currentTimeMillis())
        cloudThemeImageMemoryCache.put(memoryKey, bitmap)
        return@withContext bitmap
    }
    if (cacheFile.exists()) cacheFile.delete()

    val request = Request.Builder()
        .url(validatedUrl)
        .header("Accept", "image/avif,image/webp,image/png,image/jpeg,image/*")
        .build()
    val call = ksuApp.okhttpClient.newCall(request)
    val cancellationWatcher = watchCloudThemeCallCancellation(call)
    val temporaryFile = File(cacheDirectory, ".${cacheFile.name}.${System.nanoTime()}.part")
    try {
        call.execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            if (response.request.url.scheme != "https" ||
                !isAllowedCloudThemeHost(response.request.url.host)
            ) {
                return@withContext null
            }
            val contentLength = response.body.contentLength()
            if (contentLength > CLOUD_THEME_IMAGE_MAX_BYTES) return@withContext null
            var copied = 0L
            response.body.byteStream().use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        copied += read
                        if (copied > CLOUD_THEME_IMAGE_MAX_BYTES) return@withContext null
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                    output.fd.sync()
                }
            }
        }
        val bitmap = decodeCloudThemeBitmap(temporaryFile, maxSide) ?: return@withContext null
        synchronized(cloudThemeImageDiskLock) {
            if (!cacheFile.exists() && !temporaryFile.renameTo(cacheFile)) {
                temporaryFile.copyTo(cacheFile, overwrite = true)
            }
            cacheFile.setLastModified(System.currentTimeMillis())
            trimCloudThemeImageDiskCache(cacheDirectory)
        }
        cloudThemeImageMemoryCache.put(memoryKey, bitmap)
        bitmap
    } finally {
        temporaryFile.delete()
        cancellationWatcher.cancel()
    }
}

private fun decodeCloudThemeBitmap(file: File, maxSide: Int): Bitmap? {
    if (!file.isFile || file.length() !in 1..CLOUD_THEME_IMAGE_MAX_BYTES) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    if (bounds.outWidth > 16_384 || bounds.outHeight > 16_384) return null
    if (bounds.outWidth.toLong() * bounds.outHeight.toLong() > CLOUD_THEME_IMAGE_MAX_PIXELS) return null
    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxSide) {
        sampleSize *= 2
    }
    return runCatching {
        BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
    }.getOrNull()
}

private fun cloudThemeImageCacheName(url: String): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(url.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
}

private fun trimCloudThemeImageDiskCache(directory: File) {
    val files = directory.listFiles()
        .orEmpty()
        .filter { it.isFile && !it.name.endsWith(".part") }
        .sortedByDescending(File::lastModified)
    var retainedBytes = 0L
    files.forEach { file ->
        retainedBytes += file.length()
        if (retainedBytes > CLOUD_THEME_IMAGE_DISK_CACHE_BYTES) file.delete()
    }
}
