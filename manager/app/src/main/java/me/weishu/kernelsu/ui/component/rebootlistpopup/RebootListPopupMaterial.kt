package me.weishu.kernelsu.ui.component.rebootlistpopup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.KsuIsValid
import me.weishu.kernelsu.ui.screen.home.HomeMetricCardWallpaperBackground
import me.weishu.kernelsu.ui.screen.home.HomeMetricCardWallpaperTarget
import me.weishu.kernelsu.ui.screen.home.rememberHomeMetricCardWallpaperBitmap
import me.weishu.kernelsu.ui.screen.home.rememberHomeMetricCardWallpaperState
import me.weishu.kernelsu.ui.util.reboot

@Composable
fun RebootDropdownItems(
    hasWallpaper: Boolean,
    onItemClick: (String) -> Unit,
) {
    getRebootListOption().forEach { option ->
        DropdownMenuItem(
            text = {
                Text(
                    text = "  " + stringResource(option.labelRes),
                    color = if (hasWallpaper) Color.White else Color.Unspecified,
                )
            },
            onClick = { onItemClick(option.reason) }
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
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = stringResource(id = R.string.reboot)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = if (hasWallpaper) Color.Transparent else Color.Unspecified,
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 196.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    HomeMetricCardWallpaperBackground(
                        bitmap = wallpaperBitmap,
                        videoUriString = wallpaperState.videoUriString,
                        videoCrop = wallpaperState.crop,
                    )
                    Column {
                        RebootDropdownItems(hasWallpaper = hasWallpaper) { reason ->
                            expanded = false
                            reboot(reason)
                        }
                    }
                }
            }
        }
    }
}
