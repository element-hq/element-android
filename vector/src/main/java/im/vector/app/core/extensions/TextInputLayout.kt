/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.os.Build
import android.text.Editable
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.autofill.HintConstants
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.core.platform.SimpleTextWatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import reactivecircus.flowbinding.android.widget.textChanges

fun TextInputLayout.editText() = this.editText!!

/**
 * Detect if a field starts or ends with spaces.
 */
fun TextInputLayout.hasSurroundingSpaces() = editText().text.toString().let { it.trim() != it }

fun TextInputLayout.hasContentFlow(mapper: (CharSequence) -> CharSequence = { it }) = editText().textChanges().map { mapper(it).isNotEmpty() }

fun TextInputLayout.content() = editText().text.toString()

fun TextInputLayout.hasContent() = !editText().text.isNullOrEmpty()

fun TextInputLayout.clearErrorOnChange(lifecycleOwner: LifecycleOwner) {
    onTextChange(lifecycleOwner) {
        error = null
        isErrorEnabled = false
    }
}

fun TextInputLayout.onTextChange(lifecycleOwner: LifecycleOwner, action: (CharSequence) -> Unit) {
    editText().textChanges()
            .onEach(action)
            .launchIn(lifecycleOwner.lifecycleScope)
}

fun TextInputLayout.associateContentStateWith(button: View, enabledPredicate: (String) -> Boolean = { it.isNotEmpty() }) {
    button.isEnabled = enabledPredicate(content())
    editText().addTextChangedListener(object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            val newContent = s.toString()
            button.isEnabled = enabledPredicate(newContent)
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

/**
 * Set a listener for when the input has lost focus, such as moving to the another input field.
 * The listener is only called when the view is in a resumed state to avoid triggers when exiting a screen.
 */
fun TextInputLayout.setOnFocusLostListener(lifecycleOwner: LifecycleOwner, action: () -> Unit) {
    editText().setOnFocusChangeListener { _, hasFocus ->
        when (hasFocus) {
            false -> lifecycleOwner.lifecycleScope.launch { lifecycleOwner.withResumed { action() } }
            else -> {
                // do nothing
            }
        }
    }
}

fun TextInputLayout.autofillPhoneNumber() = setAutofillHint(HintConstants.AUTOFILL_HINT_PHONE_NUMBER)
fun TextInputLayout.autofillEmail() = setAutofillHint(HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS)

private fun TextInputLayout.setAutofillHint(hintType: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setAutofillHints(hintType)
    }
}
