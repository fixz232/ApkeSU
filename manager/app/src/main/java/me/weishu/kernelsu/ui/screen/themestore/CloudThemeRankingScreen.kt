package me.weishu.kernelsu.ui.screen.themestore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproScreen
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.CloudTheme
import me.weishu.kernelsu.ui.util.CloudThemeCatalogSnapshot
import me.weishu.kernelsu.ui.util.CloudThemeCatalogSource
import me.weishu.kernelsu.ui.util.CloudThemeLocalState
import me.weishu.kernelsu.ui.util.CloudThemeRepository
import me.weishu.kernelsu.ui.util.CloudThemeUsageStatistics
import me.weishu.kernelsu.ui.util.calculateUsageStatistics
import java.text.NumberFormat
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun CloudThemeRankingScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val repository = remember(context.applicationContext) {
        CloudThemeRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<CloudThemeCatalogSnapshot?>(null) }
    var localState by remember { mutableStateOf(repository.readLocalState()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }

    fun refresh(force: Boolean) {
        if (refreshing) return
        refreshing = true
        scope.launch {
            try {
                snapshot = repository.loadCatalog(forceRefresh = force)
                localState = repository.readLocalState()
                loadFailed = false
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                loadFailed = snapshot == null
            } finally {
                loading = false
                refreshing = false
            }
        }
    }

    LaunchedEffect(repository) {
        try {
            snapshot = repository.loadCatalog(forceRefresh = false)
            localState = repository.readLocalState()
            loadFailed = false
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            loadFailed = true
        } finally {
            loading = false
        }
    }
    LifecycleResumeEffect(repository) {
        localState = repository.readLocalState()
        onPauseOrDispose { }
    }

    val statistics = remember(snapshot) {
        snapshot?.catalog?.calculateUsageStatistics()
    }
    val rankedThemes = remember(statistics) {
        statistics?.rankedThemes.orEmpty()
    }
    val onBack = dropUnlessResumed { navigator.pop() }
    val onOpenTheme: (String) -> Unit = { themeId ->
        navigator.push(Route.CloudThemeDetail(themeId))
    }
    val content: @Composable (PaddingValues) -> Unit = { paddingValues ->
        CloudThemeRankingContent(
            snapshot = snapshot,
            statistics = statistics,
            rankedThemes = rankedThemes,
            localState = localState,
            loading = loading,
            refreshing = refreshing,
            loadFailed = loadFailed,
            onRefresh = { refresh(true) },
            onOpenTheme = onOpenTheme,
            modifier = Modifier.padding(paddingValues),
        )
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproScreen(
            title = stringResource(R.string.cloud_theme_ranking_title),
            bottomInnerPadding = 0.dp,
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                content(paddingValues)
                CloudThemeRankingBackButton(onBack)
            }
        }
    } else {
        MiuixScaffold(
            containerColor = Color.Transparent,
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars
                .add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
            topBar = {
                MiuixTopAppBar(
                    title = stringResource(R.string.cloud_theme_ranking_title),
                    color = Color.Transparent,
                    titleColor = colorScheme.onSurface,
                    navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            MiuixIcon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = colorScheme.onBackground,
                            )
                        }
                    },
                )
            },
            content = content,
        )
    }
}

@Composable
private fun CloudThemeRankingContent(
    snapshot: CloudThemeCatalogSnapshot?,
    statistics: CloudThemeUsageStatistics?,
    rankedThemes: List<CloudTheme>,
    localState: CloudThemeLocalState,
    loading: Boolean,
    refreshing: Boolean,
    loadFailed: Boolean,
    onRefresh: () -> Unit,
    onOpenTheme: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (refreshing) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        statistics?.let {
            item {
                CloudThemeStatisticsPanel(
                    statistics = it,
                    onRefresh = onRefresh,
                    refreshing = refreshing,
                )
            }
        }
        snapshot?.let { current ->
            if (current.offline || current.source == CloudThemeCatalogSource.Bundled) {
                item { CloudThemeRankingCatalogStatus(current) }
            }
        }

        when {
            loading && statistics == null -> {
                items(4) { CloudThemeRankingLoadingItem() }
            }
            loadFailed && statistics == null -> {
                item { CloudThemeRankingEmpty(error = true, onRetry = onRefresh) }
            }
            statistics != null && rankedThemes.isEmpty() -> {
                item {
                    CloudThemeRankingEmpty(
                        error = false,
                        onRetry = if (snapshot?.offline == true) onRefresh else null,
                    )
                }
            }
            else -> {
                itemsIndexed(rankedThemes, key = { _, theme -> theme.id }) { index, theme ->
                    CloudThemeRankingItem(
                        rank = index + 1,
                        theme = theme,
                        categoryName = snapshot?.catalog?.categoryName(theme.categoryId).orEmpty(),
                        localState = localState,
                        onClick = { onOpenTheme(theme.id) },
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CloudThemeStatisticsPanel(
    statistics: CloudThemeUsageStatistics,
    onRefresh: () -> Unit,
    refreshing: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = cloudThemeSurfaceColor(),
        tonalElevation = if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Leaderboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cloud_theme_ranking_overview),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cloudThemeTextColor(),
                    )
                    Text(
                        text = stringResource(R.string.cloud_theme_ranking_method),
                        style = MaterialTheme.typography.bodySmall,
                        color = cloudThemeMutedColor(),
                    )
                }
                IconButton(enabled = !refreshing, onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.cloud_theme_refresh),
                        tint = cloudThemeTextColor(),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CloudThemeStatistic(
                    label = stringResource(R.string.cloud_theme_ranking_total_usage),
                    value = NumberFormat.getIntegerInstance().format(statistics.totalUsageCount),
                    modifier = Modifier.weight(1f),
                )
                CloudThemeStatistic(
                    label = stringResource(R.string.cloud_theme_ranking_theme_count),
                    value = NumberFormat.getIntegerInstance().format(statistics.publishedThemeCount),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CloudThemeStatistic(
                    label = stringResource(R.string.cloud_theme_ranking_creator_count),
                    value = NumberFormat.getIntegerInstance().format(statistics.creatorCount),
                    modifier = Modifier.weight(1f),
                )
                CloudThemeStatistic(
                    label = stringResource(R.string.cloud_theme_ranking_category_count),
                    value = NumberFormat.getIntegerInstance().format(statistics.categoryCount),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CloudThemeStatistic(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = cloudThemeTextColor(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = cloudThemeMutedColor(),
            maxLines = 2,
        )
    }
}

@Composable
private fun CloudThemeRankingItem(
    rank: Int,
    theme: CloudTheme,
    categoryName: String,
    localState: CloudThemeLocalState,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = cloudThemeSurfaceColor(),
        tonalElevation = if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) 0.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = if (rank <= 3) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (rank <= 3) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            CloudThemeRemoteImage(
                url = theme.coverUrl,
                contentDescription = theme.name,
                modifier = Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(6.dp)),
                maxSide = 320,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cloudThemeTextColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.cloud_theme_author, theme.author.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = cloudThemeMutedColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.cloud_theme_ranking_usage_count,
                            NumberFormat.getIntegerInstance().format(theme.downloadCount.coerceAtLeast(0L)),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    Text(
                        text = categoryName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = cloudThemeMutedColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (localState.isFavorite(theme.id)) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = stringResource(R.string.cloud_theme_remove_favorite),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    if (localState.record(theme.id) != null) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = stringResource(R.string.cloud_theme_downloaded),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudThemeRankingCatalogStatus(snapshot: CloudThemeCatalogSnapshot) {
    val tint = if (snapshot.errorMessage != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(
                if (snapshot.source == CloudThemeCatalogSource.Bundled) {
                    R.string.cloud_theme_bundled_catalog
                } else {
                    R.string.cloud_theme_offline_catalog
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = cloudThemeTextColor(),
        )
    }
}

@Composable
private fun CloudThemeRankingLoadingItem() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = cloudThemeSurfaceColor(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(cloudThemeMutedColor().copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            )
            Box(
                Modifier
                    .size(66.dp)
                    .background(cloudThemeMutedColor().copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.72f)
                        .height(16.dp)
                        .background(cloudThemeMutedColor().copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.48f)
                        .height(11.dp)
                        .background(cloudThemeMutedColor().copy(alpha = 0.09f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun CloudThemeRankingEmpty(
    error: Boolean,
    onRetry: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = if (error) Icons.Rounded.ErrorOutline else Icons.Rounded.Image,
            contentDescription = null,
            tint = cloudThemeMutedColor(),
            modifier = Modifier.size(38.dp),
        )
        Text(
            text = stringResource(
                if (error) R.string.cloud_theme_error_title else R.string.cloud_theme_ranking_empty_title
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = cloudThemeTextColor(),
        )
        Text(
            text = stringResource(
                if (error) R.string.cloud_theme_error_summary else R.string.cloud_theme_ranking_empty_summary
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = cloudThemeMutedColor(),
            textAlign = TextAlign.Center,
        )
        onRetry?.let {
            OutlinedButton(onClick = it) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.cloud_theme_retry))
            }
        }
    }
}

@Composable
private fun CloudThemeRankingBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 16.dp, top = 14.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.34f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = Color.White,
        )
    }
}
