/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.timeline

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.query.findIncludingEvent
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.filter.FilterRepository
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal interface FetchTokenAndPaginateTask : Task<FetchTokenAndPaginateTask.Params, TokenChunkEventPersistor.Result> {

    data class Params(
            val roomId: String,
            val lastKnownEventId: String,
            val direction: PaginationDirection,
            val limit: Int
    )
}

internal class DefaultFetchTokenAndPaginateTask @Inject constructor(
        private val roomAPI: RoomAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val filterRepository: FilterRepository,
        private val paginationTask: PaginationTask,
        private val globalErrorReceiver: GlobalErrorReceiver
) : FetchTokenAndPaginateTask {

    override suspend fun execute(params: FetchTokenAndPaginateTask.Params): TokenChunkEventPersistor.Result {
        val filter = filterRepository.getRoomFilter()
        val response = executeRequest(globalErrorReceiver) {
            roomAPI.getContextOfEvent(params.roomId, params.lastKnownEventId, 0, filter)
        }
        val fromToken = if (params.direction == PaginationDirection.FORWARDS) {
            response.end
        } else {
            response.start
        }
                ?: throw IllegalStateException("No token found")

        monarchy.awaitTransaction { realm ->
            val chunkToUpdate = ChunkEntity.findIncludingEvent(realm, params.lastKnownEventId)
            if (params.direction == PaginationDirection.FORWARDS) {
                chunkToUpdate?.nextToken = fromToken
            } else {
                chunkToUpdate?.prevToken = fromToken
            }
        }
        val paginationParams = PaginationTask.Params(
                roomId = params.roomId,
                from = fromToken,
                direction = params.direction,
                limit = params.limit
        )
        return paginationTask.execute(paginationParams)
    }
}
