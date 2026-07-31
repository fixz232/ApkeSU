package me.weishu.kernelsu.ui.component.rebootlistpopup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.KsuIsValid
import me.weishu.kernelsu.ui.component.ListPopupDefaults
import me.weishu.kernelsu.ui.screen.home.HomeMetricCardWallpaperBackground
import me.weishu.kernelsu.ui.screen.home.HomeMetricCardWallpaperTarget
import me.weishu.kernelsu.ui.screen.home.rememberHomeMetricCardWallpaperBitmap
import me.weishu.kernelsu.ui.screen.home.rememberHomeMetricCardWallpaperState
import top.yukonga.miuix.kmp.basic.DropdownColors
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close2
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun RebootListPopupMiuix(
    modifier: Modifier = Modifier,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.TopEnd,
    tint: Color = colorScheme.onBackground,
    enabled: Boolean = true,
) {
    val showTopPopup = remember { mutableStateOf(false) }
    val wallpaperState = rememberHomeMetricCardWallpaperState(
        target = HomeMetricCardWallpaperTarget.RebootMenu,
        onWallpaperSelected = {},
    )
    val wallpaperBitmap = rememberHomeMetricCardWallpaperBitmap(
        uriString = wallpaperState.uriString,
        crop = wallpaperState.crop,
    )
    val hasWallpaper = wallpaperBitmap != null || !wallpaperState.videoUriString.isNullOrBlank()
    val dropdownColors = if (hasWallpaper) {
        DropdownDefaults.dropdownColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
        )
    } else {
        DropdownDefaults.dropdownColors()
    }

    KsuIsValid {
        val onReboot = rememberRebootAction()
        IconButton(
            modifier = modifier,
            enabled = enabled,
            onClick = { showTopPopup.value = true },
            holdDownState = showTopPopup.value
        ) {
            Icon(
                imageVector = MiuixIcons.Close2,
                contentDescription = stringResource(id = R.string.reboot),
                tint = tint
            )
        }
        OverlayListPopup(
            show = showTopPopup.value,
            popupPositionProvider = ListPopupDefaults.MenuPositionProvider,
            alignment = alignment,
            onDismissRequest = {
                showTopPopup.value = false
            },
            content = {
                val rebootOptions = getRebootListOption()

                Box(
                    modifier = Modifier
                        .widthIn(min = 196.dp)
                ) {
                    HomeMetricCardWallpaperBackground(
                        bitmap = wallpaperBitmap,
                        videoUriString = wallpaperState.videoUriString,
                        videoCrop = wallpaperState.crop,
                    )
                    ListPopupColumn {
                        rebootOptions.forEachIndexed { idx, option ->
                            RebootDropdownItem(
                                option = option,
                                showTopPopup = showTopPopup,
                                optionSize = rebootOptions.size,
                                index = idx,
                                modifier = Modifier.fillMaxWidth(),
                                dropdownColors = dropdownColors,
                                onReboot = onReboot,
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun RebootDropdownItem(
    option: RebootListOption,
    showTopPopup: MutableState<Boolean>,
    optionSize: Int,
    index: Int,
    modifier: Modifier = Modifier,
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
    onReboot: (String) -> Unit,
) {
    me.weishu.kernelsu.ui.component.miuix.DropdownItem(
        text = stringResource(option.labelRes),
        optionSize = optionSize,
        modifier = modifier,
        dropdownColors = dropdownColors,
        onSelectedIndexChange = {
            showTopPopup.value = false
            onReboot(option.reason)
        },
        index = index
    )
}
