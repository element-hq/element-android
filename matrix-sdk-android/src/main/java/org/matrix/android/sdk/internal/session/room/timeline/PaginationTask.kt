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
