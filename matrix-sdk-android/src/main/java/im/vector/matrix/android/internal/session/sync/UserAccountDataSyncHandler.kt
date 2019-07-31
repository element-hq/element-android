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
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.getDirectRooms
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.membership.RoomMembers
import im.vector.matrix.android.internal.session.sync.model.InvitedRoomSync
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataDirectMessages
import im.vector.matrix.android.internal.session.sync.model.UserAccountDataSync
import im.vector.matrix.android.internal.session.user.accountdata.DirectChatsHelper
import im.vector.matrix.android.internal.session.user.accountdata.UpdateUserAccountDataTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import io.realm.Realm
import timber.log.Timber
import javax.inject.Inject

internal class UserAccountDataSyncHandler @Inject constructor(private val monarchy: Monarchy,
                                                              private val credentials: Credentials,
                                                              private val directChatsHelper: DirectChatsHelper,
                                                              private val updateUserAccountDataTask: UpdateUserAccountDataTask,
                                                              private val taskExecutor: TaskExecutor) {

    fun handle(accountData: UserAccountDataSync?, invites: Map<String, InvitedRoomSync>?) {
        accountData?.list?.forEach {
            when (it) {
                is UserAccountDataDirectMessages -> handleDirectChatRooms(it)
                else                             -> return@forEach
            }
        }
        monarchy.doWithRealm { realm ->
            synchronizeWithServerIfNeeded(realm, invites)
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

    // If we get some direct chat invites, we synchronize the user account data including those.
    private fun synchronizeWithServerIfNeeded(realm: Realm, invites: Map<String, InvitedRoomSync>?) {
        if (invites.isNullOrEmpty()) return
        val directChats = directChatsHelper.getLocalUserAccount()
        var hasUpdate = false
        invites.forEach { (roomId, _) ->
            val myUserStateEvent = RoomMembers(realm, roomId).getStateEvent(credentials.userId)
            val inviterId = myUserStateEvent?.sender
            val myUserRoomMember: RoomMember? = myUserStateEvent?.let { it.asDomain().content?.toModel() }
            val isDirect = myUserRoomMember?.isDirect
            if (inviterId != null && inviterId != credentials.userId && isDirect == true) {
                directChats
                        .getOrPut(inviterId, { arrayListOf() })
                        .apply {
                            if (contains(roomId)) {
                                Timber.v("Direct chats already include room $roomId with user $inviterId")
                            } else {
                                add(roomId)
                                hasUpdate = true
                            }
                        }
            }
        }
        if (hasUpdate) {
            val updateUserAccountParams = UpdateUserAccountDataTask.DirectChatParams(
                    directMessages = directChats
            )
            updateUserAccountDataTask.configureWith(updateUserAccountParams).executeBy(taskExecutor)
        }
    }
}