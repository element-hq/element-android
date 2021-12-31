/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.userdirectory

import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.home.AvatarRenderer

@EpoxyModelClass(layout = R.layout.item_invite_by_mail)
abstract class InviteByEmailItem : VectorEpoxyModel<InviteByEmailItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var foundItem: ThreePidUser
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var clickListener: ClickListener? = null
    @EpoxyAttribute var selected: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.itemTitleText.text = foundItem.email
        holder.checkedImageView.isVisible = false
        holder.avatarImageView.isVisible = true
        holder.view.setOnClickListener(clickListener)
        if (selected) {
            holder.checkedImageView.isVisible = true
            holder.avatarImageView.isVisible = false
        } else {
            holder.checkedImageView.isVisible = false
            holder.avatarImageView.isVisible = true
        }
    }

    class Holder : VectorEpoxyHolder() {
        val itemTitleText by bind<TextView>(R.id.itemTitle)
        val avatarImageView by bind<ImageView>(R.id.itemAvatar)
        val checkedImageView by bind<ImageView>(R.id.itemAvatarChecked)
    }
}
