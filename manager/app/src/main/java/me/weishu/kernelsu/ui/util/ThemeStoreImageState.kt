package me.weishu.kernelsu.ui.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberThemeStoreImageState(slot: ThemeStoreImageSlot): ThemeStoreImageState {
    val context = LocalContext.current.applicationContext
    val prefs = remember(context) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    var state by remember(context, slot) {
        mutableStateOf(readThemeStoreImageState(context, slot))
    }

    DisposableEffect(context, prefs, slot) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key !in slot.preferenceKeys) return@OnSharedPreferenceChangeListener
            state = readThemeStoreImageState(context, slot)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    return state
}
