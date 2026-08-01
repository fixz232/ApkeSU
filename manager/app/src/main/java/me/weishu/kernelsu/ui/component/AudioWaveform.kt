package me.weishu.kernelsu.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun AudioWaveform(
    uriString: String?,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val samples = produceState<FloatArray?>(null, uriString, context) {
        value = withContext(Dispatchers.IO) { readWaveformSamples(context, uriString) }
    }.value ?: return
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        val step = size.width / samples.size
        samples.forEachIndexed { index, sample ->
            val x = step * (index + 0.5f)
            val halfHeight = (size.height * (0.10f + sample * 0.38f)).coerceAtMost(size.height / 2f)
            drawLine(
                color = if (index.toFloat() / samples.size <= progress.coerceIn(0f, 1f)) activeColor else inactiveColor,
                start = Offset(x, size.height / 2f - halfHeight),
                end = Offset(x, size.height / 2f + halfHeight),
                strokeWidth = (step * 0.44f).coerceAtLeast(1f),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun readWaveformSamples(context: android.content.Context, uriString: String?): FloatArray? {
    if (uriString.isNullOrBlank()) return null
    val bucketCount = 96
    val energy = LongArray(bucketCount)
    val counts = IntArray(bucketCount)
    return runCatching {
        context.contentResolver.openInputStream(uriString.toUri())?.use { input ->
            val buffer = ByteArray(16 * 1024)
            var totalRead = 0
            val maxRead = 1024 * 1024
            while (totalRead < maxRead) {
                val count = input.read(buffer, 0, minOf(buffer.size, maxRead - totalRead))
                if (count <= 0) break
                for (index in 0 until count) {
                    val bucket = ((totalRead + index).toLong() * bucketCount / maxRead).toInt()
                        .coerceIn(0, bucketCount - 1)
                    energy[bucket] += abs((buffer[index].toInt() and 0xFF) - 128)
                    counts[bucket]++
                }
                totalRead += count
            }
            if (totalRead <= 0) return@use null
            val raw = FloatArray(bucketCount) { index ->
                if (counts[index] == 0) 0f else energy[index].toFloat() / counts[index] / 128f
            }
            val peak = raw.maxOrNull()?.coerceAtLeast(0.01f) ?: 1f
            FloatArray(bucketCount) { index -> (raw[index] / peak).coerceIn(0.05f, 1f) }
        }
    }.getOrNull()
}
