/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.extensions

fun CharSequence.ensurePrefix(prefix: CharSequence): CharSequence {
    return when {
        startsWith(prefix) -> this
        else -> "$prefix$this"
    }
}

/**
 * Append a new line and then the provided string.
 */
fun StringBuilder.appendNl(str: String) = append("\n").append(str)

/**
 * Returns null if the string is empty.
 */
fun String.ensureNotEmpty() = ifEmpty { null }
