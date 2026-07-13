package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.theme.ColorMode

@Composable
internal fun isDayNightSwitchChecked(themeMode: Int): Boolean {
    val colorMode = ColorMode.fromValue(themeMode)
    return colorMode.isDark || colorMode.isSystem && isSystemInDarkTheme()
}

@Composable
internal fun DayNightSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val switchStyle = LocalSwitchStyle.current
    if (switchStyle == SwitchStyle.Original) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
    } else {
        StyledSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            style = switchStyle,
        )
    }
}
