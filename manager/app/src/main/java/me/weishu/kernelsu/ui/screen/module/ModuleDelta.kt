package me.weishu.kernelsu.ui.screen.module

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.data.model.ModuleUpdateInfo
import me.weishu.kernelsu.ui.component.ObserveAsEvents
import me.weishu.kernelsu.ui.component.delta.DeltaCard
import me.weishu.kernelsu.ui.component.delta.DeltaColors
import me.weishu.kernelsu.ui.component.delta.DeltaEmptyCard
import me.weishu.kernelsu.ui.component.delta.DeltaPillButton
import me.weishu.kernelsu.ui.component.delta.DeltaScreen
import me.weishu.kernelsu.ui.component.delta.DeltaSwitch
import me.weishu.kernelsu.ui.component.delta.deltaSp
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog

@Composable
fun ModulePagerDelta(
    uiState: ModuleUiState,
    confirmDialogState: ModuleConfirmDialogState?,
    moduleEvent: Flow<ModuleEffect>,
    actions: ModuleActions,
    bottomInnerPadding: Dp,
) {
    val selectZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        val data = activityResult.data ?: return@rememberLauncherForActivityResult
        val uris = mutableListOf<Uri>()
        val clipData = data.clipData
        if (clipData != null) {
            for (index in 0 until clipData.itemCount) {
                clipData.getItemAt(index)?.uri?.let { uris.add(it) }
            }
        } else {
            data.data?.let { uris.add(it) }
        }
        actions.onOpenFlash(uris)
    }
    val context = LocalContext.current
    val confirmDialog = rememberConfirmDialog(
        onConfirm = {
            when (val request = confirmDialogState?.request) {
                is ModuleConfirmRequest.Uninstall -> actions.onUninstallModule(request.module)
                is ModuleConfirmRequest.Update -> actions.onConfirmUpdate(request)
                null -> Unit
            }
        },
        onDismiss = actions.onDismissConfirmRequest,
    )

    fun openInstallPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        selectZipLauncher.launch(intent)
    }

    LaunchedEffect(confirmDialogState) {
        confirmDialogState?.let {
            confirmDialog.showConfirm(
                title = it.title,
                content = it.content,
                markdown = it.markdown,
                html = it.html,
                confirm = it.confirm,
                dismiss = it.dismiss,
            )
        }
    }

    ObserveAsEvents(moduleEvent) { event ->
        val message = when (event) {
            is ModuleEffect.SnackBar -> event.message
            is ModuleEffect.Toast -> event.message
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val searchText = uiState.searchStatus.searchText
    val modules = if (searchText.isBlank()) uiState.moduleList else uiState.searchResults

    DeltaScreen(
        title = stringResource(R.string.module),
        icon = Icons.Rounded.Extension,
        bottomInnerPadding = bottomInnerPadding,
        topActionIcon = Icons.Rounded.Backup,
        onTopActionClick = actions.onOpenWallpaperBackup,
        topActionContentDescription = stringResource(R.string.module_wallpaper_backup_open),
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 18.dp,
                    end = 20.dp,
                    bottom = contentPadding.calculateBottomPadding() + 70.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (uiState.loadError != null) {
                    item {
                        DeltaModuleLoadError(onRetry = actions.onRefresh)
                    }
                }
                if (modules.isEmpty() && uiState.loadError == null) {
                    item {
                        DeltaEmptyCard(
                            text = if (uiState.hasLoaded) {
                                stringResource(R.string.module_empty)
                            } else {
                                stringResource(R.string.refresh_refresh)
                            },
                        )
                    }
                    item {
                        DeltaPillButton(
                            text = stringResource(R.string.module_repos),
                            onClick = actions.onOpenRepo,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    items(modules, key = { it.id }) { module ->
                        DeltaModuleCard(
                            module = module,
                            updateInfo = uiState.updateInfo[module.id],
                            actions = actions,
                        )
                    }
                }
            }
            if (uiState.installButtonVisible) {
                DeltaInstallFab(
                    onClick = ::openInstallPicker,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 22.dp,
                            bottom = contentPadding.calculateBottomPadding() + 14.dp,
                        ),
                )
            }
        }
    }
}

@Composable
private fun DeltaModuleLoadError(onRetry: () -> Unit) {
    DeltaCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.module_failed_to_load),
                color = DeltaColors.Muted,
                fontSize = deltaSp(14f),
                fontWeight = FontWeight.Bold,
            )
            DeltaPillButton(
                text = stringResource(R.string.network_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DeltaModuleCard(
    module: Module,
    updateInfo: ModuleUpdateInfo?,
    actions: ModuleActions,
) {
    val pending = module.update || module.remove
    val textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None

    DeltaCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 112.dp)
                    .padding(start = 18.dp, top = 18.dp, end = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = module.name,
                            color = DeltaColors.Ink,
                            fontSize = deltaSp(19f, maxScale = 1.0f),
                            lineHeight = deltaSp(23f, maxScale = 1.0f),
                            fontWeight = FontWeight.Black,
                            textDecoration = textDecoration,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (module.metamodule) {
                            Text(
                                text = "META",
                                color = DeltaColors.Ink,
                                fontSize = deltaSp(10f, maxScale = 1.0f),
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(CircleShape)
                                    .background(DeltaColors.AccentSoft)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stringResource(R.string.module_version)} ${module.version}",
                        color = DeltaColors.Muted,
                        fontSize = deltaSp(14f, maxScale = 1.0f),
                        lineHeight = deltaSp(18f, maxScale = 1.0f),
                        fontWeight = FontWeight.Medium,
                        textDecoration = textDecoration,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${stringResource(R.string.module_author)} ${module.author}",
                        color = DeltaColors.Muted,
                        fontSize = deltaSp(14f, maxScale = 1.0f),
                        lineHeight = deltaSp(18f, maxScale = 1.0f),
                        fontWeight = FontWeight.Medium,
                        textDecoration = textDecoration,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (module.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = module.description,
                            color = DeltaColors.Muted,
                            fontSize = deltaSp(13f, maxScale = 1.0f),
                            lineHeight = deltaSp(17f, maxScale = 1.0f),
                            fontWeight = FontWeight.Medium,
                            textDecoration = textDecoration,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                DeltaSwitch(
                    checked = module.enabled && !module.remove,
                    enabled = !pending,
                    onCheckedChange = {
                        if (it != module.enabled) actions.onToggleModule(module)
                    },
                )
            }

            DeltaModuleActions(
                module = module,
                updateInfo = updateInfo,
                actions = actions,
            )
        }
    }
}

@Composable
private fun DeltaModuleActions(
    module: Module,
    updateInfo: ModuleUpdateInfo?,
    actions: ModuleActions,
) {
    val pending = module.update || module.remove
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (updateInfo != null && !module.remove) {
            DeltaPillButton(
                text = stringResource(R.string.module_update),
                onClick = { actions.onRequestUpdateConfirmation(module, updateInfo) },
                icon = Icons.Rounded.Add,
                modifier = Modifier.weight(1f),
            )
        }
        if (module.hasActionScript && module.enabled && !pending) {
            DeltaPillButton(
                text = stringResource(R.string.action),
                onClick = { actions.onExecuteModuleAction(module) },
                icon = Icons.Rounded.PlayArrow,
                modifier = Modifier.weight(1f),
            )
        }
        if (module.hasWebUi && module.enabled && !pending) {
            DeltaPillButton(
                text = "WebUI",
                onClick = { actions.onOpenWebUi(module) },
                icon = Icons.Rounded.Code,
                modifier = Modifier.weight(1f),
            )
        }
        DeltaPillButton(
            text = stringResource(if (module.remove) R.string.undo else R.string.uninstall),
            onClick = {
                if (module.remove) {
                    actions.onUndoUninstallModule(module)
                } else {
                    actions.onRequestUninstallConfirmation(module)
                }
            },
            icon = if (module.remove) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.Delete,
            background = if (module.remove) DeltaColors.AccentMuted else DeltaColors.Danger.copy(alpha = 0.18f),
            foreground = if (module.remove) DeltaColors.Ink else DeltaColors.Danger,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DeltaInstallFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(DeltaColors.AccentSoft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.module_install),
            tint = DeltaColors.Ink,
            modifier = Modifier.size(34.dp),
        )
    }
}
