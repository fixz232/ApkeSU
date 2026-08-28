package me.weishu.kernelsu.ui.screen.module

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.Module
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.viewmodel.ModuleViewModel
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private data class PendingModuleWallpaperImport(
    val uri: Uri,
    val preview: ModuleWallpaperBackupPreview,
)

private data class PendingModuleWallpaperBundleImport(
    val uri: Uri,
    val preview: ModuleWallpaperBundlePreview,
)

@Composable
fun ModuleWallpaperBackupScreen() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val moduleViewModel = viewModel<ModuleViewModel>()
    val uiState by moduleViewModel.uiState.collectAsStateWithLifecycle()
    var selectedModuleId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExportModule by remember { mutableStateOf<Module?>(null) }
    var pendingImport by remember { mutableStateOf<PendingModuleWallpaperImport?>(null) }
    var pendingBundleImport by remember { mutableStateOf<PendingModuleWallpaperBundleImport?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showModulePicker by rememberSaveable { mutableStateOf(false) }
    var showRestoreConfirmation by rememberSaveable { mutableStateOf(false) }
    var restoreMode by rememberSaveable { mutableStateOf(ModuleWallpaperRestoreMode.Replace) }
    var showBundleRestoreConfirmation by rememberSaveable { mutableStateOf(false) }
    var bundleRestoreMode by rememberSaveable { mutableStateOf(ModuleWallpaperRestoreMode.Replace) }
    var showSlotOverwritePicker by rememberSaveable { mutableStateOf(false) }
    var pendingOverwriteSlotIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingDeleteSlotIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var appliedSlotIndex by rememberSaveable(selectedModuleId) { mutableIntStateOf(-1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var configurationRevision by remember { mutableIntStateOf(0) }

    val modules = uiState.moduleList
    val selectedModule = modules.firstOrNull { it.id == selectedModuleId }
    val selectedSnapshot = remember(selectedModuleId, configurationRevision) {
        selectedModuleId?.let { readModuleCardWallpaperSnapshot(context, it) }
    }
    val savedSlots = remember(selectedModuleId, configurationRevision) {
        selectedModuleId?.let { readModuleWallpaperSavedSlots(context, it) }
            ?: List(MODULE_WALLPAPER_SAVED_SLOT_COUNT) { null }
    }
    val selectedBackupImageCount = (selectedSnapshot?.allEntries()?.size ?: 0) +
        savedSlots.filterNotNull().sumOf { it.snapshot.allEntries().size }
    val selectedHasBackupData = selectedBackupImageCount > 0
    val configuredModuleCount = remember(modules, configurationRevision) {
        modules.count { module ->
            readModuleCardWallpaperSnapshot(context, module.id).allEntries().isNotEmpty() ||
                readModuleWallpaperSavedSlots(context, module.id).any { it != null }
        }
    }

    LaunchedEffect(Unit) {
        moduleViewModel.initializePreferences()
        moduleViewModel.fetchModuleList(checkUpdate = false)
    }
    LaunchedEffect(modules, pendingImport?.preview?.sourceModuleId) {
        if (modules.isEmpty()) return@LaunchedEffect
        val importedModuleId = pendingImport?.preview?.sourceModuleId
        selectedModuleId = when {
            importedModuleId != null && modules.any { it.id == importedModuleId } -> importedModuleId
            selectedModuleId != null && modules.any { it.id == selectedModuleId } -> selectedModuleId
            else -> modules.first().id
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(MODULE_WALLPAPER_BACKUP_MIME_TYPE),
    ) { uri ->
        val module = pendingExportModule
        pendingExportModule = null
        if (uri == null || module == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    exportModuleWallpaperBackup(
                        context = context,
                        destination = uri,
                        moduleId = module.id,
                        moduleName = module.name,
                    )
                }
                if (result.success) {
                    Toast.makeText(
                        context,
                        R.string.module_wallpaper_backup_export_success,
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    errorMessage = result.error.toDisplayMessage(
                        resources.getString(R.string.module_wallpaper_backup_export_failed)
                    )
                }
            } finally {
                busy = false
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    previewModuleWallpaperBackup(context, uri)
                }
                val preview = result.preview
                if (result.success && preview != null) {
                    pendingBundleImport = null
                    pendingImport = PendingModuleWallpaperImport(uri, preview)
                } else {
                    errorMessage = result.error.toDisplayMessage(
                        resources.getString(R.string.module_wallpaper_backup_import_failed)
                    )
                }
            } finally {
                busy = false
            }
        }
    }
    val bundleExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(MODULE_WALLPAPER_BUNDLE_MIME_TYPE),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    exportAllModuleWallpaperBackup(context, uri, modules)
                }
                if (result.success) {
                    Toast.makeText(
                        context,
                        resources.getString(
                            R.string.module_wallpaper_bundle_export_success,
                            result.preview?.moduleCount ?: 0,
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    errorMessage = result.error.toDisplayMessage(
                        resources.getString(R.string.module_wallpaper_bundle_export_failed)
                    )
                }
            } finally {
                busy = false
            }
        }
    }
    val bundleImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    previewAllModuleWallpaperBackup(context, uri)
                }
                val preview = result.preview
                if (result.success && preview != null) {
                    pendingImport = null
                    pendingBundleImport = PendingModuleWallpaperBundleImport(uri, preview)
                } else {
                    errorMessage = result.error.toDisplayMessage(
                        resources.getString(R.string.module_wallpaper_bundle_import_failed)
                    )
                }
            } finally {
                busy = false
            }
        }
    }

    fun exportSelectedModule() {
        val module = selectedModule ?: return
        if (!selectedHasBackupData) return
        pendingExportModule = module
        exportLauncher.launch(
            "${module.id.safeBackupFilePart()}-wallpaper.$MODULE_WALLPAPER_BACKUP_EXTENSION"
        )
    }

    fun restorePendingBundleImport() {
        val pending = pendingBundleImport ?: return
        showBundleRestoreConfirmation = false
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    restoreAllModuleWallpaperBackup(
                        context = context,
                        source = pending.uri,
                        installedModules = modules,
                        mode = bundleRestoreMode,
                    )
                }
                if (result.success) {
                    pendingBundleImport = null
                    configurationRevision++
                    appliedSlotIndex = -1
                    Toast.makeText(
                        context,
                        resources.getString(
                            R.string.module_wallpaper_bundle_restore_success,
                            result.restoredModules,
                            result.skippedModules,
                        ),
                        Toast.LENGTH_LONG,
                    ).show()
                } else {
                    errorMessage = result.error.toDisplayMessage(
                        resources.getString(R.string.module_wallpaper_bundle_restore_failed)
                    )
                }
            } finally {
                busy = false
            }
        }
    }

    fun restorePendingImport() {
        val pending = pendingImport ?: return
        val targetModuleId = selectedModuleId ?: return
        showRestoreConfirmation = false
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    restoreModuleWallpaperBackup(
                        context = context,
                        source = pending.uri,
                        targetModuleId = targetModuleId,
                        mode = restoreMode,
                    )
                }
                if (result.success) {
                    pendingImport = null
                    configurationRevision++
                    appliedSlotIndex = -1
                    Toast.makeText(
                        context,
                        R.string.module_wallpaper_backup_restore_success,
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    errorMessage = result.error.toDisplayMessage(
                        resources.getString(R.string.module_wallpaper_backup_restore_failed)
                    )
                }
            } finally {
                busy = false
            }
        }
    }

    fun saveCurrentToSlot(slotIndex: Int) {
        val moduleId = selectedModuleId ?: return
        pendingOverwriteSlotIndex = null
        showSlotOverwritePicker = false
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    saveCurrentModuleWallpaperToSlot(context, moduleId, slotIndex)
                }
                if (result.success) {
                    configurationRevision++
                    Toast.makeText(
                        context,
                        resources.getString(R.string.module_wallpaper_slot_save_success, slotIndex + 1),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    errorMessage = result.error.toDisplayMessage(
                        resources.getString(R.string.module_wallpaper_slot_save_failed)
                    )
                }
            } finally {
                busy = false
            }
        }
    }

    fun saveCurrentToNextSlot() {
        val emptyIndex = savedSlots.indexOfFirst { it == null }
        if (emptyIndex >= 0) {
            saveCurrentToSlot(emptyIndex)
        } else {
            showSlotOverwritePicker = true
        }
    }

    fun applySavedSlot(slotIndex: Int) {
        val moduleId = selectedModuleId ?: return
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    applyModuleWallpaperSavedSlot(context, moduleId, slotIndex)
                }
                if (result.success) {
                    configurationRevision++
                    appliedSlotIndex = slotIndex
                    Toast.makeText(
                        context,
                        resources.getString(R.string.module_wallpaper_slot_apply_success, slotIndex + 1),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    errorMessage = result.error.toDisplayMessage(
                        resources.getString(R.string.module_wallpaper_slot_apply_failed)
                    )
                }
            } finally {
                busy = false
            }
        }
    }

    fun deleteSavedSlot(slotIndex: Int) {
        val moduleId = selectedModuleId ?: return
        pendingDeleteSlotIndex = null
        busy = true
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    deleteModuleWallpaperSavedSlot(context, moduleId, slotIndex)
                }
                if (result.success) {
                    configurationRevision++
                    if (appliedSlotIndex == slotIndex) appliedSlotIndex = -1
                    Toast.makeText(
                        context,
                        resources.getString(R.string.module_wallpaper_slot_delete_success, slotIndex + 1),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    errorMessage = result.error.toDisplayMessage(
                        resources.getString(R.string.module_wallpaper_slot_delete_failed)
                    )
                }
            } finally {
                busy = false
            }
        }
    }

    MiuixScaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.module_wallpaper_backup_title),
                color = androidx.compose.ui.graphics.Color.Transparent,
                titleColor = colorScheme.onSurface,
                navigationIcon = {
                    MiuixIconButton(onClick = dropUnlessResumed { navigator.pop() }) {
                        MiuixIcon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ModuleWallpaperBundleCard(
                    configuredModuleCount = configuredModuleCount,
                    busy = busy,
                    modulesLoaded = modules.isNotEmpty(),
                    onExport = {
                        bundleExportLauncher.launch(
                            "all-module-wallpapers.$MODULE_WALLPAPER_BUNDLE_EXTENSION"
                        )
                    },
                    onImport = {
                        bundleImportLauncher.launch(
                            arrayOf(MODULE_WALLPAPER_BUNDLE_MIME_TYPE, "application/octet-stream", "*/*")
                        )
                    },
                )
            }
            item {
                ModuleWallpaperBackupHeader(
                    configured = selectedHasBackupData,
                    imageCount = selectedBackupImageCount,
                )
            }
            item {
                ModuleWallpaperTargetCard(
                    module = selectedModule,
                    snapshot = selectedSnapshot,
                    loading = uiState.isRefreshing && modules.isEmpty(),
                    enabled = !busy,
                    loadError = uiState.loadError,
                    onChoose = { showModulePicker = true },
                    onRetry = { moduleViewModel.fetchModuleList(checkUpdate = false) },
                )
            }
            item {
                ModuleWallpaperSavedSlotsSection(
                    slots = savedSlots,
                    appliedSlotIndex = appliedSlotIndex,
                    canSave = selectedModule != null && !selectedSnapshot?.allEntries().isNullOrEmpty(),
                    busy = busy,
                    onSaveNext = ::saveCurrentToNextSlot,
                    onSaveToSlot = { slotIndex ->
                        if (savedSlots[slotIndex] == null) {
                            saveCurrentToSlot(slotIndex)
                        } else {
                            pendingOverwriteSlotIndex = slotIndex
                        }
                    },
                    onApply = ::applySavedSlot,
                    onDelete = { pendingDeleteSlotIndex = it },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !busy && selectedModule != null && selectedHasBackupData,
                        onClick = ::exportSelectedModule,
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(7.dp))
                        Text(stringResource(R.string.module_wallpaper_backup_export))
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        onClick = {
                            importLauncher.launch(
                                arrayOf(MODULE_WALLPAPER_BACKUP_MIME_TYPE, "application/octet-stream", "*/*")
                            )
                        },
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(7.dp))
                        Text(stringResource(R.string.module_wallpaper_backup_import))
                    }
                }
            }
            if (busy) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = stringResource(R.string.module_wallpaper_backup_processing),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            pendingImport?.let { pending ->
                item {
                    ModuleWallpaperImportPreviewCard(
                        preview = pending.preview,
                        targetModule = selectedModule,
                        busy = busy,
                        restoreEnabled = !busy && selectedModule != null,
                        onChooseTarget = { showModulePicker = true },
                        onRestore = { showRestoreConfirmation = true },
                        onDiscard = { pendingImport = null },
                    )
                }
            }
            pendingBundleImport?.let { pending ->
                item {
                    ModuleWallpaperBundleImportPreviewCard(
                        preview = pending.preview,
                        busy = busy,
                        onRestore = { showBundleRestoreConfirmation = true },
                        onDiscard = { pendingBundleImport = null },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showModulePicker) {
        ModuleWallpaperModulePicker(
            modules = modules,
            selectedModuleId = selectedModuleId,
            onSelect = {
                selectedModuleId = it
                appliedSlotIndex = -1
                showModulePicker = false
            },
            onDismiss = { showModulePicker = false },
        )
    }
    if (showSlotOverwritePicker) {
        AlertDialog(
            onDismissRequest = { showSlotOverwritePicker = false },
            icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
            title = { Text(stringResource(R.string.module_wallpaper_slot_choose_overwrite)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    savedSlots.forEachIndexed { index, slot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSlotOverwritePicker = false
                                    pendingOverwriteSlotIndex = index
                                }
                                .padding(horizontal = 4.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.module_wallpaper_slot_label, index + 1),
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = stringResource(
                                    R.string.module_wallpaper_slot_image_count,
                                    slot?.snapshot?.allEntries()?.size ?: 0,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSlotOverwritePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    pendingOverwriteSlotIndex?.let { slotIndex ->
        AlertDialog(
            onDismissRequest = { pendingOverwriteSlotIndex = null },
            icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
            title = { Text(stringResource(R.string.module_wallpaper_slot_overwrite_title)) },
            text = {
                Text(stringResource(R.string.module_wallpaper_slot_overwrite_message, slotIndex + 1))
            },
            confirmButton = {
                TextButton(onClick = { saveCurrentToSlot(slotIndex) }) {
                    Text(stringResource(R.string.module_wallpaper_slot_overwrite))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverwriteSlotIndex = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    pendingDeleteSlotIndex?.let { slotIndex ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSlotIndex = null },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.module_wallpaper_slot_delete_title)) },
            text = {
                Text(stringResource(R.string.module_wallpaper_slot_delete_message, slotIndex + 1))
            },
            confirmButton = {
                TextButton(onClick = { deleteSavedSlot(slotIndex) }) {
                    Text(stringResource(R.string.module_wallpaper_slot_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSlotIndex = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    if (showRestoreConfirmation) {
        val preview = pendingImport?.preview
        val target = selectedModule
        if (preview != null && target != null) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirmation = false },
                icon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                title = { Text(stringResource(R.string.module_wallpaper_backup_restore_confirm_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(
                                R.string.module_wallpaper_backup_restore_confirm_message,
                                target.name,
                                preview.imageCount,
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = restoreMode == ModuleWallpaperRestoreMode.Merge,
                                onClick = { restoreMode = ModuleWallpaperRestoreMode.Merge },
                                label = { Text(stringResource(R.string.module_wallpaper_restore_merge)) },
                            )
                            FilterChip(
                                selected = restoreMode == ModuleWallpaperRestoreMode.Replace,
                                onClick = { restoreMode = ModuleWallpaperRestoreMode.Replace },
                                label = { Text(stringResource(R.string.module_wallpaper_restore_replace)) },
                            )
                        }
                        Text(
                            text = stringResource(
                                if (restoreMode == ModuleWallpaperRestoreMode.Merge) {
                                    R.string.module_wallpaper_restore_merge_summary
                                } else {
                                    R.string.module_wallpaper_restore_replace_summary
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = ::restorePendingImport) {
                        Text(stringResource(R.string.module_wallpaper_backup_restore))
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
    if (showBundleRestoreConfirmation) {
        val preview = pendingBundleImport?.preview
        if (preview != null) {
            AlertDialog(
                onDismissRequest = { showBundleRestoreConfirmation = false },
                icon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                title = { Text(stringResource(R.string.module_wallpaper_bundle_restore_confirm_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(
                                R.string.module_wallpaper_bundle_restore_confirm_message,
                                preview.moduleCount,
                                preview.imageCount,
                            )
                        )
                        RestoreModeSelector(
                            mode = bundleRestoreMode,
                            onModeChange = { bundleRestoreMode = it },
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = ::restorePendingBundleImport) {
                        Text(stringResource(R.string.module_wallpaper_bundle_restore))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBundleRestoreConfirmation = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.module_wallpaper_backup_error_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

@Composable
private fun RestoreModeSelector(
    mode: ModuleWallpaperRestoreMode,
    onModeChange: (ModuleWallpaperRestoreMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == ModuleWallpaperRestoreMode.Merge,
                onClick = { onModeChange(ModuleWallpaperRestoreMode.Merge) },
                label = { Text(stringResource(R.string.module_wallpaper_restore_merge)) },
            )
            FilterChip(
                selected = mode == ModuleWallpaperRestoreMode.Replace,
                onClick = { onModeChange(ModuleWallpaperRestoreMode.Replace) },
                label = { Text(stringResource(R.string.module_wallpaper_restore_replace)) },
            )
        }
        Text(
            text = stringResource(
                if (mode == ModuleWallpaperRestoreMode.Merge) {
                    R.string.module_wallpaper_restore_merge_summary
                } else {
                    R.string.module_wallpaper_restore_replace_summary
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModuleWallpaperBundleCard(
    configuredModuleCount: Int,
    busy: Boolean,
    modulesLoaded: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.module_wallpaper_bundle_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    R.string.module_wallpaper_bundle_summary,
                    configuredModuleCount,
                    MODULE_WALLPAPER_SAVED_SLOT_COUNT,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !busy && configuredModuleCount > 0,
                    onClick = onExport,
                ) {
                    Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(7.dp))
                    Text(stringResource(R.string.module_wallpaper_bundle_export))
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !busy && modulesLoaded,
                    onClick = onImport,
                ) {
                    Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(7.dp))
                    Text(stringResource(R.string.module_wallpaper_bundle_import))
                }
            }
        }
    }
}

@Composable
private fun ModuleWallpaperBundleImportPreviewCard(
    preview: ModuleWallpaperBundlePreview,
    busy: Boolean,
    onRestore: () -> Unit,
    onDiscard: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.module_wallpaper_bundle_preview_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BackupPreviewLine(
                label = stringResource(R.string.module_wallpaper_bundle_modules),
                value = preview.moduleCount.toString(),
            )
            BackupPreviewLine(
                label = stringResource(R.string.module_wallpaper_backup_contents),
                value = stringResource(
                    R.string.module_wallpaper_backup_contents_value,
                    preview.imageCount,
                    formatBackupSize(preview.totalBytes),
                ),
            )
            BackupPreviewLine(
                label = stringResource(R.string.module_wallpaper_backup_created_at),
                value = if (preview.createdAtMillis > 0L) {
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(preview.createdAtMillis))
                } else {
                    stringResource(R.string.module_wallpaper_backup_unknown)
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    onClick = onDiscard,
                ) {
                    Text(stringResource(R.string.module_wallpaper_backup_discard))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    onClick = onRestore,
                ) {
                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.module_wallpaper_bundle_restore))
                }
            }
        }
    }
}

@Composable
private fun ModuleWallpaperSavedSlotsSection(
    slots: List<ModuleWallpaperSavedSlot?>,
    appliedSlotIndex: Int,
    canSave: Boolean,
    busy: Boolean,
    onSaveNext: () -> Unit,
    onSaveToSlot: (Int) -> Unit,
    onApply: (Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.module_wallpaper_slots_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.module_wallpaper_slots_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave && !busy,
            onClick = onSaveNext,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.module_wallpaper_slot_save_current))
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            itemsIndexed(
                items = slots,
                key = { index, _ -> index },
            ) { index, slot ->
                ModuleWallpaperSavedSlotCard(
                    slotIndex = index,
                    slot = slot,
                    applied = index == appliedSlotIndex,
                    canSave = canSave,
                    busy = busy,
                    onSave = { onSaveToSlot(index) },
                    onApply = { onApply(index) },
                    onDelete = { onDelete(index) },
                )
            }
        }
    }
}

@Composable
private fun ModuleWallpaperSavedSlotCard(
    slotIndex: Int,
    slot: ModuleWallpaperSavedSlot?,
    applied: Boolean,
    canSave: Boolean,
    busy: Boolean,
    onSave: () -> Unit,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    val previewEntry = slot?.snapshot?.allEntries()?.firstOrNull()
    val previewBitmap = rememberModuleCardWallpaperBitmap(previewEntry)
    val cardShape = RoundedCornerShape(8.dp)
    Card(
        modifier = Modifier
            .width(172.dp)
            .clickable(
                enabled = !busy && (slot != null || canSave),
                onClick = if (slot == null) onSave else onApply,
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (applied) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MODULE_CARD_WALLPAPER_ASPECT_RATIO)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (previewBitmap != null) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = stringResource(
                        R.string.module_wallpaper_slot_preview_description,
                        slotIndex + 1,
                    ),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = if (slot == null) Icons.Rounded.Image else Icons.Rounded.FileOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp),
                )
            }
            if (applied) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.module_wallpaper_slot_applied),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(21.dp),
                )
            }
        }
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 6.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(R.string.module_wallpaper_slot_label, slotIndex + 1),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (slot == null) {
                    stringResource(R.string.module_wallpaper_slot_empty)
                } else {
                    stringResource(
                        R.string.module_wallpaper_slot_details,
                        slot.snapshot.allEntries().size,
                        if (slot.snapshot.carouselEnabled) {
                            stringResource(R.string.module_wallpaper_backup_carousel_on)
                        } else {
                            stringResource(R.string.module_wallpaper_backup_carousel_off)
                        },
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (slot != null && slot.savedAtMillis > 0L) {
                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(slot.savedAtMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    enabled = canSave && !busy,
                    onClick = onSave,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = stringResource(
                            if (slot == null) {
                                R.string.module_wallpaper_slot_save
                            } else {
                                R.string.module_wallpaper_slot_overwrite
                            }
                        ),
                    )
                }
                if (slot != null) {
                    IconButton(
                        enabled = !busy,
                        onClick = onDelete,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.module_wallpaper_slot_delete),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleWallpaperBackupHeader(
    configured: Boolean,
    imageCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (configured) Icons.Rounded.CheckCircle else Icons.Rounded.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(34.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.module_wallpaper_backup_single_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (configured) {
                    stringResource(R.string.module_wallpaper_backup_ready, imageCount)
                } else {
                    stringResource(R.string.module_wallpaper_backup_not_ready)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModuleWallpaperTargetCard(
    module: Module?,
    snapshot: ModuleCardWallpaperSnapshot?,
    loading: Boolean,
    enabled: Boolean,
    loadError: String?,
    onChoose: () -> Unit,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !loading, onClick = onChoose),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.module_wallpaper_backup_target),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                when {
                    loading -> Text(stringResource(R.string.module_wallpaper_backup_loading_modules))
                    loadError != null -> {
                        Text(
                            text = stringResource(R.string.module_wallpaper_backup_module_load_failed),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onRetry, contentPadding = PaddingValues()) {
                            Text(stringResource(R.string.module_wallpaper_backup_retry))
                        }
                    }
                    module == null -> Text(stringResource(R.string.module_wallpaper_backup_choose_module))
                    else -> {
                        Text(
                            text = module.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = module.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(
                                R.string.module_wallpaper_backup_current_config,
                                snapshot?.allEntries()?.size ?: 0,
                                if (snapshot?.carouselEnabled == true) {
                                    stringResource(R.string.module_wallpaper_backup_carousel_on)
                                } else {
                                    stringResource(R.string.module_wallpaper_backup_carousel_off)
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.module_wallpaper_backup_choose_module),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModuleWallpaperImportPreviewCard(
    preview: ModuleWallpaperBackupPreview,
    targetModule: Module?,
    busy: Boolean,
    restoreEnabled: Boolean,
    onChooseTarget: () -> Unit,
    onRestore: () -> Unit,
    onDiscard: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.module_wallpaper_backup_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BackupPreviewLine(
                label = stringResource(R.string.module_wallpaper_backup_source_module),
                value = "${preview.sourceModuleName} (${preview.sourceModuleId})",
            )
            BackupPreviewLine(
                label = stringResource(R.string.module_wallpaper_backup_contents),
                value = stringResource(
                    R.string.module_wallpaper_backup_contents_value,
                    preview.imageCount,
                    formatBackupSize(preview.totalBytes),
                ),
            )
            BackupPreviewLine(
                label = stringResource(R.string.module_wallpaper_backup_created_at),
                value = if (preview.createdAtMillis > 0L) {
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(preview.createdAtMillis))
                } else {
                    stringResource(R.string.module_wallpaper_backup_unknown)
                },
            )
            BackupPreviewLine(
                label = stringResource(R.string.module_wallpaper_backup_restore_target),
                value = targetModule?.name ?: stringResource(R.string.module_wallpaper_backup_choose_module),
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                onClick = onChooseTarget,
            ) {
                Text(stringResource(R.string.module_wallpaper_backup_change_target))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                    onClick = onDiscard,
                ) {
                    Text(stringResource(R.string.module_wallpaper_backup_discard))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = restoreEnabled,
                    onClick = onRestore,
                ) {
                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.module_wallpaper_backup_restore))
                }
            }
        }
    }
}

@Composable
private fun BackupPreviewLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ModuleWallpaperModulePicker(
    modules: List<Module>,
    selectedModuleId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.module_wallpaper_backup_choose_module)) },
        text = {
            if (modules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.module_wallpaper_backup_no_modules))
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(modules, key = { it.id }) { module ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(module.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = module.id == selectedModuleId,
                                onClick = { onSelect(module.id) },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = module.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = module.id,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

private fun String.safeBackupFilePart(): String {
    return map { character ->
        if (character.isLetterOrDigit() || character == '.' || character == '-' || character == '_') {
            character
        } else {
            '_'
        }
    }.joinToString(separator = "").take(80).ifBlank { "module" }
}

private fun Throwable?.toDisplayMessage(fallback: String): String {
    return this?.localizedMessage
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.take(240)
        ?.takeIf { it.isNotBlank() }
        ?: fallback
}

private fun formatBackupSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024.0) return "%.1f KiB".format(kib)
    return "%.1f MiB".format(kib / 1024.0)
}
