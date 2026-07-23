package me.weishu.kernelsu.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults as MaterialCardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.theme.LocalBlurIntensity
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

enum class FrostedGlassCardStyle {
    Mist,
    Ice,
    Pearl,
}

object FrostedGlassTokens {
    val Background = Color(0xFFF0F4F7)
    val Surface = Color(0xFFF8FBFD)
    val SurfaceTint = Color(0xFFEAF0F4)
    val Frost = Color(0xFFDCE6EC)
    val Ice = Color(0xFFCDE6EE)
    val Pearl = Color(0xFFE6DFEC)
    val Stroke = Color.White
    val SubtleStroke = Color(0xFFC9D5DE)
    val PressedOverlay = Color(0xFFE7EDF1)
    val Shadow = Color(0xFF6D7D89)
    val DarkBackground = Color(0xFF11171C)
    val DarkSurface = Color(0xFF202A31)
    val DarkSurfaceTint = Color(0xFF2A353D)
    val DarkFrost = Color(0xFF33424C)
    val DarkIce = Color(0xFF294752)
    val DarkPearl = Color(0xFF443B4C)
    val DarkStroke = Color(0xFFDDEBF2)
    val DarkSubtleStroke = Color(0xFF53636E)
    val DarkShadow = Color(0xFF05080A)
}

@Composable
@ReadOnlyComposable
fun isLiquidGlassTheme(): Boolean {
    return LocalInterfaceStyle.current == InterfaceStyle.LiquidGlass.value
}

@Composable
@ReadOnlyComposable
fun liquidGlassBackdropColor(): Color {
    return if (isLiquidGlassTheme()) {
        if (isInDarkTheme()) FrostedGlassTokens.DarkBackground else FrostedGlassTokens.Background
    } else {
        MiuixTheme.colorScheme.surface
    }
}

@Composable
@ReadOnlyComposable
fun liquidGlassSurfaceColor(): Color {
    return if (isInDarkTheme()) FrostedGlassTokens.DarkSurface else FrostedGlassTokens.Surface
}

fun Modifier.liquidGlassSurface(
    backdrop: Backdrop?,
    shape: Shape,
    surfaceColor: Color = FrostedGlassTokens.Surface,
    surfaceAlpha: Float = 0.54f,
    blurRadius: Dp = 16.dp,
    enableRefraction: Boolean = false,
    refractionHeight: Dp = 16.dp,
    refractionAmount: Dp = 10.dp,
    chromaticAberration: Float = 0.22f,
    strokeAlpha: Float = 0.70f,
    darkMode: Boolean = false,
    cardStyle: FrostedGlassCardStyle = FrostedGlassCardStyle.Mist,
): Modifier {
    val boundedAlpha = surfaceAlpha.coerceIn(0f, 1f)
    val glassBase = surfaceColor.copy(alpha = boundedAlpha)
    val sheen = if (darkMode) FrostedGlassTokens.DarkSurfaceTint else FrostedGlassTokens.SurfaceTint
    val frost = if (darkMode) FrostedGlassTokens.DarkFrost else FrostedGlassTokens.Frost
    val stroke = if (darkMode) FrostedGlassTokens.DarkStroke else FrostedGlassTokens.Stroke
    val subtleStroke = if (darkMode) FrostedGlassTokens.DarkSubtleStroke else FrostedGlassTokens.SubtleStroke
    val shadow = if (darkMode) FrostedGlassTokens.DarkShadow else FrostedGlassTokens.Shadow
    val styleTint = when (cardStyle) {
        FrostedGlassCardStyle.Mist -> frost
        FrostedGlassCardStyle.Ice -> if (darkMode) FrostedGlassTokens.DarkIce else FrostedGlassTokens.Ice
        FrostedGlassCardStyle.Pearl -> if (darkMode) FrostedGlassTokens.DarkPearl else FrostedGlassTokens.Pearl
    }
    val frostSheen = Brush.verticalGradient(
        listOf(
            stroke.copy(alpha = if (darkMode) 0.12f else 0.34f),
            sheen.copy(alpha = if (darkMode) 0.30f else 0.18f),
            styleTint.copy(alpha = if (darkMode) 0.30f else 0.24f),
        )
    )
    val materialTint = when (cardStyle) {
        FrostedGlassCardStyle.Mist -> Brush.horizontalGradient(
            listOf(Color.Transparent, styleTint.copy(alpha = 0.18f), Color.Transparent)
        )
        FrostedGlassCardStyle.Ice -> Brush.linearGradient(
            listOf(styleTint.copy(alpha = 0.30f), Color.Transparent, sheen.copy(alpha = 0.18f))
        )
        FrostedGlassCardStyle.Pearl -> Brush.horizontalGradient(
            listOf(styleTint.copy(alpha = 0.22f), stroke.copy(alpha = 0.10f), styleTint.copy(alpha = 0.18f))
        )
    }
    val material = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx(), blurRadius.toPx())
                if (enableRefraction) {
                    lens(
                        refractionHeight = refractionHeight.toPx(),
                        refractionAmount = refractionAmount.toPx(),
                        depthEffect = true,
                        chromaticAberration = chromaticAberration,
                    )
                }
            },
            onDrawSurface = {
                drawRect(glassBase)
                drawRect(frostSheen)
                drawRect(materialTint)
                val edge = 1.dp.toPx()
                when (cardStyle) {
                    FrostedGlassCardStyle.Mist -> {
                        drawRect(
                            stroke.copy(alpha = if (darkMode) 0.24f else 0.54f),
                            Offset(size.width * 0.08f, 0f),
                            Size(size.width * 0.56f, edge),
                        )
                    }
                    FrostedGlassCardStyle.Ice -> {
                        drawRect(
                            styleTint.copy(alpha = if (darkMode) 0.34f else 0.46f),
                            Offset(size.width * 0.08f, 0f),
                            Size(size.width * 0.30f, edge * 1.35f),
                        )
                        drawRect(
                            stroke.copy(alpha = if (darkMode) 0.18f else 0.42f),
                            Offset(size.width * 0.68f, size.height - edge),
                            Size(size.width * 0.22f, edge),
                        )
                    }
                    FrostedGlassCardStyle.Pearl -> {
                        drawRect(
                            stroke.copy(alpha = if (darkMode) 0.20f else 0.46f),
                            Offset(size.width * 0.22f, 0f),
                            Size(size.width * 0.56f, edge),
                        )
                        drawRect(
                            styleTint.copy(alpha = if (darkMode) 0.22f else 0.34f),
                            Offset(size.width * 0.35f, size.height - edge),
                            Size(size.width * 0.30f, edge),
                        )
                    }
                }
            },
        )
    } else {
        Modifier
            .background(glassBase, shape)
            .background(frostSheen, shape)
            .background(materialTint, shape)
    }

    return this
        .shadow(
            elevation = 3.dp,
            shape = shape,
            clip = false,
            ambientColor = shadow.copy(alpha = if (darkMode) 0.22f else 0.08f),
            spotColor = shadow.copy(alpha = if (darkMode) 0.30f else 0.12f),
        )
        .clip(shape)
        .then(material)
        .border(
            1.dp,
            subtleStroke.copy(alpha = (0.42f + strokeAlpha.coerceIn(0f, 1f) * 0.34f)),
            shape,
        )
}

@Composable
fun Modifier.globalLiquidGlassSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    surfaceColor: Color = Color.Unspecified,
    surfaceAlpha: Float = 0.54f,
    blurRadius: Dp = 16.dp,
    enableRefraction: Boolean = false,
    refractionHeight: Dp = 16.dp,
    refractionAmount: Dp = 10.dp,
    chromaticAberration: Float = 0.22f,
    strokeAlpha: Float = 0.70f,
    cardStyle: FrostedGlassCardStyle = FrostedGlassCardStyle.Mist,
): Modifier {
    if (!isLiquidGlassTheme()) return this
    val blurIntensity = LocalBlurIntensity.current
    val darkMode = isInDarkTheme()
    val resolvedSurfaceColor = if (surfaceColor == Color.Unspecified) {
        liquidGlassSurfaceColor()
    } else {
        surfaceColor
    }
    val scaledBlurRadius = blurRadius * blurIntensity
    val effectiveBlurRadius = if (scaledBlurRadius < 12.dp) 12.dp else scaledBlurRadius
    return liquidGlassSurface(
        backdrop = LocalLiquidGlassBackdrop.current,
        shape = shape,
        surfaceColor = resolvedSurfaceColor,
        surfaceAlpha = surfaceAlpha,
        blurRadius = effectiveBlurRadius,
        enableRefraction = enableRefraction,
        refractionHeight = refractionHeight,
        refractionAmount = refractionAmount,
        chromaticAberration = chromaticAberration,
        strokeAlpha = strokeAlpha,
        darkMode = darkMode,
        cardStyle = cardStyle,
    )
}

@Composable
fun liquidGlassMiuixCardColors(
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
) = MiuixCardDefaults.defaultColors(
    color = if (isLiquidGlassTheme()) Color.Transparent else color
)

@Composable
fun liquidGlassMaterialCardColors(
    containerColor: Color,
) = MaterialCardDefaults.cardColors(
    containerColor = if (isLiquidGlassTheme()) Color.Transparent else containerColor
)

@Composable
fun Modifier.globalLiquidGlassButton(): Modifier {
    return globalLiquidGlassSurface(
        shape = CircleShape,
        surfaceAlpha = 0.52f,
        blurRadius = 12.dp,
        strokeAlpha = 0.58f,
        cardStyle = FrostedGlassCardStyle.Pearl,
    )
}
