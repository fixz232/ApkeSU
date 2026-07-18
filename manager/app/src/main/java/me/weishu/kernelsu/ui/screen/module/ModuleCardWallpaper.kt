package me.weishu.kernelsu.ui.screen.module

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.ThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperCrop
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotCrop
import me.weishu.kernelsu.ui.util.takePersistableImageReadPermission
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

internal const val MODULE_CARD_WALLPAPER_ASPECT_RATIO = 1.72f

private const val MODULE_CARD_WALLPAPER_MAX_SIDE = 1200
private const val MODULE_CARD_WALLPAPER_CAROUSEL_INTERVAL_MILLIS = 5_000L
private const val MODULE_CARD_WALLPAPER_KEY_PREFIX = "module_card_wallpaper"

internal data class ModuleCardWallpaperEntry(
    val uriString: String,
    val crop: CustomWallpaperCrop,
)

internal data class ModuleCardWallpaperState(
    val entries: List<ModuleCardWallpaperEntry>,
    val selectedIndex: Int,
    val carouselEnabled: Boolean,
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
    val currentOnWallpaperSelected by rememberUpdatedState(onWallpaperSelected)
    val prefs = remember(context) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    var entries by remember(moduleId) {
        mutableStateOf(readModuleCardWallpaperEntries(prefs, moduleId))
    }
    var selectedIndex by remember(moduleId) {
        mutableIntStateOf(0)
    }
    var carouselEnabled by remember(moduleId) {
        mutableStateOf(prefs.getBoolean(moduleWallpaperCarouselKey(moduleId), false) && entries.size > 1)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val uniqueUris = uris.distinctBy { it.toString() }
        val defaultCrop = DEFAULT_CUSTOM_WALLPAPER_CROP
        val nextEntries = uniqueUris.mapIndexed { index, uri ->
            val storageKey = moduleWallpaperEntryStorageKey(moduleId, index)
            val nextUriString = persistCustomImageReference(context, uri, storageKey)
                ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
            ModuleCardWallpaperEntry(nextUriString, defaultCrop)
        }
        entries.forEach { releaseCustomImageReference(context, it.uriString) }
        entries = nextEntries
        selectedIndex = 0
        carouselEnabled = nextEntries.size > 1
        prefs.edit(commit = true) {
            putModuleCardWallpaperEntries(moduleId, nextEntries)
            putBoolean(moduleWallpaperCarouselKey(moduleId), carouselEnabled)
        }
        currentOnWallpaperSelected()
    }

    return remember(moduleId, entries, selectedIndex, carouselEnabled, launcher, prefs, context) {
        ModuleCardWallpaperState(
            entries = entries,
            selectedIndex = selectedIndex,
            carouselEnabled = carouselEnabled && entries.size > 1,
            onPickWallpaper = {
                launcher.launch(arrayOf("image/*"))
            },
            onSelectWallpaper = { nextIndex ->
                if (entries.isNotEmpty()) {
                    selectedIndex = nextIndex.coerceIn(0, entries.lastIndex)
                }
            },
            onToggleCarousel = {
                if (entries.size > 1) {
                    carouselEnabled = !carouselEnabled
                    prefs.edit(commit = true) {
                        putBoolean(moduleWallpaperCarouselKey(moduleId), carouselEnabled)
                    }
                }
            },
            onCropChange = { nextCrop ->
                val safeCrop = sanitizeCustomWallpaperCrop(nextCrop)
                val safeIndex = selectedIndex.coerceIn(0, entries.lastIndex.coerceAtLeast(0))
                val nextEntries = entries.mapIndexed { index, entry ->
                    if (index == safeIndex) entry.copy(crop = safeCrop) else entry
                }
                entries = nextEntries
                prefs.edit(commit = true) {
                    putModuleCardWallpaperEntries(moduleId, nextEntries)
                }
            },
            onClearWallpaper = {
                entries.forEach { releaseCustomImageReference(context, it.uriString) }
                entries = emptyList()
                selectedIndex = 0
                carouselEnabled = false
                prefs.edit(commit = true) {
                    removeModuleCardWallpaperEntries(moduleId)
                    remove(moduleWallpaperCarouselKey(moduleId))
                }
            },
            onSyncThemeStore = {
                syncModuleWallpaperToThemeStore(context, entries.getOrNull(selectedIndex))
            },
        )
    }
}

@Composable
internal fun rememberModuleCardWallpaperFrame(
    state: ModuleCardWallpaperState,
    paused: Boolean,
): ModuleCardWallpaperEntry? {
    LaunchedEffect(paused, state.carouselEnabled, state.entries.size, state.selectedIndex) {
        if (!paused && state.carouselEnabled && state.entries.size > 1) {
            delay(MODULE_CARD_WALLPAPER_CAROUSEL_INTERVAL_MILLIS)
            state.onSelectWallpaper((state.selectedIndex + 1) % state.entries.size)
        }
    }
    return state.currentEntry
}

@Composable
internal fun rememberModuleCardWallpaperBitmap(
    entry: ModuleCardWallpaperEntry?,
): Bitmap? {
    return rememberModuleCardWallpaperBitmap(
        uriString = entry?.uriString,
        crop = entry?.crop ?: DEFAULT_CUSTOM_WALLPAPER_CROP,
    )
}

@Composable
internal fun rememberModuleCardWallpaperBitmap(
    uriString: String?,
    crop: CustomWallpaperCrop,
): Bitmap? {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, uriString, crop, context) {
        value = if (uriString.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                loadCustomImageBitmap(
                    context = context,
                    uriString = uriString,
                    maxSide = MODULE_CARD_WALLPAPER_MAX_SIDE,
                    crop = crop,
                )
            }
        }
    }
    return bitmapState.value
}

@Composable
internal fun BoxScope.ModuleCardWallpaperBackground(
    bitmap: Bitmap?,
    overlayColor: Color? = null,
) {
    if (bitmap == null) return

    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
        modifier = Modifier.matchParentSize(),
        bitmap = imageBitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                overlayColor ?: if (isInDarkTheme()) {
                    Color.Black.copy(alpha = 0.56f)
                } else {
                    Color.White.copy(alpha = 0.68f)
                }
            )
    )
}

@Composable
internal fun ModuleCardWallpaperPreviewDialog(
    show: Boolean,
    moduleName: String,
    uriString: String?,
    bitmap: Bitmap?,
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

private fun readModuleCardWallpaperEntries(
    prefs: SharedPreferences,
    moduleId: String,
): List<ModuleCardWallpaperEntry> {
    val entriesJson = prefs.getString(moduleWallpaperEntriesKey(moduleId), null)
    if (!entriesJson.isNullOrBlank()) {
        val parsedEntries = runCatching {
            val array = JSONArray(entriesJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val uriString = item.optString("uri").takeIf { it.isNotBlank() } ?: continue
                    add(
                        ModuleCardWallpaperEntry(
                            uriString = uriString,
                            crop = item.optCrop(DEFAULT_CUSTOM_WALLPAPER_CROP),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
        if (parsedEntries.isNotEmpty()) return parsedEntries
    }

    val legacyUriString = prefs.getString(moduleWallpaperUriKey(moduleId), null)
        ?.takeIf { it.isNotBlank() }
        ?: return emptyList()
    return listOf(
        ModuleCardWallpaperEntry(
            uriString = legacyUriString,
            crop = readModuleCardWallpaperCrop(prefs, moduleId),
        )
    )
}

private fun SharedPreferences.Editor.putModuleCardWallpaperEntries(
    moduleId: String,
    entries: List<ModuleCardWallpaperEntry>,
) {
    if (entries.isEmpty()) {
        removeModuleCardWallpaperEntries(moduleId)
        return
    }

    val array = JSONArray()
    entries.forEach { entry ->
        val safeCrop = sanitizeCustomWallpaperCrop(entry.crop)
        array.put(
            JSONObject()
                .put("uri", entry.uriString)
                .put("crop", safeCrop.toJson())
        )
    }
    putString(moduleWallpaperEntriesKey(moduleId), array.toString())
    putString(moduleWallpaperUriKey(moduleId), entries.first().uriString)
    putModuleCardWallpaperCrop(moduleId, entries.first().crop)
}

private fun SharedPreferences.Editor.removeModuleCardWallpaperEntries(moduleId: String) {
    remove(moduleWallpaperEntriesKey(moduleId))
    remove(moduleWallpaperUriKey(moduleId))
    removeModuleCardWallpaperCrop(moduleId)
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

private fun moduleWallpaperEntryStorageKey(moduleId: String, index: Int): String {
    return "${MODULE_CARD_WALLPAPER_KEY_PREFIX}_${moduleId}_entry_${index}_uri"
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
