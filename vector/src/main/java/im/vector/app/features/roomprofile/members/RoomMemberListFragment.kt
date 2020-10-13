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

package im.vector.app.features.roomprofile.members

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.RoomProfileArgs
import kotlinx.android.synthetic.main.fragment_room_member_list.*
import kotlinx.android.synthetic.main.fragment_room_setting_generic.*
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomThirdPartyInviteContent
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class RoomMemberListFragment @Inject constructor(
        val viewModelFactory: RoomMemberListViewModel.Factory,
        private val roomMemberListController: RoomMemberListController,
        private val avatarRenderer: AvatarRenderer
) : VectorBaseFragment(), RoomMemberListController.Callback {

    private val viewModel: RoomMemberListViewModel by fragmentViewModel()
    private val roomProfileArgs: RoomProfileArgs by args()

    override fun getLayoutResId() = R.layout.fragment_room_member_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomMemberListController.callback = this
        setupToolbar(roomSettingsToolbar)
        setupSearchView()
        setupInviteUsersButton()
        recyclerView.configureWith(roomMemberListController, hasFixedSize = true)
    }

    private fun setupInviteUsersButton() {
        inviteUsersButton.debouncedClicks {
            navigator.openInviteUsersToRoom(requireContext(), roomProfileArgs.roomId)
        }
        // Hide FAB when list is scrolling
        recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        when (newState) {
                            RecyclerView.SCROLL_STATE_IDLE     -> {
                                if (withState(viewModel) { it.actionsPermissions.canInvite }) {
                                    inviteUsersButton.show()
                                }
                            }
                            RecyclerView.SCROLL_STATE_DRAGGING,
                            RecyclerView.SCROLL_STATE_SETTLING -> {
                                inviteUsersButton.hide()
                            }
                        }
                    }
                }
        )
    }

    private fun setupSearchView() {
        searchView.queryHint = getString(R.string.search_members_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.handle(RoomMemberListAction.FilterMemberList(newText))
                return true
            }
        })
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        roomMemberListController.setData(viewState)
        renderRoomSummary(viewState)
        inviteUsersButton.isVisible = viewState.actionsPermissions.canInvite
        // Display filter only if there are more than 2 members in this room
        searchViewAppBarLayout.isVisible = viewState.roomSummary()?.otherMemberIds.orEmpty().size > 1
    }

    override fun onRoomMemberClicked(roomMember: RoomMemberSummary) {
        navigator.openRoomMemberProfile(roomMember.userId, roomId = roomProfileArgs.roomId, context = requireActivity())
    }

    override fun onThreePidInviteClicked(event: Event) {
        // Display a dialog to revoke invite if power level is high enough
        val content = event.content.toModel<RoomThirdPartyInviteContent>() ?: return
        val stateKey = event.stateKey ?: return
        if (withState(viewModel) { it.actionsPermissions.canRevokeThreePidInvite }) {
            AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.three_pid_revoke_invite_dialog_title)
                    .setMessage(getString(R.string.three_pid_revoke_invite_dialog_content, content.displayName))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.revoke) { _, _ ->
                        viewModel.handle(RoomMemberListAction.RevokeThreePidInvite(stateKey))
                    }
                    .show()
        }
    }

    private fun renderRoomSummary(state: RoomMemberListViewState) {
        state.roomSummary()?.let {
            roomSettingsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), roomSettingsToolbarAvatarImageView)
        }
    }
}
