package me.weishu.kernelsu.ui.screen.themestore

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.StartupAnimationOverlay
import me.weishu.kernelsu.ui.component.CustomVideoBackground
import me.weishu.kernelsu.ui.component.rememberCustomVideoFrameBitmap
import me.weishu.kernelsu.ui.component.rememberCustomImageBitmap
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproScreen
import me.weishu.kernelsu.ui.component.skrootpro.skrootproSp
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.screen.settings.SettingsWallpaperCropDialog
import me.weishu.kernelsu.ui.screen.settings.SettingsWallpaperPreviewDialog
import me.weishu.kernelsu.ui.screen.settings.SettingsVideoBackgroundPreviewDialog
import me.weishu.kernelsu.ui.util.CUSTOM_STARTUP_ANIMATION_MIME_TYPES
import me.weishu.kernelsu.ui.util.CUSTOM_WALLPAPER_URI_KEY
import me.weishu.kernelsu.ui.util.CustomPageBackgroundTarget
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.CustomNavigationIconState
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_NAVIGATION_ICON_CROP
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.StartupSoundPlayer
import me.weishu.kernelsu.ui.util.THEME_STORE_FILE_EXTENSION
import me.weishu.kernelsu.ui.util.THEME_STORE_FILE_MIME_TYPE
import me.weishu.kernelsu.ui.util.ThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.ThemeStoreImageState
import me.weishu.kernelsu.ui.util.ThemeStoreSummary
import me.weishu.kernelsu.ui.util.exportThemeStorePackage
import me.weishu.kernelsu.ui.util.importThemeStorePackage
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.readThemeStoreSummary
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotCrop
import me.weishu.kernelsu.ui.util.setThemeStoreImageSlotVideo
import me.weishu.kernelsu.ui.util.setThemeStoreStartupAnimation
import me.weishu.kernelsu.ui.util.setThemeStoreStartupSound
import me.weishu.kernelsu.ui.util.setThemeStoreVideoBackground
import me.weishu.kernelsu.ui.util.setThemeStoreVideoBackgroundDurationSeconds
import me.weishu.kernelsu.ui.util.setThemeStoreWallpaper
import me.weishu.kernelsu.ui.util.setThemeStoreWallpaperCrop
import me.weishu.kernelsu.ui.util.setCustomNavigationIcon
import me.weishu.kernelsu.ui.util.setCustomNavigationIconCrop
import me.weishu.kernelsu.ui.util.takePersistableAudioReadPermission
import me.weishu.kernelsu.ui.util.takePersistableImageReadPermission
import me.weishu.kernelsu.ui.util.takePersistableStartupAnimationReadPermission
import me.weishu.kernelsu.ui.util.takePersistableVideoBackgroundReadPermission
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import kotlin.math.roundToInt

@Composable
fun ThemeStoreScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf(readThemeStoreSummary(context)) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var cropTarget by remember { mutableStateOf<CropTarget?>(null) }
    var pendingCardImageSlot by remember { mutableStateOf<ThemeStoreImageSlot?>(null) }
    var pendingCardVideoSlot by remember { mutableStateOf<ThemeStoreImageSlot?>(null) }
    var pendingNavigationIconSlot by remember { mutableStateOf<CustomNavigationIconSlot?>(null) }
    var showWallpaperPreview by rememberSaveable { mutableStateOf(false) }
    var showVideoBackgroundPreview by rememberSaveable { mutableStateOf(false) }
    var previewCardVideoSlot by rememberSaveable { mutableStateOf<String?>(null) }
    var startupPreviewUri by rememberSaveable { mutableStateOf<String?>(null) }

    fun refreshSummary() {
        summary = readThemeStoreSummary(context)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(THEME_STORE_FILE_MIME_TYPE),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                exportThemeStorePackage(context, uri)
            }
            busy = false
            val message = if (result.success) {
                if (result.warnings.isEmpty()) {
                    R.string.theme_store_export_success
                } else {
                    R.string.theme_store_export_partial
                }
            } else {
                R.string.theme_store_export_failed
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                importThemeStorePackage(context, uri)
            }
            busy = false
            refreshSummary()
            Toast.makeText(
                context,
                if (result.success) R.string.theme_store_import_success else R.string.theme_store_import_failed,
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    val cardImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val slot = pendingCardImageSlot ?: return@rememberLauncherForActivityResult
        pendingCardImageSlot = null
        uri ?: return@rememberLauncherForActivityResult
        val uriString = persistCustomImageReference(context, uri, slot.uriKey)
            ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
        setThemeStoreImageSlot(context, slot, uriString)
        refreshSummary()
        cropTarget = CropTarget.Card(slot)
    }
    val cardVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val slot = pendingCardVideoSlot ?: return@rememberLauncherForActivityResult
        pendingCardVideoSlot = null
        uri ?: return@rememberLauncherForActivityResult
        takePersistableVideoBackgroundReadPermission(context, uri)
        setThemeStoreImageSlotVideo(context, slot, uri.toString())
        refreshSummary()
        cropTarget = CropTarget.Card(slot)
        previewCardVideoSlot = null
    }
    val navigationIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val slot = pendingNavigationIconSlot ?: return@rememberLauncherForActivityResult
        pendingNavigationIconSlot = null
        uri ?: return@rememberLauncherForActivityResult
        val uriString = persistCustomImageReference(context, uri, slot.uriKey)
            ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
        setCustomNavigationIcon(context, slot, uriString)
        refreshSummary()
        cropTarget = CropTarget.NavigationIcon(slot)
    }
    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val uriString = persistCustomImageReference(context, uri, CUSTOM_WALLPAPER_URI_KEY)
            ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
        setThemeStoreWallpaper(
            context = context,
            uriString = uriString,
            opacity = summary.wallpaper.opacity,
            passthroughEnabled = summary.wallpaper.passthroughEnabled,
            passthroughOpacity = summary.wallpaper.passthroughOpacity,
        )
        refreshSummary()
        cropTarget = CropTarget.Wallpaper
    }
    val videoBackgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        takePersistableVideoBackgroundReadPermission(context, uri)
        setThemeStoreVideoBackground(
            context = context,
            uriString = uri.toString(),
            durationSeconds = summary.wallpaper.videoDurationSeconds,
            opacity = summary.wallpaper.opacity,
            passthroughEnabled = summary.wallpaper.passthroughEnabled,
            passthroughOpacity = summary.wallpaper.passthroughOpacity,
        )
        refreshSummary()
        showWallpaperPreview = false
        showVideoBackgroundPreview = true
    }
    val startupSoundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        StartupSoundPlayer.clearAutoPlaySuppression()
        uri ?: return@rememberLauncherForActivityResult
        takePersistableAudioReadPermission(context, uri)
        setThemeStoreStartupSound(context, uri.toString())
        refreshSummary()
        StartupSoundPlayer.play(context, uri.toString()) {
            Toast.makeText(context, R.string.settings_startup_sound_play_failed, Toast.LENGTH_SHORT).show()
        }
    }
    val startupAnimationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        takePersistableStartupAnimationReadPermission(context, uri)
        setThemeStoreStartupAnimation(context, uri.toString())
        refreshSummary()
        startupPreviewUri = uri.toString()
    }

    LaunchedEffect(Unit) {
        refreshSummary()
    }

    val actions = ThemeStoreActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onExport = {
            val fileName = "epkesu-theme.${THEME_STORE_FILE_EXTENSION}"
            exportLauncher.launch(fileName)
        },
        onImport = {
            importLauncher.launch(arrayOf(THEME_STORE_FILE_MIME_TYPE, "application/octet-stream", "*/*"))
        },
        onPickCard = { slot ->
            pendingCardImageSlot = slot
            cardImageLauncher.launch(arrayOf("image/*"))
        },
        onPickCardVideo = { slot ->
            if (slot.supportsVideo) {
                pendingCardVideoSlot = slot
                cardVideoLauncher.launch(arrayOf("video/*"))
            }
        },
        onPreviewCardVideo = { slot ->
            if (slot.supportsVideo) {
                previewCardVideoSlot = slot.name
            }
        },
        onCropCard = { slot -> cropTarget = CropTarget.Card(slot) },
        onClearCard = { slot ->
            setThemeStoreImageSlot(context, slot, null)
            refreshSummary()
            if (slot.name == previewCardVideoSlot) {
                previewCardVideoSlot = null
            }
        },
        onPickNavigationIcon = { slot ->
            pendingNavigationIconSlot = slot
            navigationIconLauncher.launch(arrayOf("image/*"))
        },
        onCropNavigationIcon = { slot -> cropTarget = CropTarget.NavigationIcon(slot) },
        onClearNavigationIcon = { slot ->
            setCustomNavigationIcon(context, slot, null)
            refreshSummary()
        },
        onPickWallpaper = {
            wallpaperLauncher.launch(arrayOf("image/*"))
        },
        onPreviewWallpaper = {
            showWallpaperPreview = true
        },
        onCropWallpaper = {
            cropTarget = CropTarget.Wallpaper
        },
        onClearWallpaper = {
            setThemeStoreWallpaper(context, null)
            setThemeStoreVideoBackground(context, null)
            refreshSummary()
            showWallpaperPreview = false
            showVideoBackgroundPreview = false
        },
        onSetWallpaperOpacity = { opacity ->
            if (summary.wallpaper.hasVideoSelected) {
                setThemeStoreVideoBackground(
                    context = context,
                    uriString = summary.wallpaper.videoUriString,
                    durationSeconds = summary.wallpaper.videoDurationSeconds,
                    opacity = opacity,
                    passthroughEnabled = summary.wallpaper.passthroughEnabled,
                    passthroughOpacity = summary.wallpaper.passthroughOpacity,
                )
            } else {
                setThemeStoreWallpaper(
                    context = context,
                    uriString = summary.wallpaper.uriString,
                    opacity = opacity,
                    crop = summary.wallpaper.crop,
                    passthroughEnabled = summary.wallpaper.passthroughEnabled,
                    passthroughOpacity = summary.wallpaper.passthroughOpacity,
                )
            }
            refreshSummary()
        },
        onPickVideoBackground = {
            videoBackgroundLauncher.launch(arrayOf("video/*"))
        },
        onPreviewVideoBackground = {
            showVideoBackgroundPreview = true
        },
        onSetVideoBackgroundDurationSeconds = { seconds ->
            setThemeStoreVideoBackgroundDurationSeconds(context, seconds)
            refreshSummary()
        },
        onPickStartupSound = {
            StartupSoundPlayer.suppressNextAutoPlay()
            startupSoundLauncher.launch(arrayOf("audio/*"))
        },
        onPreviewStartupSound = {
            StartupSoundPlayer.play(context, summary.startupSoundUri) {
                Toast.makeText(context, R.string.settings_startup_sound_play_failed, Toast.LENGTH_SHORT).show()
            }
        },
        onClearStartupSound = {
            StartupSoundPlayer.stop()
            setThemeStoreStartupSound(context, null)
            refreshSummary()
        },
        onPickStartupAnimation = {
            startupAnimationLauncher.launch(CUSTOM_STARTUP_ANIMATION_MIME_TYPES)
        },
        onPreviewStartupAnimation = {
            startupPreviewUri = summary.startupAnimationUri
        },
        onClearStartupAnimation = {
            setThemeStoreStartupAnimation(context, null)
            refreshSummary()
            startupPreviewUri = null
        },
    )

    val screenContent = @Composable { paddingValues: PaddingValues ->
        ThemeStoreContent(
            summary = summary,
            busy = busy,
            actions = actions,
            modifier = Modifier.padding(paddingValues),
        )
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproThemeStoreScreen(
            onBack = actions.onBack,
            content = screenContent,
        )
    } else {
        MiuixThemeStoreScreen(actions.onBack, screenContent)
    }

    val target = cropTarget
    when (target) {
        is CropTarget.Card -> {
            val cardState = summary.cardState(target.slot)
            val videoPreviewBitmap = rememberCustomVideoFrameBitmap(cardState.videoUriString)
            SettingsWallpaperCropDialog(
                show = cardState.hasSelected,
                uriString = cardState.uriString ?: cardState.videoUriString,
                crop = cardState.crop,
                onCropChange = {
                    setThemeStoreImageSlotCrop(context, target.slot, it)
                    refreshSummary()
                    cropTarget = null
                    if (cardState.hasVideoSelected) {
                        previewCardVideoSlot = target.slot.name
                    }
                },
                onDismissRequest = { cropTarget = null },
                title = stringResource(target.slot.cropTitleRes),
                editorAspectRatio = target.slot.aspectRatio,
                cropAspectRatio = target.slot.aspectRatio,
                previewBitmap = videoPreviewBitmap,
            )
        }

        is CropTarget.NavigationIcon -> {
            val iconState = summary.navigationIcons[target.slot]
            SettingsWallpaperCropDialog(
                show = iconState.hasSelected,
                uriString = iconState.uriString,
                crop = iconState.crop,
                onCropChange = {
                    setCustomNavigationIconCrop(context, target.slot, it)
                    refreshSummary()
                    cropTarget = null
                },
                onDismissRequest = { cropTarget = null },
                title = stringResource(target.slot.cropTitleRes),
                emptyText = stringResource(R.string.settings_navigation_icon_not_selected),
                editorAspectRatio = 1f,
                cropAspectRatio = 1f,
                defaultCrop = DEFAULT_CUSTOM_NAVIGATION_ICON_CROP,
            )
        }

        CropTarget.Wallpaper -> {
            SettingsWallpaperCropDialog(
                show = summary.wallpaper.hasSelected,
                uriString = summary.wallpaper.uriString,
                crop = summary.wallpaper.crop,
                onCropChange = {
                    setThemeStoreWallpaperCrop(context, it)
                    refreshSummary()
                    cropTarget = null
                    showWallpaperPreview = true
                },
                onDismissRequest = { cropTarget = null },
            )
        }

        null -> Unit
    }

    SettingsWallpaperPreviewDialog(
        show = showWallpaperPreview,
        uriString = summary.wallpaper.uriString,
        opacity = summary.wallpaper.opacity,
        crop = summary.wallpaper.crop,
        passthroughEnabled = summary.wallpaper.passthroughEnabled,
        passthroughOpacity = summary.wallpaper.passthroughOpacity,
        onDismissRequest = { showWallpaperPreview = false },
    )
    SettingsVideoBackgroundPreviewDialog(
        show = showVideoBackgroundPreview,
        uriString = summary.wallpaper.videoUriString,
        durationSeconds = summary.wallpaper.videoDurationSeconds,
        opacity = summary.wallpaper.opacity,
        passthroughEnabled = summary.wallpaper.passthroughEnabled,
        passthroughOpacity = summary.wallpaper.passthroughOpacity,
        onDismissRequest = { showVideoBackgroundPreview = false },
    )
    val previewVideoSlot = previewCardVideoSlot?.let { slotName ->
        ThemeStoreImageSlot.entries.firstOrNull { it.name == slotName }
    }
    val previewVideoState = previewVideoSlot?.let(summary::cardState)
    ThemeStoreCardVideoPreviewDialog(
        show = previewVideoSlot != null,
        slot = previewVideoSlot,
        uriString = previewVideoState?.videoUriString,
        crop = previewVideoState?.crop,
        onDismissRequest = { previewCardVideoSlot = null },
    )

    if (!startupPreviewUri.isNullOrBlank()) {
        StartupAnimationOverlay(
            uriString = startupPreviewUri,
            onFinished = { startupPreviewUri = null },
            onError = {
                startupPreviewUri = null
                Toast.makeText(context, R.string.settings_startup_animation_play_failed, Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun MiuixThemeStoreScreen(
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.theme_store),
                color = Color.Transparent,
                titleColor = colorScheme.onSurface,
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                            tint = colorScheme.onBackground,
                        )
                    }
                },
            )
        },
        content = content,
    )
}

@Composable
private fun SkrootproThemeStoreScreen(
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    SkrootproScreen(
        title = stringResource(R.string.theme_store),
        bottomInnerPadding = 0.dp,
    ) { paddingValues ->
        Box {
            content(paddingValues)
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 14.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.12f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ThemeStoreContent(
    summary: ThemeStoreSummary,
    busy: Boolean,
    actions: ThemeStoreActions,
    modifier: Modifier = Modifier,
) {
    var cardsExpanded by rememberSaveable { mutableStateOf(true) }
    var navigationExpanded by rememberSaveable { mutableStateOf(false) }
    var mediaExpanded by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ThemeStoreHero(summary = summary, busy = busy, actions = actions)

        ThemeStoreSection(
            title = stringResource(R.string.theme_store_cards),
            configured = summary.cardConfiguredCount,
            total = ThemeStoreImageSlot.entries.size,
            expanded = cardsExpanded,
            onExpandedChange = { cardsExpanded = it },
        ) {
            ThemeStoreCardImageItem(
                slot = ThemeStoreImageSlot.Lkm,
                state = summary.lkmCard,
                icon = Icons.Rounded.SettingsSuggest,
                actions = actions,
            )
            ThemeStoreCardImageItem(
                slot = ThemeStoreImageSlot.Superuser,
                state = summary.superuserCard,
                icon = Icons.Rounded.Security,
                actions = actions,
            )
            ThemeStoreCardImageItem(
                slot = ThemeStoreImageSlot.Module,
                state = summary.moduleCard,
                icon = Icons.Rounded.Extension,
                actions = actions,
            )
            ThemeStoreCardImageItem(
                slot = ThemeStoreImageSlot.StatusMonitor,
                state = summary.statusMonitorCard,
                icon = Icons.Rounded.CheckCircle,
                actions = actions,
            )
            ThemeStoreCardImageItem(
                slot = ThemeStoreImageSlot.SystemInfo,
                state = summary.systemInfoCard,
                icon = Icons.Rounded.SettingsSuggest,
                actions = actions,
            )
            ThemeStoreCardImageItem(
                slot = ThemeStoreImageSlot.RebootMenu,
                state = summary.rebootMenuCard,
                icon = Icons.Rounded.PowerSettingsNew,
                actions = actions,
            )
        }

        ThemeStoreSection(
            title = stringResource(R.string.theme_store_navigation_icons),
            configured = summary.navigationIconConfiguredCount,
            total = themeStoreNavigationIconSlots.size,
            expanded = navigationExpanded,
            onExpandedChange = { navigationExpanded = it },
        ) {
            ThemeStoreNavigationIconGrid(summary = summary, actions = actions)
        }

        ThemeStoreSection(
            title = stringResource(R.string.theme_store_wallpaper_media),
            configured = summary.mediaConfiguredCount,
            total = summary.mediaItemCount,
            expanded = mediaExpanded,
            onExpandedChange = { mediaExpanded = it },
        ) {
            ThemeStoreWallpaperItem(summary = summary, actions = actions)
            ThemeStoreMediaItem(
                title = stringResource(R.string.settings_startup_sound),
                summary = stringResource(
                    if (summary.startupSoundUri.isNullOrBlank()) {
                        R.string.settings_startup_sound_summary
                    } else {
                        R.string.settings_startup_sound_selected_summary
                    }
                ),
                selected = !summary.startupSoundUri.isNullOrBlank(),
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                onPick = actions.onPickStartupSound,
                onPreview = actions.onPreviewStartupSound,
                onClear = actions.onClearStartupSound,
            )
            ThemeStoreMediaItem(
                title = stringResource(R.string.settings_startup_animation),
                summary = stringResource(
                    if (summary.startupAnimationUri.isNullOrBlank()) {
                        R.string.settings_startup_animation_summary
                    } else {
                        R.string.settings_startup_animation_selected_summary
                    }
                ),
                selected = !summary.startupAnimationUri.isNullOrBlank(),
                icon = Icons.Rounded.PlayCircle,
                onPick = actions.onPickStartupAnimation,
                onPreview = actions.onPreviewStartupAnimation,
                onClear = actions.onClearStartupAnimation,
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun ThemeStoreHero(
    summary: ThemeStoreSummary,
    busy: Boolean,
    actions: ThemeStoreActions,
) {
    ThemeStoreSurface {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Wallpaper,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.theme_store_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = themeStoreTextColor(),
                    )
                    Text(
                        text = stringResource(R.string.theme_store_selected_count, summary.selectedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeStoreMutedColor(),
                    )
                    Text(
                        text = stringResource(R.string.theme_store_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeStoreMutedColor(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ThemeStoreHeroActions(
                    busy = busy,
                    onExport = actions.onExport,
                    onImport = actions.onImport,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeStoreHeroActions(
    busy: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            enabled = !busy,
            onClick = onExport,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.theme_store_export), maxLines = 1)
        }
        OutlinedButton(
            enabled = !busy,
            onClick = onImport,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.theme_store_import), maxLines = 1)
        }
    }
}

@Composable
private fun ThemeStoreSection(
    title: String,
    configured: Int,
    total: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onExpandedChange(!expanded) }
                .padding(start = 4.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
                        SkrootproColors.Text
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontSize = if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
                        skrootproSp(13f, maxScale = 1f)
                    } else {
                        14.sp
                    },
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.theme_store_configured_count, configured, total),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeStoreMutedColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = title,
                tint = if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
                    SkrootproColors.Muted
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        }
    }
}

@Composable
private fun ThemeStoreCardImageItem(
    slot: ThemeStoreImageSlot,
    state: ThemeStoreImageState,
    icon: ImageVector,
    actions: ThemeStoreActions,
) {
    val imageBitmap = rememberCustomImageBitmap(
        uriString = state.uriString,
        maxSide = 900,
        crop = state.crop,
    )
    val supportsVideo = slot.supportsVideo
    ThemeStoreSurface {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(slot.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = themeStoreTextColor(),
                    )
                    Text(
                        text = stringResource(
                            when {
                                state.hasVideoSelected -> R.string.settings_video_background_selected_summary
                                state.hasSelected -> R.string.theme_store_item_selected
                                else -> R.string.theme_store_item_empty
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeStoreMutedColor(),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(slot.aspectRatio)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.hasVideoSelected -> {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp),
                            text = stringResource(R.string.home_lkm_video_wallpaper_pick),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        ThemeStoreSelectedBadge(modifier = Modifier.align(Alignment.TopEnd))
                    }

                    imageBitmap != null -> {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = imageBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                        )
                        ThemeStoreSelectedBadge(modifier = Modifier.align(Alignment.TopEnd))
                    }

                    else -> {
                        Text(
                            text = stringResource(R.string.settings_wallpaper_not_selected),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (supportsVideo) {
                ThemeStoreActionFlow {
                    ThemeStoreSmallButton(
                        text = stringResource(R.string.settings_wallpaper),
                        icon = Icons.Rounded.UploadFile,
                        onClick = { actions.onPickCard(slot) },
                    )
                    ThemeStoreSmallButton(
                        text = stringResource(R.string.settings_video_background),
                        icon = Icons.Rounded.Videocam,
                        onClick = { actions.onPickCardVideo(slot) },
                    )
                    if (state.hasSelected) {
                        if (state.hasImageSelected || state.hasVideoSelected) {
                            ThemeStoreSmallButton(
                                text = stringResource(R.string.settings_wallpaper_crop),
                                icon = Icons.Rounded.ImageSearch,
                                onClick = { actions.onCropCard(slot) },
                            )
                        }
                        if (state.hasVideoSelected) {
                            ThemeStoreSmallButton(
                                text = stringResource(R.string.settings_video_background_preview),
                                icon = Icons.Rounded.PlayCircle,
                                onClick = { actions.onPreviewCardVideo(slot) },
                            )
                        }
                        ThemeStoreSmallButton(
                            text = stringResource(R.string.close),
                            icon = Icons.Rounded.Close,
                            onClick = { actions.onClearCard(slot) },
                        )
                    }
                }
            } else {
                ThemeStoreActionRow(
                    selected = state.hasSelected,
                    onPick = { actions.onPickCard(slot) },
                    onCrop = { actions.onCropCard(slot) },
                    onClear = { actions.onClearCard(slot) },
                )
            }
        }
    }
}

@Composable
private fun ThemeStoreNavigationIconGrid(
    summary: ThemeStoreSummary,
    actions: ThemeStoreActions,
) {
    ThemeStoreSurface {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            themeStoreNavigationIconSlots.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowItems.forEach { item ->
                        ThemeStoreNavigationIconTile(
                            modifier = Modifier.weight(1f),
                            slot = item.slot,
                            state = summary.navigationIcons[item.slot],
                            icon = item.icon,
                            actions = actions,
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeStoreNavigationIconTile(
    slot: CustomNavigationIconSlot,
    state: CustomNavigationIconState,
    icon: ImageVector,
    actions: ThemeStoreActions,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = rememberCustomImageBitmap(
        uriString = state.uriString,
        maxSide = 256,
        crop = state.crop,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (imageBitmap != null) {
                Image(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(13.dp)),
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
                ThemeStoreSelectedBadge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    compact = true,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(slot.titleRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = themeStoreTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    if (state.hasSelected) {
                        R.string.theme_store_item_selected
                    } else {
                        R.string.theme_store_item_empty
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = themeStoreMutedColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ThemeStoreActionFlow(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ThemeStoreSmallButton(
                text = stringResource(R.string.select_file),
                icon = Icons.Rounded.UploadFile,
                onClick = { actions.onPickNavigationIcon(slot) },
            )
            if (state.hasSelected) {
                ThemeStoreSmallButton(
                    text = stringResource(R.string.settings_navigation_icon_crop_action),
                    icon = Icons.Rounded.ImageSearch,
                    onClick = { actions.onCropNavigationIcon(slot) },
                )
                ThemeStoreSmallButton(
                    text = stringResource(R.string.close),
                    icon = Icons.Rounded.Close,
                    onClick = { actions.onClearNavigationIcon(slot) },
                )
            }
        }
    }
}

@Composable
private fun ThemeStoreWallpaperItem(
    summary: ThemeStoreSummary,
    actions: ThemeStoreActions,
) {
    var opacity by remember(summary.wallpaper.opacity) { mutableFloatStateOf(summary.wallpaper.opacity) }
    var videoDuration by remember(summary.wallpaper.videoDurationSeconds) {
        mutableFloatStateOf(summary.wallpaper.videoDurationSeconds.toFloat())
    }
    val imageBitmap = rememberCustomImageBitmap(
        uriString = summary.wallpaper.uriString,
        maxSide = 900,
        crop = summary.wallpaper.crop,
    )
    ThemeStoreSurface {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Wallpaper, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_background),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = themeStoreTextColor(),
                    )
                    Text(
                        text = backgroundSummary(
                            hasWallpaper = summary.wallpaper.hasImageSelected,
                            hasVideo = summary.wallpaper.hasVideoSelected,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = themeStoreMutedColor(),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    summary.wallpaper.hasVideoSelected -> {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp),
                            text = stringResource(R.string.settings_video_background_selected_summary),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        ThemeStoreSelectedBadge(modifier = Modifier.align(Alignment.TopEnd))
                    }

                    imageBitmap != null -> {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = imageBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 1f - opacity.coerceIn(0f, 1f)))
                        )
                        ThemeStoreSelectedBadge(modifier = Modifier.align(Alignment.TopEnd))
                    }

                    else -> {
                        Text(
                            text = stringResource(R.string.settings_background_not_selected),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (summary.wallpaper.hasSelected) {
                androidx.compose.material3.Slider(
                    value = opacity,
                    onValueChange = {
                        opacity = it
                        actions.onSetWallpaperOpacity(it)
                    },
                    valueRange = MIN_CUSTOM_WALLPAPER_OPACITY..MAX_CUSTOM_WALLPAPER_OPACITY,
                )
            }
            if (summary.wallpaper.hasVideoSelected) {
                Text(
                    text = stringResource(
                        R.string.settings_video_background_duration_value,
                        videoDuration.roundToInt(),
                    ),
                    color = themeStoreMutedColor(),
                    style = MaterialTheme.typography.bodySmall,
                )
                androidx.compose.material3.Slider(
                    value = videoDuration,
                    onValueChange = {
                        videoDuration = it
                        actions.onSetVideoBackgroundDurationSeconds(it.roundToInt())
                    },
                    valueRange = MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS.toFloat()..
                        MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS.toFloat(),
                    steps = MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS -
                        MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS - 1,
                )
            }
            ThemeStoreActionFlow {
                ThemeStoreSmallButton(
                    text = stringResource(R.string.settings_wallpaper),
                    icon = Icons.Rounded.FileUpload,
                    onClick = actions.onPickWallpaper,
                )
                ThemeStoreSmallButton(
                    text = stringResource(R.string.settings_video_background),
                    icon = Icons.Rounded.Videocam,
                    onClick = actions.onPickVideoBackground,
                )
                if (summary.wallpaper.hasSelected) {
                    if (summary.wallpaper.hasImageSelected) {
                        ThemeStoreSmallButton(
                            text = stringResource(R.string.settings_wallpaper_crop),
                            icon = Icons.Rounded.ImageSearch,
                            onClick = actions.onCropWallpaper,
                        )
                        ThemeStoreSmallButton(
                            text = stringResource(R.string.settings_wallpaper_preview),
                            icon = Icons.Rounded.PlayCircle,
                            onClick = actions.onPreviewWallpaper,
                        )
                    }
                    if (summary.wallpaper.hasVideoSelected) {
                        ThemeStoreSmallButton(
                            text = stringResource(R.string.settings_video_background_preview),
                            icon = Icons.Rounded.PlayCircle,
                            onClick = actions.onPreviewVideoBackground,
                        )
                    }
                    ThemeStoreSmallButton(
                        text = stringResource(R.string.close),
                        icon = Icons.Rounded.Close,
                        onClick = actions.onClearWallpaper,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeStoreMediaItem(
    title: String,
    summary: String,
    selected: Boolean,
    icon: ImageVector,
    onPick: () -> Unit,
    onPreview: () -> Unit,
    onClear: () -> Unit,
) {
    ThemeStoreSurface {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = themeStoreTextColor(),
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = themeStoreMutedColor(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ThemeStoreActionFlow {
                ThemeStoreSmallButton(
                    text = stringResource(R.string.select_file),
                    icon = Icons.Rounded.UploadFile,
                    onClick = onPick,
                )
                if (selected) {
                    ThemeStoreSmallButton(
                        text = stringResource(R.string.open),
                        icon = Icons.Rounded.PlayCircle,
                        onClick = onPreview,
                    )
                    ThemeStoreSmallButton(
                        text = stringResource(R.string.close),
                        icon = Icons.Rounded.Close,
                        onClick = onClear,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeStoreCardVideoPreviewDialog(
    show: Boolean,
    slot: ThemeStoreImageSlot?,
    uriString: String?,
    crop: CustomWallpaperCrop?,
    onDismissRequest: () -> Unit,
) {
    if (!show || slot == null || uriString.isNullOrBlank() || crop == null) return

    val aspectRatio = when (slot) {
        ThemeStoreImageSlot.Lkm -> 1.86f
        else -> slot.aspectRatio
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(slot.titleRes)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                CustomVideoBackground(
                    uriString = uriString,
                    drawOverlay = false,
                    crop = crop,
                    touchPassthrough = true,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.44f))
                )
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = Color.White,
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}

@Composable
private fun ThemeStoreActionRow(
    selected: Boolean,
    onPick: () -> Unit,
    onCrop: () -> Unit,
    onClear: () -> Unit,
    cropText: String? = null,
) {
    ThemeStoreActionFlow {
        ThemeStoreSmallButton(
            text = stringResource(R.string.select_file),
            icon = Icons.Rounded.UploadFile,
            onClick = onPick,
        )
        if (selected) {
            ThemeStoreSmallButton(
                text = cropText ?: stringResource(R.string.settings_wallpaper_crop),
                icon = Icons.Rounded.ImageSearch,
                onClick = onCrop,
            )
            ThemeStoreSmallButton(
                text = stringResource(R.string.close),
                icon = Icons.Rounded.Close,
                onClick = onClear,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeStoreActionFlow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun ThemeStoreSmallButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        Box(
            modifier = modifier
                .height(36.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(SkrootproColors.Purple)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = skrootproSp(12.5f, maxScale = 1.0f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        return
    }

    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.size(6.dp))
        Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ThemeStoreSurface(
    content: @Composable () -> Unit,
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SkrootproColors.BarSurface),
        ) {
            content()
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        content = { content() },
    )
}

@Composable
private fun ThemeStoreSelectedBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .size(if (compact) 22.dp else 28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(if (compact) 14.dp else 18.dp),
        )
    }
}

@Composable
private fun backgroundSummary(
    hasWallpaper: Boolean,
    hasVideo: Boolean,
): String {
    return stringResource(
        when {
            hasVideo -> R.string.settings_video_background_selected_summary
            hasWallpaper -> R.string.settings_wallpaper_selected_summary
            else -> R.string.settings_background_summary
        }
    )
}

@Composable
private fun themeStoreTextColor(): Color {
    return if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproColors.Text
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun themeStoreMutedColor(): Color {
    return if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproColors.Muted
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private data class ThemeStoreActions(
    val onBack: () -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onPickCard: (ThemeStoreImageSlot) -> Unit,
    val onPickCardVideo: (ThemeStoreImageSlot) -> Unit,
    val onPreviewCardVideo: (ThemeStoreImageSlot) -> Unit,
    val onCropCard: (ThemeStoreImageSlot) -> Unit,
    val onClearCard: (ThemeStoreImageSlot) -> Unit,
    val onPickNavigationIcon: (CustomNavigationIconSlot) -> Unit,
    val onCropNavigationIcon: (CustomNavigationIconSlot) -> Unit,
    val onClearNavigationIcon: (CustomNavigationIconSlot) -> Unit,
    val onPickWallpaper: () -> Unit,
    val onPreviewWallpaper: () -> Unit,
    val onCropWallpaper: () -> Unit,
    val onClearWallpaper: () -> Unit,
    val onSetWallpaperOpacity: (Float) -> Unit,
    val onPickVideoBackground: () -> Unit,
    val onPreviewVideoBackground: () -> Unit,
    val onSetVideoBackgroundDurationSeconds: (Int) -> Unit,
    val onPickStartupSound: () -> Unit,
    val onPreviewStartupSound: () -> Unit,
    val onClearStartupSound: () -> Unit,
    val onPickStartupAnimation: () -> Unit,
    val onPreviewStartupAnimation: () -> Unit,
    val onClearStartupAnimation: () -> Unit,
)

private sealed interface CropTarget {
    data class Card(val slot: ThemeStoreImageSlot) : CropTarget
    data class NavigationIcon(val slot: CustomNavigationIconSlot) : CropTarget
    data object Wallpaper : CropTarget
}

private data class ThemeStoreNavigationIconOption(
    val slot: CustomNavigationIconSlot,
    val icon: ImageVector,
)

private val themeStoreNavigationIconSlots = listOf(
    ThemeStoreNavigationIconOption(CustomNavigationIconSlot.Home, Icons.Rounded.Cottage),
    ThemeStoreNavigationIconOption(CustomNavigationIconSlot.Superuser, Icons.Rounded.Security),
    ThemeStoreNavigationIconOption(CustomNavigationIconSlot.Module, Icons.Rounded.Extension),
    ThemeStoreNavigationIconOption(CustomNavigationIconSlot.Settings, Icons.Rounded.Settings),
)

private val ThemeStoreImageSlot.titleRes: Int
    get() = when (this) {
        ThemeStoreImageSlot.Lkm -> R.string.theme_store_lkm_card
        ThemeStoreImageSlot.Superuser -> R.string.theme_store_superuser_card
        ThemeStoreImageSlot.Module -> R.string.theme_store_module_card
        ThemeStoreImageSlot.StatusMonitor -> R.string.theme_store_status_monitor_card
        ThemeStoreImageSlot.SystemInfo -> R.string.theme_store_system_info_card
        ThemeStoreImageSlot.RebootMenu -> R.string.theme_store_reboot_menu_card
    }

private val ThemeStoreImageSlot.cropTitleRes: Int
    get() = when (this) {
        ThemeStoreImageSlot.Lkm -> R.string.home_lkm_wallpaper_crop
        ThemeStoreImageSlot.Superuser -> R.string.home_superuser_wallpaper_crop
        ThemeStoreImageSlot.Module -> R.string.home_module_wallpaper_crop
        ThemeStoreImageSlot.StatusMonitor -> R.string.home_status_monitor_wallpaper_crop
        ThemeStoreImageSlot.SystemInfo -> R.string.home_system_info_wallpaper_crop
        ThemeStoreImageSlot.RebootMenu -> R.string.home_reboot_menu_wallpaper_crop
    }

private val ThemeStoreImageSlot.aspectRatio: Float
    get() = when (this) {
        ThemeStoreImageSlot.Lkm -> 1.86f
        ThemeStoreImageSlot.Superuser,
        ThemeStoreImageSlot.Module -> 1.72f
        ThemeStoreImageSlot.StatusMonitor -> 2.72f
        ThemeStoreImageSlot.SystemInfo -> 1.36f
        ThemeStoreImageSlot.RebootMenu -> 0.72f
    }

private val ThemeStoreImageSlot.supportsVideo: Boolean
    get() = this == ThemeStoreImageSlot.Lkm || this == ThemeStoreImageSlot.RebootMenu

private fun ThemeStoreSummary.cardState(slot: ThemeStoreImageSlot): ThemeStoreImageState {
    return when (slot) {
        ThemeStoreImageSlot.Lkm -> lkmCard
        ThemeStoreImageSlot.Superuser -> superuserCard
        ThemeStoreImageSlot.Module -> moduleCard
        ThemeStoreImageSlot.StatusMonitor -> statusMonitorCard
        ThemeStoreImageSlot.SystemInfo -> systemInfoCard
        ThemeStoreImageSlot.RebootMenu -> rebootMenuCard
    }
}

private val ThemeStoreSummary.cardConfiguredCount: Int
    get() = ThemeStoreImageSlot.entries.count { cardState(it).hasSelected }

private val ThemeStoreSummary.navigationIconConfiguredCount: Int
    get() = navigationIcons.selectedCount

private val ThemeStoreSummary.mediaConfiguredCount: Int
    get() = CustomPageBackgroundTarget.entries.count { pageBackgrounds[it].hasMedia } +
        listOf(
            wallpaper.hasSelected,
            !startupSoundUri.isNullOrBlank(),
            !startupAnimationUri.isNullOrBlank(),
        ).count { it }

private val ThemeStoreSummary.mediaItemCount: Int
    get() = CustomPageBackgroundTarget.entries.size + 3
