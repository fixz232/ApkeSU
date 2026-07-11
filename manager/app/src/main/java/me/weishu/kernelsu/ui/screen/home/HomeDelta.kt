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
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.delta.DeltaCard
import me.weishu.kernelsu.ui.component.delta.DeltaColors
import me.weishu.kernelsu.ui.component.delta.DeltaPillButton
import me.weishu.kernelsu.ui.component.delta.DeltaScreen
import me.weishu.kernelsu.ui.component.delta.DeltaSectionTitle
import me.weishu.kernelsu.ui.component.delta.deltaSp

@Composable
fun HomePagerDelta(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val warnings = homeWarningMessages(state)
    DeltaScreen(
        title = stringResource(R.string.home),
        icon = Icons.Rounded.Home,
        bottomInnerPadding = bottomInnerPadding,
    ) { contentPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 18.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DeltaStatusCard(state = state, actions = actions)
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    DeltaMetricTile(
                        label = stringResource(R.string.superuser),
                        value = state.superuserCount.toString(),
                        icon = Icons.Rounded.Security,
                        onClick = actions.onSuperuserClick,
                        modifier = Modifier.weight(1f),
                    )
                    DeltaMetricTile(
                        label = stringResource(R.string.module),
                        value = state.moduleCount.toString(),
                        icon = Icons.Rounded.Extension,
                        onClick = actions.onModuleClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                DeltaManagerCard(state = state)
            }
            if (warnings.isNotEmpty()) {
                item {
                    DeltaWarningsCard(warnings = warnings)
                }
            }
            if (state.showHomeSupportCard) {
                item {
                    DeltaSupportCard(actions = actions)
                }
            }
        }
    }
}

@Composable
private fun DeltaStatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    DeltaCard(contentPadding = PaddingValues(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = DeltaColors.Ink,
                    fontSize = deltaSp(28f, maxScale = 1.0f),
                    lineHeight = deltaSp(32f, maxScale = 1.0f),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (state.isKernelActive) {
                        "${stringResource(state.rootRuntimeState.labelRes)} · v${state.ksuVersionLabel}"
                    } else {
                        stringResource(R.string.home_not_installed)
                    },
                    color = DeltaColors.Muted,
                    fontSize = deltaSp(17f, maxScale = 1.0f),
                    lineHeight = deltaSp(21f, maxScale = 1.0f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeltaPillButton(
                        text = if (!state.isKernelActive) {
                            stringResource(R.string.module_install)
                        } else {
                            stringResource(R.string.alpha_current)
                        },
                        icon = Icons.Rounded.InstallMobile,
                        onClick = actions.onInstallClick,
                        background = DeltaColors.AccentSoft,
                    )
                    DeltaPillButton(
                        text = stringResource(R.string.root_diagnose),
                        icon = Icons.Rounded.Info,
                        onClick = actions.onDiagnoseClick,
                        background = DeltaColors.SurfaceDeep,
                    )
                    if (!state.isKernelActive) {
                        DeltaPillButton(
                            text = stringResource(R.string.home_jailbreak),
                            icon = Icons.Rounded.WarningAmber,
                            onClick = actions.onJailbreakClick,
                            background = DeltaColors.Danger.copy(alpha = 0.14f),
                            foreground = DeltaColors.Danger,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(DeltaColors.AccentSoft.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Android,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }
}

@Composable
private fun DeltaMetricTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DeltaCard(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = DeltaColors.Muted,
                    fontSize = deltaSp(14f, maxScale = 1.0f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    color = DeltaColors.Accent,
                    fontSize = deltaSp(25f, maxScale = 1.0f),
                    lineHeight = deltaSp(29f, maxScale = 1.0f),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeltaColors.Accent,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun DeltaManagerCard(state: HomeUiState) {
    DeltaCard(contentPadding = PaddingValues(17.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = DeltaColors.Accent,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = "App",
                    color = DeltaColors.Ink,
                    fontSize = deltaSp(20f, maxScale = 1.0f),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            DeltaInfoLine(
                label = stringResource(R.string.alpha_current),
                value = state.systemInfo.managerVersion,
            )
            DeltaInfoLine(
                label = stringResource(R.string.home_kernel),
                value = state.systemInfo.kernelVersion,
            )
            DeltaInfoLine(
                label = stringResource(R.string.home_device_model),
                value = state.systemInfo.deviceModel,
            )
        }
    }
}

@Composable
private fun DeltaWarningsCard(warnings: List<String>) {
    DeltaCard(contentPadding = PaddingValues(17.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = DeltaColors.Danger,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.home_warning_title),
                    color = DeltaColors.Ink,
                    fontSize = deltaSp(17f, maxScale = 1.0f),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            warnings.take(3).forEach { warning ->
                Text(
                    text = warning,
                    color = DeltaColors.Muted,
                    fontSize = deltaSp(13f),
                    lineHeight = deltaSp(17f),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun DeltaSupportCard(actions: HomeActions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DeltaSectionTitle(text = stringResource(R.string.home_support_title))
        DeltaCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.home_support_content),
                    color = DeltaColors.Muted,
                    fontSize = deltaSp(13f),
                    lineHeight = deltaSp(17f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DeltaPillButton(
                        text = "GitHub",
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = { actions.onOpenUrl("https://github.com/fixz232/ApkeSU.git") },
                        modifier = Modifier.weight(1f),
                    )
                    DeltaPillButton(
                        text = "Sponsor",
                        icon = Icons.Rounded.FavoriteBorder,
                        onClick = { actions.onOpenUrl("https://github.com/fixz232/ApkeSU.git") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeltaInfoLine(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = DeltaColors.Muted,
            fontSize = deltaSp(13f),
            lineHeight = deltaSp(17f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(76.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = DeltaColors.Ink,
            fontSize = deltaSp(13f),
            lineHeight = deltaSp(17f),
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
