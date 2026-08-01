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
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.component.AudioWaveform
import me.weishu.kernelsu.ui.util.BackgroundMusicPlayer
import me.weishu.kernelsu.ui.util.AppAudioSettings
import me.weishu.kernelsu.ui.util.AudioPreviewPlayer
import me.weishu.kernelsu.ui.util.AudioPreviewState
import me.weishu.kernelsu.ui.util.AudioTrackSettings
import me.weishu.kernelsu.ui.util.AudioScheme
import me.weishu.kernelsu.ui.util.ClickSoundPlayer
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_AUDIO_VOLUME
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_STARTUP_SOUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_AUDIO_VOLUME
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_STARTUP_SOUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.StartupSoundPlayer
import me.weishu.kernelsu.ui.util.readAppAudioSettings
import me.weishu.kernelsu.ui.util.readAudioSchemes
import me.weishu.kernelsu.ui.util.saveCurrentAudioScheme
import me.weishu.kernelsu.ui.util.applyAudioScheme
import me.weishu.kernelsu.ui.util.deleteAudioScheme
import me.weishu.kernelsu.ui.util.inspectMediaFile
import me.weishu.kernelsu.ui.util.isAudioUriReferencedBySavedScheme
import me.weishu.kernelsu.ui.util.persistCustomAudioReference
import me.weishu.kernelsu.ui.util.releaseCustomAudioReference
import me.weishu.kernelsu.ui.util.setAppAudioSettings
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var audioSettings by remember(context) { mutableStateOf(readAppAudioSettings(context)) }
    var audioSchemes by remember(context) { mutableStateOf(readAudioSchemes(context)) }
    val previewState by AudioPreviewPlayer.state.collectAsState()
    val scope = rememberCoroutineScope()

    fun updateAudioSettings(next: AppAudioSettings) {
        val previous = audioSettings
        val value = next.normalized()
        setAppAudioSettings(context, value)
        audioSettings = value
        if (!value.masterEnabled ||
            previous.startup.enabled && !value.startup.enabled ||
            previous.click.enabled && !value.click.enabled ||
            previous.background.enabled && !value.background.enabled
        ) {
            AudioPreviewPlayer.stop()
        }
        if (!value.masterEnabled) {
            StartupSoundPlayer.stop()
            ClickSoundPlayer.release()
            BackgroundMusicPlayer.stop()
        } else if (value.background.enabled && !uiState.customBackgroundMusicUri.isNullOrBlank()) {
            BackgroundMusicPlayer.playConfigured(context)
        } else {
            BackgroundMusicPlayer.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose { AudioPreviewPlayer.stop() }
    }

    val startupSoundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        StartupSoundPlayer.clearAutoPlaySuppression()
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            if (!inspectMediaFile(context, uri).decodable) {
                Toast.makeText(context, R.string.settings_audio_invalid_file, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val uriString = withContext(Dispatchers.IO) {
                persistCustomAudioReference(context, uri, "startup_sound")
            }
            if (uriString == null) {
                Toast.makeText(context, R.string.settings_audio_import_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (uriString != uiState.customStartupSoundUri) {
                val previous = uiState.customStartupSoundUri
                if (!isAudioUriReferencedBySavedScheme(context, previous)) {
                    releaseCustomAudioReference(context, previous)
                }
            }
            val enabledSettings = audioSettings.copy(
                masterEnabled = true,
                startup = audioSettings.startup.copy(enabled = true),
            )
            updateAudioSettings(enabledSettings)
            viewModel.setCustomStartupSoundUri(uriString)
            AudioPreviewPlayer.play(
                context = context,
                uriString = uriString,
                volume = uiState.customStartupSoundVolume,
                settings = enabledSettings.startup,
            ) {
                Toast.makeText(context, R.string.settings_startup_sound_play_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val clickSoundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            if (!inspectMediaFile(context, uri).decodable) {
                Toast.makeText(context, R.string.settings_audio_invalid_file, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val uriString = withContext(Dispatchers.IO) {
                persistCustomAudioReference(context, uri, "click_sound")
            }
            if (uriString == null) {
                Toast.makeText(context, R.string.settings_audio_import_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (uriString != uiState.customClickSoundUri) {
                val previous = uiState.customClickSoundUri
                if (!isAudioUriReferencedBySavedScheme(context, previous)) {
                    releaseCustomAudioReference(context, previous)
                }
            }
            val enabledSettings = audioSettings.copy(
                masterEnabled = true,
                click = audioSettings.click.copy(enabled = true),
            )
            updateAudioSettings(enabledSettings)
            viewModel.setCustomClickSoundUri(uriString)
            AudioPreviewPlayer.play(context, uriString, uiState.customClickSoundVolume, enabledSettings.click) {
                Toast.makeText(context, R.string.settings_click_sound_play_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val backgroundMusicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            if (!inspectMediaFile(context, uri).decodable) {
                Toast.makeText(context, R.string.settings_audio_invalid_file, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val uriString = withContext(Dispatchers.IO) {
                persistCustomAudioReference(context, uri, "background_music")
            }
            if (uriString == null) {
                Toast.makeText(context, R.string.settings_audio_import_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (uriString != uiState.customBackgroundMusicUri) {
                val previous = uiState.customBackgroundMusicUri
                if (!isAudioUriReferencedBySavedScheme(context, previous)) {
                    releaseCustomAudioReference(context, previous)
                }
            }
            val enabledSettings = audioSettings.copy(
                masterEnabled = true,
                background = audioSettings.background.copy(enabled = true),
            )
            updateAudioSettings(enabledSettings)
            viewModel.setCustomBackgroundMusicUri(uriString)
            AudioPreviewPlayer.play(context, uriString, uiState.customBackgroundMusicVolume, enabledSettings.background) {
                Toast.makeText(context, R.string.settings_background_music_play_failed, Toast.LENGTH_SHORT).show()
            }
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
            AudioPreviewPlayer.play(
                context = context,
                uriString = uiState.customStartupSoundUri,
                volume = uiState.customStartupSoundVolume,
                settings = audioSettings.startup,
            ) {
                Toast.makeText(context, R.string.settings_startup_sound_play_failed, Toast.LENGTH_SHORT).show()
            }
        },
        onClearStartupSound = {
            StartupSoundPlayer.stop()
            if (!isAudioUriReferencedBySavedScheme(context, uiState.customStartupSoundUri)) {
                releaseCustomAudioReference(context, uiState.customStartupSoundUri)
            }
            viewModel.clearCustomStartupSound()
        },
        onSetStartupSoundDurationSeconds = viewModel::setCustomStartupSoundDurationSeconds,
        onSetStartupSoundVolume = viewModel::setCustomStartupSoundVolume,
        onPickClickSound = { clickSoundLauncher.launch(arrayOf("audio/*")) },
        onPreviewClickSound = {
            AudioPreviewPlayer.play(
                context,
                uiState.customClickSoundUri,
                uiState.customClickSoundVolume,
                audioSettings.click,
            ) {
                Toast.makeText(context, R.string.settings_click_sound_play_failed, Toast.LENGTH_SHORT).show()
            }
        },
        onClearClickSound = {
            ClickSoundPlayer.release()
            if (!isAudioUriReferencedBySavedScheme(context, uiState.customClickSoundUri)) {
                releaseCustomAudioReference(context, uiState.customClickSoundUri)
            }
            viewModel.clearCustomClickSound()
        },
        onSetClickSoundVolume = viewModel::setCustomClickSoundVolume,
        onPickBackgroundMusic = { backgroundMusicLauncher.launch(arrayOf("audio/*")) },
        onPreviewBackgroundMusic = {
            AudioPreviewPlayer.play(
                context = context,
                uriString = uiState.customBackgroundMusicUri,
                volume = uiState.customBackgroundMusicVolume,
                settings = audioSettings.background,
            ) {
                Toast.makeText(context, R.string.settings_background_music_play_failed, Toast.LENGTH_SHORT).show()
            }
        },
        onClearBackgroundMusic = {
            BackgroundMusicPlayer.stop()
            if (!isAudioUriReferencedBySavedScheme(context, uiState.customBackgroundMusicUri)) {
                releaseCustomAudioReference(context, uiState.customBackgroundMusicUri)
            }
            viewModel.clearCustomBackgroundMusic()
        },
        onSetBackgroundMusicVolume = {
            viewModel.setCustomBackgroundMusicVolume(it)
            BackgroundMusicPlayer.updateVolume(it)
        },
        onSetAudioSettings = ::updateAudioSettings,
        onPausePreview = AudioPreviewPlayer::pause,
        onResumePreview = AudioPreviewPlayer::resume,
        onStopPreview = AudioPreviewPlayer::stop,
        onSeekPreview = AudioPreviewPlayer::seekTo,
        onSaveScheme = { name ->
            val saved = saveCurrentAudioScheme(context, name)
            if (saved != null) audioSchemes = readAudioSchemes(context)
            saved != null
        },
        onApplyScheme = { scheme ->
            if (applyAudioScheme(context, scheme)) {
                AudioPreviewPlayer.stop()
                BackgroundMusicPlayer.stop()
                audioSettings = scheme.settings
                viewModel.refresh()
                if (scheme.settings.masterEnabled && scheme.settings.background.enabled &&
                    !scheme.backgroundMusicUri.isNullOrBlank()
                ) {
                    BackgroundMusicPlayer.playConfigured(context)
                }
                true
            } else false
        },
        onDeleteScheme = { scheme ->
            val deleted = deleteAudioScheme(context, scheme.id)
            if (deleted) audioSchemes = readAudioSchemes(context)
            deleted
        },
    )

    SoundEffectsScreenMiuix(
        uiState = uiState,
        audioSettings = audioSettings,
        previewState = previewState,
        audioSchemes = audioSchemes,
        actions = actions,
        onBack = onBack,
    )
}

@Composable
private fun SoundEffectsScreenMiuix(
    uiState: SettingsUiState,
    audioSettings: AppAudioSettings,
    previewState: AudioPreviewState,
    audioSchemes: List<AudioScheme>,
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
            audioSettings = audioSettings,
            previewState = previewState,
            audioSchemes = audioSchemes,
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
    audioSettings: AppAudioSettings,
    previewState: AudioPreviewState,
    audioSchemes: List<AudioScheme>,
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

        AudioMasterCard(
            settings = audioSettings,
            onValueChange = actions.onSetAudioSettings,
        )

        AudioSchemesCard(
            schemes = audioSchemes,
            onSave = actions.onSaveScheme,
            onApply = actions.onApplyScheme,
            onDelete = actions.onDeleteScheme,
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
            enabled = audioSettings.startup.enabled,
            onEnabledChange = {
                actions.onSetAudioSettings(audioSettings.copy(startup = audioSettings.startup.copy(enabled = it)))
            },
            onPick = actions.onPickStartupSound,
            onPreview = actions.onPreviewStartupSound,
            onClear = actions.onClearStartupSound,
        ) {
            MediaFileInfoSummary(rememberMediaFileInfo(uiState.customStartupSoundUri))
            AudioPreviewControls(
                uriString = uiState.customStartupSoundUri,
                state = previewState,
                actions = actions,
            )
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
            AudioTrackControls(
                value = audioSettings.startup,
                mediaDurationMs = rememberMediaFileInfo(uiState.customStartupSoundUri)?.durationMillis,
                onValueChange = {
                    actions.onSetAudioSettings(audioSettings.copy(startup = it))
                },
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
            enabled = audioSettings.click.enabled,
            onEnabledChange = {
                actions.onSetAudioSettings(audioSettings.copy(click = audioSettings.click.copy(enabled = it)))
            },
            onPick = actions.onPickClickSound,
            onPreview = actions.onPreviewClickSound,
            onClear = actions.onClearClickSound,
        ) {
            MediaFileInfoSummary(rememberMediaFileInfo(uiState.customClickSoundUri))
            AudioPreviewControls(uiState.customClickSoundUri, previewState, actions)
            SoundSlider(
                title = stringResource(R.string.settings_click_sound_volume),
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                value = uiState.customClickSoundVolume,
                valueRange = MIN_CUSTOM_AUDIO_VOLUME..MAX_CUSTOM_AUDIO_VOLUME,
                valueLabel = { stringResource(R.string.settings_audio_volume_value, (it * 100).roundToInt()) },
                onValueChange = actions.onSetClickSoundVolume,
            )
            AudioTrackControls(
                value = audioSettings.click,
                mediaDurationMs = rememberMediaFileInfo(uiState.customClickSoundUri)?.durationMillis,
                onValueChange = { actions.onSetAudioSettings(audioSettings.copy(click = it)) },
                allowLoop = false,
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
            enabled = audioSettings.background.enabled,
            onEnabledChange = {
                val next = audioSettings.copy(background = audioSettings.background.copy(enabled = it))
                actions.onSetAudioSettings(next)
                if (!it) BackgroundMusicPlayer.stop()
            },
            onPick = actions.onPickBackgroundMusic,
            onPreview = actions.onPreviewBackgroundMusic,
            onClear = actions.onClearBackgroundMusic,
        ) {
            MediaFileInfoSummary(rememberMediaFileInfo(uiState.customBackgroundMusicUri))
            AudioPreviewControls(uiState.customBackgroundMusicUri, previewState, actions)
            SoundSlider(
                title = stringResource(R.string.settings_background_music_volume),
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                value = uiState.customBackgroundMusicVolume,
                valueRange = MIN_CUSTOM_AUDIO_VOLUME..MAX_CUSTOM_AUDIO_VOLUME,
                valueLabel = { stringResource(R.string.settings_audio_volume_value, (it * 100).roundToInt()) },
                onValueChange = actions.onSetBackgroundMusicVolume,
            )
            AudioTrackControls(
                value = audioSettings.background,
                mediaDurationMs = rememberMediaFileInfo(uiState.customBackgroundMusicUri)?.durationMillis,
                onValueChange = { actions.onSetAudioSettings(audioSettings.copy(background = it)) },
            )
        }
    }
}

@Composable
private fun AudioMasterCard(
    settings: AppAudioSettings,
    onValueChange: (AppAudioSettings) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AudioToggleRow(
                title = stringResource(R.string.settings_audio_master),
                summary = stringResource(R.string.settings_audio_master_summary),
                checked = settings.masterEnabled,
                onCheckedChange = { onValueChange(settings.copy(masterEnabled = it)) },
            )
            Text(
                text = stringResource(R.string.settings_audio_policy_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            AudioToggleRow(
                title = stringResource(R.string.settings_audio_respect_silent),
                checked = settings.respectSilentMode,
                onCheckedChange = { onValueChange(settings.copy(respectSilentMode = it)) },
            )
            AudioToggleRow(
                title = stringResource(R.string.settings_audio_respect_dnd),
                checked = settings.respectDoNotDisturb,
                onCheckedChange = { onValueChange(settings.copy(respectDoNotDisturb = it)) },
            )
            AudioToggleRow(
                title = stringResource(R.string.settings_audio_headset),
                checked = settings.pauseOnHeadsetDisconnect,
                onCheckedChange = { onValueChange(settings.copy(pauseOnHeadsetDisconnect = it)) },
            )
            AudioToggleRow(
                title = stringResource(R.string.settings_audio_haptic),
                checked = settings.hapticWithClick,
                onCheckedChange = { onValueChange(settings.copy(hapticWithClick = it)) },
            )
        }
    }
}

@Composable
private fun AudioSchemesCard(
    schemes: List<AudioScheme>,
    onSave: (String) -> Boolean,
    onApply: (AudioScheme) -> Boolean,
    onDelete: (AudioScheme) -> Boolean,
) {
    var name by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.settings_audio_schemes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(32) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_audio_scheme_name)) },
                )
                FilledTonalButton(
                    enabled = name.isNotBlank(),
                    onClick = { if (onSave(name)) name = "" },
                ) {
                    Text(stringResource(R.string.settings_audio_scheme_save))
                }
            }
            schemes.forEach { scheme ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(scheme.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            text = stringResource(
                                R.string.settings_audio_scheme_files,
                                listOf(
                                    scheme.startupSoundUri,
                                    scheme.clickSoundUri,
                                    scheme.backgroundMusicUri,
                                ).count { !it.isNullOrBlank() },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { onApply(scheme) }) {
                        Text(stringResource(R.string.settings_audio_scheme_apply))
                    }
                    TextButton(onClick = { onDelete(scheme) }) {
                        Text(stringResource(R.string.settings_audio_scheme_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioPreviewControls(
    uriString: String?,
    state: AudioPreviewState,
    actions: SoundEffectsActions,
) {
    if (uriString.isNullOrBlank()) return
    val active = state.uriString == uriString
    AudioWaveform(
        uriString = uriString,
        progress = if (active && state.durationMs > 0L) state.positionMs.toFloat() / state.durationMs else 0f,
    )
    if (!active) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_audio_progress),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Slider(
            value = state.positionMs.toFloat().coerceAtMost(state.durationMs.toFloat().coerceAtLeast(1f)),
            onValueChange = { actions.onSeekPreview(it.toLong()) },
            valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = if (state.playing) actions.onPausePreview else actions.onResumePreview,
            ) {
                Icon(
                    imageVector = if (state.playing) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(
                        if (state.playing) R.string.settings_audio_pause else R.string.settings_audio_resume
                    ),
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = actions.onStopPreview,
            ) {
                Icon(Icons.Rounded.StopCircle, contentDescription = null)
                Text(
                    text = stringResource(R.string.settings_audio_stop),
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun AudioTrackControls(
    value: AudioTrackSettings,
    mediaDurationMs: Long?,
    onValueChange: (AudioTrackSettings) -> Unit,
    allowLoop: Boolean = true,
) {
    val settings = value.normalized()
    val duration = mediaDurationMs?.coerceAtLeast(1L) ?: 1L
    val end = settings.trimEndMs.takeIf { it > 0L }?.coerceAtMost(duration) ?: duration
    SoundSlider(
        title = stringResource(R.string.settings_audio_trim_start),
        icon = Icons.Rounded.Timer,
        value = settings.trimStartMs.coerceAtMost(end).toFloat(),
        valueRange = 0f..duration.toFloat(),
        valueLabel = { stringResource(R.string.settings_audio_seconds_decimal, it / 1000f) },
        onValueChange = {
            onValueChange(settings.copy(trimStartMs = it.toLong().coerceAtMost(end)).normalized())
        },
    )
    SoundSlider(
        title = stringResource(R.string.settings_audio_trim_end),
        icon = Icons.Rounded.Timer,
        value = end.toFloat(),
        valueRange = 0f..duration.toFloat(),
        valueLabel = { stringResource(R.string.settings_audio_seconds_decimal, it / 1000f) },
        onValueChange = {
            onValueChange(
                settings.copy(trimEndMs = it.toLong().coerceAtLeast(settings.trimStartMs)).normalized()
            )
        },
    )
    SoundSlider(
        title = stringResource(R.string.settings_audio_fade_in),
        icon = Icons.AutoMirrored.Rounded.VolumeUp,
        value = settings.fadeInMs.toFloat(),
        valueRange = 0f..5_000f,
        valueLabel = { stringResource(R.string.settings_audio_milliseconds, it.roundToInt()) },
        onValueChange = { onValueChange(settings.copy(fadeInMs = it.roundToInt()).normalized()) },
    )
    SoundSlider(
        title = stringResource(R.string.settings_audio_fade_out),
        icon = Icons.AutoMirrored.Rounded.VolumeUp,
        value = settings.fadeOutMs.toFloat(),
        valueRange = 0f..5_000f,
        valueLabel = { stringResource(R.string.settings_audio_milliseconds, it.roundToInt()) },
        onValueChange = { onValueChange(settings.copy(fadeOutMs = it.roundToInt()).normalized()) },
    )
    AudioToggleRow(
        title = stringResource(R.string.settings_audio_normalize),
        checked = settings.normalizeVolume,
        onCheckedChange = { onValueChange(settings.copy(normalizeVolume = it)) },
    )
    if (allowLoop) {
        AudioToggleRow(
            title = stringResource(R.string.settings_audio_loop),
            checked = settings.loop,
            onCheckedChange = { onValueChange(settings.copy(loop = it)) },
        )
    }
}

@Composable
private fun AudioToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            summary?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SoundEditorCard(
    title: String,
    summary: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onPick: () -> Unit,
    onPreview: () -> Unit,
    onClear: () -> Unit,
    controls: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerHigh),
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
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                )
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
                    enabled = selected && enabled,
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
    val onSetAudioSettings: (AppAudioSettings) -> Unit,
    val onPausePreview: () -> Unit,
    val onResumePreview: () -> Unit,
    val onStopPreview: () -> Unit,
    val onSeekPreview: (Long) -> Unit,
    val onSaveScheme: (String) -> Boolean,
    val onApplyScheme: (AudioScheme) -> Boolean,
    val onDeleteScheme: (AudioScheme) -> Boolean,
)
