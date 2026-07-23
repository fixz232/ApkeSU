package me.weishu.kernelsu.ui.component.rain

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.R
import kotlin.random.Random

const val RAIN_STYLE_KEY = "rain_style"
const val RAIN_CARD_MOTION_ENABLED_KEY = "rain_card_motion_enabled"
const val DEFAULT_RAIN_CARD_MOTION_ENABLED = true

enum class RainStyle(
    val value: String,
    @StringRes val labelRes: Int,
    @StringRes val summaryRes: Int,
    @StringRes val mottoRes: Int,
    @ColorInt val keyColor: Int,
) {
    LightRain(
        value = "light_rain",
        labelRes = R.string.rain_style_light,
        summaryRes = R.string.rain_style_light_summary,
        mottoRes = R.string.rain_motto_light,
        keyColor = 0xFF5E84A6.toInt(),
    ),
    MediumRain(
        value = "medium_rain",
        labelRes = R.string.rain_style_medium,
        summaryRes = R.string.rain_style_medium_summary,
        mottoRes = R.string.rain_motto_medium,
        keyColor = 0xFF4F7397.toInt(),
    ),
    HeavyRain(
        value = "heavy_rain",
        labelRes = R.string.rain_style_heavy,
        summaryRes = R.string.rain_style_heavy_summary,
        mottoRes = R.string.rain_motto_heavy,
        keyColor = 0xFF3B566F.toInt(),
    ),
    Thunderstorm(
        value = "thunderstorm",
        labelRes = R.string.rain_style_thunderstorm,
        summaryRes = R.string.rain_style_thunderstorm_summary,
        mottoRes = R.string.rain_motto_thunderstorm,
        keyColor = 0xFF666FA8.toInt(),
    );

    companion object {
        const val DEFAULT_VALUE = "light_rain"

        fun fromValue(value: String?): RainStyle = entries.firstOrNull { it.value == value } ?: LightRain

        fun fromIndex(index: Int): RainStyle = entries.getOrElse(index) { LightRain }

        fun selectedIndex(value: String?): Int = entries.indexOf(fromValue(value)).coerceAtLeast(0)
    }
}

val LocalRainStyle = staticCompositionLocalOf { RainStyle.LightRain }

@Immutable
data class RainPalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val fog: Color,
    val cloud: Color,
    val rain: Color,
    val rainAccent: Color,
    val ripple: Color,
    val surfaceTop: Color,
    val surfaceBottom: Color,
    val outline: Color,
    val highlight: Color,
    val content: Color,
    val shadow: Color,
)

fun rainPalette(style: RainStyle, dark: Boolean): RainPalette {
    val darkScene = dark || style == RainStyle.HeavyRain || style == RainStyle.Thunderstorm
    if (darkScene) {
        val thunder = style == RainStyle.Thunderstorm
        return RainPalette(
            backgroundTop = if (thunder) Color(0xFF30384E) else Color(0xFF2C3B4E),
            backgroundBottom = if (thunder) Color(0xFF151A28) else Color(0xFF19222D),
            fog = if (thunder) Color(0xFF858AA8) else Color(0xFF789094),
            cloud = if (thunder) Color(0xFF737A98) else Color(0xFF62747B),
            rain = Color(0xFFD5E9F7),
            rainAccent = if (thunder) Color(0xFFE2DFFF) else Color(0xFFA4E5DF),
            ripple = if (thunder) Color(0xFFBDB8F2) else Color(0xFF78BFC1),
            surfaceTop = if (thunder) Color(0xC52C3045) else Color(0xC326383E),
            surfaceBottom = if (thunder) Color(0xB31A1D2D) else Color(0xB319282D),
            outline = if (thunder) Color(0xFF9AA2D0) else Color(0xFF82ABB2),
            highlight = Color(0xFFF3FAFF),
            content = Color(0xFFF1F6FA),
            shadow = Color(0xFF080D13),
        )
    }

    return when (style) {
        RainStyle.LightRain -> RainPalette(
            backgroundTop = Color(0xFF7395B8),
            backgroundBottom = Color(0xFFA1B8CF),
            fog = Color(0xFFB8C6D4),
            cloud = Color(0xFFDCE4E3),
            rain = Color(0xFFF4FBFF),
            rainAccent = Color(0xFFC8F1EE),
            ripple = Color(0xFF6196A2),
            surfaceTop = Color(0xCFF1F7F7),
            surfaceBottom = Color(0xB8D8E6E7),
            outline = Color(0xFF7097A5),
            highlight = Color.White,
            content = Color(0xFF172F3A),
            shadow = Color(0xFF3E5A64),
        )

        RainStyle.MediumRain -> RainPalette(
            backgroundTop = Color(0xFF607F9F),
            backgroundBottom = Color(0xFF91A9C1),
            fog = Color(0xFFA9B9C8),
            cloud = Color(0xFFC8D5D3),
            rain = Color(0xFFF0F9FF),
            rainAccent = Color(0xFFB9ECE8),
            ripple = Color(0xFF588F9B),
            surfaceTop = Color(0xCCE7F0F0),
            surfaceBottom = Color(0xB8CDDCDD),
            outline = Color(0xFF628B99),
            highlight = Color.White,
            content = Color(0xFF17313B),
            shadow = Color(0xFF34535D),
        )

        RainStyle.HeavyRain,
        RainStyle.Thunderstorm,
        -> error("Dark rain palettes are handled above")
    }
}

internal data class RainSceneSpec(
    val dropCount: Int,
    val rippleCount: Int,
    val cycleMillis: Int,
    val minLengthDp: Float,
    val maxLengthDp: Float,
    val minStrokeDp: Float,
    val maxStrokeDp: Float,
    val windRatio: Float,
    val minAlpha: Float,
    val maxAlpha: Float,
) {
    companion object {
        fun forStyle(style: RainStyle): RainSceneSpec = when (style) {
            RainStyle.LightRain -> RainSceneSpec(68, 6, 15_000, 8f, 17f, 0.38f, 0.72f, 0.08f, 0.18f, 0.42f)
            RainStyle.MediumRain -> RainSceneSpec(104, 10, 11_000, 12f, 24f, 0.46f, 0.9f, 0.13f, 0.18f, 0.48f)
            RainStyle.HeavyRain -> RainSceneSpec(142, 15, 8_500, 18f, 34f, 0.54f, 1.08f, 0.20f, 0.20f, 0.54f)
            RainStyle.Thunderstorm -> RainSceneSpec(168, 18, 7_200, 20f, 38f, 0.58f, 1.18f, 0.24f, 0.22f, 0.58f)
        }
    }
}

internal fun forceRainDarkTheme(style: RainStyle): Boolean {
    return style == RainStyle.HeavyRain || style == RainStyle.Thunderstorm
}

internal fun isRainLightningEnabled(
    style: RainStyle,
    dark: Boolean,
    animationsEnabled: Boolean,
): Boolean = style == RainStyle.Thunderstorm && dark && animationsEnabled

internal fun nextLightningDelayMillis(random: Random): Long = random.nextLong(3_000L, 8_001L)

internal fun nextLightningFlashDurationMillis(random: Random): Long = random.nextLong(50L, 101L)
