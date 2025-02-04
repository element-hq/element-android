/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.ended

import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.features.roomprofile.polls.RoomPollsType
import im.vector.app.features.roomprofile.polls.list.ui.RoomPollsListFragment
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings

@AndroidEntryPoint
class RoomEndedPollsFragment : RoomPollsListFragment() {

    override fun getEmptyListTitle(canLoadMore: Boolean, nbLoadedDays: Int): String {
        return if (canLoadMore) {
            stringProvider.getQuantityString(CommonPlurals.room_polls_ended_no_item_for_loaded_period, nbLoadedDays, nbLoadedDays)
        } else {
            getString(CommonStrings.room_polls_ended_no_item)
        }
    }

    override fun getRoomPollsType(): RoomPollsType {
        return RoomPollsType.ENDED
    }
}
