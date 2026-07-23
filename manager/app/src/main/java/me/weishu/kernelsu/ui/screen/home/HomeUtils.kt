package me.weishu.kernelsu.ui.screen.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.PackageInfoCompat
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R

@Immutable
data class ManagerVersion(
    val versionName: String,
    val versionCode: Long
)

@Immutable
data class SystemInfo(
    val kernelVersion: String,
    val managerVersion: String,
    val deviceModel: String,
    val fingerprint: String,
    val selinuxStatus: String,
    val seccompStatus: Int
)

fun getManagerVersion(context: Context): ManagerVersion {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)!!
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return ManagerVersion(
        versionName = packageInfo.versionName!!,
        versionCode = versionCode
    )
}

@Composable
fun homeWarningMessages(state: HomeUiState): List<String> = buildList {
    if (state.showManagerPrBuildWarning) {
        add(stringResource(id = R.string.home_pr_build_warning))
    } else if (state.showKernelPrBuildWarning) {
        add(stringResource(id = R.string.home_pr_kernel_warning))
    }
    if (state.showVersionMismatchWarning && !state.showManagerWarning) {
        add(
            stringResource(
                id = R.string.home_version_mismatch,
                state.currentManagerVersionCode,
                state.ksuVersion ?: 0
            )
        )
    }
    if (state.showGkiWarning) {
        add(stringResource(id = R.string.home_gki_warning))
    }
    if (state.showManagerWarning) {
        val driverVersion = state.ksuVersion
        if (driverVersion != null && driverVersion.toLong() != state.currentManagerVersionCode) {
            add(
                stringResource(
                    id = R.string.home_manager_build_mismatch_warning,
                    driverVersion,
                    state.currentManagerVersionCode,
                )
            )
        } else {
            add(stringResource(id = R.string.home_manager_identity_warning))
        }
    }
    if (state.showUAPIMisMatchWarning) {
        add(
            stringResource(
                id = R.string.uapi_mismatch,
                state.managerUAPIVersion,
                state.kernelUAPIVersion ?: 0,
            )
        )
    }
    if (state.showRequireKernelWarning) {
        val message = if (state.currentManagerVersionCode < (state.ksuVersion ?: 0)) {
            stringResource(
                id = R.string.require_manager_version,
                state.currentManagerVersionCode,
                state.ksuVersion ?: 0,
            )
        } else {
            stringResource(
                id = R.string.require_kernel_version,
                state.ksuVersion ?: 0,
                Natives.minimalSupportedKernel
            )
        }
        add(message)
    }
    if (state.showDaemonVersionWarning) {
        add(stringResource(R.string.root_runtime_version_warning))
    }
    if (state.showRootWarning) {
        add(stringResource(id = R.string.root_daemon_warning))
    }
}
