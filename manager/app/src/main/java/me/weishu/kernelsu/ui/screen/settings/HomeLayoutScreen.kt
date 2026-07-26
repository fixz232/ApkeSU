package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Layers
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import me.weishu.kernelsu.ui.util.HomeLayoutState
import me.weishu.kernelsu.ui.util.CustomBackgroundState
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.homeLayoutItemsForPreset
import me.weishu.kernelsu.ui.util.minimumHomeLayoutHeight
import me.weishu.kernelsu.ui.util.moveHomeLayoutCardLayer
import me.weishu.kernelsu.ui.util.readHomeLayoutState
import me.weishu.kernelsu.ui.util.resizeHomeLayoutItem
import me.weishu.kernelsu.ui.util.sanitizeHomeLayoutItem
import me.weishu.kernelsu.ui.util.saveHomeLayoutState
import me.weishu.kernelsu.ui.util.snapHomeLayoutItem
import me.weishu.kernelsu.ui.util.suggestedHomeLayoutHeight
import me.weishu.kernelsu.ui.viewmodel.HomeViewModel
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import me.weishu.kernelsu.ui.theme.LocalImmersiveBackgroundActive
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeLayoutScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val homeViewModel = viewModel<HomeViewModel>()
    val settingsViewModel = viewModel<SettingsViewModel>()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val popBack = dropUnlessResumed { navigator.pop() }
    val initialState = remember(context) { readHomeLayoutState(context) }
    var state by remember { mutableStateOf(initialState) }
    var savedState by remember { mutableStateOf(initialState) }
    var selectedCard by remember { mutableStateOf(HomeLayoutCard.Lkm) }
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
    val saveSuccess = stringResource(R.string.home_layout_save_success)
    val saveFailed = stringResource(R.string.home_layout_save_failed)
    val resetSuccess = stringResource(R.string.home_layout_reset_success)
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

    fun updateItem(card: HomeLayoutCard, transform: (HomeLayoutItem) -> HomeLayoutItem) {
        state = state.copy(
            items = state.items.map { item ->
                if (item.card == card) sanitizeHomeLayoutItem(transform(item)) else item
            },
        )
    }

    fun beginInteraction() {
        if (interactionStartState == null) interactionStartState = state
    }

    fun finishInteraction(card: HomeLayoutCard) {
        val currentItem = state.items.firstOrNull { it.card == card }
        if (currentItem != null) {
            val snapped = snapHomeLayoutItem(currentItem, state.items)
            updateItem(card) { snapped }
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

    fun save(
        next: HomeLayoutState = state,
        successText: String = saveSuccess,
        onSuccess: () -> Unit = {},
    ) {
        if (saving) return
        saving = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { saveHomeLayoutState(context, next) }
            if (ok) {
                val persisted = readHomeLayoutState(context)
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

            message?.let {
                TonalCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            HomeLayoutPresetControls(
                state = state,
                onPresetSelected = { preset ->
                    applyState(state.copy(items = homeLayoutItemsForPreset(preset)))
                },
                onAutoAvoidChange = { enabled ->
                    applyState(state.copy(autoAvoidOverlap = enabled))
                },
            )

            HomeLayoutPreview(
                state = state,
                homeUiState = homeUiState,
                onOpenFullscreen = { fullscreenEditor = true },
            )

            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showPreciseControls = !showPreciseControls },
            ) {
                Icon(
                    imageVector = if (showPreciseControls) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_layout_precise_controls))
            }

            AnimatedVisibility(visible = showPreciseControls) {
                HomeLayoutInspector(
                    item = state.items.first { it.card == selectedCard },
                    onCardSelected = { selectedCard = it },
                    onItemChange = { item -> updateItem(item.card) { item } },
                    onCommittedItemChange = { item ->
                        applyState(
                            state.copy(
                                items = state.items.map { current ->
                                    if (current.card == item.card) sanitizeHomeLayoutItem(item) else current
                                },
                            ),
                        )
                    },
                    onInteractionStart = ::beginInteraction,
                    onInteractionEnd = { finishInteraction(selectedCard) },
                    onAlign = { x ->
                        applyState(
                            state.copy(
                                items = state.items.map { item ->
                                    if (item.card == selectedCard) item.copy(x = x) else item
                                },
                            ),
                        )
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
                CompositionLocalProvider(LocalInterfaceStyle provides InterfaceStyle.Miuix.value) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HomePagerMiuix(
                            state = homeUiState,
                            actions = rememberHomeLayoutPreviewActions(),
                            bottomInnerPadding = if (showLayerPanel) 420.dp else 112.dp,
                            installFeedbackActive = false,
                            homeLayoutOverride = state.copy(enabled = true),
                            homeLayoutEditor = HomeLayoutEditor(
                                selectedCard = selectedCard,
                                onSelectedCardChange = { selectedCard = it },
                                onDragCard = { card, delta ->
                                    updateItem(card) { item ->
                                        item.copy(
                                            x = item.x + delta.x,
                                            y = item.y + delta.y,
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
                                .fillMaxWidth(),
                            state = state,
                            selectedCard = selectedCard,
                            showLayers = showLayerPanel,
                            canUndo = undoStack.isNotEmpty(),
                            canRedo = redoStack.isNotEmpty(),
                            overlappingCards = overlappingCards,
                            saving = saving,
                            dirty = dirty,
                            onSelectedCardChange = { selectedCard = it },
                            onToggleLayers = { showLayerPanel = !showLayerPanel },
                            onUndo = ::undo,
                            onRedo = ::redo,
                            onVisibilityChange = { card, visible ->
                                val item = state.items.first { it.card == card }
                                applyState(
                                    state.copy(
                                        items = state.items.map { current ->
                                            if (current.card == card) item.copy(visible = visible) else current
                                        },
                                    ),
                                )
                            },
                            onMoveLayer = { card, direction ->
                                applyState(
                                    state.copy(
                                        items = moveHomeLayoutCardLayer(state.items, card, direction),
                                    ),
                                )
                            },
                            onAlign = { x ->
                                applyState(
                                    state.copy(
                                        items = state.items.map { item ->
                                            if (item.card == selectedCard) item.copy(x = x) else item
                                        },
                                    ),
                                )
                            },
                            onAutoAvoidChange = { enabled ->
                                applyState(state.copy(autoAvoidOverlap = enabled))
                            },
                            onClose = { fullscreenEditor = false },
                            onSave = { save(onSuccess = { fullscreenEditor = false }) },
                        )
                    }
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
        opacity = homeBackground.opacity,
        crop = homeBackground.crop,
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
private fun HomeLayoutPresetControls(
    state: HomeLayoutState,
    onPresetSelected: (HomeLayoutPreset) -> Unit,
    onAutoAvoidChange: (Boolean) -> Unit,
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
                        selected = state.items.matchesPreset(preset),
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
        }
    }
}

@Composable
private fun HomeLayoutFullscreenControls(
    state: HomeLayoutState,
    selectedCard: HomeLayoutCard,
    showLayers: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    overlappingCards: Set<HomeLayoutCard>,
    saving: Boolean,
    dirty: Boolean,
    onSelectedCardChange: (HomeLayoutCard) -> Unit,
    onToggleLayers: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onVisibilityChange: (HomeLayoutCard, Boolean) -> Unit,
    onMoveLayer: (HomeLayoutCard, Int) -> Unit,
    onAlign: (Float) -> Unit,
    onAutoAvoidChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.windowInsetsPadding(
            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
        ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 3.dp,
    ) {
        Column {
            AnimatedVisibility(visible = showLayers) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                    state.items.sortedByDescending { it.zIndex }.forEach { item ->
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
                Text(
                    text = stringResource(
                        R.string.home_layout_selected_card,
                        stringResource(selectedCard.labelRes()),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                IconButton(onClick = onClose, enabled = !saving) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                }
                Button(onClick = onSave, enabled = !saving && dirty) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.home_layout_save))
                }
            }
        }
    }
}

@Composable
private fun HomeLayoutPreview(
    state: HomeLayoutState,
    homeUiState: HomeUiState,
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
                    CompositionLocalProvider(LocalInterfaceStyle provides InterfaceStyle.Miuix.value) {
                        HomeLayoutCanvas(
                            state = state,
                            modifier = Modifier.fillMaxWidth(),
                        ) { item ->
                            HomeLayoutCardContent(
                                item = item,
                                state = homeUiState,
                                actions = actions,
                                installFeedbackActive = false,
                            )
                        }
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
    onCardSelected: (HomeLayoutCard) -> Unit,
    onItemChange: (HomeLayoutItem) -> Unit,
    onCommittedItemChange: (HomeLayoutItem) -> Unit,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
    onAlign: (Float) -> Unit,
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
            HomeLayoutSlider(
                title = stringResource(R.string.home_layout_width),
                value = item.width,
                valueRange = 0.36f..1f,
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
            HomeLayoutSlider(
                title = stringResource(R.string.home_layout_horizontal),
                value = item.x,
                valueRange = 0f..1f,
                label = "${(item.x * 100).roundToInt()}%",
                onValueChange = {
                    onInteractionStart()
                    onItemChange(item.copy(x = it))
                },
                onValueChangeFinished = onInteractionEnd,
            )
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
            if (item.card == HomeLayoutCard.Lkm) {
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
        }
    }
}

@Composable
private fun HomeLayoutSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
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

private fun List<HomeLayoutItem>.matchesPreset(preset: HomeLayoutPreset): Boolean {
    val expected = homeLayoutItemsForPreset(preset).associateBy { it.card }
    return all { item ->
        val target = expected[item.card] ?: return@all false
        item.visible == target.visible &&
            kotlin.math.abs(item.x - target.x) < 0.001f &&
            kotlin.math.abs(item.y - target.y) < 0.001f &&
            kotlin.math.abs(item.width - target.width) < 0.001f &&
            kotlin.math.abs(item.aspectRatio - target.aspectRatio) < 0.001f
    }
}

private const val HOME_LAYOUT_HISTORY_LIMIT = 30
