package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.alpha.AlphaBottomBar
import me.weishu.kernelsu.ui.component.delta.DeltaBottomBar
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproBottomBar
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
        if (targetIndex in 1..2 && !fullFeatured) return
        if (targetIndex == selectedPage) return

        navJob?.cancel()

        selectedPage = targetIndex
        isNavigating = true

        val distance = abs(targetIndex - pagerState.currentPage).coerceAtLeast(1)
        val duration = (175 + distance * 45).coerceIn(220, 320)
        val layoutInfo = pagerState.layoutInfo
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val currentDistanceInPages = targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction
        val scrollPixels = currentDistanceInPages * pageSize

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.animateScrollBy(
                    value = scrollPixels,
                    animationSpec = tween(easing = FastOutSlowInEasing, durationMillis = duration)
                )
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }

    fun updateFeatureAvailability(available: Boolean) {
        fullFeatured = available
        if (!available && selectedPage in 1..2) {
            animateToPage(0)
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): MainPagerState {
    return remember(pagerState, coroutineScope) {
        MainPagerState(pagerState, coroutineScope)
    }
}

@Composable
fun BottomBar(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
) {
    if (!LocalMainPagerState.current.fullFeatured) return

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

    when (LocalUiMode.current) {
        UiMode.Miuix -> BottomBarMiuix(blurBackdrop, backdrop, modifier)
        UiMode.Material -> BottomBarMaterial()
    }
}

@Composable
fun SideRail(
    blurBackdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    if (!LocalMainPagerState.current.fullFeatured) return

    when (LocalUiMode.current) {
        UiMode.Miuix -> NavigationRailMiuix(blurBackdrop, modifier)
        UiMode.Material -> NavigationRailMaterial(modifier)
    }
}
