package me.weishu.kernelsu.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.KernelVersion
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.WarningLevel
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.material.TonalCard
import me.weishu.kernelsu.ui.component.material.expressiveTopAppBarColors
import me.weishu.kernelsu.ui.component.rebootlistpopup.RebootListPopup
import me.weishu.kernelsu.ui.component.statustag.StatusTag

@Composable
fun HomePagerMaterial(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = { TopBar(scrollBehavior = scrollBehavior) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            if (state.showManagerPrBuildWarning) {
                WarningCard(stringResource(id = R.string.home_pr_build_warning), level = WarningLevel.Notice)
            } else if (state.showKernelPrBuildWarning) {
                WarningCard(stringResource(id = R.string.home_pr_kernel_warning), level = WarningLevel.Notice)
            }
            if (state.showVersionMismatchWarning) {
                WarningCard(
                    stringResource(
                        id = R.string.home_version_mismatch,
                        state.currentManagerVersionCode,
                        state.ksuVersion ?: 0
                    )
                )
            }
            if (state.showGkiWarning) {
                WarningCard(stringResource(id = R.string.home_gki_warning), level = WarningLevel.Notice)
            }
            if (state.showUAPIMisMatchWarning) {
                WarningCard(
                    stringResource(
                        id = R.string.uapi_mismatch,
                        state.managerUAPIVersion,
                        state.kernelUAPIVersion ?: 0,
                    )
                )
            }
            if (state.showRequireKernelWarning) {
                if (state.currentManagerVersionCode < (state.ksuVersion ?: 0)) {
                    WarningCard(
                        stringResource(
                            id = R.string.require_manager_version,
                            state.currentManagerVersionCode,
                            state.ksuVersion ?: 0,
                        )
                    )
                } else {
                    WarningCard(
                        stringResource(
                            id = R.string.require_kernel_version,
                            state.ksuVersion ?: 0,
                            Natives.minimalSupportedKernel
                        )
                    )
                }
            }
            if (state.showRootWarning) {
                WarningCard(stringResource(id = R.string.grant_root_failed))
            }
            StatusCard(
                state = state,
                actions = actions,
            )
            if (state.isKernelActive) {
                MaterialMetricCards(state = state, actions = actions)
            }
            InfoCard(systemInfo = state.systemInfo)
            if (state.showHomeSupportCard) {
                DonateCard(onOpenUrl = actions.onOpenUrl)
            }
            if (state.showHomeLearnCard) {
                LearnMoreCard(onOpenUrl = actions.onOpenUrl)
            }
            Spacer(Modifier.height(bottomInnerPadding))
        }
    }
}

@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    LargeFlexibleTopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = { RebootListPopup() },
        colors = expressiveTopAppBarColors(),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun StatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        val ksuActive = state.ksuVersion != null
        val notInstalled = !ksuActive && state.kernelVersion.isGKI()
        val wallpaperState = rememberHomeMetricCardWallpaperState(
            target = HomeMetricCardWallpaperTarget.MaterialLkm,
            onWallpaperSelected = {},
        )
        val wallpaperBitmap = rememberHomeMetricCardWallpaperBitmap(
            uriString = wallpaperState.uriString,
            crop = wallpaperState.crop,
        )
        val hasWallpaper = wallpaperBitmap != null || !wallpaperState.videoUriString.isNullOrBlank()

        val containerColor = if (ksuActive) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
        val contentColor = if (hasWallpaper) {
            Color.White
        } else {
            MaterialTheme.colorScheme.contentColorFor(containerColor)
        }

        val statusIcon = when {
            ksuActive -> Icons.Outlined.CheckCircle
            notInstalled -> Icons.Outlined.Warning
            else -> Icons.Outlined.Block
        }
        val statusTitle = when {
            ksuActive -> stringResource(R.string.home_working)
            notInstalled -> stringResource(R.string.home_not_installed)
            else -> stringResource(R.string.home_unsupported)
        }
        val statusSummary = when {
            ksuActive -> stringResource(R.string.home_working_version, "${state.ksuVersion}-${state.kernelUAPIVersion}")
            notInstalled -> stringResource(R.string.home_click_to_install)
            else -> stringResource(R.string.home_unsupported_reason)
        }
        val workingMode = if (ksuActive) {
            when (state.lkmMode) {
                null -> ""
                true -> "LKM"
                else -> "GKI"
            }
        } else ""

        val statusTrailing: (@Composable () -> Unit)? = if (ksuActive && workingMode.isNotEmpty()) {
            {
                StatusTag(
                    label = workingMode,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    backgroundColor = MaterialTheme.colorScheme.primary
                )
            }
        } else if (notInstalled && state.isSELinuxPermissive) {
            {
                Button(
                    onClick = actions.onJailbreakClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.home_jailbreak))
                }
            }
        } else null

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = containerColor,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.large,
            onClick = {
                if (!state.isLateLoadMode) {
                    actions.onInstallClick()
                }
            }
        ) {
            Box {
                HomeMetricCardWallpaperBackground(
                    bitmap = wallpaperBitmap,
                    videoUriString = wallpaperState.videoUriString,
                    videoCrop = wallpaperState.crop,
                    visualSettings = wallpaperState.visualSettings,
                )
                ListItem(
                    modifier = Modifier,
                    leadingContent = {
                        Icon(statusIcon, contentDescription = statusTitle)
                    },
                    trailingContent = statusTrailing,
                    overlineContent = null,
                    supportingContent = {
                        Text(
                            text = statusSummary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = contentColor,
                        leadingContentColor = contentColor,
                        trailingContentColor = contentColor,
                        supportingContentColor = contentColor.copy(alpha = 0.78f)
                    ),
                    elevation = ListItemDefaults.elevation(),
                    content = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleMediumEmphasized
                            )
                            if (ksuActive && state.isSafeMode) {
                                Spacer(Modifier.width(8.dp))
                                StatusTag(
                                    label = stringResource(id = R.string.safe_mode),
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    backgroundColor = MaterialTheme.colorScheme.errorContainer
                                )
                            }
                            if (ksuActive && state.isLateLoadMode) {
                                Spacer(Modifier.width(8.dp))
                                StatusTag(
                                    label = stringResource(id = R.string.jailbreak_mode),
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    backgroundColor = MaterialTheme.colorScheme.errorContainer
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WarningCard(
    message: String,
    level: WarningLevel = WarningLevel.Error,
    onClick: (() -> Unit)? = null
) {
    val containerColor = when (level) {
        WarningLevel.Error -> MaterialTheme.colorScheme.errorContainer
        WarningLevel.Notice -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.contentColorFor(containerColor)
            )
        }
    }
    if (onClick != null) {
        TonalCard(containerColor = containerColor, onClick = onClick, content = content)
    } else {
        TonalCard(containerColor = containerColor, content = content)
    }
}

@Composable
private fun MaterialMetricCards(
    state: HomeUiState,
    actions: HomeActions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        MaterialMetricCard(
            target = HomeMetricCardWallpaperTarget.Superuser,
            title = stringResource(R.string.superuser),
            value = state.superuserCount.toString(),
            onClick = actions.onSuperuserClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        MaterialMetricCard(
            target = HomeMetricCardWallpaperTarget.Module,
            title = stringResource(R.string.module),
            value = state.moduleCount.toString(),
            onClick = actions.onModuleClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun MaterialMetricCard(
    target: HomeMetricCardWallpaperTarget,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WallpaperTonalCard(
        target = target,
        modifier = modifier,
        onClick = onClick,
    ) { primaryColor, secondaryColor ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = secondaryColor,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = primaryColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LearnMoreCard(onOpenUrl: (String) -> Unit) {
    val url = stringResource(R.string.home_learn_kernelsu_url)
    TonalCard(onClick = { onOpenUrl(url) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = stringResource(R.string.home_learn_kernelsu), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_click_to_learn_kernelsu),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DonateCard(onOpenUrl: (String) -> Unit) {
    TonalCard(onClick = { onOpenUrl("https://patreon.com/weishu") }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = stringResource(R.string.home_support_title), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_support_content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoCard(systemInfo: SystemInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        StatusMonitorCard(systemInfo)
        SystemInfoCard(systemInfo)
    }
}

@Composable
private fun StatusMonitorCard(systemInfo: SystemInfo) {
    WallpaperTonalCard(
        target = HomeMetricCardWallpaperTarget.StatusMonitor,
    ) { primaryColor, secondaryColor ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
        ) {
            @Composable
            fun StatusItem(label: String, content: String) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = primaryColor)
                Text(text = content, style = MaterialTheme.typography.bodyMedium, color = secondaryColor)
            }

            val selinuxDisplay = when (systemInfo.selinuxStatus) {
                "Enforcing" -> stringResource(R.string.selinux_status_enforcing)
                "Permissive" -> stringResource(R.string.selinux_status_permissive)
                "Disabled" -> stringResource(R.string.selinux_status_disabled)
                else -> stringResource(R.string.selinux_status_unknown)
            }
            StatusItem(stringResource(R.string.home_selinux_status), selinuxDisplay)
            Spacer(Modifier.height(16.dp))
            val seccompDisplay = when (systemInfo.seccompStatus) {
                -1 -> stringResource(R.string.seccomp_status_not_supported)
                0 -> stringResource(R.string.seccomp_status_disabled)
                1 -> stringResource(R.string.seccomp_status_strict)
                2 -> stringResource(R.string.seccomp_status_filter)
                else -> stringResource(R.string.seccomp_status_unknown)
            }
            StatusItem(stringResource(R.string.home_seccomp_status), seccompDisplay)
        }
    }
}

@Composable
private fun SystemInfoCard(systemInfo: SystemInfo) {
    WallpaperTonalCard(
        target = HomeMetricCardWallpaperTarget.SystemInfo,
    ) { primaryColor, secondaryColor ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)
        ) {
            @Composable
            fun InfoCardItem(label: String, content: String) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = primaryColor)
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                )
            }

            InfoCardItem(stringResource(R.string.home_manager_version), systemInfo.managerVersion)
            Spacer(Modifier.height(16.dp))
            InfoCardItem(stringResource(R.string.home_kernel), systemInfo.kernelVersion)
            Spacer(Modifier.height(16.dp))
            InfoCardItem(stringResource(R.string.home_device_model), systemInfo.deviceModel)
            Spacer(Modifier.height(16.dp))
            InfoCardItem(stringResource(R.string.home_fingerprint), systemInfo.fingerprint)
        }
    }
}

@Composable
private fun WallpaperTonalCard(
    target: HomeMetricCardWallpaperTarget,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable (primaryColor: Color, secondaryColor: Color) -> Unit,
) {
    val wallpaperState = rememberHomeMetricCardWallpaperState(
        target = target,
        onWallpaperSelected = {},
    )
    val wallpaperBitmap = rememberHomeMetricCardWallpaperBitmap(
        uriString = wallpaperState.uriString,
        crop = wallpaperState.crop,
    )
    val hasWallpaper = wallpaperBitmap != null || !wallpaperState.videoUriString.isNullOrBlank()
    val primaryColor = if (hasWallpaper) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryColor = if (hasWallpaper) {
        Color.White.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val cardContent: @Composable () -> Unit = {
        Box {
            HomeMetricCardWallpaperBackground(
                bitmap = wallpaperBitmap,
                videoUriString = wallpaperState.videoUriString,
                videoCrop = wallpaperState.crop,
                visualSettings = wallpaperState.visualSettings,
            )
            content(primaryColor, secondaryColor)
        }
    }
    if (onClick != null) {
        TonalCard(modifier = modifier, onClick = onClick, content = cardContent)
    } else {
        TonalCard(modifier = modifier, content = cardContent)
    }
}

@Preview(name = "Activated")
@Composable
private fun StatusCardActivatedPreview() {
    StatusCard(
        state = previewHomeScreenState(ksuVersion = 12345, lkmMode = true),
        actions = previewHomeActions
    )
}

@Preview(name = "Not Activated")
@Composable
private fun StatusCardNotActivatedPreview() {
    StatusCard(state = previewHomeScreenState(ksuVersion = null, lkmMode = null), actions = previewHomeActions)
}

@Preview(name = "Permissive")
@Composable
private fun StatusCardPermissivePreview() {
    StatusCard(
        state = previewHomeScreenState(ksuVersion = null, lkmMode = null, selinuxStatus = "Permissive"),
        actions = previewHomeActions
    )
}

@Preview(name = "Jailbreak")
@Composable
private fun StatusCardJailbreakPreview() {
    StatusCard(
        state = previewHomeScreenState(ksuVersion = 12345, lkmMode = true, isLateLoadMode = true),
        actions = previewHomeActions
    )
}

private val previewSystemInfo = SystemInfo(
    kernelVersion = "6.1.0-android14-0-g123456789000-ab12345678",
    managerVersion = "3.0.0 (30000)",
    deviceModel = "Google Pixel 6 Pro",
    fingerprint = "google/raven/raven:14/AP1A.240305.019:user/release-keys",
    selinuxStatus = "Enforcing",
    seccompStatus = 2
)

private val previewUriHandler = object : UriHandler {
    override fun openUri(uri: String) {}
}

private val previewHomeActions = HomeActions(
    onInstallClick = {},
    onSuperuserClick = {},
    onModuleClick = {},
    onOpenUrl = {},
)

@Composable
private fun HomeScreenPreviewContent(
    ksuVersion: Int?,
    lkmMode: Boolean?,
    isSafeMode: Boolean = false,
    isLateLoadMode: Boolean = false,
    selinuxStatus: String = "Enforcing",
) {
    CompositionLocalProvider(LocalUriHandler provides previewUriHandler) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val actions = previewHomeActions
            StatusCard(
                state = previewHomeScreenState(
                    ksuVersion = ksuVersion,
                    lkmMode = lkmMode,
                    isSafeMode = isSafeMode,
                    isLateLoadMode = isLateLoadMode,
                    selinuxStatus = selinuxStatus,
                ),
                actions = actions
            )
            InfoCard(previewSystemInfo.copy(selinuxStatus = selinuxStatus))
            DonateCard(onOpenUrl = {})
            LearnMoreCard(onOpenUrl = {})
        }
    }
}

@Preview(name = "Home Activated", showBackground = true)
@Composable
private fun HomeScreenActivatedPreview() {
    HomeScreenPreviewContent(ksuVersion = 12345, lkmMode = true)
}

@Preview(name = "Home Not Activated", showBackground = true)
@Composable
private fun HomeScreenNotActivatedPreview() {
    HomeScreenPreviewContent(ksuVersion = null, lkmMode = null)
}

@Preview(name = "Home Permissive", showBackground = true)
@Composable
private fun HomeScreenPermissivePreview() {
    HomeScreenPreviewContent(ksuVersion = null, lkmMode = null, selinuxStatus = "Permissive")
}

@Preview(name = "Home Jailbreak", showBackground = true)
@Composable
private fun HomeScreenJailbreakPreview() {
    HomeScreenPreviewContent(ksuVersion = 12345, lkmMode = true, isLateLoadMode = true)
}

private fun previewHomeScreenState(
    ksuVersion: Int?,
    lkmMode: Boolean?,
    isSafeMode: Boolean = false,
    isLateLoadMode: Boolean = false,
    selinuxStatus: String = "Enforcing",
) = HomeUiState(
    kernelVersion = KernelVersion(6, 1, 0),
    ksuVersion = ksuVersion,
    lkmMode = lkmMode,
    isManager = true,
    isManagerPrBuild = false,
    isKernelPrBuild = false,
    requiresNewKernel = false,
    isRootAvailable = ksuVersion != null,
    rootRuntimeState = if (ksuVersion != null) RootRuntimeState.Running else RootRuntimeState.DriverDisconnected,
    isSafeMode = isSafeMode,
    isLateLoadMode = isLateLoadMode,
    currentManagerVersionCode = 10000,
    showVersionMismatchWarningSetting = true,
    superuserCount = 0,
    moduleCount = 0,
    systemInfo = previewSystemInfo.copy(selinuxStatus = selinuxStatus),
    kernelUAPIVersion = 1,
    managerUAPIVersion = 1,
    uapiMismatch = false
)
