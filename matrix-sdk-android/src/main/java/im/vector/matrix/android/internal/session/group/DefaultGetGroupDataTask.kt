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

import arrow.core.Try
import arrow.core.fix
import arrow.instances.`try`.monad.monad
import arrow.typeclasses.binding
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.database.model.GroupSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.group.model.GroupRooms
import im.vector.matrix.android.internal.session.group.model.GroupSummaryResponse
import im.vector.matrix.android.internal.session.group.model.GroupUsers
import im.vector.matrix.android.internal.util.tryTransactionSync
import io.realm.kotlin.createObject
import javax.inject.Inject

internal interface GetGroupDataTask : Task<GetGroupDataTask.Params, Unit> {

    data class Params(val groupId: String)

}

internal class DefaultGetGroupDataTask @Inject constructor(
        private val groupAPI: GroupAPI,
        private val monarchy: Monarchy
) : GetGroupDataTask {

    override suspend fun execute(params: GetGroupDataTask.Params): Try<Unit> {
        val groupId = params.groupId
        return Try.monad().binding {

            val groupSummary = executeRequest<GroupSummaryResponse> {
                apiCall = groupAPI.getSummary(groupId)
            }.bind()

            val groupRooms = executeRequest<GroupRooms> {
                apiCall = groupAPI.getRooms(groupId)
            }.bind()

            val groupUsers = executeRequest<GroupUsers> {
                apiCall = groupAPI.getUsers(groupId)
            }.bind()

            insertInDb(groupSummary, groupRooms, groupUsers, groupId).bind()
        }.fix()
    }


    private fun insertInDb(groupSummary: GroupSummaryResponse,
                           groupRooms: GroupRooms,
                           groupUsers: GroupUsers,
                           groupId: String): Try<Unit> {
        return monarchy
                .tryTransactionSync { realm ->
                    val groupSummaryEntity = GroupSummaryEntity.where(realm, groupId).findFirst()
                                             ?: realm.createObject(groupId)

                    groupSummaryEntity.avatarUrl = groupSummary.profile?.avatarUrl ?: ""
                    val name = groupSummary.profile?.name
                    groupSummaryEntity.displayName = if (name.isNullOrEmpty()) groupId else name
                    groupSummaryEntity.shortDescription = groupSummary.profile?.shortDescription ?: ""

                    val roomIds = groupRooms.rooms.map { it.roomId }
                    groupSummaryEntity.roomIds.clear()
                    groupSummaryEntity.roomIds.addAll(roomIds)

                    val userIds = groupUsers.users.map { it.userId }
                    groupSummaryEntity.userIds.clear()
                    groupSummaryEntity.userIds.addAll(userIds)

                }
    }


}