/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.RoomService
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.session.room.alias.GetRoomIdByAliasTask
import org.matrix.android.sdk.internal.session.room.create.CreateRoomTask
import org.matrix.android.sdk.internal.session.room.membership.RoomChangeMembershipStateDataSource
import org.matrix.android.sdk.internal.session.room.membership.joining.JoinRoomTask
import org.matrix.android.sdk.internal.session.room.read.MarkAllRoomsReadTask
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryDataSource
import org.matrix.android.sdk.internal.session.user.accountdata.UpdateBreadcrumbsTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import javax.inject.Inject

internal class DefaultRoomService @Inject constructor(
        private val createRoomTask: CreateRoomTask,
        private val joinRoomTask: JoinRoomTask,
        private val markAllRoomsReadTask: MarkAllRoomsReadTask,
        private val updateBreadcrumbsTask: UpdateBreadcrumbsTask,
        private val roomIdByAliasTask: GetRoomIdByAliasTask,
        private val roomGetter: RoomGetter,
        private val roomSummaryDataSource: RoomSummaryDataSource,
        private val roomChangeMembershipStateDataSource: RoomChangeMembershipStateDataSource,
        private val taskExecutor: TaskExecutor
) : RoomService {

    override fun createRoom(createRoomParams: CreateRoomParams, callback: MatrixCallback<String>): Cancelable {
        return createRoomTask
                .configureWith(createRoomParams) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun getRoom(roomId: String): Room? {
        return roomGetter.getRoom(roomId)
    }

    override fun getExistingDirectRoomWithUser(otherUserId: String): Room? {
        return roomGetter.getDirectRoomWith(otherUserId)
    }

    override fun getRoomSummary(roomIdOrAlias: String): RoomSummary? {
        return roomSummaryDataSource.getRoomSummary(roomIdOrAlias)
    }

    override fun getRoomSummaries(queryParams: RoomSummaryQueryParams): List<RoomSummary> {
        return roomSummaryDataSource.getRoomSummaries(queryParams)
    }

    override fun getRoomSummariesLive(queryParams: RoomSummaryQueryParams): LiveData<List<RoomSummary>> {
        return roomSummaryDataSource.getRoomSummariesLive(queryParams)
    }

    override fun getBreadcrumbs(queryParams: RoomSummaryQueryParams): List<RoomSummary> {
        return roomSummaryDataSource.getBreadcrumbs(queryParams)
    }

    override fun getBreadcrumbsLive(queryParams: RoomSummaryQueryParams): LiveData<List<RoomSummary>> {
        return roomSummaryDataSource.getBreadcrumbsLive(queryParams)
    }

    override fun onRoomDisplayed(roomId: String): Cancelable {
        return updateBreadcrumbsTask
                .configureWith(UpdateBreadcrumbsTask.Params(roomId))
                .executeBy(taskExecutor)
    }

    override fun joinRoom(roomIdOrAlias: String, reason: String?, viaServers: List<String>, callback: MatrixCallback<Unit>): Cancelable {
        return joinRoomTask
                .configureWith(JoinRoomTask.Params(roomIdOrAlias, reason, viaServers)) {
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

    override fun getRoomIdByAlias(roomAlias: String, searchOnServer: Boolean, callback: MatrixCallback<Optional<String>>): Cancelable {
        return roomIdByAliasTask
                .configureWith(GetRoomIdByAliasTask.Params(roomAlias, searchOnServer)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun getChangeMembershipsLive(): LiveData<Map<String, ChangeMembershipState>> {
        return roomChangeMembershipStateDataSource.getLiveStates()
    }
}
