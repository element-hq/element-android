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

package im.vector.app.features.roomprofile.polls

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import im.vector.app.features.roomprofile.polls.active.RoomActivePollsFragment
import im.vector.app.features.roomprofile.polls.ended.RoomEndedPollsFragment

class RoomPollsPagerAdapter(
        private val fragment: Fragment
) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = RoomPollsType.values().size

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            RoomPollsType.ACTIVE.ordinal -> instantiateFragment(RoomActivePollsFragment::class.java.name)
            RoomPollsType.ENDED.ordinal -> instantiateFragment(RoomEndedPollsFragment::class.java.name)
            else -> throw IllegalArgumentException("position should be between 0 and ${itemCount - 1}, while it was $position")
        }
    }

    private fun instantiateFragment(fragmentName: String): Fragment {
        return fragment.childFragmentManager.fragmentFactory.instantiate(fragment.requireContext().classLoader, fragmentName)
    }
}
