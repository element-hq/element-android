/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
