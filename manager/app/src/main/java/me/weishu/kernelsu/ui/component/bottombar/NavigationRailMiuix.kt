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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import me.weishu.kernelsu.ui.component.pixel.isPixelInterfaceStyle
import me.weishu.kernelsu.ui.component.pixel.pixelNavigationContainerColor
import me.weishu.kernelsu.ui.component.pixel.pixelNavigationSurface
import me.weishu.kernelsu.ui.component.ink.inkNavigationContainerColor
import me.weishu.kernelsu.ui.component.ink.inkNavigationIndicator
import me.weishu.kernelsu.ui.component.ink.inkNavigationSurface
import me.weishu.kernelsu.ui.component.ink.isInkInterfaceStyle
import me.weishu.kernelsu.ui.component.snow.seasonNavigationContainerColor
import me.weishu.kernelsu.ui.component.snow.seasonNavigationIndicator
import me.weishu.kernelsu.ui.component.snow.seasonNavigationSurface
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.CustomNavigationIconState
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun NavigationRailMiuix(
    blurBackdrop: LayerBackdrop?,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    val mainState = LocalMainPagerState.current
    val customIcons = LocalCustomNavigationIcons.current
    val railColor = when {
        isPixelInterfaceStyle() -> pixelNavigationContainerColor()
        isInkInterfaceStyle() -> inkNavigationContainerColor()
        blurBackdrop != null -> Color.Transparent
        isSnowInterfaceStyle() -> seasonNavigationContainerColor()
        else -> MiuixTheme.colorScheme.surface
    }

    BlurredBar(blurBackdrop, blurActive = !isPixelInterfaceStyle()) {
        if (customIcons.hasCustomization) {
            MiuixCustomNavigationRail(
                blurBackdrop = blurBackdrop,
                modifier = modifier,
                selectedIndex = mainState.selectedPage,
                navigationBadge = navigationBadge,
                onSelected = mainState::animateToPage,
            )
        } else {
            val items = BottomBarDestination.entries.map { destination ->
                Pair(customIcons[destination.slot].displayLabel(stringResource(destination.label)), destination.icon)
            }
            NavigationRail(
                modifier = modifier
                    .navigationRailDecoration()
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
                        badge = navigationBadgeFor(index, navigationBadge),
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
    navigationBadge: NavigationBadgeState,
    onSelected: (Int) -> Unit,
) {
    val customIcons = LocalCustomNavigationIcons.current
    val railColor = when {
        isPixelInterfaceStyle() -> pixelNavigationContainerColor()
        isInkInterfaceStyle() -> inkNavigationContainerColor()
        blurBackdrop != null -> Color.Transparent
        isSnowInterfaceStyle() -> seasonNavigationContainerColor()
        else -> MiuixTheme.colorScheme.surface
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(82.dp)
            .background(railColor)
            .navigationRailDecoration()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        BottomBarDestination.entries.forEachIndexed { index, destination ->
            MiuixCustomNavigationRailItem(
                destination = destination,
                state = customIcons[destination.slot],
                selected = selectedIndex == index,
                badge = navigationBadgeFor(index, navigationBadge),
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
    badge: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    val label = state.displayLabel(stringResource(destination.label))
    val isSnowStyle = isSnowInterfaceStyle()
    val isInkStyle = isInkInterfaceStyle()
    val itemShape = if (isSnowStyle || isInkStyle) RoundedCornerShape(10.dp) else CircleShape
    val iconTint = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    }
    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .width(72.dp)
            .clip(itemShape)
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
                .then(
                    if (selected && isSnowStyle) {
                        Modifier.seasonNavigationIndicator(itemShape)
                    } else if (selected && isInkStyle) {
                        Modifier.inkNavigationIndicator(itemShape, interactionKey = destination)
                    } else {
                        Modifier.background(
                            if (selected) {
                                MiuixTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                Color.Transparent
                            },
                            itemShape,
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            val icon: @Composable () -> Unit = {
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
            if (badge != null) {
                BadgedBox(badge = { badge() }) { icon() }
            } else {
                icon()
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

@Composable
private fun Modifier.navigationRailDecoration(): Modifier {
    val shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
    return when {
        isPixelInterfaceStyle() -> pixelNavigationSurface(shape)
        isInkInterfaceStyle() -> inkNavigationSurface(shape, paintBackground = false)
        isSnowInterfaceStyle() -> seasonNavigationSurface(shape, paintBackground = false)
        else -> this
    }
}
