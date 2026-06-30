package me.weishu.kernelsu.ui.webui

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun HandleWebUIEventMiuix(
    webUIState: WebUIState,
    fileLauncher: ActivityResultLauncher<Intent>
) {
    when (val event = webUIState.uiEvent) {
        is WebUIEvent.ShowAlert -> {
            val showDialog = remember(event) { mutableStateOf(true) }
            WindowDialog(
                show = showDialog.value,
                content = {
                    Column {
                        Text(event.message)
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                webUIState.onAlertResult()
                                showDialog.value = false
                            },
                            text = stringResource(R.string.confirm),
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            )
        }

        is WebUIEvent.ShowConfirm -> {
            val showDialog = remember(event) { mutableStateOf(true) }
            WindowDialog(
                show = showDialog.value,
                onDismissRequest = { webUIState.onConfirmResult(false) },
                content = {
                    Column {
                        Text(event.message)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    webUIState.onConfirmResult(false)
                                    showDialog.value = false
                                },
                                text = stringResource(android.R.string.cancel),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            TextButton(
                                onClick = {
                                    webUIState.onConfirmResult(true)
                                    showDialog.value = false
                                },
                                text = stringResource(R.string.confirm),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }
                }
            )
        }

        is WebUIEvent.ShowPrompt -> {
            val showDialog = remember(event) { mutableStateOf(true) }
            var text by remember(event) { mutableStateOf(event.defaultValue) }
            WindowDialog(
                show = showDialog.value,
                modifier = Modifier.imePadding(),
                onDismissRequest = {
                    webUIState.onPromptResult(null)
                    showDialog.value = false
                },
                content = {
                    Column {
                        Text(event.message)
                        Spacer(Modifier.height(12.dp))
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            value = text,
                            maxLines = 1,
                            onValueChange = { text = it },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    webUIState.onPromptResult(null)
                                    showDialog.value = false
                                },
                                text = stringResource(android.R.string.cancel),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            TextButton(
                                onClick = {
                                    webUIState.onPromptResult(text)
                                    showDialog.value = false
                                },
                                text = stringResource(R.string.confirm),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }
                }
            )
        }

        is WebUIEvent.ShowFileChooser -> {
            LaunchedEffect(event) {
                try {
                    fileLauncher.launch(event.intent)
                } catch (_: Exception) {
                    webUIState.onFileChooserResult(null)
                }
            }
        }

        else -> {}
    }
}
