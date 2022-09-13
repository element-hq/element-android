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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.databinding.FragmentNewChatBottomSheetBinding
import im.vector.app.features.navigation.Navigator
import javax.inject.Inject

@AndroidEntryPoint
class NewChatBottomSheet @Inject constructor() : BottomSheetDialogFragment() {

    @Inject lateinit var navigator: Navigator

    private lateinit var binding: FragmentNewChatBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNewChatBottomSheetBinding.inflate(inflater, container, false)
        initFABs()
        return binding.root
    }

    private fun initFABs() {
        binding.startChat.setOnClickListener {
            navigator.openCreateDirectRoom(requireActivity())
        }

        binding.createRoom.setOnClickListener {
            navigator.openCreateRoom(requireActivity())
        }

        binding.exploreRooms.setOnClickListener {
            navigator.openRoomDirectory(requireContext())
        }
    }

    companion object {
        const val TAG = "NewChatBottomSheet"
    }
}
