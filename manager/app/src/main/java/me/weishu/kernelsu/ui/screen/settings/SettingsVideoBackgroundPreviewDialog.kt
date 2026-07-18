package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.CustomVideoBackground
import me.weishu.kernelsu.ui.component.CustomVideoPassthroughBackground
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperPassthroughOpacity
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

@Composable
fun SettingsVideoBackgroundPreviewDialog(
    show: Boolean,
    uriString: String?,
    durationSeconds: Int,
    opacity: Float,
    passthroughEnabled: Boolean,
    passthroughOpacity: Float,
    onDismissRequest: () -> Unit,
) {
    if (!show) return

    MiuixVideoBackgroundPreviewDialog(
        uriString = uriString,
        durationSeconds = durationSeconds,
        opacity = opacity,
        passthroughEnabled = passthroughEnabled,
        passthroughOpacity = passthroughOpacity,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun MiuixVideoBackgroundPreviewDialog(
    uriString: String?,
    durationSeconds: Int,
    opacity: Float,
    passthroughEnabled: Boolean,
    passthroughOpacity: Float,
    onDismissRequest: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = stringResource(R.string.settings_video_background_preview),
        onDismissRequest = onDismissRequest,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VideoPreviewFrame(
                    uriString = uriString,
                    durationSeconds = durationSeconds,
                    opacity = opacity,
                    passthroughEnabled = passthroughEnabled,
                    passthroughOpacity = passthroughOpacity,
                )
                MiuixTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(android.R.string.ok),
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}

@Composable
private fun VideoPreviewFrame(
    uriString: String?,
    durationSeconds: Int,
    opacity: Float,
    passthroughEnabled: Boolean,
    passthroughOpacity: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (uriString.isNullOrBlank()) {
            Text(
                modifier = Modifier.padding(24.dp),
                text = stringResource(R.string.settings_video_background_not_selected),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Box
        }

        CustomVideoBackground(
            uriString = uriString,
            durationSeconds = durationSeconds,
            opacity = opacity,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_wallpaper_preview_hint),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.settings_wallpaper_preview_ui_sample),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (passthroughEnabled) {
            val passthroughAlpha = sanitizeCustomWallpaperPassthroughOpacity(passthroughOpacity)
            CustomVideoPassthroughBackground(
                uriString = uriString,
                durationSeconds = durationSeconds,
                imageAlpha = passthroughAlpha,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }
    }
}
