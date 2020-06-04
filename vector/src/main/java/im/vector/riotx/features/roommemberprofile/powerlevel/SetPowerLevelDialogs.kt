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

package im.vector.riotx.features.roommemberprofile.powerlevel

import android.app.Activity
import android.content.DialogInterface
import android.view.KeyEvent
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import im.vector.matrix.android.api.session.room.powerlevels.PowerLevelsConstants
import im.vector.riotx.R
import im.vector.riotx.core.extensions.hideKeyboard
import kotlinx.android.synthetic.main.dialog_set_power_level.view.*

object SetPowerLevelDialogs {

    fun showChoice(activity: Activity, currentValue: Int, listener: (Int) -> Unit) {
        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_set_power_level, null)
        dialogLayout.powerLevelRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            dialogLayout.powerLevelCustomLayout.isVisible = checkedId == R.id.powerLevelCustomRadio
        }
        dialogLayout.powerLevelCustomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                dialogLayout.powerLevelCustomValue.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                //NOOP
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                //NOOP
            }
        })
        dialogLayout.powerLevelCustomSlider.progress = currentValue
        when (currentValue) {
            PowerLevelsConstants.DEFAULT_ROOM_ADMIN_LEVEL     -> dialogLayout.powerLevelAdminRadio.isChecked = true
            PowerLevelsConstants.DEFAULT_ROOM_MODERATOR_LEVEL -> dialogLayout.powerLevelModeratorRadio.isChecked = true
            PowerLevelsConstants.DEFAULT_ROOM_USER_LEVEL      -> dialogLayout.powerLevelDefaultRadio.isChecked = true
            else                                              -> dialogLayout.powerLevelCustomRadio.isChecked = true
        }

        AlertDialog.Builder(activity)
                .setTitle("Change power level")
                .setView(dialogLayout)
                .setPositiveButton(R.string.action_change)
                { _, _ ->
                    val newValue = when (dialogLayout.powerLevelRadioGroup.checkedRadioButtonId) {
                        R.id.powerLevelAdminRadio     -> PowerLevelsConstants.DEFAULT_ROOM_ADMIN_LEVEL
                        R.id.powerLevelModeratorRadio -> PowerLevelsConstants.DEFAULT_ROOM_MODERATOR_LEVEL
                        R.id.powerLevelDefaultRadio   -> PowerLevelsConstants.DEFAULT_ROOM_USER_LEVEL
                        else                          -> dialogLayout.powerLevelCustomSlider.progress
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
        // ask to the user to confirmation thu upgrade.
        AlertDialog.Builder(activity)
                .setMessage(R.string.room_participants_power_level_prompt)
                .setPositiveButton(R.string.yes) { _, _ ->
                    onValidate()
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }
}
