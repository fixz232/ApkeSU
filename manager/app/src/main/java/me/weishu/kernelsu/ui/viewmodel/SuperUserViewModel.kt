package me.weishu.kernelsu.ui.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.data.model.AppInfo
import me.weishu.kernelsu.data.repository.SuperUserRepository
import me.weishu.kernelsu.data.repository.SuperUserRepositoryImpl
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.component.SearchStatus
import me.weishu.kernelsu.ui.screen.superuser.GroupedApps
import me.weishu.kernelsu.ui.screen.superuser.SuperUserUiState
import me.weishu.kernelsu.ui.util.HanziToPinyin
import me.weishu.kernelsu.ui.util.ownerNameForUid
import me.weishu.kernelsu.ui.util.pickPrimary
import java.text.Collator
import java.util.Locale

internal const val RECENTLY_INSTALLED_WINDOW_MILLIS = 60 * 60 * 1000L

internal const val SORT_BY_NAME = 0
internal const val SORT_BY_PACKAGE_NAME = 1
internal const val SORT_BY_INSTALL_TIME = 2
internal const val SORT_BY_UPDATE_TIME = 3

private const val PREFS_SORT_OPTION = "superuser_sort_option"

internal fun buildRecentlyInstalledGroups(
    groups: List<GroupedApps>,
    nowMillis: Long = System.currentTimeMillis(),
): List<GroupedApps> {
    val cutoffMillis = nowMillis - RECENTLY_INSTALLED_WINDOW_MILLIS

    return groups.mapNotNull { group ->
        val latestInstallTime = group.apps.maxOfOrNull { it.packageInfo.firstInstallTime } ?: return@mapNotNull null
        if (latestInstallTime < cutoffMillis) {
            null
        } else {
            group to latestInstallTime
        }
    }.sortedWith(
        compareByDescending<Pair<GroupedApps, Long>> { it.second }
            .thenBy { it.first.primary.label.lowercase() }
    ).map { it.first }
}

class SuperUserViewModel(
    private val repo: SuperUserRepository = SuperUserRepositoryImpl()
) : ViewModel() {

    companion object {
        private const val TAG = "SuperUserViewModel"

        private val appsLock = Any()
        private var cachedApps: List<AppInfo> = emptyList()
        private val groupedAppsLock = Any()
        private var cachedGroupedApps: List<GroupedApps> = emptyList()

        val apps: List<AppInfo>
            get() = synchronized(appsLock) { cachedApps }

        @JvmStatic
        fun getGroupedApp(uid: Int): GroupedApps? {
            return synchronized(groupedAppsLock) { cachedGroupedApps.find { it.uid == uid } }
        }
    }

    private val _uiState = MutableStateFlow(SuperUserUiState())
    val uiState: StateFlow<SuperUserUiState> = _uiState.asStateFlow()
    private val prefs = ksuApp.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val refreshMutex = Mutex()
    private val searchQuery = MutableStateFlow("")
    private var sortJob: Job? = null
    var isNeedRefresh = false
        private set

    init {
        viewModelScope.launchSearchQueryCollector(searchQuery, ::applySearchText)
    }

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    fun initializePreferences() {
        val showSystemApps = prefs.getBoolean("show_system_apps", false)
        val showOnlyPrimaryUserApps = prefs.getBoolean("show_only_primary_user_apps", false)
        val sortOption = prefs.getInt(PREFS_SORT_OPTION, 0)
        _uiState.update {
            it.copy(
                showSystemApps = showSystemApps,
                showOnlyPrimaryUserApps = showOnlyPrimaryUserApps,
                sortOption = sortOption,
            )
        }
    }

    fun updateSortOption(option: Int): Job {
        prefs.edit { putInt(PREFS_SORT_OPTION, option) }
        _uiState.update { it.copy(sortOption = option) }
        sortJob?.cancel()
        return viewModelScope.launch {
            val current = _uiState.value.groupedApps
            if (current.isEmpty()) return@launch
            updateVisibleApps(current)
        }.also { sortJob = it }
    }

    private fun refilterVisibleApps(): Job = viewModelScope.launch {
        // Re-filter when a filter setting changes
        val grouped = withContext(Dispatchers.IO) {
            buildGroups(filterAndSort(apps))
        }
        updateVisibleApps(grouped)
    }

    fun toggleShowSystemApps(): Job {
        val newValue = !_uiState.value.showSystemApps
        prefs.edit { putBoolean("show_system_apps", newValue) }
        _uiState.update { it.copy(showSystemApps = newValue) }
        return refilterVisibleApps()
    }

    fun toggleShowOnlyPrimaryUserApps(): Job {
        val newValue = !_uiState.value.showOnlyPrimaryUserApps
        prefs.edit { putBoolean("show_only_primary_user_apps", newValue) }
        _uiState.update { it.copy(showOnlyPrimaryUserApps = newValue) }
        return refilterVisibleApps()
    }

    fun updateSearchStatus(status: SearchStatus) {
        val previous = _uiState.value.searchStatus
        _uiState.update { it.copy(searchStatus = status) }
        if (previous.searchText != status.searchText) {
            searchQuery.value = status.searchText
        }
    }

    fun updateSearchText(text: String) {
        updateSearchStatus(_uiState.value.searchStatus.copy(searchText = text))
    }

    private fun filterSearchResults(groups: List<GroupedApps>, text: String): List<GroupedApps> {
        if (text.isEmpty()) return emptyList()

        return groups.mapNotNull { group ->
            val uidMatched = group.uid.toString().contains(text, true)
            val matchedPackageNames = group.apps.filter {
                uidMatched ||
                it.label.contains(text, true) ||
                        it.packageName.contains(text, true) ||
                        HanziToPinyin.getInstance().toPinyinString(it.label).contains(text, true)
            }.mapTo(linkedSetOf()) { it.packageName }

            if (matchedPackageNames.isEmpty()) {
                null
            } else {
                val sortedApps = group.apps.sortedWith(
                    compareByDescending { it.packageName in matchedPackageNames }
                )
                group.copy(
                    apps = sortedApps,
                    matchedPackageNames = matchedPackageNames,
                )
            }
        }
    }

    private suspend fun applySearchText(text: String) {
        _uiState.update {
            it.copy(
                searchStatus = it.searchStatus.copy(
                    resultStatus = searchLoadingStatusFor(text)
                )
            )
        }

        if (text.isEmpty()) {
            _uiState.update { state ->
                state.copy(
                    searchResults = emptyList(),
                    searchStatus = state.searchStatus.copy(resultStatus = SearchStatus.ResultStatus.DEFAULT)
                )
            }
            return
        }

        val result = withContext(Dispatchers.IO) {
            filterSearchResults(_uiState.value.groupedApps, text)
        }

        _uiState.update {
            it.copy(
                searchResults = result,
                searchStatus = it.searchStatus.copy(resultStatus = searchResultStatusFor(text, result.isEmpty()))
            )
        }
    }

    private fun updateCachedGroupedApps(grouped: List<GroupedApps>) {
        synchronized(groupedAppsLock) {
            cachedGroupedApps = grouped
        }
    }

    private suspend fun updateVisibleApps(grouped: List<GroupedApps>, resort: Boolean = true) {
        val sortOption = _uiState.value.sortOption
        val searchText = _uiState.value.searchStatus.searchText
        val (sorted, searchResults, recentlyInstalledResults) = withContext(Dispatchers.IO) {
            val s = if (resort) sortGroups(grouped, sortOption) else grouped
            Triple(s, filterSearchResults(s, searchText), buildRecentlyInstalledGroups(s))
        }
        _uiState.update {
            it.copy(
                groupedApps = sorted,
                recentlyInstalledResults = recentlyInstalledResults,
                searchResults = searchResults,
                searchStatus = it.searchStatus.copy(
                    resultStatus = searchResultStatusFor(searchText, searchResults.isEmpty())
                )
            )
        }
    }

    private fun filterAndSort(list: List<AppInfo>): List<AppInfo> {
        val comparator = compareBy<AppInfo> {
            when {
                it.allowSu -> 0
                it.hasCustomProfile -> 1
                else -> 2
            }
        }.then(compareBy(Collator.getInstance(Locale.getDefault()), AppInfo::label))

        val currentState = _uiState.value

        return list.filter {
            if (it.packageName == ksuApp.packageName) return@filter false
            if (it.allowSu || it.hasCustomProfile) {
                return@filter true
            }
            val userFilter = !currentState.showOnlyPrimaryUserApps || it.uid / 100000 == 0
            val appInfo = it.packageInfo.applicationInfo ?: return@filter false
            val isSystemApp = appInfo.flags.and(ApplicationInfo.FLAG_SYSTEM) != 0
            val typeFilter = it.uid == 2000
                    || currentState.showSystemApps
                    || !isSystemApp
            userFilter && typeFilter
        }.sortedWith(comparator)
    }

    private fun buildCachedGroups(apps: List<AppInfo>): List<GroupedApps> {
        return buildGroups(apps.filter { it.packageName != ksuApp.packageName })
    }

    private fun buildGroups(
        apps: List<AppInfo>,
        umount: (Int) -> Boolean = {
            runCatching { Natives.uidShouldUmount(it) }.getOrDefault(false)
        },
    ): List<GroupedApps> {
        val collator = Collator.getInstance(Locale.getDefault())
        val comparator = compareBy<AppInfo> {
            when {
                it.allowSu -> 0
                it.hasCustomProfile -> 1
                else -> 2
            }
        }.then(Comparator { a, b -> collator.compare(a.label, b.label) })
        return apps.groupBy { it.uid }.map { (uid, list) ->
            val sorted = list.sortedWith(comparator)
            val primary = pickPrimary(sorted)
            val shouldUmount = umount(uid)
            val ownerName = if (sorted.size > 1) ownerNameForUid(uid, sorted) else null

            GroupedApps(
                uid = uid,
                apps = sorted,
                primary = primary,
                anyAllowSu = sorted.any { it.allowSu },
                anyCustom = sorted.any { it.hasCustomProfile },
                shouldUmount = shouldUmount,
                ownerName = ownerName
            )
        }
    }

    private fun groupRank(group: GroupedApps): Int = when {
        group.anyAllowSu -> 0
        group.anyCustom -> 1
        group.apps.size > 1 -> 2
        group.shouldUmount -> 4
        else -> 3
    }

    private fun sortGroups(groups: List<GroupedApps>, sortOption: Int): List<GroupedApps> {
        val sortType = sortOption / 2
        val reverse = sortOption % 2 != 0

        val collator = Collator.getInstance(Locale.getDefault())
        val base: Comparator<GroupedApps> = when (sortType) {
            SORT_BY_PACKAGE_NAME -> compareBy { it.primary.packageName }
            SORT_BY_INSTALL_TIME -> compareBy { it.primary.packageInfo.firstInstallTime }
            SORT_BY_UPDATE_TIME -> compareBy { it.primary.packageInfo.lastUpdateTime }
            else -> Comparator { a, b -> collator.compare(a.primary.label, b.primary.label) }
        }
        val secondary = if (reverse) base.reversed() else base

        return groups.sortedWith(Comparator { a, b ->
            val ra = groupRank(a)
            val rb = groupRank(b)
            if (ra != rb) ra - rb else secondary.compare(a, b)
        })
    }

    suspend fun fetchAppList() {
        refreshMutex.withLock {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            repo.getAppList().onSuccess { (newApps, ids) ->
                val (cachedGroups, grouped) = withContext(Dispatchers.IO) {
                    val cached = buildCachedGroups(newApps)
                    val umountByUid = cached.associate { it.uid to it.shouldUmount }
                    cached to buildGroups(filterAndSort(newApps)) {
                        umountByUid[it] ?: runCatching { Natives.uidShouldUmount(it) }.getOrDefault(false)
                    }
                }

                // Update cache for static method
                synchronized(appsLock) { cachedApps = newApps }
                updateCachedGroupedApps(cachedGroups)
                updateVisibleApps(grouped)
                _uiState.update { it.copy(userIds = ids, isRefreshing = false, hasLoaded = true) }
            }.onFailure { e ->
                Log.e(TAG, "fetchAppList failed", e)
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        hasLoaded = true,
                        error = e
                    )
                }
            }

            isNeedRefresh = false
        }
    }

    private suspend fun refreshAppList(resort: Boolean = true) {
        refreshMutex.withLock {
            val currentApps = synchronized(appsLock) { cachedApps }
            if (currentApps.isEmpty()) return

            repo.refreshProfiles(currentApps).onSuccess { updatedApps ->
                // Update cache for static method
                synchronized(appsLock) { cachedApps = updatedApps }

                val (cachedGroups, grouped) = withContext(Dispatchers.IO) {
                    val cached = buildCachedGroups(updatedApps)
                    val umountByUid = cached.associate { it.uid to it.shouldUmount }
                    val visible = buildGroups(filterAndSort(updatedApps)) {
                        umountByUid[it] ?: runCatching { Natives.uidShouldUmount(it) }.getOrDefault(false)
                    }
                    val result = if (resort) {
                        visible
                    } else {
                        val byUid = visible.associateBy { it.uid }
                        _uiState.value.groupedApps.map { group ->
                            byUid[group.uid] ?: group.copy(
                                shouldUmount = runCatching {
                                    Natives.uidShouldUmount(group.uid)
                                }.getOrDefault(false)
                            )
                        }
                    }
                    cached to result
                }
                updateCachedGroupedApps(cachedGroups)

                updateVisibleApps(grouped, resort = resort)
                _uiState.update { it.copy(isRefreshing = false) }
                isNeedRefresh = false
            }
        }
    }

    fun loadAppList(force: Boolean = false, resort: Boolean = true): Job {
        return viewModelScope.launch {
            if (force || _uiState.value.groupedApps.isEmpty()) {
                fetchAppList()
            } else {
                refreshAppList(resort)
            }
        }
    }

}
