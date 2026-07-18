package me.weishu.kernelsu.ui.screen.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.BackgroundMusicPlayer
import me.weishu.kernelsu.ui.util.ClickSoundPlayer
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_AUDIO_VOLUME
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_STARTUP_SOUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_AUDIO_VOLUME
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_STARTUP_SOUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.StartupSoundPlayer
import me.weishu.kernelsu.ui.util.releasePersistableAudioReadPermission
import me.weishu.kernelsu.ui.util.takePersistableAudioReadPermission
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

@Composable
fun SoundEffectsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val startupSoundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        StartupSoundPlayer.clearAutoPlaySuppression()
        uri ?: return@rememberLauncherForActivityResult
        takePersistableAudioReadPermission(context, uri)
        val uriString = uri.toString()
        viewModel.setCustomStartupSoundUri(uriString)
        StartupSoundPlayer.play(
            context = context,
            uriString = uriString,
            durationSeconds = uiState.customStartupSoundDurationSeconds,
            volume = uiState.customStartupSoundVolume,
        ) {
            Toast.makeText(context, R.string.settings_startup_sound_play_failed, Toast.LENGTH_SHORT).show()
        }
    }

    val clickSoundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        takePersistableAudioReadPermission(context, uri)
        val uriString = uri.toString()
        viewModel.setCustomClickSoundUri(uriString)
        ClickSoundPlayer.play(context, uriString, uiState.customClickSoundVolume) {
            Toast.makeText(context, R.string.settings_click_sound_play_failed, Toast.LENGTH_SHORT).show()
        }
    }

    val backgroundMusicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        takePersistableAudioReadPermission(context, uri)
        val uriString = uri.toString()
        viewModel.setCustomBackgroundMusicUri(uriString)
        BackgroundMusicPlayer.play(context, uriString, uiState.customBackgroundMusicVolume) {
            Toast.makeText(context, R.string.settings_background_music_play_failed, Toast.LENGTH_SHORT).show()
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val actions = SoundEffectsActions(
        onPickStartupSound = {
            StartupSoundPlayer.suppressNextAutoPlay()
            startupSoundLauncher.launch(arrayOf("audio/*"))
        },
        onPreviewStartupSound = {
            StartupSoundPlayer.play(
                context = context,
                uriString = uiState.customStartupSoundUri,
                durationSeconds = uiState.customStartupSoundDurationSeconds,
                volume = uiState.customStartupSoundVolume,
            ) {
                Toast.makeText(context, R.string.settings_startup_sound_play_failed, Toast.LENGTH_SHORT).show()
            }
        },
        onClearStartupSound = {
            StartupSoundPlayer.stop()
            releasePersistableAudioReadPermission(context, uiState.customStartupSoundUri)
            viewModel.clearCustomStartupSound()
        },
        onSetStartupSoundDurationSeconds = viewModel::setCustomStartupSoundDurationSeconds,
        onSetStartupSoundVolume = viewModel::setCustomStartupSoundVolume,
        onPickClickSound = { clickSoundLauncher.launch(arrayOf("audio/*")) },
        onPreviewClickSound = {
            ClickSoundPlayer.play(context, uiState.customClickSoundUri, uiState.customClickSoundVolume) {
                Toast.makeText(context, R.string.settings_click_sound_play_failed, Toast.LENGTH_SHORT).show()
            }
        },
        onClearClickSound = {
            ClickSoundPlayer.release()
            releasePersistableAudioReadPermission(context, uiState.customClickSoundUri)
            viewModel.clearCustomClickSound()
        },
        onSetClickSoundVolume = viewModel::setCustomClickSoundVolume,
        onPickBackgroundMusic = { backgroundMusicLauncher.launch(arrayOf("audio/*")) },
        onPreviewBackgroundMusic = {
            BackgroundMusicPlayer.play(
                context = context,
                uriString = uiState.customBackgroundMusicUri,
                volume = uiState.customBackgroundMusicVolume,
            ) {
                Toast.makeText(context, R.string.settings_background_music_play_failed, Toast.LENGTH_SHORT).show()
            }
        },
        onClearBackgroundMusic = {
            BackgroundMusicPlayer.stop()
            releasePersistableAudioReadPermission(context, uiState.customBackgroundMusicUri)
            viewModel.clearCustomBackgroundMusic()
        },
        onSetBackgroundMusicVolume = {
            viewModel.setCustomBackgroundMusicVolume(it)
            BackgroundMusicPlayer.updateVolume(it)
        },
    )

    SoundEffectsScreenMiuix(
        uiState = uiState,
        actions = actions,
        onBack = onBack,
    )
}

@Composable
private fun SoundEffectsScreenMiuix(
    uiState: SettingsUiState,
    actions: SoundEffectsActions,
    onBack: () -> Unit,
) {
    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.settings_sound_effects),
                color = Color.Transparent,
                titleColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface,
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        SoundEffectsContent(
            uiState = uiState,
            actions = actions,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SoundEffectsContent(
    uiState: SettingsUiState,
    actions: SoundEffectsActions,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_sound_effects_summary),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        SoundEditorCard(
            title = stringResource(R.string.settings_startup_sound),
            summary = stringResource(
                if (uiState.customStartupSoundUri.isNullOrBlank()) {
                    R.string.settings_startup_sound_summary
                } else {
                    R.string.settings_startup_sound_selected_summary
                }
            ),
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            selected = !uiState.customStartupSoundUri.isNullOrBlank(),
            onPick = actions.onPickStartupSound,
            onPreview = actions.onPreviewStartupSound,
            onClear = actions.onClearStartupSound,
        ) {
            SoundSlider(
                title = stringResource(R.string.settings_startup_sound_duration),
                icon = Icons.Rounded.Timer,
                value = uiState.customStartupSoundDurationSeconds.toFloat(),
                valueRange = MIN_CUSTOM_STARTUP_SOUND_DURATION_SECONDS.toFloat()..
                    MAX_CUSTOM_STARTUP_SOUND_DURATION_SECONDS.toFloat(),
                valueLabel = { stringResource(R.string.settings_startup_sound_duration_value, it.roundToInt()) },
                onValueChange = { actions.onSetStartupSoundDurationSeconds(it.roundToInt()) },
            )
            SoundSlider(
                title = stringResource(R.string.settings_startup_sound_volume),
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                value = uiState.customStartupSoundVolume,
                valueRange = MIN_CUSTOM_AUDIO_VOLUME..MAX_CUSTOM_AUDIO_VOLUME,
                valueLabel = { stringResource(R.string.settings_audio_volume_value, (it * 100).roundToInt()) },
                onValueChange = actions.onSetStartupSoundVolume,
            )
        }

        SoundEditorCard(
            title = stringResource(R.string.settings_click_sound),
            summary = stringResource(
                if (uiState.customClickSoundUri.isNullOrBlank()) {
                    R.string.settings_click_sound_summary
                } else {
                    R.string.settings_click_sound_selected_summary
                }
            ),
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            selected = !uiState.customClickSoundUri.isNullOrBlank(),
            onPick = actions.onPickClickSound,
            onPreview = actions.onPreviewClickSound,
            onClear = actions.onClearClickSound,
        ) {
            SoundSlider(
                title = stringResource(R.string.settings_click_sound_volume),
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                value = uiState.customClickSoundVolume,
                valueRange = MIN_CUSTOM_AUDIO_VOLUME..MAX_CUSTOM_AUDIO_VOLUME,
                valueLabel = { stringResource(R.string.settings_audio_volume_value, (it * 100).roundToInt()) },
                onValueChange = actions.onSetClickSoundVolume,
            )
        }

        SoundEditorCard(
            title = stringResource(R.string.settings_background_music),
            summary = stringResource(
                if (uiState.customBackgroundMusicUri.isNullOrBlank()) {
                    R.string.settings_background_music_summary
                } else {
                    R.string.settings_background_music_selected_summary
                }
            ),
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            selected = !uiState.customBackgroundMusicUri.isNullOrBlank(),
            onPick = actions.onPickBackgroundMusic,
            onPreview = actions.onPreviewBackgroundMusic,
            onClear = actions.onClearBackgroundMusic,
        ) {
            SoundSlider(
                title = stringResource(R.string.settings_background_music_volume),
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                value = uiState.customBackgroundMusicVolume,
                valueRange = MIN_CUSTOM_AUDIO_VOLUME..MAX_CUSTOM_AUDIO_VOLUME,
                valueLabel = { stringResource(R.string.settings_audio_volume_value, (it * 100).roundToInt()) },
                onValueChange = actions.onSetBackgroundMusicVolume,
            )
        }
    }
}

@Composable
private fun SoundEditorCard(
    title: String,
    summary: String,
    icon: ImageVector,
    selected: Boolean,
    onPick: () -> Unit,
    onPreview: () -> Unit,
    onClear: () -> Unit,
    controls: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = summary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = onPick,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_audio_choose),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onPreview,
                    enabled = selected,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_audio_preview),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onClear,
                    enabled = selected,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_audio_clear),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (selected) {
                controls()
            }
        }
    }
}

@Composable
private fun SoundSlider(
    title: String,
    icon: ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: @Composable (Float) -> String,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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

private data class SoundEffectsActions(
    val onPickStartupSound: () -> Unit,
    val onPreviewStartupSound: () -> Unit,
    val onClearStartupSound: () -> Unit,
    val onSetStartupSoundDurationSeconds: (Int) -> Unit,
    val onSetStartupSoundVolume: (Float) -> Unit,
    val onPickClickSound: () -> Unit,
    val onPreviewClickSound: () -> Unit,
    val onClearClickSound: () -> Unit,
    val onSetClickSoundVolume: (Float) -> Unit,
    val onPickBackgroundMusic: () -> Unit,
    val onPreviewBackgroundMusic: () -> Unit,
    val onClearBackgroundMusic: () -> Unit,
    val onSetBackgroundMusicVolume: (Float) -> Unit,
)
