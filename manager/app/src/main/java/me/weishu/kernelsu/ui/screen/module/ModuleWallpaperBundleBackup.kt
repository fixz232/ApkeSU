package me.weishu.kernelsu.ui.screen.module

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import me.weishu.kernelsu.data.model.Module
import org.json.JSONArray
import org.json.JSONObject

internal const val MODULE_WALLPAPER_BUNDLE_EXTENSION = "apksumwall"
internal const val MODULE_WALLPAPER_BUNDLE_MIME_TYPE = "application/zip"

private const val BUNDLE_KIND = "apkesu.module-wallpaper-bundle"
private const val BUNDLE_VERSION = 1
private const val BUNDLE_MANIFEST = "bundle.json"
private const val MAX_BUNDLE_MODULES = 256
private const val MAX_MODULE_ARCHIVE_BYTES = 512L * 1024L * 1024L
private const val MAX_BUNDLE_BYTES = 2L * 1024L * 1024L * 1024L
private const val BUFFER_SIZE = 64 * 1024
private val MODULE_ARCHIVE_PATH = Regex("modules/[a-zA-Z0-9._-]{1,100}\\.apksumwp")
private val SHA256 = Regex("[0-9a-f]{64}")

internal data class ModuleWallpaperBundlePreview(
    val moduleCount: Int,
    val imageCount: Int,
    val totalBytes: Long,
    val createdAtMillis: Long,
)

internal data class ModuleWallpaperBundleResult(
    val success: Boolean,
    val preview: ModuleWallpaperBundlePreview? = null,
    val restoredModules: Int = 0,
    val skippedModules: Int = 0,
    val error: Throwable? = null,
)

private data class BundleModuleArchive(
    val moduleId: String,
    val moduleName: String,
    val path: String,
    val file: File,
    val size: Long,
    val sha256: String,
    val preview: ModuleWallpaperBackupPreview,
)

private data class ExtractedBundle(
    val directory: File,
    val preview: ModuleWallpaperBundlePreview,
    val modules: List<BundleModuleArchive>,
)

internal fun exportAllModuleWallpaperBackup(
    context: Context,
    destination: Uri,
    modules: List<Module>,
): ModuleWallpaperBundleResult = runCatching {
    val configured = modules.distinctBy(Module::id).filter { module ->
        readModuleCardWallpaperSnapshot(context, module.id).allEntries().isNotEmpty() ||
            readModuleWallpaperSavedSlots(context, module.id).any { it != null }
    }
    require(configured.isNotEmpty()) { "No module wallpaper configuration is available" }
    require(configured.size <= MAX_BUNDLE_MODULES) { "Too many modules in wallpaper backup" }
    val directory = createBundleTempDir(context)
    try {
        val archives = mutableListOf<BundleModuleArchive>()
        configured.forEachIndexed { index, module ->
            val file = File(directory, "module_$index.apksumwp")
            val result = exportModuleWallpaperBackup(
                context = context,
                destination = file.toUri(),
                moduleId = module.id,
                moduleName = module.name,
            )
            val preview = result.preview
            if (!result.success || preview == null) throw result.error ?: IOException("Unable to back up ${module.name}")
            require(file.length() in 1..MAX_MODULE_ARCHIVE_BYTES) { "A module wallpaper backup is too large" }
            archives += BundleModuleArchive(
                moduleId = module.id,
                moduleName = module.name,
                path = "modules/${module.id.safeArchiveName()}-$index.apksumwp",
                file = file,
                size = file.length(),
                sha256 = file.sha256(),
                preview = preview,
            )
        }
        val totalBytes = archives.sumOf { it.size }
        require(totalBytes <= MAX_BUNDLE_BYTES) { "The complete wallpaper backup is too large" }
        val createdAt = System.currentTimeMillis()
        val manifest = createBundleManifest(archives, createdAt)
        val bundleFile = File(directory, "complete.$MODULE_WALLPAPER_BUNDLE_EXTENSION")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(bundleFile))).use { zip ->
            archives.forEach { archive ->
                zip.putNextEntry(ZipEntry(archive.path))
                archive.file.inputStream().use { it.copyTo(zip, BUFFER_SIZE) }
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry(BUNDLE_MANIFEST))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        context.contentResolver.openOutputStream(destination, "wt")?.use { output ->
            bundleFile.inputStream().use { it.copyTo(output, BUFFER_SIZE) }
        } ?: throw IOException("Unable to open the complete backup destination")
        ModuleWallpaperBundlePreview(
            moduleCount = archives.size,
            imageCount = archives.sumOf { it.preview.imageCount },
            totalBytes = totalBytes,
            createdAtMillis = createdAt,
        )
    } finally {
        directory.deleteRecursively()
    }
}.fold(
    onSuccess = { ModuleWallpaperBundleResult(success = true, preview = it) },
    onFailure = {
        if (it is CancellationException) throw it
        ModuleWallpaperBundleResult(success = false, error = it)
    },
)

internal fun previewAllModuleWallpaperBackup(
    context: Context,
    source: Uri,
): ModuleWallpaperBundleResult = runCatching {
    val extracted = extractBundle(context, source)
    try {
        extracted.preview
    } finally {
        extracted.directory.deleteRecursively()
    }
}.fold(
    onSuccess = { ModuleWallpaperBundleResult(success = true, preview = it) },
    onFailure = {
        if (it is CancellationException) throw it
        ModuleWallpaperBundleResult(success = false, error = it)
    },
)

internal fun restoreAllModuleWallpaperBackup(
    context: Context,
    source: Uri,
    installedModules: List<Module>,
    mode: ModuleWallpaperRestoreMode,
): ModuleWallpaperBundleResult {
    return runCatching {
        val extracted = extractBundle(context, source)
        try {
            val installedById = installedModules.associateBy(Module::id)
            val preparedRestores = mutableListOf<PreparedModuleWallpaperRestore>()
            var skipped = 0
            try {
                extracted.modules.forEach { archive ->
                    val target = installedById[archive.moduleId]
                    if (target == null) {
                        skipped++
                    } else {
                        preparedRestores += prepareModuleWallpaperRestore(
                            context = context,
                            source = archive.file.toUri(),
                            targetModuleId = target.id,
                            mode = mode,
                        )
                    }
                }
                commitPreparedModuleWallpaperRestores(context, preparedRestores)
            } catch (error: Throwable) {
                releasePreparedModuleWallpaperRestores(context, preparedRestores)
                throw error
            }
            Triple(extracted.preview, preparedRestores.size, skipped)
        } finally {
            extracted.directory.deleteRecursively()
        }
    }.fold(
        onSuccess = { (preview, restored, skipped) ->
            ModuleWallpaperBundleResult(
                success = true,
                preview = preview,
                restoredModules = restored,
                skippedModules = skipped,
            )
        },
        onFailure = {
            if (it is CancellationException) throw it
            ModuleWallpaperBundleResult(success = false, error = it)
        },
    )
}

private fun createBundleManifest(
    archives: List<BundleModuleArchive>,
    createdAtMillis: Long,
): JSONObject {
    val modules = JSONArray()
    archives.forEach { archive ->
        modules.put(
            JSONObject()
                .put("moduleId", archive.moduleId)
                .put("moduleName", archive.moduleName.take(160))
                .put("path", archive.path)
                .put("size", archive.size)
                .put("sha256", archive.sha256)
                .put("imageCount", archive.preview.imageCount)
        )
    }
    return JSONObject()
        .put("kind", BUNDLE_KIND)
        .put("version", BUNDLE_VERSION)
        .put("createdAt", createdAtMillis)
        .put("modules", modules)
}

private fun extractBundle(context: Context, source: Uri): ExtractedBundle {
    val directory = createBundleTempDir(context)
    try {
        var manifestBytes: ByteArray? = null
        var totalBytes = 0L
        val files = linkedMapOf<String, File>()
        ZipInputStream(BufferedInputStream(context.contentResolver.openInputStream(source)
            ?: throw IOException("Unable to open the complete wallpaper backup"))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val path = entry.name
                require(path == BUNDLE_MANIFEST || MODULE_ARCHIVE_PATH.matches(path)) { "Invalid bundle entry" }
                require(!entry.isDirectory && path !in files) { "Duplicate or invalid bundle entry" }
                if (path == BUNDLE_MANIFEST) {
                    manifestBytes = zip.readLimited(256 * 1024L)
                } else {
                    require(files.size < MAX_BUNDLE_MODULES) { "Too many modules in wallpaper backup" }
                    val file = File(directory, "archive_${files.size}.apksumwp")
                    val copied = zip.copyLimited(file, MAX_MODULE_ARCHIVE_BYTES)
                    totalBytes += copied
                    require(totalBytes <= MAX_BUNDLE_BYTES) { "The complete wallpaper backup is too large" }
                    files[path] = file
                }
                zip.closeEntry()
            }
        }
        val manifest = JSONObject(manifestBytes?.toString(Charsets.UTF_8)
            ?: throw IOException("Complete backup manifest is missing"))
        require(manifest.optString("kind") == BUNDLE_KIND && manifest.optInt("version") == BUNDLE_VERSION) {
            "This is not a supported complete module wallpaper backup"
        }
        val modulesJson = manifest.optJSONArray("modules") ?: throw IOException("Module list is missing")
        require(modulesJson.length() in 1..MAX_BUNDLE_MODULES) { "Invalid module count" }
        val seenIds = hashSetOf<String>()
        val seenPaths = hashSetOf<String>()
        val modules = buildList {
            for (index in 0 until modulesJson.length()) {
                val item = modulesJson.optJSONObject(index) ?: throw IOException("Invalid module entry")
                val moduleId = item.optString("moduleId").trim()
                require(moduleId.isNotEmpty() && moduleId.length <= 160 && seenIds.add(moduleId)) { "Invalid module ID" }
                val path = item.optString("path")
                require(MODULE_ARCHIVE_PATH.matches(path) && seenPaths.add(path)) { "Invalid module archive path" }
                val file = files[path] ?: throw IOException("Module archive is missing")
                val size = item.optLong("size", -1L)
                val sha256 = item.optString("sha256").lowercase()
                require(size == file.length() && SHA256.matches(sha256) && file.sha256() == sha256) {
                    "Module archive verification failed"
                }
                val previewResult = previewModuleWallpaperBackup(context, file.toUri())
                val preview = previewResult.preview
                require(previewResult.success && preview != null && preview.sourceModuleId == moduleId) {
                    "Nested module wallpaper backup is invalid"
                }
                add(
                    BundleModuleArchive(
                        moduleId = moduleId,
                        moduleName = item.optString("moduleName").take(160).ifBlank { moduleId },
                        path = path,
                        file = file,
                        size = size,
                        sha256 = sha256,
                        preview = preview,
                    )
                )
            }
        }
        require(files.keys == seenPaths) { "Complete backup contains unreferenced files" }
        return ExtractedBundle(
            directory = directory,
            preview = ModuleWallpaperBundlePreview(
                moduleCount = modules.size,
                imageCount = modules.sumOf { it.preview.imageCount },
                totalBytes = modules.sumOf { it.size },
                createdAtMillis = manifest.optLong("createdAt", 0L).coerceAtLeast(0L),
            ),
            modules = modules,
        )
    } catch (error: Throwable) {
        directory.deleteRecursively()
        throw error
    }
}

private fun createBundleTempDir(context: Context): File {
    return File(context.cacheDir, "module-wallpaper-bundle-${UUID.randomUUID()}").also {
        require(it.mkdirs()) { "Unable to create complete backup storage" }
    }
}

private fun String.safeArchiveName(): String {
    return map { if (it.isLetterOrDigit() || it == '.' || it == '_' || it == '-') it else '_' }
        .joinToString(separator = "")
        .take(80)
        .ifBlank { "module" }
}

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count > 0) digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString(separator = "") {
        (it.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}

private fun ZipInputStream.readLimited(maxBytes: Long): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        if (count == 0) continue
        total += count
        require(total <= maxBytes) { "Complete backup manifest is too large" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun ZipInputStream.copyLimited(target: File, maxBytes: Long): Long {
    var total = 0L
    FileOutputStream(target).use { output ->
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            if (count == 0) continue
            total += count
            require(total <= maxBytes) { "A module archive is too large" }
            output.write(buffer, 0, count)
        }
        output.fd.sync()
    }
    require(total > 0L) { "A module archive is empty" }
    return total
}
