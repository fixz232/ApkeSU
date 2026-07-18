package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.R

internal enum class PathConfigBackend {
    PathmaskLkm,
    SusfsGki,
    Disabled,
    Unknown,
}

internal fun resolvePathConfigBackend(
    isLkmMode: Boolean,
    isLateLoadMode: Boolean,
    runtimeModeResolved: Boolean,
): PathConfigBackend = when {
    !runtimeModeResolved -> PathConfigBackend.Unknown
    isLateLoadMode -> PathConfigBackend.Disabled
    isLkmMode -> PathConfigBackend.PathmaskLkm
    else -> PathConfigBackend.SusfsGki
}

internal val SettingsUiState.pathConfigBackend: PathConfigBackend
    get() = resolvePathConfigBackend(isLkmMode, isLateLoadMode, runtimeModeResolved)

internal val SettingsUiState.isGkiMode: Boolean
    get() = pathConfigBackend == PathConfigBackend.SusfsGki

internal val SettingsUiState.canOpenPathConfig: Boolean
    get() = pathConfigBackend == PathConfigBackend.PathmaskLkm || pathConfigBackend == PathConfigBackend.SusfsGki

internal val SettingsUiState.canToggleKPatchNext: Boolean
    get() = runtimeModeResolved && !isLateLoadMode && (kPatchNextConflict == null || isKPatchNextEnabled)

internal val SettingsUiState.canOpenKPatchNextWebUi: Boolean
    get() = runtimeModeResolved && !isLateLoadMode && isKPatchNextEnabled && isKPatchNextWebUiAvailable

@Composable
internal fun pathConfigTitle(uiState: SettingsUiState): String = stringResource(
    when (uiState.pathConfigBackend) {
        PathConfigBackend.SusfsGki -> R.string.settings_susfs_path_config
        PathConfigBackend.PathmaskLkm,
        PathConfigBackend.Disabled,
        PathConfigBackend.Unknown,
        -> R.string.hidden_path_config
    }
)

@Composable
internal fun pathConfigSummary(uiState: SettingsUiState): String = stringResource(
    when (uiState.pathConfigBackend) {
        PathConfigBackend.PathmaskLkm -> R.string.hidden_path_config_summary
        PathConfigBackend.SusfsGki -> R.string.settings_susfs_path_config_summary
        PathConfigBackend.Disabled -> R.string.settings_hidden_path_jailbreak_disabled_summary
        PathConfigBackend.Unknown -> R.string.settings_runtime_mode_detecting
    }
)

@Composable
internal fun kPatchNextSummary(uiState: SettingsUiState): String {
    return when {
        !uiState.runtimeModeResolved -> stringResource(R.string.settings_runtime_mode_detecting)
        uiState.isLateLoadMode -> stringResource(R.string.settings_kpatch_next_jailbreak_disabled_summary)
        uiState.kPatchNextConflict != null -> stringResource(
            R.string.settings_kpatch_next_conflict_summary,
            uiState.kPatchNextConflict
        )
        uiState.isKPatchNextUnresolved -> stringResource(R.string.settings_kpatch_next_unresolved_summary)
        uiState.isKPatchNextPendingRemove -> stringResource(R.string.settings_kpatch_next_pending_remove_summary)
        uiState.isKPatchNextPendingUpdate -> stringResource(R.string.settings_kpatch_next_pending_update_summary)
        uiState.isKPatchNextInstalled && uiState.kPatchNextVersion.isNotBlank() -> stringResource(
            R.string.settings_kpatch_next_installed_summary,
            uiState.kPatchNextVersion
        )
        else -> stringResource(R.string.settings_kpatch_next_summary)
    }
}
