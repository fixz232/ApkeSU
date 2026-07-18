package me.weishu.kernelsu.ui.screen.install

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.getKernelVersion
import me.weishu.kernelsu.ui.component.choosekmidialog.ChooseKmiDialog
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.screen.flash.FlashIt
import me.weishu.kernelsu.ui.util.LkmSelection
import me.weishu.kernelsu.ui.util.getAvailablePartitions
import me.weishu.kernelsu.ui.util.getCurrentKmi
import me.weishu.kernelsu.ui.util.getDefaultPartition
import me.weishu.kernelsu.ui.util.getFileName
import me.weishu.kernelsu.ui.util.getSlotSuffix
import me.weishu.kernelsu.ui.util.getSupportedKmis
import me.weishu.kernelsu.ui.util.isAbDevice
import me.weishu.kernelsu.ui.util.rootAvailable

@Composable
fun InstallScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    var installMethod by rememberSaveable { mutableStateOf<InstallMethod?>(null) }
    var lkmSelection by rememberSaveable { mutableStateOf<LkmSelection>(LkmSelection.KmiNone) }
    var partitionSelectionIndex by rememberSaveable { mutableIntStateOf(0) }
    var hasCustomSelected by rememberSaveable { mutableStateOf(false) }
    var selectedPartitionName by rememberSaveable { mutableStateOf<String?>(null) }
    val showChooseKmiDialog = rememberSaveable { mutableStateOf(false) }
    var installAfterKmiSelection by rememberSaveable { mutableStateOf(false) }
    var selectingHiddenPathKmi by rememberSaveable { mutableStateOf(false) }
    var advancedOptionsShown by rememberSaveable { mutableStateOf(false) }
    var allowShell by rememberSaveable { mutableStateOf(false) }
    var enableAdb by rememberSaveable { mutableStateOf(false) }
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
    val installMethodOptions = remember(
        rootAvailable,
        isAbDevice,
        isGkiDevice,
        selectFileTip,
        selectFileTipNoGki,
    ) {
        buildList {
            add(InstallMethod.SelectFile(summary = if (isGkiDevice) selectFileTip else selectFileTipNoGki))
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
    val defaultIndex = remember(partitions, defaultPartition) {
        partitions.indexOf(defaultPartition).coerceAtLeast(0)
    }

    LaunchedEffect(
        partitions,
        defaultIndex,
        hasCustomSelected,
        selectedPartitionName,
    ) {
        if (partitions.isEmpty()) return@LaunchedEffect
        val preferredPartition = if (hasCustomSelected) {
            selectedPartitionName
        } else {
            defaultPartition
        }
        val preferredIndex = preferredPartition
            ?.let(partitions::indexOf)
            ?.takeIf { it >= 0 }

        if (hasCustomSelected && preferredIndex == null) {
            hasCustomSelected = false
            selectedPartitionName = null
        }
        partitionSelectionIndex = (preferredIndex ?: defaultIndex).coerceIn(0, partitions.lastIndex)
    }

    val displayPartitions = remember(partitions, defaultPartition) {
        partitions.map { name -> if (defaultPartition == name) "$name (default)" else name }
    }

    fun showMessage(message: String) {
        scope.launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
            val patchLkm = when {
                method is InstallMethod.SelectFile -> selectedLkm
                selectedLkm is LkmSelection.PathMaskKmiString -> LkmSelection.KmiNone
                else -> selectedLkm
            }
            val selectedPartition = when (method) {
                is InstallMethod.DirectInstall,
                is InstallMethod.DirectInstallToInactiveSlot -> partitions.getOrNull(partitionSelectionIndex)

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
                    ota = method is InstallMethod.DirectInstallToInactiveSlot,
                    partition = selectedPartition,
                    allowShell = allowShell,
                    enableAdb = enableAdb,
                )
            )
        }
    }

    ChooseKmiDialog(
        show = showChooseKmiDialog.value,
        onDismissRequest = {
            showChooseKmiDialog.value = false
            installAfterKmiSelection = false
            selectingHiddenPathKmi = false
        },
        onSelected = { kmi ->
            kmi?.let {
                val selectedLkm = if (selectingHiddenPathKmi) {
                    LkmSelection.PathMaskKmiString(it)
                } else {
                    LkmSelection.KmiString(it)
                }
                replaceLkmSelection(selectedLkm)
                if (installAfterKmiSelection) {
                    onInstall(selectedLkm)
                    installAfterKmiSelection = false
                }
                selectingHiddenPathKmi = false
            }
        }
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
        is InstallMethod.AnyKernel -> method.uri != null && rootAvailable
        is InstallMethod.DirectInstall -> rootAvailable
        is InstallMethod.DirectInstallToInactiveSlot -> rootAvailable &&
                (slotSuffix == "_a" || slotSuffix == "_b") && partitions.isNotEmpty()
    }

    val state = InstallUiState(
        installMethod = installMethod,
        lkmSelection = lkmSelection,
        partitionSelectionIndex = partitionSelectionIndex,
        displayPartitions = displayPartitions,
        currentKmi = currentKmi,
        slotSuffix = slotSuffix,
        installMethodOptions = installMethodOptions,
        rootAvailable = rootAvailable,
        canSelectPartition = displayPartitions.isNotEmpty() &&
                (installMethod is InstallMethod.DirectInstall ||
                        installMethod is InstallMethod.DirectInstallToInactiveSlot),
        canInstall = canInstall && !navigationLocked,
        hiddenPathLkmEnabled = lkmSelection is LkmSelection.PathMaskKmiString,
        advancedOptionsShown = advancedOptionsShown,
        allowShell = allowShell,
        enableAdb = enableAdb,
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
                if (lkmSelection is LkmSelection.KmiString ||
                    lkmSelection is LkmSelection.PathMaskKmiString
                ) {
                    replaceLkmSelection(LkmSelection.KmiNone)
                }
            }
        },
        onSelectBootImage = {
            selectImageLauncher.launch(BOOT_IMAGE_MIME_TYPES)
        },
        onSelectHiddenPathKmi = {
            installAfterKmiSelection = false
            selectingHiddenPathKmi = true
            showChooseKmiDialog.value = true
        },
        onSetHiddenPathLkmEnabled = { enabled ->
            if (enabled) {
                installAfterKmiSelection = false
                selectingHiddenPathKmi = true
                showChooseKmiDialog.value = true
            } else if (lkmSelection is LkmSelection.PathMaskKmiString) {
                replaceLkmSelection(LkmSelection.KmiNone)
            }
        },
        onSelectAnyKernel = {
            selectAnyKernelLauncher.launch(ANYKERNEL_MIME_TYPES)
        },
        onUploadLkm = {
            selectLkmLauncher.launch(LKM_MIME_TYPES)
        },
        onClearLkm = { replaceLkmSelection(LkmSelection.KmiNone) },
        onSelectPartition = { index ->
            partitions.getOrNull(index)?.let { selectedPartition ->
                hasCustomSelected = true
                selectedPartitionName = selectedPartition
                partitionSelectionIndex = index
            }
        },
        onNext = onNext@{
            val method = installMethod
            val isLkmSelected = lkmSelection != LkmSelection.KmiNone
            val isKmiUnknown = currentKmi.isBlank()
            val isSelectFileMode = method is InstallMethod.SelectFile
            val isAnyKernelMode = method is InstallMethod.AnyKernel
            val isDirectInstall = method is InstallMethod.DirectInstall ||
                    method is InstallMethod.DirectInstallToInactiveSlot
            val currentKmiUnsupported = currentKmi.isNotBlank() &&
                    supportedKmis.isNotEmpty() && currentKmi !in supportedKmis
            if (isDirectInstall && !isLkmSelected && currentKmiUnsupported) {
                showMessage(
                    resources.getString(R.string.install_current_kmi_unsupported, currentKmi)
                )
                return@onNext
            }
            if (!isAnyKernelMode && !isLkmSelected && (isSelectFileMode || isKmiUnknown)) {
                installAfterKmiSelection = true
                selectingHiddenPathKmi = false
                showChooseKmiDialog.value = true
            } else {
                onInstall(lkmSelection)
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
    )

    InstallScreenMiuix(state, actions)
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
