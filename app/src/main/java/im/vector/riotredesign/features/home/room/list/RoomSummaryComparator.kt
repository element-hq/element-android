/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.features.home.room.list

import im.vector.matrix.android.api.session.room.model.RoomSummary

class RoomSummaryComparator
    : Comparator<RoomSummary> {

    override fun compare(leftRoomSummary: RoomSummary?, rightRoomSummary: RoomSummary?): Int {
        val retValue: Int
        var leftHighlightCount = 0
        var rightHighlightCount = 0
        var leftNotificationCount = 0
        var rightNotificationCount = 0
        var rightTimestamp = 0L
        var leftTimestamp = 0L

        if (null != leftRoomSummary) {
            leftHighlightCount = leftRoomSummary.highlightCount
            leftNotificationCount = leftRoomSummary.notificationCount
            leftTimestamp = leftRoomSummary.lastMessage?.originServerTs ?: 0
        }
        if (null != rightRoomSummary) {
            rightHighlightCount = rightRoomSummary.highlightCount
            rightNotificationCount = rightRoomSummary.notificationCount
            rightTimestamp = rightRoomSummary.lastMessage?.originServerTs ?: 0
        }

        if (leftRoomSummary?.lastMessage == null) {
            retValue = 1
        } else if (rightRoomSummary?.lastMessage == null) {
            retValue = -1
        } else if (rightHighlightCount > 0 && leftHighlightCount == 0) {
            retValue = 1
        } else if (rightHighlightCount == 0 && leftHighlightCount > 0) {
            retValue = -1
        } else if (rightNotificationCount > 0 && leftNotificationCount == 0) {
            retValue = 1
        } else if (rightNotificationCount == 0 && leftNotificationCount > 0) {
            retValue = -1
        } else {
            val deltaTimestamp = rightTimestamp - leftTimestamp
            if (deltaTimestamp > 0) {
                retValue = 1
            } else if (deltaTimestamp < 0) {
                retValue = -1
            } else {
                retValue = 0
            }
        }
        return retValue

    }

}