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
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence

/**
 * A generic list item.
 * Displays an item with a title, and optional description.
 * Can display an accessory on the right, that can be an image or an indeterminate progress.
 * If provided with an action, will display a button at the bottom of the list item.
 */
@EpoxyModelClass(layout = R.layout.item_generic_list)
abstract class GenericItem : VectorEpoxyModel<GenericItem.Holder>() {

    @EpoxyAttribute
    var title: EpoxyCharSequence? = null

    @EpoxyAttribute
    var description: EpoxyCharSequence? = null

    @EpoxyAttribute
    var style: ItemStyle = ItemStyle.NORMAL_TEXT

    @EpoxyAttribute
    @DrawableRes
    var endIconResourceId: Int = -1

    @EpoxyAttribute
    @DrawableRes
    var titleIconResourceId: Int = -1

    @EpoxyAttribute
    var hasIndeterminateProcess = false

    @EpoxyAttribute
    var buttonAction: Action? = null

    @EpoxyAttribute
    var destructiveButtonAction: Action? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var itemClickAction: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.titleText.setTextOrHide(title?.charSequence)

        if (titleIconResourceId != -1) {
            holder.titleIcon.setImageResource(titleIconResourceId)
            holder.titleIcon.isVisible = true
        } else {
            holder.titleIcon.isVisible = false
        }

        holder.titleText.textSize = style.toTextSize()

        holder.descriptionText.setTextOrHide(description?.charSequence)

        if (hasIndeterminateProcess) {
            holder.progressBar.isVisible = true
            holder.accessoryImage.isVisible = false
        } else {
            holder.progressBar.isVisible = false
            if (endIconResourceId != -1) {
                holder.accessoryImage.setImageResource(endIconResourceId)
                holder.accessoryImage.isVisible = true
            } else {
                holder.accessoryImage.isVisible = false
            }
        }

        holder.actionButton.setTextOrHide(buttonAction?.title)
        holder.actionButton.onClick(buttonAction?.listener)

        holder.destructiveButton.setTextOrHide(destructiveButtonAction?.title)
        holder.destructiveButton.onClick(destructiveButtonAction?.listener)

        holder.root.onClick(itemClickAction)
    }

    class Holder : VectorEpoxyHolder() {
        val root by bind<View>(R.id.item_generic_root)
        val titleIcon by bind<ImageView>(R.id.item_generic_title_image)
        val titleText by bind<TextView>(R.id.item_generic_title_text)
        val descriptionText by bind<TextView>(R.id.item_generic_description_text)
        val accessoryImage by bind<ImageView>(R.id.item_generic_accessory_image)
        val progressBar by bind<ProgressBar>(R.id.item_generic_progress_bar)
        val actionButton by bind<Button>(R.id.item_generic_action_button)
        val destructiveButton by bind<Button>(R.id.item_generic_destructive_action_button)
    }
}
