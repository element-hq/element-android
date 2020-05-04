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

package im.vector.riotx.features.disclaimer

import android.app.Activity
import androidx.preference.PreferenceManager
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import im.vector.riotx.BuildConfig
import im.vector.riotx.R
import im.vector.riotx.core.extensions.setTextWithColoredPart
import im.vector.riotx.core.utils.openPlayStore

// Increase this value to show again the disclaimer dialog after an upgrade of the application
private const val CURRENT_DISCLAIMER_VALUE = 1

private const val SHARED_PREF_KEY = "LAST_DISCLAIMER_VERSION_VALUE"

fun showDisclaimerDialog(activity: Activity) {
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)

    if (sharedPrefs.getInt(SHARED_PREF_KEY, 0) < CURRENT_DISCLAIMER_VALUE) {
        sharedPrefs.edit {
            putInt(SHARED_PREF_KEY, CURRENT_DISCLAIMER_VALUE)
        }

        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_disclaimer_content, null)

        val textView = (dialogLayout as ViewGroup).findViewById<TextView>(R.id.dialogDisclaimerContentLine2)
        @Suppress("ConstantConditionIf")
        if (BuildConfig.FLAVOR == "gplay") {
            textView.setTextWithColoredPart(R.string.alpha_disclaimer_content_line_2_gplay, R.string.alpha_disclaimer_content_line_2_gplay_colored_part)

            textView.setOnClickListener {
                openPlayStore(activity)
            }
        } else {
            textView.setText(R.string.alpha_disclaimer_content_line_2_fdroid)
        }

        AlertDialog.Builder(activity)
                .setView(dialogLayout)
                .setCancelable(false)
                .setPositiveButton(R.string._continue, null)
                .show()
    }
}
