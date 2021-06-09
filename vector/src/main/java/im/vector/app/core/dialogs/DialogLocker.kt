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

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.core.platform.Restorable
import timber.log.Timber

private const val KEY_DIALOG_IS_DISPLAYED = "DialogLocker.KEY_DIALOG_IS_DISPLAYED"

/**
 * Class to avoid displaying twice the same dialog
 */
class DialogLocker(savedInstanceState: Bundle?) : Restorable {

    private var isDialogDisplayed = savedInstanceState?.getBoolean(KEY_DIALOG_IS_DISPLAYED, false) == true

    private fun unlock() {
        isDialogDisplayed = false
    }

    private fun lock() {
        isDialogDisplayed = true
    }

    fun displayDialog(builder: () -> MaterialAlertDialogBuilder): AlertDialog? {
        return if (isDialogDisplayed) {
            Timber.w("Filtered dialog request")
            null
        } else {
            builder
                    .invoke()
                    .create()
                    .apply {
                        setOnShowListener { lock() }
                        setOnCancelListener { unlock() }
                        setOnDismissListener { unlock() }
                        show()
                    }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_DIALOG_IS_DISPLAYED, isDialogDisplayed)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        isDialogDisplayed = savedInstanceState?.getBoolean(KEY_DIALOG_IS_DISPLAYED, false) == true
    }
}
