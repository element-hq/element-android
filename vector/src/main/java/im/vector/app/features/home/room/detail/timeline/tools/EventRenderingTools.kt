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

package im.vector.app.features.home.room.detail.timeline.tools

import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.text.toSpannable
import im.vector.app.core.linkify.VectorLinkify
import im.vector.app.core.utils.EvenBetterLinkMovementMethod
import im.vector.app.core.utils.isValidUrl
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.html.PillImageSpan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.permalinks.MatrixLinkify
import org.matrix.android.sdk.api.session.permalinks.MatrixPermalinkSpan

fun CharSequence.findPillsAndProcess(scope: CoroutineScope, processBlock: (PillImageSpan) -> Unit) {
    scope.launch(Dispatchers.Main) {
        withContext(Dispatchers.IO) {
            toSpannable().let { spannable ->
                spannable.getSpans(0, spannable.length, PillImageSpan::class.java)
            }
        }.forEach { processBlock(it) }
    }
}

fun CharSequence.linkify(callback: TimelineEventController.UrlClickCallback?): CharSequence {
    val text = this.toString()
    // SpannableStringBuilder is used to avoid Epoxy throwing ImmutableModelException
    val spannable = SpannableStringBuilder(this)
    MatrixLinkify.addLinks(spannable, object : MatrixPermalinkSpan.Callback {
        override fun onUrlClicked(url: String) {
            callback?.onUrlClicked(url, text)
        }
    })
    VectorLinkify.addLinks(spannable, true)
    return spannable
}

// Better link movement methods fixes the issue when
// long pressing to open the context menu on a TextView also triggers an autoLink click.
fun createLinkMovementMethod(urlClickCallback: TimelineEventController.UrlClickCallback?): EvenBetterLinkMovementMethod {
    return EvenBetterLinkMovementMethod(object : EvenBetterLinkMovementMethod.OnLinkClickListener {
        override fun onLinkClicked(textView: TextView, span: ClickableSpan, url: String, actualText: String): Boolean {
            // Always return false if the url is not valid, so the EvenBetterLinkMovementMethod can fallback to default click listener.
            return url.isValidUrl() && urlClickCallback?.onUrlClicked(url, actualText) == true
        }
    })
            .apply {
                // We need also to fix the case when long click on link will trigger long click on cell
                setOnLinkLongClickListener { tv, url ->
                    // Long clicks are handled by parent, return true to block android to do something with url
                    // Always return false if the url is not valid, so the EvenBetterLinkMovementMethod can fallback to default click listener.
                    if (url.isValidUrl() && urlClickCallback?.onUrlLongClicked(url) == true) {
                        tv.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0))
                        true
                    } else {
                        false
                    }
                }
            }
}
