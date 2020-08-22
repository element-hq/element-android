/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.app.core.ui.list

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.utils.DebouncedClickListener
import im.vector.app.features.themes.ThemeUtils

/**
 * A generic list item.
 * Displays an item with a title, and optional description.
 * Can display an accessory on the right, that can be an image or an indeterminate progress.
 * If provided with an action, will display a button at the bottom of the list item.
 */
@EpoxyModelClass(layout = R.layout.item_generic_with_value)
abstract class GenericItemWithValue : VectorEpoxyModel<GenericItemWithValue.Holder>() {

    @EpoxyAttribute
    var title: CharSequence? = null

    @EpoxyAttribute
    var value: CharSequence? = null

    @EpoxyAttribute
    @ColorInt
    var valueColorInt: Int? = null

    @EpoxyAttribute
    @DrawableRes
    var titleIconResourceId: Int = -1

    @EpoxyAttribute
    var itemClickAction: View.OnClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.titleText.setTextOrHide(title)

        if (titleIconResourceId != -1) {
            holder.titleIcon.setImageResource(titleIconResourceId)
            holder.titleIcon.isVisible = true
        } else {
            holder.titleIcon.isVisible = false
        }

        holder.valueText.setTextOrHide(value)

        if (valueColorInt != null) {
            holder.valueText.setTextColor(valueColorInt!!)
        } else {
            holder.valueText.setTextColor(ThemeUtils.getColor(holder.view.context, R.attr.riotx_text_primary))
        }

        holder.view.setOnClickListener(itemClickAction?.let { DebouncedClickListener(it) })
    }

    class Holder : VectorEpoxyHolder() {
        val titleIcon by bind<ImageView>(R.id.itemGenericWithValueTitleIcon)
        val titleText by bind<TextView>(R.id.itemGenericWithValueLabelText)
        val valueText by bind<TextView>(R.id.itemGenericWithValueValueText)
    }
}
