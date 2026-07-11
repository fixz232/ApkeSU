package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R

@Composable
fun BuiltinMountDetailsDialog(
    show: Boolean,
    uiState: SettingsUiState,
    onDismissRequest: () -> Unit,
) {
    if (!show) return

    val uriHandler = LocalUriHandler.current
    val compatibility = when (uiState.builtinMountCompatibility) {
        "compatible" -> stringResource(R.string.settings_builtin_mount_compatibility_compatible)
        "unsupported" -> stringResource(R.string.settings_builtin_mount_compatibility_unsupported)
        "not_required" -> stringResource(R.string.settings_builtin_mount_compatibility_not_required)
        else -> stringResource(R.string.settings_builtin_mount_compatibility_unknown)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.settings_builtin_mount_details)) },
        text = {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null)
                            Text(
                                text = stringResource(
                                    R.string.settings_builtin_mount_not_root_driver,
                                    uiState.builtinMountLkmCount,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    BuiltinMountDetailLine(
                        label = stringResource(R.string.settings_builtin_mount_compatibility),
                        value = compatibility,
                        emphasized = true,
                    )
                    BuiltinMountDetailLine(
                        label = stringResource(R.string.settings_builtin_mount_current_kmi),
                        value = uiState.builtinMountCurrentKmi.ifBlank { "-" },
                    )
                    BuiltinMountDetailLine(
                        label = stringResource(R.string.settings_builtin_mount_supported_kmis),
                        value = uiState.builtinMountSupportedKmis.joinToString("\n").ifBlank { "-" },
                        monospace = true,
                    )
                    BuiltinMountDetailLine(
                        label = stringResource(R.string.settings_builtin_mount_sha256),
                        value = uiState.builtinMountArchiveSha256.ifBlank { "-" },
                        monospace = true,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = uiState.builtinMountSourceUrl.isNotBlank()) {
                                uriHandler.openUri(uiState.builtinMountSourceUrl)
                            },
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_builtin_mount_source),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.padding(1.dp),
                            )
                        }
                        Text(
                            text = uiState.builtinMountSourceUrl.ifBlank { "-" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun BuiltinMountDetailLine(
    label: String,
    value: String,
    emphasized: Boolean = false,
    monospace: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
        )
    }
}
