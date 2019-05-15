/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotredesign.features.home.group

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import com.airbnb.mvrx.args
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.replaceChildFragment
import im.vector.riotredesign.core.glide.GlideApp
import im.vector.riotredesign.core.platform.ToolbarConfigurable
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.features.home.AvatarRenderer
import im.vector.riotredesign.features.home.room.list.RoomListFragment
import im.vector.riotredesign.features.home.room.list.RoomListParams
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_selected_group.*

@Parcelize
data class SelectedGroupParams(
        val groupId: String,
        val groupName: String,
        val groupAvatar: String
) : Parcelable

class SelectedGroupFragment : VectorBaseFragment() {

    private val selectedGroupParams: SelectedGroupParams by args()

    override fun getLayoutResId(): Int {
        return R.layout.fragment_selected_group
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null) {
            updateSelectedFragment(RoomListFragment.DisplayMode.HOME)
            toolbar.setTitle(RoomListFragment.DisplayMode.HOME.titleRes)
        }
        setupBottomNavigationView()
        setupToolbar()
    }

    private fun setupToolbar() {
        val parentActivity = vectorBaseActivity
        if (parentActivity is ToolbarConfigurable) {
            parentActivity.configure(toolbar)
        }
        val toolbarLogoTarget = object : SimpleTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                toolbar.logo = resource
            }
        }
        AvatarRenderer.render(
                requireContext(),
                GlideApp.with(this),
                selectedGroupParams.groupAvatar,
                selectedGroupParams.groupId,
                selectedGroupParams.groupName,
                toolbarLogoTarget
        )
    }

    private fun setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener {
            val displayMode = when {
                it.itemId == R.id.bottom_action_people -> RoomListFragment.DisplayMode.PEOPLE
                it.itemId == R.id.bottom_action_rooms  -> RoomListFragment.DisplayMode.ROOMS
                else                                   -> RoomListFragment.DisplayMode.HOME
            }
            updateSelectedFragment(displayMode)
            toolbar.setTitle(displayMode.titleRes)
            true
        }
    }

    private fun updateSelectedFragment(displayMode: RoomListFragment.DisplayMode) {
        val roomListParams = RoomListParams(displayMode)
        val roomListFragment = RoomListFragment.newInstance(roomListParams)
        replaceChildFragment(roomListFragment, R.id.roomListContainer)
    }

    companion object {

        fun newInstance(args: SelectedGroupParams): SelectedGroupFragment {
            return SelectedGroupFragment().apply {
                setArguments(args)
            }
        }

    }
}