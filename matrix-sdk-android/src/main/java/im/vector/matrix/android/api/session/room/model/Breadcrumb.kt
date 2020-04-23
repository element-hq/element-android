package im.vector.matrix.android.api.session.room.model

import im.vector.matrix.android.api.session.room.send.UserDraft

/**
 * This data class holds data about a breadcrumb.
 */
data class Breadcrumb(
        val roomId: String,
        val displayName: String = "",
        val avatarUrl: String = "",
        val notificationCount: Int = 0,
        val highlightCount: Int = 0,
        val hasUnreadMessages: Boolean = false,
        val userDrafts: List<UserDraft> = emptyList(),
        var isEncrypted: Boolean,
        val typingRoomMemberIds: List<String> = emptyList()
)
