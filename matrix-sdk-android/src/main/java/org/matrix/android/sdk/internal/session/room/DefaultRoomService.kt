/*
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
import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.identity.model.SignInvitationResult
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.RoomService
import org.matrix.android.sdk.api.session.room.RoomSortOrder
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.UpdatableLivePageResult
import org.matrix.android.sdk.api.session.room.alias.RoomAliasDescription
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.peeking.PeekResult
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.summary.RoomAggregateNotificationCount
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.alias.DeleteRoomAliasTask
import org.matrix.android.sdk.internal.session.room.alias.GetRoomIdByAliasTask
import org.matrix.android.sdk.internal.session.room.create.CreateRoomTask
import org.matrix.android.sdk.internal.session.room.membership.RoomChangeMembershipStateDataSource
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.session.room.membership.joining.JoinRoomTask
import org.matrix.android.sdk.internal.session.room.membership.leaving.LeaveRoomTask
import org.matrix.android.sdk.internal.session.room.peeking.PeekRoomTask
import org.matrix.android.sdk.internal.session.room.peeking.ResolveRoomStateTask
import org.matrix.android.sdk.internal.session.room.read.MarkAllRoomsReadTask
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryDataSource
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryUpdater
import org.matrix.android.sdk.internal.session.user.accountdata.UpdateBreadcrumbsTask
import org.matrix.android.sdk.internal.util.fetchCopied
import javax.inject.Inject

internal class DefaultRoomService @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val createRoomTask: CreateRoomTask,
        private val joinRoomTask: JoinRoomTask,
        private val markAllRoomsReadTask: MarkAllRoomsReadTask,
        private val updateBreadcrumbsTask: UpdateBreadcrumbsTask,
        private val roomIdByAliasTask: GetRoomIdByAliasTask,
        private val deleteRoomAliasTask: DeleteRoomAliasTask,
        private val resolveRoomStateTask: ResolveRoomStateTask,
        private val peekRoomTask: PeekRoomTask,
        private val roomGetter: RoomGetter,
        private val roomSummaryDataSource: RoomSummaryDataSource,
        private val roomChangeMembershipStateDataSource: RoomChangeMembershipStateDataSource,
        private val leaveRoomTask: LeaveRoomTask,
        private val roomSummaryUpdater: RoomSummaryUpdater
) : RoomService {

    override suspend fun createRoom(createRoomParams: CreateRoomParams): String {
        return createRoomTask.executeRetry(createRoomParams, 3)
    }

    override fun getRoom(roomId: String): Room? {
        return roomGetter.getRoom(roomId)
    }

    override fun getExistingDirectRoomWithUser(otherUserId: String): String? {
        return roomGetter.getDirectRoomWith(otherUserId)
    }

    override fun getRoomSummary(roomIdOrAlias: String): RoomSummary? {
        return roomSummaryDataSource.getRoomSummary(roomIdOrAlias)
    }

    override fun getRoomSummaries(queryParams: RoomSummaryQueryParams,
                                  sortOrder: RoomSortOrder): List<RoomSummary> {
        return roomSummaryDataSource.getRoomSummaries(queryParams, sortOrder)
    }

    override fun refreshJoinedRoomSummaryPreviews(roomId: String?) {
        val roomSummaries = getRoomSummaries(roomSummaryQueryParams {
            if (roomId != null) {
                this.roomId = QueryStringValue.Equals(roomId)
            }
            memberships = listOf(Membership.JOIN)
        })

        if (roomSummaries.isNotEmpty()) {
            monarchy.runTransactionSync { realm ->
                roomSummaries.forEach {
                    roomSummaryUpdater.refreshLatestPreviewContent(realm, it.roomId)
                }
            }
        }
    }

    override fun getRoomSummariesLive(queryParams: RoomSummaryQueryParams,
                                      sortOrder: RoomSortOrder): LiveData<List<RoomSummary>> {
        return roomSummaryDataSource.getRoomSummariesLive(queryParams, sortOrder)
    }

    override fun getPagedRoomSummariesLive(queryParams: RoomSummaryQueryParams,
                                           pagedListConfig: PagedList.Config,
                                           sortOrder: RoomSortOrder): LiveData<PagedList<RoomSummary>> {
        return roomSummaryDataSource.getSortedPagedRoomSummariesLive(queryParams, pagedListConfig, sortOrder)
    }

    override fun getFilteredPagedRoomSummariesLive(queryParams: RoomSummaryQueryParams,
                                                   pagedListConfig: PagedList.Config,
                                                   sortOrder: RoomSortOrder): UpdatableLivePageResult {
        return roomSummaryDataSource.getUpdatablePagedRoomSummariesLive(queryParams, pagedListConfig, sortOrder)
    }

    override fun getRoomCountLive(queryParams: RoomSummaryQueryParams): LiveData<Int> {
        return roomSummaryDataSource.getCountLive(queryParams)
    }

    override fun getNotificationCountForRooms(queryParams: RoomSummaryQueryParams): RoomAggregateNotificationCount {
        return roomSummaryDataSource.getNotificationCountForRooms(queryParams)
    }

    override fun getBreadcrumbs(queryParams: RoomSummaryQueryParams): List<RoomSummary> {
        return roomSummaryDataSource.getBreadcrumbs(queryParams)
    }

    override fun getBreadcrumbsLive(queryParams: RoomSummaryQueryParams): LiveData<List<RoomSummary>> {
        return roomSummaryDataSource.getBreadcrumbsLive(queryParams)
    }

    override suspend fun onRoomDisplayed(roomId: String) {
        updateBreadcrumbsTask.execute(UpdateBreadcrumbsTask.Params(roomId))
    }

    override suspend fun joinRoom(roomIdOrAlias: String, reason: String?, viaServers: List<String>) {
        joinRoomTask.execute(JoinRoomTask.Params(roomIdOrAlias, reason, viaServers))
    }

    override suspend fun joinRoom(roomId: String,
                                  reason: String?,
                                  thirdPartySigned: SignInvitationResult) {
        joinRoomTask.execute(JoinRoomTask.Params(roomId, reason, thirdPartySigned = thirdPartySigned))
    }

    override suspend fun leaveRoom(roomId: String, reason: String?) {
        leaveRoomTask.execute(LeaveRoomTask.Params(roomId, reason))
    }

    override suspend fun markAllAsRead(roomIds: List<String>) {
        markAllRoomsReadTask.execute(MarkAllRoomsReadTask.Params(roomIds))
    }

    override suspend fun getRoomIdByAlias(roomAlias: String, searchOnServer: Boolean): Optional<RoomAliasDescription> {
        return roomIdByAliasTask.execute(GetRoomIdByAliasTask.Params(roomAlias, searchOnServer))
    }

    override suspend fun deleteRoomAlias(roomAlias: String) {
        deleteRoomAliasTask.execute(DeleteRoomAliasTask.Params(roomAlias))
    }

    override fun getChangeMemberships(roomIdOrAlias: String): ChangeMembershipState {
        return roomChangeMembershipStateDataSource.getState(roomIdOrAlias)
    }

    override fun getChangeMembershipsLive(): LiveData<Map<String, ChangeMembershipState>> {
        return roomChangeMembershipStateDataSource.getLiveStates()
    }

    override fun getRoomMember(userId: String, roomId: String): RoomMemberSummary? {
        val roomMemberEntity = monarchy.fetchCopied {
            RoomMemberHelper(it, roomId).getLastRoomMember(userId)
        }
        return roomMemberEntity?.asDomain()
    }

    override fun getRoomMemberLive(userId: String, roomId: String): LiveData<Optional<RoomMemberSummary>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm ->
                    RoomMemberHelper(realm, roomId).queryRoomMembersEvent()
                            .equalTo(RoomMemberSummaryEntityFields.USER_ID, userId)
                },
                { it.asDomain() }
        )
        return Transformations.map(liveData) { results ->
            results.firstOrNull().toOptional()
        }
    }

    override suspend fun getRoomState(roomId: String): List<Event> {
        return resolveRoomStateTask.execute(ResolveRoomStateTask.Params(roomId))
    }

    override suspend fun peekRoom(roomIdOrAlias: String): PeekResult {
        return peekRoomTask.execute(PeekRoomTask.Params(roomIdOrAlias))
    }

    override fun getFlattenRoomSummaryChildrenOf(spaceId: String?, memberships: List<Membership>): List<RoomSummary> {
        if (spaceId == null) {
            return roomSummaryDataSource.getFlattenOrphanRooms()
        }
        return roomSummaryDataSource.getAllRoomSummaryChildOf(spaceId, memberships)
    }

    override fun getFlattenRoomSummaryChildrenOfLive(spaceId: String?, memberships: List<Membership>): LiveData<List<RoomSummary>> {
        if (spaceId == null) {
            return roomSummaryDataSource.getFlattenOrphanRoomsLive()
        }
        return roomSummaryDataSource.getAllRoomSummaryChildOfLive(spaceId, memberships)
    }
}
