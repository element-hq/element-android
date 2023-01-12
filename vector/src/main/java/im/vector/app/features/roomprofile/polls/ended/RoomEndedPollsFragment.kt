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

package im.vector.app.features.roomprofile.polls.ended

import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.features.roomprofile.polls.RoomPollsType
import im.vector.app.features.roomprofile.polls.list.ui.RoomPollsListFragment

@AndroidEntryPoint
class RoomEndedPollsFragment : RoomPollsListFragment() {

    override fun getEmptyListTitle(canLoadMore: Boolean, nbLoadedDays: Int): String {
        return if (canLoadMore) {
            stringProvider.getQuantityString(R.plurals.room_polls_ended_no_item_for_loaded_period, nbLoadedDays, nbLoadedDays)
        } else {
            getString(R.string.room_polls_ended_no_item)
        }
    }

    override fun getRoomPollsType(): RoomPollsType {
        return RoomPollsType.ENDED
    }
}
