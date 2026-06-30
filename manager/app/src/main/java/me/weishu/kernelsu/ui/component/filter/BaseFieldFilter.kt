package me.weishu.kernelsu.ui.component.filter

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

abstract class BaseFieldFilter() {
    private var inputValue = mutableStateOf(TextFieldValue())

    constructor(value: String) : this() {
        inputValue.value = TextFieldValue(value, TextRange(value.lastIndex + 1))
    }

    protected abstract fun onFilter(
        inputTextFieldValue: TextFieldValue,
        lastTextFieldValue: TextFieldValue,
    ): TextFieldValue

    fun setInputValue(value: String) {
        inputValue.value = TextFieldValue(value, TextRange(value.lastIndex + 1))
    }

    fun getInputValue(): TextFieldValue {
        return inputValue.value
    }

    fun onValueChange(): (TextFieldValue) -> Unit {
        return {
            inputValue.value = onFilter(it, inputValue.value)
        }
    }
}
