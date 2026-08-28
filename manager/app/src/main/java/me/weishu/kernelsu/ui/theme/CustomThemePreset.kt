package me.weishu.kernelsu.ui.theme

import me.weishu.kernelsu.ui.InterfaceStyle

object ThemeAppearanceDefaults {
    const val PAGE_SCALE = 1.0f
    const val FONT_SCALE = 1.0f
    const val BLUR_INTENSITY = 1.0f
    const val MONET_SURFACE_OPACITY = 1.0f
    const val MIN_MONET_SURFACE_OPACITY = 0.45f
    const val MAX_MONET_SURFACE_OPACITY = 1.0f
}

fun sanitizeMonetSurfaceOpacity(value: Float): Float {
    return if (value.isFinite()) {
        value.coerceIn(
            ThemeAppearanceDefaults.MIN_MONET_SURFACE_OPACITY,
            ThemeAppearanceDefaults.MAX_MONET_SURFACE_OPACITY,
        )
    } else {
        ThemeAppearanceDefaults.MONET_SURFACE_OPACITY
    }
}

data class ThemeAppearanceSnapshot(
    val colorMode: Int,
    val miuixMonet: Boolean,
    val keyColor: Int,
    val colorStyle: String,
    val colorSpec: String,
    val monetSurfaceOpacity: Float,
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
    "monet_surface_opacity",
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
