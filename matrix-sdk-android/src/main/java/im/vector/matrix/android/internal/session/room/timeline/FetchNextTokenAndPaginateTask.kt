/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.timeline

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.findIncludingEvent
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.filter.FilterRepository
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface FetchNextTokenAndPaginateTask : Task<FetchNextTokenAndPaginateTask.Params, TokenChunkEventPersistor.Result> {

    data class Params(
            val roomId: String,
            val lastKnownEventId: String,
            val limit: Int
    )
}

internal class DefaultFetchNextTokenAndPaginateTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val monarchy: Monarchy,
        private val filterRepository: FilterRepository,
        private val paginationTask: PaginationTask,
        private val eventBus: EventBus
) : FetchNextTokenAndPaginateTask {

    override suspend fun execute(params: FetchNextTokenAndPaginateTask.Params): TokenChunkEventPersistor.Result {
        val filter = filterRepository.getRoomFilter()
        val response = executeRequest<EventContextResponse>(eventBus) {
            apiCall = roomAPI.getContextOfEvent(params.roomId, params.lastKnownEventId, 0, filter)
        }
        if (response.end == null) {
            throw IllegalStateException("No next token found")
        }
        monarchy.awaitTransaction {
            ChunkEntity.findIncludingEvent(it, params.lastKnownEventId)?.nextToken = response.end
        }
        val paginationParams = PaginationTask.Params(
                roomId = params.roomId,
                from = response.end,
                direction = PaginationDirection.FORWARDS,
                limit = params.limit
        )
        return paginationTask.execute(paginationParams)
    }
}
