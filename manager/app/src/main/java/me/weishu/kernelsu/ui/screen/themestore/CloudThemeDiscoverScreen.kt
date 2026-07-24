package me.weishu.kernelsu.ui.screen.themestore

import android.graphics.Bitmap
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors
import me.weishu.kernelsu.ui.util.CloudTheme
import me.weishu.kernelsu.ui.util.CloudThemeCatalogSnapshot
import me.weishu.kernelsu.ui.util.CloudThemeCatalogSource
import me.weishu.kernelsu.ui.util.CloudThemeLocalRecord
import me.weishu.kernelsu.ui.util.CloudThemeLocalState
import me.weishu.kernelsu.ui.util.CloudThemePublicationStatus
import me.weishu.kernelsu.ui.util.CloudThemeRepository
import me.weishu.kernelsu.ui.util.loadCloudThemeImage
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

private enum class CloudThemeDiscoverFilter {
    All,
    Featured,
    Latest,
    Updates,
    Favorites,
}

@Composable
internal fun CloudThemeDiscoverContent(
    onOpenTheme: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context.applicationContext) {
        CloudThemeRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<CloudThemeCatalogSnapshot?>(null) }
    var localState by remember { mutableStateOf(repository.readLocalState()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(CloudThemeDiscoverFilter.All) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }

    fun refresh(force: Boolean) {
        if (refreshing) return
        refreshing = true
        scope.launch {
            try {
                snapshot = repository.loadCatalog(forceRefresh = force)
                localState = repository.readLocalState()
            } finally {
                loading = false
                refreshing = false
            }
        }
    }

    LaunchedEffect(repository) {
        snapshot = repository.loadCatalog(forceRefresh = false)
        localState = repository.readLocalState()
        loading = false
    }
    LifecycleResumeEffect(repository) {
        localState = repository.readLocalState()
        onPauseOrDispose { }
    }

    val visibleThemes = remember(snapshot, localState, query, filter, categoryId) {
        val normalizedQuery = query.trim().lowercase()
        val all = snapshot?.catalog?.themes
            .orEmpty()
            .asSequence()
            .filter { it.status == CloudThemePublicationStatus.Published }
            .filter { categoryId == null || it.categoryId == categoryId }
            .filter { theme ->
                normalizedQuery.isBlank() || listOf(
                    theme.name,
                    theme.author.name,
                    theme.description,
                    theme.tags.joinToString(" "),
                ).any { normalizedQuery in it.lowercase() }
            }
            .filter { theme ->
                when (filter) {
                    CloudThemeDiscoverFilter.All,
                    CloudThemeDiscoverFilter.Latest -> true
                    CloudThemeDiscoverFilter.Featured -> theme.featured
                    CloudThemeDiscoverFilter.Updates -> {
                        val record = localState.record(theme.id)
                        record != null && (
                            record.versionCode < theme.versionCode ||
                                (record.versionCode == theme.versionCode && record.sha256 != theme.sha256)
                            )
                    }
                    CloudThemeDiscoverFilter.Favorites -> localState.isFavorite(theme.id)
                }
            }
            .sortedWith(
                compareByDescending<CloudTheme> { filter == CloudThemeDiscoverFilter.Featured && it.featured }
                    .thenByDescending(CloudTheme::publishedAt)
            )
            .toList()
        if (filter == CloudThemeDiscoverFilter.Latest) all.take(24) else all
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CloudThemeDiscoverHeader(
                snapshot = snapshot,
                refreshing = refreshing,
                onRefresh = { refresh(true) },
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.cloud_theme_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = stringResource(R.string.cloud_theme_clear_search),
                            )
                        }
                    }
                } else {
                    null
                },
            )
        }
        item {
            CloudThemeFilters(
                selected = filter,
                onSelected = { filter = it },
            )
        }
        snapshot?.catalog?.categories?.takeIf { it.isNotEmpty() }?.let { categories ->
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = categoryId == null,
                            onClick = { categoryId = null },
                            label = { Text(stringResource(R.string.cloud_theme_category_all)) },
                        )
                    }
                    items(categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text(category.name, maxLines = 1) },
                        )
                    }
                }
            }
        }

        if (loading && snapshot == null) {
            items(3) { CloudThemeLoadingCard() }
        } else if (snapshot == null) {
            item {
                CloudThemeEmptyState(
                    error = true,
                    onRetry = { refresh(true) },
                )
            }
        } else if (visibleThemes.isEmpty()) {
            item {
                CloudThemeEmptyState(
                    error = false,
                    onRetry = if (snapshot?.offline == true) ({ refresh(true) }) else null,
                )
            }
        } else {
            items(visibleThemes, key = CloudTheme::id) { theme ->
                CloudThemeCard(
                    theme = theme,
                    categoryName = snapshot?.catalog?.categoryName(theme.categoryId).orEmpty(),
                    record = localState.record(theme.id),
                    isActive = localState.isActive(theme.id),
                    favorite = localState.isFavorite(theme.id),
                    onFavorite = { favorite ->
                        scope.launch {
                            localState = withContext(Dispatchers.IO) {
                                repository.setFavorite(theme.id, favorite)
                            }
                        }
                    },
                    onClick = { onOpenTheme(theme.id) },
                )
            }
        }
        item { Spacer(modifier = Modifier.height(18.dp)) }
    }
}

@Composable
private fun CloudThemeDiscoverHeader(
    snapshot: CloudThemeCatalogSnapshot?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cloud_theme_discover_heading),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cloudThemeTextColor(),
                )
                Text(
                    text = stringResource(
                        R.string.cloud_theme_catalog_count,
                        snapshot?.catalog?.themes?.count {
                            it.status == CloudThemePublicationStatus.Published
                        } ?: 0,
                    ),
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
        snapshot?.let { current ->
            if (current.offline || current.source == CloudThemeCatalogSource.Bundled) {
                CloudThemeStatusBand(
                    icon = Icons.Rounded.CloudOff,
                    text = stringResource(
                        if (current.source == CloudThemeCatalogSource.Bundled) {
                            R.string.cloud_theme_bundled_catalog
                        } else {
                            R.string.cloud_theme_offline_catalog
                        }
                    ),
                    isError = current.errorMessage != null,
                )
            }
            if (current.fetchedAt > 0L) {
                Text(
                    text = stringResource(
                        R.string.cloud_theme_catalog_updated,
                        formatCloudThemeDate(current.fetchedAt),
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = cloudThemeMutedColor(),
                )
            }
        }
    }
}

@Composable
private fun CloudThemeFilters(
    selected: CloudThemeDiscoverFilter,
    onSelected: (CloudThemeDiscoverFilter) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CloudThemeDiscoverFilter.entries, key = { it.name }) { filter ->
            val label = stringResource(
                when (filter) {
                    CloudThemeDiscoverFilter.All -> R.string.cloud_theme_filter_all
                    CloudThemeDiscoverFilter.Featured -> R.string.cloud_theme_filter_featured
                    CloudThemeDiscoverFilter.Latest -> R.string.cloud_theme_filter_latest
                    CloudThemeDiscoverFilter.Updates -> R.string.cloud_theme_filter_updates
                    CloudThemeDiscoverFilter.Favorites -> R.string.cloud_theme_filter_favorites
                }
            )
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(label) },
                leadingIcon = when (filter) {
                    CloudThemeDiscoverFilter.Featured -> ({
                        Icon(Icons.Rounded.NewReleases, contentDescription = null, modifier = Modifier.size(16.dp))
                    })
                    CloudThemeDiscoverFilter.Updates -> ({
                        Icon(Icons.Rounded.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                    })
                    CloudThemeDiscoverFilter.Favorites -> ({
                        Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    })
                    else -> null
                },
            )
        }
    }
}

@Composable
private fun CloudThemeCard(
    theme: CloudTheme,
    categoryName: String,
    record: CloudThemeLocalRecord?,
    isActive: Boolean,
    favorite: Boolean,
    onFavorite: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = cloudThemeSurfaceColor(),
        tonalElevation = if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) 0.dp else 1.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) {
                CloudThemeRemoteImage(
                    url = theme.coverUrl,
                    contentDescription = theme.name,
                    modifier = Modifier.fillMaxSize(),
                    maxSide = 1200,
                )
                if (theme.featured) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                    ) {
                        Text(
                            text = stringResource(R.string.cloud_theme_featured),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.42f), CircleShape),
                    onClick = { onFavorite(!favorite) },
                ) {
                    Icon(
                        imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(
                            if (favorite) R.string.cloud_theme_remove_favorite else R.string.cloud_theme_add_favorite
                        ),
                        tint = Color.White,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = theme.name,
                            style = MaterialTheme.typography.titleMedium,
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
                    }
                    CloudThemeRecordBadge(theme, record, isActive)
                }
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cloudThemeMutedColor(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$categoryName · ${theme.versionName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = cloudThemeMutedColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(
                            R.string.cloud_theme_size_downloads,
                            Formatter.formatShortFileSize(context, theme.sizeBytes),
                            NumberFormat.getIntegerInstance().format(theme.downloadCount),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = cloudThemeMutedColor(),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudThemeRecordBadge(
    theme: CloudTheme,
    record: CloudThemeLocalRecord?,
    isActive: Boolean,
) {
    val compatible = theme.isCompatible(BuildConfig.VERSION_CODE.toLong())
    val (icon, label, tint) = when {
        !compatible -> Triple(
            Icons.Rounded.ErrorOutline,
            stringResource(R.string.cloud_theme_incompatible_short),
            MaterialTheme.colorScheme.error,
        )
        record?.let {
            it.versionCode < theme.versionCode ||
                (it.versionCode == theme.versionCode && it.sha256 != theme.sha256)
        } == true -> Triple(
            Icons.Rounded.Update,
            stringResource(R.string.cloud_theme_update_available),
            MaterialTheme.colorScheme.tertiary,
        )
        isActive &&
            record?.appliedVersionCode == theme.versionCode &&
            record.appliedSha256 == theme.sha256 -> Triple(
            Icons.Rounded.CheckCircle,
            stringResource(R.string.cloud_theme_applied),
            MaterialTheme.colorScheme.primary,
        )
        record?.versionCode == theme.versionCode && record.sha256 == theme.sha256 -> Triple(
            Icons.Rounded.CheckCircle,
            stringResource(R.string.cloud_theme_downloaded),
            MaterialTheme.colorScheme.primary,
        )
        else -> return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
        )
    }
}

@Composable
private fun CloudThemeLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = cloudThemeSurfaceColor(),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(cloudThemeMutedColor().copy(alpha = 0.12f)),
            )
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.55f)
                        .height(18.dp)
                        .background(cloudThemeMutedColor().copy(alpha = 0.13f), RoundedCornerShape(4.dp))
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.86f)
                        .height(12.dp)
                        .background(cloudThemeMutedColor().copy(alpha = 0.09f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun CloudThemeEmptyState(
    error: Boolean,
    onRetry: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 42.dp),
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
                if (error) R.string.cloud_theme_error_title else R.string.cloud_theme_empty_title
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = cloudThemeTextColor(),
        )
        Text(
            text = stringResource(
                if (error) R.string.cloud_theme_error_summary else R.string.cloud_theme_empty_summary
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = cloudThemeMutedColor(),
        )
        onRetry?.let {
            OutlinedButton(onClick = it) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.cloud_theme_retry))
            }
        }
    }
}

@Composable
private fun CloudThemeStatusBand(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isError: Boolean,
) {
    val tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = cloudThemeTextColor())
    }
}

@Composable
internal fun CloudThemeRemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxSide: Int = 1400,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, url, maxSide, context.applicationContext) {
        value = try {
            loadCloudThemeImage(context.applicationContext, url, maxSide)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        val loadedBitmap = bitmap
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
internal fun cloudThemeSurfaceColor(): Color {
    return if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproColors.BarSurface
    } else {
        MaterialTheme.colorScheme.surface
    }
}

@Composable
internal fun cloudThemeTextColor(): Color {
    return if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproColors.Text
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}

@Composable
internal fun cloudThemeMutedColor(): Color {
    return if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproColors.Muted
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
}

internal fun formatCloudThemeDate(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
}
