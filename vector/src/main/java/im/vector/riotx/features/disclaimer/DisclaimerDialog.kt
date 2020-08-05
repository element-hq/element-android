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
import androidx.appcompat.app.AlertDialog
import im.vector.riotx.R
import im.vector.riotx.core.utils.openPlayStore

fun showDisclaimerDialog(activity: Activity) {
    val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_disclaimer_content, null)

    AlertDialog.Builder(activity)
            .setView(dialogLayout)
            .setCancelable(false)
            .setPositiveButton(R.string.the_beta_is_over_get_element) { _, _ ->
                openPlayStore(activity, "im.vector.app")
            }
            .setNegativeButton(R.string.later, null)
            .show()
}
