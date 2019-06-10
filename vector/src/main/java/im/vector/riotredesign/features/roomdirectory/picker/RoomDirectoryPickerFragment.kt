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

package im.vector.riotredesign.features.roomdirectory.picker

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.session.room.model.thirdparty.RoomDirectoryData
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryActivity
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryModule
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryNavigationViewModel
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryViewModel
import kotlinx.android.synthetic.main.fragment_room_directory_picker.*
import org.koin.android.ext.android.inject
import org.koin.android.scope.ext.android.bindScope
import org.koin.android.scope.ext.android.getOrCreateScope
import timber.log.Timber

// TODO Set title to R.string.select_room_directory
// TODO Menu to add custom room directory (not done in RiotWeb so far...)
class RoomDirectoryPickerFragment : VectorBaseFragment(), RoomDirectoryPickerController.Callback {

    private val viewModel: RoomDirectoryViewModel by activityViewModel()
    private val navigationViewModel: RoomDirectoryNavigationViewModel by activityViewModel()
    private val pickerViewModel: RoomDirectoryPickerViewModel by fragmentViewModel()
    private val roomDirectoryPickerController: RoomDirectoryPickerController by inject()

    override fun getLayoutResId() = R.layout.fragment_room_directory_picker

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vectorBaseActivity.setSupportActionBar(toolbar)

        vectorBaseActivity.supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        bindScope(getOrCreateScope(RoomDirectoryModule.ROOM_DIRECTORY_SCOPE))

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)

        roomDirectoryPickerList.layoutManager = layoutManager
        roomDirectoryPickerController.callback = this

        roomDirectoryPickerList.setController(roomDirectoryPickerController)
    }


    override fun onRoomDirectoryClicked(roomDirectoryData: RoomDirectoryData) {
        Timber.v("onRoomDirectoryClicked: $roomDirectoryData")
        viewModel.setRoomDirectoryData(roomDirectoryData)

        navigationViewModel.goTo(RoomDirectoryActivity.Navigation.Back)
    }

    override fun retry() {
        Timber.v("Retry")
        pickerViewModel.load()
    }

    override fun invalidate() = withState(pickerViewModel) { state ->
        // Populate list with Epoxy
        roomDirectoryPickerController.setData(state)
    }
}