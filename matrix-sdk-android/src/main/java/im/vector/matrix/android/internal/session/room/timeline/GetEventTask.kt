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

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

// TODO Add parent task

internal class GetEventTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val eventBus: EventBus
) : Task<GetEventTask.Params, Event> {

    internal data class Params(
            val roomId: String,
            val eventId: String
    )

    override suspend fun execute(params: Params): Event {
        return executeRequest(eventBus) {
            apiCall = roomAPI.getEvent(params.roomId, params.eventId)
        }
    }
}
