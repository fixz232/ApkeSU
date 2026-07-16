package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.component.CustomNavigationIconImage
import me.weishu.kernelsu.ui.component.snow.isSnowInterfaceStyle
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.CustomNavigationIconState
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun NavigationRailMiuix(
    blurBackdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    val mainState = LocalMainPagerState.current
    val customIcons = LocalCustomNavigationIcons.current
    val railColor = when {
        blurBackdrop != null -> Color.Transparent
        isSnowInterfaceStyle() -> MiuixTheme.colorScheme.surface.copy(alpha = 0.78f)
        else -> MiuixTheme.colorScheme.surface
    }

    BlurredBar(blurBackdrop) {
        if (customIcons.hasSelected) {
            MiuixCustomNavigationRail(
                blurBackdrop = blurBackdrop,
                modifier = modifier,
                selectedIndex = mainState.selectedPage,
                onSelected = mainState::animateToPage,
            )
        } else {
            val items = BottomBarDestination.entries.map { destination ->
                Pair(stringResource(destination.label), destination.icon)
            }
            NavigationRail(
                modifier = modifier
                    .fillMaxHeight(),
                color = railColor,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                items.forEachIndexed { index, (label, icon) ->
                    NavigationRailItem(
                        icon = icon,
                        label = label,
                        selected = mainState.selectedPage == index,
                        onClick = {
                            mainState.animateToPage(index)
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiuixCustomNavigationRail(
    blurBackdrop: LayerBackdrop?,
    modifier: Modifier,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    val customIcons = LocalCustomNavigationIcons.current
    val railColor = when {
        blurBackdrop != null -> Color.Transparent
        isSnowInterfaceStyle() -> MiuixTheme.colorScheme.surface.copy(alpha = 0.78f)
        else -> MiuixTheme.colorScheme.surface
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(82.dp)
            .background(railColor)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        BottomBarDestination.entries.forEachIndexed { index, destination ->
            MiuixCustomNavigationRailItem(
                destination = destination,
                state = customIcons[destination.slot],
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ColumnScope.MiuixCustomNavigationRailItem(
    destination: BottomBarDestination,
    state: CustomNavigationIconState,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(destination.label)
    val iconTint = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    }
    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .width(72.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            CustomNavigationIconImage(
                state = state,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                alpha = if (selected) 1f else 0.72f,
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = iconTint,
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
        )
    }
}
