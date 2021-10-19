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

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class CircularCacheTest {

    @Test
    fun `when putting more than cache size then cache is limited to cache size`() {
        val (cache, internalData) = createIntCache(cacheSize = 3)

        cache.putInOrder(1, 1, 1, 1, 1, 1)

        internalData shouldBeEqualTo arrayOf(1, 1, 1)
    }

    @Test
    fun `when putting more than cache then acts as FIFO`() {
        val (cache, internalData) = createIntCache(cacheSize = 3)

        cache.putInOrder(1, 2, 3, 4)

        internalData shouldBeEqualTo arrayOf(4, 2, 3)
    }

    @Test
    fun `given empty cache when checking if contains key then is false`() {
        val (cache, _) = createIntCache(cacheSize = 3)

        val result = cache.contains(1)

        result shouldBeEqualTo false
    }

    @Test
    fun `given cached key when checking if contains key then is true`() {
        val (cache, _) = createIntCache(cacheSize = 3)

        cache.put(1)
        val result = cache.contains(1)

        result shouldBeEqualTo true
    }

    private fun createIntCache(cacheSize: Int): Pair<CircularCache<Int>, Array<Int?>> {
        var internalData: Array<Int?>? = null
        val factory: (Int) -> Array<Int?> = {
            Array<Int?>(it) { null }.also { array -> internalData = array }
        }
        return CircularCache(cacheSize, factory) to internalData!!
    }

    private fun CircularCache<Int>.putInOrder(vararg keys: Int) {
        keys.forEach { put(it) }
    }
}
