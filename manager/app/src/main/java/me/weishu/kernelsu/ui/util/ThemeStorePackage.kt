package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.edit
import androidx.core.net.toUri
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.BACKGROUND_SCROLL_FOLLOW_ENABLED_KEY
import me.weishu.kernelsu.ui.component.SWITCH_STYLE_KEY
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.pixel.PIXEL_STYLE_KEY
import me.weishu.kernelsu.ui.component.pixel.PIXEL_PET_ENABLED_KEY
import me.weishu.kernelsu.ui.component.pixel.PixelPetStore
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.custom.CUSTOM_CARD_STYLE_ACTIVE_ID_KEY
import me.weishu.kernelsu.ui.component.custom.CUSTOM_CARD_STYLE_LIBRARY_KEY
import me.weishu.kernelsu.ui.component.custom.CUSTOM_SWITCH_STYLE_ACTIVE_ID_KEY
import me.weishu.kernelsu.ui.component.custom.CUSTOM_SWITCH_STYLE_LIBRARY_KEY
import me.weishu.kernelsu.ui.component.custom.ComponentStyleKind
import me.weishu.kernelsu.ui.component.custom.ComponentStyleStore
import me.weishu.kernelsu.ui.component.custom.CustomCardStyle
import me.weishu.kernelsu.ui.component.custom.CustomSwitchSource
import me.weishu.kernelsu.ui.component.custom.CustomSwitchStyle
import me.weishu.kernelsu.ui.component.custom.MAX_COMPONENT_IMAGE_BYTES
import me.weishu.kernelsu.ui.component.custom.MAX_SAVED_COMPONENT_STYLES
import me.weishu.kernelsu.ui.component.custom.encodeCardStyleLibrary
import me.weishu.kernelsu.ui.component.custom.encodeSwitchStyleLibrary
import me.weishu.kernelsu.ui.component.decoration.UI_DECORATION_CONFIG_KEY
import me.weishu.kernelsu.ui.component.decoration.UiCardDecoration
import me.weishu.kernelsu.ui.component.decoration.UiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.UiNavigationDecoration
import me.weishu.kernelsu.ui.theme.ColorMode
import me.weishu.kernelsu.ui.theme.ThemeAppearanceDefaults
import me.weishu.kernelsu.ui.theme.ThemePreset
import me.weishu.kernelsu.ui.theme.ThemeSyncStrategy
import me.weishu.kernelsu.ui.theme.defaultThemePresetForUiMode
import me.weishu.kernelsu.ui.theme.sanitizeMonetSurfaceOpacity
import me.weishu.kernelsu.ui.theme.themePreferenceKey
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val THEME_STORE_SCHEMA = "io.github.fixz.apkesu.theme"
internal const val THEME_STORE_VERSION = 6
private const val MAX_THEME_STORE_ENTRY_COUNT = 80
private const val MAX_THEME_STORE_JSON_BYTES = 256L * 1024L
private const val MAX_THEME_STORE_ASSET_BYTES = 500L * 1024L * 1024L
private const val MAX_THEME_STORE_ASSETS_BYTES = 512L * 1024L * 1024L
private const val MAX_THEME_STORE_PREVIEW_IMAGE_BYTES = 16L * 1024L * 1024L
private val APP_FONT_PACKAGE_SHA256 = Regex("[a-fA-F0-9]{64}")
const val THEME_STORE_FILE_MIME_TYPE = "application/zip"
const val THEME_STORE_FILE_EXTENSION = "kstheme"

enum class ThemeStoreImageGroup {
    Home,
    Install,
}

enum class ThemeStoreImageSlot(
    val id: String,
    val group: ThemeStoreImageGroup,
    val uriKey: String,
    val videoUriKey: String?,
    val cropLeftKey: String,
    val cropTopKey: String,
    val cropRightKey: String,
    val cropBottomKey: String,
    val introducedInPackageVersion: Int = 5,
) {
    Lkm(
        id = "lkm",
        group = ThemeStoreImageGroup.Home,
        uriKey = "home_lkm_card_wallpaper_uri",
        videoUriKey = "home_lkm_card_wallpaper_video_uri",
        cropLeftKey = "home_lkm_card_wallpaper_crop_left",
        cropTopKey = "home_lkm_card_wallpaper_crop_top",
        cropRightKey = "home_lkm_card_wallpaper_crop_right",
        cropBottomKey = "home_lkm_card_wallpaper_crop_bottom",
    ),
    ClassicMiuixLkm(
        id = "classic_miuix_lkm",
        group = ThemeStoreImageGroup.Home,
        uriKey = "home_classic_miuix_lkm_card_wallpaper_uri",
        videoUriKey = "home_classic_miuix_lkm_card_wallpaper_video_uri",
        cropLeftKey = "home_classic_miuix_lkm_card_wallpaper_crop_left",
        cropTopKey = "home_classic_miuix_lkm_card_wallpaper_crop_top",
        cropRightKey = "home_classic_miuix_lkm_card_wallpaper_crop_right",
        cropBottomKey = "home_classic_miuix_lkm_card_wallpaper_crop_bottom",
        introducedInPackageVersion = 6,
    ),
    MaterialLkm(
        id = "material_lkm",
        group = ThemeStoreImageGroup.Home,
        uriKey = "home_material_lkm_card_wallpaper_uri",
        videoUriKey = "home_material_lkm_card_wallpaper_video_uri",
        cropLeftKey = "home_material_lkm_card_wallpaper_crop_left",
        cropTopKey = "home_material_lkm_card_wallpaper_crop_top",
        cropRightKey = "home_material_lkm_card_wallpaper_crop_right",
        cropBottomKey = "home_material_lkm_card_wallpaper_crop_bottom",
        introducedInPackageVersion = 6,
    ),
    Superuser(
        id = "superuser",
        group = ThemeStoreImageGroup.Home,
        uriKey = "home_superuser_card_wallpaper_uri",
        videoUriKey = "home_superuser_card_wallpaper_video_uri",
        cropLeftKey = "home_superuser_card_wallpaper_crop_left",
        cropTopKey = "home_superuser_card_wallpaper_crop_top",
        cropRightKey = "home_superuser_card_wallpaper_crop_right",
        cropBottomKey = "home_superuser_card_wallpaper_crop_bottom",
    ),
    Module(
        id = "module",
        group = ThemeStoreImageGroup.Home,
        uriKey = "home_module_card_wallpaper_uri",
        videoUriKey = "home_module_card_wallpaper_video_uri",
        cropLeftKey = "home_module_card_wallpaper_crop_left",
        cropTopKey = "home_module_card_wallpaper_crop_top",
        cropRightKey = "home_module_card_wallpaper_crop_right",
        cropBottomKey = "home_module_card_wallpaper_crop_bottom",
    ),
    StatusMonitor(
        id = "status_monitor",
        group = ThemeStoreImageGroup.Home,
        uriKey = "home_status_monitor_wallpaper_uri",
        videoUriKey = "home_status_monitor_wallpaper_video_uri",
        cropLeftKey = "home_status_monitor_wallpaper_crop_left",
        cropTopKey = "home_status_monitor_wallpaper_crop_top",
        cropRightKey = "home_status_monitor_wallpaper_crop_right",
        cropBottomKey = "home_status_monitor_wallpaper_crop_bottom",
    ),
    SystemInfo(
        id = "system_info",
        group = ThemeStoreImageGroup.Home,
        uriKey = "home_system_info_wallpaper_uri",
        videoUriKey = "home_system_info_wallpaper_video_uri",
        cropLeftKey = "home_system_info_wallpaper_crop_left",
        cropTopKey = "home_system_info_wallpaper_crop_top",
        cropRightKey = "home_system_info_wallpaper_crop_right",
        cropBottomKey = "home_system_info_wallpaper_crop_bottom",
    ),
    RebootMenu(
        id = "reboot_menu",
        group = ThemeStoreImageGroup.Home,
        uriKey = "home_reboot_menu_wallpaper_uri",
        videoUriKey = "home_reboot_menu_wallpaper_video_uri",
        cropLeftKey = "home_reboot_menu_wallpaper_crop_left",
        cropTopKey = "home_reboot_menu_wallpaper_crop_top",
        cropRightKey = "home_reboot_menu_wallpaper_crop_right",
        cropBottomKey = "home_reboot_menu_wallpaper_crop_bottom",
    ),
    InstallImage(
        id = "install_image",
        group = ThemeStoreImageGroup.Install,
        uriKey = "install_image_card_wallpaper_uri",
        videoUriKey = "install_image_card_wallpaper_video_uri",
        cropLeftKey = "install_image_card_wallpaper_crop_left",
        cropTopKey = "install_image_card_wallpaper_crop_top",
        cropRightKey = "install_image_card_wallpaper_crop_right",
        cropBottomKey = "install_image_card_wallpaper_crop_bottom",
    ),
    InstallMethods(
        id = "install_methods",
        group = ThemeStoreImageGroup.Install,
        uriKey = "install_methods_card_wallpaper_uri",
        videoUriKey = "install_methods_card_wallpaper_video_uri",
        cropLeftKey = "install_methods_card_wallpaper_crop_left",
        cropTopKey = "install_methods_card_wallpaper_crop_top",
        cropRightKey = "install_methods_card_wallpaper_crop_right",
        cropBottomKey = "install_methods_card_wallpaper_crop_bottom",
    ),
    InstallOptions(
        id = "install_options",
        group = ThemeStoreImageGroup.Install,
        uriKey = "install_options_card_wallpaper_uri",
        videoUriKey = "install_options_card_wallpaper_video_uri",
        cropLeftKey = "install_options_card_wallpaper_crop_left",
        cropTopKey = "install_options_card_wallpaper_crop_top",
        cropRightKey = "install_options_card_wallpaper_crop_right",
        cropBottomKey = "install_options_card_wallpaper_crop_bottom",
    );

    val preferenceKeys: Set<String>
        get() = buildSet {
            add(uriKey)
            videoUriKey?.let(::add)
            add(cropLeftKey)
            add(cropTopKey)
            add(cropRightKey)
            add(cropBottomKey)
            add(nightUriKey)
            add(nightVideoUriKey)
            add(nightCropKey)
            add(responsiveCropsKey)
            add(nightResponsiveCropsKey)
            add(variantSettingsKey)
            addAll(visualKeys.all)
            addAll(nightVisualKeys.all)
        }

    val nightUriKey: String get() = "theme_card_${id}_night_uri"
    val nightVideoUriKey: String get() = "theme_card_${id}_night_video_uri"
    val nightCropKey: String get() = "theme_card_${id}_night_crop"
    val responsiveCropsKey: String get() = "theme_card_${id}_responsive_crops"
    val nightResponsiveCropsKey: String get() = "theme_card_${id}_night_responsive_crops"
    val variantSettingsKey: String get() = "theme_card_${id}_variant_settings"
    val visualKeys: MediaVisualPreferenceKeys get() = MediaVisualPreferenceKeys("theme_card_${id}_visual")
    val nightVisualKeys: MediaVisualPreferenceKeys get() = MediaVisualPreferenceKeys("theme_card_${id}_night_visual")

    val defaultVisualSettings: MediaVisualSettings
        get() = MediaVisualSettings(
            overlayAlpha = if (group == ThemeStoreImageGroup.Home) 0.44f else 0.42f,
        )
}

data class ThemeStoreImageState(
    val uriString: String?,
    val videoUriString: String?,
    val crop: CustomWallpaperCrop,
    val visualSettings: MediaVisualSettings = MediaVisualSettings(),
    val responsiveCrops: ResponsiveCropSet = ResponsiveCropSet(),
    val nightUriString: String? = null,
    val nightVideoUriString: String? = null,
    val nightCrop: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
    val nightVisualSettings: MediaVisualSettings = MediaVisualSettings(),
    val nightResponsiveCrops: ResponsiveCropSet = ResponsiveCropSet(),
    val variantSettings: MediaVariantSettings = MediaVariantSettings(),
) {
    val hasSelected: Boolean
        get() = hasImageSelected || hasVideoSelected || hasNightSelected
    val hasImageSelected: Boolean
        get() = !uriString.isNullOrBlank()
    val hasVideoSelected: Boolean
        get() = !videoUriString.isNullOrBlank()

    val hasNightSelected: Boolean
        get() = !nightUriString.isNullOrBlank() || !nightVideoUriString.isNullOrBlank()

    fun activeVariant(isDark: Boolean, nowMillis: Long = System.currentTimeMillis(), seed: Int = 0): ActiveMediaVariant {
        val useNight = hasNightSelected && when (variantSettings.mode) {
            MediaVariantMode.FollowSystem -> isDark
            MediaVariantMode.Schedule -> {
                val minutes = java.util.Calendar.getInstance().run {
                    timeInMillis = nowMillis
                    get(java.util.Calendar.HOUR_OF_DAY) * 60 + get(java.util.Calendar.MINUTE)
                }
                val settings = variantSettings.normalized()
                if (settings.dayStartMinutes <= settings.nightStartMinutes) {
                    minutes !in settings.dayStartMinutes until settings.nightStartMinutes
                } else {
                    minutes in settings.nightStartMinutes until settings.dayStartMinutes
                }
            }
            MediaVariantMode.Random -> {
                val bucket = nowMillis / (variantSettings.normalized().randomIntervalMinutes * 60_000L)
                ((bucket + seed.toLong()) and 1L) == 1L
            }
        }
        return if (useNight) {
            ActiveMediaVariant(
                uriString = nightUriString,
                videoUriString = nightVideoUriString,
                crop = nightCrop,
                responsiveCrops = nightResponsiveCrops,
                visualSettings = nightVisualSettings,
            )
        } else {
            ActiveMediaVariant(uriString, videoUriString, crop, responsiveCrops, visualSettings)
        }
    }
}

data class ActiveMediaVariant(
    val uriString: String?,
    val videoUriString: String?,
    val crop: CustomWallpaperCrop,
    val responsiveCrops: ResponsiveCropSet,
    val visualSettings: MediaVisualSettings,
)

data class ThemeStoreWallpaperState(
    val uriString: String?,
    val videoUriString: String?,
    val videoDurationSeconds: Int,
    val videoFrameRate: Int = DEFAULT_CUSTOM_VIDEO_BACKGROUND_FRAME_RATE,
    val scrollFollowEnabled: Boolean = false,
    val opacity: Float,
    val crop: CustomWallpaperCrop,
    val visualSettings: MediaVisualSettings = MediaVisualSettings(),
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

data class ThemeStoreAppearanceState(
    val themeMode: Int,
    val miuixMonet: Boolean,
    val keyColor: Int,
    val colorStyle: String,
    val colorSpec: String,
    val monetSurfaceOpacity: Float,
    val pixelStyle: String = PixelStyle.DEFAULT_VALUE,
    val pixelPetEnabled: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("themeMode", themeMode)
        .put("miuixMonet", miuixMonet)
        .put("keyColor", keyColor)
        .put("colorStyle", colorStyle)
        .put("colorSpec", colorSpec)
        .put("monetSurfaceOpacity", sanitizeMonetSurfaceOpacity(monetSurfaceOpacity))
        .put("pixelStyle", PixelStyle.fromValue(pixelStyle).value)
        .put("pixelPetEnabled", pixelPetEnabled)

    companion object {
        fun fromJson(json: JSONObject, fallback: ThemeStoreAppearanceState): ThemeStoreAppearanceState {
            val style = json.optString("colorStyle", fallback.colorStyle)
                .takeIf { value -> PaletteStyle.entries.any { it.name == value } }
                ?: fallback.colorStyle
            val spec = json.optString("colorSpec", fallback.colorSpec)
                .takeIf { value -> ColorSpec.SpecVersion.entries.any { it.name == value } }
                ?: fallback.colorSpec
            val miuixMonet = json.optBoolean("miuixMonet", fallback.miuixMonet)
            val requestedMode = ColorMode.fromValue(json.optInt("themeMode", fallback.themeMode))
            val effectiveMode = if (miuixMonet) {
                if (requestedMode.isMonet) requestedMode.value else requestedMode.toMonetMode()
            } else {
                if (requestedMode.isMonet) requestedMode.toNonMonetMode() else requestedMode.value
            }
            return ThemeStoreAppearanceState(
                themeMode = effectiveMode,
                miuixMonet = miuixMonet,
                keyColor = json.optInt("keyColor", fallback.keyColor),
                colorStyle = style,
                colorSpec = spec,
                monetSurfaceOpacity = sanitizeMonetSurfaceOpacity(
                    json.optDouble(
                        "monetSurfaceOpacity",
                        fallback.monetSurfaceOpacity.toDouble(),
                    ).toFloat()
                ),
                pixelStyle = PixelStyle.fromValue(json.optString("pixelStyle", fallback.pixelStyle)).value,
                pixelPetEnabled = json.optBoolean("pixelPetEnabled", fallback.pixelPetEnabled),
            )
        }
    }
}

data class ThemeStoreSummary(
    val lkmCard: ThemeStoreImageState,
    val classicMiuixLkmCard: ThemeStoreImageState,
    val materialLkmCard: ThemeStoreImageState,
    val superuserCard: ThemeStoreImageState,
    val moduleCard: ThemeStoreImageState,
    val statusMonitorCard: ThemeStoreImageState,
    val systemInfoCard: ThemeStoreImageState,
    val rebootMenuCard: ThemeStoreImageState,
    val installImageCard: ThemeStoreImageState,
    val installMethodsCard: ThemeStoreImageState,
    val installOptionsCard: ThemeStoreImageState,
    val navigationIcons: CustomNavigationIconSet,
    val pageBackgrounds: CustomPageBackgroundSet,
    val wallpaper: ThemeStoreWallpaperState,
    val audio: ThemeStoreAudioState,
    val startupAnimationUri: String?,
    val appFont: AppFontState,
    val appearance: ThemeStoreAppearanceState,
) {
    val startupSoundUri: String?
        get() = audio.startupSoundUri

    val selectedCount: Int
        get() = navigationIcons.selectedCount +
            CustomPageBackgroundTarget.entries.count { pageBackgrounds[it].hasMedia } +
            listOf(
                lkmCard.hasSelected,
                classicMiuixLkmCard.hasSelected,
                materialLkmCard.hasSelected,
                superuserCard.hasSelected,
                moduleCard.hasSelected,
                statusMonitorCard.hasSelected,
                systemInfoCard.hasSelected,
                rebootMenuCard.hasSelected,
                installImageCard.hasSelected,
                installMethodsCard.hasSelected,
                installOptionsCard.hasSelected,
                wallpaper.hasSelected,
                !startupAnimationUri.isNullOrBlank(),
            ).count { it } + audio.configuredCount +
            (if (appFont.preset != AppFontPreset.System) 1 else 0) +
            1
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

data class ComponentStylePackageContent(
    val cardStyle: CustomCardStyle? = null,
    val switchStyle: CustomSwitchStyle? = null,
) {
    val kind: ComponentStyleKind
        get() = when {
            cardStyle != null && switchStyle == null -> ComponentStyleKind.Card
            switchStyle != null && cardStyle == null -> ComponentStyleKind.Switch
            else -> error("Component package must contain exactly one style")
        }
}

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
        classicMiuixLkmCard = prefs.readImageSlot(ThemeStoreImageSlot.ClassicMiuixLkm),
        materialLkmCard = prefs.readImageSlot(ThemeStoreImageSlot.MaterialLkm),
        superuserCard = prefs.readImageSlot(ThemeStoreImageSlot.Superuser),
        moduleCard = prefs.readImageSlot(ThemeStoreImageSlot.Module),
        statusMonitorCard = prefs.readImageSlot(ThemeStoreImageSlot.StatusMonitor),
        systemInfoCard = prefs.readImageSlot(ThemeStoreImageSlot.SystemInfo),
        rebootMenuCard = prefs.readImageSlot(ThemeStoreImageSlot.RebootMenu),
        installImageCard = prefs.readImageSlot(ThemeStoreImageSlot.InstallImage),
        installMethodsCard = prefs.readImageSlot(ThemeStoreImageSlot.InstallMethods),
        installOptionsCard = prefs.readImageSlot(ThemeStoreImageSlot.InstallOptions),
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
            videoFrameRate = sanitizeCustomVideoBackgroundFrameRate(
                prefs.getInt(
                    CUSTOM_VIDEO_BACKGROUND_FRAME_RATE_KEY,
                    DEFAULT_CUSTOM_VIDEO_BACKGROUND_FRAME_RATE,
                )
            ),
            scrollFollowEnabled = prefs.getBoolean(BACKGROUND_SCROLL_FOLLOW_ENABLED_KEY, false),
            opacity = sanitizeCustomWallpaperOpacity(
                prefs.getFloat(CUSTOM_WALLPAPER_OPACITY_KEY, DEFAULT_CUSTOM_WALLPAPER_OPACITY)
            ),
            crop = prefs.readCustomWallpaperCrop(),
            visualSettings = prefs.readMediaVisualSettings(GLOBAL_BACKGROUND_VISUAL_KEYS),
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
        appFont = readAppFontState(context),
        appearance = prefs.readThemeStoreAppearance(context),
    )
}

fun readThemeStoreImageState(
    context: Context,
    slot: ThemeStoreImageSlot,
): ThemeStoreImageState = themeStorePrefs(context).readImageSlot(slot)

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

fun setThemeStoreImageSlotVisualSettings(
    context: Context,
    slot: ThemeStoreImageSlot,
    settings: MediaVisualSettings,
    night: Boolean = false,
) {
    themeStorePrefs(context).edit {
        putMediaVisualSettings(if (night) slot.nightVisualKeys else slot.visualKeys, settings)
    }
}

fun setThemeStoreImageSlotResponsiveCrops(
    context: Context,
    slot: ThemeStoreImageSlot,
    crops: ResponsiveCropSet,
    night: Boolean = false,
) {
    themeStorePrefs(context).edit(commit = true) {
        putString(
            if (night) slot.nightResponsiveCropsKey else slot.responsiveCropsKey,
            crops.normalized().toJson().toString(),
        )
    }
}

fun setThemeStoreImageSlotVariantSettings(
    context: Context,
    slot: ThemeStoreImageSlot,
    settings: MediaVariantSettings,
) {
    themeStorePrefs(context).edit(commit = true) {
        putString(slot.variantSettingsKey, settings.normalized().toJson().toString())
    }
}

fun setThemeStoreImageSlotNightMedia(
    context: Context,
    slot: ThemeStoreImageSlot,
    uriString: String?,
    video: Boolean,
) {
    val prefs = themeStorePrefs(context)
    val previousImage = prefs.getString(slot.nightUriKey, null)
    val previousVideo = prefs.getString(slot.nightVideoUriKey, null)
    prefs.edit(commit = true) {
        if (uriString.isNullOrBlank()) {
            remove(slot.nightUriKey)
            remove(slot.nightVideoUriKey)
            remove(slot.nightCropKey)
        } else if (video) {
            remove(slot.nightUriKey)
            putString(slot.nightVideoUriKey, uriString)
            putString(slot.nightCropKey, DEFAULT_CUSTOM_WALLPAPER_CROP.toJson().toString())
        } else {
            putString(slot.nightUriKey, uriString)
            remove(slot.nightVideoUriKey)
            putString(slot.nightCropKey, DEFAULT_CUSTOM_WALLPAPER_CROP.toJson().toString())
        }
    }
    if (!video && previousImage != uriString) releaseCustomImageReference(context, previousImage)
    if (video || uriString.isNullOrBlank()) releaseCustomImageReference(context, previousImage)
    if (video && previousVideo != uriString) releasePersistableVideoBackgroundReadPermission(context, previousVideo)
    if (!video || uriString.isNullOrBlank()) releasePersistableVideoBackgroundReadPermission(context, previousVideo)
}

fun setThemeStoreImageSlotNightCrop(context: Context, slot: ThemeStoreImageSlot, crop: CustomWallpaperCrop) {
    themeStorePrefs(context).edit(commit = true) {
        putString(slot.nightCropKey, crop.toJson().toString())
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
    frameRate: Int? = null,
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
        if (frameRate != null) {
            putInt(
                CUSTOM_VIDEO_BACKGROUND_FRAME_RATE_KEY,
                sanitizeCustomVideoBackgroundFrameRate(frameRate),
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

fun setThemeStoreVideoBackgroundFrameRate(context: Context, frameRate: Int) {
    themeStorePrefs(context).edit {
        putInt(
            CUSTOM_VIDEO_BACKGROUND_FRAME_RATE_KEY,
            sanitizeCustomVideoBackgroundFrameRate(frameRate),
        )
    }
}

fun setThemeStoreStartupSound(context: Context, uriString: String?) {
    val prefs = themeStorePrefs(context)
    val previous = prefs.getString(CUSTOM_STARTUP_SOUND_URI_KEY, null)
    if (previous != uriString && !isAudioUriReferencedBySavedScheme(context, previous)) {
        releaseCustomAudioReference(context, previous)
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
                    val nightAsset = zip.writeUriAsset(
                        context = appContext,
                        uriString = state.nightUriString,
                        assetId = "card_${slot.id}_night",
                        warnings = warnings,
                        budget = assetBudget,
                    )
                    val nightVideoAsset = zip.writeUriAsset(
                        context = appContext,
                        uriString = state.nightVideoUriString,
                        assetId = "card_${slot.id}_night_video",
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
                            .put("crop", state.crop.toJson())
                            .put("visualSettings", state.visualSettings.toJson())
                            .put("responsiveCrops", state.responsiveCrops.toJson())
                            .put("nightAsset", nightAsset?.toJson())
                            .put("nightUri", state.nightUriString)
                            .put("nightVideoAsset", nightVideoAsset?.toJson())
                            .put("nightVideoUri", state.nightVideoUriString)
                            .put("nightCrop", state.nightCrop.toJson())
                            .put("nightVisualSettings", state.nightVisualSettings.toJson())
                            .put("nightResponsiveCrops", state.nightResponsiveCrops.toJson())
                            .put("variantSettings", state.variantSettings.toJson()),
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
                            .put("crop", state.crop.toJson())
                            .put("presentation", state.presentationToJson()),
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
                            .put("crop", state.crop.toJson())
                            .put("visualSettings", state.visualSettings.toJson()),
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
                    videoFrameRate = sanitizeCustomVideoBackgroundFrameRate(
                        prefs.getInt(
                            CUSTOM_VIDEO_BACKGROUND_FRAME_RATE_KEY,
                            DEFAULT_CUSTOM_VIDEO_BACKGROUND_FRAME_RATE,
                        )
                    ),
                    scrollFollowEnabled = prefs.getBoolean(BACKGROUND_SCROLL_FOLLOW_ENABLED_KEY, false),
                    opacity = sanitizeCustomWallpaperOpacity(
                        prefs.getFloat(CUSTOM_WALLPAPER_OPACITY_KEY, DEFAULT_CUSTOM_WALLPAPER_OPACITY)
                    ),
                    crop = prefs.readCustomWallpaperCrop(),
                    visualSettings = prefs.readMediaVisualSettings(GLOBAL_BACKGROUND_VISUAL_KEYS),
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
                        .put("videoFrameRate", wallpaperState.videoFrameRate)
                        .put("scrollFollowEnabled", wallpaperState.scrollFollowEnabled)
                        .put("opacity", wallpaperState.opacity)
                        .put("crop", wallpaperState.crop.toJson())
                        .put("visualSettings", wallpaperState.visualSettings.toJson())
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
                config.put("audioSettings", readAppAudioSettings(appContext).toJson())

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
                        .put("uri", startupAnimationUri)
                        .put("settings", readStartupAnimationSettings(appContext).toJson()),
                )

                config.put(
                    "font",
                    zip.writeAppFont(
                        context = appContext,
                        state = readAppFontState(appContext),
                        warnings = warnings,
                        budget = assetBudget,
                    ),
                )

                config.put(
                    "components",
                    zip.writeActiveComponentStyles(
                        context = appContext,
                        warnings = warnings,
                        budget = assetBudget,
                    ),
                )
                config.put("appearance", prefs.readThemeStoreAppearance(appContext).toJson())

                val layoutStickerAssets = mutableMapOf<String, JSONObject?>()
                var layoutStickerIndex = 0
                config.put(
                    "homeLayout",
                    homeLayoutStateToJson(readHomeLayoutState(appContext)) { sticker ->
                        val key = sticker.uriString
                        val asset = layoutStickerAssets.getOrPut(key) {
                            zip.writeUriAsset(
                                context = appContext,
                                uriString = sticker.uriString,
                                assetId = "home_layout_sticker_${layoutStickerIndex++}",
                                warnings = warnings,
                                budget = assetBudget,
                            )?.toJson()
                        }
                        if (asset != null) {
                            JSONObject().put("asset", JSONObject(asset.toString()))
                        } else {
                            JSONObject().put("uri", sticker.uriString)
                        }
                    },
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

fun exportCardComponentStylePackage(
    context: Context,
    style: CustomCardStyle,
    destination: Uri,
): ThemeStorePackageResult = exportComponentStylePackage(
    context = context,
    destination = destination,
    content = ComponentStylePackageContent(cardStyle = style.normalized()),
)

fun exportSwitchComponentStylePackage(
    context: Context,
    style: CustomSwitchStyle,
    destination: Uri,
): ThemeStorePackageResult = exportComponentStylePackage(
    context = context,
    destination = destination,
    content = ComponentStylePackageContent(switchStyle = style.normalized()),
)

private fun exportComponentStylePackage(
    context: Context,
    destination: Uri,
    content: ComponentStylePackageContent,
): ThemeStorePackageResult {
    val appContext = context.applicationContext
    val warnings = mutableListOf<ThemeStorePackageWarning>()
    val stagingFile = runCatching {
        File.createTempFile(
            "component-style-export-",
            ".$THEME_STORE_FILE_EXTENSION",
            appContext.cacheDir,
        )
    }.getOrElse { error ->
        return ThemeStorePackageResult(success = false, error = error)
    }
    return runCatching {
        val profile = readThemeAuthorProfile(appContext)
        val styleAuthor = content.cardStyle?.author ?: content.switchStyle?.author.orEmpty()
        val config = createEmptyThemeStoreConfig(
            displayName = styleAuthor.ifBlank { profile.displayName },
            bio = profile.bio,
        )
        val assetBudget = ThemeStoreAssetBudget()
        FileOutputStream(stagingFile).use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                config.put(
                    "components",
                    zip.writeComponentStyles(
                        context = appContext,
                        content = content,
                        warnings = warnings,
                        budget = assetBudget,
                    ),
                )
                val configBytes = config.toString(2).toByteArray(Charsets.UTF_8)
                require(configBytes.size <= MAX_THEME_STORE_JSON_BYTES) {
                    "Component package metadata is too large"
                }
                zip.putNextEntry(ZipEntry("theme.json"))
                zip.write(configBytes)
                zip.closeEntry()
            }
        }
        require(warnings.isEmpty()) { "Component package contains an unavailable image" }
        openThemeStoreUriOutputStream(appContext, destination).use { output ->
            FileInputStream(stagingFile).use { input -> input.copyTo(output) }
        }
        ThemeStorePackageResult(success = true)
    }.getOrElse { error ->
        ThemeStorePackageResult(success = false, warnings = warnings, error = error)
    }.also {
        stagingFile.delete()
    }
}

fun readComponentStylePackage(
    context: Context,
    source: Uri,
    expectedKind: ComponentStyleKind,
): ComponentStylePackageContent {
    val appContext = context.applicationContext
    val tempDir = File(
        appContext.cacheDir,
        "component-style-import-${System.nanoTime()}",
    ).apply { mkdirs() }
    return try {
        val assetsDir = File(tempDir, "assets").apply { mkdirs() }
        val extracted = extractThemeStoreZip(appContext, source, tempDir, assetsDir)
        val config = JSONObject(extracted.themeJson)
        validateThemeStoreConfig(config)
        validateEmbeddedThemeStoreAssets(config, assetsDir)
        validateEmbeddedThemeStoreMedia(appContext, config, assetsDir)
        val components = config.optJSONObject("components")
            ?: error("Theme package does not contain a component style")
        val hasCardStyle = components.optJSONObject("cardStyle") != null
        val hasSwitchStyle = components.optJSONObject("switchStyle") != null
        val actualKind = when {
            hasCardStyle && !hasSwitchStyle -> ComponentStyleKind.Card
            hasSwitchStyle && !hasCardStyle -> ComponentStyleKind.Switch
            else -> error("Component package must contain exactly one style")
        }
        require(actualKind == expectedKind) { "Component package type does not match this editor" }
        val content = parseComponentStyleContent(appContext, config, assetsDir)
        content
    } finally {
        tempDir.deleteRecursively()
    }
}

fun exportCloudThemeStorePackage(context: Context, destination: Uri): ThemeStorePackageResult {
    val appContext = context.applicationContext
    val sourceFile = File(appContext.cacheDir, "cloud-theme-source-${System.nanoTime()}.$THEME_STORE_FILE_EXTENSION")
    val sanitizedFile = File(
        appContext.cacheDir,
        "cloud-theme-sanitized-${System.nanoTime()}.$THEME_STORE_FILE_EXTENSION",
    )
    val extractionDir = File(appContext.cacheDir, "cloud-theme-export-${System.nanoTime()}")
    return try {
        val exported = exportThemeStorePackage(appContext, Uri.fromFile(sourceFile))
        if (!exported.success) return exported
        require(exported.warnings.isEmpty()) {
            "Some configured theme resources could not be embedded"
        }
        val assetsDir = File(extractionDir, "assets").apply { mkdirs() }
        val extracted = FileInputStream(sourceFile).use { input ->
            extractThemeStoreArchive(input, extractionDir, assetsDir)
        }
        val config = JSONObject(extracted.themeJson)
        validateThemeStoreConfig(config)
        validateEmbeddedThemeStoreAssets(config, assetsDir)
        validateEmbeddedThemeStoreMedia(appContext, config, assetsDir)
        sanitizeThemeStoreConfigForCloud(config)
        validateThemeStoreConfigForCloud(config)

        FileOutputStream(sanitizedFile).use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                writeThemeStoreAssetDirectory(zip, assetsDir, assetsDir)
                val configBytes = config.toString(2).toByteArray(Charsets.UTF_8)
                require(configBytes.size <= MAX_THEME_STORE_JSON_BYTES) {
                    "Theme package metadata is too large"
                }
                zip.putNextEntry(ZipEntry("theme.json"))
                zip.write(configBytes)
                zip.closeEntry()
            }
        }
        openThemeStoreUriOutputStream(appContext, destination).use { output ->
            FileInputStream(sanitizedFile).use { input -> input.copyTo(output) }
        }
        ThemeStorePackageResult(success = true)
    } catch (error: Throwable) {
        ThemeStorePackageResult(success = false, error = error)
    } finally {
        sourceFile.delete()
        sanitizedFile.delete()
        extractionDir.deleteRecursively()
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
            validateEmbeddedThemeStoreMedia(appContext, config, tempAssetsDir)
            collectLegacyThemeStoreUriWarnings(appContext, config, warnings)
            ThemeStorePackageResult(success = true, warnings = warnings)
        } finally {
            tempDir.deleteRecursively()
        }
    }.getOrElse {
        ThemeStorePackageResult(success = false, warnings = warnings, error = it)
    }
}

fun previewThemeStorePackage(
    context: Context,
    source: Uri,
    requireCloudSafe: Boolean = false,
): ThemeStorePackagePreviewResult {
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
            validateEmbeddedThemeStoreMedia(appContext, config, tempAssetsDir)
            if (requireCloudSafe) {
                validateThemeStoreConfigForCloud(config)
            }
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

internal fun resolveImportedAudioSettings(
    current: AppAudioSettings,
    packaged: AppAudioSettings?,
    startupImported: Boolean,
    clickImported: Boolean,
    backgroundImported: Boolean,
): AppAudioSettings? {
    packaged?.let { return it.normalized() }
    if (!startupImported && !clickImported && !backgroundImported) return null
    return current.copy(
        masterEnabled = true,
        startup = if (startupImported) current.startup.copy(enabled = true) else current.startup,
        click = if (clickImported) current.click.copy(enabled = true) else current.click,
        background = if (backgroundImported) current.background.copy(enabled = true) else current.background,
    ).normalized()
}

fun importThemeStorePackage(
    context: Context,
    source: Uri,
    clearCloudThemeState: Boolean = true,
): ThemeStorePackageResult {
    val appContext = context.applicationContext
    val warnings = mutableListOf<ThemeStorePackageWarning>()
    val componentStyleStore = ComponentStyleStore(appContext)
    var pendingComponentImageUris: List<String> = emptyList()
    var replacedSwitchStyles: Pair<List<CustomSwitchStyle>, List<CustomSwitchStyle>>? = null
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
            val packageVersion = config.getInt("version")
            val supportsMediaPolicies = packageVersion >= 5
            val previousSummary = readThemeStoreSummary(appContext)
            val previousHomeLayout = readHomeLayoutState(appContext)
            val pendingAppearance = config.optJSONObject("appearance")?.let { appearanceJson ->
                ThemeStoreAppearanceState.fromJson(appearanceJson, previousSummary.appearance)
            }

            val componentOnlyPackage = config.optString("packageType") == COMPONENT_ONLY_PACKAGE_TYPE
            val componentsJson = config.optJSONObject("components")
            val pendingComponentStyles = if (
                componentsJson?.optJSONObject("cardStyle") != null ||
                componentsJson?.optJSONObject("switchStyle") != null
            ) {
                parseComponentStyleContent(appContext, config, tempAssetsDir).also { content ->
                    pendingComponentImageUris = content.switchStyle
                        ?.let { listOfNotNull(it.imageUri, it.imageOnUri).distinct() }
                        .orEmpty()
                }
            } else {
                null
            }

            if (componentOnlyPackage) {
                val content = pendingComponentStyles
                    ?: error("Component package does not contain a component style")
                val prefs = themeStorePrefs(appContext)
                val editor = prefs.edit()
                replacedSwitchStyles = stageImportedComponentStyles(
                    prefs = prefs,
                    editor = editor,
                    content = content,
                    store = componentStyleStore,
                )
                require(editor.commit()) { "Unable to save imported component style" }
                replacedSwitchStyles?.let { (previous, current) ->
                    componentStyleStore.cleanupReplacedSwitchImages(previous, current)
                }
                return@runCatching ThemeStorePackageResult(success = true, warnings = warnings)
            }

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
            val pendingNavigationIcons = mutableMapOf<CustomNavigationIconSlot, CustomNavigationIconState>()
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
            var pendingAudioSettings: AppAudioSettings? = null
            var hasStartupAnimation = false
            var pendingStartupAnimationUri: String? = null
            var pendingStartupAnimationSettings: StartupAnimationSettings? = null
            var pendingAppFont: AppFontState? = null
            var pendingHomeLayout: HomeLayoutState? = null

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
                    val importedNightImage = importAssetUri(
                        context = appContext,
                        assetOwnerJson = slotJson,
                        tempAssetsDir = tempAssetsDir,
                        stagingAssetsDir = stagingAssetsDir,
                        targetAssetsDir = targetAssetsDir,
                        assetId = "card_${slot.id}_night",
                        warnings = warnings,
                        budget = assetBudget,
                        assetKey = "nightAsset",
                        uriKey = "nightUri",
                    )
                    val importedNightVideo = importAssetUri(
                        context = appContext,
                        assetOwnerJson = slotJson,
                        tempAssetsDir = tempAssetsDir,
                        stagingAssetsDir = stagingAssetsDir,
                        targetAssetsDir = targetAssetsDir,
                        assetId = "card_${slot.id}_night_video",
                        warnings = warnings,
                        budget = assetBudget,
                        assetKey = "nightVideoAsset",
                        uriKey = "nightVideoUri",
                    )
                    if (importedImage is ImportedThemeAsset.Unavailable ||
                        importedVideo is ImportedThemeAsset.Unavailable ||
                        importedNightImage is ImportedThemeAsset.Unavailable ||
                        importedNightVideo is ImportedThemeAsset.Unavailable
                    ) {
                        return@forEach
                    }
                    val importedUri = (importedImage as ImportedThemeAsset.Resolved).uriString
                    val importedVideoUri = (importedVideo as ImportedThemeAsset.Resolved).uriString
                    val importedNightUri = (importedNightImage as ImportedThemeAsset.Resolved).uriString
                    val importedNightVideoUri = (importedNightVideo as ImportedThemeAsset.Resolved).uriString
                    val crop = slotJson.optCrop("crop", DEFAULT_CUSTOM_WALLPAPER_CROP)
                    val nightCrop = slotJson.optCrop("nightCrop", crop)
                    val previous = previousSummary.imageState(slot)
                    pendingCards[slot] = ThemeStoreImageState(
                        uriString = importedUri.takeUnless { !importedVideoUri.isNullOrBlank() },
                        videoUriString = importedVideoUri,
                        crop = crop,
                        visualSettings = if (supportsMediaPolicies) {
                            MediaVisualSettings.fromJson(
                                slotJson.optJSONObject("visualSettings"),
                                slot.defaultVisualSettings,
                            )
                        } else {
                            previous.visualSettings
                        },
                        responsiveCrops = if (supportsMediaPolicies) {
                            ResponsiveCropSet.fromJson(slotJson.optJSONObject("responsiveCrops"), crop)
                        } else {
                            previous.responsiveCrops
                        },
                        nightUriString = if (supportsMediaPolicies) {
                            importedNightUri.takeUnless { !importedNightVideoUri.isNullOrBlank() }
                        } else {
                            previous.nightUriString
                        },
                        nightVideoUriString = if (supportsMediaPolicies) {
                            importedNightVideoUri
                        } else {
                            previous.nightVideoUriString
                        },
                        nightCrop = if (supportsMediaPolicies) nightCrop else previous.nightCrop,
                        nightVisualSettings = if (supportsMediaPolicies) {
                            MediaVisualSettings.fromJson(
                                slotJson.optJSONObject("nightVisualSettings"),
                                slot.defaultVisualSettings,
                            )
                        } else {
                            previous.nightVisualSettings
                        },
                        nightResponsiveCrops = if (supportsMediaPolicies) {
                            ResponsiveCropSet.fromJson(slotJson.optJSONObject("nightResponsiveCrops"), nightCrop)
                        } else {
                            previous.nightResponsiveCrops
                        },
                        variantSettings = if (supportsMediaPolicies) {
                            MediaVariantSettings.fromJson(slotJson.optJSONObject("variantSettings"))
                        } else {
                            previous.variantSettings
                        },
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
                    val uriString = (importedAsset as ImportedThemeAsset.Resolved).uriString
                    val crop = slotJson.optCrop("crop", DEFAULT_CUSTOM_NAVIGATION_ICON_CROP)
                    pendingNavigationIcons[slot] = if (supportsMediaPolicies) {
                        slotJson.optJSONObject("presentation").toNavigationIconState(uriString, crop)
                    } else {
                        previousSummary.navigationIcons[slot].copy(uriString = uriString, crop = crop)
                    }
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
                        visualSettings = if (supportsMediaPolicies) {
                            MediaVisualSettings.fromJson(targetJson.optJSONObject("visualSettings"))
                        } else {
                            previousSummary.pageBackgrounds[target].visualSettings
                        },
                    )
                }
            }

            config.optJSONObject("wallpaper")?.takeUnless { componentOnlyPackage }?.let { wallpaperJson ->
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
                    videoFrameRate = sanitizeCustomVideoBackgroundFrameRate(
                        wallpaperJson.optInt(
                            "videoFrameRate",
                            previousSummary.wallpaper.videoFrameRate,
                        )
                    ),
                    scrollFollowEnabled = wallpaperJson.optBoolean("scrollFollowEnabled", false),
                    opacity = sanitizeCustomWallpaperOpacity(
                        wallpaperJson.optDouble(
                            "opacity",
                            DEFAULT_CUSTOM_WALLPAPER_OPACITY.toDouble(),
                        ).toFloat()
                    ),
                    crop = wallpaperJson.optCrop("crop", DEFAULT_CUSTOM_WALLPAPER_CROP),
                    visualSettings = if (supportsMediaPolicies) {
                        MediaVisualSettings.fromJson(wallpaperJson.optJSONObject("visualSettings"))
                    } else {
                        previousSummary.wallpaper.visualSettings
                    },
                    passthroughEnabled = wallpaperJson.optBoolean("passthroughEnabled", false),
                    passthroughOpacity = sanitizeCustomWallpaperPassthroughOpacity(
                        wallpaperJson.optDouble(
                            "passthroughOpacity",
                            DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY.toDouble(),
                        ).toFloat()
                    ),
                )
            }

            config.optJSONObject("startupSound")?.takeUnless { componentOnlyPackage }?.let { soundJson ->
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

            config.optJSONObject("clickSound")?.takeUnless { componentOnlyPackage }?.let { soundJson ->
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

            config.optJSONObject("backgroundMusic")?.takeUnless { componentOnlyPackage }?.let { musicJson ->
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

            config.optJSONObject("audioSettings")?.takeUnless { componentOnlyPackage }?.let { settingsJson ->
                pendingAudioSettings = AppAudioSettings.fromJson(settingsJson)
            }
            pendingAudioSettings = resolveImportedAudioSettings(
                current = readAppAudioSettings(appContext),
                packaged = pendingAudioSettings,
                startupImported = hasStartupSound && !pendingStartupSoundUri.isNullOrBlank(),
                clickImported = hasClickSound && !pendingClickSoundUri.isNullOrBlank(),
                backgroundImported = hasBackgroundMusic && !pendingBackgroundMusicUri.isNullOrBlank(),
            )

            config.optJSONObject("startupAnimation")?.takeUnless { componentOnlyPackage }?.let { animationJson ->
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
                pendingStartupAnimationSettings = if (supportsMediaPolicies) {
                    StartupAnimationSettings.fromJson(animationJson.optJSONObject("settings"))
                } else {
                    readStartupAnimationSettings(appContext)
                }
            }

            config.optJSONObject("font")?.let { fontJson ->
                val preset = AppFontPreset.fromValue(fontJson.getString("preset"))
                pendingAppFont = if (preset == AppFontPreset.Custom) {
                    importPackagedAppFont(
                        fontJson = fontJson,
                        tempAssetsDir = tempAssetsDir,
                        stagingAssetsDir = stagingAssetsDir,
                    )
                } else {
                    safeAssetFile(stagingAssetsDir, APP_FONT_CUSTOM_FILE_NAME).delete()
                    AppFontState(preset = preset)
                }
            }

            config.optJSONObject("homeLayout")
                ?.takeUnless { componentOnlyPackage }
                ?.let { layoutJson ->
                    pendingHomeLayout = importThemeStoreHomeLayout(
                        context = appContext,
                        layoutJson = layoutJson,
                        tempAssetsDir = tempAssetsDir,
                        stagingAssetsDir = stagingAssetsDir,
                        targetAssetsDir = targetAssetsDir,
                        warnings = warnings,
                        budget = assetBudget,
                    )
                }

            val prefs = themeStorePrefs(appContext)
            val editor = prefs.edit()
            pendingAppearance?.let { editor.putThemeStoreAppearance(prefs, it) }
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
                editor.putMediaVisualSettings(slot.visualKeys, pending.visualSettings)
                editor.putString(slot.responsiveCropsKey, pending.responsiveCrops.toJson().toString())
                editor.putOptionalString(slot.nightUriKey, pending.nightUriString)
                editor.putOptionalString(slot.nightVideoUriKey, pending.nightVideoUriString)
                editor.putString(slot.nightCropKey, pending.nightCrop.toJson().toString())
                editor.putMediaVisualSettings(slot.nightVisualKeys, pending.nightVisualSettings)
                editor.putString(
                    slot.nightResponsiveCropsKey,
                    pending.nightResponsiveCrops.toJson().toString(),
                )
                editor.putString(slot.variantSettingsKey, pending.variantSettings.toJson().toString())
            }

            pendingNavigationIcons.forEach { (slot, pending) ->
                val importedUri = pending.uriString
                if (importedUri.isNullOrBlank()) {
                    editor.remove(slot.uriKey)
                    editor.removeCustomNavigationIconCrop(slot)
                } else {
                    editor.putString(slot.uriKey, importedUri)
                    editor.putCustomNavigationIconCrop(slot, pending.crop)
                }
                editor.putNavigationIconPresentation(slot, pending)
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

            pendingAudioSettings?.let { settings ->
                editor.putString(CUSTOM_AUDIO_SETTINGS_KEY, settings.toJson().toString())
            }

            if (hasStartupAnimation) {
                editor.putOptionalString(CUSTOM_STARTUP_ANIMATION_URI_KEY, pendingStartupAnimationUri)
                pendingStartupAnimationSettings?.let { settings ->
                    editor.putString(CUSTOM_STARTUP_ANIMATION_SETTINGS_KEY, settings.toJson().toString())
                }
            }

            pendingAppFont?.let(editor::putAppFontState)
            pendingHomeLayout?.let(editor::putHomeLayoutState)

            pendingComponentStyles?.let { content ->
                replacedSwitchStyles = stageImportedComponentStyles(
                    prefs = prefs,
                    editor = editor,
                    content = content,
                    store = componentStyleStore,
                )
            }

            val directorySwap = beginThemeStoreDirectorySwap(targetDir, nextStagingDir)
            stagingDir = null
            var preferencesCommitted = false
            try {
                require(editor.commit()) { "Unable to save imported theme settings" }
                preferencesCommitted = true
                pendingAppearance?.let { appearance ->
                    PixelPetStore.applyThemeEnabled(appContext, appearance.pixelPetEnabled)
                }
            } finally {
                if (!preferencesCommitted) {
                    directorySwap.rollback()
                }
            }
            if (!directorySwap.finish()) {
                warnings += ThemeStorePackageWarning("previous_theme_backup")
            }
            replacedSwitchStyles?.let { (previous, current) ->
                componentStyleStore.cleanupReplacedSwitchImages(previous, current)
            }
            releaseReplacedThemeStoreReferences(
                context = appContext,
                previous = previousSummary,
                current = readThemeStoreSummary(appContext),
            )
            releaseReplacedHomeLayoutStickerReferences(
                context = appContext,
                previous = previousHomeLayout,
                current = readHomeLayoutState(appContext),
            )
            if (clearCloudThemeState && !componentOnlyPackage) {
                runCatching {
                    CloudThemeRepository(appContext).recordExternalThemeApplied()
                }.onFailure { error ->
                    warnings += ThemeStorePackageWarning(
                        assetId = "cloud_theme_state",
                        reason = error.safeCloudThemeMessage(),
                    )
                }
            }

            ThemeStorePackageResult(success = true, warnings = warnings)
        } finally {
            tempDir.deleteRecursively()
            stagingDir?.deleteRecursively()
        }
    }.getOrElse { error ->
        pendingComponentImageUris.forEach(componentStyleStore::discardSwitchImageIfUnreferenced)
        ThemeStorePackageResult(success = false, warnings = warnings, error = error)
    }
}

private fun themeStorePrefs(context: Context): SharedPreferences {
    return context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
}

private fun SharedPreferences.readThemeStoreAppearance(context: Context): ThemeStoreAppearanceState {
    val uiMode = InterfaceStyle.normalizeValue(getString("ui_mode", UiMode.DEFAULT_VALUE))
    val defaultPreset = defaultThemePresetForUiMode(uiMode)
    val strategy = ThemeSyncStrategy.fromValue(
        getString("theme_sync_strategy", ThemeSyncStrategy.SHARED.value)
    )
    fun key(base: String) = themePreferenceKey(base, strategy, uiMode)
    return ThemeStoreAppearanceState(
        themeMode = getInt(key("color_mode"), defaultPreset.colorMode.value),
        miuixMonet = getBoolean(key("miuix_monet"), defaultPreset.miuixMonet),
        keyColor = getInt(key("key_color"), defaultPreset.keyColor),
        colorStyle = getString(key("color_style"), defaultPreset.paletteStyle.name)
            ?: defaultPreset.paletteStyle.name,
        colorSpec = getString(key("color_spec"), defaultPreset.colorSpec.name)
            ?: defaultPreset.colorSpec.name,
        monetSurfaceOpacity = sanitizeMonetSurfaceOpacity(
            getFloat(key("monet_surface_opacity"), defaultPreset.monetSurfaceOpacity)
        ),
        pixelStyle = PixelStyle.fromValue(getString(PIXEL_STYLE_KEY, PixelStyle.DEFAULT_VALUE)).value,
        pixelPetEnabled = context.applicationContext
            .getSharedPreferences("pixel_pet", Context.MODE_PRIVATE)
            .getBoolean(PIXEL_PET_ENABLED_KEY, false),
    )
}

private fun SharedPreferences.Editor.putThemeStoreAppearance(
    prefs: SharedPreferences,
    appearance: ThemeStoreAppearanceState,
) {
    val uiMode = InterfaceStyle.normalizeValue(prefs.getString("ui_mode", UiMode.DEFAULT_VALUE))
    val strategy = ThemeSyncStrategy.fromValue(
        prefs.getString("theme_sync_strategy", ThemeSyncStrategy.SHARED.value)
    )
    fun key(base: String) = themePreferenceKey(base, strategy, uiMode)
    putInt(key("color_mode"), appearance.themeMode)
    putBoolean(key("miuix_monet"), appearance.miuixMonet)
    putInt(key("key_color"), appearance.keyColor)
    putString(key("color_style"), appearance.colorStyle)
    putString(key("color_spec"), appearance.colorSpec)
    putFloat(key("monet_surface_opacity"), sanitizeMonetSurfaceOpacity(appearance.monetSurfaceOpacity))
    putString(key("theme_preset"), ThemePreset.CUSTOM.value)
    putString(PIXEL_STYLE_KEY, PixelStyle.fromValue(appearance.pixelStyle).value)
}

private fun stageImportedComponentStyles(
    prefs: SharedPreferences,
    editor: SharedPreferences.Editor,
    content: ComponentStylePackageContent,
    store: ComponentStyleStore,
): Pair<List<CustomSwitchStyle>, List<CustomSwitchStyle>>? {
    content.cardStyle?.let { style ->
        val current = store.readCardStyles()
        require(current.size < MAX_SAVED_COMPONENT_STYLES || current.any { it.id == style.id }) {
            "Card style library is full"
        }
        val updated = ComponentStyleStore.upsertCardStyle(current, style)
        val currentDecoration = UiDecorationConfig.fromJsonString(
            prefs.getString(UI_DECORATION_CONFIG_KEY, null)
        )
        editor
            .putString(CUSTOM_CARD_STYLE_LIBRARY_KEY, encodeCardStyleLibrary(updated))
            .putString(CUSTOM_CARD_STYLE_ACTIVE_ID_KEY, style.id)
            .putString(
                UI_DECORATION_CONFIG_KEY,
                currentDecoration.copy(
                    enabled = true,
                    card = UiCardDecoration.Custom,
                    navigation = UiNavigationDecoration.Custom,
                ).normalized().toJsonString(),
            )
    }
    return content.switchStyle?.let { style ->
        val current = store.readSwitchStyles()
        require(current.size < MAX_SAVED_COMPONENT_STYLES || current.any { it.id == style.id }) {
            "Switch style library is full"
        }
        val updated = ComponentStyleStore.upsertSwitchStyle(current, style)
        editor
            .putString(CUSTOM_SWITCH_STYLE_LIBRARY_KEY, encodeSwitchStyleLibrary(updated))
            .putString(CUSTOM_SWITCH_STYLE_ACTIVE_ID_KEY, style.id)
            .putString(SWITCH_STYLE_KEY, SwitchStyle.Custom.value)
        current to updated
    }
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
            removeMediaVisualSettings(target.visualKeys)
            return
        }
    }
    putFloat(target.opacityKey, sanitizeCustomWallpaperOpacity(state.opacity))
    putImportedPageBackgroundCrop(target, state.crop)
    putInt(
        target.videoDurationSecondsKey,
        sanitizeCustomVideoBackgroundDurationSeconds(state.videoDurationSeconds),
    )
    putMediaVisualSettings(target.visualKeys, state.visualSettings)
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
    putInt(
        CUSTOM_VIDEO_BACKGROUND_FRAME_RATE_KEY,
        sanitizeCustomVideoBackgroundFrameRate(state.videoFrameRate),
    )
    putBoolean(BACKGROUND_SCROLL_FOLLOW_ENABLED_KEY, state.scrollFollowEnabled)
    putFloat(CUSTOM_WALLPAPER_OPACITY_KEY, sanitizeCustomWallpaperOpacity(state.opacity))
    putBoolean(CUSTOM_WALLPAPER_PASSTHROUGH_ENABLED_KEY, state.passthroughEnabled)
    putFloat(
        CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY_KEY,
        sanitizeCustomWallpaperPassthroughOpacity(state.passthroughOpacity),
    )
    putMediaVisualSettings(GLOBAL_BACKGROUND_VISUAL_KEYS, state.visualSettings)
}

private fun SharedPreferences.Editor.putOptionalString(key: String, value: String?) {
    if (value.isNullOrBlank()) {
        remove(key)
    } else {
        putString(key, value)
    }
}

private fun SharedPreferences.Editor.putNavigationIconPresentation(
    slot: CustomNavigationIconSlot,
    state: CustomNavigationIconState,
) {
    val value = state.normalized()
    putFloat(slot.sizeScaleKey, value.sizeScale)
    putFloat(slot.innerPaddingKey, value.innerPaddingDp)
    putFloat(slot.verticalOffsetKey, value.verticalOffsetDp)
    putFloat(slot.opacityKey, value.opacity)
    if (value.tintArgb == null) remove(slot.tintArgbKey) else putLong(slot.tintArgbKey, value.tintArgb)
    putString(slot.maskKey, value.mask.value)
    putOptionalString(slot.labelKey, value.labelOverride)
}

private fun importThemeStoreHomeLayout(
    context: Context,
    layoutJson: JSONObject,
    tempAssetsDir: File,
    stagingAssetsDir: File,
    targetAssetsDir: File,
    warnings: MutableList<ThemeStorePackageWarning>,
    budget: ThemeStoreAssetBudget,
): HomeLayoutState {
    var stickerIndex = 0
    return requireNotNull(
        homeLayoutStateFromJson(layoutJson) { stickerJson ->
            when (
                val imported = importAssetUri(
                    context = context,
                    assetOwnerJson = stickerJson,
                    tempAssetsDir = tempAssetsDir,
                    stagingAssetsDir = stagingAssetsDir,
                    targetAssetsDir = targetAssetsDir,
                    assetId = "home_layout_sticker_${stickerIndex++}",
                    warnings = warnings,
                    budget = budget,
                )
            ) {
                is ImportedThemeAsset.Resolved -> imported.uriString
                ImportedThemeAsset.Unavailable -> null
            }
        },
    ) { "Theme package home layout is invalid" }
}

private fun releaseReplacedHomeLayoutStickerReferences(
    context: Context,
    previous: HomeLayoutState,
    current: HomeLayoutState,
) {
    fun stickerUris(state: HomeLayoutState): Set<String> =
        (state.items + state.landscapeItems)
            .flatMap { it.stickers }
            .map { it.uriString }
            .filter(String::isNotBlank)
            .toSet()

    (stickerUris(previous) - stickerUris(current)).forEach { uriString ->
        releaseCustomImageReference(context, uriString)
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
        if (oldState.nightUriString != newState.nightUriString) {
            releaseCustomImageReference(context, oldState.nightUriString)
        }
        if (oldState.nightVideoUriString != newState.nightVideoUriString) {
            releasePersistableVideoBackgroundReadPermission(context, oldState.nightVideoUriString)
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
        if (!isAudioUriReferencedBySavedScheme(context, previous.startupSoundUri)) {
            releaseCustomAudioReference(context, previous.startupSoundUri)
        }
    }
    if (previous.audio.clickSoundUri != current.audio.clickSoundUri) {
        if (!isAudioUriReferencedBySavedScheme(context, previous.audio.clickSoundUri)) {
            releaseCustomAudioReference(context, previous.audio.clickSoundUri)
        }
    }
    if (previous.audio.backgroundMusicUri != current.audio.backgroundMusicUri) {
        if (!isAudioUriReferencedBySavedScheme(context, previous.audio.backgroundMusicUri)) {
            releaseCustomAudioReference(context, previous.audio.backgroundMusicUri)
        }
    }
    if (previous.startupAnimationUri != current.startupAnimationUri) {
        if (!isStartupAnimationUriReferencedByPreset(context, previous.startupAnimationUri)) {
            releasePersistableStartupAnimationReadPermission(context, previous.startupAnimationUri)
        }
    }
}

private fun ThemeStoreSummary.imageState(slot: ThemeStoreImageSlot): ThemeStoreImageState {
    return when (slot) {
        ThemeStoreImageSlot.Lkm -> lkmCard
        ThemeStoreImageSlot.ClassicMiuixLkm -> classicMiuixLkmCard
        ThemeStoreImageSlot.MaterialLkm -> materialLkmCard
        ThemeStoreImageSlot.Superuser -> superuserCard
        ThemeStoreImageSlot.Module -> moduleCard
        ThemeStoreImageSlot.StatusMonitor -> statusMonitorCard
        ThemeStoreImageSlot.SystemInfo -> systemInfoCard
        ThemeStoreImageSlot.RebootMenu -> rebootMenuCard
        ThemeStoreImageSlot.InstallImage -> installImageCard
        ThemeStoreImageSlot.InstallMethods -> installMethodsCard
        ThemeStoreImageSlot.InstallOptions -> installOptionsCard
    }
}

private fun SharedPreferences.readImageSlot(slot: ThemeStoreImageSlot): ThemeStoreImageState {
    val crop = sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = getFloat(slot.cropLeftKey, DEFAULT_CUSTOM_WALLPAPER_CROP.left),
            top = getFloat(slot.cropTopKey, DEFAULT_CUSTOM_WALLPAPER_CROP.top),
            right = getFloat(slot.cropRightKey, DEFAULT_CUSTOM_WALLPAPER_CROP.right),
            bottom = getFloat(slot.cropBottomKey, DEFAULT_CUSTOM_WALLPAPER_CROP.bottom),
        )
    )
    fun objectValue(key: String): JSONObject? = getString(key, null)?.let { value ->
        runCatching { JSONObject(value) }.getOrNull()
    }
    return ThemeStoreImageState(
        uriString = getString(slot.uriKey, null),
        videoUriString = slot.videoUriKey?.let { getString(it, null) },
        crop = crop,
        visualSettings = readMediaVisualSettings(slot.visualKeys, slot.defaultVisualSettings),
        responsiveCrops = ResponsiveCropSet.fromJson(objectValue(slot.responsiveCropsKey), crop),
        nightUriString = getString(slot.nightUriKey, null),
        nightVideoUriString = getString(slot.nightVideoUriKey, null),
        nightCrop = objectValue(slot.nightCropKey)?.let { value -> value.optCropValue(DEFAULT_CUSTOM_WALLPAPER_CROP) }
            ?: DEFAULT_CUSTOM_WALLPAPER_CROP,
        nightVisualSettings = readMediaVisualSettings(slot.nightVisualKeys, slot.defaultVisualSettings),
        nightResponsiveCrops = ResponsiveCropSet.fromJson(
            objectValue(slot.nightResponsiveCropsKey),
            DEFAULT_CUSTOM_WALLPAPER_CROP,
        ),
        variantSettings = MediaVariantSettings.fromJson(objectValue(slot.variantSettingsKey)),
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

private fun createEmptyThemeStoreConfig(displayName: String, bio: String): JSONObject {
    return JSONObject()
        .put("schema", THEME_STORE_SCHEMA)
        .put("version", THEME_STORE_VERSION)
        .put("packageType", COMPONENT_ONLY_PACKAGE_TYPE)
        .put("exportedAt", System.currentTimeMillis())
        .put(
            "author",
            JSONObject()
                .put("displayName", displayName.trim().take(64))
                .put("realName", "")
                .put("gender", ThemeAuthorGender.Unspecified.storageValue)
                .put("bio", bio.trim().take(512))
                .put("avatar", null),
        )
        .put(
            "startupSound",
            JSONObject()
                .put("asset", null)
                .put("uri", null)
                .put("durationSeconds", DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS)
                .put("volume", DEFAULT_CUSTOM_AUDIO_VOLUME.toDouble()),
        )
        .put(
            "clickSound",
            JSONObject()
                .put("asset", null)
                .put("uri", null)
                .put("volume", DEFAULT_CUSTOM_AUDIO_VOLUME.toDouble()),
        )
        .put(
            "backgroundMusic",
            JSONObject()
                .put("asset", null)
                .put("uri", null)
                .put("volume", DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME.toDouble()),
        )
        .put(
            "startupAnimation",
            JSONObject()
                .put("asset", null)
                .put("uri", null),
        )
        .put("font", appFontConfigJson(AppFontPreset.System))
}

private fun ZipOutputStream.writeActiveComponentStyles(
    context: Context,
    warnings: MutableList<ThemeStorePackageWarning>,
    budget: ThemeStoreAssetBudget,
): JSONObject {
    val store = ComponentStyleStore(context)
    val prefs = themeStorePrefs(context)
    val decoration = UiDecorationConfig.fromJsonString(
        prefs.getString(UI_DECORATION_CONFIG_KEY, null)
    )
    val cardStyleActive = decoration.enabled && (
        decoration.card == UiCardDecoration.Custom ||
            decoration.navigation == UiNavigationDecoration.Custom
        )
    val switchStyleActive = prefs.getString(SWITCH_STYLE_KEY, SwitchStyle.DEFAULT_VALUE) ==
        SwitchStyle.Custom.value
    return writeComponentStyles(
        context = context,
        content = ComponentStylePackageContent(
            cardStyle = store.readActiveCardStyle().takeIf { cardStyleActive },
            switchStyle = store.readActiveSwitchStyle().takeIf { switchStyleActive },
        ),
        warnings = warnings,
        budget = budget,
    )
}

private fun ZipOutputStream.writeComponentStyles(
    context: Context,
    content: ComponentStylePackageContent,
    warnings: MutableList<ThemeStorePackageWarning>,
    budget: ThemeStoreAssetBudget,
): JSONObject {
    return JSONObject().apply {
        content.cardStyle?.normalized()?.let { style ->
            put("cardStyle", style.toJson())
        }
        content.switchStyle?.normalized()?.let { style ->
            fun writeSwitchImage(uri: String?, sha256: String?, assetId: String): ExportedThemeAsset? {
                if (uri.isNullOrBlank()) return null
                val imageFile = ComponentStyleStore(context).resolveImageFile(uri)
                    ?: error("Image-based switch style is missing an image")
                require(fileSha256(imageFile).equals(sha256, ignoreCase = true)) {
                    "Switch style image hash does not match"
                }
                return writeUriAsset(context, uri, assetId, warnings, budget)
            }
            val imageAsset = if (style.source == CustomSwitchSource.Image) {
                writeSwitchImage(style.imageUri, style.imageSha256, "component_switch_image")
            } else null
            val imageOnAsset = if (style.source == CustomSwitchSource.Image) {
                writeSwitchImage(style.imageOnUri, style.imageOnSha256, "component_switch_image_on")
            } else null
            if (style.source == CustomSwitchSource.Image && imageAsset == null && imageOnAsset == null) {
                warnings += ThemeStorePackageWarning(
                    assetId = "component_switch_images",
                    reason = "Image-based switch style is missing its state images",
                )
            }
            put(
                "switchStyle",
                JSONObject()
                    .put("style", style.toJson(includeLocalImageUri = false))
                    .put("imageAsset", imageAsset?.toJson())
                    .put("imageUri", null)
                    .put("imageOnAsset", imageOnAsset?.toJson())
                    .put("imageOnUri", null),
            )
        }
    }
}

private fun parseComponentStyleContent(
    context: Context,
    config: JSONObject,
    assetsDir: File,
): ComponentStylePackageContent {
    val components = config.optJSONObject("components")
        ?: error("Theme package does not contain a component style")
    val cardStyle = components.optJSONObject("cardStyle")?.let(CustomCardStyle::fromJson)
    val switchOwner = components.optJSONObject("switchStyle")
    val switchStyle = switchOwner?.let { owner ->
        val packaged = CustomSwitchStyle.fromJson(
            owner.optJSONObject("style") ?: error("Switch style data is missing"),
            allowLocalImageUri = false,
        )
        if (packaged.source == CustomSwitchSource.Image) {
            val store = ComponentStyleStore(context)
            fun persistAsset(key: String) = owner.optJSONObject(key)?.let { asset ->
                val imageFile = safeComponentAssetFile(assetsDir, asset.optString("path"))
                store.persistSwitchImage(Uri.fromFile(imageFile)) to asset
            }
            val off = persistAsset("imageAsset")
            val on = persistAsset("imageOnAsset")
            require(off != null || on != null) { "Image-based switch style is missing its state images" }
            try {
                off?.let { (stored, _) ->
                    require(
                        packaged.imageSha256.isNullOrBlank() ||
                            packaged.imageSha256.equals(stored.sha256, ignoreCase = true)
                    ) { "Switch style off image hash does not match" }
                }
                on?.let { (stored, _) ->
                    require(
                        packaged.imageOnSha256.isNullOrBlank() ||
                            packaged.imageOnSha256.equals(stored.sha256, ignoreCase = true)
                    ) { "Switch style on image hash does not match" }
                }
                packaged.copy(
                    imageUri = off?.first?.uriString,
                    imageSha256 = off?.first?.sha256,
                    imageMimeType = off?.second?.optString("mimeType")?.takeIf(String::isNotBlank)
                        ?: off?.first?.mimeType,
                    imageOnUri = on?.first?.uriString,
                    imageOnSha256 = on?.first?.sha256,
                    imageOnMimeType = on?.second?.optString("mimeType")?.takeIf(String::isNotBlank)
                        ?: on?.first?.mimeType,
                ).normalized()
            } catch (error: Throwable) {
                listOfNotNull(off?.first?.uriString, on?.first?.uriString)
                    .forEach(store::discardSwitchImageIfUnreferenced)
                throw error
            }
        } else {
            packaged.copy(
                imageUri = null,
                imageSha256 = null,
                imageMimeType = null,
                imageOnUri = null,
                imageOnSha256 = null,
                imageOnMimeType = null,
            ).normalized()
        }
    }
    require(cardStyle != null || switchStyle != null) { "Component style data is empty" }
    return ComponentStylePackageContent(cardStyle = cardStyle, switchStyle = switchStyle)
}

private fun safeComponentAssetFile(assetsDir: File, packagePath: String): File {
    require(packagePath.startsWith("assets/")) { "Invalid component image path" }
    val root = assetsDir.canonicalFile
    val candidate = File(root, packagePath.removePrefix("assets/")).canonicalFile
    require(candidate.toPath().startsWith(root.toPath()) && candidate.isFile) {
        "Component image is missing"
    }
    return candidate
}

private fun fileSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
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

internal fun CustomNavigationIconState.presentationToJson(): JSONObject = normalized().let { value ->
    JSONObject()
        .put("sizeScale", value.sizeScale.toDouble())
        .put("innerPaddingDp", value.innerPaddingDp.toDouble())
        .put("verticalOffsetDp", value.verticalOffsetDp.toDouble())
        .put("opacity", value.opacity.toDouble())
        .put("tintArgb", value.tintArgb)
        .put("mask", value.mask.value)
        .put("label", value.labelOverride)
}

internal fun JSONObject?.toNavigationIconState(
    uriString: String?,
    crop: CustomWallpaperCrop,
): CustomNavigationIconState {
    val json = this ?: return CustomNavigationIconState(uriString = uriString, crop = crop)
    return CustomNavigationIconState(
        uriString = uriString,
        crop = crop,
        sizeScale = json.optDouble("sizeScale", 1.0).toFloat(),
        innerPaddingDp = json.optDouble("innerPaddingDp", 0.0).toFloat(),
        verticalOffsetDp = json.optDouble("verticalOffsetDp", 0.0).toFloat(),
        opacity = json.optDouble("opacity", 1.0).toFloat(),
        tintArgb = json.opt("tintArgb")?.takeUnless { it === JSONObject.NULL }?.let {
            (it as? Number)?.toLong()
        },
        mask = CustomNavigationIconMask.fromValue(json.optString("mask")),
        labelOverride = json.optString("label").takeIf(String::isNotBlank),
    ).normalized()
}

private fun ZipOutputStream.writeAppFont(
    context: Context,
    state: AppFontState,
    warnings: MutableList<ThemeStorePackageWarning>,
    budget: ThemeStoreAssetBudget,
): JSONObject {
    if (state.preset != AppFontPreset.Custom) {
        return appFontConfigJson(state.preset)
    }

    val file = appFontFile(context)
    var entryOpen = false
    return runCatching {
        val metadata = validateAppFontFile(file)
        require(budget.totalBytes + metadata.sizeBytes <= MAX_THEME_STORE_ASSETS_BYTES) {
            "Theme package assets are too large"
        }
        val path = "assets/$APP_FONT_CUSTOM_FILE_NAME"
        putNextEntry(ZipEntry(path))
        entryOpen = true
        FileInputStream(file).use { input -> input.copyTo(this) }
        closeEntry()
        entryOpen = false
        budget.totalBytes += metadata.sizeBytes
        JSONObject()
            .put("preset", AppFontPreset.Custom.value)
            .put(
                "name",
                sanitizeAppFontDisplayName(state.customDisplayName) ?: APP_FONT_CUSTOM_FILE_NAME,
            )
            .put(
                "asset",
                ExportedThemeAsset(
                    path = path,
                    displayName = state.customDisplayName,
                    mimeType = "font/ttf",
                ).toJson(),
            )
            .put("sha256", metadata.sha256)
            .put("sizeBytes", metadata.sizeBytes)
    }.getOrElse { error ->
        if (entryOpen) runCatching { closeEntry() }
        warnings += ThemeStorePackageWarning(
            assetId = "app_font",
            reason = error.message?.lineSequence()?.firstOrNull()?.take(160),
        )
        appFontConfigJson(AppFontPreset.System)
    }
}

private fun appFontConfigJson(preset: AppFontPreset): JSONObject {
    return JSONObject()
        .put("preset", preset.value)
        .put("name", null)
        .put("asset", null)
        .put("sha256", null)
        .put("sizeBytes", 0L)
}

private fun ZipOutputStream.writeUriAsset(
    context: Context,
    uriString: String?,
    assetId: String,
    warnings: MutableList<ThemeStorePackageWarning>,
    budget: ThemeStoreAssetBudget,
): ExportedThemeAsset? {
    if (uriString.isNullOrBlank()) return null
    val uri = uriString.toUri()
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
    require(config.optString("packageType").let { it.isBlank() || it == COMPONENT_ONLY_PACKAGE_TYPE }) {
        "Unsupported theme package type"
    }
    if (config.optString("packageType") == COMPONENT_ONLY_PACKAGE_TYPE) {
        val components = config.optJSONObject("components")
            ?: error("Component package does not contain a component style")
        val styleCount = listOf("cardStyle", "switchStyle").count { key ->
            components.optJSONObject(key) != null
        }
        require(styleCount == 1) { "Component package must contain exactly one style" }
    }
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
    if (version >= 5 && config.optString("packageType") != COMPONENT_ONLY_PACKAGE_TYPE) {
        require(config.optJSONObject("audioSettings") != null) {
            "Theme package is missing audio settings"
        }
        require(config.getJSONObject("startupAnimation").optJSONObject("settings") != null) {
            "Theme package is missing startup animation settings"
        }
        val cards = config.optJSONObject("cards") ?: error("Theme package is missing cards")
        ThemeStoreImageSlot.entries
            .filter { slot -> version >= slot.introducedInPackageVersion }
            .forEach { slot ->
                val card = cards.optJSONObject(slot.id) ?: error("Theme package is missing card ${slot.id}")
                listOf(
                    "visualSettings",
                    "responsiveCrops",
                    "nightVisualSettings",
                    "nightResponsiveCrops",
                    "variantSettings",
                ).forEach { key ->
                    require(card.optJSONObject(key) != null) { "Theme card ${slot.id} is missing $key" }
                }
            }
    }
    if (config.has("font")) {
        val font = config.optJSONObject("font")
            ?: error("Theme package font settings are invalid")
        val fontKeys = font.keys().asSequence().toSet()
        require(fontKeys.all { it in APP_FONT_PACKAGE_KEYS }) {
            "Theme package contains unknown font settings"
        }
        val presetValue = font.optString("preset")
        require(AppFontPreset.entries.any { it.value == presetValue }) {
            "Theme package font preset is invalid"
        }
        val preset = AppFontPreset.fromValue(presetValue)
        if (preset == AppFontPreset.Custom) {
            require(font.opt("name") is String) { "Theme package font name is invalid" }
            val name = sanitizeAppFontDisplayName(font.getString("name"))
            require(
                !name.isNullOrBlank() &&
                    name.length <= 96 &&
                    name.endsWith(".ttf", ignoreCase = true)
            ) { "Theme package font name is invalid" }
            require(font.optJSONObject("asset") != null) {
                "Theme package custom font is missing"
            }
            require(APP_FONT_PACKAGE_SHA256.matches(font.optString("sha256"))) {
                "Theme package font checksum is invalid"
            }
            require(font.opt("sizeBytes") is Number) {
                "Theme package font size is invalid"
            }
            require(font.getLong("sizeBytes") in 1..MAX_CUSTOM_APP_FONT_BYTES) {
                "Theme package font is too large"
            }
        } else {
            val rawAsset = font.opt("asset")
            require(rawAsset == null || rawAsset === JSONObject.NULL) {
                "Built-in font preset contains an unexpected font file"
            }
        }
    }
    config.optJSONObject("appearance")?.let(::validateThemeStoreAppearance)
    config.optJSONObject("homeLayout")?.let(::validateThemeStoreHomeLayout)
    validateComponentStyleConfig(config)
}

private fun validateThemeStoreHomeLayout(layout: JSONObject) {
    require(layout.optString("schema") == HOME_LAYOUT_TRANSFER_SCHEMA) {
        "Theme package home layout schema is invalid"
    }
    require(layout.optInt("version", 0) in 1..HOME_LAYOUT_TRANSFER_VERSION) {
        "Theme package home layout version is invalid"
    }
    listOf("enabled", "autoSnap", "autoAvoidOverlap").forEach { key ->
        require(layout.opt(key) is Boolean) { "Theme package home layout $key is invalid" }
    }

    fun validateSection(name: String) {
        val section = layout.optJSONObject(name)
            ?: error("Theme package home layout is missing $name")
        val records = section.optJSONArray("items")
            ?: error("Theme package home layout $name is missing cards")
        require(records.length() <= HomeLayoutCard.entries.size) {
            "Theme package home layout contains too many cards"
        }
        val seenCards = mutableSetOf<HomeLayoutCard>()
        for (index in 0 until records.length()) {
            val record = records.optJSONObject(index)
                ?: error("Theme package home layout card is invalid")
            val card = HomeLayoutCard.fromValue(record.optString("card"))
                ?: error("Theme package home layout card is unknown")
            require(seenCards.add(card)) { "Theme package home layout repeats a card" }
            listOf("x", "y", "width", "scale", "aspectRatio", "height", "textScale").forEach { key ->
                val value = record.opt(key) as? Number
                require(value?.toDouble()?.isFinite() == true) {
                    "Theme package home layout card $key is invalid"
                }
            }
            require(record.opt("visible") is Boolean) {
                "Theme package home layout card visibility is invalid"
            }
            require(record.opt("zIndex") is Number) {
                "Theme package home layout card layer is invalid"
            }
            require(record.optString("customTitle").length <= 80) {
                "Theme package home layout title is too long"
            }
            require(record.optString("customSubtitle").length <= 160) {
                "Theme package home layout subtitle is too long"
            }
            require(HomeLayoutWallpaperFit.entries.any { it.value == record.optString("wallpaperFit") }) {
                "Theme package home layout wallpaper fit is invalid"
            }
            val stickers = record.optJSONArray("stickers") ?: JSONArray()
            require(stickers.length() <= 12) { "Theme package home layout has too many stickers" }
            for (stickerIndex in 0 until stickers.length()) {
                val sticker = stickers.optJSONObject(stickerIndex)
                    ?: error("Theme package home layout sticker is invalid")
                require(sticker.optString("id").length <= 80) {
                    "Theme package home layout sticker ID is invalid"
                }
                require(sticker.optString("uri").length <= 2048) {
                    "Theme package home layout sticker URI is invalid"
                }
                val asset = sticker.opt("asset")
                require(asset == null || asset === JSONObject.NULL || asset is JSONObject) {
                    "Theme package home layout sticker asset is invalid"
                }
                require(asset is JSONObject || sticker.optString("uri").isNotBlank()) {
                    "Theme package home layout sticker is missing its image"
                }
                listOf("x", "y", "width", "opacity").forEach { key ->
                    val value = sticker.opt(key) as? Number
                    require(value?.toDouble()?.isFinite() == true) {
                        "Theme package home layout sticker $key is invalid"
                    }
                }
            }
        }
    }

    validateSection("portrait")
    validateSection("landscape")
    require(homeLayoutStateFromJson(layout) != null) {
        "Theme package home layout cannot be decoded"
    }
}

private fun homeLayoutStickerRecords(layout: JSONObject?): List<JSONObject> = buildList {
    if (layout == null) return@buildList
    listOf("portrait", "landscape").forEach { sectionName ->
        val items = layout.optJSONObject(sectionName)?.optJSONArray("items") ?: return@forEach
        for (itemIndex in 0 until items.length()) {
            val stickers = items.optJSONObject(itemIndex)?.optJSONArray("stickers") ?: continue
            for (stickerIndex in 0 until stickers.length()) {
                stickers.optJSONObject(stickerIndex)?.let(::add)
            }
        }
    }
}

private fun validateThemeStoreAppearance(appearance: JSONObject) {
    val allowedKeys = setOf(
        "themeMode",
        "miuixMonet",
        "keyColor",
        "colorStyle",
        "colorSpec",
        "monetSurfaceOpacity",
        "pixelStyle",
        "pixelPetEnabled",
    )
    require(appearance.keys().asSequence().all { it in allowedKeys }) {
        "Theme package contains unknown appearance settings"
    }
    appearance.opt("themeMode")?.let { raw ->
        require(raw is Number && ColorMode.entries.any { it.value == raw.toInt() }) {
            "Theme color mode is invalid"
        }
    }
    appearance.opt("miuixMonet")?.let { raw ->
        require(raw is Boolean) { "Theme Monet setting is invalid" }
    }
    appearance.opt("keyColor")?.let { raw ->
        require(raw is Number) { "Theme key color is invalid" }
    }
    appearance.opt("colorStyle")?.let { raw ->
        require(raw is String && PaletteStyle.entries.any { it.name == raw }) {
            "Theme palette style is invalid"
        }
    }
    appearance.opt("colorSpec")?.let { raw ->
        require(raw is String && ColorSpec.SpecVersion.entries.any { it.name == raw }) {
            "Theme color specification is invalid"
        }
    }
    appearance.opt("monetSurfaceOpacity")?.let { raw ->
        val value = (raw as? Number)?.toDouble()
        require(
            value != null && value.isFinite() &&
                value >= ThemeAppearanceDefaults.MIN_MONET_SURFACE_OPACITY &&
                value <= ThemeAppearanceDefaults.MAX_MONET_SURFACE_OPACITY
        ) { "Theme Monet surface opacity is invalid" }
    }
    appearance.opt("pixelStyle")?.let { raw ->
        require(raw is String && PixelStyle.entries.any { it.value == raw }) {
            "Theme pixel style is invalid"
        }
    }
    appearance.opt("pixelPetEnabled")?.let { raw ->
        require(raw is Boolean) { "Theme pixel pet setting is invalid" }
    }
}

private fun validateComponentStyleConfig(config: JSONObject) {
    val components = config.optJSONObject("components") ?: return
    val keys = components.keys().asSequence().toSet()
    require(keys.all { it == "cardStyle" || it == "switchStyle" }) {
        "Theme package contains an unknown component style"
    }
    components.optJSONObject("cardStyle")?.let { CustomCardStyle.fromJson(it) }
    components.optJSONObject("switchStyle")?.let { owner ->
        val styleJson = owner.optJSONObject("style") ?: error("Switch style data is missing")
        require(styleJson.optString("image_uri").length <= MAX_COMPONENT_PACKAGE_URI_LENGTH) {
            "Switch style local image URI is too long"
        }
        require(styleJson.optString("image_on_uri").length <= MAX_COMPONENT_PACKAGE_URI_LENGTH) {
            "Switch style local on-image URI is too long"
        }
        val style = CustomSwitchStyle.fromJson(styleJson, allowLocalImageUri = false)
        require(owner.optString("imageUri").length <= MAX_COMPONENT_PACKAGE_URI_LENGTH) {
            "Switch style image URI is too long"
        }
        require(owner.optString("imageOnUri").length <= MAX_COMPONENT_PACKAGE_URI_LENGTH) {
            "Switch style on-image URI is too long"
        }
        if (style.source == CustomSwitchSource.Image) {
            val hasOff = owner.optJSONObject("imageAsset") != null || owner.optString("imageUri").isNotBlank()
            val hasOn = owner.optJSONObject("imageOnAsset") != null || owner.optString("imageOnUri").isNotBlank()
            require(hasOff || hasOn) {
                "Image-based switch style is missing its state images"
            }
            require(!hasOff || COMPONENT_PACKAGE_SHA256.matches(styleJson.optString("image_sha256"))) {
                "Switch style image hash is invalid"
            }
            require(!hasOn || COMPONENT_PACKAGE_SHA256.matches(styleJson.optString("image_on_sha256"))) {
                "Switch style on-image hash is invalid"
            }
        } else {
            require(
                owner.optJSONObject("imageAsset") == null &&
                    owner.optString("imageUri").isBlank() &&
                    owner.optJSONObject("imageOnAsset") == null &&
                    owner.optString("imageOnUri").isBlank()
            ) {
                "Pixel switch style contains an unexpected image"
            }
        }
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
        validateOwner(
            cards?.optJSONObject(slot.id),
            "asset",
            "videoAsset",
            "nightAsset",
            "nightVideoAsset",
        )
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
    homeLayoutStickerRecords(config.optJSONObject("homeLayout")).forEach { sticker ->
        validateOwner(sticker, "asset")
    }
    val fontOwner = config.optJSONObject("font")
    validateOwner(fontOwner, "asset")
    if (AppFontPreset.fromValue(fontOwner?.optString("preset")) == AppFontPreset.Custom) {
        val fontAsset = fontOwner?.getJSONObject("asset")
            ?: error("Theme package custom font is missing")
        val path = fontAsset.getString("path")
        val fontFile = safeAssetFile(tempAssetsDir, path.removePrefix("assets/"))
        validateAppFontFile(
            file = fontFile,
            expectedSha256 = fontOwner.getString("sha256"),
            expectedSizeBytes = fontOwner.getLong("sizeBytes"),
            validateTypeface = false,
        )
    }
    val switchOwner = config.optJSONObject("components")?.optJSONObject("switchStyle")
    validateOwner(switchOwner, "imageAsset", "imageOnAsset")
    switchOwner?.let { owner ->
        val style = owner.optJSONObject("style") ?: return@let
        if (CustomSwitchSource.fromValue(style.optString("source")) == CustomSwitchSource.Image) {
            fun validateSwitchImage(assetKey: String, hashKey: String) {
                val asset = owner.optJSONObject(assetKey) ?: return
                val imageFile = safeComponentAssetFile(tempAssetsDir, asset.optString("path"))
                require(imageFile.length() in 1..MAX_COMPONENT_IMAGE_BYTES) {
                    "Component switch image is too large"
                }
                require(fileSha256(imageFile).equals(style.optString(hashKey), ignoreCase = true)) {
                    "Switch style image hash does not match"
                }
            }
            validateSwitchImage("imageAsset", "image_sha256")
            validateSwitchImage("imageOnAsset", "image_on_sha256")
        }
    }
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

private fun validateEmbeddedThemeStoreMedia(
    context: Context,
    config: JSONObject,
    tempAssetsDir: File,
) {
    fun validateOwner(owner: JSONObject?, vararg assetKeys: String) {
        if (owner == null) return
        assetKeys.forEach { assetKey ->
            val asset = owner.optJSONObject(assetKey) ?: return@forEach
            val path = asset.optString("path")
            if (!path.startsWith("assets/")) return@forEach
            val file = safeAssetFile(tempAssetsDir, path.removePrefix("assets/"))
            requireImportedMediaDecodable(
                context = context,
                file = file,
                assetId = path,
                mimeType = asset.optString("mimeType").takeIf(String::isNotBlank),
            )
        }
    }

    val cards = config.optJSONObject("cards")
    ThemeStoreImageSlot.entries.forEach { slot ->
        validateOwner(
            cards?.optJSONObject(slot.id),
            "asset",
            "videoAsset",
            "nightAsset",
            "nightVideoAsset",
        )
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
    homeLayoutStickerRecords(config.optJSONObject("homeLayout")).forEach { sticker ->
        validateOwner(sticker, "asset")
    }
    validateOwner(
        config.optJSONObject("components")?.optJSONObject("switchStyle"),
        "imageAsset",
        "imageOnAsset",
    )
}

internal fun sanitizeThemeStoreConfigForCloud(config: JSONObject) {
    fun sanitizeOwner(owner: JSONObject?, vararg assetPairs: Pair<String, String>) {
        if (owner == null) return
        assetPairs.forEach { (assetKey, uriKey) ->
            val rawAsset = owner.opt(assetKey)
            val uri = owner.optString(uriKey).trim()
            if (uri.isNotEmpty()) {
                require(rawAsset is JSONObject) {
                    "Cloud theme resource is not embedded: $uriKey"
                }
            }
            owner.remove(uriKey)
        }
    }

    config.optJSONObject("author")?.apply {
        put("realName", "")
        put("gender", ThemeAuthorGender.Unspecified.storageValue)
    }
    val mediaPairs = arrayOf("asset" to "uri", "videoAsset" to "videoUri")
    val cards = config.optJSONObject("cards")
    ThemeStoreImageSlot.entries.forEach { slot ->
        sanitizeOwner(
            cards?.optJSONObject(slot.id),
            *mediaPairs,
            "nightAsset" to "nightUri",
            "nightVideoAsset" to "nightVideoUri",
        )
    }
    val navigationIcons = config.optJSONObject("navigationIcons")
    CustomNavigationIconSlot.entries.forEach { slot ->
        sanitizeOwner(navigationIcons?.optJSONObject(slot.id), "asset" to "uri")
    }
    val pageBackgrounds = config.optJSONObject("pageBackgrounds")
    CustomPageBackgroundTarget.entries.forEach { target ->
        sanitizeOwner(pageBackgrounds?.optJSONObject(target.id), *mediaPairs)
    }
    sanitizeOwner(config.optJSONObject("wallpaper"), *mediaPairs)
    sanitizeOwner(config.optJSONObject("startupSound"), "asset" to "uri")
    sanitizeOwner(config.optJSONObject("clickSound"), "asset" to "uri")
    sanitizeOwner(config.optJSONObject("backgroundMusic"), "asset" to "uri")
    sanitizeOwner(config.optJSONObject("startupAnimation"), "asset" to "uri")
    homeLayoutStickerRecords(config.optJSONObject("homeLayout")).forEach { sticker ->
        sanitizeOwner(sticker, "asset" to "uri")
    }
    sanitizeOwner(
        config.optJSONObject("components")?.optJSONObject("switchStyle"),
        "imageAsset" to "imageUri",
        "imageOnAsset" to "imageOnUri",
    )
    config.optJSONObject("components")
        ?.optJSONObject("switchStyle")
        ?.optJSONObject("style")
        ?.apply {
            put("image_uri", null)
            put("image_on_uri", null)
        }
}

internal fun validateThemeStoreConfigForCloud(config: JSONObject) {
    fun validateOwner(owner: JSONObject?, vararg uriKeys: String) {
        if (owner == null) return
        uriKeys.forEach { uriKey ->
            require(owner.optString(uriKey).isBlank()) {
                "Cloud theme package contains a device-specific $uriKey"
            }
        }
    }

    if (config.optInt("version", 0) >= 4) {
        val author = config.optJSONObject("author")
            ?: error("Theme package is missing author information")
        require(author.optString("realName").isBlank()) {
            "Cloud theme package contains a private author name"
        }
        require(author.optString("gender") == ThemeAuthorGender.Unspecified.storageValue) {
            "Cloud theme package contains a private author gender"
        }
    }
    val uriKeys = arrayOf("uri", "videoUri")
    val cards = config.optJSONObject("cards")
    ThemeStoreImageSlot.entries.forEach { slot ->
        validateOwner(cards?.optJSONObject(slot.id), *uriKeys, "nightUri", "nightVideoUri")
    }
    val navigationIcons = config.optJSONObject("navigationIcons")
    CustomNavigationIconSlot.entries.forEach { slot ->
        validateOwner(navigationIcons?.optJSONObject(slot.id), "uri")
    }
    val pageBackgrounds = config.optJSONObject("pageBackgrounds")
    CustomPageBackgroundTarget.entries.forEach { target ->
        validateOwner(pageBackgrounds?.optJSONObject(target.id), *uriKeys)
    }
    validateOwner(config.optJSONObject("wallpaper"), *uriKeys)
    validateOwner(config.optJSONObject("startupSound"), "uri")
    validateOwner(config.optJSONObject("clickSound"), "uri")
    validateOwner(config.optJSONObject("backgroundMusic"), "uri")
    validateOwner(config.optJSONObject("startupAnimation"), "uri")
    homeLayoutStickerRecords(config.optJSONObject("homeLayout")).forEach { sticker ->
        validateOwner(sticker, "uri")
    }
    validateOwner(
        config.optJSONObject("components")?.optJSONObject("switchStyle"),
        "imageUri",
        "imageOnUri",
    )
    require(
        config.optJSONObject("components")
            ?.optJSONObject("switchStyle")
            ?.optJSONObject("style")
            ?.optString("image_uri")
            .isNullOrBlank()
    ) { "Cloud theme package contains a device-specific image_uri" }
    require(
        config.optJSONObject("components")
            ?.optJSONObject("switchStyle")
            ?.optJSONObject("style")
            ?.optString("image_on_uri")
            .isNullOrBlank()
    ) { "Cloud theme package contains a device-specific image_on_uri" }
}

private fun writeThemeStoreAssetDirectory(
    zip: ZipOutputStream,
    root: File,
    directory: File,
) {
    directory.listFiles()?.sortedBy(File::getName)?.forEach { file ->
        if (file.isDirectory) {
            writeThemeStoreAssetDirectory(zip, root, file)
        } else if (file.isFile) {
            val relativePath = file.relativeTo(root).invariantSeparatorsPath
            val entryName = validateThemeStoreArchiveEntryName("assets/$relativePath")
            zip.putNextEntry(ZipEntry(entryName))
            FileInputStream(file).use { input -> input.copyTo(zip) }
            zip.closeEntry()
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
        return hasResource(owner) ||
            hasResource(owner, "videoAsset", "videoUri") ||
            hasResource(owner, "nightAsset", "nightUri") ||
            hasResource(owner, "nightVideoAsset", "nightVideoUri")
    }

    var count = 0
    if (config.optJSONObject("appearance") != null) count++
    if (config.optJSONObject("homeLayout") != null) count++
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
    val components = config.optJSONObject("components")
    if (components?.optJSONObject("cardStyle") != null) count++
    if (components?.optJSONObject("switchStyle") != null) count++
    val fontPreset = config.optJSONObject("font")?.optString("preset")
    if (!fontPreset.isNullOrBlank() && fontPreset != AppFontPreset.System.value) count++
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
        add(config.optJSONObject("components")?.optJSONObject("switchStyle"))
    }
    return owners.firstNotNullOfOrNull { owner ->
        readThemeStorePreviewImage(owner, "asset", tempAssetsDir)
            ?: readThemeStorePreviewImage(owner, "nightAsset", tempAssetsDir)
            ?: readThemeStorePreviewImage(owner, "imageAsset", tempAssetsDir)
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
            openThemeStoreUriInputStream(context, uriString.toUri()).use { }
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
        checkOwner(owner, "card_${slot.id}_night", "nightAsset", "nightUri")
        checkOwner(owner, "card_${slot.id}_night_video", "nightVideoAsset", "nightVideoUri")
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
    checkOwner(
        config.optJSONObject("components")?.optJSONObject("switchStyle"),
        "component_switch_image",
        "imageAsset",
        "imageUri",
    )
    checkOwner(
        config.optJSONObject("components")?.optJSONObject("switchStyle"),
        "component_switch_image_on",
        "imageOnAsset",
        "imageOnUri",
    )
}

private const val MAX_COMPONENT_PACKAGE_URI_LENGTH = 1_024
private const val COMPONENT_ONLY_PACKAGE_TYPE = "component"
private val COMPONENT_PACKAGE_SHA256 = Regex("[a-fA-F0-9]{64}")
private val APP_FONT_PACKAGE_KEYS = setOf("preset", "name", "asset", "sha256", "sizeBytes")

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
        requireImportedMediaDecodable(
            context = context,
            file = stagingFile,
            assetId = assetId,
            mimeType = rawAsset.optString("mimeType").takeIf(String::isNotBlank),
        )
        val targetFile = safeAssetFile(targetAssetsDir, relativePath)
        return ImportedThemeAsset.Resolved(Uri.fromFile(targetFile).toString())
    }

    val legacyUriString = assetOwnerJson.optString(uriKey).takeIf { it.isNotBlank() }
        ?: return ImportedThemeAsset.Resolved(null)
    val startingBudget = budget.totalBytes
    return runCatching {
        val legacyUri = legacyUriString.toUri()
        val mimeType = runCatching { context.contentResolver.getType(legacyUri) }.getOrNull()
        val extension = safeAssetExtension(
            displayName = queryDisplayName(context, legacyUri) ?: legacyUri.lastPathSegment,
            mimeType = mimeType,
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
            requireImportedMediaDecodable(context, stagingFile, assetId, mimeType)
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

private fun requireImportedMediaDecodable(
    context: Context,
    file: File,
    assetId: String,
    mimeType: String?,
) {
    val info = inspectMediaFileBlocking(context, Uri.fromFile(file), mimeType)
    require(info.decodable) {
        "Unable to decode imported media $assetId: ${info.error ?: info.displayName}"
    }
}

private fun importPackagedAppFont(
    fontJson: JSONObject,
    tempAssetsDir: File,
    stagingAssetsDir: File,
): AppFontState {
    val asset = fontJson.getJSONObject("asset")
    val packagePath = asset.getString("path")
    require(packagePath.startsWith("assets/")) { "Invalid embedded font path" }
    val source = safeAssetFile(tempAssetsDir, packagePath.removePrefix("assets/"))
    val destination = safeAssetFile(stagingAssetsDir, APP_FONT_CUSTOM_FILE_NAME)
    destination.parentFile?.mkdirs()
    val temporary = File(destination.parentFile, "$APP_FONT_CUSTOM_FILE_NAME.importing")
    temporary.delete()
    try {
        FileInputStream(source).use { input ->
            FileOutputStream(temporary).use { output -> input.copyTo(output) }
        }
        val metadata = validateAppFontFile(
            file = temporary,
            expectedSha256 = fontJson.getString("sha256"),
            expectedSizeBytes = fontJson.getLong("sizeBytes"),
        )
        if (!temporary.renameTo(destination)) {
            temporary.copyTo(destination, overwrite = true)
            temporary.delete()
        }
        return AppFontState(
            preset = AppFontPreset.Custom,
            customDisplayName = sanitizeAppFontDisplayName(fontJson.optString("name"))
                ?: APP_FONT_CUSTOM_FILE_NAME,
            customSha256 = metadata.sha256,
            customSizeBytes = metadata.sizeBytes,
            customFileAvailable = true,
        )
    } finally {
        temporary.delete()
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

private fun JSONObject.optCropValue(fallback: CustomWallpaperCrop): CustomWallpaperCrop {
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = optDouble("left", fallback.left.toDouble()).toFloat(),
            top = optDouble("top", fallback.top.toDouble()).toFloat(),
            right = optDouble("right", fallback.right.toDouble()).toFloat(),
            bottom = optDouble("bottom", fallback.bottom.toDouble()).toFloat(),
        )
    )
}
