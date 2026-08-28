package me.weishu.kernelsu.ui.screen.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.BUILTIN_MOUNT_MODE_MAGIC
import me.weishu.kernelsu.ui.util.BUILTIN_MOUNT_VARIANT_FULL
import me.weishu.kernelsu.ui.util.HYBRID_MOUNT_MODULE_ID
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import me.weishu.kernelsu.ui.webui.WebUIActivity

@Composable
fun BuiltinMountScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onBack = dropUnlessResumed { navigator.pop() }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val canOpenWebUi = uiState.isBuiltinMountEnabled && uiState.isBuiltinMountWebUiAvailable

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_builtin_mount)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.settings_builtin_mount_refresh),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BuiltinMountEnablePanel(
                uiState = uiState,
                onEnabledChange = viewModel::setBuiltinMountEnabled,
            )

            BuiltinMountChoiceSection(
                title = stringResource(R.string.settings_builtin_mount_default_mode),
                summary = stringResource(R.string.settings_builtin_mount_default_mode_summary),
                options = listOf(
                    stringResource(R.string.settings_builtin_mount_mode_overlay),
                    stringResource(R.string.settings_builtin_mount_mode_magic),
                ),
                selectedIndex = if (uiState.builtinMountDefaultMode == BUILTIN_MOUNT_MODE_MAGIC) 1 else 0,
                onSelected = viewModel::setBuiltinMountDefaultMode,
            )

            HorizontalDivider()

            BuiltinMountChoiceSection(
                title = stringResource(R.string.settings_builtin_mount_variant),
                summary = stringResource(R.string.settings_builtin_mount_variant_summary),
                options = listOf(
                    stringResource(R.string.settings_builtin_mount_variant_lite_short),
                    stringResource(R.string.settings_builtin_mount_variant_full_short),
                ),
                selectedIndex = if (uiState.builtinMountVariant == BUILTIN_MOUNT_VARIANT_FULL) 1 else 0,
                onSelected = viewModel::setBuiltinMountVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_section_advanced),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                BuiltinMountCommandButton(
                    icon = Icons.Rounded.Info,
                    label = stringResource(R.string.settings_builtin_mount_details),
                    onClick = { showDetails = true },
                )
                BuiltinMountCommandButton(
                    icon = Icons.Rounded.DeveloperMode,
                    label = stringResource(R.string.settings_builtin_mount_webui),
                    enabled = canOpenWebUi,
                    onClick = {
                        context.startActivity(
                            Intent(context, WebUIActivity::class.java)
                                .setData("kernelsu://webui/$HYBRID_MOUNT_MODULE_ID".toUri())
                                .putExtra("id", HYBRID_MOUNT_MODULE_ID),
                        )
                    },
                )
                Text(
                    text = stringResource(
                        if (canOpenWebUi) {
                            R.string.settings_builtin_mount_webui_summary
                        } else {
                            R.string.settings_builtin_mount_webui_disabled_summary
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    BuiltinMountDetailsDialog(
        show = showDetails,
        uiState = uiState,
        onDismissRequest = { showDetails = false },
    )
}

@Composable
private fun BuiltinMountEnablePanel(
    uiState: SettingsUiState,
    onEnabledChange: (Boolean) -> Unit,
) {
    val conflict = uiState.builtinMountConflict
    val summary = conflict?.let {
        stringResource(R.string.settings_builtin_mount_conflict_summary, it)
    } ?: stringResource(R.string.settings_builtin_mount_summary)
    val canToggle = conflict == null || uiState.isBuiltinMountEnabled

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (conflict == null) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        contentColor = if (conflict == null) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Layers,
                contentDescription = null,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_builtin_mount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            StyledSwitch(
                checked = uiState.isBuiltinMountEnabled,
                enabled = canToggle,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Composable
private fun BuiltinMountChoiceSection(
    title: String,
    summary: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    selected = selectedIndex == index,
                    onClick = { onSelected(index) },
                    label = {
                        Text(
                            text = option,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun BuiltinMountCommandButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}
