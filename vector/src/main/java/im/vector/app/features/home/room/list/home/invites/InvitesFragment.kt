/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.list.home.invites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentInvitesBinding
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.home.room.list.RoomListListener
import im.vector.app.features.notifications.NotificationDrawerManager
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import javax.inject.Inject

@AndroidEntryPoint
class InvitesFragment : VectorBaseFragment<FragmentInvitesBinding>(), RoomListListener {

    @Inject lateinit var controller: InvitesController
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager

    private val viewModel by fragmentViewModel(InvitesViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentInvitesBinding {
        return FragmentInvitesBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(views.invitesToolbar)
                .allowBack()

        views.invitesRecycler.configureWith(controller)
        controller.listener = this

        viewModel.onEach(InvitesViewState::roomMembershipChanges) {
            controller.roomChangeMembershipStates = it
        }

        viewModel.observeViewEvents {
            when (it) {
                is InvitesViewEvents.Failure -> showFailure(it.throwable)
                is InvitesViewEvents.OpenRoom -> handleOpenRoom(it.roomSummary, it.shouldCloseInviteView)
                InvitesViewEvents.Close -> handleClose()
            }
        }
    }

    private fun handleClose() {
        requireActivity().finish()
    }

    private fun handleOpenRoom(roomSummary: RoomSummary, shouldCloseInviteView: Boolean) {
        navigator.openRoom(
                context = requireActivity(),
                roomId = roomSummary.roomId,
                isInviteAlreadyAccepted = true,
                trigger = ViewRoom.Trigger.RoomList // #6508
        )
        if (shouldCloseInviteView) {
            requireActivity().finish()
        }
    }

    override fun invalidate(): Unit = withState(viewModel) { state ->
        super.invalidate()

        state.pagedList?.observe(viewLifecycleOwner) { list ->
            controller.submitList(list)
        }
    }

    override fun onRejectRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.updateEvents { it.clearMemberShipNotificationForRoom(room.roomId) }
        viewModel.handle(InvitesAction.RejectInvitation(room))
    }

    override fun onAcceptRoomInvitation(room: RoomSummary) {
        notificationDrawerManager.updateEvents { it.clearMemberShipNotificationForRoom(room.roomId) }
        viewModel.handle(InvitesAction.AcceptInvitation(room))
    }

    override fun onJoinSuggestedRoom(room: SpaceChildInfo) = Unit

    override fun onSuggestedRoomClicked(room: SpaceChildInfo) = Unit

    override fun onRoomClicked(room: RoomSummary) = Unit

    override fun onRoomLongClicked(room: RoomSummary): Boolean = false
}
