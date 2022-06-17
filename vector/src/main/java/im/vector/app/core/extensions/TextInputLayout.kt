/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.extensions

import android.text.Editable
import android.view.View
import android.view.inputmethod.EditorInfo
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.core.platform.SimpleTextWatcher
import kotlinx.coroutines.flow.map
import reactivecircus.flowbinding.android.widget.textChanges

fun TextInputLayout.editText() = this.editText!!

/**
 * Detect if a field starts or ends with spaces.
 */
fun TextInputLayout.hasSurroundingSpaces() = editText().text.toString().let { it.trim() != it }

fun TextInputLayout.hasContentFlow(mapper: (CharSequence) -> CharSequence = { it }) = editText().textChanges().map { mapper(it).isNotEmpty() }

fun TextInputLayout.content() = editText().text.toString()

fun TextInputLayout.hasContent() = !editText().text.isNullOrEmpty()

fun TextInputLayout.associateContentStateWith(button: View) {
    editText().addTextChangedListener(object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            val newContent = s.toString()
            button.isEnabled = newContent.isNotEmpty()
        }
    })
}

fun TextInputLayout.setOnImeDoneListener(action: () -> Unit) {
    editText().setOnEditorActionListener { _, actionId, _ ->
        when (actionId) {
            EditorInfo.IME_ACTION_DONE -> {
                action()
                true
            }
            else -> false
        }
    }
}

fun TextInputLayout.setOnFocusLostListener(action: () -> Unit) {
    editText().setOnFocusChangeListener { _, hasFocus ->
        when (hasFocus) {
            false -> action()
            else -> {
                // do nothing
            }
        }
    }
}
