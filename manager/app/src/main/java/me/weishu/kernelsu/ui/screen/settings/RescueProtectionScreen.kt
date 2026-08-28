package me.weishu.kernelsu.ui.screen.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.ApkeMetricGrid
import me.weishu.kernelsu.ui.component.ApkeMetricItem
import me.weishu.kernelsu.ui.component.ApkeSecondaryScaffold
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.util.RescueConfigState
import me.weishu.kernelsu.ui.util.RescueImageState
import me.weishu.kernelsu.ui.util.RescueStatus
import me.weishu.kernelsu.ui.util.enableRescueModule
import me.weishu.kernelsu.ui.util.getRescueDiagnostics
import me.weishu.kernelsu.ui.util.getRescueLogs
import me.weishu.kernelsu.ui.util.getRescueStatus
import me.weishu.kernelsu.ui.util.importRescueImage
import me.weishu.kernelsu.ui.util.runRescueCommand
import me.weishu.kernelsu.ui.util.saveRescueConfig
import me.weishu.kernelsu.ui.util.testRescueEnvironment
import me.weishu.kernelsu.ui.util.verifyRescueBackups
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun rescueText(@StringRes id: Int, vararg args: Any): String = ksuApp.getString(id, *args)

private const val RESCUE_IMPORT_TAG = "RescueProtection"
private const val RESCUE_IMPORT_CACHE_MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L

private val rescueTabs: List<String>
    get() = listOf(
        rescueText(R.string.rescue_tab_protection),
        rescueText(R.string.rescue_tab_config),
        rescueText(R.string.rescue_tab_diagnostics),
    )

@Composable
fun RescueProtectionScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var status by remember { mutableStateOf(RescueStatus()) }
    var logs by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showBackupConfirm by remember { mutableStateOf(false) }
    var showImportConfirmFor by remember { mutableStateOf<RescueImageState?>(null) }
    var pendingImportPartition by remember { mutableStateOf<String?>(null) }
    var pendingImportExpectedSize by remember { mutableLongStateOf(0L) }
    var pendingImportForce by remember { mutableStateOf(false) }
    var testReport by remember { mutableStateOf("") }
    var diagnosticExportText by remember { mutableStateOf("") }
    var includeDtbo by remember { mutableStateOf(false) }
    var includeVbmeta by remember { mutableStateOf(false) }
    var backupOtherSlot by remember { mutableStateOf(false) }
    var allowDangerousAutoRestore by remember { mutableStateOf(false) }
    var bootPath by remember { mutableStateOf("") }
    var vendorBootPath by remember { mutableStateOf("") }
    var initBootPath by remember { mutableStateOf("") }
    var dtboPath by remember { mutableStateOf("") }
    var vbmetaPath by remember { mutableStateOf("") }

    val diagnosticExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(diagnosticExportText.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open diagnostic output")
                }.isSuccess
            }
            Toast.makeText(
                context,
                rescueText(
                    if (ok) R.string.rescue_diagnostics_exported
                    else R.string.rescue_diagnostics_export_failed
                ),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val imageImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val partition = pendingImportPartition
        val expectedSize = pendingImportExpectedSize
        val force = pendingImportForce
        pendingImportPartition = null
        pendingImportExpectedSize = 0L
        pendingImportForce = false
        if (uri == null || partition == null) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            busy = true
            var file: File? = null
            val ok = try {
                file = copyRescueImageToCache(context, uri, partition, expectedSize)
                importRescueImage(partition, file.absolutePath, force)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.w(RESCUE_IMPORT_TAG, "Failed to stage rescue image", error)
                false
            } finally {
                file?.delete()
                status = getRescueStatus()
                logs = getRescueLogs()
                busy = false
            }
            Toast.makeText(
                context,
                if (ok) rescueText(R.string.rescue_image_imported) else rescueText(R.string.rescue_image_import_failed),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun loadConfig(current: RescueStatus) {
        includeDtbo = current.config.includeDtbo
        includeVbmeta = current.config.includeVbmeta
        backupOtherSlot = current.config.backupOtherSlot
        allowDangerousAutoRestore = current.config.allowDangerousAutoRestore
        bootPath = current.config.customPartitions["boot"].orEmpty()
        vendorBootPath = current.config.customPartitions["vendor_boot"].orEmpty()
        initBootPath = current.config.customPartitions["init_boot"].orEmpty()
        dtboPath = current.config.customPartitions["dtbo"].orEmpty()
        vbmetaPath = current.config.customPartitions["vbmeta"].orEmpty()
    }

    fun refresh(syncConfig: Boolean = false) {
        scope.launch {
            loading = true
            try {
                val current = getRescueStatus()
                status = current
                logs = getRescueLogs()
                if (syncConfig && current.available) {
                    loadConfig(current)
                }
            } finally {
                loading = false
            }
        }
    }

    fun runAction(command: String, success: String, fail: String, timeoutMultiplier: Long = 6) {
        scope.launch {
            busy = true
            val result = try {
                runRescueCommand(command, timeoutMultiplier)
            } finally {
                status = getRescueStatus()
                logs = getRescueLogs()
                busy = false
            }
            Toast.makeText(
                context,
                if (result.success) success else result.errorMessage.ifBlank { fail },
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun verifyBackups() {
        scope.launch {
            busy = true
            val report = try {
                verifyRescueBackups().also { testReport = it.text.ifBlank { it.reason } }
            } finally {
                status = getRescueStatus()
                logs = getRescueLogs()
                busy = false
            }
            Toast.makeText(
                context,
                if (report.ok) {
                    rescueText(R.string.rescue_verification_passed)
                } else {
                    report.reason.ifBlank { rescueText(R.string.rescue_verification_failed) }
                },
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun refreshAndEnable() {
        runAction(
            command = "refresh-enable",
            success = rescueText(R.string.rescue_refresh_enable_success),
            fail = rescueText(R.string.rescue_refresh_enable_failed),
            timeoutMultiplier = 90,
        )
    }

    fun reenableModule(id: String) {
        scope.launch {
            busy = true
            val result = try {
                enableRescueModule(id)
            } finally {
                status = getRescueStatus()
                logs = getRescueLogs()
                busy = false
            }
            Toast.makeText(
                context,
                if (result.success) {
                    rescueText(R.string.rescue_module_enabled)
                } else {
                    result.errorMessage.ifBlank { rescueText(R.string.rescue_operation_failed) }
                },
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun exportDiagnostics() {
        scope.launch {
            diagnosticExportText = getRescueDiagnostics().ifBlank {
                logs.ifBlank { status.log }
            }
            diagnosticExportLauncher.launch("apkesu-rescue-diagnostics.txt")
        }
    }

    fun saveConfig() {
        scope.launch {
            busy = true
            val result = try {
                val custom = mapOf(
                    "boot" to bootPath,
                    "vendor_boot" to vendorBootPath,
                    "init_boot" to initBootPath,
                    "dtbo" to dtboPath,
                    "vbmeta" to vbmetaPath,
                ).filterValues(String::isNotBlank)
                saveRescueConfig(
                    RescueConfigState(
                        includeDtbo = includeDtbo,
                        includeVbmeta = includeVbmeta,
                        backupOtherSlot = backupOtherSlot,
                        allowDangerousAutoRestore = allowDangerousAutoRestore,
                        customPartitions = custom,
                    )
                )
            } finally {
                status = getRescueStatus()
                logs = getRescueLogs()
                busy = false
            }
            Toast.makeText(
                context,
                if (result.success) {
                    rescueText(R.string.rescue_config_saved)
                } else {
                    result.errorMessage.ifBlank { rescueText(R.string.rescue_config_save_failed) }
                },
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun testEnvironment() {
        scope.launch {
            busy = true
            val report = try {
                testRescueEnvironment().also { result ->
                    testReport = result.text.ifBlank {
                        if (result.ok) {
                            rescueText(R.string.rescue_test_passed)
                        } else {
                            rescueText(R.string.rescue_test_failed_reason, result.reason)
                        }
                    }
                }
            } finally {
                status = getRescueStatus()
                logs = getRescueLogs()
                busy = false
            }
            Toast.makeText(
                context,
                if (report.ok) rescueText(R.string.rescue_test_passed) else rescueText(R.string.rescue_test_failed),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun launchImageImport(partition: String, expectedSize: Long, force: Boolean) {
        pendingImportPartition = partition
        pendingImportExpectedSize = expectedSize
        pendingImportForce = force
        imageImportLauncher.launch(arrayOf("*/*"))
    }

    LaunchedEffect(Unit) {
        refresh(syncConfig = true)
    }

    val pageContainerColor = Color.Transparent
    val barContainerColor = immersiveSurfaceColor(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    )
    val tabs = rescueTabs
    val activeTab = selectedTab.coerceIn(tabs.indices)
    ApkeSecondaryScaffold(
        title = rescueText(R.string.rescue_protection),
        onBack = { navigator.pop() },
        containerColor = pageContainerColor,
    ) { innerPadding, _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PrimaryTabRow(
                selectedTabIndex = activeTab,
                containerColor = barContainerColor,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                when (activeTab) {
                    0 -> RescueHomePage(
                        status = status,
                        busy = busy,
                        onBackup = {
                            val backupExists = status.manifestCreatedAt.isNotBlank() ||
                                status.images.any(RescueImageState::exists)
                            if (!backupExists) {
                                runAction(
                                    "backup",
                                    rescueText(R.string.rescue_backup_completed),
                                    rescueText(R.string.rescue_backup_failed),
                                    timeoutMultiplier = 30,
                                )
                            } else {
                                showBackupConfirm = true
                            }
                        },
                        onTest = ::testEnvironment,
                        onVerify = ::verifyBackups,
                        onRefreshEnable = ::refreshAndEnable,
                        onToggle = { enabled ->
                            runAction(
                                if (enabled) "enable" else "disable",
                                if (enabled) {
                                    rescueText(R.string.rescue_protection_enabled)
                                } else {
                                    rescueText(R.string.rescue_protection_disabled)
                                },
                                rescueText(R.string.rescue_operation_failed),
                            )
                        },
                        onRestore = { showRestoreConfirm = true },
                    )

                    1 -> {
                        RescueConfigCard(
                            busy = busy,
                            includeDtbo = includeDtbo,
                            includeVbmeta = includeVbmeta,
                            backupOtherSlot = backupOtherSlot,
                            allowDangerousAutoRestore = allowDangerousAutoRestore,
                            bootPath = bootPath,
                            vendorBootPath = vendorBootPath,
                            initBootPath = initBootPath,
                            dtboPath = dtboPath,
                            vbmetaPath = vbmetaPath,
                            onIncludeDtboChange = { includeDtbo = it },
                            onIncludeVbmetaChange = { includeVbmeta = it },
                            onBackupOtherSlotChange = { backupOtherSlot = it },
                            onAllowDangerousAutoRestoreChange = { allowDangerousAutoRestore = it },
                            onBootPathChange = { bootPath = it },
                            onVendorBootPathChange = { vendorBootPath = it },
                            onInitBootPathChange = { initBootPath = it },
                            onDtboPathChange = { dtboPath = it },
                            onVbmetaPathChange = { vbmetaPath = it },
                            onSave = ::saveConfig,
                        )
                        RescueImagesCard(
                            status = status,
                            onImport = { image ->
                                if (image.exists) {
                                    showImportConfirmFor = image
                                } else {
                                    launchImageImport(image.name, image.partitionSize, force = false)
                                }
                            },
                        )
                    }

                    else -> RescueDiagnosticsPage(
                        status = status,
                        logs = logs,
                        busy = busy,
                        testReport = testReport,
                        onRefresh = { refresh(syncConfig = true) },
                        onExport = ::exportDiagnostics,
                        onEnableModule = ::reenableModule,
                        onClearLogs = {
                            runAction(
                                "clear-logs",
                                rescueText(R.string.rescue_logs_cleared),
                                rescueText(R.string.rescue_logs_clear_failed),
                            )
                        },
                    )
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
            title = { Text(rescueText(R.string.rescue_restore_confirm_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(rescueText(R.string.rescue_restore_confirm_message))
                    StatusLine(
                        rescueText(R.string.rescue_restore_partitions),
                        status.images
                            .filter { it.restore && it.exists }
                            .joinToString { it.label.ifBlank { it.name } }
                            .ifBlank { "-" },
                    )
                    StatusLine(
                        rescueText(R.string.rescue_manifest_slot),
                        status.manifestSlot.ifBlank { "-" },
                    )
                    StatusLine(
                        rescueText(R.string.rescue_backup_age),
                        formatBackupAge(status.manifestCreatedAt),
                    )
                    StatusLine(
                        rescueText(R.string.rescue_manifest_size),
                        formatSize(status.manifestTotalSize),
                    )
                    StatusLine(
                        rescueText(R.string.rescue_verification),
                        rescueText(
                            if (status.verified && status.images.all { !it.exists || (it.sizeOk && it.sha256Ok) }) {
                                R.string.rescue_verification_passed
                            } else {
                                R.string.rescue_verification_failed
                            }
                        ),
                    )
                    if (status.manifestFingerprint.isNotBlank()) {
                        Text(
                            text = "${rescueText(R.string.rescue_manifest_fingerprint)}\n${status.manifestFingerprint}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        runAction(
                            "restore",
                            rescueText(R.string.rescue_restore_started),
                            rescueText(R.string.rescue_restore_failed),
                            timeoutMultiplier = 20,
                        )
                    },
                ) {
                    Text(rescueText(R.string.rescue_restore_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text(rescueText(R.string.rescue_cancel))
                }
            },
        )
    }

    if (showBackupConfirm) {
        AlertDialog(
            onDismissRequest = { showBackupConfirm = false },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
            title = { Text(rescueText(R.string.rescue_backup_overwrite_title)) },
            text = { Text(rescueText(R.string.rescue_backup_overwrite_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showBackupConfirm = false
                        runAction(
                            "backup --force",
                            rescueText(R.string.rescue_backup_overwritten),
                            rescueText(R.string.rescue_backup_overwrite_failed),
                            timeoutMultiplier = 30,
                        )
                    },
                ) {
                    Text(rescueText(R.string.rescue_backup_overwrite_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupConfirm = false }) {
                    Text(rescueText(R.string.rescue_cancel))
                }
            },
        )
    }

    showImportConfirmFor?.let { image ->
        AlertDialog(
            onDismissRequest = { showImportConfirmFor = null },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
            title = { Text(rescueText(R.string.rescue_image_overwrite_title, image.name)) },
            text = {
                Text(rescueText(R.string.rescue_image_overwrite_message, image.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirmFor = null
                        launchImageImport(image.name, image.partitionSize, force = true)
                    },
                ) {
                    Text(rescueText(R.string.rescue_image_overwrite_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmFor = null }) {
                    Text(rescueText(R.string.rescue_cancel))
                }
            },
        )
    }
}

@Composable
private fun RescueHomePage(
    status: RescueStatus,
    busy: Boolean,
    onBackup: () -> Unit,
    onTest: () -> Unit,
    onVerify: () -> Unit,
    onRefreshEnable: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onRestore: () -> Unit,
) {
    RescueStatusCard(status = status)
    if (status.configChangedProtectionDisabled) {
        RescueCard(title = rescueText(R.string.rescue_config_changed_title)) {
            Text(
                text = rescueText(R.string.rescue_config_changed_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                onClick = onRefreshEnable,
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(rescueText(R.string.rescue_refresh_enable_action))
            }
        }
    }
    RescueGuidedFlowCard(
        status = status,
        busy = busy,
        onBackup = onBackup,
        onTest = onTest,
        onVerify = onVerify,
        onToggle = onToggle,
        onRestore = onRestore,
    )
}

@Composable
private fun RescueStatusCard(status: RescueStatus) {
    RescueCard(title = rescueText(R.string.rescue_current_status)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rescuePhaseLabel(status),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = status.readyReason.ifBlank {
                        status.statusError.ifBlank { rescueText(R.string.rescue_status_ready_summary) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = rescuePhaseColor(status).copy(alpha = 0.14f),
                contentColor = rescuePhaseColor(status),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    text = rescuePhaseLabel(status),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ApkeMetricGrid(
            items = listOf(
                ApkeMetricItem(
                    label = rescueText(R.string.rescue_status_current_slot),
                    value = status.currentSlot.ifBlank { "-" },
                ),
                ApkeMetricItem(
                    label = rescueText(R.string.rescue_status_backup_complete),
                    value = "${status.images.count { it.exists && it.sizeOk }}/${status.images.count { !it.otherSlot || it.restore }}",
                ),
                ApkeMetricItem(
                    label = rescueText(R.string.rescue_verification),
                    value = rescueText(
                        if (status.verified) R.string.rescue_value_yes else R.string.rescue_value_no
                    ),
                ),
            ),
        )
        if (status.restoreInterrupted) {
            Text(
                text = status.restoreTransactionError.ifBlank {
                    status.restoreTransaction?.errorMessage.orEmpty().ifBlank {
                        rescueText(R.string.rescue_restore_interrupted)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun rescuePhaseLabel(status: RescueStatus): String = rescueText(
    when {
        !status.available -> R.string.rescue_phase_unconfigured
        status.restoreInterrupted || status.phase == "restore_error" -> R.string.rescue_phase_restore_error
        status.images.any { it.exists && (!it.sizeOk || !it.sha256Ok) } -> R.string.rescue_phase_verification_failed
        status.configChangedProtectionDisabled || status.phase == "config_changed" ->
            R.string.rescue_phase_paused
        status.phase == "needs_backup" -> R.string.rescue_phase_needs_backup
        status.phase == "needs_verification" -> R.string.rescue_phase_needs_verification
        status.phase == "protected" || status.enabled -> R.string.rescue_phase_protected
        status.phase == "ready_to_enable" -> R.string.rescue_phase_ready
        else -> R.string.rescue_phase_unconfigured
    }
)

@Composable
private fun rescuePhaseColor(status: RescueStatus): Color = when {
    status.restoreInterrupted || status.phase == "restore_error" ||
        status.images.any { it.exists && (!it.sizeOk || !it.sha256Ok) } ->
        MaterialTheme.colorScheme.error
    status.phase == "protected" || status.enabled -> MaterialTheme.colorScheme.primary
    status.configChangedProtectionDisabled || status.phase == "config_changed" ->
        MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun RescueImagesCard(
    status: RescueStatus,
    onImport: (RescueImageState) -> Unit,
) {
    RescueCard(title = rescueText(R.string.rescue_image_backups)) {
        if (!status.available) {
            Text(
                text = status.statusError.ifBlank { rescueText(R.string.rescue_value_unknown) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            return@RescueCard
        }
        status.images.ifEmpty {
            listOf(RescueImageState(name = "boot", required = true))
        }.forEachIndexed { index, image ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            RescueImageRow(image, onImport)
        }
    }
}

@Composable
private fun RescueImageRow(
    image: RescueImageState,
    onImport: (RescueImageState) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = buildString {
                    append(image.name.ifBlank { rescueText(R.string.rescue_value_unknown) })
                    if (image.required) append(" *")
                    if (image.otherSlot) append(" / ${rescueText(R.string.rescue_image_other_slot)}")
                    if (image.custom) append(" / ${rescueText(R.string.rescue_image_manual)}")
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            val ok = image.exists && image.sizeOk && image.sha256Ok
            Text(
                text = when {
                    ok -> rescueText(R.string.rescue_image_status_ok)
                    image.partition.isBlank() -> rescueText(R.string.rescue_image_status_no_partition)
                    !image.exists -> rescueText(R.string.rescue_value_not_backed_up)
                    !image.sizeOk -> rescueText(R.string.rescue_image_status_size_error)
                    !image.sha256Ok -> rescueText(R.string.rescue_image_status_checksum_failed)
                    else -> rescueText(R.string.rescue_image_status_error)
                },
                color = when {
                    ok -> MaterialTheme.colorScheme.primary
                    image.partition.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.labelMedium,
            )
            if (image.restore && !image.otherSlot) {
                IconButton(
                    enabled = image.partition.isNotBlank() && image.partitionSize > 0,
                    onClick = { onImport(image) },
                ) {
                    Icon(
                        Icons.Rounded.FileUpload,
                        contentDescription = if (image.exists) {
                            rescueText(R.string.rescue_import_image_overwrite)
                        } else {
                            rescueText(R.string.rescue_import_image_backup)
                        },
                    )
                }
            }
        }
        Text(
            text = image.partition.ifBlank { rescueText(R.string.rescue_partition_not_detected) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (image.exists) {
            Text(
                text = rescueText(
                    R.string.rescue_image_details,
                    formatSize(image.size),
                    formatSize(image.partitionSize),
                    image.sha256.take(12),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RescueDiagnosticsPage(
    status: RescueStatus,
    logs: String,
    busy: Boolean,
    testReport: String,
    onRefresh: () -> Unit,
    onExport: () -> Unit,
    onEnableModule: (String) -> Unit,
    onClearLogs: () -> Unit,
) {
    RescueCard(title = rescueText(R.string.rescue_diagnostic_status)) {
        StatusLine(
            rescueText(R.string.rescue_status_protection),
            rescuePhaseLabel(status),
        )
        StatusLine(
            rescueText(R.string.rescue_status_current_slot),
            status.currentSlot.ifBlank { rescueText(R.string.rescue_value_no_slot) },
        )
        StatusLine(
            rescueText(R.string.rescue_status_boot_mode),
            status.bootMode.ifBlank { rescueText(R.string.rescue_value_not_detected) },
        )
        StatusLine(
            rescueText(R.string.rescue_status_device),
            status.device.ifBlank { rescueText(R.string.rescue_value_not_detected) },
        )
        StatusLine(
            rescueText(R.string.rescue_status_backup_time),
            status.manifestCreatedAt.ifBlank { rescueText(R.string.rescue_value_not_backed_up) },
        )
        StatusLine(
            rescueText(R.string.rescue_manifest_slot),
            status.manifestSlot.ifBlank { "-" },
        )
        StatusLine(
            rescueText(R.string.rescue_manifest_device),
            status.manifestDevice.ifBlank { "-" },
        )
        StatusLine(
            rescueText(R.string.rescue_manifest_size),
            formatSize(status.manifestTotalSize),
        )
        if (status.manifestFingerprint.isNotBlank()) {
            StatusLine(
                rescueText(R.string.rescue_manifest_fingerprint),
                status.manifestFingerprint,
            )
        }
        StatusLine(
            rescueText(R.string.rescue_status_pending_boot),
            rescueText(
                if (status.pendingBoot) R.string.rescue_value_yes else R.string.rescue_value_no
            ),
        )
        StatusLine(rescueText(R.string.rescue_status_failure_count), status.bootCount.toString())
        StatusLine(
            rescueText(R.string.rescue_status_restore_attempts),
            status.autoRestoreAttempts.toString(),
        )
    }

    status.restoreTransaction?.let { transaction ->
        RescueCard(title = rescueText(R.string.rescue_restore_transaction)) {
            StatusLine(rescueText(R.string.rescue_transaction_id), transaction.id.ifBlank { "-" })
            StatusLine(rescueText(R.string.rescue_transaction_phase), transaction.phase.ifBlank { "-" })
            StatusLine(rescueText(R.string.rescue_transaction_updated), transaction.updatedAt.ifBlank { "-" })
            transaction.entries.forEach { entry ->
                StatusLine(
                    entry.label.ifBlank { entry.name },
                    "${entry.status} · ${formatSize(entry.expectedSize)}",
                )
            }
            val error = transaction.errorMessage.ifBlank { status.restoreTransactionError }
            if (error.isNotBlank()) {
                Text(
                    text = listOf(transaction.errorCode, error)
                        .filter(String::isNotBlank)
                        .joinToString(": "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (
        status.rescueDisabledModules.isNotEmpty() ||
        status.skipModulesOnce ||
        status.skipModulesThisBoot
    ) {
        RescueCard(title = rescueText(R.string.rescue_disabled_modules)) {
            Text(
                text = rescueText(R.string.rescue_disabled_modules_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            status.rescueDisabledModules.forEachIndexed { index, module ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            module.name.ifBlank { module.id },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            listOf(module.id, module.version)
                                .filter(String::isNotBlank)
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        enabled = !busy && module.installed && module.disabled,
                        onClick = { onEnableModule(module.id) },
                    ) {
                        Text(rescueText(R.string.rescue_enable_module_action))
                    }
                }
            }
        }
    }

    if (testReport.isNotBlank()) {
        RescueTextCard(rescueText(R.string.rescue_test_report), testReport)
    }
    RescueTextCard(
        rescueText(R.string.rescue_logs),
        logs.ifBlank { status.log }.ifBlank { rescueText(R.string.rescue_logs_empty) },
    )
    RescueCard(title = rescueText(R.string.rescue_diagnostic_actions)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                modifier = Modifier.weight(1f),
                enabled = !busy,
                onClick = onRefresh,
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(rescueText(R.string.rescue_refresh))
            }
            FilledTonalButton(
                modifier = Modifier.weight(1f),
                enabled = !busy,
                onClick = onExport,
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(rescueText(R.string.rescue_export_diagnostics))
            }
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            onClick = onClearLogs,
        ) {
            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(rescueText(R.string.rescue_clear_logs))
        }
    }
    RescueHelpCard()
}

@Composable
private fun RescueConfigCard(
    busy: Boolean,
    includeDtbo: Boolean,
    includeVbmeta: Boolean,
    backupOtherSlot: Boolean,
    allowDangerousAutoRestore: Boolean,
    bootPath: String,
    vendorBootPath: String,
    initBootPath: String,
    dtboPath: String,
    vbmetaPath: String,
    onIncludeDtboChange: (Boolean) -> Unit,
    onIncludeVbmetaChange: (Boolean) -> Unit,
    onBackupOtherSlotChange: (Boolean) -> Unit,
    onAllowDangerousAutoRestoreChange: (Boolean) -> Unit,
    onBootPathChange: (String) -> Unit,
    onVendorBootPathChange: (String) -> Unit,
    onInitBootPathChange: (String) -> Unit,
    onDtboPathChange: (String) -> Unit,
    onVbmetaPathChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    RescueCard(title = rescueText(R.string.rescue_tab_config)) {
        SwitchLine(
            rescueText(R.string.rescue_dual_slot_backup),
            rescueText(R.string.rescue_dual_slot_backup_summary),
            backupOtherSlot,
            onBackupOtherSlotChange,
        )
        SwitchLine(
            rescueText(R.string.rescue_include_dtbo),
            rescueText(R.string.rescue_include_dtbo_summary),
            includeDtbo,
            onIncludeDtboChange,
        )
        SwitchLine(
            rescueText(R.string.rescue_include_vbmeta),
            rescueText(R.string.rescue_include_vbmeta_summary),
            includeVbmeta,
            onIncludeVbmetaChange,
        )
        if (includeDtbo || includeVbmeta) {
            SwitchLine(
                rescueText(R.string.rescue_allow_dangerous_restore),
                rescueText(R.string.rescue_allow_dangerous_restore_summary),
                allowDangerousAutoRestore,
                onAllowDangerousAutoRestoreChange,
            )
        }
        PartitionPathField(rescueText(R.string.rescue_partition_path, "boot"), bootPath, onBootPathChange)
        PartitionPathField(rescueText(R.string.rescue_partition_path, "vendor_boot"), vendorBootPath, onVendorBootPathChange)
        PartitionPathField(rescueText(R.string.rescue_partition_path, "init_boot"), initBootPath, onInitBootPathChange)
        if (includeDtbo) {
            PartitionPathField(rescueText(R.string.rescue_partition_path, "dtbo"), dtboPath, onDtboPathChange)
        }
        if (includeVbmeta) {
            PartitionPathField(rescueText(R.string.rescue_partition_path, "vbmeta"), vbmetaPath, onVbmetaPathChange)
        }
        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            onClick = onSave,
        ) {
            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(rescueText(R.string.rescue_save_config))
        }
    }
}

@Composable
private fun RescueGuidedFlowCard(
    status: RescueStatus,
    busy: Boolean,
    onBackup: () -> Unit,
    onTest: () -> Unit,
    onVerify: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onRestore: () -> Unit,
) {
    RescueCard(title = rescueText(R.string.rescue_guided_flow)) {
        RescueFlowStep(
            index = 1,
            title = rescueText(R.string.rescue_check_environment),
            summary = rescueText(R.string.rescue_flow_environment_summary),
            completed = status.environmentChecked,
            enabled = status.available && !busy,
            action = rescueText(R.string.rescue_flow_check_action),
            onClick = onTest,
        )
        RescueFlowStep(
            index = 2,
            title = rescueText(R.string.rescue_backup_current_images),
            summary = rescueText(R.string.rescue_flow_backup_summary),
            completed = status.ready,
            enabled = status.available && status.environmentChecked && !busy,
            action = rescueText(R.string.rescue_flow_backup_action),
            onClick = onBackup,
        )
        RescueFlowStep(
            index = 3,
            title = rescueText(R.string.rescue_full_verification),
            summary = rescueText(R.string.rescue_flow_verify_summary),
            completed = status.verified,
            enabled = status.ready && !busy,
            action = rescueText(R.string.rescue_flow_verify_action),
            onClick = onVerify,
        )
        RescueFlowStep(
            index = 4,
            title = rescueText(R.string.rescue_enable_protection),
            summary = rescueText(R.string.rescue_flow_enable_summary),
            completed = status.enabled,
            enabled = status.verified && !busy,
            action = rescueText(
                if (status.enabled) R.string.rescue_disable_action
                else R.string.rescue_enable_action
            ),
            onClick = { onToggle(!status.enabled) },
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = status.available && status.ready && status.verified && !busy,
            onClick = onRestore,
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(rescueText(R.string.rescue_restore_preserve_data))
        }
    }
}

@Composable
private fun RescueFlowStep(
    index: Int,
    title: String,
    summary: String,
    completed: Boolean,
    enabled: Boolean,
    action: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (completed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (completed) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            if (completed) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp).size(18.dp),
                )
            } else {
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    text = index.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                enabled = enabled,
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                Text(action)
            }
        }
    }
}

@Composable
private fun RescueHelpCard() {
    RescueCard(title = rescueText(R.string.rescue_steps_title)) {
        StepLine(
            1,
            rescueText(R.string.rescue_step_config_title),
            rescueText(R.string.rescue_step_config_summary),
        )
        StepLine(
            2,
            rescueText(R.string.rescue_step_check_title),
            rescueText(R.string.rescue_step_check_summary),
        )
        StepLine(
            3,
            rescueText(R.string.rescue_step_save_title),
            rescueText(R.string.rescue_step_save_summary),
        )
        StepLine(
            4,
            rescueText(R.string.rescue_step_backup_title),
            rescueText(R.string.rescue_step_backup_summary),
        )
        StepLine(
            5,
            rescueText(R.string.rescue_step_enable_title),
            rescueText(R.string.rescue_step_enable_summary),
        )
        StepLine(
            6,
            rescueText(R.string.rescue_step_flash_title),
            rescueText(R.string.rescue_step_flash_summary),
        )
        StepLine(
            7,
            rescueText(R.string.rescue_step_auto_title),
            rescueText(R.string.rescue_step_auto_summary),
        )
    }

    RescueCard(title = rescueText(R.string.rescue_auto_restore_title)) {
        HelpLine(rescueText(R.string.rescue_auto_restore_pending))
        HelpLine(rescueText(R.string.rescue_auto_restore_failures))
        HelpLine(rescueText(R.string.rescue_auto_restore_pending_only))
        HelpLine(rescueText(R.string.rescue_auto_restore_pstore))
        HelpLine(rescueText(R.string.rescue_auto_restore_recovery))
        HelpLine(rescueText(R.string.rescue_auto_restore_limit))
        HelpLine(rescueText(R.string.rescue_auto_restore_preserves_data))
        HelpLine(rescueText(R.string.rescue_auto_restore_slots))
        HelpLine(rescueText(R.string.rescue_auto_restore_dangerous))
        HelpLine(rescueText(R.string.rescue_auto_restore_hard_brick))
    }
}

@Composable
private fun RescueTextCard(title: String, text: String) {
    RescueCard(title = title) {
        SelectionContainer {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchLine(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val switchStyle = LocalSwitchStyle.current
        if (switchStyle == SwitchStyle.Original) {
            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        } else {
            StyledSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                style = switchStyle,
            )
        }
    }
}

@Composable
private fun PartitionPathField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text("/dev/block/by-name/boot_a") },
    )
}

@Composable
private fun RescueCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .uiDecoratedCard(shape = shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
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
            modifier = Modifier.weight(0.36f),
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.weight(0.64f),
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HelpLine(text: String) {
    Text(
        text = "- $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StepLine(index: Int, title: String, summary: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                text = index.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatSize(size: Long): String {
    if (size <= 0) return "-"
    val mib = size / 1024.0 / 1024.0
    return String.format(Locale.getDefault(), "%.1f MiB", mib)
}

private fun formatBackupAge(createdAt: String, nowMillis: Long = System.currentTimeMillis()): String {
    if (createdAt.isBlank()) return rescueText(R.string.rescue_value_not_backed_up)
    val created = runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
            isLenient = false
        }.parse(createdAt)
    }.getOrNull() ?: return createdAt
    val ageMinutes = ((nowMillis - created.time).coerceAtLeast(0L) / 60_000L)
    return when {
        ageMinutes < 1 -> rescueText(R.string.rescue_backup_age_now)
        ageMinutes < 60 -> rescueText(R.string.rescue_backup_age_minutes, ageMinutes)
        ageMinutes < 24 * 60 -> rescueText(R.string.rescue_backup_age_hours, ageMinutes / 60)
        else -> rescueText(R.string.rescue_backup_age_days, ageMinutes / (24 * 60))
    }
}

private suspend fun copyRescueImageToCache(
    context: Context,
    uri: Uri,
    partition: String,
    expectedSize: Long,
): File = withContext(Dispatchers.IO) {
    require(expectedSize > 0) { "Cannot determine target partition size" }
    val dir = File(context.cacheDir, "rescue-import")
    check(dir.isDirectory || dir.mkdirs()) { "Cannot create rescue import cache" }
    val now = System.currentTimeMillis()
    dir.listFiles()?.forEach { cached ->
        if (now - cached.lastModified() > RESCUE_IMPORT_CACHE_MAX_AGE_MILLIS) {
            cached.delete()
        }
    }

    val safePartition = partition.replace(Regex("[^A-Za-z0-9_]"), "_")
    val file = File(dir, "${safePartition}_${System.nanoTime()}.img")
    val temporary = File(dir, ".${file.name}.part")
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            temporary.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                while (true) {
                    val remaining = expectedSize + 1L - copied
                    if (remaining <= 0L) {
                        throw IOException("Selected image is larger than the target partition")
                    }
                    val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied += read
                }
                output.fd.sync()
                if (copied != expectedSize) {
                    throw IOException(
                        "Image size mismatch: selected=$copied, partition=$expectedSize"
                    )
                }
            }
        } ?: throw IOException("Cannot open selected image")
        check(temporary.renameTo(file)) { "Cannot finalize rescue import cache" }
        file
    } catch (error: Exception) {
        temporary.delete()
        file.delete()
        throw error
    }
}
