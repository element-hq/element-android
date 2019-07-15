/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.home.room.filtered

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.replaceFragment
import im.vector.riotx.core.platform.ToolbarConfigurable
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.features.home.room.list.RoomListFragment
import im.vector.riotx.features.home.room.list.RoomListParams
import kotlinx.android.synthetic.main.activity_filtered_rooms.*

class FilteredRoomsActivity : VectorBaseActivity(), ToolbarConfigurable {

    private lateinit var roomListFragment: RoomListFragment

    override fun getLayoutRes(): Int {
        return R.layout.activity_filtered_rooms
    }

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFirstCreation()) {
            roomListFragment = RoomListFragment.newInstance(RoomListParams(RoomListFragment.DisplayMode.FILTERED))
            replaceFragment(roomListFragment, R.id.filteredRoomsFragmentContainer, FRAGMENT_TAG)
        } else {
            roomListFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as RoomListFragment
        }

        filteredRoomsSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // TODO Create a viewModel and remove this public fun
                roomListFragment.filterRoomsWith(newText)
                return true
            }
        })

        // Open the keyboard immediately
        filteredRoomsSearchView.requestFocus()
    }

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar)
    }

    companion object {
        private const val FRAGMENT_TAG = "RoomListFragment"

        fun newIntent(context: Context): Intent {
            return Intent(context, FilteredRoomsActivity::class.java)
        }
    }
}
