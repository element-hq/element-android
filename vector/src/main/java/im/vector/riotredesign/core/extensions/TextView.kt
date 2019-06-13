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

package im.vector.riotredesign.core.extensions

import android.widget.TextView
import androidx.core.view.isVisible

/**
 * Set a text in the TextView, or set visibility to GONE if the text is null
 */
fun TextView.setTextOrHide(newText: String?, hideWhenBlank: Boolean = true) {
    if (newText == null
            || (newText.isBlank() && hideWhenBlank)) {
        isVisible = false
    } else {
        this.text = newText
        isVisible = true
    }
}