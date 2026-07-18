package me.weishu.kernelsu.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.weishu.kernelsu.data.repository.SettingsRepository
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.GLOBAL_SCROLL_EFFECT_ENABLED_KEY
import me.weishu.kernelsu.ui.component.GLOBAL_SCROLL_EFFECT_KEY
import me.weishu.kernelsu.ui.component.AUTO_HIDE_NAVIGATION_BAR_KEY
import me.weishu.kernelsu.ui.component.GLOBAL_SNOW_EFFECT_KEY
import me.weishu.kernelsu.ui.component.GLOBAL_SNOW_ENABLED_KEY
import me.weishu.kernelsu.ui.component.GlobalScrollEffect
import me.weishu.kernelsu.ui.component.GlobalSnowEffect
import me.weishu.kernelsu.ui.component.NIGHT_BACKGROUND_EFFECT_KEY
import me.weishu.kernelsu.ui.component.NIGHT_BACKGROUND_PASSTHROUGH_KEY
import me.weishu.kernelsu.ui.component.NIGHT_BACKGROUND_PASSTHROUGH_OPACITY_KEY
import me.weishu.kernelsu.ui.component.DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.SCROLL_HIDE_NAVIGATION_BAR_KEY
import me.weishu.kernelsu.ui.component.SWITCH_STYLE_KEY
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.snow.SEASON_STYLE_KEY
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.pixel.PIXEL_STYLE_KEY
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.decoration.UI_DECORATION_CONFIG_KEY
import me.weishu.kernelsu.ui.component.decoration.UiDecorationConfig
import me.weishu.kernelsu.ui.theme.AppSettings
import me.weishu.kernelsu.ui.theme.DELTA_COLOR_VARIANT_KEY
import me.weishu.kernelsu.ui.theme.DeltaColorVariant
import me.weishu.kernelsu.ui.theme.THEME_SYNC_STRATEGY_KEY
import me.weishu.kernelsu.ui.theme.ThemeController
import me.weishu.kernelsu.ui.theme.ThemePreset
import me.weishu.kernelsu.ui.theme.ThemePreferenceKeys
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.CustomNavigationIconSet
import me.weishu.kernelsu.ui.util.CustomPageBackgroundSet
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.CUSTOM_PAGE_BACKGROUND_PREFERENCE_KEYS
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_AUDIO_VOLUME
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.CUSTOM_BACKGROUND_MUSIC_URI_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY
import me.weishu.kernelsu.ui.util.CUSTOM_CLICK_SOUND_VOLUME_KEY

class MainActivityViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val prefs = ksuApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val settingRepo: SettingsRepository = SettingsRepositoryImpl()
    private val mainPageState = MainPageState(savedStateHandle)
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in observedKeys) {
            _uiState.value = readUiStateSafely()
        }
    }

    private val _uiState = MutableStateFlow(readUiStateSafely())
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()
    val selectedMainPage: StateFlow<Int> = mainPageState.selectedPage

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
        super.onCleared()
    }

    fun setSelectedMainPage(page: Int) {
        mainPageState.updateSelectedPage(page)
    }

    private fun readUiStateSafely(): MainActivityUiState {
        return runCatching { readUiState() }.getOrElse {
            Log.e(TAG, "read activity settings failed", it)
            fallbackUiState()
        }
    }

    private fun readUiState(): MainActivityUiState {
        val interfaceStyle = settingRepo.uiMode
        val isLiquidGlassInterface = interfaceStyle == InterfaceStyle.LiquidGlass.value
        return MainActivityUiState(
            appSettings = ThemeController.getAppSettings(ksuApp),
            pageScale = settingRepo.pageScale,
            fontScale = settingRepo.fontScale,
            blurIntensity = settingRepo.blurIntensity,
            enableBlur = if (isLiquidGlassInterface) false else settingRepo.enableBlur,
            enableFloatingBottomBar = settingRepo.enableFloatingBottomBar,
            enableFloatingBottomBarBlur = if (isLiquidGlassInterface) false else settingRepo.enableFloatingBottomBarBlur,
            autoHideNavigationBar = settingRepo.autoHideNavigationBar,
            scrollHideNavigationBar = settingRepo.scrollHideNavigationBar,
            switchStyle = settingRepo.switchStyle,
            seasonStyle = settingRepo.seasonStyle,
            pixelStyle = settingRepo.pixelStyle,
            uiDecorationConfig = settingRepo.uiDecorationConfig,
            globalSnowEnabled = settingRepo.globalSnowEnabled,
            globalSnowEffect = settingRepo.globalSnowEffect,
            nightBackgroundEffect = settingRepo.nightBackgroundEffect,
            nightBackgroundPassthrough = settingRepo.nightBackgroundPassthrough,
            nightBackgroundPassthroughOpacity = settingRepo.nightBackgroundPassthroughOpacity,
            globalScrollEffectEnabled = settingRepo.globalScrollEffectEnabled,
            globalScrollEffect = settingRepo.globalScrollEffect,
            uiMode = UiMode.fromValue(interfaceStyle),
            interfaceStyle = interfaceStyle,
            customWallpaperUri = settingRepo.customWallpaperUri,
            customWallpaperOpacity = settingRepo.customWallpaperOpacity,
            customWallpaperCrop = settingRepo.customWallpaperCrop,
            customWallpaperPassthroughEnabled = settingRepo.customWallpaperPassthroughEnabled,
            customWallpaperPassthroughOpacity = settingRepo.customWallpaperPassthroughOpacity,
            customVideoBackgroundUri = settingRepo.customVideoBackgroundUri,
            customVideoBackgroundDurationSeconds = settingRepo.customVideoBackgroundDurationSeconds,
            customPageBackgrounds = settingRepo.customPageBackgrounds,
            customStartupAnimationUri = settingRepo.customStartupAnimationUri,
            customStartupSoundUri = settingRepo.customStartupSoundUri,
            customClickSoundUri = settingRepo.customClickSoundUri,
            customClickSoundVolume = settingRepo.customClickSoundVolume,
            customBackgroundMusicUri = settingRepo.customBackgroundMusicUri,
            customBackgroundMusicVolume = settingRepo.customBackgroundMusicVolume,
            customNavigationIcons = settingRepo.customNavigationIcons,
            deltaColorVariant = settingRepo.deltaColorVariant,
        )
    }

    private fun fallbackUiState(): MainActivityUiState {
        val preset = ThemePreset.CLEAN_TOOL
        return MainActivityUiState(
            appSettings = AppSettings(
                colorMode = preset.colorMode,
                keyColor = preset.keyColor,
                paletteStyle = preset.paletteStyle,
                colorSpec = preset.colorSpec,
            ),
            pageScale = 1f,
            fontScale = 1f,
            blurIntensity = 1f,
            enableBlur = false,
            enableFloatingBottomBar = false,
            enableFloatingBottomBarBlur = false,
            autoHideNavigationBar = false,
            scrollHideNavigationBar = false,
            switchStyle = SwitchStyle.DEFAULT_VALUE,
            seasonStyle = SeasonStyle.DEFAULT_VALUE,
            pixelStyle = PixelStyle.DEFAULT_VALUE,
            uiDecorationConfig = UiDecorationConfig(),
            globalSnowEnabled = false,
            globalSnowEffect = GlobalSnowEffect.DEFAULT_VALUE,
            nightBackgroundEffect = NightBackgroundEffect.DEFAULT_VALUE,
            nightBackgroundPassthrough = false,
            nightBackgroundPassthroughOpacity = DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY,
            globalScrollEffectEnabled = false,
            globalScrollEffect = GlobalScrollEffect.DEFAULT_VALUE,
            uiMode = UiMode.fromValue(InterfaceStyle.Miuix.value),
            interfaceStyle = InterfaceStyle.Miuix.value,
            customWallpaperUri = null,
            customWallpaperOpacity = DEFAULT_CUSTOM_WALLPAPER_OPACITY,
            customWallpaperCrop = CustomWallpaperCrop(),
            customWallpaperPassthroughEnabled = false,
            customWallpaperPassthroughOpacity = DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY,
            customVideoBackgroundUri = null,
            customVideoBackgroundDurationSeconds = DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
            customPageBackgrounds = CustomPageBackgroundSet(),
            customStartupAnimationUri = null,
            customStartupSoundUri = null,
            customClickSoundUri = null,
            customClickSoundVolume = DEFAULT_CUSTOM_AUDIO_VOLUME,
            customBackgroundMusicUri = null,
            customBackgroundMusicVolume = DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME,
            customNavigationIcons = CustomNavigationIconSet(),
            deltaColorVariant = DeltaColorVariant.DEFAULT_VALUE,
        )
    }

    private companion object {
        private const val TAG = "MainActivityViewModel"

        val observedKeys = buildSet {
            add(THEME_SYNC_STRATEGY_KEY)
            addAll(ThemePreferenceKeys)
            InterfaceStyle.entries.forEach { style ->
                ThemePreferenceKeys.forEach { key ->
                    add("${key}_${style.value}")
                }
            }
            addAll(
                listOf(
            "ui_mode",
            SWITCH_STYLE_KEY,
            SEASON_STYLE_KEY,
            PIXEL_STYLE_KEY,
            UI_DECORATION_CONFIG_KEY,
            GLOBAL_SNOW_ENABLED_KEY,
            GLOBAL_SNOW_EFFECT_KEY,
            NIGHT_BACKGROUND_EFFECT_KEY,
            NIGHT_BACKGROUND_PASSTHROUGH_KEY,
            NIGHT_BACKGROUND_PASSTHROUGH_OPACITY_KEY,
            GLOBAL_SCROLL_EFFECT_ENABLED_KEY,
            GLOBAL_SCROLL_EFFECT_KEY,
            AUTO_HIDE_NAVIGATION_BAR_KEY,
            SCROLL_HIDE_NAVIGATION_BAR_KEY,
            DELTA_COLOR_VARIANT_KEY,
            "custom_wallpaper_uri",
            "custom_wallpaper_opacity",
            "custom_wallpaper_crop_left",
            "custom_wallpaper_crop_top",
            "custom_wallpaper_crop_right",
            "custom_wallpaper_crop_bottom",
            "custom_wallpaper_passthrough_enabled",
            "custom_wallpaper_passthrough_opacity",
            "custom_video_background_uri",
            "custom_video_background_duration_seconds",
            "custom_startup_animation_uri",
            "custom_startup_sound_uri",
            "custom_click_sound_uri",
            CUSTOM_CLICK_SOUND_VOLUME_KEY,
            CUSTOM_BACKGROUND_MUSIC_URI_KEY,
            CUSTOM_BACKGROUND_MUSIC_VOLUME_KEY,
                )
            )
            CustomNavigationIconSlot.entries.forEach { slot ->
                add(slot.uriKey)
                add(slot.cropLeftKey)
                add(slot.cropTopKey)
                add(slot.cropRightKey)
                add(slot.cropBottomKey)
            }
            addAll(CUSTOM_PAGE_BACKGROUND_PREFERENCE_KEYS)
        }
    }
}

private const val SELECTED_MAIN_PAGE_KEY = "selected_main_page"

private class MainPageState(
    private val savedStateHandle: SavedStateHandle,
) {
    val selectedPage: StateFlow<Int> = savedStateHandle.getStateFlow(SELECTED_MAIN_PAGE_KEY, 0)

    fun updateSelectedPage(page: Int) {
        savedStateHandle[SELECTED_MAIN_PAGE_KEY] = MainPagerConfig.coercePage(page)
    }
}

object MainPagerConfig {
    const val PAGE_COUNT = 4
    const val LAST_PAGE_INDEX = PAGE_COUNT - 1

    fun coercePage(page: Int): Int = page.coerceIn(0, LAST_PAGE_INDEX)
}
