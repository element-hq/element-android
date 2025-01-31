/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.accountdata

object RoomAccountDataTypes {
    const val EVENT_TYPE_VIRTUAL_ROOM = "im.vector.is_virtual_room"
    const val EVENT_TYPE_TAG = "m.tag"
    const val EVENT_TYPE_FULLY_READ = "m.fully_read"
    const val EVENT_TYPE_SPACE_ORDER = "org.matrix.msc3230.space_order" // m.space_order
    const val EVENT_TYPE_TAGGED_EVENTS = "m.tagged_events"
}
