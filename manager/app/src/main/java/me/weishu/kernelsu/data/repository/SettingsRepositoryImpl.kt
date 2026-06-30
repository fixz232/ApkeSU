package me.weishu.kernelsu.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.edit
import com.topjohnwu.superuser.ShellUtils
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.magica.BootCompletedReceiver
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.GLOBAL_SNOW_EFFECT_KEY
import me.weishu.kernelsu.ui.component.GLOBAL_SNOW_ENABLED_KEY
import me.weishu.kernelsu.ui.component.GlobalSnowEffect
import me.weishu.kernelsu.ui.component.SWITCH_STYLE_KEY
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.theme.CustomThemePreset
import me.weishu.kernelsu.ui.theme.DELTA_COLOR_VARIANT_KEY
import me.weishu.kernelsu.ui.theme.DeltaColorVariant
import me.weishu.kernelsu.ui.theme.ThemeAppearanceDefaults
import me.weishu.kernelsu.ui.theme.ThemeAppearanceSnapshot
import me.weishu.kernelsu.ui.theme.ThemePreset
import me.weishu.kernelsu.ui.theme.ThemeSyncStrategy
import me.weishu.kernelsu.ui.theme.THEME_SYNC_STRATEGY_KEY
import me.weishu.kernelsu.ui.theme.defaultThemePresetForUiMode
import me.weishu.kernelsu.ui.theme.themePreferenceKey
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_CROP_BOTTOM_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_CROP_LEFT_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_CROP_RIGHT_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_CROP_TOP_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_OPACITY_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_PASSTHROUGH_ENABLED_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_URI_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_VIDEO_BACKGROUND_URI_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_STARTUP_ANIMATION_URI_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_BACKGROUND_MUSIC_URI_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_CLICK_SOUND_URI_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_CLICK_SOUND_VOLUME_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_STARTUP_SOUND_DURATION_SECONDS_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_STARTUP_SOUND_URI_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_STARTUP_SOUND_VOLUME_KEY
import me.weishu.kernelsu.ui.util.CustomNavigationIconSet
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.CustomPageBackgroundSet
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_AUDIO_VOLUME
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.LauncherIconOption
import me.weishu.kernelsu.ui.util.applyLauncherIcon
import me.weishu.kernelsu.ui.util.execKsud
import me.weishu.kernelsu.ui.util.getBuiltinMountStatus as readBuiltinMountStatus
import me.weishu.kernelsu.ui.util.getFeaturePersistValue
import me.weishu.kernelsu.ui.util.getFeatureStatus
import me.weishu.kernelsu.ui.util.getKPatchNextStatus as readKPatchNextStatus
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import me.weishu.kernelsu.ui.util.releasePersistableVideoBackgroundReadPermission
import me.weishu.kernelsu.ui.util.clearCustomPageBackground as clearPageBackground
import me.weishu.kernelsu.ui.util.readCustomNavigationIconSet
import me.weishu.kernelsu.ui.util.readCustomPageBackgroundSet
import me.weishu.kernelsu.ui.util.sanitizeCustomAudioVolume
import me.weishu.kernelsu.ui.util.sanitizeCustomBackgroundMusicVolume
import me.weishu.kernelsu.ui.util.sanitizeCustomStartupSoundDurationSeconds
import me.weishu.kernelsu.ui.util.sanitizeCustomVideoBackgroundDurationSeconds
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperCrop
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperOpacity
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperPassthroughOpacity
import me.weishu.kernelsu.ui.util.setCustomPageBackgroundCrop as writeCustomPageBackgroundCrop
import me.weishu.kernelsu.ui.util.setCustomPageBackgroundOpacity as writeCustomPageBackgroundOpacity
import me.weishu.kernelsu.ui.util.setCustomPageBackgroundVideo as writeCustomPageBackgroundVideo
import me.weishu.kernelsu.ui.util.setCustomPageBackgroundVideoDurationSeconds as writeCustomPageBackgroundVideoDurationSeconds
import me.weishu.kernelsu.ui.util.setCustomPageBackgroundWallpaper as writeCustomPageBackgroundWallpaper
import me.weishu.kernelsu.ui.util.setBuiltinMountDefaultMode as writeBuiltinMountDefaultMode
import me.weishu.kernelsu.ui.util.setBuiltinMountEnabled as writeBuiltinMountEnabled
import me.weishu.kernelsu.ui.util.setKPatchNextEnabled as writeKPatchNextEnabled
import me.weishu.kernelsu.ui.util.getEpkesuHideStatus as readEpkesuHideStatus
import me.weishu.kernelsu.ui.util.setEpkesuHideEnabled as writeEpkesuHideEnabled
import me.weishu.kernelsu.ui.util.setCustomNavigationIcon as writeCustomNavigationIcon
import me.weishu.kernelsu.ui.util.setCustomNavigationIconCrop as writeCustomNavigationIconCrop
import java.util.UUID

class SettingsRepositoryImpl : SettingsRepository {

    private val prefs by lazy {
        ksuApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override var uiMode: String
        get() = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        set(value) = prefs.edit { putString("ui_mode", value) }

    override var checkModuleUpdate: Boolean
        get() = prefs.getBoolean("module_check_update", true)
        set(value) = prefs.edit { putBoolean("module_check_update", value) }

    override var showVersionMismatchWarning: Boolean
        get() = prefs.getBoolean(SHOW_VERSION_MISMATCH_WARNING_KEY, true)
        set(value) = prefs.edit { putBoolean(SHOW_VERSION_MISMATCH_WARNING_KEY, value) }

    override var showHomeSupportCard: Boolean
        get() = prefs.getBoolean(SHOW_HOME_SUPPORT_CARD_KEY, true)
        set(value) = prefs.edit { putBoolean(SHOW_HOME_SUPPORT_CARD_KEY, value) }

    override var showHomeLearnCard: Boolean
        get() = prefs.getBoolean(SHOW_HOME_LEARN_CARD_KEY, true)
        set(value) = prefs.edit { putBoolean(SHOW_HOME_LEARN_CARD_KEY, value) }

    override var themeMode: Int
        get() = prefs.getInt(themeKey("color_mode"), defaultThemePreset.colorMode.value)
        set(value) = prefs.edit {
            putInt(themeKey("color_mode"), value)
            putString(themeKey("theme_preset"), ThemePreset.CUSTOM.value)
        }

    override var miuixMonet: Boolean
        get() = prefs.getBoolean(themeKey("miuix_monet"), defaultThemePreset.miuixMonet)
        set(value) = prefs.edit {
            putBoolean(themeKey("miuix_monet"), value)
            putString(themeKey("theme_preset"), ThemePreset.CUSTOM.value)
        }

    override var keyColor: Int
        get() = prefs.getInt(themeKey("key_color"), defaultThemePreset.keyColor)
        set(value) = prefs.edit {
            putInt(themeKey("key_color"), value)
            putString(themeKey("theme_preset"), ThemePreset.CUSTOM.value)
        }

    override var colorStyle: String
        get() = prefs.getString(themeKey("color_style"), defaultThemePreset.paletteStyle.name)
            ?: defaultThemePreset.paletteStyle.name
        set(value) = prefs.edit {
            putString(themeKey("color_style"), value)
            putString(themeKey("theme_preset"), ThemePreset.CUSTOM.value)
        }

    override var colorSpec: String
        get() = prefs.getString(themeKey("color_spec"), defaultThemePreset.colorSpec.name)
            ?: defaultThemePreset.colorSpec.name
        set(value) = prefs.edit {
            putString(themeKey("color_spec"), value)
            putString(themeKey("theme_preset"), ThemePreset.CUSTOM.value)
        }

    override var themePreset: String
        get() = prefs.getString(themeKey("theme_preset"), "") ?: ""
        set(value) = prefs.edit { putString(themeKey("theme_preset"), value) }

    override var enablePredictiveBack: Boolean
        get() = prefs.getBoolean("enable_predictive_back", false)
        set(value) = prefs.edit { putBoolean("enable_predictive_back", value) }

    override var enableBlur: Boolean
        get() = prefs.getBoolean(themeKey("enable_blur"), defaultThemePreset.enableBlur)
        set(value) = prefs.edit {
            putBoolean(themeKey("enable_blur"), value)
            putString(themeKey("theme_preset"), ThemePreset.CUSTOM.value)
        }

    override var enableFloatingBottomBar: Boolean
        get() = prefs.getBoolean(themeKey("enable_floating_bottom_bar"), defaultThemePreset.enableFloatingBottomBar)
        set(value) = prefs.edit {
            putBoolean(themeKey("enable_floating_bottom_bar"), value)
            putString(themeKey("theme_preset"), ThemePreset.CUSTOM.value)
        }

    override var enableFloatingBottomBarBlur: Boolean
        get() = prefs.getBoolean(themeKey("enable_floating_bottom_bar_blur"), defaultThemePreset.enableFloatingBottomBarBlur)
        set(value) = prefs.edit {
            putBoolean(themeKey("enable_floating_bottom_bar_blur"), value)
            putString(themeKey("theme_preset"), ThemePreset.CUSTOM.value)
        }

    override var pageScale: Float
        get() = sanitizeScale(
            prefs.getFloat(themeKey("page_scale"), defaultThemePreset.pageScale),
            min = 0.8f,
            max = 1.1f,
            fallback = defaultThemePreset.pageScale,
        )
        set(value) = prefs.edit {
            putFloat(themeKey("page_scale"), sanitizeScale(value, 0.8f, 1.1f, defaultThemePreset.pageScale))
            putString(themeKey("theme_preset"), ThemePreset.CUSTOM.value)
        }

    override var fontScale: Float
        get() = sanitizeScale(
            prefs.getFloat(themeKey("font_scale"), ThemeAppearanceDefaults.FONT_SCALE),
            min = 0.85f,
            max = 1.2f,
            fallback = ThemeAppearanceDefaults.FONT_SCALE,
        )
        set(value) = prefs.edit {
            putFloat(themeKey("font_scale"), sanitizeScale(value, 0.85f, 1.2f, ThemeAppearanceDefaults.FONT_SCALE))
            putString(themeKey("theme_preset"), ThemePreset.CUSTOM.value)
        }

    override var blurIntensity: Float
        get() = sanitizeScale(
            prefs.getFloat(themeKey("blur_intensity"), ThemeAppearanceDefaults.BLUR_INTENSITY),
            min = 0.5f,
            max = 1.5f,
            fallback = ThemeAppearanceDefaults.BLUR_INTENSITY,
        )
        set(value) = prefs.edit {
            putFloat(
                themeKey("blur_intensity"),
                sanitizeScale(value, 0.5f, 1.5f, ThemeAppearanceDefaults.BLUR_INTENSITY)
            )
            putString(themeKey("theme_preset"), ThemePreset.CUSTOM.value)
        }

    override var switchStyle: String
        get() = SwitchStyle.fromValue(prefs.getString(SWITCH_STYLE_KEY, SwitchStyle.DEFAULT_VALUE)).value
        set(value) = prefs.edit { putString(SWITCH_STYLE_KEY, SwitchStyle.fromValue(value).value) }

    override var globalSnowEnabled: Boolean
        get() = prefs.getBoolean(GLOBAL_SNOW_ENABLED_KEY, false)
        set(value) = prefs.edit { putBoolean(GLOBAL_SNOW_ENABLED_KEY, value) }

    override var globalSnowEffect: String
        get() = GlobalSnowEffect.fromValue(
            prefs.getString(GLOBAL_SNOW_EFFECT_KEY, GlobalSnowEffect.DEFAULT_VALUE)
        ).value
        set(value) = prefs.edit {
            putString(GLOBAL_SNOW_EFFECT_KEY, GlobalSnowEffect.fromValue(value).value)
        }

    override var themeSyncStrategy: ThemeSyncStrategy
        get() = ThemeSyncStrategy.fromValue(prefs.getString(THEME_SYNC_STRATEGY_KEY, ThemeSyncStrategy.SHARED.value))
        set(value) {
            val oldValue = themeSyncStrategy
            if (oldValue == value) return
            val currentSnapshot = currentThemeSnapshot()
            val currentPreset = themePreset
            prefs.edit {
                putString(THEME_SYNC_STRATEGY_KEY, value.value)
                if (value == ThemeSyncStrategy.PER_STYLE) {
                    InterfaceStyle.entries.forEach { style ->
                        writeThemeSnapshot(currentSnapshot, style.value, value)
                    }
                    putString(themeKey("theme_preset", value, uiMode), currentPreset)
                } else {
                    writeThemeSnapshot(currentSnapshot, UiMode.DEFAULT_VALUE, ThemeSyncStrategy.SHARED)
                    putString(themeKey("theme_preset", ThemeSyncStrategy.SHARED, UiMode.DEFAULT_VALUE), currentPreset)
                }
            }
        }

    override var enableWebDebugging: Boolean
        get() = prefs.getBoolean("enable_web_debugging", false)
        set(value) = prefs.edit { putBoolean("enable_web_debugging", value) }

    override var deltaColorVariant: String
        get() = DeltaColorVariant.fromValue(
            prefs.getString(DELTA_COLOR_VARIANT_KEY, DeltaColorVariant.DEFAULT_VALUE)
                ?: DeltaColorVariant.DEFAULT_VALUE
        ).value
        set(value) = prefs.edit {
            putString(DELTA_COLOR_VARIANT_KEY, DeltaColorVariant.fromValue(value).value)
        }

    override var autoJailbreak: Boolean
        get() = prefs.getBoolean("auto_jailbreak", false)
        set(value) {
            runCatching {
                ksuApp.packageManager.setComponentEnabledSetting(
                    ComponentName(ksuApp, BootCompletedReceiver::class.java),
                    if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }.onFailure {
                Log.e("Settings", "failed to change boot receiver state to $value", it)
            }
            prefs.edit {
                putBoolean("auto_jailbreak", value)
            }
        }

    override var launcherIcon: String
        get() = LauncherIconOption.fromValue(
            prefs.getString(LauncherIconOption.PREF_KEY, LauncherIconOption.DEFAULT_VALUE)
        ).value
        set(value) {
            val option = LauncherIconOption.fromValue(value)
            if (applyLauncherIcon(ksuApp, option)) {
                prefs.edit {
                    putString(LauncherIconOption.PREF_KEY, option.value)
                }
            }
        }

    override var customManagerName: String
        get() = sanitizeCustomManagerName(prefs.getString(CUSTOM_MANAGER_NAME_KEY, null))
        set(value) {
            val name = sanitizeCustomManagerName(value)
            prefs.edit {
                if (name.isBlank()) {
                    remove(CUSTOM_MANAGER_NAME_KEY)
                } else {
                    putString(CUSTOM_MANAGER_NAME_KEY, name)
                }
            }
        }

    override var customWallpaperUri: String?
        get() = prefs.getString(CUSTOM_WALLPAPER_URI_KEY, null)
        set(value) {
            val previous = customWallpaperUri
            val previousVideo = customVideoBackgroundUri
            prefs.edit(commit = true) {
                if (value.isNullOrBlank()) {
                    remove(CUSTOM_WALLPAPER_URI_KEY)
                    remove(CUSTOM_WALLPAPER_CROP_LEFT_KEY)
                    remove(CUSTOM_WALLPAPER_CROP_TOP_KEY)
                    remove(CUSTOM_WALLPAPER_CROP_RIGHT_KEY)
                    remove(CUSTOM_WALLPAPER_CROP_BOTTOM_KEY)
                } else {
                    putString(CUSTOM_WALLPAPER_URI_KEY, value)
                    remove(CUSTOM_VIDEO_BACKGROUND_URI_KEY)
                    putFloat(CUSTOM_WALLPAPER_CROP_LEFT_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.left)
                    putFloat(CUSTOM_WALLPAPER_CROP_TOP_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.top)
                    putFloat(CUSTOM_WALLPAPER_CROP_RIGHT_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.right)
                    putFloat(CUSTOM_WALLPAPER_CROP_BOTTOM_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.bottom)
                }
            }
            if (previous != value) {
                releaseCustomImageReference(ksuApp, previous)
            }
            if (!value.isNullOrBlank() && previousVideo != null) {
                releasePersistableVideoBackgroundReadPermission(ksuApp, previousVideo)
            }
        }

    override var customWallpaperOpacity: Float
        get() = sanitizeCustomWallpaperOpacity(
            prefs.getFloat(CUSTOM_WALLPAPER_OPACITY_KEY, DEFAULT_CUSTOM_WALLPAPER_OPACITY)
        )
        set(value) = prefs.edit {
            putFloat(CUSTOM_WALLPAPER_OPACITY_KEY, sanitizeCustomWallpaperOpacity(value))
        }

    override var customWallpaperCrop: CustomWallpaperCrop
        get() = sanitizeCustomWallpaperCrop(
            CustomWallpaperCrop(
                left = prefs.getFloat(CUSTOM_WALLPAPER_CROP_LEFT_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.left),
                top = prefs.getFloat(CUSTOM_WALLPAPER_CROP_TOP_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.top),
                right = prefs.getFloat(CUSTOM_WALLPAPER_CROP_RIGHT_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.right),
                bottom = prefs.getFloat(CUSTOM_WALLPAPER_CROP_BOTTOM_KEY, DEFAULT_CUSTOM_WALLPAPER_CROP.bottom),
            )
        )
        set(value) = prefs.edit(commit = true) {
            val crop = sanitizeCustomWallpaperCrop(value)
            putFloat(CUSTOM_WALLPAPER_CROP_LEFT_KEY, crop.left)
            putFloat(CUSTOM_WALLPAPER_CROP_TOP_KEY, crop.top)
            putFloat(CUSTOM_WALLPAPER_CROP_RIGHT_KEY, crop.right)
            putFloat(CUSTOM_WALLPAPER_CROP_BOTTOM_KEY, crop.bottom)
        }

    override var customWallpaperPassthroughEnabled: Boolean
        get() = prefs.getBoolean(CUSTOM_WALLPAPER_PASSTHROUGH_ENABLED_KEY, false)
        set(value) = prefs.edit { putBoolean(CUSTOM_WALLPAPER_PASSTHROUGH_ENABLED_KEY, value) }

    override var customWallpaperPassthroughOpacity: Float
        get() = sanitizeCustomWallpaperPassthroughOpacity(
            prefs.getFloat(
                CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY_KEY,
                DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
            )
        )
        set(value) = prefs.edit {
            putFloat(CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY_KEY, sanitizeCustomWallpaperPassthroughOpacity(value))
        }

    override var customVideoBackgroundUri: String?
        get() = prefs.getString(CUSTOM_VIDEO_BACKGROUND_URI_KEY, null)
        set(value) {
            val previous = customVideoBackgroundUri
            val previousWallpaper = customWallpaperUri
            prefs.edit(commit = true) {
                if (value.isNullOrBlank()) {
                    remove(CUSTOM_VIDEO_BACKGROUND_URI_KEY)
                } else {
                    putString(CUSTOM_VIDEO_BACKGROUND_URI_KEY, value)
                    remove(CUSTOM_WALLPAPER_URI_KEY)
                    remove(CUSTOM_WALLPAPER_CROP_LEFT_KEY)
                    remove(CUSTOM_WALLPAPER_CROP_TOP_KEY)
                    remove(CUSTOM_WALLPAPER_CROP_RIGHT_KEY)
                    remove(CUSTOM_WALLPAPER_CROP_BOTTOM_KEY)
                }
            }
            if (previous != value) {
                releasePersistableVideoBackgroundReadPermission(ksuApp, previous)
            }
            if (!value.isNullOrBlank() && previousWallpaper != null) {
                releaseCustomImageReference(ksuApp, previousWallpaper)
            }
        }

    override var customVideoBackgroundDurationSeconds: Int
        get() = sanitizeCustomVideoBackgroundDurationSeconds(
            prefs.getInt(
                CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS_KEY,
                DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
            )
        )
        set(value) = prefs.edit {
            putInt(
                CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS_KEY,
                sanitizeCustomVideoBackgroundDurationSeconds(value),
            )
        }

    override val customPageBackgrounds: CustomPageBackgroundSet
        get() = prefs.readCustomPageBackgroundSet()

    override fun setCustomPageBackgroundWallpaper(target: CustomPageBackgroundTarget, uriString: String?) {
        writeCustomPageBackgroundWallpaper(ksuApp, target, uriString)
    }

    override fun setCustomPageBackgroundVideo(target: CustomPageBackgroundTarget, uriString: String?) {
        writeCustomPageBackgroundVideo(ksuApp, target, uriString)
    }

    override fun setCustomPageBackgroundOpacity(target: CustomPageBackgroundTarget, opacity: Float) {
        writeCustomPageBackgroundOpacity(ksuApp, target, opacity)
    }

    override fun setCustomPageBackgroundCrop(target: CustomPageBackgroundTarget, crop: CustomWallpaperCrop) {
        writeCustomPageBackgroundCrop(ksuApp, target, crop)
    }

    override fun setCustomPageBackgroundVideoDurationSeconds(
        target: CustomPageBackgroundTarget,
        seconds: Int,
    ) {
        writeCustomPageBackgroundVideoDurationSeconds(ksuApp, target, seconds)
    }

    override fun clearCustomPageBackground(target: CustomPageBackgroundTarget) {
        clearPageBackground(ksuApp, target)
    }

    override var customStartupSoundUri: String?
        get() = prefs.getString(CUSTOM_STARTUP_SOUND_URI_KEY, null)
        set(value) = prefs.edit {
            if (value.isNullOrBlank()) {
                remove(CUSTOM_STARTUP_SOUND_URI_KEY)
            } else {
                putString(CUSTOM_STARTUP_SOUND_URI_KEY, value)
            }
        }

    override var customStartupSoundDurationSeconds: Int
        get() = sanitizeCustomStartupSoundDurationSeconds(
            prefs.getInt(
                CUSTOM_STARTUP_SOUND_DURATION_SECONDS_KEY,
                DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
            )
        )
        set(value) = prefs.edit {
            putInt(
                CUSTOM_STARTUP_SOUND_DURATION_SECONDS_KEY,
                sanitizeCustomStartupSoundDurationSeconds(value),
            )
        }

    override var customStartupSoundVolume: Float
        get() = sanitizeCustomAudioVolume(
            prefs.getFloat(CUSTOM_STARTUP_SOUND_VOLUME_KEY, DEFAULT_CUSTOM_AUDIO_VOLUME)
        )
        set(value) = prefs.edit {
            putFloat(CUSTOM_STARTUP_SOUND_VOLUME_KEY, sanitizeCustomAudioVolume(value))
        }

    override var customClickSoundUri: String?
        get() = prefs.getString(CUSTOM_CLICK_SOUND_URI_KEY, null)
        set(value) = prefs.edit {
            if (value.isNullOrBlank()) {
                remove(CUSTOM_CLICK_SOUND_URI_KEY)
            } else {
                putString(CUSTOM_CLICK_SOUND_URI_KEY, value)
            }
        }

    override var customClickSoundVolume: Float
        get() = sanitizeCustomAudioVolume(
            prefs.getFloat(CUSTOM_CLICK_SOUND_VOLUME_KEY, DEFAULT_CUSTOM_AUDIO_VOLUME)
        )
        set(value) = prefs.edit {
            putFloat(CUSTOM_CLICK_SOUND_VOLUME_KEY, sanitizeCustomAudioVolume(value))
        }

    override var customBackgroundMusicUri: String?
        get() = prefs.getString(CUSTOM_BACKGROUND_MUSIC_URI_KEY, null)
        set(value) = prefs.edit {
            if (value.isNullOrBlank()) {
                remove(CUSTOM_BACKGROUND_MUSIC_URI_KEY)
            } else {
                putString(CUSTOM_BACKGROUND_MUSIC_URI_KEY, value)
            }
        }

    override var customBackgroundMusicVolume: Float
        get() = sanitizeCustomBackgroundMusicVolume(
            prefs.getFloat(CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY, DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME)
        )
        set(value) = prefs.edit {
            putFloat(CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY, sanitizeCustomBackgroundMusicVolume(value))
        }

    override var customStartupAnimationUri: String?
        get() = prefs.getString(CUSTOM_STARTUP_ANIMATION_URI_KEY, null)
        set(value) = prefs.edit {
            if (value.isNullOrBlank()) {
                remove(CUSTOM_STARTUP_ANIMATION_URI_KEY)
            } else {
                putString(CUSTOM_STARTUP_ANIMATION_URI_KEY, value)
            }
        }

    override val customNavigationIcons: CustomNavigationIconSet
        get() = prefs.readCustomNavigationIconSet()

    override fun setCustomNavigationIcon(slot: CustomNavigationIconSlot, uriString: String?) {
        writeCustomNavigationIcon(ksuApp, slot, uriString)
    }

    override fun setCustomNavigationIconCrop(slot: CustomNavigationIconSlot, crop: CustomWallpaperCrop) {
        writeCustomNavigationIconCrop(ksuApp, slot, crop)
    }

    override suspend fun getSuCompatStatus(): String = getFeatureStatus("su_compat")

    override suspend fun getSuCompatPersistValue(): Long? = getFeaturePersistValue("su_compat")

    override fun isSuEnabled(): Boolean = Natives.isSuEnabled()

    override fun setSuEnabled(enabled: Boolean): Boolean = Natives.setSuEnabled(enabled)

    override fun setSuCompatModePref(mode: Int) = prefs.edit { putInt("su_compat_mode", mode) }

    override fun getSuCompatModePref(): Int = prefs.getInt("su_compat_mode", 0)

    override suspend fun getKernelUmountStatus(): String = getFeatureStatus("kernel_umount")

    override fun isKernelUmountEnabled(): Boolean = Natives.isKernelUmountEnabled()

    override fun setKernelUmountEnabled(enabled: Boolean): Boolean = Natives.setKernelUmountEnabled(enabled)

    override suspend fun getSelinuxHideStatus(): String {
        val status = getFeatureStatus("selinux_hide")
        if (status == "managed") {
            return status
        }
        return if (Natives.isSelinuxHideSupported()) "supported" else "unsupported"
    }

    override fun isSelinuxHideEnabled(): Boolean = Natives.isSelinuxHideEnabled()

    override fun setSelinuxHideEnabled(enabled: Boolean): Int = Natives.setSelinuxHideEnabled(enabled)

    override suspend fun getSulogStatus(): String = getFeatureStatus("sulog")

    override suspend fun getSulogPersistValue(): Long? = getFeaturePersistValue("sulog")

    override fun setSulogEnabled(enabled: Boolean): Boolean = execKsud("feature set sulog ${if (enabled) 1 else 0}", true)

    override suspend fun getAdbRootStatus(): String = getFeatureStatus("adb_root")

    override suspend fun getAdbRootPersistValue(): Long? = getFeaturePersistValue("adb_root")

    override fun setAdbRootEnabled(enabled: Boolean): Boolean =
        if (execKsud("feature set adb_root ${if (enabled) 1 else 0}", true)) {
            ShellUtils.fastCmd("setprop ctl.restart adbd")
            true
        } else {
            false
        }

    override suspend fun getAvcSpoofStatus(): String = getFeatureStatus("avc_spoof")

    override fun isAvcSpoofEnabled(): Boolean = Natives.isAvcSpoofEnabled()

    override fun setAvcSpoofEnabled(enabled: Boolean): Boolean = Natives.setAvcSpoofEnabled(enabled)

    override fun isDefaultUmountModules(): Boolean = Natives.isDefaultUmountModules()

    override fun setDefaultUmountModules(enabled: Boolean): Boolean = Natives.setDefaultUmountModules(enabled)

    override suspend fun getBuiltinMountStatus() = readBuiltinMountStatus()

    override fun setBuiltinMountEnabled(enabled: Boolean): Boolean = writeBuiltinMountEnabled(enabled)

    override fun setBuiltinMountDefaultMode(mode: String): Boolean = writeBuiltinMountDefaultMode(mode)

    override suspend fun getKPatchNextStatus() = readKPatchNextStatus()

    override fun setKPatchNextEnabled(enabled: Boolean): Boolean = writeKPatchNextEnabled(enabled)

    override suspend fun getEpkesuHideStatus(): Boolean = readEpkesuHideStatus().enabled

    override fun setEpkesuHideEnabled(enabled: Boolean): Boolean = writeEpkesuHideEnabled(enabled)

    override fun isLkmMode(): Boolean = Natives.isLkmMode

    override fun applyThemePreset(preset: ThemePreset) {
        val targetUiMode = preset.targetUiMode(uiMode)
        prefs.edit {
            putString("ui_mode", targetUiMode)
            writeThemeSnapshot(
                ThemeAppearanceSnapshot(
                    colorMode = preset.colorMode.value,
                    miuixMonet = preset.miuixMonet,
                    keyColor = preset.keyColor,
                    colorStyle = preset.paletteStyle.name,
                    colorSpec = preset.colorSpec.name,
                    enableBlur = preset.enableBlur,
                    enableFloatingBottomBar = preset.enableFloatingBottomBar,
                    enableFloatingBottomBarBlur = preset.enableFloatingBottomBarBlur,
                    pageScale = preset.pageScale,
                    fontScale = ThemeAppearanceDefaults.FONT_SCALE,
                    blurIntensity = ThemeAppearanceDefaults.BLUR_INTENSITY,
                ),
                targetUiMode,
            )
            putString(themeKey("theme_preset", themeSyncStrategy, targetUiMode), preset.value)
        }
    }

    override fun saveCustomThemePreset(name: String): CustomThemePreset? {
        val sanitizedName = name.trim().take(40)
        if (sanitizedName.isBlank()) return null
        val preset = CustomThemePreset(
            id = UUID.randomUUID().toString(),
            name = sanitizedName,
            uiMode = uiMode,
            updatedAt = System.currentTimeMillis(),
            snapshot = currentThemeSnapshot(),
        )
        prefs.edit {
            val ids = getCustomThemePresetIds().toMutableList()
            ids += preset.id
            putStringSet(CUSTOM_THEME_PRESET_IDS_KEY, ids.toSet())
            writeCustomThemePreset(preset)
        }
        return preset
    }

    override fun applyCustomThemePreset(presetId: String): Boolean {
        val preset = getCustomThemePresets().firstOrNull { it.id == presetId } ?: return false
        prefs.edit {
            putString("ui_mode", preset.uiMode)
            writeThemeSnapshot(preset.snapshot, preset.uiMode)
            putString(themeKey("theme_preset", themeSyncStrategy, preset.uiMode), ThemePreset.CUSTOM.value)
        }
        return true
    }

    override fun renameCustomThemePreset(presetId: String, name: String): Boolean {
        val preset = getCustomThemePresets().firstOrNull { it.id == presetId } ?: return false
        val sanitizedName = name.trim().take(40)
        if (sanitizedName.isBlank()) return false
        prefs.edit {
            writeCustomThemePreset(
                preset.copy(
                    name = sanitizedName,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
        return true
    }

    override fun deleteCustomThemePreset(presetId: String): Boolean {
        if (presetId !in getCustomThemePresetIds()) return false
        prefs.edit {
            val ids = getCustomThemePresetIds().filterNot { it == presetId }
            putStringSet(CUSTOM_THEME_PRESET_IDS_KEY, ids.toSet())
            CUSTOM_THEME_PRESET_FIELDS.forEach { field ->
                remove(customThemePresetKey(presetId, field))
            }
        }
        return true
    }

    override fun getCustomThemePresets(): List<CustomThemePreset> {
        return getCustomThemePresetIds()
            .mapNotNull(::readCustomThemePreset)
            .sortedByDescending { it.updatedAt }
    }

    override fun resetThemeToDefault() {
        val preset = defaultThemePresetForUiMode(uiMode)
        prefs.edit {
            writeThemeSnapshot(
                ThemeAppearanceSnapshot(
                    colorMode = preset.colorMode.value,
                    miuixMonet = preset.miuixMonet,
                    keyColor = preset.keyColor,
                    colorStyle = preset.paletteStyle.name,
                    colorSpec = preset.colorSpec.name,
                    enableBlur = preset.enableBlur,
                    enableFloatingBottomBar = preset.enableFloatingBottomBar,
                    enableFloatingBottomBarBlur = preset.enableFloatingBottomBarBlur,
                    pageScale = preset.pageScale,
                    fontScale = ThemeAppearanceDefaults.FONT_SCALE,
                    blurIntensity = ThemeAppearanceDefaults.BLUR_INTENSITY,
                ),
                uiMode,
            )
            putString(themeKey("theme_preset"), preset.value)
        }
    }

    override fun execKsudFeatureSave() {
        execKsud("feature save", true)
    }

    private val defaultThemePreset: ThemePreset
        get() = defaultThemePresetForUiMode(uiMode)

    private fun themeKey(base: String): String {
        return themeKey(base, themeSyncStrategy, uiMode)
    }

    private fun themeKey(
        base: String,
        strategy: ThemeSyncStrategy,
        style: String,
    ): String {
        return themePreferenceKey(base, strategy, style)
    }

    private fun currentThemeSnapshot(): ThemeAppearanceSnapshot {
        return ThemeAppearanceSnapshot(
            colorMode = themeMode,
            miuixMonet = miuixMonet,
            keyColor = keyColor,
            colorStyle = colorStyle,
            colorSpec = colorSpec,
            enableBlur = enableBlur,
            enableFloatingBottomBar = enableFloatingBottomBar,
            enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
            pageScale = pageScale,
            fontScale = fontScale,
            blurIntensity = blurIntensity,
        )
    }

    private fun android.content.SharedPreferences.Editor.writeThemeSnapshot(
        snapshot: ThemeAppearanceSnapshot,
        style: String,
        strategy: ThemeSyncStrategy = themeSyncStrategy,
    ) {
        putInt(themeKey("color_mode", strategy, style), snapshot.colorMode)
        putBoolean(themeKey("miuix_monet", strategy, style), snapshot.miuixMonet)
        putInt(themeKey("key_color", strategy, style), snapshot.keyColor)
        putString(themeKey("color_style", strategy, style), snapshot.colorStyle)
        putString(themeKey("color_spec", strategy, style), snapshot.colorSpec)
        putBoolean(themeKey("enable_blur", strategy, style), snapshot.enableBlur)
        putBoolean(themeKey("enable_floating_bottom_bar", strategy, style), snapshot.enableFloatingBottomBar)
        putBoolean(themeKey("enable_floating_bottom_bar_blur", strategy, style), snapshot.enableFloatingBottomBarBlur)
        putFloat(themeKey("page_scale", strategy, style), snapshot.pageScale)
        putFloat(themeKey("font_scale", strategy, style), snapshot.fontScale)
        putFloat(themeKey("blur_intensity", strategy, style), snapshot.blurIntensity)
    }

    private fun getCustomThemePresetIds(): List<String> {
        return prefs.getStringSet(CUSTOM_THEME_PRESET_IDS_KEY, emptySet()).orEmpty().toList()
    }

    private fun readCustomThemePreset(id: String): CustomThemePreset? {
        val name = prefs.getString(customThemePresetKey(id, "name"), null)?.takeIf { it.isNotBlank() } ?: return null
        val uiMode = prefs.getString(customThemePresetKey(id, "ui_mode"), UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        val defaultPreset = defaultThemePresetForUiMode(uiMode)
        val updatedAt = prefs.getLong(customThemePresetKey(id, "updated_at"), 0L)
        val snapshot = ThemeAppearanceSnapshot(
            colorMode = prefs.getInt(customThemePresetKey(id, "color_mode"), defaultPreset.colorMode.value),
            miuixMonet = prefs.getBoolean(customThemePresetKey(id, "miuix_monet"), defaultPreset.miuixMonet),
            keyColor = prefs.getInt(customThemePresetKey(id, "key_color"), defaultPreset.keyColor),
            colorStyle = prefs.getString(customThemePresetKey(id, "color_style"), defaultPreset.paletteStyle.name)
                ?: defaultPreset.paletteStyle.name,
            colorSpec = prefs.getString(customThemePresetKey(id, "color_spec"), defaultPreset.colorSpec.name)
                ?: defaultPreset.colorSpec.name,
            enableBlur = prefs.getBoolean(customThemePresetKey(id, "enable_blur"), defaultPreset.enableBlur),
            enableFloatingBottomBar = prefs.getBoolean(
                customThemePresetKey(id, "enable_floating_bottom_bar"),
                defaultPreset.enableFloatingBottomBar,
            ),
            enableFloatingBottomBarBlur = prefs.getBoolean(
                customThemePresetKey(id, "enable_floating_bottom_bar_blur"),
                defaultPreset.enableFloatingBottomBarBlur,
            ),
            pageScale = sanitizeScale(
                prefs.getFloat(customThemePresetKey(id, "page_scale"), defaultPreset.pageScale),
                0.8f,
                1.1f,
                defaultPreset.pageScale,
            ),
            fontScale = sanitizeScale(
                prefs.getFloat(customThemePresetKey(id, "font_scale"), ThemeAppearanceDefaults.FONT_SCALE),
                0.85f,
                1.2f,
                ThemeAppearanceDefaults.FONT_SCALE,
            ),
            blurIntensity = sanitizeScale(
                prefs.getFloat(customThemePresetKey(id, "blur_intensity"), ThemeAppearanceDefaults.BLUR_INTENSITY),
                0.5f,
                1.5f,
                ThemeAppearanceDefaults.BLUR_INTENSITY,
            ),
        )
        return CustomThemePreset(id, name, uiMode, updatedAt, snapshot)
    }

    private fun android.content.SharedPreferences.Editor.writeCustomThemePreset(preset: CustomThemePreset) {
        putString(customThemePresetKey(preset.id, "name"), preset.name)
        putString(customThemePresetKey(preset.id, "ui_mode"), preset.uiMode)
        putLong(customThemePresetKey(preset.id, "updated_at"), preset.updatedAt)
        putInt(customThemePresetKey(preset.id, "color_mode"), preset.snapshot.colorMode)
        putBoolean(customThemePresetKey(preset.id, "miuix_monet"), preset.snapshot.miuixMonet)
        putInt(customThemePresetKey(preset.id, "key_color"), preset.snapshot.keyColor)
        putString(customThemePresetKey(preset.id, "color_style"), preset.snapshot.colorStyle)
        putString(customThemePresetKey(preset.id, "color_spec"), preset.snapshot.colorSpec)
        putBoolean(customThemePresetKey(preset.id, "enable_blur"), preset.snapshot.enableBlur)
        putBoolean(customThemePresetKey(preset.id, "enable_floating_bottom_bar"), preset.snapshot.enableFloatingBottomBar)
        putBoolean(
            customThemePresetKey(preset.id, "enable_floating_bottom_bar_blur"),
            preset.snapshot.enableFloatingBottomBarBlur,
        )
        putFloat(customThemePresetKey(preset.id, "page_scale"), preset.snapshot.pageScale)
        putFloat(customThemePresetKey(preset.id, "font_scale"), preset.snapshot.fontScale)
        putFloat(customThemePresetKey(preset.id, "blur_intensity"), preset.snapshot.blurIntensity)
    }

    private fun customThemePresetKey(id: String, field: String): String {
        return "custom_theme_preset_${id}_${field}"
    }

    private fun sanitizeScale(value: Float, min: Float, max: Float, fallback: Float): Float {
        return if (value.isFinite()) value.coerceIn(min, max) else fallback
    }

    private fun sanitizeCustomManagerName(value: String?): String {
        return value.orEmpty().trim().take(MAX_CUSTOM_MANAGER_NAME_LENGTH)
    }

    private companion object {
        const val CUSTOM_MANAGER_NAME_KEY = "custom_manager_name"
        const val MAX_CUSTOM_MANAGER_NAME_LENGTH = 40
        const val CUSTOM_THEME_PRESET_IDS_KEY = "custom_theme_preset_ids"
        val CUSTOM_THEME_PRESET_FIELDS = listOf(
            "name",
            "ui_mode",
            "updated_at",
            "color_mode",
            "miuix_monet",
            "key_color",
            "color_style",
            "color_spec",
            "enable_blur",
            "enable_floating_bottom_bar",
            "enable_floating_bottom_bar_blur",
            "page_scale",
            "font_scale",
            "blur_intensity",
        )
    }
}
