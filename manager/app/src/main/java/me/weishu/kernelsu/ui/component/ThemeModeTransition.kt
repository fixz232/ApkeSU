package me.weishu.kernelsu.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ThemeModeTransitionOverlay(
    darkMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha = remember { Animatable(0f) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(darkMode) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        alpha.snapTo(0.16f)
        alpha.animateTo(0f, tween(durationMillis = 200))
    }
    if (alpha.value > 0.001f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background((if (darkMode) Color.Black else Color.White).copy(alpha = alpha.value)),
        )
    }
}
