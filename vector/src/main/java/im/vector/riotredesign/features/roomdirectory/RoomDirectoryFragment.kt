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

package im.vector.riotredesign.features.roomdirectory

import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyVisibilityTracker
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoom
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.SimpleTextWatcher
import im.vector.riotredesign.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_room_directory.*
import org.koin.android.ext.android.get
import timber.log.Timber


/**
 * What can be improved:
 * - When filtering more (when entering new chars), we could filter on result we already have, during the new server request, to avoid empty screen effect
 *
 * FIXME Rotate screen launch again the request
 *
 * For Nad:
 * Display number of rooms?
 * Picto size are not correct
 * Where I put the room directory picker?
 *
 */
class RoomDirectoryFragment : VectorBaseFragment(), RoomDirectoryController.Callback {

    private val viewModel: RoomDirectoryViewModel by fragmentViewModel()

    private val roomDirectoryController = RoomDirectoryController(get())

    override fun getLayoutResId() = R.layout.fragment_room_directory

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vectorBaseActivity.setSupportActionBar(toolbar)

        vectorBaseActivity.supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        roomDirectoryFilter.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                // TODO Debounce
                viewModel.filterWith(roomDirectoryFilter.text.toString())
            }
        })

        createNewRoom.setOnClickListener {
            vectorBaseActivity.notImplemented()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val epoxyVisibilityTracker = EpoxyVisibilityTracker()
        epoxyVisibilityTracker.attach(roomDirectoryList)

        val layoutManager = LinearLayoutManager(context)

        roomDirectoryList.layoutManager = layoutManager
        roomDirectoryController.callback = this

        roomDirectoryList.setController(roomDirectoryController)
    }

    override fun onPublicRoomClicked(publicRoom: PublicRoom) {
        Timber.v("PublicRoomClicked: $publicRoom")
        vectorBaseActivity.notImplemented()
    }

    override fun onPublicRoomJoin(publicRoom: PublicRoom) {
        Timber.v("PublicRoomJoinClicked: $publicRoom")
        viewModel.joinRoom(publicRoom)
    }

    override fun loadMore() {
        viewModel.loadMore()
    }

    override fun invalidate() = withState(viewModel) { state ->
        // Populate list with Epoxy
        roomDirectoryController.setData(state)
    }
}