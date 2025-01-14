/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.ui.list

import android.view.Gravity
import android.widget.TextView
import androidx.annotation.ColorInt
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
 * A generic list item.
 * Displays an item with a title, and optional description.
 * Can display an accessory on the right, that can be an image or an indeterminate progress.
 * If provided with an action, will display a button at the bottom of the list item.
 */
@EpoxyModelClass
abstract class GenericFooterItem : VectorEpoxyModel<GenericFooterItem.Holder>(R.layout.item_generic_footer) {

    @EpoxyAttribute
    var text: EpoxyCharSequence? = null

    @EpoxyAttribute
    var style: ItemStyle = ItemStyle.NORMAL_TEXT

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var itemClickAction: ClickListener? = null

    @EpoxyAttribute
    var centered: Boolean = true

    @EpoxyAttribute
    @ColorInt
    var textColor: Int? = null

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.text.setTextOrHide(text?.charSequence)
        holder.text.typeface = style.toTypeFace()
        holder.text.textSize = style.toTextSize()
        holder.text.gravity = if (centered) Gravity.CENTER_HORIZONTAL else Gravity.START

        if (textColor != null) {
            holder.text.setTextColor(textColor!!)
        } else {
            holder.text.setTextColor(ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary))
        }

        holder.view.onClick(itemClickAction)
    }

    class Holder : VectorEpoxyHolder() {
        val text by bind<TextView>(R.id.itemGenericFooterText)
    }
}
