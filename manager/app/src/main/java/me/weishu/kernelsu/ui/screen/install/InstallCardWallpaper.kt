package me.weishu.kernelsu.ui.screen.install

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import me.weishu.kernelsu.ui.component.CustomVideoBackground
import me.weishu.kernelsu.ui.component.rememberCustomImageBitmap
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.ThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.ThemeStoreImageState
import me.weishu.kernelsu.ui.util.rememberThemeStoreImageState

private const val INSTALL_CARD_WALLPAPER_MAX_SIDE = 1200

@Composable
internal fun BoxScope.InstallCardWallpaperBackground(slot: ThemeStoreImageSlot) {
    InstallCardWallpaperBackground(state = rememberThemeStoreImageState(slot))
}

@Composable
internal fun BoxScope.InstallCardWallpaperBackground(state: ThemeStoreImageState) {
    if (!state.hasSelected) return

    val imageBitmap = rememberCustomImageBitmap(
        uriString = state.uriString,
        maxSide = INSTALL_CARD_WALLPAPER_MAX_SIDE,
        crop = state.crop,
    )
    val darkTheme = isInDarkTheme()
    val overlayColor = remember(darkTheme) {
        if (darkTheme) {
            Color.Black.copy(alpha = 0.54f)
        } else {
            Color.White.copy(alpha = 0.58f)
        }
    }

    Box(modifier = Modifier.matchParentSize()) {
        if (!state.videoUriString.isNullOrBlank()) {
            CustomVideoBackground(
                uriString = state.videoUriString,
                crop = state.crop,
                drawOverlay = false,
                touchPassthrough = true,
                modifier = Modifier.matchParentSize(),
            )
        } else if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(overlayColor)
        )
    }
}
