package me.weishu.kernelsu.ui.screen.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.custom.LocalCustomCardStyle
import me.weishu.kernelsu.ui.component.decoration.LocalUiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.LocalUiDecorationScope
import me.weishu.kernelsu.ui.component.decoration.PIXEL_CARD_DECORATIONS
import me.weishu.kernelsu.ui.component.decoration.CustomUiDecorationPreset
import me.weishu.kernelsu.ui.component.decoration.UiBackgroundDecoration
import me.weishu.kernelsu.ui.component.decoration.UiCardDecoration
import me.weishu.kernelsu.ui.component.decoration.UiDecorationBackdrop
import me.weishu.kernelsu.ui.component.decoration.UiDecorationChromeOverlay
import me.weishu.kernelsu.ui.component.decoration.UiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.UiDecorationPreset
import me.weishu.kernelsu.ui.component.decoration.UiDecorationScope
import me.weishu.kernelsu.ui.component.decoration.UiNavigationDecoration
import me.weishu.kernelsu.ui.component.decoration.UiTopBarDecoration
import me.weishu.kernelsu.ui.component.decoration.customUiDecorationPresetsFromJson
import me.weishu.kernelsu.ui.component.decoration.customUiDecorationPresetsToJson
import me.weishu.kernelsu.ui.component.decoration.forPreview
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.screen.colorpalette.ThemePresetNameDialog
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.TabRow as MiuixTabRow
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

private enum class DecorationLibraryTab(@StringRes val labelRes: Int) {
    Components(R.string.ui_decoration_tab_components),
    Presets(R.string.ui_decoration_tab_presets),
    Tuning(R.string.ui_decoration_tab_tuning),
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
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    var appliedConfig by remember { mutableStateOf(uiState.uiDecorationConfig) }
    var draftConfig by remember { mutableStateOf(uiState.uiDecorationConfig) }
    var selectedTab by rememberSaveable { mutableIntStateOf(DecorationLibraryTab.Components.ordinal) }
    var selectedCategory by rememberSaveable { mutableIntStateOf(NO_DECORATION_CATEGORY) }
    var showSavePresetDialog by rememberSaveable { mutableStateOf(false) }
    var presetToRename by remember { mutableStateOf<CustomUiDecorationPreset?>(null) }
    var presetToDelete by remember { mutableStateOf<CustomUiDecorationPreset?>(null) }
    val hasChanges = draftConfig != appliedConfig

    val showMessage: (Int) -> Unit = { messageRes ->
        Toast.makeText(context, resources.getString(messageRes), Toast.LENGTH_LONG).show()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(UI_DECORATION_PRESET_MIME_TYPE),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val exported = withContext(Dispatchers.IO) {
                runCatching {
                    val json = customUiDecorationPresetsToJson(uiState.customUiDecorationPresets)
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open preset output")
                }.isSuccess
            }
            showMessage(
                if (exported) R.string.ui_decoration_export_success else R.string.ui_decoration_export_failed
            )
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                        input.reader(Charsets.UTF_8).readText()
                    } ?: error("Unable to open preset input")
                    customUiDecorationPresetsFromJson(raw).also { presets ->
                        require(presets.isNotEmpty()) { "Preset file has no valid entries" }
                    }
                }
            }
            parsed.onSuccess { presets ->
                val count = viewModel.importCustomUiDecorationPresets(presets)
                showMessage(
                    if (count > 0) R.string.ui_decoration_import_success else R.string.ui_decoration_import_failed
                )
            }.onFailure {
                showMessage(R.string.ui_decoration_import_failed)
            }
        }
    }

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
    LaunchedEffect(uiState.uiDecorationSaveState) {
        when (uiState.uiDecorationSaveState) {
            UiDecorationSaveState.Saved -> {
                showMessage(R.string.ui_decoration_save_success)
                viewModel.consumeUiDecorationSaveState()
            }
            UiDecorationSaveState.Failed -> {
                showMessage(R.string.ui_decoration_save_failed)
                viewModel.consumeUiDecorationSaveState()
            }
            UiDecorationSaveState.Idle,
            UiDecorationSaveState.Saving -> Unit
        }
    }

    val discardDialog = rememberConfirmDialog(
        onConfirm = { navigator.pop() },
    )
    val closeScreen: () -> Unit = {
        if (uiState.uiDecorationSaveState == UiDecorationSaveState.Saving) {
            showMessage(R.string.ui_decoration_saving)
        } else if (hasChanges) {
            discardDialog.showConfirm(
                title = resources.getString(R.string.ui_decoration_discard_title),
                content = resources.getString(R.string.ui_decoration_discard_message),
                confirm = resources.getString(R.string.ui_decoration_discard),
                dismiss = resources.getString(R.string.ui_decoration_keep_editing),
            )
        } else {
            navigator.pop()
        }
    }
    val activeCategory = DecorationCategory.entries.getOrNull(selectedCategory)
    val navigateBack: () -> Unit = {
        if (activeCategory != null) {
            selectedCategory = NO_DECORATION_CATEGORY
        } else {
            closeScreen()
        }
    }
    BackHandler(
        enabled = activeCategory != null || hasChanges || uiState.uiDecorationSaveState == UiDecorationSaveState.Saving,
    ) {
        navigateBack()
    }

    ThemePresetNameDialog(
        show = showSavePresetDialog,
        title = stringResource(R.string.ui_decoration_save_custom_preset),
        onDismissRequest = { showSavePresetDialog = false },
        onConfirm = { name ->
            showMessage(
                if (viewModel.saveCustomUiDecorationPreset(name, draftConfig)) {
                    R.string.ui_decoration_custom_preset_saved
                } else {
                    R.string.ui_decoration_custom_preset_save_failed
                }
            )
        },
    )
    presetToRename?.let { preset ->
        ThemePresetNameDialog(
            show = true,
            title = stringResource(R.string.ui_decoration_rename_custom_preset),
            initialName = preset.name,
            onDismissRequest = { presetToRename = null },
            onConfirm = { name ->
                showMessage(
                    if (viewModel.renameCustomUiDecorationPreset(preset.id, name)) {
                        R.string.ui_decoration_custom_preset_renamed
                    } else {
                        R.string.ui_decoration_custom_preset_rename_failed
                    }
                )
            },
        )
    }
    val deleteDialog = rememberConfirmDialog(
        onConfirm = {
            presetToDelete?.let { preset ->
                showMessage(
                    if (viewModel.deleteCustomUiDecorationPreset(preset.id)) {
                        R.string.ui_decoration_custom_preset_deleted
                    } else {
                        R.string.ui_decoration_custom_preset_delete_failed
                    }
                )
            }
            presetToDelete = null
        },
        onDismiss = { presetToDelete = null },
    )
    LaunchedEffect(presetToDelete?.id) {
        presetToDelete?.let { preset ->
            deleteDialog.showConfirm(
                title = resources.getString(R.string.ui_decoration_delete_custom_preset),
                content = resources.getString(R.string.ui_decoration_delete_custom_preset_message, preset.name),
                confirm = resources.getString(R.string.ui_decoration_delete),
                dismiss = resources.getString(android.R.string.cancel),
            )
        }
    }

    val applyDraft = {
        if (uiState.uiDecorationSaveState != UiDecorationSaveState.Saving) {
            viewModel.setUiDecorationConfig(draftConfig.normalized())
        }
    }

    val bodyContent: @Composable (PaddingValues) -> Unit = { innerPadding ->
        DecorationLibraryBody(
            selectedTab = selectedTab,
            onTabSelected = {
                selectedTab = it
                selectedCategory = NO_DECORATION_CATEGORY
            },
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            config = draftConfig,
            uiState = uiState,
            onConfigChange = { draftConfig = it.normalized() },
            onReset = { draftConfig = UiDecorationConfig() },
            onSaveCustomPreset = { showSavePresetDialog = true },
            onRenameCustomPreset = { presetToRename = it },
            onDeleteCustomPreset = { presetToDelete = it },
            onImportCustomPresets = { importLauncher.launch(arrayOf(UI_DECORATION_PRESET_MIME_TYPE, "text/plain")) },
            onExportCustomPresets = { exportLauncher.launch(UI_DECORATION_PRESET_EXPORT_NAME) },
            onShowMessage = showMessage,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }

    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = activeCategory?.let { stringResource(it.labelRes) }
                    ?: stringResource(R.string.settings_ui_decoration_library),
                color = Color.Transparent,
                titleColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface,
                navigationIcon = {
                    MiuixIconButton(onClick = navigateBack) {
                        MiuixIcon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground,
                            contentDescription = activeCategory?.let {
                                stringResource(R.string.ui_decoration_back_to_categories)
                            } ?: stringResource(R.string.close),
                        )
                    }
                },
            )
        },
        bottomBar = {
            DecorationApplyBar(
                hasChanges = hasChanges,
                enabled = draftConfig.enabled,
                saveState = uiState.uiDecorationSaveState,
                onRevert = { draftConfig = appliedConfig },
                onApply = applyDraft,
            )
        },
        content = bodyContent,
    )
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
    onSaveCustomPreset: () -> Unit,
    onRenameCustomPreset: (CustomUiDecorationPreset) -> Unit,
    onDeleteCustomPreset: (CustomUiDecorationPreset) -> Unit,
    onImportCustomPresets: () -> Unit,
    onExportCustomPresets: () -> Unit,
    onShowMessage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interfaceStyle = LocalInterfaceStyle.current
    val effectiveConfig = config.effectiveOnNativePixelSurface(
        pixelStyleActive = interfaceStyle == InterfaceStyle.Pixel.value,
    )
    val conflicts = decorationConflicts(
        config = config,
        interfaceStyle = interfaceStyle,
        globalSnowEnabled = uiState.globalSnowEnabled,
        nightBackgroundEffect = uiState.nightBackgroundEffect,
    )
    BoxWithConstraints(modifier = modifier) {
        val expanded = maxWidth >= 760.dp
        if (expanded) {
            Row(
                modifier = Modifier
                    .widthIn(max = 1120.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            ) {
                DecorationEditorPane(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    selectedCategory = selectedCategory,
                    onCategorySelected = onCategorySelected,
                    config = config,
                    effectiveConfig = effectiveConfig,
                    interfaceStyle = interfaceStyle,
                    uiState = uiState,
                    onConfigChange = onConfigChange,
                    onReset = onReset,
                    onSaveCustomPreset = onSaveCustomPreset,
                    onRenameCustomPreset = onRenameCustomPreset,
                    onDeleteCustomPreset = onDeleteCustomPreset,
                    onImportCustomPresets = onImportCustomPresets,
                    onExportCustomPresets = onExportCustomPresets,
                    onShowMessage = onShowMessage,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider()
                DecorationPreviewPane(
                    configured = config,
                    effective = effectiveConfig,
                    conflicts = conflicts,
                    modifier = Modifier.width(340.dp),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            ) {
                CompactDecorationPreview(
                    configured = config,
                    effective = effectiveConfig,
                    conflicts = conflicts,
                )
                DecorationEditorPane(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    selectedCategory = selectedCategory,
                    onCategorySelected = onCategorySelected,
                    config = config,
                    effectiveConfig = effectiveConfig,
                    interfaceStyle = interfaceStyle,
                    uiState = uiState,
                    onConfigChange = onConfigChange,
                    onReset = onReset,
                    onSaveCustomPreset = onSaveCustomPreset,
                    onRenameCustomPreset = onRenameCustomPreset,
                    onDeleteCustomPreset = onDeleteCustomPreset,
                    onImportCustomPresets = onImportCustomPresets,
                    onExportCustomPresets = onExportCustomPresets,
                    onShowMessage = onShowMessage,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DecorationEditorPane(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    selectedCategory: Int,
    onCategorySelected: (Int) -> Unit,
    config: UiDecorationConfig,
    effectiveConfig: UiDecorationConfig,
    interfaceStyle: String,
    uiState: SettingsUiState,
    onConfigChange: (UiDecorationConfig) -> Unit,
    onReset: () -> Unit,
    onSaveCustomPreset: () -> Unit,
    onRenameCustomPreset: (CustomUiDecorationPreset) -> Unit,
    onDeleteCustomPreset: (CustomUiDecorationPreset) -> Unit,
    onImportCustomPresets: () -> Unit,
    onExportCustomPresets: () -> Unit,
    onShowMessage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeCategory = DecorationCategory.entries.getOrNull(selectedCategory)
    Column(modifier = modifier) {
        if (selectedTab == DecorationLibraryTab.Components.ordinal && activeCategory != null) {
            DecorationCategoryDetail(
                category = activeCategory,
                config = config,
                effectiveConfig = effectiveConfig,
                interfaceStyle = interfaceStyle,
                recentComponents = uiState.recentUiDecorationComponents,
                onConfigChange = onConfigChange,
            )
        } else {
            DecorationTabBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
            when (DecorationLibraryTab.entries.getOrElse(selectedTab) { DecorationLibraryTab.Components }) {
                DecorationLibraryTab.Components -> DecorationComponentsOverview(
                    config = config,
                    effectiveConfig = effectiveConfig,
                    onCategorySelected = { onCategorySelected(it.ordinal) },
                )
                DecorationLibraryTab.Presets -> DecorationPresetsTab(
                    config = config,
                    customPresets = uiState.customUiDecorationPresets,
                    onConfigChange = onConfigChange,
                    onSaveCustomPreset = onSaveCustomPreset,
                    onRenameCustomPreset = onRenameCustomPreset,
                    onDeleteCustomPreset = onDeleteCustomPreset,
                    onImportCustomPresets = onImportCustomPresets,
                    onExportCustomPresets = onExportCustomPresets,
                )
                DecorationLibraryTab.Tuning -> DecorationCurrentTab(
                    config = config,
                    uiState = uiState,
                    onConfigChange = onConfigChange,
                    onReset = onReset,
                    onShowMessage = onShowMessage,
                )
            }
        }
    }
}

@Composable
private fun DecorationTabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val labels = DecorationLibraryTab.entries.map { stringResource(it.labelRes) }
    val activeTab = selectedTab.coerceIn(labels.indices)
    MiuixTabRow(
        tabs = labels,
        selectedTabIndex = activeTab,
        onTabSelected = onTabSelected,
        height = 48.dp,
    )
}

@Composable
private fun DecorationPreviewPane(
    configured: UiDecorationConfig,
    effective: UiDecorationConfig,
    conflicts: List<Int>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            DecorationIntro(
                title = stringResource(R.string.ui_decoration_preview_title),
                summary = stringResource(R.string.ui_decoration_preview_summary),
                icon = Icons.Rounded.AutoAwesome,
            )
        }
        item {
            DecorationPhonePreview(
                config = effective.forPreview(),
                modifier = Modifier.fillMaxWidth().aspectRatio(0.78f),
            )
        }
        if (!configured.enabled) {
            item {
                CompatibilityNotice(
                    message = stringResource(R.string.ui_decoration_preview_disabled_notice),
                    warning = true,
                )
            }
        }
        if (configured != effective) {
            item { EffectiveConfigurationNotice(configured = configured, effective = effective) }
        }
        items(conflicts.take(2), key = { "preview_conflict_$it" }) { messageRes ->
            CompatibilityNotice(message = stringResource(messageRes), warning = true)
        }
        item {
            Text(
                text = stringResource(R.string.ui_decoration_preview_motion_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CompactDecorationPreview(
    configured: UiDecorationConfig,
    effective: UiDecorationConfig,
    conflicts: List<Int>,
) {
    Surface(
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DecorationMiniPreview(
                config = effective.forPreview(),
                modifier = Modifier.width(82.dp).height(52.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.ui_decoration_live_preview),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when {
                        configured != effective -> stringResource(R.string.ui_decoration_effective_differs)
                        conflicts.isNotEmpty() -> pluralStringResource(
                            R.plurals.ui_decoration_conflict_count,
                            conflicts.size,
                            conflicts.size,
                        )
                        configured.enabled -> stringResource(R.string.ui_decoration_status_on)
                        else -> stringResource(R.string.ui_decoration_preview_only)
                    },
                    color = if (conflicts.isNotEmpty()) {
                        MaterialTheme.colorScheme.error
                    } else if (configured != effective) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EffectiveConfigurationNotice(
    configured: UiDecorationConfig,
    effective: UiDecorationConfig,
) {
    val differences = listOfNotNull(
        if (configured.card != effective.card) {
            stringResource(
                R.string.ui_decoration_effective_change_format,
                stringResource(R.string.ui_decoration_category_card),
                stringResource(configured.card.labelRes()),
                stringResource(effective.card.labelRes()),
            )
        } else null,
        if (configured.background != effective.background) {
            stringResource(
                R.string.ui_decoration_effective_change_format,
                stringResource(R.string.ui_decoration_category_background),
                stringResource(configured.background.labelRes()),
                stringResource(effective.background.labelRes()),
            )
        } else null,
        if (configured.topBar != effective.topBar) {
            stringResource(
                R.string.ui_decoration_effective_change_format,
                stringResource(R.string.ui_decoration_category_top_bar),
                stringResource(configured.topBar.labelRes()),
                stringResource(effective.topBar.labelRes()),
            )
        } else null,
        if (configured.navigation != effective.navigation) {
            stringResource(
                R.string.ui_decoration_effective_change_format,
                stringResource(R.string.ui_decoration_category_navigation),
                stringResource(configured.navigation.labelRes()),
                stringResource(effective.navigation.labelRes()),
            )
        } else null,
    )
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = stringResource(R.string.ui_decoration_effective_configuration),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.ui_decoration_effective_configuration_summary),
                style = MaterialTheme.typography.bodySmall,
            )
            differences.forEach { difference ->
                Text(text = difference, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DecorationComponentsOverview(
    config: UiDecorationConfig,
    effectiveConfig: UiDecorationConfig,
    onCategorySelected: (DecorationCategory) -> Unit,
) {
    val navigator = LocalNavigator.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            DecorationIntro(
                title = stringResource(R.string.ui_decoration_component_categories),
                summary = stringResource(R.string.ui_decoration_component_categories_summary),
                icon = Icons.Rounded.Palette,
            )
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { navigator.push(Route.CardStyleCreator) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Brush, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.card_style_creator_entry),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.card_style_creator_entry_summary),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
        }
        items(DecorationCategory.entries, key = { it.name }) { category ->
            DecorationCategoryRow(
                category = category,
                config = config,
                effectiveConfig = effectiveConfig,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun DecorationCategoryRow(
    category: DecorationCategory,
    config: UiDecorationConfig,
    effectiveConfig: UiDecorationConfig,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val effective = category.isEffective(config, effectiveConfig)
    Surface(
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow),
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DecorationMiniPreview(
                config = singleSlotPreview(config, category, category.selectedValue(config)),
                modifier = Modifier.width(76.dp).height(50.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(category.labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        R.string.ui_decoration_category_selected_format,
                        stringResource(category.selectedLabelRes(config)),
                    ),
                    color = if (effective) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = pluralStringResource(
                    R.plurals.ui_decoration_option_count,
                    category.optionCount(),
                    category.optionCount(),
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DecorationPresetsTab(
    config: UiDecorationConfig,
    customPresets: List<CustomUiDecorationPreset>,
    onConfigChange: (UiDecorationConfig) -> Unit,
    onSaveCustomPreset: () -> Unit,
    onRenameCustomPreset: (CustomUiDecorationPreset) -> Unit,
    onDeleteCustomPreset: (CustomUiDecorationPreset) -> Unit,
    onImportCustomPresets: () -> Unit,
    onExportCustomPresets: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            CustomPresetManager(
                config = config,
                presets = customPresets,
                onApply = { onConfigChange(it.config) },
                onSave = onSaveCustomPreset,
                onRename = onRenameCustomPreset,
                onDelete = onDeleteCustomPreset,
                onImport = onImportCustomPresets,
                onExport = onExportCustomPresets,
            )
        }
    }
}

@Composable
private fun DecorationCategoryDetail(
    category: DecorationCategory,
    config: UiDecorationConfig,
    effectiveConfig: UiDecorationConfig,
    interfaceStyle: String,
    recentComponents: List<String>,
    onConfigChange: (UiDecorationConfig) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = query.trim()
    val resources = LocalResources.current
    val customStyleAvailable = LocalCustomCardStyle.current != null
    val cardOptions = UiCardDecoration.entries.filter { option ->
        (option != UiCardDecoration.Custom || customStyleAvailable) && matchesComponentQuery(
            query = normalizedQuery,
            label = resources.getString(option.labelRes()),
            summary = resources.getString(option.summaryRes()),
        )
    }
    val backgroundOptions = UiBackgroundDecoration.entries.filter { option ->
        matchesComponentQuery(
            query = normalizedQuery,
            label = resources.getString(option.labelRes()),
            summary = resources.getString(option.summaryRes()),
        )
    }
    val topBarOptions = UiTopBarDecoration.entries.filter { option ->
        matchesComponentQuery(
            query = normalizedQuery,
            label = resources.getString(option.labelRes()),
            summary = resources.getString(option.summaryRes()),
        )
    }
    val navigationOptions = UiNavigationDecoration.entries.filter { option ->
        (option != UiNavigationDecoration.Custom || customStyleAvailable) && matchesComponentQuery(
            query = normalizedQuery,
            label = resources.getString(option.labelRes()),
            summary = resources.getString(option.summaryRes()),
        )
    }
    val recommendedCards = recommendedCardDecorations(interfaceStyle)
    val cardSections = cardDecorationSections(
        options = cardOptions,
        recommended = recommendedCards,
        recentTokens = recentComponents,
        searchActive = normalizedQuery.isNotEmpty(),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = stringResource(category.summaryRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(MAX_COMPONENT_SEARCH_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.ui_decoration_search_components)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.ui_decoration_clear_search),
                            )
                        }
                    }
                } else {
                    null
                },
            )
        }
        when (category) {
            DecorationCategory.Card -> cardSections.forEach { section ->
                item(key = "card_section_${section.labelRes}") {
                    ComponentGroupHeading(
                        title = stringResource(section.labelRes),
                        count = section.options.size,
                    )
                }
                items(section.options, key = { it.value }) { option ->
                    val selected = config.card == option
                    DecorationOptionRow(
                        label = stringResource(option.labelRes()),
                        summary = stringResource(option.summaryRes()),
                        selected = selected,
                        effective = effectiveConfig.card == option,
                        badge = when {
                            selected && effectiveConfig.card != option -> {
                                stringResource(R.string.ui_decoration_provided_by_interface_style)
                            }
                            option in recommendedCards -> stringResource(R.string.ui_decoration_recommended)
                            else -> null
                        },
                        previewConfig = singleSlotPreview(config, category, option.value),
                        onClick = { onConfigChange(config.copy(card = option)) },
                    )
                }
            }
            DecorationCategory.Background -> items(backgroundOptions, key = { it.value }) { option ->
                val selected = config.background == option
                DecorationOptionRow(
                    label = stringResource(option.labelRes()),
                    summary = stringResource(option.summaryRes()),
                    selected = selected,
                    effective = effectiveConfig.background == option,
                    badge = if (selected && effectiveConfig.background != option) {
                        stringResource(R.string.ui_decoration_provided_by_interface_style)
                    } else {
                        null
                    },
                    previewConfig = singleSlotPreview(config, category, option.value),
                    onClick = { onConfigChange(config.copy(background = option)) },
                )
            }
            DecorationCategory.TopBar -> items(topBarOptions, key = { it.value }) { option ->
                val selected = config.topBar == option
                DecorationOptionRow(
                    label = stringResource(option.labelRes()),
                    summary = stringResource(option.summaryRes()),
                    selected = selected,
                    effective = effectiveConfig.topBar == option,
                    badge = if (selected && effectiveConfig.topBar != option) {
                        stringResource(R.string.ui_decoration_provided_by_interface_style)
                    } else {
                        null
                    },
                    previewConfig = singleSlotPreview(config, category, option.value),
                    onClick = { onConfigChange(config.copy(topBar = option)) },
                )
            }
            DecorationCategory.Navigation -> items(navigationOptions, key = { it.value }) { option ->
                val selected = config.navigation == option
                DecorationOptionRow(
                    label = stringResource(option.labelRes()),
                    summary = stringResource(option.summaryRes()),
                    selected = selected,
                    effective = effectiveConfig.navigation == option,
                    badge = if (selected && effectiveConfig.navigation != option) {
                        stringResource(R.string.ui_decoration_provided_by_interface_style)
                    } else {
                        null
                    },
                    previewConfig = singleSlotPreview(config, category, option.value),
                    onClick = { onConfigChange(config.copy(navigation = option)) },
                )
            }
        }
        val noResults = when (category) {
            DecorationCategory.Card -> cardOptions.isEmpty()
            DecorationCategory.Background -> backgroundOptions.isEmpty()
            DecorationCategory.TopBar -> topBarOptions.isEmpty()
            DecorationCategory.Navigation -> navigationOptions.isEmpty()
        }
        if (noResults) {
            item {
                EmptyComponentSearch(query = normalizedQuery, onClear = { query = "" })
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
    onShowMessage: (Int) -> Unit,
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
            DecorationScopePicker(
                selected = config.scopes,
                onSelectedChange = { onConfigChange(config.copy(scopes = it)) },
                onRejectEmpty = { onShowMessage(R.string.ui_decoration_scope_required) },
            )
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
private fun DecorationScopePicker(
    selected: Set<UiDecorationScope>,
    onSelectedChange: (Set<UiDecorationScope>) -> Unit,
    onRejectEmpty: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UiDecorationScope.entries.chunked(2).forEach { scopes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                scopes.forEach { scope ->
                    val checked = scope in selected
                    FilterChip(
                        selected = checked,
                        onClick = {
                            val next = if (checked) selected - scope else selected + scope
                            if (next.isEmpty()) onRejectEmpty() else onSelectedChange(next)
                        },
                        label = {
                            Text(
                                text = stringResource(scope.labelRes()),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = if (checked) {
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
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(2 - scopes.size) { Spacer(Modifier.weight(1f)) }
            }
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
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(UiDecorationPreset.entries, key = { it.value }) { preset ->
            val selected = selectedPreset == preset
            val presetPreview = config.withPreset(preset).copy(motionEnabled = false)
            val shape = RoundedCornerShape(8.dp)
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
                shape = shape,
                modifier = Modifier
                    .width(184.dp)
                    .height(116.dp)
                    .clip(shape)
                    .selectable(
                        selected = selected,
                        onClick = { onPresetSelected(preset) },
                        role = Role.RadioButton,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DecorationMiniPreview(
                        config = presetPreview,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(preset.labelRes()),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(preset.summaryRes()),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = stringResource(R.string.ui_decoration_selected),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomPresetManager(
    config: UiDecorationConfig,
    presets: List<CustomUiDecorationPreset>,
    onApply: (CustomUiDecorationPreset) -> Unit,
    onSave: () -> Unit,
    onRename: (CustomUiDecorationPreset) -> Unit,
    onDelete: (CustomUiDecorationPreset) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ui_decoration_custom_presets),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.ui_decoration_custom_presets_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = onSave) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.ui_decoration_save_custom_preset_short))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.ui_decoration_import))
            }
            OutlinedButton(
                onClick = onExport,
                enabled = presets.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.ui_decoration_export))
            }
        }
        if (presets.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.ui_decoration_custom_presets_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(14.dp),
                )
            }
        } else {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { preset ->
                    val selected = preset.config.normalized() == config.normalized()
                    Surface(
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                onClick = { onApply(preset) },
                                role = Role.RadioButton,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            DecorationMiniPreview(
                                config = preset.config.forPreview(),
                                modifier = Modifier.width(82.dp).height(54.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = preset.config.matchingPreset()?.let { stringResource(it.labelRes()) }
                                        ?: stringResource(R.string.ui_decoration_preset_custom),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { onRename(preset) }) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = stringResource(R.string.ui_decoration_rename_custom_preset),
                                )
                            }
                            IconButton(onClick = { onDelete(preset) }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.ui_decoration_delete_custom_preset),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentGroupHeading(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun EmptyComponentSearch(query: String, onClear: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(30.dp),
        )
        Text(
            text = stringResource(R.string.ui_decoration_search_empty, query),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.ui_decoration_clear_search))
        }
    }
}

@Composable
private fun DecorationOptionRow(
    label: String,
    summary: String,
    selected: Boolean,
    effective: Boolean,
    badge: String?,
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
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DecorationMiniPreview(
                config = previewConfig,
                modifier = Modifier.width(76.dp).height(50.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                badge?.let {
                    Text(
                        text = it,
                        color = if (selected && !effective) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.ui_decoration_selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun DecorationMiniPreview(config: UiDecorationConfig, modifier: Modifier = Modifier) {
    val frameShape = RoundedCornerShape(7.dp)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalUiDecorationConfig provides config.forPreview().copy(motionEnabled = false),
        LocalUiDecorationScope provides UiDecorationScope.Secondary,
    ) {
        Box(
            modifier = modifier
                .clip(frameShape)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            UiDecorationBackdrop(Modifier.fillMaxSize())
            Surface(
                color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerHigh),
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
        MiuixSlider(value = value, onValueChange = onValueChange, valueRange = 0f..1f)
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
        LocalUiDecorationConfig provides config.forPreview(),
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
    saveState: UiDecorationSaveState,
    onRevert: () -> Unit,
    onApply: () -> Unit,
) {
    val saving = saveState == UiDecorationSaveState.Saving
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
                        if (saving) {
                            stringResource(R.string.ui_decoration_saving)
                        } else {
                            stringResource(R.string.ui_decoration_unsaved)
                        }
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
            if (hasChanges && !saving) {
                IconButton(onClick = onRevert) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = stringResource(R.string.ui_decoration_revert_changes),
                    )
                }
            }
            Button(onClick = onApply, enabled = hasChanges && !saving) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    stringResource(
                        if (saving) R.string.ui_decoration_saving else R.string.ui_decoration_apply
                    )
                )
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

private fun DecorationCategory.selectedValue(config: UiDecorationConfig): String = when (this) {
    DecorationCategory.Card -> config.card.value
    DecorationCategory.Background -> config.background.value
    DecorationCategory.TopBar -> config.topBar.value
    DecorationCategory.Navigation -> config.navigation.value
}

@StringRes
private fun DecorationCategory.selectedLabelRes(config: UiDecorationConfig): Int = when (this) {
    DecorationCategory.Card -> config.card.labelRes()
    DecorationCategory.Background -> config.background.labelRes()
    DecorationCategory.TopBar -> config.topBar.labelRes()
    DecorationCategory.Navigation -> config.navigation.labelRes()
}

private fun DecorationCategory.optionCount(): Int = when (this) {
    DecorationCategory.Card -> UiCardDecoration.entries.size
    DecorationCategory.Background -> UiBackgroundDecoration.entries.size
    DecorationCategory.TopBar -> UiTopBarDecoration.entries.size
    DecorationCategory.Navigation -> UiNavigationDecoration.entries.size
}

private fun DecorationCategory.isEffective(
    configured: UiDecorationConfig,
    effective: UiDecorationConfig,
): Boolean = when (this) {
    DecorationCategory.Card -> configured.card == effective.card
    DecorationCategory.Background -> configured.background == effective.background
    DecorationCategory.TopBar -> configured.topBar == effective.topBar
    DecorationCategory.Navigation -> configured.navigation == effective.navigation
}

@StringRes
private fun UiDecorationPreset.labelRes(): Int = when (this) {
    UiDecorationPreset.Refined -> R.string.ui_decoration_preset_refined
    UiDecorationPreset.Blossom -> R.string.ui_decoration_preset_blossom
    UiDecorationPreset.Lotus -> R.string.ui_decoration_preset_lotus
    UiDecorationPreset.Autumn -> R.string.ui_decoration_preset_autumn
    UiDecorationPreset.Winter -> R.string.ui_decoration_preset_winter
    UiDecorationPreset.Tech -> R.string.ui_decoration_preset_tech
    UiDecorationPreset.Pixel -> R.string.ui_decoration_preset_pixel
}

@StringRes
private fun UiDecorationPreset.summaryRes(): Int = when (this) {
    UiDecorationPreset.Refined -> R.string.ui_decoration_preset_refined_summary
    UiDecorationPreset.Blossom -> R.string.ui_decoration_preset_blossom_summary
    UiDecorationPreset.Lotus -> R.string.ui_decoration_preset_lotus_summary
    UiDecorationPreset.Autumn -> R.string.ui_decoration_preset_autumn_summary
    UiDecorationPreset.Winter -> R.string.ui_decoration_preset_winter_summary
    UiDecorationPreset.Tech -> R.string.ui_decoration_preset_tech_summary
    UiDecorationPreset.Pixel -> R.string.ui_decoration_preset_pixel_summary
}

@StringRes
private fun UiCardDecoration.labelRes(): Int = when (this) {
    UiCardDecoration.None -> R.string.ui_decoration_component_none
    UiCardDecoration.Custom -> R.string.ui_decoration_card_custom
    UiCardDecoration.Highlight -> R.string.ui_decoration_card_highlight
    UiCardDecoration.Blossom -> R.string.ui_decoration_card_blossom
    UiCardDecoration.Lotus -> R.string.ui_decoration_card_lotus
    UiCardDecoration.Maple -> R.string.ui_decoration_card_maple
    UiCardDecoration.Snow -> R.string.ui_decoration_card_snow
    UiCardDecoration.Circuit -> R.string.ui_decoration_card_circuit
    UiCardDecoration.PixelFrame -> R.string.ui_decoration_card_pixel_frame
    UiCardDecoration.PixelHandheld -> R.string.ui_decoration_card_pixel_handheld
    UiCardDecoration.PixelArcade -> R.string.ui_decoration_card_pixel_arcade
    UiCardDecoration.PixelPastoral -> R.string.ui_decoration_card_pixel_pastoral
    UiCardDecoration.PixelStarVoyage -> R.string.ui_decoration_card_pixel_star_voyage
    UiCardDecoration.PixelInkJade -> R.string.ui_decoration_card_pixel_ink_jade
    UiCardDecoration.PixelWasteland -> R.string.ui_decoration_card_pixel_wasteland
    UiCardDecoration.PixelOcean -> R.string.ui_decoration_card_pixel_ocean
    UiCardDecoration.PixelCyber -> R.string.ui_decoration_card_pixel_cyber
    UiCardDecoration.PixelThreeKingdoms -> R.string.ui_decoration_card_pixel_three_kingdoms
    UiCardDecoration.PixelBianliang -> R.string.ui_decoration_card_pixel_bianliang
    UiCardDecoration.PixelFishingHarbor -> R.string.ui_decoration_card_pixel_fishing_harbor
    UiCardDecoration.PixelTribalJungle -> R.string.ui_decoration_card_pixel_tribal_jungle
    UiCardDecoration.PixelLavaValley -> R.string.ui_decoration_card_pixel_lava_valley
    UiCardDecoration.PixelDunhuangDesert -> R.string.ui_decoration_card_pixel_dunhuang_desert
    UiCardDecoration.PixelVikingSnowfield -> R.string.ui_decoration_card_pixel_viking_snowfield
    UiCardDecoration.PixelJiangnanWatertown -> R.string.ui_decoration_card_pixel_jiangnan_watertown
    UiCardDecoration.PixelCloudTown -> R.string.ui_decoration_card_pixel_cloud_town
}

@StringRes
private fun UiCardDecoration.summaryRes(): Int = when (this) {
    UiCardDecoration.None -> R.string.ui_decoration_component_none_summary
    UiCardDecoration.Custom -> R.string.ui_decoration_card_custom_summary
    UiCardDecoration.Highlight -> R.string.ui_decoration_card_highlight_summary
    UiCardDecoration.Blossom -> R.string.ui_decoration_card_blossom_summary
    UiCardDecoration.Lotus -> R.string.ui_decoration_card_lotus_summary
    UiCardDecoration.Maple -> R.string.ui_decoration_card_maple_summary
    UiCardDecoration.Snow -> R.string.ui_decoration_card_snow_summary
    UiCardDecoration.Circuit -> R.string.ui_decoration_card_circuit_summary
    UiCardDecoration.PixelFrame -> R.string.ui_decoration_card_pixel_frame_summary
    UiCardDecoration.PixelHandheld -> R.string.ui_decoration_card_pixel_handheld_summary
    UiCardDecoration.PixelArcade -> R.string.ui_decoration_card_pixel_arcade_summary
    UiCardDecoration.PixelPastoral -> R.string.ui_decoration_card_pixel_pastoral_summary
    UiCardDecoration.PixelStarVoyage -> R.string.ui_decoration_card_pixel_star_voyage_summary
    UiCardDecoration.PixelInkJade -> R.string.ui_decoration_card_pixel_ink_jade_summary
    UiCardDecoration.PixelWasteland -> R.string.ui_decoration_card_pixel_wasteland_summary
    UiCardDecoration.PixelOcean -> R.string.ui_decoration_card_pixel_ocean_summary
    UiCardDecoration.PixelCyber -> R.string.ui_decoration_card_pixel_cyber_summary
    UiCardDecoration.PixelThreeKingdoms -> R.string.ui_decoration_card_pixel_three_kingdoms_summary
    UiCardDecoration.PixelBianliang -> R.string.ui_decoration_card_pixel_bianliang_summary
    UiCardDecoration.PixelFishingHarbor -> R.string.ui_decoration_card_pixel_fishing_harbor_summary
    UiCardDecoration.PixelTribalJungle -> R.string.ui_decoration_card_pixel_tribal_jungle_summary
    UiCardDecoration.PixelLavaValley -> R.string.ui_decoration_card_pixel_lava_valley_summary
    UiCardDecoration.PixelDunhuangDesert -> R.string.ui_decoration_card_pixel_dunhuang_desert_summary
    UiCardDecoration.PixelVikingSnowfield -> R.string.ui_decoration_card_pixel_viking_snowfield_summary
    UiCardDecoration.PixelJiangnanWatertown -> R.string.ui_decoration_card_pixel_jiangnan_watertown_summary
    UiCardDecoration.PixelCloudTown -> R.string.ui_decoration_card_pixel_cloud_town_summary
}

@StringRes
private fun UiBackgroundDecoration.labelRes(): Int = when (this) {
    UiBackgroundDecoration.None -> R.string.ui_decoration_component_none
    UiBackgroundDecoration.SoftRays -> R.string.ui_decoration_background_soft_rays
    UiBackgroundDecoration.StarMap -> R.string.ui_decoration_background_star_map
    UiBackgroundDecoration.Botanical -> R.string.ui_decoration_background_botanical
    UiBackgroundDecoration.Frost -> R.string.ui_decoration_background_frost
    UiBackgroundDecoration.PixelGrid -> R.string.ui_decoration_background_pixel_grid
}

@StringRes
private fun UiBackgroundDecoration.summaryRes(): Int = when (this) {
    UiBackgroundDecoration.None -> R.string.ui_decoration_component_none_summary
    UiBackgroundDecoration.SoftRays -> R.string.ui_decoration_background_soft_rays_summary
    UiBackgroundDecoration.StarMap -> R.string.ui_decoration_background_star_map_summary
    UiBackgroundDecoration.Botanical -> R.string.ui_decoration_background_botanical_summary
    UiBackgroundDecoration.Frost -> R.string.ui_decoration_background_frost_summary
    UiBackgroundDecoration.PixelGrid -> R.string.ui_decoration_background_pixel_grid_summary
}

@StringRes
private fun UiTopBarDecoration.labelRes(): Int = when (this) {
    UiTopBarDecoration.None -> R.string.ui_decoration_component_none
    UiTopBarDecoration.FineLine -> R.string.ui_decoration_top_bar_fine_line
    UiTopBarDecoration.Prism -> R.string.ui_decoration_top_bar_prism
    UiTopBarDecoration.Seasonal -> R.string.ui_decoration_top_bar_seasonal
    UiTopBarDecoration.Circuit -> R.string.ui_decoration_top_bar_circuit
    UiTopBarDecoration.PixelHud -> R.string.ui_decoration_top_bar_pixel_hud
}

@StringRes
private fun UiTopBarDecoration.summaryRes(): Int = when (this) {
    UiTopBarDecoration.None -> R.string.ui_decoration_component_none_summary
    UiTopBarDecoration.FineLine -> R.string.ui_decoration_top_bar_fine_line_summary
    UiTopBarDecoration.Prism -> R.string.ui_decoration_top_bar_prism_summary
    UiTopBarDecoration.Seasonal -> R.string.ui_decoration_top_bar_seasonal_summary
    UiTopBarDecoration.Circuit -> R.string.ui_decoration_top_bar_circuit_summary
    UiTopBarDecoration.PixelHud -> R.string.ui_decoration_top_bar_pixel_hud_summary
}

@StringRes
private fun UiNavigationDecoration.labelRes(): Int = when (this) {
    UiNavigationDecoration.None -> R.string.ui_decoration_component_none
    UiNavigationDecoration.Custom -> R.string.ui_decoration_navigation_custom
    UiNavigationDecoration.UnderGlow -> R.string.ui_decoration_navigation_under_glow
    UiNavigationDecoration.LiquidHalo -> R.string.ui_decoration_navigation_liquid_halo
    UiNavigationDecoration.Orbit -> R.string.ui_decoration_navigation_orbit
    UiNavigationDecoration.MinimalLine -> R.string.ui_decoration_navigation_minimal_line
    UiNavigationDecoration.PixelDock -> R.string.ui_decoration_navigation_pixel_dock
}

@StringRes
private fun UiNavigationDecoration.summaryRes(): Int = when (this) {
    UiNavigationDecoration.None -> R.string.ui_decoration_component_none_summary
    UiNavigationDecoration.Custom -> R.string.ui_decoration_navigation_custom_summary
    UiNavigationDecoration.UnderGlow -> R.string.ui_decoration_navigation_under_glow_summary
    UiNavigationDecoration.LiquidHalo -> R.string.ui_decoration_navigation_liquid_halo_summary
    UiNavigationDecoration.Orbit -> R.string.ui_decoration_navigation_orbit_summary
    UiNavigationDecoration.MinimalLine -> R.string.ui_decoration_navigation_minimal_line_summary
    UiNavigationDecoration.PixelDock -> R.string.ui_decoration_navigation_pixel_dock_summary
}

@StringRes
private fun UiDecorationScope.labelRes(): Int = when (this) {
    UiDecorationScope.Home -> R.string.ui_decoration_scope_home
    UiDecorationScope.SuperUser -> R.string.ui_decoration_scope_superuser
    UiDecorationScope.Modules -> R.string.ui_decoration_scope_modules
    UiDecorationScope.Settings -> R.string.ui_decoration_scope_settings
    UiDecorationScope.Secondary -> R.string.ui_decoration_scope_secondary
}

private data class CardDecorationSection(
    @StringRes val labelRes: Int,
    val options: List<UiCardDecoration>,
)

private fun cardDecorationSections(
    options: List<UiCardDecoration>,
    recommended: List<UiCardDecoration>,
    recentTokens: List<String>,
    searchActive: Boolean,
): List<CardDecorationSection> {
    if (options.isEmpty()) return emptyList()
    if (searchActive) {
        return listOf(CardDecorationSection(R.string.ui_decoration_group_search_results, options))
    }
    val remaining = options.toMutableList()
    fun takeSection(@StringRes labelRes: Int, candidates: List<UiCardDecoration>): CardDecorationSection? {
        val selected = candidates.distinct().filter(remaining::contains)
        remaining.removeAll(selected.toSet())
        return selected.takeIf { it.isNotEmpty() }?.let { CardDecorationSection(labelRes, it) }
    }
    val recent = recentTokens
        .filter { it.startsWith("card:") }
        .map { UiCardDecoration.fromValue(it.substringAfter(':')) }
    return listOfNotNull(
        takeSection(R.string.ui_decoration_group_recommended, recommended.take(MAX_FEATURED_COMPONENTS)),
        takeSection(R.string.ui_decoration_group_recent, recent.take(MAX_FEATURED_COMPONENTS)),
        takeSection(
            R.string.ui_decoration_group_basic,
            listOf(UiCardDecoration.None, UiCardDecoration.Highlight, UiCardDecoration.Circuit),
        ),
        takeSection(
            R.string.ui_decoration_group_seasonal,
            listOf(
                UiCardDecoration.Blossom,
                UiCardDecoration.Lotus,
                UiCardDecoration.Maple,
                UiCardDecoration.Snow,
            ),
        ),
        takeSection(R.string.ui_decoration_group_pixel, PIXEL_CARD_DECORATIONS.toList()),
        remaining.takeIf { it.isNotEmpty() }?.let {
            CardDecorationSection(R.string.ui_decoration_group_other, it.toList())
        },
    )
}

private fun recommendedCardDecorations(interfaceStyle: String): List<UiCardDecoration> = when (interfaceStyle) {
    InterfaceStyle.Pixel.value -> listOf(
        UiCardDecoration.PixelFrame,
        UiCardDecoration.PixelPastoral,
        UiCardDecoration.PixelInkJade,
        UiCardDecoration.PixelOcean,
        UiCardDecoration.PixelCyber,
        UiCardDecoration.PixelThreeKingdoms,
        UiCardDecoration.PixelBianliang,
        UiCardDecoration.PixelFishingHarbor,
        UiCardDecoration.PixelTribalJungle,
        UiCardDecoration.PixelLavaValley,
        UiCardDecoration.PixelDunhuangDesert,
        UiCardDecoration.PixelVikingSnowfield,
        UiCardDecoration.PixelJiangnanWatertown,
        UiCardDecoration.PixelCloudTown,
    )
    InterfaceStyle.Snow.value -> listOf(
        UiCardDecoration.Blossom,
        UiCardDecoration.Lotus,
        UiCardDecoration.Maple,
        UiCardDecoration.Snow,
    )
    else -> listOf(UiCardDecoration.Highlight, UiCardDecoration.Circuit)
}

private fun matchesComponentQuery(query: String, label: String, summary: String): Boolean {
    if (query.isBlank()) return true
    return label.contains(query, ignoreCase = true) || summary.contains(query, ignoreCase = true)
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

private const val UI_DECORATION_PRESET_MIME_TYPE = "application/json"
private const val UI_DECORATION_PRESET_EXPORT_NAME = "ApkeSU_UI_Decoration_Presets.json"
private const val MAX_COMPONENT_SEARCH_LENGTH = 60
private const val MAX_FEATURED_COMPONENTS = 4
private const val NO_DECORATION_CATEGORY = -1
