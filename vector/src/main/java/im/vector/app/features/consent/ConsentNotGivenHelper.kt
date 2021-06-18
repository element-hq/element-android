/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.app.features.consent

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.dialogs.DialogLocker
import im.vector.app.core.platform.Restorable
import im.vector.app.features.webview.VectorWebViewActivity
import im.vector.app.features.webview.WebViewMode

class ConsentNotGivenHelper(private val activity: Activity,
                            private val dialogLocker: DialogLocker) :
        Restorable by dialogLocker {

    /* ==========================================================================================
     * Public methods
     * ========================================================================================== */

    /**
     * Display the consent dialog, if not already displayed
     */
    fun displayDialog(consentUri: String, homeServerHost: String) {
        dialogLocker.displayDialog {
            MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.settings_app_term_conditions)
                    .setMessage(activity.getString(R.string.dialog_user_consent_content, homeServerHost))
                    .setPositiveButton(R.string.dialog_user_consent_submit) { _, _ ->
                        openWebViewActivity(consentUri)
                    }
        }
    }

    /* ==========================================================================================
     * Private
     * ========================================================================================== */

    private fun openWebViewActivity(consentUri: String) {
        val intent = VectorWebViewActivity.getIntent(activity, consentUri, activity.getString(R.string.settings_app_term_conditions), WebViewMode.CONSENT)
        activity.startActivity(intent)
    }
}
