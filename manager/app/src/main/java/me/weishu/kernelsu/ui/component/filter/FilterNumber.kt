package me.weishu.kernelsu.ui.component.filter

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

class FilterNumber(
    value: Int,
    private val minValue: Int = Int.MIN_VALUE,
    private val maxValue: Int = Int.MAX_VALUE,
) : BaseFieldFilter(value.toString()) {

    override fun onFilter(
        inputTextFieldValue: TextFieldValue,
        lastTextFieldValue: TextFieldValue
    ): TextFieldValue {
        return filterInputNumber(inputTextFieldValue, lastTextFieldValue, minValue, maxValue)
    }

    private fun filterInputNumber(
        inputTextFieldValue: TextFieldValue,
        lastInputTextFieldValue: TextFieldValue,
        minValue: Int,
        maxValue: Int,
    ): TextFieldValue {
        val inputString = inputTextFieldValue.text
        val newString = StringBuilder()
        val supportNegative = minValue < 0
        var isNegative = false

        if (supportNegative && inputString.isNotEmpty() && inputString.first() == '-') {
            isNegative = true
            newString.append('-')
        }

        for ((i, c) in inputString.withIndex()) {
            if (i == 0 && isNegative) continue
            when (c) {
                in '0'..'9' -> {
                    newString.append(c)
                    val tempText = newString.toString()
                    if (tempText != "-" && tempText.isNotEmpty()) {
                        try {
                            val tempValue = tempText.toInt()
                            if (tempValue !in minValue..maxValue) {
                                newString.deleteCharAt(newString.lastIndex)
                            }
                        } catch (e: NumberFormatException) {
                            newString.deleteCharAt(newString.lastIndex)
                        }
                    }
                }
            }
        }

        val cursor = if (inputTextFieldValue.selection.collapsed) {
            if (inputTextFieldValue.selection.end != inputString.length) {
                inputTextFieldValue.selection.end + (newString.length - inputString.length)
            } else {
                newString.length
            }
        } else {
            newString.length
        }
        val textRange = TextRange(cursor.coerceIn(0, newString.length))

        return lastInputTextFieldValue.copy(
            text = newString.toString(),
            selection = textRange
        )
    }
}
