package me.weishu.kernelsu.ui.component.snow

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.staticCompositionLocalOf
import me.weishu.kernelsu.R

const val SEASON_STYLE_KEY = "season_style"
const val SEASON_CARD_MOTION_ENABLED_KEY = "season_card_motion_enabled"
const val DEFAULT_SEASON_CARD_MOTION_ENABLED = true

enum class SeasonStyle(
    val value: String,
    @StringRes val labelRes: Int,
    @StringRes val summaryRes: Int,
    @ColorInt val keyColor: Int,
    @DrawableRes val wallpaperRes: Int,
) {
    Spring(
        value = "spring",
        labelRes = R.string.season_style_spring,
        summaryRes = R.string.season_style_spring_summary,
        keyColor = 0xFF4F7F42.toInt(),
        wallpaperRes = R.drawable.seasonal_spring_wallpaper,
    ),
    Summer(
        value = "summer",
        labelRes = R.string.season_style_summer,
        summaryRes = R.string.season_style_summer_summary,
        keyColor = 0xFF167C80.toInt(),
        wallpaperRes = R.drawable.seasonal_summer_wallpaper,
    ),
    Autumn(
        value = "autumn",
        labelRes = R.string.season_style_autumn,
        summaryRes = R.string.season_style_autumn_summary,
        keyColor = 0xFF9A5A2B.toInt(),
        wallpaperRes = R.drawable.seasonal_autumn_wallpaper,
    ),
    Winter(
        value = "winter",
        labelRes = R.string.season_style_winter,
        summaryRes = R.string.season_style_winter_summary,
        keyColor = 0xFF287E86.toInt(),
        wallpaperRes = R.drawable.snow_style_wallpaper,
    );

    companion object {
        const val DEFAULT_VALUE = "winter"

        fun fromValue(value: String?): SeasonStyle = entries.firstOrNull { it.value == value } ?: Winter

        fun fromIndex(index: Int): SeasonStyle = entries.getOrElse(index) { Winter }

        fun selectedIndex(value: String?): Int = entries.indexOf(fromValue(value))
    }
}

val LocalSeasonStyle = staticCompositionLocalOf { SeasonStyle.Winter }
