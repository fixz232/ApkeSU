package me.weishu.kernelsu.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.ui.component.AutoHidingNavigationBar
import me.weishu.kernelsu.ui.component.CustomWallpaperRoot
import me.weishu.kernelsu.ui.component.GlobalScrollEffect
import me.weishu.kernelsu.ui.component.GlobalScrollEffectOverlay
import me.weishu.kernelsu.ui.component.GlobalSnowEffectOverlay
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.LocalNightBackgroundEffectActive
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.NightBackgroundEffectOverlay
import me.weishu.kernelsu.ui.component.navigationBarVisibilityController
import me.weishu.kernelsu.ui.component.rememberNavigationBarVisibilityState
import me.weishu.kernelsu.ui.component.StartupAnimationOverlay
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.bottombar.BottomBar
import me.weishu.kernelsu.ui.component.bottombar.MainPagerState
import me.weishu.kernelsu.ui.component.bottombar.SideRail
import me.weishu.kernelsu.ui.component.bottombar.rememberMainPagerState
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.decoration.LocalUiDecorationConfig
import me.weishu.kernelsu.ui.component.decoration.LocalUiDecorationScope
import me.weishu.kernelsu.ui.component.decoration.UiDecorationBackdrop
import me.weishu.kernelsu.ui.component.decoration.UiDecorationChromeOverlay
import me.weishu.kernelsu.ui.component.decoration.UiDecorationScope
import me.weishu.kernelsu.ui.component.liquid.LocalLiquidGlassBackdrop
import me.weishu.kernelsu.ui.component.liquid.liquidGlassBackdropColor
import me.weishu.kernelsu.ui.component.pixel.LocalPixelStyle
import me.weishu.kernelsu.ui.component.pixel.PixelBackdrop
import me.weishu.kernelsu.ui.component.pixel.PixelChromeOverlay
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.snow.LocalSeasonStyle
import me.weishu.kernelsu.ui.component.snow.SeasonAmbientOverlay
import me.weishu.kernelsu.ui.component.snow.SeasonChromeOverlay
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyleWallpaper
import me.weishu.kernelsu.ui.component.globalScrollEffectController
import me.weishu.kernelsu.ui.component.rememberGlobalScrollEffectState
import me.weishu.kernelsu.ui.navigation3.HandleDeepLink
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.navigation3.rememberNavigator
import me.weishu.kernelsu.ui.screen.about.AboutScreen
import me.weishu.kernelsu.ui.screen.appprofile.AppProfileScreen
import me.weishu.kernelsu.ui.screen.colorpalette.ColorPaletteScreen
import me.weishu.kernelsu.ui.screen.executemoduleaction.ExecuteModuleActionScreen
import me.weishu.kernelsu.ui.screen.flash.FlashIt
import me.weishu.kernelsu.ui.screen.flash.FlashScreen
import me.weishu.kernelsu.ui.screen.home.HomePager
import me.weishu.kernelsu.ui.screen.install.InstallScreen
import me.weishu.kernelsu.ui.screen.launchericon.LauncherIconScreen
import me.weishu.kernelsu.ui.screen.module.ModulePager
import me.weishu.kernelsu.ui.screen.modulerepo.ModuleRepoDetailScreen
import me.weishu.kernelsu.ui.screen.modulerepo.ModuleRepoScreen
import me.weishu.kernelsu.ui.screen.navigationicon.NavigationIconScreen
import me.weishu.kernelsu.ui.screen.settings.BackgroundSettingsScreen
import me.weishu.kernelsu.ui.screen.settings.AiChatScreen
import me.weishu.kernelsu.ui.screen.settings.AiModuleStudioScreen
import me.weishu.kernelsu.ui.screen.settings.BuiltinMountScreen
import me.weishu.kernelsu.ui.screen.settings.CpuSpoofScreen
import me.weishu.kernelsu.ui.screen.settings.GraphicsRendererScreen
import me.weishu.kernelsu.ui.screen.settings.HiddenPathConfigScreen
import me.weishu.kernelsu.ui.screen.settings.SusfsPathConfigScreen
import me.weishu.kernelsu.ui.screen.settings.RescueProtectionScreen
import me.weishu.kernelsu.ui.screen.settings.HomeCardWallpaperScreen
import me.weishu.kernelsu.ui.screen.settings.PreInstallStyleSettingsScreen
import me.weishu.kernelsu.ui.screen.settings.SettingPager
import me.weishu.kernelsu.ui.screen.settings.SoundEffectsScreen
import me.weishu.kernelsu.ui.screen.settings.StartupAnimationScreen
import me.weishu.kernelsu.ui.screen.settings.UiDecorationLibraryScreen
import me.weishu.kernelsu.ui.screen.settings.VisualEffectsScreen
import me.weishu.kernelsu.ui.screen.sulog.SulogScreen
import me.weishu.kernelsu.ui.screen.superuser.SuperUserPager
import me.weishu.kernelsu.ui.screen.template.AppProfileTemplateScreen
import me.weishu.kernelsu.ui.screen.templateeditor.TemplateEditorScreen
import me.weishu.kernelsu.ui.screen.themestore.ThemeStoreScreen
import me.weishu.kernelsu.ui.screen.home.hasBlockingRootVersionMismatch
import me.weishu.kernelsu.ui.theme.KernelSUTheme
import me.weishu.kernelsu.ui.theme.LocalAutoHideNavigationBar
import me.weishu.kernelsu.ui.theme.LocalBlurIntensity
import me.weishu.kernelsu.ui.theme.LocalColorMode
import me.weishu.kernelsu.ui.theme.LocalDeltaColorVariant
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.theme.LocalEnableFloatingBottomBar
import me.weishu.kernelsu.ui.theme.LocalEnableFloatingBottomBarBlur
import me.weishu.kernelsu.ui.theme.LocalImmersiveBackgroundActive
import me.weishu.kernelsu.ui.theme.LocalScrollHideNavigationBar
import me.weishu.kernelsu.ui.util.BackgroundMusicPlayer
import me.weishu.kernelsu.ui.util.ClickSoundPlayer
import me.weishu.kernelsu.ui.util.KernelStatusEvents
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons
import me.weishu.kernelsu.ui.util.LocalScrollAnimation
import me.weishu.kernelsu.ui.util.LocalScrollAnimationEffect
import me.weishu.kernelsu.ui.util.ManagerUpdateChecker
import me.weishu.kernelsu.ui.util.ManagerUpdateInfo
import me.weishu.kernelsu.ui.util.ensureManagerRegistered
import me.weishu.kernelsu.ui.util.getFileName
import me.weishu.kernelsu.ui.util.install
import me.weishu.kernelsu.ui.util.ksuRootAvailable
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import me.weishu.kernelsu.ui.util.rememberContentReady
import me.weishu.kernelsu.ui.util.rootAvailable
import me.weishu.kernelsu.ui.util.StartupSoundPlayer
import me.weishu.kernelsu.ui.viewmodel.MainActivityUiState
import me.weishu.kernelsu.ui.viewmodel.MainActivityViewModel
import me.weishu.kernelsu.ui.viewmodel.MainPagerConfig
import me.weishu.kernelsu.ui.webui.WebUIActivity
import me.weishu.kernelsu.ui.util.CustomBackgroundState
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {

    private val intentState = MutableStateFlow(0)
    private val managerReadyState = MutableStateFlow(false)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            val managerReady = runCatching {
                Natives.refreshInfo()
                Natives.isManager || ensureManagerRegistered()
            }.onFailure {
                Log.e(TAG, "refresh manager identity failed", it)
            }.getOrDefault(false)
            managerReadyState.value = managerReady
            val kernelCompatible = runCatching { !Natives.requireNewKernel() }.getOrDefault(false)
            if (managerReady && kernelCompatible) {
                runCatching { check(install()) { "ksud install command failed" } }
                    .onSuccess { KernelStatusEvents.requestRefresh() }
                    .onFailure { Log.e(TAG, "install ksud failed", it) }
            }
        }

        setContent {
            val viewModel = viewModel<MainActivityViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val selectedMainPage by viewModel.selectedMainPage.collectAsStateWithLifecycle()
            val managerReady by managerReadyState.collectAsStateWithLifecycle()
            val appSettings = uiState.appSettings
            val uiMode = uiState.uiMode
            val isLiquidGlassInterface = uiState.interfaceStyle == InterfaceStyle.LiquidGlass.value
            val startupAnimationUri = uiState.customStartupAnimationUri
            val clickSoundUri = uiState.customClickSoundUri
            val clickSoundVolume = uiState.customClickSoundVolume
            val backgroundMusicUri = uiState.customBackgroundMusicUri
            val backgroundMusicVolume = uiState.customBackgroundMusicVolume
            var showStartupAnimation by rememberSaveable { mutableStateOf(!startupAnimationUri.isNullOrBlank()) }
            val effectiveEnableBlur = if (isLiquidGlassInterface) {
                false
            } else {
                uiState.enableBlur
            }
            val effectiveEnableFloatingBottomBarBlur = if (isLiquidGlassInterface) {
                false
            } else {
                uiState.enableFloatingBottomBarBlur
            }
            val darkMode = appSettings.colorMode.isDark ||
                (appSettings.colorMode.isSystem && isSystemInDarkTheme())

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }

            LaunchedEffect(clickSoundUri) {
                if (clickSoundUri.isNullOrBlank()) {
                    ClickSoundPlayer.release()
                }
            }

            LaunchedEffect(backgroundMusicUri, backgroundMusicVolume) {
                if (backgroundMusicUri.isNullOrBlank()) {
                    BackgroundMusicPlayer.stop()
                } else {
                    BackgroundMusicPlayer.play(this@MainActivity, backgroundMusicUri, backgroundMusicVolume)
                }
            }

            val navigator = rememberNavigator(Route.Main)
            val currentRoute = navigator.current() as? Route
            val uiDecorationScope = resolveUiDecorationScope(currentRoute, selectedMainPage)
            val systemDensity = LocalDensity.current
            val density = remember(systemDensity, uiState.pageScale, uiState.fontScale) {
                Density(
                    density = systemDensity.density * uiState.pageScale,
                    fontScale = systemDensity.fontScale * uiState.fontScale,
                )
            }
            val effectiveUiDecorationConfig = remember(uiState.uiDecorationConfig, uiState.interfaceStyle) {
                uiState.uiDecorationConfig.deduplicateNativePixelChrome(
                    pixelStyleActive = uiState.interfaceStyle == InterfaceStyle.Pixel.value,
                )
            }

            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalUiDecorationConfig provides effectiveUiDecorationConfig,
                LocalUiDecorationScope provides uiDecorationScope,
                LocalDensity provides density,
                LocalColorMode provides appSettings.colorMode.value,
                LocalEnableBlur provides effectiveEnableBlur,
                LocalBlurIntensity provides uiState.blurIntensity,
                LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
                LocalEnableFloatingBottomBarBlur provides effectiveEnableFloatingBottomBarBlur,
                LocalAutoHideNavigationBar provides uiState.autoHideNavigationBar,
                LocalScrollHideNavigationBar provides uiState.scrollHideNavigationBar,
                LocalUiMode provides uiMode,
                LocalInterfaceStyle provides uiState.interfaceStyle,
                LocalSeasonStyle provides SeasonStyle.fromValue(uiState.seasonStyle),
                LocalPixelStyle provides PixelStyle.fromValue(uiState.pixelStyle),
                LocalDeltaColorVariant provides uiState.deltaColorVariant,
                LocalCustomNavigationIcons provides uiState.customNavigationIcons,
                LocalSwitchStyle provides SwitchStyle.fromValue(uiState.switchStyle),
                LocalNightBackgroundEffectActive provides (
                    darkMode &&
                        !uiState.nightBackgroundPassthrough &&
                        NightBackgroundEffect.fromValue(uiState.nightBackgroundEffect) != NightBackgroundEffect.Off
                    ),
                LocalScrollAnimation provides uiState.globalScrollEffectEnabled,
                LocalScrollAnimationEffect provides GlobalScrollEffect.fromValue(uiState.globalScrollEffect),
            ) {
                KernelSUTheme(appSettings = appSettings, uiMode = uiMode) {
                    HandleDeepLink(intentState = intentState.collectAsStateWithLifecycle())
                    ManagerUpdatePrompt()
                    ZipFileIntentHandler(intentState = intentState, isManager = managerReady)
                    ShortcutIntentHandler(intentState = intentState)
                    val mainScreenEntry = @Composable {
                        MainScreen(
                            initialPage = selectedMainPage,
                            onPageChanged = viewModel::setSelectedMainPage,
                        )
                    }

                    val navDisplay = @Composable {
                        NavDisplay(
                            modifier = Modifier.fillMaxSize(),
                            backStack = navigator.backStack,
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator()
                            ),
                            onBack = {
                                when (val top = navigator.current()) {
                                    is Route.TemplateEditor -> {
                                        if (!top.readOnly) {
                                            navigator.setResult("template_edit", true)
                                        } else {
                                            navigator.pop()
                                        }
                                    }

                                    else -> navigator.pop()
                                }
                            },
                            entryProvider = entryProvider {
                                entry<Route.Main> { mainScreenEntry() }
                                entry<Route.About> { AboutScreen() }
                                entry<Route.Sulog> { SulogScreen() }
                                entry<Route.ColorPalette> { ColorPaletteScreen() }
                                entry<Route.LauncherIcon> { LauncherIconScreen() }
                                entry<Route.NavigationIcons> { NavigationIconScreen() }
                                entry<Route.Backgrounds> { BackgroundSettingsScreen() }
                                entry<Route.SoundEffects> { SoundEffectsScreen() }
                                entry<Route.StartupAnimation> { StartupAnimationScreen() }
                                entry<Route.HomeCardWallpapers> { HomeCardWallpaperScreen() }
                                entry<Route.PreInstallStyleSettings> { PreInstallStyleSettingsScreen() }
                                entry<Route.VisualEffects> { VisualEffectsScreen() }
                                entry<Route.UiDecorationLibrary> { UiDecorationLibraryScreen() }
                                entry<Route.HiddenPathConfig> {
                                    if (!Natives.isLkmMode && !Natives.isLateLoadMode) {
                                        SusfsPathConfigScreen()
                                    } else {
                                        HiddenPathConfigScreen()
                                    }
                                }
                                entry<Route.SusfsPathConfig> { SusfsPathConfigScreen() }
                                entry<Route.AiChat> { AiChatScreen() }
                                entry<Route.AiModuleStudio> { AiModuleStudioScreen() }
                                entry<Route.RescueProtection> { RescueProtectionScreen() }
                                entry<Route.CpuSpoof> { CpuSpoofScreen() }
                                entry<Route.GraphicsRenderer> { GraphicsRendererScreen() }
                                entry<Route.BuiltinMount> { BuiltinMountScreen() }
                                entry<Route.ThemeStore> { ThemeStoreScreen() }
                                entry<Route.AppProfileTemplate> { AppProfileTemplateScreen() }
                                entry<Route.TemplateEditor> { key -> TemplateEditorScreen(key.template, key.readOnly) }
                                entry<Route.AppProfile> { key -> AppProfileScreen(key.uid) }
                                entry<Route.ModuleRepo> { ModuleRepoScreen() }
                                entry<Route.ModuleRepoDetail> { key -> ModuleRepoDetailScreen(key.module) }
                                entry<Route.Install> { InstallScreen() }
                                entry<Route.Flash> { key -> FlashScreen(key.flashIt) }
                                entry<Route.ExecuteModuleAction> { key -> ExecuteModuleActionScreen(key.moduleId, key.fromShortcut) }
                                entry<Route.Home> { mainScreenEntry() }
                                entry<Route.SuperUser> { mainScreenEntry() }
                                entry<Route.Module> { mainScreenEntry() }
                                entry<Route.Settings> { mainScreenEntry() }
                            },
                            transitionSpec = stableNavForwardTransition(),
                            popTransitionSpec = stableNavPopTransition(),
                            predictivePopTransitionSpec = { _ -> stableNavPopTransitionContentTransform() },
                            transitionEffects = NavDisplayTransitionEffects(
                                enableCornerClip = false,
                                dimAmount = 0f,
                                blockInputDuringTransition = true,
                                popDirectionFollowsSwipeEdge = true,
                            ),
                        )
                    }
                    val globalGlassBackdrop = rememberBlurBackdrop(effectiveEnableBlur)
                    var routeInitialized by remember { mutableStateOf(false) }
                    var navigationTransitionActive by remember { mutableStateOf(false) }
                    LaunchedEffect(currentRoute) {
                        if (!routeInitialized) {
                            routeInitialized = true
                        } else {
                            navigationTransitionActive = true
                            delay(NAV_TRANSITION_DURATION_MS.toLong())
                            navigationTransitionActive = false
                        }
                    }
                    val effectiveBackground = uiState.effectiveCustomBackground(selectedMainPage, currentRoute)
                    val seasonalStyleActive = uiState.interfaceStyle == InterfaceStyle.Snow.value
                    val pixelStyleActive = uiState.interfaceStyle == InterfaceStyle.Pixel.value
                    val hasCustomBackground =
                        !effectiveBackground.wallpaperUriString.isNullOrBlank() ||
                            !effectiveBackground.videoUriString.isNullOrBlank()
                    val immersiveBackgroundActive =
                        seasonalStyleActive ||
                            pixelStyleActive ||
                            hasCustomBackground ||
                            (
                                darkMode &&
                                    NightBackgroundEffect.fromValue(uiState.nightBackgroundEffect) !=
                                    NightBackgroundEffect.Off
                                )
                    val globalScrollEffectState = rememberGlobalScrollEffectState(
                        enabled = uiState.globalScrollEffectEnabled && !navigationTransitionActive,
                        effectValue = uiState.globalScrollEffect,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .globalScrollEffectController(globalScrollEffectState)
                    ) {
                        CustomWallpaperRoot(
                            uriString = effectiveBackground.wallpaperUriString,
                            videoUriString = effectiveBackground.videoUriString,
                            videoDurationSeconds = effectiveBackground.videoDurationSeconds,
                            opacity = effectiveBackground.opacity,
                            crop = effectiveBackground.crop,
                            passthroughEnabled = uiState.customWallpaperPassthroughEnabled,
                            passthroughOpacity = uiState.customWallpaperPassthroughOpacity,
                        ) {
                            CompositionLocalProvider(
                                LocalImmersiveBackgroundActive provides immersiveBackgroundActive,
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (seasonalStyleActive && !hasCustomBackground) {
                                        SeasonStyleWallpaper(modifier = Modifier.fillMaxSize())
                                        SeasonAmbientOverlay(modifier = Modifier.fillMaxSize())
                                    }
                                    if (pixelStyleActive && !hasCustomBackground) {
                                        PixelBackdrop(modifier = Modifier.fillMaxSize())
                                    }
                                    if (!uiState.nightBackgroundPassthrough) {
                                        NightBackgroundEffectOverlay(
                                            enabled = darkMode,
                                            effectValue = uiState.nightBackgroundEffect,
                                            passthrough = false,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    UiDecorationBackdrop(modifier = Modifier.fillMaxSize())
                                    CompositionLocalProvider(LocalLiquidGlassBackdrop provides globalGlassBackdrop) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .customClickSound(clickSoundUri, clickSoundVolume)
                                                .then(
                                                    if (globalGlassBackdrop != null) {
                                                        Modifier.layerBackdrop(globalGlassBackdrop)
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                        ) {
                                            when (uiMode) {
                                                UiMode.Material -> androidx.compose.material3.Scaffold(
                                                    containerColor = Color.Transparent
                                                ) { navDisplay() }

                                                UiMode.Miuix -> Scaffold(containerColor = Color.Transparent) { navDisplay() }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        UiDecorationChromeOverlay(modifier = Modifier.fillMaxSize())
                        SeasonChromeOverlay(modifier = Modifier.fillMaxSize())
                        PixelChromeOverlay(modifier = Modifier.fillMaxSize())

                        if (uiState.nightBackgroundPassthrough) {
                            NightBackgroundEffectOverlay(
                                enabled = darkMode,
                                effectValue = uiState.nightBackgroundEffect,
                                passthrough = true,
                                passthroughOpacity = uiState.nightBackgroundPassthroughOpacity,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        GlobalSnowEffectOverlay(
                            enabled = darkMode && uiState.globalSnowEnabled,
                            effectValue = uiState.globalSnowEffect,
                            modifier = Modifier.fillMaxSize(),
                        )

                        GlobalScrollEffectOverlay(
                            state = globalScrollEffectState,
                            modifier = Modifier.fillMaxSize(),
                        )

                        if (showStartupAnimation && !startupAnimationUri.isNullOrBlank()) {
                            StartupAnimationOverlay(
                                uriString = startupAnimationUri,
                                onFinished = { showStartupAnimation = false },
                                onError = { showStartupAnimation = false },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        StartupSoundPlayer.playConfigured(this)
        BackgroundMusicPlayer.playConfigured(this)
    }

    override fun onStop() {
        StartupSoundPlayer.stop()
        ClickSoundPlayer.release()
        BackgroundMusicPlayer.stop()
        super.onStop()
    }

    override fun onDestroy() {
        StartupSoundPlayer.stop()
        ClickSoundPlayer.release()
        BackgroundMusicPlayer.stop()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Increment intentState to trigger LaunchedEffect re-execution
        intentState.value += 1
    }
}

private fun MainActivityUiState.effectiveCustomBackground(
    mainPage: Int,
    currentRoute: Route?,
): CustomBackgroundState {
    val routeBackground = when (currentRoute) {
        Route.Install -> customPageBackgrounds[CustomPageBackgroundTarget.Install]
        else -> null
    }?.takeIf { it.hasMedia }
    if (routeBackground != null) {
        return routeBackground
    }

    val pageBackground = CustomPageBackgroundTarget.fromMainPageIndex(mainPage)
        ?.let { customPageBackgrounds[it] }
        ?.takeIf { it.hasMedia }
    if (pageBackground != null) {
        return pageBackground
    }

    return CustomBackgroundState(
        wallpaperUriString = customWallpaperUri,
        videoUriString = customVideoBackgroundUri,
        opacity = customWallpaperOpacity,
        crop = customWallpaperCrop,
        videoDurationSeconds = customVideoBackgroundDurationSeconds,
    )
}

@Composable
private fun ManagerUpdatePrompt() {
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<ManagerUpdateInfo?>(null) }
    val forceUpdate = updateInfo?.force == true
    val updateTitle = stringResource(
        if (forceUpdate) R.string.manager_force_update_title else R.string.manager_update_title
    )
    val downloadText = stringResource(R.string.download)
    val updateContent = updateInfo?.let { latest ->
        val version = stringResource(
            if (latest.force) R.string.manager_force_update_message else R.string.manager_update_message,
            latest.versionName,
            latest.versionCode,
        )
        val changelog = latest.changelog.trim().take(MAX_MANAGER_UPDATE_CHANGELOG_LENGTH)
        if (changelog.isBlank()) {
            version
        } else {
            stringResource(R.string.manager_update_changelog, version, changelog)
        }
    }
    val updateDialog = rememberConfirmDialog(
        onConfirm = {
            updateInfo?.let { ManagerUpdateChecker.download(context, it) }
        },
        onDismiss = {
            if (updateInfo?.force != true) {
                updateInfo = null
            }
        },
    )

    LaunchedEffect(updateInfo, updateContent, updateTitle, downloadText) {
        val latest = updateInfo ?: return@LaunchedEffect
        val content = updateContent ?: return@LaunchedEffect
        updateDialog.showConfirm(
            title = updateTitle,
            content = content,
            markdown = latest.changelog.isNotBlank(),
            confirm = downloadText,
            dismissible = !latest.force,
        )
    }

    LaunchedEffect(Unit) {
        val latest = ManagerUpdateChecker.checkLatest(context) ?: return@LaunchedEffect
        updateInfo = latest
    }
}

@Composable
private fun Modifier.customClickSound(uriString: String?, volume: Float): Modifier {
    if (uriString.isNullOrBlank()) return this
    val context = LocalContext.current.applicationContext
    return pointerInput(uriString, volume) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val startPosition = down.position
            var wasConsumed = down.isConsumed
            var moved = false
            var completed = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                wasConsumed = wasConsumed || change.isConsumed
                if ((change.position - startPosition).getDistance() > viewConfiguration.touchSlop) {
                    moved = true
                }
                if (!change.pressed) {
                    completed = true
                    break
                }
            }

            if (wasConsumed && completed && !moved) {
                ClickSoundPlayer.play(context, uriString, volume)
            }
        }
    }
}

private const val NAV_TRANSITION_DURATION_MS = 200
private const val MAX_MANAGER_UPDATE_CHANGELOG_LENGTH = 4000
private const val TAG = "MainActivity"

private fun <T : Any> stableNavForwardTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    stableNavForwardTransitionContentTransform()
}

private fun <T : Any> stableNavPopTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    stableNavPopTransitionContentTransform()
}

private fun stableNavForwardTransitionContentTransform(): ContentTransform {
    val alphaSpec = tween<Float>(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
    return ContentTransform(
        targetContentEnter = fadeIn(animationSpec = alphaSpec) +
            slideInHorizontally(
                animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
            ) { width -> width / 12 },
        initialContentExit = fadeOut(animationSpec = alphaSpec) +
            slideOutHorizontally(
                animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
            ) { width -> -width / 16 },
        targetContentZIndex = 1f,
        sizeTransform = null,
    )
}

private fun stableNavPopTransitionContentTransform(): ContentTransform {
    val alphaSpec = tween<Float>(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
    return ContentTransform(
        targetContentEnter = fadeIn(animationSpec = alphaSpec) +
            slideInHorizontally(
                animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
            ) { width -> -width / 12 },
        initialContentExit = fadeOut(animationSpec = alphaSpec) +
            slideOutHorizontally(
                animationSpec = tween(NAV_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
            ) { width -> width / 16 },
        targetContentZIndex = 1f,
        sizeTransform = null,
    )
}

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> { error("LocalMainPagerState not provided") }

private fun resolveUiDecorationScope(route: Route?, selectedMainPage: Int): UiDecorationScope {
    return when (route) {
        Route.Home -> UiDecorationScope.Home
        Route.SuperUser -> UiDecorationScope.SuperUser
        Route.Module -> UiDecorationScope.Modules
        Route.Settings -> UiDecorationScope.Settings
        Route.Main -> when (selectedMainPage) {
            0 -> UiDecorationScope.Home
            1 -> UiDecorationScope.SuperUser
            2 -> UiDecorationScope.Modules
            3 -> UiDecorationScope.Settings
            else -> UiDecorationScope.Secondary
        }
        else -> UiDecorationScope.Secondary
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
) {
    val navController = LocalNavigator.current
    val enableBlur = LocalEnableBlur.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val autoHideNavigationBar = LocalAutoHideNavigationBar.current
    val scrollHideNavigationBar = LocalScrollHideNavigationBar.current
    val refreshTick by KernelStatusEvents.refreshTick.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { MainPagerConfig.PAGE_COUNT })
    val mainPagerState = rememberMainPagerState(pagerState)
    val isFullFeatured by produceState(initialValue = false, refreshTick) {
        val fullFeatured = kotlinx.coroutines.withContext(Dispatchers.IO) {
            runCatching { Natives.refreshInfo() }
            val managerRegistered = runCatching {
                Natives.isManager || ensureManagerRegistered()
            }.getOrDefault(false)
            runCatching {
                val driverVersion = Natives.version.takeIf { it > 0 }
                val requiresNewKernel = Natives.requireNewKernel()
                val uapiMismatch = Natives.checkUAPIMismatch()
                managerRegistered &&
                    driverVersion != null &&
                    ksuRootAvailable() &&
                    !hasBlockingRootVersionMismatch(
                        managerVersionCode = BuildConfig.VERSION_CODE.toLong(),
                        driverVersion = driverVersion,
                        requiresNewKernel = requiresNewKernel,
                        uapiMismatch = uapiMismatch,
                    )
            }.getOrDefault(false)
        }
        value = fullFeatured
    }
    val userScrollEnabled = isFullFeatured
    val uiMode = LocalUiMode.current
    val surfaceColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surface // Blur is not used in Material, this is just a placeholder
        UiMode.Miuix -> liquidGlassBackdropColor()
    }
    val blurBackdrop = rememberBlurBackdrop(enableBlur)
    val floatingBarBackdrop = if (enableFloatingBottomBar && enableFloatingBottomBarBlur) {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    } else {
        null
    }

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        onPageChanged(settledPage)
    }

    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }

    LaunchedEffect(isFullFeatured) {
        mainPagerState.updateFeatureAvailability(isFullFeatured)
    }

    MainScreenBackHandler(mainPagerState, navController)

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val useNavigationRail = isLandscape && !(uiMode == UiMode.Miuix && enableFloatingBottomBar)
    val navigationBarVisibilityState = rememberNavigationBarVisibilityState(
        enabled = !useNavigationRail && (autoHideNavigationBar || scrollHideNavigationBar),
        autoHideAfterInactivity = autoHideNavigationBar,
        hideOnScroll = scrollHideNavigationBar,
    )

    LaunchedEffect(isFullFeatured) {
        if (isFullFeatured) {
            navigationBarVisibilityState.reveal(resetIdleTimer = true)
        }
    }

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState,
        LocalLiquidGlassBackdrop provides blurBackdrop,
    ) {
        val contentReady = rememberContentReady()
        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            Box(modifier = if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier) {
                HorizontalPager(
                    modifier = Modifier
                        .then(if (floatingBarBackdrop != null) Modifier.layerBackdrop(floatingBarBackdrop) else Modifier),
                    state = mainPagerState.pagerState,
                    beyondViewportPageCount = if (contentReady) 3 else 0,
                    userScrollEnabled = userScrollEnabled,
                ) { page ->
                    val isCurrentPage = page == settledPage
                    when (page) {
                        0 -> if (isCurrentPage || contentReady) HomePager(navController, bottomInnerPadding, isCurrentPage)
                        1 -> if (isCurrentPage || contentReady) SuperUserPager(navController, bottomInnerPadding, isCurrentPage)
                        2 -> if (isCurrentPage || contentReady) ModulePager(bottomInnerPadding, isCurrentPage)
                        3 -> if (isCurrentPage || contentReady) SettingPager(navController, bottomInnerPadding)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarVisibilityController(navigationBarVisibilityState),
        ) {
        if (useNavigationRail) {
            val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Start)
            val navBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(containerColor = Color.Transparent) {
                    Row {
                        SideRail(
                            blurBackdrop = blurBackdrop,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }

                UiMode.Miuix -> Scaffold(containerColor = Color.Transparent) { _ ->
                    Row {
                        SideRail(
                            blurBackdrop = blurBackdrop,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }
            }
        } else {
            val bottomBar = @Composable {
                AutoHidingNavigationBar(
                    visible = navigationBarVisibilityState.visible,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        BottomBar(
                            blurBackdrop = blurBackdrop,
                            backdrop = floatingBarBackdrop,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(
                    bottomBar = bottomBar,
                    containerColor = Color.Transparent,
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }

                UiMode.Miuix -> Scaffold(
                    bottomBar = bottomBar,
                    containerColor = Color.Transparent,
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }
            }
        }
        }
    }
}


@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navController: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navController.current() is Route.Main && navController.backStackSize() == 1 && mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        }
    )
}

/**
 * Handles ZIP file installation from external apps (e.g., file managers).
 * - In normal mode: Shows a confirmation dialog before installation
 * - In safe mode: Shows a Toast notification and prevents installation
 */
@SuppressLint("StringFormatInvalid", "LocalContextGetResourceValueCall")
@Composable
private fun ZipFileIntentHandler(
    intentState: MutableStateFlow<Int>,
    isManager: Boolean,
) {
    val activity = LocalActivity.current ?: return
    val context = LocalContext.current
    var zipUri by remember { mutableStateOf<Uri?>(null) }
    var isAnyKernel by remember { mutableStateOf(false) }
    val isSafeMode = runCatching { Natives.isSafeMode }.getOrDefault(false)
    val clearZipUri = {
        zipUri = null
        isAnyKernel = false
    }
    val navigator = LocalNavigator.current

    val installDialog = rememberConfirmDialog(
        onConfirm = {
            zipUri?.let { uri ->
                val flashIt = if (isAnyKernel) {
                    FlashIt.FlashAnyKernel(uri)
                } else {
                    FlashIt.FlashModules(listOf(uri))
                }
                navigator.push(Route.Flash(flashIt))
            }
            clearZipUri()
        },
        onDismiss = clearZipUri
    )

    fun getDisplayName(uri: Uri): String {
        return uri.getFileName(context) ?: uri.lastPathSegment ?: "Unknown"
    }

    val intentStateValue by intentState.collectAsStateWithLifecycle()
    LaunchedEffect(intentStateValue, isManager) {
        val currentIntent = activity.intent
        val uri = currentIntent?.data ?: return@LaunchedEffect

        val supportedScheme = uri.scheme == "content" || uri.scheme == "file"
        val component = currentIntent.component?.className.orEmpty()
        val isAnyKernelIntent = component.endsWith("FlashAnyKernel")
        if (!isManager || !supportedScheme || currentIntent.type != "application/zip") {
            return@LaunchedEffect
        }

        activity.intent.data = null
        activity.intent.type = null

        if (isSafeMode) {
            Toast.makeText(context, context.getString(R.string.safe_mode_module_disabled), Toast.LENGTH_SHORT).show()
        } else {
            zipUri = uri
            isAnyKernel = isAnyKernelIntent
            installDialog.showConfirm(
                title = if (isAnyKernelIntent) {
                    context.getString(R.string.anykernel_install)
                } else {
                    context.getString(R.string.module)
                },
                content = context.getString(
                    R.string.module_install_prompt_with_name,
                    "\n${getDisplayName(uri)}"
                )
            )
        }
    }
}

@Composable
private fun ShortcutIntentHandler(
    intentState: MutableStateFlow<Int>,
) {
    val activity = LocalActivity.current ?: return
    val context = LocalContext.current
    val intentStateValue by intentState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    LaunchedEffect(intentStateValue) {
        val intent = activity.intent
        val type = intent?.getStringExtra("shortcut_type") ?: return@LaunchedEffect

        when (type) {
            "module_action" -> {
                val moduleId = intent.getStringExtra("module_id") ?: return@LaunchedEffect
                navigator.push(Route.ExecuteModuleAction(moduleId, fromShortcut = true))
                intent.removeExtra("shortcut_type")
                intent.removeExtra("module_id")
            }

            "module_webui" -> {
                val moduleId = intent.getStringExtra("module_id") ?: return@LaunchedEffect
                val webIntent = Intent(context, WebUIActivity::class.java)
                    .setData("kernelsu://webui/$moduleId".toUri())
                    .putExtra("id", moduleId)
                context.startActivity(webIntent)
                intent.removeExtra("shortcut_type")
                intent.removeExtra("module_id")
            }

            else -> return@LaunchedEffect
        }
    }
}
