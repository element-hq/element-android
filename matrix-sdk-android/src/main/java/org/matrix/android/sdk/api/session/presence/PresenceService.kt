/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.presence

import org.matrix.android.sdk.api.session.presence.model.PresenceEnum
import org.matrix.android.sdk.api.session.presence.model.UserPresence

/**
 * This interface defines methods for handling user presence information.
 */
interface PresenceService {
    /**
     * Update the presence status for the current user.
     * @param presence the new presence state
     * @param statusMsg the status message to attach to this state
     */
    suspend fun setMyPresence(presence: PresenceEnum, statusMsg: String? = null)

    /**
     * Fetch the given user's presence state.
     * @param userId the userId whose presence state to get.
     */
    suspend fun fetchPresence(userId: String): UserPresence

    // TODO Add live data (of Flow) of the presence of a userId
}
