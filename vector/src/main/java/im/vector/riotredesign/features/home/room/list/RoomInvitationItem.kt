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

import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyHolder
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.core.extensions.setTextOrHide
import im.vector.riotredesign.features.home.AvatarRenderer


@EpoxyModelClass(layout = R.layout.item_room_invitation)
abstract class RoomInvitationItem : VectorEpoxyModel<RoomInvitationItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var roomName: CharSequence
    @EpoxyAttribute lateinit var roomId: String
    @EpoxyAttribute var secondLine: CharSequence? = null
    @EpoxyAttribute var avatarUrl: String? = null
    @EpoxyAttribute var listener: (() -> Unit)? = null
    @EpoxyAttribute var acceptListener: (() -> Unit)? = null
    @EpoxyAttribute var rejectListener: (() -> Unit)? = null


    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.setOnClickListener { listener?.invoke() }
        holder.acceptView.setOnClickListener { acceptListener?.invoke() }
        holder.rejectView.setOnClickListener { rejectListener?.invoke() }
        holder.titleView.text = roomName
        holder.subtitleView.setTextOrHide(secondLine)
        avatarRenderer.render(avatarUrl, roomId, roomName.toString(), holder.avatarImageView)
    }

    class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.roomInvitationNameView)
        val subtitleView by bind<TextView>(R.id.roomInvitationSubTitle)
        val acceptView by bind<Button>(R.id.roomInvitationAccept)
        val rejectView by bind<Button>(R.id.roomInvitationReject)
        val avatarImageView by bind<ImageView>(R.id.roomInvitationAvatarImageView)
        val rootView by bind<ViewGroup>(R.id.itemRoomInvitationLayout)
    }

}