package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.MediaFileInfo
import me.weishu.kernelsu.ui.util.MediaMotionStyle
import me.weishu.kernelsu.ui.util.MediaVariantMode
import me.weishu.kernelsu.ui.util.MediaVariantSettings
import me.weishu.kernelsu.ui.util.MediaVisualSettings
import me.weishu.kernelsu.ui.util.formatMediaBytes
import me.weishu.kernelsu.ui.util.formatMediaDuration
import me.weishu.kernelsu.ui.util.inspectMediaFile
import kotlin.math.roundToInt

@Composable
internal fun MediaVisualControls(
    value: MediaVisualSettings,
    onValueChange: (MediaVisualSettings) -> Unit,
    modifier: Modifier = Modifier,
    showContrast: Boolean = true,
    showTemperature: Boolean = false,
    showNoise: Boolean = false,
    showMotion: Boolean = false,
) {
    val settings = value.normalized()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MediaEditorSlider(
            title = stringResource(R.string.media_editor_brightness),
            value = settings.brightness,
            valueRange = -0.6f..0.6f,
            valueLabel = { signedPercent(it) },
            onValueChange = { onValueChange(settings.copy(brightness = it).normalized()) },
        )
        if (showContrast) {
            MediaEditorSlider(
                title = stringResource(R.string.media_editor_contrast),
                value = settings.contrast,
                valueRange = 0.5f..1.8f,
                valueLabel = { "${(it * 100).roundToInt()}%" },
                onValueChange = { onValueChange(settings.copy(contrast = it).normalized()) },
            )
        }
        MediaEditorSlider(
            title = stringResource(R.string.media_editor_saturation),
            value = settings.saturation,
            valueRange = 0f..2f,
            valueLabel = { "${(it * 100).roundToInt()}%" },
            onValueChange = { onValueChange(settings.copy(saturation = it).normalized()) },
        )
        if (showTemperature) {
            MediaEditorSlider(
                title = stringResource(R.string.media_editor_temperature),
                value = settings.temperature,
                valueRange = -1f..1f,
                valueLabel = { signedPercent(it) },
                onValueChange = { onValueChange(settings.copy(temperature = it).normalized()) },
            )
        }
        MediaEditorSlider(
            title = stringResource(R.string.media_editor_opacity),
            value = settings.opacity,
            valueRange = 0.1f..1f,
            valueLabel = { "${(it * 100).roundToInt()}%" },
            onValueChange = { onValueChange(settings.copy(opacity = it).normalized()) },
        )
        MediaEditorSlider(
            title = stringResource(R.string.media_editor_blur),
            value = settings.blurRadius,
            valueRange = 0f..28f,
            valueLabel = { "${it.roundToInt()} dp" },
            onValueChange = { onValueChange(settings.copy(blurRadius = it).normalized()) },
        )
        MediaEditorSlider(
            title = stringResource(R.string.media_editor_overlay),
            value = settings.overlayAlpha,
            valueRange = 0f..0.82f,
            valueLabel = { "${(it * 100).roundToInt()}%" },
            onValueChange = { onValueChange(settings.copy(overlayAlpha = it).normalized()) },
        )
        if (showNoise) {
            MediaEditorSlider(
                title = stringResource(R.string.media_editor_noise),
                value = settings.noiseAlpha,
                valueRange = 0f..0.22f,
                valueLabel = { "${(it * 100).roundToInt()}%" },
                onValueChange = { onValueChange(settings.copy(noiseAlpha = it).normalized()) },
            )
        }
        if (showMotion) {
            Text(
                text = stringResource(R.string.media_editor_motion),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MediaMotionStyle.entries.forEach { style ->
                    FilterChip(
                        selected = settings.motionStyle == style,
                        onClick = { onValueChange(settings.copy(motionStyle = style)) },
                        label = { Text(stringResource(style.labelRes())) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun MediaFileInfoSummary(
    info: MediaFileInfo?,
    modifier: Modifier = Modifier,
) {
    if (info == null) return
    val details = listOfNotNull(
        info.mimeType,
        formatMediaBytes(info.sizeBytes),
        info.resolution,
        formatMediaDuration(info.durationMillis),
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = info.displayName,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        if (details.isNotEmpty()) {
            Text(
                text = details.joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (!info.decodable) {
            Text(
                text = stringResource(R.string.media_editor_invalid_file),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun rememberMediaFileInfo(uriString: String?): MediaFileInfo? {
    val context = LocalContext.current
    return produceState<MediaFileInfo?>(initialValue = null, context, uriString) {
        value = inspectMediaFile(context, uriString)
    }.value
}

@Composable
internal fun MediaVariantControls(
    value: MediaVariantSettings,
    onValueChange: (MediaVariantSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = value.normalized()
    val minuteFormat = stringResource(R.string.media_variant_minutes)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.media_variant_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MediaVariantMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.mode == mode,
                    onClick = { onValueChange(settings.copy(mode = mode)) },
                    label = { Text(stringResource(mode.labelRes())) },
                )
            }
        }
        when (settings.mode) {
            MediaVariantMode.FollowSystem -> Text(
                text = stringResource(R.string.media_variant_follow_system_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )

            MediaVariantMode.Schedule -> {
                MediaEditorSlider(
                    title = stringResource(R.string.media_variant_day_start),
                    value = settings.dayStartMinutes.toFloat(),
                    valueRange = 0f..1439f,
                    valueLabel = { formatMinutes(it.roundToInt()) },
                    onValueChange = {
                        onValueChange(settings.copy(dayStartMinutes = it.roundToInt()).normalized())
                    },
                )
                MediaEditorSlider(
                    title = stringResource(R.string.media_variant_night_start),
                    value = settings.nightStartMinutes.toFloat(),
                    valueRange = 0f..1439f,
                    valueLabel = { formatMinutes(it.roundToInt()) },
                    onValueChange = {
                        onValueChange(settings.copy(nightStartMinutes = it.roundToInt()).normalized())
                    },
                )
            }

            MediaVariantMode.Random -> MediaEditorSlider(
                title = stringResource(R.string.media_variant_random_interval),
                value = settings.randomIntervalMinutes.toFloat(),
                valueRange = 5f..240f,
                valueLabel = { minuteFormat.format(it.roundToInt()) },
                onValueChange = {
                    onValueChange(settings.copy(randomIntervalMinutes = it.roundToInt()).normalized())
                },
            )
        }
    }
}

@Composable
internal fun MediaEditorSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = valueLabel(value),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

private fun signedPercent(value: Float): String {
    val percent = (value * 100).roundToInt()
    return if (percent > 0) "+$percent%" else "$percent%"
}

private fun MediaMotionStyle.labelRes(): Int = when (this) {
    MediaMotionStyle.None -> R.string.media_motion_none
    MediaMotionStyle.Parallax -> R.string.media_motion_parallax
    MediaMotionStyle.SlowZoom -> R.string.media_motion_slow_zoom
    MediaMotionStyle.SlowPan -> R.string.media_motion_slow_pan
}

private fun MediaVariantMode.labelRes(): Int = when (this) {
    MediaVariantMode.FollowSystem -> R.string.media_variant_follow_system
    MediaVariantMode.Schedule -> R.string.media_variant_schedule
    MediaVariantMode.Random -> R.string.media_variant_random
}

private fun formatMinutes(value: Int): String {
    val safe = value.coerceIn(0, 1439)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
