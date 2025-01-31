/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.commonmark.ext.maths

import org.commonmark.node.CustomNode
import org.commonmark.node.Delimited

internal class InlineMaths(private val delimiter: InlineDelimiter) : CustomNode(), Delimited {
    enum class InlineDelimiter {
        SINGLE_DOLLAR,
        ROUND_BRACKET_ESCAPED
    }

    override fun getOpeningDelimiter(): String {
        return when (delimiter) {
            InlineDelimiter.SINGLE_DOLLAR -> "$"
            InlineDelimiter.ROUND_BRACKET_ESCAPED -> "\\("
        }
    }

    override fun getClosingDelimiter(): String {
        return when (delimiter) {
            InlineDelimiter.SINGLE_DOLLAR -> "$"
            InlineDelimiter.ROUND_BRACKET_ESCAPED -> "\\)"
        }
    }
}
