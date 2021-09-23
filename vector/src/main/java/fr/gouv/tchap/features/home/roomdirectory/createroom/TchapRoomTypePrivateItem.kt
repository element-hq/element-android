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
package fr.gouv.tchap.features.home.roomdirectory.createroom

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.onClick

@EpoxyModelClass(layout = R.layout.item_tchap_room_type_private)
abstract class TchapRoomTypePrivateItem : EpoxyModelWithHolder<TchapRoomTypePrivateItem.Holder>() {

    @EpoxyAttribute
    var selected: Boolean = false

    @EpoxyAttribute
    var clickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.containerView.onClick(clickListener)
        holder.containerView.isSelected = selected
        holder.containerView.setBackgroundResource(R.drawable.bg_tchap_private)
        holder.titleView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_tchap_room_lock_red, 0, 0, 0)
        holder.titleView.setText(R.string.tchap_room_creation_private_room_title)
        holder.titleView.setTextColor(ContextCompat.getColor(holder.view.context, R.color.tchap_room_private))
        holder.descriptionView.setText(R.string.tchap_room_creation_private_room_info)
    }

    class Holder : VectorEpoxyHolder() {
        val containerView by bind<View>(R.id.roomItemContainer)
        val titleView by bind<TextView>(R.id.roomTypeTitle)
        val descriptionView by bind<TextView>(R.id.roomDescription)
    }
}
