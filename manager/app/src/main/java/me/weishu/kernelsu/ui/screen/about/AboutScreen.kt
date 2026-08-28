package me.weishu.kernelsu.ui.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.component.ApkeGroupedList
import me.weishu.kernelsu.ui.component.ApkeListDivider
import me.weishu.kernelsu.ui.component.ApkeSecondaryScaffold
import me.weishu.kernelsu.ui.component.ApkeUiTokens

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

    AboutScreenCompact(state, actions)
}

@Composable
private fun AboutScreenCompact(
    state: AboutUiState,
    actions: AboutScreenActions,
) {
    ApkeSecondaryScaffold(
        title = state.title,
        onBack = actions.onBack,
        maxContentWidth = 680.dp,
    ) { innerPadding, _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = ApkeUiTokens.PageHorizontalPadding,
                top = innerPadding.calculateTopPadding() + 12.dp,
                end = ApkeUiTokens.PageHorizontalPadding,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                RowAboutIdentity(state)
            }
            item {
                ApkeGroupedList {
                    state.links.forEachIndexed { index, link ->
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { actions.onOpenLink(link.url) },
                            headlineContent = { Text(link.fullText) },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                )
                            },
                        )
                        if (index < state.links.lastIndex) ApkeListDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun RowAboutIdentity(state: AboutUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(ApkeUiTokens.ToolCardRadius),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                AboutAppIcon(
                    size = 64.dp,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = null,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = state.appName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.versionName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val QQ_GROUP_URL = "https://qm.qq.com/q/Q8PmBoq9iK"
private const val OFFICIAL_TELEGRAM_URL = "https://t.me/ApkeSu"
private const val OFFICIAL_UPSTREAM_URL = "https://github.com/tiann/KernelSU"
