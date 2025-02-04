/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

// Create a new Set including the provided element if not already present, or removing the element if already present
fun <T> Set<T>.toggle(element: T, singleElement: Boolean = false): Set<T> {
    return if (contains(element)) {
        if (singleElement) {
            emptySet()
        } else {
            minus(element)
        }
    } else {
        if (singleElement) {
            setOf(element)
        } else {
            plus(element)
        }
    }
}
