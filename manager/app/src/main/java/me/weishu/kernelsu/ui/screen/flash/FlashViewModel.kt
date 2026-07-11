package me.weishu.kernelsu.ui.screen.flash

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.util.FlashResult

@Immutable
data class FlashExecutionState(
    val started: Boolean = false,
    val text: String = "",
    val log: String = "",
    val showRebootAction: Boolean = false,
    val status: FlashingStatus = FlashingStatus.FLASHING,
)

class FlashViewModel : ViewModel() {
    private val startLock = Any()
    private val _state = MutableStateFlow(FlashExecutionState())
    val state: StateFlow<FlashExecutionState> = _state.asStateFlow()

    fun start(
        action: FlashIt,
        flashErrorCode: String,
        flashCheckLog: String,
    ) {
        synchronized(startLock) {
            if (_state.value.started) return
            _state.update { it.copy(started = true) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val visibleLog = StringBuilder(_state.value.text)
            val fullLog = StringBuilder(_state.value.log)
            val outputLock = Any()
            val result = runCatching {
                flashIt(
                    action,
                    onStdout = { line ->
                        synchronized(outputLock) {
                            val output = "$line\n"
                            if (output.startsWith("\u001b[H\u001b[J")) {
                                visibleLog.clear().append(output.substring(6))
                            } else {
                                visibleLog.append(output)
                            }
                            fullLog.append(line).append('\n')
                            publishProgress(visibleLog, fullLog)
                        }
                    },
                    onStderr = { line ->
                        synchronized(outputLock) {
                            fullLog.append(line).append('\n')
                            _state.update { it.copy(log = fullLog.toString()) }
                        }
                    },
                )
            }.getOrElse { throwable ->
                val message = throwable.localizedMessage ?: throwable.javaClass.simpleName
                fullLog.append(message).append('\n')
                FlashResult(1, message, false)
            }

            if (result.code != 0) {
                visibleLog.append("$flashErrorCode: ${result.code}.\n ${result.err} $flashCheckLog\n")
            }
            if (result.showReboot) {
                visibleLog.append("\n\n\n")
            }

            _state.update {
                it.copy(
                    text = visibleLog.toString(),
                    log = fullLog.toString(),
                    showRebootAction = result.showReboot,
                    status = if (result.code == 0) FlashingStatus.SUCCESS else FlashingStatus.FAILED,
                )
            }
        }
    }

    fun markInterrupted(message: String) {
        synchronized(startLock) {
            if (_state.value.started) return
            val output = "$message\n"
            _state.value = FlashExecutionState(
                started = true,
                text = output,
                log = output,
                status = FlashingStatus.FAILED,
            )
        }
    }

    private fun publishProgress(visibleLog: StringBuilder, fullLog: StringBuilder) {
        _state.update {
            it.copy(
                text = visibleLog.toString(),
                log = fullLog.toString(),
            )
        }
    }
}
