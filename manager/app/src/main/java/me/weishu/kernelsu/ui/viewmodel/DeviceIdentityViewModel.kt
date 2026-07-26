package me.weishu.kernelsu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.util.DeviceIdentifierKind
import me.weishu.kernelsu.ui.util.DeviceIdentityActionResult
import me.weishu.kernelsu.ui.util.DeviceIdentityException
import me.weishu.kernelsu.ui.util.DeviceIdentityFailure
import me.weishu.kernelsu.ui.util.DeviceIdentityRepository
import me.weishu.kernelsu.ui.util.DeviceIdentitySnapshot

data class DeviceIdentityUiState(
    val loading: Boolean = true,
    val busyKind: DeviceIdentifierKind? = null,
    val restoringAll: Boolean = false,
    val snapshot: DeviceIdentitySnapshot = DeviceIdentitySnapshot(),
    val failure: DeviceIdentityFailure? = null,
)

class DeviceIdentityViewModel(
    private val repository: DeviceIdentityRepository = DeviceIdentityRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeviceIdentityUiState())
    val uiState: StateFlow<DeviceIdentityUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true || _uiState.value.busyKind != null || _uiState.value.restoringAll) {
            return
        }
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, failure = null) }
            try {
                val snapshot = repository.getSnapshot()
                _uiState.update { it.copy(loading = false, snapshot = snapshot) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        snapshot = it.snapshot.copy(
                            rootAvailable = false,
                            error = error.message.orEmpty(),
                        ),
                        failure = (error as? DeviceIdentityException)?.failure
                            ?: DeviceIdentityFailure.CommandFailed,
                    )
                }
            }
        }
    }

    suspend fun applyIdentifier(
        kind: DeviceIdentifierKind,
        value: String,
    ): DeviceIdentityActionResult = runOperation(kind) {
        repository.applyIdentifier(kind, value)
    }

    suspend fun restoreIdentifier(kind: DeviceIdentifierKind): DeviceIdentityActionResult =
        runOperation(kind) {
            repository.restoreIdentifier(kind)
        }

    suspend fun restoreAll(): DeviceIdentityActionResult {
        if (_uiState.value.busyKind != null || _uiState.value.restoringAll) {
            return DeviceIdentityActionResult(
                success = false,
                snapshot = _uiState.value.snapshot,
                failure = DeviceIdentityFailure.CommandFailed,
                detail = "operation_busy",
            )
        }
        _uiState.update { it.copy(restoringAll = true, failure = null) }
        return try {
            repository.restoreAll().also(::applyResult)
        } finally {
            _uiState.update { it.copy(restoringAll = false) }
        }
    }

    private suspend fun runOperation(
        kind: DeviceIdentifierKind,
        operation: suspend () -> DeviceIdentityActionResult,
    ): DeviceIdentityActionResult {
        if (_uiState.value.busyKind != null || _uiState.value.restoringAll) {
            return DeviceIdentityActionResult(
                success = false,
                snapshot = _uiState.value.snapshot,
                failure = DeviceIdentityFailure.CommandFailed,
                detail = "operation_busy",
            )
        }
        _uiState.update { it.copy(busyKind = kind, failure = null) }
        return try {
            operation().also(::applyResult)
        } finally {
            _uiState.update { it.copy(busyKind = null) }
        }
    }

    private fun applyResult(result: DeviceIdentityActionResult) {
        _uiState.update {
            it.copy(
                loading = false,
                snapshot = result.snapshot,
                failure = result.failure,
            )
        }
    }
}
