package me.weishu.kernelsu.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val SWITCH_STYLE_KEY = "switch_style"

enum class SwitchStyle(
    val value: String,
    val labelRes: Int,
) {
    Original("original", R.string.settings_switch_style_original),
    Capsule("capsule", R.string.settings_switch_style_capsule),
    CloudStar("cloud_star", R.string.settings_switch_style_cloud_star),
    Bb8("bb8", R.string.settings_switch_style_bb8),
    Sparkle("sparkle", R.string.settings_switch_style_sparkle);

    companion object {
        val Default = CloudStar
        const val DEFAULT_VALUE = "cloud_star"

        fun fromValue(value: String?): SwitchStyle {
            return entries.firstOrNull { it.value == value } ?: Default
        }

        fun fromIndex(index: Int): SwitchStyle {
            return entries.getOrElse(index) { Default }
        }

        fun selectedIndex(value: String): Int {
            return entries.indexOf(fromValue(value)).coerceAtLeast(0)
        }
    }
}

val LocalSwitchStyle = compositionLocalOf { SwitchStyle.Default }

@Composable
fun StyledSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: SwitchStyle = LocalSwitchStyle.current,
) {
    val resolvedStyle = if (style == SwitchStyle.Original) SwitchStyle.CloudStar else style
    val progressTarget = if (checked) 1f else 0f
    val knobProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 220),
        label = "sunMoonSwitchKnob",
    )
    val trackColor by animateColorAsState(
        targetValue = switchTrackColor(resolvedStyle, checked),
        animationSpec = tween(durationMillis = 220),
        label = "sunMoonSwitchTrack",
    )
    val ambientMotion = if (
        resolvedStyle == SwitchStyle.CloudStar ||
        resolvedStyle == SwitchStyle.Bb8 ||
        resolvedStyle == SwitchStyle.Sparkle
    ) {
        val transition = rememberInfiniteTransition(label = "animatedSwitch")
        val motion by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "switchAmbientMotion",
        )
        motion
    } else {
        0f
    }
    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        )
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .size(width = 74.dp, height = 38.dp)
            .then(toggleModifier)
    ) {
        val alpha = if (enabled) 1f else 0.45f
        val corner = size.height / 2f

        when (resolvedStyle) {
            SwitchStyle.Capsule -> drawCapsuleTrack(trackColor, checked, alpha)
            SwitchStyle.CloudStar -> drawCloudStarTrack(
                trackColor = trackColor,
                nightProgress = knobProgress,
                ambientMotion = ambientMotion,
                alpha = alpha,
            )
            SwitchStyle.Bb8 -> drawBb8Track(
                trackColor = trackColor,
                nightProgress = knobProgress,
                ambientMotion = ambientMotion,
                alpha = alpha,
            )
            SwitchStyle.Sparkle -> drawSparkleTrack(
                trackColor = trackColor,
                activeProgress = knobProgress,
                alpha = alpha,
            )
            SwitchStyle.Original -> Unit
        }

        val inset = 3.dp.toPx()
        val radius = size.height / 2f - inset
        val startX = inset + radius
        val endX = size.width - inset - radius
        val center = Offset(startX + (endX - startX) * knobProgress, size.height / 2f)

        when (resolvedStyle) {
            SwitchStyle.Capsule -> drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = radius,
                center = center,
            )
            SwitchStyle.CloudStar -> drawCloudStarKnob(
                center = center,
                radius = radius,
                nightProgress = knobProgress,
                alpha = alpha,
            )
            SwitchStyle.Bb8 -> drawBb8Droid(
                movementProgress = knobProgress,
                ambientMotion = ambientMotion,
                alpha = alpha,
            )
            SwitchStyle.Sparkle -> drawSparkleKnob(
                center = center,
                radius = radius,
                activeProgress = knobProgress,
                ambientMotion = ambientMotion,
                alpha = alpha,
            )
            SwitchStyle.Original -> Unit
        }
    }
}

private fun switchTrackColor(style: SwitchStyle, checked: Boolean): Color {
    return when (style) {
        SwitchStyle.Capsule -> if (checked) Color(0xFF34C77B) else Color(0xFFD8DEE8)
        SwitchStyle.CloudStar -> if (checked) Color(0xFF090B12) else Color(0xFF2196F3)
        SwitchStyle.Bb8 -> if (checked) Color(0xFF111A39) else Color(0xFF7FA9C0)
        SwitchStyle.Sparkle -> if (checked) Color(0xFF172A3D) else Color(0xFF121212)
        SwitchStyle.Original -> Color.Transparent
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudStarTrack(
    trackColor: Color,
    nightProgress: Float,
    ambientMotion: Float,
    alpha: Float,
) {
    val corner = size.height / 2f
    drawRoundRect(
        color = trackColor.copy(alpha = alpha),
        size = size,
        cornerRadius = CornerRadius(corner, corner),
    )

    val inset = 3.dp.toPx()
    val radius = size.height / 2f - inset
    val knobCenter = Offset(
        x = inset + radius + (size.width - inset * 2f - radius * 2f) * nightProgress,
        y = size.height / 2f,
    )
    val dayAlpha = (1f - nightProgress).coerceIn(0f, 1f) * alpha
    val nightAlpha = nightProgress.coerceIn(0f, 1f) * alpha

    repeat(3) { index ->
        drawCircle(
            color = Color.White.copy(alpha = dayAlpha * (0.10f - index * 0.02f)),
            radius = radius * (1.25f + index * 0.34f),
            center = knobCenter,
        )
    }

    val cloudDrift = sin(ambientMotion * TWO_PI) * 2.dp.toPx()
    drawCloudLayer(
        centerX = size.width * 0.70f + cloudDrift,
        centerY = size.height * 0.64f,
        color = Color(0xFFCAD4DF).copy(alpha = dayAlpha * 0.82f),
        scale = 1f,
    )
    drawCloudLayer(
        centerX = size.width * 0.78f - cloudDrift * 0.55f,
        centerY = size.height * 0.72f,
        color = Color(0xFFF2F5F8).copy(alpha = dayAlpha * 0.92f),
        scale = 0.82f,
    )

    val starColor = Color.White.copy(alpha = nightAlpha)
    drawTwinklingStar(
        center = Offset(size.width * 0.13f, size.height * 0.25f),
        radius = 3.8.dp.toPx(),
        color = starColor.copy(alpha = starColor.alpha * twinkle(ambientMotion, 0.10f)),
    )
    drawTwinklingStar(
        center = Offset(size.width * 0.28f, size.height * 0.68f),
        radius = 2.6.dp.toPx(),
        color = starColor.copy(alpha = starColor.alpha * twinkle(ambientMotion, 0.42f)),
    )
    drawTwinklingStar(
        center = Offset(size.width * 0.38f, size.height * 0.25f),
        radius = 2.dp.toPx(),
        color = starColor.copy(alpha = starColor.alpha * twinkle(ambientMotion, 0.74f)),
    )
    drawCircle(
        color = starColor.copy(alpha = starColor.alpha * 0.72f),
        radius = 1.1.dp.toPx(),
        center = Offset(size.width * 0.20f, size.height * 0.48f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudStarKnob(
    center: Offset,
    radius: Float,
    nightProgress: Float,
    alpha: Float,
) {
    drawCircle(
        color = lerp(Color(0xFFFFDE2E), Color(0xFFF2F3F5), nightProgress).copy(alpha = alpha),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = Color(0xFFFFF5A6).copy(alpha = (1f - nightProgress) * 0.45f * alpha),
        radius = radius * 0.58f,
        center = center.copy(x = center.x - radius * 0.16f, y = center.y - radius * 0.18f),
    )

    val craterColor = Color(0xFF9CA0A6).copy(alpha = nightProgress * 0.72f * alpha)
    drawCircle(
        color = craterColor,
        radius = radius * 0.20f,
        center = center.copy(x = center.x - radius * 0.30f, y = center.y + radius * 0.02f),
    )
    drawCircle(
        color = craterColor,
        radius = radius * 0.13f,
        center = center.copy(x = center.x + radius * 0.17f, y = center.y - radius * 0.32f),
    )
    drawCircle(
        color = craterColor,
        radius = radius * 0.09f,
        center = center.copy(x = center.x + radius * 0.25f, y = center.y + radius * 0.32f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudLayer(
    centerX: Float,
    centerY: Float,
    color: Color,
    scale: Float,
) {
    if (color.alpha <= 0f) return
    drawCircle(color, radius = 5.2.dp.toPx() * scale, center = Offset(centerX - 6.dp.toPx() * scale, centerY))
    drawCircle(color, radius = 7.dp.toPx() * scale, center = Offset(centerX, centerY - 3.dp.toPx() * scale))
    drawCircle(color, radius = 5.dp.toPx() * scale, center = Offset(centerX + 7.dp.toPx() * scale, centerY))
    drawRoundRect(
        color = color,
        topLeft = Offset(centerX - 12.dp.toPx() * scale, centerY - 1.dp.toPx() * scale),
        size = Size(24.dp.toPx() * scale, 7.dp.toPx() * scale),
        cornerRadius = CornerRadius(4.dp.toPx() * scale, 4.dp.toPx() * scale),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTwinklingStar(
    center: Offset,
    radius: Float,
    color: Color,
) {
    if (color.alpha <= 0f) return
    val inner = radius * 0.20f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
    drawPath(path = path, color = color)
}

private fun twinkle(progress: Float, phase: Float): Float {
    return 0.66f + 0.34f * ((sin((progress + phase) * TWO_PI) + 1f) * 0.5f)
}

private const val TWO_PI = (PI * 2.0).toFloat()

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkleTrack(
    trackColor: Color,
    activeProgress: Float,
    alpha: Float,
) {
    val corner = size.height / 2f
    val borderColor = lerp(Color(0xFF777777), Color(0xFF54A8FC), activeProgress)
    drawRoundRect(
        color = Color(0xFF414344).copy(alpha = alpha),
        size = size,
        cornerRadius = CornerRadius(corner, corner),
    )

    val inset = 3.dp.toPx()
    drawRoundRect(
        color = trackColor.copy(alpha = alpha),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(corner - inset, corner - inset),
    )
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF3A9BFC).copy(alpha = (0.22f + activeProgress * 0.18f) * alpha),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.5f, -size.height * 0.72f),
            radius = size.width * 0.86f,
        ),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(corner - inset, corner - inset),
    )
    drawRoundRect(
        color = borderColor.copy(alpha = (0.68f + activeProgress * 0.22f) * alpha),
        size = size,
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 1.dp.toPx()),
    )

    if (activeProgress > 0f) {
        val radius = size.height / 2f - inset
        val startX = inset + radius
        val endX = size.width - inset - radius
        val center = Offset(startX + (endX - startX) * activeProgress, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0080FF).copy(alpha = 0.24f * activeProgress * alpha),
                    Color.Transparent,
                ),
                center = center,
                radius = radius * 2.1f,
            ),
            radius = radius * 2.1f,
            center = center,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkleKnob(
    center: Offset,
    radius: Float,
    activeProgress: Float,
    ambientMotion: Float,
    alpha: Float,
) {
    val primary = Color(0xFF54A8FC)
    val topColor = lerp(Color(0xFF666666), Color(0xFF045AB1), activeProgress)
    val edgeColor = lerp(Color(0xFF414344), primary, activeProgress)
    drawCircle(
        color = Color.Black.copy(alpha = 0.22f * alpha),
        radius = radius * 1.05f,
        center = center.copy(y = center.y + 1.2.dp.toPx()),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                topColor.copy(alpha = alpha),
                edgeColor.copy(alpha = alpha),
            ),
            center = center.copy(y = center.y - radius * 0.72f),
            radius = radius * 1.75f,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = lerp(Color(0xFFAAAAAA), primary, activeProgress).copy(alpha = alpha),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
    )
    drawArc(
        color = primary.copy(alpha = (0.50f + activeProgress * 0.38f) * alpha),
        startAngle = 18f,
        sweepAngle = 144f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.77f, center.y - radius * 0.77f),
        size = Size(radius * 1.54f, radius * 1.54f),
        style = Stroke(width = 1.35.dp.toPx()),
    )

    SPARKLE_ANGLES.forEachIndexed { index, angleDegrees ->
        val speed = (index % 5 + 1).toFloat()
        val phase = (ambientMotion * speed + index * 0.071f) % 1f
        val angle = angleDegrees * PI.toFloat() / 180f
        val distance = radius * (0.14f + phase * 0.72f)
        val particleRadius = (if (index % 3 == 0) 0.95f else 0.58f).dp.toPx() *
            (1f - phase * 0.36f)
        drawCircle(
            color = lerp(Color(0xFFD9D9D9), Color(0xFFACACAC), activeProgress).copy(
                alpha = (0.24f + (1f - phase) * 0.68f) * alpha,
            ),
            radius = particleRadius,
            center = Offset(
                center.x + cos(angle) * distance,
                center.y + sin(angle) * distance,
            ),
        )
    }

    rotate(degrees = -225f * activeProgress, pivot = center) {
        drawTwinklingStar(
            center = center,
            radius = radius * 0.38f,
            color = Color(0xFFF1F3F5).copy(alpha = alpha),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBb8Track(
    trackColor: Color,
    nightProgress: Float,
    ambientMotion: Float,
    alpha: Float,
) {
    val corner = size.height / 2f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                lerp(Color(0xFF7DA7BE), Color(0xFF2C4770), nightProgress).copy(alpha = alpha),
                trackColor.copy(alpha = alpha),
                lerp(Color(0xFFA6C5D4), Color(0xFF070E2B), nightProgress).copy(alpha = alpha),
            ),
        ),
        size = size,
        cornerRadius = CornerRadius(corner, corner),
    )

    val dayAlpha = (1f - nightProgress).coerceIn(0f, 1f) * alpha
    val nightAlpha = nightProgress.coerceIn(0f, 1f) * alpha
    drawRoundRect(
        color = lerp(Color(0xFFB18D71), Color(0xFF544137), nightProgress)
            .copy(alpha = 0.74f * alpha),
        topLeft = Offset(0f, size.height * 0.79f),
        size = Size(size.width, size.height * 0.21f),
        cornerRadius = CornerRadius(corner * 0.34f, corner * 0.34f),
    )

    val cloudDrift = sin(ambientMotion * TWO_PI) * 1.5.dp.toPx()
    drawCloudLayer(
        centerX = size.width * 0.76f + cloudDrift + nightProgress * size.width * 0.38f,
        centerY = size.height * 0.34f,
        color = Color.White.copy(alpha = dayAlpha * 0.68f),
        scale = 0.54f,
    )
    drawCloudLayer(
        centerX = size.width * 0.53f - cloudDrift * 0.45f + nightProgress * size.width * 0.48f,
        centerY = size.height * 0.60f,
        color = Color(0xFFD6DEE3).copy(alpha = dayAlpha * 0.55f),
        scale = 0.46f,
    )

    drawCircle(
        color = Color(0xFFFEF8E8).copy(alpha = dayAlpha * 0.92f),
        radius = 4.2.dp.toPx(),
        center = Offset(size.width * 0.70f, size.height * 0.26f - nightProgress * size.height),
    )
    drawCircle(
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFFE6AC5C).copy(alpha = dayAlpha),
                Color(0xFFD75449).copy(alpha = dayAlpha),
            )
        ),
        radius = 3.4.dp.toPx(),
        center = Offset(size.width * 0.88f, size.height * 0.66f + nightProgress * size.height),
    )

    val starPositions = listOf(
        0.10f to 0.22f,
        0.22f to 0.52f,
        0.38f to 0.18f,
        0.49f to 0.64f,
        0.65f to 0.30f,
        0.78f to 0.55f,
        0.90f to 0.20f,
    )
    starPositions.forEachIndexed { index, (x, y) ->
        drawCircle(
            color = Color.White.copy(
                alpha = nightAlpha * (0.48f + 0.42f * twinkle(ambientMotion, index * 0.13f)),
            ),
            radius = (if (index % 3 == 0) 1.15f else 0.75f).dp.toPx(),
            center = Offset(size.width * x, size.height * (1f - nightProgress + y * nightProgress)),
        )
    }

    drawCircle(
        color = Color(0xFFB8C4CB).copy(alpha = nightAlpha * 0.82f),
        radius = 4.8.dp.toPx(),
        center = Offset(size.width * 0.18f, size.height * (1.18f - nightProgress * 0.88f)),
    )
    drawCircle(
        color = Color(0xFF8198A7).copy(alpha = nightAlpha * 0.72f),
        radius = 2.2.dp.toPx(),
        center = Offset(size.width * 0.23f, size.height * (1.10f - nightProgress * 0.68f)),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBb8Droid(
    movementProgress: Float,
    ambientMotion: Float,
    alpha: Float,
) {
    val bodyRadius = 10.2.dp.toPx()
    val sideInset = bodyRadius + 4.dp.toPx()
    val robotX = sideInset + (size.width - sideInset * 2f) * movementProgress
    val bodyCenter = Offset(robotX, size.height - bodyRadius - 3.dp.toPx())
    val accent = Color(0xFFDE7D2F).copy(alpha = alpha)
    val bodyColor = Color(0xFFF7F7F3).copy(alpha = alpha)
    val outline = Color(0xFFADB1B4).copy(alpha = alpha)

    drawOval(
        color = Color(0xFF3A271C).copy(alpha = 0.20f * alpha),
        topLeft = Offset(bodyCenter.x - bodyRadius * 1.12f, size.height - 6.dp.toPx()),
        size = Size(bodyRadius * 2.24f, 5.dp.toPx()),
    )

    rotate(
        degrees = 45f + movementProgress * 180f + sin(ambientMotion * TWO_PI) * 2.5f,
        pivot = bodyCenter,
    ) {
        drawCircle(bodyColor, radius = bodyRadius, center = bodyCenter)
        drawCircle(outline, radius = bodyRadius, center = bodyCenter, style = Stroke(0.9.dp.toPx()))
        drawCircle(accent, radius = bodyRadius * 0.60f, center = bodyCenter, style = Stroke(1.8.dp.toPx()))
        drawCircle(
            color = Color(0xFFECECEA).copy(alpha = alpha),
            radius = bodyRadius * 0.40f,
            center = bodyCenter,
        )
        drawLine(
            color = accent,
            start = bodyCenter.copy(x = bodyCenter.x - bodyRadius * 0.58f),
            end = bodyCenter.copy(x = bodyCenter.x + bodyRadius * 0.58f),
            strokeWidth = 1.35.dp.toPx(),
        )
        drawLine(
            color = accent,
            start = bodyCenter.copy(y = bodyCenter.y - bodyRadius * 0.58f),
            end = bodyCenter.copy(y = bodyCenter.y + bodyRadius * 0.58f),
            strokeWidth = 1.35.dp.toPx(),
        )
        repeat(4) { index ->
            val angle = index * (PI.toFloat() / 2f) + PI.toFloat() / 4f
            drawCircle(
                color = Color(0xFFD7D9D8).copy(alpha = alpha),
                radius = 0.72.dp.toPx(),
                center = Offset(
                    bodyCenter.x + kotlin.math.cos(angle) * bodyRadius * 0.82f,
                    bodyCenter.y + sin(angle) * bodyRadius * 0.82f,
                ),
            )
        }
    }

    val headHalfWidth = 7.2.dp.toPx()
    val headHeight = 7.7.dp.toPx()
    val headBaseY = bodyCenter.y - bodyRadius + 1.2.dp.toPx()
    val headPath = Path().apply {
        moveTo(robotX - headHalfWidth, headBaseY)
        cubicTo(
            robotX - headHalfWidth,
            headBaseY - headHeight * 0.64f,
            robotX - headHalfWidth * 0.42f,
            headBaseY - headHeight,
            robotX,
            headBaseY - headHeight,
        )
        cubicTo(
            robotX + headHalfWidth * 0.42f,
            headBaseY - headHeight,
            robotX + headHalfWidth,
            headBaseY - headHeight * 0.64f,
            robotX + headHalfWidth,
            headBaseY,
        )
        close()
    }
    drawPath(headPath, bodyColor)
    drawPath(headPath, outline, style = Stroke(0.8.dp.toPx()))
    drawLine(
        color = accent,
        start = Offset(robotX - headHalfWidth * 0.88f, headBaseY - headHeight * 0.32f),
        end = Offset(robotX + headHalfWidth * 0.88f, headBaseY - headHeight * 0.32f),
        strokeWidth = 1.25.dp.toPx(),
    )
    drawCircle(
        color = Color(0xFF17191C).copy(alpha = alpha),
        radius = 2.05.dp.toPx(),
        center = Offset(robotX - 1.4.dp.toPx(), headBaseY - headHeight * 0.58f),
    )
    drawCircle(
        color = Color(0xFFE53935).copy(alpha = alpha),
        radius = 0.65.dp.toPx(),
        center = Offset(robotX - 1.7.dp.toPx(), headBaseY - headHeight * 0.54f),
    )
    drawCircle(
        color = Color(0xFF4D5358).copy(alpha = alpha),
        radius = 0.95.dp.toPx(),
        center = Offset(robotX + 4.dp.toPx(), headBaseY - headHeight * 0.42f),
    )
    drawLine(
        color = Color(0xFFB8BDC0).copy(alpha = alpha),
        start = Offset(robotX + 2.7.dp.toPx(), headBaseY - headHeight + 0.5.dp.toPx()),
        end = Offset(robotX + 2.7.dp.toPx(), headBaseY - headHeight - 4.2.dp.toPx()),
        strokeWidth = 0.75.dp.toPx(),
    )
    drawLine(
        color = Color(0xFFB8BDC0).copy(alpha = alpha),
        start = Offset(robotX, headBaseY - headHeight + 0.4.dp.toPx()),
        end = Offset(robotX, headBaseY - headHeight - 2.2.dp.toPx()),
        strokeWidth = 0.65.dp.toPx(),
    )
    drawLine(
        color = Color(0xFF22262A).copy(alpha = alpha),
        start = Offset(robotX + 2.7.dp.toPx(), headBaseY - headHeight - 4.2.dp.toPx()),
        end = Offset(robotX + 2.7.dp.toPx(), headBaseY - headHeight - 5.5.dp.toPx()),
        strokeWidth = 0.9.dp.toPx(),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCapsuleTrack(
    trackColor: Color,
    checked: Boolean,
    alpha: Float,
) {
    val corner = size.height / 2f
    drawRoundRect(
        color = trackColor.copy(alpha = alpha),
        size = size,
        cornerRadius = CornerRadius(corner, corner),
    )
    val markColor = Color.White.copy(alpha = 0.78f * alpha)
    if (checked) {
        drawCircle(markColor, radius = 3.2.dp.toPx(), center = Offset(size.width * 0.28f, size.height * 0.50f))
        drawCircle(markColor, radius = 1.9.dp.toPx(), center = Offset(size.width * 0.40f, size.height * 0.50f))
    } else {
        drawRoundRect(
            color = Color(0xFF9AA3B2).copy(alpha = 0.55f * alpha),
            topLeft = Offset(size.width * 0.56f, size.height * 0.46f),
            size = Size(15.dp.toPx(), 2.5.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )
    }
}

private val SPARKLE_ANGLES = floatArrayOf(
    25f, 100f, 280f, 200f, 30f, 300f, 250f, 210f,
    100f, 15f, 75f, 65f, 50f, 320f, 220f, 215f,
    135f, 45f, 78f, 89f, 65f, 97f, 174f, 236f,
)
