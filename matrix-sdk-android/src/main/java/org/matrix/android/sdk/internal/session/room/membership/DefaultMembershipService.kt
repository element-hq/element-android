/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.membership

import androidx.lifecycle.LiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.members.MembershipService
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.query.process
import org.matrix.android.sdk.internal.session.room.membership.admin.MembershipAdminTask
import org.matrix.android.sdk.internal.session.room.membership.joining.InviteTask
import org.matrix.android.sdk.internal.session.room.membership.joining.JoinRoomTask
import org.matrix.android.sdk.internal.session.room.membership.leaving.LeaveRoomTask
import org.matrix.android.sdk.internal.session.room.membership.threepid.InviteThreePidTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.util.fetchCopied
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

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultMembershipService
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
