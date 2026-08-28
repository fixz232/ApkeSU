package me.weishu.kernelsu.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.theme.immersivePageColor
import me.weishu.kernelsu.ui.theme.immersiveScrolledTopBarColor
import me.weishu.kernelsu.ui.theme.immersiveSurfaceColor
import me.weishu.kernelsu.ui.theme.immersiveTopBarColor
import me.weishu.kernelsu.ui.util.SusfsPathConfigState
import me.weishu.kernelsu.ui.util.getSusfsPathConfig
import me.weishu.kernelsu.ui.util.normalizeSusfsPath
import me.weishu.kernelsu.ui.util.saveAndApplySusfsPathConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SusfsPathConfigScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val onBack = dropUnlessResumed { navigator.pop() }
    var state by remember { mutableStateOf(SusfsPathConfigState()) }
    var paths by remember { mutableStateOf(emptyList<String>()) }
    var input by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var applying by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf("") }

    fun refresh() {
        if (loading && state.toolPath.isNotBlank()) return
        scope.launch {
            loading = true
            val refreshed = getSusfsPathConfig()
            state = refreshed
            paths = refreshed.paths
            actionError = ""
            loading = false
        }
    }

    fun addPath() {
        val normalized = normalizeSusfsPath(input)
        when {
            normalized == null -> Toast.makeText(context, R.string.susfs_path_invalid, Toast.LENGTH_LONG).show()
            normalized in paths -> Toast.makeText(context, R.string.susfs_path_duplicate, Toast.LENGTH_SHORT).show()
            else -> {
                paths = paths + normalized
                input = ""
            }
        }
    }

    fun apply() {
        if (applying || !state.available) return
        scope.launch {
            applying = true
            actionError = ""
            val result = saveAndApplySusfsPathConfig(paths)
            if (result.success) {
                val refreshed = getSusfsPathConfig()
                state = if (refreshed.available) refreshed else state.copy(paths = paths)
                paths = if (refreshed.available) refreshed.paths else paths
                Toast.makeText(
                    context,
                    if (result.requiresReboot) {
                        resources.getString(R.string.susfs_path_apply_reboot)
                    } else {
                        resources.getString(R.string.susfs_path_apply_success, result.appliedCount)
                    },
                    Toast.LENGTH_LONG,
                ).show()
            } else {
                actionError = result.error.ifBlank { "apply_failed" }
            }
            applying = false
        }
    }

    LaunchedEffect(Unit) {
        val refreshed = getSusfsPathConfig()
        state = refreshed
        paths = refreshed.paths
        loading = false
    }

    Scaffold(
        containerColor = immersivePageColor(MaterialTheme.colorScheme.background),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_susfs_path_config)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    ForegroundToolProtectionTopBarAction(
                        onClick = { navigator.push(Route.ForegroundToolProtection) },
                    )
                    IconButton(onClick = ::refresh, enabled = !loading && !applying) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.susfs_path_refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = immersiveTopBarColor(MaterialTheme.colorScheme.background),
                    scrolledContainerColor = immersiveScrolledTopBarColor(
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
        },
        bottomBar = {
            Surface(
                color = immersiveSurfaceColor(
                    defaultColor = MaterialTheme.colorScheme.surface,
                    darkAlpha = 0.70f,
                    lightAlpha = 0.76f,
                ),
                tonalElevation = 3.dp,
            ) {
                Button(
                    onClick = ::apply,
                    enabled = state.available && !loading && !applying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (applying) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.susfs_path_apply))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SusfsStatusPanel(state = state, loading = loading)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.susfs_path_list),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.susfs_path_input_label)) },
                            placeholder = { Text(stringResource(R.string.susfs_path_input_hint)) },
                            singleLine = true,
                            enabled = state.available && !applying,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addPath() }),
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = ::addPath,
                            enabled = state.available && input.isNotBlank() && !applying,
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.susfs_path_add))
                        }
                    }

                    if (paths.isEmpty()) {
                        Text(
                            text = stringResource(R.string.susfs_path_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        paths.forEachIndexed { index, path ->
                            if (index > 0) HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = path,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                IconButton(
                                    onClick = { paths = paths.filterNot { it == path } },
                                    enabled = !applying,
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = stringResource(R.string.susfs_path_remove, path),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (actionError.isNotBlank()) {
                Text(
                    text = stringResource(R.string.susfs_path_apply_failed, actionError),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = immersiveSurfaceColor(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.susfs_path_notes_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.susfs_path_notes_summary),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SusfsStatusPanel(
    state: SusfsPathConfigState,
    loading: Boolean,
) {
    val available = state.available && !loading
    val message = when {
        loading -> stringResource(R.string.processing)
        state.available -> stringResource(R.string.susfs_path_available, state.toolPath)
        state.error == "gki_mode_required" -> stringResource(R.string.susfs_path_gki_required)
        state.error == "root_unavailable" -> stringResource(R.string.susfs_path_root_unavailable)
        else -> stringResource(R.string.susfs_path_tool_unavailable)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (available) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    if (available) Icons.Rounded.CheckCircleOutline else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.susfs_path_status),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
