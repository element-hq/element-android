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

package im.vector.app.features.autocomplete.command

import android.text.Spannable
import com.otaliastudios.autocomplete.AutocompletePolicy
import javax.inject.Inject

class CommandAutocompletePolicy @Inject constructor() : AutocompletePolicy {

    var enabled: Boolean = true

    override fun getQuery(text: Spannable): CharSequence {
        if (text.length > 0) {
            return text.substring(1, text.length)
        }
        // Should not happen
        return ""
    }

    override fun onDismiss(text: Spannable?) {
    }

    // Only if text which starts with '/' and without space
    override fun shouldShowPopup(text: Spannable?, cursorPos: Int): Boolean {
        return enabled && text?.startsWith("/") == true &&
                !text.contains(" ")
    }

    override fun shouldDismissPopup(text: Spannable?, cursorPos: Int): Boolean {
        return !shouldShowPopup(text, cursorPos)
    }
}
