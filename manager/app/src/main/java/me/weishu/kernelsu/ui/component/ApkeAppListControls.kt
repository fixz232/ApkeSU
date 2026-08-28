package me.weishu.kernelsu.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R

enum class ApkeAppSort(@StringRes val labelRes: Int) {
    Name(R.string.sort_by_name),
    PackageName(R.string.sort_by_package_name),
    UserId(R.string.app_list_sort_user_id),
}

@Composable
fun ApkeAppSortMenu(
    selected: ApkeAppSort,
    onSelected: (ApkeAppSort) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(
        onClick = { expanded = true },
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Sort,
            contentDescription = stringResource(R.string.menu_sort),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ApkeAppSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    leadingIcon = if (option == selected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
fun ApkeSelectionToolbar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(ApkeUiTokens.ToolCardRadius),
    ) {
        BoxWithConstraints {
            val compact = maxWidth < 440.dp
            val selectionSummary: @Composable RowScope.() -> Unit = {
                Text(
                    text = stringResource(R.string.app_list_selected_count, selectedCount),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
            }
            val selectionToggle: @Composable () -> Unit = {
                androidx.compose.material3.TextButton(
                    onClick = if (selectedCount == totalCount) onClear else onSelectAll,
                    modifier = Modifier.heightIn(min = ApkeUiTokens.MinTouchTarget),
                ) {
                    Text(
                        stringResource(
                            if (selectedCount == totalCount) {
                                R.string.app_list_clear_selection
                            } else {
                                R.string.app_list_select_all
                            },
                        ),
                    )
                }
            }
            if (compact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        selectionSummary()
                        selectionToggle()
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = androidx.compose.ui.Alignment.CenterEnd,
                    ) {
                        actions()
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    selectionSummary()
                    selectionToggle()
                    actions()
                }
            }
        }
    }
}
