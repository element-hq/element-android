/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.preview

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSpacePreviewBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.spaces.SpacePreviewSharedAction
import im.vector.app.features.spaces.SpacePreviewSharedActionViewModel
import im.vector.lib.core.utils.flow.throttleFirst
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.util.MatrixItem
import reactivecircus.flowbinding.appcompat.navigationClicks
import javax.inject.Inject

@Parcelize
data class SpacePreviewArgs(
        val idOrAlias: String
) : Parcelable

@AndroidEntryPoint
class SpacePreviewFragment :
        VectorBaseFragment<FragmentSpacePreviewBinding>() {

    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var epoxyController: SpacePreviewController

    private val viewModel by fragmentViewModel(SpacePreviewViewModel::class)
    lateinit var sharedActionViewModel: SpacePreviewSharedActionViewModel

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSpacePreviewBinding {
        return FragmentSpacePreviewBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(SpacePreviewSharedActionViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.observeViewEvents {
            handleViewEvents(it)
        }

        views.roomPreviewNoPreviewToolbar
                .navigationClicks()
                .throttleFirst(300)
                .onEach { sharedActionViewModel.post(SpacePreviewSharedAction.DismissAction) }
                .launchIn(viewLifecycleOwner.lifecycleScope)

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
            else -> sharedActionViewModel.post(SpacePreviewSharedAction.HideModalLoading)
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
                sharedActionViewModel.post(SpacePreviewSharedAction.ShowErrorMessage(viewEvents.message ?: getString(CommonStrings.matrix_error)))
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
