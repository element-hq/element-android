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

package org.matrix.android.sdk.internal.session.media

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.cache.CacheStrategy
import org.matrix.android.sdk.api.session.media.PreviewUrlData
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.database.model.PreviewUrlCacheEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.unescapeHtml
import java.util.Date
import javax.inject.Inject

internal interface GetPreviewUrlTask : Task<GetPreviewUrlTask.Params, PreviewUrlData> {
    data class Params(
            val url: String,
            val timestamp: Long?,
            val cacheStrategy: CacheStrategy
    )
}

internal class DefaultGetPreviewUrlTask @Inject constructor(
        private val mediaAPI: MediaAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        @SessionDatabase private val monarchy: Monarchy
) : GetPreviewUrlTask {

    override suspend fun execute(params: GetPreviewUrlTask.Params): PreviewUrlData {
        return when (params.cacheStrategy) {
            CacheStrategy.NoCache       -> doRequest(params.url, params.timestamp)
            is CacheStrategy.TtlCache   -> doRequestWithCache(
                    params.url,
                    params.timestamp,
                    params.cacheStrategy.validityDurationInMillis,
                    params.cacheStrategy.strict
            )
            CacheStrategy.InfiniteCache -> doRequestWithCache(
                    params.url,
                    params.timestamp,
                    Long.MAX_VALUE,
                    true
            )
        }
    }

    private suspend fun doRequest(url: String, timestamp: Long?): PreviewUrlData {
        return executeRequest(globalErrorReceiver) {
            mediaAPI.getPreviewUrlData(url, timestamp)
        }
                .toPreviewUrlData(url)
    }

    private fun JsonDict.toPreviewUrlData(url: String): PreviewUrlData {
        return PreviewUrlData(
                url = (get("og:url") as? String) ?: url,
                siteName = (get("og:site_name") as? String)?.unescapeHtml(),
                title = (get("og:title") as? String)?.unescapeHtml(),
                description = (get("og:description") as? String)?.unescapeHtml(),
                mxcUrl = get("og:image") as? String,
                imageHeight = (get("og:image:height") as? Double)?.toInt(),
                imageWidth = (get("og:image:width") as? Double)?.toInt(),
        )
    }

    private suspend fun doRequestWithCache(url: String, timestamp: Long?, validityDurationInMillis: Long, strict: Boolean): PreviewUrlData {
        // Get data from cache
        var dataFromCache: PreviewUrlData? = null
        var isCacheValid = false
        monarchy.doWithRealm { realm ->
            val entity = PreviewUrlCacheEntity.get(realm, url)
            dataFromCache = entity?.toDomain()
            isCacheValid = entity != null && Date().time < entity.lastUpdatedTimestamp + validityDurationInMillis
        }

        val finalDataFromCache = dataFromCache
        if (finalDataFromCache != null && isCacheValid) {
            return finalDataFromCache
        }

        // No cache or outdated cache
        val data = try {
            doRequest(url, timestamp)
        } catch (throwable: Throwable) {
            // In case of error, we can return value from cache even if outdated
            return finalDataFromCache
                    ?.takeIf { !strict }
                    ?: throw throwable
        }

        // Store cache
        monarchy.awaitTransaction { realm ->
            val previewUrlCacheEntity = PreviewUrlCacheEntity.getOrCreate(realm, url)
            previewUrlCacheEntity.urlFromServer = data.url
            previewUrlCacheEntity.siteName = data.siteName
            previewUrlCacheEntity.title = data.title
            previewUrlCacheEntity.description = data.description
            previewUrlCacheEntity.mxcUrl = data.mxcUrl
            previewUrlCacheEntity.imageHeight = data.imageHeight
            previewUrlCacheEntity.imageWidth = data.imageWidth
            previewUrlCacheEntity.lastUpdatedTimestamp = Date().time
        }

        return data
    }
}
