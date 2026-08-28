package me.weishu.kernelsu.ui.screen.launchericon

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.skrootpro.skrootproSp
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.screen.settings.ManagerNameDialog
import me.weishu.kernelsu.ui.screen.settings.SettingsWallpaperCropDialog
import me.weishu.kernelsu.ui.util.CustomWallpaperCrop
import me.weishu.kernelsu.ui.util.LauncherIconOption
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap
import me.weishu.kernelsu.ui.util.module.Shortcut
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

private val FullImageCrop = CustomWallpaperCrop(0f, 0f, 1f, 1f)

private object LauncherIdentityColors {
    val Page = Color(0xFFF4F6FA)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFEEF2F7)
    val Ink = Color(0xFF172033)
    val Muted = Color(0xFF5D687A)
    val Subtle = Color(0xFF7D8796)
    val Border = Color(0xFFD8E0EA)
    val Accent = Color(0xFF3568D4)
    val AccentSoft = Color(0xFFE8EEFC)
}

@Composable
fun LauncherIconScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedIndex = LauncherIconOption.selectedIndex(uiState.launcherIcon)
    val scope = rememberCoroutineScope()
    var customIconUri by remember { mutableStateOf<String?>(null) }
    var customIconCrop by remember { mutableStateOf(FullImageCrop) }
    var showCustomIconCrop by remember { mutableStateOf(false) }
    var showManagerNameDialog by remember { mutableStateOf(false) }
    val customIconFailedMessage = stringResource(R.string.settings_app_icon_custom_failed)
    val defaultManagerName = stringResource(R.string.app_name)
    DisposableEffect(context) {
        val window = (context as? Activity)?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            val previousLightStatusBars = controller.isAppearanceLightStatusBars
            val previousLightNavigationBars = controller.isAppearanceLightNavigationBars
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
            onDispose {
                controller.isAppearanceLightStatusBars = previousLightStatusBars
                controller.isAppearanceLightNavigationBars = previousLightNavigationBars
            }
        }
    }
    val customIconPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        customIconUri = uri.toString()
        customIconCrop = FullImageCrop
        showCustomIconCrop = true
    }
    val pickCustomIcon = dropUnlessResumed {
        customIconPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    val createCustomIconShortcut = createShortcut@{ crop: CustomWallpaperCrop ->
        val uri = customIconUri ?: return@createShortcut
        customIconCrop = crop
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                loadCustomImageBitmap(context, uri, maxSide = 1024, crop = crop)
            }
            if (bitmap == null) {
                Toast.makeText(
                    context,
                    customIconFailedMessage,
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val managerName = uiState.customManagerName.ifBlank { defaultManagerName }
                Shortcut.createManagerShortcut(context, bitmap, managerName)
            }
            showCustomIconCrop = false
            customIconUri = null
            customIconCrop = FullImageCrop
        }
    }

    LauncherIconScreenMiuix(
        selectedIndex = selectedIndex,
        customManagerName = uiState.customManagerName,
        defaultManagerName = defaultManagerName,
        onBack = dropUnlessResumed { navigator.pop() },
        onEditManagerName = { showManagerNameDialog = true },
        onSelect = viewModel::setLauncherIconByIndex,
        onPickCustomIcon = pickCustomIcon,
    )

    SettingsWallpaperCropDialog(
        show = showCustomIconCrop,
        uriString = customIconUri,
        crop = customIconCrop,
        onCropChange = createCustomIconShortcut,
        onDismissRequest = {
            showCustomIconCrop = false
            customIconUri = null
            customIconCrop = FullImageCrop
        },
        title = stringResource(R.string.settings_app_icon_custom_crop),
        emptyText = stringResource(R.string.settings_app_icon_custom_empty),
        editorAspectRatio = 1f,
        cropAspectRatio = 1f,
        defaultCrop = FullImageCrop,
    )
    ManagerNameDialog(
        show = showManagerNameDialog,
        initialName = uiState.customManagerName,
        onDismissRequest = { showManagerNameDialog = false },
        onConfirm = viewModel::setCustomManagerName,
    )
}

@Composable
private fun LauncherIconScreenMiuix(
    selectedIndex: Int,
    customManagerName: String,
    defaultManagerName: String,
    onBack: () -> Unit,
    onEditManagerName: () -> Unit,
    onSelect: (Int) -> Unit,
    onPickCustomIcon: () -> Unit,
) {
    Scaffold(
        containerColor = LauncherIdentityColors.Page,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_manager_identity),
                color = LauncherIdentityColors.Page,
                titleColor = LauncherIdentityColors.Ink,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = LauncherIdentityColors.Ink,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LauncherIconPickerContent(
            selectedIndex = selectedIndex,
            customManagerName = customManagerName,
            defaultManagerName = defaultManagerName,
            onEditManagerName = onEditManagerName,
            onSelect = onSelect,
            onPickCustomIcon = onPickCustomIcon,
            onRestore = { onSelect(0) },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun LauncherIconPickerContent(
    selectedIndex: Int,
    customManagerName: String,
    defaultManagerName: String,
    onEditManagerName: () -> Unit,
    onSelect: (Int) -> Unit,
    onPickCustomIcon: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier
            .fillMaxSize()
            .background(LauncherIdentityColors.Page)
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_manager_name),
                    color = LauncherIdentityColors.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = skrootproSp(18f, maxScale = 1f),
                )
                ManagerNameCard(
                    customManagerName = customManagerName,
                    defaultManagerName = defaultManagerName,
                    onClick = onEditManagerName,
                )
                Text(
                    text = stringResource(R.string.settings_app_icon_picker_header),
                    color = LauncherIdentityColors.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = skrootproSp(18f, maxScale = 1f),
                )
                Text(
                    text = stringResource(R.string.settings_app_icon_picker_hint),
                    color = LauncherIdentityColors.Muted,
                    fontSize = skrootproSp(13f, maxScale = 1f),
                    lineHeight = skrootproSp(18f, maxScale = 1f),
                )
            }
        }

        item {
            LauncherIconCustomCard(
                onClick = onPickCustomIcon,
            )
        }

        itemsIndexed(LauncherIconOption.entries) { index, option ->
            LauncherIconOptionCard(
                option = option,
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
            )
        }

        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(LauncherIdentityColors.Surface)
                        .border(1.dp, LauncherIdentityColors.Border, RoundedCornerShape(18.dp))
                        .clickable(onClick = onRestore)
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.settings_app_icon_restore),
                            color = LauncherIdentityColors.Ink,
                            fontSize = skrootproSp(17f, maxScale = 1f),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.settings_app_icon_restore_summary),
                            color = LauncherIdentityColors.Muted,
                            fontSize = skrootproSp(14f, maxScale = 1f),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_app_icon_only_builtin),
                    color = LauncherIdentityColors.Muted,
                    fontSize = skrootproSp(12.5f, maxScale = 1f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ManagerNameCard(
    customManagerName: String,
    defaultManagerName: String,
    onClick: () -> Unit,
) {
    val displayName = customManagerName.ifBlank { defaultManagerName }
    val summary = if (customManagerName.isBlank()) {
        stringResource(R.string.settings_manager_name_default_summary, defaultManagerName)
    } else {
        stringResource(R.string.settings_manager_name_custom_summary, customManagerName)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LauncherIdentityColors.Surface)
            .border(1.dp, LauncherIdentityColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(LauncherIdentityColors.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayName.take(1).ifBlank { "A" },
                    color = LauncherIdentityColors.Accent,
                    fontSize = skrootproSp(22f, maxScale = 1f),
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = displayName,
                    color = LauncherIdentityColors.Ink,
                    fontSize = skrootproSp(17f, maxScale = 1f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    color = LauncherIdentityColors.Muted,
                    fontSize = skrootproSp(12.5f, maxScale = 1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.settings_manager_name_edit),
                color = LauncherIdentityColors.Accent,
                fontSize = skrootproSp(13f, maxScale = 1f),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun LauncherIconCustomCard(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LauncherIdentityColors.Surface)
            .border(1.dp, LauncherIdentityColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .border(
                        width = 1.5.dp,
                        color = LauncherIdentityColors.Border,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(4.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(LauncherIdentityColors.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.settings_app_icon_custom_pick),
                    tint = LauncherIdentityColors.Accent,
                    modifier = Modifier.size(34.dp),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_app_icon_custom),
                    color = LauncherIdentityColors.Ink,
                    fontSize = skrootproSp(13.5f, maxScale = 1f),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.settings_app_icon_custom_pick),
                    color = LauncherIdentityColors.Subtle,
                    fontSize = skrootproSp(10.5f, maxScale = 1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LauncherIconOptionCard(
    option: LauncherIconOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val cardColor = if (selected) LauncherIdentityColors.AccentSoft else LauncherIdentityColors.Surface
    val accentColor = if (selected) LauncherIdentityColors.Accent else LauncherIdentityColors.Border

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .clip(shape)
            .background(cardColor)
            .border(if (selected) 2.dp else 1.dp, accentColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .border(
                        width = if (selected) 2.5.dp else 1.5.dp,
                        color = accentColor,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(4.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(LauncherIdentityColors.SurfaceMuted),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(option.foregroundRes),
                    contentDescription = stringResource(option.labelRes),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(option.labelRes),
                    color = if (selected) LauncherIdentityColors.Accent else LauncherIdentityColors.Ink,
                    fontSize = skrootproSp(13.5f, maxScale = 1f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(LauncherIdentityColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.settings_app_icon_picker_selected),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
