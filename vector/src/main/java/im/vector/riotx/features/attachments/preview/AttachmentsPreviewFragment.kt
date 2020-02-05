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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
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
        private val attachmentMiniaturePreviewController: AttachmentMiniaturePreviewController,
        private val attachmentBigPreviewController: AttachmentBigPreviewController
) : VectorBaseFragment(), AttachmentMiniaturePreviewController.Callback {

    private val fragmentArgs: AttachmentsPreviewArgs by args()
    private val viewModel: AttachmentsPreviewViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_attachments_preview

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupToolbar(attachmentPreviewerToolbar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        attachmentPreviewerMiniatureList.cleanup()
    }

    private fun setupRecyclerViews() {
        attachmentPreviewerMiniatureList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        attachmentPreviewerMiniatureList.setHasFixedSize(true)
        attachmentPreviewerMiniatureList.adapter = attachmentMiniaturePreviewController.adapter
        attachmentMiniaturePreviewController.callback = this

        attachmentPreviewerBigList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(attachmentPreviewerBigList)
        attachmentPreviewerBigList.setHasFixedSize(true)
        attachmentPreviewerBigList.adapter = attachmentBigPreviewController.adapter

    }

    override fun invalidate() = withState(viewModel) { state ->
        attachmentMiniaturePreviewController.setData(state)
        attachmentBigPreviewController.setData(state)
    }

    override fun onAttachmentClicked(contentAttachmentData: ContentAttachmentData) {

    }

}
