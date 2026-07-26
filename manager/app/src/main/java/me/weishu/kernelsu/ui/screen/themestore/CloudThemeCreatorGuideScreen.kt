package me.weishu.kernelsu.ui.screen.themestore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.navigation3.LocalNavigator

@Composable
fun CloudThemeCreatorGuideScreen() {
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cloud_theme_creator_guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GuideIcon(Icons.AutoMirrored.Rounded.HelpOutline)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.cloud_theme_creator_guide_intro_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.cloud_theme_creator_guide_intro),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            item {
                GuideStep(
                    number = 1,
                    icon = Icons.Rounded.Inventory2,
                    title = stringResource(R.string.cloud_theme_creator_guide_step_package),
                    body = stringResource(R.string.cloud_theme_creator_guide_step_package_body),
                )
            }
            item {
                GuideStep(
                    number = 2,
                    icon = Icons.Rounded.CloudUpload,
                    title = stringResource(R.string.cloud_theme_creator_guide_step_release),
                    body = stringResource(R.string.cloud_theme_creator_guide_step_release_body),
                )
                GuideCode(stringResource(R.string.cloud_theme_creator_guide_release_example))
            }
            item {
                GuideStep(
                    number = 3,
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.cloud_theme_creator_guide_step_fields),
                    body = stringResource(R.string.cloud_theme_creator_guide_step_fields_body),
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        GuideField(
                            stringResource(R.string.cloud_theme_creator_theme_id),
                            stringResource(R.string.cloud_theme_creator_guide_example_theme_id),
                        )
                        GuideField(
                            stringResource(R.string.cloud_theme_creator_theme_name),
                            stringResource(R.string.cloud_theme_creator_guide_example_theme_name),
                        )
                        GuideField(
                            stringResource(R.string.cloud_theme_creator_description),
                            stringResource(R.string.cloud_theme_creator_guide_example_description),
                        )
                        GuideField(
                            stringResource(R.string.cloud_theme_creator_category_title),
                            stringResource(R.string.cloud_theme_creator_guide_example_category),
                        )
                        GuideField(
                            stringResource(R.string.cloud_theme_creator_tags),
                            stringResource(R.string.cloud_theme_creator_guide_example_tags),
                        )
                        GuideField(
                            stringResource(R.string.cloud_theme_creator_version_title),
                            stringResource(
                                R.string.cloud_theme_creator_guide_example_version,
                                BuildConfig.VERSION_CODE,
                            ),
                        )
                        GuideField(
                            stringResource(R.string.cloud_theme_creator_license),
                            "CC-BY-4.0",
                            showDivider = false,
                        )
                    }
                }
            }
            item {
                GuideStep(
                    number = 4,
                    icon = Icons.AutoMirrored.Rounded.FactCheck,
                    title = stringResource(R.string.cloud_theme_creator_guide_step_verify),
                    body = stringResource(R.string.cloud_theme_creator_guide_step_verify_body),
                )
            }
            item {
                GuideStep(
                    number = 5,
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.cloud_theme_creator_guide_step_submit),
                    body = stringResource(R.string.cloud_theme_creator_guide_step_submit_body),
                )
            }
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.cloud_theme_creator_guide_checks_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.cloud_theme_creator_guide_checks),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideStep(
    number: Int,
    icon: ImageVector,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GuideCode(value: String) {
    SelectionContainer {
        Text(
            text = value,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(6.dp),
                )
                .padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun GuideField(
    label: String,
    value: String,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(0.38f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectionContainer(modifier = Modifier.weight(0.62f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (showDivider) HorizontalDivider()
    }
}

@Composable
private fun GuideIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
