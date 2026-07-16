package me.weishu.kernelsu.ui.component

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import java.util.ArrayDeque
import kotlinx.coroutines.flow.collectLatest
import me.weishu.kernelsu.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

const val GLOBAL_SCROLL_EFFECT_ENABLED_KEY = "global_scroll_effect_enabled"
const val GLOBAL_SCROLL_EFFECT_KEY = "global_scroll_effect"

enum class GlobalScrollEffect(
    val value: String,
    @StringRes val labelRes: Int,
) {
    Trail("trail", R.string.settings_scroll_animation_effect_trail),
    Ripple("ripple", R.string.settings_scroll_animation_effect_ripple),
    Wave("wave", R.string.settings_scroll_animation_effect_wave),
    Burst("burst", R.string.settings_scroll_animation_effect_burst),
    Aurora("aurora", R.string.settings_scroll_animation_effect_aurora);

    companion object {
        val Default = Trail
        const val DEFAULT_VALUE = "trail"

        fun fromValue(value: String?): GlobalScrollEffect {
            return entries.firstOrNull { it.value == value } ?: Default
        }

        fun fromIndex(index: Int): GlobalScrollEffect {
            return entries.getOrElse(index) { Default }
        }

        fun selectedIndex(value: String): Int {
            return entries.indexOf(fromValue(value)).coerceAtLeast(0)
        }
    }
}

@Composable
fun rememberGlobalScrollEffectState(
    enabled: Boolean,
    effectValue: String,
): GlobalScrollEffectState {
    val state = remember { GlobalScrollEffectState() }
    val effect = GlobalScrollEffect.fromValue(effectValue)

    SideEffect {
        state.updateConfig(enabled = enabled, effect = effect)
    }

    LaunchedEffect(state, state.enabled) {
        if (!state.enabled) return@LaunchedEffect

        snapshotFlow(state::hasPulses).collectLatest { hasPulses ->
            if (!hasPulses) return@collectLatest

            while (state.enabled && state.hasPulses()) {
                state.advanceFrame(withFrameMillis { it })
            }
        }
    }

    return state
}

fun Modifier.globalScrollEffectController(
    state: GlobalScrollEffectState,
    pointerFallbackEnabled: Boolean = false,
): Modifier {
    if (!state.enabled) return this

    return this
        .onSizeChanged(state::updateSize)
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                state.beginPointer(down.position)
                var previousPosition = down.position
                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val current = event.changes.firstOrNull { it.pressed }
                    if (current != null) {
                        state.updatePointer(current.position)
                        if (pointerFallbackEnabled) {
                            state.emitFromPointerDrag(current.position - previousPosition)
                        }
                        previousPosition = current.position
                    }
                } while (event.changes.any { it.pressed })
            }
        }
        .nestedScroll(state.nestedScrollConnection)
}

@Composable
fun GlobalScrollEffectOverlay(
    state: GlobalScrollEffectState,
    modifier: Modifier = Modifier,
) {
    if (!state.enabled || !state.hasPulses()) return

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.fillMaxSize()) {
        val frameMillis = state.frameMillis.takeIf { it > 0L } ?: SystemClock.uptimeMillis()
        state.activePulses.forEach { pulse ->
            val rawProgress = (frameMillis - pulse.startedAtMillis).toFloat() / pulse.durationMillis
            if (rawProgress < 0f || rawProgress > 1f) return@forEach

            val progress = rawProgress.coerceIn(0f, 1f)
            drawScrollPulse(
                effect = state.effect,
                pulse = pulse,
                progress = easeOutCubic(progress),
                alpha = pulseAlpha(progress),
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
            )
        }
    }
}

class GlobalScrollEffectState {
    var enabled by mutableStateOf(false)
        private set

    var effect by mutableStateOf(GlobalScrollEffect.Default)
        private set

    var frameMillis by mutableLongStateOf(0L)
        internal set

    private var activePulseCount by mutableIntStateOf(0)
    private var viewportSize = IntSize.Zero
    private var lastPointer: Offset? = null
    private var lastPointerMillis = 0L
    private var pointerGestureStartedMillis = 0L
    private var lastSpawnMillis = 0L
    private var lastNestedScrollMillis = 0L
    private var pendingPointerDelta = Offset.Zero
    private var pendingScrollDelta = Offset.Zero
    private val pulses = ArrayDeque<ScrollPulse>()

    internal val activePulses: Iterable<ScrollPulse>
        get() = pulses

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val delta = if (consumed != Offset.Zero) consumed else available
            if (delta != Offset.Zero) {
                val nowMillis = SystemClock.uptimeMillis()
                lastNestedScrollMillis = nowMillis
                pendingPointerDelta = Offset.Zero
                emitFromScroll(delta, nowMillis)
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val x = consumed.x + available.x
            val y = consumed.y + available.y
            val magnitude = hypot(x.toDouble(), y.toDouble()).toFloat()
            if (magnitude > FLING_THRESHOLD) {
                pendingPointerDelta = Offset.Zero
                pendingScrollDelta = Offset.Zero
                emit(
                    delta = Offset(x.signValue(), y.signValue()) * FLING_DELTA,
                    strength = 1.3f,
                )
            }
            return Velocity.Zero
        }
    }

    fun updateConfig(enabled: Boolean, effect: GlobalScrollEffect) {
        if (this.enabled != enabled) {
            this.enabled = enabled
            if (!enabled) {
                resetInputState()
                clearPulses()
            }
        }
        if (this.effect != effect) {
            this.effect = effect
            resetInputState()
            clearPulses()
        }
    }

    fun updateSize(size: IntSize) {
        if (viewportSize != size) {
            clearPulses()
        }
        viewportSize = size
    }

    fun beginPointer(position: Offset) {
        val nowMillis = SystemClock.uptimeMillis()
        pendingPointerDelta = Offset.Zero
        lastPointer = position
        lastPointerMillis = nowMillis
        pointerGestureStartedMillis = nowMillis
    }

    fun updatePointer(position: Offset) {
        lastPointer = position
        lastPointerMillis = SystemClock.uptimeMillis()
    }

    fun emitFromPointerDrag(delta: Offset) {
        val nowMillis = SystemClock.uptimeMillis()
        if (
            lastNestedScrollMillis > 0L &&
            nowMillis - lastNestedScrollMillis <= NESTED_SCROLL_SUPPRESSION_MILLIS
        ) {
            pendingPointerDelta = Offset.Zero
            return
        }

        pendingPointerDelta += delta
        if (nowMillis - pointerGestureStartedMillis < POINTER_FALLBACK_DELAY_MILLIS) return

        val magnitude = pendingPointerDelta.magnitude()
        if (magnitude < POINTER_DRAG_THRESHOLD || !canSpawn(nowMillis)) return

        emit(
            delta = pendingPointerDelta,
            strength = (magnitude / 54f).coerceIn(0.5f, 1.15f),
            nowMillis = nowMillis,
        )
        pendingPointerDelta = Offset.Zero
    }

    fun hasPulses(): Boolean {
        return activePulseCount > 0
    }

    internal fun advanceFrame(nowMillis: Long) {
        frameMillis = nowMillis
        var changed = false
        while (pulses.isNotEmpty()) {
            val pulse = pulses.first()
            if (nowMillis - pulse.startedAtMillis <= pulse.durationMillis) break
            pulses.removeFirst()
            changed = true
        }
        if (changed) syncPulseCount()
    }

    private fun emitFromScroll(delta: Offset, nowMillis: Long) {
        pendingScrollDelta += delta
        val magnitude = pendingScrollDelta.magnitude()
        if (magnitude < SCROLL_THRESHOLD) return
        if (!canSpawn(nowMillis)) return

        emit(
            delta = pendingScrollDelta,
            strength = (magnitude / 72f).coerceIn(0.55f, 1.2f),
            nowMillis = nowMillis,
        )
        pendingScrollDelta = Offset.Zero
    }

    private fun emit(
        delta: Offset,
        strength: Float,
        nowMillis: Long = SystemClock.uptimeMillis(),
    ) {
        if (!enabled || viewportSize == IntSize.Zero) return

        lastSpawnMillis = nowMillis
        val fallback = Offset(viewportSize.width * 0.5f, viewportSize.height * 0.5f)
        val pointer = lastPointer?.takeIf {
            nowMillis - lastPointerMillis <= POINTER_RECENCY_MILLIS &&
            it.x in 0f..viewportSize.width.toFloat() && it.y in 0f..viewportSize.height.toFloat()
        } ?: fallback
        val direction = delta.normalized()
        val inputDistance = delta.magnitude().coerceAtMost(MAX_ORIGIN_OFFSET_INPUT)
        val center = Offset(
            x = (pointer.x - direction.x * inputDistance * 0.32f)
                .coerceIn(0f, viewportSize.width.toFloat()),
            y = (pointer.y - direction.y * inputDistance * 0.32f)
                .coerceIn(0f, viewportSize.height.toFloat()),
        )

        while (pulses.size >= maxPulsesFor(effect)) {
            pulses.removeFirst()
        }

        pulses.addLast(
            ScrollPulse(
                center = center,
                delta = delta,
                strength = strength.coerceIn(0.45f, 1.55f),
                startedAtMillis = nowMillis,
                durationMillis = durationFor(effect),
                seed = (nowMillis xor (center.x.toLong() shl 12) xor center.y.toLong()).toInt(),
            )
        )
        frameMillis = nowMillis
        syncPulseCount()
    }

    private fun canSpawn(nowMillis: Long): Boolean {
        return nowMillis - lastSpawnMillis >= spawnIntervalFor(effect)
    }

    private fun clearPulses() {
        if (pulses.isEmpty()) return
        pulses.clear()
        syncPulseCount()
    }

    private fun syncPulseCount() {
        activePulseCount = pulses.size
    }

    private fun resetInputState() {
        pendingPointerDelta = Offset.Zero
        pendingScrollDelta = Offset.Zero
        lastPointer = null
        lastPointerMillis = 0L
        pointerGestureStartedMillis = 0L
        lastSpawnMillis = 0L
        lastNestedScrollMillis = 0L
    }
}

internal data class ScrollPulse(
    val center: Offset,
    val delta: Offset,
    val strength: Float,
    val startedAtMillis: Long,
    val durationMillis: Int,
    val seed: Int,
)

private fun DrawScope.drawScrollPulse(
    effect: GlobalScrollEffect,
    pulse: ScrollPulse,
    progress: Float,
    alpha: Float,
    primary: Color,
    secondary: Color,
    tertiary: Color,
) {
    when (effect) {
        GlobalScrollEffect.Trail -> drawTrailPulse(pulse, progress, alpha, primary, secondary)
        GlobalScrollEffect.Ripple -> drawRipplePulse(pulse, progress, alpha, primary, secondary)
        GlobalScrollEffect.Wave -> drawWavePulse(pulse, progress, alpha, primary, tertiary)
        GlobalScrollEffect.Burst -> drawBurstPulse(pulse, progress, alpha, primary, secondary)
        GlobalScrollEffect.Aurora -> drawAuroraPulse(pulse, progress, alpha, primary, secondary, tertiary)
    }
}

private fun DrawScope.drawTrailPulse(
    pulse: ScrollPulse,
    progress: Float,
    alpha: Float,
    primary: Color,
    secondary: Color,
) {
    val direction = pulse.delta.normalized()
    val normal = Offset(-direction.y, direction.x)
    val baseLength = (42.dp.toPx() + 40.dp.toPx() * pulse.strength) * (1f + progress * 0.14f)
    val baseWidth = 1.4.dp.toPx() + 1.2.dp.toPx() * pulse.strength
    val laneGap = 8.dp.toPx()
    val minWidth = 0.35.dp.toPx()

    repeat(3) { index ->
        val lane = index - 1
        val laneWeight = if (lane == 0) 1f else 0.64f
        val length = baseLength * (1f - abs(lane) * 0.12f)
        val head = pulse.center + normal * (lane * laneGap) - direction * (baseLength * progress)
        drawLine(
            color = if (lane == 0) {
                primary.copy(alpha = 0.4f * alpha)
            } else {
                secondary.copy(alpha = 0.24f * alpha)
            },
            start = head,
            end = head - direction * length,
            strokeWidth = (baseWidth * alpha * laneWeight).coerceAtLeast(minWidth),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawRipplePulse(
    pulse: ScrollPulse,
    progress: Float,
    alpha: Float,
    primary: Color,
    secondary: Color,
) {
    val radius = (20.dp.toPx() + 88.dp.toPx() * progress) * pulse.strength
    val strokeWidth = (1.8.dp.toPx() * (0.7f + pulse.strength * 0.3f))
        .coerceAtLeast(0.5.dp.toPx())
    drawCircle(
        color = primary.copy(alpha = 0.3f * alpha),
        radius = radius,
        center = pulse.center,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    val innerProgress = ((progress - 0.14f) / 0.86f).coerceIn(0f, 1f)
    if (innerProgress > 0f) {
        drawCircle(
            color = secondary.copy(alpha = 0.2f * alpha),
            radius = (14.dp.toPx() + 54.dp.toPx() * innerProgress) * pulse.strength,
            center = pulse.center,
            style = Stroke(width = strokeWidth * 0.72f, cap = StrokeCap.Round),
        )
    }

    drawCircle(
        color = primary.copy(alpha = 0.12f * alpha * (1f - progress)),
        radius = (3.dp.toPx() + 4.dp.toPx() * pulse.strength) * (1f - progress * 0.4f),
        center = pulse.center,
    )
}

private fun DrawScope.drawWavePulse(
    pulse: ScrollPulse,
    progress: Float,
    alpha: Float,
    primary: Color,
    tertiary: Color,
) {
    val direction = pulse.delta.normalized()
    val normal = Offset(-direction.y, direction.x)
    val amplitude = (6.dp.toPx() + 9.dp.toPx() * pulse.strength) * alpha
    val length = 136.dp.toPx() * pulse.strength
    val center = pulse.center - direction * (progress * 36.dp.toPx())
    val path = Path()

    repeat(WAVE_POINT_COUNT) { index ->
        val ratio = index / (WAVE_POINT_COUNT - 1f)
        val envelope = sin(PI.toFloat() * ratio)
        val phase = ratio * TWO_PI * 1.2f + progress * TWO_PI
        val point = center +
            direction * ((ratio - 0.5f) * length) +
            normal * (sin(phase) * amplitude * envelope)
        if (index == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }

    drawPath(
        path = path,
        color = tertiary.copy(alpha = 0.1f * alpha),
        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
        path = path,
        color = primary.copy(alpha = 0.38f * alpha),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawBurstPulse(
    pulse: ScrollPulse,
    progress: Float,
    alpha: Float,
    primary: Color,
    secondary: Color,
) {
    val travelDirection = pulse.delta.normalized() * -1f
    repeat(BURST_PARTICLE_COUNT) { index ->
        val spread = (seededUnit(pulse.seed, index * 3) - 0.5f) * 1.45f
        val speed = 0.56f + seededUnit(pulse.seed, index * 3 + 1) * 0.72f
        val width = 0.8.dp.toPx() + seededUnit(pulse.seed, index * 3 + 2) * 1.2.dp.toPx()
        val particleDirection = travelDirection.rotated(spread)
        val distance = (10.dp.toPx() + 52.dp.toPx() * speed) * progress * pulse.strength
        val head = pulse.center + particleDirection * distance
        val tailLength = (4.dp.toPx() + 8.dp.toPx() * speed) * (1f - progress)
        drawLine(
            color = if (index % 2 == 0) {
                primary.copy(alpha = 0.36f * alpha)
            } else {
                secondary.copy(alpha = 0.26f * alpha)
            },
            start = head,
            end = head - particleDirection * tailLength,
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawAuroraPulse(
    pulse: ScrollPulse,
    progress: Float,
    alpha: Float,
    primary: Color,
    secondary: Color,
    tertiary: Color,
) {
    val direction = pulse.delta.normalized()
    val angle = atan2(direction.y, direction.x)
    val glowSize = Size(
        width = (104.dp.toPx() + 76.dp.toPx() * progress) * pulse.strength,
        height = (24.dp.toPx() + 24.dp.toPx() * progress) * pulse.strength,
    )
    val center = pulse.center - direction * (progress * 38.dp.toPx())
    rotate(degrees = angle * RAD_TO_DEG, pivot = center) {
        drawOval(
            color = tertiary.copy(alpha = 0.055f * alpha),
            topLeft = center - Offset(glowSize.width * 0.56f, glowSize.height * 0.9f),
            size = Size(glowSize.width * 1.12f, glowSize.height * 1.8f),
        )
        drawOval(
            color = primary.copy(alpha = 0.13f * alpha),
            topLeft = center - Offset(glowSize.width / 2f, glowSize.height / 2f),
            size = glowSize,
        )
        drawOval(
            color = secondary.copy(alpha = 0.1f * alpha),
            topLeft = center - Offset(glowSize.width * 0.28f, glowSize.height * 0.7f),
            size = Size(glowSize.width * 0.68f, glowSize.height * 1.24f),
        )
    }
}

private fun durationFor(effect: GlobalScrollEffect): Int {
    return when (effect) {
        GlobalScrollEffect.Trail -> 360
        GlobalScrollEffect.Ripple -> 460
        GlobalScrollEffect.Wave -> 420
        GlobalScrollEffect.Burst -> 380
        GlobalScrollEffect.Aurora -> 500
    }
}

private fun spawnIntervalFor(effect: GlobalScrollEffect): Long {
    return when (effect) {
        GlobalScrollEffect.Trail -> 48L
        GlobalScrollEffect.Ripple -> 72L
        GlobalScrollEffect.Wave -> 64L
        GlobalScrollEffect.Burst -> 72L
        GlobalScrollEffect.Aurora -> 84L
    }
}

private fun maxPulsesFor(effect: GlobalScrollEffect): Int {
    return when (effect) {
        GlobalScrollEffect.Trail -> 10
        GlobalScrollEffect.Ripple -> 8
        GlobalScrollEffect.Wave -> 8
        GlobalScrollEffect.Burst -> 7
        GlobalScrollEffect.Aurora -> 6
    }
}

private fun Offset.normalized(): Offset {
    val distance = magnitude()
    if (distance <= 0.01f) return Offset(0f, 1f)
    return Offset(x / distance, y / distance)
}

private fun Offset.magnitude(): Float {
    return hypot(x.toDouble(), y.toDouble()).toFloat()
}

private fun Offset.rotated(radians: Float): Offset {
    val cosine = cos(radians)
    val sine = sin(radians)
    return Offset(
        x = x * cosine - y * sine,
        y = x * sine + y * cosine,
    )
}

private fun Float.signValue(): Float {
    return when {
        this > 0f -> 1f
        this < 0f -> -1f
        else -> 0f
    }
}

private fun easeOutCubic(value: Float): Float {
    val inverse = 1f - value
    return 1f - inverse * inverse * inverse
}

private fun pulseAlpha(progress: Float): Float {
    val fadeIn = smoothStep((progress / 0.1f).coerceIn(0f, 1f))
    val fadeOut = 1f - smoothStep(((progress - 0.18f) / 0.82f).coerceIn(0f, 1f))
    return fadeIn * fadeOut
}

private fun smoothStep(value: Float): Float {
    return value * value * (3f - 2f * value)
}

private fun seededUnit(seed: Int, index: Int): Float {
    var value = seed + index * -1640531527
    value = (value xor (value ushr 16)) * -2048144789
    value = (value xor (value ushr 13)) * -1028477387
    value = value xor (value ushr 16)
    return (value ushr 8) / 16777215f
}

private const val SCROLL_THRESHOLD = 8f
private const val POINTER_DRAG_THRESHOLD = 9f
private const val FLING_THRESHOLD = 900f
private const val FLING_DELTA = 44f
private const val MAX_ORIGIN_OFFSET_INPUT = 56f
private const val POINTER_RECENCY_MILLIS = 1_200L
private const val POINTER_FALLBACK_DELAY_MILLIS = 32L
private const val NESTED_SCROLL_SUPPRESSION_MILLIS = 90L
private const val WAVE_POINT_COUNT = 15
private const val BURST_PARTICLE_COUNT = 9
private const val TWO_PI = (PI * 2.0).toFloat()
private const val RAD_TO_DEG = (180.0 / PI).toFloat()
