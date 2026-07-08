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
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val THEME_STORE_SCHEMA = "io.github.fixz.apkesu.theme"
private const val THEME_STORE_VERSION = 2
private const val MAX_THEME_STORE_ENTRY_COUNT = 64
private const val MAX_THEME_STORE_JSON_BYTES = 256L * 1024L
private const val MAX_THEME_STORE_ASSET_BYTES = 256L * 1024L * 1024L
private const val MAX_THEME_STORE_ASSETS_BYTES = 512L * 1024L * 1024L
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
    val startupSoundUri: String?,
    val startupAnimationUri: String?,
) {
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
                !startupSoundUri.isNullOrBlank(),
                !startupAnimationUri.isNullOrBlank(),
            ).count { it }
}

data class ThemeStorePackageResult(
    val success: Boolean,
    val warnings: List<String> = emptyList(),
    val error: Throwable? = null,
)

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
        startupSoundUri = prefs.getString(CUSTOM_STARTUP_SOUND_URI_KEY, null),
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
    return runCatching {
        val appContext = context.applicationContext
        val prefs = themeStorePrefs(appContext)
        val warnings = mutableListOf<String>()
        val resolver = appContext.contentResolver
        val config = JSONObject()
            .put("schema", THEME_STORE_SCHEMA)
            .put("version", THEME_STORE_VERSION)
            .put("exportedAt", System.currentTimeMillis())

        resolver.openOutputStream(destination)?.use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                val cardsJson = JSONObject()
                ThemeStoreImageSlot.entries.forEach { slot ->
                    val state = prefs.readImageSlot(slot)
                    val asset = zip.writeUriAsset(
                        context = appContext,
                        uriString = state.uriString,
                        assetId = "card_${slot.id}",
                        warnings = warnings,
                    )
                    val videoAsset = zip.writeUriAsset(
                        context = appContext,
                        uriString = state.videoUriString,
                        assetId = "card_${slot.id}_video",
                        warnings = warnings,
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
                    )
                    val videoAsset = zip.writeUriAsset(
                        context = appContext,
                        uriString = state.videoUriString,
                        assetId = "page_background_${target.id}_video",
                        warnings = warnings,
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
                )
                val videoBackgroundAsset = zip.writeUriAsset(
                    context = appContext,
                    uriString = wallpaperState.videoUriString,
                    assetId = "custom_video_background",
                    warnings = warnings,
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
                )
                config.put(
                    "startupSound",
                    JSONObject()
                        .put("asset", startupSoundAsset?.toJson())
                        .put("uri", startupSoundUri),
                )

                val startupAnimationUri = prefs.getString(CUSTOM_STARTUP_ANIMATION_URI_KEY, null)
                val startupAnimationAsset = zip.writeUriAsset(
                    context = appContext,
                    uriString = startupAnimationUri,
                    assetId = "startup_animation",
                    warnings = warnings,
                )
                config.put(
                    "startupAnimation",
                    JSONObject()
                        .put("asset", startupAnimationAsset?.toJson())
                        .put("uri", startupAnimationUri),
                )

                zip.putNextEntry(ZipEntry("theme.json"))
                zip.write(config.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        } ?: error("Unable to open destination")
        ThemeStorePackageResult(success = true, warnings = warnings)
    }.getOrElse {
        ThemeStorePackageResult(success = false, error = it)
    }
}

fun importThemeStorePackage(context: Context, source: Uri): ThemeStorePackageResult {
    val appContext = context.applicationContext
    val tempDir = File(appContext.cacheDir, "theme-store-import").apply {
        deleteRecursively()
        mkdirs()
    }
    var stagingDir: File? = null
    return runCatching {
        try {
            val tempAssetsDir = File(tempDir, "assets").apply { mkdirs() }
            val themeJson = extractThemeStoreZip(appContext, source, tempDir, tempAssetsDir)
            val config = JSONObject(themeJson)
            require(config.optString("schema") == THEME_STORE_SCHEMA) { "Unsupported theme package" }
            require(config.optInt("version", 0) in 1..THEME_STORE_VERSION) { "Unsupported theme package version" }

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
            var hasStartupAnimation = false
            var pendingStartupAnimationUri: String? = null

            ThemeStoreImageSlot.entries.forEach { slot ->
                val slotJson = cardsJson.optJSONObject(slot.id)
                if (slotJson != null) {
                    val importedUri = importAssetUri(slotJson, tempAssetsDir, stagingAssetsDir, targetAssetsDir)
                        ?: slotJson.optString("uri").takeIf { it.isNotBlank() }
                    val importedVideoUri = if (slot.videoUriKey != null) {
                        importAssetUri(
                            assetOwnerJson = slotJson,
                            tempAssetsDir = tempAssetsDir,
                            stagingAssetsDir = stagingAssetsDir,
                            targetAssetsDir = targetAssetsDir,
                            assetKey = "videoAsset",
                            uriKey = "videoUri",
                        )
                    } else {
                        null
                    }
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
                    val importedUri = importAssetUri(slotJson, tempAssetsDir, stagingAssetsDir, targetAssetsDir)
                        ?: slotJson.optString("uri").takeIf { it.isNotBlank() }
                    pendingNavigationIcons[slot] = importedUri to slotJson.optCrop(
                        "crop",
                        DEFAULT_CUSTOM_NAVIGATION_ICON_CROP,
                    )
                }
            }

            CustomPageBackgroundTarget.entries.forEach { target ->
                val targetJson = pageBackgroundsJson.optJSONObject(target.id)
                if (targetJson != null) {
                    val importedUri = importAssetUri(targetJson, tempAssetsDir, stagingAssetsDir, targetAssetsDir)
                        ?: targetJson.optString("uri").takeIf { it.isNotBlank() }
                    val importedVideoUri = importAssetUri(
                        assetOwnerJson = targetJson,
                        tempAssetsDir = tempAssetsDir,
                        stagingAssetsDir = stagingAssetsDir,
                        targetAssetsDir = targetAssetsDir,
                        assetKey = "videoAsset",
                        uriKey = "videoUri",
                    )
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
                val importedUri = importAssetUri(wallpaperJson, tempAssetsDir, stagingAssetsDir, targetAssetsDir)
                    ?: wallpaperJson.optString("uri").takeIf { it.isNotBlank() }
                val importedVideoUri = importAssetUri(
                    assetOwnerJson = wallpaperJson,
                    tempAssetsDir = tempAssetsDir,
                    stagingAssetsDir = stagingAssetsDir,
                    targetAssetsDir = targetAssetsDir,
                    assetKey = "videoAsset",
                    uriKey = "videoUri",
                )
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
                hasStartupSound = true
                pendingStartupSoundUri = importAssetUri(soundJson, tempAssetsDir, stagingAssetsDir, targetAssetsDir)
                    ?: soundJson.optString("uri").takeIf { it.isNotBlank() }
            }

            config.optJSONObject("startupAnimation")?.let { animationJson ->
                hasStartupAnimation = true
                pendingStartupAnimationUri = importAssetUri(animationJson, tempAssetsDir, stagingAssetsDir, targetAssetsDir)
                    ?: animationJson.optString("uri").takeIf { it.isNotBlank() }
            }

            replaceThemeStoreDirectory(targetDir, nextStagingDir)
            stagingDir = null

            val prefs = themeStorePrefs(appContext)
            pendingCards.forEach { (slot, pending) ->
                val importedUri = pending.uriString.takeUnless { pending.hasVideoSelected }
                val importedVideoUri = pending.videoUriString.takeIf { slot.videoUriKey != null }
                val previous = prefs.getString(slot.uriKey, null)
                val previousVideo = slot.videoUriKey?.let { prefs.getString(it, null) }
                prefs.edit(commit = true) {
                    if (importedUri.isNullOrBlank()) {
                        remove(slot.uriKey)
                        removeImageSlotCrop(slot)
                    } else {
                        putString(slot.uriKey, importedUri)
                        putImageSlotCrop(slot, pending.crop)
                    }
                    slot.videoUriKey?.let { videoUriKey ->
                        if (importedVideoUri.isNullOrBlank()) {
                            remove(videoUriKey)
                        } else {
                            remove(slot.uriKey)
                            putImageSlotCrop(slot, pending.crop)
                            putString(videoUriKey, importedVideoUri)
                        }
                    }
                }
                if (previous != importedUri) {
                    releaseCustomImageReference(appContext, previous)
                }
                if (previousVideo != importedVideoUri) {
                    releasePersistableVideoBackgroundReadPermission(appContext, previousVideo)
                }
            }

            pendingNavigationIcons.forEach { (slot, pending) ->
                val (importedUri, crop) = pending
                val previous = prefs.getString(slot.uriKey, null)
                prefs.edit(commit = true) {
                    if (importedUri.isNullOrBlank()) {
                        remove(slot.uriKey)
                        removeCustomNavigationIconCrop(slot)
                    } else {
                        putString(slot.uriKey, importedUri)
                        putCustomNavigationIconCrop(slot, crop)
                    }
                }
                if (previous != importedUri) {
                    releaseCustomImageReference(appContext, previous)
                }
            }

            pendingPageBackgrounds.forEach { (target, pending) ->
                applyThemeStorePageBackground(appContext, target, pending)
            }

            pendingWallpaper?.let { wallpaper ->
                val previous = prefs.getString(CUSTOM_WALLPAPER_URI_KEY, null)
                val previousVideo = prefs.getString(CUSTOM_VIDEO_BACKGROUND_URI_KEY, null)
                val nextWallpaperUri = wallpaper.uriString.takeUnless { wallpaper.videoUriString != null }
                prefs.edit(commit = true) {
                    if (nextWallpaperUri.isNullOrBlank()) {
                        remove(CUSTOM_WALLPAPER_URI_KEY)
                        removeCustomWallpaperCrop()
                    } else {
                        putString(CUSTOM_WALLPAPER_URI_KEY, nextWallpaperUri)
                        putCustomWallpaperCrop(wallpaper.crop)
                    }
                    if (wallpaper.videoUriString.isNullOrBlank()) {
                        remove(CUSTOM_VIDEO_BACKGROUND_URI_KEY)
                    } else {
                        putString(CUSTOM_VIDEO_BACKGROUND_URI_KEY, wallpaper.videoUriString)
                    }
                    putInt(CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS_KEY, wallpaper.videoDurationSeconds)
                    putFloat(CUSTOM_WALLPAPER_OPACITY_KEY, wallpaper.opacity)
                    putBoolean(CUSTOM_WALLPAPER_PASSTHROUGH_ENABLED_KEY, wallpaper.passthroughEnabled)
                    putFloat(CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY_KEY, wallpaper.passthroughOpacity)
                }
                if (previous != nextWallpaperUri) {
                    releaseCustomImageReference(appContext, previous)
                }
                if (previousVideo != wallpaper.videoUriString) {
                    releasePersistableVideoBackgroundReadPermission(appContext, previousVideo)
                }
            }

            if (hasStartupSound) {
                setThemeStoreStartupSound(appContext, pendingStartupSoundUri)
            }

            if (hasStartupAnimation) {
                setThemeStoreStartupAnimation(appContext, pendingStartupAnimationUri)
            }

            ThemeStorePackageResult(success = true)
        } finally {
            tempDir.deleteRecursively()
            stagingDir?.deleteRecursively()
        }
    }.getOrElse {
        ThemeStorePackageResult(success = false, error = it)
    }
}

private fun themeStorePrefs(context: Context): SharedPreferences {
    return context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
}

private fun applyThemeStorePageBackground(
    context: Context,
    target: CustomPageBackgroundTarget,
    state: CustomBackgroundState,
) {
    when {
        state.hasVideo -> setCustomPageBackgroundVideo(context, target, state.videoUriString)
        state.hasWallpaper -> setCustomPageBackgroundWallpaper(context, target, state.wallpaperUriString)
        else -> {
            clearCustomPageBackground(context, target)
            return
        }
    }
    setCustomPageBackgroundOpacity(context, target, state.opacity)
    setCustomPageBackgroundCrop(context, target, state.crop)
    setCustomPageBackgroundVideoDurationSeconds(context, target, state.videoDurationSeconds)
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
    warnings: MutableList<String>,
): ExportedThemeAsset? {
    if (uriString.isNullOrBlank()) return null
    val uri = Uri.parse(uriString)
    val displayName = queryDisplayName(context, uri) ?: uri.lastPathSegment
    val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    val extension = safeAssetExtension(displayName, mimeType)
    val path = "assets/$assetId$extension"

    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            putNextEntry(ZipEntry(path))
            input.copyTo(this)
            closeEntry()
        } ?: error("Unable to open $uri")
        ExportedThemeAsset(
            path = path,
            displayName = displayName,
            mimeType = mimeType,
        )
    }.getOrElse {
        warnings += assetId
        null
    }
}

private fun extractThemeStoreZip(
    context: Context,
    source: Uri,
    tempDir: File,
    tempAssetsDir: File,
): String {
    var themeJson: String? = null
    var entryCount = 0
    var totalAssetsBytes = 0L
    context.contentResolver.openInputStream(source)?.use { input ->
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                require(entryCount <= MAX_THEME_STORE_ENTRY_COUNT) { "Theme package has too many entries" }
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                when {
                    entry.name == "theme.json" -> {
                        themeJson = zip.readEntryBytes(MAX_THEME_STORE_JSON_BYTES).toString(Charsets.UTF_8)
                    }

                    entry.name.startsWith("assets/") -> {
                        val outputFile = safeAssetFile(tempAssetsDir, entry.name.removePrefix("assets/"))
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
    } ?: error("Unable to open source")

    return themeJson ?: error("theme.json not found in ${tempDir.name}")
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

private fun replaceThemeStoreDirectory(targetDir: File, stagingDir: File) {
    val backupDir = File(targetDir.parentFile, "${targetDir.name}-backup").apply {
        deleteRecursively()
    }
    if (targetDir.exists()) {
        require(targetDir.renameTo(backupDir)) { "Unable to backup current theme package" }
    }
    if (!stagingDir.renameTo(targetDir)) {
        if (backupDir.exists()) {
            backupDir.renameTo(targetDir)
        }
        error("Unable to install theme package")
    }
    backupDir.deleteRecursively()
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
    assetOwnerJson: JSONObject,
    tempAssetsDir: File,
    stagingAssetsDir: File,
    targetAssetsDir: File,
    assetKey: String = "asset",
    uriKey: String = "uri",
): String? {
    val assetJson = assetOwnerJson.optJSONObject(assetKey)
        ?: return assetOwnerJson.optString(uriKey).takeIf { it.isNotBlank() }
    val path = assetJson.optString("path").takeIf { it.startsWith("assets/") } ?: return null
    val relativePath = path.removePrefix("assets/")
    val tempFile = safeAssetFile(tempAssetsDir, relativePath)
    if (!tempFile.isFile) return null
    val stagingFile = safeAssetFile(stagingAssetsDir, relativePath)
    stagingFile.parentFile?.mkdirs()
    FileInputStream(tempFile).use { input ->
        FileOutputStream(stagingFile).use { output ->
            input.copyTo(output)
        }
    }
    val targetFile = safeAssetFile(targetAssetsDir, relativePath)
    return Uri.fromFile(targetFile).toString()
}

private fun safeAssetFile(root: File, relativePath: String): File {
    val safeName = relativePath
        .replace('\\', '/')
        .split('/')
        .filter { it.isNotBlank() && it != "." && it != ".." }
        .joinToString("/")
    require(safeName.isNotBlank()) { "Invalid asset path" }
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
