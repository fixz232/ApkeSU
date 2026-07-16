package me.weishu.kernelsu.ui.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FindReplace
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.markdown.GithubMarkdown
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route

private enum class StudioFileDialogMode {
    Create,
    Rename,
}

@Composable
fun AiModuleStudioScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val viewModel = viewModel<AiModuleStudioViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var templateDialogVisible by rememberSaveable { mutableStateOf(false) }
    var metadataDialogVisible by rememberSaveable { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var fileDialogMode by rememberSaveable { mutableStateOf<StudioFileDialogMode?>(null) }
    var filePath by rememberSaveable { mutableStateOf("") }
    var deleteDialogVisible by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let(viewModel::exportProject)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImportUri = uri
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.ai_module_studio_title))
                        Text(
                            text = when (state.saveState) {
                                AiModuleDraftSaveState.Saved -> stringResource(R.string.ai_module_studio_saved)
                                AiModuleDraftSaveState.Saving -> stringResource(R.string.ai_module_studio_saving)
                                AiModuleDraftSaveState.Failed -> stringResource(R.string.ai_module_studio_save_failed)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.saveState == AiModuleDraftSaveState.Failed) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.ai_chat_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            StudioWorkspaceHeader(
                state = state,
                selected = state.selectedTab,
                onSelected = viewModel::selectTab,
            )
            when (state.selectedTab) {
                AiModuleStudioTab.Project -> StudioProject(
                    state = state,
                    onNewProject = { templateDialogVisible = true },
                    onImportProject = {
                        importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                    },
                    onEditMetadata = { metadataDialogVisible = true },
                    onExport = {
                        val metadata = state.project.metadata
                        val id = metadata.moduleId.ifBlank { "apkesu_module" }
                        val version = metadata.version.ifBlank { "1.0.0" }
                            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        exportLauncher.launch("${id}-${version}.zip")
                    },
                    onOpenIssue = { issue ->
                        issue.path?.let(viewModel::selectFile)
                        viewModel.selectTab(AiModuleStudioTab.Editor)
                    },
                )

                AiModuleStudioTab.Editor -> StudioEditor(
                    state = state,
                    onSelectFile = viewModel::selectFile,
                    onContentChange = viewModel::updateSelectedFileContent,
                    onCreateFile = {
                        filePath = ""
                        fileDialogMode = StudioFileDialogMode.Create
                    },
                    onRenameFile = {
                        filePath = state.selectedFile?.path.orEmpty()
                        fileDialogMode = StudioFileDialogMode.Rename
                    },
                    onDeleteFile = { deleteDialogVisible = true },
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onReplaceAll = viewModel::replaceAllInSelectedFile,
                )

                AiModuleStudioTab.Assistant -> StudioAssistant(
                    state = state,
                    onModeChange = viewModel::updateAiMode,
                    onPromptChange = viewModel::updateAiPrompt,
                    onSend = viewModel::requestAi,
                    onStop = viewModel::stopAi,
                    onRetry = viewModel::retryAi,
                    onReloadConfig = viewModel::reloadApiConfig,
                    onApplyCode = viewModel::requestApplyAiCode,
                    onOpenApiConfig = { navigator.push(Route.AiChat) },
                    onOpenTargetFile = { path ->
                        viewModel.selectFile(path)
                        viewModel.selectTab(AiModuleStudioTab.Editor)
                    },
                    onCopy = { copyToClipboard(context, it) },
                )
            }
        }
    }

    if (templateDialogVisible) {
        ModuleTemplateDialog(
            initial = state.project.metadata,
            onDismiss = { templateDialogVisible = false },
            onConfirm = { template, metadata ->
                templateDialogVisible = false
                viewModel.rebuildProject(template, metadata)
            },
        )
    }

    if (metadataDialogVisible) {
        ModuleMetadataDialog(
            initial = state.project.metadata,
            onDismiss = { metadataDialogVisible = false },
            onConfirm = { metadata ->
                metadataDialogVisible = false
                viewModel.updateProjectMetadata(metadata)
            },
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            icon = { Icon(Icons.Rounded.UploadFile, contentDescription = null) },
            title = { Text(stringResource(R.string.ai_module_studio_import_confirm_title)) },
            text = { Text(stringResource(R.string.ai_module_studio_import_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        viewModel.importProject(uri)
                    }
                ) {
                    Text(stringResource(R.string.ai_module_studio_import_project))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(stringResource(R.string.ai_chat_cancel))
                }
            },
        )
    }

    fileDialogMode?.let { mode ->
        FilePathDialog(
            mode = mode,
            value = filePath,
            onValueChange = { filePath = it },
            onDismiss = { fileDialogMode = null },
            onConfirm = {
                val success = when (mode) {
                    StudioFileDialogMode.Create -> viewModel.createFile(filePath)
                    StudioFileDialogMode.Rename -> viewModel.renameSelectedFile(filePath)
                }
                if (success) fileDialogMode = null
            },
        )
    }

    if (deleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { deleteDialogVisible = false },
            title = { Text(stringResource(R.string.ai_module_studio_delete_file_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.ai_module_studio_delete_file_message,
                        state.selectedFile?.path.orEmpty(),
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteDialogVisible = false
                        viewModel.deleteSelectedFile()
                    }
                ) { Text(stringResource(R.string.ai_chat_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogVisible = false }) {
                    Text(stringResource(R.string.ai_chat_cancel))
                }
            },
        )
    }

    state.pendingRequest?.let { pending ->
        AiProjectTransmissionDialog(
            pending = pending,
            onDismiss = viewModel::cancelAiRequest,
            onConfirm = viewModel::confirmAiRequest,
        )
    }

    state.pendingCodeReplacement?.let { replacement ->
        AiCodeReplacementDialog(
            path = state.aiTargetPath.orEmpty(),
            currentLength = state.project.files.firstOrNull { it.path == state.aiTargetPath }?.content?.length ?: 0,
            replacement = replacement,
            onDismiss = viewModel::cancelApplyAiCode,
            onConfirm = viewModel::confirmApplyAiCode,
        )
    }
}

@Composable
private fun StudioWorkspaceHeader(
    state: AiModuleStudioUiState,
    selected: AiModuleStudioTab,
    onSelected: (AiModuleStudioTab) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            ProjectStatusStrip(state)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StudioTabs(selected = selected, onSelected = onSelected)
        }
    }
}

@Composable
private fun ProjectStatusStrip(state: AiModuleStudioUiState) {
    val metadata = state.project.metadata
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Rounded.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                metadata.name.ifBlank { stringResource(R.string.ai_module_studio_unnamed_module) },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(
                    R.string.ai_module_studio_project_summary,
                    metadata.moduleId.ifBlank { "-" },
                    state.project.files.size,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ValidationBadge(state.validation)
    }
}

@Composable
private fun ValidationBadge(validation: AiModuleValidationResult) {
    val errors = validation.errors.size
    val warnings = validation.warnings.size
    val color = when {
        errors > 0 -> MaterialTheme.colorScheme.error
        warnings > 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val icon = when {
        errors > 0 -> Icons.Rounded.ErrorOutline
        warnings > 0 -> Icons.Rounded.WarningAmber
        else -> Icons.Rounded.CheckCircle
    }
    val text = when {
        errors > 0 -> stringResource(R.string.ai_module_studio_error_count, errors)
        warnings > 0 -> stringResource(R.string.ai_module_studio_warning_count, warnings)
        else -> stringResource(R.string.ai_module_studio_validation_ok)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

@Composable
private fun StudioTabs(
    selected: AiModuleStudioTab,
    onSelected: (AiModuleStudioTab) -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = selected.ordinal,
        containerColor = Color.Transparent,
    ) {
        AiModuleStudioTab.entries.forEach { tab ->
            val label = when (tab) {
                AiModuleStudioTab.Project -> stringResource(R.string.ai_module_studio_tab_project)
                AiModuleStudioTab.Editor -> stringResource(R.string.ai_module_studio_tab_editor)
                AiModuleStudioTab.Assistant -> stringResource(R.string.ai_module_studio_tab_assistant)
            }
            val icon = when (tab) {
                AiModuleStudioTab.Project -> Icons.Rounded.FolderOpen
                AiModuleStudioTab.Editor -> Icons.Rounded.Code
                AiModuleStudioTab.Assistant -> Icons.Rounded.AutoFixHigh
            }
            Tab(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(label, maxLines = 1)
                    }
                },
            )
        }
    }
}

@Composable
private fun StudioEditor(
    state: AiModuleStudioUiState,
    onSelectFile: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onCreateFile: () -> Unit,
    onRenameFile: () -> Unit,
    onDeleteFile: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReplaceAll: (String, String, Boolean) -> Int,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 760.dp) {
            Row(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilePane(
                    files = state.project.files,
                    selectedPath = state.project.selectedPath,
                    onSelectFile = onSelectFile,
                    onCreateFile = onCreateFile,
                    modifier = Modifier.width(248.dp).fillMaxHeight(),
                )
                EditorPane(
                    state = state,
                    onContentChange = onContentChange,
                    onCreateFile = onCreateFile,
                    onRenameFile = onRenameFile,
                    onDeleteFile = onDeleteFile,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onReplaceAll = onReplaceAll,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MobileFileRow(
                    files = state.project.files,
                    selectedPath = state.project.selectedPath,
                    onSelectFile = onSelectFile,
                    onCreateFile = onCreateFile,
                )
                EditorPane(
                    state = state,
                    onContentChange = onContentChange,
                    onCreateFile = onCreateFile,
                    onRenameFile = onRenameFile,
                    onDeleteFile = onDeleteFile,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onReplaceAll = onReplaceAll,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun FilePane(
    files: List<AiModuleStudioFile>,
    selectedPath: String,
    onSelectFile: (String) -> Unit,
    onCreateFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.ai_module_studio_files),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onCreateFile) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.ai_module_studio_add_file))
                }
            }
            HorizontalDivider()
            LazyColumn(contentPadding = PaddingValues(vertical = 6.dp)) {
                items(files, key = AiModuleStudioFile::path) { file ->
                    FileRow(file, selectedPath == file.path) { onSelectFile(file.path) }
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: AiModuleStudioFile, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Surface(color = background, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(file.path, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MobileFileRow(
    files: List<AiModuleStudioFile>,
    selectedPath: String,
    onSelectFile: (String) -> Unit,
    onCreateFile: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = false,
                onClick = onCreateFile,
                label = { Text(stringResource(R.string.ai_module_studio_add_file)) },
                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        items(files, key = AiModuleStudioFile::path) { file ->
            FilterChip(
                selected = selectedPath == file.path,
                onClick = { onSelectFile(file.path) },
                label = { Text(file.path, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun EditorPane(
    state: AiModuleStudioUiState,
    onContentChange: (String) -> Unit,
    onCreateFile: () -> Unit,
    onRenameFile: () -> Unit,
    onDeleteFile: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReplaceAll: (String, String, Boolean) -> Int,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedFile
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var replacement by rememberSaveable { mutableStateOf("") }
    var matchCase by rememberSaveable { mutableStateOf(false) }
    var replaceResult by rememberSaveable { mutableStateOf<Int?>(null) }
    val matchCount = remember(selected?.content, searchQuery, matchCase) {
        if (selected == null || searchQuery.isEmpty()) 0 else runCatching {
            Regex(
                Regex.escape(searchQuery),
                if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE),
            ).findAll(selected.content).count()
        }.getOrDefault(0)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 560.dp) {
                    Column {
                        EditorFileHeader(selected = selected, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        EditorToolbar(
                            canUndo = state.canUndo,
                            canRedo = state.canRedo,
                            hasSelection = selected != null,
                            canDelete = selected != null && selected.path != MODULE_PROP_PATH,
                            searchVisible = searchVisible,
                            onUndo = onUndo,
                            onRedo = onRedo,
                            onToggleSearch = { searchVisible = !searchVisible },
                            onRenameFile = onRenameFile,
                            onDeleteFile = onDeleteFile,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EditorFileHeader(selected = selected, modifier = Modifier.weight(1f))
                        EditorToolbar(
                            canUndo = state.canUndo,
                            canRedo = state.canRedo,
                            hasSelection = selected != null,
                            canDelete = selected != null && selected.path != MODULE_PROP_PATH,
                            searchVisible = searchVisible,
                            onUndo = onUndo,
                            onRedo = onRedo,
                            onToggleSearch = { searchVisible = !searchVisible },
                            onRenameFile = onRenameFile,
                            onDeleteFile = onDeleteFile,
                        )
                    }
                }
            }
            if (searchVisible) {
                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    SearchReplaceBar(
                        query = searchQuery,
                        replacement = replacement,
                        matchCase = matchCase,
                        matchCount = matchCount,
                        replaceResult = replaceResult,
                        onQueryChange = { searchQuery = it; replaceResult = null },
                        onReplacementChange = { replacement = it; replaceResult = null },
                        onMatchCaseChange = { matchCase = it; replaceResult = null },
                        onReplaceAll = { replaceResult = onReplaceAll(searchQuery, replacement, matchCase) },
                        onClose = { searchVisible = false },
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (selected == null) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    OutlinedButton(onClick = onCreateFile) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ai_module_studio_add_file))
                    }
                }
            } else {
                OutlinedTextField(
                    value = selected.content,
                    onValueChange = onContentChange,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    supportingText = {
                        Text(stringResource(R.string.ai_module_studio_utf8_lf))
                    },
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }
    }
}

@Composable
private fun EditorFileHeader(
    selected: AiModuleStudioFile?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            selected?.path ?: stringResource(R.string.ai_module_studio_no_file),
            style = MaterialTheme.typography.titleSmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(R.string.ai_module_studio_character_count, selected?.content?.length ?: 0),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EditorToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    hasSelection: Boolean,
    canDelete: Boolean,
    searchVisible: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleSearch: () -> Unit,
    onRenameFile: () -> Unit,
    onDeleteFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = stringResource(R.string.ai_module_studio_undo))
        }
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = stringResource(R.string.ai_module_studio_redo))
        }
        IconButton(onClick = onToggleSearch, enabled = hasSelection) {
            Icon(
                if (searchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = stringResource(R.string.ai_module_studio_search_replace),
            )
        }
        IconButton(onClick = onRenameFile, enabled = hasSelection) {
            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.ai_module_studio_rename_file))
        }
        IconButton(onClick = onDeleteFile, enabled = canDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.ai_module_studio_delete_file))
        }
    }
}

@Composable
private fun SearchReplaceBar(
    query: String,
    replacement: String,
    matchCase: Boolean,
    matchCount: Int,
    replaceResult: Int?,
    onQueryChange: (String) -> Unit,
    onReplacementChange: (String) -> Unit,
    onMatchCaseChange: (Boolean) -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text(stringResource(R.string.ai_module_studio_search)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.ai_chat_cancel))
                }
            }
            OutlinedTextField(
                value = replacement,
                onValueChange = onReplacementChange,
                label = { Text(stringResource(R.string.ai_module_studio_replace_with)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = matchCase, onCheckedChange = onMatchCaseChange)
                Text(stringResource(R.string.ai_module_studio_match_case), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(
                    if (replaceResult == null) {
                        stringResource(R.string.ai_module_studio_match_count, matchCount)
                    } else {
                        stringResource(R.string.ai_module_studio_replaced_count, replaceResult)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onReplaceAll, enabled = query.isNotEmpty() && matchCount > 0) {
                    Icon(Icons.Rounded.FindReplace, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.ai_module_studio_replace_all))
                }
            }
        }
    }
}

@Composable
private fun StudioProject(
    state: AiModuleStudioUiState,
    onNewProject: () -> Unit,
    onImportProject: () -> Unit,
    onEditMetadata: () -> Unit,
    onExport: () -> Unit,
    onOpenIssue: (AiModuleValidationIssue) -> Unit,
) {
    val metadata = state.project.metadata
    val issues = state.validation.issues
    val busy = state.importingProject || state.aiGenerating
    val canEditMetadata = state.project.files.any { it.path == MODULE_PROP_PATH }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(0.44f).fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.importingProject) {
                        item { ProjectImportProgress() }
                    }
                    item {
                        ProjectOverviewPanel(
                            metadata = metadata,
                            fileCount = state.project.files.size,
                            characterCount = state.project.files.sumOf { it.content.length },
                            canEdit = canEditMetadata && !busy,
                            onEditMetadata = onEditMetadata,
                        )
                    }
                    item {
                        ProjectSectionTitle(stringResource(R.string.ai_module_studio_project_actions))
                    }
                    item {
                        ProjectActionButtons(
                            busy = busy,
                            canExport = state.validation.canExport,
                            onNewProject = onNewProject,
                            onImportProject = onImportProject,
                            onExport = onExport,
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(0.56f).fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        ProjectSectionTitle(
                            title = stringResource(R.string.ai_module_studio_checks_title),
                            trailing = { ValidationBadge(state.validation) },
                        )
                    }
                    item { ProjectCheckSummary(state.validation) }
                    items(issues, key = { "${it.code}-${it.path}-${it.line}-${it.detail}" }) { issue ->
                        ValidationIssueRow(issue = issue, onClick = { onOpenIssue(issue) })
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.importingProject) {
                    item { ProjectImportProgress() }
                }
                item {
                    ProjectOverviewPanel(
                        metadata = metadata,
                        fileCount = state.project.files.size,
                        characterCount = state.project.files.sumOf { it.content.length },
                        canEdit = canEditMetadata && !busy,
                        onEditMetadata = onEditMetadata,
                    )
                }
                item {
                    ProjectSectionTitle(stringResource(R.string.ai_module_studio_project_actions))
                }
                item {
                    ProjectActionButtons(
                        busy = busy,
                        canExport = state.validation.canExport,
                        onNewProject = onNewProject,
                        onImportProject = onImportProject,
                        onExport = onExport,
                    )
                }
                item {
                    ProjectSectionTitle(
                        title = stringResource(R.string.ai_module_studio_checks_title),
                        trailing = { ValidationBadge(state.validation) },
                    )
                }
                item { ProjectCheckSummary(state.validation) }
                items(issues, key = { "${it.code}-${it.path}-${it.line}-${it.detail}" }) { issue ->
                    ValidationIssueRow(issue = issue, onClick = { onOpenIssue(issue) })
                }
            }
        }
    }
}

@Composable
private fun ProjectImportProgress() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            stringResource(R.string.ai_module_studio_importing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProjectOverviewPanel(
    metadata: AiModuleTemplateMetadata,
    fileCount: Int,
    characterCount: Int,
    canEdit: Boolean,
    onEditMetadata: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.ai_module_studio_module_info),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        metadata.name.ifBlank { stringResource(R.string.ai_module_studio_unnamed_module) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        metadata.moduleId.ifBlank { "-" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onEditMetadata, enabled = canEdit) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.ai_module_studio_edit_module_info),
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            ProjectMetadataRow(
                label = stringResource(R.string.ai_module_studio_version),
                value = "${metadata.version.ifBlank { "-" }} (${metadata.versionCode.ifBlank { "-" }})",
            )
            ProjectMetadataRow(
                label = stringResource(R.string.ai_module_studio_author),
                value = metadata.author.ifBlank { "-" },
            )
            Text(
                metadata.description.ifBlank { "-" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                stringResource(R.string.ai_module_studio_project_size_summary, fileCount, characterCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun ProjectSectionTitle(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
private fun ProjectCheckSummary(validation: AiModuleValidationResult) {
    val issues = validation.issues
    Surface(
        color = if (issues.isEmpty()) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                if (issues.isEmpty()) Icons.Rounded.CheckCircle else Icons.Rounded.Shield,
                contentDescription = null,
                tint = if (issues.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (issues.isEmpty()) {
                        stringResource(R.string.ai_module_studio_ready_to_export)
                    } else {
                        stringResource(
                            R.string.ai_module_studio_inspection_summary,
                            validation.errors.size,
                            validation.warnings.size,
                        )
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (issues.isEmpty()) {
                        stringResource(R.string.ai_module_studio_no_issues)
                    } else {
                        stringResource(R.string.ai_module_studio_fix_errors_export)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProjectMetadataRow(
    label: String,
    value: String,
    monospace: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.34f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(0.66f),
        )
    }
}

@Composable
private fun ProjectActionButtons(
    busy: Boolean,
    canExport: Boolean,
    onNewProject: () -> Unit,
    onImportProject: () -> Unit,
    onExport: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 520.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProjectNewButton(onNewProject, !busy, Modifier.weight(1f))
                    ProjectImportButton(onImportProject, !busy, Modifier.weight(1f))
                }
                ProjectExportButton(onExport, canExport && !busy, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProjectNewButton(onNewProject, !busy, Modifier.weight(1f))
                ProjectImportButton(onImportProject, !busy, Modifier.weight(1f))
                ProjectExportButton(onExport, canExport && !busy, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProjectNewButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.ai_module_studio_new_project), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProjectImportButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.ai_module_studio_import_project), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProjectExportButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.ai_module_studio_export), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ValidationIssueRow(issue: AiModuleValidationIssue, onClick: () -> Unit) {
    val color = if (issue.severity == AiModuleIssueSeverity.Error) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                if (issue.severity == AiModuleIssueSeverity.Error) Icons.Rounded.ErrorOutline else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(issueTitle(issue.code), style = MaterialTheme.typography.titleSmall, color = color)
                val location = buildString {
                    append(issue.path.orEmpty())
                    issue.line?.let { append(":$it") }
                }
                if (location.isNotBlank()) {
                    Text(location, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                }
                if (issue.detail.isNotBlank()) {
                    Text(
                        issue.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioAssistant(
    state: AiModuleStudioUiState,
    onModeChange: (AiModuleAiMode) -> Unit,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onReloadConfig: () -> Unit,
    onApplyCode: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onOpenTargetFile: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    val targetPath = state.aiTargetPath
        .takeIf { state.aiResponse.isNotBlank() }
        ?: state.project.selectedPath
    val targetAvailable = state.project.files.any { it.path == targetPath }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.width(340.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AssistantContextPanel(
                        state = state,
                        targetPath = targetPath,
                        targetAvailable = targetAvailable,
                        onReloadConfig = onReloadConfig,
                        onOpenApiConfig = onOpenApiConfig,
                        onOpenTargetFile = onOpenTargetFile,
                    )
                    AssistantModeSelector(
                        selected = state.aiMode,
                        enabled = !state.aiGenerating,
                        onModeChange = onModeChange,
                    )
                    state.aiError?.let { detail ->
                        AssistantError(detail = detail, onRetry = onRetry)
                    }
                    AssistantComposer(
                        state = state,
                        onPromptChange = onPromptChange,
                        onSend = onSend,
                        onStop = onStop,
                        expanded = true,
                    )
                }
                AssistantResponsePane(
                    state = state,
                    onCopy = onCopy,
                    onRetry = onRetry,
                    onApplyCode = onApplyCode,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistantContextPanel(
                    state = state,
                    targetPath = targetPath,
                    targetAvailable = targetAvailable,
                    onReloadConfig = onReloadConfig,
                    onOpenApiConfig = onOpenApiConfig,
                    onOpenTargetFile = onOpenTargetFile,
                )
                AssistantResponsePane(
                    state = state,
                    onCopy = onCopy,
                    onRetry = onRetry,
                    onApplyCode = onApplyCode,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                state.aiError?.let { detail ->
                    AssistantError(detail = detail, onRetry = onRetry)
                }
                AssistantModeSelector(
                    selected = state.aiMode,
                    enabled = !state.aiGenerating,
                    onModeChange = onModeChange,
                )
                AssistantComposer(
                    state = state,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onStop = onStop,
                    expanded = false,
                )
            }
        }
    }
}

@Composable
private fun AssistantContextPanel(
    state: AiModuleStudioUiState,
    targetPath: String,
    targetAvailable: Boolean,
    onReloadConfig: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onOpenTargetFile: (String) -> Unit,
) {
    val apiColor = if (state.apiConfigValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    if (state.apiConfigValid) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = apiColor,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (state.apiConfigValid) {
                            stringResource(R.string.ai_module_studio_api_ready)
                        } else {
                            stringResource(R.string.ai_module_studio_api_not_ready)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = apiColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        state.apiTargetHost,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!state.apiConfigValid) {
                    TextButton(onClick = onOpenApiConfig, enabled = !state.aiGenerating) {
                        Text(stringResource(R.string.ai_module_studio_configure_api))
                    }
                }
                IconButton(onClick = onReloadConfig, enabled = !state.aiGenerating) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.ai_module_studio_reload_api))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.ai_module_studio_ai_target_file),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        targetPath.ifBlank { stringResource(R.string.ai_module_studio_no_file) },
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = { onOpenTargetFile(targetPath) },
                    enabled = targetAvailable && !state.aiGenerating,
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = stringResource(R.string.ai_module_studio_open_target_file))
                }
            }
        }
    }
}

@Composable
private fun AssistantModeSelector(
    selected: AiModuleAiMode,
    enabled: Boolean,
    onModeChange: (AiModuleAiMode) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AiModuleAiMode.entries, key = AiModuleAiMode::name) { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onModeChange(mode) },
                enabled = enabled,
                label = { Text(aiModeTitle(mode)) },
            )
        }
    }
}

@Composable
private fun AssistantComposer(
    state: AiModuleStudioUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    expanded: Boolean,
) {
    OutlinedTextField(
        value = state.aiPrompt,
        onValueChange = onPromptChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.ai_module_studio_ai_request)) },
        placeholder = { Text(aiModeHint(state.aiMode)) },
        minLines = if (expanded) 5 else 2,
        maxLines = if (expanded) 9 else 4,
        enabled = !state.aiGenerating,
        shape = RoundedCornerShape(8.dp),
        trailingIcon = {
            IconButton(onClick = if (state.aiGenerating) onStop else onSend) {
                Icon(
                    if (state.aiGenerating) Icons.Rounded.StopCircle else Icons.AutoMirrored.Rounded.Send,
                    contentDescription = if (state.aiGenerating) {
                        stringResource(R.string.ai_chat_stop)
                    } else {
                        stringResource(R.string.ai_chat_send)
                    },
                )
            }
        },
    )
}

@Composable
private fun AssistantError(detail: String, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 3)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.ai_chat_retry)) }
        }
    }
}

@Composable
private fun AssistantResponsePane(
    state: AiModuleStudioUiState,
    onCopy: (String) -> Unit,
    onRetry: () -> Unit,
    onApplyCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.ai_module_studio_response),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.aiResponse.isNotBlank() && !state.aiGenerating) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onCopy(state.aiResponse) }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.ai_chat_copy))
                    }
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.ai_chat_retry))
                    }
                    Button(onClick = onApplyCode, enabled = state.aiTargetPath != null) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ai_module_studio_apply_code), maxLines = 1)
                    }
                }
            }
            if (state.aiGenerating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (state.aiResponse.isBlank()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            if (state.aiGenerating) {
                                stringResource(R.string.ai_chat_generating)
                            } else {
                                stringResource(R.string.ai_module_studio_ai_empty)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    item {
                        GithubMarkdown(
                            content = state.aiResponse,
                            isMarkdown = true,
                            allowRemoteContent = false,
                            contentPaddingDp = 0,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleTemplateDialog(
    initial: AiModuleTemplateMetadata,
    onDismiss: () -> Unit,
    onConfirm: (AiModuleTemplate, AiModuleTemplateMetadata) -> Unit,
) {
    var template by rememberSaveable { mutableStateOf(AiModuleTemplate.Basic) }
    var moduleId by rememberSaveable { mutableStateOf(initial.moduleId) }
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var version by rememberSaveable { mutableStateOf(initial.version) }
    var versionCode by rememberSaveable { mutableStateOf(initial.versionCode) }
    var author by rememberSaveable { mutableStateOf(initial.author) }
    var description by rememberSaveable { mutableStateOf(initial.description) }
    val metadata = AiModuleTemplateMetadata(moduleId, name, version, versionCode, author, description)
    val metadataValid = moduleId.matches(Regex("^[a-zA-Z][a-zA-Z0-9._-]+$")) &&
        name.isNotBlank() && version.isNotBlank() && versionCode.toIntOrNull()?.let { it > 0 } == true &&
        author.isNotBlank() && description.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ai_module_studio_template_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.ai_module_studio_template), style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiModuleTemplate.entries.forEach { item ->
                        FilterChip(
                            selected = template == item,
                            onClick = { template = item },
                            label = { Text(templateTitle(item)) },
                        )
                    }
                }
                OutlinedTextField(moduleId, { moduleId = it }, label = { Text(stringResource(R.string.ai_module_studio_module_id)) }, singleLine = true)
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.ai_module_studio_module_name)) }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(version, { version = it }, label = { Text(stringResource(R.string.ai_module_studio_version)) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(versionCode, { versionCode = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.ai_module_studio_version_code)) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(author, { author = it }, label = { Text(stringResource(R.string.ai_module_studio_author)) }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.ai_module_studio_description)) }, minLines = 2, maxLines = 3)
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.ai_module_studio_replace_project_warning), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(template, metadata) }, enabled = metadataValid) {
                Text(stringResource(R.string.ai_module_studio_generate_project))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_chat_cancel)) } },
    )
}

@Composable
private fun ModuleMetadataDialog(
    initial: AiModuleTemplateMetadata,
    onDismiss: () -> Unit,
    onConfirm: (AiModuleTemplateMetadata) -> Unit,
) {
    var moduleId by rememberSaveable { mutableStateOf(initial.moduleId) }
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var version by rememberSaveable { mutableStateOf(initial.version) }
    var versionCode by rememberSaveable { mutableStateOf(initial.versionCode) }
    var author by rememberSaveable { mutableStateOf(initial.author) }
    var description by rememberSaveable { mutableStateOf(initial.description) }
    val metadata = AiModuleTemplateMetadata(moduleId, name, version, versionCode, author, description)
    val metadataValid = moduleId.matches(Regex("^[a-zA-Z][a-zA-Z0-9._-]+$")) &&
        name.isNotBlank() && version.isNotBlank() && versionCode.toIntOrNull()?.let { it > 0 } == true &&
        author.isNotBlank() && description.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ai_module_studio_edit_module_info)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(moduleId, { moduleId = it.take(120) }, label = { Text(stringResource(R.string.ai_module_studio_module_id)) }, singleLine = true)
                OutlinedTextField(name, { name = it.take(160) }, label = { Text(stringResource(R.string.ai_module_studio_module_name)) }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(version, { version = it.take(80) }, label = { Text(stringResource(R.string.ai_module_studio_version)) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(versionCode, { versionCode = it.filter(Char::isDigit).take(10) }, label = { Text(stringResource(R.string.ai_module_studio_version_code)) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(author, { author = it.take(160) }, label = { Text(stringResource(R.string.ai_module_studio_author)) }, singleLine = true)
                OutlinedTextField(description, { description = it.take(500) }, label = { Text(stringResource(R.string.ai_module_studio_description)) }, minLines = 2, maxLines = 4)
                Text(
                    stringResource(R.string.ai_module_studio_metadata_preserved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(metadata) }, enabled = metadataValid) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ai_module_studio_save_module_info))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_chat_cancel)) } },
    )
}

@Composable
private fun FilePathDialog(
    mode: StudioFileDialogMode,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (mode == StudioFileDialogMode.Create) {
                    stringResource(R.string.ai_module_studio_add_file)
                } else {
                    stringResource(R.string.ai_module_studio_rename_file)
                }
            )
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it.take(180)) },
                label = { Text(stringResource(R.string.ai_module_studio_file_path)) },
                supportingText = { Text(stringResource(R.string.ai_module_studio_file_path_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = value.isNotBlank()) {
                Text(stringResource(R.string.ai_chat_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_chat_cancel)) } },
    )
}

@Composable
private fun AiProjectTransmissionDialog(
    pending: AiModulePendingRequest,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Shield, contentDescription = null) },
        title = { Text(stringResource(R.string.ai_chat_confirm_send_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.ai_chat_send_destination, pending.targetHost), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.ai_chat_external_api_warning))
                Text(stringResource(R.string.ai_module_studio_transmission_summary, pending.fileCount))
                if (pending.sendsAuthentication) Text(stringResource(R.string.ai_chat_data_auth))
                SelectionContainer {
                    Text(
                        pending.prompt.take(600),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.ai_chat_confirm_send)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_chat_cancel)) } },
    )
}

@Composable
private fun AiCodeReplacementDialog(
    path: String,
    currentLength: Int,
    replacement: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Code, contentDescription = null) },
        title = { Text(stringResource(R.string.ai_module_studio_apply_code_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(path, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.ai_module_studio_replacement_size, currentLength, replacement.length))
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(8.dp)) {
                    SelectionContainer {
                        Text(
                            replacement.take(2_000),
                            modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState()).heightIn(max = 260.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(stringResource(R.string.ai_module_studio_ai_not_executed), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.ai_module_studio_replace_file)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ai_chat_cancel)) } },
    )
}

@Composable
private fun issueTitle(code: AiModuleIssueCode): String = stringResource(
    when (code) {
        AiModuleIssueCode.MissingModuleProp -> R.string.ai_module_issue_missing_module_prop
        AiModuleIssueCode.MissingProperty -> R.string.ai_module_issue_missing_property
        AiModuleIssueCode.InvalidModuleId -> R.string.ai_module_issue_invalid_module_id
        AiModuleIssueCode.InvalidVersionCode -> R.string.ai_module_issue_invalid_version_code
        AiModuleIssueCode.DuplicateProperty -> R.string.ai_module_issue_duplicate_property
        AiModuleIssueCode.TooManyFiles -> R.string.ai_module_issue_too_many_files
        AiModuleIssueCode.InvalidPath -> R.string.ai_module_issue_invalid_path
        AiModuleIssueCode.DuplicatePath -> R.string.ai_module_issue_duplicate_path
        AiModuleIssueCode.ReservedPath -> R.string.ai_module_issue_reserved_path
        AiModuleIssueCode.FileTooLarge -> R.string.ai_module_issue_file_too_large
        AiModuleIssueCode.ProjectTooLarge -> R.string.ai_module_issue_project_too_large
        AiModuleIssueCode.NullCharacter -> R.string.ai_module_issue_null_character
        AiModuleIssueCode.MissingShebang -> R.string.ai_module_issue_missing_shebang
        AiModuleIssueCode.WindowsLineEnding -> R.string.ai_module_issue_windows_line_ending
        AiModuleIssueCode.DestructiveRootCommand -> R.string.ai_module_issue_destructive_root_command
        AiModuleIssueCode.BlockDeviceWrite -> R.string.ai_module_issue_block_device_write
        AiModuleIssueCode.UnsafePermission -> R.string.ai_module_issue_unsafe_permission
        AiModuleIssueCode.RuntimeSecurityChange -> R.string.ai_module_issue_runtime_security_change
        AiModuleIssueCode.RemotePipeExecution -> R.string.ai_module_issue_remote_pipe_execution
        AiModuleIssueCode.RebootCommand -> R.string.ai_module_issue_reboot_command
        AiModuleIssueCode.ManagerDirectoryMutation -> R.string.ai_module_issue_manager_directory_mutation
        AiModuleIssueCode.MalformedSystemProperty -> R.string.ai_module_issue_malformed_system_property
        AiModuleIssueCode.BroadSePolicyRule -> R.string.ai_module_issue_broad_sepolicy_rule
        AiModuleIssueCode.MissingWebUiEntry -> R.string.ai_module_issue_missing_webui_entry
    }
)

@Composable
private fun aiModeTitle(mode: AiModuleAiMode): String = when (mode) {
    AiModuleAiMode.Ask -> stringResource(R.string.ai_module_studio_ai_ask)
    AiModuleAiMode.ReviewProject -> stringResource(R.string.ai_module_studio_ai_review)
    AiModuleAiMode.GenerateCurrentFile -> stringResource(R.string.ai_module_studio_ai_generate_file)
    AiModuleAiMode.FixCurrentFile -> stringResource(R.string.ai_module_studio_ai_fix_file)
}

@Composable
private fun aiModeHint(mode: AiModuleAiMode): String = when (mode) {
    AiModuleAiMode.Ask -> stringResource(R.string.ai_module_studio_ai_ask_hint)
    AiModuleAiMode.ReviewProject -> stringResource(R.string.ai_module_studio_ai_review_hint)
    AiModuleAiMode.GenerateCurrentFile -> stringResource(R.string.ai_module_studio_ai_generate_hint)
    AiModuleAiMode.FixCurrentFile -> stringResource(R.string.ai_module_studio_ai_fix_hint)
}

@Composable
private fun templateTitle(template: AiModuleTemplate): String = when (template) {
    AiModuleTemplate.Basic -> stringResource(R.string.ai_module_template_basic)
    AiModuleTemplate.BootService -> stringResource(R.string.ai_module_template_boot_service)
    AiModuleTemplate.SystemProperties -> stringResource(R.string.ai_module_template_system_properties)
    AiModuleTemplate.WebUi -> stringResource(R.string.ai_module_template_webui)
    AiModuleTemplate.Complete -> stringResource(R.string.ai_module_template_complete)
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("ApkeSU AI", text))
    Toast.makeText(context, context.getString(R.string.ai_chat_copied), Toast.LENGTH_SHORT).show()
}
