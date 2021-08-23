/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.raw

import org.matrix.android.sdk.api.cache.CacheStrategy

/**
 * Useful methods to fetch raw data from the server. The access token will not be used to fetched the data
 */
interface RawService {
    /**
     * Get a URL, either from cache or from the remote server, depending on the cache strategy
     */
    suspend fun getUrl(url: String, cacheStrategy: CacheStrategy): String

    /**
     * Specific case for the well-known file. Cache validity is 8 hours
     * @param domain the domain to get the .well-known file, for instance "matrix.org".
     * The URL will be "https://{domain}/.well-known/matrix/client"
     */
    suspend fun getWellknown(domain: String): String

    /**
     * Clear all the cache data
     */
    suspend fun clearCache()
}
