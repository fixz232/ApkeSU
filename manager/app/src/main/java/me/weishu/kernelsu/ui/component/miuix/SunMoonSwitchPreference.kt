package me.weishu.kernelsu.ui.component.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.SwitchStyle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun SunMoonSwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        startAction?.invoke()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = if (enabled) colorScheme.onSurface else colorScheme.disabledOnSecondaryVariant,
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    color = if (enabled) colorScheme.onSurfaceVariantSummary else colorScheme.disabledOnSecondaryVariant,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val switchStyle = LocalSwitchStyle.current
        if (switchStyle == SwitchStyle.Original) {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        } else {
            StyledSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                style = switchStyle,
            )
        }
    }
}
