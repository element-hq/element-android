/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

/**
 * Recursive through the throwable and its causes for the given predicate.
 *
 * @return true when the predicate finds a match.
 */
tailrec fun Throwable?.crawlCausesFor(predicate: (Throwable) -> Boolean): Boolean {
    return when {
        this == null -> false
        else -> {
            when (predicate(this)) {
                true -> true
                else -> this.cause.crawlCausesFor(predicate)
            }
        }
    }
}
