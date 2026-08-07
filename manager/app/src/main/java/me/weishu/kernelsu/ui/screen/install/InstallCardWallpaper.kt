package me.weishu.kernelsu.ui.screen.install

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import me.weishu.kernelsu.ui.component.CustomVideoBackground
import me.weishu.kernelsu.ui.component.MediaVisualLayer
import me.weishu.kernelsu.ui.component.rememberCustomImageBitmap
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.ThemeStoreImageSlot
import me.weishu.kernelsu.ui.util.ThemeStoreImageState
import me.weishu.kernelsu.ui.util.rememberThemeStoreImageState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val INSTALL_CARD_WALLPAPER_MAX_SIDE = 1200

@Composable
internal fun BoxScope.InstallCardWallpaperBackground(slot: ThemeStoreImageSlot) {
    InstallCardWallpaperBackground(state = rememberThemeStoreImageState(slot))
}

@Composable
internal fun BoxScope.InstallCardWallpaperBackground(state: ThemeStoreImageState) {
    if (!state.hasSelected) return

    val darkTheme = isInDarkTheme()
    val nowMillis = produceState(System.currentTimeMillis(), state.variantSettings) {
        while (isActive) {
            value = System.currentTimeMillis()
            delay(30_000L)
        }
    }.value
    val active = state.activeVariant(darkTheme, nowMillis)
    val imageBitmap = rememberCustomImageBitmap(
        uriString = active.uriString,
        maxSide = INSTALL_CARD_WALLPAPER_MAX_SIDE,
        crop = active.crop,
    )
    val overlayColor = remember(darkTheme) {
        if (darkTheme) {
            Color.Black.copy(alpha = 0.10f)
        } else {
            Color.White.copy(alpha = 0.42f)
        }
    }

    Box(modifier = Modifier.matchParentSize()) {
        MediaVisualLayer(
            settings = active.visualSettings,
            modifier = Modifier.matchParentSize(),
        ) { colorFilter ->
            if (!active.videoUriString.isNullOrBlank()) {
                CustomVideoBackground(
                    uriString = active.videoUriString,
                    crop = active.crop,
                    drawOverlay = false,
                    modifier = Modifier.matchParentSize(),
                )
            } else if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(overlayColor)
        )
    }
}
