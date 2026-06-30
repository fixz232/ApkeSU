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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.CustomVideoBackground
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.releasePersistableVideoBackgroundReadPermission
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperCrop
import me.weishu.kernelsu.ui.util.takePersistableImageReadPermission
import me.weishu.kernelsu.ui.util.takePersistableVideoBackgroundReadPermission

internal const val HOME_METRIC_CARD_WALLPAPER_ASPECT_RATIO = 1.72f

private const val HOME_LKM_CARD_WALLPAPER_ASPECT_RATIO = 1.08f
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
}

internal data class HomeMetricCardWallpaperState(
    val uriString: String?,
    val videoUriString: String?,
    val crop: CustomWallpaperCrop,
    val onPickWallpaper: () -> Unit,
    val onPickVideoWallpaper: () -> Unit,
    val onCropChange: (CustomWallpaperCrop) -> Unit,
    val onClearWallpaper: () -> Unit,
) {
    val hasSelectedWallpaper: Boolean
        get() = !uriString.isNullOrBlank()
    val hasSelectedVideoWallpaper: Boolean
        get() = !videoUriString.isNullOrBlank()
    val hasSelectedAnyWallpaper: Boolean
        get() = hasSelectedWallpaper || hasSelectedVideoWallpaper
}

@Composable
internal fun rememberHomeMetricCardWallpaperState(
    target: HomeMetricCardWallpaperTarget,
    onWallpaperSelected: () -> Unit,
): HomeMetricCardWallpaperState {
    val context = LocalContext.current
    val currentOnWallpaperSelected by rememberUpdatedState(onWallpaperSelected)
    val prefs = remember(context) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    var uriString by remember(target) {
        mutableStateOf(prefs.getString(target.uriKey, null))
    }
    var videoUriString by remember(target) {
        mutableStateOf(prefs.getString(target.videoUriKey, null))
    }
    var crop by remember(target) {
        mutableStateOf(readHomeMetricCardWallpaperCrop(prefs, target))
    }
    DisposableEffect(prefs, target) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
            if (key == null || key !in target.preferenceKeys) return@OnSharedPreferenceChangeListener
            uriString = preferences.getString(target.uriKey, null)
            videoUriString = preferences.getString(target.videoUriKey, null)
            crop = readHomeMetricCardWallpaperCrop(preferences, target)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val nextUriString = persistCustomImageReference(context, uri, target.uriKey)
            ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
        val previousUriString = uriString
        val defaultCrop = DEFAULT_CUSTOM_WALLPAPER_CROP
        if (previousUriString != nextUriString) {
            releaseCustomImageReference(context, previousUriString)
        }
        releasePersistableVideoBackgroundReadPermission(context, videoUriString)
        uriString = nextUriString
        videoUriString = null
        crop = defaultCrop
        prefs.edit(commit = true) {
            putString(target.uriKey, nextUriString)
            remove(target.videoUriKey)
            putHomeMetricCardWallpaperCrop(target, defaultCrop)
        }
        currentOnWallpaperSelected()
    }
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val nextUriString = uri.toString()
        val previousUriString = uriString
        val previousVideoUriString = videoUriString
        takePersistableVideoBackgroundReadPermission(context, uri)
        releaseCustomImageReference(context, previousUriString)
        if (previousVideoUriString != nextUriString) {
            releasePersistableVideoBackgroundReadPermission(context, previousVideoUriString)
        }
        uriString = null
        videoUriString = nextUriString
        crop = DEFAULT_CUSTOM_WALLPAPER_CROP
        prefs.edit(commit = true) {
            remove(target.uriKey)
            putHomeMetricCardWallpaperCrop(target, DEFAULT_CUSTOM_WALLPAPER_CROP)
            putString(target.videoUriKey, nextUriString)
        }
        currentOnWallpaperSelected()
    }

    return remember(target, uriString, videoUriString, crop, launcher, videoLauncher, prefs, context) {
        HomeMetricCardWallpaperState(
            uriString = uriString,
            videoUriString = videoUriString,
            crop = crop,
            onPickWallpaper = {
                launcher.launch(arrayOf("image/*"))
            },
            onPickVideoWallpaper = {
                videoLauncher.launch(arrayOf("video/*"))
            },
            onCropChange = { nextCrop ->
                val safeCrop = sanitizeCustomWallpaperCrop(nextCrop)
                crop = safeCrop
                prefs.edit(commit = true) {
                    putHomeMetricCardWallpaperCrop(target, safeCrop)
                }
            },
            onClearWallpaper = {
                releaseCustomImageReference(context, uriString)
                releasePersistableVideoBackgroundReadPermission(context, videoUriString)
                uriString = null
                videoUriString = null
                crop = DEFAULT_CUSTOM_WALLPAPER_CROP
                prefs.edit(commit = true) {
                    remove(target.uriKey)
                    remove(target.videoUriKey)
                    removeHomeMetricCardWallpaperCrop(target)
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
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, uriString, crop, context) {
        value = if (uriString.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                loadCustomImageBitmap(
                    context = context,
                    uriString = uriString,
                    maxSide = HOME_METRIC_CARD_WALLPAPER_MAX_SIDE,
                    crop = crop,
                )
            }
        }
    }
    return bitmapState.value
}

@Composable
internal fun BoxScope.HomeMetricCardWallpaperBackground(
    bitmap: Bitmap?,
    videoUriString: String? = null,
    videoCrop: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
) {
    if (bitmap == null && videoUriString.isNullOrBlank()) return

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
            contentScale = ContentScale.Crop
        )
    }
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = if (isInDarkTheme()) 0.52f else 0.44f))
    )
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
