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

package im.vector.riotx.features.home.room.list

import androidx.annotation.StringRes
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotx.core.resources.StringProvider
import javax.inject.Inject

class RoomSummaryController @Inject constructor(private val stringProvider: StringProvider,
                                                private val roomSummaryItemFactory: RoomSummaryItemFactory
) : TypedEpoxyController<RoomListViewState>() {

    var listener: Listener? = null

    override fun buildModels(viewState: RoomListViewState) {
        val roomSummaries = viewState.asyncFilteredRooms()
        roomSummaries?.forEach { (category, summaries) ->
            if (summaries.isEmpty()) {
                return@forEach
            } else {
                val isExpanded = viewState.isCategoryExpanded(category)
                buildRoomCategory(viewState, summaries, category.titleRes, viewState.isCategoryExpanded(category)) {
                    listener?.onToggleRoomCategory(category)
                }
                if (isExpanded) {
                    buildRoomModels(summaries,
                            viewState.joiningRoomsIds,
                            viewState.joiningErrorRoomsIds,
                            viewState.rejectingRoomsIds,
                            viewState.rejectingErrorRoomsIds)
                }
            }
        }
    }

    private fun buildRoomCategory(viewState: RoomListViewState,
                                  summaries: List<RoomSummary>,
                                  @StringRes titleRes: Int,
                                  isExpanded: Boolean,
                                  mutateExpandedState: () -> Unit) {
        if (summaries.isEmpty()) {
            return
        }
        //TODO should add some business logic later
        val unreadCount = if (summaries.isEmpty()) {
            0
        } else {
            summaries.map { it.notificationCount }.sumBy { i -> i }
        }
        val showHighlighted = summaries.any { it.highlightCount > 0 }
        roomCategoryItem {
            id(titleRes)
            title(stringProvider.getString(titleRes).toUpperCase())
            expanded(isExpanded)
            unreadCount(unreadCount)
            showHighlighted(showHighlighted)
            listener {
                mutateExpandedState()
                setData(viewState)
            }
        }
    }

    private fun buildRoomModels(summaries: List<RoomSummary>,
                                joiningRoomsIds: Set<String>,
                                joiningErrorRoomsIds: Set<String>,
                                rejectingRoomsIds: Set<String>,
                                rejectingErrorRoomsIds: Set<String>) {
        summaries.forEach { roomSummary ->
            roomSummaryItemFactory
                    .create(roomSummary, joiningRoomsIds, joiningErrorRoomsIds, rejectingRoomsIds, rejectingErrorRoomsIds, listener)
                    .addTo(this)
        }
    }

    interface Listener {
        fun onToggleRoomCategory(roomCategory: RoomCategory)
        fun onRoomSelected(room: RoomSummary)
        fun onRejectRoomInvitation(room: RoomSummary)
        fun onAcceptRoomInvitation(room: RoomSummary)
    }

}
