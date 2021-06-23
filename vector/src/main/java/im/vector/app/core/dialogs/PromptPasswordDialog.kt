/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.core.dialogs

import android.app.Activity
import android.content.DialogInterface
import android.text.Editable
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.platform.SimpleTextWatcher
import im.vector.app.databinding.DialogPromptPasswordBinding

class PromptPasswordDialog {

    private var passwordVisible = false

    fun show(activity: Activity, listener: (String) -> Unit) {
        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_prompt_password, null)
        val views = DialogPromptPasswordBinding.bind(dialogLayout)
        val textWatcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                views.promptPasswordTil.error = null
            }
        }
        views.promptPassword.addTextChangedListener(textWatcher)

        views.promptPasswordPasswordReveal.setOnClickListener {
            passwordVisible = !passwordVisible
            views.promptPassword.showPassword(passwordVisible)
            views.promptPasswordPasswordReveal.render(passwordVisible)
        }

        MaterialAlertDialogBuilder(activity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.devices_delete_dialog_title)
                .setView(dialogLayout)
                .setPositiveButton(R.string.auth_submit, null)
                .setNegativeButton(R.string.cancel, null)
                .setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                        dialog.cancel()
                        return@OnKeyListener true
                    }
                    false
                })
                .setOnDismissListener {
                    dialogLayout.hideKeyboard()
                }
                .create()
                .apply {
                    setOnShowListener {
                        getButton(AlertDialog.BUTTON_POSITIVE)
                                .setOnClickListener {
                                    if (views.promptPassword.text.toString().isEmpty()) {
                                        views.promptPasswordTil.error = activity.getString(R.string.error_empty_field_your_password)
                                    } else {
                                        listener.invoke(views.promptPassword.text.toString())
                                        dismiss()
                                    }
                                }
                    }
                }
                .show()
    }
}
