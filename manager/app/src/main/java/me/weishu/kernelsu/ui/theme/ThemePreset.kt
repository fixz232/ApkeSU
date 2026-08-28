package me.weishu.kernelsu.ui.theme

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.weishu.kernelsu.ui.InterfaceStyle

enum class ThemePreset(
    val value: String,
    val titleRes: Int,
    val summaryRes: Int,
    val colorMode: ColorMode,
    val keyColor: Int,
    val paletteStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
    val miuixMonet: Boolean,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val pageScale: Float,
    val monetSurfaceOpacity: Float = ThemeAppearanceDefaults.MONET_SURFACE_OPACITY,
) {
    CUSTOM(
        value = "custom",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_custom,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_custom_summary,
        colorMode = ColorMode.SYSTEM,
        keyColor = 0,
        paletteStyle = PaletteStyle.TonalSpot,
        colorSpec = ColorSpec.SpecVersion.Default,
        miuixMonet = false,
        enableBlur = false,
        enableFloatingBottomBar = false,
        enableFloatingBottomBarBlur = false,
        pageScale = 1.0f,
    ),

    CLEAN_TOOL(
        value = "clean_tool",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_clean_tool,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_clean_tool_summary,
        colorMode = ColorMode.SYSTEM,
        keyColor = 0xFF607D8F.toInt(),
        paletteStyle = PaletteStyle.Neutral,
        colorSpec = ColorSpec.SpecVersion.Default,
        miuixMonet = false,
        enableBlur = false,
        enableFloatingBottomBar = false,
        enableFloatingBottomBarBlur = false,
        pageScale = 1.0f,
    ),

    DYNAMIC_COLOR(
        value = "dynamic_color",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_dynamic_color,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_dynamic_color_summary,
        colorMode = ColorMode.MONET_SYSTEM,
        keyColor = 0,
        paletteStyle = PaletteStyle.TonalSpot,
        colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        miuixMonet = true,
        enableBlur = true,
        enableFloatingBottomBar = false,
        enableFloatingBottomBarBlur = false,
        pageScale = 1.0f,
    ),

    PREMIUM_GLOSS(
        value = "premium_gloss",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_premium_gloss,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_premium_gloss_summary,
        colorMode = ColorMode.MONET_DARK,
        keyColor = 0,
        paletteStyle = PaletteStyle.Fidelity,
        colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        miuixMonet = true,
        enableBlur = true,
        enableFloatingBottomBar = true,
        enableFloatingBottomBarBlur = true,
        pageScale = 1.0f,
    ),

    LIQUID_GLASS(
        value = "liquid_glass",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_liquid_glass,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_liquid_glass_summary,
        colorMode = ColorMode.SYSTEM,
        keyColor = 0xFF58758A.toInt(),
        paletteStyle = PaletteStyle.TonalSpot,
        colorSpec = ColorSpec.SpecVersion.Default,
        miuixMonet = false,
        enableBlur = true,
        enableFloatingBottomBar = true,
        enableFloatingBottomBarBlur = true,
        pageScale = 1.0f,
    ),

    SNOW(
        value = "snow",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_snow,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_snow_summary,
        colorMode = ColorMode.SYSTEM,
        keyColor = 0xFF287E86.toInt(),
        paletteStyle = PaletteStyle.Fidelity,
        colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        miuixMonet = false,
        enableBlur = false,
        enableFloatingBottomBar = true,
        enableFloatingBottomBarBlur = false,
        pageScale = 1.0f,
    ),

    RAIN(
        value = "rain",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_rain,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_rain_summary,
        colorMode = ColorMode.SYSTEM,
        keyColor = 0xFF5E84A6.toInt(),
        paletteStyle = PaletteStyle.Fidelity,
        colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        miuixMonet = false,
        enableBlur = false,
        enableFloatingBottomBar = true,
        enableFloatingBottomBarBlur = false,
        pageScale = 1.0f,
    ),

    PIXEL(
        value = "pixel",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_pixel,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_pixel_summary,
        colorMode = ColorMode.SYSTEM,
        keyColor = 0xFF526D43.toInt(),
        paletteStyle = PaletteStyle.Fidelity,
        colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        miuixMonet = false,
        enableBlur = false,
        enableFloatingBottomBar = true,
        enableFloatingBottomBarBlur = false,
        pageScale = 1.0f,
    ),

    SKROOTPRO(
        value = "skrootpro",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_skrootpro,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_skrootpro_summary,
        colorMode = ColorMode.SYSTEM,
        keyColor = 0xFF6A00F4.toInt(),
        paletteStyle = PaletteStyle.Fidelity,
        colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        miuixMonet = false,
        enableBlur = false,
        enableFloatingBottomBar = true,
        enableFloatingBottomBarBlur = false,
        pageScale = 1.0f,
    ),

    ALPHA(
        value = "alpha",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_alpha,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_alpha_summary,
        colorMode = ColorMode.SYSTEM,
        keyColor = 0xFF3E86BE.toInt(),
        paletteStyle = PaletteStyle.Fidelity,
        colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        miuixMonet = false,
        enableBlur = false,
        enableFloatingBottomBar = false,
        enableFloatingBottomBarBlur = false,
        pageScale = 1.0f,
    ),

    DELTA(
        value = "delta",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_delta,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_delta_summary,
        colorMode = ColorMode.SYSTEM,
        keyColor = 0xFF2F8A3B.toInt(),
        paletteStyle = PaletteStyle.Fidelity,
        colorSpec = ColorSpec.SpecVersion.SPEC_2025,
        miuixMonet = false,
        enableBlur = false,
        enableFloatingBottomBar = true,
        enableFloatingBottomBarBlur = false,
        pageScale = 1.0f,
    ),

    GEEK_DARK(
        value = "geek_dark",
        titleRes = me.weishu.kernelsu.R.string.theme_preset_geek_dark,
        summaryRes = me.weishu.kernelsu.R.string.theme_preset_geek_dark_summary,
        colorMode = ColorMode.DARK_AMOLED,
        keyColor = 0xFF9E9E9E.toInt(),
        paletteStyle = PaletteStyle.Monochrome,
        colorSpec = ColorSpec.SpecVersion.Default,
        miuixMonet = false,
        enableBlur = false,
        enableFloatingBottomBar = false,
        enableFloatingBottomBarBlur = false,
        pageScale = 0.95f,
    );

    companion object {
        val workshopPresets: List<ThemePreset>
            get() = entries.filterNot { it == CUSTOM || it == DELTA }

        fun fromValue(value: String?): ThemePreset = entries.find { it.value == value } ?: CLEAN_TOOL
    }

    fun targetUiMode(currentUiMode: String): String {
        return when (this) {
            CUSTOM -> InterfaceStyle.normalizeValue(currentUiMode)
            SKROOTPRO -> InterfaceStyle.Skrootpro.value
            ALPHA -> InterfaceStyle.Alpha.value
            DELTA -> InterfaceStyle.Delta.value
            LIQUID_GLASS -> InterfaceStyle.LiquidGlass.value
            SNOW -> InterfaceStyle.Snow.value
            RAIN -> InterfaceStyle.Rain.value
            PIXEL -> InterfaceStyle.Pixel.value
            PREMIUM_GLOSS -> InterfaceStyle.Miuix.value
            GEEK_DARK, CLEAN_TOOL, DYNAMIC_COLOR -> InterfaceStyle.Miuix.value
        }
    }

    fun isCompatibleWith(uiMode: String): Boolean {
        return this == CUSTOM || targetUiMode(uiMode) == uiMode
    }
}
