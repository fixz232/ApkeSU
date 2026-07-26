package me.weishu.kernelsu.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.pixel.LocalPixelStyle
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.rain.LocalRainStyle
import me.weishu.kernelsu.ui.component.rain.forceRainDarkTheme
import me.weishu.kernelsu.ui.util.AppFontState
import me.weishu.kernelsu.ui.util.resolveAppFontFamily
import me.weishu.kernelsu.ui.webui.MonetColorsProvider
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.defaultTextStyles

@Composable
fun MiuixKernelSUTheme(
    appSettings: AppSettings,
    appFontState: AppFontState,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val isLiquidGlass = LocalInterfaceStyle.current == InterfaceStyle.LiquidGlass.value
    val forcePixelCyberDark =
        LocalInterfaceStyle.current == InterfaceStyle.Pixel.value &&
            LocalPixelStyle.current == PixelStyle.CyberHacker
    val forceRainDark =
        LocalInterfaceStyle.current == InterfaceStyle.Rain.value &&
            forceRainDarkTheme(LocalRainStyle.current)
    val forceInterfaceDark = forcePixelCyberDark || forceRainDark
    val darkTheme = forceInterfaceDark ||
        appSettings.colorMode.isDark ||
        (appSettings.colorMode.isSystem && systemDarkTheme)
    val colorStyle = appSettings.paletteStyle
    val colorSpec = appSettings.colorSpec
    val materialColorScheme = rememberAppColorScheme(
        appSettings = appSettings,
        forceDark = forceInterfaceDark,
    )
    val appFontFamily = remember(appFontState) {
        resolveAppFontFamily(context, appFontState)
    }
    val materialTypography = remember(appFontFamily) {
        Typography(fontFamily = appFontFamily)
    }
    val miuixTextStyles = remember(appFontFamily) {
        val defaults = defaultTextStyles()
        defaults.copy(
            main = defaults.main.copy(fontFamily = appFontFamily),
            paragraph = defaults.paragraph.copy(fontFamily = appFontFamily),
            body1 = defaults.body1.copy(fontFamily = appFontFamily),
            body2 = defaults.body2.copy(fontFamily = appFontFamily),
            button = defaults.button.copy(fontFamily = appFontFamily),
            footnote1 = defaults.footnote1.copy(fontFamily = appFontFamily),
            footnote2 = defaults.footnote2.copy(fontFamily = appFontFamily),
            headline1 = defaults.headline1.copy(fontFamily = appFontFamily),
            headline2 = defaults.headline2.copy(fontFamily = appFontFamily),
            subtitle = defaults.subtitle.copy(fontFamily = appFontFamily),
            title1 = defaults.title1.copy(fontFamily = appFontFamily),
            title2 = defaults.title2.copy(fontFamily = appFontFamily),
            title3 = defaults.title3.copy(fontFamily = appFontFamily),
            title4 = defaults.title4.copy(fontFamily = appFontFamily),
        )
    }

    val miuixPaletteStyle = try {
        ThemePaletteStyle.valueOf(colorStyle.name)
    } catch (_: Exception) {
        ThemePaletteStyle.TonalSpot
    }

    val miuixColorSpec = if (colorSpec == ColorSpec.SpecVersion.SPEC_2025) {
        ThemeColorSpec.Spec2025
    } else {
        ThemeColorSpec.Spec2021
    }

    val controller = ThemeController(
        if (forceInterfaceDark) {
            ColorSchemeMode.Dark
        } else {
            when (appSettings.colorMode) {
                ColorMode.SYSTEM -> ColorSchemeMode.System
                ColorMode.LIGHT -> ColorSchemeMode.Light
                ColorMode.DARK, ColorMode.DARK_AMOLED -> ColorSchemeMode.Dark
                ColorMode.MONET_SYSTEM -> ColorSchemeMode.MonetSystem
                ColorMode.MONET_LIGHT -> ColorSchemeMode.MonetLight
                ColorMode.MONET_DARK -> ColorSchemeMode.MonetDark
            }
        },
        keyColor = when {
            isLiquidGlass -> Color(0xFF58758A)
            appSettings.keyColor == 0 -> null
            else -> Color(appSettings.keyColor)
        },
        isDark = darkTheme,
        paletteStyle = miuixPaletteStyle,
        colorSpec = miuixColorSpec,
    )

    MiuixTheme(
        controller = controller,
        textStyles = miuixTextStyles,
        content = {
            LaunchedEffect(darkTheme) {
                val window = (context as? Activity)?.window ?: return@LaunchedEffect
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            MonetColorsProvider.UpdateCss()
            MaterialExpressiveTheme(
                colorScheme = materialColorScheme,
                typography = materialTypography,
                motionScheme = MotionScheme.expressive(),
            ) {
                val fontContentColor = MiuixTheme.colorScheme.onBackground.copy(
                    alpha = appFontState.opacity,
                )
                CompositionLocalProvider(
                    LocalContentColor provides fontContentColor,
                    MaterialLocalContentColor provides fontContentColor,
                ) {
                    content()
                }
            }
        }
    )
}

@Composable
private fun rememberAppColorScheme(
    appSettings: AppSettings,
    forceDark: Boolean = false,
): ColorScheme {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = forceDark || appSettings.colorMode.isDark ||
        (appSettings.colorMode.isSystem && systemDarkTheme)

    return if (appSettings.keyColor == 0) {
        val baseScheme = if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        rememberDynamicColorScheme(
            seedColor = Color.Unspecified,
            isDark = darkTheme,
            isAmoled = appSettings.colorMode.isAmoled,
            style = appSettings.paletteStyle,
            specVersion = appSettings.colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error,
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = Color(appSettings.keyColor),
            isDark = darkTheme,
            isAmoled = appSettings.colorMode.isAmoled,
            style = appSettings.paletteStyle,
            specVersion = appSettings.colorSpec,
        )
    }
}
