/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.commonmark.ext.maths.internal

import org.commonmark.ext.maths.DisplayMaths
import org.commonmark.ext.maths.InlineMaths
import org.commonmark.node.Text
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

internal class DollarMathsDelimiterProcessor : DelimiterProcessor {
    override fun getOpeningCharacter() = '$'

    override fun getClosingCharacter() = '$'

    override fun getMinLength() = 1

    override fun getDelimiterUse(opener: DelimiterRun, closer: DelimiterRun): Int {
        return if (opener.length() == 1 && closer.length() == 1) 1 // inline
        else if (opener.length() == 2 && closer.length() == 2) 2 // display
        else 0
    }

    override fun process(opener: Text, closer: Text, delimiterUse: Int) {
        val maths = if (delimiterUse == 1) {
            InlineMaths(InlineMaths.InlineDelimiter.SINGLE_DOLLAR)
        } else {
            DisplayMaths(DisplayMaths.DisplayDelimiter.DOUBLE_DOLLAR)
        }
        var tmp = opener.next
        while (tmp != null && tmp !== closer) {
            val next = tmp.next
            maths.appendChild(tmp)
            tmp = next
        }
        opener.insertAfter(maths)
    }
}
