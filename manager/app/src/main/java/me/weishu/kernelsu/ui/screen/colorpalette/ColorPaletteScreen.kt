package me.weishu.kernelsu.ui.screen.colorpalette

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.weishu.kernelsu.KernelSUApplication
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.ColorMode
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel

@Composable
fun ColorPaletteScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var predictiveBackUpdatePending by remember { mutableStateOf(false) }
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPaletteStyle = try {
        PaletteStyle.valueOf(uiState.colorStyle)
    } catch (_: Exception) {
        PaletteStyle.TonalSpot
    }
    val currentColorSpec = try {
        ColorSpec.SpecVersion.valueOf(uiState.colorSpec)
    } catch (_: Exception) {
        ColorSpec.SpecVersion.Default
    }
    val state = ColorPaletteUiState(
        uiState = uiState,
        currentColorMode = ColorMode.fromValue(uiState.themeMode),
        currentPaletteStyle = currentPaletteStyle,
        currentColorSpec = currentColorSpec,
        predictiveBackUpdatePending = predictiveBackUpdatePending,
    )
    val actions = ColorPaletteScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onSetThemeMode = viewModel::setThemeMode,
        onSetMiuixMonet = viewModel::setMiuixMonet,
        onSetKeyColor = viewModel::setKeyColor,
        onSetColorMode = viewModel::setColorMode,
        onSetColorStyle = viewModel::setColorStyle,
        onSetColorSpec = viewModel::setColorSpec,
        onSetMonetSurfaceOpacity = viewModel::setMonetSurfaceOpacity,
        onApplyThemePreset = viewModel::applyThemePreset,
        onSetEnableBlur = viewModel::setEnableBlur,
        onSetEnableFloatingBottomBar = viewModel::setEnableFloatingBottomBar,
        onSetEnableFloatingBottomBarBlur = viewModel::setEnableFloatingBottomBarBlur,
        onSetAutoHideNavigationBar = viewModel::setAutoHideNavigationBar,
        onSetScrollHideNavigationBar = viewModel::setScrollHideNavigationBar,
        onSetModuleTopBarAutoHideEnabled = viewModel::setModuleTopBarAutoHideEnabled,
        onSetEnablePredictiveBack = predictiveBack@{
            if (predictiveBackUpdatePending || it == uiState.enablePredictiveBack) {
                return@predictiveBack
            }
            predictiveBackUpdatePending = true
            if (!KernelSUApplication.setEnableOnBackInvokedCallback(context.applicationInfo, it)) {
                predictiveBackUpdatePending = false
                android.widget.Toast.makeText(
                    context,
                    R.string.settings_predictive_back_apply_failed,
                    android.widget.Toast.LENGTH_LONG,
                ).show()
                return@predictiveBack
            }
            viewModel.setEnablePredictiveBack(it)
            scope.launch {
                delay(220)
                val currentActivity = activity
                if (currentActivity == null || currentActivity.isFinishing || currentActivity.isDestroyed) {
                    predictiveBackUpdatePending = false
                } else {
                    currentActivity.recreate()
                }
            }
        },
        onSetPageScale = viewModel::setPageScale,
        onSetFontScale = viewModel::setFontScale,
        onSetBlurIntensity = viewModel::setBlurIntensity,
        onSaveCustomThemePreset = viewModel::saveCustomThemePreset,
        onApplyCustomThemePreset = viewModel::applyCustomThemePreset,
        onRenameCustomThemePreset = viewModel::renameCustomThemePreset,
        onDeleteCustomThemePreset = viewModel::deleteCustomThemePreset,
        onSetThemeSyncStrategy = viewModel::setThemeSyncStrategy,
        onResetThemeToDefault = viewModel::resetThemeToDefault,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> ColorPaletteScreenMiuix(state, actions)
        UiMode.Material -> ColorPaletteScreenMaterial(state, actions)
    }
}
