package me.weishu.kernelsu.ui.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val state = AboutUiState(
        title = stringResource(R.string.about),
        appName = stringResource(R.string.app_name),
        versionName = BuildConfig.VERSION_NAME,
        links = listOf(
            LinkInfo(
                fullText = stringResource(R.string.about_join_qq_group),
                url = QQ_GROUP_URL,
            ),
            LinkInfo(
                fullText = stringResource(R.string.about_official_telegram),
                url = OFFICIAL_TELEGRAM_URL,
            ),
            LinkInfo(
                fullText = stringResource(R.string.about_official_upstream),
                url = OFFICIAL_UPSTREAM_URL,
            ),
        ),
    )
    val actions = AboutScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onOpenLink = { url ->
            // A device may not have a handler for every external scheme. Keep the
            // About page usable instead of letting the URI handler exception crash it.
            runCatching { uriHandler.openUri(url) }
        },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutScreenMiuix(state, actions)
        UiMode.Material -> AboutScreenMaterial(state, actions)
    }
}

private const val QQ_GROUP_URL = "https://qm.qq.com/q/Q8PmBoq9iK"
private const val OFFICIAL_TELEGRAM_URL = "https://t.me/ApkeSu"
private const val OFFICIAL_UPSTREAM_URL = "https://github.com/tiann/KernelSU"
