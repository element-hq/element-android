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
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.replaceFragment
import kotlinx.android.synthetic.main.activity_home.*
import timber.log.Timber

class HomeNavigator {

    var activity: HomeActivity? = null

    fun openSelectedGroup(groupSummary: GroupSummary) {
        Timber.v("Open selected group ${groupSummary.groupId}")
        activity?.let {
            it.drawerLayout?.closeDrawer(GravityCompat.START)

            val args = HomeDetailParams(groupSummary.groupId, groupSummary.displayName, groupSummary.avatarUrl)
            val homeDetailFragment = HomeDetailFragment.newInstance(args)
            it.replaceFragment(homeDetailFragment, R.id.homeDetailFragmentContainer)
        }
    }
}
