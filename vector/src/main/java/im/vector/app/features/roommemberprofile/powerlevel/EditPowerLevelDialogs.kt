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

package im.vector.app.features.roommemberprofile.powerlevel

import android.app.Activity
import android.content.DialogInterface
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import kotlinx.android.synthetic.main.dialog_edit_power_level.view.*

object EditPowerLevelDialogs {

    fun showChoice(activity: Activity, currentRole: Role, listener: (Int) -> Unit) {
        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_edit_power_level, null)
        dialogLayout.powerLevelRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            dialogLayout.powerLevelCustomEditLayout.isVisible = checkedId == R.id.powerLevelCustomRadio
        }
        dialogLayout.powerLevelCustomEdit.setText(currentRole.value.toString())

        when (currentRole) {
            Role.Admin     -> dialogLayout.powerLevelAdminRadio.isChecked = true
            Role.Moderator -> dialogLayout.powerLevelModeratorRadio.isChecked = true
            Role.Default   -> dialogLayout.powerLevelDefaultRadio.isChecked = true
            else           -> dialogLayout.powerLevelCustomRadio.isChecked = true
        }

        AlertDialog.Builder(activity)
                .setTitle(R.string.power_level_edit_title)
                .setView(dialogLayout)
                .setPositiveButton(R.string.edit) { _, _ ->
                    val newValue = when (dialogLayout.powerLevelRadioGroup.checkedRadioButtonId) {
                        R.id.powerLevelAdminRadio     -> Role.Admin.value
                        R.id.powerLevelModeratorRadio -> Role.Moderator.value
                        R.id.powerLevelDefaultRadio   -> Role.Default.value
                        else                          -> {
                            dialogLayout.powerLevelCustomEdit.text?.toString()?.toInt() ?: currentRole.value
                        }
                    }
                    listener(newValue)
                }
                .setNegativeButton(R.string.cancel, null)
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
        AlertDialog.Builder(activity)
                .setMessage(R.string.room_participants_power_level_prompt)
                .setPositiveButton(R.string.yes) { _, _ ->
                    onValidate()
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    fun showDemoteWarning(activity: Activity, onValidate: () -> Unit) {
        // Ask to the user the confirmation to downgrade his own role.
        AlertDialog.Builder(activity)
                .setTitle(R.string.room_participants_power_level_demote_warning_title)
                .setMessage(R.string.room_participants_power_level_demote_warning_prompt)
                .setPositiveButton(R.string.room_participants_power_level_demote) { _, _ ->
                    onValidate()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }
}
