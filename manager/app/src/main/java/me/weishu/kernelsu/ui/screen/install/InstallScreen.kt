package me.weishu.kernelsu.ui.screen.install

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.getKernelVersion
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
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
import me.weishu.kernelsu.ui.util.isAbDevice
import me.weishu.kernelsu.ui.util.rootAvailable

@Composable
fun InstallScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val uiMode = LocalUiMode.current
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    var installMethod by rememberSaveable { mutableStateOf<InstallMethod?>(null) }
    var lkmSelection by rememberSaveable { mutableStateOf<LkmSelection>(LkmSelection.KmiNone) }
    var partitionSelectionIndex by rememberSaveable { mutableIntStateOf(0) }
    var hasCustomSelected by rememberSaveable { mutableStateOf(false) }
    var selectedPartitionName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedBootImageFileName by rememberSaveable { mutableStateOf("") }
    var hiddenPathImagePickerPending by rememberSaveable { mutableStateOf(false) }
    val showChooseKmiDialog = rememberSaveable { mutableStateOf(false) }
    var installAfterKmiSelection by rememberSaveable { mutableStateOf(false) }
    var advancedOptionsShown by rememberSaveable { mutableStateOf(false) }
    var allowShell by rememberSaveable { mutableStateOf(false) }
    var enableAdb by rememberSaveable { mutableStateOf(false) }

    val isOta = installMethod is InstallMethod.DirectInstallToInactiveSlot

    val currentKmi by produceState(initialValue = "") {
        value = loadInstallState("") { getCurrentKmi() }
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
    val hiddenPathLkmPatchSummary = stringResource(id = R.string.hidden_path_lkm_patch_summary)
    val installMethodOptions = remember(
        rootAvailable,
        isAbDevice,
        isGkiDevice,
        selectFileTip,
        selectFileTipNoGki,
        hiddenPathLkmPatchSummary,
    ) {
        buildList {
            add(InstallMethod.SelectFile(summary = if (isGkiDevice) selectFileTip else selectFileTipNoGki))
            add(InstallMethod.HiddenPathLkmPatch(summary = hiddenPathLkmPatchSummary))
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
        selectedBootImageFileName,
        selectedPartitionName,
    ) {
        if (partitions.isEmpty()) return@LaunchedEffect
        val preferredPartition = if (hasCustomSelected) {
            selectedPartitionName
        } else {
            inferPartitionFromImageName(selectedBootImageFileName) ?: defaultPartition
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

    fun selectPartitionForFileName(fileName: String) {
        if (hasCustomSelected || partitions.isEmpty()) return
        val inferred = inferPartitionFromImageName(fileName) ?: return
        val index = partitions.indexOf(inferred)
        if (index >= 0) {
            partitionSelectionIndex = index
        }
    }

    fun showMessage(message: String) {
        scope.launch {
            if (uiMode == UiMode.Material) {
                snackbarHost.showSnackbar(message)
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onInstall = { selectedLkm: LkmSelection ->
        installMethod?.let { method ->
            if (method is InstallMethod.SelectFile && method.uri == null) {
                return@let
            }
            if (method is InstallMethod.HiddenPathLkmPatch && method.uri == null) {
                return@let
            }
            if (method is InstallMethod.AnyKernel) {
                method.uri?.let { uri -> navigator.push(Route.Flash(FlashIt.FlashAnyKernel(uri))) }
                return@let
            }
            val patchLkm = if (method is InstallMethod.HiddenPathLkmPatch) {
                when (selectedLkm) {
                    is LkmSelection.PathMaskKmiString -> selectedLkm
                    else -> return@let
                }
            } else {
                selectedLkm
            }
            val selectedPartition = when (method) {
                is InstallMethod.SelectFile,
                is InstallMethod.HiddenPathLkmPatch,
                is InstallMethod.DirectInstall,
                is InstallMethod.DirectInstallToInactiveSlot -> partitions.getOrNull(partitionSelectionIndex)

                else -> null
            }
            val bootUri = when (method) {
                is InstallMethod.SelectFile -> method.uri
                is InstallMethod.HiddenPathLkmPatch -> method.uri
                else -> null
            }
            navigator.push(
                Route.Flash(
                    FlashIt.FlashBoot(
                        boot = bootUri,
                        lkm = patchLkm,
                        ota = method is InstallMethod.DirectInstallToInactiveSlot,
                        partition = selectedPartition,
                        allowShell = allowShell,
                        enableAdb = enableAdb,
                    )
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
                val selectedLkm = if (installMethod is InstallMethod.HiddenPathLkmPatch) {
                    LkmSelection.PathMaskKmiString(it)
                } else {
                    LkmSelection.KmiString(it)
                }
                lkmSelection = selectedLkm
                if (installAfterKmiSelection) {
                    onInstall(selectedLkm)
                    installAfterKmiSelection = false
                }
            }
        }
    )

    val selectLkmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                if (isKoFile(context, uri)) {
                    lkmSelection = LkmSelection.LkmUri(uri)
                } else {
                    lkmSelection = LkmSelection.KmiNone
                    showMessage(resources.getString(R.string.install_only_support_ko_file))
                }
            }
        }
    }
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val fileName = uri.getFileName(context)?.takeUnless { name -> name.isBlank() }
                    ?: uri.lastPathSegment
                    ?: if (isGkiDevice) selectFileTip else selectFileTipNoGki
                selectedBootImageFileName = fileName
                selectPartitionForFileName(fileName)
                installMethod = if (hiddenPathImagePickerPending) {
                    lkmSelection = LkmSelection.KmiNone
                    InstallMethod.HiddenPathLkmPatch(uri, summary = fileName)
                } else {
                    InstallMethod.SelectFile(uri, summary = fileName)
                }
                hiddenPathImagePickerPending = false
            }
        } else {
            hiddenPathImagePickerPending = false
        }
    }
    val selectAnyKernelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val fileName = uri.getFileName(context)?.takeUnless { name -> name.isBlank() }
                    ?: uri.lastPathSegment
                    ?: resources.getString(R.string.anykernel_install)
                installMethod = InstallMethod.AnyKernel(uri, summary = fileName)
            }
        }
    }

    val canInstall = when (val method = installMethod) {
        null -> false
        is InstallMethod.SelectFile -> method.uri != null
        is InstallMethod.HiddenPathLkmPatch -> method.uri != null
        is InstallMethod.AnyKernel -> method.uri != null
        is InstallMethod.DirectInstall,
        is InstallMethod.DirectInstallToInactiveSlot -> rootAvailable
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
                (installMethod is InstallMethod.SelectFile ||
                        installMethod is InstallMethod.HiddenPathLkmPatch ||
                        installMethod is InstallMethod.DirectInstall ||
                        installMethod is InstallMethod.DirectInstallToInactiveSlot),
        canInstall = canInstall,
        advancedOptionsShown = advancedOptionsShown,
        allowShell = allowShell,
        enableAdb = enableAdb,
    )
    val actions = InstallScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onSelectMethod = onSelectMethod@{ method ->
            if ((method is InstallMethod.DirectInstall ||
                    method is InstallMethod.DirectInstallToInactiveSlot) && !rootAvailable
            ) {
                showMessage(resources.getString(R.string.direct_install_root_required))
                return@onSelectMethod
            }
            installMethod = method
            if (method !is InstallMethod.SelectFile && method !is InstallMethod.HiddenPathLkmPatch) {
                selectedBootImageFileName = ""
            }
            if (method !is InstallMethod.HiddenPathLkmPatch && lkmSelection is LkmSelection.PathMaskKmiString) {
                lkmSelection = LkmSelection.KmiNone
            }
        },
        onSelectBootImage = {
            hiddenPathImagePickerPending = false
            selectImageLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/octet-stream"
                addCategory(Intent.CATEGORY_OPENABLE)
            })
        },
        onSelectHiddenPathLkmBootImage = {
            hiddenPathImagePickerPending = true
            selectImageLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/octet-stream"
                addCategory(Intent.CATEGORY_OPENABLE)
            })
        },
        onSelectHiddenPathKmi = {
            installAfterKmiSelection = false
            showChooseKmiDialog.value = true
        },
        onSelectAnyKernel = {
            selectAnyKernelLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/zip"
                addCategory(Intent.CATEGORY_OPENABLE)
            })
        },
        onUploadLkm = {
            selectLkmLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/octet-stream"
                addCategory(Intent.CATEGORY_OPENABLE)
            })
        },
        onClearLkm = { lkmSelection = LkmSelection.KmiNone },
        onSelectPartition = { index ->
            partitions.getOrNull(index)?.let { selectedPartition ->
                hasCustomSelected = true
                selectedPartitionName = selectedPartition
                partitionSelectionIndex = index
            }
        },
        onNext = {
            val method = installMethod
            val isHiddenPathMode = method is InstallMethod.HiddenPathLkmPatch
            val isLkmSelected = lkmSelection != LkmSelection.KmiNone
            val isKmiUnknown = currentKmi.isBlank()
            val isSelectFileMode = method is InstallMethod.SelectFile
            val isAnyKernelMode = method is InstallMethod.AnyKernel
            val needsHiddenPathKmi = isHiddenPathMode && lkmSelection !is LkmSelection.PathMaskKmiString
            if (!isAnyKernelMode && (needsHiddenPathKmi || (!isLkmSelected && (isSelectFileMode || isKmiUnknown)))) {
                installAfterKmiSelection = true
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

    when (LocalUiMode.current) {
        UiMode.Miuix -> InstallScreenMiuix(state, actions)
        UiMode.Material -> InstallScreenMaterial(state, actions, snackbarHost)
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

private fun inferPartitionFromImageName(fileName: String): String? {
    val normalized = fileName
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .lowercase()
        .replace('-', '_')

    return when {
        "vendor_boot" in normalized || "vendorboot" in normalized || "verboot" in normalized -> "vendor_boot"
        "init_boot" in normalized || "initboot" in normalized || "intboot" in normalized -> "init_boot"
        normalized == "boot.img" ||
                normalized.startsWith("boot.") ||
                normalized.startsWith("boot_") ||
                normalized.contains("_boot.") ||
                normalized.contains("_boot_") -> "boot"
        else -> null
    }
}
