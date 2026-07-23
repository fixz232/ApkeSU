package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.edit
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val THEME_STORE_SCHEMA = "io.github.fixz.apkesu.theme"
private const val THEME_STORE_VERSION = 4
private const val MAX_THEME_STORE_ENTRY_COUNT = 64
private const val MAX_THEME_STORE_JSON_BYTES = 256L * 1024L
private const val MAX_THEME_STORE_ASSET_BYTES = 256L * 1024L * 1024L
private const val MAX_THEME_STORE_ASSETS_BYTES = 512L * 1024L * 1024L
private const val MAX_THEME_STORE_PREVIEW_IMAGE_BYTES = 16L * 1024L * 1024L
const val THEME_STORE_FILE_MIME_TYPE = "application/zip"
const val THEME_STORE_FILE_EXTENSION = "kstheme"

enum class ThemeStoreImageSlot(
    val id: String,
    val uriKey: String,
    val videoUriKey: String?,
    val cropLeftKey: String,
    val cropTopKey: String,
    val cropRightKey: String,
    val cropBottomKey: String,
) {
    Lkm(
        id = "lkm",
        uriKey = "home_lkm_card_wallpaper_uri",
        videoUriKey = "home_lkm_card_wallpaper_video_uri",
        cropLeftKey = "home_lkm_card_wallpaper_crop_left",
        cropTopKey = "home_lkm_card_wallpaper_crop_top",
        cropRightKey = "home_lkm_card_wallpaper_crop_right",
        cropBottomKey = "home_lkm_card_wallpaper_crop_bottom",
    ),
    Superuser(
        id = "superuser",
        uriKey = "home_superuser_card_wallpaper_uri",
        videoUriKey = null,
        cropLeftKey = "home_superuser_card_wallpaper_crop_left",
        cropTopKey = "home_superuser_card_wallpaper_crop_top",
        cropRightKey = "home_superuser_card_wallpaper_crop_right",
        cropBottomKey = "home_superuser_card_wallpaper_crop_bottom",
    ),
    Module(
        id = "module",
        uriKey = "home_module_card_wallpaper_uri",
        videoUriKey = null,
        cropLeftKey = "home_module_card_wallpaper_crop_left",
        cropTopKey = "home_module_card_wallpaper_crop_top",
        cropRightKey = "home_module_card_wallpaper_crop_right",
        cropBottomKey = "home_module_card_wallpaper_crop_bottom",
    ),
    StatusMonitor(
        id = "status_monitor",
        uriKey = "home_status_monitor_wallpaper_uri",
        videoUriKey = null,
        cropLeftKey = "home_status_monitor_wallpaper_crop_left",
        cropTopKey = "home_status_monitor_wallpaper_crop_top",
        cropRightKey = "home_status_monitor_wallpaper_crop_right",
        cropBottomKey = "home_status_monitor_wallpaper_crop_bottom",
    ),
    SystemInfo(
        id = "system_info",
        uriKey = "home_system_info_wallpaper_uri",
        videoUriKey = null,
        cropLeftKey = "home_system_info_wallpaper_crop_left",
        cropTopKey = "home_system_info_wallpaper_crop_top",
        cropRightKey = "home_system_info_wallpaper_crop_right",
        cropBottomKey = "home_system_info_wallpaper_crop_bottom",
    ),
    RebootMenu(
        id = "reboot_menu",
        uriKey = "home_reboot_menu_wallpaper_uri",
        videoUriKey = "home_reboot_menu_wallpaper_video_uri",
        cropLeftKey = "home_reboot_menu_wallpaper_crop_left",
        cropTopKey = "home_reboot_menu_wallpaper_crop_top",
        cropRightKey = "home_reboot_menu_wallpaper_crop_right",
        cropBottomKey = "home_reboot_menu_wallpaper_crop_bottom",
    ),
}

data class ThemeStoreImageState(
    val uriString: String?,
    val videoUriString: String?,
    val crop: CustomWallpaperCrop,
) {
    val hasSelected: Boolean
        get() = hasImageSelected || hasVideoSelected
    val hasImageSelected: Boolean
        get() = !uriString.isNullOrBlank()
    val hasVideoSelected: Boolean
        get() = !videoUriString.isNullOrBlank()
}

data class ThemeStoreWallpaperState(
    val uriString: String?,
    val videoUriString: String?,
    val videoDurationSeconds: Int,
    val opacity: Float,
    val crop: CustomWallpaperCrop,
    val passthroughEnabled: Boolean,
    val passthroughOpacity: Float,
) {
    val hasSelected: Boolean
        get() = hasImageSelected || hasVideoSelected
    val hasImageSelected: Boolean
        get() = !uriString.isNullOrBlank()
    val hasVideoSelected: Boolean
        get() = !videoUriString.isNullOrBlank()
}

data class ThemeStoreAudioState(
    val startupSoundUri: String?,
    val startupSoundDurationSeconds: Int,
    val startupSoundVolume: Float,
    val clickSoundUri: String?,
    val clickSoundVolume: Float,
    val backgroundMusicUri: String?,
    val backgroundMusicVolume: Float,
) {
    val configuredCount: Int
        get() = listOf(startupSoundUri, clickSoundUri, backgroundMusicUri)
            .count { !it.isNullOrBlank() }
}

data class ThemeStoreSummary(
    val lkmCard: ThemeStoreImageState,
    val superuserCard: ThemeStoreImageState,
    val moduleCard: ThemeStoreImageState,
    val statusMonitorCard: ThemeStoreImageState,
    val systemInfoCard: ThemeStoreImageState,
    val rebootMenuCard: ThemeStoreImageState,
    val navigationIcons: CustomNavigationIconSet,
    val pageBackgrounds: CustomPageBackgroundSet,
    val wallpaper: ThemeStoreWallpaperState,
    val audio: ThemeStoreAudioState,
    val startupAnimationUri: String?,
) {
    val startupSoundUri: String?
        get() = audio.startupSoundUri

    val selectedCount: Int
        get() = navigationIcons.selectedCount +
            CustomPageBackgroundTarget.entries.count { pageBackgrounds[it].hasMedia } +
            listOf(
                lkmCard.hasSelected,
                superuserCard.hasSelected,
                moduleCard.hasSelected,
                statusMonitorCard.hasSelected,
                systemInfoCard.hasSelected,
                rebootMenuCard.hasSelected,
                wallpaper.hasSelected,
                !startupAnimationUri.isNullOrBlank(),
            ).count { it } + audio.configuredCount
}

data class ThemeStorePackageResult(
    val success: Boolean,
    val warnings: List<ThemeStorePackageWarning> = emptyList(),
    val error: Throwable? = null,
)

data class ThemeStorePackageWarning(
    val assetId: String,
    val reason: String? = null,
)

data class ThemeStorePackagePreviewImage(
    val bytes: ByteArray,
    val mimeType: String? = null,
)

data class ThemeStorePackageAuthor(
    val displayName: String,
    val realName: String,
    val gender: ThemeAuthorGender,
    val bio: String,
    val avatar: ThemeStorePackagePreviewImage? = null,
)

data class ThemeStorePackagePreview(
    val version: Int,
    val exportedAt: Long,
    val configuredResourceCount: Int,
    val author: ThemeStorePackageAuthor?,
    val cover: ThemeStorePackagePreviewImage?,
)

data class ThemeStorePackagePreviewResult(
    val success: Boolean,
    val preview: ThemeStorePackagePreview? = null,
    val warnings: List<ThemeStorePackageWarning> = emptyList(),
    val error: Throwable? = null,
)

internal data class ExtractedThemeStoreArchive(
    val themeJson: String,
    val assetsBytes: Long,
)

private class ThemeStoreAssetBudget(
    var totalBytes: Long = 0L,
)

private sealed interface ImportedThemeAsset {
    data class Resolved(val uriString: String?) : ImportedThemeAsset
    data object Unavailable : ImportedThemeAsset
}

fun readThemeStoreSummary(context: Context): ThemeStoreSummary {
    val prefs = themeStorePrefs(context)
    return ThemeStoreSummary(
        lkmCard = prefs.readImageSlot(ThemeStoreImageSlot.Lkm),
        superuserCard = prefs.readImageSlot(ThemeStoreImageSlot.Superuser),
        moduleCard = prefs.readImageSlot(ThemeStoreImageSlot.Module),
        statusMonitorCard = prefs.readImageSlot(ThemeStoreImageSlot.StatusMonitor),
        systemInfoCard = prefs.readImageSlot(ThemeStoreImageSlot.SystemInfo),
        rebootMenuCard = prefs.readImageSlot(ThemeStoreImageSlot.RebootMenu),
        navigationIcons = prefs.readCustomNavigationIconSet(),
        pageBackgrounds = prefs.readCustomPageBackgroundSet(),
        wallpaper = ThemeStoreWallpaperState(
            uriString = prefs.getString(CUSTOM_WALLPAPER_URI_KEY, null),
            videoUriString = prefs.getString(CUSTOM_VIDEO_BACKGROUND_URI_KEY, null),
            videoDurationSeconds = sanitizeCustomVideoBackgroundDurationSeconds(
                prefs.getInt(
                    CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS_KEY,
                    DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
                )
            ),
            opacity = sanitizeCustomWallpaperOpacity(
                prefs.getFloat(CUSTOM_WALLPAPER_OPACITY_KEY, DEFAULT_CUSTOM_WALLPAPER_OPACITY)
            ),
            crop = prefs.readCustomWallpaperCrop(),
            passthroughEnabled = prefs.getBoolean(CUSTOM_WALLPAPER_PASSTHROUGH_ENABLED_KEY, false),
            passthroughOpacity = sanitizeCustomWallpaperPassthroughOpacity(
                prefs.getFloat(
                    CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY_KEY,
                    DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY,
                )
            ),
        ),
        audio = ThemeStoreAudioState(
            startupSoundUri = prefs.getString(CUSTOM_STARTUP_SOUND_URI_KEY, null),
            startupSoundDurationSeconds = sanitizeCustomStartupSoundDurationSeconds(
                prefs.getInt(
                    CUSTOM_STARTUP_SOUND_DURATION_SECONDS_KEY,
                    DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
                )
            ),
            startupSoundVolume = sanitizeCustomAudioVolume(
                prefs.getFloat(CUSTOM_STARTUP_SOUND_VOLUME_KEY, DEFAULT_CUSTOM_AUDIO_VOLUME)
            ),
            clickSoundUri = prefs.getString(CUSTOM_CLICK_SOUND_URI_KEY, null),
            clickSoundVolume = sanitizeCustomAudioVolume(
                prefs.getFloat(CUSTOM_CLICK_SOUND_VOLUME_KEY, DEFAULT_CUSTOM_AUDIO_VOLUME)
            ),
            backgroundMusicUri = prefs.getString(CUSTOM_BACKGROUND_MUSIC_URI_KEY, null),
            backgroundMusicVolume = sanitizeCustomBackgroundMusicVolume(
                prefs.getFloat(
                    CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY,
                    DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME,
                )
            ),
        ),
        startupAnimationUri = prefs.getString(CUSTOM_STARTUP_ANIMATION_URI_KEY, null),
    )
}

fun setThemeStoreImageSlot(context: Context, slot: ThemeStoreImageSlot, uriString: String?) {
    val prefs = themeStorePrefs(context)
    val previous = prefs.getString(slot.uriKey, null)
    val previousVideo = slot.videoUriKey?.let { prefs.getString(it, null) }
    prefs.edit(commit = true) {
        if (uriString.isNullOrBlank()) {
            remove(slot.uriKey)
            slot.videoUriKey?.let(::remove)
            removeImageSlotCrop(slot)
        } else {
            putString(slot.uriKey, uriString)
            slot.videoUriKey?.let(::remove)
            putImageSlotCrop(slot, DEFAULT_CUSTOM_WALLPAPER_CROP)
        }
    }
    if (previous != uriString) {
        releaseCustomImageReference(context, previous)
    }
    releasePersistableVideoBackgroundReadPermission(context, previousVideo)
}

fun setThemeStoreImageSlotVideo(context: Context, slot: ThemeStoreImageSlot, uriString: String?) {
    val videoUriKey = slot.videoUriKey ?: return
    val prefs = themeStorePrefs(context)
    val previous = prefs.getString(slot.uriKey, null)
    val previousVideo = prefs.getString(videoUriKey, null)
    prefs.edit(commit = true) {
        if (uriString.isNullOrBlank()) {
            remove(videoUriKey)
        } else {
            remove(slot.uriKey)
            putImageSlotCrop(slot, DEFAULT_CUSTOM_WALLPAPER_CROP)
            putString(videoUriKey, uriString)
        }
    }
    if (!uriString.isNullOrBlank()) {
        releaseCustomImageReference(context, previous)
    }
    if (previousVideo != uriString) {
        releasePersistableVideoBackgroundReadPermission(context, previousVideo)
    }
}

fun setThemeStoreImageSlotCrop(context: Context, slot: ThemeStoreImageSlot, crop: CustomWallpaperCrop) {
    themeStorePrefs(context).edit(commit = true) {
        putImageSlotCrop(slot, crop)
    }
}

fun setThemeStoreWallpaper(
    context: Context,
    uriString: String?,
    opacity: Float = DEFAULT_CUSTOM_WALLPAPER_OPACITY,
    crop: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
    passthroughEnabled: Boolean = false,
    passthroughOpacity: Float = DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY,
) {
    val prefs = themeStorePrefs(context)
    val previous = prefs.getString(CUSTOM_WALLPAPER_URI_KEY, null)
    val previousVideo = prefs.getString(CUSTOM_VIDEO_BACKGROUND_URI_KEY, null)
    prefs.edit(commit = true) {
        if (uriString.isNullOrBlank()) {
            remove(CUSTOM_WALLPAPER_URI_KEY)
            removeCustomWallpaperCrop()
        } else {
            putString(CUSTOM_WALLPAPER_URI_KEY, uriString)
            remove(CUSTOM_VIDEO_BACKGROUND_URI_KEY)
            putCustomWallpaperCrop(crop)
        }
        putFloat(CUSTOM_WALLPAPER_OPACITY_KEY, sanitizeCustomWallpaperOpacity(opacity))
        putBoolean(CUSTOM_WALLPAPER_PASSTHROUGH_ENABLED_KEY, passthroughEnabled)
        putFloat(
            CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY_KEY,
            sanitizeCustomWallpaperPassthroughOpacity(passthroughOpacity),
        )
    }
    if (previous != uriString) {
        releaseCustomImageReference(context, previous)
    }
    if (!uriString.isNullOrBlank()) {
        releasePersistableVideoBackgroundReadPermission(context, previousVideo)
    }
}

fun setThemeStoreWallpaperCrop(context: Context, crop: CustomWallpaperCrop) {
    themeStorePrefs(context).edit(commit = true) {
        putCustomWallpaperCrop(crop)
    }
}

fun setThemeStoreVideoBackground(
    context: Context,
    uriString: String?,
    durationSeconds: Int? = null,
    opacity: Float = DEFAULT_CUSTOM_WALLPAPER_OPACITY,
    passthroughEnabled: Boolean = false,
    passthroughOpacity: Float = DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY,
) {
    val prefs = themeStorePrefs(context)
    val previousVideo = prefs.getString(CUSTOM_VIDEO_BACKGROUND_URI_KEY, null)
    val previousWallpaper = prefs.getString(CUSTOM_WALLPAPER_URI_KEY, null)
    prefs.edit(commit = true) {
        if (uriString.isNullOrBlank()) {
            remove(CUSTOM_VIDEO_BACKGROUND_URI_KEY)
        } else {
            putString(CUSTOM_VIDEO_BACKGROUND_URI_KEY, uriString)
            remove(CUSTOM_WALLPAPER_URI_KEY)
            removeCustomWallpaperCrop()
        }
        if (durationSeconds != null) {
            putInt(
                CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS_KEY,
                sanitizeCustomVideoBackgroundDurationSeconds(durationSeconds),
            )
        }
        putFloat(CUSTOM_WALLPAPER_OPACITY_KEY, sanitizeCustomWallpaperOpacity(opacity))
        putBoolean(CUSTOM_WALLPAPER_PASSTHROUGH_ENABLED_KEY, passthroughEnabled)
        putFloat(
            CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY_KEY,
            sanitizeCustomWallpaperPassthroughOpacity(passthroughOpacity),
        )
    }
    if (previousVideo != uriString) {
        releasePersistableVideoBackgroundReadPermission(context, previousVideo)
    }
    if (!uriString.isNullOrBlank()) {
        releaseCustomImageReference(context, previousWallpaper)
    }
}

fun setThemeStoreVideoBackgroundDurationSeconds(context: Context, seconds: Int) {
    themeStorePrefs(context).edit {
        putInt(
            CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS_KEY,
            sanitizeCustomVideoBackgroundDurationSeconds(seconds),
        )
    }
}

fun setThemeStoreStartupSound(context: Context, uriString: String?) {
    val prefs = themeStorePrefs(context)
    val previous = prefs.getString(CUSTOM_STARTUP_SOUND_URI_KEY, null)
    if (previous != uriString) {
        releasePersistableAudioReadPermission(context, previous)
    }
    prefs.edit {
        if (uriString.isNullOrBlank()) {
            remove(CUSTOM_STARTUP_SOUND_URI_KEY)
        } else {
            putString(CUSTOM_STARTUP_SOUND_URI_KEY, uriString)
        }
    }
}

fun setThemeStoreStartupAnimation(context: Context, uriString: String?) {
    val prefs = themeStorePrefs(context)
    val previous = prefs.getString(CUSTOM_STARTUP_ANIMATION_URI_KEY, null)
    if (previous != uriString) {
        releasePersistableStartupAnimationReadPermission(context, previous)
    }
    prefs.edit {
        if (uriString.isNullOrBlank()) {
            remove(CUSTOM_STARTUP_ANIMATION_URI_KEY)
        } else {
            putString(CUSTOM_STARTUP_ANIMATION_URI_KEY, uriString)
        }
    }
}

fun exportThemeStorePackage(context: Context, destination: Uri): ThemeStorePackageResult {
    val warnings = mutableListOf<ThemeStorePackageWarning>()
    return runCatching {
        val appContext = context.applicationContext
        val prefs = themeStorePrefs(appContext)
        val authorProfile = readThemeAuthorProfile(appContext)
        val assetBudget = ThemeStoreAssetBudget()
        val config = JSONObject()
            .put("schema", THEME_STORE_SCHEMA)
            .put("version", THEME_STORE_VERSION)
            .put("exportedAt", System.currentTimeMillis())

        openThemeStoreUriOutputStream(appContext, destination).use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                val authorAvatar = zip.writeUriAsset(
                    context = appContext,
                    uriString = authorProfile.avatarUriString,
                    assetId = "author_avatar",
                    warnings = warnings,
                    budget = assetBudget,
                )
                config.put(
                    "author",
                    JSONObject()
                        .put("displayName", authorProfile.displayName)
                        .put("realName", authorProfile.realName)
                        .put("gender", authorProfile.gender.storageValue)
                        .put("bio", authorProfile.bio)
                        .put("avatar", authorAvatar?.toJson()),
                )

                val cardsJson = JSONObject()
                ThemeStoreImageSlot.entries.forEach { slot ->
                    val state = prefs.readImageSlot(slot)
                    val asset = zip.writeUriAsset(
                        context = appContext,
                        uriString = state.uriString,
                        assetId = "card_${slot.id}",
                        warnings = warnings,
                        budget = assetBudget,
                    )
                    val videoAsset = zip.writeUriAsset(
                        context = appContext,
                        uriString = state.videoUriString,
                        assetId = "card_${slot.id}_video",
                        warnings = warnings,
                        budget = assetBudget,
                    )
                    cardsJson.put(
                        slot.id,
                        JSONObject()
                            .put("asset", asset?.toJson())
                            .put("uri", state.uriString)
                            .put("videoAsset", videoAsset?.toJson())
                            .put("videoUri", state.videoUriString)
                            .put("crop", state.crop.toJson()),
                    )
                }
                config.put("cards", cardsJson)

                val navigationIconsJson = JSONObject()
                CustomNavigationIconSlot.entries.forEach { slot ->
                    val state = prefs.readCustomNavigationIconState(slot)
                    val asset = zip.writeUriAsset(
                        context = appContext,
                        uriString = state.uriString,
                        assetId = "navigation_icon_${slot.id}",
                        warnings = warnings,
                        budget = assetBudget,
                    )
                    navigationIconsJson.put(
                        slot.id,
                        JSONObject()
                            .put("asset", asset?.toJson())
                            .put("uri", state.uriString)
                            .put("crop", state.crop.toJson()),
                    )
                }
                config.put("navigationIcons", navigationIconsJson)

                val pageBackgroundsJson = JSONObject()
                val pageBackgrounds = prefs.readCustomPageBackgroundSet()
                CustomPageBackgroundTarget.entries.forEach { target ->
                    val state = pageBackgrounds[target]
                    val asset = zip.writeUriAsset(
                        context = appContext,
                        uriString = state.wallpaperUriString,
                        assetId = "page_background_${target.id}",
                        warnings = warnings,
                        budget = assetBudget,
                    )
                    val videoAsset = zip.writeUriAsset(
                        context = appContext,
                        uriString = state.videoUriString,
                        assetId = "page_background_${target.id}_video",
                        warnings = warnings,
                        budget = assetBudget,
                    )
                    pageBackgroundsJson.put(
                        target.id,
                        JSONObject()
                            .put("asset", asset?.toJson())
                            .put("uri", state.wallpaperUriString)
                            .put("videoAsset", videoAsset?.toJson())
                            .put("videoUri", state.videoUriString)
                            .put("videoDurationSeconds", state.videoDurationSeconds)
                            .put("opacity", state.opacity)
                            .put("crop", state.crop.toJson()),
                    )
                }
                config.put("pageBackgrounds", pageBackgroundsJson)

                val wallpaperState = ThemeStoreWallpaperState(
                    uriString = prefs.getString(CUSTOM_WALLPAPER_URI_KEY, null),
                    videoUriString = prefs.getString(CUSTOM_VIDEO_BACKGROUND_URI_KEY, null),
                    videoDurationSeconds = sanitizeCustomVideoBackgroundDurationSeconds(
                        prefs.getInt(
                            CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS_KEY,
                            DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
                        )
                    ),
                    opacity = sanitizeCustomWallpaperOpacity(
                        prefs.getFloat(CUSTOM_WALLPAPER_OPACITY_KEY, DEFAULT_CUSTOM_WALLPAPER_OPACITY)
                    ),
                    crop = prefs.readCustomWallpaperCrop(),
                    passthroughEnabled = prefs.getBoolean(CUSTOM_WALLPAPER_PASSTHROUGH_ENABLED_KEY, false),
                    passthroughOpacity = sanitizeCustomWallpaperPassthroughOpacity(
                        prefs.getFloat(
                            CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY_KEY,
                            DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY,
                        )
                    ),
                )
                val wallpaperAsset = zip.writeUriAsset(
                    context = appContext,
                    uriString = wallpaperState.uriString,
                    assetId = "custom_wallpaper",
                    warnings = warnings,
                    budget = assetBudget,
                )
                val videoBackgroundAsset = zip.writeUriAsset(
                    context = appContext,
                    uriString = wallpaperState.videoUriString,
                    assetId = "custom_video_background",
                    warnings = warnings,
                    budget = assetBudget,
                )
                config.put(
                    "wallpaper",
                    JSONObject()
                        .put("asset", wallpaperAsset?.toJson())
                        .put("uri", wallpaperState.uriString)
                        .put("videoAsset", videoBackgroundAsset?.toJson())
                        .put("videoUri", wallpaperState.videoUriString)
                        .put("videoDurationSeconds", wallpaperState.videoDurationSeconds)
                        .put("opacity", wallpaperState.opacity)
                        .put("crop", wallpaperState.crop.toJson())
                        .put("passthroughEnabled", wallpaperState.passthroughEnabled)
                        .put("passthroughOpacity", wallpaperState.passthroughOpacity),
                )

                val startupSoundUri = prefs.getString(CUSTOM_STARTUP_SOUND_URI_KEY, null)
                val startupSoundAsset = zip.writeUriAsset(
                    context = appContext,
                    uriString = startupSoundUri,
                    assetId = "startup_sound",
                    warnings = warnings,
                    budget = assetBudget,
                )
                config.put(
                    "startupSound",
                    JSONObject()
                        .put("asset", startupSoundAsset?.toJson())
                        .put("uri", startupSoundUri)
                        .put(
                            "durationSeconds",
                            sanitizeCustomStartupSoundDurationSeconds(
                                prefs.getInt(
                                    CUSTOM_STARTUP_SOUND_DURATION_SECONDS_KEY,
                                    DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
                                )
                            ),
                        )
                        .put(
                            "volume",
                            sanitizeCustomAudioVolume(
                                prefs.getFloat(
                                    CUSTOM_STARTUP_SOUND_VOLUME_KEY,
                                    DEFAULT_CUSTOM_AUDIO_VOLUME,
                                )
                            ),
                        ),
                )

                val clickSoundUri = prefs.getString(CUSTOM_CLICK_SOUND_URI_KEY, null)
                val clickSoundAsset = zip.writeUriAsset(
                    context = appContext,
                    uriString = clickSoundUri,
                    assetId = "click_sound",
                    warnings = warnings,
                    budget = assetBudget,
                )
                config.put(
                    "clickSound",
                    JSONObject()
                        .put("asset", clickSoundAsset?.toJson())
                        .put("uri", clickSoundUri)
                        .put(
                            "volume",
                            sanitizeCustomAudioVolume(
                                prefs.getFloat(
                                    CUSTOM_CLICK_SOUND_VOLUME_KEY,
                                    DEFAULT_CUSTOM_AUDIO_VOLUME,
                                )
                            ),
                        ),
                )

                val backgroundMusicUri = prefs.getString(CUSTOM_BACKGROUND_MUSIC_URI_KEY, null)
                val backgroundMusicAsset = zip.writeUriAsset(
                    context = appContext,
                    uriString = backgroundMusicUri,
                    assetId = "background_music",
                    warnings = warnings,
                    budget = assetBudget,
                )
                config.put(
                    "backgroundMusic",
                    JSONObject()
                        .put("asset", backgroundMusicAsset?.toJson())
                        .put("uri", backgroundMusicUri)
                        .put(
                            "volume",
                            sanitizeCustomBackgroundMusicVolume(
                                prefs.getFloat(
                                    CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY,
                                    DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME,
                                )
                            ),
                        ),
                )

                val startupAnimationUri = prefs.getString(CUSTOM_STARTUP_ANIMATION_URI_KEY, null)
                val startupAnimationAsset = zip.writeUriAsset(
                    context = appContext,
                    uriString = startupAnimationUri,
                    assetId = "startup_animation",
                    warnings = warnings,
                    budget = assetBudget,
                )
                config.put(
                    "startupAnimation",
                    JSONObject()
                        .put("asset", startupAnimationAsset?.toJson())
                        .put("uri", startupAnimationUri),
                )

                val configBytes = config.toString(2).toByteArray(Charsets.UTF_8)
                require(configBytes.size <= MAX_THEME_STORE_JSON_BYTES) { "Theme package metadata is too large" }
                zip.putNextEntry(ZipEntry("theme.json"))
                zip.write(configBytes)
                zip.closeEntry()
            }
        }
        ThemeStorePackageResult(success = true, warnings = warnings)
    }.getOrElse {
        ThemeStorePackageResult(success = false, warnings = warnings, error = it)
    }
}

fun validateThemeStorePackage(context: Context, source: Uri): ThemeStorePackageResult {
    val appContext = context.applicationContext
    val warnings = mutableListOf<ThemeStorePackageWarning>()
    val tempDir = File(
        appContext.cacheDir,
        "theme-store-validation-${System.nanoTime()}",
    ).apply { mkdirs() }
    return runCatching {
        try {
            val tempAssetsDir = File(tempDir, "assets").apply { mkdirs() }
            val extracted = extractThemeStoreZip(appContext, source, tempDir, tempAssetsDir)
            val config = JSONObject(extracted.themeJson)
            validateThemeStoreConfig(config)
            validateEmbeddedThemeStoreAssets(config, tempAssetsDir)
            collectLegacyThemeStoreUriWarnings(appContext, config, warnings)
            ThemeStorePackageResult(success = true, warnings = warnings)
        } finally {
            tempDir.deleteRecursively()
        }
    }.getOrElse {
        ThemeStorePackageResult(success = false, warnings = warnings, error = it)
    }
}

fun previewThemeStorePackage(context: Context, source: Uri): ThemeStorePackagePreviewResult {
    val appContext = context.applicationContext
    val warnings = mutableListOf<ThemeStorePackageWarning>()
    val tempDir = File(
        appContext.cacheDir,
        "theme-store-preview-${System.nanoTime()}",
    ).apply { mkdirs() }
    return runCatching {
        try {
            val tempAssetsDir = File(tempDir, "assets").apply { mkdirs() }
            val extracted = extractThemeStoreZip(appContext, source, tempDir, tempAssetsDir)
            val config = JSONObject(extracted.themeJson)
            validateThemeStoreConfig(config)
            validateEmbeddedThemeStoreAssets(config, tempAssetsDir)
            collectLegacyThemeStoreUriWarnings(appContext, config, warnings)
            val author = parseThemeStorePackageAuthor(config)?.let { metadata ->
                metadata.copy(
                    avatar = readThemeStorePreviewImage(
                        owner = config.optJSONObject("author"),
                        assetKey = "avatar",
                        tempAssetsDir = tempAssetsDir,
                    )
                )
            }
            ThemeStorePackagePreviewResult(
                success = true,
                preview = ThemeStorePackagePreview(
                    version = config.optInt("version", 0),
                    exportedAt = config.optLong("exportedAt", 0L).coerceAtLeast(0L),
                    configuredResourceCount = countConfiguredThemeStoreResources(config),
                    author = author,
                    cover = findThemeStorePreviewCover(config, tempAssetsDir),
                ),
                warnings = warnings,
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }.getOrElse { error ->
        ThemeStorePackagePreviewResult(
            success = false,
            warnings = warnings,
            error = error,
        )
    }
}

fun importThemeStorePackage(context: Context, source: Uri): ThemeStorePackageResult {
    val appContext = context.applicationContext
    val warnings = mutableListOf<ThemeStorePackageWarning>()
    val tempDir = File(appContext.cacheDir, "theme-store-import").apply {
        deleteRecursively()
        mkdirs()
    }
    var stagingDir: File? = null
    return runCatching {
        try {
            val tempAssetsDir = File(tempDir, "assets").apply { mkdirs() }
            val extracted = extractThemeStoreZip(appContext, source, tempDir, tempAssetsDir)
            val assetBudget = ThemeStoreAssetBudget(extracted.assetsBytes)
            val config = JSONObject(extracted.themeJson)
            validateThemeStoreConfig(config)
            validateEmbeddedThemeStoreAssets(config, tempAssetsDir)

            val themeStoreDir = File(appContext.filesDir, "theme-store").apply { mkdirs() }
            val targetDir = File(themeStoreDir, "current")
            val nextStagingDir = File(themeStoreDir, "import-staging").apply {
                deleteRecursively()
                mkdirs()
            }
            stagingDir = nextStagingDir
            copyDirectoryContents(targetDir, nextStagingDir)
            val stagingAssetsDir = File(nextStagingDir, "assets").apply { mkdirs() }
            val targetAssetsDir = File(targetDir, "assets")
            val cardsJson = config.optJSONObject("cards") ?: JSONObject()
            val pendingCards = mutableMapOf<ThemeStoreImageSlot, ThemeStoreImageState>()
            val navigationIconsJson = config.optJSONObject("navigationIcons") ?: JSONObject()
            val pendingNavigationIcons = mutableMapOf<CustomNavigationIconSlot, Pair<String?, CustomWallpaperCrop>>()
            val pageBackgroundsJson = config.optJSONObject("pageBackgrounds") ?: JSONObject()
            val pendingPageBackgrounds = mutableMapOf<CustomPageBackgroundTarget, CustomBackgroundState>()
            var pendingWallpaper: ThemeStoreWallpaperState? = null
            var hasStartupSound = false
            var pendingStartupSoundUri: String? = null
            var pendingStartupSoundDurationSeconds: Int? = null
            var pendingStartupSoundVolume: Float? = null
            var hasClickSound = false
            var pendingClickSoundUri: String? = null
            var pendingClickSoundVolume = DEFAULT_CUSTOM_AUDIO_VOLUME
            var hasBackgroundMusic = false
            var pendingBackgroundMusicUri: String? = null
            var pendingBackgroundMusicVolume = DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME
            var hasStartupAnimation = false
            var pendingStartupAnimationUri: String? = null

            ThemeStoreImageSlot.entries.forEach { slot ->
                val slotJson = cardsJson.optJSONObject(slot.id)
                if (slotJson != null) {
                    val importedImage = importAssetUri(
                        context = appContext,
                        assetOwnerJson = slotJson,
                        tempAssetsDir = tempAssetsDir,
                        stagingAssetsDir = stagingAssetsDir,
                        targetAssetsDir = targetAssetsDir,
                        assetId = "card_${slot.id}",
                        warnings = warnings,
                        budget = assetBudget,
                    )
                    val importedVideo = if (slot.videoUriKey != null) {
                        importAssetUri(
                            context = appContext,
                            assetOwnerJson = slotJson,
                            tempAssetsDir = tempAssetsDir,
                            stagingAssetsDir = stagingAssetsDir,
                            targetAssetsDir = targetAssetsDir,
                            assetId = "card_${slot.id}_video",
                            warnings = warnings,
                            budget = assetBudget,
                            assetKey = "videoAsset",
                            uriKey = "videoUri",
                        )
                    } else {
                        ImportedThemeAsset.Resolved(null)
                    }
                    if (importedImage is ImportedThemeAsset.Unavailable ||
                        importedVideo is ImportedThemeAsset.Unavailable
                    ) {
                        return@forEach
                    }
                    val importedUri = (importedImage as ImportedThemeAsset.Resolved).uriString
                    val importedVideoUri = (importedVideo as ImportedThemeAsset.Resolved).uriString
                    pendingCards[slot] = ThemeStoreImageState(
                        uriString = importedUri.takeUnless { !importedVideoUri.isNullOrBlank() },
                        videoUriString = importedVideoUri,
                        crop = slotJson.optCrop("crop", DEFAULT_CUSTOM_WALLPAPER_CROP),
                    )
                }
            }

            CustomNavigationIconSlot.entries.forEach { slot ->
                val slotJson = navigationIconsJson.optJSONObject(slot.id)
                if (slotJson != null) {
                    val importedAsset = importAssetUri(
                        context = appContext,
                        assetOwnerJson = slotJson,
                        tempAssetsDir = tempAssetsDir,
                        stagingAssetsDir = stagingAssetsDir,
                        targetAssetsDir = targetAssetsDir,
                        assetId = "navigation_icon_${slot.id}",
                        warnings = warnings,
                        budget = assetBudget,
                    )
                    if (importedAsset is ImportedThemeAsset.Unavailable) return@forEach
                    pendingNavigationIcons[slot] =
                        (importedAsset as ImportedThemeAsset.Resolved).uriString to slotJson.optCrop(
                        "crop",
                        DEFAULT_CUSTOM_NAVIGATION_ICON_CROP,
                    )
                }
            }

            CustomPageBackgroundTarget.entries.forEach { target ->
                val targetJson = pageBackgroundsJson.optJSONObject(target.id)
                if (targetJson != null) {
                    val importedImage = importAssetUri(
                        context = appContext,
                        assetOwnerJson = targetJson,
                        tempAssetsDir = tempAssetsDir,
                        stagingAssetsDir = stagingAssetsDir,
                        targetAssetsDir = targetAssetsDir,
                        assetId = "page_background_${target.id}",
                        warnings = warnings,
                        budget = assetBudget,
                    )
                    val importedVideo = importAssetUri(
                        context = appContext,
                        assetOwnerJson = targetJson,
                        tempAssetsDir = tempAssetsDir,
                        stagingAssetsDir = stagingAssetsDir,
                        targetAssetsDir = targetAssetsDir,
                        assetId = "page_background_${target.id}_video",
                        warnings = warnings,
                        budget = assetBudget,
                        assetKey = "videoAsset",
                        uriKey = "videoUri",
                    )
                    if (importedImage is ImportedThemeAsset.Unavailable ||
                        importedVideo is ImportedThemeAsset.Unavailable
                    ) {
                        return@forEach
                    }
                    val importedUri = (importedImage as ImportedThemeAsset.Resolved).uriString
                    val importedVideoUri = (importedVideo as ImportedThemeAsset.Resolved).uriString
                    pendingPageBackgrounds[target] = CustomBackgroundState(
                        wallpaperUriString = importedUri.takeUnless { !importedVideoUri.isNullOrBlank() },
                        videoUriString = importedVideoUri,
                        opacity = sanitizeCustomWallpaperOpacity(
                            targetJson.optDouble(
                                "opacity",
                                DEFAULT_CUSTOM_WALLPAPER_OPACITY.toDouble(),
                            ).toFloat()
                        ),
                        crop = targetJson.optCrop("crop", DEFAULT_CUSTOM_WALLPAPER_CROP),
                        videoDurationSeconds = sanitizeCustomVideoBackgroundDurationSeconds(
                            targetJson.optInt(
                                "videoDurationSeconds",
                                DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
                            )
                        ),
                    )
                }
            }

            config.optJSONObject("wallpaper")?.let { wallpaperJson ->
                val importedImage = importAssetUri(
                    context = appContext,
                    assetOwnerJson = wallpaperJson,
                    tempAssetsDir = tempAssetsDir,
                    stagingAssetsDir = stagingAssetsDir,
                    targetAssetsDir = targetAssetsDir,
                    assetId = "custom_wallpaper",
                    warnings = warnings,
                    budget = assetBudget,
                )
                val importedVideo = importAssetUri(
                    context = appContext,
                    assetOwnerJson = wallpaperJson,
                    tempAssetsDir = tempAssetsDir,
                    stagingAssetsDir = stagingAssetsDir,
                    targetAssetsDir = targetAssetsDir,
                    assetId = "custom_video_background",
                    warnings = warnings,
                    budget = assetBudget,
                    assetKey = "videoAsset",
                    uriKey = "videoUri",
                )
                if (importedImage is ImportedThemeAsset.Unavailable ||
                    importedVideo is ImportedThemeAsset.Unavailable
                ) {
                    return@let
                }
                val importedUri = (importedImage as ImportedThemeAsset.Resolved).uriString
                val importedVideoUri = (importedVideo as ImportedThemeAsset.Resolved).uriString
                pendingWallpaper = ThemeStoreWallpaperState(
                    uriString = importedUri,
                    videoUriString = importedVideoUri,
                    videoDurationSeconds = sanitizeCustomVideoBackgroundDurationSeconds(
                        wallpaperJson.optInt(
                            "videoDurationSeconds",
                            DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
                        )
                    ),
                    opacity = sanitizeCustomWallpaperOpacity(
                        wallpaperJson.optDouble(
                            "opacity",
                            DEFAULT_CUSTOM_WALLPAPER_OPACITY.toDouble(),
                        ).toFloat()
                    ),
                    crop = wallpaperJson.optCrop("crop", DEFAULT_CUSTOM_WALLPAPER_CROP),
                    passthroughEnabled = wallpaperJson.optBoolean("passthroughEnabled", false),
                    passthroughOpacity = sanitizeCustomWallpaperPassthroughOpacity(
                        wallpaperJson.optDouble(
                            "passthroughOpacity",
                            DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY.toDouble(),
                        ).toFloat()
                    ),
                )
            }

            config.optJSONObject("startupSound")?.let { soundJson ->
                val importedAsset = importAssetUri(
                    context = appContext,
                    assetOwnerJson = soundJson,
                    tempAssetsDir = tempAssetsDir,
                    stagingAssetsDir = stagingAssetsDir,
                    targetAssetsDir = targetAssetsDir,
                    assetId = "startup_sound",
                    warnings = warnings,
                    budget = assetBudget,
                )
                if (importedAsset is ImportedThemeAsset.Unavailable) return@let
                hasStartupSound = true
                pendingStartupSoundUri = (importedAsset as ImportedThemeAsset.Resolved).uriString
                if (soundJson.has("durationSeconds")) {
                    pendingStartupSoundDurationSeconds = sanitizeCustomStartupSoundDurationSeconds(
                        soundJson.optInt(
                            "durationSeconds",
                            DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
                        )
                    )
                }
                if (soundJson.has("volume")) {
                    pendingStartupSoundVolume = sanitizeCustomAudioVolume(
                        soundJson.optDouble(
                            "volume",
                            DEFAULT_CUSTOM_AUDIO_VOLUME.toDouble(),
                        ).toFloat()
                    )
                }
            }

            config.optJSONObject("clickSound")?.let { soundJson ->
                val importedAsset = importAssetUri(
                    context = appContext,
                    assetOwnerJson = soundJson,
                    tempAssetsDir = tempAssetsDir,
                    stagingAssetsDir = stagingAssetsDir,
                    targetAssetsDir = targetAssetsDir,
                    assetId = "click_sound",
                    warnings = warnings,
                    budget = assetBudget,
                )
                if (importedAsset is ImportedThemeAsset.Unavailable) return@let
                hasClickSound = true
                pendingClickSoundUri = (importedAsset as ImportedThemeAsset.Resolved).uriString
                pendingClickSoundVolume = sanitizeCustomAudioVolume(
                    soundJson.optDouble(
                        "volume",
                        DEFAULT_CUSTOM_AUDIO_VOLUME.toDouble(),
                    ).toFloat()
                )
            }

            config.optJSONObject("backgroundMusic")?.let { musicJson ->
                val importedAsset = importAssetUri(
                    context = appContext,
                    assetOwnerJson = musicJson,
                    tempAssetsDir = tempAssetsDir,
                    stagingAssetsDir = stagingAssetsDir,
                    targetAssetsDir = targetAssetsDir,
                    assetId = "background_music",
                    warnings = warnings,
                    budget = assetBudget,
                )
                if (importedAsset is ImportedThemeAsset.Unavailable) return@let
                hasBackgroundMusic = true
                pendingBackgroundMusicUri = (importedAsset as ImportedThemeAsset.Resolved).uriString
                pendingBackgroundMusicVolume = sanitizeCustomBackgroundMusicVolume(
                    musicJson.optDouble(
                        "volume",
                        DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME.toDouble(),
                    ).toFloat()
                )
            }

            config.optJSONObject("startupAnimation")?.let { animationJson ->
                val importedAsset = importAssetUri(
                    context = appContext,
                    assetOwnerJson = animationJson,
                    tempAssetsDir = tempAssetsDir,
                    stagingAssetsDir = stagingAssetsDir,
                    targetAssetsDir = targetAssetsDir,
                    assetId = "startup_animation",
                    warnings = warnings,
                    budget = assetBudget,
                )
                if (importedAsset is ImportedThemeAsset.Unavailable) return@let
                hasStartupAnimation = true
                pendingStartupAnimationUri = (importedAsset as ImportedThemeAsset.Resolved).uriString
            }

            val previousSummary = readThemeStoreSummary(appContext)
            val directorySwap = beginThemeStoreDirectorySwap(targetDir, nextStagingDir)
            stagingDir = null

            val prefs = themeStorePrefs(appContext)
            val editor = prefs.edit()
            pendingCards.forEach { (slot, pending) ->
                val importedUri = pending.uriString.takeUnless { pending.hasVideoSelected }
                val importedVideoUri = pending.videoUriString.takeIf { slot.videoUriKey != null }
                if (importedUri.isNullOrBlank()) {
                    editor.remove(slot.uriKey)
                    editor.removeImageSlotCrop(slot)
                } else {
                    editor.putString(slot.uriKey, importedUri)
                    editor.putImageSlotCrop(slot, pending.crop)
                }
                slot.videoUriKey?.let { videoUriKey ->
                    if (importedVideoUri.isNullOrBlank()) {
                        editor.remove(videoUriKey)
                    } else {
                        editor.remove(slot.uriKey)
                        editor.putImageSlotCrop(slot, pending.crop)
                        editor.putString(videoUriKey, importedVideoUri)
                    }
                }
            }

            pendingNavigationIcons.forEach { (slot, pending) ->
                val (importedUri, crop) = pending
                if (importedUri.isNullOrBlank()) {
                    editor.remove(slot.uriKey)
                    editor.removeCustomNavigationIconCrop(slot)
                } else {
                    editor.putString(slot.uriKey, importedUri)
                    editor.putCustomNavigationIconCrop(slot, crop)
                }
            }

            pendingPageBackgrounds.forEach { (target, pending) ->
                editor.putImportedPageBackground(target, pending)
            }

            pendingWallpaper?.let { wallpaper ->
                editor.putImportedWallpaper(wallpaper)
            }

            if (hasStartupSound) {
                editor.putOptionalString(CUSTOM_STARTUP_SOUND_URI_KEY, pendingStartupSoundUri)
                pendingStartupSoundDurationSeconds?.let { durationSeconds ->
                    editor.putInt(CUSTOM_STARTUP_SOUND_DURATION_SECONDS_KEY, durationSeconds)
                }
                pendingStartupSoundVolume?.let { volume ->
                    editor.putFloat(CUSTOM_STARTUP_SOUND_VOLUME_KEY, volume)
                }
            }

            if (hasClickSound) {
                editor.putOptionalString(CUSTOM_CLICK_SOUND_URI_KEY, pendingClickSoundUri)
                editor.putFloat(CUSTOM_CLICK_SOUND_VOLUME_KEY, pendingClickSoundVolume)
            }

            if (hasBackgroundMusic) {
                editor.putOptionalString(CUSTOM_BACKGROUND_MUSIC_URI_KEY, pendingBackgroundMusicUri)
                editor.putFloat(CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY, pendingBackgroundMusicVolume)
            }

            if (hasStartupAnimation) {
                editor.putOptionalString(CUSTOM_STARTUP_ANIMATION_URI_KEY, pendingStartupAnimationUri)
            }

            var preferencesCommitted = false
            try {
                require(editor.commit()) { "Unable to save imported theme settings" }
                preferencesCommitted = true
            } finally {
                if (!preferencesCommitted) {
                    directorySwap.rollback()
                }
            }
            if (!directorySwap.finish()) {
                warnings += ThemeStorePackageWarning("previous_theme_backup")
            }
            releaseReplacedThemeStoreReferences(
                context = appContext,
                previous = previousSummary,
                current = readThemeStoreSummary(appContext),
            )

            ThemeStorePackageResult(success = true, warnings = warnings)
        } finally {
            tempDir.deleteRecursively()
            stagingDir?.deleteRecursively()
        }
    }.getOrElse {
        ThemeStorePackageResult(success = false, warnings = warnings, error = it)
    }
}

private fun themeStorePrefs(context: Context): SharedPreferences {
    return context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
}

private fun SharedPreferences.Editor.putImportedPageBackground(
    target: CustomPageBackgroundTarget,
    state: CustomBackgroundState,
) {
    when {
        state.hasVideo -> {
            remove(target.wallpaperUriKey)
            putString(target.videoUriKey, state.videoUriString)
        }

        state.hasWallpaper -> {
            putString(target.wallpaperUriKey, state.wallpaperUriString)
            remove(target.videoUriKey)
        }

        else -> {
            remove(target.wallpaperUriKey)
            remove(target.videoUriKey)
            remove(target.opacityKey)
            remove(target.videoDurationSecondsKey)
            removeImportedPageBackgroundCrop(target)
            return
        }
    }
    putFloat(target.opacityKey, sanitizeCustomWallpaperOpacity(state.opacity))
    putImportedPageBackgroundCrop(target, state.crop)
    putInt(
        target.videoDurationSecondsKey,
        sanitizeCustomVideoBackgroundDurationSeconds(state.videoDurationSeconds),
    )
}

private fun SharedPreferences.Editor.putImportedPageBackgroundCrop(
    target: CustomPageBackgroundTarget,
    crop: CustomWallpaperCrop,
) {
    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    putFloat(target.cropLeftKey, safeCrop.left)
    putFloat(target.cropTopKey, safeCrop.top)
    putFloat(target.cropRightKey, safeCrop.right)
    putFloat(target.cropBottomKey, safeCrop.bottom)
}

private fun SharedPreferences.Editor.removeImportedPageBackgroundCrop(target: CustomPageBackgroundTarget) {
    remove(target.cropLeftKey)
    remove(target.cropTopKey)
    remove(target.cropRightKey)
    remove(target.cropBottomKey)
}

private fun SharedPreferences.Editor.putImportedWallpaper(state: ThemeStoreWallpaperState) {
    val wallpaperUri = state.uriString.takeUnless { state.hasVideoSelected }
    if (wallpaperUri.isNullOrBlank()) {
        remove(CUSTOM_WALLPAPER_URI_KEY)
        removeCustomWallpaperCrop()
    } else {
        putString(CUSTOM_WALLPAPER_URI_KEY, wallpaperUri)
        putCustomWallpaperCrop(state.crop)
    }
    putOptionalString(CUSTOM_VIDEO_BACKGROUND_URI_KEY, state.videoUriString)
    putInt(
        CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS_KEY,
        sanitizeCustomVideoBackgroundDurationSeconds(state.videoDurationSeconds),
    )
    putFloat(CUSTOM_WALLPAPER_OPACITY_KEY, sanitizeCustomWallpaperOpacity(state.opacity))
    putBoolean(CUSTOM_WALLPAPER_PASSTHROUGH_ENABLED_KEY, state.passthroughEnabled)
    putFloat(
        CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY_KEY,
        sanitizeCustomWallpaperPassthroughOpacity(state.passthroughOpacity),
    )
}

private fun SharedPreferences.Editor.putOptionalString(key: String, value: String?) {
    if (value.isNullOrBlank()) {
        remove(key)
    } else {
        putString(key, value)
    }
}

private fun releaseReplacedThemeStoreReferences(
    context: Context,
    previous: ThemeStoreSummary,
    current: ThemeStoreSummary,
) {
    ThemeStoreImageSlot.entries.forEach { slot ->
        val oldState = previous.imageState(slot)
        val newState = current.imageState(slot)
        if (oldState.uriString != newState.uriString) {
            releaseCustomImageReference(context, oldState.uriString)
        }
        if (oldState.videoUriString != newState.videoUriString) {
            releasePersistableVideoBackgroundReadPermission(context, oldState.videoUriString)
        }
    }
    CustomNavigationIconSlot.entries.forEach { slot ->
        val oldUri = previous.navigationIcons[slot].uriString
        if (oldUri != current.navigationIcons[slot].uriString) {
            releaseCustomImageReference(context, oldUri)
        }
    }
    CustomPageBackgroundTarget.entries.forEach { target ->
        val oldState = previous.pageBackgrounds[target]
        val newState = current.pageBackgrounds[target]
        if (oldState.wallpaperUriString != newState.wallpaperUriString) {
            releaseCustomImageReference(context, oldState.wallpaperUriString)
        }
        if (oldState.videoUriString != newState.videoUriString) {
            releasePersistableVideoBackgroundReadPermission(context, oldState.videoUriString)
        }
    }
    if (previous.wallpaper.uriString != current.wallpaper.uriString) {
        releaseCustomImageReference(context, previous.wallpaper.uriString)
    }
    if (previous.wallpaper.videoUriString != current.wallpaper.videoUriString) {
        releasePersistableVideoBackgroundReadPermission(context, previous.wallpaper.videoUriString)
    }
    if (previous.startupSoundUri != current.startupSoundUri) {
        releasePersistableAudioReadPermission(context, previous.startupSoundUri)
    }
    if (previous.audio.clickSoundUri != current.audio.clickSoundUri) {
        releasePersistableAudioReadPermission(context, previous.audio.clickSoundUri)
    }
    if (previous.audio.backgroundMusicUri != current.audio.backgroundMusicUri) {
        releasePersistableAudioReadPermission(context, previous.audio.backgroundMusicUri)
    }
    if (previous.startupAnimationUri != current.startupAnimationUri) {
        releasePersistableStartupAnimationReadPermission(context, previous.startupAnimationUri)
    }
}

private fun ThemeStoreSummary.imageState(slot: ThemeStoreImageSlot): ThemeStoreImageState {
    return when (slot) {
        ThemeStoreImageSlot.Lkm -> lkmCard
        ThemeStoreImageSlot.Superuser -> superuserCard
        ThemeStoreImageSlot.Module -> moduleCard
        ThemeStoreImageSlot.StatusMonitor -> statusMonitorCard
        ThemeStoreImageSlot.SystemInfo -> systemInfoCard
        ThemeStoreImageSlot.RebootMenu -> rebootMenuCard
    }
}

private fun SharedPreferences.readImageSlot(slot: ThemeStoreImageSlot): ThemeStoreImageState {
    return ThemeStoreImageState(
        uriString = getString(slot.uriKey, null),
        videoUriString = slot.videoUriKey?.let { getString(it, null) },
        crop = sanitizeCustomWallpaperCrop(
            CustomWallpaperCrop(
                left = getFloat(slot.cropLeftKey, DEFAULT_CUSTOM_WALLPAPER_CROP.left),
                top = getFloat(slot.cropTopKey, DEFAULT_CUSTOM_WALLPAPER_CROP.top),
                right = getFloat(slot.cropRightKey, DEFAULT_CUSTOM_WALLPAPER_CROP.right),
                bottom = getFloat(slot.cropBottomKey, DEFAULT_CUSTOM_WALLPAPER_CROP.bottom),
            )
        ),
    )
}

private fun SharedPreferences.readCustomWallpaperCrop(): CustomWallpaperCrop {
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = getFloat(CUSTOM_WALLPAPER_CROP_LEFT_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.left),
            top = getFloat(CUSTOM_WALLPAPER_CROP_TOP_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.top),
            right = getFloat(CUSTOM_WALLPAPER_CROP_RIGHT_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.right),
            bottom = getFloat(CUSTOM_WALLPAPER_CROP_BOTTOM_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.bottom),
        )
    )
}

private fun SharedPreferences.Editor.putImageSlotCrop(slot: ThemeStoreImageSlot, crop: CustomWallpaperCrop) {
    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    putFloat(slot.cropLeftKey, safeCrop.left)
    putFloat(slot.cropTopKey, safeCrop.top)
    putFloat(slot.cropRightKey, safeCrop.right)
    putFloat(slot.cropBottomKey, safeCrop.bottom)
}

private fun SharedPreferences.Editor.removeImageSlotCrop(slot: ThemeStoreImageSlot) {
    remove(slot.cropLeftKey)
    remove(slot.cropTopKey)
    remove(slot.cropRightKey)
    remove(slot.cropBottomKey)
}

private fun SharedPreferences.Editor.putCustomWallpaperCrop(crop: CustomWallpaperCrop) {
    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    putFloat(CUSTOM_WALLPAPER_CROP_LEFT_KEY, safeCrop.left)
    putFloat(CUSTOM_WALLPAPER_CROP_TOP_KEY, safeCrop.top)
    putFloat(CUSTOM_WALLPAPER_CROP_RIGHT_KEY, safeCrop.right)
    putFloat(CUSTOM_WALLPAPER_CROP_BOTTOM_KEY, safeCrop.bottom)
}

private fun SharedPreferences.Editor.removeCustomWallpaperCrop() {
    remove(CUSTOM_WALLPAPER_CROP_LEFT_KEY)
    remove(CUSTOM_WALLPAPER_CROP_TOP_KEY)
    remove(CUSTOM_WALLPAPER_CROP_RIGHT_KEY)
    remove(CUSTOM_WALLPAPER_CROP_BOTTOM_KEY)
}

private data class ExportedThemeAsset(
    val path: String,
    val displayName: String?,
    val mimeType: String?,
)

private fun ExportedThemeAsset.toJson(): JSONObject {
    return JSONObject()
        .put("path", path)
        .put("displayName", displayName)
        .put("mimeType", mimeType)
}

private fun ZipOutputStream.writeUriAsset(
    context: Context,
    uriString: String?,
    assetId: String,
    warnings: MutableList<ThemeStorePackageWarning>,
    budget: ThemeStoreAssetBudget,
): ExportedThemeAsset? {
    if (uriString.isNullOrBlank()) return null
    val uri = Uri.parse(uriString)
    val displayName = queryDisplayName(context, uri) ?: uri.lastPathSegment
    val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    val extension = safeAssetExtension(displayName, mimeType)
    val path = "assets/$assetId$extension"

    var entryOpen = false
    return runCatching {
        openThemeStoreUriInputStream(context, uri).use { input ->
            putNextEntry(ZipEntry(path))
            entryOpen = true
            input.copyThemeStoreAssetTo(this, budget)
            closeEntry()
            entryOpen = false
        }
        ExportedThemeAsset(
            path = path,
            displayName = displayName,
            mimeType = mimeType,
        )
    }.getOrElse {
        if (entryOpen) {
            runCatching { closeEntry() }
        }
        warnings += ThemeStorePackageWarning(
            assetId = assetId,
            reason = it.message?.lineSequence()?.firstOrNull()?.take(160),
        )
        null
    }
}

private fun InputStream.copyThemeStoreAssetTo(
    output: OutputStream,
    budget: ThemeStoreAssetBudget,
): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copied = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        val nextAssetBytes = copied + read
        val nextTotalBytes = budget.totalBytes + read
        require(nextAssetBytes <= MAX_THEME_STORE_ASSET_BYTES) { "Theme package asset is too large" }
        require(nextTotalBytes <= MAX_THEME_STORE_ASSETS_BYTES) { "Theme package assets are too large" }
        output.write(buffer, 0, read)
        copied = nextAssetBytes
        budget.totalBytes = nextTotalBytes
    }
    return copied
}

private fun extractThemeStoreZip(
    context: Context,
    source: Uri,
    tempDir: File,
    tempAssetsDir: File,
): ExtractedThemeStoreArchive {
    return openThemeStoreUriInputStream(context, source).use { input ->
        extractThemeStoreArchive(input, tempDir, tempAssetsDir)
    }
}

internal fun extractThemeStoreArchive(
    input: InputStream,
    tempDir: File,
    tempAssetsDir: File,
): ExtractedThemeStoreArchive {
    var themeJson: String? = null
    var entryCount = 0
    var totalAssetsBytes = 0L
    val seenEntries = mutableSetOf<String>()
    ZipInputStream(input.buffered()).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            entryCount++
            require(entryCount <= MAX_THEME_STORE_ENTRY_COUNT) { "Theme package has too many entries" }
            val entryName = validateThemeStoreArchiveEntryName(entry.name, entry.isDirectory)
            require(seenEntries.add(entryName)) { "Theme package contains duplicate entry: $entryName" }
            if (entry.isDirectory) {
                zip.closeEntry()
                continue
            }
            when {
                entryName == "theme.json" -> {
                    require(themeJson == null) { "Theme package contains duplicate theme.json" }
                    themeJson = zip.readEntryBytes(MAX_THEME_STORE_JSON_BYTES).toString(Charsets.UTF_8)
                }

                entryName.startsWith("assets/") -> {
                    val outputFile = safeAssetFile(tempAssetsDir, entryName.removePrefix("assets/"))
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { output ->
                        val copied = zip.copyEntryTo(output, MAX_THEME_STORE_ASSET_BYTES)
                        totalAssetsBytes += copied
                        require(totalAssetsBytes <= MAX_THEME_STORE_ASSETS_BYTES) {
                            "Theme package assets are too large"
                        }
                    }
                }
            }
            zip.closeEntry()
        }
    }

    return ExtractedThemeStoreArchive(
        themeJson = themeJson ?: error("theme.json not found in ${tempDir.name}"),
        assetsBytes = totalAssetsBytes,
    )
}

internal fun validateThemeStoreArchiveEntryName(entryName: String, directory: Boolean = false): String {
    require(entryName.isNotBlank()) { "Theme package contains an empty entry name" }
    require('\u0000' !in entryName && '\\' !in entryName) { "Theme package contains an invalid entry path" }
    require(!entryName.startsWith('/')) { "Theme package contains an absolute entry path" }
    val normalized = if (directory) entryName.removeSuffix("/") else entryName
    require(normalized.isNotBlank()) { "Theme package contains an invalid entry path" }
    val segments = normalized.split('/')
    require(segments.all { it.isNotBlank() && it != "." && it != ".." && ':' !in it }) {
        "Theme package contains an invalid entry path"
    }
    return normalized
}

internal fun validateThemeStoreConfig(config: JSONObject) {
    require(config.optString("schema") == THEME_STORE_SCHEMA) { "Unsupported theme package" }
    val version = config.optInt("version", 0)
    require(version in 1..THEME_STORE_VERSION) {
        "Unsupported theme package version"
    }
    if (version >= 3) {
        listOf("startupSound", "clickSound", "backgroundMusic", "startupAnimation").forEach { key ->
            require(config.optJSONObject(key) != null) { "Theme package is missing $key" }
        }
        val startupSound = config.getJSONObject("startupSound")
        val clickSound = config.getJSONObject("clickSound")
        val backgroundMusic = config.getJSONObject("backgroundMusic")
        require(startupSound.opt("durationSeconds") is Number) {
            "Theme package is missing startup sound duration"
        }
        require(startupSound.opt("volume") is Number) {
            "Theme package is missing startup sound volume"
        }
        require(clickSound.opt("volume") is Number) {
            "Theme package is missing click sound volume"
        }
        require(backgroundMusic.opt("volume") is Number) {
            "Theme package is missing background music volume"
        }
    }
    if (version >= 4) {
        val author = config.optJSONObject("author")
            ?: error("Theme package is missing author information")
        listOf("displayName", "realName", "gender", "bio").forEach { key ->
            require(author.opt(key) is String) { "Theme package has invalid author information" }
        }
        require(author.getString("displayName").length <= 64) { "Theme author name is too long" }
        require(author.getString("realName").length <= 64) { "Theme author name is too long" }
        require(author.getString("bio").length <= 512) { "Theme author bio is too long" }
        require(
            ThemeAuthorGender.entries.any {
                it.storageValue == author.getString("gender")
            }
        ) { "Theme author gender is invalid" }
    }
}

internal fun validateEmbeddedThemeStoreAssets(config: JSONObject, tempAssetsDir: File) {
    fun validateOwner(owner: JSONObject?, vararg assetKeys: String) {
        if (owner == null) return
        assetKeys.forEach { assetKey ->
            val rawAsset = owner.opt(assetKey)
            if (rawAsset == null || rawAsset === JSONObject.NULL) return@forEach
            require(rawAsset is JSONObject) { "Invalid embedded asset metadata" }
            val path = rawAsset.optString("path")
            require(path.startsWith("assets/")) { "Invalid embedded asset path" }
            val relativePath = path.removePrefix("assets/")
            require(safeAssetFile(tempAssetsDir, relativePath).isFile) {
                "Embedded asset is missing: $relativePath"
            }
        }
    }

    val cards = config.optJSONObject("cards")
    ThemeStoreImageSlot.entries.forEach { slot ->
        validateOwner(cards?.optJSONObject(slot.id), "asset", "videoAsset")
    }
    val navigationIcons = config.optJSONObject("navigationIcons")
    CustomNavigationIconSlot.entries.forEach { slot ->
        validateOwner(navigationIcons?.optJSONObject(slot.id), "asset")
    }
    val pageBackgrounds = config.optJSONObject("pageBackgrounds")
    CustomPageBackgroundTarget.entries.forEach { target ->
        validateOwner(pageBackgrounds?.optJSONObject(target.id), "asset", "videoAsset")
    }
    validateOwner(config.optJSONObject("wallpaper"), "asset", "videoAsset")
    validateOwner(config.optJSONObject("startupSound"), "asset")
    validateOwner(config.optJSONObject("clickSound"), "asset")
    validateOwner(config.optJSONObject("backgroundMusic"), "asset")
    validateOwner(config.optJSONObject("startupAnimation"), "asset")
    validateOwner(config.optJSONObject("author"), "avatar")
    config.optJSONObject("author")
        ?.optJSONObject("avatar")
        ?.optString("path")
        ?.takeIf { it.startsWith("assets/") }
        ?.let { path ->
            val avatarFile = safeAssetFile(tempAssetsDir, path.removePrefix("assets/"))
            require(avatarFile.length() <= MAX_THEME_STORE_PREVIEW_IMAGE_BYTES) {
                "Theme author avatar is too large"
            }
        }
}

internal fun parseThemeStorePackageAuthor(config: JSONObject): ThemeStorePackageAuthor? {
    if (config.optInt("version", 0) < 4) return null
    val author = config.optJSONObject("author") ?: return null
    val sanitized = sanitizeThemeAuthorProfile(
        ThemeAuthorProfile(
            displayName = author.optString("displayName"),
            realName = author.optString("realName"),
            gender = ThemeAuthorGender.fromStorageValue(author.optString("gender")),
            bio = author.optString("bio"),
        )
    )
    return ThemeStorePackageAuthor(
        displayName = sanitized.displayName,
        realName = sanitized.realName,
        gender = sanitized.gender,
        bio = sanitized.bio,
    )
}

internal fun countConfiguredThemeStoreResources(config: JSONObject): Int {
    fun hasResource(
        owner: JSONObject?,
        assetKey: String = "asset",
        uriKey: String = "uri",
    ): Boolean {
        if (owner == null) return false
        return owner.opt(assetKey) is JSONObject || owner.optString(uriKey).isNotBlank()
    }

    fun hasImageOrVideo(owner: JSONObject?): Boolean {
        return hasResource(owner) || hasResource(owner, "videoAsset", "videoUri")
    }

    var count = 0
    val cards = config.optJSONObject("cards")
    ThemeStoreImageSlot.entries.forEach { slot ->
        if (hasImageOrVideo(cards?.optJSONObject(slot.id))) count++
    }
    val navigationIcons = config.optJSONObject("navigationIcons")
    CustomNavigationIconSlot.entries.forEach { slot ->
        if (hasResource(navigationIcons?.optJSONObject(slot.id))) count++
    }
    val pageBackgrounds = config.optJSONObject("pageBackgrounds")
    CustomPageBackgroundTarget.entries.forEach { target ->
        if (hasImageOrVideo(pageBackgrounds?.optJSONObject(target.id))) count++
    }
    if (hasImageOrVideo(config.optJSONObject("wallpaper"))) count++
    if (hasResource(config.optJSONObject("startupSound"))) count++
    if (hasResource(config.optJSONObject("clickSound"))) count++
    if (hasResource(config.optJSONObject("backgroundMusic"))) count++
    if (hasResource(config.optJSONObject("startupAnimation"))) count++
    return count
}

private fun findThemeStorePreviewCover(
    config: JSONObject,
    tempAssetsDir: File,
): ThemeStorePackagePreviewImage? {
    val owners = buildList {
        add(config.optJSONObject("wallpaper"))
        val pageBackgrounds = config.optJSONObject("pageBackgrounds")
        CustomPageBackgroundTarget.entries.forEach { target ->
            add(pageBackgrounds?.optJSONObject(target.id))
        }
        val cards = config.optJSONObject("cards")
        ThemeStoreImageSlot.entries.forEach { slot ->
            add(cards?.optJSONObject(slot.id))
        }
        val navigationIcons = config.optJSONObject("navigationIcons")
        CustomNavigationIconSlot.entries.forEach { slot ->
            add(navigationIcons?.optJSONObject(slot.id))
        }
    }
    return owners.firstNotNullOfOrNull { owner ->
        readThemeStorePreviewImage(owner, "asset", tempAssetsDir)
    }
}

private fun readThemeStorePreviewImage(
    owner: JSONObject?,
    assetKey: String,
    tempAssetsDir: File,
): ThemeStorePackagePreviewImage? {
    val asset = owner?.optJSONObject(assetKey) ?: return null
    val path = asset.optString("path")
    if (!path.startsWith("assets/")) return null
    val file = safeAssetFile(tempAssetsDir, path.removePrefix("assets/"))
    if (!file.isFile || file.length() !in 1..MAX_THEME_STORE_PREVIEW_IMAGE_BYTES) return null
    return ThemeStorePackagePreviewImage(
        bytes = file.readBytes(),
        mimeType = asset.optString("mimeType").takeIf(String::isNotBlank),
    )
}

private fun collectLegacyThemeStoreUriWarnings(
    context: Context,
    config: JSONObject,
    warnings: MutableList<ThemeStorePackageWarning>,
) {
    fun checkOwner(
        owner: JSONObject?,
        assetId: String,
        assetKey: String = "asset",
        uriKey: String = "uri",
    ) {
        if (owner == null) return
        val embedded = owner.opt(assetKey)
        if (embedded != null && embedded !== JSONObject.NULL) return
        val uriString = owner.optString(uriKey).takeIf { it.isNotBlank() } ?: return
        runCatching {
            openThemeStoreUriInputStream(context, Uri.parse(uriString)).use { }
        }.onFailure { error ->
            warnings += ThemeStorePackageWarning(
                assetId = assetId,
                reason = error.message?.lineSequence()?.firstOrNull()?.take(160),
            )
        }
    }

    val cards = config.optJSONObject("cards")
    ThemeStoreImageSlot.entries.forEach { slot ->
        val owner = cards?.optJSONObject(slot.id)
        checkOwner(owner, "card_${slot.id}")
        checkOwner(owner, "card_${slot.id}_video", "videoAsset", "videoUri")
    }
    val navigationIcons = config.optJSONObject("navigationIcons")
    CustomNavigationIconSlot.entries.forEach { slot ->
        checkOwner(navigationIcons?.optJSONObject(slot.id), "navigation_icon_${slot.id}")
    }
    val pageBackgrounds = config.optJSONObject("pageBackgrounds")
    CustomPageBackgroundTarget.entries.forEach { target ->
        val owner = pageBackgrounds?.optJSONObject(target.id)
        checkOwner(owner, "page_background_${target.id}")
        checkOwner(owner, "page_background_${target.id}_video", "videoAsset", "videoUri")
    }
    config.optJSONObject("wallpaper")?.let { owner ->
        checkOwner(owner, "custom_wallpaper")
        checkOwner(owner, "custom_video_background", "videoAsset", "videoUri")
    }
    checkOwner(config.optJSONObject("startupSound"), "startup_sound")
    checkOwner(config.optJSONObject("clickSound"), "click_sound")
    checkOwner(config.optJSONObject("backgroundMusic"), "background_music")
    checkOwner(config.optJSONObject("startupAnimation"), "startup_animation")
}

private fun ZipInputStream.readEntryBytes(maxBytes: Long): ByteArray {
    val output = ByteArrayOutputStream()
    copyEntryTo(output, maxBytes)
    return output.toByteArray()
}

private fun ZipInputStream.copyEntryTo(output: OutputStream, maxBytes: Long): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copied = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        copied += read
        require(copied <= maxBytes) { "Theme package entry is too large" }
        output.write(buffer, 0, read)
    }
    return copied
}

private data class ThemeStoreDirectorySwap(
    val targetDir: File,
    val backupDir: File,
    val hadPreviousDirectory: Boolean,
) {
    fun rollback() {
        if (targetDir.exists()) {
            require(targetDir.deleteRecursively()) { "Unable to remove incomplete theme package" }
        }
        if (hadPreviousDirectory) {
            require(backupDir.renameTo(targetDir)) { "Unable to restore previous theme package" }
        }
    }

    fun finish(): Boolean {
        return !backupDir.exists() || backupDir.deleteRecursively()
    }
}

private fun beginThemeStoreDirectorySwap(targetDir: File, stagingDir: File): ThemeStoreDirectorySwap {
    val backupDir = File(targetDir.parentFile, "${targetDir.name}-backup").apply {
        if (exists()) {
            require(deleteRecursively()) { "Unable to clear previous theme backup" }
        }
    }
    val hadPreviousDirectory = targetDir.exists()
    if (hadPreviousDirectory) {
        require(targetDir.renameTo(backupDir)) { "Unable to backup current theme package" }
    }
    if (!stagingDir.renameTo(targetDir)) {
        if (hadPreviousDirectory) {
            require(backupDir.renameTo(targetDir)) { "Unable to restore current theme package" }
        }
        error("Unable to install theme package")
    }
    return ThemeStoreDirectorySwap(targetDir, backupDir, hadPreviousDirectory)
}

private fun copyDirectoryContents(sourceDir: File, destinationDir: File) {
    if (!sourceDir.isDirectory) return
    sourceDir.listFiles()?.forEach { source ->
        val destination = File(destinationDir, source.name)
        if (source.isDirectory) {
            destination.mkdirs()
            copyDirectoryContents(source, destination)
        } else if (source.isFile) {
            destination.parentFile?.mkdirs()
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

private fun importAssetUri(
    context: Context,
    assetOwnerJson: JSONObject,
    tempAssetsDir: File,
    stagingAssetsDir: File,
    targetAssetsDir: File,
    assetId: String,
    warnings: MutableList<ThemeStorePackageWarning>,
    budget: ThemeStoreAssetBudget,
    assetKey: String = "asset",
    uriKey: String = "uri",
): ImportedThemeAsset {
    val rawAsset = assetOwnerJson.opt(assetKey)
    if (rawAsset != null && rawAsset !== JSONObject.NULL) {
        require(rawAsset is JSONObject) { "Invalid embedded asset metadata: $assetId" }
        val path = rawAsset.optString("path")
        require(path.startsWith("assets/")) { "Invalid embedded asset path: $assetId" }
        val relativePath = path.removePrefix("assets/")
        val tempFile = safeAssetFile(tempAssetsDir, relativePath)
        require(tempFile.isFile) { "Embedded asset is missing: $assetId" }
        val stagingFile = safeAssetFile(stagingAssetsDir, relativePath)
        stagingFile.parentFile?.mkdirs()
        FileInputStream(tempFile).use { input ->
            FileOutputStream(stagingFile).use { output ->
                input.copyTo(output)
            }
        }
        val targetFile = safeAssetFile(targetAssetsDir, relativePath)
        return ImportedThemeAsset.Resolved(Uri.fromFile(targetFile).toString())
    }

    val legacyUriString = assetOwnerJson.optString(uriKey).takeIf { it.isNotBlank() }
        ?: return ImportedThemeAsset.Resolved(null)
    val startingBudget = budget.totalBytes
    return runCatching {
        val legacyUri = Uri.parse(legacyUriString)
        val extension = safeAssetExtension(
            displayName = queryDisplayName(context, legacyUri) ?: legacyUri.lastPathSegment,
            mimeType = runCatching { context.contentResolver.getType(legacyUri) }.getOrNull(),
        )
        val safeAssetId = assetId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val relativePath = "legacy/$safeAssetId$extension"
        val stagingFile = safeAssetFile(stagingAssetsDir, relativePath)
        val temporaryFile = File(stagingFile.parentFile, "${stagingFile.name}.tmp")
        stagingFile.parentFile?.mkdirs()
        try {
            openThemeStoreUriInputStream(context, legacyUri).use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    input.copyThemeStoreAssetTo(output, budget)
                }
            }
            if (!temporaryFile.renameTo(stagingFile)) {
                temporaryFile.copyTo(stagingFile, overwrite = true)
                temporaryFile.delete()
            }
        } finally {
            temporaryFile.delete()
        }
        ImportedThemeAsset.Resolved(
            Uri.fromFile(safeAssetFile(targetAssetsDir, relativePath)).toString()
        )
    }.getOrElse {
        budget.totalBytes = startingBudget
        warnings += ThemeStorePackageWarning(
            assetId = assetId,
            reason = it.message?.lineSequence()?.firstOrNull()?.take(160),
        )
        ImportedThemeAsset.Unavailable
    }
}

private fun openThemeStoreUriInputStream(context: Context, uri: Uri): InputStream {
    if (uri.scheme == "file") {
        val path = uri.path ?: error("Invalid file URI")
        return FileInputStream(File(path))
    }
    return context.contentResolver.openInputStream(uri) ?: error("Unable to open $uri")
}

private fun openThemeStoreUriOutputStream(context: Context, uri: Uri): OutputStream {
    if (uri.scheme == "file") {
        val path = uri.path ?: error("Invalid file URI")
        val file = File(path)
        file.parentFile?.mkdirs()
        return FileOutputStream(file)
    }
    return context.contentResolver.openOutputStream(uri) ?: error("Unable to open $uri")
}

private fun safeAssetFile(root: File, relativePath: String): File {
    val safeName = validateThemeStoreArchiveEntryName(relativePath)
    val rootFile = root.canonicalFile
    val target = File(rootFile, safeName).canonicalFile
    require(target.path == rootFile.path || target.path.startsWith(rootFile.path + File.separator)) {
        "Invalid asset path"
    }
    return target
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex < 0) null else cursor.getString(nameIndex)
        }
    }.getOrNull()
}

private fun safeAssetExtension(displayName: String?, mimeType: String?): String {
    val fromName = displayName
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase(Locale.ROOT)
        ?.filter { it.isLetterOrDigit() }
        ?.takeIf { it.length in 1..8 }
    if (fromName != null) return ".$fromName"

    return when {
        mimeType?.startsWith("image/png") == true -> ".png"
        mimeType?.startsWith("image/webp") == true -> ".webp"
        mimeType.equals(CUSTOM_STARTUP_ANIMATION_GIF_MIME_TYPE, ignoreCase = true) -> ".gif"
        mimeType?.startsWith("image/") == true -> ".jpg"
        mimeType?.startsWith("audio/mpeg") == true -> ".mp3"
        mimeType?.startsWith("audio/ogg") == true -> ".ogg"
        mimeType?.startsWith("audio/wav") == true -> ".wav"
        mimeType?.startsWith("audio/") == true -> ".audio"
        mimeType?.startsWith("video/mp4") == true -> ".mp4"
        mimeType?.startsWith("video/webm") == true -> ".webm"
        mimeType?.startsWith("video/quicktime") == true -> ".mov"
        mimeType?.startsWith("video/x-matroska") == true -> ".mkv"
        mimeType?.startsWith("video/3gpp") == true -> ".3gp"
        mimeType?.startsWith("video/") == true -> ".video"
        else -> ".bin"
    }
}

private fun CustomWallpaperCrop.toJson(): JSONObject {
    return JSONObject()
        .put("left", left)
        .put("top", top)
        .put("right", right)
        .put("bottom", bottom)
}

private fun JSONObject.optCrop(key: String, fallback: CustomWallpaperCrop): CustomWallpaperCrop {
    val cropJson = optJSONObject(key) ?: return fallback
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = cropJson.optDouble("left", fallback.left.toDouble()).toFloat(),
            top = cropJson.optDouble("top", fallback.top.toDouble()).toFloat(),
            right = cropJson.optDouble("right", fallback.right.toDouble()).toFloat(),
            bottom = cropJson.optDouble("bottom", fallback.bottom.toDouble()).toFloat(),
        )
    )
}
