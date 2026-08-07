package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import me.weishu.kernelsu.R

enum class CustomPageBackgroundTarget(
    val id: String,
    val mainPageIndex: Int,
    @StringRes val titleRes: Int,
    val wallpaperUriKey: String,
    val videoUriKey: String,
    val opacityKey: String,
    val cropLeftKey: String,
    val cropTopKey: String,
    val cropRightKey: String,
    val cropBottomKey: String,
    val videoDurationSecondsKey: String,
) {
    Home(
        id = "home",
        mainPageIndex = 0,
        titleRes = R.string.settings_page_background_home,
        wallpaperUriKey = "custom_page_background_home_wallpaper_uri",
        videoUriKey = "custom_page_background_home_video_uri",
        opacityKey = "custom_page_background_home_opacity",
        cropLeftKey = "custom_page_background_home_crop_left",
        cropTopKey = "custom_page_background_home_crop_top",
        cropRightKey = "custom_page_background_home_crop_right",
        cropBottomKey = "custom_page_background_home_crop_bottom",
        videoDurationSecondsKey = "custom_page_background_home_video_duration_seconds",
    ),
    Superuser(
        id = "superuser",
        mainPageIndex = 1,
        titleRes = R.string.settings_page_background_superuser,
        wallpaperUriKey = "custom_page_background_superuser_wallpaper_uri",
        videoUriKey = "custom_page_background_superuser_video_uri",
        opacityKey = "custom_page_background_superuser_opacity",
        cropLeftKey = "custom_page_background_superuser_crop_left",
        cropTopKey = "custom_page_background_superuser_crop_top",
        cropRightKey = "custom_page_background_superuser_crop_right",
        cropBottomKey = "custom_page_background_superuser_crop_bottom",
        videoDurationSecondsKey = "custom_page_background_superuser_video_duration_seconds",
    ),
    Module(
        id = "module",
        mainPageIndex = 2,
        titleRes = R.string.settings_page_background_module,
        wallpaperUriKey = "custom_page_background_module_wallpaper_uri",
        videoUriKey = "custom_page_background_module_video_uri",
        opacityKey = "custom_page_background_module_opacity",
        cropLeftKey = "custom_page_background_module_crop_left",
        cropTopKey = "custom_page_background_module_crop_top",
        cropRightKey = "custom_page_background_module_crop_right",
        cropBottomKey = "custom_page_background_module_crop_bottom",
        videoDurationSecondsKey = "custom_page_background_module_video_duration_seconds",
    ),
    Settings(
        id = "settings",
        mainPageIndex = 3,
        titleRes = R.string.settings_page_background_settings,
        wallpaperUriKey = "custom_page_background_settings_wallpaper_uri",
        videoUriKey = "custom_page_background_settings_video_uri",
        opacityKey = "custom_page_background_settings_opacity",
        cropLeftKey = "custom_page_background_settings_crop_left",
        cropTopKey = "custom_page_background_settings_crop_top",
        cropRightKey = "custom_page_background_settings_crop_right",
        cropBottomKey = "custom_page_background_settings_crop_bottom",
        videoDurationSecondsKey = "custom_page_background_settings_video_duration_seconds",
    ),
    Install(
        id = "install",
        mainPageIndex = -1,
        titleRes = R.string.settings_page_background_install,
        wallpaperUriKey = "custom_page_background_install_wallpaper_uri",
        videoUriKey = "custom_page_background_install_video_uri",
        opacityKey = "custom_page_background_install_opacity",
        cropLeftKey = "custom_page_background_install_crop_left",
        cropTopKey = "custom_page_background_install_crop_top",
        cropRightKey = "custom_page_background_install_crop_right",
        cropBottomKey = "custom_page_background_install_crop_bottom",
        videoDurationSecondsKey = "custom_page_background_install_video_duration_seconds",
    );

    companion object {
        fun fromMainPageIndex(page: Int): CustomPageBackgroundTarget? {
            return entries.firstOrNull { it.mainPageIndex == page }
        }
    }

    val visualKeys: MediaVisualPreferenceKeys
        get() = MediaVisualPreferenceKeys("custom_page_background_${id}_visual")
}

val GLOBAL_BACKGROUND_VISUAL_KEYS = MediaVisualPreferenceKeys("custom_global_background_visual")

@Immutable
data class CustomBackgroundState(
    val wallpaperUriString: String? = null,
    val videoUriString: String? = null,
    val opacity: Float = DEFAULT_CUSTOM_WALLPAPER_OPACITY,
    val crop: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
    val videoDurationSeconds: Int = DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
    val videoFrameRate: Int = DEFAULT_CUSTOM_VIDEO_BACKGROUND_FRAME_RATE,
    val visualSettings: MediaVisualSettings = MediaVisualSettings(),
) {
    val hasWallpaper: Boolean
        get() = !wallpaperUriString.isNullOrBlank()

    val hasVideo: Boolean
        get() = !videoUriString.isNullOrBlank()

    val hasMedia: Boolean
        get() = hasWallpaper || hasVideo
}

@Immutable
data class CustomPageBackgroundSet(
    val home: CustomBackgroundState = CustomBackgroundState(),
    val superuser: CustomBackgroundState = CustomBackgroundState(),
    val module: CustomBackgroundState = CustomBackgroundState(),
    val settings: CustomBackgroundState = CustomBackgroundState(),
    val install: CustomBackgroundState = CustomBackgroundState(),
) {
    operator fun get(target: CustomPageBackgroundTarget): CustomBackgroundState {
        return when (target) {
            CustomPageBackgroundTarget.Home -> home
            CustomPageBackgroundTarget.Superuser -> superuser
            CustomPageBackgroundTarget.Module -> module
            CustomPageBackgroundTarget.Settings -> settings
            CustomPageBackgroundTarget.Install -> install
        }
    }

    fun forMainPage(page: Int): CustomBackgroundState? {
        val target = CustomPageBackgroundTarget.fromMainPageIndex(page) ?: return null
        return get(target)
    }
}

val CUSTOM_PAGE_BACKGROUND_PREFERENCE_KEYS: Set<String> = buildSet {
    CustomPageBackgroundTarget.entries.forEach { target ->
        add(target.wallpaperUriKey)
        add(target.videoUriKey)
        add(target.opacityKey)
        add(target.cropLeftKey)
        add(target.cropTopKey)
        add(target.cropRightKey)
        add(target.cropBottomKey)
        add(target.videoDurationSecondsKey)
        addAll(target.visualKeys.all)
    }
    addAll(GLOBAL_BACKGROUND_VISUAL_KEYS.all)
    add(CUSTOM_VIDEO_BACKGROUND_FRAME_RATE_KEY)
}

fun readCustomPageBackgroundSet(context: Context): CustomPageBackgroundSet {
    return customPageBackgroundPrefs(context).readCustomPageBackgroundSet()
}

fun setCustomPageBackgroundWallpaper(
    context: Context,
    target: CustomPageBackgroundTarget,
    uriString: String?,
) {
    val prefs = customPageBackgroundPrefs(context)
    val previousWallpaper = prefs.getString(target.wallpaperUriKey, null)
    val previousVideo = prefs.getString(target.videoUriKey, null)
    prefs.edit(commit = true) {
        if (uriString.isNullOrBlank()) {
            remove(target.wallpaperUriKey)
            removeCustomPageBackgroundCrop(target)
        } else {
            putString(target.wallpaperUriKey, uriString)
            remove(target.videoUriKey)
            putCustomPageBackgroundCrop(target, DEFAULT_CUSTOM_WALLPAPER_CROP)
        }
    }
    if (previousWallpaper != uriString) {
        releaseCustomImageReference(context, previousWallpaper)
    }
    if (!uriString.isNullOrBlank() && previousVideo != null) {
        releasePersistableVideoBackgroundReadPermission(context, previousVideo)
    }
}

fun setCustomPageBackgroundVideo(
    context: Context,
    target: CustomPageBackgroundTarget,
    uriString: String?,
) {
    val prefs = customPageBackgroundPrefs(context)
    val previousVideo = prefs.getString(target.videoUriKey, null)
    val previousWallpaper = prefs.getString(target.wallpaperUriKey, null)
    prefs.edit(commit = true) {
        if (uriString.isNullOrBlank()) {
            remove(target.videoUriKey)
        } else {
            putString(target.videoUriKey, uriString)
            remove(target.wallpaperUriKey)
            removeCustomPageBackgroundCrop(target)
        }
    }
    if (previousVideo != uriString) {
        releasePersistableVideoBackgroundReadPermission(context, previousVideo)
    }
    if (!uriString.isNullOrBlank() && previousWallpaper != null) {
        releaseCustomImageReference(context, previousWallpaper)
    }
}

fun clearCustomPageBackground(context: Context, target: CustomPageBackgroundTarget) {
    val prefs = customPageBackgroundPrefs(context)
    val previousWallpaper = prefs.getString(target.wallpaperUriKey, null)
    val previousVideo = prefs.getString(target.videoUriKey, null)
    prefs.edit(commit = true) {
        remove(target.wallpaperUriKey)
        remove(target.videoUriKey)
        remove(target.opacityKey)
        remove(target.videoDurationSecondsKey)
        removeMediaVisualSettings(target.visualKeys)
        removeCustomPageBackgroundCrop(target)
    }
    releaseCustomImageReference(context, previousWallpaper)
    releasePersistableVideoBackgroundReadPermission(context, previousVideo)
}

fun setCustomPageBackgroundOpacity(
    context: Context,
    target: CustomPageBackgroundTarget,
    opacity: Float,
) {
    customPageBackgroundPrefs(context).edit {
        putFloat(target.opacityKey, sanitizeCustomWallpaperOpacity(opacity))
    }
}

fun setCustomPageBackgroundCrop(
    context: Context,
    target: CustomPageBackgroundTarget,
    crop: CustomWallpaperCrop,
) {
    customPageBackgroundPrefs(context).edit(commit = true) {
        putCustomPageBackgroundCrop(target, crop)
    }
}

fun setCustomPageBackgroundVideoDurationSeconds(
    context: Context,
    target: CustomPageBackgroundTarget,
    seconds: Int,
) {
    customPageBackgroundPrefs(context).edit {
        putInt(target.videoDurationSecondsKey, sanitizeCustomVideoBackgroundDurationSeconds(seconds))
    }
}

fun setCustomPageBackgroundVisualSettings(
    context: Context,
    target: CustomPageBackgroundTarget,
    settings: MediaVisualSettings,
) {
    customPageBackgroundPrefs(context).edit {
        putMediaVisualSettings(target.visualKeys, settings)
    }
}

fun readGlobalBackgroundVisualSettings(context: Context): MediaVisualSettings {
    return customPageBackgroundPrefs(context).readMediaVisualSettings(GLOBAL_BACKGROUND_VISUAL_KEYS)
}

fun setGlobalBackgroundVisualSettings(context: Context, settings: MediaVisualSettings) {
    customPageBackgroundPrefs(context).edit {
        putMediaVisualSettings(GLOBAL_BACKGROUND_VISUAL_KEYS, settings)
    }
}

internal fun SharedPreferences.readCustomPageBackgroundSet(): CustomPageBackgroundSet {
    return CustomPageBackgroundSet(
        home = readCustomPageBackgroundState(CustomPageBackgroundTarget.Home),
        superuser = readCustomPageBackgroundState(CustomPageBackgroundTarget.Superuser),
        module = readCustomPageBackgroundState(CustomPageBackgroundTarget.Module),
        settings = readCustomPageBackgroundState(CustomPageBackgroundTarget.Settings),
        install = readCustomPageBackgroundState(CustomPageBackgroundTarget.Install),
    )
}

private fun SharedPreferences.readCustomPageBackgroundState(
    target: CustomPageBackgroundTarget,
): CustomBackgroundState {
    return CustomBackgroundState(
        wallpaperUriString = getString(target.wallpaperUriKey, null),
        videoUriString = getString(target.videoUriKey, null),
        opacity = sanitizeCustomWallpaperOpacity(
            getFloat(target.opacityKey, DEFAULT_CUSTOM_WALLPAPER_OPACITY)
        ),
        crop = sanitizeCustomWallpaperCrop(
            CustomWallpaperCrop(
                left = getFloat(target.cropLeftKey, DEFAULT_CUSTOM_WALLPAPER_CROP.left),
                top = getFloat(target.cropTopKey, DEFAULT_CUSTOM_WALLPAPER_CROP.top),
                right = getFloat(target.cropRightKey, DEFAULT_CUSTOM_WALLPAPER_CROP.right),
                bottom = getFloat(target.cropBottomKey, DEFAULT_CUSTOM_WALLPAPER_CROP.bottom),
            )
        ),
        videoDurationSeconds = sanitizeCustomVideoBackgroundDurationSeconds(
            getInt(target.videoDurationSecondsKey, DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS)
        ),
        videoFrameRate = sanitizeCustomVideoBackgroundFrameRate(
            getInt(
                CUSTOM_VIDEO_BACKGROUND_FRAME_RATE_KEY,
                DEFAULT_CUSTOM_VIDEO_BACKGROUND_FRAME_RATE,
            )
        ),
        visualSettings = readMediaVisualSettings(target.visualKeys),
    )
}

private fun SharedPreferences.Editor.putCustomPageBackgroundCrop(
    target: CustomPageBackgroundTarget,
    crop: CustomWallpaperCrop,
) {
    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    putFloat(target.cropLeftKey, safeCrop.left)
    putFloat(target.cropTopKey, safeCrop.top)
    putFloat(target.cropRightKey, safeCrop.right)
    putFloat(target.cropBottomKey, safeCrop.bottom)
}

private fun SharedPreferences.Editor.removeCustomPageBackgroundCrop(target: CustomPageBackgroundTarget) {
    remove(target.cropLeftKey)
    remove(target.cropTopKey)
    remove(target.cropRightKey)
    remove(target.cropBottomKey)
}

private fun customPageBackgroundPrefs(context: Context): SharedPreferences {
    return context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
}
