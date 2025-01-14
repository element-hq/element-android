/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.invites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.fragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentInvitesBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.home.room.list.RoomListListener
import im.vector.app.features.notifications.NotificationDrawerManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.Invites
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(views.invitesToolbar)
                .allowBack()

        views.invitesStateView.contentView = views.invitesRecycler

        views.invitesRecycler.configureWith(controller)
        controller.listener = this

        viewModel.onEach(InvitesViewState::roomMembershipChanges) {
            controller.roomChangeMembershipStates = it
        }

        viewModel.observeViewEvents {
            when (it) {
                is InvitesViewEvents.Failure -> showFailure(it.throwable)
                is InvitesViewEvents.OpenRoom -> handleOpenRoom(it.roomSummary, it.shouldCloseInviteView, it.isInviteAlreadySelected)
            }
        }

        viewModel.invites.onEach {
            when (it) {
                is InvitesContentState.Content -> {
                    views.invitesStateView.state = StateView.State.Content
                    controller.submitList(it.content)
                }
                is InvitesContentState.Empty -> {
                    views.invitesStateView.state = StateView.State.Empty(
                            title = it.title,
                            image = it.image,
                            message = it.message
                    )
                }
                is InvitesContentState.Error -> {
                    when (views.invitesStateView.state) {
                        StateView.State.Content -> showErrorInSnackbar(it.throwable)
                        else -> views.invitesStateView.state = StateView.State.Error(it.throwable.message)
                    }
                }
                InvitesContentState.Loading -> views.invitesStateView.state = StateView.State.Loading
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun handleOpenRoom(
            roomSummary: RoomSummary,
            shouldCloseInviteView: Boolean,
            isInviteAlreadyAccepted: Boolean,
    ) {
        navigator.openRoom(
                context = requireActivity(),
                roomId = roomSummary.roomId,
                isInviteAlreadyAccepted = isInviteAlreadyAccepted,
                trigger = ViewRoom.Trigger.RoomList // #6508
        )
        if (shouldCloseInviteView) {
            requireActivity().finish()
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

    override fun onRoomClicked(room: RoomSummary) {
        viewModel.handle(InvitesAction.SelectRoom(room))
    }

    override fun onRoomLongClicked(room: RoomSummary): Boolean = false
}
