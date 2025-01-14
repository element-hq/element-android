/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.attachments.preview

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.insertBeforeLast
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.OnSnapPositionChangeListener
import im.vector.app.core.utils.SnapOnScrollListener
import im.vector.app.core.utils.attachSnapHelperWithListener
import im.vector.app.databinding.FragmentAttachmentsPreviewBinding
import im.vector.app.features.media.createUCropWithDefaultSettings
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import java.io.File
import javax.inject.Inject

@Parcelize
data class AttachmentsPreviewArgs(
        val attachments: List<ContentAttachmentData>
) : Parcelable

@AndroidEntryPoint
class AttachmentsPreviewFragment :
        VectorBaseFragment<FragmentAttachmentsPreviewBinding>(),
        AttachmentMiniaturePreviewController.Callback,
        VectorMenuProvider {

    @Inject lateinit var attachmentMiniaturePreviewController: AttachmentMiniaturePreviewController
    @Inject lateinit var attachmentBigPreviewController: AttachmentBigPreviewController
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var clock: Clock

    private val fragmentArgs: AttachmentsPreviewArgs by args()
    private val viewModel: AttachmentsPreviewViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAttachmentsPreviewBinding {
        return FragmentAttachmentsPreviewBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyInsets()
        setupRecyclerViews()
        setupToolbar(views.attachmentPreviewerToolbar)
        views.attachmentPreviewerSendButton.debouncedClicks {
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

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.attachmentsPreviewRemoveAction -> {
                handleRemoveAction()
                true
            }
            R.id.attachmentsPreviewEditAction -> {
                handleEditAction()
                true
            }
            else -> false
        }
    }

    override fun handlePrepareMenu(menu: Menu) {
        withState(viewModel) { state ->
            val editMenuItem = menu.findItem(R.id.attachmentsPreviewEditAction)
            val showEditMenuItem = state.attachments.getOrNull(state.currentAttachmentIndex)?.isEditable().orFalse()
            editMenuItem.setVisible(showEditMenuItem)
        }
    }

    override fun getMenuRes() = R.menu.vector_attachments_preview

    override fun onDestroyView() {
        views.attachmentPreviewerMiniatureList.cleanup()
        views.attachmentPreviewerBigList.cleanup()
        attachmentMiniaturePreviewController.callback = null
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        invalidateOptionsMenu()
        if (state.attachments.isEmpty()) {
            requireActivity().setResult(RESULT_CANCELED)
            requireActivity().finish()
        } else {
            attachmentMiniaturePreviewController.setData(state)
            attachmentBigPreviewController.setData(state)
            views.attachmentPreviewerBigList.scrollToPosition(state.currentAttachmentIndex)
            views.attachmentPreviewerMiniatureList.scrollToPosition(state.currentAttachmentIndex)
            views.attachmentPreviewerSendImageOriginalSize.text = getCheckboxText(state)
        }
    }

    private fun getCheckboxText(state: AttachmentsPreviewViewState): CharSequence {
        val nbImages = state.attachments.count { it.type == ContentAttachmentData.Type.IMAGE }
        val nbVideos = state.attachments.count { it.type == ContentAttachmentData.Type.VIDEO }
        return when {
            nbVideos == 0 -> resources.getQuantityString(CommonPlurals.send_images_with_original_size, nbImages)
            nbImages == 0 -> resources.getQuantityString(CommonPlurals.send_videos_with_original_size, nbVideos)
            else -> getString(CommonStrings.send_images_and_video_with_original_size)
        }
    }

    override fun onAttachmentClicked(position: Int, contentAttachmentData: ContentAttachmentData) {
        viewModel.handle(AttachmentsPreviewAction.SetCurrentAttachment(position))
    }

    private fun setResultAndFinish() = withState(viewModel) {
        (requireActivity() as? AttachmentsPreviewActivity)?.setResultAndFinish(
                it.attachments,
                views.attachmentPreviewerSendImageOriginalSize.isChecked
        )
    }

    private fun applyInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.window?.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            view?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        ViewCompat.setOnApplyWindowInsetsListener(views.attachmentPreviewerBottomContainer) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBarsInsets.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(views.attachmentPreviewerToolbar) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBarsInsets.top
            }
            insets
        }
    }

    private fun handleRemoveAction() {
        viewModel.handle(AttachmentsPreviewAction.RemoveCurrentAttachment)
    }

    private fun handleEditAction() = withState(viewModel) {
        val currentAttachment = it.attachments.getOrNull(it.currentAttachmentIndex) ?: return@withState
        val destinationFile = File(requireContext().cacheDir, currentAttachment.name.insertBeforeLast("_edited_image_${clock.epochMillis()}"))
        val uri = currentAttachment.queryUri
        createUCropWithDefaultSettings(colorProvider, uri, destinationFile.toUri(), currentAttachment.name)
                .getIntent(requireContext())
                .let { intent -> uCropActivityResultLauncher.launch(intent) }
    }

    private fun setupRecyclerViews() {
        attachmentMiniaturePreviewController.callback = this

        views.attachmentPreviewerMiniatureList.let {
            it.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            it.setHasFixedSize(true)
            it.adapter = attachmentMiniaturePreviewController.adapter
        }

        views.attachmentPreviewerBigList.let {
            it.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
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
