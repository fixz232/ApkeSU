package me.weishu.kernelsu.ui.viewmodel

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.GlobalSnowEffect
import me.weishu.kernelsu.ui.component.SwitchStyle
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
    val switchStyle: String = SwitchStyle.DEFAULT_VALUE,
    val globalSnowEnabled: Boolean = false,
    val globalSnowEffect: String = GlobalSnowEffect.DEFAULT_VALUE,
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
