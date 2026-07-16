package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.decoration.LocalUiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.LocalUiDecorationScope
import me.weishu.kernelsu.ui.component.decoration.UiBackgroundDecoration
import me.weishu.kernelsu.ui.component.decoration.UiCardDecoration
import me.weishu.kernelsu.ui.component.decoration.UiDecorationBackdrop
import me.weishu.kernelsu.ui.component.decoration.UiDecorationChromeOverlay
import me.weishu.kernelsu.ui.component.decoration.UiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.UiDecorationPreset
import me.weishu.kernelsu.ui.component.decoration.UiDecorationScope
import me.weishu.kernelsu.ui.component.decoration.UiNavigationDecoration
import me.weishu.kernelsu.ui.component.decoration.UiTopBarDecoration
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

private enum class DecorationLibraryTab(@StringRes val labelRes: Int) {
    Library(R.string.ui_decoration_tab_library),
    Current(R.string.ui_decoration_tab_current),
    Preview(R.string.ui_decoration_tab_preview),
}

private enum class DecorationCategory(
    @StringRes val labelRes: Int,
    @StringRes val summaryRes: Int,
    val icon: ImageVector,
) {
    Card(
        R.string.ui_decoration_category_card,
        R.string.ui_decoration_category_card_summary,
        Icons.Rounded.Style,
    ),
    Background(
        R.string.ui_decoration_category_background,
        R.string.ui_decoration_category_background_summary,
        Icons.Rounded.Wallpaper,
    ),
    TopBar(
        R.string.ui_decoration_category_top_bar,
        R.string.ui_decoration_category_top_bar_summary,
        Icons.Rounded.Layers,
    ),
    Navigation(
        R.string.ui_decoration_category_navigation,
        R.string.ui_decoration_category_navigation_summary,
        Icons.Rounded.BlurOn,
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiDecorationLibraryScreen() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var appliedConfig by remember { mutableStateOf(uiState.uiDecorationConfig) }
    var draftConfig by remember { mutableStateOf(uiState.uiDecorationConfig) }
    var selectedTab by rememberSaveable { mutableIntStateOf(DecorationLibraryTab.Library.ordinal) }
    var selectedCategory by rememberSaveable { mutableIntStateOf(DecorationCategory.Card.ordinal) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val hasChanges = draftConfig != appliedConfig

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    LaunchedEffect(uiState.uiDecorationConfig) {
        val hadLocalChanges = draftConfig != appliedConfig
        appliedConfig = uiState.uiDecorationConfig
        if (!hadLocalChanges) {
            draftConfig = uiState.uiDecorationConfig
        }
    }

    val closeScreen = {
        if (hasChanges) showDiscardDialog = true else navigator.pop()
    }
    BackHandler(enabled = hasChanges) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
            title = { Text(stringResource(R.string.ui_decoration_discard_title)) },
            text = { Text(stringResource(R.string.ui_decoration_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        navigator.pop()
                    },
                ) {
                    Text(stringResource(R.string.ui_decoration_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.ui_decoration_keep_editing))
                }
            },
        )
    }

    val applyDraft = {
        val normalized = draftConfig.normalized()
        viewModel.setUiDecorationConfig(normalized)
        appliedConfig = normalized
        draftConfig = normalized
    }

    when (LocalUiMode.current) {
        UiMode.Material -> Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            ),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_ui_decoration_library)) },
                    navigationIcon = {
                        IconButton(onClick = closeScreen) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                )
            },
            bottomBar = {
                DecorationApplyBar(
                    hasChanges = hasChanges,
                    enabled = draftConfig.enabled,
                    onRevert = { draftConfig = appliedConfig },
                    onApply = applyDraft,
                )
            },
        ) { innerPadding ->
            DecorationLibraryBody(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                config = draftConfig,
                uiState = uiState,
                onConfigChange = { draftConfig = it.normalized() },
                onReset = { draftConfig = UiDecorationConfig() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }

        UiMode.Miuix -> MiuixScaffold(
            containerColor = Color.Transparent,
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars
                .add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
            topBar = {
                MiuixTopAppBar(
                    title = stringResource(R.string.settings_ui_decoration_library),
                    color = Color.Transparent,
                    titleColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface,
                    navigationIcon = {
                        MiuixIconButton(onClick = closeScreen) {
                            MiuixIcon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground,
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    },
                )
            },
            bottomBar = {
                DecorationApplyBar(
                    hasChanges = hasChanges,
                    enabled = draftConfig.enabled,
                    onRevert = { draftConfig = appliedConfig },
                    onApply = applyDraft,
                )
            },
        ) { innerPadding ->
            DecorationLibraryBody(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                config = draftConfig,
                uiState = uiState,
                onConfigChange = { draftConfig = it.normalized() },
                onReset = { draftConfig = UiDecorationConfig() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun DecorationLibraryBody(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    selectedCategory: Int,
    onCategorySelected: (Int) -> Unit,
    config: UiDecorationConfig,
    uiState: SettingsUiState,
    onConfigChange: (UiDecorationConfig) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                DecorationLibraryTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        text = {
                            Text(
                                text = stringResource(tab.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }

            when (DecorationLibraryTab.entries.getOrElse(selectedTab) { DecorationLibraryTab.Library }) {
                DecorationLibraryTab.Library -> DecorationComponentsTab(
                    config = config,
                    selectedCategory = DecorationCategory.entries.getOrElse(selectedCategory) {
                        DecorationCategory.Card
                    },
                    onCategorySelected = { onCategorySelected(it.ordinal) },
                    onConfigChange = onConfigChange,
                )
                DecorationLibraryTab.Current -> DecorationCurrentTab(
                    config = config,
                    uiState = uiState,
                    onConfigChange = onConfigChange,
                    onReset = onReset,
                )
                DecorationLibraryTab.Preview -> DecorationPreviewTab(config = config)
            }
        }
    }
}

@Composable
private fun DecorationComponentsTab(
    config: UiDecorationConfig,
    selectedCategory: DecorationCategory,
    onCategorySelected: (DecorationCategory) -> Unit,
    onConfigChange: (UiDecorationConfig) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            DecorationIntro(
                title = stringResource(R.string.ui_decoration_presets),
                summary = stringResource(R.string.ui_decoration_presets_summary),
                icon = Icons.Rounded.AutoAwesome,
            )
        }
        item {
            PresetPicker(
                config = config,
                onPresetSelected = { onConfigChange(config.withPreset(it)) },
            )
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)) }
        item {
            DecorationIntro(
                title = stringResource(R.string.ui_decoration_components),
                summary = stringResource(R.string.ui_decoration_components_summary),
                icon = Icons.Rounded.Palette,
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DecorationCategory.entries, key = { it.name }) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelected(category) },
                        label = { Text(stringResource(category.labelRes)) },
                        leadingIcon = {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(selectedCategory.summaryRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        when (selectedCategory) {
            DecorationCategory.Card -> items(UiCardDecoration.entries, key = { it.value }) { option ->
                DecorationOptionRow(
                    label = stringResource(option.labelRes()),
                    summary = stringResource(selectedCategory.summaryRes),
                    selected = config.card == option,
                    previewConfig = singleSlotPreview(config, selectedCategory, option.value),
                    onClick = { onConfigChange(config.copy(card = option)) },
                )
            }
            DecorationCategory.Background -> items(UiBackgroundDecoration.entries, key = { it.value }) { option ->
                DecorationOptionRow(
                    label = stringResource(option.labelRes()),
                    summary = stringResource(selectedCategory.summaryRes),
                    selected = config.background == option,
                    previewConfig = singleSlotPreview(config, selectedCategory, option.value),
                    onClick = { onConfigChange(config.copy(background = option)) },
                )
            }
            DecorationCategory.TopBar -> items(UiTopBarDecoration.entries, key = { it.value }) { option ->
                DecorationOptionRow(
                    label = stringResource(option.labelRes()),
                    summary = stringResource(selectedCategory.summaryRes),
                    selected = config.topBar == option,
                    previewConfig = singleSlotPreview(config, selectedCategory, option.value),
                    onClick = { onConfigChange(config.copy(topBar = option)) },
                )
            }
            DecorationCategory.Navigation -> items(UiNavigationDecoration.entries, key = { it.value }) { option ->
                DecorationOptionRow(
                    label = stringResource(option.labelRes()),
                    summary = stringResource(selectedCategory.summaryRes),
                    selected = config.navigation == option,
                    previewConfig = singleSlotPreview(config, selectedCategory, option.value),
                    onClick = { onConfigChange(config.copy(navigation = option)) },
                )
            }
        }
    }
}

@Composable
private fun DecorationCurrentTab(
    config: UiDecorationConfig,
    uiState: SettingsUiState,
    onConfigChange: (UiDecorationConfig) -> Unit,
    onReset: () -> Unit,
) {
    val conflicts = decorationConflicts(
        config = config,
        interfaceStyle = LocalInterfaceStyle.current,
        globalSnowEnabled = uiState.globalSnowEnabled,
        nightBackgroundEffect = uiState.nightBackgroundEffect,
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            MasterDecorationSwitch(
                enabled = config.enabled,
                onEnabledChange = { onConfigChange(config.copy(enabled = it)) },
            )
        }
        item {
            DecorationSectionHeading(
                title = stringResource(R.string.ui_decoration_current_combination),
                summary = config.matchingPreset()?.let { stringResource(it.labelRes()) }
                    ?: stringResource(R.string.ui_decoration_preset_custom),
                icon = Icons.Rounded.Layers,
            )
        }
        item {
            CombinationList(config = config)
        }
        item {
            DecorationSectionHeading(
                title = stringResource(R.string.ui_decoration_tuning),
                summary = stringResource(R.string.ui_decoration_tuning_summary),
                icon = Icons.Rounded.Tune,
            )
        }
        item {
            DecorationSlider(
                title = stringResource(R.string.ui_decoration_intensity),
                summary = stringResource(R.string.ui_decoration_intensity_summary),
                value = config.intensity,
                onValueChange = { onConfigChange(config.copy(intensity = it)) },
            )
        }
        item {
            DecorationSlider(
                title = stringResource(R.string.ui_decoration_opacity),
                summary = stringResource(R.string.ui_decoration_opacity_summary),
                value = config.opacity,
                onValueChange = { onConfigChange(config.copy(opacity = it)) },
            )
        }
        item {
            SettingToggleRow(
                title = stringResource(R.string.ui_decoration_motion),
                summary = stringResource(R.string.ui_decoration_motion_summary),
                checked = config.motionEnabled,
                icon = Icons.Rounded.MotionPhotosOn,
                onCheckedChange = { onConfigChange(config.copy(motionEnabled = it)) },
            )
        }
        item {
            DecorationSectionHeading(
                title = stringResource(R.string.ui_decoration_scope),
                summary = stringResource(R.string.ui_decoration_scope_summary),
                icon = Icons.Rounded.Settings,
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(UiDecorationScope.entries, key = { it.value }) { scope ->
                    FilterChip(
                        selected = scope in config.scopes,
                        onClick = {
                            val nextScopes = if (scope in config.scopes) {
                                config.scopes - scope
                            } else {
                                config.scopes + scope
                            }
                            if (nextScopes.isNotEmpty()) {
                                onConfigChange(config.copy(scopes = nextScopes))
                            }
                        },
                        label = { Text(stringResource(scope.labelRes())) },
                        leadingIcon = if (scope in config.scopes) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.ui_decoration_scope_required),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            DecorationSectionHeading(
                title = stringResource(R.string.ui_decoration_compatibility),
                summary = stringResource(R.string.ui_decoration_compatibility_summary),
                icon = if (conflicts.isEmpty()) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
            )
        }
        if (conflicts.isEmpty()) {
            item { CompatibilityNotice(message = stringResource(R.string.ui_decoration_no_conflicts), warning = false) }
        } else {
            items(conflicts) { messageRes ->
                CompatibilityNotice(message = stringResource(messageRes), warning = true)
            }
        }
        item {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ui_decoration_reset_default))
            }
        }
    }
}

@Composable
private fun DecorationPreviewTab(config: UiDecorationConfig) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DecorationIntro(
                title = stringResource(R.string.ui_decoration_preview_title),
                summary = stringResource(R.string.ui_decoration_preview_summary),
                icon = Icons.Rounded.AutoAwesome,
            )
        }
        if (!config.enabled) {
            item {
                CompatibilityNotice(
                    message = stringResource(R.string.ui_decoration_preview_disabled_notice),
                    warning = true,
                )
            }
        }
        item {
            DecorationPhonePreview(
                config = config.copy(enabled = true),
                modifier = Modifier
                    .widthIn(max = 410.dp)
                    .fillMaxWidth()
                    .aspectRatio(0.78f),
            )
        }
        item {
            Text(
                text = stringResource(R.string.ui_decoration_preview_motion_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.widthIn(max = 520.dp),
            )
        }
    }
}

@Composable
private fun DecorationIntro(title: String, summary: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(9.dp).size(22.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PresetPicker(
    config: UiDecorationConfig,
    onPresetSelected: (UiDecorationPreset) -> Unit,
) {
    val selectedPreset = config.matchingPreset()
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 4.dp),
    ) {
        items(UiDecorationPreset.entries, key = { it.value }) { preset ->
            val selected = selectedPreset == preset
            val previewConfig = config.withPreset(preset).copy(enabled = true, motionEnabled = false)
            Surface(
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(188.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPresetSelected(preset) },
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    DecorationMiniPreview(
                        config = previewConfig,
                        modifier = Modifier.fillMaxWidth().height(68.dp),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(preset.labelRes()),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(preset.summaryRes()),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = stringResource(R.string.ui_decoration_selected),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DecorationOptionRow(
    label: String,
    summary: String,
    selected: Boolean,
    previewConfig: UiDecorationConfig,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f)
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DecorationMiniPreview(
                config = previewConfig,
                modifier = Modifier.width(94.dp).height(64.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
                Text(
                    text = summary,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.ui_decoration_selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DecorationMiniPreview(config: UiDecorationConfig, modifier: Modifier = Modifier) {
    val frameShape = RoundedCornerShape(7.dp)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalUiDecorationConfig provides config.copy(enabled = true, motionEnabled = false),
        LocalUiDecorationScope provides UiDecorationScope.Secondary,
    ) {
        Box(
            modifier = modifier
                .clip(frameShape)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            UiDecorationBackdrop(Modifier.fillMaxSize())
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.58f)
                    .height(25.dp)
                    .uiDecoratedCard(RoundedCornerShape(5.dp)),
            ) { }
            UiDecorationChromeOverlay(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun MasterDecorationSwitch(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Surface(
        color = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.ui_decoration_enable),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.ui_decoration_enable_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            StyledSwitch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun DecorationSectionHeading(title: String, summary: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CombinationList(config: UiDecorationConfig) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.84f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            CombinationRow(
                icon = Icons.Rounded.Style,
                title = stringResource(R.string.ui_decoration_category_card),
                value = stringResource(config.card.labelRes()),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            CombinationRow(
                icon = Icons.Rounded.Wallpaper,
                title = stringResource(R.string.ui_decoration_category_background),
                value = stringResource(config.background.labelRes()),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            CombinationRow(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.ui_decoration_category_top_bar),
                value = stringResource(config.topBar.labelRes()),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            CombinationRow(
                icon = Icons.Rounded.BlurOn,
                title = stringResource(R.string.ui_decoration_category_navigation),
                value = stringResource(config.navigation.labelRes()),
            )
        }
    }
}

@Composable
private fun CombinationRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DecorationSlider(
    title: String,
    summary: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = "${(value * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f)
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    summary: String,
    checked: Boolean,
    icon: ImageVector,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        StyledSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CompatibilityNotice(message: String, warning: Boolean) {
    val container = if (warning) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.58f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
    }
    val content = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (warning) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(text = message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DecorationPhonePreview(config: UiDecorationConfig, modifier: Modifier = Modifier) {
    val frameShape = RoundedCornerShape(12.dp)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalUiDecorationConfig provides config,
        LocalUiDecorationScope provides UiDecorationScope.Secondary,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = frameShape,
            tonalElevation = 2.dp,
            modifier = modifier,
        ) {
            Box(Modifier.fillMaxSize().clip(frameShape)) {
                UiDecorationBackdrop(Modifier.fillMaxSize())
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ApkeSU",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(8.dp).size(18.dp),
                            )
                        }
                    }
                    PreviewStatusCard(config = config)
                    PreviewListCard(config = config)
                    Spacer(Modifier.weight(1f))
                    PreviewNavigation()
                }
                UiDecorationChromeOverlay(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PreviewStatusCard(config: UiDecorationConfig) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
        shape = shape,
        modifier = Modifier.fillMaxWidth().uiDecoratedCard(shape),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(6.dp).size(17.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ui_decoration_preview_status),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.ui_decoration_preview_status_summary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))
            Text(
                text = config.matchingPreset()?.let { stringResource(it.labelRes()) }
                    ?: stringResource(R.string.ui_decoration_preset_custom),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun PreviewListCard(config: UiDecorationConfig) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
        shape = shape,
        modifier = Modifier.fillMaxWidth().uiDecoratedCard(shape),
    ) {
        Column {
            PreviewListRow(
                title = stringResource(R.string.ui_decoration_preview_module),
                summary = stringResource(R.string.ui_decoration_preview_module_summary),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            PreviewListRow(
                title = stringResource(R.string.ui_decoration_preview_scope),
                summary = stringResource(R.string.ui_decoration_preview_scope_summary),
            )
        }
    }
}

@Composable
private fun PreviewListRow(title: String, summary: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PreviewNavigation() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf(Icons.Rounded.CheckCircle, Icons.Rounded.Style, Icons.Rounded.Layers, Icons.Rounded.Settings)
                .forEachIndexed { index, icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp),
                    )
                }
        }
    }
}

@Composable
private fun DecorationApplyBar(
    hasChanges: Boolean,
    enabled: Boolean,
    onRevert: () -> Unit,
    onApply: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = if (hasChanges) {
                        stringResource(R.string.ui_decoration_unsaved)
                    } else {
                        stringResource(R.string.ui_decoration_saved)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (enabled) {
                        stringResource(R.string.ui_decoration_status_on)
                    } else {
                        stringResource(R.string.ui_decoration_status_off)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (hasChanges) {
                IconButton(onClick = onRevert) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = stringResource(R.string.ui_decoration_revert_changes),
                    )
                }
            }
            Button(onClick = onApply, enabled = hasChanges) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.ui_decoration_apply))
            }
        }
    }
}

private fun singleSlotPreview(
    config: UiDecorationConfig,
    category: DecorationCategory,
    value: String,
): UiDecorationConfig {
    val empty = config.copy(
        enabled = true,
        card = UiCardDecoration.None,
        background = UiBackgroundDecoration.None,
        topBar = UiTopBarDecoration.None,
        navigation = UiNavigationDecoration.None,
        motionEnabled = false,
        scopes = UiDecorationScope.entries.toSet(),
    )
    return when (category) {
        DecorationCategory.Card -> empty.copy(card = UiCardDecoration.fromValue(value))
        DecorationCategory.Background -> empty.copy(background = UiBackgroundDecoration.fromValue(value))
        DecorationCategory.TopBar -> empty.copy(topBar = UiTopBarDecoration.fromValue(value))
        DecorationCategory.Navigation -> empty.copy(navigation = UiNavigationDecoration.fromValue(value))
    }
}

@StringRes
private fun UiDecorationPreset.labelRes(): Int = when (this) {
    UiDecorationPreset.Refined -> R.string.ui_decoration_preset_refined
    UiDecorationPreset.Blossom -> R.string.ui_decoration_preset_blossom
    UiDecorationPreset.Lotus -> R.string.ui_decoration_preset_lotus
    UiDecorationPreset.Autumn -> R.string.ui_decoration_preset_autumn
    UiDecorationPreset.Winter -> R.string.ui_decoration_preset_winter
    UiDecorationPreset.Tech -> R.string.ui_decoration_preset_tech
}

@StringRes
private fun UiDecorationPreset.summaryRes(): Int = when (this) {
    UiDecorationPreset.Refined -> R.string.ui_decoration_preset_refined_summary
    UiDecorationPreset.Blossom -> R.string.ui_decoration_preset_blossom_summary
    UiDecorationPreset.Lotus -> R.string.ui_decoration_preset_lotus_summary
    UiDecorationPreset.Autumn -> R.string.ui_decoration_preset_autumn_summary
    UiDecorationPreset.Winter -> R.string.ui_decoration_preset_winter_summary
    UiDecorationPreset.Tech -> R.string.ui_decoration_preset_tech_summary
}

@StringRes
private fun UiCardDecoration.labelRes(): Int = when (this) {
    UiCardDecoration.None -> R.string.ui_decoration_component_none
    UiCardDecoration.Highlight -> R.string.ui_decoration_card_highlight
    UiCardDecoration.Blossom -> R.string.ui_decoration_card_blossom
    UiCardDecoration.Lotus -> R.string.ui_decoration_card_lotus
    UiCardDecoration.Maple -> R.string.ui_decoration_card_maple
    UiCardDecoration.Snow -> R.string.ui_decoration_card_snow
    UiCardDecoration.Circuit -> R.string.ui_decoration_card_circuit
}

@StringRes
private fun UiBackgroundDecoration.labelRes(): Int = when (this) {
    UiBackgroundDecoration.None -> R.string.ui_decoration_component_none
    UiBackgroundDecoration.SoftRays -> R.string.ui_decoration_background_soft_rays
    UiBackgroundDecoration.StarMap -> R.string.ui_decoration_background_star_map
    UiBackgroundDecoration.Botanical -> R.string.ui_decoration_background_botanical
    UiBackgroundDecoration.Frost -> R.string.ui_decoration_background_frost
}

@StringRes
private fun UiTopBarDecoration.labelRes(): Int = when (this) {
    UiTopBarDecoration.None -> R.string.ui_decoration_component_none
    UiTopBarDecoration.FineLine -> R.string.ui_decoration_top_bar_fine_line
    UiTopBarDecoration.Prism -> R.string.ui_decoration_top_bar_prism
    UiTopBarDecoration.Seasonal -> R.string.ui_decoration_top_bar_seasonal
    UiTopBarDecoration.Circuit -> R.string.ui_decoration_top_bar_circuit
}

@StringRes
private fun UiNavigationDecoration.labelRes(): Int = when (this) {
    UiNavigationDecoration.None -> R.string.ui_decoration_component_none
    UiNavigationDecoration.UnderGlow -> R.string.ui_decoration_navigation_under_glow
    UiNavigationDecoration.LiquidHalo -> R.string.ui_decoration_navigation_liquid_halo
    UiNavigationDecoration.Orbit -> R.string.ui_decoration_navigation_orbit
    UiNavigationDecoration.MinimalLine -> R.string.ui_decoration_navigation_minimal_line
}

@StringRes
private fun UiDecorationScope.labelRes(): Int = when (this) {
    UiDecorationScope.Home -> R.string.ui_decoration_scope_home
    UiDecorationScope.SuperUser -> R.string.ui_decoration_scope_superuser
    UiDecorationScope.Modules -> R.string.ui_decoration_scope_modules
    UiDecorationScope.Settings -> R.string.ui_decoration_scope_settings
    UiDecorationScope.Secondary -> R.string.ui_decoration_scope_secondary
}

private fun decorationConflicts(
    config: UiDecorationConfig,
    interfaceStyle: String,
    globalSnowEnabled: Boolean,
    nightBackgroundEffect: String,
): List<Int> = buildList {
    if (
        interfaceStyle == InterfaceStyle.Snow.value &&
        config.card in setOf(
            UiCardDecoration.Blossom,
            UiCardDecoration.Lotus,
            UiCardDecoration.Maple,
            UiCardDecoration.Snow,
        )
    ) {
        add(R.string.ui_decoration_conflict_four_seasons)
    }
    if (globalSnowEnabled && (config.card == UiCardDecoration.Snow || config.background == UiBackgroundDecoration.Frost)) {
        add(R.string.ui_decoration_conflict_snow)
    }
    if (
        NightBackgroundEffect.fromValue(nightBackgroundEffect) != NightBackgroundEffect.Off &&
        config.background != UiBackgroundDecoration.None
    ) {
        add(R.string.ui_decoration_conflict_night_background)
    }
}
