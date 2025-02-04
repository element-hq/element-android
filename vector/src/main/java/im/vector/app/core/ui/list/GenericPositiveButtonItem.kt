/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.ui.list

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.android.material.button.MaterialButton
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick

/**
 * A generic button list item.
 */
@EpoxyModelClass
abstract class GenericPositiveButtonItem : VectorEpoxyModel<GenericPositiveButtonItem.Holder>(R.layout.item_positive_button) {

    @EpoxyAttribute
    var text: String? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var buttonClickAction: ClickListener? = null

    @EpoxyAttribute
    @ColorInt
    var textColor: Int? = null

    @EpoxyAttribute
    @DrawableRes
    var iconRes: Int? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.button.text = text
        if (iconRes != null) {
            holder.button.setIconResource(iconRes!!)
        } else {
            holder.button.icon = null
        }
        holder.button.onClick(buttonClickAction)
    }

    class Holder : VectorEpoxyHolder() {
        val button by bind<MaterialButton>(R.id.itemGenericItemButton)
    }
}
