/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.ui.list

import android.widget.TextView
import androidx.annotation.ColorInt
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.themes.ThemeUtils

/**
 * A generic list item header left aligned with notice color.
 */
@EpoxyModelClass
abstract class GenericHeaderItem : VectorEpoxyModel<GenericHeaderItem.Holder>(R.layout.item_generic_header) {

    @EpoxyAttribute
    var text: String? = null

    @EpoxyAttribute
    @ColorInt
    var textColor: Int? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.text.setTextOrHide(text)
        if (textColor != null) {
            holder.text.setTextColor(textColor!!)
        } else {
            holder.text.setTextColor(ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_notice_text_color))
        }
    }

    class Holder : VectorEpoxyHolder() {
        val text by bind<TextView>(R.id.itemGenericHeaderText)
    }
}
