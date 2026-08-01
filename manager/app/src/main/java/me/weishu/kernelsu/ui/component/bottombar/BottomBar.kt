package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.alpha.AlphaBottomBar
import me.weishu.kernelsu.ui.component.delta.DeltaBottomBar
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproBottomBar
import me.weishu.kernelsu.ui.util.shouldShowSplitPane
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import kotlin.math.abs

class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    var fullFeatured by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex !in 0 until pagerState.pageCount) return
        if (targetIndex in 1..2 && !fullFeatured) return
        if (targetIndex == selectedPage) return

        navJob?.cancel()

        selectedPage = targetIndex
        isNavigating = true

        val distance = abs(targetIndex - pagerState.currentPage).coerceAtLeast(1)
        val duration = (175 + distance * 45).coerceIn(220, 320)
        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                // Let Pager resolve its measured page size. A manual pixel offset can be
                // zero before the first layout pass, leaving the selected page unchanged.
                pagerState.animateScrollToPage(
                    page = targetIndex,
                    animationSpec = tween(easing = FastOutSlowInEasing, durationMillis = duration)
                )
            } finally {
                if (navJob == myJob) {
                    if (
                        shouldSnapMainPagerToTarget(
                            targetPage = targetIndex,
                            currentPage = pagerState.currentPage,
                            currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
                        )
                    ) {
                        withContext(NonCancellable) {
                            runCatching { pagerState.scrollToPage(targetIndex) }
                        }
                    }
                    isNavigating = false
                    selectedPage = pagerState.currentPage
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }

    fun updateFeatureAvailability(available: Boolean?) {
        val resolvedAvailability = available ?: return
        fullFeatured = resolvedAvailability
        if (shouldResetMainPagerForFeatureAvailability(resolvedAvailability, selectedPage)) {
            animateToPage(0)
        }
    }
}

internal fun shouldResetMainPagerForFeatureAvailability(
    available: Boolean?,
    selectedPage: Int,
): Boolean = available == false && selectedPage in 1..2

internal fun shouldSnapMainPagerToTarget(
    targetPage: Int,
    currentPage: Int,
    currentPageOffsetFraction: Float,
): Boolean = targetPage != currentPage ||
    !currentPageOffsetFraction.isFinite() ||
    abs(currentPageOffsetFraction) > MAIN_PAGER_SETTLED_EPSILON

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): MainPagerState {
    return remember(pagerState, coroutineScope) {
        MainPagerState(pagerState, coroutineScope)
    }
}

private const val MAIN_PAGER_SETTLED_EPSILON = 0.001f

@Immutable
data class NavigationBadgeState(
    val superuserCount: Int = 0,
    val moduleEnabledCount: Int = 0,
    val moduleUpdatableCount: Int = 0,
)

internal enum class BadgeTone { Alert, Accent }

@Immutable
internal data class NavBadge(val count: Int, val tone: BadgeTone)

internal fun badgeFor(index: Int, state: NavigationBadgeState): NavBadge? = when (index) {
    BottomBarDestination.SuperUser.ordinal ->
        state.superuserCount.takeIf { it > 0 }?.let { NavBadge(it, BadgeTone.Accent) }

    BottomBarDestination.Module.ordinal -> when {
        state.moduleUpdatableCount > 0 -> NavBadge(state.moduleUpdatableCount, BadgeTone.Alert)
        state.moduleEnabledCount > 0 -> NavBadge(state.moduleEnabledCount, BadgeTone.Accent)
        else -> null
    }

    else -> null
}

@Composable
fun useNavigationRail(enableFloatingBottomBar: Boolean): Boolean {
    return shouldShowSplitPane() && !(LocalUiMode.current == UiMode.Miuix && enableFloatingBottomBar)
}

@Composable
fun BottomBar(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop?,
    navigationBadge: NavigationBadgeState = NavigationBadgeState(),
    modifier: Modifier = Modifier,
) {
    if (!LocalMainPagerState.current.fullFeatured) return

    if (LocalUiMode.current == UiMode.Material) {
        BottomBarMaterial(navigationBadge)
        return
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        val mainState = LocalMainPagerState.current
        SkrootproBottomBar(
            selectedIndex = mainState.selectedPage,
            onSelected = mainState::animateToPage,
            modifier = modifier,
        )
        return
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Alpha.value) {
        val mainState = LocalMainPagerState.current
        AlphaBottomBar(
            selectedIndex = mainState.selectedPage,
            onSelected = mainState::animateToPage,
            modifier = modifier,
        )
        return
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Delta.value) {
        val mainState = LocalMainPagerState.current
        DeltaBottomBar(
            selectedIndex = mainState.selectedPage,
            onSelected = mainState::animateToPage,
            modifier = modifier,
        )
        return
    }

    BottomBarMiuix(blurBackdrop, backdrop, navigationBadge, modifier)
}

@Composable
fun SideRail(
    blurBackdrop: LayerBackdrop?,
    navigationBadge: NavigationBadgeState = NavigationBadgeState(),
    modifier: Modifier = Modifier,
) {
    if (!LocalMainPagerState.current.fullFeatured) return

    if (LocalUiMode.current == UiMode.Material) {
        NavigationRailMaterial(navigationBadge, modifier)
        return
    }

    NavigationRailMiuix(blurBackdrop, navigationBadge, modifier)
}
