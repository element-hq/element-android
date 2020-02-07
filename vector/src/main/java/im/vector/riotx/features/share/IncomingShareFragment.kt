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

package im.vector.riotx.features.share

import android.content.ClipDescription
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.kbeanie.multipicker.utils.IntentUtils
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.attachments.AttachmentsHelper
import im.vector.riotx.features.login.LoginActivity
import kotlinx.android.synthetic.main.fragment_incoming_share.*
import javax.inject.Inject

class IncomingShareFragment @Inject constructor(
        val incomingShareViewModelFactory: IncomingShareViewModel.Factory,
        private val incomingShareController: IncomingShareController,
        private val sessionHolder: ActiveSessionHolder
) : VectorBaseFragment(), AttachmentsHelper.Callback, IncomingShareController.Callback {

    private lateinit var attachmentsHelper: AttachmentsHelper
    private val incomingShareViewModel: IncomingShareViewModel by fragmentViewModel()

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
        attachmentsHelper = AttachmentsHelper.create(this, this).register()

        val intent = vectorBaseActivity.intent
        if (intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_SEND_MULTIPLE) {
            var isShareManaged = attachmentsHelper.handleShareIntent(
                    IntentUtils.getPickerIntentForSharing(intent)
            )
            if (!isShareManaged) {
                isShareManaged = handleTextShare(intent)
            }
            if (!isShareManaged) {
                cannotManageShare()
            }
        } else {
            cannotManageShare()
        }

        incomingShareSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                incomingShareViewModel.handle(IncomingShareAction.FilterWith(newText))
                return true
            }
        })
        sendShareButton.setOnClickListener { _ ->
            handleSendShare()
        }
        incomingShareViewModel.observeViewEvents {
            when (it) {
                is IncomingShareViewEvents.ShareToRoom -> handleShareToRoom(it)
            }.exhaustive
        }
    }

    private fun handleShareToRoom(event: IncomingShareViewEvents.ShareToRoom) {
        if (event.showAlert) {
            showConfirmationDialog(event.roomSummary, event.sharedData)
        } else {
            navigator.openRoomForSharing(requireActivity(), event.roomSummary.roomId, event.sharedData)
        }
    }

    private fun handleSendShare() {
        incomingShareViewModel.handle(IncomingShareAction.ShareToSelectedRooms)
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
        incomingShareViewModel.handle(IncomingShareAction.UpdateSharedData(sharedData))
    }

    override fun onAttachmentsProcessFailed() {
        cannotManageShare()
    }

    private fun cannotManageShare() {
        Toast.makeText(requireContext(), R.string.error_handling_incoming_share, Toast.LENGTH_LONG).show()
        requireActivity().finish()
    }

    private fun handleTextShare(intent: Intent): Boolean {
        if (intent.type == ClipDescription.MIMETYPE_TEXT_PLAIN) {
            val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            return if (sharedText.isNullOrEmpty()) {
                false
            } else {
                val sharedData = SharedData.Text(sharedText)
                incomingShareViewModel.handle(IncomingShareAction.UpdateSharedData(sharedData))
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
                    navigator.openRoomForSharing(requireActivity(), roomSummary.roomId, sharedData)
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

    override fun invalidate() = withState(incomingShareViewModel) {
        sendShareButton.isVisible = it.multiSelectionEnabled
        incomingShareController.setData(it)
    }

    override fun onRoomClicked(roomSummary: RoomSummary) {
        incomingShareViewModel.handle(IncomingShareAction.SelectRoom(roomSummary, false))
    }

    override fun onRoomLongClicked(roomSummary: RoomSummary): Boolean {
        incomingShareViewModel.handle(IncomingShareAction.SelectRoom(roomSummary, true))
        return true
    }
}
