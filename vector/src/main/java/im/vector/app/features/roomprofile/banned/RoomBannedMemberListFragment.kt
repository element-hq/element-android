/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.roomprofile.banned

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.util.toMatrixItem
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.toast
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.RoomProfileArgs
import kotlinx.android.synthetic.main.fragment_room_setting_generic.*
import javax.inject.Inject

class RoomBannedMemberListFragment @Inject constructor(
        val viewModelFactory: RoomBannedListMemberViewModel.Factory,
        private val roomMemberListController: RoomBannedMemberListController,
        private val avatarRenderer: AvatarRenderer
) : VectorBaseFragment(), RoomBannedMemberListController.Callback {

    private val viewModel: RoomBannedListMemberViewModel by fragmentViewModel()
    private val roomProfileArgs: RoomProfileArgs by args()

    override fun getLayoutResId() = R.layout.fragment_room_setting_generic

    override fun onUnbanClicked(roomMember: RoomMemberSummary) {
        viewModel.handle(RoomBannedListMemberAction.QueryInfo(roomMember))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomMemberListController.callback = this
        setupToolbar(roomSettingsToolbar)
        recyclerView.configureWith(roomMemberListController, hasFixedSize = true)

        viewModel.observeViewEvents {
            when (it) {
                is RoomBannedViewEvents.ShowBannedInfo -> {
                    val canBan = withState(viewModel) { state -> state.canUserBan }
                    AlertDialog.Builder(requireActivity())
                            .setTitle(getString(R.string.member_banned_by, it.bannedByUserId))
                            .setMessage(getString(R.string.reason_colon, it.banReason))
                            .setPositiveButton(R.string.ok, null)
                            .apply {
                                if (canBan) {
                                    setNegativeButton(R.string.room_participants_action_unban) { _, _ ->
                                        viewModel.handle(RoomBannedListMemberAction.UnBanUser(it.roomMemberSummary))
                                    }
                                }
                            }
                            .show()
                }
                is RoomBannedViewEvents.ToastError -> {
                    requireActivity().toast(it.info)
                }
            }
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

    private fun renderRoomSummary(state: RoomBannedMemberListViewState) {
        state.roomSummary()?.let {
            roomSettingsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), roomSettingsToolbarAvatarImageView)
        }
    }
}
