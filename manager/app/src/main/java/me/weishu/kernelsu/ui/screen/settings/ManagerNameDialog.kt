package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog

private const val MAX_CUSTOM_NAME_LENGTH = 40

@Composable
fun ManagerNameDialog(
    show: Boolean,
    initialName: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    if (!show) return
    MiuixNameDialog(
        initialName = initialName,
        titleRes = R.string.settings_manager_name,
        summaryRes = R.string.settings_manager_name_dialog_summary,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
    )
}

@Composable
fun HomeTitleDialog(
    show: Boolean,
    initialTitle: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    if (!show) return
    MiuixNameDialog(
        initialName = initialTitle,
        titleRes = R.string.settings_home_title,
        summaryRes = R.string.settings_home_title_dialog_summary,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
    )
}

@Composable
private fun MiuixNameDialog(
    initialName: String,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    OverlayDialog(
        show = true,
        title = stringResource(titleRes),
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(summaryRes))
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    maxLines = 1,
                    onValueChange = { name = it.take(MAX_CUSTOM_NAME_LENGTH) },
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
                        onClick = {
                            onConfirm(name.trim())
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
