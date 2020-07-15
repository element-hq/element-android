/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.roomdirectory.picker

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.session.room.model.thirdparty.RoomDirectoryData
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.roomdirectory.RoomDirectoryAction
import im.vector.riotx.features.roomdirectory.RoomDirectorySharedAction
import im.vector.riotx.features.roomdirectory.RoomDirectorySharedActionViewModel
import im.vector.riotx.features.roomdirectory.RoomDirectoryViewModel
import kotlinx.android.synthetic.main.fragment_room_directory_picker.*
import timber.log.Timber
import javax.inject.Inject

// TODO Menu to add custom room directory (not done in RiotWeb so far...)
class RoomDirectoryPickerFragment @Inject constructor(val roomDirectoryPickerViewModelFactory: RoomDirectoryPickerViewModel.Factory,
                                                      private val roomDirectoryPickerController: RoomDirectoryPickerController
) : VectorBaseFragment(), RoomDirectoryPickerController.Callback {

    private val viewModel: RoomDirectoryViewModel by activityViewModel()
    private lateinit var sharedActionViewModel: RoomDirectorySharedActionViewModel
    private val pickerViewModel: RoomDirectoryPickerViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_room_directory_picker

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vectorBaseActivity.setSupportActionBar(toolbar)

        vectorBaseActivity.supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        sharedActionViewModel = activityViewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        setupRecyclerView()
    }

    override fun onDestroyView() {
        roomDirectoryPickerList.cleanup()
        roomDirectoryPickerController.callback = null
        super.onDestroyView()
    }

    override fun getMenuRes() = R.menu.menu_directory_server_picker

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_add_custom_hs) {
            // TODO
            vectorBaseActivity.notImplemented("Entering custom homeserver")
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        roomDirectoryPickerList.configureWith(roomDirectoryPickerController)
        roomDirectoryPickerController.callback = this
    }

    override fun onRoomDirectoryClicked(roomDirectoryData: RoomDirectoryData) {
        Timber.v("onRoomDirectoryClicked: $roomDirectoryData")
        viewModel.handle(RoomDirectoryAction.SetRoomDirectoryData(roomDirectoryData))

        sharedActionViewModel.post(RoomDirectorySharedAction.Back)
    }

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.select_room_directory)
    }

    override fun retry() {
        Timber.v("Retry")
        pickerViewModel.handle(RoomDirectoryPickerAction.Retry)
    }

    override fun invalidate() = withState(pickerViewModel) { state ->
        // Populate list with Epoxy
        roomDirectoryPickerController.setData(state)
    }
}
