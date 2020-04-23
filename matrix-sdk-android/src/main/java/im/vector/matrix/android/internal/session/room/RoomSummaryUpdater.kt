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

package im.vector.matrix.android.internal.session.room

import dagger.Lazy
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.*
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import im.vector.matrix.android.internal.crypto.crosssigning.SessionToCryptoRoomMembersUpdate
import im.vector.matrix.android.internal.database.helper.isEventRead
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.database.mapper.map
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.room.membership.RoomDisplayNameResolver
import im.vector.matrix.android.internal.session.room.timeline.TimelineEventDecryptor
import im.vector.matrix.android.internal.session.sync.RoomSyncHandler
import im.vector.matrix.android.internal.session.sync.model.RoomSyncSummary
import im.vector.matrix.android.internal.session.sync.model.RoomSyncUnreadNotifications
import im.vector.matrix.sqldelight.session.RoomSummaryHeroes
import im.vector.matrix.sqldelight.session.SessionDatabase
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

internal class RoomSummaryUpdater @Inject constructor(
        @UserId private val userId: String,
        private val sessionDatabase: SessionDatabase,
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
                EventType.STATE_ROOM_MEMBER,
                EventType.STATE_ROOM_HISTORY_VISIBILITY,
                EventType.CALL_INVITE,
                EventType.CALL_HANGUP,
                EventType.CALL_ANSWER,
                EventType.ENCRYPTED,
                EventType.STATE_ROOM_ENCRYPTION,
                EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                EventType.STICKER,
                EventType.STATE_ROOM_CREATE
        )
    }

    fun update(roomId: String,
               newMembership: Membership? = null,
               roomSummary: RoomSyncSummary? = null,
               unreadNotifications: RoomSyncUnreadNotifications? = null,
               updateMembers: Boolean = false,
               ephemeralResult: RoomSyncHandler.EphemeralResult? = null,
               inviterId: String? = null) {

        val currentRoomSummary = sessionDatabase.roomSummaryQueries.get(roomId).executeAsOneOrNull()
        val heroes = if (roomSummary != null && roomSummary.heroes.isNotEmpty()) {
            roomSummary.heroes
        } else {
            emptyList()
        }
        sessionDatabase.roomSummaryQueries.deleteHeroes(roomId)
        heroes.forEach {
            sessionDatabase.roomSummaryQueries.setHeroes(RoomSummaryHeroes.Impl(it, roomId))
        }

        val invitedMemberCount = if (roomSummary?.invitedMembersCount != null) {
            roomSummary.invitedMembersCount
        } else {
            currentRoomSummary?.invited_members_count ?: 0
        }
        val joinedMemberCount = if (roomSummary?.joinedMembersCount != null) {
            roomSummary.joinedMembersCount
        } else {
            currentRoomSummary?.joined_members_count ?: 0
        }
        val highlightCount = unreadNotifications?.highlightCount ?: 0
        val notificationCount = unreadNotifications?.notificationCount ?: 0

        val membership = newMembership ?: currentRoomSummary?.membership?.map() ?: Membership.NONE
        val latestPreviewableEventId = getLastestKnownEventId(roomId = roomId)
        val lastTopicEvent = sessionDatabase.stateEventQueries.getCurrentStateEvent(roomId, type = EventType.STATE_ROOM_TOPIC, stateKey = "").executeAsOneOrNull()
        val lastCanonicalAliasEvent = sessionDatabase.stateEventQueries.getCurrentStateEvent(roomId, type = EventType.STATE_ROOM_CANONICAL_ALIAS, stateKey = "").executeAsOneOrNull()
        val lastAliasesEvent = sessionDatabase.stateEventQueries.getCurrentStateEvent(roomId, type = EventType.STATE_ROOM_ALIASES, stateKey = "").executeAsOneOrNull()


        // Don't use current state for this one as we are only interested in having MXCRYPTO_ALGORITHM_MEGOLM event in the room
        val encryptionEvent = sessionDatabase.eventQueries.findWithContent(roomId = roomId, content = "\"algorithm\":\"$MXCRYPTO_ALGORITHM_MEGOLM\"").executeAsList().firstOrNull()

        val hasUnreadMessages = notificationCount > 0
                // avoid this call if we are sure there are unread events
                || !sessionDatabase.isEventRead(userId, roomId, latestPreviewableEventId)

        val displayName = roomDisplayNameResolver.resolve(roomId, membership).toString()
        val avatarUrl = roomAvatarResolver.resolve(roomId)
        val topic = ContentMapper.map(lastTopicEvent?.content).toModel<RoomTopicContent>()?.topic
        val canonicalAlias = ContentMapper.map(lastCanonicalAliasEvent?.content).toModel<RoomCanonicalAliasContent>()
                ?.canonicalAlias

        val roomAliases = ContentMapper.map(lastAliasesEvent?.content).toModel<RoomAliasesContent>()?.aliases
                ?: emptyList()

        sessionDatabase.roomAliasesQueries.deleteAllForRoom(roomId)
        roomAliases.forEach { alias ->
            sessionDatabase.roomAliasesQueries.insert(roomId, alias)
        }

        val isEncrypted = encryptionEvent != null
        val directUserId = currentRoomSummary?.direct_user_id
        sessionDatabase.userQueries.deleteAllTypingUsers()
        ephemeralResult?.typingUserIds?.forEach { typingId ->
            sessionDatabase.userQueries.insertTyping(roomId, typingId)
        }
        val isDirect = currentRoomSummary?.is_direct ?: false

        if (latestPreviewableEventId != null && latestPreviewableEventId.isNotEmpty()) {
            if (selectEventType(latestPreviewableEventId) == EventType.ENCRYPTED
                    && selectDecryptionResult(latestPreviewableEventId) == null) {
                Timber.v("Should decrypt $latestPreviewableEventId for room: $displayName")
                timelineEventDecryptor.get().requestDecryption(TimelineEventDecryptor.DecryptionRequest(latestPreviewableEventId, ""))
            }
        }
        val newInviterId = if (membership == Membership.INVITE && inviterId != null) {
            inviterId
        } else if (membership != Membership.INVITE) {
            null
        } else {
            currentRoomSummary?.inviter_id
        }
        val newRoomSummaryEntity = im.vector.matrix.sqldelight.session.RoomSummaryEntity.Impl(
                room_id = roomId,
                membership = membership.map(),
                avatar_url = avatarUrl,
                display_name = displayName,
                invited_members_count = invitedMemberCount,
                topic = topic,
                joined_members_count = joinedMemberCount,
                latest_previewable_event = latestPreviewableEventId,
                is_direct = isDirect,
                notification_count = notificationCount,
                highlight_count = highlightCount,
                canonical_alias = canonicalAlias,
                is_encrypted = isEncrypted,
                has_unread = hasUnreadMessages,
                direct_user_id = directUserId,
                versioning_state = currentRoomSummary?.versioning_state
                        ?: VersioningState.NONE.name,
                room_encryption_trust_level = currentRoomSummary?.room_encryption_trust_level,
                inviter_id = newInviterId
        )
        sessionDatabase.roomSummaryQueries.insertOrUpdate(newRoomSummaryEntity)
        if (isEncrypted && updateMembers) {
            // The set of “all users” depends on the type of room:
            // For regular / topic rooms, all users including yourself, are considered when decorating a room
            // For 1:1 and group DM rooms, all other users (i.e. excluding yourself) are considered when decorating a room
            val excludedIds = if (isDirect) {
                listOf(userId)
            } else {
                emptyList()
            }
            val listToCheck = sessionDatabase.roomMemberSummaryQueries.getAllUserIdFromRoom(
                    memberships = Membership.activeMemberships().map(),
                    excludedIds = excludedIds,
                    roomId = roomId
            ).executeAsList()
            eventBus.post(SessionToCryptoRoomMembersUpdate(roomId, listToCheck))
        }
    }

    private fun getLastestKnownEventId(roomId: String): String? {
        return sessionDatabase.timelineEventQueries.getLatestKnownEventId(roomId = roomId, types = PREVIEWABLE_TYPES).executeAsOneOrNull()
                ?.takeIf { it.isNotBlank() }
    }

    private fun selectEventType(eventId: String): String? {
        return sessionDatabase.eventQueries.selectType(eventId).executeAsOneOrNull()
    }

    private fun selectDecryptionResult(eventId: String): String? {
        return sessionDatabase.eventQueries.selectDecryptionResult(eventId).executeAsOneOrNull()?.decryption_result_json
    }

}
