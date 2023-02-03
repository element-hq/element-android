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

package im.vector.app.features.home.room.list.home

import androidx.paging.PagedList
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.app.core.platform.StateView
import im.vector.app.core.utils.createUIHandler
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.list.RoomListListener
import im.vector.app.features.home.room.list.RoomSummaryItemFactory
import im.vector.app.features.home.room.list.RoomSummaryPlaceHolderItem_
import im.vector.app.features.settings.FontScalePreferences
import org.matrix.android.sdk.api.session.room.ResultBoundaries
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class HomeFilteredRoomsController @Inject constructor(
        private val roomSummaryItemFactory: RoomSummaryItemFactory,
        fontScalePreferences: FontScalePreferences,
        roomSummaryRoomListDiffCallback: RoomSummaryRoomListDiffCallback,
) : PagedListEpoxyController<RoomSummary>(
        // Important it must match the PageList builder notify Looper
        modelBuildingHandler = createUIHandler(),
        itemDiffCallback = roomSummaryRoomListDiffCallback,
) {

    private var roomChangeMembershipStates: Map<String, ChangeMembershipState>? = null
        set(value) {
            field = value
            // ideally we could search for visible models and update only those
            requestForcedModelBuild()
        }

    var listener: RoomListListener? = null

    private var emptyStateData: StateView.State.Empty? = null

    private val shouldUseSingleLine: Boolean

    var initialLoadOccurred = false

    init {
        val fontScale = fontScalePreferences.getResolvedFontScaleValue()
        shouldUseSingleLine = fontScale.scale > FontScalePreferences.SCALE_LARGE
    }

    fun submitRoomsList(roomsList: PagedList<RoomSummary>) {
        submitList(roomsList)
        // If room is empty we may have a new EmptyState to display
        if (roomsList.isEmpty()) {
            requestForcedModelBuild()
        }
    }

    override fun addModels(models: List<EpoxyModel<*>>) {
        val emptyStateData = this.emptyStateData
        if (models.isEmpty() && emptyStateData != null) {
            roomListEmptyItem {
                id("state_item")
                emptyData(emptyStateData)
            }
        } else {
            super.addModels(models)
        }
    }

    fun boundaryChange(boundary: ResultBoundaries) {
        // Sometimes the room stays on empty state, need
        val boundaryHasLoadedSomething = boundary.frontLoaded || boundary.zeroItemLoaded
        if (initialLoadOccurred != boundaryHasLoadedSomething) {
            initialLoadOccurred = boundaryHasLoadedSomething
            requestForcedModelBuild()
        }
    }

    fun submitEmptyStateData(state: StateView.State.Empty?) {
        this.emptyStateData = state
    }

    override fun buildItemModel(currentPosition: Int, item: RoomSummary?): EpoxyModel<*> {
        return if (item == null) {
            val host = this
            RoomSummaryPlaceHolderItem_().apply {
                id(currentPosition)
                useSingleLineForLastEvent(host.shouldUseSingleLine)
            }
        } else {
            roomSummaryItemFactory.create(
                    roomSummary = item,
                    roomChangeMembershipStates = roomChangeMembershipStates.orEmpty(),
                    selectedRoomIds = emptySet(),
                    displayMode = RoomListDisplayMode.ROOMS,
                    listener = listener,
                    singleLineLastEvent = shouldUseSingleLine
            )
        }
    }
}
