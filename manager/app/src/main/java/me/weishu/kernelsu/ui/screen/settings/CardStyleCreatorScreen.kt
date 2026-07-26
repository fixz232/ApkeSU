package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileOpen
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
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
import me.weishu.kernelsu.ui.component.custom.PixelEditToolbar
import me.weishu.kernelsu.ui.component.custom.PixelEditorSection
import me.weishu.kernelsu.ui.component.custom.PixelGrid
import me.weishu.kernelsu.ui.component.custom.PixelGridEditor
import me.weishu.kernelsu.ui.component.custom.PixelMotionEditor
import me.weishu.kernelsu.ui.component.custom.PixelPaletteEditor
import me.weishu.kernelsu.ui.component.custom.TRANSPARENT_PIXEL
import me.weishu.kernelsu.ui.component.custom.drawCustomCardChrome
import me.weishu.kernelsu.ui.component.custom.drawCustomCardInterior
import me.weishu.kernelsu.ui.component.custom.drawCustomNavigationStyle
import me.weishu.kernelsu.ui.component.custom.filledWhere
import me.weishu.kernelsu.ui.component.custom.hasSameDimensionsAs
import me.weishu.kernelsu.ui.component.custom.mirroredHorizontally
import me.weishu.kernelsu.ui.component.custom.rememberComponentMotionProgress
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
    val nameRequired = stringResource(R.string.component_creator_name_required)
    val saveFailed = stringResource(R.string.component_creator_save_failed)
    val deleteFailed = stringResource(R.string.component_creator_delete_failed)
    val cloudDescription = stringResource(R.string.card_style_creator_cloud_description)
    val cloudCategory = stringResource(R.string.card_style_creator_cloud_category)

    var styles by remember { mutableStateOf(store.readCardStyles()) }
    val initialStyle = remember {
        store.readActiveCardStyle() ?: styles.firstOrNull() ?: starterCardStyle(defaultName)
    }
    val cardSaver = remember {
        Saver<CustomCardStyle, String>(
            save = { it.toJsonString() },
            restore = { raw -> runCatching { CustomCardStyle.fromJsonString(raw) }.getOrNull() },
        )
    }
    var draft by rememberSaveable(stateSaver = cardSaver) { mutableStateOf(initialStyle) }
    var baseline by remember { mutableStateOf(initialStyle) }
    var selectedPage by rememberSaveable { mutableIntStateOf(CardCreatorPage.Design.ordinal) }
    var editorScope by rememberSaveable { mutableIntStateOf(CardEditorScope.Card.ordinal) }
    var selectedTarget by rememberSaveable { mutableIntStateOf(CustomCardTarget.Default.ordinal) }
    var selectedCardLayer by rememberSaveable { mutableIntStateOf(CardPixelLayer.Top.ordinal) }
    var selectedNavigationLayer by rememberSaveable { mutableIntStateOf(NavigationPixelLayer.Top.ordinal) }
    var selectedColor by rememberSaveable { mutableLongStateOf(DEFAULT_PIXEL_PALETTE[3]) }
    var undoStack by remember { mutableStateOf<List<PixelGrid>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<PixelGrid>>(emptyList()) }
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
    val editorKey = "${currentScope.name}:${currentTarget.name}:${currentCardLayer.name}:${currentNavigationLayer.name}"

    fun currentGrid(): PixelGrid = when (currentScope) {
        CardEditorScope.Card -> draft.layersFor(currentTarget).layer(currentCardLayer)
        CardEditorScope.BottomBar -> draft.bottomBar.layer(currentNavigationLayer)
        CardEditorScope.FloatingBottomBar -> draft.floatingBottomBar.layer(currentNavigationLayer)
    }

    fun updateGrid(grid: PixelGrid) {
        if (!grid.hasSameDimensionsAs(currentGrid())) {
            undoStack = emptyList()
            redoStack = emptyList()
            return
        }
        draft = when (currentScope) {
            CardEditorScope.Card -> draft.withLayers(
                currentTarget,
                draft.layersFor(currentTarget).withLayer(currentCardLayer, grid),
            )
            CardEditorScope.BottomBar -> draft.copy(
                bottomBar = draft.bottomBar.withLayer(currentNavigationLayer, grid),
            )
            CardEditorScope.FloatingBottomBar -> draft.copy(
                floatingBottomBar = draft.floatingBottomBar.withLayer(currentNavigationLayer, grid),
            )
        }
    }

    fun pushUndo(snapshot: PixelGrid) {
        undoStack = (undoStack + snapshot).takeLast(MAX_PIXEL_HISTORY)
        redoStack = emptyList()
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

    LaunchedEffect(editorKey) {
        undoStack = emptyList()
        redoStack = emptyList()
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
                    val imported = readComponentStylePackage(context, uri, ComponentStyleKind.Card)
                        .cardStyle ?: error("Card style is missing")
                    require(store.saveCardStyle(imported, apply = false)) { "Unable to save imported style" }
                    imported
                }
            }.onSuccess { imported ->
                styles = store.readCardStyles()
                draft = styles.firstOrNull { it.id == imported.id } ?: imported
                baseline = draft
                selectedPage = CardCreatorPage.Design.ordinal
                showMessage(importSuccess)
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
    val onBack = dropUnlessResumed { navigator.pop() }
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.close))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val fresh = starterCardStyle(defaultName)
                            draft = fresh
                            baseline = fresh
                            selectedPage = CardCreatorPage.Design.ordinal
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
                    onDraftChange = { draft = it },
                    onScopeSelected = { editorScope = it.ordinal },
                    onTargetSelected = {
                        editorScope = CardEditorScope.Card.ordinal
                        selectedTarget = it.ordinal
                    },
                    onCardLayerSelected = { selectedCardLayer = it.ordinal },
                    onNavigationLayerSelected = { selectedNavigationLayer = it.ordinal },
                    onColorSelected = { selectedColor = it },
                    onPaletteChange = { draft = draft.copy(palette = it).normalized() },
                    onStrokeStart = ::pushUndo,
                    onGridChange = ::updateGrid,
                    onUndo = {
                        val current = currentGrid()
                        val previous = undoStack.lastOrNull()
                            ?.takeIf { it.hasSameDimensionsAs(current) }
                            ?: run {
                                undoStack = emptyList()
                                redoStack = emptyList()
                                return@CardDesignPage
                            }
                        redoStack = (redoStack + current).takeLast(MAX_PIXEL_HISTORY)
                        undoStack = undoStack.dropLast(1)
                        updateGrid(previous)
                    },
                    onRedo = {
                        val current = currentGrid()
                        val next = redoStack.lastOrNull()
                            ?.takeIf { it.hasSameDimensionsAs(current) }
                            ?: run {
                                undoStack = emptyList()
                                redoStack = emptyList()
                                return@CardDesignPage
                            }
                        undoStack = (undoStack + current).takeLast(MAX_PIXEL_HISTORY)
                        redoStack = redoStack.dropLast(1)
                        updateGrid(next)
                    },
                    onFill = {
                        pushUndo(currentGrid())
                        updateGrid(
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
                        )
                    },
                    onMirror = {
                        pushUndo(currentGrid())
                        updateGrid(currentGrid().mirroredHorizontally())
                    },
                    onClear = {
                        pushUndo(currentGrid())
                        updateGrid(currentGrid().cleared())
                    },
                    onOpenHomeLayout = { navigator.push(Route.HomeLayout) },
                )
                CardCreatorPage.Motion -> CardMotionPage(
                    draft = draft,
                    homeUiState = homeUiState,
                    mappedHomeLayout = mappedHomeLayout,
                    selectedTarget = currentTarget,
                    onTargetSelected = { selectedTarget = it.ordinal },
                    onDraftChange = { draft = it },
                    onOpenHomeLayout = { navigator.push(Route.HomeLayout) },
                )
                CardCreatorPage.Library -> CardStyleLibraryPage(
                    styles = styles,
                    activeId = store.readActiveCardStyle()?.id,
                    onLoad = { style ->
                        draft = style
                        baseline = style
                        selectedPage = CardCreatorPage.Design.ordinal
                    },
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
    onDraftChange: (CustomCardStyle) -> Unit,
    onScopeSelected: (CardEditorScope) -> Unit,
    onTargetSelected: (CustomCardTarget) -> Unit,
    onCardLayerSelected: (CardPixelLayer) -> Unit,
    onNavigationLayerSelected: (NavigationPixelLayer) -> Unit,
    onColorSelected: (Long) -> Unit,
    onPaletteChange: (List<Long>) -> Unit,
    onStrokeStart: (PixelGrid) -> Unit,
    onGridChange: (PixelGrid) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFill: () -> Unit,
    onMirror: () -> Unit,
    onClear: () -> Unit,
    onOpenHomeLayout: () -> Unit,
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
            PixelEditorSection(stringResource(R.string.card_style_creator_xiaomi_preview)) {
                XiaomiCardStylePreview(
                    style = draft,
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
        item {
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
                                onDraftChange(
                                    draft.copy(
                                        cardOverrides = draft.cardOverrides - selectedTarget,
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.card_style_creator_reset_override))
                        }
                    }
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
                    isCellEditable = { x, y, width, height ->
                        isCardCreatorCellEditable(
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
                        if (currentGrid.height <= CARD_TOP_GRID_HEIGHT) 160.dp else 240.dp
                    ),
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
private fun CardMotionPage(
    draft: CustomCardStyle,
    homeUiState: HomeUiState,
    mappedHomeLayout: HomeLayoutState,
    selectedTarget: CustomCardTarget,
    onTargetSelected: (CustomCardTarget) -> Unit,
    onDraftChange: (CustomCardStyle) -> Unit,
    onOpenHomeLayout: () -> Unit,
) {
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
            )
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
private fun XiaomiCardStylePreview(
    style: CustomCardStyle,
    homeUiState: HomeUiState,
    mappedHomeLayout: HomeLayoutState,
    selectedTarget: CustomCardTarget,
    selectedScope: CardEditorScope,
    onTargetSelected: (CustomCardTarget) -> Unit,
    onScopeSelected: (CardEditorScope) -> Unit,
    onOpenHomeLayout: () -> Unit,
) {
    var previewSurfaceIndex by rememberSaveable(style.id) {
        mutableIntStateOf(CardPreviewSurface.Home.ordinal)
    }
    var previewLayoutIndex by rememberSaveable(style.id) {
        mutableIntStateOf(CardPreviewLayout.Xiaomi.ordinal)
    }
    val previewSurface = CardPreviewSurface.entries.getOrElse(previewSurfaceIndex) {
        CardPreviewSurface.Home
    }
    val previewLayout = CardPreviewLayout.entries.getOrElse(previewLayoutIndex) {
        CardPreviewLayout.Xiaomi
    }
    val motionProgress = rememberComponentMotionProgress(
        rule = style.motion,
        enabled = true,
        label = "cardStyleCreatorPreview",
    )
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
            }
            ChoiceChipRow(
                options = CardPreviewSurface.entries,
                selected = previewSurface,
                label = { stringResource(it.labelRes()) },
                onSelected = { previewSurfaceIndex = it.ordinal },
            )
            when (previewSurface) {
                CardPreviewSurface.Home -> {
                    ChoiceChipRow(
                        options = CardPreviewLayout.entries,
                        selected = previewLayout,
                        label = { stringResource(it.labelRes()) },
                        onSelected = { previewLayoutIndex = it.ordinal },
                    )
                    HomeMappedCardStylePreview(
                        style = style,
                        homeUiState = homeUiState,
                        layout = when (previewLayout) {
                            CardPreviewLayout.Xiaomi -> HomeLayoutState(enabled = true)
                            CardPreviewLayout.Current -> mappedHomeLayout.copy(enabled = true)
                        },
                        selectedTarget = selectedTarget.takeIf { selectedScope == CardEditorScope.Card },
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
                        style = style,
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
                        style = style,
                        floating = false,
                        selected = selectedScope == CardEditorScope.BottomBar,
                        motionProgress = motionProgress,
                        onClick = { onScopeSelected(CardEditorScope.BottomBar) },
                    )
                    NavigationStylePreview(
                        style = style,
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

@Composable
private fun HomeMappedCardStylePreview(
    style: CustomCardStyle,
    homeUiState: HomeUiState,
    layout: HomeLayoutState,
    selectedTarget: CustomCardTarget?,
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
            LocalInterfaceStyle provides InterfaceStyle.Miuix.value,
            LocalUiDecorationConfig provides previewDecoration,
            LocalUiDecorationScope provides UiDecorationScope.Home,
            LocalCustomCardStyle provides style,
        ) {
            HomeLayoutCanvas(
                state = layout,
                modifier = Modifier.fillMaxWidth(),
                selectedCard = selectedTarget?.toHomeLayoutCard(),
                onCardSelected = { card -> onTargetSelected(card.toCustomCardTarget()) },
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

private fun CardPixelLayer.labelRes(): Int = when (this) {
    CardPixelLayer.Top -> R.string.component_creator_layer_top
    CardPixelLayer.Border -> R.string.component_creator_layer_border
    CardPixelLayer.Interior -> R.string.component_creator_layer_interior
}

private fun NavigationPixelLayer.labelRes(): Int = when (this) {
    NavigationPixelLayer.Top -> R.string.component_creator_layer_top
    NavigationPixelLayer.Border -> R.string.component_creator_layer_border
}

private fun Throwable.componentEditorMessage(context: android.content.Context): String {
    return message?.lineSequence()?.firstOrNull()?.take(180)
        ?: context.getString(R.string.component_creator_unknown_error)
}

private const val MAX_PIXEL_HISTORY = 40
