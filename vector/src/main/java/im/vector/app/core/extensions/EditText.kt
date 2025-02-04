/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.text.Editable
import android.text.InputType
import android.text.Spanned
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.DrawableRes
import im.vector.app.core.platform.SimpleTextWatcher

fun EditText.setupAsSearch(
        @DrawableRes searchIconRes: Int = im.vector.lib.ui.styles.R.drawable.ic_home_search,
        @DrawableRes clearIconRes: Int = im.vector.lib.ui.styles.R.drawable.ic_x_gray
) {
    addTextChangedListener(object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            val clearIcon = if (s.isNotEmpty()) clearIconRes else 0
            setCompoundDrawablesWithIntrinsicBounds(searchIconRes, 0, clearIcon, 0)
        }
    })

    maxLines = 1
    inputType = InputType.TYPE_CLASS_TEXT
    imeOptions = EditorInfo.IME_ACTION_SEARCH
    setOnEditorActionListener { _, actionId, _ ->
        var consumed = false
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            hideKeyboard()
            consumed = true
        }
        consumed
    }

    setOnTouchListener(View.OnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            if (event.rawX >= (this.right - this.compoundPaddingRight)) {
                text = null
                return@OnTouchListener true
            }
        }
        return@OnTouchListener false
    })
}

fun EditText.setTextIfDifferent(newText: CharSequence?): Boolean {
    if (!isTextDifferent(newText, text)) {
        // Previous text is the same. No op
        return false
    }
    setText(newText)
    // Since the text changed we move the cursor to the end of the new text.
    // This allows us to fill in text programmatically with a different value,
    // but if the user is typing and the view is rebound we won't lose their cursor position.
    setSelection(newText?.length ?: 0)
    return true
}

private fun isTextDifferent(str1: CharSequence?, str2: CharSequence?): Boolean {
    if (str1 === str2) {
        return false
    }
    if (str1 == null || str2 == null) {
        return true
    }
    val length = str1.length
    if (length != str2.length) {
        return true
    }
    if (str1 is Spanned) {
        return str1 != str2
    }
    for (i in 0 until length) {
        if (str1[i] != str2[i]) {
            return true
        }
    }
    return false
}
