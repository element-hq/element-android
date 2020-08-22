/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Call each for each item, and between between each items
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
