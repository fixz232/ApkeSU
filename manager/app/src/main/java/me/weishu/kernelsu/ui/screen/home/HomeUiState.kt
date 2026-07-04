package me.weishu.kernelsu.ui.screen.home

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.KernelVersion

private const val LKM_MODE_LABEL = "LKM"
private const val HIDDEN_PATH_LKM_MODE_LABEL = "\u9690\u85cf\u8def\u5f84LKM"
private const val GKI_MODE_LABEL = "GKI"

@Immutable
data class HomeUiState(
    val kernelVersion: KernelVersion,
    val ksuVersion: Int?,
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
        get() = isManager && !requiresNewKernel && isRootAvailable

    val showGkiWarning: Boolean
        get() = showGkiWarningSetting && ksuVersion != null && lkmMode == false

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
        get() = ksuVersion != null && !isRootAvailable

    val showManagerWarning: Boolean
        get() = ksuVersion != null && !isManager

    val showManagerPrBuildWarning: Boolean
        get() = isManager && isManagerPrBuild

    val showKernelPrBuildWarning: Boolean
        get() = isManager && !isManagerPrBuild && isKernelPrBuild

    val showVersionMismatchWarning: Boolean
        get() = showVersionMismatchWarningSetting &&
                ksuVersion != null &&
                currentManagerVersionCode < ksuVersion.toLong()
}

@Immutable
data class HomeActions(
    val onInstallClick: () -> Unit,
    val onSuperuserClick: () -> Unit,
    val onModuleClick: () -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onJailbreakClick: () -> Unit = {},
)
