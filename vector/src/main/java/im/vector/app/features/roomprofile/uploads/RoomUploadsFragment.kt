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

package im.vector.app.features.roomprofile.uploads

import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.tabs.TabLayoutMediator
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.intent.getMimeTypeFromUri
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.saveMedia
import im.vector.app.core.utils.shareMedia
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.roomprofile.RoomProfileArgs
import kotlinx.android.synthetic.main.fragment_room_uploads.*
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class RoomUploadsFragment @Inject constructor(
        private val viewModelFactory: RoomUploadsViewModel.Factory,
        private val avatarRenderer: AvatarRenderer,
        private val notificationUtils: NotificationUtils
) : VectorBaseFragment(), RoomUploadsViewModel.Factory by viewModelFactory {

    private val roomProfileArgs: RoomProfileArgs by args()

    private val viewModel: RoomUploadsViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_room_uploads

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sectionsPagerAdapter = RoomUploadsPagerAdapter(this)
        roomUploadsViewPager.adapter = sectionsPagerAdapter

        TabLayoutMediator(roomUploadsTabs, roomUploadsViewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.uploads_media_title)
                1 -> tab.text = getString(R.string.uploads_files_title)
            }
        }.attach()

        setupToolbar(roomUploadsToolbar)

        viewModel.observeViewEvents {
            when (it) {
                is RoomUploadsViewEvents.FileReadyForSharing -> {
                    shareMedia(requireContext(), it.file, getMimeTypeFromUri(requireContext(), it.file.toUri()))
                }
                is RoomUploadsViewEvents.FileReadyForSaving  -> {
                    saveMedia(
                            context = requireContext(),
                            file = it.file,
                            title = it.title,
                            mediaMimeType = getMimeTypeFromUri(requireContext(), it.file.toUri()),
                            notificationUtils = notificationUtils
                    )
                }
                is RoomUploadsViewEvents.Failure             -> showFailure(it.throwable)
            }.exhaustive
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        renderRoomSummary(state)
    }

    private fun renderRoomSummary(state: RoomUploadsViewState) {
        state.roomSummary()?.let {
            roomUploadsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), roomUploadsToolbarAvatarImageView)
        }
    }
}
