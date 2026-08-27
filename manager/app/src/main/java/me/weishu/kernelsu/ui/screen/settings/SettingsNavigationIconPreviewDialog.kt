package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.CustomNavigationIconImage
import me.weishu.kernelsu.ui.util.CustomNavigationIconState
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.CustomNavigationIconSet
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

@Composable
fun SettingsNavigationIconPreviewDialog(
    show: Boolean,
    slot: CustomNavigationIconSlot,
    icons: CustomNavigationIconSet,
    onDismissRequest: () -> Unit,
) {
    if (!show) return

    val title = stringResource(slot.previewTitleRes)
    var style by remember { mutableStateOf(NavigationPreviewStyle.Standard) }

    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismissRequest,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NavigationIconPreviewFrame(
                    selectedSlot = slot,
                    icons = icons,
                    style = style,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NavigationPreviewStyle.entries.forEach { option ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = style == option,
                            onClick = { style = option },
                            label = { Text(stringResource(option.labelRes)) },
                        )
                    }
                }
                MiuixTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(android.R.string.ok),
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}

private val CustomNavigationIconSlot.fallbackIcon: ImageVector
    get() = when (this) {
        CustomNavigationIconSlot.Home -> Icons.Filled.Home
        CustomNavigationIconSlot.Kpm -> Icons.Filled.Memory
        CustomNavigationIconSlot.Superuser -> Icons.Filled.Shield
        CustomNavigationIconSlot.Module -> Icons.Filled.Extension
        CustomNavigationIconSlot.Settings -> Icons.Filled.Settings
    }

@Composable
private fun NavigationIconPreviewFrame(
    selectedSlot: CustomNavigationIconSlot,
    icons: CustomNavigationIconSet,
    style: NavigationPreviewStyle,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(style.backgroundColor(), style.shape)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CustomNavigationIconSlot.entries.forEach { itemSlot ->
                val state = icons[itemSlot]
                val selected = itemSlot == selectedSlot
                val label = state.displayLabel(stringResource(itemSlot.labelRes))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else Color.Transparent,
                                if (style == NavigationPreviewStyle.Pixel) RoundedCornerShape(4.dp) else CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        CustomNavigationIconImage(
                            state = state,
                            contentDescription = label,
                            modifier = Modifier.size(24.dp),
                            alpha = if (selected) 1f else 0.74f,
                        ) {
                            Icon(
                                imageVector = itemSlot.fallbackIcon,
                                contentDescription = label,
                                modifier = Modifier.size(24.dp),
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = label,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
        Text(
            modifier = Modifier.padding(horizontal = 12.dp),
            text = stringResource(R.string.settings_navigation_icon_preview_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class NavigationPreviewStyle(val labelRes: Int) {
    Standard(R.string.settings_navigation_preview_standard),
    Frosted(R.string.settings_navigation_preview_frosted),
    Liquid(R.string.settings_navigation_preview_liquid),
    Pixel(R.string.settings_navigation_preview_pixel),
}

private val NavigationPreviewStyle.shape
    get() = if (this == NavigationPreviewStyle.Pixel) RoundedCornerShape(6.dp) else RoundedCornerShape(18.dp)

@Composable
private fun NavigationPreviewStyle.backgroundColor(): Color = when (this) {
    NavigationPreviewStyle.Standard -> MaterialTheme.colorScheme.surfaceVariant
    NavigationPreviewStyle.Frosted -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    NavigationPreviewStyle.Liquid -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
    NavigationPreviewStyle.Pixel -> Color(0xFF15131B)
}
