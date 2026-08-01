package me.weishu.kernelsu.ui.screen.superuser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.util.AppIdFailure
import me.weishu.kernelsu.ui.util.AppIdSnapshot
import me.weishu.kernelsu.ui.util.SsaidXmlEditor
import me.weishu.kernelsu.ui.viewmodel.AppIdAppGroup
import me.weishu.kernelsu.ui.viewmodel.AppIdManagerUiState
import me.weishu.kernelsu.ui.viewmodel.AppIdManagerViewModel
import me.weishu.kernelsu.ui.viewmodel.AppIdNotice
import me.weishu.kernelsu.ui.viewmodel.filterAppIdGroups
import java.text.DateFormat
import java.util.Date

private enum class AppIdConfirmation {
    Apply,
    Restore,
    RandomReset,
}

@Composable
fun AppIdManagerScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel = viewModel<AppIdManagerViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onBack = dropUnlessResumed { navigator.pop() }
    var confirmation by rememberSaveable { mutableStateOf<AppIdConfirmation?>(null) }
    var randomResetUid by rememberSaveable { mutableStateOf<Int?>(null) }
    val visibleGroups = remember(state.groups, state.query, state.showSystemApps) {
        filterAppIdGroups(state.groups, state.query, state.showSystemApps)
    }

    LaunchedEffect(state.notice) {
        val message = when (state.notice) {
            AppIdNotice.Staged -> R.string.app_id_manager_staged
            AppIdNotice.Restored -> R.string.app_id_manager_restore_staged
            AppIdNotice.PendingCanceled -> R.string.app_id_manager_pending_canceled
            null -> return@LaunchedEffect
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        viewModel.clearNotice()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = immersivePageColor(MaterialTheme.colorScheme.background),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_id_manager_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refreshApps,
                        enabled = !state.loadingApps && !state.busy,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.app_id_manager_refresh),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = immersiveTopBarColor(MaterialTheme.colorScheme.background),
                    scrolledContainerColor = immersiveScrolledTopBarColor(
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppIdSafetyCard()
            }
            item {
                AppIdSearchSection(
                    query = state.query,
                    showSystemApps = state.showSystemApps,
                    onQueryChange = viewModel::updateQuery,
                    onShowSystemAppsChange = viewModel::setShowSystemApps,
                )
            }
            state.selected?.let { selected ->
                item(key = "editor-${selected.uid}") {
                    AppIdEditor(
                        state = state,
                        onClose = viewModel::closeSelection,
                        onDraftChange = viewModel::updateDraft,
                        onGenerate = viewModel::generateRandomId,
                        onApply = { confirmation = AppIdConfirmation.Apply },
                        onRestore = { confirmation = AppIdConfirmation.Restore },
                        onCancelPending = viewModel::cancelPending,
                        onRetry = viewModel::refreshSelection,
                        onCopy = { value -> copyAppId(context, value) },
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.app_id_manager_apps, visibleGroups.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (state.loadingApps) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            state.listError?.let { failure ->
                item {
                    AppIdErrorCard(failure = failure, onRetry = viewModel::refreshApps)
                }
            }
            if (!state.loadingApps && state.listError == null && visibleGroups.isEmpty()) {
                item {
                    AppIdEmptyCard()
                }
            }
            items(visibleGroups, key = { it.uid }) { group ->
                AppIdAppRow(
                    group = group,
                    snapshot = state.snapshots[group.uid],
                    failure = state.actionErrors[group.uid],
                    selected = state.selected?.uid == group.uid,
                    enabled = !state.busy && !state.loadingApps,
                    loading = state.loadingApps && state.snapshots[group.uid] == null,
                    busy = state.actionUid == group.uid,
                    onClick = { viewModel.select(group) },
                    onCopy = { value -> copyAppId(context, value) },
                    onRandomReset = {
                        randomResetUid = group.uid
                        confirmation = AppIdConfirmation.RandomReset
                    },
                )
            }
        }
    }

    when (confirmation) {
        AppIdConfirmation.Apply -> {
            val selected = state.selected
            AlertDialog(
                onDismissRequest = { if (!state.busy) confirmation = null },
                icon = { Icon(Icons.Rounded.Fingerprint, contentDescription = null) },
                title = { Text(stringResource(R.string.app_id_manager_apply_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.app_id_manager_apply_message,
                            selected?.primary?.label.orEmpty(),
                            state.draft.lowercase(),
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmation = null
                            viewModel.stageDraft()
                        },
                        enabled = SsaidXmlEditor.isValidAppId(state.draft.trim()) && !state.busy,
                    ) {
                        Text(stringResource(R.string.app_id_manager_apply))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmation = null }, enabled = !state.busy) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }

        AppIdConfirmation.Restore -> {
            AlertDialog(
                onDismissRequest = { if (!state.busy) confirmation = null },
                icon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                title = { Text(stringResource(R.string.app_id_manager_restore_title)) },
                text = { Text(stringResource(R.string.app_id_manager_restore_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmation = null
                            viewModel.restoreOriginal()
                        },
                        enabled = !state.busy,
                    ) {
                        Text(stringResource(R.string.app_id_manager_restore))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmation = null }, enabled = !state.busy) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }

        AppIdConfirmation.RandomReset -> {
            val target = state.groups.firstOrNull { it.uid == randomResetUid }
            if (target != null) {
                AlertDialog(
                    onDismissRequest = {
                        if (!state.busy) {
                            confirmation = null
                            randomResetUid = null
                        }
                    },
                    icon = { Icon(Icons.Rounded.AutoFixHigh, contentDescription = null) },
                    title = { Text(stringResource(R.string.app_id_manager_random_reset_title)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(
                                    R.string.app_id_manager_random_reset_message,
                                    target.primary.label,
                                )
                            )
                            if (target.apps.size > 1) {
                                Text(
                                    text = stringResource(
                                        R.string.app_id_manager_shared_uid_warning,
                                        target.apps.size,
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                confirmation = null
                                randomResetUid = null
                                viewModel.randomReset(target)
                            },
                            enabled = !state.busy,
                        ) {
                            Text(stringResource(R.string.app_id_manager_random_reset))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                confirmation = null
                                randomResetUid = null
                            },
                            enabled = !state.busy,
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    },
                )
            }
        }

        null -> Unit
    }
}

@Composable
private fun AppIdSafetyCard() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.app_id_manager_reboot_required),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.app_id_manager_safety_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun AppIdSearchSection(
    query: String,
    showSystemApps: Boolean,
    onQueryChange: (String) -> Unit,
    onShowSystemAppsChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (query.isNotBlank()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.app_id_manager_clear_search),
                        )
                    }
                }
            } else {
                null
            },
            label = { Text(stringResource(R.string.app_id_manager_search)) },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowSystemAppsChange(!showSystemApps) }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.show_system_apps),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.app_id_manager_system_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            StyledSwitch(
                checked = showSystemApps,
                onCheckedChange = onShowSystemAppsChange,
            )
        }
    }
}

@Composable
private fun AppIdEditor(
    state: AppIdManagerUiState,
    onClose: () -> Unit,
    onDraftChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onApply: () -> Unit,
    onRestore: () -> Unit,
    onCancelPending: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val group = state.selected ?: return
    val snapshot = state.selectedSnapshot
    val normalizedDraft = state.draft.trim().lowercase()
    val validDraft = SsaidXmlEditor.isValidAppId(normalizedDraft)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconImage(
                    packageInfo = group.primary.packageInfo,
                    label = group.primary.label,
                    modifier = Modifier.size(48.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = group.primary.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.app_id_manager_uid_user,
                            group.uid,
                            group.uid / 100000,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClose, enabled = !state.busy) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }

            if (group.apps.size > 1) {
                Text(
                    text = stringResource(R.string.app_id_manager_shared_uid_warning, group.apps.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.loadingSelection) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (state.selectedError != null) {
                AppIdInlineError(failure = state.selectedError, onRetry = onRetry)
            } else if (snapshot != null) {
                HorizontalDivider()
                AppIdValueRow(
                    label = stringResource(R.string.app_id_manager_current_value),
                    value = snapshot.currentId,
                    missingText = stringResource(R.string.app_id_manager_not_allocated),
                    onCopy = onCopy,
                )
                if (snapshot.hasPendingChange) {
                    AppIdValueRow(
                        label = stringResource(R.string.app_id_manager_pending_value),
                        value = snapshot.pendingId,
                        missingText = stringResource(R.string.app_id_manager_regenerate_after_reboot),
                        onCopy = onCopy,
                        pending = true,
                    )
                }
                if (snapshot.hasOriginalBackup) {
                    val backupText = when {
                        snapshot.originalWasAbsent -> stringResource(R.string.app_id_manager_original_absent)
                        snapshot.originalId != null -> snapshot.originalId
                        else -> stringResource(R.string.app_id_manager_not_allocated)
                    }
                    AppIdValueRow(
                        label = stringResource(R.string.app_id_manager_original_value),
                        value = snapshot.originalId,
                        missingText = backupText,
                        onCopy = onCopy,
                    )
                    snapshot.backupTimestamp?.let { timestamp ->
                        Text(
                            text = stringResource(
                                R.string.app_id_manager_backup_time,
                                DateFormat.getDateTimeInstance().format(Date(timestamp)),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                snapshot.lastApplied?.let { lastApplied ->
                    Text(
                        text = stringResource(R.string.app_id_manager_last_applied, lastApplied),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.app_id_manager_custom_value)) },
                    supportingText = {
                        Text(
                            text = if (state.draft.isBlank() || validDraft) {
                                stringResource(R.string.app_id_manager_format_hint)
                            } else {
                                stringResource(R.string.app_id_manager_invalid_value)
                            },
                        )
                    },
                    isError = state.draft.isNotBlank() && !validDraft,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    trailingIcon = {
                        IconButton(onClick = onGenerate, enabled = !state.busy) {
                            Icon(
                                imageVector = Icons.Rounded.AutoFixHigh,
                                contentDescription = stringResource(R.string.app_id_manager_generate),
                            )
                        }
                    },
                )

                Button(
                    onClick = onApply,
                    enabled = validDraft && !state.busy && normalizedDraft != (snapshot.pendingId ?: snapshot.currentId),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.app_id_manager_apply))
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onRestore,
                        enabled = snapshot.hasOriginalBackup &&
                            !state.busy &&
                            (if (snapshot.originalWasAbsent) null else snapshot.originalId) !=
                            (if (snapshot.hasPendingChange) snapshot.pendingId else snapshot.currentId),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.app_id_manager_restore), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onCancelPending,
                        enabled = snapshot.hasPendingChange && !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.app_id_manager_cancel_pending), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIdValueRow(
    label: String,
    value: String?,
    missingText: String,
    onCopy: (String) -> Unit,
    pending: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (pending) Icons.Rounded.Schedule else Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (pending) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value ?: missingText,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = if (value != null) FontFamily.Monospace else FontFamily.Default,
                fontWeight = FontWeight.Medium,
            )
        }
        if (value != null) {
            IconButton(onClick = { onCopy(value) }) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(R.string.app_id_manager_copy),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AppIdAppRow(
    group: AppIdAppGroup,
    snapshot: AppIdSnapshot?,
    failure: AppIdFailure?,
    selected: Boolean,
    enabled: Boolean,
    loading: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    onCopy: (String) -> Unit,
    onRandomReset: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow)
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconImage(
                    packageInfo = group.primary.packageInfo,
                    label = group.primary.label,
                    modifier = Modifier.size(48.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = group.primary.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = group.primary.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.app_id_manager_version,
                            group.primary.packageInfo.versionName?.takeIf(String::isNotBlank) ?: "-",
                            PackageInfoCompat.getLongVersionCode(group.primary.packageInfo),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    if (group.apps.size > 1) {
                        Text(
                            text = stringResource(R.string.app_id_manager_shared_packages, group.apps.size, group.uid),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Fingerprint,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SSAID",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(58.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = when {
                            snapshot?.currentId != null -> snapshot.currentId
                            loading -> stringResource(R.string.app_id_manager_reading_value)
                            snapshot == null -> stringResource(R.string.app_id_manager_value_unavailable)
                            else -> stringResource(R.string.app_id_manager_not_allocated)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = if (snapshot?.currentId != null) FontFamily.Monospace else FontFamily.Default,
                        fontWeight = FontWeight.Medium,
                        color = if (snapshot?.currentId != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (snapshot?.hasPendingChange == true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.app_id_manager_pending_inline,
                                    snapshot.pendingId
                                        ?: stringResource(R.string.app_id_manager_regenerate_after_reboot),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontFamily = if (snapshot.pendingId != null) FontFamily.Monospace else FontFamily.Default,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            failure?.let {
                AppIdFailureText(failure = it, color = MaterialTheme.colorScheme.error)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { snapshot?.currentId?.let(onCopy) },
                    enabled = enabled && snapshot?.currentId != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.app_id_manager_copy_action),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilledTonalButton(
                    onClick = onRandomReset,
                    enabled = enabled && !loading,
                    modifier = Modifier.weight(1f),
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.app_id_manager_random_reset),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIdErrorCard(failure: AppIdFailure, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppIdFailureText(
                failure = failure,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.network_retry))
            }
        }
    }
}

@Composable
private fun AppIdInlineError(failure: AppIdFailure, onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            AppIdFailureText(
                failure = failure,
                color = MaterialTheme.colorScheme.error,
            )
        }
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.network_retry))
        }
    }
}

@Composable
private fun AppIdFailureText(failure: AppIdFailure, color: Color) {
    Text(
        text = stringResource(
            when (failure) {
                AppIdFailure.RootUnavailable -> R.string.app_id_manager_error_root
                AppIdFailure.SettingsFileMissing -> R.string.app_id_manager_error_file_missing
                AppIdFailure.InvalidSettingsFile -> R.string.app_id_manager_error_invalid_xml
                AppIdFailure.InvalidAppId -> R.string.app_id_manager_invalid_value
                AppIdFailure.BackupMissing -> R.string.app_id_manager_error_backup_missing
                AppIdFailure.CommandTimeout -> R.string.app_id_manager_error_timeout
                AppIdFailure.CommandFailed -> R.string.app_id_manager_error_command
                AppIdFailure.StagingPreparationFailed -> R.string.app_id_manager_error_stage_prepare
                AppIdFailure.BootScriptStagingFailed -> R.string.app_id_manager_error_stage_script
                AppIdFailure.PendingXmlStagingFailed -> R.string.app_id_manager_error_stage_xml
                AppIdFailure.StagingVerificationFailed -> R.string.app_id_manager_error_stage_verify
            }
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = color,
    )
}

@Composable
private fun AppIdEmptyCard() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Apps, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.app_id_manager_empty))
        }
    }
}

private fun copyAppId(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_id_manager_title), value))
    Toast.makeText(context, R.string.app_id_manager_copied, Toast.LENGTH_SHORT).show()
}
