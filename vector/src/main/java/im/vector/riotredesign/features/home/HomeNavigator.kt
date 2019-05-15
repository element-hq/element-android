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

import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentManager
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.replaceFragment
import im.vector.riotredesign.features.home.group.SelectedGroupFragment
import im.vector.riotredesign.features.home.group.SelectedGroupParams
import im.vector.riotredesign.features.home.room.detail.RoomDetailActivity
import im.vector.riotredesign.features.home.room.detail.RoomDetailArgs
import kotlinx.android.synthetic.main.activity_home.*
import timber.log.Timber

class HomeNavigator {

    var activity: HomeActivity? = null

    private var rootRoomId: String? = null

    fun openSelectedGroup(groupSummary: GroupSummary) {
        Timber.v("Open selected group ${groupSummary.groupId}")
        activity?.let {
            val args = SelectedGroupParams(groupSummary.groupId, groupSummary.displayName, groupSummary.avatarUrl)
            val selectedGroupFragment = SelectedGroupFragment.newInstance(args)
            it.drawerLayout?.closeDrawer(GravityCompat.START)
            it.replaceFragment(selectedGroupFragment, R.id.homeDetailFragmentContainer)
        }
    }

    fun openRoomDetail(roomId: String,
                       eventId: String?) {
        Timber.v("Open room detail $roomId - $eventId")
        activity?.let {
            //TODO enable eventId permalink. It doesn't work enough at the moment.
            it.drawerLayout?.closeDrawer(GravityCompat.START)
            val args = RoomDetailArgs(roomId)
            val roomDetailIntent = RoomDetailActivity.newIntent(it, args)
            it.startActivity(roomDetailIntent)
        }
    }

    fun openGroupDetail(groupId: String) {
        Timber.v("Open group detail $groupId")
    }

    fun openUserDetail(userId: String) {
        Timber.v("Open user detail $userId")
    }

    // Private Methods *****************************************************************************

    private fun clearBackStack(fragmentManager: FragmentManager) {
        if (fragmentManager.backStackEntryCount > 0) {
            val first = fragmentManager.getBackStackEntryAt(0)
            fragmentManager.popBackStack(first.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    private fun isRoot(roomId: String): Boolean {
        return rootRoomId == roomId
    }

}