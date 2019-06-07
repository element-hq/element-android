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
import android.os.Parcelable
import android.view.LayoutInflater
import androidx.core.view.forEachIndexed
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.ToolbarConfigurable
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.features.home.room.list.RoomListFragment
import im.vector.riotredesign.features.home.room.list.RoomListParams
import im.vector.riotredesign.features.home.room.list.UnreadCounterBadgeView
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_home_detail.*


@Parcelize
data class HomeDetailParams(
        val groupId: String,
        val groupName: String,
        val groupAvatar: String
) : Parcelable


private const val CURRENT_DISPLAY_MODE = "CURRENT_DISPLAY_MODE"

private const val INDEX_CATCHUP = 0
private const val INDEX_PEOPLE = 1
private const val INDEX_ROOMS = 2

class HomeDetailFragment : VectorBaseFragment() {

    private val params: HomeDetailParams by args()
    private val unreadCounterBadgeViews = arrayListOf<UnreadCounterBadgeView>()
    private lateinit var currentDisplayMode: RoomListFragment.DisplayMode

    private val viewModel: HomeDetailViewModel by fragmentViewModel()

    override fun getLayoutResId(): Int {
        return R.layout.fragment_home_detail
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        currentDisplayMode = savedInstanceState?.getSerializable(CURRENT_DISPLAY_MODE) as? RoomListFragment.DisplayMode
                ?: RoomListFragment.DisplayMode.HOME
        switchDisplayMode(currentDisplayMode)
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
                params.groupAvatar,
                params.groupId,
                params.groupName,
                groupToolbarAvatarImageView
        )
        groupToolbarAvatarImageView.setOnClickListener {
            (vectorBaseActivity as? HomeActivity)?.openDrawer()
        }
    }

    private fun setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener {
            val displayMode = when (it.itemId) {
                R.id.bottom_action_home   -> RoomListFragment.DisplayMode.HOME
                R.id.bottom_action_people -> RoomListFragment.DisplayMode.PEOPLE
                R.id.bottom_action_rooms  -> RoomListFragment.DisplayMode.ROOMS
                else                      -> RoomListFragment.DisplayMode.HOME
            }
            if (currentDisplayMode != displayMode) {
                currentDisplayMode = displayMode
                switchDisplayMode(displayMode)
            }
            true
        }

        val menuView = bottomNavigationView.getChildAt(0) as BottomNavigationMenuView
        menuView.forEachIndexed { index, view ->
            val itemView = view as BottomNavigationItemView
            val badgeLayout = LayoutInflater.from(requireContext()).inflate(R.layout.vector_home_badge_unread_layout, menuView, false)
            val unreadCounterBadgeView: UnreadCounterBadgeView = badgeLayout.findViewById(R.id.actionUnreadCounterBadgeView)
            itemView.addView(badgeLayout)
            unreadCounterBadgeViews.add(index, unreadCounterBadgeView)
        }
    }

    private fun switchDisplayMode(displayMode: RoomListFragment.DisplayMode) {
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


    override fun invalidate() = withState(viewModel) {
        unreadCounterBadgeViews[INDEX_CATCHUP].render(UnreadCounterBadgeView.State(it.notificationCountCatchup, it.notificationHighlightCatchup))
        unreadCounterBadgeViews[INDEX_PEOPLE].render(UnreadCounterBadgeView.State(it.notificationCountPeople, it.notificationHighlightPeople))
        unreadCounterBadgeViews[INDEX_ROOMS].render(UnreadCounterBadgeView.State(it.notificationCountRooms, it.notificationHighlightRooms))
    }

    companion object {

        fun newInstance(args: HomeDetailParams): HomeDetailFragment {
            return HomeDetailFragment().apply {
                setArguments(args)
            }
        }

    }
}