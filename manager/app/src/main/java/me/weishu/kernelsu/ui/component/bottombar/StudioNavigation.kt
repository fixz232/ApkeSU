package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.component.CustomNavigationIconImage
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private val StudioNavigationItemShape = RoundedCornerShape(8.dp)

@Composable
fun StudioBottomBar(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val destinations = BottomBarDestination.entries.toList()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surface),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colorScheme.outline.copy(alpha = 0.18f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEachIndexed { index, destination ->
                StudioBottomBarItem(
                    destination = destination,
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                )
            }
        }
        Spacer(modifier = Modifier.height(navigationPadding))
    }
}

@Composable
private fun RowScope.StudioBottomBarItem(
    destination: BottomBarDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val customIcons = LocalCustomNavigationIcons.current
    val label = stringResource(destination.label)
    val tint = if (selected) colorScheme.primary else colorScheme.onSurfaceVariantSummary
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(StudioNavigationItemShape)
            .background(if (selected) colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CustomNavigationIconImage(
            state = customIcons[destination.slot],
            contentDescription = label,
            modifier = Modifier.size(19.dp),
            alpha = if (selected) 1f else 0.72f,
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun StudioNavigationRail(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val destinations = BottomBarDestination.entries.toList()
    val customIcons = LocalCustomNavigationIcons.current
    Column(
        modifier = modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(colorScheme.surface)
            .padding(top = topPadding + 14.dp, bottom = bottomPadding + 12.dp, start = 10.dp, end = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(StudioNavigationItemShape)
                .background(colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "A",
                color = colorScheme.onPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        destinations.forEachIndexed { index, destination ->
            val selected = selectedIndex == index
            val label = stringResource(destination.label)
            val tint = if (selected) colorScheme.primary else colorScheme.onSurfaceVariantSummary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(StudioNavigationItemShape)
                    .background(if (selected) colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable(role = Role.Tab, onClick = { onSelected(index) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CustomNavigationIconImage(
                    state = customIcons[destination.slot],
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                    alpha = if (selected) 1f else 0.72f,
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    color = tint,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
