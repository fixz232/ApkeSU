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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.AppIconImage
import me.weishu.kernelsu.ui.component.delta.DeltaCard
import me.weishu.kernelsu.ui.component.delta.DeltaColors
import me.weishu.kernelsu.ui.component.delta.DeltaEmptyCard
import me.weishu.kernelsu.ui.component.delta.DeltaPillButton
import me.weishu.kernelsu.ui.component.delta.DeltaScreen
import me.weishu.kernelsu.ui.component.delta.DeltaSearchField
import me.weishu.kernelsu.ui.component.delta.DeltaShapes
import me.weishu.kernelsu.ui.component.delta.deltaSp

@Composable
fun SuperUserPagerDelta(
    uiState: SuperUserUiState,
    actions: SuperUserActions,
    bottomInnerPadding: Dp,
) {
    val searchText = uiState.searchStatus.searchText
    val apps = if (searchText.isBlank()) uiState.groupedApps else uiState.searchResults

    DeltaScreen(
        title = stringResource(R.string.superuser),
        icon = Icons.Rounded.Security,
        bottomInnerPadding = bottomInnerPadding,
        topActionIcon = Icons.Rounded.Fingerprint,
        onTopActionClick = actions.onOpenAppIdManager,
        topActionContentDescription = stringResource(R.string.app_id_manager_open),
        secondaryTopActionIcon = Icons.Rounded.AcUnit,
        onSecondaryTopActionClick = actions.onOpenAppFreeze,
        secondaryTopActionContentDescription = stringResource(R.string.app_freeze_open),
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 18.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DeltaSearchField(
                    searchText = searchText,
                    onSearchTextChange = actions.onSearchTextChange,
                    onClearSearch = actions.onClearSearch,
                )
            }
            if (uiState.error != null) {
                item {
                    DeltaGrantLoadError(onRetry = actions.onRefresh)
                }
            }
            if (apps.isEmpty() && uiState.error == null) {
                item {
                    DeltaEmptyCard(
                        text = if (uiState.hasLoaded) {
                            stringResource(R.string.superuser_empty)
                        } else {
                            stringResource(R.string.refresh_refresh)
                        },
                    )
                }
            } else {
                items(apps, key = { it.uid }) { group ->
                    DeltaGrantRow(
                        group = group,
                        onClick = { actions.onOpenProfile(group) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeltaGrantRow(
    group: GroupedApps,
    onClick: () -> Unit,
) {
    val title = group.ownerName ?: group.primary.label
    val summary = if (group.apps.size > 1) {
        stringResource(R.string.group_contains_apps, group.apps.size)
    } else {
        group.primary.packageName
    }
    DeltaCard(
        modifier = Modifier
            .height(94.dp)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconImage(
                packageInfo = group.primary.packageInfo,
                label = group.primary.label,
                modifier = Modifier
                    .size(52.dp)
                    .clip(DeltaShapes.SmallPill),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 6.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    color = DeltaColors.Ink,
                    fontSize = deltaSp(18f, maxScale = 1.0f),
                    lineHeight = deltaSp(22f, maxScale = 1.0f),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    color = DeltaColors.Muted,
                    fontSize = deltaSp(13.5f, maxScale = 1.0f),
                    lineHeight = deltaSp(17f, maxScale = 1.0f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DeltaGrantStatusBadge(
                granted = group.anyAllowSu,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = DeltaColors.Muted,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp),
            )
        }
    }
}

@Composable
private fun DeltaGrantStatusBadge(granted: Boolean) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (granted) DeltaColors.AccentSoft else DeltaColors.SurfaceDeep)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (granted) "ROOT" else "OFF",
            color = if (granted) DeltaColors.Ink else DeltaColors.Muted,
            fontSize = deltaSp(11f, maxScale = 1.0f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun DeltaGrantLoadError(onRetry: () -> Unit) {
    DeltaCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.superuser_failed_to_load),
                color = DeltaColors.Muted,
                fontSize = deltaSp(14f),
                fontWeight = FontWeight.Bold,
            )
            DeltaPillButton(
                text = stringResource(R.string.network_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
