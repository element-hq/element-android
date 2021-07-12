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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import im.vector.app.databinding.FragmentRoomPreviewNoPreviewBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomdirectory.JoinState

import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

/**
 * Note: this Fragment is also used for world readable room for the moment
 */
class RoomPreviewNoPreviewFragment @Inject constructor(
        val roomPreviewViewModelFactory: RoomPreviewViewModel.Factory,
        private val avatarRenderer: AvatarRenderer
) : VectorBaseFragment<FragmentRoomPreviewNoPreviewBinding>() {

    private val roomPreviewViewModel: RoomPreviewViewModel by fragmentViewModel()
    private val roomPreviewData: RoomPreviewData by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomPreviewNoPreviewBinding {
        return FragmentRoomPreviewNoPreviewBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.roomPreviewNoPreviewToolbar)

        views.roomPreviewNoPreviewJoin.commonClicked = { roomPreviewViewModel.handle(RoomPreviewAction.Join) }
    }

    override fun invalidate() = withState(roomPreviewViewModel) { state ->
        TransitionManager.beginDelayedTransition(views.coordinatorLayout)

        views.roomPreviewNoPreviewJoin.render(
                when (state.roomJoinState) {
                    JoinState.NOT_JOINED    -> ButtonStateView.State.Button
                    JoinState.JOINING       -> ButtonStateView.State.Loading
                    JoinState.JOINED        -> ButtonStateView.State.Loaded
                    JoinState.JOINING_ERROR -> ButtonStateView.State.Error
                }
        )

        if (state.lastError == null) {
            views.roomPreviewNoPreviewError.isVisible = false
        } else {
            views.roomPreviewNoPreviewError.isVisible = true
            views.roomPreviewNoPreviewError.text = errorFormatter.toHumanReadable(state.lastError)
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
                views.roomPreviewPeekingProgress.isVisible = true
                views.roomPreviewNoPreviewJoin.isVisible = false
            }
            is Success -> {
                views.roomPreviewPeekingProgress.isVisible = false
                when (state.peekingState.invoke()) {
                    PeekingState.FOUND     -> {
                        // show join buttons
                        views.roomPreviewNoPreviewJoin.isVisible = true
                        renderState(bestName, state.matrixItem(), state.roomTopic)
                    }
                    PeekingState.NO_ACCESS -> {
                        views.roomPreviewNoPreviewJoin.isVisible = true
                        views.roomPreviewNoPreviewLabel.isVisible = true
                        views.roomPreviewNoPreviewLabel.setText(R.string.room_preview_no_preview_join)
                        renderState(bestName, state.matrixItem().takeIf { state.roomAlias != null }, state.roomTopic)
                    }
                    else                   -> {
                        views.roomPreviewNoPreviewJoin.isVisible = false
                        views.roomPreviewNoPreviewLabel.isVisible = true
                        views.roomPreviewNoPreviewLabel.setText(R.string.room_preview_not_found)
                        renderState(bestName, null, state.roomTopic)
                    }
                }
            }
            else       -> {
                // Render with initial state, no peeking
                views.roomPreviewPeekingProgress.isVisible = false
                views.roomPreviewNoPreviewJoin.isVisible = true
                renderState(bestName, state.matrixItem(), state.roomTopic)
                views.roomPreviewNoPreviewLabel.isVisible = false
            }
        }
    }

    private fun renderState(roomName: String, matrixItem: MatrixItem?, topic: String?) {
        // Toolbar
        if (matrixItem != null) {
            views.roomPreviewNoPreviewToolbarAvatar.isVisible = true
            views.roomPreviewNoPreviewAvatar.isVisible = true
            avatarRenderer.render(matrixItem, views.roomPreviewNoPreviewToolbarAvatar)
            avatarRenderer.render(matrixItem, views.roomPreviewNoPreviewAvatar)
        } else {
            views.roomPreviewNoPreviewToolbarAvatar.isVisible = false
            views.roomPreviewNoPreviewAvatar.isVisible = false
        }
        views.roomPreviewNoPreviewToolbarTitle.text = roomName

        // Screen
        views.roomPreviewNoPreviewName.text = roomName
        views.roomPreviewNoPreviewTopic.setTextOrHide(topic)
    }
}
