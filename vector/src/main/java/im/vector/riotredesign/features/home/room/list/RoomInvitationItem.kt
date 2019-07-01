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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyHolder
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.core.extensions.setTextOrHide
import im.vector.riotredesign.core.platform.ButtonStateView
import im.vector.riotredesign.features.home.AvatarRenderer


@EpoxyModelClass(layout = R.layout.item_room_invitation)
abstract class RoomInvitationItem : VectorEpoxyModel<RoomInvitationItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var roomName: CharSequence
    @EpoxyAttribute lateinit var roomId: String
    @EpoxyAttribute var secondLine: CharSequence? = null
    @EpoxyAttribute var avatarUrl: String? = null
    @EpoxyAttribute var listener: (() -> Unit)? = null
    @EpoxyAttribute var invitationAcceptInProgress: Boolean = false
    @EpoxyAttribute var invitationAcceptInError: Boolean = false
    @EpoxyAttribute var invitationRejectInProgress: Boolean = false
    @EpoxyAttribute var invitationRejectInError: Boolean = false
    @EpoxyAttribute var acceptListener: (() -> Unit)? = null
    @EpoxyAttribute var rejectListener: (() -> Unit)? = null


    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.setOnClickListener { listener?.invoke() }

        // When a request is in progress (accept or reject), we only use the accept State button
        val requestInProgress = invitationAcceptInProgress || invitationRejectInProgress

        when {
            requestInProgress       -> holder.acceptView.render(ButtonStateView.State.Loading)
            invitationAcceptInError -> holder.acceptView.render(ButtonStateView.State.Error)
            else                    -> holder.acceptView.render(ButtonStateView.State.Button)
        }
        // ButtonStateView.State.Loaded not used because roomSummary will not be displayed as a room invitation anymore


        holder.acceptView.callback = object : ButtonStateView.Callback {
            override fun onButtonClicked() {
                acceptListener?.invoke()
            }

            override fun onRetryClicked() {
                acceptListener?.invoke()
            }
        }

        holder.rejectView.isVisible = !requestInProgress

        when {
            invitationRejectInError -> holder.rejectView.render(ButtonStateView.State.Error)
            else                    -> holder.rejectView.render(ButtonStateView.State.Button)
        }

        holder.rejectView.callback = object : ButtonStateView.Callback {
            override fun onButtonClicked() {
                rejectListener?.invoke()
            }

            override fun onRetryClicked() {
                rejectListener?.invoke()
            }
        }
        holder.titleView.text = roomName
        holder.subtitleView.setTextOrHide(secondLine)
        avatarRenderer.render(avatarUrl, roomId, roomName.toString(), holder.avatarImageView)
    }

    class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.roomInvitationNameView)
        val subtitleView by bind<TextView>(R.id.roomInvitationSubTitle)
        val acceptView by bind<ButtonStateView>(R.id.roomInvitationAccept)
        val rejectView by bind<ButtonStateView>(R.id.roomInvitationReject)
        val avatarImageView by bind<ImageView>(R.id.roomInvitationAvatarImageView)
        val rootView by bind<ViewGroup>(R.id.itemRoomInvitationLayout)
    }

}