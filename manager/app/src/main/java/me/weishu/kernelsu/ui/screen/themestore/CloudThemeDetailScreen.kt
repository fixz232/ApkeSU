package me.weishu.kernelsu.ui.screen.themestore

import android.content.Intent
import android.text.format.Formatter
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproScreen
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.CloudTheme
import me.weishu.kernelsu.ui.util.CloudThemeCatalogSnapshot
import me.weishu.kernelsu.ui.util.CloudThemeInstaller
import me.weishu.kernelsu.ui.util.CloudThemeLocalRecord
import me.weishu.kernelsu.ui.util.CloudThemeLocalState
import me.weishu.kernelsu.ui.util.CloudThemeOperationProgress
import me.weishu.kernelsu.ui.util.CloudThemeOperationResult
import me.weishu.kernelsu.ui.util.CloudThemeOperationStage
import me.weishu.kernelsu.ui.util.CloudThemePublicationStatus
import me.weishu.kernelsu.ui.util.CloudThemeRepository
import me.weishu.kernelsu.ui.util.safeCloudThemeMessage
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private enum class CloudThemePendingConfirmation {
    Apply,
    Rollback,
}

@Composable
fun CloudThemeDetailScreen(themeId: String) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val repository = remember(context.applicationContext) {
        CloudThemeRepository(context.applicationContext)
    }
    val installer = remember(repository) {
        CloudThemeInstaller(context.applicationContext, repository)
    }
    var snapshot by remember { mutableStateOf<CloudThemeCatalogSnapshot?>(null) }
    var theme by remember { mutableStateOf<CloudTheme?>(null) }
    var loading by remember { mutableStateOf(true) }
    var localState by remember { mutableStateOf(repository.readLocalState()) }
    var operationJob by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableStateOf<CloudThemeOperationProgress?>(null) }
    var operationResult by remember { mutableStateOf<CloudThemeOperationResult?>(null) }
    var pendingConfirmation by rememberSaveable {
        mutableStateOf<CloudThemePendingConfirmation?>(null)
    }

    suspend fun loadTheme(force: Boolean) {
        loading = true
        val loaded = repository.loadCatalog(forceRefresh = force)
        snapshot = loaded
        theme = loaded.catalog.theme(themeId)
        localState = repository.readLocalState()
        loading = false
    }

    fun startOperation(block: suspend ((CloudThemeOperationProgress) -> Unit) -> CloudThemeOperationResult) {
        if (operationJob != null) return
        operationResult = null
        operationJob = scope.launch {
            try {
                operationResult = block { progress = it }
                localState = repository.readLocalState()
            } catch (_: CancellationException) {
                operationResult = null
            } finally {
                progress = null
                operationJob = null
            }
        }
    }

    LaunchedEffect(themeId, repository) {
        loadTheme(force = false)
    }

    val onBack = dropUnlessResumed { navigator.pop() }
    val currentTheme = theme
    val record = localState.record(themeId)
    val isActive = localState.isActive(themeId)
    val compatible = currentTheme?.isCompatible(BuildConfig.VERSION_CODE.toLong()) == true &&
        currentTheme.status == CloudThemePublicationStatus.Published
    val downloading = operationJob != null && progress?.stage == CloudThemeOperationStage.Downloading

    val content: @Composable (PaddingValues) -> Unit = { paddingValues ->
        when {
            loading -> CloudThemeDetailLoading(Modifier.padding(paddingValues))
            currentTheme == null -> CloudThemeDetailMissing(
                modifier = Modifier.padding(paddingValues),
                message = snapshot?.errorMessage,
                onRetry = { scope.launch { loadTheme(force = true) } },
            )
            else -> CloudThemeDetailContent(
                theme = currentTheme,
                categoryName = snapshot?.catalog?.categoryName(currentTheme.categoryId).orEmpty(),
                record = record,
                isActive = isActive,
                canRollback = localState.canRollback(currentTheme.id),
                busy = operationJob != null,
                progress = progress,
                result = operationResult,
                modifier = Modifier.padding(paddingValues),
                onOpenAuthor = currentTheme.author.profileUrl?.let { url ->
                    {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        }
                    }
                },
                onSave = {
                    startOperation { report -> installer.saveToLibrary(currentTheme, report) }
                },
                onRollback = { pendingConfirmation = CloudThemePendingConfirmation.Rollback },
            )
        }
    }

    val actionBar: @Composable () -> Unit = {
        if (currentTheme != null) {
            CloudThemeDetailActionBar(
                theme = currentTheme,
                record = record,
                isActive = isActive,
                favorite = localState.isFavorite(currentTheme.id),
                compatible = compatible,
                busy = operationJob != null,
                canCancel = downloading,
                progress = progress,
                onFavorite = { favorite ->
                    scope.launch {
                        localState = withContext(Dispatchers.IO) {
                            repository.setFavorite(currentTheme.id, favorite)
                        }
                    }
                },
                onSave = {
                    startOperation { report -> installer.saveToLibrary(currentTheme, report) }
                },
                onApply = { pendingConfirmation = CloudThemePendingConfirmation.Apply },
                onCancel = { operationJob?.cancel() },
            )
        }
    }

    if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) {
        SkrootproScreen(
            title = currentTheme?.name ?: stringResource(R.string.cloud_theme_detail_title),
            bottomInnerPadding = 0.dp,
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    content(paddingValues)
                    CloudThemeDetailBackButton(onBack)
                }
                actionBar()
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
                    title = currentTheme?.name ?: stringResource(R.string.cloud_theme_detail_title),
                    color = Color.Transparent,
                    titleColor = colorScheme.onSurface,
                    navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            MiuixIcon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.close),
                                tint = colorScheme.onBackground,
                            )
                        }
                    },
                )
            },
            bottomBar = actionBar,
            content = content,
        )
    }

    pendingConfirmation?.let { confirmation ->
        val isRollback = confirmation == CloudThemePendingConfirmation.Rollback
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = {
                Text(
                    stringResource(
                        if (isRollback) R.string.cloud_theme_rollback_confirm_title
                        else R.string.cloud_theme_apply_confirm_title
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (isRollback) R.string.cloud_theme_rollback_confirm_message
                        else R.string.cloud_theme_apply_confirm_message
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConfirmation = null
                        val selectedTheme = currentTheme ?: return@Button
                        if (isRollback) {
                            startOperation { report ->
                                installer.rollbackTheme(selectedTheme.id, report)
                            }
                        } else {
                            startOperation { report ->
                                installer.applyTheme(selectedTheme, report)
                            }
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            if (isRollback) R.string.cloud_theme_rollback
                            else R.string.cloud_theme_apply_action
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmation = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CloudThemeDetailContent(
    theme: CloudTheme,
    categoryName: String,
    record: CloudThemeLocalRecord?,
    isActive: Boolean,
    canRollback: Boolean,
    busy: Boolean,
    progress: CloudThemeOperationProgress?,
    result: CloudThemeOperationResult?,
    modifier: Modifier,
    onOpenAuthor: (() -> Unit)?,
    onSave: () -> Unit,
    onRollback: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CloudThemeRemoteImage(
                url = theme.coverUrl,
                contentDescription = theme.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                maxSide = 1800,
            )
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = theme.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = cloudThemeTextColor(),
                        )
                        Text(
                            text = stringResource(
                                R.string.cloud_theme_version_meta,
                                theme.versionName,
                                theme.versionCode,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = cloudThemeMutedColor(),
                        )
                    }
                    if (theme.featured) {
                        Text(
                            text = stringResource(R.string.cloud_theme_featured),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = cloudThemeTextColor(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (onOpenAuthor != null) Modifier.clickable(onClick = onOpenAuthor) else Modifier)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cloud_theme_author, theme.author.name),
                            style = MaterialTheme.typography.labelLarge,
                            color = cloudThemeTextColor(),
                        )
                        if (theme.author.bio.isNotBlank()) {
                            Text(
                                text = theme.author.bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = cloudThemeMutedColor(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        if (theme.screenshotUrls.isNotEmpty()) {
            item {
                CloudThemeSectionTitle(stringResource(R.string.cloud_theme_screenshots))
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(theme.screenshotUrls, key = { it }) { screenshot ->
                        CloudThemeRemoteImage(
                            url = screenshot,
                            contentDescription = theme.name,
                            modifier = Modifier
                                .width(276.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp)),
                            maxSide = 1400,
                        )
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CloudThemeCompatibilityPanel(theme)
                CloudThemeInstallStatusPanel(
                    record = record,
                    theme = theme,
                    isActive = isActive,
                    canRollback = canRollback,
                    busy = busy,
                    onRollback = onRollback,
                )
                progress?.let { CloudThemeProgressPanel(it) }
                result?.let { CloudThemeResultPanel(it) }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CloudThemeSectionTitle(stringResource(R.string.cloud_theme_package_details))
                CloudThemeMetadataRow(
                    stringResource(R.string.cloud_theme_category),
                    categoryName,
                )
                CloudThemeMetadataRow(
                    stringResource(R.string.cloud_theme_file_size),
                    Formatter.formatShortFileSize(context, theme.sizeBytes),
                )
                CloudThemeMetadataRow(
                    stringResource(R.string.cloud_theme_license),
                    theme.license,
                )
                CloudThemeMetadataRow(
                    stringResource(R.string.cloud_theme_published_at),
                    formatCloudThemeDate(theme.publishedAt),
                )
                SelectionContainer {
                    CloudThemeMetadataRow(
                        stringResource(R.string.cloud_theme_sha256),
                        theme.sha256,
                    )
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && theme.isCompatible(BuildConfig.VERSION_CODE.toLong()) &&
                        theme.status == CloudThemePublicationStatus.Published,
                    onClick = onSave,
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(7.dp))
                    Text(
                        stringResource(
                            if (record?.let {
                                    it.versionCode < theme.versionCode ||
                                        (it.versionCode == theme.versionCode && it.sha256 != theme.sha256)
                                } == true
                            ) {
                                R.string.cloud_theme_save_update
                            } else {
                                R.string.cloud_theme_save_to_library
                            }
                        )
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CloudThemeSectionTitle(stringResource(R.string.cloud_theme_changelog))
                Text(
                    text = theme.changelog.ifBlank {
                        stringResource(R.string.cloud_theme_no_changelog)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = cloudThemeMutedColor(),
                )
            }
        }
    }
}

@Composable
private fun CloudThemeCompatibilityPanel(theme: CloudTheme) {
    val compatible = theme.isCompatible(BuildConfig.VERSION_CODE.toLong()) &&
        theme.status == CloudThemePublicationStatus.Published
    val tint = if (compatible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    CloudThemeDetailPanel {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (compatible) Icons.Rounded.Security else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = tint,
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(
                        if (compatible) R.string.cloud_theme_compatible
                        else R.string.cloud_theme_incompatible
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cloudThemeTextColor(),
                )
                Text(
                    text = stringResource(
                        R.string.cloud_theme_manager_range,
                        theme.minManagerVersionCode,
                        theme.maxManagerVersionCode?.toString()
                            ?: stringResource(R.string.cloud_theme_no_upper_limit),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = cloudThemeMutedColor(),
                )
                if (theme.status == CloudThemePublicationStatus.Deprecated) {
                    Text(
                        text = stringResource(R.string.cloud_theme_deprecated_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudThemeInstallStatusPanel(
    record: CloudThemeLocalRecord?,
    theme: CloudTheme,
    isActive: Boolean,
    canRollback: Boolean,
    busy: Boolean,
    onRollback: () -> Unit,
) {
    if (record == null) return
    CloudThemeDetailPanel {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (
                        record.versionCode < theme.versionCode ||
                        (record.versionCode == theme.versionCode && record.sha256 != theme.sha256)
                    ) {
                        Icons.Rounded.Update
                    } else {
                        Icons.Rounded.CheckCircle
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (isActive) {
                                R.string.cloud_theme_applied
                            } else {
                                R.string.cloud_theme_downloaded_to_library
                            }
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = cloudThemeTextColor(),
                    )
                    Text(
                        text = stringResource(
                            R.string.cloud_theme_local_version,
                            record.versionName,
                            record.versionCode,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = cloudThemeMutedColor(),
                    )
                }
            }
            if (canRollback && record.rollbackEntryId != null) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    onClick = onRollback,
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(7.dp))
                    Text(stringResource(R.string.cloud_theme_rollback))
                }
            }
        }
    }
}

@Composable
private fun CloudThemeProgressPanel(progress: CloudThemeOperationProgress) {
    CloudThemeDetailPanel {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = cloudThemeStageLabel(progress.stage),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = cloudThemeTextColor(),
            )
            val fraction = progress.fraction
            if (fraction != null) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = cloudThemeMutedColor(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CloudThemeResultPanel(result: CloudThemeOperationResult) {
    val tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val message = when {
        result.success && result.rollbackPerformed -> stringResource(R.string.cloud_theme_rollback_success)
        result.success -> stringResource(R.string.cloud_theme_operation_success)
        result.rollbackPerformed -> stringResource(R.string.cloud_theme_apply_failed_rolled_back)
        else -> stringResource(R.string.cloud_theme_operation_failed)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(
            imageVector = if (result.success) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = tint,
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = cloudThemeTextColor())
            result.error?.let {
                Text(
                    text = it.safeCloudThemeMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = cloudThemeMutedColor(),
                )
            }
            if (result.warnings.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.cloud_theme_warning_count, result.warnings.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = cloudThemeMutedColor(),
                )
            }
        }
    }
}

@Composable
private fun CloudThemeDetailActionBar(
    theme: CloudTheme,
    record: CloudThemeLocalRecord?,
    isActive: Boolean,
    favorite: Boolean,
    compatible: Boolean,
    busy: Boolean,
    canCancel: Boolean,
    progress: CloudThemeOperationProgress?,
    onFavorite: (Boolean) -> Unit,
    onSave: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
) {
    val updateAvailable = record?.let {
        it.versionCode < theme.versionCode ||
            (it.versionCode == theme.versionCode && it.sha256 != theme.sha256)
    } == true
    val activeCurrentVersion = isActive &&
        record?.appliedVersionCode == theme.versionCode &&
        record.appliedSha256 == theme.sha256

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cloudThemeSurfaceColor(),
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            if (progress?.fraction != null) {
                LinearProgressIndicator(
                    progress = { progress.fraction ?: 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(enabled = !busy, onClick = { onFavorite(!favorite) }) {
                    Icon(
                        imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(
                            if (favorite) R.string.cloud_theme_remove_favorite else R.string.cloud_theme_add_favorite
                        ),
                    )
                }
                IconButton(enabled = compatible && !busy, onClick = onSave) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = stringResource(R.string.cloud_theme_save_to_library),
                    )
                }
                if (busy && canCancel) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onCancel) {
                        Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(7.dp))
                        Text(stringResource(R.string.cloud_theme_cancel_download))
                    }
                } else {
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = compatible && !busy && !activeCurrentVersion,
                        onClick = onApply,
                    ) {
                        Icon(
                            imageVector = if (updateAvailable) {
                                Icons.Rounded.Update
                            } else {
                                Icons.Rounded.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(7.dp))
                        Text(
                            stringResource(
                                when {
                                    busy -> R.string.cloud_theme_processing
                                    updateAvailable -> {
                                        R.string.cloud_theme_update_and_apply
                                    }
                                    activeCurrentVersion -> R.string.cloud_theme_applied
                                    record?.versionCode == theme.versionCode && record.sha256 == theme.sha256 -> {
                                        R.string.cloud_theme_apply_action
                                    }
                                    else -> R.string.cloud_theme_download_and_apply
                                }
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudThemeMetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(90.dp),
            style = MaterialTheme.typography.bodySmall,
            color = cloudThemeMutedColor(),
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = cloudThemeTextColor(),
        )
    }
}

@Composable
private fun CloudThemeSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = cloudThemeTextColor(),
    )
}

@Composable
private fun CloudThemeDetailPanel(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = cloudThemeSurfaceColor(),
        tonalElevation = if (LocalInterfaceStyle.current == InterfaceStyle.Skrootpro.value) 0.dp else 1.dp,
        content = content,
    )
}

@Composable
private fun CloudThemeDetailLoading(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.55f))
    }
}

@Composable
private fun CloudThemeDetailMissing(
    modifier: Modifier,
    message: String?,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = cloudThemeMutedColor(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            stringResource(R.string.cloud_theme_not_found),
            style = MaterialTheme.typography.titleMedium,
            color = cloudThemeTextColor(),
        )
        message?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = cloudThemeMutedColor())
        }
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedButton(onClick = onRetry) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.cloud_theme_retry))
        }
    }
}

@Composable
private fun CloudThemeDetailBackButton(onClick: () -> Unit) {
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
            Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.close),
            tint = Color.White,
        )
    }
}

@Composable
private fun cloudThemeStageLabel(stage: CloudThemeOperationStage): String {
    return stringResource(
        when (stage) {
            CloudThemeOperationStage.Downloading -> R.string.cloud_theme_stage_downloading
            CloudThemeOperationStage.Verifying -> R.string.cloud_theme_stage_verifying
            CloudThemeOperationStage.Importing -> R.string.cloud_theme_stage_importing
            CloudThemeOperationStage.BackingUp -> R.string.cloud_theme_stage_backing_up
            CloudThemeOperationStage.Applying -> R.string.cloud_theme_stage_applying
            CloudThemeOperationStage.RollingBack -> R.string.cloud_theme_stage_rolling_back
        }
    )
}
