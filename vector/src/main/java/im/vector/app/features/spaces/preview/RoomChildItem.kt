/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.preview

import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class RoomChildItem : VectorEpoxyModel<RoomChildItem.Holder>(R.layout.item_space_roomchild) {

    @EpoxyAttribute
    lateinit var roomId: String

    @EpoxyAttribute
    lateinit var title: String

    @EpoxyAttribute
    var topic: String? = null

    @EpoxyAttribute
    lateinit var memberCount: String

    @EpoxyAttribute
    var avatarUrl: String? = null

    @EpoxyAttribute
    lateinit var avatarRenderer: AvatarRenderer

    @EpoxyAttribute
    var depth: Int = 0

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.roomNameText.text = title
        holder.roomTopicText.setTextOrHide(topic)
        holder.memberCountText.text = memberCount

        avatarRenderer.render(
                MatrixItem.RoomItem(roomId, title, avatarUrl),
                holder.avatarImageView
        )
        holder.tabView.tabDepth = depth
    }

    override fun unbind(holder: Holder) {
        avatarRenderer.clear(holder.avatarImageView)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val avatarImageView by bind<ImageView>(R.id.childRoomAvatar)
        val roomNameText by bind<TextView>(R.id.childRoomName)
        val roomTopicText by bind<TextView>(R.id.childRoomTopic)
        val memberCountText by bind<TextView>(R.id.spaceChildMemberCountText)
        val tabView by bind<SpaceTabView>(R.id.spaceChildTabView)
    }
}
