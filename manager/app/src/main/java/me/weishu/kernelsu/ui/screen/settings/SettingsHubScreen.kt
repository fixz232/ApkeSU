package me.weishu.kernelsu.ui.screen.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor

enum class SettingsCategory(
    val routeValue: String,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
) {
    Appearance(
        routeValue = "appearance",
        titleRes = R.string.settings_hub_appearance,
        summaryRes = R.string.settings_hub_appearance_summary,
    ),
    HomeAndManager(
        routeValue = "home_manager",
        titleRes = R.string.settings_hub_home_manager,
        summaryRes = R.string.settings_hub_home_manager_summary,
    ),
    RootAndPermissions(
        routeValue = "root_permissions",
        titleRes = R.string.settings_hub_root_permissions,
        summaryRes = R.string.settings_hub_root_permissions_summary,
    ),
    MountAndHide(
        routeValue = "mount_hide",
        titleRes = R.string.settings_hub_mount_hide,
        summaryRes = R.string.settings_hub_mount_hide_summary,
    ),
    Toolbox(
        routeValue = "toolbox",
        titleRes = R.string.settings_hub_toolbox,
        summaryRes = R.string.settings_hub_toolbox_summary,
    ),
    AppAndMaintenance(
        routeValue = "app_maintenance",
        titleRes = R.string.settings_hub_app_maintenance,
        summaryRes = R.string.settings_hub_app_maintenance_summary,
    );

    companion object {
        fun fromRouteValue(value: String?): SettingsCategory =
            entries.firstOrNull { it.routeValue == value } ?: Appearance
    }
}

@Composable
fun SettingsHubScreen(
    uiState: SettingsUiState,
    bottomInnerPadding: Dp,
    onOpenCategory: (SettingsCategory) -> Unit,
    onPageModeChange: (SettingsPageMode) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    SettingsPageModeButton(
                        currentMode = SettingsPageMode.Categories,
                        onModeChange = onPageModeChange,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = immersiveScrolledTopBarColor(
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 14.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                end = 14.dp,
                bottom = innerPadding.calculateBottomPadding() + bottomInnerPadding + 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(SettingsCategory.entries, key = SettingsCategory::routeValue) { category ->
                SettingsCategoryCard(
                    category = category,
                    status = categoryStatus(category, uiState),
                    onClick = { onOpenCategory(category) },
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    category: SettingsCategory,
    status: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val accent = categoryAccent(category)
    Surface(
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = shape,
        modifier = modifier
            .uiDecoratedCard(shape = shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 66.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    contentColor = accent,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = category.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = stringResource(category.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = status ?: stringResource(category.summaryRes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
        }
    }
}

@Composable
internal fun categoryAccent(category: SettingsCategory): Color = when (category) {
    SettingsCategory.Appearance -> MaterialTheme.colorScheme.primary
    SettingsCategory.HomeAndManager -> MaterialTheme.colorScheme.tertiary
    SettingsCategory.RootAndPermissions -> MaterialTheme.colorScheme.error
    SettingsCategory.MountAndHide -> MaterialTheme.colorScheme.secondary
    SettingsCategory.Toolbox -> MaterialTheme.colorScheme.primary
    SettingsCategory.AppAndMaintenance -> MaterialTheme.colorScheme.onSurfaceVariant
}

internal fun SettingsCategory.icon(): ImageVector = when (this) {
    SettingsCategory.Appearance -> Icons.Rounded.Palette
    SettingsCategory.HomeAndManager -> Icons.Rounded.DashboardCustomize
    SettingsCategory.RootAndPermissions -> Icons.Rounded.Security
    SettingsCategory.MountAndHide -> Icons.Rounded.FolderSpecial
    SettingsCategory.Toolbox -> Icons.Rounded.Build
    SettingsCategory.AppAndMaintenance -> Icons.Rounded.SettingsApplications
}

@Composable
private fun categoryStatus(category: SettingsCategory, uiState: SettingsUiState): String? = when (category) {
    SettingsCategory.Appearance -> null
    SettingsCategory.HomeAndManager -> uiState.customHomeTitle.takeIf(String::isNotBlank)
    SettingsCategory.RootAndPermissions -> when {
        uiState.isLateLoadMode -> stringResource(R.string.settings_runtime_mode_jailbreak)
        uiState.isLkmMode -> stringResource(R.string.settings_runtime_mode_lkm)
        uiState.runtimeModeResolved -> stringResource(R.string.settings_runtime_mode_gki)
        else -> null
    }
    SettingsCategory.MountAndHide -> uiState.builtinMountConflict?.let {
        stringResource(R.string.settings_builtin_mount_conflict_summary, it)
    }
    SettingsCategory.Toolbox -> null
    SettingsCategory.AppAndMaintenance -> null
}
