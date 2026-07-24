package me.weishu.kernelsu.ui.component.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import me.weishu.kernelsu.ui.component.SWITCH_STYLE_KEY
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.decoration.UI_DECORATION_CONFIG_KEY
import me.weishu.kernelsu.ui.component.decoration.UiCardDecoration
import me.weishu.kernelsu.ui.component.decoration.UiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.UiNavigationDecoration
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

data class StoredComponentImage(
    val uriString: String,
    val sha256: String,
    val mimeType: String,
)

@SuppressLint("UseKtx") // Synchronous commits are required so callers can report persistence failures.
class ComponentStyleStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE)

    fun readCardStyles(): List<CustomCardStyle> = synchronized(componentStyleLock) {
        runCatching {
            decodeCardStyleLibrary(prefs.getString(CUSTOM_CARD_STYLE_LIBRARY_KEY, null))
        }.getOrDefault(emptyList())
    }

    fun readSwitchStyles(): List<CustomSwitchStyle> = synchronized(componentStyleLock) {
        runCatching {
            decodeSwitchStyleLibrary(prefs.getString(CUSTOM_SWITCH_STYLE_LIBRARY_KEY, null))
        }.getOrDefault(emptyList())
    }

    fun readActiveCardStyle(): CustomCardStyle? {
        val activeId = prefs.getString(CUSTOM_CARD_STYLE_ACTIVE_ID_KEY, null) ?: return null
        return readCardStyles().firstOrNull { it.id == activeId }
    }

    fun readActiveSwitchStyle(): CustomSwitchStyle? {
        val activeId = prefs.getString(CUSTOM_SWITCH_STYLE_ACTIVE_ID_KEY, null) ?: return null
        return readSwitchStyles().firstOrNull { it.id == activeId }
    }

    fun saveCardStyle(style: CustomCardStyle, apply: Boolean): Boolean = synchronized(componentStyleLock) {
        val normalized = style.normalized().copy(updatedAt = System.currentTimeMillis())
        val current = readCardStyles()
        if (current.size >= MAX_SAVED_COMPONENT_STYLES && current.none { it.id == normalized.id }) {
            return@synchronized false
        }
        val styles = upsertCardStyle(current, normalized)
        val editor = prefs.edit()
            .putString(CUSTOM_CARD_STYLE_LIBRARY_KEY, encodeCardStyleLibrary(styles))
        if (apply) {
            val currentConfig = UiDecorationConfig.fromJsonString(
                prefs.getString(UI_DECORATION_CONFIG_KEY, null)
            )
            editor
                .putString(CUSTOM_CARD_STYLE_ACTIVE_ID_KEY, normalized.id)
                .putString(
                    UI_DECORATION_CONFIG_KEY,
                    currentConfig.copy(
                        enabled = true,
                        card = UiCardDecoration.Custom,
                        navigation = UiNavigationDecoration.Custom,
                    ).normalized().toJsonString(),
                )
        }
        editor.commit()
    }

    fun saveSwitchStyle(style: CustomSwitchStyle, apply: Boolean): Boolean = synchronized(componentStyleLock) {
        val normalized = style.normalized().copy(updatedAt = System.currentTimeMillis())
        val current = readSwitchStyles()
        if (current.size >= MAX_SAVED_COMPONENT_STYLES && current.none { it.id == normalized.id }) {
            return@synchronized false
        }
        val styles = upsertSwitchStyle(current, normalized)
        val editor = prefs.edit()
            .putString(CUSTOM_SWITCH_STYLE_LIBRARY_KEY, encodeSwitchStyleLibrary(styles))
        if (apply) {
            editor
                .putString(CUSTOM_SWITCH_STYLE_ACTIVE_ID_KEY, normalized.id)
                .putString(SWITCH_STYLE_KEY, SwitchStyle.Custom.value)
        }
        editor.commit().also { committed ->
            if (committed) cleanupReplacedImages(current, styles)
        }
    }

    fun deleteCardStyle(styleId: String): Boolean = synchronized(componentStyleLock) {
        val current = readCardStyles()
        val updated = current.filterNot { it.id == styleId }
        if (updated.size == current.size) return@synchronized false
        val editor = prefs.edit()
            .putString(CUSTOM_CARD_STYLE_LIBRARY_KEY, encodeCardStyleLibrary(updated))
        if (prefs.getString(CUSTOM_CARD_STYLE_ACTIVE_ID_KEY, null) == styleId) {
            val config = UiDecorationConfig.fromJsonString(
                prefs.getString(UI_DECORATION_CONFIG_KEY, null)
            )
            editor
                .remove(CUSTOM_CARD_STYLE_ACTIVE_ID_KEY)
                .putString(
                    UI_DECORATION_CONFIG_KEY,
                    config.copy(
                        card = UiCardDecoration.Highlight,
                        navigation = UiNavigationDecoration.UnderGlow,
                    ).toJsonString(),
                )
        }
        editor.commit()
    }

    fun deleteSwitchStyle(styleId: String): Boolean = synchronized(componentStyleLock) {
        val current = readSwitchStyles()
        val updated = current.filterNot { it.id == styleId }
        if (updated.size == current.size) return@synchronized false
        val editor = prefs.edit()
            .putString(CUSTOM_SWITCH_STYLE_LIBRARY_KEY, encodeSwitchStyleLibrary(updated))
        if (prefs.getString(CUSTOM_SWITCH_STYLE_ACTIVE_ID_KEY, null) == styleId) {
            editor
                .remove(CUSTOM_SWITCH_STYLE_ACTIVE_ID_KEY)
                .putString(SWITCH_STYLE_KEY, SwitchStyle.DEFAULT_VALUE)
        }
        if (!editor.commit()) return@synchronized false
        cleanupReplacedImages(current, updated)
        true
    }

    private fun cleanupReplacedImages(
        previous: List<CustomSwitchStyle>,
        current: List<CustomSwitchStyle>,
    ) {
        runCatching {
            val referenced = current.mapNotNull { style ->
                resolveImageFile(style.imageUri)?.canonicalPath
            }.toSet()
            previous.mapNotNull { style -> resolveImageFile(style.imageUri) }
                .distinctBy(File::getCanonicalPath)
                .filterNot { it.canonicalPath in referenced }
                .forEach(File::delete)
        }
    }

    fun persistSwitchImage(source: Uri): StoredComponentImage = synchronized(componentStyleLock) {
        val staging = File.createTempFile("component-image-", ".tmp", imageDirectory())
        try {
            val declaredMimeType = runCatching { appContext.contentResolver.getType(source) }
                .getOrNull()
                ?.lowercase()
                ?.takeIf { it.startsWith("image/") }
                ?: "image/*"
            val digest = MessageDigest.getInstance("SHA-256")
            var size = 0L
            openInput(source).use { input ->
                FileOutputStream(staging).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        require(size <= MAX_COMPONENT_IMAGE_BYTES) { "Component image is too large" }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
            require(size > 0L) { "Component image is empty" }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(staging.absolutePath, bounds)
            require(bounds.outWidth in 1..MAX_COMPONENT_IMAGE_SIDE) { "Component image width is invalid" }
            require(bounds.outHeight in 1..MAX_COMPONENT_IMAGE_SIDE) { "Component image height is invalid" }
            require(bounds.outWidth.toLong() * bounds.outHeight <= MAX_COMPONENT_IMAGE_PIXELS) {
                "Component image has too many pixels"
            }
            val mimeType = bounds.outMimeType
                ?.lowercase()
                ?.takeIf { it.startsWith("image/") }
                ?: declaredMimeType
            val sha256 = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            val extension = when {
                mimeType.contains("png") -> ".png"
                mimeType.contains("webp") -> ".webp"
                mimeType.contains("gif") -> ".gif"
                mimeType.contains("heif") || mimeType.contains("heic") -> ".heic"
                mimeType.contains("avif") -> ".avif"
                else -> ".jpg"
            }
            val destination = File(imageDirectory(), "$sha256$extension")
            if (!destination.isFile) {
                require(staging.renameTo(destination)) { "Unable to store component image" }
            }
            StoredComponentImage(
                uriString = Uri.fromFile(destination).toString(),
                sha256 = sha256,
                mimeType = mimeType,
            )
        } finally {
            staging.delete()
        }
    }

    internal fun cleanupReplacedSwitchImages(
        previous: List<CustomSwitchStyle>,
        current: List<CustomSwitchStyle>,
    ) = synchronized(componentStyleLock) {
        cleanupReplacedImages(previous, current)
    }

    fun resolveImageFile(uriString: String?): File? {
        val uri = uriString?.let(Uri::parse) ?: return null
        if (uri.scheme != "file") return null
        val candidate = runCatching { File(requireNotNull(uri.path)).canonicalFile }.getOrNull() ?: return null
        val root = runCatching { imageDirectory().canonicalFile }.getOrNull() ?: return null
        return candidate.takeIf { it.isFile && it.toPath().startsWith(root.toPath()) }
    }

    fun discardSwitchImageIfUnreferenced(uriString: String?): Boolean = synchronized(componentStyleLock) {
        val candidate = resolveImageFile(uriString) ?: return@synchronized false
        val candidatePath = runCatching { candidate.canonicalPath }.getOrNull()
            ?: return@synchronized false
        val referenced = readSwitchStyles().any { style ->
            resolveImageFile(style.imageUri)?.let { file ->
                runCatching { file.canonicalPath == candidatePath }.getOrDefault(false)
            } == true
        }
        !referenced && candidate.delete()
    }

    private fun openInput(uri: Uri): InputStream {
        return when (uri.scheme) {
            "file" -> FileInputStream(requireNotNull(uri.path))
            else -> appContext.contentResolver.openInputStream(uri)
                ?: error("Unable to open component image")
        }
    }

    private fun imageDirectory(): File = File(appContext.filesDir, COMPONENT_IMAGE_DIRECTORY).apply {
        require(isDirectory || mkdirs()) { "Unable to create component image directory" }
    }

    companion object {
        internal fun upsertCardStyle(
            current: List<CustomCardStyle>,
            style: CustomCardStyle,
        ): List<CustomCardStyle> = listOf(style) + current.filterNot { it.id == style.id }
            .take(MAX_SAVED_COMPONENT_STYLES - 1)

        internal fun upsertSwitchStyle(
            current: List<CustomSwitchStyle>,
            style: CustomSwitchStyle,
        ): List<CustomSwitchStyle> = listOf(style) + current.filterNot { it.id == style.id }
            .take(MAX_SAVED_COMPONENT_STYLES - 1)
    }
}

private const val SETTINGS_PREFERENCES = "settings"
private const val COMPONENT_IMAGE_DIRECTORY = "component-styles/images"
private val componentStyleLock = Any()
