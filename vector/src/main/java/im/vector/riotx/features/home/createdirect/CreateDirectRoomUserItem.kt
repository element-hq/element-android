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
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.features.home.AvatarRenderer

@EpoxyModelClass(layout = R.layout.item_create_direct_room_user)
abstract class CreateDirectRoomUserItem : VectorEpoxyModel<CreateDirectRoomUserItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute var showLetter: Boolean = false
    @EpoxyAttribute var firstLetter: String = ""
    @EpoxyAttribute var name: String? = null
    @EpoxyAttribute var userId: String = ""
    @EpoxyAttribute var avatarUrl: String? = null
    @EpoxyAttribute var clickListener: View.OnClickListener? = null

    override fun bind(holder: Holder) {
        holder.view.setOnClickListener(clickListener)
        holder.nameView.text = name
        holder.letterView.visibility = if (showLetter) View.VISIBLE else View.INVISIBLE
        holder.letterView.text = firstLetter
        avatarRenderer.render(avatarUrl, userId, name, holder.avatarImageView)
    }

    class Holder : VectorEpoxyHolder() {
        val letterView by bind<TextView>(R.id.createDirectRoomUserLetter)
        val nameView by bind<TextView>(R.id.createDirectRoomUserName)
        val avatarImageView by bind<ImageView>(R.id.createDirectRoomUserAvatar)
    }

}