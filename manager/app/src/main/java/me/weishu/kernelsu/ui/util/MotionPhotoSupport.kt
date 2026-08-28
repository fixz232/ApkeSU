package me.weishu.kernelsu.ui.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Locale

private const val MAX_MOTION_PHOTO_SOURCE_BYTES = 500L * 1024L * 1024L
private const val MOTION_PHOTO_PREFIX_BYTES = 4 * 1024 * 1024
private const val MP4_SCAN_CHUNK_BYTES = 128 * 1024
private const val MP4_SCAN_OVERLAP_BYTES = 16
internal const val CUSTOM_STARTUP_ANIMATION_DIR_NAME = "custom-startup-animations"

data class StartupAnimationImport(
    val uri: Uri,
    val mediaInfo: MediaFileInfo,
    val extractedFromMotionPhoto: Boolean,
    val requiresPersistablePermission: Boolean,
)

internal data class MotionPhotoContainerScan(
    val videoOffsets: List<Long>,
    val declaredAsMotionPhoto: Boolean,
)

private sealed interface MotionPhotoExtraction {
    data object NotPresent : MotionPhotoExtraction
    data class Extracted(val uri: Uri, val mediaInfo: MediaFileInfo) : MotionPhotoExtraction
}

suspend fun prepareStartupAnimationImport(
    context: Context,
    sourceUri: Uri,
): Result<StartupAnimationImport> = withContext(Dispatchers.IO) {
    runCatching {
        val appContext = context.applicationContext
        when (val motionPhoto = extractMotionPhotoVideo(appContext, sourceUri)) {
            is MotionPhotoExtraction.Extracted -> StartupAnimationImport(
                uri = motionPhoto.uri,
                mediaInfo = motionPhoto.mediaInfo,
                extractedFromMotionPhoto = true,
                requiresPersistablePermission = false,
            )

            MotionPhotoExtraction.NotPresent -> {
                val info = inspectMediaFileBlocking(appContext, sourceUri)
                require(info.decodable) {
                    info.error ?: "Unable to decode the selected startup animation"
                }
                StartupAnimationImport(
                    uri = sourceUri,
                    mediaInfo = info,
                    extractedFromMotionPhoto = false,
                    requiresPersistablePermission = sourceUri.scheme != "file",
                )
            }
        }
    }
}

internal fun deletePersistedStartupAnimationReference(context: Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    runCatching {
        val uri = Uri.parse(uriString)
        if (uri.scheme != "file") return@runCatching
        val path = uri.path ?: return@runCatching
        val file = File(path).canonicalFile
        val animationDir = customStartupAnimationDir(context.applicationContext).canonicalFile
        if (file.path != animationDir.path &&
            !file.path.startsWith(animationDir.path + File.separator)
        ) {
            return@runCatching
        }
        file.delete()
    }
}

private fun extractMotionPhotoVideo(context: Context, sourceUri: Uri): MotionPhotoExtraction {
    if (!canContainMotionPhoto(context, sourceUri)) return MotionPhotoExtraction.NotPresent

    val sourceFile = File.createTempFile("motion-photo-source-", ".bin", context.cacheDir)
    try {
        copySourceToFile(context, sourceUri, sourceFile)
        val scan = scanMotionPhotoContainer(sourceFile)
        if (scan.videoOffsets.isEmpty()) {
            if (scan.declaredAsMotionPhoto) {
                throw IOException("The Motion Photo video payload is missing")
            }
            return MotionPhotoExtraction.NotPresent
        }

        val animationDir = customStartupAnimationDir(context).apply { mkdirs() }
        var lastDecodeError: String? = null
        for (offset in scan.videoOffsets) {
            val importingFile = File.createTempFile(".motion-photo-", ".mp4.importing", animationDir)
            try {
                copyFileRange(sourceFile, offset, importingFile)
                val info = inspectMediaFileBlocking(
                    context = context,
                    uri = Uri.fromFile(importingFile),
                    mimeTypeHint = "video/mp4",
                )
                if (!info.decodable || info.width == null || info.height == null) {
                    lastDecodeError = info.error ?: "Unable to decode the Motion Photo video"
                    continue
                }

                val digest = sha256(importingFile).take(24)
                val targetFile = File(animationDir, "motion-$digest.mp4")
                if (targetFile.isFile) {
                    importingFile.delete()
                } else if (!importingFile.renameTo(targetFile)) {
                    importingFile.copyTo(targetFile, overwrite = true)
                    importingFile.delete()
                }
                return MotionPhotoExtraction.Extracted(
                    uri = Uri.fromFile(targetFile),
                    mediaInfo = info.copy(
                        displayName = targetFile.name,
                        mimeType = "video/mp4",
                        sizeBytes = targetFile.length(),
                    ),
                )
            } finally {
                importingFile.delete()
            }
        }
        throw IOException(lastDecodeError ?: "The Motion Photo video payload is damaged")
    } finally {
        sourceFile.delete()
    }
}

private fun canContainMotionPhoto(context: Context, uri: Uri): Boolean {
    val mimeType = runCatching { context.contentResolver.getType(uri) }
        .getOrNull()
        ?.lowercase(Locale.ROOT)
    if (mimeType == "image/jpeg" || mimeType == "image/jpg" ||
        mimeType == "image/heic" || mimeType == "image/heif"
    ) {
        return true
    }
    return listOfNotNull(uri.toString(), queryMotionPhotoDisplayName(context, uri)).any { value ->
        val normalized = value.lowercase(Locale.ROOT).substringBefore('?').substringBefore('#')
        listOf(".jpg", ".jpeg", ".heic", ".heif").any(normalized::endsWith)
    }
}

private fun queryMotionPhotoDisplayName(context: Context, uri: Uri): String? {
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

private fun copySourceToFile(context: Context, sourceUri: Uri, target: File) {
    val input = if (sourceUri.scheme == "file") {
        FileInputStream(File(sourceUri.path ?: throw IOException("Invalid file URI")))
    } else {
        context.contentResolver.openInputStream(sourceUri)
            ?: throw IOException("Unable to open the selected Motion Photo")
    }
    input.use { source ->
        FileOutputStream(target).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var copied = 0L
            while (true) {
                val read = source.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                copied += read
                if (copied > MAX_MOTION_PHOTO_SOURCE_BYTES) {
                    throw IOException("The selected Motion Photo is too large")
                }
                output.write(buffer, 0, read)
            }
            if (copied == 0L) throw IOException("The selected Motion Photo is empty")
            output.fd.sync()
        }
    }
}

private fun copyFileRange(source: File, offset: Long, target: File) {
    require(offset in 1 until source.length()) { "Invalid Motion Photo video offset" }
    RandomAccessFile(source, "r").use { input ->
        input.seek(offset)
        FileOutputStream(target).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var copied = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                copied += read
                if (copied > MAX_MOTION_PHOTO_SOURCE_BYTES) {
                    throw IOException("The Motion Photo video is too large")
                }
                output.write(buffer, 0, read)
            }
            if (copied == 0L) throw IOException("The Motion Photo video is empty")
            output.fd.sync()
        }
    }
}

internal fun scanMotionPhotoContainer(file: File): MotionPhotoContainerScan {
    require(file.isFile) { "Motion Photo source does not exist" }
    val fileSize = file.length()
    if (fileSize <= 0L) return MotionPhotoContainerScan(emptyList(), false)

    val prefixSize = minOf(fileSize, MOTION_PHOTO_PREFIX_BYTES.toLong()).toInt()
    val prefix = ByteArray(prefixSize)
    FileInputStream(file).use { input ->
        var readTotal = 0
        while (readTotal < prefix.size) {
            val read = input.read(prefix, readTotal, prefix.size - readTotal)
            if (read < 0) break
            if (read == 0) continue
            readTotal += read
        }
    }
    val xmp = prefix.toString(Charsets.ISO_8859_1)
    val declared = xmp.contains("MotionPhoto", ignoreCase = true) ||
        xmp.contains("MicroVideo", ignoreCase = true) ||
        xmp.contains("MotionPhoto_Data", ignoreCase = true)

    val offsets = linkedSetOf<Long>()
    parseDeclaredVideoOffsets(xmp, fileSize).forEach { offset ->
        if (offset in 1 until fileSize) offsets += offset
    }
    findEmbeddedMp4Offsets(file).forEach { offset ->
        if (offset in 1 until fileSize) offsets += offset
    }
    return MotionPhotoContainerScan(offsets.toList(), declared)
}

private fun parseDeclaredVideoOffsets(xmp: String, fileSize: Long): List<Long> {
    val offsets = mutableListOf<Long>()
    val offsetPatterns = listOf(
        Regex("""(?i)(?:[\w.-]+:)?MicroVideoOffset\s*=\s*[\"'](\d+)[\"']"""),
        Regex("""(?is)<(?:[\w.-]+:)?MicroVideoOffset\b[^>]*>\s*(\d+)\s*</"""),
    )
    for (pattern in offsetPatterns) {
        for (match in pattern.findAll(xmp)) {
            val fromEnd = match.groupValues[1].toLongOrNull() ?: continue
            if (fromEnd in 1 until fileSize) offsets += fileSize - fromEnd
        }
    }

    val itemPattern = Regex("""(?is)<(?:[\w.-]+:)?Item\b[^>]{0,2048}>""")
    for (match in itemPattern.findAll(xmp)) {
        val tag = match.value
        val semantic = readXmlAttribute(tag, "Semantic")
        val mime = readXmlAttribute(tag, "Mime")
        if (!semantic.equals("MotionPhoto", ignoreCase = true) &&
            !mime.equals("video/mp4", ignoreCase = true)
        ) {
            continue
        }
        val length = readXmlAttribute(tag, "Length")?.toLongOrNull() ?: continue
        val padding = readXmlAttribute(tag, "Padding")?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        val offset = fileSize - length - padding
        if (length > 0L && offset in 1 until fileSize) offsets += offset
    }
    return offsets.distinct()
}

private fun readXmlAttribute(tag: String, name: String): String? {
    val pattern = Regex("""(?i)(?:[\w.-]+:)?$name\s*=\s*[\"']([^\"']+)[\"']""")
    return pattern.find(tag)?.groupValues?.getOrNull(1)
}

private fun findEmbeddedMp4Offsets(file: File): List<Long> {
    val offsets = linkedSetOf<Long>()
    FileInputStream(file).use { input ->
        val buffer = ByteArray(MP4_SCAN_CHUNK_BYTES + MP4_SCAN_OVERLAP_BYTES)
        var carry = 0
        var consumed = 0L
        while (true) {
            val read = input.read(buffer, carry, MP4_SCAN_CHUNK_BYTES)
            if (read < 0) break
            if (read == 0) continue
            val total = carry + read
            val baseOffset = consumed - carry
            if (total >= 12) {
                for (index in 0..total - 12) {
                    if (isMp4FtypBox(buffer, index, total)) {
                        offsets += baseOffset + index
                    }
                }
            }
            consumed += read
            carry = minOf(MP4_SCAN_OVERLAP_BYTES, total)
            buffer.copyInto(buffer, 0, total - carry, total)
        }
    }
    return offsets.toList()
}

private fun isMp4FtypBox(bytes: ByteArray, offset: Int, limit: Int): Boolean {
    if (offset < 0 || offset + 12 > limit) return false
    if (bytes[offset + 4] != 'f'.code.toByte() ||
        bytes[offset + 5] != 't'.code.toByte() ||
        bytes[offset + 6] != 'y'.code.toByte() ||
        bytes[offset + 7] != 'p'.code.toByte()
    ) {
        return false
    }
    val boxSize = ((bytes[offset].toLong() and 0xffL) shl 24) or
        ((bytes[offset + 1].toLong() and 0xffL) shl 16) or
        ((bytes[offset + 2].toLong() and 0xffL) shl 8) or
        (bytes[offset + 3].toLong() and 0xffL)
    if (boxSize < 16L) return false

    val brand = String(bytes, offset + 8, 4, Charsets.US_ASCII)
        .lowercase(Locale.ROOT)
    return brand == "isom" || brand.startsWith("iso") || brand.startsWith("mp4") ||
        brand.startsWith("3gp") || brand == "avc1" || brand == "m4v " ||
        brand == "msnv" || brand == "qt  " || brand == "dash"
}

private fun customStartupAnimationDir(context: Context): File {
    return File(context.filesDir, CUSTOM_STARTUP_ANIMATION_DIR_NAME)
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}
