package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal enum class AiModuleTemplate {
    Basic,
    BootService,
    SystemProperties,
    WebUi,
    Complete,
}

internal enum class AiModuleAiMode {
    Ask,
    ReviewProject,
    GenerateCurrentFile,
    FixCurrentFile,
}

@Immutable
internal data class AiModuleTemplateMetadata(
    val moduleId: String = "apkesu_module",
    val name: String = "ApkeSU Module",
    val version: String = "1.0.0",
    val versionCode: String = "1",
    val author: String = "Your Name",
    val description: String = "A module created with ApkeSU AI Module Studio.",
)

@Immutable
internal data class AiModuleStudioFile(
    val path: String,
    val content: String,
)

@Immutable
internal data class AiModuleStudioProject(
    val files: List<AiModuleStudioFile>,
    val selectedPath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = createdAt,
    val lastAiPrompt: String = "",
    val lastAiResponse: String = "",
) {
    val selectedFile: AiModuleStudioFile?
        get() = files.firstOrNull { it.path == selectedPath }

    val metadata: AiModuleTemplateMetadata
        get() = parseModuleMetadata(files.firstOrNull { it.path == MODULE_PROP_PATH }?.content.orEmpty())
}

internal enum class AiModuleIssueSeverity {
    Error,
    Warning,
}

internal enum class AiModuleIssueCode {
    MissingModuleProp,
    MissingProperty,
    InvalidModuleId,
    InvalidVersionCode,
    DuplicateProperty,
    TooManyFiles,
    InvalidPath,
    DuplicatePath,
    ReservedPath,
    FileTooLarge,
    ProjectTooLarge,
    NullCharacter,
    MissingShebang,
    WindowsLineEnding,
    DestructiveRootCommand,
    BlockDeviceWrite,
    UnsafePermission,
    RuntimeSecurityChange,
    RemotePipeExecution,
    RebootCommand,
    ManagerDirectoryMutation,
    MalformedSystemProperty,
    BroadSePolicyRule,
    MissingWebUiEntry,
}

@Immutable
internal data class AiModuleValidationIssue(
    val severity: AiModuleIssueSeverity,
    val code: AiModuleIssueCode,
    val path: String? = null,
    val line: Int? = null,
    val detail: String = "",
)

internal data class AiModuleValidationResult(
    val issues: List<AiModuleValidationIssue>,
) {
    val errors: List<AiModuleValidationIssue>
        get() = issues.filter { it.severity == AiModuleIssueSeverity.Error }
    val warnings: List<AiModuleValidationIssue>
        get() = issues.filter { it.severity == AiModuleIssueSeverity.Warning }
    val canExport: Boolean
        get() = errors.isEmpty()
}

internal enum class AiModuleImportError {
    InvalidArchive,
    EmptyArchive,
    TooManyFiles,
    FileTooLarge,
    ProjectTooLarge,
    InvalidPath,
    DuplicatePath,
    BinaryFile,
    MissingModuleProp,
}

internal class AiModuleImportException(
    val reason: AiModuleImportError,
    val entryPath: String = "",
    cause: Throwable? = null,
) : IllegalArgumentException(reason.name, cause)

internal fun createAiModuleProject(
    template: AiModuleTemplate,
    metadata: AiModuleTemplateMetadata,
    now: Long = System.currentTimeMillis(),
): AiModuleStudioProject {
    val safeMetadata = metadata.sanitized()
    val files = linkedMapOf<String, String>()
    files[MODULE_PROP_PATH] = buildModuleProp(safeMetadata)
    files[CUSTOMIZE_PATH] = buildCustomizeScript(template, safeMetadata)
    files[README_PATH] = buildReadme(safeMetadata)

    if (template in setOf(AiModuleTemplate.BootService, AiModuleTemplate.Complete)) {
        files[SERVICE_PATH] = SERVICE_TEMPLATE
        files[POST_FS_DATA_PATH] = POST_FS_DATA_TEMPLATE
    }
    if (template in setOf(AiModuleTemplate.SystemProperties, AiModuleTemplate.Complete)) {
        files[SYSTEM_PROP_PATH] = SYSTEM_PROP_TEMPLATE
    }
    if (template in setOf(AiModuleTemplate.WebUi, AiModuleTemplate.Complete)) {
        files[ACTION_PATH] = ACTION_TEMPLATE
        files[WEB_UI_INDEX_PATH] = WEB_UI_INDEX_TEMPLATE
        files[WEB_UI_STYLE_PATH] = WEB_UI_STYLE_TEMPLATE
        files[WEB_UI_SCRIPT_PATH] = WEB_UI_SCRIPT_TEMPLATE
    }
    if (template == AiModuleTemplate.Complete) {
        files[UNINSTALL_PATH] = UNINSTALL_TEMPLATE
        files[SEPOLICY_PATH] = SEPOLICY_TEMPLATE
        files[INIT_RC_PATH] = INIT_RC_TEMPLATE
    }

    return AiModuleStudioProject(
        files = files.map { (path, content) -> AiModuleStudioFile(path, content) },
        selectedPath = MODULE_PROP_PATH,
        createdAt = now,
        modifiedAt = now,
    )
}

internal fun validateAiModuleProject(project: AiModuleStudioProject): AiModuleValidationResult {
    val issues = mutableListOf<AiModuleValidationIssue>()
    if (project.files.size > MAX_MODULE_STUDIO_FILES) {
        issues += AiModuleValidationIssue(
            AiModuleIssueSeverity.Error,
            AiModuleIssueCode.TooManyFiles,
            detail = project.files.size.toString(),
        )
    }

    val normalizedPaths = hashSetOf<String>()
    var totalCharacters = 0L
    project.files.forEach { file ->
        totalCharacters += file.content.length
        val normalized = normalizeModuleFilePath(file.path)
        if (normalized == null || normalized != file.path) {
            issues += AiModuleValidationIssue(
                AiModuleIssueSeverity.Error,
                AiModuleIssueCode.InvalidPath,
                path = file.path,
            )
        } else if (!normalizedPaths.add(normalized.lowercase(Locale.ROOT))) {
            issues += AiModuleValidationIssue(
                AiModuleIssueSeverity.Error,
                AiModuleIssueCode.DuplicatePath,
                path = file.path,
            )
        }
        if (normalized?.lowercase(Locale.ROOT) in RESERVED_MODULE_PATHS) {
            issues += AiModuleValidationIssue(
                AiModuleIssueSeverity.Error,
                AiModuleIssueCode.ReservedPath,
                path = file.path,
            )
        }
        if (file.content.length > MAX_MODULE_STUDIO_FILE_CHARS) {
            issues += AiModuleValidationIssue(
                AiModuleIssueSeverity.Error,
                AiModuleIssueCode.FileTooLarge,
                path = file.path,
                detail = file.content.length.toString(),
            )
        }
        if ('\u0000' in file.content) {
            issues += AiModuleValidationIssue(
                AiModuleIssueSeverity.Error,
                AiModuleIssueCode.NullCharacter,
                path = file.path,
            )
        }
        validateTextFile(file, issues)
    }
    if (totalCharacters > MAX_MODULE_STUDIO_PROJECT_CHARS) {
        issues += AiModuleValidationIssue(
            AiModuleIssueSeverity.Error,
            AiModuleIssueCode.ProjectTooLarge,
            detail = totalCharacters.toString(),
        )
    }

    val moduleProp = project.files.firstOrNull { it.path == MODULE_PROP_PATH }
    if (moduleProp == null) {
        issues += AiModuleValidationIssue(
            AiModuleIssueSeverity.Error,
            AiModuleIssueCode.MissingModuleProp,
            path = MODULE_PROP_PATH,
        )
    } else {
        validateModuleProp(moduleProp, issues)
    }

    if (
        project.files.any { it.path.startsWith("webroot/") } &&
        project.files.none { it.path == WEB_UI_INDEX_PATH }
    ) {
        issues += AiModuleValidationIssue(
            AiModuleIssueSeverity.Warning,
            AiModuleIssueCode.MissingWebUiEntry,
            path = WEB_UI_INDEX_PATH,
        )
    }

    return AiModuleValidationResult(
        issues.sortedWith(
            compareBy<AiModuleValidationIssue> { it.severity }
                .thenBy { it.path.orEmpty() }
                .thenBy { it.line ?: 0 }
        )
    )
}

private fun validateModuleProp(
    file: AiModuleStudioFile,
    issues: MutableList<AiModuleValidationIssue>,
) {
    val parsed = parsePropertyLines(file.content)
    parsed.duplicates.forEach { key ->
        issues += AiModuleValidationIssue(
            AiModuleIssueSeverity.Warning,
            AiModuleIssueCode.DuplicateProperty,
            path = file.path,
            detail = key,
        )
    }
    REQUIRED_MODULE_PROPERTIES.forEach { key ->
        if (parsed.values[key].isNullOrBlank()) {
            issues += AiModuleValidationIssue(
                AiModuleIssueSeverity.Error,
                AiModuleIssueCode.MissingProperty,
                path = file.path,
                detail = key,
            )
        }
    }
    parsed.values["id"]?.takeIf(String::isNotBlank)?.let { id ->
        if (!MODULE_ID_REGEX.matches(id)) {
            issues += AiModuleValidationIssue(
                AiModuleIssueSeverity.Error,
                AiModuleIssueCode.InvalidModuleId,
                path = file.path,
                detail = id,
            )
        }
    }
    parsed.values["versionCode"]?.takeIf(String::isNotBlank)?.let { versionCode ->
        if (versionCode.toIntOrNull()?.let { it > 0 } != true) {
            issues += AiModuleValidationIssue(
                AiModuleIssueSeverity.Error,
                AiModuleIssueCode.InvalidVersionCode,
                path = file.path,
                detail = versionCode,
            )
        }
    }
}

private fun validateTextFile(
    file: AiModuleStudioFile,
    issues: MutableList<AiModuleValidationIssue>,
) {
    val lowerPath = file.path.lowercase(Locale.ROOT)
    if (lowerPath.endsWith(".sh")) {
        if (!file.content.startsWith("#!/system/bin/sh") && !file.content.startsWith("#!/system/bin/bash")) {
            issues += AiModuleValidationIssue(
                AiModuleIssueSeverity.Warning,
                AiModuleIssueCode.MissingShebang,
                path = file.path,
                line = 1,
            )
        }
        if ('\r' in file.content) {
            issues += AiModuleValidationIssue(
                AiModuleIssueSeverity.Warning,
                AiModuleIssueCode.WindowsLineEnding,
                path = file.path,
            )
        }
        file.content.lineSequence().forEachIndexed { index, line ->
            DANGEROUS_SHELL_RULES.forEach { rule ->
                if (rule.regex.containsMatchIn(line)) {
                    issues += AiModuleValidationIssue(
                        severity = rule.severity,
                        code = rule.code,
                        path = file.path,
                        line = index + 1,
                        detail = line.trim().take(MAX_ISSUE_DETAIL_CHARS),
                    )
                }
            }
        }
    }
    if (lowerPath == SYSTEM_PROP_PATH) {
        file.content.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isNotBlank() && !line.startsWith('#') && !PROPERTY_LINE_REGEX.matches(line)) {
                issues += AiModuleValidationIssue(
                    AiModuleIssueSeverity.Warning,
                    AiModuleIssueCode.MalformedSystemProperty,
                    path = file.path,
                    line = index + 1,
                    detail = line.take(MAX_ISSUE_DETAIL_CHARS),
                )
            }
        }
    }
    if (lowerPath == SEPOLICY_PATH && BROAD_SEPOLICY_REGEX.containsMatchIn(file.content)) {
        issues += AiModuleValidationIssue(
            AiModuleIssueSeverity.Warning,
            AiModuleIssueCode.BroadSePolicyRule,
            path = file.path,
        )
    }
}

internal fun normalizeModuleFilePath(rawPath: String): String? {
    val normalized = rawPath.trim()
        .replace('\\', '/')
        .replace(Regex("/+"), "/")
        .trim('/')
    if (normalized.isBlank() || normalized.length > MAX_MODULE_PATH_CHARS) return null
    if (rawPath.trim().startsWith('/') || rawPath.trim().endsWith('/')) return null
    val segments = normalized.split('/')
    if (segments.any { it.isBlank() || it == "." || it == ".." || !MODULE_PATH_SEGMENT_REGEX.matches(it) }) {
        return null
    }
    if (segments.first().equals("META-INF", ignoreCase = true)) return null
    return normalized
}

internal fun parseModuleMetadata(content: String): AiModuleTemplateMetadata {
    val values = parsePropertyLines(content).values
    return AiModuleTemplateMetadata(
        moduleId = values["id"].orEmpty(),
        name = values["name"].orEmpty(),
        version = values["version"].orEmpty(),
        versionCode = values["versionCode"].orEmpty(),
        author = values["author"].orEmpty(),
        description = values["description"].orEmpty(),
    )
}

internal fun updateAiModuleMetadata(
    currentContent: String,
    metadata: AiModuleTemplateMetadata,
): String {
    val standardProperties = buildModuleProp(metadata.sanitized()).trimEnd()
    val retainedLines = currentContent.lineSequence()
        .filterNot { rawLine ->
            val line = rawLine.trim()
            val separator = line.indexOf('=')
            separator > 0 && line.substring(0, separator).trim() in REQUIRED_MODULE_PROPERTIES
        }
        .toList()
        .dropWhile(String::isBlank)
        .dropLastWhile(String::isBlank)

    return buildString {
        appendLine(standardProperties)
        if (retainedLines.isNotEmpty()) {
            appendLine()
            append(retainedLines.joinToString("\n"))
            appendLine()
        }
    }
}

internal fun readAiModuleProjectZip(
    input: InputStream,
    now: Long = System.currentTimeMillis(),
): AiModuleStudioProject {
    val importedFiles = mutableListOf<AiModuleStudioFile>()
    val importedPaths = hashSetOf<String>()
    var totalBytes = 0L

    try {
        ZipInputStream(BufferedInputStream(input), Charsets.UTF_8).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val rawPath = entry.name.replace('\\', '/')
                if (entry.isDirectory || rawPath.startsWith("META-INF/", ignoreCase = true) ||
                    rawPath.startsWith("__MACOSX/", ignoreCase = true)
                ) {
                    zip.closeEntry()
                    continue
                }
                if (importedFiles.size >= MAX_MODULE_STUDIO_FILES) {
                    throw AiModuleImportException(AiModuleImportError.TooManyFiles)
                }
                val path = normalizeModuleFilePath(rawPath)
                    ?: throw AiModuleImportException(AiModuleImportError.InvalidPath, rawPath)
                if (!importedPaths.add(path.lowercase(Locale.ROOT))) {
                    throw AiModuleImportException(AiModuleImportError.DuplicatePath, path)
                }
                if (entry.size > MAX_IMPORT_FILE_BYTES) {
                    throw AiModuleImportException(AiModuleImportError.FileTooLarge, path)
                }
                val bytes = readImportedZipEntry(zip, path, totalBytes)
                totalBytes += bytes.size
                val content = decodeImportedText(bytes, path)
                if (content.length > MAX_MODULE_STUDIO_FILE_CHARS) {
                    throw AiModuleImportException(AiModuleImportError.FileTooLarge, path)
                }
                importedFiles += AiModuleStudioFile(path, content)
                zip.closeEntry()
            }
        }
    } catch (error: AiModuleImportException) {
        throw error
    } catch (error: Exception) {
        throw AiModuleImportException(AiModuleImportError.InvalidArchive, cause = error)
    }

    if (importedFiles.isEmpty()) {
        throw AiModuleImportException(AiModuleImportError.EmptyArchive)
    }
    val rootedFiles = stripImportedProjectRoot(importedFiles)
    if (rootedFiles.sumOf { it.content.length.toLong() } > MAX_MODULE_STUDIO_PROJECT_CHARS) {
        throw AiModuleImportException(AiModuleImportError.ProjectTooLarge)
    }
    return AiModuleStudioProject(
        files = rootedFiles.sortedWith(
            compareBy<AiModuleStudioFile> { if (it.path == MODULE_PROP_PATH) 0 else 1 }
                .thenBy(AiModuleStudioFile::path)
        ),
        selectedPath = MODULE_PROP_PATH,
        createdAt = now,
        modifiedAt = now,
    )
}

private fun readImportedZipEntry(
    zip: ZipInputStream,
    path: String,
    totalBytesBeforeEntry: Long,
): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = zip.read(buffer)
        if (count < 0) break
        if (output.size().toLong() + count > MAX_IMPORT_FILE_BYTES) {
            throw AiModuleImportException(AiModuleImportError.FileTooLarge, path)
        }
        if (totalBytesBeforeEntry + output.size() + count > MAX_IMPORT_PROJECT_BYTES) {
            throw AiModuleImportException(AiModuleImportError.ProjectTooLarge)
        }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun decodeImportedText(bytes: ByteArray, path: String): String {
    if (bytes.any { it == 0.toByte() }) {
        throw AiModuleImportException(AiModuleImportError.BinaryFile, path)
    }
    return try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
            .removePrefix("\uFEFF")
    } catch (error: CharacterCodingException) {
        throw AiModuleImportException(AiModuleImportError.BinaryFile, path, error)
    }
}

private fun stripImportedProjectRoot(files: List<AiModuleStudioFile>): List<AiModuleStudioFile> {
    if (files.any { it.path == MODULE_PROP_PATH }) return files
    val candidates = files.filter { it.path.endsWith("/$MODULE_PROP_PATH") }
    if (candidates.size != 1) {
        throw AiModuleImportException(AiModuleImportError.MissingModuleProp)
    }
    val prefix = candidates.single().path.removeSuffix(MODULE_PROP_PATH)
    if (files.any { !it.path.startsWith(prefix) }) {
        throw AiModuleImportException(AiModuleImportError.MissingModuleProp)
    }
    val paths = hashSetOf<String>()
    return files.map { file ->
        val strippedPath = file.path.removePrefix(prefix)
        val normalized = normalizeModuleFilePath(strippedPath)
            ?: throw AiModuleImportException(AiModuleImportError.InvalidPath, strippedPath)
        if (!paths.add(normalized.lowercase(Locale.ROOT))) {
            throw AiModuleImportException(AiModuleImportError.DuplicatePath, normalized)
        }
        file.copy(path = normalized)
    }
}

internal fun writeAiModuleZip(project: AiModuleStudioProject, output: OutputStream) {
    val validation = validateAiModuleProject(project)
    require(validation.canExport) { "Module project has validation errors" }
    ZipOutputStream(BufferedOutputStream(output), Charsets.UTF_8).use { zip ->
        zip.setLevel(6)
        zip.setComment("Created by ApkeSU AI Module Studio")
        project.files
            .sortedWith(compareBy<AiModuleStudioFile> { if (it.path == MODULE_PROP_PATH) 0 else 1 }.thenBy { it.path })
            .forEach { file ->
                val entry = ZipEntry(file.path).apply { time = ZIP_EPOCH_MILLIS }
                zip.putNextEntry(entry)
                zip.write(file.content.replace("\r\n", "\n").replace('\r', '\n').toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
    }
}

internal fun buildAiModuleProjectSnapshot(
    project: AiModuleStudioProject,
    maxCharacters: Int = MAX_AI_PROJECT_SNAPSHOT_CHARS,
): String {
    val selected = project.selectedFile
    val ordered = buildList {
        if (selected != null) add(selected)
        addAll(project.files.filterNot { it.path == selected?.path }.sortedBy(AiModuleStudioFile::path))
    }
    val output = StringBuilder()
    ordered.forEach { file ->
        if (output.length >= maxCharacters) return@forEach
        val remaining = maxCharacters - output.length
        val header = "\n\n===== ${file.path} =====\n"
        if (header.length >= remaining) return@forEach
        output.append(header)
        val contentLimit = remaining - header.length
        output.append(file.content.take(contentLimit))
        if (file.content.length > contentLimit) output.append("\n...[file truncated]")
    }
    return output.toString().trim().take(maxCharacters)
}

internal fun extractFirstAiCodeBlock(response: String): String? {
    val match = AI_CODE_BLOCK_REGEX.find(response) ?: return null
    return match.groupValues[2]
        .removeSuffix("\r\n")
        .removeSuffix("\n")
        .takeIf(String::isNotBlank)
}

internal fun AiModuleStudioProject.toJson(): JSONObject = JSONObject()
    .put("schema", MODULE_STUDIO_SCHEMA_VERSION)
    .put("selectedPath", selectedPath)
    .put("createdAt", createdAt)
    .put("modifiedAt", modifiedAt)
    .put("lastAiPrompt", lastAiPrompt)
    .put("lastAiResponse", lastAiResponse)
    .put("files", JSONArray().apply {
        files.forEach { file ->
            put(JSONObject().put("path", file.path).put("content", file.content))
        }
    })

internal fun parseAiModuleStudioProject(raw: String): AiModuleStudioProject? = runCatching {
    val root = JSONObject(raw)
    val filesArray = root.optJSONArray("files") ?: return@runCatching null
    require(filesArray.length() in 1..MAX_MODULE_STUDIO_FILES)
    var totalCharacters = 0L
    val normalizedPaths = hashSetOf<String>()
    val files = buildList {
        for (index in 0 until filesArray.length()) {
            val item = filesArray.optJSONObject(index) ?: continue
            val path = item.optString("path")
            if (path.isBlank()) continue
            require(normalizeModuleFilePath(path) == path)
            require(normalizedPaths.add(path.lowercase(Locale.ROOT)))
            val content = item.optString("content")
            require(content.length <= MAX_MODULE_STUDIO_FILE_CHARS)
            totalCharacters += content.length
            require(totalCharacters <= MAX_MODULE_STUDIO_PROJECT_CHARS)
            add(AiModuleStudioFile(path, content))
        }
    }
    if (files.isEmpty()) return@runCatching null
    val requestedPath = root.optString("selectedPath")
    val selectedPath = requestedPath.takeIf { path -> files.any { it.path == path } } ?: files.first().path
    AiModuleStudioProject(
        files = files,
        selectedPath = selectedPath,
        createdAt = root.optLong("createdAt", System.currentTimeMillis()),
        modifiedAt = root.optLong("modifiedAt", System.currentTimeMillis()),
        lastAiPrompt = root.optString("lastAiPrompt").take(MAX_AI_PROMPT_CHARS),
        lastAiResponse = root.optString("lastAiResponse").take(MAX_AI_RESPONSE_CHARS),
    )
}.getOrNull()

private data class ParsedPropertyLines(
    val values: Map<String, String>,
    val duplicates: Set<String>,
)

private fun parsePropertyLines(content: String): ParsedPropertyLines {
    val values = linkedMapOf<String, String>()
    val duplicates = linkedSetOf<String>()
    content.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank() || line.startsWith('#')) return@forEach
        val separator = line.indexOf('=')
        if (separator <= 0) return@forEach
        val key = line.substring(0, separator).trim()
        val value = line.substring(separator + 1).trim()
        if (key in values) duplicates += key
        values[key] = value
    }
    return ParsedPropertyLines(values, duplicates)
}

private fun AiModuleTemplateMetadata.sanitized(): AiModuleTemplateMetadata = copy(
    moduleId = moduleId.trim().take(80),
    name = name.singleLineProperty().take(120),
    version = version.singleLineProperty().take(64),
    versionCode = versionCode.filter(Char::isDigit).take(10).ifBlank { "1" },
    author = author.singleLineProperty().take(120),
    description = description.singleLineProperty().take(240),
)

private fun String.singleLineProperty(): String =
    replace('\n', ' ').replace('\r', ' ').replace('=', '-').trim()

private fun buildModuleProp(metadata: AiModuleTemplateMetadata): String = """
    id=${metadata.moduleId}
    name=${metadata.name}
    version=${metadata.version}
    versionCode=${metadata.versionCode}
    author=${metadata.author}
    description=${metadata.description}
""".trimIndent() + "\n"

private fun buildCustomizeScript(
    template: AiModuleTemplate,
    metadata: AiModuleTemplateMetadata,
): String = buildString {
    val printableName = "- Installing ${metadata.name}".shellSingleQuoted()
    appendLine("#!/system/bin/sh")
    appendLine("ui_print $printableName")
    appendLine("ui_print \"- Module ID: \$MODID\"")
    appendLine()
    appendLine("# customize.sh is sourced by the ApkeSU/KernelSU installer.")
    appendLine("# Add installation-time checks below. Use abort \"message\" to stop safely.")
    if (template == AiModuleTemplate.Complete) {
        appendLine()
        appendLine("if [ -d \"\$MODPATH/initrc\" ]; then")
        appendLine("  set_perm_recursive \"\$MODPATH/initrc\" 0 0 0755 0755")
        appendLine("fi")
    }
}

private fun buildReadme(metadata: AiModuleTemplateMetadata): String = """
    # ${metadata.name}

    ${metadata.description}

    ## Compatibility

    - ApkeSU / KernelSU modern module installer
    - Android API and device requirements should be documented here

    ## Safety

    Review every shell command and keep a known-good boot image before installation.
""".trimIndent() + "\n"

private fun String.shellSingleQuoted(): String = buildString {
    append(39.toChar())
    this@shellSingleQuoted.forEach { character ->
        if (character.code == 39) {
            append(39.toChar())
            append(92.toChar())
            append(39.toChar())
            append(39.toChar())
        } else {
            append(character)
        }
    }
    append(39.toChar())
}

private data class DangerousShellRule(
    val regex: Regex,
    val severity: AiModuleIssueSeverity,
    val code: AiModuleIssueCode,
)

private val DANGEROUS_SHELL_RULES = listOf(
    DangerousShellRule(
        Regex("(?:^|[;&|]\\s*)rm\\s+-[^\\n]*r[^\\n]*f[^\\n]*\\s+/(?:\\s|$|\\*)"),
        AiModuleIssueSeverity.Error,
        AiModuleIssueCode.DestructiveRootCommand,
    ),
    DangerousShellRule(
        Regex("\\b(?:dd|cat)\\b[^\\n]*(?:of=|>)\\s*/dev/block/"),
        AiModuleIssueSeverity.Error,
        AiModuleIssueCode.BlockDeviceWrite,
    ),
    DangerousShellRule(
        Regex("\\bchmod\\b[^\\n]*(?:777|a\\+rwx)[^\\n]*\\s/(?:\\s|$|\\*)"),
        AiModuleIssueSeverity.Error,
        AiModuleIssueCode.UnsafePermission,
    ),
    DangerousShellRule(
        Regex("\\bsetenforce\\s+0\\b|\\bsetprop\\s+ro\\.secure\\s+0\\b"),
        AiModuleIssueSeverity.Warning,
        AiModuleIssueCode.RuntimeSecurityChange,
    ),
    DangerousShellRule(
        Regex("\\b(?:curl|wget)\\b[^\\n]*\\|\\s*(?:sh|bash)\\b"),
        AiModuleIssueSeverity.Warning,
        AiModuleIssueCode.RemotePipeExecution,
    ),
    DangerousShellRule(
        Regex("(?:^|[;&|]\\s*)reboot(?:\\s|$)"),
        AiModuleIssueSeverity.Warning,
        AiModuleIssueCode.RebootCommand,
    ),
    DangerousShellRule(
        Regex("/data/adb/(?:modules|ksu|ap)(?:/|\\s|$)"),
        AiModuleIssueSeverity.Warning,
        AiModuleIssueCode.ManagerDirectoryMutation,
    ),
)

internal const val MODULE_PROP_PATH = "module.prop"
internal const val CUSTOMIZE_PATH = "customize.sh"
internal const val README_PATH = "README.md"
internal const val SERVICE_PATH = "service.sh"
internal const val POST_FS_DATA_PATH = "post-fs-data.sh"
internal const val SYSTEM_PROP_PATH = "system.prop"
internal const val ACTION_PATH = "action.sh"
internal const val UNINSTALL_PATH = "uninstall.sh"
internal const val SEPOLICY_PATH = "sepolicy.rule"
internal const val WEB_UI_INDEX_PATH = "webroot/index.html"
internal const val WEB_UI_STYLE_PATH = "webroot/style.css"
internal const val WEB_UI_SCRIPT_PATH = "webroot/script.js"
internal const val INIT_RC_PATH = "initrc/apkesu_module.rc"
internal const val MAX_MODULE_STUDIO_FILES = 64
internal const val MAX_MODULE_STUDIO_FILE_CHARS = 256_000
internal const val MAX_MODULE_STUDIO_PROJECT_CHARS = 2_000_000L
internal const val MAX_AI_PROMPT_CHARS = 8_000
internal const val MAX_AI_RESPONSE_CHARS = 160_000
private const val MAX_IMPORT_FILE_BYTES = MAX_MODULE_STUDIO_FILE_CHARS * 4L
private const val MAX_IMPORT_PROJECT_BYTES = MAX_MODULE_STUDIO_PROJECT_CHARS * 4L
private const val MAX_AI_PROJECT_SNAPSHOT_CHARS = 120_000
private const val MAX_MODULE_PATH_CHARS = 180
private const val MAX_ISSUE_DETAIL_CHARS = 180
private const val MODULE_STUDIO_SCHEMA_VERSION = 1
private const val ZIP_EPOCH_MILLIS = 315_532_800_000L

private val MODULE_ID_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9._-]+$")
private val MODULE_PATH_SEGMENT_REGEX = Regex("[a-zA-Z0-9._@+ -]+")
private val PROPERTY_LINE_REGEX = Regex("[a-zA-Z0-9._-]+=[^\\r\\n]*")
private val BROAD_SEPOLICY_REGEX = Regex("(?m)^\\s*allow\\s+\\S+\\s+\\S+\\s*:\\s*\\S+\\s+\\*\\s*$")
private val AI_CODE_BLOCK_REGEX = Regex("```([^\\r\\n`]*)\\r?\\n([\\s\\S]*?)```", RegexOption.MULTILINE)
private val REQUIRED_MODULE_PROPERTIES = setOf("id", "name", "version", "versionCode", "author", "description")
private val RESERVED_MODULE_PATHS = setOf("disable", "remove", "update", "skip_mount")

private val SERVICE_TEMPLATE = """
    #!/system/bin/sh
    MODDIR=${'$'}{0%/*}

    # Runs in the late_start service stage. Keep this script non-blocking.
    until [ "${'$'}(getprop sys.boot_completed)" = "1" ]; do
      sleep 2
    done

    # Add background service logic below.
""".trimIndent() + "\n"

private val POST_FS_DATA_TEMPLATE = """
    #!/system/bin/sh
    MODDIR=${'$'}{0%/*}

    # Runs before Zygote starts. Avoid long waits, network access, and destructive I/O.
""".trimIndent() + "\n"

private val SYSTEM_PROP_TEMPLATE = """
    # Add resetprop-compatible properties below.
    # persist.example.enabled=1
""".trimIndent() + "\n"

private val ACTION_TEMPLATE = """
    #!/system/bin/sh
    MODDIR=${'$'}{0%/*}

    echo "ApkeSU module action"
    # Add an explicit, user-triggered action below.
""".trimIndent() + "\n"

private val UNINSTALL_TEMPLATE = """
    #!/system/bin/sh
    MODDIR=${'$'}{0%/*}

    # Remove only data created by this module. Never delete shared manager directories.
""".trimIndent() + "\n"

private val SEPOLICY_TEMPLATE = """
    # Add the narrowest required SELinux policy rules.
    # Example: allow my_domain my_type:file { read open getattr };
""".trimIndent() + "\n"

private val INIT_RC_TEMPLATE = """
    # This file is enabled by executable permission set in customize.sh.
    on property:sys.boot_completed=1
        # Add a narrowly scoped init action here.
""".trimIndent() + "\n"

private val WEB_UI_INDEX_TEMPLATE = """
    <!doctype html>
    <html lang="en">
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <title>Module control</title>
      <link rel="stylesheet" href="style.css">
    </head>
    <body>
      <main>
        <h1>Module control</h1>
        <p id="status">Ready</p>
        <button id="refresh" type="button">Refresh</button>
      </main>
      <script src="script.js"></script>
    </body>
    </html>
""".trimIndent() + "\n"

private val WEB_UI_STYLE_TEMPLATE = """
    :root { color-scheme: light dark; font-family: system-ui, sans-serif; }
    body { margin: 0; background: Canvas; color: CanvasText; }
    main { max-width: 42rem; margin: 0 auto; padding: 1.25rem; }
    button { min-height: 2.75rem; padding: 0 1rem; font: inherit; }
""".trimIndent() + "\n"

private val WEB_UI_SCRIPT_TEMPLATE = """
    const status = document.querySelector('#status');
    document.querySelector('#refresh').addEventListener('click', () => {
      status.textContent = `Updated at ${'$'}{new Date().toLocaleTimeString()}`;
    });
""".trimIndent() + "\n"
