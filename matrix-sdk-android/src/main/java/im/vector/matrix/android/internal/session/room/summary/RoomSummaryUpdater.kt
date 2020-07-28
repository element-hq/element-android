/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.room.summary

import dagger.Lazy
import im.vector.matrix.android.api.crypto.RoomEncryptionTrustLevel
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomAliasesContent
import im.vector.matrix.android.api.session.room.model.RoomCanonicalAliasContent
import im.vector.matrix.android.api.session.room.model.RoomNameContent
import im.vector.matrix.android.api.session.room.model.RoomTopicContent
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import im.vector.matrix.android.internal.crypto.crosssigning.SessionToCryptoRoomMembersUpdate
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.model.CurrentStateEventEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.model.RoomMemberSummaryEntityFields
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.query.findAllInRoomWithSendStates
import im.vector.matrix.android.internal.database.query.getOrCreate
import im.vector.matrix.android.internal.database.query.getOrNull
import im.vector.matrix.android.internal.database.query.isEventRead
import im.vector.matrix.android.internal.database.query.latestEvent
import im.vector.matrix.android.internal.database.query.whereType
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.room.RoomAvatarResolver
import im.vector.matrix.android.internal.session.room.membership.RoomDisplayNameResolver
import im.vector.matrix.android.internal.session.room.membership.RoomMemberHelper
import im.vector.matrix.android.internal.session.room.timeline.TimelineEventDecryptor
import im.vector.matrix.android.internal.session.sync.model.RoomSyncSummary
import im.vector.matrix.android.internal.session.sync.model.RoomSyncUnreadNotifications
import io.realm.Realm
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

internal class RoomSummaryUpdater @Inject constructor(
        @UserId private val userId: String,
        private val roomDisplayNameResolver: RoomDisplayNameResolver,
        private val roomAvatarResolver: RoomAvatarResolver,
        private val timelineEventDecryptor: Lazy<TimelineEventDecryptor>,
        private val eventBus: EventBus) {

    companion object {
        // TODO: maybe allow user of SDK to give that list
        val PREVIEWABLE_TYPES = listOf(
                // TODO filter message type (KEY_VERIFICATION_READY, etc.)
                EventType.MESSAGE,
                EventType.STATE_ROOM_NAME,
                EventType.STATE_ROOM_TOPIC,
                EventType.STATE_ROOM_AVATAR,
                EventType.STATE_ROOM_MEMBER,
                EventType.STATE_ROOM_HISTORY_VISIBILITY,
                EventType.CALL_INVITE,
                EventType.CALL_HANGUP,
                EventType.CALL_ANSWER,
                EventType.ENCRYPTED,
                EventType.STATE_ROOM_ENCRYPTION,
                EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                EventType.STICKER,
                EventType.REACTION,
                EventType.STATE_ROOM_CREATE
        )
    }

    fun update(realm: Realm,
               roomId: String,
               membership: Membership? = null,
               roomSummary: RoomSyncSummary? = null,
               unreadNotifications: RoomSyncUnreadNotifications? = null,
               updateMembers: Boolean = false,
               inviterId: String? = null) {
        val roomSummaryEntity = RoomSummaryEntity.getOrCreate(realm, roomId)
        if (roomSummary != null) {
            if (roomSummary.heroes.isNotEmpty()) {
                roomSummaryEntity.heroes.clear()
                roomSummaryEntity.heroes.addAll(roomSummary.heroes)
            }
            if (roomSummary.invitedMembersCount != null) {
                roomSummaryEntity.invitedMembersCount = roomSummary.invitedMembersCount
            }
            if (roomSummary.joinedMembersCount != null) {
                roomSummaryEntity.joinedMembersCount = roomSummary.joinedMembersCount
            }
        }
        roomSummaryEntity.highlightCount = unreadNotifications?.highlightCount ?: 0
        roomSummaryEntity.notificationCount = unreadNotifications?.notificationCount ?: 0

        if (membership != null) {
            roomSummaryEntity.membership = membership
        }

        val latestPreviewableEvent = TimelineEventEntity.latestEvent(realm, roomId, includesSending = true,
                filterTypes = PREVIEWABLE_TYPES, filterContentRelation = true)

        val lastNameEvent = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_NAME, stateKey = "")?.root
        val lastTopicEvent = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_TOPIC, stateKey = "")?.root
        val lastCanonicalAliasEvent = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_CANONICAL_ALIAS, stateKey = "")?.root
        val lastAliasesEvent = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_ALIASES, stateKey = "")?.root

        // Don't use current state for this one as we are only interested in having MXCRYPTO_ALGORITHM_MEGOLM event in the room
        val encryptionEvent = EventEntity.whereType(realm, roomId = roomId, type = EventType.STATE_ROOM_ENCRYPTION)
                .contains(EventEntityFields.CONTENT, "\"algorithm\":\"$MXCRYPTO_ALGORITHM_MEGOLM\"")
                .findFirst()

        roomSummaryEntity.hasUnreadMessages = roomSummaryEntity.notificationCount > 0
                // avoid this call if we are sure there are unread events
                || !isEventRead(realm.configuration, userId, roomId, latestPreviewableEvent?.eventId)

        roomSummaryEntity.displayName = roomDisplayNameResolver.resolve(realm, roomId).toString()
        roomSummaryEntity.avatarUrl = roomAvatarResolver.resolve(realm, roomId)
        roomSummaryEntity.name = ContentMapper.map(lastNameEvent?.content).toModel<RoomNameContent>()?.name
        roomSummaryEntity.topic = ContentMapper.map(lastTopicEvent?.content).toModel<RoomTopicContent>()?.topic
        roomSummaryEntity.latestPreviewableEvent = latestPreviewableEvent
        roomSummaryEntity.canonicalAlias = ContentMapper.map(lastCanonicalAliasEvent?.content).toModel<RoomCanonicalAliasContent>()
                ?.canonicalAlias

        val roomAliases = ContentMapper.map(lastAliasesEvent?.content).toModel<RoomAliasesContent>()?.aliases
                .orEmpty()
        roomSummaryEntity.aliases.clear()
        roomSummaryEntity.aliases.addAll(roomAliases)
        roomSummaryEntity.flatAliases = roomAliases.joinToString(separator = "|", prefix = "|")
        roomSummaryEntity.isEncrypted = encryptionEvent != null
        roomSummaryEntity.encryptionEventTs = encryptionEvent?.originServerTs

        if (roomSummaryEntity.membership == Membership.INVITE && inviterId != null) {
            roomSummaryEntity.inviterId = inviterId
        } else if (roomSummaryEntity.membership != Membership.INVITE) {
            roomSummaryEntity.inviterId = null
        }
        roomSummaryEntity.updateHasFailedSending()

        if (latestPreviewableEvent?.root?.type == EventType.ENCRYPTED && latestPreviewableEvent.root?.decryptionResultJson == null) {
            Timber.v("Should decrypt ${latestPreviewableEvent.eventId}")
            timelineEventDecryptor.get().requestDecryption(TimelineEventDecryptor.DecryptionRequest(latestPreviewableEvent.eventId, ""))
        }

        if (updateMembers) {
            val otherRoomMembers = RoomMemberHelper(realm, roomId)
                    .queryActiveRoomMembersEvent()
                    .notEqualTo(RoomMemberSummaryEntityFields.USER_ID, userId)
                    .findAll()
                    .asSequence()
                    .map { it.userId }

            roomSummaryEntity.otherMemberIds.clear()
            roomSummaryEntity.otherMemberIds.addAll(otherRoomMembers)
            if (roomSummaryEntity.isEncrypted) {
                eventBus.post(SessionToCryptoRoomMembersUpdate(roomId, roomSummaryEntity.isDirect, roomSummaryEntity.otherMemberIds.toList() + userId))
            }
        }
    }

    private fun RoomSummaryEntity.updateHasFailedSending() {
        hasFailedSending = TimelineEventEntity.findAllInRoomWithSendStates(realm, roomId, SendState.HAS_FAILED_STATES).isNotEmpty()
    }

    fun updateSendingInformation(realm: Realm, roomId: String) {
        val roomSummaryEntity = RoomSummaryEntity.getOrCreate(realm, roomId)
        roomSummaryEntity.updateHasFailedSending()
        roomSummaryEntity.latestPreviewableEvent = TimelineEventEntity.latestEvent(realm, roomId, includesSending = true,
                filterTypes = PREVIEWABLE_TYPES, filterContentRelation = true)
    }

    fun updateShieldTrust(realm: Realm,
                          roomId: String,
                          trust: RoomEncryptionTrustLevel?) {
        val roomSummaryEntity = RoomSummaryEntity.getOrCreate(realm, roomId)
        if (roomSummaryEntity.isEncrypted) {
            roomSummaryEntity.roomEncryptionTrustLevel = trust
        }
    }
}
