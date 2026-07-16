package me.weishu.kernelsu.ui.component.material

import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassSurface
import me.weishu.kernelsu.ui.component.liquid.liquidGlassMaterialCardColors

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    shape: Shape = MaterialTheme.shapes.large,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .globalLiquidGlassSurface(shape = shape)
            .uiDecoratedCard(shape = shape, enabled = enabled),
        enabled = enabled,
        colors = liquidGlassMaterialCardColors(containerColor = containerColor),
        shape = shape
    ) {
        content()
    }
}

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .globalLiquidGlassSurface(shape = shape)
            .uiDecoratedCard(shape = shape),
        colors = liquidGlassMaterialCardColors(containerColor = containerColor),
        shape = shape
    ) {
        content()
    }
}
