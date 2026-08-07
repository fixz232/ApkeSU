package me.weishu.kernelsu.ui.screen.module

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

const val MODULE_TOP_BAR_AUTO_HIDE_ENABLED_KEY = "module_top_bar_auto_hide_enabled"

private const val MODULE_TOP_BAR_IDLE_TIMEOUT_MILLIS = 5_000L

@Composable
internal fun rememberModuleTopBarVisible(
    enabled: Boolean,
    isScrollInProgress: Boolean,
): Boolean {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(enabled, isScrollInProgress) {
        if (!enabled) {
            visible = true
        } else if (isScrollInProgress) {
            visible = false
        } else {
            delay(MODULE_TOP_BAR_IDLE_TIMEOUT_MILLIS)
            visible = true
        }
    }

    return visible
}
