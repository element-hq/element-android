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
package im.vector.app.features.home

import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.onClick

@EpoxyModelClass(layout = R.layout.item_home_drawer_action)
abstract class HomeDrawerActionItem : EpoxyModelWithHolder<HomeDrawerActionItem.Holder>() {

    @EpoxyAttribute
    @DrawableRes
    var iconRes: Int = 0

    @EpoxyAttribute
    @StringRes
    var titleRes: Int = 0

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var itemClickAction: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.icon.setImageResource(iconRes)
        holder.title.setText(titleRes)
        holder.view.onClick(itemClickAction)
    }

    class Holder : VectorEpoxyHolder() {
        val icon by bind<ImageView>(R.id.actionIconImageView)
        val title by bind<TextView>(R.id.actionTitleText)
    }
}
