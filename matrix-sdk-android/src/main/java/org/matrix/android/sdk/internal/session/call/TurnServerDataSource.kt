/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
        return cachedTurnServerResponse.data ?: turnServerTask.execute(Unit).also {
            cachedTurnServerResponse.data = it
        }
    }
}
