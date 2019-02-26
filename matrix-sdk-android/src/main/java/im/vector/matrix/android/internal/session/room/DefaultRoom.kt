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

package im.vector.matrix.android.internal.session.room

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.read.ReadService
import im.vector.matrix.android.api.session.room.send.SendService
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.members.LoadRoomMembersTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith

internal class DefaultRoom(
        override val roomId: String,
        private val loadRoomMembersTask: LoadRoomMembersTask,
        private val monarchy: Monarchy,
        private val timelineService: TimelineService,
        private val sendService: SendService,
        private val readService: ReadService,
        private val taskExecutor: TaskExecutor


) : Room,
    TimelineService by timelineService,
    SendService by sendService,
    ReadService by readService {

    override val roomSummary: LiveData<RoomSummary> by lazy {
        val liveData = monarchy
                .findAllMappedWithChanges(
                        { realm -> RoomSummaryEntity.where(realm, roomId).isNotEmpty(RoomSummaryEntityFields.DISPLAY_NAME) },
                        { from -> from.asDomain() })

        Transformations.map(liveData) {
            it.first()
        }
    }

    override fun loadRoomMembersIfNeeded(): Cancelable {
        val params = LoadRoomMembersTask.Params(roomId, Membership.LEAVE)
        return loadRoomMembersTask.configureWith(params).executeBy(taskExecutor)
    }
}