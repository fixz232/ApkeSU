package me.weishu.kernelsu.ui.screen.install

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.material.SegmentedCheckboxItem
import me.weishu.kernelsu.ui.component.material.SegmentedColumn
import me.weishu.kernelsu.ui.component.material.SegmentedDropdownItem
import me.weishu.kernelsu.ui.component.material.SegmentedListItem
import me.weishu.kernelsu.ui.component.material.SegmentedRadioItem
import me.weishu.kernelsu.ui.component.material.SnackBarHost
import me.weishu.kernelsu.ui.component.material.TopBarBackButton
import me.weishu.kernelsu.ui.component.material.expressiveTopAppBarColors
import me.weishu.kernelsu.ui.util.BootPatchMode
import me.weishu.kernelsu.ui.util.LkmSelection

/**
 * @author weishu
 * @date 2024/3/12.
 */
@Composable
internal fun InstallScreenMaterial(
    uiState: InstallUiState,
    actions: InstallScreenActions,
    snackBarHost: SnackbarHostState,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            TopBar(
                onBack = actions.onBack,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackBarHost(hostState = snackBarHost, modifier = Modifier.safeDrawingPadding()) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            SelectInstallMethod(
                state = uiState,
                onSelected = actions.onSelectMethod,
                onDownloadFile = actions.onDownloadFile,
                onSelectBootImage = actions.onSelectBootImage,
                onSelectAnyKernel = actions.onSelectAnyKernel,
            )

            if (uiState.installMethod != null && uiState.installMethod !is InstallMethod.AnyKernel && uiState.installMethod !is InstallMethod.DownloadFile) {
                PatchModeSelector(uiState, actions)
            }

            SegmentedColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = buildList {
                    val isDownload = uiState.installMethod is InstallMethod.DownloadFile
                    val partitionItems = if (isDownload) {
                        uiState.remoteDisplayPartitions
                    } else {
                        uiState.displayPartitions
                    }
                    val partitionIndex = if (isDownload) {
                        uiState.remotePartitionSelectionIndex
                    } else {
                        uiState.partitionSelectionIndex
                    }
                    if (partitionItems.isNotEmpty()) add {
                        SegmentedDropdownItem(
                            enabled = uiState.canSelectPartition,
                            items = partitionItems,
                            selectedIndex = partitionIndex,
                            title = if (isDownload) {
                                stringResource(R.string.install_select_partition)
                            } else {
                                "${stringResource(R.string.install_select_partition)} (${uiState.slotSuffix})"
                            },
                            onItemSelected = actions.onSelectPartition,
                            icon = Icons.Filled.Edit
                        )
                    }
                    val usesBuiltInLkm = !isDownload &&
                            (uiState.patchMode != BootPatchMode.Normal ||
                            uiState.lkmSelection !is LkmSelection.LkmUri
                            )
                    if (uiState.installMethod !is InstallMethod.AnyKernel && uiState.installMethod !is InstallMethod.DownloadFile && usesBuiltInLkm) add {
                        TargetKmiItem(uiState, actions.onSelectPatchKmi)
                    }
                    if (uiState.installMethod !is InstallMethod.AnyKernel &&
                        uiState.installMethod !is InstallMethod.DownloadFile &&
                        uiState.patchMode == BootPatchMode.Normal
                    ) add {
                        SegmentedListItem(
                            leadingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.DriveFileMove,
                                    null
                                )
                            },
                            headlineContent = { Text(stringResource(R.string.install_upload_lkm_file)) },
                            supportingContent = {
                                (uiState.lkmSelection as? LkmSelection.LkmUri)?.let {
                                    Text(
                                        stringResource(
                                            R.string.selected_lkm,
                                            it.uri.lastPathSegment ?: "(file)"
                                        )
                                    )
                                }
                            },
                            trailingContent = {
                                if (uiState.lkmSelection is LkmSelection.LkmUri) {
                                    IconButton(onClick = actions.onClearLkm) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(android.R.string.cancel)
                                        )
                                    }
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                                }
                            },
                            onClick = actions.onUploadLkm
                        )
                    }
                }
            )

            SegmentedColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                    item {
                        val rotationState by animateFloatAsState(
                            targetValue = if (uiState.advancedOptionsShown) 180f else 0f,
                            label = "RotationAnimation"
                        )
                        SegmentedListItem(
                            headlineContent = { Text(stringResource(R.string.advanced_options)) },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Filled.ExpandMore,
                                    contentDescription = stringResource(R.string.expand),
                                    modifier = Modifier.graphicsLayer { rotationZ = rotationState }
                                )
                            },
                            onClick = actions.onAdvancedOptionsClicked
                        )
                    }
                    item(visible = uiState.advancedOptionsShown) {
                        SegmentedCheckboxItem(
                            title = stringResource(id = R.string.allow_shell),
                            summary = stringResource(id = R.string.allow_shell_summary),
                            checked = uiState.allowShell,
                            onCheckedChange = actions.onSelectAllowShell,
                        )
                    }
                    item(visible = uiState.advancedOptionsShown && uiState.canForceBackup) {
                        SegmentedCheckboxItem(
                            title = stringResource(id = R.string.install_force_backup),
                            summary = stringResource(id = R.string.install_force_backup_summary),
                            checked = uiState.forceBackup,
                            onCheckedChange = actions.onSelectForceBackup,
                        )
                    }
                    item(visible = uiState.advancedOptionsShown) {
                        SegmentedCheckboxItem(
                            title = stringResource(id = R.string.enable_adb),
                            summary = stringResource(id = R.string.enable_adb_summary),
                            checked = uiState.enableAdb,
                            onCheckedChange = actions.onSelectEnableAdb,
                        )
                    }
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = uiState.canInstall,
                onClick = actions.onNext
            ) { Text(stringResource(R.string.install_next)) }
        }
    }
}

@Composable
private fun PatchModeSelector(
    state: InstallUiState,
    actions: InstallScreenActions,
) {
    SegmentedColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
        item {
            SegmentedRadioItem(
                title = stringResource(R.string.install_patch_mode_normal),
                summary = stringResource(R.string.install_patch_mode_normal_summary),
                selected = state.patchMode == BootPatchMode.Normal,
                onClick = { actions.onSelectPatchMode(BootPatchMode.Normal) },
            )
        }
        item {
            SegmentedRadioItem(
                title = stringResource(R.string.install_patch_mode_hidden_path),
                summary = stringResource(R.string.install_patch_mode_hidden_path_summary),
                selected = state.patchMode == BootPatchMode.HiddenPath,
                onClick = { actions.onSelectPatchMode(BootPatchMode.HiddenPath) },
            )
        }
    }
}

@Composable
private fun TargetKmiItem(
    state: InstallUiState,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        leadingContent = { Icon(Icons.Filled.Edit, null) },
        headlineContent = { Text(stringResource(R.string.install_target_kmi)) },
        supportingContent = { Text(targetKmiSummary(state)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        onClick = onClick,
    )
}

@Composable
private fun targetKmiSummary(state: InstallUiState): String = when (state.targetKmiSource) {
    InstallKmiSource.Detecting -> stringResource(R.string.install_kmi_detecting)
    InstallKmiSource.Automatic -> stringResource(R.string.install_kmi_detected, state.targetKmi)
    InstallKmiSource.Manual -> stringResource(R.string.install_kmi_manual, state.targetKmi)
    InstallKmiSource.CurrentDevice -> stringResource(R.string.install_kmi_current, state.targetKmi)
    InstallKmiSource.Failed -> stringResource(R.string.install_kmi_detection_failed)
    InstallKmiSource.None -> stringResource(R.string.install_kmi_not_selected)
}

@Composable
private fun SelectInstallMethod(
    state: InstallUiState,
    onSelected: (InstallMethod) -> Unit,
    onDownloadFile: () -> Unit,
    onSelectBootImage: () -> Unit,
    onSelectAnyKernel: () -> Unit,
) {
    val confirmDialog = rememberConfirmDialog(
        onConfirm = {
            onSelected(InstallMethod.DirectInstallToInactiveSlot)
        },
        onDismiss = null
    )
    val dialogTitle = stringResource(android.R.string.dialog_alert_title)
    val dialogContent = stringResource(R.string.install_inactive_slot_warning)

    val onClick = { option: InstallMethod ->
        when (option) {
            is InstallMethod.SelectFile -> onSelectBootImage()
            is InstallMethod.DownloadFile -> onDownloadFile()
            is InstallMethod.DirectInstall -> onSelected(option)
            is InstallMethod.DirectInstallToInactiveSlot -> confirmDialog.showConfirm(dialogTitle, dialogContent)
            is InstallMethod.AnyKernel -> onSelectAnyKernel()
        }
    }

    key(state.installMethodOptions.size) {
        SegmentedColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            content = state.installMethodOptions.map { option ->
                {
                    SegmentedRadioItem(
                        title = stringResource(option.label),
                        summary = option.summary,
                        selected = option.javaClass == state.installMethod?.javaClass,
                        onClick = { onClick(option) }
                    )
                }
            }
        )
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    LargeFlexibleTopAppBar(
        title = { Text(stringResource(R.string.install)) },
        navigationIcon = {
            TopBarBackButton(onClick = onBack)
        },
        colors = expressiveTopAppBarColors(),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}
