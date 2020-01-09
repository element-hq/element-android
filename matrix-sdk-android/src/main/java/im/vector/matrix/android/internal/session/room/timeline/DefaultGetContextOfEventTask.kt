/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.timeline

import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.filter.FilterRepository
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface GetContextOfEventTask : Task<GetContextOfEventTask.Params, TokenChunkEventPersistor.Result> {

    data class Params(
            val roomId: String,
            val eventId: String,
            val limit: Int
    )
}

internal class DefaultGetContextOfEventTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val filterRepository: FilterRepository,
        private val tokenChunkEventPersistor: TokenChunkEventPersistor,
        private val eventBus: EventBus
) : GetContextOfEventTask {

    override suspend fun execute(params: GetContextOfEventTask.Params): TokenChunkEventPersistor.Result {
        val filter = filterRepository.getRoomFilter()
        val response = executeRequest<EventContextResponse>(eventBus) {
            apiCall = roomAPI.getContextOfEvent(params.roomId, params.eventId, params.limit, filter)
        }
        return tokenChunkEventPersistor.insertInDb(response, params.roomId, PaginationDirection.BACKWARDS)
    }
}
