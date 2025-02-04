/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.extensions

import im.vector.app.features.analytics.plan.JoinedRoom
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoom

fun Int?.toAnalyticsRoomSize(): JoinedRoom.RoomSize {
    return when (this) {
        null,
        2 -> JoinedRoom.RoomSize.Two
        in 3..10 -> JoinedRoom.RoomSize.ThreeToTen
        in 11..100 -> JoinedRoom.RoomSize.ElevenToOneHundred
        in 101..1000 -> JoinedRoom.RoomSize.OneHundredAndOneToAThousand
        else -> JoinedRoom.RoomSize.MoreThanAThousand
    }
}

fun RoomSummary?.toAnalyticsJoinedRoom(trigger: JoinedRoom.Trigger?): JoinedRoom {
    return JoinedRoom(
            isDM = this?.isDirect.orFalse(),
            isSpace = this?.roomType == RoomType.SPACE,
            roomSize = this?.joinedMembersCount?.toAnalyticsRoomSize() ?: JoinedRoom.RoomSize.Two,
            trigger = trigger
    )
}

fun PublicRoom.toAnalyticsJoinedRoom(trigger: JoinedRoom.Trigger?): JoinedRoom {
    return JoinedRoom(
            isDM = false,
            isSpace = false,
            roomSize = numJoinedMembers.toAnalyticsRoomSize(),
            trigger = trigger
    )
}
