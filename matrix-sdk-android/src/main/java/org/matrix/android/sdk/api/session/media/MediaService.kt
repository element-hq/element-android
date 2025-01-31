/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
     * Get Url Preview data from the homeserver, or from cache, depending on the cache strategy.
     * @param url The url to get the preview data from
     * @param timestamp The optional timestamp. Note that this parameter is not taken into account
     * if the data is already in cache and the cache strategy allow to use it
     * @param cacheStrategy the cache strategy, see the type for more details
     */
    suspend fun getPreviewUrl(url: String, timestamp: Long?, cacheStrategy: CacheStrategy): PreviewUrlData

    /**
     * Clear the cache of all retrieved UrlPreview data.
     */
    suspend fun clearCache()
}
