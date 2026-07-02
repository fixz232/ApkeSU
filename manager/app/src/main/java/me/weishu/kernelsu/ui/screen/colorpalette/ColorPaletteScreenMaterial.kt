package me.weishu.kernelsu.ui.screen.colorpalette

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.material.SegmentedColumn
import me.weishu.kernelsu.ui.component.material.SegmentedDropdownItem
import me.weishu.kernelsu.ui.component.material.SegmentedListItem
import me.weishu.kernelsu.ui.component.material.SegmentedSwitchItem
import me.weishu.kernelsu.ui.component.material.TonalCard
import me.weishu.kernelsu.ui.theme.CustomThemePreset
import me.weishu.kernelsu.ui.theme.ColorMode
import me.weishu.kernelsu.ui.theme.ThemePreset
import me.weishu.kernelsu.ui.theme.ThemeSyncStrategy
import me.weishu.kernelsu.ui.theme.keyColorOptions
import kotlin.math.roundToInt

@Composable
fun ColorPaletteScreenMaterial(
    state: ColorPaletteUiState,
    actions: ColorPaletteScreenActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uiState = state.uiState
    val currentColorMode = state.currentColorMode
    val currentKeyColor = uiState.keyColor
    val colorStyle = state.currentPaletteStyle
    val colorSpec = state.currentColorSpec
    val haptic = LocalHapticFeedback.current
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var renamePreset by remember { mutableStateOf<CustomThemePreset?>(null) }

    LaunchedEffect(Unit) {
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = actions.onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                title = { Text(stringResource(R.string.settings_theme)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        val navBars = WindowInsets.navigationBars.asPaddingValues()
        val captionBar = WindowInsets.captionBar.asPaddingValues()

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isDark = currentColorMode.isDark || currentColorMode.isSystem && isSystemInDarkTheme()
            val currentPreset = ThemePreset.fromValue(uiState.themePreset)
            ThemePreviewCard(
                keyColor = currentKeyColor,
                isDark = isDark,
                paletteStyle = colorStyle,
                colorSpec = colorSpec,
                presetLabel = stringResource(currentPreset.titleRes),
                colorModeLabel = themeColorModeLabel(currentColorMode),
                keyColorLabel = themeKeyColorLabel(currentKeyColor),
            )

            ThemePresetCardsMaterial(
                uiMode = uiState.uiMode,
                currentPreset = currentPreset,
                isDark = isDark,
                onApplyThemePreset = actions.onApplyThemePreset,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeSectionHeaderMaterial(
                    title = stringResource(R.string.theme_section_color_mode),
                    summary = stringResource(R.string.theme_section_color_mode_summary),
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        ColorButtonMaterial(
                            color = Color.Unspecified,
                            isSelected = currentKeyColor == 0,
                            isDark = isDark,
                            paletteStyle = colorStyle,
                            colorSpec = colorSpec,
                            onClick = {
                                actions.onSetKeyColor(0)
                            }
                        )
                    }

                    items(keyColorOptions) { color ->
                        ColorButtonMaterial(
                            color = Color(color),
                            isSelected = currentKeyColor == color,
                            isDark = isDark,
                            paletteStyle = colorStyle,
                            colorSpec = colorSpec,
                            onClick = {
                                actions.onSetKeyColor(color)
                            }
                        )
                    }
                }

                val options = listOf(
                    listOf(ColorMode.SYSTEM) to stringResource(R.string.settings_theme_mode_system),
                    listOf(ColorMode.LIGHT) to stringResource(R.string.settings_theme_mode_light),
                    listOf(ColorMode.DARK) to stringResource(R.string.settings_theme_mode_dark),
                    listOf(ColorMode.DARK_AMOLED) to stringResource(R.string.settings_theme_mode_amoled)
                )

                options.chunked(4).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        rowOptions.forEachIndexed { index, (modes, label) ->
                            ToggleButton(
                                checked = currentColorMode in modes,
                                onCheckedChange = {
                                    if (it) {
                                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                        actions.onSetColorMode(modes.first())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    rowOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            ) {
                                Icon(
                                    imageVector = when (modes.first()) {
                                        ColorMode.SYSTEM -> Icons.Filled.Brightness4
                                        ColorMode.LIGHT -> Icons.Filled.Brightness7
                                        ColorMode.DARK -> Icons.Filled.Brightness3
                                        ColorMode.DARK_AMOLED -> Icons.Filled.Brightness1
                                        else -> Icons.Filled.Brightness4
                                    },
                                    contentDescription = label
                                )
                            }
                        }
                    }
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            val styles = PaletteStyle.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Style,
                                title = stringResource(R.string.settings_color_style),
                                items = styles.map { it.name },
                                selectedIndex = styles.indexOf(colorStyle),
                                onItemSelected = { index ->
                                    actions.onSetColorStyle(styles[index].name)
                                }
                            )
                        },
                        {
                            val specs = ColorSpec.SpecVersion.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.DesignServices,
                                title = stringResource(R.string.settings_color_spec),
                                items = specs.map { it.name },
                                selectedIndex = specs.indexOf(colorSpec).coerceAtLeast(0),
                                onItemSelected = { index ->
                                    actions.onSetColorSpec(specs[index].name)
                                }
                            )
                        }
                    )
                )

                ThemeSectionHeaderMaterial(
                    title = stringResource(R.string.theme_section_advanced_appearance),
                    summary = stringResource(R.string.theme_section_advanced_appearance_summary),
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    SegmentedColumn(
                        content = listOf(
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.AutoMirrored.Rounded.MenuOpen,
                                    title = stringResource(id = R.string.settings_enable_predictive_back),
                                    summary = stringResource(id = R.string.settings_enable_predictive_back_summary),
                                    checked = uiState.enablePredictiveBack,
                                    onCheckedChange = actions.onSetEnablePredictiveBack
                                )
                            }
                        )
                    )
                }

                TonalCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AdvancedSliderMaterial(
                            icon = Icons.Rounded.AspectRatio,
                            title = stringResource(R.string.settings_page_scale),
                            summary = stringResource(id = R.string.settings_page_scale_summary),
                            value = uiState.pageScale,
                            valueRange = 0.8f..1.1f,
                            onValueChangeFinished = actions.onSetPageScale,
                        )
                        AdvancedSliderMaterial(
                            icon = Icons.Rounded.FontDownload,
                            title = stringResource(R.string.settings_font_scale),
                            summary = stringResource(id = R.string.settings_font_scale_summary),
                            value = uiState.fontScale,
                            valueRange = 0.85f..1.2f,
                            onValueChangeFinished = actions.onSetFontScale,
                        )
                        AdvancedSliderMaterial(
                            icon = Icons.Rounded.BlurOn,
                            title = stringResource(R.string.settings_blur_intensity),
                            summary = stringResource(id = R.string.settings_blur_intensity_summary),
                            value = uiState.blurIntensity,
                            valueRange = 0.5f..1.5f,
                            onValueChangeFinished = actions.onSetBlurIntensity,
                        )
                    }
                }

                ThemeCustomPresetsMaterial(
                    customPresets = uiState.customThemePresets,
                    onSaveCustomThemePreset = { showSavePresetDialog = true },
                    onApplyCustomThemePreset = actions.onApplyCustomThemePreset,
                    onRenameCustomThemePreset = { renamePreset = it },
                    onDeleteCustomThemePreset = actions.onDeleteCustomThemePreset,
                    themeSyncStrategy = uiState.themeSyncStrategy,
                    onSetThemeSyncStrategy = actions.onSetThemeSyncStrategy,
                    onResetThemeToDefault = actions.onResetThemeToDefault,
                )
            }

            Spacer(modifier = Modifier.height(16.dp + navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()))
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
private fun ThemeSectionHeaderMaterial(
    title: String,
    summary: String? = null,
) {
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (summary != null) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
private fun ThemePresetCardsMaterial(
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ThemeSectionHeaderMaterial(
                title = stringResource(R.string.theme_workshop),
                summary = stringResource(R.string.theme_workshop_summary),
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(visiblePresets) { preset ->
                ThemePresetCardMaterial(
                    preset = preset,
                    selected = currentPreset == preset,
                    enabled = preset != ThemePreset.CUSTOM,
                    isDark = isDark,
                    onClick = { onApplyThemePreset(preset) }
                )
            }
        }
    }
}

@Composable
private fun ThemePresetCardMaterial(
    preset: ThemePreset,
    selected: Boolean,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Surface(
        onClick = {
            if (enabled) onClick()
        },
        enabled = enabled,
        modifier = Modifier.width(178.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PresetMiniPreviewMaterial(preset = preset, isDark = isDark)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(preset.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = stringResource(preset.summaryRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PresetMiniPreviewMaterial(
    preset: ThemePreset,
    isDark: Boolean,
) {
    val previewIsDark = preset.colorMode.isDark || preset.colorMode.isSystem && isDark
    val seedColor = if (preset.keyColor == 0) MaterialTheme.colorScheme.primary else Color(preset.keyColor)
    val previewScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = previewIsDark,
        style = preset.paletteStyle,
        specVersion = preset.colorSpec,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
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
        ).forEach { color ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ThemeCustomPresetsMaterial(
    customPresets: List<CustomThemePreset>,
    onSaveCustomThemePreset: () -> Unit,
    onApplyCustomThemePreset: (String) -> Unit,
    onRenameCustomThemePreset: (CustomThemePreset) -> Unit,
    onDeleteCustomThemePreset: (String) -> Unit,
    themeSyncStrategy: ThemeSyncStrategy,
    onSetThemeSyncStrategy: (ThemeSyncStrategy) -> Unit,
    onResetThemeToDefault: () -> Unit,
) {
    val syncItems = listOf(
        stringResource(R.string.theme_sync_shared),
        stringResource(R.string.theme_sync_per_style),
    )
    SegmentedColumn(
        modifier = Modifier.padding(top = 4.dp),
        title = stringResource(R.string.theme_custom_presets),
        content = buildList {
            add {
                SegmentedListItem(
                    onClick = onSaveCustomThemePreset,
                    headlineContent = { Text(stringResource(R.string.theme_custom_preset_save)) },
                    supportingContent = { Text(stringResource(R.string.theme_custom_preset_save_summary)) },
                    leadingContent = { Icon(Icons.Rounded.Save, null) },
                )
            }
            customPresets.forEach { preset ->
                add {
                    SegmentedListItem(
                        onClick = { onApplyCustomThemePreset(preset.id) },
                        headlineContent = { Text(preset.name) },
                        supportingContent = { Text(stringResource(R.string.theme_custom_preset_item_summary)) },
                        leadingContent = { Icon(Icons.Rounded.Tune, null) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onRenameCustomThemePreset(preset) }) {
                                    Icon(Icons.Rounded.Edit, stringResource(R.string.theme_custom_preset_rename))
                                }
                                IconButton(onClick = { onDeleteCustomThemePreset(preset.id) }) {
                                    Icon(Icons.Rounded.Delete, stringResource(R.string.theme_custom_preset_delete))
                                }
                            }
                        },
                    )
                }
            }
            add {
                SegmentedDropdownItem(
                    icon = Icons.Rounded.SyncAlt,
                    title = stringResource(R.string.theme_sync_strategy),
                    summary = stringResource(R.string.theme_sync_strategy_summary),
                    items = syncItems,
                    selectedIndex = if (themeSyncStrategy == ThemeSyncStrategy.SHARED) 0 else 1,
                    onItemSelected = { index ->
                        onSetThemeSyncStrategy(
                            if (index == 0) ThemeSyncStrategy.SHARED else ThemeSyncStrategy.PER_STYLE
                        )
                    },
                )
            }
            add {
                SegmentedListItem(
                    onClick = onResetThemeToDefault,
                    headlineContent = { Text(stringResource(R.string.theme_reset_default)) },
                    supportingContent = { Text(stringResource(R.string.theme_reset_default_summary)) },
                    leadingContent = { Icon(Icons.Rounded.RestartAlt, null) },
                )
            }
        }
    )
}

@Composable
private fun AdvancedSliderMaterial(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (Float) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = "${(sliderValue * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChangeFinished(sliderValue) },
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCard(
    keyColor: Int,
    isDark: Boolean,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
    presetLabel: String,
    colorModeLabel: String,
    keyColorLabel: String,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight
    val dynamicColor = keyColor == 0

    val colorScheme = if (dynamicColor) {
        val baseScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        rememberDynamicColorScheme(
            seedColor = Color.Unspecified,
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = Color(keyColor),
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
        )

    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier
                    .width(168.dp)
                    .aspectRatio(screenRatio),
                color = colorScheme.background,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurface
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.secondaryContainer)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorScheme.surfaceContainerHighest)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorScheme.surfaceContainerHighest)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.surfaceContainerHigh)
                            )
                        }
                    }

                    Surface(
                        color = colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Home, null, tint = colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemePreviewInfoMaterial(
                    label = stringResource(R.string.theme_current_preset),
                    value = presetLabel,
                    modifier = Modifier.weight(1f)
                )
                ThemePreviewInfoMaterial(
                    label = stringResource(R.string.theme_current_mode),
                    value = colorModeLabel,
                    modifier = Modifier.weight(1f)
                )
            }
            ThemePreviewInfoMaterial(
                label = stringResource(R.string.settings_key_color),
                value = keyColorLabel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ThemePreviewInfoMaterial(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ColorButtonMaterial(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colorScheme = if (color == Color.Unspecified) {
        val baseScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        rememberDynamicColorScheme(
            seedColor = Color.Unspecified,
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = color,
            isDark = isDark,
            style = paletteStyle,
            specVersion = colorSpec,
        )
    }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f)
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}
