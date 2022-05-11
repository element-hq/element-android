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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.R
import im.vector.app.databinding.ListItemInviteBinding
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.toMatrixItem

class InvitesAdapter(
        private val avatarRenderer: AvatarRenderer,
        private val inviteUserTask: ((String) -> String?)?,
        private val onInviteClicked: ((RoomSummary) -> Unit)?,
) : RecyclerView.Adapter<InvitesAdapter.ViewHolder>() {

    private val invites = mutableListOf<RoomSummary>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListItemInviteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(invites[position])
    }

    override fun getItemCount() = invites.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(invites: List<RoomSummary>) {
        this.invites.clear()
        this.invites.addAll(invites)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ListItemInviteBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(invite: RoomSummary) {
            avatarRenderer.render(invite.toMatrixItem(), binding.avatar)
            binding.name.text = invite.name
            binding.root.setOnClickListener { onInviteClicked?.invoke(invite) }

            invite.inviterId?.let {
                val inviterName =  inviteUserTask?.invoke(it)
                if (inviterName != null) {
                    binding.invitedBy.text = binding.root.context.getString(R.string.invited_by, inviterName)
                } else {
                    binding.invitedBy.text = ""
                }
            }
        }
    }
}
