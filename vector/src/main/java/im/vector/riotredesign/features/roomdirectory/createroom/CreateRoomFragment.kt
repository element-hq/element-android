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

package im.vector.riotredesign.features.roomdirectory.createroom

import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryActivity
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryModule
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryNavigationViewModel
import kotlinx.android.synthetic.main.fragment_create_room.*
import org.koin.android.ext.android.inject
import org.koin.android.scope.ext.android.bindScope
import org.koin.android.scope.ext.android.getOrCreateScope
import timber.log.Timber

class CreateRoomFragment : VectorBaseFragment(), CreateRoomController.Listener {

    private val navigationViewModel: RoomDirectoryNavigationViewModel by activityViewModel()
    private val viewModel: CreateRoomViewModel by fragmentViewModel()
    private val createRoomController: CreateRoomController by inject()

    override fun getLayoutResId() = R.layout.fragment_create_room

    override fun getMenuRes() = R.menu.vector_room_creation

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        bindScope(getOrCreateScope(RoomDirectoryModule.ROOM_DIRECTORY_SCOPE))

        vectorBaseActivity.setSupportActionBar(createRoomToolbar)

        setupRecyclerView()

        createRoomClose.setOnClickListener {
            navigationViewModel.goTo(RoomDirectoryActivity.Navigation.Back)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_create_room -> {
                viewModel.doCreateRoom()
                true
            }
            else                    ->
                super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)

        createRoomForm.layoutManager = layoutManager
        createRoomController.listener = this

        createRoomForm.setController(createRoomController)
    }

    override fun onNameChange(newName: String) {
        viewModel.setName(newName)
    }

    override fun setIsPublic(isPublic: Boolean) {
        viewModel.setIsPublic(isPublic)
    }

    override fun setIsInRoomDirectory(isInRoomDirectory: Boolean) {
        viewModel.setIsInRoomDirectory(isInRoomDirectory)
    }

    override fun retry() {
        Timber.v("Retry")
        viewModel.doCreateRoom()
    }

    override fun invalidate() = withState(viewModel) { state ->
        if (state.asyncCreateRoomRequest is Success) {
            vectorBaseActivity.notImplemented("navigate to freshly created room")
        } else {
            // Populate list with Epoxy
            createRoomController.setData(state)
        }
    }


}
