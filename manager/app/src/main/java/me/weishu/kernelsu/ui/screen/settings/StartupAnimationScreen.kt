package me.weishu.kernelsu.ui.screen.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.StartupAnimationOverlay
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.CUSTOM_STARTUP_ANIMATION_MIME_TYPES
import me.weishu.kernelsu.ui.util.isCustomStartupAnimationVideo
import me.weishu.kernelsu.ui.util.releasePersistableStartupAnimationReadPermission
import me.weishu.kernelsu.ui.util.takePersistableStartupAnimationReadPermission
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

@Composable
fun StartupAnimationScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showPreview = rememberSaveable { mutableStateOf(false) }
    val previewUri = rememberSaveable { mutableStateOf<String?>(null) }

    val startupAnimationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        takePersistableStartupAnimationReadPermission(context, uri)
        val uriString = uri.toString()
        if (uiState.customStartupAnimationUri != uriString) {
            releasePersistableStartupAnimationReadPermission(context, uiState.customStartupAnimationUri)
        }
        viewModel.setCustomStartupAnimationUri(uriString)
        if (!isCustomStartupAnimationVideo(context, uri)) {
            previewUri.value = uriString
            showPreview.value = true
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val actions = StartupAnimationActions(
        onPick = { startupAnimationLauncher.launch(CUSTOM_STARTUP_ANIMATION_MIME_TYPES) },
        onPreview = {
            uiState.customStartupAnimationUri?.let { uriString ->
                previewUri.value = uriString
                showPreview.value = true
            }
        },
        onClear = {
            releasePersistableStartupAnimationReadPermission(context, uiState.customStartupAnimationUri)
            viewModel.clearCustomStartupAnimation()
            showPreview.value = false
            previewUri.value = null
        },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when (LocalUiMode.current) {
            UiMode.Material -> StartupAnimationScreenMaterial(
                uiState = uiState,
                actions = actions,
                onBack = onBack,
            )

            UiMode.Miuix -> StartupAnimationScreenMiuix(
                uiState = uiState,
                actions = actions,
                onBack = onBack,
            )
        }

        if (showPreview.value && !previewUri.value.isNullOrBlank()) {
            StartupAnimationOverlay(
                uriString = previewUri.value,
                onFinished = {
                    showPreview.value = false
                    previewUri.value = null
                },
                onError = {
                    showPreview.value = false
                    previewUri.value = null
                    Toast.makeText(context, R.string.settings_startup_animation_play_failed, Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}

@Composable
private fun StartupAnimationScreenMaterial(
    uiState: SettingsUiState,
    actions: StartupAnimationActions,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_startup_animation)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        StartupAnimationContent(
            uiState = uiState,
            actions = actions,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun StartupAnimationScreenMiuix(
    uiState: SettingsUiState,
    actions: StartupAnimationActions,
    onBack: () -> Unit,
) {
    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.settings_startup_animation),
                color = Color.Transparent,
                titleColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface,
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        StartupAnimationContent(
            uiState = uiState,
            actions = actions,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun StartupAnimationContent(
    uiState: SettingsUiState,
    actions: StartupAnimationActions,
    modifier: Modifier,
) {
    val selected = !uiState.customStartupAnimationUri.isNullOrBlank()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_startup_animation_page_summary),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        StartupAnimationStatusCard(
            selected = selected,
            uriString = uiState.customStartupAnimationUri,
        )

        StartupAnimationActionCard(
            selected = selected,
            onPick = actions.onPick,
            onPreview = actions.onPreview,
            onClear = actions.onClear,
        )

        StartupAnimationHintCard()
    }
}

@Composable
private fun StartupAnimationStatusCard(
    selected: Boolean,
    uriString: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_startup_animation_status_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        if (selected) {
                            R.string.settings_startup_animation_status_selected
                        } else {
                            R.string.settings_startup_animation_status_empty
                        }
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (selected && !uriString.isNullOrBlank()) {
                    Text(
                        text = uriString,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun StartupAnimationActionCard(
    selected: Boolean,
    onPick: () -> Unit,
    onPreview: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StartupAnimationCardTitle(
                icon = Icons.Rounded.PlayCircle,
                title = stringResource(R.string.settings_startup_animation),
                summary = stringResource(
                    if (selected) {
                        R.string.settings_startup_animation_selected_summary
                    } else {
                        R.string.settings_startup_animation_summary
                    }
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = onPick,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_startup_animation_choose),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onPreview,
                    enabled = selected,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_audio_preview),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onClear,
                    enabled = selected,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_audio_clear),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun StartupAnimationHintCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
    ) {
        StartupAnimationCardTitle(
            modifier = Modifier.padding(14.dp),
            icon = Icons.Rounded.Info,
            title = stringResource(R.string.settings_startup_animation_hint_title),
            summary = stringResource(R.string.settings_startup_animation_hint),
        )
    }
}

@Composable
private fun StartupAnimationCardTitle(
    icon: ImageVector,
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private data class StartupAnimationActions(
    val onPick: () -> Unit,
    val onPreview: () -> Unit,
    val onClear: () -> Unit,
)
