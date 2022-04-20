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

package org.matrix.android.sdk.api.session.room.model

import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.presence.model.UserPresence
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

/**
 * This class holds some data of a room.
 * It can be retrieved by [org.matrix.android.sdk.api.session.room.Room] and [org.matrix.android.sdk.api.session.room.RoomService]
 */
data class RoomSummary(
        val roomId: String,
        // Computed display name
        val displayName: String = "",
        val name: String = "",
        val topic: String = "",
        val avatarUrl: String = "",
        val canonicalAlias: String? = null,
        val aliases: List<String> = emptyList(),
        val joinRules: RoomJoinRules? = null,
        val isDirect: Boolean = false,
        val directUserId: String? = null,
        val directUserPresence: UserPresence? = null,
        val joinedMembersCount: Int? = 0,
        val invitedMembersCount: Int? = 0,
        val latestPreviewableEvent: TimelineEvent? = null,
        val otherMemberIds: List<String> = emptyList(),
        val notificationCount: Int = 0,
        val highlightCount: Int = 0,
        val hasUnreadMessages: Boolean = false,
        val tags: List<RoomTag> = emptyList(),
        val membership: Membership = Membership.NONE,
        val versioningState: VersioningState = VersioningState.NONE,
        val readMarkerId: String? = null,
        val userDrafts: List<UserDraft> = emptyList(),
        val isEncrypted: Boolean,
        val encryptionEventTs: Long?,
        val typingUsers: List<SenderInfo>,
        val inviterId: String? = null,
        val breadcrumbsIndex: Int = NOT_IN_BREADCRUMBS,
        val roomEncryptionTrustLevel: RoomEncryptionTrustLevel? = null,
        val hasFailedSending: Boolean = false,
        val roomType: String? = null,
        val spaceParents: List<SpaceParentInfo>? = null,
        val spaceChildren: List<SpaceChildInfo>? = null,
        val flattenParentIds: List<String> = emptyList(),
        val roomEncryptionAlgorithm: RoomEncryptionAlgorithm? = null
) {

    val isVersioned: Boolean
        get() = versioningState != VersioningState.NONE

    val hasNewMessages: Boolean
        get() = notificationCount != 0

    val isLowPriority: Boolean
        get() = hasTag(RoomTag.ROOM_TAG_LOW_PRIORITY)

    val isFavorite: Boolean
        get() = hasTag(RoomTag.ROOM_TAG_FAVOURITE)

    val isPublic: Boolean
        get() = joinRules == RoomJoinRules.PUBLIC

    fun hasTag(tag: String) = tags.any { it.name == tag }

    val canStartCall: Boolean
        get() = joinedMembersCount == 2

    companion object {
        const val NOT_IN_BREADCRUMBS = -1
    }
}
