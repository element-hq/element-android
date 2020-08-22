/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.share

import android.app.Activity
import android.content.ClipDescription
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.PERMISSIONS_FOR_WRITING_FILES
import im.vector.app.core.utils.PERMISSION_REQUEST_CODE_PICK_ATTACHMENT
import im.vector.app.core.utils.allGranted
import im.vector.app.core.utils.checkPermissions
import im.vector.app.features.attachments.AttachmentsHelper
import im.vector.app.features.attachments.preview.AttachmentsPreviewActivity
import im.vector.app.features.attachments.preview.AttachmentsPreviewArgs
import im.vector.app.features.login.LoginActivity
import kotlinx.android.synthetic.main.fragment_incoming_share.*
import javax.inject.Inject

/**
 * Display the list of rooms
 * The user can select multiple rooms to send the data to
 */
class IncomingShareFragment @Inject constructor(
        val incomingShareViewModelFactory: IncomingShareViewModel.Factory,
        private val incomingShareController: IncomingShareController,
        private val sessionHolder: ActiveSessionHolder
) : VectorBaseFragment(), AttachmentsHelper.Callback, IncomingShareController.Callback {

    private lateinit var attachmentsHelper: AttachmentsHelper
    private val viewModel: IncomingShareViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_incoming_share

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // If we are not logged in, stop the sharing process and open login screen.
        // In the future, we might want to relaunch the sharing process after login.
        if (!sessionHolder.hasActiveSession()) {
            startLoginActivity()
            return
        }
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupToolbar(incomingShareToolbar)
        attachmentsHelper = AttachmentsHelper(requireContext(), this).register()

        val intent = vectorBaseActivity.intent
        val isShareManaged = when (intent?.action) {
            Intent.ACTION_SEND          -> {
                var isShareManaged = attachmentsHelper.handleShareIntent(requireContext(), intent)
                if (!isShareManaged) {
                    isShareManaged = handleTextShare(intent)
                }
                isShareManaged
            }
            Intent.ACTION_SEND_MULTIPLE -> attachmentsHelper.handleShareIntent(requireContext(), intent)
            else                        -> false
        }

        if (!isShareManaged) {
            cannotManageShare(R.string.error_handling_incoming_share)
        }

        incomingShareSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.handle(IncomingShareAction.FilterWith(newText))
                return true
            }
        })
        sendShareButton.setOnClickListener { _ ->
            handleSendShare()
        }
        viewModel.observeViewEvents {
            when (it) {
                is IncomingShareViewEvents.ShareToRoom            -> handleShareToRoom(it)
                is IncomingShareViewEvents.EditMediaBeforeSending -> handleEditMediaBeforeSending(it)
                is IncomingShareViewEvents.MultipleRoomsShareDone -> handleMultipleRoomsShareDone(it)
            }.exhaustive
        }
    }

    private fun handleMultipleRoomsShareDone(viewEvent: IncomingShareViewEvents.MultipleRoomsShareDone) {
        requireActivity().let {
            navigator.openRoom(it, viewEvent.roomId)
            it.finish()
        }
    }

    private fun handleEditMediaBeforeSending(event: IncomingShareViewEvents.EditMediaBeforeSending) {
        val intent = AttachmentsPreviewActivity.newIntent(requireContext(), AttachmentsPreviewArgs(event.contentAttachmentData))
        startActivityForResult(intent, AttachmentsPreviewActivity.REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val hasBeenHandled = attachmentsHelper.onActivityResult(requestCode, resultCode, data)
        if (!hasBeenHandled && resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                AttachmentsPreviewActivity.REQUEST_CODE -> {
                    val sendData = AttachmentsPreviewActivity.getOutput(data)
                    val keepOriginalSize = AttachmentsPreviewActivity.getKeepOriginalSize(data)
                    viewModel.handle(IncomingShareAction.UpdateSharedData(SharedData.Attachments(sendData)))
                    viewModel.handle(IncomingShareAction.ShareMedia(keepOriginalSize))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // We need the read file permission
        checkPermissions(PERMISSIONS_FOR_WRITING_FILES, this, PERMISSION_REQUEST_CODE_PICK_ATTACHMENT)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE_PICK_ATTACHMENT && !allGranted(grantResults)) {
            // Permission is mandatory
            cannotManageShare(R.string.missing_permissions_error)
        }
    }

    private fun handleShareToRoom(event: IncomingShareViewEvents.ShareToRoom) {
        if (event.showAlert) {
            showConfirmationDialog(event.roomSummary, event.sharedData)
        } else {
            navigator.openRoomForSharingAndFinish(requireActivity(), event.roomSummary.roomId, event.sharedData)
        }
    }

    private fun handleSendShare() {
        viewModel.handle(IncomingShareAction.ShareToSelectedRooms)
    }

    override fun onDestroyView() {
        incomingShareController.callback = null
        incomingShareRoomList.cleanup()
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        incomingShareRoomList.configureWith(incomingShareController, hasFixedSize = true)
        incomingShareController.callback = this
    }

    override fun onContentAttachmentsReady(attachments: List<ContentAttachmentData>) {
        val sharedData = SharedData.Attachments(attachments)
        viewModel.handle(IncomingShareAction.UpdateSharedData(sharedData))
    }

    override fun onAttachmentsProcessFailed() {
        cannotManageShare(R.string.error_handling_incoming_share)
    }

    private fun cannotManageShare(@StringRes messageResId: Int) {
        Toast.makeText(requireContext(), messageResId, Toast.LENGTH_LONG).show()
        requireActivity().finish()
    }

    private fun handleTextShare(intent: Intent): Boolean {
        if (intent.type == ClipDescription.MIMETYPE_TEXT_PLAIN) {
            val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            return if (sharedText.isNullOrEmpty()) {
                false
            } else {
                val sharedData = SharedData.Text(sharedText)
                viewModel.handle(IncomingShareAction.UpdateSharedData(sharedData))
                true
            }
        }
        return false
    }

    private fun showConfirmationDialog(roomSummary: RoomSummary, sharedData: SharedData) {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.send_attachment)
                .setMessage(getString(R.string.share_confirm_room, roomSummary.displayName))
                .setPositiveButton(R.string.send) { _, _ ->
                    navigator.openRoomForSharingAndFinish(requireActivity(), roomSummary.roomId, sharedData)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun startLoginActivity() {
        val intent = LoginActivity.newIntent(requireActivity(), null)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun invalidate() = withState(viewModel) {
        sendShareButton.isVisible = it.isInMultiSelectionMode
        incomingShareController.setData(it)
    }

    override fun onRoomClicked(roomSummary: RoomSummary) {
        viewModel.handle(IncomingShareAction.SelectRoom(roomSummary, false))
    }

    override fun onRoomLongClicked(roomSummary: RoomSummary): Boolean {
        viewModel.handle(IncomingShareAction.SelectRoom(roomSummary, true))
        return true
    }
}
