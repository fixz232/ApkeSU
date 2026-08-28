@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package me.weishu.kernelsu.ui.screen.superuser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.component.ApkeEmptyState
import me.weishu.kernelsu.ui.component.ApkeAppSort
import me.weishu.kernelsu.ui.component.ApkeAppSortMenu
import me.weishu.kernelsu.ui.component.ApkeErrorState
import me.weishu.kernelsu.ui.component.ApkeLoadingState
import me.weishu.kernelsu.ui.component.ApkeMetricGrid
import me.weishu.kernelsu.ui.component.ApkeMetricItem
import me.weishu.kernelsu.ui.component.ApkeSecondaryScaffold
import me.weishu.kernelsu.ui.component.ApkeSelectionToolbar
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.util.AppFreezeFailure
import me.weishu.kernelsu.ui.util.AppFreezeProtection
import me.weishu.kernelsu.ui.util.FreezableApp
import me.weishu.kernelsu.ui.viewmodel.AppFreezeFilter
import me.weishu.kernelsu.ui.viewmodel.AppFreezeNotice
import me.weishu.kernelsu.ui.viewmodel.AppFreezeUiState
import me.weishu.kernelsu.ui.viewmodel.AppFreezeViewModel
import me.weishu.kernelsu.ui.viewmodel.visibleAppFreezeApps

@Composable
fun AppFreezeScreen() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<AppFreezeViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sort by rememberSaveable { mutableStateOf(ApkeAppSort.Name) }
    val visibleApps = remember(state, sort) {
        val apps = visibleAppFreezeApps(state)
        when (sort) {
            ApkeAppSort.Name -> apps.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.label },
            )
            ApkeAppSort.PackageName -> apps.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.packageName },
            )
            ApkeAppSort.UserId -> apps.sortedWith(
                compareBy<FreezableApp> { it.userId }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
            )
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingFreeze by remember { mutableStateOf<FreezableApp?>(null) }
    var pendingBatchFrozen by remember { mutableStateOf<Boolean?>(null) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(emptySet<String>()) }
    var hasResumed by rememberSaveable { mutableStateOf(false) }
    val onBack = dropUnlessResumed { navigator.pop() }
    val noticeMessage = appFreezeNoticeMessage(state.notice)
    val selectedApps = remember(visibleApps, selectedKeys) {
        visibleApps.filter { it.selectionKey() in selectedKeys }
    }

    LaunchedEffect(state.apps) {
        val installedKeys = state.apps.mapTo(hashSetOf(), FreezableApp::selectionKey)
        selectedKeys = selectedKeys.intersect(installedKeys)
    }

    LifecycleResumeEffect(Unit) {
        if (hasResumed) viewModel.refresh() else hasResumed = true
        onPauseOrDispose { }
    }
    LaunchedEffect(state.notice, noticeMessage) {
        if (
            (state.notice is AppFreezeNotice.Changed || state.notice is AppFreezeNotice.BatchChanged) &&
            noticeMessage.isNotBlank()
        ) {
            snackbarHostState.showSnackbar(noticeMessage)
            viewModel.consumeNotice()
        }
    }

    ApkeSecondaryScaffold(
        title = stringResource(R.string.app_freeze_title),
        onBack = onBack,
        containerColor = immersivePageColor(MaterialTheme.colorScheme.background),
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(
                enabled = !state.loading,
                onClick = {
                    selectionMode = !selectionMode
                    if (!selectionMode) selectedKeys = emptySet()
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.SelectAll,
                    contentDescription = stringResource(R.string.app_list_select_mode),
                    tint = if (selectionMode) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            ApkeAppSortMenu(
                selected = sort,
                onSelected = { sort = it },
                enabled = !state.loading,
            )
            IconButton(
                enabled = !state.refreshing,
                onClick = viewModel::refresh,
            ) {
                if (state.refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.refresh_refresh),
                    )
                }
            }
        },
    ) { paddingValues, _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                AppFreezeSafetyNotice()
            }
            item {
                AppFreezeSummary(state)
            }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateQuery("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.app_freeze_clear_search),
                                )
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.app_freeze_search)) },
                )
            }
            item {
                AppFreezeFilters(
                    state = state,
                    onFilter = viewModel::updateFilter,
                    onToggleSystem = viewModel::toggleSystemApps,
                )
            }
            if (selectionMode) {
                item {
                    ApkeSelectionToolbar(
                        selectedCount = selectedApps.size,
                        totalCount = visibleApps.size,
                        onSelectAll = {
                            selectedKeys = visibleApps.mapTo(linkedSetOf(), FreezableApp::selectionKey)
                        },
                        onClear = { selectedKeys = emptySet() },
                    ) {
                        IconButton(
                            enabled = selectedApps.any { !it.frozen && it.protection == null },
                            onClick = { pendingBatchFrozen = true },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AcUnit,
                                contentDescription = stringResource(R.string.app_freeze_selected),
                            )
                        }
                        IconButton(
                            enabled = selectedApps.any(FreezableApp::frozen),
                            onClick = { pendingBatchFrozen = false },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = stringResource(R.string.app_unfreeze_selected),
                            )
                        }
                    }
                }
            }
            val actionFailure = state.notice as? AppFreezeNotice.Failed
            if (actionFailure != null) {
                item {
                    ApkeErrorState(
                        title = appFreezeNoticeMessage(actionFailure),
                        supportingText = stringResource(R.string.app_freeze_retry_summary),
                        onRetry = {
                            viewModel.consumeNotice()
                            viewModel.refresh()
                        },
                    )
                }
            }
            if (state.loading && state.apps.isEmpty()) {
                item { ApkeLoadingState() }
            }
            state.loadError?.let { error ->
                item {
                    ApkeErrorState(
                        title = appFreezeFailureMessage(error.failure, error.detail),
                        supportingText = stringResource(R.string.app_freeze_retry_summary),
                        onRetry = viewModel::refresh,
                    )
                }
            }
            if (!state.loading && state.loadError == null && visibleApps.isEmpty()) {
                item {
                    ApkeEmptyState(
                        title = stringResource(R.string.app_freeze_empty),
                    )
                }
            }
            items(
                items = visibleApps,
                key = { "${it.packageName}:${it.userId}" },
                contentType = { "freezable-app" },
            ) { app ->
                AppFreezeRow(
                    app = app,
                    busy = app.key in state.busyKeys,
                    selectionMode = selectionMode,
                    selected = app.selectionKey() in selectedKeys,
                    onSelectionToggle = {
                        val key = app.selectionKey()
                        selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                    },
                    onToggle = {
                        if (app.frozen) {
                            viewModel.setFrozen(app, false)
                        } else {
                            pendingFreeze = app
                        }
                    },
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    pendingFreeze?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingFreeze = null },
            icon = {
                Icon(
                    imageVector = if (app.systemApp) Icons.Rounded.WarningAmber else Icons.Rounded.AcUnit,
                    contentDescription = null,
                )
            },
            title = { Text(stringResource(R.string.app_freeze_confirm_title, app.label)) },
            text = {
                Text(
                    stringResource(
                        if (app.systemApp) {
                            R.string.app_freeze_confirm_system_message
                        } else {
                            R.string.app_freeze_confirm_message
                        },
                        app.packageName,
                        app.userId,
                    )
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingFreeze = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingFreeze = null
                        viewModel.setFrozen(app, true)
                    },
                ) {
                    Text(stringResource(R.string.app_freeze_action))
                }
            },
        )
    }

    pendingBatchFrozen?.let { freeze ->
        val targets = selectedApps.filter { app ->
            app.frozen != freeze && (!freeze || app.protection == null)
        }
        AlertDialog(
            onDismissRequest = { pendingBatchFrozen = null },
            icon = {
                Icon(
                    imageVector = if (freeze) Icons.Rounded.AcUnit else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                )
            },
            title = {
                Text(
                    stringResource(
                        if (freeze) R.string.app_freeze_selected_title else R.string.app_unfreeze_selected_title,
                    ),
                )
            },
            text = {
                Text(stringResource(R.string.app_freeze_selected_message, targets.size))
            },
            dismissButton = {
                TextButton(onClick = { pendingBatchFrozen = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                    enabled = targets.isNotEmpty(),
                    onClick = {
                        pendingBatchFrozen = null
                        viewModel.setFrozenBatch(targets, freeze)
                        selectedKeys = emptySet()
                        selectionMode = false
                    },
                ) {
                    Text(
                        stringResource(
                            if (freeze) R.string.app_freeze_action else R.string.app_unfreeze_action,
                        ),
                    )
                }
            },
        )
    }
}

@Composable
private fun AppFreezeSafetyNotice() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Rounded.AcUnit, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.app_freeze_notice_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.app_freeze_notice_summary),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun AppFreezeSummary(state: AppFreezeUiState) {
    val frozen = state.apps.count(FreezableApp::frozen)
    val active = state.apps.size - frozen
    ApkeMetricGrid(
        items = listOf(
            ApkeMetricItem(
                label = stringResource(R.string.app_freeze_filter_frozen),
                value = frozen.toString(),
            ),
            ApkeMetricItem(
                label = stringResource(R.string.app_freeze_filter_active),
                value = active.toString(),
            ),
        ),
    )
}

@Composable
private fun AppFreezeMetric(
    value: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppFreezeFilters(
    state: AppFreezeUiState,
    onFilter: (AppFreezeFilter) -> Unit,
    onToggleSystem: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AppFreezeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { onFilter(filter) },
                    label = { Text(stringResource(filter.labelResource())) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleSystem)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.show_system_apps),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.app_freeze_system_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.showSystemApps,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun AppFreezeRow(
    app: FreezableApp,
    busy: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectionToggle: () -> Unit,
    onToggle: () -> Unit,
) {
    val canToggle = app.frozen || app.protection == null
    Surface(
        color = if (app.frozen) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainer)
        },
        shape = RoundedCornerShape(8.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .then(
                        if (selectionMode) {
                            Modifier.clickable(onClick = onSelectionToggle)
                        } else {
                            Modifier
                        },
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onSelectionToggle() },
                    )
                }
                AppIconImage(
                    packageInfo = app.packageInfo,
                    label = app.label,
                    modifier = Modifier.size(46.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.app_freeze_user, app.userId),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    selectionMode -> Unit
                    busy -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                    app.frozen -> FilledTonalButton(onClick = onToggle) {
                        Text(stringResource(R.string.app_unfreeze_action))
                    }
                    canToggle -> OutlinedButton(onClick = onToggle) {
                        Icon(
                            imageVector = Icons.Rounded.AcUnit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(5.dp))
                        Text(stringResource(R.string.app_freeze_action))
                    }
                    else -> Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = stringResource(R.string.app_freeze_protected),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!app.frozen && app.protection != null) {
                HorizontalDivider()
                Text(
                    text = stringResource(app.protection.descriptionResource()),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppFreezeLoadError(
    message: String,
    refreshing: Boolean,
    onRetry: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null)
                Text(
                    text = message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !refreshing,
                onClick = onRetry,
            ) {
                Text(stringResource(R.string.app_freeze_retry))
            }
        }
    }
}

@Composable
private fun AppFreezeEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.app_freeze_empty),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.app_freeze_empty_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun appFreezeNoticeMessage(notice: AppFreezeNotice?): String {
    return when (notice) {
        null -> ""
        is AppFreezeNotice.Changed -> stringResource(
            if (notice.frozen) R.string.app_freeze_success else R.string.app_unfreeze_success,
            notice.label,
        )
        is AppFreezeNotice.BatchChanged -> stringResource(
            if (notice.frozen) R.string.app_freeze_batch_success else R.string.app_unfreeze_batch_success,
            notice.changedCount,
            notice.requestedCount,
        )
        is AppFreezeNotice.Failed -> stringResource(
            R.string.app_freeze_operation_failed,
            notice.label,
            appFreezeFailureMessage(notice.failure, notice.detail),
        )
    }
}

@Composable
private fun appFreezeFailureMessage(failure: AppFreezeFailure, detail: String): String {
    val base = stringResource(
        when (failure) {
            AppFreezeFailure.InvalidTarget -> R.string.app_freeze_error_invalid
            AppFreezeFailure.ProtectedTarget -> R.string.app_freeze_error_protected
            AppFreezeFailure.AppNotFound -> R.string.app_freeze_error_missing
            AppFreezeFailure.RootUnavailable -> R.string.app_freeze_error_root
            AppFreezeFailure.CommandFailed -> R.string.app_freeze_error_command
            AppFreezeFailure.VerificationFailed -> R.string.app_freeze_error_verify
            AppFreezeFailure.PersistenceFailed -> R.string.app_freeze_error_persistence
        }
    )
    return if (detail.isBlank() || detail == failure.name) base else "$base: $detail"
}

private fun AppFreezeFilter.labelResource(): Int = when (this) {
    AppFreezeFilter.All -> R.string.app_freeze_filter_all
    AppFreezeFilter.Frozen -> R.string.app_freeze_filter_frozen
    AppFreezeFilter.Active -> R.string.app_freeze_filter_active
}

private fun AppFreezeProtection.descriptionResource(): Int = when (this) {
    AppFreezeProtection.Manager -> R.string.app_freeze_protection_manager
    AppFreezeProtection.Launcher -> R.string.app_freeze_protection_launcher
    AppFreezeProtection.CriticalSystem -> R.string.app_freeze_protection_critical
    AppFreezeProtection.CoreUid -> R.string.app_freeze_protection_core_uid
}

private fun FreezableApp.selectionKey(): String = "$packageName:$userId"
