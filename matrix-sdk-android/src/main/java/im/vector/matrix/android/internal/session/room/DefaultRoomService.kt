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
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.internal.database.mapper.RoomSummaryMapper
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.create.CreateRoomTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.fetchManaged
import javax.inject.Inject

internal class DefaultRoomService @Inject constructor(private val monarchy: Monarchy,
                                                      private val roomSummaryMapper: RoomSummaryMapper,
                                                      private val createRoomTask: CreateRoomTask,
                                                      private val roomFactory: RoomFactory,
                                                      private val taskExecutor: TaskExecutor) : RoomService {

    override fun createRoom(createRoomParams: CreateRoomParams, callback: MatrixCallback<String>) {
        createRoomTask
                .configureWith(createRoomParams)
                .dispatchTo(callback)
                .executeBy(taskExecutor)
    }

    override fun getRoom(roomId: String): Room? {
        monarchy.fetchManaged { RoomEntity.where(it, roomId).findFirst() } ?: return null
        return roomFactory.create(roomId)
    }

    override fun liveRoomSummaries(): LiveData<List<RoomSummary>> {
        return monarchy.findAllMappedWithChanges(
                { realm -> RoomSummaryEntity.where(realm).isNotEmpty(RoomSummaryEntityFields.DISPLAY_NAME) },
                { roomSummaryMapper.map(it) }
        )
    }
}