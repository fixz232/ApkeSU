package me.weishu.kernelsu.data.repository

import me.weishu.kernelsu.ui.theme.ThemePreset
import me.weishu.kernelsu.ui.theme.CustomThemePreset
import me.weishu.kernelsu.ui.theme.ThemeSyncStrategy
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.decoration.UiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.CustomUiDecorationPreset
import me.weishu.kernelsu.ui.util.BuiltinMountStatus
import me.weishu.kernelsu.ui.util.CustomNavigationIconSet
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.CustomPageBackgroundSet
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.KPatchNextStatus

const val SHOW_VERSION_MISMATCH_WARNING_KEY = "show_version_mismatch_warning"
const val SHOW_GKI_WARNING_KEY = "show_gki_warning"
const val SHOW_HOME_SUPPORT_CARD_KEY = "show_home_support_card"
const val SHOW_HOME_LEARN_CARD_KEY = "show_home_learn_card"
const val GRAPHICS_RENDERER_FEATURE_ENABLED_KEY = "graphics_renderer_feature_enabled"

interface SettingsRepository {
    var uiMode: String
    var checkModuleUpdate: Boolean
    var showVersionMismatchWarning: Boolean
    var showGkiWarning: Boolean
    var showHomeSupportCard: Boolean
    var showHomeLearnCard: Boolean
    var graphicsRendererFeatureEnabled: Boolean
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var themePreset: String
    var enablePredictiveBack: Boolean
    var enableBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    var autoHideNavigationBar: Boolean
    var scrollHideNavigationBar: Boolean
    var pageScale: Float
    var fontScale: Float
    var blurIntensity: Float
    var switchStyle: String
    var seasonStyle: String
    var pixelStyle: String
    val uiDecorationConfig: UiDecorationConfig
    fun saveUiDecorationConfig(config: UiDecorationConfig): Boolean
    fun getCustomUiDecorationPresets(): List<CustomUiDecorationPreset>
    fun saveCustomUiDecorationPreset(name: String, config: UiDecorationConfig): CustomUiDecorationPreset?
    fun renameCustomUiDecorationPreset(presetId: String, name: String): Boolean
    fun deleteCustomUiDecorationPreset(presetId: String): Boolean
    fun importCustomUiDecorationPresets(presets: List<CustomUiDecorationPreset>): Int
    fun getRecentUiDecorationComponents(): List<String>
    var globalSnowEnabled: Boolean
    var globalSnowEffect: String
    var nightBackgroundEffect: String
    var nightBackgroundPassthrough: Boolean
    var nightBackgroundPassthroughOpacity: Float
    var globalScrollEffectEnabled: Boolean
    var globalScrollEffect: String
    var themeSyncStrategy: ThemeSyncStrategy
    var enableWebDebugging: Boolean
    var autoJailbreak: Boolean
    var launcherIcon: String
    var customManagerName: String
    var customWallpaperUri: String?
    var customWallpaperOpacity: Float
    var customWallpaperCrop: CustomWallpaperCrop
    var customWallpaperPassthroughEnabled: Boolean
    var customWallpaperPassthroughOpacity: Float
    var customVideoBackgroundUri: String?
    var customVideoBackgroundDurationSeconds: Int
    val customPageBackgrounds: CustomPageBackgroundSet
    var customStartupAnimationUri: String?
    var customStartupSoundUri: String?
    var customStartupSoundDurationSeconds: Int
    var customStartupSoundVolume: Float
    var customClickSoundUri: String?
    var customClickSoundVolume: Float
    var customBackgroundMusicUri: String?
    var customBackgroundMusicVolume: Float
    var deltaColorVariant: String
    val customNavigationIcons: CustomNavigationIconSet
    fun setCustomPageBackgroundWallpaper(target: CustomPageBackgroundTarget, uriString: String?)
    fun setCustomPageBackgroundVideo(target: CustomPageBackgroundTarget, uriString: String?)
    fun setCustomPageBackgroundOpacity(target: CustomPageBackgroundTarget, opacity: Float)
    fun setCustomPageBackgroundCrop(target: CustomPageBackgroundTarget, crop: CustomWallpaperCrop)
    fun setCustomPageBackgroundVideoDurationSeconds(target: CustomPageBackgroundTarget, seconds: Int)
    fun clearCustomPageBackground(target: CustomPageBackgroundTarget)
    fun setCustomNavigationIcon(slot: CustomNavigationIconSlot, uriString: String?)
    fun setCustomNavigationIconCrop(slot: CustomNavigationIconSlot, crop: CustomWallpaperCrop)

    suspend fun getSuCompatStatus(): String
    suspend fun getSuCompatPersistValue(): Long?
    fun isSuEnabled(): Boolean
    fun setSuEnabled(enabled: Boolean): Boolean
    fun setSuCompatModePref(mode: Int)
    fun getSuCompatModePref(): Int

    suspend fun getKernelUmountStatus(): String
    fun isKernelUmountEnabled(): Boolean
    fun setKernelUmountEnabled(enabled: Boolean): Boolean

    suspend fun getSelinuxHideStatus(): String
    fun isSelinuxHideEnabled(): Boolean
    fun setSelinuxHideEnabled(enabled: Boolean): Int

    suspend fun getSulogStatus(): String
    suspend fun getSulogPersistValue(): Long?
    fun setSulogEnabled(enabled: Boolean): Boolean

    suspend fun getAdbRootStatus(): String
    suspend fun getAdbRootPersistValue(): Long?
    fun setAdbRootEnabled(enabled: Boolean): Boolean

    suspend fun getAvcSpoofStatus(): String
    fun isAvcSpoofEnabled(): Boolean
    fun setAvcSpoofEnabled(enabled: Boolean): Boolean

    fun isDefaultUmountModules(): Boolean
    fun setDefaultUmountModules(enabled: Boolean): Boolean

    suspend fun getBuiltinMountStatus(): BuiltinMountStatus
    fun setBuiltinMountEnabled(enabled: Boolean): Boolean
    fun setBuiltinMountDefaultMode(mode: String): Boolean
    fun setBuiltinMountVariant(variant: String): Boolean

    suspend fun getKPatchNextStatus(): KPatchNextStatus
    fun setKPatchNextEnabled(enabled: Boolean): Boolean

    suspend fun getEpkesuHideStatus(): Boolean
    fun setEpkesuHideEnabled(enabled: Boolean): Boolean

    fun isLkmMode(): Boolean

    fun applyThemePreset(preset: ThemePreset)
    fun saveCustomThemePreset(name: String): CustomThemePreset?
    fun applyCustomThemePreset(presetId: String): Boolean
    fun renameCustomThemePreset(presetId: String, name: String): Boolean
    fun deleteCustomThemePreset(presetId: String): Boolean
    fun getCustomThemePresets(): List<CustomThemePreset>
    fun resetThemeToDefault()

    fun execKsudFeatureSave()
}
