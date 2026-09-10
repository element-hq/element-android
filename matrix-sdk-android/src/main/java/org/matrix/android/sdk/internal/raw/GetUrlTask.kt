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

package org.matrix.android.sdk.internal.raw

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.cache.CacheStrategy
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.internal.database.model.RawCacheEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.di.GlobalDatabase
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import java.util.Date
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

internal interface GetUrlTask : Task<GetUrlTask.Params, String> {
    data class Params(
            val url: String,
            val cacheStrategy: CacheStrategy
    )
}

internal class DefaultGetUrlTask @Inject constructor(
        private val rawAPI: RawAPI,
        @GlobalDatabase private val monarchy: Monarchy
) : GetUrlTask {

    override suspend fun execute(params: GetUrlTask.Params): String {
        return when (params.cacheStrategy) {
            CacheStrategy.NoCache -> doRequest(params.url)
            is CacheStrategy.TtlCache -> doRequestWithCache(
                    params.url,
                    params.cacheStrategy.validityDurationInMillis,
                    params.cacheStrategy.strict
            )
            CacheStrategy.InfiniteCache -> doRequestWithCache(
                    params.url,
                    Long.MAX_VALUE,
                    true
            )
        }
    }

    private suspend fun doRequest(url: String): String {
        return executeRequest(null) {
            rawAPI.getUrl(url)
        }
                .string()
    }

    private suspend fun doRequestWithCache(url: String, validityDurationInMillis: Long, strict: Boolean): String {
        // Get data from cache
        var snapshot: RawCacheSnapshot? = null
        monarchy.doWithRealm { realm ->
            snapshot = RawCacheEntity.get(realm, url)?.let {
                RawCacheSnapshot(
                        data = it.data,
                        isNotFound = it.isNotFound,
                        lastUpdatedTimestamp = it.lastUpdatedTimestamp
                )
            }
        }

        val staleData = when (val resolution = resolveRawCache(snapshot, Date().time, validityDurationInMillis, strict)) {
            is RawCacheResolution.Return -> return resolution.data
            // The server already told us this url does not exist, and we have nothing to fall back
            // on. Fail as a live request would have, but without issuing it.
            RawCacheResolution.ThrowNotFound -> throw notFoundFailure()
            is RawCacheResolution.Fetch -> resolution.staleData
        }

        // No cache or outdated cache
        val data = try {
            doRequest(url)
        } catch (throwable: Throwable) {
            if (throwable.isDefinitiveNotFound()) {
                // Remember it, so that we do not hammer the server with a request we know fails.
                // Note that `data` is intentionally left as it is, to keep the stale fallback working.
                monarchy.awaitTransaction { realm ->
                    val rawCacheEntity = RawCacheEntity.getOrCreate(realm, url)
                    rawCacheEntity.isNotFound = true
                    rawCacheEntity.lastUpdatedTimestamp = Date().time
                }
            }
            // In case of error, we can return value from cache even if outdated
            return staleData ?: throw throwable
        }

        // Store cache
        monarchy.awaitTransaction { realm ->
            val rawCacheEntity = RawCacheEntity.getOrCreate(realm, url)
            rawCacheEntity.data = data
            rawCacheEntity.isNotFound = false
            rawCacheEntity.lastUpdatedTimestamp = Date().time
        }

        return data
    }

    private fun notFoundFailure() = Failure.OtherServerError("", HttpsURLConnection.HTTP_NOT_FOUND)
}

/**
 * True when the server gave a definitive answer that the resource does not exist, as opposed to a
 * transient error such as being offline or a 5xx, which must keep being retried.
 *
 * Note that [org.matrix.android.sdk.api.failure.is404] cannot be used here: it only matches a
 * Matrix API error (a [Failure.ServerError] carrying `M_NOT_FOUND`), whereas requesting a plain
 * file returns an HTML error page, which ends up as a [Failure.OtherServerError].
 */
private fun Throwable.isDefinitiveNotFound() = this is Failure.OtherServerError &&
        (httpCode == HttpsURLConnection.HTTP_NOT_FOUND || /* 404 */
                httpCode == HttpsURLConnection.HTTP_GONE) /* 410 */
