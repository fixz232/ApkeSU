package me.weishu.kernelsu.ui.theme

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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.pixel.LocalPixelStyle
import me.weishu.kernelsu.ui.component.rain.LocalRainStyle
import me.weishu.kernelsu.ui.component.rain.rainPalette
import me.weishu.kernelsu.ui.util.AppFontState
import me.weishu.kernelsu.ui.util.resolveAppFontFamily
import me.weishu.kernelsu.ui.webui.MonetColorsProvider
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
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
    val isRainStyle = LocalInterfaceStyle.current == InterfaceStyle.Rain.value
    val rainStyle = LocalRainStyle.current
    val forceInterfaceDark = isInterfaceForcedDark(
        interfaceStyle = LocalInterfaceStyle.current,
        rainStyle = LocalRainStyle.current,
        pixelStyle = LocalPixelStyle.current,
    )
    val darkTheme = resolveEffectiveDarkMode(
        colorMode = appSettings.colorMode,
        systemDark = systemDarkTheme,
        interfaceStyle = LocalInterfaceStyle.current,
        rainStyle = LocalRainStyle.current,
        pixelStyle = LocalPixelStyle.current,
    )
    val colorStyle = appSettings.paletteStyle
    val colorSpec = appSettings.colorSpec
    val materialColorScheme = rememberAppColorScheme(
        appSettings = appSettings,
        forceDark = forceInterfaceDark,
    )
    val rainColors = remember(isRainStyle, rainStyle, darkTheme) {
        if (isRainStyle) rainPalette(rainStyle, darkTheme) else null
    }
    val effectiveMaterialColorScheme = remember(materialColorScheme, rainColors, darkTheme) {
        when {
            rainColors != null -> {
                val secondaryText = if (darkTheme) Color(0xFFB8C6CE) else Color(0xFF435D68)
                materialColorScheme.copy(
                    onBackground = rainColors.content,
                    onSurface = rainColors.content,
                    onSurfaceVariant = secondaryText,
                    outline = rainColors.outline,
                )
            }

            else -> materialColorScheme
        }
    }
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
            val baseMiuixColors = MiuixTheme.colorScheme
            val effectiveMiuixColors = remember(
                baseMiuixColors,
                appSettings.colorMode,
                appSettings.monetSurfaceOpacity,
                rainColors,
                darkTheme,
            ) {
                val base = if (appSettings.colorMode.isMonet) {
                    baseMiuixColors.withMonetSurfaceOpacity(appSettings.monetSurfaceOpacity)
                } else {
                    baseMiuixColors
                }
                when {
                    rainColors != null -> base.withRainTextColors(
                        content = rainColors.content,
                        secondary = if (darkTheme) Color(0xFFB8C6CE) else Color(0xFF435D68),
                        outline = rainColors.outline,
                    )

                    else -> base
                }
            }
            MiuixTheme(
                colors = effectiveMiuixColors,
                textStyles = miuixTextStyles,
            ) {
                MaterialExpressiveTheme(
                    colorScheme = effectiveMaterialColorScheme,
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
                        MonetColorsProvider.UpdateCss()
                        content()
                    }
                }
            }
        }
    )
}

private fun Colors.withRainTextColors(
    content: Color,
    secondary: Color,
    outline: Color,
): Colors = copy(
    onBackground = content,
    onBackgroundVariant = secondary,
    onSurface = content,
    onSurfaceSecondary = secondary,
    onSurfaceVariantSummary = secondary,
    onSurfaceVariantActions = content,
    onSurfaceContainer = content,
    onSurfaceContainerVariant = secondary,
    onSurfaceContainerHigh = content,
    onSurfaceContainerHighest = content,
    outline = outline,
)

@Composable
private fun rememberAppColorScheme(
    appSettings: AppSettings,
    forceDark: Boolean = false,
): ColorScheme {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = forceDark || appSettings.colorMode.isDark ||
        (appSettings.colorMode.isSystem && systemDarkTheme)

    val baseScheme = if (appSettings.keyColor == 0) {
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
    return remember(baseScheme, appSettings.colorMode, appSettings.monetSurfaceOpacity) {
        if (appSettings.colorMode.isMonet) {
            baseScheme.withMonetSurfaceOpacity(appSettings.monetSurfaceOpacity)
        } else {
            baseScheme
        }
    }
}

private fun ColorScheme.withMonetSurfaceOpacity(opacity: Float): ColorScheme {
    val safeOpacity = sanitizeMonetSurfaceOpacity(opacity)
    if (safeOpacity >= 1f) return this
    fun Color.withOpacity() = copy(alpha = safeOpacity)
    return copy(
        surface = surface.withOpacity(),
        surfaceDim = surfaceDim.withOpacity(),
        surfaceBright = surfaceBright.withOpacity(),
        surfaceContainerLowest = surfaceContainerLowest.withOpacity(),
        surfaceContainerLow = surfaceContainerLow.withOpacity(),
        surfaceContainer = surfaceContainer.withOpacity(),
        surfaceContainerHigh = surfaceContainerHigh.withOpacity(),
        surfaceContainerHighest = surfaceContainerHighest.withOpacity(),
        surfaceVariant = surfaceVariant.withOpacity(),
    )
}

private fun Colors.withMonetSurfaceOpacity(opacity: Float): Colors {
    val safeOpacity = sanitizeMonetSurfaceOpacity(opacity)
    if (safeOpacity >= 1f) return this
    fun Color.withOpacity() = copy(alpha = safeOpacity)
    return copy(
        surface = surface.withOpacity(),
        surfaceVariant = surfaceVariant.withOpacity(),
        surfaceContainer = surfaceContainer.withOpacity(),
        surfaceContainerHigh = surfaceContainerHigh.withOpacity(),
        surfaceContainerHighest = surfaceContainerHighest.withOpacity(),
    )
}
