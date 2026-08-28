package me.weishu.kernelsu.ui.viewmodel

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.weishu.kernelsu.data.model.AppInfo
import me.weishu.kernelsu.data.repository.SuperUserRepository
import me.weishu.kernelsu.data.repository.SuperUserRepositoryImpl
import me.weishu.kernelsu.ui.util.AppIdException
import me.weishu.kernelsu.ui.util.AppIdFailure
import me.weishu.kernelsu.ui.util.AppIdLookup
import me.weishu.kernelsu.ui.util.AppIdManagerRepository
import me.weishu.kernelsu.ui.util.AppIdSnapshot
import me.weishu.kernelsu.ui.util.PinyinUtil
import java.text.Collator
import java.util.Locale

@Immutable
data class AppIdAppGroup(
    val uid: Int,
    val apps: List<AppInfo>,
    val primary: AppInfo,
) {
    val packageNames: Set<String>
        get() = apps.mapTo(linkedSetOf()) { it.packageName }

    val isSystemOnly: Boolean
        get() = apps.all { app ->
            val flags = app.packageInfo.applicationInfo?.flags ?: 0
            flags and ApplicationInfo.FLAG_SYSTEM != 0
        }
}

sealed interface AppIdNotice {
    data object Staged : AppIdNotice
    data object Restored : AppIdNotice
    data object PendingCanceled : AppIdNotice
    data class BatchStaged(val changedCount: Int, val requestedCount: Int) : AppIdNotice
}

@Immutable
data class AppIdManagerUiState(
    val groups: List<AppIdAppGroup> = emptyList(),
    val snapshots: Map<Int, AppIdSnapshot> = emptyMap(),
    val actionErrors: Map<Int, AppIdFailure> = emptyMap(),
    val query: String = "",
    val showSystemApps: Boolean = false,
    val loadingApps: Boolean = true,
    val listError: AppIdFailure? = null,
    val selected: AppIdAppGroup? = null,
    val selectedSnapshot: AppIdSnapshot? = null,
    val selectedError: AppIdFailure? = null,
    val loadingSelection: Boolean = false,
    val busy: Boolean = false,
    val actionUid: Int? = null,
    val draft: String = "",
    val notice: AppIdNotice? = null,
)

class AppIdManagerViewModel(
    private val appRepository: SuperUserRepository = SuperUserRepositoryImpl(),
    private val idRepository: AppIdManagerRepository = AppIdManagerRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppIdManagerUiState())
    val uiState: StateFlow<AppIdManagerUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null
    private var selectionJob: Job? = null

    init {
        refreshApps()
    }

    fun refreshApps() {
        if (refreshJob?.isActive == true || _uiState.value.busy) return
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(loadingApps = true, listError = null) }
            try {
                val apps = appRepository.getAppList().getOrThrow().first
                val collator = Collator.getInstance(Locale.getDefault())
                val groups = apps
                    .groupBy { it.uid }
                    .mapNotNull { (uid, groupedApps) ->
                        val sorted = groupedApps.sortedWith { first, second ->
                            collator.compare(first.label, second.label)
                        }
                        val primary = sorted.firstOrNull() ?: return@mapNotNull null
                        AppIdAppGroup(uid = uid, apps = sorted, primary = primary)
                    }
                    .sortedWith { first, second -> collator.compare(first.primary.label, second.primary.label) }
                val previousUid = _uiState.value.selected?.uid
                val selected = groups.firstOrNull { it.uid == previousUid }
                _uiState.update {
                    it.copy(
                        groups = groups,
                        snapshots = emptyMap(),
                        actionErrors = emptyMap(),
                        selected = selected,
                        selectedSnapshot = null,
                        selectedError = null,
                        draft = "",
                    )
                }
                val snapshots = idRepository.getSnapshots(
                    groups.map { group ->
                        AppIdLookup(
                            uid = group.uid,
                            packageName = group.primary.packageName,
                            knownPackages = group.packageNames,
                        )
                    },
                )
                val selectedSnapshot = selected?.let { snapshots[it.uid] }
                _uiState.update {
                    it.copy(
                        snapshots = snapshots,
                        loadingApps = false,
                        selectedSnapshot = selectedSnapshot,
                        draft = selectedSnapshot?.pendingId ?: selectedSnapshot?.currentId.orEmpty(),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val failure = error.toAppIdFailure()
                _uiState.update {
                    it.copy(
                        loadingApps = false,
                        listError = failure,
                        selectedSnapshot = null,
                        selectedError = if (it.selected != null) failure else null,
                        loadingSelection = false,
                        draft = "",
                    )
                }
            }
        }
    }

    fun updateQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun setShowSystemApps(value: Boolean) {
        _uiState.update { it.copy(showSystemApps = value) }
    }

    fun select(group: AppIdAppGroup) {
        if (_uiState.value.busy) return
        val snapshot = _uiState.value.snapshots[group.uid]
        _uiState.update {
            it.copy(
                selected = group,
                selectedSnapshot = snapshot,
                selectedError = it.actionErrors[group.uid],
                loadingSelection = snapshot == null,
                draft = snapshot?.pendingId ?: snapshot?.currentId.orEmpty(),
            )
        }
        if (snapshot == null) loadSelection(group)
    }

    fun closeSelection() {
        if (_uiState.value.busy) return
        selectionJob?.cancel()
        _uiState.update {
            it.copy(
                selected = null,
                selectedSnapshot = null,
                selectedError = null,
                loadingSelection = false,
                draft = "",
            )
        }
    }

    fun refreshSelection() {
        if (_uiState.value.busy) return
        _uiState.value.selected?.let(::loadSelection)
    }

    fun updateDraft(value: String) {
        _uiState.update { it.copy(draft = value.trim().take(64)) }
    }

    fun generateRandomId() {
        _uiState.update { state ->
            val snapshot = state.selectedSnapshot
            state.copy(
                draft = idRepository.generateRandomAppId(
                    setOfNotNull(snapshot?.currentId, snapshot?.pendingId),
                ),
            )
        }
    }

    fun randomReset(group: AppIdAppGroup) {
        val state = _uiState.value
        if (state.busy) return
        selectionJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    busy = true,
                    actionUid = group.uid,
                    actionErrors = it.actionErrors - group.uid,
                    selectedError = if (it.selected?.uid == group.uid) null else it.selectedError,
                    notice = null,
                )
            }
            try {
                val snapshot = idRepository.randomReset(
                    uid = group.uid,
                    packageName = group.primary.packageName,
                    knownPackages = group.packageNames,
                )
                _uiState.update {
                    it.copy(
                        busy = false,
                        actionUid = null,
                        snapshots = it.snapshots + (group.uid to snapshot),
                        selectedSnapshot = if (it.selected?.uid == group.uid) snapshot else it.selectedSnapshot,
                        selectedError = if (it.selected?.uid == group.uid) null else it.selectedError,
                        draft = if (it.selected?.uid == group.uid) {
                            snapshot.pendingId ?: snapshot.currentId.orEmpty()
                        } else {
                            it.draft
                        },
                        notice = AppIdNotice.Staged,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val failure = error.toAppIdFailure()
                _uiState.update {
                    it.copy(
                        busy = false,
                        actionUid = null,
                        actionErrors = it.actionErrors + (group.uid to failure),
                        selectedError = if (it.selected?.uid == group.uid) failure else it.selectedError,
                    )
                }
            }
        }
    }

    fun randomResetBatch(groups: List<AppIdAppGroup>) {
        val targets = groups.distinctBy(AppIdAppGroup::uid)
        if (targets.isEmpty() || _uiState.value.busy) return
        selectionJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    busy = true,
                    actionUid = null,
                    actionErrors = it.actionErrors - targets.map(AppIdAppGroup::uid).toSet(),
                    notice = null,
                )
            }
            var changed = 0
            var snapshots = _uiState.value.snapshots
            var errors = _uiState.value.actionErrors
            targets.forEach { group ->
                try {
                    val snapshot = idRepository.randomReset(
                        uid = group.uid,
                        packageName = group.primary.packageName,
                        knownPackages = group.packageNames,
                    )
                    snapshots = snapshots + (group.uid to snapshot)
                    errors = errors - group.uid
                    changed++
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    errors = errors + (group.uid to error.toAppIdFailure())
                }
                _uiState.update {
                    it.copy(snapshots = snapshots, actionErrors = errors)
                }
            }
            _uiState.update {
                val selectedSnapshot = it.selected?.let { group -> snapshots[group.uid] }
                it.copy(
                    busy = false,
                    actionUid = null,
                    snapshots = snapshots,
                    actionErrors = errors,
                    selectedSnapshot = selectedSnapshot ?: it.selectedSnapshot,
                    draft = selectedSnapshot?.pendingId ?: selectedSnapshot?.currentId ?: it.draft,
                    notice = AppIdNotice.BatchStaged(changed, targets.size),
                )
            }
        }
    }

    fun stageDraft() {
        runSelectedAction(AppIdNotice.Staged) { group, draft ->
            idRepository.stageAppId(
                uid = group.uid,
                packageName = group.primary.packageName,
                knownPackages = group.packageNames,
                newValue = draft,
            )
        }
    }

    fun restoreOriginal() {
        runSelectedAction(AppIdNotice.Restored) { group, _ ->
            idRepository.restoreOriginal(
                uid = group.uid,
                packageName = group.primary.packageName,
                knownPackages = group.packageNames,
            )
        }
    }

    fun cancelPending() {
        runSelectedAction(AppIdNotice.PendingCanceled) { group, _ ->
            idRepository.cancelPending(
                uid = group.uid,
                packageName = group.primary.packageName,
                knownPackages = group.packageNames,
            )
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun loadSelection(group: AppIdAppGroup) {
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(loadingSelection = true, selectedError = null)
            }
            try {
                val snapshot = idRepository.getSnapshot(
                    uid = group.uid,
                    packageName = group.primary.packageName,
                    knownPackages = group.packageNames,
                )
                if (_uiState.value.selected?.uid != group.uid) return@launch
                _uiState.update {
                    it.copy(
                        selectedSnapshot = snapshot,
                        snapshots = it.snapshots + (group.uid to snapshot),
                        actionErrors = it.actionErrors - group.uid,
                        loadingSelection = false,
                        draft = snapshot.pendingId ?: snapshot.currentId.orEmpty(),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (_uiState.value.selected?.uid == group.uid) {
                    _uiState.update {
                        it.copy(loadingSelection = false, selectedError = error.toAppIdFailure())
                    }
                }
            }
        }
    }

    private fun runSelectedAction(
        notice: AppIdNotice,
        action: suspend (AppIdAppGroup, String) -> AppIdSnapshot,
    ) {
        val state = _uiState.value
        val group = state.selected ?: return
        if (state.busy) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    busy = true,
                    actionUid = group.uid,
                    selectedError = null,
                    actionErrors = it.actionErrors - group.uid,
                    notice = null,
                )
            }
            try {
                val snapshot = action(group, state.draft)
                if (_uiState.value.selected?.uid != group.uid) return@launch
                _uiState.update {
                    it.copy(
                        busy = false,
                        actionUid = null,
                        selectedSnapshot = snapshot,
                        snapshots = it.snapshots + (group.uid to snapshot),
                        selectedError = null,
                        draft = snapshot.pendingId ?: snapshot.currentId.orEmpty(),
                        notice = notice,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val failure = error.toAppIdFailure()
                _uiState.update {
                    it.copy(
                        busy = false,
                        actionUid = null,
                        selectedError = failure,
                        actionErrors = it.actionErrors + (group.uid to failure),
                    )
                }
            }
        }
    }

    private fun Throwable.toAppIdFailure(): AppIdFailure =
        (this as? AppIdException)?.failure ?: AppIdFailure.CommandFailed
}

internal fun filterAppIdGroups(
    groups: List<AppIdAppGroup>,
    query: String,
    showSystemApps: Boolean,
): List<AppIdAppGroup> {
    val normalizedQuery = query.trim()
    return groups.filter { group ->
        val typeMatches = showSystemApps || !group.isSystemOnly
        val queryMatches = normalizedQuery.isBlank() || group.apps.any { app ->
            app.label.contains(normalizedQuery, ignoreCase = true) ||
                app.packageName.contains(normalizedQuery, ignoreCase = true) ||
                PinyinUtil.toPinyin(app.label).contains(normalizedQuery, ignoreCase = true) ||
                group.uid.toString().contains(normalizedQuery)
        }
        typeMatches && queryMatches
    }
}
