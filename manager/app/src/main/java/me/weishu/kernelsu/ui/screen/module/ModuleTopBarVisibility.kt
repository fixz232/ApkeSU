package me.weishu.kernelsu.ui.screen.module

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private const val MODULE_TOP_BAR_IDLE_TIMEOUT_MILLIS = 3_000L

@Composable
internal fun rememberModuleTopBarVisible(isScrollInProgress: Boolean): Boolean {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(isScrollInProgress) {
        if (isScrollInProgress) {
            visible = false
        } else {
            delay(MODULE_TOP_BAR_IDLE_TIMEOUT_MILLIS)
            visible = true
        }
    }

    return visible
}
