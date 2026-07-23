package me.weishu.kernelsu.ui.theme

import me.weishu.kernelsu.ui.InterfaceStyle

object ThemeAppearanceDefaults {
    const val PAGE_SCALE = 1.0f
    const val FONT_SCALE = 1.0f
    const val BLUR_INTENSITY = 1.0f
}

data class ThemeAppearanceSnapshot(
    val colorMode: Int,
    val miuixMonet: Boolean,
    val keyColor: Int,
    val colorStyle: String,
    val colorSpec: String,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val pageScale: Float,
    val fontScale: Float,
    val blurIntensity: Float,
)

data class CustomThemePreset(
    val id: String,
    val name: String,
    val uiMode: String,
    val updatedAt: Long,
    val snapshot: ThemeAppearanceSnapshot,
)

enum class ThemeSyncStrategy(val value: String) {
    SHARED("shared"),
    PER_STYLE("per_style");

    companion object {
        fun fromValue(value: String?): ThemeSyncStrategy {
            return entries.firstOrNull { it.value == value } ?: SHARED
        }
    }
}

const val THEME_SYNC_STRATEGY_KEY = "theme_sync_strategy"

val ThemePreferenceKeys = listOf(
    "color_mode",
    "miuix_monet",
    "key_color",
    "color_style",
    "color_spec",
    "theme_preset",
    "enable_blur",
    "enable_floating_bottom_bar",
    "enable_floating_bottom_bar_blur",
    "page_scale",
    "font_scale",
    "blur_intensity",
)

fun themePreferenceKey(
    base: String,
    strategy: ThemeSyncStrategy,
    style: String,
): String {
    return if (strategy == ThemeSyncStrategy.PER_STYLE) {
        "${base}_${style}"
    } else {
        base
    }
}

fun defaultThemePresetForUiMode(uiMode: String): ThemePreset {
    return when (uiMode) {
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
}
