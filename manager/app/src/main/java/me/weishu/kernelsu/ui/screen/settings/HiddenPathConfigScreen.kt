package me.weishu.kernelsu.ui.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.HIDDEN_PATH_CONFIG_FILE_NAME
import me.weishu.kernelsu.ui.util.HIDDEN_PATH_CONFIG_MIME_TYPE
import me.weishu.kernelsu.ui.util.HiddenPathConfigState
import me.weishu.kernelsu.ui.util.clearHiddenPathLogs
import me.weishu.kernelsu.ui.util.getHiddenPathConfig
import me.weishu.kernelsu.ui.util.getHiddenPathLogs
import me.weishu.kernelsu.ui.util.parseHiddenPathConfigJson
import me.weishu.kernelsu.ui.util.saveAndApplyHiddenPathConfig
import me.weishu.kernelsu.ui.util.toConfigJson
import me.weishu.kernelsu.ui.util.unloadHiddenPathKernelPaths

private const val TEXT_CONFIG = "\u914d\u7f6e"
private const val TEXT_LOG = "\u65e5\u5fd7"
private const val TEXT_HELP = "\u8bf4\u660e"
private const val TEXT_SUSUF_CONFIG = "susuf\u914d\u7f6e"
private const val TEXT_PATH_CONFIG = "\u8def\u5f84\u914d\u7f6e"
private const val TEXT_APP_CONFIG = "\u5e94\u7528\u9009\u62e9\u914d\u7f6e"
private const val TEXT_IMPORT_EXPORT_CONFIG = "\u914d\u7f6e\u5bfc\u5165\u5bfc\u51fa"
private const val TEXT_IMPORT_CONFIG = "\u5bfc\u5165\u914d\u7f6e"
private const val TEXT_EXPORT_CONFIG = "\u5bfc\u51fa\u914d\u7f6e"
private const val TEXT_ADD = "\u6dfb\u52a0"
private const val TEXT_ADD_APP_FROM_LIST = "\u4ece\u5e94\u7528\u5217\u8868\u9009\u62e9"
private const val TEXT_APP_PICKER_TITLE = "\u9009\u62e9\u5e94\u7528"
private const val TEXT_APP_SEARCH = "\u641c\u7d22\u5e94\u7528\u3001\u5305\u540d\u6216 UID"
private const val TEXT_DELETE = "\u5220\u9664"
private const val TEXT_APPLY = "\u786e\u5b9a\u9690\u85cf"
private const val TEXT_UNLOAD_ACTIVE = "\u5220\u9664\u5185\u6838\u6b63\u5728\u9690\u85cf\u8def\u5f84"
private const val TEXT_REFRESH = "\u5237\u65b0"
private const val TEXT_CLEAR = "\u6e05\u7a7a"
private const val TEXT_COPY_LOG = "\u590d\u5236\u65e5\u5fd7"
private const val TEXT_EXPORT_LOG = "\u5bfc\u51fa\u65e5\u5fd7"
private const val TEXT_LOG_FILE_NAME = "pathmask-log.txt"
private const val TEXT_EMPTY_PATH = "\u8fd8\u6ca1\u6709\u6dfb\u52a0\u8981\u9690\u85cf\u7684\u8def\u5f84"
private const val TEXT_EMPTY_APP = "\u8fd8\u6ca1\u6709\u6dfb\u52a0\u76ee\u6807\u5e94\u7528"
private const val TEXT_EMPTY_APP_PICKER = "\u6ca1\u6709\u627e\u5230\u5339\u914d\u5e94\u7528"
private const val TEXT_APPLY_OK = "\u9690\u85cf\u8def\u5f84\u914d\u7f6e\u5df2\u5e94\u7528"
private const val TEXT_APPLY_FAIL = "\u5e94\u7528\u5931\u8d25\uff0c\u8bf7\u67e5\u770b\u65e5\u5fd7"
private const val TEXT_UNLOAD_ACTIVE_OK = "\u5df2\u5220\u9664\u5185\u6838\u6b63\u5728\u9690\u85cf\u8def\u5f84"
private const val TEXT_UNLOAD_ACTIVE_FAIL = "\u5220\u9664\u5931\u8d25\uff0c\u8bf7\u67e5\u770b\u65e5\u5fd7"
private const val TEXT_IMPORT_OK = "\u914d\u7f6e\u5df2\u5bfc\u5165\uff0c\u70b9\u51fb\u786e\u5b9a\u9690\u85cf\u540e\u751f\u6548"
private const val TEXT_IMPORT_FAIL = "\u5bfc\u5165\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u914d\u7f6e\u6587\u4ef6"
private const val TEXT_EXPORT_OK = "\u914d\u7f6e\u5df2\u5bfc\u51fa"
private const val TEXT_EXPORT_FAIL = "\u5bfc\u51fa\u5931\u8d25"
private const val TEXT_LOG_COPY_OK = "\u65e5\u5fd7\u5df2\u590d\u5236"
private const val TEXT_LOG_EXPORT_OK = "\u65e5\u5fd7\u5df2\u5bfc\u51fa"
private const val TEXT_LOG_EXPORT_FAIL = "\u65e5\u5fd7\u5bfc\u51fa\u5931\u8d25"
private const val TEXT_CLIPBOARD_EMPTY = "\u526a\u8d34\u677f\u91cc\u6ca1\u6709\u53ef\u7528\u8def\u5f84"
private const val TEXT_NO_PATH = "\u8def\u5f84\u5217\u8868\u4e3a\u7a7a\uff0c\u8bf7\u5148\u6dfb\u52a0\u8981\u9690\u85cf\u7684\u8def\u5f84"
private const val TEXT_NO_APP = "\u5df2\u5f00\u542f\u6309\u5e94\u7528 UID \u9690\u85cf\uff0c\u8bf7\u6dfb\u52a0\u5305\u540d\u6216 UID\uff1b\u8981\u5168\u5c40\u751f\u6548\u5c31\u5173\u95ed\u8fd9\u4e2a\u5f00\u5173"

private val COMMON_HIDDEN_PATHS = listOf(
    "/data/adb/ksu",
    "/data/adb/modules",
    "/data/adb/ap",
    "/system/bin/su",
    "/system/xbin/su",
    "/vendor/bin/su",
)

@Composable
fun HiddenPathConfigScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onBack = dropUnlessResumed { navigator.pop() }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var config by remember { mutableStateOf(HiddenPathConfigState()) }
    var logs by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var applying by remember { mutableStateOf(false) }
    var unloadingActive by remember { mutableStateOf(false) }
    var pathInput by rememberSaveable { mutableStateOf("") }
    var appInput by rememberSaveable { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(HIDDEN_PATH_CONFIG_MIME_TYPE),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(config.toConfigJson().toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open output stream")
                }.isSuccess
            }
            Toast.makeText(
                context,
                if (ok) TEXT_EXPORT_OK else TEXT_EXPORT_FAIL,
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.use { input ->
                        input.reader(Charsets.UTF_8).readText()
                    } ?: error("Unable to open input stream")
                    parseHiddenPathConfigJson(text, config)
                }
            }
            imported
                .onSuccess { next ->
                    config = next
                    Toast.makeText(context, TEXT_IMPORT_OK, Toast.LENGTH_LONG).show()
                }
                .onFailure {
                    Toast.makeText(context, TEXT_IMPORT_FAIL, Toast.LENGTH_LONG).show()
                }
        }
    }
    val logExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(displayLogText(logs).toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open log output stream")
                }.isSuccess
            }
            Toast.makeText(
                context,
                if (ok) TEXT_LOG_EXPORT_OK else TEXT_LOG_EXPORT_FAIL,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun refreshAll() {
        scope.launch {
            loading = true
            config = getHiddenPathConfig()
            logs = getHiddenPathLogs()
            loading = false
        }
    }

    fun applyHiddenPathConfig() {
        scope.launch {
            applying = true
            val ok = saveAndApplyHiddenPathConfig(
                config.copy(
                    targetPaths = config.targetPaths.map(String::trim).filter(String::isNotEmpty).distinct(),
                    appPackages = config.appPackages.map(String::trim).filter(String::isNotEmpty).distinct(),
                )
            )
            config = getHiddenPathConfig()
            logs = getHiddenPathLogs()
            applying = false
            Toast.makeText(
                context,
                if (ok) TEXT_APPLY_OK else TEXT_APPLY_FAIL,
                Toast.LENGTH_LONG,
            ).show()
            if (!ok) {
                selectedTab = 1
            }
        }
    }

    fun unloadActiveHiddenPaths() {
        scope.launch {
            unloadingActive = true
            val ok = unloadHiddenPathKernelPaths()
            config = getHiddenPathConfig()
            logs = getHiddenPathLogs()
            unloadingActive = false
            Toast.makeText(
                context,
                if (ok) TEXT_UNLOAD_ACTIVE_OK else TEXT_UNLOAD_ACTIVE_FAIL,
                Toast.LENGTH_LONG,
            ).show()
            if (!ok) {
                selectedTab = 1
            }
        }
    }

    LaunchedEffect(Unit) {
        config = getHiddenPathConfig()
        logs = getHiddenPathLogs()
        loading = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hidden_path_config)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        bottomBar = {
            if (!loading && selectedTab == 0) {
                FixedApplyBar(
                    applying = applying,
                    enabled = !applying && !unloadingActive && config.blockReason() == null,
                    blockReason = config.blockReason(),
                    onApply = ::applyHiddenPathConfig,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                listOf(TEXT_CONFIG, TEXT_LOG, TEXT_HELP).forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }

            if (loading) {
                LoadingContent()
            } else {
                when (selectedTab) {
                    0 -> HiddenPathConfigTab(
                        config = config,
                        pathInput = pathInput,
                        appInput = appInput,
                        applying = applying,
                        unloadingActive = unloadingActive,
                        onPathInputChange = { pathInput = it },
                        onAppInputChange = { appInput = it },
                        onConfigChange = { config = it },
                        onImportConfig = {
                            importLauncher.launch(arrayOf(HIDDEN_PATH_CONFIG_MIME_TYPE, "text/*", "*/*"))
                        },
                        onExportConfig = {
                            exportLauncher.launch(HIDDEN_PATH_CONFIG_FILE_NAME)
                        },
                        onAddPath = {
                            val path = pathInput.trim()
                            if (path.isNotEmpty()) {
                                config = config.copy(targetPaths = (config.targetPaths + path).distinct())
                                pathInput = ""
                            }
                        },
                        onAddApp = {
                            val app = appInput.trim()
                            if (app.isNotEmpty()) {
                                config = config.copy(appPackages = (config.appPackages + app).distinct())
                                appInput = ""
                            }
                        },
                        onUnloadActive = ::unloadActiveHiddenPaths,
                        onAddAppPackage = { packageName ->
                            config = config.copy(appPackages = (config.appPackages + packageName).distinct())
                        },
                    )
                    1 -> HiddenPathLogTab(
                        logs = logs,
                        onRefresh = { refreshAll() },
                        onCopy = {
                            copyTextToClipboard(context, "pathmask.log", displayLogText(logs))
                            Toast.makeText(context, TEXT_LOG_COPY_OK, Toast.LENGTH_SHORT).show()
                        },
                        onExport = {
                            logExportLauncher.launch(TEXT_LOG_FILE_NAME)
                        },
                        onClear = {
                            scope.launch {
                                if (clearHiddenPathLogs()) {
                                    logs = getHiddenPathLogs()
                                }
                            }
                        },
                    )
                    else -> HiddenPathHelpTab()
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HiddenPathConfigTab(
    config: HiddenPathConfigState,
    pathInput: String,
    appInput: String,
    applying: Boolean,
    unloadingActive: Boolean,
    onPathInputChange: (String) -> Unit,
    onAppInputChange: (String) -> Unit,
    onConfigChange: (HiddenPathConfigState) -> Unit,
    onImportConfig: () -> Unit,
    onExportConfig: () -> Unit,
    onAddPath: () -> Unit,
    onAddApp: () -> Unit,
    onUnloadActive: () -> Unit,
    onAddAppPackage: (String) -> Unit,
) {
    val blockReason = config.blockReason()
    var showAppPicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HiddenPathStatusPanel(
            config = config,
            unloadingActive = unloadingActive,
            onUnloadActive = onUnloadActive,
        )

        ConfigSection(
            title = TEXT_IMPORT_EXPORT_CONFIG,
            summary = "\u53ef\u5c06\u5f53\u524d\u9690\u85cf\u8def\u5f84\u914d\u7f6e\u5907\u4efd\u6216\u6062\u590d\u5230\u672c\u9875\u3002",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !applying && !unloadingActive,
                    onClick = onImportConfig,
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(TEXT_IMPORT_CONFIG)
                }
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = !applying && !unloadingActive,
                    onClick = onExportConfig,
                ) {
                    Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(TEXT_EXPORT_CONFIG)
                }
            }
        }

        ConfigSection(
            title = TEXT_SUSUF_CONFIG,
            summary = "\u8fd9\u4e9b\u5f00\u5173\u4f1a\u4e0e\u4e0b\u9762\u7684\u8def\u5f84\u548c\u5e94\u7528\u5217\u8868\u4e00\u8d77\u5199\u5165 pathmask\u3002",
        ) {
            ToggleRow(
                title = "\u6309\u5e94\u7528 UID \u9690\u85cf",
                summary = "\u5f00\u542f\u540e\u53ea\u5bf9\u4e0b\u9762\u9009\u4e2d\u7684\u5e94\u7528\u751f\u6548\uff1b\u5173\u95ed\u540e\u5168\u5c40\u751f\u6548\u3002",
                checked = config.useAppScope,
                onCheckedChange = { onConfigChange(config.copy(useAppScope = it)) },
            )
            ToggleRow(
                title = "\u9690\u85cf\u76ee\u5f55\u5217\u8868",
                summary = "\u540c\u65f6\u62e6\u622a getdents64\uff0c\u907f\u514d\u76ee\u5f55\u91cc\u770b\u5230\u76ee\u6807\u8def\u5f84\u3002",
                checked = config.hideDirents,
                onCheckedChange = { onConfigChange(config.copy(hideDirents = it)) },
            )
            ToggleRow(
                title = "\u9690\u85cf\u9694\u79bb\u8fdb\u7a0b",
                summary = "\u5728 deny \u8303\u56f4\u4e0b\u4e5f\u5bf9 Android isolated UID \u751f\u6548\u3002",
                checked = config.hideIsolated,
                onCheckedChange = { onConfigChange(config.copy(hideIsolated = it)) },
            )
        }

        ConfigSection(title = "$TEXT_PATH_CONFIG (${config.targetPaths.size})") {
            AddRow(
                value = pathInput,
                label = "\u8def\u5f84",
                placeholder = "/data/local/tmp/pathmask",
                onValueChange = onPathInputChange,
                onAdd = onAddPath,
            )
            SmartPathActions(
                currentValue = pathInput,
                onUsePath = onPathInputChange,
                onAddPath = { path ->
                    onConfigChange(config.copy(targetPaths = (config.targetPaths + path).distinct()))
                },
            )
            EditableList(
                items = config.targetPaths,
                emptyText = TEXT_EMPTY_PATH,
                emptyKind = EmptyListKind.Path,
                onDelete = { item ->
                    onConfigChange(config.copy(targetPaths = config.targetPaths - item))
                },
            )
        }

        ConfigSection(title = "$TEXT_APP_CONFIG (${config.appPackages.size})") {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showAppPicker = true },
            ) {
                Icon(Icons.Rounded.Android, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(TEXT_ADD_APP_FROM_LIST)
            }
            AddRow(
                value = appInput,
                label = "\u5305\u540d\u6216 UID",
                placeholder = "com.example.app",
                onValueChange = onAppInputChange,
                onAdd = onAddApp,
            )
            EditableList(
                items = config.appPackages,
                emptyText = TEXT_EMPTY_APP,
                emptyKind = EmptyListKind.App,
                onDelete = { item ->
                    onConfigChange(config.copy(appPackages = config.appPackages - item))
                },
            )
        }
    }

    if (showAppPicker) {
        HiddenPathAppPickerDialog(
            selectedPackages = config.appPackages,
            onDismissRequest = { showAppPicker = false },
            onSelectPackage = onAddAppPackage,
        )
    }
}

@Composable
private fun HiddenPathStatusPanel(
    config: HiddenPathConfigState,
    unloadingActive: Boolean,
    onUnloadActive: () -> Unit,
) {
    ConfigSection(title = "\u5f53\u524d\u72b6\u6001") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (config.loaded) "\u5df2\u52a0\u8f7d" else "\u672a\u52a0\u8f7d",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "KMI ${config.currentKmi.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(
                text = if (config.loaded) "\u8fd0\u884c\u4e2d" else "\u5f85\u52a0\u8f7d",
                positive = config.loaded,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        StatusLine("\u52a0\u8f7d\u72b6\u6001", if (config.loaded) "\u5df2\u52a0\u8f7d" else "\u672a\u52a0\u8f7d")
        StatusLine("KMI", config.currentKmi.ifBlank { "-" })
        StatusLine("\u5df2\u89e3\u6790\u8def\u5f84", config.resolvedCount.ifBlank { "-" })
        if (!config.loaded) {
            config.blockReason()?.let { reason ->
                StatusLine("\u539f\u56e0", reason)
            }
        }
        if (config.activeTargetPaths.isNotBlank()) {
            StatusLine("\u5185\u6838\u6b63\u5728\u9690\u85cf", config.activeTargetPaths)
        }
        if (config.loaded || config.activeTargetPaths.isNotBlank()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !unloadingActive,
                onClick = onUnloadActive,
            ) {
                if (unloadingActive) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = TEXT_UNLOAD_ACTIVE,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun HiddenPathConfigState.blockReason(): String? {
    return when {
        targetPaths.isEmpty() -> TEXT_NO_PATH
        useAppScope && appPackages.isEmpty() -> TEXT_NO_APP
        else -> null
    }
}

@Composable
private fun ConfigSection(
    title: String,
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun FixedApplyBar(
    applying: Boolean,
    enabled: Boolean,
    blockReason: String?,
    onApply: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (blockReason != null) {
                Text(
                    text = blockReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                onClick = onApply,
            ) {
                if (applying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(TEXT_APPLY)
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    positive: Boolean,
) {
    val contentColor = if (positive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = contentColor.copy(alpha = 0.12f),
        contentColor = contentColor,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val switchStyle = LocalSwitchStyle.current
        if (switchStyle == SwitchStyle.Original) {
            Switch(checked = checked, onCheckedChange = null)
        } else {
            StyledSwitch(checked = checked, onCheckedChange = null, style = switchStyle)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartPathActions(
    currentValue: String,
    onUsePath: (String) -> Unit,
    onAddPath: (String) -> Unit,
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "\u5e38\u7528\u8def\u5f84",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = {
                    val text = readClipboardPath(context)
                    if (text.isNullOrBlank()) {
                        Toast.makeText(context, TEXT_CLIPBOARD_EMPTY, Toast.LENGTH_SHORT).show()
                    } else {
                        onUsePath(text)
                    }
                },
                label = { Text("\u7c98\u8d34\u8def\u5f84") },
                leadingIcon = {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
            val trimmedCurrent = currentValue.trim()
            if (trimmedCurrent.isNotBlank()) {
                AssistChip(
                    onClick = { onAddPath(trimmedCurrent) },
                    label = { Text("\u6dfb\u52a0\u5f53\u524d\u8f93\u5165") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
            COMMON_HIDDEN_PATHS.forEach { path ->
                AssistChip(
                    onClick = { onAddPath(path) },
                    label = { Text(path, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }
    }
}

@Composable
private fun AddRow(
    value: String,
    label: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 360.dp
        val textField: @Composable (Modifier) -> Unit = { modifier ->
            OutlinedTextField(
                modifier = modifier,
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
            )
        }
        val addButton: @Composable (Modifier) -> Unit = { modifier ->
            FilledTonalButton(
                modifier = modifier,
                enabled = value.isNotBlank(),
                onClick = onAdd,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(TEXT_ADD)
            }
        }

        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                textField(Modifier.fillMaxWidth())
                addButton(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                textField(Modifier.weight(1f))
                addButton(Modifier)
            }
        }
    }
}

@Composable
private fun EditableList(
    items: List<String>,
    emptyText: String,
    emptyKind: EmptyListKind,
    onDelete: (String) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyListState(text = emptyText, kind = emptyKind)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = item,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                IconButton(onClick = { onDelete(item) }) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = TEXT_DELETE,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private enum class EmptyListKind {
    Path,
    App,
}

@Composable
private fun EmptyListState(
    text: String,
    kind: EmptyListKind,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = when (kind) {
                    EmptyListKind.Path -> Icons.Rounded.Folder
                    EmptyListKind.App -> Icons.Rounded.Android
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(0.36f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.64f),
        )
    }
}

@Composable
private fun HiddenPathLogTab(
    logs: String,
    onRefresh: () -> Unit,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                modifier = Modifier.weight(1f),
                onClick = onRefresh,
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(TEXT_REFRESH)
            }
            FilledTonalButton(
                modifier = Modifier.weight(1f),
                onClick = onCopy,
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(TEXT_COPY_LOG)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onExport,
            ) {
                Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(TEXT_EXPORT_LOG)
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onClear,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(TEXT_CLEAR)
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    text = displayLogText(logs),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HiddenPathHelpTab() {
    val items = listOf(
        "\u8fd9\u4e2a\u9875\u9762\u914d\u7f6e\u5185\u7f6e pathmask LKM\uff0c\u4e0d\u662f\u666e\u901a\u6a21\u5757 WebUI\u3002",
        "\u8def\u5f84\u914d\u7f6e\u8981\u586b\u7edd\u5bf9\u8def\u5f84\uff0c\u591a\u4e2a\u8def\u5f84\u4f1a\u4ee5 target_paths \u53c2\u6570\u4f20\u7ed9 LKM\u3002",
        "\u5e94\u7528\u9009\u62e9\u652f\u6301\u5305\u540d\u6216 UID\uff0c\u5e94\u7528\u8303\u56f4\u6a21\u5f0f\u4f1a\u89e3\u6790\u6210 deny_uids\u3002",
        "\u70b9\u51fb\u786e\u5b9a\u9690\u85cf\u540e\uff0cksud \u4f1a\u7b49\u5f85\u76ee\u6807\u8def\u5f84\u51fa\u73b0\uff0c\u518d\u70ed\u91cd\u8f7d pathmask \u5e76\u5e94\u7528\u65b0\u914d\u7f6e\u3002",
        "\u5982\u679c\u6ca1\u6709\u9690\u85cf\u6548\u679c\uff0c\u5148\u770b\u65e5\u5fd7\u91cc\u7684 resolved_count\u3001KMI \u548c\u5185\u6838 dmesg \u8f93\u51fa\u3002",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ConfigSection(title = TEXT_HELP) {
            items.forEachIndexed { index, item ->
                HelpLine(index = index + 1, text = item)
            }
        }
    }
}

@Composable
private fun HelpLine(
    index: Int,
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "$index.",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HiddenPathAppPickerDialog(
    selectedPackages: List<String>,
    onDismissRequest: () -> Unit,
    onSelectPackage: (String) -> Unit,
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf<List<HiddenPathAppCandidate>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    val selectedSet = remember(selectedPackages) { selectedPackages.toSet() }
    val filteredApps by remember(apps, query) {
        derivedStateOf {
            val keyword = query.trim()
            if (keyword.isBlank()) {
                apps
            } else {
                apps.filter { app ->
                    app.label.contains(keyword, ignoreCase = true) ||
                        app.packageName.contains(keyword, ignoreCase = true) ||
                        app.uid.toString().contains(keyword)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        apps = loadHiddenPathAppCandidates(context)
        loading = false
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = TEXT_APP_PICKER_TITLE,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(TEXT_APP_SEARCH) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    filteredApps.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyListState(text = TEXT_EMPTY_APP_PICKER, kind = EmptyListKind.App)
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                val selected = app.packageName in selectedSet || app.uid.toString() in selectedSet
                                HiddenPathAppPickerRow(
                                    app = app,
                                    selected = selected,
                                    onClick = {
                                        if (!selected) {
                                            onSelectPackage(app.packageName)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenPathAppPickerRow(
    app: HiddenPathAppCandidate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !selected, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.56f)
        } else {
            Color.Transparent
        },
    ) {
        ListItem(
            headlineContent = {
                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(
                    text = "${app.packageName}\nUID ${app.uid}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = {
                AppIconImage(
                    packageInfo = app.packageInfo,
                    label = app.label,
                    modifier = Modifier.size(42.dp),
                )
            },
            trailingContent = {
                if (selected) {
                    Text(
                        text = "\u5df2\u6dfb\u52a0",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Icon(Icons.Rounded.Add, contentDescription = TEXT_ADD)
                }
            },
        )
    }
}

private data class HiddenPathAppCandidate(
    val label: String,
    val packageInfo: PackageInfo,
) {
    val packageName: String
        get() = packageInfo.packageName

    val uid: Int
        get() = packageInfo.applicationInfo?.uid ?: -1
}

private suspend fun loadHiddenPathAppCandidates(context: Context): List<HiddenPathAppCandidate> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val packages = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }
    }.getOrDefault(emptyList())

    packages.mapNotNull { packageInfo ->
        val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
        if (appInfo.isResourceOverlay) return@mapNotNull null
        HiddenPathAppCandidate(
            label = runCatching { appInfo.loadLabel(pm).toString() }.getOrDefault(packageInfo.packageName),
            packageInfo = packageInfo,
        )
    }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}

private fun readClipboardPath(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = clipboard.primaryClip ?: return null
    return (0 until clip.itemCount)
        .asSequence()
        .mapNotNull { index -> clip.getItemAt(index).coerceToText(context)?.toString() }
        .flatMap { it.lineSequence() }
        .map(String::trim)
        .firstOrNull { it.startsWith("/") }
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun displayLogText(logs: String): String {
    return logs.ifBlank { "\u6682\u65e0\u9690\u85cf\u8def\u5f84\u65e5\u5fd7" }
}
