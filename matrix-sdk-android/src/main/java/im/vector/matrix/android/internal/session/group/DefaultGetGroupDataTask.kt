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

package im.vector.matrix.android.internal.session.group

import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.group.model.GroupRooms
import im.vector.matrix.android.internal.session.group.model.GroupSummaryResponse
import im.vector.matrix.android.internal.session.group.model.GroupUsers
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.GroupSummaryEntity
import im.vector.matrix.sqldelight.session.Memberships
import im.vector.matrix.sqldelight.session.SessionDatabase
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface GetGroupDataTask : Task<GetGroupDataTask.Params, Unit> {

    data class Params(val groupId: String)
}

internal class DefaultGetGroupDataTask @Inject constructor(
        private val groupAPI: GroupAPI,
        private val sessionDatabase: SessionDatabase,
        private val eventBus: EventBus,
        private val coroutineDispatchers: MatrixCoroutineDispatchers
) : GetGroupDataTask {

    override suspend fun execute(params: GetGroupDataTask.Params) {
        val groupId = params.groupId
        val groupSummary = executeRequest<GroupSummaryResponse>(eventBus) {
            apiCall = groupAPI.getSummary(groupId)
        }
        val groupRooms = executeRequest<GroupRooms>(eventBus) {
            apiCall = groupAPI.getRooms(groupId)
        }
        val groupUsers = executeRequest<GroupUsers>(eventBus) {
            apiCall = groupAPI.getUsers(groupId)
        }
        insertInDb(groupSummary, groupRooms, groupUsers, groupId)
    }

    private suspend fun insertInDb(groupSummary: GroupSummaryResponse,
                                   groupRooms: GroupRooms,
                                   groupUsers: GroupUsers,
                                   groupId: String) {
        sessionDatabase
                .awaitTransaction(coroutineDispatchers) {
                    val name = groupSummary.profile?.name
                    val membership = when (groupSummary.user?.membership) {
                        Membership.JOIN.value -> Memberships.JOIN
                        Membership.INVITE.value -> Memberships.INVITE
                        else -> Memberships.LEAVE
                    }

                    val groupSummaryEntity = GroupSummaryEntity.Impl(
                            group_id = groupId,
                            display_name = if (name.isNullOrEmpty()) groupId else name,
                            short_description = groupSummary.profile?.shortDescription ?: "",
                            membership = membership,
                            avatar_url = groupSummary.profile?.avatarUrl ?: ""
                    )
                    it.groupSummaryQueries.insertOrReplaceGroupSummary(groupSummaryEntity)
                    it.groupSummaryQueries.deleteGroupRooms(listOf(groupId))
                    groupRooms.rooms.forEach { groupRoom ->
                        sessionDatabase.groupSummaryQueries.insertGroupRoom(groupId, groupRoom.roomId)
                    }
                    it.groupSummaryQueries.deleteGroupUser(listOf(groupId))
                    groupUsers.users.forEach { groupUser ->
                        sessionDatabase.groupSummaryQueries.insertGroupUser(groupId, groupUser.userId)
                    }
                }
    }
}
