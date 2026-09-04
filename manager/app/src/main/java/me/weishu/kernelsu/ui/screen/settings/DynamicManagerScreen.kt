package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.repository.DynamicManagerCandidate
import me.weishu.kernelsu.ui.component.ApkeEmptyState
import me.weishu.kernelsu.ui.component.ApkeErrorState
import me.weishu.kernelsu.ui.component.ApkeFixedActionBar
import me.weishu.kernelsu.ui.component.ApkeGroupedList
import me.weishu.kernelsu.ui.component.ApkeListDivider
import me.weishu.kernelsu.ui.component.ApkeLoadingState
import me.weishu.kernelsu.ui.component.ApkeSecondaryScaffold
import me.weishu.kernelsu.ui.component.ApkeStatus
import me.weishu.kernelsu.ui.component.ApkeStatusTone
import me.weishu.kernelsu.ui.component.ApkeUiTokens
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.viewmodel.DynamicManagerNotice
import me.weishu.kernelsu.ui.viewmodel.DynamicManagerUiState
import me.weishu.kernelsu.ui.viewmodel.DynamicManagerViewModel

@Composable
fun DynamicManagerScreen() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<DynamicManagerViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var pendingGrant by remember { mutableStateOf<DynamicManagerCandidate?>(null) }
    var confirmRevoke by remember { mutableStateOf(false) }
    var showManualConfig by remember { mutableStateOf(false) }
    val grantedMessage = stringResource(R.string.dynamic_manager_granted)
    val configuredMessage = stringResource(R.string.dynamic_manager_configured)
    val revokedMessage = stringResource(R.string.dynamic_manager_revoked)
    val failedPrefix = stringResource(R.string.dynamic_manager_failed)

    LaunchedEffect(state.notice) {
        val notice = state.notice ?: return@LaunchedEffect
        val message = when (notice) {
            DynamicManagerNotice.Granted -> grantedMessage
            DynamicManagerNotice.Configured -> configuredMessage
            DynamicManagerNotice.Revoked -> revokedMessage
            is DynamicManagerNotice.Failed -> "$failedPrefix: ${notice.detail}"
        }
        snackbarHostState.showSnackbar(message)
        viewModel.consumeNotice()
    }

    val status = when {
        state.loading -> ApkeStatus(stringResource(R.string.loading))
        state.loadError != null -> ApkeStatus(
            text = stringResource(R.string.dynamic_manager_status_unavailable),
            tone = ApkeStatusTone.Error,
        )
        !state.runtime.supported -> ApkeStatus(
            text = stringResource(R.string.dynamic_manager_unsupported),
            tone = ApkeStatusTone.Warning,
        )
        state.runtime.error.isNotBlank() -> ApkeStatus(
            text = stringResource(R.string.dynamic_manager_status_unavailable),
            tone = ApkeStatusTone.Error,
        )
        state.runtime.active -> ApkeStatus(
            text = stringResource(R.string.dynamic_manager_active),
            tone = ApkeStatusTone.Success,
        )
        state.runtime.configured -> ApkeStatus(
            text = stringResource(R.string.dynamic_manager_inactive),
            tone = ApkeStatusTone.Warning,
        )
        else -> ApkeStatus(stringResource(R.string.dynamic_manager_not_configured))
    }

    ApkeSecondaryScaffold(
        title = stringResource(R.string.dynamic_manager_title),
        onBack = navigator::pop,
        status = status,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(
                onClick = viewModel::refresh,
                enabled = !state.loading && !state.refreshing && !state.busy,
            ) {
                if (state.refreshing) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.dynamic_manager_refresh),
                    )
                }
            }
        },
        bottomBar = {
            if (state.runtime.configured) {
                ApkeFixedActionBar {
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { confirmRevoke = true },
                        enabled = !state.busy,
                        modifier = Modifier.heightIn(min = ApkeUiTokens.MinTouchTarget),
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.dynamic_manager_revoke))
                    }
                }
            }
        },
    ) { innerPadding, _ ->
        when {
            state.loading && state.candidates.isEmpty() -> ApkeLoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            state.loadError != null && state.candidates.isEmpty() -> ApkeErrorState(
                title = stringResource(R.string.dynamic_manager_status_unavailable),
                supportingText = state.loadError,
                onRetry = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            else -> DynamicManagerContent(
                state = state,
                onQueryChange = viewModel::updateQuery,
                onGrant = { pendingGrant = it },
                onManualConfig = { showManualConfig = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    pendingGrant?.let { candidate ->
        AlertDialog(
            onDismissRequest = { if (!state.busy) pendingGrant = null },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
            title = { Text(stringResource(R.string.dynamic_manager_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dynamic_manager_confirm_message,
                        candidate.label,
                        candidate.packageName,
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingGrant = null
                        viewModel.grant(candidate)
                    },
                    enabled = !state.busy,
                ) { Text(stringResource(R.string.dynamic_manager_grant)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingGrant = null }, enabled = !state.busy) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (confirmRevoke) {
        AlertDialog(
            onDismissRequest = { if (!state.busy) confirmRevoke = false },
            title = { Text(stringResource(R.string.dynamic_manager_revoke_title)) },
            text = { Text(stringResource(R.string.dynamic_manager_revoke_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmRevoke = false
                        viewModel.revoke()
                    },
                    enabled = !state.busy,
                ) { Text(stringResource(R.string.dynamic_manager_revoke)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRevoke = false }, enabled = !state.busy) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (showManualConfig) {
        DynamicManagerManualDialog(
            initialSize = state.runtime.certificateSize.takeIf { state.runtime.configured },
            initialHash = state.runtime.certificateSha256.takeIf { state.runtime.configured }.orEmpty(),
            busy = state.busy,
            onDismiss = { if (!state.busy) showManualConfig = false },
            onConfirm = { size, hash ->
                showManualConfig = false
                viewModel.setManual(size, hash)
            },
        )
    }
}

@Composable
private fun DynamicManagerContent(
    state: DynamicManagerUiState,
    onQueryChange: (String) -> Unit,
    onGrant: (DynamicManagerCandidate) -> Unit,
    onManualConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = ApkeUiTokens.PageHorizontalPadding,
            vertical = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "security-warning") {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(ApkeUiTokens.ToolCardRadius),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            stringResource(R.string.dynamic_manager_security_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.dynamic_manager_security_summary),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        if (state.runtime.supported && state.runtime.error.isNotBlank()) {
            item(key = "runtime-error") {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(ApkeUiTokens.ToolCardRadius),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null)
                        Text(
                            text = state.runtime.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item(key = "runtime") {
            ApkeGroupedList(title = stringResource(R.string.dynamic_manager_runtime)) {
                DynamicManagerStatusRow(state)
            }
        }

        item(key = "manual-config") {
            OutlinedButton(
                onClick = onManualConfig,
                enabled = state.runtime.supported && !state.busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ApkeUiTokens.MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.dynamic_manager_manual_config))
            }
        }

        item(key = "search") {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.dynamic_manager_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.dynamic_manager_clear_search),
                            )
                        }
                    }
                },
            )
        }

        if (state.visibleCandidates.isEmpty()) {
            item(key = "empty") {
                ApkeEmptyState(
                    title = stringResource(R.string.dynamic_manager_no_candidates),
                    supportingText = stringResource(R.string.dynamic_manager_no_candidates_summary),
                )
            }
        } else {
            items(
                items = state.visibleCandidates,
                key = { "${it.packageName}:${it.appId}" },
                contentType = { "dynamic-manager-candidate" },
            ) { candidate ->
                val selected = candidate.isSelected
                DynamicManagerCandidateRow(
                    candidate = candidate,
                    selected = selected,
                    active = selected && state.runtime.active,
                    enabled = state.runtime.supported && candidate.isChangeable,
                    busy = state.busy,
                    submitting = state.submittingPackage == candidate.packageName,
                    onClick = { if (!selected || !state.runtime.active) onGrant(candidate) },
                )
            }
        }
        item(key = "bottom-space") { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun DynamicManagerStatusRow(state: DynamicManagerUiState) {
    val runtime = state.runtime
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (runtime.active) Icons.Rounded.CheckCircle else Icons.Rounded.AdminPanelSettings,
                contentDescription = null,
                tint = if (runtime.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = when {
                    runtime.active -> stringResource(R.string.dynamic_manager_active)
                    runtime.configured -> stringResource(R.string.dynamic_manager_inactive)
                    else -> stringResource(R.string.dynamic_manager_not_configured)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (runtime.configured) {
            ApkeListDivider()
            Text(
                stringResource(
                    R.string.dynamic_manager_certificate,
                    runtime.certificateSize,
                    runtime.certificateSha256.take(16),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val activeAppIds = runtime.managerSignatureIndexes
                .filterValues { it == 255 }
                .keys
                .sorted()
            if (activeAppIds.isNotEmpty()) {
                Text(
                    stringResource(
                        R.string.dynamic_manager_active_app_ids,
                        activeAppIds.joinToString(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DynamicManagerManualDialog(
    initialSize: Int?,
    initialHash: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit,
) {
    var sizeText by remember(initialSize) { mutableStateOf(initialSize?.toString().orEmpty()) }
    var hashText by remember(initialHash) { mutableStateOf(initialHash) }
    val size = sizeText.toIntOrNull()
    val validSize = size != null && size in 0x100..0x1000
    val validHash = hashText.length == 64 && hashText.all { it in '0'..'9' || it in 'a'..'f' }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
        title = { Text(stringResource(R.string.dynamic_manager_manual_config)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.dynamic_manager_manual_config_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { value -> sizeText = value.filter(Char::isDigit).take(4) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    singleLine = true,
                    label = { Text(stringResource(R.string.dynamic_manager_signature_size)) },
                    supportingText = {
                        if (sizeText.isNotEmpty() && !validSize) {
                            Text(stringResource(R.string.dynamic_manager_signature_size_error))
                        }
                    },
                    isError = sizeText.isNotEmpty() && !validSize,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = hashText,
                    onValueChange = { value ->
                        hashText = value
                            .trim()
                            .lowercase()
                            .filter { it in '0'..'9' || it in 'a'..'f' }
                            .take(64)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    minLines = 2,
                    maxLines = 3,
                    label = { Text(stringResource(R.string.dynamic_manager_signature_sha256)) },
                    supportingText = {
                        Text(
                            if (hashText.isNotEmpty() && !validHash) {
                                stringResource(R.string.dynamic_manager_signature_sha256_error)
                            } else {
                                "${hashText.length}/64"
                            },
                        )
                    },
                    isError = hashText.isNotEmpty() && !validHash,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(size ?: return@Button, hashText) },
                enabled = !busy && validSize && validHash,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun DynamicManagerCandidateRow(
    candidate: DynamicManagerCandidate,
    selected: Boolean,
    active: Boolean,
    enabled: Boolean,
    busy: Boolean,
    submitting: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !busy && (!selected || !active), onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(ApkeUiTokens.ToolCardRadius),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconImage(
                packageInfo = candidate.packageInfo,
                label = candidate.label,
                modifier = Modifier.size(46.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    candidate.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    candidate.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.dynamic_manager_app_id, candidate.appId),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                submitting -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                selected && active -> Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.dynamic_manager_selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
                selected -> Button(onClick = onClick, enabled = enabled && !busy) {
                    Text(stringResource(R.string.dynamic_manager_rebind))
                }
                else -> OutlinedButton(onClick = onClick, enabled = enabled && !busy) {
                    Text(stringResource(R.string.dynamic_manager_grant))
                }
            }
        }
    }
}
