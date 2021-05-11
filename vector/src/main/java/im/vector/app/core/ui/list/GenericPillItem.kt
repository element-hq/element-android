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

import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.widget.ImageViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.themes.ThemeUtils

/**
 * A generic list item.
 * Displays an item with a title, and optional description.
 * Can display an accessory on the right, that can be an image or an indeterminate progress.
 * If provided with an action, will display a button at the bottom of the list item.
 */
@EpoxyModelClass(layout = R.layout.item_generic_pill_footer)
abstract class GenericPillItem : VectorEpoxyModel<GenericPillItem.Holder>() {

    @EpoxyAttribute
    var text: CharSequence? = null

    @EpoxyAttribute
    var style: ItemStyle = ItemStyle.NORMAL_TEXT

    @EpoxyAttribute
    var itemClickAction: GenericItem.Action? = null

    @EpoxyAttribute
    var centered: Boolean = false

    @EpoxyAttribute
    @DrawableRes
    var imageRes: Int? = null

    @EpoxyAttribute
    var tintIcon: Boolean = true

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.textView.setTextOrHide(text)
        holder.textView.typeface = style.toTypeFace()
        holder.textView.textSize = style.toTextSize()
        holder.textView.gravity = if (centered) Gravity.CENTER_HORIZONTAL else Gravity.START

        imageRes?.let { holder.imageView.setImageResource(it) }
        if (tintIcon) {
            val iconTintColor = ThemeUtils.getColor(holder.view.context, R.attr.riotx_text_secondary)
            ImageViewCompat.setImageTintList(holder.imageView, ColorStateList.valueOf(iconTintColor))
        } else {
            ImageViewCompat.setImageTintList(holder.imageView, null)
        }

        holder.view.setOnClickListener {
            itemClickAction?.perform?.run()
        }
    }

    class Holder : VectorEpoxyHolder() {
        val imageView by bind<ImageView>(R.id.itemGenericPillImage)
        val textView by bind<TextView>(R.id.itemGenericPillText)
    }
}
