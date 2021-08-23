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

package im.vector.app.features.spaces.preview

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.appcompat.navigationClicks
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSpacePreviewBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.spaces.SpacePreviewSharedAction
import im.vector.app.features.spaces.SpacePreviewSharedActionViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.util.MatrixItem
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Parcelize
data class SpacePreviewArgs(
        val idOrAlias: String
) : Parcelable

class SpacePreviewFragment @Inject constructor(
        private val viewModelFactory: SpacePreviewViewModel.Factory,
        private val avatarRenderer: AvatarRenderer,
        private val epoxyController: SpacePreviewController
) : VectorBaseFragment<FragmentSpacePreviewBinding>(), SpacePreviewViewModel.Factory {

    private val viewModel by fragmentViewModel(SpacePreviewViewModel::class)
    lateinit var sharedActionViewModel: SpacePreviewSharedActionViewModel

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSpacePreviewBinding {
        return FragmentSpacePreviewBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(SpacePreviewSharedActionViewModel::class.java)
    }

    override fun create(initialState: SpacePreviewState) = viewModelFactory.create(initialState)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.observeViewEvents {
            handleViewEvents(it)
        }

        views.roomPreviewNoPreviewToolbar.navigationClicks()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { sharedActionViewModel.post(SpacePreviewSharedAction.DismissAction) }
                .disposeOnDestroyView()

        views.spacePreviewRecyclerView.configureWith(epoxyController)

        views.spacePreviewAcceptInviteButton.debouncedClicks {
            viewModel.handle(SpacePreviewViewAction.AcceptInvite)
        }

        views.spacePreviewDeclineInviteButton.debouncedClicks {
            viewModel.handle(SpacePreviewViewAction.DismissInvite)
        }
    }

    override fun onDestroyView() {
        views.spacePreviewRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) {
        when (it.spaceInfo) {
            is Uninitialized,
            is Loading -> {
                views.spacePreviewPeekingProgress.isVisible = true
                views.spacePreviewButtonBar.isVisible = true
                views.spacePreviewAcceptInviteButton.isEnabled = false
                views.spacePreviewDeclineInviteButton.isEnabled = false
            }
            is Fail -> {
                views.spacePreviewPeekingProgress.isVisible = false
                views.spacePreviewButtonBar.isVisible = false
            }
            is Success -> {
                views.spacePreviewPeekingProgress.isVisible = false
                views.spacePreviewButtonBar.isVisible = true
                views.spacePreviewAcceptInviteButton.isEnabled = true
                views.spacePreviewDeclineInviteButton.isEnabled = true
                epoxyController.setData(it)
            }
        }
        updateToolbar(it)

        when (it.inviteTermination) {
            is Loading -> sharedActionViewModel.post(SpacePreviewSharedAction.ShowModalLoading)
            else       -> sharedActionViewModel.post(SpacePreviewSharedAction.HideModalLoading)
        }
    }

    private fun handleViewEvents(viewEvents: SpacePreviewViewEvents) {
        when (viewEvents) {
            SpacePreviewViewEvents.Dismiss -> {
                sharedActionViewModel.post(SpacePreviewSharedAction.DismissAction)
            }
            SpacePreviewViewEvents.JoinSuccess -> {
                sharedActionViewModel.post(SpacePreviewSharedAction.HideModalLoading)
                sharedActionViewModel.post(SpacePreviewSharedAction.DismissAction)
            }
            is SpacePreviewViewEvents.JoinFailure -> {
                sharedActionViewModel.post(SpacePreviewSharedAction.HideModalLoading)
                sharedActionViewModel.post(SpacePreviewSharedAction.ShowErrorMessage(viewEvents.message ?: getString(R.string.matrix_error)))
            }
        }
    }

    private fun updateToolbar(spacePreviewState: SpacePreviewState) {
//        when (val preview = spacePreviewState.peekResult.invoke()) {
//            is SpacePeekResult.Success -> {
//                val roomPeekResult = preview.summary.roomPeekResult
        val spaceName = spacePreviewState.spaceInfo.invoke()?.name ?: spacePreviewState.name ?: ""
        val spaceAvatarUrl = spacePreviewState.spaceInfo.invoke()?.avatarUrl ?: spacePreviewState.avatarUrl
        val mxItem = MatrixItem.SpaceItem(spacePreviewState.idOrAlias, spaceName, spaceAvatarUrl)
        avatarRenderer.render(mxItem, views.spacePreviewToolbarAvatar)
        views.roomPreviewNoPreviewToolbarTitle.text = spaceName
//            }
//            is SpacePeekResult.SpacePeekError,
//            null -> {
//                // what to do here?
//                val mxItem = MatrixItem.RoomItem(spacePreviewState.idOrAlias, spacePreviewState.name, spacePreviewState.avatarUrl)
//                avatarRenderer.renderSpace(mxItem, views.spacePreviewToolbarAvatar)
//                views.roomPreviewNoPreviewToolbarTitle.text = spacePreviewState.name
//            }
//        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.handle(SpacePreviewViewAction.ViewReady)
    }
}
