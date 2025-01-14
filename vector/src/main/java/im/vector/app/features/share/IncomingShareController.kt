/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.share

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Incomplete
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.list.RoomSummaryItemFactory
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class IncomingShareController @Inject constructor(
        private val roomSummaryItemFactory: RoomSummaryItemFactory,
        private val stringProvider: StringProvider
) : TypedEpoxyController<IncomingShareViewState>() {

    interface Callback {
        fun onRoomClicked(roomSummary: RoomSummary)
        fun onRoomLongClicked(roomSummary: RoomSummary): Boolean
    }

    var callback: Callback? = null

    override fun buildModels(data: IncomingShareViewState) {
        val host = this
        if (data.sharedData == null || data.filteredRoomSummaries is Incomplete) {
            loadingItem {
                id("loading")
            }
            return
        }
        val roomSummaries = data.filteredRoomSummaries()
        if (roomSummaries.isNullOrEmpty()) {
            noResultItem {
                id("no_result")
                text(host.stringProvider.getString(CommonStrings.no_result_placeholder))
            }
        } else {
            roomSummaries.forEach { roomSummary ->
                roomSummaryItemFactory
                        .createRoomItem(
                                roomSummary,
                                data.selectedRoomIds,
                                RoomListDisplayMode.FILTERED,
                                singleLineLastEvent = false,
                                callback?.let { it::onRoomClicked },
                                callback?.let { it::onRoomLongClicked }
                        )
                        .addTo(this)
            }
        }
    }
}
