package me.weishu.kernelsu.ui.screen.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.material.TonalCard
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.createRootShell
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val IMAGE_TOOL_OUTPUT_DIR = "/sdcard/Download/ApkeSU-images"
private const val IMAGE_TOOL_GRID_COLUMNS = 3

@Composable
fun ImageToolScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val onBack = dropUnlessResumed { navigator.pop() }
    var partitionPath by remember { mutableStateOf("/dev/block/by-name/boot") }
    var outputName by remember { mutableStateOf("boot_backup.img") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageLabel by remember { mutableStateOf("") }
    var confirmFlash by remember { mutableStateOf(false) }
    var selectedPartition by remember { mutableStateOf<ImagePartitionTarget?>(null) }
    var cardFlashTarget by remember { mutableStateOf<ImagePartitionTarget?>(null) }
    var cardFlashImageUri by remember { mutableStateOf<Uri?>(null) }
    var cardFlashImageLabel by remember { mutableStateOf("") }
    var cardFlashConfirmed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var log by remember { mutableStateOf("") }
    var partitions by remember { mutableStateOf(emptyList<ImagePartitionTarget>()) }
    val selectedLogPrefix = stringResource(R.string.image_tool_selected_prefix)

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        selectedImageUri = uri
        selectedImageLabel = uri.lastPathSegment.orEmpty().ifBlank { uri.toString() }
        log = "$selectedLogPrefix $selectedImageLabel"
    }

    val cardImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || cardFlashTarget == null) {
            cardFlashTarget = null
            cardFlashImageUri = null
            cardFlashImageLabel = ""
            cardFlashConfirmed = false
            return@rememberLauncherForActivityResult
        }
        cardFlashImageUri = uri
        cardFlashImageLabel = uri.lastPathSegment.orEmpty().ifBlank { uri.toString() }
        cardFlashConfirmed = false
    }

    fun runOperation(block: suspend () -> ImageToolResult) {
        if (busy) return
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { block() }
                    .getOrElse { ImageToolResult(false, it.localizedMessage ?: it.toString()) }
            }
            log = result.log
            busy = false
        }
    }

    fun refreshPartitions() {
        if (scanning) return
        scanning = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { scanImagePartitions() } }
            result.fold(
                onSuccess = { scanned ->
                    partitions = scanned
                    scanError = null
                    log = "partitions=${scanned.size}"
                },
                onFailure = { error ->
                    partitions = emptyList()
                    scanError = (error.localizedMessage ?: error.toString()).take(240)
                    log = "partition scan failed: ${error.localizedMessage ?: error}"
                },
            )
            scanning = false
        }
    }

    LaunchedEffect(Unit) {
        refreshPartitions()
    }

    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.image_tool_title),
                color = Color.Transparent,
                titleColor = MiuixTheme.colorScheme.onSurface,
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MiuixTheme.colorScheme.onBackground,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.image_tool_warning_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.image_tool_warning_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.image_tool_partition_cards_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (partitions.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.image_tool_partition_count, partitions.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    FilledTonalButton(
                        enabled = !busy && !scanning,
                        onClick = ::refreshPartitions,
                    ) {
                        Text(stringResource(R.string.image_tool_refresh_partitions))
                    }
                }
                Text(
                    text = stringResource(R.string.image_tool_partition_cards_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    scanning && partitions.isEmpty() -> {
                        TonalCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text(
                                    text = stringResource(R.string.image_tool_scanning_partitions),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    scanError != null -> {
                        TonalCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.image_tool_scan_failed, scanError.orEmpty()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                FilledTonalButton(
                                    enabled = !scanning,
                                    onClick = ::refreshPartitions,
                                ) {
                                    Text(stringResource(R.string.image_tool_refresh_partitions))
                                }
                            }
                        }
                    }
                    partitions.isEmpty() -> {
                        TonalCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.image_tool_no_partitions),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    else -> {
                        ImagePartitionGrid(
                            partitions = partitions,
                            busy = busy,
                            onPartitionClick = { selectedPartition = it },
                        )
                    }
                }
                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.image_tool_custom_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.image_tool_custom_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = partitionPath,
                        onValueChange = { partitionPath = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.image_tool_partition_path)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    )
                    OutlinedTextField(
                        value = outputName,
                        onValueChange = { outputName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.image_tool_output_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    )
                }
            }

            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ImageToolActionHeader(
                        icon = Icons.Rounded.Download,
                        title = stringResource(R.string.image_tool_extract_title),
                        summary = stringResource(R.string.image_tool_extract_summary),
                    )
                    Button(
                        enabled = !busy,
                        onClick = {
                            runOperation {
                                extractImage(partitionPath, outputName)
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.image_tool_extract_action))
                    }
                }
            }

            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ImageToolActionHeader(
                        icon = Icons.Rounded.UploadFile,
                        title = stringResource(R.string.image_tool_flash_title),
                        summary = stringResource(R.string.image_tool_flash_summary),
                    )
                    FilledTonalButton(
                        enabled = !busy,
                        onClick = { imagePicker.launch(arrayOf("application/octet-stream", "image/*", "*/*")) },
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.image_tool_pick_image))
                    }
                    if (selectedImageLabel.isNotBlank()) {
                        Text(
                            text = selectedImageLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = confirmFlash,
                            onCheckedChange = { confirmFlash = it },
                        )
                        Text(
                            text = stringResource(R.string.image_tool_confirm_flash),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        enabled = !busy && confirmFlash && selectedImageUri != null,
                        onClick = {
                            val uri = selectedImageUri ?: return@Button
                            runOperation { flashImage(context, uri, partitionPath) }
                        },
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.image_tool_flash_action))
                    }
                }
            }

            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ImageToolActionHeader(
                        icon = Icons.Rounded.EditNote,
                        title = stringResource(R.string.image_tool_modify_title),
                        summary = stringResource(R.string.image_tool_modify_summary),
                    )
                    Button(
                        enabled = !busy && selectedImageUri != null,
                        onClick = {
                            val uri = selectedImageUri ?: return@Button
                            runOperation { createEditableCopy(context, uri, outputName) }
                        },
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.image_tool_modify_action))
                    }
                }
            }

            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.image_tool_log_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalButton(
                            enabled = log.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("image_tool_log", log),
                                        ),
                                    )
                                }
                            },
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                        }
                    }
                    Text(
                        text = log.ifBlank { stringResource(R.string.image_tool_log_empty) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    selectedPartition?.let { target ->
        ImagePartitionActionsDialog(
            target = target,
            busy = busy,
            onDismissRequest = { selectedPartition = null },
            onExtract = {
                selectedPartition = null
                runOperation { extractImage(target.path, "${target.name}.img") }
            },
            onModify = {
                selectedPartition = null
                runOperation { extractImage(target.path, "${target.name}_editable.img") }
            },
            onFlash = {
                selectedPartition = null
                cardFlashTarget = target
                cardFlashImageUri = null
                cardFlashImageLabel = ""
                cardFlashConfirmed = false
                cardImagePicker.launch(arrayOf("application/octet-stream", "image/*", "*/*"))
            },
        )
    }

    val flashTarget = cardFlashTarget
    val flashUri = cardFlashImageUri
    if (flashTarget != null && flashUri != null) {
        ImagePartitionFlashDialog(
            target = flashTarget,
            imageLabel = cardFlashImageLabel,
            confirmed = cardFlashConfirmed,
            busy = busy,
            onConfirmedChange = { cardFlashConfirmed = it },
            onDismissRequest = {
                cardFlashTarget = null
                cardFlashImageUri = null
                cardFlashImageLabel = ""
                cardFlashConfirmed = false
            },
            onFlash = {
                val targetPath = flashTarget.path
                val selectedUri = flashUri
                cardFlashTarget = null
                cardFlashImageUri = null
                cardFlashImageLabel = ""
                cardFlashConfirmed = false
                runOperation { flashImage(context, selectedUri, targetPath) }
            },
        )
    }
}

@Composable
private fun ImagePartitionGrid(
    partitions: List<ImagePartitionTarget>,
    busy: Boolean,
    onPartitionClick: (ImagePartitionTarget) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        partitions.chunked(IMAGE_TOOL_GRID_COLUMNS).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { target ->
                    ImagePartitionCard(
                        target = target,
                        enabled = !busy,
                        onClick = { onPartitionClick(target) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(IMAGE_TOOL_GRID_COLUMNS - rowItems.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePartitionCard(
    target: ImagePartitionTarget,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = target.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ImagePartitionActionsDialog(
    target: ImagePartitionTarget,
    busy: Boolean,
    onDismissRequest: () -> Unit,
    onExtract: () -> Unit,
    onModify: () -> Unit,
    onFlash: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Rounded.Memory, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.image_tool_partition_actions_title, target.name),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = target.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
                if (!target.realPath.isNullOrBlank() && target.realPath != target.path) {
                    Text(
                        text = target.realPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    onClick = onExtract,
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.image_tool_extract_action))
                }
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    onClick = onModify,
                ) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.image_tool_modify_action_short))
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    onClick = onFlash,
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.image_tool_flash_action))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun ImagePartitionFlashDialog(
    target: ImagePartitionTarget,
    imageLabel: String,
    confirmed: Boolean,
    busy: Boolean,
    onConfirmedChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onFlash: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Rounded.UploadFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.image_tool_confirm_flash_action)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.image_tool_flash_target, target.name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = target.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = imageLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = confirmed,
                        enabled = !busy,
                        onCheckedChange = onConfirmedChange,
                    )
                    Text(
                        text = stringResource(R.string.image_tool_confirm_flash),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && confirmed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                onClick = onFlash,
            ) {
                Text(stringResource(R.string.image_tool_confirm_flash_action))
            }
        },
        dismissButton = {
            TextButton(
                enabled = !busy,
                onClick = onDismissRequest,
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun ImageToolActionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class ImageToolResult(
    val success: Boolean,
    val log: String,
)

private data class ImagePartitionTarget(
    val name: String,
    val path: String,
    val realPath: String? = null,
)

private suspend fun scanImagePartitions(): List<ImagePartitionTarget> = withContext(Dispatchers.IO) {
    val stdout = arrayListOf<String>()
    val stderr = arrayListOf<String>()
    val command = """
        for p in /dev/block/by-name/*; do
          [ -e "${'$'}p" ] || continue
          name="${'$'}(basename "${'$'}p")"
          real="${'$'}(readlink -f "${'$'}p" 2>/dev/null || echo "${'$'}p")"
          echo "${'$'}name|${'$'}p|${'$'}real"
        done
    """.trimIndent()
    val result = createRootShell(globalMnt = true).use { shell ->
        shell.newJob().add(command).to(stdout, stderr).exec()
    }
    check(result.isSuccess) {
        stderr.joinToString("\n").ifBlank { "root shell could not read /dev/block/by-name" }
    }
    stdout.mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size < 2) return@mapNotNull null
        val name = parts[0].trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val path = parts[1].trim().takeIf { it.startsWith("/dev/block/") } ?: return@mapNotNull null
        val real = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
        ImagePartitionTarget(
            name = name,
            path = path,
            realPath = real,
        )
    }
        .distinctBy { it.path }
        .sortedWith(compareBy<ImagePartitionTarget> { imagePartitionSortWeight(it.name) }.thenBy { it.name })
}

private fun imagePartitionSortWeight(name: String): Int {
    val baseName = name.lowercase().removeSuffix("_a").removeSuffix("_b")
    return when (baseName) {
        "boot" -> 0
        "init_boot" -> 1
        "vendor_boot" -> 2
        "dtbo" -> 3
        "vbmeta" -> 4
        "recovery" -> 5
        "super" -> 6
        "system" -> 7
        "vendor" -> 8
        "product" -> 9
        else -> 20
    }
}

private suspend fun extractImage(
    partitionPath: String,
    outputName: String,
): ImageToolResult = withContext(Dispatchers.IO) {
    val partition = sanitizeImageToolPartition(partitionPath)
    val name = sanitizeImageToolOutputName(outputName)
    val output = "$IMAGE_TOOL_OUTPUT_DIR/$name"
    val command = """
        set -eu
        part=${shellQuote(partition)}
        out=${shellQuote(output)}
        test -b "${'$'}part"
        mkdir -p ${shellQuote(IMAGE_TOOL_OUTPUT_DIR)}
        dd if="${'$'}part" of="${'$'}out" bs=4M conv=fsync
        sync
        ls -l "${'$'}out"
        sha256sum "${'$'}out" 2>/dev/null || toybox sha256sum "${'$'}out"
    """.trimIndent()
    runImageToolRootCommand(command, "extract")
}

private suspend fun flashImage(
    context: Context,
    uri: Uri,
    partitionPath: String,
): ImageToolResult = withContext(Dispatchers.IO) {
    val partition = sanitizeImageToolPartition(partitionPath)
    val staged = File(context.cacheDir, "image-tool-flash.img")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Unable to open selected image" }
        FileOutputStream(staged).use { output -> input.copyTo(output) }
    }
    require(staged.length() > 0L) { "Selected image is empty" }
    val sha256 = sha256(staged)
    val command = """
        set -eu
        img=${shellQuote(staged.absolutePath)}
        part=${shellQuote(partition)}
        test -f "${'$'}img"
        test -b "${'$'}part"
        image_size="${'$'}(wc -c < "${'$'}img" | tr -d ' ')"
        partition_size="${'$'}(blockdev --getsize64 "${'$'}part" 2>/dev/null || true)"
        if [ -n "${'$'}partition_size" ] && [ "${'$'}image_size" -gt "${'$'}partition_size" ]; then
          echo "image is larger than target partition: image=${'$'}image_size partition=${'$'}partition_size" >&2
          exit 1
        fi
        dd if="${'$'}img" of="${'$'}part" bs=4M conv=fsync
        sync
        echo "flashed=${'$'}part"
        echo "image_size=${'$'}image_size"
        echo "partition_size=${'$'}partition_size"
        echo "sha256=$sha256"
    """.trimIndent()
    runImageToolRootCommand(command, "flash").also {
        staged.delete()
    }
}

private suspend fun createEditableCopy(
    context: Context,
    uri: Uri,
    outputName: String,
): ImageToolResult = withContext(Dispatchers.IO) {
    val name = sanitizeImageToolOutputName(outputName).removeSuffix(".img") + "_editable.img"
    val staged = File(context.cacheDir, name)
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Unable to open selected image" }
        FileOutputStream(staged).use { output -> input.copyTo(output) }
    }
    require(staged.length() > 0L) { "Selected image is empty" }
    val output = "$IMAGE_TOOL_OUTPUT_DIR/$name"
    val command = """
        set -eu
        mkdir -p ${shellQuote(IMAGE_TOOL_OUTPUT_DIR)}
        cp -f ${shellQuote(staged.absolutePath)} ${shellQuote(output)}
        sync
        ls -l ${shellQuote(output)}
        sha256sum ${shellQuote(output)} 2>/dev/null || toybox sha256sum ${shellQuote(output)}
        echo "editable_copy=${shellQuote(output)}"
    """.trimIndent()
    runImageToolRootCommand(command, "modify").also {
        staged.delete()
    }
}

private suspend fun runImageToolRootCommand(command: String, label: String): ImageToolResult {
    val stdout = arrayListOf<String>()
    val stderr = arrayListOf<String>()
    val result = createRootShell(globalMnt = true).use { shell ->
        shell.newJob().add(command).to(stdout, stderr).exec()
    }
    return ImageToolResult(
        success = result.isSuccess,
        log = buildString {
            appendLine("[$label] ${if (result.isSuccess) "success" else "failed"}")
            if (stdout.isNotEmpty()) {
                appendLine("stdout:")
                appendLine(stdout.joinToString("\n"))
            }
            if (stderr.isNotEmpty()) {
                appendLine("stderr:")
                appendLine(stderr.joinToString("\n"))
            }
        }.trim(),
    )
}

private fun sanitizeImageToolPartition(value: String): String {
    val path = value.trim()
    require(path.startsWith("/dev/block/")) { "Partition path must start with /dev/block/" }
    require(".." !in path && path.none { it == '\n' || it == '\r' || it == '\u0000' }) {
        "Partition path is invalid"
    }
    return path
}

private fun sanitizeImageToolOutputName(value: String): String {
    val name = value.trim()
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .ifBlank { "image_backup.img" }
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .take(96)
    return if (name.endsWith(".img", ignoreCase = true)) name else "$name.img"
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

private fun shellQuote(value: String): String {
    return "'${value.replace("'", "'\"'\"'")}'"
}
