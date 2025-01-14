/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory

/**
 * Join state of a room.
 */
enum class JoinState {
    NOT_JOINED,
    JOINING,
    JOINING_ERROR,

    // Room is joined and this is confirmed by the sync
    JOINED
}
