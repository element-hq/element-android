/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.tag

data class RoomTag(
        val name: String,
        val order: Double?
) {

    companion object {
        const val ROOM_TAG_FAVOURITE = "m.favourite"
        const val ROOM_TAG_LOW_PRIORITY = "m.lowpriority"
        const val ROOM_TAG_SERVER_NOTICE = "m.server_notice"
    }
}
