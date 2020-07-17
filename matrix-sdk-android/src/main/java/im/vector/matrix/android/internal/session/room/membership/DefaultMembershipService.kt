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
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.matrix.android.api.session.room.members.MembershipService
import im.vector.matrix.android.api.session.room.members.RoomMemberQueryParams
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMemberSummary
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.RoomMemberSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomMemberSummaryEntityFields
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.query.process
import im.vector.matrix.android.internal.session.room.membership.admin.MembershipAdminTask
import im.vector.matrix.android.internal.session.room.membership.joining.InviteTask
import im.vector.matrix.android.internal.session.room.membership.joining.JoinRoomTask
import im.vector.matrix.android.internal.session.room.membership.leaving.LeaveRoomTask
import im.vector.matrix.android.internal.session.room.membership.threepid.InviteThreePidTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.fetchCopied
import io.realm.Realm
import io.realm.RealmQuery

internal class DefaultMembershipService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @SessionDatabase private val monarchy: Monarchy,
        private val taskExecutor: TaskExecutor,
        private val loadRoomMembersTask: LoadRoomMembersTask,
        private val inviteTask: InviteTask,
        private val inviteThreePidTask: InviteThreePidTask,
        private val joinTask: JoinRoomTask,
        private val leaveRoomTask: LeaveRoomTask,
        private val membershipAdminTask: MembershipAdminTask,
        @UserId
        private val userId: String
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

    override fun getRoomMember(userId: String): RoomMemberSummary? {
        val roomMemberEntity = monarchy.fetchCopied {
            RoomMemberHelper(it, roomId).getLastRoomMember(userId)
        }
        return roomMemberEntity?.asDomain()
    }

    override fun getRoomMembers(queryParams: RoomMemberQueryParams): List<RoomMemberSummary> {
        return monarchy.fetchAllMappedSync(
                {
                    roomMembersQuery(it, queryParams)
                },
                {
                    it.asDomain()
                }
        )
    }

    override fun getRoomMembersLive(queryParams: RoomMemberQueryParams): LiveData<List<RoomMemberSummary>> {
        return monarchy.findAllMappedWithChanges(
                {
                    roomMembersQuery(it, queryParams)
                },
                {
                    it.asDomain()
                }
        )
    }

    private fun roomMembersQuery(realm: Realm, queryParams: RoomMemberQueryParams): RealmQuery<RoomMemberSummaryEntity> {
        return RoomMemberHelper(realm, roomId).queryRoomMembersEvent()
                .process(RoomMemberSummaryEntityFields.USER_ID, queryParams.userId)
                .process(RoomMemberSummaryEntityFields.MEMBERSHIP_STR, queryParams.memberships)
                .process(RoomMemberSummaryEntityFields.DISPLAY_NAME, queryParams.displayName)
                .apply {
                    if (queryParams.excludeSelf) {
                        notEqualTo(RoomMemberSummaryEntityFields.USER_ID, userId)
                    }
                }
    }

    override fun getNumberOfJoinedMembers(): Int {
        return Realm.getInstance(monarchy.realmConfiguration).use {
            RoomMemberHelper(it, roomId).getNumberOfJoinedMembers()
        }
    }

    override fun ban(userId: String, reason: String?, callback: MatrixCallback<Unit>): Cancelable {
        val params = MembershipAdminTask.Params(MembershipAdminTask.Type.BAN, roomId, userId, reason)
        return membershipAdminTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun unban(userId: String, reason: String?, callback: MatrixCallback<Unit>): Cancelable {
        val params = MembershipAdminTask.Params(MembershipAdminTask.Type.UNBAN, roomId, userId, reason)
        return membershipAdminTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun kick(userId: String, reason: String?, callback: MatrixCallback<Unit>): Cancelable {
        val params = MembershipAdminTask.Params(MembershipAdminTask.Type.KICK, roomId, userId, reason)
        return membershipAdminTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun invite(userId: String, reason: String?, callback: MatrixCallback<Unit>): Cancelable {
        val params = InviteTask.Params(roomId, userId, reason)
        return inviteTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun invite3pid(threePid: ThreePid, callback: MatrixCallback<Unit>): Cancelable {
        val params = InviteThreePidTask.Params(roomId, threePid)
        return inviteThreePidTask
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
