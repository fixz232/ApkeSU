package me.weishu.kernelsu.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.util.CUSTOM_NAVIGATION_ICON_MAX_SIDE
import me.weishu.kernelsu.ui.util.CustomNavigationIconMask
import me.weishu.kernelsu.ui.util.CustomNavigationIconState

@Composable
fun CustomNavigationIconImage(
    state: CustomNavigationIconState,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    fallback: @Composable () -> Unit,
) {
    val value = state.normalized()
    val imageBitmap = rememberCustomImageBitmap(
        uriString = value.uriString,
        maxSide = CUSTOM_NAVIGATION_ICON_MAX_SIDE,
        crop = value.crop,
    )

    if (imageBitmap != null) {
        val maskModifier = when (value.mask) {
            CustomNavigationIconMask.Original -> Modifier
            CustomNavigationIconMask.Circle -> Modifier.clip(CircleShape)
            CustomNavigationIconMask.Square -> Modifier.clip(RoundedCornerShape(0.dp))
            CustomNavigationIconMask.RoundedSquare -> Modifier.clip(RoundedCornerShape(7.dp))
        }
        Image(
            modifier = modifier
                .graphicsLayer {
                    scaleX = value.sizeScale
                    scaleY = value.sizeScale
                    translationY = value.verticalOffsetDp.dp.toPx()
                }
                .padding(value.innerPaddingDp.dp)
                .then(maskModifier),
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            contentScale = if (value.mask == CustomNavigationIconMask.Original) ContentScale.Fit else ContentScale.Crop,
            alpha = (alpha * value.opacity).coerceIn(0f, 1f),
            colorFilter = value.tintArgb?.let { ColorFilter.tint(Color(it.toInt())) },
        )
    } else {
        fallback()
    }
}
