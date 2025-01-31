/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.permalinks

import android.text.Spannable
import org.matrix.android.sdk.api.MatrixPatterns

/**
 *  MatrixLinkify take a piece of text and turns all of the
 *  matrix patterns matches in the text into clickable links.
 */
object MatrixLinkify {

    /**
     * Find the matrix spans i.e matrix id , user id ... to display them as URL.
     *
     * @param spannable the text in which the matrix items has to be clickable.
     * @param callback listener to be notified when the span is clicked
     */
    @Suppress("UNUSED_PARAMETER")
    fun addLinks(spannable: Spannable, callback: MatrixPermalinkSpan.Callback?): Boolean {
        /**
         * I disable it because it mess up with pills, and even with pills, it does not work correctly:
         * The url is not correct. Ex: for @user:matrix.org, the url will be @user:matrix.org, instead of a matrix.to
         */

        // sanity checks
        if (spannable.isEmpty()) {
            return false
        }
        val text = spannable.toString()
        var hasMatch = false
        for (pattern in MatrixPatterns.MATRIX_PATTERNS) {
            for (match in pattern.findAll(spannable)) {
                hasMatch = true
                val startPos = match.range.first
                if (startPos == 0 || text[startPos - 1] != '/') {
                    val endPos = match.range.last + 1
                    var url = text.substring(match.range)
                    if (MatrixPatterns.isUserId(url) ||
                            MatrixPatterns.isRoomAlias(url) ||
                            MatrixPatterns.isRoomId(url) ||
                            MatrixPatterns.isGroupId(url) ||
                            MatrixPatterns.isEventId(url)) {
                        url = PermalinkService.MATRIX_TO_URL_BASE + url
                    }
                    val span = MatrixPermalinkSpan(url, callback)
                    spannable.setSpan(span, startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        return hasMatch

//        return false
    }
}
