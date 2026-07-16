package me.weishu.kernelsu.ui.screen.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ui.util.getFileName
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.roundToInt

internal class AiAttachmentReader(
    context: Context,
    private val secureStore: AiChatSecureStore,
) {
    private val appContext = context.applicationContext

    suspend fun readDocument(uri: Uri): AiAttachment = withContext(Dispatchers.IO) {
        val name = resolveName(uri)
        val mimeType = appContext.contentResolver.getType(uri).orEmpty()
            .ifBlank { "application/octet-stream" }
        val declaredSize = querySize(uri)
        val lowerName = name.lowercase(Locale.ROOT)
        if (mimeType == "application/zip" || lowerName.endsWith(".zip")) {
            return@withContext readArchive(uri, name, declaredSize, mimeType)
        }

        val bytes = readLimited(uri, MAX_DOCUMENT_BYTES + 1)
        if (bytes.size > MAX_DOCUMENT_BYTES) {
            throw AttachmentTooLargeException(MAX_DOCUMENT_BYTES)
        }
        val decoded = decodeText(bytes, lowerName, mimeType)
            ?: throw UnsupportedAttachmentException()
        val normalized = normalizeText(decoded)
        val content = prettyPrintStructuredText(normalized, lowerName)
        AiAttachment(
            kind = AiAttachmentKind.Text,
            name = name,
            sizeBytes = declaredSize.takeIf { it >= 0 } ?: bytes.size.toLong(),
            mimeType = mimeType,
            extractedText = content.take(MAX_ATTACHMENT_TEXT_CHARS),
            truncated = content.length > MAX_ATTACHMENT_TEXT_CHARS,
            sha256 = bytes.sha256(),
        )
    }

    suspend fun readImage(uri: Uri): AiAttachment = withContext(Dispatchers.IO) {
        val name = resolveName(uri)
        val source = ImageDecoder.createSource(appContext.contentResolver, uri)
        var originalWidth = 0
        var originalHeight = 0
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            originalWidth = info.size.width
            originalHeight = info.size.height
            require(originalWidth > 0 && originalHeight > 0) { "Invalid image dimensions" }
            val scale = minOf(
                1f,
                MAX_IMAGE_DIMENSION.toFloat() / max(originalWidth, originalHeight).toFloat(),
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            decoder.setTargetSize(
                max(1, (originalWidth * scale).roundToInt()),
                max(1, (originalHeight * scale).roundToInt()),
            )
        }
        val encoded = encodeImage(bitmap)
        bitmap.recycle()
        if (encoded.size > MAX_ENCODED_IMAGE_BYTES) {
            throw AttachmentTooLargeException(MAX_ENCODED_IMAGE_BYTES)
        }
        val storageId = secureStore.writeImage(encoded)
        AiAttachment(
            kind = AiAttachmentKind.Image,
            name = name,
            sizeBytes = encoded.size.toLong(),
            mimeType = "image/webp",
            storageId = storageId,
            sha256 = encoded.sha256(),
            width = originalWidth,
            height = originalHeight,
        )
    }

    private fun readArchive(
        uri: Uri,
        name: String,
        declaredSize: Long,
        mimeType: String,
    ): AiAttachment {
        val digest = MessageDigest.getInstance("SHA-256")
        val report = StringBuilder()
        var entryCount = 0
        var textEntryCount = 0
        var totalExtracted = 0
        var truncated = false
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open attachment")
        input.use { raw ->
            ZipInputStream(raw).use { zip ->
                while (entryCount < MAX_ARCHIVE_ENTRIES) {
                    val entry = zip.nextEntry ?: break
                    entryCount += 1
                    report.append("- ").append(entry.name.take(MAX_ARCHIVE_ENTRY_NAME_CHARS))
                    if (entry.isDirectory) {
                        report.append("/")
                    } else if (isReadableArchiveEntry(entry.name)) {
                        val remaining = MAX_ARCHIVE_EXTRACTED_BYTES - totalExtracted
                        if (remaining <= 0) {
                            truncated = true
                        } else {
                            val bytes = readZipEntryLimited(zip, minOf(MAX_ARCHIVE_ENTRY_BYTES, remaining))
                            totalExtracted += bytes.size
                            val decoded = decodeText(
                                bytes,
                                entry.name.lowercase(Locale.ROOT),
                                "text/plain",
                            )
                            if (decoded != null) {
                                textEntryCount += 1
                                report.append("\n```text\n")
                                    .append(normalizeText(decoded).take(MAX_ARCHIVE_ENTRY_CHARS))
                                    .append("\n```")
                                if (decoded.length > MAX_ARCHIVE_ENTRY_CHARS) truncated = true
                            }
                        }
                    }
                    report.append("\n")
                    zip.closeEntry()
                }
                if (zip.nextEntry != null) truncated = true
            }
        }
        val header = buildString {
            append("Archive inventory: ").append(entryCount).append(" entries")
            append(", ").append(textEntryCount).append(" readable text entries.\n")
            if (truncated) append("The archive report was truncated for safety.\n")
        }
        val reportText = (header + report).take(MAX_ATTACHMENT_TEXT_CHARS)
        digest.update(reportText.toByteArray(Charsets.UTF_8))
        return AiAttachment(
            kind = AiAttachmentKind.Archive,
            name = name,
            sizeBytes = declaredSize.coerceAtLeast(0L),
            mimeType = mimeType,
            extractedText = reportText,
            truncated = truncated || header.length + report.length > MAX_ATTACHMENT_TEXT_CHARS,
            sha256 = digest.digest().toHex(),
        )
    }

    private fun encodeImage(bitmap: Bitmap): ByteArray {
        var last = ByteArray(0)
        for (quality in IMAGE_QUALITY_STEPS) {
            val output = ByteArrayOutputStream()
            check(bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, output)) {
                "Unable to encode image"
            }
            last = output.toByteArray()
            if (last.size <= TARGET_ENCODED_IMAGE_BYTES) break
        }
        return last
    }

    private fun resolveName(uri: Uri): String =
        uri.getFileName(appContext).orEmpty().ifBlank { uri.lastPathSegment ?: "attachment" }

    private fun querySize(uri: Uri): Long {
        return runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1L
            } ?: -1L
        }.getOrDefault(-1L)
    }

    private fun readLimited(uri: Uri, limit: Int): ByteArray {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open attachment")
        return input.use { stream ->
            val output = ByteArrayOutputStream(minOf(limit, DEFAULT_BUFFER_SIZE))
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (output.size() < limit) {
                val count = stream.read(buffer, 0, minOf(buffer.size, limit - output.size()))
                if (count < 0) break
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }
}

private fun decodeText(bytes: ByteArray, lowerName: String, mimeType: String): String? {
    if (bytes.isEmpty()) return ""
    val knownText = mimeType.startsWith("text/") || TEXT_EXTENSIONS.any(lowerName::endsWith)
    val charset = when {
        bytes.startsWithBytes(UTF8_BOM) -> StandardCharsets.UTF_8
        bytes.startsWithBytes(UTF16_LE_BOM) -> StandardCharsets.UTF_16LE
        bytes.startsWithBytes(UTF16_BE_BOM) -> StandardCharsets.UTF_16BE
        looksLikeUtf16Le(bytes) -> StandardCharsets.UTF_16LE
        looksLikeUtf16Be(bytes) -> StandardCharsets.UTF_16BE
        else -> StandardCharsets.UTF_8
    }
    if (!knownText && charset == StandardCharsets.UTF_8 && looksBinary(bytes)) return null
    return runCatching {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
            .removePrefix("\uFEFF")
    }.getOrNull()?.takeUnless { !knownText && it.count { char -> char == '\uFFFD' } > it.length / 50 }
}

private fun normalizeText(text: String): String = text
    .replace("\r\n", "\n")
    .replace('\r', '\n')
    .map { char -> if (char == '\n' || char == '\t' || !char.isISOControl()) char else ' ' }
    .joinToString("")
    .trim()

private fun prettyPrintStructuredText(text: String, lowerName: String): String {
    if (text.length > MAX_STRUCTURED_PARSE_CHARS) return text
    return when {
        lowerName.endsWith(".json") -> runCatching {
            if (text.trimStart().startsWith("[")) JSONArray(text).toString(2) else JSONObject(text).toString(2)
        }.getOrDefault(text)
        else -> text
    }
}

private fun looksBinary(bytes: ByteArray): Boolean {
    val sample = bytes.take(MAX_BINARY_SAMPLE_BYTES)
    if (sample.any { it == 0.toByte() }) return true
    val controls = sample.count { byte ->
        val value = byte.toInt() and 0xff
        value < 0x20 && value !in setOf(0x09, 0x0a, 0x0d)
    }
    return controls > sample.size / 20
}

private fun looksLikeUtf16Le(bytes: ByteArray): Boolean =
    bytes.size >= 8 && bytes.indices.filter { it % 2 == 1 }.take(32).count { bytes[it] == 0.toByte() } >= 6

private fun looksLikeUtf16Be(bytes: ByteArray): Boolean =
    bytes.size >= 8 && bytes.indices.filter { it % 2 == 0 }.take(32).count { bytes[it] == 0.toByte() } >= 6

private fun ByteArray.startsWithBytes(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

private fun isReadableArchiveEntry(name: String): Boolean {
    val lower = name.lowercase(Locale.ROOT)
    return !lower.contains("../") && TEXT_EXTENSIONS.any(lower::endsWith)
}

private fun readZipEntryLimited(zip: ZipInputStream, limit: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(limit, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (output.size() < limit) {
        val count = zip.read(buffer, 0, minOf(buffer.size, limit - output.size()))
        if (count < 0) break
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).toHex()

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

internal class AttachmentTooLargeException(val maxBytes: Int) : IOException()
internal class UnsupportedAttachmentException : IOException()

private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
private val UTF16_LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
private val UTF16_BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
private val TEXT_EXTENSIONS = setOf(
    ".txt", ".log", ".md", ".json", ".xml", ".yaml", ".yml", ".toml",
    ".ini", ".conf", ".cfg", ".prop", ".properties", ".sh", ".rc", ".csv",
    ".kt", ".kts", ".java", ".c", ".h", ".rs", ".py", ".js", ".ts",
)
private val IMAGE_QUALITY_STEPS = intArrayOf(84, 74, 64)
private const val MAX_DOCUMENT_BYTES = 256 * 1024
private const val MAX_ATTACHMENT_TEXT_CHARS = 48_000
private const val MAX_STRUCTURED_PARSE_CHARS = 128_000
private const val MAX_BINARY_SAMPLE_BYTES = 4_096
private const val MAX_IMAGE_DIMENSION = 1_536
private const val TARGET_ENCODED_IMAGE_BYTES = 900 * 1024
private const val MAX_ENCODED_IMAGE_BYTES = 1_500 * 1024
private const val MAX_ARCHIVE_ENTRIES = 80
private const val MAX_ARCHIVE_ENTRY_BYTES = 24 * 1024
private const val MAX_ARCHIVE_EXTRACTED_BYTES = 192 * 1024
private const val MAX_ARCHIVE_ENTRY_CHARS = 12_000
private const val MAX_ARCHIVE_ENTRY_NAME_CHARS = 160
