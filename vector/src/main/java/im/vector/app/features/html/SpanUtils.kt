/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.html

import android.os.Build
import android.text.Spanned
import android.text.style.MetricAffectingSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.emoji2.text.EmojiCompat
import im.vector.app.features.home.room.detail.timeline.item.BindingOptions
import javax.inject.Inject

class SpanUtils @Inject constructor() {
    fun getBindingOptions(charSequence: CharSequence): BindingOptions {
        val emojiCharSequence = EmojiCompat.get().process(charSequence)

        if (emojiCharSequence !is Spanned) {
            return BindingOptions()
        }

        return BindingOptions(
                canUseTextFuture = canUseTextFuture(emojiCharSequence),
                preventMutation = mustPreventMutation(emojiCharSequence)
        )
    }

    // Workaround for https://issuetracker.google.com/issues/188454876
    private fun canUseTextFuture(spanned: Spanned): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // On old devices, it works correctly
            return true
        }

        return spanned
                .getSpans(0, spanned.length, Any::class.java)
                .all { it !is StrikethroughSpan && it !is UnderlineSpan && it !is MetricAffectingSpan }
    }

    // Workaround for setting text during binding which mutate the text itself
    private fun mustPreventMutation(spanned: Spanned): Boolean {
        return spanned
                .getSpans(0, spanned.length, Any::class.java)
                .any { it is MetricAffectingSpan }
    }
}
