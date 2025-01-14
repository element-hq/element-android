/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.form

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.themes.ThemeUtils

@EpoxyModelClass
abstract class FormAdvancedToggleItem : VectorEpoxyModel<FormAdvancedToggleItem.Holder>(R.layout.item_form_advanced_toggle) {

    @EpoxyAttribute lateinit var title: String
    @EpoxyAttribute var expanded: Boolean = false
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var listener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        val tintColor = ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
        val expandedArrowDrawableRes = if (expanded) R.drawable.ic_expand_more else R.drawable.ic_expand_less
        val expandedArrowDrawable = ContextCompat.getDrawable(holder.view.context, expandedArrowDrawableRes)?.also {
            DrawableCompat.setTint(it, tintColor)
        }
        holder.titleView.setCompoundDrawablesWithIntrinsicBounds(null, null, expandedArrowDrawable, null)
        holder.titleView.text = title
        holder.view.onClick(listener)
    }

    class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.itemFormAdvancedToggleTitleView)
    }
}
