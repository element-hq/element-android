/*
 * Copyright 2019 New Vector Ltd
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
