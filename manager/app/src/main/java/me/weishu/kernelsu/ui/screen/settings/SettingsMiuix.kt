package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fence
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.component.KsuIsValid
import me.weishu.kernelsu.ui.component.dialog.rememberLoadingDialog
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassSurface
import me.weishu.kernelsu.ui.component.liquid.FrostedGlassCardStyle
import me.weishu.kernelsu.ui.component.snow.snowMiuixCardColors
import me.weishu.kernelsu.ui.component.snow.snowMiuixCardSurface
import me.weishu.kernelsu.ui.component.snow.isSnowInterfaceStyle
import me.weishu.kernelsu.ui.component.snow.SeasonStyle
import me.weishu.kernelsu.ui.component.pixel.PixelStyle
import me.weishu.kernelsu.ui.component.pixel.pixelAwareMiuixCardCornerRadius
import me.weishu.kernelsu.ui.component.pixel.pixelPalette
import me.weishu.kernelsu.ui.component.rain.RainStyle
import me.weishu.kernelsu.ui.component.rain.rainPalette
import me.weishu.kernelsu.ui.component.ink.InkStyle
import me.weishu.kernelsu.ui.component.ink.inkPalette
import me.weishu.kernelsu.ui.component.miuix.SendLogDialog
import me.weishu.kernelsu.ui.component.uninstalldialog.UninstallDialog
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.theme.skrootproTopBarColors
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.MAX_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_WALLPAPER_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.util.MIN_CUSTOM_VIDEO_BACKGROUND_DURATION_SECONDS
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import me.weishu.kernelsu.ui.component.miuix.SunMoonSwitchPreference as SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * @author weishu
 * @date 2023/1/1.
 */
@Composable
fun SettingPagerMiuix(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = immersiveTopBarColor(
        if (blurActive) Color.Transparent else colorScheme.surface,
    )
    val topBarColors = skrootproTopBarColors(barColor, colorScheme.onSurface)
    val loadingDialog = rememberLoadingDialog()
    val context = LocalContext.current
    val showUninstallDialog = rememberSaveable { mutableStateOf(false) }
    val showSendLogDialog = rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var expandedCategoryRoute by rememberSaveable {
        mutableStateOf(readLastSettingsCategory(context)?.routeValue)
    }
    var recentCategoryRoutes by rememberSaveable {
        mutableStateOf(
            readRecentSettingsCategories(context).joinToString(",") { it.routeValue }
        )
    }
    val recentCategories = recentCategoryRoutes
        .split(',')
        .filter(String::isNotBlank)
        .map(SettingsCategory::fromRouteValue)
        .distinct()
    fun categoryVisible(category: SettingsCategory): Boolean =
        SettingsCatalog.categoryMatches(context, category, uiState, searchQuery)
    fun categoryExpanded(category: SettingsCategory): Boolean =
        searchQuery.isNotBlank() || expandedCategoryRoute == category.routeValue
    fun updateCategory(category: SettingsCategory, expanded: Boolean) {
        expandedCategoryRoute = if (expanded) category.routeValue else null
        if (expanded) {
            recordSettingsCategoryInteraction(context, category)
            recentCategoryRoutes = readRecentSettingsCategories(context)
                .joinToString(",") { it.routeValue }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = topBarColors.container,
                    titleColor = topBarColors.content,
                    title = stringResource(R.string.settings),
                    scrollBehavior = scrollBehavior
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
                    androidx.compose.material3.OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        placeholder = {
                            androidx.compose.material3.Text(stringResource(R.string.settings_search_hint))
                        },
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                androidx.compose.material3.IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    androidx.compose.material3.Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.settings_search_clear),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )

                    if (searchQuery.isBlank() && recentCategories.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_recent_changes),
                            color = colorScheme.onSurfaceVariantSummary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 10.dp),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            recentCategories.forEach { category ->
                                androidx.compose.material3.FilterChip(
                                    selected = expandedCategoryRoute == category.routeValue,
                                    onClick = { updateCategory(category, true) },
                                    label = {
                                        androidx.compose.material3.Text(stringResource(category.titleRes))
                                    },
                                    modifier = Modifier.height(48.dp),
                                )
                            }
                        }
                    }

                    val hasSearchResults = SettingsCategory.entries.any(::categoryVisible)
                    if (!hasSearchResults) {
                        Text(
                            text = stringResource(R.string.settings_search_empty, searchQuery),
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 28.dp),
                        )
                    }

                    if (categoryVisible(SettingsCategory.Appearance)) {
                    CollapsibleMiuixSection(
                        title = stringResource(R.string.settings_hub_appearance),
                        summary = stringResource(R.string.settings_hub_appearance_summary),
                        icon = Icons.Rounded.Palette,
                        itemCount = SettingsCatalog.visibleEntryCount(SettingsCategory.Appearance, uiState),
                        expanded = categoryExpanded(SettingsCategory.Appearance),
                        onExpandedChange = { updateCategory(SettingsCategory.Appearance, it) },
                        topPadding = 12.dp,
                    ) {
                        val dayNightChecked = isDayNightSwitchChecked(uiState.themeMode)
                        OverlayDropdownPreference(
                            title = stringResource(id = R.string.settings_ui_mode),
                            summary = stringResource(id = R.string.settings_ui_mode_summary),
                            items = InterfaceStyle.selectableEntries.map { stringResource(it.labelRes) },
                            startAction = {
                                Icon(
                                    Icons.Rounded.Dashboard,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_ui_mode),
                                    tint = colorScheme.onBackground
                                )
                            },
                            selectedIndex = InterfaceStyle.selectedIndex(uiState.uiMode),
                            onSelectedIndexChange = actions.onSetUiModeIndex
                        )
                        if (
                            uiState.uiMode == InterfaceStyle.Alpha.value ||
                            uiState.uiMode == InterfaceStyle.Delta.value
                        ) {
                            AlphaDeltaMiuixPreference(
                                deltaSelected = uiState.uiMode == InterfaceStyle.Delta.value,
                                onModeSelected = actions.onSetAlphaDeltaMode,
                            )
                        }
                        if (uiState.uiMode == InterfaceStyle.Miuix.value) {
                            SwitchPreference(
                                title = stringResource(R.string.settings_miuix_classic_home_layout),
                                summary = stringResource(R.string.settings_miuix_classic_home_layout_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Dashboard,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = null,
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                checked = uiState.miuixClassicHomeLayoutEnabled,
                                onCheckedChange = actions.onSetMiuixClassicHomeLayoutEnabled,
                            )
                        }
                        if (uiState.uiMode == InterfaceStyle.Snow.value) {
                            SeasonMiuixPreference(
                                selectedValue = uiState.seasonStyle,
                                cardMotionEnabled = uiState.seasonCardMotionEnabled,
                                onSelectedIndexChange = actions.onSetSeasonStyleIndex,
                                onCardMotionEnabledChange = actions.onSetSeasonCardMotionEnabled,
                            )
                        }
                        if (uiState.uiMode == InterfaceStyle.Rain.value) {
                            RainMiuixPreference(
                                selectedValue = uiState.rainStyle,
                                cardMotionEnabled = uiState.rainCardMotionEnabled,
                                onSelectedIndexChange = actions.onSetRainStyleIndex,
                                onCardMotionEnabledChange = actions.onSetRainCardMotionEnabled,
                            )
                        }
                        if (uiState.uiMode == InterfaceStyle.Ink.value) {
                            InkMiuixPreference(
                                selectedValue = uiState.inkStyle,
                                fontEnabled = uiState.inkFontEnabled,
                                cardMotionEnabled = uiState.inkCardMotionEnabled,
                                onSelectedIndexChange = actions.onSetInkStyleIndex,
                                onFontEnabledChange = actions.onSetInkFontEnabled,
                                onCardMotionEnabledChange = actions.onSetInkCardMotionEnabled,
                            )
                        }
                        if (uiState.uiMode == InterfaceStyle.Pixel.value) {
                            PixelMiuixPreference(
                                selectedValue = uiState.pixelStyle,
                                cardMotionEnabled = uiState.pixelCardMotionEnabled,
                                onSelectedIndexChange = actions.onSetPixelStyleIndex,
                                onCardMotionEnabledChange = actions.onSetPixelCardMotionEnabled,
                            )
                        }
                        DayNightMiuixPreference(
                            title = stringResource(id = R.string.settings_day_night_switch),
                            summary = stringResource(id = R.string.settings_day_night_switch_summary),
                            checked = dayNightChecked,
                            onCheckedChange = actions.onSetDayNightMode,
                        )
                        CategorizedMiuixActionRow(
                            title = stringResource(id = R.string.theme_store),
                            summary = stringResource(id = R.string.theme_store_settings_summary),
                            icon = Icons.Rounded.Storefront,
                            onClick = actions.onOpenThemeStore,
                        )
                    }
                    }

                    if (categoryVisible(SettingsCategory.HomeAndManager)) {
                    CollapsibleMiuixSection(
                        title = stringResource(R.string.settings_hub_home_manager),
                        summary = stringResource(R.string.settings_hub_home_manager_summary),
                        icon = Icons.Rounded.Dashboard,
                        itemCount = SettingsCatalog.visibleEntryCount(SettingsCategory.HomeAndManager, uiState),
                        expanded = categoryExpanded(SettingsCategory.HomeAndManager),
                        onExpandedChange = { updateCategory(SettingsCategory.HomeAndManager, it) },
                    ) {
                        CategorizedMiuixActionRow(
                            title = stringResource(id = R.string.settings_manager_identity),
                            summary = stringResource(id = R.string.settings_manager_identity_summary),
                            icon = Icons.Rounded.Apps,
                            onClick = actions.onOpenLauncherIcon,
                        )
                        CategorizedMiuixActionRow(
                            title = stringResource(id = R.string.settings_home_title),
                            summary = if (uiState.customHomeTitle.isBlank()) {
                                stringResource(id = R.string.settings_home_title_default_summary)
                            } else {
                                stringResource(
                                    id = R.string.settings_home_title_custom_summary,
                                    uiState.customHomeTitle,
                                )
                            },
                            icon = Icons.Rounded.EditNote,
                            onClick = actions.onEditHomeTitle,
                        )
                        CategorizedMiuixActionRow(
                            title = stringResource(id = R.string.home_layout_title),
                            summary = stringResource(id = R.string.home_layout_settings_summary),
                            icon = Icons.Rounded.Dashboard,
                            onClick = actions.onOpenHomeLayout,
                        )
                        CategorizedMiuixActionRow(
                            title = stringResource(id = R.string.pixel_pet_title),
                            summary = stringResource(
                                id = if (uiState.pixelPetEnabled) {
                                    R.string.pixel_pet_enabled_summary
                                } else {
                                    R.string.pixel_pet_disabled_summary
                                },
                            ),
                            icon = Icons.Rounded.AutoFixHigh,
                            onClick = actions.onOpenPixelPet,
                        )
                        CategorizedMiuixSwitchRow(
                            title = stringResource(id = R.string.settings_show_home_support_card),
                            summary = stringResource(id = R.string.settings_show_home_support_card_summary),
                            icon = Icons.Rounded.Visibility,
                            checked = uiState.showHomeSupportCard,
                            onCheckedChange = actions.onSetShowHomeSupportCard,
                        )
                        CategorizedMiuixSwitchRow(
                            title = stringResource(id = R.string.settings_show_home_learn_card),
                            summary = stringResource(id = R.string.settings_show_home_learn_card_summary),
                            icon = Icons.Rounded.Visibility,
                            checked = uiState.showHomeLearnCard,
                            onCheckedChange = actions.onSetShowHomeLearnCard,
                        )
                    }
                    }

                    KsuIsValid {
                        if (categoryVisible(SettingsCategory.RootAndPermissions)) {
                        CollapsibleMiuixSection(
                            title = stringResource(R.string.settings_hub_root_permissions),
                            summary = stringResource(R.string.settings_hub_root_permissions_summary),
                            icon = Icons.Rounded.Security,
                            itemCount = SettingsCatalog.visibleEntryCount(SettingsCategory.RootAndPermissions, uiState),
                            expanded = categoryExpanded(SettingsCategory.RootAndPermissions),
                            onExpandedChange = { updateCategory(SettingsCategory.RootAndPermissions, it) },
                        ) {
                            val profileTemplate = stringResource(id = R.string.settings_profile_template)
                            CategorizedMiuixActionRow(
                                title = profileTemplate,
                                summary = stringResource(id = R.string.settings_profile_template_summary),
                                icon = Icons.Rounded.Fence,
                                onClick = actions.onOpenProfileTemplate,
                            )
                            val suCompatModeItems = listOf(
                                stringResource(id = R.string.settings_mode_enable_by_default),
                                stringResource(id = R.string.settings_mode_disable_until_reboot),
                                stringResource(id = R.string.settings_mode_disable_always),
                            )

                            val suSummary = when (uiState.suCompatStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_sucompat_summary)
                            }
                            OverlayDropdownPreference(
                                title = stringResource(id = R.string.settings_sucompat),
                                summary = suSummary,
                                items = suCompatModeItems,
                                startAction = {
                                    Icon(
                                        Icons.Rounded.RemoveModerator,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_sucompat),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                enabled = uiState.suCompatStatus == "supported",
                                selectedIndex = uiState.suCompatMode,
                                onSelectedIndexChange = actions.onSetSuCompatMode
                            )

                            val umountSummary = when (uiState.kernelUmountStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_kernel_umount_summary)
                            }
                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_kernel_umount),
                                summary = umountSummary,
                                icon = Icons.Rounded.RemoveCircle,
                                enabled = uiState.kernelUmountStatus == "supported",
                                checked = uiState.isKernelUmountEnabled,
                                onCheckedChange = actions.onSetKernelUmountEnabled,
                            )

                            val webViewUmountSummary = when (uiState.webViewZygoteUmountStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_webview_zygote_umount_summary)
                            }
                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_webview_zygote_umount),
                                summary = webViewUmountSummary,
                                icon = Icons.Rounded.Language,
                                enabled = uiState.webViewZygoteUmountStatus == "supported",
                                checked = uiState.isWebViewZygoteUmountEnabled,
                                onCheckedChange = actions.onSetWebViewZygoteUmountEnabled,
                            )

                            val selinuxHideSummary = when (uiState.selinuxHideStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_selinux_hide_summary)
                            }
                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_selinux_hide),
                                summary = selinuxHideSummary,
                                icon = Icons.Rounded.Policy,
                                enabled = uiState.selinuxHideStatus == "supported",
                                checked = uiState.isSelinuxHideEnabled,
                                onCheckedChange = actions.onSetSelinuxHideEnabled,
                            )

                            val sulogSummary = when (uiState.sulogStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_sulog_summary)
                            }
                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_sulog),
                                summary = sulogSummary,
                                icon = Icons.AutoMirrored.Rounded.Article,
                                enabled = uiState.sulogStatus == "supported",
                                checked = uiState.isSulogEnabled,
                                onCheckedChange = actions.onSetSulogEnabled,
                            )

                            val adbRootSummary = when (uiState.adbRootStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_adb_root_summary)
                            }
                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_adb_root),
                                summary = adbRootSummary,
                                icon = Icons.Rounded.Adb,
                                enabled = uiState.adbRootStatus == "supported",
                                checked = uiState.isAdbRootEnabled,
                                onCheckedChange = actions.onSetAdbRootEnabled,
                            )

                            val avcSpoofSummary = when (uiState.avcSpoofStatus) {
                                "unsupported" -> stringResource(id = R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(id = R.string.feature_status_managed_summary)
                                else -> stringResource(id = R.string.settings_avc_spoof_summary)
                            }
                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_avc_spoof),
                                summary = avcSpoofSummary,
                                icon = Icons.Rounded.EditNote,
                                enabled = uiState.avcSpoofStatus == "supported",
                                checked = uiState.isAvcSpoofEnabled,
                                onCheckedChange = actions.onSetAvcSpoofEnabled,
                            )
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_soft_reboot),
                                summary = stringResource(id = R.string.settings_soft_reboot_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.RestartAlt,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_soft_reboot),
                                        tint = if (uiState.isLateLoadMode) colorScheme.disabledOnSecondaryVariant else colorScheme.onBackground
                                    )
                                },
                                enabled = !uiState.isLateLoadMode,
                                checked = uiState.isLateLoadMode || uiState.useSoftReboot,
                                onCheckedChange = actions.onSetUseSoftReboot
                            )
                        }
                        }

                        if (categoryVisible(SettingsCategory.MountAndHide)) {
                        CollapsibleMiuixSection(
                            title = stringResource(R.string.settings_hub_mount_hide),
                            summary = stringResource(R.string.settings_hub_mount_hide_summary),
                            icon = Icons.Rounded.Layers,
                            itemCount = SettingsCatalog.visibleEntryCount(SettingsCategory.MountAndHide, uiState),
                            expanded = categoryExpanded(SettingsCategory.MountAndHide),
                            onExpandedChange = { updateCategory(SettingsCategory.MountAndHide, it) },
                        ) {
                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_umount_modules_default),
                                summary = stringResource(id = R.string.settings_umount_modules_default_summary),
                                icon = Icons.Rounded.FolderDelete,
                                checked = uiState.isDefaultUmountModules,
                                onCheckedChange = actions.onSetDefaultUmountModules,
                            )

                            val builtinMountSummary = uiState.builtinMountConflict?.let {
                                stringResource(id = R.string.settings_builtin_mount_conflict_summary, it)
                            } ?: stringResource(id = R.string.settings_builtin_mount_summary)
                            CategorizedMiuixActionRow(
                                title = stringResource(id = R.string.settings_builtin_mount),
                                summary = builtinMountSummary,
                                icon = Icons.Rounded.Layers,
                                onClick = actions.onOpenBuiltinMount,
                            )

                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_kpatch_next),
                                summary = kPatchNextSummary(uiState),
                                icon = Icons.Rounded.DeveloperMode,
                                enabled = uiState.canToggleKPatchNext,
                                checked = uiState.isKPatchNextSwitchChecked,
                                onCheckedChange = actions.onSetKPatchNextEnabled,
                            )

                            CategorizedMiuixActionRow(
                                title = stringResource(id = R.string.settings_kpatch_next_webui),
                                summary = stringResource(
                                    id = if (uiState.canOpenKPatchNextWebUi) {
                                        R.string.settings_kpatch_next_webui_summary
                                    } else {
                                        R.string.settings_kpatch_next_webui_disabled_summary
                                    }
                                ),
                                icon = Icons.Rounded.Apps,
                                enabled = uiState.canOpenKPatchNextWebUi,
                                onClick = actions.onOpenKPatchNextWebUi,
                            )

                            CategorizedMiuixActionRow(
                                title = pathConfigTitle(uiState),
                                summary = pathConfigSummary(uiState),
                                icon = Icons.Rounded.Visibility,
                                enabled = uiState.canOpenPathConfig,
                                onClick = actions.onOpenHiddenPathConfig,
                            )
                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_epkesu_hide),
                                summary = stringResource(id = R.string.settings_epkesu_hide_summary),
                                icon = Icons.Rounded.Visibility,
                                checked = uiState.isEpkesuHideEnabled,
                                onCheckedChange = actions.onSetEpkesuHideEnabled,
                            )
                        }
                        }

                        if (categoryVisible(SettingsCategory.Toolbox)) {
                        CollapsibleMiuixSection(
                            title = stringResource(R.string.settings_hub_toolbox),
                            summary = stringResource(R.string.settings_hub_toolbox_summary),
                            icon = Icons.Rounded.DeveloperMode,
                            itemCount = SettingsCatalog.visibleEntryCount(SettingsCategory.Toolbox, uiState),
                            expanded = categoryExpanded(SettingsCategory.Toolbox),
                            onExpandedChange = { updateCategory(SettingsCategory.Toolbox, it) },
                        ) {
                            CategorizedMiuixActionRow(
                                title = stringResource(id = R.string.rescue_protection),
                                summary = stringResource(id = R.string.rescue_protection_summary),
                                icon = Icons.Rounded.Security,
                                onClick = actions.onOpenRescueProtection,
                            )
                            CategorizedMiuixActionRow(
                                title = stringResource(id = R.string.image_tool_title),
                                summary = stringResource(id = R.string.image_tool_settings_summary),
                                icon = Icons.Rounded.ImageSearch,
                                onClick = actions.onOpenImageTool,
                            )
                            CategorizedMiuixActionRow(
                                title = stringResource(id = R.string.settings_cpu_spoof),
                                summary = stringResource(id = R.string.settings_cpu_spoof_summary),
                                icon = Icons.Rounded.DeveloperMode,
                                onClick = actions.onOpenCpuSpoof,
                            )
                            CategorizedMiuixActionRow(
                                title = stringResource(id = R.string.settings_device_identity),
                                summary = stringResource(id = R.string.settings_device_identity_summary),
                                icon = Icons.Rounded.Badge,
                                onClick = actions.onOpenDeviceIdentity,
                            )
                            CategorizedMiuixActionRow(
                                title = stringResource(id = R.string.settings_ai_chat),
                                summary = stringResource(id = R.string.settings_ai_chat_summary),
                                icon = Icons.Rounded.AutoFixHigh,
                                onClick = actions.onOpenAiChat,
                            )
                            CategorizedMiuixSwitchRow(
                                title = stringResource(R.string.settings_graphics_renderer_tool),
                                summary = stringResource(R.string.settings_graphics_renderer_tool_summary),
                                icon = Icons.Rounded.DeveloperMode,
                                checked = uiState.graphicsRendererFeatureEnabled,
                                onCheckedChange = actions.onSetGraphicsRendererFeatureEnabled,
                            )

                            if (uiState.graphicsRendererFeatureEnabled) {
                                CategorizedMiuixActionRow(
                                    title = stringResource(R.string.settings_graphics_renderer),
                                    summary = stringResource(R.string.settings_graphics_renderer_summary),
                                    icon = Icons.Rounded.DeveloperMode,
                                    onClick = actions.onOpenGraphicsRenderer,
                                )
                            }
                            if (uiState.isKpmSettingsEntryVisible) {
                                CategorizedMiuixActionRow(
                                    title = stringResource(R.string.kpm_title),
                                    summary = stringResource(R.string.kpm_settings_summary),
                                    icon = Icons.Rounded.Bolt,
                                    onClick = actions.onOpenKpm,
                                )
                            }
                        }
                        }
                    }

                    if (categoryVisible(SettingsCategory.AppAndMaintenance)) {
                    CollapsibleMiuixSection(
                        title = stringResource(R.string.settings_hub_app_maintenance),
                        summary = stringResource(R.string.settings_hub_app_maintenance_summary),
                        icon = Icons.Rounded.BugReport,
                        itemCount = SettingsCatalog.visibleEntryCount(SettingsCategory.AppAndMaintenance, uiState),
                        expanded = categoryExpanded(SettingsCategory.AppAndMaintenance),
                        onExpandedChange = { updateCategory(SettingsCategory.AppAndMaintenance, it) },
                        bottomPadding = 12.dp,
                    ) {
                        CategorizedMiuixActionRow(
                            title = stringResource(id = R.string.settings_language),
                            summary = stringResource(id = R.string.settings_language_summary),
                            icon = Icons.Rounded.Language,
                            onClick = actions.onOpenLanguage,
                        )
                        KsuIsValid {
                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_module_check_update),
                                summary = stringResource(id = R.string.settings_module_check_update_summary),
                                icon = Icons.Rounded.UploadFile,
                                checked = uiState.checkModuleUpdate,
                                onCheckedChange = actions.onSetCheckModuleUpdate,
                            )
                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_version_mismatch_warning),
                                summary = stringResource(id = R.string.settings_version_mismatch_warning_summary),
                                icon = Icons.Rounded.BugReport,
                                checked = uiState.showVersionMismatchWarning,
                                onCheckedChange = actions.onSetShowVersionMismatchWarning,
                            )
                            CategorizedMiuixSwitchRow(
                                title = stringResource(id = R.string.settings_gki_warning),
                                summary = stringResource(id = R.string.settings_gki_warning_summary),
                                icon = Icons.Rounded.BugReport,
                                checked = uiState.showGkiWarning,
                                onCheckedChange = actions.onSetShowGkiWarning,
                            )
                        }
                        CategorizedMiuixSwitchRow(
                            title = stringResource(id = R.string.enable_web_debugging),
                            summary = stringResource(id = R.string.enable_web_debugging_summary),
                            icon = Icons.Rounded.DeveloperMode,
                            checked = uiState.enableWebDebugging,
                            onCheckedChange = actions.onSetEnableWebDebugging,
                        )
                        CategorizedMiuixSwitchRow(
                            title = stringResource(id = R.string.settings_auto_jailbreak),
                            summary = stringResource(id = R.string.settings_auto_jailbreak_summary),
                            icon = Icons.Rounded.ElectricalServices,
                            enabled = uiState.isLateLoadMode,
                            checked = uiState.autoJailbreak,
                            onCheckedChange = actions.onSetAutoJailbreak,
                        )
                        if (uiState.isLkmMode) {
                            val uninstall = stringResource(id = R.string.settings_uninstall)
                            CategorizedMiuixActionRow(
                                title = uninstall,
                                enabled = !uiState.isLateLoadMode,
                                icon = Icons.Rounded.Delete,
                                onClick = { showUninstallDialog.value = true },
                            )
                            UninstallDialog(
                                show = showUninstallDialog.value,
                                onDismissRequest = { showUninstallDialog.value = false }
                            )
                        }
                        CategorizedMiuixActionRow(
                            title = stringResource(id = R.string.send_log),
                            icon = Icons.Rounded.BugReport,
                            onClick = { showSendLogDialog.value = true },
                        )
                        SendLogDialog(
                            show = showSendLogDialog.value,
                            onDismissRequest = { showSendLogDialog.value = false },
                            loadingDialog = loadingDialog
                        )
                        val about = stringResource(id = R.string.about)
                        CategorizedMiuixActionRow(
                            title = about,
                            icon = Icons.Rounded.ContactPage,
                            onClick = actions.onOpenAbout,
                        )
                    }
                    }
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun CategorizedMiuixActionRow(
    title: String,
    summary: String = "",
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = title,
        summary = summary,
        startAction = {
            Icon(
                imageVector = icon,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = null,
                tint = if (enabled) colorScheme.onBackground else colorScheme.disabledOnSecondaryVariant,
            )
        },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun CategorizedMiuixSwitchRow(
    title: String,
    summary: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchPreference(
        title = title,
        summary = summary,
        startAction = {
            Icon(
                imageVector = icon,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = null,
                tint = if (enabled) colorScheme.onBackground else colorScheme.disabledOnSecondaryVariant,
            )
        },
        enabled = enabled,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun SettingsSectionTitle(
    text: String,
    summary: String,
    icon: ImageVector,
    itemCount: Int,
    topPadding: Dp = 18.dp,
    expanded: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    val expandedState = expanded == true
    val rotation by animateFloatAsState(
        targetValue = if (expandedState) 180f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "settingsSectionArrowRotation",
    )
    val shape = RoundedCornerShape(18.dp)
    val countLabel = if (itemCount == 1) {
        stringResource(R.string.settings_section_item_count_single)
    } else {
        stringResource(R.string.settings_section_item_count, itemCount)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .clip(shape)
            .background(
                color = if (expandedState) {
                    colorScheme.surfaceContainerHigh.copy(alpha = 0.82f)
                } else {
                    colorScheme.surfaceContainer.copy(alpha = 0.58f)
                },
                shape = shape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 14.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = colorScheme.primaryContainer.copy(alpha = if (expandedState) 0.78f else 0.52f),
                    shape = RoundedCornerShape(13.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                color = colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            modifier = Modifier
                .background(
                    color = if (expandedState) {
                        colorScheme.primaryContainer.copy(alpha = 0.72f)
                    } else {
                        colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
                    },
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
            text = countLabel,
            color = if (expandedState) colorScheme.primary else colorScheme.onSurfaceVariantActions,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        if (expanded != null) {
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = text,
                tint = colorScheme.primary,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

@Composable
private fun CollapsibleMiuixSection(
    title: String,
    summary: String,
    icon: ImageVector,
    itemCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    topPadding: Dp = 18.dp,
    bottomPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    SettingsSectionTitle(
        text = title,
        summary = summary,
        icon = icon,
        itemCount = itemCount,
        topPadding = topPadding,
        expanded = expanded,
        onClick = { onExpandedChange(!expanded) },
    )
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)),
        exit = shrinkVertically(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing)),
    ) {
        Card(
            cornerRadius = pixelAwareMiuixCardCornerRadius(18.dp),
            modifier = Modifier
                .padding(top = 6.dp, bottom = bottomPadding)
                .fillMaxWidth()
                .settingsLiquidGlassSurface(),
            colors = snowMiuixCardColors(
                color = immersiveSurfaceColor(colorScheme.surfaceContainer),
            ),
            insideMargin = PaddingValues(top = if (isSnowInterfaceStyle()) 8.dp else 0.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun AlphaDeltaMiuixPreference(
    deltaSelected: Boolean,
    onModeSelected: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.Palette,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(24.dp),
            contentDescription = null,
            tint = colorScheme.onBackground,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_alpha_delta_mode),
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.settings_alpha_delta_mode_summary),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp, end = 24.dp, top = 10.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.66f))
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false, true).forEach { useDelta ->
            val selected = deltaSelected == useDelta
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) colorScheme.primaryContainer.copy(alpha = 0.92f) else Color.Transparent,
                    )
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onModeSelected(useDelta) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (useDelta) R.string.settings_delta_mode else R.string.settings_alpha_mode,
                    ),
                    color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SeasonMiuixPreference(
    selectedValue: String,
    cardMotionEnabled: Boolean,
    onSelectedIndexChange: (Int) -> Unit,
    onCardMotionEnabledChange: (Boolean) -> Unit,
) {
    val selectedSeason = SeasonStyle.fromValue(selectedValue)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.Palette,
            modifier = Modifier.padding(end = 12.dp).size(24.dp),
            contentDescription = null,
            tint = colorScheme.onBackground,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_season_style),
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(selectedSeason.summaryRes),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp, end = 24.dp, top = 10.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.66f))
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SeasonStyle.entries.forEachIndexed { index, season ->
            val selected = season == selectedSeason
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) colorScheme.primaryContainer.copy(alpha = 0.92f) else Color.Transparent
                    )
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onSelectedIndexChange(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(season.labelRes),
                    color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
    SwitchPreference(
        title = stringResource(R.string.settings_season_card_motion),
        summary = stringResource(R.string.settings_season_card_motion_summary),
        startAction = {
            Icon(
                Icons.Rounded.AutoFixHigh,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = stringResource(R.string.settings_season_card_motion),
                tint = colorScheme.onBackground,
            )
        },
        checked = cardMotionEnabled,
        onCheckedChange = onCardMotionEnabledChange,
    )
}

@Composable
private fun RainMiuixPreference(
    selectedValue: String,
    cardMotionEnabled: Boolean,
    onSelectedIndexChange: (Int) -> Unit,
    onCardMotionEnabledChange: (Boolean) -> Unit,
) {
    val selectedStyle = RainStyle.fromValue(selectedValue)
    val dark = isInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.WaterDrop,
            modifier = Modifier.padding(end = 12.dp).size(24.dp),
            contentDescription = null,
            tint = colorScheme.onBackground,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_rain_style),
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(selectedStyle.summaryRes),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp, end = 24.dp, top = 10.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.66f))
            .selectableGroup()
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        RainStyle.entries.toList().chunked(2).forEach { styleRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                styleRow.forEach { rainStyle ->
                    val index = RainStyle.entries.indexOf(rainStyle)
                    val selected = rainStyle == selectedStyle
                    val preview = rainPalette(rainStyle, dark)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) {
                                    colorScheme.primaryContainer.copy(alpha = 0.92f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onSelectedIndexChange(index) },
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 32.dp, height = 22.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(preview.backgroundTop, preview.backgroundBottom),
                                    ),
                                )
                                .border(1.dp, preview.outline.copy(alpha = 0.78f), RoundedCornerShape(5.dp)),
                        ) {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val lineCount = when (rainStyle) {
                                    RainStyle.LightRain -> 3
                                    RainStyle.MediumRain -> 4
                                    RainStyle.HeavyRain -> 5
                                    RainStyle.Thunderstorm -> 6
                                    RainStyle.AfterRain -> 2
                                }
                                repeat(lineCount) { line ->
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height((7 + line % 3 * 3).dp)
                                            .graphicsLayer { rotationZ = -12f }
                                            .background(preview.rain.copy(alpha = 0.78f)),
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 2.dp)
                                    .width(18.dp)
                                    .height(1.dp)
                                    .background(preview.ripple.copy(alpha = 0.72f)),
                            )
                            if (rainStyle == RainStyle.AfterRain) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 3.dp, end = 4.dp)
                                        .size(5.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(preview.highlight.copy(alpha = 0.78f)),
                                )
                            }
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(rainStyle.labelRes),
                            color = if (selected) {
                                colorScheme.onPrimaryContainer
                            } else {
                                colorScheme.onSurfaceVariantSummary
                            },
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
    SwitchPreference(
        title = stringResource(R.string.settings_rain_card_motion),
        summary = stringResource(R.string.settings_rain_card_motion_summary),
        startAction = {
            Icon(
                Icons.Rounded.AutoFixHigh,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = stringResource(R.string.settings_rain_card_motion),
                tint = colorScheme.onBackground,
            )
        },
        checked = cardMotionEnabled,
        onCheckedChange = onCardMotionEnabledChange,
    )
}

@Composable
private fun InkMiuixPreference(
    selectedValue: String,
    fontEnabled: Boolean,
    cardMotionEnabled: Boolean,
    onSelectedIndexChange: (Int) -> Unit,
    onFontEnabledChange: (Boolean) -> Unit,
    onCardMotionEnabledChange: (Boolean) -> Unit,
) {
    val selectedStyle = InkStyle.fromValue(selectedValue)
    val dark = isInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.Brush,
            modifier = Modifier.padding(end = 12.dp).size(24.dp),
            contentDescription = null,
            tint = colorScheme.onBackground,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_ink_style),
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(selectedStyle.summaryRes),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp, end = 24.dp, top = 10.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.66f))
            .selectableGroup()
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        InkStyle.entries.toList().chunked(2).forEach { styleRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                styleRow.forEach { inkStyle ->
                    val index = InkStyle.entries.indexOf(inkStyle)
                    val selected = inkStyle == selectedStyle
                    val preview = inkPalette(inkStyle, dark)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) {
                                    colorScheme.primaryContainer.copy(alpha = 0.92f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onSelectedIndexChange(index) },
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(width = 34.dp, height = 22.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(preview.backgroundTop, preview.backgroundBottom),
                                    ),
                                )
                                .border(1.dp, preview.outline.copy(alpha = 0.72f), RoundedCornerShape(5.dp)),
                        ) {
                            drawLine(
                                color = preview.farMountain.copy(alpha = 0.72f),
                                start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.72f),
                                end = androidx.compose.ui.geometry.Offset(size.width * 0.46f, size.height * 0.40f),
                                strokeWidth = 1.dp.toPx(),
                            )
                            drawLine(
                                color = preview.nearMountain.copy(alpha = 0.78f),
                                start = androidx.compose.ui.geometry.Offset(size.width * 0.34f, size.height * 0.76f),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.46f),
                                strokeWidth = 1.dp.toPx(),
                            )
                            drawLine(
                                color = preview.water.copy(alpha = 0.74f),
                                start = androidx.compose.ui.geometry.Offset(size.width * 0.20f, size.height * 0.84f),
                                end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.84f),
                                strokeWidth = 0.7.dp.toPx(),
                            )
                            drawCircle(
                                color = preview.seal.copy(alpha = 0.82f),
                                radius = 1.5.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.23f),
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(inkStyle.labelRes),
                            color = if (selected) {
                                colorScheme.onPrimaryContainer
                            } else {
                                colorScheme.onSurfaceVariantSummary
                            },
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
    SwitchPreference(
        title = stringResource(R.string.settings_ink_font),
        summary = stringResource(R.string.settings_ink_font_summary),
        startAction = {
            Icon(
                Icons.Rounded.EditNote,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = stringResource(R.string.settings_ink_font),
                tint = colorScheme.onBackground,
            )
        },
        checked = fontEnabled,
        onCheckedChange = onFontEnabledChange,
    )
    SwitchPreference(
        title = stringResource(R.string.settings_ink_card_motion),
        summary = stringResource(R.string.settings_ink_card_motion_summary),
        startAction = {
            Icon(
                Icons.Rounded.AutoFixHigh,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = stringResource(R.string.settings_ink_card_motion),
                tint = colorScheme.onBackground,
            )
        },
        checked = cardMotionEnabled,
        onCheckedChange = onCardMotionEnabledChange,
    )
}

@Composable
private fun PixelMiuixPreference(
    selectedValue: String,
    cardMotionEnabled: Boolean,
    onSelectedIndexChange: (Int) -> Unit,
    onCardMotionEnabledChange: (Boolean) -> Unit,
) {
    val selectedStyle = PixelStyle.fromValue(selectedValue)
    val dark = isInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.Palette,
            modifier = Modifier.padding(end = 12.dp).size(24.dp),
            contentDescription = null,
            tint = colorScheme.onBackground,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_pixel_style),
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(selectedStyle.summaryRes),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp, end = 24.dp, top = 10.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.66f))
            .selectableGroup()
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        PixelStyle.entries.toList().chunked(2).forEach { styleRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                styleRow.forEach { pixelStyle ->
                    val index = PixelStyle.entries.indexOf(pixelStyle)
                    val selected = pixelStyle == selectedStyle
                    val previewPalette = pixelPalette(pixelStyle, dark)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (selected) {
                                    colorScheme.primaryContainer.copy(alpha = 0.92f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onSelectedIndexChange(index) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(width = 26.dp, height = 18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(previewPalette.surface)
                                .border(1.dp, previewPalette.outline, RoundedCornerShape(2.dp)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .fillMaxWidth(0.55f)
                                    .height(4.dp)
                                    .background(previewPalette.primary),
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(7.dp, 4.dp)
                                    .background(previewPalette.secondary),
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(pixelStyle.labelRes),
                            color = if (selected) {
                                colorScheme.onPrimaryContainer
                            } else {
                                colorScheme.onSurfaceVariantSummary
                            },
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(2 - styleRow.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    SwitchPreference(
        title = stringResource(R.string.settings_pixel_card_motion),
        summary = stringResource(R.string.settings_pixel_card_motion_summary),
        startAction = {
            Icon(
                Icons.Rounded.AutoFixHigh,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = stringResource(R.string.settings_pixel_card_motion),
                tint = colorScheme.onBackground,
            )
        },
        checked = cardMotionEnabled,
        onCheckedChange = onCardMotionEnabledChange,
    )
}

@Composable
private fun DayNightMiuixPreference(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Palette,
            modifier = Modifier.padding(end = 12.dp),
            contentDescription = title,
            tint = colorScheme.onBackground,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DayNightSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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
private fun Modifier.settingsLiquidGlassSurface(): Modifier {
    return globalLiquidGlassSurface(
        shape = RoundedCornerShape(18.dp),
        surfaceAlpha = 0.58f,
        blurRadius = 10.dp,
        refractionHeight = 14.dp,
        refractionAmount = 9.dp,
        strokeAlpha = 0.66f,
        cardStyle = FrostedGlassCardStyle.Pearl,
    ).snowMiuixCardSurface(shape = RoundedCornerShape(18.dp))
}
