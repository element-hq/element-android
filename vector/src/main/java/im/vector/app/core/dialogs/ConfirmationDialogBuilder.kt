/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.core.dialogs

import android.app.Activity
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.databinding.DialogConfirmationWithReasonBinding

object ConfirmationDialogBuilder {

    fun show(activity: Activity,
             askForReason: Boolean,
             @StringRes titleRes: Int,
             @StringRes confirmationRes: Int,
             @StringRes positiveRes: Int,
             @StringRes reasonHintRes: Int,
             confirmation: (String?) -> Unit) {
        val layout = activity.layoutInflater.inflate(R.layout.dialog_confirmation_with_reason, null)
        val views = DialogConfirmationWithReasonBinding.bind(layout)
        views.dialogConfirmationText.setText(confirmationRes)

        views.dialogReasonCheck.isVisible = askForReason
        views.dialogReasonTextInputLayout.isVisible = askForReason

        views.dialogReasonCheck.setOnCheckedChangeListener { _, isChecked ->
            views.dialogReasonTextInputLayout.isEnabled = isChecked
        }
        if (askForReason && reasonHintRes != 0) {
            views.dialogReasonInput.setHint(reasonHintRes)
        }

        MaterialAlertDialogBuilder(activity)
                .setTitle(titleRes)
                .setView(layout)
                .setPositiveButton(positiveRes) { _, _ ->
                    val reason = views.dialogReasonInput.text.toString()
                            .takeIf { askForReason }
                            ?.takeIf { views.dialogReasonCheck.isChecked }
                            ?.takeIf { it.isNotBlank() }
                    confirmation(reason)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }
}
