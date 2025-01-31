/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.raw

import org.matrix.android.sdk.api.cache.CacheStrategy
import org.matrix.android.sdk.api.raw.RawService
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class DefaultRawService @Inject constructor(
        private val getUrlTask: GetUrlTask,
        private val cleanRawCacheTask: CleanRawCacheTask
) : RawService {
    override suspend fun getUrl(url: String, cacheStrategy: CacheStrategy): String {
        return getUrlTask.execute(GetUrlTask.Params(url, cacheStrategy))
    }

    override suspend fun getWellknown(domain: String): String {
        return getUrl(
                "https://$domain/.well-known/matrix/client",
                CacheStrategy.TtlCache(TimeUnit.HOURS.toMillis(8), false)
        )
    }

    override suspend fun clearCache() {
        cleanRawCacheTask.execute(Unit)
    }
}
