package me.weishu.kernelsu.ui.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private const val THEME_AUTHOR_PREFS = "theme_author_profile"
private const val THEME_AUTHOR_DISPLAY_NAME_KEY = "display_name"
private const val THEME_AUTHOR_REAL_NAME_KEY = "real_name"
private const val THEME_AUTHOR_GENDER_KEY = "gender"
private const val THEME_AUTHOR_BIO_KEY = "bio"
private const val THEME_AUTHOR_AVATAR_URI_KEY = "avatar_uri"
private const val MAX_THEME_AUTHOR_NAME_LENGTH = 32
private const val MAX_THEME_AUTHOR_BIO_LENGTH = 160
private const val MAX_THEME_AUTHOR_AVATAR_BYTES = 16L * 1024L * 1024L
private const val MAX_THEME_AUTHOR_AVATAR_SIDE = 16_384

enum class ThemeAuthorGender(val storageValue: String) {
    Unspecified("unspecified"),
    Male("male"),
    Female("female"),
    Other("other");

    companion object {
        fun fromStorageValue(value: String?): ThemeAuthorGender {
            return entries.firstOrNull { it.storageValue == value } ?: Unspecified
        }
    }
}

data class ThemeAuthorProfile(
    val displayName: String = "",
    val realName: String = "",
    val gender: ThemeAuthorGender = ThemeAuthorGender.Unspecified,
    val bio: String = "",
    val avatarUriString: String? = null,
) {
    val isConfigured: Boolean
        get() = displayName.isNotBlank() ||
            realName.isNotBlank() ||
            gender != ThemeAuthorGender.Unspecified ||
            bio.isNotBlank() ||
            !avatarUriString.isNullOrBlank()
}

data class ThemeAuthorProfileSaveResult(
    val success: Boolean,
    val profile: ThemeAuthorProfile? = null,
    val error: Throwable? = null,
)

fun readThemeAuthorProfile(context: Context): ThemeAuthorProfile {
    val prefs = context.applicationContext.getSharedPreferences(THEME_AUTHOR_PREFS, Context.MODE_PRIVATE)
    return sanitizeThemeAuthorProfile(
        ThemeAuthorProfile(
            displayName = prefs.getString(THEME_AUTHOR_DISPLAY_NAME_KEY, null).orEmpty(),
            realName = prefs.getString(THEME_AUTHOR_REAL_NAME_KEY, null).orEmpty(),
            gender = ThemeAuthorGender.fromStorageValue(
                prefs.getString(THEME_AUTHOR_GENDER_KEY, null)
            ),
            bio = prefs.getString(THEME_AUTHOR_BIO_KEY, null).orEmpty(),
            avatarUriString = prefs.getString(THEME_AUTHOR_AVATAR_URI_KEY, null),
        )
    )
}

fun saveThemeAuthorProfile(
    context: Context,
    profile: ThemeAuthorProfile,
    avatarSource: Uri? = null,
    removeAvatar: Boolean = false,
): ThemeAuthorProfileSaveResult {
    val appContext = context.applicationContext
    val previous = readThemeAuthorProfile(appContext)
    var stagedAvatarUri: String? = null
    return runCatching {
        val sanitized = sanitizeThemeAuthorProfile(profile)
        val avatarUri = when {
            removeAvatar -> null
            avatarSource != null -> copyThemeAuthorAvatar(appContext, avatarSource).also {
                stagedAvatarUri = it
            }
            else -> previous.avatarUriString
        }
        val saved = sanitized.copy(avatarUriString = avatarUri)
        val prefs = appContext.getSharedPreferences(THEME_AUTHOR_PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit().apply {
            putString(THEME_AUTHOR_DISPLAY_NAME_KEY, saved.displayName)
            putString(THEME_AUTHOR_REAL_NAME_KEY, saved.realName)
            putString(THEME_AUTHOR_GENDER_KEY, saved.gender.storageValue)
            putString(THEME_AUTHOR_BIO_KEY, saved.bio)
            if (avatarUri.isNullOrBlank()) {
                remove(THEME_AUTHOR_AVATAR_URI_KEY)
            } else {
                putString(THEME_AUTHOR_AVATAR_URI_KEY, avatarUri)
            }
        }
        val committed = editor.commit()
        check(committed) { "Unable to save author profile" }
        if (previous.avatarUriString != avatarUri) {
            deleteThemeAuthorAvatar(appContext, previous.avatarUriString)
        }
        ThemeAuthorProfileSaveResult(success = true, profile = saved)
    }.getOrElse { error ->
        deleteThemeAuthorAvatar(appContext, stagedAvatarUri)
        ThemeAuthorProfileSaveResult(success = false, profile = previous, error = error)
    }
}

internal fun sanitizeThemeAuthorProfile(profile: ThemeAuthorProfile): ThemeAuthorProfile {
    return profile.copy(
        displayName = sanitizeThemeAuthorSingleLine(profile.displayName),
        realName = sanitizeThemeAuthorSingleLine(profile.realName),
        bio = sanitizeThemeAuthorBio(profile.bio),
        avatarUriString = profile.avatarUriString?.trim()?.takeIf(String::isNotBlank),
    )
}

private fun sanitizeThemeAuthorSingleLine(value: String): String {
    return value
        .filterNot(Char::isISOControl)
        .trim()
        .replace(Regex("\\s+"), " ")
        .take(MAX_THEME_AUTHOR_NAME_LENGTH)
        .trim()
}

private fun sanitizeThemeAuthorBio(value: String): String {
    val normalized = value
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .filter { it == '\n' || !it.isISOControl() }
        .lineSequence()
        .take(4)
        .joinToString("\n") { it.trimEnd() }
        .trim()
    return normalized.take(MAX_THEME_AUTHOR_BIO_LENGTH).trim()
}

private fun copyThemeAuthorAvatar(context: Context, source: Uri): String {
    val avatarDir = themeAuthorAvatarDirectory(context)
    val temporary = File(avatarDir, ".avatar-${UUID.randomUUID()}.tmp")
    val target = File(avatarDir, "avatar-${UUID.randomUUID()}.image")
    try {
        context.contentResolver.openInputStream(source)?.use { input ->
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    copied += read
                    require(copied <= MAX_THEME_AUTHOR_AVATAR_BYTES) {
                        "Author avatar is too large"
                    }
                    output.write(buffer, 0, read)
                }
                require(copied > 0L) { "Author avatar is empty" }
            }
        } ?: error("Unable to open author avatar")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(temporary.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Selected file is not an image" }
        require(bounds.outWidth <= MAX_THEME_AUTHOR_AVATAR_SIDE && bounds.outHeight <= MAX_THEME_AUTHOR_AVATAR_SIDE) {
            "Author avatar dimensions are too large"
        }
        require(temporary.renameTo(target)) { "Unable to store author avatar" }
        return Uri.fromFile(target).toString()
    } finally {
        temporary.delete()
    }
}

private fun deleteThemeAuthorAvatar(context: Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    runCatching {
        val uri = Uri.parse(uriString)
        if (uri.scheme != "file") return@runCatching
        val file = File(uri.path ?: return@runCatching).canonicalFile
        val root = themeAuthorAvatarDirectory(context).canonicalFile
        if (file.parentFile == root && file.name.startsWith("avatar-") && file.name.endsWith(".image")) {
            file.delete()
        }
    }
}

private fun themeAuthorAvatarDirectory(context: Context): File {
    return File(context.filesDir, "theme-author").apply { mkdirs() }
}
