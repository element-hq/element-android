/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.cache

sealed class CacheStrategy {
    /**
     * Data is always fetched from the server.
     */
    object NoCache : CacheStrategy()

    /**
     * Once data is retrieved, it is stored for the provided amount of time.
     * In case of error, and if strict is set to false, the cache can be returned if available
     */
    data class TtlCache(val validityDurationInMillis: Long, val strict: Boolean) : CacheStrategy()

    /**
     * Once retrieved, the data is stored in cache and will be always get from the cache.
     */
    object InfiniteCache : CacheStrategy()
}
