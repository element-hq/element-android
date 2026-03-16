/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.appfunctions

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageResult(
        val roomId: String,
        val roomName: String,
        val senderId: String,
        val senderName: String,
        val body: String,
        val timestamp: Long,
        val eventId: String
)

@JsonClass(generateAdapter = true)
data class RoomInfo(
        val roomId: String,
        val displayName: String,
        val lastMessage: String?,
        val lastMessageSender: String?,
        val unreadCount: Int,
        val isDirect: Boolean
)

@JsonClass(generateAdapter = true)
data class UnreadSummary(
        val totalUnread: Int,
        val totalMentions: Int,
        val rooms: List<RoomUnread>
)

@JsonClass(generateAdapter = true)
data class RoomUnread(
        val roomId: String,
        val roomName: String,
        val unreadCount: Int,
        val hasMentions: Boolean
)
