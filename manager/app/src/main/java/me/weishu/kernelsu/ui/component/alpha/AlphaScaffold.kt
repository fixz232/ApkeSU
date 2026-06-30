package me.weishu.kernelsu.ui.component.alpha

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.SwitchStyle

private data class AlphaPalette(
    val background: Color,
    val topBar: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val accent: Color,
    val accentSoft: Color,
    val text: Color,
    val muted: Color,
    val disabled: Color,
    val divider: Color,
)

object AlphaColors {
    private val Alpha = AlphaPalette(
        background = Color(0xFFF7F7F7),
        topBar = Color(0xFFFAFAFA),
        surface = Color(0xFFEFEFEF),
        surfaceStrong = Color(0xFFE9E9E9),
        accent = Color(0xFF3E86BE),
        accentSoft = Color(0xFFC8E2F3),
        text = Color(0xFF303236),
        muted = Color(0xFF666A70),
        disabled = Color(0xFFB8B8B8),
        divider = Color(0xFFFFFFFF),
    )

    private val Delta = AlphaPalette(
        background = Color(0xFFFCF9FC),
        topBar = Color(0xFFFCF9FC),
        surface = Color(0xFFF0F3EE),
        surfaceStrong = Color(0xFFDDEADD),
        accent = Color(0xFF2E8B3F),
        accentSoft = Color(0xFFA9DFAE),
        text = Color(0xFF171A17),
        muted = Color(0xFF4D574D),
        disabled = Color(0xFF9AA49A),
        divider = Color(0xFFD7E1D6),
    )

    val Background: Color
        @Composable
        @ReadOnlyComposable
        get() = current.background

    val TopBar: Color
        @Composable
        @ReadOnlyComposable
        get() = current.topBar

    val Surface: Color
        @Composable
        @ReadOnlyComposable
        get() = current.surface

    val SurfaceStrong: Color
        @Composable
        @ReadOnlyComposable
        get() = current.surfaceStrong

    val Accent: Color
        @Composable
        @ReadOnlyComposable
        get() = current.accent

    val AccentSoft: Color
        @Composable
        @ReadOnlyComposable
        get() = current.accentSoft

    val Text: Color
        @Composable
        @ReadOnlyComposable
        get() = current.text

    val Muted: Color
        @Composable
        @ReadOnlyComposable
        get() = current.muted

    val Disabled: Color
        @Composable
        @ReadOnlyComposable
        get() = current.disabled

    val Divider: Color
        @Composable
        @ReadOnlyComposable
        get() = current.divider

    private val current: AlphaPalette
        @Composable
        @ReadOnlyComposable
        get() = if (isDeltaStyle()) Delta else Alpha
}

object AlphaShapes {
    val Card: Shape
        @Composable
        @ReadOnlyComposable
        get() = if (isDeltaStyle()) RoundedCornerShape(30.dp) else RoundedCornerShape(8.dp)

    val Control: Shape
        @Composable
        @ReadOnlyComposable
        get() = if (isDeltaStyle()) RoundedCornerShape(26.dp) else RoundedCornerShape(8.dp)

    val Button: Shape
        @Composable
        @ReadOnlyComposable
        get() = if (isDeltaStyle()) CircleShape else RoundedCornerShape(8.dp)

    val BottomBar: Shape
        @Composable
        @ReadOnlyComposable
        get() = if (isDeltaStyle()) RoundedCornerShape(36.dp) else RoundedCornerShape(0.dp)

    val NavItem: Shape
        @Composable
        @ReadOnlyComposable
        get() = if (isDeltaStyle()) RoundedCornerShape(28.dp) else RoundedCornerShape(0.dp)
}

@Composable
@ReadOnlyComposable
fun isDeltaStyle(): Boolean {
    return LocalInterfaceStyle.current == InterfaceStyle.Delta.value
}

@Composable
fun alphaSp(value: Float, maxScale: Float = 1.12f): TextUnit {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(0.85f)
    val cappedScale = fontScale.coerceAtMost(maxScale)
    return (value * cappedScale / fontScale).sp
}

@Composable
fun AlphaScreen(
    title: String,
    bottomInnerPadding: Dp,
    topActionIcon: ImageVector? = null,
    onTopActionClick: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AlphaColors.Background)
    ) {
        AlphaTopBar(
            title = title,
            actionIcon = topActionIcon,
            onActionClick = onTopActionClick,
        )
        Box(modifier = Modifier.weight(1f)) {
            content(PaddingValues(bottom = bottomInnerPadding + 8.dp))
        }
    }
}

@Composable
fun AlphaTopBar(
    title: String,
    actionIcon: ImageVector? = null,
    onActionClick: () -> Unit = {},
) {
    if (isDeltaStyle()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AlphaColors.TopBar)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .padding(bottom = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    color = AlphaColors.Text,
                    fontSize = alphaSp(28f, maxScale = 1.02f),
                    lineHeight = alphaSp(32f, maxScale = 1.02f),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (actionIcon != null) {
                    IconButton(
                        onClick = onActionClick,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = AlphaColors.Text,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 34.dp),
                horizontalArrangement = Arrangement.spacedBy(38.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(AlphaColors.Accent)
                    )
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp)
            .background(AlphaColors.TopBar)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .height(68.dp)
            .padding(start = 18.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = AlphaColors.Text,
            fontSize = alphaSp(24f, maxScale = 1.04f),
            lineHeight = alphaSp(28f, maxScale = 1.04f),
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionIcon != null) {
            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = AlphaColors.Text,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
fun AlphaCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable () -> Unit,
) {
    val shape = AlphaShapes.Card
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (isDeltaStyle()) 4.dp else 0.dp, shape)
            .clip(shape)
            .background(AlphaColors.Surface)
            .padding(contentPadding),
    ) {
        content()
    }
}

@Composable
fun AlphaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(AlphaShapes.Button)
            .background(if (enabled) AlphaColors.Accent else AlphaColors.Disabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = alphaSp(14f, maxScale = 1.04f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AlphaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(AlphaShapes.Button)
            .background(if (isDeltaStyle()) AlphaColors.AccentSoft else AlphaColors.Background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AlphaColors.Accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = if (isDeltaStyle()) AlphaColors.Text else AlphaColors.Accent,
            fontSize = alphaSp(14f, maxScale = 1.04f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AlphaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val switchStyle = LocalSwitchStyle.current
    if (switchStyle == SwitchStyle.Original) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier.scale(0.88f),
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AlphaColors.Accent,
                checkedTrackColor = AlphaColors.AccentSoft,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD8D8D8),
            ),
        )
    } else {
        StyledSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            style = switchStyle,
        )
    }
}

@Composable
fun AlphaBottomBar(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val delta = isDeltaStyle()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (delta) 40.dp else 0.dp)
            .padding(bottom = if (delta) navBottom + 12.dp else navBottom)
            .height(if (delta) 68.dp else 60.dp)
            .shadow(if (delta) 10.dp else 8.dp, AlphaShapes.BottomBar)
            .clip(AlphaShapes.BottomBar)
            .background(if (delta) AlphaColors.Surface.copy(alpha = 0.94f) else AlphaColors.TopBar)
            .padding(horizontal = if (delta) 8.dp else 4.dp, vertical = if (delta) 7.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlphaNavDestination.entries.forEachIndexed { index, destination ->
            AlphaNavItem(
                destination = destination,
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AlphaNavItem(
    destination: AlphaNavDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isDeltaStyle()) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .clip(AlphaShapes.NavItem)
                .background(if (selected) AlphaColors.AccentSoft else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = stringResource(destination.label),
                tint = AlphaColors.Text,
                modifier = Modifier.size(29.dp),
            )
            if (selected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(destination.label),
                    color = AlphaColors.Text,
                    fontSize = alphaSp(14f, maxScale = 1.0f),
                    lineHeight = alphaSp(16f, maxScale = 1.0f),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        val color = if (selected) AlphaColors.Accent else AlphaColors.Disabled
        Column(
            modifier = modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = stringResource(destination.label),
                tint = color,
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = stringResource(destination.label),
                color = color,
                fontSize = alphaSp(12f, maxScale = 1.0f),
                lineHeight = alphaSp(14f, maxScale = 1.0f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

enum class AlphaNavDestination(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Home(R.string.home, Icons.Rounded.Home),
    SuperUser(R.string.superuser, Icons.Rounded.Security),
    Module(R.string.module, Icons.Rounded.Extension),
    Settings(R.string.settings, Icons.Rounded.Settings),
}
