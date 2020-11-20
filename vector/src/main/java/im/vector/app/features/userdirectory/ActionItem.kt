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

package im.vector.app.features.userdirectory

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.utils.DebouncedClickListener

@EpoxyModelClass(layout = R.layout.item_contact_action)
abstract class ActionItem : VectorEpoxyModel<ActionItem.Holder>() {

    @EpoxyAttribute var title: CharSequence? = null
    @EpoxyAttribute @DrawableRes var actionIconRes: Int? = null
    @EpoxyAttribute var clickAction: View.OnClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.setOnClickListener(clickAction?.let { DebouncedClickListener(it) })
        // If name is empty, use userId as name and force it being centered
        holder.actionTitleText.setTextOrHide(title)
        if (actionIconRes != null) {
            holder.actionTitleImageView.setImageResource(actionIconRes!!)
        } else {
            holder.actionTitleImageView.setImageDrawable(null)
        }
    }

    class Holder : VectorEpoxyHolder() {
        val actionTitleText by bind<TextView>(R.id.actionTitleText)
        val actionTitleImageView by bind<ImageView>(R.id.actionIconImageView)
    }
}
