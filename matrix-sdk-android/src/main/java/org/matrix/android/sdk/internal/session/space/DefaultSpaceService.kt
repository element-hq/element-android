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

package org.matrix.android.sdk.internal.session.space

import androidx.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.space.CreateSpaceParams
import org.matrix.android.sdk.api.session.space.Space
import org.matrix.android.sdk.api.session.space.SpaceService
import org.matrix.android.sdk.api.session.space.SpaceSummary
import org.matrix.android.sdk.api.session.space.SpaceSummaryQueryParams
import org.matrix.android.sdk.api.session.space.model.SpaceChildContent
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.RoomGetter
import org.matrix.android.sdk.internal.session.room.alias.DeleteRoomAliasTask
import org.matrix.android.sdk.internal.session.room.alias.GetRoomIdByAliasTask
import org.matrix.android.sdk.internal.session.room.create.CreateRoomTask
import org.matrix.android.sdk.internal.session.room.membership.RoomChangeMembershipStateDataSource
import org.matrix.android.sdk.internal.session.room.membership.joining.JoinRoomTask
import org.matrix.android.sdk.internal.session.room.membership.leaving.LeaveRoomTask
import org.matrix.android.sdk.internal.session.room.read.MarkAllRoomsReadTask
import org.matrix.android.sdk.internal.session.space.peeking.PeekSpaceTask
import org.matrix.android.sdk.internal.session.space.peeking.SpacePeekResult
import org.matrix.android.sdk.internal.session.user.accountdata.UpdateBreadcrumbsTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import javax.inject.Inject

internal class DefaultSpaceService @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val createRoomTask: CreateRoomTask,
        private val joinRoomTask: JoinRoomTask,
        private val joinSpaceTask: JoinSpaceTask,
        private val markAllRoomsReadTask: MarkAllRoomsReadTask,
        private val updateBreadcrumbsTask: UpdateBreadcrumbsTask,
        private val roomIdByAliasTask: GetRoomIdByAliasTask,
        private val deleteRoomAliasTask: DeleteRoomAliasTask,
        private val roomGetter: RoomGetter,
        private val spaceSummaryDataSource: SpaceSummaryDataSource,
        private val peekSpaceTask: PeekSpaceTask,
        private val resolveSpaceInfoTask: ResolveSpaceInfoTask,
        private val leaveRoomTask: LeaveRoomTask,
        private val roomChangeMembershipStateDataSource: RoomChangeMembershipStateDataSource,
        private val taskExecutor: TaskExecutor
) : SpaceService {

    override suspend fun createSpace(params: CreateSpaceParams): String {
        return createRoomTask.execute(params)
    }

    override fun getSpace(spaceId: String): Space? {
        return roomGetter.getRoom(spaceId)
                ?.takeIf { it.roomSummary()?.roomType == RoomType.SPACE }
                ?.let { DefaultSpace(it) }
    }

    override fun getSpaceSummariesLive(queryParams: SpaceSummaryQueryParams): LiveData<List<SpaceSummary>> {
        return spaceSummaryDataSource.getRoomSummariesLive(queryParams)
    }

    override fun getSpaceSummaries(spaceSummaryQueryParams: SpaceSummaryQueryParams): List<SpaceSummary> {
        return spaceSummaryDataSource.getSpaceSummaries(spaceSummaryQueryParams)
    }

    override suspend fun peekSpace(spaceId: String): SpacePeekResult {
        return peekSpaceTask.execute(PeekSpaceTask.Params(spaceId))
    }

    override suspend fun querySpaceChildren(spaceId: String): Pair<RoomSummary, List<SpaceChildInfo>> {
        return resolveSpaceInfoTask.execute(ResolveSpaceInfoTask.Params.withId(spaceId)).let { response ->
            val spaceDesc = response.rooms?.firstOrNull { it.roomId == spaceId }
            Pair(
                    first = RoomSummary(
                            roomId = spaceDesc?.roomId ?: spaceId,
                            roomType = spaceDesc?.roomType,
                            name = spaceDesc?.name ?: "",
                            displayName = spaceDesc?.name ?: "",
                            topic = spaceDesc?.topic ?: "",
                            joinedMembersCount = spaceDesc?.numJoinedMembers,
                            avatarUrl = spaceDesc?.avatarUrl ?: "",
                            encryptionEventTs = null,
                            typingUsers = emptyList(),
                            isEncrypted = false
                    ),
                    second = response.rooms
                            ?.filter { it.roomId != spaceId }
                            ?.map { childSummary ->
                                val childStateEv = response.events
                                        ?.firstOrNull { it.stateKey == childSummary.roomId && it.type == EventType.STATE_SPACE_CHILD }
                                        ?.content.toModel<SpaceChildContent>()
                                SpaceChildInfo(
                                        roomSummary = RoomSummary(
                                                roomId = childSummary.roomId,
                                                roomType = childSummary.roomType,
                                                name = childSummary.name ?: "",
                                                displayName = childSummary.name ?: "",
                                                topic = childSummary.topic ?: "",
                                                joinedMembersCount = childSummary.numJoinedMembers,
                                                avatarUrl = childSummary.avatarUrl ?: "",
                                                encryptionEventTs = null,
                                                typingUsers = emptyList(),
                                                isEncrypted = false
                                        ),
                                        order = childStateEv?.order,
                                        present = childStateEv?.present ?: false,
                                        autoJoin = childStateEv?.default ?: false,
                                        viaServers = childStateEv?.via ?: emptyList()
                                )
                            } ?: emptyList()
            )
        }
    }

    override suspend fun joinSpace(spaceIdOrAlias: String,
                                   reason: String?,
                                   viaServers: List<String>,
                                   autoJoinChild: List<SpaceService.ChildAutoJoinInfo>): SpaceService.JoinSpaceResult {
        try {
            joinSpaceTask.execute(JoinSpaceTask.Params(spaceIdOrAlias, reason, viaServers))
            // TODO partial success
            return SpaceService.JoinSpaceResult.Success
//            val childJoinFailures = mutableMapOf<String, Throwable>()
//            autoJoinChild.forEach { info ->
//                // TODO what if the child is it self a subspace with some default children?
//                try {
//                    joinRoomTask.execute(JoinRoomTask.Params(info.roomIdOrAlias, null, info.viaServers))
//                } catch (failure: Throwable) {
//                    // TODO, i could already be a member of this room, handle that as it should not be an error in this context
//                    childJoinFailures[info.roomIdOrAlias] = failure
//                }
//            }
//            return if (childJoinFailures.isEmpty()) {
//                SpaceService.JoinSpaceResult.Success
//            } else {
//                SpaceService.JoinSpaceResult.PartialSuccess(childJoinFailures)
//            }
        } catch (throwable: Throwable) {
            return SpaceService.JoinSpaceResult.Fail(throwable)
        }
    }

    override suspend fun rejectInvite(spaceId: String, reason: String?) {
        leaveRoomTask.execute(LeaveRoomTask.Params(spaceId, reason))
    }
}
