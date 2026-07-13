package me.weishu.kernelsu.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.theme.LocalBlurIntensity
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassSurface
import me.weishu.kernelsu.ui.component.liquid.lens
import me.weishu.kernelsu.ui.component.liquid.liquidGlassBackdropColor
import me.weishu.kernelsu.ui.component.liquid.liquidGlassSurfaceColor
import me.weishu.kernelsu.ui.component.liquid.vibrancy
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val surfaceColor = liquidGlassBackdropColor()
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isLiquidGlass = LocalInterfaceStyle.current == InterfaceStyle.LiquidGlass.value
    val blurIntensity = LocalBlurIntensity.current
    val liquidShape = remember { RoundedCornerShape(0.dp) }
    val surfaceColor = if (isLiquidGlass) liquidGlassSurfaceColor() else MiuixTheme.colorScheme.surface
    Box(
        modifier = when {
            blurActive && backdrop != null && isLiquidGlass -> {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { liquidShape },
                    effects = {
                        vibrancy()
                        blur((8.dp.toPx() * blurIntensity), (8.dp.toPx() * blurIntensity))
                        lens(
                            refractionHeight = 18.dp.toPx(),
                            refractionAmount = 12.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = 0.25f,
                        )
                    },
                    onDrawSurface = {
                        drawRect(surfaceColor.copy(alpha = 0.70f))
                    },
                )
            }

            blurActive && backdrop != null -> {
                Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    blurRadius = 25f * blurIntensity,
                    colors = BlurColors(
                        blendColors = listOf(
                            BlendColorEntry(color = surfaceColor.copy(0.87f)),
                        ),
                    ),
                )
            }

            isLiquidGlass -> Modifier.globalLiquidGlassSurface(
                shape = liquidShape,
                surfaceColor = surfaceColor,
                surfaceAlpha = 0.74f,
                blurRadius = 0.dp,
                strokeAlpha = 0.52f,
            )

            else -> Modifier
        },
    ) {
        content()
    }
}
