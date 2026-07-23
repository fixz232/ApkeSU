package me.weishu.kernelsu.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.annotation.StringRes
import me.weishu.kernelsu.R

enum class UiMode(val value: String) {
    Miuix("miuix");

    companion object {
        fun fromValue(value: String): UiMode = Miuix

        val DEFAULT_VALUE = Miuix.value
    }
}

enum class InterfaceStyle(val value: String, @StringRes val labelRes: Int) {
    Miuix(UiMode.Miuix.value, R.string.interface_style_miuix),
    Studio("studio", R.string.interface_style_studio),
    LiquidGlass("liquid_glass", R.string.interface_style_liquid_glass),
    Snow("snow", R.string.interface_style_snow),
    Rain("rain", R.string.interface_style_rain),
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

        fun isMiuixBased(value: String): Boolean = true
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }

val LocalInterfaceStyle = staticCompositionLocalOf { InterfaceStyle.Miuix.value }

val LocalSkrootproTopBarColor = staticCompositionLocalOf { Color(0xFF6A00F4) }

val LocalSkrootproTopBarContentColor = staticCompositionLocalOf { Color.White }
