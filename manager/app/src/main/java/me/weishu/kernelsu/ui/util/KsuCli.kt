package me.weishu.kernelsu.ui.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.os.Process
import android.os.SystemClock
import android.provider.OpenableColumns
import android.system.Os
import android.util.Base64
import android.util.Log
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
const val CPU_SPOOF_PROPERTY_VALUE_LIMIT = 91
private val managerRegistrationLock = Any()
const val HYBRID_MOUNT_MODULE_ID = "hybrid_mount"
const val KPATCH_NEXT_MODULE_ID = "KPatch-Next"
const val BUILTIN_MOUNT_MODE_OVERLAY = "overlay"
const val BUILTIN_MOUNT_MODE_MAGIC = "magic"
const val BUILTIN_MOUNT_VARIANT_LITE = "lite"
const val BUILTIN_MOUNT_VARIANT_FULL = "full"
const val HIDDEN_PATH_CONFIG_FILE_NAME = "apkesu_hidden_path_config.json"
const val HIDDEN_PATH_CONFIG_MIME_TYPE = "application/json"
private const val SUSFS_PATH_CONFIG_DIR = "/data/adb/ksu/susfs"
private const val SUSFS_PATH_CONFIG_FILE = "$SUSFS_PATH_CONFIG_DIR/paths.txt"
private const val SUSFS_PATH_SERVICE_FILE = "/data/adb/service.d/98-apkesu-susfs-paths.sh"
private const val GRAPHICS_RENDERER_DIR = "/data/adb/apkesu/graphics_renderer"
private const val GRAPHICS_RENDERER_MODE_FILE = "$GRAPHICS_RENDERER_DIR/mode"
private const val GRAPHICS_RENDERER_BACKUP_MARKER = "$GRAPHICS_RENDERER_DIR/backup_complete"
private const val GRAPHICS_RENDERER_ORIGINAL_RENDERER = "$GRAPHICS_RENDERER_DIR/original_renderer"
private const val GRAPHICS_RENDERER_ORIGINAL_DISABLE = "$GRAPHICS_RENDERER_DIR/original_disable_vulkan"
private const val GRAPHICS_RENDERER_RESTART_MARKER = "$GRAPHICS_RENDERER_DIR/restart_required"
private const val GRAPHICS_RENDERER_SERVICE = "/data/adb/service.d/99-apkesu-graphics-renderer.sh"
private const val GRAPHICS_RENDERER_SERVICE_ASSET = "graphics_renderer_service.sh"
private const val GRAPHICS_RENDERER_VERIFICATION_ATTEMPTS = 8
private const val GRAPHICS_RENDERER_VERIFICATION_DELAY_MILLIS = 250L

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
    val variant: String = BUILTIN_MOUNT_VARIANT_LITE,
    val webUi: Boolean = false,
    val sourceUrl: String = "",
    val archiveSha256: String = "",
    val lkmCount: Int = 0,
    val supportedKmis: List<String> = emptyList(),
    val currentKmi: String = "",
    val compatibility: String = "unknown",
    val lkmPurpose: String = "",
    val apkeSuRootDriver: Boolean = false,
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
    val configured: Boolean = false,
    val applied: Boolean = false,
)

data class CpuSpoofStatus(
    val supported: Boolean = false,
    val configured: Boolean = false,
    val enabled: Boolean = false,
    val applied: Boolean = false,
    val current: String = "",
    val target: String = "",
    val original: String = "",
    val manufacturer: String = "",
    val platform: String = "",
    val error: String = "",
)

data class CpuSpoofCommandResult(
    val success: Boolean,
    val error: String = "",
)

internal fun mergeCpuSpoofStatus(
    previous: CpuSpoofStatus,
    refreshed: CpuSpoofStatus,
): CpuSpoofStatus {
    val hasPayload = refreshed.supported ||
        refreshed.configured ||
        refreshed.enabled ||
        refreshed.applied ||
        refreshed.current.isNotBlank() ||
        refreshed.target.isNotBlank() ||
        refreshed.original.isNotBlank() ||
        refreshed.manufacturer.isNotBlank() ||
        refreshed.platform.isNotBlank()
    return if (refreshed.error.isNotBlank() && !hasPayload) {
        previous.copy(error = refreshed.error)
    } else {
        refreshed
    }
}

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

data class HiddenPathVisibilityResult(
    val uid: Int = -1,
    val path: String = "",
    val status: String = "probe_failed",
    val visible: Boolean = false,
    val rootExists: Boolean = false,
    val moduleLoaded: Boolean = false,
    val resolvedCount: String = "",
    val error: String = "",
)

data class SusfsPathConfigState(
    val available: Boolean = false,
    val toolPath: String = "",
    val paths: List<String> = emptyList(),
    val error: String = "",
)

data class SusfsPathApplyResult(
    val success: Boolean = false,
    val appliedCount: Int = 0,
    val requiresReboot: Boolean = false,
    val error: String = "",
)

data class RootDiagnosticInfo(
    val driverVersion: Int = 0,
    val kernelModuleLoaded: Boolean = false,
    val ksuRootShell: Boolean = false,
    val fallbackRootShell: Boolean = false,
    val managerRegistered: Boolean = false,
    val managerPackage: String = BuildConfig.APPLICATION_ID,
    val managerUid: Int = -1,
    val kernelUapi: Int = 0,
    val managerUapi: Int = 0,
    val packagedKsudVersion: String = "",
    val installedKsudVersion: String = "",
    val currentKmi: String = "",
    val currentSlot: String = "",
    val workMode: String = "unknown",
    val hiddenPathLkm: Boolean = false,
)

data class InstalledKsudStatus(
    val present: Boolean = false,
    val versionCode: Int? = null,
)

data class RescueImageState(
    val name: String = "",
    val label: String = "",
    val partition: String = "",
    val image: String = "",
    val required: Boolean = false,
    val custom: Boolean = false,
    val exists: Boolean = false,
    val size: Long = 0,
    val partitionSize: Long = 0,
    val sha256: String = "",
    val sha256Ok: Boolean = true,
    val sizeOk: Boolean = true,
    val otherSlot: Boolean = false,
    val restore: Boolean = true,
)

data class RescueConfigState(
    val includeDtbo: Boolean = false,
    val includeVbmeta: Boolean = false,
    val backupOtherSlot: Boolean = false,
    val allowDangerousAutoRestore: Boolean = false,
    val customPartitions: Map<String, String> = emptyMap(),
)

data class RescueStatus(
    val enabled: Boolean = false,
    val config: RescueConfigState = RescueConfigState(),
    val images: List<RescueImageState> = emptyList(),
    val bootCount: Int = 0,
    val autoRestoreAttempts: Int = 0,
    val pendingBoot: Boolean = false,
    val currentSlot: String = "",
    val bootMode: String = "",
    val device: String = "",
    val manifestCreatedAt: String = "",
    val lastRestoreDone: Boolean = false,
    val ready: Boolean = false,
    val readyReason: String = "",
    val log: String = "",
) {
    val requiredReady: Boolean
        get() = ready
}

data class RescueTestReport(
    val ok: Boolean = false,
    val reason: String = "",
    val text: String = "",
)

fun RescueConfigState.toConfigJson(): String {
    val custom = JSONObject()
    customPartitions.forEach { (key, value) ->
        if (value.isNotBlank()) {
            custom.put(key, value)
        }
    }
    return JSONObject()
        .put("includeDtbo", includeDtbo)
        .put("includeVbmeta", includeVbmeta)
        .put("backupOtherSlot", backupOtherSlot)
        .put("allowDangerousAutoRestore", allowDangerousAutoRestore)
        .put("customPartitions", custom)
        .toString()
}

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
    return runCatching {
        context.contentResolver.query(
            this,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use null
            }
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }
    }.getOrNull()
}

fun createRootShell(globalMnt: Boolean = false): Shell {
    Shell.enableVerboseLogging = BuildConfig.DEBUG
    val builder = Shell.Builder.create()
    fun buildRootShellOrThrow(label: String, vararg commands: String): Shell {
        val shell = builder.build(*commands)
        if (shell.isUsableRoot()) {
            return shell
        }
        shell.closeQuietly()
        error("$label shell is not root")
    }

    val tryKsuShell = {
        if (globalMnt) {
            buildRootShellOrThrow("ksu", getKsuDaemonPath(), "debug", "su", "-g")
        } else {
            buildRootShellOrThrow("ksu", getKsuDaemonPath(), "debug", "su")
        }
    }
    val trySuShell = {
        if (globalMnt) {
            buildRootShellOrThrow("su", "su", "-mm")
        } else {
            buildRootShellOrThrow("su", "su")
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

fun execKsud(
    args: String,
    newShell: Boolean = false,
    globalMnt: Boolean = false,
): Boolean {
    if (shouldSkipUnsafeKsudCommand()) {
        Log.w(TAG, "skip ksud command without safe root shell: $args")
        return false
    }

    return if (newShell) {
        withNewRootShell(globalMnt = globalMnt) {
            ShellUtils.fastCmdResult(this, "${shellQuote(getKsuDaemonPath())} $args")
        }
    } else {
        ShellUtils.fastCmdResult(
            getRootShell(globalMnt),
            "${shellQuote(getKsuDaemonPath())} $args",
        )
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
        val variant = obj.optString("variant", BUILTIN_MOUNT_VARIANT_LITE)
            .takeIf { it == BUILTIN_MOUNT_VARIANT_LITE || it == BUILTIN_MOUNT_VARIANT_FULL }
            ?: BUILTIN_MOUNT_VARIANT_LITE
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
            variant = variant,
            webUi = obj.optBoolean("webui", false),
            sourceUrl = obj.optString("sourceUrl", ""),
            archiveSha256 = obj.optString("archiveSha256", ""),
            lkmCount = obj.optInt("lkmCount", 0),
            supportedKmis = obj.optJSONArray("supportedKmis").toStringList(),
            currentKmi = obj.optString("currentKmi", ""),
            compatibility = obj.optString("compatibility", "unknown"),
            lkmPurpose = obj.optString("lkmPurpose", ""),
            apkeSuRootDriver = obj.optBoolean("apkesuRootDriver", false),
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

fun setBuiltinMountVariant(variant: String): Boolean {
    val normalized = if (variant == BUILTIN_MOUNT_VARIANT_FULL) {
        BUILTIN_MOUNT_VARIANT_FULL
    } else {
        BUILTIN_MOUNT_VARIANT_LITE
    }
    return execKsud("builtin-mount set-variant $normalized", true)
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

    val stdout = ArrayList<String>()
    val stderr = ArrayList<String>()
    val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
        getRootShell().newJob()
            .add("${getKsuDaemonPath()} epkesu-hide status")
            .to(stdout, stderr)
            .exec()
    }

    if (result == null) {
        Log.w(TAG, "epkesu-hide status timed out")
        KsuCli.reset()
        return@withContext EpkesuHideStatus()
    }

    if (!result.isSuccess) {
        Log.w(TAG, "epkesu-hide status failed: ${stderr.joinToString("\n")}")
        return@withContext EpkesuHideStatus()
    }

    runCatching {
        val obj = JSONObject(stdout.joinToString("\n"))
        EpkesuHideStatus(
            enabled = obj.optBoolean("enabled", false),
            configured = obj.optBoolean("configured", obj.optBoolean("enabled", false)),
            applied = obj.optBoolean("applied", obj.optBoolean("enabled", false)),
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

fun isCpuSpoofModelValid(model: String): Boolean {
    if (model.any { it.isISOControl() }) return false
    val value = model.trim()
    return value.isNotEmpty() &&
        !value.startsWith('-') &&
        value.toByteArray(Charsets.UTF_8).size <= CPU_SPOOF_PROPERTY_VALUE_LIMIT
}

suspend fun getCpuSpoofStatus(): CpuSpoofStatus = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext CpuSpoofStatus(error = "root_unavailable")
    }

    val stdout = ArrayList<String>()
    val stderr = ArrayList<String>()
    val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
        getRootShell().newJob()
            .add("${shellQuote(getKsuDaemonPath())} cpu-spoof status")
            .to(stdout, stderr)
            .exec()
    }

    if (result == null) {
        Log.w(TAG, "cpu-spoof status timed out")
        KsuCli.reset()
        return@withContext CpuSpoofStatus(error = "timeout")
    }

    if (!result.isSuccess) {
        val error = stderr.joinToString("\n").trim().ifBlank { "status_failed" }
        Log.w(TAG, "cpu-spoof status failed: $error")
        return@withContext CpuSpoofStatus(error = error)
    }

    runCatching {
        val obj = JSONObject(stdout.joinToString("\n"))
        CpuSpoofStatus(
            supported = obj.optBoolean("supported", false),
            configured = obj.optBoolean("configured", false),
            enabled = obj.optBoolean("enabled", false),
            applied = obj.optBoolean("applied", false),
            current = obj.optString("current", ""),
            target = obj.optString("target", ""),
            original = obj.optString("original", ""),
            manufacturer = obj.optString("manufacturer", ""),
            platform = obj.optString("platform", ""),
            error = obj.optString("error", ""),
        )
    }.getOrElse {
        Log.w(TAG, "parse cpu-spoof status failed: ${stdout.joinToString("\n")}", it)
        CpuSpoofStatus(error = "parse_failed")
    }
}

suspend fun saveCpuSpoofTarget(model: String): CpuSpoofCommandResult {
    if (!isCpuSpoofModelValid(model)) {
        return CpuSpoofCommandResult(false, "invalid_cpu_model")
    }
    return runCpuSpoofCommand("configure --model ${shellQuote(model.trim())}")
}

suspend fun setCpuSpoofEnabled(enabled: Boolean): CpuSpoofCommandResult {
    return runCpuSpoofCommand(if (enabled) "enable" else "disable")
}

suspend fun restoreDefaultCpuSpoof(): CpuSpoofCommandResult {
    return runCpuSpoofCommand("restore-default")
}

suspend fun getGraphicsRendererStatus(): GraphicsRendererStatus = withContext(Dispatchers.IO) {
    if (!rootAvailable()) {
        return@withContext GraphicsRendererStatus(error = "root_unavailable")
    }

    val stdout = ArrayList<String>()
    val stderr = ArrayList<String>()
    val command = """
        renderer="${'$'}(getprop debug.hwui.renderer 2>/dev/null)"
        disable_vulkan="${'$'}(getprop debug.hwui.disable_vulkan 2>/dev/null)"
        egl_driver="${'$'}(getprop ro.hardware.egl 2>/dev/null)"
        hardware_vulkan="${'$'}(getprop ro.hardware.vulkan 2>/dev/null)"
        vulkan_feature="${'$'}(pm list features 2>/dev/null | grep 'android.hardware.vulkan.level' | head -n 1)"
        vulkan_driver=""
        for candidate in /vendor/lib64/hw/vulkan.*.so /vendor/lib/hw/vulkan.*.so /system/vendor/lib64/hw/vulkan.*.so /system/vendor/lib/hw/vulkan.*.so; do
          if [ -e "${'$'}candidate" ]; then vulkan_driver="${'$'}candidate"; break; fi
        done
        configured_mode="${'$'}(cat $GRAPHICS_RENDERER_MODE_FILE 2>/dev/null)"
        original_renderer="${'$'}(cat $GRAPHICS_RENDERER_ORIGINAL_RENDERER 2>/dev/null)"
        original_disable_vulkan="${'$'}(cat $GRAPHICS_RENDERER_ORIGINAL_DISABLE 2>/dev/null)"
        [ -f $GRAPHICS_RENDERER_BACKUP_MARKER ] && backup_available=1 || backup_available=0
        [ -x $GRAPHICS_RENDERER_SERVICE ] && persistent=1 || persistent=0
        [ -f $GRAPHICS_RENDERER_RESTART_MARKER ] && restart_required=1 || restart_required=0
        printf 'renderer=%s\n' "${'$'}renderer"
        printf 'disable_vulkan=%s\n' "${'$'}disable_vulkan"
        printf 'egl_driver=%s\n' "${'$'}egl_driver"
        printf 'hardware_vulkan=%s\n' "${'$'}hardware_vulkan"
        printf 'vulkan_feature=%s\n' "${'$'}vulkan_feature"
        printf 'vulkan_driver=%s\n' "${'$'}vulkan_driver"
        printf 'configured_mode=%s\n' "${'$'}configured_mode"
        printf 'original_renderer=%s\n' "${'$'}original_renderer"
        printf 'original_disable_vulkan=%s\n' "${'$'}original_disable_vulkan"
        printf 'backup_available=%s\n' "${'$'}backup_available"
        printf 'persistent=%s\n' "${'$'}persistent"
        printf 'restart_required=%s\n' "${'$'}restart_required"
    """.trimIndent()
    val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
        getRootShell().newJob().add(command).to(stdout, stderr).exec()
    }
    if (result == null) {
        KsuCli.reset()
        return@withContext GraphicsRendererStatus(error = "timeout")
    }
    if (!result.isSuccess) {
        return@withContext GraphicsRendererStatus(
            rootAvailable = true,
            error = stderr.joinToString("\n").trim().ifBlank { "status_failed" },
        )
    }
    parseGraphicsRendererStatus(stdout)
}

suspend fun setGraphicsRendererMode(
    mode: GraphicsRendererMode,
    persistent: Boolean,
): GraphicsRendererCommandResult = withContext(Dispatchers.IO) {
    if (mode == GraphicsRendererMode.Custom) {
        return@withContext GraphicsRendererCommandResult(false, error = "invalid_mode")
    }
    val before = getGraphicsRendererStatus()
    if (!before.rootAvailable) {
        return@withContext GraphicsRendererCommandResult(false, before, before.error.ifBlank { "root_unavailable" })
    }
    if (mode == GraphicsRendererMode.Vulkan && !before.vulkanSupported) {
        return@withContext GraphicsRendererCommandResult(false, before, "vulkan_unsupported")
    }
    if (mode == GraphicsRendererMode.SystemDefault) {
        return@withContext restoreGraphicsRendererDefault(before)
    }

    val serviceBase64 = runCatching {
        ksuApp.assets.open(GRAPHICS_RENDERER_SERVICE_ASSET).use { input ->
            Base64.encodeToString(input.readBytes(), Base64.NO_WRAP)
        }
    }.getOrElse {
        return@withContext GraphicsRendererCommandResult(false, before, "service_asset_missing")
    }

    if (!before.backupAvailable) {
        val backup = runGraphicsRendererCommands(
            listOf(
                "mkdir -p $GRAPHICS_RENDERER_DIR && chmod 0700 $GRAPHICS_RENDERER_DIR",
                atomicWriteCommand(GRAPHICS_RENDERER_ORIGINAL_RENDERER, before.rendererProperty),
                atomicWriteCommand(GRAPHICS_RENDERER_ORIGINAL_DISABLE, before.disableVulkanProperty),
                ": > $GRAPHICS_RENDERER_BACKUP_MARKER && chmod 0600 $GRAPHICS_RENDERER_BACKUP_MARKER",
            )
        )
        if (!backup.first) {
            return@withContext GraphicsRendererCommandResult(false, before, backup.second.ifBlank { "backup_failed" })
        }
    }

    val applied = applyGraphicsRendererRuntime(mode)
    if (!applied.first) {
        rollbackGraphicsRendererRuntime(before)
        return@withContext GraphicsRendererCommandResult(
            false,
            getGraphicsRendererStatus(),
            applied.second.ifBlank { "apply_failed" },
        )
    }
    val runtimeStatus = awaitGraphicsRendererStatus { it.matchesRuntimeMode(mode) }
    if (!runtimeStatus.matchesRuntimeMode(mode)) {
        logGraphicsRendererVerificationFailure("runtime", mode, runtimeStatus)
        rollbackGraphicsRendererRuntime(before)
        return@withContext GraphicsRendererCommandResult(
            false,
            getGraphicsRendererStatus(),
            "runtime_verification_failed",
        )
    }

    val committed = writeGraphicsRendererConfiguration(mode, persistent, serviceBase64, restartRequired = true)
    if (!committed.first) {
        rollbackGraphicsRendererRuntime(before)
        restorePreviousGraphicsRendererConfiguration(before, serviceBase64)
        return@withContext GraphicsRendererCommandResult(
            false,
            getGraphicsRendererStatus(),
            committed.second.ifBlank { "config_write_failed" },
        )
    }

    val finalStatus = awaitGraphicsRendererStatus { status ->
        status.configuredMode == mode &&
            status.persistent == persistent &&
            status.matchesRuntimeMode(mode)
    }
    val verified = finalStatus.configuredMode == mode &&
        finalStatus.persistent == persistent &&
        finalStatus.matchesRuntimeMode(mode)
    if (!verified) {
        logGraphicsRendererVerificationFailure("final", mode, finalStatus)
        rollbackGraphicsRendererRuntime(before)
        restorePreviousGraphicsRendererConfiguration(before, serviceBase64)
        return@withContext GraphicsRendererCommandResult(
            false,
            getGraphicsRendererStatus(),
            "final_verification_failed",
        )
    }
    GraphicsRendererCommandResult(true, finalStatus)
}

private suspend fun restoreGraphicsRendererDefault(
    before: GraphicsRendererStatus,
): GraphicsRendererCommandResult {
    if (!before.configured && !before.backupAvailable) {
        val cleanup = runGraphicsRendererCommands(
            listOf(
                "rm -f $GRAPHICS_RENDERER_SERVICE $GRAPHICS_RENDERER_SERVICE.tmp",
                "rm -f $GRAPHICS_RENDERER_MODE_FILE $GRAPHICS_RENDERER_ORIGINAL_RENDERER " +
                    "$GRAPHICS_RENDERER_ORIGINAL_DISABLE $GRAPHICS_RENDERER_BACKUP_MARKER " +
                    "$GRAPHICS_RENDERER_RESTART_MARKER",
                "rmdir $GRAPHICS_RENDERER_DIR 2>/dev/null || true",
            )
        )
        val status = getGraphicsRendererStatus()
        return GraphicsRendererCommandResult(
            success = cleanup.first && !status.configured && !status.persistent,
            status = status,
            error = when {
                !cleanup.first -> cleanup.second.ifBlank { "cleanup_failed" }
                status.configured || status.persistent -> "cleanup_verification_failed"
                else -> ""
            },
        )
    }
    if (!before.backupAvailable) {
        return GraphicsRendererCommandResult(false, before, "backup_missing")
    }
    val restored = runGraphicsRendererCommands(
        graphicsRendererPropertyCommands(
            before.originalRendererProperty,
            before.originalDisableVulkanProperty,
        )
    )
    if (!restored.first) {
        rollbackGraphicsRendererRuntime(before)
        return GraphicsRendererCommandResult(
            false,
            getGraphicsRendererStatus(),
            restored.second.ifBlank { "restore_failed" },
        )
    }
    val runtimeStatus = awaitGraphicsRendererStatus { status ->
        status.rendererProperty == before.originalRendererProperty &&
            status.disableVulkanProperty == before.originalDisableVulkanProperty
    }
    if (
        runtimeStatus.rendererProperty != before.originalRendererProperty ||
        runtimeStatus.disableVulkanProperty != before.originalDisableVulkanProperty
    ) {
        rollbackGraphicsRendererRuntime(before)
        return GraphicsRendererCommandResult(
            false,
            getGraphicsRendererStatus(),
            "restore_verification_failed",
        )
    }
    val cleanup = runGraphicsRendererCommands(
        listOf(
            "rm -f $GRAPHICS_RENDERER_SERVICE",
            "rm -f $GRAPHICS_RENDERER_MODE_FILE $GRAPHICS_RENDERER_ORIGINAL_RENDERER " +
                "$GRAPHICS_RENDERER_ORIGINAL_DISABLE $GRAPHICS_RENDERER_BACKUP_MARKER " +
                "$GRAPHICS_RENDERER_RESTART_MARKER",
            "rmdir $GRAPHICS_RENDERER_DIR 2>/dev/null || true",
        )
    )
    if (!cleanup.first) {
        return GraphicsRendererCommandResult(false, runtimeStatus, cleanup.second.ifBlank { "cleanup_failed" })
    }
    val finalStatus = getGraphicsRendererStatus()
    return GraphicsRendererCommandResult(
        success = !finalStatus.configured && !finalStatus.persistent,
        status = finalStatus,
        error = if (!finalStatus.configured && !finalStatus.persistent) "" else "cleanup_verification_failed",
    )
}

private suspend fun applyGraphicsRendererRuntime(mode: GraphicsRendererMode): Pair<Boolean, String> =
    runGraphicsRendererCommands(
        when (mode) {
            GraphicsRendererMode.Vulkan -> graphicsRendererPropertyCommands("skiavk", "false")
            GraphicsRendererMode.OpenGl -> graphicsRendererPropertyCommands("skiagl", "true")
            else -> emptyList()
        }
    )

private suspend fun rollbackGraphicsRendererRuntime(status: GraphicsRendererStatus) {
    runGraphicsRendererCommands(
        graphicsRendererPropertyCommands(status.rendererProperty, status.disableVulkanProperty)
    )
}

private fun graphicsRendererPropertyCommands(renderer: String, disableVulkan: String): List<String> = listOf(
    resetGraphicsPropertyCommand("debug.hwui.renderer", renderer),
    resetGraphicsPropertyCommand("debug.hwui.disable_vulkan", disableVulkan),
)

private fun resetGraphicsPropertyCommand(name: String, value: String): String {
    val executable = shellQuote(getKsuDaemonPath())
    return if (value.isEmpty()) {
        "$executable resetprop --delete ${shellQuote(name)}"
    } else {
        "$executable resetprop ${shellQuote(name)} ${shellQuote(value)}"
    }
}

private suspend fun awaitGraphicsRendererStatus(
    predicate: (GraphicsRendererStatus) -> Boolean,
): GraphicsRendererStatus {
    var status = getGraphicsRendererStatus()
    repeat(GRAPHICS_RENDERER_VERIFICATION_ATTEMPTS - 1) {
        if (predicate(status)) return status
        if (!status.rootAvailable && status.error == "root_unavailable") return status
        delay(GRAPHICS_RENDERER_VERIFICATION_DELAY_MILLIS)
        status = getGraphicsRendererStatus()
    }
    return status
}

private fun logGraphicsRendererVerificationFailure(
    stage: String,
    expectedMode: GraphicsRendererMode,
    status: GraphicsRendererStatus,
) {
    Log.w(
        TAG,
        "graphics renderer $stage verification failed: expected=${expectedMode.value}, " +
            "current=${status.currentMode.value}, renderer=${status.rendererProperty}, " +
            "disableVulkan=${status.disableVulkanProperty}, configured=${status.configuredMode?.value}, " +
            "persistent=${status.persistent}, error=${status.error}",
    )
}

private suspend fun writeGraphicsRendererConfiguration(
    mode: GraphicsRendererMode,
    persistent: Boolean,
    serviceBase64: String,
    restartRequired: Boolean,
): Pair<Boolean, String> {
    val commands = mutableListOf(
        "mkdir -p $GRAPHICS_RENDERER_DIR /data/adb/service.d && chmod 0700 $GRAPHICS_RENDERER_DIR",
        atomicWriteCommand(GRAPHICS_RENDERER_MODE_FILE, mode.value),
    )
    if (persistent) {
        commands += "printf '%s' ${shellQuote(serviceBase64)} | $BUSYBOX base64 -d > " +
            "$GRAPHICS_RENDERER_SERVICE.tmp && chmod 0700 $GRAPHICS_RENDERER_SERVICE.tmp && " +
            "chown 0:0 $GRAPHICS_RENDERER_SERVICE.tmp && mv -f $GRAPHICS_RENDERER_SERVICE.tmp $GRAPHICS_RENDERER_SERVICE"
    } else {
        commands += "rm -f $GRAPHICS_RENDERER_SERVICE $GRAPHICS_RENDERER_SERVICE.tmp"
    }
    commands += if (restartRequired) {
        ": > $GRAPHICS_RENDERER_RESTART_MARKER && chmod 0600 $GRAPHICS_RENDERER_RESTART_MARKER"
    } else {
        "rm -f $GRAPHICS_RENDERER_RESTART_MARKER"
    }
    return runGraphicsRendererCommands(commands)
}

private suspend fun restorePreviousGraphicsRendererConfiguration(
    status: GraphicsRendererStatus,
    serviceBase64: String,
) {
    val mode = status.configuredMode
    if (mode == null) {
        runGraphicsRendererCommands(
            listOf(
                "rm -f $GRAPHICS_RENDERER_MODE_FILE $GRAPHICS_RENDERER_SERVICE $GRAPHICS_RENDERER_RESTART_MARKER"
            )
        )
    } else {
        writeGraphicsRendererConfiguration(mode, status.persistent, serviceBase64, status.restartRequired)
    }
}

private fun atomicWriteCommand(path: String, value: String): String =
    "printf '%s' ${shellQuote(value)} > $path.tmp && chmod 0600 $path.tmp && mv -f $path.tmp $path"

private suspend fun runGraphicsRendererCommands(commands: List<String>): Pair<Boolean, String> {
    if (commands.isEmpty()) return true to ""
    val stdout = ArrayList<String>()
    val stderr = ArrayList<String>()
    val script = buildString {
        appendLine("set -e")
        commands.forEach(::appendLine)
    }
    val result = runCatching {
        withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
            getRootShell().newJob().add(script).to(stdout, stderr).exec()
        }
    }.getOrElse { error ->
        return false to error.message.orEmpty().ifBlank { "shell_failed" }
    }
    if (result == null) {
        KsuCli.reset()
        return false to "timeout"
    }
    val detail = stderr.joinToString("\n").trim().ifBlank { stdout.joinToString("\n").trim() }
    return result.isSuccess to detail
}

private suspend fun runCpuSpoofCommand(command: String): CpuSpoofCommandResult = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext CpuSpoofCommandResult(false, "root_unavailable")
    }

    val stdout = ArrayList<String>()
    val stderr = ArrayList<String>()
    val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
        getRootShell().newJob()
            .add("${shellQuote(getKsuDaemonPath())} cpu-spoof $command")
            .to(stdout, stderr)
            .exec()
    }
    if (result == null) {
        Log.w(TAG, "cpu-spoof command timed out: $command")
        KsuCli.reset()
        return@withContext CpuSpoofCommandResult(false, "timeout")
    }

    if (!result.isSuccess) {
        val error = stderr.joinToString("\n").trim().ifBlank {
            stdout.joinToString("\n").trim().ifBlank { "command_failed" }
        }
        Log.w(TAG, "cpu-spoof command failed: $command, $error")
        return@withContext CpuSpoofCommandResult(false, error)
    }

    CpuSpoofCommandResult(true)
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

fun normalizeSusfsPath(raw: String): String? {
    val trimmed = raw.trim()
    if (
        trimmed.isEmpty() ||
        !trimmed.startsWith('/') ||
        trimmed == "/" ||
        trimmed.length > 4096 ||
        trimmed.any(Char::isISOControl)
    ) {
        return null
    }
    val normalized = trimmed.trimEnd('/').ifEmpty { return null }
    val blockedManagementPaths = listOf(
        "/data/adb/modules",
        "/data/adb/ksu",
        "/data/adb/ap",
    )
    if (
        normalized == "/data/adb" ||
        blockedManagementPaths.any { normalized == it || normalized.startsWith("$it/") }
    ) {
        return null
    }
    return normalized
}

suspend fun getSusfsPathConfig(): SusfsPathConfigState = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext SusfsPathConfigState(error = "root_unavailable")
    }
    if (Natives.isLateLoadMode || Natives.isLkmMode) {
        return@withContext SusfsPathConfigState(error = "gki_mode_required")
    }

    val stdout = ArrayList<String>()
    val stderr = ArrayList<String>()
    val command = buildString {
        appendLine("tool=''")
        appendLine("for candidate in /data/adb/ksu/bin/ksu_susfs /data/adb/ap/bin/ksu_susfs /system/bin/ksu_susfs; do")
        appendLine("  if [ -x \"${'$'}candidate\" ]; then tool=\"${'$'}candidate\"; break; fi")
        appendLine("done")
        appendLine("if [ -z \"${'$'}tool\" ]; then tool=\$(command -v ksu_susfs 2>/dev/null); fi")
        appendLine("printf '__TOOL__=%s\\n' \"${'$'}tool\"")
        appendLine("if [ -f ${shellQuote(SUSFS_PATH_CONFIG_FILE)} ]; then")
        appendLine("  while IFS= read -r target_path; do")
        appendLine("    [ -n \"${'$'}target_path\" ] && printf '__PATH__=%s\\n' \"${'$'}target_path\"")
        appendLine("  done < ${shellQuote(SUSFS_PATH_CONFIG_FILE)}")
        appendLine("fi")
    }
    val result = runCatching {
        withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
            getRootShell().newJob().add(command).to(stdout, stderr).exec()
        }
    }.getOrElse { error ->
        KsuCli.reset()
        return@withContext SusfsPathConfigState(error = error.message.orEmpty().ifBlank { "shell_failed" })
    }
    if (result == null) {
        KsuCli.reset()
        return@withContext SusfsPathConfigState(error = "timeout")
    }
    if (!result.isSuccess) {
        return@withContext SusfsPathConfigState(
            error = stderr.joinToString("\n").trim().ifBlank { "probe_failed" },
        )
    }

    val toolPath = stdout.firstOrNull { it.startsWith("__TOOL__=") }
        ?.substringAfter('=')
        ?.trim()
        .orEmpty()
    val paths = stdout.asSequence()
        .filter { it.startsWith("__PATH__=") }
        .map { it.substringAfter('=') }
        .mapNotNull(::normalizeSusfsPath)
        .distinct()
        .toList()
    SusfsPathConfigState(
        available = toolPath.isNotBlank(),
        toolPath = toolPath,
        paths = paths,
        error = if (toolPath.isBlank()) "tool_unavailable" else "",
    )
}

suspend fun saveAndApplySusfsPathConfig(paths: List<String>): SusfsPathApplyResult = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext SusfsPathApplyResult(error = "root_unavailable")
    }
    if (Natives.isLateLoadMode || Natives.isLkmMode) {
        return@withContext SusfsPathApplyResult(error = "gki_mode_required")
    }
    if (paths.size > 128) {
        return@withContext SusfsPathApplyResult(error = "too_many_paths")
    }
    val normalized = paths.mapNotNull(::normalizeSusfsPath).distinct()
    if (normalized.size != paths.size) {
        return@withContext SusfsPathApplyResult(error = "invalid_path")
    }

    val previous = getSusfsPathConfig()
    if (!previous.available) {
        return@withContext SusfsPathApplyResult(error = previous.error.ifBlank { "tool_unavailable" })
    }
    val requiresReboot = previous.paths.any { it !in normalized }
    val configText = normalized.joinToString(separator = "\n", postfix = if (normalized.isEmpty()) "" else "\n")
    val serviceScript = susfsPathServiceScript()
    val serviceBase64 = Base64.encodeToString(serviceScript.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    val stdout = ArrayList<String>()
    val stderr = ArrayList<String>()
    val pendingConfig = "$SUSFS_PATH_CONFIG_FILE.pending"
    val pendingService = "$SUSFS_PATH_SERVICE_FILE.pending"
    val command = buildString {
        appendLine("set -e")
        appendLine("mkdir -p ${shellQuote(SUSFS_PATH_CONFIG_DIR)} /data/adb/service.d")
        appendLine("trap 'rm -f $pendingConfig $pendingConfig.tmp $pendingService' EXIT")
        appendLine(atomicWriteCommand(pendingConfig, configText))
        appendLine(
            "printf '%s' ${shellQuote(serviceBase64)} | $BUSYBOX base64 -d > " +
                shellQuote(pendingService)
        )
        appendLine("chmod 0700 ${shellQuote(pendingService)}")
        appendLine("chown 0:0 ${shellQuote(pendingService)}")
        normalized.forEach { path ->
            appendLine("${shellQuote(previous.toolPath)} add_sus_path ${shellQuote(path)}")
        }
        appendLine("mv -f ${shellQuote(pendingConfig)} ${shellQuote(SUSFS_PATH_CONFIG_FILE)}")
        appendLine("mv -f ${shellQuote(pendingService)} ${shellQuote(SUSFS_PATH_SERVICE_FILE)}")
    }
    val result = runCatching {
        withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS * 3) {
            getRootShell().newJob().add(command).to(stdout, stderr).exec()
        }
    }.getOrElse { error ->
        KsuCli.reset()
        return@withContext SusfsPathApplyResult(
            requiresReboot = requiresReboot,
            error = error.message.orEmpty().ifBlank { "shell_failed" },
        )
    }
    if (result == null) {
        KsuCli.reset()
        return@withContext SusfsPathApplyResult(requiresReboot = requiresReboot, error = "timeout")
    }
    if (!result.isSuccess) {
        return@withContext SusfsPathApplyResult(
            requiresReboot = true,
            error = stderr.joinToString("\n").trim().ifBlank {
                stdout.joinToString("\n").trim().ifBlank { "apply_failed" }
            },
        )
    }
    SusfsPathApplyResult(
        success = true,
        appliedCount = normalized.size,
        requiresReboot = requiresReboot,
    )
}

private fun susfsPathServiceScript(): String = """#!/system/bin/sh
CONFIG=$SUSFS_PATH_CONFIG_FILE
TOOL=
for candidate in /data/adb/ksu/bin/ksu_susfs /data/adb/ap/bin/ksu_susfs /system/bin/ksu_susfs; do
    if [ -x "${'$'}candidate" ]; then
        TOOL="${'$'}candidate"
        break
    fi
done
if [ -z "${'$'}TOOL" ]; then
    TOOL=\$(command -v ksu_susfs 2>/dev/null)
fi
[ -x "${'$'}TOOL" ] || exit 0
[ -f "${'$'}CONFIG" ] || exit 0
while IFS= read -r target_path; do
    case "${'$'}target_path" in
        /*) "${'$'}TOOL" add_sus_path "${'$'}target_path" >/dev/null 2>&1 ;;
    esac
done < "${'$'}CONFIG"
"""

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

suspend fun testHiddenPathVisibility(uid: Int, path: String): HiddenPathVisibilityResult =
    withContext(Dispatchers.IO) {
        if (uid < 0 || path.isBlank()) {
            return@withContext HiddenPathVisibilityResult(error = "invalid UID or path")
        }
        if (shouldSkipUnsafeKsudCommand()) {
            return@withContext HiddenPathVisibilityResult(error = "root shell unavailable")
        }

        runCatching {
            val stdout = ArrayList<String>()
            val stderr = ArrayList<String>()
            val command = "${shellQuote(getKsuDaemonPath())} pathmask test-visibility " +
                "--uid $uid --path ${shellQuote(path)}"
            val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
                getRootShell().newJob()
                    .add(command)
                    .to(stdout, stderr)
                    .exec()
            }
            if (result == null) {
                KsuCli.reset()
                return@runCatching HiddenPathVisibilityResult(
                    uid = uid,
                    path = path,
                    error = "visibility probe timed out",
                )
            }
            if (!result.isSuccess) {
                return@runCatching HiddenPathVisibilityResult(
                    uid = uid,
                    path = path,
                    error = stderr.joinToString("\n").ifBlank { "visibility probe failed" },
                )
            }

            val obj = JSONObject(stdout.joinToString("\n"))
            HiddenPathVisibilityResult(
                uid = obj.optInt("uid", uid),
                path = obj.optString("path", path),
                status = obj.optString("status", "probe_failed"),
                visible = obj.optBoolean("visible", false),
                rootExists = obj.optBoolean("rootExists", false),
                moduleLoaded = obj.optBoolean("moduleLoaded", false),
                resolvedCount = obj.optString("resolvedCount", ""),
                error = obj.optString("error", ""),
            )
        }.getOrElse {
            Log.w(TAG, "pathmask visibility probe unavailable", it)
            KsuCli.reset()
            HiddenPathVisibilityResult(uid = uid, path = path, error = it.message.orEmpty())
        }
    }

suspend fun getRescueStatus(): RescueStatus = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext RescueStatus()
    }

    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS) {
            getRootShell().newJob()
                .add("${getKsuDaemonPath()} rescue status")
                .to(stdout, stderr)
                .exec()
        }

        if (result == null) {
            Log.w(TAG, "rescue status timed out")
            KsuCli.reset()
            return@runCatching RescueStatus()
        }

        if (!result.isSuccess) {
            Log.w(TAG, "rescue status failed: ${stderr.joinToString("\n")}")
            return@runCatching RescueStatus()
        }

        val obj = JSONObject(stdout.joinToString("\n"))
        val manifest = obj.optJSONObject("manifest")
        val device = obj.optJSONObject("device")
        RescueStatus(
            enabled = obj.optBoolean("enabled", false),
            config = obj.optJSONObject("config").toRescueConfigState(),
            images = obj.optJSONArray("images").toRescueImageList(),
            bootCount = obj.optInt("bootCount", 0),
            autoRestoreAttempts = obj.optInt("autoRestoreAttempts", 0),
            pendingBoot = obj.optBoolean("pendingBoot", false),
            currentSlot = obj.optString("currentSlot", ""),
            bootMode = obj.optString("bootMode", ""),
            device = listOf(
                device?.optString("brand", "").orEmpty(),
                device?.optString("model", "").orEmpty(),
            ).filter(String::isNotBlank).joinToString(" "),
            manifestCreatedAt = manifest?.optString("createdAt", "").orEmpty(),
            lastRestoreDone = obj.optBoolean("lastRestoreDone", false),
            ready = obj.optBoolean("ready", false),
            readyReason = obj.optString("readyReason", ""),
            log = obj.optString("log", ""),
        )
    }.getOrElse {
        Log.w(TAG, "rescue status unavailable", it)
        KsuCli.reset()
        RescueStatus()
    }
}

suspend fun runRescueCommand(command: String, timeoutMultiplier: Long = 6): Boolean = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext false
    }

    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS * timeoutMultiplier) {
            getRootShell().newJob()
                .add("${getKsuDaemonPath()} rescue $command")
                .to(stdout, stderr)
                .exec()
        }

        if (result == null) {
            Log.w(TAG, "rescue $command timed out")
            KsuCli.reset()
            return@runCatching false
        }

        if (!result.isSuccess) {
            Log.w(TAG, "rescue $command failed: ${stderr.joinToString("\n")}")
        }
        result.isSuccess
    }.getOrElse {
        Log.w(TAG, "rescue $command unavailable", it)
        KsuCli.reset()
        false
    }
}

suspend fun saveRescueConfig(config: RescueConfigState): Boolean = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext false
    }

    runCatching {
        val stderr = ArrayList<String>()
        val result = getRootShell().newJob()
            .add("${getKsuDaemonPath()} rescue import-config-json ${shellQuote(config.toConfigJson())}")
            .to(null, stderr)
            .exec()
        if (!result.isSuccess) {
            Log.w(TAG, "rescue config save failed: ${stderr.joinToString("\n")}")
        }
        result.isSuccess
    }.getOrElse {
        Log.w(TAG, "rescue config save unavailable", it)
        KsuCli.reset()
        false
    }
}

suspend fun importRescueImage(partition: String, sourcePath: String, force: Boolean): Boolean = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext false
    }

    runCatching {
        val stderr = ArrayList<String>()
        val forceArg = if (force) " --force" else ""
        val result = getRootShell().newJob()
            .add(
                "${getKsuDaemonPath()} rescue import-image " +
                    "${shellQuote(partition)} ${shellQuote(sourcePath)}$forceArg"
            )
            .to(null, stderr)
            .exec()
        if (!result.isSuccess) {
            Log.w(TAG, "rescue image import failed: ${stderr.joinToString("\n")}")
        }
        result.isSuccess
    }.getOrElse {
        Log.w(TAG, "rescue image import unavailable", it)
        KsuCli.reset()
        false
    }
}

suspend fun testRescueEnvironment(): RescueTestReport = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext RescueTestReport(reason = "ksud command unavailable")
    }

    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = getRootShell().newJob()
            .add("${getKsuDaemonPath()} rescue test")
            .to(stdout, stderr)
            .exec()
        val raw = stdout.joinToString("\n")
        val obj = JSONObject(raw)
        RescueTestReport(
            ok = result.isSuccess && obj.optBoolean("ok", false),
            reason = obj.optString("reason", stderr.joinToString("\n")),
            text = raw,
        )
    }.getOrElse {
        Log.w(TAG, "rescue test unavailable", it)
        KsuCli.reset()
        RescueTestReport(reason = it.message.orEmpty())
    }
}

suspend fun getRescueLogs(): String = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext ""
    }

    runCatching {
        val stdout = ArrayList<String>()
        getRootShell().newJob()
            .add("${getKsuDaemonPath()} rescue logs")
            .to(stdout, null)
            .exec()
        stdout.joinToString("\n")
    }.getOrDefault("")
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

fun install(): Boolean {
    val start = SystemClock.elapsedRealtime()
    val libadbroot = File(ksuApp.applicationInfo.nativeLibraryDir, "libadbroot.so").absolutePath
    val dataPath = ksuApp.applicationInfo.deviceProtectedDataDir
    val result = execKsud(
        "install --libadbroot ${shellQuote(libadbroot)} --data-path ${shellQuote(dataPath)}",
        true,
    )
    Log.w(TAG, "install result: $result, cost: ${SystemClock.elapsedRealtime() - start}ms")
    return result
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
        "module enable ${shellQuote(id)}"
    } else {
        "module disable ${shellQuote(id)}"
    }
    val result = execKsud(cmd, true)
    Log.i(TAG, "$cmd result: $result")
    return result
}

fun undoUninstallModule(id: String): Boolean {
    val cmd = "module undo-uninstall ${shellQuote(id)}"
    val result = execKsud(cmd, true)
    Log.i(TAG, "undo uninstall module $id result: $result")
    return result
}

fun uninstallModule(id: String): Boolean {
    val cmd = "module uninstall ${shellQuote(id)}"
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
    val requestedName = File(fileName).name
    val baseName = requestedName.substringBeforeLast('.', requestedName)
        .take(32)
        .padEnd(3, '_')
    val extension = requestedName.substringAfterLast('.', "")
        .takeIf { it.isNotEmpty() }
        ?.let { ".$it" }
        .orEmpty()
    val file = File.createTempFile("${baseName}_", extension, ksuApp.cacheDir)
    return try {
        val input = ksuApp.contentResolver.openInputStream(uri)
            ?: error("Unable to open selected file: $uri")
        input.use { source ->
            file.outputStream().use { output ->
                source.copyTo(output)
            }
        }
        require(file.length() > 0) { "Selected file is empty: $uri" }
        file
    } catch (error: Exception) {
        file.delete()
        throw error
    }
}

fun flashModule(
    uri: Uri,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    if (!install()) {
        val error = "Failed to install the ApkeSU daemon"
        onStderr(error)
        return FlashResult(1, error, false)
    }

    val file = copyUriToCache(uri, "module.zip")
    try {
        val cmd = "module install ${shellQuote(file.absolutePath)}"
        val result = flashWithIO("${shellQuote(getKsuDaemonPath())} $cmd", onStdout, onStderr)
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
        newJob().add("${shellQuote(getKsuDaemonPath())} module action ${shellQuote(moduleId)}")
            .to(stdoutCallback, stderrCallback).exec()
    }

    Log.i("KernelSU", "Module runAction result: $result")

    return result
}

fun restoreBoot(
    onStdout: (String) -> Unit, onStderr: (String) -> Unit
): FlashResult {
    val result = flashWithIO("${shellQuote(getKsuDaemonPath())} boot-restore -f", onStdout, onStderr)
    return FlashResult(result)
}

fun uninstallPermanently(
    onStdout: (String) -> Unit, onStderr: (String) -> Unit
): FlashResult {
    val result = flashWithIO(
        "${shellQuote(getKsuDaemonPath())} uninstall --package-name ${shellQuote(BuildConfig.APPLICATION_ID)}",
        onStdout,
        onStderr,
    )
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
    data object KmiNone : LkmSelection()
}

suspend fun installBoot(
    bootUri: Uri?,
    lkm: LkmSelection,
    ota: Boolean,
    partition: String?,
    allowShell: Boolean,
    enableAdb: Boolean,
    forceBackup: Boolean,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit,
): FlashResult {
    var bootFile: File? = null
    var lkmFile: File? = null
    var patchedOutput: File? = null

    return try {
        bootFile = bootUri?.let { uri -> copyUriToCache(uri, "boot.img") }
        var cmd = "boot-patch"

        cmd += bootFile?.let { " -b ${shellQuote(it.absolutePath)}" } ?: " -f"

        if (allowShell) {
            cmd += " --allow-shell"
        }
        if (enableAdb) {
            cmd += " --enable-adbd"
        }
        if (ota) {
            cmd += " -u"
        }
        if (forceBackup) {
            cmd += " --backup"
        }

        when (lkm) {
            is LkmSelection.LkmUri -> {
                val selectedLkmFile = copyUriToCache(lkm.uri, "kernelsu-tmp-lkm.ko")
                lkmFile = selectedLkmFile
                cmd += " -m ${shellQuote(selectedLkmFile.absolutePath)}"
            }

            is LkmSelection.KmiString -> {
                cmd += " --kmi ${shellQuote(lkm.value)}"
            }

            is LkmSelection.PathMaskKmiString -> {
                cmd += " --pathmask-lkm --kmi ${shellQuote(lkm.value)}"
            }

            LkmSelection.KmiNone -> Unit
        }

        if (bootFile != null) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val outputName = "apkesu_patched_$timestamp.img"
            val outputFile = preparePatchedImageOutput(ksuApp.cacheDir, outputName)
            patchedOutput = outputFile
            cmd += " -o ${shellQuote(ksuApp.cacheDir.absolutePath)}"
            cmd += " --out-name ${shellQuote(outputName)}"
        } else {
            partition?.let { part ->
                cmd += " --partition ${shellQuote(part)}"
            }
        }

        if (bootFile == null) {
            // Direct install writes the patched boot immediately. Refresh the
            // persistent daemon first so the next boot keeps the APK-bundled
            // ksud/version instead of an older /data/adb/ksud copy.
            if (!install()) {
                val error = "Failed to install the ApkeSU daemon before direct install"
                onStderr(error)
                return FlashResult(1, error, false)
            }
        }

        val result = flashWithIO("${shellQuote(getKsuDaemonPath())} $cmd", onStdout, onStderr)
        Log.i("KernelSU", "install boot result: ${result.isSuccess}")

        if (!result.isSuccess) {
            return FlashResult(result, false)
        }

        patchedOutput?.let { output ->
            var outputError = validatePatchedImageOutput(output)
            if (outputError != null && output.isFile && output.length() > 0L) {
                Log.w(TAG, "$outputError; attempting to restore app access")
                restorePatchedImageAccess(output)
                outputError = validatePatchedImageOutput(output)
            }
            if (outputError != null) {
                val error = "Patched image output is unavailable: $outputError"
                onStderr(error)
                return FlashResult(1, error, false)
            }
            val savedPath = runCatching {
                saveFileToDownloads(
                    context = ksuApp,
                    displayName = output.name,
                    source = output,
                )
            }.getOrElse { throwable ->
                val error = "Failed to save patched image: ${throwable.localizedMessage ?: throwable.javaClass.simpleName}"
                onStderr(error)
                return FlashResult(1, error, false)
            }
            onStdout("- Patched image saved to $savedPath")
        }

        if (bootFile != null && rootAvailable() && !install()) {
            onStderr("Warning: patched successfully, but failed to refresh the ApkeSU daemon")
        }

        val showReboot = bootUri == null
        FlashResult(result, showReboot)
    } finally {
        bootFile?.delete()
        lkmFile?.delete()
        patchedOutput?.delete()
    }
}

internal fun preparePatchedImageOutput(cacheDir: File, outputName: String): File {
    check(cacheDir.isDirectory || cacheDir.mkdirs()) {
        "Unable to prepare patched image directory"
    }
    val output = File(cacheDir, outputName)
    if (output.exists()) {
        check(output.delete()) { "Unable to replace stale patched image output" }
    }
    output.outputStream().use { }
    return output
}

internal fun validatePatchedImageOutput(output: File): String? {
    if (!output.isFile) return "file is missing"
    if (output.length() <= 0L) return "file is empty"
    return runCatching {
        output.inputStream().use { it.read() }
    }.exceptionOrNull()?.let { throwable ->
        "file is not readable (${throwable.localizedMessage ?: throwable.javaClass.simpleName})"
    }
}

private fun restorePatchedImageAccess(output: File): Boolean {
    val uid = Process.myUid()
    val result = withNewRootShell {
        newJob()
            .add(
                "chown $uid:$uid ${shellQuote(output.absolutePath)} && " +
                    "chmod 0600 ${shellQuote(output.absolutePath)}"
            )
            .exec()
    }
    if (!result.isSuccess) {
        Log.w(TAG, "Failed to restore patched image access: ${result.err.joinToString("; ")}")
    }
    return result.isSuccess
}

fun reboot(reason: String = "") {
    if (reason == "soft_reboot") {
        execKsud("soft-reboot", true, true)
        return
    }
    val shell = getRootShell()
    if (reason == "recovery") {
        // KEYCODE_POWER = 26, hide incorrect "Factory data reset" message
        ShellUtils.fastCmd(shell, "/system/bin/input keyevent 26")
    }
    val quotedReason = shellQuote(reason)
    ShellUtils.fastCmd(
        shell,
        "/system/bin/svc power reboot $quotedReason || /system/bin/reboot $quotedReason"
    )
}

fun flashAnyKernelZip(
    uri: Uri,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    if (!install()) {
        val error = "Failed to install the ApkeSU daemon before AnyKernel flash"
        onStderr(error)
        return FlashResult(1, error, false)
    }

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    val tmpFile = copyUriToCache(uri, "anykernel_${timestamp}.zip")

    val destZip = tmpFile.absolutePath
    val destZipName = tmpFile.name
    val destDirFile = File(ksuApp.cacheDir, "anykernel3_${timestamp}")
    val destDir = destDirFile.absolutePath

    val cmd = """
        mkdir -p '$destDir' && \
        $BUSYBOX unzip -p -o '$destZip' "META-INF/com/google/android/update-binary" > '$destDir/update-binary' 2>/dev/null && \
        $BUSYBOX test -s '$destDir/update-binary' && \
        $BUSYBOX cp '$destZip' '$destDir/$destZipName' && \
        $BUSYBOX chmod 755 '$destDir/update-binary' && \
        $BUSYBOX chown root:root '$destDir/update-binary' && \
        (cd '$destDir' && \
            if [ -f './update-binary' ] && $BUSYBOX grep -q "AnyKernel3" './update-binary'; then \
                AKHOME='$destDir/tmp' $BUSYBOX ash '$destDir/update-binary' 3 1 '$destDir/$destZipName'; \
            else \
                echo 'No installer script found' >&2; exit 1; \
            fi)
    """.trimIndent().replace(Regex("\\s+\\\\\\s*"), " ")

    return try {
        val result = flashWithIoAk3(cmd, onStdout, onStderr)
        if (result.isSuccess) {
            runCatching {
                if (!execKsud("rescue mark-pending ${shellQuote("AnyKernel install")}", true)) {
                    onStderr("Rescue protection: failed to mark next boot pending")
                }
            }.onFailure {
                Log.w(TAG, "failed to mark rescue pending after AnyKernel install", it)
            }
        }
        FlashResult(result, result.isSuccess)
    } finally {
        runCatching {
            createRootShell(true).use { shell ->
                shell.newJob()
                    .add("rm -rf ${shellQuote(destDir)} ${shellQuote(destZip)}")
                    .exec()
            }
        }
        tmpFile.delete()
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

fun ksuRootAvailable(): Boolean {
    return runCatching {
        val shell = Shell.Builder.create().build(getKsuDaemonPath(), "debug", "su")
        try {
            shell.isRoot
        } finally {
            shell.closeQuietly()
        }
    }.getOrDefault(false)
}

fun apkeSuKernelModuleLoaded(): Boolean {
    return runCatching {
        File("/sys/module/kernelsu").isDirectory ||
            File("/sys/module/apkesu").isDirectory
    }.getOrDefault(false)
}

fun apkeSuRootAvailable(): Boolean {
    if (runCatching { Natives.version > 0 }.getOrDefault(false) ||
        apkeSuKernelModuleLoaded()
    ) {
        return true
    }
    if (ksuRootAvailable()) {
        return true
    }

    return runCatching {
        withNewRootShell {
            if (!isRoot) {
                return@withNewRootShell false
            }

            val versionOutput = ArrayList<String>()
            newJob()
                .add("su -v 2>/dev/null || /system/bin/su -v 2>/dev/null || true")
                .to(versionOutput, null)
                .exec()
            val suVersion = versionOutput.joinToString("\n")
            if (suVersion.contains("KernelSU", ignoreCase = true) ||
                suVersion.contains("ApkeSU", ignoreCase = true)
            ) {
                return@withNewRootShell true
            }

            val marker = ShellUtils.fastCmd(
                this,
                "if [ -d /sys/module/kernelsu ] || [ -d /sys/module/apkesu ] || " +
                    "grep -Eq '^(kernelsu|apkesu) ' /proc/modules 2>/dev/null; " +
                    "then echo 1; else echo 0; fi"
            ).trim()
            marker == "1"
        }
    }.getOrDefault(false)
}

suspend fun collectRootDiagnosticInfo(): RootDiagnosticInfo = withContext(Dispatchers.IO) {
    runCatching { Natives.refreshInfo() }
    val driverVersion = runCatching { Natives.version }.getOrDefault(0)
    val managerRegistered = runCatching { Natives.isManager }.getOrDefault(false)
    val kernelUapi = runCatching { Natives.kernelUAPIVersion }.getOrDefault(0)
    val managerUapi = runCatching { Natives.managerUAPIVersion }.getOrDefault(0)
    val lkmMode = runCatching { Natives.isLkmMode }.getOrDefault(false)
    val lateLoadMode = runCatching { Natives.isLateLoadMode }.getOrDefault(false)
    val ksuRootShell = ksuRootAvailable()
    val kernelModuleLoaded = apkeSuKernelModuleLoaded()
    val ksud = shellQuote(getKsuDaemonPath())

    var fallbackRootShell = false
    var packagedKsudVersion = ""
    var installedKsudVersion = ""
    var currentKmi = ""
    var currentSlot = ""
    runCatching {
        withNewRootShell {
            fallbackRootShell = isRoot
            packagedKsudVersion = ShellUtils.fastCmd(
                this,
                "$ksud debug userspace-version 2>/dev/null",
            ).trim()
            installedKsudVersion = ShellUtils.fastCmd(
                this,
                "if [ -x /data/adb/ksud ]; then " +
                    "/data/adb/ksud debug userspace-version 2>/dev/null; else echo missing; fi",
            ).trim()
            currentKmi = ShellUtils.fastCmd(
                this,
                "$ksud boot-info current-kmi 2>/dev/null",
            ).trim()
            currentSlot = ShellUtils.fastCmd(
                this,
                "getprop ro.boot.slot_suffix 2>/dev/null",
            ).trim().ifBlank {
                ShellUtils.fastCmd(this, "getprop ro.boot.slot 2>/dev/null").trim()
            }
        }
    }.onFailure {
        Log.w(TAG, "collect root shell diagnostics failed", it)
    }

    val workMode = when {
        lateLoadMode -> "late_load"
        lkmMode -> "lkm"
        driverVersion > 0 || kernelModuleLoaded || ksuRootShell -> "gki"
        else -> "unknown"
    }
    RootDiagnosticInfo(
        driverVersion = driverVersion,
        kernelModuleLoaded = kernelModuleLoaded,
        ksuRootShell = ksuRootShell,
        fallbackRootShell = fallbackRootShell,
        managerRegistered = managerRegistered,
        managerUid = Os.getuid(),
        kernelUapi = kernelUapi,
        managerUapi = managerUapi,
        packagedKsudVersion = packagedKsudVersion,
        installedKsudVersion = installedKsudVersion,
        currentKmi = currentKmi,
        currentSlot = currentSlot,
        workMode = workMode,
        hiddenPathLkm = if (fallbackRootShell && lkmMode) {
            isHiddenPathLkmMode()
        } else {
            false
        },
    )
}

fun getInstalledKsudStatus(): InstalledKsudStatus {
    return runCatching {
        withNewRootShell {
            if (!isRoot) return@withNewRootShell InstalledKsudStatus()
            val stdout = ArrayList<String>()
            newJob()
                .add(
                    "if [ -x /data/adb/ksud ]; then " +
                        "echo present; /data/adb/ksud debug userspace-version 2>/dev/null; " +
                        "else echo missing; fi"
                )
                .to(stdout, null)
                .exec()
            val present = stdout.firstOrNull()?.trim() == "present"
            val versionCode = stdout.firstOrNull { it.trim().startsWith("{") }
                ?.let { JSONObject(it).optString("versionCode").toIntOrNull() }
            InstalledKsudStatus(present = present, versionCode = versionCode)
        }
    }.getOrElse {
        Log.w(TAG, "installed ksud status unavailable", it)
        InstalledKsudStatus()
    }
}

fun ensureManagerRegistered(): Boolean {
    return synchronized(managerRegistrationLock) {
        if (runCatching { Natives.refreshInfo(); Natives.isManager }.getOrDefault(false)) {
            return@synchronized true
        }
        if (!apkeSuRootAvailable()) {
            return@synchronized false
        }

        val managerUid = Os.getuid()
        val result = runCatching {
            val ksud = shellQuote(getKsuDaemonPath())
            val packageName = shellQuote(BuildConfig.APPLICATION_ID)
            val command = "$ksud register-manager --package-name $packageName --manager-uid $managerUid"
            withNewRootShell {
                newJob().add(command).exec()
            }
        }.onFailure {
            Log.w(TAG, "register manager appid failed", it)
        }.getOrNull()

        if (result?.isSuccess != true) {
            Log.w(TAG, "register manager appid failed: ${result?.err?.joinToString("\n")}")
            return@synchronized false
        }

        KsuCli.reset()
        runCatching {
            Natives.refreshInfo()
            Natives.isManager
        }.getOrDefault(false)
    }
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
    runCatching {
        val shell = getRootShell()
        val cmd = "boot-info current-kmi"
        ShellUtils.fastCmd(shell, "${shellQuote(getKsuDaemonPath())} $cmd").trim()
    }.getOrElse {
        Log.w(TAG, "current KMI detection failed", it)
        ""
    }
}

suspend fun getSupportedKmis(): List<String> = withContext(Dispatchers.IO) {
    runCatching {
        val shell = getRootShell()
        val cmd = "boot-info supported-kmis"
        val result = shell.newJob()
            .add("${shellQuote(getKsuDaemonPath())} $cmd")
            .to(ArrayList(), null)
            .exec()
        check(result.isSuccess) { result.err.joinToString("\n").ifBlank { "ksud exited with ${result.code}" } }
        result.out.map { it.trim() }
            .filter { it.matches(kmiNameRegex) }
            .distinct()
            .ifEmpty { fallbackSupportedKmis }
    }.getOrElse {
        Log.w(TAG, "supported KMI detection failed; using packaged fallback list", it)
        fallbackSupportedKmis
    }
}

suspend fun isAbDevice(): Boolean = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = "boot-info is-ab-device"
    ShellUtils.fastCmd(shell, "${getKsuDaemonPath()} $cmd").trim().toBoolean()
}

suspend fun getDefaultPartition(ota: Boolean = false): String = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    if (shell.isRoot) {
        val cmd = if (ota) "boot-info default-partition --ota" else "boot-info default-partition"
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

suspend fun getAvailablePartitions(ota: Boolean = false): List<String> = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = if (ota) "boot-info available-partitions --ota" else "boot-info available-partitions"
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
    val result = shell.newJob()
        .add("${shellQuote(getKsuDaemonPath())} sepolicy check ${shellQuote(rules)}")
        .to(ArrayList(), null)
            .exec()
    return result.isSuccess
}

fun getSepolicy(pkg: String): String {
    val shell = getRootShell()
    val result = shell.newJob()
        .add("${shellQuote(getKsuDaemonPath())} profile get-sepolicy ${shellQuote(pkg)}")
        .to(ArrayList(), null)
            .exec()
    Log.i(TAG, "code: ${result.code}, out: ${result.out}, err: ${result.err}")
    return result.out.joinToString("\n")
}

fun setSepolicy(pkg: String, rules: String): Boolean {
    val shell = getRootShell()
    val result = shell.newJob()
        .add("${shellQuote(getKsuDaemonPath())} profile set-sepolicy ${shellQuote(pkg)} ${shellQuote(rules)}")
        .to(ArrayList(), null).exec()
    Log.i(TAG, "set sepolicy result: ${result.code}")
    return result.isSuccess
}

fun listAppProfileTemplates(): List<String> {
    val shell = getRootShell()
    return shell.newJob().add("${shellQuote(getKsuDaemonPath())} profile list-templates").to(ArrayList(), null)
        .exec().out
}

fun getAppProfileTemplate(id: String): String {
    val shell = getRootShell()
    return shell.newJob().add("${shellQuote(getKsuDaemonPath())} profile get-template ${shellQuote(id)}")
        .to(ArrayList(), null).exec().out.joinToString("\n")
}

fun setAppProfileTemplate(id: String, template: String): Boolean {
    val shell = getRootShell()
    val cmd = "${shellQuote(getKsuDaemonPath())} profile set-template ${shellQuote(id)} ${shellQuote(template)}"
    return shell.newJob().add(cmd)
        .to(ArrayList(), null).exec().isSuccess
}

fun deleteAppProfileTemplate(id: String): Boolean {
    val shell = getRootShell()
    return shell.newJob().add("${shellQuote(getKsuDaemonPath())} profile delete-template ${shellQuote(id)}")
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

private fun JSONObject?.toRescueImageState(): RescueImageState {
    if (this == null) {
        return RescueImageState()
    }
    return RescueImageState(
        name = optString("name", ""),
        label = optString("label", ""),
        partition = optString("partition", ""),
        image = optString("image", ""),
        required = optBoolean("required", false),
        custom = optBoolean("custom", false),
        exists = optBoolean("exists", false),
        size = optLong("size", 0),
        partitionSize = optLong("partitionSize", 0),
        sha256 = optString("sha256", ""),
        sha256Ok = optBoolean("sha256Ok", true),
        sizeOk = optBoolean("sizeOk", true),
        otherSlot = optBoolean("otherSlot", false),
        restore = optBoolean("restore", true),
    )
}

private fun JSONObject?.toRescueConfigState(): RescueConfigState {
    if (this == null) {
        return RescueConfigState()
    }
    val custom = optJSONObject("customPartitions")
    val keys = custom?.keys()
    val map = mutableMapOf<String, String>()
    if (keys != null) {
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = custom.optString(key, "")
        }
    }
    return RescueConfigState(
        includeDtbo = optBoolean("includeDtbo", false),
        includeVbmeta = optBoolean("includeVbmeta", false),
        backupOtherSlot = optBoolean("backupOtherSlot", false),
        allowDangerousAutoRestore = optBoolean("allowDangerousAutoRestore", false),
        customPartitions = map,
    )
}

private fun JSONArray?.toRescueImageList(): List<RescueImageState> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(optJSONObject(index).toRescueImageState())
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
    val result = shell.newJob().add("am force-stop$userArg ${shellQuote(packageName)}").exec()
    Log.i(TAG, "force stop $packageName result: $result")
}

fun launchApp(packageName: String, userId: Int? = null) {
    val shell = getRootShell()
    val userArg = userId?.let { " --user $it" } ?: ""
    val result =
        shell.newJob()
            .add(
                "component=\$(cmd package resolve-activity --brief$userArg ${shellQuote(packageName)} | " +
                    "tail -n 1); [ -n \"\$component\" ] && " +
                    "cmd activity start-activity$userArg -n \"\$component\""
            )
            .exec()
    Log.i(TAG, "launch $packageName result: $result")
}

fun restartApp(packageName: String, userId: Int? = null) {
    forceStopApp(packageName, userId)
    launchApp(packageName, userId)
}
