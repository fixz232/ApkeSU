package me.weishu.kernelsu.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

const val AUTO_HIDE_NAVIGATION_BAR_KEY = "auto_hide_navigation_bar"
const val SCROLL_HIDE_NAVIGATION_BAR_KEY = "scroll_hide_navigation_bar"
const val NAVIGATION_BAR_IDLE_TIMEOUT_MILLIS = 3_000L

@Composable
fun rememberNavigationBarVisibilityState(
    enabled: Boolean,
    autoHideAfterInactivity: Boolean,
    hideOnScroll: Boolean,
): NavigationBarVisibilityState {
    val state = remember { NavigationBarVisibilityState() }
    val scrollThresholdPx = with(LocalDensity.current) { 20.dp.toPx() }

    SideEffect {
        state.updateConfig(
            enabled = enabled,
            autoHideAfterInactivity = autoHideAfterInactivity,
            hideOnScroll = hideOnScroll,
            scrollThresholdPx = scrollThresholdPx,
        )
    }

    LaunchedEffect(state.enabled, state.autoHideAfterInactivity, state.interactionVersion) {
        if (state.enabled && state.autoHideAfterInactivity) {
            delay(NAVIGATION_BAR_IDLE_TIMEOUT_MILLIS)
            state.hideAfterInactivity()
        }
    }

    return state
}

fun Modifier.navigationBarVisibilityController(
    state: NavigationBarVisibilityState,
): Modifier {
    if (!state.enabled) return this

    return this
        .pointerInput(state, state.autoHideAfterInactivity) {
            awaitEachGesture {
                awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
                state.onPointerInteraction()
            }
        }
        .then(if (state.hideOnScroll) Modifier.nestedScroll(state.nestedScrollConnection) else Modifier)
}

@Composable
fun AutoHidingNavigationBar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 150)) +
            expandVertically(
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Bottom,
            ) +
            slideInVertically(
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
            ) { height -> height / 3 },
        exit = fadeOut(tween(durationMillis = 140)) +
            shrinkVertically(
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Bottom,
            ) +
            slideOutVertically(
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
            ) { height -> height / 3 },
    ) {
        content()
    }
}

@Stable
class NavigationBarVisibilityState internal constructor() {
    var visible by mutableStateOf(true)
        private set

    var enabled by mutableStateOf(false)
        private set

    var autoHideAfterInactivity by mutableStateOf(false)
        private set

    var hideOnScroll by mutableStateOf(false)
        private set

    var interactionVersion by mutableIntStateOf(0)
        private set

    private var scrollThresholdPx = 20f
    private var accumulatedScroll = 0f
    private var scrollDirection = 0

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (source == NestedScrollSource.UserInput) {
                onScroll(consumed.y)
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val velocityY = consumed.y + available.y
            if (hideOnScroll && abs(velocityY) >= FLING_VELOCITY_THRESHOLD) {
                if (velocityY < 0f) hide() else reveal(resetIdleTimer = true)
            }
            return Velocity.Zero
        }
    }

    internal fun updateConfig(
        enabled: Boolean,
        autoHideAfterInactivity: Boolean,
        hideOnScroll: Boolean,
        scrollThresholdPx: Float,
    ) {
        val becameActive = enabled && !this.enabled
        val inactivityModeEnabled = autoHideAfterInactivity && !this.autoHideAfterInactivity
        this.enabled = enabled
        this.autoHideAfterInactivity = autoHideAfterInactivity
        this.hideOnScroll = hideOnScroll
        this.scrollThresholdPx = scrollThresholdPx.coerceAtLeast(1f)

        if (!enabled || (!autoHideAfterInactivity && !hideOnScroll)) {
            visible = true
            resetScrollAccumulator()
        } else if (becameActive || inactivityModeEnabled) {
            reveal(resetIdleTimer = true)
        }
    }

    internal fun onPointerInteraction() {
        if (enabled && autoHideAfterInactivity) {
            reveal(resetIdleTimer = true)
        }
    }

    fun reveal(resetIdleTimer: Boolean = false) {
        if (!enabled) return
        visible = true
        resetScrollAccumulator()
        if (resetIdleTimer && autoHideAfterInactivity) {
            interactionVersion++
        }
    }

    internal fun hideAfterInactivity() {
        if (enabled && autoHideAfterInactivity) hide()
    }

    private fun onScroll(deltaY: Float) {
        if (!enabled || !hideOnScroll || abs(deltaY) < MIN_SCROLL_DELTA) return

        val direction = if (deltaY < 0f) -1 else 1
        if (direction != scrollDirection) {
            accumulatedScroll = 0f
            scrollDirection = direction
        }
        accumulatedScroll += deltaY
        if (abs(accumulatedScroll) < scrollThresholdPx) return

        // Negative Y means browsing forward/down through vertically scrollable content.
        if (accumulatedScroll < 0f) {
            hide()
        } else {
            reveal(resetIdleTimer = true)
        }
        resetScrollAccumulator()
    }

    private fun hide() {
        visible = false
        resetScrollAccumulator()
    }

    private fun resetScrollAccumulator() {
        accumulatedScroll = 0f
        scrollDirection = 0
    }

    private companion object {
        const val MIN_SCROLL_DELTA = 0.5f
        const val FLING_VELOCITY_THRESHOLD = 850f
    }
}
