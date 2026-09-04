package me.weishu.kernelsu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.weishu.kernelsu.data.repository.DynamicManagerCandidate
import me.weishu.kernelsu.data.repository.DynamicManagerRepository
import me.weishu.kernelsu.ui.util.DynamicManagerCliState

sealed interface DynamicManagerNotice {
    data object Granted : DynamicManagerNotice
    data object Configured : DynamicManagerNotice
    data object Revoked : DynamicManagerNotice
    data class Failed(val detail: String) : DynamicManagerNotice
}

data class DynamicManagerUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val submittingPackage: String? = null,
    val query: String = "",
    val runtime: DynamicManagerCliState = DynamicManagerCliState(),
    val candidates: List<DynamicManagerCandidate> = emptyList(),
    val loadError: String? = null,
    val notice: DynamicManagerNotice? = null,
) {
    val busy: Boolean get() = submittingPackage != null
    val visibleCandidates: List<DynamicManagerCandidate>
        get() {
            val normalized = query.trim()
            if (normalized.isEmpty()) return candidates
            return candidates.filter { candidate ->
                candidate.label.contains(normalized, ignoreCase = true) ||
                    candidate.packageName.contains(normalized, ignoreCase = true) ||
                    candidate.appId.toString() == normalized
            }
        }
}

class DynamicManagerViewModel(
    private val repository: DynamicManagerRepository = DynamicManagerRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(DynamicManagerUiState())
    val uiState: StateFlow<DynamicManagerUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true || _uiState.value.busy) return
        refreshJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = it.candidates.isEmpty(),
                    refreshing = true,
                    loadError = null,
                )
            }
            repository.load()
                .onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            runtime = snapshot.runtime,
                            candidates = snapshot.candidates,
                            loadError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            loadError = error.message.orEmpty().ifBlank { "Dynamic Manager status is unavailable" },
                        )
                    }
                }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query.take(160)) }
    }

    fun grant(candidate: DynamicManagerCandidate) {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(submittingPackage = candidate.packageName, notice = null) }
            repository.grant(candidate)
                .onSuccess {
                    _uiState.update { it.copy(notice = DynamicManagerNotice.Granted) }
                    reloadAfterOperation()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            submittingPackage = null,
                            notice = DynamicManagerNotice.Failed(error.message.orEmpty()),
                        )
                    }
                }
        }
    }

    fun setManual(certificateSize: Int, certificateSha256: String) {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(submittingPackage = MANUAL_SUBMISSION, notice = null) }
            repository.setManual(certificateSize, certificateSha256)
                .onSuccess {
                    _uiState.update { it.copy(notice = DynamicManagerNotice.Configured) }
                    reloadAfterOperation()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            submittingPackage = null,
                            notice = DynamicManagerNotice.Failed(error.message.orEmpty()),
                        )
                    }
                }
        }
    }

    fun revoke() {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(submittingPackage = "", notice = null) }
            repository.revoke()
                .onSuccess {
                    _uiState.update { it.copy(notice = DynamicManagerNotice.Revoked) }
                    reloadAfterOperation()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            submittingPackage = null,
                            notice = DynamicManagerNotice.Failed(error.message.orEmpty()),
                        )
                    }
                }
        }
    }

    fun consumeNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private suspend fun reloadAfterOperation() {
        repository.load()
            .onSuccess { snapshot ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        submittingPackage = null,
                        runtime = snapshot.runtime,
                        candidates = snapshot.candidates,
                        loadError = null,
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        submittingPackage = null,
                        loadError = error.message.orEmpty(),
                    )
                }
            }
    }

    private companion object {
        const val MANUAL_SUBMISSION = "@manual"
    }
}
