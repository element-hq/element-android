/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.attachments.preview

import im.vector.app.core.platform.VectorViewModel

class AttachmentsPreviewViewModel(initialState: AttachmentsPreviewViewState) :
        VectorViewModel<AttachmentsPreviewViewState, AttachmentsPreviewAction, AttachmentsPreviewViewEvents>(initialState) {

    override fun handle(action: AttachmentsPreviewAction) {
        when (action) {
            is AttachmentsPreviewAction.SetCurrentAttachment -> handleSetCurrentAttachment(action)
            is AttachmentsPreviewAction.UpdatePathOfCurrentAttachment -> handleUpdatePathOfCurrentAttachment(action)
            AttachmentsPreviewAction.RemoveCurrentAttachment -> handleRemoveCurrentAttachment()
        }
    }

    private fun handleRemoveCurrentAttachment() = withState {
        val currentAttachment = it.attachments.getOrNull(it.currentAttachmentIndex) ?: return@withState
        val attachments = it.attachments.minusElement(currentAttachment)
        val newAttachmentIndex = it.currentAttachmentIndex.coerceAtMost(attachments.size - 1)
        setState {
            copy(attachments = attachments, currentAttachmentIndex = newAttachmentIndex)
        }
    }

    private fun handleUpdatePathOfCurrentAttachment(action: AttachmentsPreviewAction.UpdatePathOfCurrentAttachment) = withState {
        val attachments = it.attachments.mapIndexed { index, contentAttachmentData ->
            if (index == it.currentAttachmentIndex) {
                contentAttachmentData.copy(queryUri = action.newUri)
            } else {
                contentAttachmentData
            }
        }
        setState {
            copy(attachments = attachments)
        }
    }

    private fun handleSetCurrentAttachment(action: AttachmentsPreviewAction.SetCurrentAttachment) = setState {
        copy(currentAttachmentIndex = action.index)
    }
}
