/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.replaceChildFragment
import im.vector.riotredesign.core.platform.RiotFragment
import im.vector.riotredesign.features.home.group.GroupListFragment
import im.vector.riotredesign.features.home.room.list.RoomListFragment

class HomeDrawerFragment : RiotFragment() {

    companion object {

        fun newInstance(): HomeDrawerFragment {
            return HomeDrawerFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home_drawer, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null) {
            val groupListFragment = GroupListFragment.newInstance()
            replaceChildFragment(groupListFragment, R.id.groupListFragmentContainer)
            val roomListFragment = RoomListFragment.newInstance()
            replaceChildFragment(roomListFragment, R.id.roomListFragmentContainer)
        }
    }

}