/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
