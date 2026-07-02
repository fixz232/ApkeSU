package me.weishu.kernelsu.ui.screen.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.magica.MagicaService
import me.weishu.kernelsu.ui.InterfaceStyle
import me.weishu.kernelsu.ui.LocalInterfaceStyle
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.dialog.rememberLoadingDialog
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.KernelStatusEvents
import me.weishu.kernelsu.ui.viewmodel.HomeViewModel

@Composable
fun HomePager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true
) {
    val viewModel = viewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mainState = LocalMainPagerState.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val loadingDialog = rememberLoadingDialog()
    val scope = rememberCoroutineScope()
    var installFeedbackActive by remember { mutableStateOf(false) }
    var jailbreakInProgress by remember { mutableStateOf(false) }
    val refreshTick by KernelStatusEvents.refreshTick.collectAsStateWithLifecycle()

    var hasActivated by remember { mutableStateOf(false) }
    if (isCurrentPage) hasActivated = true

    if (hasActivated) {
        LaunchedEffect(Unit) {
            viewModel.refresh()
        }
    }

    LifecycleResumeEffect(Unit) {
        if (hasActivated) {
            viewModel.refresh()
        }
        onPauseOrDispose {}
    }

    LaunchedEffect(refreshTick) {
        if (hasActivated) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.ksuVersion) {
        if (uiState.ksuVersion != null) {
            installFeedbackActive = false
        }
    }

    LaunchedEffect(uiState.isLateLoadMode, uiState.ksuVersion) {
        if (jailbreakInProgress && (uiState.isLateLoadMode || uiState.ksuVersion != null)) {
            jailbreakInProgress = false
            loadingDialog.hide()
        }
    }

    val showInlineInstallFeedback = uiState.ksuVersion == null && uiState.kernelVersion.isGKI()
    val actions = HomeActions(
        onInstallClick = {
            if (showInlineInstallFeedback) {
                if (!installFeedbackActive) {
                    installFeedbackActive = true
                    scope.launch {
                        delay(650)
                        navigator.push(Route.Install)
                        installFeedbackActive = false
                    }
                }
            } else {
                navigator.push(Route.Install)
            }
        },
        onSuperuserClick = { if (!uiState.showRequireKernelWarning) mainState.animateToPage(1) },
        onModuleClick = { if (!uiState.showRequireKernelWarning) mainState.animateToPage(2) },
        onOpenUrl = uriHandler::openUri,
        onJailbreakClick = {
            if (jailbreakInProgress) return@HomeActions
            if (uiState.isLateLoadMode) {
                KernelStatusEvents.requestRefresh()
                return@HomeActions
            }
            loadingDialog.showLoading()
            jailbreakInProgress = true
            val started = runCatching {
                context.startService(Intent(context, MagicaService::class.java))
            }
            if (started.isFailure) {
                jailbreakInProgress = false
                loadingDialog.hide()
                Toast.makeText(context, R.string.jailbreak_timeout, Toast.LENGTH_LONG).show()
                KernelStatusEvents.requestRefresh()
                return@HomeActions
            }
            // Manager will be force-stopped and restarted by late-load on success.
            // If that doesn't happen within timeout, jailbreak likely failed.
            scope.launch {
                delay(30_000)
                if (jailbreakInProgress) {
                    jailbreakInProgress = false
                    loadingDialog.hide()
                    Toast.makeText(context, R.string.jailbreak_timeout, Toast.LENGTH_LONG).show()
                    KernelStatusEvents.requestRefresh()
                }
            }
        },
    )

    when (LocalInterfaceStyle.current) {
        InterfaceStyle.Skrootpro.value -> {
            HomePagerSkrootpro(
                state = uiState,
                actions = actions,
                bottomInnerPadding = bottomInnerPadding,
            )
            return
        }

        InterfaceStyle.Delta.value -> {
            HomePagerDelta(
                state = uiState,
                actions = actions,
                bottomInnerPadding = bottomInnerPadding,
            )
            return
        }

        InterfaceStyle.Alpha.value -> {
            HomePagerAlpha(
                state = uiState,
                actions = actions,
                bottomInnerPadding = bottomInnerPadding,
            )
            return
        }
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> HomePagerMiuix(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
            installFeedbackActive = installFeedbackActive && showInlineInstallFeedback,
        )

        UiMode.Material -> HomePagerMaterial(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
            installFeedbackActive = installFeedbackActive && showInlineInstallFeedback,
        )
    }
}
