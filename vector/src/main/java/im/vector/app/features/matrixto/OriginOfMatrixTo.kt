/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.matrixto

import im.vector.app.features.analytics.plan.JoinedRoom
import im.vector.app.features.analytics.plan.ViewRoom

enum class OriginOfMatrixTo {
    LINK,
    NOTIFICATION,
    TIMELINE,
    SPACE_EXPLORE,
    ROOM_LIST,
    USER_CODE
}

fun OriginOfMatrixTo.toJoinedRoomTrigger(): JoinedRoom.Trigger? {
    return when (this) {
        OriginOfMatrixTo.LINK -> JoinedRoom.Trigger.MobilePermalink
        OriginOfMatrixTo.NOTIFICATION -> JoinedRoom.Trigger.Notification
        OriginOfMatrixTo.TIMELINE -> JoinedRoom.Trigger.Timeline
        OriginOfMatrixTo.SPACE_EXPLORE -> JoinedRoom.Trigger.SpaceHierarchy
        OriginOfMatrixTo.ROOM_LIST -> JoinedRoom.Trigger.RoomDirectory
        OriginOfMatrixTo.USER_CODE -> null
    }
}

fun OriginOfMatrixTo.toViewRoomTrigger(): ViewRoom.Trigger? {
    return when (this) {
        OriginOfMatrixTo.LINK -> ViewRoom.Trigger.MobilePermalink
        OriginOfMatrixTo.NOTIFICATION -> ViewRoom.Trigger.Notification
        OriginOfMatrixTo.TIMELINE -> ViewRoom.Trigger.Timeline
        OriginOfMatrixTo.SPACE_EXPLORE -> ViewRoom.Trigger.SpaceHierarchy
        OriginOfMatrixTo.ROOM_LIST -> ViewRoom.Trigger.RoomDirectory
        OriginOfMatrixTo.USER_CODE -> null
    }
}
