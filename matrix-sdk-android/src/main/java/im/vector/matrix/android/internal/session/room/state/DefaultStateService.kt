/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.state

import androidx.lifecycle.LiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.state.StateService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith

internal class DefaultStateService @AssistedInject constructor(@Assisted private val roomId: String,
                                                               private val stateEventDataSource: StateEventDataSource,
                                                               private val taskExecutor: TaskExecutor,
                                                               private val sendStateTask: SendStateTask
) : StateService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): StateService
    }

    override fun getStateEvent(eventType: String, stateKey: QueryStringValue): Event? {
        return stateEventDataSource.getStateEvent(roomId, eventType, stateKey)
    }

    override fun getStateEventLive(eventType: String, stateKey: QueryStringValue): LiveData<Optional<Event>> {
        return stateEventDataSource.getStateEventLive(roomId, eventType, stateKey)
    }

    override fun getStateEvents(eventTypes: Set<String>, stateKey: QueryStringValue): List<Event> {
        return stateEventDataSource.getStateEvents(roomId, eventTypes, stateKey)
    }

    override fun getStateEventsLive(eventTypes: Set<String>, stateKey: QueryStringValue): LiveData<List<Event>> {
        return stateEventDataSource.getStateEventsLive(roomId, eventTypes, stateKey)
    }

    override fun sendStateEvent(
            eventType: String,
            stateKey: String?,
            body: JsonDict,
            callback: MatrixCallback<Unit>
    ): Cancelable {
        val params = SendStateTask.Params(
                roomId = roomId,
                stateKey = stateKey,
                eventType = eventType,
                body = body
        )
        return sendStateTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun updateTopic(topic: String, callback: MatrixCallback<Unit>): Cancelable {
        return sendStateEvent(
                eventType = EventType.STATE_ROOM_TOPIC,
                body = mapOf("topic" to topic),
                callback = callback,
                stateKey = null
        )
    }
}
