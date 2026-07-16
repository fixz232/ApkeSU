package me.weishu.kernelsu.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.LocalSkrootproTopBarColor
import me.weishu.kernelsu.ui.LocalSkrootproTopBarContentColor
import me.weishu.kernelsu.ui.component.snow.LocalSeasonStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle

data class TopBarColors(
    val container: Color,
    val content: Color,
)

@Composable
fun skrootproTopBarColors(defaultContainer: Color, defaultContent: Color): TopBarColors {
    if (LocalInterfaceStyle.current == InterfaceStyle.Snow.value) {
        val dark = isInDarkTheme()
        val seasonalContainer = when (LocalSeasonStyle.current) {
            SeasonStyle.Spring -> if (dark) Color(0xB31B3320) else Color(0xC7F6FAEF)
            SeasonStyle.Summer -> if (dark) Color(0xB3103035) else Color(0xC7F0FAF8)
            SeasonStyle.Autumn -> if (dark) Color(0xB3372D25) else Color(0xC7FFF7EA)
            SeasonStyle.Winter -> if (dark) Color(0xB3132B35) else Color(0xC7F5FBFD)
        }
        return TopBarColors(
            container = seasonalContainer,
            content = defaultContent,
        )
    }

    if (LocalImmersiveBackgroundActive.current) {
        return TopBarColors(container = Color.Transparent, content = defaultContent)
    }

    return when (LocalInterfaceStyle.current) {
        InterfaceStyle.Skrootpro.value -> TopBarColors(
            container = LocalSkrootproTopBarColor.current,
            content = LocalSkrootproTopBarContentColor.current,
        )

        InterfaceStyle.LiquidGlass.value -> TopBarColors(
            container = Color.Transparent,
            content = defaultContent,
        )

        else -> TopBarColors(container = defaultContainer, content = defaultContent)
    }
}
