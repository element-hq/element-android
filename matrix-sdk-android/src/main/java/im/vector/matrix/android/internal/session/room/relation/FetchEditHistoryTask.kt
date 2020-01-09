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
package im.vector.matrix.android.internal.session.room.relation

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.RelationType
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface FetchEditHistoryTask : Task<FetchEditHistoryTask.Params, List<Event>> {

    data class Params(
            val roomId: String,
            val isRoomEncrypted: Boolean,
            val eventId: String
    )
}

internal class DefaultFetchEditHistoryTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val eventBus: EventBus
) : FetchEditHistoryTask {

    override suspend fun execute(params: FetchEditHistoryTask.Params): List<Event> {
        val response = executeRequest<RelationsResponse>(eventBus) {
            apiCall = roomAPI.getRelations(params.roomId,
                    params.eventId,
                    RelationType.REPLACE,
                    if (params.isRoomEncrypted) EventType.ENCRYPTED else EventType.MESSAGE)
        }

        val events = response.chunks.toMutableList()
        response.originalEvent?.let { events.add(it) }
        return events
    }
}
