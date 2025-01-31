/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.sync.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
sealed class LazyRoomSyncEphemeral {
    data class Parsed(val roomSyncEphemeral: RoomSyncEphemeral) : LazyRoomSyncEphemeral()
    object Stored : LazyRoomSyncEphemeral()
}
