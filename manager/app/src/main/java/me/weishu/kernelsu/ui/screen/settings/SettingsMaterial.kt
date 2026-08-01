package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.RemoveModerator
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.KsuIsValid
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.material.SegmentedColumn
import me.weishu.kernelsu.ui.component.material.SegmentedDropdownItem
import me.weishu.kernelsu.ui.component.material.SegmentedListItem
import me.weishu.kernelsu.ui.component.material.SegmentedSwitchItem
import me.weishu.kernelsu.ui.component.material.SendLogBottomSheet
import me.weishu.kernelsu.ui.component.material.SnackBarHost
import me.weishu.kernelsu.ui.component.material.expressiveTopAppBarColors
import me.weishu.kernelsu.ui.component.uninstalldialog.UninstallDialog

/**
 * Compact Material settings page.
 *
 * Keep the page single-level for the collapsed settings mode, but use the same
 * six sections as the category mode so every entry has one predictable home.
 */
@Composable
fun SettingPagerMaterial(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = remember { SnackbarHostState() }
    val showUninstallDialog = rememberSaveable { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    UninstallDialog(
        show = showUninstallDialog.value,
        onDismissRequest = { showUninstallDialog.value = false },
    )

    ExpressiveScaffold(
        topBar = { TopBar(scrollBehavior = scrollBehavior) },
        snackbarHost = {
            SnackBarHost(
                hostState = snackBarHost,
                modifier = Modifier.padding(bottom = bottomInnerPadding),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp),
        ) {
            MaterialSettingsSection(SettingsCategory.Appearance) {
                SegmentedColumn(
                    content = listOf(
                        {
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Dashboard,
                                title = stringResource(R.string.settings_ui_mode),
                                summary = stringResource(R.string.settings_ui_mode_summary),
                                items = InterfaceStyle.selectableEntries.map { stringResource(it.labelRes) },
                                selectedIndex = InterfaceStyle.selectedIndex(uiState.uiMode),
                                onItemSelected = actions.onSetUiModeIndex,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Filled.Palette,
                                title = stringResource(R.string.settings_day_night_switch),
                                summary = stringResource(R.string.settings_day_night_switch_summary),
                                checked = isDayNightSwitchChecked(uiState.themeMode),
                                onCheckedChange = actions.onSetDayNightMode,
                            )
                        },
                        {
                            MaterialSettingsLink(
                                title = stringResource(R.string.settings_theme),
                                summary = stringResource(R.string.settings_theme_summary),
                                icon = Icons.Filled.Palette,
                                onClick = actions.onOpenTheme,
                            )
                        },
                        {
                            MaterialSettingsLink(
                                title = stringResource(R.string.theme_store),
                                summary = stringResource(R.string.theme_store_settings_summary),
                                icon = Icons.Rounded.Storefront,
                                onClick = actions.onOpenThemeStore,
                            )
                        },
                        {
                            MaterialSettingsLink(
                                title = stringResource(R.string.settings_section_visual_effects),
                                summary = stringResource(R.string.settings_visual_effects_summary),
                                icon = Icons.Rounded.Visibility,
                                onClick = actions.onOpenVisualEffects,
                            )
                        },
                        {
                            MaterialSettingsLink(
                                title = stringResource(R.string.settings_ui_decoration_library),
                                summary = stringResource(R.string.settings_ui_decoration_library_summary),
                                icon = Icons.Rounded.Brush,
                                onClick = actions.onOpenUiDecorationLibrary,
                            )
                        },
                    ),
                )
            }

            MaterialSettingsSection(SettingsCategory.HomeAndManager) {
                SegmentedColumn(
                    content = listOf(
                        {
                            MaterialSettingsLink(
                                title = stringResource(R.string.settings_manager_identity),
                                summary = stringResource(R.string.settings_manager_identity_summary),
                                icon = Icons.Rounded.Badge,
                                onClick = actions.onOpenLauncherIcon,
                            )
                        },
                        {
                            MaterialSettingsLink(
                                title = stringResource(R.string.settings_home_title),
                                summary = if (uiState.customHomeTitle.isBlank()) {
                                    stringResource(R.string.settings_home_title_default_summary)
                                } else {
                                    stringResource(
                                        R.string.settings_home_title_custom_summary,
                                        uiState.customHomeTitle,
                                    )
                                },
                                icon = Icons.Rounded.EditNote,
                                onClick = actions.onEditHomeTitle,
                            )
                        },
                        {
                            MaterialSettingsLink(
                                title = stringResource(R.string.home_layout_title),
                                summary = stringResource(R.string.home_layout_settings_summary),
                                icon = Icons.Rounded.Dashboard,
                                onClick = actions.onOpenHomeLayout,
                            )
                        },
                        {
                            MaterialSettingsLink(
                                title = stringResource(R.string.settings_navigation_icons),
                                summary = stringResource(R.string.settings_navigation_icons_summary),
                                icon = Icons.Rounded.Apps,
                                onClick = actions.onOpenNavigationIcons,
                            )
                        },
                        {
                            MaterialSettingsLink(
                                title = stringResource(R.string.theme_store_cards),
                                summary = stringResource(R.string.theme_store_assets_summary),
                                icon = Icons.Rounded.Layers,
                                onClick = actions.onOpenHomeCardWallpapers,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.Visibility,
                                title = stringResource(R.string.settings_show_home_support_card),
                                summary = stringResource(R.string.settings_show_home_support_card_summary),
                                checked = uiState.showHomeSupportCard,
                                onCheckedChange = actions.onSetShowHomeSupportCard,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.Visibility,
                                title = stringResource(R.string.settings_show_home_learn_card),
                                summary = stringResource(R.string.settings_show_home_learn_card_summary),
                                checked = uiState.showHomeLearnCard,
                                onCheckedChange = actions.onSetShowHomeLearnCard,
                            )
                        },
                    ),
                )
            }

            KsuIsValid {
                MaterialSettingsSection(SettingsCategory.RootAndPermissions) {
                    val suCompatModeItems = listOf(
                        stringResource(R.string.settings_mode_enable_by_default),
                        stringResource(R.string.settings_mode_disable_until_reboot),
                        stringResource(R.string.settings_mode_disable_always),
                    )
                    SegmentedColumn(
                        content = listOf(
                            {
                                MaterialSettingsLink(
                                    title = stringResource(R.string.settings_profile_template),
                                    summary = stringResource(R.string.settings_profile_template_summary),
                                    icon = Icons.Filled.Fence,
                                    onClick = actions.onOpenProfileTemplate,
                                )
                            },
                            {
                                SegmentedDropdownItem(
                                    icon = Icons.Filled.RemoveModerator,
                                    title = stringResource(R.string.settings_sucompat),
                                    summary = materialFeatureSummary(
                                        uiState.suCompatStatus,
                                        R.string.settings_sucompat_summary,
                                    ),
                                    items = suCompatModeItems,
                                    enabled = uiState.suCompatStatus == "supported",
                                    selectedIndex = uiState.suCompatMode,
                                    onItemSelected = actions.onSetSuCompatMode,
                                )
                            },
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.Filled.RemoveCircle,
                                    title = stringResource(R.string.settings_kernel_umount),
                                    summary = materialFeatureSummary(
                                        uiState.kernelUmountStatus,
                                        R.string.settings_kernel_umount_summary,
                                    ),
                                    enabled = uiState.kernelUmountStatus == "supported",
                                    checked = uiState.isKernelUmountEnabled,
                                    onCheckedChange = actions.onSetKernelUmountEnabled,
                                )
                            },
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.Filled.Policy,
                                    title = stringResource(R.string.settings_selinux_hide),
                                    summary = materialFeatureSummary(
                                        uiState.selinuxHideStatus,
                                        R.string.settings_selinux_hide_summary,
                                    ),
                                    enabled = uiState.selinuxHideStatus == "supported",
                                    checked = uiState.isSelinuxHideEnabled,
                                    onCheckedChange = actions.onSetSelinuxHideEnabled,
                                )
                            },
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.AutoMirrored.Filled.Article,
                                    title = stringResource(R.string.settings_sulog),
                                    summary = materialFeatureSummary(
                                        uiState.sulogStatus,
                                        R.string.settings_sulog_summary,
                                    ),
                                    enabled = uiState.sulogStatus == "supported",
                                    checked = uiState.isSulogEnabled,
                                    onCheckedChange = actions.onSetSulogEnabled,
                                )
                            },
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.Filled.Adb,
                                    title = stringResource(R.string.settings_adb_root),
                                    summary = materialFeatureSummary(
                                        uiState.adbRootStatus,
                                        R.string.settings_adb_root_summary,
                                    ),
                                    enabled = uiState.adbRootStatus == "supported",
                                    checked = uiState.isAdbRootEnabled,
                                    onCheckedChange = actions.onSetAdbRootEnabled,
                                )
                            },
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.Filled.Policy,
                                    title = stringResource(R.string.settings_avc_spoof),
                                    summary = materialFeatureSummary(
                                        uiState.avcSpoofStatus,
                                        R.string.settings_avc_spoof_summary,
                                    ),
                                    enabled = uiState.avcSpoofStatus == "supported",
                                    checked = uiState.isAvcSpoofEnabled,
                                    onCheckedChange = actions.onSetAvcSpoofEnabled,
                                )
                            },
                        ),
                    )
                }
            }

            KsuIsValid {
                MaterialSettingsSection(SettingsCategory.MountAndHide) {
                    val builtinMountSummary = uiState.builtinMountConflict?.let {
                        stringResource(R.string.settings_builtin_mount_conflict_summary, it)
                    } ?: stringResource(R.string.settings_builtin_mount_summary)
                    SegmentedColumn(
                        content = buildList {
                            add {
                                SegmentedSwitchItem(
                                    icon = Icons.Filled.FolderDelete,
                                    title = stringResource(R.string.settings_umount_modules_default),
                                    summary = stringResource(R.string.settings_umount_modules_default_summary),
                                    checked = uiState.isDefaultUmountModules,
                                    onCheckedChange = actions.onSetDefaultUmountModules,
                                )
                            }
                            add {
                                MaterialSettingsLink(
                                    title = stringResource(R.string.settings_builtin_mount),
                                    summary = builtinMountSummary,
                                    icon = Icons.Rounded.Layers,
                                    onClick = actions.onOpenBuiltinMount,
                                )
                            }
                            add {
                                SegmentedSwitchItem(
                                    icon = Icons.Filled.DeveloperMode,
                                    title = stringResource(R.string.settings_kpatch_next),
                                    summary = kPatchNextSummary(uiState),
                                    enabled = uiState.canToggleKPatchNext,
                                    checked = uiState.isKPatchNextSwitchChecked,
                                    onCheckedChange = actions.onSetKPatchNextEnabled,
                                )
                            }
                            if (uiState.isKPatchNextEnabled || uiState.canOpenKPatchNextWebUi) {
                                add {
                                    MaterialSettingsLink(
                                        title = stringResource(R.string.settings_kpatch_next_webui),
                                        summary = stringResource(
                                            if (uiState.canOpenKPatchNextWebUi) {
                                                R.string.settings_kpatch_next_webui_summary
                                            } else {
                                                R.string.settings_kpatch_next_webui_disabled_summary
                                            },
                                        ),
                                        icon = Icons.Rounded.Apps,
                                        enabled = uiState.canOpenKPatchNextWebUi,
                                        onClick = actions.onOpenKPatchNextWebUi,
                                    )
                                }
                            }
                            add {
                                MaterialSettingsLink(
                                    title = pathConfigTitle(uiState),
                                    summary = pathConfigSummary(uiState),
                                    icon = Icons.Rounded.Visibility,
                                    enabled = uiState.canOpenPathConfig,
                                    onClick = actions.onOpenHiddenPathConfig,
                                )
                            }
                            add {
                                SegmentedSwitchItem(
                                    icon = Icons.Filled.Policy,
                                    title = stringResource(R.string.settings_epkesu_hide),
                                    summary = stringResource(R.string.settings_epkesu_hide_summary),
                                    checked = uiState.isEpkesuHideEnabled,
                                    onCheckedChange = actions.onSetEpkesuHideEnabled,
                                )
                            }
                        },
                    )
                }
            }

            MaterialSettingsSection(SettingsCategory.Toolbox) {
                SegmentedColumn(
                    content = buildList {
                        add {
                            MaterialSettingsLink(
                                title = stringResource(R.string.rescue_protection),
                                summary = stringResource(R.string.rescue_protection_summary),
                                icon = Icons.Rounded.AutoFixHigh,
                                onClick = actions.onOpenRescueProtection,
                            )
                        }
                        add {
                            MaterialSettingsLink(
                                title = stringResource(R.string.image_tool_title),
                                summary = stringResource(R.string.image_tool_settings_summary),
                                icon = Icons.Rounded.ImageSearch,
                                onClick = actions.onOpenImageTool,
                            )
                        }
                        add {
                            MaterialSettingsLink(
                                title = stringResource(R.string.settings_cpu_spoof),
                                summary = stringResource(R.string.settings_cpu_spoof_summary),
                                icon = Icons.Filled.DeveloperMode,
                                onClick = actions.onOpenCpuSpoof,
                            )
                        }
                        add {
                            MaterialSettingsLink(
                                title = stringResource(R.string.settings_device_identity),
                                summary = stringResource(R.string.settings_device_identity_summary),
                                icon = Icons.Rounded.Badge,
                                onClick = actions.onOpenDeviceIdentity,
                            )
                        }
                        add {
                            MaterialSettingsLink(
                                title = stringResource(R.string.settings_ai_chat),
                                summary = stringResource(R.string.settings_ai_chat_summary),
                                icon = Icons.Rounded.AutoFixHigh,
                                onClick = actions.onOpenAiChat,
                            )
                        }
                        add {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.Tune,
                                title = stringResource(R.string.settings_graphics_renderer_tool),
                                summary = stringResource(R.string.settings_graphics_renderer_tool_summary),
                                checked = uiState.graphicsRendererFeatureEnabled,
                                onCheckedChange = actions.onSetGraphicsRendererFeatureEnabled,
                            )
                        }
                        if (uiState.graphicsRendererFeatureEnabled) {
                            add {
                                MaterialSettingsLink(
                                    title = stringResource(R.string.settings_graphics_renderer),
                                    summary = stringResource(R.string.settings_graphics_renderer_summary),
                                    icon = Icons.Rounded.Tune,
                                    onClick = actions.onOpenGraphicsRenderer,
                                )
                            }
                        }
                    },
                )
            }

            MaterialSettingsSection(SettingsCategory.AppAndMaintenance) {
                SegmentedColumn(
                    content = listOf(
                        {
                            MaterialSettingsLink(
                                title = stringResource(R.string.settings_language),
                                summary = stringResource(R.string.settings_language_summary),
                                icon = Icons.Rounded.Language,
                                onClick = actions.onOpenLanguage,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Filled.DeveloperMode,
                                title = stringResource(R.string.enable_web_debugging),
                                summary = stringResource(R.string.enable_web_debugging_summary),
                                checked = uiState.enableWebDebugging,
                                onCheckedChange = actions.onSetEnableWebDebugging,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Filled.ElectricalServices,
                                title = stringResource(R.string.settings_auto_jailbreak),
                                summary = stringResource(R.string.settings_auto_jailbreak_summary),
                                enabled = uiState.isLateLoadMode,
                                checked = uiState.autoJailbreak,
                                onCheckedChange = actions.onSetAutoJailbreak,
                            )
                        },
                    ),
                )

                KsuIsValid {
                    Spacer(Modifier.height(8.dp))
                    SegmentedColumn(
                        content = listOf(
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.Rounded.UploadFile,
                                    title = stringResource(R.string.settings_module_check_update),
                                    summary = stringResource(R.string.settings_module_check_update_summary),
                                    checked = uiState.checkModuleUpdate,
                                    onCheckedChange = actions.onSetCheckModuleUpdate,
                                )
                            },
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.Rounded.BugReport,
                                    title = stringResource(R.string.settings_version_mismatch_warning),
                                    summary = stringResource(R.string.settings_version_mismatch_warning_summary),
                                    checked = uiState.showVersionMismatchWarning,
                                    onCheckedChange = actions.onSetShowVersionMismatchWarning,
                                )
                            },
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.Rounded.BugReport,
                                    title = stringResource(R.string.settings_gki_warning),
                                    summary = stringResource(R.string.settings_gki_warning_summary),
                                    checked = uiState.showGkiWarning,
                                    onCheckedChange = actions.onSetShowGkiWarning,
                                )
                            },
                        ),
                    )
                }

                if (uiState.isLkmMode) {
                    Spacer(Modifier.height(8.dp))
                    SegmentedColumn(
                        content = listOf {
                            val uninstall = stringResource(R.string.settings_uninstall)
                            SegmentedListItem(
                                onClick = { showUninstallDialog.value = true },
                                enabled = !uiState.isLateLoadMode,
                                headlineContent = { Text(uninstall) },
                                supportingContent = { Text(stringResource(R.string.settings_uninstall_summary)) },
                                leadingContent = { Icon(Icons.Filled.Delete, uninstall) },
                            )
                        },
                    )
                }

                Spacer(Modifier.height(8.dp))
                SegmentedColumn(
                    content = listOf(
                        {
                            SegmentedListItem(
                                onClick = { showBottomSheet = true },
                                headlineContent = { Text(stringResource(R.string.send_log)) },
                                supportingContent = { Text(stringResource(R.string.settings_log_export_summary)) },
                                leadingContent = {
                                    Icon(Icons.Filled.BugReport, stringResource(R.string.send_log))
                                },
                            )
                        },
                        {
                            SegmentedListItem(
                                onClick = actions.onOpenAbout,
                                headlineContent = { Text(stringResource(R.string.about)) },
                                supportingContent = { Text(stringResource(R.string.settings_about_summary)) },
                                leadingContent = {
                                    Icon(Icons.Filled.ContactPage, stringResource(R.string.about))
                                },
                            )
                        },
                    ),
                )
            }

            if (showBottomSheet) {
                SendLogBottomSheet(
                    onDismiss = { showBottomSheet = false },
                    snackbarHostState = snackBarHost,
                )
            }
            Spacer(Modifier.height(bottomInnerPadding + 8.dp))
        }
    }
}

@Composable
private fun MaterialSettingsSection(
    category: SettingsCategory,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
    ) {
        MaterialSettingsSectionHeader(category)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun MaterialSettingsSectionHeader(category: SettingsCategory) {
    val accent = categoryAccent(category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    color = accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = category.icon(),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(category.summaryRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun materialFeatureSummary(status: String, defaultSummary: Int): String = when (status) {
    "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
    "managed" -> stringResource(R.string.feature_status_managed_summary)
    else -> stringResource(defaultSummary)
}

@Composable
private fun TopBar(scrollBehavior: TopAppBarScrollBehavior? = null) {
    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        colors = expressiveTopAppBarColors(),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun MaterialSettingsLink(
    title: String,
    summary: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    SegmentedListItem(
        onClick = onClick,
        enabled = enabled,
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(icon, contentDescription = title) },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        },
    )
}
