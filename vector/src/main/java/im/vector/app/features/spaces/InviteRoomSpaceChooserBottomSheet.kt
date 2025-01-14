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
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.args
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetSpaceInviteChooserBinding
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@AndroidEntryPoint
class InviteRoomSpaceChooserBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetSpaceInviteChooserBinding>() {

    @Parcelize
    data class Args(
            val spaceId: String,
            val roomId: String
    ) : Parcelable

    override val showExpanded = true

    private val inviteArgs: Args by args()

    @Inject
    lateinit var activeSessionHolder: ActiveSessionHolder

    var onItemSelected: ((roomId: String) -> Unit)? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetSpaceInviteChooserBinding {
        return BottomSheetSpaceInviteChooserBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Not going for full view model for now, as it may change

        val summary = activeSessionHolder.getSafeActiveSession()?.spaceService()?.getSpace(inviteArgs.spaceId)?.spaceSummary()

        val spaceName = summary?.name

        views.inviteToSpaceButton.isVisible = true
        views.inviteToSpaceButton.title = getString(CommonStrings.invite_to_space_with_name, spaceName)
        views.inviteToSpaceButton.subTitle = getString(CommonStrings.invite_to_space_with_name_desc, spaceName)
        views.inviteToSpaceButton.debouncedClicks {
            dismiss()
            onItemSelected?.invoke(inviteArgs.spaceId)
        }

        views.inviteToRoomOnly.isVisible = true
        views.inviteToRoomOnly.title = getString(CommonStrings.invite_just_to_this_room)
        views.inviteToRoomOnly.subTitle = getString(CommonStrings.invite_just_to_this_room_desc, spaceName)
        views.inviteToRoomOnly.debouncedClicks {
            dismiss()
            onItemSelected?.invoke(inviteArgs.roomId)
        }
    }

    companion object {
        fun showInstance(
                fragmentManager: FragmentManager,
                spaceId: String,
                roomId: String,
                onItemSelected: (roomId: String) -> Unit
        ) {
            InviteRoomSpaceChooserBottomSheet().apply {
                this.onItemSelected = onItemSelected
                setArguments(Args(spaceId, roomId))
            }.show(fragmentManager, InviteRoomSpaceChooserBottomSheet::class.java.name)
        }
    }
}
