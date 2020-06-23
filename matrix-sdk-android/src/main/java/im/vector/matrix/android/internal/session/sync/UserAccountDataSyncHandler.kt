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
import im.vector.matrix.android.api.pushrules.RuleScope
import im.vector.matrix.android.api.pushrules.RuleSetKey
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.mapper.PushRulesMapper
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.BreadcrumbsEntity
import im.vector.matrix.android.internal.database.model.IgnoredUserEntity
import im.vector.matrix.android.internal.database.model.PushRulesEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import im.vector.matrix.android.internal.database.model.UserAccountDataEntity
import im.vector.matrix.android.internal.database.model.UserAccountDataEntityFields
import im.vector.matrix.android.internal.database.query.getDirectRooms
import im.vector.matrix.android.internal.database.query.getOrCreate
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.room.membership.RoomMemberHelper
import im.vector.matrix.android.internal.session.sync.model.InvitedRoomSync
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountData
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountDataBreadcrumbs
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountDataDirectMessages
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountDataIgnoredUsers
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountDataPushRules
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountDataSync
import im.vector.matrix.android.internal.session.user.accountdata.DirectChatsHelper
import im.vector.matrix.android.internal.session.user.accountdata.UpdateUserAccountDataTask
import io.realm.Realm
import io.realm.RealmList
import io.realm.kotlin.where
import timber.log.Timber
import javax.inject.Inject

internal class UserAccountDataSyncHandler @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        @UserId private val userId: String,
        private val directChatsHelper: DirectChatsHelper,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask) {

    fun handle(realm: Realm, accountData: UserAccountDataSync?) {
        accountData?.list?.forEach {
            // Generic handling, just save in base
            handleGenericAccountData(realm, it.type, it.content)

            // Didn't want to break too much thing, so i re-serialize to jsonString before reparsing
            // TODO would be better to have a mapper?
            val toJson = MoshiProvider.providesMoshi().adapter(Event::class.java).toJson(it)
            val model = toJson?.let { json ->
                MoshiProvider.providesMoshi().adapter(UserAccountData::class.java).fromJson(json)
            }
            // Specific parsing
            when (model) {
                is UserAccountDataDirectMessages -> handleDirectChatRooms(realm, model)
                is UserAccountDataPushRules      -> handlePushRules(realm, model)
                is UserAccountDataIgnoredUsers   -> handleIgnoredUsers(realm, model)
                is UserAccountDataBreadcrumbs    -> handleBreadcrumbs(realm, model)
            }
        }
    }

    // If we get some direct chat invites, we synchronize the user account data including those.
    suspend fun synchronizeWithServerIfNeeded(invites: Map<String, InvitedRoomSync>) {
        if (invites.isNullOrEmpty()) return
        val directChats = directChatsHelper.getLocalUserAccount()
        var hasUpdate = false
        monarchy.doWithRealm { realm ->
            invites.forEach { (roomId, _) ->
                val myUserStateEvent = RoomMemberHelper(realm, roomId).getLastStateEvent(userId)
                val inviterId = myUserStateEvent?.sender
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
        }
        if (hasUpdate) {
            val updateUserAccountParams = UpdateUserAccountDataTask.DirectChatParams(
                    directMessages = directChats
            )
            updateUserAccountDataTask.execute(updateUserAccountParams)
        }
    }

    private fun handlePushRules(realm: Realm, userAccountDataPushRules: UserAccountDataPushRules) {
        val pushRules = userAccountDataPushRules.content
        realm.where(PushRulesEntity::class.java)
                .findAll()
                .deleteAllFromRealm()

        // Save only global rules for the moment
        val globalRules = pushRules.global

        val content = PushRulesEntity(RuleScope.GLOBAL).apply { kind = RuleSetKey.CONTENT }
        globalRules.content?.forEach { rule ->
            content.pushRules.add(PushRulesMapper.map(rule))
        }
        realm.insertOrUpdate(content)

        val override = PushRulesEntity(RuleScope.GLOBAL).apply { kind = RuleSetKey.OVERRIDE }
        globalRules.override?.forEach { rule ->
            PushRulesMapper.map(rule).also {
                override.pushRules.add(it)
            }
        }
        realm.insertOrUpdate(override)

        val rooms = PushRulesEntity(RuleScope.GLOBAL).apply { kind = RuleSetKey.ROOM }
        globalRules.room?.forEach { rule ->
            rooms.pushRules.add(PushRulesMapper.map(rule))
        }
        realm.insertOrUpdate(rooms)

        val senders = PushRulesEntity(RuleScope.GLOBAL).apply { kind = RuleSetKey.SENDER }
        globalRules.sender?.forEach { rule ->
            senders.pushRules.add(PushRulesMapper.map(rule))
        }
        realm.insertOrUpdate(senders)

        val underrides = PushRulesEntity(RuleScope.GLOBAL).apply { kind = RuleSetKey.UNDERRIDE }
        globalRules.underride?.forEach { rule ->
            underrides.pushRules.add(PushRulesMapper.map(rule))
        }
        realm.insertOrUpdate(underrides)
    }

    private fun handleDirectChatRooms(realm: Realm, directMessages: UserAccountDataDirectMessages) {
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

    private fun handleIgnoredUsers(realm: Realm, userAccountDataIgnoredUsers: UserAccountDataIgnoredUsers) {
        val userIds = userAccountDataIgnoredUsers.content.ignoredUsers.keys
        realm.where(IgnoredUserEntity::class.java)
                .findAll()
                .deleteAllFromRealm()
        // And save the new received list
        userIds.forEach { realm.createObject(IgnoredUserEntity::class.java).apply { userId = it } }
        // TODO If not initial sync, we should execute a init sync
    }

    private fun handleBreadcrumbs(realm: Realm, userAccountDataBreadcrumbs: UserAccountDataBreadcrumbs) {
        val recentRoomIds = userAccountDataBreadcrumbs.content.recentRoomIds
        val entity = BreadcrumbsEntity.getOrCreate(realm)

        // And save the new received list
        entity.recentRoomIds = RealmList<String>().apply { addAll(recentRoomIds) }

        // Update the room summaries
        // Reset all the indexes...
        RoomSummaryEntity.where(realm)
                .greaterThan(RoomSummaryEntityFields.BREADCRUMBS_INDEX, RoomSummary.NOT_IN_BREADCRUMBS)
                .findAll()
                .forEach {
                    it.breadcrumbsIndex = RoomSummary.NOT_IN_BREADCRUMBS
                }

        // ...and apply new indexes
        recentRoomIds.forEachIndexed { index, roomId ->
            RoomSummaryEntity.where(realm, roomId)
                    .findFirst()
                    ?.breadcrumbsIndex = index
        }
    }

    fun handleGenericAccountData(realm: Realm, type: String, content: Content?) {
        val existing = realm.where<UserAccountDataEntity>()
                .equalTo(UserAccountDataEntityFields.TYPE, type)
                .findFirst()
        if (existing != null) {
            // Update current value
            existing.contentStr = ContentMapper.map(content)
        } else {
            realm.createObject(UserAccountDataEntity::class.java).let { accountDataEntity ->
                accountDataEntity.type = type
                accountDataEntity.contentStr = ContentMapper.map(content)
            }
        }
    }
}
