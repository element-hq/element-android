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

package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.crypto.RoomEncryptionTrustLevel
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.VersioningState
import im.vector.matrix.android.api.session.room.model.tag.RoomTag
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.sqldelight.session.RoomSummaryWithTimeline
import javax.inject.Inject

internal class RoomSummaryMapper @Inject constructor() {

    fun map(roomSummaryWithTimeline: RoomSummaryWithTimeline, tags: List<RoomTag>): RoomSummary {
        return RoomSummary(
                roomId = roomSummaryWithTimeline.summary_room_id,
                displayName = roomSummaryWithTimeline.display_name ?: "",
                topic = roomSummaryWithTimeline.topic ?: "",
                avatarUrl = roomSummaryWithTimeline.avatar_url ?: "",
                isDirect = roomSummaryWithTimeline.is_direct,
                latestPreviewableEvent = createTimelineEvent(roomSummaryWithTimeline),
                joinedMembersCount = roomSummaryWithTimeline.joined_members_count,
                invitedMembersCount = roomSummaryWithTimeline.invited_members_count,
                otherMemberIds = emptyList(),
                highlightCount = roomSummaryWithTimeline.highlight_count,
                notificationCount = roomSummaryWithTimeline.notification_count,
                hasUnreadMessages = roomSummaryWithTimeline.has_unread,
                versioningState = VersioningState.valueOf(roomSummaryWithTimeline.versioning_state),
                tags = tags,
                membership = roomSummaryWithTimeline.membership.map(),
                readMarkerId = roomSummaryWithTimeline.read_marker_id,
                userDrafts = emptyList(),
                canonicalAlias = roomSummaryWithTimeline.canonical_alias,
                inviterId = roomSummaryWithTimeline.inviter_id,
                isEncrypted = roomSummaryWithTimeline.is_encrypted,
                typingRoomMemberIds = emptyList(),//roomSummaryEntity.typingUserIds.toList(),
                breadcrumbsIndex = roomSummaryWithTimeline.breadcrumb_index ?: -1,
                roomEncryptionTrustLevel = roomSummaryWithTimeline.room_encryption_trust_level?.let {
                    try {
                        RoomEncryptionTrustLevel.valueOf(it)
                    } catch (failure: Throwable) {
                        null
                    }
                }
        )
    }

    private fun createTimelineEvent(roomSummaryWithTimeline: RoomSummaryWithTimeline): TimelineEvent? {
        val type = roomSummaryWithTimeline.type ?: return null
        val roomId = roomSummaryWithTimeline.summary_room_id
        val eventId = roomSummaryWithTimeline.event_id ?: return null
        val displayIndex = roomSummaryWithTimeline.display_index ?: return null
        val localId = roomSummaryWithTimeline.local_id ?: return null
        val event = Event(
                type = type,
                roomId = roomId,
                eventId = eventId,
                content = ContentMapper.map(roomSummaryWithTimeline.content),
                prevContent = ContentMapper.map(roomSummaryWithTimeline.prev_content),
                originServerTs = roomSummaryWithTimeline.origin_server_ts,
                senderId = roomSummaryWithTimeline.sender_id,
                redacts = roomSummaryWithTimeline.redacts,
                stateKey = roomSummaryWithTimeline.state_key,
                unsignedData = null
        ).setDecryptionValues(roomSummaryWithTimeline.decryption_result_json, roomSummaryWithTimeline.decryption_error_code)


        return TimelineEvent(
                root = event,
                eventId = eventId,
                annotations = null,
                displayIndex = displayIndex,
                isUniqueDisplayName = roomSummaryWithTimeline.is_unique_display_name ?: false,
                localId = localId,
                readReceipts = emptyList(),
                senderAvatar = roomSummaryWithTimeline.sender_avatar,
                senderName = roomSummaryWithTimeline.sender_name
        )
    }

}
