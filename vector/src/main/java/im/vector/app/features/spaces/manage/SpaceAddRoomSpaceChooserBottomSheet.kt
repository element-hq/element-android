/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetAddRoomsOrSpacesToSpaceBinding

class SpaceAddRoomSpaceChooserBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetAddRoomsOrSpacesToSpaceBinding>() {

    override val showExpanded = true

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            BottomSheetAddRoomsOrSpacesToSpaceBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.createRooms.views.bottomSheetActionClickableZone.debouncedClicks {
            setFragmentResult(REQUEST_KEY, Bundle().apply {
                putString(BUNDLE_KEY_ACTION, ACTION_CREATE_ROOM)
            })
            dismiss()
        }

        views.addSpaces.views.bottomSheetActionClickableZone.debouncedClicks {
            setFragmentResult(REQUEST_KEY, Bundle().apply {
                putString(BUNDLE_KEY_ACTION, ACTION_ADD_SPACES)
            })
            dismiss()
        }

        views.addRooms.views.bottomSheetActionClickableZone.debouncedClicks {
            setFragmentResult(REQUEST_KEY, Bundle().apply {
                putString(BUNDLE_KEY_ACTION, ACTION_ADD_ROOMS)
            })
            dismiss()
        }
    }

    companion object {

        const val REQUEST_KEY = "SpaceAddRoomSpaceChooserBottomSheet"
        const val BUNDLE_KEY_ACTION = "SpaceAddRoomSpaceChooserBottomSheet.Action"
        const val ACTION_ADD_ROOMS = "Action.AddRoom"
        const val ACTION_ADD_SPACES = "Action.AddSpaces"
        const val ACTION_CREATE_ROOM = "Action.CreateRoom"

        fun newInstance(): SpaceAddRoomSpaceChooserBottomSheet {
            return SpaceAddRoomSpaceChooserBottomSheet()
        }
    }
}
