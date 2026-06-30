package me.weishu.kernelsu.ui.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import me.weishu.kernelsu.ui.component.GlobalScrollEffect

val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> {
    error("CompositionLocal LocalSnackbarHost not present")
}

val LocalShowSwitchIcon = compositionLocalOf { false }

val LocalScrollAnimation = compositionLocalOf { false }

val LocalScrollAnimationEffect = compositionLocalOf { GlobalScrollEffect.Default }
