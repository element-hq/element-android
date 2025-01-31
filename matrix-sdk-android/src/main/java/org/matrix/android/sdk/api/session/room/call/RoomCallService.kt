/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.call

/**
 * This interface defines methods to handle calls in a room. It's implemented at the room level.
 */
interface RoomCallService {
    /**
     * Return true if calls (audio or video) can be performed on this Room.
     */
    fun canStartCall(): Boolean
}
