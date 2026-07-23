package me.weishu.kernelsu.ui.screen.superuser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.viewmodel.SuperUserViewModel

@Composable
fun SuperUserPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true
) {
    val viewModel = viewModel<SuperUserViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasActivated by remember { mutableStateOf(false) }
    if (isCurrentPage) hasActivated = true

    if (hasActivated) {
        val latestUiState by rememberUpdatedState(uiState)
        LifecycleResumeEffect(Unit) {
            viewModel.initializePreferences()
            val state = latestUiState
            when {
                !state.hasLoaded || state.error != null -> viewModel.loadAppList(force = true)
                viewModel.isNeedRefresh -> viewModel.loadAppList(resort = false)
            }
            onPauseOrDispose {}
        }
    }

    val onSearchTextChange: (String) -> Unit = viewModel::updateSearchText
    val onToggleShowSystemApps: () -> Unit = {
        viewModel.toggleShowSystemApps()
    }
    val onToggleShowOnlyPrimaryUserApps: () -> Unit = {
        viewModel.toggleShowOnlyPrimaryUserApps()
    }
    val onOpenProfile: (GroupedApps) -> Unit = fun(group: GroupedApps) {
        if (navigator.current() is Route.AppProfile) return
        navigator.push(Route.AppProfile(group.uid))
        viewModel.markNeedRefresh()
    }
    val actions = SuperUserActions(
        onRefresh = { viewModel.loadAppList(force = true) },
        onOpenSulog = { navigator.push(Route.Sulog) },
        onOpenAppIdManager = { navigator.push(Route.AppIdManager) },
        onSearchTextChange = onSearchTextChange,
        onSearchStatusChange = viewModel::updateSearchStatus,
        onClearSearch = { onSearchTextChange("") },
        onToggleShowSystemApps = onToggleShowSystemApps,
        onToggleShowOnlyPrimaryUserApps = onToggleShowOnlyPrimaryUserApps,
        onUpdateSortOption = { viewModel.updateSortOption(it) },
        onOpenProfile = onOpenProfile,
    )

    when (LocalInterfaceStyle.current) {
        InterfaceStyle.Studio.value -> {
            SuperUserPagerStudio(
                uiState = uiState,
                actions = actions,
                bottomInnerPadding = bottomInnerPadding,
            )
            return
        }

        InterfaceStyle.Skrootpro.value -> {
            SuperUserPagerSkrootpro(
                uiState = uiState,
                actions = actions,
                bottomInnerPadding = bottomInnerPadding,
            )
            return
        }

        InterfaceStyle.Delta.value -> {
            SuperUserPagerDelta(
                uiState = uiState,
                actions = actions,
                bottomInnerPadding = bottomInnerPadding,
            )
            return
        }

        InterfaceStyle.Alpha.value -> {
            SuperUserPagerAlpha(
                uiState = uiState,
                actions = actions,
                bottomInnerPadding = bottomInnerPadding,
            )
            return
        }

        InterfaceStyle.Snow.value,
        InterfaceStyle.Rain.value,
        InterfaceStyle.Pixel.value -> {
            SuperUserPagerMiuix(
                uiState = uiState,
                actions = actions,
                bottomInnerPadding = bottomInnerPadding,
            )
            return
        }
    }

    SuperUserPagerMiuix(
        uiState = uiState,
        actions = actions,
        bottomInnerPadding = bottomInnerPadding,
    )
}
