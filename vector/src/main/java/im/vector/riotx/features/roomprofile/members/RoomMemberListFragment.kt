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

package im.vector.riotx.features.roomprofile.members

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.RoomMemberSummary
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.roomprofile.RoomProfileArgs
import kotlinx.android.synthetic.main.fragment_room_setting_generic.*
import javax.inject.Inject

class RoomMemberListFragment @Inject constructor(
        val viewModelFactory: RoomMemberListViewModel.Factory,
        private val roomMemberListController: RoomMemberListController,
        private val avatarRenderer: AvatarRenderer
) : VectorBaseFragment(), RoomMemberListController.Callback {

    private val viewModel: RoomMemberListViewModel by fragmentViewModel()
    private val roomProfileArgs: RoomProfileArgs by args()

    override fun getLayoutResId() = R.layout.fragment_room_setting_generic

    override fun getMenuRes() = R.menu.menu_room_member_list

    override fun onPrepareOptionsMenu(menu: Menu) {
        val canInvite = withState(viewModel) {
            it.actionsPermissions.canInvite
        }
        menu.findItem(R.id.menu_room_member_list_add_member).isVisible = canInvite
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_room_member_list_add_member -> {
                navigator.openInviteUsersToRoom(requireContext(), roomProfileArgs.roomId)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomMemberListController.callback = this
        setupToolbar(roomSettingsToolbar)
        recyclerView.configureWith(roomMemberListController, hasFixedSize = true)
        viewModel.selectSubscribe(this, RoomMemberListViewState::actionsPermissions) {
            invalidateOptionsMenu()
        }
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        roomMemberListController.setData(viewState)
        renderRoomSummary(viewState)
    }

    override fun onRoomMemberClicked(roomMember: RoomMemberSummary) {
        navigator.openRoomMemberProfile(roomMember.userId, roomId = roomProfileArgs.roomId, context = requireActivity())
    }

    override fun onThreePidInvites(event: Event) {
        // TODO Display a bottom sheet to revoke invite if power level is high enough
    }

    private fun renderRoomSummary(state: RoomMemberListViewState) {
        state.roomSummary()?.let {
            roomSettingsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), roomSettingsToolbarAvatarImageView)
        }
    }
}
