package me.weishu.kernelsu.ui.screen.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Fence
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.ApkeSecondaryScaffold
import me.weishu.kernelsu.ui.component.ApkeUiTokens
import me.weishu.kernelsu.ui.component.ink.InkStyle
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.rain.RainStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.ink.inkMiuixCardSurface
import me.weishu.kernelsu.ui.component.ink.isInkInterfaceStyle
import me.weishu.kernelsu.ui.component.dialog.rememberLoadingDialog
import me.weishu.kernelsu.ui.component.miuix.SendLogDialog
import me.weishu.kernelsu.ui.component.uninstalldialog.UninstallDialog
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.theme.LocalImmersiveBackgroundActive
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.util.KPATCH_NEXT_MODULE_ID
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import me.weishu.kernelsu.ui.webui.WebUIActivity

@Composable
fun SettingsCategoryScreen(routeValue: String) {
    val category = SettingsCategory.fromRouteValue(routeValue)
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val onBack = dropUnlessResumed { navigator.pop() }
    var showHomeTitleDialog by rememberSaveable { mutableStateOf(false) }
    var showSendLogDialog by rememberSaveable { mutableStateOf(false) }
    var showUninstallDialog by rememberSaveable { mutableStateOf(false) }
    val loadingDialog = rememberLoadingDialog()
    LifecycleResumeEffect(category.routeValue) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    SettingsCategoryScaffold(
        category = category,
        onBack = onBack,
    ) {
        when (category) {
            SettingsCategory.Appearance -> AppearanceSettingsContent(
                uiState = uiState,
                onSetUiMode = { selectedIndex ->
                    val selected = InterfaceStyle.fromIndex(selectedIndex)
                    if (selected != InterfaceStyle.Alpha || uiState.uiMode != InterfaceStyle.Delta.value) {
                        viewModel.setUiMode(selected.value)
                    }
                },
                onSetAlphaDeltaMode = { useDelta ->
                    viewModel.setUiMode(if (useDelta) InterfaceStyle.Delta.value else InterfaceStyle.Alpha.value)
                },
                onSetMiuixClassicHomeLayout = viewModel::setMiuixClassicHomeLayoutEnabled,
                onSetSeasonStyle = viewModel::setSeasonStyleIndex,
                onSetSeasonCardMotion = viewModel::setSeasonCardMotionEnabled,
                onSetRainStyle = viewModel::setRainStyleIndex,
                onSetRainCardMotion = viewModel::setRainCardMotionEnabled,
                onSetDayNightMode = viewModel::setDayNightMode,
                onSetInkStyle = viewModel::setInkStyleIndex,
                onSetInkFontEnabled = viewModel::setInkFontEnabled,
                onSetInkCardMotion = viewModel::setInkCardMotionEnabled,
                onSetPixelStyle = viewModel::setPixelStyleIndex,
                onSetPixelCardMotion = viewModel::setPixelCardMotionEnabled,
                onOpen = navigator::push,
            )
            SettingsCategory.HomeAndManager -> HomeManagerSettingsContent(
                uiState = uiState,
                onEditHomeTitle = { showHomeTitleDialog = true },
                onSetShowSupportCard = viewModel::setShowHomeSupportCard,
                onSetShowLearnCard = viewModel::setShowHomeLearnCard,
                onOpen = navigator::push,
            )
            SettingsCategory.RootAndPermissions -> RootPermissionSettingsContent(
                uiState = uiState,
                onSetSuCompatMode = viewModel::setSuCompatMode,
                onSetKernelUmount = viewModel::setKernelUmountEnabled,
                onSetWebViewZygoteUmount = viewModel::setWebViewZygoteUmountEnabled,
                onSetSelinuxHide = viewModel::setSelinuxHideEnabled,
                onSetSulog = viewModel::setSulogEnabled,
                onSetAdbRoot = viewModel::setAdbRootEnabled,
                onSetAvcSpoof = viewModel::setAvcSpoofEnabled,
                onSetUseSoftReboot = viewModel::setUseSoftReboot,
                onOpen = navigator::push,
            )
            SettingsCategory.MountAndHide -> MountHideSettingsContent(
                uiState = uiState,
                onSetDefaultUmountModules = viewModel::setDefaultUmountModules,
                onSetKPatchNext = { enabled ->
                    if (!uiState.isLateLoadMode) viewModel.setKPatchNextEnabled(enabled)
                },
                onOpenKPatchNextWebUi = {
                    if (uiState.canOpenKPatchNextWebUi) {
                        context.startActivity(
                            Intent(context, WebUIActivity::class.java)
                                .setData("kernelsu://webui/$KPATCH_NEXT_MODULE_ID".toUri())
                                .putExtra("id", KPATCH_NEXT_MODULE_ID)
                        )
                    }
                },
                onOpenPathConfig = {
                    when (uiState.pathConfigBackend) {
                        PathConfigBackend.PathmaskLkm -> navigator.push(Route.HiddenPathConfig)
                        PathConfigBackend.SusfsGki -> navigator.push(Route.SusfsPathConfig)
                        PathConfigBackend.Disabled,
                        PathConfigBackend.Unknown,
                        -> Unit
                    }
                },
                onSetEpkesuHide = viewModel::setEpkesuHideEnabled,
                onOpen = navigator::push,
            )
            SettingsCategory.Toolbox -> ToolboxSettingsContent(
                uiState = uiState,
                onOpen = navigator::push,
                onSetGraphicsRendererEnabled = viewModel::setGraphicsRendererFeatureEnabled,
            )
            SettingsCategory.AppAndMaintenance -> AppMaintenanceSettingsContent(
                uiState = uiState,
                onSetCheckModuleUpdate = viewModel::setCheckModuleUpdate,
                onSetVersionMismatchWarning = viewModel::setShowVersionMismatchWarning,
                onSetGkiWarning = viewModel::setShowGkiWarning,
                onSetWebDebugging = viewModel::setEnableWebDebugging,
                onSetAutoJailbreak = viewModel::setAutoJailbreak,
                onSendLog = { showSendLogDialog = true },
                onUninstall = { showUninstallDialog = true },
                onOpen = navigator::push,
            )
        }
    }

    HomeTitleDialog(
        show = showHomeTitleDialog,
        initialTitle = uiState.customHomeTitle,
        onDismissRequest = { showHomeTitleDialog = false },
        onConfirm = viewModel::setCustomHomeTitle,
    )
    SendLogDialog(
        show = showSendLogDialog,
        onDismissRequest = { showSendLogDialog = false },
        loadingDialog = loadingDialog,
    )
    UninstallDialog(
        show = showUninstallDialog,
        onDismissRequest = { showUninstallDialog = false },
    )
}

@Composable
private fun SettingsCategoryScaffold(
    category: SettingsCategory,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    ApkeSecondaryScaffold(
        title = stringResource(category.titleRes),
        onBack = onBack,
        maxContentWidth = 680.dp,
    ) { innerPadding, _ ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = ApkeUiTokens.PageHorizontalPadding,
                        top = innerPadding.calculateTopPadding() + 4.dp,
                        end = ApkeUiTokens.PageHorizontalPadding,
                        bottom = bottomPadding + 18.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun AppearanceSettingsContent(
    uiState: SettingsUiState,
    onSetUiMode: (Int) -> Unit,
    onSetAlphaDeltaMode: (Boolean) -> Unit,
    onSetMiuixClassicHomeLayout: (Boolean) -> Unit,
    onSetSeasonStyle: (Int) -> Unit,
    onSetSeasonCardMotion: (Boolean) -> Unit,
    onSetRainStyle: (Int) -> Unit,
    onSetRainCardMotion: (Boolean) -> Unit,
    onSetDayNightMode: (Boolean) -> Unit,
    onSetInkStyle: (Int) -> Unit,
    onSetInkFontEnabled: (Boolean) -> Unit,
    onSetInkCardMotion: (Boolean) -> Unit,
    onSetPixelStyle: (Int) -> Unit,
    onSetPixelCardMotion: (Boolean) -> Unit,
    onOpen: (Route) -> Unit,
) {
    val styles = InterfaceStyle.selectableEntries
    SettingsGroup(stringResource(R.string.settings_group_interface)) {
        SettingsChoiceRow(
            title = stringResource(R.string.settings_ui_mode),
            summary = stringResource(R.string.settings_ui_mode_summary),
            icon = Icons.Rounded.Dashboard,
            options = styles.map { stringResource(it.labelRes) },
            selectedIndex = InterfaceStyle.selectedIndex(uiState.uiMode),
            onSelected = onSetUiMode,
        )
        if (uiState.uiMode == InterfaceStyle.Miuix.value) {
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_miuix_classic_home_layout),
                summary = stringResource(R.string.settings_miuix_classic_home_layout_summary),
                icon = Icons.Rounded.Dashboard,
                checked = uiState.miuixClassicHomeLayoutEnabled,
                onCheckedChange = onSetMiuixClassicHomeLayout,
            )
        }
        if (uiState.uiMode == InterfaceStyle.Alpha.value || uiState.uiMode == InterfaceStyle.Delta.value) {
            SettingsDivider()
            SettingsChoiceRow(
                title = stringResource(R.string.settings_alpha_delta_mode),
                summary = stringResource(R.string.settings_alpha_delta_mode_summary),
                icon = Icons.Rounded.Tune,
                options = listOf(
                    stringResource(R.string.settings_alpha_mode),
                    stringResource(R.string.settings_delta_mode),
                ),
                selectedIndex = if (uiState.uiMode == InterfaceStyle.Delta.value) 1 else 0,
                onSelected = { onSetAlphaDeltaMode(it == 1) },
            )
        }
        if (uiState.uiMode == InterfaceStyle.Ink.value) {
            SettingsDivider()
            SettingsChoiceRow(
                title = stringResource(R.string.settings_ink_style),
                summary = stringResource(InkStyle.fromValue(uiState.inkStyle).summaryRes),
                icon = Icons.Rounded.Brush,
                options = InkStyle.entries.map { stringResource(it.labelRes) },
                selectedIndex = InkStyle.selectedIndex(uiState.inkStyle),
                onSelected = onSetInkStyle,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_ink_font),
                summary = stringResource(R.string.settings_ink_font_summary),
                icon = Icons.Rounded.TextFields,
                checked = uiState.inkFontEnabled,
                onCheckedChange = onSetInkFontEnabled,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_ink_card_motion),
                summary = stringResource(R.string.settings_ink_card_motion_summary),
                icon = Icons.Rounded.AutoFixHigh,
                checked = uiState.inkCardMotionEnabled,
                onCheckedChange = onSetInkCardMotion,
            )
        }
        if (uiState.uiMode == InterfaceStyle.Snow.value) {
            SettingsDivider()
            val selected = SeasonStyle.fromValue(uiState.seasonStyle)
            SettingsChoiceRow(
                title = stringResource(R.string.settings_season_style),
                summary = stringResource(selected.summaryRes),
                icon = Icons.Rounded.Palette,
                options = SeasonStyle.entries.map { stringResource(it.labelRes) },
                selectedIndex = SeasonStyle.selectedIndex(uiState.seasonStyle),
                onSelected = onSetSeasonStyle,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_season_card_motion),
                summary = stringResource(R.string.settings_season_card_motion_summary),
                icon = Icons.Rounded.AutoFixHigh,
                checked = uiState.seasonCardMotionEnabled,
                onCheckedChange = onSetSeasonCardMotion,
            )
        }
        if (uiState.uiMode == InterfaceStyle.Rain.value) {
            SettingsDivider()
            val selected = RainStyle.fromValue(uiState.rainStyle)
            SettingsChoiceRow(
                title = stringResource(R.string.settings_rain_style),
                summary = stringResource(selected.summaryRes),
                icon = Icons.Rounded.Palette,
                options = RainStyle.entries.map { stringResource(it.labelRes) },
                selectedIndex = RainStyle.selectedIndex(uiState.rainStyle),
                onSelected = onSetRainStyle,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_rain_card_motion),
                summary = stringResource(R.string.settings_rain_card_motion_summary),
                icon = Icons.Rounded.AutoFixHigh,
                checked = uiState.rainCardMotionEnabled,
                onCheckedChange = onSetRainCardMotion,
            )
        }
        if (uiState.uiMode == InterfaceStyle.Pixel.value) {
            SettingsDivider()
            val selected = PixelStyle.fromValue(uiState.pixelStyle)
            SettingsChoiceRow(
                title = stringResource(R.string.settings_pixel_style),
                summary = stringResource(selected.summaryRes),
                icon = Icons.Rounded.Palette,
                options = PixelStyle.entries.map { stringResource(it.labelRes) },
                selectedIndex = PixelStyle.selectedIndex(uiState.pixelStyle),
                onSelected = onSetPixelStyle,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_pixel_card_motion),
                summary = stringResource(R.string.settings_pixel_card_motion_summary),
                icon = Icons.Rounded.AutoFixHigh,
                checked = uiState.pixelCardMotionEnabled,
                onCheckedChange = onSetPixelCardMotion,
            )
        }
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_day_night_switch),
            summary = stringResource(R.string.settings_day_night_switch_summary),
            icon = Icons.Rounded.Palette,
            checked = isDayNightSwitchChecked(uiState.themeMode),
            onCheckedChange = onSetDayNightMode,
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_theme_resources)) {
        SettingsActionRow(
            title = stringResource(R.string.theme_store),
            summary = stringResource(R.string.theme_store_settings_summary),
            icon = Icons.Rounded.Storefront,
            onClick = { onOpen(Route.ThemeStore) },
        )
    }
}

@Composable
private fun HomeManagerSettingsContent(
    uiState: SettingsUiState,
    onEditHomeTitle: () -> Unit,
    onSetShowSupportCard: (Boolean) -> Unit,
    onSetShowLearnCard: (Boolean) -> Unit,
    onOpen: (Route) -> Unit,
) {
    SettingsGroup(stringResource(R.string.settings_group_manager)) {
        SettingsActionRow(
            title = stringResource(R.string.settings_manager_identity),
            summary = stringResource(R.string.settings_manager_identity_summary),
            icon = Icons.Rounded.Apps,
            onClick = { onOpen(Route.LauncherIcon) },
        )
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.settings_home_title),
            summary = if (uiState.customHomeTitle.isBlank()) {
                stringResource(R.string.settings_home_title_default_summary)
            } else {
                stringResource(R.string.settings_home_title_custom_summary, uiState.customHomeTitle)
            },
            icon = Icons.Rounded.EditNote,
            onClick = onEditHomeTitle,
        )
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.pixel_pet_title),
            summary = stringResource(
                if (uiState.pixelPetEnabled) {
                    R.string.pixel_pet_enabled_summary
                } else {
                    R.string.pixel_pet_disabled_summary
                }
            ),
            icon = Icons.Rounded.AutoFixHigh,
            onClick = { onOpen(Route.PixelPet) },
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_home_layout)) {
        SettingsActionRow(
            title = stringResource(R.string.home_layout_title),
            summary = stringResource(R.string.home_layout_settings_summary),
            icon = Icons.Rounded.Dashboard,
            onClick = { onOpen(Route.HomeLayout) },
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_home_cards)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_show_home_support_card),
            summary = stringResource(R.string.settings_show_home_support_card_summary),
            icon = Icons.Rounded.Visibility,
            checked = uiState.showHomeSupportCard,
            onCheckedChange = onSetShowSupportCard,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_show_home_learn_card),
            summary = stringResource(R.string.settings_show_home_learn_card_summary),
            icon = Icons.Rounded.Visibility,
            checked = uiState.showHomeLearnCard,
            onCheckedChange = onSetShowLearnCard,
        )
    }
}

@Composable
private fun RootPermissionSettingsContent(
    uiState: SettingsUiState,
    onSetSuCompatMode: (Int) -> Unit,
    onSetKernelUmount: (Boolean) -> Unit,
    onSetWebViewZygoteUmount: (Boolean) -> Unit,
    onSetSelinuxHide: (Boolean) -> Unit,
    onSetSulog: (Boolean) -> Unit,
    onSetAdbRoot: (Boolean) -> Unit,
    onSetAvcSpoof: (Boolean) -> Unit,
    onSetUseSoftReboot: (Boolean) -> Unit,
    onOpen: (Route) -> Unit,
) {
    SettingsGroup(stringResource(R.string.settings_group_app_profiles)) {
        SettingsActionRow(
            title = stringResource(R.string.settings_profile_template),
            summary = stringResource(R.string.settings_profile_template_summary),
            icon = Icons.Rounded.Fence,
            onClick = { onOpen(Route.AppProfileTemplate) },
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_root_core)) {
        val suSummary = when (uiState.suCompatStatus) {
            "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
            "managed" -> stringResource(R.string.feature_status_managed_summary)
            else -> stringResource(R.string.settings_sucompat_summary)
        }
        SettingsChoiceRow(
            title = stringResource(R.string.settings_sucompat),
            summary = suSummary,
            icon = Icons.Rounded.RemoveModerator,
            options = listOf(
                stringResource(R.string.settings_mode_enable_by_default),
                stringResource(R.string.settings_mode_disable_until_reboot),
                stringResource(R.string.settings_mode_disable_always),
            ),
            selectedIndex = uiState.suCompatMode,
            enabled = uiState.suCompatStatus == "supported",
            onSelected = onSetSuCompatMode,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_kernel_umount),
            summary = featureSummary(uiState.kernelUmountStatus, R.string.settings_kernel_umount_summary),
            icon = Icons.Rounded.FolderDelete,
            enabled = uiState.kernelUmountStatus == "supported",
            checked = uiState.isKernelUmountEnabled,
            onCheckedChange = onSetKernelUmount,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_webview_zygote_umount),
            summary = featureSummary(
                uiState.webViewZygoteUmountStatus,
                R.string.settings_webview_zygote_umount_summary,
            ),
            icon = Icons.Rounded.Language,
            enabled = uiState.webViewZygoteUmountStatus == "supported",
            checked = uiState.isWebViewZygoteUmountEnabled,
            onCheckedChange = onSetWebViewZygoteUmount,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_selinux_hide),
            summary = featureSummary(uiState.selinuxHideStatus, R.string.settings_selinux_hide_summary),
            icon = Icons.Rounded.Shield,
            enabled = uiState.selinuxHideStatus == "supported",
            checked = uiState.isSelinuxHideEnabled,
            onCheckedChange = onSetSelinuxHide,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_sulog),
            summary = featureSummary(uiState.sulogStatus, R.string.settings_sulog_summary),
            icon = Icons.AutoMirrored.Rounded.Article,
            enabled = uiState.sulogStatus == "supported",
            checked = uiState.isSulogEnabled,
            onCheckedChange = onSetSulog,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_adb_root),
            summary = featureSummary(uiState.adbRootStatus, R.string.settings_adb_root_summary),
            icon = Icons.Rounded.Adb,
            enabled = uiState.adbRootStatus == "supported",
            checked = uiState.isAdbRootEnabled,
            onCheckedChange = onSetAdbRoot,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_avc_spoof),
            summary = featureSummary(uiState.avcSpoofStatus, R.string.settings_avc_spoof_summary),
            icon = Icons.Rounded.Policy,
            enabled = uiState.avcSpoofStatus == "supported",
            checked = uiState.isAvcSpoofEnabled,
            onCheckedChange = onSetAvcSpoof,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_soft_reboot),
            summary = stringResource(R.string.settings_soft_reboot_summary),
            icon = Icons.Rounded.ElectricalServices,
            enabled = !uiState.isLateLoadMode,
            checked = uiState.isLateLoadMode || uiState.useSoftReboot,
            onCheckedChange = onSetUseSoftReboot,
        )
    }
}

@Composable
private fun MountHideSettingsContent(
    uiState: SettingsUiState,
    onSetDefaultUmountModules: (Boolean) -> Unit,
    onSetKPatchNext: (Boolean) -> Unit,
    onOpenKPatchNextWebUi: () -> Unit,
    onOpenPathConfig: () -> Unit,
    onSetEpkesuHide: (Boolean) -> Unit,
    onOpen: (Route) -> Unit,
) {
    SettingsGroup(stringResource(R.string.settings_group_mounting)) {
        SettingsActionRow(
            title = stringResource(R.string.settings_builtin_mount),
            summary = uiState.builtinMountConflict?.let {
                stringResource(R.string.settings_builtin_mount_conflict_summary, it)
            } ?: stringResource(R.string.settings_builtin_mount_summary),
            icon = Icons.Rounded.Layers,
            onClick = { onOpen(Route.BuiltinMount) },
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_umount_modules_default),
            summary = stringResource(R.string.settings_umount_modules_default_summary),
            icon = Icons.Rounded.FolderDelete,
            checked = uiState.isDefaultUmountModules,
            onCheckedChange = onSetDefaultUmountModules,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_kpatch_next),
            summary = kPatchNextSummary(uiState),
            icon = Icons.Rounded.DeveloperMode,
            enabled = uiState.canToggleKPatchNext,
            checked = uiState.isKPatchNextSwitchChecked,
            onCheckedChange = onSetKPatchNext,
        )
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.settings_kpatch_next_webui),
            summary = stringResource(
                if (uiState.canOpenKPatchNextWebUi) {
                    R.string.settings_kpatch_next_webui_summary
                } else {
                    R.string.settings_kpatch_next_webui_disabled_summary
                }
            ),
            icon = Icons.Rounded.Apps,
            enabled = uiState.canOpenKPatchNextWebUi,
            onClick = onOpenKPatchNextWebUi,
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_hiding)) {
        SettingsActionRow(
            title = pathConfigTitle(uiState),
            summary = pathConfigSummary(uiState),
            icon = Icons.Rounded.Visibility,
            enabled = uiState.canOpenPathConfig,
            onClick = onOpenPathConfig,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_epkesu_hide),
            summary = stringResource(R.string.settings_epkesu_hide_summary),
            icon = Icons.Rounded.Security,
            checked = uiState.isEpkesuHideEnabled,
            onCheckedChange = onSetEpkesuHide,
        )
    }
}

@Composable
private fun ToolboxSettingsContent(
    uiState: SettingsUiState,
    onOpen: (Route) -> Unit,
    onSetGraphicsRendererEnabled: (Boolean) -> Unit,
) {
    SettingsGroup(stringResource(R.string.settings_toolbox_group_recovery)) {
        SettingsActionRow(
            title = stringResource(R.string.rescue_protection),
            summary = stringResource(R.string.rescue_protection_summary),
            icon = Icons.Rounded.Security,
            onClick = { onOpen(Route.RescueProtection) },
        )
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.image_tool_title),
            summary = stringResource(R.string.image_tool_settings_summary),
            icon = Icons.Rounded.ImageSearch,
            onClick = { onOpen(Route.ImageTool) },
        )
    }
    SettingsGroup(stringResource(R.string.settings_toolbox_group_system_identity)) {
        SettingsActionRow(
            title = stringResource(R.string.settings_cpu_spoof),
            summary = stringResource(R.string.settings_cpu_spoof_summary),
            icon = Icons.Rounded.DeveloperMode,
            onClick = { onOpen(Route.CpuSpoof) },
        )
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.settings_device_identity),
            summary = stringResource(R.string.settings_device_identity_summary),
            icon = Icons.Rounded.Badge,
            onClick = { onOpen(Route.DeviceIdentity) },
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_graphics_renderer_tool),
            summary = stringResource(R.string.settings_graphics_renderer_tool_summary),
            icon = Icons.Rounded.Tune,
            checked = uiState.graphicsRendererFeatureEnabled,
            onCheckedChange = onSetGraphicsRendererEnabled,
        )
        if (uiState.graphicsRendererFeatureEnabled) {
            SettingsDivider()
            SettingsActionRow(
                title = stringResource(R.string.settings_graphics_renderer),
                summary = stringResource(R.string.settings_graphics_renderer_summary),
                icon = Icons.Rounded.Tune,
                onClick = { onOpen(Route.GraphicsRenderer) },
            )
        }
        if (uiState.isKpmSettingsEntryVisible) {
            SettingsDivider()
            SettingsActionRow(
                title = stringResource(R.string.kpm_title),
                summary = stringResource(R.string.kpm_settings_summary),
                icon = Icons.Rounded.Bolt,
                onClick = { onOpen(Route.Kpm) },
            )
        }
    }
    SettingsGroup(stringResource(R.string.settings_toolbox_group_creative)) {
        SettingsActionRow(
            title = stringResource(R.string.settings_ai_chat),
            summary = stringResource(R.string.settings_ai_chat_summary),
            icon = Icons.Rounded.AutoFixHigh,
            onClick = { onOpen(Route.AiChat) },
        )
    }
}

@Composable
private fun AppMaintenanceSettingsContent(
    uiState: SettingsUiState,
    onSetCheckModuleUpdate: (Boolean) -> Unit,
    onSetVersionMismatchWarning: (Boolean) -> Unit,
    onSetGkiWarning: (Boolean) -> Unit,
    onSetWebDebugging: (Boolean) -> Unit,
    onSetAutoJailbreak: (Boolean) -> Unit,
    onSendLog: () -> Unit,
    onUninstall: () -> Unit,
    onOpen: (Route) -> Unit,
) {
    SettingsGroup(stringResource(R.string.settings_group_general)) {
        SettingsActionRow(
            title = stringResource(R.string.settings_language),
            summary = stringResource(R.string.settings_language_summary),
            icon = Icons.Rounded.Language,
            onClick = { onOpen(Route.LanguageSettings) },
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_updates_warnings)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_module_check_update),
            summary = stringResource(R.string.settings_module_check_update_summary),
            icon = Icons.Rounded.UploadFile,
            checked = uiState.checkModuleUpdate,
            onCheckedChange = onSetCheckModuleUpdate,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_version_mismatch_warning),
            summary = stringResource(R.string.settings_version_mismatch_warning_summary),
            icon = Icons.Rounded.BugReport,
            checked = uiState.showVersionMismatchWarning,
            onCheckedChange = onSetVersionMismatchWarning,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_gki_warning),
            summary = stringResource(R.string.settings_gki_warning_summary),
            icon = Icons.Rounded.BugReport,
            checked = uiState.showGkiWarning,
            onCheckedChange = onSetGkiWarning,
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_diagnostics)) {
        SettingsActionRow(
            title = stringResource(R.string.send_log),
            summary = stringResource(R.string.settings_log_export_summary),
            icon = Icons.Rounded.BugReport,
            onClick = onSendLog,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.enable_web_debugging),
            summary = stringResource(R.string.enable_web_debugging_summary),
            icon = Icons.Rounded.DeveloperMode,
            checked = uiState.enableWebDebugging,
            onCheckedChange = onSetWebDebugging,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_auto_jailbreak),
            summary = stringResource(R.string.settings_auto_jailbreak_summary),
            icon = Icons.Rounded.ElectricalServices,
            enabled = uiState.isLateLoadMode,
            checked = uiState.autoJailbreak,
            onCheckedChange = onSetAutoJailbreak,
        )
    }
    SettingsGroup(stringResource(R.string.settings_group_maintenance_about)) {
        SettingsActionRow(
            title = stringResource(R.string.settings_uninstall),
            summary = stringResource(
                if (uiState.isLkmMode && !uiState.isLateLoadMode) {
                    R.string.settings_uninstall_summary
                } else {
                    R.string.feature_status_unsupported_summary
                }
            ),
            icon = Icons.Rounded.Delete,
            enabled = uiState.isLkmMode && !uiState.isLateLoadMode,
            destructive = true,
            onClick = onUninstall,
        )
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.about),
            summary = stringResource(R.string.settings_about_summary),
            icon = Icons.Rounded.ContactPage,
            onClick = { onOpen(Route.About) },
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val isInk = isInkInterfaceStyle()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            color = if (isInk) {
                Color.Transparent
            } else {
                immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow)
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = shape,
            modifier = Modifier
                .fillMaxWidth()
                .inkMiuixCardSurface(shape = shape),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    summary: String,
    icon: ImageVector,
    enabled: Boolean = true,
    destructive: Boolean = false,
    trailingText: String? = null,
    onClick: () -> Unit,
) {
    val accent = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsRowIcon(icon = icon, color = accent, enabled = enabled)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = if (enabled) accent.takeIf { destructive } ?: MaterialTheme.colorScheme.onSurface else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailingText?.let {
            Text(
                text = it,
                color = accent,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 96.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.8f else 0.35f),
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .heightIn(min = 60.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsRowIcon(icon = icon, color = MaterialTheme.colorScheme.primary, enabled = enabled)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.45f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StyledSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsChoiceRow(
    title: String,
    summary: String,
    icon: ImageVector,
    options: List<String>,
    selectedIndex: Int,
    enabled: Boolean = true,
    onSelected: (Int) -> Unit,
) {
    var showDialog by rememberSaveable(title) { mutableStateOf(false) }
    val safeIndex = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    SettingsActionRow(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled && options.isNotEmpty(),
        trailingText = options.getOrNull(safeIndex),
        onClick = { showDialog = true },
    )
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState())
                        .selectableGroup(),
                ) {
                    options.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = index == safeIndex,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onSelected(index)
                                        showDialog = false
                                    },
                                )
                                .padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = index == safeIndex,
                                onClick = null,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(option, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsRowIcon(icon: ImageVector, color: Color, enabled: Boolean) {
    Surface(
        color = color.copy(alpha = if (enabled) 0.13f else 0.06f),
        contentColor = color.copy(alpha = if (enabled) 1f else 0.45f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f),
    )
}

@Composable
private fun featureSummary(status: String, defaultSummary: Int): String = when (status) {
    "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
    "managed" -> stringResource(R.string.feature_status_managed_summary)
    else -> stringResource(defaultSummary)
}
