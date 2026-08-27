package me.weishu.kernelsu.ui.component.pixel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelPetAnimationPolicyTest {
    @Test
    fun deviceTierChangesSamplingRateWithoutChangingTimelineDuration() {
        val standard = PixelPetAnimationPolicy.forDevice(lowRam = false, powerSave = false)
        val reduced = PixelPetAnimationPolicy.forDevice(lowRam = true, powerSave = false)
        val powerSave = PixelPetAnimationPolicy.forDevice(lowRam = false, powerSave = true)

        assertEquals(PixelPetPerformanceTier.Standard, standard.tier)
        assertEquals(PixelPetPerformanceTier.Reduced, reduced.tier)
        assertEquals(PixelPetPerformanceTier.Reduced, powerSave.tier)
        assertEquals(standard.timelineMillis, reduced.timelineMillis)
        assertEquals(standard.timelineMillis, powerSave.timelineMillis)
        assertTrue(standard.targetFps > reduced.targetFps)
        assertTrue(reduced.secondaryEffectStride > standard.secondaryEffectStride)
    }

    @Test
    fun reducedSamplingKeepsActionFrameTimingAligned() {
        val standard = PixelPetAnimationPolicy(PixelPetPerformanceTier.Standard)
        val reduced = PixelPetAnimationPolicy(PixelPetPerformanceTier.Reduced)
        PixelPetAction.entries.forEach { action ->
            listOf(0f, 0.16f, 0.33f, 0.5f, 0.82f, 1f).forEach { phase ->
                val standardFrame = pixelPetFrameIndex(quantizePixelPetPhase(phase, standard), action)
                val reducedFrame = pixelPetFrameIndex(quantizePixelPetPhase(phase, reduced), action)
                val count = PixelPetReferenceSprites.frameCount(action)
                val distance = kotlin.math.abs(standardFrame - reducedFrame)
                assertTrue("$action phase $phase drifted too far", distance <= 1 || distance == count - 1)
            }
        }
    }

    @Test
    fun allActionFrameIndexesStayInsideDeclaredCycles() {
        PixelPetAction.entries.forEach { action ->
            repeat(101) { sample ->
                val frame = pixelPetFrameIndex(sample / 100f, action)
                assertTrue(frame in 0 until PixelPetReferenceSprites.frameCount(action))
            }
        }
    }

    @Test
    fun authoredTransitionsUseDiscreteFramesBeforeTheTargetLoop() {
        val transitions = listOf(
            PixelPetAction.Idle to PixelPetAction.Walking,
            PixelPetAction.Walking to PixelPetAction.Idle,
            PixelPetAction.Idle to PixelPetAction.Eating,
            PixelPetAction.Eating to PixelPetAction.Idle,
            PixelPetAction.Idle to PixelPetAction.Sleeping,
            PixelPetAction.Sleeping to PixelPetAction.Idle,
        )
        transitions.forEach { (from, to) ->
            val early = resolvePixelPetRenderMotion(to, from, transitionStartedAt = 1_000L, now = 1_000L, phase = 0.4f)
            val late = resolvePixelPetRenderMotion(to, from, transitionStartedAt = 1_000L, now = 2_000L, phase = 0.4f)
            assertTrue(early.frame in 0 until PixelPetSpriteAtlas.frameCount(early.action))
            assertTrue(early.inTransition)
            assertEquals(to, late.action)
            assertTrue(late.frame in 0 until PixelPetSpriteAtlas.frameCount(to))
        }
    }
}
