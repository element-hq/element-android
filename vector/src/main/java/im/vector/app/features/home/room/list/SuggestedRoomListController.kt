/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

class SuggestedRoomListController(
        private val roomSummaryItemFactory: RoomSummaryItemFactory
) : CollapsableTypedEpoxyController<SuggestedRoomInfo>() {

    var listener: RoomListListener? = null

    override fun buildModels(data: SuggestedRoomInfo?) {
        data?.rooms?.forEach { info ->
            add(roomSummaryItemFactory.createSuggestion(info, data.joinEcho, listener))
        }
    }
}
