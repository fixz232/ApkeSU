package me.weishu.kernelsu.ui.screen.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.KernelVersion
import me.weishu.kernelsu.R

private const val LKM_MODE_LABEL = "LKM"
private const val HIDDEN_PATH_LKM_MODE_LABEL = "\u9690\u85cf\u8def\u5f84LKM"
private const val GKI_MODE_LABEL = "GKI"

enum class RootRuntimeState(@StringRes val labelRes: Int) {
    DriverDisconnected(R.string.root_state_driver_disconnected),
    ManagerUnregistered(R.string.root_state_manager_unregistered),
    DaemonError(R.string.root_state_daemon_error),
    VersionMismatch(R.string.root_state_version_mismatch),
    Running(R.string.root_state_running);

    companion object {
        fun resolve(
            driverConnected: Boolean,
            managerRegistered: Boolean,
            daemonRootAvailable: Boolean,
            versionMismatch: Boolean,
        ): RootRuntimeState = when {
            !driverConnected -> DriverDisconnected
            !managerRegistered -> ManagerUnregistered
            !daemonRootAvailable -> DaemonError
            versionMismatch -> VersionMismatch
            else -> Running
        }
    }
}

enum class KernelHookType(@StringRes val labelRes: Int) {
    Tracepoint(R.string.hook_type_tracepoint);

    companion object {
        fun resolve(
            hasActiveDriver: Boolean,
            hasTracepoint: Boolean? = null,
        ): List<KernelHookType> = when {
            !hasActiveDriver -> emptyList()
            hasTracepoint == false -> emptyList()
            else -> listOf(Tracepoint)
        }
    }
}

@Composable
fun kernelHookTypeLabel(types: List<KernelHookType>): String {
    if (types.isEmpty()) return "--"

    val labels = ArrayList<String>(types.size)
    for (type in types) {
        labels += stringResource(type.labelRes)
    }
    return labels.joinToString(separator = " / ")
}

@Immutable
data class HomeUiState(
    val kernelVersion: KernelVersion,
    val ksuVersion: Int?,
    val isKernelActive: Boolean = ksuVersion != null,
    val managerUAPIVersion: Int,
    val kernelUAPIVersion: Int?,
    val lkmMode: Boolean?,
    val hiddenPathLkmMode: Boolean = false,
    val isManager: Boolean,
    val isManagerPrBuild: Boolean,
    val isKernelPrBuild: Boolean,
    val requiresNewKernel: Boolean,
    val uapiMismatch: Boolean,
    val isRootAvailable: Boolean,
    val rootRuntimeState: RootRuntimeState = RootRuntimeState.DriverDisconnected,
    val kernelHookTypes: List<KernelHookType> = emptyList(),
    val isSafeMode: Boolean,
    val isLateLoadMode: Boolean,
    val currentManagerVersionCode: Long,
    val showVersionMismatchWarningSetting: Boolean,
    val showGkiWarningSetting: Boolean = true,
    val showHomeSupportCard: Boolean = true,
    val showHomeLearnCard: Boolean = true,
    val superuserCount: Int,
    val moduleCount: Int,
    val systemInfo: SystemInfo,
) {
    val isSELinuxPermissive: Boolean
        get() = systemInfo.selinuxStatus == "Permissive"

    val isFullFeatured: Boolean
        get() = rootRuntimeState == RootRuntimeState.Running

    val showGkiWarning: Boolean
        get() = showGkiWarningSetting && isKernelActive && lkmMode == false

    val lkmModeLabel: String
        get() = if (hiddenPathLkmMode) HIDDEN_PATH_LKM_MODE_LABEL else LKM_MODE_LABEL

    val workingModeLabel: String?
        get() = when (lkmMode) {
            true -> lkmModeLabel
            false -> GKI_MODE_LABEL
            null -> null
        }

    val showRequireKernelWarning: Boolean
        get() = isManager && requiresNewKernel

    val showUAPIMisMatchWarning: Boolean
        get() = isManager && showRequireKernelWarning && uapiMismatch

    val showRootWarning: Boolean
        get() = rootRuntimeState == RootRuntimeState.DaemonError

    val showManagerWarning: Boolean
        get() = rootRuntimeState == RootRuntimeState.ManagerUnregistered

    val showManagerPrBuildWarning: Boolean
        get() = isManager && isManagerPrBuild

    val showKernelPrBuildWarning: Boolean
        get() = isManager && !isManagerPrBuild && isKernelPrBuild

    val showVersionMismatchWarning: Boolean
        get() = showVersionMismatchWarningSetting &&
                ksuVersion != null &&
                currentManagerVersionCode < ksuVersion.toLong()

    val ksuVersionLabel: String
        get() = ksuVersion?.let { version ->
            kernelUAPIVersion?.let { "$version-$it" } ?: version.toString()
        } ?: "--"
}

@Immutable
data class HomeActions(
    val onInstallClick: () -> Unit,
    val onSuperuserClick: () -> Unit,
    val onModuleClick: () -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onJailbreakClick: () -> Unit = {},
    val onStyleSettingsClick: () -> Unit = {},
    val onDiagnoseClick: () -> Unit = {},
)
