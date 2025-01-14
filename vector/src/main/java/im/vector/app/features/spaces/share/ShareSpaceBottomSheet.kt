/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.share

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.BottomSheetSpaceInviteBinding
import im.vector.app.features.invite.InviteUsersToRoomActivity
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class ShareSpaceBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetSpaceInviteBinding>() {

    @Parcelize
    data class Args(
            val spaceId: String,
            val postCreation: Boolean = false
    ) : Parcelable

    override val showExpanded = true

    private val viewModel: ShareSpaceViewModel by fragmentViewModel(ShareSpaceViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetSpaceInviteBinding {
        return BottomSheetSpaceInviteBinding.inflate(inflater, container, false)
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        val summary = state.spaceSummary.invoke()

        val spaceName = summary?.name

        if (state.postCreation) {
            views.headerText.text = getString(CommonStrings.invite_people_to_your_space)
            views.descriptionText.setTextOrHide(getString(CommonStrings.invite_people_to_your_space_desc, spaceName))
        } else {
            views.headerText.text = getString(CommonStrings.invite_to_space, spaceName)
            views.descriptionText.setTextOrHide(null)
        }

        views.inviteByLinkButton.isVisible = state.canShareLink
        views.inviteByMxidButton.isVisible = state.canInviteByMxId
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.inviteByMxidButton.debouncedClicks {
            viewModel.handle(ShareSpaceAction.InviteByMxId)
        }

        views.inviteByLinkButton.debouncedClicks {
            viewModel.handle(ShareSpaceAction.InviteByLink)
        }

        viewModel.observeViewEvents { event ->
            when (event) {
                is ShareSpaceViewEvents.NavigateToInviteUser -> {
                    val intent = InviteUsersToRoomActivity.getIntent(requireContext(), event.spaceId)
                    startActivity(intent)
                    dismissAllowingStateLoss()
                }
                is ShareSpaceViewEvents.ShowInviteByLink -> {
                    startSharePlainTextIntent(
                            context = requireContext(),
                            activityResultLauncher = null,
                            chooserTitle = getString(CommonStrings.share_by_text),
                            text = getString(CommonStrings.share_space_link_message, event.spaceName, event.permalink),
                            extraTitle = getString(CommonStrings.share_space_link_message, event.spaceName, event.permalink)
                    )
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    companion object {

        fun show(fragmentManager: FragmentManager, spaceId: String, postCreation: Boolean = false): ShareSpaceBottomSheet {
            return ShareSpaceBottomSheet().apply {
                setArguments(Args(spaceId = spaceId, postCreation = postCreation))
            }.also {
                it.show(fragmentManager, ShareSpaceBottomSheet::class.java.name)
            }
        }
    }
}
