package me.weishu.kernelsu.ui.screen.colorpalette

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassSurface
import me.weishu.kernelsu.ui.component.liquid.liquidGlassMiuixCardColors
import me.weishu.kernelsu.ui.component.miuix.ScaleDialog
import me.weishu.kernelsu.ui.theme.ColorMode
import me.weishu.kernelsu.ui.theme.CustomThemePreset
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.theme.keyColorOptions
import me.weishu.kernelsu.ui.theme.ThemePreset
import me.weishu.kernelsu.ui.theme.ThemeSyncStrategy
import me.weishu.kernelsu.ui.theme.skrootproTopBarColors
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import me.weishu.kernelsu.ui.component.miuix.SunMoonSwitchPreference as SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.roundToInt

@Composable
fun ColorPaletteScreenMiuix(
    state: ColorPaletteUiState,
    actions: ColorPaletteScreenActions,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlurState = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlurState)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    val topBarColors = skrootproTopBarColors(barColor, colorScheme.onSurface)
    val uiState = state.uiState
    val isLiquidGlassInterface = uiState.uiMode == InterfaceStyle.LiquidGlass.value
    val currentColorMode = state.currentColorMode
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var renamePreset by remember { mutableStateOf<CustomThemePreset?>(null) }
    val isDark = currentColorMode.isDark || currentColorMode.isSystem && isSystemInDarkTheme()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = topBarColors.container,
                    titleColor = topBarColors.content,
                    title = stringResource(R.string.settings_theme),
                    navigationIcon = {
                        IconButton(
                            onClick = actions.onBack
                        ) {
                            val layoutDirection = LocalLayoutDirection.current
                            Icon(
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = topBarColors.content
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        val showScaleDialog = rememberSaveable { mutableStateOf(false) }

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
                    val currentPreset = ThemePreset.fromValue(uiState.themePreset)
                    Spacer(modifier = Modifier.height(32.dp))
                    ThemePreviewCardMiuix(
                        keyColor = uiState.keyColor,
                        isDark = isDark,
                        miuixMonet = uiState.miuixMonet,
                        enableFloatingBottomBar = uiState.enableFloatingBottomBar,
                        enableFloatingBottomBarBlur = if (isLiquidGlassInterface) {
                            false
                        } else {
                            uiState.enableFloatingBottomBarBlur
                        },
                        paletteStyle = state.currentPaletteStyle,
                        colorSpec = state.currentColorSpec,
                        presetLabel = stringResource(currentPreset.titleRes),
                        colorModeLabel = themeColorModeLabel(currentColorMode),
                        keyColorLabel = themeKeyColorLabel(uiState.keyColor),
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    ThemePresetCardsMiuix(
                        uiMode = uiState.uiMode,
                        currentPreset = currentPreset,
                        isDark = isDark,
                        onApplyThemePreset = actions.onApplyThemePreset,
                    )

                    run {
                        val themeItems = listOf(
                            stringResource(id = R.string.settings_theme_mode_system),
                            stringResource(id = R.string.settings_theme_mode_light),
                            stringResource(id = R.string.settings_theme_mode_dark),
                        )
                        TabRow(
                            tabs = themeItems,
                            selectedTabIndex = (if (uiState.themeMode >= 3) uiState.themeMode - 3 else uiState.themeMode).coerceIn(0, 2),
                            onTabSelected = { index ->
                                actions.onSetThemeMode(index)
                            },
                            height = 48.dp,
                        )

                        Card(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth()
                                .themeLiquidGlassSurface(),
                            colors = liquidGlassMiuixCardColors(),
                        ) {
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_monet),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Wallpaper,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_monet),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.miuixMonet,
                                onCheckedChange = {
                                    actions.onSetMiuixMonet(it)
                                }
                            )

                            AnimatedVisibility(
                                visible = uiState.miuixMonet
                            ) {
                                Column {
                                    val colorItems = listOf(
                                        stringResource(id = R.string.settings_key_color_default),
                                        stringResource(id = R.string.color_red),
                                        stringResource(id = R.string.color_pink),
                                        stringResource(id = R.string.color_purple),
                                        stringResource(id = R.string.color_deep_purple),
                                        stringResource(id = R.string.color_indigo),
                                        stringResource(id = R.string.color_blue),
                                        stringResource(id = R.string.color_cyan),
                                        stringResource(id = R.string.color_teal),
                                        stringResource(id = R.string.color_green),
                                        stringResource(id = R.string.color_yellow),
                                        stringResource(id = R.string.color_amber),
                                        stringResource(id = R.string.color_orange),
                                        stringResource(id = R.string.color_brown),
                                        stringResource(id = R.string.color_blue_grey),
                                        stringResource(id = R.string.color_sakura),
                                    )
                                    val colorValues = listOf(0) + keyColorOptions
                                    OverlayDropdownPreference(
                                        title = stringResource(id = R.string.settings_key_color),
                                        items = colorItems,
                                        startAction = {
                                            Icon(
                                                Icons.Rounded.Colorize,
                                                modifier = Modifier.padding(end = 6.dp),
                                                contentDescription = stringResource(id = R.string.settings_key_color),
                                                tint = colorScheme.onBackground
                                            )
                                        },
                                        selectedIndex = colorValues.indexOf(uiState.keyColor).takeIf { it >= 0 } ?: 0,
                                        onSelectedIndexChange = { index ->
                                            actions.onSetKeyColor(colorValues[index])
                                        }
                                    )

                                    AnimatedVisibility(
                                        visible = uiState.keyColor != 0
                                    ) {
                                        Column {
                                            val styles = PaletteStyle.entries
                                            OverlayDropdownPreference(
                                                title = stringResource(R.string.settings_color_style),
                                                startAction = {
                                                    Icon(
                                                        Icons.Rounded.Style,
                                                        modifier = Modifier.padding(end = 6.dp),
                                                        contentDescription = stringResource(id = R.string.settings_color_style),
                                                        tint = colorScheme.onBackground
                                                    )
                                                },
                                                items = styles.map { it.name },
                                                selectedIndex = styles.indexOfFirst { it.name == uiState.colorStyle }.coerceAtLeast(0),
                                                onSelectedIndexChange = { index ->
                                                    actions.onSetColorStyle(styles[index].name)
                                                }
                                            )

                                            val specs = ColorSpec.SpecVersion.entries
                                            OverlayDropdownPreference(
                                                title = stringResource(R.string.settings_color_spec),
                                                startAction = {
                                                    Icon(
                                                        Icons.Rounded.DesignServices,
                                                        modifier = Modifier.padding(end = 6.dp),
                                                        contentDescription = stringResource(id = R.string.settings_color_spec),
                                                        tint = colorScheme.onBackground
                                                    )
                                                },
                                                items = specs.map { it.name },
                                                selectedIndex = specs.indexOfFirst { it.name == uiState.colorSpec }.coerceAtLeast(0),
                                                onSelectedIndexChange = { index ->
                                                    actions.onSetColorSpec(specs[index].name)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .themeLiquidGlassSurface(),
                        colors = liquidGlassMiuixCardColors(),
                    ) {
                        if (!isLiquidGlassInterface && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_enable_blur),
                                summary = stringResource(id = R.string.settings_enable_blur_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.BlurOn,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_enable_blur),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.enableBlur,
                                onCheckedChange = {
                                    actions.onSetEnableBlur(it)
                                }
                            )
                        }
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_floating_bottom_bar),
                            summary = stringResource(id = R.string.settings_floating_bottom_bar_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.CallToAction,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_floating_bottom_bar),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.enableFloatingBottomBar,
                            onCheckedChange = {
                                actions.onSetEnableFloatingBottomBar(it)
                            }
                        )
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_auto_hide_navigation_bar),
                            summary = stringResource(id = R.string.settings_auto_hide_navigation_bar_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Timer,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(
                                        id = R.string.settings_auto_hide_navigation_bar
                                    ),
                                    tint = colorScheme.onBackground,
                                )
                            },
                            checked = uiState.autoHideNavigationBar,
                            onCheckedChange = actions.onSetAutoHideNavigationBar,
                        )
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_scroll_hide_navigation_bar),
                            summary = stringResource(id = R.string.settings_scroll_hide_navigation_bar_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.SwapVert,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(
                                        id = R.string.settings_scroll_hide_navigation_bar
                                    ),
                                    tint = colorScheme.onBackground,
                                )
                            },
                            checked = uiState.scrollHideNavigationBar,
                            onCheckedChange = actions.onSetScrollHideNavigationBar,
                        )
                        AnimatedVisibility(
                            visible = !isLiquidGlassInterface &&
                                uiState.enableFloatingBottomBar &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ) {
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_enable_glass),
                                summary = stringResource(id = R.string.settings_enable_glass_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.WaterDrop,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_enable_glass),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.enableFloatingBottomBarBlur,
                                onCheckedChange = {
                                    actions.onSetEnableFloatingBottomBarBlur(it)
                                }
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .themeLiquidGlassSurface(),
                        colors = liquidGlassMiuixCardColors(),
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_enable_predictive_back),
                                summary = stringResource(id = R.string.settings_enable_predictive_back_summary),
                                startAction = {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.MenuOpen,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_enable_predictive_back),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.enablePredictiveBack,
                                onCheckedChange = {
                                    actions.onSetEnablePredictiveBack(it)
                                }
                            )
                        }

                        AdvancedSliderMiuix(
                            title = stringResource(id = R.string.settings_page_scale),
                            summary = stringResource(id = R.string.settings_page_scale_summary),
                            icon = Icons.Rounded.AspectRatio,
                            value = uiState.pageScale,
                            valueRange = 0.8f..1.1f,
                            keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                            onValueChangeFinished = actions.onSetPageScale,
                            onClick = { showScaleDialog.value = !showScaleDialog.value },
                            holdDownState = showScaleDialog.value,
                        )
                        AdvancedSliderMiuix(
                            title = stringResource(id = R.string.settings_font_scale),
                            summary = stringResource(id = R.string.settings_font_scale_summary),
                            icon = Icons.Rounded.FontDownload,
                            value = uiState.fontScale,
                            valueRange = 0.85f..1.2f,
                            keyPoints = listOf(0.85f, 1f, 1.1f, 1.2f),
                            onValueChangeFinished = actions.onSetFontScale,
                        )
                        AdvancedSliderMiuix(
                            title = stringResource(id = R.string.settings_blur_intensity),
                            summary = stringResource(id = R.string.settings_blur_intensity_summary),
                            icon = Icons.Rounded.BlurOn,
                            value = uiState.blurIntensity,
                            valueRange = 0.5f..1.5f,
                            keyPoints = listOf(0.5f, 1f, 1.25f, 1.5f),
                            onValueChangeFinished = actions.onSetBlurIntensity,
                        )
                        ScaleDialog(
                            show = showScaleDialog.value,
                            onDismissRequest = { showScaleDialog.value = false },
                            volumeState = { uiState.pageScale },
                            onVolumeChange = {
                                actions.onSetPageScale(it)
                            }
                        )
                    }

                    ThemeCustomPresetsMiuix(
                        customPresets = uiState.customThemePresets,
                        themeSyncStrategy = uiState.themeSyncStrategy,
                        onSaveCustomThemePreset = { showSavePresetDialog = true },
                        onApplyCustomThemePreset = actions.onApplyCustomThemePreset,
                        onRenameCustomThemePreset = { renamePreset = it },
                        onDeleteCustomThemePreset = actions.onDeleteCustomThemePreset,
                        onSetThemeSyncStrategy = actions.onSetThemeSyncStrategy,
                        onResetThemeToDefault = actions.onResetThemeToDefault,
                    )
                }
                item {
                    Spacer(
                        Modifier.height(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                    WindowInsets.captionBar.asPaddingValues().calculateBottomPadding() +
                                    12.dp
                        )
                    )
                }
            }
        }
    }

    ThemePresetNameDialog(
        show = showSavePresetDialog,
        title = stringResource(R.string.theme_custom_preset_save),
        onDismissRequest = { showSavePresetDialog = false },
        onConfirm = actions.onSaveCustomThemePreset,
    )
    renamePreset?.let { preset ->
        ThemePresetNameDialog(
            show = true,
            title = stringResource(R.string.theme_custom_preset_rename),
            initialName = preset.name,
            onDismissRequest = { renamePreset = null },
            onConfirm = { name -> actions.onRenameCustomThemePreset(preset.id, name) },
        )
    }
}

@Composable
private fun themeColorModeLabel(mode: ColorMode): String {
    val displayMode = if (mode.isMonet) ColorMode.fromValue(mode.toNonMonetMode()) else mode
    return when (displayMode) {
        ColorMode.LIGHT -> stringResource(R.string.settings_theme_mode_light)
        ColorMode.DARK -> stringResource(R.string.settings_theme_mode_dark)
        ColorMode.DARK_AMOLED -> stringResource(R.string.settings_theme_mode_amoled)
        else -> stringResource(R.string.settings_theme_mode_system)
    }
}

@Composable
private fun themeKeyColorLabel(keyColor: Int): String {
    return if (keyColor == 0) {
        stringResource(R.string.settings_key_color_default)
    } else {
        "#%06X".format(keyColor and 0x00FFFFFF)
    }
}

@Composable
private fun ThemeSectionHeaderMiuix(
    title: String,
    summary: String? = null,
) {
    Column(
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            color = colorScheme.onBackground,
            fontSize = 17.sp
        )
        if (summary != null) {
            Text(
                text = summary,
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ThemePresetCardsMiuix(
    uiMode: String,
    currentPreset: ThemePreset,
    isDark: Boolean,
    onApplyThemePreset: (ThemePreset) -> Unit,
) {
    val compatiblePresets = ThemePreset.workshopPresets.filter { it.isCompatibleWith(uiMode) }
    val visiblePresets = if (currentPreset == ThemePreset.CUSTOM) {
        listOf(ThemePreset.CUSTOM) + compatiblePresets
    } else {
        compatiblePresets
    }

    ThemeSectionHeaderMiuix(
        title = stringResource(R.string.theme_workshop),
        summary = stringResource(R.string.theme_workshop_summary),
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(visiblePresets) { preset ->
            ThemePresetCardMiuix(
                preset = preset,
                selected = currentPreset == preset,
                enabled = preset != ThemePreset.CUSTOM,
                isDark = isDark,
                onClick = { onApplyThemePreset(preset) }
            )
        }
    }
}

@Composable
private fun ThemePresetCardMiuix(
    preset: ThemePreset,
    selected: Boolean,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) colorScheme.primary else colorScheme.outline.copy(alpha = 0.45f)
    Card(
        modifier = Modifier
            .width(176.dp)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled) { onClick() }
            .themeLiquidGlassSurface(),
        colors = liquidGlassMiuixCardColors(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PresetMiniPreviewMiuix(preset = preset, isDark = isDark)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Palette,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp),
                    contentDescription = null,
                    tint = if (selected) colorScheme.primary else colorScheme.onBackground
                )
                Text(
                    text = stringResource(preset.titleRes),
                    color = colorScheme.onBackground,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(
                        Icons.Rounded.Tune,
                        modifier = Modifier.size(18.dp),
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                }
            }
            Text(
                text = stringResource(preset.summaryRes),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PresetMiniPreviewMiuix(
    preset: ThemePreset,
    isDark: Boolean,
) {
    val previewIsDark = preset.colorMode.isDark || preset.colorMode.isSystem && isDark
    val seedColor = if (preset.keyColor == 0) colorScheme.primary else Color(preset.keyColor)
    val previewScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = previewIsDark,
        style = preset.paletteStyle,
        specVersion = preset.colorSpec,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(previewScheme.background)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            previewScheme.primary,
            previewScheme.secondaryContainer,
            previewScheme.tertiaryContainer,
            previewScheme.surfaceContainerHighest
        ).forEach { previewColor ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewColor)
            )
        }
    }
}

@Composable
private fun Modifier.themeLiquidGlassSurface(): Modifier {
    return globalLiquidGlassSurface(
        shape = RoundedCornerShape(18.dp),
        surfaceAlpha = 0.58f,
        blurRadius = 10.dp,
        refractionHeight = 14.dp,
        refractionAmount = 9.dp,
        strokeAlpha = 0.66f,
    )
}

@Composable
private fun ThemeCustomPresetsMiuix(
    customPresets: List<CustomThemePreset>,
    themeSyncStrategy: ThemeSyncStrategy,
    onSaveCustomThemePreset: () -> Unit,
    onApplyCustomThemePreset: (String) -> Unit,
    onRenameCustomThemePreset: (CustomThemePreset) -> Unit,
    onDeleteCustomThemePreset: (String) -> Unit,
    onSetThemeSyncStrategy: (ThemeSyncStrategy) -> Unit,
    onResetThemeToDefault: () -> Unit,
) {
    val syncItems = listOf(
        stringResource(R.string.theme_sync_shared),
        stringResource(R.string.theme_sync_per_style),
    )
    Card(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .themeLiquidGlassSurface(),
        colors = liquidGlassMiuixCardColors(),
    ) {
        ArrowPreference(
            title = stringResource(R.string.theme_custom_preset_save),
            summary = stringResource(R.string.theme_custom_preset_save_summary),
            startAction = {
                Icon(
                    Icons.Rounded.Save,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(id = R.string.theme_custom_preset_save),
                    tint = colorScheme.onBackground
                )
            },
            onClick = onSaveCustomThemePreset,
        )
        customPresets.forEach { preset ->
            ArrowPreference(
                title = preset.name,
                summary = stringResource(R.string.theme_custom_preset_item_summary),
                startAction = {
                    Icon(
                        Icons.Rounded.Tune,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = preset.name,
                        tint = colorScheme.onBackground
                    )
                },
                endActions = {
                    IconButton(onClick = { onRenameCustomThemePreset(preset) }) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.theme_custom_preset_rename),
                            tint = colorScheme.onSurfaceVariantActions,
                        )
                    }
                    IconButton(onClick = { onDeleteCustomThemePreset(preset.id) }) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.theme_custom_preset_delete),
                            tint = colorScheme.onSurfaceVariantActions,
                        )
                    }
                },
                onClick = { onApplyCustomThemePreset(preset.id) },
            )
        }
        OverlayDropdownPreference(
            title = stringResource(R.string.theme_sync_strategy),
            summary = stringResource(R.string.theme_sync_strategy_summary),
            items = syncItems,
            startAction = {
                Icon(
                    Icons.Rounded.SyncAlt,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(id = R.string.theme_sync_strategy),
                    tint = colorScheme.onBackground
                )
            },
            selectedIndex = if (themeSyncStrategy == ThemeSyncStrategy.SHARED) 0 else 1,
            onSelectedIndexChange = { index ->
                onSetThemeSyncStrategy(
                    if (index == 0) ThemeSyncStrategy.SHARED else ThemeSyncStrategy.PER_STYLE
                )
            },
        )
        ArrowPreference(
            title = stringResource(R.string.theme_reset_default),
            summary = stringResource(R.string.theme_reset_default_summary),
            startAction = {
                Icon(
                    Icons.Rounded.RestartAlt,
                    modifier = Modifier.padding(end = 6.dp),
                    contentDescription = stringResource(id = R.string.theme_reset_default),
                    tint = colorScheme.onBackground
                )
            },
            onClick = onResetThemeToDefault,
        )
    }
}

@Composable
private fun AdvancedSliderMiuix(
    title: String,
    summary: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    keyPoints: List<Float>,
    onValueChangeFinished: (Float) -> Unit,
    onClick: () -> Unit = {},
    holdDownState: Boolean = false,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    ArrowPreference(
        title = title,
        summary = summary,
        startAction = {
            Icon(
                icon,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = title,
                tint = colorScheme.onBackground
            )
        },
        endActions = {
            Text(
                text = "${(sliderValue * 100).roundToInt()}%",
                color = colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = onClick,
        holdDownState = holdDownState,
        bottomAction = {
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onValueChangeFinished(sliderValue) },
                valueRange = valueRange,
                showKeyPoints = true,
                keyPoints = keyPoints,
                magnetThreshold = 0.01f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            )
        },
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCardMiuix(
    keyColor: Int,
    isDark: Boolean,
    miuixMonet: Boolean,
    enableFloatingBottomBar: Boolean = false,
    enableFloatingBottomBarBlur: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
    presetLabel: String,
    colorModeLabel: String,
    keyColorLabel: String,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight

    val seedColor = if (keyColor == 0) colorScheme.primary else Color(keyColor)
    val effectiveStyle = if (keyColor == 0) PaletteStyle.TonalSpot else paletteStyle
    val effectiveSpec = if (keyColor == 0) ColorSpec.SpecVersion.Default else colorSpec
    val dynamicCs = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        style = effectiveStyle,
        specVersion = effectiveSpec,
    )

    val bgColor = if (miuixMonet) dynamicCs.background else colorScheme.surface
    val textColor = if (miuixMonet) dynamicCs.onSurface else colorScheme.onBackground
    val accentCardColor = when {
        miuixMonet -> dynamicCs.secondaryContainer
        isDark -> Color(0xFF1A3825)
        else -> Color(0xFFDFFAE4)
    }
    val cardColor = if (miuixMonet) dynamicCs.surfaceContainerHighest else colorScheme.surfaceVariant
    val navBarColor = if (miuixMonet) dynamicCs.surfaceContainer else colorScheme.surface
    val iconColor = if (miuixMonet) dynamicCs.primary else colorScheme.primary
    val navSelectedColor = colorScheme.onSurfaceContainer
    val navUnselectedColor = colorScheme.onSurfaceContainer.copy(alpha = 0.5f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .themeLiquidGlassSurface(),
        colors = liquidGlassMiuixCardColors(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(168.dp)
                    .aspectRatio(screenRatio)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 12.sp,
                        color = textColor
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentCardColor)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.8f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(.1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(.1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardColor)
                    )
                }

            }

            if (enableFloatingBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (enableFloatingBottomBarBlur) navBarColor.copy(alpha = 0.5f)
                                else navBarColor
                            )
                            .border(0.5.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (it == 0) iconColor else textColor)
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(textColor.copy(alpha = 0.1f))
                    )
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .fillMaxWidth()
                            .background(navBarColor)
                            .padding(top = 2.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (it == 0) navSelectedColor else navUnselectedColor)
                            )
                        }
                    }
                }
            }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemePreviewInfoMiuix(
                    label = stringResource(R.string.theme_current_preset),
                    value = presetLabel,
                    modifier = Modifier.weight(1f)
                )
                ThemePreviewInfoMiuix(
                    label = stringResource(R.string.theme_current_mode),
                    value = colorModeLabel,
                    modifier = Modifier.weight(1f)
                )
            }
            ThemePreviewInfoMiuix(
                label = stringResource(R.string.settings_key_color),
                value = keyColorLabel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ThemePreviewInfoMiuix(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp
            )
            Text(
                text = value,
                color = colorScheme.onBackground,
                fontSize = 14.sp
            )
        }
    }
}
