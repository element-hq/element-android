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

package im.vector.app.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetInvitesBinding
import org.matrix.android.sdk.api.session.room.model.RoomSummary

class InvitesBottomSheet(
        private val invites: List<RoomSummary>,
        private val avatarRenderer: AvatarRenderer,
        private val inviteUserTask: ((String) -> String?)?,
        private val onInviteClicked: ((RoomSummary) -> Unit)?
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetInvitesBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetInvitesBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInvitesList()
    }

    private fun setupInvitesList() {
        val adapter = InvitesAdapter(avatarRenderer, inviteUserTask) {
            dismiss()
            onInviteClicked?.invoke(it)
        }
        val layoutManager = LinearLayoutManager(context)
        binding.invitesList.adapter = adapter
        binding.invitesList.layoutManager = layoutManager
        adapter.updateList(invites)
    }

    companion object {
        const val TAG = "InvitesBottomSheet"
    }
}
