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

package im.vector.matrix.android.internal.session.room.membership

import androidx.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.toModel
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

internal class DefaultMembershipService(private val roomId: String,
                                        private val monarchy: Monarchy,
                                        private val taskExecutor: TaskExecutor,
                                        private val loadRoomMembersTask: LoadRoomMembersTask,
                                        private val inviteTask: InviteTask,
                                        private val joinTask: JoinRoomTask,
                                        private val leaveRoomTask: LeaveRoomTask
) : MembershipService {

    override fun loadRoomMembersIfNeeded(): Cancelable {
        val params = LoadRoomMembersTask.Params(roomId, Membership.LEAVE)
        return loadRoomMembersTask.configureWith(params).executeBy(taskExecutor)
    }

    override fun getRoomMember(userId: String): RoomMember? {
        val eventEntity = monarchy.fetchCopied {
            RoomMembers(it, roomId).queryRoomMemberEvent(userId).findFirst()
        }
        return eventEntity?.asDomain()?.content.toModel()
    }

    override fun getRoomMemberIdsLive(): LiveData<List<String>> {
        return monarchy.findAllMappedWithChanges(
                {
                    RoomMembers(it, roomId).queryRoomMembersEvent()
                },
                {
                    it.stateKey!!
                }
        )
    }

    override fun getNumberOfJoinedMembers(): Int {
        var result = 0
        monarchy.runTransactionSync {
            result = RoomMembers(it, roomId).getNumberOfJoinedMembers()
        }
        return result
    }

    override fun invite(userId: String, callback: MatrixCallback<Unit>) {
        val params = InviteTask.Params(roomId, userId)
        inviteTask.configureWith(params)
                .dispatchTo(callback)
                .executeBy(taskExecutor)
    }

    override fun join(callback: MatrixCallback<Unit>) {
        val params = JoinRoomTask.Params(roomId)
        joinTask.configureWith(params)
                .dispatchTo(callback)
                .executeBy(taskExecutor)
    }

    override fun leave(callback: MatrixCallback<Unit>) {
        val params = LeaveRoomTask.Params(roomId)
        leaveRoomTask.configureWith(params)
                .dispatchTo(callback)
                .executeBy(taskExecutor)
    }
}