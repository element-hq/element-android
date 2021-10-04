/*
 * Copyright (c) 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.call

import android.os.SystemClock
import org.matrix.android.sdk.api.session.call.TurnServerResponse
import javax.inject.Inject

internal class TurnServerDataSource @Inject constructor(private val turnServerTask: GetTurnServerTask) {

    private val cachedTurnServerResponse = object {
        // Keep one minute safe to avoid considering the data is valid and then actually it is not when effectively using it.
        private val MIN_TTL = 60

        private val now = { SystemClock.elapsedRealtime() / 1000 }

        private var expiresAt: Long = 0

        var data: TurnServerResponse? = null
            get() = if (expiresAt > now()) field else null
            set(value) {
                expiresAt = now() + (value?.ttl ?: 0) - MIN_TTL
                field = value
            }
    }

    suspend fun getTurnServer(): TurnServerResponse {
        return cachedTurnServerResponse.data ?: turnServerTask.execute(GetTurnServerTask.Params).also {
            cachedTurnServerResponse.data = it
        }
    }
}
