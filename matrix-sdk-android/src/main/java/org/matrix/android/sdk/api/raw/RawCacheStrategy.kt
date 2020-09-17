/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.api.raw

sealed class RawCacheStrategy {
    // Data is always fetched from the server
    object NoCache: RawCacheStrategy()

    // Once data is retrieved, it is stored for the provided amount of time.
    // In case of error, and if strict is set to false, the cache can be returned if available
    data class TtlCache(val validityDurationInMillis: Long, val strict: Boolean): RawCacheStrategy()

    // Once retrieved, the data is stored in cache and will be always get from the cache
    object InfiniteCache: RawCacheStrategy()
}
