/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

import android.app.Activity
import android.content.DialogInterface
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.databinding.DialogRecoveryKeySavedInfoBinding
import im.vector.lib.strings.CommonStrings
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
                +activity.getString(CommonStrings.bootstrap_crosssigning_print_it)
                +"\n\n"
                image(ContextCompat.getDrawable(activity, R.drawable.ic_check_on)!!)
                +" "
                +activity.getString(CommonStrings.bootstrap_crosssigning_save_usb)
                +"\n\n"
                image(ContextCompat.getDrawable(activity, R.drawable.ic_check_on)!!)
                +" "
                +activity.getString(CommonStrings.bootstrap_crosssigning_save_cloud)
                +"\n\n"
            }
        }

        MaterialAlertDialogBuilder(activity)
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .setTitle(CommonStrings.devices_delete_dialog_title)
                .setView(dialogLayout)
                .setPositiveButton(CommonStrings.ok, null)
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
