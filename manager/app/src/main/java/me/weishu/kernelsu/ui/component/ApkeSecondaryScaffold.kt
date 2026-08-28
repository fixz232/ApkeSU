@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package me.weishu.kernelsu.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor

object ApkeUiTokens {
    val PageHorizontalPadding = 16.dp
    val CompactListHorizontalPadding = 12.dp
    val ToolCardRadius = 8.dp
    val ListRowMinHeight = 56.dp
    val ListRowMaxHeight = 64.dp
    val MinTouchTarget = 48.dp
    const val MotionDurationShort = 180
    const val MotionDurationMedium = 220
    const val MotionDurationLong = 240
}

enum class ApkeWindowWidthClass {
    Compact,
    Medium,
    Expanded,
}

@Immutable
data class ApkeWindowSizeClass(
    val width: ApkeWindowWidthClass,
) {
    val isCompact: Boolean get() = width == ApkeWindowWidthClass.Compact
    val isExpanded: Boolean get() = width == ApkeWindowWidthClass.Expanded
}

val LocalApkeWindowSizeClass = compositionLocalOf {
    ApkeWindowSizeClass(ApkeWindowWidthClass.Compact)
}

fun apkeWindowSizeClass(maxWidth: Dp): ApkeWindowSizeClass = ApkeWindowSizeClass(
    width = when {
        maxWidth < 600.dp -> ApkeWindowWidthClass.Compact
        maxWidth < 840.dp -> ApkeWindowWidthClass.Medium
        else -> ApkeWindowWidthClass.Expanded
    },
)

enum class ApkeStatusTone {
    Neutral,
    Success,
    Warning,
    Error,
}

@Immutable
data class ApkeStatus(
    val text: String,
    val tone: ApkeStatusTone = ApkeStatusTone.Neutral,
    val icon: ImageVector? = null,
)

@Immutable
data class ApkeMetricItem(
    val label: String,
    val value: String,
)

@Composable
fun ApkeSecondaryScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    status: ApkeStatus? = null,
    snackbarHostState: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    maxContentWidth: Dp = 960.dp,
    containerColor: Color = Color.Transparent,
    content: @Composable (PaddingValues, ApkeWindowSizeClass) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSizeClass = apkeWindowSizeClass(maxWidth)
        androidx.compose.runtime.CompositionLocalProvider(
            LocalApkeWindowSizeClass provides windowSizeClass,
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = containerColor,
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(ApkeUiTokens.MinTouchTarget),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = stringResource(R.string.back),
                                    )
                                }
                            },
                            actions = actions,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = immersiveScrolledTopBarColor(
                                    MaterialTheme.colorScheme.surface,
                                ),
                            ),
                            scrollBehavior = scrollBehavior,
                        )
                        if (status != null) {
                            ApkeStatusStrip(
                                status = status,
                                modifier = Modifier.padding(
                                    horizontal = ApkeUiTokens.PageHorizontalPadding,
                                    vertical = 6.dp,
                                ),
                            )
                        }
                    }
                },
                bottomBar = bottomBar,
                snackbarHost = {
                    if (snackbarHostState != null) {
                        SnackbarHost(hostState = snackbarHostState)
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = maxContentWidth),
                    ) {
                        content(innerPadding, windowSizeClass)
                    }
                }
            }
        }
    }
}

@Composable
fun ApkeStatusStrip(
    status: ApkeStatus,
    modifier: Modifier = Modifier,
) {
    val colors = when (status.tone) {
        ApkeStatusTone.Neutral -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
        ApkeStatusTone.Success -> MaterialTheme.colorScheme.tertiaryContainer to
            MaterialTheme.colorScheme.onTertiaryContainer
        ApkeStatusTone.Warning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.58f) to
            MaterialTheme.colorScheme.onErrorContainer
        ApkeStatusTone.Error -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
    }
    val icon = status.icon ?: when (status.tone) {
        ApkeStatusTone.Warning -> Icons.Rounded.WarningAmber
        ApkeStatusTone.Error -> Icons.Rounded.ErrorOutline
        else -> Icons.Rounded.Info
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.first,
        contentColor = colors.second,
        shape = RoundedCornerShape(ApkeUiTokens.ToolCardRadius),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = status.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun ApkeGroupedList(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    start = ApkeUiTokens.CompactListHorizontalPadding,
                    bottom = 7.dp,
                ),
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(ApkeUiTokens.ToolCardRadius),
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun ApkeListDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = ApkeUiTokens.CompactListHorizontalPadding),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f),
    )
}

@Composable
fun ApkeFixedActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = ApkeUiTokens.PageHorizontalPadding,
                    top = 10.dp,
                    end = ApkeUiTokens.PageHorizontalPadding,
                    bottom = WindowInsets.navigationBars.getBottom(
                        androidx.compose.ui.platform.LocalDensity.current,
                    ).let { with(androidx.compose.ui.platform.LocalDensity.current) { it.toDp() } } + 10.dp,
                )
                .heightIn(min = ApkeUiTokens.MinTouchTarget),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun ApkeResponsiveMetrics(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val widthClass = LocalApkeWindowSizeClass.current.width
    when (widthClass) {
        ApkeWindowWidthClass.Compact -> FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) { content() }
        ApkeWindowWidthClass.Medium -> FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 3,
        ) { content() }
        ApkeWindowWidthClass.Expanded -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) { content() }
    }
}

@Composable
fun ApkeMetricGrid(
    items: List<ApkeMetricItem>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth < 360.dp -> 1
            maxWidth < 600.dp -> 2
            else -> 3
        }.coerceAtMost(items.size.coerceAtLeast(1))
        val gap = 8.dp
        val itemWidth = (maxWidth - gap * (columns - 1)) / columns
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            maxItemsInEachRow = columns,
        ) {
            items.forEach { item ->
                Surface(
                    modifier = Modifier
                        .width(itemWidth)
                        .heightIn(min = ApkeUiTokens.ListRowMinHeight),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.58f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApkeLoadingState(
    label: String = stringResource(R.string.loading),
    modifier: Modifier = Modifier,
) {
    ApkeCenteredState(
        modifier = modifier,
        icon = null,
        title = label,
        supportingText = null,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
    }
}

@Composable
fun ApkeEmptyState(
    title: String,
    supportingText: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Inbox,
) {
    ApkeCenteredState(
        modifier = modifier,
        icon = icon,
        title = title,
        supportingText = supportingText,
    )
}

@Composable
fun ApkeErrorState(
    title: String,
    supportingText: String? = null,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onExportDiagnostics: (() -> Unit)? = null,
) {
    ApkeCenteredState(
        modifier = modifier,
        icon = Icons.Rounded.ErrorOutline,
        title = title,
        supportingText = supportingText,
    ) {
        if (onRetry != null || onExportDiagnostics != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onRetry != null) {
                    Button(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.retry))
                    }
                }
                if (onExportDiagnostics != null) {
                    OutlinedButton(
                        onClick = onExportDiagnostics,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.export_diagnostics))
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkeCenteredState(
    title: String,
    supportingText: String?,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            leading != null -> leading()
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}
