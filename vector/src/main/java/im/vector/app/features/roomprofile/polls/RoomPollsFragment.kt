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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentRoomPollsBinding
import im.vector.app.features.roomprofile.RoomProfileArgs

@AndroidEntryPoint
class RoomPollsFragment : VectorBaseFragment<FragmentRoomPollsBinding>() {

    private val roomProfileArgs: RoomProfileArgs by args()

    private val viewModel: RoomPollsViewModel by fragmentViewModel()

    private var tabLayoutMediator: TabLayoutMediator? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomPollsBinding {
        return FragmentRoomPollsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupTabs()
    }

    override fun onDestroyView() {
        views.roomPollsViewPager.adapter = null
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
        super.onDestroyView()
    }

    private fun setupToolbar() {
        setupToolbar(views.roomPollsToolbar)
                .allowBack()
    }

    private fun setupTabs() {
        views.roomPollsViewPager.adapter = RoomPollsPagerAdapter(this)

        tabLayoutMediator = TabLayoutMediator(views.roomPollsTabs, views.roomPollsViewPager) { tab, position ->
            when (position) {
                RoomPollsType.ACTIVE.ordinal -> tab.text = getString(R.string.room_polls_active)
                RoomPollsType.ENDED.ordinal -> tab.text = getString(R.string.room_polls_ended)
            }
        }.also { it.attach() }
    }
}
