package me.weishu.kernelsu.ui.screen.home

import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.KernelVersion
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.CustomVideoBackground
import me.weishu.kernelsu.ui.component.ListPopupDefaults
import me.weishu.kernelsu.ui.component.rememberCustomVideoFrameBitmap
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassButton
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassSurface
import me.weishu.kernelsu.ui.component.liquid.isLiquidGlassTheme
import me.weishu.kernelsu.ui.component.liquid.liquidGlassMiuixCardColors
import me.weishu.kernelsu.ui.component.miuix.DropdownItem
import me.weishu.kernelsu.ui.component.rebootlistpopup.RebootListPopupMiuix
import me.weishu.kernelsu.ui.component.statustag.StatusTagMiuix
import me.weishu.kernelsu.ui.screen.settings.SettingsWallpaperCropDialog
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.theme.skrootproTopBarColors
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.releasePersistableVideoBackgroundReadPermission
import me.weishu.kernelsu.ui.util.releaseCustomImageReference
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import me.weishu.kernelsu.ui.util.sanitizeCustomWallpaperCrop
import me.weishu.kernelsu.ui.util.takePersistableImageReadPermission
import me.weishu.kernelsu.ui.util.takePersistableVideoBackgroundReadPermission
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomePagerMiuix(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
    installFeedbackActive: Boolean = false,
) {
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    val topBarColors = skrootproTopBarColors(barColor, colorScheme.onSurface)
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopBar(
                backdrop = backdrop,
                barColor = topBarColors.container,
                contentColor = topBarColors.content,
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatusCard(
                            state = state,
                            actions = actions,
                            installFeedbackActive = installFeedbackActive,
                        )
                        WarningSummaryCard(messages = homeWarningMessages(state))
                        InfoCard(systemInfo = state.systemInfo)
                        SecondaryLinksCard(
                            onOpenUrl = actions.onOpenUrl,
                            showSupport = state.showHomeSupportCard,
                            showLearn = state.showHomeLearnCard,
                        )
                    }
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    backdrop: LayerBackdrop?,
    barColor: Color,
    contentColor: Color,
) {
    BlurredBar(backdrop) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(barColor)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .height(56.dp)
                .padding(start = 24.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.app_name),
                color = contentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                RebootListPopupMiuix(tint = contentColor)
            }
        }
    }
}

@Composable
private fun StatusCard(
    state: HomeUiState,
    actions: HomeActions,
    installFeedbackActive: Boolean = false,
) {
    when {
        state.ksuVersion != null -> ActivatedStatusCard(state = state, actions = actions)
        state.kernelVersion.isGKI() -> InstallStatusCard(
            state = state,
            actions = actions,
            installFeedbackActive = installFeedbackActive
        )
        else -> UnsupportedStatusCard(state = state, actions = actions)
    }
}

@Composable
private fun ActivatedStatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val containerColor = when {
            isDynamicColor -> colorScheme.secondaryContainer
            isInDarkTheme() -> Color(0xFF1A3825)
            else -> Color(0xFFDFFAE4)
        }
        val accentColor = if (isDynamicColor) {
            colorScheme.primary
        } else {
            Color(0xFF1FAF55)
        }
        val workingMode = state.workingModeLabel
        val wallpaperState = rememberLkmCardWallpaperState(
            onWallpaperSelected = {}
        )
        val wallpaperBitmap = rememberLkmCardWallpaperBitmap(
            uriString = if (state.lkmMode == true) wallpaperState.uriString else null,
            crop = wallpaperState.crop,
        )
        val lkmVideoUriString = if (state.lkmMode == true) wallpaperState.videoUriString else null
        val hasLkmWallpaper = wallpaperBitmap != null || !lkmVideoUriString.isNullOrBlank()
        val primaryContentColor = if (hasLkmWallpaper) Color.White else colorScheme.onSurface
        val secondaryContentColor = if (hasLkmWallpaper) {
            Color.White.copy(alpha = 0.82f)
        } else {
            colorScheme.onSurfaceVariantSummary
        }
        val statusTagBackgroundColor = if (hasLkmWallpaper) {
            Color.White.copy(alpha = 0.18f)
        } else {
            accentColor.copy(alpha = 0.16f)
        }
        val statusTagContentColor = if (hasLkmWallpaper) Color.White else accentColor
        val iconBubbleColor = if (hasLkmWallpaper) {
            Color.White.copy(alpha = 0.20f)
        } else {
            accentColor.copy(alpha = 0.16f)
        }
        val iconTint = if (hasLkmWallpaper) Color.White else accentColor

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .homeLiquidGlassSurface(enabled = !hasLkmWallpaper),
            colors = homeLiquidGlassCardColors(
                color = containerColor,
                enabled = !hasLkmWallpaper,
            ),
            onClick = {
                if (!state.isLateLoadMode) {
                    actions.onInstallClick()
                }
            },
            showIndication = !state.isLateLoadMode,
            pressFeedbackType = PressFeedbackType.Tilt
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (state.lkmMode == true) {
                    LkmCardWallpaperBackground(
                        bitmap = wallpaperBitmap,
                        videoUriString = lkmVideoUriString,
                        videoCrop = wallpaperState.crop,
                    )
                }
                Icon(
                    modifier = Modifier
                        .size(148.dp)
                        .align(Alignment.BottomEnd)
                        .offset(24.dp, 28.dp),
                    imageVector = Icons.Rounded.CheckCircleOutline,
                    tint = iconTint.copy(alpha = if (hasLkmWallpaper) 0.18f else 0.22f),
                    contentDescription = null
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(iconBubbleColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Rounded.CheckCircleOutline,
                            tint = iconTint,
                            contentDescription = null
                        )
                    }
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.home_working),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (workingMode != null) {
                            StatusTagMiuix(
                                label = workingMode,
                                backgroundColor = statusTagBackgroundColor,
                                contentColor = statusTagContentColor
                            )
                        }
                        if (state.isSafeMode) {
                            StatusTagMiuix(
                                label = stringResource(id = R.string.safe_mode),
                                backgroundColor = colorScheme.errorContainer,
                                contentColor = colorScheme.onErrorContainer
                            )
                        }
                        if (state.isLateLoadMode) {
                            StatusTagMiuix(
                                label = stringResource(id = R.string.jailbreak_mode),
                                backgroundColor = colorScheme.errorContainer,
                                contentColor = colorScheme.onErrorContainer
                            )
                        }
                    }
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(
                            R.string.home_working_version,
                            "${state.ksuVersion}-${state.kernelUAPIVersion}"
                        ),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryContentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                target = HomeMetricCardWallpaperTarget.Superuser,
                title = stringResource(R.string.superuser),
                value = state.superuserCount.toString(),
                onClick = actions.onSuperuserClick
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                target = HomeMetricCardWallpaperTarget.Module,
                title = stringResource(R.string.module),
                value = state.moduleCount.toString(),
                onClick = actions.onModuleClick
            )
        }
    }
}

private data class LkmCardWallpaperState(
    val uriString: String?,
    val videoUriString: String?,
    val crop: CustomWallpaperCrop,
    val onPickWallpaper: () -> Unit,
    val onPickVideoWallpaper: () -> Unit,
    val onCropChange: (CustomWallpaperCrop) -> Unit,
    val onClearWallpaper: () -> Unit,
) {
    val hasSelectedWallpaper: Boolean
        get() = !uriString.isNullOrBlank()
    val hasSelectedVideoWallpaper: Boolean
        get() = !videoUriString.isNullOrBlank()
    val hasSelectedAnyWallpaper: Boolean
        get() = hasSelectedWallpaper || hasSelectedVideoWallpaper
}

@Composable
private fun rememberLkmCardWallpaperState(
    onWallpaperSelected: () -> Unit,
): LkmCardWallpaperState {
    val context = LocalContext.current
    val currentOnWallpaperSelected by rememberUpdatedState(onWallpaperSelected)
    val prefs = remember(context) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    var uriString by remember {
        mutableStateOf(prefs.getString(LKM_CARD_WALLPAPER_URI_KEY, null))
    }
    var videoUriString by remember {
        mutableStateOf(prefs.getString(LKM_CARD_WALLPAPER_VIDEO_URI_KEY, null))
    }
    var crop by remember {
        mutableStateOf(readLkmCardWallpaperCrop(prefs))
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val nextUriString = persistCustomImageReference(context, uri, LKM_CARD_WALLPAPER_URI_KEY)
            ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
        val previousUriString = uriString
        val defaultCrop = DEFAULT_CUSTOM_WALLPAPER_CROP
        if (previousUriString != nextUriString) {
            releaseCustomImageReference(context, previousUriString)
        }
        releasePersistableVideoBackgroundReadPermission(context, videoUriString)
        uriString = nextUriString
        videoUriString = null
        crop = defaultCrop
        prefs.edit(commit = true) {
            putString(LKM_CARD_WALLPAPER_URI_KEY, nextUriString)
            remove(LKM_CARD_WALLPAPER_VIDEO_URI_KEY)
            putLkmCardWallpaperCrop(defaultCrop)
        }
        currentOnWallpaperSelected()
    }
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val nextUriString = uri.toString()
        val previousUriString = uriString
        val previousVideoUriString = videoUriString
        takePersistableVideoBackgroundReadPermission(context, uri)
        releaseCustomImageReference(context, previousUriString)
        if (previousVideoUriString != nextUriString) {
            releasePersistableVideoBackgroundReadPermission(context, previousVideoUriString)
        }
        uriString = null
        videoUriString = nextUriString
        crop = DEFAULT_CUSTOM_WALLPAPER_CROP
        prefs.edit(commit = true) {
            remove(LKM_CARD_WALLPAPER_URI_KEY)
            putLkmCardWallpaperCrop(DEFAULT_CUSTOM_WALLPAPER_CROP)
            putString(LKM_CARD_WALLPAPER_VIDEO_URI_KEY, nextUriString)
        }
        currentOnWallpaperSelected()
    }

    return remember(uriString, videoUriString, crop, launcher, videoLauncher, prefs, context) {
        LkmCardWallpaperState(
            uriString = uriString,
            videoUriString = videoUriString,
            crop = crop,
            onPickWallpaper = {
                launcher.launch(arrayOf("image/*"))
            },
            onPickVideoWallpaper = {
                videoLauncher.launch(arrayOf("video/*"))
            },
            onCropChange = { nextCrop ->
                val safeCrop = sanitizeCustomWallpaperCrop(nextCrop)
                crop = safeCrop
                prefs.edit(commit = true) {
                    putLkmCardWallpaperCrop(safeCrop)
                }
            },
            onClearWallpaper = {
                releaseCustomImageReference(context, uriString)
                releasePersistableVideoBackgroundReadPermission(context, videoUriString)
                uriString = null
                videoUriString = null
                crop = DEFAULT_CUSTOM_WALLPAPER_CROP
                prefs.edit(commit = true) {
                    remove(LKM_CARD_WALLPAPER_URI_KEY)
                    remove(LKM_CARD_WALLPAPER_VIDEO_URI_KEY)
                    removeLkmCardWallpaperCrop()
                }
            },
        )
    }
}

@Composable
private fun rememberLkmCardWallpaperBitmap(
    uriString: String?,
    crop: CustomWallpaperCrop,
): Bitmap? {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, uriString, crop) {
        value = if (uriString == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                loadCustomImageBitmap(
                    context = context,
                    uriString = uriString,
                    maxSide = LKM_CARD_WALLPAPER_MAX_SIDE,
                    crop = crop,
                )
            }
        }
    }
    return bitmapState.value
}

private fun readLkmCardWallpaperCrop(prefs: android.content.SharedPreferences): CustomWallpaperCrop {
    return sanitizeCustomWallpaperCrop(
        CustomWallpaperCrop(
            left = prefs.getFloat(
                LKM_CARD_WALLPAPER_CROP_LEFT_KEY,
                DEFAULT_CUSTOM_WALLPAPER_CROP.left
            ),
            top = prefs.getFloat(
                LKM_CARD_WALLPAPER_CROP_TOP_KEY,
                DEFAULT_CUSTOM_WALLPAPER_CROP.top
            ),
            right = prefs.getFloat(
                LKM_CARD_WALLPAPER_CROP_RIGHT_KEY,
                DEFAULT_CUSTOM_WALLPAPER_CROP.right
            ),
            bottom = prefs.getFloat(
                LKM_CARD_WALLPAPER_CROP_BOTTOM_KEY,
                DEFAULT_CUSTOM_WALLPAPER_CROP.bottom
            ),
        )
    )
}

private fun android.content.SharedPreferences.Editor.putLkmCardWallpaperCrop(
    crop: CustomWallpaperCrop
) {
    val safeCrop = sanitizeCustomWallpaperCrop(crop)
    putFloat(LKM_CARD_WALLPAPER_CROP_LEFT_KEY, safeCrop.left)
    putFloat(LKM_CARD_WALLPAPER_CROP_TOP_KEY, safeCrop.top)
    putFloat(LKM_CARD_WALLPAPER_CROP_RIGHT_KEY, safeCrop.right)
    putFloat(LKM_CARD_WALLPAPER_CROP_BOTTOM_KEY, safeCrop.bottom)
}

private fun android.content.SharedPreferences.Editor.removeLkmCardWallpaperCrop() {
    remove(LKM_CARD_WALLPAPER_CROP_LEFT_KEY)
    remove(LKM_CARD_WALLPAPER_CROP_TOP_KEY)
    remove(LKM_CARD_WALLPAPER_CROP_RIGHT_KEY)
    remove(LKM_CARD_WALLPAPER_CROP_BOTTOM_KEY)
}

@Composable
private fun BoxScope.LkmCardWallpaperBackground(
    bitmap: Bitmap?,
    videoUriString: String?,
    videoCrop: CustomWallpaperCrop = DEFAULT_CUSTOM_WALLPAPER_CROP,
) {
    if (bitmap == null && videoUriString.isNullOrBlank()) return

    if (!videoUriString.isNullOrBlank()) {
        CustomVideoBackground(
            uriString = videoUriString,
            drawOverlay = false,
            crop = videoCrop,
            touchPassthrough = true,
            modifier = Modifier.matchParentSize(),
        )
    } else if (bitmap != null) {
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        Image(
            modifier = Modifier.matchParentSize(),
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = if (isInDarkTheme()) 0.50f else 0.42f))
    )
}

@Composable
private fun LkmCardWallpaperActions(
    hasWallpaper: Boolean,
    showCrop: Boolean,
    showClear: Boolean,
    onPickWallpaper: () -> Unit,
    onPickVideoWallpaper: () -> Unit,
    onEditCrop: () -> Unit,
    onPreviewWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (hasWallpaper) {
        Color.Black.copy(alpha = 0.28f)
    } else {
        colorScheme.surfaceContainerHigh.copy(alpha = 0.76f)
    }
    val contentColor = if (hasWallpaper) {
        Color.White
    } else {
        colorScheme.onSurfaceVariantActions
    }
    val showTopPopup = remember { mutableStateOf(false) }
    val menuActions = buildList<Pair<String, () -> Unit>> {
        add(stringResource(R.string.home_lkm_wallpaper_pick) to onPickWallpaper)
        add(stringResource(R.string.home_lkm_video_wallpaper_pick) to onPickVideoWallpaper)
        if (showCrop) {
            add(stringResource(R.string.home_lkm_wallpaper_crop) to onEditCrop)
        }
        if (hasWallpaper) {
            add(stringResource(R.string.home_lkm_wallpaper_preview) to onPreviewWallpaper)
        }
        if (showClear) {
            add(stringResource(R.string.home_lkm_wallpaper_clear) to onClearWallpaper)
        }
    }

    IconButton(
        modifier = modifier.background(containerColor, RoundedCornerShape(999.dp)),
        minHeight = 36.dp,
        minWidth = 36.dp,
        onClick = { showTopPopup.value = true },
        holdDownState = showTopPopup.value
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(R.string.home_lkm_wallpaper_pick),
            tint = contentColor
        )
    }
    OverlayListPopup(
        show = showTopPopup.value,
        popupPositionProvider = ListPopupDefaults.MenuPositionProvider,
        alignment = PopupPositionProvider.Align.TopEnd,
        onDismissRequest = { showTopPopup.value = false },
        content = {
            ListPopupColumn {
                menuActions.forEachIndexed { index, action ->
                    DropdownItem(
                        text = action.first,
                        optionSize = menuActions.size,
                        index = index,
                        onSelectedIndexChange = { selectedIndex ->
                            showTopPopup.value = false
                            menuActions[selectedIndex].second()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun LkmCardWallpaperPreviewDialog(
    show: Boolean,
    bitmap: Bitmap?,
    videoUriString: String?,
    videoCrop: CustomWallpaperCrop,
    onDismissRequest: () -> Unit,
) {
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    OverlayDialog(
        show = show && (imageBitmap != null || !videoUriString.isNullOrBlank()),
        title = stringResource(R.string.home_lkm_wallpaper_preview),
        onDismissRequest = onDismissRequest,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(LKM_CARD_WALLPAPER_ASPECT_RATIO)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    LkmCardWallpaperBackground(
                        bitmap = bitmap,
                        videoUriString = videoUriString,
                        videoCrop = videoCrop,
                    )
                    Icon(
                        modifier = Modifier
                            .size(112.dp)
                            .align(Alignment.BottomEnd)
                            .offset(18.dp, 26.dp),
                        imageVector = Icons.Rounded.CheckCircleOutline,
                        tint = Color.White.copy(alpha = 0.18f),
                        contentDescription = null
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.White.copy(alpha = 0.20f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Rounded.CheckCircleOutline,
                                tint = Color.White,
                                contentDescription = null
                            )
                        }
                        Text(
                            text = stringResource(R.string.home_working),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        StatusTagMiuix(
                            label = "LKM",
                            backgroundColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    }
                }
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .globalLiquidGlassButton(),
                    text = stringResource(android.R.string.ok),
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    )
}

@Composable
private fun InstallStatusCard(
    state: HomeUiState,
    actions: HomeActions,
    installFeedbackActive: Boolean = false,
) {
    val containerColor = when {
        isDynamicColor -> colorScheme.tertiaryContainer
        isInDarkTheme() -> Color(0xFF3A2A10)
        else -> Color(0xFFFFF1D6)
    }
    val accentColor = if (isDynamicColor) {
        colorScheme.onTertiaryContainer
    } else if (isInDarkTheme()) {
        Color(0xFFFFB74D)
    } else {
        Color(0xFF7A4300)
    }
    val actionContentColor = containerColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .homeLiquidGlassSurface(),
        colors = liquidGlassMiuixCardColors(containerColor),
        onClick = {
            if (!state.isLateLoadMode && !installFeedbackActive) {
                actions.onInstallClick()
            }
        },
        showIndication = !state.isLateLoadMode && !installFeedbackActive,
        pressFeedbackType = PressFeedbackType.Tilt
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                modifier = Modifier
                    .size(138.dp)
                    .align(Alignment.BottomEnd)
                    .offset(42.dp, 44.dp),
                imageVector = Icons.Rounded.PowerSettingsNew,
                tint = accentColor.copy(alpha = 0.09f),
                contentDescription = null
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusIconBubbleMiuix(
                    icon = Icons.Rounded.PowerSettingsNew,
                    iconContentDescription = stringResource(R.string.home_not_installed),
                    accentColor = accentColor,
                    pulse = !installFeedbackActive,
                    loading = installFeedbackActive
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.home_not_installed),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.home_click_to_install),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                InstallActionRowMiuix(
                    installFeedbackActive = installFeedbackActive,
                    accentColor = accentColor,
                    actionContentColor = actionContentColor,
                    onInstallClick = actions.onInstallClick,
                    showJailbreak = true,
                    onJailbreakClick = actions.onJailbreakClick
                )
            }
        }
    }
}

@Composable
private fun StatusIconBubbleMiuix(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconContentDescription: String,
    accentColor: Color,
    pulse: Boolean,
    loading: Boolean,
) {
    val transition = rememberInfiniteTransition(label = "miuix_status_icon_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "miuix_status_icon_scale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "miuix_status_icon_alpha"
    )

    Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        if (pulse) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .background(accentColor.copy(alpha = pulseAlpha), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(accentColor.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    progress = 0.64f,
                    size = 23.dp,
                    strokeWidth = 2.2.dp
                )
            } else {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = icon,
                    tint = accentColor,
                    contentDescription = iconContentDescription
                )
            }
        }
    }
}

@Composable
private fun InstallActionRowMiuix(
    installFeedbackActive: Boolean,
    accentColor: Color,
    actionContentColor: Color,
    onInstallClick: () -> Unit,
    showJailbreak: Boolean,
    onJailbreakClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "miuix_install_progress")
    val progress by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "miuix_install_progress_value"
    )

    if (installFeedbackActive) {
        Row(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                progress = progress,
                size = 18.dp,
                strokeWidth = 2.dp
            )
            Text(
                text = stringResource(R.string.home_install_preparing),
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                text = stringResource(R.string.install),
                onClick = onInstallClick,
                colors = ButtonDefaults.textButtonColors(
                    color = accentColor,
                    textColor = actionContentColor
                )
            )
            if (showJailbreak) {
                TextButton(
                    text = stringResource(R.string.home_jailbreak),
                    onClick = onJailbreakClick,
                    colors = ButtonDefaults.textButtonColors(
                        color = accentColor,
                        textColor = actionContentColor
                    )
                )
            }
        }
    }
}

@Composable
private fun UnsupportedStatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .homeLiquidGlassSurface(),
        colors = liquidGlassMiuixCardColors(
            if (isDynamicColor) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainer
        ),
        onClick = {
            if (!state.isLateLoadMode) {
                actions.onInstallClick()
            }
        },
        showIndication = !state.isLateLoadMode,
        pressFeedbackType = PressFeedbackType.Tilt
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(colorScheme.errorContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Rounded.ErrorOutline,
                    tint = colorScheme.onErrorContainer,
                    contentDescription = stringResource(R.string.home_unsupported)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_unsupported),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.home_unsupported_reason),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    target: HomeMetricCardWallpaperTarget,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wallpaperState = rememberHomeMetricCardWallpaperState(
        target = target,
        onWallpaperSelected = {}
    )
    val wallpaperBitmap = rememberHomeMetricCardWallpaperBitmap(
        uriString = wallpaperState.uriString,
        crop = wallpaperState.crop,
    )
    val videoUriString = wallpaperState.videoUriString
    val hasWallpaper = wallpaperBitmap != null || !videoUriString.isNullOrBlank()
    val contentColor = if (hasWallpaper) Color.White else colorScheme.onSurface
    val summaryColor = if (hasWallpaper) {
        Color.White.copy(alpha = 0.82f)
    } else {
        colorScheme.onSurfaceVariantSummary
    }

    Card(
        modifier = modifier.homeLiquidGlassSurface(enabled = !hasWallpaper),
        colors = homeLiquidGlassCardColors(enabled = !hasWallpaper),
        insideMargin = PaddingValues(0.dp),
        onClick = onClick,
        showIndication = true,
        pressFeedbackType = PressFeedbackType.Tilt
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HomeMetricCardWallpaperBackground(
                bitmap = wallpaperBitmap,
                videoUriString = videoUriString,
                videoCrop = wallpaperState.crop,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = summaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetricCardWallpaperActions(
    target: HomeMetricCardWallpaperTarget,
    hasWallpaper: Boolean,
    showClear: Boolean,
    onPickWallpaper: () -> Unit,
    onEditCrop: () -> Unit,
    onPreviewWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (hasWallpaper) {
        Color.Black.copy(alpha = 0.28f)
    } else {
        colorScheme.surfaceContainerHigh.copy(alpha = 0.76f)
    }
    val contentColor = if (hasWallpaper) {
        Color.White
    } else {
        colorScheme.onSurfaceVariantActions
    }
    val showTopPopup = remember { mutableStateOf(false) }
    val menuActions = buildList<Pair<String, () -> Unit>> {
        add(stringResource(target.pickLabelRes) to onPickWallpaper)
        if (showClear) {
            add(stringResource(target.cropLabelRes) to onEditCrop)
        }
        if (hasWallpaper) {
            add(stringResource(target.previewLabelRes) to onPreviewWallpaper)
        }
        if (showClear) {
            add(stringResource(target.clearLabelRes) to onClearWallpaper)
        }
    }

    IconButton(
        modifier = modifier.background(containerColor, RoundedCornerShape(999.dp)),
        minHeight = 32.dp,
        minWidth = 32.dp,
        onClick = { showTopPopup.value = true },
        holdDownState = showTopPopup.value
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(target.pickLabelRes),
            tint = contentColor
        )
    }
    OverlayListPopup(
        show = showTopPopup.value,
        popupPositionProvider = ListPopupDefaults.MenuPositionProvider,
        alignment = PopupPositionProvider.Align.TopEnd,
        onDismissRequest = { showTopPopup.value = false },
        content = {
            ListPopupColumn {
                menuActions.forEachIndexed { index, action ->
                    DropdownItem(
                        text = action.first,
                        optionSize = menuActions.size,
                        index = index,
                        onSelectedIndexChange = { selectedIndex ->
                            showTopPopup.value = false
                            menuActions[selectedIndex].second()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun MetricCardWallpaperPreviewDialog(
    show: Boolean,
    target: HomeMetricCardWallpaperTarget,
    bitmap: Bitmap?,
    title: String,
    value: String,
    onDismissRequest: () -> Unit,
) {
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    OverlayDialog(
        show = show && imageBitmap != null,
        title = stringResource(target.previewLabelRes),
        onDismissRequest = onDismissRequest,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(target.aspectRatio)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    if (imageBitmap != null) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = imageBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = if (isInDarkTheme()) 0.52f else 0.44f))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = value,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(android.R.string.ok),
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    )
}

@Composable
private fun WarningSummaryCard(
    messages: List<String>,
) {
    if (messages.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val visibleMessages = if (expanded) messages else messages.take(1)
    val hiddenCount = messages.size - visibleMessages.size
    val warningContainer = when {
        isDynamicColor -> colorScheme.errorContainer
        isInDarkTheme() -> Color(0XFF310808)
        else -> Color(0xFFF8E2E2)
    }
    val warningContent = if (isDynamicColor) colorScheme.onErrorContainer else Color(0xFFF72727)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .homeLiquidGlassSurface(
                surfaceColor = Color(0xFFFFF2F2),
                surfaceAlpha = 0.66f,
            ),
        colors = liquidGlassMiuixCardColors(warningContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(warningContent.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = Icons.Rounded.WarningAmber,
                        tint = warningContent,
                        contentDescription = null
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.home_warning_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = warningContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = visibleMessages.first(),
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = warningContent,
                        maxLines = if (expanded) 3 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            visibleMessages.drop(1).forEach { message ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = message,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = warningContent
                )
            }
            if (messages.size > 1) {
                TextButton(
                    text = if (expanded) {
                        stringResource(R.string.home_warning_show_less)
                    } else {
                        stringResource(R.string.home_warning_more, hiddenCount)
                    },
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.textButtonColors(
                        color = warningContent.copy(alpha = 0.12f),
                        textColor = warningContent
                    )
                )
            }
        }
    }
}

@Composable
private fun SecondaryLinksCard(
    onOpenUrl: (String) -> Unit,
    showSupport: Boolean = true,
    showLearn: Boolean = true,
) {
    if (!showSupport && !showLearn) return

    val learnUrl = stringResource(R.string.home_learn_kernelsu_url)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .homeLiquidGlassSurface(),
        colors = liquidGlassMiuixCardColors(),
    ) {
        if (showSupport) {
            BasicComponent(
                title = stringResource(R.string.home_support_title),
                summary = stringResource(R.string.home_support_content),
                startAction = {
                    Icon(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(22.dp),
                        imageVector = Icons.Rounded.FavoriteBorder,
                        tint = colorScheme.primary,
                        contentDescription = null
                    )
                },
                endActions = {
                    Icon(
                        imageVector = MiuixIcons.Basic.ArrowRight,
                        tint = colorScheme.onSurfaceVariantActions,
                        contentDescription = null
                    )
                },
                onClick = { onOpenUrl("https://patreon.com/weishu") },
                insideMargin = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
            )
        }
        if (showSupport && showLearn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(start = 52.dp, end = 18.dp)
                    .background(colorScheme.outline.copy(alpha = 0.2f))
            )
        }
        if (showLearn) {
            BasicComponent(
                title = stringResource(R.string.home_learn_kernelsu),
                summary = stringResource(R.string.home_click_to_learn_kernelsu),
                startAction = {
                    Icon(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(22.dp),
                        imageVector = Icons.Rounded.Info,
                        tint = colorScheme.primary,
                        contentDescription = null
                    )
                },
                endActions = {
                    Icon(
                        imageVector = MiuixIcons.Basic.ArrowRight,
                        tint = colorScheme.onSurfaceVariantActions,
                        contentDescription = null
                    )
                },
                onClick = { onOpenUrl(learnUrl) },
                insideMargin = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
            )
        }
    }
}

@Composable
private fun InfoCard(systemInfo: SystemInfo) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copiedText = stringResource(R.string.home_copied_to_clipboard)
    var fingerprintExpanded by remember { mutableStateOf(false) }
    val statusWallpaperState = rememberHomeMetricCardWallpaperState(
        target = HomeMetricCardWallpaperTarget.StatusMonitor,
        onWallpaperSelected = {}
    )
    val statusWallpaperBitmap = rememberHomeMetricCardWallpaperBitmap(
        uriString = statusWallpaperState.uriString,
        crop = statusWallpaperState.crop,
    )
    val systemInfoWallpaperState = rememberHomeMetricCardWallpaperState(
        target = HomeMetricCardWallpaperTarget.SystemInfo,
        onWallpaperSelected = {}
    )
    val systemInfoWallpaperBitmap = rememberHomeMetricCardWallpaperBitmap(
        uriString = systemInfoWallpaperState.uriString,
        crop = systemInfoWallpaperState.crop,
    )

    fun copyValue(label: String, content: String) {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, content)))
            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier.homeLiquidGlassSurface(),
        colors = liquidGlassMiuixCardColors(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val selinuxDisplay = when (systemInfo.selinuxStatus) {
                "Enforcing" -> stringResource(R.string.selinux_status_enforcing)
                "Permissive" -> stringResource(R.string.selinux_status_permissive)
                "Disabled" -> stringResource(R.string.selinux_status_disabled)
                else -> stringResource(R.string.selinux_status_unknown)
            }
            val seccompDisplay = when (systemInfo.seccompStatus) {
                -1 -> stringResource(R.string.seccomp_status_not_supported)
                0 -> stringResource(R.string.seccomp_status_disabled)
                1 -> stringResource(R.string.seccomp_status_strict)
                2 -> stringResource(R.string.seccomp_status_filter)
                else -> stringResource(R.string.seccomp_status_unknown)
            }
            StatusMonitorPanelMiuix(
                selinuxLabel = stringResource(R.string.home_selinux_status),
                selinuxValue = selinuxDisplay,
                selinuxDotColor = selinuxDotColorMiuix(systemInfo.selinuxStatus),
                seccompLabel = stringResource(R.string.home_seccomp_status),
                seccompValue = seccompDisplay,
                seccompDotColor = seccompDotColorMiuix(systemInfo.seccompStatus),
                wallpaperState = statusWallpaperState,
                wallpaperBitmap = statusWallpaperBitmap,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colorScheme.outline.copy(alpha = 0.28f))
            )
            SystemInfoPanelMiuix(
                systemInfo = systemInfo,
                fingerprintExpanded = fingerprintExpanded,
                onFingerprintExpandedChange = { fingerprintExpanded = it },
                wallpaperState = systemInfoWallpaperState,
                wallpaperBitmap = systemInfoWallpaperBitmap,
                onCopyValue = ::copyValue,
            )
        }
    }
}

@Composable
private fun StatusMonitorPanelMiuix(
    selinuxLabel: String,
    selinuxValue: String,
    selinuxDotColor: Color,
    seccompLabel: String,
    seccompValue: String,
    seccompDotColor: Color,
    wallpaperState: HomeMetricCardWallpaperState,
    wallpaperBitmap: Bitmap?,
) {
    val videoUriString = wallpaperState.videoUriString
    val hasWallpaper = wallpaperBitmap != null || !videoUriString.isNullOrBlank()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasWallpaper) {
                    Modifier.background(Color.Transparent, RoundedCornerShape(14.dp))
                } else if (isLiquidGlassTheme()) {
                    Modifier.globalLiquidGlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        surfaceAlpha = 0.42f,
                        blurRadius = 8.dp,
                        refractionHeight = 10.dp,
                        refractionAmount = 7.dp,
                        strokeAlpha = 0.48f,
                    )
                } else {
                    Modifier.background(
                        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            )
            .clip(RoundedCornerShape(14.dp)),
    ) {
        HomeMetricCardWallpaperBackground(
            bitmap = wallpaperBitmap,
            videoUriString = videoUriString,
            videoCrop = wallpaperState.crop,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusMonitorLineMiuix(
                icon = Icons.Rounded.Security,
                label = selinuxLabel,
                value = selinuxValue,
                dotColor = selinuxDotColor,
                backgroundColor = if (hasWallpaper) Color.White.copy(alpha = 0.16f) else statusMonitorSurfaceColorMiuix(selinuxDotColor),
                hasWallpaper = hasWallpaper,
            )
            StatusMonitorLineMiuix(
                icon = Icons.Rounded.Lock,
                label = seccompLabel,
                value = seccompValue,
                dotColor = seccompDotColor,
                backgroundColor = if (hasWallpaper) Color.White.copy(alpha = 0.16f) else statusMonitorSurfaceColorMiuix(seccompDotColor),
                hasWallpaper = hasWallpaper,
            )
        }
    }
}

@Composable
private fun SystemInfoPanelMiuix(
    systemInfo: SystemInfo,
    fingerprintExpanded: Boolean,
    onFingerprintExpandedChange: (Boolean) -> Unit,
    wallpaperState: HomeMetricCardWallpaperState,
    wallpaperBitmap: Bitmap?,
    onCopyValue: (String, String) -> Unit,
) {
    val videoUriString = wallpaperState.videoUriString
    val hasWallpaper = wallpaperBitmap != null || !videoUriString.isNullOrBlank()
    val rowColor = if (hasWallpaper) Color.White else colorScheme.onSurface
    val labelColor = if (hasWallpaper) {
        Color.White.copy(alpha = 0.72f)
    } else {
        colorScheme.onSurfaceVariantSummary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (hasWallpaper) Color.Transparent else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clip(RoundedCornerShape(14.dp)),
    ) {
        HomeMetricCardWallpaperBackground(
            bitmap = wallpaperBitmap,
            videoUriString = videoUriString,
            videoCrop = wallpaperState.crop,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InfoRowMiuix(
                label = stringResource(R.string.home_manager_version),
                value = systemInfo.managerVersion,
                onCopy = { onCopyValue("manager_version", systemInfo.managerVersion) },
                labelColor = labelColor,
                valueColor = rowColor,
            )
            InfoRowMiuix(
                label = stringResource(R.string.home_device_model),
                value = systemInfo.deviceModel,
                onCopy = { onCopyValue("device_model", systemInfo.deviceModel) },
                labelColor = labelColor,
                valueColor = rowColor,
            )
            InfoRowMiuix(
                label = stringResource(R.string.home_kernel),
                value = systemInfo.kernelVersion,
                maxLines = 3,
                onCopy = { onCopyValue("kernel_version", systemInfo.kernelVersion) },
                labelColor = labelColor,
                valueColor = rowColor,
            )
            InfoRowMiuix(
                label = stringResource(R.string.home_fingerprint),
                value = systemInfo.fingerprint,
                displayValue = compactFingerprint(systemInfo.fingerprint, fingerprintExpanded),
                maxLines = if (fingerprintExpanded) 4 else 1,
                expanded = fingerprintExpanded,
                onExpandToggle = { onFingerprintExpandedChange(!fingerprintExpanded) },
                onCopy = { onCopyValue("fingerprint", systemInfo.fingerprint) },
                labelColor = labelColor,
                valueColor = rowColor,
            )
        }
    }
}

@Composable
private fun HomeWallpaperCropDialog(
    show: Boolean,
    target: HomeMetricCardWallpaperTarget,
    uriString: String?,
    crop: CustomWallpaperCrop,
    onCropChange: (CustomWallpaperCrop) -> Unit,
    onDismissRequest: () -> Unit,
) {
    SettingsWallpaperCropDialog(
        show = show,
        uriString = uriString,
        crop = crop,
        onCropChange = onCropChange,
        onDismissRequest = onDismissRequest,
        title = stringResource(target.cropLabelRes),
        editorAspectRatio = target.aspectRatio,
        cropAspectRatio = target.aspectRatio,
    )
}

@Composable
private fun HomeWallpaperActionsMiuix(
    target: HomeMetricCardWallpaperTarget,
    hasWallpaper: Boolean,
    showClear: Boolean,
    onPickWallpaper: () -> Unit,
    onEditCrop: () -> Unit,
    onPreviewWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MetricCardWallpaperActions(
        modifier = modifier,
        target = target,
        hasWallpaper = hasWallpaper,
        showClear = showClear,
        onPickWallpaper = onPickWallpaper,
        onEditCrop = onEditCrop,
        onPreviewWallpaper = onPreviewWallpaper,
        onClearWallpaper = onClearWallpaper,
    )
}

@Composable
private fun StatusMonitorWallpaperPreviewDialogMiuix(
    show: Boolean,
    bitmap: Bitmap?,
    selinuxLabel: String,
    selinuxValue: String,
    selinuxDotColor: Color,
    seccompLabel: String,
    seccompValue: String,
    seccompDotColor: Color,
    onDismissRequest: () -> Unit,
) {
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    OverlayDialog(
        show = show && imageBitmap != null,
        title = stringResource(HomeMetricCardWallpaperTarget.StatusMonitor.previewLabelRes),
        onDismissRequest = onDismissRequest,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(HomeMetricCardWallpaperTarget.StatusMonitor.aspectRatio)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    if (imageBitmap != null) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = imageBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = if (isInDarkTheme()) 0.52f else 0.44f))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusMonitorLineMiuix(
                            icon = Icons.Rounded.Security,
                            label = selinuxLabel,
                            value = selinuxValue,
                            dotColor = selinuxDotColor,
                            backgroundColor = Color.White.copy(alpha = 0.16f),
                            hasWallpaper = true,
                        )
                        StatusMonitorLineMiuix(
                            icon = Icons.Rounded.Lock,
                            label = seccompLabel,
                            value = seccompValue,
                            dotColor = seccompDotColor,
                            backgroundColor = Color.White.copy(alpha = 0.16f),
                            hasWallpaper = true,
                        )
                    }
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(android.R.string.ok),
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    )
}

@Composable
private fun SystemInfoWallpaperPreviewDialogMiuix(
    show: Boolean,
    bitmap: Bitmap?,
    systemInfo: SystemInfo,
    fingerprintExpanded: Boolean,
    onDismissRequest: () -> Unit,
) {
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    OverlayDialog(
        show = show && imageBitmap != null,
        title = stringResource(HomeMetricCardWallpaperTarget.SystemInfo.previewLabelRes),
        onDismissRequest = onDismissRequest,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(HomeMetricCardWallpaperTarget.SystemInfo.aspectRatio)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    if (imageBitmap != null) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = imageBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = if (isInDarkTheme()) 0.52f else 0.44f))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SystemInfoPreviewLineMiuix(
                            label = stringResource(R.string.home_manager_version),
                            value = systemInfo.managerVersion,
                        )
                        SystemInfoPreviewLineMiuix(
                            label = stringResource(R.string.home_device_model),
                            value = systemInfo.deviceModel,
                        )
                        SystemInfoPreviewLineMiuix(
                            label = stringResource(R.string.home_kernel),
                            value = systemInfo.kernelVersion,
                            maxLines = 3,
                        )
                        SystemInfoPreviewLineMiuix(
                            label = stringResource(R.string.home_fingerprint),
                            value = compactFingerprint(systemInfo.fingerprint, fingerprintExpanded),
                            maxLines = if (fingerprintExpanded) 4 else 1,
                        )
                    }
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(android.R.string.ok),
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    )
}

@Composable
private fun SystemInfoPreviewLineMiuix(
    label: String,
    value: String,
    maxLines: Int = 2,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusMonitorLineMiuix(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    dotColor: Color,
    backgroundColor: Color,
    hasWallpaper: Boolean = false,
) {
    val labelColor = if (hasWallpaper) Color.White.copy(alpha = 0.72f) else colorScheme.onSurfaceVariantSummary
    val valueColor = if (hasWallpaper) Color.White else colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = icon,
            tint = dotColor,
            contentDescription = null
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(dotColor, CircleShape)
        )
    }
}

@Composable
private fun InfoRowMiuix(
    label: String,
    value: String,
    onCopy: () -> Unit,
    displayValue: String = value,
    maxLines: Int = 2,
    expanded: Boolean? = null,
    onExpandToggle: (() -> Unit)? = null,
    labelColor: Color = colorScheme.onSurfaceVariantSummary,
    valueColor: Color = colorScheme.onSurface,
    actionColor: Color = colorScheme.onSurfaceVariantActions,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val contentModifier = if (onExpandToggle != null) {
        Modifier.clickable(onClick = onExpandToggle)
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(contentModifier)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    modifier = if (expanded != null) {
                        Modifier.weight(1f, fill = false)
                    } else {
                        Modifier.fillMaxWidth()
                    },
                    text = displayValue,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis
                )
                if (expanded != null && onExpandToggle != null) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = if (expanded) {
                            Icons.Rounded.ExpandLess
                        } else {
                            Icons.Rounded.ExpandMore
                        },
                        contentDescription = stringResource(
                            if (expanded) R.string.home_collapse_fingerprint else R.string.home_expand_fingerprint
                        ),
                        tint = actionColor
                    )
                }
            }
        }
        IconButton(
            minHeight = 36.dp,
            minWidth = 36.dp,
            onClick = onCopy
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = stringResource(R.string.home_copy_value),
                tint = actionColor
            )
        }
        trailingAction?.invoke()
    }
}

@Composable
private fun statusMonitorSurfaceColorMiuix(dotColor: Color): Color {
    val alpha = if (isInDarkTheme()) 0.18f else 0.10f
    return dotColor.copy(alpha = alpha)
}

@Composable
private fun selinuxDotColorMiuix(status: String): Color {
    return when (status) {
        "Enforcing" -> Color(0xFF2E7D32)
        "Permissive" -> colorScheme.onTertiaryContainer
        "Disabled" -> colorScheme.error
        else -> colorScheme.outline
    }
}

@Composable
private fun seccompDotColorMiuix(status: Int): Color {
    return when (status) {
        1 -> Color(0xFF2E7D32)
        2 -> Color(0xFF1976D2)
        0 -> colorScheme.error
        else -> colorScheme.outline
    }
}

@Composable
private fun Modifier.homeLiquidGlassSurface(
    enabled: Boolean = true,
    surfaceColor: Color = Color.White,
    surfaceAlpha: Float = 0.58f,
): Modifier {
    if (!enabled) return this
    return globalLiquidGlassSurface(
        shape = RoundedCornerShape(18.dp),
        surfaceColor = surfaceColor,
        surfaceAlpha = surfaceAlpha,
        blurRadius = 10.dp,
        refractionHeight = 14.dp,
        refractionAmount = 9.dp,
        strokeAlpha = 0.66f,
    )
}

@Composable
private fun homeLiquidGlassCardColors(
    color: Color = colorScheme.surfaceContainer,
    enabled: Boolean = true,
) = if (enabled) {
    liquidGlassMiuixCardColors(color)
} else {
    CardDefaults.defaultColors(color = color)
}

private fun compactFingerprint(fingerprint: String, expanded: Boolean): String {
    return if (expanded || fingerprint.length <= 10) {
        fingerprint
    } else {
        "${fingerprint.take(10)}..."
    }
}

private const val LKM_CARD_WALLPAPER_URI_KEY = "home_lkm_card_wallpaper_uri"
private const val LKM_CARD_WALLPAPER_VIDEO_URI_KEY = "home_lkm_card_wallpaper_video_uri"
private const val LKM_CARD_WALLPAPER_CROP_LEFT_KEY = "home_lkm_card_wallpaper_crop_left"
private const val LKM_CARD_WALLPAPER_CROP_TOP_KEY = "home_lkm_card_wallpaper_crop_top"
private const val LKM_CARD_WALLPAPER_CROP_RIGHT_KEY = "home_lkm_card_wallpaper_crop_right"
private const val LKM_CARD_WALLPAPER_CROP_BOTTOM_KEY = "home_lkm_card_wallpaper_crop_bottom"
private const val LKM_CARD_WALLPAPER_MAX_SIDE = 1600
private const val LKM_CARD_WALLPAPER_ASPECT_RATIO = 1.86f

@Preview(name = "Activated")
@Composable
private fun StatusCardActivatedPreview() {
    StatusCard(
        state = previewHomeScreenState(ksuVersion = 12345, lkmMode = true, superuserCount = 5, moduleCount = 10),
        actions = HomeActions({}, {}, {}, {})
    )
}

@Preview(name = "Not Activated")
@Composable
private fun StatusCardNotActivatedPreview() {
    StatusCard(state = previewHomeScreenState(ksuVersion = null, lkmMode = null), actions = HomeActions({}, {}, {}, {}))
}

@Preview(name = "Permissive")
@Composable
private fun StatusCardPermissivePreview() {
    StatusCard(
        state = previewHomeScreenState(ksuVersion = null, lkmMode = null, selinuxStatus = "Permissive"),
        actions = HomeActions({}, {}, {}, {})
    )
}

@Preview(name = "Jailbreak")
@Composable
private fun StatusCardJailbreakPreview() {
    StatusCard(
        state = previewHomeScreenState(ksuVersion = 12345, lkmMode = true, isLateLoadMode = true, superuserCount = 5, moduleCount = 10),
        actions = HomeActions({}, {}, {}, {})
    )
}

private val previewSystemInfo = SystemInfo(
    kernelVersion = "6.12.23-android16-5-g123456789000-abogki123456789-4k",
    managerVersion = "3.0.0 (30000)",
    deviceModel = "Xiaomi 17 Pro Max",
    fingerprint = "Xiaomi/popsicle/popsicle:16/BQ2A.250705.001-BP2A.250605.031.A3/OS3.0.313.0.WPBCNXM:user/release-keys",
    selinuxStatus = "Enforcing",
    seccompStatus = 2
)

private val previewUriHandler = object : UriHandler {
    override fun openUri(uri: String) {}
}

@Composable
private fun HomeScreenPreviewContent(
    ksuVersion: Int?,
    lkmMode: Boolean?,
    isSafeMode: Boolean = false,
    isLateLoadMode: Boolean = false,
    superuserCount: Int = 0,
    moduleCount: Int = 0,
    selinuxStatus: String = "Enforcing",
) {
    CompositionLocalProvider(LocalUriHandler provides previewUriHandler) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val actions = HomeActions({}, {}, {}, {})
            StatusCard(
                state = previewHomeScreenState(
                    ksuVersion = ksuVersion,
                    lkmMode = lkmMode,
                    isSafeMode = isSafeMode,
                    isLateLoadMode = isLateLoadMode,
                    superuserCount = superuserCount,
                    moduleCount = moduleCount,
                    selinuxStatus = selinuxStatus,
                ),
                actions = actions
            )
            InfoCard(previewSystemInfo.copy(selinuxStatus = selinuxStatus))
            SecondaryLinksCard(onOpenUrl = {})
        }
    }
}

@Preview(name = "Home Activated", showBackground = true)
@Composable
private fun HomeScreenActivatedPreview() {
    HomeScreenPreviewContent(ksuVersion = 12345, lkmMode = true, superuserCount = 5, moduleCount = 10)
}

@Preview(name = "Home Not Activated", showBackground = true)
@Composable
private fun HomeScreenNotActivatedPreview() {
    HomeScreenPreviewContent(ksuVersion = null, lkmMode = null)
}

@Preview(name = "Home Permissive", showBackground = true)
@Composable
private fun HomeScreenPermissivePreview() {
    HomeScreenPreviewContent(ksuVersion = null, lkmMode = null, selinuxStatus = "Permissive")
}

@Preview(name = "Home Jailbreak", showBackground = true)
@Composable
private fun HomeScreenJailbreakPreview() {
    HomeScreenPreviewContent(ksuVersion = 12345, lkmMode = true, isLateLoadMode = true, superuserCount = 5, moduleCount = 10)
}

private fun previewHomeScreenState(
    ksuVersion: Int?,
    lkmMode: Boolean?,
    isSafeMode: Boolean = false,
    isLateLoadMode: Boolean = false,
    superuserCount: Int = 0,
    moduleCount: Int = 0,
    selinuxStatus: String = "Enforcing",
) = HomeUiState(
    kernelVersion = KernelVersion(6, 1, 0),
    ksuVersion = ksuVersion,
    lkmMode = lkmMode,
    isManager = true,
    isManagerPrBuild = false,
    isKernelPrBuild = false,
    requiresNewKernel = false,
    isRootAvailable = ksuVersion != null,
    isSafeMode = isSafeMode,
    isLateLoadMode = isLateLoadMode,
    currentManagerVersionCode = 10000,
    showVersionMismatchWarningSetting = true,
    superuserCount = superuserCount,
    moduleCount = moduleCount,
    systemInfo = previewSystemInfo.copy(selinuxStatus = selinuxStatus),
    kernelUAPIVersion = 1,
    managerUAPIVersion = 1,
    uapiMismatch = false,
)
