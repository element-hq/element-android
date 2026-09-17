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

/**
 * Realm-free view of a RawCacheEntity, so that the cache decision can be unit tested.
 */
internal data class RawCacheSnapshot(
        val data: String,
        val isNotFound: Boolean,
        val lastUpdatedTimestamp: Long,
)

/**
 * What to do with a cache entry, as decided by [resolveRawCache].
 */
internal sealed interface RawCacheResolution {
    /**
     * Return this value straight away, no request needed.
     */
    data class Return(val data: String) : RawCacheResolution

    /**
     * The url is known to be missing on the server, and there is no usable value to fall back on.
     * Fail like a live request would have, without issuing it.
     */
    object ThrowNotFound : RawCacheResolution

    /**
     * Perform the request. [staleData], when not null, is what to return if the request fails.
     */
    data class Fetch(val staleData: String?) : RawCacheResolution
}

/**
 * Decide whether a request has to be performed, given what is currently in cache.
 *
 * A cached "not found" behaves exactly like a live 404 would: the stale value is returned when the
 * caller tolerates it, otherwise the call fails. The only difference is that no request is issued.
 *
 * @param snapshot the cached entry, or null if there is none.
 * @param now current epoch time in milliseconds.
 * @param validityDurationInMillis how long a cached entry, positive or negative, stays valid.
 * @param strict when false, an outdated or missing value can be replaced by a stale cached one.
 */
internal fun resolveRawCache(
        snapshot: RawCacheSnapshot?,
        now: Long,
        validityDurationInMillis: Long,
        strict: Boolean,
): RawCacheResolution {
    snapshot ?: return RawCacheResolution.Fetch(null)

    // A stale value is only usable by a caller which does not require fresh data.
    val staleData = snapshot.data.takeIf { !strict && it.isNotEmpty() }
    val isCacheValid = now < snapshot.lastUpdatedTimestamp + validityDurationInMillis

    return when {
        !isCacheValid -> RawCacheResolution.Fetch(staleData)
        !snapshot.isNotFound -> RawCacheResolution.Return(snapshot.data)
        staleData != null -> RawCacheResolution.Return(staleData)
        else -> RawCacheResolution.ThrowNotFound
    }
}
