package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
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
import me.weishu.kernelsu.ui.component.MAX_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.component.MIN_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.alpha.AlphaCard
import me.weishu.kernelsu.ui.component.alpha.AlphaColors
import me.weishu.kernelsu.ui.component.alpha.AlphaShapes
import me.weishu.kernelsu.ui.component.alpha.AlphaScreen
import me.weishu.kernelsu.ui.component.alpha.AlphaSwitch
import me.weishu.kernelsu.ui.component.alpha.alphaSp
import me.weishu.kernelsu.ui.util.BUILTIN_MOUNT_MODE_MAGIC
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_STARTUP_SOUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_STARTUP_SOUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import kotlin.math.roundToInt

@Composable
fun SettingPagerAlpha(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    AlphaScreen(
        title = stringResource(R.string.settings),
        bottomInnerPadding = bottomInnerPadding,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    top = 18.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AlphaSection(title = stringResource(R.string.settings_ui_mode)) {
                AlphaStylePicker(uiState = uiState, actions = actions)
            }

            AlphaSection(
                title = stringResource(R.string.settings_section_appearance),
                collapsible = true,
            ) {
                val dayNightChecked = isDayNightSwitchChecked(uiState.themeMode)
                AlphaSwitchRow(
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
                AlphaActionRow(
                    title = stringResource(R.string.settings_section_visual_effects),
                    summary = stringResource(R.string.settings_visual_effects_summary),
                    icon = Icons.Rounded.Visibility,
                    onClick = actions.onOpenVisualEffects,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_theme),
                    summary = stringResource(R.string.settings_theme_summary),
                    icon = Icons.Rounded.Palette,
                    onClick = actions.onOpenTheme,
                )
                AlphaActionRow(
                    title = stringResource(R.string.theme_store),
                    summary = stringResource(R.string.theme_store_settings_summary),
                    icon = Icons.Rounded.Storefront,
                    onClick = actions.onOpenThemeStore,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_manager_identity),
                    summary = stringResource(R.string.settings_manager_identity_summary),
                    icon = Icons.Rounded.Apps,
                    onClick = actions.onOpenLauncherIcon,
                )
                AlphaActionRow(
                    title = stringResource(R.string.home_card_wallpapers),
                    summary = stringResource(R.string.home_card_wallpapers_summary),
                    icon = Icons.Rounded.Wallpaper,
                    onClick = actions.onOpenHomeCardWallpapers,
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_show_home_support_card),
                    summary = stringResource(R.string.settings_show_home_support_card_summary),
                    checked = uiState.showHomeSupportCard,
                    onCheckedChange = actions.onSetShowHomeSupportCard,
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_show_home_learn_card),
                    summary = stringResource(R.string.settings_show_home_learn_card_summary),
                    checked = uiState.showHomeLearnCard,
                    onCheckedChange = actions.onSetShowHomeLearnCard,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_backgrounds),
                    summary = stringResource(R.string.settings_backgrounds_summary),
                    icon = Icons.Rounded.Wallpaper,
                    onClick = actions.onOpenBackgrounds,
                )
            }

            AlphaSection(title = stringResource(R.string.alpha_startup_media)) {
                AlphaActionRow(
                    title = stringResource(R.string.settings_sound_effects),
                    summary = stringResource(R.string.settings_sound_effects_summary),
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    onClick = actions.onOpenSoundEffects,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_startup_animation),
                    summary = stringResource(
                        if (uiState.customStartupAnimationUri == null) {
                            R.string.settings_startup_animation_summary
                        } else {
                            R.string.settings_startup_animation_selected_summary
                        }
                    ),
                    icon = Icons.Rounded.PlayCircle,
                    onClick = actions.onOpenStartupAnimation,
                )
            }

            AlphaSection(
                title = stringResource(R.string.settings_section_updates),
                collapsible = true,
            ) {
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_module_check_update),
                    summary = stringResource(R.string.settings_module_check_update_summary),
                    checked = uiState.checkModuleUpdate,
                    onCheckedChange = actions.onSetCheckModuleUpdate,
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_version_mismatch_warning),
                    summary = stringResource(R.string.settings_version_mismatch_warning_summary),
                    checked = uiState.showVersionMismatchWarning,
                    onCheckedChange = actions.onSetShowVersionMismatchWarning,
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_gki_warning),
                    summary = stringResource(R.string.settings_gki_warning_summary),
                    checked = uiState.showGkiWarning,
                    onCheckedChange = actions.onSetShowGkiWarning,
                )
            }

            AlphaSection(
                title = stringResource(R.string.settings_section_root_features),
                collapsible = true,
            ) {
                AlphaFeatureRows(uiState = uiState, actions = actions)
            }

            AlphaSection(
                title = stringResource(R.string.settings_section_advanced),
                collapsible = true,
            ) {
                AlphaActionRow(
                    title = stringResource(R.string.settings_profile_template),
                    summary = stringResource(R.string.settings_profile_template_summary),
                    onClick = actions.onOpenProfileTemplate,
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_umount_modules_default),
                    summary = stringResource(R.string.settings_umount_modules_default_summary),
                    checked = uiState.isDefaultUmountModules,
                    onCheckedChange = actions.onSetDefaultUmountModules,
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_builtin_mount),
                    summary = uiState.builtinMountConflict?.let {
                        stringResource(R.string.settings_builtin_mount_conflict_summary, it)
                    } ?: stringResource(R.string.settings_builtin_mount_summary),
                    checked = uiState.isBuiltinMountEnabled,
                    onCheckedChange = actions.onSetBuiltinMountEnabled,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_builtin_mount_default_mode),
                    summary = stringResource(
                        if (uiState.builtinMountDefaultMode == BUILTIN_MOUNT_MODE_MAGIC) {
                            R.string.settings_builtin_mount_mode_magic
                        } else {
                            R.string.settings_builtin_mount_mode_overlay
                        }
                    ),
                    icon = Icons.Rounded.Apps,
                    onClick = {
                        actions.onSetBuiltinMountDefaultMode(
                            if (uiState.builtinMountDefaultMode == BUILTIN_MOUNT_MODE_MAGIC) 0 else 1
                        )
                    },
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_builtin_mount_webui),
                    summary = stringResource(
                        if (uiState.isBuiltinMountEnabled && uiState.isBuiltinMountWebUiAvailable) {
                            R.string.settings_builtin_mount_webui_summary
                        } else {
                            R.string.settings_builtin_mount_webui_disabled_summary
                        }
                    ),
                    icon = Icons.Rounded.DeveloperMode,
                    onClick = {
                        if (uiState.isBuiltinMountEnabled && uiState.isBuiltinMountWebUiAvailable) {
                            actions.onOpenBuiltinMountWebUi()
                        }
                    },
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_kpatch_next),
                    summary = kPatchNextSummary(uiState),
                    checked = uiState.isKPatchNextEnabled,
                    enabled = uiState.canToggleKPatchNext,
                    onCheckedChange = actions.onSetKPatchNextEnabled,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_kpatch_next_webui),
                    summary = stringResource(
                        if (uiState.canOpenKPatchNextWebUi) {
                            R.string.settings_kpatch_next_webui_summary
                        } else {
                            R.string.settings_kpatch_next_webui_disabled_summary
                        }
                    ),
                    icon = Icons.Rounded.Apps,
                    onClick = {
                        if (uiState.canOpenKPatchNextWebUi) {
                            actions.onOpenKPatchNextWebUi()
                        }
                    },
                )
                AlphaActionRow(
                    title = stringResource(R.string.hidden_path_config),
                    summary = stringResource(R.string.hidden_path_config_summary),
                    icon = Icons.Rounded.Visibility,
                    onClick = actions.onOpenHiddenPathConfig,
                )
                AlphaActionRow(
                    title = stringResource(R.string.rescue_protection),
                    summary = stringResource(R.string.rescue_protection_summary),
                    icon = Icons.Rounded.Security,
                    onClick = actions.onOpenRescueProtection,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_ai_chat),
                    summary = stringResource(R.string.settings_ai_chat_summary),
                    icon = Icons.Rounded.AutoFixHigh,
                    onClick = actions.onOpenAiChat,
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.enable_web_debugging),
                    summary = stringResource(R.string.enable_web_debugging_summary),
                    checked = uiState.enableWebDebugging,
                    onCheckedChange = actions.onSetEnableWebDebugging,
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_auto_jailbreak),
                    summary = stringResource(R.string.settings_auto_jailbreak_summary),
                    checked = uiState.autoJailbreak,
                    enabled = uiState.isLateLoadMode,
                    onCheckedChange = actions.onSetAutoJailbreak,
                )
            }

            AlphaSection(
                title = stringResource(R.string.settings_section_maintenance),
                collapsible = true,
            ) {
                AlphaActionRow(
                    title = stringResource(R.string.about),
                    summary = "",
                    onClick = actions.onOpenAbout,
                )
            }
        }
    }
}

@Composable
private fun AlphaFeatureRows(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
) {
    AlphaSwitchRow(
        title = stringResource(R.string.settings_kernel_umount),
        summary = alphaFeatureSummary(uiState.kernelUmountStatus, R.string.settings_kernel_umount_summary),
        checked = uiState.isKernelUmountEnabled,
        enabled = uiState.kernelUmountStatus == "supported",
        onCheckedChange = actions.onSetKernelUmountEnabled,
    )
    AlphaSwitchRow(
        title = stringResource(R.string.settings_selinux_hide),
        summary = alphaFeatureSummary(uiState.selinuxHideStatus, R.string.settings_selinux_hide_summary),
        checked = uiState.isSelinuxHideEnabled,
        enabled = uiState.selinuxHideStatus == "supported",
        onCheckedChange = actions.onSetSelinuxHideEnabled,
    )
    AlphaSwitchRow(
        title = stringResource(R.string.settings_sulog),
        summary = alphaFeatureSummary(uiState.sulogStatus, R.string.settings_sulog_summary),
        checked = uiState.isSulogEnabled,
        enabled = uiState.sulogStatus == "supported",
        onCheckedChange = actions.onSetSulogEnabled,
    )
    AlphaSwitchRow(
        title = stringResource(R.string.settings_adb_root),
        summary = alphaFeatureSummary(uiState.adbRootStatus, R.string.settings_adb_root_summary),
        checked = uiState.isAdbRootEnabled,
        enabled = uiState.adbRootStatus == "supported",
        onCheckedChange = actions.onSetAdbRootEnabled,
    )
    AlphaSwitchRow(
        title = stringResource(R.string.settings_avc_spoof),
        summary = alphaFeatureSummary(uiState.avcSpoofStatus, R.string.settings_avc_spoof_summary),
        checked = uiState.isAvcSpoofEnabled,
        enabled = uiState.avcSpoofStatus == "supported",
        onCheckedChange = actions.onSetAvcSpoofEnabled,
    )
    AlphaSwitchRow(
        title = stringResource(R.string.settings_epkesu_hide),
        summary = stringResource(R.string.settings_epkesu_hide_summary),
        checked = uiState.isEpkesuHideEnabled,
        onCheckedChange = actions.onSetEpkesuHideEnabled,
    )
}

@Composable
private fun AlphaInlineSectionLabel(text: String) {
    Text(
        text = text,
        color = AlphaColors.Accent,
        fontSize = alphaSp(12.5f),
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun alphaFeatureSummary(status: String, defaultSummary: Int): String {
    return when (status) {
        "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
        "managed" -> stringResource(R.string.feature_status_managed_summary)
        else -> stringResource(defaultSummary)
    }
}

@Composable
private fun AlphaSection(
    title: String,
    collapsible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(!collapsible) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (collapsible) Modifier.clickable { expanded = !expanded } else Modifier)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                color = AlphaColors.Text,
                fontSize = alphaSp(20f, maxScale = 1.04f),
                lineHeight = alphaSp(24f, maxScale = 1.04f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (collapsible) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = title,
                    tint = AlphaColors.Muted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (expanded) {
            AlphaCard(contentPadding = PaddingValues(0.dp)) {
                Column(content = content)
            }
        }
    }
}

@Composable
private fun AlphaStylePicker(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        InterfaceStyle.entries.forEachIndexed { index, style ->
            val selected = InterfaceStyle.selectedIndex(uiState.uiMode) == index
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(AlphaShapes.Control)
                    .background(if (selected) AlphaColors.Accent else AlphaColors.SurfaceStrong)
                    .clickable { actions.onSetUiModeIndex(index) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(style.labelRes),
                    color = if (selected) Color.White else AlphaColors.Muted,
                    fontSize = alphaSp(13f, maxScale = 1.0f),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AlphaSwitchStylePicker(
    selectedStyle: String,
    onStyleSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_switch_style),
            color = AlphaColors.Text,
            fontSize = alphaSp(15f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SwitchStyle.entries.forEachIndexed { index, style ->
                val selected = SwitchStyle.fromValue(selectedStyle) == style
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(AlphaShapes.Control)
                        .background(if (selected) AlphaColors.Accent else AlphaColors.SurfaceStrong)
                        .clickable { onStyleSelected(index) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(style.labelRes),
                        color = if (selected) Color.White else AlphaColors.Muted,
                        fontSize = alphaSp(13f, maxScale = 1.0f),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphaSnowEffectPicker(
    selectedEffect: String,
    onEffectSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_global_snow_effect),
            color = AlphaColors.Text,
            fontSize = alphaSp(15f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GlobalSnowEffect.entries.forEachIndexed { index, effect ->
                val selected = GlobalSnowEffect.fromValue(selectedEffect) == effect
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(AlphaShapes.Control)
                        .background(if (selected) AlphaColors.Accent else AlphaColors.SurfaceStrong)
                        .clickable { onEffectSelected(index) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(effect.labelRes),
                        color = if (selected) Color.White else AlphaColors.Muted,
                        fontSize = alphaSp(13f, maxScale = 1.0f),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphaNightBackgroundEffectPicker(
    selectedEffect: String,
    onEffectSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_night_background_effect),
            color = AlphaColors.Text,
            fontSize = alphaSp(15f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NightBackgroundEffect.entries.forEachIndexed { index, effect ->
                val selected = NightBackgroundEffect.fromValue(selectedEffect) == effect
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(AlphaShapes.Control)
                        .background(if (selected) AlphaColors.Accent else AlphaColors.SurfaceStrong)
                        .clickable { onEffectSelected(index) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(effect.labelRes),
                        color = if (selected) Color.White else AlphaColors.Muted,
                        fontSize = alphaSp(13f, maxScale = 1.0f),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphaScrollEffectPicker(
    selectedEffect: String,
    onEffectSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_scroll_animation_effect),
            color = AlphaColors.Text,
            fontSize = alphaSp(15f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GlobalScrollEffect.entries.forEachIndexed { index, effect ->
                val selected = GlobalScrollEffect.fromValue(selectedEffect) == effect
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(AlphaShapes.Control)
                        .background(if (selected) AlphaColors.Accent else AlphaColors.SurfaceStrong)
                        .clickable { onEffectSelected(index) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(effect.labelRes),
                        color = if (selected) Color.White else AlphaColors.Muted,
                        fontSize = alphaSp(13f, maxScale = 1.0f),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphaActionRow(
    title: String,
    summary: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AlphaColors.Accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AlphaColors.Text,
                fontSize = alphaSp(15f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    color = AlphaColors.Muted,
                    fontSize = alphaSp(12f),
                    lineHeight = alphaSp(15f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AlphaSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    trailingContent: (@Composable (Boolean, Boolean, (Boolean) -> Unit) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) AlphaColors.Text else AlphaColors.Disabled,
                fontSize = alphaSp(15f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    color = AlphaColors.Muted,
                    fontSize = alphaSp(12f),
                    lineHeight = alphaSp(15f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingContent != null) {
            trailingContent(checked, enabled, onCheckedChange)
        } else {
            AlphaSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun AlphaSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: @Composable (Float) -> String,
    onValueChange: (Float) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = AlphaColors.Text,
                fontSize = alphaSp(15f),
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = valueLabel(sliderValue),
                color = AlphaColors.Muted,
                fontSize = alphaSp(12f),
                fontWeight = FontWeight.Bold,
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
private fun AlphaDurationRow(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    AlphaSliderRow(
        title = stringResource(R.string.settings_startup_sound_duration),
        value = value.toFloat(),
        valueRange = MIN_CUSTOM_STARTUP_SOUND_DURATION_SECONDS.toFloat()..
            MAX_CUSTOM_STARTUP_SOUND_DURATION_SECONDS.toFloat(),
        valueLabel = { stringResource(R.string.settings_startup_sound_duration_value, it.roundToInt()) },
        onValueChange = { onValueChange(it.roundToInt()) },
    )
}

@Composable
private fun AlphaVideoDurationRow(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    AlphaSliderRow(
        title = stringResource(R.string.settings_video_background_duration),
        value = value.toFloat(),
        valueRange = MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS.toFloat()..
            MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS.toFloat(),
        valueLabel = { stringResource(R.string.settings_video_background_duration_value, it.roundToInt()) },
        onValueChange = { onValueChange(it.roundToInt()) },
    )
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
