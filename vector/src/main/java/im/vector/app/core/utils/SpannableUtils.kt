/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.utils

import android.text.Spannable
import android.text.style.BulletSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.ColorInt
import me.gujun.android.span.Span
import me.gujun.android.span.span

fun Spannable.styleMatchingText(match: String, typeFace: Int): Spannable {
    if (match.isEmpty()) return this
    indexOf(match).takeIf { it != -1 }?.let { start ->
        this.setSpan(StyleSpan(typeFace), start, start + match.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return this
}

fun Spannable.colorizeMatchingText(match: String, @ColorInt color: Int): Spannable {
    if (match.isEmpty()) return this
    indexOf(match).takeIf { it != -1 }?.let { start ->
        this.setSpan(ForegroundColorSpan(color), start, start + match.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return this
}

fun Spannable.tappableMatchingText(match: String, clickSpan: ClickableSpan): Spannable {
    if (match.isEmpty()) return this
    indexOf(match).takeIf { it != -1 }?.let { start ->
        this.setSpan(clickSpan, start, start + match.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return this
}

fun Span.bullet(
        text: CharSequence = "",
        init: Span.() -> Unit = {}
): Span = apply {
    append(Span(parent = this).apply {
        this.text = text
        this.spans.add(BulletSpan())
        init()
        build()
    })
}

fun String.colorTerminatingFullStop(@ColorInt color: Int): CharSequence {
    val fullStop = "."
    return if (endsWith(fullStop)) {
        span {
            +this@colorTerminatingFullStop.removeSuffix(fullStop)
            span(fullStop) {
                textColor = color
            }
        }
    } else {
        this
    }
}
