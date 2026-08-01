package me.weishu.kernelsu.ui.screen.home

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.CustomVideoBackground
import me.weishu.kernelsu.ui.component.MediaVisualLayer
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.HomeLayoutWallpaperFit
import me.weishu.kernelsu.ui.util.MediaVariantSettings
import me.weishu.kernelsu.ui.util.MediaVisualSettings
import me.weishu.kernelsu.ui.util.ResponsiveCropSet
import me.weishu.kernelsu.ui.util.ThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.generateResponsiveCrops
import me.weishu.kernelsu.ui.util.inspectMediaFile
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.component.preloadCustomImageBitmap
import me.weishu.kernelsu.ui.component.rememberCustomImageAndroidBitmap
import me.weishu.kernelsu.ui.util.readThemeStoreImageState
import me.weishu.kernelsu.ui.util.releasePersistableVideoBackgroundReadPermission
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotCrop
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotNightCrop
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotNightMedia
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotResponsiveCrops
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotVariantSettings
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotVideo
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotVisualSettings
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperCrop
import me.weishu.kernelsu.ui.util.takePersistableImageReadPermission
import me.weishu.kernelsu.ui.util.takePersistableVideoBackgroundReadPermission

internal const val HOME_METRIC_CARD_WALLPAPER_ASPECT_RATIO = 1.72f

private const val HOME_LKM_CARD_WALLPAPER_ASPECT_RATIO = 1.08f
private const val HOME_CLASSIC_MIUIX_LKM_CARD_WALLPAPER_ASPECT_RATIO = 0.84f
private const val HOME_MATERIAL_LKM_CARD_WALLPAPER_ASPECT_RATIO = 3.4f
private const val HOME_METRIC_CARD_WALLPAPER_MAX_SIDE = 1200
private const val HOME_REBOOT_MENU_WALLPAPER_ASPECT_RATIO = 0.72f

internal enum class HomeMetricCardWallpaperTarget(
    private val keyPrefix: String,
    @StringRes val titleRes: Int,
    val aspectRatio: Float,
    @StringRes val pickLabelRes: Int,
    @StringRes val cropLabelRes: Int,
    @StringRes val previewLabelRes: Int,
    @StringRes val clearLabelRes: Int,
) {
    Lkm(
        keyPrefix = "home_lkm_card_wallpaper",
        titleRes = R.string.home_card_main,
        aspectRatio = HOME_LKM_CARD_WALLPAPER_ASPECT_RATIO,
        pickLabelRes = R.string.home_lkm_wallpaper_pick,
        cropLabelRes = R.string.home_lkm_wallpaper_crop,
        previewLabelRes = R.string.home_lkm_wallpaper_preview,
        clearLabelRes = R.string.home_lkm_wallpaper_clear,
    ),
    ClassicMiuixLkm(
        keyPrefix = "home_classic_miuix_lkm_card_wallpaper",
        titleRes = R.string.home_card_classic_miuix_lkm,
        aspectRatio = HOME_CLASSIC_MIUIX_LKM_CARD_WALLPAPER_ASPECT_RATIO,
        pickLabelRes = R.string.home_lkm_wallpaper_pick,
        cropLabelRes = R.string.home_lkm_wallpaper_crop,
        previewLabelRes = R.string.home_lkm_wallpaper_preview,
        clearLabelRes = R.string.home_lkm_wallpaper_clear,
    ),
    MaterialLkm(
        keyPrefix = "home_material_lkm_card_wallpaper",
        titleRes = R.string.home_card_material_lkm,
        aspectRatio = HOME_MATERIAL_LKM_CARD_WALLPAPER_ASPECT_RATIO,
        pickLabelRes = R.string.home_lkm_wallpaper_pick,
        cropLabelRes = R.string.home_lkm_wallpaper_crop,
        previewLabelRes = R.string.home_lkm_wallpaper_preview,
        clearLabelRes = R.string.home_lkm_wallpaper_clear,
    ),
    Superuser(
        keyPrefix = "home_superuser_card_wallpaper",
        titleRes = R.string.home_card_superuser,
        aspectRatio = HOME_METRIC_CARD_WALLPAPER_ASPECT_RATIO,
        pickLabelRes = R.string.home_superuser_wallpaper_pick,
        cropLabelRes = R.string.home_superuser_wallpaper_crop,
        previewLabelRes = R.string.home_superuser_wallpaper_preview,
        clearLabelRes = R.string.home_superuser_wallpaper_clear,
    ),
    Module(
        keyPrefix = "home_module_card_wallpaper",
        titleRes = R.string.home_card_module,
        aspectRatio = HOME_METRIC_CARD_WALLPAPER_ASPECT_RATIO,
        pickLabelRes = R.string.home_module_wallpaper_pick,
        cropLabelRes = R.string.home_module_wallpaper_crop,
        previewLabelRes = R.string.home_module_wallpaper_preview,
        clearLabelRes = R.string.home_module_wallpaper_clear,
    ),
    StatusMonitor(
        keyPrefix = "home_status_monitor_wallpaper",
        titleRes = R.string.home_card_status_monitor,
        aspectRatio = 2.72f,
        pickLabelRes = R.string.home_status_monitor_wallpaper_pick,
        cropLabelRes = R.string.home_status_monitor_wallpaper_crop,
        previewLabelRes = R.string.home_status_monitor_wallpaper_preview,
        clearLabelRes = R.string.home_status_monitor_wallpaper_clear,
    ),
    SystemInfo(
        keyPrefix = "home_system_info_wallpaper",
        titleRes = R.string.home_card_system_info,
        aspectRatio = 1.36f,
        pickLabelRes = R.string.home_system_info_wallpaper_pick,
        cropLabelRes = R.string.home_system_info_wallpaper_crop,
        previewLabelRes = R.string.home_system_info_wallpaper_preview,
        clearLabelRes = R.string.home_system_info_wallpaper_clear,
    ),
    RebootMenu(
        keyPrefix = "home_reboot_menu_wallpaper",
        titleRes = R.string.home_card_reboot_menu,
        aspectRatio = HOME_REBOOT_MENU_WALLPAPER_ASPECT_RATIO,
        pickLabelRes = R.string.home_reboot_menu_wallpaper_pick,
        cropLabelRes = R.string.home_reboot_menu_wallpaper_crop,
        previewLabelRes = R.string.home_reboot_menu_wallpaper_preview,
        clearLabelRes = R.string.home_reboot_menu_wallpaper_clear,
    );

    val uriKey: String get() = "${keyPrefix}_uri"
    val videoUriKey: String get() = "${keyPrefix}_video_uri"
    val cropLeftKey: String get() = "${keyPrefix}_crop_left"
    val cropTopKey: String get() = "${keyPrefix}_crop_top"
    val cropRightKey: String get() = "${keyPrefix}_crop_right"
    val cropBottomKey: String get() = "${keyPrefix}_crop_bottom"

    val preferenceKeys: Set<String>
        get() = setOf(uriKey, videoUriKey, cropLeftKey, cropTopKey, cropRightKey, cropBottomKey)

    val themeSlot: ThemeStoreImageSlot
        get() = when (this) {
            Lkm -> ThemeStoreImageSlot.Lkm
            ClassicMiuixLkm -> ThemeStoreImageSlot.ClassicMiuixLkm
            MaterialLkm -> ThemeStoreImageSlot.MaterialLkm
            Superuser -> ThemeStoreImageSlot.Superuser
            Module -> ThemeStoreImageSlot.Module
            StatusMonitor -> ThemeStoreImageSlot.StatusMonitor
            SystemInfo -> ThemeStoreImageSlot.SystemInfo
            RebootMenu -> ThemeStoreImageSlot.RebootMenu
        }
}

internal data class HomeMetricCardWallpaperState(
    val uriString: String?,
    val videoUriString: String?,
    val crop: CustomWallpaperCrop,
    val visualSettings: MediaVisualSettings,
    val responsiveCrops: ResponsiveCropSet,
    val dayUriString: String?,
    val dayVideoUriString: String?,
    val nightUriString: String?,
    val nightVideoUriString: String?,
    val nightSelected: Boolean,
    val variantSettings: MediaVariantSettings,
    val onPickWallpaper: () -> Unit,
    val onPickVideoWallpaper: () -> Unit,
    val onCropChange: (CustomWallpaperCrop) -> Unit,
    val onVisualSettingsChange: (MediaVisualSettings) -> Unit,
    val onResponsiveCropsChange: (ResponsiveCropSet) -> Unit,
    val onVariantSettingsChange: (MediaVariantSettings) -> Unit,
    val onClearWallpaper: () -> Unit,
) {
    val hasSelectedWallpaper: Boolean
        get() = !uriString.isNullOrBlank()
    val hasSelectedVideoWallpaper: Boolean
        get() = !videoUriString.isNullOrBlank()
    val hasSelectedAnyWallpaper: Boolean
        get() = hasSelectedWallpaper || hasSelectedVideoWallpaper
}

internal fun hasHomeMetricCardWallpaperImage(
    context: Context,
    target: HomeMetricCardWallpaperTarget,
): Boolean {
    val storedState = readThemeStoreImageState(context, target.themeSlot)
    return !storedState.uriString.isNullOrBlank() || !storedState.nightUriString.isNullOrBlank()
}

internal suspend fun preloadHomeMetricCardWallpaperImages(
    context: Context,
    target: HomeMetricCardWallpaperTarget,
) {
    val storedState = readThemeStoreImageState(context, target.themeSlot)
    val requests = buildList {
        storedState.uriString?.takeIf(String::isNotBlank)?.let { uriString ->
            add(uriString to storedState.responsiveCrops.forAspectRatio(target.aspectRatio))
        }
        storedState.nightUriString?.takeIf(String::isNotBlank)?.let { uriString ->
            add(uriString to storedState.nightResponsiveCrops.forAspectRatio(target.aspectRatio))
        }
    }.distinct()
    coroutineScope {
        requests.map { (uriString, crop) ->
            async {
                preloadCustomImageBitmap(
                    context = context,
                    uriString = uriString,
                    maxSide = HOME_METRIC_CARD_WALLPAPER_MAX_SIDE,
                    crop = crop,
                )
            }
        }.awaitAll()
    }
}

@Composable
internal fun rememberHomeMetricCardWallpaperState(
    target: HomeMetricCardWallpaperTarget,
    onWallpaperSelected: () -> Unit,
    forceNight: Boolean? = null,
): HomeMetricCardWallpaperState {
    val context = LocalContext.current
    val currentOnWallpaperSelected by rememberUpdatedState(onWallpaperSelected)
    val scope = rememberCoroutineScope()
    val darkTheme = isInDarkTheme()
    val prefs = remember(context) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    val slot = target.themeSlot
    var storedState by remember(target) { mutableStateOf(readThemeStoreImageState(context, slot)) }
    val nowMillis by produceState(System.currentTimeMillis(), storedState.variantSettings) {
        while (isActive) {
            value = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    DisposableEffect(prefs, target) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key !in slot.preferenceKeys) return@OnSharedPreferenceChangeListener
            storedState = readThemeStoreImageState(context, slot)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    val scheduled = storedState.activeVariant(darkTheme, nowMillis, slot.id.hashCode())
    val selectedNight = forceNight ?: (
        storedState.hasNightSelected && scheduled.uriString == storedState.nightUriString &&
            scheduled.videoUriString == storedState.nightVideoUriString
        )
    val active = if (selectedNight) {
        me.weishu.kernelsu.ui.util.ActiveMediaVariant(
            uriString = storedState.nightUriString,
            videoUriString = storedState.nightVideoUriString,
            crop = storedState.nightCrop,
            responsiveCrops = storedState.nightResponsiveCrops,
            visualSettings = storedState.nightVisualSettings,
        )
    } else {
        me.weishu.kernelsu.ui.util.ActiveMediaVariant(
            uriString = storedState.uriString,
            videoUriString = storedState.videoUriString,
            crop = storedState.crop,
            responsiveCrops = storedState.responsiveCrops,
            visualSettings = storedState.visualSettings,
        )
    }

    fun updateResponsiveCrops(uriString: String, night: Boolean) {
        scope.launch {
            val info = inspectMediaFile(context, uriString)
            val width = info?.width ?: return@launch
            val height = info.height ?: return@launch
            setThemeStoreImageSlotResponsiveCrops(
                context,
                slot,
                generateResponsiveCrops(width, height),
                night = night,
            )
        }
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val storageKey = if (selectedNight) slot.nightUriKey else slot.uriKey
        val nextUriString = persistCustomImageReference(context, uri, storageKey)
            ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
        if (selectedNight) {
            setThemeStoreImageSlotNightMedia(context, slot, nextUriString, video = false)
        } else {
            setThemeStoreImageSlot(context, slot, nextUriString)
        }
        updateResponsiveCrops(nextUriString, selectedNight)
        currentOnWallpaperSelected()
    }
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val nextUriString = uri.toString()
        takePersistableVideoBackgroundReadPermission(context, uri)
        if (selectedNight) {
            setThemeStoreImageSlotNightMedia(context, slot, nextUriString, video = true)
        } else {
            setThemeStoreImageSlotVideo(context, slot, nextUriString)
        }
        updateResponsiveCrops(nextUriString, selectedNight)
        currentOnWallpaperSelected()
    }

    return remember(target, storedState, active, selectedNight, launcher, videoLauncher, context, forceNight) {
        HomeMetricCardWallpaperState(
            uriString = active.uriString,
            videoUriString = active.videoUriString,
            crop = active.responsiveCrops.forAspectRatio(target.aspectRatio),
            visualSettings = active.visualSettings,
            responsiveCrops = active.responsiveCrops,
            dayUriString = storedState.uriString,
            dayVideoUriString = storedState.videoUriString,
            nightUriString = storedState.nightUriString,
            nightVideoUriString = storedState.nightVideoUriString,
            nightSelected = selectedNight,
            variantSettings = storedState.variantSettings,
            onPickWallpaper = {
                launcher.launch(arrayOf("image/*"))
            },
            onPickVideoWallpaper = {
                videoLauncher.launch(arrayOf("video/*"))
            },
            onCropChange = { nextCrop ->
                val safeCrop = sanitizeCustomWallpaperCrop(nextCrop)
                if (selectedNight) {
                    setThemeStoreImageSlotNightCrop(context, slot, safeCrop)
                } else {
                    setThemeStoreImageSlotCrop(context, slot, safeCrop)
                }
                setThemeStoreImageSlotResponsiveCrops(
                    context = context,
                    slot = slot,
                    crops = active.responsiveCrops.withCropForAspectRatio(target.aspectRatio, safeCrop),
                    night = selectedNight,
                )
            },
            onVisualSettingsChange = { settings ->
                setThemeStoreImageSlotVisualSettings(context, slot, settings, night = selectedNight)
            },
            onResponsiveCropsChange = { crops ->
                setThemeStoreImageSlotResponsiveCrops(context, slot, crops, night = selectedNight)
            },
            onVariantSettingsChange = { settings ->
                setThemeStoreImageSlotVariantSettings(context, slot, settings)
            },
            onClearWallpaper = {
                if (selectedNight) {
                    setThemeStoreImageSlotNightMedia(context, slot, null, video = false)
                } else {
                    setThemeStoreImageSlot(context, slot, null)
                }
            },
        )
    }
}

@Composable
internal fun rememberHomeMetricCardWallpaperBitmap(
    uriString: String?,
    crop: CustomWallpaperCrop,
): Bitmap? {
    return rememberCustomImageAndroidBitmap(
        uriString = uriString,
        maxSide = HOME_METRIC_CARD_WALLPAPER_MAX_SIDE,
        crop = crop,
    )
}

@Composable
internal fun BoxScope.HomeMetricCardWallpaperBackground(
    bitmap: Bitmap?,
    videoUriString: String? = null,
    videoCrop: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
    wallpaperFit: HomeLayoutWallpaperFit = HomeLayoutWallpaperFit.Crop,
    visualSettings: MediaVisualSettings,
) {
    if (bitmap == null && videoUriString.isNullOrBlank()) return

    val adjusted = visualSettings.copy(
        overlayAlpha = (visualSettings.overlayAlpha + if (isInDarkTheme()) 0.08f else 0f).coerceAtMost(0.82f)
    )
    MediaVisualLayer(settings = adjusted, modifier = Modifier.matchParentSize()) { colorFilter ->
        if (!videoUriString.isNullOrBlank()) {
            CustomVideoBackground(
                uriString = videoUriString,
                drawOverlay = false,
                crop = videoCrop,
                touchPassthrough = true,
                modifier = Modifier.matchParentSize(),
            )
        } else if (bitmap != null) {
            val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
            Image(
                modifier = Modifier.matchParentSize(),
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = wallpaperFit.toContentScale(),
                colorFilter = colorFilter,
            )
        }
    }
}

internal fun HomeLayoutWallpaperFit.toContentScale(): ContentScale = when (this) {
    HomeLayoutWallpaperFit.Crop -> ContentScale.Crop
    HomeLayoutWallpaperFit.Fit -> ContentScale.Fit
    HomeLayoutWallpaperFit.Stretch -> ContentScale.FillBounds
}

private fun readHomeMetricCardWallpaperCrop(
    prefs: SharedPreferences,
    target: HomeMetricCardWallpaperTarget,
): CustomWallpaperCrop {
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = prefs.getFloat(target.cropLeftKey, DEFAULT_CUSTOM_WALLPAPER_CROP.left),
            top = prefs.getFloat(target.cropTopKey, DEFAULT_CUSTOM_WALLPAPER_CROP.top),
            right = prefs.getFloat(target.cropRightKey, DEFAULT_CUSTOM_WALLPAPER_CROP.right),
            bottom = prefs.getFloat(target.cropBottomKey, DEFAULT_CUSTOM_WALLPAPER_CROP.bottom),
        )
    )
}

private fun SharedPreferences.Editor.putHomeMetricCardWallpaperCrop(
    target: HomeMetricCardWallpaperTarget,
    crop: CustomWallpaperCrop,
) {
    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    putFloat(target.cropLeftKey, safeCrop.left)
    putFloat(target.cropTopKey, safeCrop.top)
    putFloat(target.cropRightKey, safeCrop.right)
    putFloat(target.cropBottomKey, safeCrop.bottom)
}

private fun SharedPreferences.Editor.removeHomeMetricCardWallpaperCrop(
    target: HomeMetricCardWallpaperTarget,
) {
    remove(target.cropLeftKey)
    remove(target.cropTopKey)
    remove(target.cropRightKey)
    remove(target.cropBottomKey)
}
