package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.custom.ComponentStyleKind
import me.weishu.kernelsu.ui.component.custom.ComponentStyleStore
import me.weishu.kernelsu.ui.component.custom.CustomSwitchSource
import me.weishu.kernelsu.ui.component.custom.CustomSwitchStyle
import me.weishu.kernelsu.ui.component.custom.DEFAULT_PIXEL_PALETTE
import me.weishu.kernelsu.ui.component.custom.PixelEditToolbar
import me.weishu.kernelsu.ui.component.custom.PixelEditorSection
import me.weishu.kernelsu.ui.component.custom.PixelGrid
import me.weishu.kernelsu.ui.component.custom.PixelGridEditor
import me.weishu.kernelsu.ui.component.custom.PixelMotionEditor
import me.weishu.kernelsu.ui.component.custom.PixelPaletteEditor
import me.weishu.kernelsu.ui.component.custom.SWITCH_THUMB_GRID_SIZE
import me.weishu.kernelsu.ui.component.custom.SWITCH_TRACK_GRID_HEIGHT
import me.weishu.kernelsu.ui.component.custom.SWITCH_TRACK_GRID_WIDTH
import me.weishu.kernelsu.ui.component.custom.SwitchImageScale
import me.weishu.kernelsu.ui.component.custom.TRANSPARENT_PIXEL
import me.weishu.kernelsu.ui.component.custom.drawCustomSwitchStyle
import me.weishu.kernelsu.ui.component.custom.filled
import me.weishu.kernelsu.ui.component.custom.hasSameDimensionsAs
import me.weishu.kernelsu.ui.component.custom.mirroredHorizontally
import me.weishu.kernelsu.ui.component.custom.rememberComponentMotionProgress
import me.weishu.kernelsu.ui.component.custom.rememberCustomSwitchImage
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.THEME_STORE_FILE_MIME_TYPE
import me.weishu.kernelsu.ui.util.canonicalCloudThemePackageFileName
import me.weishu.kernelsu.ui.util.exportSwitchComponentStylePackage
import me.weishu.kernelsu.ui.util.prepareSwitchStyleCloudSubmission
import me.weishu.kernelsu.ui.util.readComponentStylePackage
import kotlin.math.roundToInt
import kotlin.math.sqrt

private enum class SwitchCreatorPage {
    Design,
    Motion,
    Library,
}

private enum class SwitchPixelLayer {
    TrackOff,
    TrackOn,
    ThumbOff,
    ThumbOn,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchStyleCreatorScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val store = remember(context) { ComponentStyleStore(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultName = stringResource(R.string.switch_style_creator_default_name)
    val saveSuccess = stringResource(R.string.component_creator_saved)
    val applySuccess = stringResource(R.string.component_creator_applied)
    val importSuccess = stringResource(R.string.component_creator_imported)
    val exportSuccess = stringResource(R.string.component_creator_exported)
    val imageSuccess = stringResource(R.string.switch_style_creator_image_saved)
    val nameRequired = stringResource(R.string.component_creator_name_required)
    val imageRequired = stringResource(R.string.switch_style_creator_image_required)
    val saveFailed = stringResource(R.string.component_creator_save_failed)
    val deleteFailed = stringResource(R.string.component_creator_delete_failed)
    val cloudDescription = stringResource(R.string.switch_style_creator_cloud_description)
    val cloudCategory = stringResource(R.string.switch_style_creator_cloud_category)

    var styles by remember { mutableStateOf(store.readSwitchStyles()) }
    val initialStyle = remember {
        store.readActiveSwitchStyle() ?: styles.firstOrNull() ?: starterSwitchStyle(defaultName)
    }
    val switchSaver = remember {
        Saver<CustomSwitchStyle, String>(
            save = { it.toJsonString(includeLocalImageUri = true) },
            restore = { raw ->
                runCatching { CustomSwitchStyle.fromJsonString(raw, allowLocalImageUri = true) }.getOrNull()
            },
        )
    }
    var draft by rememberSaveable(stateSaver = switchSaver) { mutableStateOf(initialStyle) }
    var baseline by remember { mutableStateOf(initialStyle) }
    var selectedPage by rememberSaveable { mutableIntStateOf(SwitchCreatorPage.Design.ordinal) }
    var selectedLayer by rememberSaveable { mutableIntStateOf(SwitchPixelLayer.TrackOff.ordinal) }
    var selectedColor by rememberSaveable { mutableLongStateOf(DEFAULT_PIXEL_PALETTE[3]) }
    var previewChecked by rememberSaveable { mutableStateOf(true) }
    var undoStack by remember { mutableStateOf<List<PixelGrid>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<PixelGrid>>(emptyList()) }
    var exportSnapshot by remember { mutableStateOf<CustomSwitchStyle?>(null) }
    var deleteCandidate by remember { mutableStateOf<CustomSwitchStyle?>(null) }
    var busy by remember { mutableStateOf(false) }
    val transientImages = remember { linkedSetOf<String>() }
    val requestedLayer = SwitchPixelLayer.entries.getOrElse(selectedLayer) { SwitchPixelLayer.TrackOff }
    val currentLayer = if (
        draft.source == CustomSwitchSource.Image &&
        requestedLayer != SwitchPixelLayer.ThumbOff &&
        requestedLayer != SwitchPixelLayer.ThumbOn
    ) {
        SwitchPixelLayer.ThumbOff
    } else {
        requestedLayer
    }
    val editorKey = "${draft.source.name}:${currentLayer.name}"

    fun currentGrid(): PixelGrid = when (currentLayer) {
        SwitchPixelLayer.TrackOff -> draft.trackOff
        SwitchPixelLayer.TrackOn -> draft.trackOn
        SwitchPixelLayer.ThumbOff -> draft.thumbOff
        SwitchPixelLayer.ThumbOn -> draft.thumbOn
    }

    fun updateGrid(grid: PixelGrid) {
        if (!grid.hasSameDimensionsAs(currentGrid())) {
            undoStack = emptyList()
            redoStack = emptyList()
            return
        }
        draft = when (currentLayer) {
            SwitchPixelLayer.TrackOff -> draft.copy(trackOff = grid)
            SwitchPixelLayer.TrackOn -> draft.copy(trackOn = grid)
            SwitchPixelLayer.ThumbOff -> draft.copy(thumbOff = grid)
            SwitchPixelLayer.ThumbOn -> draft.copy(thumbOn = grid)
        }
    }

    fun pushUndo(snapshot: PixelGrid) {
        undoStack = (undoStack + snapshot).takeLast(MAX_SWITCH_PIXEL_HISTORY)
        redoStack = emptyList()
    }

    fun showMessage(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun refreshAfterSave(styleId: String) {
        styles = store.readSwitchStyles()
        val stored = styles.firstOrNull { it.id == styleId }
        if (stored != null) {
            draft = stored
            baseline = stored
        }
    }

    fun validateDraft(style: CustomSwitchStyle): Boolean {
        if (style.name.isBlank()) {
            showMessage(nameRequired)
            return false
        }
        if (style.source == CustomSwitchSource.Image && store.resolveImageFile(style.imageUri) == null) {
            showMessage(imageRequired)
            return false
        }
        return true
    }

    fun saveDraft(apply: Boolean): Boolean {
        if (!validateDraft(draft)) return false
        val normalized = draft.normalized()
        if (!store.saveSwitchStyle(normalized, apply)) {
            showMessage(saveFailed)
            return false
        }
        refreshAfterSave(normalized.id)
        showMessage(if (apply) applySuccess else saveSuccess)
        return true
    }

    LaunchedEffect(currentLayer) {
        selectedLayer = currentLayer.ordinal
        undoStack = emptyList()
        redoStack = emptyList()
    }

    DisposableEffect(store) {
        onDispose {
            transientImages.forEach { uri -> store.discardSwitchImageIfUnreferenced(uri) }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (busy) return@rememberLauncherForActivityResult
        busy = true
        coroutineScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { store.persistSwitchImage(uri) }
            }.onSuccess { stored ->
                transientImages += stored.uriString
                draft = draft.copy(
                    source = CustomSwitchSource.Image,
                    imageUri = stored.uriString,
                    imageSha256 = stored.sha256,
                    imageMimeType = stored.mimeType,
                ).normalized()
                showMessage(imageSuccess)
            }.onFailure { error ->
                showMessage(error.switchEditorMessage(context))
            }
            busy = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (busy) return@rememberLauncherForActivityResult
        busy = true
        coroutineScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val imported = readComponentStylePackage(context, uri, ComponentStyleKind.Switch)
                        .switchStyle ?: error("Switch style is missing")
                    if (!store.saveSwitchStyle(imported, apply = false)) {
                        store.discardSwitchImageIfUnreferenced(imported.imageUri)
                        error("Unable to save imported style")
                    }
                    imported
                }
            }.onSuccess { imported ->
                styles = store.readSwitchStyles()
                draft = styles.firstOrNull { it.id == imported.id } ?: imported
                baseline = draft
                selectedPage = SwitchCreatorPage.Design.ordinal
                showMessage(importSuccess)
            }.onFailure { error ->
                showMessage(error.switchEditorMessage(context))
            }
            busy = false
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(THEME_STORE_FILE_MIME_TYPE),
    ) { uri ->
        val snapshot = exportSnapshot
        exportSnapshot = null
        if (uri == null || snapshot == null || busy) return@rememberLauncherForActivityResult
        busy = true
        coroutineScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val result = exportSwitchComponentStylePackage(context, snapshot, uri)
                    require(result.success) { result.error?.message ?: "Unable to export switch style" }
                    require(result.warnings.isEmpty()) { "Switch style package contains unavailable resources" }
                }
            }.onSuccess {
                showMessage(exportSuccess)
            }.onFailure { error ->
                showMessage(error.switchEditorMessage(context))
            }
            busy = false
        }
    }

    fun requestExport(style: CustomSwitchStyle) {
        if (!validateDraft(style)) return
        exportSnapshot = style.normalized()
        exportLauncher.launch(canonicalCloudThemePackageFileName(style.name))
    }

    fun openCloudSubmission() {
        if (busy || !saveDraft(apply = false)) return
        val snapshot = draft.normalized()
        busy = true
        coroutineScope.launch {
            runCatching {
                prepareSwitchStyleCloudSubmission(
                    context = context,
                    style = snapshot,
                    description = cloudDescription,
                    categoryName = cloudCategory,
                )
            }.onSuccess {
                navigator.push(Route.CloudThemeCreatorSubmission)
            }.onFailure { error ->
                showMessage(error.switchEditorMessage(context))
            }
            busy = false
        }
    }

    val dirty = draft != baseline
    val onBack = dropUnlessResumed { navigator.pop() }
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.switch_style_creator_title))
                        if (dirty) {
                            Text(
                                text = stringResource(R.string.component_creator_unsaved),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.close))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val fresh = starterSwitchStyle(defaultName)
                            draft = fresh
                            baseline = fresh
                            selectedPage = SwitchCreatorPage.Design.ordinal
                        },
                        enabled = !busy,
                    ) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.component_creator_new))
                    }
                    IconButton(
                        onClick = { importLauncher.launch(arrayOf(THEME_STORE_FILE_MIME_TYPE, "application/zip")) },
                        enabled = !busy,
                    ) {
                        Icon(Icons.Rounded.FileOpen, stringResource(R.string.component_creator_import))
                    }
                    IconButton(onClick = { requestExport(draft) }, enabled = !busy) {
                        Icon(Icons.Rounded.Upload, stringResource(R.string.component_creator_export))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                shadowElevation = 5.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { saveDraft(apply = false) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.component_creator_save))
                    }
                    Button(
                        onClick = { saveDraft(apply = true) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                        }
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.component_creator_apply))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val currentPage = SwitchCreatorPage.entries.getOrElse(selectedPage) {
                SwitchCreatorPage.Design
            }
            SwitchCreatorTabs(currentPage.ordinal) { selectedPage = it }
            when (currentPage) {
                SwitchCreatorPage.Design -> SwitchDesignPage(
                    draft = draft,
                    editorKey = editorKey,
                    selectedLayer = currentLayer,
                    selectedColor = selectedColor,
                    currentGrid = currentGrid(),
                    previewChecked = previewChecked,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    onDraftChange = { draft = it },
                    onLayerSelected = { selectedLayer = it.ordinal },
                    onColorSelected = { selectedColor = it },
                    onPaletteChange = { draft = draft.copy(palette = it).normalized() },
                    onPreviewCheckedChange = { previewChecked = it },
                    onSelectImage = { imageLauncher.launch(arrayOf("image/png", "image/jpeg", "image/webp", "image/*")) },
                    onRemoveImage = {
                        draft = draft.copy(
                            source = CustomSwitchSource.Pixel,
                            imageUri = null,
                            imageSha256 = null,
                            imageMimeType = null,
                        )
                    },
                    onStrokeStart = ::pushUndo,
                    onGridChange = ::updateGrid,
                    onUndo = {
                        val current = currentGrid()
                        val previous = undoStack.lastOrNull()
                            ?.takeIf { it.hasSameDimensionsAs(current) }
                        if (previous != null) {
                            redoStack = (redoStack + current).takeLast(MAX_SWITCH_PIXEL_HISTORY)
                            undoStack = undoStack.dropLast(1)
                            updateGrid(previous)
                        } else {
                            undoStack = emptyList()
                            redoStack = emptyList()
                        }
                    },
                    onRedo = {
                        val current = currentGrid()
                        val next = redoStack.lastOrNull()
                            ?.takeIf { it.hasSameDimensionsAs(current) }
                        if (next != null) {
                            undoStack = (undoStack + current).takeLast(MAX_SWITCH_PIXEL_HISTORY)
                            redoStack = redoStack.dropLast(1)
                            updateGrid(next)
                        } else {
                            undoStack = emptyList()
                            redoStack = emptyList()
                        }
                    },
                    onFill = {
                        pushUndo(currentGrid())
                        updateGrid(currentGrid().filled(selectedColor))
                    },
                    onMirror = {
                        pushUndo(currentGrid())
                        updateGrid(currentGrid().mirroredHorizontally())
                    },
                    onClear = {
                        pushUndo(currentGrid())
                        updateGrid(currentGrid().cleared())
                    },
                    busy = busy,
                )
                SwitchCreatorPage.Motion -> SwitchMotionPage(
                    draft = draft,
                    checked = previewChecked,
                    onCheckedChange = { previewChecked = it },
                    onDraftChange = { draft = it },
                )
                SwitchCreatorPage.Library -> SwitchStyleLibraryPage(
                    styles = styles,
                    activeId = store.readActiveSwitchStyle()?.id,
                    onLoad = { style ->
                        draft = style
                        baseline = style
                        selectedPage = SwitchCreatorPage.Design.ordinal
                    },
                    onApply = { style ->
                        if (store.saveSwitchStyle(style, apply = true)) {
                            styles = store.readSwitchStyles()
                            showMessage(applySuccess)
                        } else {
                            showMessage(saveFailed)
                        }
                    },
                    onExport = ::requestExport,
                    onDelete = { deleteCandidate = it },
                    onImport = { importLauncher.launch(arrayOf(THEME_STORE_FILE_MIME_TYPE, "application/zip")) },
                    onCloudSubmission = ::openCloudSubmission,
                    busy = busy,
                )
            }
        }
    }

    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.component_creator_delete_title)) },
            text = { Text(stringResource(R.string.component_creator_delete_message, candidate.name)) },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text(stringResource(android.R.string.cancel)) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (store.deleteSwitchStyle(candidate.id)) {
                            styles = store.readSwitchStyles()
                            if (draft.id == candidate.id) {
                                val fresh = starterSwitchStyle(defaultName)
                                draft = fresh
                                baseline = fresh
                            }
                        } else {
                            showMessage(deleteFailed)
                        }
                        deleteCandidate = null
                    },
                ) { Text(stringResource(R.string.component_creator_delete)) }
            },
        )
    }
}

@Composable
private fun SwitchCreatorTabs(selected: Int, onSelected: (Int) -> Unit) {
    PrimaryTabRow(selectedTabIndex = selected) {
        SwitchCreatorPage.entries.forEach { page ->
            Tab(
                selected = selected == page.ordinal,
                onClick = { onSelected(page.ordinal) },
                text = { Text(stringResource(page.labelRes())) },
            )
        }
    }
}

@Composable
private fun SwitchDesignPage(
    draft: CustomSwitchStyle,
    editorKey: String,
    selectedLayer: SwitchPixelLayer,
    selectedColor: Long,
    currentGrid: PixelGrid,
    previewChecked: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onDraftChange: (CustomSwitchStyle) -> Unit,
    onLayerSelected: (SwitchPixelLayer) -> Unit,
    onColorSelected: (Long) -> Unit,
    onPaletteChange: (List<Long>) -> Unit,
    onPreviewCheckedChange: (Boolean) -> Unit,
    onSelectImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onStrokeStart: (PixelGrid) -> Unit,
    onGridChange: (PixelGrid) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFill: () -> Unit,
    onMirror: () -> Unit,
    onClear: () -> Unit,
    busy: Boolean,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PixelEditorSection(stringResource(R.string.component_creator_identity)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onDraftChange(draft.copy(name = it.take(48))) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.component_creator_name)) },
                )
                OutlinedTextField(
                    value = draft.author,
                    onValueChange = { onDraftChange(draft.copy(author = it.take(64))) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.component_creator_author)) },
                )
            }
        }
        item {
            PixelEditorSection(stringResource(R.string.switch_style_creator_preview)) {
                SwitchStylePreview(
                    style = draft,
                    checked = previewChecked,
                    onCheckedChange = onPreviewCheckedChange,
                )
            }
        }
        item {
            PixelEditorSection(stringResource(R.string.switch_style_creator_source)) {
                SwitchChoiceChipRow(
                    options = CustomSwitchSource.entries,
                    selected = draft.source,
                    label = { stringResource(it.labelRes()) },
                    onSelected = { source -> onDraftChange(draft.copy(source = source)) },
                )
                if (draft.source == CustomSwitchSource.Image) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onSelectImage,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.switch_style_creator_select_image))
                        }
                        if (draft.imageUri != null) {
                            OutlinedButton(onClick = onRemoveImage, enabled = !busy) {
                                Text(stringResource(R.string.switch_style_creator_remove_image))
                            }
                        }
                    }
                    SwitchChoiceChipRow(
                        options = SwitchImageScale.entries,
                        selected = draft.imageScale,
                        label = { stringResource(it.labelRes()) },
                        onSelected = { onDraftChange(draft.copy(imageScale = it)) },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.switch_style_creator_image_opacity),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${(draft.imageOpacity * 100).roundToInt()}%",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Slider(
                            value = draft.imageOpacity,
                            onValueChange = { onDraftChange(draft.copy(imageOpacity = it)) },
                            valueRange = 0.1f..1f,
                        )
                    }
                    draft.imageSha256?.let { hash ->
                        Text(
                            text = "SHA256  ${hash.take(12)}...${hash.takeLast(8)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
        item {
            PixelEditorSection(stringResource(R.string.component_creator_edit_area)) {
                val availableLayers = if (draft.source == CustomSwitchSource.Image) {
                    listOf(SwitchPixelLayer.ThumbOff, SwitchPixelLayer.ThumbOn)
                } else {
                    SwitchPixelLayer.entries
                }
                SwitchChoiceChipRow(
                    options = availableLayers,
                    selected = selectedLayer.takeIf { it in availableLayers } ?: availableLayers.first(),
                    label = { stringResource(it.labelRes()) },
                    onSelected = onLayerSelected,
                )
                Text(
                    text = stringResource(
                        R.string.component_creator_grid_size,
                        currentGrid.width,
                        currentGrid.height,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                PixelGridEditor(
                    grid = currentGrid,
                    selectedColor = selectedColor,
                    contentDescription = stringResource(R.string.component_creator_pixel_canvas),
                    onStrokeStart = onStrokeStart,
                    onGridChange = onGridChange,
                    gestureKey = editorKey,
                    modifier = Modifier.height(if (currentGrid.width == currentGrid.height) 260.dp else 190.dp),
                )
                PixelEditToolbar(
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onSelectEraser = { onColorSelected(TRANSPARENT_PIXEL) },
                    onFill = onFill,
                    onMirror = onMirror,
                    onClear = onClear,
                )
                PixelPaletteEditor(
                    palette = draft.palette,
                    selectedColor = selectedColor,
                    onColorSelected = onColorSelected,
                    onPaletteChange = onPaletteChange,
                )
            }
        }
    }
}

@Composable
private fun SwitchMotionPage(
    draft: CustomSwitchStyle,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDraftChange: (CustomSwitchStyle) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SwitchStylePreview(style = draft, checked = checked, onCheckedChange = onCheckedChange)
        }
        item {
            PixelEditorSection(stringResource(R.string.component_creator_motion_rules)) {
                PixelMotionEditor(
                    rule = draft.motion,
                    onRuleChange = { onDraftChange(draft.copy(motion = it)) },
                )
            }
        }
    }
}

@Composable
private fun SwitchStylePreview(
    style: CustomSwitchStyle,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val checkedProgress by animateFloatAsState(if (checked) 1f else 0f, label = "switchCreatorChecked")
    val motionProgress = rememberComponentMotionProgress(style.motion, true, "switchCreatorMotion")
    val image = rememberCustomSwitchImage(style)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (checked) R.string.switch_style_creator_state_on else R.string.switch_style_creator_state_off
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = style.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Canvas(Modifier.size(width = 118.dp, height = 58.dp)) {
                drawCustomSwitchStyle(
                    style = style,
                    checkedProgress = checkedProgress,
                    enabledAlpha = 1f,
                    motionProgress = motionProgress,
                    image = image,
                )
            }
        }
    }
}

@Composable
private fun SwitchStyleLibraryPage(
    styles: List<CustomSwitchStyle>,
    activeId: String?,
    onLoad: (CustomSwitchStyle) -> Unit,
    onApply: (CustomSwitchStyle) -> Unit,
    onExport: (CustomSwitchStyle) -> Unit,
    onDelete: (CustomSwitchStyle) -> Unit,
    onImport: () -> Unit,
    onCloudSubmission: () -> Unit,
    busy: Boolean,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onImport, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.component_creator_import))
                }
                Button(onClick = onCloudSubmission, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.component_creator_cloud_submit))
                }
            }
        }
        if (styles.isEmpty()) {
            item {
                PixelEditorSection(stringResource(R.string.component_creator_library_empty)) {
                    Text(
                        text = stringResource(R.string.component_creator_library_empty_summary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(styles, key = CustomSwitchStyle::id) { style ->
            Card(
                onClick = { onLoad(style) },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (style.id == activeId) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                ),
                border = BorderStroke(
                    1.dp,
                    if (style.id == activeId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(style.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = style.author.ifBlank { stringResource(R.string.component_creator_unknown_author) },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onApply(style) }, enabled = !busy) {
                        Icon(Icons.Rounded.Check, stringResource(R.string.component_creator_apply))
                    }
                    IconButton(onClick = { onExport(style) }, enabled = !busy) {
                        Icon(Icons.Rounded.Upload, stringResource(R.string.component_creator_export))
                    }
                    IconButton(onClick = { onDelete(style) }, enabled = !busy) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.component_creator_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SwitchChoiceChipRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

private fun starterSwitchStyle(name: String): CustomSwitchStyle {
    val off = 0xFF4A5260L
    val on = DEFAULT_PIXEL_PALETTE[3]
    val onHighlight = DEFAULT_PIXEL_PALETTE[5]
    val white = 0xFFFFFFFFL
    val trackOff = PixelGrid(
        SWITCH_TRACK_GRID_WIDTH,
        SWITCH_TRACK_GRID_HEIGHT,
        List(SWITCH_TRACK_GRID_WIDTH * SWITCH_TRACK_GRID_HEIGHT) { index ->
            val x = index % SWITCH_TRACK_GRID_WIDTH
            val y = index / SWITCH_TRACK_GRID_WIDTH
            if ((x + y) % 7 == 0) 0xFF626C7BL else off
        },
    )
    val trackOn = PixelGrid(
        SWITCH_TRACK_GRID_WIDTH,
        SWITCH_TRACK_GRID_HEIGHT,
        List(SWITCH_TRACK_GRID_WIDTH * SWITCH_TRACK_GRID_HEIGHT) { index ->
            val x = index % SWITCH_TRACK_GRID_WIDTH
            val y = index / SWITCH_TRACK_GRID_WIDTH
            if ((x + y) % 5 == 0) onHighlight else on
        },
    )
    fun thumb(checked: Boolean): PixelGrid = PixelGrid(
        SWITCH_THUMB_GRID_SIZE,
        SWITCH_THUMB_GRID_SIZE,
        List(SWITCH_THUMB_GRID_SIZE * SWITCH_THUMB_GRID_SIZE) { index ->
            val x = index % SWITCH_THUMB_GRID_SIZE
            val y = index / SWITCH_THUMB_GRID_SIZE
            val center = (SWITCH_THUMB_GRID_SIZE - 1) / 2f
            val distance = sqrt((x - center) * (x - center) + (y - center) * (y - center))
            when {
                distance > center -> TRANSPARENT_PIXEL
                checked && x in 3..4 && y in 5..7 -> on
                checked && x in 4..8 && y in 7..8 -> on
                !checked && x in 3..8 && y in 5..6 -> off
                else -> white
            }
        },
    )
    return CustomSwitchStyle(
        name = name,
        trackOff = trackOff,
        trackOn = trackOn,
        thumbOff = thumb(checked = false),
        thumbOn = thumb(checked = true),
    )
}

private fun SwitchCreatorPage.labelRes(): Int = when (this) {
    SwitchCreatorPage.Design -> R.string.component_creator_tab_design
    SwitchCreatorPage.Motion -> R.string.component_creator_tab_motion
    SwitchCreatorPage.Library -> R.string.component_creator_tab_library
}

private fun SwitchPixelLayer.labelRes(): Int = when (this) {
    SwitchPixelLayer.TrackOff -> R.string.switch_style_creator_track_off
    SwitchPixelLayer.TrackOn -> R.string.switch_style_creator_track_on
    SwitchPixelLayer.ThumbOff -> R.string.switch_style_creator_thumb_off
    SwitchPixelLayer.ThumbOn -> R.string.switch_style_creator_thumb_on
}

private fun CustomSwitchSource.labelRes(): Int = when (this) {
    CustomSwitchSource.Pixel -> R.string.switch_style_creator_source_pixel
    CustomSwitchSource.Image -> R.string.switch_style_creator_source_image
}

private fun SwitchImageScale.labelRes(): Int = when (this) {
    SwitchImageScale.Crop -> R.string.switch_style_creator_image_crop
    SwitchImageScale.Fit -> R.string.switch_style_creator_image_fit
}

private fun Throwable.switchEditorMessage(context: android.content.Context): String {
    return message?.lineSequence()?.firstOrNull()?.take(180)
        ?: context.getString(R.string.component_creator_unknown_error)
}

private const val MAX_SWITCH_PIXEL_HISTORY = 40
