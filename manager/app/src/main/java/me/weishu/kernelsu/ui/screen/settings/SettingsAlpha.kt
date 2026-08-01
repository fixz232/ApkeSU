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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import me.weishu.kernelsu.ui.component.alpha.alphaStrongWeight
import me.weishu.kernelsu.ui.component.alpha.alphaSp
import me.weishu.kernelsu.ui.component.alpha.isSnowStyle
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
    val snowStyle = isSnowStyle()
    AlphaScreen(
        title = stringResource(R.string.settings),
        bottomInnerPadding = bottomInnerPadding,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = if (snowStyle) 14.dp else 16.dp,
                    top = if (snowStyle) 12.dp else 18.dp,
                    end = if (snowStyle) 14.dp else 16.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(if (snowStyle) 10.dp else 14.dp),
        ) {
            AlphaSection(
                title = stringResource(R.string.settings_hub_appearance),
                icon = Icons.Rounded.Palette,
                collapsible = true,
            ) {
                AlphaDeltaModePicker(
                    deltaSelected = uiState.uiMode == InterfaceStyle.Delta.value,
                    onModeSelected = actions.onSetAlphaDeltaMode,
                )
                AlphaStylePicker(uiState = uiState, actions = actions)
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
                    title = stringResource(R.string.theme_store),
                    summary = stringResource(R.string.theme_store_settings_summary),
                    icon = Icons.Rounded.Storefront,
                    onClick = actions.onOpenThemeStore,
                )
            }

            AlphaSection(
                title = stringResource(R.string.settings_hub_home_manager),
                icon = Icons.Rounded.Apps,
                collapsible = true,
            ) {
                AlphaActionRow(
                    title = stringResource(R.string.settings_manager_identity),
                    summary = stringResource(R.string.settings_manager_identity_summary),
                    icon = Icons.Rounded.Apps,
                    onClick = actions.onOpenLauncherIcon,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_home_title),
                    summary = if (uiState.customHomeTitle.isBlank()) {
                        stringResource(R.string.settings_home_title_default_summary)
                    } else {
                        stringResource(R.string.settings_home_title_custom_summary, uiState.customHomeTitle)
                    },
                    icon = Icons.Rounded.EditNote,
                    onClick = actions.onEditHomeTitle,
                )
                AlphaActionRow(
                    title = stringResource(R.string.home_layout_title),
                    summary = stringResource(R.string.home_layout_settings_summary),
                    icon = Icons.Rounded.Apps,
                    onClick = actions.onOpenHomeLayout,
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
            }

            AlphaSection(
                title = stringResource(R.string.settings_hub_root_permissions),
                icon = Icons.Rounded.Security,
                collapsible = true,
            ) {
                AlphaActionRow(
                    title = stringResource(R.string.settings_profile_template),
                    summary = stringResource(R.string.settings_profile_template_summary),
                    onClick = actions.onOpenProfileTemplate,
                )
                AlphaFeatureRows(uiState = uiState, actions = actions)
            }

            AlphaSection(
                title = stringResource(R.string.settings_hub_mount_hide),
                icon = Icons.Rounded.RemoveModerator,
                collapsible = true,
            ) {
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_umount_modules_default),
                    summary = stringResource(R.string.settings_umount_modules_default_summary),
                    checked = uiState.isDefaultUmountModules,
                    onCheckedChange = actions.onSetDefaultUmountModules,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_builtin_mount),
                    summary = uiState.builtinMountConflict?.let {
                        stringResource(R.string.settings_builtin_mount_conflict_summary, it)
                    } ?: stringResource(R.string.settings_builtin_mount_summary),
                    icon = Icons.Rounded.Apps,
                    onClick = actions.onOpenBuiltinMount,
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_kpatch_next),
                    summary = kPatchNextSummary(uiState),
                    checked = uiState.isKPatchNextSwitchChecked,
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
                    enabled = uiState.canOpenKPatchNextWebUi,
                    onClick = {
                        if (uiState.canOpenKPatchNextWebUi) {
                            actions.onOpenKPatchNextWebUi()
                        }
                    },
                )
                AlphaActionRow(
                    title = pathConfigTitle(uiState),
                    summary = pathConfigSummary(uiState),
                    icon = Icons.Rounded.Visibility,
                    enabled = uiState.canOpenPathConfig,
                    onClick = actions.onOpenHiddenPathConfig,
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_epkesu_hide),
                    summary = stringResource(R.string.settings_epkesu_hide_summary),
                    checked = uiState.isEpkesuHideEnabled,
                    onCheckedChange = actions.onSetEpkesuHideEnabled,
                )
            }

            AlphaSection(
                title = stringResource(R.string.settings_hub_toolbox),
                icon = Icons.Rounded.DeveloperMode,
                collapsible = true,
            ) {
                AlphaActionRow(
                    title = stringResource(R.string.rescue_protection),
                    summary = stringResource(R.string.rescue_protection_summary),
                    icon = Icons.Rounded.Security,
                    onClick = actions.onOpenRescueProtection,
                )
                AlphaActionRow(
                    title = stringResource(R.string.image_tool_title),
                    summary = stringResource(R.string.image_tool_settings_summary),
                    icon = Icons.Rounded.ImageSearch,
                    onClick = actions.onOpenImageTool,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_cpu_spoof),
                    summary = stringResource(R.string.settings_cpu_spoof_summary),
                    icon = Icons.Rounded.DeveloperMode,
                    onClick = actions.onOpenCpuSpoof,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_device_identity),
                    summary = stringResource(R.string.settings_device_identity_summary),
                    icon = Icons.Rounded.Badge,
                    onClick = actions.onOpenDeviceIdentity,
                )
                AlphaActionRow(
                    title = stringResource(R.string.settings_ai_chat),
                    summary = stringResource(R.string.settings_ai_chat_summary),
                    icon = Icons.Rounded.AutoFixHigh,
                    onClick = actions.onOpenAiChat,
                )
                AlphaSwitchRow(
                    title = stringResource(R.string.settings_graphics_renderer_tool),
                    summary = stringResource(R.string.settings_graphics_renderer_tool_summary),
                    checked = uiState.graphicsRendererFeatureEnabled,
                    onCheckedChange = actions.onSetGraphicsRendererFeatureEnabled,
                )
                if (uiState.graphicsRendererFeatureEnabled) {
                    AlphaActionRow(
                        title = stringResource(R.string.settings_graphics_renderer),
                        summary = stringResource(R.string.settings_graphics_renderer_summary),
                        icon = Icons.Rounded.DeveloperMode,
                        onClick = actions.onOpenGraphicsRenderer,
                    )
                }
            }

            AlphaSection(
                title = stringResource(R.string.settings_hub_app_maintenance),
                icon = Icons.Rounded.Info,
                collapsible = true,
            ) {
                AlphaActionRow(
                    title = stringResource(R.string.settings_language),
                    summary = stringResource(R.string.settings_language_summary),
                    icon = Icons.Rounded.Language,
                    onClick = actions.onOpenLanguage,
                )
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
        AlphaActionRow(
            title = stringResource(R.string.settings_sucompat),
            summary = suCompatSummary,
            icon = Icons.Rounded.RemoveModerator,
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
                            Icon(Icons.Rounded.Check, contentDescription = null)
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
}

@Composable
private fun AlphaInlineSectionLabel(text: String) {
    Text(
        text = text,
        color = AlphaColors.Accent,
        fontSize = alphaSp(12.5f),
        fontWeight = alphaStrongWeight(),
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
    icon: ImageVector? = null,
    collapsible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val snow = isSnowStyle()
    var expanded by rememberSaveable(title, collapsible, snow) {
        mutableStateOf(!collapsible)
    }

    if (snow) {
        AlphaCard(contentPadding = PaddingValues(0.dp)) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 58.dp)
                        .then(if (collapsible) Modifier.clickable { expanded = !expanded } else Modifier)
                        .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(AlphaShapes.Control)
                                .background(AlphaColors.AccentSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = AlphaColors.Accent,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                    Text(
                        text = title,
                        color = AlphaColors.Text,
                        fontSize = alphaSp(16f, maxScale = 1.04f),
                        lineHeight = alphaSp(20f, maxScale = 1.04f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(1.dp)
                            .background(AlphaColors.Divider.copy(alpha = 0.30f)),
                    )
                    Column(
                        modifier = Modifier.padding(bottom = 4.dp),
                        content = content,
                    )
                }
            }
        }
        return
    }

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
                fontWeight = alphaStrongWeight(),
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
private fun AlphaDeltaModePicker(
    deltaSelected: Boolean,
    onModeSelected: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_alpha_delta_mode),
            color = AlphaColors.Text,
            fontSize = alphaSp(14f, maxScale = 1.0f),
            fontWeight = alphaStrongWeight(),
        )
        Text(
            text = stringResource(R.string.settings_alpha_delta_mode_summary),
            color = AlphaColors.Muted,
            fontSize = alphaSp(12f, maxScale = 1.0f),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AlphaShapes.Control)
                .background(AlphaColors.SurfaceStrong)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(false, true).forEach { useDelta ->
                val selected = deltaSelected == useDelta
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(AlphaShapes.Control)
                        .background(if (selected) AlphaColors.Accent else AlphaColors.SurfaceStrong)
                        .clickable(enabled = !selected) { onModeSelected(useDelta) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (useDelta) R.string.settings_delta_mode else R.string.settings_alpha_mode
                        ),
                        color = if (selected) AlphaColors.OnAccent else AlphaColors.Muted,
                        fontSize = alphaSp(13f, maxScale = 1.0f),
                        fontWeight = alphaStrongWeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphaStylePicker(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
) {
    val snow = isSnowStyle()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = if (snow) 12.dp else 8.dp, vertical = if (snow) 10.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(if (snow) 8.dp else 6.dp),
    ) {
        InterfaceStyle.selectableEntries.forEachIndexed { index, style ->
            val selected = InterfaceStyle.selectedIndex(uiState.uiMode) == index
            Row(
                modifier = Modifier
                    .height(if (snow) 36.dp else 34.dp)
                    .clip(AlphaShapes.Control)
                    .background(
                        when {
                            selected && snow -> AlphaColors.AccentSoft
                            selected -> AlphaColors.Accent
                            else -> AlphaColors.SurfaceStrong
                        }
                    )
                    .clickable { actions.onSetUiModeIndex(index) }
                    .padding(horizontal = if (snow) 11.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (snow && selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = AlphaColors.Accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = stringResource(style.labelRes),
                    color = when {
                        selected && snow -> AlphaColors.Accent
                        selected -> AlphaColors.OnAccent
                        else -> AlphaColors.Muted
                    },
                    fontSize = alphaSp(13f, maxScale = 1.0f),
                    fontWeight = alphaStrongWeight(),
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
            fontWeight = alphaStrongWeight(),
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
                        color = if (selected) AlphaColors.OnAccent else AlphaColors.Muted,
                        fontSize = alphaSp(13f, maxScale = 1.0f),
                        fontWeight = alphaStrongWeight(),
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
            fontWeight = alphaStrongWeight(),
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
                        color = if (selected) AlphaColors.OnAccent else AlphaColors.Muted,
                        fontSize = alphaSp(13f, maxScale = 1.0f),
                        fontWeight = alphaStrongWeight(),
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
            fontWeight = alphaStrongWeight(),
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
                        color = if (selected) AlphaColors.OnAccent else AlphaColors.Muted,
                        fontSize = alphaSp(13f, maxScale = 1.0f),
                        fontWeight = alphaStrongWeight(),
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
            fontWeight = alphaStrongWeight(),
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
                        color = if (selected) AlphaColors.OnAccent else AlphaColors.Muted,
                        fontSize = alphaSp(13f, maxScale = 1.0f),
                        fontWeight = alphaStrongWeight(),
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
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val snow = isSnowStyle()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (snow) 62.dp else 52.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (snow) 12.dp else 14.dp, vertical = if (snow) 9.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (snow) 11.dp else 10.dp),
    ) {
        if (icon != null) {
            if (snow) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(AlphaShapes.Control)
                        .background(AlphaColors.SurfaceStrong),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) AlphaColors.Accent else AlphaColors.Disabled,
                        modifier = Modifier.size(19.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) AlphaColors.Accent else AlphaColors.Disabled,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) AlphaColors.Text else AlphaColors.Disabled,
                fontSize = alphaSp(if (snow) 14.5f else 15f),
                fontWeight = alphaStrongWeight(),
                maxLines = if (snow) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    color = if (enabled) AlphaColors.Muted else AlphaColors.Disabled,
                    fontSize = alphaSp(if (snow) 12.5f else 12f),
                    lineHeight = alphaSp(if (snow) 16f else 15f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (snow) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = if (enabled) AlphaColors.Muted else AlphaColors.Disabled,
                modifier = Modifier.size(18.dp),
            )
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
    val snow = isSnowStyle()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (snow) 60.dp else 52.dp)
            .padding(horizontal = if (snow) 12.dp else 14.dp, vertical = if (snow) 9.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) AlphaColors.Text else AlphaColors.Disabled,
                fontSize = alphaSp(if (snow) 14.5f else 15f),
                fontWeight = alphaStrongWeight(),
                maxLines = if (snow) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    color = AlphaColors.Muted,
                    fontSize = alphaSp(if (snow) 12.5f else 12f),
                    lineHeight = alphaSp(if (snow) 16f else 15f),
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
                fontWeight = alphaStrongWeight(),
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
