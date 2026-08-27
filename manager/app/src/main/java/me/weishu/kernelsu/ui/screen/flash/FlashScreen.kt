package me.weishu.kernelsu.ui.screen.flash

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.repository.isSoftRebootPreferred
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.KernelStatusEvents
import me.weishu.kernelsu.ui.util.reboot

@Composable
fun FlashScreen(flashIt: FlashIt) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val materialSnackbarHost = remember { androidx.compose.material3.SnackbarHostState() }
    val flashViewModel = viewModel<FlashViewModel>()
    val executionState by flashViewModel.state.collectAsStateWithLifecycle()
    val needJailbreakWarning = flashIt.needsJailbreakFlashWarning() && Natives.isLateLoadMode
    val softReboot = flashIt is FlashIt.FlashModules && isSoftRebootPreferred()
    var flashingEnabled by rememberSaveable { mutableStateOf(!needJailbreakWarning) }
    var operationRequested by rememberSaveable(flashIt) { mutableStateOf(false) }
    var refreshSent by rememberSaveable(flashIt) { mutableStateOf(false) }
    val logSavedMessage = stringResource(R.string.log_saved)
    val logSaveFailedMessage = stringResource(R.string.log_save_failed)
    val flashErrorCode = stringResource(R.string.flash_error_code)
    val flashCheckLog = stringResource(R.string.flash_check_log)
    val flashInterrupted = stringResource(R.string.flash_interrupted)

    fun showMessage(message: String) {
        scope.launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(flashingEnabled, flashIt, executionState.started) {
        if (!flashingEnabled) return@LaunchedEffect
        if (!operationRequested) {
            operationRequested = true
            flashViewModel.start(flashIt, flashErrorCode, flashCheckLog)
        } else if (!executionState.started) {
            flashViewModel.markInterrupted(flashInterrupted)
        }
    }

    LaunchedEffect(executionState.status) {
        if (executionState.status == FlashingStatus.SUCCESS && !refreshSent) {
            refreshSent = true
            KernelStatusEvents.requestRefresh()
        }
    }

    val flashInProgress = executionState.started &&
        executionState.status == FlashingStatus.FLASHING
    BackHandler(enabled = flashInProgress) {
        // A partition write may continue after its UI coroutine is cancelled.
    }

    val state = FlashUiState(
        text = executionState.text,
        showRebootAction = executionState.showRebootAction,
        flashingStatus = executionState.status,
        showJailbreakWarning = needJailbreakWarning && !flashingEnabled,
        rebootLabelRes = if (softReboot) R.string.reboot_soft else R.string.reboot,
    )
    val actions = FlashScreenActions(
        onBack = dropUnlessResumed {
            if (!flashInProgress) navigator.pop()
        },
        onSaveLog = saveLog(context, executionState.log, scope, logSavedMessage, logSaveFailedMessage) {
            showMessage(it)
        },
        onReboot = {
            KernelStatusEvents.requestRefresh()
            scope.launch {
                withContext(Dispatchers.IO) {
                    reboot(if (softReboot) "soft_reboot" else "")
                }
            }
        },
        onConfirmJailbreakWarning = { flashingEnabled = true },
        onDismissJailbreakWarning = dropUnlessResumed { navigator.pop() },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> FlashScreenMiuix(state, actions)
        UiMode.Material -> FlashScreenMaterial(state, actions, materialSnackbarHost)
    }
}
