package me.weishu.kernelsu.ui.component.ink

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.R

const val INK_STYLE_KEY = "ink_style"
const val INK_FONT_ENABLED_KEY = "ink_font_enabled"
const val INK_CARD_MOTION_ENABLED_KEY = "ink_card_motion_enabled"
const val DEFAULT_INK_FONT_ENABLED = true
const val DEFAULT_INK_CARD_MOTION_ENABLED = true

enum class InkStyle(
    val value: String,
    @StringRes val labelRes: Int,
    @StringRes val summaryRes: Int,
    @ColorInt val keyColor: Int,
) {
    MistJiangnan(
        value = "mist_jiangnan",
        labelRes = R.string.ink_style_mist_jiangnan,
        summaryRes = R.string.ink_style_mist_jiangnan_summary,
        keyColor = 0xFF52746B.toInt(),
    ),
    VerdantLandscape(
        value = "verdant_landscape",
        labelRes = R.string.ink_style_verdant_landscape,
        summaryRes = R.string.ink_style_verdant_landscape_summary,
        keyColor = 0xFF3F7763.toInt(),
    ),
    CinnabarScroll(
        value = "cinnabar_scroll",
        labelRes = R.string.ink_style_cinnabar_scroll,
        summaryRes = R.string.ink_style_cinnabar_scroll_summary,
        keyColor = 0xFF9E493F.toInt(),
    ),
    PurpleNightMountain(
        value = "purple_night_mountain",
        labelRes = R.string.ink_style_purple_night_mountain,
        summaryRes = R.string.ink_style_purple_night_mountain_summary,
        keyColor = 0xFF76628E.toInt(),
    );

    companion object {
        const val DEFAULT_VALUE = "mist_jiangnan"

        fun fromValue(value: String?): InkStyle =
            entries.firstOrNull { it.value == value } ?: MistJiangnan

        fun fromIndex(index: Int): InkStyle = entries.getOrElse(index) { MistJiangnan }

        fun selectedIndex(value: String?): Int = entries.indexOf(fromValue(value)).coerceAtLeast(0)
    }
}

val LocalInkStyle = staticCompositionLocalOf { InkStyle.MistJiangnan }

val LocalInkFontEnabled = staticCompositionLocalOf { DEFAULT_INK_FONT_ENABLED }

@Immutable
data class InkPalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val mist: Color,
    val farMountain: Color,
    val nearMountain: Color,
    val water: Color,
    val surfaceTop: Color,
    val surfaceBottom: Color,
    val outline: Color,
    val content: Color,
    val secondaryContent: Color,
    val primary: Color,
    val secondary: Color,
    val seal: Color,
    val shadow: Color,
)

fun inkPalette(style: InkStyle, dark: Boolean): InkPalette {
    return when (style) {
        InkStyle.MistJiangnan -> if (dark) {
            InkPalette(
                backgroundTop = Color(0xFF1B2021),
                backgroundBottom = Color(0xFF0C1112),
                mist = Color(0xFFC6D1D0),
                farMountain = Color(0xFF738389),
                nearMountain = Color(0xFF324543),
                water = Color(0xFF73969C),
                surfaceTop = Color(0xE0242C2D),
                surfaceBottom = Color(0xD0182021),
                outline = Color(0xFF71878A),
                content = Color(0xFFE9EEED),
                secondaryContent = Color(0xFFB8C5C3),
                primary = Color(0xFF83A9A1),
                secondary = Color(0xFF829CB0),
                seal = Color(0xFFCB6A5E),
                shadow = Color(0xFF050807),
            )
        } else {
            InkPalette(
                backgroundTop = Color(0xFFF5F6F4),
                backgroundBottom = Color(0xFFD6DEDC),
                mist = Color(0xFFFBFCFB),
                farMountain = Color(0xFF96A7AA),
                nearMountain = Color(0xFF4F6260),
                water = Color(0xFF668990),
                surfaceTop = Color(0xF2FAFBF9),
                surfaceBottom = Color(0xE8E7ECE9),
                outline = Color(0xFF70817F),
                content = Color(0xFF202B29),
                secondaryContent = Color(0xFF4D5D59),
                primary = Color(0xFF4A7069),
                secondary = Color(0xFF647F94),
                seal = Color(0xFFAD493F),
                shadow = Color(0xFF536260),
            )
        }

        InkStyle.VerdantLandscape -> if (dark) {
            InkPalette(
                backgroundTop = Color(0xFF19221F),
                backgroundBottom = Color(0xFF0B1210),
                mist = Color(0xFFC3D0C8),
                farMountain = Color(0xFF6B8176),
                nearMountain = Color(0xFF2C493C),
                water = Color(0xFF6C8D98),
                surfaceTop = Color(0xE022302A),
                surfaceBottom = Color(0xD016211D),
                outline = Color(0xFF6B8879),
                content = Color(0xFFE7EFEA),
                secondaryContent = Color(0xFFB4C6BB),
                primary = Color(0xFF7CA88F),
                secondary = Color(0xFF8098AB),
                seal = Color(0xFFC46358),
                shadow = Color(0xFF040806),
            )
        } else {
            InkPalette(
                backgroundTop = Color(0xFFF4F6F2),
                backgroundBottom = Color(0xFFD5DED5),
                mist = Color(0xFFFAFCF9),
                farMountain = Color(0xFF96AA9B),
                nearMountain = Color(0xFF426557),
                water = Color(0xFF638797),
                surfaceTop = Color(0xF2F9FBF7),
                surfaceBottom = Color(0xE8E4EBE3),
                outline = Color(0xFF697E72),
                content = Color(0xFF1D2B25),
                secondaryContent = Color(0xFF4C5E55),
                primary = Color(0xFF477260),
                secondary = Color(0xFF6D8192),
                seal = Color(0xFFAF4A40),
                shadow = Color(0xFF506159),
            )
        }

        InkStyle.CinnabarScroll -> if (dark) {
            InkPalette(
                backgroundTop = Color(0xFF221F1F),
                backgroundBottom = Color(0xFF100D0D),
                mist = Color(0xFFD0C9C5),
                farMountain = Color(0xFF7A7F82),
                nearMountain = Color(0xFF45464A),
                water = Color(0xFF758A96),
                surfaceTop = Color(0xE02F2928),
                surfaceBottom = Color(0xD01E1918),
                outline = Color(0xFF887873),
                content = Color(0xFFF0EBE8),
                secondaryContent = Color(0xFFC7BAB4),
                primary = Color(0xFFCC7569),
                secondary = Color(0xFF8296A2),
                seal = Color(0xFFE15E4E),
                shadow = Color(0xFF090505),
            )
        } else {
            InkPalette(
                backgroundTop = Color(0xFFF5F4F2),
                backgroundBottom = Color(0xFFDDD9D5),
                mist = Color(0xFFFCFBF8),
                farMountain = Color(0xFF9AA3A8),
                nearMountain = Color(0xFF555D61),
                water = Color(0xFF748894),
                surfaceTop = Color(0xF2FBFAF7),
                surfaceBottom = Color(0xE8EBE7E2),
                outline = Color(0xFF827A76),
                content = Color(0xFF2D2927),
                secondaryContent = Color(0xFF5E5752),
                primary = Color(0xFF98483F),
                secondary = Color(0xFF6D8290),
                seal = Color(0xFFC83F34),
                shadow = Color(0xFF625B57),
            )
        }

        InkStyle.PurpleNightMountain -> if (dark) {
            InkPalette(
                backgroundTop = Color(0xFF201E27),
                backgroundBottom = Color(0xFF0D0B12),
                mist = Color(0xFFCBC7D5),
                farMountain = Color(0xFF737789),
                nearMountain = Color(0xFF3A3A49),
                water = Color(0xFF728698),
                surfaceTop = Color(0xE02B2934),
                surfaceBottom = Color(0xD01B1922),
                outline = Color(0xFF7F778E),
                content = Color(0xFFEDEAF1),
                secondaryContent = Color(0xFFC2BCC9),
                primary = Color(0xFFA18FB8),
                secondary = Color(0xFF77968E),
                seal = Color(0xFFD26676),
                shadow = Color(0xFF050307),
            )
        } else {
            InkPalette(
                backgroundTop = Color(0xFFF4F3F6),
                backgroundBottom = Color(0xFFDAD6DF),
                mist = Color(0xFFFCFAFD),
                farMountain = Color(0xFFA1A5B2),
                nearMountain = Color(0xFF5A5969),
                water = Color(0xFF6F8296),
                surfaceTop = Color(0xF2FAF9FB),
                surfaceBottom = Color(0xE8EAE6ED),
                outline = Color(0xFF7C7684),
                content = Color(0xFF2B282F),
                secondaryContent = Color(0xFF5C5762),
                primary = Color(0xFF715F89),
                secondary = Color(0xFF5F8078),
                seal = Color(0xFFB84E60),
                shadow = Color(0xFF5C5761),
            )
        }
    }
}
