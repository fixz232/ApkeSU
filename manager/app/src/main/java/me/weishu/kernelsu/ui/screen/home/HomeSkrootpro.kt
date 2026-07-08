package me.weishu.kernelsu.ui.screen.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproButton
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproDivider
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproScreen
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproSectionTitle
import me.weishu.kernelsu.ui.component.skrootpro.skrootproSp

@Composable
fun HomePagerSkrootpro(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var outputCleared by remember(state.isKernelActive) { mutableStateOf(false) }
    val outputText = if (!state.isKernelActive) {
        stringResource(R.string.skrootpro_env_not_installed)
    } else {
        stringResource(R.string.skrootpro_env_active)
    }

    SkrootproScreen(
        title = stringResource(R.string.skrootpro_title),
        bottomInnerPadding = bottomInnerPadding,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
        ) {
            Text(
                text = stringResource(R.string.skrootpro_intro_title),
                color = SkrootproColors.Muted,
                fontSize = skrootproSp(14f, maxScale = 1.0f),
                lineHeight = skrootproSp(18f, maxScale = 1.0f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Text(
                text = stringResource(R.string.skrootpro_intro_body),
                color = SkrootproColors.Muted,
                fontSize = skrootproSp(10.5f, maxScale = 1.0f),
                lineHeight = skrootproSp(13.5f, maxScale = 1.0f),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            SkrootproDivider(modifier = Modifier.padding(horizontal = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(356.dp)
                    .padding(horizontal = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 10.dp),
                ) {
                    SkrootproSectionTitle(stringResource(R.string.skrootpro_base_env))
                    SkrootproButton(
                        text = stringResource(R.string.skrootpro_install_env),
                        onClick = actions.onInstallClick,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    SkrootproSectionTitle(stringResource(R.string.skrootpro_system_env))
                    SystemStatusLine(
                        label = "SELinux",
                        value = when (state.systemInfo.selinuxStatus) {
                            "Enforcing" -> stringResource(R.string.seccomp_status_strict)
                            "Permissive" -> "Permissive"
                            else -> state.systemInfo.selinuxStatus
                        },
                    )
                    SystemStatusLine(
                        label = "Seccomp",
                        value = when (state.systemInfo.seccompStatus) {
                            2 -> stringResource(R.string.seccomp_status_filter)
                            1 -> stringResource(R.string.seccomp_status_strict)
                            0 -> stringResource(R.string.seccomp_status_disabled)
                            else -> stringResource(R.string.seccomp_status_unknown)
                        },
                    )
                    SystemStatusLine(
                        label = "Adb",
                        value = stringResource(R.string.skrootpro_adb_disabled),
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    SkrootproDivider()
                    SkrootproSectionTitle(stringResource(R.string.skrootpro_basic_menu))
                    SkrootproButton(
                        text = stringResource(R.string.skrootpro_test_root),
                        onClick = actions.onSuperuserClick,
                        modifier = Modifier.fillMaxWidth(0.95f),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    SkrootproButton(
                        text = stringResource(R.string.skrootpro_execute_root),
                        onClick = actions.onModuleClick,
                        modifier = Modifier.fillMaxWidth(0.95f),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    SkrootproButton(
                        text = stringResource(R.string.skrootpro_parasite),
                        onClick = actions.onJailbreakClick,
                        modifier = Modifier.fillMaxWidth(0.95f),
                    )
                }

                SkrootproDivider(vertical = true)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 10.dp),
                ) {
                    SkrootproSectionTitle(stringResource(R.string.skrootpro_output_info))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SkrootproButton(
                            text = stringResource(R.string.skrootpro_copy),
                            onClick = {
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText("SKRoot(Pro)", outputText)
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        SkrootproButton(
                            text = stringResource(R.string.skrootpro_clear),
                            onClick = {
                                outputCleared = true
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = if (outputCleared) "" else outputText,
                        color = SkrootproColors.Text,
                        fontSize = skrootproSp(12f, maxScale = 1.0f),
                        lineHeight = skrootproSp(16f, maxScale = 1.0f),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF777777))
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemStatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckBox,
            contentDescription = null,
            tint = SkrootproColors.Success,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(15.dp),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "$label: $value",
            color = SkrootproColors.Success,
            fontSize = skrootproSp(10.5f, maxScale = 1.0f),
            lineHeight = skrootproSp(13f, maxScale = 1.0f),
            fontWeight = FontWeight.Bold,
        )
    }
}
