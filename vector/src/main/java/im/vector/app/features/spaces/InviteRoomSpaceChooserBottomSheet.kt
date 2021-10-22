/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.spaces

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetSpaceInviteChooserBinding
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

    interface InteractionListener {
        fun inviteToSpace(spaceId: String)
        fun inviteToRoom(roomId: String)
    }

    var interactionListener: InteractionListener? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetSpaceInviteChooserBinding {
        return BottomSheetSpaceInviteChooserBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Not going for full view model for now, as it may change

        val summary = activeSessionHolder.getSafeActiveSession()?.spaceService()?.getSpace(inviteArgs.spaceId)?.spaceSummary()

        val spaceName = summary?.name

        views.inviteToSpaceButton.isVisible = true
        views.inviteToSpaceButton.title = getString(R.string.invite_to_space_with_name, spaceName)
        views.inviteToSpaceButton.subTitle = getString(R.string.invite_to_space_with_name_desc, spaceName)
        views.inviteToSpaceButton.debouncedClicks {
            dismiss()
            interactionListener?.inviteToSpace(inviteArgs.spaceId)
        }

        views.inviteToRoomOnly.isVisible = true
        views.inviteToRoomOnly.title = getString(R.string.invite_just_to_this_room)
        views.inviteToRoomOnly.subTitle = getString(R.string.invite_just_to_this_room_desc, spaceName)
        views.inviteToRoomOnly.debouncedClicks {
            dismiss()
            interactionListener?.inviteToRoom(inviteArgs.roomId)
        }
    }

    companion object {

        fun newInstance(spaceId: String, roomId: String, interactionListener: InteractionListener): InviteRoomSpaceChooserBottomSheet {
            return InviteRoomSpaceChooserBottomSheet().apply {
                this.interactionListener = interactionListener
                setArguments(Args(spaceId, roomId))
            }
        }
    }
}
