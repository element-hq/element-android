/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.consent

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.core.dialogs.DialogLocker
import im.vector.app.core.platform.Restorable
import im.vector.app.features.webview.VectorWebViewActivity
import im.vector.app.features.webview.WebViewMode
import im.vector.lib.strings.CommonStrings

class ConsentNotGivenHelper(
        private val activity: Activity,
        private val dialogLocker: DialogLocker
) :
        Restorable by dialogLocker {

    /* ==========================================================================================
     * Public methods
     * ========================================================================================== */

    /**
     * Display the consent dialog, if not already displayed.
     */
    fun displayDialog(consentUri: String, homeServerHost: String) {
        dialogLocker.displayDialog {
            MaterialAlertDialogBuilder(activity)
                    .setTitle(CommonStrings.settings_app_term_conditions)
                    .setMessage(activity.getString(CommonStrings.dialog_user_consent_content, homeServerHost))
                    .setPositiveButton(CommonStrings.dialog_user_consent_submit) { _, _ ->
                        openWebViewActivity(consentUri)
                    }
        }
    }

    /* ==========================================================================================
     * Private
     * ========================================================================================== */

    private fun openWebViewActivity(consentUri: String) {
        val intent = VectorWebViewActivity.getIntent(activity, consentUri, activity.getString(CommonStrings.settings_app_term_conditions), WebViewMode.CONSENT)
        activity.startActivity(intent)
    }
}
