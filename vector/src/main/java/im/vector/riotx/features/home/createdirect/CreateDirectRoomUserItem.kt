/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotx.features.home.createdirect

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.amulyakhare.textdrawable.TextDrawable
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.getColorFromUserId

@EpoxyModelClass(layout = R.layout.item_create_direct_room_user)
abstract class CreateDirectRoomUserItem : VectorEpoxyModel<CreateDirectRoomUserItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute var name: String? = null
    @EpoxyAttribute var userId: String = ""
    @EpoxyAttribute var avatarUrl: String? = null
    @EpoxyAttribute var clickListener: View.OnClickListener? = null
    @EpoxyAttribute var selected: Boolean = false

    override fun bind(holder: Holder) {
        holder.view.setOnClickListener(clickListener)
        // If name is empty, use userId as name and force it being centered
        if (name.isNullOrEmpty()) {
            holder.userIdView.visibility = View.GONE
            holder.nameView.text = userId
        } else {
            holder.userIdView.visibility = View.VISIBLE
            holder.nameView.text = name
            holder.userIdView.text = userId
        }
        if (selected) {
            holder.avatarCheckedImageView.visibility = View.VISIBLE
            val backgroundColor = ContextCompat.getColor(holder.view.context, R.color.riotx_accent)
            val backgroundDrawable = TextDrawable.builder().buildRound("", backgroundColor)
            holder.avatarImageView.setImageDrawable(backgroundDrawable)
        } else {
            holder.avatarCheckedImageView.visibility = View.GONE
            avatarRenderer.render(avatarUrl, userId, name, holder.avatarImageView)
        }
    }

    class Holder : VectorEpoxyHolder() {
        val userIdView by bind<TextView>(R.id.createDirectRoomUserID)
        val nameView by bind<TextView>(R.id.createDirectRoomUserName)
        val avatarImageView by bind<ImageView>(R.id.createDirectRoomUserAvatar)
        val avatarCheckedImageView by bind<ImageView>(R.id.createDirectRoomUserAvatarChecked)
    }

}