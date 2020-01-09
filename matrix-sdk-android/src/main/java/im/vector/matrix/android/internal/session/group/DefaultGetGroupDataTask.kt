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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.group.model.GroupRooms
import im.vector.matrix.android.internal.session.group.model.GroupSummaryResponse
import im.vector.matrix.android.internal.session.group.model.GroupUsers
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface GetGroupDataTask : Task<GetGroupDataTask.Params, Unit> {

    data class Params(val groupId: String)
}

internal class DefaultGetGroupDataTask @Inject constructor(
        private val groupAPI: GroupAPI,
        private val monarchy: Monarchy,
        private val eventBus: EventBus
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

    private fun insertInDb(groupSummary: GroupSummaryResponse,
                           groupRooms: GroupRooms,
                           groupUsers: GroupUsers,
                           groupId: String) {
        monarchy
                .writeAsync { realm ->
                    val groupSummaryEntity = GroupSummaryEntity.where(realm, groupId).findFirst()
                            ?: realm.createObject(GroupSummaryEntity::class.java, groupId)

                    groupSummaryEntity.avatarUrl = groupSummary.profile?.avatarUrl ?: ""
                    val name = groupSummary.profile?.name
                    groupSummaryEntity.displayName = if (name.isNullOrEmpty()) groupId else name
                    groupSummaryEntity.shortDescription = groupSummary.profile?.shortDescription ?: ""

                    groupSummaryEntity.roomIds.clear()
                    groupRooms.rooms.mapTo(groupSummaryEntity.roomIds) { it.roomId }

                    groupSummaryEntity.userIds.clear()
                    groupUsers.users.mapTo(groupSummaryEntity.userIds) { it.userId }

                    groupSummaryEntity.membership = when (groupSummary.user?.membership) {
                        Membership.JOIN.value   -> Membership.JOIN
                        Membership.INVITE.value -> Membership.INVITE
                        else                    -> Membership.LEAVE
                    }
                }
    }
}
