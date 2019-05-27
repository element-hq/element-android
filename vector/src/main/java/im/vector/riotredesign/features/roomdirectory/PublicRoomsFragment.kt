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
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyVisibilityTracker
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.widget.RxTextView
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoom
import im.vector.riotredesign.R
import im.vector.riotredesign.core.error.ErrorFormatter
import im.vector.riotredesign.core.extensions.addFragmentToBackstack
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.features.roomdirectory.picker.RoomDirectoryPickerFragment
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_public_rooms.*
import org.koin.android.ext.android.inject
import org.koin.android.scope.ext.android.bindScope
import org.koin.android.scope.ext.android.getOrCreateScope
import timber.log.Timber
import java.util.concurrent.TimeUnit


/**
 * What can be improved:
 * - When filtering more (when entering new chars), we could filter on result we already have, during the new server request, to avoid empty screen effect
 *
 * TODO For Nad:
 * Display number of rooms?
 * Picto size are not correct
 * Where I put the room directory picker?
 * World Readable badge
 * Guest can join badge
 *
 */
class PublicRoomsFragment : VectorBaseFragment(), PublicRoomsController.Callback {

    private val viewModel: RoomDirectoryViewModel by activityViewModel()
    private val publicRoomsController: PublicRoomsController by inject()
    private val errorFormatter: ErrorFormatter by inject()

    override fun getLayoutResId() = R.layout.fragment_public_rooms

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vectorBaseActivity.setSupportActionBar(toolbar)

        vectorBaseActivity.supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        RxTextView.textChanges(publicRoomsFilter)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeBy {
                    viewModel.filterWith(it.toString())
                }
                .disposeOnDestroy()

        publicRoomsCreateNewRoom.setOnClickListener {
            // TODO homeActivityViewModel.createRoom()

            vectorBaseActivity.notImplemented()
        }

        publicRoomsChangeDirectory.setOnClickListener {
            vectorBaseActivity.addFragmentToBackstack(RoomDirectoryPickerFragment(), R.id.simpleFragmentContainer)
        }

        viewModel.joinRoomErrorLiveData.observe(this, Observer {
            it.getContentIfNotHandled()?.let { throwable ->
                Snackbar.make(publicRoomsCoordinator, errorFormatter.toHumanReadable(throwable), Snackbar.LENGTH_SHORT)
                        .show()
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        bindScope(getOrCreateScope(RoomDirectoryModule.ROOM_DIRECTORY_SCOPE))

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val epoxyVisibilityTracker = EpoxyVisibilityTracker()
        epoxyVisibilityTracker.attach(publicRoomsList)

        val layoutManager = LinearLayoutManager(context)

        publicRoomsList.layoutManager = layoutManager
        publicRoomsController.callback = this

        publicRoomsList.setController(publicRoomsController)
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
        publicRoomsController.setData(state)

        // Directory name
        publicRoomsDirectoryName.text = state.roomDirectoryDisplayName
    }
}