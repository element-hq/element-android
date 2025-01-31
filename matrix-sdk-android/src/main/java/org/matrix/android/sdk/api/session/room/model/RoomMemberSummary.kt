/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model

import org.matrix.android.sdk.api.session.presence.model.UserPresence

/**
 * Class representing a simplified version of EventType.STATE_ROOM_MEMBER state event content.
 */
data class RoomMemberSummary constructor(
        val membership: Membership,
        val userId: String,
        val userPresence: UserPresence? = null,
        val displayName: String? = null,
        val avatarUrl: String? = null
)
