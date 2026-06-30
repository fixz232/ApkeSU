package me.weishu.kernelsu.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.alpha.AlphaButton
import me.weishu.kernelsu.ui.component.alpha.AlphaCard
import me.weishu.kernelsu.ui.component.alpha.AlphaColors
import me.weishu.kernelsu.ui.component.alpha.AlphaOutlinedButton
import me.weishu.kernelsu.ui.component.alpha.AlphaScreen
import me.weishu.kernelsu.ui.component.alpha.alphaSp

@Composable
fun HomePagerAlpha(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val warnings = homeWarningMessages(state)
    AlphaScreen(
        title = stringResource(R.string.home),
        bottomInnerPadding = bottomInnerPadding,
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 18.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                AlphaStatusCard(state = state, actions = actions)
            }
            item {
                AlphaManagerCard(state = state)
            }
            if (warnings.isNotEmpty()) {
                item {
                    AlphaWarningsCard(warnings = warnings)
                }
            }
            if (state.showHomeSupportCard) {
                item {
                    AlphaSupportCard(actions = actions)
                }
            }
            item {
                AlphaFollowCard(actions = actions)
            }
        }
    }
}

@Composable
private fun AlphaStatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    AlphaCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Android,
                    contentDescription = null,
                    tint = AlphaColors.Accent,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    color = AlphaColors.Accent,
                    fontSize = alphaSp(22f, maxScale = 1.02f),
                    lineHeight = alphaSp(26f, maxScale = 1.02f),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                AlphaInstallAction(
                    installed = state.ksuVersion != null,
                    onInstallClick = actions.onInstallClick,
                    onJailbreakClick = actions.onJailbreakClick,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AlphaInfoLine(
                    label = stringResource(R.string.alpha_current),
                    value = state.ksuVersion?.let {
                        "${it}-${state.kernelUAPIVersion ?: 0}"
                    } ?: stringResource(R.string.home_not_installed),
                    strong = true,
                )
                AlphaInfoLine(
                    label = state.lkmModeLabel,
                    value = alphaYesNo(state.lkmMode == true),
                    strong = true,
                )
                AlphaInfoLine(
                    label = "Root",
                    value = alphaYesNo(state.isRootAvailable),
                    strong = true,
                )
            }
        }
    }
}

@Composable
private fun AlphaManagerCard(state: HomeUiState) {
    AlphaCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = AlphaColors.Accent,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = "App",
                    color = AlphaColors.Accent,
                    fontSize = alphaSp(22f, maxScale = 1.02f),
                    lineHeight = alphaSp(26f, maxScale = 1.02f),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AlphaInfoLine(
                    label = stringResource(R.string.alpha_current),
                    value = state.systemInfo.managerVersion,
                    strong = true,
                )
                AlphaInfoLine(
                    label = stringResource(R.string.home_kernel),
                    value = state.systemInfo.kernelVersion,
                    strong = false,
                )
                AlphaInfoLine(
                    label = stringResource(R.string.home_device_model),
                    value = state.systemInfo.deviceModel,
                    strong = false,
                )
            }
        }
    }
}

@Composable
private fun AlphaWarningsCard(warnings: List<String>) {
    AlphaCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = AlphaColors.Accent,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.home_warning_title),
                    color = AlphaColors.Text,
                    fontSize = alphaSp(18f, maxScale = 1.03f),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            warnings.take(3).forEach { warning ->
                Text(
                    text = warning,
                    color = AlphaColors.Muted,
                    fontSize = alphaSp(13f),
                    lineHeight = alphaSp(17f),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun AlphaSupportCard(actions: HomeActions) {
    AlphaCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.home_support_title),
                color = AlphaColors.Text,
                fontSize = alphaSp(20f, maxScale = 1.03f),
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.home_support_content),
                color = AlphaColors.Muted,
                fontSize = alphaSp(14f),
                lineHeight = alphaSp(18f),
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AlphaIconLink(
                    label = "GitHub",
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = { actions.onOpenUrl("https://github.com/fixz232/ApkeSU.git") },
                )
                AlphaIconLink(
                    label = "Sponsor",
                    icon = Icons.Rounded.FavoriteBorder,
                    onClick = { actions.onOpenUrl("https://github.com/fixz232/ApkeSU.git") },
                )
            }
        }
    }
}

@Composable
private fun AlphaFollowCard(actions: HomeActions) {
    AlphaCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.alpha_follow_us),
                color = AlphaColors.Text,
                fontSize = alphaSp(20f, maxScale = 1.03f),
                fontWeight = FontWeight.Black,
            )
            AlphaFollowRow(
                name = "@KernelSU",
                onClick = { actions.onOpenUrl("https://github.com/tiann/KernelSU") },
            )
            AlphaFollowRow(
                name = "@ApkeSU",
                onClick = { actions.onOpenUrl("https://github.com/fixz232/ApkeSU.git") },
            )
        }
    }
}

@Composable
private fun AlphaFollowRow(
    name: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            color = AlphaColors.Text,
            fontSize = alphaSp(14f),
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            tint = AlphaColors.Text,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AlphaIconLink(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(width = 96.dp, height = 52.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AlphaColors.Text,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            color = AlphaColors.Muted,
            fontSize = alphaSp(11f, maxScale = 1.0f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AlphaInstallAction(
    installed: Boolean,
    onInstallClick: () -> Unit,
    onJailbreakClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        AlphaOutlinedButton(
            text = stringResource(R.string.module_install),
            icon = Icons.Rounded.InstallMobile,
            onClick = onInstallClick,
        )
        if (!installed) {
            AlphaButton(
                text = stringResource(R.string.home_jailbreak),
                icon = Icons.Rounded.WarningAmber,
                onClick = onJailbreakClick,
            )
        }
    }
}

@Composable
private fun AlphaInfoLine(
    label: String,
    value: String,
    strong: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = AlphaColors.Muted,
            fontSize = alphaSp(13.5f),
            lineHeight = alphaSp(17f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(72.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = AlphaColors.Text,
            fontSize = alphaSp(13.5f),
            lineHeight = alphaSp(17f),
            fontWeight = if (strong) FontWeight.Black else FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun alphaYesNo(value: Boolean): String {
    return stringResource(if (value) R.string.alpha_yes else R.string.alpha_no)
}
