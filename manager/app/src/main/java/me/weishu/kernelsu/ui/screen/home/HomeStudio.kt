package me.weishu.kernelsu.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.rebootlistpopup.RebootListPopupMiuix
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private val StudioPanelShape = RoundedCornerShape(8.dp)
private val StudioActionShape = RoundedCornerShape(8.dp)
private val StudioBadgeShape = RoundedCornerShape(4.dp)
private const val PROJECT_URL = "https://github.com/fixz232/ApkeSU.git"

@Composable
fun HomePagerStudio(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
    installFeedbackActive: Boolean = false,
) {
    val warningMessages = homeWarningMessages(state)
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { StudioTopBar(onDiagnoseClick = actions.onDiagnoseClick) },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.statusBars)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StudioRuntimePanel(
                        state = state,
                        installFeedbackActive = installFeedbackActive,
                        onDiagnoseClick = actions.onDiagnoseClick,
                    )
                    StudioCommandDeck(state = state, actions = actions)
                    if (!state.isKernelActive) {
                        StudioJailbreakCommand(onClick = actions.onJailbreakClick)
                    }
                    if (warningMessages.isNotEmpty()) {
                        StudioAttentionList(messages = warningMessages)
                    }
                    StudioDeviceContext(state = state)
                    StudioProjectLinks(state = state, actions = actions)
                    Spacer(modifier = Modifier.height(bottomInnerPadding + 10.dp))
                }
            }
        }
    }
}

@Composable
private fun StudioTopBar(onDiagnoseClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(StudioActionShape)
                .background(colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                color = colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.interface_style_studio),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        IconButton(onClick = onDiagnoseClick) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = stringResource(R.string.root_diagnose),
                tint = colorScheme.onSurfaceVariantSummary,
            )
        }
        RebootListPopupMiuix(tint = colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun StudioRuntimePanel(
    state: HomeUiState,
    installFeedbackActive: Boolean,
    onDiagnoseClick: () -> Unit,
) {
    val running = state.rootRuntimeState == RootRuntimeState.Running
    val accentColor = if (running) colorScheme.primary else colorScheme.error
    val stateIcon = if (running) Icons.Rounded.CheckCircleOutline else Icons.Rounded.ErrorOutline
    val modeLabel = state.workingModeLabel ?: stringResource(R.string.home_not_installed)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StudioPanelShape)
            .background(colorScheme.surfaceContainer)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(accentColor, CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.studio_status_label),
                        color = colorScheme.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(state.rootRuntimeState.labelRes),
                    color = colorScheme.onSurface,
                    fontSize = 24.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = modeLabel,
                    color = colorScheme.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .clip(StudioBadgeShape)
                    .background(accentColor.copy(alpha = 0.12f))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.ksuVersionLabel,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        StudioDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StudioRuntimeFact(
                label = stringResource(R.string.home_working_version),
                value = state.ksuVersionLabel,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(colorScheme.outline.copy(alpha = 0.20f)),
            )
            StudioRuntimeFact(
                label = stringResource(R.string.home_kernel_hook),
                value = kernelHookTypeLabel(state.kernelHookTypes),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StudioActionShape)
                .clickable(role = Role.Button, onClick = onDiagnoseClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = stateIcon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.root_diagnose),
                color = colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(18.dp),
            )
        }

        if (installFeedbackActive) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = accentColor,
                trackColor = colorScheme.onSurface.copy(alpha = 0.08f),
            )
        }
    }
}

@Composable
private fun StudioRuntimeFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            color = colorScheme.onSurfaceVariantSummary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StudioCommandDeck(state: HomeUiState, actions: HomeActions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StudioSectionLabel(stringResource(R.string.studio_quick_actions))
        StudioInstallCommand(
            enabled = !state.isLateLoadMode,
            onClick = actions.onInstallClick,
        )
        StudioManagementCommands(state = state, actions = actions)
    }
}

@Composable
private fun StudioInstallCommand(enabled: Boolean, onClick: () -> Unit) {
    val containerColor = if (enabled) colorScheme.primary else colorScheme.surfaceContainer
    val contentColor = if (enabled) colorScheme.onPrimary else colorScheme.onSurface.copy(alpha = 0.42f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StudioPanelShape)
            .background(containerColor)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.InstallMobile,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.install),
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.studio_install_summary),
                color = contentColor.copy(alpha = 0.76f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun StudioManagementCommands(state: HomeUiState, actions: HomeActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StudioPanelShape)
            .background(colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp),
    ) {
        StudioManagementCommand(
            icon = Icons.Rounded.Security,
            title = stringResource(R.string.superuser),
            value = if (state.isFullFeatured) state.superuserCount.toString() else "--",
            enabled = state.isFullFeatured,
            onClick = actions.onSuperuserClick,
        )
        StudioDivider()
        StudioManagementCommand(
            icon = Icons.Rounded.Extension,
            title = stringResource(R.string.module),
            value = if (state.isFullFeatured) state.moduleCount.toString() else "--",
            enabled = state.isFullFeatured,
            onClick = actions.onModuleClick,
        )
    }
}

@Composable
private fun StudioManagementCommand(
    icon: ImageVector,
    title: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.42f)
    val iconColor = if (enabled) colorScheme.primary else contentColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StudioActionShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = if (enabled) colorScheme.primary else contentColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariantSummary.copy(alpha = if (enabled) 1f else 0.42f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun StudioJailbreakCommand(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StudioPanelShape)
            .background(colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = colorScheme.error,
            modifier = Modifier.size(21.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.home_jailbreak),
                color = colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.studio_jailbreak_summary),
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun StudioAttentionList(messages: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StudioPanelShape)
            .background(colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = stringResource(R.string.studio_attention),
                color = colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        messages.forEachIndexed { index, message ->
            if (index > 0) {
                StudioDivider()
            }
            Text(
                text = message,
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun StudioDeviceContext(state: HomeUiState) {
    val selinuxStatus = when (state.systemInfo.selinuxStatus) {
        "Enforcing" -> stringResource(R.string.selinux_status_enforcing)
        "Permissive" -> stringResource(R.string.selinux_status_permissive)
        "Disabled" -> stringResource(R.string.selinux_status_disabled)
        else -> stringResource(R.string.selinux_status_unknown)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StudioSectionLabel(stringResource(R.string.studio_system_snapshot))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(StudioPanelShape)
                .background(colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp),
        ) {
            StudioSystemRow(stringResource(R.string.home_device_model), state.systemInfo.deviceModel)
            StudioSystemRow(stringResource(R.string.home_kernel), state.systemInfo.kernelVersion)
            StudioSystemRow(
                stringResource(R.string.home_kernel_hook),
                kernelHookTypeLabel(state.kernelHookTypes),
            )
            StudioSystemRow(stringResource(R.string.home_selinux_status), selinuxStatus)
            StudioSystemRow(
                stringResource(R.string.home_manager_version),
                state.systemInfo.managerVersion,
                showDivider = false,
            )
        }
    }
}

@Composable
private fun StudioSystemRow(label: String, value: String, showDivider: Boolean = true) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.38f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.62f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDivider) {
            StudioDivider()
        }
    }
}

@Composable
private fun StudioProjectLinks(state: HomeUiState, actions: HomeActions) {
    if (!state.showHomeSupportCard && !state.showHomeLearnCard) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StudioPanelShape)
            .background(colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp),
    ) {
        if (state.showHomeLearnCard) {
            StudioProjectLink(
                icon = Icons.Rounded.Info,
                text = stringResource(R.string.home_learn_kernelsu),
                onClick = { actions.onOpenUrl(PROJECT_URL) },
            )
        }
        if (state.showHomeLearnCard && state.showHomeSupportCard) {
            StudioDivider()
        }
        if (state.showHomeSupportCard) {
            StudioProjectLink(
                icon = Icons.Rounded.Extension,
                text = stringResource(R.string.home_support_title),
                onClick = { actions.onOpenUrl(PROJECT_URL) },
            )
        }
    }
}

@Composable
private fun StudioProjectLink(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StudioActionShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorScheme.primary,
            modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(11.dp))
        Text(
            text = text,
            color = colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun StudioDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colorScheme.outline.copy(alpha = 0.18f)),
    )
}

@Composable
private fun StudioSectionLabel(text: String) {
    Text(
        text = text,
        color = colorScheme.onSurface,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
