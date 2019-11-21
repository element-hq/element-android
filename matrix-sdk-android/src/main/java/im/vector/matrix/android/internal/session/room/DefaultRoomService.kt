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
import im.vector.matrix.android.internal.session.room.alias.RoomAliasDescription
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.VersioningState
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.mapper.RoomSummaryMapper
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.alias.GetRoomIdByAliasTask
import im.vector.matrix.android.internal.session.room.create.CreateRoomTask
import im.vector.matrix.android.internal.session.room.membership.joining.JoinRoomTask
import im.vector.matrix.android.internal.session.room.read.MarkAllRoomsReadTask
import im.vector.matrix.android.internal.session.user.accountdata.UpdateBreadcrumbsTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import io.realm.Realm
import javax.inject.Inject

internal class DefaultRoomService @Inject constructor(private val monarchy: Monarchy,
                                                      private val roomSummaryMapper: RoomSummaryMapper,
                                                      private val createRoomTask: CreateRoomTask,
                                                      private val joinRoomTask: JoinRoomTask,
                                                      private val markAllRoomsReadTask: MarkAllRoomsReadTask,
                                                      private val updateBreadcrumbsTask: UpdateBreadcrumbsTask,
                                                      private val roomIdByAliasTask: GetRoomIdByAliasTask,
                                                      private val roomFactory: RoomFactory,
                                                      private val taskExecutor: TaskExecutor) : RoomService {


    override fun createRoom(createRoomParams: CreateRoomParams, callback: MatrixCallback<String>): Cancelable {
        return createRoomTask
                .configureWith(createRoomParams) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun getRoom(roomId: String): Room? {
        return Realm.getInstance(monarchy.realmConfiguration).use {
            if (RoomEntity.where(it, roomId).findFirst() != null) {
                roomFactory.create(roomId)
            } else {
                null
            }
        }
    }

    override fun liveRoomSummaries(): LiveData<List<RoomSummary>> {
        return monarchy.findAllMappedWithChanges(
                { realm ->
                    RoomSummaryEntity.where(realm)
                            .isNotEmpty(RoomSummaryEntityFields.DISPLAY_NAME)
                            .notEqualTo(RoomSummaryEntityFields.VERSIONING_STATE_STR, VersioningState.UPGRADED_ROOM_JOINED.name)
                },
                { roomSummaryMapper.map(it) }
        )
    }

    override fun liveBreadcrumbs(): LiveData<List<RoomSummary>> {
        return monarchy.findAllMappedWithChanges(
                { realm ->
                    RoomSummaryEntity.where(realm)
                            .isNotEmpty(RoomSummaryEntityFields.DISPLAY_NAME)
                            .notEqualTo(RoomSummaryEntityFields.VERSIONING_STATE_STR, VersioningState.UPGRADED_ROOM_JOINED.name)
                            .greaterThan(RoomSummaryEntityFields.BREADCRUMBS_INDEX, RoomSummaryEntity.NOT_IN_BREADCRUMBS)
                            .sort(RoomSummaryEntityFields.BREADCRUMBS_INDEX)
                },
                { roomSummaryMapper.map(it) }
        )
    }

    override fun onRoomDisplayed(roomId: String): Cancelable {
        return updateBreadcrumbsTask
                .configureWith(UpdateBreadcrumbsTask.Params(roomId))
                .executeBy(taskExecutor)
    }

    override fun joinRoom(roomId: String, reason: String?, viaServers: List<String>, callback: MatrixCallback<Unit>): Cancelable {
        return joinRoomTask
                .configureWith(JoinRoomTask.Params(roomId, reason, viaServers)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun markAllAsRead(roomIds: List<String>, callback: MatrixCallback<Unit>): Cancelable {
        return markAllRoomsReadTask
                .configureWith(MarkAllRoomsReadTask.Params(roomIds)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun getRoomIdByAlias(roomAlias: String, callback: MatrixCallback<String?>): Cancelable {
        return roomIdByAliasTask
                .configureWith(GetRoomIdByAliasTask.Params(roomAlias)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }
}
