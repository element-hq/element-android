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
import androidx.lifecycle.asLiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.members.MembershipService
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.helper.findLatestSessionInfo
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.RoomMembersLoadStatusType
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.query.QueryStringValueProcessor
import org.matrix.android.sdk.internal.query.process
import org.matrix.android.sdk.internal.session.room.RoomDataSource
import org.matrix.android.sdk.internal.session.room.membership.admin.MembershipAdminTask
import org.matrix.android.sdk.internal.session.room.membership.joining.InviteTask
import org.matrix.android.sdk.internal.session.room.membership.threepid.InviteThreePidTask

internal class DefaultMembershipService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @SessionDatabase private val realmInstance: RealmInstance,
        private val loadRoomMembersTask: LoadRoomMembersTask,
        private val inviteTask: InviteTask,
        private val inviteThreePidTask: InviteThreePidTask,
        private val membershipAdminTask: MembershipAdminTask,
        private val roomDataSource: RoomDataSource,
        private val cryptoService: CryptoService,
        @UserId
        private val userId: String,
        private val queryStringValueProcessor: QueryStringValueProcessor
) : MembershipService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultMembershipService
    }

    override suspend fun loadRoomMembersIfNeeded() {
        val params = LoadRoomMembersTask.Params(roomId, excludeMembership = Membership.LEAVE)
        loadRoomMembersTask.execute(params)
    }

    override suspend fun areAllMembersLoaded(): Boolean {
        val status = roomDataSource.getRoomMembersLoadStatus(roomId)
        return status == RoomMembersLoadStatusType.LOADED
    }

    override fun areAllMembersLoadedLive(): LiveData<Boolean> {
        return roomDataSource.getRoomMembersLoadStatusLive(roomId)
    }

    override fun getRoomMember(userId: String): RoomMemberSummary? {
        val realm = realmInstance.getBlockingRealm()
        val roomMemberEntity = RoomMemberHelper(realm, roomId).getLastRoomMember(userId)
        return roomMemberEntity?.asDomain()
    }

    override fun getRoomMembers(queryParams: RoomMemberQueryParams): List<RoomMemberSummary> {
        val realm = realmInstance.getBlockingRealm()
        return roomMembersQuery(realm, queryParams).find()
                .map(this::mapRoomMember)
    }

    override fun getRoomMembersLive(queryParams: RoomMemberQueryParams): LiveData<List<RoomMemberSummary>> {
        return realmInstance.queryList(this::mapRoomMember) {
            roomMembersQuery(it, queryParams)
        }.asLiveData()
    }

    private fun mapRoomMember(roomMemberSummaryEntity: RoomMemberSummaryEntity): RoomMemberSummary {
        return roomMemberSummaryEntity.asDomain()
    }

    private fun roomMembersQuery(realm: TypedRealm, queryParams: RoomMemberQueryParams): RealmQuery<RoomMemberSummaryEntity> {
        return with(queryStringValueProcessor) {
            RoomMemberHelper(realm, roomId).queryRoomMembersEvent()
                    .process(RoomMemberSummaryEntityFields.USER_ID, queryParams.userId)
                    .process(RoomMemberSummaryEntityFields.MEMBERSHIP_STR, queryParams.memberships)
                    .process(RoomMemberSummaryEntityFields.DISPLAY_NAME, queryParams.displayName)
                    .apply {
                        if (queryParams.excludeSelf) {
                            query("userId != $0", userId)
                        }
                    }
        }
    }

    override fun getNumberOfJoinedMembers(): Int {
        val realm = realmInstance.getBlockingRealm()
        return RoomMemberHelper(realm, roomId).getNumberOfJoinedMembers()
    }

    override suspend fun ban(userId: String, reason: String?) {
        val params = MembershipAdminTask.Params(MembershipAdminTask.Type.BAN, roomId, userId, reason)
        membershipAdminTask.execute(params)
    }

    override suspend fun unban(userId: String, reason: String?) {
        val params = MembershipAdminTask.Params(MembershipAdminTask.Type.UNBAN, roomId, userId, reason)
        membershipAdminTask.execute(params)
    }

    override suspend fun remove(userId: String, reason: String?) {
        val params = MembershipAdminTask.Params(MembershipAdminTask.Type.KICK, roomId, userId, reason)
        membershipAdminTask.execute(params)
    }

    override suspend fun invite(userId: String, reason: String?) {
        sendShareHistoryKeysIfNeeded(userId)
        val params = InviteTask.Params(roomId, userId, reason)
        inviteTask.execute(params)
    }

    private suspend fun sendShareHistoryKeysIfNeeded(userId: String) {
        if (!cryptoService.isShareKeysOnInviteEnabled()) return
        // TODO not sure it's the right way to get the latest messages in a room
        val realm = realmInstance.getRealm()
        val sessionInfo = ChunkEntity.findLatestSessionInfo(realm, roomId)
        cryptoService.sendSharedHistoryKeys(roomId, userId, sessionInfo)
    }

    override suspend fun invite3pid(threePid: ThreePid) {
        val params = InviteThreePidTask.Params(roomId, threePid)
        return inviteThreePidTask.execute(params)
    }
}
