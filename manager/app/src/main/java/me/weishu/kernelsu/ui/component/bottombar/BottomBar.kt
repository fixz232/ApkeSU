package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.PagerNavigationSpringSpec
import me.weishu.kernelsu.ui.component.alpha.AlphaBottomBar
import me.weishu.kernelsu.ui.component.delta.DeltaBottomBar
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproBottomBar
import me.weishu.kernelsu.ui.util.CustomNavigationIconSet
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.CustomNavigationIconState
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.shouldShowSplitPane
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import kotlin.math.abs

class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
    private val pageCountState: MutableIntState,
    restoredDestination: MainDestination = MainDestination.Home,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    var fullFeatured by mutableStateOf(false)
        private set

    var kpmActive by mutableStateOf(false)
        private set

    private var navJob: Job? = null
    private var kpmReconfigurationJob: Job? = null
    private var navigationGeneration = 0L
    private var pendingRestoredDestination: MainDestination? = restoredDestination
    private var fullFeaturedUnavailableObservations = 0
    private var kpmInactiveObservations = 0

    fun animateToPage(targetIndex: Int) {
        if (targetIndex !in 0 until pagerState.pageCount) return
        if (targetIndex > 0 && !fullFeatured) return
        if (targetIndex == selectedPage && !isNavigating && pagerState.settledPage == targetIndex) return

        val generation = ++navigationGeneration
        navJob?.cancel()
        kpmReconfigurationJob?.cancel()
        kpmReconfigurationJob = null
        pendingRestoredDestination = null

        selectedPage = targetIndex
        isNavigating = true

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.springAnimateToPage(targetIndex)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                if (navigationGeneration == generation && navJob == myJob) {
                    isNavigating = false
                    selectedPage = pagerState.settledPage
                }
            }
        }
    }

    fun animateTo(destination: MainDestination) {
        mainDestinations(kpmActive)
            .indexOf(destination)
            .takeIf { it >= 0 }
            ?.let(::animateToPage)
    }

    fun syncSettledPage(page: Int = pagerState.settledPage): MainDestination {
        if (kpmReconfigurationJob?.isActive == true) {
            return destinationForPage(selectedPage)
        }
        if (page in 0 until pagerState.pageCount) {
            if (!isNavigating) {
                selectedPage = page
            }
        }
        return destinationForPage(selectedPage)
    }

    fun destinationForPage(page: Int = selectedPage): MainDestination {
        return mainDestinations(kpmActive).getOrNull(page) ?: MainDestination.Home
    }

    fun updateFeatureAvailability(available: Boolean?) {
        val resolvedAvailability = available ?: return
        if (resolvedAvailability) {
            fullFeaturedUnavailableObservations = 0
            fullFeatured = true
            return
        }
        if (!fullFeatured) return

        // A root/identity probe can fail for one refresh while the daemon is
        // restarting. Do not tear down the main navigation on that single miss.
        fullFeaturedUnavailableObservations++
        if (fullFeaturedUnavailableObservations < 2) return
        fullFeaturedUnavailableObservations = 0
        fullFeatured = false
        if (
            shouldResetMainPagerForFeatureAvailability(
                available = false,
                selectedPage = selectedPage,
                kpmActive = kpmActive,
            )
        ) {
            animateToPage(0)
        }
    }

    fun updateKpmAvailability(available: Boolean?) {
        val resolvedAvailability = available ?: return
        if (resolvedAvailability) {
            kpmInactiveObservations = 0
        } else {
            if (!kpmActive) return

            // KPatch-Next status is read through a root shell and can return a
            // transient incomplete result during boot or module refresh. Keep
            // the committed KPM destination until two probes agree it is gone.
            kpmInactiveObservations++
            if (kpmInactiveObservations < 2) return
            kpmInactiveObservations = 0
        }
        if (kpmActive == resolvedAvailability) return
        val currentPage = if (isNavigating || kpmReconfigurationJob?.isActive == true) {
            selectedPage
        } else {
            pagerState.settledPage
        }
        val currentDestination = destinationForPage(currentPage)
        val restoredDestination = pendingRestoredDestination
        val generation = ++navigationGeneration
        navJob?.cancel()
        navJob = null
        kpmReconfigurationJob?.cancel()
        isNavigating = false
        pageCountState.intValue = mainDestinations(resolvedAvailability).size
        kpmActive = resolvedAvailability
        val targetDestination = if (
            resolvedAvailability &&
            restoredDestination == MainDestination.Kpm &&
            currentDestination == MainDestination.Home
        ) {
            MainDestination.Kpm
        } else {
            currentDestination
        }
        pendingRestoredDestination = null
        val target = mainDestinations(resolvedAvailability).indexOf(targetDestination)
            .takeIf { it >= 0 }
            ?: 0
        selectedPage = target
        kpmReconfigurationJob = coroutineScope.launch {
            kotlinx.coroutines.yield()
            if (
                navigationGeneration == generation &&
                kpmActive == resolvedAvailability &&
                target in 0 until pagerState.pageCount
            ) {
                runCatching { pagerState.scrollToPage(target) }
                if (navigationGeneration == generation && kpmActive == resolvedAvailability) {
                    selectedPage = pagerState.settledPage
                }
            }
            if (navigationGeneration == generation) {
                kpmReconfigurationJob = null
            }
        }
    }
}

private suspend fun PagerState.springAnimateToPage(target: Int) {
    if (target !in 0 until pageCount) return

    var needsSnap = false
    scroll(MutatePriority.UserInput) {
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        if (pageSize <= 0) {
            needsSnap = true
            return@scroll
        }

        val currentOffset = currentPageOffsetFraction
        if (!currentOffset.isFinite()) {
            needsSnap = true
            return@scroll
        }
        val distance = target - currentPage - currentOffset
        val targetScroll = distance * pageSize
        if (abs(targetScroll) <= MAIN_PAGER_SCROLL_EPSILON) return@scroll

        var consumedScroll = 0f
        var skipScroll = false
        Animatable(0f).animateTo(
            targetValue = targetScroll,
            animationSpec = PagerNavigationSpringSpec,
        ) {
            if (skipScroll) return@animateTo

            val delta = value - consumedScroll
            if (abs(delta) > MAIN_PAGER_SCROLL_EPSILON) {
                val consumed = scrollBy(delta)
                consumedScroll += consumed
                if (abs(delta - consumed) > MAIN_PAGER_CONSUMPTION_EPSILON) {
                    skipScroll = true
                }
            } else {
                consumedScroll = value
            }

            if (abs(velocity) < MAIN_PAGER_VELOCITY_EPSILON &&
                abs(targetScroll - consumedScroll) < MAIN_PAGER_REMAINING_EPSILON
            ) {
                skipScroll = true
            }
        }

        val remaining = targetScroll - consumedScroll
        if (abs(remaining) > MAIN_PAGER_SCROLL_EPSILON) {
            scrollBy(remaining)
        }
    }

    if (
        needsSnap ||
        currentPage != target ||
        !currentPageOffsetFraction.isFinite() ||
        abs(currentPageOffsetFraction) > MAIN_PAGER_SETTLED_EPSILON
    ) {
        scrollToPage(target)
    }
}

internal fun shouldResetMainPagerForFeatureAvailability(
    available: Boolean?,
    selectedPage: Int,
    kpmActive: Boolean = false,
): Boolean {
    if (available != false) return false
    return when (mainDestinations(kpmActive).getOrNull(selectedPage)) {
        MainDestination.Kpm,
        MainDestination.SuperUser,
        MainDestination.Module,
        -> true
        else -> false
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    pageCountState: MutableIntState = androidx.compose.runtime.remember { mutableIntStateOf(4) },
    restoredDestination: MainDestination = MainDestination.Home,
): MainPagerState {
    // restoredDestination is only an initial hint. Re-keying this state on every
    // persisted destination update would cancel navigation and reset the pager.
    return remember(pagerState, coroutineScope, pageCountState) {
        MainPagerState(pagerState, coroutineScope, pageCountState, restoredDestination)
    }
}

private const val MAIN_PAGER_SETTLED_EPSILON = 0.001f
private const val MAIN_PAGER_SCROLL_EPSILON = 0.5f
private const val MAIN_PAGER_CONSUMPTION_EPSILON = 0.1f
private const val MAIN_PAGER_VELOCITY_EPSILON = 0.1f
private const val MAIN_PAGER_REMAINING_EPSILON = 1f

@Immutable
data class NavigationBadgeState(
    val superuserCount: Int = 0,
    val moduleCount: Int = 0,
)

internal enum class BadgeTone { Alert, Accent }

@Immutable
internal data class NavBadge(val count: Int, val tone: BadgeTone)

internal fun badgeFor(destination: MainDestination, state: NavigationBadgeState): NavBadge? = when (destination) {
    MainDestination.SuperUser ->
        state.superuserCount.takeIf { it > 0 }?.let { NavBadge(it, BadgeTone.Accent) }

    MainDestination.Module ->
        state.moduleCount.takeIf { it > 0 }?.let { NavBadge(it, BadgeTone.Accent) }

    else -> null
}

enum class MainDestination(
    @get:androidx.annotation.StringRes val label: Int,
    val icon: ImageVector,
    val slot: CustomNavigationIconSlot? = null,
) {
    Home(R.string.home, Icons.Rounded.Home, CustomNavigationIconSlot.Home),
    Kpm(R.string.kpm_short_title, Icons.Rounded.Memory, CustomNavigationIconSlot.Kpm),
    SuperUser(R.string.superuser, Icons.Rounded.Security, CustomNavigationIconSlot.Superuser),
    Module(R.string.module, Icons.Rounded.Extension, CustomNavigationIconSlot.Module),
    Settings(R.string.settings, Icons.Rounded.Settings, CustomNavigationIconSlot.Settings),
}

fun mainDestinations(kpmActive: Boolean): List<MainDestination> {
    return if (kpmActive) {
        listOf(
            MainDestination.Home,
            MainDestination.Kpm,
            MainDestination.SuperUser,
            MainDestination.Module,
            MainDestination.Settings,
        )
    } else {
        listOf(
            MainDestination.Home,
            MainDestination.SuperUser,
            MainDestination.Module,
            MainDestination.Settings,
        )
    }
}

internal fun CustomNavigationIconSet.stateFor(destination: MainDestination): CustomNavigationIconState {
    return destination.slot?.let(::get) ?: CustomNavigationIconState()
}

internal fun CustomNavigationIconSet.labelFor(destination: MainDestination, fallback: String): String {
    return stateFor(destination).displayLabel(fallback)
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
    val mainState = LocalMainPagerState.current
    if (!mainState.fullFeatured) return
    val destinations = mainDestinations(mainState.kpmActive)

    if (LocalUiMode.current == UiMode.Material) {
        BottomBarMaterial(navigationBadge, destinations)
        return
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        val mainState = LocalMainPagerState.current
        SkrootproBottomBar(
            selectedIndex = mainState.selectedPage,
            onSelected = mainState::animateToPage,
            destinations = destinations,
            modifier = modifier,
        )
        return
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Alpha.value) {
        val mainState = LocalMainPagerState.current
        AlphaBottomBar(
            selectedIndex = mainState.selectedPage,
            onSelected = mainState::animateToPage,
            destinations = destinations,
            modifier = modifier,
        )
        return
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Delta.value) {
        val mainState = LocalMainPagerState.current
        DeltaBottomBar(
            selectedIndex = mainState.selectedPage,
            onSelected = mainState::animateToPage,
            destinations = destinations,
            modifier = modifier,
        )
        return
    }

    BottomBarMiuix(blurBackdrop, backdrop, navigationBadge, destinations, modifier)
}

@Composable
fun SideRail(
    blurBackdrop: LayerBackdrop?,
    navigationBadge: NavigationBadgeState = NavigationBadgeState(),
    modifier: Modifier = Modifier,
) {
    val mainState = LocalMainPagerState.current
    if (!mainState.fullFeatured) return
    val destinations = mainDestinations(mainState.kpmActive)

    if (LocalUiMode.current == UiMode.Material) {
        NavigationRailMaterial(navigationBadge, destinations, modifier)
        return
    }

    NavigationRailMiuix(blurBackdrop, navigationBadge, destinations, modifier)
}
