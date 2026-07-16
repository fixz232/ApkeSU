package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Slider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.GlobalScrollEffect
import me.weishu.kernelsu.ui.component.GlobalSnowEffect
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.MAX_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.component.MIN_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproDivider
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproScreen
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproSectionTitle
import me.weishu.kernelsu.ui.component.skrootpro.skrootproSp
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_STARTUP_SOUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_STARTUP_SOUND_DURATION_SECONDS
import kotlin.math.roundToInt

@Composable
fun SettingPagerSkrootpro(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    var appearanceExpanded by rememberSaveable { mutableStateOf(false) }
    var updatesExpanded by rememberSaveable { mutableStateOf(false) }
    var rootFeaturesExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var maintenanceExpanded by rememberSaveable { mutableStateOf(false) }

    SkrootproScreen(
        title = stringResource(R.string.skrootpro_title),
        bottomInnerPadding = bottomInnerPadding,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            SkrootproSectionTitle(stringResource(R.string.settings_ui_mode))
            SkrootproStylePicker(uiState = uiState, actions = actions)
            SkrootproDivider(modifier = Modifier.padding(vertical = 18.dp))

            CollapsibleSkrootproSection(
                title = stringResource(R.string.settings_section_appearance),
                expanded = appearanceExpanded,
                onExpandedChange = { appearanceExpanded = it },
            ) {
                val dayNightChecked = isDayNightSwitchChecked(uiState.themeMode)
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_day_night_switch),
                    summary = stringResource(R.string.settings_day_night_switch_summary),
                    checked = dayNightChecked,
                    onCheckedChange = actions.onSetDayNightMode,
                    trailingContent = { checked, enabled, onCheckedChange ->
                        DayNightSwitch(
                            checked = checked,
                            enabled = enabled,
                            onCheckedChange = onCheckedChange,
                        )
                    },
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_section_visual_effects),
                    summary = stringResource(R.string.settings_visual_effects_summary),
                    leadingIcon = Icons.Rounded.Visibility,
                    onClick = actions.onOpenVisualEffects,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_ui_decoration_library),
                    summary = stringResource(R.string.settings_ui_decoration_library_summary),
                    leadingIcon = Icons.Rounded.AutoFixHigh,
                    onClick = actions.onOpenUiDecorationLibrary,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_theme),
                    summary = stringResource(R.string.settings_theme_summary),
                    onClick = actions.onOpenTheme,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.theme_store),
                    summary = stringResource(R.string.theme_store_settings_summary),
                    onClick = actions.onOpenThemeStore,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_manager_identity),
                    summary = stringResource(R.string.settings_manager_identity_summary),
                    leadingIcon = Icons.Rounded.Apps,
                    onClick = actions.onOpenLauncherIcon,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_navigation_icons),
                    summary = stringResource(R.string.settings_navigation_icons_summary),
                    leadingIcon = Icons.Rounded.Apps,
                    onClick = actions.onOpenNavigationIcons,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.home_card_wallpapers),
                    summary = stringResource(R.string.home_card_wallpapers_summary),
                    leadingIcon = Icons.Rounded.Wallpaper,
                    onClick = actions.onOpenHomeCardWallpapers,
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_show_home_support_card),
                    summary = stringResource(R.string.settings_show_home_support_card_summary),
                    checked = uiState.showHomeSupportCard,
                    onCheckedChange = actions.onSetShowHomeSupportCard,
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_show_home_learn_card),
                    summary = stringResource(R.string.settings_show_home_learn_card_summary),
                    checked = uiState.showHomeLearnCard,
                    onCheckedChange = actions.onSetShowHomeLearnCard,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_backgrounds),
                    summary = stringResource(R.string.settings_backgrounds_summary),
                    leadingIcon = Icons.Rounded.Wallpaper,
                    onClick = actions.onOpenBackgrounds,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_sound_effects),
                    summary = stringResource(R.string.settings_sound_effects_summary),
                    leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp,
                    onClick = actions.onOpenSoundEffects,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_startup_animation),
                    summary = stringResource(
                        if (uiState.customStartupAnimationUri == null) {
                            R.string.settings_startup_animation_summary
                        } else {
                            R.string.settings_startup_animation_selected_summary
                        }
                    ),
                    leadingIcon = Icons.Rounded.PlayCircle,
                    onClick = actions.onOpenStartupAnimation,
                )
            }

            CollapsibleSkrootproSection(
                title = stringResource(R.string.settings_section_updates),
                expanded = updatesExpanded,
                onExpandedChange = { updatesExpanded = it },
            ) {
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_module_check_update),
                    checked = uiState.checkModuleUpdate,
                    onCheckedChange = actions.onSetCheckModuleUpdate,
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_version_mismatch_warning),
                    summary = stringResource(R.string.settings_version_mismatch_warning_summary),
                    checked = uiState.showVersionMismatchWarning,
                    onCheckedChange = actions.onSetShowVersionMismatchWarning,
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_gki_warning),
                    summary = stringResource(R.string.settings_gki_warning_summary),
                    checked = uiState.showGkiWarning,
                    onCheckedChange = actions.onSetShowGkiWarning,
                )
            }

            CollapsibleSkrootproSection(
                title = stringResource(R.string.settings_section_root_features),
                expanded = rootFeaturesExpanded,
                onExpandedChange = { rootFeaturesExpanded = it },
            ) {
                val suCompatOptions = listOf(
                    stringResource(R.string.settings_mode_enable_by_default),
                    stringResource(R.string.settings_mode_disable_until_reboot),
                    stringResource(R.string.settings_mode_disable_always),
                )
                var suCompatExpanded by remember { mutableStateOf(false) }
                val suCompatSummary = when (uiState.suCompatStatus) {
                    "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
                    "managed" -> stringResource(R.string.feature_status_managed_summary)
                    else -> suCompatOptions.getOrElse(uiState.suCompatMode) { suCompatOptions.first() }
                }
                Box {
                    SkrootproActionRow(
                        title = stringResource(R.string.settings_sucompat),
                        summary = suCompatSummary,
                        leadingIcon = Icons.Rounded.RemoveModerator,
                        onClick = {
                            if (uiState.suCompatStatus == "supported") suCompatExpanded = true
                        },
                    )
                    DropdownMenu(
                        expanded = suCompatExpanded,
                        onDismissRequest = { suCompatExpanded = false },
                    ) {
                        suCompatOptions.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                leadingIcon = {
                                    if (uiState.suCompatMode == index) {
                                        androidx.compose.material3.Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                        )
                                    }
                                },
                                onClick = {
                                    suCompatExpanded = false
                                    actions.onSetSuCompatMode(index)
                                },
                            )
                        }
                    }
                }
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_kernel_umount),
                    checked = uiState.isKernelUmountEnabled,
                    onCheckedChange = actions.onSetKernelUmountEnabled,
                    enabled = uiState.kernelUmountStatus == "supported",
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_selinux_hide),
                    checked = uiState.isSelinuxHideEnabled,
                    onCheckedChange = actions.onSetSelinuxHideEnabled,
                    enabled = uiState.selinuxHideStatus == "supported",
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_sulog),
                    checked = uiState.isSulogEnabled,
                    onCheckedChange = actions.onSetSulogEnabled,
                    enabled = uiState.sulogStatus == "supported",
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_adb_root),
                    checked = uiState.isAdbRootEnabled,
                    onCheckedChange = actions.onSetAdbRootEnabled,
                    enabled = uiState.adbRootStatus == "supported",
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_avc_spoof),
                    checked = uiState.isAvcSpoofEnabled,
                    onCheckedChange = actions.onSetAvcSpoofEnabled,
                    enabled = uiState.avcSpoofStatus == "supported",
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_epkesu_hide),
                    checked = uiState.isEpkesuHideEnabled,
                    onCheckedChange = actions.onSetEpkesuHideEnabled,
                )
            }

            CollapsibleSkrootproSection(
                title = stringResource(R.string.settings_section_advanced),
                expanded = advancedExpanded,
                onExpandedChange = { advancedExpanded = it },
            ) {
                SkrootproActionRow(
                    title = stringResource(R.string.settings_profile_template),
                    summary = stringResource(R.string.settings_profile_template_summary),
                    onClick = actions.onOpenProfileTemplate,
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_umount_modules_default),
                    checked = uiState.isDefaultUmountModules,
                    onCheckedChange = actions.onSetDefaultUmountModules,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_builtin_mount),
                    summary = uiState.builtinMountConflict?.let {
                        stringResource(R.string.settings_builtin_mount_conflict_summary, it)
                    } ?: stringResource(R.string.settings_builtin_mount_summary),
                    leadingIcon = Icons.Rounded.Apps,
                    onClick = actions.onOpenBuiltinMount,
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_kpatch_next),
                    summary = kPatchNextSummary(uiState),
                    checked = uiState.isKPatchNextEnabled,
                    onCheckedChange = actions.onSetKPatchNextEnabled,
                    enabled = uiState.canToggleKPatchNext,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_kpatch_next_webui),
                    summary = stringResource(
                        if (uiState.canOpenKPatchNextWebUi) {
                            R.string.settings_kpatch_next_webui_summary
                        } else {
                            R.string.settings_kpatch_next_webui_disabled_summary
                        }
                    ),
                    leadingIcon = Icons.Rounded.Apps,
                    onClick = {
                        if (uiState.canOpenKPatchNextWebUi) {
                            actions.onOpenKPatchNextWebUi()
                        }
                    },
                )
                SkrootproActionRow(
                    title = stringResource(R.string.hidden_path_config),
                    summary = stringResource(R.string.hidden_path_config_summary),
                    leadingIcon = Icons.Rounded.Visibility,
                    onClick = actions.onOpenHiddenPathConfig,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_cpu_spoof),
                    summary = stringResource(R.string.settings_cpu_spoof_summary),
                    leadingIcon = Icons.Rounded.DeveloperMode,
                    onClick = actions.onOpenCpuSpoof,
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_graphics_renderer_tool),
                    summary = stringResource(R.string.settings_graphics_renderer_tool_summary),
                    checked = uiState.graphicsRendererFeatureEnabled,
                    onCheckedChange = actions.onSetGraphicsRendererFeatureEnabled,
                )
                if (uiState.graphicsRendererFeatureEnabled) {
                    SkrootproActionRow(
                        title = stringResource(R.string.settings_graphics_renderer),
                        summary = stringResource(R.string.settings_graphics_renderer_summary),
                        leadingIcon = Icons.Rounded.DeveloperMode,
                        onClick = actions.onOpenGraphicsRenderer,
                    )
                }
                SkrootproActionRow(
                    title = stringResource(R.string.rescue_protection),
                    summary = stringResource(R.string.rescue_protection_summary),
                    leadingIcon = Icons.Rounded.Security,
                    onClick = actions.onOpenRescueProtection,
                )
                SkrootproActionRow(
                    title = stringResource(R.string.settings_ai_chat),
                    summary = stringResource(R.string.settings_ai_chat_summary),
                    leadingIcon = Icons.Rounded.AutoFixHigh,
                    onClick = actions.onOpenAiChat,
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.enable_web_debugging),
                    checked = uiState.enableWebDebugging,
                    onCheckedChange = actions.onSetEnableWebDebugging,
                )
                SkrootproSwitchRow(
                    title = stringResource(R.string.settings_auto_jailbreak),
                    checked = uiState.autoJailbreak,
                    onCheckedChange = actions.onSetAutoJailbreak,
                    enabled = uiState.isLateLoadMode,
                )
            }

            CollapsibleSkrootproSection(
                title = stringResource(R.string.settings_section_maintenance),
                expanded = maintenanceExpanded,
                onExpandedChange = { maintenanceExpanded = it },
            ) {
                SkrootproActionRow(
                    title = stringResource(R.string.about),
                    summary = "",
                    onClick = actions.onOpenAbout,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SkrootproStylePicker(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(SkrootproColors.BarSurface, CircleShape)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        InterfaceStyle.entries.forEachIndexed { index, style ->
            val selected = InterfaceStyle.selectedIndex(uiState.uiMode) == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(
                        color = if (selected) SkrootproColors.Purple else Color.Transparent,
                        shape = CircleShape,
                    )
                    .clickable { actions.onSetUiModeIndex(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(style.labelRes),
                    color = if (selected) Color.White else SkrootproColors.Muted,
                    fontSize = skrootproSp(14f, maxScale = 1.04f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SkrootproSwitchStylePicker(
    selectedStyle: String,
    onStyleSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_switch_style),
            color = SkrootproColors.Text,
            fontSize = skrootproSp(15f, maxScale = 1.04f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(SkrootproColors.BarSurface, CircleShape)
                .horizontalScroll(rememberScrollState())
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SwitchStyle.entries.forEachIndexed { index, style ->
                val selected = SwitchStyle.fromValue(selectedStyle) == style
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .background(
                            color = if (selected) SkrootproColors.Purple else Color.Transparent,
                            shape = CircleShape,
                        )
                        .clickable { onStyleSelected(index) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(style.labelRes),
                        color = if (selected) Color.White else SkrootproColors.Muted,
                        fontSize = skrootproSp(13f, maxScale = 1.0f),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SkrootproSnowEffectPicker(
    selectedEffect: String,
    onEffectSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_global_snow_effect),
            color = SkrootproColors.Text,
            fontSize = skrootproSp(15f, maxScale = 1.04f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(SkrootproColors.BarSurface, CircleShape)
                .horizontalScroll(rememberScrollState())
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            GlobalSnowEffect.entries.forEachIndexed { index, effect ->
                val selected = GlobalSnowEffect.fromValue(selectedEffect) == effect
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .background(
                            color = if (selected) SkrootproColors.Purple else Color.Transparent,
                            shape = CircleShape,
                        )
                        .clickable { onEffectSelected(index) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(effect.labelRes),
                        color = if (selected) Color.White else SkrootproColors.Muted,
                        fontSize = skrootproSp(13f, maxScale = 1.0f),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SkrootproNightBackgroundEffectPicker(
    selectedEffect: String,
    onEffectSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_night_background_effect),
            color = SkrootproColors.Text,
            fontSize = skrootproSp(15f, maxScale = 1.04f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(SkrootproColors.BarSurface, CircleShape)
                .horizontalScroll(rememberScrollState())
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NightBackgroundEffect.entries.forEachIndexed { index, effect ->
                val selected = NightBackgroundEffect.fromValue(selectedEffect) == effect
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .background(
                            color = if (selected) SkrootproColors.Purple else Color.Transparent,
                            shape = CircleShape,
                        )
                        .clickable { onEffectSelected(index) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(effect.labelRes),
                        color = if (selected) Color.White else SkrootproColors.Muted,
                        fontSize = skrootproSp(13f, maxScale = 1.0f),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SkrootproScrollEffectPicker(
    selectedEffect: String,
    onEffectSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_scroll_animation_effect),
            color = SkrootproColors.Text,
            fontSize = skrootproSp(15f, maxScale = 1.04f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(SkrootproColors.BarSurface, CircleShape)
                .horizontalScroll(rememberScrollState())
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            GlobalScrollEffect.entries.forEachIndexed { index, effect ->
                val selected = GlobalScrollEffect.fromValue(selectedEffect) == effect
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .background(
                            color = if (selected) SkrootproColors.Purple else Color.Transparent,
                            shape = CircleShape,
                        )
                        .clickable { onEffectSelected(index) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(effect.labelRes),
                        color = if (selected) Color.White else SkrootproColors.Muted,
                        fontSize = skrootproSp(13f, maxScale = 1.0f),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSkrootproSection(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                color = SkrootproColors.Text,
                fontSize = skrootproSp(13f, maxScale = 1.0f),
                lineHeight = skrootproSp(16f, maxScale = 1.0f),
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = title,
                tint = SkrootproColors.Muted,
                modifier = Modifier.size(20.dp),
            )
        }
        if (expanded) {
            content()
        }
    }
}

@Composable
private fun SkrootproDurationSliderRow(
    title: String,
    value: Int,
    valueRange: IntRange = MIN_CUSTOM_STARTUP_SOUND_DURATION_SECONDS..MAX_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
    valueLabelRes: Int = R.string.settings_startup_sound_duration_value,
    onValueChange: (Int) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val currentSeconds = sliderValue.roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SkrootproColors.BarSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = SkrootproColors.Text,
            )
            Text(
                text = title,
                color = SkrootproColors.Text,
                fontSize = skrootproSp(16f, maxScale = 1.05f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(valueLabelRes, currentSeconds),
                color = SkrootproColors.Muted,
                fontSize = skrootproSp(13f, maxScale = 1.05f),
                fontWeight = FontWeight.Medium,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it.roundToInt())
            },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first - 1).coerceAtLeast(0),
        )
    }
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
private fun SkrootproSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = MIN_CUSTOM_WALLPAPER_OPACITY..MAX_CUSTOM_WALLPAPER_OPACITY,
    onValueChange: (Float) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SkrootproColors.BarSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = SkrootproColors.Text,
                fontSize = skrootproSp(16f, maxScale = 1.05f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${(sliderValue * 100).roundToInt()}%",
                color = SkrootproColors.Muted,
                fontSize = skrootproSp(13f, maxScale = 1.05f),
                fontWeight = FontWeight.Medium,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
            },
            valueRange = valueRange,
        )
    }
}

@Composable
private fun SkrootproActionRow(
    title: String,
    summary: String,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SkrootproColors.BarSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leadingIcon != null) {
                androidx.compose.material3.Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = SkrootproColors.Text,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SkrootproColors.Text,
                    fontSize = skrootproSp(16f, maxScale = 1.05f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        color = SkrootproColors.Muted,
                        fontSize = skrootproSp(12.5f, maxScale = 1.05f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        androidx.compose.material3.Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = SkrootproColors.Muted,
        )
    }
}

@Composable
private fun SkrootproSwitchRow(
    title: String,
    summary: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    trailingContent: (@Composable (Boolean, Boolean, (Boolean) -> Unit) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SkrootproColors.BarSurface)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) SkrootproColors.Text else SkrootproColors.DisabledText,
                fontSize = skrootproSp(16f, maxScale = 1.05f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    color = SkrootproColors.Muted,
                    fontSize = skrootproSp(12.5f, maxScale = 1.05f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingContent != null) {
            trailingContent(checked, enabled, onCheckedChange)
        } else {
            val switchStyle = LocalSwitchStyle.current
            if (switchStyle == SwitchStyle.Original) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                )
            } else {
                StyledSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                    style = switchStyle,
                )
            }
        }
    }
}
