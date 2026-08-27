package me.weishu.kernelsu.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.LocalSkrootproTopBarColor
import me.weishu.kernelsu.ui.LocalSkrootproTopBarContentColor
import me.weishu.kernelsu.ui.component.snow.LocalSeasonStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.snow.seasonTopBarContentColor
import me.weishu.kernelsu.ui.component.pixel.LocalPixelStyle
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.pixel.pixelPalette
import me.weishu.kernelsu.ui.component.rain.isRainInterfaceStyle
import me.weishu.kernelsu.ui.component.rain.rainTopBarContainerColor
import me.weishu.kernelsu.ui.component.rain.rainTopBarContentColor

data class TopBarColors(
    val container: Color,
    val content: Color,
)

@Composable
fun skrootproTopBarColors(defaultContainer: Color, defaultContent: Color): TopBarColors {
    if (LocalImmersiveBackgroundActive.current) {
        return TopBarColors(container = Color.Transparent, content = defaultContent)
    }

    if (isRainInterfaceStyle()) {
        return TopBarColors(
            container = rainTopBarContainerColor(),
            content = rainTopBarContentColor(),
        )
    }

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
            content = seasonTopBarContentColor(defaultContent),
        )
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Pixel.value) {
        val pixelStyle = LocalPixelStyle.current
        val dark = isInDarkTheme()
        val palette = pixelPalette(pixelStyle, dark)
        return TopBarColors(
            container = palette.surface.copy(alpha = 0.94f),
            content = when (pixelStyle) {
                PixelStyle.ClassicHandheld,
                PixelStyle.NeonArcade,
                PixelStyle.PastoralFields,
                PixelStyle.StarVoyage,
                PixelStyle.InkJade,
                PixelStyle.CyberHacker,
                PixelStyle.ThreeKingdoms,
                PixelStyle.BianliangMarket,
                PixelStyle.FishingHarbor,
                PixelStyle.TribalJungle,
                PixelStyle.LavaValley,
                PixelStyle.DunhuangDesert,
                PixelStyle.VikingSnowfield,
                PixelStyle.JiangnanWatertown,
                PixelStyle.CloudTown,
                PixelStyle.PetCompanion,
                -> palette.primary

                PixelStyle.RustWasteland -> if (dark) {
                    lerp(palette.primary, palette.highlight, 0.20f)
                } else {
                    palette.primary
                }

                PixelStyle.OceanDepths -> defaultContent
            },
        )
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
