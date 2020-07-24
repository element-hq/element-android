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

import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.tag.RoomTag
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.session.typing.DefaultTypingUsersTracker
import javax.inject.Inject

internal class RoomSummaryMapper @Inject constructor(private val timelineEventMapper: TimelineEventMapper,
                                                     private val typingUsersTracker: DefaultTypingUsersTracker) {

    fun map(roomSummaryEntity: RoomSummaryEntity): RoomSummary {
        val tags = roomSummaryEntity.tags.map {
            RoomTag(it.tagName, it.tagOrder)
        }

        val latestEvent = roomSummaryEntity.latestPreviewableEvent?.let {
            timelineEventMapper.map(it, buildReadReceipts = false)
        }
        // typings are updated through the sync where room summary entity gets updated no matter what, so it's ok get there
        val typingUsers = typingUsersTracker.getTypingUsers(roomSummaryEntity.roomId)

        return RoomSummary(
                roomId = roomSummaryEntity.roomId,
                displayName = roomSummaryEntity.displayName ?: "",
                name = roomSummaryEntity.name ?: "",
                topic = roomSummaryEntity.topic ?: "",
                avatarUrl = roomSummaryEntity.avatarUrl ?: "",
                isDirect = roomSummaryEntity.isDirect,
                latestPreviewableEvent = latestEvent,
                joinedMembersCount = roomSummaryEntity.joinedMembersCount,
                invitedMembersCount = roomSummaryEntity.invitedMembersCount,
                otherMemberIds = roomSummaryEntity.otherMemberIds.toList(),
                highlightCount = roomSummaryEntity.highlightCount,
                notificationCount = roomSummaryEntity.notificationCount,
                hasUnreadMessages = roomSummaryEntity.hasUnreadMessages,
                tags = tags,
                typingUsers = typingUsers,
                membership = roomSummaryEntity.membership,
                versioningState = roomSummaryEntity.versioningState,
                readMarkerId = roomSummaryEntity.readMarkerId,
                userDrafts = roomSummaryEntity.userDrafts?.userDrafts?.map { DraftMapper.map(it) }.orEmpty(),
                canonicalAlias = roomSummaryEntity.canonicalAlias,
                aliases = roomSummaryEntity.aliases.toList(),
                isEncrypted = roomSummaryEntity.isEncrypted,
                encryptionEventTs = roomSummaryEntity.encryptionEventTs,
                breadcrumbsIndex = roomSummaryEntity.breadcrumbsIndex,
                roomEncryptionTrustLevel = roomSummaryEntity.roomEncryptionTrustLevel,
                inviterId = roomSummaryEntity.inviterId,
                hasFailedSending = roomSummaryEntity.hasFailedSending
        )
    }
}
