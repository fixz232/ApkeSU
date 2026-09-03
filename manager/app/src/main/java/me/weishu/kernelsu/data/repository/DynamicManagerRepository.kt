package me.weishu.kernelsu.data.repository

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.DynamicManagerCliState
import me.weishu.kernelsu.ui.util.clearDynamicManager
import me.weishu.kernelsu.ui.util.getDynamicManagerStatus
import me.weishu.kernelsu.ui.util.setDynamicManagerApk
import java.io.File
import java.util.zip.ZipFile

private const val PER_USER_RANGE = 100_000
private const val FIRST_APPLICATION_APP_ID = 10_000
private const val LAST_APPLICATION_APP_ID = 19_999

data class DynamicManagerCandidate(
    val packageInfo: PackageInfo,
    val label: String,
    val packageName: String,
    val apkPath: String,
    val appId: Int,
)

data class DynamicManagerSnapshot(
    val runtime: DynamicManagerCliState,
    val candidates: List<DynamicManagerCandidate>,
)

class DynamicManagerRepository {
    suspend fun load(): Result<DynamicManagerSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val runtime = getDynamicManagerStatus().getOrThrow()
            val installedPackages = installedPackages()
            val packagesByAppId = installedPackages
                .mapNotNull { packageInfo ->
                    packageInfo.applicationInfo?.let { applicationInfo ->
                        applicationInfo.uid % PER_USER_RANGE to packageInfo.packageName
                    }
                }
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            val candidates = installedPackages
                .asSequence()
                .mapNotNull { packageInfo -> toCandidate(packageInfo, packagesByAppId) }
                .sortedWith(
                    compareByDescending<DynamicManagerCandidate> {
                        runtime.packageName == it.packageName && runtime.appId == it.appId
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
                        .thenBy { it.packageName },
                )
                .toList()
            DynamicManagerSnapshot(runtime, candidates)
        }
    }

    suspend fun grant(candidate: DynamicManagerCandidate): Result<Unit> = withContext(Dispatchers.IO) {
        setDynamicManagerApk(
            apkPath = candidate.apkPath,
            packageName = candidate.packageName,
            appId = candidate.appId,
        )
    }

    suspend fun revoke(): Result<Unit> = withContext(Dispatchers.IO) {
        clearDynamicManager()
    }

    private fun installedPackages(): List<PackageInfo> {
        val packageManager = ksuApp.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(0)
        }
    }

    private fun toCandidate(
        packageInfo: PackageInfo,
        packagesByAppId: Map<Int, List<String>>,
    ): DynamicManagerCandidate? {
        val applicationInfo = packageInfo.applicationInfo ?: return null
        val packageName = packageInfo.packageName.orEmpty()
        if (packageName == ksuApp.packageName ||
            packageName == "io.github.fixz.apkesu" ||
            packageName == "io.github.fixz.apkesu.vivo"
        ) {
            return null
        }
        val appId = applicationInfo.uid % PER_USER_RANGE
        if (appId !in FIRST_APPLICATION_APP_ID..LAST_APPLICATION_APP_ID) return null
        if (packagesByAppId[appId].orEmpty().distinct().size != 1) return null
        val apkPath = applicationInfo.sourceDir.orEmpty()
        val apkPaths = buildList {
            add(apkPath)
            applicationInfo.splitSourceDirs.orEmpty()
                .filterTo(this) { it.isNotBlank() }
        }
        if (!apkPath.startsWith("/data/app/") || !containsKsud(applicationInfo.nativeLibraryDir, apkPaths)) {
            return null
        }
        val label = runCatching {
            applicationInfo.loadLabel(ksuApp.packageManager).toString()
        }.getOrDefault(packageName)
        return DynamicManagerCandidate(
            packageInfo = packageInfo,
            label = label,
            packageName = packageName,
            apkPath = apkPath,
            appId = appId,
        )
    }

    private fun containsKsud(nativeLibraryDir: String?, apkPaths: List<String>): Boolean {
        if (!nativeLibraryDir.isNullOrBlank() && File(nativeLibraryDir, "libksud.so").isFile) {
            return true
        }
        return apkPaths.any { apkPath ->
            runCatching {
                ZipFile(apkPath).use { archive ->
                    archive.entries().asSequence().any { entry ->
                        !entry.isDirectory &&
                            entry.name.startsWith("lib/") &&
                            entry.name.endsWith("/libksud.so")
                    }
                }
            }.getOrDefault(false)
        }
    }
}
