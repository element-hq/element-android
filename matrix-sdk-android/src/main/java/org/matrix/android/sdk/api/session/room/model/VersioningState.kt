/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model

/**
 * Enum for the versioning state of a room.
 */
enum class VersioningState {
    /**
     * The room is not versioned.
     */
    NONE,

    /**
     * The room has been upgraded, but the new room is not joined yet.
     */
    UPGRADED_ROOM_NOT_JOINED,

    /**
     * The room has been upgraded, and the new room has been joined.
     */
    UPGRADED_ROOM_JOINED;

    fun isUpgraded() = this != NONE
}
