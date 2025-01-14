/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.epoxy.bottomsheet

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.themes.ThemeUtils

/**
 * A action for bottom sheet.
 */
@EpoxyModelClass
abstract class BottomSheetActionItem : VectorEpoxyModel<BottomSheetActionItem.Holder>(R.layout.item_bottom_sheet_action) {

    @EpoxyAttribute
    @DrawableRes
    var iconRes: Int = 0

    @EpoxyAttribute
    var showIcon = true

    @EpoxyAttribute
    var text: String? = null

    @StringRes
    @EpoxyAttribute
    var textRes: Int = 0

    @EpoxyAttribute
    var showExpand = false

    @EpoxyAttribute
    var expanded = false

    @EpoxyAttribute
    var selected = false

    @EpoxyAttribute
    var subMenuItem = false

    @EpoxyAttribute
    var destructive = false

    @EpoxyAttribute
    var showBetaLabel = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    lateinit var listener: ClickListener

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.onClick(listener)
        holder.startSpace.isVisible = subMenuItem
        val tintColor = if (destructive) {
            ThemeUtils.getColor(holder.view.context, com.google.android.material.R.attr.colorError)
        } else {
            ThemeUtils.getColor(holder.view.context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
        }
        holder.icon.isVisible = showIcon
        holder.icon.setImageResource(iconRes)
        ImageViewCompat.setImageTintList(holder.icon, ColorStateList.valueOf(tintColor))
        if (text != null) {
            holder.text.text = text
        } else {
            holder.text.setText(textRes)
        }
        holder.text.setTextColor(tintColor)
        holder.selected.isInvisible = !selected
        if (showExpand) {
            val expandDrawable = if (expanded) {
                ContextCompat.getDrawable(holder.view.context, R.drawable.ic_expand_less)
            } else {
                ContextCompat.getDrawable(holder.view.context, R.drawable.ic_expand_more)
            }
            expandDrawable?.also {
                DrawableCompat.setTint(it, tintColor)
            }
            holder.text.setCompoundDrawablesWithIntrinsicBounds(null, null, expandDrawable, null)
        } else {
            holder.text.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
        holder.betaLabel.isVisible = showBetaLabel
    }

    class Holder : VectorEpoxyHolder() {
        val startSpace by bind<View>(R.id.actionStartSpace)
        val icon by bind<ImageView>(R.id.actionIcon)
        val text by bind<TextView>(R.id.actionTitle)
        val selected by bind<ImageView>(R.id.actionSelected)
        val betaLabel by bind<TextView>(R.id.actionBetaTextView)
    }
}
