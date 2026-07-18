package me.weishu.kernelsu.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.pixel.LocalPixelStyle
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.webui.MonetColorsProvider
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
fun MiuixKernelSUTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val isLiquidGlass = LocalInterfaceStyle.current == InterfaceStyle.LiquidGlass.value
    val forcePixelCyberDark =
        LocalInterfaceStyle.current == InterfaceStyle.Pixel.value &&
            LocalPixelStyle.current == PixelStyle.CyberHacker
    val darkTheme = forcePixelCyberDark ||
        appSettings.colorMode.isDark ||
        (appSettings.colorMode.isSystem && systemDarkTheme)
    val colorStyle = appSettings.paletteStyle
    val colorSpec = appSettings.colorSpec
    val materialColorScheme = rememberAppColorScheme(
        appSettings = appSettings,
        forceDark = forcePixelCyberDark,
    )

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
        if (forcePixelCyberDark) {
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
            isLiquidGlass -> Color(0xFFE7F1FF)
            appSettings.keyColor == 0 -> null
            else -> Color(appSettings.keyColor)
        },
        isDark = darkTheme,
        paletteStyle = miuixPaletteStyle,
        colorSpec = miuixColorSpec,
    )

    MiuixTheme(
        controller = controller,
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
                motionScheme = MotionScheme.expressive(),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MiuixTheme.colorScheme.onBackground,
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
