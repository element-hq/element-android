/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.filtered

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.SearchView
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityFilteredRoomsBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.home.room.list.RoomListFragment
import im.vector.app.features.home.room.list.RoomListParams

@AndroidEntryPoint
class FilteredRoomsActivity : VectorBaseActivity<ActivityFilteredRoomsBinding>() {

    private val roomListFragment: RoomListFragment?
        get() {
            return supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? RoomListFragment
        }

    override fun getBinding() = ActivityFilteredRoomsBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.RoomFilter
        setupToolbar(views.filteredRoomsToolbar)
                .allowBack()
        if (isFirstCreation()) {
            val params = RoomListParams(RoomListDisplayMode.FILTERED)
            replaceFragment(views.filteredRoomsFragmentContainer, RoomListFragment::class.java, params, FRAGMENT_TAG)
        }
        views.filteredRoomsSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                roomListFragment?.filterRoomsWith(newText)
                return true
            }
        })
        // Open the keyboard immediately
        views.filteredRoomsSearchView.requestFocus()
    }

    companion object {
        private const val FRAGMENT_TAG = "RoomListFragment"

        fun newIntent(context: Context): Intent {
            return Intent(context, FilteredRoomsActivity::class.java)
        }
    }
}
