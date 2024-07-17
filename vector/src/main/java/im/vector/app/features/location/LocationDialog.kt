/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.location

import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.lib.strings.CommonStrings

fun Fragment.showUserLocationNotAvailableErrorDialog(onConfirmListener: () -> Unit) {
    MaterialAlertDialogBuilder(requireActivity())
            .setTitle(CommonStrings.location_not_available_dialog_title)
            .setMessage(CommonStrings.location_not_available_dialog_content)
            .setPositiveButton(CommonStrings.ok) { _, _ ->
                onConfirmListener()
            }
            .setCancelable(false)
            .show()
}
