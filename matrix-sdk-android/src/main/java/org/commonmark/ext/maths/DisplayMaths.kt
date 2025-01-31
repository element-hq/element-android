/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.commonmark.ext.maths

import org.commonmark.node.CustomBlock

internal class DisplayMaths(private val delimiter: DisplayDelimiter) : CustomBlock() {
    enum class DisplayDelimiter {
        DOUBLE_DOLLAR,
        SQUARE_BRACKET_ESCAPED
    }
}
