package me.weishu.kernelsu.ui.component.pixel

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.decoration.PIXEL_CARD_DECORATIONS
import me.weishu.kernelsu.ui.component.decoration.PixelCardPattern
import me.weishu.kernelsu.ui.component.decoration.drawPixelCardPatternOverlay
import me.weishu.kernelsu.ui.component.decoration.drawPixelCardPatternUnderlay
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.component.custom.CustomCardTarget
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@ReadOnlyComposable
fun isPixelInterfaceStyle(): Boolean = LocalInterfaceStyle.current == InterfaceStyle.Pixel.value

@Composable
fun PixelBackdrop(modifier: Modifier = Modifier) {
    val style = LocalPixelStyle.current
    val dark = isInDarkTheme()
    val palette = pixelPalette(style, dark)
    val cyberProgress = rememberCyberDataProgress(style)
    val bianliangProgress = rememberBianliangMistProgress(style)
    val harborProgress = rememberFishingHarborBreezeProgress(style)
    val jungleProgress = rememberTribalJungleMistProgress(style)
    val lavaProgress = rememberLavaValleyFlowProgress(style)
    val dunhuangProgress = rememberDunhuangSandProgress(style)
    val vikingProgress = rememberVikingSnowProgress(style)
    val jiangnanProgress = rememberJiangnanRainProgress(style)
    val cloudTownProgress = rememberCloudTownDriftProgress(style)
    Canvas(modifier = modifier.fillMaxSize()) {
        drawPixelBackdropBase(style, palette)
        drawPixelGrid(style, palette)
        when (style) {
            PixelStyle.ClassicHandheld -> drawHandheldScene(palette)
            PixelStyle.NeonArcade -> drawArcadeScene(palette)
            PixelStyle.PastoralFields -> drawPastoralScene(palette)
            PixelStyle.StarVoyage -> drawStarVoyageScene(palette)
            PixelStyle.InkJade -> drawInkJadeScene(palette)
            PixelStyle.RustWasteland -> drawRustWastelandScene(palette)
            PixelStyle.OceanDepths -> drawOceanDepthsScene(palette)
            PixelStyle.CyberHacker -> drawCyberHackerScene(palette, cyberProgress)
            PixelStyle.ThreeKingdoms -> drawThreeKingdomsScene(palette)
            PixelStyle.BianliangMarket -> drawBianliangScene(palette, bianliangProgress)
            PixelStyle.FishingHarbor -> drawFishingHarborScene(palette, harborProgress)
            PixelStyle.TribalJungle -> drawTribalJungleScene(palette, jungleProgress)
            PixelStyle.LavaValley -> drawLavaValleyScene(palette, lavaProgress)
            PixelStyle.DunhuangDesert -> drawDunhuangScene(palette, dunhuangProgress)
            PixelStyle.VikingSnowfield -> drawVikingScene(palette, vikingProgress, dark)
            PixelStyle.JiangnanWatertown -> drawJiangnanScene(palette, jiangnanProgress, dark)
            PixelStyle.CloudTown -> drawCloudTownScene(palette, cloudTownProgress, dark)
        }
    }
}

@Composable
private fun rememberCyberDataProgress(style: PixelStyle): Float {
    if (style != PixelStyle.CyberHacker) return 0f
    val systemAnimationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!systemAnimationsEnabled) return 0.36f
    val transition = rememberInfiniteTransition(label = "pixelCyberData")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 8_000, easing = LinearEasing)),
        label = "pixelCyberDataProgress",
    )
    return progress
}

@Composable
private fun rememberBianliangMistProgress(style: PixelStyle): Float {
    if (style != PixelStyle.BianliangMarket) return 0f
    val systemAnimationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!systemAnimationsEnabled) return 0.42f
    val transition = rememberInfiniteTransition(label = "pixelBianliangMist")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 12_000, easing = LinearEasing)),
        label = "pixelBianliangMistProgress",
    )
    return progress
}

@Composable
private fun rememberFishingHarborBreezeProgress(style: PixelStyle): Float {
    if (style != PixelStyle.FishingHarbor) return 0f
    val systemAnimationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!systemAnimationsEnabled) return 0.38f
    val transition = rememberInfiniteTransition(label = "pixelFishingHarborBreeze")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 14_000, easing = LinearEasing)),
        label = "pixelFishingHarborBreezeProgress",
    )
    return progress
}

@Composable
private fun rememberTribalJungleMistProgress(style: PixelStyle): Float {
    if (style != PixelStyle.TribalJungle) return 0f
    val systemAnimationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!systemAnimationsEnabled) return 0.41f
    val transition = rememberInfiniteTransition(label = "pixelTribalJungleMist")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 15_000, easing = LinearEasing)),
        label = "pixelTribalJungleMistProgress",
    )
    return progress
}

@Composable
private fun rememberLavaValleyFlowProgress(style: PixelStyle): Float {
    if (style != PixelStyle.LavaValley) return 0f
    val systemAnimationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!systemAnimationsEnabled) return 0.44f
    val transition = rememberInfiniteTransition(label = "pixelLavaValleyFlow")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 12_000, easing = LinearEasing)),
        label = "pixelLavaValleyFlowProgress",
    )
    return progress
}

@Composable
private fun rememberDunhuangSandProgress(style: PixelStyle): Float {
    if (style != PixelStyle.DunhuangDesert) return 0f
    val systemAnimationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!systemAnimationsEnabled) return 0.39f
    val transition = rememberInfiniteTransition(label = "pixelDunhuangSand")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 16_000, easing = LinearEasing)),
        label = "pixelDunhuangSandProgress",
    )
    return progress
}

@Composable
private fun rememberVikingSnowProgress(style: PixelStyle): Float {
    if (style != PixelStyle.VikingSnowfield) return 0f
    val systemAnimationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!systemAnimationsEnabled) return 0.37f
    val transition = rememberInfiniteTransition(label = "pixelVikingSnow")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 15_000, easing = LinearEasing)),
        label = "pixelVikingSnowProgress",
    )
    return progress
}

@Composable
private fun rememberJiangnanRainProgress(style: PixelStyle): Float {
    if (style != PixelStyle.JiangnanWatertown) return 0f
    val systemAnimationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!systemAnimationsEnabled) return 0.41f
    val transition = rememberInfiniteTransition(label = "pixelJiangnanRain")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 14_000, easing = LinearEasing)),
        label = "pixelJiangnanRainProgress",
    )
    return progress
}

@Composable
private fun rememberCloudTownDriftProgress(style: PixelStyle): Float {
    if (style != PixelStyle.CloudTown) return 0f
    val systemAnimationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!systemAnimationsEnabled) return 0.43f
    val transition = rememberInfiniteTransition(label = "pixelCloudTownDrift")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 18_000, easing = LinearEasing)),
        label = "pixelCloudTownDriftProgress",
    )
    return progress
}

@Composable
fun PixelChromeOverlay(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle()) return
    val style = LocalPixelStyle.current
    val palette = pixelPalette(style, isInDarkTheme())
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Canvas(modifier = modifier.fillMaxSize()) {
        val inset = 14.dp.toPx()
        val top = max(10.dp.toPx(), statusBarPadding.toPx() + 6.dp.toPx())
        val unit = 3.dp.toPx()
        drawRect(
            color = palette.primary.copy(alpha = 0.58f),
            topLeft = Offset(inset, top),
            size = Size(unit * 7f, unit),
        )
        drawRect(
            color = palette.secondary.copy(alpha = 0.62f),
            topLeft = Offset(inset + unit * 8f, top),
            size = Size(unit * 2f, unit),
        )
        drawRect(
            color = palette.primary.copy(alpha = 0.58f),
            topLeft = Offset(size.width - inset - unit * 7f, top),
            size = Size(unit * 7f, unit),
        )
        drawPixelCorner(Offset(inset, top + unit * 3f), unit, palette.outline.copy(alpha = 0.46f), false)
        drawPixelCorner(
            Offset(size.width - inset, top + unit * 3f),
            unit,
            palette.outline.copy(alpha = 0.46f),
            true,
        )
        drawPixelHudAccent(style, palette, top, unit)
        drawPixelModeHudPolish(style, palette, top, unit)
    }
}

@Composable
fun PixelOceanMotto(modifier: Modifier = Modifier) {
    if (!isPixelInterfaceStyle() || LocalPixelStyle.current != PixelStyle.OceanDepths) return
    val palette = pixelPalette(PixelStyle.OceanDepths, isInDarkTheme())
    val shape = pixelMottoShape
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(shape)
            .background(palette.surface.copy(alpha = 0.92f), shape)
            .border(1.dp, palette.primary.copy(alpha = 0.74f), shape)
            .drawWithContent {
                drawContent()
                val unit = 2.dp.toPx()
                repeat(4) { index ->
                    val lift = if (index % 2 == 0) unit * 0.7f else 0f
                    val leftX = unit * (2f + index * 1.5f)
                    val rightX = size.width - unit * (3.5f + index * 1.5f)
                    drawRect(
                        color = palette.primary.copy(alpha = 0.68f),
                        topLeft = Offset(leftX, unit * 1.2f - lift),
                        size = Size(unit * 1.6f, unit * 0.65f),
                    )
                    drawRect(
                        color = palette.primary.copy(alpha = 0.68f),
                        topLeft = Offset(rightX, unit * 1.2f - lift),
                        size = Size(unit * 1.6f, unit * 0.65f),
                    )
                }
                listOf(unit * 2f, size.width - unit * 3f).forEach { x ->
                    drawRect(
                        color = palette.highlight.copy(alpha = 0.76f),
                        topLeft = Offset(x, size.height - unit * 3f),
                        size = Size(unit, unit),
                        style = Stroke(width = unit * 0.35f),
                    )
                }
                drawRect(
                    color = palette.secondary.copy(alpha = 0.78f),
                    topLeft = Offset(unit * 5f, size.height - unit * 2f),
                    size = Size(unit, unit),
                )
                drawRect(
                    color = palette.secondary.copy(alpha = 0.78f),
                    topLeft = Offset(size.width - unit * 6f, size.height - unit * 2f),
                    size = Size(unit, unit),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(me.weishu.kernelsu.R.string.pixel_ocean_motto),
            modifier = Modifier.padding(horizontal = 28.dp),
            color = palette.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun Modifier.pixelMiuixCardSurface(
    shape: Shape = RoundedCornerShape(12.dp),
    enabled: Boolean = true,
    paintBackground: Boolean = true,
    customTarget: CustomCardTarget = CustomCardTarget.Default,
): Modifier {
    if (!enabled || !isPixelInterfaceStyle()) return this
    val style = LocalPixelStyle.current
    val dark = isInDarkTheme()
    val palette = pixelPalette(style, dark)
    val paintPalette = pixelCardPaintPalette(style, dark)
    val cardMotionEnabled = LocalPixelCardMotionEnabled.current
    val cardMotionProgress = LocalPixelCardMotionProgress.current
    val cardShape = if (isPixelInterfaceStyle()) RectangleShape else shape
    return this
        .drawWithContent {
            drawContent()
            drawPixelCardDecoration(style, paintPalette)
            if (cardMotionEnabled) {
                drawPixelCardMotionOverlay(style, paintPalette, cardMotionProgress.value)
            }
        }
        .clip(cardShape)
        .then(
            if (paintBackground) {
                val surfaceAlpha = if (style == PixelStyle.CyberHacker) {
                    0.84f
                } else if (dark) {
                    0.90f
                } else {
                    0.92f
                }
                Modifier.background(palette.surface.copy(alpha = surfaceAlpha), cardShape)
            } else {
                Modifier
            }
        )
        .border(
            width = if (
                style == PixelStyle.OceanDepths ||
                style == PixelStyle.ThreeKingdoms ||
                style == PixelStyle.BianliangMarket ||
                style == PixelStyle.FishingHarbor ||
                style == PixelStyle.TribalJungle ||
                style == PixelStyle.LavaValley ||
                style == PixelStyle.DunhuangDesert ||
                style == PixelStyle.VikingSnowfield
            ) {
                2.dp
            } else {
                1.dp
            },
            color = when (style) {
                PixelStyle.OceanDepths -> palette.primary.copy(alpha = if (dark) 0.78f else 0.64f)
                PixelStyle.CyberHacker -> palette.primary.copy(alpha = 0.92f)
                PixelStyle.ThreeKingdoms -> palette.outline.copy(alpha = if (dark) 0.88f else 0.76f)
                PixelStyle.BianliangMarket -> palette.primary.copy(alpha = if (dark) 0.86f else 0.70f)
                PixelStyle.FishingHarbor -> palette.secondary.copy(alpha = if (dark) 0.84f else 0.72f)
                PixelStyle.TribalJungle -> palette.primary.copy(alpha = if (dark) 0.86f else 0.74f)
                PixelStyle.LavaValley -> palette.primary.copy(alpha = if (dark) 0.90f else 0.78f)
                PixelStyle.DunhuangDesert -> palette.primary.copy(alpha = if (dark) 0.86f else 0.74f)
                PixelStyle.VikingSnowfield -> palette.outline.copy(alpha = if (dark) 0.90f else 0.78f)
                PixelStyle.JiangnanWatertown -> palette.outline.copy(alpha = if (dark) 0.76f else 0.62f)
                PixelStyle.CloudTown -> palette.secondary.copy(alpha = if (dark) 0.68f else 0.54f)
                else -> palette.outline.copy(alpha = if (dark) 0.86f else 0.72f)
            },
            shape = cardShape,
        )
        .drawWithContent {
            drawPixelCardMaterial(style, paintPalette)
            drawPixelCardUnderlay(style, paintPalette)
            if (cardMotionEnabled) {
                drawPixelCardMotionUnderlay(style, paintPalette, cardMotionProgress.value)
            }
            drawContent()
        }
        .uiDecoratedCard(
            shape = cardShape,
            enabled = enabled,
            nativeDecorations = PIXEL_CARD_DECORATIONS,
            customTarget = customTarget,
        )
}

@Composable
@ReadOnlyComposable
fun pixelAwareMiuixCardCornerRadius(defaultRadius: Dp): Dp {
    return resolvePixelMiuixCardCornerRadius(isPixelInterfaceStyle(), defaultRadius)
}

internal fun resolvePixelMiuixCardCornerRadius(pixelStyle: Boolean, defaultRadius: Dp): Dp {
    return if (pixelStyle) 0.dp else defaultRadius
}

@Composable
@ReadOnlyComposable
fun pixelAwareMiuixCardShape(defaultShape: Shape): Shape {
    return if (isPixelInterfaceStyle()) RectangleShape else defaultShape
}

internal val pixelMottoShape: Shape = RectangleShape

internal fun pixelCardPaintPalette(style: PixelStyle, dark: Boolean): PixelPalette {
    val palette = pixelPalette(style, dark)
    if (dark || style == PixelStyle.CyberHacker) return palette
    return palette.copy(
        highlight = lerp(palette.primary, palette.highlight, 0.42f),
    )
}

@Composable
fun pixelMiuixCardColors(
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
    enabled: Boolean = true,
) = if (enabled && isPixelInterfaceStyle()) {
    CardDefaults.defaultColors(
        // The modifier owns the pixel surface. Keeping Card's own layer transparent
        // prevents it from covering mode-specific material and interior artwork.
        color = pixelCardContentLayerColor(color),
    )
} else {
    CardDefaults.defaultColors(color = color)
}

internal fun pixelCardContentLayerColor(baseColor: Color): Color = baseColor.copy(alpha = 0f)

@Composable
fun pixelNavigationContainerColor(): Color {
    val dark = isInDarkTheme()
    val style = LocalPixelStyle.current
    return pixelPalette(style, dark).surface.copy(
        alpha = if (style == PixelStyle.CyberHacker) 0.90f else if (dark) 0.96f else 0.95f,
    )
}

@Composable
fun Modifier.pixelNavigationSurface(shape: Shape): Modifier {
    if (!isPixelInterfaceStyle()) return this
    val style = LocalPixelStyle.current
    val palette = pixelPalette(style, isInDarkTheme())
    return clip(shape)
        .background(pixelNavigationContainerColor(), shape)
        .border(
            1.dp,
            if (style == PixelStyle.CyberHacker) {
                palette.primary.copy(alpha = 0.94f)
            } else {
                palette.outline.copy(alpha = 0.86f)
            },
            shape,
        )
        .drawWithContent {
            val unit = 3.dp.toPx().coerceAtMost(size.minDimension / 8f)
            drawPixelNavigationFoundation(style, palette, unit)
            drawContent()
            drawPixelFrame(unit, palette.primary.copy(alpha = 0.64f), palette.secondary.copy(alpha = 0.70f))
            drawPixelModeNavigationFrame(style, palette, unit)
            drawPixelNavigationAccent(style, palette, unit)
        }
}

@Composable
fun Modifier.pixelNavigationIndicator(shape: Shape): Modifier {
    if (!isPixelInterfaceStyle()) return this
    val style = LocalPixelStyle.current
    val palette = pixelPalette(style, isInDarkTheme())
    return clip(shape)
        .background(palette.primary.copy(alpha = 0.18f), shape)
        .border(1.dp, palette.primary.copy(alpha = 0.72f), shape)
        .drawWithContent {
            val unit = 2.dp.toPx().coerceAtMost(size.minDimension / 10f)
            drawPixelIndicatorFoundation(style, palette, unit)
            drawContent()
            drawPixelIndicatorAccent(style, palette, unit)
            drawPixelModeIndicatorPolish(style, palette, unit)
        }
}

private fun DrawScope.drawPixelNavigationFoundation(
    style: PixelStyle,
    palette: PixelPalette,
    unit: Float,
) {
    if (unit <= 0f) return
    val accentAlpha = if (style == PixelStyle.CyberHacker) 0.16f else 0.075f
    if (size.width >= size.height) {
        drawRect(
            color = palette.highlight.copy(alpha = accentAlpha * 0.72f),
            topLeft = Offset(size.width * 0.15f, unit * 0.72f),
            size = Size(size.width * 0.28f, unit * 0.34f),
        )
        drawRect(
            color = palette.shadow.copy(alpha = accentAlpha),
            topLeft = Offset(size.width * 0.36f, size.height - unit * 1.08f),
            size = Size(size.width * 0.49f, unit * 0.38f),
        )
    } else {
        drawRect(
            color = palette.highlight.copy(alpha = accentAlpha * 0.72f),
            topLeft = Offset(unit * 0.72f, size.height * 0.15f),
            size = Size(unit * 0.34f, size.height * 0.28f),
        )
        drawRect(
            color = palette.shadow.copy(alpha = accentAlpha),
            topLeft = Offset(size.width - unit * 1.08f, size.height * 0.36f),
            size = Size(unit * 0.38f, size.height * 0.49f),
        )
    }
}

private fun DrawScope.drawPixelIndicatorFoundation(
    style: PixelStyle,
    palette: PixelPalette,
    unit: Float,
) {
    if (unit <= 0f) return
    val accentAlpha = if (style == PixelStyle.CyberHacker) 0.20f else 0.10f
    drawRect(
        color = palette.highlight.copy(alpha = accentAlpha * 0.75f),
        topLeft = Offset(unit * 1.5f, unit * 0.72f),
        size = Size((size.width - unit * 3f).coerceAtLeast(0f), unit * 0.36f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = accentAlpha),
        topLeft = Offset(size.width * 0.31f, size.height - unit * 1.08f),
        size = Size(size.width * 0.38f, unit * 0.38f),
    )
}

private fun DrawScope.drawPixelBackdropBase(style: PixelStyle, palette: PixelPalette) {
    drawRect(palette.background)
    when (style) {
        PixelStyle.ClassicHandheld -> {
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.76f),
                topLeft = Offset(0f, size.height * 0.68f),
                size = Size(size.width, size.height * 0.32f),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.035f),
                topLeft = Offset(size.width * 0.06f, size.height * 0.11f),
                size = Size(size.width * 0.88f, size.height * 0.52f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        PixelStyle.NeonArcade -> {
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.86f),
                topLeft = Offset(0f, size.height * 0.54f),
                size = Size(size.width, size.height * 0.46f),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.045f),
                topLeft = Offset(0f, size.height * 0.64f),
                size = Size(size.width, 2.dp.toPx()),
            )
        }

        PixelStyle.PastoralFields -> {
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.72f),
                topLeft = Offset(0f, size.height * 0.56f),
                size = Size(size.width, size.height * 0.44f),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.035f),
                topLeft = Offset(0f, size.height * 0.76f),
                size = Size(size.width, size.height * 0.24f),
            )
        }

        PixelStyle.StarVoyage -> {
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.60f),
                topLeft = Offset(0f, size.height * 0.45f),
                size = Size(size.width, size.height * 0.55f),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.035f),
                topLeft = Offset(size.width * 0.12f, size.height * 0.20f),
                size = Size(size.width * 0.76f, size.height * 0.58f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        PixelStyle.InkJade -> {
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.58f),
                topLeft = Offset(0f, size.height * 0.62f),
                size = Size(size.width, size.height * 0.38f),
            )
            drawRect(
                color = palette.highlight.copy(alpha = 0.025f),
                topLeft = Offset(0f, size.height * 0.34f),
                size = Size(size.width, size.height * 0.22f),
            )
        }

        PixelStyle.RustWasteland -> {
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.78f),
                topLeft = Offset(0f, size.height * 0.65f),
                size = Size(size.width, size.height * 0.35f),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.035f),
                topLeft = Offset(0f, size.height * 0.71f),
                size = Size(size.width, 4.dp.toPx()),
            )
        }

        PixelStyle.OceanDepths -> {
            val surfaceY = size.height * 0.18f
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.74f),
                topLeft = Offset(0f, surfaceY),
                size = Size(size.width, size.height - surfaceY),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.040f),
                topLeft = Offset(0f, size.height * 0.54f),
                size = Size(size.width, size.height * 0.46f),
            )
            drawRect(
                color = palette.highlight.copy(alpha = 0.075f),
                topLeft = Offset(0f, surfaceY),
                size = Size(size.width, 2.dp.toPx()),
            )
        }

        PixelStyle.CyberHacker -> {
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.94f),
                topLeft = Offset(0f, size.height * 0.56f),
                size = Size(size.width, size.height * 0.44f),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.075f),
                topLeft = Offset(size.width * 0.04f, size.height * 0.08f),
                size = Size(size.width * 0.92f, size.height * 0.84f),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawRect(
                color = CYBER_MAGENTA.copy(alpha = 0.12f),
                topLeft = Offset(0f, size.height * 0.43f),
                size = Size(size.width * 0.31f, 1.dp.toPx()),
            )
        }

        PixelStyle.ThreeKingdoms -> {
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.76f),
                topLeft = Offset(0f, size.height * 0.66f),
                size = Size(size.width, size.height * 0.34f),
            )
            drawRect(
                color = palette.outline.copy(alpha = 0.045f),
                topLeft = Offset(size.width * 0.06f, size.height * 0.12f),
                size = Size(size.width * 0.88f, size.height * 0.66f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        PixelStyle.BianliangMarket -> {
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.72f),
                topLeft = Offset(0f, size.height * 0.64f),
                size = Size(size.width, size.height * 0.36f),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.038f),
                topLeft = Offset(size.width * 0.05f, size.height * 0.13f),
                size = Size(size.width * 0.90f, size.height * 0.58f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        PixelStyle.FishingHarbor -> {
            val horizon = size.height * 0.58f
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.78f),
                topLeft = Offset(0f, horizon),
                size = Size(size.width, size.height - horizon),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.034f),
                topLeft = Offset(0f, size.height * 0.72f),
                size = Size(size.width, size.height * 0.28f),
            )
            drawRect(
                color = palette.highlight.copy(alpha = 0.060f),
                topLeft = Offset(0f, horizon),
                size = Size(size.width, 2.dp.toPx()),
            )
        }

        PixelStyle.TribalJungle -> {
            val groundY = size.height * 0.72f
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.80f),
                topLeft = Offset(0f, groundY),
                size = Size(size.width, size.height - groundY),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.040f),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 0.24f),
            )
            drawRect(
                color = palette.highlight.copy(alpha = 0.026f),
                topLeft = Offset(size.width * 0.05f, size.height * 0.20f),
                size = Size(size.width * 0.90f, size.height * 0.46f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        PixelStyle.LavaValley -> {
            val valleyY = size.height * 0.66f
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.84f),
                topLeft = Offset(0f, valleyY),
                size = Size(size.width, size.height - valleyY),
            )
            drawRect(
                color = palette.shadow.copy(alpha = 0.10f),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 0.30f),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.038f),
                topLeft = Offset(size.width * 0.05f, size.height * 0.18f),
                size = Size(size.width * 0.90f, size.height * 0.54f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        PixelStyle.DunhuangDesert -> {
            val groundY = size.height * 0.69f
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.78f),
                topLeft = Offset(0f, groundY),
                size = Size(size.width, size.height - groundY),
            )
            drawRect(
                color = palette.secondary.copy(alpha = 0.028f),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 0.34f),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.034f),
                topLeft = Offset(size.width * 0.05f, size.height * 0.18f),
                size = Size(size.width * 0.90f, size.height * 0.54f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        PixelStyle.VikingSnowfield -> {
            val snowY = size.height * 0.68f
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.82f),
                topLeft = Offset(0f, snowY),
                size = Size(size.width, size.height - snowY),
            )
            drawRect(
                color = palette.secondary.copy(alpha = 0.032f),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 0.28f),
            )
            drawRect(
                color = palette.highlight.copy(alpha = 0.050f),
                topLeft = Offset(size.width * 0.05f, size.height * 0.17f),
                size = Size(size.width * 0.90f, size.height * 0.56f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        PixelStyle.JiangnanWatertown -> {
            val riverY = size.height * 0.68f
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.74f),
                topLeft = Offset(0f, riverY),
                size = Size(size.width, size.height - riverY),
            )
            drawRect(
                color = palette.secondary.copy(alpha = 0.026f),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 0.30f),
            )
            drawRect(
                color = palette.highlight.copy(alpha = 0.036f),
                topLeft = Offset(size.width * 0.06f, size.height * 0.16f),
                size = Size(size.width * 0.88f, size.height * 0.58f),
                style = Stroke(width = 0.7.dp.toPx()),
            )
        }

        PixelStyle.CloudTown -> {
            drawRect(
                color = palette.backgroundAlt.copy(alpha = 0.42f),
                topLeft = Offset(0f, size.height * 0.70f),
                size = Size(size.width, size.height * 0.30f),
            )
            drawRoundRect(
                color = palette.highlight.copy(alpha = 0.050f),
                topLeft = Offset(size.width * 0.06f, size.height * 0.14f),
                size = Size(size.width * 0.88f, size.height * 0.60f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                style = Stroke(width = 0.7.dp.toPx()),
            )
            drawRoundRect(
                color = palette.secondary.copy(alpha = 0.030f),
                topLeft = Offset(size.width * 0.15f, size.height * 0.08f),
                size = Size(size.width * 0.70f, 3.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            )
        }
    }
}

private fun DrawScope.drawPixelGrid(style: PixelStyle, palette: PixelPalette) {
    if (
        style == PixelStyle.PastoralFields ||
        style == PixelStyle.InkJade ||
        style == PixelStyle.OceanDepths ||
        style == PixelStyle.ThreeKingdoms ||
        style == PixelStyle.BianliangMarket ||
        style == PixelStyle.FishingHarbor ||
        style == PixelStyle.TribalJungle ||
        style == PixelStyle.LavaValley ||
        style == PixelStyle.DunhuangDesert ||
        style == PixelStyle.VikingSnowfield ||
        style == PixelStyle.JiangnanWatertown ||
        style == PixelStyle.CloudTown
    ) return
    val cell = when (style) {
        PixelStyle.ClassicHandheld -> 28.dp.toPx()
        PixelStyle.NeonArcade -> 36.dp.toPx()
        PixelStyle.StarVoyage -> 44.dp.toPx()
        PixelStyle.RustWasteland -> 40.dp.toPx()
        PixelStyle.CyberHacker -> 24.dp.toPx()
        PixelStyle.ThreeKingdoms -> 34.dp.toPx()
        PixelStyle.BianliangMarket -> 38.dp.toPx()
        PixelStyle.FishingHarbor -> 40.dp.toPx()
        PixelStyle.TribalJungle -> 34.dp.toPx()
        PixelStyle.LavaValley -> 32.dp.toPx()
        PixelStyle.DunhuangDesert -> 38.dp.toPx()
        PixelStyle.VikingSnowfield -> 36.dp.toPx()
        PixelStyle.JiangnanWatertown -> 40.dp.toPx()
        PixelStyle.CloudTown -> 42.dp.toPx()
    }
    val verticalAlpha = when (style) {
        PixelStyle.CyberHacker -> 0.12f
        PixelStyle.NeonArcade -> 0.085f
        PixelStyle.ClassicHandheld -> 0.060f
        PixelStyle.ThreeKingdoms -> 0.050f
        PixelStyle.BianliangMarket -> 0.042f
        PixelStyle.FishingHarbor -> 0.038f
        PixelStyle.TribalJungle -> 0.036f
        PixelStyle.LavaValley -> 0.040f
        PixelStyle.DunhuangDesert -> 0.034f
        PixelStyle.VikingSnowfield -> 0.036f
        PixelStyle.JiangnanWatertown -> 0.030f
        PixelStyle.CloudTown -> 0.026f
        else -> 0.045f
    }
    val horizontalAlpha = verticalAlpha * 0.82f
    var x = cell
    while (x < size.width) {
        drawLine(
            color = palette.outline.copy(alpha = verticalAlpha),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 0.5.dp.toPx(),
        )
        x += cell
    }
    var y = cell
    while (y < size.height) {
        drawLine(
            color = palette.outline.copy(alpha = horizontalAlpha),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.5.dp.toPx(),
        )
        y += cell
    }
}

private fun DrawScope.drawPixelCardMaterial(
    style: PixelStyle,
    palette: PixelPalette,
) {
    val line = 2.dp.toPx().coerceAtMost(size.minDimension / 20f)
    when (style) {
        PixelStyle.ClassicHandheld -> {
            drawRect(palette.surfaceAlt.copy(alpha = 0.20f), Offset.Zero, Size(size.width, line * 4f))
            drawRect(palette.highlight.copy(alpha = 0.045f), Offset(line * 5f, line * 2f), Size(size.width * 0.18f, line))
        }
        PixelStyle.NeonArcade -> {
            drawRect(palette.primary.copy(alpha = 0.055f), Offset.Zero, Size(size.width * 0.50f, line * 3f))
            drawRect(palette.secondary.copy(alpha = 0.055f), Offset(size.width * 0.50f, 0f), Size(size.width * 0.50f, line * 3f))
        }
        PixelStyle.PastoralFields -> {
            drawRect(palette.secondary.copy(alpha = 0.045f), Offset.Zero, Size(size.width, line * 3f))
            drawRect(palette.primary.copy(alpha = 0.035f), Offset(0f, size.height - line * 2f), Size(size.width, line * 2f))
        }
        PixelStyle.StarVoyage -> {
            drawRect(palette.primary.copy(alpha = 0.050f), Offset.Zero, Size(size.width, line * 3f))
            drawRect(palette.secondary.copy(alpha = 0.045f), Offset(size.width * 0.42f, 0f), Size(size.width * 0.16f, line))
        }
        PixelStyle.InkJade -> {
            drawRect(palette.primary.copy(alpha = 0.040f), Offset.Zero, Size(size.width, line * 3f))
            drawRect(palette.secondary.copy(alpha = 0.045f), Offset(size.width * 0.46f, 0f), Size(size.width * 0.08f, line))
        }
        PixelStyle.RustWasteland -> {
            drawRect(palette.shadow.copy(alpha = 0.12f), Offset.Zero, Size(size.width, line * 3f))
            drawRect(palette.primary.copy(alpha = 0.050f), Offset(size.width * 0.14f, 0f), Size(size.width * 0.24f, line * 2f))
            drawRect(palette.secondary.copy(alpha = 0.040f), Offset(size.width * 0.62f, 0f), Size(size.width * 0.16f, line * 2f))
        }
        PixelStyle.OceanDepths -> {
            drawRect(palette.primary.copy(alpha = 0.055f), Offset.Zero, Size(size.width, line * 3f))
            repeat(3) { index ->
                val y = size.height * (0.30f + index * 0.19f)
                val start = size.width * (0.08f + index * 0.11f)
                val width = size.width * (0.38f - index * 0.05f)
                drawRect(
                    color = (if (index == 1) palette.secondary else palette.primary).copy(alpha = 0.035f),
                    topLeft = Offset(start, y),
                    size = Size(width, line * 0.72f),
                )
            }
            drawRect(
                palette.surfaceAlt.copy(alpha = 0.14f),
                Offset(0f, size.height - line * 3f),
                Size(size.width, line * 3f),
            )
        }
        PixelStyle.CyberHacker -> drawCyberCardMaterial(palette, line)
        PixelStyle.ThreeKingdoms -> drawThreeKingdomsCardMaterial(palette, line)
        PixelStyle.BianliangMarket -> drawBianliangCardMaterial(palette, line)
        PixelStyle.FishingHarbor -> drawFishingHarborCardMaterial(palette, line)
        PixelStyle.TribalJungle -> drawTribalJungleCardMaterial(palette, line)
        PixelStyle.LavaValley -> drawLavaValleyCardMaterial(palette, line)
        PixelStyle.DunhuangDesert -> drawDunhuangCardMaterial(palette, line)
        PixelStyle.VikingSnowfield -> drawVikingCardMaterial(palette, line)
        PixelStyle.JiangnanWatertown -> drawJiangnanCardMaterial(palette, line)
        PixelStyle.CloudTown -> drawCloudTownCardMaterial(palette, line)
    }
}

private fun DrawScope.drawHandheldScene(palette: PixelPalette) {
    val unit = 6.dp.toPx()
    val screenTop = size.height * 0.15f
    val screenHeight = size.height * 0.42f
    drawRect(
        color = palette.outline.copy(alpha = 0.075f),
        topLeft = Offset(size.width * 0.08f, screenTop),
        size = Size(size.width * 0.84f, screenHeight),
        style = Stroke(width = unit * 0.45f),
    )
    repeat(4) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.045f + index * 0.008f),
            topLeft = Offset(size.width * 0.12f, screenTop + screenHeight * (0.18f + index * 0.18f)),
            size = Size(size.width * (0.58f - index * 0.06f), unit * 0.55f),
        )
    }
    repeat(7) { index ->
        drawRect(
            color = palette.primary.copy(alpha = 0.08f + index * 0.006f),
            topLeft = Offset(index * size.width * 0.14f, size.height - unit * (2f + index % 3)),
            size = Size(size.width * 0.15f, unit * (2f + index % 3)),
        )
    }
    repeat(3) { index ->
        drawRect(
            color = (if (index == 0) palette.secondary else palette.primary).copy(alpha = 0.28f),
            topLeft = Offset(size.width - unit * (3f + index * 1.8f), 34.dp.toPx()),
            size = Size(unit * 0.72f, unit * 0.72f),
        )
    }
}

private fun DrawScope.drawArcadeScene(palette: PixelPalette) {
    val unit = 5.dp.toPx()
    val horizon = size.height * 0.64f
    drawRect(
        color = palette.secondary.copy(alpha = 0.10f),
        topLeft = Offset(0f, horizon),
        size = Size(size.width, unit * 0.65f),
    )
    repeat(9) { index ->
        val x = size.width * index / 8f
        val vanishingX = size.width / 2f
        val color = if (index % 2 == 0) palette.primary else palette.secondary
        drawLine(
            color = color.copy(alpha = 0.075f),
            start = Offset(vanishingX, horizon),
            end = Offset(x, size.height),
            strokeWidth = unit * 0.35f,
        )
    }
    repeat(5) { index ->
        val progress = (index + 1) / 6f
        val y = horizon + (size.height - horizon) * progress * progress
        drawLine(
            color = (if (index % 2 == 0) palette.primary else palette.secondary).copy(alpha = 0.075f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = unit * 0.35f,
        )
    }
    repeat(6) { index ->
        val color = if (index % 2 == 0) palette.primary else palette.secondary
        drawRect(
            color = color.copy(alpha = 0.16f),
            topLeft = Offset(size.width * (0.10f + index * 0.15f), size.height * (0.16f + index % 2 * 0.05f)),
            size = Size(unit * 3f, unit * 0.72f),
        )
    }
}

private fun DrawScope.drawPastoralScene(palette: PixelPalette) {
    val unit = 5.dp.toPx()
    val fieldTop = size.height * 0.59f
    listOf(
        Offset(size.width * 0.10f, size.height * 0.15f),
        Offset(size.width * 0.63f, size.height * 0.22f),
    ).forEachIndexed { index, origin ->
        drawRect(
            color = palette.highlight.copy(alpha = 0.065f),
            topLeft = origin,
            size = Size(size.width * (0.20f + index * 0.05f), unit * 1.2f),
        )
        drawRect(
            color = palette.highlight.copy(alpha = 0.045f),
            topLeft = origin + Offset(unit * 3f, -unit),
            size = Size(size.width * 0.10f, unit),
        )
    }
    listOf(
        palette.primary.copy(alpha = 0.060f),
        palette.secondary.copy(alpha = 0.052f),
        palette.primary.copy(alpha = 0.085f),
    ).forEachIndexed { index, color ->
        drawRect(
            color = color,
            topLeft = Offset(0f, fieldTop + size.height * index * 0.11f),
            size = Size(size.width, size.height * 0.11f),
        )
    }
    repeat(5) { index ->
        drawRect(
            color = palette.outline.copy(alpha = 0.055f),
            topLeft = Offset(size.width * (0.02f + index * 0.21f), fieldTop + size.height * 0.05f),
            size = Size(size.width * 0.14f, unit * 0.55f),
        )
    }
    val fenceY = size.height * 0.55f
    drawRect(
        color = palette.secondary.copy(alpha = 0.15f),
        topLeft = Offset(size.width * 0.08f, fenceY),
        size = Size(size.width * 0.84f, unit * 0.75f),
    )
    listOf(0.12f, 0.33f, 0.55f, 0.77f, 0.90f).forEach { x ->
        drawRect(
            color = palette.secondary.copy(alpha = 0.17f),
            topLeft = Offset(size.width * x, fenceY - unit * 2f),
            size = Size(unit * 0.72f, unit * 4.5f),
        )
    }
    PASTORAL_FLOWER_POINTS.forEachIndexed { index, point ->
        drawRect(
            color = palette.primary.copy(alpha = 0.16f),
            topLeft = Offset(size.width * point.first, size.height * point.second),
            size = Size(unit * 0.65f, unit * 2f),
        )
        drawRect(
            color = (if (index % 3 == 0) palette.highlight else palette.secondary).copy(alpha = 0.20f),
            topLeft = Offset(size.width * point.first - unit * 0.65f, size.height * point.second - unit),
            size = Size(unit * 2f, unit),
        )
    }
    val cottageX = size.width * 0.70f
    val cottageY = fieldTop - unit * 7f
    drawRect(
        color = palette.secondary.copy(alpha = 0.13f),
        topLeft = Offset(cottageX, cottageY + unit * 2f),
        size = Size(unit * 9f, unit * 5f),
    )
    repeat(3) { level ->
        drawRect(
            color = palette.primary.copy(alpha = 0.14f + level * 0.018f),
            topLeft = Offset(cottageX + unit * level, cottageY + unit * level),
            size = Size(unit * (9f - level * 2f), unit),
        )
    }
    drawRect(
        color = palette.highlight.copy(alpha = 0.15f),
        topLeft = Offset(cottageX + unit * 2f, cottageY + unit * 4f),
        size = Size(unit * 1.5f, unit * 1.5f),
    )
}

private fun DrawScope.drawStarVoyageScene(palette: PixelPalette) {
    val unit = 3.dp.toPx()
    PIXEL_STAR_POINTS.forEachIndexed { index, point ->
        val side = if (index % 5 == 0) unit * 2f else unit
        drawRect(
            color = (if (index % 4 == 0) palette.secondary else palette.highlight).copy(alpha = 0.25f),
            topLeft = Offset(size.width * point.first, size.height * point.second),
            size = Size(side, side),
        )
    }
    val center = Offset(size.width * 0.76f, size.height * 0.23f)
    drawCircle(
        color = palette.primary.copy(alpha = 0.085f),
        radius = 42.dp.toPx(),
        center = center,
    )
    drawCircle(
        color = palette.highlight.copy(alpha = 0.055f),
        radius = 25.dp.toPx(),
        center = center - Offset(8.dp.toPx(), 8.dp.toPx()),
    )
    drawLine(
        color = palette.secondary.copy(alpha = 0.21f),
        start = Offset(center.x - 58.dp.toPx(), center.y + 12.dp.toPx()),
        end = Offset(center.x + 58.dp.toPx(), center.y - 12.dp.toPx()),
        strokeWidth = unit * 0.75f,
    )
    val route = listOf(
        Offset(size.width * 0.14f, size.height * 0.71f),
        Offset(size.width * 0.34f, size.height * 0.64f),
        Offset(size.width * 0.53f, size.height * 0.73f),
    )
    route.zipWithNext().forEach { (start, end) ->
        drawLine(palette.primary.copy(alpha = 0.075f), start, end, unit * 0.55f)
    }
    route.forEachIndexed { index, point ->
        val side = if (index == 1) unit * 1.6f else unit
        drawRect(
            color = (if (index == 1) palette.secondary else palette.primary).copy(alpha = 0.20f),
            topLeft = point - Offset(side / 2f, side / 2f),
            size = Size(side, side),
        )
    }
}

private fun DrawScope.drawInkJadeScene(palette: PixelPalette) {
    val unit = 5.dp.toPx()
    drawCircle(
        color = palette.secondary.copy(alpha = 0.075f),
        radius = 34.dp.toPx(),
        center = Offset(size.width * 0.79f, size.height * 0.20f),
    )
    val mountains = listOf(
        Triple(0.08f, 0.78f, 5),
        Triple(0.30f, 0.73f, 9),
        Triple(0.58f, 0.80f, 6),
        Triple(0.84f, 0.71f, 10),
    )
    mountains.forEachIndexed { index, (centerX, baseY, levels) ->
        repeat(levels) { level ->
            val width = unit * (2.2f + level * 2.1f)
            drawRect(
                color = palette.primary.copy(alpha = 0.050f + index * 0.010f + level * 0.006f),
                topLeft = Offset(
                    size.width * centerX - width / 2f,
                    size.height * baseY - unit * (levels - level),
                ),
                size = Size(width, unit * 1.25f),
            )
        }
    }
    listOf(
        Triple(0.06f, 0.39f, 0.38f),
        Triple(0.30f, 0.50f, 0.54f),
        Triple(0.60f, 0.40f, 0.28f),
    ).forEachIndexed { index, (startX, y, widthFraction) ->
        drawRect(
            color = (if (index == 1) palette.secondary else palette.highlight).copy(alpha = 0.060f),
            topLeft = Offset(size.width * startX, size.height * y),
            size = Size(size.width * widthFraction, unit * 0.72f),
        )
    }
    val sealSize = unit * 6f
    val sealOrigin = Offset(size.width * 0.10f, size.height * 0.18f)
    drawRect(
        color = palette.secondary.copy(alpha = 0.17f),
        topLeft = sealOrigin,
        size = Size(sealSize, sealSize),
        style = Stroke(width = unit * 0.48f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.19f),
        topLeft = sealOrigin + Offset(unit * 2f, unit * 2f),
        size = Size(unit * 2f, unit * 2f),
    )
}

private fun DrawScope.drawRustWastelandScene(palette: PixelPalette) {
    val unit = 5.dp.toPx()
    val horizon = size.height * 0.70f
    drawCircle(
        color = palette.primary.copy(alpha = 0.075f),
        radius = 46.dp.toPx(),
        center = Offset(size.width * 0.72f, size.height * 0.23f),
    )
    drawRect(
        color = palette.primary.copy(alpha = 0.085f),
        topLeft = Offset(0f, horizon),
        size = Size(size.width, unit * 1.4f),
    )
    val ruins = listOf(
        Triple(0.05f, 0.10f, 7f),
        Triple(0.21f, 0.16f, 11f),
        Triple(0.47f, 0.09f, 5f),
        Triple(0.66f, 0.18f, 9f),
        Triple(0.88f, 0.08f, 6f),
    )
    ruins.forEachIndexed { index, (x, widthFraction, heightUnits) ->
        val width = size.width * widthFraction
        val height = unit * heightUnits
        drawRect(
            color = (if (index % 2 == 0) palette.primary else palette.outline).copy(alpha = 0.12f),
            topLeft = Offset(size.width * x, horizon - height),
            size = Size(width, height),
        )
        drawRect(
            color = palette.shadow.copy(alpha = 0.30f),
            topLeft = Offset(size.width * x + unit, horizon - height + unit * 2f),
            size = Size(unit * 1.5f, unit * 2f),
        )
    }
    WASTELAND_DUST_POINTS.forEachIndexed { index, point ->
        val side = if (index % 4 == 0) unit else unit * 0.55f
        drawRect(
            color = (if (index % 3 == 0) palette.secondary else palette.primary).copy(alpha = 0.15f),
            topLeft = Offset(size.width * point.first, size.height * point.second),
            size = Size(side, side),
        )
    }
    val towerX = size.width * 0.80f
    drawRect(
        color = palette.secondary.copy(alpha = 0.15f),
        topLeft = Offset(towerX, horizon - unit * 13f),
        size = Size(unit, unit * 13f),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.18f),
        topLeft = Offset(towerX - unit * 2f, horizon - unit * 11f),
        size = Size(unit * 5f, unit),
    )
    drawLine(
        color = palette.secondary.copy(alpha = 0.11f),
        start = Offset(towerX - unit * 3f, horizon),
        end = Offset(towerX, horizon - unit * 13f),
        strokeWidth = unit * 0.45f,
    )
    drawLine(
        color = palette.secondary.copy(alpha = 0.11f),
        start = Offset(towerX + unit * 3f, horizon),
        end = Offset(towerX, horizon - unit * 13f),
        strokeWidth = unit * 0.45f,
    )
}

private fun DrawScope.drawOceanDepthsScene(palette: PixelPalette) {
    val unit = 5.dp.toPx()
    val surfaceY = size.height * 0.18f
    val seabedY = size.height * 0.84f

    repeat(12) { index ->
        val segmentWidth = size.width / 10f
        val x = segmentWidth * index - segmentWidth
        val lift = if (index % 2 == 0) unit else 0f
        drawRect(
            color = palette.primary.copy(alpha = 0.10f),
            topLeft = Offset(x, surfaceY - lift),
            size = Size(segmentWidth + unit, unit),
        )
        if (index % 3 == 1) {
            drawRect(
                color = palette.highlight.copy(alpha = 0.11f),
                topLeft = Offset(x + unit, surfaceY - lift - unit * 0.65f),
                size = Size(segmentWidth * 0.42f, unit * 0.55f),
            )
        }
    }

    listOf(0.16f, 0.39f, 0.68f).forEachIndexed { index, x ->
        val columnWidth = size.width * (0.055f + index * 0.012f)
        drawRect(
            color = palette.highlight.copy(alpha = 0.022f + index * 0.008f),
            topLeft = Offset(size.width * x, surfaceY + unit * (index + 1)),
            size = Size(columnWidth, size.height * (0.34f + index * 0.06f)),
        )
    }

    val fish = listOf(
        Triple(0.17f, 0.39f, false),
        Triple(0.72f, 0.46f, true),
        Triple(0.42f, 0.63f, false),
        Triple(0.82f, 0.70f, true),
    )
    fish.forEachIndexed { index, (x, y, facesLeft) ->
        val bodyWidth = unit * (2.4f + index % 2 * 0.7f)
        val bodyHeight = unit * 1.15f
        val bodyX = size.width * x
        val color = (if (index % 2 == 0) palette.primary else palette.secondary)
            .copy(alpha = 0.13f)
        drawRect(color, Offset(bodyX, size.height * y), Size(bodyWidth, bodyHeight))
        val tailX = if (facesLeft) bodyX + bodyWidth else bodyX - unit
        drawRect(
            color = color.copy(alpha = color.alpha * 0.82f),
            topLeft = Offset(tailX, size.height * y + unit * 0.3f),
            size = Size(unit, unit * 0.65f),
        )
        val eyeX = if (facesLeft) bodyX + unit * 0.35f else bodyX + bodyWidth - unit * 0.65f
        drawRect(
            color = palette.highlight.copy(alpha = 0.18f),
            topLeft = Offset(eyeX, size.height * y + unit * 0.2f),
            size = Size(unit * 0.35f, unit * 0.35f),
        )
    }

    OCEAN_BUBBLE_POINTS.forEachIndexed { index, point ->
        val side = unit * (0.55f + index % 3 * 0.28f)
        drawRect(
            color = palette.highlight.copy(alpha = 0.10f + index % 2 * 0.035f),
            topLeft = Offset(size.width * point.first, size.height * point.second),
            size = Size(side, side),
            style = Stroke(width = unit * 0.28f),
        )
    }

    drawRect(
        color = palette.outline.copy(alpha = 0.12f),
        topLeft = Offset(0f, seabedY),
        size = Size(size.width, size.height - seabedY),
    )
    repeat(11) { index ->
        val blockWidth = size.width / 10f
        val top = seabedY - unit * (index % 3)
        drawRect(
            color = (if (index % 4 == 0) palette.secondary else palette.primary)
                .copy(alpha = if (index % 4 == 0) 0.12f else 0.09f),
            topLeft = Offset(index * blockWidth - unit, top),
            size = Size(blockWidth + unit, size.height - top),
        )
    }
    listOf(0.10f to 7f, 0.18f to 10f, 0.27f to 6f, 0.62f to 8f, 0.69f to 11f).forEachIndexed { index, item ->
        val stemX = size.width * item.first
        val stemHeight = unit * item.second
        val stemColor = palette.primary.copy(alpha = 0.13f + index % 2 * 0.025f)
        drawRect(stemColor, Offset(stemX, seabedY - stemHeight), Size(unit * 0.75f, stemHeight))
        drawRect(
            stemColor,
            Offset(stemX - unit, seabedY - stemHeight * 0.72f),
            Size(unit * 1.8f, unit * 0.7f),
        )
    }
    val coralX = size.width * 0.84f
    drawRect(
        palette.secondary.copy(alpha = 0.16f),
        Offset(coralX, seabedY - unit * 9f),
        Size(unit, unit * 9f),
    )
    listOf(-3f to 6f, 2f to 5f, 4f to 8f).forEach { (xOffset, height) ->
        drawRect(
            palette.secondary.copy(alpha = 0.15f),
            Offset(coralX + unit * xOffset, seabedY - unit * height),
            Size(unit, unit * height),
        )
        drawRect(
            palette.secondary.copy(alpha = 0.13f),
            Offset(coralX + unit * minOf(xOffset, 0f), seabedY - unit * height),
            Size(unit * (kotlin.math.abs(xOffset) + 1f), unit),
        )
    }
}

private fun DrawScope.drawPixelCardUnderlay(style: PixelStyle, palette: PixelPalette) {
    val unit = 3.dp.toPx().coerceAtMost(size.minDimension / 10f)
    drawPixelCardPatternUnderlay(
        pattern = style.cardPattern(),
        unit = unit,
        primary = palette.primary,
        secondary = palette.secondary,
        highlight = palette.highlight,
        shadow = palette.shadow,
    )
}

private fun DrawScope.drawPixelCardDecoration(style: PixelStyle, palette: PixelPalette) {
    val unit = 3.dp.toPx().coerceAtMost(size.minDimension / 10f)
    drawPixelCardPatternOverlay(
        pattern = style.cardPattern(),
        unit = unit,
        primary = palette.primary,
        secondary = palette.secondary,
        highlight = palette.highlight,
        shadow = palette.shadow,
    )
}

internal fun PixelStyle.cardPattern(): PixelCardPattern = when (this) {
    PixelStyle.ClassicHandheld -> PixelCardPattern.Handheld
    PixelStyle.NeonArcade -> PixelCardPattern.Arcade
    PixelStyle.PastoralFields -> PixelCardPattern.Pastoral
    PixelStyle.StarVoyage -> PixelCardPattern.StarVoyage
    PixelStyle.InkJade -> PixelCardPattern.InkJade
    PixelStyle.RustWasteland -> PixelCardPattern.Wasteland
    PixelStyle.OceanDepths -> PixelCardPattern.Ocean
    PixelStyle.CyberHacker -> PixelCardPattern.Cyber
    PixelStyle.ThreeKingdoms -> PixelCardPattern.ThreeKingdoms
    PixelStyle.BianliangMarket -> PixelCardPattern.Bianliang
    PixelStyle.FishingHarbor -> PixelCardPattern.FishingHarbor
    PixelStyle.TribalJungle -> PixelCardPattern.TribalJungle
    PixelStyle.LavaValley -> PixelCardPattern.LavaValley
    PixelStyle.DunhuangDesert -> PixelCardPattern.DunhuangDesert
    PixelStyle.VikingSnowfield -> PixelCardPattern.VikingSnowfield
    PixelStyle.JiangnanWatertown -> PixelCardPattern.JiangnanWatertown
    PixelStyle.CloudTown -> PixelCardPattern.CloudTown
}

private fun DrawScope.drawPixelHudAccent(
    style: PixelStyle,
    palette: PixelPalette,
    top: Float,
    unit: Float,
) {
    val centerX = size.width / 2f
    when (style) {
        PixelStyle.ClassicHandheld -> {
            drawRect(palette.shadow.copy(alpha = 0.54f), Offset(centerX - unit * 4f, top), Size(unit * 8f, unit))
            drawRect(palette.primary.copy(alpha = 0.64f), Offset(centerX - unit * 3f, top + unit), Size(unit * 6f, unit))
            drawRect(palette.secondary.copy(alpha = 0.78f), Offset(centerX + unit * 2f, top + unit), Size(unit, unit))
        }
        PixelStyle.NeonArcade -> {
            drawRect(palette.primary.copy(alpha = 0.72f), Offset(centerX - unit * 5f, top), Size(unit * 4.5f, unit))
            drawRect(palette.secondary.copy(alpha = 0.72f), Offset(centerX + unit * 0.5f, top), Size(unit * 4.5f, unit))
            drawRect(palette.highlight.copy(alpha = 0.60f), Offset(centerX - unit, top + unit * 1.5f), Size(unit * 2f, unit * 0.55f))
        }
        PixelStyle.PastoralFields -> {
            drawRect(palette.secondary.copy(alpha = 0.56f), Offset(centerX - unit * 5f, top + unit), Size(unit * 10f, unit * 0.65f))
            listOf(-3f, 0f, 3f).forEachIndexed { index, x ->
                drawRect(palette.primary.copy(alpha = 0.66f), Offset(centerX + unit * x, top), Size(unit * 0.7f, unit * 1.5f))
                if (index != 1) {
                    drawRect(palette.highlight.copy(alpha = 0.66f), Offset(centerX + unit * (x - 0.4f), top), Size(unit * 1.5f, unit * 0.65f))
                }
            }
        }
        PixelStyle.StarVoyage -> {
            drawRect(palette.primary.copy(alpha = 0.66f), Offset(centerX - unit * 4f, top + unit), Size(unit * 8f, unit * 0.55f))
            drawRect(palette.secondary.copy(alpha = 0.74f), Offset(centerX - unit * 0.5f, top - unit), Size(unit, unit * 3f))
            drawRect(palette.highlight.copy(alpha = 0.68f), Offset(centerX - unit * 1.5f, top), Size(unit * 3f, unit))
        }
        PixelStyle.InkJade -> {
            drawRect(
                color = palette.secondary.copy(alpha = 0.64f),
                topLeft = Offset(centerX - unit * 2f, top - unit),
                size = Size(unit * 4f, unit * 0.75f),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.68f),
                topLeft = Offset(centerX - unit * 4f, top),
                size = Size(unit * 8f, unit * 0.65f),
            )
            drawRect(palette.primary.copy(alpha = 0.58f), Offset(centerX - unit * 2.5f, top + unit), Size(unit * 5f, unit * 0.55f))
        }
        PixelStyle.RustWasteland -> {
            drawRect(
                color = palette.primary.copy(alpha = 0.62f),
                topLeft = Offset(centerX - unit * 5f, top),
                size = Size(unit * 4f, unit),
            )
            drawRect(
                color = palette.secondary.copy(alpha = 0.66f),
                topLeft = Offset(centerX + unit, top),
                size = Size(unit * 3f, unit),
            )
            drawRect(
                color = palette.outline.copy(alpha = 0.80f),
                topLeft = Offset(centerX - unit * 0.5f, top - unit),
                size = Size(unit, unit * 3f),
            )
        }
        PixelStyle.OceanDepths -> {
            repeat(5) { index ->
                val x = centerX + unit * (index - 2.5f) * 1.6f
                val lift = if (index % 2 == 0) unit * 0.7f else 0f
                drawRect(
                    color = palette.primary.copy(alpha = 0.68f),
                    topLeft = Offset(x, top + unit - lift),
                    size = Size(unit * 1.7f, unit * 0.62f),
                )
                if (index == 1 || index == 4) {
                    drawRect(
                        color = palette.highlight.copy(alpha = 0.70f),
                        topLeft = Offset(x + unit * 0.35f, top + unit * 0.35f - lift),
                        size = Size(unit * 0.7f, unit * 0.42f),
                    )
                }
            }
            drawRect(
                color = palette.secondary.copy(alpha = 0.78f),
                topLeft = Offset(centerX + unit * 3.8f, top - unit),
                size = Size(unit * 0.65f, unit * 2.2f),
            )
        }
        PixelStyle.CyberHacker -> drawCyberHudAccent(palette, top, unit)
        PixelStyle.ThreeKingdoms -> drawThreeKingdomsHudAccent(palette, top, unit)
        PixelStyle.BianliangMarket -> drawBianliangHudAccent(palette, top, unit)
        PixelStyle.FishingHarbor -> drawFishingHarborHudAccent(palette, top, unit)
        PixelStyle.TribalJungle -> drawTribalJungleHudAccent(palette, top, unit)
        PixelStyle.LavaValley -> drawLavaValleyHudAccent(palette, top, unit)
        PixelStyle.DunhuangDesert -> drawDunhuangHudAccent(palette, top, unit)
        PixelStyle.VikingSnowfield -> drawVikingHudAccent(palette, top, unit)
        PixelStyle.JiangnanWatertown -> drawJiangnanHudAccent(palette, top, unit)
        PixelStyle.CloudTown -> drawCloudTownHudAccent(palette, top, unit)
    }
}

private fun DrawScope.drawPixelNavigationAccent(
    style: PixelStyle,
    palette: PixelPalette,
    unit: Float,
) {
    val centerX = size.width / 2f
    when (style) {
        PixelStyle.ClassicHandheld -> {
            drawRect(palette.shadow.copy(alpha = 0.58f), Offset(centerX - unit * 4f, 0f), Size(unit * 8f, unit))
            drawRect(palette.primary.copy(alpha = 0.62f), Offset(centerX - unit * 3f, unit), Size(unit * 6f, unit * 0.55f))
            repeat(3) { index ->
                drawRect(
                    palette.secondary.copy(alpha = 0.58f),
                    Offset(size.width - unit * (5f - index * 1.3f), size.height - unit * 2.5f),
                    Size(unit * 0.65f, unit * 0.65f),
                )
            }
        }
        PixelStyle.NeonArcade -> {
            drawRect(palette.primary.copy(alpha = 0.72f), Offset(size.width * 0.20f, 0f), Size(size.width * 0.28f, unit))
            drawRect(palette.secondary.copy(alpha = 0.72f), Offset(size.width * 0.52f, 0f), Size(size.width * 0.28f, unit))
            drawRect(palette.highlight.copy(alpha = 0.58f), Offset(centerX - unit, unit), Size(unit * 2f, unit * 0.55f))
        }
        PixelStyle.PastoralFields -> {
            drawRect(palette.secondary.copy(alpha = 0.56f), Offset(centerX - unit * 5f, 0f), Size(unit * 10f, unit * 0.65f))
            listOf(-3f, 0f, 3f).forEach { x ->
                drawRect(palette.primary.copy(alpha = 0.62f), Offset(centerX + unit * x, 0f), Size(unit * 0.65f, unit * 2f))
            }
        }
        PixelStyle.StarVoyage -> {
            drawRect(palette.primary.copy(alpha = 0.62f), Offset(centerX - unit * 5f, 0f), Size(unit * 10f, unit * 0.55f))
            drawRect(palette.secondary.copy(alpha = 0.72f), Offset(centerX - unit * 0.5f, 0f), Size(unit, unit * 2f))
            listOf(unit * 2f, size.width - unit * 3f).forEach { x ->
                drawRect(palette.highlight.copy(alpha = 0.62f), Offset(x, unit * 2f), Size(unit, unit))
            }
        }
        PixelStyle.InkJade -> {
            drawRect(
                color = palette.secondary.copy(alpha = 0.76f),
                topLeft = Offset(centerX - unit * 2f, unit),
                size = Size(unit * 4f, unit),
            )
            drawRect(
                color = palette.primary.copy(alpha = 0.70f),
                topLeft = Offset(centerX - unit, unit * 2f),
                size = Size(unit * 2f, unit),
            )
        }
        PixelStyle.RustWasteland -> {
            listOf(
                Offset(unit * 2f, unit * 2f),
                Offset(size.width - unit * 3f, unit * 2f),
            ).forEach { origin ->
                drawRect(
                    color = palette.secondary.copy(alpha = 0.72f),
                    topLeft = origin,
                    size = Size(unit, unit),
                )
            }
            drawRect(
                color = palette.primary.copy(alpha = 0.66f),
                topLeft = Offset(size.width * 0.42f, 0f),
                size = Size(size.width * 0.12f, unit),
            )
        }
        PixelStyle.OceanDepths -> {
            repeat(6) { index ->
                val segmentWidth = size.width * 0.11f
                val x = size.width * 0.17f + segmentWidth * index
                val lift = if (index % 2 == 0) unit * 0.65f else 0f
                drawRect(
                    color = palette.primary.copy(alpha = 0.68f),
                    topLeft = Offset(x, unit - lift),
                    size = Size(segmentWidth + unit * 0.2f, unit * 0.68f),
                )
            }
            listOf(unit * 3f, size.width - unit * 4f).forEachIndexed { index, x ->
                drawRect(
                    color = (if (index == 0) palette.highlight else palette.secondary).copy(alpha = 0.70f),
                    topLeft = Offset(x, size.height - unit * 2.5f),
                    size = Size(unit, unit),
                    style = Stroke(width = unit * 0.34f),
                )
            }
        }
        PixelStyle.CyberHacker -> drawCyberNavigationAccent(palette, unit)
        PixelStyle.ThreeKingdoms -> drawThreeKingdomsNavigationAccent(palette, unit)
        PixelStyle.BianliangMarket -> drawBianliangNavigationAccent(palette, unit)
        PixelStyle.FishingHarbor -> drawFishingHarborNavigationAccent(palette, unit)
        PixelStyle.TribalJungle -> drawTribalJungleNavigationAccent(palette, unit)
        PixelStyle.LavaValley -> drawLavaValleyNavigationAccent(palette, unit)
        PixelStyle.DunhuangDesert -> drawDunhuangNavigationAccent(palette, unit)
        PixelStyle.VikingSnowfield -> drawVikingNavigationAccent(palette, unit)
        PixelStyle.JiangnanWatertown -> drawJiangnanNavigationAccent(palette, unit)
        PixelStyle.CloudTown -> drawCloudTownNavigationAccent(palette, unit)
    }
}

private fun DrawScope.drawPixelIndicatorAccent(
    style: PixelStyle,
    palette: PixelPalette,
    unit: Float,
) {
    when (style) {
        PixelStyle.ClassicHandheld -> {
            drawRect(palette.shadow.copy(alpha = 0.62f), Offset(unit * 2f, unit), Size(unit * 4f, unit))
            drawRect(palette.secondary.copy(alpha = 0.82f), Offset(unit * 2f, unit), Size(unit, unit))
        }
        PixelStyle.NeonArcade -> {
            drawRect(palette.primary.copy(alpha = 0.82f), Offset(unit * 2f, unit), Size(unit * 3f, unit))
            drawRect(palette.secondary.copy(alpha = 0.82f), Offset(size.width - unit * 5f, unit), Size(unit * 3f, unit))
        }
        PixelStyle.PastoralFields -> {
            listOf(2f, 3.5f, 5f).forEachIndexed { index, x ->
                drawRect(
                    palette.primary.copy(alpha = 0.76f),
                    Offset(unit * x, unit * (1.5f - index * 0.3f)),
                    Size(unit * 0.65f, unit * (1f + index * 0.3f)),
                )
            }
            drawRect(palette.secondary.copy(alpha = 0.74f), Offset(size.width - unit * 4f, unit), Size(unit * 2f, unit))
        }
        PixelStyle.StarVoyage -> {
            val center = Offset(size.width - unit * 4f, unit * 2f)
            drawRect(palette.highlight.copy(alpha = 0.82f), center - Offset(unit * 0.5f, unit * 1.5f), Size(unit, unit * 3f))
            drawRect(palette.highlight.copy(alpha = 0.82f), center - Offset(unit * 1.5f, unit * 0.5f), Size(unit * 3f, unit))
        }
        PixelStyle.InkJade -> {
            drawRect(palette.secondary.copy(alpha = 0.78f), Offset(size.width * 0.42f, 0f), Size(size.width * 0.16f, unit))
            drawRect(palette.primary.copy(alpha = 0.74f), Offset(size.width * 0.46f, unit), Size(size.width * 0.08f, unit))
        }
        PixelStyle.RustWasteland -> {
            listOf(Offset(unit * 2f, unit), Offset(size.width - unit * 3f, unit)).forEach { origin ->
                drawRect(palette.secondary.copy(alpha = 0.80f), origin, Size(unit, unit))
                drawRect(palette.shadow.copy(alpha = 0.72f), origin + Offset(unit * 0.25f, unit * 0.25f), Size(unit * 0.5f, unit * 0.5f))
            }
        }
        PixelStyle.OceanDepths -> {
            repeat(4) { index ->
                val x = size.width * 0.22f + unit * index * 1.35f
                val lift = if (index % 2 == 0) unit * 0.55f else 0f
                drawRect(
                    color = palette.primary.copy(alpha = 0.80f),
                    topLeft = Offset(x, unit * 1.35f - lift),
                    size = Size(unit * 1.45f, unit * 0.62f),
                )
            }
            drawRect(
                color = palette.secondary.copy(alpha = 0.84f),
                topLeft = Offset(size.width - unit * 3f, unit),
                size = Size(unit, unit),
            )
        }
        PixelStyle.CyberHacker -> drawCyberIndicatorAccent(palette, unit)
        PixelStyle.ThreeKingdoms -> drawThreeKingdomsIndicatorAccent(palette, unit)
        PixelStyle.BianliangMarket -> drawBianliangIndicatorAccent(palette, unit)
        PixelStyle.FishingHarbor -> drawFishingHarborIndicatorAccent(palette, unit)
        PixelStyle.TribalJungle -> drawTribalJungleIndicatorAccent(palette, unit)
        PixelStyle.LavaValley -> drawLavaValleyIndicatorAccent(palette, unit)
        PixelStyle.DunhuangDesert -> drawDunhuangIndicatorAccent(palette, unit)
        PixelStyle.VikingSnowfield -> drawVikingIndicatorAccent(palette, unit)
        PixelStyle.JiangnanWatertown -> drawJiangnanIndicatorAccent(palette, unit)
        PixelStyle.CloudTown -> drawCloudTownIndicatorAccent(palette, unit)
    }
}

private fun DrawScope.drawPixelFrame(unit: Float, primary: Color, secondary: Color) {
    val long = unit * 5f
    drawRect(primary, Offset(0f, 0f), Size(long, unit))
    drawRect(primary, Offset(0f, 0f), Size(unit, long))
    drawRect(primary, Offset(size.width - long, 0f), Size(long, unit))
    drawRect(primary, Offset(size.width - unit, 0f), Size(unit, long))
    drawRect(primary, Offset(0f, size.height - unit), Size(long, unit))
    drawRect(primary, Offset(0f, size.height - long), Size(unit, long))
    drawRect(primary, Offset(size.width - long, size.height - unit), Size(long, unit))
    drawRect(primary, Offset(size.width - unit, size.height - long), Size(unit, long))
    drawRect(secondary, Offset(unit * 2f, 0f), Size(unit, unit))
    drawRect(secondary, Offset(size.width - unit * 3f, size.height - unit), Size(unit, unit))
}

private fun DrawScope.drawPixelCorner(origin: Offset, unit: Float, color: Color, right: Boolean) {
    val direction = if (right) -1f else 1f
    drawRect(
        color = color,
        topLeft = Offset(if (right) origin.x - unit * 4f else origin.x, origin.y),
        size = Size(unit * 4f, unit),
    )
    drawRect(
        color = color,
        topLeft = Offset(if (right) origin.x - unit else origin.x, origin.y),
        size = Size(unit, unit * 3f),
    )
    drawRect(
        color = color.copy(alpha = color.alpha * 0.72f),
        topLeft = Offset(origin.x + direction * unit * 5f - if (right) unit else 0f, origin.y),
        size = Size(unit, unit),
    )
}

private val PIXEL_STAR_POINTS = listOf(
    0.08f to 0.12f,
    0.18f to 0.31f,
    0.29f to 0.18f,
    0.42f to 0.39f,
    0.54f to 0.11f,
    0.67f to 0.34f,
    0.83f to 0.16f,
    0.91f to 0.43f,
    0.12f to 0.64f,
    0.34f to 0.76f,
    0.57f to 0.59f,
    0.78f to 0.82f,
)

private val WASTELAND_DUST_POINTS = listOf(
    0.08f to 0.18f,
    0.17f to 0.39f,
    0.29f to 0.24f,
    0.38f to 0.56f,
    0.52f to 0.17f,
    0.61f to 0.43f,
    0.72f to 0.30f,
    0.84f to 0.12f,
    0.93f to 0.48f,
    0.13f to 0.63f,
    0.44f to 0.68f,
    0.69f to 0.59f,
)

private val OCEAN_BUBBLE_POINTS = listOf(
    0.08f to 0.31f,
    0.13f to 0.55f,
    0.26f to 0.48f,
    0.34f to 0.72f,
    0.51f to 0.37f,
    0.59f to 0.58f,
    0.73f to 0.29f,
    0.79f to 0.61f,
    0.90f to 0.43f,
)

private val PASTORAL_FLOWER_POINTS = listOf(
    0.08f to 0.69f,
    0.16f to 0.76f,
    0.27f to 0.66f,
    0.39f to 0.82f,
    0.52f to 0.72f,
    0.63f to 0.86f,
    0.79f to 0.78f,
    0.91f to 0.68f,
)
