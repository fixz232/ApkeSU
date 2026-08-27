package me.weishu.kernelsu.ui.screen.navigationicon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.CustomNavigationIconImage
import me.weishu.kernelsu.ui.screen.settings.MediaEditorSlider
import me.weishu.kernelsu.ui.screen.settings.MediaFileInfoSummary
import me.weishu.kernelsu.ui.screen.settings.rememberMediaFileInfo
import me.weishu.kernelsu.ui.util.CustomNavigationIconMask
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.CustomNavigationIconState
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

@Composable
internal fun NavigationIconAdjustmentDialog(
    show: Boolean,
    slot: CustomNavigationIconSlot,
    state: CustomNavigationIconState,
    onValueChange: (CustomNavigationIconState) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (!show) return
    val value = state.normalized()
    val info = rememberMediaFileInfo(value.uriString)
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb().toLong() and 0xFFFFFFFFL

    OverlayDialog(
        show = true,
        title = stringResource(R.string.settings_navigation_icon_adjust_title),
        onDismissRequest = onDismissRequest,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(86.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CustomNavigationIconImage(
                        state = value,
                        contentDescription = stringResource(slot.labelRes),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = slot.fallbackIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
                MediaFileInfoSummary(info)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value.labelOverride.orEmpty(),
                    onValueChange = { onValueChange(value.copy(labelOverride = it).normalized()) },
                    label = { Text(stringResource(R.string.settings_navigation_icon_name)) },
                    singleLine = true,
                )
                MediaEditorSlider(
                    title = stringResource(R.string.settings_navigation_icon_size),
                    value = value.sizeScale,
                    valueRange = 0.6f..1.4f,
                    valueLabel = { "${(it * 100).toInt()}%" },
                    onValueChange = { onValueChange(value.copy(sizeScale = it).normalized()) },
                )
                MediaEditorSlider(
                    title = stringResource(R.string.settings_navigation_icon_padding),
                    value = value.innerPaddingDp,
                    valueRange = 0f..8f,
                    valueLabel = { "${it.toInt()} dp" },
                    onValueChange = { onValueChange(value.copy(innerPaddingDp = it).normalized()) },
                )
                MediaEditorSlider(
                    title = stringResource(R.string.settings_navigation_icon_vertical),
                    value = value.verticalOffsetDp,
                    valueRange = -8f..8f,
                    valueLabel = { "${if (it > 0) "+" else ""}${it.toInt()} dp" },
                    onValueChange = { onValueChange(value.copy(verticalOffsetDp = it).normalized()) },
                )
                MediaEditorSlider(
                    title = stringResource(R.string.settings_navigation_icon_opacity),
                    value = value.opacity,
                    valueRange = 0.2f..1f,
                    valueLabel = { "${(it * 100).toInt()}%" },
                    onValueChange = { onValueChange(value.copy(opacity = it).normalized()) },
                )
                Text(
                    text = stringResource(R.string.settings_navigation_icon_mask),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomNavigationIconMask.entries.forEach { mask ->
                        FilterChip(
                            selected = value.mask == mask,
                            onClick = { onValueChange(value.copy(mask = mask)) },
                            label = { Text(stringResource(mask.labelRes)) },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_navigation_icon_tint),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        null to R.string.settings_navigation_icon_tint_original,
                        primaryArgb to R.string.settings_navigation_icon_tint_primary,
                        0xFFFFFFFFL to R.string.settings_navigation_icon_tint_white,
                        0xFF000000L to R.string.settings_navigation_icon_tint_black,
                    ).forEach { (argb, labelRes) ->
                        FilterChip(
                            selected = value.tintArgb == argb,
                            onClick = { onValueChange(value.copy(tintArgb = argb)) },
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }
                if (info?.hasAlpha == false) {
                    Text(
                        text = stringResource(R.string.settings_navigation_icon_no_alpha_warning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                value.tintArgb?.let { argb ->
                    val contrast = contrastRatio(Color(argb.toInt()), Color(0xFF121212))
                    Text(
                        text = stringResource(
                            if (contrast >= 3f) R.string.settings_navigation_icon_night_contrast_ok
                            else R.string.settings_navigation_icon_night_contrast_warning
                        ),
                        color = if (contrast >= 3f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
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
        CustomNavigationIconSlot.Home -> Icons.Rounded.Home
        CustomNavigationIconSlot.Kpm -> Icons.Rounded.Memory
        CustomNavigationIconSlot.Superuser -> Icons.Rounded.Security
        CustomNavigationIconSlot.Module -> Icons.Rounded.Extension
        CustomNavigationIconSlot.Settings -> Icons.Rounded.Settings
    }

private val CustomNavigationIconMask.labelRes: Int
    get() = when (this) {
        CustomNavigationIconMask.Original -> R.string.settings_navigation_icon_mask_original
        CustomNavigationIconMask.Circle -> R.string.settings_navigation_icon_mask_circle
        CustomNavigationIconMask.Square -> R.string.settings_navigation_icon_mask_square
        CustomNavigationIconMask.RoundedSquare -> R.string.settings_navigation_icon_mask_rounded
    }

private fun contrastRatio(foreground: Color, background: Color): Float {
    val lighter = maxOf(foreground.luminance(), background.luminance())
    val darker = minOf(foreground.luminance(), background.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
