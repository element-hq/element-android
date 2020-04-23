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

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.state.StateService
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.database.repository.CurrentStateEventDataSource
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import kotlinx.coroutines.flow.Flow

internal class DefaultStateService @AssistedInject constructor(@Assisted private val roomId: String,
                                                               private val taskExecutor: TaskExecutor,
                                                               private val sendStateTask: SendStateTask,
                                                               private val currentStateEventDataSource: CurrentStateEventDataSource
) : StateService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): StateService
    }

    override fun getStateEvent(eventType: String, stateKey: String): Event? {
        return currentStateEventDataSource.getCurrentMapped(
                roomId = roomId,
                type = eventType,
                stateKey = stateKey
        )
    }

    override fun getStateEventLive(eventType: String, stateKey: String): Flow<Optional<Event>> {
        return currentStateEventDataSource.getCurrentLiveMapped(
                roomId = roomId,
                type = eventType,
                stateKey = stateKey
        )
    }

    override fun updateTopic(topic: String, callback: MatrixCallback<Unit>) {
        val params = SendStateTask.Params(roomId,
                                          EventType.STATE_ROOM_TOPIC,
                                          mapOf(
                                                  "topic" to topic
                                          ))

        sendStateTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }
}
