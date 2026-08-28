package me.weishu.kernelsu.ui.screen.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.StartupAnimationOverlay
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.util.CUSTOM_STARTUP_ANIMATION_MIME_TYPES
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.MediaFileInfo
import me.weishu.kernelsu.ui.util.StartupAnimationScaleMode
import me.weishu.kernelsu.ui.util.StartupAnimationSettings
import me.weishu.kernelsu.ui.util.StartupAnimationPreset
import me.weishu.kernelsu.ui.util.StartupSoundPlayer
import me.weishu.kernelsu.ui.util.inspectMediaFile
import me.weishu.kernelsu.ui.util.readStartupAnimationPresets
import me.weishu.kernelsu.ui.util.saveCurrentStartupAnimationPreset
import me.weishu.kernelsu.ui.util.applyStartupAnimationPreset
import me.weishu.kernelsu.ui.util.deleteStartupAnimationPreset
import me.weishu.kernelsu.ui.util.isStartupAnimationUriReferencedByPreset
import me.weishu.kernelsu.ui.util.isCustomStartupAnimationVideo
import me.weishu.kernelsu.ui.util.prepareStartupAnimationImport
import me.weishu.kernelsu.ui.util.releasePersistableStartupAnimationReadPermission
import me.weishu.kernelsu.ui.util.takePersistableStartupAnimationReadPermission
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun StartupAnimationScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showPreview = rememberSaveable { mutableStateOf(false) }
    val previewUri = rememberSaveable { mutableStateOf<String?>(null) }
    val cropOrientation = rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val animationPresets = remember { mutableStateOf(readStartupAnimationPresets(context)) }

    val startupAnimationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val imported = prepareStartupAnimationImport(context, uri).getOrElse {
                Toast.makeText(context, R.string.settings_startup_animation_invalid, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (imported.requiresPersistablePermission) {
                takePersistableStartupAnimationReadPermission(context, uri)
            }
            val uriString = imported.uri.toString()
            if (uiState.customStartupAnimationUri != uriString) {
                val previous = uiState.customStartupAnimationUri
                if (!isStartupAnimationUriReferencedByPreset(context, previous)) {
                    releasePersistableStartupAnimationReadPermission(context, previous)
                }
            }
            viewModel.setCustomStartupAnimationUri(uriString)
            previewUri.value = uriString
            showPreview.value = true
            if (imported.extractedFromMotionPhoto) {
                Toast.makeText(
                    context,
                    R.string.settings_startup_animation_motion_photo_imported,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val actions = StartupAnimationActions(
        onPick = { startupAnimationLauncher.launch(CUSTOM_STARTUP_ANIMATION_MIME_TYPES) },
        onPreview = {
            uiState.customStartupAnimationUri?.let { uriString ->
                if (uiState.startupAnimationSettings.syncStartupSound) {
                    StartupSoundPlayer.playConfigured(context)
                }
                previewUri.value = uriString
                showPreview.value = true
            }
        },
        onPortraitCrop = { cropOrientation.value = "portrait" },
        onLandscapeCrop = { cropOrientation.value = "landscape" },
        onSettingsChange = viewModel::setStartupAnimationSettings,
        onSavePreset = { name ->
            val saved = saveCurrentStartupAnimationPreset(context, name)
            if (saved != null) animationPresets.value = readStartupAnimationPresets(context)
            saved != null
        },
        onApplyPreset = { preset ->
            val applied = applyStartupAnimationPreset(context, preset)
            if (applied) viewModel.refresh()
            applied
        },
        onDeletePreset = { preset ->
            val deleted = deleteStartupAnimationPreset(context, preset.id)
            if (deleted) animationPresets.value = readStartupAnimationPresets(context)
            deleted
        },
        onClear = {
            if (!isStartupAnimationUriReferencedByPreset(context, uiState.customStartupAnimationUri)) {
                releasePersistableStartupAnimationReadPermission(context, uiState.customStartupAnimationUri)
            }
            viewModel.clearCustomStartupAnimation()
            showPreview.value = false
            previewUri.value = null
        },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        StartupAnimationScreenMiuix(
            uiState = uiState,
            presets = animationPresets.value,
            actions = actions,
            onBack = onBack,
        )

        if (showPreview.value && !previewUri.value.isNullOrBlank()) {
            StartupAnimationOverlay(
                uriString = previewUri.value,
                settings = uiState.startupAnimationSettings,
                onFinished = {
                    StartupSoundPlayer.stop()
                    showPreview.value = false
                    previewUri.value = null
                },
                onError = {
                    StartupSoundPlayer.stop()
                    showPreview.value = false
                    previewUri.value = null
                    Toast.makeText(context, R.string.settings_startup_animation_play_failed, Toast.LENGTH_SHORT).show()
                },
            )
        }

        val orientation = cropOrientation.value
        SettingsWallpaperCropDialog(
            show = orientation != null,
            uriString = uiState.customStartupAnimationUri,
            crop = if (orientation == "landscape") {
                uiState.startupAnimationSettings.landscapeCrop
            } else {
                uiState.startupAnimationSettings.portraitCrop
            },
            onCropChange = { crop ->
                val current = uiState.startupAnimationSettings
                viewModel.setStartupAnimationSettings(
                    if (orientation == "landscape") current.copy(landscapeCrop = crop)
                    else current.copy(portraitCrop = crop)
                )
            },
            onDismissRequest = { cropOrientation.value = null },
            editorAspectRatio = if (orientation == "landscape") 16f / 9f else 9f / 16f,
            cropAspectRatio = if (orientation == "landscape") 16f / 9f else 9f / 16f,
        )
    }
}

@Composable
private fun StartupAnimationScreenMiuix(
    uiState: SettingsUiState,
    presets: List<StartupAnimationPreset>,
    actions: StartupAnimationActions,
    onBack: () -> Unit,
) {
    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.settings_startup_animation),
                color = Color.Transparent,
                titleColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface,
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        StartupAnimationContent(
            uiState = uiState,
            presets = presets,
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
private fun StartupAnimationContent(
    uiState: SettingsUiState,
    presets: List<StartupAnimationPreset>,
    actions: StartupAnimationActions,
    modifier: Modifier,
) {
    val selected = !uiState.customStartupAnimationUri.isNullOrBlank()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_startup_animation_page_summary),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        StartupAnimationStatusCard(
            selected = selected,
            uriString = uiState.customStartupAnimationUri,
        )

        StartupAnimationActionCard(
            selected = selected,
            onPick = actions.onPick,
            onPreview = actions.onPreview,
            onClear = actions.onClear,
        )

        StartupAnimationSettingsCard(
            selected = selected,
            settings = uiState.startupAnimationSettings,
            onSettingsChange = actions.onSettingsChange,
            onPortraitCrop = actions.onPortraitCrop,
            onLandscapeCrop = actions.onLandscapeCrop,
        )

        StartupAnimationPresetCard(
            selected = selected,
            presets = presets,
            onSave = actions.onSavePreset,
            onApply = actions.onApplyPreset,
            onDelete = actions.onDeletePreset,
        )

        StartupAnimationHintCard()
    }
}

@Composable
private fun StartupAnimationPresetCard(
    selected: Boolean,
    presets: List<StartupAnimationPreset>,
    onSave: (String) -> Boolean,
    onApply: (StartupAnimationPreset) -> Boolean,
    onDelete: (StartupAnimationPreset) -> Boolean,
) {
    var name by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.settings_startup_animation_library),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(32) },
                    modifier = Modifier.weight(1f),
                    enabled = selected,
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_startup_animation_preset_name)) },
                )
                FilledTonalButton(
                    enabled = selected && name.isNotBlank(),
                    onClick = { if (onSave(name)) name = "" },
                ) {
                    Text(stringResource(R.string.settings_audio_scheme_save))
                }
            }
            presets.forEach { preset ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(preset.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { onApply(preset) }) {
                        Text(stringResource(R.string.settings_audio_scheme_apply))
                    }
                    TextButton(onClick = { onDelete(preset) }) {
                        Text(stringResource(R.string.settings_audio_scheme_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun StartupAnimationStatusCard(
    selected: Boolean,
    uriString: String?,
) {
    val mediaInfo = rememberMediaFileInfo(uriString)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_startup_animation_status_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        if (selected) {
                            R.string.settings_startup_animation_status_selected
                        } else {
                            R.string.settings_startup_animation_status_empty
                        }
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (selected) {
                    MediaFileInfoSummary(info = mediaInfo)
                }
            }
        }
    }
}

@Composable
private fun StartupAnimationSettingsCard(
    selected: Boolean,
    settings: StartupAnimationSettings,
    onSettingsChange: (StartupAnimationSettings) -> Unit,
    onPortraitCrop: () -> Unit,
    onLandscapeCrop: () -> Unit,
) {
    val value = settings.normalized()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_startup_animation_display),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StartupAnimationScaleMode.entries.forEach { mode ->
                    FilterChip(
                        selected = value.scaleMode == mode,
                        onClick = { onSettingsChange(value.copy(scaleMode = mode)) },
                        label = {
                            Text(
                                stringResource(
                                    when (mode) {
                                        StartupAnimationScaleMode.Fit -> R.string.settings_startup_animation_fit
                                        StartupAnimationScaleMode.Fill -> R.string.settings_startup_animation_fill
                                        StartupAnimationScaleMode.Crop -> R.string.settings_startup_animation_crop
                                    }
                                )
                            )
                        },
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_startup_animation_background),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    0xFF000000L to R.string.settings_startup_animation_background_black,
                    0xFF202124L to R.string.settings_startup_animation_background_charcoal,
                    0xFFFFFFFFL to R.string.settings_startup_animation_background_light,
                ).forEach { (argb, label) ->
                    FilterChip(
                        selected = value.backgroundArgb == argb,
                        onClick = { onSettingsChange(value.copy(backgroundArgb = argb)) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            MediaEditorSlider(
                title = stringResource(R.string.media_editor_brightness),
                value = value.brightness,
                valueRange = 0.35f..1.35f,
                valueLabel = { "${(it * 100).roundToInt()}%" },
                onValueChange = { onSettingsChange(value.copy(brightness = it)) },
            )
            MediaEditorSlider(
                title = stringResource(R.string.settings_startup_animation_speed),
                value = value.playbackSpeed,
                valueRange = 0.5f..2f,
                valueLabel = { "%.2fx".format(it) },
                onValueChange = { onSettingsChange(value.copy(playbackSpeed = it)) },
            )
            MediaEditorSlider(
                title = stringResource(R.string.settings_startup_animation_duration),
                value = value.durationMillis.toFloat(),
                valueRange = 500f..10_000f,
                valueLabel = { "%.1fs".format(it / 1_000f) },
                onValueChange = { onSettingsChange(value.copy(durationMillis = it.toLong())) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), enabled = selected, onClick = onPortraitCrop) {
                    Text(stringResource(R.string.settings_startup_animation_portrait_crop))
                }
                OutlinedButton(modifier = Modifier.weight(1f), enabled = selected, onClick = onLandscapeCrop) {
                    Text(stringResource(R.string.settings_startup_animation_landscape_crop))
                }
            }
            StartupAnimationSwitchRow(
                title = stringResource(R.string.settings_startup_animation_tap_skip),
                checked = value.allowTapSkip,
                onCheckedChange = { onSettingsChange(value.copy(allowTapSkip = it)) },
            )
            StartupAnimationSwitchRow(
                title = stringResource(R.string.settings_startup_animation_swipe_skip),
                checked = value.allowSwipeSkip,
                onCheckedChange = { onSettingsChange(value.copy(allowSwipeSkip = it)) },
            )
            StartupAnimationSwitchRow(
                title = stringResource(R.string.settings_startup_animation_audio_sync),
                checked = value.syncStartupSound,
                onCheckedChange = { onSettingsChange(value.copy(syncStartupSound = it)) },
            )
        }
    }
}

@Composable
private fun StartupAnimationSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StartupAnimationActionCard(
    selected: Boolean,
    onPick: () -> Unit,
    onPreview: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StartupAnimationCardTitle(
                icon = Icons.Rounded.PlayCircle,
                title = stringResource(R.string.settings_startup_animation),
                summary = stringResource(
                    if (selected) {
                        R.string.settings_startup_animation_selected_summary
                    } else {
                        R.string.settings_startup_animation_summary
                    }
                ),
            )

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
                        text = stringResource(R.string.settings_startup_animation_choose),
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
        }
    }
}

@Composable
private fun StartupAnimationHintCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
    ) {
        StartupAnimationCardTitle(
            modifier = Modifier.padding(14.dp),
            icon = Icons.Rounded.Info,
            title = stringResource(R.string.settings_startup_animation_hint_title),
            summary = stringResource(R.string.settings_startup_animation_hint),
        )
    }
}

@Composable
private fun StartupAnimationCardTitle(
    icon: ImageVector,
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
}

private data class StartupAnimationActions(
    val onPick: () -> Unit,
    val onPreview: () -> Unit,
    val onPortraitCrop: () -> Unit,
    val onLandscapeCrop: () -> Unit,
    val onSettingsChange: (StartupAnimationSettings) -> Unit,
    val onSavePreset: (String) -> Boolean,
    val onApplyPreset: (StartupAnimationPreset) -> Boolean,
    val onDeletePreset: (StartupAnimationPreset) -> Boolean,
    val onClear: () -> Unit,
)
