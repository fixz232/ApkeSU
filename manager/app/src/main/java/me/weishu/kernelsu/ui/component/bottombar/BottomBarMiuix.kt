package me.weishu.kernelsu.ui.component.bottombar

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.component.CustomNavigationIconImage
import me.weishu.kernelsu.ui.component.FloatingBottomBar
import me.weishu.kernelsu.ui.component.FloatingBottomBarItem
import me.weishu.kernelsu.ui.component.liquid.isLiquidGlassTheme
import me.weishu.kernelsu.ui.component.liquid.liquidGlassSurfaceColor
import me.weishu.kernelsu.ui.component.pixel.isPixelInterfaceStyle
import me.weishu.kernelsu.ui.component.pixel.pixelNavigationContainerColor
import me.weishu.kernelsu.ui.component.pixel.pixelNavigationSurface
import me.weishu.kernelsu.ui.component.snow.isSnowInterfaceStyle
import me.weishu.kernelsu.ui.component.snow.seasonNavigationContainerColor
import me.weishu.kernelsu.ui.component.snow.seasonNavigationIndicator
import me.weishu.kernelsu.ui.component.snow.seasonNavigationSurface
import me.weishu.kernelsu.ui.theme.LocalEnableFloatingBottomBar
import me.weishu.kernelsu.ui.theme.LocalEnableFloatingBottomBarBlur
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.CustomNavigationIconState
import me.weishu.kernelsu.ui.util.CustomNavigationIconSet
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BottomBarMiuix(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop?,
    modifier: Modifier,
) {
    val mainState = LocalMainPagerState.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val isLiquidGlass = isLiquidGlassTheme()
    val isSnowStyle = isSnowInterfaceStyle()
    val isPixelStyle = isPixelInterfaceStyle()

    val destinations = BottomBarDestination.entries.toList()
    val customIcons = LocalCustomNavigationIcons.current
    val barColor = if (isPixelStyle) {
        pixelNavigationContainerColor()
    } else if (blurBackdrop != null) {
        Color.Transparent
    } else if (isLiquidGlass) {
        liquidGlassSurfaceColor().copy(alpha = 0.72f)
    } else if (isSnowStyle) {
        seasonNavigationContainerColor()
    } else {
        MiuixTheme.colorScheme.surface
    }
    if (!enableFloatingBottomBar) {
        val navigationShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        val navigationModifier = modifier.then(
            when {
                isPixelStyle -> Modifier.pixelNavigationSurface(navigationShape)
                isSnowStyle -> Modifier.seasonNavigationSurface(navigationShape, paintBackground = false)
                else -> Modifier
            },
        )
        BlurredBar(blurBackdrop, blurActive = !isPixelStyle) {
            if (customIcons.hasSelected) {
                MiuixCustomNavigationBar(
                    modifier = navigationModifier,
                    color = barColor,
                    destinations = destinations,
                    customIcons = customIcons,
                    selectedIndex = mainState.selectedPage,
                    onSelected = mainState::animateToPage,
                )
            } else {
                val items = destinations.map { destination ->
                    NavigationItem(
                        label = stringResource(destination.label),
                        icon = destination.icon,
                    )
                }
                NavigationBar(
                    modifier = navigationModifier,
                    color = barColor,
                    content = {
                        items.forEachIndexed { index, item ->
                            NavigationBarItem(
                                modifier = Modifier.weight(1f),
                                icon = item.icon,
                                label = item.label,
                                selected = mainState.selectedPage == index,
                                onClick = {
                                    mainState.animateToPage(index)
                                }
                            )
                        }
                    }
                )
            }
        }
    } else {
        FloatingBottomBar(
            modifier = modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            selectedIndex = { mainState.selectedPage },
            onSelected = { mainState.animateToPage(it) },
            backdrop = backdrop,
            tabsCount = destinations.size,
            isBlurEnabled = enableFloatingBottomBarBlur && backdrop != null,
        ) {
            destinations.forEachIndexed { index, destination ->
                val label = stringResource(destination.label)
                FloatingBottomBarItem(
                    onClick = {
                        mainState.animateToPage(index)
                    },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                ) {
                    CustomNavigationIconImage(
                        state = customIcons[destination.slot],
                        contentDescription = label,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = label,
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixCustomNavigationBar(
    modifier: Modifier,
    color: Color,
    destinations: List<BottomBarDestination>,
    customIcons: CustomNavigationIconSet,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEachIndexed { index, destination ->
                MiuixCustomNavigationBarItem(
                    destination = destination,
                    state = customIcons[destination.slot],
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                )
            }
        }
        Spacer(modifier = Modifier.height(navPadding))
    }
}

@Composable
private fun RowScope.MiuixCustomNavigationBarItem(
    destination: BottomBarDestination,
    state: CustomNavigationIconState,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(destination.label)
    val isSnowStyle = isSnowInterfaceStyle()
    val itemShape = if (isSnowStyle) RoundedCornerShape(10.dp) else CircleShape
    val iconTint = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(itemShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .then(
                    if (selected && isSnowStyle) {
                        Modifier.seasonNavigationIndicator(itemShape)
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
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}

enum class BottomBarDestination(
    @get:StringRes val label: Int,
    val icon: ImageVector,
    val slot: CustomNavigationIconSlot,
) {
    Home(R.string.home, Icons.Rounded.Cottage, CustomNavigationIconSlot.Home),
    SuperUser(R.string.superuser, Icons.Rounded.Security, CustomNavigationIconSlot.Superuser),
    Module(R.string.module, Icons.Rounded.Extension, CustomNavigationIconSlot.Module),
    Setting(R.string.settings, Icons.Rounded.Settings, CustomNavigationIconSlot.Settings)
}
