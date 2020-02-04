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

package im.vector.riotx.features.attachments.preview

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.platform.VectorBaseFragment
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_attachments_preview.*
import javax.inject.Inject

@Parcelize
data class AttachmentsPreviewArgs(
        val attachments: List<ContentAttachmentData>
) : Parcelable

class AttachmentsPreviewFragment @Inject constructor(
        val viewModelFactory: AttachmentsPreviewViewModel.Factory,
        private val attachmentPreviewController: AttachmentPreviewController
) : VectorBaseFragment() {

    private val fragmentArgs: AttachmentsPreviewArgs by args()
    private val viewModel: AttachmentsPreviewViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_attachments_preview

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        attachmentPreviewerList.cleanup()
    }

    private fun setupRecyclerView() {
        attachmentPreviewerList.configureWith(attachmentPreviewController, hasFixedSize = true)
    }

    override fun invalidate() = withState(viewModel) { state ->

    }
}
