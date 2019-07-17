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
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.members.MembershipService
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.session.room.membership.joining.InviteTask
import im.vector.matrix.android.internal.session.room.membership.joining.JoinRoomTask
import im.vector.matrix.android.internal.session.room.membership.leaving.LeaveRoomTask
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.fetchCopied
import javax.inject.Inject

internal class DefaultMembershipService @Inject constructor(private val roomId: String,
                                                            private val monarchy: Monarchy,
                                                            private val loadRoomMembersTask: LoadRoomMembersTask,
                                                            private val inviteTask: InviteTask,
                                                            private val joinTask: JoinRoomTask,
                                                            private val leaveRoomTask: LeaveRoomTask
) : MembershipService {

    override suspend fun loadRoomMembersIfNeeded() {
        loadRoomMembersTask.execute(LoadRoomMembersTask.Params(roomId, Membership.LEAVE))
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

    override suspend fun invite(userId: String) {
        inviteTask.execute(InviteTask.Params(roomId, userId))
    }

    override suspend fun join() {
        joinTask.execute(JoinRoomTask.Params(roomId))
    }

    override suspend fun leave() {
        leaveRoomTask.execute(LeaveRoomTask.Params(roomId))
    }
}