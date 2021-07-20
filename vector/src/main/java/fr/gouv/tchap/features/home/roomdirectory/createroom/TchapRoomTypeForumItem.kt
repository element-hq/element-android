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
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.onClick

@EpoxyModelClass(layout = R.layout.item_tchap_room_type)
abstract class TchapRoomTypeForumItem : EpoxyModelWithHolder<TchapRoomTypeForumItem.Holder>() {

    @EpoxyAttribute
    var selected: Boolean = false

    @EpoxyAttribute
    var checked: Boolean = false

    @EpoxyAttribute
    var clickListener: ClickListener? = null

    @EpoxyAttribute
    var checkListener: OnCheckedChangeListener? = null

    @EpoxyAttribute
    var userDomain: String? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.containerView.onClick(clickListener)
        holder.containerView.isSelected = selected
        holder.containerView.setBackgroundResource(R.drawable.bg_tchap_forum)
        holder.titleView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_tchap_forum, 0, 0, 0)
        holder.titleView.setText(R.string.tchap_room_creation_forum_room_title)
        holder.titleView.setTextColor(ContextCompat.getColor(holder.view.context, R.color.tchap_room_forum))
        holder.descriptionView.setText(R.string.tchap_room_creation_forum_room_info)
        holder.description2View.setText(R.string.tchap_room_creation_public_info)
        holder.switchView.text = holder.view.context.getString(R.string.tchap_room_creation_limited_domain, userDomain.orEmpty())
        holder.description2View.isVisible = selected
        holder.switchView.isChecked = checked
        holder.switchView.isVisible = selected
        holder.switchView.setOnCheckedChangeListener(checkListener)
    }

    class Holder : VectorEpoxyHolder() {
        val containerView by bind<View>(R.id.roomItemContainer)
        val titleView by bind<TextView>(R.id.roomTypeTitle)
        val descriptionView by bind<TextView>(R.id.roomDescription)
        val description2View by bind<TextView>(R.id.roomDescription2)
        val switchView by bind<CompoundButton>(R.id.toggleButton)
    }
}
