/*
 * Copyright (c) 2026 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.raw

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val A_VALIDITY_DURATION = 1_000L
private const val A_TIMESTAMP = 10_000L
private const val SOME_DATA = "{}"

class RawCacheResolverTest {

    private fun resolve(
            snapshot: RawCacheSnapshot?,
            now: Long = A_TIMESTAMP,
            strict: Boolean = false,
    ) = resolveRawCache(
            snapshot = snapshot,
            now = now,
            validityDurationInMillis = A_VALIDITY_DURATION,
            strict = strict,
    )

    private fun snapshot(
            data: String = SOME_DATA,
            isNotFound: Boolean = false,
    ) = RawCacheSnapshot(data = data, isNotFound = isNotFound, lastUpdatedTimestamp = A_TIMESTAMP)

    @Test
    fun `given no cache entry when resolving then the request is performed with no fallback`() {
        resolve(null) shouldBeEqualTo RawCacheResolution.Fetch(null)
    }

    @Test
    fun `given a valid entry when resolving then the cached data is returned`() {
        resolve(snapshot()) shouldBeEqualTo RawCacheResolution.Return(SOME_DATA)
    }

    @Test
    fun `given a valid entry when resolving in strict mode then the cached data is still returned`() {
        resolve(snapshot(), strict = true) shouldBeEqualTo RawCacheResolution.Return(SOME_DATA)
    }

    @Test
    fun `given an entry which just expired when resolving then the request is performed`() {
        resolve(snapshot(), now = A_TIMESTAMP + A_VALIDITY_DURATION) shouldBeEqualTo RawCacheResolution.Fetch(SOME_DATA)
    }

    @Test
    fun `given an expired entry when resolving in strict mode then there is no stale fallback`() {
        resolve(
                snapshot(),
                now = A_TIMESTAMP + A_VALIDITY_DURATION,
                strict = true,
        ) shouldBeEqualTo RawCacheResolution.Fetch(null)
    }

    @Test
    fun `given a valid not found entry with no previous data when resolving then it fails without a request`() {
        resolve(snapshot(data = "", isNotFound = true)) shouldBeEqualTo RawCacheResolution.ThrowNotFound
    }

    @Test
    fun `given a valid not found entry with previous data when resolving then the stale data is returned`() {
        resolve(snapshot(isNotFound = true)) shouldBeEqualTo RawCacheResolution.Return(SOME_DATA)
    }

    @Test
    fun `given a valid not found entry with previous data when resolving in strict mode then it fails`() {
        resolve(snapshot(isNotFound = true), strict = true) shouldBeEqualTo RawCacheResolution.ThrowNotFound
    }

    @Test
    fun `given an expired not found entry when resolving then the request is performed again`() {
        resolve(
                snapshot(isNotFound = true),
                now = A_TIMESTAMP + A_VALIDITY_DURATION,
        ) shouldBeEqualTo RawCacheResolution.Fetch(SOME_DATA)
    }

    @Test
    fun `given an expired not found entry with no data when resolving then the request is performed again`() {
        resolve(
                snapshot(data = "", isNotFound = true),
                now = A_TIMESTAMP + A_VALIDITY_DURATION,
        ) shouldBeEqualTo RawCacheResolution.Fetch(null)
    }
}
