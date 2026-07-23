package me.weishu.kernelsu.ui.viewmodel

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.GlobalScrollEffect
import me.weishu.kernelsu.ui.component.GlobalSnowEffect
import me.weishu.kernelsu.ui.component.DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.snow.DEFAULT_SEASON_CARD_MOTION_ENABLED
import me.weishu.kernelsu.ui.component.rain.RainStyle
import me.weishu.kernelsu.ui.component.rain.DEFAULT_RAIN_CARD_MOTION_ENABLED
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.pixel.DEFAULT_PIXEL_CARD_MOTION_ENABLED
import me.weishu.kernelsu.ui.component.decoration.UiDecorationConfig
import me.weishu.kernelsu.ui.theme.AppSettings
import me.weishu.kernelsu.ui.util.CustomNavigationIconSet
import me.weishu.kernelsu.ui.util.CustomPageBackgroundSet
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val fontScale: Float,
    val blurIntensity: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val autoHideNavigationBar: Boolean,
    val scrollHideNavigationBar: Boolean,
    val switchStyle: String = SwitchStyle.DEFAULT_VALUE,
    val seasonStyle: String = SeasonStyle.DEFAULT_VALUE,
    val seasonCardMotionEnabled: Boolean = DEFAULT_SEASON_CARD_MOTION_ENABLED,
    val rainStyle: String = RainStyle.DEFAULT_VALUE,
    val rainCardMotionEnabled: Boolean = DEFAULT_RAIN_CARD_MOTION_ENABLED,
    val pixelStyle: String = PixelStyle.DEFAULT_VALUE,
    val pixelCardMotionEnabled: Boolean = DEFAULT_PIXEL_CARD_MOTION_ENABLED,
    val uiDecorationConfig: UiDecorationConfig = UiDecorationConfig(),
    val globalSnowEnabled: Boolean = false,
    val globalSnowEffect: String = GlobalSnowEffect.DEFAULT_VALUE,
    val nightBackgroundEffect: String = NightBackgroundEffect.DEFAULT_VALUE,
    val nightBackgroundPassthrough: Boolean = false,
    val nightBackgroundPassthroughOpacity: Float = DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY,
    val globalScrollEffectEnabled: Boolean = false,
    val globalScrollEffect: String = GlobalScrollEffect.DEFAULT_VALUE,
    val uiMode: UiMode,
    val interfaceStyle: String,
    val customWallpaperUri: String?,
    val customWallpaperOpacity: Float,
    val customWallpaperCrop: CustomWallpaperCrop,
    val customWallpaperPassthroughEnabled: Boolean,
    val customWallpaperPassthroughOpacity: Float,
    val customVideoBackgroundUri: String?,
    val customVideoBackgroundDurationSeconds: Int,
    val customPageBackgrounds: CustomPageBackgroundSet,
    val customStartupAnimationUri: String?,
    val customStartupSoundUri: String?,
    val customClickSoundUri: String?,
    val customClickSoundVolume: Float,
    val customBackgroundMusicUri: String?,
    val customBackgroundMusicVolume: Float,
    val customNavigationIcons: CustomNavigationIconSet,
    val deltaColorVariant: String,
)
