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

package org.matrix.android.sdk.internal.session.sync.handler

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.realmListOf
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.failure.InitialSyncRequestReason
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.pushrules.RuleScope
import org.matrix.android.sdk.api.session.pushrules.RuleSetKey
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.sync.model.InvitedRoomSync
import org.matrix.android.sdk.api.session.sync.model.UserAccountDataSync
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.database.mapper.PushRulesMapper
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.BreadcrumbsEntity
import org.matrix.android.sdk.internal.database.model.IgnoredUserEntity
import org.matrix.android.sdk.internal.database.model.PushRulesEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.UserAccountDataEntity
import org.matrix.android.sdk.internal.database.query.findAllFrom
import org.matrix.android.sdk.internal.database.query.getDirectRooms
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.SessionListeners
import org.matrix.android.sdk.internal.session.dispatchTo
import org.matrix.android.sdk.internal.session.pushers.GetPushRulesResponse
import org.matrix.android.sdk.internal.session.room.RoomAvatarResolver
import org.matrix.android.sdk.internal.session.room.membership.RoomDisplayNameResolver
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.session.sync.model.accountdata.BreadcrumbsContent
import org.matrix.android.sdk.internal.session.sync.model.accountdata.DirectMessagesContent
import org.matrix.android.sdk.internal.session.sync.model.accountdata.IgnoredUsersContent
import org.matrix.android.sdk.internal.session.sync.model.accountdata.toMutable
import org.matrix.android.sdk.internal.session.user.accountdata.DirectChatsHelper
import org.matrix.android.sdk.internal.session.user.accountdata.UpdateUserAccountDataTask
import timber.log.Timber
import javax.inject.Inject

internal class UserAccountDataSyncHandler @Inject constructor(
        @SessionDatabase private val realmInstance: RealmInstance,
        @UserId private val userId: String,
        private val directChatsHelper: DirectChatsHelper,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        private val roomAvatarResolver: RoomAvatarResolver,
        private val roomDisplayNameResolver: RoomDisplayNameResolver,
        @SessionId private val sessionId: String,
        private val sessionManager: SessionManager,
        private val sessionListeners: SessionListeners
) {

    fun handle(realm: MutableRealm, accountData: UserAccountDataSync?) {
        accountData?.list?.forEach { event ->
            // Generic handling, just save in base
            handleGenericAccountData(realm, event.type, event.content)
            when (event.type) {
                UserAccountDataTypes.TYPE_DIRECT_MESSAGES -> handleDirectChatRooms(realm, event)
                UserAccountDataTypes.TYPE_PUSH_RULES -> handlePushRules(realm, event)
                UserAccountDataTypes.TYPE_IGNORED_USER_LIST -> handleIgnoredUsers(realm, event)
                UserAccountDataTypes.TYPE_BREADCRUMBS -> handleBreadcrumbs(realm, event)
            }
        }
    }

    // If we get some direct chat invites, we synchronize the user account data including those.
    suspend fun synchronizeWithServerIfNeeded(invites: Map<String, InvitedRoomSync>) {
        if (invites.isNullOrEmpty()) return
        val directChats = directChatsHelper.getLocalDirectMessages().toMutable()
        var hasUpdate = false
        val realm = realmInstance.getRealm()
        invites.forEach { (roomId, _) ->
            val myUserStateEvent = RoomMemberHelper(realm, roomId).getLastStateEvent(userId)
            val inviterId = myUserStateEvent?.sender
            val myUserRoomMember: RoomMemberContent? = myUserStateEvent?.let { it.asDomain().content?.toModel() }
            val isDirect = myUserRoomMember?.isDirect
            if (inviterId != null && inviterId != userId && isDirect == true) {
                directChats
                        .getOrPut(inviterId) { arrayListOf() }
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

    private fun handlePushRules(realm: MutableRealm, event: UserAccountDataEvent) {
        val pushRules = event.content.toModel<GetPushRulesResponse>() ?: return
        realm
                .query(PushRulesEntity::class)
                .find()
                .forEach {
                    //it.deleteOnCascade()
                }

        // Save only global rules for the moment
        val globalRules = pushRules.global

        val content = PushRulesEntity().apply {
            scope = RuleScope.GLOBAL
            kind = RuleSetKey.CONTENT
        }
        globalRules.content?.forEach { rule ->
            content.pushRules.add(PushRulesMapper.map(rule))
        }
        realm.copyToRealm(content, updatePolicy = UpdatePolicy.ALL)

        val override = PushRulesEntity().apply {
            scope = RuleScope.GLOBAL
            kind = RuleSetKey.OVERRIDE
        }
        globalRules.override?.forEach { rule ->
            PushRulesMapper.map(rule).also {
                override.pushRules.add(it)
            }
        }
        realm.copyToRealm(override, updatePolicy = UpdatePolicy.ALL)

        val rooms = PushRulesEntity().apply {
            scope = RuleScope.GLOBAL
            kind = RuleSetKey.ROOM
        }
        globalRules.room?.forEach { rule ->
            rooms.pushRules.add(PushRulesMapper.map(rule))
        }
        realm.copyToRealm(rooms, updatePolicy = UpdatePolicy.ALL)

        val senders = PushRulesEntity().apply {
            scope = RuleScope.GLOBAL
            kind = RuleSetKey.SENDER
        }
        globalRules.sender?.forEach { rule ->
            senders.pushRules.add(PushRulesMapper.map(rule))
        }
        realm.copyToRealm(senders, updatePolicy = UpdatePolicy.ALL)

        val underrides = PushRulesEntity().apply {
            scope = RuleScope.GLOBAL
            kind = RuleSetKey.UNDERRIDE
        }
        globalRules.underride?.forEach { rule ->
            underrides.pushRules.add(PushRulesMapper.map(rule))
        }
        realm.copyToRealm(underrides, updatePolicy = UpdatePolicy.ALL)
    }

    private fun handleDirectChatRooms(realm: MutableRealm, event: UserAccountDataEvent) {
        val content = event.content.toModel<DirectMessagesContent>() ?: return
        content.forEach { (userId, roomIds) ->
            roomIds.forEach { roomId ->
                val roomSummaryEntity = RoomSummaryEntity.where(realm, roomId).first().find()
                if (roomSummaryEntity != null) {
                    roomSummaryEntity.isDirect = true
                    roomSummaryEntity.directUserId = userId
                    // Also update the avatar and displayname, there is a specific treatment for DMs
                    roomSummaryEntity.avatarUrl = roomAvatarResolver.resolve(realm, roomId)
                    roomSummaryEntity.setDisplayName(roomDisplayNameResolver.resolve(realm, roomId))
                }
            }
        }

        // Handle previous direct rooms
        RoomSummaryEntity.getDirectRooms(realm, excludeRoomIds = content.values.flatten().toSet())
                .forEach {
                    it.isDirect = false
                    it.directUserId = null
                    // Also update the avatar and displayname, there was a specific treatment for DMs
                    it.avatarUrl = roomAvatarResolver.resolve(realm, it.roomId)
                    it.setDisplayName(roomDisplayNameResolver.resolve(realm, it.roomId))
                }
    }

    private fun handleIgnoredUsers(realm: MutableRealm, event: UserAccountDataEvent) {
        val userIds = event.content.toModel<IgnoredUsersContent>()?.ignoredUsers?.keys ?: return
        val currentIgnoredUsers = realm.query(IgnoredUserEntity::class).find()
        val currentIgnoredUserIds = currentIgnoredUsers.map { it.userId }
        // Delete the previous list
        realm.delete(currentIgnoredUsers)
        // And save the new received list
        userIds.forEach {
            val ignoredUserEntity = IgnoredUserEntity().apply {
                userId = it
            }
            realm.copyToRealm(ignoredUserEntity)
        }

        // Delete all the TimelineEvents for all the ignored users
        // See https://spec.matrix.org/latest/client-server-api/#client-behaviour-22 :
        // "Once ignored, the client will no longer receive events sent by that user, with the exception of state events"
        // So just delete all non-state events from our local storage.
        TimelineEventEntity.findAllFrom(realm, userIds)
                .also { Timber.d("Deleting ${it.size} TimelineEventEntity from ignored users") }
                .forEach {
                    //TODO DELETE ON CASCADE
                    //it.deleteOnCascade(true)
                }

        // Handle the case when some users are unignored from another session
        val mustRefreshCache = currentIgnoredUserIds.any { currentIgnoredUserId -> currentIgnoredUserId !in userIds }
        if (mustRefreshCache) {
            Timber.d("A user has been unignored from another session, an initial sync should be performed")
            dispatchMustRefresh()
        }
    }

    private fun dispatchMustRefresh() {
        val session = sessionManager.getSessionComponent(sessionId)?.session()
        session.dispatchTo(sessionListeners) { safeSession, listener ->
            listener.onGlobalError(safeSession, GlobalError.InitialSyncRequest(InitialSyncRequestReason.IGNORED_USERS_LIST_CHANGE))
        }
    }

    private fun handleBreadcrumbs(realm: MutableRealm, event: UserAccountDataEvent) {
        val recentRoomIds = event.content.toModel<BreadcrumbsContent>()?.recentRoomIds ?: return
        val entity = BreadcrumbsEntity.getOrCreate(realm)

        // And save the new received list
        entity.recentRoomIds = realmListOf<String>().apply { addAll(recentRoomIds) }

        // Update the room summaries
        // Reset all the indexes...
        RoomSummaryEntity.where(realm)
                .query("breadcrumbsIndex > $0", RoomSummary.NOT_IN_BREADCRUMBS)
                .find()
                .forEach {
                    it.breadcrumbsIndex = RoomSummary.NOT_IN_BREADCRUMBS
                }

        // ...and apply new indexes
        recentRoomIds.forEachIndexed { index, roomId ->
            RoomSummaryEntity.where(realm, roomId)
                    .first()
                    .find()
                    ?.breadcrumbsIndex = index
        }
    }

    fun handleGenericAccountData(realm: MutableRealm, type: String, content: Content?) {
        val existing = realm.query(UserAccountDataEntity::class)
                .query("type == $0", type)
                .first()
                .find()
        if (existing != null) {
            // Update current value
            existing.contentStr = ContentMapper.map(content)
        } else {
            val newEntity = UserAccountDataEntity().apply {
                this.type = type
                this.contentStr = ContentMapper.map(content)
            }
            realm.copyToRealm(newEntity)
        }
    }
}
