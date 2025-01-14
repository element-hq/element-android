/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory.picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentRoomDirectoryPickerBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.roomdirectory.RoomDirectoryAction
import im.vector.app.features.roomdirectory.RoomDirectoryData
import im.vector.app.features.roomdirectory.RoomDirectoryServer
import im.vector.app.features.roomdirectory.RoomDirectorySharedAction
import im.vector.app.features.roomdirectory.RoomDirectorySharedActionViewModel
import im.vector.app.features.roomdirectory.RoomDirectoryViewModel
import im.vector.lib.strings.CommonStrings
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RoomDirectoryPickerFragment :
        VectorBaseFragment<FragmentRoomDirectoryPickerBinding>(),
        OnBackPressed,
        RoomDirectoryPickerController.Callback {

    @Inject lateinit var roomDirectoryPickerController: RoomDirectoryPickerController

    private val viewModel: RoomDirectoryViewModel by activityViewModel()
    private lateinit var sharedActionViewModel: RoomDirectorySharedActionViewModel
    private val pickerViewModel: RoomDirectoryPickerViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomDirectoryPickerBinding {
        return FragmentRoomDirectoryPickerBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SwitchDirectory
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(views.toolbar)
                .setTitle(CommonStrings.select_room_directory)
                .allowBack()

        sharedActionViewModel = activityViewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        setupRecyclerView()

        // Give the current data to our controller. There maybe a better way to do that...
        withState(viewModel) {
            roomDirectoryPickerController.currentRoomDirectoryData = it.roomDirectoryData
        }
    }

    override fun onDestroyView() {
        views.roomDirectoryPickerList.cleanup()
        roomDirectoryPickerController.callback = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        views.roomDirectoryPickerList.configureWith(roomDirectoryPickerController)
        roomDirectoryPickerController.callback = this
    }

    override fun onRoomDirectoryClicked(roomDirectoryData: RoomDirectoryData) {
        Timber.v("onRoomDirectoryClicked: $roomDirectoryData")
        viewModel.handle(RoomDirectoryAction.SetRoomDirectoryData(roomDirectoryData))

        sharedActionViewModel.post(RoomDirectorySharedAction.Back)
    }

    override fun onStartEnterServer() {
        pickerViewModel.handle(RoomDirectoryPickerAction.EnterEditMode)
    }

    override fun onCancelEnterServer() {
        pickerViewModel.handle(RoomDirectoryPickerAction.ExitEditMode)
    }

    override fun onEnterServerChange(server: String) {
        pickerViewModel.handle(RoomDirectoryPickerAction.SetServerUrl(server))
    }

    override fun onSubmitServer() {
        pickerViewModel.handle(RoomDirectoryPickerAction.Submit)
    }

    override fun onRemoveServer(roomDirectoryServer: RoomDirectoryServer) {
        pickerViewModel.handle(RoomDirectoryPickerAction.RemoveServer(roomDirectoryServer))
    }

    override fun retry() {
        Timber.v("Retry")
        pickerViewModel.handle(RoomDirectoryPickerAction.Retry)
    }

    override fun invalidate() = withState(pickerViewModel) { state ->
        // Populate list with Epoxy
        roomDirectoryPickerController.setData(state)
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        // Leave the add server mode if started
        return withState(pickerViewModel) {
            if (it.inEditMode) {
                pickerViewModel.handle(RoomDirectoryPickerAction.ExitEditMode)
                true
            } else {
                false
            }
        }
    }
}
