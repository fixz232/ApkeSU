package me.weishu.kernelsu.ui.component.choosekmidialog

import androidx.compose.runtime.Composable

@Composable
fun ChooseKmiDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onSelected: (String?) -> Unit
) {
    ChooseKmiDialogMiuix(show, onDismissRequest, onSelected)
}
