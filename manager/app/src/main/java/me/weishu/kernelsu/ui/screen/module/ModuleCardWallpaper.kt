package me.weishu.kernelsu.ui.screen.module

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.MediaVisualLayer
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.ThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.MediaVisualSettings
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperCrop
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotCrop
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.random.Random
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

internal const val MODULE_CARD_WALLPAPER_ASPECT_RATIO = 1.72f

internal const val MODULE_CARD_WALLPAPER_MAX_SIDE = 1200
internal const val MODULE_CARD_WALLPAPER_MAX_COUNT = 32
internal const val MODULE_CARD_WALLPAPER_MAX_FILE_BYTES = 24L * 1024L * 1024L
internal const val MODULE_CARD_WALLPAPER_DEFAULT_INTERVAL_MILLIS = 5_000L
internal const val MODULE_CARD_WALLPAPER_MIN_INTERVAL_MILLIS = 3_000L
internal const val MODULE_CARD_WALLPAPER_MAX_INTERVAL_MILLIS = 60_000L
private const val MODULE_CARD_WALLPAPER_KEY_PREFIX = "module_card_wallpaper"
private const val MODULE_CARD_WALLPAPER_SCHEMA_VERSION = 2

internal enum class ModuleWallpaperVariant(val value: String) {
    Day("day"),
    Night("night"),
}

internal enum class ModuleWallpaperCarouselOrder(val value: String) {
    Sequential("sequential"),
    Random("random");

    companion object {
        fun fromValue(value: String?): ModuleWallpaperCarouselOrder {
            return entries.firstOrNull { it.value == value } ?: Sequential
        }
    }
}

internal data class ModuleCardWallpaperEntry(
    val uriString: String,
    val crop: CustomWallpaperCrop,
    val visualSettings: MediaVisualSettings = MediaVisualSettings(),
    val autoContrast: Boolean = true,
)

internal data class ModuleWallpaperCollection(
    val entries: List<ModuleCardWallpaperEntry> = emptyList(),
    val carouselEnabled: Boolean = false,
    val carouselOrder: ModuleWallpaperCarouselOrder = ModuleWallpaperCarouselOrder.Sequential,
    val intervalMillis: Long = MODULE_CARD_WALLPAPER_DEFAULT_INTERVAL_MILLIS,
    val selectedIndex: Int = 0,
) {
    fun normalized(): ModuleWallpaperCollection {
        val safeEntries = entries.take(MODULE_CARD_WALLPAPER_MAX_COUNT)
        return copy(
            entries = safeEntries,
            carouselEnabled = carouselEnabled && safeEntries.size > 1,
            intervalMillis = intervalMillis.coerceIn(
                MODULE_CARD_WALLPAPER_MIN_INTERVAL_MILLIS,
                MODULE_CARD_WALLPAPER_MAX_INTERVAL_MILLIS,
            ),
            selectedIndex = selectedIndex.coerceIn(0, safeEntries.lastIndex.coerceAtLeast(0)),
        )
    }
}

internal data class ModuleCardWallpaperSnapshot(
    val entries: List<ModuleCardWallpaperEntry>,
    val carouselEnabled: Boolean,
    val carouselOrder: ModuleWallpaperCarouselOrder = ModuleWallpaperCarouselOrder.Sequential,
    val intervalMillis: Long = MODULE_CARD_WALLPAPER_DEFAULT_INTERVAL_MILLIS,
    val selectedIndex: Int = 0,
    val nightEntries: List<ModuleCardWallpaperEntry> = emptyList(),
    val nightCarouselEnabled: Boolean = false,
    val nightCarouselOrder: ModuleWallpaperCarouselOrder = ModuleWallpaperCarouselOrder.Sequential,
    val nightIntervalMillis: Long = MODULE_CARD_WALLPAPER_DEFAULT_INTERVAL_MILLIS,
    val nightSelectedIndex: Int = 0,
) {
    fun collection(variant: ModuleWallpaperVariant, fallbackToDay: Boolean = false): ModuleWallpaperCollection {
        if (variant == ModuleWallpaperVariant.Night && nightEntries.isEmpty() && fallbackToDay) {
            return collection(ModuleWallpaperVariant.Day)
        }
        return when (variant) {
            ModuleWallpaperVariant.Day -> ModuleWallpaperCollection(
                entries = entries,
                carouselEnabled = carouselEnabled,
                carouselOrder = carouselOrder,
                intervalMillis = intervalMillis,
                selectedIndex = selectedIndex,
            )
            ModuleWallpaperVariant.Night -> ModuleWallpaperCollection(
                entries = nightEntries,
                carouselEnabled = nightCarouselEnabled,
                carouselOrder = nightCarouselOrder,
                intervalMillis = nightIntervalMillis,
                selectedIndex = nightSelectedIndex,
            )
        }.normalized()
    }

    fun withCollection(
        variant: ModuleWallpaperVariant,
        collection: ModuleWallpaperCollection,
    ): ModuleCardWallpaperSnapshot {
        val value = collection.normalized()
        return when (variant) {
            ModuleWallpaperVariant.Day -> copy(
                entries = value.entries,
                carouselEnabled = value.carouselEnabled,
                carouselOrder = value.carouselOrder,
                intervalMillis = value.intervalMillis,
                selectedIndex = value.selectedIndex,
            )
            ModuleWallpaperVariant.Night -> copy(
                nightEntries = value.entries,
                nightCarouselEnabled = value.carouselEnabled,
                nightCarouselOrder = value.carouselOrder,
                nightIntervalMillis = value.intervalMillis,
                nightSelectedIndex = value.selectedIndex,
            )
        }
    }

    fun allEntries(): List<ModuleCardWallpaperEntry> = entries + nightEntries

    fun normalized(): ModuleCardWallpaperSnapshot {
        return ModuleCardWallpaperSnapshot(entries = emptyList(), carouselEnabled = false)
            .withCollection(ModuleWallpaperVariant.Day, collection(ModuleWallpaperVariant.Day))
            .withCollection(ModuleWallpaperVariant.Night, collection(ModuleWallpaperVariant.Night))
    }
}

internal fun readModuleCardWallpaperSnapshot(
    context: Context,
    moduleId: String,
): ModuleCardWallpaperSnapshot {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return readModuleCardWallpaperSnapshot(prefs, moduleId)
}

internal fun replaceModuleCardWallpaperSnapshot(
    context: Context,
    moduleId: String,
    snapshot: ModuleCardWallpaperSnapshot,
): Boolean {
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val previous = readModuleCardWallpaperSnapshot(prefs, moduleId)
    val normalized = snapshot.normalized()
    val editor = prefs.edit().putModuleCardWallpaperSnapshot(moduleId, normalized)
    if (!editor.commit()) return false
    releaseRemovedModuleWallpaperEntries(context, previous, normalized)
    return true
}

internal fun replaceModuleCardWallpaperSnapshots(
    context: Context,
    replacements: Map<String, ModuleCardWallpaperSnapshot>,
): Boolean {
    if (replacements.isEmpty()) return true
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val previous = replacements.keys.associateWith { readModuleCardWallpaperSnapshot(prefs, it) }
    val normalized = replacements.mapValues { it.value.normalized() }
    val editor = prefs.edit()
    normalized.forEach { (moduleId, snapshot) ->
        editor.putModuleCardWallpaperSnapshot(moduleId, snapshot)
    }
    if (!editor.commit()) return false
    normalized.forEach { (moduleId, snapshot) ->
        releaseRemovedModuleWallpaperEntries(context, previous.getValue(moduleId), snapshot)
    }
    return true
}

internal data class ModuleCardWallpaperState(
    val entries: List<ModuleCardWallpaperEntry>,
    val selectedIndex: Int,
    val carouselEnabled: Boolean,
    val carouselOrder: ModuleWallpaperCarouselOrder,
    val intervalMillis: Long,
    val variant: ModuleWallpaperVariant,
    val onPickWallpaper: () -> Unit,
    val onSelectWallpaper: (Int) -> Unit,
    val onToggleCarousel: () -> Unit,
    val onCropChange: (CustomWallpaperCrop) -> Unit,
    val onClearWallpaper: () -> Unit,
    val onSyncThemeStore: () -> Boolean,
) {
    val currentEntry: ModuleCardWallpaperEntry?
        get() = entries.getOrNull(selectedIndex.coerceIn(0, entries.lastIndex.coerceAtLeast(0)))
    val uriString: String?
        get() = currentEntry?.uriString
    val crop: CustomWallpaperCrop
        get() = currentEntry?.crop ?: DEFAULT_CUSTOM_WALLPAPER_CROP
    val hasSelectedWallpaper: Boolean
        get() = entries.isNotEmpty()
    val canPlayCarousel: Boolean
        get() = entries.size > 1
}

@Composable
internal fun rememberModuleCardWallpaperState(
    moduleId: String,
    onWallpaperSelected: () -> Unit = {},
): ModuleCardWallpaperState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnWallpaperSelected by rememberUpdatedState(onWallpaperSelected)
    val prefs = remember(context) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    var snapshot by remember(moduleId) { mutableStateOf(readModuleCardWallpaperSnapshot(prefs, moduleId)) }
    val requestedVariant = if (isInDarkTheme()) ModuleWallpaperVariant.Night else ModuleWallpaperVariant.Day
    val variant = if (requestedVariant == ModuleWallpaperVariant.Night && snapshot.nightEntries.isEmpty()) {
        ModuleWallpaperVariant.Day
    } else {
        requestedVariant
    }
    val collection = snapshot.collection(variant)
    var selectedIndex by remember(moduleId, variant) { mutableIntStateOf(collection.selectedIndex) }

    LifecycleResumeEffect(moduleId) {
        val refreshed = readModuleCardWallpaperSnapshot(context, moduleId)
        snapshot = refreshed
        val refreshedCollection = refreshed.collection(
            if (requestedVariant == ModuleWallpaperVariant.Night && refreshed.nightEntries.isNotEmpty()) {
                ModuleWallpaperVariant.Night
            } else {
                ModuleWallpaperVariant.Day
            }
        )
        selectedIndex = refreshedCollection.selectedIndex
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                importModuleWallpaperEntries(context, moduleId, variant, uris)
            }
            imported.onSuccess { nextEntries ->
                val nextCollection = collection.copy(
                    entries = nextEntries,
                    carouselEnabled = nextEntries.size > 1,
                    selectedIndex = 0,
                )
                val nextSnapshot = snapshot.withCollection(variant, nextCollection)
                if (replaceModuleCardWallpaperSnapshot(context, moduleId, nextSnapshot)) {
                    snapshot = nextSnapshot
                    selectedIndex = 0
                    currentOnWallpaperSelected()
                } else {
                    nextEntries.forEach { releaseCustomImageReference(context, it.uriString) }
                    Toast.makeText(context, R.string.module_wallpaper_save_failed, Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, R.string.module_wallpaper_import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    return remember(moduleId, snapshot, collection, selectedIndex, variant, launcher, context) {
        ModuleCardWallpaperState(
            entries = collection.entries,
            selectedIndex = selectedIndex,
            carouselEnabled = collection.carouselEnabled,
            carouselOrder = collection.carouselOrder,
            intervalMillis = collection.intervalMillis,
            variant = variant,
            onPickWallpaper = {
                launcher.launch(arrayOf("image/*"))
            },
            onSelectWallpaper = { nextIndex ->
                if (collection.entries.isNotEmpty()) {
                    selectedIndex = nextIndex.coerceIn(0, collection.entries.lastIndex)
                }
            },
            onToggleCarousel = {
                if (collection.entries.size > 1) {
                    val nextCollection = collection.copy(carouselEnabled = !collection.carouselEnabled)
                    val nextSnapshot = snapshot.withCollection(variant, nextCollection)
                    if (replaceModuleCardWallpaperSnapshot(context, moduleId, nextSnapshot)) {
                        snapshot = nextSnapshot
                    }
                }
            },
            onCropChange = { nextCrop ->
                val safeCrop = sanitizeCustomWallpaperCrop(nextCrop)
                val safeIndex = selectedIndex.coerceIn(0, collection.entries.lastIndex.coerceAtLeast(0))
                val nextEntries = collection.entries.mapIndexed { index, entry ->
                    if (index == safeIndex) entry.copy(crop = safeCrop) else entry
                }
                val nextSnapshot = snapshot.withCollection(variant, collection.copy(entries = nextEntries))
                if (replaceModuleCardWallpaperSnapshot(context, moduleId, nextSnapshot)) {
                    snapshot = nextSnapshot
                }
            },
            onClearWallpaper = {
                val nextSnapshot = snapshot.withCollection(variant, ModuleWallpaperCollection())
                if (replaceModuleCardWallpaperSnapshot(context, moduleId, nextSnapshot)) {
                    snapshot = nextSnapshot
                    selectedIndex = 0
                }
            },
            onSyncThemeStore = {
                syncModuleWallpaperToThemeStore(context, collection.entries.getOrNull(selectedIndex))
            },
        )
    }
}

@Composable
internal fun rememberModuleCardWallpaperFrame(
    state: ModuleCardWallpaperState,
    paused: Boolean,
): ModuleCardWallpaperEntry? {
    val context = LocalContext.current
    var lifecyclePaused by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        lifecyclePaused = false
        onPauseOrDispose { lifecyclePaused = true }
    }
    val nextIndex = remember(
        state.entries,
        state.selectedIndex,
        state.carouselOrder,
    ) {
        when {
            state.entries.size <= 1 -> state.selectedIndex
            state.carouselOrder == ModuleWallpaperCarouselOrder.Sequential -> {
                (state.selectedIndex + 1) % state.entries.size
            }
            else -> {
                val candidates = state.entries.indices.filterNot { it == state.selectedIndex }
                candidates[Random.nextInt(candidates.size)]
            }
        }
    }
    LaunchedEffect(nextIndex, state.entries) {
        preloadModuleWallpaperBitmap(
            context = context,
            entry = state.entries.getOrNull(nextIndex),
            maxSide = MODULE_CARD_WALLPAPER_MAX_SIDE,
        )
    }
    LaunchedEffect(
        paused,
        lifecyclePaused,
        state.carouselEnabled,
        state.entries.size,
        state.selectedIndex,
        state.intervalMillis,
        nextIndex,
    ) {
        if (!paused && !lifecyclePaused && state.carouselEnabled && state.entries.size > 1) {
            delay(state.intervalMillis)
            state.onSelectWallpaper(nextIndex)
        }
    }
    return state.currentEntry
}

internal data class ModuleWallpaperBitmapLoadState(
    val bitmap: Bitmap? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
)

@Composable
internal fun rememberModuleCardWallpaperLoadState(
    entry: ModuleCardWallpaperEntry?,
    maxSide: Int = MODULE_CARD_WALLPAPER_MAX_SIDE,
): ModuleWallpaperBitmapLoadState {
    val context = LocalContext.current
    var state by remember {
        mutableStateOf(ModuleWallpaperBitmapLoadState(loading = entry != null))
    }
    LaunchedEffect(entry, maxSide, context) {
        if (entry == null) {
            state = ModuleWallpaperBitmapLoadState()
        } else {
            state = state.copy(loading = true, failed = false)
            val bitmap = loadModuleWallpaperBitmapCached(context, entry, maxSide)
            state = ModuleWallpaperBitmapLoadState(
                bitmap = bitmap,
                loading = false,
                failed = bitmap == null,
            )
        }
    }
    return state
}

@Composable
internal fun rememberModuleCardWallpaperBitmap(
    entry: ModuleCardWallpaperEntry?,
): Bitmap? {
    return rememberModuleCardWallpaperLoadState(entry).bitmap
}

@Composable
internal fun rememberModuleCardWallpaperBitmap(
    uriString: String?,
    crop: CustomWallpaperCrop,
): Bitmap? {
    val entry = remember(uriString, crop) {
        uriString?.takeIf(String::isNotBlank)?.let {
            ModuleCardWallpaperEntry(uriString = it, crop = crop)
        }
    }
    return rememberModuleCardWallpaperLoadState(entry).bitmap
}

@Composable
internal fun BoxScope.ModuleCardWallpaperBackground(
    bitmap: Bitmap?,
    entry: ModuleCardWallpaperEntry? = null,
    overlayColor: Color? = null,
    contentIsLight: Boolean = isInDarkTheme(),
) {
    if (bitmap == null) return

    val visualSettings = entry?.visualSettings?.normalized() ?: MediaVisualSettings()
    MediaVisualLayer(
        settings = visualSettings.copy(overlayAlpha = 0f),
        modifier = Modifier.matchParentSize(),
    ) {
        Crossfade(
            targetState = bitmap,
            animationSpec = tween(durationMillis = 180),
            label = "module-wallpaper-crossfade",
            modifier = Modifier.fillMaxSize(),
        ) { target ->
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = remember(target) { target.asImageBitmap() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    }
    if (visualSettings.overlayAlpha > 0f) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = visualSettings.overlayAlpha))
        )
    }
    val resolvedOverlay = overlayColor ?: if (entry?.autoContrast != false) {
        remember(bitmap, contentIsLight) {
            calculateAutomaticContrastOverlay(bitmap, contentIsLight)
        }
    } else {
        null
    }
    if (resolvedOverlay != null) {
        Box(modifier = Modifier.matchParentSize().background(resolvedOverlay))
    } else if (entry == null) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (isInDarkTheme()) Color.Black.copy(alpha = 0.56f)
                    else Color.White.copy(alpha = 0.68f)
                )
        )
    }
}

private fun calculateAutomaticContrastOverlay(bitmap: Bitmap, contentIsLight: Boolean): Color {
    val xStep = max(1, bitmap.width / 8)
    val yStep = max(1, bitmap.height / 8)
    var luminanceTotal = 0.0
    var sampleCount = 0
    var y = yStep / 2
    while (y < bitmap.height) {
        var x = xStep / 2
        while (x < bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            luminanceTotal += android.graphics.Color.red(pixel) / 255.0 * 0.2126
            luminanceTotal += android.graphics.Color.green(pixel) / 255.0 * 0.7152
            luminanceTotal += android.graphics.Color.blue(pixel) / 255.0 * 0.0722
            sampleCount++
            x += xStep
        }
        y += yStep
    }
    val luminance = if (sampleCount > 0) luminanceTotal / sampleCount else 0.5
    return if (contentIsLight) {
        val alpha = ((luminance - 0.16) / luminance.coerceAtLeast(0.01)).coerceIn(0.34, 0.72)
        Color.Black.copy(alpha = alpha.toFloat())
    } else {
        val alpha = ((0.72 - luminance) / (1.0 - luminance).coerceAtLeast(0.01)).coerceIn(0.38, 0.76)
        Color.White.copy(alpha = alpha.toFloat())
    }
}

@Composable
internal fun ModuleCardWallpaperPreviewDialog(
    show: Boolean,
    moduleName: String,
    uriString: String?,
    bitmap: Bitmap?,
    loadFailed: Boolean = false,
    onDismissRequest: () -> Unit,
) {
    if (!show) return

    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    OverlayDialog(
        show = true,
        title = stringResource(R.string.module_wallpaper_preview),
        onDismissRequest = onDismissRequest,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ModuleCardWallpaperPreviewFrame(
                    moduleName = moduleName,
                    imageBitmap = imageBitmap,
                    uriString = uriString,
                    loadFailed = loadFailed,
                )
                MiuixTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(android.R.string.ok),
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}

@Composable
private fun ModuleCardWallpaperPreviewFrame(
    moduleName: String,
    imageBitmap: ImageBitmap?,
    uriString: String?,
    loadFailed: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(MODULE_CARD_WALLPAPER_ASPECT_RATIO)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            imageBitmap != null -> {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isInDarkTheme()) {
                                Color.Black.copy(alpha = 0.56f)
                            } else {
                                Color.White.copy(alpha = 0.68f)
                            }
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = moduleName,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.module_wallpaper_preview_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            uriString.isNullOrBlank() -> Text(
                modifier = Modifier.padding(24.dp),
                text = stringResource(R.string.settings_wallpaper_not_selected),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            loadFailed -> Text(
                modifier = Modifier.padding(24.dp),
                text = stringResource(R.string.module_wallpaper_load_failed),
                color = MaterialTheme.colorScheme.error,
            )

            else -> CircularProgressIndicator()
        }
    }
}

private fun readModuleCardWallpaperCrop(
    prefs: SharedPreferences,
    moduleId: String,
): CustomWallpaperCrop {
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = prefs.getFloat(moduleWallpaperCropLeftKey(moduleId), DEFAULT_CUSTOM_WALLPAPER_CROP.left),
            top = prefs.getFloat(moduleWallpaperCropTopKey(moduleId), DEFAULT_CUSTOM_WALLPAPER_CROP.top),
            right = prefs.getFloat(moduleWallpaperCropRightKey(moduleId), DEFAULT_CUSTOM_WALLPAPER_CROP.right),
            bottom = prefs.getFloat(moduleWallpaperCropBottomKey(moduleId), DEFAULT_CUSTOM_WALLPAPER_CROP.bottom),
        )
    )
}

private fun readModuleCardWallpaperSnapshot(
    prefs: SharedPreferences,
    moduleId: String,
): ModuleCardWallpaperSnapshot {
    val raw = prefs.getString(moduleWallpaperEntriesKey(moduleId), null)
    if (!raw.isNullOrBlank()) {
        runCatching {
            if (raw.trimStart().startsWith("{")) {
                val root = JSONObject(raw)
                require(root.optInt("version", -1) == MODULE_CARD_WALLPAPER_SCHEMA_VERSION)
                return ModuleCardWallpaperSnapshot(entries = emptyList(), carouselEnabled = false)
                    .withCollection(ModuleWallpaperVariant.Day, root.optJSONObject("day").toCollection())
                    .withCollection(ModuleWallpaperVariant.Night, root.optJSONObject("night").toCollection())
            }
            val legacyEntries = JSONArray(raw).toModuleWallpaperEntries()
            if (legacyEntries.isNotEmpty()) {
                return ModuleCardWallpaperSnapshot(
                    entries = legacyEntries,
                    carouselEnabled = prefs.getBoolean(moduleWallpaperCarouselKey(moduleId), false),
                ).normalized()
            }
        }
    }

    val legacyUriString = prefs.getString(moduleWallpaperUriKey(moduleId), null)?.takeIf(String::isNotBlank)
    val entries = legacyUriString?.let {
        listOf(ModuleCardWallpaperEntry(it, readModuleCardWallpaperCrop(prefs, moduleId)))
    }.orEmpty()
    return ModuleCardWallpaperSnapshot(
        entries = entries,
        carouselEnabled = entries.size > 1 && prefs.getBoolean(moduleWallpaperCarouselKey(moduleId), false),
    ).normalized()
}

internal fun SharedPreferences.Editor.putModuleCardWallpaperSnapshot(
    moduleId: String,
    snapshot: ModuleCardWallpaperSnapshot,
): SharedPreferences.Editor {
    val value = snapshot.normalized()
    if (value.allEntries().isEmpty()) {
        removeModuleCardWallpaperEntries(moduleId)
        remove(moduleWallpaperCarouselKey(moduleId))
        return this
    }
    val root = JSONObject()
        .put("version", MODULE_CARD_WALLPAPER_SCHEMA_VERSION)
        .put("day", value.collection(ModuleWallpaperVariant.Day).toJson())
        .put("night", value.collection(ModuleWallpaperVariant.Night).toJson())
    putString(moduleWallpaperEntriesKey(moduleId), root.toString())
    value.entries.firstOrNull()?.let { first ->
        putString(moduleWallpaperUriKey(moduleId), first.uriString)
        putModuleCardWallpaperCrop(moduleId, first.crop)
    } ?: run {
        remove(moduleWallpaperUriKey(moduleId))
        removeModuleCardWallpaperCrop(moduleId)
    }
    putBoolean(moduleWallpaperCarouselKey(moduleId), value.carouselEnabled)
    return this
}

private fun SharedPreferences.Editor.removeModuleCardWallpaperEntries(moduleId: String) {
    remove(moduleWallpaperEntriesKey(moduleId))
    remove(moduleWallpaperUriKey(moduleId))
    removeModuleCardWallpaperCrop(moduleId)
}

internal fun importModuleWallpaperEntries(
    context: Context,
    moduleId: String,
    variant: ModuleWallpaperVariant,
    uris: List<Uri>,
): Result<List<ModuleCardWallpaperEntry>> = runCatching {
    val uniqueUris = uris.distinctBy(Uri::toString)
    require(uniqueUris.isNotEmpty()) { "No image was selected" }
    require(uniqueUris.size <= MODULE_CARD_WALLPAPER_MAX_COUNT) {
        "At most $MODULE_CARD_WALLPAPER_MAX_COUNT images can be selected"
    }
    val imported = mutableListOf<ModuleCardWallpaperEntry>()
    try {
        uniqueUris.forEachIndexed { index, uri ->
            val persisted = persistCustomImageReference(
                context = context,
                sourceUri = uri,
                storageKey = uniqueModuleWallpaperStorageKey(
                    moduleWallpaperEntryStorageKey(moduleId, variant, index)
                ),
                maxBytes = MODULE_CARD_WALLPAPER_MAX_FILE_BYTES,
            ) ?: error("Unable to copy image ${index + 1}")
            val file = Uri.parse(persisted).path?.let(::File)
                ?: error("Invalid copied image ${index + 1}")
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                releaseCustomImageReference(context, persisted)
                error("Image ${index + 1} cannot be decoded")
            }
            imported += ModuleCardWallpaperEntry(
                uriString = persisted,
                crop = DEFAULT_CUSTOM_WALLPAPER_CROP,
            )
        }
        imported
    } catch (error: Throwable) {
        imported.forEach { releaseCustomImageReference(context, it.uriString) }
        throw error
    }
}

internal fun duplicateModuleWallpaperSnapshotForTarget(
    context: Context,
    targetModuleId: String,
    source: ModuleCardWallpaperSnapshot,
): Result<ModuleCardWallpaperSnapshot> = runCatching {
    val copiedUris = mutableListOf<String>()
    fun copyCollection(
        variant: ModuleWallpaperVariant,
        collection: ModuleWallpaperCollection,
    ): ModuleWallpaperCollection {
        val copiedEntries = collection.entries.mapIndexed { index, entry ->
            val copied = persistCustomImageReference(
                context = context,
                sourceUri = Uri.parse(entry.uriString),
                storageKey = uniqueModuleWallpaperStorageKey(
                    moduleWallpaperEntryStorageKey(targetModuleId, variant, index)
                ),
                maxBytes = MODULE_CARD_WALLPAPER_MAX_FILE_BYTES,
            ) ?: error("Unable to copy module wallpaper ${index + 1}")
            copiedUris += copied
            entry.copy(uriString = copied)
        }
        return collection.copy(entries = copiedEntries).normalized()
    }
    try {
        ModuleCardWallpaperSnapshot(entries = emptyList(), carouselEnabled = false)
            .withCollection(
                ModuleWallpaperVariant.Day,
                copyCollection(ModuleWallpaperVariant.Day, source.collection(ModuleWallpaperVariant.Day)),
            )
            .withCollection(
                ModuleWallpaperVariant.Night,
                copyCollection(ModuleWallpaperVariant.Night, source.collection(ModuleWallpaperVariant.Night)),
            )
    } catch (error: Throwable) {
        copiedUris.forEach { releaseCustomImageReference(context, it) }
        throw error
    }
}

internal fun releaseModuleWallpaperSnapshot(
    context: Context,
    snapshot: ModuleCardWallpaperSnapshot,
) {
    snapshot.allEntries().map(ModuleCardWallpaperEntry::uriString).distinct().forEach {
        releaseCustomImageReference(context, it)
    }
}

internal fun releaseRemovedModuleWallpaperEntries(
    context: Context,
    previous: ModuleCardWallpaperSnapshot,
    next: ModuleCardWallpaperSnapshot,
) {
    val retainedUris = next.allEntries().mapTo(hashSetOf(), ModuleCardWallpaperEntry::uriString)
    previous.allEntries()
        .asSequence()
        .map(ModuleCardWallpaperEntry::uriString)
        .distinct()
        .filterNot(retainedUris::contains)
        .forEach { releaseCustomImageReference(context, it) }
}

private fun ModuleWallpaperCollection.toJson(): JSONObject {
    val value = normalized()
    return JSONObject()
        .put("entries", value.entries.toJson())
        .put("carouselEnabled", value.carouselEnabled)
        .put("carouselOrder", value.carouselOrder.value)
        .put("intervalMillis", value.intervalMillis)
        .put("selectedIndex", value.selectedIndex)
}

private fun JSONObject?.toCollection(): ModuleWallpaperCollection {
    if (this == null) return ModuleWallpaperCollection()
    return ModuleWallpaperCollection(
        entries = optJSONArray("entries").toModuleWallpaperEntries(),
        carouselEnabled = optBoolean("carouselEnabled", false),
        carouselOrder = ModuleWallpaperCarouselOrder.fromValue(optString("carouselOrder")),
        intervalMillis = optLong("intervalMillis", MODULE_CARD_WALLPAPER_DEFAULT_INTERVAL_MILLIS),
        selectedIndex = optInt("selectedIndex", 0),
    ).normalized()
}

private fun List<ModuleCardWallpaperEntry>.toJson(): JSONArray {
    return JSONArray().also { array ->
        take(MODULE_CARD_WALLPAPER_MAX_COUNT).forEach { entry ->
            array.put(
                JSONObject()
                    .put("uri", entry.uriString)
                    .put("crop", sanitizeCustomWallpaperCrop(entry.crop).toJson())
                    .put("visualSettings", entry.visualSettings.toJson())
                    .put("autoContrast", entry.autoContrast)
            )
        }
    }
}

private fun JSONArray?.toModuleWallpaperEntries(): List<ModuleCardWallpaperEntry> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length().coerceAtMost(MODULE_CARD_WALLPAPER_MAX_COUNT)) {
            val item = optJSONObject(index) ?: continue
            val uriString = item.optString("uri").takeIf(String::isNotBlank) ?: continue
            add(
                ModuleCardWallpaperEntry(
                    uriString = uriString,
                    crop = item.optCrop(DEFAULT_CUSTOM_WALLPAPER_CROP),
                    visualSettings = MediaVisualSettings.fromJson(item.optJSONObject("visualSettings")),
                    autoContrast = item.optBoolean("autoContrast", true),
                )
            )
        }
    }
}

private fun SharedPreferences.Editor.putModuleCardWallpaperCrop(
    moduleId: String,
    crop: CustomWallpaperCrop,
) {
    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    putFloat(moduleWallpaperCropLeftKey(moduleId), safeCrop.left)
    putFloat(moduleWallpaperCropTopKey(moduleId), safeCrop.top)
    putFloat(moduleWallpaperCropRightKey(moduleId), safeCrop.right)
    putFloat(moduleWallpaperCropBottomKey(moduleId), safeCrop.bottom)
}

private fun SharedPreferences.Editor.removeModuleCardWallpaperCrop(moduleId: String) {
    remove(moduleWallpaperCropLeftKey(moduleId))
    remove(moduleWallpaperCropTopKey(moduleId))
    remove(moduleWallpaperCropRightKey(moduleId))
    remove(moduleWallpaperCropBottomKey(moduleId))
}

private fun syncModuleWallpaperToThemeStore(
    context: Context,
    entry: ModuleCardWallpaperEntry?,
): Boolean {
    entry ?: return false
    return runCatching {
        val copiedUriString = persistCustomImageReference(
            context = context,
            sourceUri = Uri.parse(entry.uriString),
            storageKey = ThemeStoreImageSlot.Module.uriKey,
        ) ?: return false
        setThemeStoreImageSlot(context, ThemeStoreImageSlot.Module, copiedUriString)
        setThemeStoreImageSlotCrop(context, ThemeStoreImageSlot.Module, entry.crop)
        true
    }.getOrDefault(false)
}

private fun CustomWallpaperCrop.toJson(): JSONObject {
    return JSONObject()
        .put("left", left)
        .put("top", top)
        .put("right", right)
        .put("bottom", bottom)
}

private fun JSONObject.optCrop(fallback: CustomWallpaperCrop): CustomWallpaperCrop {
    val cropJson = optJSONObject("crop") ?: return fallback
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = cropJson.optDouble("left", fallback.left.toDouble()).toFloat(),
            top = cropJson.optDouble("top", fallback.top.toDouble()).toFloat(),
            right = cropJson.optDouble("right", fallback.right.toDouble()).toFloat(),
            bottom = cropJson.optDouble("bottom", fallback.bottom.toDouble()).toFloat(),
        )
    )
}

private fun moduleWallpaperUriKey(moduleId: String): String {
    return "${MODULE_CARD_WALLPAPER_KEY_PREFIX}_${moduleId}_uri"
}

private fun moduleWallpaperEntryStorageKey(
    moduleId: String,
    variant: ModuleWallpaperVariant,
    index: Int,
): String {
    return "${MODULE_CARD_WALLPAPER_KEY_PREFIX}_${moduleId}_${variant.value}_entry_${index}_uri"
}

internal fun uniqueModuleWallpaperStorageKey(base: String): String {
    return "${base}_${UUID.randomUUID()}"
}

private fun moduleWallpaperEntriesKey(moduleId: String): String {
    return "${MODULE_CARD_WALLPAPER_KEY_PREFIX}_${moduleId}_entries"
}

private fun moduleWallpaperCarouselKey(moduleId: String): String {
    return "${MODULE_CARD_WALLPAPER_KEY_PREFIX}_${moduleId}_carousel"
}

private fun moduleWallpaperCropLeftKey(moduleId: String): String {
    return "${MODULE_CARD_WALLPAPER_KEY_PREFIX}_${moduleId}_crop_left"
}

private fun moduleWallpaperCropTopKey(moduleId: String): String {
    return "${MODULE_CARD_WALLPAPER_KEY_PREFIX}_${moduleId}_crop_top"
}

private fun moduleWallpaperCropRightKey(moduleId: String): String {
    return "${MODULE_CARD_WALLPAPER_KEY_PREFIX}_${moduleId}_crop_right"
}

private fun moduleWallpaperCropBottomKey(moduleId: String): String {
    return "${MODULE_CARD_WALLPAPER_KEY_PREFIX}_${moduleId}_crop_bottom"
}
