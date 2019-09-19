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

package im.vector.riotx.features.home.room.list

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.features.home.AvatarRenderer


@EpoxyModelClass(layout = R.layout.item_room)
abstract class RoomSummaryItem : VectorEpoxyModel<RoomSummaryItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var roomName: CharSequence
    @EpoxyAttribute lateinit var roomId: String
    @EpoxyAttribute lateinit var lastFormattedEvent: CharSequence
    @EpoxyAttribute lateinit var lastEventTime: CharSequence
    @EpoxyAttribute var avatarUrl: String? = null
    @EpoxyAttribute var unreadNotificationCount: Int = 0
    @EpoxyAttribute var hasUnreadMessage: Boolean = false
    @EpoxyAttribute var showHighlighted: Boolean = false
    @EpoxyAttribute var listener: (() -> Unit)? = null


    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.setOnClickListener { listener?.invoke() }
        holder.titleView.text = roomName
        holder.lastEventTimeView.text = lastEventTime
        holder.lastEventView.text = lastFormattedEvent
        holder.unreadCounterBadgeView.render(UnreadCounterBadgeView.State(unreadNotificationCount, showHighlighted))
        holder.unreadIndentIndicator.isVisible = hasUnreadMessage
        avatarRenderer.render(avatarUrl, roomId, roomName.toString(), holder.avatarImageView)
    }

    class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.roomNameView)
        val unreadCounterBadgeView by bind<UnreadCounterBadgeView>(R.id.roomUnreadCounterBadgeView)
        val unreadIndentIndicator by bind<View>(R.id.roomUnreadIndicator)
        val lastEventView by bind<TextView>(R.id.roomLastEventView)
        val lastEventTimeView by bind<TextView>(R.id.roomLastEventTimeView)
        val avatarImageView by bind<ImageView>(R.id.roomAvatarImageView)
        val rootView by bind<ViewGroup>(R.id.itemRoomLayout)
    }

}