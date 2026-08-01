package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.custom.ComponentStyleKind
import me.weishu.kernelsu.ui.component.custom.ComponentStyleStore
import me.weishu.kernelsu.ui.component.custom.CustomSwitchSource
import me.weishu.kernelsu.ui.component.custom.CustomSwitchStyle
import me.weishu.kernelsu.ui.component.custom.DEFAULT_PIXEL_PALETTE
import me.weishu.kernelsu.ui.component.custom.MAX_SWITCH_TRANSITION_DURATION_MS
import me.weishu.kernelsu.ui.component.custom.MIN_SWITCH_TRANSITION_DURATION_MS
import me.weishu.kernelsu.ui.component.custom.PixelCanvasTool
import me.weishu.kernelsu.ui.component.custom.PixelComponentPreset
import me.weishu.kernelsu.ui.component.custom.PixelEditorSection
import me.weishu.kernelsu.ui.component.custom.PixelGrid
import me.weishu.kernelsu.ui.component.custom.PixelGridEditor
import me.weishu.kernelsu.ui.component.custom.PixelMotionEditor
import me.weishu.kernelsu.ui.component.custom.PixelPaletteEditor
import me.weishu.kernelsu.ui.component.custom.PixelSelection
import me.weishu.kernelsu.ui.component.custom.PixelSymmetry
import me.weishu.kernelsu.ui.component.custom.SWITCH_THUMB_GRID_SIZE
import me.weishu.kernelsu.ui.component.custom.SWITCH_TRACK_GRID_HEIGHT
import me.weishu.kernelsu.ui.component.custom.SWITCH_TRACK_GRID_WIDTH
import me.weishu.kernelsu.ui.component.custom.SwitchImageScale
import me.weishu.kernelsu.ui.component.custom.SwitchImageBlend
import me.weishu.kernelsu.ui.component.custom.SwitchImageAppearance
import me.weishu.kernelsu.ui.component.custom.SwitchTransitionEasing
import me.weishu.kernelsu.ui.component.custom.TRANSPARENT_PIXEL
import me.weishu.kernelsu.ui.component.custom.cropped
import me.weishu.kernelsu.ui.component.custom.decodeImageToPixelGrid
import me.weishu.kernelsu.ui.component.custom.drawCustomSwitchStyle
import me.weishu.kernelsu.ui.component.custom.drawPixelLayer
import me.weishu.kernelsu.ui.component.custom.filled
import me.weishu.kernelsu.ui.component.custom.formatArgbHex
import me.weishu.kernelsu.ui.component.custom.hasSameDimensionsAs
import me.weishu.kernelsu.ui.component.custom.mirroredHorizontally
import me.weishu.kernelsu.ui.component.custom.mirroredVertically
import me.weishu.kernelsu.ui.component.custom.movedSelection
import me.weishu.kernelsu.ui.component.custom.parseArgbHex
import me.weishu.kernelsu.ui.component.custom.pasted
import me.weishu.kernelsu.ui.component.custom.rememberComponentMotionProgress
import me.weishu.kernelsu.ui.component.custom.composeEasing
import me.weishu.kernelsu.ui.component.custom.rememberCustomSwitchImages
import me.weishu.kernelsu.ui.component.custom.imageAppearanceFor
import me.weishu.kernelsu.ui.component.custom.rotatedSelection
import me.weishu.kernelsu.ui.component.custom.withPreset
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

private enum class SwitchImageSlot {
    Off,
    On,
}

private enum class SwitchAppearanceSection {
    Track,
    Border,
    Thumb,
    Effects,
    Transition,
}

private sealed interface SwitchEditorPendingAction {
    data object Back : SwitchEditorPendingAction
    data object New : SwitchEditorPendingAction
    data object Import : SwitchEditorPendingAction
    data class Load(val style: CustomSwitchStyle) : SwitchEditorPendingAction
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
    val imageConverted = stringResource(R.string.switch_style_creator_image_converted)
    val draftRecovered = stringResource(R.string.switch_style_creator_draft_recovered)
    val nameRequired = stringResource(R.string.component_creator_name_required)
    val imageRequired = stringResource(R.string.switch_style_creator_image_required)
    val saveFailed = stringResource(R.string.component_creator_save_failed)
    val deleteFailed = stringResource(R.string.component_creator_delete_failed)
    val cloudDescription = stringResource(R.string.switch_style_creator_cloud_description)
    val cloudCategory = stringResource(R.string.switch_style_creator_cloud_category)

    var styles by remember { mutableStateOf(store.readSwitchStyles()) }
    val libraryInitialStyle = remember {
        store.readActiveSwitchStyle() ?: styles.firstOrNull() ?: starterSwitchStyle(defaultName)
    }
    val recoveredDraft = remember { store.readSwitchEditorDraft() }
    val initialStyle = remember { recoveredDraft ?: libraryInitialStyle }
    val initialBaseline = remember {
        styles.firstOrNull { it.id == initialStyle.id } ?: libraryInitialStyle
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
    var baseline by remember { mutableStateOf(initialBaseline) }
    var selectedPage by rememberSaveable { mutableIntStateOf(SwitchCreatorPage.Design.ordinal) }
    var selectedLayer by rememberSaveable { mutableIntStateOf(SwitchPixelLayer.TrackOff.ordinal) }
    var selectedColor by rememberSaveable { mutableLongStateOf(DEFAULT_PIXEL_PALETTE[3]) }
    var previewChecked by rememberSaveable { mutableStateOf(true) }
    var selectedStateOn by rememberSaveable { mutableStateOf(false) }
    var undoStack by remember { mutableStateOf<List<CustomSwitchStyle>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<CustomSwitchStyle>>(emptyList()) }
    var interactionStartDraft by remember { mutableStateOf<CustomSwitchStyle?>(null) }
    var selectedTool by rememberSaveable { mutableStateOf(PixelCanvasTool.Pencil) }
    var symmetry by rememberSaveable { mutableStateOf(PixelSymmetry.None) }
    var showGrid by rememberSaveable { mutableStateOf(true) }
    var selection by remember { mutableStateOf<PixelSelection?>(null) }
    var pixelClipboard by remember { mutableStateOf<PixelGrid?>(null) }
    var layerClipboard by remember { mutableStateOf<PixelGrid?>(null) }
    var recentColors by remember { mutableStateOf<List<Long>>(emptyList()) }
    var fullscreenEditor by remember { mutableStateOf(false) }
    var showAssetLibrary by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<SwitchEditorPendingAction?>(null) }
    var importCollision by remember { mutableStateOf<CustomSwitchStyle?>(null) }
    var pendingImageSlot by remember { mutableStateOf(SwitchImageSlot.Off) }
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

    fun withCurrentGrid(source: CustomSwitchStyle, grid: PixelGrid): CustomSwitchStyle {
        if (!grid.hasSameDimensionsAs(currentGrid())) return source
        return when (currentLayer) {
            SwitchPixelLayer.TrackOff -> source.copy(trackOff = grid)
            SwitchPixelLayer.TrackOn -> source.copy(trackOn = grid)
            SwitchPixelLayer.ThumbOff -> source.copy(thumbOff = grid)
            SwitchPixelLayer.ThumbOn -> source.copy(thumbOn = grid)
        }
    }

    fun pushUndo(snapshot: CustomSwitchStyle) {
        if (undoStack.lastOrNull() != snapshot) {
            undoStack = (undoStack + snapshot).takeLast(MAX_SWITCH_PIXEL_HISTORY)
        }
        redoStack = emptyList()
    }

    fun applyDraft(next: CustomSwitchStyle) {
        if (next == draft) return
        pushUndo(draft)
        draft = next
        interactionStartDraft = null
    }

    fun updateGrid(grid: PixelGrid) {
        draft = withCurrentGrid(draft, grid)
    }

    fun beginInteraction() {
        if (interactionStartDraft == null) interactionStartDraft = draft
    }

    fun finishInteraction() {
        interactionStartDraft?.let { start -> if (start != draft) pushUndo(start) }
        interactionStartDraft = null
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        undoStack = undoStack.dropLast(1)
        redoStack = (redoStack + draft).takeLast(MAX_SWITCH_PIXEL_HISTORY)
        draft = previous
        interactionStartDraft = null
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        redoStack = redoStack.dropLast(1)
        undoStack = (undoStack + draft).takeLast(MAX_SWITCH_PIXEL_HISTORY)
        draft = next
        interactionStartDraft = null
    }

    fun showMessage(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun selectColor(color: Long) {
        selectedColor = color
        if (color != TRANSPARENT_PIXEL) {
            recentColors = (listOf(color) + recentColors.filterNot { it == color }).take(MAX_RECENT_SWITCH_COLORS)
        }
    }

    fun refreshAfterSave(styleId: String) {
        styles = store.readSwitchStyles()
        val stored = styles.firstOrNull { it.id == styleId }
        if (stored != null) {
            draft = stored
            baseline = stored
            undoStack = emptyList()
            redoStack = emptyList()
            interactionStartDraft = null
            store.clearSwitchEditorDraft()
        }
    }

    fun loadIntoEditor(style: CustomSwitchStyle) {
        draft = style
        baseline = style
        undoStack = emptyList()
        redoStack = emptyList()
        interactionStartDraft = null
        selection = null
        store.clearSwitchEditorDraft()
        selectedPage = SwitchCreatorPage.Design.ordinal
    }

    fun validateDraft(style: CustomSwitchStyle): Boolean {
        if (style.name.isBlank()) {
            showMessage(nameRequired)
            return false
        }
        if (style.source == CustomSwitchSource.Image) {
            val offValid = style.imageUri?.let(store::resolveImageFile) != null
            val onValid = style.imageOnUri?.let(store::resolveImageFile) != null
            val configuredImageMissing =
                (style.imageUri != null && !offValid) || (style.imageOnUri != null && !onValid)
            if ((!offValid && !onValid) || configuredImageMissing) {
                showMessage(imageRequired)
                return false
            }
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
        selection = null
    }

    LaunchedEffect(draft, baseline) {
        if (draft != baseline) {
            delay(SWITCH_DRAFT_AUTOSAVE_DELAY_MS)
            withContext(Dispatchers.IO) { store.saveSwitchEditorDraft(draft) }
        } else {
            withContext(Dispatchers.IO) { store.clearSwitchEditorDraft() }
        }
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
                applyDraft(
                    when (pendingImageSlot) {
                        SwitchImageSlot.Off -> draft.copy(
                            source = CustomSwitchSource.Image,
                            imageUri = stored.uriString,
                            imageSha256 = stored.sha256,
                            imageMimeType = stored.mimeType,
                        )
                        SwitchImageSlot.On -> draft.copy(
                            source = CustomSwitchSource.Image,
                            imageOnUri = stored.uriString,
                            imageOnSha256 = stored.sha256,
                            imageOnMimeType = stored.mimeType,
                        )
                    }.normalized()
                )
                showMessage(imageSuccess)
            }.onFailure { error ->
                showMessage(error.switchEditorMessage(context))
            }
            busy = false
        }
    }

    val pixelImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (busy) return@rememberLauncherForActivityResult
        val sourceGrid = currentGrid()
        val sourceDraft = draft
        busy = true
        coroutineScope.launch {
            runCatching {
                decodeImageToPixelGrid(
                    context = context,
                    source = uri,
                    width = sourceGrid.width,
                    height = sourceGrid.height,
                    palette = sourceDraft.palette,
                )
            }.onSuccess { importedGrid ->
                applyDraft(withCurrentGrid(draft, importedGrid))
                selectedTool = PixelCanvasTool.Pencil
                showMessage(imageConverted)
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
                    readComponentStylePackage(context, uri, ComponentStyleKind.Switch)
                        .switchStyle ?: error("Switch style is missing")
                }
            }.onSuccess { imported ->
                if (styles.any { it.id == imported.id }) {
                    importCollision = imported
                } else if (store.saveSwitchStyle(imported, apply = false)) {
                    styles = store.readSwitchStyles()
                    loadIntoEditor(styles.firstOrNull { it.id == imported.id } ?: imported)
                    showMessage(importSuccess)
                } else {
                    imported.imageUris().forEach(store::discardSwitchImageIfUnreferenced)
                    showMessage(saveFailed)
                }
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
    val popBack = dropUnlessResumed { navigator.pop() }

    fun performPendingAction(action: SwitchEditorPendingAction) {
        when (action) {
            SwitchEditorPendingAction.Back -> {
                store.clearSwitchEditorDraft()
                popBack()
            }
            SwitchEditorPendingAction.New -> {
                val fresh = starterSwitchStyle(defaultName)
                loadIntoEditor(fresh)
            }
            SwitchEditorPendingAction.Import -> {
                importLauncher.launch(arrayOf(THEME_STORE_FILE_MIME_TYPE, "application/zip"))
            }
            is SwitchEditorPendingAction.Load -> {
                loadIntoEditor(action.style)
            }
        }
    }

    fun requestAction(action: SwitchEditorPendingAction) {
        if (dirty) pendingAction = action else performPendingAction(action)
    }

    BackHandler(enabled = dirty && !fullscreenEditor) {
        requestAction(SwitchEditorPendingAction.Back)
    }

    LaunchedEffect(Unit) {
        if (recoveredDraft != null && recoveredDraft != initialBaseline) {
            showMessage(draftRecovered)
        }
    }

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
                    IconButton(onClick = { requestAction(SwitchEditorPendingAction.Back) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.close))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            requestAction(SwitchEditorPendingAction.New)
                        },
                        enabled = !busy,
                    ) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.component_creator_new))
                    }
                    IconButton(
                        onClick = { requestAction(SwitchEditorPendingAction.Import) },
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
                    selectedStateOn = selectedStateOn,
                    onSelectedStateChange = { selectedStateOn = it },
                    onImageAppearanceChange = { appearance ->
                        applyDraft(
                            if (selectedStateOn) {
                                draft.copy(imageOnAppearance = appearance)
                            } else {
                                draft.copy(imageOffAppearance = appearance)
                            },
                        )
                    },
                    onImageAppearanceUpdate = { appearance ->
                        draft = if (selectedStateOn) {
                            draft.copy(imageOnAppearance = appearance)
                        } else {
                            draft.copy(imageOffAppearance = appearance)
                        }
                    },
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    selectedTool = selectedTool,
                    symmetry = symmetry,
                    showGrid = showGrid,
                    selection = selection,
                    recentColors = recentColors,
                    onDraftChange = ::applyDraft,
                    onDraftUpdate = { draft = it },
                    onLayerSelected = {
                        selectedLayer = it.ordinal
                        selection = null
                    },
                    onColorSelected = ::selectColor,
                    onPaletteChange = { applyDraft(draft.copy(palette = it).normalized()) },
                    onPreviewCheckedChange = { previewChecked = it },
                     onSelectImage = { slot ->
                         selectedStateOn = slot == SwitchImageSlot.On
                         pendingImageSlot = slot
                         imageLauncher.launch(arrayOf("image/png", "image/jpeg", "image/webp", "image/*"))
                     },
                     onRemoveImage = { slot ->
                         selectedStateOn = slot == SwitchImageSlot.On
                         applyDraft(
                            when (slot) {
                                SwitchImageSlot.Off -> draft.copy(
                                    imageUri = null,
                                    imageSha256 = null,
                                    imageMimeType = null,
                                )
                                SwitchImageSlot.On -> draft.copy(
                                    imageOnUri = null,
                                    imageOnSha256 = null,
                                    imageOnMimeType = null,
                                )
                            }
                        )
                    },
                    onConvertImage = { pixelImageLauncher.launch(arrayOf("image/*")) },
                    onToolSelected = { selectedTool = it },
                    onSymmetryChange = { symmetry = it },
                    onShowGridChange = { showGrid = it },
                    onSelectionChange = { selection = it },
                    onStrokeStart = { beginInteraction() },
                    onStrokeEnd = ::finishInteraction,
                    onGridChange = ::updateGrid,
                    onUndo = ::undo,
                    onRedo = ::redo,
                    onFill = { applyDraft(withCurrentGrid(draft, currentGrid().filled(selectedColor))) },
                    onMirrorHorizontal = {
                        applyDraft(withCurrentGrid(draft, currentGrid().mirroredHorizontally()))
                    },
                    onMirrorVertical = {
                        applyDraft(withCurrentGrid(draft, currentGrid().mirroredVertically()))
                    },
                    onCopySelection = {
                        pixelClipboard = selection?.let(currentGrid()::cropped) ?: currentGrid()
                    },
                    onPasteSelection = {
                        pixelClipboard?.let { copied ->
                            val target = selection
                            val x = target?.left ?: 0
                            val y = target?.top ?: 0
                            applyDraft(withCurrentGrid(draft, currentGrid().pasted(copied, x, y)))
                            selection = PixelSelection(
                                x,
                                y,
                                (x + copied.width).coerceAtMost(currentGrid().width),
                                (y + copied.height).coerceAtMost(currentGrid().height),
                            )
                        }
                    },
                    canPasteSelection = pixelClipboard != null,
                    onMoveSelection = { dx, dy ->
                        selection?.let { area ->
                            val (next, moved) = currentGrid().movedSelection(area, dx, dy)
                            applyDraft(withCurrentGrid(draft, next))
                            selection = moved
                        }
                    },
                    onRotateSelection = {
                        selection?.let { area ->
                            val (next, rotated) = currentGrid().rotatedSelection(area)
                            applyDraft(withCurrentGrid(draft, next))
                            selection = rotated
                        }
                    },
                    onCopyLayer = { layerClipboard = currentGrid() },
                    onPasteLayer = {
                        layerClipboard?.takeIf { it.hasSameDimensionsAs(currentGrid()) }?.let { copied ->
                            applyDraft(withCurrentGrid(draft, copied))
                        }
                    },
                    canPasteLayer = layerClipboard?.hasSameDimensionsAs(currentGrid()) == true,
                    onSwapStateLayers = {
                        applyDraft(
                            when (currentLayer) {
                                SwitchPixelLayer.TrackOff, SwitchPixelLayer.TrackOn -> draft.copy(
                                    trackOff = draft.trackOn,
                                    trackOn = draft.trackOff,
                                )
                                SwitchPixelLayer.ThumbOff, SwitchPixelLayer.ThumbOn -> draft.copy(
                                    thumbOff = draft.thumbOn,
                                    thumbOn = draft.thumbOff,
                                )
                            }
                        )
                    },
                    onOpenAssets = { showAssetLibrary = true },
                    onOpenFullscreen = { fullscreenEditor = true },
                    onClear = { applyDraft(withCurrentGrid(draft, currentGrid().cleared())) },
                    onInteractionStart = ::beginInteraction,
                    onInteractionEnd = ::finishInteraction,
                    busy = busy,
                )
                SwitchCreatorPage.Motion -> SwitchMotionPage(
                    draft = draft,
                    checked = previewChecked,
                    onCheckedChange = { previewChecked = it },
                    onDraftUpdate = { draft = it },
                    onInteractionStart = ::beginInteraction,
                    onInteractionEnd = ::finishInteraction,
                )
                SwitchCreatorPage.Library -> SwitchStyleLibraryPage(
                    styles = styles,
                    activeId = store.readActiveSwitchStyle()?.id,
                    onLoad = { style -> requestAction(SwitchEditorPendingAction.Load(style)) },
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
                    onImport = { requestAction(SwitchEditorPendingAction.Import) },
                    onCloudSubmission = ::openCloudSubmission,
                    busy = busy,
                )
            }
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(stringResource(R.string.switch_style_creator_unsaved_title)) },
            text = { Text(stringResource(R.string.switch_style_creator_unsaved_message)) },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(stringResource(R.string.switch_style_creator_keep_editing))
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            pendingAction = null
                            store.clearSwitchEditorDraft()
                            performPendingAction(action)
                        },
                    ) {
                        Text(stringResource(R.string.switch_style_creator_discard_changes))
                    }
                    Button(
                        onClick = {
                            if (saveDraft(apply = false)) {
                                pendingAction = null
                                performPendingAction(action)
                            }
                        },
                    ) {
                        Text(stringResource(R.string.component_creator_save))
                    }
                }
            },
        )
    }

    importCollision?.let { imported ->
        AlertDialog(
            onDismissRequest = {
                imported.imageUris().forEach(store::discardSwitchImageIfUnreferenced)
                importCollision = null
            },
            title = { Text(stringResource(R.string.switch_style_creator_import_conflict_title)) },
            text = { Text(stringResource(R.string.switch_style_creator_import_conflict_message, imported.name)) },
            dismissButton = {
                TextButton(
                    onClick = {
                        val duplicate = imported.copy(id = "switch-${UUID.randomUUID()}")
                        if (store.saveSwitchStyle(duplicate, apply = false)) {
                            styles = store.readSwitchStyles()
                            loadIntoEditor(styles.firstOrNull { it.id == duplicate.id } ?: duplicate)
                            showMessage(importSuccess)
                        } else {
                            duplicate.imageUris().forEach(store::discardSwitchImageIfUnreferenced)
                            showMessage(saveFailed)
                        }
                        importCollision = null
                    },
                ) {
                    Text(stringResource(R.string.switch_style_creator_import_as_copy))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (store.saveSwitchStyle(imported, apply = false)) {
                            styles = store.readSwitchStyles()
                            loadIntoEditor(styles.firstOrNull { it.id == imported.id } ?: imported)
                            showMessage(importSuccess)
                        } else {
                            imported.imageUris().forEach(store::discardSwitchImageIfUnreferenced)
                            showMessage(saveFailed)
                        }
                        importCollision = null
                    },
                ) {
                    Text(stringResource(R.string.switch_style_creator_replace_existing))
                }
            },
        )
    }

    if (showAssetLibrary) {
        val primary = selectedColor.takeIf { it != TRANSPARENT_PIXEL }
            ?: draft.palette.firstOrNull { it != TRANSPARENT_PIXEL }
            ?: DEFAULT_PIXEL_PALETTE[3]
        val secondary = draft.palette.firstOrNull { it != TRANSPARENT_PIXEL && it != primary } ?: primary
        SwitchPixelAssetLibrary(
            currentGrid = currentGrid(),
            primary = primary,
            secondary = secondary,
            onDismiss = { showAssetLibrary = false },
            onApply = { preset ->
                applyDraft(withCurrentGrid(draft, currentGrid().withPreset(preset, primary, secondary)))
                showAssetLibrary = false
            },
        )
    }

    if (fullscreenEditor) {
        SwitchPixelFullscreenEditor(
            draft = draft,
            editorKey = editorKey,
            selectedLayer = currentLayer,
            selectedColor = selectedColor,
            currentGrid = currentGrid(),
            previewChecked = previewChecked,
            selectedTool = selectedTool,
            symmetry = symmetry,
            showGrid = showGrid,
            selection = selection,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            canPasteSelection = pixelClipboard != null,
            onDismiss = { fullscreenEditor = false },
            onLayerSelected = {
                selectedLayer = it.ordinal
                selection = null
            },
            onColorSelected = ::selectColor,
            onPaletteChange = { applyDraft(draft.copy(palette = it).normalized()) },
            onPreviewCheckedChange = { previewChecked = it },
            onToolSelected = { selectedTool = it },
            onSymmetryChange = { symmetry = it },
            onShowGridChange = { showGrid = it },
            onSelectionChange = { selection = it },
            onStrokeStart = ::beginInteraction,
            onStrokeEnd = ::finishInteraction,
            onGridChange = ::updateGrid,
            onUndo = ::undo,
            onRedo = ::redo,
            onFill = { applyDraft(withCurrentGrid(draft, currentGrid().filled(selectedColor))) },
            onMirrorHorizontal = { applyDraft(withCurrentGrid(draft, currentGrid().mirroredHorizontally())) },
            onMirrorVertical = { applyDraft(withCurrentGrid(draft, currentGrid().mirroredVertically())) },
            onCopySelection = { pixelClipboard = selection?.let(currentGrid()::cropped) ?: currentGrid() },
            onPasteSelection = {
                pixelClipboard?.let { copied ->
                    val x = selection?.left ?: 0
                    val y = selection?.top ?: 0
                    applyDraft(withCurrentGrid(draft, currentGrid().pasted(copied, x, y)))
                }
            },
            onMoveSelection = { dx, dy ->
                selection?.let { area ->
                    val (next, moved) = currentGrid().movedSelection(area, dx, dy)
                    applyDraft(withCurrentGrid(draft, next))
                    selection = moved
                }
            },
            onRotateSelection = {
                selection?.let { area ->
                    val (next, rotated) = currentGrid().rotatedSelection(area)
                    applyDraft(withCurrentGrid(draft, next))
                    selection = rotated
                }
            },
            onConvertImage = {
                fullscreenEditor = false
                pixelImageLauncher.launch(arrayOf("image/*"))
            },
            onOpenAssets = {
                fullscreenEditor = false
                showAssetLibrary = true
            },
            onClear = { applyDraft(withCurrentGrid(draft, currentGrid().cleared())) },
        )
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
    selectedStateOn: Boolean,
    onSelectedStateChange: (Boolean) -> Unit,
    onImageAppearanceChange: (SwitchImageAppearance) -> Unit,
    onImageAppearanceUpdate: (SwitchImageAppearance) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    selectedTool: PixelCanvasTool,
    symmetry: PixelSymmetry,
    showGrid: Boolean,
    selection: PixelSelection?,
    recentColors: List<Long>,
    onDraftChange: (CustomSwitchStyle) -> Unit,
    onDraftUpdate: (CustomSwitchStyle) -> Unit,
    onLayerSelected: (SwitchPixelLayer) -> Unit,
    onColorSelected: (Long) -> Unit,
    onPaletteChange: (List<Long>) -> Unit,
    onPreviewCheckedChange: (Boolean) -> Unit,
    onSelectImage: (SwitchImageSlot) -> Unit,
    onRemoveImage: (SwitchImageSlot) -> Unit,
    onConvertImage: () -> Unit,
    onToolSelected: (PixelCanvasTool) -> Unit,
    onSymmetryChange: (PixelSymmetry) -> Unit,
    onShowGridChange: (Boolean) -> Unit,
    onSelectionChange: (PixelSelection?) -> Unit,
    onStrokeStart: () -> Unit,
    onStrokeEnd: () -> Unit,
    onGridChange: (PixelGrid) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFill: () -> Unit,
    onMirrorHorizontal: () -> Unit,
    onMirrorVertical: () -> Unit,
    onCopySelection: () -> Unit,
    onPasteSelection: () -> Unit,
    canPasteSelection: Boolean,
    onMoveSelection: (Int, Int) -> Unit,
    onRotateSelection: () -> Unit,
    onCopyLayer: () -> Unit,
    onPasteLayer: () -> Unit,
    canPasteLayer: Boolean,
    onSwapStateLayers: () -> Unit,
    onOpenAssets: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onClear: () -> Unit,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
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
                    val imageAppearance = draft.imageAppearanceFor(selectedStateOn)
                    SwitchImageSlotRow(
                        title = stringResource(R.string.switch_style_creator_image_off),
                        imageUri = draft.imageUri,
                        hash = draft.imageSha256,
                        busy = busy,
                        onSelect = { onSelectImage(SwitchImageSlot.Off) },
                        onRemove = { onRemoveImage(SwitchImageSlot.Off) },
                    )
                    SwitchImageSlotRow(
                        title = stringResource(R.string.switch_style_creator_image_on),
                        imageUri = draft.imageOnUri,
                        hash = draft.imageOnSha256,
                        busy = busy,
                        onSelect = { onSelectImage(SwitchImageSlot.On) },
                        onRemove = { onRemoveImage(SwitchImageSlot.On) },
                    )
                    SwitchChoiceChipRow(
                        options = listOf(false, true),
                        selected = selectedStateOn,
                        label = { enabled ->
                            stringResource(
                                if (enabled) {
                                    R.string.switch_style_creator_state_on
                                } else {
                                    R.string.switch_style_creator_state_off
                                },
                            )
                        },
                        onSelected = onSelectedStateChange,
                    )
                    SwitchChoiceChipRow(
                        options = SwitchImageScale.entries,
                        selected = imageAppearance.scale,
                        label = { stringResource(it.labelRes()) },
                        onSelected = { onImageAppearanceChange(imageAppearance.copy(scale = it)) },
                    )
                    SwitchValueSlider(
                        title = stringResource(R.string.switch_style_creator_image_opacity),
                        value = imageAppearance.opacity,
                        valueRange = 0.1f..1f,
                        valueText = "${(imageAppearance.opacity * 100).roundToInt()}%",
                        onValueChange = { onImageAppearanceUpdate(imageAppearance.copy(opacity = it)) },
                        onInteractionStart = onInteractionStart,
                        onInteractionEnd = onInteractionEnd,
                    )
                    SwitchValueSlider(
                        title = stringResource(R.string.switch_style_creator_image_zoom),
                        value = imageAppearance.zoom,
                        valueRange = 0.5f..3f,
                        valueText = String.format("%.2fx", imageAppearance.zoom),
                        onValueChange = { onImageAppearanceUpdate(imageAppearance.copy(zoom = it)) },
                        onInteractionStart = onInteractionStart,
                        onInteractionEnd = onInteractionEnd,
                    )
                    SwitchValueSlider(
                        title = stringResource(R.string.switch_style_creator_image_offset_x),
                        value = imageAppearance.offsetX,
                        valueRange = -1f..1f,
                        valueText = "${(imageAppearance.offsetX * 100).roundToInt()}%",
                        onValueChange = { onImageAppearanceUpdate(imageAppearance.copy(offsetX = it)) },
                        onInteractionStart = onInteractionStart,
                        onInteractionEnd = onInteractionEnd,
                    )
                    SwitchValueSlider(
                        title = stringResource(R.string.switch_style_creator_image_offset_y),
                        value = imageAppearance.offsetY,
                        valueRange = -1f..1f,
                        valueText = "${(imageAppearance.offsetY * 100).roundToInt()}%",
                        onValueChange = { onImageAppearanceUpdate(imageAppearance.copy(offsetY = it)) },
                        onInteractionStart = onInteractionStart,
                        onInteractionEnd = onInteractionEnd,
                    )
                    SwitchValueSlider(
                        title = stringResource(R.string.switch_style_creator_image_rotation),
                        value = imageAppearance.rotationDegrees,
                        valueRange = -180f..180f,
                        valueText = "${imageAppearance.rotationDegrees.roundToInt()} deg",
                        onValueChange = { onImageAppearanceUpdate(imageAppearance.copy(rotationDegrees = it)) },
                        onInteractionStart = onInteractionStart,
                        onInteractionEnd = onInteractionEnd,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = imageAppearance.flipHorizontal,
                            onClick = {
                                onImageAppearanceChange(
                                    imageAppearance.copy(flipHorizontal = !imageAppearance.flipHorizontal),
                                )
                            },
                            label = { Text(stringResource(R.string.switch_style_creator_flip_horizontal)) },
                        )
                        FilterChip(
                            selected = imageAppearance.flipVertical,
                            onClick = {
                                onImageAppearanceChange(
                                    imageAppearance.copy(flipVertical = !imageAppearance.flipVertical),
                                )
                            },
                            label = { Text(stringResource(R.string.switch_style_creator_flip_vertical)) },
                        )
                    }
                    SwitchChoiceChipRow(
                        options = SwitchImageBlend.entries,
                        selected = imageAppearance.blend,
                        label = { stringResource(it.labelRes()) },
                        onSelected = { onImageAppearanceChange(imageAppearance.copy(blend = it)) },
                    )
                    SwitchValueSlider(
                        title = stringResource(R.string.switch_style_creator_image_saturation),
                        value = imageAppearance.saturation,
                        valueRange = 0f..2f,
                        valueText = "${(imageAppearance.saturation * 100).roundToInt()}%",
                        onValueChange = { onImageAppearanceUpdate(imageAppearance.copy(saturation = it)) },
                        onInteractionStart = onInteractionStart,
                        onInteractionEnd = onInteractionEnd,
                    )
                    SwitchValueSlider(
                        title = stringResource(R.string.switch_style_creator_image_brightness),
                        value = imageAppearance.brightness,
                        valueRange = -1f..1f,
                        valueText = "${(imageAppearance.brightness * 100).roundToInt()}%",
                        onValueChange = { onImageAppearanceUpdate(imageAppearance.copy(brightness = it)) },
                        onInteractionStart = onInteractionStart,
                        onInteractionEnd = onInteractionEnd,
                    )
                    SwitchColorField(
                        title = stringResource(R.string.switch_style_creator_image_tint),
                        color = imageAppearance.tint,
                        allowNone = true,
                        onColorChange = { onImageAppearanceChange(imageAppearance.copy(tint = it)) },
                    )
                }
            }
        }
        item {
            SwitchAppearanceEditor(
                draft = draft,
                selectedStateOn = selectedStateOn,
                onSelectedStateChange = onSelectedStateChange,
                onDraftChange = onDraftChange,
                onDraftUpdate = onDraftUpdate,
                onInteractionStart = onInteractionStart,
                onInteractionEnd = onInteractionEnd,
            )
        }
        item {
            PixelEditorSection(stringResource(R.string.component_creator_edit_area)) {
                val availableLayers = if (draft.source == CustomSwitchSource.Image) {
                    listOf(SwitchPixelLayer.ThumbOff, SwitchPixelLayer.ThumbOn)
                } else {
                    SwitchPixelLayer.entries
                }
                SwitchLayerPanel(
                    draft = draft,
                    layers = availableLayers,
                    selected = selectedLayer.takeIf { it in availableLayers } ?: availableLayers.first(),
                    onSelected = onLayerSelected,
                    onCopyLayer = onCopyLayer,
                    onPasteLayer = onPasteLayer,
                    canPasteLayer = canPasteLayer,
                    onSwapStateLayers = onSwapStateLayers,
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
                    onStrokeStart = { onStrokeStart() },
                    onStrokeEnd = onStrokeEnd,
                    onGridChange = onGridChange,
                    tool = selectedTool,
                    showGrid = showGrid,
                    symmetry = symmetry,
                    selection = selection,
                    onSelectionChange = onSelectionChange,
                    onColorPicked = onColorSelected,
                    gestureKey = editorKey,
                    modifier = Modifier.height(if (currentGrid.width == currentGrid.height) 260.dp else 190.dp),
                )
                SwitchPixelToolbar(
                    selectedTool = selectedTool,
                    symmetry = symmetry,
                    showGrid = showGrid,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    hasSelection = selection != null,
                    canPasteSelection = canPasteSelection,
                    onToolSelected = onToolSelected,
                    onSymmetryChange = onSymmetryChange,
                    onShowGridChange = onShowGridChange,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onFill = onFill,
                    onMirrorHorizontal = onMirrorHorizontal,
                    onMirrorVertical = onMirrorVertical,
                    onCopySelection = onCopySelection,
                    onPasteSelection = onPasteSelection,
                    onMoveSelection = onMoveSelection,
                    onRotateSelection = onRotateSelection,
                    onConvertImage = onConvertImage,
                    onOpenAssets = onOpenAssets,
                    onOpenFullscreen = onOpenFullscreen,
                    onClear = onClear,
                )
                if (recentColors.isNotEmpty()) {
                    SwitchRecentColors(recentColors, selectedColor, onColorSelected)
                }
                PixelPaletteEditor(
                    palette = draft.palette,
                    selectedColor = selectedColor,
                    onColorSelected = onColorSelected,
                    onPaletteChange = onPaletteChange,
                )
            }
        }
        item {
            SwitchDesignCheckSection(draft)
        }
    }
}

@Composable
private fun SwitchImageSlotRow(
    title: String,
    imageUri: String?,
    hash: String?,
    busy: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (imageUri == null) {
                        stringResource(R.string.switch_style_creator_image_not_set)
                    } else {
                        hash?.let { "SHA256 ${it.take(10)}...${it.takeLast(6)}" }
                            ?: stringResource(R.string.switch_style_creator_image_ready)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (imageUri == null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            FilledTonalButton(onClick = onSelect, enabled = !busy) {
                Icon(Icons.Rounded.Image, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.switch_style_creator_select_image))
            }
            if (imageUri != null) {
                IconButton(onClick = onRemove, enabled = !busy) {
                    Icon(Icons.Rounded.Delete, stringResource(R.string.switch_style_creator_remove_image))
                }
            }
        }
    }
}

@Composable
private fun SwitchAppearanceEditor(
    draft: CustomSwitchStyle,
    selectedStateOn: Boolean,
    onSelectedStateChange: (Boolean) -> Unit,
    onDraftChange: (CustomSwitchStyle) -> Unit,
    onDraftUpdate: (CustomSwitchStyle) -> Unit,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
) {
    var selectedSection by rememberSaveable {
        mutableIntStateOf(SwitchAppearanceSection.Track.ordinal)
    }
    val currentSection = SwitchAppearanceSection.entries.getOrElse(selectedSection) {
        SwitchAppearanceSection.Track
    }
    PixelEditorSection(stringResource(R.string.switch_style_creator_appearance)) {
        SwitchChoiceChipRow(
            options = listOf(false, true),
            selected = selectedStateOn,
            label = { enabled ->
                stringResource(
                    if (enabled) {
                        R.string.switch_style_creator_state_on
                    } else {
                        R.string.switch_style_creator_state_off
                    },
                )
            },
            onSelected = onSelectedStateChange,
        )
        SwitchChoiceChipRow(
            options = SwitchAppearanceSection.entries,
            selected = currentSection,
            label = { stringResource(it.labelRes()) },
            onSelected = { selectedSection = it.ordinal },
        )
        when (currentSection) {
            SwitchAppearanceSection.Track -> {
                SwitchValueSlider(
                    title = stringResource(R.string.switch_style_creator_track_width),
                    value = draft.trackScaleX,
                    valueRange = 0.65f..1f,
                    valueText = "${(draft.trackScaleX * 100).roundToInt()}%",
                    onValueChange = { onDraftUpdate(draft.copy(trackScaleX = it)) },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
                )
                SwitchValueSlider(
                    title = stringResource(R.string.switch_style_creator_track_height),
                    value = draft.trackScaleY,
                    valueRange = 0.55f..1f,
                    valueText = "${(draft.trackScaleY * 100).roundToInt()}%",
                    onValueChange = { onDraftUpdate(draft.copy(trackScaleY = it)) },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
                )
                SwitchValueSlider(
                    title = stringResource(R.string.switch_style_creator_corner_radius),
                    value = draft.cornerRadiusFraction,
                    valueRange = 0f..0.5f,
                    valueText = "${(draft.cornerRadiusFraction * 200).roundToInt()}%",
                    onValueChange = { onDraftUpdate(draft.copy(cornerRadiusFraction = it)) },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
                )
                SwitchColorField(
                    title = "${stringResource(R.string.switch_style_creator_track_color)} · " +
                        stringResource(
                            if (selectedStateOn) {
                                R.string.switch_style_creator_state_on
                            } else {
                                R.string.switch_style_creator_state_off
                            },
                        ),
                    color = if (selectedStateOn) draft.trackOnColorOverride else draft.trackOffColorOverride,
                    allowNone = true,
                    onColorChange = { color ->
                        onDraftChange(
                            if (selectedStateOn) {
                                draft.copy(trackOnColorOverride = color)
                            } else {
                                draft.copy(trackOffColorOverride = color)
                            },
                        )
                    },
                )
            }
            SwitchAppearanceSection.Border -> {
                SwitchColorField(
                    title = "${stringResource(R.string.switch_style_creator_border_color)} · " +
                        stringResource(
                            if (selectedStateOn) {
                                R.string.switch_style_creator_state_on
                            } else {
                                R.string.switch_style_creator_state_off
                            },
                        ),
                    color = if (selectedStateOn) draft.borderOnColorOverride else draft.borderOffColorOverride,
                    allowNone = true,
                    onColorChange = { color ->
                        onDraftChange(
                            if (selectedStateOn) {
                                draft.copy(borderOnColorOverride = color)
                            } else {
                                draft.copy(borderOffColorOverride = color)
                            },
                        )
                    },
                )
                SwitchValueSlider(
                    title = stringResource(R.string.switch_style_creator_border_width),
                    value = draft.borderWidthDp,
                    valueRange = 0f..4f,
                    valueText = String.format("%.1f dp", draft.borderWidthDp),
                    onValueChange = { onDraftUpdate(draft.copy(borderWidthDp = it)) },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
                )
            }
            SwitchAppearanceSection.Thumb -> {
                SwitchValueSlider(
                    title = stringResource(R.string.switch_style_creator_thumb_size),
                    value = draft.thumbScale,
                    valueRange = 0.55f..1.1f,
                    valueText = "${(draft.thumbScale * 100).roundToInt()}%",
                    onValueChange = { onDraftUpdate(draft.copy(thumbScale = it)) },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
                )
                SwitchValueSlider(
                    title = stringResource(R.string.switch_style_creator_thumb_padding),
                    value = draft.thumbPaddingDp,
                    valueRange = 0f..8f,
                    valueText = String.format("%.1f dp", draft.thumbPaddingDp),
                    onValueChange = { onDraftUpdate(draft.copy(thumbPaddingDp = it)) },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
                )
                SwitchValueSlider(
                    title = stringResource(R.string.switch_style_creator_thumb_travel),
                    value = draft.thumbTravel,
                    valueRange = 0.5f..1f,
                    valueText = "${(draft.thumbTravel * 100).roundToInt()}%",
                    onValueChange = { onDraftUpdate(draft.copy(thumbTravel = it)) },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
                )
                SwitchColorField(
                    title = "${stringResource(R.string.switch_style_creator_thumb_color)} · " +
                        stringResource(
                            if (selectedStateOn) {
                                R.string.switch_style_creator_state_on
                            } else {
                                R.string.switch_style_creator_state_off
                            },
                        ),
                    color = if (selectedStateOn) draft.thumbOnColorOverride else draft.thumbOffColorOverride,
                    allowNone = true,
                    onColorChange = { color ->
                        onDraftChange(
                            if (selectedStateOn) {
                                draft.copy(thumbOnColorOverride = color)
                            } else {
                                draft.copy(thumbOffColorOverride = color)
                            },
                        )
                    },
                )
            }
            SwitchAppearanceSection.Effects -> {
                SwitchColorField(
                    title = stringResource(R.string.switch_style_creator_shadow_color),
                    color = draft.shadowColor,
                    onColorChange = { it?.let { color -> onDraftChange(draft.copy(shadowColor = color)) } },
                )
                SwitchValueSlider(
                    title = stringResource(R.string.switch_style_creator_shadow_radius),
                    value = draft.shadowRadiusDp,
                    valueRange = 0f..8f,
                    valueText = String.format("%.1f dp", draft.shadowRadiusDp),
                    onValueChange = { onDraftUpdate(draft.copy(shadowRadiusDp = it)) },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
                )
                SwitchColorField(
                    title = stringResource(R.string.switch_style_creator_glow_color),
                    color = draft.glowColor,
                    onColorChange = { it?.let { color -> onDraftChange(draft.copy(glowColor = color)) } },
                )
                SwitchValueSlider(
                    title = stringResource(R.string.switch_style_creator_glow_radius),
                    value = draft.glowRadiusDp,
                    valueRange = 0f..8f,
                    valueText = String.format("%.1f dp", draft.glowRadiusDp),
                    onValueChange = { onDraftUpdate(draft.copy(glowRadiusDp = it)) },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
                )
                SwitchValueSlider(
                    title = stringResource(R.string.switch_style_creator_disabled_alpha),
                    value = draft.disabledAlpha,
                    valueRange = 0.2f..1f,
                    valueText = "${(draft.disabledAlpha * 100).roundToInt()}%",
                    onValueChange = { onDraftUpdate(draft.copy(disabledAlpha = it)) },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
                )
            }
            SwitchAppearanceSection.Transition -> {
                SwitchValueSlider(
                    title = stringResource(R.string.switch_style_creator_transition_duration),
                    value = draft.transitionDurationMillis.toFloat(),
                    valueRange = MIN_SWITCH_TRANSITION_DURATION_MS.toFloat()..
                        MAX_SWITCH_TRANSITION_DURATION_MS.toFloat(),
                    valueText = "${draft.transitionDurationMillis} ms",
                    onValueChange = {
                        onDraftUpdate(draft.copy(transitionDurationMillis = it.roundToInt()))
                    },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
                )
                SwitchChoiceChipRow(
                    options = SwitchTransitionEasing.entries,
                    selected = draft.transitionEasing,
                    label = { stringResource(it.labelRes()) },
                    onSelected = { onDraftChange(draft.copy(transitionEasing = it)) },
                )
            }
        }
    }
}

@Composable
private fun SwitchLayerPanel(
    draft: CustomSwitchStyle,
    layers: List<SwitchPixelLayer>,
    selected: SwitchPixelLayer,
    onSelected: (SwitchPixelLayer) -> Unit,
    onCopyLayer: () -> Unit,
    onPasteLayer: () -> Unit,
    canPasteLayer: Boolean,
    onSwapStateLayers: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        layers.forEach { layer ->
            val grid = draft.gridFor(layer)
            Surface(
                onClick = { onSelected(layer) },
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(
                    if (selected == layer) 2.dp else 1.dp,
                    if (selected == layer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                color = if (selected == layer) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Canvas(Modifier.size(width = 62.dp, height = 30.dp)) {
                        drawPixelLayer(grid, Offset.Zero, size)
                    }
                    Text(stringResource(layer.labelRes()), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onCopyLayer) {
            Icon(Icons.Rounded.ContentCopy, stringResource(R.string.switch_style_creator_copy_layer))
        }
        IconButton(onClick = onPasteLayer, enabled = canPasteLayer) {
            Icon(Icons.Rounded.ContentPaste, stringResource(R.string.switch_style_creator_paste_layer))
        }
        IconButton(onClick = onSwapStateLayers) {
            Icon(Icons.Rounded.SwapHoriz, stringResource(R.string.switch_style_creator_swap_states))
        }
    }
}

@Composable
private fun SwitchPixelToolbar(
    selectedTool: PixelCanvasTool,
    symmetry: PixelSymmetry,
    showGrid: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    hasSelection: Boolean,
    canPasteSelection: Boolean,
    onToolSelected: (PixelCanvasTool) -> Unit,
    onSymmetryChange: (PixelSymmetry) -> Unit,
    onShowGridChange: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFill: () -> Unit,
    onMirrorHorizontal: () -> Unit,
    onMirrorVertical: () -> Unit,
    onCopySelection: () -> Unit,
    onPasteSelection: () -> Unit,
    onMoveSelection: (Int, Int) -> Unit,
    onRotateSelection: () -> Unit,
    onConvertImage: () -> Unit,
    onOpenAssets: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SwitchToolButton(Icons.AutoMirrored.Rounded.Undo, R.string.component_creator_undo, false, canUndo, onUndo)
        SwitchToolButton(Icons.AutoMirrored.Rounded.Redo, R.string.component_creator_redo, false, canRedo, onRedo)
        SwitchToolButton(Icons.Rounded.Edit, R.string.card_style_creator_tool_pencil, selectedTool == PixelCanvasTool.Pencil, true) {
            onToolSelected(PixelCanvasTool.Pencil)
        }
        SwitchToolButton(Icons.Rounded.Brush, R.string.component_creator_eraser, selectedTool == PixelCanvasTool.Eraser, true) {
            onToolSelected(PixelCanvasTool.Eraser)
        }
        SwitchToolButton(Icons.Rounded.Colorize, R.string.card_style_creator_tool_eyedropper, selectedTool == PixelCanvasTool.Eyedropper, true) {
            onToolSelected(PixelCanvasTool.Eyedropper)
        }
        SwitchToolButton(Icons.Rounded.FormatColorFill, R.string.card_style_creator_tool_flood_fill, selectedTool == PixelCanvasTool.FloodFill, true) {
            onToolSelected(PixelCanvasTool.FloodFill)
        }
        SwitchToolButton(Icons.Rounded.SelectAll, R.string.switch_style_creator_select_region, selectedTool == PixelCanvasTool.Select, true) {
            onToolSelected(PixelCanvasTool.Select)
        }
        SwitchToolButton(Icons.Rounded.Palette, R.string.component_creator_fill, false, true, onFill)
        SwitchToolButton(Icons.Rounded.Flip, R.string.component_creator_mirror, false, true, onMirrorHorizontal)
        SwitchToolButton(Icons.Rounded.FlipToBack, R.string.card_style_creator_mirror_vertical, false, true, onMirrorVertical)
        SwitchToolButton(Icons.Rounded.ContentCopy, R.string.switch_style_creator_copy_selection, false, hasSelection, onCopySelection)
        SwitchToolButton(Icons.Rounded.ContentPaste, R.string.switch_style_creator_paste_selection, false, canPasteSelection, onPasteSelection)
        SwitchToolButton(Icons.AutoMirrored.Rounded.RotateRight, R.string.switch_style_creator_rotate_selection, false, hasSelection, onRotateSelection)
        SwitchToolButton(Icons.Rounded.Image, R.string.card_style_creator_import_image, false, true, onConvertImage)
        SwitchToolButton(Icons.Rounded.DashboardCustomize, R.string.card_style_creator_asset_library, false, true, onOpenAssets)
        SwitchToolButton(Icons.Rounded.GridOn, R.string.card_style_creator_toggle_grid, showGrid, true) {
            onShowGridChange(!showGrid)
        }
        SwitchToolButton(Icons.Rounded.Fullscreen, R.string.card_style_creator_fullscreen, false, true, onOpenFullscreen)
        SwitchToolButton(Icons.Rounded.Delete, R.string.component_creator_clear_layer, false, true, onClear)
    }
    if (hasSelection) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            SwitchToolButton(Icons.AutoMirrored.Rounded.ArrowBack, R.string.switch_style_creator_move_left, false, true) { onMoveSelection(-1, 0) }
            SwitchToolButton(Icons.AutoMirrored.Rounded.ArrowForward, R.string.switch_style_creator_move_right, false, true) { onMoveSelection(1, 0) }
            SwitchToolButton(Icons.Rounded.ArrowUpward, R.string.switch_style_creator_move_up, false, true) { onMoveSelection(0, -1) }
            SwitchToolButton(Icons.Rounded.ArrowDownward, R.string.switch_style_creator_move_down, false, true) { onMoveSelection(0, 1) }
        }
    }
    SwitchChoiceChipRow(
        options = PixelSymmetry.entries,
        selected = symmetry,
        label = { stringResource(it.labelRes()) },
        onSelected = onSymmetryChange,
    )
}

@Composable
private fun SwitchToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, stringResource(labelRes))
        }
    }
}

@Composable
private fun SwitchRecentColors(
    colors: List<Long>,
    selectedColor: Long,
    onSelected: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.switch_style_creator_recent_colors), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { color ->
                val shape = RoundedCornerShape(4.dp)
                Box(
                    Modifier
                        .size(30.dp)
                        .background(Color(color.toInt()), shape)
                        .border(
                            if (color == selectedColor) 3.dp else 1.dp,
                            if (color == selectedColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape,
                        )
                        .clickable { onSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun SwitchColorField(
    title: String,
    color: Long?,
    allowNone: Boolean = false,
    onColorChange: (Long?) -> Unit,
) {
    var value by remember(color) { mutableStateOf(color?.let(::formatArgbHex).orEmpty()) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (color != null) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(Color(color.toInt()), RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = { next ->
                value = next.take(9)
                parseArgbHex(value)?.let { onColorChange(it) }
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(title) },
        )
        if (allowNone) {
            TextButton(onClick = {
                value = ""
                onColorChange(null)
            }) {
                Text(stringResource(R.string.switch_style_creator_tint_none))
            }
        }
    }
}

@Composable
private fun SwitchValueSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
) {
    var active by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(valueText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value,
            onValueChange = { next ->
                if (!active) {
                    active = true
                    onInteractionStart()
                }
                onValueChange(next)
            },
            onValueChangeFinished = {
                if (active) {
                    active = false
                    onInteractionEnd()
                }
            },
            valueRange = valueRange,
        )
    }
}

@Composable
private fun SwitchDesignCheckSection(draft: CustomSwitchStyle) {
    val issues = buildList {
        if (draft.source == CustomSwitchSource.Image && draft.imageUri == null && draft.imageOnUri == null) {
            add(stringResource(R.string.switch_style_creator_check_image_missing))
        }
        if (draft.source == CustomSwitchSource.Pixel && draft.trackOff.isTransparent()) {
            add(stringResource(R.string.switch_style_creator_check_track_off_empty))
        }
        if (draft.source == CustomSwitchSource.Pixel && draft.trackOn.isTransparent()) {
            add(stringResource(R.string.switch_style_creator_check_track_on_empty))
        }
        if (draft.thumbOff.isTransparent() && draft.thumbOn.isTransparent()) {
            add(stringResource(R.string.switch_style_creator_check_thumb_empty))
        }
        val imageAppearances = listOf(
            draft.imageAppearanceFor(on = false),
            draft.imageAppearanceFor(on = true),
        )
        if (imageAppearances.any {
                it.zoom > 2f ||
                    kotlin.math.abs(it.offsetX) > 0.65f ||
                    kotlin.math.abs(it.offsetY) > 0.65f
            }
        ) {
            add(stringResource(R.string.switch_style_creator_check_image_clipping))
        }
        if (draft.hasLowThumbContrast()) {
            add(stringResource(R.string.switch_style_creator_check_contrast))
        }
    }
    PixelEditorSection(stringResource(R.string.switch_style_creator_design_check)) {
        if (issues.isEmpty()) {
            Text(
                stringResource(R.string.switch_style_creator_check_passed),
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            issues.forEach { issue ->
                Text("- $issue", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SwitchPixelAssetLibrary(
    currentGrid: PixelGrid,
    primary: Long,
    secondary: Long,
    onDismiss: () -> Unit,
    onApply: (PixelComponentPreset) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.card_style_creator_asset_library),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.switch_style_creator_asset_library_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PixelComponentPreset.entries.forEach { preset ->
                val preview = remember(currentGrid, preset, primary, secondary) {
                    currentGrid.cleared().withPreset(preset, primary, secondary)
                }
                Card(
                    onClick = { onApply(preset) },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Canvas(Modifier.size(width = 92.dp, height = 44.dp)) {
                            drawPixelLayer(preview, Offset.Zero, size)
                        }
                        Text(stringResource(preset.labelRes()), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchPixelFullscreenEditor(
    draft: CustomSwitchStyle,
    editorKey: String,
    selectedLayer: SwitchPixelLayer,
    selectedColor: Long,
    currentGrid: PixelGrid,
    previewChecked: Boolean,
    selectedTool: PixelCanvasTool,
    symmetry: PixelSymmetry,
    showGrid: Boolean,
    selection: PixelSelection?,
    canUndo: Boolean,
    canRedo: Boolean,
    canPasteSelection: Boolean,
    onDismiss: () -> Unit,
    onLayerSelected: (SwitchPixelLayer) -> Unit,
    onColorSelected: (Long) -> Unit,
    onPaletteChange: (List<Long>) -> Unit,
    onPreviewCheckedChange: (Boolean) -> Unit,
    onToolSelected: (PixelCanvasTool) -> Unit,
    onSymmetryChange: (PixelSymmetry) -> Unit,
    onShowGridChange: (Boolean) -> Unit,
    onSelectionChange: (PixelSelection?) -> Unit,
    onStrokeStart: () -> Unit,
    onStrokeEnd: () -> Unit,
    onGridChange: (PixelGrid) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFill: () -> Unit,
    onMirrorHorizontal: () -> Unit,
    onMirrorVertical: () -> Unit,
    onCopySelection: () -> Unit,
    onPasteSelection: () -> Unit,
    onMoveSelection: (Int, Int) -> Unit,
    onRotateSelection: () -> Unit,
    onConvertImage: () -> Unit,
    onOpenAssets: () -> Unit,
    onClear: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.switch_style_creator_fullscreen))
                            Text(
                                stringResource(selectedLayer.labelRes()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.close))
                        }
                    },
                    actions = {
                        IconButton(onClick = onUndo, enabled = canUndo) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.component_creator_undo))
                        }
                        IconButton(onClick = onRedo, enabled = canRedo) {
                            Icon(Icons.AutoMirrored.Rounded.Redo, stringResource(R.string.component_creator_redo))
                        }
                    },
                )
            },
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                val availableLayers = if (draft.source == CustomSwitchSource.Image) {
                    listOf(SwitchPixelLayer.ThumbOff, SwitchPixelLayer.ThumbOn)
                } else {
                    SwitchPixelLayer.entries
                }
                val editor: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SwitchChoiceChipRow(
                            options = availableLayers,
                            selected = selectedLayer.takeIf { it in availableLayers } ?: availableLayers.first(),
                            label = { stringResource(it.labelRes()) },
                            onSelected = onLayerSelected,
                        )
                        PixelGridEditor(
                            grid = currentGrid,
                            selectedColor = selectedColor,
                            contentDescription = stringResource(R.string.component_creator_pixel_canvas),
                            onStrokeStart = { onStrokeStart() },
                            onStrokeEnd = onStrokeEnd,
                            onGridChange = onGridChange,
                            onColorPicked = onColorSelected,
                            tool = selectedTool,
                            showGrid = showGrid,
                            symmetry = symmetry,
                            selection = selection,
                            onSelectionChange = onSelectionChange,
                            gestureKey = "fullscreen:$editorKey",
                            constrainHeight = false,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                        SwitchPixelToolbar(
                            selectedTool = selectedTool,
                            symmetry = symmetry,
                            showGrid = showGrid,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            hasSelection = selection != null,
                            canPasteSelection = canPasteSelection,
                            onToolSelected = onToolSelected,
                            onSymmetryChange = onSymmetryChange,
                            onShowGridChange = onShowGridChange,
                            onUndo = onUndo,
                            onRedo = onRedo,
                            onFill = onFill,
                            onMirrorHorizontal = onMirrorHorizontal,
                            onMirrorVertical = onMirrorVertical,
                            onCopySelection = onCopySelection,
                            onPasteSelection = onPasteSelection,
                            onMoveSelection = onMoveSelection,
                            onRotateSelection = onRotateSelection,
                            onConvertImage = onConvertImage,
                            onOpenAssets = onOpenAssets,
                            onOpenFullscreen = onDismiss,
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
                if (maxWidth >= 720.dp) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(0.75f).fillMaxSize(), contentAlignment = Alignment.Center) {
                            SwitchStylePreview(
                                style = draft,
                                checked = previewChecked,
                                onCheckedChange = onPreviewCheckedChange,
                            )
                        }
                        editor(Modifier.weight(1.25f).fillMaxSize())
                    }
                } else {
                    editor(Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun SwitchMotionPage(
    draft: CustomSwitchStyle,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDraftUpdate: (CustomSwitchStyle) -> Unit,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
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
                    onRuleChange = { onDraftUpdate(draft.copy(motion = it)) },
                    onInteractionStart = onInteractionStart,
                    onInteractionEnd = onInteractionEnd,
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
    val checkedProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = style.transitionDurationMillis,
            easing = style.transitionEasing.composeEasing(),
        ),
        label = "switchCreatorChecked",
    )
    val motionProgress = rememberComponentMotionProgress(style.motion, true, "switchCreatorMotion")
    val images = rememberCustomSwitchImages(style)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Canvas(Modifier.size(width = 118.dp, height = 58.dp)) {
                    drawCustomSwitchStyle(
                        style = style,
                        checkedProgress = checkedProgress,
                        enabledAlpha = 1f,
                        motionProgress = motionProgress,
                        images = images,
                    )
                }
                Text(
                    stringResource(R.string.switch_style_creator_disabled_preview),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Canvas(Modifier.size(width = 74.dp, height = 38.dp)) {
                    drawCustomSwitchStyle(
                        style = style,
                        checkedProgress = checkedProgress,
                        enabledAlpha = style.disabledAlpha,
                        motionProgress = -1f,
                        images = images,
                    )
                }
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

private fun SwitchImageBlend.labelRes(): Int = when (this) {
    SwitchImageBlend.Normal -> R.string.switch_style_creator_blend_normal
    SwitchImageBlend.Multiply -> R.string.switch_style_creator_blend_multiply
    SwitchImageBlend.Screen -> R.string.switch_style_creator_blend_screen
    SwitchImageBlend.Add -> R.string.switch_style_creator_blend_add
}

private fun SwitchAppearanceSection.labelRes(): Int = when (this) {
    SwitchAppearanceSection.Track -> R.string.switch_style_creator_track_geometry
    SwitchAppearanceSection.Border -> R.string.switch_style_creator_border
    SwitchAppearanceSection.Thumb -> R.string.switch_style_creator_thumb_geometry
    SwitchAppearanceSection.Effects -> R.string.switch_style_creator_light_effects
    SwitchAppearanceSection.Transition -> R.string.switch_style_creator_transition
}

private fun SwitchTransitionEasing.labelRes(): Int = when (this) {
    SwitchTransitionEasing.Standard -> R.string.switch_style_creator_easing_standard
    SwitchTransitionEasing.Linear -> R.string.switch_style_creator_easing_linear
    SwitchTransitionEasing.Accelerate -> R.string.switch_style_creator_easing_accelerate
    SwitchTransitionEasing.Decelerate -> R.string.switch_style_creator_easing_decelerate
}

private fun PixelSymmetry.labelRes(): Int = when (this) {
    PixelSymmetry.None -> R.string.switch_style_creator_symmetry_none
    PixelSymmetry.Horizontal -> R.string.switch_style_creator_symmetry_horizontal
    PixelSymmetry.Vertical -> R.string.switch_style_creator_symmetry_vertical
    PixelSymmetry.Both -> R.string.switch_style_creator_symmetry_both
}

private fun PixelComponentPreset.labelRes(): Int = when (this) {
    PixelComponentPreset.CornerBrackets -> R.string.card_style_creator_asset_corners
    PixelComponentPreset.SteppedFrame -> R.string.card_style_creator_asset_frame
    PixelComponentPreset.DataLine -> R.string.card_style_creator_asset_data_line
    PixelComponentPreset.SnowCap -> R.string.card_style_creator_asset_snow
    PixelComponentPreset.WaterRipple -> R.string.card_style_creator_asset_water
    PixelComponentPreset.LeafVine -> R.string.card_style_creator_asset_leaf
}

private fun CustomSwitchStyle.gridFor(layer: SwitchPixelLayer): PixelGrid = when (layer) {
    SwitchPixelLayer.TrackOff -> trackOff
    SwitchPixelLayer.TrackOn -> trackOn
    SwitchPixelLayer.ThumbOff -> thumbOff
    SwitchPixelLayer.ThumbOn -> thumbOn
}

private fun CustomSwitchStyle.imageUris(): List<String> = listOfNotNull(imageUri, imageOnUri).distinct()

private fun PixelGrid.isTransparent(): Boolean = pixels.all { it == TRANSPARENT_PIXEL }

private fun CustomSwitchStyle.hasLowThumbContrast(): Boolean {
    fun contrast(track: PixelGrid, thumb: PixelGrid): Float {
        val trackLum = if (source == CustomSwitchSource.Image) {
            argbLuminance(trackBaseColor)
        } else {
            track.averageLuminance(argbLuminance(trackBaseColor))
        }
        val thumbLum = thumb.averageLuminance(argbLuminance(thumbBaseColor))
        val lighter = maxOf(trackLum, thumbLum) + 0.05f
        val darker = minOf(trackLum, thumbLum) + 0.05f
        return lighter / darker
    }
    return contrast(trackOff, thumbOff) < 1.5f || contrast(trackOn, thumbOn) < 1.5f
}

private fun PixelGrid.averageLuminance(fallback: Float): Float {
    val opaque = pixels.filter { it != TRANSPARENT_PIXEL }
    if (opaque.isEmpty()) return fallback
    return opaque.map(::argbLuminance).average().toFloat()
}

private fun argbLuminance(argb: Long): Float {
    fun channel(value: Int): Float {
        val normalized = value / 255f
        return if (normalized <= 0.04045f) normalized / 12.92f
        else Math.pow(((normalized + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    }
    val red = channel(((argb shr 16) and 0xFF).toInt())
    val green = channel(((argb shr 8) and 0xFF).toInt())
    val blue = channel((argb and 0xFF).toInt())
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

private fun Throwable.switchEditorMessage(context: android.content.Context): String {
    return message?.lineSequence()?.firstOrNull()?.take(180)
        ?: context.getString(R.string.component_creator_unknown_error)
}

private const val MAX_SWITCH_PIXEL_HISTORY = 40
private const val MAX_RECENT_SWITCH_COLORS = 8
private const val SWITCH_DRAFT_AUTOSAVE_DELAY_MS = 650L
