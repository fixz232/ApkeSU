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
import me.weishu.kernelsu.core.tasks.BootKernelVersion
import me.weishu.kernelsu.core.tasks.ExtractImage
import me.weishu.kernelsu.core.tasks.ProbeResult
import me.weishu.kernelsu.core.utils.DataSourceChannel
import me.weishu.kernelsu.ksuApp
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * @author weishu
 * @date 2023/1/1.
 */
private const val TAG = "KsuCli"
private const val SHELL_JOB_TIMEOUT_MILLIS = 10_000L
private const val STATUS_TIMEOUT_MILLIS = 30_000L
private const val DIAGNOSTIC_TIMEOUT_MILLIS = 60_000L
private const val LONG_IO_TIMEOUT_MILLIS = 300_000L
private const val ERROR_PREFIX = "APKESU_ERROR:"
private const val ANDROID_16_API = 36
private const val BUSYBOX = "/data/adb/ksu/bin/busybox"
const val CPU_SPOOF_PROPERTY_VALUE_LIMIT = 91
private val managerRegistrationLock = Any()
private const val MANAGER_REGISTRATION_RETRY_MILLIS = 30_000L
private const val FIRST_APPLICATION_APPID = 10_000
private const val LAST_APPLICATION_APPID = 19_999
private const val DYNAMIC_MANAGER_STATUS_SCHEMA_VERSION = 1
private const val DYNAMIC_MANAGER_MIN_CERTIFICATE_SIZE = 0x100
private const val DYNAMIC_MANAGER_MAX_CERTIFICATE_SIZE = 1024
private val DYNAMIC_MANAGER_CERTIFICATE_SHA256 = Regex("[0-9a-f]{64}")
private var lastManagerRegistrationFailureKey: String? = null
private var lastManagerRegistrationFailureAt = 0L
const val HYBRID_MOUNT_MODULE_ID = "hybrid_mount"
const val KPATCH_NEXT_MODULE_ID = "KPatch-Next"
const val BUILTIN_MOUNT_MODE_OVERLAY = "overlay"
const val BUILTIN_MOUNT_MODE_MAGIC = "magic"
const val BUILTIN_MOUNT_VARIANT_LITE = "lite"
const val BUILTIN_MOUNT_VARIANT_FULL = "full"
const val HIDDEN_PATH_CONFIG_FILE_NAME = "apkesu_hidden_path_config.json"
const val HIDDEN_PATH_CONFIG_MIME_TYPE = "application/json"
const val HIDDEN_PATH_MAX_AUTO_LOAD_DELAY_SECONDS = 300
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

internal fun isManagerHiddenModuleId(moduleId: String): Boolean {
    return moduleId.equals(KPATCH_NEXT_MODULE_ID, ignoreCase = true)
}

private fun getKsuDaemonPath(): String {
    return ksuApp.applicationInfo.nativeLibraryDir + File.separator + "libksud.so"
}

data class FlashResult(
    val code: Int,
    val err: String,
    val showReboot: Boolean,
) {
    constructor(result: Shell.Result, showReboot: Boolean) : this(result.code, result.err.joinToString("\n"), showReboot)
    constructor(result: Shell.Result) : this(result, result.isSuccess)
}

data class DynamicManagerCliState(
    val supported: Boolean = false,
    val configured: Boolean = false,
    val active: Boolean = false,
    val packageName: String = "",
    val appId: Int = 0,
    val certificateSize: Int = 0,
    val certificateSha256: String = "",
    val error: String = "",
)

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
    val error: String = "",
)

data class KpmCaps(
    val backend: String = "kpatch-next",
    val managementAvailable: Boolean = false,
    val supported: Boolean = false,
    val kernelSupported: Boolean = false,
    val policyEnabled: Boolean = true,
    val lateLoad: Boolean = false,
    val abiVersion: Int = 0,
    val capabilities: Int = 0,
    val maxImageSize: Long = 0,
    val maxLoaded: Int = 0,
    val disabledReason: String = "",
    val error: String = "",
)

data class KpmEntry(
    val id: String,
    val name: String = "",
    val version: String = "",
    val license: String = "",
    val author: String = "",
    val description: String = "",
    val args: String = "",
    val enabled: Boolean = false,
    val loaded: Boolean = false,
    val quarantined: Boolean = false,
    val quarantineReason: String = "",
    val sourceName: String = "",
    val importedAt: String = "",
    val error: String = "",
)

data class KpmCommandResult(
    val success: Boolean,
    val output: String = "",
    val error: String = "",
)

data class KpmExcludedApp(
    val packageName: String,
    val uid: Int,
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
    val autoLoadEnabled: Boolean = true,
    val autoLoadDelaySeconds: Int = 0,
    val autoLoadRemainingSeconds: Int = 0,
    val loaded: Boolean = false,
    val currentKmi: String = "",
    val phase: String = "unconfigured",
    val savedCount: Int = 0,
    val availableCount: Int = 0,
    val activeCount: Int = 0,
    val resolvedCount: String = "",
    val activeTargetPaths: String = "",
    val missingTargetPaths: List<String> = emptyList(),
    val unresolvedTargetCount: Int = 0,
    val unresolvedTargetPaths: List<String> = emptyList(),
    val requiresReload: Boolean = false,
    val requiresReboot: Boolean = false,
    val hasPendingCandidate: Boolean = false,
    val lastErrorCode: String = "",
    val lastErrorMessage: String = "",
    val lastLog: String = "",
    val resolvedAppUids: List<String> = emptyList(),
    val unresolvedAppPackages: List<String> = emptyList(),
) {
    val notEffectiveTargetCount: Int
        get() = (missingTargetPaths.size + unresolvedTargetCount)
            .coerceAtLeast((savedCount - activeCount).coerceAtLeast(0))

    val isPartial: Boolean
        get() = loaded && phase == "partial"
}

data class HiddenPathConfigReadResult(
    val config: HiddenPathConfigState? = null,
    val error: String = "",
    val errorCode: String = "",
)

internal fun HiddenPathConfigState.editableEquals(other: HiddenPathConfigState): Boolean {
    return targetPaths == other.targetPaths &&
        appPackages == other.appPackages &&
        useAppScope == other.useAppScope &&
        hideDirents == other.hideDirents &&
        hideIsolated == other.hideIsolated &&
        autoLoadEnabled == other.autoLoadEnabled &&
        autoLoadDelaySeconds == other.autoLoadDelaySeconds
}

data class ToolCommandResult(
    val success: Boolean = false,
    val errorCode: String = "",
    val errorMessage: String = "",
    val timedOut: Boolean = false,
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
    val dangerous: Boolean = false,
    val verificationState: String = "unknown",
)

data class RescueDisabledModule(
    val id: String = "",
    val name: String = "",
    val version: String = "",
    val installed: Boolean = false,
    val disabled: Boolean = false,
)

data class RescueRestoreEntry(
    val name: String = "",
    val label: String = "",
    val imagePath: String = "",
    val devicePath: String = "",
    val expectedSha256: String = "",
    val expectedSize: Long = 0,
    val status: String = "",
)

data class RescueRestoreTransaction(
    val id: String = "",
    val reason: String = "",
    val automatic: Boolean = false,
    val description: String = "",
    val activateSlot: String = "",
    val phase: String = "",
    val errorCode: String = "",
    val errorMessage: String = "",
    val startedAt: String = "",
    val updatedAt: String = "",
    val entries: List<RescueRestoreEntry> = emptyList(),
)

data class RescueConfigState(
    val includeDtbo: Boolean = false,
    val includeVbmeta: Boolean = false,
    val backupOtherSlot: Boolean = false,
    val allowDangerousAutoRestore: Boolean = false,
    val customPartitions: Map<String, String> = emptyMap(),
)

data class RescueStatus(
    val available: Boolean = false,
    val phase: String = "unavailable",
    val statusErrorCode: String = "",
    val statusError: String = "",
    val enabled: Boolean = false,
    val config: RescueConfigState = RescueConfigState(),
    val images: List<RescueImageState> = emptyList(),
    val bootCount: Int = 0,
    val autoRestoreAttempts: Int = 0,
    val pendingBoot: Boolean = false,
    val currentSlot: String = "",
    val bootMode: String = "",
    val device: String = "",
    val deviceFingerprint: String = "",
    val manifestCreatedAt: String = "",
    val manifestSlot: String = "",
    val manifestDevice: String = "",
    val manifestFingerprint: String = "",
    val manifestTotalSize: Long = 0,
    val lastRestoreDone: Boolean = false,
    val skipModulesOnce: Boolean = false,
    val skipModulesThisBoot: Boolean = false,
    val ready: Boolean = false,
    val readyReason: String = "",
    val verified: Boolean = false,
    val environmentChecked: Boolean = false,
    val configChangedProtectionDisabled: Boolean = false,
    val restoreInterrupted: Boolean = false,
    val restoreTransactionError: String = "",
    val restoreTransaction: RescueRestoreTransaction? = null,
    val rescueDisabledModules: List<RescueDisabledModule> = emptyList(),
    val log: String = "",
) {
    val requiredReady: Boolean
        get() = ready
}

data class RescueTestReport(
    val ok: Boolean = false,
    val errorCode: String = "",
    val reason: String = "",
    val text: String = "",
    val backupReady: Boolean = false,
    val backupReason: String = "",
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
    require(autoLoadDelaySeconds in 0..HIDDEN_PATH_MAX_AUTO_LOAD_DELAY_SECONDS) {
        "Pathmask auto-load delay is out of range"
    }
    return JSONObject()
        .put("schemaVersion", 3)
        .put("targetPaths", JSONArray(targetPaths.cleanConfigList()))
        .put("appPackages", JSONArray(appPackages.cleanConfigList()))
        .put("useAppScope", useAppScope)
        .put("hideDirents", hideDirents)
        .put("hideIsolated", hideIsolated)
        .put("autoLoadEnabled", autoLoadEnabled)
        .put("autoLoadDelaySeconds", autoLoadDelaySeconds)
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
        autoLoadEnabled = obj.optBoolean("autoLoadEnabled", current.autoLoadEnabled),
        // Older exports had no delay field and therefore always loaded immediately.
        autoLoadDelaySeconds = obj.readAutoLoadDelaySeconds(0),
    )
}

private fun JSONObject.readAutoLoadDelaySeconds(defaultValue: Int): Int {
    if (!has("autoLoadDelaySeconds")) return defaultValue
    val numeric = (opt("autoLoadDelaySeconds") as? Number)?.toDouble()
    require(numeric != null && numeric.isFinite() && numeric % 1.0 == 0.0) {
        "Pathmask auto-load delay must be an integer"
    }
    val seconds = numeric.toInt()
    require(seconds in 0..HIDDEN_PATH_MAX_AUTO_LOAD_DELAY_SECONDS) {
        "Pathmask auto-load delay must be between 0 and $HIDDEN_PATH_MAX_AUTO_LOAD_DELAY_SECONDS seconds"
    }
    return seconds
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
        return@withContext KPatchNextStatus(error = "Root shell is unavailable")
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
            return@runCatching KPatchNextStatus(error = "KPatch-Next status timed out")
        }

        if (!result.isSuccess) {
            val error = stderr.joinToString("\n").trim().ifBlank {
                "KPatch-Next status command failed"
            }
            Log.w(TAG, "kpatch-next status failed: $error")
            return@runCatching KPatchNextStatus(error = error)
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
        KPatchNextStatus(
            error = it.message.orEmpty().ifBlank { "KPatch-Next status is unavailable" },
        )
    }
}

fun setKPatchNextEnabled(enabled: Boolean): Boolean {
    val command = if (enabled) "enable" else "disable"
    return execKsud("kpatch-next $command", true)
}

private data class KsudCommandOutput(
    val success: Boolean,
    val code: Int,
    val stdout: String,
    val stderr: String,
)

private suspend fun runKsudCommandWithOutput(
    args: String,
    timeoutMillis: Long = SHELL_JOB_TIMEOUT_MILLIS,
): KsudCommandOutput = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext KsudCommandOutput(
            success = false,
            code = -1,
            stdout = "",
            stderr = "Root shell is unavailable",
        )
    }

    val stdout = ArrayList<String>()
    val stderr = ArrayList<String>()
    val result = withTimeoutOrNull(timeoutMillis) {
        getRootShell().newJob()
            .add("${shellQuote(getKsuDaemonPath())} $args")
            .to(stdout, stderr)
            .exec()
    }
    if (result == null) {
        KsuCli.reset()
        return@withContext KsudCommandOutput(
            success = false,
            code = -2,
            stdout = stdout.joinToString("\n"),
            stderr = "Command timed out after ${timeoutMillis}ms",
        )
    }
    KsudCommandOutput(
        success = result.isSuccess,
        code = result.code,
        stdout = stdout.joinToString("\n"),
        stderr = stderr.joinToString("\n"),
    )
}

internal fun parseDynamicManagerStatusJson(content: String): DynamicManagerCliState {
    val json = JSONObject(content)
    require(json.optInt("schemaVersion", -1) == DYNAMIC_MANAGER_STATUS_SCHEMA_VERSION) {
        "Unsupported Dynamic Manager status schema"
    }
    val supported = json.optBoolean("supported", false)
    val configured = json.optBoolean("configured", false)
    val active = json.optBoolean("active", false)
    val packageName = json.optString("packageName", "")
    val appId = json.optInt("appId", 0)
    val certificateSize = json.optInt("certificateSize", 0)
    val certificateSha256 = json.optString("certificateSha256", "")
    require(!active || configured) { "Active Dynamic Manager is not configured" }
    if (configured) {
        require(packageName.isNotBlank() && packageName.length < 256) {
            "Dynamic Manager package name is invalid"
        }
        require(appId in FIRST_APPLICATION_APPID..LAST_APPLICATION_APPID) {
            "Dynamic Manager App ID is invalid"
        }
        require(certificateSize in DYNAMIC_MANAGER_MIN_CERTIFICATE_SIZE..DYNAMIC_MANAGER_MAX_CERTIFICATE_SIZE) {
            "Dynamic Manager certificate size is invalid"
        }
        require(DYNAMIC_MANAGER_CERTIFICATE_SHA256.matches(certificateSha256)) {
            "Dynamic Manager certificate SHA-256 is invalid"
        }
    }
    return DynamicManagerCliState(
        supported = supported,
        configured = configured,
        active = active,
        packageName = packageName,
        appId = appId,
        certificateSize = certificateSize,
        certificateSha256 = certificateSha256,
        error = json.optString("error", "").takeUnless { it == "null" }.orEmpty(),
    )
}

suspend fun getDynamicManagerStatus(): Result<DynamicManagerCliState> {
    val result = runKsudCommandWithOutput("kernel dynamic-manager status")
    if (!result.success) {
        return Result.failure(
            IllegalStateException(
                result.stderr.trim().ifBlank { "ksud exited with code ${result.code}" },
            ),
        )
    }
    return runCatching { parseDynamicManagerStatusJson(result.stdout) }
}

suspend fun setDynamicManagerApk(
    apkPath: String,
    packageName: String,
    appId: Int,
): Result<Unit> {
    val result = runKsudCommandWithOutput(
        "kernel dynamic-manager set-apk ${shellQuote(apkPath)} " +
            "--package ${shellQuote(packageName)} --appid $appId",
        timeoutMillis = SHELL_JOB_TIMEOUT_MILLIS * 2,
    )
    return if (result.success) {
        Result.success(Unit)
    } else {
        Result.failure(
            IllegalStateException(
                result.stderr.trim().ifBlank { "ksud exited with code ${result.code}" },
            ),
        )
    }
}

suspend fun clearDynamicManager(): Result<Unit> {
    val result = runKsudCommandWithOutput("kernel dynamic-manager clear")
    return if (result.success) {
        Result.success(Unit)
    } else {
        Result.failure(
            IllegalStateException(
                result.stderr.trim().ifBlank { "ksud exited with code ${result.code}" },
            ),
        )
    }
}

private fun KsudCommandOutput.toKpmResult(): KpmCommandResult {
    return KpmCommandResult(
        success = success,
        output = stdout.trim(),
        error = if (success) "" else stderr.trim().ifBlank { "ksud exited with code $code" },
    )
}

suspend fun getKpmCaps(): KpmCaps {
    val result = runKsudCommandWithOutput("kpm caps").toKpmResult()
    if (!result.success) {
        return KpmCaps(error = result.error)
    }
    return runCatching {
        val obj = JSONObject(result.output)
        val capabilities = obj.optInt("capabilities", 0)
        KpmCaps(
            backend = obj.optString("backend", "kpatch-next"),
            managementAvailable = obj.optBoolean(
                "managementAvailable",
                obj.optBoolean("kernelSupported", capabilities != 0),
            ),
            supported = obj.optBoolean("supported", capabilities != 0) && capabilities != 0,
            kernelSupported = obj.optBoolean("kernelSupported", capabilities != 0),
            policyEnabled = obj.optBoolean("policyEnabled", true),
            lateLoad = obj.optBoolean("lateLoad", false),
            abiVersion = obj.optInt("abiVersion", 0),
            capabilities = capabilities,
            maxImageSize = obj.optLong("maxImageSize", 0L),
            maxLoaded = obj.optInt("maxLoaded", 0),
            disabledReason = obj.optString("disabledReason", ""),
        )
    }.getOrElse { error ->
        KpmCaps(error = "Invalid KPM capability response: ${error.message.orEmpty()}")
    }
}

suspend fun setKpmPolicy(enabled: Boolean): KpmCommandResult {
    val action = if (enabled) "enable" else "disable"
    return runKsudCommandWithOutput("kpm policy $action", timeoutMillis = 30_000L)
        .toKpmResult()
}

suspend fun getKpmList(): KpmCommandResult {
    return runKsudCommandWithOutput("kpm list", timeoutMillis = SHELL_JOB_TIMEOUT_MILLIS)
        .toKpmResult()
}

suspend fun importKpm(
    source: File,
    args: String,
    force: Boolean,
    enable: Boolean,
): KpmCommandResult {
    val options = buildString {
        append("kpm import ")
        append(shellQuote(source.absolutePath))
        append(" --trusted --args ")
        append(shellQuote(args))
        if (force) append(" --force")
        if (enable) append(" --enable")
    }
    return runKsudCommandWithOutput(options, timeoutMillis = 30_000L).toKpmResult()
}

suspend fun setKpmEnabled(id: String, enabled: Boolean): KpmCommandResult {
    val action = if (enabled) "enable" else "disable"
    return runKsudCommandWithOutput("kpm $action ${shellQuote(id)}", timeoutMillis = 30_000L)
        .toKpmResult()
}

suspend fun loadKpm(id: String): KpmCommandResult {
    return runKsudCommandWithOutput("kpm load ${shellQuote(id)}", timeoutMillis = 30_000L)
        .toKpmResult()
}

suspend fun unloadKpm(id: String): KpmCommandResult {
    return runKsudCommandWithOutput("kpm unload ${shellQuote(id)}", timeoutMillis = 30_000L)
        .toKpmResult()
}

suspend fun removeKpm(id: String): KpmCommandResult {
    return runKsudCommandWithOutput("kpm remove ${shellQuote(id)}", timeoutMillis = 30_000L)
        .toKpmResult()
}

suspend fun controlKpm(id: String, args: String): KpmCommandResult {
    return runKsudCommandWithOutput(
        "kpm control ${shellQuote(id)} --args ${shellQuote(args)}",
        timeoutMillis = 30_000L,
    ).toKpmResult()
}

suspend fun getKpmExcludedApps(): List<KpmExcludedApp> {
    val result = runKsudCommandWithOutput("kpm exclude-list").toKpmResult()
    if (!result.success) error(result.error)
    val array = JSONArray(result.output)
    return buildList {
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val packageName = obj.optString("package").trim()
            val uid = obj.optInt("uid", -1)
            if (packageName.isNotBlank() && uid > 0) {
                add(KpmExcludedApp(packageName, uid))
            }
        }
    }
}

suspend fun setKpmAppExcluded(
    packageName: String,
    uid: Int,
    excluded: Boolean,
): KpmCommandResult {
    return runKsudCommandWithOutput(
        "kpm exclude ${shellQuote(packageName)} $uid --enabled $excluded",
        timeoutMillis = 30_000L,
    ).toKpmResult()
}

fun parseKpmEntries(content: String): List<KpmEntry> {
    val array = JSONArray(content)
    return buildList {
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val id = obj.optString("id").trim()
            if (id.isBlank()) continue
            add(
                KpmEntry(
                    id = id,
                    name = obj.optString("name", id),
                    version = obj.optString("version", ""),
                    license = obj.optString("license", ""),
                    author = obj.optString("author", ""),
                    description = obj.optString("description", ""),
                    args = obj.optString("args", ""),
                    enabled = obj.optBoolean("enabled", false),
                    loaded = obj.optBoolean("loaded", false),
                    quarantined = obj.optBoolean("quarantined", false),
                    quarantineReason = obj.optString("quarantineReason", ""),
                    sourceName = obj.optString("sourceName", ""),
                    importedAt = obj.optString("importedAt", ""),
                    error = obj.optString("error", ""),
                ),
            )
        }
    }
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

private suspend fun runStructuredKsudCommand(
    area: String,
    command: String,
    timeoutMillis: Long = STATUS_TIMEOUT_MILLIS,
): ToolCommandResult = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext ToolCommandResult(
            errorCode = "$area.root_unavailable",
            errorMessage = "root shell unavailable",
        )
    }
    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(timeoutMillis) {
            getRootShell().newJob()
                .add("${shellQuote(getKsuDaemonPath())} $command")
                .to(stdout, stderr)
                .exec()
        }
        if (result == null) {
            KsuCli.reset()
            return@runCatching ToolCommandResult(
                errorCode = "$area.timeout",
                errorMessage = "$area command timed out",
                timedOut = true,
            )
        }
        if (result.isSuccess) {
            ToolCommandResult(success = true)
        } else {
            val raw = stderr.joinToString("\n").trim().ifBlank {
                stdout.joinToString("\n").trim().ifBlank { "$area command failed" }
            }
            val structured = parseStructuredKsudError(raw, "$area.command_failed")
            Log.w(TAG, "$area command failed: $command, $raw")
            structured
        }
    }.getOrElse { error ->
        KsuCli.reset()
        ToolCommandResult(
            errorCode = "$area.unavailable",
            errorMessage = error.message.orEmpty().ifBlank { "$area command unavailable" },
        )
    }
}

private suspend fun getKsudTextOutput(
    command: String,
    timeoutMillis: Long = STATUS_TIMEOUT_MILLIS,
): String = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext ""
    }
    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(timeoutMillis) {
            getRootShell().newJob()
                .add("${shellQuote(getKsuDaemonPath())} $command")
                .to(stdout, stderr)
                .exec()
        }
        if (result == null) {
            KsuCli.reset()
            return@runCatching ""
        }
        if (result.isSuccess) {
            stdout.joinToString("\n")
        } else {
            stderr.joinToString("\n").ifBlank { stdout.joinToString("\n") }
        }
    }.getOrElse { error ->
        Log.w(TAG, "ksud text command unavailable: $command", error)
        KsuCli.reset()
        ""
    }
}

internal fun parseStructuredKsudError(raw: String, fallbackCode: String): ToolCommandResult {
    val payload = raw.substringAfter(ERROR_PREFIX, "")
    if (payload.isBlank()) {
        return ToolCommandResult(errorCode = fallbackCode, errorMessage = raw.trim())
    }
    val code = payload.substringBefore(':').trim().ifBlank { fallbackCode }
    val message = payload.substringAfter(':', "").trim().ifBlank { raw.trim() }
    return ToolCommandResult(errorCode = code, errorMessage = message)
}

suspend fun readHiddenPathConfig(): HiddenPathConfigReadResult = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext HiddenPathConfigReadResult(error = "root shell unavailable")
    }

    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(STATUS_TIMEOUT_MILLIS) {
            getRootShell().newJob()
                .add("${getKsuDaemonPath()} pathmask status")
                .to(stdout, stderr)
                .exec()
        }

        if (result == null) {
            Log.w(TAG, "pathmask status timed out")
            KsuCli.reset()
            return@runCatching HiddenPathConfigReadResult(error = "pathmask status timed out")
        }

        if (!result.isSuccess) {
            val error = stderr.joinToString("\n").trim().ifBlank { "pathmask status failed" }
            Log.w(TAG, "pathmask status failed: $error")
            return@runCatching HiddenPathConfigReadResult(error = error)
        }

        val obj = JSONObject(stdout.joinToString("\n"))
        val statusError = obj.optString("error", "").trim()
        if (statusError.isNotBlank()) {
            return@runCatching HiddenPathConfigReadResult(
                error = statusError,
                errorCode = obj.optString("errorCode", "pathmask.status_failed"),
            )
        }
        HiddenPathConfigReadResult(config = HiddenPathConfigState(
            targetPaths = obj.optJSONArray("targetPaths").toStringList(),
            appPackages = obj.optJSONArray("appPackages").toStringList(),
            useAppScope = obj.optBoolean("useAppScope", true),
            hideDirents = obj.optBoolean("hideDirents", true),
            hideIsolated = obj.optBoolean("hideIsolated", true),
            autoLoadEnabled = obj.optBoolean("autoLoadEnabled", true),
            autoLoadDelaySeconds = obj.optInt("autoLoadDelaySeconds", 0)
                .coerceIn(0, HIDDEN_PATH_MAX_AUTO_LOAD_DELAY_SECONDS),
            autoLoadRemainingSeconds = obj.optInt("autoLoadRemainingSeconds", 0)
                .coerceAtLeast(0),
            loaded = obj.optBoolean("loaded", false),
            currentKmi = obj.optString("currentKmi", ""),
            phase = obj.optString("phase", "unconfigured"),
            savedCount = obj.optInt("savedCount", 0),
            availableCount = obj.optInt("availableCount", 0),
            activeCount = obj.optInt("activeCount", 0),
            resolvedCount = obj.optString("resolvedCount", ""),
            activeTargetPaths = obj.optString("activeTargetPaths", ""),
            missingTargetPaths = obj.optJSONArray("missingTargetPaths").toStringList(),
            unresolvedTargetCount = obj.optInt("unresolvedTargetCount", 0).coerceAtLeast(0),
            unresolvedTargetPaths = obj.optJSONArray("unresolvedTargetPaths").toStringList(),
            requiresReload = obj.optBoolean("requiresReload", false),
            requiresReboot = obj.optBoolean("requiresReboot", false),
            hasPendingCandidate = obj.optBoolean("hasPendingCandidate", false),
            lastErrorCode = obj.optString("lastErrorCode", ""),
            lastErrorMessage = obj.optString("lastErrorMessage", ""),
            lastLog = obj.optString("lastLog", ""),
            resolvedAppUids = obj.optJSONArray("resolvedAppUids").toStringList(),
            unresolvedAppPackages = obj.optJSONArray("unresolvedAppPackages").toStringList(),
        ))
    }.getOrElse {
        Log.w(TAG, "pathmask status unavailable", it)
        KsuCli.reset()
        HiddenPathConfigReadResult(
            error = it.message ?: "pathmask status unavailable",
            errorCode = "pathmask.status_unavailable",
        )
    }
}

suspend fun getHiddenPathConfig(): HiddenPathConfigState =
    readHiddenPathConfig().config ?: HiddenPathConfigState()

suspend fun saveAndApplyHiddenPathConfig(config: HiddenPathConfigState): ToolCommandResult {
    return runStructuredKsudCommand(
        area = "pathmask",
        command = "pathmask apply-json ${shellQuote(config.toConfigJson())}",
        timeoutMillis = LONG_IO_TIMEOUT_MILLIS,
    )
}

suspend fun setHiddenPathAutoLoad(
    enabled: Boolean,
    delaySeconds: Int? = null,
): ToolCommandResult {
    val safeDelay = delaySeconds?.coerceIn(0, HIDDEN_PATH_MAX_AUTO_LOAD_DELAY_SECONDS)
    val delayArgument = safeDelay?.let { " --delay-seconds $it" }.orEmpty()
    return runStructuredKsudCommand(
        area = "pathmask",
        command = "pathmask set-auto-load $enabled$delayArgument",
    )
}

suspend fun unloadHiddenPathKernelPaths(): ToolCommandResult = runStructuredKsudCommand(
    area = "pathmask",
    command = "pathmask unload",
)

suspend fun deleteHiddenPathConfig(): ToolCommandResult = runStructuredKsudCommand(
    area = "pathmask",
    command = "pathmask delete-config",
)

suspend fun getHiddenPathDiagnostics(): String = getKsudTextOutput(
    command = "pathmask diagnostics",
    timeoutMillis = DIAGNOSTIC_TIMEOUT_MILLIS,
)

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

internal fun susfsPathServiceScript(): String = """#!/system/bin/sh
CONFIG=$SUSFS_PATH_CONFIG_FILE
find_tool() {
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
    [ -x "${'$'}TOOL" ]
}

apply_paths() {
    failed=0
    while IFS= read -r target_path; do
        case "${'$'}target_path" in
            /*) "${'$'}TOOL" add_sus_path "${'$'}target_path" >/dev/null 2>&1 || failed=1 ;;
        esac
    done < "${'$'}CONFIG"
    [ "${'$'}failed" -eq 0 ]
}

attempt=0
while [ "${'$'}attempt" -lt 30 ]; do
    [ -f "${'$'}CONFIG" ] || exit 0
    if find_tool && apply_paths; then
        exit 0
    fi
    attempt=${'$'}((attempt + 1))
    sleep 1
done
exit 0
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

suspend fun clearHiddenPathLogs(): ToolCommandResult = runStructuredKsudCommand(
    area = "pathmask",
    command = "pathmask clear-logs",
)

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
        return@withContext RescueStatus(
            statusErrorCode = "rescue.root_unavailable",
            statusError = "ksud command unavailable",
        )
    }

    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(STATUS_TIMEOUT_MILLIS) {
            getRootShell().newJob()
                .add("${getKsuDaemonPath()} rescue status")
                .to(stdout, stderr)
                .exec()
        }

        if (result == null) {
            Log.w(TAG, "rescue status timed out")
            KsuCli.reset()
            return@runCatching RescueStatus(
                statusErrorCode = "rescue.timeout",
                statusError = "rescue status timed out",
            )
        }

        if (!result.isSuccess) {
            val error = stderr.joinToString("\n").ifBlank { "rescue status command failed" }
            Log.w(TAG, "rescue status failed: $error")
            val structured = parseStructuredKsudError(error, "rescue.status_failed")
            return@runCatching RescueStatus(
                statusErrorCode = structured.errorCode,
                statusError = structured.errorMessage,
            )
        }

        val obj = JSONObject(stdout.joinToString("\n"))
        val manifest = obj.optJSONObject("manifest")
        val device = obj.optJSONObject("device")
        val manifestDevice = manifest?.optJSONObject("device")
        val images = obj.optJSONArray("images").toRescueImageList()
        RescueStatus(
            available = obj.optBoolean("statusOk", true),
            phase = obj.optString("phase", "unavailable"),
            statusErrorCode = obj.optString("statusErrorCode", ""),
            statusError = obj.optString("statusError", ""),
            enabled = obj.optBoolean("enabled", false),
            config = obj.optJSONObject("config").toRescueConfigState(),
            images = images,
            bootCount = obj.optInt("bootCount", 0),
            autoRestoreAttempts = obj.optInt("autoRestoreAttempts", 0),
            pendingBoot = obj.optBoolean("pendingBoot", false),
            currentSlot = obj.optString("currentSlot", ""),
            bootMode = obj.optString("bootMode", ""),
            device = listOf(
                device?.optString("brand", "").orEmpty(),
                device?.optString("model", "").orEmpty(),
            ).filter(String::isNotBlank).joinToString(" "),
            deviceFingerprint = device?.optString("fingerprint", "").orEmpty(),
            manifestCreatedAt = manifest?.optString("createdAt", "").orEmpty(),
            manifestSlot = manifest?.optString("slot", "").orEmpty(),
            manifestDevice = listOf(
                manifestDevice?.optString("brand", "").orEmpty(),
                manifestDevice?.optString("model", "").orEmpty(),
                manifestDevice?.optString("device", "").orEmpty(),
            ).filter(String::isNotBlank).distinct().joinToString(" "),
            manifestFingerprint = manifestDevice?.optString("fingerprint", "").orEmpty(),
            manifestTotalSize = images.filter(RescueImageState::exists).sumOf(RescueImageState::size),
            lastRestoreDone = obj.optBoolean("lastRestoreDone", false),
            skipModulesOnce = obj.optBoolean("skipModulesOnce", false),
            skipModulesThisBoot = obj.optBoolean("skipModulesThisBoot", false),
            ready = obj.optBoolean("ready", false),
            readyReason = obj.optString("readyReason", ""),
            verified = obj.optBoolean("verified", false),
            environmentChecked = obj.optBoolean("environmentChecked", false),
            configChangedProtectionDisabled = obj.optBoolean("configChangedProtectionDisabled", false),
            restoreInterrupted = obj.optBoolean("restoreInterrupted", false),
            restoreTransactionError = obj.optString("restoreTransactionError", ""),
            restoreTransaction = obj.optJSONObject("restoreTransaction").toRescueRestoreTransaction(),
            rescueDisabledModules = obj.optJSONArray("rescueDisabledModules").toRescueDisabledModules(),
            log = obj.optString("log", ""),
        )
    }.getOrElse {
        Log.w(TAG, "rescue status unavailable", it)
        KsuCli.reset()
        RescueStatus(
            statusErrorCode = "rescue.status_unavailable",
            statusError = it.message.orEmpty().ifBlank { "rescue status unavailable" },
        )
    }
}

suspend fun runRescueCommand(
    command: String,
    timeoutMultiplier: Long = 6,
): ToolCommandResult = runStructuredKsudCommand(
    area = "rescue",
    command = "rescue $command",
    timeoutMillis = SHELL_JOB_TIMEOUT_MILLIS * timeoutMultiplier.coerceAtLeast(1),
)

suspend fun saveRescueConfig(config: RescueConfigState): ToolCommandResult = runStructuredKsudCommand(
    area = "rescue",
    command = "rescue import-config-json ${shellQuote(config.toConfigJson())}",
    timeoutMillis = STATUS_TIMEOUT_MILLIS,
)

suspend fun importRescueImage(partition: String, sourcePath: String, force: Boolean): Boolean = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext false
    }

    runCatching {
        val stderr = ArrayList<String>()
        val forceArg = if (force) " --force" else ""
        val result = withTimeoutOrNull(SHELL_JOB_TIMEOUT_MILLIS * 30) {
            getRootShell().newJob()
                .add(
                    "${getKsuDaemonPath()} rescue import-image " +
                        "${shellQuote(partition)} ${shellQuote(sourcePath)}$forceArg"
                )
                .to(null, stderr)
                .exec()
        }
        if (result == null) {
            Log.w(TAG, "rescue image import timed out")
            KsuCli.reset()
            return@runCatching false
        }
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
        val result = withTimeoutOrNull(STATUS_TIMEOUT_MILLIS) {
            getRootShell().newJob()
                .add("${shellQuote(getKsuDaemonPath())} rescue test")
                .to(stdout, stderr)
                .exec()
        }
        if (result == null) {
            KsuCli.reset()
            return@runCatching RescueTestReport(
                errorCode = "rescue.timeout",
                reason = "rescue environment check timed out",
            )
        }
        val raw = stdout.joinToString("\n")
        val obj = JSONObject(raw)
        RescueTestReport(
            ok = result.isSuccess && obj.optBoolean("ok", false),
            errorCode = obj.optString("errorCode", ""),
            reason = obj.optString("reason", stderr.joinToString("\n")),
            text = raw,
            backupReady = obj.optBoolean("backupReady", false),
            backupReason = obj.optString("backupReason", ""),
        )
    }.getOrElse {
        Log.w(TAG, "rescue test unavailable", it)
        KsuCli.reset()
        RescueTestReport(reason = it.message.orEmpty())
    }
}

suspend fun verifyRescueBackups(): RescueTestReport = withContext(Dispatchers.IO) {
    if (shouldSkipUnsafeKsudCommand()) {
        return@withContext RescueTestReport(
            errorCode = "rescue.root_unavailable",
            reason = "ksud command unavailable",
        )
    }
    runCatching {
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val result = withTimeoutOrNull(LONG_IO_TIMEOUT_MILLIS) {
            getRootShell().newJob()
                .add("${shellQuote(getKsuDaemonPath())} rescue verify")
                .to(stdout, stderr)
                .exec()
        }
        if (result == null) {
            KsuCli.reset()
            return@runCatching RescueTestReport(
                errorCode = "rescue.timeout",
                reason = "rescue verification timed out",
            )
        }
        val raw = stdout.joinToString("\n")
        val obj = runCatching { JSONObject(raw) }.getOrNull()
        RescueTestReport(
            ok = result.isSuccess && obj?.optBoolean("ok", false) == true,
            errorCode = obj?.optString("errorCode", "").orEmpty(),
            reason = obj?.optString("reason", stderr.joinToString("\n")).orEmpty(),
            text = raw.ifBlank { stderr.joinToString("\n") },
        )
    }.getOrElse { error ->
        Log.w(TAG, "rescue verification unavailable", error)
        KsuCli.reset()
        RescueTestReport(
            errorCode = "rescue.unavailable",
            reason = error.message.orEmpty(),
        )
    }
}

suspend fun getRescueLogs(): String = getKsudTextOutput(
    command = "rescue logs",
    timeoutMillis = DIAGNOSTIC_TIMEOUT_MILLIS,
)

suspend fun getRescueDiagnostics(): String = getKsudTextOutput(
    command = "rescue diagnostics",
    timeoutMillis = DIAGNOSTIC_TIMEOUT_MILLIS,
)

suspend fun enableRescueModule(id: String): ToolCommandResult = runStructuredKsudCommand(
    area = "rescue",
    command = "rescue enable-module ${shellQuote(id)}",
    timeoutMillis = STATUS_TIMEOUT_MILLIS,
)

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
        return (0 until array.length()).count { index ->
            val id = array.optJSONObject(index)?.optString("id").orEmpty()
            id.isNotBlank() && !isManagerHiddenModuleId(id)
        }
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

private fun writeLkmFile(lkm: LkmSelection): File? {
    if (lkm !is LkmSelection.LkmUri) return null
    return copyUriToCache(lkm.uri, "kernelsu-tmp-lkm.ko")
}

private fun bootPatchFlags(
    allowShell: Boolean,
    enableAdb: Boolean,
    forceBackup: Boolean,
): String = buildString {
    if (allowShell) append(" --allow-shell")
    if (enableAdb) append(" --enable-adbd")
    if (forceBackup) append(" --backup")
}

enum class BootPatchMode {
    Normal,
    HiddenPath,
}

internal fun BootPatchMode.cliArguments(): String = when (this) {
    BootPatchMode.Normal -> ""
    BootPatchMode.HiddenPath -> " --pathmask-lkm"
}

suspend fun installBoot(
    bootUri: Uri?,
    lkm: LkmSelection,
    patchMode: BootPatchMode,
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

        val effectivePatchMode = if (lkm is LkmSelection.PathMaskKmiString) {
            BootPatchMode.HiddenPath
        } else {
            patchMode
        }
        cmd += effectivePatchMode.cliArguments()

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
                cmd += " --kmi ${shellQuote(lkm.value)}"
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

        FlashResult(result, bootUri == null)
    } finally {
        bootFile?.delete()
        lkmFile?.delete()
        patchedOutput?.delete()
    }
}

/** Downloads a factory/OTA archive, extracts [partition], then patches it locally. */
suspend fun downloadBoot(
    url: String,
    partition: String,
    lkm: LkmSelection,
    patchMode: BootPatchMode,
    allowShell: Boolean,
    enableAdb: Boolean,
    forceBackup: Boolean,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit,
): FlashResult = withContext(Dispatchers.IO) {
    val bootFile = File(ksuApp.cacheDir, "download-boot.img")
    var lkmFile: File? = null
    var patchedOutput: File? = null
    try {
        onStdout("- Downloading and extracting $partition")
        val channel = DataSourceChannel(newDownloadClient(), url)
        val magic = try {
            readMagic(channel)
        } finally {
            channel.position(0)
        }
        val image = ExtractImage(bootFile, onStdout)
        val probeChannel = DataSourceChannel(newDownloadClient(), url)
        val probedKmi = try {
            if (magic == "CrAU") {
                ExtractImage.probePayload(
                    probeChannel,
                    withKmi = lkm is LkmSelection.KmiNone,
                    onProgress = onStdout,
                ).kmi
            } else {
                ExtractImage.probe(
                    probeChannel,
                    withKmi = lkm is LkmSelection.KmiNone,
                    onProgress = onStdout,
                ).kmi
            }
        } finally {
            probeChannel.close()
        }
        try {
            if (magic == "CrAU") image.consumePayload(channel, partition) else image.consume(channel, partition)
        } finally {
            channel.close()
        }

        val autoKmi = if (lkm is LkmSelection.KmiNone) {
            (probedKmi ?: BootKernelVersion.parseKmiFromBoot(bootFile))?.also {
                onStdout("- Auto detected KMI: $it")
            }
        } else {
            null
        }
        if (autoKmi == null && lkm is LkmSelection.KmiNone) {
            return@withContext FlashResult(-1, "Failed to determine KMI from the package", false)
        }

        val effectivePatchMode = if (lkm is LkmSelection.PathMaskKmiString) {
            BootPatchMode.HiddenPath
        } else {
            patchMode
        }
        var cmd = "${shellQuote(getKsuDaemonPath())} boot-patch -b ${shellQuote(bootFile.absolutePath)}"
        if (allowShell) cmd += " --allow-shell"
        if (enableAdb) cmd += " --enable-adbd"
        if (forceBackup) cmd += " --backup"
        cmd += effectivePatchMode.cliArguments()

        when (lkm) {
            is LkmSelection.LkmUri -> {
                val selectedLkmFile = copyUriToCache(lkm.uri, "kernelsu-tmp-lkm.ko")
                lkmFile = selectedLkmFile
                cmd += " -m ${shellQuote(selectedLkmFile.absolutePath)}"
            }
            is LkmSelection.KmiString -> cmd += " --kmi ${shellQuote(lkm.value)}"
            is LkmSelection.PathMaskKmiString -> cmd += " --kmi ${shellQuote(lkm.value)}"
            LkmSelection.KmiNone -> Unit
        }
        if (autoKmi != null) cmd += " --kmi ${shellQuote(autoKmi)}"
        cmd += " --partition ${shellQuote(partition)}"

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val outputName = "apkesu_patched_$timestamp.img"
        patchedOutput = preparePatchedImageOutput(ksuApp.cacheDir, outputName)
        cmd += " -o ${shellQuote(ksuApp.cacheDir.absolutePath)} --out-name ${shellQuote(outputName)}"

        val result = flashWithIO(cmd, onStdout, onStderr)
        if (!result.isSuccess) return@withContext FlashResult(result, false)

        val output = requireNotNull(patchedOutput)
        var outputError = validatePatchedImageOutput(output)
        if (outputError != null && output.isFile && output.length() > 0L) {
            restorePatchedImageAccess(output)
            outputError = validatePatchedImageOutput(output)
        }
        if (outputError != null) {
            val error = "Patched image output is unavailable: $outputError"
            onStderr(error)
            return@withContext FlashResult(1, error, false)
        }
        val savedPath = runCatching {
            saveFileToDownloads(ksuApp, output.name, output)
        }.getOrElse { throwable ->
            val error = "Failed to save patched image: ${throwable.localizedMessage ?: throwable.javaClass.simpleName}"
            onStderr(error)
            return@withContext FlashResult(1, error, false)
        }
        onStdout("- Patched image saved to $savedPath")
        FlashResult(result, false)
    } catch (error: Exception) {
        onStderr(error.localizedMessage ?: error.javaClass.simpleName)
        FlashResult(-1, error.localizedMessage ?: "Download failed", false)
    } finally {
        bootFile.delete()
        lkmFile?.delete()
        patchedOutput?.delete()
    }
}

suspend fun probeRemoteBootPartitions(url: String): ProbeResult = withContext(Dispatchers.IO) {
    DataSourceChannel(newDownloadClient(), url).use { channel ->
        val magic = readMagic(channel)
        if (magic == "CrAU") {
            ExtractImage.probePayload(channel, withKmi = false)
        } else {
            ExtractImage.probe(channel, withKmi = false)
        }
    }
}

private fun newDownloadClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

private fun readMagic(channel: DataSourceChannel): String {
    val buffer = ByteBuffer.allocate(4)
    channel.read(buffer)
    channel.position(0)
    return String(buffer.array(), StandardCharsets.ISO_8859_1)
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
            lastManagerRegistrationFailureKey = null
            lastManagerRegistrationFailureAt = 0L
            return@synchronized true
        }
        if (!apkeSuRootAvailable()) {
            return@synchronized false
        }

        val managerUid = Os.getuid()
        val managerAppId = managerUid.mod(100_000)
        if (managerAppId !in FIRST_APPLICATION_APPID..LAST_APPLICATION_APPID) {
            Log.e(TAG, "refusing manager registration for non-application uid $managerUid")
            return@synchronized false
        }
        val driverVersion = runCatching { Natives.version }.getOrDefault(0)
        val failureKey = "$driverVersion:$managerUid"
        val now = SystemClock.elapsedRealtime()
        if (failureKey == lastManagerRegistrationFailureKey &&
            now - lastManagerRegistrationFailureAt < MANAGER_REGISTRATION_RETRY_MILLIS
        ) {
            Log.w(TAG, "skip repeated manager registration for driver $driverVersion")
            return@synchronized false
        }
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
            lastManagerRegistrationFailureKey = failureKey
            lastManagerRegistrationFailureAt = SystemClock.elapsedRealtime()
            Log.w(TAG, "register manager appid failed: ${result?.err?.joinToString("\n")}")
            return@synchronized false
        }

        KsuCli.reset()
        val registered = runCatching {
            Natives.refreshInfo()
            Natives.isManager
        }.getOrDefault(false)
        if (registered) {
            lastManagerRegistrationFailureKey = null
            lastManagerRegistrationFailureAt = 0L
        } else {
            lastManagerRegistrationFailureKey = failureKey
            lastManagerRegistrationFailureAt = SystemClock.elapsedRealtime()
            Log.w(TAG, "manager registration command succeeded but kernel identity did not refresh")
        }
        registered
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

internal enum class BootImageKmiSource {
    Image,
    CurrentDevice,
}

internal data class BootImageKmiDetection(
    val kmi: String,
    val source: BootImageKmiSource,
)

internal suspend fun detectBootImageKmi(
    uri: Uri,
    fallbackKmi: String? = null,
): BootImageKmiDetection = withContext(Dispatchers.IO) {
    var bootFile: File? = null
    try {
        val selectedBootFile = copyUriToCache(uri, "boot-kmi.img")
        bootFile = selectedBootFile
        val imageHasNoKernel = runCatching {
            BootKernelVersion.isKernellessBootImage(selectedBootFile)
        }.getOrDefault(false)

        // Parse the selected image locally first. This path understands compressed
        // kernel payloads and also works before the daemon has been installed.
        runCatching { BootKernelVersion.parseKmiFromBoot(selectedBootFile) }
            .getOrNull()
            ?.takeIf { it.matches(kmiNameRegex) }
            ?.let { return@withContext BootImageKmiDetection(it, BootImageKmiSource.Image) }

        // Keep ksud as a compatibility fallback for boot formats not covered by
        // the lightweight manager parser.
        val stdout = ArrayList<String>()
        val stderr = ArrayList<String>()
        val command = "${shellQuote(getKsuDaemonPath())} boot-info image-kmi " +
            "--boot ${shellQuote(selectedBootFile.absolutePath)}"
        val result = withNewRootShell {
            newJob().add(command).to(stdout, stderr).exec()
        }
        if (result.isSuccess) {
            stdout.asSequence()
                .map(String::trim)
                .firstOrNull { it.matches(kmiNameRegex) }
                ?.let { return@withContext BootImageKmiDetection(it, BootImageKmiSource.Image) }
        }

        // init_boot images intentionally contain no kernel. For those images the
        // current device KMI is the correct target, while a normal unreadable boot
        // image must still fail and require manual selection.
        if (imageHasNoKernel) {
            val currentKmi = (fallbackKmi ?: getCurrentKmi())
                .takeIf { it.matches(kmiNameRegex) }
            if (currentKmi != null) {
                return@withContext BootImageKmiDetection(
                    currentKmi,
                    BootImageKmiSource.CurrentDevice,
                )
            }
        }

        val detail = stderr.joinToString("\n").trim()
        error(detail.ifBlank { "The selected image does not contain a supported KMI marker" })
    } finally {
        bootFile?.delete()
    }
}

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
        dangerous = optBoolean("dangerous", false),
        verificationState = optString("verificationState", "unknown"),
    )
}

private fun JSONObject?.toRescueRestoreTransaction(): RescueRestoreTransaction? {
    if (this == null) return null
    val entriesJson = optJSONArray("entries")
    val entries = buildList {
        if (entriesJson != null) {
            for (index in 0 until entriesJson.length()) {
                val entry = entriesJson.optJSONObject(index) ?: continue
                add(
                    RescueRestoreEntry(
                        name = entry.optString("name", ""),
                        label = entry.optString("label", ""),
                        imagePath = entry.optString("imagePath", ""),
                        devicePath = entry.optString("devicePath", ""),
                        expectedSha256 = entry.optString("expectedSha256", ""),
                        expectedSize = entry.optLong("expectedSize", 0),
                        status = entry.optString("status", ""),
                    )
                )
            }
        }
    }
    return RescueRestoreTransaction(
        id = optString("id", ""),
        reason = optString("reason", ""),
        automatic = optBoolean("automatic", false),
        description = optString("description", ""),
        activateSlot = optString("activateSlot", ""),
        phase = optString("phase", ""),
        errorCode = optString("errorCode", ""),
        errorMessage = optString("errorMessage", ""),
        startedAt = optString("startedAt", ""),
        updatedAt = optString("updatedAt", ""),
        entries = entries,
    )
}

private fun JSONArray?.toRescueDisabledModules(): List<RescueDisabledModule> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val module = optJSONObject(index) ?: continue
            add(
                RescueDisabledModule(
                    id = module.optString("id", ""),
                    name = module.optString("name", ""),
                    version = module.optString("version", ""),
                    installed = module.optBoolean("installed", false),
                    disabled = module.optBoolean("disabled", false),
                )
            )
        }
    }
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
