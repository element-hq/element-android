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
import android.os.Parcelable
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
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
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_create_room.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import javax.inject.Inject

@Parcelize
data class CreateRoomArgs(
        val initialName: String
) : Parcelable

class CreateRoomFragment @Inject constructor(
        private val createRoomController: CreateRoomController,
        val createRoomViewModelFactory: CreateRoomViewModel.Factory,
        colorProvider: ColorProvider
) : VectorBaseFragment(),
        CreateRoomController.Listener,
        GalleryOrCameraDialogHelper.Listener,
        OnBackPressed {

    private lateinit var sharedActionViewModel: RoomDirectorySharedActionViewModel
    private val viewModel: CreateRoomViewModel by fragmentViewModel()
    private val args: CreateRoomArgs by args()

    private val galleryOrCameraDialogHelper = GalleryOrCameraDialogHelper(this, colorProvider)

    override fun getLayoutResId() = R.layout.fragment_create_room

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vectorBaseActivity.setSupportActionBar(createRoomToolbar)
        sharedActionViewModel = activityViewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        setupWaitingView()
        setupRecyclerView()
        createRoomClose.debouncedClicks {
            sharedActionViewModel.post(RoomDirectorySharedAction.Back)
        }
        viewModel.observeViewEvents {
            when (it) {
                CreateRoomViewEvents.Quit       -> vectorBaseActivity.onBackPressed()
                is CreateRoomViewEvents.Failure -> showFailure(it.throwable)
            }.exhaustive
        }
    }

    override fun showFailure(throwable: Throwable) {
        // Note: RoomAliasError are displayed directly in the form
        if (throwable !is CreateRoomFailure.AliasError) {
            super.showFailure(throwable)
        }
    }

    private fun setupWaitingView() {
        waiting_view_status_text.isVisible = true
        waiting_view_status_text.setText(R.string.create_room_in_progress)
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

    override fun setAliasLocalPart(aliasLocalPart: String) {
        viewModel.handle(CreateRoomAction.SetRoomAliasLocalPart(aliasLocalPart))
    }

    override fun setIsEncrypted(isEncrypted: Boolean) {
        viewModel.handle(CreateRoomAction.SetIsEncrypted(isEncrypted))
    }

    override fun toggleShowAdvanced() {
        viewModel.handle(CreateRoomAction.ToggleShowAdvanced)
    }

    override fun setDisableFederation(disableFederation: Boolean) {
        viewModel.handle(CreateRoomAction.DisableFederation(disableFederation))
    }

    override fun submit() {
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
        waiting_view.isVisible = async is Loading
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
