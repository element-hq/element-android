/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.util

import org.matrix.android.sdk.internal.util.unescapeHtml

object ContentUtils {
    fun extractUsefulTextFromReply(repliedBody: String): String {
        val lines = repliedBody.lines()
        var wellFormed = repliedBody.startsWith(">")
        var endOfPreviousFound = false
        val usefulLines = ArrayList<String>()
        lines.forEach {
            if (it == "") {
                endOfPreviousFound = true
                return@forEach
            }
            if (!endOfPreviousFound) {
                wellFormed = wellFormed && it.startsWith(">")
            } else {
                usefulLines.add(it)
            }
        }
        return usefulLines.joinToString("\n").takeIf { wellFormed } ?: repliedBody
    }

    fun extractUsefulTextFromHtmlReply(repliedBody: String): String {
        if (repliedBody.startsWith("<mx-reply>")) {
            val closingTagIndex = repliedBody.lastIndexOf("</mx-reply>")
            if (closingTagIndex != -1) {
                return repliedBody.substring(closingTagIndex + "</mx-reply>".length).trim()
            }
        }
        return repliedBody
    }

    @Suppress("RegExpRedundantEscape")
    fun formatSpoilerTextFromHtml(formattedBody: String): String {
        // var reason = "",
        // can capture the spoiler reason for better formatting? ex. { reason = it.value;  ">"}
        return formattedBody.replace("(?<=<span data-mx-spoiler)=\\\".+?\\\">".toRegex(), ">")
                .replace("(?<=<span data-mx-spoiler>).+?(?=</span>)".toRegex()) { SPOILER_CHAR.repeat(it.value.length) }
                .unescapeHtml()
    }

    private const val SPOILER_CHAR = "█"
}
