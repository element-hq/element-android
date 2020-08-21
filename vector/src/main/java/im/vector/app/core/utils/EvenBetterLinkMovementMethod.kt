/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.core.utils

import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.widget.TextView
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class EvenBetterLinkMovementMethod(private val onLinkClickListener: OnLinkClickListener? = null) : BetterLinkMovementMethod() {

    interface OnLinkClickListener {
        /**
         * @param textView   The TextView on which a click was registered.
         * @param span       The ClickableSpan which is clicked on.
         * @param url        The clicked URL.
         * @param actualText The original text which is spanned. Can be used to compare actualText and target url to prevent misleading urls.
         * @return true if this click was handled, false to let Android handle the URL.
         */
        fun onLinkClicked(textView: TextView, span: ClickableSpan, url: String, actualText: String): Boolean
    }

    override fun dispatchUrlClick(textView: TextView, clickableSpan: ClickableSpan) {
        val spanned = textView.text as Spanned
        val actualText = textView.text.subSequence(spanned.getSpanStart(clickableSpan), spanned.getSpanEnd(clickableSpan)).toString()
        val url = (clickableSpan as? URLSpan)?.url ?: actualText

        if (onLinkClickListener == null || !onLinkClickListener.onLinkClicked(textView, clickableSpan, url, actualText)) {
            // Let Android handle this long click as a short-click.
            clickableSpan.onClick(textView)
        }
    }
}
