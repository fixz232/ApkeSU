package me.weishu.kernelsu.ui.screen.colorpalette

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun ThemePresetNameDialog(
    show: Boolean,
    title: String,
    initialName: String = "",
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    if (!show) return
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialThemePresetNameDialog(
            title = title,
            initialName = initialName,
            onDismissRequest = onDismissRequest,
            onConfirm = onConfirm,
        )

        UiMode.Miuix -> MiuixThemePresetNameDialog(
            title = title,
            initialName = initialName,
            onDismissRequest = onDismissRequest,
            onConfirm = onConfirm,
        )
    }
}

@Composable
private fun MaterialThemePresetNameDialog(
    title: String,
    initialName: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val trimmed = name.trim()
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                label = { Text(stringResource(R.string.theme_custom_preset_name)) },
            )
        },
        confirmButton = {
            TextButton(
                enabled = trimmed.isNotBlank(),
                onClick = {
                    onConfirm(trimmed)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun MiuixThemePresetNameDialog(
    title: String,
    initialName: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val trimmed = name.trim()
    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    maxLines = 1,
                    onValueChange = { name = it.take(40) },
                )
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(20.dp))
                    MiuixTextButton(
                        text = stringResource(R.string.confirm),
                        enabled = trimmed.isNotBlank(),
                        onClick = {
                            onConfirm(trimmed)
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        }
    )
}
