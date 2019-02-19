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
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.RiotEpoxyHolder
import im.vector.riotredesign.core.platform.CheckableFrameLayout
import im.vector.riotredesign.features.home.AvatarRenderer


@EpoxyModelClass(layout = R.layout.item_room)
abstract class RoomSummaryItem : EpoxyModelWithHolder<RoomSummaryItem.Holder>() {

    @EpoxyAttribute lateinit var roomName: CharSequence
    @EpoxyAttribute var avatarUrl: String? = null
    @EpoxyAttribute var selected: Boolean = false
    @EpoxyAttribute var unreadCount: Int = 0
    @EpoxyAttribute var showHighlighted: Boolean = false
    @EpoxyAttribute var listener: (() -> Unit)? = null


    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.unreadCounterBadgeView.render(unreadCount, showHighlighted)
        holder.rootView.isChecked = selected
        holder.rootView.setOnClickListener { listener?.invoke() }
        holder.titleView.text = roomName
        AvatarRenderer.render(avatarUrl, roomName.toString(), holder.avatarImageView)
    }

    class Holder : RiotEpoxyHolder() {
        val unreadCounterBadgeView by bind<UnreadCounterBadgeView>(R.id.roomUnreadCounterBadgeView)
        val titleView by bind<TextView>(R.id.roomNameView)
        val avatarImageView by bind<ImageView>(R.id.roomAvatarImageView)
        val rootView by bind<CheckableFrameLayout>(R.id.itemRoomLayout)
    }

}