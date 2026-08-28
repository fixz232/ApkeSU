package me.weishu.kernelsu.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.util.CpuSpoofCommandResult
import me.weishu.kernelsu.ui.util.CpuSpoofStatus
import me.weishu.kernelsu.ui.util.CPU_SPOOF_PROPERTY_VALUE_LIMIT
import me.weishu.kernelsu.ui.util.getCpuSpoofStatus
import me.weishu.kernelsu.ui.util.isCpuSpoofModelValid
import me.weishu.kernelsu.ui.util.mergeCpuSpoofStatus
import me.weishu.kernelsu.ui.util.restoreDefaultCpuSpoof
import me.weishu.kernelsu.ui.util.saveCpuSpoofTarget
import me.weishu.kernelsu.ui.util.setCpuSpoofEnabled

private data class CpuSpoofPreset(
    val title: String,
    val model: String,
)

private val cpuSpoofPresets = listOf(
    CpuSpoofPreset("Snapdragon 8 Elite", "SM8750-AB"),
    CpuSpoofPreset("Snapdragon 8 Gen 3", "SM8650-AB"),
    CpuSpoofPreset("Snapdragon 8 Gen 2", "SM8550-AB"),
    CpuSpoofPreset("Dimensity 9400", "MT6991"),
    CpuSpoofPreset("Dimensity 9300", "MT6989"),
    CpuSpoofPreset("Dimensity 9200", "MT6985"),
    CpuSpoofPreset("Tensor G4", "Tensor G4"),
    CpuSpoofPreset("Exynos 2500", "Exynos 2500"),
    CpuSpoofPreset("Kirin 9020", "Kirin 9020"),
)

@Composable
fun CpuSpoofScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onBack = dropUnlessResumed { navigator.pop() }
    var status by remember { mutableStateOf(CpuSpoofStatus()) }
    var draft by rememberSaveable { mutableStateOf("") }
    var draftDirty by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }

    fun applyStatus(next: CpuSpoofStatus, replaceDraft: Boolean) {
        status = next
        if (replaceDraft || (!draftDirty && draft.isBlank())) {
            draft = next.target.ifBlank { next.current }
            draftDirty = false
        }
    }

    fun refresh(replaceDraft: Boolean = false) {
        scope.launch {
            loading = true
            try {
                val refreshed = getCpuSpoofStatus()
                applyStatus(
                    next = mergeCpuSpoofStatus(status, refreshed),
                    replaceDraft = replaceDraft && refreshed.error.isBlank(),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                status = status.copy(error = error.message.orEmpty().ifBlank { "status_failed" })
            } finally {
                loading = false
            }
        }
    }

    fun runAction(
        successMessage: Int,
        failureMessage: Int,
        replaceDraft: Boolean = false,
        action: suspend () -> CpuSpoofCommandResult,
    ) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val result = action()
                val refreshedStatus = try {
                    getCpuSpoofStatus()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    status.copy(error = error.message.orEmpty().ifBlank { "status_failed" })
                }
                val refreshFailed = refreshedStatus.error.isNotBlank()
                applyStatus(
                    next = mergeCpuSpoofStatus(status, refreshedStatus),
                    replaceDraft = replaceDraft && result.success && !refreshFailed,
                )
                val message = when {
                    !result.success -> failureMessage
                    refreshFailed -> R.string.cpu_spoof_action_completed_refresh_failed
                    else -> successMessage
                }
                Toast.makeText(
                    context,
                    message,
                    Toast.LENGTH_LONG,
                ).show()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                Toast.makeText(context, failureMessage, Toast.LENGTH_LONG).show()
            } finally {
                busy = false
            }
        }
    }

    fun saveTarget() {
        if (!isCpuSpoofModelValid(draft)) {
            Toast.makeText(context, R.string.cpu_spoof_invalid_model, Toast.LENGTH_LONG).show()
            return
        }
        runAction(
            successMessage = R.string.cpu_spoof_target_saved,
            failureMessage = R.string.cpu_spoof_target_save_failed,
            replaceDraft = true,
        ) {
            saveCpuSpoofTarget(draft)
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled && !isCpuSpoofModelValid(draft)) {
            Toast.makeText(context, R.string.cpu_spoof_invalid_model, Toast.LENGTH_LONG).show()
            return
        }
        runAction(
            successMessage = if (enabled) R.string.cpu_spoof_enabled_message else R.string.cpu_spoof_disabled_message,
            failureMessage = R.string.cpu_spoof_toggle_failed,
            replaceDraft = true,
        ) {
            if (enabled) {
                val saved = saveCpuSpoofTarget(draft)
                if (!saved.success) saved else setCpuSpoofEnabled(true)
            } else {
                setCpuSpoofEnabled(false)
            }
        }
    }

    fun selectPreset(preset: CpuSpoofPreset) {
        if (busy) return
        draft = preset.model
        draftDirty = true
        runAction(
            successMessage = R.string.cpu_spoof_target_saved,
            failureMessage = R.string.cpu_spoof_target_save_failed,
            replaceDraft = true,
        ) {
            saveCpuSpoofTarget(preset.model)
        }
    }

    LaunchedEffect(Unit) {
        refresh(replaceDraft = true)
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_cpu_spoof)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }, enabled = !loading && !busy) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.cpu_spoof_refresh),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            } else {
                val controlsEnabled = status.supported && status.error.isBlank() && !busy
                CpuSpoofRuntimePanel(
                    status = status,
                    busy = busy,
                    onEnabledChange = ::setEnabled,
                )
                CpuSpoofCustomPanel(
                    value = draft,
                    enabled = controlsEnabled,
                    onValueChange = {
                        draft = it
                        draftDirty = true
                    },
                    onSave = ::saveTarget,
                )
                CpuSpoofPresetPanel(
                    selectedTarget = status.target,
                    enabled = controlsEnabled,
                    onSelected = ::selectPreset,
                )
                CpuSpoofRestorePanel(
                    enabled = status.configured && status.error.isBlank() && !busy,
                    onRestore = { showRestoreConfirmation = true },
                )
            }
        }
    }

    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false },
            title = { Text(stringResource(R.string.cpu_spoof_restore_title)) },
            text = { Text(stringResource(R.string.cpu_spoof_restore_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmation = false
                        runAction(
                            successMessage = R.string.cpu_spoof_restored,
                            failureMessage = R.string.cpu_spoof_restore_failed,
                            replaceDraft = true,
                        ) {
                            restoreDefaultCpuSpoof()
                        }
                    },
                ) {
                    Text(stringResource(R.string.cpu_spoof_restore_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmation = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CpuSpoofRuntimePanel(
    status: CpuSpoofStatus,
    busy: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val supported = status.supported
    val active = status.enabled && status.applied
    val stateText = when {
        status.error.isNotBlank() -> stringResource(R.string.cpu_spoof_status_error)
        !supported -> stringResource(R.string.cpu_spoof_unsupported)
        active -> stringResource(R.string.cpu_spoof_enabled)
        status.enabled -> stringResource(R.string.cpu_spoof_pending)
        else -> stringResource(R.string.cpu_spoof_disabled)
    }
    val stateColor = when {
        status.error.isNotBlank() || !supported || status.enabled && !status.applied -> MaterialTheme.colorScheme.error
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    CpuSpoofSection(
        title = stringResource(R.string.cpu_spoof_current_section),
        icon = Icons.Rounded.Memory,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stateText,
                    color = stateColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.cpu_spoof_toggle_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            StyledSwitch(
                checked = status.enabled,
                enabled = supported && status.error.isBlank() && !busy,
                onCheckedChange = onEnabledChange,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        CpuSpoofInfoRow(
            label = stringResource(R.string.cpu_spoof_current_cpu),
            value = status.current.ifBlank { stringResource(R.string.cpu_spoof_unavailable_value) },
        )
        if (status.target.isNotBlank()) {
            CpuSpoofInfoRow(
                label = stringResource(R.string.cpu_spoof_target_cpu),
                value = status.target,
            )
        }
        if (status.original.isNotBlank()) {
            CpuSpoofInfoRow(
                label = stringResource(R.string.cpu_spoof_original_cpu),
                value = status.original,
            )
        }
        if (status.manufacturer.isNotBlank()) {
            CpuSpoofInfoRow(
                label = stringResource(R.string.cpu_spoof_manufacturer),
                value = status.manufacturer,
            )
        }
        if (status.platform.isNotBlank()) {
            CpuSpoofInfoRow(
                label = stringResource(R.string.cpu_spoof_platform),
                value = status.platform,
                showDivider = false,
            )
        }
        if (status.error.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.cpu_spoof_status_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CpuSpoofCustomPanel(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val valid = isCpuSpoofModelValid(value)
    CpuSpoofSection(
        title = stringResource(R.string.cpu_spoof_custom_section),
        icon = Icons.Rounded.Memory,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            isError = value.isNotBlank() && !valid,
            label = { Text(stringResource(R.string.cpu_spoof_target_cpu)) },
            supportingText = {
                val count = value.trim().toByteArray(Charsets.UTF_8).size
                Text(stringResource(R.string.cpu_spoof_model_count, count, CPU_SPOOF_PROPERTY_VALUE_LIMIT))
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (valid) onSave() }),
        )
        if (value.isNotBlank() && !valid) {
            Text(
                text = stringResource(R.string.cpu_spoof_invalid_model),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled && valid,
        ) {
            Text(stringResource(R.string.cpu_spoof_save_target))
        }
    }
}

@Composable
private fun CpuSpoofPresetPanel(
    selectedTarget: String,
    enabled: Boolean,
    onSelected: (CpuSpoofPreset) -> Unit,
) {
    CpuSpoofSection(
        title = stringResource(R.string.cpu_spoof_common_section),
        icon = Icons.Rounded.CheckCircleOutline,
        contentPadding = 0.dp,
    ) {
        cpuSpoofPresets.forEachIndexed { index, preset ->
            CpuSpoofPresetRow(
                preset = preset,
                selected = preset.model == selectedTarget,
                enabled = enabled,
                onClick = { onSelected(preset) },
            )
            if (index != cpuSpoofPresets.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun CpuSpoofPresetRow(
    preset: CpuSpoofPreset,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = preset.title,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = preset.model,
                color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircleOutline,
                contentDescription = null,
                tint = selectedColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CpuSpoofRestorePanel(enabled: Boolean, onRestore: () -> Unit) {
    CpuSpoofSection(
        title = stringResource(R.string.cpu_spoof_restore_default),
        icon = Icons.Rounded.RestartAlt,
    ) {
        Text(
            text = stringResource(R.string.cpu_spoof_restore_summary),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.Rounded.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.cpu_spoof_restore_default))
        }
    }
}

@Composable
private fun CpuSpoofSection(
    title: String,
    icon: ImageVector,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = if (contentPadding == 0.dp) Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp) else Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            content()
        }
    }
}

@Composable
private fun CpuSpoofInfoRow(
    label: String,
    value: String,
    showDivider: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.38f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.62f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        }
    }
}
