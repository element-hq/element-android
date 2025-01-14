/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roommemberprofile.powerlevel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.databinding.DialogEditPowerLevelBinding
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.powerlevels.Role

object EditPowerLevelDialogs {

    @SuppressLint("SetTextI18n")
    fun showChoice(
            activity: Activity,
            @StringRes titleRes: Int,
            currentRole: Role,
            listener: (Int) -> Unit
    ) {
        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_edit_power_level, null)
        val views = DialogEditPowerLevelBinding.bind(dialogLayout)
        views.powerLevelRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            views.powerLevelCustomEditLayout.isVisible = checkedId == R.id.powerLevelCustomRadio
        }
        views.powerLevelCustomEdit.setText("${currentRole.value}")

        when (currentRole) {
            Role.Admin -> views.powerLevelAdminRadio.isChecked = true
            Role.Moderator -> views.powerLevelModeratorRadio.isChecked = true
            Role.Default -> views.powerLevelDefaultRadio.isChecked = true
            else -> views.powerLevelCustomRadio.isChecked = true
        }

        MaterialAlertDialogBuilder(activity)
                .setTitle(titleRes)
                .setView(dialogLayout)
                .setPositiveButton(CommonStrings.edit) { _, _ ->
                    val newValue = when (views.powerLevelRadioGroup.checkedRadioButtonId) {
                        R.id.powerLevelAdminRadio -> Role.Admin.value
                        R.id.powerLevelModeratorRadio -> Role.Moderator.value
                        R.id.powerLevelDefaultRadio -> Role.Default.value
                        else -> {
                            views.powerLevelCustomEdit.text?.toString()?.toInt() ?: currentRole.value
                        }
                    }
                    listener(newValue)
                }
                .setNegativeButton(CommonStrings.action_cancel, null)
                .setOnKeyListener(DialogInterface.OnKeyListener
                { dialog, keyCode, event ->
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
                .show()
    }

    fun showValidation(activity: Activity, onValidate: () -> Unit) {
        // Ask to the user the confirmation to upgrade.
        MaterialAlertDialogBuilder(activity)
                .setMessage(CommonStrings.room_participants_power_level_prompt)
                .setPositiveButton(CommonStrings.yes) { _, _ ->
                    onValidate()
                }
                .setNegativeButton(CommonStrings.no, null)
                .show()
    }

    fun showDemoteWarning(activity: Activity, onValidate: () -> Unit) {
        // Ask to the user the confirmation to downgrade his own role.
        MaterialAlertDialogBuilder(activity)
                .setTitle(CommonStrings.room_participants_power_level_demote_warning_title)
                .setMessage(CommonStrings.room_participants_power_level_demote_warning_prompt)
                .setPositiveButton(CommonStrings.room_participants_power_level_demote) { _, _ ->
                    onValidate()
                }
                .setNegativeButton(CommonStrings.action_cancel, null)
                .show()
    }
}
