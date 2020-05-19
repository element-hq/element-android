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

package im.vector.riotx.features.roomprofile.uploads.media

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.trackItemsVisibilityChange
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.utils.DimensionConverter
import im.vector.riotx.features.media.ImageContentRenderer
import im.vector.riotx.features.media.VideoContentRenderer
import im.vector.riotx.features.roomprofile.uploads.RoomUploadsAction
import im.vector.riotx.features.roomprofile.uploads.RoomUploadsViewModel
import kotlinx.android.synthetic.main.fragment_generic_recycler.*
import javax.inject.Inject

class RoomUploadsMediaFragment @Inject constructor(
        private val controller: UploadsMediaController,
        private val dimensionConverter: DimensionConverter
) : VectorBaseFragment(), UploadsMediaController.Listener {

    private val uploadsViewModel by parentFragmentViewModel(RoomUploadsViewModel::class)

    override fun getLayoutResId() = R.layout.fragment_generic_recycler

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.trackItemsVisibilityChange()
        recyclerView.layoutManager = GridLayoutManager(context, getNumberOfColumns())
        recyclerView.adapter = controller.adapter
        recyclerView.setHasFixedSize(true)

        controller.listener = this
    }

    private fun getNumberOfColumns(): Int {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        return dimensionConverter.pdToDp(displayMetrics.widthPixels) / IMAGE_SIZE_DP
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.cleanup()
        controller.listener = null
    }

    override fun onOpenImageClicked(view: View, mediaData: ImageContentRenderer.Data) {
        navigator.openImageViewer(requireActivity(), mediaData, view, null)
    }

    override fun onOpenVideoClicked(view: View, mediaData: VideoContentRenderer.Data) {
        navigator.openVideoViewer(requireActivity(), mediaData)
    }

    override fun loadMore() {
        uploadsViewModel.handle(RoomUploadsAction.LoadMore)
    }

    override fun onRetry() {
        uploadsViewModel.handle(RoomUploadsAction.Retry)
    }

    override fun invalidate() = withState(uploadsViewModel) { state ->
        controller.setData(state)
    }
}
