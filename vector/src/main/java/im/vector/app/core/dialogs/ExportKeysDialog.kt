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

package im.vector.app.core.dialogs

import android.app.Activity
import android.text.Editable
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.platform.SimpleTextWatcher

class ExportKeysDialog {

    private var passwordVisible = false

    fun show(activity: Activity, exportKeyDialogListener: ExportKeyDialogListener) {
        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_export_e2e_keys, null)
        val builder = AlertDialog.Builder(activity)
                .setTitle(R.string.encryption_export_room_keys)
                .setView(dialogLayout)

        val passPhrase1EditText = dialogLayout.findViewById<TextInputEditText>(R.id.exportDialogEt)
        val passPhrase2EditText = dialogLayout.findViewById<TextInputEditText>(R.id.exportDialogEtConfirm)
        val passPhrase2Til = dialogLayout.findViewById<TextInputLayout>(R.id.exportDialogTilConfirm)
        val exportButton = dialogLayout.findViewById<Button>(R.id.exportDialogSubmit)
        val textWatcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                when {
                    passPhrase1EditText.text.isNullOrEmpty()                                   -> {
                        exportButton.isEnabled = false
                        passPhrase2Til.error = null
                    }
                    passPhrase1EditText.text.toString() == passPhrase2EditText.text.toString() -> {
                        exportButton.isEnabled = true
                        passPhrase2Til.error = null
                    }
                    else                                                                       -> {
                        exportButton.isEnabled = false
                        passPhrase2Til.error = activity.getString(R.string.passphrase_passphrase_does_not_match)
                    }
                }
            }
        }

        passPhrase1EditText.addTextChangedListener(textWatcher)
        passPhrase2EditText.addTextChangedListener(textWatcher)

        val showPassword = dialogLayout.findViewById<ImageView>(R.id.exportDialogShowPassword)
        showPassword.setOnClickListener {
            passwordVisible = !passwordVisible
            passPhrase1EditText.showPassword(passwordVisible)
            passPhrase2EditText.showPassword(passwordVisible)
            showPassword.setImageResource(if (passwordVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye)
        }

        val exportDialog = builder.show()

        exportButton.setOnClickListener {
            exportKeyDialogListener.onPassphrase(passPhrase1EditText.text.toString())

            exportDialog.dismiss()
        }
    }

    interface ExportKeyDialogListener {
        fun onPassphrase(passphrase: String)
    }
}
