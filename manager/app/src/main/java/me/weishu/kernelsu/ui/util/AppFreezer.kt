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
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.data.repository.SuperUserRepository
import me.weishu.kernelsu.data.repository.SuperUserRepositoryImpl

private const val ANDROID_UIDS_PER_USER = 100_000
private const val FIRST_APPLICATION_UID = 10_000

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
                    if (before.frozen == frozen) return@runCatching before

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

internal fun validateAppFreezeKey(key: AppFreezeKey) {
    if (!APP_FREEZE_PACKAGE_PATTERN.matches(key.packageName) || key.userId !in 0..99_999) {
        throw AppFreezeException(AppFreezeFailure.InvalidTarget)
    }
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
