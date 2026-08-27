package me.weishu.kernelsu.ui.component.rebootlistpopup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.KsuIsValid
import me.weishu.kernelsu.ui.screen.home.HomeMetricCardWallpaperBackground
import me.weishu.kernelsu.ui.screen.home.HomeMetricCardWallpaperTarget
import me.weishu.kernelsu.ui.screen.home.rememberHomeMetricCardWallpaperBitmap
import me.weishu.kernelsu.ui.screen.home.rememberHomeMetricCardWallpaperState

@Composable
fun RebootDropdownItems(
    contentColor: Color? = null,
    onItemClick: (String) -> Unit,
) {
    val options = getRebootListOption()
    options.forEachIndexed { index, option ->
        DropdownMenuItem(
            selected = false,
            onClick = { onItemClick(option.reason) },
            text = {
                Text(
                    text = "  " + stringResource(option.labelRes),
                    color = contentColor ?: Color.Unspecified,
                )
            },
            shapes = MenuDefaults.itemShape(index = index, count = options.size),
        )
    }
}

@Composable
fun RebootListPopupMaterial() {
    var expanded by remember { mutableStateOf(false) }
    val wallpaperState = rememberHomeMetricCardWallpaperState(
        target = HomeMetricCardWallpaperTarget.RebootMenu,
        onWallpaperSelected = {},
    )
    val wallpaperBitmap = rememberHomeMetricCardWallpaperBitmap(
        uriString = wallpaperState.uriString,
        crop = wallpaperState.crop,
    )
    val hasWallpaper = wallpaperBitmap != null || !wallpaperState.videoUriString.isNullOrBlank()

    KsuIsValid {
        val onReboot = rememberRebootAction()

        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = stringResource(id = R.string.reboot)
            )
        }

        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShapes(),
                containerColor = if (hasWallpaper) {
                    Color.Transparent
                } else {
                    MenuDefaults.groupStandardContainerColor
                },
                tonalElevation = if (hasWallpaper) 0.dp else MenuDefaults.TonalElevation,
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(modifier = Modifier.widthIn(min = 196.dp)) {
                    HomeMetricCardWallpaperBackground(
                        bitmap = wallpaperBitmap,
                        videoUriString = wallpaperState.videoUriString,
                        videoCrop = wallpaperState.crop,
                        visualSettings = wallpaperState.visualSettings,
                    )
                    Column(
                        modifier = Modifier.padding(MenuDefaults.DropdownMenuGroupContentPadding),
                    ) {
                        RebootDropdownItems(
                            contentColor = Color.White.takeIf { hasWallpaper },
                        ) { reason ->
                            expanded = false
                            onReboot(reason)
                        }
                    }
                }
            }
        }
    }
}
