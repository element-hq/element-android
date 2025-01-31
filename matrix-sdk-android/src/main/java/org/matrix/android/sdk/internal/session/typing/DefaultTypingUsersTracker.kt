/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.typing

import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.typing.TypingUsersTracker
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class DefaultTypingUsersTracker @Inject constructor() : TypingUsersTracker {

    private val typingUsers = mutableMapOf<String, List<SenderInfo>>()

    /**
     * Set all currently typing users for a room (excluding yourself).
     */
    fun setTypingUsersFromRoom(roomId: String, senderInfoList: List<SenderInfo>) {
        val hasNewValue = typingUsers[roomId] != senderInfoList
        if (hasNewValue) {
            typingUsers[roomId] = senderInfoList
        }
    }

    override fun getTypingUsers(roomId: String): List<SenderInfo> {
        return typingUsers[roomId].orEmpty()
    }
}
