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
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.getDirectRooms
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.pushers.SavePushRulesTask
import im.vector.matrix.android.internal.session.room.membership.RoomMembers
import im.vector.matrix.android.internal.session.sync.model.InvitedRoomSync
import im.vector.matrix.android.internal.session.sync.model.accountdata.*
import im.vector.matrix.android.internal.session.user.accountdata.DirectChatsHelper
import im.vector.matrix.android.internal.session.user.accountdata.SaveIgnoredUsersTask
import im.vector.matrix.android.internal.session.user.accountdata.UpdateUserAccountDataTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.awaitTransaction
import io.realm.Realm
import timber.log.Timber
import javax.inject.Inject

internal class UserAccountDataSyncHandler @Inject constructor(private val monarchy: Monarchy,
                                                              @UserId private val userId: String,
                                                              private val directChatsHelper: DirectChatsHelper,
                                                              private val updateUserAccountDataTask: UpdateUserAccountDataTask,
                                                              private val savePushRulesTask: SavePushRulesTask,
                                                              private val saveIgnoredUsersTask: SaveIgnoredUsersTask,
                                                              private val taskExecutor: TaskExecutor) {

    suspend fun handle(accountData: UserAccountDataSync?, invites: Map<String, InvitedRoomSync>?) {
        accountData?.list?.forEach {
            when (it) {
                is UserAccountDataDirectMessages -> handleDirectChatRooms(it)
                is UserAccountDataPushRules      -> handlePushRules(it)
                is UserAccountDataIgnoredUsers   -> handleIgnoredUsers(it)
                is UserAccountDataFallback       -> Timber.d("Receive account data of unhandled type ${it.type}")
                else                             -> error("Missing code here!")
            }
        }

        // TODO Store all account data, app can be interested of it
        // accountData?.list?.forEach {
        //     it.toString()
        //     MoshiProvider.providesMoshi()
        // }

        monarchy.doWithRealm { realm ->
            synchronizeWithServerIfNeeded(realm, invites)
        }
    }

    private suspend fun handlePushRules(userAccountDataPushRules: UserAccountDataPushRules) {
        savePushRulesTask.execute(SavePushRulesTask.Params(userAccountDataPushRules.content))
    }

    private suspend fun handleDirectChatRooms(directMessages: UserAccountDataDirectMessages) {
        monarchy.awaitTransaction { realm ->
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
            val myUserStateEvent = RoomMembers(realm, roomId).getStateEvent(userId)
            val inviterId = myUserStateEvent?.sender
            val myUserRoomMember: RoomMember? = myUserStateEvent?.let { it.asDomain().content?.toModel() }
            val isDirect = myUserRoomMember?.isDirect
            if (inviterId != null && inviterId != userId && isDirect == true) {
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

    private fun handleIgnoredUsers(userAccountDataIgnoredUsers: UserAccountDataIgnoredUsers) {
        saveIgnoredUsersTask.configureWith(userAccountDataIgnoredUsers).executeBy(taskExecutor)
        // TODO If not initial sync, we should execute a init sync
    }
}
