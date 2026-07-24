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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
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
import me.weishu.kernelsu.ui.component.delta.DeltaCard
import me.weishu.kernelsu.ui.component.delta.DeltaColors
import me.weishu.kernelsu.ui.component.delta.DeltaScreen
import me.weishu.kernelsu.ui.component.delta.DeltaShapes
import me.weishu.kernelsu.ui.component.delta.DeltaSwitch
import me.weishu.kernelsu.ui.component.delta.deltaSp
import me.weishu.kernelsu.ui.theme.DeltaColorVariant
import me.weishu.kernelsu.ui.theme.isInDarkTheme
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
fun SettingPagerDelta(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    DeltaScreen(
        title = stringResource(R.string.settings),
        icon = Icons.Rounded.Settings,
        bottomInnerPadding = bottomInnerPadding,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    top = 18.dp,
                    end = 20.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DeltaSettingsSection(title = stringResource(R.string.settings_ui_mode)) {
                DeltaAlphaModePicker(
                    deltaSelected = uiState.uiMode == InterfaceStyle.Delta.value,
                    onModeSelected = actions.onSetAlphaDeltaMode,
                )
                DeltaStylePicker(uiState = uiState, actions = actions)
            }

            DeltaSettingsSection(
                title = stringResource(R.string.settings_section_appearance),
                collapsible = true,
            ) {
                val dayNightChecked = isDayNightSwitchChecked(uiState.themeMode)
                DeltaSwitchRow(
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
                DeltaActionRow(
                    title = stringResource(R.string.settings_section_visual_effects),
                    summary = stringResource(R.string.settings_visual_effects_summary),
                    icon = Icons.Rounded.Visibility,
                    onClick = actions.onOpenVisualEffects,
                )
                DeltaActionRow(
                    title = stringResource(R.string.settings_ui_decoration_library),
                    summary = stringResource(R.string.settings_ui_decoration_library_summary),
                    icon = Icons.Rounded.AutoFixHigh,
                    onClick = actions.onOpenUiDecorationLibrary,
                )
                DeltaColorVariantPicker(
                    selectedVariant = uiState.deltaColorVariant,
                    onVariantSelected = actions.onSetDeltaColorVariant,
                )
                DeltaActionRow(
                    title = stringResource(R.string.settings_theme),
                    summary = stringResource(R.string.settings_theme_summary),
                    icon = Icons.Rounded.Palette,
                    onClick = actions.onOpenTheme,
                )
                DeltaActionRow(
                    title = stringResource(R.string.theme_store),
                    summary = stringResource(R.string.theme_store_settings_summary),
                    icon = Icons.Rounded.Storefront,
                    onClick = actions.onOpenThemeStore,
                )
                DeltaActionRow(
                    title = stringResource(R.string.settings_language),
                    summary = stringResource(R.string.settings_language_summary),
                    icon = Icons.Rounded.Language,
                    onClick = actions.onOpenLanguage,
                )
                DeltaActionRow(
                    title = stringResource(R.string.settings_manager_identity),
                    summary = stringResource(R.string.settings_manager_identity_summary),
                    icon = Icons.Rounded.Apps,
                    onClick = actions.onOpenLauncherIcon,
                )
                DeltaActionRow(
                    title = stringResource(R.string.settings_home_title),
                    summary = if (uiState.customHomeTitle.isBlank()) {
                        stringResource(R.string.settings_home_title_default_summary)
                    } else {
                        stringResource(R.string.settings_home_title_custom_summary, uiState.customHomeTitle)
                    },
                    icon = Icons.Rounded.EditNote,
                    onClick = actions.onEditHomeTitle,
                )
                DeltaSwitchRow(
                    title = stringResource(R.string.settings_show_home_support_card),
                    summary = stringResource(R.string.settings_show_home_support_card_summary),
                    checked = uiState.showHomeSupportCard,
                    onCheckedChange = actions.onSetShowHomeSupportCard,
                )
                DeltaSwitchRow(
                    title = stringResource(R.string.settings_show_home_learn_card),
                    summary = stringResource(R.string.settings_show_home_learn_card_summary),
                    checked = uiState.showHomeLearnCard,
                    onCheckedChange = actions.onSetShowHomeLearnCard,
                )
            }

            DeltaSettingsSection(
                title = stringResource(R.string.settings_section_updates),
                collapsible = true,
            ) {
                DeltaSwitchRow(
                    title = stringResource(R.string.settings_module_check_update),
                    summary = stringResource(R.string.settings_module_check_update_summary),
                    checked = uiState.checkModuleUpdate,
                    onCheckedChange = actions.onSetCheckModuleUpdate,
                )
                DeltaSwitchRow(
                    title = stringResource(R.string.settings_version_mismatch_warning),
                    summary = stringResource(R.string.settings_version_mismatch_warning_summary),
                    checked = uiState.showVersionMismatchWarning,
                    onCheckedChange = actions.onSetShowVersionMismatchWarning,
                )
                DeltaSwitchRow(
                    title = stringResource(R.string.settings_gki_warning),
                    summary = stringResource(R.string.settings_gki_warning_summary),
                    checked = uiState.showGkiWarning,
                    onCheckedChange = actions.onSetShowGkiWarning,
                )
            }

            DeltaSettingsSection(
                title = stringResource(R.string.settings_section_root_features),
                collapsible = true,
            ) {
                DeltaFeatureRows(uiState = uiState, actions = actions)
            }

            DeltaSettingsSection(
                title = stringResource(R.string.settings_section_advanced),
                collapsible = true,
            ) {
                DeltaActionRow(
                    title = stringResource(R.string.settings_profile_template),
                    summary = stringResource(R.string.settings_profile_template_summary),
                    onClick = actions.onOpenProfileTemplate,
                )
                DeltaSwitchRow(
                    title = stringResource(R.string.settings_umount_modules_default),
                    summary = stringResource(R.string.settings_umount_modules_default_summary),
                    checked = uiState.isDefaultUmountModules,
                    onCheckedChange = actions.onSetDefaultUmountModules,
                )
                DeltaActionRow(
                    title = stringResource(R.string.settings_builtin_mount),
                    summary = uiState.builtinMountConflict?.let {
                        stringResource(R.string.settings_builtin_mount_conflict_summary, it)
                    } ?: stringResource(R.string.settings_builtin_mount_summary),
                    icon = Icons.Rounded.Apps,
                    onClick = actions.onOpenBuiltinMount,
                )
                DeltaSwitchRow(
                    title = stringResource(R.string.settings_kpatch_next),
                    summary = kPatchNextSummary(uiState),
                    checked = uiState.isKPatchNextEnabled,
                    enabled = uiState.canToggleKPatchNext,
                    onCheckedChange = actions.onSetKPatchNextEnabled,
                )
                DeltaActionRow(
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
                DeltaActionRow(
                    title = pathConfigTitle(uiState),
                    summary = pathConfigSummary(uiState),
                    icon = Icons.Rounded.Visibility,
                    enabled = uiState.canOpenPathConfig,
                    onClick = actions.onOpenHiddenPathConfig,
                )
                DeltaActionRow(
                    title = stringResource(R.string.settings_cpu_spoof),
                    summary = stringResource(R.string.settings_cpu_spoof_summary),
                    icon = Icons.Rounded.DeveloperMode,
                    onClick = actions.onOpenCpuSpoof,
                )
                DeltaSwitchRow(
                    title = stringResource(R.string.settings_graphics_renderer_tool),
                    summary = stringResource(R.string.settings_graphics_renderer_tool_summary),
                    checked = uiState.graphicsRendererFeatureEnabled,
                    onCheckedChange = actions.onSetGraphicsRendererFeatureEnabled,
                )
                if (uiState.graphicsRendererFeatureEnabled) {
                    DeltaActionRow(
                        title = stringResource(R.string.settings_graphics_renderer),
                        summary = stringResource(R.string.settings_graphics_renderer_summary),
                        icon = Icons.Rounded.DeveloperMode,
                        onClick = actions.onOpenGraphicsRenderer,
                    )
                }
                DeltaActionRow(
                    title = stringResource(R.string.rescue_protection),
                    summary = stringResource(R.string.rescue_protection_summary),
                    icon = Icons.Rounded.Security,
                    onClick = actions.onOpenRescueProtection,
                )
                DeltaActionRow(
                    title = stringResource(R.string.settings_ai_chat),
                    summary = stringResource(R.string.settings_ai_chat_summary),
                    icon = Icons.Rounded.AutoFixHigh,
                    onClick = actions.onOpenAiChat,
                )
                DeltaSwitchRow(
                    title = stringResource(R.string.enable_web_debugging),
                    summary = stringResource(R.string.enable_web_debugging_summary),
                    checked = uiState.enableWebDebugging,
                    onCheckedChange = actions.onSetEnableWebDebugging,
                )
                DeltaSwitchRow(
                    title = stringResource(R.string.settings_auto_jailbreak),
                    summary = stringResource(R.string.settings_auto_jailbreak_summary),
                    checked = uiState.autoJailbreak,
                    enabled = uiState.isLateLoadMode,
                    onCheckedChange = actions.onSetAutoJailbreak,
                )
            }

            DeltaSettingsSection(
                title = stringResource(R.string.settings_section_maintenance),
                collapsible = true,
            ) {
                DeltaActionRow(
                    title = stringResource(R.string.about),
                    summary = "",
                    onClick = actions.onOpenAbout,
                )
            }
        }
    }
}

@Composable
private fun DeltaColorVariantPicker(
    selectedVariant: String,
    onVariantSelected: (String) -> Unit,
) {
    val darkMode = isInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_delta_color),
            color = DeltaColors.Ink,
            fontSize = deltaSp(14f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(DeltaShapes.Control)
                .background(DeltaColors.SurfaceDeep)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeltaColorOption(
                text = stringResource(R.string.color_green),
                selected = DeltaColorVariant.fromValue(selectedVariant) == DeltaColorVariant.Green,
                selectedBackground = if (darkMode) Color(0xFF24502E) else Color(0xFFA9DFAE),
                selectedForeground = if (darkMode) Color(0xFFE8F2E9) else Color(0xFF151915),
                onClick = { onVariantSelected(DeltaColorVariant.Green.value) },
                modifier = Modifier.weight(1f),
            )
            DeltaColorOption(
                text = stringResource(R.string.color_red),
                selected = DeltaColorVariant.fromValue(selectedVariant) == DeltaColorVariant.Red,
                selectedBackground = if (darkMode) Color(0xFF5A2A28) else Color(0xFFF4B6B2),
                selectedForeground = if (darkMode) Color(0xFFF5E8E7) else Color(0xFF1A1515),
                onClick = { onVariantSelected(DeltaColorVariant.Red.value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DeltaInlineSectionLabel(text: String) {
    Text(
        text = text,
        color = DeltaColors.Accent,
        fontSize = deltaSp(12.5f),
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun DeltaSwitchStylePicker(
    selectedStyle: String,
    onStyleSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_switch_style),
            color = DeltaColors.Ink,
            fontSize = deltaSp(14f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(DeltaShapes.Control)
                .background(DeltaColors.SurfaceDeep)
                .horizontalScroll(rememberScrollState())
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwitchStyle.entries.forEachIndexed { index, style ->
                val selected = SwitchStyle.fromValue(selectedStyle) == style
                DeltaColorOption(
                    text = stringResource(style.labelRes),
                    selected = selected,
                    selectedBackground = DeltaColors.Accent,
                    selectedForeground = Color.White,
                    onClick = { onStyleSelected(index) },
                    modifier = Modifier
                        .height(30.dp)
                        .width(88.dp)
                        .padding(horizontal = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun DeltaSnowEffectPicker(
    selectedEffect: String,
    onEffectSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_global_snow_effect),
            color = DeltaColors.Ink,
            fontSize = deltaSp(14f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(DeltaShapes.Control)
                .background(DeltaColors.SurfaceDeep)
                .horizontalScroll(rememberScrollState())
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlobalSnowEffect.entries.forEachIndexed { index, effect ->
                val selected = GlobalSnowEffect.fromValue(selectedEffect) == effect
                DeltaColorOption(
                    text = stringResource(effect.labelRes),
                    selected = selected,
                    selectedBackground = DeltaColors.Accent,
                    selectedForeground = Color.White,
                    onClick = { onEffectSelected(index) },
                    modifier = Modifier
                        .height(30.dp)
                        .width(96.dp)
                        .padding(horizontal = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun DeltaNightBackgroundEffectPicker(
    selectedEffect: String,
    onEffectSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_night_background_effect),
            color = DeltaColors.Ink,
            fontSize = deltaSp(14f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(DeltaShapes.Control)
                .background(DeltaColors.SurfaceDeep)
                .horizontalScroll(rememberScrollState())
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NightBackgroundEffect.entries.forEachIndexed { index, effect ->
                val selected = NightBackgroundEffect.fromValue(selectedEffect) == effect
                DeltaColorOption(
                    text = stringResource(effect.labelRes),
                    selected = selected,
                    selectedBackground = DeltaColors.Accent,
                    selectedForeground = Color.White,
                    onClick = { onEffectSelected(index) },
                    modifier = Modifier
                        .height(30.dp)
                        .width(96.dp)
                        .padding(horizontal = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun DeltaScrollEffectPicker(
    selectedEffect: String,
    onEffectSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_scroll_animation_effect),
            color = DeltaColors.Ink,
            fontSize = deltaSp(14f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(DeltaShapes.Control)
                .background(DeltaColors.SurfaceDeep)
                .horizontalScroll(rememberScrollState())
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlobalScrollEffect.entries.forEachIndexed { index, effect ->
                val selected = GlobalScrollEffect.fromValue(selectedEffect) == effect
                DeltaColorOption(
                    text = stringResource(effect.labelRes),
                    selected = selected,
                    selectedBackground = DeltaColors.Accent,
                    selectedForeground = Color.White,
                    onClick = { onEffectSelected(index) },
                    modifier = Modifier
                        .height(30.dp)
                        .width(96.dp)
                        .padding(horizontal = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun DeltaColorOption(
    text: String,
    selected: Boolean,
    selectedBackground: Color,
    selectedForeground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(DeltaShapes.Control)
            .background(if (selected) selectedBackground else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) selectedForeground else DeltaColors.Muted,
            fontSize = deltaSp(13f, maxScale = 1.0f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeltaFeatureRows(
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
        DeltaActionRow(
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
    DeltaSwitchRow(
        title = stringResource(R.string.settings_kernel_umount),
        summary = deltaFeatureSummary(uiState.kernelUmountStatus, R.string.settings_kernel_umount_summary),
        checked = uiState.isKernelUmountEnabled,
        enabled = uiState.kernelUmountStatus == "supported",
        onCheckedChange = actions.onSetKernelUmountEnabled,
    )
    DeltaSwitchRow(
        title = stringResource(R.string.settings_selinux_hide),
        summary = deltaFeatureSummary(uiState.selinuxHideStatus, R.string.settings_selinux_hide_summary),
        checked = uiState.isSelinuxHideEnabled,
        enabled = uiState.selinuxHideStatus == "supported",
        onCheckedChange = actions.onSetSelinuxHideEnabled,
    )
    DeltaSwitchRow(
        title = stringResource(R.string.settings_sulog),
        summary = deltaFeatureSummary(uiState.sulogStatus, R.string.settings_sulog_summary),
        checked = uiState.isSulogEnabled,
        enabled = uiState.sulogStatus == "supported",
        onCheckedChange = actions.onSetSulogEnabled,
    )
    DeltaSwitchRow(
        title = stringResource(R.string.settings_adb_root),
        summary = deltaFeatureSummary(uiState.adbRootStatus, R.string.settings_adb_root_summary),
        checked = uiState.isAdbRootEnabled,
        enabled = uiState.adbRootStatus == "supported",
        onCheckedChange = actions.onSetAdbRootEnabled,
    )
    DeltaSwitchRow(
        title = stringResource(R.string.settings_avc_spoof),
        summary = deltaFeatureSummary(uiState.avcSpoofStatus, R.string.settings_avc_spoof_summary),
        checked = uiState.isAvcSpoofEnabled,
        enabled = uiState.avcSpoofStatus == "supported",
        onCheckedChange = actions.onSetAvcSpoofEnabled,
    )
    DeltaSwitchRow(
        title = stringResource(R.string.settings_epkesu_hide),
        summary = stringResource(R.string.settings_epkesu_hide_summary),
        checked = uiState.isEpkesuHideEnabled,
        onCheckedChange = actions.onSetEpkesuHideEnabled,
    )
}

@Composable
private fun deltaFeatureSummary(status: String, defaultSummary: Int): String {
    return when (status) {
        "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
        "managed" -> stringResource(R.string.feature_status_managed_summary)
        else -> stringResource(defaultSummary)
    }
}

@Composable
private fun DeltaSettingsSection(
    title: String,
    collapsible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(!collapsible) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (collapsible) Modifier.clickable { expanded = !expanded } else Modifier)
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                color = DeltaColors.Accent,
                fontSize = deltaSp(17f, maxScale = 1.02f),
                lineHeight = deltaSp(21f, maxScale = 1.02f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (collapsible) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = title,
                    tint = DeltaColors.Accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (expanded) {
            DeltaCard(contentPadding = PaddingValues(0.dp)) {
                Column(content = content)
            }
        }
    }
}

@Composable
private fun DeltaAlphaModePicker(
    deltaSelected: Boolean,
    onModeSelected: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_alpha_delta_mode),
            color = DeltaColors.Ink,
            fontSize = deltaSp(14f, maxScale = 1.0f),
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(R.string.settings_alpha_delta_mode_summary),
            color = DeltaColors.Muted,
            fontSize = deltaSp(12f, maxScale = 1.0f),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DeltaShapes.Control)
                .background(DeltaColors.SurfaceDeep)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(false, true).forEach { useDelta ->
                val selected = deltaSelected == useDelta
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(DeltaShapes.Control)
                        .background(if (selected) DeltaColors.Accent else DeltaColors.SurfaceDeep)
                        .clickable(enabled = !selected) { onModeSelected(useDelta) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (useDelta) R.string.settings_delta_mode else R.string.settings_alpha_mode
                        ),
                        color = if (selected) Color.White else DeltaColors.Muted,
                        fontSize = deltaSp(13f, maxScale = 1.0f),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeltaStylePicker(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InterfaceStyle.selectableEntries.forEachIndexed { index, style ->
            val selected = InterfaceStyle.selectedIndex(uiState.uiMode) == index
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(DeltaShapes.Control)
                    .background(if (selected) DeltaColors.AccentSoft else DeltaColors.SurfaceDeep)
                    .clickable { actions.onSetUiModeIndex(index) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(style.labelRes),
                    color = if (selected) DeltaColors.Ink else DeltaColors.Muted,
                    fontSize = deltaSp(12.5f, maxScale = 1.0f),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DeltaActionRow(
    title: String,
    summary: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) DeltaColors.Accent else DeltaColors.Disabled,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) DeltaColors.Ink else DeltaColors.Disabled,
                fontSize = deltaSp(14f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    color = if (enabled) DeltaColors.Muted else DeltaColors.Disabled,
                    fontSize = deltaSp(11.5f),
                    lineHeight = deltaSp(14.5f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DeltaSwitchRow(
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
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) DeltaColors.Ink else DeltaColors.Disabled,
                fontSize = deltaSp(14f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    color = DeltaColors.Muted,
                    fontSize = deltaSp(11.5f),
                    lineHeight = deltaSp(14.5f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingContent != null) {
            trailingContent(checked, enabled, onCheckedChange)
        } else {
            DeltaSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun DeltaSliderRow(
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
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = DeltaColors.Ink,
                fontSize = deltaSp(14f),
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = valueLabel(sliderValue),
                color = DeltaColors.Muted,
                fontSize = deltaSp(11.5f),
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
private fun DeltaDurationRow(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    DeltaSliderRow(
        title = stringResource(R.string.settings_startup_sound_duration),
        value = value.toFloat(),
        valueRange = MIN_CUSTOM_STARTUP_SOUND_DURATION_SECONDS.toFloat()..
            MAX_CUSTOM_STARTUP_SOUND_DURATION_SECONDS.toFloat(),
        valueLabel = { stringResource(R.string.settings_startup_sound_duration_value, it.roundToInt()) },
        onValueChange = { onValueChange(it.roundToInt()) },
    )
}

@Composable
private fun DeltaVideoDurationRow(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    DeltaSliderRow(
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
