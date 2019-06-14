/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.matrix.android.internal.session.user

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import io.realm.Sort
import javax.inject.Inject

@SessionScope
internal class UserEntityUpdater @Inject constructor(monarchy: Monarchy,
                                                     private val updateUserTask: UpdateUserTask,
                                                     private val taskExecutor: TaskExecutor)
    : RealmLiveEntityObserver<EventEntity>(monarchy) {

    override val query = Monarchy.Query<EventEntity> {
        EventEntity
                .where(it, type = EventType.STATE_ROOM_MEMBER)
                .sort(EventEntityFields.STATE_INDEX, Sort.DESCENDING)
                .distinct(EventEntityFields.STATE_KEY)

    }

    override fun processChanges(inserted: List<EventEntity>, updated: List<EventEntity>, deleted: List<EventEntity>) {
        val roomMembersEvents = inserted.map { it.eventId }
        val taskParams = UpdateUserTask.Params(roomMembersEvents)
        updateUserTask
                .configureWith(taskParams)
                .executeOn(TaskThread.IO)
                .executeBy(taskExecutor)
    }
}