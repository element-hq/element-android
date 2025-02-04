/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

    private fun CircularCache<Int>.putInOrder(vararg values: Int) {
        values.forEach { put(it) }
    }
}
