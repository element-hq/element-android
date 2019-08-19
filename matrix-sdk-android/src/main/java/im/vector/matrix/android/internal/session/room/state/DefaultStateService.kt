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
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.prev
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import io.realm.Realm
import io.realm.RealmConfiguration

internal class DefaultStateService @AssistedInject constructor(@Assisted private val roomId: String,
                                                               @SessionDatabase
                                                               private val realmConfiguration: RealmConfiguration,
                                                               private val taskExecutor: TaskExecutor,
                                                               private val sendStateTask: SendStateTask
) : StateService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): StateService
    }

    override fun getStateEvent(eventType: String): Event? {
        return Realm.getInstance(realmConfiguration).use { realm ->
            EventEntity.where(realm, roomId, eventType).prev()?.asDomain()
        }
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