package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.HomeLayoutCanvas
import me.weishu.kernelsu.ui.component.custom.CARD_BODY_GRID_HEIGHT
import me.weishu.kernelsu.ui.component.custom.CARD_BORDER_GRID_CELLS
import me.weishu.kernelsu.ui.component.custom.CARD_GRID_WIDTH
import me.weishu.kernelsu.ui.component.custom.CARD_TOP_GRID_HEIGHT
import me.weishu.kernelsu.ui.component.custom.CardPixelLayer
import me.weishu.kernelsu.ui.component.custom.ComponentStyleKind
import me.weishu.kernelsu.ui.component.custom.ComponentStyleStore
import me.weishu.kernelsu.ui.component.custom.CustomCardLayers
import me.weishu.kernelsu.ui.component.custom.CustomCardStyle
import me.weishu.kernelsu.ui.component.custom.CustomCardTarget
import me.weishu.kernelsu.ui.component.custom.CustomNavigationLayers
import me.weishu.kernelsu.ui.component.custom.DEFAULT_PIXEL_PALETTE
import me.weishu.kernelsu.ui.component.custom.NAVIGATION_BODY_GRID_HEIGHT
import me.weishu.kernelsu.ui.component.custom.NAVIGATION_BORDER_GRID_CELLS
import me.weishu.kernelsu.ui.component.custom.NAVIGATION_GRID_WIDTH
import me.weishu.kernelsu.ui.component.custom.NAVIGATION_TOP_GRID_HEIGHT
import me.weishu.kernelsu.ui.component.custom.NavigationPixelLayer
import me.weishu.kernelsu.ui.component.custom.LocalCustomCardStyle
import me.weishu.kernelsu.ui.component.custom.LocalComponentMotionProgressOverride
import me.weishu.kernelsu.ui.component.custom.PixelEditorSection
import me.weishu.kernelsu.ui.component.custom.PixelGrid
import me.weishu.kernelsu.ui.component.custom.PixelGridEditor
import me.weishu.kernelsu.ui.component.custom.PixelCanvasTool
import me.weishu.kernelsu.ui.component.custom.PixelComponentPreset
import me.weishu.kernelsu.ui.component.custom.PixelMotionEditor
import me.weishu.kernelsu.ui.component.custom.PixelPaletteEditor
import me.weishu.kernelsu.ui.component.custom.TRANSPARENT_PIXEL
import me.weishu.kernelsu.ui.component.custom.decodeImageToPixelGrid
import me.weishu.kernelsu.ui.component.custom.drawCustomCardChrome
import me.weishu.kernelsu.ui.component.custom.drawCustomCardInterior
import me.weishu.kernelsu.ui.component.custom.drawCustomNavigationStyle
import me.weishu.kernelsu.ui.component.custom.filledWhere
import me.weishu.kernelsu.ui.component.custom.hasSameDimensionsAs
import me.weishu.kernelsu.ui.component.custom.mirroredHorizontally
import me.weishu.kernelsu.ui.component.custom.mirroredVertically
import me.weishu.kernelsu.ui.component.custom.rememberComponentMotionProgress
import me.weishu.kernelsu.ui.component.custom.withPreset
import me.weishu.kernelsu.ui.component.decoration.LocalUiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.LocalUiDecorationScope
import me.weishu.kernelsu.ui.component.decoration.UiCardDecoration
import me.weishu.kernelsu.ui.component.decoration.UiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.UiDecorationScope
import me.weishu.kernelsu.ui.component.decoration.UiNavigationDecoration
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.screen.home.HomeActions
import me.weishu.kernelsu.ui.screen.home.HomeLayoutCardContent
import me.weishu.kernelsu.ui.screen.home.HomeUiState
import me.weishu.kernelsu.ui.util.HomeLayoutCard
import me.weishu.kernelsu.ui.util.HomeLayoutState
import me.weishu.kernelsu.ui.util.THEME_STORE_FILE_MIME_TYPE
import me.weishu.kernelsu.ui.util.canonicalCloudThemePackageFileName
import me.weishu.kernelsu.ui.util.exportCardComponentStylePackage
import me.weishu.kernelsu.ui.util.prepareCardStyleCloudSubmission
import me.weishu.kernelsu.ui.util.readHomeLayoutState
import me.weishu.kernelsu.ui.util.readComponentStylePackage
import me.weishu.kernelsu.ui.viewmodel.HomeViewModel
import java.util.UUID

private enum class CardCreatorPage {
    Design,
    Motion,
    Library,
}

private enum class CardEditorScope {
    Card,
    BottomBar,
    FloatingBottomBar,
}

private enum class CardPreviewSurface {
    Home,
    Chrome,
}

private enum class CardPreviewLayout {
    Xiaomi,
    Current,
}

private enum class CardPreviewInterface {
    Xiaomi,
    Current,
}

private enum class CardPreviewOrientation {
    Portrait,
    Landscape,
}

private enum class CardPreviewAppearance {
    System,
    Light,
    Dark,
}

private sealed interface CardEditorPendingAction {
    data object Back : CardEditorPendingAction
    data object New : CardEditorPendingAction
    data object Import : CardEditorPendingAction
    data class Load(val style: CustomCardStyle) : CardEditorPendingAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardStyleCreatorScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val store = remember(context) { ComponentStyleStore(context) }
    val homeViewModel = viewModel<HomeViewModel>()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    var mappedHomeLayout by remember(context) { mutableStateOf(readHomeLayoutState(context)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultName = stringResource(R.string.card_style_creator_default_name)
    val saveSuccess = stringResource(R.string.component_creator_saved)
    val applySuccess = stringResource(R.string.component_creator_applied)
    val importSuccess = stringResource(R.string.component_creator_imported)
    val exportSuccess = stringResource(R.string.component_creator_exported)
    val imageImportSuccess = stringResource(R.string.card_style_creator_image_imported)
    val draftRecoveredMessage = stringResource(R.string.card_style_creator_draft_recovered)
    val nameRequired = stringResource(R.string.component_creator_name_required)
    val saveFailed = stringResource(R.string.component_creator_save_failed)
    val deleteFailed = stringResource(R.string.component_creator_delete_failed)
    val cloudDescription = stringResource(R.string.card_style_creator_cloud_description)
    val cloudCategory = stringResource(R.string.card_style_creator_cloud_category)

    var styles by remember { mutableStateOf(store.readCardStyles()) }
    val libraryInitialStyle = remember {
        store.readActiveCardStyle() ?: styles.firstOrNull() ?: starterCardStyle(defaultName)
    }
    val recoveredDraft = remember { store.readCardEditorDraft() }
    val initialStyle = remember { recoveredDraft ?: libraryInitialStyle }
    val initialBaseline = remember {
        styles.firstOrNull { it.id == initialStyle.id } ?: libraryInitialStyle
    }
    val cardSaver = remember {
        Saver<CustomCardStyle, String>(
            save = { it.toJsonString() },
            restore = { raw -> runCatching { CustomCardStyle.fromJsonString(raw) }.getOrNull() },
        )
    }
    var draft by rememberSaveable(stateSaver = cardSaver) { mutableStateOf(initialStyle) }
    var baseline by remember { mutableStateOf(initialBaseline) }
    var selectedPage by rememberSaveable { mutableIntStateOf(CardCreatorPage.Design.ordinal) }
    var editorScope by rememberSaveable { mutableIntStateOf(CardEditorScope.Card.ordinal) }
    var selectedTarget by rememberSaveable { mutableIntStateOf(CustomCardTarget.Default.ordinal) }
    var selectedCardLayer by rememberSaveable { mutableIntStateOf(CardPixelLayer.Top.ordinal) }
    var selectedNavigationLayer by rememberSaveable { mutableIntStateOf(NavigationPixelLayer.Top.ordinal) }
    var selectedColor by rememberSaveable { mutableLongStateOf(DEFAULT_PIXEL_PALETTE[3]) }
    var undoStack by remember { mutableStateOf<List<CustomCardStyle>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<CustomCardStyle>>(emptyList()) }
    var interactionStartDraft by remember { mutableStateOf<CustomCardStyle?>(null) }
    var selectedTool by rememberSaveable { mutableStateOf(PixelCanvasTool.Pencil) }
    var showGrid by rememberSaveable { mutableStateOf(true) }
    var lockedLayerKeys by remember { mutableStateOf(emptySet<String>()) }
    var hiddenLayerKeys by remember { mutableStateOf(emptySet<String>()) }
    var pixelClipboard by remember { mutableStateOf<PixelGrid?>(null) }
    var fullscreenEditor by remember { mutableStateOf(false) }
    var showAssetLibrary by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<CardEditorPendingAction?>(null) }
    var importCollision by remember { mutableStateOf<CustomCardStyle?>(null) }
    var exportSnapshot by remember { mutableStateOf<CustomCardStyle?>(null) }
    var deleteCandidate by remember { mutableStateOf<CustomCardStyle?>(null) }
    var busy by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        mappedHomeLayout = readHomeLayoutState(context)
        homeViewModel.refresh()
        onPauseOrDispose { }
    }

    val currentScope = CardEditorScope.entries.getOrElse(editorScope) { CardEditorScope.Card }
    val currentTarget = CustomCardTarget.entries.getOrElse(selectedTarget) { CustomCardTarget.Default }
    val currentCardLayer = CardPixelLayer.entries.getOrElse(selectedCardLayer) { CardPixelLayer.Top }
    val currentNavigationLayer = NavigationPixelLayer.entries.getOrElse(selectedNavigationLayer) {
        NavigationPixelLayer.Top
    }
    val editorKey = cardEditorLayerKey(currentScope, currentTarget, currentCardLayer, currentNavigationLayer)
    val currentLayerKey = editorKey

    fun currentGrid(): PixelGrid = when (currentScope) {
        CardEditorScope.Card -> draft.layersFor(currentTarget).layer(currentCardLayer)
        CardEditorScope.BottomBar -> draft.bottomBar.layer(currentNavigationLayer)
        CardEditorScope.FloatingBottomBar -> draft.floatingBottomBar.layer(currentNavigationLayer)
    }

    fun withCurrentGrid(source: CustomCardStyle, grid: PixelGrid): CustomCardStyle {
        if (!grid.hasSameDimensionsAs(currentGrid())) {
            return source
        }
        return when (currentScope) {
            CardEditorScope.Card -> source.withLayers(
                currentTarget,
                source.layersFor(currentTarget).withLayer(currentCardLayer, grid),
            )
            CardEditorScope.BottomBar -> source.copy(
                bottomBar = source.bottomBar.withLayer(currentNavigationLayer, grid),
            )
            CardEditorScope.FloatingBottomBar -> source.copy(
                floatingBottomBar = source.floatingBottomBar.withLayer(currentNavigationLayer, grid),
            )
        }
    }

    fun pushUndo(snapshot: CustomCardStyle) {
        if (undoStack.lastOrNull() != snapshot) {
            undoStack = (undoStack + snapshot).takeLast(MAX_PIXEL_HISTORY)
        }
        redoStack = emptyList()
    }

    fun applyDraft(next: CustomCardStyle) {
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
        interactionStartDraft?.let { start ->
            if (start != draft) pushUndo(start)
        }
        interactionStartDraft = null
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        undoStack = undoStack.dropLast(1)
        redoStack = (redoStack + draft).takeLast(MAX_PIXEL_HISTORY)
        draft = previous
        interactionStartDraft = null
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        redoStack = redoStack.dropLast(1)
        undoStack = (undoStack + draft).takeLast(MAX_PIXEL_HISTORY)
        draft = next
        interactionStartDraft = null
    }

    fun showMessage(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun refreshAfterSave(styleId: String) {
        styles = store.readCardStyles()
        val stored = styles.firstOrNull { it.id == styleId }
        if (stored != null) {
            draft = stored
            baseline = stored
            undoStack = emptyList()
            redoStack = emptyList()
            interactionStartDraft = null
            store.clearCardEditorDraft()
        }
    }

    fun saveDraft(apply: Boolean): Boolean {
        if (draft.name.isBlank()) {
            showMessage(nameRequired)
            return false
        }
        val normalized = draft.normalized()
        val saved = store.saveCardStyle(normalized, apply)
        if (!saved) {
            showMessage(saveFailed)
            return false
        }
        refreshAfterSave(normalized.id)
        showMessage(if (apply) applySuccess else saveSuccess)
        return true
    }

    LaunchedEffect(draft, baseline) {
        if (draft != baseline) {
            delay(CARD_DRAFT_AUTOSAVE_DELAY_MS)
            withContext(Dispatchers.IO) { store.saveCardEditorDraft(draft) }
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
                    readComponentStylePackage(context, uri, ComponentStyleKind.Card)
                        .cardStyle ?: error("Card style is missing")
                }
            }.onSuccess { imported ->
                if (styles.any { it.id == imported.id }) {
                    importCollision = imported
                } else if (store.saveCardStyle(imported, apply = false)) {
                    styles = store.readCardStyles()
                    draft = styles.firstOrNull { it.id == imported.id } ?: imported
                    baseline = draft
                    undoStack = emptyList()
                    redoStack = emptyList()
                    selectedPage = CardCreatorPage.Design.ordinal
                    showMessage(importSuccess)
                } else {
                    showMessage(saveFailed)
                }
            }.onFailure { error ->
                showMessage(error.componentEditorMessage(context))
            }
            busy = false
        }
    }

    val pixelImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (busy || currentLayerKey in lockedLayerKeys) return@rememberLauncherForActivityResult
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
                showMessage(imageImportSuccess)
            }.onFailure { error ->
                showMessage(error.componentEditorMessage(context))
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
                    val result = exportCardComponentStylePackage(context, snapshot, uri)
                    require(result.success) { result.error?.message ?: "Unable to export card style" }
                    require(result.warnings.isEmpty()) { "Card style package contains unavailable resources" }
                }
            }.onSuccess {
                showMessage(exportSuccess)
            }.onFailure { error ->
                showMessage(error.componentEditorMessage(context))
            }
            busy = false
        }
    }

    fun requestExport(style: CustomCardStyle) {
        exportSnapshot = style.normalized()
        exportLauncher.launch(canonicalCloudThemePackageFileName(style.name))
    }

    fun openCloudSubmission() {
        if (busy || !saveDraft(apply = false)) return
        val snapshot = draft.normalized()
        busy = true
        coroutineScope.launch {
            runCatching {
                prepareCardStyleCloudSubmission(
                    context = context,
                    style = snapshot,
                    description = cloudDescription,
                    categoryName = cloudCategory,
                )
            }.onSuccess {
                navigator.push(Route.CloudThemeCreatorSubmission)
            }.onFailure { error ->
                showMessage(error.componentEditorMessage(context))
            }
            busy = false
        }
    }

    val dirty = draft != baseline
    val popBack = dropUnlessResumed { navigator.pop() }

    fun performPendingAction(action: CardEditorPendingAction) {
        when (action) {
            CardEditorPendingAction.Back -> {
                store.clearCardEditorDraft()
                popBack()
            }
            CardEditorPendingAction.New -> {
                draft = starterCardStyle(defaultName)
                undoStack = emptyList()
                redoStack = emptyList()
                interactionStartDraft = null
                selectedPage = CardCreatorPage.Design.ordinal
            }
            CardEditorPendingAction.Import -> {
                importLauncher.launch(arrayOf(THEME_STORE_FILE_MIME_TYPE, "application/zip"))
            }
            is CardEditorPendingAction.Load -> {
                draft = action.style
                baseline = action.style
                undoStack = emptyList()
                redoStack = emptyList()
                interactionStartDraft = null
                store.clearCardEditorDraft()
                selectedPage = CardCreatorPage.Design.ordinal
            }
        }
    }

    fun requestAction(action: CardEditorPendingAction) {
        if (dirty) pendingAction = action else performPendingAction(action)
    }

    BackHandler(enabled = dirty && !fullscreenEditor) {
        requestAction(CardEditorPendingAction.Back)
    }

    LaunchedEffect(Unit) {
        if (recoveredDraft != null && recoveredDraft != initialBaseline) {
            showMessage(draftRecoveredMessage)
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
                        Text(stringResource(R.string.card_style_creator_title))
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
                    IconButton(onClick = { requestAction(CardEditorPendingAction.Back) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            requestAction(CardEditorPendingAction.New)
                        },
                        enabled = !busy,
                    ) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.component_creator_new))
                    }
                    IconButton(
                        onClick = { requestAction(CardEditorPendingAction.Import) },
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
            val currentPage = CardCreatorPage.entries.getOrElse(selectedPage) {
                CardCreatorPage.Design
            }
            CreatorTabs(
                selected = currentPage.ordinal,
                onSelected = { selectedPage = it },
            )
            when (currentPage) {
                CardCreatorPage.Design -> CardDesignPage(
                    draft = draft,
                    homeUiState = homeUiState,
                    mappedHomeLayout = mappedHomeLayout,
                    editorKey = editorKey,
                    editorScope = currentScope,
                    selectedTarget = currentTarget,
                    selectedCardLayer = currentCardLayer,
                    selectedNavigationLayer = currentNavigationLayer,
                    selectedColor = selectedColor,
                    currentGrid = currentGrid(),
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    selectedTool = selectedTool,
                    showGrid = showGrid,
                    layerLocked = currentLayerKey in lockedLayerKeys,
                    lockedLayerKeys = lockedLayerKeys,
                    hiddenLayerKeys = hiddenLayerKeys,
                    canPaste = pixelClipboard?.hasSameDimensionsAs(currentGrid()) == true,
                    onDraftChange = ::applyDraft,
                    onScopeSelected = { editorScope = it.ordinal },
                    onTargetSelected = {
                        editorScope = CardEditorScope.Card.ordinal
                        selectedTarget = it.ordinal
                    },
                    onCardLayerSelected = { selectedCardLayer = it.ordinal },
                    onNavigationLayerSelected = { selectedNavigationLayer = it.ordinal },
                    onColorSelected = { selectedColor = it },
                    onPaletteChange = { applyDraft(draft.copy(palette = it).normalized()) },
                    onToolSelected = { selectedTool = it },
                    onShowGridChange = { showGrid = it },
                    onStrokeStart = { beginInteraction() },
                    onStrokeEnd = ::finishInteraction,
                    onGridChange = ::updateGrid,
                    onUndo = ::undo,
                    onRedo = ::redo,
                    onFill = {
                        if (currentLayerKey in lockedLayerKeys) return@CardDesignPage
                        applyDraft(withCurrentGrid(draft,
                            currentGrid().filledWhere(selectedColor) { x, y, width, height ->
                                isCardCreatorCellEditable(
                                    currentScope,
                                    currentCardLayer,
                                    currentNavigationLayer,
                                    x,
                                    y,
                                    width,
                                    height,
                                )
                            }
                        ))
                    },
                    onMirrorHorizontal = {
                        if (currentLayerKey !in lockedLayerKeys) {
                            applyDraft(withCurrentGrid(draft, currentGrid().mirroredHorizontally()))
                        }
                    },
                    onMirrorVertical = {
                        if (currentLayerKey !in lockedLayerKeys) {
                            applyDraft(withCurrentGrid(draft, currentGrid().mirroredVertically()))
                        }
                    },
                    onClear = {
                        if (currentLayerKey !in lockedLayerKeys) {
                            applyDraft(withCurrentGrid(draft, currentGrid().cleared()))
                        }
                    },
                    onToggleLayerLock = { layerKey ->
                        lockedLayerKeys = if (layerKey in lockedLayerKeys) {
                            lockedLayerKeys - layerKey
                        } else {
                            lockedLayerKeys + layerKey
                        }
                    },
                    onToggleLayerVisibility = { layerKey ->
                        hiddenLayerKeys = if (layerKey in hiddenLayerKeys) {
                            hiddenLayerKeys - layerKey
                        } else {
                            hiddenLayerKeys + layerKey
                        }
                    },
                    onCopyLayer = { pixelClipboard = currentGrid() },
                    onPasteLayer = {
                        pixelClipboard?.takeIf { it.hasSameDimensionsAs(currentGrid()) }?.let { grid ->
                            if (currentLayerKey !in lockedLayerKeys) applyDraft(withCurrentGrid(draft, grid))
                        }
                    },
                    onImportImage = { pixelImageLauncher.launch(arrayOf("image/*")) },
                    onOpenAssetLibrary = { showAssetLibrary = true },
                    onOpenFullscreen = { fullscreenEditor = true },
                    onOpenHomeLayout = { navigator.push(Route.HomeLayout) },
                )
                CardCreatorPage.Motion -> CardMotionPage(
                    draft = draft,
                    homeUiState = homeUiState,
                    mappedHomeLayout = mappedHomeLayout,
                    selectedTarget = currentTarget,
                    onTargetSelected = { selectedTarget = it.ordinal },
                    onDraftChange = { next ->
                        if (interactionStartDraft != null) draft = next else applyDraft(next)
                    },
                    onMotionInteractionStart = ::beginInteraction,
                    onMotionInteractionEnd = ::finishInteraction,
                    onOpenHomeLayout = { navigator.push(Route.HomeLayout) },
                )
                CardCreatorPage.Library -> CardStyleLibraryPage(
                    styles = styles,
                    activeId = store.readActiveCardStyle()?.id,
                    onLoad = { style -> requestAction(CardEditorPendingAction.Load(style)) },
                    onApply = { style ->
                        if (store.saveCardStyle(style, apply = true)) {
                            styles = store.readCardStyles()
                            showMessage(applySuccess)
                        } else {
                            showMessage(saveFailed)
                        }
                    },
                    onExport = ::requestExport,
                    onDelete = { deleteCandidate = it },
                    onImport = { requestAction(CardEditorPendingAction.Import) },
                    onCloudSubmission = ::openCloudSubmission,
                    busy = busy,
                )
            }
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(stringResource(R.string.card_style_creator_unsaved_title)) },
            text = { Text(stringResource(R.string.card_style_creator_unsaved_message)) },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(stringResource(R.string.card_style_creator_keep_editing))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction = null
                        store.clearCardEditorDraft()
                        performPendingAction(action)
                    },
                ) {
                    Text(stringResource(R.string.card_style_creator_discard_changes))
                }
            },
        )
    }

    importCollision?.let { imported ->
        AlertDialog(
            onDismissRequest = { importCollision = null },
            title = { Text(stringResource(R.string.card_style_creator_import_conflict_title)) },
            text = { Text(stringResource(R.string.card_style_creator_import_conflict_message, imported.name)) },
            dismissButton = {
                TextButton(
                    onClick = {
                        val duplicate = imported.copy(id = "card-${UUID.randomUUID()}")
                        if (store.saveCardStyle(duplicate, apply = false)) {
                            styles = store.readCardStyles()
                            draft = styles.firstOrNull { it.id == duplicate.id } ?: duplicate
                            baseline = draft
                            undoStack = emptyList()
                            redoStack = emptyList()
                            showMessage(importSuccess)
                        } else {
                            showMessage(saveFailed)
                        }
                        importCollision = null
                    },
                ) {
                    Text(stringResource(R.string.card_style_creator_import_as_copy))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (store.saveCardStyle(imported, apply = false)) {
                            styles = store.readCardStyles()
                            draft = styles.firstOrNull { it.id == imported.id } ?: imported
                            baseline = draft
                            undoStack = emptyList()
                            redoStack = emptyList()
                            showMessage(importSuccess)
                        } else {
                            showMessage(saveFailed)
                        }
                        importCollision = null
                    },
                ) {
                    Text(stringResource(R.string.card_style_creator_replace_existing))
                }
            },
        )
    }

    if (showAssetLibrary) {
        val assetPrimary = selectedColor.takeIf { it != TRANSPARENT_PIXEL }
            ?: draft.palette.firstOrNull { it != TRANSPARENT_PIXEL }
            ?: DEFAULT_PIXEL_PALETTE[3]
        val assetSecondary = draft.palette.firstOrNull { it != TRANSPARENT_PIXEL && it != assetPrimary }
            ?: assetPrimary
        PixelAssetLibrarySheet(
            currentGrid = currentGrid(),
            primary = assetPrimary,
            secondary = assetSecondary,
            onDismiss = { showAssetLibrary = false },
            onApply = { preset ->
                val grid = currentGrid().withPreset(
                    preset = preset,
                    primary = assetPrimary,
                    secondary = assetSecondary,
                ) { x, y, width, height ->
                    isCardCreatorCellEditable(
                        currentScope,
                        currentCardLayer,
                        currentNavigationLayer,
                        x,
                        y,
                        width,
                        height,
                    )
                }
                applyDraft(withCurrentGrid(draft, grid))
                showAssetLibrary = false
            },
        )
    }

    if (fullscreenEditor) {
        CardPixelFullscreenEditor(
            draft = draft,
            editorKey = editorKey,
            editorScope = currentScope,
            selectedTarget = currentTarget,
            selectedCardLayer = currentCardLayer,
            selectedNavigationLayer = currentNavigationLayer,
            selectedColor = selectedColor,
            currentGrid = currentGrid(),
            selectedTool = selectedTool,
            showGrid = showGrid,
            layerLocked = currentLayerKey in lockedLayerKeys,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            canPaste = pixelClipboard?.hasSameDimensionsAs(currentGrid()) == true,
            onDismiss = { fullscreenEditor = false },
            onCardLayerSelected = { selectedCardLayer = it.ordinal },
            onNavigationLayerSelected = { selectedNavigationLayer = it.ordinal },
            onColorSelected = { selectedColor = it },
            onPaletteChange = { applyDraft(draft.copy(palette = it).normalized()) },
            onToolSelected = { selectedTool = it },
            onShowGridChange = { showGrid = it },
            onStrokeStart = ::beginInteraction,
            onStrokeEnd = ::finishInteraction,
            onGridChange = ::updateGrid,
            onUndo = ::undo,
            onRedo = ::redo,
            onFill = {
                if (currentLayerKey !in lockedLayerKeys) {
                    applyDraft(withCurrentGrid(draft, currentGrid().filledWhere(selectedColor) { x, y, width, height ->
                        isCardCreatorCellEditable(
                            currentScope,
                            currentCardLayer,
                            currentNavigationLayer,
                            x,
                            y,
                            width,
                            height,
                        )
                    }))
                }
            },
            onMirrorHorizontal = {
                if (currentLayerKey !in lockedLayerKeys) {
                    applyDraft(withCurrentGrid(draft, currentGrid().mirroredHorizontally()))
                }
            },
            onMirrorVertical = {
                if (currentLayerKey !in lockedLayerKeys) {
                    applyDraft(withCurrentGrid(draft, currentGrid().mirroredVertically()))
                }
            },
            onCopy = { pixelClipboard = currentGrid() },
            onPaste = {
                pixelClipboard?.takeIf { it.hasSameDimensionsAs(currentGrid()) }?.let { grid ->
                    if (currentLayerKey !in lockedLayerKeys) applyDraft(withCurrentGrid(draft, grid))
                }
            },
            onImportImage = { pixelImageLauncher.launch(arrayOf("image/*")) },
            onOpenAssets = {
                fullscreenEditor = false
                showAssetLibrary = true
            },
            onClear = {
                if (currentLayerKey !in lockedLayerKeys) applyDraft(withCurrentGrid(draft, currentGrid().cleared()))
            },
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
                        if (store.deleteCardStyle(candidate.id)) {
                            styles = store.readCardStyles()
                            if (draft.id == candidate.id) {
                                val fresh = starterCardStyle(defaultName)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PixelAssetLibrarySheet(
    currentGrid: PixelGrid,
    primary: Long,
    secondary: Long,
    onDismiss: () -> Unit,
    onApply: (PixelComponentPreset) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.card_style_creator_asset_library),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.card_style_creator_asset_library_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PixelComponentPreset.entries.forEach { preset ->
                    val preview = remember(currentGrid, preset, primary, secondary) {
                        currentGrid.cleared().withPreset(preset, primary, secondary)
                    }
                    Card(
                        onClick = { onApply(preset) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PixelPresetPreview(preview)
                            Text(
                                stringResource(preset.labelRes()),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelPresetPreview(grid: PixelGrid) {
    val checker = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(5.dp)),
    ) {
        drawRect(checker)
        val cellWidth = size.width / grid.width
        val cellHeight = size.height / grid.height
        grid.pixels.forEachIndexed { index, argb ->
            if (argb == TRANSPARENT_PIXEL) return@forEachIndexed
            val x = index % grid.width
            val y = index / grid.width
            drawRect(
                color = Color(argb.toInt()),
                topLeft = androidx.compose.ui.geometry.Offset(x * cellWidth, y * cellHeight),
                size = androidx.compose.ui.geometry.Size(cellWidth + 0.4f, cellHeight + 0.4f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardPixelFullscreenEditor(
    draft: CustomCardStyle,
    editorKey: String,
    editorScope: CardEditorScope,
    selectedTarget: CustomCardTarget,
    selectedCardLayer: CardPixelLayer,
    selectedNavigationLayer: NavigationPixelLayer,
    selectedColor: Long,
    currentGrid: PixelGrid,
    selectedTool: PixelCanvasTool,
    showGrid: Boolean,
    layerLocked: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    canPaste: Boolean,
    onDismiss: () -> Unit,
    onCardLayerSelected: (CardPixelLayer) -> Unit,
    onNavigationLayerSelected: (NavigationPixelLayer) -> Unit,
    onColorSelected: (Long) -> Unit,
    onPaletteChange: (List<Long>) -> Unit,
    onToolSelected: (PixelCanvasTool) -> Unit,
    onShowGridChange: (Boolean) -> Unit,
    onStrokeStart: () -> Unit,
    onStrokeEnd: () -> Unit,
    onGridChange: (PixelGrid) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFill: () -> Unit,
    onMirrorHorizontal: () -> Unit,
    onMirrorVertical: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onImportImage: () -> Unit,
    onOpenAssets: () -> Unit,
    onClear: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.card_style_creator_fullscreen))
                            Text(
                                text = if (editorScope == CardEditorScope.Card) {
                                    "${stringResource(selectedTarget.labelRes())} · ${stringResource(selectedCardLayer.labelRes())}"
                                } else {
                                    "${stringResource(editorScope.labelRes())} · ${stringResource(selectedNavigationLayer.labelRes())}"
                                },
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                val wide = maxWidth >= 720.dp
                val editor: @Composable (Modifier) -> Unit = { modifier ->
                    Column(
                        modifier = modifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (editorScope == CardEditorScope.Card) {
                            ChoiceChipRow(
                                options = CardPixelLayer.entries,
                                selected = selectedCardLayer,
                                label = { stringResource(it.labelRes()) },
                                onSelected = onCardLayerSelected,
                            )
                        } else {
                            ChoiceChipRow(
                                options = NavigationPixelLayer.entries,
                                selected = selectedNavigationLayer,
                                label = { stringResource(it.labelRes()) },
                                onSelected = onNavigationLayerSelected,
                            )
                        }
                        PixelGridEditor(
                            grid = currentGrid,
                            selectedColor = selectedColor,
                            contentDescription = stringResource(R.string.component_creator_pixel_canvas),
                            onStrokeStart = { onStrokeStart() },
                            onGridChange = onGridChange,
                            onStrokeEnd = onStrokeEnd,
                            onColorPicked = onColorSelected,
                            tool = selectedTool,
                            showGrid = showGrid,
                            gestureKey = "fullscreen:$editorKey",
                            constrainHeight = false,
                            isCellEditable = { x, y, width, height ->
                                !layerLocked && isCardCreatorCellEditable(
                                    editorScope,
                                    selectedCardLayer,
                                    selectedNavigationLayer,
                                    x,
                                    y,
                                    width,
                                    height,
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                        CardPixelToolbar(
                            selectedTool = selectedTool,
                            showGrid = showGrid,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            canEdit = !layerLocked,
                            canPaste = canPaste,
                            onToolSelected = onToolSelected,
                            onShowGridChange = onShowGridChange,
                            onUndo = onUndo,
                            onRedo = onRedo,
                            onFill = onFill,
                            onMirrorHorizontal = onMirrorHorizontal,
                            onMirrorVertical = onMirrorVertical,
                            onCopy = onCopy,
                            onPaste = onPaste,
                            onImportImage = onImportImage,
                            onOpenAssets = onOpenAssets,
                            onFullscreen = onDismiss,
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
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FullscreenStylePreview(
                            draft = draft,
                            editorScope = editorScope,
                            selectedTarget = selectedTarget,
                            modifier = Modifier.weight(0.72f),
                        )
                        editor(Modifier.weight(1.28f).fillMaxSize())
                    }
                } else {
                    editor(Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun FullscreenStylePreview(
    draft: CustomCardStyle,
    editorScope: CardEditorScope,
    selectedTarget: CustomCardTarget,
    modifier: Modifier = Modifier,
) {
    val motionProgress = rememberComponentMotionProgress(draft.motion, true, "fullscreenCardPreview")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.card_style_creator_live_preview),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        when (editorScope) {
            CardEditorScope.Card -> PreviewTargetCard(
                style = draft,
                target = selectedTarget,
                selected = true,
                motionProgress = motionProgress,
                height = 128.dp,
                onClick = {},
            )
            CardEditorScope.BottomBar -> NavigationStylePreview(
                style = draft,
                floating = false,
                selected = true,
                motionProgress = motionProgress,
                onClick = {},
            )
            CardEditorScope.FloatingBottomBar -> NavigationStylePreview(
                style = draft,
                floating = true,
                selected = true,
                motionProgress = motionProgress,
                onClick = {},
            )
        }
        Text(
            stringResource(R.string.card_style_creator_pinch_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CreatorTabs(selected: Int, onSelected: (Int) -> Unit) {
    PrimaryTabRow(selectedTabIndex = selected) {
        CardCreatorPage.entries.forEach { page ->
            Tab(
                selected = selected == page.ordinal,
                onClick = { onSelected(page.ordinal) },
                text = { Text(stringResource(page.labelRes())) },
            )
        }
    }
}

@Composable
private fun CardDesignPage(
    draft: CustomCardStyle,
    homeUiState: HomeUiState,
    mappedHomeLayout: HomeLayoutState,
    editorKey: String,
    editorScope: CardEditorScope,
    selectedTarget: CustomCardTarget,
    selectedCardLayer: CardPixelLayer,
    selectedNavigationLayer: NavigationPixelLayer,
    selectedColor: Long,
    currentGrid: PixelGrid,
    canUndo: Boolean,
    canRedo: Boolean,
    selectedTool: PixelCanvasTool,
    showGrid: Boolean,
    layerLocked: Boolean,
    lockedLayerKeys: Set<String>,
    hiddenLayerKeys: Set<String>,
    canPaste: Boolean,
    onDraftChange: (CustomCardStyle) -> Unit,
    onScopeSelected: (CardEditorScope) -> Unit,
    onTargetSelected: (CustomCardTarget) -> Unit,
    onCardLayerSelected: (CardPixelLayer) -> Unit,
    onNavigationLayerSelected: (NavigationPixelLayer) -> Unit,
    onColorSelected: (Long) -> Unit,
    onPaletteChange: (List<Long>) -> Unit,
    onToolSelected: (PixelCanvasTool) -> Unit,
    onShowGridChange: (Boolean) -> Unit,
    onStrokeStart: () -> Unit,
    onStrokeEnd: () -> Unit,
    onGridChange: (PixelGrid) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFill: () -> Unit,
    onMirrorHorizontal: () -> Unit,
    onMirrorVertical: () -> Unit,
    onClear: () -> Unit,
    onToggleLayerLock: (String) -> Unit,
    onToggleLayerVisibility: (String) -> Unit,
    onCopyLayer: () -> Unit,
    onPasteLayer: () -> Unit,
    onImportImage: () -> Unit,
    onOpenAssetLibrary: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onOpenHomeLayout: () -> Unit,
) {
    val previewStyle = remember(draft, hiddenLayerKeys) {
        draft.withHiddenEditorLayers(hiddenLayerKeys)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        CardStyleIdentityEditor(draft, onDraftChange)
                    }
                    item {
                        CardStylePreviewSection(
                            style = previewStyle,
                            homeUiState = homeUiState,
                            mappedHomeLayout = mappedHomeLayout,
                            selectedTarget = selectedTarget,
                            editorScope = editorScope,
                            onTargetSelected = onTargetSelected,
                            onScopeSelected = onScopeSelected,
                            onOpenHomeLayout = onOpenHomeLayout,
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        CardPixelWorkspace(
                            draft = draft,
                            editorKey = editorKey,
                            editorScope = editorScope,
                            selectedTarget = selectedTarget,
                            selectedCardLayer = selectedCardLayer,
                            selectedNavigationLayer = selectedNavigationLayer,
                            selectedColor = selectedColor,
                            currentGrid = currentGrid,
                            selectedTool = selectedTool,
                            showGrid = showGrid,
                            layerLocked = layerLocked,
                            lockedLayerKeys = lockedLayerKeys,
                            hiddenLayerKeys = hiddenLayerKeys,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            canPaste = canPaste,
                            onDraftChange = onDraftChange,
                            onScopeSelected = onScopeSelected,
                            onTargetSelected = onTargetSelected,
                            onCardLayerSelected = onCardLayerSelected,
                            onNavigationLayerSelected = onNavigationLayerSelected,
                            onColorSelected = onColorSelected,
                            onPaletteChange = onPaletteChange,
                            onToolSelected = onToolSelected,
                            onShowGridChange = onShowGridChange,
                            onStrokeStart = onStrokeStart,
                            onStrokeEnd = onStrokeEnd,
                            onGridChange = onGridChange,
                            onUndo = onUndo,
                            onRedo = onRedo,
                            onFill = onFill,
                            onMirrorHorizontal = onMirrorHorizontal,
                            onMirrorVertical = onMirrorVertical,
                            onClear = onClear,
                            onToggleLayerLock = onToggleLayerLock,
                            onToggleLayerVisibility = onToggleLayerVisibility,
                            onCopyLayer = onCopyLayer,
                            onPasteLayer = onPasteLayer,
                            onImportImage = onImportImage,
                            onOpenAssetLibrary = onOpenAssetLibrary,
                            onOpenFullscreen = onOpenFullscreen,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    CardStylePreviewSection(
                        style = previewStyle,
                        homeUiState = homeUiState,
                        mappedHomeLayout = mappedHomeLayout,
                        selectedTarget = selectedTarget,
                        editorScope = editorScope,
                        onTargetSelected = onTargetSelected,
                        onScopeSelected = onScopeSelected,
                        onOpenHomeLayout = onOpenHomeLayout,
                    )
                }
                item {
                    CardPixelWorkspace(
                        draft = draft,
                        editorKey = editorKey,
                        editorScope = editorScope,
                        selectedTarget = selectedTarget,
                        selectedCardLayer = selectedCardLayer,
                        selectedNavigationLayer = selectedNavigationLayer,
                        selectedColor = selectedColor,
                        currentGrid = currentGrid,
                        selectedTool = selectedTool,
                        showGrid = showGrid,
                        layerLocked = layerLocked,
                        lockedLayerKeys = lockedLayerKeys,
                        hiddenLayerKeys = hiddenLayerKeys,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        canPaste = canPaste,
                        onDraftChange = onDraftChange,
                        onScopeSelected = onScopeSelected,
                        onTargetSelected = onTargetSelected,
                        onCardLayerSelected = onCardLayerSelected,
                        onNavigationLayerSelected = onNavigationLayerSelected,
                        onColorSelected = onColorSelected,
                        onPaletteChange = onPaletteChange,
                        onToolSelected = onToolSelected,
                        onShowGridChange = onShowGridChange,
                        onStrokeStart = onStrokeStart,
                        onStrokeEnd = onStrokeEnd,
                        onGridChange = onGridChange,
                        onUndo = onUndo,
                        onRedo = onRedo,
                        onFill = onFill,
                        onMirrorHorizontal = onMirrorHorizontal,
                        onMirrorVertical = onMirrorVertical,
                        onClear = onClear,
                        onToggleLayerLock = onToggleLayerLock,
                        onToggleLayerVisibility = onToggleLayerVisibility,
                        onCopyLayer = onCopyLayer,
                        onPasteLayer = onPasteLayer,
                        onImportImage = onImportImage,
                        onOpenAssetLibrary = onOpenAssetLibrary,
                        onOpenFullscreen = onOpenFullscreen,
                    )
                }
                item {
                    CardStyleIdentityEditor(draft, onDraftChange)
                }
            }
        }
    }
}

@Composable
private fun CardStyleIdentityEditor(
    draft: CustomCardStyle,
    onDraftChange: (CustomCardStyle) -> Unit,
) {
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

@Composable
private fun CardStylePreviewSection(
    style: CustomCardStyle,
    homeUiState: HomeUiState,
    mappedHomeLayout: HomeLayoutState,
    selectedTarget: CustomCardTarget,
    editorScope: CardEditorScope,
    onTargetSelected: (CustomCardTarget) -> Unit,
    onScopeSelected: (CardEditorScope) -> Unit,
    onOpenHomeLayout: () -> Unit,
) {
    PixelEditorSection(stringResource(R.string.card_style_creator_xiaomi_preview)) {
        XiaomiCardStylePreview(
            style = style,
            homeUiState = homeUiState,
            mappedHomeLayout = mappedHomeLayout,
            selectedTarget = selectedTarget,
            selectedScope = editorScope,
            onTargetSelected = onTargetSelected,
            onScopeSelected = onScopeSelected,
            onOpenHomeLayout = onOpenHomeLayout,
        )
    }
}

@Composable
private fun CardPixelWorkspace(
    draft: CustomCardStyle,
    editorKey: String,
    editorScope: CardEditorScope,
    selectedTarget: CustomCardTarget,
    selectedCardLayer: CardPixelLayer,
    selectedNavigationLayer: NavigationPixelLayer,
    selectedColor: Long,
    currentGrid: PixelGrid,
    selectedTool: PixelCanvasTool,
    showGrid: Boolean,
    layerLocked: Boolean,
    lockedLayerKeys: Set<String>,
    hiddenLayerKeys: Set<String>,
    canUndo: Boolean,
    canRedo: Boolean,
    canPaste: Boolean,
    onDraftChange: (CustomCardStyle) -> Unit,
    onScopeSelected: (CardEditorScope) -> Unit,
    onTargetSelected: (CustomCardTarget) -> Unit,
    onCardLayerSelected: (CardPixelLayer) -> Unit,
    onNavigationLayerSelected: (NavigationPixelLayer) -> Unit,
    onColorSelected: (Long) -> Unit,
    onPaletteChange: (List<Long>) -> Unit,
    onToolSelected: (PixelCanvasTool) -> Unit,
    onShowGridChange: (Boolean) -> Unit,
    onStrokeStart: () -> Unit,
    onStrokeEnd: () -> Unit,
    onGridChange: (PixelGrid) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFill: () -> Unit,
    onMirrorHorizontal: () -> Unit,
    onMirrorVertical: () -> Unit,
    onClear: () -> Unit,
    onToggleLayerLock: (String) -> Unit,
    onToggleLayerVisibility: (String) -> Unit,
    onCopyLayer: () -> Unit,
    onPasteLayer: () -> Unit,
    onImportImage: () -> Unit,
    onOpenAssetLibrary: () -> Unit,
    onOpenFullscreen: () -> Unit,
) {
    PixelEditorSection(stringResource(R.string.component_creator_edit_area)) {
        ChoiceChipRow(
            options = CardEditorScope.entries,
            selected = editorScope,
            label = { stringResource(it.labelRes()) },
            onSelected = onScopeSelected,
        )
        if (editorScope == CardEditorScope.Card) {
            ChoiceChipRow(
                options = CustomCardTarget.entries,
                selected = selectedTarget,
                label = { stringResource(it.labelRes()) },
                onSelected = onTargetSelected,
            )
            if (selectedTarget != CustomCardTarget.Default && selectedTarget in draft.cardOverrides) {
                OutlinedButton(
                    onClick = {
                        onDraftChange(draft.copy(cardOverrides = draft.cardOverrides - selectedTarget))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.card_style_creator_reset_override))
                }
            }
        }
        CardLayerPanel(
            draft = draft,
            editorScope = editorScope,
            selectedTarget = selectedTarget,
            selectedCardLayer = selectedCardLayer,
            selectedNavigationLayer = selectedNavigationLayer,
            lockedLayerKeys = lockedLayerKeys,
            hiddenLayerKeys = hiddenLayerKeys,
            onCardLayerSelected = onCardLayerSelected,
            onNavigationLayerSelected = onNavigationLayerSelected,
            onToggleLayerLock = onToggleLayerLock,
            onToggleLayerVisibility = onToggleLayerVisibility,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.component_creator_grid_size,
                    currentGrid.width,
                    currentGrid.height,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            if (layerLocked) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = stringResource(R.string.card_style_creator_layer_locked),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        PixelGridEditor(
            grid = currentGrid,
            selectedColor = selectedColor,
            contentDescription = stringResource(R.string.component_creator_pixel_canvas),
            onStrokeStart = { onStrokeStart() },
            onGridChange = onGridChange,
            onStrokeEnd = onStrokeEnd,
            onColorPicked = onColorSelected,
            tool = selectedTool,
            showGrid = showGrid,
            gestureKey = editorKey,
            isCellEditable = { x, y, width, height ->
                !layerLocked && isCardCreatorCellEditable(
                    editorScope,
                    selectedCardLayer,
                    selectedNavigationLayer,
                    x,
                    y,
                    width,
                    height,
                )
            },
            modifier = Modifier.height(
                if (currentGrid.height <= CARD_TOP_GRID_HEIGHT) 180.dp else 280.dp
            ),
        )
        CardPixelToolbar(
            selectedTool = selectedTool,
            showGrid = showGrid,
            canUndo = canUndo,
            canRedo = canRedo,
            canEdit = !layerLocked,
            canPaste = canPaste,
            onToolSelected = onToolSelected,
            onShowGridChange = onShowGridChange,
            onUndo = onUndo,
            onRedo = onRedo,
            onFill = onFill,
            onMirrorHorizontal = onMirrorHorizontal,
            onMirrorVertical = onMirrorVertical,
            onCopy = onCopyLayer,
            onPaste = onPasteLayer,
            onImportImage = onImportImage,
            onOpenAssets = onOpenAssetLibrary,
            onFullscreen = onOpenFullscreen,
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

@Composable
private fun CardLayerPanel(
    draft: CustomCardStyle,
    editorScope: CardEditorScope,
    selectedTarget: CustomCardTarget,
    selectedCardLayer: CardPixelLayer,
    selectedNavigationLayer: NavigationPixelLayer,
    lockedLayerKeys: Set<String>,
    hiddenLayerKeys: Set<String>,
    onCardLayerSelected: (CardPixelLayer) -> Unit,
    onNavigationLayerSelected: (NavigationPixelLayer) -> Unit,
    onToggleLayerLock: (String) -> Unit,
    onToggleLayerVisibility: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.card_style_creator_layers),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (editorScope == CardEditorScope.Card) {
            CardPixelLayer.entries.forEach { layer ->
                val key = cardEditorLayerKey(editorScope, selectedTarget, layer, selectedNavigationLayer)
                CardLayerRow(
                    label = stringResource(layer.labelRes()),
                    pixelCount = draft.layersFor(selectedTarget).layer(layer).pixels.count { it != TRANSPARENT_PIXEL },
                    selected = selectedCardLayer == layer,
                    locked = key in lockedLayerKeys,
                    hidden = key in hiddenLayerKeys,
                    onSelect = { onCardLayerSelected(layer) },
                    onToggleLock = { onToggleLayerLock(key) },
                    onToggleVisibility = { onToggleLayerVisibility(key) },
                )
            }
        } else {
            val navigation = if (editorScope == CardEditorScope.BottomBar) draft.bottomBar else draft.floatingBottomBar
            NavigationPixelLayer.entries.forEach { layer ->
                val key = cardEditorLayerKey(editorScope, selectedTarget, selectedCardLayer, layer)
                CardLayerRow(
                    label = stringResource(layer.labelRes()),
                    pixelCount = navigation.layer(layer).pixels.count { it != TRANSPARENT_PIXEL },
                    selected = selectedNavigationLayer == layer,
                    locked = key in lockedLayerKeys,
                    hidden = key in hiddenLayerKeys,
                    onSelect = { onNavigationLayerSelected(layer) },
                    onToggleLock = { onToggleLayerLock(key) },
                    onToggleVisibility = { onToggleLayerVisibility(key) },
                )
            }
        }
    }
}

@Composable
private fun CardLayerRow(
    label: String,
    pixelCount: Int,
    selected: Boolean,
    locked: Boolean,
    hidden: Boolean,
    onSelect: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleVisibility: () -> Unit,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.card_style_creator_pixel_count, pixelCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = stringResource(R.string.card_style_creator_toggle_layer_visibility),
                )
            }
            IconButton(onClick = onToggleLock) {
                Icon(
                    if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = stringResource(R.string.card_style_creator_toggle_layer_lock),
                )
            }
        }
    }
}

@Composable
private fun CardPixelToolbar(
    selectedTool: PixelCanvasTool,
    showGrid: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    canEdit: Boolean,
    canPaste: Boolean,
    onToolSelected: (PixelCanvasTool) -> Unit,
    onShowGridChange: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFill: () -> Unit,
    onMirrorHorizontal: () -> Unit,
    onMirrorVertical: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onImportImage: () -> Unit,
    onOpenAssets: () -> Unit,
    onFullscreen: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorToolButton(Icons.AutoMirrored.Rounded.Undo, R.string.component_creator_undo, false, canUndo, onUndo)
        EditorToolButton(Icons.AutoMirrored.Rounded.Redo, R.string.component_creator_redo, false, canRedo, onRedo)
        EditorToolButton(Icons.Rounded.Edit, R.string.card_style_creator_tool_pencil, selectedTool == PixelCanvasTool.Pencil, canEdit) {
            onToolSelected(PixelCanvasTool.Pencil)
        }
        EditorToolButton(Icons.Rounded.Brush, R.string.component_creator_eraser, selectedTool == PixelCanvasTool.Eraser, canEdit) {
            onToolSelected(PixelCanvasTool.Eraser)
        }
        EditorToolButton(Icons.Rounded.Colorize, R.string.card_style_creator_tool_eyedropper, selectedTool == PixelCanvasTool.Eyedropper, true) {
            onToolSelected(PixelCanvasTool.Eyedropper)
        }
        EditorToolButton(Icons.Rounded.FormatColorFill, R.string.card_style_creator_tool_flood_fill, selectedTool == PixelCanvasTool.FloodFill, canEdit) {
            onToolSelected(PixelCanvasTool.FloodFill)
        }
        EditorToolButton(Icons.Rounded.Palette, R.string.component_creator_fill, false, canEdit, onFill)
        EditorToolButton(Icons.Rounded.Flip, R.string.component_creator_mirror, false, canEdit, onMirrorHorizontal)
        EditorToolButton(Icons.Rounded.FlipToBack, R.string.card_style_creator_mirror_vertical, false, canEdit, onMirrorVertical)
        EditorToolButton(Icons.Rounded.ContentCopy, R.string.card_style_creator_copy_layer, false, true, onCopy)
        EditorToolButton(Icons.Rounded.ContentPaste, R.string.card_style_creator_paste_layer, false, canEdit && canPaste, onPaste)
        EditorToolButton(Icons.Rounded.Image, R.string.card_style_creator_import_image, false, canEdit, onImportImage)
        EditorToolButton(Icons.Rounded.DashboardCustomize, R.string.card_style_creator_asset_library, false, canEdit, onOpenAssets)
        EditorToolButton(Icons.Rounded.GridOn, R.string.card_style_creator_toggle_grid, showGrid, true) {
            onShowGridChange(!showGrid)
        }
        EditorToolButton(Icons.Rounded.Fullscreen, R.string.card_style_creator_fullscreen, false, true, onFullscreen)
        EditorToolButton(Icons.Rounded.Delete, R.string.component_creator_clear_layer, false, canEdit, onClear)
    }
}

@Composable
private fun EditorToolButton(
    icon: ImageVector,
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
            Icon(icon, contentDescription = stringResource(labelRes))
        }
    }
}

@Composable
private fun CardMotionPage(
    draft: CustomCardStyle,
    homeUiState: HomeUiState,
    mappedHomeLayout: HomeLayoutState,
    selectedTarget: CustomCardTarget,
    onTargetSelected: (CustomCardTarget) -> Unit,
    onDraftChange: (CustomCardStyle) -> Unit,
    onMotionInteractionStart: () -> Unit,
    onMotionInteractionEnd: () -> Unit,
    onOpenHomeLayout: () -> Unit,
) {
    var timelinePlaying by rememberSaveable(draft.id) { mutableStateOf(true) }
    var timelineProgress by rememberSaveable(draft.id) { mutableFloatStateOf(0f) }
    val runningTimelineProgress = rememberComponentMotionProgress(
        rule = draft.motion,
        enabled = timelinePlaying,
        label = "cardStyleTimeline",
    )
    val displayedTimelineProgress = if (timelinePlaying && runningTimelineProgress >= 0f) {
        runningTimelineProgress
    } else {
        timelineProgress
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            XiaomiCardStylePreview(
                style = draft,
                homeUiState = homeUiState,
                mappedHomeLayout = mappedHomeLayout,
                selectedTarget = selectedTarget,
                selectedScope = CardEditorScope.Card,
                onTargetSelected = onTargetSelected,
                onScopeSelected = {},
                onOpenHomeLayout = onOpenHomeLayout,
                motionProgressOverride = displayedTimelineProgress.takeIf { draft.motion.enabled },
            )
        }
        item {
            MotionTimelinePanel(
                rule = draft.motion,
                playing = timelinePlaying,
                progress = displayedTimelineProgress,
                onPlayingChange = { timelinePlaying = it },
                onProgressChange = {
                    timelinePlaying = false
                    timelineProgress = it
                },
            )
        }
        item {
            PixelEditorSection(stringResource(R.string.component_creator_motion_rules)) {
                PixelMotionEditor(
                    rule = draft.motion,
                    onRuleChange = { onDraftChange(draft.copy(motion = it)) },
                    onInteractionStart = onMotionInteractionStart,
                    onInteractionEnd = onMotionInteractionEnd,
                )
            }
        }
    }
}

@Composable
private fun MotionTimelinePanel(
    rule: me.weishu.kernelsu.ui.component.custom.PixelMotionRule,
    playing: Boolean,
    progress: Float,
    onPlayingChange: (Boolean) -> Unit,
    onProgressChange: (Float) -> Unit,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outline
    PixelEditorSection(stringResource(R.string.card_style_creator_timeline)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onPlayingChange(!playing) }, enabled = rule.enabled) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(
                        if (playing) R.string.card_style_creator_pause_preview else R.string.card_style_creator_play_preview
                    ),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.card_style_creator_timeline_position,
                        (progress.coerceIn(0f, 1f) * rule.durationMillis).toInt(),
                        rule.durationMillis,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = onProgressChange,
                    enabled = rule.enabled,
                    valueRange = 0f..1f,
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        ) {
            val centerY = size.height / 2f
            drawLine(
                color = trackColor,
                start = androidx.compose.ui.geometry.Offset(0f, centerY),
                end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                strokeWidth = 2.dp.toPx(),
            )
            listOf(0f, 0.5f, 1f).forEach { marker ->
                drawCircle(
                    color = if (marker <= progress) activeColor else inactiveColor,
                    radius = if (marker == 0.5f) 5.dp.toPx() else 4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width * marker, centerY),
                )
            }
        }
        Text(
            text = stringResource(R.string.card_style_creator_timeline_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun XiaomiCardStylePreview(
    style: CustomCardStyle,
    homeUiState: HomeUiState,
    mappedHomeLayout: HomeLayoutState,
    selectedTarget: CustomCardTarget,
    selectedScope: CardEditorScope,
    onTargetSelected: (CustomCardTarget) -> Unit,
    onScopeSelected: (CardEditorScope) -> Unit,
    onOpenHomeLayout: () -> Unit,
    motionProgressOverride: Float? = null,
) {
    var previewSurfaceIndex by rememberSaveable(style.id) {
        mutableIntStateOf(CardPreviewSurface.Home.ordinal)
    }
    var previewLayoutIndex by rememberSaveable(style.id) {
        mutableIntStateOf(CardPreviewLayout.Xiaomi.ordinal)
    }
    var previewInterfaceIndex by rememberSaveable(style.id) {
        mutableIntStateOf(CardPreviewInterface.Xiaomi.ordinal)
    }
    var previewOrientationIndex by rememberSaveable(style.id) {
        mutableIntStateOf(CardPreviewOrientation.Portrait.ordinal)
    }
    var previewAppearanceIndex by rememberSaveable(style.id) {
        mutableIntStateOf(CardPreviewAppearance.System.ordinal)
    }
    var previewMotionEnabled by rememberSaveable(style.id) { mutableStateOf(true) }
    var previewSettingsExpanded by rememberSaveable(style.id) { mutableStateOf(false) }
    val previewSurface = CardPreviewSurface.entries.getOrElse(previewSurfaceIndex) {
        CardPreviewSurface.Home
    }
    val previewLayout = CardPreviewLayout.entries.getOrElse(previewLayoutIndex) {
        CardPreviewLayout.Xiaomi
    }
    val previewInterface = CardPreviewInterface.entries.getOrElse(previewInterfaceIndex) {
        CardPreviewInterface.Xiaomi
    }
    val previewOrientation = CardPreviewOrientation.entries.getOrElse(previewOrientationIndex) {
        CardPreviewOrientation.Portrait
    }
    val previewAppearance = CardPreviewAppearance.entries.getOrElse(previewAppearanceIndex) {
        CardPreviewAppearance.System
    }
    val currentInterfaceStyle = LocalInterfaceStyle.current
    val automaticMotionProgress = rememberComponentMotionProgress(
        rule = style.motion,
        enabled = previewMotionEnabled && motionProgressOverride == null,
        label = "cardStyleCreatorPreview",
    )
    val motionProgress = motionProgressOverride ?: automaticMotionProgress
    val renderedStyle = if (previewMotionEnabled || motionProgressOverride != null) {
        style
    } else {
        style.copy(motion = style.motion.copy(enabled = false))
    }
    val currentScheme = MaterialTheme.colorScheme
    val previewContent: @Composable () -> Unit = {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.card_style_creator_preview_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.card_style_creator_preview_status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilterChip(
                    selected = selectedTarget == CustomCardTarget.Default && selectedScope == CardEditorScope.Card,
                    onClick = {
                        onScopeSelected(CardEditorScope.Card)
                        onTargetSelected(CustomCardTarget.Default)
                    },
                    label = { Text(stringResource(R.string.card_style_creator_target_default)) },
                )
                IconButton(onClick = { previewSettingsExpanded = !previewSettingsExpanded }) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.card_style_creator_preview_settings),
                        tint = if (previewSettingsExpanded) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            ChoiceChipRow(
                options = CardPreviewSurface.entries,
                selected = previewSurface,
                label = { stringResource(it.labelRes()) },
                onSelected = { previewSurfaceIndex = it.ordinal },
            )
            AnimatedVisibility(visible = previewSettingsExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChoiceChipRow(
                        options = CardPreviewInterface.entries,
                        selected = previewInterface,
                        label = { stringResource(it.labelRes()) },
                        onSelected = { previewInterfaceIndex = it.ordinal },
                    )
                    ChoiceChipRow(
                        options = CardPreviewOrientation.entries,
                        selected = previewOrientation,
                        label = { stringResource(it.labelRes()) },
                        onSelected = { previewOrientationIndex = it.ordinal },
                    )
                    ChoiceChipRow(
                        options = CardPreviewAppearance.entries,
                        selected = previewAppearance,
                        label = { stringResource(it.labelRes()) },
                        onSelected = { previewAppearanceIndex = it.ordinal },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.card_style_creator_preview_motion),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(checked = previewMotionEnabled, onCheckedChange = { previewMotionEnabled = it })
                    }
                }
            }
            when (previewSurface) {
                CardPreviewSurface.Home -> {
                    ChoiceChipRow(
                        options = CardPreviewLayout.entries,
                        selected = previewLayout,
                        label = { stringResource(it.labelRes()) },
                        onSelected = { previewLayoutIndex = it.ordinal },
                    )
                    HomeMappedCardStylePreview(
                        style = renderedStyle,
                        homeUiState = homeUiState,
                        layout = when (previewLayout) {
                            CardPreviewLayout.Xiaomi -> HomeLayoutState(enabled = true)
                            CardPreviewLayout.Current -> mappedHomeLayout.copy(enabled = true)
                        },
                        selectedTarget = selectedTarget.takeIf { selectedScope == CardEditorScope.Card },
                        interfaceStyle = when (previewInterface) {
                            CardPreviewInterface.Xiaomi -> InterfaceStyle.Miuix.value
                            CardPreviewInterface.Current -> currentInterfaceStyle
                        },
                        isLandscape = previewOrientation == CardPreviewOrientation.Landscape,
                        motionProgressOverride = motionProgressOverride,
                        onTargetSelected = { target ->
                            onScopeSelected(CardEditorScope.Card)
                            onTargetSelected(target)
                        },
                    )
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenHomeLayout,
                    ) {
                        Icon(Icons.Rounded.DashboardCustomize, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.card_style_creator_edit_home_layout))
                    }
                }

                CardPreviewSurface.Chrome -> {
                    PreviewTargetCard(
                        style = renderedStyle,
                        target = CustomCardTarget.RebootMenu,
                        selected = selectedScope == CardEditorScope.Card && selectedTarget == CustomCardTarget.RebootMenu,
                        motionProgress = motionProgress,
                        height = 72.dp,
                        onClick = {
                            onScopeSelected(CardEditorScope.Card)
                            onTargetSelected(CustomCardTarget.RebootMenu)
                        },
                    )
                    NavigationStylePreview(
                        style = renderedStyle,
                        floating = false,
                        selected = selectedScope == CardEditorScope.BottomBar,
                        motionProgress = motionProgress,
                        onClick = { onScopeSelected(CardEditorScope.BottomBar) },
                    )
                    NavigationStylePreview(
                        style = renderedStyle,
                        floating = true,
                        selected = selectedScope == CardEditorScope.FloatingBottomBar,
                        motionProgress = motionProgress,
                        onClick = { onScopeSelected(CardEditorScope.FloatingBottomBar) },
                    )
                }
            }
        }
    }
    }
    when (previewAppearance) {
        CardPreviewAppearance.System -> previewContent()
        CardPreviewAppearance.Light -> MaterialTheme(
            colorScheme = lightColorScheme(primary = currentScheme.primary),
            content = previewContent,
        )
        CardPreviewAppearance.Dark -> MaterialTheme(
            colorScheme = darkColorScheme(primary = currentScheme.primary),
            content = previewContent,
        )
    }
}

@Composable
private fun HomeMappedCardStylePreview(
    style: CustomCardStyle,
    homeUiState: HomeUiState,
    layout: HomeLayoutState,
    selectedTarget: CustomCardTarget?,
    interfaceStyle: String,
    isLandscape: Boolean,
    motionProgressOverride: Float?,
    onTargetSelected: (CustomCardTarget) -> Unit,
) {
    val previewActions = remember {
        HomeActions(
            onInstallClick = {},
            onSuperuserClick = {},
            onModuleClick = {},
            onOpenUrl = {},
        )
    }
    val previewDecoration = remember {
        UiDecorationConfig(
            enabled = true,
            card = UiCardDecoration.Custom,
            navigation = UiNavigationDecoration.Custom,
            intensity = 1f,
            opacity = 1f,
            motionEnabled = true,
            scopes = setOf(UiDecorationScope.Home),
        )
    }
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.card_style_creator_live_mapping),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        CompositionLocalProvider(
            LocalInterfaceStyle provides interfaceStyle,
            LocalUiDecorationConfig provides previewDecoration,
            LocalUiDecorationScope provides UiDecorationScope.Home,
            LocalCustomCardStyle provides style,
            LocalComponentMotionProgressOverride provides motionProgressOverride,
        ) {
            HomeLayoutCanvas(
                state = layout,
                modifier = Modifier.fillMaxWidth(),
                selectedCard = selectedTarget?.toHomeLayoutCard(),
                onCardSelected = { card -> onTargetSelected(card.toCustomCardTarget()) },
                isLandscapeOverride = isLandscape,
            ) { item ->
                HomeLayoutCardContent(
                    item = item,
                    state = homeUiState,
                    actions = previewActions,
                    installFeedbackActive = false,
                    forceLkmPreview = true,
                )
            }
        }
    }
}

@Composable
private fun PreviewTargetCard(
    style: CustomCardStyle,
    target: CustomCardTarget,
    selected: Boolean,
    motionProgress: Float,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(onClick = onClick),
    ) {
        Surface(
            modifier = Modifier.matchParentSize(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = shape,
        ) {}
        Canvas(Modifier.matchParentSize()) {
            drawCustomCardInterior(style, target, alpha = 0.72f, motionProgress = motionProgress)
            drawCustomCardChrome(style, target, alpha = 0.96f, motionProgress = motionProgress)
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 13.dp),
        ) {
            Text(
                text = stringResource(target.labelRes()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(target.previewValueRes()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NavigationStylePreview(
    style: CustomCardStyle,
    floating: Boolean,
    selected: Boolean,
    motionProgress: Float,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (floating) 8.dp else 2.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (floating) 58.dp else 50.dp)
            .padding(horizontal = if (floating) 14.dp else 0.dp)
            .clip(shape)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape,
            )
            .clickable(onClick = onClick),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCustomNavigationStyle(
                style = style,
                floating = floating,
                areaHeight = size.height,
                alpha = 0.95f,
                motionProgress = motionProgress,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == 0) 12.dp else 9.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(
                            2.dp,
                            if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            RoundedCornerShape(3.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun CardStyleLibraryPage(
    styles: List<CustomCardStyle>,
    activeId: String?,
    onLoad: (CustomCardStyle) -> Unit,
    onApply: (CustomCardStyle) -> Unit,
    onExport: (CustomCardStyle) -> Unit,
    onDelete: (CustomCardStyle) -> Unit,
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
        items(styles, key = CustomCardStyle::id) { style ->
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
private fun <T> ChoiceChipRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
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

private fun starterCardStyle(name: String): CustomCardStyle {
    val accent = DEFAULT_PIXEL_PALETTE[3]
    val secondary = DEFAULT_PIXEL_PALETTE[5]
    val top = PixelGrid(
        CARD_GRID_WIDTH,
        CARD_TOP_GRID_HEIGHT,
        List(CARD_GRID_WIDTH * CARD_TOP_GRID_HEIGHT) { index ->
            val x = index % CARD_GRID_WIDTH
            val y = index / CARD_GRID_WIDTH
            when {
                y == CARD_TOP_GRID_HEIGHT - 1 -> accent
                y == 1 && x in 2..5 -> secondary
                y == 2 && x in 18..21 -> accent
                else -> TRANSPARENT_PIXEL
            }
        },
    )
    val border = PixelGrid(
        CARD_GRID_WIDTH,
        CARD_BODY_GRID_HEIGHT,
        List(CARD_GRID_WIDTH * CARD_BODY_GRID_HEIGHT) { index ->
            val x = index % CARD_GRID_WIDTH
            val y = index / CARD_GRID_WIDTH
            if (x == 0 || y == 0 || x == CARD_GRID_WIDTH - 1 || y == CARD_BODY_GRID_HEIGHT - 1) {
                if ((x + y) % 3 == 0) secondary else accent
            } else {
                TRANSPARENT_PIXEL
            }
        },
    )
    val interior = PixelGrid(
        CARD_GRID_WIDTH,
        CARD_BODY_GRID_HEIGHT,
        List(CARD_GRID_WIDTH * CARD_BODY_GRID_HEIGHT) { index ->
            val x = index % CARD_GRID_WIDTH
            val y = index / CARD_GRID_WIDTH
            when {
                x >= 19 && y in 7..9 && (x + y) % 2 == 0 -> secondary
                x in 2..4 && y == 9 -> accent
                else -> TRANSPARENT_PIXEL
            }
        },
    )
    val navigationTop = PixelGrid(
        NAVIGATION_GRID_WIDTH,
        NAVIGATION_TOP_GRID_HEIGHT,
        List(NAVIGATION_GRID_WIDTH * NAVIGATION_TOP_GRID_HEIGHT) { index ->
            val x = index % NAVIGATION_GRID_WIDTH
            val y = index / NAVIGATION_GRID_WIDTH
            if (y == NAVIGATION_TOP_GRID_HEIGHT - 1 || (y == 1 && x in 9..14)) accent else TRANSPARENT_PIXEL
        },
    )
    val navigationBorder = PixelGrid(
        NAVIGATION_GRID_WIDTH,
        NAVIGATION_BODY_GRID_HEIGHT,
        List(NAVIGATION_GRID_WIDTH * NAVIGATION_BODY_GRID_HEIGHT) { index ->
            val x = index % NAVIGATION_GRID_WIDTH
            val y = index / NAVIGATION_GRID_WIDTH
            if (x == 0 || y == 0 || x == NAVIGATION_GRID_WIDTH - 1 || y == NAVIGATION_BODY_GRID_HEIGHT - 1) {
                accent
            } else {
                TRANSPARENT_PIXEL
            }
        },
    )
    return CustomCardStyle(
        name = name,
        defaultLayers = CustomCardLayers(top = top, border = border, interior = interior),
        bottomBar = CustomNavigationLayers(top = navigationTop, border = navigationBorder),
        floatingBottomBar = CustomNavigationLayers(top = navigationTop, border = navigationBorder),
    )
}

private fun isCardCreatorCellEditable(
    scope: CardEditorScope,
    cardLayer: CardPixelLayer,
    navigationLayer: NavigationPixelLayer,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
): Boolean = when {
    scope == CardEditorScope.Card && cardLayer == CardPixelLayer.Border -> {
        x < CARD_BORDER_GRID_CELLS || y < CARD_BORDER_GRID_CELLS ||
            x >= width - CARD_BORDER_GRID_CELLS || y >= height - CARD_BORDER_GRID_CELLS
    }
    scope != CardEditorScope.Card && navigationLayer == NavigationPixelLayer.Border -> {
        x < NAVIGATION_BORDER_GRID_CELLS || y < NAVIGATION_BORDER_GRID_CELLS ||
            x >= width - NAVIGATION_BORDER_GRID_CELLS || y >= height - NAVIGATION_BORDER_GRID_CELLS
    }
    else -> true
}

private fun CardCreatorPage.labelRes(): Int = when (this) {
    CardCreatorPage.Design -> R.string.component_creator_tab_design
    CardCreatorPage.Motion -> R.string.component_creator_tab_motion
    CardCreatorPage.Library -> R.string.component_creator_tab_library
}

private fun CardEditorScope.labelRes(): Int = when (this) {
    CardEditorScope.Card -> R.string.card_style_creator_scope_card
    CardEditorScope.BottomBar -> R.string.card_style_creator_scope_bottom_bar
    CardEditorScope.FloatingBottomBar -> R.string.card_style_creator_scope_floating_bar
}

private fun CardPreviewSurface.labelRes(): Int = when (this) {
    CardPreviewSurface.Home -> R.string.card_style_creator_preview_home
    CardPreviewSurface.Chrome -> R.string.card_style_creator_preview_chrome
}

private fun CardPreviewLayout.labelRes(): Int = when (this) {
    CardPreviewLayout.Xiaomi -> R.string.card_style_creator_layout_xiaomi
    CardPreviewLayout.Current -> R.string.card_style_creator_layout_current
}

private fun CustomCardTarget.toHomeLayoutCard(): HomeLayoutCard? = when (this) {
    CustomCardTarget.Lkm -> HomeLayoutCard.Lkm
    CustomCardTarget.Superuser -> HomeLayoutCard.Superuser
    CustomCardTarget.Module -> HomeLayoutCard.Module
    CustomCardTarget.StatusMonitor -> HomeLayoutCard.StatusMonitor
    CustomCardTarget.SystemInfo -> HomeLayoutCard.SystemInfo
    CustomCardTarget.Default,
    CustomCardTarget.RebootMenu -> null
}

private fun HomeLayoutCard.toCustomCardTarget(): CustomCardTarget = when (this) {
    HomeLayoutCard.Lkm -> CustomCardTarget.Lkm
    HomeLayoutCard.Superuser -> CustomCardTarget.Superuser
    HomeLayoutCard.Module -> CustomCardTarget.Module
    HomeLayoutCard.StatusMonitor -> CustomCardTarget.StatusMonitor
    HomeLayoutCard.SystemInfo -> CustomCardTarget.SystemInfo
}

private fun CustomCardTarget.labelRes(): Int = when (this) {
    CustomCardTarget.Default -> R.string.card_style_creator_target_default
    CustomCardTarget.Lkm -> R.string.card_style_creator_target_lkm
    CustomCardTarget.Superuser -> R.string.card_style_creator_target_superuser
    CustomCardTarget.Module -> R.string.card_style_creator_target_module
    CustomCardTarget.StatusMonitor -> R.string.card_style_creator_target_status
    CustomCardTarget.SystemInfo -> R.string.card_style_creator_target_system
    CustomCardTarget.RebootMenu -> R.string.card_style_creator_target_reboot
}

private fun CustomCardTarget.previewValueRes(): Int = when (this) {
    CustomCardTarget.Default -> R.string.card_style_creator_preview_default
    CustomCardTarget.Lkm -> R.string.card_style_creator_preview_lkm
    CustomCardTarget.Superuser -> R.string.card_style_creator_preview_superuser
    CustomCardTarget.Module -> R.string.card_style_creator_preview_module
    CustomCardTarget.StatusMonitor -> R.string.card_style_creator_preview_status_monitor
    CustomCardTarget.SystemInfo -> R.string.card_style_creator_preview_system
    CustomCardTarget.RebootMenu -> R.string.card_style_creator_preview_reboot
}

private fun CardPreviewInterface.labelRes(): Int = when (this) {
    CardPreviewInterface.Xiaomi -> R.string.card_style_creator_preview_interface_xiaomi
    CardPreviewInterface.Current -> R.string.card_style_creator_preview_interface_current
}

private fun CardPreviewOrientation.labelRes(): Int = when (this) {
    CardPreviewOrientation.Portrait -> R.string.card_style_creator_preview_portrait
    CardPreviewOrientation.Landscape -> R.string.card_style_creator_preview_landscape
}

private fun CardPreviewAppearance.labelRes(): Int = when (this) {
    CardPreviewAppearance.System -> R.string.card_style_creator_preview_system_theme
    CardPreviewAppearance.Light -> R.string.card_style_creator_preview_light
    CardPreviewAppearance.Dark -> R.string.card_style_creator_preview_dark
}

private fun PixelComponentPreset.labelRes(): Int = when (this) {
    PixelComponentPreset.CornerBrackets -> R.string.card_style_creator_asset_corners
    PixelComponentPreset.SteppedFrame -> R.string.card_style_creator_asset_frame
    PixelComponentPreset.DataLine -> R.string.card_style_creator_asset_data_line
    PixelComponentPreset.SnowCap -> R.string.card_style_creator_asset_snow
    PixelComponentPreset.WaterRipple -> R.string.card_style_creator_asset_water
    PixelComponentPreset.LeafVine -> R.string.card_style_creator_asset_leaf
}

private fun CardPixelLayer.labelRes(): Int = when (this) {
    CardPixelLayer.Top -> R.string.component_creator_layer_top
    CardPixelLayer.Border -> R.string.component_creator_layer_border
    CardPixelLayer.Interior -> R.string.component_creator_layer_interior
}

private fun NavigationPixelLayer.labelRes(): Int = when (this) {
    NavigationPixelLayer.Top -> R.string.component_creator_layer_top
    NavigationPixelLayer.Border -> R.string.component_creator_layer_border
}

private fun cardEditorLayerKey(
    scope: CardEditorScope,
    target: CustomCardTarget,
    cardLayer: CardPixelLayer,
    navigationLayer: NavigationPixelLayer,
): String = when (scope) {
    CardEditorScope.Card -> "${scope.name}:${target.name}:${cardLayer.name}"
    CardEditorScope.BottomBar,
    CardEditorScope.FloatingBottomBar -> "${scope.name}:${navigationLayer.name}"
}

private fun CustomCardStyle.withHiddenEditorLayers(hidden: Set<String>): CustomCardStyle {
    if (hidden.isEmpty()) return this
    var result = this
    CustomCardTarget.entries.forEach { target ->
        var layers = result.layersFor(target)
        CardPixelLayer.entries.forEach { layer ->
            val key = cardEditorLayerKey(CardEditorScope.Card, target, layer, NavigationPixelLayer.Top)
            if (key in hidden) layers = layers.withLayer(layer, layers.layer(layer).cleared())
        }
        result = result.withLayers(target, layers)
    }
    var bottomBar = result.bottomBar
    var floatingBottomBar = result.floatingBottomBar
    NavigationPixelLayer.entries.forEach { layer ->
        if (cardEditorLayerKey(CardEditorScope.BottomBar, CustomCardTarget.Default, CardPixelLayer.Top, layer) in hidden) {
            bottomBar = bottomBar.withLayer(layer, bottomBar.layer(layer).cleared())
        }
        if (cardEditorLayerKey(CardEditorScope.FloatingBottomBar, CustomCardTarget.Default, CardPixelLayer.Top, layer) in hidden) {
            floatingBottomBar = floatingBottomBar.withLayer(layer, floatingBottomBar.layer(layer).cleared())
        }
    }
    return result.copy(bottomBar = bottomBar, floatingBottomBar = floatingBottomBar)
}

private fun Throwable.componentEditorMessage(context: android.content.Context): String {
    return message?.lineSequence()?.firstOrNull()?.take(180)
        ?: context.getString(R.string.component_creator_unknown_error)
}

private const val MAX_PIXEL_HISTORY = 80
private const val CARD_DRAFT_AUTOSAVE_DELAY_MS = 700L
