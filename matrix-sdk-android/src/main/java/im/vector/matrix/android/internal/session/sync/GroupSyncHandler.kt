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

package im.vector.matrix.android.internal.session.sync

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.sync.model.GroupsSyncResponse
import im.vector.matrix.android.internal.session.sync.model.InvitedGroupSync
import io.realm.Realm


internal class GroupSyncHandler(private val monarchy: Monarchy) {

    sealed class HandlingStrategy {
        data class JOINED(val data: Map<String, Any>) : HandlingStrategy()
        data class INVITED(val data: Map<String, InvitedGroupSync>) : HandlingStrategy()
        data class LEFT(val data: Map<String, Any>) : HandlingStrategy()
    }

    fun handle(roomsSyncResponse: GroupsSyncResponse) {
        monarchy.runTransactionSync { realm ->
            handleGroupSync(realm, GroupSyncHandler.HandlingStrategy.JOINED(roomsSyncResponse.join))
            handleGroupSync(realm, GroupSyncHandler.HandlingStrategy.INVITED(roomsSyncResponse.invite))
            handleGroupSync(realm, GroupSyncHandler.HandlingStrategy.LEFT(roomsSyncResponse.leave))
        }
    }

    // PRIVATE METHODS *****************************************************************************

    private fun handleGroupSync(realm: Realm, handlingStrategy: HandlingStrategy) {
        val groups = when (handlingStrategy) {
            is HandlingStrategy.JOINED  -> handlingStrategy.data.map { handleJoinedGroup(realm, it.key) }
            is HandlingStrategy.INVITED -> handlingStrategy.data.map { handleInvitedGroup(realm, it.key) }
            is HandlingStrategy.LEFT    -> handlingStrategy.data.map { handleLeftGroup(realm, it.key) }
        }
        realm.insertOrUpdate(groups)
    }

    private fun handleJoinedGroup(realm: Realm,
                                  groupId: String): GroupEntity {

        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        groupEntity.membership = MyMembership.JOINED
        return groupEntity
    }

    private fun handleInvitedGroup(realm: Realm,
                                   groupId: String): GroupEntity {

        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        groupEntity.membership = MyMembership.INVITED
        return groupEntity

    }

    // TODO : handle it
    private fun handleLeftGroup(realm: Realm,
                                groupId: String): GroupEntity {

        val groupEntity = GroupEntity.where(realm, groupId).findFirst() ?: GroupEntity(groupId)
        groupEntity.membership = MyMembership.LEFT
        return groupEntity
    }


}