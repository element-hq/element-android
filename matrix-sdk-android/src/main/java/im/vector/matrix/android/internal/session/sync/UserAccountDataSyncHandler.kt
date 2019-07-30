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

package im.vector.matrix.android.internal.session.sync

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import im.vector.matrix.android.internal.database.query.getDirectRooms
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataDirectMessages
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataSync
import javax.inject.Inject

internal class UserAccountDataSyncHandler @Inject constructor(private val monarchy: Monarchy) {

    fun handle(accountData: UserAccountDataSync) {
        accountData.list.forEach {
            when (it) {
                is UserAccountDataDirectMessages -> handleDirectChatRooms(it)
                else                             -> return@forEach
            }
        }
    }

    private fun handleDirectChatRooms(directMessages: UserAccountDataDirectMessages) {
        monarchy.runTransactionSync { realm ->

            val oldDirectRooms = RoomSummaryEntity.getDirectRooms(realm)
            oldDirectRooms.forEach {
                it.isDirect = false
                it.directUserId = null
            }
            directMessages.content.forEach {
                val userId = it.key
                it.value.forEach { roomId ->
                    val roomSummaryEntity = RoomSummaryEntity.where(realm, roomId).findFirst()
                    if (roomSummaryEntity != null) {
                        roomSummaryEntity.isDirect = true
                        roomSummaryEntity.directUserId = userId
                        realm.insertOrUpdate(roomSummaryEntity)
                    }
                }
            }
        }
    }
}