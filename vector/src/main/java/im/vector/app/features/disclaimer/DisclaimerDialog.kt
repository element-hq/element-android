/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.disclaimer

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.di.DefaultSharedPreferences
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.features.settings.VectorSettingsUrls

// Increase this value to show again the disclaimer dialog after an upgrade of the application
private const val CURRENT_DISCLAIMER_VALUE = 2

const val SHARED_PREF_KEY = "LAST_DISCLAIMER_VERSION_VALUE"

fun showDisclaimerDialog(activity: Activity) {
    val sharedPrefs = DefaultSharedPreferences.getInstance(activity)

    if (sharedPrefs.getInt(SHARED_PREF_KEY, 0) < CURRENT_DISCLAIMER_VALUE) {
        sharedPrefs.edit {
            putInt(SHARED_PREF_KEY, CURRENT_DISCLAIMER_VALUE)
        }

        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_disclaimer_content, null)

        MaterialAlertDialogBuilder(activity)
                .setView(dialogLayout)
                .setCancelable(false)
                .setNegativeButton(R.string.disclaimer_negative_button, null)
                .setPositiveButton(R.string.disclaimer_positive_button) { _, _ ->
                    openUrlInChromeCustomTab(activity, null, VectorSettingsUrls.DISCLAIMER_URL)
                }
                .show()
    }
}

fun doNotShowDisclaimerDialog(context: Context) {
    val sharedPrefs = DefaultSharedPreferences.getInstance(context)

    sharedPrefs.edit {
        putInt(SHARED_PREF_KEY, CURRENT_DISCLAIMER_VALUE)
    }
}
