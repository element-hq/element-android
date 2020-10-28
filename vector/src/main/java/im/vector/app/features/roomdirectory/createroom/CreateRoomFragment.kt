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

package im.vector.app.features.roomdirectory.createroom

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.dialogs.GalleryOrCameraDialogHelper
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.features.roomdirectory.RoomDirectorySharedAction
import im.vector.app.features.roomdirectory.RoomDirectorySharedActionViewModel
import kotlinx.android.synthetic.main.fragment_create_room.*
import timber.log.Timber
import javax.inject.Inject

class CreateRoomFragment @Inject constructor(
        private val createRoomController: CreateRoomController,
        colorProvider: ColorProvider
) : VectorBaseFragment(),
        CreateRoomController.Listener,
        GalleryOrCameraDialogHelper.Listener,
        OnBackPressed {

    private lateinit var sharedActionViewModel: RoomDirectorySharedActionViewModel
    private val viewModel: CreateRoomViewModel by activityViewModel()

    private val galleryOrCameraDialogHelper = GalleryOrCameraDialogHelper(this, colorProvider)

    override fun getLayoutResId() = R.layout.fragment_create_room

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vectorBaseActivity.setSupportActionBar(createRoomToolbar)
        sharedActionViewModel = activityViewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        setupRecyclerView()
        createRoomClose.debouncedClicks {
            sharedActionViewModel.post(RoomDirectorySharedAction.Back)
        }
        viewModel.observeViewEvents {
            when (it) {
                CreateRoomViewEvents.Quit -> vectorBaseActivity.onBackPressed()
            }.exhaustive
        }
    }

    override fun onDestroyView() {
        createRoomForm.cleanup()
        createRoomController.listener = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        createRoomForm.configureWith(createRoomController)
        createRoomController.listener = this
    }

    override fun onAvatarDelete() {
        viewModel.handle(CreateRoomAction.SetAvatar(null))
    }

    override fun onAvatarChange() {
        galleryOrCameraDialogHelper.show()
    }

    override fun onImageReady(uri: Uri?) {
        viewModel.handle(CreateRoomAction.SetAvatar(uri))
    }

    override fun onNameChange(newName: String) {
        viewModel.handle(CreateRoomAction.SetName(newName))
    }

    override fun onTopicChange(newTopic: String) {
        viewModel.handle(CreateRoomAction.SetTopic(newTopic))
    }

    override fun setIsPublic(isPublic: Boolean) {
        viewModel.handle(CreateRoomAction.SetIsPublic(isPublic))
    }

    override fun setIsInRoomDirectory(isInRoomDirectory: Boolean) {
        viewModel.handle(CreateRoomAction.SetIsInRoomDirectory(isInRoomDirectory))
    }

    override fun setIsEncrypted(isEncrypted: Boolean) {
        viewModel.handle(CreateRoomAction.SetIsEncrypted(isEncrypted))
    }

    override fun submit() {
        viewModel.handle(CreateRoomAction.Create)
    }

    override fun retry() {
        Timber.v("Retry")
        viewModel.handle(CreateRoomAction.Create)
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        return withState(viewModel) {
            return@withState if (!it.isEmpty()) {
                AlertDialog.Builder(requireContext())
                        .setTitle(R.string.dialog_title_warning)
                        .setMessage(R.string.warning_room_not_created_yet)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.handle(CreateRoomAction.Reset)
                        }
                        .setNegativeButton(R.string.no, null)
                        .show()
                true
            } else {
                false
            }
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        val async = state.asyncCreateRoomRequest
        if (async is Success) {
            // Navigate to freshly created room
            navigator.openRoom(requireActivity(), async())

            sharedActionViewModel.post(RoomDirectorySharedAction.Close)
        } else {
            // Populate list with Epoxy
            createRoomController.setData(state)
        }
    }
}
