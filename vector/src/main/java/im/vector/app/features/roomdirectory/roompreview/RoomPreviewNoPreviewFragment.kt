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
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
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
import org.matrix.android.sdk.api.util.MatrixItem
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
            navigator.openRoom(requireActivity(), state.roomId, roomPreviewData.eventId, roomPreviewData.buildTask)
        }

        val bestName = state.roomName ?: state.roomAlias ?: state.roomId
        when (state.peekingState) {
            is Loading -> {
                roomPreviewPeekingProgress.isVisible = true
                roomPreviewNoPreviewJoin.isVisible = false
            }
            is Success -> {
                roomPreviewPeekingProgress.isVisible = false
                when (state.peekingState.invoke()) {
                    PeekingState.FOUND     -> {
                        // show join buttons
                        roomPreviewNoPreviewJoin.isVisible = true
                        renderState(bestName, state.matrixItem(), state.roomTopic)
                    }
                    PeekingState.NO_ACCESS -> {
                        roomPreviewNoPreviewJoin.isVisible = true
                        roomPreviewNoPreviewLabel.isVisible = true
                        roomPreviewNoPreviewLabel.setText(R.string.room_preview_no_preview_join)
                        renderState(bestName, state.matrixItem().takeIf { state.roomAlias != null }, state.roomTopic)
                    }
                    else                   -> {
                        roomPreviewNoPreviewJoin.isVisible = false
                        roomPreviewNoPreviewLabel.isVisible = true
                        roomPreviewNoPreviewLabel.setText(R.string.room_preview_not_found)
                        renderState(bestName, null, state.roomTopic)
                    }
                }
            }
            else       -> {
                // Render with initial state, no peeking
                roomPreviewPeekingProgress.isVisible = false
                roomPreviewNoPreviewJoin.isVisible = true
                renderState(bestName, state.matrixItem(), state.roomTopic)
                roomPreviewNoPreviewLabel.isVisible = false
            }
        }
    }

    private fun renderState(roomName: String, matrixItem: MatrixItem?, topic: String?) {
        // Toolbar
        if (matrixItem != null) {
            roomPreviewNoPreviewToolbarAvatar.isVisible = true
            roomPreviewNoPreviewAvatar.isVisible = true
            avatarRenderer.render(matrixItem, roomPreviewNoPreviewToolbarAvatar)
            avatarRenderer.render(matrixItem, roomPreviewNoPreviewAvatar)
        } else {
            roomPreviewNoPreviewToolbarAvatar.isVisible = false
            roomPreviewNoPreviewAvatar.isVisible = false
        }
        roomPreviewNoPreviewToolbarTitle.text = roomName

        // Screen
        roomPreviewNoPreviewName.text = roomName
        roomPreviewNoPreviewTopic.setTextOrHide(topic)
    }
}
