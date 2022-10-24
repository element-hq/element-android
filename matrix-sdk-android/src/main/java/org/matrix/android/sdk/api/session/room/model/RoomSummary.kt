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
        /**
         * The roomId of the room.
         */
        val roomId: String,
        /**
         * Computed display name. The value of the state event `m.room.name` if not empty, else can be the value returned
         * by [org.matrix.android.sdk.api.RoomDisplayNameFallbackProvider].
         */
        val displayName: String = "",
        /**
         * The value of the live state event `m.room.name`.
         */
        val name: String = "",
        /**
         * The value of the live state event `m.room.topic`.
         */
        val topic: String = "",
        /**
         * The value of the live state event `m.room.avatar`.
         */
        val avatarUrl: String = "",
        /**
         * The value of the live state event `m.room.canonical_alias`.
         */
        val canonicalAlias: String? = null,
        /**
         * The list of all the aliases of this room. Content of the live state event `m.room.aliases`.
         */
        val aliases: List<String> = emptyList(),
        /**
         * The value of the live state event `m.room.join_rules`.
         */
        val joinRules: RoomJoinRules? = null,
        /**
         * True is this room is referenced in the account data `m.direct`.
         */
        val isDirect: Boolean = false,
        /**
         * If [isDirect] is true, this is the id of the first other member of this room.
         */
        val directUserId: String? = null,
        /**
         * If [isDirect] is true, this it the presence of the first other member of this room.
         */
        val directUserPresence: UserPresence? = null,
        /**
         * Number of members who have joined this room.
         */
        val joinedMembersCount: Int? = 0,
        /**
         * Number of members who are invited to this room.
         */
        val invitedMembersCount: Int? = 0,
        /**
         * Latest [TimelineEvent] which can be displayed in this room. Can be used in the room list.
         */
        val latestPreviewableEvent: TimelineEvent? = null,
        /**
         * List of other member ids of this room.
         */
        val otherMemberIds: List<String> = emptyList(),
        /**
         * Number of unread message in this room.
         */
        val notificationCount: Int = 0,
        /**
         * Number of unread and highlighted message in this room.
         */
        val highlightCount: Int = 0,
        /**
         * Number of threads with unread messages in this room.
         */
        val threadNotificationCount: Int = 0,
        /**
         * Number of threads with highlighted messages in this room.
         */
        val threadHighlightCount: Int = 0,
        /**
         * True if this room has unread messages.
         */
        val hasUnreadMessages: Boolean = false,
        /**
         * List of tags in this room.
         */
        val tags: List<RoomTag> = emptyList(),
        /**
         * Current user membership in this room.
         */
        val membership: Membership = Membership.NONE,
        /**
         * Versioning state of this room.
         */
        val versioningState: VersioningState = VersioningState.NONE,
        /**
         * Value of `m.fully_read` for this room.
         */
        val readMarkerId: String? = null,
        /**
         * Message saved as draft for this room.
         */
        val userDrafts: List<UserDraft> = emptyList(),
        /**
         * True if this room is encrypted.
         */
        val isEncrypted: Boolean,
        /**
         * Timestamp of the `m.room.encryption` state event.
         */
        val encryptionEventTs: Long?,
        /**
         * List of users who are currently typing on this room.
         */
        val typingUsers: List<SenderInfo>,
        /**
         * UserId of the user who has invited the current user to this room.
         */
        val inviterId: String? = null,
        /**
         * Breadcrumb index, util to sort rooms by last seen.
         */
        val breadcrumbsIndex: Int = NOT_IN_BREADCRUMBS,
        /**
         * The room encryption trust level.
         * @see [RoomEncryptionTrustLevel]
         */
        val roomEncryptionTrustLevel: RoomEncryptionTrustLevel? = null,
        /**
         * True if a message has not been sent in this room.
         */
        val hasFailedSending: Boolean = false,
        /**
         * The type of the room. Null for regular room.
         * @see [RoomType]
         */
        val roomType: String? = null,
        /**
         * List of parent spaces.
         */
        val spaceParents: List<SpaceParentInfo>? = null,
        /**
         * List of children space.
         */
        val spaceChildren: List<SpaceChildInfo>? = null,
        /**
         * The names of the room's direct space parents if any.
         */
        val directParentNames: List<String> = emptyList(),
        /**
         * List of all the space parent Ids.
         */
        val flattenParentIds: List<String> = emptyList(),
        /**
         * Information about the encryption algorithm, if this room is encrypted.
         */
        val roomEncryptionAlgorithm: RoomEncryptionAlgorithm? = null,
) {
    /**
     * True if [versioningState] is not [VersioningState.NONE].
     */
    val isVersioned: Boolean
        get() = versioningState != VersioningState.NONE

    /**
     * True if [notificationCount] is not `0`.
     */
    val hasNewMessages: Boolean
        get() = notificationCount != 0

    /**
     * True if the room has the tag `m.lowpriority`.
     */
    val isLowPriority: Boolean
        get() = hasTag(RoomTag.ROOM_TAG_LOW_PRIORITY)

    /**
     * True if the room has the tag `m.favourite`.
     */
    val isFavorite: Boolean
        get() = hasTag(RoomTag.ROOM_TAG_FAVOURITE)

    /**
     * True if [joinRules] is [RoomJoinRules.PUBLIC].
     */
    val isPublic: Boolean
        get() = joinRules == RoomJoinRules.PUBLIC

    /**
     * Test if the room has the provided [tag].
     */
    fun hasTag(tag: String) = tags.any { it.name == tag }

    /**
     * True if a 1-1 call can be started, i.e. the room has exactly 2 joined members.
     */
    val canStartCall: Boolean
        get() = joinedMembersCount == 2

    companion object {
        /**
         * Constant to indicated that the room is not on the breadcrumbs.
         * Used by [breadcrumbsIndex].
         */
        const val NOT_IN_BREADCRUMBS = -1
    }
}
