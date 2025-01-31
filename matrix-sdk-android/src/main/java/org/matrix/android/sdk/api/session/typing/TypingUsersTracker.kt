/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.typing

import org.matrix.android.sdk.api.session.room.sender.SenderInfo

/**
 * Responsible for tracking typing users from each room.
 * It's ephemeral data and it's only saved in memory.
 */
interface TypingUsersTracker {

    /**
     * Returns the sender information of all currently typing users in a room, excluding yourself.
     */
    fun getTypingUsers(roomId: String): List<SenderInfo>
}
