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

import android.view.Gravity
import androidx.fragment.app.FragmentManager
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.addFragmentToBackstack
import im.vector.riotredesign.core.extensions.replaceFragment
import im.vector.riotredesign.features.home.room.detail.RoomDetailArgs
import im.vector.riotredesign.features.home.room.detail.RoomDetailFragment
import kotlinx.android.synthetic.main.activity_home.*
import timber.log.Timber

class HomeNavigator {

    var activity: HomeActivity? = null

    private var currentRoomId: String? = null

    fun openRoomDetail(roomId: String,
                       eventId: String?,
                       addToBackstack: Boolean = false) {
        Timber.v("Open room detail $roomId - $eventId - $addToBackstack")
        if (!addToBackstack && isRoomOpened(roomId)) {
            return
        }
        activity?.let {
            val args = RoomDetailArgs(roomId, eventId)
            val roomDetailFragment = RoomDetailFragment.newInstance(args)
            it.drawerLayout?.closeDrawer(Gravity.LEFT)
            if (addToBackstack) {
                it.addFragmentToBackstack(roomDetailFragment, R.id.homeDetailFragmentContainer, roomId)
            } else {
                currentRoomId = roomId
                clearBackStack(it.supportFragmentManager)
                it.replaceFragment(roomDetailFragment, R.id.homeDetailFragmentContainer)
            }
        }
    }

    fun openGroupDetail(groupId: String) {
        Timber.v("Open group detail $groupId")
    }

    fun openUserDetail(userId: String) {
        Timber.v("Open user detail $userId")
    }

    fun isRoomOpened(roomId: String): Boolean {
        return currentRoomId == roomId
    }

    private fun clearBackStack(fragmentManager: FragmentManager) {
        if (fragmentManager.backStackEntryCount > 0) {
            val first = fragmentManager.getBackStackEntryAt(0)
            fragmentManager.popBackStack(first.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

}