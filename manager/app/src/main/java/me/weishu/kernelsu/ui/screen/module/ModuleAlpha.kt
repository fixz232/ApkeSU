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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import me.weishu.kernelsu.ui.component.alpha.AlphaButton
import me.weishu.kernelsu.ui.component.alpha.AlphaCard
import me.weishu.kernelsu.ui.component.alpha.AlphaColors
import me.weishu.kernelsu.ui.component.alpha.AlphaOutlinedButton
import me.weishu.kernelsu.ui.component.alpha.AlphaShapes
import me.weishu.kernelsu.ui.component.alpha.AlphaScreen
import me.weishu.kernelsu.ui.component.alpha.AlphaSwitch
import me.weishu.kernelsu.ui.component.alpha.alphaStrongWeight
import me.weishu.kernelsu.ui.component.alpha.alphaSp
import me.weishu.kernelsu.ui.component.alpha.isStudioStyle
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog

@Composable
fun ModulePagerAlpha(
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
    val context = androidx.compose.ui.platform.LocalContext.current
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
    val studioStyle = isStudioStyle()

    AlphaScreen(
        title = stringResource(R.string.module),
        bottomInnerPadding = bottomInnerPadding,
        topActionIcon = if (uiState.installButtonVisible) Icons.Rounded.Add else null,
        onTopActionClick = {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            selectZipLauncher.launch(intent)
        },
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 18.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (studioStyle) {
                item {
                    StudioModuleOverview(
                        modules = uiState.moduleList,
                        updateCount = uiState.updateInfo.size,
                    )
                }
            }
            item {
                AlphaModuleSearch(
                    searchText = searchText,
                    onSearchTextChange = actions.onSearchTextChange,
                    onClearSearch = actions.onClearSearch,
                )
            }
            item {
                AlphaOutlinedButton(
                    text = stringResource(R.string.module_repos),
                    onClick = actions.onOpenRepo,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (uiState.loadError != null) {
                item {
                    AlphaModuleLoadError(onRetry = actions.onRefresh)
                }
            }
            if (modules.isEmpty() && uiState.loadError == null) {
                item {
                    AlphaCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (uiState.hasLoaded) {
                                    stringResource(R.string.module_empty)
                                } else {
                                    stringResource(R.string.refresh_refresh)
                                },
                                color = AlphaColors.Muted,
                                fontSize = alphaSp(14f),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else {
                items(modules, key = { it.id }) { module ->
                    AlphaModuleCard(
                        module = module,
                        updateInfo = uiState.updateInfo[module.id],
                        actions = actions,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphaModuleLoadError(onRetry: () -> Unit) {
    AlphaCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.module_failed_to_load),
                color = AlphaColors.Muted,
                fontSize = alphaSp(14f),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AlphaOutlinedButton(
                text = stringResource(R.string.network_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ModulePagerStudio(
    uiState: ModuleUiState,
    confirmDialogState: ModuleConfirmDialogState?,
    moduleEvent: Flow<ModuleEffect>,
    actions: ModuleActions,
    bottomInnerPadding: Dp,
) {
    ModulePagerAlpha(
        uiState = uiState,
        confirmDialogState = confirmDialogState,
        moduleEvent = moduleEvent,
        actions = actions,
        bottomInnerPadding = bottomInnerPadding,
    )
}

@Composable
private fun StudioModuleOverview(
    modules: List<Module>,
    updateCount: Int,
) {
    AlphaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StudioModuleMetric(
                value = modules.size,
                label = stringResource(R.string.module),
                modifier = Modifier.weight(1f),
            )
            StudioModuleMetric(
                value = modules.count { it.enabled && !it.remove },
                label = stringResource(R.string.skrootpro_running),
                modifier = Modifier.weight(1f),
            )
            StudioModuleMetric(
                value = updateCount,
                label = stringResource(R.string.module_update),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StudioModuleMetric(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value.toString(),
            color = AlphaColors.Accent,
            fontSize = alphaSp(18f, maxScale = 1.0f),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            color = AlphaColors.Muted,
            fontSize = alphaSp(11f, maxScale = 1.0f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AlphaModuleSearch(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(AlphaShapes.Control)
            .background(AlphaColors.Surface)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = AlphaColors.Muted,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            singleLine = true,
            cursorBrush = SolidColor(AlphaColors.Accent),
            textStyle = TextStyle(
                color = AlphaColors.Text,
                fontSize = alphaSp(15f),
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (searchText.isBlank()) {
                        Text(
                            text = stringResource(R.string.superuser_search_hint),
                            color = AlphaColors.Muted,
                            fontSize = alphaSp(14f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (searchText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(AlphaShapes.Button)
                    .background(AlphaColors.SurfaceStrong)
                    .clickable(onClick = onClearSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = AlphaColors.Muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AlphaModuleCard(
    module: Module,
    updateInfo: ModuleUpdateInfo?,
    actions: ModuleActions,
) {
    val pending = module.update || module.remove
    val textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None

    AlphaCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 14.dp, end = 10.dp, bottom = 10.dp),
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
                            color = AlphaColors.Text,
                            fontSize = alphaSp(17.5f),
                            lineHeight = alphaSp(21f),
                            fontWeight = alphaStrongWeight(),
                            textDecoration = textDecoration,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (module.metamodule) {
                            Text(
                                text = "META",
                                color = AlphaColors.Accent,
                                fontSize = alphaSp(9.5f, maxScale = 1.0f),
                                fontWeight = alphaStrongWeight(),
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clip(AlphaShapes.Control)
                                    .background(AlphaColors.AccentSoft)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${stringResource(R.string.module_version)} ${module.version}, ${stringResource(R.string.module_author)} ${module.author}",
                        color = AlphaColors.Muted,
                        fontSize = alphaSp(13.5f),
                        fontWeight = FontWeight.Bold,
                        textDecoration = textDecoration,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (module.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = module.description,
                            color = AlphaColors.Muted,
                            fontSize = alphaSp(14f),
                            lineHeight = alphaSp(18f),
                            fontWeight = FontWeight.Medium,
                            textDecoration = textDecoration,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                AlphaSwitch(
                    checked = module.enabled && !module.remove,
                    enabled = !pending,
                    onCheckedChange = {
                        if (it != module.enabled) actions.onToggleModule(module)
                    },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AlphaColors.Divider)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (updateInfo != null && !module.remove) {
                    AlphaButton(
                        text = stringResource(R.string.module_update),
                        onClick = { actions.onRequestUpdateConfirmation(module, updateInfo) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (module.hasActionScript && module.enabled && !pending) {
                    AlphaOutlinedButton(
                        text = stringResource(R.string.action),
                        icon = Icons.Rounded.PlayArrow,
                        onClick = { actions.onExecuteModuleAction(module) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (module.hasWebUi && module.enabled && !pending) {
                    AlphaOutlinedButton(
                        text = stringResource(R.string.open),
                        icon = Icons.Rounded.Code,
                        onClick = { actions.onOpenWebUi(module) },
                        modifier = Modifier.weight(1f),
                    )
                }
                AlphaOutlinedButton(
                    text = stringResource(if (module.remove) R.string.undo else R.string.uninstall),
                    icon = if (module.remove) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.Delete,
                    onClick = {
                        if (module.remove) {
                            actions.onUndoUninstallModule(module)
                        } else {
                            actions.onRequestUninstallConfirmation(module)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
