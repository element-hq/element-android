/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.invites

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.app.core.utils.createUIHandler
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.list.RoomListListener
import im.vector.app.features.home.room.list.RoomSummaryItemFactory
import im.vector.app.features.home.room.list.RoomSummaryPlaceHolderItem_
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class InvitesController @Inject constructor(
        private val roomSummaryItemFactory: RoomSummaryItemFactory,
) : PagedListEpoxyController<RoomSummary>(
        // Important it must match the PageList builder notify Looper
        modelBuildingHandler = createUIHandler()
) {

    var roomChangeMembershipStates: Map<String, ChangeMembershipState>? = null
        set(value) {
            field = value
            requestForcedModelBuild()
        }

    var listener: RoomListListener? = null

    override fun buildItemModel(currentPosition: Int, item: RoomSummary?): EpoxyModel<*> {
        item ?: return RoomSummaryPlaceHolderItem_().apply { id(currentPosition) }
        return roomSummaryItemFactory.create(item, roomChangeMembershipStates.orEmpty(), emptySet(), RoomListDisplayMode.ROOMS, listener)
    }
}
