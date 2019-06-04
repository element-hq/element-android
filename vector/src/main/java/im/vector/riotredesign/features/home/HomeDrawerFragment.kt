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
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.observeK
import im.vector.riotredesign.core.extensions.replaceChildFragment
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.features.home.group.GroupListFragment
import kotlinx.android.synthetic.main.fragment_home_drawer.*
import org.koin.android.ext.android.inject

class HomeDrawerFragment : VectorBaseFragment() {

    companion object {

        fun newInstance(): HomeDrawerFragment {
            return HomeDrawerFragment()
        }
    }

    val session by inject<Session>()

    override fun getLayoutResId() = R.layout.fragment_home_drawer

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null) {
            val groupListFragment = GroupListFragment.newInstance()
            replaceChildFragment(groupListFragment, R.id.homeDrawerGroupListContainer)
        }

        session.observeUser(session.sessionParams.credentials.userId).observeK(this) { user ->
            if (user != null) {
                AvatarRenderer.render(user.avatarUrl, user.userId, user.displayName, homeDrawerHeaderAvatarView)
                homeDrawerUsernameView.text = user.displayName
                homeDrawerUserIdView.text = user.userId
            }
        }
        homeDrawerHeaderSettingsView.setOnClickListener {
            navigator.openSettings()
        }
    }
}