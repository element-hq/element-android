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
 * A FIFO circular buffer of T
 * This class is not thread safe
 */
class CircularCache<T : Any>(cacheSize: Int, factory: (Int) -> Array<T?>) {

    companion object {
        inline fun <reified T : Any> create(cacheSize: Int) = CircularCache(cacheSize) { Array<T?>(cacheSize) { null } }
    }

    private val cache = factory(cacheSize)
    private var writeIndex = 0

    fun contains(key: T): Boolean = cache.contains(key)

    fun put(key: T) {
        if (writeIndex == cache.size) {
            writeIndex = 0
        }
        cache[writeIndex] = key
        writeIndex++
    }
}
