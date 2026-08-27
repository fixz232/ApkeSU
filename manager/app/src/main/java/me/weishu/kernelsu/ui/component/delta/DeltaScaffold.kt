package me.weishu.kernelsu.ui.component.delta

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import me.weishu.kernelsu.ui.component.bottombar.MainDestination
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.LocalNightBackgroundEffectActive
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.theme.DeltaColorVariant
import me.weishu.kernelsu.ui.theme.LocalDeltaColorVariant
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.theme.isInDarkTheme

private data class DeltaPalette(
    val background: Color,
    val surface: Color,
    val surfaceDeep: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentMuted: Color,
    val ink: Color,
    val muted: Color,
    val disabled: Color,
    val divider: Color,
    val danger: Color,
)

object DeltaColors {
    private val GreenPalette = DeltaPalette(
        background = Color(0xFFFCF9FC),
        surface = Color(0xFFF0F3EE),
        surfaceDeep = Color(0xFFE5ECE3),
        accent = Color(0xFF2E873C),
        accentSoft = Color(0xFFA9DFAE),
        accentMuted = Color(0xFFDCEEDC),
        ink = Color(0xFF151915),
        muted = Color(0xFF4A544A),
        disabled = Color(0xFF8D988D),
        divider = Color(0xFFD8E2D7),
        danger = Color(0xFFD53934),
    )

    private val RedPalette = DeltaPalette(
        background = Color(0xFFFCF9FC),
        surface = Color(0xFFF5EEEE),
        surfaceDeep = Color(0xFFEDE1E1),
        accent = Color(0xFFC9342F),
        accentSoft = Color(0xFFF4B6B2),
        accentMuted = Color(0xFFF7DDDA),
        ink = Color(0xFF1A1515),
        muted = Color(0xFF5A4B4A),
        disabled = Color(0xFF9A8C8A),
        divider = Color(0xFFE4D5D2),
        danger = Color(0xFFB3261E),
    )

    private val GreenDarkPalette = DeltaPalette(
        background = Color(0xFF101511),
        surface = Color(0xFF1B231D),
        surfaceDeep = Color(0xFF263129),
        accent = Color(0xFF69D67B),
        accentSoft = Color(0xFF24502E),
        accentMuted = Color(0xFF1B3922),
        ink = Color(0xFFE8F2E9),
        muted = Color(0xFFA8B6AA),
        disabled = Color(0xFF68766A),
        divider = Color(0xFF354438),
        danger = Color(0xFFFF8A84),
    )

    private val RedDarkPalette = DeltaPalette(
        background = Color(0xFF171111),
        surface = Color(0xFF251B1B),
        surfaceDeep = Color(0xFF342525),
        accent = Color(0xFFFF827B),
        accentSoft = Color(0xFF5A2A28),
        accentMuted = Color(0xFF41201F),
        ink = Color(0xFFF5E8E7),
        muted = Color(0xFFC0AAA8),
        disabled = Color(0xFF7D6968),
        divider = Color(0xFF4B3635),
        danger = Color(0xFFFF9A94),
    )

    val Background: Color
        @Composable
        @ReadOnlyComposable
        get() = current.background

    val Surface: Color
        @Composable
        @ReadOnlyComposable
        get() = current.surface

    val SurfaceDeep: Color
        @Composable
        @ReadOnlyComposable
        get() = current.surfaceDeep

    val Accent: Color
        @Composable
        @ReadOnlyComposable
        get() = current.accent

    val AccentSoft: Color
        @Composable
        @ReadOnlyComposable
        get() = current.accentSoft

    val AccentMuted: Color
        @Composable
        @ReadOnlyComposable
        get() = current.accentMuted

    val Ink: Color
        @Composable
        @ReadOnlyComposable
        get() = current.ink

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

    val Danger: Color
        @Composable
        @ReadOnlyComposable
        get() = current.danger

    private val current: DeltaPalette
        @Composable
        @ReadOnlyComposable
        get() = when (DeltaColorVariant.fromValue(LocalDeltaColorVariant.current)) {
            DeltaColorVariant.Red -> if (isInDarkTheme()) RedDarkPalette else RedPalette
            DeltaColorVariant.Green -> if (isInDarkTheme()) GreenDarkPalette else GreenPalette
        }
}

object DeltaShapes {
    val Card = RoundedCornerShape(30.dp)
    val Control = RoundedCornerShape(28.dp)
    val SmallPill = RoundedCornerShape(18.dp)
    val BottomBar = RoundedCornerShape(34.dp)
}

@Composable
fun deltaSp(value: Float, maxScale: Float = 1.08f): TextUnit {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(0.85f)
    val cappedScale = fontScale.coerceAtMost(maxScale)
    return (value * cappedScale / fontScale).sp
}

@Composable
fun DeltaScreen(
    title: String,
    icon: ImageVector,
    bottomInnerPadding: Dp,
    topActionIcon: ImageVector? = null,
    onTopActionClick: () -> Unit = {},
    topActionContentDescription: String? = null,
    secondaryTopActionIcon: ImageVector? = null,
    onSecondaryTopActionClick: () -> Unit = {},
    secondaryTopActionContentDescription: String? = null,
    tertiaryTopActionIcon: ImageVector? = null,
    onTertiaryTopActionClick: () -> Unit = {},
    tertiaryTopActionContentDescription: String? = null,
    transparentChrome: Boolean = false,
    topBarVisible: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val background = if (transparentChrome) {
        Color.Transparent
    } else if (LocalNightBackgroundEffectActive.current) {
        DeltaColors.Background.copy(alpha = 0.84f)
    } else {
        DeltaColors.Background
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        AnimatedVisibility(
            visible = topBarVisible,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(180),
            ) + fadeIn(animationSpec = tween(150)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(160),
            ) + fadeOut(animationSpec = tween(120)),
        ) {
            DeltaTopBar(
                title = title,
                icon = icon,
                actionIcon = topActionIcon,
                onActionClick = onTopActionClick,
                actionContentDescription = topActionContentDescription,
                secondaryActionIcon = secondaryTopActionIcon,
                onSecondaryActionClick = onSecondaryTopActionClick,
                secondaryActionContentDescription = secondaryTopActionContentDescription,
                tertiaryActionIcon = tertiaryTopActionIcon,
                onTertiaryActionClick = onTertiaryTopActionClick,
                tertiaryActionContentDescription = tertiaryTopActionContentDescription,
                forceTransparent = transparentChrome,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            content(PaddingValues(bottom = bottomInnerPadding + 8.dp))
        }
    }
}

@Composable
fun DeltaTopBar(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    onActionClick: () -> Unit = {},
    actionContentDescription: String? = null,
    secondaryActionIcon: ImageVector? = null,
    onSecondaryActionClick: () -> Unit = {},
    secondaryActionContentDescription: String? = null,
    tertiaryActionIcon: ImageVector? = null,
    onTertiaryActionClick: () -> Unit = {},
    tertiaryActionContentDescription: String? = null,
    forceTransparent: Boolean = false,
) {
    val actionCount = listOfNotNull(actionIcon, secondaryActionIcon, tertiaryActionIcon).size
    val titleHorizontalPadding = 36 + (actionCount - 1).coerceAtLeast(0) * 40
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (forceTransparent) Color.Transparent else immersiveTopBarColor(DeltaColors.Background))
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(start = 18.dp, end = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = titleHorizontalPadding.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeltaColors.Accent,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = title,
                    color = DeltaColors.Ink,
                    fontSize = deltaSp(23f, maxScale = 1.02f),
                    lineHeight = deltaSp(27f, maxScale = 1.02f),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                if (actionIcon != null) {
                    IconButton(onClick = onActionClick) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = actionContentDescription,
                            tint = DeltaColors.Accent,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                if (secondaryActionIcon != null) {
                    IconButton(onClick = onSecondaryActionClick) {
                        Icon(
                            imageVector = secondaryActionIcon,
                            contentDescription = secondaryActionContentDescription,
                            tint = DeltaColors.Accent,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                if (tertiaryActionIcon != null) {
                    IconButton(onClick = onTertiaryActionClick) {
                        Icon(
                            imageVector = tertiaryActionIcon,
                            contentDescription = tertiaryActionContentDescription,
                            tint = DeltaColors.Accent,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }
        DeltaRule()
    }
}

@Composable
private fun DeltaRule() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(34.dp),
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(DeltaColors.Accent),
            )
        }
    }
}

@Composable
fun DeltaCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    backgroundContent: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(DeltaShapes.Card)
            .background(DeltaColors.Surface)
            .padding(contentPadding),
    ) {
        backgroundContent?.invoke(this)
        content()
    }
}

@Composable
fun DeltaPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    background: Color? = null,
    foreground: Color? = null,
    enabled: Boolean = true,
) {
    val resolvedBackground = background ?: DeltaColors.AccentSoft
    val resolvedForeground = foreground ?: DeltaColors.Ink
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (enabled) resolvedBackground else DeltaColors.SurfaceDeep)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolvedForeground,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = if (enabled) resolvedForeground else DeltaColors.Disabled,
            fontSize = deltaSp(13f, maxScale = 1.0f),
            lineHeight = deltaSp(16f, maxScale = 1.0f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun DeltaSwitch(
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
            modifier = modifier.scale(0.9f),
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DeltaColors.Accent,
                checkedTrackColor = DeltaColors.AccentSoft,
                checkedBorderColor = DeltaColors.Accent.copy(alpha = 0.6f),
                uncheckedThumbColor = DeltaColors.Ink,
                uncheckedTrackColor = DeltaColors.SurfaceDeep,
                uncheckedBorderColor = DeltaColors.Disabled.copy(alpha = 0.55f),
                disabledCheckedThumbColor = DeltaColors.Disabled,
                disabledCheckedTrackColor = DeltaColors.SurfaceDeep,
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
fun DeltaSearchField(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(DeltaShapes.Control)
            .background(DeltaColors.Surface)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = DeltaColors.Muted,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            singleLine = true,
            cursorBrush = SolidColor(DeltaColors.Accent),
            textStyle = TextStyle(
                color = DeltaColors.Ink,
                fontSize = deltaSp(14f),
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (searchText.isBlank()) {
                        Text(
                            text = stringResource(R.string.superuser_search_hint),
                            color = DeltaColors.Muted,
                            fontSize = deltaSp(13f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (searchText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DeltaColors.SurfaceDeep)
                    .clickable(onClick = onClearSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = DeltaColors.Muted,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
fun DeltaSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = DeltaColors.Accent,
        fontSize = deltaSp(18f, maxScale = 1.02f),
        lineHeight = deltaSp(22f, maxScale = 1.02f),
        fontWeight = FontWeight.Black,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

@Composable
fun DeltaEmptyCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    DeltaCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = DeltaColors.Muted,
                fontSize = deltaSp(14f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun DeltaBottomBar(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    destinations: List<MainDestination>,
    modifier: Modifier = Modifier,
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 46.dp)
            .padding(bottom = navBottom + 8.dp)
            .height(62.dp)
            .shadow(8.dp, DeltaShapes.BottomBar)
            .clip(DeltaShapes.BottomBar)
            .background(DeltaColors.Surface.copy(alpha = 0.92f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        destinations.forEachIndexed { index, destination ->
            DeltaNavItem(
                destination = destination,
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                modifier = Modifier.weight(if (selectedIndex == index) 1.35f else 1f),
            )
        }
    }
}

@Composable
private fun DeltaNavItem(
    destination: MainDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(DeltaShapes.Control)
            .background(if (selected) DeltaColors.AccentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = stringResource(destination.label),
            tint = if (selected) DeltaColors.Ink else DeltaColors.Muted,
            modifier = Modifier.size(26.dp),
        )
        if (selected) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(destination.label),
                color = DeltaColors.Ink,
                fontSize = deltaSp(13f, maxScale = 1.0f),
                lineHeight = deltaSp(15f, maxScale = 1.0f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
