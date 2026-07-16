package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fence
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.KsuIsValid
import me.weishu.kernelsu.ui.component.dialog.rememberLoadingDialog
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassSurface
import me.weishu.kernelsu.ui.component.snow.snowMiuixCardColors
import me.weishu.kernelsu.ui.component.snow.snowMiuixCardSurface
import me.weishu.kernelsu.ui.component.snow.isSnowInterfaceStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.miuix.SendLogDialog
import me.weishu.kernelsu.ui.component.uninstalldialog.UninstallDialog
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.theme.skrootproTopBarColors
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import me.weishu.kernelsu.ui.component.miuix.SunMoonSwitchPreference as SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * @author weishu
 * @date 2023/1/1.
 */
@Composable
fun SettingPagerMiuix(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    val topBarColors = skrootproTopBarColors(barColor, colorScheme.onSurface)
    val loadingDialog = rememberLoadingDialog()
    val showUninstallDialog = rememberSaveable { mutableStateOf(false) }
    val showSendLogDialog = rememberSaveable { mutableStateOf(false) }
    val showWallpaperOpacitySlider = rememberSaveable { mutableStateOf(false) }
    val showWallpaperPassthroughOpacitySlider = rememberSaveable { mutableStateOf(false) }
    val showVideoBackgroundDurationSlider = rememberSaveable { mutableStateOf(false) }
    var updatesExpanded by rememberSaveable { mutableStateOf(false) }
    var appearanceExpanded by rememberSaveable { mutableStateOf(false) }
    var profilesExpanded by rememberSaveable { mutableStateOf(false) }
    var rootFeaturesExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var maintenanceExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = topBarColors.container,
                    titleColor = topBarColors.content,
                    title = stringResource(R.string.settings),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    KsuIsValid {
                        CollapsibleMiuixSection(
                            title = stringResource(R.string.settings_section_updates),
                            summary = stringResource(R.string.settings_section_updates_summary),
                            icon = Icons.Rounded.UploadFile,
                            itemCount = UPDATES_ITEM_COUNT,
                            expanded = updatesExpanded,
                            onExpandedChange = { updatesExpanded = it },
                            topPadding = 12.dp,
                        ) {
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_module_check_update),
                                summary = stringResource(id = R.string.settings_module_check_update_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.UploadFile,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_module_check_update),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.checkModuleUpdate,
                                onCheckedChange = actions.onSetCheckModuleUpdate
                            )
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_version_mismatch_warning),
                                summary = stringResource(id = R.string.settings_version_mismatch_warning_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.BugReport,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_version_mismatch_warning),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.showVersionMismatchWarning,
                                onCheckedChange = actions.onSetShowVersionMismatchWarning
                            )
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_gki_warning),
                                summary = stringResource(id = R.string.settings_gki_warning_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.BugReport,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_gki_warning),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.showGkiWarning,
                                onCheckedChange = actions.onSetShowGkiWarning
                            )
                        }
                    }

                    CollapsibleMiuixSection(
                        title = stringResource(R.string.settings_section_appearance),
                        summary = stringResource(R.string.settings_section_appearance_summary),
                        icon = Icons.Rounded.Palette,
                        itemCount = uiState.appearanceSectionItemCount(),
                        expanded = appearanceExpanded,
                        onExpandedChange = { appearanceExpanded = it },
                    ) {
                        val dayNightChecked = isDayNightSwitchChecked(uiState.themeMode)
                        OverlayDropdownPreference(
                            title = stringResource(id = R.string.settings_ui_mode),
                            summary = stringResource(id = R.string.settings_ui_mode_summary),
                            items = InterfaceStyle.entries.map { stringResource(it.labelRes) },
                            startAction = {
                                Icon(
                                    Icons.Rounded.Dashboard,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_ui_mode),
                                    tint = colorScheme.onBackground
                                )
                            },
                            selectedIndex = InterfaceStyle.selectedIndex(uiState.uiMode),
                            onSelectedIndexChange = actions.onSetUiModeIndex
                        )
                        if (uiState.uiMode == InterfaceStyle.Snow.value) {
                            SeasonMiuixPreference(
                                selectedValue = uiState.seasonStyle,
                                onSelectedIndexChange = actions.onSetSeasonStyleIndex,
                            )
                        }
                        DayNightMiuixPreference(
                            title = stringResource(id = R.string.settings_day_night_switch),
                            summary = stringResource(id = R.string.settings_day_night_switch_summary),
                            checked = dayNightChecked,
                            onCheckedChange = actions.onSetDayNightMode,
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_section_visual_effects),
                            summary = stringResource(id = R.string.settings_visual_effects_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Visibility,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_section_visual_effects),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenVisualEffects
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_ui_decoration_library),
                            summary = stringResource(id = R.string.settings_ui_decoration_library_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.AutoFixHigh,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_ui_decoration_library),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenUiDecorationLibrary
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_theme),
                            summary = stringResource(id = R.string.settings_theme_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Palette,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_theme),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenTheme
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.theme_store),
                            summary = stringResource(id = R.string.theme_store_settings_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Storefront,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.theme_store),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenThemeStore
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_manager_identity),
                            summary = stringResource(id = R.string.settings_manager_identity_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Apps,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_manager_identity),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenLauncherIcon
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_navigation_icons),
                            summary = stringResource(id = R.string.settings_navigation_icons_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Apps,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_navigation_icons),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenNavigationIcons
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.home_card_wallpapers),
                            summary = stringResource(id = R.string.home_card_wallpapers_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Wallpaper,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.home_card_wallpapers),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenHomeCardWallpapers
                        )
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_show_home_support_card),
                            summary = stringResource(id = R.string.settings_show_home_support_card_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Visibility,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_show_home_support_card),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.showHomeSupportCard,
                            onCheckedChange = actions.onSetShowHomeSupportCard
                        )
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_show_home_learn_card),
                            summary = stringResource(id = R.string.settings_show_home_learn_card_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Visibility,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_show_home_learn_card),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.showHomeLearnCard,
                            onCheckedChange = actions.onSetShowHomeLearnCard
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_backgrounds),
                            summary = stringResource(id = R.string.settings_backgrounds_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Wallpaper,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_backgrounds),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenBackgrounds
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_sound_effects),
                            summary = stringResource(id = R.string.settings_sound_effects_summary),
                            startAction = {
                                Icon(
                                    Icons.AutoMirrored.Rounded.VolumeUp,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_sound_effects),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenSoundEffects
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_startup_animation),
                            summary = stringResource(
                                if (uiState.customStartupAnimationUri == null) {
                                    R.string.settings_startup_animation_summary
                                } else {
                                    R.string.settings_startup_animation_selected_summary
                                }
                            ),
                            startAction = {
                                Icon(
                                    Icons.Rounded.PlayCircle,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_startup_animation),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenStartupAnimation
                        )
                    }

                    KsuIsValid {
                        CollapsibleMiuixSection(
                            title = stringResource(R.string.settings_section_profiles),
                            summary = stringResource(R.string.settings_section_profiles_summary),
                            icon = Icons.Rounded.Fence,
                            itemCount = 1,
                            expanded = profilesExpanded,
                            onExpandedChange = { profilesExpanded = it },
                        ) {
                            val profileTemplate = stringResource(id = R.string.settings_profile_template)
                            ArrowPreference(
                                title = profileTemplate,
                                summary = stringResource(id = R.string.settings_profile_template_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Fence,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = profileTemplate,
                                        tint = colorScheme.onBackground
                                    )
                                },
                                onClick = actions.onOpenProfileTemplate
                            )
                        }
                    }

                    KsuIsValid {
                        CollapsibleMiuixSection(
                            title = stringResource(R.string.settings_section_root_features),
                            summary = stringResource(R.string.settings_section_root_features_summary),
                            icon = Icons.Rounded.RemoveModerator,
                            itemCount = ROOT_FEATURES_ITEM_COUNT,
                            expanded = rootFeaturesExpanded,
                            onExpandedChange = { rootFeaturesExpanded = it },
                        ) {
                            val suCompatModeItems = listOf(
                                stringResource(id = R.string.settings_mode_enable_by_default),
                                stringResource(id = R.string.settings_mode_disable_until_reboot),
                                stringResource(id = R.string.settings_mode_disable_always),
                            )

                            val suSummary = when (uiState.suCompatStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_sucompat_summary)
                            }
                            OverlayDropdownPreference(
                                title = stringResource(id = R.string.settings_sucompat),
                                summary = suSummary,
                                items = suCompatModeItems,
                                startAction = {
                                    Icon(
                                        Icons.Rounded.RemoveModerator,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_sucompat),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                enabled = uiState.suCompatStatus == "supported",
                                selectedIndex = uiState.suCompatMode,
                                onSelectedIndexChange = actions.onSetSuCompatMode
                            )

                            val umountSummary = when (uiState.kernelUmountStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_kernel_umount_summary)
                            }
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_kernel_umount),
                                summary = umountSummary,
                                startAction = {
                                    Icon(
                                        Icons.Rounded.RemoveCircle,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_kernel_umount),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                enabled = uiState.kernelUmountStatus == "supported",
                                checked = uiState.isKernelUmountEnabled,
                                onCheckedChange = actions.onSetKernelUmountEnabled
                            )

                            val selinuxHideSummary = when (uiState.selinuxHideStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_selinux_hide_summary)
                            }
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_selinux_hide),
                                summary = selinuxHideSummary,
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Policy,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_selinux_hide),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                enabled = uiState.selinuxHideStatus == "supported",
                                checked = uiState.isSelinuxHideEnabled,
                                onCheckedChange = actions.onSetSelinuxHideEnabled
                            )

                            val sulogSummary = when (uiState.sulogStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_sulog_summary)
                            }
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_sulog),
                                summary = sulogSummary,
                                startAction = {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Article,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_sulog),
                                        tint = if (uiState.sulogStatus == "supported") colorScheme.onBackground else colorScheme.disabledOnSecondaryVariant
                                    )
                                },
                                enabled = uiState.sulogStatus == "supported",
                                checked = uiState.isSulogEnabled,
                                onCheckedChange = actions.onSetSulogEnabled
                            )

                            val adbRootSummary = when (uiState.adbRootStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_adb_root_summary)
                            }
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_adb_root),
                                summary = adbRootSummary,
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Adb,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_adb_root),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                enabled = uiState.adbRootStatus == "supported",
                                checked = uiState.isAdbRootEnabled,
                                onCheckedChange = actions.onSetAdbRootEnabled
                            )

                            val avcSpoofSummary = when (uiState.avcSpoofStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_avc_spoof_summary)
                            }
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_avc_spoof),
                                summary = avcSpoofSummary,
                                startAction = {
                                    Icon(
                                        Icons.Rounded.EditNote,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_avc_spoof),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                enabled = uiState.avcSpoofStatus == "supported",
                                checked = uiState.isAvcSpoofEnabled,
                                onCheckedChange = actions.onSetAvcSpoofEnabled
                            )

                            SwitchPreference(
                                title = stringResource(id = R.string.settings_epkesu_hide),
                                summary = stringResource(id = R.string.settings_epkesu_hide_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Visibility,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_epkesu_hide),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.isEpkesuHideEnabled,
                                onCheckedChange = actions.onSetEpkesuHideEnabled
                            )
                        }

                        CollapsibleMiuixSection(
                            title = stringResource(R.string.settings_section_advanced),
                            summary = stringResource(R.string.settings_section_advanced_summary),
                            icon = Icons.Rounded.DeveloperMode,
                            itemCount = ADVANCED_ITEM_COUNT + 1 +
                                if (uiState.graphicsRendererFeatureEnabled) 1 else 0,
                            expanded = advancedExpanded,
                            onExpandedChange = { advancedExpanded = it },
                        ) {
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_umount_modules_default),
                                summary = stringResource(id = R.string.settings_umount_modules_default_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.FolderDelete,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_umount_modules_default),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.isDefaultUmountModules,
                                onCheckedChange = actions.onSetDefaultUmountModules
                            )

                            val builtinMountSummary = uiState.builtinMountConflict?.let {
                                stringResource(id = R.string.settings_builtin_mount_conflict_summary, it)
                            } ?: stringResource(id = R.string.settings_builtin_mount_summary)
                            ArrowPreference(
                                title = stringResource(id = R.string.settings_builtin_mount),
                                summary = builtinMountSummary,
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Layers,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_builtin_mount),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = actions.onOpenBuiltinMount,
                            )

                            SwitchPreference(
                                title = stringResource(id = R.string.settings_kpatch_next),
                                summary = kPatchNextSummary(uiState),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.DeveloperMode,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_kpatch_next),
                                        tint = if (uiState.canToggleKPatchNext) {
                                            colorScheme.onBackground
                                        } else {
                                            colorScheme.disabledOnSecondaryVariant
                                        }
                                    )
                                },
                                enabled = uiState.canToggleKPatchNext,
                                checked = uiState.isKPatchNextEnabled,
                                onCheckedChange = actions.onSetKPatchNextEnabled
                            )

                            ArrowPreference(
                                title = stringResource(id = R.string.settings_kpatch_next_webui),
                                summary = stringResource(
                                    id = if (uiState.canOpenKPatchNextWebUi) {
                                        R.string.settings_kpatch_next_webui_summary
                                    } else {
                                        R.string.settings_kpatch_next_webui_disabled_summary
                                    }
                                ),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Apps,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_kpatch_next_webui),
                                        tint = if (uiState.canOpenKPatchNextWebUi) {
                                            colorScheme.onBackground
                                        } else {
                                            colorScheme.disabledOnSecondaryVariant
                                        }
                                    )
                                },
                                enabled = uiState.canOpenKPatchNextWebUi,
                                onClick = actions.onOpenKPatchNextWebUi
                            )

                            ArrowPreference(
                                title = stringResource(id = R.string.hidden_path_config),
                                summary = stringResource(id = R.string.hidden_path_config_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Visibility,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.hidden_path_config),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                onClick = actions.onOpenHiddenPathConfig
                            )

                            ArrowPreference(
                                title = stringResource(id = R.string.settings_cpu_spoof),
                                summary = stringResource(id = R.string.settings_cpu_spoof_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.DeveloperMode,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_cpu_spoof),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                onClick = actions.onOpenCpuSpoof
                            )

                            SwitchPreference(
                                title = stringResource(R.string.settings_graphics_renderer_tool),
                                summary = stringResource(R.string.settings_graphics_renderer_tool_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.DeveloperMode,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(R.string.settings_graphics_renderer_tool),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                checked = uiState.graphicsRendererFeatureEnabled,
                                onCheckedChange = actions.onSetGraphicsRendererFeatureEnabled,
                            )

                            if (uiState.graphicsRendererFeatureEnabled) {
                                ArrowPreference(
                                    title = stringResource(R.string.settings_graphics_renderer),
                                    summary = stringResource(R.string.settings_graphics_renderer_summary),
                                    startAction = {
                                        Icon(
                                            Icons.Rounded.DeveloperMode,
                                            modifier = Modifier.padding(end = 6.dp),
                                            contentDescription = stringResource(R.string.settings_graphics_renderer),
                                            tint = colorScheme.onBackground,
                                        )
                                    },
                                    onClick = actions.onOpenGraphicsRenderer,
                                )
                            }

                            ArrowPreference(
                                title = stringResource(id = R.string.rescue_protection),
                                summary = stringResource(id = R.string.rescue_protection_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Security,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.rescue_protection),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                onClick = actions.onOpenRescueProtection
                            )

                            ArrowPreference(
                                title = stringResource(id = R.string.settings_ai_chat),
                                summary = stringResource(id = R.string.settings_ai_chat_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.AutoFixHigh,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_ai_chat),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                onClick = actions.onOpenAiChat
                            )

                            SwitchPreference(
                                title = stringResource(id = R.string.enable_web_debugging),
                                summary = stringResource(id = R.string.enable_web_debugging_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.DeveloperMode,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.enable_web_debugging),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.enableWebDebugging,
                                onCheckedChange = actions.onSetEnableWebDebugging
                            )
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_auto_jailbreak),
                                summary = stringResource(id = R.string.settings_auto_jailbreak_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.ElectricalServices,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_auto_jailbreak),
                                        tint = if (uiState.isLateLoadMode) colorScheme.onBackground else colorScheme.disabledOnSecondaryVariant
                                    )
                                },
                                enabled = uiState.isLateLoadMode,
                                checked = uiState.autoJailbreak,
                                onCheckedChange = actions.onSetAutoJailbreak
                            )
                        }
                    }

                    CollapsibleMiuixSection(
                        title = stringResource(R.string.settings_section_maintenance),
                        summary = stringResource(R.string.settings_section_maintenance_summary),
                        icon = Icons.Rounded.BugReport,
                        itemCount = uiState.maintenanceSectionItemCount(),
                        expanded = maintenanceExpanded,
                        onExpandedChange = { maintenanceExpanded = it },
                        bottomPadding = 12.dp,
                    ) {
                        if (uiState.isLkmMode) {
                            val uninstall = stringResource(id = R.string.settings_uninstall)
                            ArrowPreference(
                                title = uninstall,
                                enabled = !uiState.isLateLoadMode,
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = uninstall,
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = { showUninstallDialog.value = true },
                            )
                            UninstallDialog(
                                show = showUninstallDialog.value,
                                onDismissRequest = { showUninstallDialog.value = false }
                            )
                        }
                        ArrowPreference(
                            title = stringResource(id = R.string.send_log),
                            startAction = {
                                Icon(
                                    Icons.Rounded.BugReport,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.send_log),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = { showSendLogDialog.value = true },
                        )
                        SendLogDialog(
                            show = showSendLogDialog.value,
                            onDismissRequest = { showSendLogDialog.value = false },
                            loadingDialog = loadingDialog
                        )
                        val about = stringResource(id = R.string.about)
                        ArrowPreference(
                            title = about,
                            startAction = {
                                Icon(
                                    Icons.Rounded.ContactPage,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = about,
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenAbout,
                        )
                    }
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    text: String,
    summary: String,
    icon: ImageVector,
    itemCount: Int,
    topPadding: Dp = 18.dp,
    expanded: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    val expandedState = expanded == true
    val rotation by animateFloatAsState(
        targetValue = if (expandedState) 180f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "settingsSectionArrowRotation",
    )
    val shape = RoundedCornerShape(18.dp)
    val countLabel = if (itemCount == 1) {
        stringResource(R.string.settings_section_item_count_single)
    } else {
        stringResource(R.string.settings_section_item_count, itemCount)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .clip(shape)
            .background(
                color = if (expandedState) {
                    colorScheme.surfaceContainerHigh.copy(alpha = 0.82f)
                } else {
                    colorScheme.surfaceContainer.copy(alpha = 0.58f)
                },
                shape = shape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 14.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = colorScheme.primaryContainer.copy(alpha = if (expandedState) 0.78f else 0.52f),
                    shape = RoundedCornerShape(13.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                color = colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            modifier = Modifier
                .background(
                    color = if (expandedState) {
                        colorScheme.primaryContainer.copy(alpha = 0.72f)
                    } else {
                        colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
                    },
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
            text = countLabel,
            color = if (expandedState) colorScheme.primary else colorScheme.onSurfaceVariantActions,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        if (expanded != null) {
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = text,
                tint = colorScheme.primary,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

@Composable
private fun CollapsibleMiuixSection(
    title: String,
    summary: String,
    icon: ImageVector,
    itemCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    topPadding: Dp = 18.dp,
    bottomPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    SettingsSectionTitle(
        text = title,
        summary = summary,
        icon = icon,
        itemCount = itemCount,
        topPadding = topPadding,
        expanded = expanded,
        onClick = { onExpandedChange(!expanded) },
    )
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)),
        exit = shrinkVertically(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing)),
    ) {
        Card(
            modifier = Modifier
                .padding(top = 6.dp, bottom = bottomPadding)
                .fillMaxWidth()
                .settingsLiquidGlassSurface(),
            colors = snowMiuixCardColors(),
            insideMargin = PaddingValues(top = if (isSnowInterfaceStyle()) 8.dp else 0.dp),
        ) {
            content()
        }
    }
}

private const val UPDATES_ITEM_COUNT = 3
private const val ROOT_FEATURES_ITEM_COUNT = 7
private const val ADVANCED_ITEM_COUNT = 11

private fun SettingsUiState.appearanceSectionItemCount(): Int {
    return 14 + if (uiMode == InterfaceStyle.Snow.value) 1 else 0
}

@Composable
private fun SeasonMiuixPreference(
    selectedValue: String,
    onSelectedIndexChange: (Int) -> Unit,
) {
    val selectedSeason = SeasonStyle.fromValue(selectedValue)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.Palette,
            modifier = Modifier.padding(end = 12.dp).size(24.dp),
            contentDescription = null,
            tint = colorScheme.onBackground,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_season_style),
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(selectedSeason.summaryRes),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp, end = 24.dp, top = 10.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.66f))
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SeasonStyle.entries.forEachIndexed { index, season ->
            val selected = season == selectedSeason
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) colorScheme.primaryContainer.copy(alpha = 0.92f) else Color.Transparent
                    )
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onSelectedIndexChange(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(season.labelRes),
                    color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DayNightMiuixPreference(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Palette,
            modifier = Modifier.padding(end = 12.dp),
            contentDescription = title,
            tint = colorScheme.onBackground,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DayNightSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private fun SettingsUiState.maintenanceSectionItemCount(): Int {
    return if (isLkmMode) 3 else 2
}

@Composable
private fun backgroundSummary(
    hasWallpaper: Boolean,
    hasVideo: Boolean,
): String {
    return stringResource(
        when {
            hasVideo -> R.string.settings_video_background_selected_summary
            hasWallpaper -> R.string.settings_wallpaper_selected_summary
            else -> R.string.settings_background_summary
        }
    )
}

@Composable
private fun Modifier.settingsLiquidGlassSurface(): Modifier {
    return globalLiquidGlassSurface(
        shape = RoundedCornerShape(18.dp),
        surfaceAlpha = 0.58f,
        blurRadius = 10.dp,
        refractionHeight = 14.dp,
        refractionAmount = 9.dp,
        strokeAlpha = 0.66f,
    ).snowMiuixCardSurface(shape = RoundedCornerShape(18.dp))
}
