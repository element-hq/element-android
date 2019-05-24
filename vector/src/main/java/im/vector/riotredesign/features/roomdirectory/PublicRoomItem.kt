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

package im.vector.riotredesign.features.roomdirectory

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyHolder
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.features.home.AvatarRenderer

@EpoxyModelClass(layout = R.layout.item_public_room)
abstract class PublicRoomItem : VectorEpoxyModel<PublicRoomItem.Holder>() {

    enum class JoinState {
        NOT_JOINED,
        JOINING,
        JOINING_ERROR,
        JOINED
    }

    @EpoxyAttribute
    var avatarUrl: String? = null

    @EpoxyAttribute
    var roomId: String? = null

    @EpoxyAttribute
    var roomName: String? = null

    @EpoxyAttribute
    var nbOfMembers: Int = 0

    @EpoxyAttribute
    var joinState: JoinState = JoinState.NOT_JOINED

    @EpoxyAttribute
    var globalListener: (() -> Unit)? = null

    @EpoxyAttribute
    var joinListener: (() -> Unit)? = null

    override fun bind(holder: Holder) {
        holder.rootView.setOnClickListener { globalListener?.invoke() }

        AvatarRenderer.render(avatarUrl, roomId!!, roomName, holder.avatarView)
        holder.nameView.text = roomName
        // TODO Use formatter for big numbers?
        holder.counterView.text = nbOfMembers.toString()

        if (joinState == JoinState.NOT_JOINED) {
            holder.joinButton.isVisible = true
        } else {
            // We use isInvisible because we want to keep button space in the layout
            holder.joinButton.isInvisible = true
        }
        holder.joiningView.isVisible = joinState == JoinState.JOINING
        holder.retryButton.isVisible = joinState == JoinState.JOINING_ERROR
        holder.joinedView.isVisible = joinState == JoinState.JOINED

        holder.joinButton.setOnClickListener { joinListener?.invoke() }
        holder.retryButton.setOnClickListener { joinListener?.invoke() }
    }


    class Holder : VectorEpoxyHolder() {
        val rootView by bind<ViewGroup>(R.id.itemPublicRoomLayout)

        val avatarView by bind<ImageView>(R.id.itemPublicRoomAvatar)
        val nameView by bind<TextView>(R.id.itemPublicRoomName)
        val counterView by bind<TextView>(R.id.itemPublicRoomMembersCount)

        val joinedView by bind<View>(R.id.itemPublicRoomJoined)
        val joinButton by bind<View>(R.id.itemPublicRoomJoin)
        val joiningView by bind<View>(R.id.itemPublicRoomJoining)
        val retryButton by bind<View>(R.id.itemPublicRoomRetry)
    }

}

