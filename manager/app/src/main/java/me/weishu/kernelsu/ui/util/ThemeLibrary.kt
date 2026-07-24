package me.weishu.kernelsu.ui.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

private const val THEME_LIBRARY_SCHEMA = "io.github.fixz.apkesu.theme-library"
private const val THEME_LIBRARY_VERSION = 1
private const val MAX_THEME_LIBRARY_NAME_LENGTH = 48
private const val MAX_THEME_LIBRARY_ARCHIVE_BYTES = 576L * 1024L * 1024L
private val THEME_LIBRARY_ID_PATTERN = Regex("[a-zA-Z0-9_-]{1,80}")
private val themeLibraryLock = Any()

data class ThemeLibraryEntry(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAppliedAt: Long?,
    val sizeBytes: Long,
)

data class ThemeLibraryOperationResult(
    val success: Boolean,
    val entry: ThemeLibraryEntry? = null,
    val packageResult: ThemeStorePackageResult? = null,
    val error: Throwable? = null,
)

fun readThemeLibrary(context: Context): List<ThemeLibraryEntry> = synchronized(themeLibraryLock) {
    readThemeLibraryLocked(context.applicationContext)
}

fun saveCurrentThemeToLibrary(
    context: Context,
    name: String,
): ThemeLibraryOperationResult = synchronized(themeLibraryLock) {
    val appContext = context.applicationContext
    runCatching {
        val safeName = sanitizeThemeLibraryName(name)
        require(safeName.isNotBlank()) { "Theme name is required" }
        val root = themeLibraryDirectory(appContext)
        val id = UUID.randomUUID().toString()
        val temporaryFile = File(root, ".$id.tmp")
        val targetFile = themeLibraryPackageFile(root, id)
        var indexed = false
        try {
            val packageResult = exportThemeStorePackage(appContext, Uri.fromFile(temporaryFile))
            if (!packageResult.success) {
                return@synchronized ThemeLibraryOperationResult(
                    success = false,
                    packageResult = packageResult,
                    error = packageResult.error,
                )
            }
            require(temporaryFile.isFile) { "Theme package was not created" }
            moveThemeLibraryFile(temporaryFile, targetFile)
            val now = System.currentTimeMillis()
            val entry = ThemeLibraryEntry(
                id = id,
                name = safeName,
                createdAt = now,
                updatedAt = now,
                lastAppliedAt = null,
                sizeBytes = targetFile.length(),
            )
            writeThemeLibraryIndex(appContext, listOf(entry) + readThemeLibraryLocked(appContext))
            indexed = true
            ThemeLibraryOperationResult(
                success = true,
                entry = entry,
                packageResult = packageResult,
            )
        } finally {
            temporaryFile.delete()
            if (!indexed) targetFile.delete()
        }
    }.getOrElse { ThemeLibraryOperationResult(success = false, error = it) }
}

fun importThemeToLibrary(
    context: Context,
    source: Uri,
    preferredName: String? = null,
): ThemeLibraryOperationResult = synchronized(themeLibraryLock) {
    val appContext = context.applicationContext
    runCatching {
        val root = themeLibraryDirectory(appContext)
        val id = UUID.randomUUID().toString()
        val temporaryFile = File(root, ".$id.import")
        val targetFile = themeLibraryPackageFile(root, id)
        var indexed = false
        try {
            openThemeLibraryInputStream(appContext, source).use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    input.copyThemeLibraryArchiveTo(output)
                }
            }
            val validation = validateThemeStorePackage(appContext, Uri.fromFile(temporaryFile))
            if (!validation.success) {
                return@synchronized ThemeLibraryOperationResult(
                    success = false,
                    packageResult = validation,
                    error = validation.error,
                )
            }
            val importedName = preferredName
                ?: queryThemeLibraryDisplayName(appContext, source)?.substringBeforeLast('.')
                ?: "Imported theme"
            val safeName = sanitizeThemeLibraryName(importedName).ifBlank { "Imported theme" }
            moveThemeLibraryFile(temporaryFile, targetFile)
            val now = System.currentTimeMillis()
            val entry = ThemeLibraryEntry(
                id = id,
                name = safeName,
                createdAt = now,
                updatedAt = now,
                lastAppliedAt = null,
                sizeBytes = targetFile.length(),
            )
            writeThemeLibraryIndex(appContext, listOf(entry) + readThemeLibraryLocked(appContext))
            indexed = true
            ThemeLibraryOperationResult(success = true, entry = entry, packageResult = validation)
        } finally {
            temporaryFile.delete()
            if (!indexed) targetFile.delete()
        }
    }.getOrElse { ThemeLibraryOperationResult(success = false, error = it) }
}

fun applyThemeFromLibrary(
    context: Context,
    entryId: String,
    clearCloudThemeState: Boolean = true,
): ThemeLibraryOperationResult =
    synchronized(themeLibraryLock) {
        val appContext = context.applicationContext
        runCatching {
            val entries = readThemeLibraryLocked(appContext)
            val entry = entries.firstOrNull { it.id == entryId }
                ?: error("Saved theme was not found")
            val packageFile = themeLibraryPackageFile(themeLibraryDirectory(appContext), entry.id)
            require(packageFile.isFile) { "Saved theme package is missing" }
            val packageResult = importThemeStorePackage(
                appContext,
                Uri.fromFile(packageFile),
                clearCloudThemeState = clearCloudThemeState,
            )
            if (!packageResult.success) {
                return@synchronized ThemeLibraryOperationResult(
                    success = false,
                    entry = entry,
                    packageResult = packageResult,
                    error = packageResult.error,
                )
            }
            val appliedEntry = entry.copy(lastAppliedAt = System.currentTimeMillis())
            runCatching {
                writeThemeLibraryIndex(
                    appContext,
                    entries.map { if (it.id == entry.id) appliedEntry else it },
                )
            }
            ThemeLibraryOperationResult(
                success = true,
                entry = appliedEntry,
                packageResult = packageResult,
            )
        }.getOrElse { ThemeLibraryOperationResult(success = false, error = it) }
    }

fun renameThemeLibraryEntry(
    context: Context,
    entryId: String,
    name: String,
): ThemeLibraryOperationResult = synchronized(themeLibraryLock) {
    val appContext = context.applicationContext
    runCatching {
        val safeName = sanitizeThemeLibraryName(name)
        require(safeName.isNotBlank()) { "Theme name is required" }
        val entries = readThemeLibraryLocked(appContext)
        val current = entries.firstOrNull { it.id == entryId }
            ?: error("Saved theme was not found")
        val renamed = current.copy(name = safeName)
        writeThemeLibraryIndex(
            appContext,
            entries.map { if (it.id == entryId) renamed else it },
        )
        ThemeLibraryOperationResult(success = true, entry = renamed)
    }.getOrElse { ThemeLibraryOperationResult(success = false, error = it) }
}

fun deleteThemeLibraryEntry(context: Context, entryId: String): ThemeLibraryOperationResult =
    synchronized(themeLibraryLock) {
        val appContext = context.applicationContext
        runCatching {
            val entries = readThemeLibraryLocked(appContext)
            val entry = entries.firstOrNull { it.id == entryId }
                ?: error("Saved theme was not found")
            val root = themeLibraryDirectory(appContext)
            val packageFile = themeLibraryPackageFile(root, entry.id)
            val tombstone = File(root, ".${entry.id}.delete")
            tombstone.delete()
            if (packageFile.exists()) {
                require(packageFile.renameTo(tombstone)) { "Unable to stage saved theme deletion" }
            }
            try {
                writeThemeLibraryIndex(appContext, entries.filterNot { it.id == entryId })
            } catch (error: Throwable) {
                if (tombstone.exists()) {
                    require(tombstone.renameTo(packageFile)) { "Unable to restore saved theme" }
                }
                throw error
            }
            tombstone.delete()
            ThemeLibraryOperationResult(success = true, entry = entry)
        }.getOrElse { ThemeLibraryOperationResult(success = false, error = it) }
    }

fun exportThemeLibraryEntry(
    context: Context,
    entryId: String,
    destination: Uri,
): ThemeLibraryOperationResult = synchronized(themeLibraryLock) {
    val appContext = context.applicationContext
    runCatching {
        val entry = readThemeLibraryLocked(appContext).firstOrNull { it.id == entryId }
            ?: error("Saved theme was not found")
        val packageFile = themeLibraryPackageFile(themeLibraryDirectory(appContext), entry.id)
        require(packageFile.isFile) { "Saved theme package is missing" }
        FileInputStream(packageFile).use { input ->
            openThemeLibraryOutputStream(appContext, destination).use { output ->
                input.copyTo(output)
            }
        }
        ThemeLibraryOperationResult(success = true, entry = entry)
    }.getOrElse { ThemeLibraryOperationResult(success = false, error = it) }
}

internal fun sanitizeThemeLibraryName(name: String): String {
    return name
        .replace(Regex("[\\p{Cc}\\p{Cf}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
        .take(MAX_THEME_LIBRARY_NAME_LENGTH)
        .trim()
}

internal fun encodeThemeLibraryIndex(entries: List<ThemeLibraryEntry>): String {
    val items = JSONArray()
    entries.distinctBy(ThemeLibraryEntry::id).forEach { entry ->
        items.put(
            JSONObject()
                .put("id", entry.id)
                .put("name", entry.name)
                .put("createdAt", entry.createdAt)
                .put("updatedAt", entry.updatedAt)
                .put("lastAppliedAt", entry.lastAppliedAt)
                .put("sizeBytes", entry.sizeBytes)
        )
    }
    return JSONObject()
        .put("schema", THEME_LIBRARY_SCHEMA)
        .put("version", THEME_LIBRARY_VERSION)
        .put("items", items)
        .toString()
}

internal fun decodeThemeLibraryIndex(json: String): List<ThemeLibraryEntry> {
    val root = JSONObject(json)
    require(root.optString("schema") == THEME_LIBRARY_SCHEMA) { "Invalid theme library index" }
    require(root.optInt("version", 0) == THEME_LIBRARY_VERSION) {
        "Unsupported theme library index"
    }
    val items = root.optJSONArray("items") ?: JSONArray()
    return buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val id = item.optString("id")
            val name = sanitizeThemeLibraryName(item.optString("name"))
            if (!THEME_LIBRARY_ID_PATTERN.matches(id) || name.isBlank()) continue
            val createdAt = item.optLong("createdAt", 0L).coerceAtLeast(0L)
            val updatedAt = item.optLong("updatedAt", createdAt).coerceAtLeast(createdAt)
            val lastAppliedAt = item.optLong("lastAppliedAt", -1L).takeIf { it >= 0L }
            add(
                ThemeLibraryEntry(
                    id = id,
                    name = name,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    lastAppliedAt = lastAppliedAt,
                    sizeBytes = item.optLong("sizeBytes", 0L).coerceAtLeast(0L),
                )
            )
        }
    }.distinctBy(ThemeLibraryEntry::id)
}

private fun readThemeLibraryLocked(context: Context): List<ThemeLibraryEntry> {
    val root = themeLibraryDirectory(context)
    val indexed = runCatching {
        val atomicFile = themeLibraryIndexFile(root)
        if (!atomicFile.baseFile.isFile) emptyList() else {
            atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { reader ->
                decodeThemeLibraryIndex(reader.readText())
            }
        }
    }.getOrDefault(emptyList())
    val indexedById = indexed.associateBy(ThemeLibraryEntry::id)
    root.listFiles()
        .orEmpty()
        .filter { it.isFile && it.name.startsWith('.') && it.name.endsWith(".delete") }
        .forEach { tombstone ->
            val id = tombstone.name.removePrefix(".").removeSuffix(".delete")
            val packageFile = runCatching { themeLibraryPackageFile(root, id) }.getOrNull()
            if (id in indexedById && packageFile != null && !packageFile.exists()) {
                tombstone.renameTo(packageFile)
            } else {
                tombstone.delete()
            }
        }
    return root.listFiles()
        .orEmpty()
        .asSequence()
        .filter { it.isFile && it.extension.equals(THEME_STORE_FILE_EXTENSION, ignoreCase = true) }
        .mapNotNull { file ->
            val id = file.nameWithoutExtension
            if (!THEME_LIBRARY_ID_PATTERN.matches(id)) return@mapNotNull null
            indexedById[id]?.copy(sizeBytes = file.length()) ?: ThemeLibraryEntry(
                id = id,
                name = "Theme ${id.take(8)}",
                createdAt = file.lastModified().coerceAtLeast(0L),
                updatedAt = file.lastModified().coerceAtLeast(0L),
                lastAppliedAt = null,
                sizeBytes = file.length(),
            )
        }
        .sortedWith(
            compareByDescending<ThemeLibraryEntry> { it.lastAppliedAt ?: Long.MIN_VALUE }
                .thenByDescending(ThemeLibraryEntry::updatedAt)
        )
        .toList()
}

private fun writeThemeLibraryIndex(context: Context, entries: List<ThemeLibraryEntry>) {
    val root = themeLibraryDirectory(context)
    val atomicFile = themeLibraryIndexFile(root)
    val output = atomicFile.startWrite()
    try {
        output.write(encodeThemeLibraryIndex(entries).toByteArray(Charsets.UTF_8))
        output.flush()
        atomicFile.finishWrite(output)
    } catch (error: Throwable) {
        atomicFile.failWrite(output)
        throw error
    }
}

private fun themeLibraryDirectory(context: Context): File {
    return File(context.filesDir, "theme-library").apply { mkdirs() }
}

private fun themeLibraryIndexFile(root: File): AtomicFile {
    return AtomicFile(File(root, "index.json"))
}

private fun themeLibraryPackageFile(root: File, entryId: String): File {
    require(THEME_LIBRARY_ID_PATTERN.matches(entryId)) { "Invalid saved theme id" }
    return File(root, "$entryId.$THEME_STORE_FILE_EXTENSION")
}

private fun moveThemeLibraryFile(source: File, destination: File) {
    require(!destination.exists()) { "Saved theme already exists" }
    if (!source.renameTo(destination)) {
        source.copyTo(destination, overwrite = false)
        source.delete()
    }
}

private fun InputStream.copyThemeLibraryArchiveTo(output: OutputStream) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copied = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        copied += read
        require(copied <= MAX_THEME_LIBRARY_ARCHIVE_BYTES) { "Theme package is too large" }
        output.write(buffer, 0, read)
    }
}

private fun openThemeLibraryInputStream(context: Context, uri: Uri): InputStream {
    if (uri.scheme == "file") {
        return FileInputStream(File(uri.path ?: error("Invalid file URI")))
    }
    return context.contentResolver.openInputStream(uri) ?: error("Unable to open $uri")
}

private fun openThemeLibraryOutputStream(context: Context, uri: Uri): OutputStream {
    if (uri.scheme == "file") {
        val file = File(uri.path ?: error("Invalid file URI"))
        file.parentFile?.mkdirs()
        return FileOutputStream(file)
    }
    return context.contentResolver.openOutputStream(uri) ?: error("Unable to open $uri")
}

private fun queryThemeLibraryDisplayName(context: Context, uri: Uri): String? {
    if (uri.scheme == "file") return uri.lastPathSegment
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index < 0) null else cursor.getString(index)
        }
    }.getOrNull()
}
