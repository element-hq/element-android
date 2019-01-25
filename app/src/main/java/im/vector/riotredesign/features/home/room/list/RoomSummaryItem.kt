/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.features.home.room.list

import android.widget.ImageView
import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.core.platform.CheckableFrameLayout
import im.vector.riotredesign.features.home.AvatarRenderer


data class RoomSummaryItem(
        val roomName: CharSequence,
        val avatarUrl: String?,
        val isSelected: Boolean,
        val listener: (() -> Unit)? = null
) : KotlinModel(R.layout.item_room) {

    private val titleView by bind<TextView>(R.id.roomNameView)
    private val avatarImageView by bind<ImageView>(R.id.roomAvatarImageView)
    private val rootView by bind<CheckableFrameLayout>(R.id.itemRoomLayout)

    override fun bind() {
        rootView.isChecked = isSelected
        rootView.setOnClickListener { listener?.invoke() }
        titleView.text = roomName
        AvatarRenderer.render(avatarUrl, roomName.toString(), avatarImageView)
    }
}