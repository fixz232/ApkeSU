package me.weishu.kernelsu.ui.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Visibility
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.HIDDEN_PATH_CONFIG_FILE_NAME
import me.weishu.kernelsu.ui.util.HIDDEN_PATH_CONFIG_MIME_TYPE
import me.weishu.kernelsu.ui.util.HiddenPathConfigState
import me.weishu.kernelsu.ui.util.HiddenPathVisibilityResult
import me.weishu.kernelsu.ui.util.clearHiddenPathLogs
import me.weishu.kernelsu.ui.util.getHiddenPathConfig
import me.weishu.kernelsu.ui.util.getHiddenPathLogs
import me.weishu.kernelsu.ui.util.parseHiddenPathConfigJson
import me.weishu.kernelsu.ui.util.saveAndApplyHiddenPathConfig
import me.weishu.kernelsu.ui.util.toConfigJson
import me.weishu.kernelsu.ui.util.testHiddenPathVisibility
import me.weishu.kernelsu.ui.util.unloadHiddenPathKernelPaths
import org.json.JSONArray
import org.json.JSONObject

private fun hiddenPathText(@StringRes id: Int, vararg args: Any): String = ksuApp.getString(id, *args)

private val TEXT_CONFIG get() = hiddenPathText(R.string.hidden_path_tab_config)
private val TEXT_LOG get() = hiddenPathText(R.string.hidden_path_tab_log)
private val TEXT_HELP get() = hiddenPathText(R.string.hidden_path_tab_help)
private val TEXT_SUSUF_CONFIG get() = hiddenPathText(R.string.hidden_path_options)
private val TEXT_PATH_CONFIG get() = hiddenPathText(R.string.hidden_path_paths)
private val TEXT_APP_CONFIG get() = hiddenPathText(R.string.hidden_path_apps)
private val TEXT_IMPORT_EXPORT_CONFIG get() = hiddenPathText(R.string.hidden_path_import_export)
private val TEXT_TEMPLATE_CONFIG get() = hiddenPathText(R.string.hidden_path_templates)
private val TEXT_IMPORT_CONFIG get() = hiddenPathText(R.string.hidden_path_import_config)
private val TEXT_EXPORT_CONFIG get() = hiddenPathText(R.string.hidden_path_export_config)
private val TEXT_SAVE_TEMPLATE get() = hiddenPathText(R.string.hidden_path_save_template)
private val TEXT_TEMPLATE_NAME get() = hiddenPathText(R.string.hidden_path_template_name)
private val TEXT_APPLY_TEMPLATE get() = hiddenPathText(R.string.hidden_path_apply_template)
private val TEXT_ADD get() = hiddenPathText(R.string.hidden_path_add)
private val TEXT_ADD_APP_FROM_LIST get() = hiddenPathText(R.string.hidden_path_add_app_from_list)
private val TEXT_APP_PICKER_TITLE get() = hiddenPathText(R.string.hidden_path_app_picker_title)
private val TEXT_APP_SEARCH get() = hiddenPathText(R.string.hidden_path_app_search)
private val TEXT_DELETE get() = hiddenPathText(R.string.hidden_path_delete)
private val TEXT_APPLY get() = hiddenPathText(R.string.hidden_path_apply)
private val TEXT_UNLOAD_ACTIVE get() = hiddenPathText(R.string.hidden_path_unload_active)
private val TEXT_REFRESH get() = hiddenPathText(R.string.hidden_path_refresh)
private val TEXT_CLEAR get() = hiddenPathText(R.string.hidden_path_clear)
private val TEXT_COPY_LOG get() = hiddenPathText(R.string.hidden_path_copy_log)
private val TEXT_EXPORT_LOG get() = hiddenPathText(R.string.hidden_path_export_log)
private const val TEXT_LOG_FILE_NAME = "pathmask-log.txt"
private val TEXT_EMPTY_PATH get() = hiddenPathText(R.string.hidden_path_empty_path)
private val TEXT_EMPTY_APP get() = hiddenPathText(R.string.hidden_path_empty_app)
private val TEXT_EMPTY_APP_PICKER get() = hiddenPathText(R.string.hidden_path_empty_app_picker)
private val TEXT_APPLY_OK get() = hiddenPathText(R.string.hidden_path_apply_success)
private val TEXT_APPLY_FAIL get() = hiddenPathText(R.string.hidden_path_apply_failed)
private val TEXT_UNLOAD_ACTIVE_OK get() = hiddenPathText(R.string.hidden_path_unload_success)
private val TEXT_UNLOAD_ACTIVE_FAIL get() = hiddenPathText(R.string.hidden_path_unload_failed)
private val TEXT_IMPORT_OK get() = hiddenPathText(R.string.hidden_path_import_success)
private val TEXT_IMPORT_FAIL get() = hiddenPathText(R.string.hidden_path_import_failed)
private val TEXT_EXPORT_OK get() = hiddenPathText(R.string.hidden_path_export_success)
private val TEXT_EXPORT_FAIL get() = hiddenPathText(R.string.hidden_path_export_failed)
private val TEXT_TEMPLATE_SAVE_OK get() = hiddenPathText(R.string.hidden_path_template_save_success)
private val TEXT_TEMPLATE_SAVE_FAIL get() = hiddenPathText(R.string.hidden_path_template_save_failed)
private val TEXT_TEMPLATE_APPLY_OK get() = hiddenPathText(R.string.hidden_path_template_apply_success)
private val TEXT_TEMPLATE_DELETE_OK get() = hiddenPathText(R.string.hidden_path_template_delete_success)
private val TEXT_LOG_COPY_OK get() = hiddenPathText(R.string.hidden_path_log_copy_success)
private val TEXT_LOG_EXPORT_OK get() = hiddenPathText(R.string.hidden_path_log_export_success)
private val TEXT_LOG_EXPORT_FAIL get() = hiddenPathText(R.string.hidden_path_log_export_failed)
private val TEXT_CLIPBOARD_EMPTY get() = hiddenPathText(R.string.hidden_path_clipboard_empty)
private val TEXT_EMPTY_TEMPLATE get() = hiddenPathText(R.string.hidden_path_empty_template)
private val TEXT_TEMPLATE_NAME_EMPTY get() = hiddenPathText(R.string.hidden_path_template_name_empty)
private val TEXT_DUPLICATE_ITEM get() = hiddenPathText(R.string.hidden_path_duplicate_item)
private val TEXT_INVALID_PATH get() = hiddenPathText(R.string.hidden_path_invalid_path)
private val TEXT_INVALID_APP get() = hiddenPathText(R.string.hidden_path_invalid_app)
private val TEXT_NO_PATH get() = hiddenPathText(R.string.hidden_path_no_path)
private val TEXT_NO_APP get() = hiddenPathText(R.string.hidden_path_no_app)
private val TEXT_MANAGED_PATH_GLOBAL_BLOCK get() = hiddenPathText(R.string.hidden_path_managed_global_block)
private val TEXT_WAITING_CONFIG get() = hiddenPathText(R.string.hidden_path_waiting_config)
private val TEXT_WAITING_LOAD get() = hiddenPathText(R.string.hidden_path_waiting_load)
private val TEXT_RUNNING get() = hiddenPathText(R.string.hidden_path_running)
private val TEXT_VISIBILITY_TEST get() = hiddenPathText(R.string.hidden_path_visibility_test)
private val TEXT_VISIBILITY_TEST_SUMMARY get() = hiddenPathText(R.string.hidden_path_visibility_test_summary)
private val TEXT_VISIBILITY_TEST_ACTION get() = hiddenPathText(R.string.hidden_path_visibility_test_action)
private val TEXT_VISIBILITY_USE_CONFIG get() = hiddenPathText(R.string.hidden_path_visibility_use_config)
private val TEXT_VISIBILITY_UID_INVALID get() = hiddenPathText(R.string.hidden_path_visibility_uid_invalid)
private const val PREF_HIDDEN_PATH_TEMPLATES = "hidden_path_config_templates"
private const val PREF_KEY_TEMPLATES = "templates"

private val COMMON_HIDDEN_PATHS = listOf(
    "/data/adb/ksu",
    "/data/adb/modules",
    "/data/adb/ap",
    "/system/bin/su",
    "/system/xbin/su",
    "/vendor/bin/su",
)

private val MANAGED_ROOT_PATHS = listOf(
    "/data/adb/modules",
    "/data/adb/modules_update",
    "/data/adb/ksu",
    "/data/adb/ap",
)

private data class HiddenPathTemplate(
    val name: String,
    val config: HiddenPathConfigState,
)

@Composable
fun HiddenPathConfigScreen() {
    if (!Natives.isLkmMode && !Natives.isLateLoadMode) {
        SusfsPathConfigScreen()
        return
    }
    if (Natives.isLateLoadMode) {
        HiddenPathUnavailableScreen()
        return
    }
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
    var templateName by rememberSaveable { mutableStateOf("") }
    var templates by remember { mutableStateOf(loadHiddenPathTemplates(context)) }
    var visibilityUidInput by rememberSaveable { mutableStateOf("") }
    var visibilityPathInput by rememberSaveable { mutableStateOf("") }
    var visibilityTesting by remember { mutableStateOf(false) }
    var visibilityResult by remember { mutableStateOf<HiddenPathVisibilityResult?>(null) }

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
                    config = sanitizeHiddenPathConfig(next)
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

    fun addPath(rawPath: String, clearInput: Boolean) {
        val path = normalizeHiddenPath(rawPath)
        when {
            path == null -> Toast.makeText(context, TEXT_INVALID_PATH, Toast.LENGTH_LONG).show()
            path in config.targetPaths -> Toast.makeText(context, TEXT_DUPLICATE_ITEM, Toast.LENGTH_SHORT).show()
            else -> {
                config = config.copy(targetPaths = (config.targetPaths + path).distinct())
                if (clearInput) {
                    pathInput = ""
                }
            }
        }
    }

    fun addAppEntry(rawEntry: String, clearInput: Boolean) {
        val entry = normalizeAppEntry(rawEntry)
        when {
            entry == null -> Toast.makeText(context, TEXT_INVALID_APP, Toast.LENGTH_LONG).show()
            entry in config.appPackages -> Toast.makeText(context, TEXT_DUPLICATE_ITEM, Toast.LENGTH_SHORT).show()
            else -> {
                config = config.copy(appPackages = (config.appPackages + entry).distinct())
                if (clearInput) {
                    appInput = ""
                }
            }
        }
    }

    fun saveTemplate() {
        val name = templateName.trim()
        if (name.isBlank()) {
            Toast.makeText(context, TEXT_TEMPLATE_NAME_EMPTY, Toast.LENGTH_SHORT).show()
            return
        }
        val next = (templates.filterNot { it.name == name } + HiddenPathTemplate(name, sanitizeHiddenPathConfig(config)))
            .sortedBy { it.name.lowercase() }
        if (saveHiddenPathTemplates(context, next)) {
            templates = next
            templateName = ""
            Toast.makeText(context, TEXT_TEMPLATE_SAVE_OK, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, TEXT_TEMPLATE_SAVE_FAIL, Toast.LENGTH_LONG).show()
        }
    }

    fun applyTemplate(template: HiddenPathTemplate) {
        config = sanitizeHiddenPathConfig(template.config)
        Toast.makeText(context, TEXT_TEMPLATE_APPLY_OK, Toast.LENGTH_LONG).show()
    }

    fun deleteTemplate(template: HiddenPathTemplate) {
        val next = templates.filterNot { it.name == template.name }
        if (saveHiddenPathTemplates(context, next)) {
            templates = next
            Toast.makeText(context, TEXT_TEMPLATE_DELETE_OK, Toast.LENGTH_SHORT).show()
        }
    }

    fun applyHiddenPathConfig() {
        if (applying || unloadingActive) return
        applying = true
        scope.launch {
            try {
                val ok = saveAndApplyHiddenPathConfig(
                    sanitizeHiddenPathConfig(config)
                )
                config = getHiddenPathConfig()
                logs = getHiddenPathLogs()
                Toast.makeText(
                    context,
                    if (ok) TEXT_APPLY_OK else TEXT_APPLY_FAIL,
                    Toast.LENGTH_LONG,
                ).show()
                if (!ok) {
                    selectedTab = 1
                }
            } finally {
                applying = false
            }
        }
    }

    fun unloadActiveHiddenPaths() {
        if (applying || unloadingActive) return
        unloadingActive = true
        scope.launch {
            try {
                val ok = unloadHiddenPathKernelPaths()
                config = getHiddenPathConfig()
                logs = getHiddenPathLogs()
                Toast.makeText(
                    context,
                    if (ok) TEXT_UNLOAD_ACTIVE_OK else TEXT_UNLOAD_ACTIVE_FAIL,
                    Toast.LENGTH_LONG,
                ).show()
                if (!ok) {
                    selectedTab = 1
                }
            } finally {
                unloadingActive = false
            }
        }
    }

    fun testConfiguredVisibility() {
        if (visibilityTesting) return
        val uid = visibilityUidInput.trim().toIntOrNull()?.takeIf { it > 0 }
        if (uid == null) {
            Toast.makeText(context, TEXT_VISIBILITY_UID_INVALID, Toast.LENGTH_LONG).show()
            return
        }
        val path = normalizeHiddenPath(visibilityPathInput)
        if (path == null) {
            Toast.makeText(context, TEXT_INVALID_PATH, Toast.LENGTH_LONG).show()
            return
        }

        visibilityTesting = true
        visibilityResult = null
        scope.launch {
            try {
                visibilityResult = testHiddenPathVisibility(uid, path)
                logs = getHiddenPathLogs()
            } finally {
                visibilityTesting = false
            }
        }
    }

    LaunchedEffect(Unit) {
        config = getHiddenPathConfig()
        visibilityPathInput = config.targetPaths.firstOrNull().orEmpty()
        visibilityUidInput = config.appPackages.firstNotNullOfOrNull { entry ->
            entry.toIntOrNull()?.toString()
        }.orEmpty()
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
                actions = {
                    ForegroundToolProtectionTopBarAction(
                        onClick = { navigator.push(Route.ForegroundToolProtection) },
                    )
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
                        templateName = templateName,
                        templates = templates,
                        applying = applying,
                        unloadingActive = unloadingActive,
                        visibilityUidInput = visibilityUidInput,
                        visibilityPathInput = visibilityPathInput,
                        visibilityTesting = visibilityTesting,
                        visibilityResult = visibilityResult,
                        onPathInputChange = { pathInput = it },
                        onAppInputChange = { appInput = it },
                        onTemplateNameChange = { templateName = it },
                        onConfigChange = { config = it },
                        onVisibilityUidChange = {
                            visibilityUidInput = it.filter(Char::isDigit).take(10)
                            visibilityResult = null
                        },
                        onVisibilityPathChange = {
                            visibilityPathInput = it
                            visibilityResult = null
                        },
                        onUseVisibilityConfig = {
                            visibilityPathInput = config.targetPaths.firstOrNull().orEmpty()
                            visibilityUidInput = config.appPackages.firstNotNullOfOrNull { entry ->
                                entry.toIntOrNull()?.toString()
                            }.orEmpty()
                            visibilityResult = null
                        },
                        onTestVisibility = ::testConfiguredVisibility,
                        onImportConfig = {
                            importLauncher.launch(arrayOf(HIDDEN_PATH_CONFIG_MIME_TYPE, "text/*", "*/*"))
                        },
                        onExportConfig = {
                            exportLauncher.launch(HIDDEN_PATH_CONFIG_FILE_NAME)
                        },
                        onSaveTemplate = ::saveTemplate,
                        onApplyTemplate = ::applyTemplate,
                        onDeleteTemplate = ::deleteTemplate,
                        onAddPath = {
                            addPath(pathInput, clearInput = true)
                        },
                        onAddApp = {
                            addAppEntry(appInput, clearInput = true)
                        },
                        onUnloadActive = ::unloadActiveHiddenPaths,
                        onAddPathValue = { path ->
                            addPath(path, clearInput = false)
                        },
                        onAddAppUid = { uid ->
                            addAppEntry(uid.toString(), clearInput = false)
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
private fun HiddenPathUnavailableScreen() {
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hidden_path_config)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_hidden_path_jailbreak_disabled_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
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
    templateName: String,
    templates: List<HiddenPathTemplate>,
    applying: Boolean,
    unloadingActive: Boolean,
    visibilityUidInput: String,
    visibilityPathInput: String,
    visibilityTesting: Boolean,
    visibilityResult: HiddenPathVisibilityResult?,
    onPathInputChange: (String) -> Unit,
    onAppInputChange: (String) -> Unit,
    onTemplateNameChange: (String) -> Unit,
    onConfigChange: (HiddenPathConfigState) -> Unit,
    onVisibilityUidChange: (String) -> Unit,
    onVisibilityPathChange: (String) -> Unit,
    onUseVisibilityConfig: () -> Unit,
    onTestVisibility: () -> Unit,
    onImportConfig: () -> Unit,
    onExportConfig: () -> Unit,
    onSaveTemplate: () -> Unit,
    onApplyTemplate: (HiddenPathTemplate) -> Unit,
    onDeleteTemplate: (HiddenPathTemplate) -> Unit,
    onAddPath: () -> Unit,
    onAddApp: () -> Unit,
    onUnloadActive: () -> Unit,
    onAddPathValue: (String) -> Unit,
    onAddAppUid: (Int) -> Unit,
) {
    val blockReason = config.blockReason()
    var showAppPicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HiddenPathStatusPanel(
            config = config,
            unloadingActive = unloadingActive,
            onUnloadActive = onUnloadActive,
        )

        VisibilityProbeSection(
            uidInput = visibilityUidInput,
            pathInput = visibilityPathInput,
            testing = visibilityTesting,
            result = visibilityResult,
            hasCurrentConfig = config.targetPaths.isNotEmpty() &&
                config.appPackages.any { it.toIntOrNull() != null },
            onUidChange = onVisibilityUidChange,
            onPathChange = onVisibilityPathChange,
            onUseCurrentConfig = onUseVisibilityConfig,
            onTest = onTestVisibility,
        )

        ConfigSection(
            title = TEXT_IMPORT_EXPORT_CONFIG,
            summary = hiddenPathText(R.string.hidden_path_import_export_summary),
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
            title = TEXT_TEMPLATE_CONFIG,
            summary = hiddenPathText(R.string.hidden_path_templates_summary),
        ) {
            TemplateControls(
                templateName = templateName,
                templates = templates,
                enabled = !applying && !unloadingActive,
                onTemplateNameChange = onTemplateNameChange,
                onSaveTemplate = onSaveTemplate,
                onApplyTemplate = onApplyTemplate,
                onDeleteTemplate = onDeleteTemplate,
            )
        }

        ConfigSection(
            title = TEXT_SUSUF_CONFIG,
            summary = hiddenPathText(R.string.hidden_path_options_summary),
        ) {
            ToggleRow(
                title = hiddenPathText(R.string.hidden_path_uid_scope),
                summary = hiddenPathText(R.string.hidden_path_uid_scope_summary),
                checked = config.useAppScope,
                onCheckedChange = { onConfigChange(config.copy(useAppScope = it)) },
            )
            ToggleRow(
                title = hiddenPathText(R.string.hidden_path_hide_directory_entries),
                summary = hiddenPathText(R.string.hidden_path_hide_directory_entries_summary),
                checked = config.hideDirents,
                onCheckedChange = { onConfigChange(config.copy(hideDirents = it)) },
            )
            ToggleRow(
                title = hiddenPathText(R.string.hidden_path_hide_isolated_processes),
                summary = hiddenPathText(R.string.hidden_path_hide_isolated_processes_summary),
                checked = config.hideIsolated,
                onCheckedChange = { onConfigChange(config.copy(hideIsolated = it)) },
            )
        }

        ConfigSection(title = "$TEXT_PATH_CONFIG (${config.targetPaths.size})") {
            AddRow(
                value = pathInput,
                label = hiddenPathText(R.string.hidden_path_path_label),
                placeholder = "/data/local/tmp/pathmask",
                onValueChange = onPathInputChange,
                onAdd = onAddPath,
            )
            SmartPathActions(
                currentValue = pathInput,
                onUsePath = onPathInputChange,
                onAddPath = onAddPathValue,
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
                label = hiddenPathText(R.string.hidden_path_package_or_uid),
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

        Spacer(modifier = Modifier.size(96.dp))
    }

    if (showAppPicker) {
        HiddenPathAppPickerDialog(
            selectedEntries = config.appPackages,
            onDismissRequest = { showAppPicker = false },
            onSelectUid = onAddAppUid,
        )
    }
}

@Composable
private fun VisibilityProbeSection(
    uidInput: String,
    pathInput: String,
    testing: Boolean,
    result: HiddenPathVisibilityResult?,
    hasCurrentConfig: Boolean,
    onUidChange: (String) -> Unit,
    onPathChange: (String) -> Unit,
    onUseCurrentConfig: () -> Unit,
    onTest: () -> Unit,
) {
    ConfigSection(
        title = TEXT_VISIBILITY_TEST,
        summary = TEXT_VISIBILITY_TEST_SUMMARY,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 380.dp
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisibilityProbeFields(
                        uidInput = uidInput,
                        pathInput = pathInput,
                        onUidChange = onUidChange,
                        onPathChange = onPathChange,
                        onTest = onTest,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(0.35f),
                        value = uidInput,
                        onValueChange = onUidChange,
                        label = { Text("UID") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(0.65f),
                        value = pathInput,
                        onValueChange = onPathChange,
                        label = { Text(hiddenPathText(R.string.hidden_path_path_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onTest() }),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = hasCurrentConfig && !testing,
                onClick = onUseCurrentConfig,
            ) {
                Text(TEXT_VISIBILITY_USE_CONFIG)
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = !testing && uidInput.isNotBlank() && pathInput.isNotBlank(),
                onClick = onTest,
            ) {
                if (testing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(TEXT_VISIBILITY_TEST_ACTION)
                }
            }
        }

        result?.let { probe ->
            HorizontalDivider()
            val resultText = when (probe.status) {
                "not_visible" -> hiddenPathText(R.string.hidden_path_probe_not_visible)
                "visible" -> hiddenPathText(R.string.hidden_path_probe_visible)
                "missing" -> hiddenPathText(R.string.hidden_path_probe_missing)
                else -> hiddenPathText(
                    R.string.hidden_path_probe_failed,
                    probe.error.ifBlank { hiddenPathText(R.string.hidden_path_unknown_error) },
                )
            }
            Text(
                text = resultText,
                style = MaterialTheme.typography.titleSmall,
                color = when (probe.status) {
                    "not_visible" -> MaterialTheme.colorScheme.primary
                    "visible", "probe_failed" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold,
            )
            StatusLine("UID", probe.uid.toString())
            StatusLine(hiddenPathText(R.string.hidden_path_probe_path), probe.path)
            StatusLine(
                "pathmask",
                if (probe.moduleLoaded) hiddenPathText(R.string.hidden_path_loaded) else hiddenPathText(R.string.hidden_path_not_loaded),
            )
            StatusLine(
                hiddenPathText(R.string.hidden_path_root_view),
                if (probe.rootExists) hiddenPathText(R.string.hidden_path_path_exists) else hiddenPathText(R.string.hidden_path_path_missing),
            )
            StatusLine("resolved_count", probe.resolvedCount.ifBlank { "-" })
        }
    }
}

@Composable
private fun VisibilityProbeFields(
    uidInput: String,
    pathInput: String,
    onUidChange: (String) -> Unit,
    onPathChange: (String) -> Unit,
    onTest: () -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = uidInput,
        onValueChange = onUidChange,
        label = { Text("Android UID") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
    )
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = pathInput,
        onValueChange = onPathChange,
        label = { Text(hiddenPathText(R.string.hidden_path_probe_path)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onTest() }),
    )
}

@Composable
private fun TemplateControls(
    templateName: String,
    templates: List<HiddenPathTemplate>,
    enabled: Boolean,
    onTemplateNameChange: (String) -> Unit,
    onSaveTemplate: () -> Unit,
    onApplyTemplate: (HiddenPathTemplate) -> Unit,
    onDeleteTemplate: (HiddenPathTemplate) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 360.dp
        val nameField: @Composable (Modifier) -> Unit = { modifier ->
            OutlinedTextField(
                modifier = modifier,
                value = templateName,
                onValueChange = onTemplateNameChange,
                label = { Text(TEXT_TEMPLATE_NAME) },
                placeholder = { Text(hiddenPathText(R.string.hidden_path_template_example)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (enabled) onSaveTemplate() }),
            )
        }
        val saveButton: @Composable (Modifier) -> Unit = { modifier ->
            FilledTonalButton(
                modifier = modifier,
                enabled = enabled,
                onClick = onSaveTemplate,
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(TEXT_SAVE_TEMPLATE)
            }
        }

        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nameField(Modifier.fillMaxWidth())
                saveButton(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                nameField(Modifier.weight(1f))
                saveButton(Modifier)
            }
        }
    }

    if (templates.isEmpty()) {
        EmptyTemplateState()
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        templates.forEachIndexed { index, template ->
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = hiddenPathText(
                            R.string.hidden_path_template_counts,
                            template.config.targetPaths.size,
                            template.config.appPackages.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    enabled = enabled,
                    onClick = { onApplyTemplate(template) },
                ) {
                    Text(TEXT_APPLY_TEMPLATE)
                }
                IconButton(
                    enabled = enabled,
                    onClick = { onDeleteTemplate(template) },
                ) {
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

@Composable
private fun EmptyTemplateState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
    ) {
        Text(
            modifier = Modifier.padding(12.dp),
            text = TEXT_EMPTY_TEMPLATE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HiddenPathStatusPanel(
    config: HiddenPathConfigState,
    unloadingActive: Boolean,
    onUnloadActive: () -> Unit,
) {
    val statusText = when {
        config.loaded -> TEXT_RUNNING
        config.targetPaths.isEmpty() -> TEXT_WAITING_CONFIG
        else -> TEXT_WAITING_LOAD
    }
    ConfigSection(title = hiddenPathText(R.string.hidden_path_current_status)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (config.loaded) hiddenPathText(R.string.hidden_path_loaded) else hiddenPathText(R.string.hidden_path_not_loaded),
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
                text = statusText,
                positive = config.loaded,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        StatusLine(
            hiddenPathText(R.string.hidden_path_load_status),
            if (config.loaded) hiddenPathText(R.string.hidden_path_loaded) else hiddenPathText(R.string.hidden_path_not_loaded),
        )
        StatusLine("KMI", config.currentKmi.ifBlank { "-" })
        StatusLine(hiddenPathText(R.string.hidden_path_resolved_paths), config.resolvedCount.ifBlank { "-" })
        if (!config.loaded) {
            config.blockReason()?.let { reason ->
                StatusLine(hiddenPathText(R.string.hidden_path_reason), reason)
            }
        }
        if (config.activeTargetPaths.isNotBlank()) {
            StatusLine(hiddenPathText(R.string.hidden_path_kernel_active_paths), config.activeTargetPaths)
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
        targetPaths.any { normalizeHiddenPath(it) == null } -> TEXT_INVALID_PATH
        !useAppScope && targetPaths.any(::isManagedRootPath) -> TEXT_MANAGED_PATH_GLOBAL_BLOCK
        useAppScope && appPackages.isEmpty() -> TEXT_NO_APP
        useAppScope && appPackages.any { normalizeAppEntry(it) == null } -> TEXT_INVALID_APP
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
            text = hiddenPathText(R.string.hidden_path_common_paths),
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
                label = { Text(hiddenPathText(R.string.hidden_path_paste_path)) },
                leadingIcon = {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
            val trimmedCurrent = currentValue.trim()
            if (trimmedCurrent.isNotBlank()) {
                AssistChip(
                    onClick = { onAddPath(trimmedCurrent) },
                    label = { Text(hiddenPathText(R.string.hidden_path_add_current_input)) },
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
                    text = displayConfigItem(item, emptyKind),
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
        hiddenPathText(R.string.hidden_path_help_builtin),
        hiddenPathText(R.string.hidden_path_help_absolute_paths),
        hiddenPathText(R.string.hidden_path_help_app_scope),
        hiddenPathText(R.string.hidden_path_help_managed_paths),
        hiddenPathText(R.string.hidden_path_help_apply),
        hiddenPathText(R.string.hidden_path_help_debug),
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
    selectedEntries: List<String>,
    onDismissRequest: () -> Unit,
    onSelectUid: (Int) -> Unit,
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf<List<HiddenPathAppCandidate>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    val selectedSet = remember(selectedEntries) { selectedEntries.toSet() }
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
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 360.dp, max = 560.dp)
                .imePadding()
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
                                val selected = app.uid.toString() in selectedSet || app.packageName in selectedSet
                                HiddenPathAppPickerRow(
                                    app = app,
                                    selected = selected,
                                    onClick = {
                                        if (!selected) {
                                            onSelectUid(app.uid)
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
                        text = hiddenPathText(R.string.hidden_path_added),
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
    return logs.ifBlank { hiddenPathText(R.string.hidden_path_logs_empty) }
}

private fun displayConfigItem(item: String, kind: EmptyListKind): String {
    return if (kind == EmptyListKind.App && item.all(Char::isDigit)) {
        "UID $item"
    } else {
        item
    }
}

private fun sanitizeHiddenPathConfig(config: HiddenPathConfigState): HiddenPathConfigState {
    return config.copy(
        targetPaths = config.targetPaths.mapNotNull(::normalizeHiddenPath).distinct(),
        appPackages = config.appPackages.mapNotNull(::normalizeAppEntry).distinct(),
    )
}

private fun normalizeHiddenPath(rawPath: String): String? {
    val path = rawPath.trim().trimEnd('/').ifEmpty { rawPath.trim() }
    val hasTraversalSegment = path.split('/').any { segment -> segment == "." || segment == ".." }
    if (
        path.startsWith("/") && path != "/" && !hasTraversalSegment &&
        !path.contains(",") &&
        !path.contains('\u0000') &&
        !path.contains('"') &&
        !path.contains('\\') &&
        path.none { it.isISOControl() }
    ) {
        return path
    }
    return null
}

private fun isManagedRootPath(path: String): Boolean {
    val normalized = normalizeHiddenPath(path) ?: return false
    return MANAGED_ROOT_PATHS.any { managed ->
        normalized == managed || normalized.startsWith("$managed/")
    }
}

private fun normalizeAppEntry(rawEntry: String): String? {
    val entry = rawEntry.trim()
    if (entry.isBlank()) {
        return null
    }
    if (entry.all(Char::isDigit)) {
        return entry.takeIf { it.toLongOrNull() != null }
    }
    return entry.takeIf { value ->
        value.all { char ->
            char in 'a'..'z' ||
                char in 'A'..'Z' ||
                char in '0'..'9' ||
                char == '.' ||
                char == '_' ||
                char == '-' ||
                char == ':'
        }
    }
}

private fun loadHiddenPathTemplates(context: Context): List<HiddenPathTemplate> {
    val prefs = context.getSharedPreferences(PREF_HIDDEN_PATH_TEMPLATES, Context.MODE_PRIVATE)
    val text = prefs.getString(PREF_KEY_TEMPLATES, null) ?: return emptyList()
    return runCatching {
        val array = JSONArray(text)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = item.optString("name").trim().takeIf(String::isNotEmpty) ?: continue
                val configObject = item.optJSONObject("config") ?: continue
                val config = sanitizeHiddenPathConfig(parseHiddenPathConfigJson(configObject.toString()))
                add(HiddenPathTemplate(name, config))
            }
        }.sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())
}

private fun saveHiddenPathTemplates(context: Context, templates: List<HiddenPathTemplate>): Boolean {
    val array = JSONArray()
    templates.forEach { template ->
        array.put(
            JSONObject()
                .put("name", template.name)
                .put("config", JSONObject(sanitizeHiddenPathConfig(template.config).toConfigJson()))
        )
    }
    return context
        .getSharedPreferences(PREF_HIDDEN_PATH_TEMPLATES, Context.MODE_PRIVATE)
        .edit()
        .putString(PREF_KEY_TEMPLATES, array.toString())
        .commit()
}
