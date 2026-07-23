package me.weishu.kernelsu.ui.util

import android.content.Context
import androidx.core.content.edit
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.weishu.kernelsu.ksuApp
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.security.SecureRandom
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

const val APP_ID_HEX_LENGTH = 16

private const val APP_ID_ROOT = "/data/adb/apkesu/app-id"
private const val APP_ID_PENDING_DIR = "$APP_ID_ROOT/pending"
private const val APP_ID_BACKUP_DIR = "$APP_ID_ROOT/backups"
private const val APP_ID_STATUS_DIR = "$APP_ID_ROOT/status"
private const val APP_ID_BOOT_SCRIPT = "/data/adb/post-fs-data.d/apkesu-app-id.sh"
private const val APP_ID_PREFS = "app_id_manager"
private const val ROOT_COMMAND_TIMEOUT_MILLIS = 10_000L
private const val MAX_SSAID_XML_BYTES = 8 * 1024 * 1024
private const val ABSENT_BACKUP_VALUE = "__APKESU_ABSENT__"
private const val APP_ID_STAGE_SUCCESS_MARKER = "__APKESU_APP_ID_STAGE_OK__"
private const val APP_ID_STAGE_FAILURE_MARKER = "__APKESU_APP_ID_STAGE_FAILED__:"
private const val APP_ID_FILE_EXISTS_MARKER = "__APKESU_FILE_EXISTS__"
private const val APP_ID_FILE_MISSING_MARKER = "__APKESU_FILE_MISSING__"
private const val ABX_TO_XML = "/system/bin/abx2xml"
private const val XML_TO_ABX = "/system/bin/xml2abx"

enum class AppIdFailure {
    RootUnavailable,
    SettingsFileMissing,
    InvalidSettingsFile,
    InvalidAppId,
    BackupMissing,
    CommandTimeout,
    CommandFailed,
    StagingPreparationFailed,
    BootScriptStagingFailed,
    PendingXmlStagingFailed,
    StagingVerificationFailed,
}

class AppIdException(
    val failure: AppIdFailure,
    message: String = failure.name,
) : IllegalStateException(message)

data class AppIdSnapshot(
    val userId: Int,
    val uid: Int,
    val currentId: String?,
    val pendingId: String?,
    val hasPendingChange: Boolean,
    val hasOriginalBackup: Boolean,
    val originalId: String?,
    val originalWasAbsent: Boolean,
    val backupTimestamp: Long?,
    val lastApplied: String?,
)

data class AppIdLookup(
    val uid: Int,
    val packageName: String,
    val knownPackages: Set<String>,
)

internal data class SsaidEntry(
    val name: String,
    val packageName: String,
    val value: String,
)

internal object SsaidXmlEditor {
    private const val DOCUMENT_ROOT = "apkesu-ssaid-document"

    fun isValidAppId(value: String): Boolean =
        value.length == APP_ID_HEX_LENGTH && value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

    fun normalizeAppId(value: String): String = value.trim().lowercase()

    fun validate(xml: String) {
        parse(xml)
    }

    fun readEntry(xml: String, uid: Int, packageName: String): SsaidEntry? {
        return readEntry(readEntries(xml), uid, packageName)
    }

    fun readEntries(xml: String): List<SsaidEntry> {
        return settingElements(parse(xml)).map { element ->
            SsaidEntry(
                name = element.getAttribute("name"),
                packageName = element.getAttribute("package"),
                value = element.getAttribute("value").lowercase(),
            )
        }
    }

    fun readEntry(entries: List<SsaidEntry>, uid: Int, packageName: String): SsaidEntry? {
        return entries.firstOrNull { it.name == uid.toString() }
            ?: entries.firstOrNull { it.packageName == packageName }
    }

    fun replaceEntry(xml: String, uid: Int, packageName: String, newValue: String?): String {
        val normalizedValue = newValue?.let(::normalizeAppId)
        if (normalizedValue != null && !isValidAppId(normalizedValue)) {
            throw AppIdException(AppIdFailure.InvalidAppId)
        }

        val document = parse(xml)
        val current = findSetting(document, uid, packageName)
        if (normalizedValue == null) {
            current?.parentNode?.removeChild(current)
            return serialize(document)
        }

        val setting = current ?: document.createElement("setting").also { created ->
            val root = settingsElement(document)
            val nextId = settingElements(document)
                .mapNotNull { it.getAttribute("id").toLongOrNull() }
                .maxOrNull()
                ?.plus(1L)
                ?: 1L
            created.setAttribute("id", nextId.toString())
            created.setAttribute("name", uid.toString())
            created.setAttribute("package", packageName)
            created.setAttribute("defaultSysSet", "true")
            root.appendChild(created)
        }
        setting.setAttribute("value", normalizedValue)
        setting.setAttribute("defaultValue", normalizedValue)
        return serialize(document)
    }

    fun hasValueDifferences(first: String, second: String): Boolean =
        valuesByName(parse(first)) != valuesByName(parse(second))

    private fun parse(xml: String): Document {
        val bytes = xml.toByteArray(Charsets.UTF_8)
        if (bytes.isEmpty() || bytes.size > MAX_SSAID_XML_BYTES || xml.contains("<!DOCTYPE", ignoreCase = true)) {
            throw AppIdException(AppIdFailure.InvalidSettingsFile)
        }

        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isExpandEntityReferences = false
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                runCatching {
                    setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "")
                }
                runCatching {
                    setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "")
                }
            }
            val wrapped = "<$DOCUMENT_ROOT>${xmlBody(xml)}</$DOCUMENT_ROOT>"
            factory.newDocumentBuilder()
                .parse(ByteArrayInputStream(wrapped.toByteArray(Charsets.UTF_8)))
                .also(::settingsElement)
        } catch (error: AppIdException) {
            throw error
        } catch (error: Throwable) {
            throw AppIdException(AppIdFailure.InvalidSettingsFile, error.message.orEmpty())
        }
    }

    private fun findSetting(document: Document, uid: Int, packageName: String): Element? {
        val settings = settingElements(document)
        return settings.firstOrNull { it.getAttribute("name") == uid.toString() }
            ?: settings.firstOrNull { it.getAttribute("package") == packageName }
    }

    private fun settingsElement(document: Document): Element {
        val root = document.documentElement
            ?.takeIf { it.tagName == DOCUMENT_ROOT }
            ?: throw AppIdException(AppIdFailure.InvalidSettingsFile)
        val settings = buildList {
            for (index in 0 until root.childNodes.length) {
                val element = root.childNodes.item(index) as? Element ?: continue
                if (element.tagName == "settings") add(element)
            }
        }
        return settings.singleOrNull()
            ?: throw AppIdException(AppIdFailure.InvalidSettingsFile)
    }

    private fun xmlBody(xml: String): String {
        val body = xml.removePrefix("\uFEFF").trimStart()
        if (!body.startsWith("<?xml", ignoreCase = true)) return body
        val declarationEnd = body.indexOf("?>")
        if (declarationEnd < 0) throw AppIdException(AppIdFailure.InvalidSettingsFile)
        return body.substring(declarationEnd + 2)
    }

    private fun settingElements(document: Document): List<Element> {
        val nodes = document.getElementsByTagName("setting")
        return buildList(nodes.length) {
            for (index in 0 until nodes.length) {
                (nodes.item(index) as? Element)?.let(::add)
            }
        }
    }

    private fun valuesByName(document: Document): Map<String, Pair<String, String>> =
        settingElements(document).associate { element ->
            val name = element.getAttribute("name").ifBlank {
                "package:${element.getAttribute("package")}:${element.getAttribute("id")}"
            }
            name to (element.getAttribute("value") to element.getAttribute("defaultValue"))
        }

    private fun serialize(document: Document): String {
        return try {
            val transformer = TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(OutputKeys.ENCODING, Charsets.UTF_8.name())
                setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
                setOutputProperty(OutputKeys.INDENT, "no")
            }
            val root = document.documentElement
                ?.takeIf { it.tagName == DOCUMENT_ROOT }
                ?: throw AppIdException(AppIdFailure.InvalidSettingsFile)
            StringWriter().also { writer ->
                writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                for (index in 0 until root.childNodes.length) {
                    val node = root.childNodes.item(index)
                    if (
                        node.nodeType != Node.ELEMENT_NODE &&
                        node.nodeType != Node.COMMENT_NODE &&
                        node.nodeType != Node.PROCESSING_INSTRUCTION_NODE
                    ) {
                        continue
                    }
                    writer.append('\n')
                    transformer.transform(DOMSource(node), StreamResult(writer))
                }
            }.toString()
        } catch (error: Throwable) {
            throw AppIdException(AppIdFailure.InvalidSettingsFile, error.message.orEmpty())
        }
    }
}

class AppIdManagerRepository(
    context: Context = ksuApp,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(APP_ID_PREFS, Context.MODE_PRIVATE)

    suspend fun getSnapshot(
        uid: Int,
        packageName: String,
        knownPackages: Set<String>,
    ): AppIdSnapshot = withRootShell { shell ->
        readSnapshot(shell, uid, packageName, knownPackages)
    }

    suspend fun getSnapshots(targets: Collection<AppIdLookup>): Map<Int, AppIdSnapshot> {
        val uniqueTargets = targets.distinctBy(AppIdLookup::uid)
        if (uniqueTargets.isEmpty()) return emptyMap()

        return withRootShell { shell ->
            buildMap {
                uniqueTargets.groupBy { userIdForUid(it.uid) }.forEach { (userId, userTargets) ->
                    val currentXml = readRootFile(shell, settingsPath(userId), required = true)
                        ?: throw AppIdException(AppIdFailure.SettingsFileMissing)
                    val pendingXml = readRootFile(shell, pendingPath(userId), required = false)
                    val currentEntries = SsaidXmlEditor.readEntries(currentXml)
                    val pendingEntries = pendingXml?.let(SsaidXmlEditor::readEntries)
                    val lastApplied = readRootFile(
                        shell,
                        "$APP_ID_STATUS_DIR/last_applied_$userId",
                        required = false,
                    )?.trim()?.takeIf(String::isNotBlank)

                    userTargets.forEach { target ->
                        put(
                            target.uid,
                            createSnapshot(
                                userId = userId,
                                uid = target.uid,
                                packageName = target.packageName,
                                knownPackages = target.knownPackages,
                                currentEntries = currentEntries,
                                pendingEntries = pendingEntries,
                                lastApplied = lastApplied,
                            ),
                        )
                    }
                }
            }
        }
    }

    suspend fun stageAppId(
        uid: Int,
        packageName: String,
        knownPackages: Set<String>,
        newValue: String,
    ): AppIdSnapshot {
        val normalized = SsaidXmlEditor.normalizeAppId(newValue)
        if (!SsaidXmlEditor.isValidAppId(normalized)) {
            throw AppIdException(AppIdFailure.InvalidAppId)
        }
        return stageValue(uid, packageName, knownPackages, normalized, saveOriginal = true)
    }

    suspend fun randomReset(
        uid: Int,
        packageName: String,
        knownPackages: Set<String>,
    ): AppIdSnapshot = withRootShell { shell ->
        val snapshot = readSnapshot(shell, uid, packageName, knownPackages)
        val randomValue = generateRandomAppId(
            setOfNotNull(snapshot.currentId, snapshot.pendingId),
        )
        stageValueWithShell(
            shell = shell,
            uid = uid,
            packageName = packageName,
            knownPackages = knownPackages,
            newValue = randomValue,
            saveOriginal = true,
        )
    }

    suspend fun restoreOriginal(
        uid: Int,
        packageName: String,
        knownPackages: Set<String>,
    ): AppIdSnapshot {
        val backup = readOriginalBackup(uid, knownPackages)
            ?: throw AppIdException(AppIdFailure.BackupMissing)
        return stageValue(uid, packageName, knownPackages, backup.value, saveOriginal = false)
    }

    suspend fun cancelPending(
        uid: Int,
        packageName: String,
        knownPackages: Set<String>,
    ): AppIdSnapshot = withRootShell { shell ->
        val userId = userIdForUid(uid)
        val currentXml = readRootFile(shell, settingsPath(userId), required = true)
            ?: throw AppIdException(AppIdFailure.SettingsFileMissing)
        val pendingPath = pendingPath(userId)
        val pendingXml = readRootFile(shell, pendingPath, required = false)
            ?: return@withRootShell readSnapshot(shell, uid, packageName, knownPackages)
        val currentValue = SsaidXmlEditor.readEntry(currentXml, uid, packageName)?.value
        val revertedXml = SsaidXmlEditor.replaceEntry(pendingXml, uid, packageName, currentValue)

        if (SsaidXmlEditor.hasValueDifferences(currentXml, revertedXml)) {
            stageXml(shell, userId, revertedXml)
        } else {
            runRootCommand(shell, "rm -f ${shellQuote(pendingPath)}")
        }
        readSnapshot(shell, uid, packageName, knownPackages)
    }

    fun generateRandomAppId(excludedValues: Set<String> = emptySet()): String =
        createRandomAppId(excludedValues)

    private suspend fun stageValue(
        uid: Int,
        packageName: String,
        knownPackages: Set<String>,
        newValue: String?,
        saveOriginal: Boolean,
    ): AppIdSnapshot = withRootShell { shell ->
        stageValueWithShell(shell, uid, packageName, knownPackages, newValue, saveOriginal)
    }

    private suspend fun stageValueWithShell(
        shell: Shell,
        uid: Int,
        packageName: String,
        knownPackages: Set<String>,
        newValue: String?,
        saveOriginal: Boolean,
    ): AppIdSnapshot {
        val userId = userIdForUid(uid)
        val currentXml = readRootFile(shell, settingsPath(userId), required = true)
            ?: throw AppIdException(AppIdFailure.SettingsFileMissing)
        val baseXml = readRootFile(shell, pendingPath(userId), required = false) ?: currentXml
        val currentValue = SsaidXmlEditor.readEntry(currentXml, uid, packageName)?.value
        val changedXml = SsaidXmlEditor.replaceEntry(baseXml, uid, packageName, newValue)
        if (saveOriginal && readOriginalBackup(uid, knownPackages) == null) {
            saveOriginalBackup(uid, knownPackages, currentValue)
        }
        stageXml(shell, userId, changedXml)
        return readSnapshot(shell, uid, packageName, knownPackages)
    }

    private suspend fun readSnapshot(
        shell: Shell,
        uid: Int,
        packageName: String,
        knownPackages: Set<String>,
    ): AppIdSnapshot {
        val userId = userIdForUid(uid)
        val currentXml = readRootFile(shell, settingsPath(userId), required = true)
            ?: throw AppIdException(AppIdFailure.SettingsFileMissing)
        val pendingXml = readRootFile(shell, pendingPath(userId), required = false)
        val currentEntries = SsaidXmlEditor.readEntries(currentXml)
        val pendingEntries = pendingXml?.let(SsaidXmlEditor::readEntries)
        val lastApplied = readRootFile(shell, "$APP_ID_STATUS_DIR/last_applied_$userId", required = false)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return createSnapshot(
            userId = userId,
            uid = uid,
            packageName = packageName,
            knownPackages = knownPackages,
            currentEntries = currentEntries,
            pendingEntries = pendingEntries,
            lastApplied = lastApplied,
        )
    }

    private fun createSnapshot(
        userId: Int,
        uid: Int,
        packageName: String,
        knownPackages: Set<String>,
        currentEntries: List<SsaidEntry>,
        pendingEntries: List<SsaidEntry>?,
        lastApplied: String?,
    ): AppIdSnapshot {
        val currentId = SsaidXmlEditor.readEntry(currentEntries, uid, packageName)?.value
        val pendingId = pendingEntries?.let { SsaidXmlEditor.readEntry(it, uid, packageName)?.value }
        val backup = readOriginalBackup(uid, knownPackages)
        return AppIdSnapshot(
            userId = userId,
            uid = uid,
            currentId = currentId,
            pendingId = pendingId,
            hasPendingChange = pendingEntries != null && currentId != pendingId,
            hasOriginalBackup = backup != null,
            originalId = backup?.value,
            originalWasAbsent = backup != null && backup.value == null,
            backupTimestamp = backup?.timestamp,
            lastApplied = lastApplied,
        )
    }

    private suspend fun stageXml(shell: Shell, userId: Int, xml: String) {
        SsaidXmlEditor.validate(xml)
        val output = executeScript(shell, buildAppIdStagingScript(userId, xml))
        if (output.success && APP_ID_STAGE_SUCCESS_MARKER in output.stdout) return

        val failure = when {
            output.error.contains("${APP_ID_STAGE_FAILURE_MARKER}prepare") ->
                AppIdFailure.StagingPreparationFailed
            output.error.contains("${APP_ID_STAGE_FAILURE_MARKER}boot_script") ->
                AppIdFailure.BootScriptStagingFailed
            output.error.contains("${APP_ID_STAGE_FAILURE_MARKER}pending_xml") ->
                AppIdFailure.PendingXmlStagingFailed
            output.success -> AppIdFailure.StagingVerificationFailed
            else -> AppIdFailure.CommandFailed
        }
        throw AppIdException(failure, output.error.ifBlank { "exit ${output.code}" })
    }

    private suspend fun readRootFile(shell: Shell, path: String, required: Boolean): String? {
        val output = execute(shell, buildRootFileReadCommand(path))
        if (!output.success) {
            throw AppIdException(AppIdFailure.CommandFailed, output.error.ifBlank { "exit ${output.code}" })
        }

        val existsIndex = output.stdout.indexOf(APP_ID_FILE_EXISTS_MARKER)
        val missingIndex = output.stdout.indexOf(APP_ID_FILE_MISSING_MARKER)
        if (missingIndex >= 0 && (existsIndex < 0 || missingIndex < existsIndex)) {
            if (required) throw AppIdException(AppIdFailure.SettingsFileMissing)
            return null
        }
        if (existsIndex < 0) throw AppIdException(AppIdFailure.CommandFailed, "missing file marker")
        return output.stdout.drop(existsIndex + 1).joinToString("\n")
    }

    private suspend fun runRootCommand(shell: Shell, command: String) {
        val output = execute(shell, command)
        if (!output.success) {
            throw AppIdException(AppIdFailure.CommandFailed, output.error.ifBlank { "exit ${output.code}" })
        }
    }

    private suspend fun execute(shell: Shell, command: String): RootCommandOutput {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(ROOT_COMMAND_TIMEOUT_MILLIS) {
            shell.newJob().add(command).to(stdout, stderr).exec()
        } ?: throw AppIdException(AppIdFailure.CommandTimeout)
        return RootCommandOutput(result.isSuccess, result.code, stdout, stderr.joinToString("\n"))
    }

    private suspend fun executeScript(shell: Shell, script: String): RootCommandOutput {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(ROOT_COMMAND_TIMEOUT_MILLIS) {
            shell.newJob()
                .add(ByteArrayInputStream(script.toByteArray(Charsets.UTF_8)))
                .to(stdout, stderr)
                .exec()
        } ?: throw AppIdException(AppIdFailure.CommandTimeout)
        return RootCommandOutput(result.isSuccess, result.code, stdout, stderr.joinToString("\n"))
    }

    private suspend fun <T> withRootShell(block: suspend (Shell) -> T): T = withContext(Dispatchers.IO) {
        val shell = createRootShell(globalMnt = true)
        try {
            if (!runCatching { shell.isRoot }.getOrDefault(false)) {
                throw AppIdException(AppIdFailure.RootUnavailable)
            }
            block(shell)
        } finally {
            runCatching { shell.close() }
        }
    }

    private fun readOriginalBackup(uid: Int, knownPackages: Set<String>): OriginalBackup? {
        val raw = prefs.getString(backupKey(uid), null) ?: return null
        return runCatching {
            val objectValue = JSONObject(raw)
            val storedPackages = buildSet {
                val array = objectValue.optJSONArray("packages")
                if (array != null) {
                    for (index in 0 until array.length()) {
                        array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
                objectValue.optString("package").takeIf { it.isNotBlank() }?.let(::add)
            }
            if (storedPackages.intersect(knownPackages).isEmpty()) return null
            val storedValue = objectValue.getString("value")
            OriginalBackup(
                value = storedValue.takeUnless { it == ABSENT_BACKUP_VALUE },
                timestamp = objectValue.optLong("timestamp").takeIf { it > 0L },
            )
        }.getOrNull()
    }

    private fun saveOriginalBackup(uid: Int, packageNames: Set<String>, value: String?) {
        val objectValue = JSONObject()
            .put("packages", JSONArray(packageNames.sorted()))
            .put("value", value ?: ABSENT_BACKUP_VALUE)
            .put("timestamp", System.currentTimeMillis())
        prefs.edit(commit = true) { putString(backupKey(uid), objectValue.toString()) }
    }

    private fun backupKey(uid: Int): String = "original_$uid"
    private fun userIdForUid(uid: Int): Int = uid / 100000
    private fun settingsPath(userId: Int): String = "/data/system/users/$userId/settings_ssaid.xml"
    private fun pendingPath(userId: Int): String = "$APP_ID_PENDING_DIR/settings_ssaid_$userId.xml"

    private data class RootCommandOutput(
        val success: Boolean,
        val code: Int,
        val stdout: List<String>,
        val error: String,
    )

    private data class OriginalBackup(
        val value: String?,
        val timestamp: Long?,
    )
}

internal fun createRandomAppId(
    excludedValues: Set<String> = emptySet(),
    secureRandom: SecureRandom = SecureRandom(),
): String {
    val excluded = excludedValues
        .map(SsaidXmlEditor::normalizeAppId)
        .filter(SsaidXmlEditor::isValidAppId)
        .toHashSet()
    while (true) {
        val bytes = ByteArray(APP_ID_HEX_LENGTH / 2)
        secureRandom.nextBytes(bytes)
        val value = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        if (value !in excluded) return value
    }
}

internal fun buildRootFileReadCommand(path: String): String =
    "if [ -f ${shellQuote(path)} ]; then " +
        "printf '$APP_ID_FILE_EXISTS_MARKER\\n'; " +
        "if [ \"${'$'}(dd if=${shellQuote(path)} bs=1 count=3 2>/dev/null)\" = 'ABX' ]; then " +
        "if [ -x ${shellQuote(ABX_TO_XML)} ]; then " +
        "${shellQuote(ABX_TO_XML)} ${shellQuote(path)} -; " +
        "else printf 'abx2xml unavailable\\n' >&2; false; fi; " +
        "else cat ${shellQuote(path)}; fi; " +
        "else printf '$APP_ID_FILE_MISSING_MARKER\\n'; fi"

internal fun buildAppIdStagingScript(userId: Int, xml: String): String {
    val pending = "$APP_ID_PENDING_DIR/settings_ssaid_$userId.xml"
    val bootTemp = "$APP_ID_BOOT_SCRIPT.tmp"
    val pendingTemp = "$pending.tmp"
    val bootPayload = POST_FS_DATA_SCRIPT.withTrailingNewline()
    val xmlPayload = xml.withTrailingNewline()
    val bootDelimiter = heredocDelimiter("BOOT", bootPayload)
    val xmlDelimiter = heredocDelimiter("XML", xmlPayload)
    val bootBytes = bootPayload.toByteArray(Charsets.UTF_8).size
    val xmlBytes = xmlPayload.toByteArray(Charsets.UTF_8).size

    return buildString {
        appendLine("umask 077")
        appendLine("stage_fail() {")
        appendLine("    rm -f ${shellQuote(bootTemp)} ${shellQuote(pendingTemp)}")
        appendLine("    printf '$APP_ID_STAGE_FAILURE_MARKER%s\\n' \"${'$'}1\" >&2")
        appendLine("    return 1")
        appendLine("}")
        appendLine("apkesu_stage_main() {")
        appendLine(
            "mkdir -p ${shellQuote(APP_ID_PENDING_DIR)} ${shellQuote(APP_ID_BACKUP_DIR)} " +
                "${shellQuote(APP_ID_STATUS_DIR)} /data/adb/post-fs-data.d || " +
                "{ stage_fail prepare; return 1; }",
        )
        appendLine(
            "chmod 0700 ${shellQuote(APP_ID_ROOT)} ${shellQuote(APP_ID_PENDING_DIR)} " +
                "${shellQuote(APP_ID_BACKUP_DIR)} ${shellQuote(APP_ID_STATUS_DIR)} || " +
                "{ stage_fail prepare; return 1; }",
        )
        appendHeredoc(bootTemp, bootPayload, bootDelimiter, "boot_script")
        appendLine(
            "[ \"${'$'}(wc -c < ${shellQuote(bootTemp)} 2>/dev/null)\" = \"$bootBytes\" ] || " +
                "{ stage_fail boot_script; return 1; }",
        )
        appendHeredoc(pendingTemp, xmlPayload, xmlDelimiter, "pending_xml")
        appendLine(
            "[ \"${'$'}(wc -c < ${shellQuote(pendingTemp)} 2>/dev/null)\" = \"$xmlBytes\" ] || " +
                "{ stage_fail pending_xml; return 1; }",
        )
        appendLine(
            "chmod 0700 ${shellQuote(bootTemp)} || { stage_fail boot_script; return 1; }",
        )
        appendLine(
            "chmod 0600 ${shellQuote(pendingTemp)} || { stage_fail pending_xml; return 1; }",
        )
        appendLine(
            "mv -f ${shellQuote(bootTemp)} ${shellQuote(APP_ID_BOOT_SCRIPT)} || " +
                "{ stage_fail boot_script; return 1; }",
        )
        appendLine(
            "mv -f ${shellQuote(pendingTemp)} ${shellQuote(pending)} || " +
                "{ stage_fail pending_xml; return 1; }",
        )
        appendLine("if command -v restorecon >/dev/null 2>&1; then")
        appendLine(
            "    restorecon ${shellQuote(APP_ID_BOOT_SCRIPT)} ${shellQuote(pending)} " +
                ">/dev/null 2>&1 || true",
        )
        appendLine("fi")
        appendLine("printf '$APP_ID_STAGE_SUCCESS_MARKER\\n'")
        appendLine("}")
        appendLine("apkesu_stage_main")
    }
}

private fun StringBuilder.appendHeredoc(
    target: String,
    payload: String,
    delimiter: String,
    failureStage: String,
) {
    appendLine(
        "cat > ${shellQuote(target)} <<'$delimiter' || " +
            "{ stage_fail $failureStage; return 1; }",
    )
    append(payload)
    appendLine(delimiter)
}

private fun String.withTrailingNewline(): String = if (endsWith('\n')) this else "$this\n"

private fun heredocDelimiter(label: String, payload: String): String {
    var delimiter = "__APKESU_${label}_EOF__"
    val lines = payload.lineSequence().toSet()
    while (delimiter in lines) delimiter += "_"
    return delimiter
}

private fun shellQuote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"

private val POST_FS_DATA_SCRIPT = """
    #!/system/bin/sh
    BASE=/data/adb/apkesu/app-id
    PENDING="${'$'}BASE/pending"
    BACKUPS="${'$'}BASE/backups"
    STATUS="${'$'}BASE/status"

    is_abx_file() {
        [ "${'$'}(dd if="${'$'}1" bs=1 count=3 2>/dev/null)" = "ABX" ]
    }

    mkdir -p "${'$'}BACKUPS" "${'$'}STATUS"

    for staged in "${'$'}PENDING"/settings_ssaid_*.xml; do
        [ -f "${'$'}staged" ] || continue
        name="${'$'}{staged##*/}"
        user="${'$'}{name#settings_ssaid_}"
        user="${'$'}{user%.xml}"
        case "${'$'}user" in
            ''|*[!0-9]*) continue ;;
        esac

        target="/data/system/users/${'$'}user/settings_ssaid.xml"
        [ -s "${'$'}target" ] || continue
        [ -s "${'$'}staged" ] || continue
        grep -q '<settings' "${'$'}staged" 2>/dev/null || continue
        grep -q '</settings>' "${'$'}staged" 2>/dev/null || continue

        first_backup="${'$'}BACKUPS/settings_ssaid_${'$'}{user}_original.xml"
        last_backup="${'$'}BACKUPS/settings_ssaid_${'$'}{user}_last.xml"
        [ -f "${'$'}first_backup" ] || cp -p "${'$'}target" "${'$'}first_backup" 2>/dev/null
        cp -p "${'$'}target" "${'$'}last_backup" 2>/dev/null || continue

        temp="${'$'}{target}.apkesu.${'$'}${'$'}"
        cp -p "${'$'}target" "${'$'}temp" 2>/dev/null || continue
        if is_abx_file "${'$'}target"; then
            [ -x "$XML_TO_ABX" ] || { rm -f "${'$'}temp"; continue; }
            if ! "$XML_TO_ABX" "${'$'}staged" - > "${'$'}temp" 2>/dev/null || ! is_abx_file "${'$'}temp"; then
                rm -f "${'$'}temp"
                continue
            fi
        else
            if ! cat "${'$'}staged" > "${'$'}temp"; then
                rm -f "${'$'}temp"
                continue
            fi
        fi
        chmod 0600 "${'$'}temp" 2>/dev/null
        if command -v restorecon >/dev/null 2>&1; then
            restorecon "${'$'}temp" >/dev/null 2>&1
        fi
        if mv -f "${'$'}temp" "${'$'}target"; then
            rm -f "${'$'}staged"
            date '+%Y-%m-%d %H:%M:%S' > "${'$'}STATUS/last_applied_${'$'}user"
        else
            rm -f "${'$'}temp"
        fi
    done
""".trimIndent() + "\n"
