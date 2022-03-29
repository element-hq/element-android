/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package im.vector.app.features.attachments.preview

import im.vector.app.core.platform.VectorViewModel

class AttachmentsPreviewViewModel(initialState: AttachmentsPreviewViewState) :
    VectorViewModel<AttachmentsPreviewViewState, AttachmentsPreviewAction, AttachmentsPreviewViewEvents>(initialState) {

    override fun handle(action: AttachmentsPreviewAction) {
        when (action) {
            is AttachmentsPreviewAction.SetCurrentAttachment          -> handleSetCurrentAttachment(action)
            is AttachmentsPreviewAction.UpdatePathOfCurrentAttachment -> handleUpdatePathOfCurrentAttachment(action)
            AttachmentsPreviewAction.RemoveCurrentAttachment          -> handleRemoveCurrentAttachment()
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
