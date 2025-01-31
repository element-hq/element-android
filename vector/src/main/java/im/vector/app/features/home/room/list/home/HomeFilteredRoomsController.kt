/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.app.core.platform.StateView
import im.vector.app.core.utils.createUIHandler
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.list.RoomListListener
import im.vector.app.features.home.room.list.RoomSummaryItemFactory
import im.vector.app.features.home.room.list.RoomSummaryItemPlaceHolder_
import im.vector.app.features.settings.FontScalePreferences
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class HomeFilteredRoomsController @Inject constructor(
        private val roomSummaryItemFactory: RoomSummaryItemFactory,
        fontScalePreferences: FontScalePreferences
) : PagedListEpoxyController<RoomSummary>(
        // Important it must match the PageList builder notify Looper
        modelBuildingHandler = createUIHandler()
) {

    private var roomChangeMembershipStates: Map<String, ChangeMembershipState>? = null
        set(value) {
            field = value
            // ideally we could search for visible models and update only those
            requestForcedModelBuild()
        }

    var listener: RoomListListener? = null

    private var emptyStateData: StateView.State.Empty? = null
    private var currentState: StateView.State = StateView.State.Content

    private val shouldUseSingleLine: Boolean

    init {
        val fontScale = fontScalePreferences.getResolvedFontScaleValue()
        shouldUseSingleLine = fontScale.scale > FontScalePreferences.SCALE_LARGE
    }

    override fun addModels(models: List<EpoxyModel<*>>) {
        if (models.isEmpty() && emptyStateData != null) {
            emptyStateData?.let { emptyState ->
                roomListEmptyItem {
                    id("state_item")
                    emptyData(emptyState)
                }
                currentState = emptyState
            }
        } else {
            currentState = StateView.State.Content
            super.addModels(models)
        }
    }

    fun submitEmptyStateData(state: StateView.State.Empty?) {
        this.emptyStateData = state
    }

    override fun buildItemModel(currentPosition: Int, item: RoomSummary?): EpoxyModel<*> {
        return if (item == null) {
            val host = this
            RoomSummaryItemPlaceHolder_().apply {
                id(currentPosition)
                useSingleLineForLastEvent(host.shouldUseSingleLine)
            }
        } else {
            roomSummaryItemFactory.create(item, roomChangeMembershipStates.orEmpty(), emptySet(), RoomListDisplayMode.ROOMS, listener, shouldUseSingleLine)
        }
    }
}
