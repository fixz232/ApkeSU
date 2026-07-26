package me.weishu.kernelsu.ui.screen.install

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassButton
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassSurface
import me.weishu.kernelsu.ui.component.liquid.isLiquidGlassTheme
import me.weishu.kernelsu.ui.component.liquid.liquidGlassMiuixCardColors
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.LkmSelection
import me.weishu.kernelsu.ui.util.ThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.ConvertFile
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.MoveFile
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * @author weishu
 * @date 2024/3/12.
 */
@Composable
internal fun InstallScreenMiuix(
    uiState: InstallUiState,
    actions: InstallScreenActions,
) {
    val enableBlur = LocalEnableBlur.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopBar(
                onBack = actions.onBack,
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                barColor = barColor,
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .padding(top = 12.dp)
                        .padding(horizontal = 16.dp),
                    contentPadding = innerPadding,
                    overscrollEffect = null,
                ) {
                    item {
                        InstallInputPanel(
                            state = uiState,
                            actions = actions,
                        )
                        Spacer(Modifier.height(18.dp))
                    }
                }
                BottomActionBar(
                    canInstall = uiState.canInstall,
                    onNext = actions.onNext,
                )
            }
        }
    }
}

@Composable
private fun InstallInputPanel(
    state: InstallUiState,
    actions: InstallScreenActions,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SelectInstallMethod(
            state = state,
            onSelected = actions.onSelectMethod,
            onSelectBootImage = actions.onSelectBootImage,
            onSelectAnyKernel = actions.onSelectAnyKernel,
        )
        OptionalSettingsCard(state = state, actions = actions)
    }
}

@Composable
private fun InstallMethodOptionsCard(
    state: InstallUiState,
    options: List<InstallMethod>,
    onClick: (InstallMethod) -> Unit,
) {
    if (options.isEmpty()) return

    InstallWallpaperCard(
        slot = ThemeStoreImageSlot.InstallMethods,
        modifier = Modifier
            .fillMaxWidth()
            .installLiquidGlassSurface(),
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            options.forEach { option ->
                val selected = option.javaClass == state.installMethod?.javaClass
                val requiresRoot = option == InstallMethod.DirectInstall ||
                        option == InstallMethod.DirectInstallToInactiveSlot ||
                        option is InstallMethod.AnyKernel
                val available = !requiresRoot || state.rootAvailable
                val summary = if (available) {
                    if (selected) state.installMethod.summary ?: option.summary else option.summary
                } else {
                    stringResource(R.string.direct_install_root_required)
                }
                CheckboxPreference(
                    title = stringResource(id = option.label),
                    summary = summary,
                    checked = selected,
                    enabled = available,
                    onCheckedChange = { onClick(option) },
                )
            }
        }
    }
}

@Composable
private fun OptionalSettingsCard(
    state: InstallUiState,
    actions: InstallScreenActions,
) {
    if (state.installMethod is InstallMethod.AnyKernel) return

    InstallWallpaperCard(
        slot = ThemeStoreImageSlot.InstallOptions,
        modifier = Modifier
            .fillMaxWidth()
            .installLiquidGlassSurface(),
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            AnimatedVisibility(
                visible = state.canSelectPartition,
                enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                        fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
            ) {
                PartitionSelector(
                    state = state,
                    onSelectPartition = actions.onSelectPartition,
                )
            }
            if (state.installMethod is InstallMethod.SelectFile) {
                CheckboxPreference(
                    title = stringResource(R.string.hidden_path_lkm_patch),
                    summary = stringResource(R.string.hidden_path_lkm_patch_summary),
                    checked = state.hiddenPathLkmEnabled,
                    onCheckedChange = actions.onSetHiddenPathLkmEnabled,
                )
            }
            if (state.installMethod is InstallMethod.SelectFile && state.hiddenPathLkmEnabled) {
                HiddenPathLkmInfoRow(
                    lkmSelection = state.lkmSelection,
                    onSelectKmi = actions.onSelectHiddenPathKmi,
                )
            } else {
                LocalLkmFileRow(
                    lkmSelection = state.lkmSelection,
                    onUploadLkm = actions.onUploadLkm,
                    onClearLkm = actions.onClearLkm,
                )
            }
            AdvancedOptionsPanel(
                state = state,
                actions = actions,
            )
        }
    }
}

@Composable
private fun PartitionSelector(
    state: InstallUiState,
    onSelectPartition: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        OverlayDropdownPreference(
            items = state.displayPartitions,
            selectedIndex = state.partitionSelectionIndex,
            title = "${stringResource(R.string.install_select_partition)} (${state.slotSuffix})",
            onSelectedIndexChange = onSelectPartition,
            startAction = {
                Icon(
                    MiuixIcons.ConvertFile,
                    tint = colorScheme.onSurface,
                    modifier = Modifier.padding(end = 12.dp),
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun LocalLkmFileRow(
    lkmSelection: LkmSelection,
    onUploadLkm: () -> Unit,
    onClearLkm: () -> Unit,
) {
    BasicComponent(
        title = stringResource(id = R.string.install_upload_lkm_file),
        summary = (lkmSelection as? LkmSelection.LkmUri)?.let {
            stringResource(id = R.string.selected_lkm, it.uri.lastPathSegment ?: "(file)")
        },
        onClick = onUploadLkm,
        startAction = {
            Icon(
                MiuixIcons.MoveFile,
                tint = colorScheme.onSurface,
                modifier = Modifier.padding(end = 12.dp),
                contentDescription = null
            )
        },
        endActions = {
            if (lkmSelection is LkmSelection.LkmUri) {
                IconButton(onClick = onClearLkm) {
                    Icon(
                        MiuixIcons.Close,
                        modifier = Modifier.size(16.dp),
                        contentDescription = stringResource(android.R.string.cancel),
                        tint = colorScheme.onSurfaceVariantActions
                    )
                }
            } else {
                TrailingArrow()
            }
        }
    )
}

@Composable
private fun HiddenPathLkmInfoRow(
    lkmSelection: LkmSelection,
    onSelectKmi: () -> Unit,
) {
    BasicComponent(
        title = stringResource(id = R.string.hidden_path_lkm_builtin_title),
        summary = (lkmSelection as? LkmSelection.PathMaskKmiString)?.let {
            stringResource(id = R.string.hidden_path_lkm_selected_kmi, it.value)
        } ?: stringResource(id = R.string.hidden_path_lkm_builtin_summary),
        onClick = onSelectKmi,
        startAction = {
            Icon(
                MiuixIcons.MoveFile,
                tint = colorScheme.onSurface,
                modifier = Modifier.padding(end = 12.dp),
                contentDescription = null
            )
        },
        endActions = {
            TrailingArrow()
        },
    )
}

@Composable
private fun AdvancedOptionsPanel(
    state: InstallUiState,
    actions: InstallScreenActions,
) {
    BasicComponent(
        title = stringResource(id = R.string.advanced_options),
        onClick = actions.onAdvancedOptionsClicked,
        startAction = {
            Icon(
                imageVector = Icons.Rounded.Tune,
                tint = colorScheme.onSurface,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(22.dp),
                contentDescription = null
            )
        },
        endActions = {
            Icon(
                if (state.advancedOptionsShown) MiuixIcons.ExpandLess else MiuixIcons.ExpandMore,
                modifier = Modifier.size(16.dp),
                tint = colorScheme.onSurfaceVariantActions,
                contentDescription = stringResource(R.string.expand),
            )
        }
    )
    AnimatedVisibility(
        visible = state.advancedOptionsShown,
        enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)),
        exit = shrinkVertically(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
    ) {
        Column {
            CheckboxPreference(
                title = stringResource(id = R.string.allow_shell),
                checked = state.allowShell,
                summary = stringResource(id = R.string.allow_shell_summary),
                onCheckedChange = actions.onSelectAllowShell
            )
            CheckboxPreference(
                title = stringResource(id = R.string.enable_adb),
                checked = state.enableAdb,
                summary = stringResource(id = R.string.enable_adb_summary),
                onCheckedChange = actions.onSelectEnableAdb
            )
            AnimatedVisibility(visible = state.canForceBackup) {
                CheckboxPreference(
                    title = stringResource(id = R.string.install_force_backup),
                    checked = state.forceBackup,
                    summary = stringResource(id = R.string.install_force_backup_summary),
                    onCheckedChange = actions.onSelectForceBackup,
                )
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    canInstall: Boolean,
    onNext: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(
                top = 10.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                        WindowInsets.captionBar.asPaddingValues().calculateBottomPadding() + 12.dp
            ),
    ) {
        TextButton(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .globalLiquidGlassButton(),
            text = stringResource(id = R.string.install_next),
            enabled = canInstall,
            colors = ButtonDefaults.textButtonColorsPrimary(),
            onClick = onNext
        )
    }
}

@Composable
private fun SelectInstallMethod(
    state: InstallUiState,
    onSelected: (InstallMethod) -> Unit,
    onSelectBootImage: () -> Unit,
    onSelectAnyKernel: () -> Unit,
) {
    val confirmDialog = rememberConfirmDialog(
        onConfirm = {
            onSelected(InstallMethod.DirectInstallToInactiveSlot)
        }
    )
    val dialogTitle = stringResource(id = android.R.string.dialog_alert_title)
    val dialogContent = stringResource(id = R.string.install_inactive_slot_warning)

    val onClick = { option: InstallMethod ->
        when (option) {
            is InstallMethod.SelectFile -> onSelectBootImage()
            is InstallMethod.DirectInstall -> onSelected(option)
            is InstallMethod.DirectInstallToInactiveSlot -> confirmDialog.showConfirm(dialogTitle, dialogContent)
            is InstallMethod.AnyKernel -> onSelectAnyKernel()
        }
    }
    val selectFileOption = state.installMethodOptions.filterIsInstance<InstallMethod.SelectFile>().firstOrNull()
    val otherOptions = state.installMethodOptions.filterNot { it is InstallMethod.SelectFile }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        selectFileOption?.let { option ->
            val interactionSource = remember { MutableInteractionSource() }
            val selected = option.javaClass == state.installMethod?.javaClass
            SelectFileCard(
                option = option,
                selectedMethod = state.installMethod,
                selected = selected,
                interactionSource = interactionSource,
                onClick = { onClick(option) },
            )
        }
        InstallMethodOptionsCard(
            state = state,
            options = otherOptions,
            onClick = onClick,
        )
    }
}

@Composable
private fun SelectFileCard(
    option: InstallMethod.SelectFile,
    selectedMethod: InstallMethod?,
    selected: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
) {
    val selectedFile = selectedMethod as? InstallMethod.SelectFile
    val selectedFileName = selectedFile?.uri?.let { selectedFile.summary }
    val hasFile = selectedFileName != null
    val statusText = selectedFileName ?: stringResource(R.string.install_file_not_selected)
    val statusColor = if (hasFile) Color(0xFF1FAF55) else colorScheme.primary
    val summary = option.summary

    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (isLiquidGlassTheme()) {
                    Modifier.globalLiquidGlassSurface(
                        shape = shape,
                        surfaceAlpha = 0.60f,
                        blurRadius = 10.dp,
                        refractionHeight = 14.dp,
                        refractionAmount = 9.dp,
                        strokeAlpha = 0.66f,
                    )
                } else {
                    Modifier.background(colorScheme.surfaceContainerHigh.copy(alpha = 0.96f))
                }
            )
            .border(1.dp, colorScheme.primary.copy(alpha = 0.22f), shape)
            .toggleable(
                value = selected,
                onValueChange = { onClick() },
                role = Role.RadioButton,
                indication = LocalIndication.current,
                interactionSource = interactionSource
            )
    ) {
        InstallCardWallpaperBackground(ThemeStoreImageSlot.InstallImage)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (hasFile) Icons.Rounded.CheckCircleOutline else Icons.Rounded.AutoFixHigh,
                    tint = statusColor,
                    modifier = Modifier.size(25.dp),
                    contentDescription = null
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = stringResource(id = option.label),
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusPill(
                    text = statusText,
                    color = statusColor,
                    modifier = Modifier.fillMaxWidth(),
                )
                summary?.let {
                    Text(
                        text = it,
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BootImageTip()
            }
        }
    }
}

@Composable
private fun InstallWallpaperCard(
    slot: ThemeStoreImageSlot,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = liquidGlassMiuixCardColors(colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            InstallCardWallpaperBackground(slot)
            content()
        }
    }
}

@Composable
private fun Modifier.installLiquidGlassSurface(): Modifier {
    return globalLiquidGlassSurface(
        shape = RoundedCornerShape(18.dp),
        surfaceAlpha = 0.58f,
        blurRadius = 10.dp,
        refractionHeight = 14.dp,
        refractionAmount = 9.dp,
        strokeAlpha = 0.66f,
    )
}

@Composable
private fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (color == Color(0xFF1FAF55)) {
            Icon(
                imageVector = Icons.Rounded.CheckCircleOutline,
                tint = color,
                modifier = Modifier.size(13.dp),
                contentDescription = null,
            )
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BootImageTip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.primary.copy(alpha = 0.07f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            tint = colorScheme.primary,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(15.dp),
            contentDescription = null,
        )
        Text(
            text = stringResource(R.string.install_boot_image_tip),
            color = colorScheme.onSurfaceVariantSummary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun TrailingArrow() {
    val layoutDirection = LocalLayoutDirection.current
    Icon(
        modifier = Modifier
            .size(width = 10.dp, height = 16.dp)
            .graphicsLayer {
                scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
            },
        imageVector = MiuixIcons.Basic.ArrowRight,
        contentDescription = null,
        tint = colorScheme.onSurfaceVariantActions,
    )
}

@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    scrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
    barColor: Color,
) {
    BlurredBar(backdrop) {
        TopAppBar(
            color = barColor,
            title = stringResource(R.string.install),
            navigationIcon = {
                IconButton(
                    onClick = onBack
                ) {
                    val layoutDirection = LocalLayoutDirection.current
                    Icon(
                        modifier = Modifier.graphicsLayer {
                            if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                        },
                        imageVector = MiuixIcons.Back,
                        tint = colorScheme.onSurface,
                        contentDescription = null,
                    )
                }
            },
            scrollBehavior = scrollBehavior
        )
    }
}
