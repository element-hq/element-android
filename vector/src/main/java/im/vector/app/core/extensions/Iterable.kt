/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

/**
 * Returns the last element yielding the smallest value of the given function or `null` if there are no elements.
 */
inline fun <T, R : Comparable<R>> Iterable<T>.lastMinBy(selector: (T) -> R): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var minElem = iterator.next()
    var minValue = selector(minElem)
    while (iterator.hasNext()) {
        val e = iterator.next()
        val v = selector(e)
        if (minValue >= v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Call each for each item, and between between each items.
 */
inline fun <T> Collection<T>.join(each: (Int, T) -> Unit, between: (Int, T) -> Unit) {
    val lastIndex = size - 1
    forEachIndexed { idx, t ->
        each(idx, t)

        if (idx != lastIndex) {
            between(idx, t)
        }
    }
}
