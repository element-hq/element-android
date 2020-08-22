/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package im.vector.app.core.epoxy.bottomsheet

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.themes.ThemeUtils

/**
 * A action for bottom sheet.
 */
@EpoxyModelClass(layout = R.layout.item_bottom_sheet_action)
abstract class BottomSheetActionItem : VectorEpoxyModel<BottomSheetActionItem.Holder>() {

    @EpoxyAttribute
    @DrawableRes
    var iconRes: Int = 0
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
    lateinit var listener: View.OnClickListener

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.setOnClickListener {
            listener.onClick(it)
        }
        holder.startSpace.isVisible = subMenuItem
        val tintColor = if (destructive) {
            ContextCompat.getColor(holder.view.context, R.color.riotx_notice)
        } else {
            ThemeUtils.getColor(holder.view.context, R.attr.riotx_text_secondary)
        }
        holder.icon.setImageResource(iconRes)
        ImageViewCompat.setImageTintList(holder.icon, ColorStateList.valueOf(tintColor))
        holder.text.setText(textRes)
        holder.text.setTextColor(tintColor)
        holder.selected.isInvisible = !selected
        if (showExpand) {
            val expandDrawable = if (expanded) {
                ContextCompat.getDrawable(holder.view.context, R.drawable.ic_material_expand_less_black)
            } else {
                ContextCompat.getDrawable(holder.view.context, R.drawable.ic_material_expand_more_black)
            }
            expandDrawable?.also {
                DrawableCompat.setTint(it, tintColor)
            }
            holder.text.setCompoundDrawablesWithIntrinsicBounds(null, null, expandDrawable, null)
        } else {
            holder.text.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }

    class Holder : VectorEpoxyHolder() {
        val startSpace by bind<View>(R.id.actionStartSpace)
        val icon by bind<ImageView>(R.id.actionIcon)
        val text by bind<TextView>(R.id.actionTitle)
        val selected by bind<ImageView>(R.id.actionSelected)
    }
}
