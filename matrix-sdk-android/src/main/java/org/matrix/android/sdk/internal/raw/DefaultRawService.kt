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

package org.matrix.android.sdk.internal.raw

import org.matrix.android.sdk.api.raw.RawCacheStrategy
import org.matrix.android.sdk.api.raw.RawService
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class DefaultRawService @Inject constructor(
        private val getUrlTask: GetUrlTask,
        private val cleanRawCacheTask: CleanRawCacheTask
) : RawService {
    override suspend fun getUrl(url: String, rawCacheStrategy: RawCacheStrategy): String {
        return getUrlTask.execute(GetUrlTask.Params(url, rawCacheStrategy))
    }

    override suspend fun getWellknown(userId: String): String {
        val homeServerDomain = userId.substringAfter(":")
        return getUrl(
                "https://$homeServerDomain/.well-known/matrix/client",
                RawCacheStrategy.TtlCache(TimeUnit.HOURS.toMillis(8), false)
        )
    }

    override suspend fun clearCache() {
        cleanRawCacheTask.execute(Unit)
    }
}
