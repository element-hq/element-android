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

import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.api.session.room.send.UserDraft
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

/**
 * This class holds some data of a room.
 * It can be retrieved by [org.matrix.android.sdk.api.session.room.Room] and [org.matrix.android.sdk.api.session.room.RoomService]
 */
data class RoomSummary constructor(
        val roomId: String,
        // Computed display name
        val displayName: String = "",
        val name: String = "",
        val topic: String = "",
        val avatarUrl: String = "",
        val canonicalAlias: String? = null,
        val aliases: List<String> = emptyList(),
        val isDirect: Boolean = false,
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
        val hasFailedSending: Boolean = false
) {

    val isVersioned: Boolean
        get() = versioningState != VersioningState.NONE

    val hasNewMessages: Boolean
        get() = notificationCount != 0

    val isFavorite: Boolean
        get() = tags.any { it.name == RoomTag.ROOM_TAG_FAVOURITE }

    val canStartCall: Boolean
        get() = joinedMembersCount == 2

    fun scHasUnreadMessages(preferenceProvider: RoomSummaryPreferenceProvider?): Boolean {
        if (preferenceProvider == null) {
            // Fallback to default
            return hasUnreadMessages
        }
        return when(preferenceProvider.getUnreadKind()) {
            UNREAD_KIND_ORIGINAL_CONTENT -> hasUnreadOriginalContentMessages
            UNREAD_KIND_CONTENT -> hasUnreadContentMessages
            // UNREAD_KIND_DEFAULT
            else -> hasUnreadMessages
        }
    }

    fun scLatestPreviewableEvent(preferenceProvider: RoomSummaryPreferenceProvider?): TimelineEvent? {
        if (preferenceProvider == null) {
            // Fallback to default
            return latestPreviewableEvent
        }
        return when(preferenceProvider.getUnreadKind()) {
            UNREAD_KIND_ORIGINAL_CONTENT -> latestPreviewableOriginalContentEvent
            UNREAD_KIND_CONTENT -> latestPreviewableContentEvent
            // UNREAD_KIND_DEFAULT
            else -> latestPreviewableEvent
        }
    }

    companion object {
        const val NOT_IN_BREADCRUMBS = -1
        // SC addition
        const val UNREAD_KIND_DEFAULT = 0
        const val UNREAD_KIND_CONTENT = 1
        const val UNREAD_KIND_ORIGINAL_CONTENT = 2
    }

    // SC addition
    interface RoomSummaryPreferenceProvider {
        fun getUnreadKind(): Int
    }
}
