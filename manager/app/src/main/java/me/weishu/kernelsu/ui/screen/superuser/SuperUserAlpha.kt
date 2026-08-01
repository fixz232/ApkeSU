package me.weishu.kernelsu.ui.screen.superuser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.component.alpha.AlphaCard
import me.weishu.kernelsu.ui.component.alpha.AlphaColors
import me.weishu.kernelsu.ui.component.alpha.AlphaOutlinedButton
import me.weishu.kernelsu.ui.component.alpha.AlphaShapes
import me.weishu.kernelsu.ui.component.alpha.AlphaScreen
import me.weishu.kernelsu.ui.component.alpha.alphaStrongWeight
import me.weishu.kernelsu.ui.component.alpha.alphaSp

@Composable
fun SuperUserPagerAlpha(
    uiState: SuperUserUiState,
    actions: SuperUserActions,
    bottomInnerPadding: Dp,
) {
    val searchText = uiState.searchStatus.searchText
    val apps = if (searchText.isBlank()) uiState.groupedApps else uiState.searchResults

    AlphaScreen(
        title = stringResource(R.string.superuser),
        bottomInnerPadding = bottomInnerPadding,
        topActionIcon = Icons.Rounded.Apps,
        onTopActionClick = actions.onOpenAppTools,
        topActionContentDescription = stringResource(R.string.superuser_app_tools_title),
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 18.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                AlphaSuperUserSearch(
                    searchText = searchText,
                    onSearchTextChange = actions.onSearchTextChange,
                    onClearSearch = actions.onClearSearch,
                )
            }
            if (uiState.error != null) {
                item {
                    AlphaLoadErrorCard(onRetry = actions.onRefresh)
                }
            }
            if (apps.isEmpty() && uiState.error == null) {
                item {
                    AlphaEmptyCard(
                        text = if (uiState.hasLoaded) {
                            stringResource(R.string.superuser_empty)
                        } else {
                            stringResource(R.string.refresh_refresh)
                        }
                    )
                }
            } else {
                items(apps, key = { it.uid }) { group ->
                    AlphaGrantRow(
                        group = group,
                        onClick = { actions.onOpenProfile(group) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphaSuperUserSearch(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(AlphaShapes.Control)
            .background(AlphaColors.Surface)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = AlphaColors.Muted,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            singleLine = true,
            cursorBrush = SolidColor(AlphaColors.Accent),
            textStyle = TextStyle(
                color = AlphaColors.Text,
                fontSize = alphaSp(15f),
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (searchText.isBlank()) {
                        Text(
                            text = stringResource(R.string.superuser_search_hint),
                            color = AlphaColors.Muted,
                            fontSize = alphaSp(14f),
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
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClearSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = AlphaColors.Muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AlphaGrantRow(
    group: GroupedApps,
    onClick: () -> Unit,
) {
    val title = group.ownerName ?: group.primary.label
    val summary = if (group.apps.size > 1) {
        stringResource(R.string.group_contains_apps, group.apps.size)
    } else {
        group.primary.packageName
    }
    AlphaCard(
        modifier = Modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconImage(
                packageInfo = group.primary.packageInfo,
                label = group.primary.label,
                modifier = Modifier.size(40.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = title,
                    color = AlphaColors.Text,
                    fontSize = alphaSp(15.5f),
                    fontWeight = alphaStrongWeight(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    color = AlphaColors.Muted,
                    fontSize = alphaSp(13f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AlphaGrantBadge(
                    label = if (group.anyAllowSu) "ROOT" else "OFF",
                    active = group.anyAllowSu,
                )
                if (group.anyCustom) {
                    AlphaGrantBadge(label = "CUSTOM", active = true)
                }
                if (group.shouldUmount) {
                    AlphaGrantBadge(label = "UMOUNT", active = false)
                }
            }
        }
    }
}

@Composable
private fun AlphaGrantBadge(
    label: String,
    active: Boolean,
) {
    Text(
        text = label,
        color = if (active) AlphaColors.Accent else AlphaColors.Muted,
        fontSize = alphaSp(9.5f, maxScale = 1.0f),
        fontWeight = alphaStrongWeight(),
        modifier = Modifier
            .clip(AlphaShapes.Control)
            .background(if (active) AlphaColors.AccentSoft else AlphaColors.SurfaceStrong)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AlphaEmptyCard(text: String) {
    AlphaCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = AlphaColors.Muted,
                fontSize = alphaSp(14f),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AlphaLoadErrorCard(onRetry: () -> Unit) {
    AlphaCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.superuser_failed_to_load),
                color = AlphaColors.Muted,
                fontSize = alphaSp(14f),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AlphaOutlinedButton(
                text = stringResource(R.string.network_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
