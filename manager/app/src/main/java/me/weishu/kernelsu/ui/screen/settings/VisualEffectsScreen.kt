package me.weishu.kernelsu.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.GlobalScrollEffect
import me.weishu.kernelsu.ui.component.GlobalSnowEffect
import me.weishu.kernelsu.ui.component.MAX_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.component.MIN_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY
import me.weishu.kernelsu.ui.component.NightBackgroundEffect
import me.weishu.kernelsu.ui.component.StyledSwitch
import me.weishu.kernelsu.ui.component.SwitchStyle
import me.weishu.kernelsu.ui.component.material.ExpressiveSwitch
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

@Composable
fun VisualEffectsScreen() {
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val actions = VisualEffectsActions(
        onSetSwitchStyleIndex = viewModel::setSwitchStyleIndex,
        onSetGlobalSnowEnabled = viewModel::setGlobalSnowEnabled,
        onSetGlobalSnowEffectIndex = viewModel::setGlobalSnowEffectIndex,
        onSetNightBackgroundEffectIndex = viewModel::setNightBackgroundEffectIndex,
        onSetNightBackgroundPassthrough = viewModel::setNightBackgroundPassthrough,
        onSetNightBackgroundPassthroughOpacity = viewModel::setNightBackgroundPassthroughOpacity,
        onSetGlobalScrollEffectEnabled = viewModel::setGlobalScrollEffectEnabled,
        onSetGlobalScrollEffectIndex = viewModel::setGlobalScrollEffectIndex,
    )

    VisualEffectsScreenMiuix(
        uiState = uiState,
        actions = actions,
        onBack = onBack,
    )
}

@Composable
private fun VisualEffectsScreenMiuix(
    uiState: SettingsUiState,
    actions: VisualEffectsActions,
    onBack: () -> Unit,
) {
    MiuixScaffold(
        containerColor = Color.Transparent,
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.settings_section_visual_effects),
                color = Color.Transparent,
                titleColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface,
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        VisualEffectsContent(
            uiState = uiState,
            actions = actions,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun VisualEffectsContent(
    uiState: SettingsUiState,
    actions: VisualEffectsActions,
    modifier: Modifier,
) {
    val darkMode = isInDarkTheme()
    val selectedNightEffect = NightBackgroundEffect.fromValue(uiState.nightBackgroundEffect)
    val nightEffectEnabled = darkMode && selectedNightEffect != NightBackgroundEffect.Off

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_visual_effects_summary),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        VisualEffectCard(
            title = stringResource(R.string.settings_switch_style),
            icon = Icons.Rounded.Palette,
        ) {
            SwitchStyleButtonGroup(
                selectedStyle = uiState.switchStyle,
                onStyleSelected = actions.onSetSwitchStyleIndex,
            )
        }

        VisualEffectCard(
            title = stringResource(R.string.settings_global_snow),
            icon = Icons.Rounded.Star,
            enabled = darkMode,
            disabledMessage = stringResource(R.string.settings_visual_effects_dark_mode_only),
        ) {
            VisualSwitchRow(
                title = stringResource(R.string.settings_global_snow),
                summary = stringResource(R.string.settings_global_snow_summary),
                checked = uiState.globalSnowEnabled && darkMode,
                onCheckedChange = actions.onSetGlobalSnowEnabled,
                enabled = darkMode,
            )
            VisualChoiceRow(
                title = stringResource(R.string.settings_global_snow_effect),
                summary = stringResource(R.string.settings_global_snow_effect_summary),
                items = GlobalSnowEffect.entries.map { stringResource(it.labelRes) },
                selectedIndex = GlobalSnowEffect.selectedIndex(uiState.globalSnowEffect),
                onItemSelected = actions.onSetGlobalSnowEffectIndex,
                enabled = darkMode,
            )
        }

        VisualEffectCard(
            title = stringResource(R.string.settings_night_background_effect),
            icon = Icons.Rounded.Visibility,
            enabled = darkMode,
            disabledMessage = stringResource(R.string.settings_visual_effects_dark_mode_only),
        ) {
            VisualChoiceRow(
                title = stringResource(R.string.settings_night_background_effect),
                summary = stringResource(R.string.settings_night_background_effect_summary),
                items = NightBackgroundEffect.entries.map { stringResource(it.labelRes) },
                selectedIndex = NightBackgroundEffect.selectedIndex(uiState.nightBackgroundEffect),
                onItemSelected = actions.onSetNightBackgroundEffectIndex,
                enabled = darkMode,
            )
            VisualSwitchRow(
                title = stringResource(R.string.settings_night_background_passthrough),
                summary = stringResource(R.string.settings_night_background_passthrough_summary),
                checked = uiState.nightBackgroundPassthrough && nightEffectEnabled,
                onCheckedChange = actions.onSetNightBackgroundPassthrough,
                enabled = nightEffectEnabled,
            )
            if (uiState.nightBackgroundPassthrough) {
                VisualSliderRow(
                    title = stringResource(R.string.settings_night_background_passthrough_opacity),
                    summary = stringResource(R.string.settings_night_background_passthrough_opacity_summary),
                    value = uiState.nightBackgroundPassthroughOpacity,
                    valueRange = MIN_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY..MAX_NIGHT_BACKGROUND_PASSTHROUGH_OPACITY,
                    valueLabel = { "${(it * 100).roundToInt()}%" },
                    onValueChange = actions.onSetNightBackgroundPassthroughOpacity,
                    enabled = nightEffectEnabled,
                )
            }
        }

        VisualEffectCard(
            title = stringResource(R.string.settings_scroll_animation),
            icon = Icons.Rounded.PlayCircle,
        ) {
            VisualSwitchRow(
                title = stringResource(R.string.settings_scroll_animation),
                summary = stringResource(R.string.settings_scroll_animation_summary),
                checked = uiState.globalScrollEffectEnabled,
                onCheckedChange = actions.onSetGlobalScrollEffectEnabled,
            )
            VisualChoiceRow(
                title = stringResource(R.string.settings_scroll_animation_effect),
                summary = stringResource(R.string.settings_scroll_animation_effect_summary),
                items = GlobalScrollEffect.entries.map { stringResource(it.labelRes) },
                selectedIndex = GlobalScrollEffect.selectedIndex(uiState.globalScrollEffect),
                onItemSelected = actions.onSetGlobalScrollEffectIndex,
            )
        }
    }
}

@Composable
private fun VisualEffectCard(
    title: String,
    icon: ImageVector,
    enabled: Boolean = true,
    disabledMessage: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.5f),
                )
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.62f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!enabled && disabledMessage != null) {
                Text(
                    text = disabledMessage,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            content()
        }
    }
}

@Composable
private fun SwitchStyleButtonGroup(
    selectedStyle: String,
    onStyleSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(R.string.settings_switch_style), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.settings_switch_style_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SwitchStyle.entries.forEachIndexed { index, style ->
            SwitchStyleButton(
                style = style,
                selected = SwitchStyle.fromValue(selectedStyle) == style,
                onClick = { onStyleSelected(index) },
            )
        }
    }
}

@Composable
private fun SwitchStyleButton(
    style: SwitchStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(style.labelRes),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) {
                    Text(
                        text = stringResource(R.string.alpha_current),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            SwitchStylePreview(style = style)
        }
    }
}

@Composable
private fun SwitchStylePreview(style: SwitchStyle) {
    if (style == SwitchStyle.Original) {
        Switch(
            checked = true,
            onCheckedChange = null,
        )
    } else {
        StyledSwitch(
            checked = true,
            onCheckedChange = null,
            style = style,
        )
    }
}

@Composable
private fun VisualSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        ExpressiveSwitch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            showThumbIcon = false,
        )
    }
}

@Composable
private fun VisualChoiceRow(
    title: String,
    summary: String,
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        if (!enabled) expanded = false
    }
    val selectedText = items.getOrElse(selectedIndex) { items.firstOrNull().orEmpty() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
            ) {
                Text(
                    text = selectedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            onItemSelected(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VisualSliderRow(
    title: String,
    summary: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = valueLabel(value),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Slider(
            value = value,
            valueRange = valueRange,
            onValueChange = onValueChange,
            enabled = enabled,
        )
    }
}

private data class VisualEffectsActions(
    val onSetSwitchStyleIndex: (Int) -> Unit,
    val onSetGlobalSnowEnabled: (Boolean) -> Unit,
    val onSetGlobalSnowEffectIndex: (Int) -> Unit,
    val onSetNightBackgroundEffectIndex: (Int) -> Unit,
    val onSetNightBackgroundPassthrough: (Boolean) -> Unit,
    val onSetNightBackgroundPassthroughOpacity: (Float) -> Unit,
    val onSetGlobalScrollEffectEnabled: (Boolean) -> Unit,
    val onSetGlobalScrollEffectIndex: (Int) -> Unit,
)
