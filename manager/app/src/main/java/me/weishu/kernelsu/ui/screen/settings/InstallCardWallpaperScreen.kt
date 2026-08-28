package me.weishu.kernelsu.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.rememberCustomImageBitmap
import me.weishu.kernelsu.ui.component.rememberCustomVideoFrameBitmap
import me.weishu.kernelsu.ui.component.MediaVisualLayer
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.screen.install.InstallCardWallpaperBackground
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.MediaVariantSettings
import me.weishu.kernelsu.ui.util.ThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.ThemeStoreImageState
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.generateResponsiveCrops
import me.weishu.kernelsu.ui.util.inspectMediaFile
import me.weishu.kernelsu.ui.util.rememberThemeStoreImageState
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperCrop
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotCrop
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotNightCrop
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotNightMedia
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotResponsiveCrops
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotVariantSettings
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotVisualSettings
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotVideo
import me.weishu.kernelsu.ui.util.takePersistableImageReadPermission
import me.weishu.kernelsu.ui.util.takePersistableVideoBackgroundReadPermission
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

private data class InstallCardWallpaperSpec(
    val slot: ThemeStoreImageSlot,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    val icon: ImageVector,
    val previewAspectRatio: Float,
)

private val installCardWallpaperSpecs = listOf(
    InstallCardWallpaperSpec(
        slot = ThemeStoreImageSlot.InstallImage,
        titleRes = R.string.install_card_wallpaper_image_title,
        summaryRes = R.string.install_card_wallpaper_image_summary,
        icon = Icons.Rounded.AutoFixHigh,
        previewAspectRatio = 1.62f,
    ),
    InstallCardWallpaperSpec(
        slot = ThemeStoreImageSlot.InstallMethods,
        titleRes = R.string.install_card_wallpaper_methods_title,
        summaryRes = R.string.install_card_wallpaper_methods_summary,
        icon = Icons.AutoMirrored.Rounded.ListAlt,
        previewAspectRatio = 1.48f,
    ),
    InstallCardWallpaperSpec(
        slot = ThemeStoreImageSlot.InstallOptions,
        titleRes = R.string.install_card_wallpaper_options_title,
        summaryRes = R.string.install_card_wallpaper_options_summary,
        icon = Icons.Rounded.Tune,
        previewAspectRatio = 1.36f,
    ),
)

@Composable
fun InstallCardWallpaperScreen() {
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }

    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.install_card_wallpapers),
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
        InstallCardWallpaperContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun InstallCardWallpaperContent(modifier: Modifier) {
    var cropTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var previewTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var showPagePreview by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.install_card_wallpapers_summary),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showPagePreview = true },
        ) {
            Icon(Icons.Rounded.Visibility, contentDescription = null)
            Text(
                text = stringResource(R.string.install_card_wallpaper_screen_preview),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 720.dp) 2 else 1
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                installCardWallpaperSpecs.chunked(columns).forEach { specs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        specs.forEach { spec ->
                            InstallCardWallpaperItem(
                                spec = spec,
                                showCrop = cropTarget == spec.slot.id,
                                onShowCropChange = { show -> cropTarget = spec.slot.id.takeIf { show } },
                                showPreview = previewTarget == spec.slot.id,
                                onShowPreviewChange = { show -> previewTarget = spec.slot.id.takeIf { show } },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - specs.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
    }
    InstallCardWallpaperScreenPreviewDialog(
        show = showPagePreview,
        onDismissRequest = { showPagePreview = false },
    )
}

@Composable
private fun InstallCardWallpaperItem(
    spec: InstallCardWallpaperSpec,
    showCrop: Boolean,
    onShowCropChange: (Boolean) -> Unit,
    showPreview: Boolean,
    onShowPreviewChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storedState = rememberThemeStoreImageState(spec.slot)
    var editNight by rememberSaveable(spec.slot.id) { mutableStateOf(false) }
    var showAdjustments by rememberSaveable(spec.slot.id) { mutableStateOf(false) }
    val state = remember(storedState, editNight, spec.previewAspectRatio) {
        if (editNight) {
            storedState.copy(
                uriString = storedState.nightUriString,
                videoUriString = storedState.nightVideoUriString,
                crop = storedState.nightResponsiveCrops.forAspectRatio(spec.previewAspectRatio),
                visualSettings = storedState.nightVisualSettings,
                responsiveCrops = storedState.nightResponsiveCrops,
                nightUriString = null,
                nightVideoUriString = null,
                variantSettings = MediaVariantSettings(),
            )
        } else {
            storedState.copy(
                crop = storedState.responsiveCrops.forAspectRatio(spec.previewAspectRatio),
                nightUriString = null,
                nightVideoUriString = null,
                variantSettings = MediaVariantSettings(),
            )
        }
    }
    val imageBitmap = rememberCustomImageBitmap(
        uriString = state.uriString,
        crop = state.crop,
    )
    val videoFrameBitmap = rememberCustomVideoFrameBitmap(state.videoUriString)
    val title = stringResource(spec.titleRes)
    val mediaInfo = rememberMediaFileInfo(state.uriString ?: state.videoUriString)

    fun saveResponsiveCrops(uriString: String) {
        scope.launch {
            val info = inspectMediaFile(context, uriString) ?: return@launch
            val width = info.width ?: return@launch
            val height = info.height ?: return@launch
            setThemeStoreImageSlotResponsiveCrops(
                context,
                spec.slot,
                generateResponsiveCrops(width, height),
                night = editNight,
            )
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val storageKey = if (editNight) spec.slot.nightUriKey else spec.slot.uriKey
        val storedUri = persistCustomImageReference(context, uri, storageKey)
            ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
        if (editNight) {
            setThemeStoreImageSlotNightMedia(context, spec.slot, storedUri, video = false)
        } else {
            setThemeStoreImageSlot(context, spec.slot, storedUri)
        }
        saveResponsiveCrops(storedUri)
        onShowCropChange(true)
    }
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        takePersistableVideoBackgroundReadPermission(context, uri)
        if (editNight) {
            setThemeStoreImageSlotNightMedia(context, spec.slot, uri.toString(), video = true)
        } else {
            setThemeStoreImageSlotVideo(context, spec.slot, uri.toString())
        }
        saveResponsiveCrops(uri.toString())
        onShowCropChange(true)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(installCardWallpaperStatusRes(state)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.hasSelected) {
                    InstallCardWallpaperMenu(
                        onCrop = { onShowCropChange(true) },
                        onPreview = { onShowPreviewChange(true) },
                        onClear = {
                            if (editNight) {
                                setThemeStoreImageSlotNightMedia(context, spec.slot, null, video = false)
                            } else {
                                setThemeStoreImageSlot(context, spec.slot, null)
                            }
                        },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !editNight,
                    onClick = { editNight = false },
                    label = { Text(stringResource(R.string.media_variant_day)) },
                )
                FilterChip(
                    selected = editNight,
                    onClick = { editNight = true },
                    label = { Text(stringResource(R.string.media_variant_night)) },
                )
            }

            InstallCardWallpaperFrame(
                spec = spec,
                state = state,
                imageBitmap = imageBitmap,
                videoFrameBitmap = videoFrameBitmap,
            )
            MediaFileInfoSummary(mediaInfo)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = { imageLauncher.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.home_card_wallpaper_pick_image),
                        modifier = Modifier.padding(start = 7.dp),
                        maxLines = 1,
                    )
                }
                FilledTonalButton(
                    onClick = { videoLauncher.launch(arrayOf("video/*")) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.home_card_wallpaper_pick_video),
                        modifier = Modifier.padding(start = 7.dp),
                        maxLines = 1,
                    )
                }
            }
            if (state.hasSelected) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showAdjustments = !showAdjustments },
                ) {
                    Text(
                        stringResource(
                            if (showAdjustments) R.string.media_editor_hide_adjustments
                            else R.string.media_editor_show_adjustments
                        )
                    )
                }
                AnimatedVisibility(visible = showAdjustments) {
                    MediaVisualControls(
                        value = state.visualSettings,
                        onValueChange = {
                            setThemeStoreImageSlotVisualSettings(context, spec.slot, it, night = editNight)
                        },
                        showContrast = true,
                        showMotion = true,
                    )
                }
            }
            MediaVariantControls(
                value = storedState.variantSettings,
                onValueChange = { setThemeStoreImageSlotVariantSettings(context, spec.slot, it) },
            )
        }
    }

    SettingsWallpaperCropDialog(
        show = showCrop && state.hasSelected,
        uriString = state.uriString ?: state.videoUriString,
        crop = state.crop,
        onCropChange = { crop ->
            val safeCrop = sanitizeCustomWallpaperCrop(crop)
            if (editNight) {
                setThemeStoreImageSlotNightCrop(context, spec.slot, safeCrop)
            } else {
                setThemeStoreImageSlotCrop(context, spec.slot, safeCrop)
            }
            setThemeStoreImageSlotResponsiveCrops(
                context = context,
                slot = spec.slot,
                crops = state.responsiveCrops.withCropForAspectRatio(spec.previewAspectRatio, safeCrop),
                night = editNight,
            )
            onShowPreviewChange(true)
        },
        onDismissRequest = { onShowCropChange(false) },
        title = stringResource(R.string.install_card_wallpaper_crop_title, title),
        editorAspectRatio = spec.previewAspectRatio,
        cropAspectRatio = spec.previewAspectRatio,
        defaultCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
        previewBitmap = videoFrameBitmap.takeIf { state.hasVideoSelected },
        transform = state.visualSettings.transform,
        onTransformChange = { transform ->
            setThemeStoreImageSlotVisualSettings(
                context,
                spec.slot,
                state.visualSettings.copy(transform = transform),
                night = editNight,
            )
        },
        onGenerateResponsiveCrops = {
            setThemeStoreImageSlotResponsiveCrops(context, spec.slot, it, night = editNight)
        },
    )
    InstallCardWallpaperPreviewDialog(
        show = showPreview && state.hasSelected,
        title = title,
        spec = spec,
        state = state,
        onDismissRequest = { onShowPreviewChange(false) },
    )
}

@Composable
private fun InstallCardWallpaperFrame(
    spec: InstallCardWallpaperSpec,
    state: ThemeStoreImageState,
    imageBitmap: ImageBitmap?,
    videoFrameBitmap: ImageBitmap?,
) {
    val frameBitmap = imageBitmap ?: videoFrameBitmap
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(spec.previewAspectRatio)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            frameBitmap != null -> MediaVisualLayer(
                settings = state.visualSettings,
                modifier = Modifier.fillMaxSize(),
            ) { colorFilter ->
                Image(
                    bitmap = frameBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            state.hasSelected -> CircularProgressIndicator()
            else -> Text(
                text = stringResource(R.string.home_card_wallpaper_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (frameBitmap != null) {
            InstallCardWallpaperSample(spec, Color.White)
            InstallCardWallpaperSafeArea()
        }
    }
}

@Composable
private fun InstallCardWallpaperSafeArea() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.24f)
                .background(Color.Black.copy(alpha = 0.12f)),
        )
        Spacer(Modifier.weight(0.50f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.26f)
                .background(Color.Black.copy(alpha = 0.12f)),
        )
    }
}

@Composable
private fun InstallCardWallpaperSample(
    spec: InstallCardWallpaperSpec,
    contentColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            text = stringResource(spec.titleRes),
            color = contentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == 2) 0.68f else 1f)
                    .background(contentColor.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .padding(vertical = 7.dp, horizontal = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (index == 0) 0.72f else 0.52f)
                        .background(contentColor.copy(alpha = 0.78f), RoundedCornerShape(3.dp))
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun InstallCardWallpaperMenu(
    onCrop: () -> Unit,
    onPreview: () -> Unit,
    onClear: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.ai_chat_more_options),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_card_wallpaper_crop_action)) },
                leadingIcon = { Icon(Icons.Rounded.Crop, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCrop()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_card_wallpaper_preview_action)) },
                leadingIcon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPreview()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_card_wallpaper_clear_action)) },
                leadingIcon = { Icon(Icons.Rounded.Close, contentDescription = null) },
                onClick = {
                    expanded = false
                    onClear()
                },
            )
        }
    }
}

@Composable
private fun InstallCardWallpaperPreviewDialog(
    show: Boolean,
    title: String,
    spec: InstallCardWallpaperSpec,
    state: ThemeStoreImageState,
    onDismissRequest: () -> Unit,
) {
    if (!show) return

    OverlayDialog(
        show = true,
        title = stringResource(R.string.install_card_wallpaper_preview_title, title),
        onDismissRequest = onDismissRequest,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(spec.previewAspectRatio)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    InstallCardWallpaperBackground(state)
                    InstallCardWallpaperSample(
                        spec = spec,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
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
private fun InstallCardWallpaperScreenPreviewDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (!show) return

    OverlayDialog(
        show = true,
        title = stringResource(R.string.install_card_wallpaper_screen_preview),
        onDismissRequest = onDismissRequest,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState())
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
                            RoundedCornerShape(18.dp),
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.install),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    installCardWallpaperSpecs.forEach { spec ->
                        InstallCardWallpaperMappedPreview(spec)
                    }
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {},
                    ) {
                        Text(stringResource(R.string.install_next))
                    }
                    Text(
                        text = stringResource(R.string.install_card_wallpaper_language_check_summary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
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
private fun InstallCardWallpaperMappedPreview(spec: InstallCardWallpaperSpec) {
    val state = rememberThemeStoreImageState(spec.slot)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(spec.previewAspectRatio)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        InstallCardWallpaperBackground(state)
        InstallCardWallpaperSample(
            spec = spec,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun installCardWallpaperStatusRes(state: ThemeStoreImageState): Int {
    return when {
        state.hasVideoSelected -> R.string.home_card_wallpaper_video_selected
        state.hasImageSelected -> R.string.home_card_wallpaper_image_selected
        else -> R.string.home_card_wallpaper_empty
    }
}
