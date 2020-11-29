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

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.yalantis.ucrop.UCrop
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.insertBeforeLast
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.OnSnapPositionChangeListener
import im.vector.app.core.utils.SnapOnScrollListener
import im.vector.app.core.utils.attachSnapHelperWithListener
import im.vector.app.features.media.createUCropWithDefaultSettings
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_attachments_preview.*
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import java.io.File
import javax.inject.Inject

@Parcelize
data class AttachmentsPreviewArgs(
        val attachments: List<ContentAttachmentData>
) : Parcelable

class AttachmentsPreviewFragment @Inject constructor(
        val viewModelFactory: AttachmentsPreviewViewModel.Factory,
        private val attachmentMiniaturePreviewController: AttachmentMiniaturePreviewController,
        private val attachmentBigPreviewController: AttachmentBigPreviewController,
        private val colorProvider: ColorProvider
) : VectorBaseFragment(), AttachmentMiniaturePreviewController.Callback {

    private val fragmentArgs: AttachmentsPreviewArgs by args()
    private val viewModel: AttachmentsPreviewViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_attachments_preview

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyInsets()
        setupRecyclerViews()
        setupToolbar(attachmentPreviewerToolbar)
        attachmentPreviewerSendButton.setOnClickListener {
            setResultAndFinish()
        }
    }

    private val uCropActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == RESULT_OK) {
            val resultUri = activityResult.data?.let { UCrop.getOutput(it) }
            if (resultUri != null) {
                viewModel.handle(AttachmentsPreviewAction.UpdatePathOfCurrentAttachment(resultUri))
            } else {
                Toast.makeText(requireContext(), "Cannot retrieve cropped value", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.attachmentsPreviewRemoveAction -> {
                handleRemoveAction()
                true
            }
            R.id.attachmentsPreviewEditAction   -> {
                handleEditAction()
                true
            }
            else                                -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        withState(viewModel) { state ->
            val editMenuItem = menu.findItem(R.id.attachmentsPreviewEditAction)
            val showEditMenuItem = state.attachments.getOrNull(state.currentAttachmentIndex)?.isEditable().orFalse()
            editMenuItem.setVisible(showEditMenuItem)
        }

        super.onPrepareOptionsMenu(menu)
    }

    override fun getMenuRes() = R.menu.vector_attachments_preview

    override fun onDestroyView() {
        super.onDestroyView()
        attachmentPreviewerMiniatureList.cleanup()
        attachmentPreviewerBigList.cleanup()
        attachmentMiniaturePreviewController.callback = null
    }

    override fun invalidate() = withState(viewModel) { state ->
        invalidateOptionsMenu()
        if (state.attachments.isEmpty()) {
            requireActivity().setResult(RESULT_CANCELED)
            requireActivity().finish()
        } else {
            attachmentMiniaturePreviewController.setData(state)
            attachmentBigPreviewController.setData(state)
            attachmentPreviewerBigList.scrollToPosition(state.currentAttachmentIndex)
            attachmentPreviewerMiniatureList.scrollToPosition(state.currentAttachmentIndex)
            attachmentPreviewerSendImageOriginalSize.text = resources.getQuantityString(R.plurals.send_images_with_original_size, state.attachments.size)
        }
    }

    override fun onAttachmentClicked(position: Int, contentAttachmentData: ContentAttachmentData) {
        viewModel.handle(AttachmentsPreviewAction.SetCurrentAttachment(position))
    }

    private fun setResultAndFinish() = withState(viewModel) {
        (requireActivity() as? AttachmentsPreviewActivity)?.setResultAndFinish(
                it.attachments,
                attachmentPreviewerSendImageOriginalSize.isChecked
        )
    }

    private fun applyInsets() {
        view?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        ViewCompat.setOnApplyWindowInsetsListener(attachmentPreviewerBottomContainer) { v, insets ->
            v.updatePadding(bottom = insets.systemWindowInsetBottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(attachmentPreviewerToolbar) { v, insets ->
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.systemWindowInsetTop
            }
            insets
        }
    }

    private fun handleRemoveAction() {
        viewModel.handle(AttachmentsPreviewAction.RemoveCurrentAttachment)
    }

    private fun handleEditAction() = withState(viewModel) {
        val currentAttachment = it.attachments.getOrNull(it.currentAttachmentIndex) ?: return@withState
        val destinationFile = File(requireContext().cacheDir, currentAttachment.name.insertBeforeLast("_edited_image_${System.currentTimeMillis()}"))
        val uri = currentAttachment.queryUri
        createUCropWithDefaultSettings(colorProvider, uri, destinationFile.toUri(), currentAttachment.name)
                .getIntent(requireContext())
                .let { intent -> uCropActivityResultLauncher.launch(intent) }
    }

    private fun setupRecyclerViews() {
        attachmentMiniaturePreviewController.callback = this

        attachmentPreviewerMiniatureList.let {
            it.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            it.setHasFixedSize(true)
            it.adapter = attachmentMiniaturePreviewController.adapter
        }

        attachmentPreviewerBigList.let {
            it.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            it.attachSnapHelperWithListener(
                    PagerSnapHelper(),
                    SnapOnScrollListener.Behavior.NOTIFY_ON_SCROLL_STATE_IDLE,
                    object : OnSnapPositionChangeListener {
                        override fun onSnapPositionChange(position: Int) {
                            viewModel.handle(AttachmentsPreviewAction.SetCurrentAttachment(position))
                        }
                    })
            it.setHasFixedSize(true)
            it.adapter = attachmentBigPreviewController.adapter
        }
    }
}
