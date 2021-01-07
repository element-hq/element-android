/*
 * Copyright (c) 2020 New Vector Ltd
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

@EpoxyModelClass(layout = R.layout.item_space_roomchild)
abstract class RoomChildItem : VectorEpoxyModel<RoomChildItem.Holder>() {

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
