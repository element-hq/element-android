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

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.state.StateService
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith

internal class DefaultStateService(private val roomId: String,
                                   private val taskExecutor: TaskExecutor,
                                   private val sendStateTask: SendStateTask) : StateService {

    override fun updateTopic(topic: String, callback: MatrixCallback<Unit>) {
        val params = SendStateTask.Params(roomId,
                EventType.STATE_ROOM_TOPIC,
                HashMap<String, String>().apply {
                    put("topic", topic)
                })


        sendStateTask.configureWith(params)
                .dispatchTo(callback)
                .executeBy(taskExecutor)
    }
}