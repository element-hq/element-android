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

import de.spiritcroc.matrixsdk.StaticScSdkHelper
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.presence.model.UserPresence
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber

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
        val latestPreviewableContentEvent: TimelineEvent? = null,
        val latestPreviewableOriginalContentEvent: TimelineEvent? = null,
        val otherMemberIds: List<String> = emptyList(),
        val notificationCount: Int = 0,
        val highlightCount: Int = 0,
        val hasUnreadMessages: Boolean = false,
        val hasUnreadContentMessages: Boolean = false,
        val hasUnreadOriginalContentMessages: Boolean = false,
        val unreadCount: Int? = null,
        val markedUnread: Boolean = false,
        val aggregatedUnreadCount: Int = 0,
        val aggregatedNotificationCount: Int = 0,
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
        val flattenParentIds: List<String> = emptyList()
) {

    // Keep in sync with RoomSummaryEntity.kt!
    val safeUnreadCount: Int
        get() {
            return when {
                unreadCount != null && unreadCount > 0                         -> unreadCount
                hasUnreadOriginalContentMessages && roomType != RoomType.SPACE -> 1
                else                                                           -> 0
            }
        }

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

    fun scIsUnread(): Boolean {
        return markedUnread || scHasUnreadMessages()
    }

    // Keep sync with RoomSummary.scHasUnreadMessages!
    fun scHasUnreadMessages(): Boolean {
        val preferenceProvider = StaticScSdkHelper.scSdkPreferenceProvider
        if (preferenceProvider == null) {
            // Fallback to default
            return hasUnreadOriginalContentMessages
        }
        return when(preferenceProvider.roomUnreadKind(isDirect)) {
            UNREAD_KIND_ORIGINAL_CONTENT -> hasUnreadOriginalContentMessages
            UNREAD_KIND_CONTENT          -> hasUnreadContentMessages
            UNREAD_KIND_FULL             -> hasUnreadMessages
            else                         -> hasUnreadOriginalContentMessages
        }
    }

    // Keep sync with RoomSummaryEntity.scLatestPreviewableEvent!
    fun scLatestPreviewableEvent(): TimelineEvent? {
        val preferenceProvider = StaticScSdkHelper.scSdkPreferenceProvider
        if (preferenceProvider == null) {
            // Fallback to default
            return latestPreviewableOriginalContentEvent
        }
        return when(preferenceProvider.roomUnreadKind(isDirect)) {
            UNREAD_KIND_ORIGINAL_CONTENT -> latestPreviewableOriginalContentEvent
            UNREAD_KIND_CONTENT          -> latestPreviewableContentEvent
            UNREAD_KIND_FULL             -> latestPreviewableEvent
            else                         -> latestPreviewableOriginalContentEvent
        }
    }

    // Keep sync with RoomSummaryEntity.notificationCountOrMarkedUnread!
    fun notificationCountOrMarkedUnread(): Int {
        return when {
            notificationCount > 0 -> notificationCount
            markedUnread          -> 1
            else                  -> 0
        }
    }

    companion object {
        const val NOT_IN_BREADCRUMBS = -1
        // SC addition
        const val UNREAD_KIND_FULL = 0
        const val UNREAD_KIND_CONTENT = 1
        const val UNREAD_KIND_ORIGINAL_CONTENT = 2
    }
}
