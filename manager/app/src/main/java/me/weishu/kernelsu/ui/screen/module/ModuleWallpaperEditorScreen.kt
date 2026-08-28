package me.weishu.kernelsu.ui.screen.module

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.screen.settings.SettingsWallpaperCropDialog
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.MediaVisualSettings
import me.weishu.kernelsu.ui.viewmodel.ModuleViewModel

private enum class WallpaperImportMode { Add, Replace }
private enum class WallpaperBatchMode { Selected, SameType, All }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleWallpaperEditorScreen(
    moduleId: String,
    displayName: String? = null,
    displayAuthor: String? = null,
    displayVersion: String? = null,
    displayDescription: String? = null,
    allowBatch: Boolean = true,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val moduleViewModel = viewModel<ModuleViewModel>()
    val moduleUiState by moduleViewModel.uiState.collectAsStateWithLifecycle()
    val isKpmWallpaper = moduleId.startsWith(KPM_CARD_WALLPAPER_ID_PREFIX)
    val module = moduleUiState.moduleList.firstOrNull { it.id == moduleId }
    var snapshot by remember(moduleId) { mutableStateOf(readModuleCardWallpaperSnapshot(context, moduleId)) }
    var variant by remember { mutableStateOf(ModuleWallpaperVariant.Day) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var previewAspectRatio by remember { mutableFloatStateOf(MODULE_CARD_WALLPAPER_ASPECT_RATIO) }
    var importMode by remember { mutableStateOf(WallpaperImportMode.Add) }
    var busy by remember { mutableStateOf(false) }
    var showCrop by remember { mutableStateOf(false) }
    var showBatch by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val collection = snapshot.collection(variant)
    val selectedEntry = collection.entries.getOrNull(
        selectedIndex.coerceIn(0, collection.entries.lastIndex.coerceAtLeast(0))
    )
    val previewState = rememberModuleCardWallpaperLoadState(selectedEntry)

    fun commit(next: ModuleCardWallpaperSnapshot): Boolean {
        if (!replaceModuleCardWallpaperSnapshot(context, moduleId, next)) {
            errorText = resources.getString(R.string.module_wallpaper_save_failed)
            return false
        }
        snapshot = next
        selectedIndex = next.collection(variant).selectedIndex
        return true
    }

    fun updateCollection(transform: (ModuleWallpaperCollection) -> ModuleWallpaperCollection) {
        val nextCollection = transform(snapshot.collection(variant)).normalized()
        commit(snapshot.withCollection(variant, nextCollection))
    }

    fun updateSelectedEntry(
        persist: Boolean = true,
        transform: (ModuleCardWallpaperEntry) -> ModuleCardWallpaperEntry,
    ) {
        val index = selectedIndex.coerceIn(0, collection.entries.lastIndex.coerceAtLeast(0))
        if (collection.entries.getOrNull(index) == null) return
        val nextEntries = collection.entries.mapIndexed { itemIndex, entry ->
            if (itemIndex == index) transform(entry) else entry
        }
        val next = snapshot.withCollection(variant, collection.copy(entries = nextEntries, selectedIndex = index))
        snapshot = next
        if (persist && !replaceModuleCardWallpaperSnapshot(context, moduleId, next)) {
            snapshot = readModuleCardWallpaperSnapshot(context, moduleId)
            errorText = resources.getString(R.string.module_wallpaper_save_failed)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                importModuleWallpaperEntries(context, moduleId, variant, uris)
            }
            result.onSuccess { imported ->
                val previous = snapshot.collection(variant)
                val nextEntries = when (importMode) {
                    WallpaperImportMode.Add -> previous.entries + imported
                    WallpaperImportMode.Replace -> imported
                }
                if (nextEntries.size > MODULE_CARD_WALLPAPER_MAX_COUNT) {
                    imported.forEach { me.weishu.kernelsu.ui.util.releaseCustomImageReference(context, it.uriString) }
                    errorText = resources.getString(
                        R.string.module_wallpaper_too_many,
                        MODULE_CARD_WALLPAPER_MAX_COUNT,
                    )
                } else {
                    val nextCollection = previous.copy(
                        entries = nextEntries,
                        carouselEnabled = nextEntries.size > 1 && previous.carouselEnabled,
                        selectedIndex = if (importMode == WallpaperImportMode.Add) previous.entries.size else 0,
                    )
                    val next = snapshot.withCollection(variant, nextCollection)
                    if (replaceModuleCardWallpaperSnapshot(context, moduleId, next)) {
                        snapshot = next
                        selectedIndex = nextCollection.selectedIndex
                    } else {
                        imported.forEach { me.weishu.kernelsu.ui.util.releaseCustomImageReference(context, it.uriString) }
                        errorText = resources.getString(R.string.module_wallpaper_save_failed)
                    }
                }
            }.onFailure {
                errorText = resources.getString(R.string.module_wallpaper_import_failed)
            }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        if (!isKpmWallpaper) {
            moduleViewModel.initializePreferences()
            moduleViewModel.fetchModuleList(checkUpdate = false)
        }
    }
    LaunchedEffect(variant, collection.entries.size) {
        selectedIndex = collection.selectedIndex.coerceIn(0, collection.entries.lastIndex.coerceAtLeast(0))
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_wallpaper_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = navigator::pop) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                VariantSelector(
                    variant = variant,
                    onVariantChange = { variant = it },
                )
            }
            item {
                ModuleWallpaperRealPreview(
                    module = module,
                    displayName = displayName,
                    displayAuthor = displayAuthor,
                    displayVersion = displayVersion,
                    displayDescription = displayDescription,
                    entry = selectedEntry,
                    loadState = previewState,
                    aspectRatio = previewAspectRatio,
                )
            }
            item {
                PreviewRatioSelector(
                    selected = previewAspectRatio,
                    onSelected = { previewAspectRatio = it },
                )
            }
            if (variant == ModuleWallpaperVariant.Night && collection.entries.isEmpty() && snapshot.entries.isNotEmpty()) {
                item {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                val copied = withContext(Dispatchers.IO) {
                                    duplicateModuleWallpaperSnapshotForTarget(
                                        context,
                                        "${moduleId}_night_seed",
                                        ModuleCardWallpaperSnapshot(snapshot.entries, snapshot.carouselEnabled),
                                    )
                                }
                                copied.onSuccess { duplicate ->
                                    val next = snapshot.withCollection(
                                        ModuleWallpaperVariant.Night,
                                        duplicate.collection(ModuleWallpaperVariant.Day),
                                    )
                                    if (replaceModuleCardWallpaperSnapshot(context, moduleId, next)) {
                                        snapshot = next
                                    } else {
                                        releaseModuleWallpaperSnapshot(context, duplicate)
                                    }
                                }.onFailure { errorText = resources.getString(R.string.module_wallpaper_save_failed) }
                                busy = false
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.module_wallpaper_copy_day_to_night))
                    }
                }
            }
            item {
                WallpaperQueue(
                    entries = collection.entries,
                    selectedIndex = selectedIndex,
                    enabled = !busy,
                    onSelect = { index ->
                        selectedIndex = index
                        updateCollection { it.copy(selectedIndex = index) }
                    },
                    onMove = { from, to ->
                        if (from == to) return@WallpaperQueue
                        val reordered = collection.entries.toMutableList().apply {
                            add(to, removeAt(from))
                        }
                        val nextIndex = when (selectedIndex) {
                            from -> to
                            in minOf(from, to)..maxOf(from, to) -> selectedIndex + if (from < to) -1 else 1
                            else -> selectedIndex
                        }
                        selectedIndex = nextIndex
                        updateCollection { it.copy(entries = reordered, selectedIndex = nextIndex) }
                    },
                    onDelete = { index ->
                        val nextEntries = collection.entries.toMutableList().apply { removeAt(index) }
                        selectedIndex = selectedIndex.coerceAtMost(nextEntries.lastIndex.coerceAtLeast(0))
                        updateCollection {
                            it.copy(
                                entries = nextEntries,
                                selectedIndex = selectedIndex,
                                carouselEnabled = it.carouselEnabled && nextEntries.size > 1,
                            )
                        }
                    },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !busy && collection.entries.size < MODULE_CARD_WALLPAPER_MAX_COUNT,
                        onClick = {
                            importMode = WallpaperImportMode.Add
                            imagePicker.launch(arrayOf("image/*"))
                        },
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.module_wallpaper_add))
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        onClick = {
                            importMode = WallpaperImportMode.Replace
                            imagePicker.launch(arrayOf("image/*"))
                        },
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.module_wallpaper_replace))
                    }
                    IconButton(enabled = selectedEntry != null && !busy, onClick = { showCrop = true }) {
                        Icon(Icons.Rounded.Edit, stringResource(R.string.module_wallpaper_crop))
                    }
                }
            }
            selectedEntry?.let { entry ->
                item {
                    VisualSettingsEditor(
                        settings = entry.visualSettings,
                        autoContrast = entry.autoContrast,
                        onSettingsPreview = { next -> updateSelectedEntry(persist = false) { it.copy(visualSettings = next) } },
                        onSettingsCommit = { next -> updateSelectedEntry { it.copy(visualSettings = next) } },
                        onAutoContrastChange = { enabled -> updateSelectedEntry { it.copy(autoContrast = enabled) } },
                    )
                }
            }
            if (collection.entries.size > 1) {
                item {
                    CarouselSettings(
                        collection = collection,
                        onChange = { next -> commit(snapshot.withCollection(variant, next)) },
                    )
                }
            }
            if (allowBatch) item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = snapshot.allEntries().isNotEmpty() && moduleUiState.moduleList.size > 1 && !busy,
                    onClick = { showBatch = true },
                ) {
                    Icon(Icons.Rounded.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.module_wallpaper_batch_apply))
                }
            }
            if (busy) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.module_wallpaper_processing))
                    }
                }
            }
        }
    }

    SettingsWallpaperCropDialog(
        show = showCrop && selectedEntry != null,
        uriString = selectedEntry?.uriString,
        crop = selectedEntry?.crop ?: me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP,
        onCropChange = { crop -> updateSelectedEntry { it.copy(crop = crop) } },
        onDismissRequest = { showCrop = false },
        title = stringResource(R.string.module_wallpaper_crop),
        editorAspectRatio = previewAspectRatio,
        cropAspectRatio = previewAspectRatio,
    )
    if (showBatch) {
        ModuleWallpaperBatchDialog(
            sourceModule = module,
            modules = moduleUiState.moduleList,
            busy = busy,
            onDismiss = { showBatch = false },
            onApply = { targets ->
                showBatch = false
                busy = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            val replacements = linkedMapOf<String, ModuleCardWallpaperSnapshot>()
                            try {
                                targets.forEach { target ->
                                    replacements[target.id] = duplicateModuleWallpaperSnapshotForTarget(
                                        context,
                                        target.id,
                                        snapshot,
                                    ).getOrThrow()
                                }
                                if (!replaceModuleCardWallpaperSnapshots(context, replacements)) {
                                    error("Unable to save batch wallpaper settings")
                                }
                            } catch (error: Throwable) {
                                replacements.values.forEach { releaseModuleWallpaperSnapshot(context, it) }
                                throw error
                            }
                        }
                    }
                    result.onSuccess {
                        Toast.makeText(context, R.string.module_wallpaper_batch_success, Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        errorText = resources.getString(R.string.module_wallpaper_batch_failed)
                    }
                    busy = false
                }
            },
        )
    }
    errorText?.let { message ->
        AlertDialog(
            onDismissRequest = { errorText = null },
            title = { Text(stringResource(R.string.module_wallpaper_error_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorText = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

@Composable
private fun VariantSelector(
    variant: ModuleWallpaperVariant,
    onVariantChange: (ModuleWallpaperVariant) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = variant == ModuleWallpaperVariant.Day,
            onClick = { onVariantChange(ModuleWallpaperVariant.Day) },
            label = { Text(stringResource(R.string.module_wallpaper_day)) },
            leadingIcon = if (variant == ModuleWallpaperVariant.Day) {
                { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
            } else null,
        )
        FilterChip(
            selected = variant == ModuleWallpaperVariant.Night,
            onClick = { onVariantChange(ModuleWallpaperVariant.Night) },
            label = { Text(stringResource(R.string.module_wallpaper_night)) },
            leadingIcon = if (variant == ModuleWallpaperVariant.Night) {
                { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
            } else null,
        )
    }
}

@Composable
private fun PreviewRatioSelector(selected: Float, onSelected: (Float) -> Unit) {
    val options = listOf(
        2.4f to R.string.module_wallpaper_preview_compact,
        MODULE_CARD_WALLPAPER_ASPECT_RATIO to R.string.module_wallpaper_preview_standard,
        1.25f to R.string.module_wallpaper_preview_expanded,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (ratio, label) ->
            FilterChip(
                modifier = Modifier.weight(1f),
                selected = selected == ratio,
                onClick = { onSelected(ratio) },
                label = { Text(stringResource(label), maxLines = 1) },
            )
        }
    }
}

@Composable
private fun ModuleWallpaperRealPreview(
    module: Module?,
    displayName: String?,
    displayAuthor: String?,
    displayVersion: String?,
    displayDescription: String?,
    entry: ModuleCardWallpaperEntry?,
    loadState: ModuleWallpaperBitmapLoadState,
    aspectRatio: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
    ) {
        ModuleCardWallpaperBackground(
            bitmap = loadState.bitmap,
            entry = entry,
            contentIsLight = isInDarkTheme(),
        )
        if (loadState.loading) CircularProgressIndicator(Modifier.align(Alignment.Center).size(26.dp))
        if (loadState.failed) {
            Text(
                text = stringResource(R.string.module_wallpaper_load_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = module?.name ?: displayName ?: stringResource(R.string.module_wallpaper_preview_module),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = module?.let { "${it.version} · ${it.author}" }
                        ?: listOfNotNull(displayVersion, displayAuthor).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = module?.description ?: displayDescription.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (aspectRatio < 1.5f) 3 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WallpaperQueue(
    entries: List<ModuleCardWallpaperEntry>,
    selectedIndex: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(92.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.module_wallpaper_empty_queue),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val dragThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(entries, key = { _, entry -> entry.uriString }) { index, entry ->
            val state = rememberModuleCardWallpaperLoadState(entry, maxSide = 260)
            var dragDistance by remember(entry.uriString) { mutableFloatStateOf(0f) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(104.dp)
                        .aspectRatio(MODULE_CARD_WALLPAPER_ASPECT_RATIO)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = if (index == selectedIndex) 2.dp else 1.dp,
                            color = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable(enabled = enabled) { onSelect(index) }
                        .pointerInput(enabled, index, entries.size) {
                            if (!enabled) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragEnd = { dragDistance = 0f },
                                onDragCancel = { dragDistance = 0f },
                            ) { change, amount ->
                                change.consume()
                                dragDistance += amount.x
                                when {
                                    dragDistance > dragThreshold && index < entries.lastIndex -> {
                                        onMove(index, index + 1)
                                        dragDistance = 0f
                                    }
                                    dragDistance < -dragThreshold && index > 0 -> {
                                        onMove(index, index - 1)
                                        dragDistance = 0f
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    state.bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = stringResource(R.string.module_wallpaper_thumbnail, index + 1),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } ?: Icon(Icons.Rounded.Image, null)
                    Icon(
                        Icons.Rounded.DragIndicator,
                        contentDescription = stringResource(R.string.module_wallpaper_drag_reorder),
                        modifier = Modifier.align(Alignment.BottomStart).size(20.dp),
                    )
                    IconButton(
                        enabled = enabled,
                        onClick = { onDelete(index) },
                        modifier = Modifier.align(Alignment.TopEnd).size(34.dp),
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.module_wallpaper_delete_one))
                    }
                }
                Text(
                    text = "${index + 1}/${entries.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VisualSettingsEditor(
    settings: MediaVisualSettings,
    autoContrast: Boolean,
    onSettingsPreview: (MediaVisualSettings) -> Unit,
    onSettingsCommit: (MediaVisualSettings) -> Unit,
    onAutoContrastChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.module_wallpaper_visual_title), style = MaterialTheme.typography.titleMedium)
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_brightness),
            value = settings.brightness,
            range = -0.6f..0.6f,
            onPreview = { onSettingsPreview(settings.copy(brightness = it)) },
            onCommit = { onSettingsCommit(settings.copy(brightness = it)) },
        )
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_dark_overlay),
            value = settings.overlayAlpha,
            range = 0f..0.82f,
            onPreview = { onSettingsPreview(settings.copy(overlayAlpha = it)) },
            onCommit = { onSettingsCommit(settings.copy(overlayAlpha = it)) },
        )
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_blur),
            value = settings.blurRadius,
            range = 0f..28f,
            onPreview = { onSettingsPreview(settings.copy(blurRadius = it)) },
            onCommit = { onSettingsCommit(settings.copy(blurRadius = it)) },
        )
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_saturation),
            value = settings.saturation,
            range = 0f..2f,
            onPreview = { onSettingsPreview(settings.copy(saturation = it)) },
            onCommit = { onSettingsCommit(settings.copy(saturation = it)) },
        )
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_contrast),
            value = settings.contrast,
            range = 0.5f..1.8f,
            onPreview = { onSettingsPreview(settings.copy(contrast = it)) },
            onCommit = { onSettingsCommit(settings.copy(contrast = it)) },
        )
        SettingsSlider(
            label = stringResource(R.string.module_wallpaper_opacity),
            value = settings.opacity,
            range = 0.1f..1f,
            onPreview = { onSettingsPreview(settings.copy(opacity = it)) },
            onCommit = { onSettingsCommit(settings.copy(opacity = it)) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.module_wallpaper_auto_contrast), modifier = Modifier.weight(1f))
            Switch(checked = autoContrast, onCheckedChange = onAutoContrastChange)
        }
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onPreview: (Float) -> Unit,
    onCommit: (Float) -> Unit,
) {
    var localValue by remember(value) { mutableFloatStateOf(value) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (range.endInclusive > 3f) "${localValue.toInt()}" else "%.2f".format(localValue),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = localValue,
            valueRange = range,
            onValueChange = {
                localValue = it
                onPreview(it)
            },
            onValueChangeFinished = { onCommit(localValue) },
        )
    }
}

@Composable
private fun CarouselSettings(
    collection: ModuleWallpaperCollection,
    onChange: (ModuleWallpaperCollection) -> Unit,
) {
    var intervalSeconds by remember(collection.intervalMillis) {
        mutableFloatStateOf(collection.intervalMillis / 1000f)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.module_wallpaper_carousel_settings), style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.module_wallpaper_carousel_enable), modifier = Modifier.weight(1f))
            Switch(
                checked = collection.carouselEnabled,
                onCheckedChange = { onChange(collection.copy(carouselEnabled = it)) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = collection.carouselOrder == ModuleWallpaperCarouselOrder.Sequential,
                onClick = { onChange(collection.copy(carouselOrder = ModuleWallpaperCarouselOrder.Sequential)) },
                label = { Text(stringResource(R.string.module_wallpaper_order_sequential)) },
            )
            FilterChip(
                selected = collection.carouselOrder == ModuleWallpaperCarouselOrder.Random,
                onClick = { onChange(collection.copy(carouselOrder = ModuleWallpaperCarouselOrder.Random)) },
                label = { Text(stringResource(R.string.module_wallpaper_order_random)) },
            )
            FilterChip(
                selected = !collection.carouselEnabled,
                onClick = { onChange(collection.copy(carouselEnabled = false)) },
                label = { Text(stringResource(R.string.module_wallpaper_order_fixed)) },
            )
        }
        Text(stringResource(R.string.module_wallpaper_interval_seconds, intervalSeconds.toInt()))
        Slider(
            value = intervalSeconds,
            valueRange = 3f..60f,
            steps = 56,
            onValueChange = { intervalSeconds = it },
            onValueChangeFinished = {
                onChange(collection.copy(intervalMillis = intervalSeconds.toLong() * 1000L))
            },
        )
    }
}

@Composable
private fun ModuleWallpaperBatchDialog(
    sourceModule: Module?,
    modules: List<Module>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onApply: (List<Module>) -> Unit,
) {
    var mode by remember { mutableStateOf(WallpaperBatchMode.Selected) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val candidates = modules.filterNot { it.id == sourceModule?.id }
    val targets = when (mode) {
        WallpaperBatchMode.Selected -> candidates.filter { it.id in selectedIds }
        WallpaperBatchMode.SameType -> candidates.filter { it.metamodule == sourceModule?.metamodule }
        WallpaperBatchMode.All -> candidates
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.module_wallpaper_batch_apply)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = mode == WallpaperBatchMode.Selected,
                        onClick = { mode = WallpaperBatchMode.Selected },
                        label = { Text(stringResource(R.string.module_wallpaper_batch_selected)) },
                    )
                    FilterChip(
                        selected = mode == WallpaperBatchMode.SameType,
                        onClick = { mode = WallpaperBatchMode.SameType },
                        label = { Text(stringResource(R.string.module_wallpaper_batch_same_type)) },
                    )
                    FilterChip(
                        selected = mode == WallpaperBatchMode.All,
                        onClick = { mode = WallpaperBatchMode.All },
                        label = { Text(stringResource(R.string.module_wallpaper_batch_all)) },
                    )
                }
                if (mode == WallpaperBatchMode.Selected) {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(candidates, key = Module::id) { module ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (module.id in selectedIds) {
                                            selectedIds - module.id
                                        } else {
                                            selectedIds + module.id
                                        }
                                    }
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = module.id in selectedIds,
                                    onCheckedChange = null,
                                )
                                Text(module.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.module_wallpaper_batch_count, targets.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = targets.isNotEmpty() && !busy, onClick = { onApply(targets) }) {
                Text(stringResource(R.string.module_wallpaper_batch_confirm, targets.size))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
