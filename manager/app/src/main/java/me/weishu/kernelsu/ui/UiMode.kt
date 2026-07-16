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
    Studio("studio", R.string.interface_style_studio),
    LiquidGlass("liquid_glass", R.string.interface_style_liquid_glass),
    Snow("snow", R.string.interface_style_snow),
    Skrootpro("skrootpro", R.string.interface_style_skrootpro),
    Alpha("alpha", R.string.interface_style_alpha),
    Delta("delta", R.string.interface_style_delta);

    companion object {
        fun fromIndex(index: Int): InterfaceStyle = entries.getOrElse(index) { Miuix }

        fun selectedIndex(value: String): Int = entries.indexOfFirst { it.value == value }
            .takeIf { it >= 0 } ?: entries.indexOf(Miuix)

        fun isMiuixBased(value: String): Boolean = value != Material.value
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }

val LocalInterfaceStyle = staticCompositionLocalOf { InterfaceStyle.Miuix.value }

val LocalSkrootproTopBarColor = staticCompositionLocalOf { Color(0xFF6A00F4) }

val LocalSkrootproTopBarContentColor = staticCompositionLocalOf { Color.White }
