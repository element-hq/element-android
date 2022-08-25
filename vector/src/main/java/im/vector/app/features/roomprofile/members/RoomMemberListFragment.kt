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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentRoomMemberListBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.RoomProfileArgs
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomThirdPartyInviteContent
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

@AndroidEntryPoint
class RoomMemberListFragment :
        VectorBaseFragment<FragmentRoomMemberListBinding>(),
        RoomMemberListController.Callback {

    @Inject lateinit var roomMemberListController: RoomMemberListController
    @Inject lateinit var avatarRenderer: AvatarRenderer

    private val viewModel: RoomMemberListViewModel by fragmentViewModel()
    private val roomProfileArgs: RoomProfileArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomMemberListBinding {
        return FragmentRoomMemberListBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.RoomMembers
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomMemberListController.callback = this
        setupToolbar(views.roomSettingGeneric.roomSettingsToolbar)
                .allowBack()
        setupSearchView()
        setupInviteUsersButton()
        views.roomSettingGeneric.roomSettingsRecyclerView.configureWith(roomMemberListController, hasFixedSize = true)
    }

    private fun setupInviteUsersButton() {
        views.inviteUsersButton.debouncedClicks {
            navigator.openInviteUsersToRoom(requireContext(), roomProfileArgs.roomId)
        }
        // Hide FAB when list is scrolling
        views.roomSettingGeneric.roomSettingsRecyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        when (newState) {
                            RecyclerView.SCROLL_STATE_IDLE -> {
                                if (withState(viewModel) { it.actionsPermissions.canInvite }) {
                                    views.inviteUsersButton.show()
                                }
                            }
                            RecyclerView.SCROLL_STATE_DRAGGING,
                            RecyclerView.SCROLL_STATE_SETTLING -> {
                                views.inviteUsersButton.hide()
                            }
                        }
                    }
                }
        )
    }

    private fun setupSearchView() {
        views.roomSettingGeneric.searchView.queryHint = getString(R.string.search_members_hint)
        views.roomSettingGeneric.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
        views.roomSettingGeneric.roomSettingsRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        views.roomSettingGeneric.progressBar.isGone = viewState.areAllMembersLoaded
        roomMemberListController.setData(viewState)
        renderRoomSummary(viewState)
        views.inviteUsersButton.isVisible = viewState.actionsPermissions.canInvite
        // Display filter only if there are more than 2 members in this room
        views.roomSettingGeneric.searchViewAppBarLayout.isVisible = viewState.roomSummary()?.otherMemberIds.orEmpty().size > 1
    }

    override fun onRoomMemberClicked(roomMember: RoomMemberSummary) {
        navigator.openRoomMemberProfile(roomMember.userId, roomId = roomProfileArgs.roomId, context = requireActivity())
    }

    override fun onThreePidInviteClicked(event: Event) {
        // Display a dialog to revoke invite if power level is high enough
        val content = event.content.toModel<RoomThirdPartyInviteContent>() ?: return
        val stateKey = event.stateKey ?: return
        if (withState(viewModel) { it.actionsPermissions.canRevokeThreePidInvite }) {
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.three_pid_revoke_invite_dialog_title)
                    .setMessage(getString(R.string.three_pid_revoke_invite_dialog_content, content.displayName))
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_revoke) { _, _ ->
                        viewModel.handle(RoomMemberListAction.RevokeThreePidInvite(stateKey))
                    }
                    .show()
        }
    }

    private fun renderRoomSummary(state: RoomMemberListViewState) {
        state.roomSummary()?.let {
            views.roomSettingGeneric.roomSettingsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), views.roomSettingGeneric.roomSettingsToolbarAvatarImageView)
            views.roomSettingGeneric.roomSettingsDecorationToolbarAvatarImageView.render(it.roomEncryptionTrustLevel)
        }
    }
}
