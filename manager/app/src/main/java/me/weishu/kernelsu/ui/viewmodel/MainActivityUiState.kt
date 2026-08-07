package me.weishu.kernelsu.ui.viewmodel

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.GlobalScrollEffect
import me.weishu.kernelsu.ui.component.GlobalSnowEffect
import me.weishu.kernelsu.ui.component.DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.PageTransitionEffect
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.custom.CustomCardStyle
import me.weishu.kernelsu.ui.component.custom.CustomSwitchStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.snow.DEFAULT_SEASON_CARD_MOTION_ENABLED
import me.weishu.kernelsu.ui.component.rain.RainStyle
import me.weishu.kernelsu.ui.component.rain.DEFAULT_RAIN_CARD_MOTION_ENABLED
import me.weishu.kernelsu.ui.component.ink.DEFAULT_INK_FONT_ENABLED
import me.weishu.kernelsu.ui.component.ink.DEFAULT_INK_CARD_MOTION_ENABLED
import me.weishu.kernelsu.ui.component.ink.InkStyle
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.pixel.DEFAULT_PIXEL_CARD_MOTION_ENABLED
import me.weishu.kernelsu.ui.component.decoration.UiDecorationConfig
import me.weishu.kernelsu.ui.theme.AppSettings
import me.weishu.kernelsu.ui.util.AppFontState
import me.weishu.kernelsu.ui.util.AppAudioSettings
import me.weishu.kernelsu.ui.util.CustomNavigationIconSet
import me.weishu.kernelsu.ui.util.CustomPageBackgroundSet
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_VIDEO_BACKGROUND_FRAME_RATE
import me.weishu.kernelsu.ui.util.MediaVisualSettings
import me.weishu.kernelsu.ui.util.StartupAnimationSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val appFont: AppFontState,
    val pageScale: Float,
    val fontScale: Float,
    val blurIntensity: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val autoHideNavigationBar: Boolean,
    val scrollHideNavigationBar: Boolean,
    val moduleTopBarAutoHideEnabled: Boolean,
    val switchStyle: String = SwitchStyle.DEFAULT_VALUE,
    val customCardStyle: CustomCardStyle? = null,
    val customSwitchStyle: CustomSwitchStyle? = null,
    val seasonStyle: String = SeasonStyle.DEFAULT_VALUE,
    val seasonCardMotionEnabled: Boolean = DEFAULT_SEASON_CARD_MOTION_ENABLED,
    val rainStyle: String = RainStyle.DEFAULT_VALUE,
    val rainCardMotionEnabled: Boolean = DEFAULT_RAIN_CARD_MOTION_ENABLED,
    val inkStyle: String = InkStyle.DEFAULT_VALUE,
    val inkFontEnabled: Boolean = DEFAULT_INK_FONT_ENABLED,
    val inkCardMotionEnabled: Boolean = DEFAULT_INK_CARD_MOTION_ENABLED,
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
    val backgroundScrollFollowEnabled: Boolean = false,
    val pageTransitionEffect: String = PageTransitionEffect.DEFAULT_VALUE,
    val uiMode: UiMode,
    val interfaceStyle: String,
    val customWallpaperUri: String?,
    val customWallpaperOpacity: Float,
    val customWallpaperVisualSettings: MediaVisualSettings,
    val customWallpaperCrop: CustomWallpaperCrop,
    val customWallpaperPassthroughEnabled: Boolean,
    val customWallpaperPassthroughOpacity: Float,
    val customVideoBackgroundUri: String?,
    val customVideoBackgroundDurationSeconds: Int,
    val customVideoBackgroundFrameRate: Int = DEFAULT_CUSTOM_VIDEO_BACKGROUND_FRAME_RATE,
    val customPageBackgrounds: CustomPageBackgroundSet,
    val customStartupAnimationUri: String?,
    val startupAnimationSettings: StartupAnimationSettings,
    val appAudioSettings: AppAudioSettings,
    val customStartupSoundUri: String?,
    val customClickSoundUri: String?,
    val customClickSoundVolume: Float,
    val customBackgroundMusicUri: String?,
    val customBackgroundMusicVolume: Float,
    val customNavigationIcons: CustomNavigationIconSet,
    val deltaColorVariant: String,
)
