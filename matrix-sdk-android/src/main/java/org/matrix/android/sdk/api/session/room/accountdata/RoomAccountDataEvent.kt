/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.accountdata

import org.matrix.android.sdk.api.session.events.model.Content

/**
 * This is a simplified Event with just a roomId, a type and a content.
 * Currently used types are defined in [RoomAccountDataTypes].
 */
data class RoomAccountDataEvent(
        val roomId: String,
        val type: String,
        val content: Content
)
