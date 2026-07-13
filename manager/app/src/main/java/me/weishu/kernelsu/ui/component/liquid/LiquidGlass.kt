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

object LiquidGlassTokens {
    val Background = Color(0xFFF6FAFF)
    val Surface = Color.White
    val SurfaceTint = Color(0xFFEAF6FF)
    val Frost = Color(0xFFDDEFFF)
    val Stroke = Color.White
    val SubtleStroke = Color(0xFFD8E6F5)
    val PressedOverlay = Color(0xFFEEF6FF)
    val Shadow = Color(0xFF6E9EC5)
    val DarkBackground = Color(0xFF080D13)
    val DarkSurface = Color(0xFF17212C)
    val DarkSurfaceTint = Color(0xFF21364A)
    val DarkFrost = Color(0xFF29455D)
    val DarkStroke = Color(0xFFB9D9F2)
    val DarkSubtleStroke = Color(0xFF4B667C)
    val DarkShadow = Color(0xFF02060A)
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
        if (isInDarkTheme()) LiquidGlassTokens.DarkBackground else LiquidGlassTokens.Background
    } else {
        MiuixTheme.colorScheme.surface
    }
}

@Composable
@ReadOnlyComposable
fun liquidGlassSurfaceColor(): Color {
    return if (isInDarkTheme()) LiquidGlassTokens.DarkSurface else LiquidGlassTokens.Surface
}

fun Modifier.liquidGlassSurface(
    backdrop: Backdrop?,
    shape: Shape,
    surfaceColor: Color = LiquidGlassTokens.Surface,
    surfaceAlpha: Float = 0.62f,
    blurRadius: Dp = 10.dp,
    enableRefraction: Boolean = false,
    refractionHeight: Dp = 16.dp,
    refractionAmount: Dp = 10.dp,
    chromaticAberration: Float = 0.22f,
    strokeAlpha: Float = 0.70f,
    darkMode: Boolean = false,
): Modifier {
    val boundedAlpha = surfaceAlpha.coerceIn(0f, 1f)
    val glassBase = surfaceColor.copy(alpha = boundedAlpha)
    val sheen = if (darkMode) LiquidGlassTokens.DarkSurfaceTint else LiquidGlassTokens.SurfaceTint
    val frost = if (darkMode) LiquidGlassTokens.DarkFrost else LiquidGlassTokens.Frost
    val stroke = if (darkMode) LiquidGlassTokens.DarkStroke else LiquidGlassTokens.Stroke
    val subtleStroke = if (darkMode) LiquidGlassTokens.DarkSubtleStroke else LiquidGlassTokens.SubtleStroke
    val shadow = if (darkMode) LiquidGlassTokens.DarkShadow else LiquidGlassTokens.Shadow
    val glassSheen = Brush.verticalGradient(
        listOf(
            stroke.copy(alpha = if (darkMode) 0.16f else 0.46f),
            sheen.copy(alpha = if (darkMode) 0.34f else 0.20f),
            stroke.copy(alpha = if (darkMode) 0.06f else 0.12f),
            frost.copy(alpha = if (darkMode) 0.34f else 0.28f),
        )
    )
    val glassEdge = Brush.linearGradient(
        listOf(
            stroke.copy(alpha = if (darkMode) 0.28f else 0.52f),
            Color.Transparent,
            frost.copy(alpha = 0.26f),
        )
    )
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
                drawRect(glassSheen)
                drawRect(glassEdge)
            },
        )
    } else {
        Modifier
            .background(glassBase, shape)
            .background(glassSheen, shape)
            .background(glassEdge, shape)
    }

    return this
        .shadow(
            elevation = 8.dp,
            shape = shape,
            clip = false,
            ambientColor = shadow.copy(alpha = if (darkMode) 0.22f else 0.08f),
            spotColor = shadow.copy(alpha = if (darkMode) 0.30f else 0.12f),
        )
        .clip(shape)
        .then(material)
        .border(1.dp, subtleStroke.copy(alpha = 0.34f), shape)
        .border(1.dp, stroke.copy(alpha = strokeAlpha.coerceIn(0f, 1f)), shape)
}

@Composable
fun Modifier.globalLiquidGlassSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    surfaceColor: Color = Color.Unspecified,
    surfaceAlpha: Float = 0.62f,
    blurRadius: Dp = 10.dp,
    enableRefraction: Boolean = false,
    refractionHeight: Dp = 16.dp,
    refractionAmount: Dp = 10.dp,
    chromaticAberration: Float = 0.22f,
    strokeAlpha: Float = 0.70f,
): Modifier {
    if (!isLiquidGlassTheme()) return this
    val blurIntensity = LocalBlurIntensity.current
    val darkMode = isInDarkTheme()
    val resolvedSurfaceColor = if (surfaceColor == Color.Unspecified) {
        liquidGlassSurfaceColor()
    } else {
        surfaceColor
    }
    return liquidGlassSurface(
        backdrop = LocalLiquidGlassBackdrop.current,
        shape = shape,
        surfaceColor = resolvedSurfaceColor,
        surfaceAlpha = surfaceAlpha,
        blurRadius = blurRadius * blurIntensity,
        enableRefraction = enableRefraction,
        refractionHeight = refractionHeight,
        refractionAmount = refractionAmount,
        chromaticAberration = chromaticAberration,
        strokeAlpha = strokeAlpha,
        darkMode = darkMode,
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
        surfaceAlpha = 0.58f,
        blurRadius = 6.dp,
        strokeAlpha = 0.58f,
    )
}
