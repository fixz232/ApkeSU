package me.weishu.kernelsu.ui.screen.themestore

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.material.SegmentedColumn
import me.weishu.kernelsu.ui.component.material.SegmentedRadioItem
import me.weishu.kernelsu.ui.component.material.TonalCard
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.AppFontPreset
import me.weishu.kernelsu.ui.util.AppFontState
import me.weishu.kernelsu.ui.util.MAX_APP_FONT_OPACITY
import me.weishu.kernelsu.ui.util.MIN_APP_FONT_OPACITY
import me.weishu.kernelsu.ui.util.SavedAppFont
import me.weishu.kernelsu.ui.util.deleteSavedAppFont
import me.weishu.kernelsu.ui.util.importCustomAppFont
import me.weishu.kernelsu.ui.util.readAppFontState
import me.weishu.kernelsu.ui.util.removeCustomAppFont
import me.weishu.kernelsu.ui.util.selectSavedAppFont
import me.weishu.kernelsu.ui.util.setAppFontOpacity
import me.weishu.kernelsu.ui.util.setAppFontPreset
import java.util.Locale
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val APP_FONT_MIME_TYPES = arrayOf(
    "font/ttf",
    "application/x-font-ttf",
    "application/octet-stream",
)

@Composable
fun AppFontScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val onBack = dropUnlessResumed { navigator.pop() }
    var state by remember { mutableStateOf(readAppFontState(context)) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<AppFontMessage?>(null) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    var pendingDeleteFont by remember { mutableStateOf<SavedAppFont?>(null) }
    val applySuccessText = stringResource(R.string.app_font_apply_success)
    val applyFailedText = stringResource(R.string.app_font_apply_failed)
    val importSuccessText = stringResource(R.string.app_font_import_success)
    val importFailedText = stringResource(R.string.app_font_import_failed)
    val removeSuccessText = stringResource(R.string.app_font_remove_success)
    val removeFailedText = stringResource(R.string.app_font_remove_failed)
    val deleteSuccessText = stringResource(R.string.app_font_saved_delete_success)
    val deleteFailedText = stringResource(R.string.app_font_saved_delete_failed)

    fun refresh() {
        state = readAppFontState(context)
    }

    fun applyPreset(preset: AppFontPreset) {
        if (busy || preset == state.preset) return
        busy = true
        message = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { check(setAppFontPreset(context, preset)) }
            }
            if (result.isSuccess) {
                refresh()
                message = AppFontMessage(
                    text = applySuccessText,
                    isError = false,
                )
            } else {
                message = AppFontMessage(
                    text = result.exceptionOrNull().appFontError(applyFailedText),
                    isError = true,
                )
            }
            busy = false
        }
    }

    fun selectSavedFont(font: SavedAppFont) {
        if (busy || font.id == state.customId && state.isCustomActive) return
        busy = true
        message = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                selectSavedAppFont(context, font.id)
            }
            result.onSuccess { selected ->
                state = selected
                message = AppFontMessage(applySuccessText, isError = false)
            }.onFailure { error ->
                message = AppFontMessage(
                    text = error.appFontError(applyFailedText),
                    isError = true,
                )
            }
            busy = false
        }
    }

    val fontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        if (busy) return@rememberLauncherForActivityResult
        busy = true
        message = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                importCustomAppFont(context, uri)
            }
            result.onSuccess { imported ->
                state = imported
                message = AppFontMessage(
                    text = importSuccessText,
                    isError = false,
                )
                Toast.makeText(context, R.string.app_font_import_success, Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                message = AppFontMessage(
                    text = error.appFontError(importFailedText),
                    isError = true,
                )
                Toast.makeText(context, R.string.app_font_import_failed, Toast.LENGTH_SHORT).show()
            }
            busy = false
        }
    }

    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }

    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.app_font_title),
                color = Color.Transparent,
                titleColor = MiuixTheme.colorScheme.onSurface,
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MiuixTheme.colorScheme.onBackground,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        AppFontContent(
            state = state,
            busy = busy,
            message = message,
            onSelectPreset = { preset ->
                if (preset == AppFontPreset.Custom && !state.customFileAvailable) {
                    fontPicker.launch(APP_FONT_MIME_TYPES)
                } else {
                    applyPreset(preset)
                }
            },
            onImport = { fontPicker.launch(APP_FONT_MIME_TYPES) },
            onRemove = { showRemoveConfirmation = true },
            onSelectSavedFont = ::selectSavedFont,
            onDeleteSavedFont = { pendingDeleteFont = it },
            onOpacityChange = { opacity ->
                state = runCatching { setAppFontOpacity(context, opacity) }
                    .getOrElse { state.copy(opacity = opacity.coerceIn(MIN_APP_FONT_OPACITY, MAX_APP_FONT_OPACITY)) }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!busy) showRemoveConfirmation = false },
            title = { Text(stringResource(R.string.app_font_remove_confirm_title)) },
            text = { Text(stringResource(R.string.app_font_remove_confirm_summary)) },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        showRemoveConfirmation = false
                        busy = true
                        message = null
                        scope.launch {
                            val removed = withContext(Dispatchers.IO) {
                                removeCustomAppFont(context)
                            }
                            refresh()
                            message = AppFontMessage(
                                text = if (removed) removeSuccessText else removeFailedText,
                                isError = !removed,
                            )
                            busy = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.app_font_remove_action))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { showRemoveConfirmation = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    pendingDeleteFont?.let { font ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingDeleteFont = null },
            title = { Text(stringResource(R.string.app_font_saved_delete_confirm_title)) },
            text = {
                Text(stringResource(R.string.app_font_saved_delete_confirm_summary, font.displayName))
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        pendingDeleteFont = null
                        busy = true
                        message = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                deleteSavedAppFont(context, font.id)
                            }
                            result.onSuccess { next ->
                                state = next
                                message = AppFontMessage(deleteSuccessText, isError = false)
                            }.onFailure { error ->
                                refresh()
                                message = AppFontMessage(
                                    text = error.appFontError(deleteFailedText),
                                    isError = true,
                                )
                            }
                            busy = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.app_font_saved_delete_action))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { pendingDeleteFont = null },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun AppFontContent(
    state: AppFontState,
    busy: Boolean,
    message: AppFontMessage?,
    onSelectPreset: (AppFontPreset) -> Unit,
    onImport: () -> Unit,
    onRemove: () -> Unit,
    onSelectSavedFont: (SavedAppFont) -> Unit,
    onDeleteSavedFont: (SavedAppFont) -> Unit,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TonalCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FontDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.app_font_current),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
                Text(
                    text = appFontStateName(state),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.app_font_preview_text),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.app_font_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        message?.let { currentMessage ->
            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = if (currentMessage.isError) {
                            Icons.Rounded.ErrorOutline
                        } else {
                            Icons.Rounded.CheckCircle
                        },
                        contentDescription = null,
                        tint = if (currentMessage.isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Text(
                        text = currentMessage.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (currentMessage.isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }

        TonalCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.LibraryBooks,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.app_font_saved_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.app_font_saved_count, state.savedFonts.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.savedFonts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.app_font_saved_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.savedFonts.forEach { font ->
                            SavedFontRow(
                                font = font,
                                selected = state.customId == font.id && state.isCustomActive,
                                busy = busy,
                                onSelect = { onSelectSavedFont(font) },
                                onDelete = { onDeleteSavedFont(font) },
                            )
                        }
                    }
                }
            }
        }

        TonalCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.app_font_opacity_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(state.opacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(R.string.app_font_opacity_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = state.opacity,
                    onValueChange = onOpacityChange,
                    valueRange = MIN_APP_FONT_OPACITY..MAX_APP_FONT_OPACITY,
                    enabled = !busy,
                )
            }
        }

        SegmentedColumn(
            title = stringResource(R.string.app_font_presets_title),
            content = AppFontPreset.entries.map { preset ->
                {
                    SegmentedRadioItem(
                        title = stringResource(preset.titleRes()),
                        summary = stringResource(preset.summaryRes()),
                        selected = state.preset == preset,
                        enabled = !busy,
                        onClick = { onSelectPreset(preset) },
                    )
                }
            },
        )

        TonalCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_font_custom_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.customFileAvailable) {
                    Text(
                        text = state.customDisplayName ?: stringResource(R.string.app_font_custom_unnamed),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(
                            R.string.app_font_file_size,
                            formatAppFontBytes(state.customSizeBytes),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.customSha256?.let { sha256 ->
                        Text(
                            text = stringResource(R.string.app_font_sha256, sha256),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.app_font_custom_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        onClick = onImport,
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (state.customFileAvailable) {
                                    R.string.app_font_replace_action
                                } else {
                                    R.string.app_font_import_action
                                }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (state.customFileAvailable) {
                        OutlinedButton(
                            enabled = !busy,
                            onClick = onRemove,
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.app_font_remove_action),
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.app_font_import_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SavedFontRow(
    font: SavedAppFont,
    selected: Boolean,
    busy: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.FontDownload,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = font.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.app_font_file_size,
                        formatAppFontBytes(font.sizeBytes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                enabled = !busy,
                onClick = onDelete,
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.app_font_saved_delete_action),
                )
            }
        }
    }
    if (selected || busy) {
        TonalCard(modifier = Modifier.fillMaxWidth(), content = content)
    } else {
        TonalCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSelect,
            content = content,
        )
    }
}

@Composable
internal fun appFontStateName(state: AppFontState): String {
    if (state.preset == AppFontPreset.Custom) {
        return if (state.customFileAvailable) {
            state.customDisplayName ?: stringResource(R.string.app_font_preset_custom)
        } else {
            stringResource(R.string.app_font_custom_missing)
        }
    }
    return stringResource(state.preset.titleRes())
}

@StringRes
internal fun AppFontPreset.titleRes(): Int = when (this) {
    AppFontPreset.System -> R.string.app_font_preset_system
    AppFontPreset.SansSerif -> R.string.app_font_preset_sans_serif
    AppFontPreset.Serif -> R.string.app_font_preset_serif
    AppFontPreset.Monospace -> R.string.app_font_preset_monospace
    AppFontPreset.Cursive -> R.string.app_font_preset_cursive
    AppFontPreset.Custom -> R.string.app_font_preset_custom
}

@StringRes
private fun AppFontPreset.summaryRes(): Int = when (this) {
    AppFontPreset.System -> R.string.app_font_preset_system_summary
    AppFontPreset.SansSerif -> R.string.app_font_preset_sans_serif_summary
    AppFontPreset.Serif -> R.string.app_font_preset_serif_summary
    AppFontPreset.Monospace -> R.string.app_font_preset_monospace_summary
    AppFontPreset.Cursive -> R.string.app_font_preset_cursive_summary
    AppFontPreset.Custom -> R.string.app_font_preset_custom_summary
}

private fun formatAppFontBytes(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L -> String.format(
            Locale.getDefault(),
            "%.2f MiB",
            bytes / (1024.0 * 1024.0),
        )
        bytes >= 1024L -> String.format(Locale.getDefault(), "%.1f KiB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun Throwable?.appFontError(fallback: String): String {
    val detail = this?.localizedMessage
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.take(180)
        ?.takeIf(String::isNotBlank)
    return if (detail == null) fallback else "$fallback: $detail"
}

private data class AppFontMessage(
    val text: String,
    val isError: Boolean,
)
