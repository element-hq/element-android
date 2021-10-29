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
package fr.gouv.tchap.features.roomprofile.settings.linkaccess

import android.widget.TextView
import androidx.annotation.StringRes
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide

@EpoxyModelClass(layout = R.layout.item_tchap_setting_link_access)
abstract class TchapRoomLinkAccessItem : EpoxyModelWithHolder<TchapRoomLinkAccessItem.Holder>() {

    @EpoxyAttribute
    var descriptionText: String? = null

    @EpoxyAttribute
    @StringRes
    var descriptionResId: Int? = null

    @EpoxyAttribute
    var linkText: String? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var itemClickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)

        if (descriptionResId != null) {
            holder.description.setText(descriptionResId!!)
        } else {
            holder.description.setTextOrHide(descriptionText)
        }

        holder.link.setTextOrHide(linkText)

        holder.link.onClick(itemClickListener)
    }

    class Holder : VectorEpoxyHolder() {
        val description by bind<TextView>(R.id.settings_item_desc)
        val link by bind<TextView>(R.id.settings_item_link)
    }
}
