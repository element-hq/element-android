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
import java.lang.IllegalArgumentException

enum class MatrixToSource {
    LINK,
    NOTIFICATION,
    TIMELINE,
    SPACE_EXPLORE,
    ROOM_LIST,
    USER_CODE
}

fun MatrixToSource.toJoinedRoomTrigger(): JoinedRoom.Trigger {
    return when(this) {
        MatrixToSource.LINK          -> JoinedRoom.Trigger.MobilePermalink
        MatrixToSource.NOTIFICATION  -> JoinedRoom.Trigger.Notification
        MatrixToSource.TIMELINE      -> JoinedRoom.Trigger.Timeline
        MatrixToSource.SPACE_EXPLORE -> JoinedRoom.Trigger.MobileExploreRooms
        MatrixToSource.ROOM_LIST     -> JoinedRoom.Trigger.RoomDirectory
        MatrixToSource.USER_CODE     -> throw IllegalArgumentException("can't map source to join room trigger")
    }
}

fun MatrixToSource.toViewRoomTrigger(): ViewRoom.Trigger {
    return when(this) {
        MatrixToSource.LINK          -> ViewRoom.Trigger.MobilePermalink
        MatrixToSource.NOTIFICATION  -> ViewRoom.Trigger.Notification
        MatrixToSource.TIMELINE      -> ViewRoom.Trigger.Timeline
        MatrixToSource.SPACE_EXPLORE -> ViewRoom.Trigger.MobileExploreRooms
        MatrixToSource.ROOM_LIST     -> ViewRoom.Trigger.RoomDirectory
        MatrixToSource.USER_CODE     -> throw IllegalArgumentException("can't map source to join room trigger")
    }
}

