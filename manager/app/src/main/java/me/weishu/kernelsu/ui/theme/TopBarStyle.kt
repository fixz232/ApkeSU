package me.weishu.kernelsu.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalImmersiveBackgroundActive = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun immersiveTopBarColor(defaultColor: Color): Color {
    return if (LocalImmersiveBackgroundActive.current) Color.Transparent else defaultColor
}

@Composable
@ReadOnlyComposable
fun immersivePageColor(defaultColor: Color): Color {
    return if (LocalImmersiveBackgroundActive.current) Color.Transparent else defaultColor
}

@Composable
@ReadOnlyComposable
fun immersiveSurfaceColor(
    defaultColor: Color,
    darkAlpha: Float = 0.58f,
    lightAlpha: Float = 0.68f,
): Color {
    if (!LocalImmersiveBackgroundActive.current) return defaultColor
    return defaultColor.copy(alpha = if (isInDarkTheme()) darkAlpha else lightAlpha)
}

@Composable
@ReadOnlyComposable
fun immersiveScrolledTopBarColor(defaultColor: Color): Color {
    return immersiveSurfaceColor(
        defaultColor = defaultColor,
        darkAlpha = 0.52f,
        lightAlpha = 0.60f,
    )
}
