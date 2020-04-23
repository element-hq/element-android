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

import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.internal.database.helper.saveBreadcrumbs
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.pushers.PushRulePersistor
import im.vector.matrix.android.internal.session.room.membership.RoomMemberHelper
import im.vector.matrix.android.internal.session.sync.model.InvitedRoomSync
import im.vector.matrix.android.internal.session.sync.model.accountdata.*
import im.vector.matrix.android.internal.session.user.accountdata.DirectChatsHelper
import im.vector.matrix.android.internal.session.user.accountdata.UpdateUserAccountDataTask
import im.vector.matrix.sqldelight.session.SessionDatabase
import timber.log.Timber
import javax.inject.Inject

internal class UserAccountDataSyncHandler @Inject constructor(
        private val sessionDatabase: SessionDatabase,
        @UserId private val userId: String,
        private val directChatsHelper: DirectChatsHelper,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        private val pushRulesPersistor: PushRulePersistor) {

    fun handle(accountData: UserAccountDataSync?) {
        accountData?.list?.forEach {
            // Generic handling, just save in base
            handleGenericAccountData(it.type, it.content)

            // Didn't want to break too much thing, so i re-serialize to jsonString before reparsing
            // TODO would be better to have a mapper?
            val toJson = MoshiProvider.providesMoshi().adapter(Event::class.java).toJson(it)
            val model = toJson?.let { json ->
                MoshiProvider.providesMoshi().adapter(UserAccountData::class.java).fromJson(json)
            }
            // Specific parsing
            when (model) {
                is UserAccountDataDirectMessages -> handleDirectChatRooms(model)
                is UserAccountDataPushRules -> handlePushRules(model)
                is UserAccountDataIgnoredUsers -> handleIgnoredUsers(model)
                is UserAccountDataBreadcrumbs -> handleBreadcrumbs(model)
            }
        }
    }


    // If we get some direct chat invites, we synchronize the user account data including those.
    suspend fun synchronizeWithServerIfNeeded(invites: Map<String, InvitedRoomSync>) {
        if (invites.isNullOrEmpty()) return
        val directChats = directChatsHelper.getLocalUserAccount()
        var hasUpdate = false
        invites.forEach { (roomId, _) ->
            val myUserStateEvent = RoomMemberHelper(sessionDatabase, roomId).getLastStateEvent(userId)
            val inviterId = myUserStateEvent?.sender_id
            val myUserRoomMember: RoomMemberContent? = myUserStateEvent?.let { it.asDomain().content?.toModel() }
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
            updateUserAccountDataTask.execute(updateUserAccountParams)
        }
    }

    private fun handlePushRules(userAccountDataPushRules: UserAccountDataPushRules) {
        val pushRules = userAccountDataPushRules.content
        pushRulesPersistor.persist(pushRules)
    }

    private fun handleDirectChatRooms(directMessages: UserAccountDataDirectMessages) {
        sessionDatabase.roomSummaryQueries.unsetDirectForAllRooms()
        directMessages.content.forEach {
            val userId = it.key
            it.value.forEach { roomId ->
                sessionDatabase.roomSummaryQueries.setDirectForRoom(userId, roomId)
            }
        }
    }

    private fun handleIgnoredUsers(userAccountDataIgnoredUsers: UserAccountDataIgnoredUsers) {
        val userIds = userAccountDataIgnoredUsers.content.ignoredUsers.keys
        sessionDatabase.userQueries.deleteAllIgnoredUsers()
        // And save the new received list
        userIds.forEach {
            sessionDatabase.userQueries.insertIgnored(it)
        }
        // TODO If not initial sync, we should execute a init sync
    }

    private fun handleBreadcrumbs(userAccountDataBreadcrumbs: UserAccountDataBreadcrumbs) {
        val recentRoomIds = userAccountDataBreadcrumbs.content.recentRoomIds
        sessionDatabase.breadcrumbsQueries.saveBreadcrumbs(recentRoomIds)
    }

    private fun handleGenericAccountData(type: String, content: Content?) {
        val contentStr = ContentMapper.map(content)
        sessionDatabase.userAccountDataQueries.insert(type, contentStr)
    }

}
