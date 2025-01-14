/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.html

import android.text.Spanned
import android.text.style.MetricAffectingSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import im.vector.app.EmojiSpanify
import im.vector.app.features.home.room.detail.timeline.item.BindingOptions
import javax.inject.Inject

class SpanUtils @Inject constructor(
        private val emojiSpanify: EmojiSpanify
) {
    fun getBindingOptions(charSequence: CharSequence): BindingOptions {
        val emojiCharSequence = emojiSpanify.spanify(charSequence)

        if (emojiCharSequence !is Spanned) {
            return BindingOptions()
        }

        return BindingOptions(
                canUseTextFuture = canUseTextFuture(emojiCharSequence)
        )
    }

    /**
     * TextFutures do not support StrikethroughSpan, UnderlineSpan or MetricAffectingSpan
     * Workaround for https://issuetracker.google.com/issues/188454876
     */
    private fun canUseTextFuture(spanned: Spanned): Boolean {
        return spanned
                .getSpans(0, spanned.length, Any::class.java)
                .all { it !is StrikethroughSpan && it !is UnderlineSpan && it !is MetricAffectingSpan }
    }
}
