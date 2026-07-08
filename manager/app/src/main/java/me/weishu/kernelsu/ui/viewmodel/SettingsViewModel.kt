package me.weishu.kernelsu.ui.viewmodel

import android.system.OsConstants
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
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
import me.weishu.kernelsu.ui.component.GlobalScrollEffect
import me.weishu.kernelsu.ui.component.GlobalSnowEffect
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.screen.settings.SettingsUiState
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

    init {
        viewModelScope.launch(refreshExceptionHandler) {
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch(refreshExceptionHandler) {
            val checkModuleUpdate = repo.checkModuleUpdate
            val showVersionMismatchWarning = repo.showVersionMismatchWarning
            val showGkiWarning = repo.showGkiWarning
            val showHomeSupportCard = repo.showHomeSupportCard
            val showHomeLearnCard = repo.showHomeLearnCard
            val themeMode = repo.themeMode
            val miuixMonet = repo.miuixMonet
            val keyColor = repo.keyColor
            val enablePredictiveBack = repo.enablePredictiveBack
            val uiMode = repo.uiMode
            val isLiquidGlassInterface = uiMode == InterfaceStyle.LiquidGlass.value
            val enableBlur = if (isLiquidGlassInterface) false else repo.enableBlur
            val enableFloatingBottomBar = repo.enableFloatingBottomBar
            val enableFloatingBottomBarBlur = if (isLiquidGlassInterface) false else repo.enableFloatingBottomBarBlur
            val pageScale = repo.pageScale
            val fontScale = repo.fontScale
            val blurIntensity = repo.blurIntensity
            val switchStyle = repo.switchStyle
            val globalSnowEnabled = repo.globalSnowEnabled
            val globalSnowEffect = repo.globalSnowEffect
            val nightBackgroundEffect = repo.nightBackgroundEffect
            val nightBackgroundPassthrough = repo.nightBackgroundPassthrough
            val nightBackgroundPassthroughOpacity = repo.nightBackgroundPassthroughOpacity
            val globalScrollEffectEnabled = repo.globalScrollEffectEnabled
            val globalScrollEffect = repo.globalScrollEffect
            val themeSyncStrategy = repo.themeSyncStrategy
            val customThemePresets = repo.getCustomThemePresets()
            val enableWebDebugging = repo.enableWebDebugging
            val launcherIcon = repo.launcherIcon
            val customManagerName = repo.customManagerName
            val customWallpaperUri = repo.customWallpaperUri
            val customWallpaperOpacity = repo.customWallpaperOpacity
            val customWallpaperCrop = repo.customWallpaperCrop
            val customWallpaperPassthroughEnabled = repo.customWallpaperPassthroughEnabled
            val customWallpaperPassthroughOpacity = repo.customWallpaperPassthroughOpacity
            val customVideoBackgroundUri = repo.customVideoBackgroundUri
            val customVideoBackgroundDurationSeconds = repo.customVideoBackgroundDurationSeconds
            val customPageBackgrounds = repo.customPageBackgrounds
            val customStartupAnimationUri = repo.customStartupAnimationUri
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
            val themePreset = resolveThemePreset(
                repo.themePreset,
                uiMode = uiMode,
                themeMode = themeMode,
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
            val isLkmMode = repo.isLkmMode()

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
                    themeMode = themeMode,
                    miuixMonet = miuixMonet,
                    keyColor = keyColor,
                    themePreset = themePreset.value,
                    enablePredictiveBack = enablePredictiveBack,
                    enableBlur = enableBlur,
                    enableFloatingBottomBar = enableFloatingBottomBar,
                    enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
                    pageScale = pageScale,
                    fontScale = fontScale,
                    blurIntensity = blurIntensity,
                    switchStyle = switchStyle,
                    globalSnowEnabled = globalSnowEnabled,
                    globalSnowEffect = globalSnowEffect,
                    nightBackgroundEffect = nightBackgroundEffect,
                    nightBackgroundPassthrough = nightBackgroundPassthrough,
                    nightBackgroundPassthroughOpacity = nightBackgroundPassthroughOpacity,
                    globalScrollEffectEnabled = globalScrollEffectEnabled,
                    globalScrollEffect = globalScrollEffect,
                    themeSyncStrategy = themeSyncStrategy,
                    customThemePresets = customThemePresets,
                    enableWebDebugging = enableWebDebugging,
                    launcherIcon = launcherIcon,
                    customManagerName = customManagerName,
                    customWallpaperUri = customWallpaperUri,
                    customWallpaperOpacity = customWallpaperOpacity,
                    customWallpaperCrop = customWallpaperCrop,
                    customWallpaperPassthroughEnabled = customWallpaperPassthroughEnabled,
                    customWallpaperPassthroughOpacity = customWallpaperPassthroughOpacity,
                    customVideoBackgroundUri = customVideoBackgroundUri,
                    customVideoBackgroundDurationSeconds = customVideoBackgroundDurationSeconds,
                    customPageBackgrounds = customPageBackgrounds,
                    customStartupAnimationUri = customStartupAnimationUri,
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
                )
            }
        }
    }

    fun setUiMode(mode: String) {
        if (repo.themeSyncStrategy == ThemeSyncStrategy.PER_STYLE) {
            repo.uiMode = mode
            refresh()
            return
        }

        when (mode) {
            InterfaceStyle.Skrootpro.value -> {
                repo.uiMode = mode
                applyThemePreset(ThemePreset.SKROOTPRO)
                _uiState.update { it.copy(uiMode = mode) }
                return
            }

            InterfaceStyle.Alpha.value -> {
                repo.uiMode = mode
                applyThemePreset(ThemePreset.ALPHA)
                _uiState.update { it.copy(uiMode = mode) }
                return
            }

            InterfaceStyle.Delta.value -> {
                repo.uiMode = mode
                applyThemePreset(ThemePreset.DELTA)
                _uiState.update { it.copy(uiMode = mode) }
                return
            }

            InterfaceStyle.LiquidGlass.value -> {
                repo.uiMode = mode
                applyThemePreset(ThemePreset.LIQUID_GLASS)
                _uiState.update { it.copy(uiMode = mode) }
                return
            }
        }

        val oldMode = repo.uiMode
        val currentThemeMode = repo.themeMode
        val isLeavingSpecialStyle = oldMode == InterfaceStyle.Skrootpro.value ||
            oldMode == InterfaceStyle.Alpha.value ||
            oldMode == InterfaceStyle.Delta.value ||
            oldMode == InterfaceStyle.LiquidGlass.value

        if (isLeavingSpecialStyle && (mode == InterfaceStyle.Miuix.value || mode == InterfaceStyle.Material.value)) {
            repo.uiMode = mode
            applyThemePreset(ThemePreset.CLEAN_TOOL)
            return
        }

        val newThemeMode = when {
            oldMode == InterfaceStyle.Material.value && InterfaceStyle.isMiuixBased(mode) -> {
                val colorMode = ColorMode.fromValue(currentThemeMode)
                val baseMode = if (colorMode == ColorMode.DARK_AMOLED) 2 else currentThemeMode
                if (repo.miuixMonet && !colorMode.isMonet) {
                    ColorMode.fromValue(baseMode).toMonetMode()
                } else if (!repo.miuixMonet && colorMode.isMonet) {
                    ColorMode.fromValue(baseMode).toNonMonetMode()
                } else baseMode
            }

            InterfaceStyle.isMiuixBased(oldMode) &&
                mode == InterfaceStyle.Material.value -> {
                val colorMode = ColorMode.fromValue(currentThemeMode)
                if (colorMode.isMonet) {
                    colorMode.toNonMonetMode()
                } else currentThemeMode
            }

            else -> currentThemeMode
        }

        repo.uiMode = mode
        repo.themeMode = newThemeMode
        _uiState.update {
            it.copy(
                uiMode = mode,
                themeMode = newThemeMode,
                themePreset = ThemePreset.CUSTOM.value
            )
        }
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

    fun setGlobalSnowEnabled(enabled: Boolean) {
        repo.globalSnowEnabled = enabled
        _uiState.update { it.copy(globalSnowEnabled = enabled) }
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

    fun setLauncherIconByIndex(index: Int) {
        val option = LauncherIconOption.entries.getOrElse(index) { LauncherIconOption.Default }
        repo.launcherIcon = option.value
        _uiState.update { it.copy(launcherIcon = repo.launcherIcon) }
    }

    fun setCustomManagerName(name: String) {
        repo.customManagerName = name
        _uiState.update { it.copy(customManagerName = repo.customManagerName) }
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

    fun setDeltaColorVariant(variant: String) {
        val sanitized = DeltaColorVariant.fromValue(variant).value
        repo.deltaColorVariant = sanitized
        _uiState.update { it.copy(deltaColorVariant = sanitized) }
    }

    fun setCustomStartupAnimationUri(uri: String?) {
        repo.customStartupAnimationUri = uri
        _uiState.update { it.copy(customStartupAnimationUri = repo.customStartupAnimationUri) }
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
        repo.themeMode = mode.value
        _uiState.update { it.copy(themeMode = mode.value, themePreset = ThemePreset.CUSTOM.value) }
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
        if (_uiState.value.uiMode == InterfaceStyle.LiquidGlass.value && enabled) return
        repo.enableBlur = enabled
        _uiState.update { it.copy(enableBlur = enabled, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setEnableFloatingBottomBar(enabled: Boolean) {
        repo.enableFloatingBottomBar = enabled
        _uiState.update { it.copy(enableFloatingBottomBar = enabled, themePreset = ThemePreset.CUSTOM.value) }
    }

    fun setEnableFloatingBottomBarBlur(enabled: Boolean) {
        if (_uiState.value.uiMode == InterfaceStyle.LiquidGlass.value && enabled) return
        repo.enableFloatingBottomBarBlur = enabled
        _uiState.update { it.copy(enableFloatingBottomBarBlur = enabled, themePreset = ThemePreset.CUSTOM.value) }
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
            repo.execKsudFeatureSave()
            _uiState.update { it.copy(isSelinuxHideEnabled = enabled) }
            when (status) {
                0 -> {}
                -OsConstants.EAGAIN -> {
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
        _uiState.update { it.copy(autoJailbreak = enabled) }
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

    fun setBuiltinMountVariant(@Suppress("UNUSED_PARAMETER") index: Int) {
        val variant = BUILTIN_MOUNT_VARIANT_LITE
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
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setKPatchNextEnabled(enabled)) {
                refreshKPatchNextStatus()
                withContext(Dispatchers.Main) {
                    Toast.makeText(ksuApp, R.string.settings_kpatch_next_reboot_required, Toast.LENGTH_LONG).show()
                }
            } else {
                refreshKPatchNextStatus()
                withContext(Dispatchers.Main) {
                    Toast.makeText(ksuApp, R.string.settings_kpatch_next_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun setEpkesuHideEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.setEpkesuHideEnabled(enabled)) {
                _uiState.update { it.copy(isEpkesuHideEnabled = enabled) }
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
