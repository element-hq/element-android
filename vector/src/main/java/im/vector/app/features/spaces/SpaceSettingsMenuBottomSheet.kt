/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetSpaceSettingsBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.roomprofile.RoomProfileActivity
import im.vector.app.features.spaces.leave.SpaceLeaveAdvancedActivity
import im.vector.app.features.spaces.manage.ManageType
import im.vector.app.features.spaces.manage.SpaceManageActivity
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

@Parcelize
data class SpaceBottomSheetSettingsArgs(
        val spaceId: String
) : Parcelable

@AndroidEntryPoint
class SpaceSettingsMenuBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetSpaceSettingsBinding>() {

    @Inject lateinit var navigator: Navigator
    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var bugReporter: BugReporter

    private val spaceArgs: SpaceBottomSheetSettingsArgs by args()

    interface InteractionListener {
        fun onShareSpaceSelected(spaceId: String)
    }

    val settingsViewModel: SpaceMenuViewModel by fragmentViewModel()

    var interactionListener: InteractionListener? = null

    override val showExpanded = true

    var isLastAdmin: Boolean = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetSpaceSettingsBinding {
        return BottomSheetSpaceSettingsBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SpaceMenu
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.invitePeople.views.bottomSheetActionClickableZone.debouncedClicks {
            dismiss()
            interactionListener?.onShareSpaceSelected(spaceArgs.spaceId)
        }

        views.showMemberList.views.bottomSheetActionClickableZone.debouncedClicks {
            navigator.openRoomProfile(requireContext(), spaceArgs.spaceId, RoomProfileActivity.EXTRA_DIRECT_ACCESS_ROOM_MEMBERS)
        }

        views.spaceSettings.views.bottomSheetActionClickableZone.debouncedClicks {
//            navigator.openRoomProfile(requireContext(), spaceArgs.spaceId)
            startActivity(SpaceManageActivity.newIntent(requireActivity(), spaceArgs.spaceId, ManageType.Settings))
        }

        views.exploreRooms.views.bottomSheetActionClickableZone.debouncedClicks {
            startActivity(SpaceExploreActivity.newIntent(requireContext(), spaceArgs.spaceId))
        }

        views.addRooms.views.bottomSheetActionClickableZone.debouncedClicks {
            dismiss()
            startActivity(SpaceManageActivity.newIntent(requireActivity(), spaceArgs.spaceId, ManageType.AddRooms))
        }

        views.addSpaces.views.bottomSheetActionClickableZone.debouncedClicks {
            dismiss()
            startActivity(SpaceManageActivity.newIntent(requireActivity(), spaceArgs.spaceId, ManageType.AddRoomsOnlySpaces))
        }

        views.leaveSpace.views.bottomSheetActionClickableZone.debouncedClicks {
            startActivity(SpaceLeaveAdvancedActivity.newIntent(requireContext(), spaceArgs.spaceId))
        }
    }

    override fun invalidate() = withState(settingsViewModel) { state ->
        super.invalidate()

        if (state.leavingState is Success) {
            dismiss()
        }

        state.spaceSummary?.toMatrixItem()?.let {
            avatarRenderer.render(it, views.spaceAvatarImageView)
        }
        views.spaceNameView.text = state.spaceSummary?.displayName
        views.spaceDescription.setTextOrHide(state.spaceSummary?.topic?.takeIf { it.isNotEmpty() })

        views.spaceSettings.isVisible = state.canEditSettings

        views.invitePeople.isVisible = state.canInvite || state.spaceSummary?.isPublic.orFalse()
        views.addRooms.isVisible = state.canAddChild
        views.addSpaces.isVisible = state.canAddChild
    }

    companion object {
        fun newInstance(spaceId: String, interactionListener: InteractionListener): SpaceSettingsMenuBottomSheet {
            return SpaceSettingsMenuBottomSheet().apply {
                this.interactionListener = interactionListener
                setArguments(SpaceBottomSheetSettingsArgs(spaceId))
            }
        }
    }
}
