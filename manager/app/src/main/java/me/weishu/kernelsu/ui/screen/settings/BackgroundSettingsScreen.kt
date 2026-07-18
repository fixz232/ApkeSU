package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.rememberCustomVideoFrameBitmap
import me.weishu.kernelsu.ui.component.rememberCustomWallpaperPreviewBitmap
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_URI_KEY
import me.weishu.kernelsu.ui.util.CustomBackgroundState
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.takePersistableImageReadPermission
import me.weishu.kernelsu.ui.util.takePersistableVideoBackgroundReadPermission
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

private const val GLOBAL_BACKGROUND_SCOPE_KEY = "global"
private const val BACKGROUND_PREVIEW_ASPECT_RATIO = 16f / 9f

@Composable
fun BackgroundSettingsScreen() {
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingImageScopeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingVideoScopeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var cropScopeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var previewScopeKey by rememberSaveable { mutableStateOf<String?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val scopeKey = pendingImageScopeKey ?: return@rememberLauncherForActivityResult
        pendingImageScopeKey = null
        uri ?: return@rememberLauncherForActivityResult
        val storageKey = wallpaperStorageKey(scopeKey) ?: return@rememberLauncherForActivityResult
        val uriString = persistCustomImageReference(context, uri, storageKey)
            ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
        if (scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY) {
            viewModel.setCustomWallpaperUri(uriString)
        } else {
            val target = pageTargetFromScopeKey(scopeKey) ?: return@rememberLauncherForActivityResult
            viewModel.setCustomPageBackgroundWallpaper(target, uriString)
        }
        cropScopeKey = scopeKey
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val scopeKey = pendingVideoScopeKey ?: return@rememberLauncherForActivityResult
        pendingVideoScopeKey = null
        uri ?: return@rememberLauncherForActivityResult
        takePersistableVideoBackgroundReadPermission(context, uri)
        if (scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY) {
            viewModel.setCustomVideoBackgroundUri(uri.toString())
        } else {
            val target = pageTargetFromScopeKey(scopeKey) ?: return@rememberLauncherForActivityResult
            viewModel.setCustomPageBackgroundVideo(target, uri.toString())
        }
        previewScopeKey = scopeKey
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val actions = remember(viewModel) {
        BackgroundSettingsActions(
            onPickImage = { scopeKey ->
                pendingImageScopeKey = scopeKey
                imageLauncher.launch(arrayOf("image/*"))
            },
            onPickVideo = { scopeKey ->
                pendingVideoScopeKey = scopeKey
                videoLauncher.launch(arrayOf("video/*"))
            },
            onCrop = { scopeKey -> cropScopeKey = scopeKey },
            onPreview = { scopeKey -> previewScopeKey = scopeKey },
            onClear = { scopeKey ->
                if (scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY) {
                    viewModel.clearCustomWallpaper()
                    viewModel.clearCustomVideoBackground()
                } else {
                    pageTargetFromScopeKey(scopeKey)?.let(viewModel::clearCustomPageBackground)
                }
            },
            onOpacityChange = { scopeKey, opacity ->
                if (scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY) {
                    viewModel.setCustomWallpaperOpacity(opacity)
                } else {
                    pageTargetFromScopeKey(scopeKey)?.let { target ->
                        viewModel.setCustomPageBackgroundOpacity(target, opacity)
                    }
                }
            },
            onDurationChange = { scopeKey, seconds ->
                if (scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY) {
                    viewModel.setCustomVideoBackgroundDurationSeconds(seconds)
                } else {
                    pageTargetFromScopeKey(scopeKey)?.let { target ->
                        viewModel.setCustomPageBackgroundVideoDurationSeconds(target, seconds)
                    }
                }
            },
            onCropChange = { scopeKey, crop ->
                if (scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY) {
                    viewModel.setCustomWallpaperCrop(crop)
                } else {
                    pageTargetFromScopeKey(scopeKey)?.let { target ->
                        viewModel.setCustomPageBackgroundCrop(target, crop)
                    }
                }
                previewScopeKey = scopeKey
            },
            onPassthroughEnabledChange = viewModel::setCustomWallpaperPassthroughEnabled,
            onPassthroughOpacityChange = viewModel::setCustomWallpaperPassthroughOpacity,
        )
    }

    BackgroundSettingsScreenMiuix(
        uiState = uiState,
        actions = actions,
        onBack = onBack,
    )

    val cropState = cropScopeKey?.let(uiState::backgroundStateForScope)
    SettingsWallpaperCropDialog(
        show = cropState?.hasWallpaper == true,
        uriString = cropState?.wallpaperUriString,
        crop = cropState?.crop ?: CustomBackgroundState().crop,
        onCropChange = { crop ->
            cropScopeKey?.let { actions.onCropChange(it, crop) }
        },
        onDismissRequest = { cropScopeKey = null },
    )

    val previewState = previewScopeKey?.let(uiState::backgroundStateForScope)
    SettingsWallpaperPreviewDialog(
        show = previewState?.hasWallpaper == true,
        uriString = previewState?.wallpaperUriString,
        opacity = previewState?.opacity ?: CustomBackgroundState().opacity,
        crop = previewState?.crop ?: CustomBackgroundState().crop,
        passthroughEnabled = uiState.customWallpaperPassthroughEnabled,
        passthroughOpacity = uiState.customWallpaperPassthroughOpacity,
        onDismissRequest = { previewScopeKey = null },
    )
    SettingsVideoBackgroundPreviewDialog(
        show = previewState?.hasVideo == true,
        uriString = previewState?.videoUriString,
        durationSeconds = previewState?.videoDurationSeconds ?: CustomBackgroundState().videoDurationSeconds,
        opacity = previewState?.opacity ?: CustomBackgroundState().opacity,
        passthroughEnabled = uiState.customWallpaperPassthroughEnabled,
        passthroughOpacity = uiState.customWallpaperPassthroughOpacity,
        onDismissRequest = { previewScopeKey = null },
    )
}

@Composable
private fun BackgroundSettingsScreenMiuix(
    uiState: SettingsUiState,
    actions: BackgroundSettingsActions,
    onBack: () -> Unit,
) {
    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.settings_backgrounds),
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
        BackgroundSettingsContent(
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
private fun BackgroundSettingsContent(
    uiState: SettingsUiState,
    actions: BackgroundSettingsActions,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_backgrounds_summary),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        BackgroundSectionTitle(
            title = stringResource(R.string.settings_global_background),
            summary = stringResource(R.string.settings_global_background_summary),
        )
        BackgroundEditorCard(
            scopeKey = GLOBAL_BACKGROUND_SCOPE_KEY,
            title = stringResource(R.string.settings_global_background),
            summary = stringResource(R.string.settings_page_background_global_hint),
            emptyText = stringResource(R.string.settings_no_custom_background),
            state = uiState.backgroundStateForScope(GLOBAL_BACKGROUND_SCOPE_KEY),
            actions = actions,
        )

        BackgroundSectionTitle(
            title = stringResource(R.string.settings_page_backgrounds),
            summary = stringResource(R.string.settings_page_backgrounds_summary),
        )
        CustomPageBackgroundTarget.entries.forEach { target ->
            val scopeKey = pageScopeKey(target)
            BackgroundEditorCard(
                scopeKey = scopeKey,
                title = stringResource(target.titleRes),
                summary = stringResource(R.string.settings_page_background_override_hint),
                emptyText = stringResource(R.string.settings_page_background_empty),
                state = uiState.customPageBackgrounds[target],
                actions = actions,
            )
        }

        SharedBackgroundOptions(uiState = uiState, actions = actions)
        Spacer(modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun BackgroundSectionTitle(
    title: String,
    summary: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

@Composable
private fun BackgroundEditorCard(
    scopeKey: String,
    title: String,
    summary: String,
    emptyText: String,
    state: CustomBackgroundState,
    actions: BackgroundSettingsActions,
) {
    val imageBitmap = rememberCustomWallpaperPreviewBitmap(state.wallpaperUriString, state.crop)
    val videoFrameBitmap = rememberCustomVideoFrameBitmap(state.videoUriString)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (state.hasMedia) backgroundSelectionSummary(state) else summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            BackgroundPreviewFrame(
                title = title,
                emptyText = emptyText,
                imageBitmap = imageBitmap,
                videoFrameBitmap = videoFrameBitmap,
                hasMedia = state.hasMedia,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BackgroundActionButton(
                    text = stringResource(R.string.settings_page_background_pick_image),
                    icon = Icons.Rounded.PhotoLibrary,
                    onClick = { actions.onPickImage(scopeKey) },
                    modifier = Modifier.weight(1f),
                )
                BackgroundActionButton(
                    text = stringResource(R.string.settings_page_background_pick_video),
                    icon = Icons.Rounded.Videocam,
                    onClick = { actions.onPickVideo(scopeKey) },
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.hasMedia) {
                if (state.hasWallpaper) {
                    BackgroundSlider(
                        title = stringResource(R.string.settings_background_opacity),
                        value = state.opacity,
                        valueRange = MIN_CUSTOM_WALLPAPER_OPACITY..MAX_CUSTOM_WALLPAPER_OPACITY,
                        valueLabel = { "${(it * 100).roundToInt()}%" },
                        onValueChange = { actions.onOpacityChange(scopeKey, it) },
                    )
                } else {
                    BackgroundSlider(
                        title = stringResource(R.string.settings_background_opacity),
                        value = state.opacity,
                        valueRange = MIN_CUSTOM_WALLPAPER_OPACITY..MAX_CUSTOM_WALLPAPER_OPACITY,
                        valueLabel = { "${(it * 100).roundToInt()}%" },
                        onValueChange = { actions.onOpacityChange(scopeKey, it) },
                    )
                    BackgroundSlider(
                        title = stringResource(R.string.settings_video_background_duration),
                        value = state.videoDurationSeconds.toFloat(),
                        valueRange = MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS.toFloat()..
                            MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS.toFloat(),
                        valueLabel = { stringResource(R.string.settings_video_background_duration_value, it.roundToInt()) },
                        onValueChange = { actions.onDurationChange(scopeKey, it.roundToInt()) },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (state.hasWallpaper) {
                        BackgroundTextButton(
                            text = stringResource(R.string.settings_page_background_crop_action),
                            icon = Icons.Rounded.ImageSearch,
                            onClick = { actions.onCrop(scopeKey) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    BackgroundTextButton(
                        text = stringResource(R.string.settings_page_background_preview_action),
                        icon = Icons.Rounded.Visibility,
                        onClick = { actions.onPreview(scopeKey) },
                        modifier = Modifier.weight(1f),
                    )
                    BackgroundTextButton(
                        text = stringResource(R.string.settings_page_background_clear_action),
                        icon = Icons.Rounded.Close,
                        onClick = { actions.onClear(scopeKey) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundPreviewFrame(
    title: String,
    emptyText: String,
    imageBitmap: ImageBitmap?,
    videoFrameBitmap: ImageBitmap?,
    hasMedia: Boolean,
) {
    val bitmap = imageBitmap ?: videoFrameBitmap
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(BACKGROUND_PREVIEW_ASPECT_RATIO)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )

            hasMedia -> CircularProgressIndicator()

            else -> Text(
                modifier = Modifier.padding(20.dp),
                text = emptyText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.36f))
            )
            Text(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BackgroundActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BackgroundTextButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BackgroundSlider(
    title: String,
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

@Composable
private fun SharedBackgroundOptions(
    uiState: SettingsUiState,
    actions: BackgroundSettingsActions,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_shared_background_options),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.settings_wallpaper_passthrough_summary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val switchStyle = LocalSwitchStyle.current
                if (switchStyle == SwitchStyle.Original) {
                    Switch(
                        checked = uiState.customWallpaperPassthroughEnabled,
                        onCheckedChange = actions.onPassthroughEnabledChange,
                    )
                } else {
                    StyledSwitch(
                        checked = uiState.customWallpaperPassthroughEnabled,
                        onCheckedChange = actions.onPassthroughEnabledChange,
                        style = switchStyle,
                    )
                }
            }

            if (uiState.customWallpaperPassthroughEnabled) {
                BackgroundSlider(
                    title = stringResource(R.string.settings_wallpaper_passthrough_opacity),
                    value = uiState.customWallpaperPassthroughOpacity,
                    valueRange = MIN_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY..MAX_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY,
                    valueLabel = { "${(it * 100).roundToInt()}%" },
                    onValueChange = actions.onPassthroughOpacityChange,
                )
            }
        }
    }
}

private data class BackgroundSettingsActions(
    val onPickImage: (String) -> Unit,
    val onPickVideo: (String) -> Unit,
    val onCrop: (String) -> Unit,
    val onPreview: (String) -> Unit,
    val onClear: (String) -> Unit,
    val onOpacityChange: (String, Float) -> Unit,
    val onDurationChange: (String, Int) -> Unit,
    val onCropChange: (String, me.weishu.kernelsu.ui.util.CustomWallpaperCrop) -> Unit,
    val onPassthroughEnabledChange: (Boolean) -> Unit,
    val onPassthroughOpacityChange: (Float) -> Unit,
)

private fun SettingsUiState.backgroundStateForScope(scopeKey: String): CustomBackgroundState {
    if (scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY) {
        return CustomBackgroundState(
            wallpaperUriString = customWallpaperUri,
            videoUriString = customVideoBackgroundUri,
            opacity = customWallpaperOpacity,
            crop = customWallpaperCrop,
            videoDurationSeconds = customVideoBackgroundDurationSeconds,
        )
    }
    val target = pageTargetFromScopeKey(scopeKey) ?: return CustomBackgroundState()
    return customPageBackgrounds[target]
}

@Composable
private fun backgroundSelectionSummary(state: CustomBackgroundState): String {
    return stringResource(
        when {
            state.hasVideo -> R.string.settings_video_background_selected_summary
            state.hasWallpaper -> R.string.settings_wallpaper_selected_summary
            else -> R.string.settings_background_summary
        }
    )
}

private fun pageScopeKey(target: CustomPageBackgroundTarget): String {
    return "page:${target.id}"
}

private fun pageTargetFromScopeKey(scopeKey: String): CustomPageBackgroundTarget? {
    return CustomPageBackgroundTarget.entries.firstOrNull { scopeKey == pageScopeKey(it) }
}

private fun wallpaperStorageKey(scopeKey: String): String? {
    if (scopeKey == GLOBAL_BACKGROUND_SCOPE_KEY) return CUSTOM_WALLPAPER_URI_KEY
    return pageTargetFromScopeKey(scopeKey)?.wallpaperUriKey
}
