package me.weishu.kernelsu.ui.component.pixel

import android.animation.ValueAnimator
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LocalPixelCardMotionEnabled = staticCompositionLocalOf {
    DEFAULT_PIXEL_CARD_MOTION_ENABLED
}

val LocalPixelCardMotionProgress = staticCompositionLocalOf<State<Float>> {
    mutableFloatStateOf(STATIC_PIXEL_CARD_MOTION_PROGRESS)
}

@Composable
fun rememberPixelCardMotionProgress(enabled: Boolean): State<Float> {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    if (!enabled || !animationsEnabled) {
        return remember { mutableFloatStateOf(STATIC_PIXEL_CARD_MOTION_PROGRESS) }
    }
    val transition = rememberInfiniteTransition(label = "pixelCardMotion")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = PIXEL_CARD_MOTION_CYCLE_MILLIS,
                easing = PixelStepEasing,
            ),
        ),
        label = "pixelCardMotionProgress",
    )
}

internal enum class PixelCardMotionScene {
    Handheld,
    Arcade,
    Pastoral,
    StarVoyage,
    InkJade,
    Wasteland,
    Ocean,
    Cyber,
    ThreeKingdoms,
    Bianliang,
    FishingHarbor,
    TribalJungle,
    LavaValley,
    DunhuangDesert,
    VikingSnowfield,
    JiangnanWatertown,
    CloudTown,
    PetCompanion,
}

internal fun PixelStyle.cardMotionScene(): PixelCardMotionScene = when (this) {
    PixelStyle.ClassicHandheld -> PixelCardMotionScene.Handheld
    PixelStyle.NeonArcade -> PixelCardMotionScene.Arcade
    PixelStyle.PastoralFields -> PixelCardMotionScene.Pastoral
    PixelStyle.StarVoyage -> PixelCardMotionScene.StarVoyage
    PixelStyle.InkJade -> PixelCardMotionScene.InkJade
    PixelStyle.RustWasteland -> PixelCardMotionScene.Wasteland
    PixelStyle.OceanDepths -> PixelCardMotionScene.Ocean
    PixelStyle.CyberHacker -> PixelCardMotionScene.Cyber
    PixelStyle.ThreeKingdoms -> PixelCardMotionScene.ThreeKingdoms
    PixelStyle.BianliangMarket -> PixelCardMotionScene.Bianliang
    PixelStyle.FishingHarbor -> PixelCardMotionScene.FishingHarbor
    PixelStyle.TribalJungle -> PixelCardMotionScene.TribalJungle
    PixelStyle.LavaValley -> PixelCardMotionScene.LavaValley
    PixelStyle.DunhuangDesert -> PixelCardMotionScene.DunhuangDesert
    PixelStyle.VikingSnowfield -> PixelCardMotionScene.VikingSnowfield
    PixelStyle.JiangnanWatertown -> PixelCardMotionScene.JiangnanWatertown
    PixelStyle.CloudTown -> PixelCardMotionScene.CloudTown
    PixelStyle.PetCompanion -> PixelCardMotionScene.PetCompanion
}

internal fun DrawScope.drawPixelCardMotionUnderlay(
    style: PixelStyle,
    palette: PixelPalette,
    progress: Float,
    petHabitat: PixelPetHabitat = PixelPetHabitat.Garden,
) {
    if (!hasPixelCardMotionBounds()) return
    val unit = minOf(2.dp.toPx(), size.minDimension / 22f)
    val phase = progress.normalizedPixelProgress()
    when (style.cardMotionScene()) {
        PixelCardMotionScene.Handheld -> drawHandheldCardMotion(unit, phase, palette)
        PixelCardMotionScene.Arcade -> drawArcadeCardMotion(unit, phase, palette)
        PixelCardMotionScene.Pastoral -> drawPastoralCardMotion(unit, phase, palette)
        PixelCardMotionScene.StarVoyage -> drawStarVoyageCardMotion(unit, phase, palette)
        PixelCardMotionScene.InkJade -> drawInkJadeCardMotion(unit, phase, palette)
        PixelCardMotionScene.Wasteland -> drawWastelandCardMotion(unit, phase, palette)
        PixelCardMotionScene.Ocean -> drawOceanCardMotion(unit, phase, palette)
        PixelCardMotionScene.Cyber -> drawCyberCardMotion(unit, phase, palette)
        PixelCardMotionScene.ThreeKingdoms -> drawThreeKingdomsCardMotion(unit, phase, palette)
        PixelCardMotionScene.Bianliang -> drawBianliangCardMotion(unit, phase, palette)
        PixelCardMotionScene.FishingHarbor -> drawFishingHarborCardMotion(unit, phase, palette)
        PixelCardMotionScene.TribalJungle -> drawTribalJungleCardMotion(unit, phase, palette)
        PixelCardMotionScene.LavaValley -> drawLavaValleyCardMotion(unit, phase, palette)
        PixelCardMotionScene.DunhuangDesert -> drawDunhuangCardMotion(unit, phase, palette)
        PixelCardMotionScene.VikingSnowfield -> drawVikingCardMotion(unit, phase, palette)
        PixelCardMotionScene.JiangnanWatertown -> drawJiangnanCardMotion(unit, phase, palette)
        PixelCardMotionScene.CloudTown -> drawCloudTownCardMotion(unit, phase, palette)
        PixelCardMotionScene.PetCompanion -> drawPetCompanionCardMotion(unit, phase, palette, petHabitat)
    }
}

internal fun DrawScope.drawPixelCardMotionOverlay(
    style: PixelStyle,
    palette: PixelPalette,
    progress: Float,
    petHabitat: PixelPetHabitat = PixelPetHabitat.Garden,
) {
    if (!hasPixelCardMotionBounds()) return
    val unit = minOf(2.dp.toPx(), size.minDimension / 22f)
    val phase = progress.normalizedPixelProgress()
    val pulse = pixelPulse(phase)
    drawPixelCardMotionTracer(unit, phase, palette)
    when (style.cardMotionScene()) {
        PixelCardMotionScene.Handheld -> {
            drawRect(
                palette.secondary.copy(alpha = 0.42f + pulse * 0.38f),
                Offset(size.width - unit * 4f, unit * 2f),
                Size(unit, unit),
            )
            drawRect(
                palette.primary.copy(alpha = 0.28f),
                Offset(size.width * (0.12f + phase * 0.56f), unit),
                Size(unit * 3f, unit * 0.45f),
            )
        }

        PixelCardMotionScene.Arcade -> drawArcadeChaseLights(unit, phase, palette)
        PixelCardMotionScene.Pastoral -> {
            repeat(4) { index ->
                val local = (phase * 0.34f + index * 0.24f).normalizedPixelProgress()
                drawRect(
                    palette.highlight.copy(alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.34f),
                    Offset(size.width * (0.12f + index * 0.24f), unit + local * unit * 4f),
                    Size(unit * 0.55f, unit * 0.55f),
                )
            }
        }

        PixelCardMotionScene.StarVoyage -> {
            val scanX = unit * 3f + phase * (size.width - unit * 6f)
            drawRect(
                palette.highlight.copy(alpha = 0.42f),
                Offset(scanX, unit),
                Size(unit, unit),
            )
            drawRect(
                palette.secondary.copy(alpha = 0.30f + pulse * 0.32f),
                Offset(size.width - unit * 5f, size.height - unit * 2f),
                Size(unit * 2f, unit * 0.55f),
            )
        }

        PixelCardMotionScene.InkJade -> {
            val glintX = size.width * (0.58f + phase * 0.25f)
            drawRect(palette.secondary.copy(alpha = 0.34f + pulse * 0.28f), Offset(glintX, unit), Size(unit * 2f, unit * 0.55f))
            drawRect(palette.primary.copy(alpha = 0.28f), Offset(unit, size.height * (0.18f + phase * 0.42f)), Size(unit * 0.55f, unit * 2f))
        }

        PixelCardMotionScene.Wasteland -> {
            val warningOn = ((phase * 16f).toInt() % 4) < 2
            drawRect(
                palette.secondary.copy(alpha = if (warningOn) 0.62f else 0.18f),
                Offset(size.width - unit * 4f, unit),
                Size(unit * 2f, unit),
            )
            drawRect(palette.primary.copy(alpha = 0.28f), Offset(unit * 2f, size.height - unit * 1.4f), Size(size.width * 0.18f, unit * 0.45f))
        }

        PixelCardMotionScene.Ocean -> {
            repeat(3) { index ->
                val local = (phase * 0.42f + index * 0.31f).normalizedPixelProgress()
                drawCircle(
                    color = palette.highlight.copy(alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.38f),
                    radius = unit * (0.35f + index * 0.08f),
                    center = Offset(size.width - unit * (3f + index * 1.7f), size.height * (0.80f - local * 0.62f)),
                    style = Stroke(width = unit * 0.22f),
                )
            }
        }

        PixelCardMotionScene.Cyber -> {
            val glitch = ((phase * 24f).toInt() % 9) == 0
            if (glitch) {
                drawRect(palette.secondary.copy(alpha = 0.48f), Offset(unit, size.height * 0.18f), Size(size.width * 0.21f, unit * 0.55f))
                drawRect(palette.primary.copy(alpha = 0.46f), Offset(size.width * 0.70f, size.height * 0.72f), Size(size.width * 0.24f, unit * 0.55f))
            }
            drawRect(palette.outline.copy(alpha = 0.34f + pulse * 0.34f), Offset(size.width - unit * 2f, unit * 2f), Size(unit, unit))
        }

        PixelCardMotionScene.ThreeKingdoms -> {
            repeat(2) { index ->
                val ember = (phase * 0.42f + index * 0.47f).normalizedPixelProgress()
                drawRect(
                    palette.secondary.copy(alpha = sin(ember * PI.toFloat()).coerceAtLeast(0f) * 0.46f),
                    Offset(size.width * (0.15f + index * 0.71f), size.height * (0.88f - ember * 0.62f)),
                    Size(unit * 0.65f, unit * 0.65f),
                )
            }
        }

        PixelCardMotionScene.Bianliang -> {
            listOf(0.08f, 0.92f).forEachIndexed { index, x ->
                drawRect(
                    palette.secondary.copy(alpha = 0.30f + pixelPulse(phase, index * 0.34f) * 0.38f),
                    Offset(size.width * x - unit, unit * 2.2f),
                    Size(unit * 1.5f, unit * 1.5f),
                )
            }
        }

        PixelCardMotionScene.FishingHarbor -> {
            val beamY = size.height * (0.24f + phase * 0.42f)
            drawLine(
                color = palette.highlight.copy(alpha = 0.14f + pulse * 0.18f),
                start = Offset(size.width - unit * 2f, unit * 3f),
                end = Offset(size.width * 0.63f, beamY),
                strokeWidth = unit * 0.45f,
            )
        }

        PixelCardMotionScene.TribalJungle -> {
            repeat(3) { index ->
                val fireflyPulse = pixelPulse(phase, index * 0.27f)
                drawRect(
                    palette.highlight.copy(alpha = 0.16f + fireflyPulse * 0.48f),
                    Offset(size.width * (0.10f + index * 0.39f), size.height * (0.16f + index % 2 * 0.62f)),
                    Size(unit * 0.65f, unit * 0.65f),
                )
            }
        }

        PixelCardMotionScene.LavaValley -> {
            repeat(3) { index ->
                val ember = (phase * (0.48f + index * 0.04f) + index * 0.31f).normalizedPixelProgress()
                drawRect(
                    palette.highlight.copy(alpha = sin(ember * PI.toFloat()).coerceAtLeast(0f) * 0.58f),
                    Offset(size.width * (0.10f + index * 0.42f), size.height * (0.88f - ember * 0.76f)),
                    Size(unit * 0.7f, unit * 0.7f),
                )
            }
        }

        PixelCardMotionScene.DunhuangDesert -> {
            val lampPulse = pixelPulse(phase)
            drawRect(
                palette.primary.copy(alpha = 0.28f + lampPulse * 0.40f),
                Offset(size.width - unit * 4f, size.height - unit * 3f),
                Size(unit * 1.2f, unit * 1.8f),
            )
            drawRect(palette.secondary.copy(alpha = 0.32f), Offset(unit * 2f + phase * size.width * 0.25f, unit), Size(unit * 3f, unit * 0.5f))
        }

        PixelCardMotionScene.VikingSnowfield -> {
            repeat(3) { index ->
                val local = (phase * (0.28f + index * 0.03f) + index * 0.33f).normalizedPixelProgress()
                drawRect(
                    palette.highlight.copy(alpha = 0.26f + index * 0.06f),
                    Offset(size.width * (0.08f + index * 0.43f), unit + local * (size.height - unit * 3f)),
                    Size(unit * 0.55f, unit * 0.55f),
                )
            }
        }

        PixelCardMotionScene.JiangnanWatertown -> {
            repeat(2) { index ->
                val local = (phase * 0.55f + index * 0.48f).normalizedPixelProgress()
                val radius = unit * (1.5f + local * 4f)
                drawOval(
                    color = palette.primary.copy(alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.34f),
                    topLeft = Offset(size.width * (0.16f + index * 0.69f) - radius, size.height - unit * 2f - radius * 0.18f),
                    size = Size(radius * 2f, radius * 0.36f),
                    style = Stroke(width = unit * 0.22f),
                )
            }
        }

        PixelCardMotionScene.CloudTown -> {
            val starPulse = pixelPulse(phase)
            drawPixelSparkle(
                center = Offset(size.width - unit * 4f, unit * 3f),
                radius = unit * (0.7f + starPulse * 0.55f),
                color = palette.highlight.copy(alpha = 0.30f + starPulse * 0.38f),
                unit = unit,
            )
        }

        PixelCardMotionScene.PetCompanion -> {
            val starPulse = pixelPulse(phase, 0.18f)
            drawPixelSparkle(
                center = Offset(size.width - unit * 4f, unit * 3f),
                radius = unit * (0.7f + starPulse * 0.45f),
                color = palette.highlight.copy(alpha = 0.30f + starPulse * 0.34f),
                unit = unit,
            )
            repeat(3) { index ->
                val local = (phase * 0.32f + index * 0.29f).normalizedPixelProgress()
                val heartX = size.width * (0.18f + index * 0.27f)
                val heartY = size.height * (0.52f - local * 0.22f)
                drawRect(
                    palette.secondary.copy(alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.42f),
                    Offset(heartX, heartY),
                    Size(unit * 0.7f, unit * 0.7f),
                )
                drawRect(
                    palette.secondary.copy(alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.32f),
                    Offset(heartX + unit * 0.7f, heartY + unit * 0.45f),
                    Size(unit * 0.7f, unit * 0.7f),
                )
            }
            drawPetCompanionHabitatParticles(unit, phase, palette, petHabitat)
        }
    }
}

private fun DrawScope.drawPixelCardMotionTracer(
    unit: Float,
    phase: Float,
    palette: PixelPalette,
) {
    val horizontalTravel = (size.width - unit * 10f).coerceAtLeast(unit)
    val verticalTravel = (size.height - unit * 8f).coerceAtLeast(unit)
    val forwardX = unit * 3f + horizontalTravel * phase
    val reverseX = size.width - unit * 3f - horizontalTravel * phase
    val downY = unit * 3f + verticalTravel * phase
    val upY = size.height - unit * 3f - verticalTravel * phase
    val tracerWidth = (unit * 4f).coerceAtMost(size.width * 0.16f)
    val tracerHeight = maxOf(unit * 0.48f, 1f)

    drawRect(
        color = palette.primary.copy(alpha = 0.58f),
        topLeft = Offset(forwardX - tracerWidth * 0.5f, unit * 0.72f),
        size = Size(tracerWidth, tracerHeight),
    )
    drawRect(
        color = palette.secondary.copy(alpha = 0.48f),
        topLeft = Offset(reverseX - tracerWidth * 0.5f, size.height - unit * 1.2f),
        size = Size(tracerWidth, tracerHeight),
    )
    drawRect(
        color = palette.outline.copy(alpha = 0.44f),
        topLeft = Offset(unit * 0.72f, downY - tracerWidth * 0.5f),
        size = Size(tracerHeight, tracerWidth),
    )
    drawRect(
        color = palette.highlight.copy(alpha = 0.36f),
        topLeft = Offset(size.width - unit * 1.2f, upY - tracerWidth * 0.5f),
        size = Size(tracerHeight, tracerWidth),
    )
}

private fun DrawScope.drawHandheldCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    val scanY = size.height * (0.16f + phase * 0.68f)
    drawRect(
        palette.primary.copy(alpha = 0.075f),
        Offset(unit * 2f, scanY),
        Size(size.width - unit * 4f, unit * 0.55f),
    )
    repeat(5) { index ->
        val active = ((phase * 30f).toInt() + index) % 5 == 0
        drawRect(
            palette.shadow.copy(alpha = if (active) 0.16f else 0.06f),
            Offset(size.width * (0.14f + index * 0.16f), size.height - unit * 3f),
            Size(unit * 1.2f, unit * 1.2f),
        )
    }
}

private fun DrawScope.drawArcadeCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    repeat(4) { lane ->
        val local = (phase + lane * 0.19f).normalizedPixelProgress()
        val x = unit * 3f + local * (size.width - unit * 8f)
        val color = if (lane % 2 == 0) palette.primary else palette.secondary
        drawRect(
            color.copy(alpha = 0.10f + lane * 0.015f),
            Offset(x, size.height * (0.22f + lane * 0.17f)),
            Size(unit * 4f, unit * 0.55f),
        )
    }
}

private fun DrawScope.drawArcadeChaseLights(unit: Float, phase: Float, palette: PixelPalette) {
    val frame = (phase * 24f).toInt()
    repeat(8) { index ->
        val active = (index + frame) % 4 == 0
        val color = if (index % 2 == 0) palette.primary else palette.secondary
        val x = unit * 2f + index * (size.width - unit * 5f) / 7f
        drawRect(
            color.copy(alpha = if (active) 0.72f else 0.18f),
            Offset(x, unit),
            Size(unit, unit),
        )
    }
}

private fun DrawScope.drawPastoralCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    val groundY = size.height - unit * 1.5f
    repeat(8) { index ->
        val stemHeight = unit * (2.4f + index % 3)
        val sway = if (((phase * 16f).toInt() + index) % 4 < 2) -unit * 0.55f else unit * 0.55f
        val x = size.width * (0.07f + index * 0.125f)
        drawLine(
            color = palette.primary.copy(alpha = 0.18f),
            start = Offset(x, groundY),
            end = Offset(x + sway, groundY - stemHeight),
            strokeWidth = unit * 0.35f,
        )
        if (index % 3 == 1) {
            drawRect(
                palette.secondary.copy(alpha = 0.22f),
                Offset(x + sway - unit * 0.55f, groundY - stemHeight - unit * 0.5f),
                Size(unit, unit),
            )
        }
    }
}

private fun DrawScope.drawStarVoyageCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    PIXEL_MOTION_STAR_POINTS.forEachIndexed { index, (x, y) ->
        val pulse = pixelPulse(phase, index * 0.13f)
        drawRect(
            palette.highlight.copy(alpha = 0.07f + pulse * 0.18f),
            Offset(size.width * x, size.height * y),
            Size(unit * (0.45f + index % 2 * 0.25f), unit * (0.45f + index % 2 * 0.25f)),
        )
    }
    val shipX = size.width * (0.68f + phase * 0.16f)
    val shipY = size.height * (0.72f + sin(phase * TAU) * 0.04f)
    drawRect(palette.primary.copy(alpha = 0.14f), Offset(shipX, shipY), Size(unit * 3f, unit))
    drawRect(palette.secondary.copy(alpha = 0.18f), Offset(shipX - unit, shipY + unit * 0.25f), Size(unit, unit * 0.5f))
}

private fun DrawScope.drawInkJadeCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    repeat(3) { index ->
        val local = (phase * (0.24f + index * 0.03f) + index * 0.31f).normalizedPixelProgress()
        val x = -size.width * 0.22f + local * size.width * 1.25f
        val y = size.height * (0.22f + index * 0.24f)
        drawRect(palette.primary.copy(alpha = 0.055f + index * 0.016f), Offset(x, y), Size(size.width * 0.24f, unit))
        drawRect(palette.primary.copy(alpha = 0.045f), Offset(x + unit * 3f, y - unit), Size(size.width * 0.12f, unit))
    }
    drawRect(
        palette.secondary.copy(alpha = 0.10f + pixelPulse(phase) * 0.10f),
        Offset(size.width * 0.78f, size.height * 0.68f),
        Size(unit * 3f, unit * 0.55f),
    )
}

private fun DrawScope.drawWastelandCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    PIXEL_MOTION_DUST_POINTS.forEachIndexed { index, (x, y) ->
        val local = (phase * (0.30f + index % 3 * 0.04f) + index * 0.11f).normalizedPixelProgress()
        val driftX = (x + local * 0.24f).normalizedPixelProgress()
        drawRect(
            palette.primary.copy(alpha = 0.055f + index % 2 * 0.035f),
            Offset(size.width * driftX, size.height * (y + local * 0.08f)),
            Size(unit * 0.55f, unit * 0.55f),
        )
    }
    val sweepX = phase * size.width
    drawRect(palette.secondary.copy(alpha = 0.07f), Offset(sweepX, size.height * 0.18f), Size(unit * 3f, size.height * 0.56f))
}

private fun DrawScope.drawOceanCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    val waterY = size.height - unit * 3.2f
    repeat(9) { index ->
        val lift = if (((phase * 18f).toInt() + index) % 4 < 2) 0f else unit * 0.7f
        drawRect(
            palette.primary.copy(alpha = 0.10f + index % 3 * 0.025f),
            Offset(size.width * (0.04f + index * 0.11f), waterY - lift),
            Size(size.width * 0.09f, unit * 0.65f),
        )
    }
    val fishX = size.width * (0.16f + phase * 0.54f)
    val fishY = size.height * (0.57f + sin(phase * TAU * 2f) * 0.06f)
    drawRect(palette.secondary.copy(alpha = 0.15f), Offset(fishX, fishY), Size(unit * 2.5f, unit))
    drawRect(palette.secondary.copy(alpha = 0.12f), Offset(fishX - unit, fishY - unit * 0.5f), Size(unit, unit * 2f))
}

private fun DrawScope.drawCyberCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    val frame = (phase * 48f).toInt()
    repeat(6) { column ->
        repeat(4) { row ->
            val on = (column * 3 + row + frame) % 5 < 2
            if (on) {
                drawRect(
                    (if (column % 3 == 0) palette.primary else palette.secondary).copy(alpha = 0.08f + row * 0.018f),
                    Offset(size.width * (0.08f + column * 0.17f), size.height * (0.15f + row * 0.18f)),
                    Size(unit * 0.65f, unit * (0.65f + row % 2 * 0.55f)),
                )
            }
        }
    }
    val scanY = phase * size.height
    drawRect(palette.outline.copy(alpha = 0.10f), Offset(unit, scanY), Size(size.width - unit * 2f, unit * 0.45f))
}

private fun DrawScope.drawThreeKingdomsCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    listOf(0.08f, 0.86f).forEachIndexed { index, x ->
        val sway = if (((phase * 12f).toInt() + index) % 2 == 0) unit else 0f
        val poleX = size.width * x
        drawRect(palette.outline.copy(alpha = 0.16f), Offset(poleX, unit * 2f), Size(unit * 0.5f, unit * 6f))
        drawRect(
            (if (index == 0) palette.primary else palette.secondary).copy(alpha = 0.17f),
            Offset(poleX + unit * 0.5f, unit * 2.4f + sway * 0.2f),
            Size(unit * 3.5f + sway, unit * 2f),
        )
    }
    drawRect(palette.shadow.copy(alpha = 0.09f), Offset(0f, size.height - unit * 2.2f), Size(size.width, unit * 0.7f))
}

private fun DrawScope.drawBianliangCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    val riverY = size.height - unit * 3f
    repeat(4) { index ->
        val local = (phase + index * 0.23f).normalizedPixelProgress()
        val x = -size.width * 0.18f + local * size.width * 1.18f
        drawRect(
            (if (index % 2 == 0) palette.primary else palette.secondary).copy(alpha = 0.065f + index * 0.012f),
            Offset(x, riverY + index % 2 * unit),
            Size(size.width * 0.22f, unit * 0.55f),
        )
    }
    val boatX = size.width * (0.20f + phase * 0.46f)
    drawRect(palette.shadow.copy(alpha = 0.15f), Offset(boatX, riverY - unit * 1.4f), Size(unit * 4f, unit * 0.7f))
    drawRect(palette.secondary.copy(alpha = 0.13f), Offset(boatX + unit * 1.4f, riverY - unit * 2.3f), Size(unit * 0.45f, unit))
}

private fun DrawScope.drawFishingHarborCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    val tideY = size.height - unit * 2.6f
    repeat(7) { index ->
        val lifted = ((phase * 14f).toInt() + index) % 3 == 0
        drawRect(
            palette.primary.copy(alpha = if (lifted) 0.16f else 0.08f),
            Offset(size.width * (0.05f + index * 0.14f), tideY - if (lifted) unit * 0.65f else 0f),
            Size(size.width * 0.11f, unit * 0.55f),
        )
    }
    val buoyY = tideY - unit * (1.1f + pixelPulse(phase) * 0.7f)
    drawRect(palette.secondary.copy(alpha = 0.19f), Offset(size.width * 0.18f, buoyY), Size(unit, unit * 2f))
    drawRect(palette.highlight.copy(alpha = 0.16f), Offset(size.width * 0.18f - unit * 0.5f, buoyY), Size(unit * 2f, unit * 0.55f))
}

private fun DrawScope.drawTribalJungleCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    repeat(3) { index ->
        val local = (phase * (0.18f + index * 0.025f) + index * 0.31f).normalizedPixelProgress()
        val x = -size.width * 0.20f + local * size.width * 1.25f
        drawRect(
            palette.primary.copy(alpha = 0.045f + index * 0.018f),
            Offset(x, size.height * (0.24f + index * 0.20f)),
            Size(size.width * 0.20f, unit * (0.7f + index * 0.18f)),
        )
    }
    val crystalPulse = pixelPulse(phase)
    drawRect(
        palette.secondary.copy(alpha = 0.09f + crystalPulse * 0.12f),
        Offset(size.width * 0.76f, size.height - unit * 5f),
        Size(unit * 2f, unit * 3.5f),
    )
}

private fun DrawScope.drawLavaValleyCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    val lavaY = size.height - unit * 3.2f
    drawRect(palette.primary.copy(alpha = 0.10f), Offset(unit, lavaY), Size(size.width - unit * 2f, unit * 1.5f))
    repeat(6) { index ->
        val x = ((index * 0.19f + phase * 0.42f).normalizedPixelProgress()) * size.width
        drawRect(
            palette.highlight.copy(alpha = 0.13f + index % 2 * 0.05f),
            Offset(x, lavaY + index % 2 * unit * 0.7f),
            Size(unit * (1.4f + index % 3), unit * 0.55f),
        )
    }
    val crackPulse = pixelPulse(phase)
    drawLine(
        palette.secondary.copy(alpha = 0.07f + crackPulse * 0.08f),
        Offset(size.width * 0.72f, unit * 2f),
        Offset(size.width * 0.67f, size.height * 0.48f),
        unit * 0.38f,
    )
}

private fun DrawScope.drawDunhuangCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    PIXEL_MOTION_SAND_POINTS.forEachIndexed { index, (x, y) ->
        val local = (phase * (0.22f + index % 3 * 0.025f) + index * 0.13f).normalizedPixelProgress()
        val driftX = (x + local * 0.31f).normalizedPixelProgress()
        drawRect(
            palette.primary.copy(alpha = 0.055f + index % 2 * 0.028f),
            Offset(size.width * driftX, size.height * (y + sin(local * TAU) * 0.025f)),
            Size(unit * 0.55f, unit * 0.55f),
        )
    }
    repeat(5) { index ->
        val y = unit * (2.2f + index % 2 * 0.7f)
        val x = size.width * (0.12f + index * 0.16f) + sin(phase * TAU + index) * unit
        drawRect(palette.secondary.copy(alpha = 0.08f), Offset(x, y), Size(size.width * 0.12f, unit * 0.55f))
    }
}

private fun DrawScope.drawVikingCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    repeat(3) { band ->
        repeat(7) { segment ->
            val x = size.width * (segment / 7f)
            val wave = sin(phase * TAU + segment * 0.88f + band) * unit * (0.7f + band * 0.25f)
            drawRect(
                (if (band == 1) palette.secondary else palette.primary).copy(alpha = 0.055f + band * 0.025f),
                Offset(x, size.height * (0.14f + band * 0.10f) + wave),
                Size(size.width / 6.4f, unit * 0.65f),
            )
        }
    }
    val runePulse = pixelPulse(phase)
    val rune = Offset(size.width * 0.83f, size.height * 0.65f)
    drawRect(palette.secondary.copy(alpha = 0.07f + runePulse * 0.12f), rune, Size(unit, unit * 4f))
    drawRect(palette.secondary.copy(alpha = 0.07f + runePulse * 0.12f), rune - Offset(unit, -unit), Size(unit * 3f, unit))
}

private fun DrawScope.drawJiangnanCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    repeat(7) { index ->
        val local = (phase * (0.62f + index % 3 * 0.06f) + index * 0.14f).normalizedPixelProgress()
        val x = size.width * (0.05f + index * 0.15f)
        val y = -unit * 4f + local * (size.height + unit * 8f)
        drawLine(
            palette.primary.copy(alpha = 0.07f + index % 2 * 0.03f),
            Offset(x, y),
            Offset(x - unit * 1.3f, y + unit * 3f),
            unit * 0.28f,
        )
    }
    val reflection = pixelPulse(phase)
    drawRect(
        palette.secondary.copy(alpha = 0.06f + reflection * 0.09f),
        Offset(size.width * 0.72f, size.height - unit * 4f),
        Size(unit, unit * 2.7f),
    )
}

private fun DrawScope.drawCloudTownCardMotion(unit: Float, phase: Float, palette: PixelPalette) {
    repeat(3) { index ->
        val local = (phase * (0.16f + index * 0.025f) + index * 0.33f).normalizedPixelProgress()
        val x = -size.width * 0.20f + local * size.width * 1.28f
        val y = size.height * (0.22f + index * 0.23f)
        val color = palette.highlight.copy(alpha = 0.055f + index * 0.022f)
        drawRect(color, Offset(x, y), Size(size.width * 0.16f, unit * 1.2f))
        drawRect(color, Offset(x + unit * 2f, y - unit), Size(size.width * 0.09f, unit))
        drawRect(color, Offset(x + unit * 4f, y + unit), Size(size.width * 0.11f, unit))
    }
    val balloonX = size.width * (0.72f + sin(phase * TAU) * 0.04f)
    val balloonY = size.height * (0.26f + cos(phase * TAU) * 0.05f)
    drawRect(palette.secondary.copy(alpha = 0.14f), Offset(balloonX, balloonY), Size(unit * 2f, unit * 2f))
    drawRect(palette.outline.copy(alpha = 0.12f), Offset(balloonX + unit * 0.75f, balloonY + unit * 2f), Size(unit * 0.5f, unit * 2f))
}

private fun DrawScope.drawPetCompanionCardMotion(
    unit: Float,
    phase: Float,
    palette: PixelPalette,
    habitat: PixelPetHabitat,
) {
    val groundY = size.height - unit * 2.2f
    when (habitat) {
        PixelPetHabitat.Garden -> {
            repeat(8) { index ->
                val x = size.width * (0.06f + index * 0.125f)
                val sway = if (((phase * 20f).toInt() + index) % 4 < 2) unit * 0.45f else -unit * 0.45f
                drawLine(
                    color = palette.primary.copy(alpha = 0.14f + index % 2 * 0.025f),
                    start = Offset(x, groundY),
                    end = Offset(x + sway, groundY - unit * (1.3f + index % 3 * 0.45f)),
                    strokeWidth = unit * 0.3f,
                )
            }
        }

        PixelPetHabitat.Cloud -> {
            repeat(3) { index ->
                val local = (phase * 0.20f + index * 0.33f).normalizedPixelProgress()
                val x = -size.width * 0.20f + local * size.width * 1.35f
                val y = size.height * (0.18f + index * 0.19f)
                drawRect(palette.highlight.copy(alpha = 0.10f), Offset(x, y), Size(size.width * 0.24f, unit * 1.1f))
                drawRect(palette.highlight.copy(alpha = 0.08f), Offset(x + unit * 2f, y - unit), Size(size.width * 0.12f, unit))
            }
        }

        PixelPetHabitat.Moon -> {
            repeat(4) { index ->
                val sparkle = pixelPulse(phase, index * 0.22f)
                drawRect(
                    palette.highlight.copy(alpha = 0.16f + sparkle * 0.26f),
                    Offset(size.width * (0.10f + index * 0.19f), size.height * (0.16f + index % 2 * 0.18f)),
                    Size(unit * 0.6f, unit * 0.6f),
                )
            }
        }

        PixelPetHabitat.Lagoon -> {
            repeat(3) { index ->
                val local = (phase * 0.46f + index * 0.31f).normalizedPixelProgress()
                val radius = unit * (1.6f + local * 4f)
                drawOval(
                    color = palette.highlight.copy(alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.28f),
                    topLeft = Offset(size.width * (0.18f + index * 0.29f) - radius, groundY - radius * 0.18f),
                    size = Size(radius * 2f, radius * 0.36f),
                    style = Stroke(width = unit * 0.18f),
                )
            }
        }
    }
    val breath = pixelPulse(phase, 0.35f)
    val petX = size.width * 0.74f
    val petY = size.height * (0.58f - breath * 0.018f)
    drawRect(
        palette.primary.copy(alpha = 0.12f + breath * 0.08f),
        Offset(petX, petY),
        Size(unit * 3.2f, unit * 2.2f),
    )
    drawRect(
        palette.secondary.copy(alpha = 0.14f + breath * 0.08f),
        Offset(petX + unit * 0.55f, petY - unit * 0.75f),
        Size(unit * 2.1f, unit * 1.25f),
    )
}

private fun DrawScope.drawPetCompanionHabitatParticles(
    unit: Float,
    phase: Float,
    palette: PixelPalette,
    habitat: PixelPetHabitat,
) {
    when (habitat) {
        PixelPetHabitat.Garden -> repeat(3) { index ->
            val local = (phase * 0.28f + index * 0.31f).normalizedPixelProgress()
            drawRect(
                palette.highlight.copy(alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.28f),
                Offset(size.width * (0.12f + index * 0.30f), size.height * (0.76f - local * 0.34f)),
                Size(unit * 0.5f, unit * 0.5f),
            )
        }

        PixelPetHabitat.Cloud -> repeat(2) { index ->
            val local = (phase * 0.22f + index * 0.48f).normalizedPixelProgress()
            drawRect(
                palette.highlight.copy(alpha = 0.16f + pixelPulse(local) * 0.18f),
                Offset(size.width * local, size.height * (0.28f + index * 0.26f)),
                Size(unit * 2f, unit * 0.45f),
            )
        }

        PixelPetHabitat.Moon -> repeat(3) { index ->
            val pulse = pixelPulse(phase, index * 0.27f)
            drawPixelSparkle(
                center = Offset(size.width * (0.18f + index * 0.28f), size.height * (0.22f + index % 2 * 0.18f)),
                radius = unit * (0.38f + pulse * 0.28f),
                color = palette.highlight.copy(alpha = 0.16f + pulse * 0.28f),
                unit = unit,
            )
        }

        PixelPetHabitat.Lagoon -> repeat(3) { index ->
            val local = (phase * 0.38f + index * 0.29f).normalizedPixelProgress()
            drawCircle(
                color = palette.highlight.copy(alpha = sin(local * PI.toFloat()).coerceAtLeast(0f) * 0.26f),
                radius = unit * 0.38f,
                center = Offset(size.width * (0.16f + index * 0.30f), size.height * (0.78f - local * 0.44f)),
            )
        }
    }
}

private fun DrawScope.drawPixelSparkle(
    center: Offset,
    radius: Float,
    color: Color,
    unit: Float,
) {
    drawRect(color, Offset(center.x - radius, center.y - unit * 0.22f), Size(radius * 2f, unit * 0.44f))
    drawRect(color, Offset(center.x - unit * 0.22f, center.y - radius), Size(unit * 0.44f, radius * 2f))
}

private fun DrawScope.hasPixelCardMotionBounds(): Boolean {
    return size.width >= 72.dp.toPx() && size.height >= 48.dp.toPx()
}

private fun pixelPulse(progress: Float, offset: Float = 0f): Float {
    return (sin((progress + offset) * TAU) + 1f) * 0.5f
}

private fun Float.normalizedPixelProgress(): Float = ((this % 1f) + 1f) % 1f

private val PixelStepEasing = Easing { fraction ->
    val frame = (fraction * PIXEL_CARD_MOTION_FRAME_COUNT).toInt()
    (frame / PIXEL_CARD_MOTION_FRAME_COUNT.toFloat()).coerceIn(0f, 1f)
}

private val PIXEL_MOTION_STAR_POINTS = listOf(
    0.08f to 0.18f,
    0.18f to 0.72f,
    0.31f to 0.34f,
    0.48f to 0.16f,
    0.62f to 0.64f,
    0.78f to 0.28f,
    0.91f to 0.76f,
)

private val PIXEL_MOTION_DUST_POINTS = listOf(
    0.05f to 0.16f,
    0.18f to 0.42f,
    0.31f to 0.24f,
    0.44f to 0.68f,
    0.58f to 0.34f,
    0.72f to 0.58f,
    0.86f to 0.20f,
)

private val PIXEL_MOTION_SAND_POINTS = listOf(
    0.04f to 0.22f,
    0.14f to 0.62f,
    0.27f to 0.34f,
    0.41f to 0.76f,
    0.55f to 0.18f,
    0.68f to 0.52f,
    0.82f to 0.30f,
    0.93f to 0.70f,
)

private const val STATIC_PIXEL_CARD_MOTION_PROGRESS = 0.37f
private const val PIXEL_CARD_MOTION_CYCLE_MILLIS = 6_400
private const val PIXEL_CARD_MOTION_FRAME_COUNT = 64
private const val TAU = (PI * 2.0).toFloat()
