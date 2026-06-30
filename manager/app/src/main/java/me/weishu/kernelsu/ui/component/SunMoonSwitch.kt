package me.weishu.kernelsu.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R

const val SWITCH_STYLE_KEY = "switch_style"

enum class SwitchStyle(
    val value: String,
    val labelRes: Int,
) {
    Original("original", R.string.settings_switch_style_original),
    SunMoon("sun_moon", R.string.settings_switch_style_sun_moon),
    Capsule("capsule", R.string.settings_switch_style_capsule),
    Minimal("minimal", R.string.settings_switch_style_minimal),
    Neon("neon", R.string.settings_switch_style_neon);

    companion object {
        val Default = SunMoon
        const val DEFAULT_VALUE = "sun_moon"

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
fun SunMoonSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedShowsNight: Boolean = false,
) {
    StyledSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        checkedShowsNight = checkedShowsNight,
        style = SwitchStyle.SunMoon,
    )
}

@Composable
fun StyledSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedShowsNight: Boolean = false,
    style: SwitchStyle = LocalSwitchStyle.current,
) {
    val resolvedStyle = if (style == SwitchStyle.Original) SwitchStyle.SunMoon else style
    val isNight = if (checkedShowsNight) checked else !checked
    val progressTarget = when {
        resolvedStyle == SwitchStyle.SunMoon && isNight -> 0f
        checked -> 1f
        else -> 0f
    }
    val knobProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 220),
        label = "sunMoonSwitchKnob",
    )
    val trackColor by animateColorAsState(
        targetValue = switchTrackColor(resolvedStyle, checked, isNight),
        animationSpec = tween(durationMillis = 220),
        label = "sunMoonSwitchTrack",
    )
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
            SwitchStyle.SunMoon -> drawSunMoonTrack(trackColor, isNight, alpha)
            SwitchStyle.Capsule -> drawCapsuleTrack(trackColor, checked, alpha)
            SwitchStyle.Minimal -> drawMinimalTrack(trackColor, checked, alpha)
            SwitchStyle.Neon -> drawNeonTrack(trackColor, checked, alpha)
            SwitchStyle.Original -> Unit
        }

        val inset = 3.dp.toPx()
        val radius = size.height / 2f - inset
        val startX = inset + radius
        val endX = size.width - inset - radius
        val center = Offset(startX + (endX - startX) * knobProgress, size.height / 2f)

        when (resolvedStyle) {
            SwitchStyle.SunMoon -> drawSunMoonKnob(trackColor, isNight, center, radius, alpha)
            SwitchStyle.Capsule -> drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = radius,
                center = center,
            )
            SwitchStyle.Minimal -> drawCircle(
                color = if (checked) Color(0xFF111827).copy(alpha = alpha) else Color.White.copy(alpha = alpha),
                radius = radius * 0.78f,
                center = center,
            )
            SwitchStyle.Neon -> {
                drawCircle(
                    color = if (checked) Color(0xFF8CFFEA).copy(alpha = 0.28f * alpha) else Color(0xFFFFF2A6).copy(alpha = 0.22f * alpha),
                    radius = radius * 1.25f,
                    center = center,
                )
                drawCircle(
                    color = if (checked) Color(0xFFE9FFFB).copy(alpha = alpha) else Color(0xFFFFF7D1).copy(alpha = alpha),
                    radius = radius * 0.88f,
                    center = center,
                )
            }
            SwitchStyle.Original -> Unit
        }
    }
}

private fun switchTrackColor(style: SwitchStyle, checked: Boolean, isNight: Boolean): Color {
    return when (style) {
        SwitchStyle.SunMoon -> if (isNight) Color(0xFF2C303C) else Color(0xFFA9DFF1)
        SwitchStyle.Capsule -> if (checked) Color(0xFF34C77B) else Color(0xFFD8DEE8)
        SwitchStyle.Minimal -> if (checked) Color(0xFFE6E9F0) else Color(0xFFF3F4F6)
        SwitchStyle.Neon -> if (checked) Color(0xFF11152F) else Color(0xFF30323B)
        SwitchStyle.Original -> Color.Transparent
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunMoonTrack(
    trackColor: Color,
    isNight: Boolean,
    alpha: Float,
) {
    val corner = size.height / 2f
    drawRoundRect(
        color = trackColor.copy(alpha = alpha),
        size = size,
        cornerRadius = CornerRadius(corner, corner),
    )

    if (isNight) {
        val starColor = Color.White.copy(alpha = 0.82f * alpha)
        drawCircle(starColor, radius = 1.4.dp.toPx(), center = Offset(size.width * 0.63f, size.height * 0.26f))
        drawCircle(starColor, radius = 1.1.dp.toPx(), center = Offset(size.width * 0.77f, size.height * 0.44f))
        drawCircle(starColor, radius = 1.2.dp.toPx(), center = Offset(size.width * 0.58f, size.height * 0.68f))
        drawCircle(starColor, radius = 0.9.dp.toPx(), center = Offset(size.width * 0.86f, size.height * 0.24f))
    } else {
        val cloudColor = Color.White.copy(alpha = 0.86f * alpha)
        val baseY = size.height * 0.60f
        drawCircle(cloudColor, radius = 5.0.dp.toPx(), center = Offset(size.width * 0.28f, baseY))
        drawCircle(cloudColor, radius = 6.2.dp.toPx(), center = Offset(size.width * 0.36f, baseY - 2.2.dp.toPx()))
        drawCircle(cloudColor, radius = 4.7.dp.toPx(), center = Offset(size.width * 0.44f, baseY))
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(size.width * 0.25f, baseY - 2.5.dp.toPx()),
            size = Size(23.dp.toPx(), 8.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunMoonKnob(
    trackColor: Color,
    isNight: Boolean,
    center: Offset,
    radius: Float,
    alpha: Float,
) {
    drawCircle(
        color = if (isNight) Color(0xFFF2EFD9).copy(alpha = alpha) else Color(0xFFFFF1A3).copy(alpha = alpha),
        radius = radius,
        center = center,
    )

    if (isNight) {
        drawCircle(
            color = Color(0xFFD9D19A).copy(alpha = alpha),
            radius = radius * 0.56f,
            center = center.copy(x = center.x - radius * 0.08f),
        )
        drawCircle(
            color = trackColor.copy(alpha = alpha),
            radius = radius * 0.50f,
            center = center.copy(x = center.x + radius * 0.20f, y = center.y - radius * 0.05f),
        )
    } else {
        val sunColor = Color(0xFFFFCF48).copy(alpha = alpha)
        drawCircle(sunColor, radius = radius * 0.60f, center = center)
        drawCircle(Color(0xFFFFF7B9).copy(alpha = 0.88f * alpha), radius = radius * 0.34f, center = center)
    }
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMinimalTrack(
    trackColor: Color,
    checked: Boolean,
    alpha: Float,
) {
    val inset = 2.dp.toPx()
    val corner = (size.height - inset * 2f) / 2f
    drawRoundRect(
        color = trackColor.copy(alpha = alpha),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(corner, corner),
    )
    drawRoundRect(
        color = if (checked) Color(0xFF111827).copy(alpha = 0.88f * alpha) else Color(0xFF9CA3AF).copy(alpha = 0.78f * alpha),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 1.4.dp.toPx()),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeonTrack(
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
    val accent = if (checked) Color(0xFF36F3D1) else Color(0xFFFFD86B)
    drawRoundRect(
        color = accent.copy(alpha = 0.45f * alpha),
        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
        size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 1.6.dp.toPx()),
    )
    drawCircle(
        color = accent.copy(alpha = 0.18f * alpha),
        radius = 18.dp.toPx(),
        center = if (checked) Offset(size.width * 0.70f, size.height * 0.50f) else Offset(size.width * 0.30f, size.height * 0.50f),
    )
}
