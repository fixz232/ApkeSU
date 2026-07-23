package me.weishu.kernelsu.ui.screen.module

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import org.json.JSONArray
import org.json.JSONObject

internal const val MODULE_WALLPAPER_BACKUP_EXTENSION = "apksumwp"
internal const val MODULE_WALLPAPER_BACKUP_MIME_TYPE = "application/zip"
internal const val MODULE_WALLPAPER_SAVED_SLOT_COUNT = 5

private const val BACKUP_KIND = "apkesu.module-wallpaper"
private const val BACKUP_VERSION = 1
private const val SAVED_SLOT_VERSION = 1
private const val SAVED_SLOT_KEY_PREFIX = "module_wallpaper_saved_slot"
private const val MANIFEST_PATH = "manifest.json"
private const val MAX_MANIFEST_BYTES = 128 * 1024L
private const val MAX_IMAGE_COUNT = 32
private const val MAX_IMAGE_BYTES = 24 * 1024 * 1024L
private const val MAX_TOTAL_IMAGE_BYTES = 128 * 1024 * 1024L
private const val BUFFER_SIZE = 32 * 1024
private val IMAGE_PATH_PATTERN = Regex("images/wallpaper_[0-9]{2}\\.image")
private val SHA256_PATTERN = Regex("[0-9a-f]{64}")

internal data class ModuleWallpaperBackupPreview(
    val sourceModuleId: String,
    val sourceModuleName: String,
    val imageCount: Int,
    val carouselEnabled: Boolean,
    val totalBytes: Long,
    val createdAtMillis: Long,
)

internal data class ModuleWallpaperBackupResult(
    val success: Boolean,
    val preview: ModuleWallpaperBackupPreview? = null,
    val error: Throwable? = null,
)

internal data class ModuleWallpaperSavedSlot(
    val index: Int,
    val snapshot: ModuleCardWallpaperSnapshot,
    val savedAtMillis: Long,
)

internal data class ModuleWallpaperSlotResult(
    val success: Boolean,
    val error: Throwable? = null,
)

internal fun readModuleWallpaperSavedSlots(
    context: Context,
    moduleId: String,
): List<ModuleWallpaperSavedSlot?> {
    requireValidModuleId(moduleId)
    return List(MODULE_WALLPAPER_SAVED_SLOT_COUNT) { slotIndex ->
        readModuleWallpaperSavedSlot(context, moduleId, slotIndex)
    }
}

internal fun saveCurrentModuleWallpaperToSlot(
    context: Context,
    moduleId: String,
    slotIndex: Int,
): ModuleWallpaperSlotResult {
    return runSlotOperation {
        requireValidModuleId(moduleId)
        requireValidSavedSlotIndex(slotIndex)
        val source = readModuleCardWallpaperSnapshot(context, moduleId)
        require(source.entries.isNotEmpty()) { "The selected module has no custom wallpaper" }
        require(source.entries.size <= MAX_IMAGE_COUNT) { "Too many module wallpapers" }

        val copied = duplicateModuleWallpaperSnapshot(
            context = context,
            moduleId = moduleId,
            source = source,
            storageScope = "slot_$slotIndex",
        )
        val previous = readModuleWallpaperSavedSlot(context, moduleId, slotIndex)
        val savedAtMillis = System.currentTimeMillis()
        val committed = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putString(
                savedSlotPreferenceKey(moduleId, slotIndex),
                createSavedSlotJson(copied, savedAtMillis).toString(),
            )
            .commit()
        if (!committed) {
            copied.entries.forEach { releaseCustomImageReference(context, it.uriString) }
            throw IOException("Unable to save the wallpaper slot")
        }
        previous?.snapshot?.entries?.forEach {
            releaseCustomImageReference(context, it.uriString)
        }
    }
}

internal fun applyModuleWallpaperSavedSlot(
    context: Context,
    moduleId: String,
    slotIndex: Int,
): ModuleWallpaperSlotResult {
    return runSlotOperation {
        requireValidModuleId(moduleId)
        requireValidSavedSlotIndex(slotIndex)
        val saved = readModuleWallpaperSavedSlot(context, moduleId, slotIndex)
            ?: throw IOException("The selected wallpaper slot is empty or damaged")
        val copied = duplicateModuleWallpaperSnapshot(
            context = context,
            moduleId = moduleId,
            source = saved.snapshot,
            storageScope = "slot_${slotIndex}_apply",
        )
        if (!replaceModuleCardWallpaperSnapshot(context, moduleId, copied)) {
            copied.entries.forEach { releaseCustomImageReference(context, it.uriString) }
            throw IOException("Unable to apply the wallpaper slot")
        }
    }
}

internal fun deleteModuleWallpaperSavedSlot(
    context: Context,
    moduleId: String,
    slotIndex: Int,
): ModuleWallpaperSlotResult {
    return runSlotOperation {
        requireValidModuleId(moduleId)
        requireValidSavedSlotIndex(slotIndex)
        val previous = readModuleWallpaperSavedSlot(context, moduleId, slotIndex)
            ?: return@runSlotOperation
        val committed = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .remove(savedSlotPreferenceKey(moduleId, slotIndex))
            .commit()
        if (!committed) throw IOException("Unable to delete the wallpaper slot")
        previous.snapshot.entries.forEach {
            releaseCustomImageReference(context, it.uriString)
        }
    }
}

internal fun exportModuleWallpaperBackup(
    context: Context,
    destination: Uri,
    moduleId: String,
    moduleName: String,
): ModuleWallpaperBackupResult {
    return runBackupOperation {
        requireValidModuleId(moduleId)
        val snapshot = readModuleCardWallpaperSnapshot(context, moduleId)
        require(snapshot.entries.isNotEmpty()) { "The selected module has no custom wallpaper" }
        require(snapshot.entries.size <= MAX_IMAGE_COUNT) { "Too many module wallpapers" }

        val tempDir = createBackupTempDir(context)
        try {
            var totalBytes = 0L
            val assets = snapshot.entries.mapIndexed { index, entry ->
                val path = imagePath(index)
                val file = File(tempDir, "export_$index.image")
                val copied = openWallpaperInput(context, entry.uriString).use { input ->
                    copyLimitedWithSha256(input, file.outputStream(), MAX_IMAGE_BYTES)
                }
                totalBytes += copied.size
                require(totalBytes <= MAX_TOTAL_IMAGE_BYTES) { "Module wallpapers are too large to back up" }
                requireValidImage(file)
                PreparedAsset(
                    path = path,
                    file = file,
                    size = copied.size,
                    sha256 = copied.sha256,
                    crop = entry.crop,
                )
            }
            val createdAt = System.currentTimeMillis()
            val manifest = createManifest(
                moduleId = moduleId,
                moduleName = moduleName,
                carouselEnabled = snapshot.carouselEnabled,
                createdAtMillis = createdAt,
                assets = assets,
            )
            val output = context.contentResolver.openOutputStream(destination, "wt")
                ?: throw IOException("Unable to open the selected backup destination")
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_PATH))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                assets.forEach { asset ->
                    zip.putNextEntry(ZipEntry(asset.path))
                    asset.file.inputStream().use { it.copyTo(zip, BUFFER_SIZE) }
                    zip.closeEntry()
                }
            }
            ModuleWallpaperBackupPreview(
                sourceModuleId = moduleId,
                sourceModuleName = moduleName.ifBlank { moduleId },
                imageCount = assets.size,
                carouselEnabled = snapshot.carouselEnabled && assets.size > 1,
                totalBytes = totalBytes,
                createdAtMillis = createdAt,
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

internal fun previewModuleWallpaperBackup(
    context: Context,
    source: Uri,
): ModuleWallpaperBackupResult {
    return runBackupOperation {
        val extracted = extractAndValidateBackup(context, source)
        try {
            extracted.preview
        } finally {
            extracted.tempDir.deleteRecursively()
        }
    }
}

internal fun restoreModuleWallpaperBackup(
    context: Context,
    source: Uri,
    targetModuleId: String,
): ModuleWallpaperBackupResult {
    return runBackupOperation {
        requireValidModuleId(targetModuleId)
        val extracted = extractAndValidateBackup(context, source)
        val importedEntries = mutableListOf<ModuleCardWallpaperEntry>()
        try {
            extracted.assets.forEachIndexed { index, asset ->
                val persisted = persistCustomImageReference(
                    context = context,
                    sourceUri = Uri.fromFile(asset.file),
                    storageKey = "module_card_wallpaper_${targetModuleId}_restore_$index",
                ) ?: throw IOException("Unable to save wallpaper ${index + 1}")
                importedEntries += ModuleCardWallpaperEntry(
                    uriString = persisted,
                    crop = asset.crop,
                )
            }
            val committed = replaceModuleCardWallpaperSnapshot(
                context = context,
                moduleId = targetModuleId,
                snapshot = ModuleCardWallpaperSnapshot(
                    entries = importedEntries,
                    carouselEnabled = extracted.preview.carouselEnabled,
                ),
            )
            if (!committed) throw IOException("Unable to save module wallpaper settings")
            extracted.preview
        } catch (error: Throwable) {
            importedEntries.forEach { releaseCustomImageReference(context, it.uriString) }
            throw error
        } finally {
            extracted.tempDir.deleteRecursively()
        }
    }
}

private inline fun runBackupOperation(
    block: () -> ModuleWallpaperBackupPreview,
): ModuleWallpaperBackupResult {
    return try {
        ModuleWallpaperBackupResult(success = true, preview = block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        ModuleWallpaperBackupResult(success = false, error = error)
    }
}

private inline fun runSlotOperation(block: () -> Unit): ModuleWallpaperSlotResult {
    return try {
        block()
        ModuleWallpaperSlotResult(success = true)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        ModuleWallpaperSlotResult(success = false, error = error)
    }
}

private fun duplicateModuleWallpaperSnapshot(
    context: Context,
    moduleId: String,
    source: ModuleCardWallpaperSnapshot,
    storageScope: String,
): ModuleCardWallpaperSnapshot {
    val copiedEntries = mutableListOf<ModuleCardWallpaperEntry>()
    try {
        source.entries.forEachIndexed { index, entry ->
            val persisted = persistCustomImageReference(
                context = context,
                sourceUri = Uri.parse(entry.uriString),
                storageKey = "module_card_wallpaper_${moduleId}_${storageScope}_$index",
            ) ?: throw IOException("Unable to copy wallpaper ${index + 1}")
            copiedEntries += entry.copy(uriString = persisted)
        }
        return ModuleCardWallpaperSnapshot(
            entries = copiedEntries,
            carouselEnabled = source.carouselEnabled && copiedEntries.size > 1,
        )
    } catch (error: Throwable) {
        copiedEntries.forEach { releaseCustomImageReference(context, it.uriString) }
        throw error
    }
}

private fun readModuleWallpaperSavedSlot(
    context: Context,
    moduleId: String,
    slotIndex: Int,
): ModuleWallpaperSavedSlot? {
    requireValidSavedSlotIndex(slotIndex)
    val raw = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        .getString(savedSlotPreferenceKey(moduleId, slotIndex), null)
        ?: return null
    return runCatching {
        val json = JSONObject(raw)
        require(json.optInt("version", -1) == SAVED_SLOT_VERSION) {
            "Unsupported wallpaper slot version"
        }
        val entriesJson = json.optJSONArray("wallpapers")
            ?: throw IOException("Wallpaper slot contents are missing")
        require(entriesJson.length() in 1..MAX_IMAGE_COUNT) { "Invalid wallpaper slot count" }
        val entries = buildList {
            for (index in 0 until entriesJson.length()) {
                val item = entriesJson.optJSONObject(index)
                    ?: throw IOException("Invalid wallpaper slot entry ${index + 1}")
                val uriString = item.optString("uri").trim()
                require(uriString.isNotEmpty()) { "Wallpaper slot URI is missing" }
                add(
                    ModuleCardWallpaperEntry(
                        uriString = uriString,
                        crop = item.requireCrop(),
                    )
                )
            }
        }
        ModuleWallpaperSavedSlot(
            index = slotIndex,
            snapshot = ModuleCardWallpaperSnapshot(
                entries = entries,
                carouselEnabled = json.optBoolean("carouselEnabled", false) && entries.size > 1,
            ),
            savedAtMillis = json.optLong("savedAt", 0L).coerceAtLeast(0L),
        )
    }.getOrNull()
}

private fun createSavedSlotJson(
    snapshot: ModuleCardWallpaperSnapshot,
    savedAtMillis: Long,
): JSONObject {
    val wallpapers = JSONArray()
    snapshot.entries.forEach { entry ->
        wallpapers.put(
            JSONObject()
                .put("uri", entry.uriString)
                .put("crop", entry.crop.toBackupJson())
        )
    }
    return JSONObject()
        .put("version", SAVED_SLOT_VERSION)
        .put("savedAt", savedAtMillis)
        .put("carouselEnabled", snapshot.carouselEnabled && snapshot.entries.size > 1)
        .put("wallpapers", wallpapers)
}

private fun requireValidSavedSlotIndex(slotIndex: Int) {
    require(slotIndex in 0 until MODULE_WALLPAPER_SAVED_SLOT_COUNT) {
        "Invalid wallpaper slot"
    }
}

private fun savedSlotPreferenceKey(moduleId: String, slotIndex: Int): String {
    return "${SAVED_SLOT_KEY_PREFIX}_${moduleId}_$slotIndex"
}

private data class PreparedAsset(
    val path: String,
    val file: File,
    val size: Long,
    val sha256: String,
    val crop: CustomWallpaperCrop,
)

private data class ExtractedBackup(
    val tempDir: File,
    val preview: ModuleWallpaperBackupPreview,
    val assets: List<PreparedAsset>,
)

private data class CopiedAsset(
    val size: Long,
    val sha256: String,
)

private fun createManifest(
    moduleId: String,
    moduleName: String,
    carouselEnabled: Boolean,
    createdAtMillis: Long,
    assets: List<PreparedAsset>,
): JSONObject {
    val wallpapers = JSONArray()
    assets.forEach { asset ->
        wallpapers.put(
            JSONObject()
                .put("path", asset.path)
                .put("size", asset.size)
                .put("sha256", asset.sha256)
                .put("crop", asset.crop.toBackupJson())
        )
    }
    return JSONObject()
        .put("kind", BACKUP_KIND)
        .put("version", BACKUP_VERSION)
        .put("sourceModuleId", moduleId)
        .put("sourceModuleName", moduleName.ifBlank { moduleId })
        .put("createdAt", createdAtMillis)
        .put("carouselEnabled", carouselEnabled && assets.size > 1)
        .put("wallpapers", wallpapers)
}

private fun extractAndValidateBackup(context: Context, source: Uri): ExtractedBackup {
    val input = context.contentResolver.openInputStream(source)
        ?: throw IOException("Unable to open the selected backup")
    val tempDir = createBackupTempDir(context)
    try {
        var manifestBytes: ByteArray? = null
        var totalImageBytes = 0L
        val extractedFiles = linkedMapOf<String, Pair<File, CopiedAsset>>()
        val seenPaths = hashSetOf<String>()
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val path = entry.name
                requireSafeZipPath(path)
                require(seenPaths.add(path)) { "Duplicate backup entry: $path" }
                when {
                    entry.isDirectory -> throw IOException("Unexpected directory in backup: $path")
                    path == MANIFEST_PATH -> {
                        manifestBytes = readLimited(zip, MAX_MANIFEST_BYTES)
                    }
                    IMAGE_PATH_PATTERN.matches(path) -> {
                        require(extractedFiles.size < MAX_IMAGE_COUNT) { "Too many wallpapers in backup" }
                        val file = File(tempDir, "import_${extractedFiles.size}.image")
                        val copied = copyLimitedWithSha256(zip, file.outputStream(), MAX_IMAGE_BYTES)
                        totalImageBytes += copied.size
                        require(totalImageBytes <= MAX_TOTAL_IMAGE_BYTES) { "Backup wallpapers are too large" }
                        extractedFiles[path] = file to copied
                    }
                    else -> throw IOException("Unsupported backup entry: $path")
                }
                zip.closeEntry()
            }
        }

        val manifest = manifestBytes?.toString(Charsets.UTF_8)?.let(::JSONObject)
            ?: throw IOException("Backup manifest is missing")
        require(manifest.optString("kind") == BACKUP_KIND) { "This is not an ApkeSU module wallpaper backup" }
        require(manifest.optInt("version", -1) == BACKUP_VERSION) { "Unsupported module wallpaper backup version" }
        val sourceModuleId = manifest.optString("sourceModuleId").trim()
        requireValidModuleId(sourceModuleId)
        val sourceModuleName = manifest.optString("sourceModuleName")
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.take(160)
            ?: sourceModuleId
        val createdAt = manifest.optLong("createdAt", 0L).coerceAtLeast(0L)
        val wallpaperArray = manifest.optJSONArray("wallpapers")
            ?: throw IOException("Wallpaper list is missing")
        require(wallpaperArray.length() in 1..MAX_IMAGE_COUNT) { "Invalid wallpaper count" }

        val referencedPaths = hashSetOf<String>()
        val assets = buildList {
            for (index in 0 until wallpaperArray.length()) {
                val item = wallpaperArray.optJSONObject(index)
                    ?: throw IOException("Invalid wallpaper entry ${index + 1}")
                val path = item.optString("path")
                require(IMAGE_PATH_PATTERN.matches(path)) { "Invalid wallpaper path" }
                require(referencedPaths.add(path)) { "Duplicate wallpaper path" }
                val expectedSize = item.optLong("size", -1L)
                require(expectedSize in 1..MAX_IMAGE_BYTES) { "Invalid wallpaper size" }
                val expectedSha256 = item.optString("sha256").lowercase()
                require(SHA256_PATTERN.matches(expectedSha256)) { "Invalid wallpaper checksum" }
                val extracted = extractedFiles[path]
                    ?: throw IOException("Wallpaper file is missing: $path")
                require(extracted.second.size == expectedSize) { "Wallpaper size check failed" }
                require(extracted.second.sha256 == expectedSha256) { "Wallpaper checksum check failed" }
                requireValidImage(extracted.first)
                add(
                    PreparedAsset(
                        path = path,
                        file = extracted.first,
                        size = expectedSize,
                        sha256 = expectedSha256,
                        crop = item.requireCrop(),
                    )
                )
            }
        }
        require(extractedFiles.keys == referencedPaths) { "Backup contains unreferenced wallpaper files" }
        val carouselEnabled = manifest.optBoolean("carouselEnabled", false) && assets.size > 1
        return ExtractedBackup(
            tempDir = tempDir,
            preview = ModuleWallpaperBackupPreview(
                sourceModuleId = sourceModuleId,
                sourceModuleName = sourceModuleName,
                imageCount = assets.size,
                carouselEnabled = carouselEnabled,
                totalBytes = assets.sumOf { it.size },
                createdAtMillis = createdAt,
            ),
            assets = assets,
        )
    } catch (error: Throwable) {
        tempDir.deleteRecursively()
        throw error
    }
}

private fun openWallpaperInput(context: Context, uriString: String): InputStream {
    val uri = uriString.toUri()
    return if (uri.scheme == ContentResolver.SCHEME_FILE) {
        val path = uri.path ?: throw IOException("Invalid wallpaper file path")
        FileInputStream(File(path))
    } else {
        context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to read a module wallpaper")
    }
}

private fun copyLimitedWithSha256(
    input: InputStream,
    rawOutput: OutputStream,
    maxBytes: Long,
): CopiedAsset {
    val digest = MessageDigest.getInstance("SHA-256")
    var total = 0L
    BufferedOutputStream(rawOutput).use { output ->
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            total += count
            require(total <= maxBytes) { "Wallpaper exceeds the backup size limit" }
            digest.update(buffer, 0, count)
            output.write(buffer, 0, count)
        }
    }
    require(total > 0) { "Wallpaper file is empty" }
    return CopiedAsset(total, digest.digest().toHex())
}

private fun readLimited(input: InputStream, maxBytes: Long): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (count == 0) continue
        total += count
        require(total <= maxBytes) { "Backup manifest is too large" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun requireValidImage(file: File) {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, options)
    require(options.outWidth > 0 && options.outHeight > 0) { "Backup contains an invalid image" }
}

private fun JSONObject.requireCrop(): CustomWallpaperCrop {
    val crop = optJSONObject("crop") ?: throw IOException("Wallpaper crop is missing")
    val left = crop.optDouble("left", Double.NaN).toFloat()
    val top = crop.optDouble("top", Double.NaN).toFloat()
    val right = crop.optDouble("right", Double.NaN).toFloat()
    val bottom = crop.optDouble("bottom", Double.NaN).toFloat()
    require(left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()) {
        "Wallpaper crop is invalid"
    }
    require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f) {
        "Wallpaper crop is outside the image"
    }
    require(right - left >= 0.12f && bottom - top >= 0.12f) { "Wallpaper crop is too small" }
    return CustomWallpaperCrop(left, top, right, bottom)
}

private fun CustomWallpaperCrop.toBackupJson(): JSONObject {
    return JSONObject()
        .put("left", left)
        .put("top", top)
        .put("right", right)
        .put("bottom", bottom)
}

private fun requireValidModuleId(moduleId: String) {
    require(moduleId.isNotBlank() && moduleId.length <= 160) { "Invalid module ID" }
    require(moduleId.none { it == '\u0000' || it == '\n' || it == '\r' }) { "Invalid module ID" }
}

private fun requireSafeZipPath(path: String) {
    require(path.isNotBlank() && path.length <= 128) { "Invalid backup entry path" }
    require(!path.startsWith('/') && '\\' !in path) { "Unsafe backup entry path" }
    require(path.split('/').none { it.isBlank() || it == "." || it == ".." }) {
        "Unsafe backup entry path"
    }
}

private fun imagePath(index: Int): String = "images/wallpaper_${index.toString().padStart(2, '0')}.image"

private fun createBackupTempDir(context: Context): File {
    return File(context.cacheDir, "module-wallpaper-backup-${UUID.randomUUID()}").also { directory ->
        require(directory.mkdirs()) { "Unable to create temporary backup storage" }
    }
}

private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
    (byte.toInt() and 0xff).toString(16).padStart(2, '0')
}
