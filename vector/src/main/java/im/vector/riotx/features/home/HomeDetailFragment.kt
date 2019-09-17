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

package im.vector.riotx.features.home

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupState
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.platform.ToolbarConfigurable
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.ui.views.KeysBackupBanner
import im.vector.riotx.features.home.room.list.RoomListFragment
import im.vector.riotx.features.home.room.list.RoomListParams
import im.vector.riotx.features.home.room.list.UnreadCounterBadgeView
import im.vector.riotx.features.workers.signout.SignOutViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_home_detail.*
import javax.inject.Inject


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

class HomeDetailFragment : VectorBaseFragment(), KeysBackupBanner.Delegate {

    private val params: HomeDetailParams by args()
    private val unreadCounterBadgeViews = arrayListOf<UnreadCounterBadgeView>()
    private lateinit var currentDisplayMode: RoomListFragment.DisplayMode

    private val viewModel: HomeDetailViewModel by fragmentViewModel()
    private lateinit var navigationViewModel: HomeNavigationViewModel

    @Inject lateinit var session: Session
    @Inject lateinit var homeDetailViewModelFactory: HomeDetailViewModel.Factory
    @Inject lateinit var avatarRenderer: AvatarRenderer

    override fun getLayoutResId(): Int {
        return R.layout.fragment_home_detail
    }

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        currentDisplayMode = savedInstanceState?.getSerializable(CURRENT_DISPLAY_MODE) as? RoomListFragment.DisplayMode
                             ?: RoomListFragment.DisplayMode.HOME

        navigationViewModel = ViewModelProviders.of(requireActivity()).get(HomeNavigationViewModel::class.java)

        switchDisplayMode(currentDisplayMode)
        setupBottomNavigationView()
        setupToolbar()
        setupKeysBackupBanner()
    }

    private fun setupKeysBackupBanner() {
        // Keys backup banner
        // Use the SignOutViewModel, it observe the keys backup state and this is what we need here
        val model = ViewModelProviders.of(this, viewModelFactory).get(SignOutViewModel::class.java)

        model.init(session)

        model.keysBackupState.observe(this, Observer { keysBackupState ->
            when (keysBackupState) {
                null                               ->
                    homeKeysBackupBanner.render(KeysBackupBanner.State.Hidden, false)
                KeysBackupState.Disabled           ->
                    homeKeysBackupBanner.render(KeysBackupBanner.State.Setup(model.getNumberOfKeysToBackup()), false)
                KeysBackupState.NotTrusted,
                KeysBackupState.WrongBackUpVersion ->
                    // In this case, getCurrentBackupVersion() should not return ""
                    homeKeysBackupBanner.render(KeysBackupBanner.State.Recover(model.getCurrentBackupVersion()), false)
                KeysBackupState.WillBackUp,
                KeysBackupState.BackingUp          ->
                    homeKeysBackupBanner.render(KeysBackupBanner.State.BackingUp, false)
                KeysBackupState.ReadyToBackUp      ->
                    if (model.canRestoreKeys()) {
                        homeKeysBackupBanner.render(KeysBackupBanner.State.Update(model.getCurrentBackupVersion()), false)
                    } else {
                        homeKeysBackupBanner.render(KeysBackupBanner.State.Hidden, false)
                    }
                else                               ->
                    homeKeysBackupBanner.render(KeysBackupBanner.State.Hidden, false)
            }
        })

        homeKeysBackupBanner.delegate = this
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
        avatarRenderer.render(
                params.groupAvatar,
                params.groupId,
                params.groupName,
                groupToolbarAvatarImageView
        )
        groupToolbarAvatarImageView.setOnClickListener {
            navigationViewModel.goTo(HomeActivity.Navigation.OpenDrawer)
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

    /* ==========================================================================================
     * KeysBackupBanner Listener
     * ========================================================================================== */

    override fun setupKeysBackup() {
        navigator.openKeysBackupSetup(requireActivity(), false)
    }

    override fun recoverKeysBackup() {
        navigator.openKeysBackupManager(requireActivity())
    }

    override fun invalidate() = withState(viewModel) {
        unreadCounterBadgeViews[INDEX_CATCHUP].render(UnreadCounterBadgeView.State(it.notificationCountCatchup, it.notificationHighlightCatchup))
        unreadCounterBadgeViews[INDEX_PEOPLE].render(UnreadCounterBadgeView.State(it.notificationCountPeople, it.notificationHighlightPeople))
        unreadCounterBadgeViews[INDEX_ROOMS].render(UnreadCounterBadgeView.State(it.notificationCountRooms, it.notificationHighlightRooms))
        syncProgressBarWrap.visibility = when (it.syncState) {
            is SyncState.RUNNING -> if (it.syncState.afterPause) View.VISIBLE else View.GONE
            else                 -> View.GONE
        }
        // TODO Create a View
        noNetworkBanner.isVisible = it.syncState is SyncState.NO_NETWORK
    }

    companion object {

        fun newInstance(args: HomeDetailParams): HomeDetailFragment {
            return HomeDetailFragment().apply {
                setArguments(args)
            }
        }

    }
}