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

package im.vector.app.features.crypto.recover

import android.app.Activity
import android.content.DialogInterface
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.databinding.DialogRecoveryKeySavedInfoBinding
import me.gujun.android.span.image
import me.gujun.android.span.span

class KeepItSafeDialog {

    fun show(activity: Activity) {
        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_recovery_key_saved_info, null)
        val views = DialogRecoveryKeySavedInfoBinding.bind(dialogLayout)

        views.keepItSafeText.text = span {
            span {
                image(ContextCompat.getDrawable(activity, R.drawable.ic_check_on)!!)
                +" "
                +activity.getString(R.string.bootstrap_crosssigning_print_it)
                +"\n\n"
                image(ContextCompat.getDrawable(activity, R.drawable.ic_check_on)!!)
                +" "
                +activity.getString(R.string.bootstrap_crosssigning_save_usb)
                +"\n\n"
                image(ContextCompat.getDrawable(activity, R.drawable.ic_check_on)!!)
                +" "
                +activity.getString(R.string.bootstrap_crosssigning_save_cloud)
                +"\n\n"
            }
        }

        MaterialAlertDialogBuilder(activity)
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .setTitle(R.string.devices_delete_dialog_title)
                .setView(dialogLayout)
                .setPositiveButton(R.string.ok, null)
                .setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                        dialog.cancel()
                        return@OnKeyListener true
                    }
                    false
                })
                .create()
                .show()
    }
}
