/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.settings.historyvisibility

import im.vector.app.core.ui.bottomsheet.BottomSheetGenericController
import im.vector.app.features.home.room.detail.timeline.format.RoomHistoryVisibilityFormatter
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import javax.inject.Inject

class RoomHistoryVisibilityController @Inject constructor(
        private val historyVisibilityFormatter: RoomHistoryVisibilityFormatter
) : BottomSheetGenericController<RoomHistoryVisibilityState, BottomSheetRoomHistoryVisibilityAction>() {

    override fun getActions(state: RoomHistoryVisibilityState): List<BottomSheetRoomHistoryVisibilityAction> {
        return listOf(
                RoomHistoryVisibility.WORLD_READABLE,
                RoomHistoryVisibility.SHARED,
                RoomHistoryVisibility.INVITED,
                RoomHistoryVisibility.JOINED
        )
                .map { roomHistoryVisibility ->
                    BottomSheetRoomHistoryVisibilityAction(
                            roomHistoryVisibility = roomHistoryVisibility,
                            title = historyVisibilityFormatter.getSetting(roomHistoryVisibility),
                            iconResId = 0,
                            isSelected = roomHistoryVisibility == state.currentRoomHistoryVisibility,
                            destructive = false
                    )
                }
    }
}
