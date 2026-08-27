package me.weishu.kernelsu.ui.component

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

const val BACKGROUND_SCROLL_FOLLOW_ENABLED_KEY = "background_scroll_follow_enabled"

private const val BACKGROUND_SCROLL_FOLLOW_FACTOR = 0.12f
private const val BACKGROUND_SCROLL_FOLLOW_OVERSCAN = 1.12f
private const val BACKGROUND_SCROLL_OVERSCAN_SAFETY = 0.88f
private const val BACKGROUND_SCROLL_HORIZONTAL_TRAVEL_FACTOR = 0.035f
private const val BACKGROUND_SCROLL_VERTICAL_TRAVEL_FACTOR = 0.045f
private const val BACKGROUND_SCROLL_OVERSCROLL_FACTOR = 0.22f
private const val BACKGROUND_SCROLL_RESPONSE_PER_SECOND = 34f
private const val BACKGROUND_SCROLL_SETTLE_DISTANCE_PX = 0.12f
private const val DEFAULT_FRAME_SECONDS = 1f / 60f
private const val MIN_FRAME_SECONDS = 1f / 240f
private const val MAX_FRAME_SECONDS = 1f / 20f
private const val POINTER_SCROLL_SUPPRESSION_MILLIS = 72L

val LocalBackgroundScrollFollowState = staticCompositionLocalOf<BackgroundScrollFollowState?> { null }

@Composable
fun rememberBackgroundScrollFollowState(
    enabled: Boolean,
    resetKey: Any? = Unit,
    horizontalPagerDriven: Boolean = false,
): BackgroundScrollFollowState {
    val state = remember { BackgroundScrollFollowState() }
    val systemAnimationsEnabled = rememberSystemAnimationsEnabled()
    val effectiveEnabled = enabled && systemAnimationsEnabled

    SideEffect {
        state.updateConfig(effectiveEnabled, horizontalPagerDriven)
    }

    LaunchedEffect(state, resetKey) {
        state.resetImmediately()
    }

    LaunchedEffect(state, effectiveEnabled, horizontalPagerDriven) {
        if (!effectiveEnabled) {
            state.resetImmediately()
            return@LaunchedEffect
        }
        state.runMotionLoop()
    }

    return state
}

@Composable
fun Modifier.backgroundScrollFollowController(
    state: BackgroundScrollFollowState,
    pointerFallbackEnabled: Boolean = false,
): Modifier {
    if (!state.enabled) return this

    val density = LocalDensity.current
    val minimumTravel = with(density) { 24.dp.toPx() }
    val maximumTravel = with(density) { 48.dp.toPx() }

    return this
        .onSizeChanged { size ->
            state.updateViewport(size, minimumTravel, maximumTravel)
        }
        .then(
            if (pointerFallbackEnabled) {
                Modifier.pointerInput(state) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        var previousPosition = down.position
                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val current = event.changes.firstOrNull { it.pressed }
                            if (current != null) {
                                state.onPointerScroll(current.position - previousPosition)
                                previousPosition = current.position
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
            } else {
                Modifier
            }
        )
        .nestedScroll(state.nestedScrollConnection)
}

fun Modifier.backgroundScrollFollowMotion(
    state: BackgroundScrollFollowState?,
): Modifier {
    if (state?.enabled != true) return this

    return graphicsLayer {
        // Reading rapidly changing state in the layer block invalidates only the GPU layer.
        val offset = state.offset
        scaleX = BACKGROUND_SCROLL_FOLLOW_OVERSCAN
        scaleY = BACKGROUND_SCROLL_FOLLOW_OVERSCAN
        translationX = offset.x
        translationY = offset.y
    }
}

@Stable
class BackgroundScrollFollowState {
    var enabled by mutableStateOf(false)
        private set

    private var renderedOffset by mutableStateOf(Offset.Zero)
    private var targetOffset = Offset.Zero
    private var travelBounds = Offset.Zero
    private var horizontalPagerDriven = false
    private var lastNestedScrollMillis = 0L
    private val motionRequests = Channel<Unit>(capacity = Channel.CONFLATED)

    val offset: Offset
        get() = renderedOffset

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            val rawDelta = consumed + available * BACKGROUND_SCROLL_OVERSCROLL_FACTOR
            val delta = if (horizontalPagerDriven) {
                Offset(0f, rawDelta.y)
            } else {
                rawDelta
            }
            if (delta != Offset.Zero) {
                lastNestedScrollMillis = SystemClock.uptimeMillis()
                moveBy(delta)
            }
            return Offset.Zero
        }
    }

    fun updateConfig(enabled: Boolean, horizontalPagerDriven: Boolean = false) {
        val modeChanged = this.horizontalPagerDriven != horizontalPagerDriven
        this.horizontalPagerDriven = horizontalPagerDriven
        if (this.enabled == enabled && !modeChanged) return
        this.enabled = enabled
        if (!enabled || modeChanged) {
            lastNestedScrollMillis = 0L
            resetImmediately()
        }
    }

    fun updateViewport(
        size: IntSize,
        minimumTravelPx: Float,
        maximumTravelPx: Float,
    ) {
        val nextBounds = calculateBackgroundScrollTravelBounds(
            size = size,
            minimumVerticalTravelPx = minimumTravelPx,
            maximumVerticalTravelPx = maximumTravelPx,
        )
        if (travelBounds == nextBounds) return

        travelBounds = nextBounds
        targetOffset = targetOffset.coerceToTravel(travelBounds)
        renderedOffset = renderedOffset.coerceToTravel(travelBounds)
        requestMotionFrame()
    }

    fun onPointerScroll(delta: Offset) {
        if (!enabled) return
        val now = SystemClock.uptimeMillis()
        if (now - lastNestedScrollMillis <= POINTER_SCROLL_SUPPRESSION_MILLIS) return
        moveBy(if (horizontalPagerDriven) Offset(0f, delta.y) else delta)
    }

    /** Receives gesture-direction deltas from embedded scroll containers such as WebView. */
    fun onEmbeddedViewScroll(delta: Offset) {
        if (!enabled) return
        moveBy(if (horizontalPagerDriven) Offset(0f, delta.y) else delta)
    }

    fun resetImmediately() {
        targetOffset = Offset.Zero
        renderedOffset = Offset.Zero
        while (motionRequests.tryReceive().isSuccess) {
            // Discard stale work when changing pages or disabling the effect.
        }
    }

    suspend fun runMotionLoop() {
        while (enabled) {
            motionRequests.receive()
            var previousFrameNanos = 0L
            while (enabled) {
                val settled = withFrameNanos { frameNanos ->
                    val frameSeconds = if (previousFrameNanos == 0L) {
                        DEFAULT_FRAME_SECONDS
                    } else {
                        ((frameNanos - previousFrameNanos) / 1_000_000_000f)
                            .coerceIn(MIN_FRAME_SECONDS, MAX_FRAME_SECONDS)
                    }
                    previousFrameNanos = frameNanos
                    advanceFrame(frameSeconds)
                }
                if (settled) break
            }
        }
    }

    private fun moveBy(delta: Offset) {
        if (!enabled || travelBounds == Offset.Zero) return
        val nextTarget = (targetOffset + delta * BACKGROUND_SCROLL_FOLLOW_FACTOR)
            .coerceToTravel(travelBounds)
        if (targetOffset == nextTarget) return

        targetOffset = nextTarget
        requestMotionFrame()
    }

    private fun requestMotionFrame() {
        if (enabled && renderedOffset != targetOffset) {
            motionRequests.trySend(Unit)
        }
    }

    private fun advanceFrame(frameSeconds: Float): Boolean {
        val target = targetOffset
        if (renderedOffset.isNear(target, BACKGROUND_SCROLL_SETTLE_DISTANCE_PX)) {
            renderedOffset = target
            return true
        }

        renderedOffset = approachBackgroundScrollOffset(
            current = renderedOffset,
            target = target,
            frameSeconds = frameSeconds,
        )
        return false
    }
}

internal fun calculateBackgroundScrollTravelBounds(
    size: IntSize,
    minimumVerticalTravelPx: Float,
    maximumVerticalTravelPx: Float,
): Offset {
    if (size.width <= 0 || size.height <= 0) return Offset.Zero

    val minimumVertical = minimumVerticalTravelPx.coerceAtLeast(0f)
    val maximumVertical = maximumVerticalTravelPx.coerceAtLeast(minimumVertical)
    val overscanFraction = ((BACKGROUND_SCROLL_FOLLOW_OVERSCAN - 1f) / 2f) *
        BACKGROUND_SCROLL_OVERSCAN_SAFETY
    val horizontalOverscan = size.width * overscanFraction
    val verticalOverscan = size.height * overscanFraction
    val desiredHorizontal = size.width * BACKGROUND_SCROLL_HORIZONTAL_TRAVEL_FACTOR
    val desiredVertical = (size.height * BACKGROUND_SCROLL_VERTICAL_TRAVEL_FACTOR)
        .coerceIn(minimumVertical, maximumVertical)

    return Offset(
        x = min(desiredHorizontal, horizontalOverscan).coerceAtLeast(0f),
        y = min(desiredVertical, verticalOverscan).coerceAtLeast(0f),
    )
}

internal fun calculateContinuousPagerPagePosition(
    currentPage: Int,
    currentPageOffsetFraction: Float,
): Float {
    val safeOffsetFraction = currentPageOffsetFraction.takeIf(Float::isFinite) ?: 0f
    return currentPage.toFloat() + safeOffsetFraction
}

internal fun approachBackgroundScrollOffset(
    current: Offset,
    target: Offset,
    frameSeconds: Float,
): Offset {
    val safeFrameSeconds = frameSeconds.coerceIn(MIN_FRAME_SECONDS, MAX_FRAME_SECONDS)
    val progress = (
        1.0 - exp((-BACKGROUND_SCROLL_RESPONSE_PER_SECOND * safeFrameSeconds).toDouble())
    ).toFloat().coerceIn(0f, 1f)
    return current + (target - current) * progress
}

private fun Offset.coerceToTravel(bounds: Offset): Offset {
    return Offset(
        x = x.coerceIn(-bounds.x, bounds.x),
        y = y.coerceIn(-bounds.y, bounds.y),
    )
}

private fun Offset.isNear(other: Offset, threshold: Float): Boolean {
    return abs(x - other.x) <= threshold && abs(y - other.y) <= threshold
}
