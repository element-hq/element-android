/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.media

import org.matrix.android.sdk.api.cache.CacheStrategy
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.JsonDict

interface MediaService {
    /**
     * Extract URLs from a TimelineEvent.
     * @param event TimelineEvent to extract the URL from.
     * @return the list of URLs contains in the body of the TimelineEvent. It does not mean that URLs in this list have UrlPreview data
     */
    fun extractUrls(event: TimelineEvent): List<String>

    /**
     * Get Raw Url Preview data from the homeserver. There is no cache management for this request
     * @param url The url to get the preview data from
     * @param timestamp The optional timestamp
     */
    suspend fun getRawPreviewUrl(url: String, timestamp: Long?): JsonDict

    /**
     * Get Url Preview data from the homeserver, or from cache, depending on the cache strategy
     * @param url The url to get the preview data from
     * @param timestamp The optional timestamp. Note that this parameter is not taken into account
     * if the data is already in cache and the cache strategy allow to use it
     * @param cacheStrategy the cache strategy, see the type for more details
     */
    suspend fun getPreviewUrl(url: String, timestamp: Long?, cacheStrategy: CacheStrategy): PreviewUrlData

    /**
     * Clear the cache of all retrieved UrlPreview data
     */
    suspend fun clearCache()
}
