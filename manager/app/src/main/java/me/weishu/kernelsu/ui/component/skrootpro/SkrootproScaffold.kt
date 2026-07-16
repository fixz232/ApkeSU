package me.weishu.kernelsu.ui.component.skrootpro

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.LocalNightBackgroundEffectActive
import me.weishu.kernelsu.ui.theme.LocalImmersiveBackgroundActive
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.theme.isInDarkTheme

private data class SkrootproPalette(
    val purple: Color,
    val purpleDark: Color,
    val magentaLine: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val success: Color,
    val disabled: Color,
    val disabledText: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val barSurface: Color,
)

object SkrootproColors {
    private val Light = SkrootproPalette(
        purple = Color(0xFF7000F5),
        purpleDark = Color(0xFF3A00B8),
        magentaLine = Color(0xFFE000E8),
        text = Color(0xFF1E1E1E),
        muted = Color(0xFF7B7B7B),
        faint = Color(0xFFD9D9D9),
        success = Color(0xFF45A857),
        disabled = Color(0xFFE2E2E2),
        disabledText = Color(0xFF8B8B8B),
        surface = Color.White,
        surfaceStrong = Color(0xFFF5F6F8),
        barSurface = Color(0xFFF5F5F6),
    )

    private val Dark = SkrootproPalette(
        purple = Color(0xFF9D63FF),
        purpleDark = Color(0xFF43188C),
        magentaLine = Color(0xFFF05BF2),
        text = Color(0xFFF0ECF5),
        muted = Color(0xFFB7B0C0),
        faint = Color(0xFF6E6875),
        success = Color(0xFF6BD47F),
        disabled = Color(0xFF39343F),
        disabledText = Color(0xFF8E8796),
        surface = Color(0xFF18191E),
        surfaceStrong = Color(0xFF292A31),
        barSurface = Color(0xFF202126),
    )

    val Purple: Color
        @Composable @ReadOnlyComposable get() = current.purple
    val PurpleDark: Color
        @Composable @ReadOnlyComposable get() = current.purpleDark
    val MagentaLine: Color
        @Composable @ReadOnlyComposable get() = current.magentaLine
    val Text: Color
        @Composable @ReadOnlyComposable get() = current.text
    val Muted: Color
        @Composable @ReadOnlyComposable get() = current.muted
    val Faint: Color
        @Composable @ReadOnlyComposable get() = current.faint
    val Success: Color
        @Composable @ReadOnlyComposable get() = current.success
    val Disabled: Color
        @Composable @ReadOnlyComposable get() = current.disabled
    val DisabledText: Color
        @Composable @ReadOnlyComposable get() = current.disabledText
    val Surface: Color
        @Composable @ReadOnlyComposable get() = current.surface
    val SurfaceStrong: Color
        @Composable @ReadOnlyComposable get() = current.surfaceStrong
    val BarSurface: Color
        @Composable @ReadOnlyComposable get() = current.barSurface

    private val current: SkrootproPalette
        @Composable
        @ReadOnlyComposable
        get() = if (isInDarkTheme()) Dark else Light
}

@Composable
fun skrootproSp(value: Float, maxScale: Float = 1.12f): TextUnit {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(0.85f)
    val cappedScale = fontScale.coerceAtMost(maxScale)
    return (value * cappedScale / fontScale).sp
}

@Composable
fun SkrootproScreen(
    title: String,
    showAdd: Boolean = false,
    onAddClick: () -> Unit = {},
    bottomInnerPadding: Dp,
    content: @Composable (PaddingValues) -> Unit,
) {
    val background = if (LocalNightBackgroundEffectActive.current) {
        SkrootproColors.Surface.copy(alpha = 0.72f)
    } else {
        Color.Transparent
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        SkrootproTopBar(
            title = title,
            showAdd = showAdd,
            onAddClick = onAddClick,
        )
        Box(modifier = Modifier.weight(1f)) {
            content(PaddingValues(bottom = bottomInnerPadding + 10.dp))
        }
    }
}

@Composable
fun SkrootproTopBar(
    title: String,
    showAdd: Boolean = false,
    onAddClick: () -> Unit = {},
) {
    val immersiveBackgroundActive = LocalImmersiveBackgroundActive.current
    val statusBarColor = immersiveTopBarColor(SkrootproColors.PurpleDark)
    val appBarColor = immersiveTopBarColor(SkrootproColors.Purple)
    val contentColor = if (immersiveBackgroundActive) SkrootproColors.Text else Color.White
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(statusBarColor)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(appBarColor)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = contentColor,
                fontSize = skrootproSp(20f, maxScale = 1.0f),
                lineHeight = skrootproSp(24f, maxScale = 1.0f),
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showAdd) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onAddClick),
                )
            }
        }
    }
}

@Composable
fun SkrootproBottomBar(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp)
            .padding(bottom = navBottom + 7.dp)
            .height(42.dp)
            .shadow(4.dp, CircleShape)
            .background(SkrootproColors.BarSurface, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkrootproNavDestination.entries.forEachIndexed { index, destination ->
            SkrootproNavItem(
                destination = destination,
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SkrootproNavItem(
    destination: SkrootproNavDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(34.dp)
            .background(
                color = if (selected) SkrootproColors.Purple else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(destination.label),
            color = if (selected) Color.White else SkrootproColors.Muted,
            fontSize = skrootproSp(14.5f, maxScale = 1.0f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

enum class SkrootproNavDestination(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Home(R.string.home, Icons.Rounded.Home),
    SuperUser(R.string.skrootpro_nav_superuser, Icons.Rounded.Security),
    Module(R.string.module, Icons.Rounded.Extension),
    Settings(R.string.settings, Icons.Rounded.Settings),
}

@Composable
fun SkrootproButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .shadow(if (enabled) 2.dp else 0.dp, RoundedCornerShape(7.dp))
            .background(
                color = if (enabled) SkrootproColors.Purple else SkrootproColors.Disabled,
                shape = RoundedCornerShape(7.dp),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else SkrootproColors.DisabledText,
            fontSize = skrootproSp(12.5f, maxScale = 1.0f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SkrootproSectionTitle(text: String) {
    Text(
        text = text,
        color = SkrootproColors.Text,
        fontSize = skrootproSp(13f, maxScale = 1.0f),
        lineHeight = skrootproSp(16f, maxScale = 1.0f),
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
fun SkrootproDivider(
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    Box(
        modifier = if (vertical) {
            modifier
                .width(2.dp)
                .fillMaxSize()
                .background(SkrootproColors.MagentaLine)
        } else {
            modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(SkrootproColors.MagentaLine)
        }
    )
}

@Composable
fun SkrootproEmptyText(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = SkrootproColors.Faint,
            fontSize = skrootproSp(15f, maxScale = 1.0f),
            textAlign = TextAlign.Center,
        )
    }
}
