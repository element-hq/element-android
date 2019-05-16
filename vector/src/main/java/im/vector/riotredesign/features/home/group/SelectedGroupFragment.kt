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

import android.os.Bundle
import android.os.Parcelable
import com.airbnb.mvrx.args
import im.vector.riotredesign.R
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

private const val CURRENT_DISPLAY_MODE = "CURRENT_DISPLAY_MODE"

class SelectedGroupFragment : VectorBaseFragment() {

    private val selectedGroupParams: SelectedGroupParams by args()
    private lateinit var currentDisplayMode: RoomListFragment.DisplayMode

    override fun getLayoutResId(): Int {
        return R.layout.fragment_selected_group
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null) {
            currentDisplayMode = RoomListFragment.DisplayMode.HOME
        } else {
            currentDisplayMode = savedInstanceState.getSerializable(CURRENT_DISPLAY_MODE) as? RoomListFragment.DisplayMode
                                 ?: RoomListFragment.DisplayMode.HOME
        }
        renderState(currentDisplayMode)
        setupBottomNavigationView()
        setupToolbar()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(CURRENT_DISPLAY_MODE, currentDisplayMode)
        super.onSaveInstanceState(outState)
    }

    private fun setupToolbar() {
        val parentActivity = vectorBaseActivity
        if (parentActivity is ToolbarConfigurable) {
            parentActivity.configure(groupToolbar)
        }
        groupToolbar.title = ""
        AvatarRenderer.render(
                selectedGroupParams.groupAvatar,
                selectedGroupParams.groupId,
                selectedGroupParams.groupName,
                groupToolbarAvatarImageView
        )
        groupToolbarAvatarImageView.setOnClickListener {

        }
    }

    private fun setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener {
            val displayMode = when {
                it.itemId == R.id.bottom_action_people -> RoomListFragment.DisplayMode.PEOPLE
                it.itemId == R.id.bottom_action_rooms  -> RoomListFragment.DisplayMode.ROOMS
                else                                   -> RoomListFragment.DisplayMode.HOME
            }
            if (currentDisplayMode != displayMode) {
                currentDisplayMode = displayMode
                renderState(displayMode)
            }
            true
        }
    }

    private fun renderState(displayMode: RoomListFragment.DisplayMode) {
        groupToolbarTitleView.setText(displayMode.titleRes)
        updateSelectedFragment(displayMode)
    }

    private fun updateSelectedFragment(displayMode: RoomListFragment.DisplayMode) {
        val fragmentTag = "FRAGMENT_TAG_${displayMode.name}"
        var fragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (fragment == null) {
            fragment = RoomListFragment.newInstance(RoomListParams(displayMode))
        }
        childFragmentManager.beginTransaction()
                .replace(R.id.roomListContainer, fragment, fragmentTag)
                .addToBackStack(fragmentTag)
                .commit()
    }

    companion object {

        fun newInstance(args: SelectedGroupParams): SelectedGroupFragment {
            return SelectedGroupFragment().apply {
                setArguments(args)
            }
        }

    }
}