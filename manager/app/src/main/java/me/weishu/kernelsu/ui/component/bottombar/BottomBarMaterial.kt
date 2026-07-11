package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.component.CustomNavigationIconImage
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.CustomNavigationIconSlot
import me.weishu.kernelsu.ui.util.LocalCustomNavigationIcons

@Composable
fun BottomBarMaterial() {
    val mainPagerState = LocalMainPagerState.current

    val items = listOf(
        MaterialBottomDestination(CustomNavigationIconSlot.Home, Icons.Filled.Home, Icons.Outlined.Home),
        MaterialBottomDestination(CustomNavigationIconSlot.Superuser, Icons.Filled.Shield, Icons.Outlined.Shield),
        MaterialBottomDestination(CustomNavigationIconSlot.Module, Icons.Filled.Extension, Icons.Outlined.Extension),
        MaterialBottomDestination(CustomNavigationIconSlot.Settings, Icons.Filled.Settings, Icons.Outlined.Settings),
    )

    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barColor = if (isInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
    } else {
        Color.White.copy(alpha = 0.96f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 10.dp + navPadding)
            .height(72.dp)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .background(barColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            items.forEachIndexed { index, item ->
                val selected = mainPagerState.selectedPage == index
                MaterialBottomItem(
                    item = item,
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            mainPagerState.animateToPage(index)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MaterialBottomItem(
    item: MaterialBottomDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentDescription = stringResource(item.slot.labelRes)
    val customIcons = LocalCustomNavigationIcons.current
    val selectedContainer = if (isInDarkTheme()) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        Color(0xFFE6E0FF)
    }
    val selectedTint = MaterialTheme.colorScheme.primary
    val unselectedTint = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(selectedContainer, CircleShape)
            )
        }
        CustomNavigationIconImage(
            state = customIcons[item.slot],
            contentDescription = contentDescription,
            modifier = Modifier.size(28.dp),
            alpha = if (selected) 1f else 0.68f,
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = contentDescription,
                modifier = Modifier.size(26.dp),
                tint = if (selected) selectedTint else unselectedTint,
            )
        }
    }
}

private data class MaterialBottomDestination(
    val slot: CustomNavigationIconSlot,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)
