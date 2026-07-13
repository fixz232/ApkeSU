package me.weishu.kernelsu.ui.component.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassSurface
import me.weishu.kernelsu.ui.component.liquid.isLiquidGlassTheme
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType


@Composable
fun WarningCard(
    message: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val darkMode = isInDarkTheme()
    Card(
        modifier = modifier.globalLiquidGlassSurface(
            surfaceColor = if (isLiquidGlassTheme()) {
                if (darkMode) Color(0xFF32191B) else Color(0xFFFFF2F2)
            } else {
                Color.Unspecified
            },
            surfaceAlpha = 0.66f,
            blurRadius = 10.dp,
            refractionHeight = 14.dp,
            refractionAmount = 9.dp,
        ),
        onClick = { onClick?.invoke() },
        colors = CardDefaults.defaultColors(
            color = if (isLiquidGlassTheme()) {
                Color.Transparent
            } else {
                color ?: when {
                    isDynamicColor -> colorScheme.errorContainer
                    darkMode -> Color(0XFF310808)
                    else -> Color(0xFFF8E2E2)
                }
            }
        ),
        showIndication = onClick != null,
        pressFeedbackType = PressFeedbackType.Tilt
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                color = if (isDynamicColor) colorScheme.onErrorContainer else Color(0xFFF72727),
                fontSize = 14.sp
            )
            action?.invoke()
        }
    }
}
