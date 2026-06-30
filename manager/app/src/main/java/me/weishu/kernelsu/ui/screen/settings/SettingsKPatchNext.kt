package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.R

internal val SettingsUiState.canToggleKPatchNext: Boolean
    get() = kPatchNextConflict == null || isKPatchNextEnabled

internal val SettingsUiState.canOpenKPatchNextWebUi: Boolean
    get() = isKPatchNextEnabled && isKPatchNextWebUiAvailable

@Composable
internal fun kPatchNextSummary(uiState: SettingsUiState): String {
    return when {
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
