package me.weishu.kernelsu.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.pixel.LocalPixelStyle
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.rain.LocalRainStyle
import me.weishu.kernelsu.ui.component.rain.RainStyle
import me.weishu.kernelsu.ui.component.rain.forceRainDarkTheme
import me.weishu.kernelsu.ui.util.AppFontState
import me.weishu.kernelsu.ui.util.readAppFontState

enum class ColorMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
    MONET_SYSTEM(3),
    MONET_LIGHT(4),
    MONET_DARK(5),
    DARK_AMOLED(6);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: SYSTEM
    }

    val isSystem: Boolean get() = value == 0 || value == 3
    val isDark: Boolean get() = value == 2 || value == 5 || value == 6
    val isAmoled: Boolean get() = value == 6
    val isMonet: Boolean get() = this == MONET_SYSTEM || this == MONET_LIGHT || this == MONET_DARK

    fun toNonMonetMode(): Int = when (this) {
        MONET_SYSTEM -> 0
        MONET_LIGHT -> 1
        MONET_DARK -> 2
        else -> value
    }

    fun toMonetMode(): Int = when (this) {
        SYSTEM -> 3
        LIGHT -> 4
        DARK, DARK_AMOLED -> 5
        else -> value
    }
}

data class AppSettings(
    val colorMode: ColorMode,
    val keyColor: Int,
    val paletteStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
)

object ThemeController {
    fun getAppSettings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val storedUiMode = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE)
        val uiMode = InterfaceStyle.normalizeValue(storedUiMode)
        if (storedUiMode != uiMode) {
            prefs.edit().putString("ui_mode", uiMode).apply()
        }
        val defaultPreset = when (uiMode) {
            InterfaceStyle.Studio.value -> ThemePreset.STUDIO
            InterfaceStyle.Skrootpro.value -> ThemePreset.SKROOTPRO
            InterfaceStyle.Alpha.value -> ThemePreset.ALPHA
            InterfaceStyle.Delta.value -> ThemePreset.DELTA
            InterfaceStyle.LiquidGlass.value -> ThemePreset.LIQUID_GLASS
            InterfaceStyle.Snow.value -> ThemePreset.SNOW
            InterfaceStyle.Rain.value -> ThemePreset.RAIN
            InterfaceStyle.Pixel.value -> ThemePreset.PIXEL
            else -> ThemePreset.CLEAN_TOOL
        }
        val syncStrategy = ThemeSyncStrategy.fromValue(
            prefs.getString(THEME_SYNC_STRATEGY_KEY, ThemeSyncStrategy.SHARED.value)
        )
        fun key(base: String) = themePreferenceKey(base, syncStrategy, uiMode)

        var colorModeValue = prefs.getInt(key("color_mode"), defaultPreset.colorMode.value)

        if (InterfaceStyle.isMiuixBased(uiMode)) {
            val miuixMonet = prefs.getBoolean(key("miuix_monet"), false)
            val colorMode = ColorMode.fromValue(colorModeValue)
            colorModeValue = if (!miuixMonet && colorMode.isMonet) {
                colorMode.toNonMonetMode()
            } else if (miuixMonet && !colorMode.isMonet) {
                colorMode.toMonetMode()
            } else {
                colorModeValue
            }
        }

        val colorMode = ColorMode.fromValue(colorModeValue)
        val keyColor = prefs.getInt(key("key_color"), defaultPreset.keyColor)
        val paletteStyleStr = prefs.getString(key("color_style"), defaultPreset.paletteStyle.name)
        val paletteStyle = try {
            PaletteStyle.valueOf(paletteStyleStr!!)
        } catch (_: Exception) {
            defaultPreset.paletteStyle
        }
        val colorSpecStr = prefs.getString(key("color_spec"), defaultPreset.colorSpec.name)
        val colorSpec = try {
            ColorSpec.SpecVersion.valueOf(colorSpecStr!!)
        } catch (_: Exception) {
            defaultPreset.colorSpec
        }

        return AppSettings(colorMode, keyColor, paletteStyle, colorSpec)
    }
}

@Composable
fun KernelSUTheme(
    appSettings: AppSettings? = null,
    appFontState: AppFontState? = null,
    uiMode: UiMode = LocalUiMode.current,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentAppSettings = appSettings ?: ThemeController.getAppSettings(context)
    val currentAppFontState = appFontState ?: readAppFontState(context)

    MiuixKernelSUTheme(
        appSettings = currentAppSettings,
        appFontState = currentAppFontState,
        content = content
    )
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean {
    if (
        LocalInterfaceStyle.current == InterfaceStyle.Rain.value &&
        forceRainDarkTheme(LocalRainStyle.current)
    ) {
        return true
    }
    if (
        LocalInterfaceStyle.current == InterfaceStyle.Pixel.value &&
        LocalPixelStyle.current == PixelStyle.CyberHacker
    ) {
        return true
    }
    return when (LocalColorMode.current) {
        1, 4 -> false  // Force light mode
        2, 5, 6 -> true   // Force dark mode
        else -> isSystemInDarkTheme()  // Follow system (0 or default)
    }
}


val LocalColorMode = staticCompositionLocalOf { 0 }

val LocalEnableBlur = staticCompositionLocalOf { false }

val LocalBlurIntensity = staticCompositionLocalOf { ThemeAppearanceDefaults.BLUR_INTENSITY }

val LocalEnableFloatingBottomBar = staticCompositionLocalOf { false }

val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { false }

val LocalAutoHideNavigationBar = staticCompositionLocalOf { false }

val LocalScrollHideNavigationBar = staticCompositionLocalOf { false }
