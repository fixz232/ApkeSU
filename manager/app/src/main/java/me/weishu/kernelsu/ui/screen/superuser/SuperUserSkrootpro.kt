package me.weishu.kernelsu.ui.screen.superuser

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproButton
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproScreen
import me.weishu.kernelsu.ui.component.skrootpro.skrootproSp
import me.weishu.kernelsu.ui.theme.isInDarkTheme

@Composable
fun SuperUserPagerSkrootpro(
    uiState: SuperUserUiState,
    actions: SuperUserActions,
    bottomInnerPadding: Dp,
) {
    var selectedFilter by remember { mutableStateOf(SkrootproGrantFilter.All) }
    val searchText = uiState.searchStatus.searchText
    val sourceApps = if (searchText.isBlank()) uiState.groupedApps else uiState.searchResults
    val apps = remember(sourceApps, selectedFilter) {
        sourceApps.filter { selectedFilter.matches(it) }
    }
    SkrootproScreen(
        title = stringResource(R.string.superuser),
        showAdd = true,
        onAddClick = actions.onOpenAppIdManager,
        actionIcon = Icons.Rounded.Fingerprint,
        actionContentDescription = stringResource(R.string.app_id_manager_open),
        secondaryActionIcon = Icons.Rounded.AcUnit,
        onSecondaryActionClick = actions.onOpenAppFreeze,
        secondaryActionContentDescription = stringResource(R.string.app_freeze_open),
        tertiaryActionIcon = Icons.Rounded.Refresh,
        onTertiaryActionClick = actions.onRefresh,
        tertiaryActionContentDescription = stringResource(R.string.refresh_refresh),
        bottomInnerPadding = bottomInnerPadding,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 16.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SkrootproGrantOverview(groups = uiState.groupedApps)
            }
            item {
                SkrootproGrantSearch(
                    searchText = searchText,
                    onSearchTextChange = actions.onSearchTextChange,
                    onClearSearch = actions.onClearSearch,
                )
            }
            item {
                SkrootproGrantFilters(
                    selected = selectedFilter,
                    onSelected = { selectedFilter = it },
                )
            }
            if (uiState.error != null) {
                item {
                    SkrootproGrantLoadError(onRetry = actions.onRefresh)
                }
            }
            if (apps.isEmpty() && uiState.error == null) {
                item {
                    SkrootproGrantEmpty(
                        text = if (searchText.isBlank() && selectedFilter == SkrootproGrantFilter.All) {
                            stringResource(R.string.skrootpro_su_empty)
                        } else {
                            stringResource(R.string.superuser_empty)
                        },
                    )
                }
            } else {
                items(apps, key = { it.uid }) { group ->
                    SkrootproGrantRow(group = group, onClick = { actions.onOpenProfile(group) })
                }
            }
        }
    }
}

@Composable
private fun SkrootproGrantRow(group: GroupedApps, onClick: () -> Unit) {
    val tags = group.statusTags(isInDarkTheme())
    val title = group.ownerName ?: group.primary.label
    val summary = if (group.apps.size > 1) {
        stringResource(R.string.group_contains_apps, group.apps.size)
    } else {
        group.primary.packageName
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 82.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SkrootproColors.Surface)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconImage(
            packageInfo = group.primary.packageInfo,
            label = group.primary.label,
            modifier = Modifier.size(48.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 10.dp),
        ) {
            Text(
                text = title,
                color = SkrootproColors.Text,
                fontSize = skrootproSp(18f, maxScale = 1.05f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = summary,
                color = SkrootproColors.Muted,
                fontSize = skrootproSp(13f, maxScale = 1.05f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            tags.forEach { tag ->
                SkrootproStatusTag(tag)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = SkrootproColors.Muted,
            modifier = Modifier
                .padding(start = 6.dp)
                .size(26.dp),
        )
    }
}

@Composable
private fun SkrootproGrantOverview(groups: List<GroupedApps>) {
    val rootCount = groups.count { it.anyAllowSu }
    val customCount = groups.count { it.anyCustom }
    val umountCount = groups.count { it.shouldUmount }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SkrootproColors.Surface)
            .padding(18.dp),
    ) {
        Text(
            text = stringResource(
                if (rootCount > 0) R.string.superuser_dashboard_normal else R.string.superuser_dashboard_empty
            ),
            color = SkrootproColors.Text,
            fontSize = skrootproSp(18f, maxScale = 1.05f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.superuser_dashboard_summary, rootCount),
            color = SkrootproColors.Muted,
            fontSize = skrootproSp(12.5f, maxScale = 1.05f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkrootproGrantStat(
                value = rootCount,
                label = stringResource(R.string.superuser_stat_authorized),
                accent = Color(0xFF2474C8),
                modifier = Modifier.weight(1f),
            )
            SkrootproGrantStat(
                value = customCount,
                label = stringResource(R.string.superuser_stat_custom),
                accent = Color(0xFF6F38C5),
                modifier = Modifier.weight(1f),
            )
            SkrootproGrantStat(
                value = umountCount,
                label = stringResource(R.string.superuser_stat_umount),
                accent = Color(0xFF6A6A70),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SkrootproGrantStat(
    value: Int,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SkrootproColors.SurfaceStrong)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value.toString(),
            color = accent,
            fontSize = skrootproSp(19f, maxScale = 1.0f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = label,
            color = SkrootproColors.Muted,
            fontSize = skrootproSp(11f, maxScale = 1.0f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SkrootproGrantSearch(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(SkrootproColors.SurfaceStrong)
            .padding(start = 18.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = SkrootproColors.Muted,
            modifier = Modifier.size(23.dp),
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = SkrootproColors.Text,
                fontSize = skrootproSp(15f, maxScale = 1.05f),
                fontWeight = FontWeight.Medium,
            ),
            cursorBrush = SolidColor(SkrootproColors.Purple),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (searchText.isBlank()) {
                        Text(
                            text = stringResource(R.string.superuser_search_hint),
                            color = SkrootproColors.Muted,
                            fontSize = skrootproSp(15f, maxScale = 1.05f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (searchText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onClearSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = SkrootproColors.Muted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SkrootproGrantFilters(
    selected: SkrootproGrantFilter,
    onSelected: (SkrootproGrantFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SkrootproGrantFilter.entries.forEach { filter ->
            SkrootproGrantFilterChip(
                label = stringResource(filter.labelRes),
                selected = selected == filter,
                onClick = { onSelected(filter) },
            )
        }
    }
}

@Composable
private fun SkrootproGrantFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = 58.dp)
            .clip(shape)
            .background(if (selected) SkrootproColors.Purple else SkrootproColors.Surface)
            .border(
                width = 1.dp,
                color = if (selected) SkrootproColors.Purple else SkrootproColors.Disabled,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else SkrootproColors.Muted,
            fontSize = skrootproSp(12.5f, maxScale = 1.0f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SkrootproGrantEmpty(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SkrootproColors.Surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = SkrootproColors.Muted,
            fontSize = skrootproSp(14f, maxScale = 1.05f),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SkrootproStatusTag(tag: StatusMeta) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tag.bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tag.label,
            color = tag.fg,
            fontSize = skrootproSp(10.5f, maxScale = 1.0f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun GroupedApps.statusTags(darkMode: Boolean): List<StatusMeta> {
    val userId = uid / 100000
    return buildList {
        if (anyAllowSu) add(StatusMeta("ROOT", if (darkMode) Color(0xFF17324E) else Color(0xFFEAF3FF), if (darkMode) Color(0xFF8DC8FF) else Color(0xFF2474C8)))
        if (shouldUmount) add(StatusMeta("UMOUNT", if (darkMode) Color(0xFF303138) else Color(0xFFE9E9EC), if (darkMode) Color(0xFFC2C3CA) else Color(0xFF66666A)))
        if (anyCustom) add(StatusMeta("CUSTOM", if (darkMode) Color(0xFF34264B) else Color(0xFFF0E8FF), if (darkMode) Color(0xFFD0ADFF) else Color(0xFF6F38C5)))
        if (userId != 0) add(StatusMeta("USER $userId", if (darkMode) Color(0xFF19382D) else Color(0xFFEAF6F1), if (darkMode) Color(0xFF8EDDBA) else Color(0xFF2B7A59)))
        if (isEmpty()) add(StatusMeta("OFF", if (darkMode) Color(0xFF2A2B31) else Color(0xFFF1F1F2), if (darkMode) Color(0xFFB8B9C0) else Color(0xFF8A8A8A)))
    }
}

@Composable
private fun SkrootproGrantLoadError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SkrootproColors.Surface)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.superuser_failed_to_load),
            color = SkrootproColors.Muted,
            fontSize = skrootproSp(14f, maxScale = 1.05f),
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SkrootproButton(
            text = stringResource(R.string.network_retry),
            onClick = onRetry,
            modifier = Modifier.width(120.dp),
        )
    }
}

private enum class SkrootproGrantFilter(val labelRes: Int) {
    All(R.string.superuser_filter_all),
    Root(R.string.superuser_filter_root),
    Umount(R.string.superuser_filter_umount),
    Custom(R.string.superuser_filter_custom),
    MultiUser(R.string.superuser_filter_multi_user);

    fun matches(group: GroupedApps): Boolean = when (this) {
        All -> true
        Root -> group.anyAllowSu
        Umount -> group.shouldUmount
        Custom -> group.anyCustom
        MultiUser -> group.uid / 100000 != 0
    }
}
