package me.weishu.kernelsu.ui.viewmodel

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.weishu.kernelsu.data.model.AppInfo
import me.weishu.kernelsu.data.repository.SuperUserRepository
import me.weishu.kernelsu.data.repository.SuperUserRepositoryImpl
import me.weishu.kernelsu.ui.util.DEFAULT_FOREGROUND_TOOL_PACKAGES
import me.weishu.kernelsu.ui.util.ForegroundToolException
import me.weishu.kernelsu.ui.util.ForegroundToolFailure
import me.weishu.kernelsu.ui.util.ForegroundToolProtectionRepository
import me.weishu.kernelsu.ui.util.ForegroundToolStatus
import me.weishu.kernelsu.ui.util.PinyinUtil
import java.text.Collator
import java.util.Locale

private const val FOREGROUND_TOOL_STATUS_POLL_MILLIS = 2_500L
private const val ANDROID_UIDS_PER_USER = 100_000

@Immutable
data class ForegroundToolApp(
    val packageName: String,
    val label: String,
    val packageInfo: PackageInfo? = null,
    val isSystem: Boolean = false,
    val installed: Boolean = true,
) {
    val recommended: Boolean
        get() = packageName in DEFAULT_FOREGROUND_TOOL_PACKAGES
}

enum class ForegroundToolNotice {
    Enabled,
    Disabled,
    AutoDisabled,
    MovedFromTools,
    MovedFromTargets,
    LogCleared,
}

@Immutable
data class ForegroundToolProtectionUiState(
    val apps: List<ForegroundToolApp> = emptyList(),
    val targets: Set<String> = emptySet(),
    val tools: Set<String> = emptySet(),
    val status: ForegroundToolStatus = ForegroundToolStatus(),
    val loadingStatus: Boolean = true,
    val loadingApps: Boolean = true,
    val busy: Boolean = false,
    val appListFailed: Boolean = false,
    val statusStale: Boolean = false,
    val failure: ForegroundToolFailure? = null,
    val notice: ForegroundToolNotice? = null,
)

internal enum class ForegroundToolRole {
    Target,
    Tool,
}

internal data class ForegroundToolSelection(
    val targets: Set<String>,
    val tools: Set<String>,
    val conflictRemovedFrom: ForegroundToolRole? = null,
)

internal fun updateForegroundToolSelection(
    targets: Set<String>,
    tools: Set<String>,
    packageName: String,
    role: ForegroundToolRole,
    selected: Boolean,
): ForegroundToolSelection {
    if (!selected) {
        return when (role) {
            ForegroundToolRole.Target -> ForegroundToolSelection(targets - packageName, tools)
            ForegroundToolRole.Tool -> ForegroundToolSelection(targets, tools - packageName)
        }
    }
    return when (role) {
        ForegroundToolRole.Target -> ForegroundToolSelection(
            targets = targets + packageName,
            tools = tools - packageName,
            conflictRemovedFrom = ForegroundToolRole.Tool.takeIf { packageName in tools },
        )

        ForegroundToolRole.Tool -> ForegroundToolSelection(
            targets = targets - packageName,
            tools = tools + packageName,
            conflictRemovedFrom = ForegroundToolRole.Target.takeIf { packageName in targets },
        )
    }
}

internal fun buildForegroundToolApps(apps: List<AppInfo>): List<ForegroundToolApp> {
    val collator = Collator.getInstance(Locale.getDefault())
    return apps
        .groupBy(AppInfo::packageName)
        .mapNotNull { (packageName, candidates) ->
            val app = candidates.minWithOrNull(
                compareBy<AppInfo> { it.uid / ANDROID_UIDS_PER_USER }
                    .thenBy { it.uid },
            ) ?: return@mapNotNull null
            val flags = app.packageInfo.applicationInfo?.flags ?: 0
            ForegroundToolApp(
                packageName = packageName,
                label = app.label.ifBlank { packageName },
                packageInfo = app.packageInfo,
                isSystem = flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
            )
        }
        .sortedWith { first, second -> collator.compare(first.label, second.label) }
}

internal fun filterForegroundToolApps(
    apps: List<ForegroundToolApp>,
    selectedPackages: Set<String>,
    query: String,
    showSystemApps: Boolean,
): List<ForegroundToolApp> {
    val installedPackages = apps.mapTo(hashSetOf(), ForegroundToolApp::packageName)
    val missingSelections = selectedPackages
        .asSequence()
        .filterNot(installedPackages::contains)
        .sorted()
        .map { packageName ->
            ForegroundToolApp(
                packageName = packageName,
                label = packageName,
                installed = false,
            )
        }
    val normalizedQuery = query.trim()
    return (missingSelections + apps.asSequence())
        .filter { app ->
            val typeMatches = showSystemApps || !app.isSystem || app.packageName in selectedPackages
            val queryMatches = normalizedQuery.isBlank() ||
                app.label.contains(normalizedQuery, ignoreCase = true) ||
                app.packageName.contains(normalizedQuery, ignoreCase = true) ||
                PinyinUtil.toPinyin(app.label).contains(normalizedQuery, ignoreCase = true)
            typeMatches && queryMatches
        }
        .sortedWith(
            compareByDescending<ForegroundToolApp> { it.packageName in selectedPackages }
                .thenByDescending(ForegroundToolApp::recommended)
                .thenBy { it.label.lowercase(Locale.getDefault()) },
        )
        .toList()
}

class ForegroundToolProtectionViewModel(
    private val appRepository: SuperUserRepository = SuperUserRepositoryImpl(),
    private val protectionRepository: ForegroundToolProtectionRepository = ForegroundToolProtectionRepository(),
) : ViewModel() {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(ForegroundToolProtectionUiState())
    val uiState = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(FOREGROUND_TOOL_STATUS_POLL_MILLIS)
                refreshRuntimeStatus()
            }
        }
    }

    fun refresh() {
        if (_uiState.value.busy || refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loadingStatus = true,
                    loadingApps = true,
                    appListFailed = false,
                    statusStale = false,
                    failure = null,
                )
            }
            val status = try {
                protectionRepository.getStatus()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        loadingStatus = false,
                        statusStale = true,
                        failure = error.toForegroundToolFailure(),
                    )
                }
                null
            }
            if (status != null) {
                _uiState.update {
                    it.copy(
                        status = status,
                        targets = status.config.targets,
                        tools = status.config.tools,
                        loadingStatus = false,
                        statusStale = false,
                    )
                }
            }

            try {
                val apps = buildForegroundToolApps(appRepository.getAppList().getOrThrow().first)
                _uiState.update { current ->
                    val defaultTools = DEFAULT_FOREGROUND_TOOL_PACKAGES
                        .filterTo(linkedSetOf()) { packageName -> apps.any { it.packageName == packageName } }
                    current.copy(
                        apps = apps,
                        tools = if (current.status.configPresent) current.tools else defaultTools,
                        loadingApps = false,
                        appListFailed = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                _uiState.update { it.copy(loadingApps = false, appListFailed = true) }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        val current = _uiState.value
        if (current.busy || current.loadingStatus) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, failure = null, notice = null) }
            try {
                val status = protectionRepository.setEnabled(enabled, current.targets, current.tools)
                _uiState.update {
                    it.copy(
                        status = status,
                        targets = status.config.targets,
                        tools = status.config.tools,
                        busy = false,
                        statusStale = false,
                        notice = if (enabled) ForegroundToolNotice.Enabled else ForegroundToolNotice.Disabled,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update { it.copy(busy = false, failure = error.toForegroundToolFailure()) }
            }
        }
    }

    fun setTargetSelected(packageName: String, selected: Boolean) {
        val current = _uiState.value
        val next = updateForegroundToolSelection(
            current.targets,
            current.tools,
            packageName,
            ForegroundToolRole.Target,
            selected,
        )
        persistSelection(next)
    }

    fun setToolSelected(packageName: String, selected: Boolean) {
        val current = _uiState.value
        val next = updateForegroundToolSelection(
            current.targets,
            current.tools,
            packageName,
            ForegroundToolRole.Tool,
            selected,
        )
        persistSelection(next)
    }

    fun clearLog() {
        if (_uiState.value.busy || refreshJob?.isActive == true) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, failure = null, notice = null) }
            try {
                val status = protectionRepository.clearLog()
                _uiState.update {
                    it.copy(
                        status = status,
                        busy = false,
                        statusStale = false,
                        notice = ForegroundToolNotice.LogCleared,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update { it.copy(busy = false, failure = error.toForegroundToolFailure()) }
            }
        }
    }

    fun consumeNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun persistSelection(selection: ForegroundToolSelection) {
        val current = _uiState.value
        if (current.busy || refreshJob?.isActive == true ||
            (selection.targets == current.targets && selection.tools == current.tools)
        ) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, failure = null, notice = null) }
            try {
                val status = protectionRepository.saveConfig(selection.targets, selection.tools)
                val autoDisabled = current.status.config.enabled && !status.config.enabled
                val notice = when {
                    autoDisabled -> ForegroundToolNotice.AutoDisabled
                    selection.conflictRemovedFrom == ForegroundToolRole.Tool -> ForegroundToolNotice.MovedFromTools
                    selection.conflictRemovedFrom == ForegroundToolRole.Target -> ForegroundToolNotice.MovedFromTargets
                    else -> null
                }
                _uiState.update {
                    it.copy(
                        status = status,
                        targets = status.config.targets,
                        tools = status.config.tools,
                        busy = false,
                        statusStale = false,
                        notice = notice,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update { it.copy(busy = false, failure = error.toForegroundToolFailure()) }
            }
        }
    }

    private suspend fun refreshRuntimeStatus() {
        val current = _uiState.value
        if (current.busy || current.loadingStatus || refreshJob?.isActive == true) return
        try {
            val status = protectionRepository.getStatus()
            _uiState.update {
                val recoveredFromStaleStatus = it.statusStale
                it.copy(
                    status = status,
                    targets = if (recoveredFromStaleStatus) status.config.targets else it.targets,
                    tools = if (recoveredFromStaleStatus && status.configPresent) {
                        status.config.tools
                    } else {
                        it.tools
                    },
                    statusStale = false,
                    failure = if (recoveredFromStaleStatus) null else it.failure,
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            _uiState.update { it.copy(statusStale = true) }
        }
    }

    private fun Throwable.toForegroundToolFailure(): ForegroundToolFailure =
        (this as? ForegroundToolException)?.failure ?: ForegroundToolFailure.CommandFailed
}
