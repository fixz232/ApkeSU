package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.CustomNavigationIconImage
import me.weishu.kernelsu.ui.util.CustomNavigationIconState
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

@Composable
fun SettingsNavigationIconPreviewDialog(
    show: Boolean,
    slot: CustomNavigationIconSlot,
    state: CustomNavigationIconState,
    onDismissRequest: () -> Unit,
) {
    if (!show) return

    val title = stringResource(slot.previewTitleRes)
    val fallbackIcon = slot.fallbackIcon

    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismissRequest,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NavigationIconPreviewFrame(
                    title = title,
                    state = state,
                    fallbackIcon = fallbackIcon,
                )
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
        CustomNavigationIconSlot.Superuser -> Icons.Filled.Shield
        CustomNavigationIconSlot.Module -> Icons.Filled.Extension
        CustomNavigationIconSlot.Settings -> Icons.Filled.Settings
    }

@Composable
private fun NavigationIconPreviewFrame(
    title: String,
    state: CustomNavigationIconState,
    fallbackIcon: ImageVector,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                CustomNavigationIconImage(
                    state = state,
                    contentDescription = title,
                    modifier = Modifier.size(42.dp),
                ) {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = title,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.primary,
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
