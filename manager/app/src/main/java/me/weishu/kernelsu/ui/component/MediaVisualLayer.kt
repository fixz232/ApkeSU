package me.weishu.kernelsu.ui.component

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.util.MediaMotionStyle
import me.weishu.kernelsu.ui.util.MediaVisualSettings
import kotlin.math.abs

@Composable
fun MediaVisualLayer(
    settings: MediaVisualSettings,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(ColorFilter?) -> Unit,
) {
    val value = settings.normalized()
    val phase = rememberMediaMotionPhase(value.motionStyle != MediaMotionStyle.None)
    val tilt = rememberDeviceTilt(value.motionStyle == MediaMotionStyle.Parallax)
    val motionScale = when (value.motionStyle) {
        MediaMotionStyle.SlowZoom -> 1.035f + phase * 0.012f
        MediaMotionStyle.SlowPan, MediaMotionStyle.Parallax -> 1.045f
        MediaMotionStyle.None -> 1f
    }
    val translationX = when (value.motionStyle) {
        MediaMotionStyle.SlowPan -> phase * 10f
        MediaMotionStyle.Parallax -> tilt.x * 8f
        else -> 0f
    }
    val translationY = when (value.motionStyle) {
        MediaMotionStyle.SlowPan -> phase * -5f
        MediaMotionStyle.Parallax -> tilt.y * 8f
        else -> 0f
    }
    val colorFilter = mediaColorFilter(value)
    val effectModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            alpha = value.opacity
            scaleX = motionScale * if (value.transform.flipHorizontal) -1f else motionScale
            scaleY = motionScale
            rotationZ = value.transform.quarterTurns * 90f
            this.translationX = translationX
            this.translationY = translationY
            clip = true
        }
        .then(
            if (value.blurRadius > 0.05f) {
                Modifier.blur(value.blurRadius.dp)
            } else {
                Modifier
            }
        )

    Box(modifier = modifier) {
        Box(modifier = effectModifier) {
            Box(
                modifier = if (colorFilter == null) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer(colorFilter = colorFilter)
                }
            ) {
                // The layer owns color processing so images and TextureView videos behave identically.
                content(null)
            }
            MediaVisualOverlay(value)
        }
    }
}

@Composable
private fun rememberMediaMotionPhase(enabled: Boolean): Float {
    if (!enabled) return 0f
    val transition = rememberInfiniteTransition(label = "media-visual-motion")
    val phase by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "media-visual-phase",
    )
    return phase
}

@Composable
private fun BoxScope.MediaVisualOverlay(settings: MediaVisualSettings) {
    if (settings.brightness > 0f) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = settings.brightness * 0.42f))
        )
    } else if (settings.brightness < 0f) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = abs(settings.brightness) * 0.62f))
        )
    }
    if (settings.temperature != 0f) {
        val color = if (settings.temperature > 0f) Color(0xFFFFA45B) else Color(0xFF78B8FF)
        Box(
            Modifier
                .fillMaxSize()
                .background(color.copy(alpha = abs(settings.temperature) * 0.12f))
        )
    }
    if (settings.overlayAlpha > 0f) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = settings.overlayAlpha))
        )
    }
    if (settings.noiseAlpha > 0.002f) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = 120
            repeat(count) { index ->
                val xSeed = (index * 73 + 17) % 101
                val ySeed = (index * 47 + 29) % 103
                val x = size.width * xSeed / 100f
                val y = size.height * ySeed / 102f
                drawCircle(
                    color = if (index % 3 == 0) {
                        Color.White.copy(alpha = settings.noiseAlpha * 0.55f)
                    } else {
                        Color.Black.copy(alpha = settings.noiseAlpha * 0.38f)
                    },
                    radius = if (index % 5 == 0) 1.2f else 0.65f,
                    center = Offset(x, y),
                )
            }
        }
    }
}

private fun mediaColorFilter(settings: MediaVisualSettings): ColorFilter? {
    val neutral = settings.saturation == 1f && settings.contrast == 1f
    if (neutral) return null
    val saturation = ColorMatrix().apply { setToSaturation(settings.saturation) }
    val contrast = settings.contrast
    val translate = 128f * (1f - contrast)
    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f,
        )
    )
    saturation.timesAssign(contrastMatrix)
    return ColorFilter.colorMatrix(saturation)
}

@Composable
private fun rememberDeviceTilt(enabled: Boolean): Offset {
    val context = LocalContext.current
    var tilt by remember(enabled) { mutableStateOf(Offset.Zero) }
    DisposableEffect(context, enabled) {
        if (!enabled) {
            tilt = Offset.Zero
            return@DisposableEffect onDispose { }
        }
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = (-event.values[0] / 9.81f).coerceIn(-1f, 1f)
                val y = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                tilt = Offset(
                    x = tilt.x * 0.82f + x * 0.18f,
                    y = tilt.y * 0.82f + y * 0.18f,
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (manager != null && sensor != null) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { manager?.unregisterListener(listener) }
    }
    return tilt
}
