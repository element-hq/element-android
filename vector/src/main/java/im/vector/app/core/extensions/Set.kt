/*
 * Copyright (c) 2020 New Vector Ltd
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
