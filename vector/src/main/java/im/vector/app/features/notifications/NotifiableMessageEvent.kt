/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.notifications

import android.net.Uri
import org.matrix.android.sdk.api.session.events.model.EventType

data class NotifiableMessageEvent(
        override val eventId: String,
        override val editedEventId: String?,
        override val canBeReplaced: Boolean,
        val noisy: Boolean,
        val timestamp: Long,
        val senderName: String?,
        val senderId: String?,
        val body: String?,
        // We cannot use Uri? type here, as that could trigger a
        // NotSerializableException when persisting this to storage
        val imageUriString: String?,
        val roomId: String,
        val threadId: String?,
        val roomName: String?,
        val roomIsDirect: Boolean = false,
        val roomAvatarPath: String? = null,
        val senderAvatarPath: String? = null,
        val matrixID: String? = null,
        val soundName: String? = null,
        // This is used for >N notification, as the result of a smart reply
        val outGoingMessage: Boolean = false,
        val outGoingMessageFailed: Boolean = false,
        override val isRedacted: Boolean = false,
        override val isUpdated: Boolean = false
) : NotifiableEvent {

    val type: String = EventType.MESSAGE
    val description: String = body ?: ""
    val title: String = senderName ?: ""

    val imageUri: Uri?
        get() = imageUriString?.let { Uri.parse(it) }
}

fun NotifiableMessageEvent.shouldIgnoreMessageEventInRoom(currentRoomId: String?, currentThreadId: String?): Boolean {
    return when (currentRoomId) {
        null -> false
        else -> roomId == currentRoomId && threadId == currentThreadId
    }
}
