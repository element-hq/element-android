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

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.room.model.RoomSummary

class RoomSummaryController(private val callback: Callback? = null
) : TypedEpoxyController<RoomListViewState>() {

    private var isDirectRoomsExpanded = true
    private var isGroupRoomsExpanded = true

    override fun buildModels(viewState: RoomListViewState) {
        val roomSummaries = viewState.asyncRooms()
        RoomCategoryItem(
                title = "DIRECT MESSAGES",
                isExpanded = isDirectRoomsExpanded,
                listener = {
                    isDirectRoomsExpanded = !isDirectRoomsExpanded
                    setData(viewState)
                }
        )
                .id("direct_messages")
                .addTo(this)

        if (isDirectRoomsExpanded) {
            buildRoomModels(roomSummaries?.directRooms ?: emptyList(), viewState.selectedRoomId)
        }

        RoomCategoryItem(
                title = "GROUPS",
                isExpanded = isGroupRoomsExpanded,
                listener = {
                    isGroupRoomsExpanded = !isGroupRoomsExpanded
                    setData(viewState)
                }
        )
                .id("group_messages")
                .addTo(this)

        if (isGroupRoomsExpanded) {
            buildRoomModels(roomSummaries?.groupRooms ?: emptyList(), viewState.selectedRoomId)
        }

    }

    private fun buildRoomModels(summaries: List<RoomSummary>, selectedRoomId: String?) {
        summaries.forEach { roomSummary ->
            val unreadCount = roomSummary.notificationCount
            val showHighlighted = roomSummary.highlightCount > 0
            val isSelected = roomSummary.roomId == selectedRoomId
            RoomSummaryItem(
                    roomName = roomSummary.displayName,
                    avatarUrl = roomSummary.avatarUrl,
                    isSelected = isSelected,
                    showHighlighted = showHighlighted,
                    unreadCount = unreadCount,
                    listener = { callback?.onRoomSelected(roomSummary) }
            )
                    .id(roomSummary.roomId)
                    .addTo(this)
        }
    }

    interface Callback {
        fun onRoomSelected(room: RoomSummary)
    }

}
