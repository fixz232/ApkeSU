package me.weishu.kernelsu.ui.screen.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.CUSTOM_BACKGROUND_MIME_TYPES
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_URI_KEY
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
    bottomInnerPadding: Dp,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pageMode = remember { mutableStateOf(readSettingsPageMode(context)) }
    val onPageModeChange: (SettingsPageMode) -> Unit = { mode ->
        pageMode.value = mode
        setSettingsPageMode(context, mode)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    if (pageMode.value == SettingsPageMode.Categories) {
        SettingsHubScreen(
            uiState = uiState,
            bottomInnerPadding = bottomInnerPadding,
            onOpenCategory = { category ->
                navigator.push(Route.SettingsCategory(category.routeValue))
            },
            onPageModeChange = onPageModeChange,
        )
        return
    }

    val showWallpaperPreview = rememberSaveable { mutableStateOf(false) }
    val showVideoBackgroundPreview = rememberSaveable { mutableStateOf(false) }
    val showWallpaperCropEditor = rememberSaveable { mutableStateOf(false) }
    val showHomeTitleDialog = rememberSaveable { mutableStateOf(false) }
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

    val actions = SettingsScreenActions(
        onSetCheckModuleUpdate = viewModel::setCheckModuleUpdate,
        onSetShowVersionMismatchWarning = viewModel::setShowVersionMismatchWarning,
        onSetShowGkiWarning = viewModel::setShowGkiWarning,
        onSetShowHomeSupportCard = viewModel::setShowHomeSupportCard,
        onSetShowHomeLearnCard = viewModel::setShowHomeLearnCard,
        onSetMiuixClassicHomeLayoutEnabled = viewModel::setMiuixClassicHomeLayoutEnabled,
        onSetGraphicsRendererFeatureEnabled = viewModel::setGraphicsRendererFeatureEnabled,
        onOpenTheme = { navigator.push(Route.ColorPalette) },
        onOpenThemeStore = { navigator.push(Route.ThemeStore) },
        onOpenLanguage = { navigator.push(Route.LanguageSettings) },
        onOpenDynamicManager = { navigator.push(Route.DynamicManager) },
        onSetDayNightMode = viewModel::setDayNightMode,
        onSetSwitchStyleIndex = viewModel::setSwitchStyleIndex,
        onSetSeasonStyleIndex = viewModel::setSeasonStyleIndex,
        onSetSeasonCardMotionEnabled = viewModel::setSeasonCardMotionEnabled,
        onSetRainStyleIndex = viewModel::setRainStyleIndex,
        onSetRainCardMotionEnabled = viewModel::setRainCardMotionEnabled,
        onSetPixelStyleIndex = viewModel::setPixelStyleIndex,
        onSetPixelCardMotionEnabled = viewModel::setPixelCardMotionEnabled,
        onSetGlobalSnowEnabled = viewModel::setGlobalSnowEnabled,
        onSetGlobalSnowEffectIndex = viewModel::setGlobalSnowEffectIndex,
        onSetNightBackgroundEffectIndex = viewModel::setNightBackgroundEffectIndex,
        onSetNightBackgroundPassthrough = viewModel::setNightBackgroundPassthrough,
        onSetNightBackgroundPassthroughOpacity = viewModel::setNightBackgroundPassthroughOpacity,
        onSetGlobalScrollEffectEnabled = viewModel::setGlobalScrollEffectEnabled,
        onSetGlobalScrollEffectIndex = viewModel::setGlobalScrollEffectIndex,
        onSetUiModeIndex = { index ->
            val style = InterfaceStyle.fromIndex(index)
            if (style != InterfaceStyle.Alpha || uiState.uiMode != InterfaceStyle.Delta.value) {
                viewModel.setUiMode(style.value)
            }
        },
        onSetAlphaDeltaMode = { useDelta ->
            viewModel.setUiMode(
                if (useDelta) InterfaceStyle.Delta.value else InterfaceStyle.Alpha.value
            )
        },
        onOpenLauncherIcon = { navigator.push(Route.LauncherIcon) },
        onEditHomeTitle = { showHomeTitleDialog.value = true },
        onOpenNavigationIcons = { navigator.push(Route.NavigationIcons) },
        onOpenHomeLayout = { navigator.push(Route.HomeLayout) },
        onOpenHomeCardWallpapers = { navigator.push(Route.HomeCardWallpapers) },
        onOpenPixelPet = { navigator.push(Route.PixelPet) },
        onOpenVisualEffects = { navigator.push(Route.VisualEffects) },
        onOpenUiDecorationLibrary = { navigator.push(Route.UiDecorationLibrary) },
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
        onSetVideoBackgroundFrameRate = viewModel::setCustomVideoBackgroundFrameRate,
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
        onOpenProfileTemplate = { navigator.push(Route.AppProfileTemplate) },
        onSetSuCompatMode = viewModel::setSuCompatMode,
        onSetKernelUmountEnabled = viewModel::setKernelUmountEnabled,
        onSetWebViewZygoteUmountEnabled = viewModel::setWebViewZygoteUmountEnabled,
        onSetSelinuxHideEnabled = viewModel::setSelinuxHideEnabled,
        onSetSulogEnabled = viewModel::setSulogEnabled,
        onSetAdbRootEnabled = viewModel::setAdbRootEnabled,
        onSetAvcSpoofEnabled = viewModel::setAvcSpoofEnabled,
        onSetDefaultUmountModules = viewModel::setDefaultUmountModules,
        onOpenBuiltinMount = { navigator.push(Route.BuiltinMount) },
        onSetKPatchNextEnabled = { enabled ->
            if (!uiState.isLateLoadMode) {
                viewModel.setKPatchNextEnabled(enabled)
            }
        },
        onOpenKPatchNextWebUi = {
            if (uiState.canOpenKPatchNextWebUi) {
                context.startActivity(
                    Intent(context, WebUIActivity::class.java)
                        .setData("kernelsu://webui/$KPATCH_NEXT_MODULE_ID".toUri())
                        .putExtra("id", KPATCH_NEXT_MODULE_ID)
                )
            }
        },
        onOpenHiddenPathConfig = {
            when (uiState.pathConfigBackend) {
                PathConfigBackend.PathmaskLkm -> navigator.push(Route.HiddenPathConfig)
                PathConfigBackend.SusfsGki -> navigator.push(Route.SusfsPathConfig)
                PathConfigBackend.Disabled,
                PathConfigBackend.Unknown,
                -> Unit
            }
        },
        onOpenAiChat = { navigator.push(Route.AiChat) },
        onOpenRescueProtection = { navigator.push(Route.RescueProtection) },
        onOpenCpuSpoof = { navigator.push(Route.CpuSpoof) },
        onOpenDeviceIdentity = { navigator.push(Route.DeviceIdentity) },
        onOpenGraphicsRenderer = { navigator.push(Route.GraphicsRenderer) },
        onOpenKpm = { navigator.push(Route.Kpm) },
        onOpenImageTool = { navigator.push(Route.ImageTool) },
        onSetEpkesuHideEnabled = viewModel::setEpkesuHideEnabled,
        onSetEnableWebDebugging = viewModel::setEnableWebDebugging,
        onSetAutoJailbreak = viewModel::setAutoJailbreak,
        onSetUseSoftReboot = viewModel::setUseSoftReboot,
        onSetDeltaColorVariant = viewModel::setDeltaColorVariant,
        onOpenAbout = dropUnlessResumed {
            if (navigator.current() !is Route.About) {
                navigator.push(Route.About)
            }
        },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Settings use one stable Xiaomi/Miuix layout across interface themes.
        // Theme-specific colors, backgrounds, decorations, and motion are still
        // supplied through composition locals and remain available to the shared
        // screen without duplicating the settings hierarchy.
        SettingPagerMiuix(uiState, actions, bottomInnerPadding)

        SettingsPageModeButton(
            currentMode = SettingsPageMode.Collapsed,
            onModeChange = onPageModeChange,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 4.dp,
                    end = 8.dp,
                )
                .zIndex(4f),
        )
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
    HomeTitleDialog(
        show = showHomeTitleDialog.value,
        initialTitle = uiState.customHomeTitle,
        onDismissRequest = { showHomeTitleDialog.value = false },
        onConfirm = viewModel::setCustomHomeTitle,
    )
    SettingsVideoBackgroundPreviewDialog(
        show = showVideoBackgroundPreview.value,
        uriString = uiState.customVideoBackgroundUri,
        durationSeconds = uiState.customVideoBackgroundDurationSeconds,
        frameRate = uiState.customVideoBackgroundFrameRate,
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
