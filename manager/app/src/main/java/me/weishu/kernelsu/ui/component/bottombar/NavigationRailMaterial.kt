package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.component.CustomNavigationIconImage
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons

@Composable
fun NavigationRailMaterial(
    modifier: Modifier = Modifier,
) {
    val mainPagerState = LocalMainPagerState.current

    val items = listOf(
        MaterialRailDestination(CustomNavigationIconSlot.Home, Icons.Filled.Home, Icons.Outlined.Home),
        MaterialRailDestination(CustomNavigationIconSlot.Superuser, Icons.Filled.Shield, Icons.Outlined.Shield),
        MaterialRailDestination(CustomNavigationIconSlot.Module, Icons.Filled.Extension, Icons.Outlined.Extension),
        MaterialRailDestination(CustomNavigationIconSlot.Settings, Icons.Filled.Settings, Icons.Outlined.Settings),
    )
    val customIcons = LocalCustomNavigationIcons.current

    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Start + WindowInsetsSides.Vertical
        )
    ) {
        Spacer(Modifier.weight(1f))
        items.forEachIndexed { index, item ->
            val selected = mainPagerState.selectedPage == index
            val label = stringResource(item.slot.labelRes)
            NavigationRailItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        mainPagerState.animateToPage(index)
                    }
                },
                icon = {
                    CustomNavigationIconImage(
                        state = customIcons[item.slot],
                        contentDescription = label,
                        modifier = Modifier.size(26.dp),
                        alpha = if (selected) 1f else 0.68f,
                    ) {
                        Icon(
                            if (selected) item.selectedIcon else item.unselectedIcon,
                            label
                        )
                    }
                },
                label = { Text(label) }
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

private data class MaterialRailDestination(
    val slot: CustomNavigationIconSlot,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)
