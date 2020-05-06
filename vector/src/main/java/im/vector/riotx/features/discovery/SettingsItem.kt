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
package im.vector.riotx.features.discovery

import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.extensions.setTextOrHide

@EpoxyModelClass(layout = R.layout.item_settings_simple_item)
abstract class SettingsItem : EpoxyModelWithHolder<SettingsItem.Holder>() {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    @StringRes
    var titleResId: Int? = null

    @EpoxyAttribute
    @StringRes
    var descriptionResId: Int? = null

    @EpoxyAttribute
    var description: CharSequence? = null

    @EpoxyAttribute
    var itemClickListener: View.OnClickListener? = null

    override fun bind(holder: Holder) {

        if (titleResId != null) {
            holder.titleText.setText(titleResId!!)
        } else {
            holder.titleText.setTextOrHide(title)
        }

        if (descriptionResId != null) {
            holder.descriptionText.setText(descriptionResId!!)
        } else {
            holder.descriptionText.setTextOrHide(description)
        }

        //If there is only a description, use primary color
//        holder.descriptionText.setTextColor(
//                if (holder.titleText.text.isNullOrBlank()) {
//                    ThemeUtils.getColor(holder.main.context, android.R.attr.textColorPrimary)
//                } else {
//                    ThemeUtils.getColor(holder.main.context, android.R.attr.textColorSecondary)
//                }
//        )

        holder.switchButton.isVisible = false

        holder.view.setOnClickListener(itemClickListener)
    }

    class Holder : VectorEpoxyHolder() {
        val titleText by bind<TextView>(R.id.settings_item_title)
        val descriptionText by bind<TextView>(R.id.settings_item_description)
        val switchButton by bind<Switch>(R.id.settings_item_switch)
    }
}
