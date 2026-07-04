package me.weishu.kernelsu.ui.screen.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.CUSTOM_BACKGROUND_MIME_TYPES
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_URI_KEY
import me.weishu.kernelsu.ui.util.HYBRID_MOUNT_MODULE_ID
import me.weishu.kernelsu.ui.util.KPATCH_NEXT_MODULE_ID
import me.weishu.kernelsu.ui.util.isCustomVideoBackground
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.takePersistableImageReadPermission
import me.weishu.kernelsu.ui.util.takePersistableVideoBackgroundReadPermission
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import me.weishu.kernelsu.ui.webui.WebUIActivity

@Composable
fun SettingPager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    val context = LocalContext.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showWallpaperPreview = rememberSaveable { mutableStateOf(false) }
    val showVideoBackgroundPreview = rememberSaveable { mutableStateOf(false) }
    val showWallpaperCropEditor = rememberSaveable { mutableStateOf(false) }
    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        if (isCustomVideoBackground(context, uri)) {
            takePersistableVideoBackgroundReadPermission(context, uri)
            viewModel.setCustomVideoBackgroundUri(uri.toString())
            showWallpaperPreview.value = false
            showWallpaperCropEditor.value = false
            showVideoBackgroundPreview.value = true
        } else {
            val uriString = persistCustomImageReference(context, uri, CUSTOM_WALLPAPER_URI_KEY)
                ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
            viewModel.setCustomWallpaperUri(uriString)
            showVideoBackgroundPreview.value = false
            showWallpaperCropEditor.value = true
        }
    }
    val videoBackgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        takePersistableVideoBackgroundReadPermission(context, uri)
        viewModel.setCustomVideoBackgroundUri(uri.toString())
        showWallpaperPreview.value = false
        showWallpaperCropEditor.value = false
        showVideoBackgroundPreview.value = true
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val actions = SettingsScreenActions(
        onSetCheckModuleUpdate = viewModel::setCheckModuleUpdate,
        onSetShowVersionMismatchWarning = viewModel::setShowVersionMismatchWarning,
        onSetShowGkiWarning = viewModel::setShowGkiWarning,
        onSetShowHomeSupportCard = viewModel::setShowHomeSupportCard,
        onSetShowHomeLearnCard = viewModel::setShowHomeLearnCard,
        onOpenTheme = { navigator.push(Route.ColorPalette) },
        onOpenThemeStore = { navigator.push(Route.ThemeStore) },
        onSetDayNightMode = viewModel::setDayNightMode,
        onSetSwitchStyleIndex = viewModel::setSwitchStyleIndex,
        onSetGlobalSnowEnabled = viewModel::setGlobalSnowEnabled,
        onSetGlobalSnowEffectIndex = viewModel::setGlobalSnowEffectIndex,
        onSetNightBackgroundEffectIndex = viewModel::setNightBackgroundEffectIndex,
        onSetNightBackgroundPassthrough = viewModel::setNightBackgroundPassthrough,
        onSetNightBackgroundPassthroughOpacity = viewModel::setNightBackgroundPassthroughOpacity,
        onSetGlobalScrollEffectEnabled = viewModel::setGlobalScrollEffectEnabled,
        onSetGlobalScrollEffectIndex = viewModel::setGlobalScrollEffectIndex,
        onSetUiModeIndex = { index ->
            viewModel.setUiMode(InterfaceStyle.fromIndex(index).value)
        },
        onOpenLauncherIcon = { navigator.push(Route.LauncherIcon) },
        onOpenNavigationIcons = { navigator.push(Route.NavigationIcons) },
        onOpenHomeCardWallpapers = { navigator.push(Route.HomeCardWallpapers) },
        onOpenVisualEffects = { navigator.push(Route.VisualEffects) },
        onOpenBackgrounds = { navigator.push(Route.Backgrounds) },
        onOpenSoundEffects = { navigator.push(Route.SoundEffects) },
        onPickWallpaper = { wallpaperLauncher.launch(CUSTOM_BACKGROUND_MIME_TYPES) },
        onPreviewWallpaper = {
            if (uiState.customVideoBackgroundUri.isNullOrBlank()) {
                showWallpaperPreview.value = true
            } else {
                showVideoBackgroundPreview.value = true
            }
        },
        onEditWallpaperCrop = { showWallpaperCropEditor.value = true },
        onClearWallpaper = {
            viewModel.clearCustomWallpaper()
            viewModel.clearCustomVideoBackground()
            showWallpaperPreview.value = false
            showVideoBackgroundPreview.value = false
            showWallpaperCropEditor.value = false
        },
        onSetWallpaperOpacity = viewModel::setCustomWallpaperOpacity,
        onSetWallpaperCrop = viewModel::setCustomWallpaperCrop,
        onSetWallpaperPassthroughEnabled = viewModel::setCustomWallpaperPassthroughEnabled,
        onSetWallpaperPassthroughOpacity = viewModel::setCustomWallpaperPassthroughOpacity,
        onPickVideoBackground = { videoBackgroundLauncher.launch(arrayOf("video/*")) },
        onPreviewVideoBackground = { showVideoBackgroundPreview.value = true },
        onClearVideoBackground = {
            viewModel.clearCustomVideoBackground()
            showVideoBackgroundPreview.value = false
        },
        onSetVideoBackgroundDurationSeconds = viewModel::setCustomVideoBackgroundDurationSeconds,
        onSetPageBackgroundWallpaper = viewModel::setCustomPageBackgroundWallpaper,
        onSetPageBackgroundVideo = viewModel::setCustomPageBackgroundVideo,
        onSetPageBackgroundOpacity = viewModel::setCustomPageBackgroundOpacity,
        onSetPageBackgroundCrop = viewModel::setCustomPageBackgroundCrop,
        onSetPageBackgroundVideoDurationSeconds = viewModel::setCustomPageBackgroundVideoDurationSeconds,
        onClearPageBackground = viewModel::clearCustomPageBackground,
        onSaveCustomThemePreset = viewModel::saveCustomThemePreset,
        onApplyCustomThemePreset = viewModel::applyCustomThemePreset,
        onRenameCustomThemePreset = viewModel::renameCustomThemePreset,
        onDeleteCustomThemePreset = viewModel::deleteCustomThemePreset,
        onSetThemeSyncStrategy = viewModel::setThemeSyncStrategy,
        onResetThemeToDefault = viewModel::resetThemeToDefault,
        onOpenStartupAnimation = { navigator.push(Route.StartupAnimation) },
        onOpenProfileTemplate = { navigator.push(Route.AppProfileTemplate) },
        onSetSuCompatMode = viewModel::setSuCompatMode,
        onSetKernelUmountEnabled = viewModel::setKernelUmountEnabled,
        onSetSelinuxHideEnabled = viewModel::setSelinuxHideEnabled,
        onSetSulogEnabled = viewModel::setSulogEnabled,
        onSetAdbRootEnabled = viewModel::setAdbRootEnabled,
        onSetAvcSpoofEnabled = viewModel::setAvcSpoofEnabled,
        onSetDefaultUmountModules = viewModel::setDefaultUmountModules,
        onSetBuiltinMountEnabled = viewModel::setBuiltinMountEnabled,
        onSetBuiltinMountDefaultMode = viewModel::setBuiltinMountDefaultMode,
        onOpenBuiltinMountWebUi = {
            context.startActivity(
                Intent(context, WebUIActivity::class.java)
                    .setData("kernelsu://webui/$HYBRID_MOUNT_MODULE_ID".toUri())
                    .putExtra("id", HYBRID_MOUNT_MODULE_ID)
            )
        },
        onSetKPatchNextEnabled = viewModel::setKPatchNextEnabled,
        onOpenKPatchNextWebUi = {
            context.startActivity(
                Intent(context, WebUIActivity::class.java)
                    .setData("kernelsu://webui/$KPATCH_NEXT_MODULE_ID".toUri())
                    .putExtra("id", KPATCH_NEXT_MODULE_ID)
            )
        },
        onOpenHiddenPathConfig = { navigator.push(Route.HiddenPathConfig) },
        onOpenAiChat = { navigator.push(Route.AiChat) },
        onOpenRescueProtection = { navigator.push(Route.RescueProtection) },
        onSetEpkesuHideEnabled = viewModel::setEpkesuHideEnabled,
        onSetEnableWebDebugging = viewModel::setEnableWebDebugging,
        onSetAutoJailbreak = viewModel::setAutoJailbreak,
        onSetDeltaColorVariant = viewModel::setDeltaColorVariant,
        onOpenAbout = dropUnlessResumed {
            if (navigator.current() !is Route.About) {
                navigator.push(Route.About)
            }
        },
    )

    when (LocalInterfaceStyle.current) {
        InterfaceStyle.Skrootpro.value -> SettingPagerSkrootpro(uiState, actions, bottomInnerPadding)
        InterfaceStyle.Delta.value -> SettingPagerDelta(uiState, actions, bottomInnerPadding)
        InterfaceStyle.Alpha.value -> SettingPagerAlpha(uiState, actions, bottomInnerPadding)
        else -> {
            when (LocalUiMode.current) {
                UiMode.Miuix -> SettingPagerMiuix(uiState, actions, bottomInnerPadding)
                UiMode.Material -> SettingPagerMaterial(uiState, actions, bottomInnerPadding)
            }
        }
    }

    SettingsWallpaperPreviewDialog(
        show = showWallpaperPreview.value,
        uriString = uiState.customWallpaperUri,
        opacity = uiState.customWallpaperOpacity,
        crop = uiState.customWallpaperCrop,
        passthroughEnabled = uiState.customWallpaperPassthroughEnabled,
        passthroughOpacity = uiState.customWallpaperPassthroughOpacity,
        onDismissRequest = { showWallpaperPreview.value = false },
    )
    SettingsVideoBackgroundPreviewDialog(
        show = showVideoBackgroundPreview.value,
        uriString = uiState.customVideoBackgroundUri,
        durationSeconds = uiState.customVideoBackgroundDurationSeconds,
        opacity = uiState.customWallpaperOpacity,
        passthroughEnabled = uiState.customWallpaperPassthroughEnabled,
        passthroughOpacity = uiState.customWallpaperPassthroughOpacity,
        onDismissRequest = { showVideoBackgroundPreview.value = false },
    )
    SettingsWallpaperCropDialog(
        show = showWallpaperCropEditor.value,
        uriString = uiState.customWallpaperUri,
        crop = uiState.customWallpaperCrop,
        onCropChange = {
            actions.onSetWallpaperCrop(it)
            showWallpaperPreview.value = true
        },
        onDismissRequest = {
            showWallpaperCropEditor.value = false
        },
    )
}
