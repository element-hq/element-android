/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.raw

import org.matrix.android.sdk.api.cache.CacheStrategy

/**
 * Useful methods to fetch raw data from the server. The access token will not be used to fetched the data
 */
interface RawService {
    /**
     * Get a URL, either from cache or from the remote server, depending on the cache strategy.
     */
    suspend fun getUrl(url: String, cacheStrategy: CacheStrategy): String

    /**
     * Specific case for the well-known file. Cache validity is 8 hours.
     * @param domain the domain to get the .well-known file, for instance "matrix.org".
     * The URL will be "https://{domain}/.well-known/matrix/client"
     */
    suspend fun getWellknown(domain: String): String

    /**
     * Clear all the cache data.
     */
    suspend fun clearCache()
}
