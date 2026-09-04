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
import me.weishu.kernelsu.ui.util.setDynamicManagerCertificate
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
    val managerSignatureIndex: Int? = null,
) {
    val isSelected: Boolean get() = managerSignatureIndex == DYNAMIC_MANAGER_SIGNATURE_INDEX
    val isChangeable: Boolean get() = managerSignatureIndex == null || isSelected
}

private const val DYNAMIC_MANAGER_SIGNATURE_INDEX = 255

data class DynamicManagerSnapshot(
    val runtime: DynamicManagerCliState,
    val candidates: List<DynamicManagerCandidate>,
)

class DynamicManagerRepository {
    suspend fun load(): Result<DynamicManagerSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val runtime = getDynamicManagerStatus().getOrThrow()
            val installedPackages = installedPackages()
            val candidates = installedPackages
                .asSequence()
                .mapNotNull { packageInfo -> toCandidate(packageInfo, runtime.managerSignatureIndexes) }
                .sortedWith(
                    compareByDescending<DynamicManagerCandidate> { it.isSelected }
                        .thenByDescending { it.managerSignatureIndex != null }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
                        .thenBy { it.packageName },
                )
                .toList()
            DynamicManagerSnapshot(runtime, candidates)
        }
    }

    suspend fun grant(candidate: DynamicManagerCandidate): Result<Unit> = withContext(Dispatchers.IO) {
        setDynamicManagerApk(apkPath = candidate.apkPath)
    }

    suspend fun setManual(certificateSize: Int, certificateSha256: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            setDynamicManagerCertificate(certificateSize, certificateSha256)
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
        managerSignatureIndexes: Map<Int, Int>,
    ): DynamicManagerCandidate? {
        val applicationInfo = packageInfo.applicationInfo ?: return null
        val packageName = packageInfo.packageName.orEmpty()
        if (packageName == ksuApp.packageName ||
            packageName == "io.github.fixz.apkesu"
        ) {
            return null
        }
        val appId = applicationInfo.uid % PER_USER_RANGE
        if (appId !in FIRST_APPLICATION_APP_ID..LAST_APPLICATION_APP_ID) return null
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
            managerSignatureIndex = managerSignatureIndexes[appId],
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
