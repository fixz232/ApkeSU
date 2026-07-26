package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.Locale

const val APP_FONT_PRESET_KEY = "app_font_preset"
const val APP_FONT_CUSTOM_ID_KEY = "app_font_custom_id"
const val APP_FONT_CUSTOM_NAME_KEY = "app_font_custom_name"
const val APP_FONT_CUSTOM_SHA256_KEY = "app_font_custom_sha256"
const val APP_FONT_CUSTOM_SIZE_KEY = "app_font_custom_size"
const val APP_FONT_SAVED_RECORDS_KEY = "app_font_saved_records"
const val APP_FONT_OPACITY_KEY = "app_font_opacity"
const val MAX_CUSTOM_APP_FONT_BYTES = 32L * 1024L * 1024L
const val MIN_APP_FONT_OPACITY = 0.45f
const val MAX_APP_FONT_OPACITY = 1f

internal const val APP_FONT_CUSTOM_FILE_NAME = "font_custom.ttf"

val APP_FONT_PREFERENCE_KEYS = setOf(
    APP_FONT_PRESET_KEY,
    APP_FONT_CUSTOM_ID_KEY,
    APP_FONT_CUSTOM_NAME_KEY,
    APP_FONT_CUSTOM_SHA256_KEY,
    APP_FONT_CUSTOM_SIZE_KEY,
    APP_FONT_SAVED_RECORDS_KEY,
    APP_FONT_OPACITY_KEY,
)

enum class AppFontPreset(val value: String) {
    System("system"),
    SansSerif("sans_serif"),
    Serif("serif"),
    Monospace("monospace"),
    Cursive("cursive"),
    Custom("custom");

    companion object {
        val DEFAULT = System

        fun fromValue(value: String?): AppFontPreset {
            return entries.firstOrNull { it.value == value } ?: DEFAULT
        }
    }
}

@Immutable
data class SavedAppFont(
    val id: String,
    val displayName: String,
    val sha256: String,
    val sizeBytes: Long,
)

@Immutable
data class AppFontState(
    val preset: AppFontPreset = AppFontPreset.DEFAULT,
    val customId: String? = null,
    val customDisplayName: String? = null,
    val customSha256: String? = null,
    val customSizeBytes: Long = 0L,
    val customFileAvailable: Boolean = false,
    val savedFonts: List<SavedAppFont> = emptyList(),
    val opacity: Float = MAX_APP_FONT_OPACITY,
) {
    val isCustomActive: Boolean
        get() = preset == AppFontPreset.Custom && customFileAvailable

    companion object {
        val Default = AppFontState()
    }
}

internal data class AppFontFileMetadata(
    val sha256: String,
    val sizeBytes: Long,
)

fun readAppFontState(context: Context): AppFontState {
    val appContext = context.applicationContext
    val prefs = appFontPrefs(appContext)
    val file = appFontFile(appContext)
    val savedFonts = readSavedAppFonts(appContext, prefs)
    val customId = prefs.getString(APP_FONT_CUSTOM_ID_KEY, null)
        ?.takeIf(APP_FONT_ID::matches)
        ?.takeIf { id -> savedFonts.any { it.id == id } }
    return AppFontState(
        preset = AppFontPreset.fromValue(prefs.getString(APP_FONT_PRESET_KEY, null)),
        customId = customId,
        customDisplayName = sanitizeAppFontDisplayName(
            prefs.getString(APP_FONT_CUSTOM_NAME_KEY, null)
        ) ?: savedFonts.firstOrNull { it.id == customId }?.displayName,
        customSha256 = prefs.getString(APP_FONT_CUSTOM_SHA256_KEY, null)
            ?.lowercase(Locale.ROOT)
            ?.takeIf(APP_FONT_SHA256::matches)
            ?: savedFonts.firstOrNull { it.id == customId }?.sha256,
        customSizeBytes = prefs.getLong(APP_FONT_CUSTOM_SIZE_KEY, 0L)
            .takeIf { it > 0L }
            ?: savedFonts.firstOrNull { it.id == customId }?.sizeBytes
            ?: file.takeIf(File::isFile)?.length().orZero(),
        customFileAvailable = file.isFile &&
            file.length() in 1..MAX_CUSTOM_APP_FONT_BYTES &&
            hasSupportedTtfSignature(file),
        savedFonts = savedFonts,
        opacity = prefs.getFloat(APP_FONT_OPACITY_KEY, MAX_APP_FONT_OPACITY)
            .coerceIn(MIN_APP_FONT_OPACITY, MAX_APP_FONT_OPACITY),
    )
}

fun setAppFontPreset(context: Context, preset: AppFontPreset): Boolean {
    val appContext = context.applicationContext
    val current = readAppFontState(appContext)
    val next = if (preset == AppFontPreset.Custom) {
        val metadata = validateAppFontFile(appFontFile(appContext))
        current.copy(
            preset = preset,
            customSha256 = metadata.sha256,
            customSizeBytes = metadata.sizeBytes,
            customFileAvailable = true,
        )
    } else {
        current.copy(preset = preset)
    }
    return appFontPrefs(appContext).edit().putAppFontState(next).commit()
}

fun setAppFontOpacity(context: Context, opacity: Float): AppFontState {
    val appContext = context.applicationContext
    val next = readAppFontState(appContext).copy(
        opacity = opacity.coerceIn(MIN_APP_FONT_OPACITY, MAX_APP_FONT_OPACITY),
    )
    check(appFontPrefs(appContext).edit().putAppFontState(next).commit()) {
        "Unable to save font opacity"
    }
    return next
}

fun importCustomAppFont(context: Context, uri: Uri): Result<AppFontState> = runCatching {
    val appContext = context.applicationContext
    val displayName = queryAppFontDisplayName(appContext, uri)
        ?: uri.lastPathSegment
        ?: "custom-font.ttf"
    val safeDisplayName = sanitizeAppFontDisplayName(displayName)
        ?: error("Font file name is invalid")
    require(safeDisplayName.endsWith(".ttf", ignoreCase = true)) {
        "Only TTF font files are supported"
    }

    val declaredSize = queryAppFontSize(appContext, uri)
    require(declaredSize == null || declaredSize in 0..MAX_CUSTOM_APP_FONT_BYTES) {
        "Font file is larger than 32 MiB"
    }

    val target = appFontFile(appContext)
    val parent = requireNotNull(target.parentFile)
    require(parent.exists() || parent.mkdirs()) { "Unable to create font directory" }
    val temporary = File(parent, "$APP_FONT_CUSTOM_FILE_NAME.importing")
    val backup = File(parent, "$APP_FONT_CUSTOM_FILE_NAME.backup")
    temporary.delete()
    backup.delete()

    try {
        openAppFontInputStream(appContext, uri).use { input ->
            FileOutputStream(temporary).use { output ->
                input.copyAppFontTo(output)
            }
        }
        val metadata = validateAppFontFile(temporary)
        val fontId = createAppFontId()
        val savedTarget = savedAppFontFile(appContext, fontId)
        val savedParent = requireNotNull(savedTarget.parentFile)
        require(savedParent.exists() || savedParent.mkdirs()) { "Unable to create font library" }
        temporary.copyTo(savedTarget, overwrite = false)
        val savedFont = SavedAppFont(
            id = fontId,
            displayName = safeDisplayName,
            sha256 = metadata.sha256,
            sizeBytes = metadata.sizeBytes,
        )
        val savedFonts = (readSavedAppFonts(appContext) + savedFont)
            .distinctBy { it.id }
            .takeLast(MAX_SAVED_APP_FONTS)
        val next = AppFontState(
            preset = AppFontPreset.Custom,
            customId = fontId,
            customDisplayName = safeDisplayName,
            customSha256 = metadata.sha256,
            customSizeBytes = metadata.sizeBytes,
            customFileAvailable = true,
            savedFonts = savedFonts,
        )

        val hadPrevious = target.isFile
        if (hadPrevious) {
            require(target.renameTo(backup)) { "Unable to back up the current font" }
        }
        if (!temporary.renameTo(target)) {
            if (hadPrevious) backup.renameTo(target)
            error("Unable to install the selected font")
        }

        var committed = false
        try {
            committed = appFontPrefs(appContext).edit()
                .putAppFontState(next)
                .putSavedAppFonts(savedFonts)
                .commit()
            require(committed) { "Unable to save font settings" }
        } finally {
            if (!committed) {
                target.delete()
                savedTarget.delete()
                if (hadPrevious) backup.renameTo(target)
            }
        }
        backup.delete()
        next
    } finally {
        temporary.delete()
    }
}

fun selectSavedAppFont(context: Context, id: String): Result<AppFontState> = runCatching {
    val appContext = context.applicationContext
    require(APP_FONT_ID.matches(id)) { "Invalid font id" }
    val savedFonts = readSavedAppFonts(appContext)
    val savedFont = savedFonts.firstOrNull { it.id == id } ?: error("Saved font is missing")
    val source = savedAppFontFile(appContext, savedFont.id)
    validateAppFontFile(
        source,
        expectedSha256 = savedFont.sha256,
        expectedSizeBytes = savedFont.sizeBytes,
    )
    val target = appFontFile(appContext)
    val parent = requireNotNull(target.parentFile)
    require(parent.exists() || parent.mkdirs()) { "Unable to create font directory" }
    val temporary = File(parent, "$APP_FONT_CUSTOM_FILE_NAME.selecting")
    val backup = File(parent, "$APP_FONT_CUSTOM_FILE_NAME.backup")
    temporary.delete()
    backup.delete()
    source.copyTo(temporary, overwrite = true)
    validateAppFontFile(temporary, savedFont.sha256, savedFont.sizeBytes)
    val hadPrevious = target.isFile
    if (hadPrevious) {
        require(target.renameTo(backup)) { "Unable to back up the current font" }
    }
    if (!temporary.renameTo(target)) {
        if (hadPrevious) backup.renameTo(target)
        error("Unable to activate the selected font")
    }
    val next = AppFontState(
        preset = AppFontPreset.Custom,
        customId = savedFont.id,
        customDisplayName = savedFont.displayName,
        customSha256 = savedFont.sha256,
        customSizeBytes = savedFont.sizeBytes,
        customFileAvailable = true,
        savedFonts = savedFonts,
    )
    var committed = false
    try {
        committed = appFontPrefs(appContext).edit().putAppFontState(next).commit()
        require(committed) { "Unable to save font settings" }
    } finally {
        if (!committed) {
            target.delete()
            if (hadPrevious) backup.renameTo(target)
        }
        temporary.delete()
    }
    backup.delete()
    next
}

fun deleteSavedAppFont(context: Context, id: String): Result<AppFontState> = runCatching {
    val appContext = context.applicationContext
    require(APP_FONT_ID.matches(id)) { "Invalid font id" }
    val current = readAppFontState(appContext)
    val savedFonts = current.savedFonts.filterNot { it.id == id }
    val deletingCurrent = current.customId == id
    val next = if (deletingCurrent) {
        AppFontState.Default.copy(
            savedFonts = savedFonts,
            opacity = current.opacity,
        )
    } else {
        current.copy(savedFonts = savedFonts)
    }
    val committed = appFontPrefs(appContext).edit()
        .putAppFontState(next)
        .putSavedAppFonts(savedFonts)
        .commit()
    require(committed) { "Unable to delete font settings" }
    savedAppFontFile(appContext, id).delete()
    if (deletingCurrent) {
        appFontFile(appContext).delete()
    }
    readAppFontState(appContext)
}

fun removeCustomAppFont(context: Context): Boolean {
    val appContext = context.applicationContext
    val target = appFontFile(appContext)
    val backup = File(target.parentFile, "$APP_FONT_CUSTOM_FILE_NAME.removing")
    backup.delete()
    val hadFont = target.isFile
    if (hadFont && !target.renameTo(backup)) return false

    val next = readAppFontState(appContext).copy(
        preset = AppFontPreset.System,
        customId = null,
        customDisplayName = null,
        customSha256 = null,
        customSizeBytes = 0L,
        customFileAvailable = false,
    )
    val committed = appFontPrefs(appContext).edit().putAppFontState(next).commit()
    if (!committed) {
        if (hadFont) backup.renameTo(target)
        return false
    }
    backup.delete()
    return true
}

fun resolveAppFontFamily(context: Context, state: AppFontState): FontFamily {
    return when (state.preset) {
        AppFontPreset.System -> FontFamily.Default
        AppFontPreset.SansSerif -> FontFamily.SansSerif
        AppFontPreset.Serif -> FontFamily.Serif
        AppFontPreset.Monospace -> FontFamily.Monospace
        AppFontPreset.Cursive -> FontFamily.Cursive
        AppFontPreset.Custom -> runCatching {
            val file = appFontFile(context.applicationContext)
            require(file.isFile && hasSupportedTtfSignature(file))
            FontFamily(Font(file))
        }.getOrDefault(FontFamily.Default)
    }
}

internal fun appFontFile(context: Context): File {
    return File(context.filesDir, "theme-store/current/assets/$APP_FONT_CUSTOM_FILE_NAME")
}

internal fun savedAppFontFile(context: Context, id: String): File {
    return File(context.filesDir, "theme-store/fonts/$id.ttf")
}

internal fun validateAppFontFile(
    file: File,
    expectedSha256: String? = null,
    expectedSizeBytes: Long? = null,
    validateTypeface: Boolean = true,
): AppFontFileMetadata {
    require(file.isFile) { "Font file is missing" }
    val size = file.length()
    require(size in 1..MAX_CUSTOM_APP_FONT_BYTES) { "Font file is empty or larger than 32 MiB" }
    if (expectedSizeBytes != null) {
        require(size == expectedSizeBytes) { "Font file size does not match" }
    }
    require(hasSupportedTtfSignature(file)) { "File is not a supported TTF font" }
    if (validateTypeface) {
        Typeface.createFromFile(file)
    }
    val sha256 = appFontSha256(file)
    if (expectedSha256 != null) {
        require(sha256.equals(expectedSha256, ignoreCase = true)) {
            "Font file checksum does not match"
        }
    }
    return AppFontFileMetadata(sha256 = sha256, sizeBytes = size)
}

internal fun hasSupportedTtfSignature(file: File): Boolean {
    if (!file.isFile || file.length() < 4L) return false
    val header = ByteArray(4)
    return FileInputStream(file).use { input ->
        input.read(header) == header.size && hasSupportedTtfSignature(header)
    }
}

internal fun hasSupportedTtfSignature(header: ByteArray): Boolean {
    if (header.size < 4) return false
    val standard = header[0] == 0.toByte() &&
        header[1] == 1.toByte() &&
        header[2] == 0.toByte() &&
        header[3] == 0.toByte()
    val legacyTrueType = header[0] == 't'.code.toByte() &&
        header[1] == 'r'.code.toByte() &&
        header[2] == 'u'.code.toByte() &&
        header[3] == 'e'.code.toByte()
    return standard || legacyTrueType
}

internal fun sanitizeAppFontDisplayName(value: String?): String? {
    return value
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.filterNot(Char::isISOControl)
        ?.trim()
        ?.take(96)
        ?.takeIf(String::isNotBlank)
}

internal fun SharedPreferences.Editor.putAppFontState(state: AppFontState): SharedPreferences.Editor {
    putString(APP_FONT_PRESET_KEY, state.preset.value)
    putOptionalAppFontString(APP_FONT_CUSTOM_ID_KEY, state.customId)
    putOptionalAppFontString(APP_FONT_CUSTOM_NAME_KEY, state.customDisplayName)
    putOptionalAppFontString(APP_FONT_CUSTOM_SHA256_KEY, state.customSha256)
    if (state.customSizeBytes > 0L) {
        putLong(APP_FONT_CUSTOM_SIZE_KEY, state.customSizeBytes)
    } else {
        remove(APP_FONT_CUSTOM_SIZE_KEY)
    }
    putFloat(
        APP_FONT_OPACITY_KEY,
        state.opacity.coerceIn(MIN_APP_FONT_OPACITY, MAX_APP_FONT_OPACITY),
    )
    return this
}

internal fun SharedPreferences.Editor.putSavedAppFonts(
    fonts: List<SavedAppFont>,
): SharedPreferences.Editor {
    val encoded = fonts
        .filter { APP_FONT_ID.matches(it.id) && APP_FONT_SHA256.matches(it.sha256) && it.sizeBytes > 0L }
        .takeLast(MAX_SAVED_APP_FONTS)
        .joinToString("\n") { font ->
            listOf(
                font.id,
                encodeAppFontField(font.displayName),
                font.sha256.lowercase(Locale.ROOT),
                font.sizeBytes.toString(),
            ).joinToString("|")
        }
    return if (encoded.isBlank()) remove(APP_FONT_SAVED_RECORDS_KEY) else putString(APP_FONT_SAVED_RECORDS_KEY, encoded)
}

internal fun readSavedAppFonts(context: Context): List<SavedAppFont> {
    return readSavedAppFonts(context, appFontPrefs(context))
}

internal fun appFontSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}

private fun appFontPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
}

private fun readSavedAppFonts(
    context: Context,
    prefs: SharedPreferences,
): List<SavedAppFont> {
    return prefs.getString(APP_FONT_SAVED_RECORDS_KEY, null)
        .orEmpty()
        .lineSequence()
        .mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size != 4) return@mapNotNull null
            val id = parts[0].takeIf(APP_FONT_ID::matches) ?: return@mapNotNull null
            val displayName = sanitizeAppFontDisplayName(decodeAppFontField(parts[1]))
                ?: return@mapNotNull null
            val sha256 = parts[2].lowercase(Locale.ROOT).takeIf(APP_FONT_SHA256::matches)
                ?: return@mapNotNull null
            val sizeBytes = parts[3].toLongOrNull()?.takeIf { it in 1..MAX_CUSTOM_APP_FONT_BYTES }
                ?: return@mapNotNull null
            val file = savedAppFontFile(context, id)
            if (!file.isFile || file.length() != sizeBytes || !hasSupportedTtfSignature(file)) {
                return@mapNotNull null
            }
            SavedAppFont(
                id = id,
                displayName = displayName,
                sha256 = sha256,
                sizeBytes = sizeBytes,
            )
        }
        .distinctBy { it.id }
        .toList()
        .takeLast(MAX_SAVED_APP_FONTS)
}

private fun createAppFontId(): String {
    return UUID.randomUUID().toString().replace("-", "")
}

private fun encodeAppFontField(value: String): String {
    return java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}

private fun decodeAppFontField(value: String): String {
    return runCatching {
        java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
    }.getOrDefault("")
}

private fun SharedPreferences.Editor.putOptionalAppFontString(
    key: String,
    value: String?,
): SharedPreferences.Editor {
    return if (value.isNullOrBlank()) remove(key) else putString(key, value)
}

private fun queryAppFontDisplayName(context: Context, uri: Uri): String? {
    return queryAppFontColumn(context, uri, OpenableColumns.DISPLAY_NAME) { cursor, index ->
        cursor.getString(index)
    }
}

private fun queryAppFontSize(context: Context, uri: Uri): Long? {
    return queryAppFontColumn(context, uri, OpenableColumns.SIZE) { cursor, index ->
        cursor.getLong(index).takeIf { it >= 0L }
    }
}

private fun <T> queryAppFontColumn(
    context: Context,
    uri: Uri,
    column: String,
    read: (android.database.Cursor, Int) -> T?,
): T? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(column)
            if (index < 0 || cursor.isNull(index)) null else read(cursor, index)
        }
    }.getOrNull()
}

private fun openAppFontInputStream(context: Context, uri: Uri) = if (uri.scheme == "file") {
    FileInputStream(File(requireNotNull(uri.path) { "Invalid font file URI" }))
} else {
    context.contentResolver.openInputStream(uri) ?: error("Unable to open the selected font")
}

private fun java.io.InputStream.copyAppFontTo(output: FileOutputStream): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copied = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        copied += count
        require(copied <= MAX_CUSTOM_APP_FONT_BYTES) { "Font file is larger than 32 MiB" }
        output.write(buffer, 0, count)
    }
    require(copied > 0L) { "Font file is empty" }
    return copied
}

private fun Long?.orZero(): Long = this ?: 0L

private val APP_FONT_SHA256 = Regex("[a-f0-9]{64}")
private val APP_FONT_ID = Regex("[a-f0-9]{32}")
private const val MAX_SAVED_APP_FONTS = 12
