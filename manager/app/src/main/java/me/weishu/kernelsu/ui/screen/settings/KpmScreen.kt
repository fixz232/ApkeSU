package me.weishu.kernelsu.ui.screen.settings

import android.net.Uri
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.LocalSwitchStyle
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.custom.LocalCustomSwitchStyle
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.component.alpha.AlphaScreen
import me.weishu.kernelsu.ui.component.delta.DeltaScreen
import me.weishu.kernelsu.ui.component.delta.DeltaColors
import me.weishu.kernelsu.ui.component.skrootpro.SkrootproScreen
import me.weishu.kernelsu.ui.component.alpha.AlphaColors
import me.weishu.kernelsu.ui.component.liquid.globalLiquidGlassSurface
import me.weishu.kernelsu.ui.component.liquid.isLiquidGlassTheme
import me.weishu.kernelsu.ui.component.snow.snowMiuixCardSurface
import me.weishu.kernelsu.ui.component.pixel.pixelMiuixCardSurface
import me.weishu.kernelsu.ui.component.rain.rainMiuixCardSurface
import me.weishu.kernelsu.ui.component.ink.inkMiuixCardSurface
import me.weishu.kernelsu.ui.component.custom.CustomCardTarget
import me.weishu.kernelsu.ui.component.decoration.uiDecoratedCard
import me.weishu.kernelsu.ui.component.liquid.FrostedGlassCardStyle
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.util.KpmCaps
import me.weishu.kernelsu.ui.util.KpmCommandResult
import me.weishu.kernelsu.ui.util.KpmEntry
import me.weishu.kernelsu.ui.util.controlKpm
import me.weishu.kernelsu.ui.util.getKpmCaps
import me.weishu.kernelsu.ui.util.getKpmList
import me.weishu.kernelsu.ui.util.getFileName
import me.weishu.kernelsu.ui.util.importKpm
import me.weishu.kernelsu.ui.util.loadKpm
import me.weishu.kernelsu.ui.util.parseKpmEntries
import me.weishu.kernelsu.ui.util.removeKpm
import me.weishu.kernelsu.ui.util.setKpmEnabled
import me.weishu.kernelsu.ui.util.setKpmPolicy
import me.weishu.kernelsu.ui.util.getKpmExcludedApps
import me.weishu.kernelsu.ui.util.setKpmAppExcluded
import me.weishu.kernelsu.ui.util.unloadKpm
import me.weishu.kernelsu.ui.util.KPATCH_NEXT_MODULE_ID
import me.weishu.kernelsu.ui.screen.module.ModuleCardWallpaperBackground
import me.weishu.kernelsu.ui.screen.module.kpmCardWallpaperId
import me.weishu.kernelsu.ui.screen.module.rememberModuleCardWallpaperFrame
import me.weishu.kernelsu.ui.screen.module.rememberModuleCardWallpaperLoadState
import me.weishu.kernelsu.ui.screen.module.rememberModuleCardWallpaperState
import me.weishu.kernelsu.ui.webui.KPatchNextSection
import me.weishu.kernelsu.ui.webui.WebUIEvent
import me.weishu.kernelsu.ui.webui.WebUIScreen
import me.weishu.kernelsu.ui.webui.WebUIState
import me.weishu.kernelsu.ui.webui.prepareWebView
import java.io.File
import java.io.FileOutputStream
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar as MiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars

private const val NATIVE_KPM_MAX_IMAGE_SIZE = 4L * 1024L * 1024L

@Composable
fun KpmScreen(
    inPager: Boolean = false,
    bottomInnerPadding: Dp = 0.dp,
) {
    val activity = LocalActivity.current
    if (activity == null) {
        NativeKpmScreen(inPager = inPager, bottomInnerPadding = bottomInnerPadding)
        return
    }

    val webUIState = remember {
        WebUIState().also { it.configureKpatchNextEmbedded() }
    }
    var webUiReloadToken by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(webUIState, webUiReloadToken) {
        prepareWebView(
            activity = activity,
            moduleId = KPATCH_NEXT_MODULE_ID,
            webUIState = webUIState,
            allowPendingUpdate = true,
        )
    }
    DisposableEffect(webUIState) {
        onDispose { webUIState.dispose(activity) }
    }

    val webUiError = (webUIState.uiEvent as? WebUIEvent.Error)?.message
    if (webUiError != null) {
        NativeKpmScreen(
            inPager = inPager,
            bottomInnerPadding = bottomInnerPadding,
            webUiError = webUiError,
            onRetryWebUi = {
                webUIState.dispose(activity)
                webUIState.configureKpatchNextEmbedded()
                webUIState.uiEvent = WebUIEvent.Loading
                webUiReloadToken++
            },
        )
        return
    }

    KpatchNextEmbeddedScreen(
        webUIState = webUIState,
        inPager = inPager,
        bottomInnerPadding = bottomInnerPadding,
    )
}

@Composable
private fun KpatchNextEmbeddedScreen(
    webUIState: WebUIState,
    inPager: Boolean,
    bottomInnerPadding: Dp,
) {
    val loading = webUIState.uiEvent is WebUIEvent.Loading
    val embeddedDarkTheme = isInDarkTheme()
    val embeddedTheme = LocalInterfaceStyle.current
    val switchStyle = LocalSwitchStyle.current
    val customSwitchStyle = LocalCustomSwitchStyle.current
    var selectedSection by rememberSaveable { mutableStateOf(KPatchNextSection.Kpm) }

    LaunchedEffect(webUIState, switchStyle, customSwitchStyle, embeddedDarkTheme) {
        val effectiveStyle = if (switchStyle == SwitchStyle.Original) {
            SwitchStyle.CloudStar
        } else {
            switchStyle
        }
        webUIState.configureEmbeddedSwitchStyle(
            style = effectiveStyle.value,
            customStyleJson = customSwitchStyle
                ?.takeIf { effectiveStyle == SwitchStyle.Custom }
                ?.toJsonString(includeLocalImageUri = false),
            dark = embeddedDarkTheme,
        )
    }

    LaunchedEffect(webUIState.uiEvent, embeddedDarkTheme, embeddedTheme) {
        if (webUIState.webView == null) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) { getKpmList() }
        val entries = if (result.success) parseKpmEntries(result.output) else emptyList()
        webUIState.setKpmWallpaperEntries(entries, embeddedDarkTheme)
    }

    val titleRes = if (selectedSection == KPatchNextSection.Kpm) {
        R.string.kpm_title
    } else {
        R.string.kpm_exclude_apps
    }
    KpmStyledScreen(
        title = stringResource(titleRes),
        inPager = inPager,
        containsEmbeddedAndroidView = true,
        loading = loading,
        onKpmClick = {
            selectedSection = KPatchNextSection.Kpm
            webUIState.navigateToKpatchSection(KPatchNextSection.Kpm)
        },
        onExcludeClick = {
            selectedSection = KPatchNextSection.Exclude
            webUIState.navigateToKpatchSection(KPatchNextSection.Exclude)
        },
    ) { innerPadding ->
        when {
            loading -> KpmLoadingContent(innerPadding, bottomInnerPadding)
            webUIState.webView != null -> WebUIScreen(
                webUIState = webUIState,
                embeddedTheme = embeddedTheme,
                embeddedDarkTheme = embeddedDarkTheme,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(bottom = bottomInnerPadding),
                respectSafeDrawingInsets = false,
            )
            else -> KpmLoadingContent(innerPadding, bottomInnerPadding, showProgress = false)
        }
    }
}

@Composable
private fun KpmLoadingContent(
    innerPadding: PaddingValues,
    bottomInnerPadding: Dp,
    showProgress: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(bottom = bottomInnerPadding),
        contentAlignment = Alignment.Center,
    ) {
        if (showProgress) {
            CircularProgressIndicator()
        } else {
            Text(stringResource(R.string.kpm_webui_loading))
        }
    }
}

@Composable
private fun KpmStyledScreen(
    title: String,
    inPager: Boolean,
    containsEmbeddedAndroidView: Boolean,
    loading: Boolean = false,
    onKpmClick: () -> Unit,
    onExcludeClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }
    val style = LocalInterfaceStyle.current
    val backDescription = stringResource(R.string.close)
    val kpmDescription = stringResource(R.string.kpm_manage)
    val excludeDescription = stringResource(R.string.kpm_exclude_apps)
    val backIcon = Icons.AutoMirrored.Rounded.ArrowBack

    @Composable
    fun tint(selected: Boolean): Color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    when (style) {
        InterfaceStyle.Alpha.value -> AlphaScreen(
            title = title,
            bottomInnerPadding = 0.dp,
            transparentChrome = true,
            topActionIcon = if (!inPager) backIcon else Icons.Rounded.Bolt,
            onTopActionClick = if (!inPager) onBack else onKpmClick,
            topActionContentDescription = if (!inPager) backDescription else kpmDescription,
            secondaryTopActionIcon = if (!inPager) Icons.Rounded.Bolt else Icons.Rounded.Security,
            onSecondaryTopActionClick = if (!inPager) onKpmClick else onExcludeClick,
            secondaryTopActionContentDescription = if (!inPager) kpmDescription else excludeDescription,
            tertiaryTopActionIcon = if (!inPager) Icons.Rounded.Security else null,
            onTertiaryTopActionClick = onExcludeClick,
            tertiaryTopActionContentDescription = excludeDescription,
        ) { padding -> content(padding) }

        InterfaceStyle.Delta.value -> DeltaScreen(
            title = title,
            icon = Icons.Rounded.Bolt,
            bottomInnerPadding = 0.dp,
            transparentChrome = true,
            topActionIcon = if (!inPager) backIcon else Icons.Rounded.Bolt,
            onTopActionClick = if (!inPager) onBack else onKpmClick,
            topActionContentDescription = if (!inPager) backDescription else kpmDescription,
            secondaryTopActionIcon = if (!inPager) Icons.Rounded.Bolt else Icons.Rounded.Security,
            onSecondaryTopActionClick = if (!inPager) onKpmClick else onExcludeClick,
            secondaryTopActionContentDescription = if (!inPager) kpmDescription else excludeDescription,
            tertiaryTopActionIcon = if (!inPager) Icons.Rounded.Security else null,
            onTertiaryTopActionClick = onExcludeClick,
            tertiaryTopActionContentDescription = excludeDescription,
        ) { padding -> content(padding) }

        InterfaceStyle.Skrootpro.value -> SkrootproScreen(
            title = title,
            showAdd = !inPager,
            onAddClick = onBack,
            actionIcon = backIcon,
            actionContentDescription = backDescription,
            secondaryActionIcon = Icons.Rounded.Bolt,
            onSecondaryActionClick = onKpmClick,
            secondaryActionContentDescription = kpmDescription,
            tertiaryActionIcon = Icons.Rounded.Security,
            onTertiaryActionClick = onExcludeClick,
            tertiaryActionContentDescription = excludeDescription,
            bottomInnerPadding = 0.dp,
        ) { padding -> content(padding) }

        InterfaceStyle.Material.value -> Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
            topBar = {
                androidx.compose.material3.TopAppBar(
                    title = { Text(title) },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    navigationIcon = {
                        if (!inPager) {
                            IconButton(onClick = onBack) {
                                Icon(backIcon, backDescription)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onKpmClick, enabled = !loading) {
                            Icon(Icons.Rounded.Bolt, kpmDescription, tint = tint(true))
                        }
                        IconButton(onClick = onExcludeClick, enabled = !loading) {
                            Icon(Icons.Rounded.Security, excludeDescription)
                        }
                    },
                )
            },
            content = content,
        )

        else -> KpmStyledMiuixScreen(
            title = title,
            inPager = inPager,
            containsEmbeddedAndroidView = containsEmbeddedAndroidView,
            loading = loading,
            onBack = onBack,
            onKpmClick = onKpmClick,
            onExcludeClick = onExcludeClick,
            content = content,
        )
    }
}

@Composable
private fun KpmStyledMiuixScreen(
    title: String,
    inPager: Boolean,
    containsEmbeddedAndroidView: Boolean,
    loading: Boolean,
    onBack: () -> Unit,
    onKpmClick: () -> Unit,
    onExcludeClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backIcon = Icons.AutoMirrored.Rounded.ArrowBack
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current && !containsEmbeddedAndroidView)
    val scrollBehavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior()
    val barColor = Color.Transparent
    MiuixScaffold(
        containerColor = Color.Transparent,
        topBar = {
            BlurredBar(backdrop, blurActive = !containsEmbeddedAndroidView) {
                MiuixSmallTopAppBar(
                    title = title,
                    color = barColor,
                    titleColor = MiuixTheme.colorScheme.onSurface,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        if (!inPager) {
                            MiuixIconButton(onClick = onBack) {
                                MiuixIcon(backIcon, stringResource(R.string.close), tint = MiuixTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    actions = {
                        MiuixIconButton(onClick = onKpmClick) {
                            MiuixIcon(Icons.Rounded.Bolt, stringResource(R.string.kpm_manage), tint = MiuixTheme.colorScheme.primary)
                        }
                        MiuixIconButton(onClick = onExcludeClick) {
                            MiuixIcon(
                                Icons.Rounded.Security,
                                stringResource(R.string.kpm_exclude_apps),
                                tint = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    },
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null && !containsEmbeddedAndroidView) Modifier.layerBackdrop(backdrop) else Modifier) {
            content(innerPadding)
        }
    }
}

@Composable
private fun NativeKpmScreen(
    inPager: Boolean = false,
    bottomInnerPadding: Dp = 0.dp,
    webUiError: String? = null,
    onRetryWebUi: (() -> Unit)? = null,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val resources = context.resources
    val scope = rememberCoroutineScope()
    val onBack = dropUnlessResumed { navigator.pop() }

    var caps by remember { mutableStateOf<KpmCaps?>(null) }
    var entries by remember { mutableStateOf(emptyList<KpmEntry>()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var importArgs by rememberSaveable { mutableStateOf("") }
    var replaceExisting by rememberSaveable { mutableStateOf(false) }
    var enableAfterImport by rememberSaveable { mutableStateOf(false) }
    var trustAcknowledged by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<KpmEntry?>(null) }
    var controlTarget by remember { mutableStateOf<KpmEntry?>(null) }
    var controlArgs by rememberSaveable { mutableStateOf("") }
    var controlOutput by remember { mutableStateOf("") }
    var excludedApps by remember { mutableStateOf(emptySet<String>()) }
    var showExcludeDialog by rememberSaveable { mutableStateOf(false) }
    var excludeCandidates by remember { mutableStateOf(emptyList<KpmAppCandidate>()) }
    var excludeCandidatesLoading by remember { mutableStateOf(false) }
    var excludeCandidatesError by remember { mutableStateOf("") }
    var excludeCandidatesRefreshToken by remember { mutableStateOf(0) }
    val refreshMutex = remember { Mutex() }
    var refreshJob by remember { mutableStateOf<Job?>(null) }
    var refreshRequested by remember { mutableStateOf(false) }

    fun refresh() {
        if (refreshJob?.isActive == true) {
            refreshRequested = true
            return
        }
        refreshJob = scope.launch {
            refreshMutex.withLock {
                do {
                    refreshRequested = false
                    loading = true
                    errorMessage = ""
                    try {
                        val nextCaps = getKpmCaps()
                        caps = nextCaps
                        if (nextCaps.error.isNotBlank()) {
                            entries = emptyList()
                            errorMessage = nextCaps.error
                        } else if (nextCaps.lateLoad || !nextCaps.managementAvailable) {
                            entries = emptyList()
                        } else {
                            val listResult = getKpmList()
                            if (listResult.success) {
                                entries = parseKpmEntries(listResult.output)
                                val excludedResult = runCatching { getKpmExcludedApps() }
                                excludedResult.onSuccess { apps ->
                                    excludedApps = apps.mapTo(mutableSetOf()) { it.packageName }
                                }.onFailure { error ->
                                    errorMessage = error.message.orEmpty().ifBlank {
                                        resources.getString(R.string.kpm_status_failed)
                                    }
                                }
                            } else {
                                entries = emptyList()
                                errorMessage = listResult.error.ifBlank {
                                    resources.getString(R.string.kpm_status_failed)
                                }
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        entries = emptyList()
                        errorMessage = error.message.orEmpty().ifBlank {
                            resources.getString(R.string.kpm_status_failed)
                        }
                    } finally {
                        loading = false
                    }
                } while (refreshRequested)
            }
        }
    }

    fun showOperationResult(result: KpmCommandResult) {
        val message = if (result.success) {
            result.output.ifBlank { resources.getString(R.string.kpm_operation_succeeded) }
        } else {
            result.error.ifBlank { resources.getString(R.string.kpm_operation_failed) }
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun runOperation(block: suspend () -> KpmCommandResult) {
        if (busy) return
        scope.launch {
            busy = true
            try {
                val result = block()
                showOperationResult(result)
                if (result.success) {
                    refreshRequested = true
                    refresh()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Toast.makeText(
                    context,
                    error.message.orEmpty().ifBlank { resources.getString(R.string.kpm_operation_failed) },
                    Toast.LENGTH_LONG,
                ).show()
            } finally {
                busy = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            selectedImportUri = uri
            trustAcknowledged = false
            showImportDialog = true
        }
    }

    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose {
            refreshRequested = false
            refreshJob?.cancel()
            refreshJob = null
        }
    }

    KpmStyledScreen(
        title = stringResource(R.string.kpm_title),
        inPager = inPager,
        containsEmbeddedAndroidView = false,
        loading = loading || busy,
        onKpmClick = {
            if (webUiError != null && onRetryWebUi != null) onRetryWebUi() else refresh()
        },
        onExcludeClick = { showExcludeDialog = true },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = bottomInnerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            webUiError?.let { message ->
                KpmMessageCard(
                    icon = Icons.Rounded.ErrorOutline,
                    title = stringResource(R.string.kpm_webui_unavailable),
                    message = stringResource(R.string.kpm_webui_fallback, message),
                    error = true,
                )
                if (onRetryWebUi != null) {
                    OutlinedButton(onClick = onRetryWebUi, enabled = !busy) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.kpm_webui_retry))
                    }
                }
            }

            KpmOverviewCard(
                caps = caps,
                loading = loading,
                entryCount = entries.size,
                onImport = { importLauncher.launch(arrayOf("*/*")) },
                onManageExclusions = { showExcludeDialog = true },
                onPolicyChanged = { enabled ->
                    runOperation { setKpmPolicy(enabled) }
                },
            )

            if (errorMessage.isNotBlank()) {
                KpmMessageCard(
                    icon = Icons.Rounded.ErrorOutline,
                    title = stringResource(R.string.kpm_status_failed),
                    message = errorMessage,
                    error = true,
                )
            }

            if (caps?.let { it.managementAvailable && !it.lateLoad && !it.policyEnabled } == true &&
                errorMessage.isBlank()
            ) {
                KpmMessageCard(
                    icon = Icons.Rounded.StopCircle,
                    title = stringResource(R.string.kpm_policy_disabled),
                    message = stringResource(R.string.kpm_policy_disabled_summary_long),
                )
            }

            when {
                loading && caps == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                caps?.lateLoad == true && errorMessage.isBlank() -> {
                    KpmMessageCard(
                        icon = Icons.Rounded.Security,
                        title = stringResource(R.string.kpm_jailbreak_disabled),
                        message = stringResource(R.string.kpm_jailbreak_disabled_summary),
                    )
                }
                caps?.kernelSupported == false && errorMessage.isBlank() -> {
                    val kpatchPending = caps?.backend == "kpatch-next" && caps?.managementAvailable == true
                    KpmMessageCard(
                        icon = Icons.Rounded.Info,
                        title = stringResource(
                            if (kpatchPending) {
                                R.string.kpm_kpatch_pending
                            } else {
                                R.string.kpm_unsupported
                            }
                        ),
                        message = stringResource(
                            if (kpatchPending) {
                                R.string.kpm_kpatch_pending_summary
                            } else {
                                R.string.kpm_unsupported_summary
                            }
                        ),
                    )
                }
                !loading && entries.isEmpty() && errorMessage.isBlank() -> {
                    KpmMessageCard(
                        icon = Icons.Rounded.Info,
                        title = stringResource(R.string.kpm_empty),
                        message = stringResource(R.string.kpm_empty_summary),
                    )
                }
                else -> entries.forEach { entry ->
                    KpmEntryCard(
                        entry = entry,
                        busy = busy,
                        managementEnabled = caps?.managementAvailable == true && caps?.policyEnabled == true,
                        runtimeEnabled = caps?.kernelSupported == true && caps?.policyEnabled == true,
                        onEnableChanged = { enabled ->
                            runOperation { setKpmEnabled(entry.id, enabled) }
                        },
                        onLoad = { runOperation { loadKpm(entry.id) } },
                        onUnload = { runOperation { unloadKpm(entry.id) } },
                        onDelete = { pendingDelete = entry },
                        onControl = {
                            controlTarget = entry
                            controlArgs = entry.args
                            controlOutput = ""
                        },
                        onEditWallpaper = {
                            navigator.push(
                                Route.ModuleWallpaperEditor(
                                    moduleId = kpmCardWallpaperId(entry.id),
                                    displayName = entry.name.ifBlank { entry.id },
                                    displayAuthor = entry.author,
                                    displayVersion = entry.version,
                                    displayDescription = entry.description,
                                    allowBatch = false,
                                )
                            )
                        },
                    )
                }
            }

            Text(
                text = stringResource(R.string.kpm_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showExcludeDialog) {
        LaunchedEffect(showExcludeDialog, excludeCandidatesRefreshToken) {
            excludeCandidatesLoading = true
            excludeCandidatesError = ""
            try {
                excludeCandidates = withContext(Dispatchers.IO) {
                    loadKpmAppCandidates(context)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                excludeCandidatesError = error.message.orEmpty().ifBlank {
                    resources.getString(R.string.kpm_status_failed)
                }
            } finally {
                excludeCandidatesLoading = false
            }
        }
        KpmExcludeDialog(
            candidates = excludeCandidates,
            loading = excludeCandidatesLoading,
            loadError = excludeCandidatesError,
            excludedPackages = excludedApps,
            busy = busy,
            onDismiss = { if (!busy) showExcludeDialog = false },
            onRetry = {
                excludeCandidatesError = ""
                excludeCandidates = emptyList()
                excludeCandidatesRefreshToken++
            },
            onToggle = { candidate, checked ->
                if (busy) return@KpmExcludeDialog
                scope.launch {
                    busy = true
                    try {
                        val result = setKpmAppExcluded(
                            candidate.packageName,
                            candidate.uid,
                            checked,
                        )
                        showOperationResult(result)
                        if (result.success) {
                            excludedApps = if (checked) {
                                excludedApps + candidate.packageName
                            } else {
                                excludedApps - candidate.packageName
                            }
                        } else {
                            Toast.makeText(
                                context,
                                result.error.ifBlank { resources.getString(R.string.kpm_operation_failed) },
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        Toast.makeText(
                            context,
                            error.message.orEmpty().ifBlank { resources.getString(R.string.kpm_operation_failed) },
                            Toast.LENGTH_LONG,
                        ).show()
                    } finally {
                        busy = false
                    }
                }
            },
        )
    }

    if (showImportDialog && selectedImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                if (!busy) {
                    showImportDialog = false
                    selectedImportUri = null
                }
            },
            icon = { Icon(Icons.Rounded.Security, contentDescription = null) },
            title = { Text(stringResource(R.string.kpm_import_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = selectedImportUri?.getFileName(context)
                            ?: stringResource(R.string.kpm_selected_file),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(stringResource(R.string.kpm_import_warning))
                    OutlinedTextField(
                        value = importArgs,
                        onValueChange = { importArgs = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.kpm_arguments)) },
                        singleLine = false,
                        minLines = 2,
                        enabled = !busy,
                    )
                    KpmCheckRow(
                        checked = trustAcknowledged,
                        enabled = !busy,
                        title = stringResource(R.string.kpm_trust_acknowledgement),
                        onCheckedChange = { trustAcknowledged = it },
                    )
                    KpmCheckRow(
                        checked = enableAfterImport,
                        enabled = !busy,
                        title = stringResource(R.string.kpm_enable_after_import),
                        onCheckedChange = { enableAfterImport = it },
                    )
                    KpmCheckRow(
                        checked = replaceExisting,
                        enabled = !busy,
                        title = stringResource(R.string.kpm_replace_existing),
                        onCheckedChange = { replaceExisting = it },
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = trustAcknowledged && !busy,
                    onClick = {
                        val uri = selectedImportUri
                        if (uri != null) {
                            showImportDialog = false
                            scope.launch {
                                busy = true
                                var temporary: File? = null
                                try {
                                    temporary = withContext(Dispatchers.IO) {
                                        copyKpmToCache(context, uri)
                                    }
                                    val result = importKpm(
                                        source = temporary,
                                        args = importArgs,
                                        force = replaceExisting,
                                        enable = enableAfterImport,
                                    )
                                    showOperationResult(result)
                                    if (result.success) refresh()
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Throwable) {
                                    Toast.makeText(
                                        context,
                                        error.message.orEmpty().ifBlank {
                                            resources.getString(R.string.kpm_import_failed)
                                        },
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } finally {
                                    temporary?.delete()
                                    selectedImportUri = null
                                    busy = false
                                }
                            }
                        }
                    },
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.kpm_import))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        showImportDialog = false
                        selectedImportUri = null
                    },
                ) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingDelete = null },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.kpm_remove_title)) },
            text = { Text(stringResource(R.string.kpm_remove_summary, entry.name.ifBlank { entry.id })) },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        pendingDelete = null
                        runOperation { removeKpm(entry.id) }
                    },
                ) { Text(stringResource(R.string.kpm_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }, enabled = !busy) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    controlTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { if (!busy) controlTarget = null },
            icon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
            title = { Text(stringResource(R.string.kpm_control_title, entry.name.ifBlank { entry.id })) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = controlArgs,
                        onValueChange = { controlArgs = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.kpm_arguments)) },
                        enabled = !busy,
                        minLines = 2,
                    )
                    if (controlOutput.isNotBlank()) {
                        HorizontalDivider()
                        Text(
                            text = controlOutput,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val result = controlKpm(entry.id, controlArgs)
                                controlOutput = if (result.success) {
                                    result.output.ifBlank { resources.getString(R.string.kpm_empty_output) }
                                } else {
                                    result.error
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Throwable) {
                                controlOutput = error.message.orEmpty().ifBlank {
                                    resources.getString(R.string.kpm_operation_failed)
                                }
                            } finally {
                                busy = false
                            }
                        }
                    },
                ) { Text(stringResource(R.string.kpm_run_control)) }
            },
            dismissButton = {
                TextButton(onClick = { controlTarget = null }, enabled = !busy) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun KpmOverviewCard(
    caps: KpmCaps?,
    loading: Boolean,
    entryCount: Int,
    onImport: () -> Unit,
    onManageExclusions: () -> Unit,
    onPolicyChanged: (Boolean) -> Unit,
) {
    val backendName = stringResource(R.string.kpm_backend_kpatch_next)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.kpm_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when {
                            loading -> stringResource(R.string.kpm_checking)
                            caps?.lateLoad == true -> stringResource(R.string.kpm_jailbreak_disabled)
                            caps?.let { it.backend == "kpatch-next" && !it.kernelSupported } == true ->
                                stringResource(R.string.kpm_kpatch_pending)
                            caps?.kernelSupported != true -> stringResource(R.string.kpm_unsupported)
                            caps.policyEnabled -> stringResource(
                                R.string.kpm_capability_summary,
                                backendName,
                                caps.abiVersion,
                                caps.maxLoaded,
                            )
                            else -> stringResource(R.string.kpm_policy_disabled)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (caps?.managementAvailable == true && !caps.lateLoad) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.kpm_policy_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(
                                if (caps.policyEnabled) {
                                    R.string.kpm_policy_enabled_summary
                                } else {
                                    R.string.kpm_policy_disabled_summary
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StyledSwitch(
                        checked = caps.policyEnabled,
                        onCheckedChange = onPolicyChanged,
                        enabled = !loading,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.kpm_count_summary,
                        entryCount,
                        formatKpmBytes(caps.maxImageSize),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onImport, enabled = !loading && caps.policyEnabled) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.kpm_import))
                }
                OutlinedButton(
                    onClick = onManageExclusions,
                    enabled = !loading && caps.policyEnabled,
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.kpm_exclude_apps))
                }
            }
        }
    }
}

private data class KpmAppCandidate(
    val packageName: String,
    val uid: Int,
    val label: String,
)

private suspend fun loadKpmAppCandidates(context: android.content.Context): List<KpmAppCandidate> {
    return withContext(Dispatchers.IO) {
        val packages = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstalledPackages(0)
        }
        packages.mapNotNull { info ->
            val appInfo = info.applicationInfo ?: return@mapNotNull null
            val uid = appInfo.uid
            if (uid <= 0) return@mapNotNull null
            KpmAppCandidate(
                packageName = info.packageName,
                uid = uid,
                label = runCatching { appInfo.loadLabel(context.packageManager).toString() }
                    .getOrDefault(info.packageName),
            )
        }.distinctBy { it.packageName }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}

@Composable
private fun KpmExcludeDialog(
    candidates: List<KpmAppCandidate>,
    loading: Boolean,
    loadError: String,
    excludedPackages: Set<String>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onToggle: (KpmAppCandidate, Boolean) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(candidates, query) {
        val text = query.trim().lowercase()
        candidates.filter {
            text.isBlank() || it.label.lowercase().contains(text) || it.packageName.lowercase().contains(text)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Security, contentDescription = null) },
        title = { Text(stringResource(R.string.kpm_exclude_apps)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.kpm_exclude_apps_summary),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.kpm_search)) },
                    enabled = !busy,
                )
                when {
                    loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    loadError.isNotBlank() -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(loadError, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = onRetry, enabled = !busy) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.kpm_refresh))
                        }
                    }
                    filtered.isEmpty() -> Text(stringResource(R.string.kpm_exclude_apps_empty))
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(filtered, key = { it.packageName }) { candidate ->
                            val checked = candidate.packageName in excludedPackages
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !busy) { onToggle(candidate, !checked) },
                                headlineContent = {
                                    Text(candidate.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                supportingContent = {
                                    Text(
                                        "${candidate.packageName}\nUID ${candidate.uid}",
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                trailingContent = {
                                    StyledSwitch(
                                        checked = checked,
                                        onCheckedChange = { onToggle(candidate, it) },
                                        enabled = !busy,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun KpmEntryCard(
    entry: KpmEntry,
    busy: Boolean,
    managementEnabled: Boolean,
    runtimeEnabled: Boolean,
    onEnableChanged: (Boolean) -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit,
    onControl: () -> Unit,
    onEditWallpaper: () -> Unit,
) {
    val style = LocalInterfaceStyle.current
    val wallpaperState = rememberModuleCardWallpaperState(kpmCardWallpaperId(entry.id))
    val wallpaperEntry = rememberModuleCardWallpaperFrame(wallpaperState, paused = busy)
    val wallpaperLoadState = rememberModuleCardWallpaperLoadState(wallpaperEntry)
    val wallpaperBitmap = wallpaperLoadState.bitmap
    val hasWallpaper = wallpaperEntry != null
    val cardShape = if (style == InterfaceStyle.Pixel.value) RectangleShape else RoundedCornerShape(16.dp)
    val cardSurfaceColor = when (style) {
        InterfaceStyle.Alpha.value -> AlphaColors.Surface
        InterfaceStyle.Delta.value -> DeltaColors.Surface
        InterfaceStyle.Skrootpro.value -> me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors.Surface
        else -> immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow)
    }
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            when (style) {
                InterfaceStyle.Pixel.value -> Modifier.pixelMiuixCardSurface(shape = cardShape)
                InterfaceStyle.Snow.value,
                InterfaceStyle.Rain.value,
                InterfaceStyle.Ink.value -> Modifier.snowMiuixCardSurface(shape = cardShape)
                InterfaceStyle.LiquidGlass.value -> Modifier.globalLiquidGlassSurface(
                    shape = cardShape,
                    surfaceAlpha = 0.56f,
                    blurRadius = 12.dp,
                    cardStyle = FrostedGlassCardStyle.Mist,
                )
                InterfaceStyle.Alpha.value -> Modifier
                    .clip(cardShape)
                    .background(AlphaColors.Surface)
                    .border(1.dp, AlphaColors.Accent.copy(alpha = 0.34f), cardShape)
                    .uiDecoratedCard(shape = cardShape)
                InterfaceStyle.Delta.value -> Modifier
                    .clip(cardShape)
                    .background(DeltaColors.Surface)
                    .border(1.dp, DeltaColors.Accent.copy(alpha = 0.44f), cardShape)
                    .uiDecoratedCard(shape = cardShape)
                InterfaceStyle.Skrootpro.value -> Modifier
                    .clip(cardShape)
                    .background(me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors.Surface)
                    .border(1.dp, me.weishu.kernelsu.ui.component.skrootpro.SkrootproColors.Purple.copy(alpha = 0.52f), cardShape)
                    .uiDecoratedCard(shape = cardShape)
                else -> Modifier
                    .clip(cardShape)
                    .background(cardSurfaceColor)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
                    .uiDecoratedCard(shape = cardShape)
            }
        )
    val statusColor = when {
        entry.quarantined -> MaterialTheme.colorScheme.error
        entry.loaded -> MaterialTheme.colorScheme.primary
        entry.enabled -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = cardModifier,
        shape = cardShape,
        color = if (hasWallpaper) Color.Transparent else cardSurfaceColor,
    ) {
        Box {
            ModuleCardWallpaperBackground(
                bitmap = wallpaperBitmap,
                entry = wallpaperEntry,
                contentIsLight = isInDarkTheme(),
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name.ifBlank { entry.id },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(
                            entry.id.takeIf { it != entry.name },
                            entry.version.takeIf(String::isNotBlank)?.let { "v$it" },
                            entry.author.takeIf(String::isNotBlank),
                        ).joinToString(" · ").ifBlank { stringResource(R.string.kpm_metadata_missing) },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onEditWallpaper, enabled = !busy) {
                    Icon(
                        Icons.Rounded.Image,
                        contentDescription = stringResource(R.string.module_wallpaper_editor_open),
                        tint = statusColor,
                    )
                }
                IconButton(onClick = onDelete, enabled = !busy) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.kpm_remove),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (entry.description.isNotBlank()) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = when {
                    entry.quarantined -> stringResource(
                        R.string.kpm_quarantined,
                        entry.quarantineReason.ifBlank { stringResource(R.string.kpm_unknown_reason) },
                    )
                    entry.loaded -> stringResource(R.string.kpm_loaded)
                    entry.enabled -> stringResource(R.string.kpm_enabled)
                    else -> stringResource(R.string.kpm_disabled)
                },
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
            )
            if (entry.error.isNotBlank()) {
                KpmInlineNotice(
                    icon = Icons.Rounded.ErrorOutline,
                    text = entry.error,
                    error = true,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (entry.enabled) {
                    OutlinedButton(onClick = { onEnableChanged(false) }, enabled = !busy) {
                        Icon(Icons.Rounded.StopCircle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.kpm_disable))
                    }
                } else {
                    Button(onClick = { onEnableChanged(true) }, enabled = !busy && managementEnabled) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.kpm_enable))
                    }
                }
                if (entry.loaded) {
                    OutlinedButton(onClick = onUnload, enabled = !busy) {
                        Icon(Icons.Rounded.StopCircle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.kpm_unload))
                    }
                    OutlinedButton(onClick = onControl, enabled = !busy && runtimeEnabled) {
                        Icon(Icons.Rounded.Tune, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.kpm_control))
                    }
                } else {
                    OutlinedButton(
                        onClick = onLoad,
                        enabled = !busy && runtimeEnabled && entry.enabled && !entry.quarantined,
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.kpm_load))
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun KpmMessageCard(
    icon: ImageVector,
    title: String,
    message: String,
    error: Boolean = false,
) {
    val color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun KpmInlineNotice(icon: ImageVector, text: String, error: Boolean) {
    val color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun KpmCheckRow(
    checked: Boolean,
    enabled: Boolean,
    title: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.bodySmall)
    }
}

private fun copyKpmToCache(context: android.content.Context, uri: Uri): File {
    val target = File.createTempFile("apkesu-kpm-", ".kpm", context.cacheDir)
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: error(context.getString(R.string.kpm_file_open_failed))
        input.use { source ->
            FileOutputStream(target).use { destination ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > NATIVE_KPM_MAX_IMAGE_SIZE) {
                        error(context.getString(R.string.kpm_file_too_large))
                    }
                    destination.write(buffer, 0, count)
                }
                destination.fd.sync()
            }
        }
        return target
    } catch (error: Throwable) {
        target.delete()
        throw error
    }
}

private fun formatKpmBytes(value: Long): String {
    if (value <= 0) return "-"
    return if (value >= 1024L * 1024L) {
        "${value / (1024L * 1024L)} MiB"
    } else {
        "${value / 1024L} KiB"
    }
}
