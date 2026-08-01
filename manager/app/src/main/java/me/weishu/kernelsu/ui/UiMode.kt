package me.weishu.kernelsu.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.annotation.StringRes
import me.weishu.kernelsu.R

enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        fun fromValue(value: String): UiMode = when (value) {
            Material.value -> Material
            else -> Miuix
        }

        val DEFAULT_VALUE = Miuix.value
    }
}

enum class InterfaceStyle(val value: String, @StringRes val labelRes: Int) {
    Miuix(UiMode.Miuix.value, R.string.interface_style_miuix),
    Material(UiMode.Material.value, R.string.interface_style_material),
    LiquidGlass("liquid_glass", R.string.interface_style_liquid_glass),
    Snow("snow", R.string.interface_style_snow),
    Rain("rain", R.string.interface_style_rain),
    Ink("ink", R.string.interface_style_ink),
    Pixel("pixel", R.string.interface_style_pixel),
    Skrootpro("skrootpro", R.string.interface_style_skrootpro),
    Alpha("alpha", R.string.interface_style_alpha),
    Delta("delta", R.string.interface_style_delta);

    companion object {
        val selectableEntries: List<InterfaceStyle> = entries.filterNot { it == Delta }

        fun fromIndex(index: Int): InterfaceStyle = selectableEntries.getOrElse(index) { Miuix }

        fun selectedIndex(value: String): Int {
            val normalizedValue = normalizeValue(value)
            val selected = if (normalizedValue == Delta.value) {
                Alpha
            } else {
                entries.firstOrNull { it.value == normalizedValue }
            }
            return selectableEntries.indexOf(selected).takeIf { it >= 0 }
                ?: selectableEntries.indexOf(Miuix)
        }

        fun normalizeValue(value: String?): String {
            return entries.firstOrNull { it.value == value }?.value ?: Miuix.value
        }

        fun isMiuixBased(value: String): Boolean = normalizeValue(value) != Material.value
    }
}

internal fun resolveRealtimeBlurEnabled(
    interfaceStyle: String,
    requested: Boolean,
): Boolean {
    // Miuix LayerBackdrop can crash vendor RenderEffect implementations when every
    // frosted card starts capturing during a live style switch. The glass style
    // keeps its translucent fallback surfaces without entering that render path.
    return requested &&
        InterfaceStyle.normalizeValue(interfaceStyle) != InterfaceStyle.LiquidGlass.value
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }

val LocalInterfaceStyle = staticCompositionLocalOf { InterfaceStyle.Miuix.value }

val LocalSkrootproTopBarColor = staticCompositionLocalOf { Color(0xFF6A00F4) }

val LocalSkrootproTopBarContentColor = staticCompositionLocalOf { Color.White }
