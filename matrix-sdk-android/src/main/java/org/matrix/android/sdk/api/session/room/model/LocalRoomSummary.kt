/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model

import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams

/**
 * This class holds some data of a local room.
 * It can be retrieved by [org.matrix.android.sdk.api.session.room.Room] and [org.matrix.android.sdk.api.session.room.RoomService]
 */
data class LocalRoomSummary(
        /**
         * The roomId of the room.
         */
        val roomId: String,
        /**
         * The room summary of the room.
         */
        val roomSummary: RoomSummary?,
        /**
         * The creation params attached to the room.
         */
        val createRoomParams: CreateRoomParams?,
        /**
         * The roomId of the created room (ie. created on the server), if any.
         */
        val replacementRoomId: String?,
        /**
         * The creation state of the room.
         */
        val creationState: LocalRoomCreationState,
)
