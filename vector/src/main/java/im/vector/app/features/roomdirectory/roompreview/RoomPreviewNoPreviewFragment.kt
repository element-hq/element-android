/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.roomdirectory.roompreview

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.ButtonStateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomdirectory.JoinState
import kotlinx.android.synthetic.main.fragment_room_preview_no_preview.*
import javax.inject.Inject

/**
 * Note: this Fragment is also used for world readable room for the moment
 */
class RoomPreviewNoPreviewFragment @Inject constructor(
        val roomPreviewViewModelFactory: RoomPreviewViewModel.Factory,
        private val avatarRenderer: AvatarRenderer
) : VectorBaseFragment() {

    private val roomPreviewViewModel: RoomPreviewViewModel by fragmentViewModel()
    private val roomPreviewData: RoomPreviewData by args()

    override fun getLayoutResId() = R.layout.fragment_room_preview_no_preview

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(roomPreviewNoPreviewToolbar)
        // Toolbar
        avatarRenderer.render(roomPreviewData.matrixItem, roomPreviewNoPreviewToolbarAvatar)
        roomPreviewNoPreviewToolbarTitle.text = roomPreviewData.roomName ?: roomPreviewData.roomAlias

        // Screen
        avatarRenderer.render(roomPreviewData.matrixItem, roomPreviewNoPreviewAvatar)
        roomPreviewNoPreviewName.text = roomPreviewData.roomName ?: roomPreviewData.roomAlias
        roomPreviewNoPreviewTopic.setTextOrHide(roomPreviewData.topic)

        if (roomPreviewData.worldReadable) {
            roomPreviewNoPreviewLabel.setText(R.string.room_preview_world_readable_room_not_supported_yet)
        } else {
            roomPreviewNoPreviewLabel.setText(R.string.room_preview_no_preview)
        }

        roomPreviewNoPreviewJoin.callback = object : ButtonStateView.Callback {
            override fun onButtonClicked() {
                roomPreviewViewModel.handle(RoomPreviewAction.Join)
            }

            override fun onRetryClicked() {
                // Same action
                onButtonClicked()
            }
        }
    }

    override fun invalidate() = withState(roomPreviewViewModel) { state ->
        TransitionManager.beginDelayedTransition(roomPreviewNoPreviewRoot)

        roomPreviewNoPreviewJoin.render(
                when (state.roomJoinState) {
                    JoinState.NOT_JOINED    -> ButtonStateView.State.Button
                    JoinState.JOINING       -> ButtonStateView.State.Loading
                    JoinState.JOINED        -> ButtonStateView.State.Loaded
                    JoinState.JOINING_ERROR -> ButtonStateView.State.Error
                }
        )

        if (state.lastError == null) {
            roomPreviewNoPreviewError.isVisible = false
        } else {
            roomPreviewNoPreviewError.isVisible = true
            roomPreviewNoPreviewError.text = errorFormatter.toHumanReadable(state.lastError)
        }

        if (state.roomJoinState == JoinState.JOINED) {
            // Quit this screen
            requireActivity().finish()
            // Open room
            navigator.openRoom(requireActivity(), roomPreviewData.roomId)
        }
    }
}
