/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.pushers.model

import org.matrix.android.sdk.api.MatrixPatterns

/**
 * In this case, the format is:
 * <pre>
 * {
 *     "event_id":"$anEventId",
 *     "room_id":"!aRoomId",
 *     "unread":"1",
 *     "prio":"high"
 * }
 * </pre>
 * .
 */
data class PushDataFcm(
        val eventId: String?,
        val roomId: String?,
        var unread: Int?,
)

fun PushDataFcm.toPushData() = PushData(
        eventId = eventId?.takeIf { MatrixPatterns.isEventId(it) },
        roomId = roomId?.takeIf { MatrixPatterns.isRoomId(it) },
        unread = unread
)
