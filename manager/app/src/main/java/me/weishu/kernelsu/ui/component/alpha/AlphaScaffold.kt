package me.weishu.kernelsu.ui.component.alpha

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import me.weishu.kernelsu.ui.component.LocalNightBackgroundEffectActive
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.component.snow.SnowBackdrop
import me.weishu.kernelsu.ui.component.snow.SnowCapBand
import me.weishu.kernelsu.ui.theme.LocalImmersiveBackgroundActive
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private data class AlphaPalette(
    val background: Color,
    val topBar: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val accent: Color,
    val onAccent: Color,
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
        onAccent = Color.White,
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
        onAccent = Color.White,
        accentSoft = Color(0xFFA9DFAE),
        text = Color(0xFF171A17),
        muted = Color(0xFF4D574D),
        disabled = Color(0xFF9AA49A),
        divider = Color(0xFFD7E1D6),
    )

    private val AlphaDark = AlphaPalette(
        background = Color(0xFF111315),
        topBar = Color(0xFF171A1E),
        surface = Color(0xFF20242A),
        surfaceStrong = Color(0xFF2A2F36),
        accent = Color(0xFF72B9E9),
        onAccent = Color(0xFF07131A),
        accentSoft = Color(0xFF24475D),
        text = Color(0xFFE9EDF1),
        muted = Color(0xFFABB4BE),
        disabled = Color(0xFF66717D),
        divider = Color(0xFF343B43),
    )

    private val DeltaDark = AlphaPalette(
        background = Color(0xFF101511),
        topBar = Color(0xFF151B16),
        surface = Color(0xFF1C241E),
        surfaceStrong = Color(0xFF273229),
        accent = Color(0xFF67D47A),
        onAccent = Color(0xFF061509),
        accentSoft = Color(0xFF23492C),
        text = Color(0xFFE8F1E9),
        muted = Color(0xFFA8B5AA),
        disabled = Color(0xFF68756A),
        divider = Color(0xFF354238),
    )

    private val SnowLight = AlphaPalette(
        background = Color.Transparent,
        topBar = Color(0xB8F4FAFA),
        surface = Color(0xC7F7FBFB),
        surfaceStrong = Color(0xA8DDEDEE),
        accent = Color(0xFF086B72),
        onAccent = Color.White,
        accentSoft = Color(0xA8CDE9E7),
        text = Color(0xFF10282B),
        muted = Color(0xFF405E62),
        disabled = Color(0xFF7E979A),
        divider = Color(0x8FFFFFFF),
    )

    private val SnowDark = AlphaPalette(
        background = Color.Transparent,
        topBar = Color(0xA80A1A1E),
        surface = Color(0xB5192A2E),
        surfaceStrong = Color(0x9C29454A),
        accent = Color(0xFF8BE7D8),
        onAccent = Color(0xFF06231F),
        accentSoft = Color(0x9C1E5652),
        text = Color(0xFFF4FAFA),
        muted = Color(0xFFC6D8D9),
        disabled = Color(0xFF829596),
        divider = Color(0x62DDF6F4),
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

    val OnAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = current.onAccent

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

    val Snow: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isInDarkTheme()) Color(0xFFDCE9E9) else Color(0xFFFDFEFE)

    val SnowShadow: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isInDarkTheme()) Color(0xFF78999A) else Color(0xFFB5CBCC)

    private val current: AlphaPalette
        @Composable
        @ReadOnlyComposable
        get() = when {
            isSnowStyle() && isInDarkTheme() -> SnowDark
            isSnowStyle() -> SnowLight
            isStudioStyle() -> AlphaPalette(
                background = colorScheme.surface,
                topBar = colorScheme.surface,
                surface = colorScheme.surfaceContainer,
                surfaceStrong = colorScheme.primary.copy(alpha = 0.10f)
                    .compositeOver(colorScheme.surfaceContainer),
                accent = colorScheme.primary,
                onAccent = colorScheme.onPrimary,
                accentSoft = colorScheme.primary.copy(alpha = 0.14f)
                    .compositeOver(colorScheme.surfaceContainer),
                text = colorScheme.onSurface,
                muted = colorScheme.onSurfaceVariantSummary,
                disabled = colorScheme.onSurface.copy(alpha = 0.38f),
                divider = colorScheme.outline.copy(alpha = 0.18f),
            )
            isDeltaStyle() && isInDarkTheme() -> DeltaDark
            isDeltaStyle() -> Delta
            isInDarkTheme() -> AlphaDark
            else -> Alpha
        }
}

object AlphaShapes {
    val Card: Shape
        @Composable
        @ReadOnlyComposable
        get() = when {
            isDeltaStyle() -> RoundedCornerShape(30.dp)
            else -> RoundedCornerShape(8.dp)
        }

    val Control: Shape
        @Composable
        @ReadOnlyComposable
        get() = when {
            isDeltaStyle() -> RoundedCornerShape(26.dp)
            isSnowStyle() -> RoundedCornerShape(8.dp)
            else -> RoundedCornerShape(8.dp)
        }

    val Button: Shape
        @Composable
        @ReadOnlyComposable
        get() = when {
            isDeltaStyle() -> CircleShape
            isSnowStyle() -> RoundedCornerShape(8.dp)
            else -> RoundedCornerShape(8.dp)
        }

    val BottomBar: Shape
        @Composable
        @ReadOnlyComposable
        get() = when {
            isDeltaStyle() -> RoundedCornerShape(36.dp)
            isSnowStyle() -> RoundedCornerShape(18.dp)
            else -> RoundedCornerShape(0.dp)
        }

    val NavItem: Shape
        @Composable
        @ReadOnlyComposable
        get() = when {
            isDeltaStyle() -> RoundedCornerShape(28.dp)
            isSnowStyle() -> RoundedCornerShape(12.dp)
            else -> RoundedCornerShape(0.dp)
        }
}

@Composable
@ReadOnlyComposable
fun isDeltaStyle(): Boolean {
    return LocalInterfaceStyle.current == InterfaceStyle.Delta.value
}

@Composable
@ReadOnlyComposable
fun isStudioStyle(): Boolean {
    return LocalInterfaceStyle.current == InterfaceStyle.Studio.value
}

@Composable
@ReadOnlyComposable
fun isSnowStyle(): Boolean {
    return LocalInterfaceStyle.current == InterfaceStyle.Snow.value
}

@Composable
@ReadOnlyComposable
fun alphaStrongWeight(): FontWeight {
    return if (isStudioStyle() || isSnowStyle()) FontWeight.SemiBold else FontWeight.Black
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
    topActionContentDescription: String? = null,
    secondaryTopActionIcon: ImageVector? = null,
    onSecondaryTopActionClick: () -> Unit = {},
    secondaryTopActionContentDescription: String? = null,
    tertiaryTopActionIcon: ImageVector? = null,
    onTertiaryTopActionClick: () -> Unit = {},
    tertiaryTopActionContentDescription: String? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val snowStyle = isSnowStyle()
    val background = when {
        snowStyle -> Color.Transparent
        LocalNightBackgroundEffectActive.current -> AlphaColors.Background.copy(alpha = 0.84f)
        else -> AlphaColors.Background
    }
    Box(
        modifier = Modifier.fillMaxSize().background(background),
    ) {
        if (snowStyle) {
            SnowBackdrop(
                snowColor = AlphaColors.Snow,
                accentColor = AlphaColors.Accent,
                useDefaultPhoto = !LocalImmersiveBackgroundActive.current,
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            AlphaTopBar(
                title = title,
                actionIcon = topActionIcon,
                onActionClick = onTopActionClick,
                actionContentDescription = topActionContentDescription,
                secondaryActionIcon = secondaryTopActionIcon,
                onSecondaryActionClick = onSecondaryTopActionClick,
                secondaryActionContentDescription = secondaryTopActionContentDescription,
                tertiaryActionIcon = tertiaryTopActionIcon,
                onTertiaryActionClick = onTertiaryTopActionClick,
                tertiaryActionContentDescription = tertiaryTopActionContentDescription,
            )
            Box(modifier = Modifier.weight(1f)) {
                content(PaddingValues(bottom = bottomInnerPadding + 8.dp))
            }
        }
    }
}

@Composable
fun AlphaTopBar(
    title: String,
    actionIcon: ImageVector? = null,
    onActionClick: () -> Unit = {},
    actionContentDescription: String? = null,
    secondaryActionIcon: ImageVector? = null,
    onSecondaryActionClick: () -> Unit = {},
    secondaryActionContentDescription: String? = null,
    tertiaryActionIcon: ImageVector? = null,
    onTertiaryActionClick: () -> Unit = {},
    tertiaryActionContentDescription: String? = null,
) {
    val immersiveBackgroundActive = LocalImmersiveBackgroundActive.current
    val topBarColor = immersiveTopBarColor(AlphaColors.TopBar)
    val dividerColor = if (immersiveBackgroundActive) Color.Transparent else AlphaColors.Divider
    val topBarElevation = if (immersiveBackgroundActive) 0.dp else 4.dp

    if (isSnowStyle()) {
        val snowTopBarBrush = Brush.verticalGradient(
            colors = listOf(
                AlphaColors.TopBar.copy(alpha = if (immersiveBackgroundActive) 0.56f else 0.72f),
                AlphaColors.Surface.copy(alpha = if (immersiveBackgroundActive) 0.42f else 0.58f),
            )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(snowTopBarBrush),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .height(64.dp)
                    .padding(start = 18.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = AlphaColors.Text,
                    fontSize = alphaSp(23f, maxScale = 1.04f),
                    lineHeight = alphaSp(27f, maxScale = 1.04f),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AlphaTopBarActions(
                    actionIcon = actionIcon,
                    onActionClick = onActionClick,
                    actionContentDescription = actionContentDescription,
                    secondaryActionIcon = secondaryActionIcon,
                    onSecondaryActionClick = onSecondaryActionClick,
                    secondaryActionContentDescription = secondaryActionContentDescription,
                    tertiaryActionIcon = tertiaryActionIcon,
                    onTertiaryActionClick = onTertiaryActionClick,
                    tertiaryActionContentDescription = tertiaryActionContentDescription,
                    tint = AlphaColors.Text,
                    iconSize = 26.dp,
                )
            }
            SnowCapBand(
                snowColor = AlphaColors.Snow,
                shadowColor = AlphaColors.SnowShadow,
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
        }
        return
    }

    if (isStudioStyle()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarColor)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(start = 16.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = AlphaColors.Text,
                    fontSize = alphaSp(20f, maxScale = 1.04f),
                    lineHeight = alphaSp(24f, maxScale = 1.04f),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AlphaTopBarActions(
                    actionIcon = actionIcon,
                    onActionClick = onActionClick,
                    actionContentDescription = actionContentDescription,
                    secondaryActionIcon = secondaryActionIcon,
                    onSecondaryActionClick = onSecondaryActionClick,
                    secondaryActionContentDescription = secondaryActionContentDescription,
                    tertiaryActionIcon = tertiaryActionIcon,
                    onTertiaryActionClick = onTertiaryActionClick,
                    tertiaryActionContentDescription = tertiaryActionContentDescription,
                    tint = AlphaColors.Muted,
                    iconSize = 24.dp,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(dividerColor),
            )
        }
        return
    }

    if (isDeltaStyle()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarColor)
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
                AlphaTopBarActions(
                    actionIcon = actionIcon,
                    onActionClick = onActionClick,
                    actionContentDescription = actionContentDescription,
                    secondaryActionIcon = secondaryActionIcon,
                    onSecondaryActionClick = onSecondaryActionClick,
                    secondaryActionContentDescription = secondaryActionContentDescription,
                    tertiaryActionIcon = tertiaryActionIcon,
                    onTertiaryActionClick = onTertiaryActionClick,
                    tertiaryActionContentDescription = tertiaryActionContentDescription,
                    tint = AlphaColors.Text,
                    iconSize = 30.dp,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
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
            .shadow(topBarElevation)
            .background(topBarColor)
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
        AlphaTopBarActions(
            actionIcon = actionIcon,
            onActionClick = onActionClick,
            actionContentDescription = actionContentDescription,
            secondaryActionIcon = secondaryActionIcon,
            onSecondaryActionClick = onSecondaryActionClick,
            secondaryActionContentDescription = secondaryActionContentDescription,
            tertiaryActionIcon = tertiaryActionIcon,
            onTertiaryActionClick = onTertiaryActionClick,
            tertiaryActionContentDescription = tertiaryActionContentDescription,
            tint = AlphaColors.Text,
            iconSize = 28.dp,
        )
    }
}

@Composable
private fun AlphaTopBarActions(
    actionIcon: ImageVector?,
    onActionClick: () -> Unit,
    actionContentDescription: String?,
    secondaryActionIcon: ImageVector?,
    onSecondaryActionClick: () -> Unit,
    secondaryActionContentDescription: String?,
    tertiaryActionIcon: ImageVector?,
    onTertiaryActionClick: () -> Unit,
    tertiaryActionContentDescription: String?,
    tint: Color,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        if (actionIcon != null) {
            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionContentDescription,
                    tint = tint,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        if (secondaryActionIcon != null) {
            IconButton(onClick = onSecondaryActionClick) {
                Icon(
                    imageVector = secondaryActionIcon,
                    contentDescription = secondaryActionContentDescription,
                    tint = tint,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        if (tertiaryActionIcon != null) {
            IconButton(onClick = onTertiaryActionClick) {
                Icon(
                    imageVector = tertiaryActionIcon,
                    contentDescription = tertiaryActionContentDescription,
                    tint = tint,
                    modifier = Modifier.size(iconSize),
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
    val snowStyle = isSnowStyle()
    val layoutDirection = LocalLayoutDirection.current
    val resolvedPadding = if (snowStyle) {
        PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            top = contentPadding.calculateTopPadding() + 12.dp,
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = contentPadding.calculateBottomPadding(),
        )
    } else {
        contentPadding
    }
    val snowGlassBrush = Brush.linearGradient(
        colors = listOf(
            AlphaColors.Surface.copy(alpha = if (isInDarkTheme()) 0.76f else 0.84f),
            AlphaColors.SurfaceStrong.copy(alpha = if (isInDarkTheme()) 0.66f else 0.72f),
            AlphaColors.Surface.copy(alpha = if (isInDarkTheme()) 0.70f else 0.80f),
        )
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (isDeltaStyle()) 4.dp else if (snowStyle) 3.dp else 0.dp, shape)
            .clip(shape)
            .then(
                if (snowStyle) {
                    Modifier
                        .background(snowGlassBrush, shape)
                        .border(1.dp, AlphaColors.Snow.copy(alpha = 0.54f), shape)
                } else {
                    Modifier.background(AlphaColors.Surface)
                }
            )
            .uiDecoratedCard(shape = shape),
    ) {
        Box(modifier = Modifier.padding(resolvedPadding)) {
            content()
        }
        if (snowStyle) {
            SnowCapBand(
                snowColor = AlphaColors.Snow,
                shadowColor = AlphaColors.SnowShadow,
                modifier = Modifier.fillMaxWidth().height(14.dp).align(Alignment.TopCenter),
            )
        }
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
                tint = AlphaColors.OnAccent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = AlphaColors.OnAccent,
            fontSize = alphaSp(14f, maxScale = 1.04f),
            fontWeight = alphaStrongWeight(),
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
            .background(
                when {
                    isDeltaStyle() -> AlphaColors.AccentSoft
                    isStudioStyle() -> AlphaColors.SurfaceStrong
                    isSnowStyle() -> AlphaColors.SurfaceStrong
                    else -> AlphaColors.Background
                }
            )
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
            fontWeight = alphaStrongWeight(),
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
                uncheckedThumbColor = AlphaColors.Text,
                uncheckedTrackColor = AlphaColors.SurfaceStrong,
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
    if (isSnowStyle()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = navBottom + 8.dp)
                .height(64.dp)
                .shadow(4.dp, AlphaShapes.BottomBar)
                .clip(AlphaShapes.BottomBar)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AlphaColors.Surface.copy(alpha = 0.82f),
                            AlphaColors.SurfaceStrong.copy(alpha = 0.66f),
                            AlphaColors.Surface.copy(alpha = 0.76f),
                        )
                    ),
                    shape = AlphaShapes.BottomBar,
                )
                .border(1.dp, AlphaColors.Snow.copy(alpha = 0.48f), AlphaShapes.BottomBar),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 6.dp, top = 6.dp, end = 6.dp, bottom = 4.dp),
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
            SnowCapBand(
                snowColor = AlphaColors.Snow,
                shadowColor = AlphaColors.SnowShadow,
                modifier = Modifier.fillMaxWidth().height(11.dp).align(Alignment.TopCenter),
            )
        }
        return
    }

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
                    fontWeight = alphaStrongWeight(),
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
                .clip(AlphaShapes.NavItem)
                .background(
                    if (isSnowStyle() && selected) AlphaColors.AccentSoft else Color.Transparent
                )
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
                fontWeight = alphaStrongWeight(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AlphaNavigationRail(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(78.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        AlphaColors.Surface.copy(alpha = 0.78f),
                        AlphaColors.SurfaceStrong.copy(alpha = 0.62f),
                    )
                )
            )
            .border(
                width = 1.dp,
                color = AlphaColors.Snow.copy(alpha = 0.42f),
                shape = RoundedCornerShape(0.dp),
            )
            .padding(top = topPadding, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SnowCapBand(
            snowColor = AlphaColors.Snow,
            shadowColor = AlphaColors.SnowShadow,
            modifier = Modifier.fillMaxWidth().height(11.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        AlphaNavDestination.entries.forEachIndexed { index, destination ->
            AlphaRailItem(
                destination = destination,
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AlphaRailItem(
    destination: AlphaNavDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) AlphaColors.Accent else AlphaColors.Muted
    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .width(64.dp)
            .height(58.dp)
            .clip(AlphaShapes.NavItem)
            .background(if (selected) AlphaColors.AccentSoft else Color.Transparent)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = stringResource(destination.label),
            tint = color,
            modifier = Modifier.size(25.dp),
        )
        Text(
            text = stringResource(destination.label),
            color = color,
            fontSize = alphaSp(11f, maxScale = 1.0f),
            lineHeight = alphaSp(13f, maxScale = 1.0f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
