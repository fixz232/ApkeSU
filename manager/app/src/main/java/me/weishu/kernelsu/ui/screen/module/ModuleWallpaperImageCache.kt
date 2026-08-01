package me.weishu.kernelsu.ui.screen.module

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap

private const val THUMBNAIL_CACHE_DIR = "module-wallpaper-thumbnails"
private const val DISK_CACHE_LIMIT_BYTES = 96L * 1024L * 1024L
private const val MIN_MEMORY_CACHE_BYTES = 8 * 1024 * 1024
private const val MAX_MEMORY_CACHE_BYTES = 48 * 1024 * 1024

private val moduleWallpaperMemoryCache = object : LruCache<String, Bitmap>(
    (Runtime.getRuntime().maxMemory() / 8L)
        .coerceIn(MIN_MEMORY_CACHE_BYTES.toLong(), MAX_MEMORY_CACHE_BYTES.toLong())
        .toInt()
) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
}

internal suspend fun loadModuleWallpaperBitmapCached(
    context: Context,
    entry: ModuleCardWallpaperEntry,
    maxSide: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    val safeMaxSide = maxSide.coerceIn(160, 1600)
    val key = moduleWallpaperBitmapCacheKey(entry, safeMaxSide)
    synchronized(moduleWallpaperMemoryCache) {
        moduleWallpaperMemoryCache.get(key)
    }?.takeUnless(Bitmap::isRecycled)?.let { return@withContext it }

    val cacheFile = File(moduleWallpaperDiskCacheDir(context), "$key.png")
    BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { cached ->
        cacheFile.setLastModified(System.currentTimeMillis())
        synchronized(moduleWallpaperMemoryCache) {
            moduleWallpaperMemoryCache.put(key, cached)
        }
        return@withContext cached
    }

    val decoded = loadCustomImageBitmap(
        context = context,
        uriString = entry.uriString,
        maxSide = safeMaxSide,
        crop = entry.crop,
    ) ?: return@withContext null
    synchronized(moduleWallpaperMemoryCache) {
        moduleWallpaperMemoryCache.put(key, decoded)
    }
    writeDiskThumbnail(cacheFile, decoded)
    trimModuleWallpaperDiskCache(cacheFile.parentFile)
    decoded
}

internal suspend fun preloadModuleWallpaperBitmap(
    context: Context,
    entry: ModuleCardWallpaperEntry?,
    maxSide: Int,
) {
    entry ?: return
    loadModuleWallpaperBitmapCached(context, entry, maxSide)
}

internal fun clearModuleWallpaperImageCache(context: Context) {
    synchronized(moduleWallpaperMemoryCache) {
        moduleWallpaperMemoryCache.evictAll()
    }
    moduleWallpaperDiskCacheDir(context).deleteRecursively()
}

private fun moduleWallpaperBitmapCacheKey(entry: ModuleCardWallpaperEntry, maxSide: Int): String {
    val visualKey = buildString {
        append(entry.uriString)
        append('|').append(entry.crop.left)
        append('|').append(entry.crop.top)
        append('|').append(entry.crop.right)
        append('|').append(entry.crop.bottom)
        append('|').append(maxSide)
        val sourceUri = Uri.parse(entry.uriString)
        if (sourceUri.scheme == "file") {
            sourceUri.path?.let(::File)?.let { source ->
                append('|').append(source.length())
                append('|').append(source.lastModified())
            }
        }
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(visualKey.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
}

private fun moduleWallpaperDiskCacheDir(context: Context): File {
    return File(context.applicationContext.cacheDir, THUMBNAIL_CACHE_DIR).also(File::mkdirs)
}

private fun writeDiskThumbnail(target: File, bitmap: Bitmap) {
    runCatching {
        val temp = File(target.parentFile, "${target.name}.tmp")
        try {
            FileOutputStream(temp).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.fd.sync()
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
            }
        } finally {
            temp.delete()
        }
    }
}

private fun trimModuleWallpaperDiskCache(directory: File?) {
    val files = directory?.listFiles()?.filter(File::isFile).orEmpty()
    var total = files.sumOf(File::length)
    if (total <= DISK_CACHE_LIMIT_BYTES) return
    files.sortedBy(File::lastModified).forEach { file ->
        if (total <= DISK_CACHE_LIMIT_BYTES) return
        val size = file.length()
        if (file.delete()) total -= size
    }
}
