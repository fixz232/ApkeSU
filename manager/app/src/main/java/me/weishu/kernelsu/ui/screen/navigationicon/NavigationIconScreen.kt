package me.weishu.kernelsu.ui.screen.navigationicon

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.CustomNavigationIconImage
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassSurface
import me.weishu.kernelsu.ui.component.liquid.liquidGlassMiuixCardColors
import me.weishu.kernelsu.ui.component.material.SegmentedColumn
import me.weishu.kernelsu.ui.component.material.SegmentedListItem
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.screen.settings.SettingsNavigationIconPreviewDialog
import me.weishu.kernelsu.ui.screen.settings.SettingsWallpaperCropDialog
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.theme.skrootproTopBarColors
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.CustomNavigationIconSet
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.CustomNavigationIconState
import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_NAVIGATION_ICON_CROP
import me.weishu.kernelsu.ui.util.persistCustomImageReference
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import me.weishu.kernelsu.ui.util.takePersistableImageReadPermission
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.Scaffold as MaterialScaffold
import androidx.compose.material3.TopAppBar as MaterialTopAppBar
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

@Composable
fun NavigationIconScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val pickTarget = rememberSaveable { mutableStateOf<String?>(null) }
    val cropTarget = rememberSaveable { mutableStateOf<String?>(null) }
    val previewTarget = rememberSaveable { mutableStateOf<String?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val slot = pickTarget.value?.let(::navigationIconSlotFromId)
            ?: return@rememberLauncherForActivityResult
        pickTarget.value = null
        uri ?: return@rememberLauncherForActivityResult
        val uriString = persistCustomImageReference(context, uri, slot.uriKey)
            ?: uri.toString().also { takePersistableImageReadPermission(context, uri) }
        viewModel.setCustomNavigationIcon(slot, uriString)
        cropTarget.value = slot.id
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val onBack = dropUnlessResumed { navigator.pop() }
    val actions = NavigationIconActions(
        onPick = { slot ->
            pickTarget.value = slot.id
            imageLauncher.launch(arrayOf("image/*"))
        },
        onCrop = { slot ->
            cropTarget.value = slot.id
        },
        onPreview = { slot ->
            previewTarget.value = slot.id
        },
        onClear = { slot ->
            viewModel.clearCustomNavigationIcon(slot)
            if (cropTarget.value == slot.id) {
                cropTarget.value = null
            }
            if (previewTarget.value == slot.id) {
                previewTarget.value = null
            }
        },
    )

    when (LocalUiMode.current) {
        UiMode.Material -> NavigationIconScreenMaterial(
            icons = uiState.customNavigationIcons,
            actions = actions,
            onBack = onBack,
        )

        UiMode.Miuix -> NavigationIconScreenMiuix(
            icons = uiState.customNavigationIcons,
            actions = actions,
            onBack = onBack,
        )
    }

    val cropSlot = cropTarget.value?.let(::navigationIconSlotFromId)
    if (cropSlot != null) {
        val iconState = uiState.customNavigationIcons[cropSlot]
        val cropTitle = stringResource(cropSlot.cropTitleRes)
        val emptyText = stringResource(R.string.settings_navigation_icon_not_selected)
        SettingsWallpaperCropDialog(
            show = iconState.hasSelected,
            uriString = iconState.uriString,
            crop = iconState.crop,
            onCropChange = {
                viewModel.setCustomNavigationIconCrop(cropSlot, it)
                previewTarget.value = cropSlot.id
            },
            onDismissRequest = {
                cropTarget.value = null
            },
            title = cropTitle,
            emptyText = emptyText,
            editorAspectRatio = 1f,
            cropAspectRatio = 1f,
            defaultCrop = DEFAULT_CUSTOM_NAVIGATION_ICON_CROP,
        )
    }

    val previewSlot = previewTarget.value?.let(::navigationIconSlotFromId)
    if (previewSlot != null) {
        SettingsNavigationIconPreviewDialog(
            show = uiState.customNavigationIcons[previewSlot].hasSelected,
            slot = previewSlot,
            state = uiState.customNavigationIcons[previewSlot],
            onDismissRequest = {
                previewTarget.value = null
            },
        )
    }
}

@Composable
private fun NavigationIconScreenMaterial(
    icons: CustomNavigationIconSet,
    actions: NavigationIconActions,
    onBack: () -> Unit,
) {
    MaterialScaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            MaterialTopAppBar(
                title = { Text(stringResource(R.string.settings_navigation_icons)) },
                navigationIcon = {
                    MaterialIconButton(onClick = onBack) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SegmentedColumn(
                content = buildList {
                    CustomNavigationIconSlot.entries.forEach { slot ->
                        add {
                            NavigationIconMaterialItem(
                                slot = slot,
                                state = icons[slot],
                                actions = actions,
                            )
                        }
                    }
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NavigationIconMaterialItem(
    slot: CustomNavigationIconSlot,
    state: CustomNavigationIconState,
    actions: NavigationIconActions,
) {
    val title = stringResource(slot.titleRes)
    SegmentedListItem(
        onClick = { actions.onPick(slot) },
        headlineContent = { Text(title) },
        supportingContent = {
            NavigationIconSummary(
                selected = state.hasSelected,
                cropText = stringResource(R.string.settings_navigation_icon_crop_action),
                previewText = stringResource(R.string.settings_navigation_icon_preview_action),
                clearText = stringResource(R.string.settings_navigation_icon_clear_action),
                onCrop = { actions.onCrop(slot) },
                onPreview = { actions.onPreview(slot) },
                onClear = { actions.onClear(slot) },
            )
        },
        leadingContent = {
            CustomNavigationIconImage(
                state = state,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = slot.materialFallbackIcon,
                    contentDescription = title,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null
            )
        },
    )
}

@Composable
private fun NavigationIconSummary(
    selected: Boolean,
    cropText: String,
    previewText: String,
    clearText: String,
    onCrop: () -> Unit,
    onPreview: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(
                if (selected) {
                    R.string.settings_navigation_icon_selected_summary
                } else {
                    R.string.settings_navigation_icon_summary
                }
            )
        )
        if (selected) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onCrop) {
                    Text(cropText)
                }
                TextButton(onClick = onPreview) {
                    Text(previewText)
                }
                TextButton(onClick = onClear) {
                    Text(clearText)
                }
            }
        }
    }
}

@Composable
private fun NavigationIconScreenMiuix(
    icons: CustomNavigationIconSet,
    actions: NavigationIconActions,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    val topBarColors = skrootproTopBarColors(barColor, colorScheme.onSurface)

    MiuixScaffold(
        containerColor = Color.Transparent,
        topBar = {
            BlurredBar(backdrop) {
                MiuixTopAppBar(
                    color = topBarColors.container,
                    titleColor = topBarColors.content,
                    title = stringResource(R.string.settings_navigation_icons),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            MiuixIcon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                tint = topBarColors.content,
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    },
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    NavigationIconMiuixSectionTitle(
                        text = stringResource(R.string.settings_navigation_icons),
                        topPadding = 12.dp,
                    )
                    Card(
                        modifier = Modifier
                            .padding(top = 6.dp, bottom = 18.dp)
                            .fillMaxWidth()
                            .navigationIconLiquidGlassSurface(),
                        colors = liquidGlassMiuixCardColors(),
                    ) {
                        CustomNavigationIconSlot.entries.forEach { slot ->
                            NavigationIconMiuixPreference(
                                slot = slot,
                                state = icons[slot],
                                actions = actions,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationIconMiuixPreference(
    slot: CustomNavigationIconSlot,
    state: CustomNavigationIconState,
    actions: NavigationIconActions,
) {
    val title = stringResource(slot.titleRes)
    ArrowPreference(
        title = title,
        summary = stringResource(
            if (state.hasSelected) {
                R.string.settings_navigation_icon_selected_summary
            } else {
                R.string.settings_navigation_icon_summary
            }
        ),
        startAction = {
            CustomNavigationIconImage(
                state = state,
                contentDescription = title,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(30.dp),
            ) {
                MiuixIcon(
                    imageVector = slot.miuixFallbackIcon,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(24.dp),
                    contentDescription = title,
                    tint = colorScheme.onBackground
                )
            }
        },
        onClick = { actions.onPick(slot) },
        holdDownState = state.hasSelected,
        bottomAction = {
            if (state.hasSelected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MiuixTextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.settings_navigation_icon_crop_action),
                        onClick = { actions.onCrop(slot) },
                    )
                    MiuixTextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.settings_navigation_icon_preview_action),
                        onClick = { actions.onPreview(slot) },
                    )
                    MiuixTextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.settings_navigation_icon_clear_action),
                        onClick = { actions.onClear(slot) },
                    )
                }
            }
        },
    )
}

@Composable
private fun NavigationIconMiuixSectionTitle(
    text: String,
    topPadding: Dp = 18.dp,
) {
    MiuixText(
        modifier = Modifier.padding(start = 16.dp, top = topPadding, bottom = 2.dp),
        text = text,
        color = colorScheme.primary,
        fontSize = 13.sp,
    )
}

@Composable
private fun Modifier.navigationIconLiquidGlassSurface(): Modifier {
    return globalLiquidGlassSurface(
        shape = RoundedCornerShape(18.dp),
        surfaceAlpha = 0.58f,
        blurRadius = 10.dp,
        refractionHeight = 14.dp,
        refractionAmount = 9.dp,
        strokeAlpha = 0.66f,
    )
}

private val CustomNavigationIconSlot.materialFallbackIcon: ImageVector
    get() = when (this) {
        CustomNavigationIconSlot.Home -> Icons.Filled.Home
        CustomNavigationIconSlot.Superuser -> Icons.Filled.Shield
        CustomNavigationIconSlot.Module -> Icons.Filled.Extension
        CustomNavigationIconSlot.Settings -> Icons.Filled.Settings
    }

private val CustomNavigationIconSlot.miuixFallbackIcon: ImageVector
    get() = when (this) {
        CustomNavigationIconSlot.Home -> Icons.Rounded.Cottage
        CustomNavigationIconSlot.Superuser -> Icons.Rounded.Security
        CustomNavigationIconSlot.Module -> Icons.Rounded.Extension
        CustomNavigationIconSlot.Settings -> Icons.Rounded.Settings
    }

private fun navigationIconSlotFromId(id: String): CustomNavigationIconSlot? {
    return CustomNavigationIconSlot.entries.firstOrNull { it.id == id }
}

private data class NavigationIconActions(
    val onPick: (CustomNavigationIconSlot) -> Unit,
    val onCrop: (CustomNavigationIconSlot) -> Unit,
    val onPreview: (CustomNavigationIconSlot) -> Unit,
    val onClear: (CustomNavigationIconSlot) -> Unit,
)
