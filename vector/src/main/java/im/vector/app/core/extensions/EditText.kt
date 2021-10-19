/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import im.vector.app.R
import im.vector.app.core.platform.SimpleTextWatcher

fun EditText.setupAsSearch(@DrawableRes searchIconRes: Int = R.drawable.ic_search,
                           @DrawableRes clearIconRes: Int = R.drawable.ic_x_gray) {
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
