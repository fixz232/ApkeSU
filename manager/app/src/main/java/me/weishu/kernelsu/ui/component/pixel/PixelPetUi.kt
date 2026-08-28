package me.weishu.kernelsu.ui.component.pixel

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.isInDarkTheme

enum class PixelPetPage(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Companion(R.string.pixel_pet_page_companion, Icons.Rounded.Pets),
    Habitat(R.string.pixel_pet_page_habitat, Icons.Rounded.Landscape),
    Store(R.string.pixel_pet_page_store, Icons.Rounded.Storefront),
    Growth(R.string.pixel_pet_page_growth, Icons.Rounded.AutoGraph),
}

@Immutable
data class PixelPetUiColors(
    val background: Color,
    val panel: Color,
    val panelRaised: Color,
    val selected: Color,
    val outline: Color,
    val accent: Color,
    val secondary: Color,
    val content: Color,
    val mutedContent: Color,
)

@Composable
fun pixelPetUiColors(): PixelPetUiColors {
    val palette = pixelPalette(PixelStyle.PetCompanion, isInDarkTheme())
    val scheme = MaterialTheme.colorScheme
    return PixelPetUiColors(
        background = palette.background.copy(alpha = 0.30f),
        panel = palette.surface.copy(alpha = if (isInDarkTheme()) 0.88f else 0.92f),
        panelRaised = palette.surfaceAlt.copy(alpha = if (isInDarkTheme()) 0.92f else 0.86f),
        selected = palette.primary.copy(alpha = if (isInDarkTheme()) 0.24f else 0.16f),
        outline = palette.outline.copy(alpha = if (isInDarkTheme()) 0.76f else 0.58f),
        accent = palette.primary,
        secondary = palette.secondary,
        content = scheme.onSurface,
        mutedContent = scheme.onSurfaceVariant,
    )
}

@Composable
fun PixelPetScreenBackdrop(modifier: Modifier = Modifier) {
    val colors = pixelPetUiColors()
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(colors.background)
        val pixel = 2.dp.toPx()
        val stepX = size.width / 7f
        val stepY = size.height / 11f
        repeat(7) { column ->
            repeat(11) { row ->
                if ((column * 3 + row * 5) % 7 == 0) {
                    drawRect(
                        color = if ((column + row) % 2 == 0) {
                            colors.accent.copy(alpha = 0.055f)
                        } else {
                            colors.secondary.copy(alpha = 0.045f)
                        },
                        topLeft = Offset(stepX * column + pixel, stepY * row + pixel),
                        size = Size(pixel, pixel),
                    )
                }
            }
        }
    }
}

@Composable
fun PixelPetPageBar(
    selected: PixelPetPage,
    onSelected: (PixelPetPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = pixelPetUiColors()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.panel,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline),
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            PixelPetPage.entries.forEach { page ->
                val active = page == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .selectable(
                            selected = active,
                            role = Role.Tab,
                            onClick = { onSelected(page) },
                        ),
                    color = if (active) colors.selected else Color.Transparent,
                    shape = RoundedCornerShape(5.dp),
                    border = if (active) {
                        androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.72f))
                    } else {
                        null
                    },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = if (active) colors.accent else colors.mutedContent,
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(page.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) colors.content else colors.mutedContent,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PixelPetPanel(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = pixelPetUiColors()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            icon?.let {
                Icon(it, null, tint = colors.accent)
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.content,
            )
            trailing?.invoke(this)
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.panel,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    content()
                }
                Canvas(Modifier.matchParentSize()) {
                    val mark = 5.dp.toPx()
                    val stroke = 1.dp.toPx()
                    val color = colors.accent.copy(alpha = 0.56f)
                    drawLine(color, Offset.Zero, Offset(mark, 0f), stroke)
                    drawLine(color, Offset.Zero, Offset(0f, mark), stroke)
                    drawLine(color, Offset(size.width, size.height), Offset(size.width - mark, size.height), stroke)
                    drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - mark), stroke)
                }
            }
        }
    }
}

@Composable
fun PixelPetItemSurface(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = pixelPetUiColors()
    val interactionModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    Surface(
        modifier = interactionModifier.fillMaxWidth(),
        color = if (selected) colors.selected else colors.panelRaised,
        shape = RoundedCornerShape(5.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) colors.accent.copy(alpha = 0.68f) else colors.outline.copy(alpha = 0.52f),
        ),
        content = content,
    )
}

@Composable
fun PixelPetPreviewFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = pixelPetUiColors()
    Surface(
        modifier = modifier,
        color = colors.panelRaised,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline),
        content = content,
    )
}
