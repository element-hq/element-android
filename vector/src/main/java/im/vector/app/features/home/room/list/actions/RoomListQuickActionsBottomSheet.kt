/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.actions

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetGenericListBinding
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.roomprofile.notifications.RoomNotificationSettingsAction
import im.vector.app.features.roomprofile.notifications.RoomNotificationSettingsViewEvents
import im.vector.app.features.roomprofile.notifications.RoomNotificationSettingsViewModel
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import javax.inject.Inject

@Parcelize
data class RoomListActionsArgs(
        val roomId: String
) : Parcelable

/**
 * Bottom sheet fragment that shows room information with list of contextual actions.
 */
@AndroidEntryPoint
class RoomListQuickActionsBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetGenericListBinding>(),
        RoomListQuickActionsEpoxyController.Listener {

    private lateinit var sharedActionViewModel: RoomListQuickActionsSharedActionViewModel
    @Inject lateinit var sharedViewPool: RecyclerView.RecycledViewPool
    @Inject lateinit var roomListActionsEpoxyController: RoomListQuickActionsEpoxyController
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var errorFormatter: ErrorFormatter

    private val roomListActionsArgs: RoomListActionsArgs by args()
    private val viewModel: RoomNotificationSettingsViewModel by fragmentViewModel(RoomNotificationSettingsViewModel::class)

    override val showExpanded = true

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetGenericListBinding {
        return BottomSheetGenericListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(RoomListQuickActionsSharedActionViewModel::class.java)
        views.bottomSheetRecyclerView.configureWith(
                epoxyController = roomListActionsEpoxyController,
                viewPool = sharedViewPool,
                hasFixedSize = false,
                disableItemAnimation = true
        )
        roomListActionsEpoxyController.listener = this

        viewModel.observeViewEvents {
            when (it) {
                is RoomNotificationSettingsViewEvents.Failure -> displayErrorDialog(it.throwable)
            }
        }
    }

    override fun onDestroyView() {
        views.bottomSheetRecyclerView.cleanup()
        roomListActionsEpoxyController.listener = null
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) {
        val roomListViewState = RoomListQuickActionViewState(
                roomListActionsArgs,
                it
        )
        roomListActionsEpoxyController.setData(roomListViewState)
        super.invalidate()
    }

    override fun didSelectMenuAction(quickAction: RoomListQuickActionsSharedAction) {
        sharedActionViewModel.post(quickAction)
        // Do not dismiss for all the actions
        when (quickAction) {
            is RoomListQuickActionsSharedAction.LowPriority -> Unit
            is RoomListQuickActionsSharedAction.Favorite -> Unit
            else -> dismiss()
        }
    }

    override fun didSelectRoomNotificationState(roomNotificationState: RoomNotificationState) {
        viewModel.handle(RoomNotificationSettingsAction.SelectNotificationState(roomNotificationState))
    }

    companion object {
        fun newInstance(roomId: String): RoomListQuickActionsBottomSheet {
            return RoomListQuickActionsBottomSheet().apply {
                setArguments(RoomListActionsArgs(roomId))
            }
        }
    }

    private fun displayErrorDialog(throwable: Throwable) {
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(CommonStrings.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(throwable))
                .setPositiveButton(CommonStrings.ok, null)
                .show()
    }
}
