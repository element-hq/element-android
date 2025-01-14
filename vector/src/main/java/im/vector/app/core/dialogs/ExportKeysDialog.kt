/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.dialogs

import android.app.Activity
import android.text.Editable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.platform.SimpleTextWatcher
import im.vector.app.databinding.DialogExportE2eKeysBinding
import im.vector.lib.strings.CommonStrings

class ExportKeysDialog {

    fun show(activity: Activity, exportKeyDialogListener: ExportKeyDialogListener) {
        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_export_e2e_keys, null)
        val views = DialogExportE2eKeysBinding.bind(dialogLayout)
        val builder = MaterialAlertDialogBuilder(activity)
                .setTitle(CommonStrings.encryption_export_room_keys)
                .setView(dialogLayout)

        val textWatcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                when {
                    views.exportDialogEt.text.isNullOrEmpty() -> {
                        views.exportDialogSubmit.isEnabled = false
                        views.exportDialogTilConfirm.error = null
                    }
                    views.exportDialogEt.text.toString() == views.exportDialogEtConfirm.text.toString() -> {
                        views.exportDialogSubmit.isEnabled = true
                        views.exportDialogTilConfirm.error = null
                    }
                    else -> {
                        views.exportDialogSubmit.isEnabled = false
                        views.exportDialogTilConfirm.error = activity.getString(CommonStrings.passphrase_passphrase_does_not_match)
                    }
                }
            }
        }

        views.exportDialogEt.addTextChangedListener(textWatcher)
        views.exportDialogEtConfirm.addTextChangedListener(textWatcher)

        val exportDialog = builder.show()

        views.exportDialogSubmit.setOnClickListener {
            exportKeyDialogListener.onPassphrase(views.exportDialogEt.text.toString())

            exportDialog.dismiss()
        }
    }

    interface ExportKeyDialogListener {
        fun onPassphrase(passphrase: String)
    }
}
