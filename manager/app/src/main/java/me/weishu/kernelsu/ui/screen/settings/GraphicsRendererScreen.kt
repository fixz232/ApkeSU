package me.weishu.kernelsu.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.util.GraphicsRendererCommandResult
import me.weishu.kernelsu.ui.util.GraphicsRendererMode
import me.weishu.kernelsu.ui.util.GraphicsRendererStatus
import me.weishu.kernelsu.ui.util.getGraphicsRendererStatus
import me.weishu.kernelsu.ui.util.reboot
import me.weishu.kernelsu.ui.util.setGraphicsRendererMode

@Composable
fun GraphicsRendererScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val onBack = dropUnlessResumed { navigator.pop() }
    var status by remember { mutableStateOf(GraphicsRendererStatus()) }
    var selectedMode by rememberSaveable { mutableStateOf(GraphicsRendererMode.SystemDefault) }
    var persistent by rememberSaveable { mutableStateOf(false) }
    var draftDirty by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var showApplyConfirmation by rememberSaveable { mutableStateOf(false) }
    var showRebootConfirmation by rememberSaveable { mutableStateOf(false) }

    fun acceptStatus(next: GraphicsRendererStatus, replaceDraft: Boolean) {
        status = next
        if (replaceDraft || !draftDirty) {
            selectedMode = next.configuredMode ?: GraphicsRendererMode.SystemDefault
            persistent = next.persistent
            draftDirty = false
        }
    }

    fun refresh(replaceDraft: Boolean = false) {
        scope.launch {
            loading = true
            try {
                acceptStatus(getGraphicsRendererStatus(), replaceDraft)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                status = status.copy(error = error.message.orEmpty().ifBlank { "status_failed" })
            } finally {
                loading = false
            }
        }
    }

    fun applySelection() {
        scope.launch {
            busy = true
            try {
                val result = setGraphicsRendererMode(selectedMode, persistent)
                acceptStatus(result.status, replaceDraft = result.success)
                Toast.makeText(
                    context,
                    graphicsRendererResultMessage(resources, result),
                    Toast.LENGTH_LONG,
                ).show()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Toast.makeText(
                    context,
                    resources.getString(
                        R.string.graphics_renderer_apply_failed_detail,
                        sanitizeRendererError(error.message),
                    ),
                    Toast.LENGTH_LONG,
                ).show()
                refresh()
            } finally {
                busy = false
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        refresh(replaceDraft = !draftDirty)
        onPauseOrDispose { }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_graphics_renderer)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refresh(replaceDraft = !draftDirty) }, enabled = !busy) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.graphics_renderer_refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GraphicsRendererStatusCard(status = status, loading = loading)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.graphics_renderer_mode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    selectableGraphicsRendererModes.forEachIndexed { index, mode ->
                        val enabled = !busy && status.rootAvailable &&
                            (mode != GraphicsRendererMode.Vulkan || status.vulkanSupported)
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = selectableGraphicsRendererModes.size,
                            ),
                            selected = selectedMode == mode,
                            enabled = enabled,
                            onClick = {
                                selectedMode = mode
                                if (mode == GraphicsRendererMode.SystemDefault) persistent = false
                                draftDirty = true
                            },
                            label = { Text(graphicsRendererModeName(mode), maxLines = 1) },
                        )
                    }
                }
                Text(
                    graphicsRendererModeSummary(selectedMode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            GraphicsRendererPersistenceRow(
                checked = persistent,
                enabled = selectedMode != GraphicsRendererMode.SystemDefault && status.rootAvailable && !busy,
                onCheckedChange = {
                    persistent = it
                    draftDirty = true
                },
            )

            if (selectedMode == GraphicsRendererMode.Vulkan && !status.vulkanSupported) {
                GraphicsRendererNotice(
                    icon = Icons.Rounded.WarningAmber,
                    title = stringResource(R.string.graphics_renderer_vulkan_unavailable),
                    message = stringResource(R.string.graphics_renderer_vulkan_unavailable_summary),
                    error = true,
                )
            }

            if (status.restartRequired) {
                GraphicsRendererNotice(
                    icon = Icons.Rounded.RestartAlt,
                    title = stringResource(R.string.graphics_renderer_restart_required),
                    message = stringResource(R.string.graphics_renderer_restart_required_summary),
                    action = {
                        TextButton(onClick = { showRebootConfirmation = true }, enabled = !busy) {
                            Text(stringResource(R.string.reboot))
                        }
                    },
                )
            }

            Button(
                onClick = { showApplyConfirmation = true },
                enabled = !loading && !busy && status.rootAvailable &&
                    (selectedMode != GraphicsRendererMode.Vulkan || status.vulkanSupported),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.DeveloperMode, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (selectedMode == GraphicsRendererMode.SystemDefault) {
                        stringResource(R.string.graphics_renderer_restore_default)
                    } else {
                        stringResource(R.string.graphics_renderer_apply)
                    }
                )
            }

            HorizontalDivider()
            GraphicsRendererDiagnostics(status)
        }
    }

    if (showApplyConfirmation) {
        AlertDialog(
            onDismissRequest = { showApplyConfirmation = false },
            icon = { Icon(Icons.Rounded.Shield, contentDescription = null) },
            title = {
                Text(
                    if (selectedMode == GraphicsRendererMode.SystemDefault) {
                        stringResource(R.string.graphics_renderer_restore_confirm_title)
                    } else {
                        stringResource(R.string.graphics_renderer_apply_confirm_title)
                    }
                )
            },
            text = {
                Text(
                    if (selectedMode == GraphicsRendererMode.SystemDefault) {
                        stringResource(R.string.graphics_renderer_restore_confirm_message)
                    } else {
                        stringResource(
                            R.string.graphics_renderer_apply_confirm_message,
                            graphicsRendererModeName(selectedMode),
                            if (persistent) {
                                stringResource(R.string.graphics_renderer_persistent_enabled)
                            } else {
                                stringResource(R.string.graphics_renderer_persistent_disabled)
                            },
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showApplyConfirmation = false
                        applySelection()
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showApplyConfirmation = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (showRebootConfirmation) {
        AlertDialog(
            onDismissRequest = { showRebootConfirmation = false },
            title = { Text(stringResource(R.string.graphics_renderer_reboot_confirm_title)) },
            text = { Text(stringResource(R.string.graphics_renderer_reboot_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRebootConfirmation = false
                        reboot()
                    }
                ) { Text(stringResource(R.string.reboot)) }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirmation = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun GraphicsRendererStatusCard(status: GraphicsRendererStatus, loading: Boolean) {
    val error = !loading && (status.error.isNotBlank() || !status.rootAvailable)
    val warning = status.configured && !status.applied
    val color = when {
        error -> MaterialTheme.colorScheme.errorContainer
        warning -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        error -> MaterialTheme.colorScheme.onErrorContainer
        warning -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        color = color,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp, color = contentColor)
            } else {
                Icon(
                    when {
                        error -> Icons.Rounded.ErrorOutline
                        warning -> Icons.Rounded.WarningAmber
                        else -> Icons.Rounded.CheckCircle
                    },
                    contentDescription = null,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    when {
                        loading -> stringResource(R.string.graphics_renderer_detecting)
                        !status.rootAvailable -> stringResource(R.string.graphics_renderer_root_unavailable)
                        status.error.isNotBlank() -> stringResource(R.string.graphics_renderer_status_failed)
                        warning -> stringResource(R.string.graphics_renderer_not_applied)
                        status.configured -> stringResource(R.string.graphics_renderer_applied)
                        else -> stringResource(R.string.graphics_renderer_system_managed)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(
                        R.string.graphics_renderer_current_mode,
                        graphicsRendererModeName(status.currentMode),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun GraphicsRendererPersistenceRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.graphics_renderer_persist), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.graphics_renderer_persist_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StyledSwitch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun GraphicsRendererNotice(
    icon: ImageVector,
    title: String,
    message: String,
    error: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
            action?.invoke()
        }
    }
}

@Composable
private fun GraphicsRendererDiagnostics(status: GraphicsRendererStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.graphics_renderer_diagnostics),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        GraphicsRendererInfoRow(
            stringResource(R.string.graphics_renderer_configured_mode),
            status.configuredMode?.let { graphicsRendererModeName(it) }
                ?: stringResource(R.string.graphics_renderer_not_configured),
        )
        GraphicsRendererInfoRow(
            "debug.hwui.renderer",
            status.rendererProperty.ifBlank { stringResource(R.string.graphics_renderer_property_unset) },
            monospace = true,
        )
        GraphicsRendererInfoRow(
            "debug.hwui.disable_vulkan",
            status.disableVulkanProperty.ifBlank { stringResource(R.string.graphics_renderer_property_unset) },
            monospace = true,
        )
        GraphicsRendererInfoRow(
            stringResource(R.string.graphics_renderer_vulkan_capability),
            if (status.vulkanSupported) {
                stringResource(R.string.graphics_renderer_supported)
            } else {
                stringResource(R.string.graphics_renderer_unsupported)
            },
        )
        GraphicsRendererInfoRow(
            stringResource(R.string.graphics_renderer_vulkan_feature),
            status.vulkanFeature.ifBlank { stringResource(R.string.graphics_renderer_not_detected) },
            monospace = true,
        )
        GraphicsRendererInfoRow(
            stringResource(R.string.graphics_renderer_vulkan_driver),
            status.vulkanDriverPath.ifBlank { stringResource(R.string.graphics_renderer_not_detected) },
            monospace = true,
        )
        GraphicsRendererInfoRow(
            stringResource(R.string.graphics_renderer_egl_driver),
            status.eglDriver.ifBlank { stringResource(R.string.graphics_renderer_not_detected) },
            monospace = true,
        )
        GraphicsRendererInfoRow(
            stringResource(R.string.graphics_renderer_persistence_status),
            if (status.persistent) {
                stringResource(R.string.graphics_renderer_persistent_enabled)
            } else {
                stringResource(R.string.graphics_renderer_persistent_disabled)
            },
            showDivider = false,
        )
    }
}

@Composable
private fun GraphicsRendererInfoRow(
    label: String,
    value: String,
    monospace: Boolean = false,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.42f),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                modifier = Modifier.weight(0.58f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun graphicsRendererModeName(mode: GraphicsRendererMode): String = when (mode) {
    GraphicsRendererMode.SystemDefault -> stringResource(R.string.graphics_renderer_mode_system)
    GraphicsRendererMode.Vulkan -> stringResource(R.string.graphics_renderer_mode_vulkan)
    GraphicsRendererMode.OpenGl -> stringResource(R.string.graphics_renderer_mode_opengl)
    GraphicsRendererMode.Custom -> stringResource(R.string.graphics_renderer_mode_custom)
}

@Composable
private fun graphicsRendererModeSummary(mode: GraphicsRendererMode): String = when (mode) {
    GraphicsRendererMode.SystemDefault -> stringResource(R.string.graphics_renderer_mode_system_summary)
    GraphicsRendererMode.Vulkan -> stringResource(R.string.graphics_renderer_mode_vulkan_summary)
    GraphicsRendererMode.OpenGl -> stringResource(R.string.graphics_renderer_mode_opengl_summary)
    GraphicsRendererMode.Custom -> stringResource(R.string.graphics_renderer_mode_custom_summary)
}

private fun graphicsRendererResultMessage(
    resources: android.content.res.Resources,
    result: GraphicsRendererCommandResult,
): String {
    if (result.success) {
        return resources.getString(R.string.graphics_renderer_apply_success)
    }
    val knownMessage = when (result.error) {
        "root_unavailable" -> R.string.graphics_renderer_root_unavailable
        "vulkan_unsupported" -> R.string.graphics_renderer_vulkan_unavailable
        "backup_missing" -> R.string.graphics_renderer_backup_missing
        "timeout" -> R.string.graphics_renderer_timeout
        "runtime_verification_failed",
        "final_verification_failed",
        "restore_verification_failed",
        "cleanup_verification_failed",
        -> R.string.graphics_renderer_verification_failed
        else -> null
    }
    return knownMessage?.let(resources::getString)
        ?: resources.getString(
            R.string.graphics_renderer_apply_failed_detail,
            sanitizeRendererError(result.error),
        )
}

private fun sanitizeRendererError(error: String?): String = error.orEmpty()
    .replace('\n', ' ')
    .replace('\r', ' ')
    .take(240)
    .ifBlank { "operation_failed" }

private val selectableGraphicsRendererModes = listOf(
    GraphicsRendererMode.SystemDefault,
    GraphicsRendererMode.Vulkan,
    GraphicsRendererMode.OpenGl,
)
