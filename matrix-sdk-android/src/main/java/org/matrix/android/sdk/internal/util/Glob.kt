/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

internal fun String.hasSpecialGlobChar(): Boolean {
    return contains("*") || contains("?")
}

// Very simple glob to regexp converter
internal fun String.simpleGlobToRegExp(): String {
    val string = this
    return buildString {
        // append("^")
        string.forEach { char ->
            when (char) {
                '*' -> append(".*")
                '?' -> append(".")
                '.' -> append("\\.")
                '\\' -> append("\\\\")
                else -> append(char)
            }
        }
        // append("$")
    }
}
