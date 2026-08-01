package me.weishu.kernelsu.ui.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Wifi
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.util.DeviceIdentifierKind
import me.weishu.kernelsu.ui.util.DeviceIdentifierState
import me.weishu.kernelsu.ui.util.DeviceIdentifierSupport
import me.weishu.kernelsu.ui.util.DeviceIdentityActionResult
import me.weishu.kernelsu.ui.util.DeviceIdentityFailure
import me.weishu.kernelsu.ui.util.DeviceIdentityValidationError
import me.weishu.kernelsu.ui.util.generateDeviceIdentifier
import me.weishu.kernelsu.ui.util.validateDeviceIdentifier
import me.weishu.kernelsu.ui.viewmodel.DeviceIdentityUiState
import me.weishu.kernelsu.ui.viewmodel.DeviceIdentityViewModel

@Composable
fun DeviceIdentityScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val viewModel = viewModel<DeviceIdentityViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editingKind by remember { mutableStateOf<DeviceIdentifierKind?>(null) }
    var draft by remember { mutableStateOf("") }
    var pendingApply by remember { mutableStateOf<Pair<DeviceIdentifierKind, String>?>(null) }
    var pendingRestore by remember { mutableStateOf<DeviceIdentifierKind?>(null) }
    var confirmRestoreAll by remember { mutableStateOf(false) }

    fun copyValue(kind: DeviceIdentifierKind, value: String) {
        if (value.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        clipboard.setPrimaryClip(
            ClipData.newPlainText(resources.getString(kind.titleRes()), value),
        )
        Toast.makeText(context, R.string.device_identity_copied, Toast.LENGTH_SHORT).show()
    }

    fun showResult(result: DeviceIdentityActionResult, successMessage: Int) {
        val message = if (result.success) {
            resources.getString(successMessage)
        } else {
            resources.getString(result.failure.messageRes())
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun apply(kind: DeviceIdentifierKind, value: String) {
        scope.launch {
            showResult(
                result = viewModel.applyIdentifier(kind, value),
                successMessage = R.string.device_identity_apply_success,
            )
        }
    }

    fun restore(kind: DeviceIdentifierKind) {
        scope.launch {
            showResult(
                result = viewModel.restoreIdentifier(kind),
                successMessage = R.string.device_identity_restore_success,
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_device_identity)) },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !uiState.loading && !uiState.isBusy,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.device_identity_refresh),
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
        when {
            uiState.loading && !uiState.snapshot.rootAvailable -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            !uiState.snapshot.rootAvailable -> {
                DeviceIdentityErrorState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    failure = uiState.failure,
                    onRetry = viewModel::refresh,
                )
            }

            else -> {
                DeviceIdentityContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    uiState = uiState,
                    onCopy = ::copyValue,
                    onEdit = { identifier ->
                        draft = identifier.configuredValue.ifBlank { identifier.currentValue }
                        editingKind = identifier.kind
                    },
                    onRestore = { pendingRestore = it },
                    onRestoreAll = { confirmRestoreAll = true },
                )
            }
        }
    }

    val editingIdentifier = editingKind?.let(uiState.snapshot::identifier)
    if (editingIdentifier != null) {
        DeviceIdentifierEditorDialog(
            identifier = editingIdentifier,
            value = draft,
            busy = uiState.isBusy,
            onValueChange = { draft = it },
            onGenerate = { draft = generateDeviceIdentifier(editingIdentifier.kind) },
            onDismiss = { editingKind = null },
            onConfirm = {
                val validation = validateDeviceIdentifier(editingIdentifier.kind, draft)
                if (validation.isValid) {
                    pendingApply = editingIdentifier.kind to validation.normalizedValue
                    editingKind = null
                }
            },
        )
    }

    pendingApply?.let { (kind, value) ->
        DeviceIdentityConfirmationDialog(
            title = stringResource(R.string.device_identity_apply_confirm_title),
            message = stringResource(kind.applyConfirmationRes()),
            confirmLabel = stringResource(R.string.device_identity_apply_action),
            onDismiss = { pendingApply = null },
            onConfirm = {
                pendingApply = null
                apply(kind, value)
            },
        )
    }

    pendingRestore?.let { kind ->
        DeviceIdentityConfirmationDialog(
            title = stringResource(R.string.device_identity_restore_confirm_title),
            message = stringResource(
                R.string.device_identity_restore_confirm_message,
                stringResource(kind.titleRes()),
            ),
            confirmLabel = stringResource(R.string.device_identity_restore_action),
            onDismiss = { pendingRestore = null },
            onConfirm = {
                pendingRestore = null
                restore(kind)
            },
        )
    }

    if (confirmRestoreAll) {
        DeviceIdentityConfirmationDialog(
            title = stringResource(R.string.device_identity_restore_all_title),
            message = stringResource(R.string.device_identity_restore_all_message),
            confirmLabel = stringResource(R.string.device_identity_restore_all_action),
            onDismiss = { confirmRestoreAll = false },
            onConfirm = {
                confirmRestoreAll = false
                scope.launch {
                    val result = viewModel.restoreAll()
                    if (result.failedKinds.isNotEmpty()) {
                        val names = result.failedKinds.joinToString(", ") { kind ->
                            resources.getString(kind.titleRes())
                        }
                        Toast.makeText(
                            context,
                            resources.getString(R.string.device_identity_restore_all_partial, names),
                            Toast.LENGTH_LONG,
                        ).show()
                    } else {
                        showResult(result, R.string.device_identity_restore_all_success)
                    }
                }
            },
        )
    }
}

@Composable
private fun DeviceIdentityContent(
    modifier: Modifier,
    uiState: DeviceIdentityUiState,
    onCopy: (DeviceIdentifierKind, String) -> Unit,
    onEdit: (DeviceIdentifierState) -> Unit,
    onRestore: (DeviceIdentifierKind) -> Unit,
    onRestoreAll: () -> Unit,
) {
    val backedUpCount = uiState.snapshot.identifiers.count(DeviceIdentifierState::hasBackup)
    LazyColumn(
        modifier = modifier.imePadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DeviceIdentitySafetyBanner(userId = uiState.snapshot.userId)
        }
        item {
            Text(
                text = stringResource(R.string.device_identity_identifiers_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column {
                    uiState.snapshot.identifiers.forEachIndexed { index, identifier ->
                        DeviceIdentifierRow(
                            identifier = identifier,
                            busy = uiState.busyKind == identifier.kind || uiState.restoringAll,
                            actionsEnabled = !uiState.isBusy,
                            onCopy = { onCopy(identifier.kind, identifier.currentValue) },
                            onEdit = { onEdit(identifier) },
                            onRestore = { onRestore(identifier.kind) },
                        )
                        if (index != uiState.snapshot.identifiers.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRestoreAll,
                    enabled = backedUpCount > 0 && !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.restoringAll) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Rounded.Restore, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            R.string.device_identity_restore_all_with_count,
                            backedUpCount,
                        ),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.device_identity_scope_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceIdentitySafetyBanner(userId: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.device_identity_safety_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.device_identity_safety_summary, userId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun DeviceIdentifierRow(
    identifier: DeviceIdentifierState,
    busy: Boolean,
    actionsEnabled: Boolean,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRestore: () -> Unit,
) {
    val supported = identifier.support == DeviceIdentifierSupport.Supported
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = identifier.kind.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(identifier.kind.titleRes()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                DeviceIdentifierStatus(identifier)
            }
            Text(
                text = stringResource(identifier.kind.summaryRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (identifier.source.isNotBlank()) {
                Text(
                    text = identifier.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (supported && identifier.currentValue.isNotBlank()) {
                Text(
                    text = stringResource(R.string.device_identity_current_value),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SelectionContainer {
                    Text(
                        text = identifier.currentValue,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Text(
                    text = stringResource(identifier.support.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (supported) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
            if (identifier.hasConfiguredValue) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.device_identity_configured_value),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = identifier.configuredValue,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (identifier.hasBackup) {
                    TextButton(
                        onClick = onRestore,
                        enabled = actionsEnabled && !busy,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.device_identity_restore_original))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.device_identity_not_modified),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row {
                    IconButton(
                        onClick = onCopy,
                        enabled = actionsEnabled && identifier.currentValue.isNotBlank(),
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.device_identity_copy),
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        enabled = actionsEnabled && supported && !busy,
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.device_identity_edit),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceIdentifierStatus(identifier: DeviceIdentifierState) {
    val (textRes, color) = when {
        identifier.support != DeviceIdentifierSupport.Supported ->
            R.string.device_identity_status_unavailable to MaterialTheme.colorScheme.errorContainer
        identifier.applied && identifier.persistent ->
            R.string.device_identity_status_persistent to MaterialTheme.colorScheme.tertiaryContainer
        identifier.applied ->
            R.string.device_identity_status_verified to MaterialTheme.colorScheme.primaryContainer
        identifier.hasConfiguredValue ->
            R.string.device_identity_status_not_applied to MaterialTheme.colorScheme.errorContainer
        else -> R.string.device_identity_status_original to MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DeviceIdentifierEditorDialog(
    identifier: DeviceIdentifierState,
    value: String,
    busy: Boolean,
    onValueChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val validation = validateDeviceIdentifier(identifier.kind, value)
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(identifier.kind.icon(), contentDescription = null) },
        title = {
            Text(
                stringResource(
                    R.string.device_identity_edit_title,
                    stringResource(identifier.kind.titleRes()),
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(identifier.kind.editorHelpRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    singleLine = true,
                    label = { Text(stringResource(R.string.device_identity_target_value)) },
                    isError = value.isNotEmpty() && !validation.isValid,
                    supportingText = {
                        val error = validation.error
                        if (value.isNotEmpty() && error != null) {
                            Text(stringResource(error.messageRes()))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
                OutlinedButton(
                    onClick = onGenerate,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.device_identity_generate_random))
                }
                if (identifier.hasBackup) {
                    Text(
                        text = stringResource(
                            R.string.device_identity_original_value,
                            identifier.originalValue.ifBlank {
                                stringResource(R.string.device_identity_value_absent)
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.device_identity_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = validation.isValid && !busy,
            ) {
                Text(stringResource(R.string.device_identity_continue))
            }
        },
    )
}

@Composable
private fun DeviceIdentityConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Security, contentDescription = null) },
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.device_identity_cancel))
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
    )
}

@Composable
private fun DeviceIdentityErrorState(
    modifier: Modifier,
    failure: DeviceIdentityFailure?,
    onRetry: () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(R.string.device_identity_load_failed),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(failure.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.device_identity_retry))
            }
        }
    }
}

private val DeviceIdentityUiState.isBusy: Boolean
    get() = busyKind != null || restoringAll

private fun DeviceIdentifierKind.icon(): ImageVector = when (this) {
    DeviceIdentifierKind.SerialNumber -> Icons.Rounded.Tag
    DeviceIdentifierKind.AndroidId -> Icons.Rounded.Android
    DeviceIdentifierKind.WifiMac -> Icons.Rounded.Wifi
    DeviceIdentifierKind.BluetoothAddress -> Icons.Rounded.Bluetooth
    DeviceIdentifierKind.Oaid -> Icons.Rounded.Campaign
}

private fun DeviceIdentifierKind.titleRes(): Int = when (this) {
    DeviceIdentifierKind.SerialNumber -> R.string.device_identity_serial
    DeviceIdentifierKind.AndroidId -> R.string.device_identity_android_id
    DeviceIdentifierKind.WifiMac -> R.string.device_identity_wifi_mac
    DeviceIdentifierKind.BluetoothAddress -> R.string.device_identity_bluetooth_address
    DeviceIdentifierKind.Oaid -> R.string.device_identity_oaid
}

private fun DeviceIdentifierKind.summaryRes(): Int = when (this) {
    DeviceIdentifierKind.SerialNumber -> R.string.device_identity_serial_summary
    DeviceIdentifierKind.AndroidId -> R.string.device_identity_android_id_summary
    DeviceIdentifierKind.WifiMac -> R.string.device_identity_wifi_summary
    DeviceIdentifierKind.BluetoothAddress -> R.string.device_identity_bluetooth_summary
    DeviceIdentifierKind.Oaid -> R.string.device_identity_oaid_summary
}

private fun DeviceIdentifierKind.editorHelpRes(): Int = when (this) {
    DeviceIdentifierKind.SerialNumber -> R.string.device_identity_serial_help
    DeviceIdentifierKind.AndroidId -> R.string.device_identity_android_id_help
    DeviceIdentifierKind.WifiMac -> R.string.device_identity_wifi_help
    DeviceIdentifierKind.BluetoothAddress -> R.string.device_identity_bluetooth_help
    DeviceIdentifierKind.Oaid -> R.string.device_identity_oaid_help
}

private fun DeviceIdentifierKind.applyConfirmationRes(): Int = when (this) {
    DeviceIdentifierKind.SerialNumber -> R.string.device_identity_serial_confirm
    DeviceIdentifierKind.AndroidId -> R.string.device_identity_android_id_confirm
    DeviceIdentifierKind.WifiMac -> R.string.device_identity_wifi_confirm
    DeviceIdentifierKind.BluetoothAddress -> R.string.device_identity_bluetooth_confirm
    DeviceIdentifierKind.Oaid -> R.string.device_identity_oaid_confirm
}

private fun DeviceIdentifierSupport.messageRes(): Int = when (this) {
    DeviceIdentifierSupport.Supported -> R.string.device_identity_value_unavailable
    DeviceIdentifierSupport.Missing -> R.string.device_identity_source_missing
    DeviceIdentifierSupport.UnsafeFormat -> R.string.device_identity_unsafe_format
    DeviceIdentifierSupport.ToolUnavailable -> R.string.device_identity_tool_unavailable
}

private fun DeviceIdentityValidationError.messageRes(): Int = when (this) {
    DeviceIdentityValidationError.Empty -> R.string.device_identity_error_empty
    DeviceIdentityValidationError.InvalidSerial -> R.string.device_identity_error_serial
    DeviceIdentityValidationError.InvalidAndroidId -> R.string.device_identity_error_android_id
    DeviceIdentityValidationError.InvalidMac -> R.string.device_identity_error_mac
    DeviceIdentityValidationError.MulticastMac -> R.string.device_identity_error_multicast_mac
    DeviceIdentityValidationError.InvalidOaid -> R.string.device_identity_error_oaid
}

private fun DeviceIdentityFailure?.messageRes(): Int = when (this) {
    DeviceIdentityFailure.RootUnavailable -> R.string.device_identity_failure_root
    DeviceIdentityFailure.Unsupported -> R.string.device_identity_failure_unsupported
    DeviceIdentityFailure.InvalidValue -> R.string.device_identity_failure_invalid_value
    DeviceIdentityFailure.BackupMissing -> R.string.device_identity_failure_backup_missing
    DeviceIdentityFailure.BackupFailed -> R.string.device_identity_failure_backup
    DeviceIdentityFailure.PersistenceFailed -> R.string.device_identity_failure_persistence
    DeviceIdentityFailure.CommandTimeout -> R.string.device_identity_failure_timeout
    DeviceIdentityFailure.VerificationFailed -> R.string.device_identity_failure_verification
    DeviceIdentityFailure.RollbackFailed -> R.string.device_identity_failure_rollback
    DeviceIdentityFailure.CommandFailed,
    null,
    -> R.string.device_identity_failure_command
}
