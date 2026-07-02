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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import me.weishu.kernelsu.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

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

    LaunchedEffect(state.enabled, state.effect, state.spawnVersion) {
        while (state.enabled && state.hasPulses()) {
            val frame = withFrameMillis { it }
            state.frameMillis = frame
            state.pruneExpired(frame)
        }
    }

    return state
}

fun Modifier.globalScrollEffectController(state: GlobalScrollEffectState): Modifier {
    if (!state.enabled) return this

    return this
        .onSizeChanged(state::updateSize)
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                state.updatePointer(down.position)
                var previousPosition = down.position
                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val current = event.changes.firstOrNull { it.pressed }
                    if (current != null) {
                        state.updatePointer(current.position)
                        state.emitFromPointerDrag(current.position - previousPosition)
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
        state.pulsesSnapshot().forEach { pulse ->
            val rawProgress = (frameMillis - pulse.startedAtMillis).toFloat() / pulse.durationMillis
            val progress = rawProgress.coerceIn(0f, 1f)
            val eased = easeOutQuart(progress)
            val alpha = (1f - progress).coerceIn(0f, 1f)
            drawScrollPulse(
                effect = state.effect,
                pulse = pulse,
                progress = eased,
                alpha = alpha,
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

    var spawnVersion by mutableIntStateOf(0)
        private set

    private var viewportSize by mutableStateOf(IntSize.Zero)
    private var lastPointer by mutableStateOf<Offset?>(null)
    private var lastSpawnMillis by mutableLongStateOf(0L)
    private val pulses = mutableStateListOf<ScrollPulse>()

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            emitFromScroll(consumed)
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val x = consumed.x + available.x
            val y = consumed.y + available.y
            val magnitude = hypot(x.toDouble(), y.toDouble()).toFloat()
            if (magnitude > FLING_THRESHOLD) {
                emit(delta = Offset(x.signValue(), y.signValue()) * FLING_DELTA, strength = 1.35f)
            }
            return Velocity.Zero
        }
    }

    fun updateConfig(enabled: Boolean, effect: GlobalScrollEffect) {
        if (this.enabled != enabled) {
            this.enabled = enabled
            if (!enabled) {
                pulses.clear()
            }
        }
        if (this.effect != effect) {
            this.effect = effect
            pulses.clear()
            spawnVersion++
        }
    }

    fun updateSize(size: IntSize) {
        viewportSize = size
    }

    fun updatePointer(position: Offset) {
        lastPointer = position
    }

    fun emitFromPointerDrag(delta: Offset) {
        val magnitude = hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
        if (magnitude < POINTER_DRAG_THRESHOLD) return

        emit(
            delta = delta,
            strength = (magnitude / 42f).coerceIn(0.5f, 1.15f),
        )
    }

    fun hasPulses(): Boolean {
        return pulses.isNotEmpty()
    }

    internal fun pulsesSnapshot(): List<ScrollPulse> {
        return pulses.toList()
    }

    internal fun pruneExpired(nowMillis: Long) {
        pulses.removeAll { nowMillis - it.startedAtMillis > it.durationMillis }
    }

    private fun emitFromScroll(delta: Offset) {
        val magnitude = hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
        if (magnitude < SCROLL_THRESHOLD) return

        val now = SystemClock.uptimeMillis()
        if (now - lastSpawnMillis < MIN_SPAWN_INTERVAL_MILLIS) return

        emit(delta = delta, strength = (magnitude / 64f).coerceIn(0.55f, 1.2f), nowMillis = now)
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
            it.x in 0f..viewportSize.width.toFloat() && it.y in 0f..viewportSize.height.toFloat()
        } ?: fallback
        val center = Offset(
            x = (pointer.x + -delta.x * 0.45f).coerceIn(0f, viewportSize.width.toFloat()),
            y = (pointer.y + -delta.y * 0.45f).coerceIn(0f, viewportSize.height.toFloat()),
        )

        while (pulses.size >= MAX_PULSES) {
            pulses.removeAt(0)
        }

        pulses += ScrollPulse(
            center = center,
            delta = delta,
            strength = strength.coerceIn(0.45f, 1.55f),
            startedAtMillis = nowMillis,
            durationMillis = durationFor(effect),
            seed = (nowMillis xor (center.x.toLong() shl 12) xor center.y.toLong()).toInt(),
        )
        frameMillis = nowMillis
        spawnVersion++
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
    val length = (44.dp.toPx() + 46.dp.toPx() * pulse.strength) * (1f + progress * 0.2f)
    val width = (2.2.dp.toPx() + 1.5.dp.toPx() * pulse.strength) * alpha
    val color = primary.copy(alpha = 0.42f * alpha)
    val accent = secondary.copy(alpha = 0.28f * alpha)

    repeat(5) { index ->
        val lane = index - 2
        val start = pulse.center + normal * (lane * 10.dp.toPx()) - direction * (length * progress)
        val end = start - direction * length
        drawLine(
            color = if (index % 2 == 0) color else accent,
            start = start,
            end = end,
            strokeWidth = width.coerceAtLeast(0.4.dp.toPx()),
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
    val radius = (24.dp.toPx() + 96.dp.toPx() * progress) * pulse.strength
    drawCircle(
        color = primary.copy(alpha = 0.24f * alpha),
        radius = radius,
        center = pulse.center,
        style = Stroke(width = (2.2.dp.toPx() * alpha).coerceAtLeast(0.5.dp.toPx())),
    )
    drawCircle(
        color = secondary.copy(alpha = 0.12f * alpha),
        radius = radius * 0.62f,
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
    val amplitude = (8.dp.toPx() + 12.dp.toPx() * pulse.strength) * alpha
    val length = 152.dp.toPx() * pulse.strength
    val center = pulse.center - direction * (progress * 44.dp.toPx())
    val path = Path()

    repeat(18) { index ->
        val ratio = index / 17f
        val phase = ratio * TWO_PI * 1.35f + progress * TWO_PI
        val point = center +
            direction * ((ratio - 0.5f) * length) +
            normal * (sin(phase) * amplitude)
        if (index == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }

    drawPath(
        path = path,
        color = primary.copy(alpha = 0.38f * alpha),
        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round),
    )
    drawCircle(
        color = tertiary.copy(alpha = 0.13f * alpha),
        radius = (30.dp.toPx() + 30.dp.toPx() * progress) * pulse.strength,
        center = pulse.center,
    )
}

private fun DrawScope.drawBurstPulse(
    pulse: ScrollPulse,
    progress: Float,
    alpha: Float,
    primary: Color,
    secondary: Color,
) {
    val random = Random(pulse.seed)
    repeat(12) { index ->
        val angle = random.nextFloat() * TWO_PI
        val distance = (12.dp.toPx() + random.nextFloat() * 56.dp.toPx()) * progress * pulse.strength
        val center = pulse.center + Offset(cos(angle), sin(angle)) * distance
        val radius = (2.dp.toPx() + random.nextFloat() * 3.4.dp.toPx()) * (1f - progress * 0.35f)
        drawCircle(
            color = if (index % 2 == 0) primary.copy(alpha = 0.36f * alpha) else secondary.copy(alpha = 0.28f * alpha),
            radius = radius,
            center = center,
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
        width = (118.dp.toPx() + 90.dp.toPx() * progress) * pulse.strength,
        height = (34.dp.toPx() + 34.dp.toPx() * progress) * pulse.strength,
    )
    val center = pulse.center - direction * (progress * 42.dp.toPx())
    rotate(degrees = angle * RAD_TO_DEG, pivot = center) {
        drawOval(
            color = primary.copy(alpha = 0.14f * alpha),
            topLeft = center - Offset(glowSize.width / 2f, glowSize.height / 2f),
            size = glowSize,
        )
        drawOval(
            color = secondary.copy(alpha = 0.11f * alpha),
            topLeft = center - Offset(glowSize.width * 0.3f, glowSize.height * 0.66f),
            size = Size(glowSize.width * 0.72f, glowSize.height * 1.16f),
        )
        drawCircle(
            color = tertiary.copy(alpha = 0.08f * alpha),
            radius = glowSize.height * 0.72f,
            center = center,
        )
    }
}

private fun durationFor(effect: GlobalScrollEffect): Int {
    return when (effect) {
        GlobalScrollEffect.Trail -> 460
        GlobalScrollEffect.Ripple -> 620
        GlobalScrollEffect.Wave -> 560
        GlobalScrollEffect.Burst -> 520
        GlobalScrollEffect.Aurora -> 680
    }
}

private fun Offset.normalized(): Offset {
    val distance = hypot(x.toDouble(), y.toDouble()).toFloat()
    if (distance <= 0.01f) return Offset(0f, 1f)
    return Offset(x / distance, y / distance)
}

private fun Float.signValue(): Float {
    return when {
        this > 0f -> 1f
        this < 0f -> -1f
        else -> 0f
    }
}

private fun easeOutQuart(value: Float): Float {
    val inverse = 1f - value
    return 1f - inverse * inverse * inverse * inverse
}

private const val MAX_PULSES = 32
private const val SCROLL_THRESHOLD = 4f
private const val POINTER_DRAG_THRESHOLD = 7f
private const val FLING_THRESHOLD = 900f
private const val FLING_DELTA = 42f
private const val MIN_SPAWN_INTERVAL_MILLIS = 34L
private const val TWO_PI = (PI * 2.0).toFloat()
private const val RAD_TO_DEG = (180.0 / PI).toFloat()
