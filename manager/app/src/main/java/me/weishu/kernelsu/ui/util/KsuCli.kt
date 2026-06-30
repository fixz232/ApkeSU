package me.weishu.kernelsu.ui.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.os.SystemClock
import android.provider.OpenableColumns
import android.system.Os
import android.util.Log
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.parcelize.Parcelize
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.ksuApp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * @author weishu
 * @date 2023/1/1.
 */
private const val TAG = "KsuCli"
private const val SHELL_JOB_TIMEOUT_MILLIS = 10_000L
private const val ANDROID_16_API = 36
private const val BUSYBOX = "/data/adb/ksu/bin/busybox"
const val HYBRID_MOUNT_MODULE_ID = "hybrid_mount"
const val KPATCH_NEXT_MODULE_ID = "KPatch-Next"
const val BUILTIN_MOUNT_MODE_OVERLAY = "overlay"
const val BUILTIN_MOUNT_MODE_MAGIC = "magic"
const val HIDDEN_PATH_CONFIG_FILE_NAME = "apkesu_hidden_path_config.json"
const val HIDDEN_PATH_CONFIG_MIME_TYPE = "application/json"

private fun getKsuDaemonPath(): String {
    return ksuApp.applicationInfo.nativeLibraryDir + File.separator + "libksud.so"
}

data class FlashResult(val code: Int, val err: String, val showReboot: Boolean) {
    constructor(result: Shell.Result, showReboot: Boolean) : this(result.code, result.err.joinToString("\n"), showReboot)
    constructor(result: Shell.Result) : this(result, result.isSuccess)
}

data class BuiltinMountStatus(
    val moduleId: String = HYBRID_MOUNT_MODULE_ID,
    val moduleName: String = "Hybrid Mount Lite",
    val modulePath: String = "/data/adb/ksu/builtin/hybrid_mount",
    val version: String = "",
    val versionCode: String = "",
    val installed: Boolean = false,
    val enabled: Boolean = false,
    val conflict: String? = null,
    val defaultMode: String = BUILTIN_MOUNT_MODE_OVERLAY,
    val webUi: Boolean = false,
)

data class KPatchNextStatus(
    val moduleId: String = KPATCH_NEXT_MODULE_ID,
    val moduleName: String = "KPatch-Next",
    val modulePath: String = "/data/adb/modules/KPatch-Next",
    val version: String = "",
    val versionCode: String = "",
    val installed: Boolean = false,
    val enabled: Boolean = false,
    val pendingUpdate: Boolean = false,
    val pendingRemove: Boolean = false,
    val webUi: Boolean = false,
    val unresolved: Boolean = false,
    val dataDir: Boolean = false,
    val builtinAvailable: Boolean = false,
    val conflict: String? = null,
)

data class EpkesuHideStatus(
    val enabled: Boolean = false,
)

data class HiddenPathConfigState(
    val targetPaths: List<String> = emptyList(),
    val appPackages: List<String> = emptyList(),
    val useAppScope: Boolean = true,
    val hideDirents: Boolean = true,
    val hideIsolated: Boolean = true,
    val loaded: Boolean = false,
    val currentKmi: String = "",
    val resolvedCount: String = "",
    val activeTargetPaths: String = "",
    val lastLog: String = "",
)

fun HiddenPathConfigState.toConfigJson(): String {
    return JSONObject()
        .put("targetPaths", JSONArray(targetPaths.cleanConfigList()))
        .put("appPackages", JSONArray(appPackages.cleanConfigList()))
        .put("useAppScope", useAppScope)
        .put("hideDirents", hideDirents)
        .put("hideIsolated", hideIsolated)
        .toString(2)
}

fun parseHiddenPathConfigJson(content: String, current: HiddenPathConfigState = HiddenPathConfigState()): HiddenPathConfigState {
    val obj = JSONObject(content)
    return current.copy(
        targetPaths = obj.optJSONArray("targetPaths").toStringList().cleanConfigList(),
        appPackages = obj.optJSONArray("appPackages").toStringList().cleanConfigList(),
        useAppScope = obj.optBoolean("useAppScope", current.useAppScope),
        hideDirents = obj.optBoolean("hideDirents", current.hideDirents),
        hideIsolated = obj.optBoolean("hideIsolated", current.hideIsolated),
    )
}

object KsuCli {
    private val shellLock = Any()
    private var shell: Shell? = null
    private var globalMntShell: Shell? = null

    val SHELL: Shell
        get() = getCachedShell(false)

    val GLOBAL_MNT_SHELL: Shell
        get() = getCachedShell(true)

    private fun getCachedShell(globalMnt: Boolean): Shell = synchronized(shellLock) {
        val current = if (globalMnt) globalMntShell else shell
        if (current != null && current.isUsableRoot()) {
            return@synchronized current
        }

        current?.closeQuietly()
        val newShell = createRootShell(globalMnt)
        if (globalMnt) {
            globalMntShell = newShell
        } else {
            shell = newShell
        }
        newShell
    }

    fun reset(globalMnt: Boolean = false) = synchronized(shellLock) {
        val current = if (globalMnt) globalMntShell else shell
        current?.closeQuietly()
        if (globalMnt) {
            globalMntShell = null
        } else {
            shell = null
        }
    }
}

private fun Shell.isUsableRoot(): Boolean = runCatching { isRoot }.getOrDefault(false)

private fun Shell.closeQuietly() {
    runCatching { close() }
}

fun getRootShell(globalMnt: Boolean = false): Shell {
    return if (globalMnt) KsuCli.GLOBAL_MNT_SHELL else {
        KsuCli.SHELL
    }
}

inline fun <T> withNewRootShell(
    globalMnt: Boolean = false,
    block: Shell.() -> T
): T {
    return createRootShell(globalMnt).use(block)
}

fun Uri.getFileName(context: Context): String? {
    var fileName: String? = null
    val contentResolver: ContentResolver = context.contentResolver
    val cursor: Cursor? = contentResolver.query(this, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }
    return fileName
}

fun createRootShell(globalMnt: Boolean = false): Shell {
    Shell.enableVerboseLogging = BuildConfig.DEBUG
    val builder = Shell.Builder.create()
    val tryKsuShell = {
        if (globalMnt) {
            builder.build(getKsuDaemonPath(), "debug", "su", "-g")
        } else {
            builder.build(getKsuDaemonPath(), "debug", "su")
        }
    }
    val trySuShell = {
        if (globalMnt) {
            builder.build("su", "-mm")
        } else {
            builder.build("su")
        }
    }

    return try {
        tryKsuShell()
    } catch (ksuError: Throwable) {
        Log.w(TAG, "ksu failed: ", ksuError)
        try {
            trySuShell()
        } catch (suError: Throwable) {
            Log.e(TAG, "su failed: ", suError)
            builder.build("sh")
        }
    }
}

fun execKsud(args: String, newShell: Boolean = false): Boolean {
    if (shouldSkipUnsafeKsudCommand()) {
        Log.w(TAG, "skip ksud command without safe root shell: $args")
        return false
    }

    return if (newShell) {
        withNewRootShell {
            ShellUtils.fastCmdResult(this, "${getKsuDaemonPath()} $args")
        }
    } else {
        ShellUtils.fastCmdResult(getRootShell(), "${getKsuDaemonPath()} $args")
    }
}

suspend fun getBuiltinMountStatus(): BuiltinMountStatus = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext BuiltinMountStatus()
    }

    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
            getRootShell().newJob()
                .add("${getKsuDaemonPath()} builtin-mount status")
                .to(stdout, stderr)
                .exec()
        }

        if (result == null) {
            Log.w(TAG, "builtin-mount status timed out")
            KsuCli.reset()
            return@runCatching BuiltinMountStatus()
        }

        if (!result.isSuccess) {
            Log.w(TAG, "builtin-mount status failed: ${stderr.joinToString("\n")}")
            return@runCatching BuiltinMountStatus()
        }

        val obj = JSONObject(stdout.joinToString("\n"))
        val mode = obj.optString("defaultMode", BUILTIN_MOUNT_MODE_OVERLAY)
            .takeIf { it == BUILTIN_MOUNT_MODE_OVERLAY || it == BUILTIN_MOUNT_MODE_MAGIC }
            ?: BUILTIN_MOUNT_MODE_OVERLAY
        BuiltinMountStatus(
            moduleId = obj.optString("moduleId", HYBRID_MOUNT_MODULE_ID),
            moduleName = obj.optString("moduleName", "Hybrid Mount Lite"),
            modulePath = obj.optString("modulePath", "/data/adb/ksu/builtin/hybrid_mount"),
            version = obj.optString("version", ""),
            versionCode = obj.optString("versionCode", ""),
            installed = obj.optBoolean("installed", false),
            enabled = obj.optBoolean("enabled", false),
            conflict = obj.optString("conflict").takeIf { it.isNotBlank() && it != "null" },
            defaultMode = mode,
            webUi = obj.optBoolean("webui", false),
        )
    }.getOrElse {
        Log.w(TAG, "builtin-mount status unavailable", it)
        KsuCli.reset()
        BuiltinMountStatus()
    }
}

fun setBuiltinMountEnabled(enabled: Boolean): Boolean {
    val command = if (enabled) "enable" else "disable"
    return execKsud("builtin-mount $command", true)
}

fun setBuiltinMountDefaultMode(mode: String): Boolean {
    val normalized = if (mode == BUILTIN_MOUNT_MODE_MAGIC) {
        BUILTIN_MOUNT_MODE_MAGIC
    } else {
        BUILTIN_MOUNT_MODE_OVERLAY
    }
    return execKsud("builtin-mount set-default-mode $normalized", true)
}

suspend fun getKPatchNextStatus(): KPatchNextStatus = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext KPatchNextStatus()
    }

    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
            getRootShell().newJob()
                .add("${getKsuDaemonPath()} kpatch-next status")
                .to(stdout, stderr)
                .exec()
        }

        if (result == null) {
            Log.w(TAG, "kpatch-next status timed out")
            KsuCli.reset()
            return@runCatching KPatchNextStatus()
        }

        if (!result.isSuccess) {
            Log.w(TAG, "kpatch-next status failed: ${stderr.joinToString("\n")}")
            return@runCatching KPatchNextStatus()
        }

        val obj = JSONObject(stdout.joinToString("\n"))
        KPatchNextStatus(
            moduleId = obj.optString("moduleId", KPATCH_NEXT_MODULE_ID),
            moduleName = obj.optString("moduleName", "KPatch-Next"),
            modulePath = obj.optString("modulePath", "/data/adb/modules/KPatch-Next"),
            version = obj.optString("version", ""),
            versionCode = obj.optString("versionCode", ""),
            installed = obj.optBoolean("installed", false),
            enabled = obj.optBoolean("enabled", false),
            pendingUpdate = obj.optBoolean("pendingUpdate", false),
            pendingRemove = obj.optBoolean("pendingRemove", false),
            webUi = obj.optBoolean("webui", false),
            unresolved = obj.optBoolean("unresolved", false),
            dataDir = obj.optBoolean("dataDir", false),
            builtinAvailable = obj.optBoolean("builtinAvailable", false),
            conflict = obj.optString("conflict").takeIf { it.isNotBlank() && it != "null" },
        )
    }.getOrElse {
        Log.w(TAG, "kpatch-next status unavailable", it)
        KsuCli.reset()
        KPatchNextStatus()
    }
}

fun setKPatchNextEnabled(enabled: Boolean): Boolean {
    val command = if (enabled) "enable" else "disable"
    return execKsud("kpatch-next $command", true)
}

suspend fun getEpkesuHideStatus(): EpkesuHideStatus = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext EpkesuHideStatus()
    }

    val shell = getRootShell()
    val stdout = ArrayList<String>()
    val stderr = ArrayList<String>()
    val result = shell.newJob()
        .add("${getKsuDaemonPath()} epkesu-hide status")
        .to(stdout, stderr)
        .exec()

    if (!result.isSuccess) {
        Log.w(TAG, "epkesu-hide status failed: ${stderr.joinToString("\n")}")
        return@withContext EpkesuHideStatus()
    }

    runCatching {
        val obj = JSONObject(stdout.joinToString("\n"))
        EpkesuHideStatus(
            enabled = obj.optBoolean("enabled", false),
        )
    }.getOrElse {
        Log.w(TAG, "parse epkesu-hide status failed: ${stdout.joinToString("\n")}", it)
        EpkesuHideStatus()
    }
}

fun setEpkesuHideEnabled(enabled: Boolean): Boolean {
    val command = if (enabled) "enable" else "disable"
    return execKsud("epkesu-hide $command", true)
}

suspend fun getHiddenPathConfig(): HiddenPathConfigState = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext HiddenPathConfigState()
    }

    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
            getRootShell().newJob()
                .add("${getKsuDaemonPath()} pathmask status")
                .to(stdout, stderr)
                .exec()
        }

        if (result == null) {
            Log.w(TAG, "pathmask status timed out")
            KsuCli.reset()
            return@runCatching HiddenPathConfigState()
        }

        if (!result.isSuccess) {
            Log.w(TAG, "pathmask status failed: ${stderr.joinToString("\n")}")
            return@runCatching HiddenPathConfigState()
        }

        val obj = JSONObject(stdout.joinToString("\n"))
        HiddenPathConfigState(
            targetPaths = obj.optJSONArray("targetPaths").toStringList(),
            appPackages = obj.optJSONArray("appPackages").toStringList(),
            useAppScope = obj.optBoolean("useAppScope", true),
            hideDirents = obj.optBoolean("hideDirents", true),
            hideIsolated = obj.optBoolean("hideIsolated", true),
            loaded = obj.optBoolean("loaded", false),
            currentKmi = obj.optString("currentKmi", ""),
            resolvedCount = obj.optString("resolvedCount", ""),
            activeTargetPaths = obj.optString("activeTargetPaths", ""),
            lastLog = obj.optString("lastLog", ""),
        )
    }.getOrElse {
        Log.w(TAG, "pathmask status unavailable", it)
        KsuCli.reset()
        HiddenPathConfigState()
    }
}

suspend fun saveAndApplyHiddenPathConfig(config: HiddenPathConfigState): Boolean = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext false
    }

    runCatching {
        val json = JSONObject()
            .put("targetPaths", JSONArray(config.targetPaths))
            .put("appPackages", JSONArray(config.appPackages))
            .put("useAppScope", config.useAppScope)
            .put("hideDirents", config.hideDirents)
            .put("hideIsolated", config.hideIsolated)
        val importDir = "/data/adb/ksu/pathmask"
        val errFile = "$importDir/import.err"
        val logFile = "$importDir/pathmask.log"
        val ksud = shellQuote(getKsuDaemonPath())
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val importCmd = "$ksud pathmask import-json ${shellQuote(json.toString())}"
        val applyCmd = "$ksud pathmask apply"
        val command = "mkdir -p ${shellQuote(importDir)} && " +
            "($importCmd && $applyCmd) 2> ${shellQuote(errFile)}; " +
            "code=${'$'}?; " +
            "if [ ${'$'}code -ne 0 ]; then " +
            "printf '[manager] pathmask apply command failed (code=%s)\\n' \"${'$'}code\" >> ${shellQuote(logFile)}; " +
            "cat ${shellQuote(errFile)} >> ${shellQuote(logFile)}; " +
            "cat ${shellQuote(errFile)} >&2; " +
            "fi; " +
            "rm -f ${shellQuote(errFile)}; " +
            "[ ${'$'}code -eq 0 ]"
        val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS * 6) {
            getRootShell().newJob()
                .add(command)
                .to(stdout, stderr)
                .exec()
        }

        if (result == null) {
            Log.w(TAG, "pathmask apply timed out")
            KsuCli.reset()
            return@runCatching false
        }

        if (!result.isSuccess) {
            Log.w(TAG, "pathmask apply failed: ${stderr.joinToString("\n")}")
        }
        result.isSuccess
    }.getOrElse {
        Log.w(TAG, "pathmask apply unavailable", it)
        KsuCli.reset()
        false
    }
}

suspend fun getHiddenPathLogs(): String = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext ""
    }

    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
            getRootShell().newJob()
                .add("${getKsuDaemonPath()} pathmask logs")
                .to(stdout, stderr)
                .exec()
        }

        if (result == null) {
            KsuCli.reset()
            return@runCatching ""
        }
        if (result.isSuccess) stdout.joinToString("\n") else stderr.joinToString("\n")
    }.getOrElse {
        Log.w(TAG, "pathmask logs unavailable", it)
        KsuCli.reset()
        ""
    }
}

fun clearHiddenPathLogs(): Boolean {
    return execKsud("pathmask clear-logs", true)
}

fun unloadHiddenPathKernelPaths(): Boolean {
    return execKsud("pathmask unload", true)
}

fun isHiddenPathLkmMode(): Boolean {
    return runCatching {
        withNewRootShell(globalMnt = true) {
            newJob()
                .add("[ -f /pathmask.ko ] || grep -q '^pathmask ' /proc/modules")
                .exec()
                .isSuccess
        }
    }.getOrDefault(false)
}

suspend fun getFeatureStatus(feature: String): String = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext ""
    }

    val shell = getRootShell()
    val out = shell.newJob()
        .add("${getKsuDaemonPath()} feature check $feature").to(ArrayList<String>(), null).exec().out
    out.firstOrNull()?.trim().orEmpty()
}

suspend fun getFeaturePersistValue(feature: String): Long? = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext null
    }

    val shell = getRootShell()
    val out = shell.newJob()
        .add("${getKsuDaemonPath()} feature get --config $feature").to(ArrayList<String>(), null).exec().out
    val valueLine = out.firstOrNull { it.trim().startsWith("Value:") } ?: return@withContext null
    valueLine.substringAfter("Value:").trim().toLongOrNull()
}

fun install() {
    val start = SystemClock.elapsedRealtime()
    val libadbroot = File(ksuApp.applicationInfo.nativeLibraryDir, "libadbroot.so").absolutePath
    val result = execKsud("install --libadbroot $libadbroot", true)
    Log.w(TAG, "install result: $result, cost: ${SystemClock.elapsedRealtime() - start}ms")
}

fun listModules(): String {
    if (shouldSkipUnsafeKsudCommand()) {
        return "[]"
    }

    val shell = getRootShell()

    val result = shell.newJob()
        .add("${getKsuDaemonPath()} module list").to(ArrayList(), ArrayList()).exec()
    if (!result.isSuccess) {
        KsuCli.reset()
        Log.w(TAG, "module list failed: ${result.err.joinToString("\n")}")
        return "[]"
    }
    return result.out.joinToString("\n").ifBlank { "[]" }
}

suspend fun listModulesWithTimeout(timeoutMillis: Long = SHELL_JOB_TIMEOUT_MILLIS): String {
    if (shouldSkipUnsafeKsudCommand()) {
        return "[]"
    }

    val stdout = ArrayList<String>()
    val result = withTimeoutOrNull(timeoutMillis) {
        suspendCancellableCoroutine { cont ->
            val shell = getRootShell()
            shell.newJob()
                .add("${getKsuDaemonPath()} module list")
                .to(stdout, null)
                .submit(Shell.EXECUTOR) { result ->
                    if (cont.isActive) {
                        cont.resume(result)
                    }
                }
        }
    }

    if (result == null) {
        Log.w(TAG, "module list timed out after ${timeoutMillis}ms")
        KsuCli.reset()
        error("module list timed out after ${timeoutMillis}ms")
    }

    if (!result.isSuccess) {
        KsuCli.reset()
        error("module list failed: ${result.err.joinToString("\n")}")
    }

    return result.out.joinToString("\n").ifBlank { "[]" }
}

fun getModuleCount(): Int {
    val result = listModules()
    runCatching {
        val array = JSONArray(result)
        return array.length()
    }.getOrElse { return 0 }
}

fun getSuperuserCount(): Int {
    return Natives.getSuperuserCount()
}

fun toggleModule(id: String, enable: Boolean): Boolean {
    val cmd = if (enable) {
        "module enable $id"
    } else {
        "module disable $id"
    }
    val result = execKsud(cmd, true)
    Log.i(TAG, "$cmd result: $result")
    return result
}

fun undoUninstallModule(id: String): Boolean {
    val cmd = "module undo-uninstall $id"
    val result = execKsud(cmd, true)
    Log.i(TAG, "undo uninstall module $id result: $result")
    return result
}

fun uninstallModule(id: String): Boolean {
    val cmd = "module uninstall $id"
    val result = execKsud(cmd, true)
    Log.i(TAG, "uninstall module $id result: $result")
    return result
}

private fun flashWithIO(
    cmd: String,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): Shell.Result {

    val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStdout(s ?: "")
        }
    }

    val stderrCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStderr(s ?: "")
        }
    }

    return withNewRootShell {
        newJob().add(cmd).to(stdoutCallback, stderrCallback).exec()
    }
}

private fun processUiPrintLine(s: String?): Pair<Int, String?> {
    if (s == null) {
        return Pair(1, null)
    }

    val check1 = s.startsWith("ui_print")
    val trimmed = s.trim()
    val check2 = trimmed.startsWith("ui_print")
    if (!check1 && check2) return Pair(1, null)

    return if (check1) {
        Pair(1, trimmed.drop(8).dropWhile { it.isWhitespace() })
    } else {
        Pair(2, trimmed)
    }
}

private fun flashWithIoAk3(
    cmd: String,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): Shell.Result {

    val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            val (type, text) = processUiPrintLine(s)
            if (type == 1) {
                text?.let(onStdout)
            } else {
                text?.let(onStderr)
            }
        }
    }

    val stderrCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStderr(s ?: "")
        }
    }

    return withNewRootShell {
        newJob().add(cmd).to(stdoutCallback, stderrCallback).exec()
    }
}

private fun copyUriToCache(uri: Uri, fileName: String): File {
    val file = File(ksuApp.cacheDir, fileName)
    val input = ksuApp.contentResolver.openInputStream(uri)
        ?: error("Unable to open selected file: $uri")
    input.use { source ->
        file.outputStream().use { output ->
            source.copyTo(output)
        }
    }
    return file
}

fun flashModule(
    uri: Uri,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    install()

    val file = copyUriToCache(uri, "module.zip")
    try {
        val cmd = "module install ${file.absolutePath}"
        val result = flashWithIO("${getKsuDaemonPath()} $cmd", onStdout, onStderr)
        Log.i("KernelSU", "install module $uri result: $result")

        return FlashResult(result)
    } finally {
        file.delete()
    }
}

fun runModuleAction(
    moduleId: String, onStdout: (String) -> Unit, onStderr: (String) -> Unit
): Shell.Result {
    val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStdout(s ?: "")
        }
    }

    val stderrCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStderr(s ?: "")
        }
    }

    val result = withNewRootShell(true) {
        newJob().add("${getKsuDaemonPath()} module action $moduleId")
            .to(stdoutCallback, stderrCallback).exec()
    }

    Log.i("KernelSU", "Module runAction result: $result")

    return result
}

fun restoreBoot(
    onStdout: (String) -> Unit, onStderr: (String) -> Unit
): FlashResult {
    val result = flashWithIO("${getKsuDaemonPath()} boot-restore -f", onStdout, onStderr)
    return FlashResult(result)
}

fun uninstallPermanently(
    onStdout: (String) -> Unit, onStderr: (String) -> Unit
): FlashResult {
    val result = flashWithIO("${getKsuDaemonPath()} uninstall --package-name ${BuildConfig.APPLICATION_ID}", onStdout, onStderr)
    return FlashResult(result)
}

@Parcelize
sealed class LkmSelection : Parcelable {
    @Parcelize
    data class LkmUri(val uri: Uri) : LkmSelection()

    @Parcelize
    data class KmiString(val value: String) : LkmSelection()

    @Parcelize
    data class PathMaskKmiString(val value: String) : LkmSelection()

    @Parcelize
    data object PathMaskAuto : LkmSelection()

    @Parcelize
    data object KmiNone : LkmSelection()
}

fun installBoot(
    bootUri: Uri?,
    lkm: LkmSelection,
    ota: Boolean,
    partition: String?,
    allowShell: Boolean,
    enableAdb: Boolean,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit,
): FlashResult {
    val bootFile = bootUri?.let { uri -> copyUriToCache(uri, "boot.img") }

    var cmd = "boot-patch"

    cmd += if (bootFile == null) {
        // no boot.img, use -f to flash
        " -f"
    } else {
        " -b ${bootFile.absolutePath}"
    }

    if (allowShell) {
        cmd += " --allow-shell"
    }

    if (enableAdb) {
        cmd += " --enable-adbd"
    }

    if (ota) {
        cmd += " -u"
    }

    var lkmFile: File? = null
    when (lkm) {
        is LkmSelection.LkmUri -> {
            lkmFile = copyUriToCache(lkm.uri, "kernelsu-tmp-lkm.ko")
            cmd += " -m ${lkmFile.absolutePath}"
        }

        is LkmSelection.KmiString -> {
            cmd += " --kmi ${lkm.value}"
        }

        is LkmSelection.PathMaskKmiString -> {
            cmd += " --pathmask-lkm --kmi ${lkm.value}"
        }

        LkmSelection.PathMaskAuto -> {
            cmd += " --pathmask-lkm"
        }

        LkmSelection.KmiNone -> {
            // do nothing
        }
    }

    // output dir
    if (bootFile != null) {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        cmd += " -o $downloadsDir"
    }

    partition?.let { part ->
        cmd += " --partition $part"
    }

    return try {
        val result = flashWithIO("${getKsuDaemonPath()} $cmd", onStdout, onStderr)
        Log.i("KernelSU", "install boot result: ${result.isSuccess}")

        if (result.isSuccess) {
            // Keep /data/adb/ksud available after reboot for both direct flash and
            // manually flashed patched images.
            install()
        }

        // if boot uri is empty, it is direct install, when success, we should show reboot button
        val showReboot = bootUri == null && result.isSuccess
        FlashResult(result, showReboot)
    } finally {
        bootFile?.delete()
        lkmFile?.delete()
    }
}

fun reboot(reason: String = "") {
    if (reason == "soft_reboot") {
        execKsud("soft-reboot", true)
        return
    }
    val shell = getRootShell()
    if (reason == "recovery") {
        // KEYCODE_POWER = 26, hide incorrect "Factory data reset" message
        ShellUtils.fastCmd(shell, "/system/bin/input keyevent 26")
    }
    ShellUtils.fastCmd(shell, "/system/bin/svc power reboot $reason || /system/bin/reboot $reason")
}

fun flashAnyKernelZip(
    uri: Uri,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val tmpFile = copyUriToCache(uri, "anykernel_${timestamp}.zip")

    val destZip = tmpFile.absolutePath
    val destZipName = tmpFile.name
    val destDirFile = File(ksuApp.cacheDir, "anykernel3_${timestamp}")
    val destDir = destDirFile.absolutePath

    val cmd = """
        mkdir -p '$destDir' && \
        $BUSYBOX unzip -p -o '$destZip' "META-INF/com/google/android/update-binary" > '$destDir/update-binary' 2>/dev/null && \
        cp '$destZip' '$destDir/$destZipName' 2>/dev/null || true && \
        $BUSYBOX chmod 755 '$destDir/update-binary' && \
        $BUSYBOX chown root:root '$destDir/update-binary' && \
        (cd '$destDir' && \
            if [ -f './update-binary' ] && grep -q "AnyKernel3" './update-binary'; then \
                AKHOME='$destDir/tmp' $BUSYBOX ash '$destDir/update-binary' 3 1 '$destDir/$destZipName'; \
            else \
                echo 'No installer script found' >&2; exit 1; \
            fi)
    """.trimIndent().replace(Regex("\\s+\\\\\\s*"), " ")

    val result = flashWithIoAk3(cmd, onStdout, onStderr)
    try {
        return FlashResult(result, result.isSuccess)
    } finally {
        runCatching {
            createRootShell(true).use { shell ->
                shell.newJob().add("rm -rf '$destDir' '$destZip'").exec()
            }
        }
    }
}

fun rootAvailable(): Boolean {
    return runCatching {
        val available = getRootShell().isRoot
        if (!available) {
            KsuCli.reset()
        }
        available
    }.getOrDefault(false)
}

private fun shouldSkipUnsafeKsudCommand(): Boolean {
    return Build.VERSION.SDK_INT >= ANDROID_16_API && !rootAvailable()
}

private val fallbackSupportedKmis = listOf(
    "android12-5.10",
    "android13-5.10",
    "android13-5.15",
    "android14-5.15",
    "android14-6.1",
    "android15-6.6",
    "android16-6.12",
)

private val kmiNameRegex = Regex("""^android\d+-\d+(?:\.\d+)?$""")

suspend fun getCurrentKmi(): String = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = "boot-info current-kmi"
    ShellUtils.fastCmd(shell, "${getKsuDaemonPath()} $cmd").trim()
}

suspend fun getSupportedKmis(): List<String> = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = "boot-info supported-kmis"
    val out = shell.newJob().add("${getKsuDaemonPath()} $cmd").to(ArrayList(), null).exec().out
    out.map { it.trim() }
        .filter { it.matches(kmiNameRegex) }
        .distinct()
        .ifEmpty { fallbackSupportedKmis }
}

suspend fun isAbDevice(): Boolean = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = "boot-info is-ab-device"
    ShellUtils.fastCmd(shell, "${getKsuDaemonPath()} $cmd").trim().toBoolean()
}

suspend fun getDefaultPartition(): String = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    if (shell.isRoot) {
        val cmd = "boot-info default-partition"
        ShellUtils.fastCmd(shell, "${getKsuDaemonPath()} $cmd").trim()
    } else {
        if (!Os.uname().release.contains("android12-")) "init_boot" else "boot"
    }
}

suspend fun getSlotSuffix(ota: Boolean): String = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = if (ota) {
        "boot-info slot-suffix --ota"
    } else {
        "boot-info slot-suffix"
    }
    ShellUtils.fastCmd(shell, "${getKsuDaemonPath()} $cmd").trim()
}

suspend fun getAvailablePartitions(): List<String> = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = "boot-info available-partitions"
    val out = shell.newJob().add("${getKsuDaemonPath()} $cmd").to(ArrayList(), null).exec().out
    out.filter { it.isNotBlank() }.map { it.trim() }
}

fun hasMagisk(): Boolean {
    val shell = getRootShell(true)
    val result = shell.newJob().add("which magisk").exec()
    Log.i(TAG, "has magisk: ${result.isSuccess}")
    return result.isSuccess
}

fun isSepolicyValid(rules: String?): Boolean {
    if (rules == null) {
        return true
    }
    val shell = getRootShell()
    val result =
        shell.newJob().add("${getKsuDaemonPath()} sepolicy check '$rules'").to(ArrayList(), null)
            .exec()
    return result.isSuccess
}

fun getSepolicy(pkg: String): String {
    val shell = getRootShell()
    val result =
        shell.newJob().add("${getKsuDaemonPath()} profile get-sepolicy $pkg").to(ArrayList(), null)
            .exec()
    Log.i(TAG, "code: ${result.code}, out: ${result.out}, err: ${result.err}")
    return result.out.joinToString("\n")
}

fun setSepolicy(pkg: String, rules: String): Boolean {
    val shell = getRootShell()
    val result = shell.newJob().add("${getKsuDaemonPath()} profile set-sepolicy $pkg '$rules'")
        .to(ArrayList(), null).exec()
    Log.i(TAG, "set sepolicy result: ${result.code}")
    return result.isSuccess
}

fun listAppProfileTemplates(): List<String> {
    val shell = getRootShell()
    return shell.newJob().add("${getKsuDaemonPath()} profile list-templates").to(ArrayList(), null)
        .exec().out
}

fun getAppProfileTemplate(id: String): String {
    val shell = getRootShell()
    return shell.newJob().add("${getKsuDaemonPath()} profile get-template ${shellQuote(id)}")
        .to(ArrayList(), null).exec().out.joinToString("\n")
}

fun setAppProfileTemplate(id: String, template: String): Boolean {
    val shell = getRootShell()
    val cmd = "${getKsuDaemonPath()} profile set-template ${shellQuote(id)} ${shellQuote(template)}"
    return shell.newJob().add(cmd)
        .to(ArrayList(), null).exec().isSuccess
}

fun deleteAppProfileTemplate(id: String): Boolean {
    val shell = getRootShell()
    return shell.newJob().add("${getKsuDaemonPath()} profile delete-template ${shellQuote(id)}")
        .to(ArrayList(), null).exec().isSuccess
}

private fun shellQuote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) {
        return emptyList()
    }
    return buildList {
        for (index in 0 until length()) {
            optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add)
        }
    }
}

private fun List<String>.cleanConfigList(): List<String> {
    return map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
}

fun forceStopApp(packageName: String, userId: Int? = null) {
    val shell = getRootShell()
    val userArg = userId?.let { " --user $it" } ?: ""
    val result = shell.newJob().add("am force-stop$userArg $packageName").exec()
    Log.i(TAG, "force stop $packageName result: $result")
}

fun launchApp(packageName: String, userId: Int? = null) {
    val shell = getRootShell()
    val userArg = userId?.let { " --user $it" } ?: ""
    val result =
        shell.newJob()
            .add("cmd package resolve-activity --brief$userArg $packageName | tail -n 1 | xargs cmd activity start-activity$userArg -n")
            .exec()
    Log.i(TAG, "launch $packageName result: $result")
}

fun restartApp(packageName: String, userId: Int? = null) {
    forceStopApp(packageName, userId)
    launchApp(packageName, userId)
}
