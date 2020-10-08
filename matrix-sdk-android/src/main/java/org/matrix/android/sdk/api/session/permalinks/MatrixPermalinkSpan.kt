/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.permalinks

import android.text.style.ClickableSpan
import android.view.View
import org.matrix.android.sdk.api.session.permalinks.MatrixPermalinkSpan.Callback

/**
 * This MatrixPermalinkSpan is a clickable span which use a [Callback] to communicate back.
 * @param url the permalink url tied to the span
 * @param callback the callback to use.
 */
class MatrixPermalinkSpan(private val url: String,
                          private val callback: Callback? = null) : ClickableSpan() {

    interface Callback {
        fun onUrlClicked(url: String)
    }

    override fun onClick(widget: View) {
        callback?.onUrlClicked(url)
    }
}
