/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.timeline

import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.filter.FilterRepository
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface PaginationTask : Task<PaginationTask.Params, TokenChunkEventPersistor.Result> {

    data class Params(
            val roomId: String,
            val from: String,
            val direction: PaginationDirection,
            val limit: Int
    )
}

internal class DefaultPaginationTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val filterRepository: FilterRepository,
        private val tokenChunkEventPersistor: TokenChunkEventPersistor,
        private val globalErrorReceiver: GlobalErrorReceiver
) : PaginationTask {

    override suspend fun execute(params: PaginationTask.Params): TokenChunkEventPersistor.Result {
        val filter = filterRepository.getRoomFilter()
        val chunk = executeRequest(
                globalErrorReceiver,
                canRetry = true
        ) {
            roomAPI.getRoomMessagesFrom(params.roomId, params.from, params.direction.value, params.limit, filter)
        }
        return tokenChunkEventPersistor.insertInDb(chunk, params.roomId, params.direction)
    }
}
