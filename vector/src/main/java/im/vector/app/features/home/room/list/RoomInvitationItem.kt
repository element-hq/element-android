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

package im.vector.app.features.home.room.list

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.ButtonStateView
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.invite.InviteButtonStateBinder
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_room_invitation)
abstract class RoomInvitationItem : VectorEpoxyModel<RoomInvitationItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute var secondLine: String? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var listener: ClickListener? = null
    @EpoxyAttribute lateinit var changeMembershipState: ChangeMembershipState
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var acceptListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var rejectListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.onClick(listener)
        holder.acceptView.commonClicked = acceptListener
        holder.rejectView.commonClicked = rejectListener
        InviteButtonStateBinder.bind(holder.acceptView, holder.rejectView, changeMembershipState)
        holder.titleView.text = matrixItem.getBestName()
        holder.subtitleView.setTextOrHide(secondLine)
        avatarRenderer.render(matrixItem, holder.avatarImageView)
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
