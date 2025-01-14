/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.uploads.media

import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.trackItemsVisibilityChange
import im.vector.app.core.platform.StateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.FragmentGenericStateViewRecyclerBinding
import im.vector.app.features.media.AttachmentData
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.media.VideoContentRenderer
import im.vector.app.features.roomprofile.uploads.RoomUploadsAction
import im.vector.app.features.roomprofile.uploads.RoomUploadsFragment
import im.vector.app.features.roomprofile.uploads.RoomUploadsViewModel
import im.vector.app.features.roomprofile.uploads.RoomUploadsViewState
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.crypto.attachments.toElementToDecrypt
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl
import org.matrix.android.sdk.api.session.room.model.message.getThumbnailUrl
import javax.inject.Inject

@AndroidEntryPoint
class RoomUploadsMediaFragment :
        VectorBaseFragment<FragmentGenericStateViewRecyclerBinding>(),
        UploadsMediaController.Listener,
        StateView.EventCallback {

    @Inject lateinit var controller: UploadsMediaController
    @Inject lateinit var dimensionConverter: DimensionConverter

    private val uploadsViewModel by parentFragmentViewModel(RoomUploadsViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericStateViewRecyclerBinding {
        return FragmentGenericStateViewRecyclerBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.genericStateViewListStateView.contentView = views.genericStateViewListRecycler
        views.genericStateViewListStateView.eventCallback = this
        views.genericStateViewListRecycler.trackItemsVisibilityChange()
        views.genericStateViewListRecycler.layoutManager = GridLayoutManager(context, getNumberOfColumns())
        views.genericStateViewListRecycler.adapter = controller.adapter
        views.genericStateViewListRecycler.setHasFixedSize(true)

        controller.listener = this
    }

    private fun getNumberOfColumns(): Int {
        val screenWidthInPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val a = requireActivity().windowManager.currentWindowMetrics
            a.bounds.width()
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }
        val screenWidthInDp = dimensionConverter.pxToDp(screenWidthInPx)
        return screenWidthInDp / IMAGE_SIZE_DP
    }

    override fun onDestroyView() {
        views.genericStateViewListRecycler.cleanup()
        controller.listener = null
        super.onDestroyView()
    }

    // It's very strange i can't just access
    // the app bar using find by id...
    private fun trickFindAppBar(): AppBarLayout? {
        return activity?.supportFragmentManager?.fragments
                ?.filterIsInstance<RoomUploadsFragment>()
                ?.firstOrNull()
                ?.roomUploadsAppBar
    }

    override fun onOpenImageClicked(view: View, mediaData: ImageContentRenderer.Data) = withState(uploadsViewModel) { state ->
        val inMemory = getItemsArgs(state)
        navigator.openMediaViewer(
                activity = requireActivity(),
                roomId = state.roomId,
                mediaData = mediaData,
                view = view,
                inMemory = inMemory
        ) { pairs ->
            trickFindAppBar()?.let {
                pairs.add(Pair(it, ViewCompat.getTransitionName(it) ?: ""))
            }
        }
    }

    private fun getItemsArgs(state: RoomUploadsViewState): List<AttachmentData> {
        return state.mediaEvents.mapNotNull {
            when (val content = it.contentWithAttachmentContent) {
                is MessageImageContent -> {
                    ImageContentRenderer.Data(
                            eventId = it.eventId,
                            filename = content.body,
                            mimeType = content.mimeType,
                            url = content.getFileUrl(),
                            elementToDecrypt = content.encryptedFileInfo?.toElementToDecrypt(),
                            maxHeight = -1,
                            maxWidth = -1,
                            width = null,
                            height = null
                    )
                }
                is MessageVideoContent -> {
                    val thumbnailData = ImageContentRenderer.Data(
                            eventId = it.eventId,
                            filename = content.body,
                            mimeType = content.mimeType,
                            url = content.videoInfo?.getThumbnailUrl(),
                            elementToDecrypt = content.videoInfo?.thumbnailFile?.toElementToDecrypt(),
                            height = content.videoInfo?.height,
                            maxHeight = -1,
                            width = content.videoInfo?.width,
                            maxWidth = -1
                    )
                    VideoContentRenderer.Data(
                            eventId = it.eventId,
                            filename = content.body,
                            mimeType = content.mimeType,
                            url = content.getFileUrl(),
                            elementToDecrypt = content.encryptedFileInfo?.toElementToDecrypt(),
                            thumbnailMediaData = thumbnailData
                    )
                }
                else -> null
            }
        }
    }

    override fun onOpenVideoClicked(view: View, mediaData: VideoContentRenderer.Data) = withState(uploadsViewModel) { state ->
        val inMemory = getItemsArgs(state)
        navigator.openMediaViewer(
                activity = requireActivity(),
                roomId = state.roomId,
                mediaData = mediaData,
                view = view,
                inMemory = inMemory
        ) { pairs ->
            trickFindAppBar()?.let {
                pairs.add(Pair(it, ViewCompat.getTransitionName(it) ?: ""))
            }
        }
    }

    override fun loadMore() {
        uploadsViewModel.handle(RoomUploadsAction.LoadMore)
    }

    override fun onRetryClicked() {
        uploadsViewModel.handle(RoomUploadsAction.Retry)
    }

    override fun invalidate() = withState(uploadsViewModel) { state ->
        if (state.mediaEvents.isEmpty()) {
            when (state.asyncEventsRequest) {
                is Loading -> {
                    views.genericStateViewListStateView.state = StateView.State.Loading
                }
                is Fail -> {
                    views.genericStateViewListStateView.state = StateView.State.Error(errorFormatter.toHumanReadable(state.asyncEventsRequest.error))
                }
                is Success -> {
                    if (state.hasMore) {
                        // We need to load more items
                        loadMore()
                    } else {
                        views.genericStateViewListStateView.state = StateView.State.Empty(
                                title = getString(CommonStrings.uploads_media_no_result),
                                image = ContextCompat.getDrawable(requireContext(), R.drawable.ic_image)
                        )
                    }
                }
                else -> Unit
            }
        } else {
            views.genericStateViewListStateView.state = StateView.State.Content
            controller.setData(state)
        }
    }
}
