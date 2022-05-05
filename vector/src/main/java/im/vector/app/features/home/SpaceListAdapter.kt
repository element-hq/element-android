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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.databinding.ItemModalSpaceBinding
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.toMatrixItem

class SpaceListAdapter(
        private val spaces: MutableList<RoomSummary>,
        private val avatarRenderer: AvatarRenderer,
) : RecyclerView.Adapter<SpaceListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = ItemModalSpaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(spaces[position])
    }

    override fun getItemCount() = spaces.size

    fun replaceList(spaces: List<RoomSummary>) {
        this.spaces.clear()
        this.spaces.addAll(spaces)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemModalSpaceBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(space: RoomSummary) {
            avatarRenderer.render(space.toMatrixItem(), binding.avatar)
            binding.name.text = space.name
        }
    }
}
