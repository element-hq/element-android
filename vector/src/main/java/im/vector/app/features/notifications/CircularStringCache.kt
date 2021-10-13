/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.notifications

/**
 * A FIFO circular buffer of strings
 */
class CircularStringCache(cacheSize: Int) {

    private val cache = Array(cacheSize) { "" }
    private var writeIndex = 0

    fun contains(key: String): Boolean = cache.contains(key)

    fun put(key: String) {
        if (writeIndex == cache.size - 1) {
            writeIndex = 0
        }
        cache[writeIndex] = key
        writeIndex++
    }
}
