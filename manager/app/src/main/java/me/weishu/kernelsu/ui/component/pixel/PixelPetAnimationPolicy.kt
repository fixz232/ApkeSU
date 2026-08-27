package me.weishu.kernelsu.ui.component.pixel

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.floor

internal enum class PixelPetPerformanceTier(
    val targetFps: Int,
    val secondaryEffectStride: Int,
) {
    Reduced(targetFps = 18, secondaryEffectStride = 2),
    Standard(targetFps = 30, secondaryEffectStride = 1),
}

internal data class PixelPetAnimationPolicy(
    val tier: PixelPetPerformanceTier,
    val timelineMillis: Int = TIMELINE_MILLIS,
) {
    val targetFps: Int get() = tier.targetFps
    val secondaryEffectStride: Int get() = tier.secondaryEffectStride

    companion object {
        const val TIMELINE_MILLIS = 3_200

        fun forDevice(lowRam: Boolean, powerSave: Boolean): PixelPetAnimationPolicy =
            PixelPetAnimationPolicy(
                tier = if (lowRam || powerSave) PixelPetPerformanceTier.Reduced else PixelPetPerformanceTier.Standard,
            )
    }
}

internal fun quantizePixelPetPhase(
    phase: Float,
    policy: PixelPetAnimationPolicy,
): Float {
    val safePhase = phase.coerceIn(0f, 1f)
    val samples = (policy.timelineMillis * policy.targetFps / 1_000).coerceAtLeast(1)
    return floor(safePhase * samples) / samples
}

internal fun pixelPetActionLoopMillis(action: PixelPetAction): Int = when (action) {
    PixelPetAction.Idle -> 3_200
    PixelPetAction.Sleeping -> 4_800
    PixelPetAction.Walking -> 900
    PixelPetAction.Exploring -> 1_150
    PixelPetAction.Eating -> 1_450
    PixelPetAction.Cleaning,
    PixelPetAction.Watching,
    -> 1_700
    PixelPetAction.Happy,
    PixelPetAction.Petted,
    PixelPetAction.Playing,
    PixelPetAction.Frightened,
    PixelPetAction.Calling,
    PixelPetAction.Hatching,
    -> 1_050
}

internal fun pixelPetFrameIndex(phase: Float, action: PixelPetAction): Int {
    if (phase <= 0f) return 0
    val elapsedMillis = phase.coerceIn(0f, 1f) * PixelPetAnimationPolicy.TIMELINE_MILLIS
    // Reference art is the primary renderer and has a complete 8-10 frame
    // timing cycle even where the legacy fallback atlas only owns six cells.
    val count = PixelPetReferenceSprites.frameCount(action)
    val frameMillis = pixelPetActionLoopMillis(action).toFloat() / count
    return floor(elapsedMillis / frameMillis).toInt().mod(count)
}

internal data class PixelPetRenderMotion(
    val action: PixelPetAction,
    val frame: Int,
    val inTransition: Boolean = false,
)

private data class PixelPetTransitionFrame(
    val action: PixelPetAction,
    val frame: Int,
)

private const val PIXEL_PET_TRANSITION_FRAME_MILLIS = 90L

/**
 * Transitions reuse explicitly authored anticipation and settle frames. This
 * preserves hard pixel edges instead of cross-fading two Sprite bitmaps.
 */
private fun pixelPetTransitionFrames(
    from: PixelPetAction,
    to: PixelPetAction,
): List<PixelPetTransitionFrame> = when {
    from == to -> emptyList()
    to == PixelPetAction.Walking -> listOf(
        PixelPetTransitionFrame(from, pixelPetFrameIndex(0.92f, from)),
        PixelPetTransitionFrame(PixelPetAction.Walking, 0),
        PixelPetTransitionFrame(PixelPetAction.Walking, 1),
        PixelPetTransitionFrame(PixelPetAction.Walking, 2),
    )
    from == PixelPetAction.Walking -> listOf(
        PixelPetTransitionFrame(PixelPetAction.Walking, 7),
        PixelPetTransitionFrame(PixelPetAction.Walking, 8),
        PixelPetTransitionFrame(PixelPetAction.Walking, 9),
        PixelPetTransitionFrame(to, 0),
    )
    to == PixelPetAction.Sleeping -> listOf(
        PixelPetTransitionFrame(from, pixelPetFrameIndex(0.90f, from)),
        PixelPetTransitionFrame(PixelPetAction.Sleeping, 0),
        PixelPetTransitionFrame(PixelPetAction.Sleeping, 1),
        PixelPetTransitionFrame(PixelPetAction.Sleeping, 2),
    )
    from == PixelPetAction.Sleeping -> listOf(
        PixelPetTransitionFrame(PixelPetAction.Sleeping, 7),
        PixelPetTransitionFrame(PixelPetAction.Sleeping, 8),
        PixelPetTransitionFrame(PixelPetAction.Sleeping, 9),
        PixelPetTransitionFrame(to, 0),
    )
    to == PixelPetAction.Eating -> listOf(
        PixelPetTransitionFrame(from, pixelPetFrameIndex(0.88f, from)),
        PixelPetTransitionFrame(PixelPetAction.Eating, 0),
        PixelPetTransitionFrame(PixelPetAction.Eating, 1),
        PixelPetTransitionFrame(PixelPetAction.Eating, 2),
    )
    from == PixelPetAction.Eating -> listOf(
        PixelPetTransitionFrame(PixelPetAction.Eating, 7),
        PixelPetTransitionFrame(PixelPetAction.Eating, 8),
        PixelPetTransitionFrame(PixelPetAction.Eating, 9),
        PixelPetTransitionFrame(to, 0),
    )
    else -> listOf(
        PixelPetTransitionFrame(from, pixelPetFrameIndex(0.90f, from)),
        PixelPetTransitionFrame(to, 0),
        PixelPetTransitionFrame(to, 1.coerceAtMost(PixelPetSpriteAtlas.frameCount(to) - 1)),
    )
}

internal fun resolvePixelPetRenderMotion(
    targetAction: PixelPetAction,
    previousAction: PixelPetAction,
    transitionStartedAt: Long,
    now: Long,
    phase: Float,
): PixelPetRenderMotion {
    val sequence = pixelPetTransitionFrames(previousAction, targetAction)
    if (transitionStartedAt > 0L && sequence.isNotEmpty()) {
        val index = ((now - transitionStartedAt) / PIXEL_PET_TRANSITION_FRAME_MILLIS).toInt()
        sequence.getOrNull(index)?.let { return PixelPetRenderMotion(it.action, it.frame, inTransition = true) }
    }
    return PixelPetRenderMotion(targetAction, pixelPetFrameIndex(phase, targetAction))
}

@Composable
internal fun rememberPixelPetRenderMotion(
    targetAction: PixelPetAction,
    phase: Float,
): PixelPetRenderMotion {
    var observedAction by remember { mutableStateOf(targetAction) }
    var transitionFromAction by remember { mutableStateOf(targetAction) }
    var transitionStartedAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(targetAction) {
        if (targetAction != observedAction) {
            transitionFromAction = observedAction
            transitionStartedAt = SystemClock.elapsedRealtime()
            observedAction = targetAction
        }
    }
    return resolvePixelPetRenderMotion(
        targetAction = targetAction,
        previousAction = transitionFromAction,
        transitionStartedAt = transitionStartedAt,
        now = SystemClock.elapsedRealtime(),
        phase = phase,
    )
}
