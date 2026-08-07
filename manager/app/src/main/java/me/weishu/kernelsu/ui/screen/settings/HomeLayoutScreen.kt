package me.weishu.kernelsu.ui.screen.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Portrait
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.CustomWallpaperRoot
import me.weishu.kernelsu.ui.component.HomeLayoutCanvas
import me.weishu.kernelsu.ui.component.HomeLayoutEditor
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.NightBackgroundEffectOverlay
import me.weishu.kernelsu.ui.component.decoration.UiDecorationBackdrop
import me.weishu.kernelsu.ui.component.material.TonalCard
import me.weishu.kernelsu.ui.component.pixel.PixelBackdrop
import me.weishu.kernelsu.ui.component.rain.RainBackdrop
import me.weishu.kernelsu.ui.component.snow.SeasonAmbientOverlay
import me.weishu.kernelsu.ui.component.snow.SeasonStyleWallpaper
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.screen.home.HomeActions
import me.weishu.kernelsu.ui.screen.home.HomeLayoutCardContent
import me.weishu.kernelsu.ui.screen.home.HomePagerMiuix
import me.weishu.kernelsu.ui.screen.home.HomeUiState
import me.weishu.kernelsu.ui.util.HomeLayoutCard
import me.weishu.kernelsu.ui.util.HomeLayoutItem
import me.weishu.kernelsu.ui.util.HomeLayoutPreset
import me.weishu.kernelsu.ui.util.HomeLayoutSticker
import me.weishu.kernelsu.ui.util.HomeLayoutState
import me.weishu.kernelsu.ui.util.HomeLayoutWallpaperFit
import me.weishu.kernelsu.ui.util.CustomBackgroundState
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.autoArrangeHomeLayoutItems
import me.weishu.kernelsu.ui.util.decodeHomeLayoutState
import me.weishu.kernelsu.ui.util.encodeHomeLayoutState
import me.weishu.kernelsu.ui.util.homeLayoutItemsForPreset
import me.weishu.kernelsu.ui.util.itemsForOrientation
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap
import me.weishu.kernelsu.ui.util.minimumHomeLayoutHeight
import me.weishu.kernelsu.ui.util.moveHomeLayoutItem
import me.weishu.kernelsu.ui.util.moveHomeLayoutCardLayer
import me.weishu.kernelsu.ui.util.readHomeLayoutState
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import me.weishu.kernelsu.ui.util.resolveHomeLayoutCollisions
import me.weishu.kernelsu.ui.util.resizeHomeLayoutItem
import me.weishu.kernelsu.ui.util.sanitizeHomeLayoutItem
import me.weishu.kernelsu.ui.util.saveHomeLayoutState
import me.weishu.kernelsu.ui.util.snapHomeLayoutItem
import me.weishu.kernelsu.ui.util.suggestedHomeLayoutHeight
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.withItemsForOrientation
import me.weishu.kernelsu.ui.viewmodel.HomeViewModel
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import me.weishu.kernelsu.ui.theme.LocalImmersiveBackgroundActive
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import kotlin.math.roundToInt
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val HOME_LAYOUT_STICKER_MAX_BYTES = 32L * 1024L * 1024L
private const val HOME_LAYOUT_STICKER_VALIDATION_SIDE = 512
private const val HOME_LAYOUT_TRANSFER_MAX_BYTES = 256 * 1024

private val HomeLayoutMutableStateSaver = Saver<MutableState<HomeLayoutState>, String>(
    save = { state -> encodeHomeLayoutState(state.value) },
    restore = { encoded -> mutableStateOf(decodeHomeLayoutState(encoded) ?: HomeLayoutState()) },
)

@Composable
fun HomeLayoutScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val homeViewModel = viewModel<HomeViewModel>()
    val settingsViewModel = viewModel<SettingsViewModel>()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val popBack = dropUnlessResumed { navigator.pop() }
    val initialState = remember(context) { readHomeLayoutState(context) }
    var state by rememberSaveable(saver = HomeLayoutMutableStateSaver) {
        mutableStateOf(initialState)
    }
    var savedState by rememberSaveable(saver = HomeLayoutMutableStateSaver) {
        mutableStateOf(initialState)
    }
    var selectedCard by rememberSaveable { mutableStateOf(HomeLayoutCard.Lkm) }
    var editingLandscape by rememberSaveable { mutableStateOf(false) }
    var selectedStickerId by remember { mutableStateOf<String?>(null) }
    var pendingStickerUris by remember { mutableStateOf(emptySet<String>()) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var fullscreenEditor by remember { mutableStateOf(false) }
    var showPreciseControls by remember { mutableStateOf(false) }
    var showLayerPanel by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var overlappingCards by remember { mutableStateOf(emptySet<HomeLayoutCard>()) }
    var undoStack by remember { mutableStateOf(emptyList<HomeLayoutState>()) }
    var redoStack by remember { mutableStateOf(emptyList<HomeLayoutState>()) }
    var interactionStartState by remember { mutableStateOf<HomeLayoutState?>(null) }
    var fullscreenControlsHeightPx by remember { mutableStateOf(112) }
    val saveSuccess = stringResource(R.string.home_layout_save_success)
    val saveFailed = stringResource(R.string.home_layout_save_failed)
    val resetSuccess = stringResource(R.string.home_layout_reset_success)
    val importSuccess = stringResource(R.string.home_layout_import_success)
    val importFailed = stringResource(R.string.home_layout_import_failed)
    val exportSuccess = stringResource(R.string.home_layout_export_success)
    val exportFailed = stringResource(R.string.home_layout_export_failed)
    val dirty = state != savedState

    fun pushUndo(previous: HomeLayoutState) {
        undoStack = (undoStack + previous).takeLast(HOME_LAYOUT_HISTORY_LIMIT)
        redoStack = emptyList()
    }

    fun applyState(next: HomeLayoutState) {
        if (next == state) return
        pushUndo(state)
        state = next
        message = null
    }

    fun activeItems(source: HomeLayoutState = state): List<HomeLayoutItem> {
        return source.itemsForOrientation(editingLandscape)
    }

    fun replaceActiveItems(
        source: HomeLayoutState = state,
        items: List<HomeLayoutItem>,
    ): HomeLayoutState {
        return source.withItemsForOrientation(editingLandscape, items)
    }

    fun updateItem(card: HomeLayoutCard, transform: (HomeLayoutItem) -> HomeLayoutItem) {
        state = replaceActiveItems(
            items = activeItems().map { item ->
                if (item.card == card) sanitizeHomeLayoutItem(transform(item)) else item
            },
        )
    }

    fun beginInteraction() {
        if (interactionStartState == null) interactionStartState = state
    }

    fun finishInteraction(card: HomeLayoutCard) {
        val currentItems = activeItems()
        val currentItem = currentItems.firstOrNull { it.card == card }
        if (currentItem != null) {
            val snapped = if (state.autoSnap) {
                snapHomeLayoutItem(currentItem, currentItems)
            } else {
                currentItem
            }
            var nextItems = currentItems.map { item ->
                if (item.card == card) snapped else item
            }
            if (state.autoAvoidOverlap) {
                nextItems = resolveHomeLayoutCollisions(nextItems)
            }
            state = replaceActiveItems(items = nextItems)
            if (snapped.x != currentItem.x || snapped.y != currentItem.y) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
        interactionStartState?.let { start ->
            if (start != state) pushUndo(start)
        }
        interactionStartState = null
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        undoStack = undoStack.dropLast(1)
        redoStack = (redoStack + state).takeLast(HOME_LAYOUT_HISTORY_LIMIT)
        state = previous
        interactionStartState = null
        message = null
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        redoStack = redoStack.dropLast(1)
        undoStack = (undoStack + state).takeLast(HOME_LAYOUT_HISTORY_LIMIT)
        state = next
        interactionStartState = null
        message = null
    }

    fun requestBack() {
        if (dirty) showDiscardDialog = true else popBack()
    }

    fun stickerUris(source: HomeLayoutState): Set<String> {
        return (source.items + source.landscapeItems)
            .flatMap { it.stickers }
            .map { it.uriString }
            .filter(String::isNotBlank)
            .toSet()
    }

    fun releaseUris(uris: Set<String>) {
        uris.forEach { uriString -> releaseCustomImageReference(context, uriString) }
    }

    fun save(
        next: HomeLayoutState = state,
        successText: String = saveSuccess,
        onSuccess: () -> Unit = {},
    ) {
        if (saving) return
        saving = true
        val previouslySavedStickerUris = stickerUris(savedState)
        scope.launch {
            val ok = withContext(Dispatchers.IO) { saveHomeLayoutState(context, next) }
            if (ok) {
                val persisted = readHomeLayoutState(context)
                val persistedStickerUris = stickerUris(persisted)
                withContext(Dispatchers.IO) {
                    releaseUris(previouslySavedStickerUris - persistedStickerUris)
                    releaseUris(pendingStickerUris - persistedStickerUris)
                }
                pendingStickerUris = emptySet()
                state = persisted
                savedState = persisted
                undoStack = emptyList()
                redoStack = emptyList()
                interactionStartState = null
                message = successText
                onSuccess()
            } else {
                message = saveFailed
            }
            saving = false
        }
    }

    val layoutExportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val exportedState = state
        scope.launch {
            val exported = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(encodeHomeLayoutState(exportedState).toByteArray(Charsets.UTF_8))
                    } != null
                }.getOrDefault(false)
            }
            message = if (exported) exportSuccess else exportFailed
        }
    }

    val layoutImportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use(::readHomeLayoutTransfer)
                        ?.let(::decodeHomeLayoutState)
                }.getOrNull()
            }
            if (imported == null) {
                message = importFailed
            } else {
                applyState(imported)
                selectedCard = HomeLayoutCard.Lkm
                selectedStickerId = null
                overlappingCards = emptySet()
                message = importSuccess
            }
        }
    }

    val stickerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val targetCard = selectedCard
        scope.launch {
            val storageKey = "home_layout_sticker_${targetCard.value}_${UUID.randomUUID()}"
            val persistedUri = withContext(Dispatchers.IO) {
                persistCustomImageReference(
                    context = context,
                    sourceUri = uri,
                    storageKey = storageKey,
                    maxBytes = HOME_LAYOUT_STICKER_MAX_BYTES,
                )
            }
            if (persistedUri == null) {
                Toast.makeText(context, R.string.home_layout_sticker_import_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val validImage = withContext(Dispatchers.IO) {
                loadCustomImageBitmap(
                    context = context,
                    uriString = persistedUri,
                    maxSide = HOME_LAYOUT_STICKER_VALIDATION_SIDE,
                )?.let { bitmap ->
                    if (!bitmap.isRecycled) bitmap.recycle()
                    true
                } ?: false
            }
            if (!validImage) {
                withContext(Dispatchers.IO) {
                    releaseCustomImageReference(context, persistedUri)
                }
                Toast.makeText(context, R.string.home_layout_sticker_invalid, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val sticker = HomeLayoutSticker(
                id = UUID.randomUUID().toString(),
                uriString = persistedUri,
            )
            pendingStickerUris = pendingStickerUris + persistedUri
            selectedStickerId = sticker.id
            applyState(
                replaceActiveItems(
                    items = activeItems().map { item ->
                        if (item.card == targetCard) {
                            sanitizeHomeLayoutItem(item.copy(stickers = item.stickers + sticker))
                        } else {
                            item
                        }
                    },
                ),
            )
        }
    }

    val currentPendingStickerUris by rememberUpdatedState(pendingStickerUris)
    DisposableEffect(Unit) {
        onDispose {
            releaseUris(currentPendingStickerUris)
        }
    }

    BackHandler(enabled = dirty && !fullscreenEditor) { showDiscardDialog = true }

    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.home_layout_title),
                color = Color.Transparent,
                titleColor = MiuixTheme.colorScheme.onSurface,
                navigationIcon = {
                    MiuixIconButton(onClick = ::requestBack) {
                        MiuixIcon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MiuixTheme.colorScheme.onBackground,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DashboardCustomize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_layout_enable_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.home_layout_enable_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = { checked -> applyState(state.copy(enabled = checked)) },
                    )
                }
            }

            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message ?: stringResource(
                        when {
                            saving -> R.string.home_layout_saving
                            dirty -> R.string.home_layout_unsaved
                            else -> R.string.home_layout_saved
                        },
                    ),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (dirty && message == null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = !saving,
                    onClick = { layoutImportPicker.launch(arrayOf("application/json", "text/json")) },
                ) {
                    Text(stringResource(R.string.home_layout_import))
                }
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = !saving,
                    onClick = {
                        layoutExportPicker.launch("apkesu-home-layout.json")
                    },
                ) {
                    Text(stringResource(R.string.home_layout_export))
                }
            }

            HomeLayoutOrientationControls(
                editingLandscape = editingLandscape,
                onEditingLandscapeChange = { landscape ->
                    if (editingLandscape != landscape) {
                        editingLandscape = landscape
                        selectedStickerId = null
                        overlappingCards = emptySet()
                        interactionStartState = null
                    }
                },
            )

            HomeLayoutPresetControls(
                state = state,
                isLandscape = editingLandscape,
                onPresetSelected = { preset ->
                    applyState(
                        replaceActiveItems(
                            items = homeLayoutItemsForPreset(
                                preset = preset,
                                isLandscape = editingLandscape,
                            ),
                        ),
                    )
                },
                onAutoSnapChange = { enabled ->
                    applyState(state.copy(autoSnap = enabled))
                },
                onAutoAvoidChange = { enabled ->
                    applyState(state.copy(autoAvoidOverlap = enabled))
                },
                onAutoArrange = {
                    applyState(replaceActiveItems(items = autoArrangeHomeLayoutItems(activeItems())))
                },
            )

            HomeLayoutPreview(
                state = state,
                homeUiState = homeUiState,
                isLandscape = editingLandscape,
                onOpenFullscreen = { fullscreenEditor = true },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = { showPreciseControls = !showPreciseControls },
                ) {
                    Icon(
                        imageVector = if (showPreciseControls) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_layout_precise_controls))
                }
                IconButton(onClick = ::undo, enabled = undoStack.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = stringResource(R.string.home_layout_undo),
                    )
                }
                IconButton(onClick = ::redo, enabled = redoStack.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Redo,
                        contentDescription = stringResource(R.string.home_layout_redo),
                    )
                }
            }

            AnimatedVisibility(visible = showPreciseControls) {
                HomeLayoutInspector(
                    item = activeItems().first { it.card == selectedCard },
                    selectedStickerId = selectedStickerId,
                    onCardSelected = {
                        selectedCard = it
                        selectedStickerId = null
                    },
                    onItemChange = { item -> updateItem(item.card) { item } },
                    onCommittedItemChange = { item ->
                        applyState(
                            replaceActiveItems(
                                items = activeItems().map { current ->
                                    if (current.card == item.card) sanitizeHomeLayoutItem(item) else current
                                },
                            ),
                        )
                    },
                    onInteractionStart = ::beginInteraction,
                    onInteractionEnd = { finishInteraction(selectedCard) },
                    onAlign = { x ->
                        applyState(
                            replaceActiveItems(
                                items = activeItems().map { item ->
                                    if (item.card == selectedCard) item.copy(x = x) else item
                                },
                            ),
                        )
                    },
                    onNudge = { horizontal, vertical ->
                        applyState(
                            replaceActiveItems(
                                items = activeItems().map { item ->
                                    if (item.card == selectedCard) {
                                        sanitizeHomeLayoutItem(
                                            item.copy(
                                                x = if (item.width >= 0.999f) 0f else item.x + horizontal,
                                                y = item.y + vertical,
                                            ),
                                        )
                                    } else {
                                        item
                                    }
                                },
                            ),
                        )
                    },
                    onAddSticker = { stickerPicker.launch(arrayOf("image/*")) },
                    onStickerSelected = { selectedStickerId = it },
                    onStickerChange = { sticker ->
                        updateItem(selectedCard) { item ->
                            item.copy(
                                stickers = item.stickers.map { current ->
                                    if (current.id == sticker.id) sticker else current
                                },
                            )
                        }
                    },
                    onStickerDelete = { sticker ->
                        applyState(
                            replaceActiveItems(
                                items = activeItems().map { item ->
                                    if (item.card == selectedCard) {
                                        item.copy(stickers = item.stickers.filterNot { it.id == sticker.id })
                                    } else {
                                        item
                                    }
                                },
                            ),
                        )
                        selectedStickerId = null
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = !saving,
                    onClick = {
                        applyState(HomeLayoutState(enabled = state.enabled))
                        message = resetSuccess
                    },
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_layout_restore_default))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !saving && dirty,
                    onClick = { save() },
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_layout_save))
                }
            }
        }
    }

    if (fullscreenEditor) {
        Dialog(
            onDismissRequest = { fullscreenEditor = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            HomeLayoutFullscreenBackground(settingsUiState) {
                Box(modifier = Modifier.fillMaxSize()) {
                    HomePagerMiuix(
                            state = homeUiState,
                            actions = rememberHomeLayoutPreviewActions(),
                            bottomInnerPadding = with(density) {
                                fullscreenControlsHeightPx.toDp()
                            } + 12.dp,
                            installFeedbackActive = false,
                            homeLayoutOverride = state.copy(enabled = true),
                            homeLayoutLandscapeOverride = editingLandscape,
                            homeLayoutEditor = HomeLayoutEditor(
                                selectedCard = selectedCard,
                                onSelectedCardChange = { selectedCard = it },
                                onDragCard = { card, delta ->
                                    updateItem(card) { item ->
                                        moveHomeLayoutItem(
                                            item = item,
                                            horizontalDelta = delta.x,
                                            verticalDeltaRows = delta.y,
                                        )
                                    }
                                },
                                onResizeCard = { card, gesture ->
                                    updateItem(card) { item ->
                                        resizeHomeLayoutItem(
                                            item = item,
                                            edge = gesture.edge,
                                            horizontalDelta = gesture.delta.x,
                                            verticalDeltaRows = gesture.delta.y,
                                            renderedHeightRows = gesture.renderedHeightRows,
                                        )
                                    }
                                },
                                onTransformStart = { beginInteraction() },
                                onTransformEnd = ::finishInteraction,
                                onOverlapChange = { overlappingCards = it },
                            ),
                        )
                    HomeLayoutFullscreenControls(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    fullscreenControlsHeightPx = coordinates.size.height
                                },
                            state = state,
                            isLandscape = editingLandscape,
                            selectedCard = selectedCard,
                            showLayers = showLayerPanel,
                            canUndo = undoStack.isNotEmpty(),
                            canRedo = redoStack.isNotEmpty(),
                            overlappingCards = overlappingCards,
                            saving = saving,
                            dirty = dirty,
                            onSelectedCardChange = {
                                selectedCard = it
                                selectedStickerId = null
                            },
                            onOrientationChange = { landscape ->
                                editingLandscape = landscape
                                selectedStickerId = null
                                overlappingCards = emptySet()
                            },
                            onToggleLayers = { showLayerPanel = !showLayerPanel },
                            onUndo = ::undo,
                            onRedo = ::redo,
                            onVisibilityChange = { card, visible ->
                                val item = activeItems().first { it.card == card }
                                applyState(
                                    replaceActiveItems(
                                        items = activeItems().map { current ->
                                            if (current.card == card) item.copy(visible = visible) else current
                                        },
                                    ),
                                )
                            },
                            onMoveLayer = { card, direction ->
                                applyState(
                                    replaceActiveItems(
                                        items = moveHomeLayoutCardLayer(activeItems(), card, direction),
                                    ),
                                )
                            },
                            onAlign = { x ->
                                applyState(
                                    replaceActiveItems(
                                        items = activeItems().map { item ->
                                            if (item.card == selectedCard) item.copy(x = x) else item
                                        },
                                    ),
                                )
                            },
                            onNudge = { horizontal, vertical ->
                                applyState(
                                    replaceActiveItems(
                                        items = activeItems().map { item ->
                                            if (item.card == selectedCard) {
                                                sanitizeHomeLayoutItem(
                                                    item.copy(
                                                        x = if (item.width >= 0.999f) 0f else item.x + horizontal,
                                                        y = item.y + vertical,
                                                    ),
                                                )
                                            } else {
                                                item
                                            }
                                        },
                                    ),
                                )
                            },
                            onAutoAvoidChange = { enabled ->
                                applyState(state.copy(autoAvoidOverlap = enabled))
                            },
                            onAutoSnapChange = { enabled ->
                                applyState(state.copy(autoSnap = enabled))
                            },
                            onClose = { fullscreenEditor = false },
                            onSave = { save(onSuccess = { fullscreenEditor = false }) },
                        )
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.home_layout_discard_title)) },
            text = { Text(stringResource(R.string.home_layout_discard_summary)) },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.home_layout_keep_editing))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        releaseUris(pendingStickerUris)
                        pendingStickerUris = emptySet()
                        popBack()
                    },
                ) {
                    Text(stringResource(R.string.home_layout_discard_action))
                }
            },
        )
    }
}

@Composable
private fun HomeLayoutFullscreenBackground(
    settings: SettingsUiState,
    content: @Composable BoxScope.() -> Unit,
) {
    val homeBackground = settings.customPageBackgrounds[CustomPageBackgroundTarget.Home]
        .takeIf { it.hasMedia }
        ?: CustomBackgroundState(
            wallpaperUriString = settings.customWallpaperUri,
            videoUriString = settings.customVideoBackgroundUri,
            opacity = settings.customWallpaperOpacity,
            crop = settings.customWallpaperCrop,
            videoDurationSeconds = settings.customVideoBackgroundDurationSeconds,
            videoFrameRate = settings.customVideoBackgroundFrameRate,
            visualSettings = settings.customWallpaperVisualSettings,
        )
    val interfaceStyle = LocalInterfaceStyle.current
    val seasonalStyle = interfaceStyle == InterfaceStyle.Snow.value
    val rainStyle = interfaceStyle == InterfaceStyle.Rain.value
    val pixelStyle = interfaceStyle == InterfaceStyle.Pixel.value
    val darkMode = isInDarkTheme()
    val immersiveBackground = seasonalStyle || rainStyle || pixelStyle || homeBackground.hasMedia ||
        (darkMode && NightBackgroundEffect.fromValue(settings.nightBackgroundEffect) != NightBackgroundEffect.Off)

    CustomWallpaperRoot(
        uriString = homeBackground.wallpaperUriString,
        videoUriString = homeBackground.videoUriString,
        videoDurationSeconds = homeBackground.videoDurationSeconds,
        videoFrameRate = homeBackground.videoFrameRate,
        opacity = homeBackground.opacity,
        crop = homeBackground.crop,
        visualSettings = homeBackground.visualSettings,
        passthroughEnabled = settings.customWallpaperPassthroughEnabled,
        passthroughOpacity = settings.customWallpaperPassthroughOpacity,
    ) {
        CompositionLocalProvider(LocalImmersiveBackgroundActive provides immersiveBackground) {
            if (seasonalStyle && !homeBackground.hasMedia) {
                SeasonStyleWallpaper(modifier = Modifier.fillMaxSize())
                SeasonAmbientOverlay(modifier = Modifier.fillMaxSize())
            }
            if (pixelStyle && !homeBackground.hasMedia) {
                PixelBackdrop(modifier = Modifier.fillMaxSize())
            }
            if (rainStyle && !homeBackground.hasMedia) {
                RainBackdrop(modifier = Modifier.fillMaxSize())
            }
            if (!settings.nightBackgroundPassthrough) {
                NightBackgroundEffectOverlay(
                    enabled = darkMode,
                    effectValue = settings.nightBackgroundEffect,
                    passthrough = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            UiDecorationBackdrop(modifier = Modifier.fillMaxSize())
            content()
        }
    }
}

@Composable
private fun HomeLayoutOrientationControls(
    editingLandscape: Boolean,
    onEditingLandscapeChange: (Boolean) -> Unit,
) {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                modifier = Modifier.weight(1f),
                selected = !editingLandscape,
                onClick = { onEditingLandscapeChange(false) },
                leadingIcon = { Icon(Icons.Rounded.Portrait, contentDescription = null) },
                label = { Text(stringResource(R.string.home_layout_portrait)) },
            )
            FilterChip(
                modifier = Modifier.weight(1f),
                selected = editingLandscape,
                onClick = { onEditingLandscapeChange(true) },
                leadingIcon = { Icon(Icons.Rounded.Landscape, contentDescription = null) },
                label = { Text(stringResource(R.string.home_layout_landscape)) },
            )
        }
    }
}

@Composable
private fun HomeLayoutPresetControls(
    state: HomeLayoutState,
    isLandscape: Boolean,
    onPresetSelected: (HomeLayoutPreset) -> Unit,
    onAutoSnapChange: (Boolean) -> Unit,
    onAutoAvoidChange: (Boolean) -> Unit,
    onAutoArrange: () -> Unit,
) {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_layout_presets),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HomeLayoutPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = state.itemsForOrientation(isLandscape).matchesPreset(
                            preset = preset,
                            isLandscape = isLandscape,
                        ),
                        onClick = { onPresetSelected(preset) },
                        label = { Text(stringResource(preset.labelRes())) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_layout_auto_snap),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.home_layout_auto_snap_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.autoSnap,
                    onCheckedChange = onAutoSnapChange,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_layout_auto_avoid),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.home_layout_auto_avoid_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.autoAvoidOverlap,
                    onCheckedChange = onAutoAvoidChange,
                )
            }
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAutoArrange,
            ) {
                Text(stringResource(R.string.home_layout_auto_arrange))
            }
        }
    }
}

@Composable
private fun HomeLayoutFullscreenControls(
    state: HomeLayoutState,
    isLandscape: Boolean,
    selectedCard: HomeLayoutCard,
    showLayers: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    overlappingCards: Set<HomeLayoutCard>,
    saving: Boolean,
    dirty: Boolean,
    onSelectedCardChange: (HomeLayoutCard) -> Unit,
    onOrientationChange: (Boolean) -> Unit,
    onToggleLayers: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onVisibilityChange: (HomeLayoutCard, Boolean) -> Unit,
    onMoveLayer: (HomeLayoutCard, Int) -> Unit,
    onAlign: (Float) -> Unit,
    onNudge: (Float, Float) -> Unit,
    onAutoAvoidChange: (Boolean) -> Unit,
    onAutoSnapChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeItems = state.itemsForOrientation(isLandscape)
    Surface(
        modifier = modifier.windowInsetsPadding(
            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
        ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 3.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onClose, enabled = !saving) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.home_layout_selected_card,
                            stringResource(selectedCard.labelRes()),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            when {
                                saving -> R.string.home_layout_saving
                                dirty -> R.string.home_layout_unsaved
                                else -> R.string.home_layout_saved
                            },
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dirty) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                IconButton(onClick = { onOrientationChange(false) }) {
                    Icon(
                        imageVector = Icons.Rounded.Portrait,
                        contentDescription = stringResource(R.string.home_layout_portrait),
                        tint = if (!isLandscape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onOrientationChange(true) }) {
                    Icon(
                        imageVector = Icons.Rounded.Landscape,
                        contentDescription = stringResource(R.string.home_layout_landscape),
                        tint = if (isLandscape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onSave, enabled = !saving && dirty) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_layout_save))
                }
            }
            AnimatedVisibility(visible = showLayers) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.home_layout_layers),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.home_layout_auto_avoid),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Switch(
                            checked = state.autoAvoidOverlap,
                            onCheckedChange = onAutoAvoidChange,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.home_layout_auto_snap),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.autoSnap,
                            onCheckedChange = onAutoSnapChange,
                        )
                    }
                    activeItems.sortedByDescending { it.zIndex }.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (item.card == selectedCard) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .clickable { onSelectedCardChange(item.card) }
                                .padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(item.card.labelRes()),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (item.card == selectedCard) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(
                                onClick = { onVisibilityChange(item.card, !item.visible) },
                            ) {
                                Icon(
                                    imageVector = if (item.visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = stringResource(R.string.home_layout_toggle_visibility),
                                )
                            }
                            IconButton(
                                enabled = item.zIndex > 0,
                                onClick = { onMoveLayer(item.card, -1) },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = stringResource(R.string.home_layout_layer_down),
                                )
                            }
                            IconButton(
                                enabled = item.zIndex < HomeLayoutCard.entries.lastIndex,
                                onClick = { onMoveLayer(item.card, 1) },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = stringResource(R.string.home_layout_layer_up),
                                )
                            }
                        }
                    }
                }
            }
            if (overlappingCards.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = stringResource(
                            R.string.home_layout_overlap_warning,
                            homeLayoutCardNames(overlappingCards),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = stringResource(R.string.home_layout_undo),
                    )
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Redo,
                        contentDescription = stringResource(R.string.home_layout_redo),
                    )
                }
                IconButton(onClick = onToggleLayers) {
                    Icon(Icons.Rounded.Layers, contentDescription = stringResource(R.string.home_layout_layers))
                }
                IconButton(onClick = { onAlign(0f) }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.FormatAlignLeft,
                        contentDescription = stringResource(R.string.home_layout_align_left),
                    )
                }
                IconButton(onClick = { onAlign(0.5f) }) {
                    Icon(Icons.Rounded.FormatAlignCenter, contentDescription = stringResource(R.string.home_layout_align_center))
                }
                IconButton(onClick = { onAlign(1f) }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.FormatAlignRight,
                        contentDescription = stringResource(R.string.home_layout_align_right),
                    )
                }
                IconButton(
                    enabled = activeItems.first { it.card == selectedCard }.width < 0.999f,
                    onClick = { onNudge(-0.025f, 0f) },
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.home_layout_nudge_left))
                }
                IconButton(onClick = { onNudge(0f, -0.05f) }) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.home_layout_nudge_up))
                }
                IconButton(onClick = { onNudge(0f, 0.05f) }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.home_layout_nudge_down))
                }
                IconButton(
                    enabled = activeItems.first { it.card == selectedCard }.width < 0.999f,
                    onClick = { onNudge(0.025f, 0f) },
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.home_layout_nudge_right))
                }
            }
        }
    }
}

@Composable
private fun HomeLayoutPreview(
    state: HomeLayoutState,
    homeUiState: HomeUiState,
    isLandscape: Boolean,
    onOpenFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    canvasHeight: Dp = 340.dp,
) {
    val actions = rememberHomeLayoutPreviewActions()
    TonalCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_layout_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(onClick = onOpenFullscreen) {
                    Icon(Icons.Rounded.Fullscreen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_layout_fullscreen))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ),
                    )
                    .padding(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.home_layout_live_mapping_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HomeLayoutCanvas(
                        state = state,
                        modifier = Modifier.fillMaxWidth(),
                        isLandscapeOverride = isLandscape,
                    ) { item ->
                        HomeLayoutCardContent(
                            item = item,
                            state = homeUiState,
                            actions = actions,
                            installFeedbackActive = false,
                            forceLkmPreview = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberHomeLayoutPreviewActions(): HomeActions {
    return remember {
        HomeActions(
            onInstallClick = {},
            onSuperuserClick = {},
            onModuleClick = {},
            onOpenUrl = {},
        )
    }
}

@Composable
private fun HomeLayoutInspector(
    item: HomeLayoutItem,
    selectedStickerId: String?,
    onCardSelected: (HomeLayoutCard) -> Unit,
    onItemChange: (HomeLayoutItem) -> Unit,
    onCommittedItemChange: (HomeLayoutItem) -> Unit,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
    onAlign: (Float) -> Unit,
    onNudge: (Float, Float) -> Unit,
    onAddSticker: () -> Unit,
    onStickerSelected: (String?) -> Unit,
    onStickerChange: (HomeLayoutSticker) -> Unit,
    onStickerDelete: (HomeLayoutSticker) -> Unit,
) {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.home_layout_selected_card, stringResource(item.card.labelRes())),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HomeLayoutCard.entries.forEach { card ->
                    FilterChip(
                        selected = card == item.card,
                        onClick = { onCardSelected(card) },
                        label = { Text(stringResource(card.labelRes())) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_layout_card_visible),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = item.visible,
                    onCheckedChange = { onCommittedItemChange(item.copy(visible = it)) },
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = item.customTitle,
                onValueChange = { value ->
                    onCommittedItemChange(item.copy(customTitle = value.take(80)))
                },
                label = {
                    Text(
                        stringResource(
                            if (item.card == HomeLayoutCard.Lkm) {
                                R.string.home_layout_lkm_status_text
                            } else {
                                R.string.home_layout_custom_title
                            },
                        ),
                    )
                },
                supportingText = {
                    Text(
                        stringResource(
                            if (item.card == HomeLayoutCard.Lkm) {
                                R.string.home_layout_lkm_status_text_hint
                            } else {
                                R.string.home_layout_custom_text_hint
                            },
                        ),
                    )
                },
                singleLine = true,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = item.customSubtitle,
                onValueChange = { value ->
                    onCommittedItemChange(item.copy(customSubtitle = value.take(160)))
                },
                label = {
                    Text(
                        stringResource(
                            if (item.card == HomeLayoutCard.Lkm) {
                                R.string.home_layout_manager_version_text
                            } else {
                                R.string.home_layout_custom_subtitle
                            },
                        ),
                    )
                },
                supportingText = {
                    Text(
                        stringResource(
                            if (item.card == HomeLayoutCard.Lkm) {
                                R.string.home_layout_manager_version_text_hint
                            } else {
                                R.string.home_layout_custom_text_hint
                            },
                        ),
                    )
                },
                maxLines = 2,
            )
            HomeLayoutSlider(
                title = stringResource(R.string.home_layout_text_scale),
                value = item.textScale,
                valueRange = 0.72f..1.25f,
                label = "${(item.textScale * 100).roundToInt()}%",
                onValueChange = {
                    onInteractionStart()
                    onItemChange(item.copy(textScale = it))
                },
                onValueChangeFinished = onInteractionEnd,
            )
            Text(
                text = stringResource(R.string.home_layout_wallpaper_fit),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HomeLayoutWallpaperFit.entries.forEach { fit ->
                    FilterChip(
                        selected = item.wallpaperFit == fit,
                        onClick = { onCommittedItemChange(item.copy(wallpaperFit = fit)) },
                        label = { Text(stringResource(fit.labelRes())) },
                    )
                }
            }
            HomeLayoutSlider(
                title = stringResource(R.string.home_layout_width),
                value = item.width,
                valueRange = 0.28f..1f,
                label = "${(item.width * 100).roundToInt()}%",
                onValueChange = {
                    onInteractionStart()
                    onItemChange(item.copy(width = it))
                },
                onValueChangeFinished = onInteractionEnd,
            )
            val customHeight = item.height > 0f
            val heightValue = item.height.takeIf { customHeight }
                ?: suggestedHomeLayoutHeight(item.card)
            HomeLayoutSlider(
                title = stringResource(R.string.home_layout_height),
                value = heightValue,
                valueRange = minimumHomeLayoutHeight(item.card)..4f,
                label = if (customHeight) {
                    stringResource(
                        R.string.home_layout_height_value,
                        (heightValue * 150f).roundToInt(),
                    )
                } else {
                    stringResource(R.string.home_layout_height_auto)
                },
                onValueChange = {
                    onInteractionStart()
                    onItemChange(item.copy(height = it))
                },
                onValueChangeFinished = onInteractionEnd,
            )
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = customHeight,
                onClick = { onCommittedItemChange(item.copy(height = 0f)) },
            ) {
                Text(stringResource(R.string.home_layout_restore_xiaomi_height))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.home_layout_width), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = {
                    onCommittedItemChange(item.copy(width = item.width - 0.04f))
                }) {
                    Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.home_layout_shrink_width))
                }
                IconButton(onClick = {
                    onCommittedItemChange(item.copy(width = item.width + 0.04f))
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.home_layout_expand_width))
                }
                Text(stringResource(R.string.home_layout_height), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = {
                    val current = item.height.takeIf { it > 0f } ?: suggestedHomeLayoutHeight(item.card)
                    onCommittedItemChange(item.copy(height = current - 0.08f))
                }) {
                    Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.home_layout_shrink_height))
                }
                IconButton(onClick = {
                    val current = item.height.takeIf { it > 0f } ?: suggestedHomeLayoutHeight(item.card)
                    onCommittedItemChange(item.copy(height = current + 0.08f))
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.home_layout_expand_height))
                }
            }
            HomeLayoutSlider(
                title = stringResource(R.string.home_layout_horizontal),
                value = item.x,
                valueRange = 0f..1f,
                label = "${(item.x * 100).roundToInt()}%",
                enabled = item.width < 0.999f,
                onValueChange = {
                    onInteractionStart()
                    onItemChange(item.copy(x = it))
                },
                onValueChangeFinished = onInteractionEnd,
            )
            Text(
                text = stringResource(R.string.home_layout_nudge),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    enabled = item.width < 0.999f,
                    onClick = { onNudge(-0.025f, 0f) },
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.home_layout_nudge_left))
                }
                IconButton(onClick = { onNudge(0f, -0.05f) }) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.home_layout_nudge_up))
                }
                IconButton(onClick = { onNudge(0f, 0.05f) }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.home_layout_nudge_down))
                }
                IconButton(
                    enabled = item.width < 0.999f,
                    onClick = { onNudge(0.025f, 0f) },
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.home_layout_nudge_right))
                }
            }
            HomeLayoutSlider(
                title = stringResource(R.string.home_layout_vertical),
                value = item.y,
                valueRange = 0f..6f,
                label = "${(item.y / 6f * 100).roundToInt()}%",
                onValueChange = {
                    onInteractionStart()
                    onItemChange(item.copy(y = it))
                },
                onValueChangeFinished = onInteractionEnd,
            )
            Text(
                text = stringResource(R.string.home_layout_alignment),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onAlign(0f) },
                ) {
                    Text(stringResource(R.string.home_layout_align_left))
                }
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onAlign(0.5f) },
                ) {
                    Text(stringResource(R.string.home_layout_align_center))
                }
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onAlign(1f) },
                ) {
                    Text(stringResource(R.string.home_layout_align_right))
                }
            }
            if (item.card == HomeLayoutCard.Lkm && item.height <= 0f) {
                HomeLayoutSlider(
                    title = stringResource(R.string.home_layout_lkm_aspect_ratio),
                    value = item.aspectRatio,
                    valueRange = 1f..2.2f,
                    label = "${(item.aspectRatio * 100).roundToInt() / 100f} : 1",
                    onValueChange = {
                        onInteractionStart()
                        onItemChange(item.copy(aspectRatio = it))
                    },
                    onValueChangeFinished = onInteractionEnd,
                )
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onCommittedItemChange(item.copy(aspectRatio = 1f, height = 0f)) },
                ) {
                    Text(stringResource(R.string.home_layout_set_square))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_layout_stickers),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.home_layout_stickers_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    enabled = item.stickers.size < 12,
                    onClick = onAddSticker,
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_layout_add_sticker))
                }
            }
            if (item.stickers.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_layout_no_stickers),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item.stickers.forEachIndexed { index, sticker ->
                        FilterChip(
                            selected = selectedStickerId == sticker.id ||
                                (selectedStickerId == null && index == 0),
                            onClick = { onStickerSelected(sticker.id) },
                            label = {
                                Text(stringResource(R.string.home_layout_sticker_number, index + 1))
                            },
                        )
                    }
                }
                val selectedSticker = item.stickers.firstOrNull { it.id == selectedStickerId }
                    ?: item.stickers.first()
                HomeLayoutSlider(
                    title = stringResource(R.string.home_layout_sticker_horizontal),
                    value = selectedSticker.x,
                    valueRange = 0f..1f,
                    label = "${(selectedSticker.x * 100).roundToInt()}%",
                    onValueChange = {
                        onInteractionStart()
                        onStickerChange(selectedSticker.copy(x = it))
                    },
                    onValueChangeFinished = onInteractionEnd,
                )
                HomeLayoutSlider(
                    title = stringResource(R.string.home_layout_sticker_vertical),
                    value = selectedSticker.y,
                    valueRange = 0f..1f,
                    label = "${(selectedSticker.y * 100).roundToInt()}%",
                    onValueChange = {
                        onInteractionStart()
                        onStickerChange(selectedSticker.copy(y = it))
                    },
                    onValueChangeFinished = onInteractionEnd,
                )
                HomeLayoutSlider(
                    title = stringResource(R.string.home_layout_sticker_size),
                    value = selectedSticker.width,
                    valueRange = 0.08f..1f,
                    label = "${(selectedSticker.width * 100).roundToInt()}%",
                    onValueChange = {
                        onInteractionStart()
                        onStickerChange(selectedSticker.copy(width = it))
                    },
                    onValueChangeFinished = onInteractionEnd,
                )
                HomeLayoutSlider(
                    title = stringResource(R.string.home_layout_sticker_opacity),
                    value = selectedSticker.opacity,
                    valueRange = 0.1f..1f,
                    label = "${(selectedSticker.opacity * 100).roundToInt()}%",
                    onValueChange = {
                        onInteractionStart()
                        onStickerChange(selectedSticker.copy(opacity = it))
                    },
                    onValueChangeFinished = onInteractionEnd,
                )
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onStickerDelete(selectedSticker) },
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_layout_delete_sticker))
                }
            }
        }
    }
}

@Composable
private fun HomeLayoutSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
        Slider(
            value = value,
            valueRange = valueRange,
            enabled = enabled,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

private fun HomeLayoutCard.labelRes(): Int = when (this) {
    HomeLayoutCard.Lkm -> R.string.home_layout_card_lkm
    HomeLayoutCard.Superuser -> R.string.home_layout_card_superuser
    HomeLayoutCard.Module -> R.string.home_layout_card_module
    HomeLayoutCard.StatusMonitor -> R.string.home_layout_card_status_monitor
    HomeLayoutCard.SystemInfo -> R.string.home_layout_card_system_info
}

@Composable
private fun homeLayoutCardNames(cards: Set<HomeLayoutCard>): String {
    val names = mutableListOf<String>()
    for (card in cards) names += stringResource(card.labelRes())
    return names.joinToString()
}

private fun HomeLayoutPreset.labelRes(): Int = when (this) {
    HomeLayoutPreset.DualColumn -> R.string.home_layout_preset_dual
    HomeLayoutPreset.SingleColumn -> R.string.home_layout_preset_single
    HomeLayoutPreset.Compact -> R.string.home_layout_preset_compact
}

private fun List<HomeLayoutItem>.matchesPreset(
    preset: HomeLayoutPreset,
    isLandscape: Boolean,
): Boolean {
    val expected = homeLayoutItemsForPreset(preset, isLandscape).associateBy { it.card }
    return all { item ->
        val target = expected[item.card] ?: return@all false
        item.visible == target.visible &&
            kotlin.math.abs(item.x - target.x) < 0.001f &&
            kotlin.math.abs(item.y - target.y) < 0.001f &&
            kotlin.math.abs(item.width - target.width) < 0.001f &&
            kotlin.math.abs(item.aspectRatio - target.aspectRatio) < 0.001f &&
            kotlin.math.abs(item.height - target.height) < 0.001f &&
            item.zIndex == target.zIndex
    }
}

private fun HomeLayoutWallpaperFit.labelRes(): Int = when (this) {
    HomeLayoutWallpaperFit.Crop -> R.string.home_layout_wallpaper_crop
    HomeLayoutWallpaperFit.Fit -> R.string.home_layout_wallpaper_fit_inside
    HomeLayoutWallpaperFit.Stretch -> R.string.home_layout_wallpaper_stretch
}

private const val HOME_LAYOUT_HISTORY_LIMIT = 30

private fun readHomeLayoutTransfer(input: InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        require(total <= HOME_LAYOUT_TRANSFER_MAX_BYTES) { "Home layout file is too large" }
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}
