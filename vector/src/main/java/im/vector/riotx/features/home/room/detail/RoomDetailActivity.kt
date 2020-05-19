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

package im.vector.riotx.features.home.room.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import im.vector.riotx.R
import im.vector.riotx.core.extensions.hideKeyboard
import im.vector.riotx.core.extensions.replaceFragment
import im.vector.riotx.core.platform.ToolbarConfigurable
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.features.home.room.breadcrumbs.BreadcrumbsFragment
import kotlinx.android.synthetic.main.activity_room_detail.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*

class RoomDetailActivity : VectorBaseActivity(), ToolbarConfigurable {

    override fun getLayoutRes() = R.layout.activity_room_detail

    private lateinit var sharedActionViewModel: RoomDetailSharedActionViewModel

    // Simple filter
    private var currentRoomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        waitingView = waiting_view
        if (isFirstCreation()) {
            val roomDetailArgs: RoomDetailArgs? = if (intent?.action == ACTION_ROOM_DETAILS_FROM_SHORTCUT) {
                RoomDetailArgs(roomId = intent?.extras?.getString(EXTRA_ROOM_ID)!!)
            } else {
                intent?.extras?.getParcelable(EXTRA_ROOM_DETAIL_ARGS)
            }

            if (roomDetailArgs == null) return

            currentRoomId = roomDetailArgs.roomId
            replaceFragment(R.id.roomDetailContainer, RoomDetailFragment::class.java, roomDetailArgs)
            replaceFragment(R.id.roomDetailDrawerContainer, BreadcrumbsFragment::class.java)
        }

        sharedActionViewModel = viewModelProvider.get(RoomDetailSharedActionViewModel::class.java)

        sharedActionViewModel
                .observe()
                .subscribe { sharedAction ->
                    when (sharedAction) {
                        is RoomDetailSharedAction.SwitchToRoom -> switchToRoom(sharedAction)
                    }
                }
                .disposeOnDestroy()

        drawerLayout.addDrawerListener(drawerListener)
    }

    private fun switchToRoom(switchToRoom: RoomDetailSharedAction.SwitchToRoom) {
        drawerLayout.closeDrawer(GravityCompat.START)
        // Do not replace the Fragment if it's the same roomId
        if (currentRoomId != switchToRoom.roomId) {
            currentRoomId = switchToRoom.roomId
            replaceFragment(R.id.roomDetailContainer, RoomDetailFragment::class.java, RoomDetailArgs(switchToRoom.roomId))
        }
    }

    override fun onDestroy() {
        drawerLayout.removeDrawerListener(drawerListener)
        super.onDestroy()
    }

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar)
    }

    private val drawerListener = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerStateChanged(newState: Int) {
            hideKeyboard()

            if (!drawerLayout.isDrawerOpen(GravityCompat.START) && newState == DrawerLayout.STATE_DRAGGING) {
                // User is starting to open the drawer, scroll the list to op
                scrollBreadcrumbsToTop()
            }
        }
    }

    private fun scrollBreadcrumbsToTop() {
        supportFragmentManager.fragments.filterIsInstance<BreadcrumbsFragment>()
                .forEach { it.scrollToTop() }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    companion object {

        const val EXTRA_ROOM_DETAIL_ARGS = "EXTRA_ROOM_DETAIL_ARGS"
        const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        const val ACTION_ROOM_DETAILS_FROM_SHORTCUT = "ROOM_DETAILS_FROM_SHORTCUT"

        fun newIntent(context: Context, roomDetailArgs: RoomDetailArgs): Intent {
            return Intent(context, RoomDetailActivity::class.java).apply {
                putExtra(EXTRA_ROOM_DETAIL_ARGS, roomDetailArgs)
            }
        }

        fun shortcutIntent(context: Context, roomId: String): Intent {
            return Intent(context, RoomDetailActivity::class.java).apply {
                action = ACTION_ROOM_DETAILS_FROM_SHORTCUT
                putExtra(EXTRA_ROOM_ID, roomId)
            }
        }
    }
}
