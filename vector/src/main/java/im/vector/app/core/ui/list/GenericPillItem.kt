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
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence

/**
 * A generic list item with a rounded corner background and an optional icon
 */
@EpoxyModelClass(layout = R.layout.item_generic_pill_footer)
abstract class GenericPillItem : VectorEpoxyModel<GenericPillItem.Holder>() {

    @EpoxyAttribute
    var text: EpoxyCharSequence? = null

    @EpoxyAttribute
    var style: ItemStyle = ItemStyle.NORMAL_TEXT

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var itemClickAction: ClickListener? = null

    @EpoxyAttribute
    var centered: Boolean = false

    @EpoxyAttribute
    @DrawableRes
    var imageRes: Int? = null

    @EpoxyAttribute
    var tintIcon: Boolean = true

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.textView.setTextOrHide(text?.charSequence)
        holder.textView.typeface = style.toTypeFace()
        holder.textView.textSize = style.toTextSize()
        holder.textView.gravity = if (centered) Gravity.CENTER_HORIZONTAL else Gravity.START

        if (imageRes != null) {
            holder.imageView.setImageResource(imageRes!!)
            holder.imageView.isVisible = true
        } else {
            holder.imageView.isVisible = false
        }
        if (tintIcon) {
            val iconTintColor = ThemeUtils.getColor(holder.view.context, R.attr.vctr_content_secondary)
            ImageViewCompat.setImageTintList(holder.imageView, ColorStateList.valueOf(iconTintColor))
        } else {
            ImageViewCompat.setImageTintList(holder.imageView, null)
        }

        holder.view.onClick(itemClickAction)
    }

    class Holder : VectorEpoxyHolder() {
        val imageView by bind<ImageView>(R.id.itemGenericPillImage)
        val textView by bind<TextView>(R.id.itemGenericPillText)
    }
}
