/*
 * Copyright (c) 2021 New Vector Ltd
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

package fr.gouv.tchap.core.dialogs

import android.app.Activity
import android.content.DialogInterface
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.isEmail
import im.vector.app.databinding.DialogInviteByIdBinding
import im.vector.app.features.settings.VectorLocale

class InviteByEmailDialog(
        private val activity: Activity
) {

    fun interface Listener {
        fun inviteByEmail(email: String)
    }

    fun show(listener: Listener) {
        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_invite_by_id, null)
        val views = DialogInviteByIdBinding.bind(dialogLayout)

        val inviteDialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.people_search_invite_by_id_dialog_title)
                .setView(dialogLayout)
                .setPositiveButton(R.string.invite) { _, _ ->
                    val text = views.inviteByIdEditText.text.toString().lowercase(VectorLocale.applicationLocale).trim()

                    if (text.isEmail()) {
                        views.root.hideKeyboard()
                        listener.inviteByEmail(text)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()

        val inviteButton = inviteDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        inviteButton.isEnabled = false

        views.inviteByIdEditText.doOnTextChanged { text, _, _, _ ->
            if (text != null) {
                inviteButton.isEnabled = text.trim().isEmail()
            }
        }
    }
}
