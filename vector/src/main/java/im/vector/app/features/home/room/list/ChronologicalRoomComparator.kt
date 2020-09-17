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

package im.vector.app.features.home.room.list

import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class ChronologicalRoomComparator @Inject constructor() : Comparator<RoomSummary> {

    override fun compare(leftRoomSummary: RoomSummary?, rightRoomSummary: RoomSummary?): Int {
        return when {
            rightRoomSummary?.latestPreviewableEvent?.root == null -> -1
            leftRoomSummary?.latestPreviewableEvent?.root == null  -> 1
            else                                                   -> {
                val rightTimestamp = rightRoomSummary.latestPreviewableEvent?.root?.originServerTs ?: 0
                val leftTimestamp = leftRoomSummary.latestPreviewableEvent?.root?.originServerTs ?: 0

                val deltaTimestamp = rightTimestamp - leftTimestamp

                when {
                    deltaTimestamp > 0 -> 1
                    deltaTimestamp < 0 -> -1
                    else               -> 0
                }
            }
        }
    }
}
