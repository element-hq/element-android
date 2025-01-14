/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.attachments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetAttachmentTypeSelectorBinding
import im.vector.app.features.home.room.detail.TimelineViewModel

@AndroidEntryPoint
class AttachmentTypeSelectorBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetAttachmentTypeSelectorBinding>() {

    private val viewModel: AttachmentTypeSelectorViewModel by parentFragmentViewModel()
    private val timelineViewModel: TimelineViewModel by parentFragmentViewModel()
    private val sharedActionViewModel: AttachmentTypeSelectorSharedActionViewModel by viewModels(
            ownerProducer = { requireParentFragment() }
    )

    override val showExpanded = true

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetAttachmentTypeSelectorBinding {
        return BottomSheetAttachmentTypeSelectorBinding.inflate(inflater, container, false)
    }

    override fun invalidate() = withState(viewModel, timelineViewModel) { viewState, timelineState ->
        super.invalidate()
        views.location.isVisible = viewState.isLocationVisible
        views.voiceBroadcast.isVisible = viewState.isVoiceBroadcastVisible
        views.poll.isVisible = !timelineState.isThreadTimeline()
        views.textFormatting.isChecked = viewState.isTextFormattingEnabled
        views.textFormatting.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (viewState.isTextFormattingEnabled) {
                    R.drawable.ic_text_formatting
                } else {
                    R.drawable.ic_text_formatting_disabled
                }, 0, 0, 0
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.gallery.debouncedClicks { onAttachmentSelected(AttachmentType.GALLERY) }
        views.stickers.debouncedClicks { onAttachmentSelected(AttachmentType.STICKER) }
        views.file.debouncedClicks { onAttachmentSelected(AttachmentType.FILE) }
        views.voiceBroadcast.debouncedClicks { onAttachmentSelected(AttachmentType.VOICE_BROADCAST) }
        views.poll.debouncedClicks { onAttachmentSelected(AttachmentType.POLL) }
        views.location.debouncedClicks { onAttachmentSelected(AttachmentType.LOCATION) }
        views.camera.debouncedClicks { onAttachmentSelected(AttachmentType.CAMERA) }
        views.contact.debouncedClicks { onAttachmentSelected(AttachmentType.CONTACT) }
        views.textFormatting.setOnCheckedChangeListener { _, isChecked -> onTextFormattingToggled(isChecked) }
    }

    private fun onAttachmentSelected(attachmentType: AttachmentType) {
        val action = AttachmentTypeSelectorSharedAction.SelectAttachmentTypeAction(attachmentType)
        sharedActionViewModel.post(action)
        dismiss()
    }

    private fun onTextFormattingToggled(isEnabled: Boolean) =
            viewModel.handle(AttachmentTypeSelectorAction.ToggleTextFormatting(isEnabled))

    companion object {
        fun show(fragmentManager: FragmentManager) {
            val bottomSheet = AttachmentTypeSelectorBottomSheet()
            bottomSheet.show(fragmentManager, "AttachmentTypeSelectorBottomSheet")
        }
    }
}
