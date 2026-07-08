package me.weishu.kernelsu.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.ui.util.ensureManagerRegistered

@Composable
fun KsuIsValid(
    content: @Composable () -> Unit
) {
    val isValid by produceState(initialValue = false) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                Natives.isManager || ensureManagerRegistered()
            }.getOrDefault(false)
        }
    }

    if (isValid) {
        content()
    }
}
