package me.weishu.kernelsu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.AppFreezeException
import me.weishu.kernelsu.ui.util.AppFreezeFailure
import me.weishu.kernelsu.ui.util.AppFreezeKey
import me.weishu.kernelsu.ui.util.AppFreezer
import me.weishu.kernelsu.ui.util.FreezableApp

enum class AppFreezeFilter {
    All,
    Frozen,
    Active,
}

data class AppFreezeError(
    val failure: AppFreezeFailure,
    val detail: String = "",
)

sealed interface AppFreezeNotice {
    data class Changed(val label: String, val frozen: Boolean) : AppFreezeNotice
    data class BatchChanged(
        val changedCount: Int,
        val requestedCount: Int,
        val frozen: Boolean,
    ) : AppFreezeNotice
    data class Failed(
        val label: String,
        val failure: AppFreezeFailure,
        val detail: String,
    ) : AppFreezeNotice
}

data class AppFreezeUiState(
    val apps: List<FreezableApp> = emptyList(),
    val query: String = "",
    val filter: AppFreezeFilter = AppFreezeFilter.All,
    val showSystemApps: Boolean = false,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val busyKeys: Set<AppFreezeKey> = emptySet(),
    val loadError: AppFreezeError? = null,
    val notice: AppFreezeNotice? = null,
)

internal fun visibleAppFreezeApps(state: AppFreezeUiState): List<FreezableApp> {
    val query = state.query.trim()
    return state.apps.asSequence()
        .filter { app ->
            !app.systemApp || state.showSystemApps || app.frozen
        }
        .filter { app ->
            when (state.filter) {
                AppFreezeFilter.All -> true
                AppFreezeFilter.Frozen -> app.frozen
                AppFreezeFilter.Active -> !app.frozen
            }
        }
        .filter { app ->
            query.isBlank() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true) ||
                app.userId.toString() == query
        }
        .sortedWith(
            compareByDescending<FreezableApp> { it.frozen }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
                .thenBy { it.packageName }
                .thenBy { it.userId }
        )
        .toList()
}

class AppFreezeViewModel(
    private val freezer: AppFreezer = AppFreezer(ksuApp),
) : ViewModel() {
    private val refreshMutex = Mutex()
    private val _uiState = MutableStateFlow(AppFreezeUiState())
    val uiState: StateFlow<AppFreezeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshMutex.withLock {
                _uiState.update {
                    it.copy(
                        loading = it.apps.isEmpty(),
                        refreshing = true,
                        loadError = null,
                    )
                }
                freezer.loadApps()
                    .onSuccess { apps ->
                        _uiState.update {
                            it.copy(
                                apps = apps,
                                loading = false,
                                refreshing = false,
                                loadError = null,
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                loading = false,
                                refreshing = false,
                                loadError = error.toAppFreezeError(),
                            )
                        }
                    }
            }
        }
    }

    fun updateQuery(value: String) {
        _uiState.update { it.copy(query = value.take(160)) }
    }

    fun updateFilter(value: AppFreezeFilter) {
        _uiState.update { it.copy(filter = value) }
    }

    fun toggleSystemApps() {
        _uiState.update { it.copy(showSystemApps = !it.showSystemApps) }
    }

    fun setFrozen(app: FreezableApp, frozen: Boolean) {
        if (app.key in _uiState.value.busyKeys) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    busyKeys = it.busyKeys + app.key,
                    notice = null,
                )
            }
            freezer.setFrozen(app.key, frozen)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            apps = state.apps.map { current ->
                                if (current.key == updated.key) updated else current
                            },
                            busyKeys = state.busyKeys - app.key,
                            notice = AppFreezeNotice.Changed(updated.label, updated.frozen),
                        )
                    }
                }
                .onFailure { error ->
                    val failure = error.toAppFreezeError()
                    _uiState.update {
                        it.copy(
                            busyKeys = it.busyKeys - app.key,
                            notice = AppFreezeNotice.Failed(
                                label = app.label,
                                failure = failure.failure,
                                detail = failure.detail,
                            ),
                        )
                    }
                }
        }
    }

    fun setFrozenBatch(apps: List<FreezableApp>, frozen: Boolean) {
        val targets = apps
            .distinctBy(FreezableApp::key)
            .filter { app -> app.frozen != frozen && (!frozen || app.protection == null) }
        if (targets.isEmpty()) return
        val keys = targets.mapTo(linkedSetOf(), FreezableApp::key)
        if (_uiState.value.busyKeys.any(keys::contains)) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    busyKeys = it.busyKeys + keys,
                    notice = null,
                )
            }
            var changed = 0
            var firstFailure: Pair<FreezableApp, AppFreezeError>? = null
            targets.forEach { app ->
                freezer.setFrozen(app.key, frozen)
                    .onSuccess { updated ->
                        changed++
                        _uiState.update { state ->
                            state.copy(
                                apps = state.apps.map { current ->
                                    if (current.key == updated.key) updated else current
                                },
                                busyKeys = state.busyKeys - app.key,
                            )
                        }
                    }
                    .onFailure { error ->
                        val failure = error.toAppFreezeError()
                        if (firstFailure == null) firstFailure = app to failure
                        _uiState.update { state ->
                            state.copy(busyKeys = state.busyKeys - app.key)
                        }
                    }
            }
            _uiState.update { state ->
                val failure = firstFailure
                state.copy(
                    busyKeys = state.busyKeys - keys,
                    notice = if (failure == null) {
                        AppFreezeNotice.BatchChanged(
                            changedCount = changed,
                            requestedCount = targets.size,
                            frozen = frozen,
                        )
                    } else {
                        AppFreezeNotice.Failed(
                            label = failure.first.label,
                            failure = failure.second.failure,
                            detail = failure.second.detail,
                        )
                    },
                )
            }
        }
    }

    fun consumeNotice() {
        _uiState.update { it.copy(notice = null) }
    }
}

private fun Throwable.toAppFreezeError(): AppFreezeError {
    val typed = this as? AppFreezeException
    return AppFreezeError(
        failure = typed?.failure ?: AppFreezeFailure.CommandFailed,
        detail = message.orEmpty().trim().lineSequence().firstOrNull().orEmpty().take(240),
    )
}
