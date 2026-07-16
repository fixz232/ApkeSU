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
