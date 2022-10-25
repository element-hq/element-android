/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.list.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.FragmentNewChatBottomSheetBinding
import im.vector.app.features.navigation.Navigator
import javax.inject.Inject

@AndroidEntryPoint
class NewChatBottomSheet : VectorBaseBottomSheetDialogFragment<FragmentNewChatBottomSheetBinding>() {

    @Inject lateinit var navigator: Navigator

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentNewChatBottomSheetBinding {
        return FragmentNewChatBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initFABs()
    }

    private fun initFABs() {
        views.startChat.debouncedClicks {
            dismiss()
            navigator.openCreateDirectRoom(requireActivity())
        }

        views.createRoom.debouncedClicks {
            dismiss()
            navigator.openCreateRoom(requireActivity())
        }

        views.exploreRooms.debouncedClicks {
            dismiss()
            navigator.openRoomDirectory(requireContext())
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setPeekHeightAsScreenPercentage(0.5f)
        }
    }

    companion object {
        const val TAG = "NewChatBottomSheet"
    }
}
