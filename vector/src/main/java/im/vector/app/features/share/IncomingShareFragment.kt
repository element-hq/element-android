/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.share

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentIncomingShareBinding
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.attachments.ShareIntentHandler
import im.vector.app.features.attachments.preview.AttachmentsPreviewActivity
import im.vector.app.features.attachments.preview.AttachmentsPreviewArgs
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

/**
 * Display the list of rooms.
 * The user can select multiple rooms to send the data to.
 */
@AndroidEntryPoint
class IncomingShareFragment :
        VectorBaseFragment<FragmentIncomingShareBinding>(),
        IncomingShareController.Callback {

    @Inject lateinit var incomingShareController: IncomingShareController
    @Inject lateinit var shareIntentHandler: ShareIntentHandler

    private val viewModel: IncomingShareViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentIncomingShareBinding {
        return FragmentIncomingShareBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupToolbar(views.incomingShareToolbar)

        viewModel.observeViewEvents {
            when (it) {
                is IncomingShareViewEvents.ShareToRoom -> handleShareToRoom(it)
                is IncomingShareViewEvents.EditMediaBeforeSending -> handleEditMediaBeforeSending(it)
                is IncomingShareViewEvents.MultipleRoomsShareDone -> handleMultipleRoomsShareDone(it)
            }
        }

        val intent = vectorBaseActivity.intent
        val isShareManaged = when (intent?.action) {
            Intent.ACTION_SEND -> {
                val isShareManaged = handleIncomingShareIntent(intent)
                // Direct share
                if (intent.hasExtra(Intent.EXTRA_SHORTCUT_ID)) {
                    val roomId = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)!!
                    viewModel.handle(IncomingShareAction.ShareToRoom(roomId))
                }
                isShareManaged
            }
            Intent.ACTION_SEND_MULTIPLE -> handleIncomingShareIntent(intent)
            else -> false
        }

        if (!isShareManaged) {
            cannotManageShare(CommonStrings.error_handling_incoming_share)
        }

        views.incomingShareSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.handle(IncomingShareAction.FilterWith(newText))
                return true
            }
        })
        views.sendShareButton.debouncedClicks {
            handleSendShare()
        }
    }

    private fun handleIncomingShareIntent(intent: Intent) = shareIntentHandler.handleIncomingShareIntent(
            intent,
            onFile = {
                val sharedData = SharedData.Attachments(it)
                viewModel.handle(IncomingShareAction.UpdateSharedData(sharedData))
            },
            onPlainText = {
                val sharedData = SharedData.Text(it)
                viewModel.handle(IncomingShareAction.UpdateSharedData(sharedData))
            }
    )

    private fun handleMultipleRoomsShareDone(viewEvent: IncomingShareViewEvents.MultipleRoomsShareDone) {
        requireActivity().let {
            navigator.openRoom(
                    context = it,
                    roomId = viewEvent.roomId,
                    trigger = ViewRoom.Trigger.MobileLinkShare
            )
            it.finish()
        }
    }

    private fun handleEditMediaBeforeSending(event: IncomingShareViewEvents.EditMediaBeforeSending) {
        val intent = AttachmentsPreviewActivity.newIntent(requireContext(), AttachmentsPreviewArgs(event.contentAttachmentData))
        attachmentPreviewActivityResultLauncher.launch(intent)
    }

    private val attachmentPreviewActivityResultLauncher = registerStartForActivityResult {
        val data = it.data ?: return@registerStartForActivityResult
        if (it.resultCode == Activity.RESULT_OK) {
            val sendData = AttachmentsPreviewActivity.getOutput(data)
            val keepOriginalSize = AttachmentsPreviewActivity.getKeepOriginalSize(data)
            viewModel.handle(IncomingShareAction.UpdateSharedData(SharedData.Attachments(sendData)))
            viewModel.handle(IncomingShareAction.ShareMedia(keepOriginalSize))
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
        views.incomingShareRoomList.cleanup()
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        views.incomingShareRoomList.configureWith(incomingShareController, hasFixedSize = true)
        incomingShareController.callback = this
    }

    private fun cannotManageShare(@StringRes messageResId: Int) {
        Toast.makeText(requireContext(), messageResId, Toast.LENGTH_LONG).show()
        requireActivity().finish()
    }

    private fun showConfirmationDialog(roomSummary: RoomSummary, sharedData: SharedData) {
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(CommonStrings.send_attachment)
                .setMessage(getString(CommonStrings.share_confirm_room, roomSummary.displayName))
                .setPositiveButton(CommonStrings.action_send) { _, _ ->
                    navigator.openRoomForSharingAndFinish(requireActivity(), roomSummary.roomId, sharedData)
                }
                .setNegativeButton(CommonStrings.action_cancel, null)
                .show()
    }

    override fun invalidate() = withState(viewModel) {
        views.sendShareButton.isVisible = it.isInMultiSelectionMode
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
