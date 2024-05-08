package com.example.pronedvizapp.bisness

import android.text.Editable
import android.text.TextWatcher

class CurrencyTextWatcher : TextWatcher {

    private val sb = StringBuilder()
    private var ignore = false
    private val numPlace = 'X'

    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

    override fun afterTextChanged(editable: Editable) {
        if (!ignore) {
            removeFormat(editable.toString())
            applyFormat(sb.toString())
            ignore = true
            editable.replace(0, editable.length, sb.toString())
            ignore = false
        }
    }

    private fun removeFormat(text: String) {
        sb.setLength(0)
        for (i in 0 until text.length) {
            val c = text[i]
            if (isNumberChar(c)) {
                sb.append(c)
            }
        }
    }

    private fun applyFormat(text: String) {
        val template = getTemplate(text)
        sb.setLength(0)
        var i = 0
        var textIndex = 0
        while (i < template.length && textIndex < text.length) {
            if (template[i] == numPlace) {
                sb.append(text[textIndex])
                textIndex++
            } else {
                sb.append(template[i])
            }
            i++
        }
    }

    private fun isNumberChar(c: Char) = c in '0'..'9'

//    private fun getTemplate(text: String) = when (text.substring(0, Math.min(3, text.length))) {
//        "380" -> "+XXX (XXX) XX-XX-XX"
//        "7" -> "+X (XXX) XXX-XX-XX"
//        "49" -> "+XX-XX-XXX-XXXXX"
//        else -> "+XXX (XXX) XX-XX-XX"
//    }

    private fun getTemplate(text: String) = "+X (XXX) XXX-XX-XX"
}
