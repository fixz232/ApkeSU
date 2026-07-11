package me.weishu.kernelsu.ui.screen.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ui.component.material.ExpressiveSwitch
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.RescueConfigState
import me.weishu.kernelsu.ui.util.RescueImageState
import me.weishu.kernelsu.ui.util.RescueStatus
import me.weishu.kernelsu.ui.util.getRescueLogs
import me.weishu.kernelsu.ui.util.getRescueStatus
import me.weishu.kernelsu.ui.util.importRescueImage
import me.weishu.kernelsu.ui.util.runRescueCommand
import me.weishu.kernelsu.ui.util.saveRescueConfig
import me.weishu.kernelsu.ui.util.testRescueEnvironment
import java.io.File
import java.util.Locale

private const val TITLE = "救砖保护"

private val RESCUE_TABS = listOf("主页", "配置", "使用说明")

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
    var pendingImportForce by remember { mutableStateOf(false) }
    var testReport by remember { mutableStateOf("") }
    var includeDtbo by remember { mutableStateOf(false) }
    var includeVbmeta by remember { mutableStateOf(false) }
    var backupOtherSlot by remember { mutableStateOf(false) }
    var allowDangerousAutoRestore by remember { mutableStateOf(false) }
    var bootPath by remember { mutableStateOf("") }
    var vendorBootPath by remember { mutableStateOf("") }
    var initBootPath by remember { mutableStateOf("") }
    var dtboPath by remember { mutableStateOf("") }
    var vbmetaPath by remember { mutableStateOf("") }

    val imageImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val partition = pendingImportPartition ?: return@rememberLauncherForActivityResult
        val force = pendingImportForce
        scope.launch {
            busy = true
            val file = runCatching { copyRescueImageToCache(context, uri, partition) }.getOrNull()
            val ok = file?.let { importRescueImage(partition, it.absolutePath, force) } == true
            file?.delete()
            status = getRescueStatus()
            logs = getRescueLogs()
            busy = false
            Toast.makeText(
                context,
                if (ok) "镜像已导入" else "镜像导入失败，请查看日志",
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
            val current = getRescueStatus()
            status = current
            logs = getRescueLogs()
            if (syncConfig) {
                loadConfig(current)
            }
            loading = false
        }
    }

    fun runAction(command: String, success: String, fail: String, timeoutMultiplier: Long = 6) {
        scope.launch {
            busy = true
            val ok = runRescueCommand(command, timeoutMultiplier)
            status = getRescueStatus()
            logs = getRescueLogs()
            busy = false
            Toast.makeText(context, if (ok) success else fail, Toast.LENGTH_LONG).show()
        }
    }

    fun saveConfig() {
        scope.launch {
            busy = true
            val custom = mapOf(
                "boot" to bootPath,
                "vendor_boot" to vendorBootPath,
                "init_boot" to initBootPath,
                "dtbo" to dtboPath,
                "vbmeta" to vbmetaPath,
            ).filterValues(String::isNotBlank)
            val ok = saveRescueConfig(
                RescueConfigState(
                    includeDtbo = includeDtbo,
                    includeVbmeta = includeVbmeta,
                    backupOtherSlot = backupOtherSlot,
                    allowDangerousAutoRestore = allowDangerousAutoRestore,
                    customPartitions = custom,
                )
            )
            status = getRescueStatus()
            logs = getRescueLogs()
            busy = false
            Toast.makeText(context, if (ok) "配置已保存" else "配置保存失败", Toast.LENGTH_LONG).show()
        }
    }

    fun testEnvironment() {
        scope.launch {
            busy = true
            val report = testRescueEnvironment()
            testReport = report.text.ifBlank {
                if (report.ok) "检测通过" else "检测未通过：${report.reason}"
            }
            status = getRescueStatus()
            logs = getRescueLogs()
            busy = false
            Toast.makeText(context, if (report.ok) "检测通过" else "检测未通过", Toast.LENGTH_LONG).show()
        }
    }

    fun launchImageImport(partition: String, force: Boolean) {
        pendingImportPartition = partition
        pendingImportForce = force
        imageImportLauncher.launch(arrayOf("*/*"))
    }

    LaunchedEffect(Unit) {
        refresh(syncConfig = true)
    }

    val pageContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
    val barContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    Scaffold(
        containerColor = pageContainerColor,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text(TITLE) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = barContainerColor,
                    scrolledContainerColor = barContainerColor,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = barContainerColor,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                RESCUE_TABS.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
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

                when (selectedTab) {
                    0 -> RescueHomePage(
                        status = status,
                        logs = logs,
                        busy = busy,
                        testReport = testReport,
                        onImport = { image ->
                            if (image.exists) {
                                showImportConfirmFor = image
                            } else {
                                launchImageImport(image.name, force = false)
                            }
                        },
                        onBackup = {
                            if (status.manifestCreatedAt.isBlank()) {
                                runAction("backup", "备份完成", "备份失败，请查看日志", timeoutMultiplier = 30)
                            } else {
                                showBackupConfirm = true
                            }
                        },
                        onTest = ::testEnvironment,
                        onToggle = { enabled ->
                            runAction(
                                if (enabled) "enable" else "disable",
                                if (enabled) "救砖保护已启用" else "救砖保护已关闭",
                                "操作失败，请先检测环境并确认备份完整",
                            )
                        },
                        onRestore = { showRestoreConfirm = true },
                        onRefresh = { refresh(syncConfig = true) },
                        onClearLogs = { runAction("clear-logs", "日志已清空", "清空日志失败") },
                    )

                    1 -> RescueConfigCard(
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

                    else -> RescueHelpCard()
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
            title = { Text("确认保留数据回滚？") },
            text = {
                Text(
                    "这会立即写回已备份的 boot/init_boot/vendor_boot 等启动镜像，并尝试重启；不会清空 /data，也不会删除救砖备份和配置。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        runAction(
                            "restore",
                            "已开始保留数据回滚",
                            "保留数据回滚失败，请查看日志",
                            timeoutMultiplier = 20,
                        )
                    },
                ) {
                    Text("确认回滚")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (showBackupConfirm) {
        AlertDialog(
            onDismissRequest = { showBackupConfirm = false },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
            title = { Text("覆盖旧备份？") },
            text = { Text("当前已经存在一份救砖备份。只有确认当前系统可以正常启动时，才建议覆盖旧备份。") },
            confirmButton = {
                Button(
                    onClick = {
                        showBackupConfirm = false
                        runAction("backup --force", "备份已覆盖", "覆盖备份失败，请查看日志", timeoutMultiplier = 30)
                    },
                ) {
                    Text("确认覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }

    showImportConfirmFor?.let { image ->
        AlertDialog(
            onDismissRequest = { showImportConfirmFor = null },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
            title = { Text("覆盖 ${image.name} 备份？") },
            text = {
                Text(
                    "导入本地镜像会覆盖当前 ${image.name} 救砖备份。请确认选择的是当前设备、当前槽位对应的镜像。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirmFor = null
                        launchImageImport(image.name, force = true)
                    },
                ) {
                    Text("选择文件并覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmFor = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun RescueHomePage(
    status: RescueStatus,
    logs: String,
    busy: Boolean,
    testReport: String,
    onImport: (RescueImageState) -> Unit,
    onBackup: () -> Unit,
    onTest: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onRestore: () -> Unit,
    onRefresh: () -> Unit,
    onClearLogs: () -> Unit,
) {
    RescueStatusCard(status = status)
    RescueImagesCard(status = status, onImport = onImport)
    RescueActionCard(
        status = status,
        busy = busy,
        onBackup = onBackup,
        onTest = onTest,
        onToggle = onToggle,
        onRestore = onRestore,
        onRefresh = onRefresh,
        onClearLogs = onClearLogs,
    )
    if (testReport.isNotBlank()) {
        RescueTextCard("检测报告", testReport)
    }
    RescueTextCard("日志", logs.ifBlank { status.log }.ifBlank { "暂无救砖日志" })
}

@Composable
private fun RescueStatusCard(status: RescueStatus) {
    RescueCard(title = "当前状态") {
        StatusLine("保护状态", if (status.enabled) "已启用" else "未启用")
        StatusLine("备份完整", if (status.requiredReady) "可用" else "不完整")
        if (!status.requiredReady && status.readyReason.isNotBlank()) {
            StatusLine("原因", status.readyReason)
        }
        StatusLine("当前槽位", status.currentSlot.ifBlank { "无槽位或未检测到" })
        StatusLine("启动模式", status.bootMode.ifBlank { "未检测到" })
        StatusLine("设备", status.device.ifBlank { "未检测到" })
        StatusLine("备份时间", status.manifestCreatedAt.ifBlank { "未备份" })
        StatusLine("待验证启动", if (status.pendingBoot) "是" else "否")
        StatusLine("连续失败计数", status.bootCount.toString())
        StatusLine("自动恢复次数", status.autoRestoreAttempts.toString())
    }
}

@Composable
private fun RescueImagesCard(
    status: RescueStatus,
    onImport: (RescueImageState) -> Unit,
) {
    RescueCard(title = "镜像备份") {
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
                    append(image.name.ifBlank { "unknown" })
                    if (image.required) append(" *")
                    if (image.otherSlot) append(" / 另一槽")
                    if (image.custom) append(" / 手动")
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            val ok = image.exists && image.sizeOk && image.sha256Ok
            Text(
                text = when {
                    ok -> "正常"
                    image.partition.isBlank() -> "无分区"
                    !image.exists -> "未备份"
                    !image.sizeOk -> "大小异常"
                    !image.sha256Ok -> "校验失败"
                    else -> "异常"
                },
                color = when {
                    ok -> MaterialTheme.colorScheme.primary
                    image.partition.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Text(
            text = image.partition.ifBlank { "未检测到分区" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (image.exists) {
            Text(
                text = "备份 ${formatSize(image.size)} / 分区 ${formatSize(image.partitionSize)} / SHA256 ${image.sha256.take(12)}...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (image.restore && !image.otherSlot) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onImport(image) },
            ) {
                Text(if (image.exists) "导入本地镜像并覆盖" else "导入本地镜像备份")
            }
        }
    }
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
    RescueCard(title = "配置") {
        SwitchLine(
            "双槽备份",
            "中风险：额外备份另一槽 boot/vendor_boot/init_boot；手动分区路径不会自动推断另一槽。",
            backupOtherSlot,
            onBackupOtherSlotChange,
        )
        SwitchLine(
            "同时备份 dtbo",
            "中高风险：适合刷内核时 DTBO 风险较高的设备；不确定时保持关闭。",
            includeDtbo,
            onIncludeDtboChange,
        )
        SwitchLine(
            "同时备份 vbmeta",
            "高风险：只建议高级用户开启；不确定时保持关闭。",
            includeVbmeta,
            onIncludeVbmetaChange,
        )
        if (includeDtbo || includeVbmeta) {
            SwitchLine(
                "允许自动恢复高危分区",
                "高风险：开启后卡启动自动回滚会写回 dtbo/vbmeta；不确定时保持关闭。",
                allowDangerousAutoRestore,
                onAllowDangerousAutoRestoreChange,
            )
        }
        PartitionPathField("boot 分区路径", bootPath, onBootPathChange)
        PartitionPathField("vendor_boot 分区路径", vendorBootPath, onVendorBootPathChange)
        PartitionPathField("init_boot 分区路径", initBootPath, onInitBootPathChange)
        if (includeDtbo) PartitionPathField("dtbo 分区路径", dtboPath, onDtboPathChange)
        if (includeVbmeta) PartitionPathField("vbmeta 分区路径", vbmetaPath, onVbmetaPathChange)
        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            onClick = onSave,
        ) {
            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("保存配置")
        }
    }
}

@Composable
private fun RescueActionCard(
    status: RescueStatus,
    busy: Boolean,
    onBackup: () -> Unit,
    onTest: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onRestore: () -> Unit,
    onRefresh: () -> Unit,
    onClearLogs: () -> Unit,
) {
    RescueCard(title = "操作") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("启用救砖保护", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "启用前必须先备份，并通过大小和 SHA256 校验。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExpressiveSwitch(
                checked = status.enabled,
                enabled = !busy && (status.requiredReady || status.enabled),
                onCheckedChange = onToggle,
                showThumbIcon = false,
            )
        }
        FilledTonalButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = onTest) {
            Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("检测环境")
        }
        FilledTonalButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = onBackup) {
            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("备份当前镜像")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && status.requiredReady,
            onClick = onRestore,
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("保留数据回滚镜像")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("刷新")
            }
            OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = onClearLogs) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("清空日志")
            }
        }
    }
}

@Composable
private fun RescueHelpCard() {
    RescueCard(title = "推荐操作步骤") {
        StepLine(1, "进入配置页", "按设备情况开启双槽备份、dtbo/vbmeta 备份，必要时填写手动分区路径。")
        StepLine(2, "检测环境", "确认 boot 分区可识别，当前槽位、设备信息和配置可正常读取。")
        StepLine(3, "保存配置", "保存后如果修改过分区范围或手动路径，请重新备份。")
        StepLine(4, "备份当前镜像", "生成 boot/vendor_boot/init_boot 等备份，并记录大小与 SHA256。")
        StepLine(5, "启用救砖保护", "只有备份完整可用时再开启；开启后刷写启动镜像会标记下一次启动待验证。")
        StepLine(6, "刷入或修补镜像", "刷入后先正常重启一次，成功进入系统后会自动标记本次启动安全。")
        StepLine(7, "异常时自动处理", "如果启动失败或卡 recovery，救砖保护会按规则尝试保留数据回滚。")
    }

    RescueCard(title = "什么时候会自动回滚") {
        HelpLine("刷写 boot/init_boot/vendor_boot 后，下次启动会进入待验证状态。")
        HelpLine("待验证状态下连续启动失败达到 2 次，会触发自动回滚。")
        HelpLine("未处于待验证状态的普通启动不会自动回滚，避免旧备份误覆盖当前系统。")
        HelpLine("检测到 pstore 里的 panic、watchdog、oops 等启动失败线索，会触发自动回滚。")
        HelpLine("如果设备卡到 recovery/rec，且仍能运行 ksud recovery-check，也会尝试自动回滚。")
        HelpLine("自动恢复最多尝试 3 次；超过限制后会关闭保护，避免循环恢复。")
        HelpLine("默认是保留数据回滚：只恢复启动相关镜像，不会清空 /data。")
        HelpLine("双槽设备会优先恢复当前槽匹配的备份；如果只有备份槽可用，会恢复备份槽并尝试切回备份槽。")
        HelpLine("dtbo/vbmeta 属于高风险分区，只有在配置页允许后才会参与自动恢复。")
        HelpLine("极早期连 /data/adb/ksud 都无法启动的硬砖场景，仍需要 fastboot 或线刷。")
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
        ExpressiveSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            showThumbIcon = false,
        )
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
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

private suspend fun copyRescueImageToCache(context: Context, uri: Uri, partition: String): File = withContext(Dispatchers.IO) {
    val dir = File(context.cacheDir, "rescue-import").apply { mkdirs() }
    val file = File(dir, "${partition}_${System.currentTimeMillis()}.img")
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: error("Cannot open selected image")
    file
}
