package me.weishu.kernelsu.ui.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AppBlocking
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.ForegroundToolFailure
import me.weishu.kernelsu.ui.util.ForegroundToolRuntimeState
import me.weishu.kernelsu.ui.util.ForegroundToolStatus
import me.weishu.kernelsu.ui.viewmodel.ForegroundToolApp
import me.weishu.kernelsu.ui.viewmodel.ForegroundToolNotice
import me.weishu.kernelsu.ui.viewmodel.ForegroundToolProtectionUiState
import me.weishu.kernelsu.ui.viewmodel.ForegroundToolProtectionViewModel
import me.weishu.kernelsu.ui.viewmodel.filterForegroundToolApps

private enum class ForegroundToolPage(@StringRes val titleRes: Int) {
    Status(R.string.foreground_tool_tab_status),
    Targets(R.string.foreground_tool_tab_targets),
    Tools(R.string.foreground_tool_tab_tools),
}

@Composable
fun ForegroundToolProtectionTopBarAction(onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above,
        ),
        tooltip = {
            PlainTooltip {
                Text(stringResource(R.string.foreground_tool_open))
            }
        },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                Icons.Rounded.AppBlocking,
                contentDescription = stringResource(R.string.foreground_tool_open),
            )
        }
    }
}

@Composable
fun ForegroundToolProtectionScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel = viewModel<ForegroundToolProtectionViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onBack = dropUnlessResumed { navigator.pop() }
    var selectedPage by rememberSaveable { mutableIntStateOf(0) }
    var targetQuery by rememberSaveable { mutableStateOf("") }
    var toolQuery by rememberSaveable { mutableStateOf("") }
    var showTargetSystemApps by rememberSaveable { mutableStateOf(false) }
    var showToolSystemApps by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.notice) {
        val message = when (state.notice) {
            ForegroundToolNotice.Enabled -> R.string.foreground_tool_notice_enabled
            ForegroundToolNotice.Disabled -> R.string.foreground_tool_notice_disabled
            ForegroundToolNotice.AutoDisabled -> R.string.foreground_tool_notice_auto_disabled
            ForegroundToolNotice.MovedFromTools -> R.string.foreground_tool_notice_moved_from_tools
            ForegroundToolNotice.MovedFromTargets -> R.string.foreground_tool_notice_moved_from_targets
            ForegroundToolNotice.LogCleared -> R.string.foreground_tool_notice_log_cleared
            null -> return@LaunchedEffect
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        viewModel.consumeNotice()
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.foreground_tool_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !state.busy && !state.loadingStatus && !state.loadingApps,
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.foreground_tool_refresh),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                ),
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedPage) {
                ForegroundToolPage.entries.forEachIndexed { index, page ->
                    Tab(
                        selected = selectedPage == index,
                        onClick = { selectedPage = index },
                        text = {
                            Text(
                                stringResource(page.titleRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
            if (state.loadingStatus || state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Spacer(Modifier.height(4.dp))
            }

            when (ForegroundToolPage.entries[selectedPage]) {
                ForegroundToolPage.Status -> ForegroundToolStatusPage(
                    state = state,
                    onEnabledChange = viewModel::setEnabled,
                    onOpenTargets = { selectedPage = ForegroundToolPage.Targets.ordinal },
                    onOpenTools = { selectedPage = ForegroundToolPage.Tools.ordinal },
                    onCopyLog = { copyForegroundToolLog(context, state.status.recentLog) },
                    onClearLog = viewModel::clearLog,
                    onRetry = viewModel::refresh,
                )

                ForegroundToolPage.Targets -> ForegroundToolAppPage(
                    apps = state.apps,
                    selectedPackages = state.targets,
                    query = targetQuery,
                    showSystemApps = showTargetSystemApps,
                    loading = state.loadingApps,
                    listFailed = state.appListFailed,
                    busy = state.busy || state.loadingStatus || state.loadingApps,
                    role = ForegroundToolPage.Targets,
                    onQueryChange = { targetQuery = it },
                    onShowSystemAppsChange = { showTargetSystemApps = it },
                    onSelectedChange = viewModel::setTargetSelected,
                    onRetry = viewModel::refresh,
                )

                ForegroundToolPage.Tools -> ForegroundToolAppPage(
                    apps = state.apps,
                    selectedPackages = state.tools,
                    query = toolQuery,
                    showSystemApps = showToolSystemApps,
                    loading = state.loadingApps,
                    listFailed = state.appListFailed,
                    busy = state.busy || state.loadingStatus || state.loadingApps,
                    role = ForegroundToolPage.Tools,
                    onQueryChange = { toolQuery = it },
                    onShowSystemAppsChange = { showToolSystemApps = it },
                    onSelectedChange = viewModel::setToolSelected,
                    onRetry = viewModel::refresh,
                )
            }
        }
    }
}

@Composable
private fun ForegroundToolStatusPage(
    state: ForegroundToolProtectionUiState,
    onEnabledChange: (Boolean) -> Unit,
    onOpenTargets: () -> Unit,
    onOpenTools: () -> Unit,
    onCopyLog: () -> Unit,
    onClearLog: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ForegroundToolRuntimeCard(status = state.status, stale = state.statusStale)
        }
        item {
            ForegroundToolToggleCard(
                state = state,
                onEnabledChange = onEnabledChange,
                onOpenTargets = onOpenTargets,
                onOpenTools = onOpenTools,
            )
        }
        state.failure?.let { failure ->
            item {
                ForegroundToolErrorCard(failure = failure, onRetry = onRetry)
            }
        }
        item {
            ForegroundToolDetailsCard(status = state.status)
        }
        item {
            ForegroundToolWarningCard()
        }
        item {
            ForegroundToolLogCard(
                lines = state.status.recentLog,
                enabled = !state.busy,
                onCopy = onCopyLog,
                onClear = onClearLog,
            )
        }
    }
}

@Composable
private fun ForegroundToolRuntimeCard(status: ForegroundToolStatus, stale: Boolean) {
    val state = status.runtimeState
    val containerColor = when (state) {
        ForegroundToolRuntimeState.Active -> MaterialTheme.colorScheme.tertiaryContainer
        ForegroundToolRuntimeState.Waiting -> MaterialTheme.colorScheme.primaryContainer
        ForegroundToolRuntimeState.Error -> MaterialTheme.colorScheme.errorContainer
        ForegroundToolRuntimeState.Disabled -> MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = when (state) {
        ForegroundToolRuntimeState.Active -> MaterialTheme.colorScheme.onTertiaryContainer
        ForegroundToolRuntimeState.Waiting -> MaterialTheme.colorScheme.onPrimaryContainer
        ForegroundToolRuntimeState.Error -> MaterialTheme.colorScheme.onErrorContainer
        ForegroundToolRuntimeState.Disabled -> MaterialTheme.colorScheme.onSurface
    }
    val icon = when (state) {
        ForegroundToolRuntimeState.Active -> Icons.Rounded.CheckCircle
        ForegroundToolRuntimeState.Waiting -> Icons.Rounded.HourglassTop
        ForegroundToolRuntimeState.Error -> Icons.Rounded.ErrorOutline
        ForegroundToolRuntimeState.Disabled -> Icons.Rounded.AppBlocking
    }
    val title = when (state) {
        ForegroundToolRuntimeState.Active -> R.string.foreground_tool_status_active
        ForegroundToolRuntimeState.Waiting -> R.string.foreground_tool_status_waiting
        ForegroundToolRuntimeState.Error -> R.string.foreground_tool_status_error
        ForegroundToolRuntimeState.Disabled -> R.string.foreground_tool_status_disabled
    }
    val summary = when (state) {
        ForegroundToolRuntimeState.Active -> stringResource(
            R.string.foreground_tool_status_active_summary,
            status.matchedTarget.ifBlank { stringResource(R.string.foreground_tool_none) },
        )
        ForegroundToolRuntimeState.Waiting -> stringResource(R.string.foreground_tool_status_waiting_summary)
        ForegroundToolRuntimeState.Error -> stringResource(R.string.foreground_tool_status_error_summary)
        ForegroundToolRuntimeState.Disabled -> stringResource(R.string.foreground_tool_status_disabled_summary)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(summary, style = MaterialTheme.typography.bodyMedium)
                if (stale) {
                    Text(
                        stringResource(R.string.foreground_tool_status_stale),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ForegroundToolToggleCard(
    state: ForegroundToolProtectionUiState,
    onEnabledChange: (Boolean) -> Unit,
    onOpenTargets: () -> Unit,
    onOpenTools: () -> Unit,
) {
    val enabled = state.status.config.enabled
    val canEnable = state.targets.isNotEmpty() && state.tools.isNotEmpty()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        stringResource(R.string.foreground_tool_master_switch),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.foreground_tool_master_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                StyledSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = !state.busy && !state.loadingStatus && (enabled || canEnable),
                )
            }
            Text(
                stringResource(
                    R.string.foreground_tool_selection_count,
                    state.targets.size,
                    state.tools.size,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (canEnable) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenTargets,
                    modifier = Modifier.weight(1f),
                    enabled = !state.busy,
                ) {
                    Text(stringResource(R.string.foreground_tool_choose_targets))
                }
                OutlinedButton(
                    onClick = onOpenTools,
                    modifier = Modifier.weight(1f),
                    enabled = !state.busy,
                ) {
                    Text(stringResource(R.string.foreground_tool_choose_tools))
                }
            }
        }
    }
}

@Composable
private fun ForegroundToolDetailsCard(status: ForegroundToolStatus) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.foreground_tool_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ForegroundToolDetailRow(
                stringResource(R.string.foreground_tool_service),
                when {
                    status.serviceRunning -> stringResource(R.string.foreground_tool_service_running)
                    status.serviceInstalled -> stringResource(R.string.foreground_tool_service_stopped)
                    else -> stringResource(R.string.foreground_tool_service_not_installed)
                },
            )
            ForegroundToolDetailRow(
                stringResource(R.string.foreground_tool_foreground_app),
                status.foregroundPackage.ifBlank { stringResource(R.string.foreground_tool_none) },
            )
            ForegroundToolDetailRow(
                stringResource(R.string.foreground_tool_matched_app),
                status.matchedTarget.ifBlank { stringResource(R.string.foreground_tool_none) },
            )
            ForegroundToolDetailRow(
                stringResource(R.string.foreground_tool_last_result),
                stringResource(
                    R.string.foreground_tool_last_result_value,
                    status.stoppedCount,
                    status.failedCount,
                ),
            )
            ForegroundToolDetailRow(
                stringResource(R.string.foreground_tool_last_update),
                status.updatedAt.ifBlank { stringResource(R.string.foreground_tool_none) },
            )
        }
    }
}

@Composable
private fun ForegroundToolDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            modifier = Modifier.weight(0.38f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.weight(0.62f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ForegroundToolWarningCard() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.foreground_tool_warning_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.foreground_tool_warning_summary),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ForegroundToolLogCard(
    lines: List<String>,
    enabled: Boolean,
    onCopy: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.foreground_tool_logs),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onCopy, enabled = enabled && lines.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.foreground_tool_copy_log),
                    )
                }
                IconButton(onClick = onClear, enabled = enabled && lines.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.foreground_tool_clear_log),
                    )
                }
            }
            if (lines.isEmpty()) {
                Text(
                    stringResource(R.string.foreground_tool_no_logs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SelectionContainer {
                    Text(
                        lines.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ForegroundToolErrorCard(failure: ForegroundToolFailure, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null)
            Text(
                foregroundToolFailureText(failure),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(R.string.foreground_tool_retry))
            }
        }
    }
}

@Composable
private fun ForegroundToolAppPage(
    apps: List<ForegroundToolApp>,
    selectedPackages: Set<String>,
    query: String,
    showSystemApps: Boolean,
    loading: Boolean,
    listFailed: Boolean,
    busy: Boolean,
    role: ForegroundToolPage,
    onQueryChange: (String) -> Unit,
    onShowSystemAppsChange: (Boolean) -> Unit,
    onSelectedChange: (String, Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    val visibleApps = remember(apps, selectedPackages, query, showSystemApps) {
        filterForegroundToolApps(apps, selectedPackages, query, showSystemApps)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f),
            ) {
                Text(
                    stringResource(
                        if (role == ForegroundToolPage.Targets) {
                            R.string.foreground_tool_target_intro
                        } else {
                            R.string.foreground_tool_tools_intro
                        },
                    ),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.foreground_tool_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = stringResource(R.string.foreground_tool_clear_search),
                            )
                        }
                    }
                },
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = showSystemApps,
                        role = Role.Checkbox,
                        onValueChange = onShowSystemAppsChange,
                    )
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = showSystemApps, onCheckedChange = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.foreground_tool_show_system_apps))
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.foreground_tool_selected_count, selectedPackages.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (loading && apps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        if (listFailed) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.foreground_tool_app_list_failed),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(onClick = onRetry, enabled = !busy) {
                            Text(stringResource(R.string.foreground_tool_retry))
                        }
                    }
                }
            }
        }
        if (!loading && visibleApps.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Text(
                        stringResource(R.string.foreground_tool_no_apps),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(visibleApps, key = ForegroundToolApp::packageName) { app ->
            ForegroundToolAppRow(
                app = app,
                selected = app.packageName in selectedPackages,
                showRecommended = role == ForegroundToolPage.Tools,
                enabled = !busy,
                onSelectedChange = { selected -> onSelectedChange(app.packageName, selected) },
            )
        }
    }
}

@Composable
private fun ForegroundToolAppRow(
    app: ForegroundToolApp,
    selected: Boolean,
    showRecommended: Boolean,
    enabled: Boolean,
    onSelectedChange: (Boolean) -> Unit,
) {
    val rowEnabled = enabled && (app.installed || selected)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = selected,
                    enabled = rowEnabled,
                    role = Role.Checkbox,
                    onValueChange = onSelectedChange,
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val packageInfo = app.packageInfo
            if (packageInfo != null) {
                AppIconImage(
                    packageInfo = packageInfo,
                    label = app.label,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Icon(
                    Icons.Rounded.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val marker = when {
                    !app.installed -> R.string.foreground_tool_not_installed
                    showRecommended && app.recommended -> R.string.foreground_tool_recommended
                    app.isSystem -> R.string.foreground_tool_system_app
                    else -> null
                }
                marker?.let {
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!app.installed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Checkbox(checked = selected, onCheckedChange = null, enabled = rowEnabled)
        }
    }
}

@Composable
private fun foregroundToolFailureText(failure: ForegroundToolFailure): String = stringResource(
    when (failure) {
        ForegroundToolFailure.RootUnavailable -> R.string.foreground_tool_error_root
        ForegroundToolFailure.InvalidPackage -> R.string.foreground_tool_error_invalid_package
        ForegroundToolFailure.TargetRequired -> R.string.foreground_tool_error_target_required
        ForegroundToolFailure.ToolRequired -> R.string.foreground_tool_error_tool_required
        ForegroundToolFailure.SelectionConflict -> R.string.foreground_tool_error_conflict
        ForegroundToolFailure.CommandTimeout -> R.string.foreground_tool_error_timeout
        ForegroundToolFailure.CommandFailed -> R.string.foreground_tool_error_command
        ForegroundToolFailure.ServiceStartFailed -> R.string.foreground_tool_error_service
        ForegroundToolFailure.InstallPreparationFailed -> R.string.foreground_tool_error_install_prepare
        ForegroundToolFailure.ConfigStagingFailed -> R.string.foreground_tool_error_install_config
        ForegroundToolFailure.ServiceInstallFailed -> R.string.foreground_tool_error_install_service
        ForegroundToolFailure.InstallVerificationFailed -> R.string.foreground_tool_error_install_verify
    },
)

private fun copyForegroundToolLog(context: Context, lines: List<String>) {
    if (lines.isEmpty()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("ApkeSU foreground tool protection", lines.joinToString("\n")))
    Toast.makeText(context, R.string.foreground_tool_log_copied, Toast.LENGTH_SHORT).show()
}
