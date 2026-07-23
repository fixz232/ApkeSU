package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.runtime.Immutable
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.weishu.kernelsu.ui.theme.CustomThemePreset
import me.weishu.kernelsu.ui.theme.DeltaColorVariant
import me.weishu.kernelsu.ui.theme.ThemeAppearanceDefaults
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.theme.ThemePreset
import me.weishu.kernelsu.ui.theme.ThemeSyncStrategy
import me.weishu.kernelsu.ui.component.GlobalScrollEffect
import me.weishu.kernelsu.ui.component.GlobalSnowEffect
import me.weishu.kernelsu.ui.component.DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.decoration.UiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.CustomUiDecorationPreset
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.snow.DEFAULT_SEASON_CARD_MOTION_ENABLED
import me.weishu.kernelsu.ui.component.rain.RainStyle
import me.weishu.kernelsu.ui.component.rain.DEFAULT_RAIN_CARD_MOTION_ENABLED
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.pixel.DEFAULT_PIXEL_CARD_MOTION_ENABLED
import me.weishu.kernelsu.ui.util.CustomNavigationIconSet
import me.weishu.kernelsu.ui.util.CustomPageBackgroundSet
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.BUILTIN_MOUNT_MODE_OVERLAY
import me.weishu.kernelsu.ui.util.BUILTIN_MOUNT_VARIANT_LITE
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_AUDIO_VOLUME
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.LauncherIconOption

enum class UiDecorationSaveState {
    Idle,
    Saving,
    Saved,
    Failed,
}

@Immutable
data class SettingsUiState(
    val uiMode: String = UiMode.DEFAULT_VALUE,
    val checkModuleUpdate: Boolean = true,
    val showVersionMismatchWarning: Boolean = true,
    val showGkiWarning: Boolean = true,
    val showHomeSupportCard: Boolean = true,
    val showHomeLearnCard: Boolean = true,
    val graphicsRendererFeatureEnabled: Boolean = false,
    val themeMode: Int = 0,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val colorStyle: String = PaletteStyle.TonalSpot.name,
    val colorSpec: String = ColorSpec.SpecVersion.Default.name,
    val themePreset: String = ThemePreset.CLEAN_TOOL.value,
    val enablePredictiveBack: Boolean = false,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val autoHideNavigationBar: Boolean = false,
    val scrollHideNavigationBar: Boolean = false,
    val pageScale: Float = 1.0f,
    val fontScale: Float = ThemeAppearanceDefaults.FONT_SCALE,
    val blurIntensity: Float = ThemeAppearanceDefaults.BLUR_INTENSITY,
    val switchStyle: String = SwitchStyle.DEFAULT_VALUE,
    val seasonStyle: String = SeasonStyle.DEFAULT_VALUE,
    val seasonCardMotionEnabled: Boolean = DEFAULT_SEASON_CARD_MOTION_ENABLED,
    val rainStyle: String = RainStyle.DEFAULT_VALUE,
    val rainCardMotionEnabled: Boolean = DEFAULT_RAIN_CARD_MOTION_ENABLED,
    val pixelStyle: String = PixelStyle.DEFAULT_VALUE,
    val pixelCardMotionEnabled: Boolean = DEFAULT_PIXEL_CARD_MOTION_ENABLED,
    val uiDecorationConfig: UiDecorationConfig = UiDecorationConfig(),
    val uiDecorationSaveState: UiDecorationSaveState = UiDecorationSaveState.Idle,
    val customUiDecorationPresets: List<CustomUiDecorationPreset> = emptyList(),
    val recentUiDecorationComponents: List<String> = emptyList(),
    val globalSnowEnabled: Boolean = false,
    val globalSnowEffect: String = GlobalSnowEffect.DEFAULT_VALUE,
    val nightBackgroundEffect: String = NightBackgroundEffect.DEFAULT_VALUE,
    val nightBackgroundPassthrough: Boolean = false,
    val nightBackgroundPassthroughOpacity: Float = DEFAULT_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY,
    val globalScrollEffectEnabled: Boolean = false,
    val globalScrollEffect: String = GlobalScrollEffect.DEFAULT_VALUE,
    val themeSyncStrategy: ThemeSyncStrategy = ThemeSyncStrategy.SHARED,
    val customThemePresets: List<CustomThemePreset> = emptyList(),
    val enableWebDebugging: Boolean = false,
    val launcherIcon: String = LauncherIconOption.DEFAULT_VALUE,
    val customManagerName: String = "",
    val customHomeTitle: String = "",
    val customWallpaperUri: String? = null,
    val customWallpaperOpacity: Float = DEFAULT_CUSTOM_WALLPAPER_OPACITY,
    val customWallpaperCrop: CustomWallpaperCrop = CustomWallpaperCrop(),
    val customWallpaperPassthroughEnabled: Boolean = false,
    val customWallpaperPassthroughOpacity: Float = DEFAULT_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY,
    val customVideoBackgroundUri: String? = null,
    val customVideoBackgroundDurationSeconds: Int = DEFAULT_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS,
    val customPageBackgrounds: CustomPageBackgroundSet = CustomPageBackgroundSet(),
    val customStartupAnimationUri: String? = null,
    val customStartupSoundUri: String? = null,
    val customStartupSoundDurationSeconds: Int = DEFAULT_CUSTOM_STARTUP_SOUND_DURATION_SECONDS,
    val customStartupSoundVolume: Float = DEFAULT_CUSTOM_AUDIO_VOLUME,
    val customClickSoundUri: String? = null,
    val customClickSoundVolume: Float = DEFAULT_CUSTOM_AUDIO_VOLUME,
    val customBackgroundMusicUri: String? = null,
    val customBackgroundMusicVolume: Float = DEFAULT_CUSTOM_BACKGROUND_MUSIC_VOLUME,
    val customNavigationIcons: CustomNavigationIconSet = CustomNavigationIconSet(),
    val deltaColorVariant: String = DeltaColorVariant.DEFAULT_VALUE,

    // Su Compat
    val suCompatStatus: String = "",
    val suCompatMode: Int = 0, // 0: enable default, 1: disable until reboot, 2: disable always
    val isSuEnabled: Boolean = false,

    // Kernel Umount
    val kernelUmountStatus: String = "",
    val isKernelUmountEnabled: Boolean = false,

    // SELinux Hide
    val selinuxHideStatus: String = "",
    val isSelinuxHideEnabled: Boolean = false,

    // SU Log
    val sulogStatus: String = "",
    val isSulogEnabled: Boolean = false,

    // Umount Modules
    val isDefaultUmountModules: Boolean = false,

    // Built-in Hybrid Mount Lite
    val isBuiltinMountEnabled: Boolean = false,
    val builtinMountDefaultMode: String = BUILTIN_MOUNT_MODE_OVERLAY,
    val builtinMountVariant: String = BUILTIN_MOUNT_VARIANT_LITE,
    val isBuiltinMountWebUiAvailable: Boolean = false,
    val builtinMountConflict: String? = null,
    val builtinMountSourceUrl: String = "",
    val builtinMountArchiveSha256: String = "",
    val builtinMountLkmCount: Int = 0,
    val builtinMountSupportedKmis: List<String> = emptyList(),
    val builtinMountCurrentKmi: String = "",
    val builtinMountCompatibility: String = "unknown",
    val builtinMountLkmPurpose: String = "",
    val builtinMountIsApkeSuRootDriver: Boolean = false,

    // Built-in KPatch Next
    val isKPatchNextInstalled: Boolean = false,
    val isKPatchNextEnabled: Boolean = false,
    val isKPatchNextPendingUpdate: Boolean = false,
    val isKPatchNextPendingRemove: Boolean = false,
    val isKPatchNextWebUiAvailable: Boolean = false,
    val isKPatchNextUnresolved: Boolean = false,
    val kPatchNextVersion: String = "",
    val kPatchNextConflict: String? = null,

    // ApkeSU Hide
    val isEpkesuHideEnabled: Boolean = false,

    // ADB Root
    val adbRootStatus: String = "",
    val isAdbRootEnabled: Boolean = false,

    // AVC Spoof
    val avcSpoofStatus: String = "",
    val isAvcSpoofEnabled: Boolean = false,

    val isLkmMode: Boolean = false,
    val isLateLoadMode: Boolean = false,
    val runtimeModeResolved: Boolean = false,

    // Auto Jailbreak
    val autoJailbreak: Boolean = false
)

@Immutable
data class SettingsScreenActions(
    val onSetCheckModuleUpdate: (Boolean) -> Unit,
    val onSetShowVersionMismatchWarning: (Boolean) -> Unit,
    val onSetShowGkiWarning: (Boolean) -> Unit,
    val onSetShowHomeSupportCard: (Boolean) -> Unit,
    val onSetShowHomeLearnCard: (Boolean) -> Unit,
    val onSetGraphicsRendererFeatureEnabled: (Boolean) -> Unit,
    val onOpenTheme: () -> Unit,
    val onOpenThemeStore: () -> Unit,
    val onSetDayNightMode: (Boolean) -> Unit,
    val onSetSwitchStyleIndex: (Int) -> Unit,
    val onSetSeasonStyleIndex: (Int) -> Unit,
    val onSetSeasonCardMotionEnabled: (Boolean) -> Unit,
    val onSetRainStyleIndex: (Int) -> Unit,
    val onSetRainCardMotionEnabled: (Boolean) -> Unit,
    val onSetPixelStyleIndex: (Int) -> Unit,
    val onSetPixelCardMotionEnabled: (Boolean) -> Unit,
    val onSetGlobalSnowEnabled: (Boolean) -> Unit,
    val onSetGlobalSnowEffectIndex: (Int) -> Unit,
    val onSetNightBackgroundEffectIndex: (Int) -> Unit,
    val onSetNightBackgroundPassthrough: (Boolean) -> Unit,
    val onSetNightBackgroundPassthroughOpacity: (Float) -> Unit,
    val onSetGlobalScrollEffectEnabled: (Boolean) -> Unit,
    val onSetGlobalScrollEffectIndex: (Int) -> Unit,
    val onSetUiModeIndex: (Int) -> Unit,
    val onSetAlphaDeltaMode: (Boolean) -> Unit,
    val onOpenLauncherIcon: () -> Unit,
    val onEditHomeTitle: () -> Unit,
    val onOpenNavigationIcons: () -> Unit,
    val onOpenHomeCardWallpapers: () -> Unit,
    val onOpenVisualEffects: () -> Unit,
    val onOpenUiDecorationLibrary: () -> Unit,
    val onOpenBackgrounds: () -> Unit,
    val onOpenSoundEffects: () -> Unit,
    val onPickWallpaper: () -> Unit,
    val onPreviewWallpaper: () -> Unit,
    val onEditWallpaperCrop: () -> Unit,
    val onClearWallpaper: () -> Unit,
    val onSetWallpaperOpacity: (Float) -> Unit,
    val onSetWallpaperCrop: (CustomWallpaperCrop) -> Unit,
    val onSetWallpaperPassthroughEnabled: (Boolean) -> Unit,
    val onSetWallpaperPassthroughOpacity: (Float) -> Unit,
    val onPickVideoBackground: () -> Unit,
    val onPreviewVideoBackground: () -> Unit,
    val onClearVideoBackground: () -> Unit,
    val onSetVideoBackgroundDurationSeconds: (Int) -> Unit,
    val onSetPageBackgroundWallpaper: (CustomPageBackgroundTarget, String?) -> Unit,
    val onSetPageBackgroundVideo: (CustomPageBackgroundTarget, String?) -> Unit,
    val onSetPageBackgroundOpacity: (CustomPageBackgroundTarget, Float) -> Unit,
    val onSetPageBackgroundCrop: (CustomPageBackgroundTarget, CustomWallpaperCrop) -> Unit,
    val onSetPageBackgroundVideoDurationSeconds: (CustomPageBackgroundTarget, Int) -> Unit,
    val onClearPageBackground: (CustomPageBackgroundTarget) -> Unit,
    val onSaveCustomThemePreset: (String) -> Unit,
    val onApplyCustomThemePreset: (String) -> Unit,
    val onRenameCustomThemePreset: (String, String) -> Unit,
    val onDeleteCustomThemePreset: (String) -> Unit,
    val onSetThemeSyncStrategy: (ThemeSyncStrategy) -> Unit,
    val onResetThemeToDefault: () -> Unit,
    val onOpenStartupAnimation: () -> Unit,
    val onOpenProfileTemplate: () -> Unit,
    val onSetSuCompatMode: (Int) -> Unit,
    val onSetKernelUmountEnabled: (Boolean) -> Unit,
    val onSetSelinuxHideEnabled: (Boolean) -> Unit,
    val onSetSulogEnabled: (Boolean) -> Unit,
    val onSetAdbRootEnabled: (Boolean) -> Unit,
    val onSetAvcSpoofEnabled: (Boolean) -> Unit,
    val onSetDefaultUmountModules: (Boolean) -> Unit,
    val onOpenBuiltinMount: () -> Unit,
    val onSetKPatchNextEnabled: (Boolean) -> Unit,
    val onOpenKPatchNextWebUi: () -> Unit,
    val onOpenHiddenPathConfig: () -> Unit,
    val onOpenAiChat: () -> Unit,
    val onOpenRescueProtection: () -> Unit,
    val onOpenCpuSpoof: () -> Unit,
    val onOpenGraphicsRenderer: () -> Unit,
    val onSetEpkesuHideEnabled: (Boolean) -> Unit,
    val onSetEnableWebDebugging: (Boolean) -> Unit,
    val onSetAutoJailbreak: (Boolean) -> Unit,
    val onSetDeltaColorVariant: (String) -> Unit,
    val onOpenAbout: () -> Unit,
)
