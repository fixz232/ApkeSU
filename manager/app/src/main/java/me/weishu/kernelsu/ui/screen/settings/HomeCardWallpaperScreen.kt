package me.weishu.kernelsu.ui.screen.settings

import android.graphics.Bitmap
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.rememberCustomVideoFrameBitmap
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.screen.home.HomeMetricCardWallpaperBackground
import me.weishu.kernelsu.ui.screen.home.HomeMetricCardWallpaperState
import me.weishu.kernelsu.ui.screen.home.HomeMetricCardWallpaperTarget
import me.weishu.kernelsu.ui.screen.home.rememberHomeMetricCardWallpaperBitmap
import me.weishu.kernelsu.ui.screen.home.rememberHomeMetricCardWallpaperState
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

private const val MIUIX_LKM_CARD_WALLPAPER_ASPECT_RATIO = 1.86f

private data class HomeCardWallpaperSectionSpec(
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    val targets: List<HomeMetricCardWallpaperTarget>,
)

private val homeCardWallpaperSections = listOf(
    HomeCardWallpaperSectionSpec(
        titleRes = R.string.home_card_wallpapers_main_section,
        summaryRes = R.string.home_card_wallpapers_main_section_summary,
        targets = listOf(HomeMetricCardWallpaperTarget.Lkm),
    ),
    HomeCardWallpaperSectionSpec(
        titleRes = R.string.home_card_wallpapers_metric_section,
        summaryRes = R.string.home_card_wallpapers_metric_section_summary,
        targets = listOf(HomeMetricCardWallpaperTarget.Superuser, HomeMetricCardWallpaperTarget.Module),
    ),
    HomeCardWallpaperSectionSpec(
        titleRes = R.string.home_card_wallpapers_system_section,
        summaryRes = R.string.home_card_wallpapers_system_section_summary,
        targets = listOf(
            HomeMetricCardWallpaperTarget.StatusMonitor,
            HomeMetricCardWallpaperTarget.SystemInfo,
            HomeMetricCardWallpaperTarget.RebootMenu,
        ),
    ),
)

@Composable
fun HomeCardWallpaperScreen() {
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }

    when (LocalUiMode.current) {
        UiMode.Material -> HomeCardWallpaperScreenMaterial(onBack = onBack)
        UiMode.Miuix -> HomeCardWallpaperScreenMiuix(onBack = onBack)
    }
}

@Composable
private fun HomeCardWallpaperScreenMaterial(
    onBack: () -> Unit,
) {
    androidx.compose.material3.Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.home_card_wallpapers)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        HomeCardWallpaperContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun HomeCardWallpaperScreenMiuix(
    onBack: () -> Unit,
) {
    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.home_card_wallpapers),
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
        HomeCardWallpaperContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun HomeCardWallpaperContent(
    modifier: Modifier,
) {
    var cropTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var previewTarget by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.home_card_wallpapers_summary),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        homeCardWallpaperSections.forEach { section ->
            HomeCardWallpaperSection(
                section = section,
                cropTarget = cropTarget,
                onCropTargetChange = { cropTarget = it },
                previewTarget = previewTarget,
                onPreviewTargetChange = { previewTarget = it },
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun HomeCardWallpaperSection(
    section: HomeCardWallpaperSectionSpec,
    cropTarget: String?,
    onCropTargetChange: (String?) -> Unit,
    previewTarget: String?,
    onPreviewTargetChange: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeCardWallpaperSectionTitle(
            title = stringResource(section.titleRes),
            summary = stringResource(section.summaryRes),
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val useTwoColumns = maxWidth >= 720.dp && section.targets.size > 1
            if (useTwoColumns) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    section.targets.chunked(2).forEach { rowTargets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowTargets.forEach { target ->
                                HomeCardWallpaperItem(
                                    target = target,
                                    showCrop = cropTarget == target.name,
                                    onShowCropChange = { show ->
                                        onCropTargetChange(if (show) target.name else null)
                                    },
                                    showPreview = previewTarget == target.name,
                                    onShowPreviewChange = { show ->
                                        onPreviewTargetChange(if (show) target.name else null)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowTargets.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    section.targets.forEach { target ->
                        HomeCardWallpaperItem(
                            target = target,
                            showCrop = cropTarget == target.name,
                            onShowCropChange = { show ->
                                onCropTargetChange(if (show) target.name else null)
                            },
                            showPreview = previewTarget == target.name,
                            onShowPreviewChange = { show ->
                                onPreviewTargetChange(if (show) target.name else null)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCardWallpaperSectionTitle(
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
private fun HomeCardWallpaperItem(
    target: HomeMetricCardWallpaperTarget,
    showCrop: Boolean,
    onShowCropChange: (Boolean) -> Unit,
    showPreview: Boolean,
    onShowPreviewChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiMode = LocalUiMode.current
    val aspectRatio = homeCardWallpaperAspectRatio(target = target, uiMode = uiMode)
    val title = stringResource(target.titleRes)
    val state = rememberHomeMetricCardWallpaperState(
        target = target,
        onWallpaperSelected = { onShowCropChange(true) },
    )
    val bitmap = rememberHomeMetricCardWallpaperBitmap(
        uriString = state.uriString,
        crop = state.crop,
    )
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    val videoFrameBitmap = rememberCustomVideoFrameBitmap(state.videoUriString)
    val summary = stringResource(homeCardWallpaperSummaryRes(state))

    Surface(
        modifier = modifier.fillMaxWidth(),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = summary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.hasSelectedAnyWallpaper) {
                    HomeCardWallpaperOverflowMenu(
                        onEditCrop = { onShowCropChange(true) },
                        onPreview = { onShowPreviewChange(true) },
                        onClear = state.onClearWallpaper,
                    )
                }
            }
            if (state.hasSelectedAnyWallpaper) {
                HomeCardWallpaperFrame(
                    target = target,
                    title = title,
                    imageBitmap = imageBitmap,
                    videoFrameBitmap = videoFrameBitmap,
                    hasMedia = true,
                    aspectRatio = aspectRatio,
                )
            } else {
                HomeCardWallpaperEmptyRow(target = target)
            }
            HomeCardWallpaperPrimaryActions(
                onPickWallpaper = state.onPickWallpaper,
                onPickVideoWallpaper = state.onPickVideoWallpaper,
            )
        }
    }

    SettingsWallpaperCropDialog(
        show = showCrop && state.hasSelectedAnyWallpaper,
        uriString = state.uriString ?: state.videoUriString,
        crop = state.crop,
        onCropChange = {
            state.onCropChange(it)
            onShowPreviewChange(true)
        },
        onDismissRequest = { onShowCropChange(false) },
        title = stringResource(target.cropLabelRes),
        editorAspectRatio = aspectRatio,
        cropAspectRatio = aspectRatio,
        previewBitmap = if (state.hasSelectedVideoWallpaper) videoFrameBitmap else null,
    )
    HomeCardWallpaperPreviewDialog(
        show = showPreview && state.hasSelectedAnyWallpaper,
        title = title,
        target = target,
        bitmap = bitmap,
        videoUriString = state.videoUriString,
        crop = state.crop,
        aspectRatio = aspectRatio,
        onDismissRequest = { onShowPreviewChange(false) },
    )
}

@Composable
private fun HomeCardWallpaperOverflowMenu(
    onEditCrop: () -> Unit,
    onPreview: () -> Unit,
    onClear: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f), CircleShape),
            onClick = { expanded = true },
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.home_header_more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_card_wallpaper_crop_action)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.ImageSearch,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onEditCrop()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_card_wallpaper_preview_action)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onPreview()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_card_wallpaper_clear_action)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onClear()
                },
            )
        }
    }
}

@Composable
private fun HomeCardWallpaperEmptyRow(
    target: HomeMetricCardWallpaperTarget,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(112.dp)
                .height(74.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = homeCardWallpaperTargetIcon(target),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.home_card_wallpaper_empty),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(target.pickLabelRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeCardWallpaperPrimaryActions(
    onPickWallpaper: () -> Unit,
    onPickVideoWallpaper: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeCardWallpaperButton(
            text = stringResource(R.string.home_card_wallpaper_pick_image),
            icon = Icons.Rounded.Wallpaper,
            onClick = onPickWallpaper,
            modifier = Modifier.weight(1f),
        )
        HomeCardWallpaperButton(
            text = stringResource(R.string.home_card_wallpaper_pick_video),
            icon = Icons.Rounded.Videocam,
            onClick = onPickVideoWallpaper,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomeCardWallpaperFrame(
    target: HomeMetricCardWallpaperTarget,
    title: String,
    imageBitmap: ImageBitmap?,
    videoFrameBitmap: ImageBitmap?,
    hasMedia: Boolean,
    aspectRatio: Float,
) {
    val frameBitmap = imageBitmap ?: videoFrameBitmap
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            frameBitmap != null -> Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = frameBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )

            hasMedia -> CircularProgressIndicator()

            else -> Text(
                modifier = Modifier.padding(20.dp),
                text = stringResource(R.string.home_card_wallpaper_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (frameBitmap != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.42f))
            )
            HomeCardWallpaperSampleContent(
                target = target,
                fallbackTitle = title,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun HomeCardWallpaperSampleContent(
    target: HomeMetricCardWallpaperTarget,
    fallbackTitle: String,
    modifier: Modifier = Modifier,
) {
    val primaryColor = Color.White
    val secondaryColor = Color.White.copy(alpha = 0.82f)
    val accentColor = Color.White
    Box(
        modifier = modifier.padding(14.dp),
    ) {
        when (target) {
            HomeMetricCardWallpaperTarget.Lkm -> HomeCardWallpaperStatusSample(
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                accentColor = accentColor,
            )

            HomeMetricCardWallpaperTarget.Superuser,
            HomeMetricCardWallpaperTarget.Module -> HomeCardWallpaperMetricSample(
                target = target,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                accentColor = accentColor,
            )

            HomeMetricCardWallpaperTarget.StatusMonitor -> HomeCardWallpaperStatusMonitorSample(
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                accentColor = accentColor,
            )

            HomeMetricCardWallpaperTarget.SystemInfo -> HomeCardWallpaperSystemInfoSample(
                title = fallbackTitle,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
            )

            HomeMetricCardWallpaperTarget.RebootMenu -> HomeCardWallpaperRebootMenuSample(
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                accentColor = accentColor,
            )
        }
    }
}

@Composable
private fun HomeCardWallpaperStatusSample(
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Icon(
            modifier = Modifier
                .size(112.dp)
                .align(Alignment.BottomEnd),
            imageVector = Icons.Rounded.CheckCircleOutline,
            tint = accentColor.copy(alpha = 0.18f),
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircleOutline,
                    tint = accentColor,
                    contentDescription = null,
                )
            }
            Text(
                text = stringResource(R.string.home_working),
                color = primaryColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            HomeCardWallpaperPreviewTag(label = "LKM")
            Text(
                text = stringResource(R.string.home_working_version, "12345-6"),
                color = secondaryColor,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeCardWallpaperMetricSample(
    target: HomeMetricCardWallpaperTarget,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
) {
    val titleRes = when (target) {
        HomeMetricCardWallpaperTarget.Superuser -> R.string.superuser
        HomeMetricCardWallpaperTarget.Module -> R.string.module
        else -> target.titleRes
    }
    val value = when (target) {
        HomeMetricCardWallpaperTarget.Superuser -> "5"
        HomeMetricCardWallpaperTarget.Module -> "10"
        else -> "0"
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = homeCardWallpaperTargetIcon(target),
                    tint = accentColor,
                    contentDescription = null,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titleRes),
                    color = secondaryColor,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    color = primaryColor,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeCardWallpaperStatusMonitorSample(
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeCardWallpaperStatusLineSample(
            icon = Icons.Rounded.Security,
            label = stringResource(R.string.home_selinux_status),
            value = stringResource(R.string.selinux_status_enforcing),
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            accentColor = accentColor,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.20f)),
        )
        HomeCardWallpaperStatusLineSample(
            icon = Icons.Rounded.Lock,
            label = stringResource(R.string.home_seccomp_status),
            value = stringResource(R.string.seccomp_status_filter),
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            accentColor = accentColor,
        )
    }
}

@Composable
private fun HomeCardWallpaperStatusLineSample(
    icon: ImageVector,
    label: String,
    value: String,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            tint = accentColor,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = secondaryColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = primaryColor,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(accentColor, CircleShape),
        )
    }
}

@Composable
private fun HomeCardWallpaperSystemInfoSample(
    title: String,
    primaryColor: Color,
    secondaryColor: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            color = primaryColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HomeCardWallpaperInfoLineSample(
            label = stringResource(R.string.home_manager_version),
            value = "v1.0.0",
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
        )
        HomeCardWallpaperInfoLineSample(
            label = stringResource(R.string.home_kernel),
            value = "5.10.0",
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
        )
    }
}

@Composable
private fun HomeCardWallpaperRebootMenuSample(
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
) {
    val options = listOf(
        R.string.reboot,
        R.string.reboot_soft,
        R.string.reboot_recovery,
        R.string.reboot_bootloader,
        R.string.reboot_download,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = Icons.Rounded.PowerSettingsNew,
                tint = accentColor,
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.home_card_reboot_menu),
                color = primaryColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        options.forEach { labelRes ->
            Text(
                text = stringResource(labelRes),
                color = secondaryColor,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeCardWallpaperInfoLineSample(
    label: String,
    value: String,
    primaryColor: Color,
    secondaryColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            color = secondaryColor,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = primaryColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeCardWallpaperPreviewTag(
    label: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.18f),
        contentColor = Color.White,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private fun homeCardWallpaperTargetIcon(
    target: HomeMetricCardWallpaperTarget,
): ImageVector = when (target) {
    HomeMetricCardWallpaperTarget.Lkm -> Icons.Rounded.CheckCircleOutline
    HomeMetricCardWallpaperTarget.Superuser -> Icons.Rounded.Shield
    HomeMetricCardWallpaperTarget.Module -> Icons.Rounded.Extension
    HomeMetricCardWallpaperTarget.StatusMonitor -> Icons.Rounded.Security
    HomeMetricCardWallpaperTarget.SystemInfo -> Icons.Rounded.Info
    HomeMetricCardWallpaperTarget.RebootMenu -> Icons.Rounded.PowerSettingsNew
}

@Composable
private fun HomeCardWallpaperButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
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
private fun HomeCardWallpaperPreviewDialog(
    show: Boolean,
    title: String,
    target: HomeMetricCardWallpaperTarget,
    bitmap: Bitmap?,
    videoUriString: String?,
    crop: CustomWallpaperCrop,
    aspectRatio: Float,
    onDismissRequest: () -> Unit,
) {
    if (!show) return

    when (LocalUiMode.current) {
        UiMode.Material -> AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(target.previewLabelRes)) },
            text = {
                HomeCardWallpaperLivePreviewFrame(
                    target = target,
                    title = title,
                    bitmap = bitmap,
                    videoUriString = videoUriString,
                    crop = crop,
                    aspectRatio = aspectRatio,
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )

        UiMode.Miuix -> OverlayDialog(
            show = true,
            title = stringResource(target.previewLabelRes),
            onDismissRequest = onDismissRequest,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeCardWallpaperLivePreviewFrame(
                        target = target,
                        title = title,
                        bitmap = bitmap,
                        videoUriString = videoUriString,
                        crop = crop,
                        aspectRatio = aspectRatio,
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
}

@Composable
private fun HomeCardWallpaperLivePreviewFrame(
    target: HomeMetricCardWallpaperTarget,
    title: String,
    bitmap: Bitmap?,
    videoUriString: String?,
    crop: CustomWallpaperCrop,
    aspectRatio: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        HomeMetricCardWallpaperBackground(
            bitmap = bitmap,
            videoUriString = videoUriString,
            videoCrop = crop,
        )
        if (bitmap == null && videoUriString.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.home_card_wallpaper_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            HomeCardWallpaperSampleContent(
                target = target,
                fallbackTitle = title,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

private fun homeCardWallpaperAspectRatio(
    target: HomeMetricCardWallpaperTarget,
    uiMode: UiMode,
): Float {
    return if (uiMode == UiMode.Miuix && target == HomeMetricCardWallpaperTarget.Lkm) {
        MIUIX_LKM_CARD_WALLPAPER_ASPECT_RATIO
    } else {
        target.aspectRatio
    }
}

private fun homeCardWallpaperSummaryRes(
    state: HomeMetricCardWallpaperState,
): Int {
    return when {
        state.hasSelectedVideoWallpaper -> R.string.home_card_wallpaper_video_selected
        state.hasSelectedWallpaper -> R.string.home_card_wallpaper_image_selected
        else -> R.string.home_card_wallpaper_empty
    }
}
