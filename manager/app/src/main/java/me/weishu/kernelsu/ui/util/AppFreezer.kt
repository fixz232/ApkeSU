package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.data.repository.SuperUserRepository
import me.weishu.kernelsu.data.repository.SuperUserRepositoryImpl

private const val ANDROID_UIDS_PER_USER = 100_000
private const val FIRST_APPLICATION_UID = 10_000
private const val APP_FREEZE_BASE = "/data/adb/apkesu/app_freeze"
private const val APP_FREEZE_CONFIG = "$APP_FREEZE_BASE/frozen_apps.tsv"
private const val APP_FREEZE_SERVICE = "/data/adb/service.d/95-apkesu-app-freeze.sh"
private const val APP_FREEZE_COMMAND_TIMEOUT_MILLIS = 10_000L
private const val APP_FREEZE_RESTORE_ATTEMPTS = 30
private const val APP_FREEZE_MAX_ENTRIES = 4096

private val APP_FREEZE_PACKAGE_PATTERN = Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*")
private val APP_FREEZE_STATE_PATTERN =
    Regex("^Package\\s+(.+?)\\s+new suspended state:\\s+(true|false)$", RegexOption.IGNORE_CASE)

private val criticalFreezePackages = setOf(
    "android",
    "com.android.packageinstaller",
    "com.android.permissioncontroller",
    "com.android.settings",
    "com.android.systemui",
    "com.google.android.packageinstaller",
    "com.google.android.permissioncontroller",
)

data class AppFreezeKey(
    val packageName: String,
    val userId: Int,
)

enum class AppFreezeProtection {
    Manager,
    Launcher,
    CriticalSystem,
    CoreUid,
}

data class FreezableApp(
    val key: AppFreezeKey,
    val label: String,
    val packageInfo: PackageInfo,
    val frozen: Boolean,
    val systemApp: Boolean,
    val protection: AppFreezeProtection?,
) {
    val packageName: String
        get() = key.packageName
    val userId: Int
        get() = key.userId
}

enum class AppFreezeFailure {
    InvalidTarget,
    ProtectedTarget,
    AppNotFound,
    RootUnavailable,
    CommandFailed,
    VerificationFailed,
    PersistenceFailed,
}

class AppFreezeException(
    val failure: AppFreezeFailure,
    message: String = failure.name,
) : IllegalStateException(message)

class AppFreezer(
    context: Context,
    private val appRepository: SuperUserRepository = SuperUserRepositoryImpl(),
) {
    private val appContext = context.applicationContext
    private val operationMutex = Mutex()

    suspend fun loadApps(): Result<List<FreezableApp>> = withContext(Dispatchers.IO) {
        runCatching { loadAppsOrThrow() }
    }

    suspend fun setFrozen(key: AppFreezeKey, frozen: Boolean): Result<FreezableApp> =
        operationMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    validateAppFreezeKey(key)
                    val before = loadAppsOrThrow().firstOrNull { it.key == key }
                        ?: throw AppFreezeException(AppFreezeFailure.AppNotFound)
                    if (frozen && before.protection != null) {
                        throw AppFreezeException(
                            AppFreezeFailure.ProtectedTarget,
                            before.protection.name,
                        )
                    }

                    val persistedBefore = readPersistedFreezeKeys()
                    if (before.frozen == frozen) {
                        persistFreezeKeys(
                            updatePersistedFreezeKeys(persistedBefore, key, frozen),
                        )
                        return@runCatching before
                    }

                    val stdout = arrayListOf<String>()
                    val stderr = arrayListOf<String>()
                    val result = try {
                        getRootShell().newJob()
                            .add(buildAppFreezeCommand(key, frozen))
                            .to(stdout, stderr)
                            .exec()
                    } catch (error: Throwable) {
                        throw AppFreezeException(
                            AppFreezeFailure.RootUnavailable,
                            error.message.orEmpty(),
                        )
                    }
                    if (!result.isSuccess) {
                        throw AppFreezeException(
                            AppFreezeFailure.CommandFailed,
                            appFreezeCommandError(stdout, stderr),
                        )
                    }
                    val reportedState = parseAppFreezeState(stdout, key.packageName)
                    if (reportedState != null && reportedState != frozen) {
                        throw AppFreezeException(
                            AppFreezeFailure.CommandFailed,
                            appFreezeCommandError(stdout, stderr),
                        )
                    }

                    val verified = loadAppsOrThrow().firstOrNull { it.key == key }
                        ?: throw AppFreezeException(AppFreezeFailure.AppNotFound)
                    if (verified.frozen != frozen) {
                        throw AppFreezeException(AppFreezeFailure.VerificationFailed)
                    }

                    try {
                        persistFreezeKeys(
                            updatePersistedFreezeKeys(persistedBefore, key, frozen),
                        )
                    } catch (error: Throwable) {
                        // Keep the system state and the durable record aligned if the
                        // filesystem update fails after the package command succeeded.
                        runCatching {
                            getRootShell().newJob()
                                .add(buildAppFreezeCommand(key, !frozen))
                                .exec()
                        }
                        if (error is AppFreezeException) throw error
                        throw AppFreezeException(
                            AppFreezeFailure.PersistenceFailed,
                            error.message.orEmpty(),
                        )
                    }

                    if (frozen) {
                        runCatching {
                            getRootShell().newJob()
                                .add(buildAppForceStopCommand(key))
                                .exec()
                        }
                    }
                    verified
                }
            }
        }

    private suspend fun loadAppsOrThrow(): List<FreezableApp> {
        val apps = appRepository.getAppList().getOrElse { throw it }
            .first
        val launcherPackages = queryLauncherPackages(appContext.packageManager)
        return apps.asSequence()
            .mapNotNull { app ->
                val applicationInfo = app.packageInfo.applicationInfo ?: return@mapNotNull null
                val userId = applicationInfo.uid / ANDROID_UIDS_PER_USER
                val key = AppFreezeKey(app.packageName, userId)
                FreezableApp(
                    key = key,
                    label = app.label,
                    packageInfo = app.packageInfo,
                    frozen = applicationInfo.flags and ApplicationInfo.FLAG_SUSPENDED != 0,
                    systemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                        applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
                    protection = resolveAppFreezeProtection(
                        packageName = app.packageName,
                        uid = applicationInfo.uid,
                        managerPackage = BuildConfig.APPLICATION_ID,
                        launcherPackages = launcherPackages,
                    ),
                )
            }
            .distinctBy(FreezableApp::key)
            .toList()
    }
}

private suspend fun readPersistedFreezeKeys(): List<AppFreezeKey> {
    val stdout = arrayListOf<String>()
    val stderr = arrayListOf<String>()
    val result = runCatching {
        withTimeoutOrNull(APP_FREEZE_COMMAND_TIMEOUT_MILLIS) {
            getRootShell().newJob()
                .add("if [ -r ${shellQuote(APP_FREEZE_CONFIG)} ]; then cat ${shellQuote(APP_FREEZE_CONFIG)}; fi")
                .to(stdout, stderr)
                .exec()
        }
    }.getOrElse { error ->
        throw AppFreezeException(
            AppFreezeFailure.PersistenceFailed,
            error.message.orEmpty().ifBlank { "read configuration failed" },
        )
    } ?: throw AppFreezeException(AppFreezeFailure.PersistenceFailed, "read configuration timed out")

    if (!result.isSuccess) {
        throw AppFreezeException(
            AppFreezeFailure.PersistenceFailed,
            (stderr + stdout).joinToString("\n").trim().ifBlank { "read configuration failed" },
        )
    }
    return parsePersistedAppFreezeKeys(stdout)
}

private suspend fun persistFreezeKeys(keys: List<AppFreezeKey>) {
    val configText = serializePersistedAppFreezeKeys(keys)
    val script = buildAppFreezePersistenceScript(configText)
    val stdout = arrayListOf<String>()
    val stderr = arrayListOf<String>()
    val result = runCatching {
        withTimeoutOrNull(APP_FREEZE_COMMAND_TIMEOUT_MILLIS) {
            getRootShell().newJob().add(script).to(stdout, stderr).exec()
        }
    }.getOrElse { error ->
        throw AppFreezeException(
            AppFreezeFailure.PersistenceFailed,
            error.message.orEmpty().ifBlank { "write configuration failed" },
        )
    } ?: throw AppFreezeException(AppFreezeFailure.PersistenceFailed, "write configuration timed out")

    if (!result.isSuccess) {
        throw AppFreezeException(
            AppFreezeFailure.PersistenceFailed,
            (stderr + stdout).joinToString("\n").trim().ifBlank { "write configuration failed" },
        )
    }
}

internal fun validateAppFreezeKey(key: AppFreezeKey) {
    if (!APP_FREEZE_PACKAGE_PATTERN.matches(key.packageName) || key.userId !in 0..99_999) {
        throw AppFreezeException(AppFreezeFailure.InvalidTarget)
    }
}

internal fun parsePersistedAppFreezeKeys(lines: Iterable<String>): List<AppFreezeKey> {
    return lines.asSequence()
        .map(String::trim)
        .mapNotNull { line ->
            val separator = line.indexOf('|')
            if (separator <= 0 || separator == line.lastIndex) return@mapNotNull null
            val userId = line.substring(0, separator).toIntOrNull() ?: return@mapNotNull null
            val packageName = line.substring(separator + 1)
            val key = AppFreezeKey(packageName, userId)
            if (!APP_FREEZE_PACKAGE_PATTERN.matches(packageName) || userId !in 0..99_999) {
                null
            } else {
                key
            }
        }
        .distinct()
        .sortedWith(compareBy<AppFreezeKey> { it.userId }.thenBy { it.packageName })
        .take(APP_FREEZE_MAX_ENTRIES)
        .toList()
}

internal fun updatePersistedFreezeKeys(
    existing: Iterable<AppFreezeKey>,
    key: AppFreezeKey,
    frozen: Boolean,
): List<AppFreezeKey> {
    validateAppFreezeKey(key)
    val current = existing.asSequence()
        .filter { candidate ->
            APP_FREEZE_PACKAGE_PATTERN.matches(candidate.packageName) && candidate.userId in 0..99_999
        }
        .distinct()
        .filterNot { it == key }
        .toMutableList()
    if (frozen) current += key
    return current
        .sortedWith(compareBy<AppFreezeKey> { it.userId }.thenBy { it.packageName })
        .also {
            if (it.size > APP_FREEZE_MAX_ENTRIES) {
                throw AppFreezeException(
                    AppFreezeFailure.PersistenceFailed,
                    "too many frozen applications",
                )
            }
        }
}

internal fun serializePersistedAppFreezeKeys(keys: Iterable<AppFreezeKey>): String {
    val normalized = keys.toList().distinct()
    normalized.forEach(::validateAppFreezeKey)
    if (normalized.size > APP_FREEZE_MAX_ENTRIES) {
        throw AppFreezeException(AppFreezeFailure.PersistenceFailed, "too many frozen applications")
    }
    return normalized
        .sortedWith(compareBy<AppFreezeKey> { it.userId }.thenBy { it.packageName })
        .joinToString(separator = "\n", postfix = if (normalized.isEmpty()) "" else "\n") {
            "${it.userId}|${it.packageName}"
        }
}

internal fun buildAppFreezeServiceScript(): String = buildString {
    appendLine("#!/system/bin/sh")
    appendLine("CONFIG=${shellQuote(APP_FREEZE_CONFIG)}")
    appendLine("is_valid_package() {")
    appendLine("    case \"${'$'}1\" in")
    appendLine("        ''|.*|*.|*..*|*[!A-Za-z0-9_.]*) return 1 ;;")
    appendLine("    esac")
    appendLine("    return 0")
    appendLine("}")
    appendLine("restore_once() {")
    appendLine("    [ -r \"${'$'}CONFIG\" ] || return 0")
    appendLine("    command -v cmd >/dev/null 2>&1 || return 1")
    appendLine("    command -v am >/dev/null 2>&1 || return 1")
    appendLine("    failed=0")
    appendLine("    while IFS='|' read -r user_id package_name; do")
    appendLine("        case \"${'$'}user_id\" in")
    appendLine("            ''|*[!0-9]*) continue ;;")
    appendLine("        esac")
    appendLine("        [ \"${'$'}user_id\" -le 99999 ] 2>/dev/null || continue")
    appendLine("        is_valid_package \"${'$'}package_name\" || continue")
    appendLine("        if ! cmd package suspend --user \"${'$'}user_id\" \"${'$'}package_name\" >/dev/null 2>&1; then")
    appendLine("            failed=1")
    appendLine("            continue")
    appendLine("        fi")
    appendLine("        am force-stop --user \"${'$'}user_id\" \"${'$'}package_name\" >/dev/null 2>&1 || true")
    appendLine("    done < \"${'$'}CONFIG\"")
    appendLine("    [ \"${'$'}failed\" -eq 0 ]")
    appendLine("}")
    appendLine("attempt=0")
    appendLine("while [ \"${'$'}attempt\" -lt $APP_FREEZE_RESTORE_ATTEMPTS ]; do")
    appendLine("    restore_once && exit 0")
    appendLine("    attempt=${'$'}((attempt + 1))")
    appendLine("    sleep 1")
    appendLine("done")
    appendLine("exit 0")
}

internal fun buildAppFreezePersistenceScript(configText: String): String = buildString {
    val configTemp = "$APP_FREEZE_CONFIG.tmp"
    val serviceTemp = "$APP_FREEZE_SERVICE.tmp"
    appendLine("set -e")
    appendLine("umask 077")
    appendLine("mkdir -p ${shellQuote(APP_FREEZE_BASE)} /data/adb/service.d")
    appendLine(
        "printf '%s' ${shellQuote(configText)} > ${shellQuote(configTemp)} && " +
            "chmod 0600 ${shellQuote(configTemp)}",
    )
    appendLine("cat > ${shellQuote(serviceTemp)} <<'__APKESU_APP_FREEZE_EOF__'")
    append(buildAppFreezeServiceScript())
    if (!endsWith("\n")) append('\n')
    appendLine("__APKESU_APP_FREEZE_EOF__")
    appendLine("chmod 0700 ${shellQuote(serviceTemp)}")
    appendLine("chown 0:0 ${shellQuote(configTemp)} ${shellQuote(serviceTemp)} 2>/dev/null || true")
    appendLine("mv -f ${shellQuote(configTemp)} ${shellQuote(APP_FREEZE_CONFIG)}")
    appendLine("mv -f ${shellQuote(serviceTemp)} ${shellQuote(APP_FREEZE_SERVICE)}")
    appendLine("if command -v restorecon >/dev/null 2>&1; then")
    appendLine("    restorecon ${shellQuote(APP_FREEZE_BASE)} ${shellQuote(APP_FREEZE_CONFIG)} ${shellQuote(APP_FREEZE_SERVICE)} >/dev/null 2>&1 || true")
    appendLine("fi")
}

internal fun buildAppFreezeCommand(key: AppFreezeKey, frozen: Boolean): String {
    validateAppFreezeKey(key)
    val action = if (frozen) "suspend" else "unsuspend"
    return "cmd package $action --user ${key.userId} '${key.packageName}'"
}

internal fun buildAppForceStopCommand(key: AppFreezeKey): String {
    validateAppFreezeKey(key)
    return "am force-stop --user ${key.userId} '${key.packageName}'"
}

internal fun parseAppFreezeState(output: List<String>, packageName: String): Boolean? {
    return output.asSequence()
        .map(String::trim)
        .mapNotNull { APP_FREEZE_STATE_PATTERN.matchEntire(it) }
        .firstOrNull { it.groupValues[1] == packageName }
        ?.groupValues
        ?.get(2)
        ?.equals("true", ignoreCase = true)
}

internal fun resolveAppFreezeProtection(
    packageName: String,
    uid: Int,
    managerPackage: String,
    launcherPackages: Set<String>,
): AppFreezeProtection? {
    return when {
        packageName == managerPackage -> AppFreezeProtection.Manager
        packageName in launcherPackages -> AppFreezeProtection.Launcher
        packageName in criticalFreezePackages -> AppFreezeProtection.CriticalSystem
        uid.mod(ANDROID_UIDS_PER_USER) < FIRST_APPLICATION_UID -> AppFreezeProtection.CoreUid
        else -> null
    }
}

private fun queryLauncherPackages(packageManager: PackageManager): Set<String> {
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return runCatching {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(
            homeIntent,
            PackageManager.MATCH_DISABLED_COMPONENTS,
        ).mapTo(linkedSetOf()) { it.activityInfo.packageName }
    }.getOrDefault(emptySet())
}

private fun appFreezeCommandError(stdout: List<String>, stderr: List<String>): String {
    return (stderr + stdout)
        .asSequence()
        .map(String::trim)
        .firstOrNull(String::isNotBlank)
        ?.take(240)
        .orEmpty()
}

private fun shellQuote(value: String): String = "'${value.replace("'", "'\"'\"'")}'"
