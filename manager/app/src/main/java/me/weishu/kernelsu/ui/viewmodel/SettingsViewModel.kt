package me.weishu.kernelsu.ui.viewmodel

import android.system.OsConstants
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.repository.SettingsRepository
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.resolveRealtimeBlurEnabled
import me.weishu.kernelsu.ui.component.GlobalScrollEffect
import me.weishu.kernelsu.ui.component.PageTransitionEffect
import me.weishu.kernelsu.ui.component.GlobalSnowEffect
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.decoration.UiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.CustomUiDecorationPreset
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.rain.RainStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.ink.InkStyle
import me.weishu.kernelsu.ui.screen.settings.SettingsUiState
import me.weishu.kernelsu.ui.screen.settings.UiDecorationSaveState
import me.weishu.kernelsu.ui.theme.ColorMode
import me.weishu.kernelsu.ui.theme.DeltaColorVariant
import me.weishu.kernelsu.ui.theme.ThemeAppearanceDefaults
import me.weishu.kernelsu.ui.theme.ThemePreset
import me.weishu.kernelsu.ui.theme.ThemeSyncStrategy
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.BUILTIN_MOUNT_MODE_MAGIC
import me.weishu.kernelsu.ui.util.BUILTIN_MOUNT_MODE_OVERLAY
import me.weishu.kernelsu.ui.util.BUILTIN_MOUNT_VARIANT_FULL
import me.weishu.kernelsu.ui.util.BUILTIN_MOUNT_VARIANT_LITE
import me.weishu.kernelsu.ui.util.LauncherIconOption

class SettingsViewModel(
    private val repo: SettingsRepository = SettingsRepositoryImpl()
) : ViewModel() {

    private val refreshExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "refresh settings failed", throwable)
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null
    private var uiDecorationSaveJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(refreshExceptionHandler) {
            val checkModuleUpdate = repo.checkModuleUpdate
            val showVersionMismatchWarning = repo.showVersionMismatchWarning
            val showGkiWarning = repo.showGkiWarning
            val showHomeSupportCard = repo.showHomeSupportCard
            val showHomeLearnCard = repo.showHomeLearnCard
            val miuixClassicHomeLayoutEnabled = repo.miuixClassicHomeLayoutEnabled
            val graphicsRendererFeatureEnabled = repo.graphicsRendererFeatureEnabled
            val themeMode = repo.themeMode
            val miuixMonet = repo.miuixMonet
            val keyColor = repo.keyColor
            val enablePredictiveBack = repo.enablePredictiveBack
            val uiMode = repo.uiMode
            val enableBlur = resolveRealtimeBlurEnabled(uiMode, repo.enableBlur)
            val enableFloatingBottomBar = repo.enableFloatingBottomBar
            val enableFloatingBottomBarBlur = resolveRealtimeBlurEnabled(
                uiMode,
                repo.enableFloatingBottomBarBlur,
            )
            val autoHideNavigationBar = repo.autoHideNavigationBar
            val scrollHideNavigationBar = repo.scrollHideNavigationBar
            val moduleTopBarAutoHideEnabled = repo.moduleTopBarAutoHideEnabled
            val pageScale = repo.pageScale
            val fontScale = repo.fontScale
            val blurIntensity = repo.blurIntensity
            val switchStyle = repo.switchStyle
            val seasonStyle = repo.seasonStyle
            val seasonCardMotionEnabled = repo.seasonCardMotionEnabled
            val rainStyle = repo.rainStyle
            val rainCardMotionEnabled = repo.rainCardMotionEnabled
            val inkStyle = repo.inkStyle
            val inkFontEnabled = repo.inkFontEnabled
            val inkCardMotionEnabled = repo.inkCardMotionEnabled
            val pixelStyle = repo.pixelStyle
            val pixelCardMotionEnabled = repo.pixelCardMotionEnabled
            val uiDecorationConfig = repo.uiDecorationConfig
            val customUiDecorationPresets = repo.getCustomUiDecorationPresets()
            val recentUiDecorationComponents = repo.getRecentUiDecorationComponents()
            val globalSnowEnabled = repo.globalSnowEnabled
            val globalSnowEffect = repo.globalSnowEffect
            val nightBackgroundEffect = repo.nightBackgroundEffect
            val nightBackgroundPassthrough = repo.nightBackgroundPassthrough
            val nightBackgroundPassthroughOpacity = repo.nightBackgroundPassthroughOpacity
            val globalScrollEffectEnabled = repo.globalScrollEffectEnabled
            val globalScrollEffect = repo.globalScrollEffect
            val backgroundScrollFollowEnabled = repo.backgroundScrollFollowEnabled
            val pageTransitionEffect = repo.pageTransitionEffect
            val themeSyncStrategy = repo.themeSyncStrategy
            val customThemePresets = repo.getCustomThemePresets()
            val enableWebDebugging = repo.enableWebDebugging
            val launcherIcon = repo.launcherIcon
            val customManagerName = repo.customManagerName
            val customHomeTitle = repo.customHomeTitle
            val customWallpaperUri = repo.customWallpaperUri
            val customWallpaperOpacity = repo.customWallpaperOpacity
            val customWallpaperVisualSettings = repo.customWallpaperVisualSettings
            val customWallpaperCrop = repo.customWallpaperCrop
            val customWallpaperPassthroughEnabled = repo.customWallpaperPassthroughEnabled
            val customWallpaperPassthroughOpacity = repo.customWallpaperPassthroughOpacity
            val customVideoBackgroundUri = repo.customVideoBackgroundUri
            val customVideoBackgroundDurationSeconds = repo.customVideoBackgroundDurationSeconds
            val customVideoBackgroundFrameRate = repo.customVideoBackgroundFrameRate
            val customPageBackgrounds = repo.customPageBackgrounds
            val customStartupAnimationUri = repo.customStartupAnimationUri
            val startupAnimationSettings = repo.startupAnimationSettings
            val customStartupSoundUri = repo.customStartupSoundUri
            val customStartupSoundDurationSeconds = repo.customStartupSoundDurationSeconds
            val customStartupSoundVolume = repo.customStartupSoundVolume
            val customClickSoundUri = repo.customClickSoundUri
            val customClickSoundVolume = repo.customClickSoundVolume
            val customBackgroundMusicUri = repo.customBackgroundMusicUri
            val customBackgroundMusicVolume = repo.customBackgroundMusicVolume
            val customNavigationIcons = repo.customNavigationIcons
            val deltaColorVariant = repo.deltaColorVariant
            val colorStyle = repo.colorStyle
            val colorSpec = repo.colorSpec
            val monetSurfaceOpacity = repo.monetSurfaceOpacity
            val themePreset = resolveThemePreset(
                repo.themePreset,
                uiMode = uiMode,
                themeMode = themeMode,
                miuixMonet = miuixMonet,
                keyColor = keyColor,
                colorStyle = colorStyle,
                colorSpec = colorSpec,
                monetSurfaceOpacity = monetSurfaceOpacity,
                enableBlur = enableBlur,
                enableFloatingBottomBar = enableFloatingBottomBar,
                enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
                pageScale = pageScale,
                fontScale = fontScale,
                blurIntensity = blurIntensity,
            )
            val isLkmMode = repo.isLkmMode()
            val runtimeModeResolved = Natives.version > 0

            // Async loading for natives/features
            val suCompatStatus = repo.getSuCompatStatus()
            val suCompatPersistValue = repo.getSuCompatPersistValue()
            val isSuEnabled = repo.isSuEnabled()

            val suCompatMode = if (suCompatPersistValue == 0L) 2 else if (!isSuEnabled) 1 else 0

            val kernelUmountStatus = repo.getKernelUmountStatus()
            val isKernelUmountEnabled = repo.isKernelUmountEnabled()
            val selinuxHideStatus = repo.getSelinuxHideStatus()
            val isSelinuxHideEnabled = repo.isSelinuxHideEnabled()
            val sulogStatus = repo.getSulogStatus()
            val isSulogEnabled = repo.getSulogPersistValue() == 1L
            val adbRootStatus = repo.getAdbRootStatus()
            val isAdbRootEnabled = repo.getAdbRootPersistValue() == 1L
            val avcSpoofStatus = repo.getAvcSpoofStatus()
            val isAvcSpoofEnabled = repo.isAvcSpoofEnabled()
            val isDefaultUmountModules = repo.isDefaultUmountModules()
            val builtinMountStatus = repo.getBuiltinMountStatus()
            val kPatchNextStatus = repo.getKPatchNextStatus()
            val isEpkesuHideEnabled = repo.getEpkesuHideStatus()
            val autoJailbreak = repo.autoJailbreak
            val isLateLoadMode = Natives.isLateLoadMode

            _uiState.update {
                it.copy(
                    uiMode = uiMode,
                    checkModuleUpdate = checkModuleUpdate,
                    showVersionMismatchWarning = showVersionMismatchWarning,
                    showGkiWarning = showGkiWarning,
                    showHomeSupportCard = showHomeSupportCard,
                    showHomeLearnCard = showHomeLearnCard,
                    miuixClassicHomeLayoutEnabled = miuixClassicHomeLayoutEnabled,
                    graphicsRendererFeatureEnabled = graphicsRendererFeatureEnabled,
                    themeMode = themeMode,
                    miuixMonet = miuixMonet,
                    keyColor = keyColor,
                    themePreset = themePreset.value,
                    enablePredictiveBack = enablePredictiveBack,
                    enableBlur = enableBlur,
                    enableFloatingBottomBar = enableFloatingBottomBar,
                    enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
                    autoHideNavigationBar = autoHideNavigationBar,
                    scrollHideNavigationBar = scrollHideNavigationBar,
                    moduleTopBarAutoHideEnabled = moduleTopBarAutoHideEnabled,
                    pageScale = pageScale,
                    fontScale = fontScale,
                    blurIntensity = blurIntensity,
                    switchStyle = switchStyle,
                    seasonStyle = seasonStyle,
                    seasonCardMotionEnabled = seasonCardMotionEnabled,
                    rainStyle = rainStyle,
                    rainCardMotionEnabled = rainCardMotionEnabled,
                    inkStyle = inkStyle,
                    inkFontEnabled = inkFontEnabled,
                    inkCardMotionEnabled = inkCardMotionEnabled,
                    pixelStyle = pixelStyle,
                    pixelCardMotionEnabled = pixelCardMotionEnabled,
                    uiDecorationConfig = uiDecorationConfig,
                    customUiDecorationPresets = customUiDecorationPresets,
                    recentUiDecorationComponents = recentUiDecorationComponents,
                    globalSnowEnabled = globalSnowEnabled,
                    globalSnowEffect = globalSnowEffect,
                    nightBackgroundEffect = nightBackgroundEffect,
                    nightBackgroundPassthrough = nightBackgroundPassthrough,
                    nightBackgroundPassthroughOpacity = nightBackgroundPassthroughOpacity,
                    globalScrollEffectEnabled = globalScrollEffectEnabled,
                    globalScrollEffect = globalScrollEffect,
                    backgroundScrollFollowEnabled = backgroundScrollFollowEnabled,
                    pageTransitionEffect = pageTransitionEffect,
                    themeSyncStrategy = themeSyncStrategy,
                    customThemePresets = customThemePresets,
                    enableWebDebugging = enableWebDebugging,
                    launcherIcon = launcherIcon,
                    customManagerName = customManagerName,
                    customHomeTitle = customHomeTitle,
                    customWallpaperUri = customWallpaperUri,
                    customWallpaperOpacity = customWallpaperOpacity,
                    customWallpaperVisualSettings = customWallpaperVisualSettings,
                    customWallpaperCrop = customWallpaperCrop,
                    customWallpaperPassthroughEnabled = customWallpaperPassthroughEnabled,
                    customWallpaperPassthroughOpacity = customWallpaperPassthroughOpacity,
                    customVideoBackgroundUri = customVideoBackgroundUri,
                    customVideoBackgroundDurationSeconds = customVideoBackgroundDurationSeconds,
                    customVideoBackgroundFrameRate = customVideoBackgroundFrameRate,
                    customPageBackgrounds = customPageBackgrounds,
                    customStartupAnimationUri = customStartupAnimationUri,
                    startupAnimationSettings = startupAnimationSettings,
                    customStartupSoundUri = customStartupSoundUri,
                    customStartupSoundDurationSeconds = customStartupSoundDurationSeconds,
                    customStartupSoundVolume = customStartupSoundVolume,
                    customClickSoundUri = customClickSoundUri,
                    customClickSoundVolume = customClickSoundVolume,
                    customBackgroundMusicUri = customBackgroundMusicUri,
                    customBackgroundMusicVolume = customBackgroundMusicVolume,
                    customNavigationIcons = customNavigationIcons,
                    deltaColorVariant = deltaColorVariant,
                    colorStyle = colorStyle,
                    colorSpec = colorSpec,
                    monetSurfaceOpacity = monetSurfaceOpacity,
                    suCompatStatus = suCompatStatus,
                    suCompatMode = suCompatMode,
                    isSuEnabled = isSuEnabled,
                    adbRootStatus = adbRootStatus,
                    isAdbRootEnabled = isAdbRootEnabled,
                    kernelUmountStatus = kernelUmountStatus,
                    isKernelUmountEnabled = isKernelUmountEnabled,
                    selinuxHideStatus = selinuxHideStatus,
                    isSelinuxHideEnabled = isSelinuxHideEnabled,
                    sulogStatus = sulogStatus,
                    isSulogEnabled = isSulogEnabled,
                    avcSpoofStatus = avcSpoofStatus,
                    isAvcSpoofEnabled = isAvcSpoofEnabled,
                    isDefaultUmountModules = isDefaultUmountModules,
                    isBuiltinMountEnabled = builtinMountStatus.enabled,
                    builtinMountDefaultMode = builtinMountStatus.defaultMode,
                    builtinMountVariant = builtinMountStatus.variant,
                    isBuiltinMountWebUiAvailable = builtinMountStatus.webUi,
                    builtinMountConflict = builtinMountStatus.conflict,
                    builtinMountSourceUrl = builtinMountStatus.sourceUrl,
                    builtinMountArchiveSha256 = builtinMountStatus.archiveSha256,
                    builtinMountLkmCount = builtinMountStatus.lkmCount,
                    builtinMountSupportedKmis = builtinMountStatus.supportedKmis,
                    builtinMountCurrentKmi = builtinMountStatus.currentKmi,
                    builtinMountCompatibility = builtinMountStatus.compatibility,
                    builtinMountLkmPurpose = builtinMountStatus.lkmPurpose,
                    builtinMountIsApkeSuRootDriver = builtinMountStatus.apkeSuRootDriver,
                    isKPatchNextInstalled = kPatchNextStatus.installed,
                    isKPatchNextEnabled = kPatchNextStatus.enabled,
                    isKPatchNextPendingUpdate = kPatchNextStatus.pendingUpdate,
                    isKPatchNextPendingRemove = kPatchNextStatus.pendingRemove,
                    isKPatchNextWebUiAvailable = kPatchNextStatus.webUi,
                    isKPatchNextUnresolved = kPatchNextStatus.unresolved,
                    kPatchNextVersion = kPatchNextStatus.version,
                    kPatchNextConflict = kPatchNextStatus.conflict,
                    isEpkesuHideEnabled = isEpkesuHideEnabled,
                    isLkmMode = isLkmMode,
                    autoJailbreak = autoJailbreak,
                    isLateLoadMode = isLateLoadMode,
                    runtimeModeResolved = runtimeModeResolved,
                )
            }
        }
    }

    fun setUiMode(mode: String) {
        val normalizedMode = InterfaceStyle.normalizeValue(mode)
        if (repo.themeSyncStrategy == ThemeSyncStrategy.PER_STYLE) {
            repo.uiMode = normalizedMode
            refresh()
            return
        }

        when (normalizedMode) {
            InterfaceStyle.Skrootpro.value -> {
                applyInterfacePresetPreservingColorMode(normalizedMode, ThemePreset.SKROOTPRO)
                return
            }

            InterfaceStyle.Alpha.value -> {
                applyInterfacePresetPreservingColorMode(normalizedMode, ThemePreset.ALPHA)
                return
            }

            InterfaceStyle.Delta.value -> {
                applyInterfacePresetPreservingColorMode(normalizedMode, ThemePreset.DELTA)
                return
            }

            InterfaceStyle.LiquidGlass.value -> {
                applyInterfacePresetPreservingColorMode(normalizedMode, ThemePreset.LIQUID_GLASS)
                return
            }

            InterfaceStyle.Snow.value -> {
                applyInterfacePresetPreservingColorMode(normalizedMode, ThemePreset.SNOW)
                return
            }

            InterfaceStyle.Rain.value -> {
                applyInterfacePresetPreservingColorMode(normalizedMode, ThemePreset.RAIN)
                return
            }

            InterfaceStyle.Ink.value -> {
                applyInterfacePresetPreservingColorMode(normalizedMode, ThemePreset.INK)
                return
            }

            InterfaceStyle.Pixel.value -> {
                applyInterfacePresetPreservingColorMode(normalizedMode, ThemePreset.PIXEL)
                return
            }
        }

        val oldMode = repo.uiMode
        val currentThemeMode = repo.themeMode
        val isLeavingSpecialStyle = oldMode == InterfaceStyle.Skrootpro.value ||
            oldMode == InterfaceStyle.Alpha.value ||
            oldMode == InterfaceStyle.Delta.value ||
            oldMode == InterfaceStyle.LiquidGlass.value ||
            oldMode == InterfaceStyle.Snow.value ||
            oldMode == InterfaceStyle.Rain.value ||
            oldMode == InterfaceStyle.Ink.value ||
            oldMode == InterfaceStyle.Pixel.value

        if (isLeavingSpecialStyle && normalizedMode == InterfaceStyle.Miuix.value) {
            applyInterfacePresetPreservingColorMode(normalizedMode, ThemePreset.CLEAN_TOOL)
            return
        }

        repo.uiMode = normalizedMode
        repo.themeMode = currentThemeMode
        _uiState.update {
            it.copy(
                uiMode = normalizedMode,
                themeMode = currentThemeMode,
                themePreset = ThemePreset.CUSTOM.value
            )
        }
    }

    private fun applyInterfacePresetPreservingColorMode(mode: String, preset: ThemePreset) {
        val colorMode = repo.themeMode
        val selectedSeason = if (mode == InterfaceStyle.Snow.value) {
            SeasonStyle.fromValue(repo.seasonStyle)
        } else {
            null
        }
        val selectedRainStyle = if (mode == InterfaceStyle.Rain.value) {
            RainStyle.fromValue(repo.rainStyle)
        } else {
            null
        }
        val selectedInkStyle = if (mode == InterfaceStyle.Ink.value) {
            InkStyle.fromValue(repo.inkStyle)
        } else {
            null
        }
        val selectedPixelStyle = if (mode == InterfaceStyle.Pixel.value) {
            PixelStyle.fromValue(repo.pixelStyle)
        } else {
            null
        }
        repo.uiMode = mode
        repo.applyThemePreset(preset)
        repo.themeMode = colorMode
        selectedSeason?.let { repo.seasonStyle = it.value }
        selectedRainStyle?.let { repo.rainStyle = it.value }
        selectedInkStyle?.let { repo.inkStyle = it.value }
        selectedPixelStyle?.let { repo.pixelStyle = it.value }
        refresh()
    }

    fun setCheckModuleUpdate(enabled: Boolean) {
        repo.checkModuleUpdate = enabled
        _uiState.update { it.copy(checkModuleUpdate = enabled) }
    }

    fun setShowVersionMismatchWarning(enabled: Boolean) {
        repo.showVersionMismatchWarning = enabled
        _uiState.update { it.copy(showVersionMismatchWarning = enabled) }
    }

    fun setShowGkiWarning(enabled: Boolean) {
        repo.showGkiWarning = enabled
        _uiState.update { it.copy(showGkiWarning = enabled) }
    }

    fun setShowHomeSupportCard(enabled: Boolean) {
        repo.showHomeSupportCard = enabled
        _uiState.update { it.copy(showHomeSupportCard = enabled) }
    }

    fun setShowHomeLearnCard(enabled: Boolean) {
        repo.showHomeLearnCard = enabled
        _uiState.update { it.copy(showHomeLearnCard = enabled) }
    }

    fun setMiuixClassicHomeLayoutEnabled(enabled: Boolean) {
        repo.miuixClassicHomeLayoutEnabled = enabled
        _uiState.update { it.copy(miuixClassicHomeLayoutEnabled = enabled) }
    }

    fun setGraphicsRendererFeatureEnabled(enabled: Boolean) {
        repo.graphicsRendererFeatureEnabled = enabled
        _uiState.update { it.copy(graphicsRendererFeatureEnabled = enabled) }
    }

    fun setGlobalSnowEnabled(enabled: Boolean) {
        repo.globalSnowEnabled = enabled
        _uiState.update { it.copy(globalSnowEnabled = enabled) }
    }

    fun setUiDecorationConfig(config: UiDecorationConfig) {
        val normalized = config.normalized()
        if (uiDecorationSaveJob?.isActive == true) return
        _uiState.update { it.copy(uiDecorationSaveState = UiDecorationSaveState.Saving) }
        uiDecorationSaveJob = viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    repo.saveUiDecorationConfig(normalized) && repo.uiDecorationConfig == normalized
                }.getOrDefault(false)
            }
            _uiState.update { current ->
                if (saved) {
                    current.copy(
                        uiDecorationConfig = normalized,
                        uiDecorationSaveState = UiDecorationSaveState.Saved,
                        recentUiDecorationComponents = repo.getRecentUiDecorationComponents(),
                    )
                } else {
                    current.copy(uiDecorationSaveState = UiDecorationSaveState.Failed)
                }
            }
        }
    }

    fun consumeUiDecorationSaveState() {
        _uiState.update { current ->
            if (current.uiDecorationSaveState == UiDecorationSaveState.Saving) {
                current
            } else {
                current.copy(uiDecorationSaveState = UiDecorationSaveState.Idle)
            }
        }
    }

    fun saveCustomUiDecorationPreset(name: String, config: UiDecorationConfig): Boolean {
        repo.saveCustomUiDecorationPreset(name, config) ?: return false
        _uiState.update { current -> current.copy(customUiDecorationPresets = repo.getCustomUiDecorationPresets()) }
        return true
    }

    fun renameCustomUiDecorationPreset(presetId: String, name: String): Boolean {
        val saved = repo.renameCustomUiDecorationPreset(presetId, name)
        if (saved) {
            _uiState.update { it.copy(customUiDecorationPresets = repo.getCustomUiDecorationPresets()) }
        }
        return saved
    }

    fun deleteCustomUiDecorationPreset(presetId: String): Boolean {
        val deleted = repo.deleteCustomUiDecorationPreset(presetId)
        if (deleted) {
            _uiState.update { it.copy(customUiDecorationPresets = repo.getCustomUiDecorationPresets()) }
        }
        return deleted
    }

    suspend fun importCustomUiDecorationPresets(presets: List<CustomUiDecorationPreset>): Int {
        val count = withContext(Dispatchers.IO) { repo.importCustomUiDecorationPresets(presets) }
        if (count > 0) {
            _uiState.update { it.copy(customUiDecorationPresets = repo.getCustomUiDecorationPresets()) }
        }
        return count
    }

    fun setSeasonStyleIndex(index: Int) {
        val season = SeasonStyle.fromIndex(index)
        repo.seasonStyle = season.value
        _uiState.update {
            it.copy(
                seasonStyle = season.value,
                keyColor = season.keyColor,
                themePreset = ThemePreset.SNOW.value,
            )
        }
    }

    fun setSeasonCardMotionEnabled(enabled: Boolean) {
        repo.seasonCardMotionEnabled = enabled
        _uiState.update { it.copy(seasonCardMotionEnabled = enabled) }
    }

    fun setRainStyleIndex(index: Int) {
        val rainStyle = RainStyle.fromIndex(index)
        repo.rainStyle = rainStyle.value
        _uiState.update {
            it.copy(
                rainStyle = rainStyle.value,
                keyColor = rainStyle.keyColor,
                themePreset = ThemePreset.RAIN.value,
            )
        }
    }

    fun setRainCardMotionEnabled(enabled: Boolean) {
        repo.rainCardMotionEnabled = enabled
        _uiState.update { it.copy(rainCardMotionEnabled = enabled) }
    }

    fun setInkStyleIndex(index: Int) {
        val inkStyle = InkStyle.fromIndex(index)
        repo.inkStyle = inkStyle.value
        _uiState.update {
            it.copy(
                inkStyle = inkStyle.value,
                keyColor = inkStyle.keyColor,
                themePreset = ThemePreset.INK.value,
            )
        }
    }

    fun setInkFontEnabled(enabled: Boolean) {
        repo.inkFontEnabled = enabled
        _uiState.update { it.copy(inkFontEnabled = enabled) }
    }

    fun setInkCardMotionEnabled(enabled: Boolean) {
        repo.inkCardMotionEnabled = enabled
        _uiState.update { it.copy(inkCardMotionEnabled = enabled) }
    }

    fun setPixelStyleIndex(index: Int) {
        val pixelStyle = PixelStyle.fromIndex(index)
        repo.pixelStyle = pixelStyle.value
        _uiState.update {
            it.copy(
                pixelStyle = pixelStyle.value,
                keyColor = pixelStyle.keyColor,
                themePreset = ThemePreset.PIXEL.value,
            )
        }
    }

    fun setPixelCardMotionEnabled(enabled: Boolean) {
        repo.pixelCardMotionEnabled = enabled
        _uiState.update { it.copy(pixelCardMotionEnabled = enabled) }
    }

    fun setGlobalSnowEffectIndex(index: Int) {
        val effect = GlobalSnowEffect.fromIndex(index)
        repo.globalSnowEffect = effect.value
        _uiState.update { it.copy(globalSnowEffect = effect.value) }
    }

    fun setNightBackgroundEffectIndex(index: Int) {
        val effect = NightBackgroundEffect.fromIndex(index)
        repo.nightBackgroundEffect = effect.value
        _uiState.update { it.copy(nightBackgroundEffect = effect.value) }
    }

    fun setNightBackgroundPassthrough(enabled: Boolean) {
        repo.nightBackgroundPassthrough = enabled
        _uiState.update { it.copy(nightBackgroundPassthrough = enabled) }
    }

    fun setNightBackgroundPassthroughOpacity(opacity: Float) {
        repo.nightBackgroundPassthroughOpacity = opacity
        _uiState.update { it.copy(nightBackgroundPassthroughOpacity = repo.nightBackgroundPassthroughOpacity) }
    }

    fun setGlobalScrollEffectEnabled(enabled: Boolean) {
        repo.globalScrollEffectEnabled = enabled
        _uiState.update { it.copy(globalScrollEffectEnabled = enabled) }
    }

    fun setGlobalScrollEffectIndex(index: Int) {
        val effect = GlobalScrollEffect.fromIndex(index)
        repo.globalScrollEffect = effect.value
        _uiState.update { it.copy(globalScrollEffect = effect.value) }
    }

    fun setBackgroundScrollFollowEnabled(enabled: Boolean) {
        repo.backgroundScrollFollowEnabled = enabled
        _uiState.update { it.copy(backgroundScrollFollowEnabled = enabled) }
    }

    fun setPageTransitionEffectIndex(index: Int) {
        val effect = PageTransitionEffect.fromIndex(index)
        repo.pageTransitionEffect = effect.value
        _uiState.update { it.copy(pageTransitionEffect = effect.value) }
    }

    fun setLauncherIconByIndex(index: Int) {
        val option = LauncherIconOption.entries.getOrElse(index) { LauncherIconOption.Default }
        repo.launcherIcon = option.value
        _uiState.update { it.copy(launcherIcon = repo.launcherIcon) }
    }

    fun setCustomManagerName(name: String) {
        repo.customManagerName = name
        _uiState.update { it.copy(customManagerName = repo.customManagerName) }
    }

    fun setCustomHomeTitle(title: String) {
        repo.customHomeTitle = title
        _uiState.update { it.copy(customHomeTitle = repo.customHomeTitle) }
    }

    fun setCustomWallpaperUri(uri: String?) {
        repo.customWallpaperUri = uri
        _uiState.update {
            it.copy(
                customWallpaperUri = repo.customWallpaperUri,
                customWallpaperCrop = repo.customWallpaperCrop,
                customVideoBackgroundUri = repo.customVideoBackgroundUri,
            )
        }
    }

    fun clearCustomWallpaper() {
        setCustomWallpaperUri(null)
    }

    fun setCustomWallpaperOpacity(opacity: Float) {
        repo.customWallpaperOpacity = opacity
        _uiState.update { it.copy(customWallpaperOpacity = repo.customWallpaperOpacity) }
    }

    fun setCustomWallpaperVisualSettings(settings: me.weishu.kernelsu.ui.util.MediaVisualSettings) {
        repo.customWallpaperVisualSettings = settings
        _uiState.update { it.copy(customWallpaperVisualSettings = repo.customWallpaperVisualSettings) }
    }

    fun setCustomWallpaperCrop(crop: CustomWallpaperCrop) {
        repo.customWallpaperCrop = crop
        _uiState.update { it.copy(customWallpaperCrop = repo.customWallpaperCrop) }
    }

    fun setCustomWallpaperPassthroughEnabled(enabled: Boolean) {
        repo.customWallpaperPassthroughEnabled = enabled
        _uiState.update { it.copy(customWallpaperPassthroughEnabled = enabled) }
    }

    fun setCustomWallpaperPassthroughOpacity(opacity: Float) {
        repo.customWallpaperPassthroughOpacity = opacity
        _uiState.update { it.copy(customWallpaperPassthroughOpacity = repo.customWallpaperPassthroughOpacity) }
    }

    fun setCustomVideoBackgroundUri(uri: String?) {
        repo.customVideoBackgroundUri = uri
        _uiState.update {
            it.copy(
                customVideoBackgroundUri = repo.customVideoBackgroundUri,
                customWallpaperUri = repo.customWallpaperUri,
            )
        }
    }

    fun clearCustomVideoBackground() {
        setCustomVideoBackgroundUri(null)
    }

    fun setCustomVideoBackgroundDurationSeconds(seconds: Int) {
        repo.customVideoBackgroundDurationSeconds = seconds
        _uiState.update {
            it.copy(customVideoBackgroundDurationSeconds = repo.customVideoBackgroundDurationSeconds)
        }
    }

    fun setCustomVideoBackgroundFrameRate(frameRate: Int) {
        repo.customVideoBackgroundFrameRate = frameRate
        _uiState.update {
            it.copy(
                customVideoBackgroundFrameRate = repo.customVideoBackgroundFrameRate,
                customPageBackgrounds = repo.customPageBackgrounds,
            )
        }
    }

    fun setCustomPageBackgroundWallpaper(target: CustomPageBackgroundTarget, uri: String?) {
        repo.setCustomPageBackgroundWallpaper(target, uri)
        _uiState.update { it.copy(customPageBackgrounds = repo.customPageBackgrounds) }
    }

    fun setCustomPageBackgroundVideo(target: CustomPageBackgroundTarget, uri: String?) {
        repo.setCustomPageBackgroundVideo(target, uri)
        _uiState.update { it.copy(customPageBackgrounds = repo.customPageBackgrounds) }
    }

    fun setCustomPageBackgroundOpacity(target: CustomPageBackgroundTarget, opacity: Float) {
        repo.setCustomPageBackgroundOpacity(target, opacity)
        _uiState.update { it.copy(customPageBackgrounds = repo.customPageBackgrounds) }
    }

    fun setCustomPageBackgroundCrop(target: CustomPageBackgroundTarget, crop: CustomWallpaperCrop) {
        repo.setCustomPageBackgroundCrop(target, crop)
        _uiState.update { it.copy(customPageBackgrounds = repo.customPageBackgrounds) }
    }

    fun setCustomPageBackgroundVideoDurationSeconds(target: CustomPageBackgroundTarget, seconds: Int) {
        repo.setCustomPageBackgroundVideoDurationSeconds(target, seconds)
        _uiState.update { it.copy(customPageBackgrounds = repo.customPageBackgrounds) }
    }

    fun setCustomPageBackgroundVisualSettings(
        target: CustomPageBackgroundTarget,
        settings: me.weishu.kernelsu.ui.util.MediaVisualSettings,
    ) {
        repo.setCustomPageBackgroundVisualSettings(target, settings)
        _uiState.update { it.copy(customPageBackgrounds = repo.customPageBackgrounds) }
    }

    fun clearCustomPageBackground(target: CustomPageBackgroundTarget) {
        repo.clearCustomPageBackground(target)
        _uiState.update { it.copy(customPageBackgrounds = repo.customPageBackgrounds) }
    }

    fun setCustomStartupSoundUri(uri: String?) {
        repo.customStartupSoundUri = uri
        _uiState.update { it.copy(customStartupSoundUri = repo.customStartupSoundUri) }
    }

    fun clearCustomStartupSound() {
        setCustomStartupSoundUri(null)
    }

    fun setCustomStartupSoundDurationSeconds(seconds: Int) {
        repo.customStartupSoundDurationSeconds = seconds
        _uiState.update { it.copy(customStartupSoundDurationSeconds = repo.customStartupSoundDurationSeconds) }
    }

    fun setCustomStartupSoundVolume(volume: Float) {
        repo.customStartupSoundVolume = volume
        _uiState.update { it.copy(customStartupSoundVolume = repo.customStartupSoundVolume) }
    }

    fun setCustomClickSoundUri(uri: String?) {
        repo.customClickSoundUri = uri
        _uiState.update { it.copy(customClickSoundUri = repo.customClickSoundUri) }
    }

    fun clearCustomClickSound() {
        setCustomClickSoundUri(null)
    }

    fun setCustomClickSoundVolume(volume: Float) {
        repo.customClickSoundVolume = volume
        _uiState.update { it.copy(customClickSoundVolume = repo.customClickSoundVolume) }
    }

    fun setCustomBackgroundMusicUri(uri: String?) {
        repo.customBackgroundMusicUri = uri
        _uiState.update { it.copy(customBackgroundMusicUri = repo.customBackgroundMusicUri) }
    }

    fun clearCustomBackgroundMusic() {
        setCustomBackgroundMusicUri(null)
    }

    fun setCustomBackgroundMusicVolume(volume: Float) {
        repo.customBackgroundMusicVolume = volume
        _uiState.update { it.copy(customBackgroundMusicVolume = repo.customBackgroundMusicVolume) }
    }

    fun setCustomNavigationIcon(slot: CustomNavigationIconSlot, uriString: String?) {
        repo.setCustomNavigationIcon(slot, uriString)
        _uiState.update { it.copy(customNavigationIcons = repo.customNavigationIcons) }
    }

    fun clearCustomNavigationIcon(slot: CustomNavigationIconSlot) {
        setCustomNavigationIcon(slot, null)
    }

    fun setCustomNavigationIconCrop(slot: CustomNavigationIconSlot, crop: CustomWallpaperCrop) {
        repo.setCustomNavigationIconCrop(slot, crop)
        _uiState.update { it.copy(customNavigationIcons = repo.customNavigationIcons) }
    }

    fun setCustomNavigationIconPresentation(
        slot: CustomNavigationIconSlot,
        state: me.weishu.kernelsu.ui.util.CustomNavigationIconState,
    ) {
        repo.setCustomNavigationIconPresentation(slot, state)
        _uiState.update { it.copy(customNavigationIcons = repo.customNavigationIcons) }
    }

    fun setDeltaColorVariant(variant: String) {
        val sanitized = DeltaColorVariant.fromValue(variant).value
        repo.deltaColorVariant = sanitized
        _uiState.update { it.copy(deltaColorVariant = sanitized) }
    }

    fun setCustomStartupAnimationUri(uri: String?) {
        repo.customStartupAnimationUri = uri
        _uiState.update { it.copy(customStartupAnimationUri = repo.customStartupAnimationUri) }
    }

    fun setStartupAnimationSettings(settings: me.weishu.kernelsu.ui.util.StartupAnimationSettings) {
        repo.startupAnimationSettings = settings
        _uiState.update { it.copy(startupAnimationSettings = repo.startupAnimationSettings) }
    }

    fun clearCustomStartupAnimation() {
        setCustomStartupAnimationUri(null)
    }

    fun setThemeMode(mode: Int) {
        val currentUiMode = repo.uiMode
        val effectiveMode = if (InterfaceStyle.isMiuixBased(currentUiMode) && _uiState.value.miuixMonet) {
            mode + 3
        } else {
            mode
        }
        repo.themeMode = effectiveMode
        _uiState.update { it.copy(themeMode = effectiveMode, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setDayNightMode(enabled: Boolean) {
        setThemeMode(if (enabled) ColorMode.DARK.value else ColorMode.LIGHT.value)
    }

    fun setColorMode(mode: ColorMode) {
        if (mode == ColorMode.DARK_AMOLED) {
            repo.miuixMonet = false
        }
        repo.themeMode = mode.value
        _uiState.update {
            it.copy(
                themeMode = mode.value,
                miuixMonet = if (mode == ColorMode.DARK_AMOLED) false else it.miuixMonet,
                themePreset = ThemePreset.CUSTOM.value,
            )
        }
    }

    fun setMiuixMonet(enabled: Boolean) {
        val currentThemeMode = repo.themeMode
        val colorMode = ColorMode.fromValue(currentThemeMode)
        val newThemeMode = if (enabled) {
            if (!colorMode.isMonet) colorMode.toMonetMode() else currentThemeMode
        } else {
            if (colorMode.isMonet) colorMode.toNonMonetMode() else currentThemeMode
        }
        repo.miuixMonet = enabled
        repo.themeMode = newThemeMode
        _uiState.update {
            it.copy(
                miuixMonet = enabled,
                themeMode = newThemeMode,
                themePreset = ThemePreset.CUSTOM.value
            )
        }
    }

    fun setKeyColor(color: Int) {
        repo.keyColor = color
        _uiState.update { it.copy(keyColor = color, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setColorStyle(style: String) {
        repo.colorStyle = style
        _uiState.update { it.copy(colorStyle = style, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setColorSpec(spec: String) {
        repo.colorSpec = spec
        _uiState.update { it.copy(colorSpec = spec, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setMonetSurfaceOpacity(opacity: Float) {
        repo.monetSurfaceOpacity = opacity
        _uiState.update {
            it.copy(
                monetSurfaceOpacity = repo.monetSurfaceOpacity,
                themePreset = ThemePreset.CUSTOM.value,
            )
        }
    }

    fun applyThemePreset(preset: ThemePreset) {
        repo.applyThemePreset(preset)
        val uiMode = repo.uiMode
        _uiState.update {
            it.copy(
                uiMode = uiMode,
                themePreset = preset.value,
                themeMode = preset.colorMode.value,
                miuixMonet = preset.miuixMonet,
                keyColor = preset.keyColor,
                colorStyle = preset.paletteStyle.name,
                colorSpec = preset.colorSpec.name,
                monetSurfaceOpacity = preset.monetSurfaceOpacity,
                enableBlur = preset.enableBlur,
                enableFloatingBottomBar = preset.enableFloatingBottomBar,
                enableFloatingBottomBarBlur = preset.enableFloatingBottomBarBlur,
                pageScale = preset.pageScale,
                fontScale = repo.fontScale,
                blurIntensity = repo.blurIntensity,
                customThemePresets = repo.getCustomThemePresets(),
            )
        }
    }

    private fun resolveThemePreset(
        storedPreset: String,
        uiMode: String,
        themeMode: Int,
        miuixMonet: Boolean,
        keyColor: Int,
        colorStyle: String,
        colorSpec: String,
        monetSurfaceOpacity: Float,
        enableBlur: Boolean,
        enableFloatingBottomBar: Boolean,
        enableFloatingBottomBarBlur: Boolean,
        pageScale: Float,
        fontScale: Float,
        blurIntensity: Float,
    ): ThemePreset {
        if (storedPreset.isNotBlank()) {
            val preset = ThemePreset.fromValue(storedPreset)
            if (preset.isCompatibleWith(uiMode)) {
                return preset
            }
        }

        val current = ThemePreset.entries.firstOrNull { preset ->
            preset != ThemePreset.CUSTOM &&
                preset.isCompatibleWith(uiMode) &&
                preset.colorMode.value == themeMode &&
                preset.miuixMonet == miuixMonet &&
                preset.keyColor == keyColor &&
                preset.paletteStyle.name == colorStyle &&
                preset.colorSpec.name == colorSpec &&
                preset.monetSurfaceOpacity == monetSurfaceOpacity &&
                preset.enableBlur == enableBlur &&
                preset.enableFloatingBottomBar == enableFloatingBottomBar &&
                preset.enableFloatingBottomBarBlur == enableFloatingBottomBarBlur &&
                preset.pageScale == pageScale &&
                fontScale == ThemeAppearanceDefaults.FONT_SCALE &&
                blurIntensity == ThemeAppearanceDefaults.BLUR_INTENSITY
        }
        return current ?: ThemePreset.CUSTOM
    }

    fun setEnablePredictiveBack(enabled: Boolean) {
        repo.enablePredictiveBack = enabled
        _uiState.update { it.copy(enablePredictiveBack = enabled) }
    }

    fun setEnableBlur(enabled: Boolean) {
        if (_uiState.value.uiMode == InterfaceStyle.LiquidGlass.value) return
        repo.enableBlur = enabled
        _uiState.update { it.copy(enableBlur = enabled, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setEnableFloatingBottomBar(enabled: Boolean) {
        repo.enableFloatingBottomBar = enabled
        _uiState.update { it.copy(enableFloatingBottomBar = enabled, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setEnableFloatingBottomBarBlur(enabled: Boolean) {
        if (_uiState.value.uiMode == InterfaceStyle.LiquidGlass.value) return
        repo.enableFloatingBottomBarBlur = enabled
        _uiState.update { it.copy(enableFloatingBottomBarBlur = enabled, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setAutoHideNavigationBar(enabled: Boolean) {
        repo.autoHideNavigationBar = enabled
        _uiState.update { it.copy(autoHideNavigationBar = enabled) }
    }

    fun setScrollHideNavigationBar(enabled: Boolean) {
        repo.scrollHideNavigationBar = enabled
        _uiState.update { it.copy(scrollHideNavigationBar = enabled) }
    }

    fun setModuleTopBarAutoHideEnabled(enabled: Boolean) {
        repo.moduleTopBarAutoHideEnabled = enabled
        _uiState.update { it.copy(moduleTopBarAutoHideEnabled = enabled) }
    }

    fun setPageScale(scale: Float) {
        repo.pageScale = scale
        _uiState.update { it.copy(pageScale = scale, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setFontScale(scale: Float) {
        repo.fontScale = scale
        _uiState.update { it.copy(fontScale = repo.fontScale, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setBlurIntensity(intensity: Float) {
        repo.blurIntensity = intensity
        _uiState.update { it.copy(blurIntensity = repo.blurIntensity, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setSwitchStyleIndex(index: Int) {
        val style = SwitchStyle.fromIndex(index).value
        repo.switchStyle = style
        _uiState.update { it.copy(switchStyle = style) }
    }

    fun saveCustomThemePreset(name: String) {
        repo.saveCustomThemePreset(name)
        _uiState.update { it.copy(customThemePresets = repo.getCustomThemePresets()) }
    }

    fun applyCustomThemePreset(presetId: String) {
        if (repo.applyCustomThemePreset(presetId)) {
            refresh()
        }
    }

    fun renameCustomThemePreset(presetId: String, name: String) {
        if (repo.renameCustomThemePreset(presetId, name)) {
            _uiState.update { it.copy(customThemePresets = repo.getCustomThemePresets()) }
        }
    }

    fun deleteCustomThemePreset(presetId: String) {
        if (repo.deleteCustomThemePreset(presetId)) {
            _uiState.update { it.copy(customThemePresets = repo.getCustomThemePresets()) }
        }
    }

    fun setThemeSyncStrategy(strategy: ThemeSyncStrategy) {
        repo.themeSyncStrategy = strategy
        refresh()
    }

    fun resetThemeToDefault() {
        repo.resetThemeToDefault()
        refresh()
    }

    fun setEnableWebDebugging(enabled: Boolean) {
        repo.enableWebDebugging = enabled
        _uiState.update { it.copy(enableWebDebugging = enabled) }
    }

    fun setSuCompatMode(mode: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            when (mode) {
                0 -> if (repo.setSuEnabled(true)) {
                    repo.execKsudFeatureSave()
                    repo.setSuCompatModePref(0)
                    _uiState.update { it.copy(suCompatMode = 0, isSuEnabled = true) }
                }

                1 -> if (repo.setSuEnabled(true)) {
                    repo.execKsudFeatureSave()
                    if (repo.setSuEnabled(false)) {
                        // "Disable until reboot" implies it should be enabled on next boot.
                        // We set the preference to 0 (Enabled) to match the persistent state.
                        repo.setSuCompatModePref(0)
                        _uiState.update { it.copy(suCompatMode = 1, isSuEnabled = false) }
                    }
                }

                2 -> if (repo.setSuEnabled(false)) {
                    repo.execKsudFeatureSave()
                    repo.setSuCompatModePref(2)
                    _uiState.update { it.copy(suCompatMode = 2, isSuEnabled = false) }
                }
            }
        }
    }

    fun setKernelUmountEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setKernelUmountEnabled(enabled)) {
                repo.execKsudFeatureSave()
                _uiState.update { it.copy(isKernelUmountEnabled = enabled) }
            }
        }
    }

    fun setSelinuxHideEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val status = repo.setSelinuxHideEnabled(enabled)
            when (status) {
                0 -> {
                    repo.execKsudFeatureSave()
                    _uiState.update { it.copy(isSelinuxHideEnabled = enabled) }
                }
                -OsConstants.EAGAIN -> {
                    repo.execKsudFeatureSave()
                    _uiState.update { it.copy(isSelinuxHideEnabled = enabled) }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(ksuApp, R.string.settings_selinux_hide_reboot_required,
                            Toast.LENGTH_LONG).show()
                    }
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(ksuApp, ksuApp.getString(R.string.settings_selinux_hide_failed, status),
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun setAutoJailbreak(enabled: Boolean) {
        repo.autoJailbreak = enabled
        _uiState.update { it.copy(autoJailbreak = repo.autoJailbreak) }
    }

    fun setSulogEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setSulogEnabled(enabled)) {
                repo.execKsudFeatureSave()
                _uiState.update { it.copy(isSulogEnabled = enabled) }
            }
        }
    }

    fun setAdbRootEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setAdbRootEnabled(enabled)) {
                repo.execKsudFeatureSave()
                _uiState.update { it.copy(isAdbRootEnabled = enabled) }
            }
        }
    }

    fun setAvcSpoofEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setAvcSpoofEnabled(enabled)) {
                repo.execKsudFeatureSave()
                _uiState.update { it.copy(isAvcSpoofEnabled = enabled) }
            }
        }
    }

    fun setDefaultUmountModules(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setDefaultUmountModules(enabled)) {
                _uiState.update { it.copy(isDefaultUmountModules = enabled) }
            }
        }
    }

    fun setBuiltinMountEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setBuiltinMountEnabled(enabled)) {
                refreshBuiltinMountStatus()
                withContext(Dispatchers.Main) {
                    Toast.makeText(ksuApp, R.string.settings_builtin_mount_reboot_required, Toast.LENGTH_LONG).show()
                }
            } else {
                refreshBuiltinMountStatus()
                withContext(Dispatchers.Main) {
                    Toast.makeText(ksuApp, R.string.settings_builtin_mount_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun setBuiltinMountDefaultMode(index: Int) {
        val mode = if (index == 1) BUILTIN_MOUNT_MODE_MAGIC else BUILTIN_MOUNT_MODE_OVERLAY
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setBuiltinMountDefaultMode(mode)) {
                refreshBuiltinMountStatus()
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(ksuApp, R.string.settings_builtin_mount_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun setBuiltinMountVariant(index: Int) {
        val variant = if (index == 1) BUILTIN_MOUNT_VARIANT_FULL else BUILTIN_MOUNT_VARIANT_LITE
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setBuiltinMountVariant(variant)) {
                refreshBuiltinMountStatus()
                if (_uiState.value.isBuiltinMountEnabled) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(ksuApp, R.string.settings_builtin_mount_reboot_required, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                refreshBuiltinMountStatus()
                withContext(Dispatchers.Main) {
                    Toast.makeText(ksuApp, R.string.settings_builtin_mount_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun setKPatchNextEnabled(enabled: Boolean) {
        if (Natives.isLateLoadMode) {
            _uiState.update { it.copy(isLateLoadMode = true) }
            return
        }
        if (_uiState.value.isKPatchNextOperationRunning) return

        _uiState.update { it.copy(isKPatchNextOperationRunning = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = runCatching { repo.setKPatchNextEnabled(enabled) }
                    .onFailure { Log.e(TAG, "KPatch Next operation failed", it) }
                    .getOrDefault(false)
                runCatching { refreshKPatchNextStatus() }
                    .onFailure { Log.e(TAG, "Failed to refresh KPatch Next status", it) }
                withContext(Dispatchers.Main) {
                    val message = when {
                        !success -> R.string.settings_kpatch_next_failed
                        enabled -> R.string.settings_kpatch_next_install_scheduled
                        else -> R.string.settings_kpatch_next_uninstall_scheduled
                    }
                    Toast.makeText(ksuApp, message, Toast.LENGTH_LONG).show()
                }
            } finally {
                _uiState.update { it.copy(isKPatchNextOperationRunning = false) }
            }
        }
    }

    fun setEpkesuHideEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setEpkesuHideEnabled(enabled)) {
                val actualEnabled = repo.getEpkesuHideStatus()
                _uiState.update { it.copy(isEpkesuHideEnabled = actualEnabled) }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(ksuApp, R.string.settings_epkesu_hide_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun refreshBuiltinMountStatus() {
        val status = repo.getBuiltinMountStatus()
        _uiState.update {
            it.copy(
                isBuiltinMountEnabled = status.enabled,
                builtinMountDefaultMode = status.defaultMode,
                builtinMountVariant = status.variant,
                isBuiltinMountWebUiAvailable = status.webUi,
                builtinMountConflict = status.conflict,
                builtinMountSourceUrl = status.sourceUrl,
                builtinMountArchiveSha256 = status.archiveSha256,
                builtinMountLkmCount = status.lkmCount,
                builtinMountSupportedKmis = status.supportedKmis,
                builtinMountCurrentKmi = status.currentKmi,
                builtinMountCompatibility = status.compatibility,
                builtinMountLkmPurpose = status.lkmPurpose,
                builtinMountIsApkeSuRootDriver = status.apkeSuRootDriver,
            )
        }
    }

    private suspend fun refreshKPatchNextStatus() {
        val status = repo.getKPatchNextStatus()
        _uiState.update {
            it.copy(
                isKPatchNextInstalled = status.installed,
                isKPatchNextEnabled = status.enabled,
                isKPatchNextPendingUpdate = status.pendingUpdate,
                isKPatchNextPendingRemove = status.pendingRemove,
                isKPatchNextWebUiAvailable = status.webUi,
                isKPatchNextUnresolved = status.unresolved,
                kPatchNextVersion = status.version,
                kPatchNextConflict = status.conflict,
            )
        }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
