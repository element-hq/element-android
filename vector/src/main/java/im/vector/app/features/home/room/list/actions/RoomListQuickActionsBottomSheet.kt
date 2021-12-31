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
import im.vector.app.R
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetGenericListBinding
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.roomprofile.notifications.RoomNotificationSettingsAction
import im.vector.app.features.roomprofile.notifications.RoomNotificationSettingsViewEvents
import im.vector.app.features.roomprofile.notifications.RoomNotificationSettingsViewModel
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import javax.inject.Inject

@Parcelize
data class RoomListActionsArgs(
        val roomId: String
) : Parcelable

/**
 * Bottom sheet fragment that shows room information with list of contextual actions
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
            is RoomListQuickActionsSharedAction.Favorite    -> Unit
            else                                            -> dismiss()
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
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(throwable))
                .setPositiveButton(R.string.ok, null)
                .show()
    }
}
