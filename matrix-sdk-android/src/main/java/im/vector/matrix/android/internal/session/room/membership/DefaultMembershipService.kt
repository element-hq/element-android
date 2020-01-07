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

package im.vector.matrix.android.internal.session.room.membership

import androidx.lifecycle.LiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.room.members.MembershipService
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.session.room.membership.joining.InviteTask
import im.vector.matrix.android.internal.session.room.membership.joining.JoinRoomTask
import im.vector.matrix.android.internal.session.room.membership.leaving.LeaveRoomTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.fetchCopied
import io.realm.Realm

internal class DefaultMembershipService @AssistedInject constructor(@Assisted private val roomId: String,
                                                                    private val monarchy: Monarchy,
                                                                    private val taskExecutor: TaskExecutor,
                                                                    private val loadRoomMembersTask: LoadRoomMembersTask,
                                                                    private val inviteTask: InviteTask,
                                                                    private val joinTask: JoinRoomTask,
                                                                    private val leaveRoomTask: LeaveRoomTask
) : MembershipService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): MembershipService
    }

    override fun loadRoomMembersIfNeeded(matrixCallback: MatrixCallback<Unit>): Cancelable {
        val params = LoadRoomMembersTask.Params(roomId, Membership.LEAVE)
        return loadRoomMembersTask
                .configureWith(params) {
                    this.callback = matrixCallback
                }
                .executeBy(taskExecutor)
    }

    override fun getRoomMember(userId: String): RoomMember? {
        val roomMemberEntity = monarchy.fetchCopied {
            RoomMembers(it, roomId).getLastRoomMember(userId)
        }
        return roomMemberEntity?.asDomain()
    }

    override fun getRoomMembers(memberships: List<Membership>): List<RoomMember> {
        return monarchy.fetchAllMappedSync(
                {
                    RoomMembers(it, roomId).queryRoomMembersEvent()
                },
                {
                    it.asDomain()
                }
        )
    }

    override fun getRoomMembersLive(memberships: List<Membership>): LiveData<List<RoomMember>> {
        return monarchy.findAllMappedWithChanges(
                {
                    RoomMembers(it, roomId).queryRoomMembersEvent()
                },
                {
                    it.asDomain()
                }
        )
    }

    override fun getNumberOfJoinedMembers(): Int {
        return Realm.getInstance(monarchy.realmConfiguration).use {
            RoomMembers(it, roomId).getNumberOfJoinedMembers()
        }
    }

    override fun invite(userId: String, reason: String?, callback: MatrixCallback<Unit>): Cancelable {
        val params = InviteTask.Params(roomId, userId, reason)
        return inviteTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun join(reason: String?, viaServers: List<String>, callback: MatrixCallback<Unit>): Cancelable {
        val params = JoinRoomTask.Params(roomId, reason, viaServers)
        return joinTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun leave(reason: String?, callback: MatrixCallback<Unit>): Cancelable {
        val params = LeaveRoomTask.Params(roomId, reason)
        return leaveRoomTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }
}
