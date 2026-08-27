package me.weishu.kernelsu.ui.screen.install

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.getKernelVersion
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.choosekmidialog.ChooseKmiDialog
import me.weishu.kernelsu.ui.component.dialog.DownloadDialog
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.screen.flash.FlashIt
import me.weishu.kernelsu.ui.util.BootPatchMode
import me.weishu.kernelsu.ui.util.BootImageKmiSource
import me.weishu.kernelsu.ui.util.LkmSelection
import me.weishu.kernelsu.ui.util.detectBootImageKmi
import me.weishu.kernelsu.ui.util.getAvailablePartitions
import me.weishu.kernelsu.ui.util.getCurrentKmi
import me.weishu.kernelsu.ui.util.getDefaultPartition
import me.weishu.kernelsu.ui.util.getFileName
import me.weishu.kernelsu.ui.util.getSlotSuffix
import me.weishu.kernelsu.ui.util.getSupportedKmis
import me.weishu.kernelsu.ui.util.isAbDevice
import me.weishu.kernelsu.ui.util.probeRemoteBootPartitions
import me.weishu.kernelsu.ui.util.rootAvailable
import top.yukonga.miuix.kmp.basic.SnackbarHostState as MiuixSnackbarHostState

@Composable
fun InstallScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val miuixSnackbarHost = remember { MiuixSnackbarHostState() }
    val uiMode = LocalUiMode.current
    val scope = rememberCoroutineScope()
    val materialSnackbarHost = remember { androidx.compose.material3.SnackbarHostState() }
    val resources = LocalResources.current

    var installMethod by rememberSaveable { mutableStateOf<InstallMethod?>(null) }
    var downloadDialogShown by rememberSaveable { mutableStateOf(false) }
    var remotePartitions by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var remotePartitionSelectionIndex by rememberSaveable { mutableIntStateOf(0) }
    var remoteProbeInProgress by rememberSaveable { mutableStateOf(false) }
    var remoteProbeJob by remember { mutableStateOf<Job?>(null) }
    var lkmSelection by rememberSaveable { mutableStateOf<LkmSelection>(LkmSelection.KmiNone) }
    var partitionSelectionIndex by rememberSaveable { mutableIntStateOf(0) }
    var hasCustomSelected by rememberSaveable { mutableStateOf(false) }
    var selectedPartitionName by rememberSaveable { mutableStateOf<String?>(null) }
    val showChooseKmiDialog = rememberSaveable { mutableStateOf(false) }
    var installAfterKmiSelection by rememberSaveable { mutableStateOf(false) }
    var patchMode by rememberSaveable { mutableStateOf(BootPatchMode.Normal) }
    var detectedImageKmi by rememberSaveable { mutableStateOf("") }
    var manualKmi by rememberSaveable { mutableStateOf("") }
    var imageKmiSource by rememberSaveable { mutableStateOf(InstallKmiSource.None) }
    var advancedOptionsShown by rememberSaveable { mutableStateOf(false) }
    var allowShell by rememberSaveable { mutableStateOf(false) }
    var enableAdb by rememberSaveable { mutableStateOf(false) }
    var forceBackup by rememberSaveable { mutableStateOf(false) }
    var navigationLocked by remember { mutableStateOf(false) }

    val isOta = installMethod is InstallMethod.DirectInstallToInactiveSlot

    val currentKmi by produceState(initialValue = "") {
        value = loadInstallState("") { getCurrentKmi() }
    }
    val supportedKmis by produceState(initialValue = emptyList<String>()) {
        value = loadInstallState(emptyList<String>()) { getSupportedKmis() }
    }
    val partitions by produceState(initialValue = emptyList<String>(), isOta) {
        value = loadInstallState(emptyList<String>()) { getAvailablePartitions(isOta) }
    }
    val defaultPartition by produceState(initialValue = "", isOta) {
        value = loadInstallState("") { getDefaultPartition(isOta) }
    }
    val rootAvailable by produceState(initialValue = false) {
        value = loadInstallState(false) { withContext(Dispatchers.IO) { rootAvailable() } }
    }
    val isAbDevice by produceState(initialValue = false) {
        value = loadInstallState(false) { isAbDevice() }
    }
    val initialIsGkiDevice = remember {
        runCatching { getKernelVersion().isGKI() }.getOrDefault(false)
    }
    val isGkiDevice by produceState(initialValue = initialIsGkiDevice) {
        value = loadInstallState(false) { withContext(Dispatchers.IO) { getKernelVersion().isGKI() } }
    }
    val selectFileTip = stringResource(id = R.string.select_file_tip, defaultPartition)
    val selectFileTipNoGki = stringResource(id = R.string.select_file_tip_nogki)
    val downloadFileSummary = stringResource(id = R.string.download_dialog_msg)
    val installMethodOptions = remember(
        rootAvailable,
        isAbDevice,
        isGkiDevice,
        selectFileTip,
        selectFileTipNoGki,
        downloadFileSummary,
    ) {
        buildList {
            add(InstallMethod.SelectFile(summary = if (isGkiDevice) selectFileTip else selectFileTipNoGki))
            add(InstallMethod.DownloadFile(summary = downloadFileSummary))
            if (rootAvailable) add(InstallMethod.AnyKernel())
            if (isGkiDevice) {
                add(InstallMethod.DirectInstall)
                if (rootAvailable && isAbDevice) add(InstallMethod.DirectInstallToInactiveSlot)
            }
        }
    }

    val slotSuffix by produceState(initialValue = "", isOta) {
        value = loadInstallState("") { getSlotSuffix(isOta) }
    }
    val partitionOptions = partitions
    val defaultIndex = remember(partitionOptions, defaultPartition) {
        partitionOptions.indexOf(defaultPartition).coerceAtLeast(0)
    }

    LaunchedEffect(
        partitionOptions,
        defaultIndex,
        hasCustomSelected,
        selectedPartitionName,
    ) {
        if (partitionOptions.isEmpty()) return@LaunchedEffect
        val preferredPartition = if (hasCustomSelected) {
            selectedPartitionName
        } else {
            defaultPartition
        }
        val preferredIndex = preferredPartition
            ?.let(partitionOptions::indexOf)
            ?.takeIf { it >= 0 }

        if (hasCustomSelected && preferredIndex == null) {
            hasCustomSelected = false
            selectedPartitionName = null
        }
        partitionSelectionIndex = (preferredIndex ?: defaultIndex).coerceIn(0, partitionOptions.lastIndex)
    }

    val displayPartitions = remember(partitionOptions, defaultPartition) {
        partitionOptions.map { name -> if (defaultPartition == name) "$name (default)" else name }
    }
    val remoteDisplayPartitions = remember(remotePartitions, defaultPartition) {
        remotePartitions.map { name -> if (defaultPartition == name) "$name (default)" else name }
    }
    val selectedPartitionIndex = partitionSelectionIndex.coerceIn(
        0,
        partitionOptions.lastIndex.coerceAtLeast(0),
    )

    fun showMessage(message: String) {
        scope.launch {
            if (uiMode == UiMode.Material) {
                materialSnackbarHost.showSnackbar(message)
            } else {
                miuixSnackbarHost.showSnackbar(message)
            }
        }
    }

    fun replaceInstallMethod(method: InstallMethod) {
        val previousUri = installMethod.sourceUri()
        val nextUri = method.sourceUri()
        if (previousUri != null && previousUri != nextUri) {
            releaseReadPermission(context, previousUri)
        }
        installMethod = method
    }

    fun replaceLkmSelection(selection: LkmSelection) {
        val previousUri = (lkmSelection as? LkmSelection.LkmUri)?.uri
        val nextUri = (selection as? LkmSelection.LkmUri)?.uri
        if (previousUri != null && previousUri != nextUri) {
            releaseReadPermission(context, previousUri)
        }
        lkmSelection = selection
    }

    fun navigateToFlash(action: FlashIt) {
        if (navigationLocked) return
        navigationLocked = true
        try {
            navigator.push(Route.Flash(action))
        } finally {
            scope.launch {
                delay(750)
                navigationLocked = false
            }
        }
    }

    val onInstall = { selectedLkm: LkmSelection ->
        installMethod?.let { method ->
            if (method is InstallMethod.SelectFile && method.uri == null) {
                return@let
            }
            if (method is InstallMethod.AnyKernel) {
                method.uri?.let { uri -> navigateToFlash(FlashIt.FlashAnyKernel(uri)) }
                return@let
            }
            if (method is InstallMethod.DownloadFile) {
                val url = method.url ?: return@let
                val partition = method.partition ?: return@let
                navigateToFlash(
                    FlashIt.DownloadBoot(
                        url = url,
                        partition = partition,
                        lkm = selectedLkm,
                        patchMode = patchMode,
                        allowShell = allowShell,
                        enableAdb = enableAdb,
                        backup = forceBackup,
                    )
                )
                return@let
            }
            val patchLkm = when {
                method is InstallMethod.SelectFile -> selectedLkm
                selectedLkm is LkmSelection.PathMaskKmiString -> LkmSelection.KmiNone
                else -> selectedLkm
            }
            val selectedPartition = when (method) {
                is InstallMethod.DirectInstall,
                is InstallMethod.DirectInstallToInactiveSlot -> partitionOptions.getOrNull(selectedPartitionIndex)

                else -> null
            }
            val bootUri = when (method) {
                is InstallMethod.SelectFile -> method.uri
                else -> null
            }
            navigateToFlash(
                FlashIt.FlashBoot(
                    boot = bootUri,
                    lkm = patchLkm,
                    patchMode = patchMode,
                    ota = method is InstallMethod.DirectInstallToInactiveSlot,
                    partition = selectedPartition,
                    allowShell = allowShell,
                    enableAdb = enableAdb,
                    backup = method is InstallMethod.SelectFile && forceBackup,
                )
            )
        }
    }

    ChooseKmiDialog(
        show = showChooseKmiDialog.value,
        onDismissRequest = {
            showChooseKmiDialog.value = false
            installAfterKmiSelection = false
        },
        onSelected = { kmi ->
            kmi?.let {
                manualKmi = it
                imageKmiSource = InstallKmiSource.Manual
                val selectedLkm = LkmSelection.KmiString(it)
                if (installAfterKmiSelection) {
                    onInstall(selectedLkm)
                    installAfterKmiSelection = false
                }
            }
        }
    )

    DownloadDialog(
        show = downloadDialogShown,
        onConfirm = { url ->
            downloadDialogShown = false
            remoteProbeJob?.cancel()
            remoteProbeJob = scope.launch {
                remoteProbeInProgress = true
                try {
                    val result = probeRemoteBootPartitions(url)
                    if (result.partitions.isEmpty()) {
                        showMessage(resources.getString(R.string.download_no_boot_partition))
                        return@launch
                    }
                    remotePartitions = result.partitions
                    val selectedIndex = result.partitions.indexOf(defaultPartition).coerceAtLeast(0)
                    remotePartitionSelectionIndex = selectedIndex
                    replaceInstallMethod(
                        InstallMethod.DownloadFile(
                            url = url,
                            partition = result.partitions[selectedIndex],
                            summary = downloadFileSummary,
                        )
                    )
                    manualKmi = ""
                    detectedImageKmi = ""
                    imageKmiSource = InstallKmiSource.Automatic
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    showMessage(
                        resources.getString(
                            R.string.download_probe_failed,
                            error.localizedMessage ?: error.javaClass.simpleName,
                        )
                    )
                } finally {
                    remoteProbeInProgress = false
                }
            }
        },
        onDismiss = { downloadDialogShown = false },
    )

    val selectLkmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        persistReadPermission(context, uri)
        if (isKoFile(context, uri)) {
            replaceLkmSelection(LkmSelection.LkmUri(uri))
        } else {
            releaseReadPermission(context, uri)
            showMessage(resources.getString(R.string.install_only_support_ko_file))
        }
    }
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        persistReadPermission(context, uri)
        val fileName = uri.getFileName(context)?.takeUnless { name -> name.isBlank() }
            ?: uri.lastPathSegment
            ?: if (isGkiDevice) selectFileTip else selectFileTipNoGki
        replaceInstallMethod(InstallMethod.SelectFile(uri, summary = fileName))
        manualKmi = ""
        detectedImageKmi = ""
        imageKmiSource = InstallKmiSource.Detecting
        scope.launch {
            val result = runCatching {
                detectBootImageKmi(uri, currentKmi.takeIf { it.isNotBlank() })
            }
            if ((installMethod as? InstallMethod.SelectFile)?.uri != uri) return@launch
            result.onSuccess { detection ->
                detectedImageKmi = detection.kmi
                imageKmiSource = when (detection.source) {
                    BootImageKmiSource.Image -> InstallKmiSource.Automatic
                    BootImageKmiSource.CurrentDevice -> InstallKmiSource.CurrentDevice
                }
            }.onFailure {
                detectedImageKmi = ""
                imageKmiSource = InstallKmiSource.Failed
            }
        }
    }
    val selectAnyKernelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        persistReadPermission(context, uri)
        val fileName = uri.getFileName(context)?.takeUnless { name -> name.isBlank() }
            ?: uri.lastPathSegment
            ?: resources.getString(R.string.anykernel_install)
        replaceInstallMethod(InstallMethod.AnyKernel(uri, summary = fileName))
    }

    val canInstall = when (val method = installMethod) {
        null -> false
        is InstallMethod.SelectFile -> method.uri != null
        is InstallMethod.DownloadFile -> !method.url.isNullOrBlank() && !method.partition.isNullOrBlank()
        is InstallMethod.AnyKernel -> method.uri != null && rootAvailable
        is InstallMethod.DirectInstall -> rootAvailable
        is InstallMethod.DirectInstallToInactiveSlot -> rootAvailable &&
                (slotSuffix == "_a" || slotSuffix == "_b") && partitions.isNotEmpty()
    }
    val targetKmi = manualKmi.ifBlank {
        if (installMethod is InstallMethod.SelectFile) detectedImageKmi else currentKmi
    }
    val targetKmiSource = when {
        manualKmi.isNotBlank() -> InstallKmiSource.Manual
        installMethod is InstallMethod.SelectFile -> imageKmiSource
        installMethod is InstallMethod.DownloadFile -> InstallKmiSource.Automatic
        currentKmi.isNotBlank() -> InstallKmiSource.CurrentDevice
        else -> InstallKmiSource.None
    }
    val isRemoteDownload = installMethod is InstallMethod.DownloadFile
    val usesBuiltInLkm = !isRemoteDownload && installMethod !is InstallMethod.AnyKernel &&
            (patchMode != BootPatchMode.Normal || lkmSelection !is LkmSelection.LkmUri)
    val targetKmiSupported = targetKmi.isNotBlank() &&
            (supportedKmis.isEmpty() || targetKmi in supportedKmis)
    val patchReady = isRemoteDownload || !usesBuiltInLkm || targetKmiSupported

    val state = InstallUiState(
        installMethod = installMethod,
        lkmSelection = lkmSelection,
        partitionSelectionIndex = selectedPartitionIndex,
        displayPartitions = displayPartitions,
        remotePartitionSelectionIndex = remotePartitionSelectionIndex.coerceIn(0, remotePartitions.lastIndex.coerceAtLeast(0)),
        remoteDisplayPartitions = remoteDisplayPartitions,
        currentKmi = currentKmi,
        slotSuffix = slotSuffix,
        installMethodOptions = installMethodOptions,
        rootAvailable = rootAvailable,
        canSelectPartition = (if (isRemoteDownload) remoteDisplayPartitions else displayPartitions).isNotEmpty() &&
                (installMethod is InstallMethod.DirectInstall ||
                        installMethod is InstallMethod.DirectInstallToInactiveSlot ||
                        installMethod is InstallMethod.DownloadFile),
        canInstall = canInstall && patchReady &&
                (!usesBuiltInLkm || targetKmiSource != InstallKmiSource.Detecting) &&
                !remoteProbeInProgress &&
                !navigationLocked,
        patchMode = patchMode,
        targetKmi = targetKmi,
        targetKmiSource = targetKmiSource,
        advancedOptionsShown = advancedOptionsShown,
        allowShell = allowShell,
        enableAdb = enableAdb,
        forceBackup = forceBackup,
        canForceBackup = installMethod is InstallMethod.SelectFile || installMethod is InstallMethod.DownloadFile,
    )
    val actions = InstallScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onSelectMethod = onSelectMethod@{ method ->
            if ((method is InstallMethod.DirectInstall ||
                    method is InstallMethod.DirectInstallToInactiveSlot ||
                    method is InstallMethod.AnyKernel) && !rootAvailable
            ) {
                showMessage(resources.getString(R.string.direct_install_root_required))
                return@onSelectMethod
            }
            if (method is InstallMethod.DirectInstallToInactiveSlot &&
                (slotSuffix != "_a" && slotSuffix != "_b" || partitions.isEmpty())
            ) {
                showMessage(resources.getString(R.string.install_inactive_slot_unavailable))
                return@onSelectMethod
            }
            replaceInstallMethod(method)
            if (method !is InstallMethod.SelectFile) {
                manualKmi = ""
                detectedImageKmi = ""
                imageKmiSource = InstallKmiSource.None
            }
        },
        onDownloadFile = { downloadDialogShown = true },
        onSelectBootImage = {
            selectImageLauncher.launch(BOOT_IMAGE_MIME_TYPES)
        },
        onSelectPatchMode = { selectedMode -> patchMode = selectedMode },
        onSelectPatchKmi = {
            installAfterKmiSelection = false
            showChooseKmiDialog.value = true
        },
        onSelectAnyKernel = {
            selectAnyKernelLauncher.launch(ANYKERNEL_MIME_TYPES)
        },
        onUploadLkm = {
            selectLkmLauncher.launch(LKM_MIME_TYPES)
        },
        onClearLkm = { replaceLkmSelection(LkmSelection.KmiNone) },
        onSelectPartition = { index ->
            val method = installMethod
            if (method is InstallMethod.DownloadFile) {
                remotePartitionSelectionIndex = index
                remotePartitions.getOrNull(index)?.let { partition ->
                    replaceInstallMethod(method.copy(partition = partition))
                }
            } else {
                partitionOptions.getOrNull(index)?.let { selectedPartition ->
                    hasCustomSelected = true
                    selectedPartitionName = selectedPartition
                    partitionSelectionIndex = index
                }
            }
        },
        onNext = onNext@{
            val method = installMethod
            val isAnyKernelMode = method is InstallMethod.AnyKernel
            if (isAnyKernelMode) {
                onInstall(LkmSelection.KmiNone)
                return@onNext
            }
            if (method is InstallMethod.DownloadFile) {
                onInstall(lkmSelection)
                return@onNext
            }
            if (targetKmiSource == InstallKmiSource.Detecting) {
                showMessage(resources.getString(R.string.install_kmi_detecting))
                return@onNext
            }
            val customLkm = lkmSelection.takeIf {
                it is LkmSelection.LkmUri
            }
            if (customLkm == null && targetKmi.isNotBlank() &&
                supportedKmis.isNotEmpty() && targetKmi !in supportedKmis
            ) {
                showMessage(
                    resources.getString(R.string.install_current_kmi_unsupported, targetKmi)
                )
                return@onNext
            }
            if (customLkm == null && targetKmi.isBlank()) {
                installAfterKmiSelection = true
                showChooseKmiDialog.value = true
            } else {
                onInstall(customLkm ?: LkmSelection.KmiString(targetKmi))
            }
        },
        onAdvancedOptionsClicked = {
            advancedOptionsShown = !advancedOptionsShown
        },
        onSelectAllowShell = {
            allowShell = it
        },
        onSelectEnableAdb = {
            enableAdb = it
        },
        onSelectForceBackup = {
            forceBackup = it
        },
    )

    when (uiMode) {
        UiMode.Miuix -> InstallScreenMiuix(state, actions, miuixSnackbarHost)
        UiMode.Material -> InstallScreenMaterial(state, actions, materialSnackbarHost)
    }
}

private suspend fun <T> loadInstallState(defaultValue: T, block: suspend () -> T): T {
    return try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        defaultValue
    }
}

private fun persistReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}

private fun releaseReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.releasePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}

private fun InstallMethod?.sourceUri(): Uri? = when (this) {
    is InstallMethod.SelectFile -> uri
    is InstallMethod.AnyKernel -> uri
    else -> null
}

private val BOOT_IMAGE_MIME_TYPES = arrayOf(
    "application/octet-stream",
    "application/x-raw-disk-image",
    "*/*",
)

private val LKM_MIME_TYPES = arrayOf("application/octet-stream", "*/*")

private val ANYKERNEL_MIME_TYPES = arrayOf(
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream",
)
