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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.data.model.ModuleUpdateInfo
import me.weishu.kernelsu.ui.component.ObserveAsEvents
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproButton
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproEmptyText
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproScreen
import me.weishu.kernelsu.ui.component.skrootpro.skrootproSp
import me.weishu.kernelsu.ui.screen.settings.SettingsWallpaperCropDialog

@Composable
fun ModulePagerSkrootpro(
    uiState: ModuleUiState,
    confirmDialogState: ModuleConfirmDialogState?,
    moduleEvent: Flow<ModuleEffect>,
    actions: ModuleActions,
    bottomInnerPadding: Dp,
) {
    val context = LocalContext.current
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
            is ModuleEffect.Toast -> event.message
            is ModuleEffect.SnackBar -> event.message
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val modules = if (uiState.searchStatus.isExpand()) uiState.searchResults else uiState.moduleList
    SkrootproScreen(
        title = stringResource(R.string.skrootpro_title),
        showAdd = uiState.installButtonVisible,
        onAddClick = {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            selectZipLauncher.launch(intent)
        },
        secondaryActionIcon = Icons.Rounded.Backup,
        onSecondaryActionClick = actions.onOpenWallpaperBackup,
        secondaryActionContentDescription = stringResource(R.string.module_wallpaper_backup_open),
        bottomInnerPadding = bottomInnerPadding,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            SkrootproSegmentedTabs(
                onMarketClick = actions.onOpenRepo,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
            )

            if (uiState.loadError != null) {
                SkrootproModuleLoadError(
                    onRetry = actions.onRefresh,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }

            if (modules.isEmpty() && uiState.loadError == null) {
                SkrootproEmptyText(
                    text = if (uiState.hasLoaded) {
                        stringResource(R.string.skrootpro_module_empty)
                    } else {
                        stringResource(R.string.refresh_refresh)
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(modules, key = { it.id }) { module ->
                        SkrootproModuleCard(
                            module = module,
                            updateInfo = uiState.updateInfo[module.id],
                            onUpdateClick = { updateInfo ->
                                actions.onRequestUpdateConfirmation(module, updateInfo)
                            },
                            onExecuteClick = { actions.onExecuteModuleAction(module) },
                            onDeleteClick = { actions.onRequestUninstallConfirmation(module) },
                            onUndoUninstallClick = { actions.onUndoUninstallModule(module) },
                            onToggleClick = { actions.onToggleModule(module) },
                            onWebManageClick = { actions.onOpenWebUi(module) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkrootproModuleLoadError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(SkrootproColors.Surface)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.module_failed_to_load),
            color = SkrootproColors.Muted,
            fontSize = skrootproSp(13f, maxScale = 1.0f),
            fontWeight = FontWeight.Medium,
        )
        SkrootproButton(
            text = stringResource(R.string.network_retry),
            onClick = onRetry,
            modifier = Modifier
                .padding(top = 10.dp)
                .width(120.dp),
        )
    }
}

@Composable
private fun SkrootproSegmentedTabs(
    onMarketClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(SkrootproColors.BarSurface, CircleShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkrootproSegment(
            text = stringResource(R.string.skrootpro_module_installed),
            selected = true,
            onClick = {},
            modifier = Modifier.weight(1f),
        )
        SkrootproSegment(
            text = stringResource(R.string.skrootpro_module_market),
            selected = false,
            onClick = onMarketClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SkrootproSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = if (selected) SkrootproColors.Purple else SkrootproColors.BarSurface,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) androidx.compose.ui.graphics.Color.White else SkrootproColors.Muted,
            fontSize = skrootproSp(15f, maxScale = 1.0f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun SkrootproModuleCard(
    module: Module,
    updateInfo: ModuleUpdateInfo?,
    onUpdateClick: (ModuleUpdateInfo) -> Unit,
    onExecuteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUndoUninstallClick: () -> Unit,
    onToggleClick: () -> Unit,
    onWebManageClick: () -> Unit,
) {
    val pending = module.update || module.remove
    var menuExpanded by remember(module.id) { mutableStateOf(false) }
    var showWallpaperCrop by remember(module.id) { mutableStateOf(false) }
    var showWallpaperPreview by remember(module.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val wallpaperState = rememberModuleCardWallpaperState(
        moduleId = module.id,
        onWallpaperSelected = { showWallpaperCrop = true },
    )
    val wallpaperEntry = rememberModuleCardWallpaperFrame(
        state = wallpaperState,
        paused = showWallpaperCrop || showWallpaperPreview,
    )
    val wallpaperBitmap = rememberModuleCardWallpaperBitmap(wallpaperEntry)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(SkrootproColors.Surface),
    ) {
        ModuleCardWallpaperBackground(
            bitmap = wallpaperBitmap,
            overlayColor = SkrootproColors.Surface.copy(alpha = 0.70f),
        )
        Box(
            modifier = Modifier.padding(start = 20.dp, top = 9.dp, end = 14.dp, bottom = 9.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = module.name,
                    color = SkrootproColors.Text,
                    fontSize = skrootproSp(15.5f, maxScale = 1.0f),
                    lineHeight = skrootproSp(19f, maxScale = 1.0f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = module.description,
                    color = SkrootproColors.Muted,
                    fontSize = skrootproSp(11f, maxScale = 1.0f),
                    lineHeight = skrootproSp(14f, maxScale = 1.0f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp, end = 70.dp),
                )
                SkrootproModuleMetaLine(
                    label = stringResource(R.string.module_version),
                    value = module.version,
                    modifier = Modifier.padding(top = 3.dp),
                )
                SkrootproModuleMetaLine(
                    label = stringResource(R.string.module_author),
                    value = module.author,
                )
                SkrootproModuleMetaLine(
                    label = stringResource(R.string.skrootpro_status),
                    value = if (module.enabled) {
                        stringResource(R.string.skrootpro_running)
                    } else {
                        stringResource(R.string.skrootpro_stopped)
                    },
                    valueColor = if (module.enabled) SkrootproColors.Success else SkrootproColors.Muted,
                )
                if (module.hasWebUi) {
                    SkrootproButton(
                        text = stringResource(R.string.skrootpro_web_manage),
                        onClick = onWebManageClick,
                        enabled = !pending,
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .width(82.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(58.dp),
            ) {
                SkrootproButton(
                    text = stringResource(R.string.skrootpro_more),
                    onClick = { menuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    if (updateInfo != null && !module.remove) {
                        DropdownMenuItem(
                            text = { SkrootproMenuText(stringResource(R.string.module_update)) },
                            onClick = {
                                menuExpanded = false
                                onUpdateClick(updateInfo)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { SkrootproMenuText(stringResource(R.string.skrootpro_module_action_execute)) },
                        enabled = module.hasActionScript && !pending,
                        onClick = {
                            menuExpanded = false
                            onExecuteClick()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            SkrootproMenuText(
                                if (module.enabled) {
                                    stringResource(R.string.skrootpro_module_action_disable)
                                } else {
                                    stringResource(R.string.skrootpro_module_action_enable)
                                }
                            )
                        },
                        enabled = !pending,
                        onClick = {
                            menuExpanded = false
                            onToggleClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { SkrootproMenuText(stringResource(R.string.module_wallpaper_pick)) },
                        onClick = {
                            menuExpanded = false
                            wallpaperState.onPickWallpaper()
                        },
                    )
                    if (wallpaperState.hasSelectedWallpaper) {
                        DropdownMenuItem(
                            text = { SkrootproMenuText(stringResource(R.string.module_wallpaper_crop)) },
                            onClick = {
                                menuExpanded = false
                                showWallpaperCrop = true
                            },
                        )
                        DropdownMenuItem(
                            text = { SkrootproMenuText(stringResource(R.string.module_wallpaper_preview)) },
                            onClick = {
                                menuExpanded = false
                                showWallpaperPreview = true
                            },
                        )
                        if (wallpaperState.canPlayCarousel) {
                            DropdownMenuItem(
                                text = {
                                    SkrootproMenuText(
                                        stringResource(
                                            if (wallpaperState.carouselEnabled) {
                                                R.string.module_wallpaper_carousel_disable
                                            } else {
                                                R.string.module_wallpaper_carousel_enable
                                            }
                                        )
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    wallpaperState.onToggleCarousel()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { SkrootproMenuText(stringResource(R.string.module_wallpaper_sync_theme_store)) },
                            onClick = {
                                menuExpanded = false
                                val message = if (wallpaperState.onSyncThemeStore()) {
                                    R.string.module_wallpaper_sync_theme_store_success
                                } else {
                                    R.string.module_wallpaper_sync_theme_store_failed
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                        )
                        DropdownMenuItem(
                            text = { SkrootproMenuText(stringResource(R.string.module_wallpaper_clear)) },
                            onClick = {
                                menuExpanded = false
                                wallpaperState.onClearWallpaper()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            SkrootproMenuText(
                                text = stringResource(
                                    if (module.remove) R.string.undo else R.string.skrootpro_module_action_delete
                                ),
                                color = if (module.remove) SkrootproColors.Text else Color(0xFFD32F2F),
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            if (module.remove) onUndoUninstallClick() else onDeleteClick()
                        },
                    )
                }
            }
        }
    }

    SettingsWallpaperCropDialog(
        show = showWallpaperCrop && wallpaperEntry != null,
        uriString = wallpaperEntry?.uriString,
        crop = wallpaperEntry?.crop ?: wallpaperState.crop,
        onCropChange = wallpaperState.onCropChange,
        onDismissRequest = { showWallpaperCrop = false },
        title = stringResource(R.string.module_wallpaper_crop),
        editorAspectRatio = MODULE_CARD_WALLPAPER_ASPECT_RATIO,
        cropAspectRatio = MODULE_CARD_WALLPAPER_ASPECT_RATIO,
    )
    ModuleCardWallpaperPreviewDialog(
        show = showWallpaperPreview,
        moduleName = module.name,
        uriString = wallpaperEntry?.uriString,
        bitmap = wallpaperBitmap,
        onDismissRequest = { showWallpaperPreview = false },
    )
}

@Composable
private fun SkrootproMenuText(
    text: String,
    color: Color = SkrootproColors.Text,
) {
    Text(
        text = text,
        color = color,
        fontSize = skrootproSp(12f, maxScale = 1.0f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SkrootproModuleMetaLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = SkrootproColors.Muted,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            color = SkrootproColors.Muted,
            fontSize = skrootproSp(10.5f, maxScale = 1.0f),
            lineHeight = skrootproSp(13f, maxScale = 1.0f),
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = skrootproSp(10.5f, maxScale = 1.0f),
            lineHeight = skrootproSp(13f, maxScale = 1.0f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
