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

package im.vector.matrix.android.internal.session.room.uploads

import im.vector.matrix.android.api.session.room.uploads.GetUploadsResult
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.filter.FilterFactory
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.session.room.timeline.PaginationResponse
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface GetUploadsTask : Task<GetUploadsTask.Params, GetUploadsResult> {

    data class Params(
            val roomId: String,
            val numberOfEvents: Int,
            val since: String?
    )
}

internal class DefaultGetUploadsTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val tokenStore: SyncTokenStore,
        private val eventBus: EventBus)
    : GetUploadsTask {

    override suspend fun execute(params: GetUploadsTask.Params): GetUploadsResult {
        val since = params.since ?: tokenStore.getLastToken() ?: throw IllegalStateException("No token available")

        val filter = FilterFactory.createUploadsFilter(params.numberOfEvents).toJSONString()
        val chunk = executeRequest<PaginationResponse>(eventBus) {
            apiCall = roomAPI.getRoomMessagesFrom(params.roomId, since, PaginationDirection.BACKWARDS.value, params.numberOfEvents, filter)
        }

        return GetUploadsResult(
                events = chunk.events, // reverse?
                nextToken = chunk.end?.takeIf { it != chunk.start }
        )
    }
}
